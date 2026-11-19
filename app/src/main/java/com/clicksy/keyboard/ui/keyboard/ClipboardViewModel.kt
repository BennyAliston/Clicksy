package com.clicksy.keyboard.ui.keyboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clicksy.keyboard.data.ClipboardCategory
import com.clicksy.keyboard.data.ClipboardEntity
import com.clicksy.keyboard.data.ClipboardManagerService
import com.clicksy.keyboard.data.ClipboardRepository
import com.clicksy.keyboard.data.ClipboardUtils
import com.clicksy.keyboard.data.TextTransformType
import com.clicksy.keyboard.service.ClicksyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing clipboard screen state, category filtering, search, and interactions.
 */
class ClipboardViewModel(
    private val repository: ClipboardRepository,
    context: Context? = null
) : ViewModel() {

    private val prefs = context?.getSharedPreferences(ClicksyService.PREFS_NAME, Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow(ClipboardCategory.ALL)
    val selectedCategory: StateFlow<ClipboardCategory> = _selectedCategory

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds

    private val _dismissedSuggestionIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _isIncognitoMode = MutableStateFlow(
        prefs?.getBoolean(ClipboardManagerService.KEY_CLIPBOARD_INCOGNITO, false) ?: false
    )
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode

    private val _autoCloseOnPaste = MutableStateFlow(
        prefs?.getBoolean(ClipboardManagerService.KEY_CLIPBOARD_AUTO_CLOSE, true) ?: true
    )
    val autoCloseOnPaste: StateFlow<Boolean> = _autoCloseOnPaste

    init {
        viewModelScope.launch {
            repository.removeAllDuplicates()
        }
    }

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
        val maxAge = 45 * 1000 // 45 seconds suggestion visibility
        if (age < maxAge && !dismissed.contains(latest.id)) {
            latest
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isMultiSelectMode: StateFlow<Boolean> = _selectedItemIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val categoryCounts: StateFlow<Map<ClipboardCategory, Int>> = repository.allItems.map { items ->
        ClipboardCategory.values().associateWith { category ->
            if (category == ClipboardCategory.ALL) {
                items.size
            } else {
                items.count { ClipboardUtils.matchesCategory(it, category) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val clipboardItems: StateFlow<List<ClipboardEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        repository.allItems
    ) { query, category, items ->
        var filtered = items
        if (category != ClipboardCategory.ALL) {
            filtered = filtered.filter { ClipboardUtils.matchesCategory(it, category) }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.text.contains(query, ignoreCase = true) }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleIncognitoMode() {
        val next = !_isIncognitoMode.value
        _isIncognitoMode.value = next
        prefs?.edit()?.putBoolean(ClipboardManagerService.KEY_CLIPBOARD_INCOGNITO, next)?.apply()
    }

    fun toggleAutoCloseOnPaste() {
        val next = !_autoCloseOnPaste.value
        _autoCloseOnPaste.value = next
        prefs?.edit()?.putBoolean(ClipboardManagerService.KEY_CLIPBOARD_AUTO_CLOSE, next)?.apply()
    }

    fun dismissSuggestion(id: Long) {
        _dismissedSuggestionIds.value = _dismissedSuggestionIds.value + id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: ClipboardCategory) {
        _selectedCategory.value = category
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

    fun updateItemText(id: Long, newText: String) {
        viewModelScope.launch {
            repository.updateText(id, newText)
        }
    }

    fun addCustomClip(text: String, isPinned: Boolean = true) {
        viewModelScope.launch {
            repository.addCustomClip(text, isPinned)
        }
    }

    fun cleanUrlForClip(item: ClipboardEntity) {
        val cleaned = ClipboardUtils.cleanUrl(item.text)
        if (cleaned != item.text) {
            updateItemText(item.id, cleaned)
        }
    }

    fun applyTransform(item: ClipboardEntity, transform: TextTransformType) {
        val transformed = ClipboardUtils.transformText(item.text, transform)
        if (transformed != item.text && transformed.isNotBlank()) {
            updateItemText(item.id, transformed)
        }
    }

    fun duplicateClip(item: ClipboardEntity) {
        viewModelScope.launch {
            repository.addCopy(item.text)
        }
    }

    fun splitMultilineClip(item: ClipboardEntity) {
        val lines = item.text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return
        viewModelScope.launch {
            lines.reversed().forEach { line ->
                repository.addCopy(line)
            }
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

    fun selectAll() {
        val allIds = clipboardItems.value.map { it.id }.toSet()
        _selectedItemIds.value = allIds
    }

    fun toggleSelectAll() {
        val current = _selectedItemIds.value
        val allItems = clipboardItems.value
        if (current.size == allItems.size && allItems.isNotEmpty()) {
            clearSelection()
        } else {
            _selectedItemIds.value = allItems.map { it.id }.toSet()
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
            val allFavorite = items.all { it.favorite }
            items.forEach { item ->
                repository.setFavorite(item.id, !allFavorite)
            }
            clearSelection()
        }
    }

    fun getSelectedCombinedText(separator: String = "\n"): String {
        val selected = _selectedItemIds.value
        val items = clipboardItems.value.filter { it.id in selected }
        return items.joinToString(separator) { it.text }
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

    class Factory(
        private val repository: ClipboardRepository,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClipboardViewModel::class.java)) {
                return ClipboardViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
