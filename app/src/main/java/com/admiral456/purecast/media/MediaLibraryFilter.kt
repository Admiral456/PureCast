package com.admiral456.purecast.media

enum class MediaKindFilter {
    ALL,
    PHOTOS,
    VIDEOS
}

enum class MediaLibrarySort(val label: String) {
    NEWEST("Nejnovější"),
    NAME_ASCENDING("Název A–Z"),
    LARGEST_FIRST("Největší"),
    SMALLEST_FIRST("Nejmenší")
}

object MediaLibraryFilter {
    fun filter(
        media: List<MediaFile>,
        query: String,
        kind: MediaKindFilter
    ): List<MediaFile> = media.filter { file ->
        matches(file.displayName, file.mimeType, query, kind)
    }

    fun apply(
        media: List<MediaFile>,
        query: String,
        kind: MediaKindFilter,
        sort: MediaLibrarySort,
        album: String? = null
    ): List<MediaFile> {
        val filtered = filter(media, query, kind).filter { file ->
            album == null || file.albumName.equals(album, ignoreCase = true)
        }
        return when (sort) {
            MediaLibrarySort.NEWEST ->
                filtered.sortedByDescending { it.dateModifiedMillis ?: Long.MIN_VALUE }
            MediaLibrarySort.NAME_ASCENDING ->
                filtered.sortedBy { it.displayName.lowercase() }
            MediaLibrarySort.LARGEST_FIRST ->
                filtered.sortedByDescending { it.sizeBytes ?: Long.MIN_VALUE }
            MediaLibrarySort.SMALLEST_FIRST ->
                filtered.sortedBy { it.sizeBytes ?: Long.MAX_VALUE }
        }
    }

    fun albums(media: List<MediaFile>): List<String> = media
        .mapNotNull { it.albumName?.trim()?.takeIf(String::isNotEmpty) }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

    fun matches(
        displayName: String,
        mimeType: String,
        query: String,
        kind: MediaKindFilter
    ): Boolean {
        val normalizedQuery = query.trim().lowercase()
        val matchesName = normalizedQuery.isEmpty() ||
            displayName.lowercase().contains(normalizedQuery)
        val normalizedMime = mimeType.substringBefore(';').trim().lowercase()
        val matchesKind = when (kind) {
            MediaKindFilter.ALL -> true
            MediaKindFilter.PHOTOS -> normalizedMime.startsWith("image/")
            MediaKindFilter.VIDEOS -> normalizedMime.startsWith("video/")
        }
        return matchesName && matchesKind
    }
}
