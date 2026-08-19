package com.voicetyper

/**
 * Результат распознания текста от Whisper.
 */
data class TranscriptionResult(
    val text: String,
    val success: Boolean = true,
    val error: String? = null
) {
    companion object {
        val Empty = TranscriptionResult("", success = false)
        val NotRecognized = TranscriptionResult("", success = false)
    }
}