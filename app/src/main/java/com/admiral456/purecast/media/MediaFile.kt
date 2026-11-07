package com.admiral456.purecast.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.provider.OpenableColumns

data class MediaFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")

    companion object {
        private val knownMimeTypes = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
            "gif" to "image/gif",
            "heic" to "image/heic",
            "heif" to "image/heif",
            "bmp" to "image/bmp",
            "tif" to "image/tiff",
            "tiff" to "image/tiff",
            "mp4" to "video/mp4",
            "m4v" to "video/x-m4v",
            "mkv" to "video/x-matroska",
            "mov" to "video/quicktime",
            "avi" to "video/x-msvideo",
            "webm" to "video/webm",
            "mpg" to "video/mpeg",
            "mpeg" to "video/mpeg",
            "3gp" to "video/3gpp"
        )

        fun fromUri(context: Context, uri: Uri): MediaFile? {
            val resolver = context.contentResolver
            var name = uri.lastPathSegment?.substringAfterLast('/') ?: "media"
            var size: Long? = null

            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }

            val extension = name.substringAfterLast('.', "").lowercase()
            val providerMime = resolver.getType(uri)
            val mimeFromExtension = knownMimeTypes[extension]
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            val mimeType = when {
                providerMime.isNullOrBlank() -> mimeFromExtension ?: "application/octet-stream"
                providerMime == "application/octet-stream" && mimeFromExtension != null -> mimeFromExtension
                else -> providerMime
            }
            return MediaFile(uri, name, mimeType, size)
        }
    }
}
