package com.clicksy.keyboard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repository that abstracts access to the clipboard Room database.
 */
class ClipboardRepository(private val clipboardDao: ClipboardDao) {

    private val mutex = Mutex()

    val allItems: Flow<List<ClipboardEntity>> = clipboardDao.getAllItems()

    fun searchItems(query: String): Flow<List<ClipboardEntity>> {
        val searchQuery = "%$query%"
        return clipboardDao.searchItems(searchQuery)
    }

    suspend fun addCopy(text: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        mutex.withLock {
            // 1. Check duplicate consecutive entry
            val latest = clipboardDao.getLatestItem()
            if (latest != null && latest.text == text) {
                return@withLock
            }

            // 2. Check if text already exists anywhere in history
            val existing = clipboardDao.getItemByText(text)
            val now = System.currentTimeMillis()
            if (existing != null) {
                // Update timestamp so it moves to top, preserving pinned and favorite
                clipboardDao.update(existing.copy(copiedTimestamp = now))
                clipboardDao.deleteDuplicatesForText(text, existing.id)
            } else {
                // Insert new
                val newId = clipboardDao.insert(
                    ClipboardEntity(
                        text = text,
                        copiedTimestamp = now
                    )
                )
                clipboardDao.deleteDuplicatesForText(text, newId)
            }

            // 3. Keep history limit (latest 100 items). Delete oldest non-pinned.
            val count = clipboardDao.getCount()
            if (count > 100) {
                clipboardDao.deleteOldestUnpinned(count - 100)
            }
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

    suspend fun updateText(id: Long, newText: String) = withContext(Dispatchers.IO) {
        if (newText.isBlank()) return@withContext
        val existing = clipboardDao.getItemById(id) ?: return@withContext
        clipboardDao.update(existing.copy(text = newText))
    }

    suspend fun addCustomClip(text: String, isPinned: Boolean = true) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext
        mutex.withLock {
            val now = System.currentTimeMillis()
            val existing = clipboardDao.getItemByText(text)
            if (existing != null) {
                clipboardDao.update(existing.copy(copiedTimestamp = now, pinned = if (isPinned) true else existing.pinned))
                clipboardDao.deleteDuplicatesForText(text, existing.id)
            } else {
                val newId = clipboardDao.insert(
                    ClipboardEntity(
                        text = text,
                        copiedTimestamp = now,
                        pinned = isPinned
                    )
                )
                clipboardDao.deleteDuplicatesForText(text, newId)
            }
        }
    }

    suspend fun removeAllDuplicates() = withContext(Dispatchers.IO) {
        mutex.withLock {
            clipboardDao.deleteDuplicates()
        }
    }
}
