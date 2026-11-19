package com.clicksy.keyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clicksy.keyboard.R
import com.clicksy.keyboard.data.ClipboardManagerService
import com.clicksy.keyboard.data.CustomThemeConfig
import com.clicksy.keyboard.data.DictionaryProvider
import com.clicksy.keyboard.data.KeyShapeStyle
import com.clicksy.keyboard.data.KeyboardSize
import com.clicksy.keyboard.data.NeuTheme
import com.clicksy.keyboard.service.ClicksyService
import com.clicksy.keyboard.ui.keyboard.KeyType
import com.clicksy.keyboard.ui.keyboard.NeuKey
import com.clicksy.keyboard.ui.theme.BubblegumColors
import com.clicksy.keyboard.ui.theme.ClicksyColorScheme
import com.clicksy.keyboard.ui.theme.ClicksyDimensions
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.CyberpunkColors
import com.clicksy.keyboard.ui.theme.ForestColors
import com.clicksy.keyboard.ui.theme.LavenderColors
import com.clicksy.keyboard.ui.theme.MidnightColors
import com.clicksy.keyboard.ui.theme.MintyColors
import com.clicksy.keyboard.ui.theme.MonochromeColors
import com.clicksy.keyboard.ui.theme.RetroTerminalColors
import com.clicksy.keyboard.ui.theme.SunsetColors
import com.clicksy.keyboard.ui.theme.SunshineColors
import com.clicksy.keyboard.ui.theme.toColorScheme
import com.clicksy.keyboard.util.SoundType

