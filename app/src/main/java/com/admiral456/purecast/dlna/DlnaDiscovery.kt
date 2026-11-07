package com.admiral456.purecast.dlna

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

class DlnaDiscovery(context: Context) {
    private val appContext = context.applicationContext

    suspend fun discover(timeoutMs: Long = 5_000L): List<DlnaDevice> = withContext(Dispatchers.IO) {
        discoverBlocking(timeoutMs)
    }

    private fun discoverBlocking(timeoutMs: Long): List<DlnaDevice> {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = wifi.createMulticastLock("PureCast-SSDP").apply {
            setReferenceCounted(false)
            acquire()
        }
        val locations = linkedSetOf<String>()
        val searchTargets = listOf(
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "urn:schemas-upnp-org:device:MediaRenderer:2",
            "ssdp:all"
        )

        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 300
                val destination = InetAddress.getByName("239.255.255.250")
                searchTargets.forEach { target ->
                    val message = (
                        "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\nST: $target\r\n\r\n"
                    ).toByteArray(Charsets.UTF_8)
                    socket.send(DatagramPacket(message, message.size, destination, 1900))
                }

                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val buffer = ByteArray(8_192)
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val response = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        val location = response.lineSequence().mapNotNull { line ->
                            val colon = line.indexOf(':')
                            if (colon <= 0) null else if (
                                line.substring(0, colon).trim().equals("LOCATION", ignoreCase = true)
                            ) line.substring(colon + 1).trim() else null
                        }.firstOrNull()?.takeIf {
                            it.startsWith("http://", true) || it.startsWith("https://", true)
                        }
                        if (location != null) locations.add(location)
                    } catch (_: java.net.SocketTimeoutException) {
                        // Continue collecting replies until the search window expires.
                    }
                }
            }
        } finally {
            if (multicastLock.isHeld) multicastLock.release()
        }
        return locations.mapNotNull(::loadRendererDescription).distinctBy { it.id }
    }

    private fun loadRendererDescription(location: String): DlnaDevice? {
        val connection = URL(location).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept-Encoding", "identity")
        return try {
            if (connection.responseCode !in 200..299) return null
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            val document = connection.inputStream.use { factory.newDocumentBuilder().parse(it) }
            val allDevices = document.getElementsByTagName("device")
            var renderer: Element? = null
            for (index in 0 until allDevices.length) {
                val candidate = allDevices.item(index) as? Element ?: continue
                if (candidate.firstText("deviceType").contains("MediaRenderer", ignoreCase = true)) {
                    renderer = candidate
                    break
                }
            }
            val deviceElement = renderer ?: return null
            val services = deviceElement.getElementsByTagName("service")
            var serviceType: String? = null
            var controlPath: String? = null
            for (index in 0 until services.length) {
                val service = services.item(index) as? Element ?: continue
                val type = service.firstText("serviceType")
                val control = service.firstText("controlURL")
                if (type.contains("AVTransport", ignoreCase = true) && control.isNotBlank()) {
                    serviceType = type
                    controlPath = control
                    break
                }
            }
            if (serviceType == null || controlPath == null) return null
            val urlBaseText = document.getElementsByTagName("URLBase")
                .item(0)?.textContent?.trim().orEmpty()
            val controlBase = if (urlBaseText.isNotBlank()) URL(urlBaseText) else URL(location)
            val resolvedControlUrl = URL(controlBase, controlPath).toExternalForm()
            DlnaDevice(
                id = deviceElement.firstText("UDN").ifBlank { location },
                friendlyName = deviceElement.firstText("friendlyName").ifBlank { "DLNA televizor" },
                manufacturer = deviceElement.firstText("manufacturer").ifBlank { null },
                modelName = deviceElement.firstText("modelName").ifBlank { null },
                location = location,
                avTransportControlUrl = resolvedControlUrl,
                serviceType = serviceType
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun Element.firstText(tag: String): String {
        val nodes = getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent?.trim().orEmpty() else ""
    }
}
