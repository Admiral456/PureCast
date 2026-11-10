package com.admiral456.purecast.dlna

import android.content.Context

class SavedTvStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun load(): List<SavedTvProfile> =
        SavedTvProfileCodec.decode(preferences.getString(KEY_PROFILES, null))

    @Synchronized
    fun save(profiles: List<SavedTvProfile>) {
        preferences.edit()
            .putString(KEY_PROFILES, SavedTvProfileCodec.encode(profiles))
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "purecast_saved_tvs"
        private const val KEY_PROFILES = "profiles_v1"
    }
}
