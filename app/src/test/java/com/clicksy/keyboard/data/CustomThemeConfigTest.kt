package com.clicksy.keyboard.data

import androidx.compose.ui.graphics.Color
import com.clicksy.keyboard.ui.theme.CyberpunkColors
import com.clicksy.keyboard.ui.theme.SunshineColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CustomThemeConfigTest {

    @Test
    fun testFromPresetCopiesColorsCorrectly() {
        val config = CustomThemeConfig.fromPreset(CyberpunkColors)
        assertEquals(CyberpunkColors.background, config.background)
        assertEquals(CyberpunkColors.keyBackground, config.keyBackground)
        assertEquals(CyberpunkColors.accentKeyBackground, config.accentKeyBackground)
        assertEquals(CyberpunkColors.actionKeyBackground, config.actionKeyBackground)
        assertEquals(CyberpunkColors.border, config.border)
        assertEquals(CyberpunkColors.shadow, config.shadow)
        assertEquals(CyberpunkColors.textPrimary, config.textPrimary)
        assertEquals(CyberpunkColors.textOnAction, config.textOnAction)
    }

    @Test
    fun testToColorSchemeGeneratesScheme() {
        val config = CustomThemeConfig(
            background = Color(0xFF112233),
            keyBackground = Color(0xFF445566),
            accentKeyBackground = Color(0xFF778899),
            actionKeyBackground = Color(0xFFAABBCC),
            border = Color(0xFF000000),
            shadow = Color(0xFF000000),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFCCCCCC),
            textOnAction = Color(0xFFFFFFFF),
            suggestionBarBackground = Color(0xFF112233)
        )

        val scheme = config.toColorScheme()
        assertEquals(Color(0xFF112233), scheme.background)
        assertEquals(Color(0xFF445566), scheme.keyBackground)
        assertEquals(Color(0xFF778899), scheme.accentKeyBackground)
        assertEquals(Color(0xFFAABBCC), scheme.actionKeyBackground)
        assertEquals(Color(0xFF000000), scheme.border)
        assertEquals(Color(0xFF000000), scheme.shadow)
        assertEquals(Color(0xFFFFFFFF), scheme.textPrimary)
        assertEquals(Color(0xFFFFFFFF), scheme.textOnAction)
    }

    @Test
    fun testRandomThemeGenerationProducesValidColors() {
        val randomConfig = CustomThemeConfig.generateRandom()
        assertNotNull(randomConfig.background)
        assertNotNull(randomConfig.accentKeyBackground)
        assertNotNull(randomConfig.actionKeyBackground)
        val scheme = randomConfig.toColorScheme()
        assertNotNull(scheme)
    }
}
