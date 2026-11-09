package com.admiral456.purecast.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Size
import android.util.LruCache
import kotlin.math.roundToInt
import java.util.concurrent.Semaphore

/**
 * Generates small previews only. It must never prepare or alter the media file used for streaming.
 *
 * Video frame extraction is intentionally not used as a fallback: MediaMetadataRetriever invokes
 * native codec components and some OEM codec stacks can fail on unusual/high-resolution clips.
 * If Android's media provider cannot supply a thumbnail, the UI displays a video placeholder.
 */
object MediaThumbnailLoader {
    // Only a few previews are retained; original media files are never cached or modified.
    private val cache = LruCache<String, Bitmap>(MediaThumbnailPolicy.MAX_CACHE_BYTES) {
        if (it.isRecycled) 0 else it.allocationByteCount.coerceAtLeast(it.byteCount)
    }

    // Limit simultaneous native image-provider/decoder work while scrolling a large gallery.
    private val decodeSlots = Semaphore(2, true)

    fun load(
        context: Context,
        media: MediaFile,
        targetWidth: Int = 320,
        targetHeight: Int = 240
    ): Bitmap? {
        val request = MediaThumbnailPolicy.boundedSize(targetWidth, targetHeight)
        val cacheKey = "${media.uri}#${request.width}x${request.height}"
        cache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        // If both decode slots are busy, keep the lightweight placeholder instead of queuing
        // dozens of native decoders that may outlive a fast scroll.
        if (!decodeSlots.tryAcquire()) return null
        return try {
            // A prior composition may have filled the cache while this request waited for a slot.
            cache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return cached
            }

            val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val providerThumbnail = try {
                    context.contentResolver.loadThumbnail(
                        media.uri,
                        Size(request.width, request.height),
                        null
                    )
                } catch (_: OutOfMemoryError) {
                    return null
                } catch (_: Exception) {
                    null
                }
                providerThumbnail?.fitInside(request.width, request.height)
                    ?: loadImageFallback(context, media, request.width, request.height)
            } else {
                loadImageFallback(context, media, request.width, request.height)
            }

            if (thumbnail != null && !thumbnail.isRecycled) {
                cache.put(cacheKey, thumbnail)
            }
            thumbnail
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        } finally {
            decodeSlots.release()
        }
    }

    private fun loadImageFallback(
        context: Context,
        media: MediaFile,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        // Do not invoke a video decoder just to decorate the selection list.
        if (media.isVideo || !media.isImage) return null
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

        // Older Android fallback: inspect dimensions first, then decode a reduced bitmap.
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
        while (width / (sampleSize * 2) >= targetWidth * 2 ||
            height / (sampleSize * 2) >= targetHeight * 2
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
        if (scaled !== this && !isRecycled) recycle()
        return scaled
    }
}

data class MediaThumbnailSize(val width: Int, val height: Int)

object MediaThumbnailPolicy {
    const val MAX_CACHE_BYTES = 8 * 1024 * 1024

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
