package com.admiral456.purecast.media

/**
 * Maps common video filename extensions to stable MIME types.
 *
 * This is format identification only: PureCast still streams the original URI unchanged.
 * A recognized MIME type does not guarantee that a particular TV can decode the file.
 */
object VideoFormatCatalog {
    private val mimeTypes = mapOf(
        "mp4" to "video/mp4",
        "mp4v" to "video/mp4",
        "m4v" to "video/x-m4v",
        "f4v" to "video/mp4",
        "mkv" to "video/x-matroska",
        "mk3d" to "video/x-matroska",
        "mov" to "video/quicktime",
        "qt" to "video/quicktime",
        "avi" to "video/x-msvideo",
        "webm" to "video/webm",
        "mpg" to "video/mpeg",
        "mpeg" to "video/mpeg",
        "mpe" to "video/mpeg",
        "m1v" to "video/mpeg",
        "m2v" to "video/mpeg",
        "mpv" to "video/mpeg",
        "vob" to "video/mpeg",
        "3gp" to "video/3gpp",
        "3g2" to "video/3gpp2",
        "ts" to "video/mp2t",
        "m2ts" to "video/mp2t",
        "m2t" to "video/mp2t",
        "mts" to "video/mp2t",
        "flv" to "video/x-flv",
        "wmv" to "video/x-ms-wmv",
        "asf" to "video/x-ms-asf",
        "ogv" to "video/ogg",
        "dv" to "video/dv"
    )

    fun mimeTypeForExtension(extension: String?): String? =
        extension
            ?.trim()
            ?.removePrefix(".")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?.let(mimeTypes::get)
}
