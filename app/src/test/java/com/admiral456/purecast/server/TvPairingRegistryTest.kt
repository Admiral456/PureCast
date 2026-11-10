package com.admiral456.purecast.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPairingRegistryTest {
    @Test
    fun pairingCodeIsSixDigitsAndExpiresAfterFiveMinutes() {
        val registry = TvPairingRegistry()
        val ticket = registry.create("media-token-01", nowMillis = 10_000L)

        assertEquals(6, ticket.code.length)
        assertTrue(ticket.code.all(Char::isDigit))
        assertEquals(ticket, registry.ticket("media-token-01", nowMillis = 10_001L))
        assertNull(registry.ticket(
            "media-token-01",
            nowMillis = 10_000L + TvPairingRegistry.CODE_LIFETIME_MILLIS
        ))
    }

    @Test
    fun correctCodePairsOnceAndRemovesTheVisibleCode() {
        val registry = TvPairingRegistry()
        val ticket = registry.create("media-token-02", nowMillis = 0L)

        assertEquals(TvPairingResult.PAIRED, registry.verify("media-token-02", ticket.code, 1L))
        assertTrue(registry.isPaired("media-token-02"))
        assertNull(registry.ticket("media-token-02", 2L))
        assertEquals(TvPairingResult.ALREADY_PAIRED, registry.verify("media-token-02", ticket.code, 3L))
    }

    @Test
    fun incorrectCodesAreLockedAfterFiveAttempts() {
        val registry = TvPairingRegistry()
        registry.create("media-token-03", nowMillis = 0L)

        repeat(TvPairingRegistry.MAX_FAILED_ATTEMPTS - 1) {
            assertEquals(
                TvPairingResult.INVALID_CODE,
                registry.verify("media-token-03", "999999", it.toLong())
            )
        }
        assertEquals(
            TvPairingResult.LOCKED,
            registry.verify("media-token-03", "999999", 10L)
        )
        assertEquals(TvPairingResult.LOCKED, registry.verify("media-token-03", "000000", 11L))
    }

    @Test
    fun expiredCodeCannotBeUsed() {
        val registry = TvPairingRegistry()
        val ticket = registry.create("media-token-04", nowMillis = 100L)

        assertEquals(
            TvPairingResult.EXPIRED,
            registry.verify("media-token-04", ticket.code, ticket.expiresAtMillis)
        )
        assertFalse(registry.isPaired("media-token-04"))
    }

    @Test
    fun remoteCommandsRequirePairingAndDrainOnlyOnce() {
        val registry = TvPairingRegistry()
        val ticket = registry.create("media-token-05", nowMillis = 0L)

        assertFalse(registry.enqueue("media-token-05", TvRemoteAction.PAUSE))
        assertEquals(TvPairingResult.PAIRED, registry.verify("media-token-05", ticket.code, 1L))
        assertTrue(registry.enqueue("media-token-05", TvRemoteAction.PLAY))
        assertTrue(registry.enqueue("media-token-05", TvRemoteAction.SEEK, 42L))
        assertEquals(
            listOf(
                TvRemoteCommand(TvRemoteAction.PLAY),
                TvRemoteCommand(TvRemoteAction.SEEK, 42L)
            ),
            registry.drain("media-token-05")
        )
        assertTrue(registry.drain("media-token-05").isEmpty())
    }

    @Test
    fun seekRequiresAValidPosition() {
        val registry = TvPairingRegistry()
        val ticket = registry.create("media-token-06", nowMillis = 0L)
        registry.verify("media-token-06", ticket.code, 1L)

        assertFalse(registry.enqueue("media-token-06", TvRemoteAction.SEEK))
        assertFalse(registry.enqueue("media-token-06", TvRemoteAction.SEEK, -1L))
    }
}
