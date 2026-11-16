package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaThumbnailPolicyTest {
    @Test
    fun boundsEachDecodeDimensionToTheMaximumEdge() {
        assertEquals(
            MediaThumbnailSize(512, 512),
            MediaThumbnailPolicy.boundedSize(4_000, 2_000)
        )
        assertEquals(
            MediaThumbnailSize(320, 192),
            MediaThumbnailPolicy.boundedSize(320, 192)
        )
    }

    @Test
    fun invalidDimensionsAreClampedToOnePixel() {
        assertEquals(
            MediaThumbnailSize(1, 1),
            MediaThumbnailPolicy.boundedSize(0, -10)
        )
    }

    @Test
    fun maximumEdgeCannotCreateAnInvalidRange() {
        assertEquals(
            MediaThumbnailSize(1, 1),
            MediaThumbnailPolicy.boundedSize(800, 600, maximumEdge = 0)
        )
    }

    @Test
    fun cacheIsClearedAtRunningLowMemoryAndAbove() {
        assertFalse(MediaThumbnailPolicy.shouldClearCacheOnTrim(9))
        assertTrue(MediaThumbnailPolicy.shouldClearCacheOnTrim(10))
        assertTrue(MediaThumbnailPolicy.shouldClearCacheOnTrim(15))
    }

    @Test
    fun thumbnailCacheHasAFixedEightMebibyteBudget() {
        assertEquals(8 * 1024 * 1024, MediaThumbnailPolicy.MAX_CACHE_BYTES)
    }
}
