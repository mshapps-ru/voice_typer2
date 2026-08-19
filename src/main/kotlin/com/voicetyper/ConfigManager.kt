package com.voicetyper

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.nio.file.*

/**
 * Менеджер загрузки и сохранения конфигурации.
 */
class ConfigManager(private val configPath: Path) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val logger = Logger.loggerFor("ConfigManager")
    private var currentConfig: AppConfig = AppConfig()

    init {
        currentConfig = load()
    }

    /** Загружает конфигурацию из файла; если файл не найден — создаёт с дефолтными значениями */
    fun load(): AppConfig {
        if (Files.exists(configPath)) {
            try {
                val json = Files.readString(configPath)
                val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
                currentConfig = fromJsonMap(map)
                logger.info("Конфигурация загружена из: $configPath")
                return currentConfig
            } catch (e: Exception) {
                logger.error("Ошибка загрузки конфига, используются значения по умолчанию. ${e.message}")
                currentConfig = AppConfig()
            }
        } else {
            // Создаём директорию и дефолтный конфиг
            try {
                Files.createDirectories(configPath.parent)
                save(AppConfig())
                logger.info("Создан файл конфигурации по умолчанию: $configPath")
            } catch (e: Exception) {
                logger.error("Не удалось создать файл конфигурации: ${e.message}")
            }
        }
        return currentConfig
    }

    /** Сохраняет текущую конфигурацию */
    fun save(config: AppConfig) {
        try {
            Files.createDirectories(configPath.parent)
            val json = gson.toJson(config)
            Files.writeString(configPath, json)
            logger.info("Конфигурация сохранена: $configPath")
        } catch (e: Exception) {
            logger.error("Ошибка сохранения конфига: ${e.message}")
        }
    }

    /** Получает текущую конфигурацию */
    fun getConfig(): AppConfig = currentConfig

    /** Обновляет конфигурацию и сохраняет на диск */
    fun update(config: AppConfig) {
        currentConfig = config
        save(config)
    }

    /** Переводит Map из JSON в AppConfig */
    private fun fromJsonMap(map: Map<String, Any>): AppConfig {
        return AppConfig(
            showWindowHotkey = map["show_window_hotkey"] as? String ?: AppConfig().showWindowHotkey,
            recordKey = map["record_key"] as? String ?: AppConfig().recordKey,
            langToggleHotkey = map["lang_toggle_hotkey"] as? String ?: AppConfig().langToggleHotkey,
            sampleRate = (map["sample_rate"] as? Number)?.toInt() ?: AppConfig().sampleRate,
            channels = (map["channels"] as? Number)?.toInt() ?: AppConfig().channels,
            modelSize = map["model_size"] as? String ?: AppConfig().modelSize,
            device = map["device"] as? String ?: AppConfig().device,
            beamSize = (map["beam_size"] as? Number)?.toInt() ?: AppConfig().beamSize,
            language = map["language"] as? String ?: AppConfig().language,
            appLanguage = map["app_language"] as? String ?: AppConfig().appLanguage,
            initialPrompt = map["initial_prompt"] as? String ?: AppConfig().initialPrompt,
            windowSize = map["window_size"] as? String ?: AppConfig().windowSize
        )
    }
}
