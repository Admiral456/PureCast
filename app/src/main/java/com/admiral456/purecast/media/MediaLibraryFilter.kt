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

data class MediaLibraryMetadata(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val dateModifiedMillis: Long?,
    val albumName: String?
)

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
        val filtered = filterByAlbum(filter(media, query, kind), album) { it.albumName }
        return sort(filtered, sort) { file ->
            MediaLibraryMetadata(
                displayName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                dateModifiedMillis = file.dateModifiedMillis,
                albumName = file.albumName
            )
        }
    }

    fun <T> sort(
        items: List<T>,
        sort: MediaLibrarySort,
        metadata: (T) -> MediaLibraryMetadata
    ): List<T> = when (sort) {
        MediaLibrarySort.NEWEST ->
            items.sortedByDescending { metadata(it).dateModifiedMillis ?: Long.MIN_VALUE }
        MediaLibrarySort.NAME_ASCENDING ->
            items.sortedBy { metadata(it).displayName.lowercase() }
        MediaLibrarySort.LARGEST_FIRST ->
            items.sortedByDescending { metadata(it).sizeBytes ?: Long.MIN_VALUE }
        MediaLibrarySort.SMALLEST_FIRST ->
            items.sortedBy { metadata(it).sizeBytes ?: Long.MAX_VALUE }
    }

    fun <T> filterByAlbum(
        items: List<T>,
        album: String?,
        albumName: (T) -> String?
    ): List<T> = if (album == null) items else items.filter {
        albumName(it).equals(album, ignoreCase = true)
    }

    fun albumNames(names: Iterable<String?>): List<String> = names
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

    fun albums(media: List<MediaFile>): List<String> =
        albumNames(media.map { it.albumName })

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
