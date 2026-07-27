package com.clicksy.keyboard.ui.keyboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.data.EmojiData
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
import kotlinx.coroutines.launch

/**
 * Full-screen emoji panel with category tabs and a single vertically-scrollable unified emoji grid.
 * Clicking category tabs scrolls directly to the corresponding category header.
 * Scrolling updates the selected category tab automatically (Gboard style).
 */
@Composable
fun EmojiPanel(
    recentEmojis: List<String>,
    onEmojiTap: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val categories = remember(recentEmojis.toList()) {
        EmojiData.categories.toMutableList().apply {
            // Populate "Recent" category
            this[0] = this[0].copy(emojis = recentEmojis)
        }
    }

    // Pre-calculate starting indices in the flat lazy grid list for each category header
    val categoryIndices = remember(categories) {
        val indices = mutableListOf<Int>()
        var currentCount = 0
        categories.forEach { category ->
            indices.add(currentCount)
            if (category.emojis.isEmpty()) {
                currentCount += 2 // 1 for Header + 1 for Placeholder text item
            } else {
                currentCount += 1 + category.emojis.size // 1 for Header + N emoji items
            }
        }
        indices
    }

    // Reactively compute the currently active category tab index based on scroll position
    val currentCategoryIndex by remember(categoryIndices) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            var matchedIndex = 0
            for (i in categoryIndices.indices) {
                if (firstVisible >= categoryIndices[i]) {
                    matchedIndex = i
                } else {
                    break
                }
            }
            matchedIndex
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // Toolbar Row matching SuggestionBar design language (height, background, bottom border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.suggestionBarHeight)
                .background(colors.suggestionBarBackground)
                .drawBehind {
                    val strokeWidthPx = dims.borderWidth.toPx()
                    drawLine(
                        color = colors.border,
                        start = Offset(0f, size.height - strokeWidthPx / 2),
                        end = Offset(size.width, size.height - strokeWidthPx / 2),
                        strokeWidth = strokeWidthPx
                    )
                }
                .padding(horizontal = dims.keyboardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icons Row (Horizontally scrollable to adapt to 9 categories on all screen sizes)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = index == currentCategoryIndex
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .animateContentSize(animationSpec = tween(150))
                            .drawBehind {
                                if (isSelected) {
                                    val shadowOffsetPx = 2.dp.toPx()
                                    drawRoundRect(
                                        color = colors.shadow,
                                        topLeft = Offset(shadowOffsetPx, shadowOffsetPx),
                                        size = Size(size.width, size.height),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                            }
                            .background(
                                color = if (isSelected) colors.accentKeyBackground else colors.keyBackground.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) dims.borderWidth else 1.dp,
                                color = if (isSelected) colors.border else colors.border.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    gridState.scrollToItem(categoryIndices[index])
                                }
                            }
                            .padding(horizontal = if (isSelected) 10.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustomCategoryTabIcon(
                            categoryName = category.name,
                            isSelected = isSelected,
                            size = 20.dp
                        )
                        if (isSelected) {
                            Text(
                                text = category.name,
                                style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textOnAccent
                            )
                        }
                    }
                }
            }
        }

        // Single, vertically unified scrollable Emoji grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dims.keyboardPadding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 68.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            categories.forEachIndexed { catIndex, category ->
                // Header (Spans all 8 columns)
                item(
                    key = "header_${category.name}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = category.name,
                        style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                    )
                }

                if (category.emojis.isEmpty()) {
                    // Empty category placeholder (Spans all 8 columns)
                    item(
                        key = "empty_${category.name}",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "No recent emojis used yet.",
                                style = ClicksyTypography.keyLabelSmall.copy(fontSize = 13.sp),
                                color = colors.textPrimary.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                } else {
                    // Grid item list for emojis
                    items(
                        items = category.emojis,
                        key = { emoji -> "${category.name}_$emoji" }
                    ) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEmojiTap(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                style = ClicksyTypography.emojiLabel,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Floating ABC key in the bottom-left corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 16.dp, start = 16.dp)
                    .size(56.dp, 48.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    }
                    .background(colors.accentKeyBackground, RoundedCornerShape(12.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(12.dp))
                    .clickable { onBackToKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textOnAccent
                )
            }

        // Floating / bookmark backspace key in the bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .size(56.dp, 48.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    }
                    .background(colors.actionKeyBackground, RoundedCornerShape(12.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(12.dp))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    style = ClicksyTypography.keyLabel.copy(fontSize = 22.sp),
                    color = colors.textOnAction
                )
            }
        }
    }
}
