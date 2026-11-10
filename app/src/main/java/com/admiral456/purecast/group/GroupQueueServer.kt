package com.admiral456.purecast.group

import android.content.Context
import com.admiral456.purecast.network.WifiNetworkResolver
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** LAN-only coordinator. Media bytes stay on each sender and stream directly to the TV. */
class GroupQueueServer(context: Context, hostDisplayName: String) : Closeable {
    private val appContext = context.applicationContext
    private val sessionId = UUID.randomUUID().toString()
    private val inviteCode = createInviteCode()
    private val hostId = UUID.randomUUID().toString()
    private val hostToken = createSecret()
    private val lock = Any()
    private val participants = linkedMapOf<String, GroupQueueParticipant>()
    private val tokens = ConcurrentHashMap<String, String>()
    private val entries = mutableListOf<GroupQueueEntry>()
    private val joinLimiter = GroupQueueJoinLimiter()
    private val clientSockets = ConcurrentHashMap.newKeySet<Socket>()
    private val executor = ThreadPoolExecutor(
        MIN_CLIENT_THREADS,
        MAX_CLIENT_THREADS,
        CLIENT_KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(MAX_QUEUED_CLIENTS),
        ThreadFactory { task ->
            Thread(task, "PureCast-GroupQueue-client").apply { isDaemon = true }
        },
        ThreadPoolExecutor.AbortPolicy()
    )
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var baseUrl: String? = null
    @Volatile private var revision = 0L
    @Volatile private var currentEntryId: String? = null

    init {
        participants[hostId] = GroupQueueParticipant(hostId, cleanName(hostDisplayName), true)
        tokens[hostToken] = hostId
    }

    val invitationAddress: String
        get() = baseUrl ?: throw IllegalStateException("Sdílená skupina ještě není spuštěná.")

    @Synchronized
    fun start(): GroupQueueCredentials {
        if (serverSocket?.isClosed == false) return hostCredentials()
        val ip = WifiNetworkResolver.localLanIpv4(appContext)
            ?: throw IllegalStateException("Telefon není připojený k použitelné místní Wi-Fi, hotspot nebo Ethernet IPv4 síti.")
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(ip, 0))
        serverSocket = socket
        baseUrl = "http://${ip.hostAddress}:${socket.localPort}"
        Thread({
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    clientSockets.add(client)
                    try {
                        executor.execute {
                            try {
                                handleClient(client)
                            } finally {
                                clientSockets.remove(client)
                            }
                        }
                    } catch (_: RejectedExecutionException) {
                        clientSockets.remove(client)
                        try {
                            respond(client, 503, error("Server je vytížený. Zkus to znovu."))
                        } catch (_: Exception) {
                        } finally {
                            try {
                                client.close()
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (socket.isClosed) return@Thread
                }
            }
        }, "PureCast-GroupQueue").apply { isDaemon = true; start() }
        return hostCredentials()
    }

    fun hostCredentials() = GroupQueueCredentials(
        baseUrl = invitationAddress,
        accessToken = hostToken,
        participantId = hostId,
        displayName = participants[hostId]?.displayName ?: "Hostitel",
        isHost = true,
        inviteCode = inviteCode
    )

    fun snapshot(): GroupQueueSnapshot = synchronized(lock) {
        GroupQueueSnapshot(sessionId, revision, currentEntryId, participants.values.toList(), entries.toList())
    }

