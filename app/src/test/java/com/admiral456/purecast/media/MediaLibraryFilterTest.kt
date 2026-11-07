package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaLibraryFilterTest {
    @Test
    fun filtersByNameCaseInsensitively() {
        val media = listOf(
            sample("holiday-video.mp4", "video/mp4"),
            sample("family-photo.jpg", "image/jpeg")
        )
        assertEquals(
            listOf("holiday-video.mp4"),
            MediaLibraryFilter.filter(media, "HOLIDAY", MediaKindFilter.ALL).map { it.displayName }
        )
    }

    @Test
    fun filtersOnlyImages() {
        val media = listOf(
            sample("video.mp4", "video/mp4"),
            sample("photo.jpg", "image/jpeg")
        )
        assertEquals(
            listOf("photo.jpg"),
            MediaLibraryFilter.filter(media, "", MediaKindFilter.PHOTOS).map { it.displayName }
        )
    }

    @Test
    fun filtersOnlyVideos() {
        val media = listOf(
            sample("video.mp4", "video/mp4"),
            sample("photo.jpg", "image/jpeg")
        )
        assertEquals(
            listOf("video.mp4"),
            MediaLibraryFilter.filter(media, "", MediaKindFilter.VIDEOS).map { it.displayName }
        )
    }

    @Test
    fun combinesSearchAndKindFilter() {
        val media = listOf(
            sample("holiday-video.mp4", "video/mp4"),
            sample("holiday-photo.jpg", "image/jpeg"),
            sample("work-video.mp4", "video/mp4")
        )
        assertEquals(
            listOf("holiday-video.mp4"),
            MediaLibraryFilter.filter(media, "holiday", MediaKindFilter.VIDEOS).map { it.displayName }
        )
    }

    private fun sample(name: String, mime: String) =
        MediaFile(android.net.Uri.parse("content://test/$name"), name, mime, 100L)
}
