package com.clicksy.keyboard.ui.keyboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.data.ClipboardCategory
import com.clicksy.keyboard.data.ClipboardContentType
import com.clicksy.keyboard.data.ClipboardEntity
import com.clicksy.keyboard.data.ClipboardUtils
import com.clicksy.keyboard.data.TextTransformType
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Intelligent and feature-packed Neubrutalist Clipboard panel.
 * Supports:
 * - Smart categorization (All, Pinned, Favorites, Links, Code, Contacts, Snippets)
 * - Incognito Mode toggle to pause recording
 * - Keep Open / Auto-close mode toggle
 * - Color swatch previews for Hex/RGB codes
 * - Direct action intents (Open URL, Send Email, Dial Phone)
 * - Real-time query search highlighting
 * - Text Transformations (UPPERCASE, lowercase, Title Case, Trim, Single Line, Clean Link)
 * - Multi-select batch paste with configurable separators (\n, comma, space)
 * - Quick Snippet creation with pre-built canned templates
 * - Duplicate & multiline split tools
 * - Tactile swipe gestures (Green Pin, Red Delete) with haptic feedback
 */
@Composable
fun ClipboardPanel(
    viewModel: ClipboardViewModel,
    onItemTap: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestHaptic: () -> Unit = {}
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val context = LocalContext.current

    val items by viewModel.clipboardItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val isIncognitoMode by viewModel.isIncognitoMode.collectAsState()
    val autoCloseOnPaste by viewModel.autoCloseOnPaste.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ClipboardEntity?>(null) }
    var itemToTransform by remember { mutableStateOf<ClipboardEntity?>(null) }
    var itemForActionMenu by remember { mutableStateOf<ClipboardEntity?>(null) }

    val separators = listOf(
        "\n" to "Lines",
        ", " to "Commas",
        " " to "Spaces"
    )
    var selectedSeparatorIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        // 1. Top Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(colors.suggestionBarBackground)
                .border(width = dims.borderWidth, color = colors.border)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isMultiSelectMode) {
                // Multi-select Toolbar
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cancel selection button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.clearSelection()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
                        }

                        // Select All / Deselect All Toggle
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .defaultMinSize(minWidth = 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.toggleSelectAll()
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val allSelected = selectedItemIds.size == items.size && items.isNotEmpty()
                            Text(
                                if (allSelected) "☐ None" else "☑ All",
                                style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "${selectedItemIds.size}",
                            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.ExtraBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Selection Actions
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Separator toggle chip
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .defaultMinSize(minWidth = 56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    selectedSeparatorIndex = (selectedSeparatorIndex + 1) % separators.size
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = separators[selectedSeparatorIndex].second,
                                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Batch Paste All Button
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .defaultMinSize(minWidth = 76.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .clickable {
                                    val (sep, _) = separators[selectedSeparatorIndex]
                                    val combined = viewModel.getSelectedCombinedText(sep)
                                    if (combined.isNotBlank()) {
                                        onRequestHaptic()
                                        onItemTap(combined)
                                        viewModel.clearSelection()
                                    }
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📋 Paste All",
                                style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = colors.textOnAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Pin selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.pinSelected()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📌", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        // Favorite selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.favoriteSelected()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        // Share selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.shareSelected(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📤", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        // Delete selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.actionKeyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    viewModel.deleteSelected()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗑️", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            } else if (isSearchActive) {
                // Search Input Toolbar
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        singleLine = true,
                        textStyle = ClicksyTypography.suggestionText.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(colors.keyBackground, RoundedCornerShape(6.dp))
                                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search clipboard...",
                                        style = ClicksyTypography.monoLabel,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${items.size}",
                        style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .clickable {
                                onRequestHaptic()
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.setSearchQuery("")
                                } else {
                                    isSearchActive = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
                    }
                }
            } else {
                // Normal Toolbar
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📋 Clipboard",
                            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 24.dp, minHeight = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.keyBackground)
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${items.size}",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Incognito Mode Toggle (Pauses recording)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(2.dp, if (isIncognitoMode) colors.border else colors.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .background(if (isIncognitoMode) colors.actionKeyBackground else colors.keyBackground)
                                    .clickable {
                                        onRequestHaptic()
                                        viewModel.toggleIncognitoMode()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕵️", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }

                            // Keep-Open / Auto-Close Toggle
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .defaultMinSize(minWidth = 54.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                    .background(if (autoCloseOnPaste) colors.keyBackground else colors.accentKeyBackground)
                                    .clickable {
                                        onRequestHaptic()
                                        viewModel.toggleAutoCloseOnPaste()
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (autoCloseOnPaste) "⚡ Auto" else "📌 Keep",
                                    style = ClicksyTypography.monoLabel.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (autoCloseOnPaste) colors.textPrimary else colors.textOnAccent
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Add Quick Snippet / Canned Note
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                    .background(colors.accentKeyBackground)
                                    .clickable {
                                        onRequestHaptic()
                                        showAddDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("➕", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }

                            // Search Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                    .background(colors.keyBackground)
                                    .clickable {
                                        onRequestHaptic()
                                        isSearchActive = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔍", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }

                            // Clear Unpinned Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                    .background(colors.actionKeyBackground)
                                    .clickable {
                                        onRequestHaptic()
                                        showClearConfirmDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑️", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }
                        }

                        // ABC Back button (generous minimum width and touch target)
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .defaultMinSize(minWidth = 58.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .clickable {
                                    onRequestHaptic()
                                    onBackToKeyboard()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ABC",
                                style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textOnAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Incognito Status Banner (if active)
        if (isIncognitoMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.actionKeyBackground.copy(alpha = 0.35f))
                    .border(1.dp, colors.border)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🕵️ Incognito Mode Active — New copies will not be saved",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "[Resume]",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.accentKeyBackground,
                    modifier = Modifier.clickable {
                        onRequestHaptic()
                        viewModel.toggleIncognitoMode()
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. Smart Category Filter Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .horizontalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClipboardCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val count = categoryCounts[category] ?: 0

                val bg = if (isSelected) colors.accentKeyBackground else colors.keyBackground
                val textCol = if (isSelected) colors.textOnAccent else colors.textPrimary
                val borderCol = if (isSelected) colors.border else colors.border.copy(alpha = 0.6f)

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .defaultMinSize(minWidth = 56.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(bg)
                        .border(1.5.dp, borderCol, RoundedCornerShape(17.dp))
                        .clickable {
                            onRequestHaptic()
                            viewModel.setCategory(category)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = category.icon, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    Text(
                        text = "${category.label} ($count)",
                        style = ClicksyTypography.monoLabel.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = textCol,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 3. List or Empty View
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📭", style = ClicksyTypography.popupLabel)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching entries" else "No clips in ${selectedCategory.label}",
                        style = ClicksyTypography.clipboardText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap ➕ to create a reusable snippet",
                        style = ClicksyTypography.monoLabel,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val isSelected = selectedItemIds.contains(item.id)
                    ClipboardItemRow(
                        item = item,
                        searchQuery = searchQuery,
                        isSelected = isSelected,
                        isSelectionMode = isMultiSelectMode,
                        onTap = {
                            if (isMultiSelectMode) {
                                onRequestHaptic()
                                viewModel.toggleSelection(item.id)
                            } else {
                                onItemTap(item.text)
                            }
                        },
                        onLongPress = {
                            onRequestHaptic()
                            viewModel.toggleSelection(item.id)
                        },
                        onPin = {
                            onRequestHaptic()
                            viewModel.togglePin(item)
                        },
                        onDelete = {
                            onRequestHaptic()
                            viewModel.deleteItem(item)
                        },
                        onCleanUrl = {
                            onRequestHaptic()
                            viewModel.cleanUrlForClip(item)
                        },
                        onMoreOptions = {
                            onRequestHaptic()
                            itemForActionMenu = item
                        },
                        onRequestHaptic = onRequestHaptic
                    )
                }
            }
        }
    }

    // Add Snippet Overlay
    if (showAddDialog) {
        AddSnippetDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text, isPinned ->
                viewModel.addCustomClip(text, isPinned)
                showAddDialog = false
            }
        )
    }

    // Edit Snippet Overlay
    itemToEdit?.let { item ->
        EditSnippetDialog(
            initialText = item.text,
            onDismiss = { itemToEdit = null },
            onSave = { newText ->
                viewModel.updateItemText(item.id, newText)
                itemToEdit = null
            }
        )
    }

    // Transform Clip Overlay
    itemToTransform?.let { item ->
        TransformClipDialog(
            item = item,
            onDismiss = { itemToTransform = null },
            onApplyTransform = { transform ->
                viewModel.applyTransform(item, transform)
                itemToTransform = null
            },
            onDuplicate = {
                viewModel.duplicateClip(item)
                itemToTransform = null
            },
            onSplitLines = {
                viewModel.splitMultilineClip(item)
                itemToTransform = null
            }
        )
    }

    // Clear Confirmation Overlay
    if (showClearConfirmDialog) {
        ClearConfirmDialog(
            onDismiss = { showClearConfirmDialog = false },
            onConfirm = {
                viewModel.deleteAllUnpinned()
                showClearConfirmDialog = false
            }
        )
    }

    // Secondary Actions Menu Overlay
    itemForActionMenu?.let { item ->
        ClipActionMenuDialog(
            item = item,
            onDismiss = { itemForActionMenu = null },
            onEdit = {
                itemForActionMenu = null
                onRequestHaptic()
                itemToEdit = item
            },
            onTransform = {
                itemForActionMenu = null
                onRequestHaptic()
                itemToTransform = item
            },
            onFavorite = {
                itemForActionMenu = null
                onRequestHaptic()
                viewModel.toggleFavorite(item)
            },
            onDelete = {
                itemForActionMenu = null
                onRequestHaptic()
                viewModel.deleteItem(item)
            }
        )
    }
}
}

/**
 * A Clipboard History Card featuring:
 * - Content Type Badges (Link, Code, Color with actual preview swatch, Email, Phone, Text)
 * - Search keyword highlighting
 * - Direct intent action buttons (Open URL, Call, Email)
 * - Quick format/transform modal
 * - Tactile swipe gestures (Right = Pin, Left = Delete)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ClipboardItemRow(
    item: ClipboardEntity,
    searchQuery: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onCleanUrl: () -> Unit,
    onMoreOptions: () -> Unit,
    onRequestHaptic: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDeleting by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Swipe offset tracking
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(durationMillis = 150)
    )

    // Tap scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 60)
    )

    // Format relative timestamp
    val relativeTime = remember(item.copiedTimestamp) {
        val diff = System.currentTimeMillis() - item.copiedTimestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(item.copiedTimestamp))
            }
        }
    }

    // Detected content type & metadata
    val contentType = remember(item.text) { ClipboardUtils.detectContentType(item.text) }
    val metadataText = remember(item.text) { ClipboardUtils.formatItemMetadata(item.text) }
    val colorValue = remember(item.text) {
        if (contentType == ClipboardContentType.COLOR) ClipboardUtils.parseHexColor(item.text) else null
    }
    val domainText = remember(item.text) {
        if (contentType == ClipboardContentType.URL) ClipboardUtils.extractDomain(item.text) else ""
    }
    val isCleanableUrl = remember(item.text) {
        ClipboardUtils.isUrl(item.text) && ClipboardUtils.cleanUrl(item.text) != item.text.trim()
    }

    // Search query highlighted text
    val annotatedDisplayText = remember(item.text, searchQuery) {
        if (searchQuery.isBlank()) {
            AnnotatedString(item.text)
        } else {
            buildAnnotatedString {
                val text = item.text
                val lowerText = text.lowercase()
                val lowerQuery = searchQuery.lowercase()
                var current = 0
                while (current < text.length) {
                    val idx = lowerText.indexOf(lowerQuery, current)
                    if (idx == -1) {
                        append(text.substring(current))
                        break
                    }
                    append(text.substring(current, idx))
                    pushStyle(
                        SpanStyle(
                            background = colors.accentKeyBackground,
                            color = colors.textOnAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    append(text.substring(idx, idx + lowerQuery.length))
                    pop()
                    current = idx + lowerQuery.length
                }
            }
        }
    }

    AnimatedVisibility(
        visible = !isDeleting,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        val shape = RoundedCornerShape(dims.keyCornerRadius)

        // Wrapper containing swipe background reveal tracks + draggable front card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(dims.shadowOffsetX.toPx(), dims.shadowOffsetY.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(dims.keyCornerRadius.toPx())
                    )
                }
        ) {
            // Background Action Track (revealed on horizontal swipe)
            if (animatedSwipeOffset != 0f) {
                val isSwipingRight = animatedSwipeOffset > 0
                val trackBg = if (isSwipingRight) colors.accentKeyBackground else colors.actionKeyBackground
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(trackBg)
                        .border(dims.borderWidth, colors.border, shape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (isSwipingRight) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (item.pinned) "📍 Unpin" else "📌 Pin", fontWeight = FontWeight.Bold, color = colors.textOnAccent)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️ Delete", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Foreground Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                    .pointerInput(item.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > 80.dp.toPx()) {
                                    // Swipe Right -> Toggle Pin
                                    onRequestHaptic()
                                    onPin()
                                } else if (swipeOffset < -80.dp.toPx()) {
                                    // Swipe Left -> Delete
                                    onRequestHaptic()
                                    isDeleting = true
                                    scope.launch {
                                        delay(200)
                                        onDelete()
                                    }
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = {
                                swipeOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeOffset = (swipeOffset + dragAmount).coerceIn(-130.dp.toPx(), 130.dp.toPx())
                            }
                        )
                    }
                    .background(
                        if (isSelected) colors.accentKeyBackground.copy(alpha = 0.22f) else colors.keyBackground,
                        shape
                    )
                    .border(
                        width = if (isSelected) (dims.borderWidth + 1.dp) else if (item.pinned) (dims.borderWidth + 0.5.dp) else dims.borderWidth,
                        color = if (isSelected) colors.actionKeyBackground else if (item.pinned) colors.accentKeyBackground else colors.border,
                        shape = shape
                    )
                    .combinedClickable(
                        onClick = {
                            isPressed = true
                            scope.launch {
                                delay(70)
                                isPressed = false
                                onTap()
                            }
                        },
                        onLongClick = {
                            onLongPress()
                        }
                    )
                    .padding(11.dp)
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    // Header: Badges and Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Content Type Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accentKeyBackground.copy(alpha = 0.25f))
                                    .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = contentType.icon,
                                        fontSize = 10.sp
                                    )
                                    // Inline Color Swatch if Color
                                    if (colorValue != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(colorValue))
                                                .border(0.8.dp, colors.border, RoundedCornerShape(2.dp))
                                        )
                                    }
                                    Text(
                                        text = if (domainText.isNotBlank()) domainText else contentType.label,
                                        style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            if (item.pinned) {
                                Text("📌", fontSize = 12.sp)
                            }
                            if (item.favorite) {
                                Text("⭐", fontSize = 12.sp)
                            }
                            if (isSelected) {
                                Text("✓ Selected", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.ExtraBold), color = colors.actionKeyBackground)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = metadataText,
                                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                                color = colors.textSecondary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "•",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = relativeTime,
                                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text Body
                    Text(
                        text = annotatedDisplayText,
                        style = if (contentType == ClipboardContentType.CODE) ClicksyTypography.monoLabel.copy(fontSize = 13.sp) else ClicksyTypography.clipboardText,
                        color = colors.textPrimary,
                        maxLines = if (isExpanded) 18 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Horizontally scrollable primary action row
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct Launch Intents
                            if (contentType == ClipboardContentType.URL) {
                                ActionChip(
                                    label = "🌐 Open",
                                    isAccent = true,
                                    onClick = {
                                        onRequestHaptic()
                                        try {
                                            val url = ClipboardUtils.extractFirstUrl(item.text) ?: item.text
                                            val valid = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(valid)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                            } else if (contentType == ClipboardContentType.EMAIL) {
                                ActionChip(
                                    label = "✉️ Mail",
                                    isAccent = true,
                                    onClick = {
                                        onRequestHaptic()
                                        try {
                                            val emailRegex = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")
                                            val email = emailRegex.find(item.text)?.value ?: item.text.trim()
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                Toast.makeText(context, "No mail app found", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                            } else if (contentType == ClipboardContentType.PHONE) {
                                ActionChip(
                                    label = "📞 Call",
                                    isAccent = true,
                                    onClick = {
                                        onRequestHaptic()
                                        try {
                                            val phone = item.text.filter { it.isDigit() || it == '+' }
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                            }

                            // Clean URL (if tracking params exist)
                            if (isCleanableUrl) {
                                ActionChip(
                                    label = "🧹 Clean",
                                    onClick = {
                                        onRequestHaptic()
                                        try {
                                            onCleanUrl()
                                            try {
                                                Toast.makeText(context, "Cleaned tracking tags", Toast.LENGTH_SHORT).show()
                                            } catch (_: Exception) {}
                                        } catch (_: Exception) {}
                                    }
                                )
                            }

                            // Quick Copy
                            ActionChip(
                                label = "📋 Copy",
                                onClick = {
                                    onRequestHaptic()
                                    try {
                                        val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        systemClipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Clicksy Clipboard", item.text))
                                        try {
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    } catch (_: Exception) {}
                                }
                            )

                            // Pin / Unpin
                            ActionChip(
                                label = if (item.pinned) "📍 Unpin" else "📌 Pin",
                                isActive = item.pinned,
                                onClick = {
                                    onRequestHaptic()
                                    try {
                                        onPin()
                                    } catch (_: Exception) {}
                                }
                            )

                            // More Actions Menu Button (Secondary Actions: Edit, Transform, Favorite, Delete)
                            ActionChip(
                                label = "⋮ More",
                                onClick = {
                                    onRequestHaptic()
                                    onMoreOptions()
                                }
                            )
                        }

                        // Expand / Collapse toggle if long
                        if (item.text.length > 90 || item.text.contains("\n")) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .height(32.dp)
                                    .defaultMinSize(minWidth = 56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keyBackground)
                                    .border(1.2.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onRequestHaptic()
                                        isExpanded = !isExpanded
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isExpanded) "▲ Less" else "▼ More",
                                    style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A tactile Neubrutalist action chip button for the clipboard action row.
 */
@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isActive: Boolean = false,
    isDanger: Boolean = false
) {
    val colors = ClicksyTheme.colors
    val bg = when {
        isDanger -> colors.actionKeyBackground
        isAccent -> colors.accentKeyBackground
        isActive -> colors.accentKeyBackground.copy(alpha = 0.25f)
        else -> colors.keyBackground
    }
    val textCol = when {
        isAccent -> colors.textOnAccent
        else -> colors.textPrimary
    }
    val borderCol = when {
        isAccent -> colors.border
        isActive -> colors.accentKeyBackground
        else -> colors.border.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .defaultMinSize(minWidth = 52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.2.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ClicksyTypography.monoLabel.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            ),
            color = textCol,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * In-place modal overlay container for the IME keyboard context.
 * Replaces androidx.compose.ui.window.Dialog to prevent WindowManager$BadTokenException
 * in Android InputMethodService.
 */
@Composable
private fun ModalOverlayContainer(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks inside modal card */ }
        ) {
            content()
        }
    }
}

/**
 * Neubrutalist modal dialog presenting secondary actions for a clipboard clip.
 * Keeps the primary action row clean while offering full access to editing,
 * transformations, favorite bookmarking, and deletion.
 */
@Composable
private fun ClipActionMenuDialog(
    item: ClipboardEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTransform: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = ClicksyTheme.colors

    ModalOverlayContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚙️ Clip Actions",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "✕",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                // Text snippet preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.keyBackground)
                        .border(1.2.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.text.replace("\n", " ").trim(),
                        style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Action Row 1: Edit & Transform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✏️ Edit Note",
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                            .clickable { onTransform() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Transform",
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }

                // Action Row 2: Favorite & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (item.favorite) colors.accentKeyBackground.copy(alpha = 0.25f) else colors.keyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                            .clickable { onFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.favorite) "⭐ Unfavorite" else "☆ Favorite",
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.actionKeyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗑️ Delete Clip",
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Neubrutalist modal dialog to create a new reusable snippet or canned note.
 * Includes pre-made quick template pills (Email, Address, Phone, Social, Code)
 * and live character & word counters.
 */
@Composable
private fun AddSnippetDialog(
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    val colors = ClicksyTheme.colors
    var text by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(true) }

    val templates = listOf(
        "Email" to "Hi [Name],\n\nHope this finds you well.\n\nBest regards,",
        "Address" to "123 Main St, Apt 4B, City, Country",
        "Phone" to "+1 (555) 012-3456",
        "Social" to "Follow me on GitHub: @",
        "Code" to "```kotlin\nfun example() {\n    // code\n}\n```"
    )

    ModalOverlayContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "➕ New Snippet / Note",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${text.length} chars",
                        style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Templates Row
                Text(
                    text = "Quick Templates:",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    templates.forEach { (label, content) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.keyBackground)
                                .border(1.2.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable { text = content }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    maxLines = 6,
                    textStyle = ClicksyTypography.clipboardText.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(colors.keyBackground, RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Enter canned response, address, link, or note...",
                                    style = ClicksyTypography.monoLabel,
                                    color = colors.textSecondary
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pin toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPinned = !isPinned },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPinned) colors.accentKeyBackground else colors.keyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPinned) {
                            Text("✓", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textOnAccent)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin snippet to top (keep permanently)",
                        style = ClicksyTypography.monoLabel,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Cancel", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(if (text.isNotBlank()) colors.accentKeyBackground else colors.keyBackground.copy(alpha = 0.5f))
                            .clickable(enabled = text.isNotBlank()) {
                                onSave(text, isPinned)
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text("Save", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textOnAccent)
                    }
                }
            }
        }
    }
}

/**
 * Neubrutalist modal dialog to edit an existing clipboard snippet.
 */
@Composable
private fun EditSnippetDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = ClicksyTheme.colors
    var text by remember { mutableStateOf(initialText) }

    ModalOverlayContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "✏️ Edit Clip",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${text.length} chars",
                        style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    maxLines = 6,
                    textStyle = ClicksyTypography.clipboardText.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(colors.keyBackground, RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            inner()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Cancel", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(if (text.isNotBlank()) colors.accentKeyBackground else colors.keyBackground.copy(alpha = 0.5f))
                            .clickable(enabled = text.isNotBlank()) {
                                onSave(text)
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text("Save", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textOnAccent)
                    }
                }
            }
        }
    }
}

