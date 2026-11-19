package com.clicksy.keyboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Neubrutalism color scheme — each theme defines categorical,
 * high-saturation colors with solid black borders and hard shadows.
 */
data class ClicksyColorScheme(
    val background: Color,
    val keyBackground: Color,
    val accentKeyBackground: Color,
    val actionKeyBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAction: Color,
    val border: Color,
    val shadow: Color,
    val suggestionBarBackground: Color,
    val suggestionText: Color,
    val popupBackground: Color,
    val divider: Color,
    val pressedOverlay: Color
)

/**
 * Dynamically computes a high-contrast text color (dark or light) for use on top of accentKeyBackground.
 */
val ClicksyColorScheme.textOnAccent: Color
    get() {
        val r = accentKeyBackground.red
        val g = accentKeyBackground.green
        val b = accentKeyBackground.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return if (luminance > 0.5f) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    }

// ☀️ Sunshine — warm cream with electric yellow & coral
val SunshineColors = ClicksyColorScheme(
    background = Color(0xFFFFF5E1),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFFFE156),
    actionKeyBackground = Color(0xFFFF6B6B),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF555555),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF1A1A2E),
    shadow = Color(0xFF1A1A2E),
    suggestionBarBackground = Color(0xFFFFF8EC),
    suggestionText = Color(0xFF1A1A2E),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF1A1A2E),
    pressedOverlay = Color(0x15000000)
)

// 🍬 Bubblegum — lavender with hot pink & mint green
val BubblegumColors = ClicksyColorScheme(
    background = Color(0xFFF0E6FF),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFFF69B4),
    actionKeyBackground = Color(0xFF7DFFB3),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF555555),
    textOnAction = Color(0xFF1A1A2E),
    border = Color(0xFF1A1A2E),
    shadow = Color(0xFF1A1A2E),
    suggestionBarBackground = Color(0xFFF5EDFF),
    suggestionText = Color(0xFF1A1A2E),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF1A1A2E),
    pressedOverlay = Color(0x15000000)
)

// 🌿 Minty — pale mint with teal & sunny yellow
val MintyColors = ClicksyColorScheme(
    background = Color(0xFFE8FFF0),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFF00D4AA),
    actionKeyBackground = Color(0xFFFFD93D),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF555555),
    textOnAction = Color(0xFF1A1A2E),
    border = Color(0xFF1A1A2E),
    shadow = Color(0xFF1A1A2E),
    suggestionBarBackground = Color(0xFFEDFFF5),
    suggestionText = Color(0xFF1A1A2E),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF1A1A2E),
    pressedOverlay = Color(0x15000000)
)

// 🌑 Midnight — deep dark blue/gray with vibrant accents
val MidnightColors = ClicksyColorScheme(
    background = Color(0xFF1E1E2E),
    keyBackground = Color(0xFF2D2D3D),
    accentKeyBackground = Color(0xFFFFD93D),
    actionKeyBackground = Color(0xFFF38BA8),
    textPrimary = Color(0xFFCDD6F4),
    textSecondary = Color(0xFFBAC2DE),
    textOnAction = Color(0xFF11111B),
    border = Color(0xFFCDD6F4),
    shadow = Color(0xFF11111B),
    suggestionBarBackground = Color(0xFF181825),
    suggestionText = Color(0xFFCDD6F4),
    popupBackground = Color(0xFF2D2D3D),
    divider = Color(0xFFCDD6F4),
    pressedOverlay = Color(0x20FFFFFF)
)

// ⚡ Cyberpunk — deep black with neon cyan and hot magenta
val CyberpunkColors = ClicksyColorScheme(
    background = Color(0xFF0C0C14),
    keyBackground = Color(0xFF161624),
    accentKeyBackground = Color(0xFF00F0FF),
    actionKeyBackground = Color(0xFFFF0055),
    textPrimary = Color(0xFFE2E8F0),
    textSecondary = Color(0xFF94A3B8),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF00F0FF),
    shadow = Color(0xFFFF0055),
    suggestionBarBackground = Color(0xFF08080E),
    suggestionText = Color(0xFF00F0FF),
    popupBackground = Color(0xFF161624),
    divider = Color(0xFF00F0FF),
    pressedOverlay = Color(0x2500F0FF)
)

