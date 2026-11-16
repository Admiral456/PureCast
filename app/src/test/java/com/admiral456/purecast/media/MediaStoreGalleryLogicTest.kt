package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreGalleryLogicTest {
    @Test
    fun mergesImagesAndVideosByNewestDateAndRemovesDuplicateUris() {
        val result = MediaStoreGalleryLogic.merge(
            listOf(
                MediaStoreEntry("content://images/1", 10),
                MediaStoreEntry("content://videos/2", 30),
                MediaStoreEntry("content://images/3", 20),
                MediaStoreEntry("content://images/1", 10)
            ),
            limit = 10
        )

        assertEquals(
            listOf("content://videos/2", "content://images/3", "content://images/1"),
            result
        )
    }

    @Test
    fun respectsLimitAfterGlobalSorting() {
        val result = MediaStoreGalleryLogic.merge(
            listOf(
                MediaStoreEntry("old", 1),
                MediaStoreEntry("newest", 9),
                MediaStoreEntry("middle", 5)
            ),
            limit = 2
        )

        assertEquals(listOf("newest", "middle"), result)
    }

    @Test
    fun ignoresBlankUrisAndNonPositiveLimits() {
        assertEquals(
            listOf("valid"),
            MediaStoreGalleryLogic.merge(
                listOf(MediaStoreEntry("", 100), MediaStoreEntry("valid", 1)),
                limit = 1
            )
        )
        assertEquals(
            emptyList<String>(),
            MediaStoreGalleryLogic.merge(listOf(MediaStoreEntry("valid", 1)), limit = 0)
        )
    }
}
