package com.admiral456.purecast.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Generates small display-only previews. Original media bytes used for TV streaming are never
 * decoded, resized, cached or modified by this class.
 */
object MediaThumbnailLoader {
    private val cache = object : LruCache<String, Bitmap>(MediaThumbnailPolicy.MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            if (value.isRecycled) 0 else value.allocationByteCount.coerceAtLeast(value.byteCount)
    }

    /** Evict display-only thumbnails when Android reports memory pressure. */
    fun clearCache() {
        cache.evictAll()
    }

    suspend fun loadLimited(
        context: Context,
        media: MediaFile,
        targetWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        targetHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? = MediaProcessingGate.withPermit {
        currentCoroutineContext().ensureActive()

        val size = MediaThumbnailPolicy.boundedSize(targetWidth, targetHeight)
        val cacheKey = "${media.uri}#${size.width}x${size.height}"
        cache.get(cacheKey)?.takeUnless { it.isRecycled }?.let { return@withPermit it }

        // Native image providers can occasionally ignore coroutine cancellation. Finish only
        // this bounded thumbnail request, cache it if useful, then let Compose drop stale results.
        val bitmap = withContext(NonCancellable + Dispatchers.IO) {
            cache.get(cacheKey)?.takeUnless { it.isRecycled }
                ?: load(context, media, size.width, size.height)?.also { decoded ->
                    if (!decoded.isRecycled) cache.put(cacheKey, decoded)
                }
        }

        currentCoroutineContext().ensureActive()
        bitmap
    }

    fun load(
        context: Context,
        media: MediaFile,
        targetWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        targetHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? {
        val size = MediaThumbnailPolicy.boundedSize(targetWidth, targetHeight)
        return try {
            loadInternal(context, media, size.width, size.height)
        } catch (_: OutOfMemoryError) {
            // A preview is optional; a decoder memory failure must never close the gallery.
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun loadInternal(
        context: Context,
        media: MediaFile,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (!media.isImage && !media.isVideo) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Ask Android's media provider for a downsampled preview. This avoids invoking an
            // OEM video decoder just to draw a gallery tile.
            val providerThumbnail = try {
                context.contentResolver.loadThumbnail(
                    media.uri,
                    Size(targetWidth, targetHeight),
                    null
                )
            } catch (_: OutOfMemoryError) {
                return null
            } catch (_: Exception) {
                null
            }
            if (providerThumbnail != null) {
                return try {
                    providerThumbnail.fitInside(targetWidth, targetHeight)
                } catch (_: OutOfMemoryError) {
                    runCatching { providerThumbnail.recycle() }
                    null
                } catch (_: Exception) {
                    runCatching { providerThumbnail.recycle() }
                    null
                }
            }
        }

        // On older Android versions use a safe image decode only. Do not call
        // MediaMetadataRetriever/getFrameAtTime for videos; use a lightweight VIDEO placeholder.
        if (media.isVideo || !media.isImage) return null
        return loadImage(context, media, targetWidth, targetHeight)
    }

    private fun loadImage(
        context: Context,
        media: MediaFile,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val modernThumbnail = try {
                val source = ImageDecoder.createSource(context.contentResolver, media.uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val sourceWidth = info.size.width.coerceAtLeast(1)
                    val sourceHeight = info.size.height.coerceAtLeast(1)
                    val scale = minOf(
                        1f,
                        targetWidth.toFloat() / sourceWidth,
                        targetHeight.toFloat() / sourceHeight
                    )
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                    decoder.setMemorySizePolicy(ImageDecoder.MEMORY_POLICY_LOW_RAM)
                    decoder.setTargetSize(
                        (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                        (sourceHeight * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            } catch (_: OutOfMemoryError) {
                return null
            } catch (_: Exception) {
                null
            }
            if (modernThumbnail != null) {
                return modernThumbnail.fitInside(targetWidth, targetHeight)
            }
        }

        // Legacy fallback: inspect dimensions first and decode only a sampled bitmap.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(media.uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = context.contentResolver.openInputStream(media.uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        return decoded.fitInside(targetWidth, targetHeight)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        while (sampleSize <= Int.MAX_VALUE / 2 &&
            (width / (sampleSize * 2) >= targetWidth * 2 ||
                height / (sampleSize * 2) >= targetHeight * 2)
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun Bitmap.fitInside(maxWidth: Int, maxHeight: Int): Bitmap {
        if (width <= maxWidth && height <= maxHeight) return this
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== this && !isRecycled) runCatching { recycle() }
        return scaled
    }

    private const val DEFAULT_THUMBNAIL_WIDTH = 256
    private const val DEFAULT_THUMBNAIL_HEIGHT = 192
}

data class MediaThumbnailSize(val width: Int, val height: Int)

/** Pure policy helpers for bounded gallery thumbnail memory and decode dimensions. */
object MediaThumbnailPolicy {
    const val MAX_CACHE_BYTES = 8 * 1024 * 1024
    const val TRIM_MEMORY_RUNNING_LOW_THRESHOLD = 10

    fun shouldClearCacheOnTrim(level: Int): Boolean =
        level >= TRIM_MEMORY_RUNNING_LOW_THRESHOLD

    fun boundedSize(
        width: Int,
        height: Int,
        maximumEdge: Int = 512
    ): MediaThumbnailSize {
        val maximum = maximumEdge.coerceAtLeast(1)
        return MediaThumbnailSize(
            width = width.coerceIn(1, maximum),
            height = height.coerceIn(1, maximum)
        )
    }
}
