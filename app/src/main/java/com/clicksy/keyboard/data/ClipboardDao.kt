package com.clicksy.keyboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clipboard history table.
 */
@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_history ORDER BY pinned DESC, copied_timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardEntity>>

    @Query("SELECT * FROM clipboard_history WHERE text LIKE :searchQuery ORDER BY pinned DESC, copied_timestamp DESC")
    fun searchItems(searchQuery: String): Flow<List<ClipboardEntity>>

    @Query("SELECT * FROM clipboard_history ORDER BY copied_timestamp DESC LIMIT 1")
    suspend fun getLatestItem(): ClipboardEntity?

    @Query("SELECT * FROM clipboard_history WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ClipboardEntity?

    @Query("SELECT * FROM clipboard_history WHERE text = :text LIMIT 1")
    suspend fun getItemByText(text: String): ClipboardEntity?

    @Query("SELECT COUNT(*) FROM clipboard_history")
    suspend fun getCount(): Int

    @Query("UPDATE clipboard_history SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("UPDATE clipboard_history SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardEntity): Long

    @Update
    suspend fun update(item: ClipboardEntity)

    @Delete
    suspend fun delete(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_history WHERE id IN (:ids)")
    suspend fun deleteMultiple(ids: List<Long>)

    @Query("DELETE FROM clipboard_history WHERE pinned = 0")
    suspend fun deleteAllUnpinned()

    @Query("DELETE FROM clipboard_history WHERE id IN (SELECT id FROM clipboard_history WHERE pinned = 0 ORDER BY copied_timestamp ASC LIMIT :limit)")
    suspend fun deleteOldestUnpinned(limit: Int)
}
