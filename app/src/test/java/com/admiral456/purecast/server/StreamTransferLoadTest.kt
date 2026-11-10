package com.admiral456.purecast.server

import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Synthetic sustained-load checks. These exercise large, concurrent copy loops
 * without allocating giant source/destination byte arrays or relying on CI timing.
 * Real Wi-Fi and TV throughput still requires device testing.
 */
class StreamTransferLoadTest {
    @Test
    fun concurrentLargeTransfersPreserveEveryByte() {
        val workerCount = 4
        val bytesPerWorker = 16L * 1024L * 1024L
        val executor = Executors.newFixedThreadPool(workerCount)
        try {
            val tasks = (0 until workerCount).map {
                Callable {
                    val output = CountingChecksumOutputStream()
                    StreamTransfer.copyExactly(
                        input = DeterministicMediaInputStream(bytesPerWorker),
                        output = output,
                        length = bytesPerWorker
                    )
                    TransferResult(output.byteCount, output.checksum)
                }
            }

            val results = executor.invokeAll(tasks).map { it.get() }

            assertEquals(workerCount, results.size)
            results.forEach { result ->
                assertEquals(bytesPerWorker, result.byteCount)
                assertEquals(results.first().checksum, result.checksum)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun unknownLengthTransferStreamsLargeMediaUntilEof() {
        val expectedBytes = 64L * 1024L * 1024L
        val output = CountingChecksumOutputStream()

        StreamTransfer.copyToEnd(
            input = DeterministicMediaInputStream(expectedBytes),
            output = output
        )

        assertEquals(expectedBytes, output.byteCount)
    }

    private data class TransferResult(
        val byteCount: Long,
        val checksum: Long
    )

    private class DeterministicMediaInputStream(
        private val length: Long
    ) : java.io.InputStream() {
        private var position = 0L

        override fun read(): Int {
            if (position >= length) return -1
            return (position++ % 251L).toInt()
        }

        override fun read(buffer: ByteArray, offset: Int, requested: Int): Int {
            if (position >= length) return -1
            val count = minOf(requested.toLong(), length - position).toInt()
            for (index in 0 until count) {
                buffer[offset + index] = ((position + index) % 251L).toByte()
            }
            position += count
            return count
        }
    }

    private class CountingChecksumOutputStream : OutputStream() {
        var byteCount = 0L
            private set
        var checksum = 1L
            private set

        override fun write(value: Int) {
            byteCount++
            checksum = checksum * 31L + (value and 0xff)
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            for (index in offset until offset + length) {
                checksum = checksum * 31L + (buffer[index].toInt() and 0xff)
            }
            byteCount += length
        }
    }
}
