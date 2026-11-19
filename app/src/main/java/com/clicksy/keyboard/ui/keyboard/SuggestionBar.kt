package com.clicksy.keyboard.ui.keyboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clicksy.keyboard.data.ClipboardContentType
import com.clicksy.keyboard.data.ClipboardUtils
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent

/**
 * Gboard-style Neubrutalist Suggestion and Tools Toolbar displayed above the keyboard.
 *
 * Behavior:
 * 1. Default (Idle/Not Typing): Tools (Settings, Clipboard, Calculator, Voice, Emojis) take the full space.
 * 2. Typing: When user starts typing, auto-suggestions appear across the bar, tools stay hidden,
 *    and a Tools Button appears at the left corner.
 * 3. Tapping the Tools Button while typing reveals the tools with a collapse button to return to suggestions.
 */
@Composable
fun SuggestionBar(
    suggestions: List<String>,
    onSuggestionTap: (String) -> Unit,
    onClipboardTap: () -> Unit,
    onVoiceTap: () -> Unit,
    onCalculatorTap: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onEmojiTap: (() -> Unit)? = null,
    isTyping: Boolean = false,
    autocorrectEnabled: Boolean = true,
    currentWord: String = "",
    modifier: Modifier = Modifier,
    recentClipboardText: String? = null,
    onPasteClipboard: (() -> Unit)? = null,
    onDismissClipboard: (() -> Unit)? = null
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions

    var showToolsWhileTyping by remember { mutableStateOf(false) }

    // Reset temporary tools view whenever the current typed word changes
    LaunchedEffect(currentWord) {
        showToolsWhileTyping = false
    }

    val shouldShowSuggestions = isTyping && !showToolsWhileTyping && suggestions.isNotEmpty()

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
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (recentClipboardText != null && onPasteClipboard != null && onDismissClipboard != null && !isTyping) {
            // Quick Paste Clipboard Chip (shown when idle and recent clipboard text exists)
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
                val contentType = ClipboardUtils.detectContentType(recentClipboardText)
                val colorVal = if (contentType == ClipboardContentType.COLOR) ClipboardUtils.parseHexColor(recentClipboardText) else null
                val domain = if (contentType == ClipboardContentType.URL) ClipboardUtils.extractDomain(recentClipboardText) else ""

                Text(
                    text = contentType.icon,
                    fontSize = 14.sp
                )
                if (colorVal != null) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(colorVal))
                            .border(1.dp, colors.border, RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    text = when (contentType) {
                        ClipboardContentType.URL -> if (domain.isNotEmpty()) "$domain:" else "Link:"
                        ClipboardContentType.EMAIL -> "Email:"
                        ClipboardContentType.PHONE -> "Phone:"
                        ClipboardContentType.CODE -> "Code:"
                        ClipboardContentType.COLOR -> "Color:"
                        ClipboardContentType.TEXT -> "Paste:"
                    },
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
                    .clickable { onDismissClipboard() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        } else if (shouldShowSuggestions) {
            // TYPING STATE: Tools Button on left + Suggestions filling the rest of the bar

            // Tools Button (Chevron '›' at left corner to reveal tools)
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
                    .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                    .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                    .clickable { showToolsWhileTyping = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textOnAccent
                )
            }

            // Word Suggestion Slots (take full remaining width)
            val displaySuggestions = suggestions.take(3)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 3) {
                    val suggestion = displaySuggestions.getOrNull(i) ?: ""
                    val hasText = suggestion.isNotEmpty()
                    val isPrimary = autocorrectEnabled && hasText && (
                        (displaySuggestions.size >= 2 && i == 1) ||
                        (displaySuggestions.size == 1 && i == 0)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (hasText) {
                                    Modifier
                                        .drawBehind {
                                            drawRoundRect(
                                                color = colors.shadow,
                                                topLeft = Offset(if (isPrimary) 2.dp.toPx() else 1.5.dp.toPx(), if (isPrimary) 2.dp.toPx() else 1.5.dp.toPx()),
                                                size = Size(size.width, size.height),
                                                cornerRadius = CornerRadius(6.dp.toPx())
                                            )
                                        }
                                        .background(
                                            color = if (isPrimary) colors.accentKeyBackground else colors.keyBackground,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isPrimary) dims.borderWidth else 1.5.dp,
                                            color = if (isPrimary) colors.border else colors.border.copy(alpha = 0.6f),
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
                                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = if (isPrimary) 15.sp else 13.5.sp
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
        } else {
            // TOOLS STATE: Tools take full space across the toolbar
            // (either idle, or user tapped tools button while typing)

            if (showToolsWhileTyping) {
                // Back/Collapse button '‹' to return to suggestions
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
                        .clickable { showToolsWhileTyping = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            // Tools taking the remaining/full space
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Settings Tool
                ToolButton(
                    icon = "⚙️",
                    onClick = onSettingsTap,
                    modifier = Modifier.weight(1f)
                )

                // 2. Clipboard Tool
                ToolButton(
                    icon = "📋",
                    onClick = onClipboardTap,
                    modifier = Modifier.weight(1f)
                )

                // 3. Calculator Tool
                ToolButton(
                    icon = "🧮",
                    onClick = onCalculatorTap,
                    modifier = Modifier.weight(1f)
                )

                // 4. Voice Input Tool
                ToolButton(
                    icon = "🎤",
                    onClick = onVoiceTap,
                    modifier = Modifier.weight(1f)
                )

                // 5. Emoji Tool
                if (onEmojiTap != null) {
                    ToolButton(
                        icon = "😊",
                        onClick = onEmojiTap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxHeight()
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
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
    }
}
