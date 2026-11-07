package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTokenGeneratorTest {
    @Test
    fun generatesTwelveCharacterUrlSafeToken() {
        val token = MediaTokenGenerator.create()
        assertEquals(12, token.length)
        assertTrue(token.matches(Regex("[A-Za-z0-9_-]{12}")))
    }

    @Test
    fun generatesDistinctTokens() {
        val tokens = (1..500).map { MediaTokenGenerator.create() }.toSet()
        assertEquals(500, tokens.size)
    }
}
