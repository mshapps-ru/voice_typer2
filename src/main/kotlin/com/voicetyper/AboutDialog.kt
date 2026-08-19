package com.voicetyper

import javax.swing.*
import java.awt.*

/**
 * Окно «О программе».
 */
class AboutDialog(private val parent: JFrame?) {
    fun showDialog() {
        // Создаём модальный диалог, используя русский язык как в примере
        val dialog = JDialog(parent, Locales.get("ru", "about_title"), true)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        // Отключаем системный заголовок и задаём стиль
        dialog.isUndecorated = true
        dialog.layout = BorderLayout()
        // Set content pane background to match main window
        dialog.contentPane.background = Color(45, 43, 58)
        // Текст описания
        val descriptionLabel = JLabel("Voice typer - голосовой ввод и распознавание в текст")
        descriptionLabel.horizontalAlignment = SwingConstants.CENTER
        descriptionLabel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        // Set text color to match main window's text color
        descriptionLabel.foreground = Color(226, 223, 233)
        dialog.add(descriptionLabel, BorderLayout.CENTER)

        // Кнопка закрытия в правом верхнем углу (крестик)
        val closeButton = JButton("x")
        closeButton.toolTipText = Locales.get("ru", "about_close")
        closeButton.preferredSize = Dimension(45, 45)
        closeButton.isContentAreaFilled = false
        closeButton.isBorderPainted = false
        closeButton.isFocusPainted = false
        // Use same accent muted color as main window close icon
        closeButton.foreground = Color(142,135,164)
        closeButton.font = Font("Arial", Font.BOLD, 16)
        closeButton.addActionListener { dialog.dispose() }

        // Панель для кнопки закрытия (с правым выравниванием)
        val topPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        topPanel.background = Color(45,43,58)
        topPanel.add(closeButton)
        dialog.add(topPanel, BorderLayout.NORTH)

        dialog.pack()
        dialog.setLocationRelativeTo(parent)
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
    }
}
