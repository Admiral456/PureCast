package com.admiral456.purecast.dlna

import com.admiral456.purecast.media.MediaFile
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DlnaTransportInfo(val state: String, val status: String)

data class DlnaPositionInfo(
    val positionSeconds: Long?,
    val durationSeconds: Long?
)

object UpnpTime {
    fun parseSeconds(value: String?): Long? {
        if (value.isNullOrBlank() || value == "NOT_IMPLEMENTED") return null
        val parts = value.trim().split(":")
        if (parts.size != 3) return null
        val hours = parts[0].toLongOrNull() ?: return null
        val minutes = parts[1].toLongOrNull() ?: return null
        val seconds = parts[2].toDoubleOrNull() ?: return null
        if (hours < 0 || minutes !in 0..59 || seconds < 0.0 || seconds >= 60.0) return null
        return hours * 3_600L + minutes * 60L + seconds.toLong()
    }

    fun formatSeconds(value: Long): String {
        val safe = value.coerceAtLeast(0L)
        val hours = safe / 3_600L
        val minutes = (safe % 3_600L) / 60L
        val seconds = safe % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

class DlnaController {
    suspend fun play(device: DlnaDevice, mediaUrl: String, media: MediaFile) = withContext(Dispatchers.IO) {
        val didl = didlMetadata(media, mediaUrl)
        postAction(device, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>${xmlEscape(mediaUrl)}</CurrentURI>" +
            "<CurrentURIMetaData>${xmlEscape(didl)}</CurrentURIMetaData>")
        postAction(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    suspend fun resume(device: DlnaDevice) = withContext(Dispatchers.IO) {
        postAction(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    suspend fun pause(device: DlnaDevice) = withContext(Dispatchers.IO) {
        postAction(device, "Pause", "<InstanceID>0</InstanceID>")
    }

    suspend fun stop(device: DlnaDevice) = withContext(Dispatchers.IO) {
        postAction(device, "Stop", "<InstanceID>0</InstanceID>")
    }

    suspend fun seekToSeconds(device: DlnaDevice, seconds: Long) = withContext(Dispatchers.IO) {
        require(seconds >= 0L) { "Čas přehrávání nesmí být záporný." }
        val target = UpnpTime.formatSeconds(seconds)
        postAction(device, "Seek",
            "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>$target</Target>")
    }

    suspend fun getPositionInfo(device: DlnaDevice): DlnaPositionInfo = withContext(Dispatchers.IO) {
        val response = postAction(device, "GetPositionInfo", "<InstanceID>0</InstanceID>")
        val document = secureDocumentFactory().newDocumentBuilder()
            .parse(response.byteInputStream(Charsets.UTF_8))
        val position = document.getElementsByTagName("RelTime").item(0)?.textContent
        val duration = document.getElementsByTagName("TrackDuration").item(0)?.textContent
        DlnaPositionInfo(
            positionSeconds = UpnpTime.parseSeconds(position),
            durationSeconds = UpnpTime.parseSeconds(duration)
        )
    }

    suspend fun getSinkProtocolInfo(device: DlnaDevice): List<String>? = withContext(Dispatchers.IO) {
        val controlUrl = device.connectionManagerControlUrl ?: return@withContext null
        val serviceType = device.connectionManagerServiceType ?: return@withContext null
        val response = postAction(
            device,
            "GetProtocolInfo",
            "",
            controlUrl = controlUrl,
            serviceType = serviceType
        )
        val document = secureDocumentFactory().newDocumentBuilder()
            .parse(response.byteInputStream(Charsets.UTF_8))
        val sink = document.getElementsByTagName("Sink").item(0)?.textContent.orEmpty()
        sink.split(',').map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null }
    }

    suspend fun getTransportInfo(device: DlnaDevice): DlnaTransportInfo = withContext(Dispatchers.IO) {
        val response = postAction(device, "GetTransportInfo", "<InstanceID>0</InstanceID>")
        val factory = secureDocumentFactory()
        val document = factory.newDocumentBuilder().parse(response.byteInputStream(Charsets.UTF_8))
        val state = document.getElementsByTagName("CurrentTransportState")
            .item(0)?.textContent?.trim().orEmpty().ifBlank { "UNKNOWN" }
        val status = document.getElementsByTagName("CurrentTransportStatus")
            .item(0)?.textContent?.trim().orEmpty().ifBlank { "UNKNOWN" }
        DlnaTransportInfo(state, status)
    }

    private fun didlMetadata(media: MediaFile, mediaUrl: String): String {
        val upnpClass = if (media.mimeType.startsWith("image/")) {
            "object.item.imageItem.photo"
        } else {
            "object.item.videoItem"
        }
        return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
            "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
            "<dc:title>${xmlEscape(media.displayName)}</dc:title>" +
            "<upnp:class>$upnpClass</upnp:class>" +
            "<res protocolInfo=\"http-get:*:${xmlEscape(media.mimeType)}:DLNA.ORG_OP=01;DLNA.ORG_CI=0\">" +
            "${xmlEscape(mediaUrl)}</res></item></DIDL-Lite>"
    }

    private fun postAction(
        device: DlnaDevice,
        action: String,
        body: String,
        controlUrl: String = device.avTransportControlUrl,
        serviceType: String = device.serviceType
    ): String {
        val envelope =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
            "<s:Body><u:$action xmlns:u=\"${xmlEscape(serviceType)}\">" +
            "$body</u:$action></s:Body></s:Envelope>"

        val connection = URL(controlUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            connection.setRequestProperty("SOAPACTION", "\"$serviceType#$action\"")
            connection.outputStream.use { it.write(envelope.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = if (code in 200..299) {
                connection.inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            if (code !in 200..299) {
                val fault = response.replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ").trim()
                throw IllegalStateException(
                    "Televize odmítla příkaz $action (HTTP $code)." +
                        if (fault.isBlank()) "" else " " + fault.take(240)
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun secureDocumentFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }

    private fun xmlEscape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
