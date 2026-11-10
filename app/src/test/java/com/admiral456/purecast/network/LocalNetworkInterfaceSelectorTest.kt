package com.admiral456.purecast.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalNetworkInterfaceSelectorTest {
    @Test
    fun hotspotInterfacesArePreferredWhenNoWifiNetworkObjectExists() {
        assertEquals(0, LocalNetworkInterfaceSelector.priority("ap0"))
        assertEquals(0, LocalNetworkInterfaceSelector.priority("softap0"))
        assertEquals(0, LocalNetworkInterfaceSelector.priority("swlan0"))
    }

    @Test
    fun wifiInterfacesRankAheadOfWiredFallbacks() {
        assertEquals(1, LocalNetworkInterfaceSelector.priority("wlan0"))
        assertEquals(1, LocalNetworkInterfaceSelector.priority("wifi0"))
        assertEquals(2, LocalNetworkInterfaceSelector.priority("eth0"))
        assertEquals(2, LocalNetworkInterfaceSelector.priority("en0"))
        assertEquals(2, LocalNetworkInterfaceSelector.priority("rndis0"))
        assertEquals(3, LocalNetworkInterfaceSelector.priority("br0"))
    }

    @Test
    fun cellularVpnLoopbackAndUnknownInterfacesAreNeverPublished() {
        assertNull(LocalNetworkInterfaceSelector.priority("rmnet_data0"))
        assertNull(LocalNetworkInterfaceSelector.priority("ccmni0"))
        assertNull(LocalNetworkInterfaceSelector.priority("ppp0"))
        assertNull(LocalNetworkInterfaceSelector.priority("tun0"))
        assertNull(LocalNetworkInterfaceSelector.priority("lo"))
        assertNull(LocalNetworkInterfaceSelector.priority("unknown0"))
    }

    @Test
    fun interfaceMatchingIsCaseInsensitive() {
        assertEquals(1, LocalNetworkInterfaceSelector.priority("WLAN0"))
        assertEquals(0, LocalNetworkInterfaceSelector.priority("AP0"))
    }
}
