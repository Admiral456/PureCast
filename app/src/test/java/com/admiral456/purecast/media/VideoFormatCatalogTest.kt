package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoFormatCatalogTest {
    @Test
    fun recognizesCommonVideoContainersAndExtensions() {
        val expected = mapOf(
            "mp4" to "video/mp4",
            "m4v" to "video/x-m4v",
            "mkv" to "video/x-matroska",
            "mov" to "video/quicktime",
            "avi" to "video/x-msvideo",
            "webm" to "video/webm",
            "mpeg" to "video/mpeg",
            "vob" to "video/mpeg",
            "3gp" to "video/3gpp",
            "3g2" to "video/3gpp2",
            "m2ts" to "video/mp2t",
            "mts" to "video/mp2t",
            "ts" to "video/mp2t",
            "flv" to "video/x-flv",
            "wmv" to "video/x-ms-wmv",
            "asf" to "video/x-ms-asf",
            "ogv" to "video/ogg",
            "dv" to "video/dv"
        )

        expected.forEach { (extension, mimeType) ->
            assertEquals(extension, mimeType, VideoFormatCatalog.mimeTypeForExtension(extension))
        }
    }

    @Test
    fun normalizesCaseWhitespaceAndOptionalLeadingDot() {
        assertEquals("video/mp4", VideoFormatCatalog.mimeTypeForExtension(" .MP4 "))
        assertEquals("video/mp2t", VideoFormatCatalog.mimeTypeForExtension(".M2TS"))
    }

    @Test
    fun unknownOrEmptyExtensionsRemainUnclassified() {
        assertNull(VideoFormatCatalog.mimeTypeForExtension(null))
        assertNull(VideoFormatCatalog.mimeTypeForExtension(""))
        assertNull(VideoFormatCatalog.mimeTypeForExtension("   "))
        assertNull(VideoFormatCatalog.mimeTypeForExtension("mxf"))
        assertNull(VideoFormatCatalog.mimeTypeForExtension("txt"))
    }
}
