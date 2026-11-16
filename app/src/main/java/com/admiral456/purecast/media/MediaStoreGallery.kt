package com.admiral456.purecast.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MediaStoreEntry(
    val uri: String,
    val dateAddedSeconds: Long
)

internal object MediaStoreGalleryLogic {
    fun merge(
        entries: List<MediaStoreEntry>,
        limit: Int
    ): List<String> {
        if (limit <= 0) return emptyList()
        return entries
            .asSequence()
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
            .sortedByDescending { it.dateAddedSeconds }
            .take(limit)
            .map { it.uri }
            .toList()
    }
}

/**
 * Queries image and video collections independently. This follows Android's granular
 * media-permission model and avoids relying on a single Files-provider implementation.
 * On Android 14+ the platform filters each collection to the user's selected media.
 */
object MediaStoreGallery {
    suspend fun latestMediaUris(
        context: Context,
        includeImages: Boolean,
        includeVideos: Boolean,
        limit: Int = 200
    ): List<Uri> = withContext(Dispatchers.IO) {
        if ((!includeImages && !includeVideos) || limit <= 0) {
            return@withContext emptyList()
        }

        val resolver = context.contentResolver
        val entries = buildList {
            if (includeImages) {
                addAll(queryCollection(resolver, imageCollection()))
            }
            if (includeVideos) {
                addAll(queryCollection(resolver, videoCollection()))
            }
        }
        MediaStoreGalleryLogic.merge(entries, limit).map(Uri::parse)
    }

    private fun imageCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    private fun videoCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    private fun queryCollection(
        resolver: android.content.ContentResolver,
        collection: Uri
    ): List<MediaStoreEntry> {
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.IS_PENDING} = 0"
        } else {
            null
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED
        )
        val cursor = try {
            resolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: Exception) {
            null
        } ?: try {
            // A vendor provider may reject DATE_ADDED in its projection or sort order.
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                null,
                null
            )
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        return try {
            cursor.use { result ->
                val idColumn = result.getColumnIndex(MediaStore.MediaColumns._ID)
                if (idColumn < 0) return@use emptyList()
                val dateColumn = result.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                buildList {
                    while (result.moveToNext()) {
                        val id = result.getLong(idColumn)
                        val dateAdded = if (dateColumn >= 0 && !result.isNull(dateColumn)) {
                            result.getLong(dateColumn)
                        } else {
                            0L
                        }
                        add(
                            MediaStoreEntry(
                                uri = ContentUris.withAppendedId(collection, id).toString(),
                                dateAddedSeconds = dateAdded
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
