package com.clicksy.keyboard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository that abstracts access to the clipboard Room database.
 */
class ClipboardRepository(private val clipboardDao: ClipboardDao) {

    val allItems: Flow<List<ClipboardEntity>> = clipboardDao.getAllItems()

    fun searchItems(query: String): Flow<List<ClipboardEntity>> {
        val searchQuery = "%$query%"
        return clipboardDao.searchItems(searchQuery)
    }

    suspend fun addCopy(text: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        // 1. Check duplicate consecutive entry
        val latest = clipboardDao.getLatestItem()
        if (latest != null && latest.text == text) {
            return@withContext // Ignore duplicate consecutive entries
        }

        // 2. Check if text already exists anywhere in history
        val existing = clipboardDao.getItemByText(text)
        val now = System.currentTimeMillis()
        if (existing != null) {
            // Update timestamp so it moves to top, preserving pinned and favorite
            clipboardDao.update(existing.copy(copiedTimestamp = now))
        } else {
            // Insert new
            clipboardDao.insert(
                ClipboardEntity(
                    text = text,
                    copiedTimestamp = now
                )
            )
        }

        // 3. Keep history limit (latest 100 items). Delete oldest non-pinned.
        val count = clipboardDao.getCount()
        if (count > 100) {
            clipboardDao.deleteOldestUnpinned(count - 100)
        }
    }

    suspend fun deleteItem(item: ClipboardEntity) = withContext(Dispatchers.IO) {
        clipboardDao.delete(item)
    }

    suspend fun deleteMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        clipboardDao.deleteMultiple(ids)
    }

    suspend fun deleteAllUnpinned() = withContext(Dispatchers.IO) {
        clipboardDao.deleteAllUnpinned()
    }

    suspend fun togglePin(item: ClipboardEntity) = withContext(Dispatchers.IO) {
        clipboardDao.updatePinned(item.id, !item.pinned)
    }

    suspend fun toggleFavorite(item: ClipboardEntity) = withContext(Dispatchers.IO) {
        clipboardDao.updateFavorite(item.id, !item.favorite)
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        clipboardDao.updatePinned(id, pinned)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        clipboardDao.updateFavorite(id, favorite)
    }
}