/**
 * Settings and full customization studio activity for Clicksy.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember {
                getSharedPreferences(ClicksyService.PREFS_NAME, Context.MODE_PRIVATE)
            }

            var selectedTheme by remember {
                val themeName = prefs.getString(ClicksyService.KEY_THEME, NeuTheme.SUNSHINE.name)
                mutableStateOf(
                    try { NeuTheme.valueOf(themeName ?: NeuTheme.SUNSHINE.name) }
                    catch (e: Exception) { NeuTheme.SUNSHINE }
                )
            }

            var customThemeConfig by remember {
                mutableStateOf(CustomThemeConfig.loadFromPrefs(prefs))
            }

            var selectedSize by remember {
                val sizeName = prefs.getString(ClicksyService.KEY_KEYBOARD_SIZE, KeyboardSize.MEDIUM.name)
                mutableStateOf(
                    try { KeyboardSize.valueOf(sizeName ?: KeyboardSize.MEDIUM.name) }
                    catch (e: Exception) { KeyboardSize.MEDIUM }
                )
            }

            var selectedKeyShapeStyle by remember {
                val shapeName = prefs.getString(ClicksyService.KEY_KEY_SHAPE_STYLE, null)
                mutableStateOf(
                    if (shapeName != null) {
                        try { KeyShapeStyle.valueOf(shapeName) } catch (e: Exception) { KeyShapeStyle.ROUNDED }
                    } else {
                        if (prefs.getBoolean(ClicksyService.KEY_ROUNDED_KEYS, false)) KeyShapeStyle.SOFT else KeyShapeStyle.ROUNDED
                    }
                )
            }

            var selectedKeyCornerRadius by remember {
                mutableFloatStateOf(
                    prefs.getFloat(
                        ClicksyService.KEY_KEY_CORNER_RADIUS,
                        if (selectedKeyShapeStyle == KeyShapeStyle.SOFT) 14f else 8f
                    )
                )
            }

            var selectedBorderWidth by remember {
                mutableFloatStateOf(prefs.getFloat(ClicksyService.KEY_BORDER_WIDTH, 3f))
            }

            var selectedShadowOffset by remember {
                mutableFloatStateOf(prefs.getFloat(ClicksyService.KEY_SHADOW_OFFSET, 4f))
            }

            var selectedKeySpacing by remember {
                mutableFloatStateOf(prefs.getFloat(ClicksyService.KEY_KEY_SPACING, 4f))
            }

            var showSecondarySymbols by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_SHOW_SECONDARY_SYMBOLS, true))
            }

            var hapticEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_HAPTIC, true))
            }

            var autocompleteEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_AUTOCOMPLETE, true))
            }

            var autocorrectEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_AUTOCORRECT, true))
            }

            var autoCapitalizationEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_AUTO_CAPITALIZATION, true))
            }

            var showNumberRow by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_SHOW_NUMBER_ROW, false))
            }

            var hinglishEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_HINGLISH_ENABLED, true))
            }

            var learningEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_LEARNING_ENABLED, true))
            }

            var selectedSoundType by remember {
                val soundName = prefs.getString(ClicksyService.KEY_SOUND_TYPE, SoundType.MUTE.name)
                mutableStateOf(
                    try { SoundType.valueOf(soundName ?: SoundType.MUTE.name) }
                    catch (e: Exception) { SoundType.MUTE }
                )
            }

            var clipboardHistoryEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClipboardManagerService.KEY_CLIPBOARD_HISTORY_ENABLED, true))
            }

            var clipboardAutoClose by remember {
                mutableStateOf(prefs.getBoolean(ClipboardManagerService.KEY_CLIPBOARD_AUTO_CLOSE, true))
            }

            var clipboardIncognito by remember {
                mutableStateOf(prefs.getBoolean(ClipboardManagerService.KEY_CLIPBOARD_INCOGNITO, false))
            }

            val currentScheme = if (selectedTheme == NeuTheme.CUSTOM) {
                customThemeConfig.toColorScheme()
            } else {
                selectedTheme.toColorScheme()
            }

            val currentDimensions = remember(
                selectedSize,
                selectedKeyShapeStyle,
                selectedKeyCornerRadius,
                selectedBorderWidth,
                selectedShadowOffset,
                selectedKeySpacing,
                showSecondarySymbols
            ) {
                val base = ClicksyDimensions(
                    borderWidth = selectedBorderWidth.dp,
                    shadowOffsetX = selectedShadowOffset.dp,
                    shadowOffsetY = selectedShadowOffset.dp,
                    keyCornerRadius = selectedKeyCornerRadius.dp,
                    keySpacing = selectedKeySpacing.dp,
                    keyShapeStyle = selectedKeyShapeStyle,
                    showSecondarySymbols = showSecondarySymbols
                )
                when (selectedSize) {
                    KeyboardSize.SMALL -> base.copy(keyHeight = 42.dp)
                    KeyboardSize.MEDIUM -> base.copy(keyHeight = 50.dp)
                    KeyboardSize.LARGE -> base.copy(keyHeight = 58.dp)
                    KeyboardSize.EXTRA_LARGE -> base.copy(keyHeight = 65.dp)
                }
            }

            ClicksyTheme(
                colorScheme = currentScheme,
                dimensions = currentDimensions
            ) {
                SettingsScreen(
                    selectedTheme = selectedTheme,
                    customThemeConfig = customThemeConfig,
                    selectedSize = selectedSize,
                    selectedKeyShapeStyle = selectedKeyShapeStyle,
                    selectedKeyCornerRadius = selectedKeyCornerRadius,
                    selectedBorderWidth = selectedBorderWidth,
                    selectedShadowOffset = selectedShadowOffset,
                    selectedKeySpacing = selectedKeySpacing,
                    showSecondarySymbols = showSecondarySymbols,
                    selectedSoundType = selectedSoundType,
                    hapticEnabled = hapticEnabled,
                    autocompleteEnabled = autocompleteEnabled,
                    autocorrectEnabled = autocorrectEnabled,
                    autoCapitalizationEnabled = autoCapitalizationEnabled,
                    showNumberRow = showNumberRow,
                    hinglishEnabled = hinglishEnabled,
                    learningEnabled = learningEnabled,
                    clipboardHistoryEnabled = clipboardHistoryEnabled,
                    clipboardAutoClose = clipboardAutoClose,
                    clipboardIncognito = clipboardIncognito,
                    onThemeSelected = { theme ->
                        selectedTheme = theme
                        prefs.edit().putString(ClicksyService.KEY_THEME, theme.name).apply()
                    },
                    onCustomThemeChange = { updated ->
                        customThemeConfig = updated
                        CustomThemeConfig.saveToPrefs(prefs, updated)
                        if (selectedTheme != NeuTheme.CUSTOM) {
                            selectedTheme = NeuTheme.CUSTOM
                            prefs.edit().putString(ClicksyService.KEY_THEME, NeuTheme.CUSTOM.name).apply()
                        }
                    },
                    onSizeSelected = { size ->
                        selectedSize = size
                        prefs.edit().putString(ClicksyService.KEY_KEYBOARD_SIZE, size.name).apply()
                    },
                    onKeyShapeStyleSelected = { shape ->
                        selectedKeyShapeStyle = shape
                        prefs.edit().putString(ClicksyService.KEY_KEY_SHAPE_STYLE, shape.name).apply()
                        if (shape == KeyShapeStyle.SOFT) {
                            selectedKeyCornerRadius = 14f
                            prefs.edit().putFloat(ClicksyService.KEY_KEY_CORNER_RADIUS, 14f).apply()
                        } else if (shape == KeyShapeStyle.SHARP) {
                            selectedKeyCornerRadius = 0f
                            prefs.edit().putFloat(ClicksyService.KEY_KEY_CORNER_RADIUS, 0f).apply()
                        }
                    },
                    onKeyCornerRadiusSelected = { radius ->
                        selectedKeyCornerRadius = radius
                        prefs.edit().putFloat(ClicksyService.KEY_KEY_CORNER_RADIUS, radius).apply()
                    },
                    onBorderWidthSelected = { width ->
                        selectedBorderWidth = width
                        prefs.edit().putFloat(ClicksyService.KEY_BORDER_WIDTH, width).apply()
                    },
                    onShadowOffsetSelected = { offset ->
                        selectedShadowOffset = offset
                        prefs.edit().putFloat(ClicksyService.KEY_SHADOW_OFFSET, offset).apply()
                    },
                    onKeySpacingSelected = { spacing ->
                        selectedKeySpacing = spacing
                        prefs.edit().putFloat(ClicksyService.KEY_KEY_SPACING, spacing).apply()
                    },
                    onShowSecondarySymbolsToggle = { show ->
                        showSecondarySymbols = show
                        prefs.edit().putBoolean(ClicksyService.KEY_SHOW_SECONDARY_SYMBOLS, show).apply()
                    },
                    onSoundTypeSelected = { soundType ->
                        selectedSoundType = soundType
                        prefs.edit().putString(ClicksyService.KEY_SOUND_TYPE, soundType.name).apply()
                    },
                    onHapticToggle = { enabled ->
                        hapticEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_HAPTIC, enabled).apply()
                    },
                    onAutocompleteToggle = { enabled ->
                        autocompleteEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_AUTOCOMPLETE, enabled).apply()
                    },
                    onAutocorrectToggle = { enabled ->
                        autocorrectEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_AUTOCORRECT, enabled).apply()
                    },
                    onAutoCapitalizationToggle = { enabled ->
                        autoCapitalizationEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_AUTO_CAPITALIZATION, enabled).apply()
                    },
                    onShowNumberRowToggle = { enabled ->
                        showNumberRow = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_SHOW_NUMBER_ROW, enabled).apply()
                    },
                    onHinglishToggle = { enabled ->
                        hinglishEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_HINGLISH_ENABLED, enabled).apply()
                        DictionaryProvider.isHinglishEnabled = enabled
                    },
                    onLearningToggle = { enabled ->
                        learningEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_LEARNING_ENABLED, enabled).apply()
                        DictionaryProvider.isLearningEnabled = enabled
                    },
                    onClearLearnedVocabulary = {
                        DictionaryProvider.clearLearnedVocabulary(this@SettingsActivity)
                    },
                    onClipboardHistoryToggle = { enabled ->
                        clipboardHistoryEnabled = enabled
                        prefs.edit().putBoolean(ClipboardManagerService.KEY_CLIPBOARD_HISTORY_ENABLED, enabled).apply()
                    },
                    onClipboardAutoCloseToggle = { enabled ->
                        clipboardAutoClose = enabled
                        prefs.edit().putBoolean(ClipboardManagerService.KEY_CLIPBOARD_AUTO_CLOSE, enabled).apply()
                    },
                    onClipboardIncognitoToggle = { enabled ->
                        clipboardIncognito = enabled
                        prefs.edit().putBoolean(ClipboardManagerService.KEY_CLIPBOARD_INCOGNITO, enabled).apply()
                    },
                    onEnableKeyboard = {
                        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onSelectKeyboard = {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    selectedTheme: NeuTheme,
    customThemeConfig: CustomThemeConfig,
    selectedSize: KeyboardSize,
    selectedKeyShapeStyle: KeyShapeStyle,
    selectedKeyCornerRadius: Float,
    selectedBorderWidth: Float,
    selectedShadowOffset: Float,
    selectedKeySpacing: Float,
    showSecondarySymbols: Boolean,
    selectedSoundType: SoundType,
    hapticEnabled: Boolean,
    autocompleteEnabled: Boolean,
    autocorrectEnabled: Boolean,
    autoCapitalizationEnabled: Boolean,
    showNumberRow: Boolean,
    hinglishEnabled: Boolean,
    learningEnabled: Boolean,
    clipboardHistoryEnabled: Boolean,
    clipboardAutoClose: Boolean,
    clipboardIncognito: Boolean,
    onThemeSelected: (NeuTheme) -> Unit,
    onCustomThemeChange: (CustomThemeConfig) -> Unit,
    onSizeSelected: (KeyboardSize) -> Unit,
    onKeyShapeStyleSelected: (KeyShapeStyle) -> Unit,
    onKeyCornerRadiusSelected: (Float) -> Unit,
    onBorderWidthSelected: (Float) -> Unit,
    onShadowOffsetSelected: (Float) -> Unit,
    onKeySpacingSelected: (Float) -> Unit,
    onShowSecondarySymbolsToggle: (Boolean) -> Unit,
    onSoundTypeSelected: (SoundType) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onAutocompleteToggle: (Boolean) -> Unit,
    onAutocorrectToggle: (Boolean) -> Unit,
    onAutoCapitalizationToggle: (Boolean) -> Unit,
    onShowNumberRowToggle: (Boolean) -> Unit,
    onHinglishToggle: (Boolean) -> Unit,
    onLearningToggle: (Boolean) -> Unit,
    onClearLearnedVocabulary: () -> Unit,
    onClipboardHistoryToggle: (Boolean) -> Unit,
    onClipboardAutoCloseToggle: (Boolean) -> Unit,
    onClipboardIncognitoToggle: (Boolean) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    var currentDestination by remember { mutableStateOf(SettingsNavDestination.MAIN) }
    var editingColorTarget by remember { mutableStateOf<Pair<String, (Color) -> Unit>?>(null) }
    var editingColorInitial by remember { mutableStateOf(Color.White) }
    var showCopyPresetDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentDestination != SettingsNavDestination.MAIN) {
        currentDestination = SettingsNavDestination.MAIN
    }

    when (currentDestination) {
        SettingsNavDestination.MAIN -> {
            MainDashboardScreen(
                selectedTheme = selectedTheme,
                selectedSoundType = selectedSoundType,
                showNumberRow = showNumberRow,
                hinglishEnabled = hinglishEnabled,
                learningEnabled = learningEnabled,
                clipboardHistoryEnabled = clipboardHistoryEnabled,
                onNavigate = { currentDestination = it },
                onEnableKeyboard = onEnableKeyboard,
                onSelectKeyboard = onSelectKeyboard
            )
        }
        SettingsNavDestination.THEMES_AND_STYLING -> {
            ThemesAndStylingScreen(
                selectedTheme = selectedTheme,
                customThemeConfig = customThemeConfig,
                selectedSize = selectedSize,
                selectedKeyShapeStyle = selectedKeyShapeStyle,
                selectedKeyCornerRadius = selectedKeyCornerRadius,
                selectedBorderWidth = selectedBorderWidth,
                selectedShadowOffset = selectedShadowOffset,
                selectedKeySpacing = selectedKeySpacing,
                showSecondarySymbols = showSecondarySymbols,
                onThemeSelected = onThemeSelected,
                onCustomThemeChange = onCustomThemeChange,
                onSizeSelected = onSizeSelected,
                onKeyShapeStyleSelected = onKeyShapeStyleSelected,
                onKeyCornerRadiusSelected = onKeyCornerRadiusSelected,
                onBorderWidthSelected = onBorderWidthSelected,
                onShadowOffsetSelected = onShadowOffsetSelected,
                onKeySpacingSelected = onKeySpacingSelected,
                onShowSecondarySymbolsToggle = onShowSecondarySymbolsToggle,
                onEditColor = { title, initial, updater ->
                    editingColorTarget = title to updater
                    editingColorInitial = initial
                },
                onCopyPresetClicked = { showCopyPresetDialog = true },
                onBack = { currentDestination = SettingsNavDestination.MAIN }
            )
        }
        SettingsNavDestination.TYPING -> {
            TypingPreferencesScreen(
                autocompleteEnabled = autocompleteEnabled,
                autocorrectEnabled = autocorrectEnabled,
                autoCapitalizationEnabled = autoCapitalizationEnabled,
                showNumberRow = showNumberRow,
                hinglishEnabled = hinglishEnabled,
                learningEnabled = learningEnabled,
                showSecondarySymbols = showSecondarySymbols,
                selectedSize = selectedSize,
                onAutocompleteToggle = onAutocompleteToggle,
                onAutocorrectToggle = onAutocorrectToggle,
                onAutoCapitalizationToggle = onAutoCapitalizationToggle,
                onShowNumberRowToggle = onShowNumberRowToggle,
                onHinglishToggle = onHinglishToggle,
                onLearningToggle = onLearningToggle,
                onClearLearnedVocabulary = onClearLearnedVocabulary,
                onShowSecondarySymbolsToggle = onShowSecondarySymbolsToggle,
                onSizeSelected = onSizeSelected,
                onBack = { currentDestination = SettingsNavDestination.MAIN }
            )
        }
        SettingsNavDestination.SOUND_AND_HAPTICS -> {
            SoundAndHapticsScreen(
                selectedSoundType = selectedSoundType,
                hapticEnabled = hapticEnabled,
                onSoundTypeSelected = onSoundTypeSelected,
                onHapticToggle = onHapticToggle,
                onBack = { currentDestination = SettingsNavDestination.MAIN }
            )
        }
        SettingsNavDestination.CLIPBOARD -> {
            ClipboardSettingsScreen(
                clipboardHistoryEnabled = clipboardHistoryEnabled,
                clipboardAutoClose = clipboardAutoClose,
                clipboardIncognito = clipboardIncognito,
                onClipboardHistoryToggle = onClipboardHistoryToggle,
                onClipboardAutoCloseToggle = onClipboardAutoCloseToggle,
                onClipboardIncognitoToggle = onClipboardIncognitoToggle,
                onBack = { currentDestination = SettingsNavDestination.MAIN }
            )
        }
        SettingsNavDestination.ABOUT -> {
            AboutSettingsScreen(
                onBack = { currentDestination = SettingsNavDestination.MAIN }
            )
        }
    }





    // Color Picker Dialog
    editingColorTarget?.let { (title, updater) ->
        ColorPickerDialog(
            title = title,
            initialColor = editingColorInitial,
            onColorSelected = { newColor ->
                updater(newColor)
                editingColorTarget = null
            },
            onDismiss = { editingColorTarget = null }
        )
    }

    // Copy Preset Dialog
    if (showCopyPresetDialog) {
        CopyPresetDialog(
            onPresetChosen = { scheme ->
                onCustomThemeChange(CustomThemeConfig.fromPreset(scheme))
                showCopyPresetDialog = false
            },
            onDismiss = { showCopyPresetDialog = false }
        )
    }
}

private enum class SettingsNavDestination {
    MAIN,
    THEMES_AND_STYLING,
    TYPING,
    SOUND_AND_HAPTICS,
    CLIPBOARD,
    ABOUT
}

private enum class CustomizationSubTab {
    PRESETS,
    CUSTOM_PALETTE,
    KEY_GEOMETRY
}

@Composable
private fun SettingsSubScreenTopBar(
    title: String,
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
                .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(6.dp))
                .clickable { onBack() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "← Back",
                style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = colors.textPrimary
            )
        }
        Text(
            text = title,
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = colors.textPrimary
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: String,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .background(colors.keyBackground, shape)
            .border(2.5.dp, colors.border, shape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.accentKeyBackground, RoundedCornerShape(8.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = ClicksyTypography.suggestionText.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .background(colors.actionKeyBackground, RoundedCornerShape(4.dp))
                                .border(1.5.dp, colors.border, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = ClicksyTypography.monoLabel.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textOnAction
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = ClicksyTypography.clipboardText.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(colors.background, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "➔",
                    style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun MainDashboardScreen(
    selectedTheme: NeuTheme,
    selectedSoundType: SoundType,
    showNumberRow: Boolean,
    hinglishEnabled: Boolean,
    learningEnabled: Boolean,
    clipboardHistoryEnabled: Boolean,
    onNavigate: (SettingsNavDestination) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = ClicksyTypography.headerText.copy(fontSize = 36.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = "Neubrutalism Keyboard",
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v0.3.0-beta",
                    style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Activation / Setup Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .background(colors.keyBackground, RoundedCornerShape(10.dp))
                .border(3.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "⚡ Keyboard Activation",
                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enable Clicksy in system settings and select it as your current input method.",
                style = ClicksyTypography.clipboardText.copy(fontSize = 12.sp),
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                        .background(colors.actionKeyBackground, RoundedCornerShape(8.dp))
                        .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable { onEnableKeyboard() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "① " + stringResource(R.string.enable_keyboard),
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = colors.textOnAction
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                        .background(colors.accentKeyBackground, RoundedCornerShape(8.dp))
                        .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable { onSelectKeyboard() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "② " + stringResource(R.string.select_keyboard),
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TestKeyboardCard()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Categories",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsCategoryCard(
                icon = "🎨",
                title = "Themes & Styling",
                subtitle = "11 presets, custom palette studio & 3D key geometry",
                badgeText = selectedTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { onNavigate(SettingsNavDestination.THEMES_AND_STYLING) }
            )

            SettingsCategoryCard(
                icon = "⌨️",
                title = "Typing Preferences",
                subtitle = "Number row, auto-suggestions, auto-correction & capitalization",
                badgeText = if (showNumberRow) "Number Row On" else null,
                onClick = { onNavigate(SettingsNavDestination.TYPING) }
            )

            SettingsCategoryCard(
                icon = "🔊",
                title = "Sound & Haptics",
                subtitle = "Keypress sound styles (Typewriter, Bubble...) & vibration",
                badgeText = selectedSoundType.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { onNavigate(SettingsNavDestination.SOUND_AND_HAPTICS) }
            )

            SettingsCategoryCard(
                icon = "📋",
                title = "Clipboard Manager",
                subtitle = "History retention, auto-close on paste & incognito mode",
                badgeText = if (clipboardHistoryEnabled) "Active" else "Off",
                onClick = { onNavigate(SettingsNavDestination.CLIPBOARD) }
            )

            SettingsCategoryCard(
                icon = "ℹ️",
                title = "About Clicksy",
                subtitle = "Credits, Buy Me a Coffee & open source details",
                onClick = { onNavigate(SettingsNavDestination.ABOUT) }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ThemesAndStylingScreen(
    selectedTheme: NeuTheme,
    customThemeConfig: CustomThemeConfig,
    selectedSize: KeyboardSize,
    selectedKeyShapeStyle: KeyShapeStyle,
    selectedKeyCornerRadius: Float,
    selectedBorderWidth: Float,
    selectedShadowOffset: Float,
    selectedKeySpacing: Float,
    showSecondarySymbols: Boolean,
    onThemeSelected: (NeuTheme) -> Unit,
    onCustomThemeChange: (CustomThemeConfig) -> Unit,
    onSizeSelected: (KeyboardSize) -> Unit,
    onKeyShapeStyleSelected: (KeyShapeStyle) -> Unit,
    onKeyCornerRadiusSelected: (Float) -> Unit,
    onBorderWidthSelected: (Float) -> Unit,
    onShadowOffsetSelected: (Float) -> Unit,
    onKeySpacingSelected: (Float) -> Unit,
    onShowSecondarySymbolsToggle: (Boolean) -> Unit,
    onEditColor: (String, Color, (Color) -> Unit) -> Unit,
    onCopyPresetClicked: () -> Unit,
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()
    var activeSubTab by remember { mutableStateOf(CustomizationSubTab.PRESETS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // 1. Top bar: Back + Title
        SettingsSubScreenTopBar(title = "🎨 Theme & Styling", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Sub-tab switcher: [ Presets | Custom Palette | Key Geometry ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                CustomizationSubTab.PRESETS to "🎨 Presets",
                CustomizationSubTab.CUSTOM_PALETTE to "🖌️ Palette",
                CustomizationSubTab.KEY_GEOMETRY to "📐 Geometry"
            ).forEach { (tab, label) ->
                val isSelected = activeSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .drawBehind {
                            if (isSelected) {
                                drawRoundRect(
                                    color = colors.shadow,
                                    topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                                    size = Size(size.width, size.height),
                                    cornerRadius = CornerRadius(8.dp.toPx())
                                )
                            }
                        }
                        .background(
                            if (isSelected) colors.actionKeyBackground else colors.keyBackground,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 2.dp,
                            colors.border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) colors.textOnAction else colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Live Keyboard Preview (placed right in the middle where user is customizing!)
        LiveKeyboardPreviewCard(
            previewDimensions = dims,
            previewColorScheme = colors
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Controls for the active sub-tab
        when (activeSubTab) {
            CustomizationSubTab.PRESETS -> {
                PresetThemesSection(
                    selectedTheme = selectedTheme,
                    customThemeConfig = customThemeConfig,
                    onThemeSelected = onThemeSelected
                )
            }
            CustomizationSubTab.CUSTOM_PALETTE -> {
                CustomPaletteStudioCard(
                    config = customThemeConfig,
                    isCustomActive = selectedTheme == NeuTheme.CUSTOM,
                    onCustomThemeChange = onCustomThemeChange,
                    onEditColor = onEditColor,
                    onCopyPresetClicked = onCopyPresetClicked,
                    onRandomizeClicked = {
                        onCustomThemeChange(CustomThemeConfig.generateRandom())
                    },
                    onResetClicked = {
                        onCustomThemeChange(CustomThemeConfig.fromPreset(SunshineColors))
                    }
                )
            }
            CustomizationSubTab.KEY_GEOMETRY -> {
                KeyGeometryStudioCard(
                    shapeStyle = selectedKeyShapeStyle,
                    cornerRadius = selectedKeyCornerRadius,
                    borderWidth = selectedBorderWidth,
                    shadowOffset = selectedShadowOffset,
                    keySpacing = selectedKeySpacing,
                    showSecondarySymbols = showSecondarySymbols,
                    onShapeStyleSelected = onKeyShapeStyleSelected,
                    onCornerRadiusSelected = onKeyCornerRadiusSelected,
                    onBorderWidthSelected = onBorderWidthSelected,
                    onShadowOffsetSelected = onShadowOffsetSelected,
                    onKeySpacingSelected = onKeySpacingSelected,
                    onShowSecondarySymbolsToggle = onShowSecondarySymbolsToggle
                )

                Spacer(modifier = Modifier.height(18.dp))

                KeyboardSizeSection(
                    selectedSize = selectedSize,
                    onSizeSelected = onSizeSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PresetThemesSection(
    selectedTheme: NeuTheme,
    customThemeConfig: CustomThemeConfig,
    onThemeSelected: (NeuTheme) -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions

    Text(
        text = "Preset Themes",
        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        color = colors.textPrimary
    )
    Text(
        text = "Tap a preset to instantly apply its neubrutalist palette",
        style = ClicksyTypography.monoLabel,
        color = colors.textSecondary
    )

    Spacer(modifier = Modifier.height(14.dp))

    val presetThemes = listOf(
        Triple(NeuTheme.SUNSHINE, "Sunshine", SunshineColors),
        Triple(NeuTheme.BUBBLEGUM, "Bubblegum", BubblegumColors),
        Triple(NeuTheme.MINTY, "Minty", MintyColors),
        Triple(NeuTheme.MIDNIGHT, "Midnight", MidnightColors),
        Triple(NeuTheme.CYBERPUNK, "Cyberpunk", CyberpunkColors),
        Triple(NeuTheme.LAVENDER, "Lavender", LavenderColors),
        Triple(NeuTheme.FOREST, "Forest", ForestColors),
        Triple(NeuTheme.RETRO, "Retro CRT", RetroTerminalColors),
        Triple(NeuTheme.SUNSET, "Sunset", SunsetColors),
        Triple(NeuTheme.MONOCHROME, "Monochrome", MonochromeColors),
        Triple(NeuTheme.ADAPTIVE, "Adaptive", NeuTheme.ADAPTIVE.toColorScheme()),
        Triple(NeuTheme.CUSTOM, "Custom ✦", customThemeConfig.toColorScheme())
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        presetThemes.chunked(2).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPresets.forEach { (themeEnum, label, scheme) ->
                    ThemeOption(
                        label = label,
                        colorScheme = scheme,
                        isSelected = selectedTheme == themeEnum,
                        onClick = { onThemeSelected(themeEnum) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPresets.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KeyboardSizeSection(
    selectedSize: KeyboardSize,
    onSizeSelected: (KeyboardSize) -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    Text(
        text = "Keyboard Height Scale",
        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            KeyboardSize.SMALL to "Small (85%)",
            KeyboardSize.MEDIUM to "Medium (100%)",
            KeyboardSize.LARGE to "Large (115%)",
            KeyboardSize.EXTRA_LARGE to "XL (130%)"
        ).forEach { (kbSize, label) ->
            val isSelected = selectedSize == kbSize
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        if (isSelected) {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                    }
                    .background(
                        if (isSelected) colors.accentKeyBackground else colors.keyBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        if (isSelected) 3.5.dp else 2.5.dp,
                        colors.border,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSizeSelected(kbSize) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                    color = colors.textPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SoundSelector(
    selectedSoundType: SoundType,
    onSoundTypeSelected: (SoundType) -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    Text(
        text = "Keypress Sound",
        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = listOf(
            listOf(SoundType.MUTE to "Mute", SoundType.SYSTEM to "System", SoundType.BUBBLE to "Bubble"),
            listOf(SoundType.WOODBLOCK to "Woodblock", SoundType.TYPEWRITER to "Typewriter", SoundType.CHIME to "Chime")
        )
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (sType, label) ->
                    val isSelected = selectedSoundType == sType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBehind {
                                if (isSelected) {
                                    drawRoundRect(
                                        color = colors.shadow,
                                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                        size = Size(size.width, size.height),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )
                                }
                            }
                            .background(
                                if (isSelected) colors.accentKeyBackground else colors.keyBackground,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isSelected) 3.5.dp else 2.5.dp,
                                colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSoundTypeSelected(sType) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                            color = colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingPreferencesScreen(
    autocompleteEnabled: Boolean,
    autocorrectEnabled: Boolean,
    autoCapitalizationEnabled: Boolean,
    showNumberRow: Boolean,
    hinglishEnabled: Boolean,
    learningEnabled: Boolean,
    showSecondarySymbols: Boolean,
    selectedSize: KeyboardSize,
    onAutocompleteToggle: (Boolean) -> Unit,
    onAutocorrectToggle: (Boolean) -> Unit,
    onAutoCapitalizationToggle: (Boolean) -> Unit,
    onShowNumberRowToggle: (Boolean) -> Unit,
    onHinglishToggle: (Boolean) -> Unit,
    onLearningToggle: (Boolean) -> Unit,
    onClearLearnedVocabulary: () -> Unit,
    onShowSecondarySymbolsToggle: (Boolean) -> Unit,
    onSizeSelected: (KeyboardSize) -> Unit,
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        SettingsSubScreenTopBar(title = "⌨️ Typing Preferences", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        KeyboardSizeSection(
            selectedSize = selectedSize,
            onSizeSelected = onSizeSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Input & Keys",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = "Show Dedicated Number Row",
            description = "Show 0-9 digits permanently at the top of the QWERTY keyboard",
            checked = showNumberRow,
            onCheckedChange = onShowNumberRowToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = stringResource(R.string.auto_capitalization),
            description = stringResource(R.string.auto_capitalization_desc),
            checked = autoCapitalizationEnabled,
            onCheckedChange = onAutoCapitalizationToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = "Secondary Symbol Hints",
            description = "Display secondary characters on letter keys accessible via long-press",
            checked = showSecondarySymbols,
            onCheckedChange = onShowSecondarySymbolsToggle
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Suggestions & Corrections",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = stringResource(R.string.autocomplete),
            description = stringResource(R.string.autocomplete_desc),
            checked = autocompleteEnabled,
            onCheckedChange = onAutocompleteToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = stringResource(R.string.autocorrect),
            description = stringResource(R.string.autocorrect_desc),
            checked = autocorrectEnabled,
            onCheckedChange = onAutocorrectToggle
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Language & Hinglish Prediction",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = "Hinglish & Code-Switching",
            description = "Support Roman Hindi vocabulary, natural English-Hinglish code-switching, and Indian chat shortcuts (e.g. 'kya kr rhe ho', 'gm', 'nhi')",
            checked = hinglishEnabled,
            onCheckedChange = onHinglishToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = "Personalized Learning",
            description = "Learn your frequently typed words and phrases locally on-device. 100% private and offline (never uploaded)",
            checked = learningEnabled,
            onCheckedChange = onLearningToggle
        )
        Spacer(modifier = Modifier.height(14.dp))

        var showClearDialog by remember { mutableStateOf(false) }
        var showClearedToast by remember { mutableStateOf(false) }

        if (showClearDialog) {
            Dialog(onDismissRequest = { showClearDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.keyBackground)
                        .border(dims.borderWidth, colors.border, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Clear Learned Vocabulary?",
                            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will permanently remove all words, phrases, and next-word patterns learned on this device.",
                            style = ClicksyTypography.suggestionText.copy(fontSize = 13.sp),
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Cancel",
                                modifier = Modifier
                                    .clickable { showClearDialog = false }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Clear",
                                modifier = Modifier
                                    .clickable {
                                        onClearLearnedVocabulary()
                                        showClearDialog = false
                                        showClearedToast = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.keyBackground)
                .border(dims.borderWidth, colors.border, RoundedCornerShape(12.dp))
                .clickable { showClearDialog = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clear Learned Vocabulary",
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showClearedToast) "✓ Learned vocabulary successfully cleared" else "Reset on-device learned words and contextual n-grams",
                        style = ClicksyTypography.suggestionText.copy(fontSize = 12.sp),
                        color = if (showClearedToast) Color(0xFF2E7D32) else colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TestKeyboardCard()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SoundAndHapticsScreen(
    selectedSoundType: SoundType,
    hapticEnabled: Boolean,
    onSoundTypeSelected: (SoundType) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        SettingsSubScreenTopBar(title = "🔊 Sound & Haptics", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        SoundSelector(
            selectedSoundType = selectedSoundType,
            onSoundTypeSelected = onSoundTypeSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tactile Vibration",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = stringResource(R.string.haptic_feedback),
            description = "Vibrate with zero latency on every key press",
            checked = hapticEnabled,
            onCheckedChange = onHapticToggle
        )

        Spacer(modifier = Modifier.height(24.dp))

        TestKeyboardCard()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ClipboardSettingsScreen(
    clipboardHistoryEnabled: Boolean,
    clipboardAutoClose: Boolean,
    clipboardIncognito: Boolean,
    onClipboardHistoryToggle: (Boolean) -> Unit,
    onClipboardAutoCloseToggle: (Boolean) -> Unit,
    onClipboardIncognitoToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        SettingsSubScreenTopBar(title = "📋 Clipboard Manager", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Clipboard Preferences",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = "Enable Clipboard History",
            description = "Save copies to Room database for rapid pasting in the clipboard panel",
            checked = clipboardHistoryEnabled,
            onCheckedChange = onClipboardHistoryToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = "Auto-close Clipboard after Paste",
            description = "Automatically switch back to the keyboard when a clip is tapped",
            checked = clipboardAutoClose,
            onCheckedChange = onClipboardAutoCloseToggle
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggle(
            label = "Incognito Clipboard Mode",
            description = "Temporarily pause recording copies into history",
            checked = clipboardIncognito,
            onCheckedChange = onClipboardIncognitoToggle
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(colors.keyBackground, RoundedCornerShape(8.dp))
                .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "🔒 Privacy & Local Storage",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All clipboard history is stored exclusively on your device inside an offline SQLite/Room database. No network requests are made, and your clips never leave your phone.",
                    style = ClicksyTypography.clipboardText.copy(fontSize = 12.sp),
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AboutSettingsScreen(
    onBack: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        SettingsSubScreenTopBar(title = "ℹ️ About Clicksy", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .background(colors.keyBackground, RoundedCornerShape(10.dp))
                .border(3.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.about),
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.version),
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.author_credit),
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = colors.accentKeyBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A neubrutalism-styled keyboard with custom shapes, bold borders, hard shadows, and infinite color palettes.",
                    style = ClicksyTypography.clipboardText,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                        .background(Color(0xFFFFDD00), RoundedCornerShape(8.dp))
                        .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                uriHandler.openUri("https://buymeacoffee.com/bennyaliston")
                            } catch (_: Exception) { }
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☕ " + stringResource(R.string.buy_me_a_coffee),
                        style = ClicksyTypography.suggestionText.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF1A1A2E)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )
                        }
                        .background(colors.accentKeyBackground, RoundedCornerShape(8.dp))
                        .border(2.5.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                uriHandler.openUri("https://github.com/BennyAliston/Clicksy")
                            } catch (_: Exception) { }
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⭐ View on GitHub",
                        style = ClicksyTypography.suggestionText.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Live interactive keyboard preview card showing actual NeuKeys in real time.
 */
