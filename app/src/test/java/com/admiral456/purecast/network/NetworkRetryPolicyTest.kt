package com.admiral456.purecast.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRetryPolicyTest {
    @Test
    fun retryDelayUsesBoundedIncreasingBackoff() {
        assertEquals(250L, NetworkRetryPolicy.delayMillis(1))
        assertEquals(750L, NetworkRetryPolicy.delayMillis(2))
        assertEquals(1_500L, NetworkRetryPolicy.delayMillis(3))
        assertEquals(1_500L, NetworkRetryPolicy.delayMillis(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun retryAttemptMustBeOneBased() {
        NetworkRetryPolicy.delayMillis(0)
    }

    @Test
    fun onlyTransientHttpStatusesAreRetried() {
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(408))
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(425))
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(429))
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(500))
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(503))
        assertTrue(NetworkRetryPolicy.isRetryableHttpStatus(599))

        assertFalse(NetworkRetryPolicy.isRetryableHttpStatus(200))
        assertFalse(NetworkRetryPolicy.isRetryableHttpStatus(400))
        assertFalse(NetworkRetryPolicy.isRetryableHttpStatus(404))
    }
}
