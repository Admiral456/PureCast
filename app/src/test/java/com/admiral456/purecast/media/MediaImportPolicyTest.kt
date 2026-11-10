package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaImportPolicyTest {
    @Test
    fun boundedDistinctKeepsFirstOccurrenceAndOrder() {
        assertEquals(
            listOf("photo-1", "photo-2", "video-1"),
            MediaImportPolicy.boundedDistinct(
                listOf("photo-1", "photo-2", "photo-1", "video-1"),
                maxItems = 10,
                keyOf = { it }
            )
        )
    }

    @Test
    fun boundedDistinctStopsAtMaximum() {
        val selected = (1..500).map { "media-$it" }
        assertEquals(
            (1..50).map { "media-$it" },
            MediaImportPolicy.boundedDistinct(selected, maxItems = 50, keyOf = { it })
        )
    }

    @Test
    fun duplicatesDoNotConsumeAdditionalSlots() {
        assertEquals(
            listOf("a", "b", "c"),
            MediaImportPolicy.boundedDistinct(
                listOf("a", "a", "b", "b", "c", "d"),
                maxItems = 3,
                keyOf = { it }
            )
        )
    }

    @Test
    fun nonPositiveMaximumReturnsEmptyList() {
        assertTrue(MediaImportPolicy.boundedDistinct(listOf("a"), 0) { it }.isEmpty())
        assertTrue(MediaImportPolicy.boundedDistinct(listOf("a"), -1) { it }.isEmpty())
    }

    @Test
    fun availableSlotsNeverReturnsANegativeValue() {
        assertEquals(0, MediaImportPolicy.availableSlots(currentCount = 60, maximum = 50))
        assertEquals(0, MediaImportPolicy.availableSlots(currentCount = 5, maximum = 0))
        assertEquals(45, MediaImportPolicy.availableSlots(currentCount = 5, maximum = 50))
    }
}
