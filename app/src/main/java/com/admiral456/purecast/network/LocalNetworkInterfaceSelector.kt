package com.admiral456.purecast.network

import java.util.Locale

/**
 * Ranks interfaces that can expose a reachable local IPv4 address.
 * Unrecognized interfaces are intentionally ignored to avoid publishing
 * cellular carrier, VPN, loopback or virtual-only addresses.
 */
internal object LocalNetworkInterfaceSelector {
    fun priority(interfaceName: String): Int? {
        val name = interfaceName.lowercase(Locale.ROOT)
        return when {
            name.startsWith("softap") || name.startsWith("swlan") || name.startsWith("ap") -> 0
            name.startsWith("wlan") || name.startsWith("wifi") -> 1
            name.startsWith("eth") || name.startsWith("en") ||
                name.startsWith("rndis") || name.startsWith("usb") -> 2
            name.startsWith("br") -> 3
            else -> null
        }
    }
}
