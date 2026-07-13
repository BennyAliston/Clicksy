# ⌨️ Clicksy Keyboard

[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.x-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-latest-orange.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Clicksy** is a modern, high-performance, and feature-rich Android keyboard built entirely using **Jetpack Compose** and Kotlin. It embraces a bold **Neubrutalism** design system featuring high-contrast colors, solid black borders, hard offset shadows, and dynamic micro-animations.

---

## 📸 Previews

### 🎥 Demo Video

<p align="center">
  <video src="https://github.com/user-attachments/assets/cc1246aa-8b1b-4f26-b56d-59587e11e108" width="280" controls></video>
  <br/>
  <em>If the video player doesn't display above, you can open it directly: <strong><a href="https://github.com/user-attachments/assets/cc1246aa-8b1b-4f26-b56d-59587e11e108">Demo Video (MP4)</a></strong></em>
</p>

### 🖼️ Screenshots

| Rounded Keys | Bubblegum Theme | Emojis |
| :---: | :---: | :---: |
| ![Rounded Keys](https://github.com/user-attachments/assets/64fe2893-31c8-4985-b27d-469a90c91634) | ![Bubblegum Theme](https://github.com/user-attachments/assets/e373cf7b-8417-487c-bde5-e7b1dc7dcc68) | ![Emojis](https://github.com/user-attachments/assets/d23e02f9-d2cf-4eca-9fec-3151f7e9bcef) |

| Clipboard | Minty Theme | Dark Theme |
| :---: | :---: | :---: |
| ![Clipboard](https://github.com/user-attachments/assets/42cae0c5-67a8-41b7-b1d0-7f4c5259beea) | ![Minty Theme](https://github.com/user-attachments/assets/0d8aea06-da69-490b-baaf-ca715609d587) | ![Dark Theme](https://github.com/user-attachments/assets/a0123095-2a87-40e9-87d4-9459f493952a) |

<p align="center">
  <img src="https://github.com/user-attachments/assets/00b27fb0-58b5-47c3-9c33-6ab27fdb3a06" width="280" alt="Clicksy App Screen" /><br/>
  <em>App Settings & Clipboard Preview</em>
</p>

---

## ✨ Features

- **🎨 Neubrutalism Design System:** Distinct, playful typography, thick borders, and heavy drop shadows that set it apart from flat default system keyboards.
- **🌈 Dynamic & Vibrant Themes:**
  - **Sunshine ☀️:** Warm cream and bright yellow keys.
  - **Bubblegum 🍬:** Sweet pastel lavender with hot pink accents.
  - **Minty 🌿:** Refreshing mint green with teal highlights.
  - **Adaptive 🌑:** Dynamic color scheme that extracts the dominant brand color of the currently active app (e.g., green for WhatsApp, red for YouTube) and adapts the keyboard accent theme on-the-fly!
- **🔊 Rich Keypress Sound & Haptics:** Custom audio feedback profiles including *System*, *Bubble*, *Woodblock*, *Typewriter*, and *Chime*, along with customizable haptic vibration.
- **💡 Autocomplete & Suggestions:** A smart suggestion bar that learns vocabulary dynamically as you type.
- **📋 Persistent Clipboard Manager:** Gboard-like clipboard built with Room Database. Supports instant search, pinned items, favorite items, swipe actions (swipe left to delete with a fade animation, swipe right to pin), long-press multi-select, sharing, and a 5-minute auto-expiring suggestion pill above the keyboard.
- **😀 Emoji Keyboard:** Categorized emoji grid panel built natively in Compose.
- **🎙️ Voice Input:** Integrated speech-to-text recognition. (Coming Soon)
- **🔢 Custom Layout Settings:** Easily toggle a dedicated number row, adjust keyboard height scaling (85% to 130%), and modify preferences.

---

## 🛠️ Architecture & Core Components

- **[ClicksyService](app/src/main/java/com/clicksy/keyboard/service/ClicksyService.kt):** The core `InputMethodService` managing keyboard lifecycles, editor input connections, settings loading, and state updates.
- **[KeyboardScreen](app/src/main/java/com/clicksy/keyboard/ui/keyboard/KeyboardScreen.kt):** The top-level Compose container rendering the active panel (QWERTY, Symbols, Emoji, Clipboard) and driving theme animations.
- **[ClicksyTheme](app/src/main/java/com/clicksy/keyboard/ui/theme/Theme.kt):** Custom neubrutalist composition locals carrying color palettes (`ClicksyColorScheme`), dimensions (`ClicksyDimensions`), and text styles.
- **[DictionaryProvider](app/src/main/java/com/clicksy/keyboard/data/DictionaryProvider.kt):** Implements dynamic user dictionary lookup, auto-saving learned words, and suggestion scoring.
- **[ClipboardManagerService](app/src/main/java/com/clicksy/keyboard/data/ClipboardManagerService.kt):** Tracks primary clip updates, filters passwords, checks Android sensitive flags, and runs custom OTP/verification code detection.
- **[ClipboardViewModel](app/src/main/java/com/clicksy/keyboard/ui/keyboard/ClipboardViewModel.kt):** Exposes StateFlow streams for searched/filtered history and runs clipboard suggestion tickers and multi-select actions.
- **[ClipboardRepository](app/src/main/java/com/clicksy/keyboard/data/ClipboardRepository.kt):** Coordinates Room CRUD operations, caps storage history to 100 items, and manages duplicate entries.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Koala / Ladybug or newer)
* Android SDK (API Level 35 compiled SDK, API Level 26+ runtime)
* JDK 17

### Build & Run
1. Clone this repository.
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
