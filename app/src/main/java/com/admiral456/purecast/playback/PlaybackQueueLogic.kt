package com.admiral456.purecast.playback

object PlaybackQueueLogic {
    fun nextIndex(currentIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 0) return null
        if (currentIndex !in 0 until itemCount) return 0
        return (currentIndex + 1).takeIf { it < itemCount }
    }

    fun previousIndex(currentIndex: Int, itemCount: Int): Int? {
        if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
        return (currentIndex - 1).takeIf { it >= 0 }
    }

    fun indexAfterRemoval(currentIndex: Int, removedIndex: Int, newItemCount: Int): Int {
        if (newItemCount <= 0 || currentIndex !in 0 until newItemCount + 1) return -1
        return when {
            removedIndex == currentIndex -> -1
            removedIndex < currentIndex -> currentIndex - 1
            else -> currentIndex
        }.takeIf { it in 0 until newItemCount } ?: -1
    }

    fun indexAfterMove(currentIndex: Int, fromIndex: Int, toIndex: Int, itemCount: Int): Int {
        if (currentIndex !in 0 until itemCount) return -1
        if (fromIndex !in 0 until itemCount || toIndex !in 0 until itemCount) return currentIndex
        return when {
            currentIndex == fromIndex -> toIndex
            fromIndex < toIndex && currentIndex in (fromIndex + 1)..toIndex -> currentIndex - 1
            fromIndex > toIndex && currentIndex in toIndex until fromIndex -> currentIndex + 1
            else -> currentIndex
        }
    }
}
