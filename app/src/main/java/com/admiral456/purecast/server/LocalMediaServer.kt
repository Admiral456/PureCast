package com.admiral456.purecast.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.admiral456.purecast.media.MediaFile
import com.admiral456.purecast.network.WifiNetworkResolver
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private data class PublishedMedia(
    val media: MediaFile,
    val expiresAtMillis: Long
)

class LocalMediaServer(private val context: Context) : Closeable {
    private val mediaByToken = ConcurrentHashMap<String, PublishedMedia>()
    private val clientSockets = ConcurrentHashMap.newKeySet<Socket>()
    private val clientExecutor: ExecutorService = ThreadPoolExecutor(
        MIN_CLIENT_THREADS,
        MAX_CLIENT_THREADS,
        CLIENT_KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(MAX_QUEUED_CLIENTS),
        ThreadFactory { task ->
            Thread(task, "PureCast-HTTP-client").apply { isDaemon = true }
        },
        ThreadPoolExecutor.AbortPolicy()
    )
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var boundAddress: Inet4Address? = null

    @Synchronized
    fun publish(media: MediaFile): String {
        val address = findLocalIpv4()
            ?: throw IllegalStateException("Telefon není připojený k použitelné místní IPv4 síti.")
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
