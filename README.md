# ⌨️ Clicksy Keyboard

[![Version](https://img.shields.io/badge/Version-0.3.0--beta-purple.svg)](https://github.com/BennyAliston/Clicksy)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy_Me_A_Coffee-Support-yellow.svg?logo=buy-me-a-coffee)](https://buymeacoffee.com/bennyaliston)
[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.x-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-latest-orange.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 💡 What is Clicksy?

**Clicksy** is an open-source, ultra-responsive Android keyboard built from the ground up using **Kotlin** and **Jetpack Compose**. Unlike traditional OEM keyboards that rely on flat, generic design patterns and bloated cloud dependencies, Clicksy is engineered around four core pillars:

1. **🎨 Bold Neubrutalist Design System:** Distinctive high-contrast palettes, crisp solid borders, hard offset shadows, 100% opaque layers, and tactile, physical keycaps with zero application window bleed-through.
2. **⚡ 60/120 FPS Zero-Latency Performance:** Instant 0ms touch response, GPU Canvas rendering, lifecycle-cached composition trees, non-blocking asynchronous startup, and sub-0.3ms bucketed word searches for fluid typing without GC stutter.
3. **🌐 Offline-First Intelligence:** Unified 36,000+ entry English + Hinglish dictionary, on-device contextual n-gram predictions, phonetics-aware typo correction, zero external network dependencies, and strict local privacy.
4. **🛠️ Built-in Productivity Power Tools:** Voice typing with real-time decibel visualizer, custom theme studio, instant emoji search, integrated math calculator with 1-tap paste, dedicated 5×4 Numpad, and a Room-backed clipboard manager.

> 💡 **A solo open-source project by BennyAliston** • ☕ **[Buy Me a Coffee](https://buymeacoffee.com/bennyaliston)**

---

## 📸 Previews

### 🖼️ Screenshots

#### Themes
| Sunshine Theme | Bubblegum Theme | Minty Theme | Adaptive Theme |
| :---: | :---: | :---: | :---: |
| ![Sunshine Theme](https://github.com/user-attachments/assets/7162d599-67e9-4e4e-80a7-c5aedb120a04) | ![Bubblegum Theme](https://github.com/user-attachments/assets/c9970f7f-5170-4637-b0fd-514fe806b651) | ![Minty Theme](https://github.com/user-attachments/assets/c3480016-7b2b-45be-8425-b6a5ed1b67c7) | ![Dark Theme](https://github.com/user-attachments/assets/6a00494a-ae74-409f-b7be-7b2ef9377d5a) |

#### Features & Layouts
| Rounded Keys | Emojis | Clipboard & Settings |
| :---: | :---: | :---: |
| ![Rounded Keys](https://github.com/user-attachments/assets/d84a2ce1-4a8b-4691-9e94-97bcf2431efe) | ![Emojis](https://github.com/user-attachments/assets/aa0c92cd-d1fa-4c05-be82-175b92de11ac) | ![Clipboard](https://github.com/user-attachments/assets/ec2195b1-5da5-4a3a-9bc3-6367febd12cb) |

| Inbuilt Calculator | Numpad | Symbols |
| :---: | :---: | :---: |
| ![Inbuilt Calculator](https://github.com/user-attachments/assets/fa1787a9-7017-44d0-a29f-71f1af81c115) | ![Numpad](https://github.com/user-attachments/assets/a63707ba-8a8e-4417-8eff-e9285c9c70df) | ![Symbols](https://github.com/user-attachments/assets/9007f558-1fe1-41fb-8bcd-450020486835) |

---

## ✨ Key Features

- **🎨 Neubrutalist Aesthetics:** Bold typography, solid high-contrast borders, tactile offset shadows, and 100% opaque layers that make keys feel physical and delightful to tap.
- **🌐 Unified English + Hinglish Engine:** 36,000+ curated entries combining standard English with Romanized Hindi (Hinglish), 53,000+ transliteration variants from Google Dakshina, and natural chat abbreviations from OpenHinglish.
- **🎙️ Voice Typing with Live Visualizer:** Dedicated full-height Neubrutalist speech input panel with real-time waveform animating to microphone RMS decibel levels.
- **🎨 Custom Theme Studio & Handcrafted Palettes:** Personalize every color token (keycaps, borders, shadows, highlights) or switch between presets (*Cyberpunk*, *Lavender*, *Forest*, *Retro*, *Sunset*, *Monochrome*, *Sunshine*, *Bubblegum*, *Minty*).
- **🔍 Instant Emoji Search & Discovery Chips:** Fast keyword search across hundreds of emojis with popular one-tap discovery chips (*Smile*, *Love*, *Fire*, *Party*, *Food*, *Coffee*, etc.).
- **📋 Advanced Clipboard Manager:** Room-backed clipboard manager with real-time search, pinned items, swipe actions, tracking parameter cleaner (strips UTM tags), and smart privacy filters that mask OTPs, credit cards, and API keys.
- **🎯 Dynamic Input Adaptation:** Intelligently configures the keyboard for the active input field—launching the Numpad for numbers/PINs/dates, adding `@` and `.com` shortcuts for emails, adding `/` for web URLs, and disabling suggestions automatically on password fields for privacy.
- **🔣 Long-Press Secondary Symbols:** Access punctuation and special characters directly from letter keys with a simple long-press, backed by responsive haptics and a dedicated number row.
- **🧮 Built-in Calculator:** Perform arithmetic calculations right from your keyboard and insert results directly into any app with a single tap of **↵ Paste**.
- **🔢 Dedicated 5×4 Numpad:** Symmetrical number keypad tailored for uninterrupted numeric input, dialing, and rapid calculations.
- **💡 Smart Suggestion & Learning Engine:** Dynamic vocabulary learning that adapts to your style, offering contextual next-word predictions, auto-capitalization, typo correction, contraction expansions, and inline emoji suggestions.
- **🌈 Dynamic Material You Theming:** Automatically adapts to wallpaper colors on Android 12+ devices with seamless dark/light mode switching.
- **🔊 Tactile Audio & Haptics:** Zero-latency keypress vibrations and satisfying sound profiles (*System*, *Bubble*, *Woodblock*, *Typewriter*, and *Chime*).
- **⚡ 60 / 120 FPS Fluid Typing:** Native GPU-accelerated Compose rendering and memory caching for instant keyboard popups and stutter-free typing.

---

## 🛠️ Architecture & Core Components

- **[ClicksyService](app/src/main/java/com/clicksy/keyboard/service/ClicksyService.kt):** Core `InputMethodService` managing keyboard lifecycles, editor input connections, settings loading, and theme updates.
- **[KeyboardScreen](app/src/main/java/com/clicksy/keyboard/ui/keyboard/KeyboardScreen.kt):** Top-level Compose container switching seamlessly between QWERTY, Symbols, Numpad, Calculator, Emoji, Voice, and Clipboard panels.
- **[ClicksyDictionaryRepository](app/src/main/java/com/clicksy/keyboard/data/ClicksyDictionaryRepository.kt) & [ClicksyJsonParser](app/src/main/java/com/clicksy/keyboard/data/ClicksyJsonParser.kt):** Zero-allocation streaming parser and repository loading 36,000+ words into memory Trie structures.
- **[DictionaryProvider](app/src/main/java/com/clicksy/keyboard/data/DictionaryProvider.kt):** Unified vocabulary lookup, user frequency learning, bilingual code-switching predictions, and spatial QWERTY typo correction.
- **[HinglishDictionaryData](app/src/main/java/com/clicksy/keyboard/data/HinglishDictionaryData.kt):** Curated Hinglish language model, Romanized Hindi bigrams, abbreviations, and cultural proper noun casing.
- **[VoiceInputPanel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/VoiceInputPanel.kt) & [VoicePermissionActivity](app/src/main/java/com/clicksy/keyboard/util/VoicePermissionActivity.kt):** Translucent IME permission bridge and speech recognition overlay with RMS decibel visualizer.
- **[EmojiSearchPanel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/EmojiSearchPanel.kt) & [EmojiData](app/src/main/java/com/clicksy/keyboard/data/EmojiData.kt):** Multi-stage emoji search engine and categorized emoji repository.
- **[CustomThemeConfig](app/src/main/java/com/clicksy/keyboard/data/CustomThemeConfig.kt) & [ClicksyTheme](app/src/main/java/com/clicksy/keyboard/ui/theme/Theme.kt):** Neubrutalist design tokens, custom theme JSON persistence, and dynamic Material You resolution.
- **[NeuKey](app/src/main/java/com/clicksy/keyboard/ui/keyboard/NeuKey.kt):** High-performance Neubrutalist keycap component with GPU canvas rendering, tactile feedback, and secondary symbol support.
- **[InputTypeDetector](app/src/main/java/com/clicksy/keyboard/util/InputTypeDetector.kt):** Inspects Android `EditorInfo` to derive optimal layout modes, capitalization, and privacy permissions.
- **[CalculatorPanel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/CalculatorPanel.kt) & [CalculatorEngine](app/src/main/java/com/clicksy/keyboard/util/CalculatorEngine.kt):** In-keyboard mathematical expression parser and UI with real-time evaluation and 1-tap paste.
- **[ClipboardManagerService](app/src/main/java/com/clicksy/keyboard/data/ClipboardManagerService.kt) & [ClipboardViewModel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/ClipboardViewModel.kt):** Background clipboard monitor, OTP detection, Room database repository, and UI state flows.

---

## 📦 Releases & Version History

For a complete record of what changed in each version, including feature additions, performance milestones, and file-by-file breakdowns, check out:

👉 **[Release Notes: 0.3.0-beta](RELEASE_NOTES.md)**

---

## 📜 Dataset Attribution

Clicksy includes integrated datasets for Roman Hindi transliteration, conversational chat vocabulary, and English frequency lexicons under permissive open-source licenses:
- **Google Dakshina Dataset:** CC-BY-SA 4.0
- **OpenHinglish:** MIT License
- **FrequencyWords:** MIT License

Detailed licensing notices, source URLs, and modifications are documented in **[ATTRIBUTION.md](ATTRIBUTION.md)**.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Koala / Ladybug or newer)
* Android SDK (API Level 35 compiled SDK, API Level 26+ runtime)
* JDK 17 / JDK 21

### Build & Run
1. Clone this repository:
   ```bash
   git clone https://github.com/BennyAliston/Clicksy.git
   cd Clicksy
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the app.
4. Run the `:app` configuration on your Android device/emulator.

```bash
# Build the debug APK from CLI
# On Linux / macOS:
./gradlew assembleDebug

# On Windows:
.\gradlew.bat assembleDebug
```

### Enabling the Keyboard on Device
1. Open **Settings** on your Android device.
2. Navigate to **System > Languages & input > On-screen keyboard** (varies by device).
3. Select **Manage keyboards** and turn on **Clicksy**.
4. Switch your default input method to **Clicksy** via the system keyboard selector or by opening the Clicksy setup app from the launcher.

---

## 🤝 Contributing

Contributions are welcome! Please check out **[CONTRIBUTING.md](CONTRIBUTING.md)** for development guidelines, coding standards, and pull request procedures.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
