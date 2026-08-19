package com.voicetyper

import javax.swing.*
import java.awt.*

/**
 * Диалог настроек окна.
 */
class SettingsDialog(
    private val parent: JFrame?,
    private val configManager: ConfigManager,
    private val uiCanvas: UiCanvas
) {
    fun showDialog() {
        // Создаём модальный диалог со стандартными заголовками
        val dialog = JDialog(parent, Locales.get("ru", "settings_title"), true)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.isUndecorated = true
        dialog.layout = BorderLayout()
        dialog.contentPane.background = Color(45, 43, 58)

        // Кнопка закрытия (крестик "✕")
        val closeButton = JButton("x")
        closeButton.toolTipText = Locales.get("ru", "about_close")
        closeButton.preferredSize = Dimension(45, 45)
        closeButton.isContentAreaFilled = false
        closeButton.isBorderPainted = false
        closeButton.foreground = Color(142, 135, 164)
        closeButton.font = Font("Arial", Font.BOLD, 14)
        closeButton.addActionListener { dialog.dispose() }

        val topPanel = JPanel(BorderLayout())
        topPanel.background = Color(45, 43, 58)
        topPanel.add(closeButton, BorderLayout.EAST)
        dialog.add(topPanel, BorderLayout.NORTH)

        // Содержимое диалога
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.background = Color(45, 43, 58)

        val titleLabel = JLabel("Настройки")
        titleLabel.foreground = Color(226, 223, 233)
        titleLabel.font = Font("Arial", Font.BOLD, 14)
        titleLabel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(titleLabel)

        // ComboBox с размерами окна
        val options = arrayOf("260x160", "340x200", "420x240")
        val comboBox = JComboBox<String>(options)
        comboBox.selectedItem = configManager.getConfig().windowSize
        comboBox.maximumRowCount = 10
        comboBox.background = Color(58, 55, 76)
        comboBox.foreground = Color(226, 223, 233)
        comboBox.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        comboBox.preferredSize = Dimension(120, 20)
        comboBox.minimumSize = Dimension(120, 20)
        comboBox.maximumSize = Dimension(120, 40)
        comboBox.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(comboBox)

        // Кнопки Save / Cancel
        val buttonPanel = JPanel()
        buttonPanel.layout = FlowLayout(FlowLayout.CENTER, 5, 5)
        buttonPanel.background = Color(45, 43, 58)

        val btnBg = Color(58, 55, 76)
        val txtColor = Color(226, 223, 233)

        val saveButton = JButton("Сохранить")
        saveButton.background = btnBg
        saveButton.foreground = txtColor
        saveButton.isContentAreaFilled = false
        saveButton.isBorderPainted = false
        saveButton.addActionListener {
            val selected = comboBox.selectedItem as String
            val currentConfig = configManager.getConfig()
            val updated = currentConfig.copy(windowSize = selected)
            configManager.update(updated)
            val (w, h) = AppConfig.parseWindowSize(selected)
            uiCanvas.updateDimensions(w, h)
            // Ensure main frame resizes to new dimensions
            uiCanvas.frame?.pack()
            dialog.dispose()
        }

        val cancelButton = JButton("Отмена")
        cancelButton.background = btnBg
        cancelButton.foreground = txtColor
        cancelButton.isContentAreaFilled = false
        cancelButton.isBorderPainted = false
        cancelButton.addActionListener { dialog.dispose() }

        buttonPanel.add(saveButton)
        buttonPanel.add(cancelButton)

        mainPanel.add(buttonPanel)

        dialog.add(mainPanel, BorderLayout.CENTER)

        dialog.pack()
        // Увеличиваем размеры окна в 2 раза от исходного размера
        dialog.setSize(dialog.width * 2, dialog.height * 2)
        dialog.setLocationRelativeTo(parent)
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
    }
}
