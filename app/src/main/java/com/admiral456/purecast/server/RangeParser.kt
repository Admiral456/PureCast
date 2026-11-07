package com.admiral456.purecast.server

data class ByteRange(val start: Long, val endInclusive: Long) {
    val length: Long
        get() = endInclusive - start + 1L
}

object RangeParser {
    fun parse(header: String?, totalLength: Long): ByteRange? {
        if (header.isNullOrBlank() || totalLength <= 0L) return null
        if (!header.startsWith("bytes=", ignoreCase = true)) return null

        val value = header.substringAfter('=').trim()
        if (value.contains(',')) return null
        val parts = value.split('-', limit = 2)
        if (parts.size != 2) return null

        return if (parts[0].isBlank()) {
            val suffixLength = parts[1].toLongOrNull() ?: return null
            if (suffixLength <= 0L) return null
            val start = (totalLength - suffixLength).coerceAtLeast(0L)
            ByteRange(start, totalLength - 1L)
        } else {
            val start = parts[0].toLongOrNull() ?: return null
            if (start < 0L || start >= totalLength) return null
            val requestedEnd = if (parts[1].isBlank()) totalLength - 1L else {
                parts[1].toLongOrNull() ?: return null
            }
            if (requestedEnd < start) return null
            ByteRange(start, requestedEnd.coerceAtMost(totalLength - 1L))
        }
    }
}
