package com.admiral456.purecast.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueLogicTest {
    @Test
    fun nextStartsAtFirstItemWhenNoCurrentSelection() {
        assertEquals(0, PlaybackQueueLogic.nextIndex(-1, 3))
    }

    @Test
    fun nextWrapsAtEnd() {
        assertEquals(0, PlaybackQueueLogic.nextIndex(2, 3))
    }

    @Test
    fun previousWrapsAtBeginning() {
        assertEquals(2, PlaybackQueueLogic.previousIndex(0, 3))
    }

    @Test
    fun emptyQueueHasNoNextOrPrevious() {
        assertEquals(null, PlaybackQueueLogic.nextIndex(-1, 0))
        assertEquals(null, PlaybackQueueLogic.previousIndex(-1, 0))
    }

    @Test
    fun removingCurrentItemClearsCurrentIndex() {
        assertEquals(-1, PlaybackQueueLogic.indexAfterRemoval(1, 1, 2))
    }

    @Test
    fun removingItemBeforeCurrentMovesIndexBack() {
        assertEquals(1, PlaybackQueueLogic.indexAfterRemoval(2, 0, 2))
    }

    @Test
    fun movingCurrentItemMovesItsIndex() {
        assertEquals(2, PlaybackQueueLogic.indexAfterMove(0, 0, 2, 3))
    }

    @Test
    fun movingItemAcrossCurrentAdjustsIndex() {
        assertEquals(0, PlaybackQueueLogic.indexAfterMove(1, 0, 2, 3))
        assertEquals(2, PlaybackQueueLogic.indexAfterMove(1, 2, 0, 3))
    }
}
