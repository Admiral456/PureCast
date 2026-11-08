package com.admiral456.purecast.group

/**
 * Bounds invite-code guessing attempts and memory use on the LAN-only group server.
 * The clock is injectable so expiry and eviction behavior can be unit tested.
 */
internal class GroupQueueJoinLimiter(
    private val maxAttempts: Int = 10,
    private val windowMillis: Long = 60_000L,
    private val maxTrackedRemotes: Int = 256,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class AttemptWindow(val startedAt: Long, val count: Int)

    private val attempts = linkedMapOf<String, AttemptWindow>()

    init {
        require(maxAttempts > 0)
        require(windowMillis > 0L)
        require(maxTrackedRemotes > 0)
    }

    @Synchronized
    fun canAttempt(remote: String): Boolean {
        pruneExpired(clock())
        val attempt = attempts[remote] ?: return true
        return attempt.count < maxAttempts
    }

    @Synchronized
    fun recordFailure(remote: String) {
        val now = clock()
        pruneExpired(now)
        val previous = attempts[remote]
        if (previous == null) {
            if (attempts.size >= maxTrackedRemotes) {
                val oldest = attempts.entries.minByOrNull { it.value.startedAt }?.key
                if (oldest != null) attempts.remove(oldest)
            }
            attempts[remote] = AttemptWindow(now, 1)
        } else {
            attempts[remote] = previous.copy(count = previous.count + 1)
        }
    }

    @Synchronized
    fun recordSuccess(remote: String) {
        attempts.remove(remote)
    }

    @Synchronized
    private fun pruneExpired(now: Long) {
        attempts.entries.removeAll { now - it.value.startedAt > windowMillis }
    }
}
