package com.admiral456.purecast.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads only media rows that Android exposes to the current permission grant.
 * On Android 14+ this naturally respects the user's selected-photo access.
 */
object MediaStoreGallery {
    suspend fun latestMediaUris(
        context: Context,
        includeImages: Boolean,
        includeVideos: Boolean,
        limit: Int = 200
    ) : List<android.net.Uri> = withContext(Dispatchers.IO) {
        if (!includeImages && !includeVideos) return@withContext emptyList()
        val mediaTypes = buildList {
            if (includeImages) add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            if (includeVideos) add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
        val placeholders = mediaTypes.joinToString(",") { "?" }
        val selection = buildString {
            append(MediaStore.Files.FileColumns.MEDIA_TYPE)
            append(" IN (")
            append(placeholders)
            append(")")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ")
                append(MediaStore.MediaColumns.IS_PENDING)
                append(" = 0")
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val results = ArrayList<android.net.Uri>(limit.coerceAtLeast(0))

        context.contentResolver.query(
            collection,
            projection,
            selection,
            mediaTypes.toTypedArray(),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            while (cursor.moveToNext() && results.size < limit.coerceAtLeast(0)) {
                val id = cursor.getLong(idColumn)
                results += ContentUris.withAppendedId(collection, id)
            }
        }
        results
    }
}
