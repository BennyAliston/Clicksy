package com.clicksy.keyboard.data

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.clicksy.keyboard.ui.theme.ClicksyColorScheme
import com.clicksy.keyboard.ui.theme.SunshineColors
import kotlin.random.Random

/**
 * Encapsulates the user's fully custom color palette for the keyboard.
 */
data class CustomThemeConfig(
    val background: Color = Color(0xFFFFF5E1),
    val keyBackground: Color = Color(0xFFFFFFFF),
    val accentKeyBackground: Color = Color(0xFFFFE156),
    val actionKeyBackground: Color = Color(0xFFFF6B6B),
    val border: Color = Color(0xFF1A1A2E),
    val shadow: Color = Color(0xFF1A1A2E),
    val textPrimary: Color = Color(0xFF1A1A2E),
    val textSecondary: Color = Color(0xFF555555),
    val textOnAction: Color = Color(0xFFFFFFFF),
    val suggestionBarBackground: Color = Color(0xFFFFF8EC)
) {
    /**
     * Converts this custom theme config into a full [ClicksyColorScheme].
     */
    fun toColorScheme(): ClicksyColorScheme = ClicksyColorScheme(
        background = background,
        keyBackground = keyBackground,
        accentKeyBackground = accentKeyBackground,
        actionKeyBackground = actionKeyBackground,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textOnAction = textOnAction,
        border = border,
        shadow = shadow,
        suggestionBarBackground = suggestionBarBackground,
        suggestionText = textPrimary,
        popupBackground = keyBackground,
        divider = border,
        pressedOverlay = Color(0x18000000)
    )

    companion object {
        const val KEY_CUSTOM_BG = "custom_color_bg"
        const val KEY_CUSTOM_KEY_BG = "custom_color_key_bg"
        const val KEY_CUSTOM_ACCENT_BG = "custom_color_accent_bg"
        const val KEY_CUSTOM_ACTION_BG = "custom_color_action_bg"
        const val KEY_CUSTOM_BORDER = "custom_color_border"
        const val KEY_CUSTOM_SHADOW = "custom_color_shadow"
        const val KEY_CUSTOM_TEXT_PRIMARY = "custom_color_text_primary"
        const val KEY_CUSTOM_TEXT_SECONDARY = "custom_color_text_secondary"
        const val KEY_CUSTOM_TEXT_ON_ACTION = "custom_color_text_on_action"
        const val KEY_CUSTOM_SUGGESTION_BG = "custom_color_suggestion_bg"

        /**
         * Clones colors directly from an existing preset scheme.
         */
        fun fromPreset(scheme: ClicksyColorScheme): CustomThemeConfig = CustomThemeConfig(
            background = scheme.background,
            keyBackground = scheme.keyBackground,
            accentKeyBackground = scheme.accentKeyBackground,
            actionKeyBackground = scheme.actionKeyBackground,
            border = scheme.border,
            shadow = scheme.shadow,
            textPrimary = scheme.textPrimary,
            textSecondary = scheme.textSecondary,
            textOnAction = scheme.textOnAction,
            suggestionBarBackground = scheme.suggestionBarBackground
        )

        /**
         * Loads custom theme configuration from [SharedPreferences].
         */
        fun loadFromPrefs(prefs: SharedPreferences): CustomThemeConfig {
            val default = SunshineColors
            return CustomThemeConfig(
                background = Color(prefs.getInt(KEY_CUSTOM_BG, default.background.toArgb())),
                keyBackground = Color(prefs.getInt(KEY_CUSTOM_KEY_BG, default.keyBackground.toArgb())),
                accentKeyBackground = Color(prefs.getInt(KEY_CUSTOM_ACCENT_BG, default.accentKeyBackground.toArgb())),
                actionKeyBackground = Color(prefs.getInt(KEY_CUSTOM_ACTION_BG, default.actionKeyBackground.toArgb())),
                border = Color(prefs.getInt(KEY_CUSTOM_BORDER, default.border.toArgb())),
                shadow = Color(prefs.getInt(KEY_CUSTOM_SHADOW, default.shadow.toArgb())),
                textPrimary = Color(prefs.getInt(KEY_CUSTOM_TEXT_PRIMARY, default.textPrimary.toArgb())),
                textSecondary = Color(prefs.getInt(KEY_CUSTOM_TEXT_SECONDARY, default.textSecondary.toArgb())),
                textOnAction = Color(prefs.getInt(KEY_CUSTOM_TEXT_ON_ACTION, default.textOnAction.toArgb())),
                suggestionBarBackground = Color(prefs.getInt(KEY_CUSTOM_SUGGESTION_BG, default.suggestionBarBackground.toArgb()))
            )
        }

        /**
         * Saves custom theme configuration to [SharedPreferences].
         */
        fun saveToPrefs(prefs: SharedPreferences, config: CustomThemeConfig) {
            prefs.edit()
                .putInt(KEY_CUSTOM_BG, config.background.toArgb())
                .putInt(KEY_CUSTOM_KEY_BG, config.keyBackground.toArgb())
                .putInt(KEY_CUSTOM_ACCENT_BG, config.accentKeyBackground.toArgb())
                .putInt(KEY_CUSTOM_ACTION_BG, config.actionKeyBackground.toArgb())
                .putInt(KEY_CUSTOM_BORDER, config.border.toArgb())
                .putInt(KEY_CUSTOM_SHADOW, config.shadow.toArgb())
                .putInt(KEY_CUSTOM_TEXT_PRIMARY, config.textPrimary.toArgb())
                .putInt(KEY_CUSTOM_TEXT_SECONDARY, config.textSecondary.toArgb())
                .putInt(KEY_CUSTOM_TEXT_ON_ACTION, config.textOnAction.toArgb())
                .putInt(KEY_CUSTOM_SUGGESTION_BG, config.suggestionBarBackground.toArgb())
                .apply()
        }

        /**
         * Generates a fun, harmonious randomized Neubrutalist color scheme.
         */
        fun generateRandom(): CustomThemeConfig {
            val lightBgs = listOf(
                Color(0xFFFFF5E1), Color(0xFFF0E6FF), Color(0xFFE8FFF0), Color(0xFFFFF0EB),
                Color(0xFFE8F4FD), Color(0xFFFFF9DB), Color(0xFFF3E8FF), Color(0xFFE6FFFA)
            )
            val vibrantAccents = listOf(
                Color(0xFFFFE156), Color(0xFFFF69B4), Color(0xFF00D4AA), Color(0xFFFF6B6B),
                Color(0xFF4D96FF), Color(0xFFFF9F1C), Color(0xFF7DFFB3), Color(0xFFFFD93D),
                Color(0xFFB388FF), Color(0xFFFF5252), Color(0xFF00F0FF), Color(0xFFFF70A6)
            )
            val vibrantActions = listOf(
                Color(0xFFFF6B6B), Color(0xFF7DFFB3), Color(0xFFFFD93D), Color(0xFF4D96FF),
                Color(0xFFFF5757), Color(0xFF00E676), Color(0xFFFF4081), Color(0xFFFFAB00),
                Color(0xFF651FFF), Color(0xFF00B0FF)
            )

            val bg = lightBgs[Random.nextInt(lightBgs.size)]
            val accent = vibrantAccents[Random.nextInt(vibrantAccents.size)]
            var action = vibrantActions[Random.nextInt(vibrantActions.size)]
            while (action == accent) {
                action = vibrantActions[Random.nextInt(vibrantActions.size)]
            }

            return CustomThemeConfig(
                background = bg,
                keyBackground = Color.White,
                accentKeyBackground = accent,
                actionKeyBackground = action,
                border = Color(0xFF1A1A2E),
                shadow = Color(0xFF1A1A2E),
                textPrimary = Color(0xFF1A1A2E),
                textSecondary = Color(0xFF555555),
                textOnAction = Color.White,
                suggestionBarBackground = bg
            )
        }
    }
}
