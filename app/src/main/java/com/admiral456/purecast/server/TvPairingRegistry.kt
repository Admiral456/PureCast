package com.admiral456.purecast.server

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

enum class TvPairingResult {
    PAIRED,
    ALREADY_PAIRED,
    INVALID_CODE,
    EXPIRED,
    LOCKED,
    NOT_FOUND
}

enum class TvRemoteAction(val wireValue: String) {
    PLAY("play"),
    PAUSE("pause"),
    STOP("stop"),
    SEEK("seek")
}

data class TvPairingTicket(
    val code: String,
    val expiresAtMillis: Long
)

data class TvRemoteCommand(
    val action: TvRemoteAction,
    val positionSeconds: Long? = null
)

class TvPairingRegistry(
    private val random: SecureRandom = SecureRandom()
) {
    private data class Session(
        var code: String?,
        val expiresAtMillis: Long,
        var failedAttempts: Int = 0,
        var paired: Boolean = false,
        val commands: ArrayDeque<TvRemoteCommand> = ArrayDeque()
    )

    private val sessions = linkedMapOf<String, Session>()

    @Synchronized
    fun create(token: String, nowMillis: Long = System.currentTimeMillis()): TvPairingTicket {
        require(token.isNotBlank()) { "Media token must not be blank." }
        val code = String.format(Locale.US, "%06d", random.nextInt(CODE_SPACE))
        val expiresAt = nowMillis + CODE_LIFETIME_MILLIS
        sessions[token] = Session(code = code, expiresAtMillis = expiresAt)
        return TvPairingTicket(code, expiresAt)
    }

    @Synchronized
    fun ticket(token: String, nowMillis: Long = System.currentTimeMillis()): TvPairingTicket? {
        val session = sessions[token] ?: return null
        if (session.paired || nowMillis >= session.expiresAtMillis) return null
        val code = session.code ?: return null
        return TvPairingTicket(code, session.expiresAtMillis)
    }

    @Synchronized
    fun verify(
        token: String,
        enteredCode: String,
        nowMillis: Long = System.currentTimeMillis()
    ): TvPairingResult {
        val session = sessions[token] ?: return TvPairingResult.NOT_FOUND
        if (session.paired) return TvPairingResult.ALREADY_PAIRED
        if (nowMillis >= session.expiresAtMillis) return TvPairingResult.EXPIRED
        if (session.failedAttempts >= MAX_FAILED_ATTEMPTS) return TvPairingResult.LOCKED

        val expected = session.code.orEmpty().toByteArray(Charsets.US_ASCII)
        val actual = enteredCode.trim().toByteArray(Charsets.US_ASCII)
        if (expected.isNotEmpty() && MessageDigest.isEqual(expected, actual)) {
            session.paired = true
            session.code = null
            session.failedAttempts = 0
            return TvPairingResult.PAIRED
        }

        session.failedAttempts++
        return if (session.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            TvPairingResult.LOCKED
        } else {
            TvPairingResult.INVALID_CODE
        }
    }

    @Synchronized
    fun isPaired(token: String): Boolean =
        sessions[token]?.paired == true

    @Synchronized
    fun enqueue(
        token: String,
        action: TvRemoteAction,
        positionSeconds: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val session = sessions[token] ?: return false
        if (!session.paired) return false
        if (nowMillis >= session.expiresAtMillis + PAIRED_SESSION_GRACE_MILLIS) {
            sessions.remove(token)
            return false
        }
        if (action == TvRemoteAction.SEEK && (positionSeconds == null || positionSeconds < 0L)) {
            return false
        }
        if (action != TvRemoteAction.SEEK && positionSeconds != null) return false
        if (session.commands.size >= MAX_PENDING_COMMANDS) return false
        session.commands.addLast(TvRemoteCommand(action, positionSeconds))
        return true
    }

    @Synchronized
    fun drain(token: String): List<TvRemoteCommand> {
        val session = sessions[token] ?: return emptyList()
        if (!session.paired) return emptyList()
        return buildList {
            while (session.commands.isNotEmpty()) add(session.commands.removeFirst())
        }
    }

    @Synchronized
    fun remove(token: String) {
        sessions.remove(token)
    }

    @Synchronized
    fun clear() {
        sessions.clear()
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val MAX_PENDING_COMMANDS = 16
        const val CODE_LIFETIME_MILLIS = 5 * 60 * 1_000L
        private const val PAIRED_SESSION_GRACE_MILLIS = 6 * 60 * 60 * 1_000L
        private const val CODE_SPACE = 1_000_000
    }
}
