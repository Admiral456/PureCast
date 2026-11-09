package com.admiral456.purecast.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkRouteSelectorTest {
    @Test
    fun mostSpecificConnectedRouteWins() {
        val selected = NetworkRouteSelector.bestMatch(
            listOf(
                NetworkRouteMatch(0, 24, true, true),
                NetworkRouteMatch(1, 28, false, false),
                NetworkRouteMatch(2, 16, true, false)
            )
        )
        assertEquals(1, selected)
    }

    @Test
    fun wifiWinsWhenPrefixLengthsAreEqual() {
        val selected = NetworkRouteSelector.bestMatch(
            listOf(
                NetworkRouteMatch(0, 24, false, true),
                NetworkRouteMatch(1, 24, true, false)
            )
        )
        assertEquals(1, selected)
    }

    @Test
    fun activeNetworkBreaksOtherwiseEqualTies() {
        val selected = NetworkRouteSelector.bestMatch(
            listOf(
                NetworkRouteMatch(0, 24, true, false),
                NetworkRouteMatch(1, 24, true, true)
            )
        )
        assertEquals(1, selected)
    }

    @Test
    fun defaultAndInvalidRoutesAreIgnored() {
        val selected = NetworkRouteSelector.bestMatch(
            listOf(
                NetworkRouteMatch(0, 0, true, true),
                NetworkRouteMatch(1, 33, true, false),
                NetworkRouteMatch(-1, 24, true, false)
            )
        )
        assertNull(selected)
    }

    @Test
    fun hotspotSoftApInterfaceRanksAheadOfStationWifi() {
        assertEquals(0, LocalNetworkInterfaceSelector.priority("ap0"))
        assertEquals(1, LocalNetworkInterfaceSelector.priority("wlan0"))
    }
}
