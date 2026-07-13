package com.clicksy.keyboard.ui.keyboard

import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.data.ClipboardEntity
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Clipboard screen showing recent copied items.
 * Supports:
 * - Search filtering
 * - Pin/Favorite/Delete card buttons
 * - Swipe left to delete, swipe right to pin
 * - Long-press for multi-select mode with selection toolbar
 * - Playful Neobrutalist design with bold borders and hard offsets
 */
@Composable
fun ClipboardPanel(
    viewModel: ClipboardViewModel,
    onItemTap: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onLaunchSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val context = LocalContext.current

    val items by viewModel.clipboardItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Top Toolbar
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
                // Multi-select mode Toolbar
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cancel selection button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable { viewModel.clearSelection() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedItemIds.size} Selected",
                            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }

                    // Selection Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pin selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .clickable { viewModel.pinSelected() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📌", fontSize = 14.sp)
                        }
                        // Favorite selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .clickable { viewModel.favoriteSelected() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 14.sp)
                        }
                        // Share selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable { viewModel.shareSelected(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📤", fontSize = 14.sp)
                        }
                        // Delete selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.actionKeyBackground)
                                .clickable { viewModel.deleteSelected() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗑️", fontSize = 14.sp)
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
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search clipboard...",
                                        style = ClicksyTypography.monoLabel,
                                        color = colors.textSecondary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Clear search / Close search button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                            .background(colors.keyBackground)
                            .clickable {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.setSearchQuery("")
                                } else {
                                    isSearchActive = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
            } else {
                // Normal Toolbar
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📋 Clipboard",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable { isSearchActive = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍", fontSize = 14.sp)
                        }

                        // Delete All Button (Only deletes unpinned)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.actionKeyBackground)
                                .clickable { viewModel.deleteAllUnpinned() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗑️", fontSize = 14.sp)
                        }

                        // Settings Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.keyBackground)
                                .clickable { onLaunchSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 14.sp)
                        }

                        // ABC Back button
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                                .background(colors.accentKeyBackground)
                                .clickable { onBackToKeyboard() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ABC",
                                style = ClicksyTypography.keyLabelSmall,
                                color = colors.textOnAccent
                            )
                        }
                    }
                }
            }
        }

        // List or Empty View
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
                        text = if (searchQuery.isNotEmpty()) "No matching entries" else "Clipboard is empty",
                        style = ClicksyTypography.clipboardText.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Copied text will appear here",
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val isSelected = selectedItemIds.contains(item.id)
                    ClipboardItemRow(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isMultiSelectMode,
                        onTap = {
                            if (isMultiSelectMode) {
                                viewModel.toggleSelection(item.id)
                            } else {
                                onItemTap(item.text)
                            }
                        },
                        onLongPress = {
                            viewModel.toggleSelection(item.id)
                        },
                        onPin = { viewModel.togglePin(item) },
                        onFavorite = { viewModel.toggleFavorite(item) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }
}

/**
 * A Clipboard History Card featuring swipe gesture (pin/delete),
 * neobrutalist borders & shadows, expand/collapse capability,
 * and select states.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ClipboardItemRow(
    item: ClipboardEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
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
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(item.copiedTimestamp))
            }
        }
    }

    AnimatedVisibility(
        visible = !isDeleting,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        val shape = RoundedCornerShape(dims.keyCornerRadius)

        // Draw the neobrutalist shadow behind the card
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(item.id) {
                    // Custom swipe handler
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > 80.dp.toPx()) {
                                // Swipe Right -> Pin
                                onPin()
                            } else if (swipeOffset < -80.dp.toPx()) {
                                // Swipe Left -> Delete
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
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(-120.dp.toPx(), 120.dp.toPx())
                        }
                    )
                }
                .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                .background(
                    if (isSelected) colors.accentKeyBackground.copy(alpha = 0.2f) else colors.keyBackground,
                    shape
                )
                .border(
                    width = if (isSelected) (dims.borderWidth + 1.dp) else dims.borderWidth,
                    color = if (isSelected) colors.actionKeyBackground else colors.border,
                    shape = shape
                )
                .combinedClickable(
                    onClick = {
                        isPressed = true
                        scope.launch {
                            delay(80)
                            isPressed = false
                            onTap()
                        }
                    },
                    onLongClick = {
                        onLongPress()
                    }
                )
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                // Card header: status indicators & timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.pinned) {
                            Text("📌 Pinned", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (item.favorite) {
                            Text("⭐ Favorite", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (isSelected) {
                            Text("✓ Selected", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.ExtraBold), color = colors.actionKeyBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    Text(
                        text = relativeTime,
                        style = ClicksyTypography.monoLabel,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Text body (expandable)
                Text(
                    text = item.text,
                    style = ClicksyTypography.clipboardText,
                    color = colors.textPrimary,
                    maxLines = if (isExpanded) 15 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Row of operations: Copy, Pin, Favorite, Delete, Expand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Action icons row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Quick Copy button (doesn't paste, just sets system clipboard & shows Toast)
                        Text(
                            text = "📋 Copy",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.clickable {
                                val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                systemClipboard.setPrimaryClip(android.content.ClipData.newPlainText("Clicksy Clipboard", item.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Pin button
                        Text(
                            text = if (item.pinned) "📍 Unpin" else "📌 Pin",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.clickable { onPin() }
                        )

                        // Favorite button
                        Text(
                            text = if (item.favorite) "⭐ Unfav" else "☆ Fav",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.clickable { onFavorite() }
                        )

                        // Delete button
                        Text(
                            text = "✕ Delete",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.actionKeyBackground,
                            modifier = Modifier.clickable {
                                isDeleting = true
                                scope.launch {
                                    delay(200)
                                    onDelete()
                                }
                            }
                        )
                    }

                    // Expand / Collapse toggle if text is long (> 100 chars)
                    if (item.text.length > 100) {
                        Text(
                            text = if (isExpanded) "▲ Collapse" else "▼ Expand",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        )
                    }
                }
            }
        }
    }
}
