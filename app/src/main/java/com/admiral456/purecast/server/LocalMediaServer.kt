package com.admiral456.purecast.server

import android.content.Context
import android.net.Uri
import com.admiral456.purecast.media.MediaFile
import com.admiral456.purecast.network.WifiNetworkResolver
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private class TransferCountingOutputStream(
    private val delegate: OutputStream,
    private val onBytesWritten: (Long) -> Unit
) : OutputStream() {
    override fun write(value: Int) {
        delegate.write(value)
        onBytesWritten(1L)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        delegate.write(buffer, offset, length)
        onBytesWritten(length.toLong())
    }

    override fun flush() = delegate.flush()
}

private data class PublishedMedia(
    val media: MediaFile,
    val expiresAtMillis: Long
)

class LocalMediaServer(private val context: Context) : Closeable {
    private val mediaByToken = ConcurrentHashMap<String, PublishedMedia>()
    private val transferMetrics = TransferMetrics()

    internal fun transferStats(): TransferMetricsSnapshot = transferMetrics.snapshot()
    private val clientSockets = ConcurrentHashMap.newKeySet<Socket>()
    private val clientExecutor: ExecutorService = ThreadPoolExecutor(
        CLIENT_THREADS,
        CLIENT_THREADS,
        CLIENT_KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(MAX_QUEUED_CLIENTS),
        ThreadFactory { task ->
            Thread(task, "PureCast-HTTP-client").apply { isDaemon = true }
        },
        ThreadPoolExecutor.AbortPolicy()
    ).apply {
        // Grow to the full bounded worker count immediately during request bursts,
        // then release idle workers after the keep-alive interval.
        allowCoreThreadTimeOut(true)
    }
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var boundAddress: Inet4Address? = null

    @Synchronized
    fun publish(media: MediaFile): String {
        val address = findLocalIpv4()
            ?: throw IllegalStateException("Telefon není připojený k použitelné místní Wi-Fi, hotspot nebo Ethernet IPv4 síti.")
        val server = ensureServer(address)
        val token = MediaTokenGenerator.create()
        mediaByToken[token] = PublishedMedia(media, System.currentTimeMillis() + TOKEN_LIFETIME_MS)
        pruneExpiredTokens()
        return "http://" + address.hostAddress + ":" + server.localPort + "/media/" + token
    }

    fun browserPlayerUrl(mediaUrl: String): String {
        val parsed = Uri.parse(mediaUrl)
        val token = parsed.lastPathSegment ?: return mediaUrl
        return parsed.buildUpon().path("/tv/$token").build().toString()
    }

