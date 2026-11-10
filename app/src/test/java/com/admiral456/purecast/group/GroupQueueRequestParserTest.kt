package com.admiral456.purecast.group

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupQueueRequestParserTest {
    @Test
    fun parsesFixedLengthJsonPostAndRemovesQueryFromRoute() {
        val body = """{"code":"ABC12345"}"""
        val parsed = GroupQueueRequestParser.read(
            requestBytes("POST", "/v1/join?from=phone", body),
            "192.168.1.20",
            maxHeaderBytes = 8_192,
            maxBodyBytes = 8_192
        )

        assertEquals("POST", parsed?.method)
        assertEquals("/v1/join", parsed?.path)
        assertEquals(body, parsed?.body)
        assertEquals("application/json; charset=utf-8", parsed?.headers?.get("content-type"))
        assertEquals("192.168.1.20", parsed?.remote)
    }

    @Test
    fun parsesGetWithoutBody() {
        val parsed = GroupQueueRequestParser.read(
            "GET /v1/state HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(),
            "127.0.0.1",
            maxHeaderBytes = 8_192,
            maxBodyBytes = 8_192
        )

        assertEquals("GET", parsed?.method)
        assertEquals("/v1/state", parsed?.path)
        assertEquals("", parsed?.body)
    }

    @Test
    fun returnsNullWhenConnectionClosesBeforeAnyRequest() {
        assertNull(
            GroupQueueRequestParser.read(
                ByteArrayInputStream(byteArrayOf()),
                "127.0.0.1",
                maxHeaderBytes = 8_192,
                maxBodyBytes = 8_192
            )
        )
    }

    @Test(expected = IOException::class)
    fun rejectsDuplicateContentLengthHeaders() {
        val body = "{}"
        val headers = listOf(
            "Host: localhost",
            "Content-Type: application/json",
            "Content-Length: 2",
            "Content-Length: 2"
        )
        GroupQueueRequestParser.read(
            requestBytes("POST", "/v1/join", body, headers),
            "127.0.0.1",
            8_192,
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsChunkedTransferEncoding() {
        val raw = "POST /v1/join HTTP/1.1\r\nHost: localhost\r\n" +
            "Transfer-Encoding: chunked\r\n\r\n0\r\n\r\n"
        GroupQueueRequestParser.read(raw.toByteArray(), "127.0.0.1", 8_192, 8_192)
    }

    @Test(expected = IOException::class)
    fun rejectsMalformedContentLengthInsteadOfTreatingItAsZero() {
        val headers = listOf(
            "Host: localhost",
            "Content-Type: application/json",
            "Content-Length: unknown"
        )
        GroupQueueRequestParser.read(
            requestBytes("POST", "/v1/join", "{}", headers),
            "127.0.0.1",
            8_192,
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsPostWithoutContentLength() {
        val raw = "POST /v1/join HTTP/1.1\r\nHost: localhost\r\n" +
            "Content-Type: application/json\r\n\r\n{}"
        GroupQueueRequestParser.read(raw.toByteArray(), "127.0.0.1", 8_192, 8_192)
    }

    @Test(expected = IOException::class)
    fun rejectsRequestBodyLongerThanDeclared() {
        val headers = listOf(
            "Host: localhost",
            "Content-Type: application/json",
            "Content-Length: 4"
        )
        GroupQueueRequestParser.read(
            requestBytes("POST", "/v1/join", "{}", headers),
            "127.0.0.1",
            8_192,
            8_192
        )
    }

    @Test(expected = GroupQueueRequestParser.TooLarge::class)
    fun rejectsBodyAboveConfiguredLimitBeforeReadingIt() {
        val headers = listOf(
            "Host: localhost",
            "Content-Type: application/json",
            "Content-Length: 100"
        )
        GroupQueueRequestParser.read(
            requestBytes("POST", "/v1/join", "", headers),
            "127.0.0.1",
            8_192,
            16
        )
    }

    @Test(expected = IOException::class)
    fun rejectsGetWithRequestBody() {
        val raw = "GET /v1/state HTTP/1.1\r\nHost: localhost\r\n" +
            "Content-Length: 1\r\n\r\nx"
        GroupQueueRequestParser.read(raw.toByteArray(), "127.0.0.1", 8_192, 8_192)
    }

    @Test(expected = IOException::class)
    fun rejectsMalformedRequestLine() {
        GroupQueueRequestParser.read(
            "POST  /v1/join HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(),
            "127.0.0.1",
            8_192,
            8_192
        )
    }

    @Test
    fun bodyReaderFallsBackWhenBulkReadReturnsZero() {
        val body = """{"code":"ABC12345"}"""
        val request = requestBytes("POST", "/v1/join", body)
        val parsed = GroupQueueRequestParser.read(
            ZeroFirstBulkReadInputStream(request),
            "192.168.1.20",
            8_192,
            8_192
        )

        assertEquals(body, parsed?.body)
    }

    private fun requestBytes(
        method: String,
        path: String,
        body: String,
        headers: List<String> = listOf(
            "Host: localhost",
            "Content-Type: application/json; charset=utf-8",
            "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}"
        )
    ): ByteArray =
        ("$method $path HTTP/1.1\r\n" + headers.joinToString("\r\n") +
            "\r\n\r\n" + body).toByteArray(Charsets.UTF_8)

    private class ZeroFirstBulkReadInputStream(bytes: ByteArray) : InputStream() {
        private val delegate = ByteArrayInputStream(bytes)
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
}
