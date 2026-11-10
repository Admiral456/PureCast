package com.admiral456.purecast.playback

/**
 * Tracks repeated failures of the TV's status endpoints separately.
 * A failed status poll is not proof that the media stream itself stopped.
 */
data class PlaybackConnectionHealth(
    val transportFailures: Int = 0,
    val positionFailures: Int = 0
) {
    val isTransportUnresponsive: Boolean
        get() = transportFailures >= WARNING_THRESHOLD

    val isPositionUnavailable: Boolean
        get() = positionFailures >= WARNING_THRESHOLD

    val warningMessage: String?
        get() = when {
            isTransportUnresponsive ->
                "Televize opakovaně neodpovídá na stav přehrávání. PureCast přenos nepřerušuje a dál to zkouší; zkontroluj síť nebo obnov stav ručně."
            isPositionUnavailable ->
                "Televize nevrací časovou pozici videa. Samotný přenos může pokračovat; ovládání přehrávání zůstává dostupné."
            else -> null
        }

    companion object {
        const val WARNING_THRESHOLD = 2
        const val MAX_RECORDED_FAILURES = 3
    }
}

object PlaybackConnectionHealthPolicy {
    fun transportSucceeded(state: PlaybackConnectionHealth): PlaybackConnectionHealth =
        state.copy(transportFailures = 0)

    fun transportFailed(state: PlaybackConnectionHealth): PlaybackConnectionHealth =
        state.copy(
            transportFailures = (state.transportFailures + 1)
                .coerceAtMost(PlaybackConnectionHealth.MAX_RECORDED_FAILURES)
        )

    fun positionSucceeded(state: PlaybackConnectionHealth): PlaybackConnectionHealth =
        state.copy(positionFailures = 0)

    fun positionFailed(state: PlaybackConnectionHealth): PlaybackConnectionHealth =
        state.copy(
            positionFailures = (state.positionFailures + 1)
                .coerceAtMost(PlaybackConnectionHealth.MAX_RECORDED_FAILURES)
        )
}