    private data class Request(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String,
        val remote: String
    )

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 8_000
            val request = try {
                readRequest(client)
            } catch (_: GroupQueueRequestParser.TooLarge) {
                respond(client, 413, error("Požadavek je příliš velký."))
                return
            } catch (_: Exception) {
                respond(client, 400, error("Neplatný HTTP požadavek."))
                return
            } ?: return
            try {
                route(client, request)
            } catch (_: Exception) {
                respond(client, 400, error("Požadavek nelze zpracovat."))
            }
        }
    }

    private fun readRequest(socket: Socket): Request? {
        val parsed = GroupQueueRequestParser.read(
            input = socket.getInputStream(),
            remote = socket.inetAddress.hostAddress ?: "unknown",
            maxHeaderBytes = MAX_HEADER_BYTES,
            maxBodyBytes = MAX_BODY_BYTES
        ) ?: return null
        return Request(parsed.method, parsed.path, parsed.headers, parsed.body, parsed.remote)
    }

    private fun route(socket: Socket, request: Request) {
        if (request.method == "POST" && request.path == "/v1/join") {
            join(socket, request)
            return
        }
        val token = request.headers["authorization"]
            ?.takeIf { it.startsWith("Bearer ", true) }?.substringAfter(' ')?.trim().orEmpty()
        val participantId = tokens[token]
        if (participantId == null) {
            respond(socket, 401, error("Přístupový token chybí nebo vypršel."))
            return
        }
        when {
            request.method == "GET" && request.path == "/v1/state" ->
                respond(socket, 200, snapshotJson(snapshot()))
            request.method == "POST" && request.path == "/v1/leave" ->
                leave(socket, participantId, token)
            request.method == "POST" && request.path == "/v1/participants/remove" ->
                removeParticipant(socket, request, participantId)
            request.method == "POST" && request.path == "/v1/queue/add" ->
                add(socket, request, participantId)
            request.method == "POST" && request.path == "/v1/queue/remove" ->
                remove(socket, request, participantId)
            request.method == "POST" && request.path == "/v1/queue/move" ->
                move(socket, request, participantId)
            request.method == "POST" && request.path == "/v1/queue/current" ->
                setCurrent(socket, request, participantId)
            else -> respond(socket, 404, error("Požadovaná operace neexistuje."))
        }
    }

    private fun join(socket: Socket, request: Request) {
        if (!allowAttempt(request.remote)) {
            respond(socket, 429, error("Příliš mnoho pokusů o připojení. Zkus to za minutu."))
            return
        }
        val payload = JSONObject(request.body)
        if (payload.optString("code").trim().uppercase() != inviteCode) {
            recordFailure(request.remote)
            respond(socket, 401, error("Pozvánkový kód není správný."))
            return
        }
        val name = cleanName(payload.optString("displayName"))
        val id = UUID.randomUUID().toString()
        val token = createSecret()
        synchronized(lock) {
            if (participants.size >= MAX_PARTICIPANTS) {
                respond(socket, 409, error("Skupina už dosáhla limitu $MAX_PARTICIPANTS telefonů."))
                return
            }
            participants[id] = GroupQueueParticipant(id, name, false)
            tokens[token] = id
            revision++
        }
        joinLimiter.recordSuccess(request.remote)
        respond(socket, 200, JSONObject()
            .put("accessToken", token)
            .put("participantId", id)
            .put("displayName", name)
            .put("isHost", false)
            .put("snapshot", snapshotJson(snapshot())))
    }

    private fun leave(socket: Socket, participantId: String, token: String) {
        if (isHost(participantId)) {
            respond(socket, 403, error("Hostitel ukončuje skupinu zavřením relace."))
            return
        }
        synchronized(lock) {
            participants.remove(participantId)
            tokens.remove(token)
            val removedIds = entries.filter { it.senderParticipantId == participantId }.map { it.id }.toSet()
            entries.removeAll { it.senderParticipantId == participantId }
            val playingId = currentEntryId
            if (playingId != null && playingId in removedIds) currentEntryId = null
            revision++
        }
        respond(socket, 200, JSONObject().put("left", true))
    }

    private fun removeParticipant(socket: Socket, request: Request, actorParticipantId: String) {
        if (!isHost(actorParticipantId)) {
            respond(socket, 403, error("Účastníky může ze skupiny odebírat pouze hostitel."))
            return
        }
        val targetId = JSONObject(request.body).optString("participantId").trim()
        if (targetId.isBlank()) {
            respond(socket, 400, error("Není vybraný účastník k odebrání."))
            return
        }
        synchronized(lock) {
            val target = participants[targetId]
            if (target == null) {
                respond(socket, 404, error("Tento telefon už ve skupině není."))
                return
            }
            if (target.isHost) {
                respond(socket, 403, error("Hostitele nelze odebrat ze své vlastní skupiny."))
                return
            }
            participants.remove(targetId)
            val revokedTokens = tokens.entries
                .filter { it.value == targetId }
                .map { it.key }
            revokedTokens.forEach { revokedToken -> tokens.remove(revokedToken) }
            val removedEntries = GroupQueueLogic.entriesFromParticipant(entries, targetId)
            entries.removeAll { it.senderParticipantId == targetId }
            currentEntryId = GroupQueueLogic.currentEntryAfterParticipantRemoval(
                currentEntryId,
                removedEntries
            )
            revision++
        }
        respond(socket, 200, snapshotJson(snapshot()))
    }

    private fun add(socket: Socket, request: Request, participantId: String) {
        val json = JSONObject(request.body)
        val title = json.optString("title").trim().take(180)
        val mime = json.optString("mimeType").trim().take(120)
        val url = json.optString("mediaUrl").trim()
        val size = if (json.isNull("sizeBytes")) null else json.optLong("sizeBytes", -1L)
        if (title.isBlank() || mime.isBlank() || !validMediaUrl(url) || (size != null && size < 0L)) {
            respond(socket, 400, error("Položka nemá platný název, MIME typ nebo odkaz do místní sítě."))
            return
        }
        synchronized(lock) {
            // Re-check membership while holding the same lock used for queue insertion.
            // This prevents an in-flight request from re-adding items after the host removes its sender.
            val sender = participants[participantId]
            if (sender == null) {
                respond(socket, 401, error("Účastník už není součástí skupiny."))
                return
            }
            if (entries.size >= MAX_ENTRIES) {
                respond(socket, 409, error("Sdílená fronta může obsahovat nejvýše $MAX_ENTRIES položek."))
                return
            }
            entries.add(
                GroupQueueEntry(
                    UUID.randomUUID().toString(), title, mime, url, size,
                    participantId, sender.displayName, System.currentTimeMillis()
                )
            )
            revision++
        }
        respond(socket, 200, snapshotJson(snapshot()))
    }

    private fun remove(socket: Socket, request: Request, participantId: String) {
        if (!isHost(participantId)) {
            respond(socket, 403, error("Položky může odebírat pouze hostitel."))
            return
        }
        val id = JSONObject(request.body).optString("id")
        synchronized(lock) {
            if (!entries.removeAll { it.id == id }) {
                respond(socket, 404, error("Položka už ve frontě není."))
                return
            }
            if (currentEntryId == id) currentEntryId = null
            revision++
        }
        respond(socket, 200, snapshotJson(snapshot()))
    }

    private fun move(socket: Socket, request: Request, participantId: String) {
        if (!isHost(participantId)) {
            respond(socket, 403, error("Pořadí může měnit pouze hostitel."))
            return
        }
        val json = JSONObject(request.body)
        val from = json.optInt("fromIndex", -1)
        val to = json.optInt("toIndex", -1)
        synchronized(lock) {
            if (from !in entries.indices || to !in entries.indices) {
                respond(socket, 400, error("Neplatné pořadí položek."))
                return
            }
            if (from != to) {
                val entry = entries.removeAt(from)
                entries.add(to, entry)
                revision++
            }
        }
        respond(socket, 200, snapshotJson(snapshot()))
    }

    private fun setCurrent(socket: Socket, request: Request, participantId: String) {
        if (!isHost(participantId)) {
            respond(socket, 403, error("Přehrávání může řídit pouze hostitel."))
            return
        }
        val id = JSONObject(request.body).optString("id").takeIf { it.isNotBlank() }
        synchronized(lock) {
            if (id != null && entries.none { it.id == id }) {
                respond(socket, 404, error("Vybraná položka už ve frontě není."))
                return
            }
            currentEntryId = id
            revision++
        }
        respond(socket, 200, snapshotJson(snapshot()))
    }

    private fun isHost(id: String) = synchronized(lock) { participants[id]?.isHost == true }

    private fun allowAttempt(remote: String) = joinLimiter.canAttempt(remote)

    private fun recordFailure(remote: String) = joinLimiter.recordFailure(remote)

    private fun validMediaUrl(value: String): Boolean {
        if (value.length !in 1..MAX_URL_LENGTH) return false
        return try {
            val uri = URI(value)
            if (uri.scheme != "http" || uri.userInfo != null || uri.port !in 1..65535 ||
                uri.query != null || uri.fragment != null ||
                !MEDIA_PATH_PATTERN.matches(uri.path.orEmpty())
            ) return false
            val host = uri.host ?: return false
            if (!IPV4_PATTERN.matches(host)) return false
            val address = InetAddress.getByName(host)
            address is Inet4Address && address.isSiteLocalAddress &&
                !address.isLoopbackAddress && !address.isLinkLocalAddress && !address.isAnyLocalAddress
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanName(value: String) =
        value.trim().replace(Regex("[\\r\\n\\t]"), " ").take(40).ifBlank { "Telefon" }

    private fun snapshotJson(snapshot: GroupQueueSnapshot): JSONObject {
        val memberArray = JSONArray()
        snapshot.participants.forEach { member ->
            memberArray.put(JSONObject().put("id", member.id).put("displayName", member.displayName).put("isHost", member.isHost))
        }
        val entryArray = JSONArray()
        snapshot.entries.forEach { entry ->
            entryArray.put(JSONObject()
                .put("id", entry.id).put("title", entry.title).put("mimeType", entry.mimeType)
                .put("mediaUrl", entry.mediaUrl).put("sizeBytes", entry.sizeBytes ?: JSONObject.NULL)
                .put("senderParticipantId", entry.senderParticipantId).put("senderName", entry.senderName)
                .put("addedAtEpochMillis", entry.addedAtEpochMillis))
        }
        return JSONObject()
            .put("sessionId", snapshot.sessionId).put("revision", snapshot.revision)
            .put("currentEntryId", snapshot.currentEntryId ?: JSONObject.NULL)
            .put("participants", memberArray).put("entries", entryArray)
    }

    private fun error(message: String) = JSONObject().put("error", message)

    private fun respond(socket: Socket, status: Int, json: JSONObject) {
        val body = json.toString().toByteArray(Charsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"; 400 -> "Bad Request"; 401 -> "Unauthorized"; 403 -> "Forbidden"
            404 -> "Not Found"; 409 -> "Conflict"; 413 -> "Payload Too Large"
            429 -> "Too Many Requests"; 503 -> "Service Unavailable"; else -> "Error"
        }
        val header = "HTTP/1.1 $status $reason\r\nContent-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${body.size}\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n" +
            "Connection: close\r\n\r\n"
        val output = socket.getOutputStream()
        output.write(header.toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    override fun close() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        clientSockets.forEach { client ->
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
        clientSockets.clear()
        executor.shutdownNow()
        tokens.clear()
        synchronized(lock) {
            entries.clear()
            participants.clear()
            currentEntryId = null
        }
    }

    companion object {
        private const val MAX_HEADER_BYTES = 8_192
        private const val MAX_BODY_BYTES = 8_192
        private const val MIN_CLIENT_THREADS = 2
        private const val MAX_CLIENT_THREADS = 6
        private const val MAX_QUEUED_CLIENTS = 24
        private const val CLIENT_KEEP_ALIVE_SECONDS = 30L
        private const val MAX_ENTRIES = 100
        private const val MAX_PARTICIPANTS = 16
        private const val MAX_URL_LENGTH = 2_048
        private val IPV4_PATTERN = Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}$")
        private val MEDIA_PATH_PATTERN = Regex("^/media/[A-Za-z0-9_-]{12}$")
        private val random = SecureRandom()
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        private fun createInviteCode() = buildString {
            repeat(8) { append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)]) }
        }

        private fun createSecret(): String {
            val bytes = ByteArray(24)
            random.nextBytes(bytes)
            return android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
        }
    }
}
