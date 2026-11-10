package com.admiral456.purecast.server

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Bounded, forward-only transfer helpers for provider-backed media streams.
 *
 * Some ContentResolver providers return short reads; the zero-read fallback also
 * protects the transfer loop from streams that temporarily return no bulk data.
 */
internal object StreamTransfer {
    fun skipFully(input: InputStream, bytes: Long) {
        require(bytes >= 0L) { "bytes must not be negative" }
        var remaining = bytes
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped.coerceAtMost(remaining)
            } else {
                if (input.read() < 0) {
                    throw IOException("Soubor skončil před požadovaným rozsahem.")
                }
                remaining--
            }
        }
    }

    fun copyExactly(
        input: InputStream,
        output: OutputStream,
        length: Long,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) {
        require(length >= 0L) { "length must not be negative" }
        require(bufferSize > 0) { "bufferSize must be positive" }
        var remaining = length
        val buffer = acquireBuffer(bufferSize)
        while (remaining > 0L) {
            val requested = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, requested)
            when {
                read < 0 -> throw IOException("Zdrojový soubor skončil před koncem přenosu.")
                read == 0 -> {
                    val singleByte = input.read()
                    if (singleByte < 0) {
                        throw IOException("Zdrojový soubor skončil před koncem přenosu.")
                    }
                    output.write(singleByte)
                    remaining--
                }
                else -> {
                    output.write(buffer, 0, read)
                    remaining -= read.toLong()
                }
            }
        }
    }

    fun copyToEnd(
        input: InputStream,
        output: OutputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) {
        require(bufferSize > 0) { "bufferSize must be positive" }
        val buffer = acquireBuffer(bufferSize)
        while (true) {
            val read = input.read(buffer, 0, buffer.size)
            when {
                read < 0 -> return
                read == 0 -> {
                    val singleByte = input.read()
                    if (singleByte < 0) return
                    output.write(singleByte)
                }
                else -> output.write(buffer, 0, read)
            }
        }
    }

    internal const val DEFAULT_BUFFER_SIZE = 256 * 1024

    // Reuse one bounded transfer buffer per worker thread. Range-heavy TV browsers
    // can open multiple short-lived requests; this reduces allocation/GC pressure
    // without sharing mutable memory between concurrent streams.
    private val reusableDefaultBuffer = ThreadLocal.withInitial {
        ByteArray(DEFAULT_BUFFER_SIZE)
    }

    private fun acquireBuffer(bufferSize: Int): ByteArray =
        if (bufferSize == DEFAULT_BUFFER_SIZE) reusableDefaultBuffer.get() else ByteArray(bufferSize)
}
