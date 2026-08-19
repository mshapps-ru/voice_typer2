package com.voicetyper

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Класс для парсинга JSON вывода whisper-cli.exe
 * Формат: {"text": "...", "segments": [...], "language": "...", ...}
 */
class WhisperJsonParser {

    companion object {
        private val gson = com.google.gson.Gson()
    }

    /**
     * Парсит JSON вывод whisper-cli.exe
     * @param output Полный вывод процесса (stdout + stderr объединены)
     * @return TranscriptionResult с распознанным текстом
     */
    fun parse(output: String): TranscriptionResult {
        // whisper-cli.exe выводит debug-сообщения в stderr и JSON в stdout
        // При redirectErrorStream(true) stderr перенаправляется в stdout
        // JSON обычно в конце вывода, после всех debug-сообщений
        
        // Ищем начало JSON объекта
        val jsonStart = findJsonStart(output)
        if (jsonStart == -1) {
            return TranscriptionResult("", success = false, error = "JSON не найден в выводе whisper-cli.exe")
        }

        val jsonStr = output.substring(jsonStart).trim()
        
        // Логирование JSON для отладки
        if (jsonStr.length < 1000) {
            logger.debug("JSON вывод: $jsonStr")
        } else {
            logger.debug("JSON вывод (обрезан): ${jsonStr.take(500)}...")
        }

        return try {
            val jsonElement = JsonParser.parseString(jsonStr)
            val jsonObject = jsonElement.asJsonObject
            
            // Извлекаем поле text
            val text = if (jsonObject.has("text")) {
                jsonObject.get("text").asString
            } else {
                ""
            }.trim()

            // Извлекаем язык (опционально)
            val language = if (jsonObject.has("language")) {
                jsonObject.get("language").asString
            } else {
                "unknown"
            }

            logger.info("Распознано: '$text' (язык: $language)")
            
            if (text.isEmpty()) {
                TranscriptionResult.NotRecognized
            } else {
                TranscriptionResult(text, success = true)
            }
        } catch (e: Exception) {
            logger.error("Ошибка парсинга JSON: ${e.message}")
            TranscriptionResult("", success = false, error = "Ошибка парсинга JSON: ${e.message}")
        }
    }

    /**
     * Находит начало JSON объекта в строке
     * JSON начинается с '{'
     */
    private fun findJsonStart(input: String): Int {
        val braceIndex = input.indexOf('{')
        return if (braceIndex >= 0) braceIndex else -1
    }

    /**
     * Логгер для парсера
     */
    private val logger = Logger.loggerFor("WhisperJsonParser")
}
