package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography

/**
 * Suggestion bar displayed above the keyboard.
 * Shows 3 word predictions separated by dividers or a recent paste suggestion.
 * Left: clipboard button, Right: voice/emoji toggle.
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
            .border(
                width = dims.borderWidth,
                color = colors.border
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (recentClipboardText != null && onPasteClipboard != null && onDismissClipboard != null) {
            // Clipboard suggestion pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onPasteClipboard() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Paste: ",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = recentClipboardText,
                    style = ClicksyTypography.suggestionText,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(dims.borderWidth)
                    .height(dims.suggestionBarHeight - 12.dp)
                    .background(colors.divider)
            )

            // Dismiss Button
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable { onDismissClipboard() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    style = ClicksyTypography.keyLabelSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Clipboard button
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(dims.suggestionBarHeight)
                    .clickable { onClipboardTap() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📋",
                    style = ClicksyTypography.keyLabelSmall
                )
            }

        // Divider
        Box(
            modifier = Modifier
                .width(dims.borderWidth)
                .height(dims.suggestionBarHeight - 12.dp)
                .background(colors.divider)
        )

        // Suggestion slots (3 equal-width)
        val displaySuggestions = suggestions.take(3)
        val totalWeight = 3f

        for (i in 0 until 3) {
            val suggestion = displaySuggestions.getOrNull(i) ?: ""
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dims.suggestionBarHeight)
                    .clickable(enabled = suggestion.isNotEmpty()) {
                        if (suggestion.isNotEmpty()) onSuggestionTap(suggestion)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion,
                    style = ClicksyTypography.suggestionText,
                    color = colors.suggestionText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Divider between suggestions
            if (i < 2) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(dims.suggestionBarHeight - 16.dp)
                        .background(colors.divider.copy(alpha = 0.3f))
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(dims.borderWidth)
                .height(dims.suggestionBarHeight - 12.dp)
                .background(colors.divider)
        )

        // Voice input button
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(dims.suggestionBarHeight)
                .clickable { onVoiceTap() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎤",
                style = ClicksyTypography.keyLabelSmall
            )
        }
      }
    }
}
