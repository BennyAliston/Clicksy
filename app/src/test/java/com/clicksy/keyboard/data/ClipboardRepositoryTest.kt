package com.clicksy.keyboard.data

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardRepositoryTest {

    private class FakeClipboardDao : ClipboardDao {
        val items = mutableListOf<ClipboardEntity>()
        private var idCounter = 1L

        override fun getAllItems(): Flow<List<ClipboardEntity>> = flowOf(items)
        override fun searchItems(searchQuery: String): Flow<List<ClipboardEntity>> = flowOf(emptyList())

        override suspend fun getLatestItem(): ClipboardEntity? = items.maxByOrNull { it.copiedTimestamp }
        override suspend fun getItemById(id: Long): ClipboardEntity? = items.find { it.id == id }
        override suspend fun getItemByText(text: String): ClipboardEntity? = items.find { it.text == text }
        override suspend fun getCount(): Int = items.size

        override suspend fun updatePinned(id: Long, pinned: Boolean) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) items[idx] = items[idx].copy(pinned = pinned)
        }

        override suspend fun updateFavorite(id: Long, favorite: Boolean) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx != -1) items[idx] = items[idx].copy(favorite = favorite)
        }

        override suspend fun insert(item: ClipboardEntity): Long {
            val id = idCounter++
            items.add(item.copy(id = id))
            return id
        }

        override suspend fun update(item: ClipboardEntity) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx != -1) items[idx] = item
        }

        override suspend fun delete(item: ClipboardEntity) {
            items.removeAll { it.id == item.id }
        }

        override suspend fun deleteMultiple(ids: List<Long>) {
            items.removeAll { it.id in ids }
        }

        override suspend fun deleteAllUnpinned() {
            items.removeAll { !it.pinned }
        }

        override suspend fun deleteOldestUnpinned(limit: Int) {
            val unpinned = items.filter { !it.pinned }.sortedBy { it.copiedTimestamp }.take(limit)
            items.removeAll(unpinned.toSet())
        }

        override suspend fun deleteDuplicatesForText(text: String, keepId: Long) {
            items.removeAll { it.text == text && it.id != keepId }
        }

        override suspend fun deleteDuplicates() {
            val grouped = items.groupBy { it.text }
            items.clear()
            for ((_, group) in grouped) {
                val kept = group.maxByOrNull { it.id }
                if (kept != null) items.add(kept)
            }
        }
    }

    @Test
    fun testConcurrentAddCopyDoesNotCreateDuplicates() = runBlocking {
        val dao = FakeClipboardDao()
        val repo = ClipboardRepository(dao)

        // Launch two concurrent adds of identical text
        val deferred1 = async { repo.addCopy("Hello World") }
        val deferred2 = async { repo.addCopy("Hello World") }
        deferred1.await()
        deferred2.await()

        assertEquals(1, dao.items.size)
        assertEquals("Hello World", dao.items[0].text)
    }

    @Test
    fun testRemoveAllDuplicates() = runBlocking {
        val dao = FakeClipboardDao()
        val repo = ClipboardRepository(dao)

        dao.items.add(ClipboardEntity(id = 1, text = "Duplicate", copiedTimestamp = 100))
        dao.items.add(ClipboardEntity(id = 2, text = "Duplicate", copiedTimestamp = 200))

        repo.removeAllDuplicates()

        assertEquals(1, dao.items.size)
        assertEquals(2L, dao.items[0].id)
    }
}
