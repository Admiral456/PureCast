package com.admiral456.purecast.network

internal data class NetworkRouteMatch(
    val networkIndex: Int,
    val prefixLength: Int,
    val isWifi: Boolean,
    val isActive: Boolean
)

internal object NetworkRouteSelector {
    /**
     * Chooses the most-specific connected route. On equal prefixes, prefer Wi-Fi
     * over Ethernet and then the active network. Default routes are rejected:
     * a cellular default route must never win for a local TV or peer address.
     */
    fun bestMatch(matches: List<NetworkRouteMatch>): Int? =
        matches.asSequence()
            .filter { it.networkIndex >= 0 && it.prefixLength in 1..32 }
            .maxWithOrNull(
                compareBy<NetworkRouteMatch> { it.prefixLength }
                    .thenBy { if (it.isWifi) 1 else 0 }
                    .thenBy { if (it.isActive) 1 else 0 }
            )
            ?.networkIndex
}
