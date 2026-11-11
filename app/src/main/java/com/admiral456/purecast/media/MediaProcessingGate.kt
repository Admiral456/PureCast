package com.admiral456.purecast.media

import kotlinx.coroutines.sync.Semaphore

/**
 * Shared limit for optional native media work started by the gallery.
 * Thumbnail and metadata decoding use the same two slots.
 */
object MediaProcessingGate {
    private val semaphore = Semaphore(2)

    suspend fun <T> withPermit(block: suspend () -> T): T {
        semaphore.acquire()
        try {
            return block()
        } finally {
            semaphore.release()
        }
    }
}
