package com.clicksy.keyboard.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.data.EmojiData
import com.clicksy.keyboard.data.ShiftState
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent

private val ROW_LETTERS_1 = arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val ROW_LETTERS_2 = arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val ROW_LETTERS_3 = arrayOf("z", "x", "c", "v", "b", "n", "m")

private val ROW_SYMBOLS_1 = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val ROW_SYMBOLS_2 = arrayOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
private val ROW_SYMBOLS_3 = arrayOf("*", "\"", "'", ":", ";", "!", "?")

/**
 * Gboard-style Interactive Emoji Search Panel.
 *
 * Provides a faithful, Google Gboard-like emoji searching experience:
 * 1. Prominent top Search Bar with back arrow, real-time query display, cursor, and clear button.
 * 2. Instant live results strip above a full, comfortable QWERTY keyboard.
 * 3. Multi-tap emoji selection (tap multiple matching emojis without exiting).
 * 4. Trending search tags (Fire, Love, Smile, Food, Party, etc.) when the query is empty.
 * 5. Full grid expansion toggle (tap 😊 to view all search results in a vertical grid).
 * 6. Responsive Neubrutalist styling with bold borders, hard offset shadows, and tactile haptics.
 */
@Composable
fun EmojiSearchPanel(
    recentEmojis: List<String>,
    onEmojiTap: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onBackToEmojiPanel: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestHaptic: () -> Unit = {}
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }
    var isSymbolsMode by remember { mutableStateOf(false) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }

    val isShiftActive = shiftState != ShiftState.OFF

    val results = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            EmojiData.search(searchQuery)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = dims.keyboardPadding, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // =========================================================================
        // 1. TOP HEADER: Gboard-style Search Bar & Navigation
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Arrow Button (Return to Emoji Picker)
            Box(
                modifier = Modifier
                    .size(42.dp, 38.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                    .background(colors.keyBackground, RoundedCornerShape(8.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                    .clickable {
                        onRequestHaptic()
                        onBackToEmojiPanel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    style = ClicksyTypography.keyLabel.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            // Search Query Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                    .background(colors.keyBackground, RoundedCornerShape(8.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                    .clickable {
                        if (isGridView) {
                            onRequestHaptic()
                            isGridView = false
                        }
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search emojis...",
                            style = ClicksyTypography.monoLabel,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = searchQuery,
                                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Animated Neubrutalist Cursor
                            Text(
                                text = "❙",
                                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                                color = colors.accentKeyBackground
                            )
                        }

                        // Clear Query Button (✕)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.actionKeyBackground.copy(alpha = 0.2f))
                                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable {
                                    onRequestHaptic()
                                    searchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Grid / Keyboard Toggle Button
            Box(
                modifier = Modifier
                    .size(42.dp, 38.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                    .background(
                        if (isGridView) colors.accentKeyBackground else colors.keyBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                    .clickable {
                        onRequestHaptic()
                        isGridView = !isGridView
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGridView) "⌨️" else "😊",
                    fontSize = 18.sp
                )
            }
        }

        // =========================================================================
        // 2. MAIN BODY: EITHER KEYBOARD TYPING VIEW OR FULL GRID VIEW
        // =========================================================================
        if (isGridView) {
            // FULL GRID VIEW: Display all results in a scrollable vertical grid
            val gridEmojis = if (searchQuery.isBlank()) recentEmojis.ifEmpty { EmojiData.popularSearchTags.map { it.emoji } } else results

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (gridEmojis.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emojis found for \"$searchQuery\"",
                            style = ClicksyTypography.monoLabel,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = if (searchQuery.isBlank()) "Recent & Trending Emojis" else "${results.size} matches for \"$searchQuery\"",
                                style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        items(gridEmojis, key = { it }) { emoji ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        onRequestHaptic()
                                        onEmojiTap(emoji)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 26.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Bottom bar when in grid mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(colors.accentKeyBackground, RoundedCornerShape(8.dp))
                            .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                onRequestHaptic()
                                isGridView = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌨️ Switch to Keyboard",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = colors.textOnAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(38.dp)
                            .background(colors.keyBackground, RoundedCornerShape(8.dp))
                            .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                onRequestHaptic()
                                onBackToKeyboard()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ABC",
                            style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        } else {
            // TYPING VIEW: Live Results Strip + Full QWERTY Keyboard
            // 2A. Results Carousel / Quick Tags Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(colors.suggestionBarBackground, RoundedCornerShape(8.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isBlank()) {
                    // Quick Search Tags when query is empty
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmojiData.popularSearchTags.forEach { (tag, icon) ->
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.keyBackground)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(16.dp))
                                    .clickable {
                                        onRequestHaptic()
                                        searchQuery = tag.lowercase()
                                    }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = icon, fontSize = 13.sp)
                                Text(
                                    text = tag,
                                    style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                } else if (results.isNotEmpty()) {
                    // Live Matched Emoji Results Strip
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(results, key = { it }) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.keyBackground)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onRequestHaptic()
                                        onEmojiTap(emoji)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    style = ClicksyTypography.emojiLabel.copy(fontSize = 20.sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // No Results Fallback
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "No emoji found for \"$searchQuery\"",
                            style = ClicksyTypography.monoLabel,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        Text(
                            text = "Clear",
                            style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                            color = colors.actionKeyBackground,
                            modifier = Modifier
                                .clickable {
                                    onRequestHaptic()
                                    searchQuery = ""
                                }
                                .padding(end = 8.dp)
                        )
                    }
                }
            }

            // 2B. Full QWERTY Keyboard
            val keyHeight = remember(dims.keyHeight) {
                (dims.keyHeight * 0.78f).coerceIn(36.dp, 44.dp)
            }

            if (!isSymbolsMode) {
                // ==================== LETTERS MODE ====================
                // Row 1: q w e r t y u i o p
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    ROW_LETTERS_1.forEach { char ->
                        val label = if (isShiftActive) char.uppercase() else char
                        EmojiSearchKey(
                            label = label,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += label
                                if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
                            }
                        )
                    }
                }

                // Row 2: a s d f g h j k l
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    ROW_LETTERS_2.forEach { char ->
                        val label = if (isShiftActive) char.uppercase() else char
                        EmojiSearchKey(
                            label = label,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += label
                                if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Row 3: Shift + z x c v b n m + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // Shift Key
                    EmojiSearchKey(
                        label = when (shiftState) {
                            ShiftState.OFF -> "⇧"
                            ShiftState.ONCE -> "⇧"
                            ShiftState.CAPS_LOCK -> "⇪"
                        },
                        height = keyHeight,
                        modifier = Modifier.weight(1.4f),
                        isAccent = isShiftActive,
                        onTap = {
                            onRequestHaptic()
                            shiftState = when (shiftState) {
                                ShiftState.OFF -> ShiftState.ONCE
                                ShiftState.ONCE -> ShiftState.CAPS_LOCK
                                ShiftState.CAPS_LOCK -> ShiftState.OFF
                            }
                        }
                    )

                    ROW_LETTERS_3.forEach { char ->
                        val label = if (isShiftActive) char.uppercase() else char
                        EmojiSearchKey(
                            label = label,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += label
                                if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
                            }
                        )
                    }

                    // Backspace Key
                    EmojiSearchKey(
                        label = "⌫",
                        height = keyHeight,
                        modifier = Modifier.weight(1.4f),
                        isAction = true,
                        onTap = {
                            onRequestHaptic()
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            } else {
                                onBackToEmojiPanel()
                            }
                        },
                        onLongPress = {
                            onRequestHaptic()
                            searchQuery = ""
                        }
                    )
                }

                // Row 4: ?123, Comma, Space ("Search emojis"), Period, Search / Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // ?123 Switch Key
                    EmojiSearchKey(
                        label = "?123",
                        height = keyHeight,
                        modifier = Modifier.weight(1.4f),
                        onTap = {
                            onRequestHaptic()
                            isSymbolsMode = true
                        }
                    )

                    // Comma Key
                    EmojiSearchKey(
                        label = ",",
                        height = keyHeight,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onRequestHaptic()
                            searchQuery += ","
                        }
                    )

                    // Spacebar Key
                    EmojiSearchKey(
                        label = if (searchQuery.isEmpty()) "Search emojis" else "Space",
                        height = keyHeight,
                        modifier = Modifier.weight(4.4f),
                        onTap = {
                            onRequestHaptic()
                            if (searchQuery.isNotBlank() && !searchQuery.endsWith(" ")) {
                                searchQuery += " "
                            }
                        }
                    )

                    // Period Key
                    EmojiSearchKey(
                        label = ".",
                        height = keyHeight,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onRequestHaptic()
                            searchQuery += "."
                        }
                    )

                    // Search / Done Key
                    EmojiSearchKey(
                        label = "🔍",
                        height = keyHeight,
                        modifier = Modifier.weight(1.4f),
                        isAccent = true,
                        onTap = {
                            onRequestHaptic()
                            if (results.isNotEmpty()) {
                                onEmojiTap(results.first())
                            } else {
                                onBackToKeyboard()
                            }
                        }
                    )
                }
            } else {
                // ==================== SYMBOLS MODE ====================
                // Row 1: 1 2 3 4 5 6 7 8 9 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    ROW_SYMBOLS_1.forEach { char ->
                        EmojiSearchKey(
                            label = char,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += char
                            }
                        )
                    }
                }

                // Row 2: @ # $ % & - + ( ) /
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    ROW_SYMBOLS_2.forEach { char ->
                        EmojiSearchKey(
                            label = char,
                            height = keyHeight,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += char
                            }
                        )
                    }
                }

                // Row 3: Symbols + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    ROW_SYMBOLS_3.forEach { char ->
                        EmojiSearchKey(
                            label = char,
                            height = keyHeight,
                            modifier = Modifier.weight(1.2f),
                            onTap = {
                                onRequestHaptic()
                                searchQuery += char
                            }
                        )
                    }

                    // Backspace Key
                    EmojiSearchKey(
                        label = "⌫",
                        height = keyHeight,
                        modifier = Modifier.weight(1.6f),
                        isAction = true,
                        onTap = {
                            onRequestHaptic()
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            } else {
                                onBackToEmojiPanel()
                            }
                        },
                        onLongPress = {
                            onRequestHaptic()
                            searchQuery = ""
                        }
                    )
                }

                // Row 4: ABC, Comma, Space, Period, Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    EmojiSearchKey(
                        label = "ABC",
                        height = keyHeight,
                        modifier = Modifier.weight(1.5f),
                        isAccent = true,
                        onTap = {
                            onRequestHaptic()
                            isSymbolsMode = false
                        }
                    )

                    EmojiSearchKey(
                        label = ",",
                        height = keyHeight,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onRequestHaptic()
                            searchQuery += ","
                        }
                    )

                    EmojiSearchKey(
                        label = if (searchQuery.isEmpty()) "Search emojis" else "Space",
                        height = keyHeight,
                        modifier = Modifier.weight(4.4f),
                        onTap = {
                            onRequestHaptic()
                            if (searchQuery.isNotBlank() && !searchQuery.endsWith(" ")) {
                                searchQuery += " "
                            }
                        }
                    )

                    EmojiSearchKey(
                        label = ".",
                        height = keyHeight,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onRequestHaptic()
                            searchQuery += "."
                        }
                    )

                    EmojiSearchKey(
                        label = "🔍",
                        height = keyHeight,
                        modifier = Modifier.weight(1.5f),
                        isAccent = true,
                        onTap = {
                            onRequestHaptic()
                            if (results.isNotEmpty()) {
                                onEmojiTap(results.first())
                            } else {
                                onBackToKeyboard()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact Neubrutalist Key for Emoji Search typing.
 */
@Composable
private fun EmojiSearchKey(
    label: String,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isAction: Boolean = false,
    onTap: () -> Unit = {},
    onLongPress: (() -> Unit)? = null
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions

    val bg = when {
        isAction -> colors.actionKeyBackground
        isAccent -> colors.accentKeyBackground
        else -> colors.keyBackground
    }

    val textCol = when {
        isAction -> colors.textOnAction
        isAccent -> colors.textOnAccent
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .height(height)
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(dims.keyCornerRadius.toPx())
                )
            }
            .background(bg, RoundedCornerShape(dims.keyCornerRadius))
            .border(dims.borderWidth, colors.border, RoundedCornerShape(dims.keyCornerRadius))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (label.length > 2) ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp) else ClicksyTypography.keyLabel.copy(fontSize = 16.sp),
            color = textCol,
            textAlign = TextAlign.Center
        )
    }
}
