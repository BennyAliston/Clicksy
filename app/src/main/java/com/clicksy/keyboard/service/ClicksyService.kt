package com.clicksy.keyboard.service

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Intent
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.clicksy.keyboard.data.ClipboardDatabase
import com.clicksy.keyboard.data.ClipboardRepository
import com.clicksy.keyboard.data.ClipboardManagerService
import com.clicksy.keyboard.ui.keyboard.ClipboardViewModel
import com.clicksy.keyboard.settings.SettingsActivity
import com.clicksy.keyboard.data.NeuTheme
import com.clicksy.keyboard.data.KeyShapeStyle
import com.clicksy.keyboard.data.CustomThemeConfig
import com.clicksy.keyboard.data.KeyboardSize
import com.clicksy.keyboard.data.ShiftState
import com.clicksy.keyboard.data.KeyboardMode
import com.clicksy.keyboard.data.DetectedInputType
import com.clicksy.keyboard.data.AutoCapsType
import com.clicksy.keyboard.data.DictionaryProvider
import com.clicksy.keyboard.ui.keyboard.KeyboardScreen
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyDimensions
import com.clicksy.keyboard.ui.theme.toColorScheme
import com.clicksy.keyboard.util.HapticFeedbackManager
import com.clicksy.keyboard.util.SpeechRecognizerHelper
import com.clicksy.keyboard.util.VoicePermissionActivity
import com.clicksy.keyboard.util.SoundFeedbackManager
import com.clicksy.keyboard.util.SoundType
import com.clicksy.keyboard.util.InputConfiguration
import com.clicksy.keyboard.util.InputTypeDetector

private val AUTO_CAPS_REGEX = Regex("(?:[\\.!?]|\\n)\\s+$")
private val WORD_SPLIT_REGEX = Regex("[\\s\\u00A0.,;:!?()\\[\\]{}\"<>/\\\\=+\\-*&^%$#@~`|\\uFFFC]+")

/**
 * The core InputMethodService for Clicksy.
 *
 * Hosts the Compose-based keyboard UI within the IME framework.
 * Manually implements LifecycleOwner, ViewModelStoreOwner, and
 * SavedStateRegistryOwner to support Jetpack Compose in a Service context.
 */
class ClicksyService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {


    // --- Lifecycle plumbing for Compose ---
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val _savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = _savedStateRegistryController.savedStateRegistry

    // --- Services ---
    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var speechHelper: SpeechRecognizerHelper
    private lateinit var soundManager: SoundFeedbackManager
    private lateinit var clipboardManagerService: ClipboardManagerService
    private val clipboardViewModel: ClipboardViewModel by lazy {
        val database = ClipboardDatabase.getDatabase(this)
        val repository = ClipboardRepository(database.clipboardDao())
        ViewModelProvider(this, ClipboardViewModel.Factory(repository, this))[ClipboardViewModel::class.java]
    }
    private lateinit var prefs: SharedPreferences

    // --- Reactive state ---
    private var currentTheme by mutableStateOf(NeuTheme.SUNSHINE)
    private var keyboardSize by mutableStateOf(KeyboardSize.MEDIUM)
    private var hapticEnabled by mutableStateOf(true)
    private var autocompleteEnabled by mutableStateOf(true)
    private var autocorrectEnabled by mutableStateOf(true)
    private var autoCapitalizationEnabled by mutableStateOf(true)
    private var showNumberRow by mutableStateOf(false)
    private var roundedKeysEnabled by mutableStateOf(false)
    private var keyShapeStyle by mutableStateOf(KeyShapeStyle.ROUNDED)
    private var keyCornerRadius by mutableStateOf(8f)
    private var borderWidth by mutableStateOf(3f)
    private var shadowOffset by mutableStateOf(4f)
    private var keySpacing by mutableStateOf(4f)
    private var showSecondarySymbols by mutableStateOf(true)
    private var customThemeConfig by mutableStateOf(CustomThemeConfig())
    private var soundType by mutableStateOf(SoundType.MUTE)
    private var activePackageName by mutableStateOf("com.clicksy.keyboard")
    private var shiftState by mutableStateOf(ShiftState.OFF)
    private var keyboardMode by mutableStateOf(KeyboardMode.QWERTY)
    private var detectedInputType by mutableStateOf(DetectedInputType.TEXT)
    private var suggestionsAllowed by mutableStateOf(true)
    private var currentInputConfig by mutableStateOf(InputConfiguration())
    private val recentEmojis = mutableStateListOf<String>()
    private var enterLabel by mutableStateOf("↵")
    private var lastAutoCorrectedOriginal: String? = null
    private var lastAutoCorrectedReplacement: String? = null
    private var lastSpaceTapTime = 0L
    private var isDeviceDarkTheme by mutableStateOf(false)
    private var isVoiceListening by mutableStateOf(false)
    private var voiceRmsDb by mutableStateOf(0f)
    private var voicePartialText by mutableStateOf("")
    private var voiceStatusMessage by mutableStateOf("")
    private var hasVoicePermission by mutableStateOf(true)
    private var cursorUpdateTrigger by mutableLongStateOf(0L)
    private var inputSessionId by mutableLongStateOf(0L)
    private var lastTypedWordBuffer = StringBuilder()
    private var lastCompletedWord = ""

