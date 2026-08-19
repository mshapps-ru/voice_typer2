package com.voicetyper

/**
 * Локализация приложения (RU и EN).
 * Совпадает с оригинальным Python-проектом.
 */
object Locales {

    private val strings = mapOf(
        "ru" to mapOf(
            "loading_whisper" to "ЗАГРУЗКА WHISPER...",
            "ready_to_work" to "ГОТОВ К РАБОТЕ",
            "recording" to "ЗАПИСЬ...",
            "processing" to "ОБРАБОТКА...",
            "empty_signal" to "ПУСТОЙ СИГНАЛ",
            "ready" to "ГОТОВ",
            "not_recognized" to "НЕ РАСПОЗНАНО",
            "error" to "ОШИБКА",
            "language_status" to "ЯЗЫК: {lang}",
            "tray_restore" to "Развернуть",
            "tray_exit" to "Выход",
            "menu_about" to "О программе",
            "menu_settings" to "Настройки",
            "menu_exit" to "Закрыть программу",
            "about_desc" to "Голосовой ввод и распознавание в текст",
            "about_close" to "Закрыть",
            "settings_title" to "Размер окна программы:",
            "settings_app_lang" to "Язык приложения:",
            "settings_save" to "Сохранить",
            "settings_cancel" to "Отмена",
            "exit_confirm" to "Вы точно желаете закрыть\nпрограмму?",
            "exit_yes" to "Да",
            "exit_no" to "Нет",
            "about_title" to "Voice Typer",
        ),
        "en" to mapOf(
            "loading_whisper" to "LOADING WHISPER...",
            "ready_to_work" to "READY TO WORK",
            "recording" to "RECORDING...",
            "processing" to "PROCESSING...",
            "empty_signal" to "EMPTY SIGNAL",
            "ready" to "READY",
            "not_recognized" to "NOT RECOGNIZED",
            "error" to "ERROR",
            "language_status" to "LANG: {lang}",
            "tray_restore" to "Restore",
            "tray_exit" to "Exit",
            "menu_about" to "About",
            "menu_settings" to "Settings",
            "menu_exit" to "Exit program",
            "about_desc" to "Voice typing and transcription",
            "about_close" to "Close",
            "settings_title" to "Window size:",
            "settings_app_lang" to "Application language:",
            "settings_save" to "Save",
            "settings_cancel" to "Cancel",
            "exit_confirm" to "Are you sure you want to close\nthe program?",
            "exit_yes" to "Yes",
            "exit_no" to "No",
            "about_title" to "Voice Typer",
        )
    )

    fun get(appLanguage: String, key: String, vararg args: Any): String {
        val langMap = strings[appLanguage] ?: strings["ru"]!!
        val text = langMap[key] ?: key
        return if (args.isNotEmpty()) String.format(text, *args) else text
    }
}