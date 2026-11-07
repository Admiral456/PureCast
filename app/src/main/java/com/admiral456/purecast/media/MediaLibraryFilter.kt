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
    ): List<MediaFile> {
        val normalizedQuery = query.trim().lowercase()
        return media.filter { file ->
            val matchesName = normalizedQuery.isEmpty() ||
                file.displayName.lowercase().contains(normalizedQuery)
            val matchesKind = when (kind) {
                MediaKindFilter.ALL -> true
                MediaKindFilter.PHOTOS -> file.isImage
                MediaKindFilter.VIDEOS -> file.isVideo
            }
            matchesName && matchesKind
        }
    }
}