// 🌸 Lavender Dream — soft lilac with royal violet & pastel blossom
val LavenderColors = ClicksyColorScheme(
    background = Color(0xFFF5EFFF),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFC084FC),
    actionKeyBackground = Color(0xFF818CF8),
    textPrimary = Color(0xFF2E1065),
    textSecondary = Color(0xFF6B21A8),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF2E1065),
    shadow = Color(0xFF2E1065),
    suggestionBarBackground = Color(0xFFFAF5FF),
    suggestionText = Color(0xFF2E1065),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF2E1065),
    pressedOverlay = Color(0x152E1065)
)

// 🌲 Forest Moss — earthy moss with sage green & deep pine
val ForestColors = ClicksyColorScheme(
    background = Color(0xFFEBF3E8),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFF84A98C),
    actionKeyBackground = Color(0xFF52796F),
    textPrimary = Color(0xFF1D2A20),
    textSecondary = Color(0xFF52796F),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF1D2A20),
    shadow = Color(0xFF1D2A20),
    suggestionBarBackground = Color(0xFFF3F7F1),
    suggestionText = Color(0xFF1D2A20),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF1D2A20),
    pressedOverlay = Color(0x151D2A20)
)

// 📟 Retro Terminal — CRT phosphor green, amber & deep carbon
val RetroTerminalColors = ClicksyColorScheme(
    background = Color(0xFF101912),
    keyBackground = Color(0xFF19251C),
    accentKeyBackground = Color(0xFF33FF33),
    actionKeyBackground = Color(0xFFFFB000),
    textPrimary = Color(0xFF33FF33),
    textSecondary = Color(0xFF22AA22),
    textOnAction = Color(0xFF101912),
    border = Color(0xFF33FF33),
    shadow = Color(0xFF080E09),
    suggestionBarBackground = Color(0xFF0C130D),
    suggestionText = Color(0xFF33FF33),
    popupBackground = Color(0xFF19251C),
    divider = Color(0xFF33FF33),
    pressedOverlay = Color(0x2033FF33)
)

// 🍑 Sunset Glow — twilight indigo with peach & radiant coral
val SunsetColors = ClicksyColorScheme(
    background = Color(0xFFFFF2EB),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFFF9E79),
    actionKeyBackground = Color(0xFFFF5376),
    textPrimary = Color(0xFF2B1020),
    textSecondary = Color(0xFF884D67),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF2B1020),
    shadow = Color(0xFF2B1020),
    suggestionBarBackground = Color(0xFFFFF8F5),
    suggestionText = Color(0xFF2B1020),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF2B1020),
    pressedOverlay = Color(0x152B1020)
)

// 🖤 Monochrome — high-contrast pure white, graphite & bold black
val MonochromeColors = ClicksyColorScheme(
    background = Color(0xFFF4F4F6),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFE2E2E8),
    actionKeyBackground = Color(0xFF111111),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF666666),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF111111),
    shadow = Color(0xFF111111),
    suggestionBarBackground = Color(0xFFECECEF),
    suggestionText = Color(0xFF111111),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF111111),
    pressedOverlay = Color(0x15000000)
)


// 👤 Adaptive Dark Base — optimized dark neubrutal base for brand coloring
val AdaptiveDarkBase = ClicksyColorScheme(
    background = Color(0xFF16161A),
    keyBackground = Color(0xFF282830),
    accentKeyBackground = Color(0xFFFFE156), // Fallback yellow, replaced dynamically
    actionKeyBackground = Color(0xFF3F3F4C),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0C0),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    suggestionBarBackground = Color(0xFF101014),
    suggestionText = Color(0xFFFFFFFF),
    popupBackground = Color(0xFF282830),
    divider = Color(0xFF000000),
    pressedOverlay = Color(0x25FFFFFF)
)

// 👤 Adaptive Light Base — optimized light neubrutal base for device/brand coloring
val AdaptiveLightBase = ClicksyColorScheme(
    background = Color(0xFFF2F2F6),
    keyBackground = Color(0xFFFFFFFF),
    accentKeyBackground = Color(0xFFFFE156), // Fallback yellow, replaced dynamically
    actionKeyBackground = Color(0xFF1A1A2E),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF6B6B80),
    textOnAction = Color(0xFFFFFFFF),
    border = Color(0xFF1A1A2E),
    shadow = Color(0xFF1A1A2E),
    suggestionBarBackground = Color(0xFFE6E6EC),
    suggestionText = Color(0xFF1A1A2E),
    popupBackground = Color(0xFFFFFFFF),
    divider = Color(0xFF1A1A2E),
    pressedOverlay = Color(0x15000000)
)
