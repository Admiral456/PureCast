package com.admiral456.purecast.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
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
        activeWifiIpv4(context)?.let { return it }

        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (_: Exception) {
            null
        } ?: return null

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
        return candidates.minByOrNull { it.first }?.second
    }
}
