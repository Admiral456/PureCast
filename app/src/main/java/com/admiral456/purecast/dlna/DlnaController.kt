package com.admiral456.purecast.dlna

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DlnaController {
    suspend fun play(device: DlnaDevice, mediaUrl: String) = withContext(Dispatchers.IO) {
        val transport = device.serviceType
        postAction(
            device,
            transport,
            "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>" + xmlEscape(mediaUrl) +
                "</CurrentURI><CurrentURIMetaData></CurrentURIMetaData>"
        )
        postAction(device, transport, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    private fun postAction(
        device: DlnaDevice,
        serviceType: String,
        action: String,
        body: String
    ) {
        val envelope =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><u:" + action + " xmlns:u=\"" + xmlEscape(serviceType) + "\">" +
                body + "</u:" + action + "></s:Body></s:Envelope>"

        val connection = URL(device.avTransportControlUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            connection.setRequestProperty("SOAPACTION", "\""+ serviceType + "#" + action + "\"")
            connection.outputStream.use { output ->
                output.write(envelope.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(
                    "Televize odmítla příkaz " + action + " (HTTP " + code + "). " + error.take(240)
                )
            }
            connection.inputStream?.close()
        } finally {
            connection.disconnect()
        }
    }

    private fun xmlEscape(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
