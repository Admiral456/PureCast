package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TvPairingDeepLinkTest {
    @Test
    fun createAndParseRoundTripForPrivateTvUrl() {
        val url = "http://192.168.1.20:49152/tv/Abcdefgh1234"
        val deepLink = TvPairingDeepLink.create(url, "083419")

        val parsed = TvPairingDeepLink.parse(deepLink)

        assertEquals(TvPairingLaunch(url, "083419"), parsed)
        assertTrue(deepLink.startsWith("purecast://pair?"))
    }

    @Test
    fun acceptsAllPrivateIpv4RangesUsedOnHomeNetworks() {
        listOf(
            "http://10.0.0.8:50000/tv/Abcdefgh1234",
            "http://172.20.1.8:50000/tv/Abcdefgh1234",
            "http://192.168.50.8:50000/tv/Abcdefgh1234"
        ).forEach { url ->
            assertEquals(
                TvPairingLaunch(url, "000001"),
                TvPairingDeepLink.parse(linkFor(url, "000001"))
            )
        }
    }

    @Test
    fun rejectsPublicHostsAndNonHttpTargets() {
        assertNull(TvPairingDeepLink.parse(linkFor(
            "http://8.8.8.8:49152/tv/Abcdefgh1234", "123456"
        )))
        assertNull(TvPairingDeepLink.parse(linkFor(
            "https://192.168.1.20:49152/tv/Abcdefgh1234", "123456"
        )))
    }

    @Test
    fun rejectsInvalidPathPortAndCode() {
        assertNull(TvPairingDeepLink.parse(linkFor(
            "http://192.168.1.20:49152/media/Abcdefgh1234", "123456"
        )))
        assertNull(TvPairingDeepLink.parse(linkFor(
            "http://192.168.1.20:0/tv/Abcdefgh1234", "123456"
        )))
        assertNull(TvPairingDeepLink.parse(linkFor(
            "http://192.168.1.20:49152/tv/Abcdefgh1234", "12A456"
        )))
    }

    @Test
    fun rejectsDuplicateOrUnexpectedQueryParameters() {
        val encoded = URLEncoder.encode(
            "http://192.168.1.20:49152/tv/Abcdefgh1234",
            StandardCharsets.UTF_8.name()
        )
        assertNull(TvPairingDeepLink.parse(
            "purecast://pair?url=$encoded&url=$encoded&code=123456"
        ))
        assertNull(TvPairingDeepLink.parse(
            "purecast://pair?url=$encoded&code=123456&other=value"
        ))
    }

    private fun linkFor(url: String, code: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        return "purecast://pair?url=$encoded&code=$code"
    }
}
