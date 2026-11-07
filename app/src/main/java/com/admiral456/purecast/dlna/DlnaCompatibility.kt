package com.admiral456.purecast.dlna

enum class MediaCompatibility {
    SUPPORTED_MIME_TYPE,
    MIME_TYPE_NOT_ADVERTISED,
    UNKNOWN
}

object DlnaCompatibility {
    fun evaluate(mimeType: String, sinkProtocols: List<String>?): MediaCompatibility {
        if (mimeType.isBlank() || mimeType == "application/octet-stream" || sinkProtocols.isNullOrEmpty()) {
            return MediaCompatibility.UNKNOWN
        }
        val advertisedFormats = sinkProtocols.mapNotNull(::contentFormat)
        if (advertisedFormats.isEmpty()) return MediaCompatibility.UNKNOWN
        val supported = advertisedFormats.any { matchesMimeType(it, mimeType) }
        return if (supported) MediaCompatibility.SUPPORTED_MIME_TYPE
        else MediaCompatibility.MIME_TYPE_NOT_ADVERTISED
    }

    private fun contentFormat(protocolInfo: String): String? {
        val parts = protocolInfo.split(':', limit = 4)
        return parts.getOrNull(2)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private fun matchesMimeType(pattern: String, mimeType: String): Boolean {
        if (pattern == "*/*" || pattern == "*") return true
        if (pattern.endsWith("/*")) {
            return mimeType.startsWith(pattern.removeSuffix("*"))
        }
        return pattern == mimeType.lowercase()
    }
}
