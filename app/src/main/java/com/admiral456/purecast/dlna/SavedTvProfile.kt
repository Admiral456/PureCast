package com.admiral456.purecast.dlna

import java.util.Base64

data class SavedTvProfile(
    val udn: String,
    val friendlyName: String,
    val manufacturer: String?,
    val modelName: String?
)

object SavedTvProfileCodec {
    fun encode(profiles: List<SavedTvProfile>): String =
        profiles.take(SavedTvLogic.MAX_SAVED_TVS).joinToString("\n") { profile ->
            listOf(
                profile.udn,
                profile.friendlyName,
                profile.manufacturer.orEmpty(),
                profile.modelName.orEmpty()
            ).joinToString("\t") { value ->
                Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(value.toByteArray(Charsets.UTF_8))
            }
        }

    fun decode(encoded: String?): List<SavedTvProfile> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.lineSequence()
            .take(SavedTvLogic.MAX_SAVED_TVS)
            .mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size != 4) return@mapNotNull null
                val decoded = fields.map { field ->
                    runCatching {
                        String(Base64.getUrlDecoder().decode(field), Charsets.UTF_8)
                    }.getOrNull() ?: return@mapNotNull null
                }
                val udn = decoded[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SavedTvProfile(
                    udn = udn,
                    friendlyName = decoded[1].ifBlank { "DLNA televizor" },
                    manufacturer = decoded[2].ifBlank { null },
                    modelName = decoded[3].ifBlank { null }
                )
            }
            .distinctBy { it.udn }
            .toList()
    }
}

object SavedTvLogic {
    const val MAX_SAVED_TVS = 20

    fun toggle(profiles: List<SavedTvProfile>, device: DlnaDevice): List<SavedTvProfile> {
        val udn = device.udn?.takeIf { it.isNotBlank() } ?: return profiles
        return if (profiles.any { it.udn == udn }) {
            profiles.filterNot { it.udn == udn }
        } else {
            (profiles + profileFrom(device)).takeLast(MAX_SAVED_TVS)
        }
    }

    fun refreshProfiles(
        profiles: List<SavedTvProfile>,
        discovered: List<DlnaDevice>
    ): List<SavedTvProfile> {
        val byUdn = discovered.mapNotNull { device ->
            device.udn?.takeIf { it.isNotBlank() }?.let { it to device }
        }.toMap()
        return profiles.map { saved ->
            val current = byUdn[saved.udn] ?: return@map saved
            profileFrom(current)
        }
    }

    fun preferredDeviceId(
        currentDeviceId: String?,
        discovered: List<DlnaDevice>,
        savedProfiles: List<SavedTvProfile>
    ): String? {
        currentDeviceId?.takeIf { id -> discovered.any { it.id == id } }?.let { return it }
        val savedUdn = savedProfiles.firstNotNullOfOrNull { saved ->
            discovered.firstOrNull { it.udn == saved.udn }?.id
        }
        return savedUdn ?: discovered.firstOrNull()?.id
    }

    fun profileFrom(device: DlnaDevice): SavedTvProfile {
        val udn = device.udn?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Televizor nemá stabilní UPnP identifikátor.")
        return SavedTvProfile(
            udn = udn,
            friendlyName = device.friendlyName,
            manufacturer = device.manufacturer,
            modelName = device.modelName
        )
    }
}