    @Synchronized
    private fun ensureServer(address: Inet4Address): ServerSocket {
        serverSocket?.takeIf { !it.isClosed && boundAddress == address }?.let { return it }
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        if (boundAddress != null && boundAddress != address) {
            clientSockets.forEach { client ->
                try {
                    client.close()
                } catch (_: Exception) {
                }
            }
            clientSockets.clear()
            mediaByToken.clear()
        }
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(address, 0))
        serverSocket = socket
        boundAddress = address
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
                configureAcceptedSocket(client)
                clientSockets.add(client)
                try {
                    clientExecutor.execute {
                        try {
                            handleClient(client)
                        } finally {
                            clientSockets.remove(client)
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    clientSockets.remove(client)
                    try {
                        writeSimpleResponse(client, 503, "Server Busy")
                    } catch (_: Exception) {
                    } finally {
                        try {
                            client.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
                if (!server.isClosed) continue
                return
            }
        }
    }

    private fun configureAcceptedSocket(socket: Socket) {
        // Increase the kernel TCP send window for high-bitrate local video streams.
        // The OS may clamp this value; failure is non-fatal and the default remains usable.
        try {
            socket.sendBufferSize = SOCKET_SEND_BUFFER_SIZE
        } catch (_: Exception) {
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 20_000
            val parsedRequest = try {
                MediaRequestParser.read(client.getInputStream(), MAX_HEADER_BYTES)
            } catch (_: MediaRequestParser.HeadersTooLarge) {
                writeSimpleResponse(client, 431, "Request Header Fields Too Large")
                return
            } catch (_: IOException) {
                writeSimpleResponse(client, 400, "Bad Request")
                return
            } ?: return

            val method = parsedRequest.method
            val target = parsedRequest.target
            if (method != "GET" && method != "HEAD") {
                writeSimpleResponse(client, 405, "Method Not Allowed")
                return
            }

            val path = target.substringBefore('?')
            val match = MEDIA_PATH.matchEntire(path) ?: run {
                writeSimpleResponse(client, 404, "Not Found")
                return
            }
            val route = match.groupValues[1]
            val token = match.groupValues[2]
            val published = mediaByToken[token]
            if (published == null || published.expiresAtMillis < System.currentTimeMillis()) {
                mediaByToken.remove(token)
                writeSimpleResponse(client, 404, "Media Link Expired")
                return
            }

            if (route == "tv") {
                writeBrowserPlayer(client, method, token, published.media)
                return
            }

            val headers = parsedRequest.headers

            val asset = try {
                context.contentResolver.openAssetFileDescriptor(published.media.uri, "r")
            } catch (_: Exception) {
                null
            }
            if (asset == null) {
                writeSimpleResponse(client, 404, "Media Not Available")
                return
            }

            asset.use { descriptor ->
                val totalLength = if (descriptor.length >= 0L) {
                    descriptor.length
                } else {
                    published.media.sizeBytes ?: -1L
                }
                val rangeHeader = headers["range"]
                val isByteRange = rangeHeader?.startsWith("bytes=", ignoreCase = true) == true
                val range = if (isByteRange && totalLength > 0L) {
                    RangeParser.parse(rangeHeader, totalLength)
                } else null

                if (isByteRange && totalLength > 0L && range == null) {
                    val output = BufferedOutputStream(client.getOutputStream(), OUTPUT_BUFFER_SIZE)
                    output.write(
                        ("HTTP/1.1 416 Range Not Satisfiable\r\n" +
                            "Content-Range: bytes */$totalLength\r\n" +
                            "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII)
                    )
                    output.flush()
                    return
                }

                val start = range?.start ?: 0L
                val contentLength = range?.length ?: totalLength
                val status = if (range != null) "206 Partial Content" else "200 OK"
                val output = BufferedOutputStream(client.getOutputStream(), OUTPUT_BUFFER_SIZE)
                val response = StringBuilder()
                    .append("HTTP/1.1 ").append(status).append("\r\n")
                    .append("Content-Type: ").append(safeContentType(published.media.mimeType)).append("\r\n")
                    .append("Content-Disposition: inline\r\n")
                    .append("Accept-Ranges: ").append(if (totalLength > 0L) "bytes" else "none").append("\r\n")
                    .append("Cache-Control: no-store\r\n")
                    .append("X-Content-Type-Options: nosniff\r\n")
                    .append("Connection: close\r\n")
                if (contentLength >= 0L) {
                    response.append("Content-Length: ").append(contentLength).append("\r\n")
                }
                if (range != null) {
                    response.append("Content-Range: bytes ").append(range.start).append("-")
                        .append(range.endInclusive).append("/").append(totalLength).append("\r\n")
                }
                response.append("\r\n")
                output.write(response.toString().toByteArray(Charsets.US_ASCII))
                output.flush()

                if (method == "HEAD") return
                transferMetrics.transferStarted()
                var transferCompleted = false
                try {
                    descriptor.createInputStream().use { stream ->
                        val mediaOutput = TransferCountingOutputStream(output) { bytes ->
                            transferMetrics.bytesSent(bytes)
                        }
                        StreamTransfer.skipFully(stream, start)
                        if (contentLength >= 0L) {
                            StreamTransfer.copyExactly(stream, mediaOutput, contentLength, BUFFER_SIZE)
                        } else {
                            StreamTransfer.copyToEnd(stream, mediaOutput, BUFFER_SIZE)
                        }
                    }
                    output.flush()
                    transferCompleted = true
                } finally {
                    transferMetrics.transferFinished(completed = transferCompleted)
                }
            }
        }
    }

    private fun writeBrowserPlayer(socket: Socket, method: String, token: String, media: MediaFile) {
        val escapedName = htmlEscape(media.displayName)
        val safeMime = htmlEscape(safeContentType(media.mimeType))
        val mediaPath = "/media/$token"
        val content = if (media.isImage) {
            "<img id=\"picture\" data-mime=\"$safeMime\" src=\"$mediaPath\" alt=\"$escapedName\">"
        } else if (media.isVideo) {
            "<video id=\"player\" controls autoplay playsinline preload=\"metadata\">" +
                "<source src=\"$mediaPath\" type=\"$safeMime\">" +
                "Tento prohlížeč neumí přehrát vložené video. " +
                "<a href=\"$mediaPath\">Zkusit otevřít originální soubor</a></video>"
        } else {
            "<p>Televizor neumí rozpoznat typ tohoto souboru: $escapedName</p>" +
                "<a href=\"$mediaPath\">Otevřít originální soubor</a>"
        }
        val script = when {
            media.isImage -> {
                """
                <script>
                  (() => {
                    const picture = document.getElementById('picture');
                    const result = document.getElementById('compatibility');
                    if (!picture || !result) return;
                    const mime = picture.getAttribute('data-mime') || 'obrazový soubor';
                    const loaded = () => {
                      const dimensions = picture.naturalWidth && picture.naturalHeight
                        ? ' Rozlišení: ' + picture.naturalWidth + ' × ' + picture.naturalHeight + '.'
                        : '';
                      result.textContent = 'Televizor zobrazil originální obrázek (' + mime + ').' + dimensions;
                    };
                    const failed = () => {
                      result.textContent = 'Prohlížeč televize nedokázal zobrazit ' + mime + '. Soubor zůstal beze změny; tento model možná nepodporuje jeho formát.';
                    };
                    picture.addEventListener('error', failed, { once: true });
                    if (picture.complete) {
                      if (picture.naturalWidth > 0) loaded(); else failed();
                    } else {
                      picture.addEventListener('load', loaded, { once: true });
                    }
                  })();
                </script>
                """.trimIndent()
            }
            media.isVideo -> {
                """
                <script>
                  (() => {
                    const player = document.getElementById('player');
                    const result = document.getElementById('compatibility');
                    if (!player || !result) return;
                    const mime = player.querySelector('source')?.type || '';
                    const support = player.canPlayType(mime);
                    result.textContent = support === 'probably'
                      ? 'Prohlížeč pravděpodobně podporuje typ ' + mime + '. Konkrétní kodek a HDR se ověřují až při přehrávání.'
                      : support === 'maybe'
                        ? 'Prohlížeč uvádí možnou podporu ' + mime + '. Přesná kompatibilita není zaručena.'
                        : 'Prohlížeč nepotvrdil podporu ' + mime + '. PureCast soubor nepřevádí.';
                    player.addEventListener('error', () => {
                      result.textContent = 'Přehrávání selhalo. Televizor pravděpodobně nepodporuje tento formát nebo kodek.';
                    });
                    player.addEventListener('loadedmetadata', () => {
                      if (player.videoWidth && player.videoHeight) {
                        result.textContent += ' Zjištěné rozlišení: ' + player.videoWidth + ' × ' + player.videoHeight + '.';
                      }
                    });
                  })();
                </script>
                """.trimIndent()
            }
            else -> ""
        }
        val compatibilityPanel = if (media.isImage || media.isVideo) {
            "<p id=\"compatibility\" class=\"compatibility\">Ověřuji zobrazení originálního souboru v televizoru…</p>"
        } else {
            "<p class=\"compatibility\">Televizor nemusí tento typ souboru podporovat.</p>"
        }
        val html = """
            <!doctype html>
            <html lang="cs">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <meta name="color-scheme" content="dark">
              <title>PureCast – $escapedName</title>
              <style>
                *{box-sizing:border-box}
                html,body{margin:0;min-height:100%;background:#080b13;color:#fff;font-family:system-ui,-apple-system,sans-serif}
                body{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;padding:76px 22px 26px;gap:18px}
                header{position:fixed;top:0;left:0;right:0;display:flex;align-items:center;gap:12px;padding:18px 24px;background:rgba(8,11,19,.94);border-bottom:1px solid #20283a}
                .logo{width:34px;height:34px;border-radius:11px;display:grid;place-items:center;background:linear-gradient(135deg,#4059d6,#087f8c);font-weight:800}
                h1{font-size:16px;font-weight:650;margin:0;overflow-wrap:anywhere}
                .subtitle{font-size:12px;color:#9eabc5;margin-top:3px}
                main{width:min(100%,1400px);display:flex;flex-direction:column;align-items:center;gap:14px}
                .frame{display:flex;align-items:center;justify-content:center;width:100%;min-height:42vh;max-height:75vh;background:#000;border:1px solid #293248;border-radius:18px;overflow:hidden}
                img{max-width:100%;max-height:72vh;object-fit:contain}
                video{width:100%;height:auto;max-height:75vh;object-fit:contain;background:#000}
                .compatibility{width:100%;margin:0;padding:12px 15px;border:1px solid #293248;border-radius:12px;color:#bdc9e0;font-size:13px;line-height:1.5;background:#121a2a}
                .quality{font-size:12px;color:#8592ac;text-align:center}
                a{color:#aebeff}
              </style>
            </head>
            <body>
              <header>
                <div class="logo">PC</div>
                <div><h1>PureCast · $escapedName</h1><div class="subtitle">Přehrávání originálního souboru v místní síti</div></div>
              </header>
              <main>
                <div class="frame">$content</div>
                $compatibilityPanel
                <div class="quality">Bez cloudu · Bez překódování · Původní soubor</div>
              </main>
              $script
            </body>
            </html>
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val output = BufferedOutputStream(socket.getOutputStream(), OUTPUT_BUFFER_SIZE)
        output.write(
            ("HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${html.size}\r\n" +
                "Cache-Control: no-store\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII)
        )
        if (method != "HEAD") output.write(html)
        output.flush()
    }

    private fun writeSimpleResponse(socket: Socket, status: Int, reason: String) {
        val output = BufferedOutputStream(socket.getOutputStream(), OUTPUT_BUFFER_SIZE)
        val response = "HTTP/1.1 $status $reason" +
            "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun safeContentType(value: String): String {
        val normalized = value.substringBefore(';').trim().lowercase()
        val slash = normalized.indexOf('/')
        if (slash <= 0 || slash != normalized.lastIndexOf('/')) return "application/octet-stream"
        val valid = normalized.indices.all { index ->
            val character = normalized[index]
            index == slash || character in 'a'..'z' || character in '0'..'9' ||
                character in "!#&^_.+-"
        }
        return if (valid) normalized else "application/octet-stream"
    }

    private fun htmlEscape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun pruneExpiredTokens() {
        val now = System.currentTimeMillis()
        mediaByToken.entries.removeAll { it.value.expiresAtMillis < now }
    }

    private fun findLocalIpv4(): Inet4Address? = WifiNetworkResolver.localLanIpv4(context)

    override fun close() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        acceptThread = null
        boundAddress = null
        mediaByToken.clear()
        clientSockets.forEach { client ->
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
        clientSockets.clear()
        clientExecutor.shutdownNow()
    }

    companion object {
        private const val MAX_HEADER_BYTES = 16_384
        private const val BUFFER_SIZE = StreamTransfer.DEFAULT_BUFFER_SIZE
        private const val OUTPUT_BUFFER_SIZE = 64 * 1024
        private const val SOCKET_SEND_BUFFER_SIZE = 512 * 1024
        private const val CLIENT_THREADS = 8
        private const val MAX_QUEUED_CLIENTS = 16
        private const val CLIENT_KEEP_ALIVE_SECONDS = 30L
        private const val TOKEN_LIFETIME_MS = 6 * 60 * 60 * 1_000L
        private val MEDIA_PATH = Regex("^/(media|tv)/([A-Za-z0-9_-]{12})$")
    }
}
