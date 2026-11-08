package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFormatCatalogTest {
    @Test
    fun recognizesCommonStillAndAnimatedImageFormats() {
        assertEquals("image/jpeg", ImageFormatCatalog.mimeTypeForExtension("jpg"))
        assertEquals("image/jpeg", ImageFormatCatalog.mimeTypeForExtension(".JPEG"))
        assertEquals("image/png", ImageFormatCatalog.mimeTypeForExtension("png"))
        assertEquals("image/apng", ImageFormatCatalog.mimeTypeForExtension("apng"))
        assertEquals("image/webp", ImageFormatCatalog.mimeTypeForExtension("webp"))
        assertEquals("image/gif", ImageFormatCatalog.mimeTypeForExtension("gif"))
        assertEquals("image/bmp", ImageFormatCatalog.mimeTypeForExtension("dib"))
        assertEquals("image/tiff", ImageFormatCatalog.mimeTypeForExtension("tif"))
    }

    @Test
    fun recognizesModernHighEfficiencyImageFormats() {
        assertEquals("image/heic", ImageFormatCatalog.mimeTypeForExtension("heic"))
        assertEquals("image/heif", ImageFormatCatalog.mimeTypeForExtension("heif"))
        assertEquals("image/avif", ImageFormatCatalog.mimeTypeForExtension("avif"))
        assertEquals("image/jxl", ImageFormatCatalog.mimeTypeForExtension("jxl"))
        assertEquals("image/jpx", ImageFormatCatalog.mimeTypeForExtension("jpx"))
    }

    @Test
    fun recognizesVectorAndCameraRawFormatsForOriginalFileHandoff() {
        assertEquals("image/svg+xml", ImageFormatCatalog.mimeTypeForExtension("svg"))
        assertEquals("image/vnd.microsoft.icon", ImageFormatCatalog.mimeTypeForExtension("ico"))
        assertEquals("image/x-adobe-dng", ImageFormatCatalog.mimeTypeForExtension("dng"))
        assertEquals("image/x-sony-arw", ImageFormatCatalog.mimeTypeForExtension("arw"))
        assertEquals("image/x-nikon-nef", ImageFormatCatalog.mimeTypeForExtension("nef"))
        assertEquals("image/x-canon-cr3", ImageFormatCatalog.mimeTypeForExtension("cr3"))
        assertEquals("image/x-panasonic-rw2", ImageFormatCatalog.mimeTypeForExtension("rw2"))
    }

    @Test
    fun recognizesScientificAndGpuImageFormats() {
        assertEquals("image/fits", ImageFormatCatalog.mimeTypeForExtension("fits"))
        assertEquals("image/qoi", ImageFormatCatalog.mimeTypeForExtension("qoi"))
        assertEquals("image/x-exr", ImageFormatCatalog.mimeTypeForExtension("exr"))
        assertEquals("image/vnd-ms.dds", ImageFormatCatalog.mimeTypeForExtension("dds"))
        assertEquals("image/ktx", ImageFormatCatalog.mimeTypeForExtension("ktx"))
        assertEquals("image/ktx2", ImageFormatCatalog.mimeTypeForExtension("ktx2"))
    }

    @Test
    fun recognizesAdditionalCameraRawExtensions() {
        assertEquals("image/x-canon-crw", ImageFormatCatalog.mimeTypeForExtension("crw"))
        assertEquals("image/x-minolta-mrw", ImageFormatCatalog.mimeTypeForExtension("mrw"))
        assertEquals("image/x-sigma-x3f", ImageFormatCatalog.mimeTypeForExtension("x3f"))
        assertEquals("image/x-hasselblad-3fr", ImageFormatCatalog.mimeTypeForExtension("3fr"))
        assertEquals("image/x-phaseone-iiq", ImageFormatCatalog.mimeTypeForExtension("iiq"))
        assertEquals("image/x-kodak-dcr", ImageFormatCatalog.mimeTypeForExtension("dcr"))
    }

    @Test
    fun recognizesExtensionsInDisplayNamesCaseInsensitively() {
        assertEquals("image/avif", ImageFormatCatalog.mimeTypeForFileName("Holiday.PICTURE.AVIF"))
        assertEquals("image/jpeg", ImageFormatCatalog.mimeTypeForFileName("photo.JFIF"))
        assertNull(ImageFormatCatalog.mimeTypeForFileName("without-extension"))
    }

    @Test
    fun identifiesImageMimeTypesAndIgnoresParameters() {
        assertTrue(ImageFormatCatalog.isImageMimeType("image/jpeg"))
        assertTrue(ImageFormatCatalog.isImageMimeType("IMAGE/AVIF; charset=binary"))
        assertTrue(ImageFormatCatalog.isImageMimeType("image/x-sony-arw"))
        assertFalse(ImageFormatCatalog.isImageMimeType("video/mp4"))
        assertFalse(ImageFormatCatalog.isImageMimeType("application/octet-stream"))
        assertFalse(ImageFormatCatalog.isImageMimeType(null))
    }

    @Test
    fun unknownExtensionsRemainUnknown() {
        assertNull(ImageFormatCatalog.mimeTypeForExtension("not-a-format"))
        assertTrue(ImageFormatCatalog.knownExtensions().containsAll(listOf("jpg", "png", "webp", "avif", "heic", "jxl")))
    }
}
