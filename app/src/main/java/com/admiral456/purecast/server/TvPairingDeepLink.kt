package com.admiral456.purecast.server

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class TvPairingLaunch(
    val browserUrl: String,
    val code: String
)

/**
 * A small, strictly validated hand-off from a TV-displayed QR code to the Android app.
 * Only private IPv4 PureCast TV-browser URLs are accepted.
 */
object TvPairingDeepLink {
    private val mediaPath = Regex("^/tv/[A-Za-z0-9_-]{12}$")
    private val codePattern = Regex("^[0-9]{6}$")

    fun create(browserUrl: String, code: String): String {
        require(isValidBrowserUrl(browserUrl)) { "Invalid local PureCast TV URL." }
        require(codePattern.matches(code)) { "Pairing code must contain six digits." }
        val encodedUrl = URLEncoder.encode(browserUrl, StandardCharsets.UTF_8.name())
        return "purecast://pair?url=$encodedUrl&code=$code"
    }

    fun parse(value: String?): TvPairingLaunch? {
        if (value.isNullOrBlank()) return null
        val link = runCatching { URI(value) }.getOrNull() ?: return null
        if (link.scheme != "purecast" || link.rawAuthority != "pair" ||
            !link.rawPath.isNullOrEmpty() || link.rawFragment != null
        ) {
            return null
        }

        val rawQuery = link.rawQuery ?: return null
        val parameters = linkedMapOf<String, String>()
        for (part in rawQuery.split('&')) {
            val separator = part.indexOf('=')
            if (separator <= 0) return null
            val key = decode(part.substring(0, separator)) ?: return null
            val decodedValue = decode(part.substring(separator + 1)) ?: return null
            if (parameters.put(key, decodedValue) != null) return null
        }
        if (parameters.keys != setOf("url", "code")) return null

        val browserUrl = parameters["url"] ?: return null
        val code = parameters["code"] ?: return null
        if (!codePattern.matches(code) || !isValidBrowserUrl(browserUrl)) return null
        return TvPairingLaunch(browserUrl, code)
    }

    private fun decode(value: String): String? =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrNull()

    private fun isValidBrowserUrl(value: String): Boolean {
        val target = runCatching { URI(value) }.getOrNull() ?: return false
        if (target.scheme != "http" || target.rawUserInfo != null ||
            target.rawQuery != null || target.rawFragment != null ||
            target.port !in 1..65_535
        ) {
            return false
        }
        if (!mediaPath.matches(target.rawPath ?: return false)) return false
        return isPrivateIpv4(target.host ?: return false)
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val octets = host.split('.')
        if (octets.size != 4) return false
        val values = octets.map { part ->
            if (part.isEmpty() || part.any { !it.isDigit() }) return false
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return false
        }
        if (values.map(Int::toString).joinToString(".") != host) return false
        val first = values[0]
        val second = values[1]
        return first == 10 ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168)
    }
}
