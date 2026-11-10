package com.admiral456.purecast.server

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaRequestParserTest {
    @Test
    fun readsGetRequestAndPreservesByteRangeHeader() {
        val parsed = MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nHost: 192.168.1.2\r\nRange: bytes=100-199\r\n\r\n".byteInputStream(),
            8_192
        )

        assertEquals("GET", parsed?.method)
        assertEquals("/media/Abcdef123456", parsed?.target)
        assertEquals("bytes=100-199", parsed?.headers?.get("range"))
    }

    @Test
    fun acceptsHeadAndZeroLengthContent() {
        val parsed = MediaRequestParser.read(
            "HEAD /tv/Abcdef123456 HTTP/1.1\r\nContent-Length: 0\r\n\r\n".byteInputStream(),
            8_192
        )

        assertEquals("HEAD", parsed?.method)
        assertEquals("0", parsed?.headers?.get("content-length"))
    }

    @Test
    fun headerNamesAreCaseInsensitive() {
        val parsed = MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nhOsT: tv\r\nRaNgE: bytes=0-10\r\n\r\n".byteInputStream(),
            8_192
        )

        assertEquals("tv", parsed?.headers?.get("host"))
        assertEquals("bytes=0-10", parsed?.headers?.get("range"))
    }

    @Test
    fun returnsNullWhenPeerClosesBeforeSendingAnything() {
        assertNull(MediaRequestParser.read(ByteArray(0).inputStream(), 8_192))
    }

    @Test(expected = IOException::class)
    fun rejectsMalformedRequestLine() {
        MediaRequestParser.read("GET  /media/token HTTP/1.1\r\n\r\n".byteInputStream(), 8_192)
    }

    @Test(expected = IOException::class)
    fun rejectsDuplicateRangeHeadersCaseInsensitively() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nRange: bytes=0-1\r\nrange: bytes=2-3\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsMalformedHeaderNames() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nBad Header: value\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsFoldedHeaders() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nHost: tv\r\n continuation\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsChunkedRequests() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsEncodedRequests() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nContent-Encoding: gzip\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsNonZeroRequestBody() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nContent-Length: 5\r\n\r\nhello".byteInputStream(),
            8_192
        )
    }

    @Test(expected = IOException::class)
    fun rejectsInvalidContentLength() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nContent-Length: +0\r\n\r\n".byteInputStream(),
            8_192
        )
    }

    @Test(expected = MediaRequestParser.HeadersTooLarge::class)
    fun rejectsOversizedHeaders() {
        MediaRequestParser.read(
            "GET /media/Abcdef123456 HTTP/1.1\r\nHost: aaaaaaaaaaaaaaaaaaaa\r\n\r\n".byteInputStream(),
            32
        )
    }

    @Test(expected = IOException::class)
    fun rejectsNonOriginFormTargets() {
        MediaRequestParser.read(
            "GET http://tv/media/Abcdef123456 HTTP/1.1\r\n\r\n".byteInputStream(),
            8_192
        )
    }
}
