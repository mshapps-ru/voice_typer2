package com.voicetyper

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.GraphicsEnvironment
import javax.swing.*
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.pow

/**
 * Кастомный UI-интерфейс (аналог tkinter Canvas из Python-версии).
 * Отображается как плавающее окно без рамок.
 */
class UiCanvas(
    private val config: AppConfig,
    private val onRecordToggle: (Boolean) -> Unit,
    private val onLanguageToggle: () -> Unit,
    private val onSettings: () -> Unit,
    private val onAbout: () -> Unit,
    private val onExit: () -> Unit,
    private val onHide: () -> Unit,
    private val onStatusUpdate: (String, Boolean) -> Unit
) : JPanel() {

    companion object {
        private const val BG_COLOR_R = 45
        private const val BG_COLOR_G = 43
        private const val BG_COLOR_B = 58
        private val BG_COLOR = Color(BG_COLOR_R, BG_COLOR_G, BG_COLOR_B)

        private const val PANEL_COLOR_R = 58
        private const val PANEL_COLOR_G = 55
        private const val PANEL_COLOR_B = 76
        private val PANEL_COLOR = Color(PANEL_COLOR_R, PANEL_COLOR_G, PANEL_COLOR_B)

        private const val TEXT_COLOR_R = 226
        private const val TEXT_COLOR_G = 223
        private const val TEXT_COLOR_B = 233
        private val TEXT_COLOR = Color(TEXT_COLOR_R, TEXT_COLOR_G, TEXT_COLOR_B)

        private const val ACCENT_R = 142
        private const val ACCENT_G = 135
        private const val ACCENT_B = 164
        private val ACCENT_MUTED = Color(ACCENT_R, ACCENT_G, ACCENT_B)

        private const val ACCENT_ACTIVE_R = 161
        private const val ACCENT_ACTIVE_G = 118
        private const val ACCENT_ACTIVE_B = 255
        private val ACCENT_ACTIVE = Color(ACCENT_ACTIVE_R, ACCENT_ACTIVE_G, ACCENT_ACTIVE_B)

        private const val RECORD_COLOR_R = 255
        private const val RECORD_COLOR_G = 75
        private const val RECORD_COLOR_B = 75
        private val RECORD_COLOR = Color(RECORD_COLOR_R, RECORD_COLOR_G, RECORD_COLOR_B)
    }

    @Volatile
    private var isRecording = false

    @Volatile
    private var currentStatusText = Locales.get(config.appLanguage, "loading_whisper")

    @Volatile
    private var currentLanguage = config.language

    @Volatile
    var frame: JFrame? = null

    // Позиции для перетаскивания
    private var isDragging = false
    private var dragStartScreenX = 0
    private var dragStartScreenY = 0
    private var dragFrameOriginX = 0
    private var dragFrameOriginY = 0

    // Размеры
    var panelWidth: Int = 260
        private set
    var panelHeight: Int = 160
        private set

    init {
        background = BG_COLOR
        isOpaque = false
        preferredSize = Dimension(panelWidth, panelHeight)

        // Обработчики мыши для перетаскивания окна
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    val frame = SwingUtilities.getWindowAncestor(this@UiCanvas) ?: return
                    isDragging = true
                    dragStartScreenX = e.xOnScreen
                    dragStartScreenY = e.yOnScreen
                    dragFrameOriginX = frame.location.x
                    dragFrameOriginY = frame.location.y
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (!isDragging) return
                val dx = e.xOnScreen - dragStartScreenX
                val dy = e.yOnScreen - dragStartScreenY
                val frame = SwingUtilities.getWindowAncestor(this@UiCanvas) ?: return
                frame.location = Point(dragFrameOriginX + dx, dragFrameOriginY + dy)
            }
        })

        // Обработчик кликов для интерактивных элементов
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                val x = e.x
                val y = e.y
                
                // Кнопка закрытия (правый верхний угол, увеличенная область)
                if (x >= panelWidth - 25 && y <= 28) {
                    onHide()
                    return
                }
                
                // Глобус (левая часть) – переключение языка
                val globeCenterX = 45
                val globeCenterY = panelHeight / 2 + 2
                if ((x - globeCenterX).toDouble().pow(2) + (y - globeCenterY).toDouble().pow(2) < 30.0 * 30.0) {
                    onLanguageToggle()
                    return
                }
                
                // Шестерёнка (правая часть) – меню
                val gearCenterX = panelWidth - 45
                val gearCenterY = panelHeight / 2 + 2
                if ((x - gearCenterX).toDouble().pow(2) + (y - gearCenterY).toDouble().pow(2) < 30.0 * 30.0) {
                    showContextMenu(e.xOnScreen, e.yOnScreen)
                    return
                }
            }
        })
    }

    fun updateDimensions(newWidth: Int, newHeight: Int) {
        panelWidth = newWidth
        panelHeight = newHeight
        preferredSize = Dimension(panelWidth, panelHeight)
        minimumSize = Dimension(panelWidth, panelHeight)
        maximumSize = Dimension(panelWidth, panelHeight)
        revalidate()
        repaint()
        // Ensure frame resizes if dimensions changed
        this.frame?.pack()
    }

    // Update status when recording state changes (used by AppProcessor)
    fun setRecording(recording: Boolean) {
        isRecording = recording
        val status = if (recording) {
            Locales.get(config.appLanguage, "recording")
        } else {
            currentStatusText
        }
        onStatusUpdate(status, recording)
        repaint()
    }

    // Set a custom status text when not recording
    fun setStatus(text: String) {
        currentStatusText = text
        if (!isRecording) {
            onStatusUpdate(text, false)
            repaint()
        }
    }

    fun updateLanguage() {
        currentLanguage = config.language
        if (!isRecording) {
            setStatus(currentStatusText)
        }
        repaint()
    }

    /** Обновляет язык и перерисовывает UI */
    fun setLanguage(newLanguage: String) {
        currentLanguage = newLanguage
        if (!isRecording) {
            setStatus(currentStatusText)
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Основной фон
        val radius = if (panelWidth < 320) 15 else 20
        g2.color = BG_COLOR
        g2.fill(RoundRectangle2D.Double(0.0, 0.0, panelWidth.toDouble(), panelHeight.toDouble(), radius.toDouble(), radius.toDouble()))

        // Верхний декоративный элемент (капсула)
        val capW = if (panelWidth < 320) 20 else 25
        g2.color = PANEL_COLOR
        val capX = panelWidth / 2 - capW
        val capY = 10.0
        val capW2 = capW * 2.0
        val capH = 15.0
        g2.fill(RoundRectangle2D.Double(capX.toDouble(), capY, capW2.toDouble(), capH, 2.0, 2.0))

        // Кнопка закрытия (крестик)
        drawCloseButton(g2)

        // Глобус (слева)
        drawGlobe(g2)

        // Шестерёнка (справа)
        drawGear(g2)

        // Микрофон в центре
        drawMicrophone(g2)

        // Статус
        drawStatus(g2)

        g2.dispose()
    }

    private fun drawCloseButton(g2: Graphics2D) {
        val x = panelWidth - 20
        val y = 18
        g2.font = Font("Arial", Font.BOLD, 14)
        g2.color = ACCENT_MUTED
        val text = "✕"
        
        // Рисуем крестик линиями для надёжности
        val cx = x.toFloat()
        val cy = y.toFloat()
        val size = 6f
        g2.drawLine((cx - size).toInt(), (cy - size).toInt(), (cx + size).toInt(), (cy + size).toInt())
        g2.drawLine((cx - size).toInt(), (cy + size).toInt(), (cx + size).toInt(), (cy - size).toInt())
    }

    private fun drawGlobe(g2: Graphics2D) {
        val x = 45
        val y = panelHeight / 2 + 2
        val lang = currentLanguage.uppercase()

        // Глобус
        g2.font = Font("Segoe UI Symbol", Font.PLAIN, 16)
        g2.color = ACCENT_MUTED
        val globeText = "\uD83C\uDF10" // 🌐
        val globeBounds = g2.fontMetrics.getStringBounds(globeText, g2)
        g2.drawString(globeText, (x - globeBounds.width / 2).toFloat(), (y + globeBounds.height / 2).toFloat())

        // Текст языка
        g2.font = Font("Arial", Font.BOLD, 8)
        g2.color = ACCENT_MUTED
        val langBounds = g2.fontMetrics.getStringBounds(lang, g2)
        g2.drawString(lang, (x - langBounds.width / 2).toFloat(), (y + 20 + langBounds.height / 2).toFloat())
    }

    private fun drawGear(g2: Graphics2D) {
        val x = panelWidth - 45
        val y = panelHeight / 2 + 2
        g2.font = Font("Segoe UI Symbol", Font.PLAIN, 18)
        g2.color = ACCENT_MUTED
        val gearText = "\u2699" // ⚙
        val gearBounds = g2.fontMetrics.getStringBounds(gearText, g2)
        g2.drawString(gearText, (x - gearBounds.width / 2).toFloat(), (y + gearBounds.height / 2).toFloat())
    }

    private fun drawMicrophone(g2: Graphics2D) {
        val cx = panelWidth / 2
        val cy = panelHeight / 2 + 5
        val rOuter = if (panelWidth < 320) 32 else 38

        // Внешний круг
        g2.color = if (isRecording) RECORD_COLOR else ACCENT_ACTIVE
        g2.draw(java.awt.geom.Ellipse2D.Double((cx - rOuter).toDouble(), (cy - rOuter).toDouble(), (rOuter * 2).toDouble(), (rOuter * 2).toDouble()))

        // Иконка микрофона
        val scale = if (panelWidth < 320) 1.0 else 1.2
        val wCapsule = (5 * scale).toInt()
        val hCapsuleTop = (11 * scale).toInt()
        val hCapsuleBot = (3 * scale).toInt()
        val rArc = (10 * scale).toInt()
        val hLeg = (14 * scale).toInt()
        val micColor = if (isRecording) RECORD_COLOR else ACCENT_ACTIVE

        g2.color = micColor

        // Капсула (тело микрофона)
        g2.fill(RoundRectangle2D.Double((cx - wCapsule).toDouble(), (cy - hCapsuleTop).toDouble(),
            (wCapsule * 2).toDouble(), (hCapsuleTop + hCapsuleBot).toDouble(), 5.0, 5.0))

        // Дуга
        g2.draw(Arc2D.Double((cx - rArc).toDouble(), (cy - 5).toDouble(),
            (rArc * 2).toDouble(), (rArc + 5).toDouble(), 180.0, 180.0, Arc2D.PIE))

        // Ножка
        g2.drawLine(cx, cy + rArc - 1, cx, cy + hLeg)
    }

    private fun drawStatus(g2: Graphics2D) {
        val cx = panelWidth / 2
        val fontSize = if (panelWidth < 320) 7 else 8
        g2.font = Font("Arial", Font.BOLD, fontSize)
        g2.color = if (isRecording) RECORD_COLOR else ACCENT_MUTED
        val bounds = g2.fontMetrics.getStringBounds(currentStatusText, g2)
        val textX = cx - bounds.width / 2
        val textY = panelHeight - 18 + bounds.height / 2
        g2.drawString(currentStatusText, textX.toFloat(), textY.toFloat())
    }

    /**
     * Обновляет состояние при изменении языка
     */
    fun refreshLanguage() {
        currentLanguage = config.language
        if (!isRecording) {
            setStatus(currentStatusText)
        }
        repaint()
    }

    /**
     * Показывает контекстное меню при клике на шестерёнку
     */
    private fun showContextMenu(screenX: Int, screenY: Int) {
        val popupMenu = JPopupMenu()
        
        // Пункт "Настройки"
        val settingsItem = JMenuItem("Настройки")
        settingsItem.addActionListener {
            onSettings()
        }
        popupMenu.add(settingsItem)
        
        // Пункт "О программе"
        val aboutItem = JMenuItem("О программе")
        aboutItem.addActionListener {
            onAbout()
        }
        popupMenu.add(aboutItem)
        
        // Разделитель
        popupMenu.addSeparator()
        
        // Пункт "Выход"
        val exitItem = JMenuItem("Выход")
        exitItem.addActionListener {
            onExit()
        }
        popupMenu.add(exitItem)
        
        // Рассчитываем позицию меню относительно позиции клика на экране
        val frame = SwingUtilities.getWindowAncestor(this) ?: return
        val frameLocation = frame.locationOnScreen
        val localX = screenX - frameLocation.x
        val localY = screenY - frameLocation.y

        // Показываем меню чуть ниже и правее позиции клика
        popupMenu.show(this, localX + 5, localY + 5)
    }

    /**
     * Создаёт и показывает JFrame для этого UI, центрируя на экране
     */
    fun createFrame(): JFrame {
        val createdFrame = JFrame("Voice Typer").apply {
            isUndecorated = true
            isAlwaysOnTop = true
            background = BG_COLOR
            defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            isResizable = false
            add(this@UiCanvas)
            pack()
        }

        // Центрируем окно на экране
        val screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        val frameX = (screenSize.width - createdFrame.width) / 2
        val frameY = (screenSize.height - createdFrame.height) / 2
        createdFrame.setLocation(frameX, frameY)

        this.frame = createdFrame

        // Обработчик закрытия
        createdFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onHide()
            }
        })

        return createdFrame
    }
}