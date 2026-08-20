package com.voicetyper

import com.sun.jna.platform.win32.User32
import java.util.concurrent.ConcurrentHashMap

/**
 * Мини‑реализация «глобальных» горячих клавиш.
 * Проверяет состояние клавиатуры через WinAPI GetAsyncKeyState и вызывает назначенные действия при нажатии и отпускании.
 */
class HotkeyListener : AutoCloseable {
    private val user32 = User32.INSTANCE
    data class KeyActions(var press: (() -> Unit)? = null, var release: (() -> Unit)? = null)
    private val actions: MutableMap<Int, KeyActions> = ConcurrentHashMap()
    private val prevState: MutableMap<Int, Boolean> = ConcurrentHashMap()
    private var running = true
    private val thread = Thread { loop() }.apply { isDaemon = true; start() }

    /**
     * Регистрирует действие при нажатии (release=false) или отпускании (release=true) клавиши.
     */
    fun registerForPress(release: Boolean, keyName: String, action: () -> Unit) {
        val vk = KeyMap[keyName.lowercase()] ?: return
        val entry = actions.getOrPut(vk) { KeyActions() }
        if (release) entry.release = action else entry.press = action
    }

    override fun close() { running = false }

    private fun loop() {
        while (running) {
            for ((vk, keyAction) in actions) {
                val pressed = user32.GetAsyncKeyState(vk).toInt() != 0
                val wasPressed = prevState[vk] ?: false
                if (!wasPressed && pressed) keyAction.press?.invoke()
                if (wasPressed && !pressed) keyAction.release?.invoke()
                prevState[vk] = pressed
            }
            Thread.sleep(50)
        }
    }

    companion object {
        private val KeyMap = mapOf(
            "f8" to 0x77,
            "f9" to 0x78,
            "f10" to 0x79,
            "f11" to 0x7A,
            "f12" to 0x7B,
            "escape" to 0x1B,
            "backspace" to 0x08,
            "tab" to 0x09,
            "enter" to 0x0D,
            "space" to 0x20,
            "lcontrol" to 0xA2,
            "rcontrol" to 0xA3,
            "lshift" to 0xA0,
            "rshift" to 0xA1,
            "lmenu" to 0x12,
            "rmenu" to 0x12
        )
    }
}