@Composable
private fun LiveKeyboardPreviewCard(
    previewDimensions: ClicksyDimensions,
    previewColorScheme: ClicksyColorScheme
) {
    val colors = previewColorScheme
    val dims = previewDimensions
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .background(colors.background, shape)
            .border(dims.borderWidth.coerceAtLeast(1.5.dp), colors.border, shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Live Keyboard Preview",
                style = ClicksyTypography.suggestionText.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .background(colors.accentKeyBackground, RoundedCornerShape(4.dp))
                    .border(1.5.dp, colors.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Interactive",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mini live keyboard frame
        ClicksyTheme(
            colorScheme = previewColorScheme,
            dimensions = previewDimensions.copy(keyHeight = 44.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background, RoundedCornerShape(8.dp))
                    .padding(vertical = dims.keyboardPadding),
                verticalArrangement = Arrangement.spacedBy(dims.keySpacing)
            ) {
                // Row 1: Q W E R T Y U I O P
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.keySpacing)
                ) {
                    val r1 = listOf(
                        "Q" to "~", "W" to "\\", "E" to "|", "R" to "=", "T" to "/",
                        "Y" to "_", "U" to "<", "I" to ">", "O" to "[", "P" to "]"
                    )
                    r1.forEach { (char, sym) ->
                        NeuKey(
                            label = char,
                            subLabel = sym,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: A S D F G H J K L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.keySpacing)
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    val r2 = listOf(
                        "A" to "@", "S" to "#", "D" to "$", "F" to "%", "G" to "&",
                        "H" to "-", "J" to "+", "K" to "(", "L" to ")"
                    )
                    r2.forEach { (char, sym) ->
                        NeuKey(
                            label = char,
                            subLabel = sym,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Row 3: Shift, Z X C V B N M, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.keySpacing)
                ) {
                    NeuKey(
                        label = "⇧",
                        modifier = Modifier.weight(1.3f),
                        keyType = KeyType.ACCENT
                    )
                    val r3 = listOf(
                        "Z" to "*", "X" to "\"", "C" to "'", "V" to ":",
                        "B" to ";", "N" to "!", "M" to "?"
                    )
                    r3.forEach { (char, sym) ->
                        NeuKey(
                            label = char,
                            subLabel = sym,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    NeuKey(
                        label = "⌫",
                        modifier = Modifier.weight(1.3f),
                        keyType = KeyType.ACCENT
                    )
                }

                // Row 4: ?123, Emoji, Space, Enter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.keySpacing)
                ) {
                    NeuKey(
                        label = "?123",
                        modifier = Modifier.weight(1.4f),
                        keyType = KeyType.ACCENT
                    )
                    NeuKey(
                        label = "😀",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT
                    )
                    NeuKey(
                        label = "Clicksy",
                        modifier = Modifier.weight(4.5f),
                        keyType = KeyType.CHARACTER
                    )
                    NeuKey(
                        label = "↵",
                        modifier = Modifier.weight(1.5f),
                        keyType = KeyType.ACTION
                    )
                }
            }
        }
    }
}

/**
 * Card for fine-tuning Key Shape, Corner Radius, Borders, Shadows, and Spacing.
 */
@Composable
private fun KeyGeometryStudioCard(
    shapeStyle: KeyShapeStyle,
    cornerRadius: Float,
    borderWidth: Float,
    shadowOffset: Float,
    keySpacing: Float,
    showSecondarySymbols: Boolean,
    onShapeStyleSelected: (KeyShapeStyle) -> Unit,
    onCornerRadiusSelected: (Float) -> Unit,
    onBorderWidthSelected: (Float) -> Unit,
    onShadowOffsetSelected: (Float) -> Unit,
    onKeySpacingSelected: (Float) -> Unit,
    onShowSecondarySymbolsToggle: (Boolean) -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val cardShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .background(colors.keyBackground, cardShape)
            .border(3.dp, colors.border, cardShape)
            .padding(16.dp)
    ) {
        // 1. Key Shape Style
        Text(
            text = "Key Silhouette Shape",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                KeyShapeStyle.SHARP to "Sharp",
                KeyShapeStyle.ROUNDED to "Rounded",
                KeyShapeStyle.SOFT to "Soft",
                KeyShapeStyle.PILL to "Pill",
                KeyShapeStyle.CUT_CORNER to "Cyber Cut"
            ).forEach { (style, label) ->
                val isSelected = shapeStyle == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colors.accentKeyBackground else colors.background,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 1.5.dp,
                            colors.border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onShapeStyleSelected(style) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Corner Radius / Cut Size
        Text(
            text = "Corner Curvature / Cut Size (${cornerRadius.toInt()} dp)",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0f, 4f, 8f, 12f, 16f, 20f, 24f).forEach { r ->
                val isSelected = cornerRadius == r
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colors.accentKeyBackground else colors.background,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 1.5.dp,
                            colors.border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onCornerRadiusSelected(r) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${r.toInt()}",
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Border Width
        Text(
            text = "Border Width (${if (borderWidth == 0f) "None" else "${borderWidth} dp"})",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0f to "0",
                1.5f to "1.5",
                3f to "3.0",
                4.5f to "4.5",
                6f to "6.0"
            ).forEach { (w, label) ->
                val isSelected = borderWidth == w
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colors.accentKeyBackground else colors.background,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 1.5.dp,
                            colors.border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onBorderWidthSelected(w) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Shadow Depth / 3D Offset
        Text(
            text = "3D Shadow Depth (${if (shadowOffset == 0f) "Flat" else "${shadowOffset.toInt()} dp"})",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0f to "Flat (0)",
                2f to "Subtle (2)",
                4f to "Classic (4)",
                6f to "Deep (6)"
            ).forEach { (offset, label) ->
                val isSelected = shadowOffset == offset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colors.accentKeyBackground else colors.background,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 1.5.dp,
                            colors.border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onShadowOffsetSelected(offset) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Key Spacing / Gap
        Text(
            text = "Key Gap / Spacing (${keySpacing.toInt()} dp)",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                2f to "Tight (2dp)",
                4f to "Normal (4dp)",
                6f to "Spacious (6dp)"
            ).forEach { (gap, label) ->
                val isSelected = keySpacing == gap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colors.accentKeyBackground else colors.background,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            if (isSelected) 3.dp else 1.5.dp,
                            colors.border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onKeySpacingSelected(gap) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Secondary Symbols
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Key Secondary Symbol Hints",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Display long-press punctuation marks on letter keys",
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
            }
            Switch(
                checked = showSecondarySymbols,
                onCheckedChange = onShowSecondarySymbolsToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.keyBackground,
                    checkedTrackColor = colors.accentKeyBackground,
                    checkedBorderColor = colors.border,
                    uncheckedThumbColor = colors.keyBackground,
                    uncheckedTrackColor = colors.background,
                    uncheckedBorderColor = colors.border
                )
            )
        }
    }
}

/**
 * Custom Palette Studio card letting the user customize every color token.
 */
@Composable
private fun CustomPaletteStudioCard(
    config: CustomThemeConfig,
    isCustomActive: Boolean,
    onCustomThemeChange: (CustomThemeConfig) -> Unit,
    onEditColor: (String, Color, (Color) -> Unit) -> Unit,
    onCopyPresetClicked: () -> Unit,
    onRandomizeClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val cardShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .background(colors.keyBackground, cardShape)
            .border(if (isCustomActive) 3.5.dp else 2.5.dp, colors.border, cardShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎨 Custom Palette Studio",
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = if (isCustomActive) "Active Theme" else "Tap a color below to customize",
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
            }
            if (isCustomActive) {
                Box(
                    modifier = Modifier
                        .background(colors.actionKeyBackground, RoundedCornerShape(4.dp))
                        .border(1.5.dp, colors.border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LIVE",
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textOnAction
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy from preset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onCopyPresetClicked() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📋 Copy Preset",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }

            // Randomize
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.actionKeyBackground, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onRandomizeClicked() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎲 Randomize",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textOnAction
                )
            }

            // Reset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.background, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .clickable { onResetClicked() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔄 Reset",
                    style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val colorItems = listOf(
            Triple("Keyboard Background", config.background) { c: Color -> onCustomThemeChange(config.copy(background = c, suggestionBarBackground = c)) },
            Triple("Letter Keys Background", config.keyBackground) { c: Color -> onCustomThemeChange(config.copy(keyBackground = c)) },
            Triple("Accent Keys Background", config.accentKeyBackground) { c: Color -> onCustomThemeChange(config.copy(accentKeyBackground = c)) },
            Triple("Action Key Background", config.actionKeyBackground) { c: Color -> onCustomThemeChange(config.copy(actionKeyBackground = c)) },
            Triple("Border Color", config.border) { c: Color -> onCustomThemeChange(config.copy(border = c)) },
            Triple("Shadow Color", config.shadow) { c: Color -> onCustomThemeChange(config.copy(shadow = c)) },
            Triple("Key Text Color", config.textPrimary) { c: Color -> onCustomThemeChange(config.copy(textPrimary = c)) },
            Triple("Action Key Text Color", config.textOnAction) { c: Color -> onCustomThemeChange(config.copy(textOnAction = c)) },
            Triple("Secondary Hints Color", config.textSecondary) { c: Color -> onCustomThemeChange(config.copy(textSecondary = c)) },
            Triple("Suggestion Bar Background", config.suggestionBarBackground) { c: Color -> onCustomThemeChange(config.copy(suggestionBarBackground = c)) }
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            colorItems.forEach { (title, colorValue, updater) ->
                ColorPickerRow(
                    label = title,
                    color = colorValue,
                    onClick = { onEditColor(title, colorValue, updater) }
                )
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val hex = colorToHex(color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background, RoundedCornerShape(6.dp))
            .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = ClicksyTypography.suggestionText.copy(fontSize = 13.sp),
            color = colors.textPrimary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = hex,
                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp),
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, colors.border, CircleShape)
            )
        }
    }
}

