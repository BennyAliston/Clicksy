# ⌨️ Clicksy Keyboard

[![Version](https://img.shields.io/badge/Version-0.2.0--beta-purple.svg)](https://github.com/BennyAliston/Clicksy)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy_Me_A_Coffee-Support-yellow.svg?logo=buy-me-a-coffee)](https://buymeacoffee.com/bennyaliston)
[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.x-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-latest-orange.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
## 💡 What is Clicksy?

**Clicksy** is an open-source, ultra-responsive Android keyboard built from the ground up using **Kotlin** and **Jetpack Compose**. Unlike traditional OEM keyboards that rely on flat, generic design patterns and bloated cloud dependencies, Clicksy is engineered around three core pillars:

1. **🎨 Bold Neubrutalist Design System:** Distinctive high-contrast palettes, crisp solid borders, hard offset shadows, 100% opaque layers, and tactile, physical keycaps with zero application window bleed-through.
2. **⚡ 60/120 FPS Zero-Latency Performance:** Instant 0ms touch response, GPU Canvas rendering, lifecycle-cached composition trees, non-blocking asynchronous startup, and sub-0.3ms bucketed word searches for fluid typing without GC stutter.
3. **🛠️ Built-in Productivity Power Tools:** Complete mathematical calculator with 1-tap paste, dedicated 5×4 Numpad, contextual bigram next-word prediction, Gboard-aligned secondary symbol long-presses, and a persistent Room-backed clipboard manager.

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
- **🎯 Dynamic Input Adaptation:** Intelligently configures the keyboard for the active input field—launching the Numpad for numbers/PINs/dates, adding `@` and `.com` shortcuts for emails, adding `/` for web URLs, and disabling suggestions automatically on password fields for privacy.
- **🔣 Long-Press Secondary Symbols:** Access punctuation and special characters directly from letter keys with a simple long-press, backed by responsive haptics and a dedicated number row.
- **🧮 Built-in Calculator:** Perform arithmetic calculations right from your keyboard and insert results directly into any app with a single tap of **↵ Paste**.
- **🔢 Dedicated 5×4 Numpad:** Symmetrical number keypad tailored for uninterrupted numeric input, dialing, and rapid calculations.
- **💡 Smart Suggestion & Learning Engine:** Dynamic vocabulary learning that adapts to your style, offering contextual next-word predictions, auto-capitalization, typo correction, contraction expansions, and inline emoji suggestions.
- **🌈 Dynamic & Adaptive Themes:** Matches your style with Material You dynamic wallpaper theming (Android 12+), automatic system dark/light switching, plus handcrafted presets (*Sunshine* ☀️, *Bubblegum* 🍬, *Minty* 🌿).
- **📋 Integrated Clipboard Manager:** Room-backed clipboard manager with real-time search, pinned items, swipe-to-delete/pin, multi-select, and automatic OTP / verification code detection.
- **😀 Categorized Emoji Panel:** Fast, smooth emoji picker with categorized tabs and fluid scrolling.
- **🔊 Tactile Audio & Haptics:** Zero-latency keypress vibrations and satisfying sound profiles (*System*, *Bubble*, *Woodblock*, *Typewriter*, and *Chime*).
- **⚡ 60 / 120 FPS Fluid Typing:** Native GPU-accelerated Compose rendering and memory caching for instant keyboard popups and stutter-free typing.
- **⚙️ Customizable Layout:** Adjustable keyboard height scaling (85% to 130%), optional dedicated number row, and customizable preferences.

---

## 🛠️ Architecture & Core Components

- **[ClicksyService](app/src/main/java/com/clicksy/keyboard/service/ClicksyService.kt):** Core `InputMethodService` managing keyboard lifecycles, editor input connections, settings loading, and theme updates.
- **[KeyboardScreen](app/src/main/java/com/clicksy/keyboard/ui/keyboard/KeyboardScreen.kt):** Top-level Compose container switching seamlessly between QWERTY, Symbols, Numpad, Calculator, Emoji, and Clipboard panels.
- **[NeuKey](app/src/main/java/com/clicksy/keyboard/ui/keyboard/NeuKey.kt):** High-performance Neubrutalist keycap component with GPU canvas rendering, tactile feedback, and secondary symbol support.
- **[InputTypeDetector](app/src/main/java/com/clicksy/keyboard/util/InputTypeDetector.kt):** Inspects Android `EditorInfo` to derive optimal layout modes, capitalization, and privacy permissions.
- **[NumpadLayout](app/src/main/java/com/clicksy/keyboard/ui/keyboard/NumpadLayout.kt):** Symmetrical 5×4 numeric keypad with integrated calculator shortcuts and layout switches.
- **[CalculatorPanel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/CalculatorPanel.kt) & [CalculatorEngine](app/src/main/java/com/clicksy/keyboard/util/CalculatorEngine.kt):** In-keyboard mathematical expression parser and UI with real-time evaluation and 1-tap paste.
- **[DictionaryProvider](app/src/main/java/com/clicksy/keyboard/data/DictionaryProvider.kt):** Trie-based vocabulary lookup, user frequency learning, next-word prediction, and spatial QWERTY typo correction.
- **[ClipboardManagerService](app/src/main/java/com/clicksy/keyboard/data/ClipboardManagerService.kt) & [ClipboardViewModel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/ClipboardViewModel.kt):** Background clipboard monitor, OTP detection, Room database repository, and UI state flows.
- **[ClicksyTheme](app/src/main/java/com/clicksy/keyboard/ui/theme/Theme.kt):** Neubrutalist design tokens supporting dynamic Material You wallpaper palettes and preset themes.

---

## 📦 Releases & Version History

For a complete record of what changed in each version, including feature additions, performance milestones, and file-by-file breakdowns, check out:


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
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the app.
4. Run the `:app` configuration on your Android device/emulator.

```bash
# Build the debug APK from CLI
./gradlew assembleDebug
```

### Enabling the Keyboard on Device
1. Open **Settings** on your Android device.
2. Navigate to **System > Languages & input > On-screen keyboard** (varies by device).
3. Select **Manage keyboards** and turn on **Clicksy**.
4. Switch your default input method to **Clicksy** via the system keyboard selector or by opening the Clicksy setup app from the launcher.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
