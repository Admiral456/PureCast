package com.admiral456.purecast.media

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
data class MediaTechnicalInfo(
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Double? = null,
    val videoMimeType: String? = null,
    val audioMimeTypes: List<String> = emptyList(),
    val bitrateBitsPerSecond: Long? = null,
    val durationMillis: Long? = null,
    val hdrLabel: String? = null,
    val rotationDegrees: Int? = null
) {
    fun resolutionLabel(): String? =
        if (width != null && width > 0 && height != null && height > 0) "${width} × ${height}" else null

    fun summary(isVideo: Boolean): String? {
        val values = buildList {
            resolutionLabel()?.let(::add)
            frameRate?.takeIf { it.isFinite() && it > 0.0 }?.let {
                add(String.format(Locale.US, "%.2f FPS", it))
            }
            videoMimeType?.takeIf { it.isNotBlank() }?.let(::add)
            audioMimeTypes.firstOrNull()?.let(::add)
            bitrateBitsPerSecond?.takeIf { it > 0L }?.let {
                add(String.format(Locale.US, "%.1f Mbps", it / 1_000_000.0))
            }
            durationMillis?.takeIf { it > 0L }?.let {
                val seconds = it / 1_000L
                add("Délka %02d:%02d:%02d".format(
                    seconds / 3_600L,
                    (seconds % 3_600L) / 60L,
                    seconds % 60L
                ))
            }
            if (isVideo) add("HDR ${hdrLabel ?: "neověřeno"}")
        }
        return values.take(5).joinToString(" · ").ifBlank { null }
    }
}

object MediaInspector {
    suspend fun inspectLimited(context: Context, media: MediaFile): MediaTechnicalInfo? =
        MediaProcessingGate.withPermit {
            // Let one requested native inspection finish, then discard the result if its
            // card left composition or the import screen was closed.
            val result = withContext(NonCancellable + Dispatchers.IO) {
                inspect(context, media)
            }
            currentCoroutineContext().ensureActive()
            result
        }

    fun inspect(context: Context, media: MediaFile): MediaTechnicalInfo? =
        try {
            inspectInternal(context, media)
        } catch (_: OutOfMemoryError) {
            // Metadata is supplementary. Keep the selected original media usable without it.
            null
        } catch (_: Exception) {
            null
        }

    private fun inspectInternal(context: Context, media: MediaFile): MediaTechnicalInfo? {
        if (media.isImage) return inspectImage(context, media)
        if (!media.isVideo) return null

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, media.uri, emptyMap())
            var width: Int? = null
            var height: Int? = null
            var frameRate: Double? = null
            var videoMime: String? = null
            var bitrate: Long? = null
            var durationMs: Long? = null
            var hdrLabel: String? = null
            var rotation: Int? = null
            val audioTracks = mutableListOf<String>()

            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }.getOrNull() ?: continue
                if (mime.startsWith("video/") && videoMime == null) {
                    videoMime = mime
                    width = intValue(format, MediaFormat.KEY_WIDTH)
                    height = intValue(format, MediaFormat.KEY_HEIGHT)
                    frameRate = floatValue(format, MediaFormat.KEY_FRAME_RATE)?.toDouble()
                    bitrate = intValue(format, MediaFormat.KEY_BIT_RATE)?.toLong()
                    durationMs = longValue(format, MediaFormat.KEY_DURATION)?.div(1_000L)
                    rotation = intValue(format, MediaFormat.KEY_ROTATION)
                    hdrLabel = intValue(format, MediaFormat.KEY_COLOR_TRANSFER)?.let(::hdrName)
                } else if (mime.startsWith("audio/")) {
                    audioTracks.add(mime)
                }
            }

            MediaTechnicalInfo(
                width = width,
                height = height,
                frameRate = frameRate,
                videoMimeType = videoMime,
                audioMimeTypes = audioTracks.distinct(),
                bitrateBitsPerSecond = bitrate,
                durationMillis = durationMs,
                hdrLabel = hdrLabel,
                rotationDegrees = rotation
            )
        } catch (_: Exception) {
            inspectWithRetriever(context, media)
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun inspectImage(context: Context, media: MediaFile): MediaTechnicalInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dimensions = intArrayOf(0, 0)
            val decoded = runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, media.uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    dimensions[0] = info.size.width
                    dimensions[1] = info.size.height
                    // Read header dimensions without allocating a full-resolution bitmap.
                    decoder.setTargetSize(1, 1)
                }
            }.isSuccess
            if (decoded && dimensions[0] > 0 && dimensions[1] > 0) {
                return MediaTechnicalInfo(width = dimensions[0], height = dimensions[1])
            }
        }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(media.uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) null
            else MediaTechnicalInfo(width = options.outWidth, height = options.outHeight)
        } catch (_: Exception) {
            null
        }
    }

    private fun inspectWithRetriever(context: Context, media: MediaFile): MediaTechnicalInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, media.uri)
            MediaTechnicalInfo(
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDoubleOrNull(),
                bitrateBitsPerSecond = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
                durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun intValue(format: MediaFormat, key: String): Int? =
        runCatching { format.getInteger(key) }.getOrNull()

    private fun longValue(format: MediaFormat, key: String): Long? =
        runCatching { format.getLong(key) }.getOrNull()

    private fun floatValue(format: MediaFormat, key: String): Float? =
        runCatching { format.getFloat(key) }.getOrNull()
            ?: intValue(format, key)?.toFloat()

    private fun hdrName(transfer: Int): String? = when (transfer) {
        MediaFormat.COLOR_TRANSFER_ST2084 -> "PQ / HDR10"
        MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
        else -> null
    }
}
