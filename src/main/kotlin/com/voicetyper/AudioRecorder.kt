package com.voicetyper

import java.io.IOException
import javax.sound.sampled.*

/**
 * Запись аудио с микрофона через Java Sound API.
 * Поддерживает запись короткими фрагментами (для push-to-talk).
 */
class AudioRecorder(private val config: AppConfig) {

    private val logger = Logger.loggerFor("AudioRecorder")
    
    @Volatile
    private var isRecording = false

    /**
     * Записывает аудиофрагмент с момента вызова до stopRecording().
     * @return Массив коротких (PCM 16-bit, little-endian)
     */
    fun recordSync(): ShortArray {
        val format = buildFormat()
        val recorded = mutableListOf<Short>()

        try {
            val lineInfo = DataLine.Info(TargetDataLine::class.java, format)
            val line = AudioSystem.getLine(lineInfo) as TargetDataLine
            line.open(format)
            line.start()

            logger.info("Начало записи аудио: ${format.toString()}")

            val blockSize = config.sampleRate / 4 // 250 мс блоки
            val readBuffer = ByteArray(blockSize * 2)

            while (isRecording) {
                val bytesRead = line.read(readBuffer, 0, readBuffer.size)
                if (bytesRead > 0) {
                    // Конвертация PCM signed 16-bit из byteArray в ShortArray
                    for (i in 0 until bytesRead step 2) {
                        val low = readBuffer[i].toInt() and 0xFF
                        val high = readBuffer[i + 1].toInt()
                        val sample = (high shl 8 or low).toShort()
                        recorded.add(sample)
                    }
                }
            }

            line.stop()
            line.close()

            logger.info("Запись остановлена, собрано ${recorded.size} сэмплов")
            return recorded.toShortArray()

        } catch (e: LineUnavailableException) {
            logger.error("Аудиoline недоступен: ${e.message}")
            throw IOException("Аудиоустройство недоступно. Проверьте настройки микрофона.", e)
        } catch (e: Exception) {
            logger.error("Ошибка записи: ${e.message}")
            throw IOException("Ошибка записи аудио: ${e.message}", e)
        }
    }

    /**
     * Устанавливает флаг, что запись должна остановиться.
     */
    fun stopRecording() {
        isRecording = false
    }

    /**
     * Включает запись (вызывается ДО recordSync).
     */
    fun startRecording() {
        isRecording = true
    }

    /**
     * Проверяет, есть ли доступные аудиоустройства.
     */
    fun isAvailable(): Boolean {
        return try {
            val format = buildFormat()
            val info = DataLine.Info(TargetDataLine::class.java, format)
            AudioSystem.isLineSupported(info)
        } catch (e: Exception) {
            logger.warn("Проверка аудиоустройства: ${e.message}")
            false
        }
    }

    /**
     * Получает список доступных аудиоустройств.
     */
    fun getAvailableDevices(): List<String> {
        val devices = mutableListOf<String>()
        try {
            val mixerData = AudioSystem.getMixerInfo()
            for (mixerInfo in mixerData) {
                try {
                    val mixer = AudioSystem.getMixer(mixerInfo)
                    val lines = mixer.targetLines
                    if (lines.isNotEmpty()) {
                        devices.add("${mixerInfo.name}${mixerInfo.vendor.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}")
                    }
                } catch (e: Exception) {
                    // Игнорируем устройства, которые не удалось проверить
                }
            }
        } catch (e: Exception) {
            logger.warn("Получение списка устройств: ${e.message}")
        }
        return devices.ifEmpty { listOf("По умолчанию (TargetDataLine)") }
    }

    private fun buildFormat(): AudioFormat {
        return AudioFormat(
            config.sampleRate.toFloat(),
            16,
            config.channels,
            true,
            false
        )
    }
}
