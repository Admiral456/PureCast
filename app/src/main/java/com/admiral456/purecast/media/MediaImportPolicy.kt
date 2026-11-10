package com.admiral456.purecast.media

/**
 * Deterministic safeguards for user-selected media.
 * The library is intentionally bounded so document providers cannot flood memory or metadata work.
 */
object MediaImportPolicy {
    fun <T, K> boundedDistinct(
        items: Iterable<T>,
        maxItems: Int,
        keyOf: (T) -> K
    ): List<T> {
        if (maxItems <= 0) return emptyList()
        val seen = HashSet<K>()
        val result = ArrayList<T>(maxItems.coerceAtMost(64))
        for (item in items) {
            if (seen.add(keyOf(item))) {
                result += item
                if (result.size >= maxItems) break
            }
        }
        return result
    }

    fun availableSlots(currentCount: Int, maximum: Int): Int =
        (maximum.coerceAtLeast(0) - currentCount.coerceAtLeast(0)).coerceAtLeast(0)
}
