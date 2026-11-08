package com.admiral456.purecast.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import kotlin.math.roundToInt

object MediaThumbnailLoader {
    fun load(
        context: Context,
        media: MediaFile,
        targetWidth: Int = 320,
        targetHeight: Int = 240
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val providerThumbnail = runCatching {
                context.contentResolver.loadThumbnail(
                    media.uri,
                    Size(targetWidth, targetHeight),
                    null
                )
            }.getOrNull()
            if (providerThumbnail != null) return providerThumbnail
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
                val sourceWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val sourceHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val pixels = sourceWidth?.toLong()?.times(sourceHeight?.toLong() ?: 0L) ?: 0L
                if (pixels > MAX_UNSCALED_VIDEO_PIXELS) null
                else retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
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
                val modernThumbnail = runCatching {
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
                }.getOrNull()
                if (modernThumbnail != null) return modernThumbnail.fitInside(targetWidth, targetHeight)
            }

            // Fall back to BitmapFactory for formats handled by legacy decoders
            // when ImageDecoder cannot create a preview on this device.
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
        if (scaled !== this) recycle()
        return scaled
    }

    private const val MAX_UNSCALED_VIDEO_PIXELS = 12_000_000L
}
