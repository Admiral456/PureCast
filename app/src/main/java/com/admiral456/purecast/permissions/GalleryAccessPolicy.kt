package com.admiral456.purecast.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class GalleryAccessKind {
    FULL,
    SELECTED,
    IMAGES_ONLY,
    VIDEOS_ONLY,
    NONE
}

data class GalleryAccessState(
    val canReadImages: Boolean,
    val canReadVideos: Boolean,
    val selectedAccess: Boolean,
    val legacyFullAccess: Boolean = false
) {
    val hasAnyAccess: Boolean
        get() = canReadImages || canReadVideos || selectedAccess || legacyFullAccess

    val kind: GalleryAccessKind
        get() = when {
            legacyFullAccess || (canReadImages && canReadVideos) -> GalleryAccessKind.FULL
            selectedAccess -> GalleryAccessKind.SELECTED
            canReadImages -> GalleryAccessKind.IMAGES_ONLY
            canReadVideos -> GalleryAccessKind.VIDEOS_ONLY
            else -> GalleryAccessKind.NONE
        }

    val label: String
        get() = when (kind) {
            GalleryAccessKind.FULL -> "Plný přístup k fotkám a videím"
            GalleryAccessKind.SELECTED -> "Omezený přístup – jen vybrané fotky a videa"
            GalleryAccessKind.IMAGES_ONLY -> "Přístup pouze k fotkám"
            GalleryAccessKind.VIDEOS_ONLY -> "Přístup pouze k videím"
            GalleryAccessKind.NONE -> "Přístup ke galerii není povolen"
        }

    val canQueryImages: Boolean
        get() = legacyFullAccess || canReadImages || selectedAccess

    val canQueryVideos: Boolean
        get() = legacyFullAccess || canReadVideos || selectedAccess
}

object GalleryAccessPolicy {
    fun read(context: Context): GalleryAccessState {
        val sdk = Build.VERSION.SDK_INT
        val legacy = sdk <= Build.VERSION_CODES.S_V2 &&
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        val images = sdk >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED
        val videos = sdk >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) ==
            PackageManager.PERMISSION_GRANTED
        val selected = sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.checkSelfPermission(READ_MEDIA_VISUAL_USER_SELECTED) ==
            PackageManager.PERMISSION_GRANTED

        return fromGrants(sdk, legacy, images, videos, selected)
    }

    fun permissionsForSdk(sdk: Int): Array<String> = when {
        sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            READ_MEDIA_VISUAL_USER_SELECTED
        )
        sdk >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun fromGrants(
        sdk: Int,
        legacy: Boolean,
        images: Boolean,
        videos: Boolean,
        selected: Boolean
    ): GalleryAccessState = when {
        sdk <= Build.VERSION_CODES.S_V2 -> GalleryAccessState(
            canReadImages = legacy,
            canReadVideos = legacy,
            selectedAccess = false,
            legacyFullAccess = legacy
        )
        sdk == Build.VERSION_CODES.TIRAMISU -> GalleryAccessState(
            canReadImages = images,
            canReadVideos = videos,
            selectedAccess = false
        )
        else -> GalleryAccessState(
            canReadImages = images,
            canReadVideos = videos,
            selectedAccess = selected
        )
    }

    const val READ_MEDIA_VISUAL_USER_SELECTED =
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
}
