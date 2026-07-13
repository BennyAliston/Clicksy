package com.clicksy.keyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.R
import com.clicksy.keyboard.data.NeuTheme
import com.clicksy.keyboard.data.KeyboardSize
import com.clicksy.keyboard.data.ClipboardManagerService
import com.clicksy.keyboard.service.ClicksyService
import com.clicksy.keyboard.ui.theme.BubblegumColors
import com.clicksy.keyboard.ui.theme.MintyColors
import com.clicksy.keyboard.ui.theme.ClicksyColorScheme
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.SunshineColors
import com.clicksy.keyboard.ui.theme.toColorScheme
import com.clicksy.keyboard.util.SoundType

/**
 * Settings / onboarding activity for Clicksy.
 * Launched from home screen and system IME settings.
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

            var selectedSize by remember {
                val sizeName = prefs.getString(ClicksyService.KEY_KEYBOARD_SIZE, KeyboardSize.MEDIUM.name)
                mutableStateOf(
                    try { KeyboardSize.valueOf(sizeName ?: KeyboardSize.MEDIUM.name) }
                    catch (e: Exception) { KeyboardSize.MEDIUM }
                )
            }

            var hapticEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_HAPTIC, true))
            }

            var autocompleteEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_AUTOCOMPLETE, true))
            }

            var autoCapitalizationEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_AUTO_CAPITALIZATION, true))
            }

            var showNumberRow by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_SHOW_NUMBER_ROW, false))
            }

            var roundedKeysEnabled by remember {
                mutableStateOf(prefs.getBoolean(ClicksyService.KEY_ROUNDED_KEYS, false))
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

            ClicksyTheme(colorScheme = selectedTheme.toColorScheme()) {
                SettingsScreen(
                    selectedTheme = selectedTheme,
                    selectedSize = selectedSize,
                    selectedSoundType = selectedSoundType,
                    hapticEnabled = hapticEnabled,
                    autocompleteEnabled = autocompleteEnabled,
                    autoCapitalizationEnabled = autoCapitalizationEnabled,
                    showNumberRow = showNumberRow,
                    roundedKeysEnabled = roundedKeysEnabled,
                    clipboardHistoryEnabled = clipboardHistoryEnabled,
                    onThemeSelected = { theme ->
                        selectedTheme = theme
                        prefs.edit().putString(ClicksyService.KEY_THEME, theme.name).apply()
                    },
                    onSizeSelected = { size ->
                        selectedSize = size
                        prefs.edit().putString(ClicksyService.KEY_KEYBOARD_SIZE, size.name).apply()
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
                    onAutoCapitalizationToggle = { enabled ->
                        autoCapitalizationEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_AUTO_CAPITALIZATION, enabled).apply()
                    },
                    onShowNumberRowToggle = { enabled ->
                        showNumberRow = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_SHOW_NUMBER_ROW, enabled).apply()
                    },
                    onRoundedKeysToggle = { enabled ->
                        roundedKeysEnabled = enabled
                        prefs.edit().putBoolean(ClicksyService.KEY_ROUNDED_KEYS, enabled).apply()
                    },
                    onClipboardHistoryToggle = { enabled ->
                        clipboardHistoryEnabled = enabled
                        prefs.edit().putBoolean(ClipboardManagerService.KEY_CLIPBOARD_HISTORY_ENABLED, enabled).apply()
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
    selectedSize: KeyboardSize,
    selectedSoundType: SoundType,
    hapticEnabled: Boolean,
    autocompleteEnabled: Boolean,
    autoCapitalizationEnabled: Boolean,
    showNumberRow: Boolean,
    roundedKeysEnabled: Boolean,
    clipboardHistoryEnabled: Boolean,
    onThemeSelected: (NeuTheme) -> Unit,
    onSizeSelected: (KeyboardSize) -> Unit,
    onSoundTypeSelected: (SoundType) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onAutocompleteToggle: (Boolean) -> Unit,
    onAutoCapitalizationToggle: (Boolean) -> Unit,
    onShowNumberRowToggle: (Boolean) -> Unit,
    onRoundedKeysToggle: (Boolean) -> Unit,
    onClipboardHistoryToggle: (Boolean) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit
) {
    val colors = ClicksyTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.app_name),
            style = ClicksyTypography.headerText.copy(fontSize = 36.sp),
            color = colors.textPrimary
        )
        Text(
            text = "Neubrutalism keyboard",
            style = ClicksyTypography.monoLabel,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Step 1: Enable keyboard
        SectionCard(
            title = "① " + stringResource(R.string.enable_keyboard),
            description = stringResource(R.string.enable_keyboard_desc),
            buttonLabel = stringResource(R.string.enable_keyboard),
            buttonColor = colors.actionKeyBackground,
            buttonTextColor = colors.textOnAction,
            onClick = onEnableKeyboard
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Select keyboard
        SectionCard(
            title = "② " + stringResource(R.string.select_keyboard),
            description = stringResource(R.string.select_keyboard_desc),
            buttonLabel = stringResource(R.string.select_keyboard),
            buttonColor = colors.accentKeyBackground,
            buttonTextColor = colors.textPrimary,
            onClick = onSelectKeyboard
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Test keyboard
        TestKeyboardCard()

        Spacer(modifier = Modifier.height(28.dp))

        // Theme selection
        Text(
            text = stringResource(R.string.theme_title),
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOption(
                    label = stringResource(R.string.theme_sunshine),
                    colorScheme = SunshineColors,
                    isSelected = selectedTheme == NeuTheme.SUNSHINE,
                    onClick = { onThemeSelected(NeuTheme.SUNSHINE) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    label = stringResource(R.string.theme_bubblegum),
                    colorScheme = BubblegumColors,
                    isSelected = selectedTheme == NeuTheme.BUBBLEGUM,
                    onClick = { onThemeSelected(NeuTheme.BUBBLEGUM) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOption(
                    label = stringResource(R.string.theme_minty),
                    colorScheme = MintyColors,
                    isSelected = selectedTheme == NeuTheme.MINTY,
                    onClick = { onThemeSelected(NeuTheme.MINTY) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    label = "Adaptive",
                    colorScheme = NeuTheme.ADAPTIVE.toColorScheme(),
                    isSelected = selectedTheme == NeuTheme.ADAPTIVE,
                    onClick = { onThemeSelected(NeuTheme.ADAPTIVE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Keyboard Size selection
        Text(
            text = "Keyboard Size",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                KeyboardSize.SMALL to "85%",
                KeyboardSize.MEDIUM to "100%",
                KeyboardSize.LARGE to "115%",
                KeyboardSize.EXTRA_LARGE to "130%"
            ).forEach { (kbSize, label) ->
                val isSelected = selectedSize == kbSize
                val shape = RoundedCornerShape(8.dp)
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
                            shape
                        )
                        .border(
                            if (isSelected) 4.dp else 3.dp,
                            colors.border,
                            shape
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

        Spacer(modifier = Modifier.height(28.dp))

        // Keypress Sound selection
        Text(
            text = "Keypress Sound",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        val shape = RoundedCornerShape(8.dp)
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
                                    shape
                                )
                                .border(
                                    if (isSelected) 4.dp else 3.dp,
                                    colors.border,
                                    shape
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

        Spacer(modifier = Modifier.height(28.dp))

        // Toggle settings
        Text(
            text = "Preferences",
            style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            label = stringResource(R.string.haptic_feedback),
            description = "Vibrate on key press",
            checked = hapticEnabled,
            onCheckedChange = onHapticToggle
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            label = stringResource(R.string.autocomplete),
            description = "Show predictive word suggestions above the keyboard",
            checked = autocompleteEnabled,
            onCheckedChange = onAutocompleteToggle
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            label = stringResource(R.string.auto_capitalization),
            description = stringResource(R.string.auto_capitalization_desc),
            checked = autoCapitalizationEnabled,
            onCheckedChange = onAutoCapitalizationToggle
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            label = "Show Number Row",
            description = "Show a dedicated row of number keys at the top of the QWERTY keyboard",
            checked = showNumberRow,
            onCheckedChange = onShowNumberRowToggle
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            label = stringResource(R.string.rounded_keys),
            description = stringResource(R.string.rounded_keys_desc),
            checked = roundedKeysEnabled,
            onCheckedChange = onRoundedKeysToggle
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggle(
            label = "Enable Clipboard History",
            description = "Save copies to clipboard history for quick pasting",
            checked = clipboardHistoryEnabled,
            onCheckedChange = onClipboardHistoryToggle
        )

        Spacer(modifier = Modifier.height(28.dp))

        // About section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.keyBackground, RoundedCornerShape(8.dp))
                .border(3.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.about),
                    style = ClicksyTypography.suggestionText.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.version),
                    style = ClicksyTypography.monoLabel,
                    color = colors.textSecondary
                )
                Text(
                    text = "A neubrutalism-styled keyboard with bold borders,\nhard shadows, and vibrant colors.",
                    style = ClicksyTypography.clipboardText,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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

        // Button
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
    val shape = RoundedCornerShape(8.dp)
    val borderWidth = if (isSelected) 4.dp else 3.dp

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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview colors
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                colorScheme.accentKeyBackground,
                colorScheme.actionKeyBackground,
                colorScheme.keyBackground
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(color, RoundedCornerShape(4.dp))
                        .border(2.dp, colors.border, RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = ClicksyTypography.monoLabel,
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

