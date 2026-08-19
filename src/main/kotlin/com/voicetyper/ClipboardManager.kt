package com.voicetyper

import java.awt.datatransfer.DataFlavor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable

/**
 * Менеджер вставки текста.
 * Копирует текст в буфер обмена и вставляет через Ctrl+V.
 */
class ClipboardManager {

    private val logger = Logger.loggerFor("ClipboardManager")
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    /**
     * Вставляет текст в активное приложение.
     * Использует clipboard + Ctrl+V (аналог pyautogui.hotkey('ctrl', 'v')).
     */
    fun pasteText(text: String) {
        if (text.isEmpty()) return

        try {
            // Сохраняем старый контент
            var oldContent = ""
            try {
                val current = clipboard.getContents(null)
                if (current != null) {
                    val flavors = current.transferDataFlavors
                    if (flavors.any { it == DataFlavor.stringFlavor }) {
                        oldContent = current.getTransferData(DataFlavor.stringFlavor) as String
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибку чтения старого контента
            }

            // Копируем новый текст
            val stringSelection = StringSelection(text)
            clipboard.setContents(stringSelection, null)

            // Небольшая задержка для стабильности
            Thread.sleep(50)

            // Вставляем через Robot (Ctrl+V)
            val robot = java.awt.Robot()
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            robot.keyPress(java.awt.event.KeyEvent.VK_V)
            robot.keyRelease(java.awt.event.KeyEvent.VK_V)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            // Ждём завершения вставки
            Thread.sleep(100)

            // Восстанавливаем старый контент (необязательно, но полезно)
            try {
                if (oldContent.isNotEmpty()) {
                    clipboard.setContents(StringSelection(oldContent), null)
                }
            } catch (e: Exception) {
                logger.debug("Не удалось восстановить старый контент буфера: ${e.message}")
            }

        } catch (e: Exception) {
            logger.error("Ошибка вставки текста: ${e.message}")
        }
    }
}