/**
 * Interactive dialog for selecting or typing any color in the spectrum.
 */
@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    var redSlider by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var greenSlider by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blueSlider by remember { mutableFloatStateOf(initialColor.blue * 255f) }
    var hexInput by remember { mutableStateOf(colorToHex(initialColor).removePrefix("#")) }

    fun updateFromRgb(r: Float, g: Float, b: Float) {
        redSlider = r
        greenSlider = g
        blueSlider = b
        val col = Color(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
        currentColor = col
        hexInput = colorToHex(col).removePrefix("#")
    }

    Dialog(onDismissRequest = onDismiss) {
        val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
        val dialogShape = RoundedCornerShape(12.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFF1A1A2E),
                        topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
                .background(Color.White, dialogShape)
                .border(3.5.dp, Color(0xFF1A1A2E), dialogShape)
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = Color(0xFF1A1A2E)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color preview comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Initial
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Current", style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp), color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(initialColor, RoundedCornerShape(6.dp))
                            .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                    )
                }

                // New
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "New Color", style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp), color = Color(0xFF1A1A2E))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(currentColor, RoundedCornerShape(6.dp))
                            .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hex Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F7), RoundedCornerShape(6.dp))
                    .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "HEX: #", style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1A1A2E))
                BasicTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                        hexInput = clean
                        if (clean.length == 6) {
                            val parsed = parseHexToColor(clean, currentColor)
                            currentColor = parsed
                            redSlider = parsed.red * 255f
                            greenSlider = parsed.green * 255f
                            blueSlider = parsed.blue * 255f
                        }
                    },
                    textStyle = ClicksyTypography.monoLabel.copy(fontSize = 14.sp, color = Color(0xFF1A1A2E)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Swatches
            Text(
                text = "Quick Palette Swatches",
                style = ClicksyTypography.monoLabel.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(6.dp))

            val swatches = listOf(
                Color(0xFFFFFFFF), Color(0xFFF5F5F7), Color(0xFFE2E8F0), Color(0xFF64748B), Color(0xFF1E293B), Color(0xFF000000),
                Color(0xFFFFE156), Color(0xFFFFD93D), Color(0xFFFFAB00), Color(0xFFFF6B6B), Color(0xFFFF0055), Color(0xFFFF69B4),
                Color(0xFF00F0FF), Color(0xFF00D4AA), Color(0xFF7DFFB3), Color(0xFF33FF33), Color(0xFF4D96FF), Color(0xFFC084FC)
            )

            swatches.chunked(6).forEach { swatchRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    swatchRow.forEach { swColor ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swColor)
                                .border(
                                    if (currentColor == swColor) 3.dp else 1.5.dp,
                                    if (currentColor == swColor) Color(0xFFFF0055) else Color(0xFF1A1A2E),
                                    CircleShape
                                )
                                .clickable {
                                    updateFromRgb(swColor.red * 255f, swColor.green * 255f, swColor.blue * 255f)
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RGB Sliders
            Text(
                text = "Red: ${redSlider.toInt()}",
                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                color = Color.Red
            )
            Slider(
                value = redSlider,
                onValueChange = { updateFromRgb(it, greenSlider, blueSlider) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
            )

            Text(
                text = "Green: ${greenSlider.toInt()}",
                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                color = Color(0xFF00A86B)
            )
            Slider(
                value = greenSlider,
                onValueChange = { updateFromRgb(redSlider, it, blueSlider) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00A86B), activeTrackColor = Color(0xFF00A86B))
            )

            Text(
                text = "Blue: ${blueSlider.toInt()}",
                style = ClicksyTypography.monoLabel.copy(fontSize = 10.sp),
                color = Color.Blue
            )
            Slider(
                value = blueSlider,
                onValueChange = { updateFromRgb(redSlider, greenSlider, it) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                        .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1A2E)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFFE156), RoundedCornerShape(6.dp))
                        .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                        .clickable { onColorSelected(currentColor) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply",
                        style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1A2E)
                    )
                }
            }
        }
    }
}

