package com.voicetyper

import java.io.*

/**
 * Запись сырых PCM-данных (int16 LE) в WAV-файл.
 * Формат: 16kHz, Mono, 16-bit PCM.
 */
class WavFileWriter {

    /**
     * Записывает PCM-данные в WAV-файл.
     * @param pcmData Сырые 16-битные целые (little-endian)
     * @param outputPath Путь к выходному WAV-файлу
     * @param sampleRate Частота дискретизации (по умолчанию 16000)
     */
    fun write(
        pcmData: ShortArray,
        outputPath: File,
        sampleRate: Int = 16000
    ) {
        val numSamples = pcmData.size
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * 2 // 2 bytes per sample
        val bufferSize = 36 + dataSize // WAV header + data

        FileOutputStream(outputPath).use { out ->
            writeHeader(out, bufferSize, numChannels, sampleRate, bitsPerSample, dataSize)
            out.write(shortArrayToBytes(pcmData))
            out.flush()
        }
    }

    private fun writeHeader(
        out: OutputStream,
        fileSize: Int,
        numChannels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        dataSize: Int
    ) {
        // RIFF header
        writeString(out, "RIFF")
        writeInt32LE(out, fileSize - 8)
        writeString(out, "WAVE")

        // fmt chunk
        writeString(out, "fmt ")
        writeInt32LE(out, 16) // Chunk size
        writeInt16LE(out, 1) // PCM format
        writeInt16LE(out, numChannels)
        writeInt32LE(out, sampleRate)
        writeInt32LE(out, sampleRate * numChannels * bitsPerSample / 8)
        writeInt16LE(out, numChannels * bitsPerSample / 8)
        writeInt16LE(out, bitsPerSample)

        // data chunk
        writeString(out, "data")
        writeInt32LE(out, dataSize)
    }

    private fun writeString(out: OutputStream, s: String) {
        for (c in s.toCharArray()) {
            out.write(c.code)
        }
    }

    private fun writeInt32LE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun writeInt16LE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun shortArrayToBytes(data: ShortArray): ByteArray {
        val bytes = ByteArray(data.size * 2)
        for (i in data.indices) {
            bytes[i * 2] = data[i].toByte()
            bytes[i * 2 + 1] = (data[i].toInt() ushr 8).toByte()
        }
        return bytes
    }
}
