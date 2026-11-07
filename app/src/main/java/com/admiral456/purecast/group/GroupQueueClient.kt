package com.admiral456.purecast.group

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GroupQueueClient {
    fun join(address: String, inviteCode: String, displayName: String): GroupQueueJoinResult {
        val baseUrl = GroupQueueAddress.normalize(address)
        val response = request(
            baseUrl, null, "POST", "/v1/join",
            JSONObject().put("code", inviteCode.trim().uppercase()).put("displayName", displayName.trim().take(40))
        )
        val credentials = GroupQueueCredentials(
            baseUrl = baseUrl,
            accessToken = response.getString("accessToken"),
            participantId = response.getString("participantId"),
            displayName = response.optString("displayName", displayName),
            isHost = response.optBoolean("isHost", false)
        )
        return GroupQueueJoinResult(credentials, parseSnapshot(response.getJSONObject("snapshot")))
    }

    fun getState(credentials: GroupQueueCredentials) =
        parseSnapshot(request(credentials.baseUrl, credentials.accessToken, "GET", "/v1/state", null))

    fun leave(credentials: GroupQueueCredentials) {
        request(credentials.baseUrl, credentials.accessToken, "POST", "/v1/leave", JSONObject())
    }

    fun add(
        credentials: GroupQueueCredentials,
        title: String,
        mimeType: String,
        mediaUrl: String,
        sizeBytes: Long?
    ): GroupQueueSnapshot {
        val payload = JSONObject()
            .put("title", title.take(180))
            .put("mimeType", mimeType.take(120))
            .put("mediaUrl", mediaUrl)
            .put("sizeBytes", sizeBytes ?: JSONObject.NULL)
        return parseSnapshot(request(credentials.baseUrl, credentials.accessToken, "POST", "/v1/queue/add", payload))
    }

    fun remove(credentials: GroupQueueCredentials, entryId: String) =
        parseSnapshot(request(credentials.baseUrl, credentials.accessToken, "POST", "/v1/queue/remove", JSONObject().put("id", entryId)))

    fun move(credentials: GroupQueueCredentials, fromIndex: Int, toIndex: Int) =
        parseSnapshot(
            request(
                credentials.baseUrl, credentials.accessToken, "POST", "/v1/queue/move",
                JSONObject().put("fromIndex", fromIndex).put("toIndex", toIndex)
            )
        )

    fun setCurrent(credentials: GroupQueueCredentials, entryId: String?) =
        parseSnapshot(
            request(
                credentials.baseUrl, credentials.accessToken, "POST", "/v1/queue/current",
                JSONObject().put("id", entryId ?: "")
            )
        )

    private fun request(
        baseUrl: String,
        token: String?,
        method: String,
        path: String,
        body: JSONObject?
    ): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 6_000
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                throw IllegalStateException(
                    response.optString("error").ifBlank { "Sdílená fronta odpověděla HTTP $code." }
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSnapshot(json: JSONObject): GroupQueueSnapshot {
        val membersJson = json.optJSONArray("participants")
        val participants = buildList {
            if (membersJson != null) {
                for (index in 0 until membersJson.length()) {
                    val member = membersJson.optJSONObject(index) ?: continue
                    add(GroupQueueParticipant(
                        id = member.optString("id"),
                        displayName = member.optString("displayName"),
                        isHost = member.optBoolean("isHost")
                    ))
                }
            }
        }
        val entriesJson = json.optJSONArray("entries")
        val entries = buildList {
            if (entriesJson != null) {
                for (index in 0 until entriesJson.length()) {
                    val entry = entriesJson.optJSONObject(index) ?: continue
                    add(GroupQueueEntry(
                        id = entry.optString("id"),
                        title = entry.optString("title"),
                        mimeType = entry.optString("mimeType"),
                        mediaUrl = entry.optString("mediaUrl"),
                        sizeBytes = if (entry.isNull("sizeBytes")) null else entry.optLong("sizeBytes"),
                        senderParticipantId = entry.optString("senderParticipantId"),
                        senderName = entry.optString("senderName"),
                        addedAtEpochMillis = entry.optLong("addedAtEpochMillis")
                    ))
                }
            }
        }
        return GroupQueueSnapshot(
            sessionId = json.optString("sessionId"),
            revision = json.optLong("revision"),
            currentEntryId = if (json.isNull("currentEntryId")) null else json.optString("currentEntryId"),
            participants = participants,
            entries = entries
        )
    }

}
