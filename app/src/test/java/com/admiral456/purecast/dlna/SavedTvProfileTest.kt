package com.admiral456.purecast.dlna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTvProfileTest {
    @Test
    fun codecRoundTripsUnicodeNamesAndOptionalMetadata() {
        val profiles = listOf(
            SavedTvProfile("uuid:tv-1", "Obývák – Samsung", "Samsung", "QN90")
        )

        assertEquals(profiles, SavedTvProfileCodec.decode(SavedTvProfileCodec.encode(profiles)))
    }

    @Test
    fun codecSkipsMalformedRecordsAndDuplicateIdentifiers() {
        val profile = SavedTvProfile("uuid:tv-2", "LG", null, null)
        val encoded = SavedTvProfileCodec.encode(listOf(profile))
        val decoded = SavedTvProfileCodec.decode("$encoded\nnot-a-profile\n$encoded")

        assertEquals(listOf(profile), decoded)
    }

    @Test
    fun togglingUsesStableUdnAndCannotSaveLocationOnlyDevice() {
        val device = device(id = "uuid:tv-3", udn = "uuid:tv-3")
        val added = SavedTvLogic.toggle(emptyList(), device)

        assertEquals(listOf(SavedTvLogic.profileFrom(device)), added)
        assertTrue(SavedTvLogic.toggle(added, device).isEmpty())
        assertTrue(SavedTvLogic.toggle(emptyList(), device.copy(udn = null)).isEmpty())
    }

    @Test
    fun refreshMatchesUdnAndUpdatesFriendlyNameAfterDiscovery() {
        val previous = SavedTvProfile("uuid:tv-4", "TV", "Sony", "Old model")
        val rediscovered = device(
            id = "uuid:tv-4",
            udn = "uuid:tv-4",
            friendlyName = "Bravia – obývák",
            manufacturer = "Sony",
            modelName = "New model",
            location = "http://192.168.1.55/device.xml"
        )

        assertEquals(
            listOf(SavedTvLogic.profileFrom(rediscovered)),
            SavedTvLogic.refreshProfiles(listOf(previous), listOf(rediscovered))
        )
    }

    @Test
    fun preferredDeviceKeepsCurrentSelectionThenUsesFirstSavedMatch() {
        val saved = listOf(
            SavedTvProfile("uuid:tv-5", "TV 5", null, null),
            SavedTvProfile("uuid:tv-6", "TV 6", null, null)
        )
        val first = device("uuid:tv-5", "uuid:tv-5")
        val second = device("uuid:tv-6", "uuid:tv-6")
        val found = listOf(first, second)

        assertEquals(
            second.id,
            SavedTvLogic.preferredDeviceId(second.id, found, saved)
        )
        assertEquals(
            first.id,
            SavedTvLogic.preferredDeviceId("offline", found, saved)
        )
        assertNull(SavedTvLogic.preferredDeviceId(null, emptyList(), saved))
    }

    private fun device(
        id: String,
        udn: String?,
        friendlyName: String = "TV",
        manufacturer: String? = null,
        modelName: String? = null,
        location: String = "http://192.168.1.20/device.xml"
    ) = DlnaDevice(
        id = id,
        friendlyName = friendlyName,
        manufacturer = manufacturer,
        modelName = modelName,
        location = location,
        avTransportControlUrl = "$location/control",
        serviceType = "urn:schemas-upnp-org:service:AVTransport:1",
        udn = udn
    )
}
