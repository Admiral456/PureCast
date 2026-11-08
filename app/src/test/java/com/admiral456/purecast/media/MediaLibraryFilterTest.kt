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
        val old = entry("old.jpg", modified = 1_000L)
        val recent = entry("recent.jpg", modified = 3_000L)
        val unknown = entry("unknown.jpg")

        val result = MediaLibraryFilter.sort(
            listOf(old, unknown, recent),
            MediaLibrarySort.NEWEST,
            TestEntry::metadata
        )

        assertEquals(listOf("recent.jpg", "old.jpg", "unknown.jpg"), result.map { it.name })
    }

    @Test
    fun sortsByNameWithoutCaseSensitivity() {
        val result = MediaLibraryFilter.sort(
            listOf(entry("zebra.jpg"), entry("Apple.jpg"), entry("banana.jpg")),
            MediaLibrarySort.NAME_ASCENDING,
            TestEntry::metadata
        )
        assertEquals(listOf("Apple.jpg", "banana.jpg", "zebra.jpg"), result.map { it.name })
    }

    @Test
    fun sortsByFileSizeAndKeepsUnknownSizesAtEnd() {
        val small = entry("small.jpg", size = 100L)
        val large = entry("large.mp4", mime = "video/mp4", size = 900L)
        val unknown = entry("unknown.jpg")

        val largestFirst = MediaLibraryFilter.sort(
            listOf(small, unknown, large),
            MediaLibrarySort.LARGEST_FIRST,
            TestEntry::metadata
        )
        assertEquals(listOf("large.mp4", "small.jpg", "unknown.jpg"), largestFirst.map { it.name })

        val smallestFirst = MediaLibraryFilter.sort(
            listOf(large, unknown, small),
            MediaLibrarySort.SMALLEST_FIRST,
            TestEntry::metadata
        )
        assertEquals(listOf("small.jpg", "large.mp4", "unknown.jpg"), smallestFirst.map { it.name })
    }

    @Test
    fun filtersByAlbumCaseInsensitivelyAndListsUniqueAlbums() {
        val first = entry("one.jpg", album = "Dovolená")
        val second = entry("two.jpg", album = "dovolená")
        val third = entry("work.jpg", album = "Práce")

        val result = listOf(first, second, third).filterByAlbum("DOVOLENÁ") { it.album }
        assertEquals(listOf("one.jpg", "two.jpg"), result.map { it.name })
        assertEquals(
            listOf("Dovolená", "Práce"),
            MediaLibraryFilter.albumNames(listOf(first.album, second.album, third.album))
        )
    }

    @Test
    fun blankSearchMatchesEveryItemInSelectedKind() {
        assertTrue(MediaLibraryFilter.matches("photo.jpg", "image/jpeg", "   ", MediaKindFilter.ALL))
    }

    private data class TestEntry(
        val name: String,
        val mime: String,
        val size: Long?,
        val modified: Long?,
        val album: String?
    ) {
        fun metadata() = MediaLibraryMetadata(name, mime, size, modified, album)
    }

    private fun entry(
        name: String,
        mime: String = "image/jpeg",
        size: Long? = null,
        modified: Long? = null,
        album: String? = null
    ) = TestEntry(name, mime, size, modified, album)
}
