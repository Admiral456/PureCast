package com.admiral456.purecast.media

data class MediaImportPlan(
    val acceptedUriKeys: List<String>,
    val alreadyPresentCount: Int,
    val skippedByLimit: Int,
    val availableSlots: Int
)

object MediaImportPolicy {
    fun plan(
        selectedUriKeys: List<String>,
        existingUriKeys: Set<String>,
        currentItemCount: Int,
        maxItems: Int,
        appendToLibrary: Boolean
    ): MediaImportPlan {
        val uniqueKeys = selectedUriKeys.distinct()
        val newKeys = if (appendToLibrary) {
            uniqueKeys.filterNot { it in existingUriKeys }
        } else {
            uniqueKeys
        }
        val availableSlots = if (appendToLibrary) {
            (maxItems - currentItemCount).coerceAtLeast(0)
        } else {
            maxItems.coerceAtLeast(0)
        }
        val accepted = newKeys.take(availableSlots)
        return MediaImportPlan(
            acceptedUriKeys = accepted,
            alreadyPresentCount = uniqueKeys.size - newKeys.size,
            skippedByLimit = newKeys.size - accepted.size,
            availableSlots = availableSlots
        )
    }
}
