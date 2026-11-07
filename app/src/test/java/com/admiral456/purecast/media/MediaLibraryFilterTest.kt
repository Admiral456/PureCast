package com.admiral456.purecast.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryFilterTest {
    @Test
    fun filtersByNameCaseInsensitively() {
        assertTrue(MediaLibraryFilter.matches("holiday-video.mp4", "video/mp4", "HOLIDAY", MediaKindFilter.ALL))
        assertFalse(MediaLibraryFilter.matches("family-photo.jpg", "image/jpeg", "HOLIDAY", MediaKindFilter.ALL))
    }

    @Test
    fun filtersOnlyImages() {
        assertTrue(MediaLibraryFilter.matches("photo.jpg", "image/jpeg", "", MediaKindFilter.PHOTOS))
        assertFalse(MediaLibraryFilter.matches("video.mp4", "video/mp4", "", MediaKindFilter.PHOTOS))
    }

    @Test
    fun filtersOnlyVideos() {
        assertTrue(MediaLibraryFilter.matches("video.mp4", "video/mp4", "", MediaKindFilter.VIDEOS))
        assertFalse(MediaLibraryFilter.matches("photo.jpg", "image/jpeg", "", MediaKindFilter.VIDEOS))
    }

    @Test
    fun combinesSearchAndKindFilter() {
        assertTrue(MediaLibraryFilter.matches("holiday-video.mp4", "video/mp4", "holiday", MediaKindFilter.VIDEOS))
        assertFalse(MediaLibraryFilter.matches("holiday-photo.jpg", "image/jpeg", "holiday", MediaKindFilter.VIDEOS))
        assertFalse(MediaLibraryFilter.matches("work-video.mp4", "video/mp4", "holiday", MediaKindFilter.VIDEOS))
    }

    @Test
    fun blankSearchMatchesEveryItemInSelectedKind() {
        assertTrue(MediaLibraryFilter.matches("photo.jpg", "image/jpeg", "   ", MediaKindFilter.ALL))
    }
}
