package com.admiral456.purecast.group

data class GroupQueueEntry(
    val id: String,
    val title: String,
    val mimeType: String,
    val mediaUrl: String,
    val sizeBytes: Long?,
    val senderParticipantId: String,
    val senderName: String,
    val addedAtEpochMillis: Long
)

data class GroupQueueParticipant(
    val id: String,
    val displayName: String,
    val isHost: Boolean
)

data class GroupQueueSnapshot(
    val sessionId: String,
    val revision: Long,
    val currentEntryId: String?,
    val participants: List<GroupQueueParticipant>,
    val entries: List<GroupQueueEntry>
)

data class GroupQueueCredentials(
    val baseUrl: String,
    val accessToken: String,
    val participantId: String,
    val displayName: String,
    val isHost: Boolean,
    val inviteCode: String? = null
)

data class GroupQueueJoinResult(
    val credentials: GroupQueueCredentials,
    val snapshot: GroupQueueSnapshot
)

object GroupQueueLogic {
    fun nextIndex(currentIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 0) return null
        if (currentIndex !in 0 until itemCount) return 0
        return (currentIndex + 1).takeIf { it < itemCount }
    }

    fun previousIndex(currentIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
        return (currentIndex - 1).takeIf { it >= 0 }
    }

    fun move(entries: List<GroupQueueEntry>, fromIndex: Int, toIndex: Int): List<GroupQueueEntry> {
        if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) {
            return entries
        }
        return entries.toMutableList().also { mutable ->
            val entry = mutable.removeAt(fromIndex)
            mutable.add(toIndex, entry)
        }
    }

    fun currentIndex(entries: List<GroupQueueEntry>, currentEntryId: String?): Int =
        if (currentEntryId == null) -1 else entries.indexOfFirst { it.id == currentEntryId }

    fun nextEntry(entries: List<GroupQueueEntry>, currentEntryId: String?): GroupQueueEntry? {
        val index = nextIndex(currentIndex(entries, currentEntryId), entries.size) ?: return null
        return entries.getOrNull(index)
    }

    fun previousEntry(entries: List<GroupQueueEntry>, currentEntryId: String?): GroupQueueEntry? {
        val index = previousIndex(currentIndex(entries, currentEntryId), entries.size) ?: return null
        return entries.getOrNull(index)
    }
}
