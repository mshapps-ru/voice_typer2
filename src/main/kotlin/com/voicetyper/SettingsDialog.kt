package com.voicetyper

import javax.swing.*
import java.awt.*

/**
 * Диалог настроек окна.
 */
import com.voicetyper.Logger
import com.voicetyper.WhisperEngine

class SettingsDialog(
    private val parent: JFrame?,
    private val configManager: ConfigManager,
    private val uiCanvas: UiCanvas
) {
    private val logger = Logger.loggerFor("SettingsDialog")
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
        closeButton.isFocusPainted = false
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

        // Label for database name
        val dbLabel = JLabel("База whisper.cpp")
        dbLabel.foreground = Color(226, 223, 233)
        dbLabel.font = Font("Arial", Font.PLAIN, 14)
        dbLabel.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(dbLabel)

        // ComboBox с выбором модели Whisper (увеличенная высота)
        val modelOptions = arrayOf("tiny", "small", "medium", "base")
        val modelComboBox = JComboBox<String>(modelOptions)
        modelComboBox.selectedItem = configManager.getConfig().modelSize
        modelComboBox.maximumRowCount = 4
        modelComboBox.background = Color(58, 55, 76)
        modelComboBox.foreground = Color(226, 223, 233)
        modelComboBox.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        // increase height by 2x
        modelComboBox.preferredSize = Dimension(120, 40)
        modelComboBox.minimumSize = Dimension(120, 40)
        modelComboBox.maximumSize = Dimension(120, 80)
        modelComboBox.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(modelComboBox)

        // Label for main page size
        val dbLabel2 = JLabel("Размер окна приложения")
        dbLabel2.foreground = Color(226, 223, 233)
        dbLabel2.font = Font("Arial", Font.PLAIN, 14)
        dbLabel2.alignmentX = Component.CENTER_ALIGNMENT
        mainPanel.add(dbLabel2)

        // ComboBox с размерами окна (увеличенная высота)
        val options = arrayOf("260x160", "340x200", "420x240")
        val comboBox = JComboBox<String>(options)
        comboBox.selectedItem = configManager.getConfig().windowSize
        comboBox.maximumRowCount = 10
        comboBox.background = Color(58, 55, 76)
        comboBox.foreground = Color(226, 223, 233)
        comboBox.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
        comboBox.preferredSize = Dimension(120, 40)
        comboBox.minimumSize = Dimension(120, 40)
        comboBox.maximumSize = Dimension(120, 80)
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
            val selectedModel = modelComboBox.selectedItem as String
            val selectedWindow = comboBox.selectedItem as String
            val currentConfig = configManager.getConfig()
            val updated = currentConfig.copy(windowSize = selectedWindow, modelSize = selectedModel)
            configManager.update(updated)
            // Verify and download model if needed
            try {
                val tempEngine = WhisperEngine(updated)
                if (!tempEngine.ensureModelAvailable()) {
                    logger.warn("Failed to download whisper model ${updated.modelSize}")
                }
            } catch (e: Exception) {
                logger.error("Error checking model availability: ${e.message}")
            }
            val (w, h) = AppConfig.parseWindowSize(selectedWindow)
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
        dialog.setSize(dialog.width * 2, dialog.height + dialog.height / 2 )
        dialog.setLocationRelativeTo(parent)
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
    }
}
