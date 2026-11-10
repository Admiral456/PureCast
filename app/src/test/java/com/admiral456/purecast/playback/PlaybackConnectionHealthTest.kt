package com.admiral456.purecast.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackConnectionHealthTest {
    @Test
    fun healthySessionDoesNotShowWarning() {
        assertNull(PlaybackConnectionHealth().warningMessage)
    }

    @Test
    fun singleFailedPollDoesNotCreatePrematureWarning() {
        val state = PlaybackConnectionHealthPolicy.transportFailed(PlaybackConnectionHealth())
        assertEquals(1, state.transportFailures)
        assertNull(state.warningMessage)
    }

    @Test
    fun repeatedTransportFailuresWarnWithoutClaimingStreamStopped() {
        val first = PlaybackConnectionHealthPolicy.transportFailed(PlaybackConnectionHealth())
        val second = PlaybackConnectionHealthPolicy.transportFailed(first)
        assertTrue(second.warningMessage!!.contains("přenos nepřerušuje"))
        assertEquals(2, second.transportFailures)
    }

    @Test
    fun missingPositionHasDifferentNonFatalWarning() {
        val first = PlaybackConnectionHealthPolicy.positionFailed(PlaybackConnectionHealth())
        val second = PlaybackConnectionHealthPolicy.positionFailed(first)
        assertTrue(second.warningMessage!!.contains("časovou pozici"))
        assertTrue(second.warningMessage!!.contains("může pokračovat"))
        assertEquals(0, second.transportFailures)
    }

    @Test
    fun successfulTransportPollClearsOnlyTransportFailures() {
        val failed = PlaybackConnectionHealth(transportFailures = 2, positionFailures = 1)
        val recovered = PlaybackConnectionHealthPolicy.transportSucceeded(failed)
        assertEquals(0, recovered.transportFailures)
        assertEquals(1, recovered.positionFailures)
        assertNull(recovered.warningMessage)
    }

    @Test
    fun successfulPositionPollClearsPositionWarning() {
        val failed = PlaybackConnectionHealth(transportFailures = 0, positionFailures = 2)
        val recovered = PlaybackConnectionHealthPolicy.positionSucceeded(failed)
        assertEquals(0, recovered.positionFailures)
        assertNull(recovered.warningMessage)
    }

    @Test
    fun countersStayBoundedDuringLongOutages() {
        var state = PlaybackConnectionHealth()
        repeat(20) {
            state = PlaybackConnectionHealthPolicy.transportFailed(state)
            state = PlaybackConnectionHealthPolicy.positionFailed(state)
        }
        assertEquals(PlaybackConnectionHealth.MAX_RECORDED_FAILURES, state.transportFailures)
        assertEquals(PlaybackConnectionHealth.MAX_RECORDED_FAILURES, state.positionFailures)
        assertTrue(state.warningMessage != null)
    }
}
