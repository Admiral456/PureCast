package com.admiral456.purecast.server

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamTransferTest {
    @Test
    fun copyExactlyHandlesZeroAndShortBulkReads() {
        val source = ZeroThenShortInputStream("PureCast media".toByteArray())
        val output = ByteArrayOutputStream()

        StreamTransfer.copyExactly(source, output, 8L, bufferSize = 5)

        assertArrayEquals("PureCast".toByteArray(), output.toByteArray())
        assertEquals(' '.code, source.read())
    }

    @Test
    fun copyExactlyNeverReadsBeyondRequestedLength() {
        val source = "abcdef".byteInputStream()
        val output = ByteArrayOutputStream()

        StreamTransfer.copyExactly(source, output, 3L, bufferSize = 2)

        assertEquals("abc", output.toString(Charsets.UTF_8.name()))
        assertEquals('d'.code, source.read())
    }

    @Test(expected = IOException::class)
    fun copyExactlyRejectsPrematureEndOfSource() {
        StreamTransfer.copyExactly(
            "short".byteInputStream(),
            ByteArrayOutputStream(),
            10L
        )
    }

    @Test
    fun copyToEndHandlesZeroAndShortBulkReads() {
        val source = ZeroThenShortInputStream("Original bytes".toByteArray())
        val output = ByteArrayOutputStream()

        StreamTransfer.copyToEnd(source, output, bufferSize = 4)

        assertArrayEquals("Original bytes".toByteArray(), output.toByteArray())
    }

    @Test
    fun skipFullyFallsBackToSingleByteReadsWhenSkipReturnsZero() {
        val source = object : InputStream() {
            private val delegate = "0123456789".byteInputStream()
            override fun read(): Int = delegate.read()
            override fun skip(count: Long): Long = 0L
        }

        StreamTransfer.skipFully(source, 7L)

        assertEquals('7'.code, source.read())
    }

    @Test(expected = IOException::class)
    fun skipFullyRejectsOffsetBeyondEndOfSource() {
        StreamTransfer.skipFully("short".byteInputStream(), 10L)
    }

    @Test
    fun zeroLengthCopyDoesNotConsumeSource() {
        val source = "keep".byteInputStream()
        StreamTransfer.copyExactly(source, ByteArrayOutputStream(), 0L)
        assertEquals('k'.code, source.read())
    }

    private class ZeroThenShortInputStream(
        private val bytes: ByteArray
    ) : InputStream() {
        private var position = 0
        private var returnZero = true

        override fun read(): Int =
            if (position >= bytes.size) -1 else bytes[position++].toInt() and 0xff

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (returnZero) {
                returnZero = false
                return 0
            }
            if (position >= bytes.size) return -1
            val count = minOf(length, 2, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
    }
}
