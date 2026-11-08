package com.admiral456.purecast.dlna

import org.junit.Assert.assertEquals
import org.junit.Test

class DlnaCompatibilityTest {
    @Test
    fun recognizesExactAdvertisedMimeType() {
        val sink = listOf("http-get:*:video/mp4:*")
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("video/mp4", sink)
        )
    }

    @Test
    fun recognizesWildcardMimeType() {
        val sink = listOf("http-get:*:video/*:*")
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("video/webm", sink)
        )
    }

    @Test
    fun reportsMimeTypeNotAdvertised() {
        val sink = listOf("http-get:*:image/jpeg:*")
        assertEquals(
            MediaCompatibility.MIME_TYPE_NOT_ADVERTISED,
            DlnaCompatibility.evaluate("video/mp4", sink)
        )
    }

    @Test
    fun unknownWhenRendererDoesNotPublishProtocolInfo() {
        assertEquals(
            MediaCompatibility.UNKNOWN,
            DlnaCompatibility.evaluate("video/mp4", null)
        )
    }

    @Test
    fun unknownForGenericBinaryMimeType() {
        assertEquals(
            MediaCompatibility.UNKNOWN,
            DlnaCompatibility.evaluate("application/octet-stream", listOf("http-get:*:*:*"))
        )
    }
    @Test
    fun ignoresDlnaFeatureParametersWhenMatchingMimeType() {
        val sink = listOf("http-get:*:image/jpeg;DLNA.ORG_PN=JPEG_LRG:DLNA.ORG_OP=01")
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("image/jpeg; charset=binary", sink)
        )
    }

    @Test
    fun matchesModernImageTypesCaseInsensitively() {
        val sink = listOf("http-get:*:IMAGE/AVIF:*", "http-get:*:image/heic:*")
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("image/avif", sink)
        )
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("IMAGE/HEIC", sink)
        )
    }

    @Test
    fun ignoresMalformedProtocolInfoEntries() {
        val sink = listOf("not-a-protocol-info", "http-get:*:*", "http-get:*:image/png:*")
        assertEquals(
            MediaCompatibility.SUPPORTED_MIME_TYPE,
            DlnaCompatibility.evaluate("image/png", sink)
        )
    }

}
