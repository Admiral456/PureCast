package com.admiral456.purecast.group

import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI

object GroupQueueAddress {
    private val ipv4Pattern = Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}$")

    fun normalize(value: String): String {
        val raw = value.trim().removeSuffix("/")
        val candidate = if (raw.contains("://")) raw else "http://$raw"
        val uri = try {
            URI(candidate)
        } catch (_: Exception) {
            throw IllegalArgumentException("Zadej adresu hostitele ve formátu 192.168.1.20:port.")
        }
        if (uri.scheme != "http" || uri.userInfo != null || (uri.path.isNotEmpty() && uri.path != "/") ||
            uri.query != null || uri.fragment != null || uri.port !in 1..65535
        ) {
            throw IllegalArgumentException("Adresa hostitele není platná. Použij HTTP adresu v místní Wi-Fi síti.")
        }
        val host = uri.host ?: throw IllegalArgumentException("V adrese chybí IPv4 adresa hostitele.")
        if (!ipv4Pattern.matches(host)) {
            throw IllegalArgumentException("Adresa hostitele musí být místní IPv4 adresa.")
        }
        val address = InetAddress.getByName(host)
        if (address !is Inet4Address || !address.isSiteLocalAddress ||
            address.isLoopbackAddress || address.isLinkLocalAddress || address.isAnyLocalAddress
        ) {
            throw IllegalArgumentException("Hostitel musí být dostupný přes soukromou IPv4 adresu Wi-Fi.")
        }
        return "http://$host:" + uri.port
    }
}
