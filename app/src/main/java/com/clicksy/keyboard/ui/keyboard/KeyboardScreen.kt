package com.clicksy.keyboard.ui.keyboard

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicksy.keyboard.data.DictionaryProvider
import com.clicksy.keyboard.data.KeyboardMode
import com.clicksy.keyboard.data.KeyboardSize
import com.clicksy.keyboard.data.ShiftState
import com.clicksy.keyboard.ui.theme.ClicksyTheme

/**
 * Main keyboard screen that orchestrates all sub-layouts.
 *
 * Manages keyboard state (mode, shift, current word) and delegates
 * to QwertyLayout, SymbolLayout, EmojiPanel, or ClipboardPanel.
 */
@SuppressLint("InternalInsetResource", "DiscouragedApi")
@Composable
fun KeyboardScreen(
    recentEmojis: List<String>,
    onEmojiUsed: (String) -> Unit,
    mode: KeyboardMode,
    onModeChange: (KeyboardMode) -> Unit,
    shiftState: ShiftState,
    onShiftToggle: () -> Unit,
    onShiftLock: () -> Unit,
    enterLabel: String,
    clipboardViewModel: ClipboardViewModel,
    onTextInput: (String) -> Unit,
    onDeleteBackward: () -> Unit,
    onEnterAction: () -> Unit,
    onSpace: () -> Unit,
    onRequestHaptic: () -> Unit,
    onVoiceInput: () -> Unit,
    onSuggestionSelected: (String) -> Unit = {},
    getCurrentWord: () -> String,
    getPreviousWord: () -> String = { "" },
    onLaunchSettings: () -> Unit,
    modifier: Modifier = Modifier,
    autocompleteEnabled: Boolean = true,
    keyboardSize: KeyboardSize = KeyboardSize.MEDIUM,
    showNumberRow: Boolean = false
) {
    val colors = ClicksyTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val navigationBarHeight = remember {
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val heightPx = resources.getDimensionPixelSize(resourceId)
            (heightPx / density.density).dp + 20.dp
        } else {
            20.dp
        }
    }

    val panelHeight = remember(keyboardSize) {
        when (keyboardSize) {
            KeyboardSize.SMALL -> 230.dp
            KeyboardSize.MEDIUM -> 280.dp
            KeyboardSize.LARGE -> 320.dp
            KeyboardSize.EXTRA_LARGE -> 360.dp
        }
    }

    // Keyboard state
    var currentWord by remember { mutableStateOf("") }
    var previousWord by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    val clipboardSuggestion by clipboardViewModel.clipboardSuggestion.collectAsState()

    // Initialize dictionary
    remember { DictionaryProvider.initialize(context); true }

    // Update suggestions based on current word
    fun updateSuggestions() {
        currentWord = getCurrentWord()
        previousWord = getPreviousWord()
    }

    LaunchedEffect(currentWord, previousWord) {
        suggestions = withContext(Dispatchers.Default) {
            if (currentWord.isNotBlank()) {
                DictionaryProvider.getSuggestions(currentWord, limit = 3)
            } else if (previousWord.isNotBlank()) {
                DictionaryProvider.getNextWordPredictions(previousWord, limit = 3)
            } else {
                emptyList()
            }
        }
    }

    // Common character input handler
    fun handleCharacterInput(text: String) {
        onTextInput(text)
        onRequestHaptic()
        updateSuggestions()
    }

    fun handleBackspace() {
        onDeleteBackward()
        onRequestHaptic()
        updateSuggestions()
    }

    fun handleSpace() {
        onSpace()
        onRequestHaptic()
        currentWord = ""
        suggestions = emptyList()
    }

    fun handleEnter() {
        onEnterAction()
        onRequestHaptic()
        currentWord = ""
        suggestions = emptyList()
    }

    fun handleSuggestionTap(suggestion: String) {
        onSuggestionSelected(suggestion)
        onRequestHaptic()
        currentWord = ""
        suggestions = emptyList()
    }

    fun handleEmojiInput(emoji: String) {
        onTextInput(emoji)
        onRequestHaptic()
        onEmojiUsed(emoji)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        when (mode) {
            KeyboardMode.QWERTY, KeyboardMode.SYMBOLS_1, KeyboardMode.SYMBOLS_2 -> {
                // Suggestion bar (shown above keyboard for text input modes)
                SuggestionBar(
                    suggestions = suggestions,
                    onSuggestionTap = ::handleSuggestionTap,
                    onClipboardTap = { onModeChange(KeyboardMode.CLIPBOARD) },
                    onVoiceTap = onVoiceInput,
                    recentClipboardText = clipboardSuggestion?.text,
                    onPasteClipboard = {
                        clipboardSuggestion?.let { suggestion ->
                            onTextInput(suggestion.text)
                            clipboardViewModel.dismissSuggestion(suggestion.id)
                        }
                    },
                    onDismissClipboard = {
                        clipboardSuggestion?.let { suggestion ->
                            clipboardViewModel.dismissSuggestion(suggestion.id)
                        }
                    }
                )

                when (mode) {
                    KeyboardMode.QWERTY -> {
                        QwertyLayout(
                            shiftState = shiftState,
                            enterLabel = enterLabel,
                            showNumberRow = showNumberRow,
                            onCharacterInput = ::handleCharacterInput,
                            onBackspace = ::handleBackspace,
                            onShiftToggle = onShiftToggle,
                            onShiftLock = onShiftLock,
                            onEnter = ::handleEnter,
                            onSwitchToSymbols = {
                                onModeChange(KeyboardMode.SYMBOLS_1)
                                onRequestHaptic()
                            },
                            onSwitchToEmoji = {
                                onModeChange(KeyboardMode.EMOJI)
                                onRequestHaptic()
                            },
                            onSpace = ::handleSpace
                        )
                    }
                    KeyboardMode.SYMBOLS_1, KeyboardMode.SYMBOLS_2 -> {
                        SymbolLayout(
                            isPage2 = mode == KeyboardMode.SYMBOLS_2,
                            enterLabel = enterLabel,
                            onCharacterInput = ::handleCharacterInput,
                            onBackspace = ::handleBackspace,
                            onEnter = ::handleEnter,
                            onSwitchToQwerty = {
                                onModeChange(KeyboardMode.QWERTY)
                                onRequestHaptic()
                            },
                            onSwitchToEmoji = {
                                onModeChange(KeyboardMode.EMOJI)
                                onRequestHaptic()
                            },
                            onToggleSymbolPage = {
                                onModeChange(if (mode == KeyboardMode.SYMBOLS_1) {
                                    KeyboardMode.SYMBOLS_2
                                } else {
                                    KeyboardMode.SYMBOLS_1
                                })
                                onRequestHaptic()
                            },
                            onSpace = ::handleSpace
                        )
                    }
                    else -> { /* handled above */ }
                }
            }

            KeyboardMode.EMOJI -> {
                EmojiPanel(
                    recentEmojis = recentEmojis,
                    onEmojiTap = ::handleEmojiInput,
                    onBackToKeyboard = {
                        onModeChange(KeyboardMode.QWERTY)
                        onRequestHaptic()
                    },
                    onBackspace = ::handleBackspace,
                    modifier = Modifier.height(panelHeight)
                )
            }

            KeyboardMode.CLIPBOARD -> {
                ClipboardPanel(
                    viewModel = clipboardViewModel,
                    onItemTap = { text ->
                        onTextInput(text)
                        onModeChange(KeyboardMode.QWERTY)
                        onRequestHaptic()
                    },
                    onBackToKeyboard = {
                        onModeChange(KeyboardMode.QWERTY)
                        onRequestHaptic()
                    },
                    onLaunchSettings = onLaunchSettings,
                    modifier = Modifier.height(panelHeight)
                )
            }
        }
        Spacer(modifier = Modifier.height(navigationBarHeight))
    }
}
