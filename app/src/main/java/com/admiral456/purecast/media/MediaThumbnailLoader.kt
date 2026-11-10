package com.admiral456.purecast.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object MediaThumbnailLoader {
    // Only two native thumbnail decoders may allocate bitmaps at the same time.
    private val decodeSemaphore = Semaphore(2)

    suspend fun loadLimited(
        context: Context,
        media: MediaFile,
        targetWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        targetHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? = decodeSemaphore.withPermit {
        withContext(Dispatchers.IO) {
            load(context, media, targetWidth, targetHeight)
        }
    }
    fun load(
        context: Context,
        media: MediaFile,
        targetWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        targetHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? {
        return try {
            loadInternal(
                context,
                media,
                targetWidth.coerceIn(1, MAX_THUMBNAIL_DIMENSION),
                targetHeight.coerceIn(1, MAX_THUMBNAIL_DIMENSION)
            )
        } catch (_: OutOfMemoryError) {
            // A preview is optional; never let a large source image or video terminate the gallery.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                try {
                    return providerThumbnail.fitInside(targetWidth, targetHeight)
                } catch (_: OutOfMemoryError) {
                    return null
                } catch (_: Exception) {
                    runCatching { providerThumbnail.recycle() }
                }
            }
        }

        if (media.isVideo) return loadVideoFrame(context, media, targetWidth, targetHeight)
        if (media.isImage) return loadImage(context, media, targetWidth, targetHeight)
        return null
    }

    private fun loadVideoFrame(
        context: Context,
        media: MediaFile,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, media.uri)
            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    -1L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetWidth,
                    targetHeight
                )
            } else {
                // API 26 has no scaled-frame API. Avoid decoding multi-megapixel frames,
                // and skip the preview entirely if the provider does not expose dimensions.
                val sourceWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val sourceHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val pixels = if (sourceWidth != null && sourceHeight != null &&
                    sourceWidth > 0 && sourceHeight > 0
                ) {
                    sourceWidth.toLong() * sourceHeight.toLong()
                } else {
                    Long.MAX_VALUE
                }
                if (pixels <= MAX_UNSCALED_VIDEO_PIXELS) {
                    retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } else {
                    null
                }
            }
            frame?.fitInside(targetWidth, targetHeight)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadImage(
        context: Context,
        media: MediaFile,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
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
                if (modernThumbnail != null) return modernThumbnail.fitInside(targetWidth, targetHeight)
            }

            // Fall back to a sampled BitmapFactory decode; never decode the full-resolution
            // source merely to fill a small gallery tile.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(media.uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, targetWidth, targetHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(media.uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            decoded.fitInside(targetWidth, targetHeight)
        } catch (_: Exception) {
            null
        }
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
        if (scaled !== this) runCatching { recycle() }
        return scaled
    }

    private const val DEFAULT_THUMBNAIL_WIDTH = 256
    private const val DEFAULT_THUMBNAIL_HEIGHT = 192
    private const val MAX_THUMBNAIL_DIMENSION = 512
    private const val MAX_UNSCALED_VIDEO_PIXELS = 2_500_000L
}