/**
 * Quick Transform and Utility Modal Dialog for a clipboard clip.
 */
@Composable
private fun TransformClipDialog(
    item: ClipboardEntity,
    onDismiss: () -> Unit,
    onApplyTransform: (TextTransformType) -> Unit,
    onDuplicate: () -> Unit,
    onSplitLines: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val hasMultipleLines = item.text.contains("\n")

    ModalOverlayContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ Transform & Tools",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "✕",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Text(
                    text = "Select an instant text transformation:",
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )

                // Grid of transformations
                val transforms = listOf(
                    TextTransformType.UPPERCASE,
                    TextTransformType.LOWERCASE,
                    TextTransformType.TITLE_CASE,
                    TextTransformType.TRIM,
                    TextTransformType.SINGLE_LINE
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transforms.take(3).forEach { t ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .clickable { onApplyTransform(t) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${t.icon} ${t.label}",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transforms.drop(3).forEach { t ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .clickable { onApplyTransform(t) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${t.icon} ${t.label}",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                    if (ClipboardUtils.isUrl(item.text)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .clickable { onApplyTransform(TextTransformType.CLEAN_URL) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🧹 Clean URL",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textOnAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Power Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                            .clickable { onDuplicate() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📑 Duplicate",
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }

                    if (hasMultipleLines) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .clickable { onSplitLines() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "➗ Split Lines",
                                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Neubrutalist confirmation modal dialog for clearing unpinned history safely.
 */
@Composable
private fun ClearConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = ClicksyTheme.colors

    ModalOverlayContainer(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.background, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "🗑️ Clear Unpinned Clips?",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will remove all recent unpinned clips. Your pinned snippets and favorites will be preserved.",
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.actionKeyBackground)
                            .clickable { onConfirm() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Clear", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

