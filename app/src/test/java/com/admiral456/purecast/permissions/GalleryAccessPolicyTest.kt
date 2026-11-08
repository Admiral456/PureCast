package com.admiral456.purecast.permissions

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryAccessPolicyTest {
    @Test
    fun android12AndOlderUsesLegacyReadPermission() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            GalleryAccessPolicy.permissionsForSdk(Build.VERSION_CODES.S)
        )
        val state = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.S, legacy = true, images = false, videos = false, selected = false
        )
        assertEquals(GalleryAccessKind.FULL, state.kind)
        assertTrue(state.canQueryImages)
        assertTrue(state.canQueryVideos)
    }

    @Test
    fun android13RequestsImagesAndVideosTogether() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            ),
            GalleryAccessPolicy.permissionsForSdk(Build.VERSION_CODES.TIRAMISU)
        )
        val state = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.TIRAMISU,
            legacy = false,
            images = true,
            videos = false,
            selected = false
        )
        assertEquals(GalleryAccessKind.IMAGES_ONLY, state.kind)
        assertTrue(state.canQueryImages)
        assertFalse(state.canQueryVideos)
    }

    @Test
    fun android14RequestsFullAndSelectedAccessInOneOperation() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                GalleryAccessPolicy.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            GalleryAccessPolicy.permissionsForSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        )
        val state = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            legacy = false,
            images = false,
            videos = false,
            selected = true
        )
        assertEquals(GalleryAccessKind.SELECTED, state.kind)
        assertTrue(state.canQueryImages)
        assertTrue(state.canQueryVideos)
    }

    @Test
    fun android14CanGrantOnlyImageOrOnlyVideoAccess() {
        val imagesOnly = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            legacy = false,
            images = true,
            videos = false,
            selected = false
        )
        assertEquals(GalleryAccessKind.IMAGES_ONLY, imagesOnly.kind)
        assertTrue(imagesOnly.canQueryImages)
        assertFalse(imagesOnly.canQueryVideos)

        val videosOnly = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            legacy = false,
            images = false,
            videos = true,
            selected = false
        )
        assertEquals(GalleryAccessKind.VIDEOS_ONLY, videosOnly.kind)
        assertFalse(videosOnly.canQueryImages)
        assertTrue(videosOnly.canQueryVideos)
    }

    @Test
    fun android15AndLaterKeepSelectedMediaPermissionInRequest() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                GalleryAccessPolicy.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            GalleryAccessPolicy.permissionsForSdk(35)
        )
        assertArrayEquals(
            GalleryAccessPolicy.permissionsForSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
            GalleryAccessPolicy.permissionsForSdk(36)
        )
    }

    @Test
    fun noGrantMeansNoMediaStoreAccessButDoesNotImplyPickerBlocked() {
        val state = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            legacy = false,
            images = false,
            videos = false,
            selected = false
        )
        assertEquals(GalleryAccessKind.NONE, state.kind)
        assertFalse(state.hasAnyAccess)
        assertFalse(state.canQueryImages)
        assertFalse(state.canQueryVideos)
    }

    @Test
    fun fullAndroid14GrantIsNotMistakenForSelectedOnly() {
        val state = GalleryAccessPolicy.fromGrants(
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            legacy = false,
            images = true,
            videos = true,
            selected = true
        )
        assertEquals(GalleryAccessKind.FULL, state.kind)
    }
}
