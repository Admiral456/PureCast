package com.admiral456.purecast.server

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Strict, bounded HTTP request-header parser for the local media server.
 *
 * The media endpoint serves only GET and HEAD requests and does not accept
 * request bodies. Ambiguous header framing (duplicate headers, transfer coding,
 * content encoding or non-zero Content-Length) is rejected rather than guessed.
 */
internal object MediaRequestParser {
    data class ParsedRequest(
        val method: String,
        val target: String,
        val headers: Map<String, String>
    )

    class HeadersTooLarge : IOException("HTTP request headers exceed the configured limit")

    private val validToken = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
    private val unsignedDecimal = Regex("^[0-9]+$")

    fun read(input: InputStream, maxHeaderBytes: Int): ParsedRequest? {
        require(maxHeaderBytes >= 4) { "maxHeaderBytes must be at least 4" }

        val headerBytes = ByteArrayOutputStream()
        val terminator = byteArrayOf(13, 10, 13, 10)
        var matched = 0
        while (headerBytes.size() < maxHeaderBytes) {
            val next = input.read()
            if (next < 0) {
                if (headerBytes.size() == 0) return null
                throw IOException("Incomplete HTTP request headers")
            }
            headerBytes.write(next)
            matched = when {
                next == terminator[matched].toInt() -> matched + 1
                next == 13 -> 1
                else -> 0
            }
            if (matched == terminator.size) break
        }
        if (matched != terminator.size) throw HeadersTooLarge()

        val headerText = String(headerBytes.toByteArray(), Charsets.ISO_8859_1)
            .removeSuffix("\r\n\r\n")
        val lines = headerText.split("\r\n")
        val requestParts = lines.firstOrNull()?.split(' ')
            ?: throw IOException("Missing HTTP request line")
        if (requestParts.size != 3 || requestParts.any(String::isBlank)) {
            throw IOException("Malformed HTTP request line")
        }

        val method = requestParts[0]
        val target = requestParts[1]
        val version = requestParts[2]
        if (!validToken.matches(method)) throw IOException("Invalid HTTP method")
        if (version != "HTTP/1.0" && version != "HTTP/1.1") {
            throw IOException("Unsupported HTTP version")
        }
        if (!target.startsWith("/") || target.startsWith("//") ||
            target.any { it.code < 0x21 || it.code == 0x7f } || '#' in target
        ) {
            throw IOException("Invalid HTTP request target")
        }

        val headers = linkedMapOf<String, String>()
        lines.drop(1).forEach { line ->
            if (line.isEmpty() || line.startsWith(' ') || line.startsWith('\t')) {
                throw IOException("Malformed or folded HTTP header")
            }
            val colon = line.indexOf(':')
            if (colon <= 0) throw IOException("Malformed HTTP header")
            val name = line.substring(0, colon)
            if (!validToken.matches(name)) throw IOException("Invalid HTTP header name")
            val key = name.lowercase()
            if (headers.containsKey(key)) throw IOException("Duplicate HTTP header")
            val value = line.substring(colon + 1).trim()
            if (value.any { (it.code < 0x20 && it != '\t') || it.code == 0x7f }) {
                throw IOException("Invalid HTTP header value")
            }
            headers[key] = value
        }

        if (headers.containsKey("transfer-encoding")) {
            throw IOException("Transfer-Encoding is not supported")
        }
        if (headers.containsKey("content-encoding")) {
            throw IOException("Encoded requests are not supported")
        }

        headers["content-length"]?.let { rawLength ->
            if (!unsignedDecimal.matches(rawLength)) throw IOException("Invalid Content-Length")
            val length = rawLength.toLongOrNull() ?: throw IOException("Invalid Content-Length")
            if (length != 0L) throw IOException("Media requests must not contain a body")
        }

        return ParsedRequest(
            method = method.uppercase(),
            target = target,
            headers = headers
        )
    }
}
