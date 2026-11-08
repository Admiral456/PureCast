package com.admiral456.purecast.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads small control-plane responses without allowing an untrusted peer to
 * allocate an unbounded byte array. The caller owns and closes the stream.
 */
internal fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
    require(maxBytes > 0) { "maxBytes must be positive" }
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0

    while (true) {
        val allowedRead = minOf(buffer.size, maxBytes - total + 1)
        val read = read(buffer, 0, allowedRead)
        if (read < 0) break
        if (read == 0) {
            val singleByte = read()
            if (singleByte < 0) break
            total++
            if (total > maxBytes) {
                throw IOException("Síťová odpověď překročila povolenou velikost $maxBytes bajtů.")
            }
            output.write(singleByte)
            continue
        }
        total += read
        if (total > maxBytes) {
            throw IOException("Síťová odpověď překročila povolenou velikost $maxBytes bajtů.")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun InputStream.readTextLimited(maxBytes: Int): String =
    String(readBytesLimited(maxBytes), Charsets.UTF_8)

private const val DEFAULT_BUFFER_SIZE = 8_192
