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
import com.clicksy.keyboard.data.DetectedInputType
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
    getCurrentWord: () -> String = { "" },
    getPreviousWord: () -> String = { "" },
    getCursorContext: (() -> Pair<String, String>)? = null,
    onLaunchSettings: () -> Unit,
    modifier: Modifier = Modifier,
    autocompleteEnabled: Boolean = true,
    keyboardSize: KeyboardSize = KeyboardSize.MEDIUM,
    showNumberRow: Boolean = false,
    detectedInputType: DetectedInputType = DetectedInputType.TEXT,
    suggestionsAllowed: Boolean = true
) {
    val colors = ClicksyTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current

    val navigationBarHeight = remember {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val heightPx = context.resources.getDimensionPixelSize(resourceId)
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

    // Update suggestions based on current word
    fun updateSuggestions() {
        if (getCursorContext != null) {
            val (curr, prev) = getCursorContext()
            currentWord = curr
            previousWord = prev
        } else {
            currentWord = getCurrentWord()
            previousWord = getPreviousWord()
        }
    }

    LaunchedEffect(Unit) {
        updateSuggestions()
    }

    LaunchedEffect(currentWord, previousWord, detectedInputType, suggestionsAllowed, autocompleteEnabled) {
        suggestions = withContext(Dispatchers.Default) {
            if (!suggestionsAllowed || !autocompleteEnabled) {
                emptyList()
            } else if (detectedInputType == DetectedInputType.EMAIL) {
                val emailDomains = listOf("@gmail.com", "@outlook.com", "@yahoo.com", "@icloud.com", "@hotmail.com", "@proton.me")
                if (currentWord.contains("@")) {
                    val query = currentWord.substringAfter("@").lowercase()
                    val matches = emailDomains.filter { it.removePrefix("@").startsWith(query) }
                    if (matches.isNotEmpty()) matches.take(3) else emailDomains.take(3)
                } else if (currentWord.isNotEmpty()) {
                    emailDomains.take(3)
                } else {
                    listOf("@gmail.com", "@outlook.com", "@yahoo.com")
                }
            } else if (detectedInputType == DetectedInputType.URI) {
                val tlds = listOf(".com", ".org", ".net", ".io")
                if (currentWord.isEmpty()) {
                    listOf("https://", "www.", ".com")
                } else {
                    val query = currentWord.substringAfterLast(".", "").lowercase()
                    val matches = tlds.filter { it.removePrefix(".").startsWith(query) }
                    if (matches.isNotEmpty()) matches.take(3) else tlds.take(3)
                }
            } else if (currentWord.isNotBlank()) {
                val candidates = DictionaryProvider.getSuggestions(currentWord, prevWord = previousWord, limit = 5)
                val primary = candidates.firstOrNull() ?: currentWord
                val isCustomSpelling = currentWord.length >= 2 && !primary.equals(currentWord, ignoreCase = true) && !candidates.any { it.equals(currentWord, ignoreCase = true) }
                val left = if (isCustomSpelling) {
                    "\"$currentWord\""
                } else {
                    candidates.getOrNull(1) ?: ""
                }
                val right = if (left == "\"$currentWord\"") {
                    candidates.getOrNull(1) ?: candidates.getOrNull(2) ?: ""
                } else {
                    candidates.getOrNull(2) ?: ""
                }

                listOf(left, primary, right).filter { it.isNotEmpty() }
            } else if (previousWord.isNotBlank()) {
                val nextWords = DictionaryProvider.getNextWordPredictions(previousWord, limit = 3)
                if (nextWords.size >= 2) {
                    listOf(nextWords.getOrNull(1) ?: "", nextWords.getOrNull(0) ?: "", nextWords.getOrNull(2) ?: "").filter { it.isNotEmpty() }
                } else {
                    nextWords
                }
            } else {
                val defaultStarters = DictionaryProvider.getNextWordPredictions("", limit = 3)
                if (defaultStarters.size >= 2) {
                    listOf(defaultStarters.getOrNull(1) ?: "", defaultStarters.getOrNull(0) ?: "", defaultStarters.getOrNull(2) ?: "").filter { it.isNotEmpty() }
                } else {
                    defaultStarters
                }
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
        updateSuggestions()
    }

    fun handleEnter() {
        onEnterAction()
        onRequestHaptic()
        updateSuggestions()
    }

    fun handleSuggestionTap(suggestion: String) {
        onSuggestionSelected(suggestion)
        onRequestHaptic()
        updateSuggestions()
    }

    fun handleEmojiInput(emoji: String) {
        onTextInput(emoji)
        onRequestHaptic()
        onEmojiUsed(emoji)
    }

    // Stable remembered callbacks to avoid recomposition ripples across keyboard layouts
    val onCharacterInputStable = remember(onTextInput, onRequestHaptic) {
        { text: String -> handleCharacterInput(text) }
    }
    val onBackspaceStable = remember(onDeleteBackward, onRequestHaptic) {
        { handleBackspace() }
    }
    val onSpaceStable = remember(onSpace, onRequestHaptic) {
        { handleSpace() }
    }
    val onEnterStable = remember(onEnterAction, onRequestHaptic) {
        { handleEnter() }
    }
    val onSuggestionTapStable = remember(onSuggestionSelected, onRequestHaptic) {
        { suggestion: String -> handleSuggestionTap(suggestion) }
    }
    val onEmojiTapStable = remember(onTextInput, onRequestHaptic, onEmojiUsed) {
        { emoji: String -> handleEmojiInput(emoji) }
    }
    val onSwitchToSymbolsStable = remember(onModeChange, onRequestHaptic) {
        {
            onModeChange(KeyboardMode.SYMBOLS_1)
            onRequestHaptic()
        }
    }
    val onSwitchToQwertyStable = remember(onModeChange, onRequestHaptic) {
        {
            onModeChange(KeyboardMode.QWERTY)
            onRequestHaptic()
        }
    }
    val onSwitchToEmojiStable = remember(onModeChange, onRequestHaptic) {
        {
            onModeChange(KeyboardMode.EMOJI)
            onRequestHaptic()
        }
    }
    val onSwitchToNumpadStable = remember(onModeChange, onRequestHaptic) {
        {
            onModeChange(KeyboardMode.NUMPAD)
            onRequestHaptic()
        }
    }
    val onSwitchToCalculatorStable = remember(onModeChange, onRequestHaptic) {
        {
            onModeChange(KeyboardMode.CALCULATOR)
            onRequestHaptic()
        }
    }
    val onClipboardTapStable = remember(onModeChange) {
        { onModeChange(KeyboardMode.CLIPBOARD) }
    }
    val onToggleSymbolPageStable = remember(mode, onModeChange, onRequestHaptic) {
        {
            onModeChange(if (mode == KeyboardMode.SYMBOLS_1) KeyboardMode.SYMBOLS_2 else KeyboardMode.SYMBOLS_1)
            onRequestHaptic()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        when (mode) {
            KeyboardMode.QWERTY, KeyboardMode.SYMBOLS_1, KeyboardMode.SYMBOLS_2, KeyboardMode.NUMPAD -> {
                // Suggestion bar (shown above keyboard for text input modes)
                SuggestionBar(
                    suggestions = suggestions,
                    onSuggestionTap = onSuggestionTapStable,
                    onClipboardTap = onClipboardTapStable,
                    onVoiceTap = onVoiceInput,
                    onCalculatorTap = onSwitchToCalculatorStable,
                    recentClipboardText = if (!suggestionsAllowed || detectedInputType == DetectedInputType.PASSWORD || detectedInputType == DetectedInputType.NUMBER_PASSWORD) null else clipboardSuggestion?.text,
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
                            detectedInputType = detectedInputType,
                            onCharacterInput = onCharacterInputStable,
                            onBackspace = onBackspaceStable,
                            onShiftToggle = onShiftToggle,
                            onShiftLock = onShiftLock,
                            onEnter = onEnterStable,
                            onSwitchToSymbols = onSwitchToSymbolsStable,
                            onSwitchToEmoji = onSwitchToEmojiStable,
                            onSpace = onSpaceStable
                        )
                    }
                    KeyboardMode.SYMBOLS_1, KeyboardMode.SYMBOLS_2 -> {
                        SymbolLayout(
                            isPage2 = mode == KeyboardMode.SYMBOLS_2,
                            enterLabel = enterLabel,
                            onCharacterInput = onCharacterInputStable,
                            onBackspace = onBackspaceStable,
                            onEnter = onEnterStable,
                            onSwitchToQwerty = onSwitchToQwertyStable,
                            onSwitchToEmoji = onSwitchToEmojiStable,
                            onSwitchToNumpad = onSwitchToNumpadStable,
                            onToggleSymbolPage = onToggleSymbolPageStable,
                            onSpace = onSpaceStable
                        )
                    }
                    KeyboardMode.NUMPAD -> {
                        NumpadLayout(
                            enterLabel = enterLabel,
                            detectedInputType = detectedInputType,
                            onCharacterInput = onCharacterInputStable,
                            onBackspace = onBackspaceStable,
                            onEnter = onEnterStable,
                            onSwitchToQwerty = onSwitchToQwertyStable,
                            onSwitchToSymbols = onSwitchToSymbolsStable,
                            onSwitchToCalculator = onSwitchToCalculatorStable
                        )
                    }
                    else -> { /* handled above */ }
                }
            }

            KeyboardMode.CALCULATOR -> {
                CalculatorPanel(
                    onInsertText = { resultText ->
                        onTextInput(resultText)
                    },
                    onBackToKeyboard = onSwitchToQwertyStable,
                    onRequestHaptic = onRequestHaptic
                )
            }

            KeyboardMode.EMOJI -> {
                EmojiPanel(
                    recentEmojis = recentEmojis,
                    onEmojiTap = onEmojiTapStable,
                    onBackToKeyboard = onSwitchToQwertyStable,
                    onBackspace = onBackspaceStable,
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
