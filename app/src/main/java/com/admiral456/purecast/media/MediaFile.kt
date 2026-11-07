package com.admiral456.purecast.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class MediaFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?
) {
    companion object {
        fun fromUri(context: Context, uri: Uri): MediaFile? {
            val resolver = context.contentResolver
            var name = "media"
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

            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            return MediaFile(uri, name, mimeType, size)
        }
    }
}
