package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaTechnicalInfoTest {
    @Test
    fun videoSummaryIncludesResolutionFpsCodecsAndBitrate() {
        val info = MediaTechnicalInfo(
            width = 3840,
            height = 2160,
            frameRate = 59.94,
            videoMimeType = "video/hevc",
            audioMimeTypes = listOf("audio/eac3"),
            bitrateBitsPerSecond = 24_000_000L,
            durationMillis = 125_000L,
            hdrLabel = "PQ / HDR10"
        )

        assertEquals(
            "3840 × 2160 · 59.94 FPS · video/hevc · audio/eac3 · 24.0 Mbps",
            info.summary(isVideo = true)
        )
    }

    @Test
    fun imageSummaryContainsOnlyPositiveDimensions() {
        assertEquals(
            "8064 × 6048",
            MediaTechnicalInfo(width = 8064, height = 6048).summary(isVideo = false)
        )
        assertNull(MediaTechnicalInfo(width = 0, height = -1).summary(isVideo = false))
    }

    @Test
    fun invalidTechnicalValuesDoNotCreateAnEmptySummary() {
        assertNull(
            MediaTechnicalInfo(frameRate = Double.NaN, bitrateBitsPerSecond = -1L)
                .summary(isVideo = false)
        )
    }
}
