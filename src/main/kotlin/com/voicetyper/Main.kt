package com.voicetyper

import java.awt.EventQueue
import java.awt.Frame
import java.awt.SystemTray
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.JFrame

/**
 * Точка входа приложения.
 * Аналог main() из Python-версии.
 */
fun main() {
    // Устанавливаем UTF-8 для корректного вывода кириллицы
    System.setProperty("file.encoding", "UTF-8")
    
    // Перенаправляем stdout/stderr в UTF-8
    val utf8Out = java.io.PrintStream(System.out, false, StandardCharsets.UTF_8)
    val utf8Err = java.io.PrintStream(System.err, false, StandardCharsets.UTF_8)
    System.setOut(utf8Out)
    System.setErr(utf8Err)
    EventQueue.invokeAndWait {
        runVoiceTyper()
    }
}

/**
 * Основная логика запуска приложения.
 */
fun runVoiceTyper() {
    val logger = Logger.loggerFor("Main")
    logger.info("=== Voice Typer v1.0.0 (Kotlin) ===")
    
    // Загрузка конфигурации
    val configDir = File(System.getProperty("user.home"), ".config/voice-typer")
    val configPath = File(configDir, "config.json")
    configDir.mkdirs()
    
    val configManager = ConfigManager(configPath.toPath())
    val config = configManager.load()
    
    // Инициализация компонентов
    val audioRecorder = AudioRecorder(config)
    val whisperEngine = WhisperEngine(config)
    val clipboardManager = ClipboardManager()
    
    // Создание процессора
    lateinit var processor: AppProcessor
    lateinit var uiCanvas: UiCanvas
    
    // UI — создаём frame отдельно для референсов
    uiCanvas = UiCanvas(
        config = config,
        onRecordToggle = {},
        onLanguageToggle = { processor.toggleLanguage() },
        onSettings = { logger.info("Настройки открыты") },
        onAbout = { logger.info("О программе") },
        onExit = { processor.shutdown(); System.exit(0) },
        onHide = { uiCanvas.frame?.isVisible = false },
        onStatusUpdate = { status, _ -> logger.info("Статус: $status") }
    )
    
    // Системный трей — callbacks с frame из uiCanvas
    val trayManager = if (SystemTray.isSupported()) {
        TrayIconManager(
            config = config,
            restoreCallback = { uiCanvas.frame?.isVisible = true },
            exitCallback = { processor.shutdown(); System.exit(0) }
        )
    } else {
        logger.warn("SystemTray не поддерживается на этой платформе")
        TrayIconManager(config, {}, {})
    }
    
    // Горячие клавиши
    val hotkeyListener = HotkeyListener()
    
    processor = AppProcessor(
        configManager = configManager,
        audioRecorder = audioRecorder,
        whisperEngine = whisperEngine,
        clipboardManager = clipboardManager,
        uiCanvas = uiCanvas,
        trayManager = trayManager,
        hotkeyListener = hotkeyListener
    )
    
    // Создание и показ UI
    val frame = uiCanvas.createFrame()
    frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
    frame.addWindowListener(object : java.awt.event.WindowAdapter() {
        override fun windowClosing(e: java.awt.event.WindowEvent?) {
            processor.shutdown()
            Runtime.getRuntime().exit(0)
        }
        
        override fun windowOpened(e: java.awt.event.WindowEvent?) {
            // Первичная настройка после открытия окна
            val (w, h) = AppConfig.parseWindowSize(config.windowSize)
            uiCanvas.updateDimensions(w, h)
        }
    })
    
    // Сохраняем ссылку на frame в UiCanvas для возможности скрытия/показа
    // (можно добавить сеттер в UiCanvas)
    
    // Обработка сигналов прерывания
    Runtime.getRuntime().addShutdownHook(Thread {
        processor.shutdown()
    })
    
    // Показ окна
    frame.isVisible = true
    
    logger.info("Voice Typer запущен и готов к работе")
}

/**
 * Безопасный выход из приложения.
 */
private fun exitApplication() {
    System.exit(0)
}
