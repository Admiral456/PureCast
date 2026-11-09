package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferMetricsTest {
    @Test
    fun calculatesRollingSendRateAndTotalBytes() {
        var now = 1_000_000_000L
        val metrics = TransferMetrics(clockNanos = { now })

        metrics.transferStarted()
        metrics.bytesSent(1_000_000L)
        now += 2_000_000_000L
        metrics.bytesSent(2_000_000L)

        val snapshot = metrics.snapshot()

        assertEquals(3_000_000L, snapshot.totalBytesSent)
        assertEquals(1_500_000L, snapshot.currentBytesPerSecond)
        assertEquals(1, snapshot.activeTransfers)
    }

    @Test
    fun rateFallsToZeroAfterTheRollingWindowExpires() {
        var now = 5_000_000_000L
        val metrics = TransferMetrics(clockNanos = { now })

        metrics.transferStarted()
        metrics.bytesSent(8_000_000L)
        assertTrue(metrics.snapshot().currentBytesPerSecond > 0L)

        now += 5_000_000_001L

        assertEquals(0L, metrics.snapshot().currentBytesPerSecond)
        assertEquals(8_000_000L, metrics.snapshot().totalBytesSent)
    }

    @Test
    fun rollingRateDropsOldSamplesButKeepsLifetimeTotal() {
        var now = 10_000_000_000L
        val metrics = TransferMetrics(clockNanos = { now })

        metrics.bytesSent(10_000_000L)
        now += 4_000_000_000L
        metrics.bytesSent(1_000_000L)
        now += 2_000_000_000L

        val snapshot = metrics.snapshot()

        assertEquals(11_000_000L, snapshot.totalBytesSent)
        assertEquals(500_000L, snapshot.currentBytesPerSecond)
    }

    @Test
    fun tracksPeakConcurrentTransfersAndCompletionCounts() {
        val metrics = TransferMetrics()

        metrics.transferStarted()
        metrics.transferStarted()
        metrics.transferFinished(completed = true)
        metrics.transferStarted()
        metrics.transferFinished(completed = false)

        val snapshot = metrics.snapshot()

        assertEquals(1, snapshot.activeTransfers)
        assertEquals(2, snapshot.peakConcurrentTransfers)
        assertEquals(1L, snapshot.completedTransfers)
        assertEquals(1L, snapshot.interruptedTransfers)
    }

    @Test
    fun ignoresNonPositiveByteCounts() {
        val metrics = TransferMetrics()
        metrics.bytesSent(0L)
        metrics.bytesSent(-10L)

        val snapshot = metrics.snapshot()

        assertEquals(0L, snapshot.totalBytesSent)
        assertEquals(0L, snapshot.currentBytesPerSecond)
    }

    @Test
    fun transferFinishNeverLeavesNegativeActiveCount() {
        val metrics = TransferMetrics()

        metrics.transferFinished(completed = false)

        assertEquals(0, metrics.snapshot().activeTransfers)
        assertEquals(1L, metrics.snapshot().interruptedTransfers)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnInvalidRateWindow() {
        TransferMetrics(windowNanos = 0L)
    }
}
