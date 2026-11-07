package com.admiral456.purecast.playback

import android.content.Context
import com.admiral456.purecast.server.LocalMediaServer

object PureCastRuntime {
    @Volatile
    private var server: LocalMediaServer? = null

    @Synchronized
    fun getMediaServer(context: Context): LocalMediaServer {
        server?.let { return it }
        return LocalMediaServer(context.applicationContext).also { server = it }
    }

    @Synchronized
    fun shutdown() {
        server?.close()
        server = null
    }
}
