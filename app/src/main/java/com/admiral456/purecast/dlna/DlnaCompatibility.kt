package com.admiral456.purecast.dlna

enum class MediaCompatibility {
    SUPPORTED_MIME_TYPE,
    MIME_TYPE_NOT_ADVERTISED,
    UNKNOWN
}

object DlnaCompatibility {
    fun evaluate(mimeType: String, sinkProtocols: List<String>?): MediaCompatibility {
        val requestedMime = normalizeMimeType(mimeType)
        if (requestedMime.isBlank() || requestedMime == "application/octet-stream" || sinkProtocols.isNullOrEmpty()) {
            return MediaCompatibility.UNKNOWN
        }
        val advertisedFormats = sinkProtocols.mapNotNull(::contentFormat)
        if (advertisedFormats.isEmpty()) return MediaCompatibility.UNKNOWN
        val supported = advertisedFormats.any { matchesMimeType(it, requestedMime) }
        return if (supported) MediaCompatibility.SUPPORTED_MIME_TYPE
        else MediaCompatibility.MIME_TYPE_NOT_ADVERTISED
    }

    private fun contentFormat(protocolInfo: String): String? {
        val parts = protocolInfo.trim().split(':', limit = 4)
        if (parts.size < 3) return null
        return normalizeMimeType(parts[2]).takeIf { it.isNotBlank() }
    }

    private fun normalizeMimeType(value: String): String =
        value.substringBefore(';').trim().lowercase()

    private fun matchesMimeType(pattern: String, mimeType: String): Boolean {
        if (pattern == "*/*" || pattern == "*") return true
        if (pattern.endsWith("/*")) {
            return mimeType.startsWith(pattern.removeSuffix("*"))
        }
        return pattern == mimeType
    }
}
