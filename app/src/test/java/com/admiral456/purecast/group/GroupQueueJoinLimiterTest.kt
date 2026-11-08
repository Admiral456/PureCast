package com.admiral456.purecast.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupQueueJoinLimiterTest {
    @Test
    fun blocksAfterConfiguredNumberOfFailures() {
        var now = 1_000L
        val limiter = GroupQueueJoinLimiter(maxAttempts = 3, clock = { now })

        repeat(3) { limiter.recordFailure("192.168.1.10") }

        assertFalse(limiter.canAttempt("192.168.1.10"))
        assertTrue(limiter.canAttempt("192.168.1.11"))
    }

    @Test
    fun expiresAttemptWindowAfterConfiguredDuration() {
        var now = 1_000L
        val limiter = GroupQueueJoinLimiter(
            maxAttempts = 2,
            windowMillis = 60_000L,
            clock = { now }
        )
        repeat(2) { limiter.recordFailure("192.168.1.10") }
        assertFalse(limiter.canAttempt("192.168.1.10"))

        now += 60_001L

        assertTrue(limiter.canAttempt("192.168.1.10"))
    }

    @Test
    fun successfulJoinClearsPreviousFailures() {
        var now = 1_000L
        val limiter = GroupQueueJoinLimiter(maxAttempts = 2, clock = { now })
        limiter.recordFailure("192.168.1.10")
        limiter.recordFailure("192.168.1.10")
        assertFalse(limiter.canAttempt("192.168.1.10"))

        limiter.recordSuccess("192.168.1.10")

        assertTrue(limiter.canAttempt("192.168.1.10"))
    }

    @Test
    fun evictsOldestRemoteWhenTrackingCapacityIsReached() {
        var now = 1_000L
        val limiter = GroupQueueJoinLimiter(
            maxAttempts = 1,
            maxTrackedRemotes = 2,
            clock = { now }
        )
        limiter.recordFailure("192.168.1.10")
        now++
        limiter.recordFailure("192.168.1.11")
        now++
        limiter.recordFailure("192.168.1.12")

        assertEquals(true, limiter.canAttempt("192.168.1.10"))
        assertFalse(limiter.canAttempt("192.168.1.11"))
        assertFalse(limiter.canAttempt("192.168.1.12"))
    }

    @Test
    fun expiredEntriesArePrunedDuringNewAttempts() {
        var now = 1_000L
        val limiter = GroupQueueJoinLimiter(
            maxAttempts = 1,
            windowMillis = 10L,
            maxTrackedRemotes = 1,
            clock = { now }
        )
        limiter.recordFailure("192.168.1.10")
        now += 11L

        limiter.recordFailure("192.168.1.11")

        assertTrue(limiter.canAttempt("192.168.1.10"))
        assertFalse(limiter.canAttempt("192.168.1.11"))
    }
}
