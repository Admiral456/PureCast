package com.admiral456.purecast.server

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class TransferMetricsSnapshot(
    val activeTransfers: Int = 0,
    val peakConcurrentTransfers: Int = 0,
    val totalBytesSent: Long = 0L,
    val currentBytesPerSecond: Long = 0L,
    val completedTransfers: Long = 0L,
    val interruptedTransfers: Long = 0L
)

/**
 * Lightweight process-local accounting for media response bodies.
 *
 * The rate is calculated from bytes accepted by the server output stream over
 * a rolling window. It is not a measurement of Wi-Fi PHY rate or TV decode rate.
 */
internal class TransferMetrics(
    private val clockNanos: () -> Long = System::nanoTime,
    private val windowNanos: Long = DEFAULT_WINDOW_NANOS
) {
    private data class Sample(val timestampNanos: Long, val bytes: Long)

    private val activeTransfers = AtomicInteger(0)
    private val peakConcurrentTransfers = AtomicInteger(0)
    private val totalBytesSent = AtomicLong(0L)
    private val completedTransfers = AtomicLong(0L)
    private val interruptedTransfers = AtomicLong(0L)
    private val sampleLock = Any()
    private val recentSamples = ArrayDeque<Sample>()

    init {
        require(windowNanos > 0L) { "windowNanos must be positive" }
    }

    fun transferStarted() {
        val active = activeTransfers.incrementAndGet()
        peakConcurrentTransfers.updateAndGet { peak -> maxOf(peak, active) }
    }

    fun bytesSent(bytes: Long) {
        if (bytes <= 0L) return
        totalBytesSent.addAndGet(bytes)
        val now = clockNanos()
        synchronized(sampleLock) {
            pruneSamples(now)
            recentSamples.addLast(Sample(now, bytes))
        }
    }

    fun transferFinished(completed: Boolean) {
        activeTransfers.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
        if (completed) {
            completedTransfers.incrementAndGet()
        } else {
            interruptedTransfers.incrementAndGet()
        }
    }

    fun snapshot(): TransferMetricsSnapshot {
        val now = clockNanos()
        val currentRate = synchronized(sampleLock) {
            pruneSamples(now)
            if (recentSamples.isEmpty()) {
                0L
            } else {
                var bytes = 0L
                for (sample in recentSamples) bytes += sample.bytes
                val elapsed = (now - recentSamples.first().timestampNanos)
                    .coerceAtLeast(1L)
                    .coerceAtMost(windowNanos)
                (bytes.toDouble() * NANOS_PER_SECOND.toDouble() / elapsed.toDouble()).toLong().coerceAtLeast(0L)
            }
        }
        return TransferMetricsSnapshot(
            activeTransfers = activeTransfers.get(),
            peakConcurrentTransfers = peakConcurrentTransfers.get(),
            totalBytesSent = totalBytesSent.get(),
            currentBytesPerSecond = currentRate,
            completedTransfers = completedTransfers.get(),
            interruptedTransfers = interruptedTransfers.get()
        )
    }

    private fun pruneSamples(now: Long) {
        while (recentSamples.isNotEmpty()) {
            val age = now - recentSamples.first().timestampNanos
            if (age < windowNanos) break
            recentSamples.removeFirst()
        }
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val DEFAULT_WINDOW_NANOS = 5_000_000_000L
    }
}
