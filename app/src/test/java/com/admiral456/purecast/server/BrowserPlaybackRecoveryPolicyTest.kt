package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserPlaybackRecoveryPolicyTest {
    @Test
    fun retryDelaysIncreaseAndRemainBounded() {
        assertEquals(500L, BrowserPlaybackRecoveryPolicy.delayMillis(1))
        assertEquals(1_000L, BrowserPlaybackRecoveryPolicy.delayMillis(2))
        assertEquals(2_000L, BrowserPlaybackRecoveryPolicy.delayMillis(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun retryNumberCannotBeZero() {
        BrowserPlaybackRecoveryPolicy.delayMillis(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun retryNumberCannotExceedLimit() {
        BrowserPlaybackRecoveryPolicy.delayMillis(4)
    }
}
