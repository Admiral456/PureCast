package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
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
    fun sortsByNewestAndKeepsUnknownDatesAtEnd() {
        val old = media("old.jpg", modified = 1_000L)
        val recent = media("recent.jpg", modified = 3_000L)
        val unknown = media("unknown.jpg")

        val result = MediaLibraryFilter.apply(
            listOf(old, unknown, recent),
            "",
            MediaKindFilter.ALL,
            MediaLibrarySort.NEWEST
        )

        assertEquals(listOf("recent.jpg", "old.jpg", "unknown.jpg"), result.map { it.displayName })
    }

    @Test
    fun sortsByNameWithoutCaseSensitivity() {
        val result = MediaLibraryFilter.apply(
            listOf(media("zebra.jpg"), media("Apple.jpg"), media("banana.jpg")),
            "",
            MediaKindFilter.ALL,
            MediaLibrarySort.NAME_ASCENDING
        )
        assertEquals(listOf("Apple.jpg", "banana.jpg", "zebra.jpg"), result.map { it.displayName })
    }

    @Test
    fun sortsByFileSizeAndKeepsUnknownSizesAtEnd() {
        val small = media("small.jpg", size = 100L)
        val large = media("large.mp4", "video/mp4", size = 900L)
        val unknown = media("unknown.jpg")

        val largestFirst = MediaLibraryFilter.apply(
            listOf(small, unknown, large),
            "",
            MediaKindFilter.ALL,
            MediaLibrarySort.LARGEST_FIRST
        )
        assertEquals(listOf("large.mp4", "small.jpg", "unknown.jpg"), largestFirst.map { it.displayName })

        val smallestFirst = MediaLibraryFilter.apply(
            listOf(large, unknown, small),
            "",
            MediaKindFilter.ALL,
            MediaLibrarySort.SMALLEST_FIRST
        )
        assertEquals(listOf("small.jpg", "large.mp4", "unknown.jpg"), smallestFirst.map { it.displayName })
    }

    @Test
    fun filtersByAlbumCaseInsensitivelyAndListsUniqueAlbums() {
        val first = media("one.jpg", album = "Dovolená")
        val second = media("two.jpg", album = "dovolená")
        val third = media("work.jpg", album = "Práce")

        val result = MediaLibraryFilter.apply(
            listOf(first, second, third),
            "",
            MediaKindFilter.ALL,
            MediaLibrarySort.NAME_ASCENDING,
            album = "DOVOLENÁ"
        )

        assertEquals(listOf("one.jpg", "two.jpg"), result.map { it.displayName })
        assertEquals(listOf("Dovolená", "Práce"), MediaLibraryFilter.albums(listOf(first, second, third)))
    }

    private fun media(
        name: String,
        mime: String = "image/jpeg",
        size: Long? = null,
        modified: Long? = null,
        album: String? = null
    ) = MediaFile(
        uri = android.net.Uri.parse("content://test/$name"),
        displayName = name,
        mimeType = mime,
        sizeBytes = size,
        dateModifiedMillis = modified,
        albumName = album
    )

    @Test
    fun blankSearchMatchesEveryItemInSelectedKind() {
        assertTrue(MediaLibraryFilter.matches("photo.jpg", "image/jpeg", "   ", MediaKindFilter.ALL))
    }
}
