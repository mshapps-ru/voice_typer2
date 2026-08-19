package com.voicetyper

import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser.*
import java.awt.*
import java.awt.image.BufferedImage

/**
 * Менеджер системного трея.
 * Отображает иконку голосового микрофона в области уведомлений.
 */
class TrayIconManager(
    private val config: AppConfig,
    private val restoreCallback: () -> Unit,
    private val exitCallback: () -> Unit
) {

    private val logger = Logger.loggerFor("TrayIconManager")

    @Volatile
    private var trayIcon: TrayIcon? = null

    init {
        if (SystemTray.isSupported()) {
            try {
                val tray = SystemTray.getSystemTray()
                val icon = createMicrophoneIcon(color = Color(161, 118, 255))
                val tooltip = "Voice Typer (Whisper)"

                val menu = createMenu()
                trayIcon = TrayIcon(icon, tooltip, menu)
                trayIcon?.isImageAutoSize = true

                tray.add(trayIcon!!)
                logger.info("Иконка в системном трее добавлена")
            } catch (e: Exception) {
                logger.error("Ошибка инициализации системного трея: ${e.message}")
            }
        } else {
            logger.warn("SystemTray не поддерживается на этой платформе")
        }
    }

    /** Создаёт иконку микрофона */
    private fun createMicrophoneIcon(color: Color): BufferedImage {
        val size = 64
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Микрофон (капсула + дуга + ножки)
        val pad = 10
        g2d.color = color

        // Капсула (тело микрофона)
        val capsuleRect = Rectangle(size / 2 - 6, pad + 4, 12, 20)
        g2d.fillRoundRect(capsuleRect.x, capsuleRect.y, capsuleRect.width, capsuleRect.height, 6, 6)

        // Дуга (верхушка)
        g2d.drawArc(size / 2 - 14, pad + 10, 28, 26, 0, 180)

        // Ножки
        g2d.drawLine(size / 2, pad + 34, size / 2, size - pad)
        g2d.drawLine(size / 2 - 12, size - pad, size / 2 + 12, size - pad)

        g2d.dispose()
        return image
    }

    /** Обновляет иконку (например, при записи — жёлтая подсветка) */
    fun updateIcon(recording: Boolean) {
        if (trayIcon == null) return

        val newIcon = createMicrophoneIcon(
            color = if (recording) Color(255, 215, 0) else Color(161, 118, 255)
        )
        trayIcon?.image = newIcon
    }

    private fun createMenu(): PopupMenu {
        return PopupMenu().apply {
            // Для русской локализации используем транслит, т.к. PopupMenu работает с локализованными строками
            val ruRestore = "Развернуть"
            val ruExit = "Выход"
            val enRestore = "Restore"
            val enExit = "Exit"

            val lang = config.appLanguage
            val labelRestore = if (lang == "en") enRestore else ruRestore
            val labelExit = if (lang == "en") enExit else ruExit

            add(MenuItem(labelRestore).apply { addActionListener { restoreCallback() } })
            add(MenuItem(labelExit).apply { addActionListener { exitCallback() } })
        }
    }

    /** Очистка ресурсов */
    fun shutdown() {
        try {
            trayIcon?.let { tray ->
                SystemTray.getSystemTray().remove(tray)
                logger.info("Иконка из системного трея удалена")
            }
        } catch (e: Exception) {
            logger.error("Ошибка удаления иконки из трея: ${e.message}")
        }
        trayIcon = null
    }
}