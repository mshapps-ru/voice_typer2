package com.voicetyper

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Обёртка над whisper.cpp CLI.
 * Вызывает whisper-cli.exe с нужными параметрами и парсит JSON-результат.
 */
class WhisperEngine(private val config: AppConfig) {

    private val logger = Logger.loggerFor("WhisperEngine")
    private val executor = Executors.newSingleThreadExecutor()

    // Путь к whisper-cli.exe — ищем в resources/win/ или в PATH
    private val whisperExecutable: String by lazy {
        findWhisperExecutable()
    }

    // Путь к модели whisper
    private val modelFile: File by lazy {
        val modelDir = File("resources/win/models")
        modelDir.mkdirs()
        File(modelDir, "ggml-${config.modelSize}.bin")
    }

    // URL для скачивания модели (GitHub releases)
    private val modelUrl: String
        get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${config.modelSize.uppercase()}.bin"

    /**
     * Скачивает модель если её нет
     */
    fun ensureModelAvailable(): Boolean {
        if (modelFile.exists()) {
            logger.info("Модель найдена: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")
            return true
        }

        logger.info("Модель не найдена. Загрузка: $modelUrl")
        return try {
            modelFile.parentFile.mkdirs()
            val url = URL(modelUrl)
            val totalSize = url.openConnection().contentLengthLong
            var downloaded = 0L

            url.openStream().use { input ->
                modelFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        val progress = if (totalSize > 0) "${downloaded * 100 / totalSize}%" else "..."
                        logger.debug("Загрузка модели: $progress ($downloaded / $totalSize bytes)")
                    }
                }
            }
            logger.info("Модель успешно загружена: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")
            true
        } catch (e: Exception) {
            logger.error("Ошибка загрузки модели: ${e.message}")
            false
        }
    }

    /**
     * Запускает транскрипцию WAV-файла асинхронно.
     * @return CompletableFuture с результатом распознания
     */
    fun transcribeAsync(wavFile: File, lang: String ): CompletableFuture<TranscriptionResult> {
        return CompletableFuture.supplyAsync {
            try {
                val jsonParser = WhisperJsonParser()
                logger.info("Запуск транскрипции: ${wavFile.absolutePath}")
                logger.info("Модель: ${config.modelSize}, Язык: ${lang}")

                val process = ProcessBuilder(*buildCommand(wavFile, lang))
                    .redirectErrorStream(true)
                    .start()

                if (!process.waitFor(60, TimeUnit.SECONDS)) {
                    logger.error("Таймаут транскрипции")
                    process.destroyForcibly()
                    return@supplyAsync TranscriptionResult("", success = false, error = "Таймаут транскрипции")
                }

                val exitCode = process.exitValue()
                if (exitCode != 0) {
                    logger.error("Whisper завершился с кодом $exitCode")
                    return@supplyAsync TranscriptionResult("", success = false, error = "Код возврата: $exitCode")
                }

                val stdoutText = process.inputStream.bufferedReader().use { it.readText() }
                val jsonFile = File(wavFile.parentFile, wavFile.name + ".json")
                val finalOutput = if (jsonFile.exists()) {
                    try {
                        logger.info("Транскрипция читается из JSON‑файла ${jsonFile.absolutePath}")
                        jsonFile.readText(Charsets.UTF_8)
                    } catch (e: Exception) {
                        logger.warn("Не удалось прочитать JSON‑файл ${jsonFile.absolutePath}: ${e.message}")
                        stdoutText
                    }
                } else {
                    logger.info("JSON‑файл не найден, читаем stdout процесса")
                    stdoutText
                }

                val result = jsonParser.parse(finalOutput)
                logger.info("Распознавание завершено: ${result.text.take(150)}")
                val wawFileForDelete = File(wavFile.parentFile, wavFile.name)
                val jsonFileForDelete = File(wavFile.parentFile, wavFile.name + ".json")
                logger.info("Удаляем временный wav файл: ${wawFileForDelete}")
                wawFileForDelete.delete()
                logger.info("Удаляем временный json файл: ${jsonFileForDelete}")
                jsonFileForDelete.delete()
                result
            } catch (e: Exception) {
                logger.error("Ошибка транскрипции: ${e.message}")
                TranscriptionResult("", success = false, error = e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Проверка доступности whisper-cli.exe
     */
    fun isAvailable(): Boolean {
        return File(whisperExecutable).exists()
    }

    /** Инициализация: проверяем доступность whisper-cli.exe и модели */
    fun initialize(): Boolean {
        val available = isAvailable()
        if (!available) {
            logger.error("whisper-cli.exe не найден! Положите whisper-cli.exe в resources/win/")
            return false
        }
        logger.info("whisper.cpp найден: $whisperExecutable")
        return ensureModelAvailable()
    }

    private fun buildCommand(wavFile: File, lang: String ): Array<String> {
        val cmd = mutableListOf(
            whisperExecutable,
            "--file", wavFile.absolutePath,
            "--model", modelFile.absolutePath,
            "--beam-size", config.beamSize.toString(),
            "--output-json"
        )

        // Язык: оставляем авто, если не указано явно. Это позволяет Whisper автоматически определять язык.
        if (config.language.isNotEmpty() && config.language != "auto") {
            cmd.add("--language")
            cmd.add(lang)
        }

        // Промпт: Отключено, так как может влиять на распознавание
        /*if (config.initialPrompt.isNotBlank()) {
            cmd.add("--prompt")
            cmd.add(config.initialPrompt)
        }*/

        return cmd.toTypedArray()
    }

    /** Ищет whisper-cli.exe в resources/win/ или в PATH */
    private fun findWhisperExecutable(): String {
        // 1. Ищем в resources/win/ относительно workdir
        val localPath = File("resources/win/whisper-cli.exe")
        if (localPath.exists()) {
            return localPath.absolutePath
        }

        // 2. Ищем в текущей директории
        val currentPath = File("whisper-cli.exe")
        if (currentPath.exists()) {
            return currentPath.absolutePath
        }

        // 3. Ищем в PATH
        val pathEnv = System.getenv("PATH") ?: ""
        for (dir in pathEnv.split(File.pathSeparator)) {
            val whisperInPath = File(dir, "whisper-cli.exe")
            if (whisperInPath.exists()) {
                return whisperInPath.absolutePath
            }
        }

        // Не найден — возвращаем путь, который будет использоваться
        return File("resources/win/whisper-cli.exe").absolutePath
    }

    fun shutdown() {
        executor.shutdown()
    }
}