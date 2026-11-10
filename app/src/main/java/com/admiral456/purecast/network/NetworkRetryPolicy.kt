package com.admiral456.purecast.network

/**
 * Deterministic retry policy for transient local-network failures.
 * Retries are bounded so an offline TV still produces a visible error.
 */
object NetworkRetryPolicy {
    fun delayMillis(failedAttempt: Int): Long {
        require(failedAttempt >= 1) { "Attempt count must be one-based." }
        return when (failedAttempt) {
            1 -> 250L
            2 -> 750L
            else -> 1_500L
        }
    }

    fun isRetryableHttpStatus(statusCode: Int): Boolean =
        statusCode == 408 ||
            statusCode == 425 ||
            statusCode == 429 ||
            statusCode in 500..599
}
