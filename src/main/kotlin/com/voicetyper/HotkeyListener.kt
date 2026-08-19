package com.voicetyper

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Временно отключён из-за проблем с JNA 5.15.0 и сигнатурами HHOOK/HHOOKPROC.
 * Код сохранён в закомментированном виде для последующего восстановления.
 */
class HotkeyListener : AutoCloseable {

    // ─── Полный код оставлен для последующего восстановления ───
    // (см. git diff или предыдущую версию)

    companion object {
        const val VK_F8 = 0xF7
        const val VK_F9 = 0xF8
        const val VK_F10 = 0xF9
        const val VK_F11 = 0xFA
        const val VK_F12 = 0xFB
        const val VK_ESCAPE = 0x1B
        const val VK_SPACE = 0x20
        const val VK_LCONTROL = 0xA2
        const val VK_RCONTROL = 0xA3
        const val VK_LSHIFT = 0xA0
        const val VK_RSHIFT = 0xA1
        const val VK_LMENU = 0xA4
        const val VK_RMENU = 0xA5
        const val VK_RETURN = 0x0D
        const val VK_TAB = 0x09
        const val VK_BACK = 0x08
    }

    private val logger = Logger.loggerFor("HotkeyListener")
    private val keyEvents = ConcurrentLinkedQueue<KeyEvent>()
    private val eventListeners = mutableListOf<(KeyEvent) -> Unit>()

    @Volatile
    private var isRunning = AtomicBoolean(false)

    // stub
    private var hookHandle: Any? = null

    private val keyMap = mapOf(
        "f8" to VK_F8, "f9" to VK_F9, "f10" to VK_F10, "f11" to VK_F11, "f12" to VK_F12,
        "escape" to VK_ESCAPE, "backspace" to VK_BACK, "tab" to VK_TAB, "enter" to VK_RETURN,
        "space" to VK_SPACE, "lcontrol" to VK_LCONTROL, "lshift" to VK_LSHIFT,
        "lmenu" to VK_LMENU, "rcontrol" to VK_RCONTROL, "rshift" to VK_RSHIFT, "rmenu" to VK_RMENU,
    )

    fun start() {
        logger.warn("HotkeyListener отключён — временно недоступен")
        isRunning.set(true)
    }

    fun onKeyPress(action: (KeyEvent) -> Unit) { eventListeners.add { event -> if (event.type == KeyEventType.PRESS) action(event) } }
    fun onKeyRelease(action: (KeyEvent) -> Unit) { eventListeners.add { event -> if (event.type == KeyEventType.RELEASE) action(event) } }
    fun getEventListeners(): List<(KeyEvent) -> Unit> = eventListeners

    fun registerForPress(release: Boolean, keyName: String, action: () -> Unit) {
        // stub
    }

    override fun close() {
        isRunning.set(false)
        eventListeners.clear()
    }

    private fun getKeyName(lParamLong: Long): String = ""

    private fun vkCodeToName(vk: Int): String = when (vk) {
        VK_F8 -> "f8"; VK_F9 -> "f9"; VK_F10 -> "f10"; VK_F11 -> "f11"; VK_F12 -> "f12"
        VK_ESCAPE -> "escape"; VK_SPACE -> "space"
        VK_LCONTROL, VK_RCONTROL -> "lcontrol"; VK_LSHIFT, VK_RSHIFT -> "lshift"
        VK_LMENU, VK_RMENU -> "lmenu"; VK_RETURN -> "enter"; VK_TAB -> "tab"; VK_BACK -> "backspace"
        else -> vk.toString()
    }

    data class KeyEvent(val type: KeyEventType, val key: String)
    enum class KeyEventType { PRESS, RELEASE }
}
