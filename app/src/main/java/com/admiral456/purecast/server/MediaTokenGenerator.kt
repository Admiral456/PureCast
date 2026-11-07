package com.admiral456.purecast.server

import java.security.SecureRandom
import java.util.Base64

internal object MediaTokenGenerator {
    private val secureRandom = SecureRandom()

    fun create(): String {
        val bytes = ByteArray(9)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
