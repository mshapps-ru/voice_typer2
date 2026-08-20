package com.voicetyper

import java.awt.EventQueue
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * Центральный процессор приложения — координирует все компоненты.
 */
class AppProcessor(
    private val configManager: ConfigManager,
    private val audioRecorder: AudioRecorder,
    private val whisperEngine: WhisperEngine,
    private val clipboardManager: ClipboardManager,
    private val uiCanvas: UiCanvas,
    private val trayManager: TrayIconManager,
    private val hotkeyListener: HotkeyListener
) : AutoCloseable {

    private val logger = Logger.loggerFor("AppProcessor")
    private val executor = Executors.newCachedThreadPool()
    var config = configManager.getConfig()
        private set

    @Volatile
    private var isVisibilityToggled = false

    @Volatile
    private var isRecording = false

    init {
        logger.info("=== Voice Typer запущен ===")
        logger.info("Модель: ${config.modelSize}, Язык: ${config.language}")
        logger.info("Окно: ${config.windowSize}")
        
        // Настройка UI
        setupUi()
        
        // Настройка горячих клавиш (ЗАКОММЕНТИРОВАНО - временно отключено)
        setupHotkeys()
        
        // Запуск транскрипции
        if (!whisperEngine.initialize()) {
            uiCanvas.setStatus(Locales.get(config.appLanguage, "error"))
            logger.error("Не удалось инициализировать whisper.cpp (не найдена модель или ejecutable)")
        }
    }

    private fun setupUi() {
        val (w, h) = AppConfig.parseWindowSize(config.windowSize)
        uiCanvas.updateDimensions(w, h)
        uiCanvas.setStatus(Locales.get(config.appLanguage, "ready_to_work"))
    }

    // Закомментировано - временно отключено
    private fun setupHotkeys() {
        val hkRecord = config.recordKey.lowercase()
        val hkLang = config.langToggleHotkey.lowercase()
        val hkShow = config.showWindowHotkey.lowercase()

        hotkeyListener.registerForPress(false, hkRecord) { startRecording() }
        hotkeyListener.registerForPress(true, hkRecord) { stopRecording() }

        hotkeyListener.registerForPress(true, hkLang) { toggleLanguage() }
        hotkeyListener.registerForPress(true, hkShow) { toggleVisibility() }

        logger.info("Горячие клавиши настроены: F9=запись, F10=язык, F8=окно")
    }

    private fun startRecording() {
        if (isRecording) return
        isRecording = true
        
        EventQueue.invokeLater {
            uiCanvas.setRecording(true)
            trayManager.updateIcon(true)
        }

        executor.submit {
            try {
                audioRecorder.startRecording()
                val audioData = audioRecorder.recordSync()
                transcribeAudio(audioData)
            } catch (e: Exception) {
                logger.error("Ошибка записи: ${e.message}")
                EventQueue.invokeLater {
                    uiCanvas.setStatus(Locales.get(config.appLanguage, "error"))
                    uiCanvas.setRecording(false)
                    trayManager.updateIcon(false)
                }
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        audioRecorder.stopRecording()
        isRecording = false
    }

    private fun transcribeAudio(audioData: ShortArray) {
        if (audioData.isEmpty()) {
            EventQueue.invokeLater {
                uiCanvas.setStatus(Locales.get(config.appLanguage, "empty_signal"))
                uiCanvas.setRecording(false)
                trayManager.updateIcon(false)
            }
            return
        }

        EventQueue.invokeLater {
            uiCanvas.setStatus(Locales.get(config.appLanguage, "processing"))
        }

        // Создаём временный WAV-файл
        val tempFile = File.createTempFile("voice_typer_", ".wav")
        tempFile.deleteOnExit()

        var lang = config.language

        try {
            WavFileWriter().write(audioData, tempFile, config.sampleRate)

            // Запускаем транскрипцию
            val future = whisperEngine.transcribeAsync(tempFile, lang)
            future.thenAccept { result ->
                EventQueue.invokeLater {
                    if (result.success && result.text.isNotEmpty()) {
                        clipboardManager.pasteText(result.text)
                        uiCanvas.setStatus(Locales.get(config.appLanguage, "ready"))
                        logger.info("Распознано: ${result.text.take(50)}")
                    } else {
                        uiCanvas.setStatus(Locales.get(config.appLanguage, "not_recognized"))
                    }
                    uiCanvas.setRecording(false)
                    trayManager.updateIcon(false)
                }
            }.exceptionally { e ->
                logger.error("Ошибка транскрипции: ${e.message}")
                EventQueue.invokeLater {
                    uiCanvas.setStatus(Locales.get(config.appLanguage, "error"))
                    uiCanvas.setRecording(false)
                    trayManager.updateIcon(false)
                }
                null
            }
        } catch (e: Exception) {
            logger.error("Ошибка создания WAV: ${e.message}")
            EventQueue.invokeLater {
                uiCanvas.setStatus(Locales.get(config.appLanguage, "error"))
                uiCanvas.setRecording(false)
                trayManager.updateIcon(false)
            }
        }
    }

    /** Переключение видимости окна */
    fun toggleVisibility() {
        isVisibilityToggled = !isVisibilityToggled
        uiCanvas.frame?.apply {
            isVisible = isVisibilityToggled
        }
        logger.info("Переключение видимости: ${if (isVisibilityToggled) "показать" else "скрыть"}")
    }

    /** Переключение языка */
    fun toggleLanguage() {
        val newLang = if (config.language == "ru") "en" else "ru"
        config = config.copy(language = newLang)
        configManager.update(config)
        uiCanvas.setLanguage(newLang)

        config.language = newLang;

        logger.info("Язык транскрибации изменён: $newLang")
    }

    fun shutdown() {
        logger.info("Остановка Voice Typer")
        executor.shutdown()
        hotkeyListener.close()
        trayManager.shutdown()
        Logger.shutdown()
    }

    override fun close() {
        shutdown()
    }
}
