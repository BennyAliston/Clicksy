package com.clicksy.keyboard.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography for neubrutalism keyboard.
 * Bold display type for keys, calm readable type for suggestions.
 */
object ClicksyTypography {

    /** Primary key label — bold and impactful */
    val keyLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    )

    /** Small key label — for function keys like ?123, Shift text */
    val keyLabelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.sp
    )

    /** Suggestion bar text */
    val suggestionText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.sp
    )

    /** Emoji display in grid */
    val emojiLabel = TextStyle(
        fontSize = 26.sp,
        letterSpacing = 0.sp
    )

    /** Key popup (magnified preview) */
    val popupLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    )

    /** Clipboard item text */
    val clipboardText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp
    )

    /** Settings / header text */
    val headerText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    )

    /** Monospace label for meta information */
    val monoLabel = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
}
