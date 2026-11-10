package com.admiral456.purecast.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.provider.OpenableColumns
import android.provider.MediaStore

data class MediaFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val dateModifiedMillis: Long? = null,
    val albumName: String? = null
) {
    val isImage: Boolean get() = ImageFormatCatalog.isImageMimeType(mimeType)
    val isVideo: Boolean get() = mimeType.substringBefore(';').trim().startsWith("video/", ignoreCase = true)
    val isPlayableMedia: Boolean get() = isImage || isVideo

    companion object {
        private val knownMimeTypes = mapOf(
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

            safeQuery(context, uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }

            var dateModifiedMillis: Long? = null
            var albumName: String? = null
            val metadataProjection = arrayOf(
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
            )
            val metadataCursor = safeQuery(context, uri, metadataProjection)
                ?: safeQuery(context, uri, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED))
            metadataCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        val modifiedSeconds = cursor.getLong(modifiedIndex)
                        if (modifiedSeconds > 0) {
                            dateModifiedMillis = modifiedSeconds * 1_000L
                        }
                    }
                    val albumIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
                    if (albumIndex >= 0 && !cursor.isNull(albumIndex)) {
                        albumName = cursor.getString(albumIndex)?.trim()?.takeIf { it.isNotEmpty() }
                    }
                }
            }

            val extension = name.substringAfterLast('.', "").lowercase()
            val providerMime = runCatching { resolver.getType(uri) }
                .getOrNull()
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase()
            val mimeFromExtension = ImageFormatCatalog.mimeTypeForExtension(extension)
                ?: knownMimeTypes[extension]
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            val mimeType = when {
                mimeFromExtension != null -> mimeFromExtension
                providerMime.isNullOrBlank() -> "application/octet-stream"
                providerMime in setOf("application/octet-stream", "binary/octet-stream") ->
                    providerMime
                else -> providerMime
            }
            return MediaFile(uri, name, mimeType, size, dateModifiedMillis, albumName)
        }

        private fun safeQuery(context: Context, uri: Uri, projection: Array<String>) =
            runCatching {
                context.contentResolver.query(uri, projection, null, null, null)
            }.getOrNull()
    }
}
