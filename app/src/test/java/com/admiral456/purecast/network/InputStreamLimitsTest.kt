package com.admiral456.purecast.network

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class InputStreamLimitsTest {
    @Test
    fun readsResponseExactlyAtLimit() {
        val expected = ByteArray(32) { it.toByte() }
        val actual = ByteArrayInputStream(expected).readBytesLimited(expected.size)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun rejectsResponseLargerThanLimit() {
        val input = ByteArrayInputStream(ByteArray(33))
        try {
            input.readBytesLimited(32)
            throw AssertionError("Expected response-size limit to reject oversized input")
        } catch (_: IOException) {
            // Expected.
        }
    }

    @Test
    fun continuesWhenBulkReadTemporarilyReturnsZero() {
        val delegate = ByteArrayInputStream("PureCast".toByteArray(Charsets.UTF_8))
        val input = object : java.io.InputStream() {
            private var returnZero = true

            override fun read(): Int = delegate.read()

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (returnZero) {
                    returnZero = false
                    return 0
                }
                return delegate.read(buffer, offset, length)
            }
        }
        assertEquals("PureCast", input.readTextLimited(32))
    }

    @Test
    fun preservesUtf8Text() {
        val expected = "Televize – fotografie žluťoučký kůň"
        val actual = ByteArrayInputStream(expected.toByteArray(Charsets.UTF_8))
            .readTextLimited(256)
        assertEquals(expected, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveLimit() {
        ByteArrayInputStream(byteArrayOf()).readBytesLimited(0)
    }
}