    private lateinit var shiftStateManager: ShiftStateManager

    private var clipChangedListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadPreferences()
    }

    companion object {
        const val PREFS_NAME = "clicksy_prefs"
        const val KEY_THEME = "theme"
        const val KEY_KEYBOARD_SIZE = "keyboard_size"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_AUTOCOMPLETE = "autocomplete_enabled"
        const val KEY_AUTOCORRECT = "autocorrect_enabled"
        const val KEY_AUTO_CAPITALIZATION = "auto_capitalization_enabled"
        const val KEY_SHOW_NUMBER_ROW = "show_number_row"
        const val KEY_SOUND_TYPE = "sound_type"
        const val KEY_RECENT_EMOJIS = "recent_emojis"
        const val KEY_ROUNDED_KEYS = "rounded_keys_enabled"
        const val KEY_KEY_SHAPE_STYLE = "key_shape_style"
        const val KEY_KEY_CORNER_RADIUS = "key_corner_radius"
        const val KEY_BORDER_WIDTH = "border_width"
        const val KEY_SHADOW_OFFSET = "shadow_offset"
        const val KEY_KEY_SPACING = "key_spacing"
        const val KEY_SHOW_SECONDARY_SYMBOLS = "show_secondary_symbols"
        const val KEY_HINGLISH_ENABLED = "hinglish_enabled"
        const val KEY_LEARNING_ENABLED = "learning_enabled"
    }

    override fun onCreate() {
        super.onCreate()
        _savedStateRegistryController.performRestore(null)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        hapticManager = HapticFeedbackManager(this)
        speechHelper = SpeechRecognizerHelper(this)
        soundManager = SoundFeedbackManager(this)
        
        val database = ClipboardDatabase.getDatabase(this)
        val repository = ClipboardRepository(database.clipboardDao())
        clipboardManagerService = ClipboardManagerService(this, repository)
        clipboardManagerService.startListening()
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        loadPreferences()
        isDeviceDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        shiftStateManager = ShiftStateManager { newState ->
            shiftState = newState
        }
        loadRecentEmojis()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            DictionaryProvider.initialize(this@ClicksyService)
        }
    }

    override fun onCreateInputView(): View {
        // Ensure owners are set on the decor view BEFORE ComposeView is attached to prevent IllegalStateException
        // and allow WindowInsets to propagate to the view tree by disabling decor fits system windows
        window?.window?.let { win ->
            win.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
            val decorView = win.decorView
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            // Set layout parameters to ensure it matches parent width and wraps content height
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Keep the composition alive while the IME Service lives so reopening the keyboard has 0ms delay
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@ClicksyService))

            // Wire up lifecycle owners for Compose
            // CRITICAL: Must be set before setContent
            setViewTreeLifecycleOwner(this@ClicksyService)
            setViewTreeViewModelStoreOwner(this@ClicksyService)
            setViewTreeSavedStateRegistryOwner(this@ClicksyService)

            setContent {
                val dimensions = remember(
                    keyboardSize,
                    keyShapeStyle,
                    keyCornerRadius,
                    borderWidth,
                    shadowOffset,
                    keySpacing,
                    showSecondarySymbols
                ) {
                    val defaultDims = ClicksyDimensions(
                        borderWidth = borderWidth.dp,
                        shadowOffsetX = shadowOffset.dp,
                        shadowOffsetY = shadowOffset.dp,
                        keyCornerRadius = keyCornerRadius.dp,
                        keySpacing = keySpacing.dp,
                        keyShapeStyle = keyShapeStyle,
                        showSecondarySymbols = showSecondarySymbols
                    )
                    when (keyboardSize) {
                        KeyboardSize.SMALL -> defaultDims.copy(keyHeight = 42.dp)
                        KeyboardSize.MEDIUM -> defaultDims.copy(keyHeight = 50.dp)
                        KeyboardSize.LARGE -> defaultDims.copy(keyHeight = 58.dp)
                        KeyboardSize.EXTRA_LARGE -> defaultDims.copy(keyHeight = 65.dp)
                    }
                }
                val colorScheme = if (currentTheme == NeuTheme.CUSTOM) {
                    customThemeConfig.toColorScheme()
                } else {
                    currentTheme.toColorScheme(activePackageName = activePackageName, isDark = isDeviceDarkTheme)
                }
                ClicksyTheme(
                    colorScheme = colorScheme,
                    dimensions = dimensions
                ) {
                    KeyboardScreen(
                        recentEmojis = recentEmojis,
                        onEmojiUsed = { emoji ->
                            recentEmojis.remove(emoji)
                            recentEmojis.add(0, emoji)
                            if (recentEmojis.size > 30) {
                                recentEmojis.removeRange(30, recentEmojis.size)
                            }
                            saveRecentEmojis()
                        },
                        mode = keyboardMode,
                        onModeChange = { newMode ->
                            if (keyboardMode == KeyboardMode.VOICE_INPUT && newMode != KeyboardMode.VOICE_INPUT) {
                                stopVoiceInput(finishComposing = true)
                            }
                            keyboardMode = newMode
                        },
                        shiftState = shiftState,
                        onShiftToggle = {
                            shiftStateManager.handleShiftTap()
                        },
                        onShiftLock = {
                            shiftStateManager.handleShiftLongPress()
                        },
                        enterLabel = enterLabel,
                        clipboardViewModel = clipboardViewModel,
                        onLaunchSettings = {
                            val intent = Intent(this@ClicksyService, SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },
                        autocompleteEnabled = autocompleteEnabled,
                        autocorrectEnabled = autocorrectEnabled,
                        keyboardSize = keyboardSize,
                        showNumberRow = showNumberRow,
                        detectedInputType = detectedInputType,
                        suggestionsAllowed = suggestionsAllowed,
                        cursorUpdateTrigger = cursorUpdateTrigger,
                        inputSessionId = inputSessionId,
                        onTextInput = { text ->
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            val ic = currentInputConnection

                            // Update local word buffer
                            if (text.length == 1 && (text[0].isLetterOrDigit() || text[0] == '\'')) {
                                lastTypedWordBuffer.append(text)
                            } else if (text == " " || text == "\n" || text.any { !it.isLetterOrDigit() && it != '\'' }) {
                                if (lastTypedWordBuffer.isNotEmpty()) {
                                    lastCompletedWord = lastTypedWordBuffer.toString()
                                }
                                lastTypedWordBuffer.clear()
                            }

                            // Smart Punctuation: if user types a punctuation mark immediately after a space, snap it: "hello " + "." -> "hello. "
                            // Only apply in standard text mode
                            if (detectedInputType == DetectedInputType.TEXT) {
                                if (text.length == 1 && (text == "." || text == "," || text == "!" || text == "?" || text == ";" || text == ":")) {
                                    val textBefore = ic?.getTextBeforeCursor(1, 0)?.toString() ?: ""
                                    if (textBefore == " ") {
                                        ic?.deleteSurroundingText(1, 0)
                                        ic?.commitText("$text ", 1)
                                        if (text == "." || text == "!" || text == "?") {
                                            shiftStateManager.forceState(ShiftState.ONCE)
                                        }
                                        return@KeyboardScreen
                                    }
                                }
                            }

                            // If it's a suggestion selection, it ends with a space (learn word if eligible)
                            if (text.endsWith(" ") && suggestionsAllowed) {
                                val word = text.trim()
                                learnWordIfEligible(word)
                                lastCompletedWord = word
                                lastTypedWordBuffer.clear()
                            }
                            ic?.commitText(text, 1)
                            shiftStateManager.handleCharacterInput(text)
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onDeleteBackward = {
                            if (lastTypedWordBuffer.isNotEmpty()) {
                                lastTypedWordBuffer.deleteCharAt(lastTypedWordBuffer.length - 1)
                            }
                            val ic = currentInputConnection
                            if (ic != null) {
                                // Check if we should undo the previous auto-correction
                                val orig = lastAutoCorrectedOriginal
                                val repl = lastAutoCorrectedReplacement
                                if (orig != null && repl != null) {
                                    val textBefore = ic.getTextBeforeCursor(repl.length + 1, 0)?.toString() ?: ""
                                    if (textBefore == "$repl ") {
                                        ic.deleteSurroundingText(repl.length + 1, 0)
                                        ic.commitText(orig, 1)
                                        DictionaryProvider.learnWord(this@ClicksyService, orig)
                                        lastAutoCorrectedOriginal = null
                                        lastAutoCorrectedReplacement = null
                                        lastTypedWordBuffer.clear()
                                        lastTypedWordBuffer.append(orig)
                                        shiftStateManager.resetDoubleTapTimer()
                                        return@KeyboardScreen
                                    }
                                }
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null

                                val selectedText = ic.getSelectedText(0)
                                if (!selectedText.isNullOrEmpty()) {
                                    ic.commitText("", 1)
                                    lastTypedWordBuffer.clear()
                                } else {
                                    val beforeText = ic.getTextBeforeCursor(2, 0)
                                    if (beforeText != null && beforeText.length >= 2 &&
                                        Character.isHighSurrogate(beforeText[beforeText.length - 2]) &&
                                        Character.isLowSurrogate(beforeText[beforeText.length - 1])
                                    ) {
                                        ic.deleteSurroundingText(2, 0)
                                    } else {
                                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                                    }
                                }
                            }
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onEnterAction = {
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            if (lastTypedWordBuffer.isNotEmpty()) {
                                learnWordIfEligible(lastTypedWordBuffer.toString())
                                lastCompletedWord = lastTypedWordBuffer.toString()
                                lastTypedWordBuffer.clear()
                            } else {
                                learnLastTypedWord()
                            }
                            handleEnterAction()
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onSpace = {
                            val currentTime = System.currentTimeMillis()
                            val delta = currentTime - lastSpaceTapTime
                            val ic = currentInputConnection
                            val rawBefore = ic?.getTextBeforeCursor(8, 0)?.toString() ?: ""
                            val textBefore = cleanText(rawBefore)

                            // Double-tap Space shortcut: converts "  " into ". " and triggers Auto-Caps (only for standard text)
                            if (detectedInputType == DetectedInputType.TEXT && delta in 60..500 && textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                                ic?.beginBatchEdit()
                                try {
                                    val deleteCount = calculateDeleteCount(rawBefore, " ")
                                    ic.deleteSurroundingText(deleteCount, 0)
                                    ic.commitText(". ", 1)
                                } finally {
                                    ic?.endBatchEdit()
                                }
                                lastSpaceTapTime = 0L
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null
                                lastTypedWordBuffer.clear()
                                shiftStateManager.forceState(ShiftState.ONCE)
                                return@KeyboardScreen
                            }
                            lastSpaceTapTime = currentTime

                            val (currFromCursor, prevWord, prevPrevWord) = getCursorContext()
                            val currentWord = if (currFromCursor.isNotBlank()) currFromCursor else lastTypedWordBuffer.toString()

                            val isPassword = isPasswordField(currentInputEditorInfo)
                            val isEligibleForAutoCorrect = autocorrectEnabled && currentInputConfig.autocorrectAllowed && !isPassword && when (detectedInputType) {
                                DetectedInputType.TEXT -> true
                                DetectedInputType.URI -> false
                                DetectedInputType.EMAIL -> false
                                else -> false
                            }

                            if (isEligibleForAutoCorrect && currentWord.isNotBlank()) {
                                val correction = DictionaryProvider.getAutoCorrection(currentWord, prevWord, prevPrevWord)
                                if (correction != null && !correction.equals(currentWord, ignoreCase = false)) {
                                    if (ic != null) {
                                        ic.beginBatchEdit()
                                        try {
                                            val deleteCount = calculateDeleteCount(rawBefore, currentWord)
                                            ic.deleteSurroundingText(deleteCount, 0)
                                            ic.commitText("$correction ", 1)
                                        } finally {
                                            ic.endBatchEdit()
                                        }
                                        lastAutoCorrectedOriginal = currentWord
                                        lastAutoCorrectedReplacement = correction
                                        learnWordIfEligible(correction, prevWord, prevPrevWord)
                                        lastCompletedWord = correction
                                    }
                                } else {
                                    learnWordIfEligible(currentWord, prevWord, prevPrevWord)
                                    ic?.commitText(" ", 1)
                                    lastAutoCorrectedOriginal = null
                                    lastAutoCorrectedReplacement = null
                                    lastCompletedWord = currentWord
                                }
                            } else {
                                learnWordIfEligible(currentWord, prevWord, prevPrevWord)
                                ic?.commitText(" ", 1)
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null
                                if (currentWord.isNotBlank()) {
                                    lastCompletedWord = currentWord
                                }
                            }
                            lastTypedWordBuffer.clear()
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onRequestHaptic = {
                            if (hapticEnabled) {
                                hapticManager.performKeyTick()
                            }
                            soundManager.playSound(soundType)
                        },
                        onVoiceInput = {
                            handleVoiceInputTrigger()
                        },
                        isVoiceListening = isVoiceListening,
                        voiceRmsDb = voiceRmsDb,
                        voicePartialText = voicePartialText,
                        voiceStatusMessage = voiceStatusMessage,
                        hasVoicePermission = hasVoicePermission,
                        onVoiceToggleListening = {
                            toggleVoiceListening()
                        },
                        onRequestVoicePermission = {
                            requestVoicePermission()
                        },
                        onSuggestionSelected = { suggestion ->
                            val ic = currentInputConnection
                            if (ic != null) {
                                val (word, prevWord, prevPrevWord) = getCursorContext()
                                val wordToDelete = if (word.isNotEmpty()) word else lastTypedWordBuffer.toString()
                                val cleaned = suggestion.removeSurrounding("\"")

                                ic.beginBatchEdit()
                                try {
                                    val rawBefore = ic.getTextBeforeCursor(wordToDelete.length + 8, 0)?.toString() ?: ""
                                    val deleteCount = if (wordToDelete.isNotEmpty()) calculateDeleteCount(rawBefore, wordToDelete) else 0

                                    if (detectedInputType == DetectedInputType.EMAIL) {
                                        if (cleaned.startsWith("@")) {
                                            if (wordToDelete.contains("@")) {
                                                val username = wordToDelete.substringBefore("@")
                                                if (deleteCount > 0) {
                                                    ic.deleteSurroundingText(deleteCount, 0)
                                                }
                                                ic.commitText("$username$cleaned", 1)
                                            } else {
                                                ic.commitText(cleaned, 1)
                                            }
                                        } else {
                                            if (deleteCount > 0) {
                                                ic.deleteSurroundingText(deleteCount, 0)
                                            }
                                            ic.commitText(cleaned, 1)
                                        }
                                    } else if (detectedInputType == DetectedInputType.URI) {
                                        if (wordToDelete.isNotEmpty() && cleaned.startsWith(".")) {
                                            ic.commitText(cleaned, 1)
                                        } else {
                                            if (deleteCount > 0) {
                                                ic.deleteSurroundingText(deleteCount, 0)
                                            }
                                            ic.commitText("$cleaned ", 1)
                                            learnWordIfEligible(cleaned, prevWord, prevPrevWord)
                                        }
                                    } else {
                                        if (deleteCount > 0) {
                                            ic.deleteSurroundingText(deleteCount, 0)
                                        }
                                        ic.commitText("$cleaned ", 1)
                                        learnWordIfEligible(cleaned, prevWord, prevPrevWord)
                                    }
                                } finally {
                                    ic.endBatchEdit()
                                }
                            }
                            lastCompletedWord = suggestion.removeSurrounding("\"")
                            lastTypedWordBuffer.clear()
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            shiftStateManager.handleCharacterInput(" ")
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        getCurrentWord = {
                            getCurrentWordBeforeCursor()
                        },
                        getPreviousWord = {
                            getPreviousWordBeforeCursor()
                        },
                        getCursorContext = ::getCursorContext
                    )
                }
            }
        }

        return composeView
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Ensure owners are set on the decor view so they are found during traversal
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (isVoiceListening) {
            stopVoiceInput(finishComposing = true)
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputSessionId = System.currentTimeMillis()
        logInputMetadata("onStartInput", attribute)
        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.updateActiveEditorInfo(attribute)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputSessionId = System.currentTimeMillis()
        currentInputConfig = InputTypeDetector.detect(info)
        detectedInputType = currentInputConfig.detectedType
        suggestionsAllowed = currentInputConfig.suggestionsAllowed
        enterLabel = currentInputConfig.enterLabel
        logInputMetadata("onStartInputView", info)

        if (!restarting) {
            lastTypedWordBuffer.clear()
            val initialWord = getCurrentWordBeforeCursor()
            if (initialWord.isNotEmpty()) {
                lastTypedWordBuffer.append(initialWord)
            }
            lastCompletedWord = ""
            lastAutoCorrectedOriginal = null
            lastAutoCorrectedReplacement = null
        }
        loadPreferences()
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isDeviceDarkTheme != nightMode) {
            isDeviceDarkTheme = nightMode
        }
        activePackageName = info?.packageName ?: "com.clicksy.keyboard"

        if (keyboardMode == KeyboardMode.VOICE_INPUT) {
            stopVoiceInput(finishComposing = true)
            keyboardMode = currentInputConfig.initialMode
        } else if (!restarting || keyboardMode == KeyboardMode.QWERTY || keyboardMode == KeyboardMode.NUMPAD) {
            keyboardMode = currentInputConfig.initialMode
        }

        shiftState = ShiftState.OFF
        shiftStateManager.forceState(ShiftState.OFF)
        checkAutoCaps()

        cursorUpdateTrigger = System.currentTimeMillis()

        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.updateActiveEditorInfo(info)
            clipboardManagerService.processCurrentClipboard()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        inputSessionId = System.currentTimeMillis()
        logInputMetadata("onFinishInputView", currentInputEditorInfo)
        lastTypedWordBuffer.clear()
        lastCompletedWord = ""
        lastAutoCorrectedOriginal = null
        lastAutoCorrectedReplacement = null
        if (isVoiceListening) {
            stopVoiceInput(finishComposing = true)
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        inputSessionId = System.currentTimeMillis()
        logInputMetadata("onFinishInput", currentInputEditorInfo)
        lastTypedWordBuffer.clear()
        lastCompletedWord = ""
        lastAutoCorrectedOriginal = null
        lastAutoCorrectedReplacement = null
    }

    private fun logInputMetadata(event: String, info: EditorInfo?) {
        if (info == null) {
            android.util.Log.d("ClicksyIME", "[$event] info=null")
            return
        }
        val pkg = info.packageName ?: "unknown"
        val inputType = info.inputType
        val maskClass = inputType and android.text.InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        val maskFlags = inputType and android.text.InputType.TYPE_MASK_FLAGS
        val imeAction = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val noSuggestions = (maskFlags and android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val multiLine = (maskFlags and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        android.util.Log.d(
            "ClicksyIME",
            "[$event] pkg=$pkg, type=0x${Integer.toHexString(inputType)} (class=$maskClass, var=0x${Integer.toHexString(maskVariation)}, flags=0x${Integer.toHexString(maskFlags)}, noSugg=$noSuggestions, multiLine=$multiLine), action=$imeAction, config=(detected=${currentInputConfig.detectedType}, suggAllowed=${currentInputConfig.suggestionsAllowed}, autoCorrectAllowed=${currentInputConfig.autocorrectAllowed})"
        )
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        val isTypingAdvance = oldSelStart != -1 && (newSelStart == oldSelStart + 1 || (newSelStart > oldSelStart && newSelStart - oldSelStart <= 2))

        if (!isTypingAdvance) {
            val textBefore = cleanText(currentInputConnection?.getTextBeforeCursor(2, 0)?.toString())
            if (textBefore.endsWith(" ") || textBefore.endsWith("\n")) {
                lastTypedWordBuffer.clear()
            } else {
                val cursorWord = getCurrentWordBeforeCursor()
                if (cursorWord.isNotEmpty()) {
                    lastTypedWordBuffer.clear()
                    lastTypedWordBuffer.append(cursorWord)
                }
            }
        } else {
            val cursorWord = getCurrentWordBeforeCursor()
            if (cursorWord.isNotEmpty()) {
                lastTypedWordBuffer.clear()
                lastTypedWordBuffer.append(cursorWord)
            }
        }

        cursorUpdateTrigger = System.currentTimeMillis()
        if (shiftState == ShiftState.OFF) {
            checkAutoCaps()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val nightMode = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isDeviceDarkTheme != nightMode) {
            isDeviceDarkTheme = nightMode
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.stopListening()
        }
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
        if (_lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        speechHelper.destroy()
        _viewModelStore.clear()
    }

    // --- Private helpers ---

    private fun isPasswordField(info: EditorInfo?): Boolean {
        if (info == null) return false
        return InputTypeDetector.isPasswordField(info)
    }

    private fun isPasswordField(inputType: Int): Boolean {
        val info = currentInputEditorInfo
        if (info != null && (info.inputType == inputType || inputType == 0)) {
            return isPasswordField(info)
        }
        return InputTypeDetector.isPasswordField(inputType)
    }

    private fun isZeroWidthOrInvisible(c: Char): Boolean {
        return c == '\u200B' || // Zero-width space
            c == '\u200C' || // Zero-width non-joiner
            c == '\u200D' || // Zero-width joiner
            c == '\uFEFF' || // Zero-width no-break space / BOM
            c == '\uFFFC' || // Object replacement character
            c == '\u200E' || // Left-to-right mark
            c == '\u200F' || // Right-to-left mark
            c == '\u00AD'    // Soft hyphen
    }

    private fun cleanText(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (c in text) {
            when {
                c == '\u00A0' || c == '\u2007' || c == '\u202F' -> sb.append(' ')
                isZeroWidthOrInvisible(c) -> { /* ignore zero-width char */ }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun calculateDeleteCount(rawBefore: String, word: String): Int {
        if (rawBefore.endsWith(word)) return word.length
        val rawChars = rawBefore.toCharArray()
        var matchedInWord = word.length - 1
        var count = 0
        for (i in rawChars.indices.reversed()) {
            val c = rawChars[i]
            count++
            if (matchedInWord >= 0 && c.equals(word[matchedInWord], ignoreCase = true)) {
                matchedInWord--
                if (matchedInWord < 0) {
                    var lookback = i - 1
                    while (lookback >= 0 && isZeroWidthOrInvisible(rawChars[lookback])) {
                        count++
                        lookback--
                    }
                    return count
                }
            }
        }
        return word.length
    }

    private fun shouldAutoCapitalize(textBeforeCursor: String): Boolean {
        if (textBeforeCursor.isEmpty()) return true
        if (textBeforeCursor.endsWith("\n") || textBeforeCursor.endsWith("\r")) return true
        return AUTO_CAPS_REGEX.containsMatchIn(textBeforeCursor)
    }

    private fun checkAutoCaps() {
        val ic = currentInputConnection ?: return
        if (shiftState == ShiftState.CAPS_LOCK) return

        val info = currentInputEditorInfo
        val inputType = info?.inputType ?: 0

        if (isPasswordField(info)) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        if (!autoCapitalizationEnabled) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        val config = currentInputConfig
        if (config.autoCapsType == AutoCapsType.NONE) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        if (config.autoCapsType == AutoCapsType.CHARACTERS) {
            shiftStateManager.forceState(ShiftState.CAPS_LOCK)
            return
        }

        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""

        if (config.autoCapsType == AutoCapsType.WORDS) {
            if (textBefore.isEmpty() || textBefore.last().isWhitespace() || textBefore.endsWith("\n")) {
                shiftStateManager.forceState(ShiftState.ONCE)
            } else {
                shiftStateManager.forceState(ShiftState.OFF)
            }
            return
        }

        // SENTENCES
        if (shouldAutoCapitalize(textBefore)) {
            shiftStateManager.forceState(ShiftState.ONCE)
        } else {
            shiftStateManager.forceState(ShiftState.OFF)
        }
    }

    private fun loadRecentEmojis() {
        val serialized = prefs.getString(KEY_RECENT_EMOJIS, "") ?: ""
        recentEmojis.clear()
        if (serialized.isNotEmpty()) {
            recentEmojis.addAll(serialized.split(","))
        }
    }

    private fun saveRecentEmojis() {
        val serialized = recentEmojis.joinToString(",")
        prefs.edit().putString(KEY_RECENT_EMOJIS, serialized).apply()
    }

    private fun loadPreferences() {
        val themeName = prefs.getString(KEY_THEME, NeuTheme.SUNSHINE.name)
        currentTheme = try {
            NeuTheme.valueOf(themeName ?: NeuTheme.SUNSHINE.name)
        } catch (e: Exception) {
            NeuTheme.SUNSHINE
        }
        val sizeName = prefs.getString(KEY_KEYBOARD_SIZE, KeyboardSize.MEDIUM.name)
        keyboardSize = try {
            KeyboardSize.valueOf(sizeName ?: KeyboardSize.MEDIUM.name)
        } catch (e: Exception) {
            KeyboardSize.MEDIUM
        }
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true)
        autocompleteEnabled = prefs.getBoolean(KEY_AUTOCOMPLETE, true)
        autocorrectEnabled = prefs.getBoolean(KEY_AUTOCORRECT, true)
        autoCapitalizationEnabled = prefs.getBoolean(KEY_AUTO_CAPITALIZATION, true)
        showNumberRow = prefs.getBoolean(KEY_SHOW_NUMBER_ROW, false)
        roundedKeysEnabled = prefs.getBoolean(KEY_ROUNDED_KEYS, false)

        val shapeName = prefs.getString(KEY_KEY_SHAPE_STYLE, null)
        keyShapeStyle = if (shapeName != null) {
            try { KeyShapeStyle.valueOf(shapeName) } catch (e: Exception) { KeyShapeStyle.ROUNDED }
        } else {
            if (roundedKeysEnabled) KeyShapeStyle.SOFT else KeyShapeStyle.ROUNDED
        }

        keyCornerRadius = prefs.getFloat(KEY_KEY_CORNER_RADIUS, if (keyShapeStyle == KeyShapeStyle.SOFT) 14f else 8f)
        borderWidth = prefs.getFloat(KEY_BORDER_WIDTH, 3f)
        shadowOffset = prefs.getFloat(KEY_SHADOW_OFFSET, 4f)
        keySpacing = prefs.getFloat(KEY_KEY_SPACING, 4f)
        showSecondarySymbols = prefs.getBoolean(KEY_SHOW_SECONDARY_SYMBOLS, true)
        DictionaryProvider.isHinglishEnabled = prefs.getBoolean(KEY_HINGLISH_ENABLED, true)
        DictionaryProvider.isLearningEnabled = prefs.getBoolean(KEY_LEARNING_ENABLED, true)
        customThemeConfig = CustomThemeConfig.loadFromPrefs(prefs)
        
        val soundName = prefs.getString(KEY_SOUND_TYPE, SoundType.MUTE.name) ?: SoundType.MUTE.name
        soundType = try {
            SoundType.valueOf(soundName)
        } catch (e: Exception) {
            SoundType.MUTE
        }
    }

    private fun updateEnterLabel(info: EditorInfo?) {
        enterLabel = InputTypeDetector.getEnterLabel(info)
    }

    private fun handleEnterAction() {
        val info = currentInputEditorInfo
        val actionId = currentInputConfig.actionId

        if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED &&
            actionId != EditorInfo.IME_ACTION_NONE
        ) {
            currentInputConnection?.performEditorAction(actionId)
        } else {
            val isMultiline = ((info?.inputType ?: 0) and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            if (isMultiline) {
                currentInputConnection?.commitText("\n", 1)
            } else {
                val performed = currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE) == true
                if (!performed) {
                    currentInputConnection?.commitText("\n", 1)
                }
            }
        }
    }

    private fun getCursorContext(): Triple<String, String, String> {
        val rawTextBefore = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString()

        val textBefore = cleanText(rawTextBefore)

        if (textBefore.isEmpty()) {
            val bufferedWord = lastTypedWordBuffer.toString()
            return Triple(bufferedWord, lastCompletedWord, "")
        }

        val isSentenceTerminated = textBefore.endsWith(". ") || textBefore.endsWith("? ") || textBefore.endsWith("! ") || textBefore.endsWith("\n")
        val isEndsWithSpace = textBefore.endsWith(" ") || textBefore.endsWith("\n") || textBefore.endsWith("\t")
        val words = textBefore.trim().split(WORD_SPLIT_REGEX).filter { it.isNotBlank() }

        var currentWord = if (isEndsWithSpace) "" else (words.lastOrNull() ?: "")
        if (currentWord.isEmpty() && lastTypedWordBuffer.isNotEmpty()) {
            currentWord = lastTypedWordBuffer.toString()
        }

        val previousWord = if (isSentenceTerminated) {
            ""
        } else if (isEndsWithSpace) {
            words.lastOrNull() ?: lastCompletedWord
        } else {
            if (words.size >= 2) words[words.size - 2] else lastCompletedWord
        }
        val prevPrevWord = if (isSentenceTerminated) {
            ""
        } else if (isEndsWithSpace) {
            if (words.size >= 2) words[words.size - 2] else ""
        } else {
            if (words.size >= 3) words[words.size - 3] else ""
        }

        return Triple(currentWord, previousWord, prevPrevWord)
    }

    private fun getPreviousWordBeforeCursor(): String {
        val rawTextBefore = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString()

        val textBefore = cleanText(rawTextBefore)
        if (textBefore.isEmpty()) return lastCompletedWord

        val words = textBefore.trim().split(WORD_SPLIT_REGEX)
            .filter { it.isNotBlank() }

        return if (words.size >= 2) words[words.size - 2] else if (words.size == 1 && textBefore.endsWith(" ")) words[0] else lastCompletedWord
    }

    private fun getCurrentWordBeforeCursor(): String {
        val rawTextBefore = currentInputConnection
            ?.getTextBeforeCursor(50, 0)
            ?.toString()

        val textBefore = cleanText(rawTextBefore)

        if (textBefore.isNotEmpty()) {
            if (textBefore.endsWith(" ") || textBefore.endsWith("\n")) {
                return if (lastTypedWordBuffer.isNotEmpty()) lastTypedWordBuffer.toString() else ""
            }
            val word = textBefore.split(WORD_SPLIT_REGEX)
                .lastOrNull()
                ?.takeIf { it.isNotBlank() }
            if (!word.isNullOrEmpty()) return word
        }

        return lastTypedWordBuffer.toString()
    }

    private fun learnWordIfEligible(word: String, prevWord: String = "", prevPrevWord: String = "") {
        val isPassword = isPasswordField(currentInputEditorInfo)
        if (isPassword || word.isBlank()) return
        val eligible = when (detectedInputType) {
            DetectedInputType.TEXT -> true
            DetectedInputType.URI -> !word.contains(".") && !word.startsWith("http") && !word.startsWith("www.")
            DetectedInputType.EMAIL -> !word.contains("@")
            else -> false
        }
        if (eligible) {
            DictionaryProvider.learnWord(this, word)
            if (prevWord.isNotBlank()) {
                DictionaryProvider.learnWordSequence(this, prevWord, word)
            }
            if (prevPrevWord.isNotBlank() && prevWord.isNotBlank()) {
                DictionaryProvider.learnTrigram(this, prevPrevWord, prevWord, word)
            }
        }
    }

    private fun learnLastTypedWord() {
        val isPassword = isPasswordField(currentInputEditorInfo)
        if (isPassword) return
        val rawTextBefore = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString()

        val textBefore = cleanText(rawTextBefore)

        if (textBefore.isNotEmpty()) {
            val words = textBefore.trim().split(WORD_SPLIT_REGEX)
                .filter { it.isNotBlank() }

            if (words.isNotEmpty()) {
                val lastWord = words.last()
                val prevWord = if (words.size >= 2) words[words.size - 2] else ""
                val prevPrevWord = if (words.size >= 3) words[words.size - 3] else ""
                learnWordIfEligible(lastWord, prevWord, prevPrevWord)
                return
            }
        }

        if (lastTypedWordBuffer.isNotEmpty()) {
            learnWordIfEligible(lastTypedWordBuffer.toString(), lastCompletedWord, "")
        } else if (lastCompletedWord.isNotEmpty()) {
            learnWordIfEligible(lastCompletedWord)
        }
    }

    private fun handleVoiceInputTrigger() {
        hasVoicePermission = speechHelper.hasPermission()
        keyboardMode = KeyboardMode.VOICE_INPUT
        if (!hasVoicePermission) {
            voiceStatusMessage = "Microphone permission required"
            requestVoicePermission()
        } else {
            startSpeechRecognition()
        }
    }

    private fun requestVoicePermission() {
        VoicePermissionActivity.requestPermission(
            context = this,
            onGranted = {
                hasVoicePermission = true
                voiceStatusMessage = "Permission granted"
                startSpeechRecognition()
            },
            onDenied = {
                hasVoicePermission = false
                voiceStatusMessage = "Microphone permission denied"
            }
        )
    }

    private fun startSpeechRecognition() {
        if (!speechHelper.isRecognitionAvailable()) {
            voiceStatusMessage = "Speech recognition unavailable on this device"
            return
        }

        isVoiceListening = true
        voicePartialText = ""
        voiceRmsDb = 0f
        voiceStatusMessage = "Listening..."

        speechHelper.startListening(object : SpeechRecognizerHelper.VoiceInputCallback {
            override fun onListeningStarted() {
                isVoiceListening = true
                voiceStatusMessage = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                voiceRmsDb = rmsdB
            }

            override fun onResult(text: String, isFinal: Boolean) {
                if (isFinal) {
                    isVoiceListening = false
                    voicePartialText = ""
                    voiceRmsDb = 0f
                    voiceStatusMessage = "Tap mic to speak"

                    val ic = currentInputConnection
                    if (ic != null && text.isNotBlank()) {
                        val textBefore = ic.getTextBeforeCursor(1, 0)
                        val needsLeadingSpace = !textBefore.isNullOrEmpty() && !textBefore.endsWith(" ") && !textBefore.endsWith("\n")
                        val commitStr = if (needsLeadingSpace) " $text " else "$text "
                        ic.commitText(commitStr, 1)
                    }
                } else {
                    voicePartialText = text
                    currentInputConnection?.setComposingText(text, 1)
                }
            }

            override fun onError(errorMessage: String) {
                isVoiceListening = false
                voiceRmsDb = 0f
                voiceStatusMessage = errorMessage
                currentInputConnection?.finishComposingText()
            }

            override fun onListeningStopped() {
                isVoiceListening = false
                voiceRmsDb = 0f
            }
        })
    }

    private fun toggleVoiceListening() {
        if (isVoiceListening) {
            stopVoiceInput(finishComposing = true)
        } else {
            if (!speechHelper.hasPermission()) {
                requestVoicePermission()
            } else {
                startSpeechRecognition()
            }
        }
    }

    private fun stopVoiceInput(finishComposing: Boolean = false) {
        isVoiceListening = false
        voiceRmsDb = 0f
        speechHelper.stopListening()
        if (finishComposing) {
            currentInputConnection?.finishComposingText()
            voicePartialText = ""
        }
    }
}
