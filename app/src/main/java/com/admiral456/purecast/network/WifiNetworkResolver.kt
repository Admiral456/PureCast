package com.admiral456.purecast.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object WifiNetworkResolver {
    fun activeWifiNetwork(context: Context): Network? {
        val connectivity = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        val active = connectivity.activeNetwork
        return connectivity.allNetworks
            .asSequence()
            .filter { network ->
                connectivity.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            .sortedBy { if (it == active) 0 else 1 }
            .firstOrNull()
    }

    fun activeWifiIpv4(context: Context): Inet4Address? {
        val connectivity = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        val active = connectivity.activeNetwork
        val wifiNetworks = connectivity.allNetworks
            .asSequence()
            .filter { network ->
                connectivity.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            .sortedBy { if (it == active) 0 else 1 }

        return wifiNetworks
            .mapNotNull(connectivity::getLinkProperties)
            .asSequence()
            .flatMap { properties -> properties.linkAddresses.asSequence() }
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }
    }

    /**
     * Returns an IPv4 address on a local network that another device can reach.
     *
     * A normal Wi-Fi Network is preferred. When Android is acting as a hotspot,
     * some OEM builds do not expose the tethering AP as a ConnectivityManager
     * Network, so fall back to known local interfaces (AP/SoftAP, Wi-Fi, Ethernet).
     * Cellular, VPN and loopback interfaces are deliberately never selected.
     */
    fun localLanIpv4(context: Context): Inet4Address? {
        val candidates = localInterfaceIpv4Candidates()
        // Prefer the SoftAP address when the phone is simultaneously connected
        // upstream as a Wi-Fi client and serving its own hotspot.
        candidates.firstOrNull { it.first == 0 }?.second?.let { return it }
        activeWifiIpv4(context)?.let { return it }
        return candidates.minByOrNull { it.first }?.second
    }

    /**
     * Selects the Android Network whose connected route actually contains
     * [address]. This avoids forcing local TV/group traffic over a different
     * Wi-Fi connection when the phone has both an upstream Wi-Fi and a hotspot.
     *
     * Only Wi-Fi and Ethernet routes are eligible. Cellular/VPN default routes
     * are not a substitute for a shared local network.
     */
    fun networkForAddress(context: Context, address: InetAddress): Network? {
        val connectivity = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val active = connectivity.activeNetwork
        val matches = mutableListOf<NetworkRouteMatch>()
        val networks = connectivity.allNetworks

        networks.forEachIndexed { index, network ->
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return@forEachIndexed
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!isWifi && !isEthernet) return@forEachIndexed

            val properties = connectivity.getLinkProperties(network) ?: return@forEachIndexed
            val prefixLength = properties.routes.asSequence()
                .filter { route ->
                    route.destination.address is Inet4Address &&
                        route.destination.prefixLength in 1..32 &&
                        route.matches(address)
                }
                .maxOfOrNull { it.destination.prefixLength }
                ?: return@forEachIndexed

            matches += NetworkRouteMatch(
                networkIndex = index,
                prefixLength = prefixLength,
                isWifi = isWifi,
                isActive = network == active
            )
        }

        val selectedIndex = NetworkRouteSelector.bestMatch(matches) ?: return null
        return networks.getOrNull(selectedIndex)
    }

    private fun localInterfaceIpv4Candidates(): List<Pair<Int, Inet4Address>> {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        val candidates = mutableListOf<Pair<Int, Inet4Address>>()
        while (interfaces.hasMoreElements()) {
            val networkInterface = try {
                interfaces.nextElement()
            } catch (_: Exception) {
                continue
            }
            val preference = LocalNetworkInterfaceSelector.priority(networkInterface.name) ?: continue
            val usable = try {
                networkInterface.isUp && !networkInterface.isLoopback
            } catch (_: Exception) {
                false
            }
            if (!usable) continue

            val addresses = try {
                networkInterface.inetAddresses
            } catch (_: Exception) {
                continue
            }
            while (addresses.hasMoreElements()) {
                val address = try {
                    addresses.nextElement()
                } catch (_: Exception) {
                    continue
                }
                if (address is Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                    candidates += preference to address
                }
            }
        }
        return candidates
    }
}
