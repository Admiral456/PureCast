package com.admiral456.purecast.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaImportPolicyTest {
    @Test
    fun deduplicatesSelectedUrisBeforeReadingMetadata() {
        val plan = MediaImportPolicy.plan(
            selectedUriKeys = listOf("a", "b", "a", "c", "b"),
            existingUriKeys = emptySet(),
            currentItemCount = 0,
            maxItems = 50,
            appendToLibrary = false
        )

        assertEquals(listOf("a", "b", "c"), plan.acceptedUriKeys)
        assertEquals(0, plan.alreadyPresentCount)
        assertEquals(0, plan.skippedByLimit)
    }

    @Test
    fun skipsAlreadyImportedUrisWithoutUsingTheRemainingCapacity() {
        val plan = MediaImportPolicy.plan(
            selectedUriKeys = listOf("a", "b", "c", "d"),
            existingUriKeys = setOf("a", "c"),
            currentItemCount = 48,
            maxItems = 50,
            appendToLibrary = true
        )

        assertEquals(listOf("b", "d"), plan.acceptedUriKeys)
        assertEquals(2, plan.alreadyPresentCount)
        assertEquals(0, plan.skippedByLimit)
        assertEquals(2, plan.availableSlots)
    }

    @Test
    fun enforcesTheLibraryLimitBeforeAnyMetadataWork() {
        val keys = (1..80).map { "uri-$it" }
        val plan = MediaImportPolicy.plan(
            selectedUriKeys = keys,
            existingUriKeys = emptySet(),
            currentItemCount = 0,
            maxItems = 50,
            appendToLibrary = false
        )

        assertEquals(50, plan.acceptedUriKeys.size)
        assertEquals(30, plan.skippedByLimit)
        assertEquals(keys.take(50), plan.acceptedUriKeys)
    }

    @Test
    fun fullLibraryReturnsNoUrisForAnAppendSelection() {
        val plan = MediaImportPolicy.plan(
            selectedUriKeys = listOf("new-a", "new-b"),
            existingUriKeys = emptySet(),
            currentItemCount = 50,
            maxItems = 50,
            appendToLibrary = true
        )

        assertEquals(emptyList<String>(), plan.acceptedUriKeys)
        assertEquals(2, plan.skippedByLimit)
        assertEquals(0, plan.availableSlots)
    }
}
