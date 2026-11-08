package com.admiral456.purecast.media

/**
 * Image filename extensions for media hand-off.
 *
 * MIME recognition does not imply that Android or a particular TV can decode
 * every format. PureCast keeps the original bytes and never converts images.
 */
object ImageFormatCatalog {
    private val mimeTypes = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "jpe" to "image/jpeg",
        "jfif" to "image/jpeg",
        "pjpeg" to "image/jpeg",
        "pjp" to "image/jpeg",
        "png" to "image/png",
        "apng" to "image/apng",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "heic" to "image/heic",
        "heics" to "image/heic",
        "heif" to "image/heif",
        "heifs" to "image/heif",
        "avif" to "image/avif",
        "jxl" to "image/jxl",
        "bmp" to "image/bmp",
        "dib" to "image/bmp",
        "tif" to "image/tiff",
        "tiff" to "image/tiff",
        "svg" to "image/svg+xml",
        "ico" to "image/vnd.microsoft.icon",
        "icns" to "image/icns",
        "jp2" to "image/jp2",
        "j2k" to "image/j2k",
        "jpf" to "image/jpx",
        "jpx" to "image/jpx",
        "jpm" to "image/jpm",
        "dng" to "image/x-adobe-dng",
        "arw" to "image/x-sony-arw",
        "cr2" to "image/x-canon-cr2",
        "cr3" to "image/x-canon-cr3",
        "nef" to "image/x-nikon-nef",
        "nrw" to "image/x-nikon-nrw",
        "orf" to "image/x-olympus-orf",
        "pef" to "image/x-pentax-pef",
        "raf" to "image/x-fuji-raf",
        "rw2" to "image/x-panasonic-rw2",
        "srw" to "image/x-samsung-srw",
        "psd" to "image/vnd.adobe.photoshop",
        "xcf" to "image/x-xcf",
        "jxr" to "image/vnd.ms-photo",
        "wdp" to "image/vnd.ms-photo",
        "hdp" to "image/vnd.ms-photo",
        "wbmp" to "image/vnd.wap.wbmp",
        "tga" to "image/x-tga",
        "pcx" to "image/x-pcx",
        "ppm" to "image/x-portable-pixmap",
        "pgm" to "image/x-portable-graymap",
        "pbm" to "image/x-portable-bitmap",
        "pnm" to "image/x-portable-anymap"
    )

    fun mimeTypeForExtension(extension: String): String? =
        mimeTypes[extension.trim().removePrefix(".").lowercase()]

    fun mimeTypeForFileName(displayName: String): String? =
        mimeTypeForExtension(displayName.substringAfterLast('.', ""))

    fun isImageMimeType(mimeType: String?): Boolean =
        mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.startsWith("image/", ignoreCase = true) == true

    fun knownExtensions(): Set<String> = mimeTypes.keys
}
