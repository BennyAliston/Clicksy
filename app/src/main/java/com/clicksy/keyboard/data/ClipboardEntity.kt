package com.clicksy.keyboard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a copied clipboard item.
 */
@Entity(tableName = "clipboard_history")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "text")
    val text: String,
    
    @ColumnInfo(name = "copied_timestamp")
    val copiedTimestamp: Long,
    
    @ColumnInfo(name = "pinned")
    val pinned: Boolean = false,
    
    @ColumnInfo(name = "favorite")
    val favorite: Boolean = false
)
