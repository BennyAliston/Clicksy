package com.clicksy.keyboard.ui.keyboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent

/**
 * Premium Neubrutalist Suggestion Bar displayed above the keyboard.
 * Features 3D pill keycaps for word suggestions with primary word highlight,
 * clipboard quick paste, and action buttons.
 */
@Composable
fun SuggestionBar(
    suggestions: List<String>,
    onSuggestionTap: (String) -> Unit,
    onClipboardTap: () -> Unit,
    onVoiceTap: () -> Unit,
    modifier: Modifier = Modifier,
    recentClipboardText: String? = null,
    onPasteClipboard: (() -> Unit)? = null,
    onDismissClipboard: (() -> Unit)? = null
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions

    Row(
        modifier = modifier
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
            .padding(horizontal = dims.keyboardPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (recentClipboardText != null && onPasteClipboard != null && onDismissClipboard != null) {
            // Quick Paste Clipboard Chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onPasteClipboard() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📋",
                    fontSize = 14.sp
                )
                Text(
                    text = "Paste:",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                    color = colors.textOnAccent
                )
                Text(
                    text = recentClipboardText,
                    style = ClicksyTypography.suggestionText,
                    color = colors.textOnAccent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Dismiss Button
            Box(
                modifier = Modifier
                    .size(34.dp, 34.dp)
                    .background(colors.keyBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onDismissClipboard() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        } else {
            // Clipboard Tool Button
            Box(
                modifier = Modifier
                    .size(36.dp, 34.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    .background(colors.keyBackground, RoundedCornerShape(6.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onClipboardTap() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📋",
                    fontSize = 15.sp
                )
            }

            // Word Suggestion Slots (3 Interactive Neubrutalist Chips)
            val displaySuggestions = suggestions.take(3)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .animateContentSize(animationSpec = tween(150)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 3) {
                    val suggestion = displaySuggestions.getOrNull(i) ?: ""
                    val isPrimary = i == 0 && suggestion.isNotEmpty()
                    val hasText = suggestion.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (hasText) {
                                    Modifier
                                        .drawBehind {
                                            if (isPrimary) {
                                                drawRoundRect(
                                                    color = colors.shadow,
                                                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                                    size = Size(size.width, size.height),
                                                    cornerRadius = CornerRadius(6.dp.toPx())
                                                )
                                            }
                                        }
                                        .background(
                                            color = if (isPrimary) colors.accentKeyBackground else colors.keyBackground.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isPrimary) dims.borderWidth else 1.dp,
                                            color = if (isPrimary) colors.border else colors.border.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSuggestionTap(suggestion) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasText) {
                            Text(
                                text = suggestion,
                                style = ClicksyTypography.suggestionText.copy(
                                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isPrimary) 15.sp else 14.sp
                                ),
                                color = if (isPrimary) colors.textOnAccent else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Voice Input Tool Button
            Box(
                modifier = Modifier
                    .size(36.dp, 34.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    .background(colors.keyBackground, RoundedCornerShape(6.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onVoiceTap() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎤",
                    fontSize = 15.sp
                )
            }
        }
    }
}
