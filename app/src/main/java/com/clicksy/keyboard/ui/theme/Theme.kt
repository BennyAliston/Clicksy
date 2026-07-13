package com.clicksy.keyboard.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clicksy.keyboard.data.NeuTheme
import kotlin.math.exp

/**
 * Neubrutalism-specific dimension tokens.
 */
data class ClicksyDimensions(
    val borderWidth: Dp = 3.dp,
    val shadowOffsetX: Dp = 4.dp,
    val shadowOffsetY: Dp = 4.dp,
    val keyCornerRadius: Dp = 8.dp,
    val keyHeight: Dp = 50.dp,
    val keySpacing: Dp = 4.dp,
    val suggestionBarHeight: Dp = 40.dp,
    val keyboardPadding: Dp = 4.dp
)

val LocalClicksyColors = staticCompositionLocalOf { SunshineColors }
val LocalClicksyDimensions = staticCompositionLocalOf { ClicksyDimensions() }

/**
 * Central accessor for neubrutalism design tokens.
 */
object ClicksyTheme {
    val colors: ClicksyColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalClicksyColors.current

    val dimensions: ClicksyDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalClicksyDimensions.current
}

/**
 * Helper to dynamically extract the dominant brand color of the target application.
 * Uses hardcoded values for common applications, and extracts & averages launcher icon pixels as fallback.
 */
fun getAppDominantColor(context: Context, packageName: String): Color {
    // Highly optimized overrides for common popular applications
    when (packageName) {
        "com.whatsapp" -> return Color(0xFF25D366)
        "com.google.android.youtube" -> return Color(0xFFFF0000)
        "com.facebook.katana", "com.facebook.lite", "com.facebook.orca" -> return Color(0xFF1877F2)
        "com.instagram.android" -> return Color(0xFFE1306C)
        "com.spotify.music" -> return Color(0xFF1DB954)
        "com.slack" -> return Color(0xFF4A154B)
        "com.reddit.frontpage" -> return Color(0xFFFF4500)
        "com.twitter.android", "com.x.android" -> return Color(0xFF1DA1F2)
        "com.clicksy.keyboard" -> return Color(0xFFFFE156)
    }

    try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val drawable = pm.getApplicationIcon(appInfo)

        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        // Scale down to 1x1 to average the pixels
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val colorInt = scaledBitmap.getPixel(0, 0)

        bitmap.recycle()
        scaledBitmap.recycle()

        // Validate saturation to make sure we don't end up with gray/dull elements
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, hsl)
        if (hsl[1] < 0.15f || hsl[2] < 0.15f || hsl[2] > 0.95f) {
            return Color(0xFFFFE156) // Fallback to Sunshine yellow
        }
        return Color(colorInt)
    } catch (e: Exception) {
        return Color(0xFFFFE156)
    }
}

/**
 * Extension helper to convert a brand color to a beautiful, soft neubrutalist pastel background color.
 */
fun Color.toPastel(isDark: Boolean): Color {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsl)
    if (isDark) {
        hsl[1] = 0.20f // low saturation
        hsl[2] = 0.12f // dark value
    } else {
        hsl[1] = 0.12f // very soft saturation
        hsl[2] = 0.97f // clean light background value
    }
    return Color(android.graphics.Color.HSVToColor(hsl))
}

/**
 * Construct a dynamic, app-adaptive neubrutalist color scheme.
 * The keyboard adapts dynamically to the target application icon's colors.
 */
@Composable
fun getAdaptiveColorScheme(packageName: String, isDark: Boolean): ClicksyColorScheme {
    val context = LocalContext.current
    
    val brandColor = remember(packageName) {
        getAppDominantColor(context, packageName)
    }
    
    return AdaptiveDarkBase.copy(
        accentKeyBackground = brandColor
    )
}

/**
 * Maps [NeuTheme] enum to the corresponding color scheme, resolving target app package color if adaptive.
 */
@Composable
fun NeuTheme.toColorScheme(
    activePackageName: String = "com.clicksy.keyboard",
    isDark: Boolean = isSystemInDarkTheme()
): ClicksyColorScheme = when (this) {
    NeuTheme.SUNSHINE -> SunshineColors
    NeuTheme.BUBBLEGUM -> BubblegumColors
    NeuTheme.MINTY -> MintyColors
    NeuTheme.ADAPTIVE -> getAdaptiveColorScheme(activePackageName, isDark)
}

/**
 * Root theme composable wrapping all keyboard UI.
 */
@Composable
fun ClicksyTheme(
    colorScheme: ClicksyColorScheme = SunshineColors,
    dimensions: ClicksyDimensions = ClicksyDimensions(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalClicksyColors provides colorScheme,
        LocalClicksyDimensions provides dimensions
    ) {
        MaterialTheme {
            content()
        }
    }
}
