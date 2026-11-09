package com.admiral456.purecast.group

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Strict, bounded HTTP/1.x request reader for the local shared-queue API.
 *
 * The API intentionally supports only a single request per connection and
 * fixed-length JSON bodies; chunked transfer coding and ambiguous framing
 * are rejected rather than guessed.
 */
internal object GroupQueueRequestParser {
    data class ParsedRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String,
        val remote: String
    )

    class TooLarge : IOException("HTTP request exceeds the configured limit")

    fun read(
        input: InputStream,
        remote: String,
        maxHeaderBytes: Int,
        maxBodyBytes: Int
    ): ParsedRequest? {
        require(maxHeaderBytes >= 4) { "maxHeaderBytes must be at least 4" }
        require(maxBodyBytes >= 0) { "maxBodyBytes must not be negative" }

        val headerBytes = ByteArrayOutputStream()
        val terminator = byteArrayOf(13, 10, 13, 10)
        var matched = 0
        while (headerBytes.size() < maxHeaderBytes) {
            val next = input.read()
            if (next < 0) {
                if (headerBytes.size() == 0) return null
                throw IOException("Incomplete HTTP headers")
            }
            headerBytes.write(next)
            matched = when {
                next == terminator[matched].toInt() -> matched + 1
                next == 13 -> 1
                else -> 0
            }
            if (matched == terminator.size) break
        }
        if (matched != terminator.size) throw TooLarge()

        val headerText = String(headerBytes.toByteArray(), Charsets.ISO_8859_1)
            .removeSuffix("\r\n\r\n")
        val lines = headerText.split("\r\n")
        val requestParts = lines.firstOrNull()?.split(' ')
            ?: throw IOException("Missing HTTP request line")
        if (requestParts.size != 3 || requestParts.any(String::isBlank)) {
            throw IOException("Malformed HTTP request line")
        }

        val method = requestParts[0].uppercase()
        if (method != "GET" && method != "POST") throw IOException("Unsupported HTTP method")
        val target = requestParts[1]
        val version = requestParts[2]
        if (version != "HTTP/1.0" && version != "HTTP/1.1") {
            throw IOException("Unsupported HTTP version")
        }
        if (!target.startsWith("/") || target.startsWith("//") || '#' in target) {
            throw IOException("Invalid request target")
        }
        val path = target.substringBefore('?')
        if (path.isBlank()) throw IOException("Missing request path")

        val headers = linkedMapOf<String, String>()
        val validHeaderName = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
        lines.drop(1).forEach { line ->
            if (line.isEmpty() || line.startsWith(' ') || line.startsWith('\t')) {
                throw IOException("Malformed or folded HTTP header")
            }
            val colon = line.indexOf(':')
            if (colon <= 0) throw IOException("Malformed HTTP header")
            val name = line.substring(0, colon).trim()
            if (!validHeaderName.matches(name)) throw IOException("Invalid HTTP header name")
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
            throw IOException("Encoded request bodies are not supported")
        }

        val rawLength = headers["content-length"]
        val length = if (rawLength == null) {
            if (method == "POST") throw IOException("POST requires Content-Length")
            0
        } else {
            if (rawLength.isEmpty() || rawLength.any { it !in '0'..'9' }) {
                throw IOException("Invalid Content-Length")
            }
            rawLength.toLongOrNull() ?: throw TooLarge()
        }
        if (length > maxBodyBytes.toLong() || length > Int.MAX_VALUE) throw TooLarge()
        if (method == "GET" && length != 0L) throw IOException("GET request body is not supported")
        if (method == "POST") {
            val contentType = headers["content-type"]?.substringBefore(';')?.trim()
            if (!contentType.equals("application/json", ignoreCase = true)) {
                throw IOException("POST requires an application/json body")
            }
            if (length == 0L) throw IOException("POST body must not be empty")
        }

        val bodyBytes = ByteArray(length.toInt())
        var offset = 0
        while (offset < bodyBytes.size) {
            val count = input.read(bodyBytes, offset, bodyBytes.size - offset)
            when {
                count < 0 -> throw IOException("Incomplete HTTP request body")
                count == 0 -> {
                    val singleByte = input.read()
                    if (singleByte < 0) throw IOException("Incomplete HTTP request body")
                    bodyBytes[offset++] = singleByte.toByte()
                }
                else -> offset += count
            }
        }

        return ParsedRequest(
            method = method,
            path = path,
            headers = headers,
            body = String(bodyBytes, Charsets.UTF_8),
            remote = remote
        )
    }
}
