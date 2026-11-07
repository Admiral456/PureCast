package com.admiral456.purecast.media

enum class MediaKindFilter {
    ALL,
    PHOTOS,
    VIDEOS
}

object MediaLibraryFilter {
    fun filter(
        media: List<MediaFile>,
        query: String,
        kind: MediaKindFilter
    ): List<MediaFile> = media.filter { file ->
        matches(file.displayName, file.mimeType, query, kind)
    }

    fun matches(
        displayName: String,
        mimeType: String,
        query: String,
        kind: MediaKindFilter
    ): Boolean {
        val normalizedQuery = query.trim().lowercase()
        val matchesName = normalizedQuery.isEmpty() ||
            displayName.lowercase().contains(normalizedQuery)
        val normalizedMime = mimeType.lowercase()
        val matchesKind = when (kind) {
            MediaKindFilter.ALL -> true
            MediaKindFilter.PHOTOS -> normalizedMime.startsWith("image/")
            MediaKindFilter.VIDEOS -> normalizedMime.startsWith("video/")
        }
        return matchesName && matchesKind
    }
}
