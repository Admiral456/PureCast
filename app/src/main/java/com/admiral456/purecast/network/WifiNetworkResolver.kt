package com.admiral456.purecast.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address

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
}
