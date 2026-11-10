package com.admiral456.purecast.server

/**
 * Bounded retry schedule for recoverable network errors in a TV browser player.
 * Codec and decode errors are not network errors and are never retried.
 */
object BrowserPlaybackRecoveryPolicy {
    const val MAX_RETRIES = 3

    fun delayMillis(retryNumber: Int): Long {
        require(retryNumber in 1..MAX_RETRIES) {
            "Retry number must be between 1 and $MAX_RETRIES."
        }
        return when (retryNumber) {
            1 -> 500L
            2 -> 1_000L
            else -> 2_000L
        }
    }
}
