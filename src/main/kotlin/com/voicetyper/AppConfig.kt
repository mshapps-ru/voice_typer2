package com.voicetyper

/**
 * Модель данных конфигурации приложения.
 * Все значения по умолчанию совпадают с оригинальным Python-проектом.
 */
data class AppConfig(
    // Горячие клавиши
    val showWindowHotkey: String = "f8",
    val recordKey: String = "f9",
    val langToggleHotkey: String = "f10",

    // Параметры аудио
    val sampleRate: Int = 16000,
    val channels: Int = 1,

    // Параметры Whisper
    val modelSize: String = "base",
    val device: String = "cpu",
    val beamSize: Int = 5,

    // Язык
    var language: String = "",
    val appLanguage: String = "ru",

    // Параметры ввода
    val initialPrompt: String = "",

    // Окно
    val windowSize: String = "260x160"
) {
    companion object {
        /** Разбирает строку разрешения окна (например, "320x200") */
        fun parseWindowSize(sizeStr: String): Pair<Int, Int> {
            return try {
                val parts = sizeStr.split("x")
                parts[0].toInt() to parts[1].toInt()
            } catch (e: Exception) {
                260 to 160
            }
        }
    }
}
