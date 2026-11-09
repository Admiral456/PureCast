package com.admiral456.purecast.playback

/**
 * Keeps DLNA transport controls consistent with the last state reported by the TV.
 * Unknown states remain actionable because some renderers do not report transport state reliably.
 */
object PlaybackControlAvailability {
    fun canPause(state: String?): Boolean =
        state !in setOf("PAUSED_PLAYBACK", "STOPPED", "NO_MEDIA_PRESENT")

    fun canResume(state: String?): Boolean =
        state !in setOf("PLAYING", "TRANSITIONING", "STOPPED", "NO_MEDIA_PRESENT")

    fun canStop(state: String?): Boolean =
        state !in setOf("STOPPED", "NO_MEDIA_PRESENT")
}
