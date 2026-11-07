package com.admiral456.purecast.server

import android.content.Context
import com.admiral456.purecast.media.MediaFile
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LocalMediaServer(private val context: Context) : Closeable {
    private val mediaByToken = ConcurrentHashMap<String, MediaFile>()
    private val clientExecutor: ExecutorService = Executors.newCachedThreadPool()
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null

    @Synchronized
    fun publish(media: MediaFile): String {
        val server = ensureServer()
        mediaByToken.clear()
        val token = UUID.randomUUID().toString()
        mediaByToken[token] = media
        val address = findLocalIpv4()
            ?: throw IllegalStateException("Nepodařilo se zjistit IPv4 adresu Wi-Fi.")
        return "http://" + address.hostAddress + ":" + server.localPort + "/media/" + token
    }

    @Synchronized
    private fun ensureServer(): ServerSocket {
        serverSocket?.takeIf { !it.isClosed }?.let { return it }

        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress("0.0.0.0", 0))
        serverSocket = socket
        acceptThread = Thread({ acceptLoop(socket) }, "PureCast-HTTP").apply {
            isDaemon = true
            start()
        }
        return socket
    }

    private fun acceptLoop(server: ServerSocket) {
        while (!server.isClosed) {
            try {
                val client = server.accept()
                clientExecutor.execute { handleClient(client) }
            } catch (_: Exception) {
                if (!server.isClosed) continue
                return
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val input = client.getInputStream()
            val headerBytes = ByteArrayOutputStream()
            var matched = 0
            val terminator = byteArrayOf(13, 10, 13, 10)

            while (headerBytes.size() < 16_384) {
                val next = input.read()
                if (next < 0) return
                headerBytes.write(next)
                matched = when {
                    next == terminator[matched].toInt() -> matched + 1
                    next == 13 -> 1
                    else -> 0
                }
                if (matched == terminator.size) break
            }

            if (matched != terminator.size) {
                writeSimpleResponse(client, 431, "Request Header Fields Too Large")
                return
            }

            val lines = String(headerBytes.toByteArray(), Charsets.ISO_8859_1).split("\r\n")
            val request = lines.firstOrNull()?.split(' ') ?: return
            val method = request.getOrNull(0)?.uppercase().orEmpty()
            val target = request.getOrNull(1).orEmpty()
            val headers = lines.drop(1)
                .mapNotNull { line ->
                    val colon = line.indexOf(':')
                    if (colon <= 0) null else {
                        line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
                    }
                }.toMap()

            if (method != "GET" && method != "HEAD") {
                writeSimpleResponse(client, 405, "Method Not Allowed")
                return
            }

            val encodedToken = target.substringAfter("/media/", "").substringBefore('?')
            val token = try {
                URLDecoder.decode(encodedToken, Charsets.UTF_8.name())
            } catch (_: Exception) {
                ""
            }
            val media = mediaByToken[token]
            if (media == null) {
                writeSimpleResponse(client, 404, "Not Found")
                return
            }

            val asset = try {
                context.contentResolver.openAssetFileDescriptor(media.uri, "r")
            } catch (_: Exception) {
                null
            }
            if (asset == null) {
                writeSimpleResponse(client, 404, "Media Not Available")
                return
            }

            asset.use { descriptor ->
                val totalLength = if (descriptor.length >= 0L) descriptor.length else (media.sizeBytes ?: -1L)
                val rangeHeader = headers["range"]
                val range = if (rangeHeader != null && totalLength > 0L) {
                    RangeParser.parse(rangeHeader, totalLength)
                } else null

                if (rangeHeader != null && range == null) {
                    val output = BufferedOutputStream(client.getOutputStream())
                    output.write(
                        ("HTTP/1.1 416 Range Not Satisfiable\r\n" +
                            "Content-Range: bytes */" + totalLength + "\r\n" +
                            "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII)
                    )
                    output.flush()
                    return
                }

                val start = range?.start ?: 0L
                val contentLength = range?.length ?: totalLength
                val status = if (range != null) "206 Partial Content" else "200 OK"
                val output = BufferedOutputStream(client.getOutputStream())
                val response = StringBuilder()
                    .append("HTTP/1.1 ").append(status).append("\r\n")
                    .append("Content-Type: ").append(safeHeaderValue(media.mimeType)).append("\r\n")
                    .append("Content-Disposition: inline\r\n")
                    .append("Accept-Ranges: bytes\r\n")
                    .append("Cache-Control: no-store\r\n")
                    .append("Connection: close\r\n")
                if (contentLength >= 0L) response.append("Content-Length: ").append(contentLength).append("\r\n")
                if (range != null) {
                    response.append("Content-Range: bytes ").append(range.start).append("-")
                        .append(range.endInclusive).append("/").append(totalLength).append("\r\n")
                }
                response.append("\r\n")
                output.write(response.toString().toByteArray(Charsets.US_ASCII))
                output.flush()

                if (method == "HEAD") return
                val stream = descriptor.createInputStream()
                stream.use {
                    skipFully(it, start)
                    if (contentLength >= 0L) copyExactly(it, output, contentLength) else it.copyTo(output, 64 * 1024)
                }
                output.flush()
            }
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                if (input.read() < 0) throw IllegalStateException("Soubor skončil před požadovaným rozsahem.")
                remaining--
            }
        }
    }

    private fun copyExactly(input: InputStream, output: BufferedOutputStream, length: Long) {
        var remaining = length
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0L) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) break
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun writeSimpleResponse(socket: Socket, status: Int, reason: String) {
        val output = BufferedOutputStream(socket.getOutputStream())
        val response = "HTTP/1.1 " + status + " " + reason +
            "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun safeHeaderValue(value: String): String =
        value.replace("\r", "").replace("\n", "").ifBlank { "application/octet-stream" }

    private fun findLocalIpv4(): Inet4Address? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                    return address
                }
            }
        }
        return null
    }

    override fun close() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        acceptThread = null
        mediaByToken.clear()
        clientExecutor.shutdownNow()
    }
}
