package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaThumbnailPolicyTest {
    @Test
    fun normalThumbnailDimensionsRemainUnchanged() {
        assertEquals(
            MediaThumbnailSize(320, 240),
            MediaThumbnailPolicy.boundedSize(320, 240)
        )
    }

    @Test
    fun oversizedThumbnailDimensionsAreClamped() {
        assertEquals(
            MediaThumbnailSize(512, 512),
            MediaThumbnailPolicy.boundedSize(8_000, 4_000)
        )
    }

    @Test
    fun invalidThumbnailDimensionsAreMadeSafe() {
        assertEquals(
            MediaThumbnailSize(1, 1),
            MediaThumbnailPolicy.boundedSize(0, -50)
        )
    }

    @Test
    fun maximumEdgeCannotBeConfiguredBelowOne() {
        assertEquals(
            MediaThumbnailSize(1, 1),
            MediaThumbnailPolicy.boundedSize(100, 100, maximumEdge = 0)
        )
    }

    @Test
    fun thumbnailCacheHasAFixedSmallMemoryBudget() {
        assertEquals(8 * 1024 * 1024, MediaThumbnailPolicy.MAX_CACHE_BYTES)
    }

    @Test
    fun cacheIsClearedWhenAndroidReportsRunningLowMemory() {
        assertEquals(false, MediaThumbnailPolicy.shouldClearCacheOnTrim(0))
        assertEquals(false, MediaThumbnailPolicy.shouldClearCacheOnTrim(9))
        assertEquals(true, MediaThumbnailPolicy.shouldClearCacheOnTrim(10))
        assertEquals(true, MediaThumbnailPolicy.shouldClearCacheOnTrim(20))
    }
}
