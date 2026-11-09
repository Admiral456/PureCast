package com.admiral456.purecast.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupQueueLogicTest {
    private val entries = listOf(
        entry("one"),
        entry("two"),
        entry("three")
    )

    @Test
    fun nextAndPreviousRespectQueueBoundaries() {
        assertEquals(0, GroupQueueLogic.nextIndex(-1, 3))
        assertEquals(1, GroupQueueLogic.nextIndex(0, 3))
        assertNull(GroupQueueLogic.nextIndex(2, 3))
        assertNull(GroupQueueLogic.nextIndex(0, 0))

        assertNull(GroupQueueLogic.previousIndex(0, 3))
        assertEquals(1, GroupQueueLogic.previousIndex(2, 3))
        assertNull(GroupQueueLogic.previousIndex(-1, 3))
        assertNull(GroupQueueLogic.previousIndex(0, 0))
    }

    @Test
    fun movingEntryPreservesAllEntriesAndOrder() {
        val moved = GroupQueueLogic.move(entries, 0, 2)
        assertEquals(listOf("two", "three", "one"), moved.map { it.id })
        assertEquals(listOf("one", "two", "three"), entries.map { it.id })
    }

    @Test
    fun invalidMoveReturnsOriginalList() {
        assertEquals(entries, GroupQueueLogic.move(entries, -1, 2))
        assertEquals(entries, GroupQueueLogic.move(entries, 0, 3))
        assertEquals(entries, GroupQueueLogic.move(entries, 1, 1))
    }

    @Test
    fun removingParticipantRemovesOnlyTheirEntriesAndClearsTheirCurrentPlayback() {
        val mixedEntries = listOf(
            entry("one", sender = "phone-a"),
            entry("two", sender = "phone-b"),
            entry("three", sender = "phone-a")
        )
        val removed = GroupQueueLogic.entriesFromParticipant(mixedEntries, "phone-a")

        assertEquals(listOf("one", "three"), removed.map { it.id })
        assertEquals(
            listOf("two"),
            mixedEntries.filterNot { it in removed }
        )
        assertEquals(null, GroupQueueLogic.currentEntryAfterParticipantRemoval("three", removed))
        assertEquals("two", GroupQueueLogic.currentEntryAfterParticipantRemoval("two", removed))
    }

    @Test
    fun currentAndNavigationResolveByStableEntryId() {
        assertEquals(1, GroupQueueLogic.currentIndex(entries, "two"))
        assertEquals(-1, GroupQueueLogic.currentIndex(entries, "missing"))
        assertEquals("three", GroupQueueLogic.nextEntry(entries, "two")?.id)
        assertEquals("one", GroupQueueLogic.previousEntry(entries, "two")?.id)
        assertNull(GroupQueueLogic.nextEntry(entries, "three"))
    }

    private fun entry(id: String, sender: String = "sender") = GroupQueueEntry(
        id = id,
        title = id,
        mimeType = "video/mp4",
        mediaUrl = "http://192.168.1.20:1234/media/abcdefgh1234",
        sizeBytes = 100L,
        senderParticipantId = sender,
        senderName = "Telefon",
        addedAtEpochMillis = 1L
    )
}
