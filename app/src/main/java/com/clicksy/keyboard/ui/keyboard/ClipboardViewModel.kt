package com.clicksy.keyboard.ui.keyboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clicksy.keyboard.data.ClipboardEntity
import com.clicksy.keyboard.data.ClipboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing clipboard screen state and user interactions.
 */
class ClipboardViewModel(private val repository: ClipboardRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds

    private val _dismissedSuggestionIds = MutableStateFlow<Set<Long>>(emptySet())

    private val ticker = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(10000) // 10 seconds tick
        }
    }

    val clipboardSuggestion: StateFlow<ClipboardEntity?> = combine(
        repository.allItems,
        _dismissedSuggestionIds,
        ticker
    ) { items, dismissed, currentTime ->
        val latest = items.firstOrNull() ?: return@combine null
        val age = currentTime - latest.copiedTimestamp
        val maxAge = 5 * 60 * 1000 // 5 minutes suggestion visibility
        if (age < maxAge && !dismissed.contains(latest.id)) {
            latest
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isMultiSelectMode: StateFlow<Boolean> = _selectedItemIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissSuggestion(id: Long) {
        _dismissedSuggestionIds.value = _dismissedSuggestionIds.value + id
    }

    val clipboardItems: StateFlow<List<ClipboardEntity>> = combine(
        _searchQuery,
        repository.allItems
    ) { query, items ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { it.text.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(item: ClipboardEntity) {
        viewModelScope.launch {
            repository.togglePin(item)
        }
    }

    fun toggleFavorite(item: ClipboardEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun deleteItem(item: ClipboardEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _selectedItemIds.value = _selectedItemIds.value - item.id
        }
    }

    fun deleteAllUnpinned() {
        viewModelScope.launch {
            repository.deleteAllUnpinned()
            clearSelection()
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteMultiple(_selectedItemIds.value.toList())
            clearSelection()
        }
    }

    fun pinSelected() {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            val items = clipboardItems.value.filter { it.id in selected }
            // If all selected are pinned, unpin them; otherwise pin them all
            val allPinned = items.all { it.pinned }
            items.forEach { item ->
                repository.setPinned(item.id, !allPinned)
            }
            clearSelection()
        }
    }

    fun favoriteSelected() {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            val items = clipboardItems.value.filter { it.id in selected }
            // If all selected are favorites, remove from favorite; otherwise favorite them all
            val allFavorite = items.all { it.favorite }
            items.forEach { item ->
                repository.setFavorite(item.id, !allFavorite)
            }
            clearSelection()
        }
    }

    fun shareSelected(context: Context) {
        val selected = _selectedItemIds.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val items = clipboardItems.value.filter { it.id in selected }
            val shareText = items.joinToString("\n\n") { it.text }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Clips").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            clearSelection()
        }
    }

    class Factory(private val repository: ClipboardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClipboardViewModel::class.java)) {
                return ClipboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