/**
 * Dialog to clone any preset theme into the custom theme builder.
 */
@Composable
private fun CopyPresetDialog(
    onPresetChosen: (ClicksyColorScheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFF1A1A2E),
                        topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
                .background(Color.White, dialogShape)
                .border(3.5.dp, Color(0xFF1A1A2E), dialogShape)
                .padding(18.dp)
        ) {
            Text(
                text = "Copy Preset to Custom",
                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = Color(0xFF1A1A2E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a preset theme as your starting template:",
                style = ClicksyTypography.monoLabel,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            val presets = listOf(
                "Sunshine" to SunshineColors,
                "Bubblegum" to BubblegumColors,
                "Minty" to MintyColors,
                "Midnight" to MidnightColors,
                "Cyberpunk" to CyberpunkColors,
                "Lavender" to LavenderColors,
                "Forest" to ForestColors,
                "Retro CRT" to RetroTerminalColors,
                "Sunset" to SunsetColors,
                "Monochrome" to MonochromeColors
            )

            presets.forEach { (name, scheme) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(scheme.background, RoundedCornerShape(6.dp))
                        .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                        .clickable { onPresetChosen(scheme) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = scheme.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(scheme.accentKeyBackground, scheme.actionKeyBackground, scheme.keyBackground).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color(0xFF1A1A2E), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                    .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = ClicksyTypography.monoLabel.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A2E)
                )
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun parseHexToColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    buttonLabel: String,
    buttonColor: Color,
    buttonTextColor: Color,
    onClick: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .background(colors.keyBackground, shape)
            .border(3.dp, colors.border, shape)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = ClicksyTypography.suggestionText.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = ClicksyTypography.clipboardText,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .background(buttonColor, shape)
                .border(3.dp, colors.border, shape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonLabel,
                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                color = buttonTextColor
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    colorScheme: ClicksyColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val shape = RoundedCornerShape(8.dp)
    val borderWidth = if (isSelected) 4.dp else 2.5.dp

    Column(
        modifier = modifier
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
            }
            .background(colorScheme.background, shape)
            .border(borderWidth, colors.border, shape)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                colorScheme.accentKeyBackground,
                colorScheme.actionKeyBackground,
                colorScheme.keyBackground
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color, RoundedCornerShape(4.dp))
                        .border(1.5.dp, colors.border, RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = ClicksyTypography.monoLabel.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = colorScheme.textPrimary
        )
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.keyBackground, shape)
            .border(3.dp, colors.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = ClicksyTypography.monoLabel,
                color = colors.textSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.keyBackground,
                checkedTrackColor = colors.accentKeyBackground,
                checkedBorderColor = colors.border,
                uncheckedThumbColor = colors.keyBackground,
                uncheckedTrackColor = colors.background,
                uncheckedBorderColor = colors.border
            )
        )
    }
}

@Composable
private fun TestKeyboardCard() {
    var text by remember { mutableStateOf("") }
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.shadow,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .background(colors.keyBackground, shape)
            .border(3.dp, colors.border, shape)
            .padding(16.dp)
    ) {
        Text(
            text = "③ " + stringResource(R.string.test_keyboard),
            style = ClicksyTypography.suggestionText.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.test_keyboard_desc),
            style = ClicksyTypography.clipboardText,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = ClicksyTypography.suggestionText.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.textPrimary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colors.background, shape)
                        .border(3.dp, colors.border, shape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.test_keyboard_placeholder),
                            style = ClicksyTypography.monoLabel,
                            color = colors.textSecondary
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
