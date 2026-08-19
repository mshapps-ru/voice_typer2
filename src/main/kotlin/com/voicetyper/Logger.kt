package com.voicetyper

import java.io.*
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Простой менеджер логирования.
 * Логи выводятся в stdout и в файл logs/voicetyper.log
 */
class Logger private constructor(private val tag: String) {

    companion object {
        private const val LOG_DIR = "logs"
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        @Volatile
        private var logWriter: PrintWriter? = null

        init {
            try {
                val logFile = File(LOG_DIR, "voicetyper.log")
                logFile.parentFile?.mkdirs()
                // Используем OutputStreamWriter с UTF-8 для корректной кириллицы
                logWriter = PrintWriter(BufferedWriter(
                    OutputStreamWriter(FileOutputStream(logFile, true), StandardCharsets.UTF_8)
                ), true)
            } catch (e: Exception) {
                // Игнорируем ошибку инициализации файла
            }
        }

        fun loggerFor(tag: String): Logger = Logger(tag)

        fun shutdown() {
            logWriter?.apply {
                flush()
                close()
            }
            logWriter = null
        }
    }

    private fun log(level: String, message: String) {
        val timestamp = LocalDateTime.now().format(FORMATTER)
        val formatted = "[$timestamp] [$level] [$tag] $message"

        // Вывод в stdout/stderr с принудительной кодировкой
        val bytes = formatted.toByteArray(StandardCharsets.UTF_8)
        if (level == "ERROR") {
            System.err.write(bytes)
            System.err.write('\n'.toInt())
            System.err.flush()
        } else {
            System.out.write(bytes)
            System.out.write('\n'.toInt())
            System.out.flush()
        }

        // В файл
        logWriter?.println(formatted)
    }

    fun info(message: String) = log("INFO", message)
    fun warn(message: String) = log("WARN", message)
    fun error(message: String) = log("ERROR", message)
    fun debug(message: String) = log("DEBUG", message)
}
