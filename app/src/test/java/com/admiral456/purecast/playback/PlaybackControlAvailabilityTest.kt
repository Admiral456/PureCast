package com.admiral456.purecast.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControlAvailabilityTest {
    @Test
    fun unknownStateKeepsControlsAvailable() {
        assertTrue(PlaybackControlAvailability.canPause(null))
        assertTrue(PlaybackControlAvailability.canResume("UNKNOWN"))
        assertTrue(PlaybackControlAvailability.canStop(null))
    }

    @Test
    fun playingStateAllowsPauseButNotResume() {
        assertTrue(PlaybackControlAvailability.canPause("PLAYING"))
        assertFalse(PlaybackControlAvailability.canResume("PLAYING"))
        assertTrue(PlaybackControlAvailability.canStop("PLAYING"))
    }

    @Test
    fun pausedStateAllowsResumeButNotPause() {
        assertFalse(PlaybackControlAvailability.canPause("PAUSED_PLAYBACK"))
        assertTrue(PlaybackControlAvailability.canResume("PAUSED_PLAYBACK"))
        assertTrue(PlaybackControlAvailability.canStop("PAUSED_PLAYBACK"))
    }

    @Test
    fun stoppedStateDisablesTransportActions() {
        for (state in listOf("STOPPED", "NO_MEDIA_PRESENT")) {
            assertFalse(PlaybackControlAvailability.canPause(state))
            assertFalse(PlaybackControlAvailability.canResume(state))
            assertFalse(PlaybackControlAvailability.canStop(state))
        }
    }

    @Test
    fun transitioningStateDoesNotOfferPauseOrResume() {
        assertFalse(PlaybackControlAvailability.canPause("TRANSITIONING"))
        assertFalse(PlaybackControlAvailability.canResume("TRANSITIONING"))
    }
}
