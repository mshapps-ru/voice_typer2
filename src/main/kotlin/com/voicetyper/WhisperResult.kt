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
        // Ищем начало и конец JSON объекта. Whisper может вывести много debug‑текстов.
        val jsonRange = findJsonRange(output)
        if (jsonRange.first == -1 || jsonRange.second == -1) {
            return TranscriptionResult("", success = false, error = "JSON не найден в выводе whisper-cli.exe")
        }

        val jsonStr = output.substring(jsonRange.first, jsonRange.second + 1).trim()
        
        // Логирование JSON для отладки
        if (jsonStr.length < 1000) {
            logger.debug("JSON вывод: $jsonStr")
        } else {
            logger.debug("JSON вывод (обрезан): ${jsonStr.take(500)}...")
        }

        return try {
            val jsonElement = JsonParser.parseString(jsonStr)
            val jsonObject = jsonElement.asJsonObject
            
            // Извлекаем язык (опционально)
            val language = if (jsonObject.has("language")) {
                jsonObject.get("language").asString
            } else {
                "unknown"
            }

            // Составляем полный текст из сегментов/прямого поля
            var text=""
            if (jsonObject.has("segments")) {
                val segments = jsonObject.getAsJsonArray("segments")
                for (elem in segments) {
                    val segObj = elem.asJsonObject
                    if (segObj.has("text")) {
                        text += segObj.get("text").asString + " "
                    }
                }
            } else if (jsonObject.has("transcription")) {
                val transArr = jsonObject.getAsJsonArray("transcription")
                for (elem in transArr) {
                    val tObj = elem.asJsonObject
                    if (tObj.has("text")) {
                        text += tObj.get("text").asString + " "
                    }
                }
            } else if (jsonObject.has("text")) {
                text = jsonObject.get("text").asString
            }
            text = text.trim()

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
    /**
     * Ищет диапазон первого полноценного JSON‑объекта в строке.
     * Возвращает пару <начало, конец> индексов. Если не найден – (-1,-1).
     */
    private fun findJsonRange(input: String): Pair<Int, Int> {
        val start = input.indexOf('{')
        if (start == -1) return Pair(-1, -1)

        var depth = 0
        for (i in start until input.length) {
            when (input[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Pair(start, i)
                    }
                }
            }
        }
        return Pair(-1, -1)
    }

    /**
     * Логгер для парсера
     */
    private val logger = Logger.loggerFor("WhisperJsonParser")
}
