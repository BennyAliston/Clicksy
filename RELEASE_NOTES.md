# 🚀 Clicksy Keyboard — Release Notes: 0.3.0-beta

**Release Version:** `0.3.0-beta` (`versionCode 3`)  
**Target SDK:** Android 15 (API 35) | **Min SDK:** Android 8.0 (API 26)  
**Status:** Beta Release  

---

## 🌟 Highlights & Major Features

### 1. 🌐 Unified English + Hinglish 36,000+ Vocabulary Engine
- **Massive Dual-Corpus Lexicon:** Integrated 36,633 normalized entries (24,615 Hinglish, 8,377 English, and 3,641 bilingual overlap) bundled directly in-app (`clicksy_dictionary.json`).
- **Google Dakshina Dataset Integration:** 53,000+ native Devanagari-to-Roman transliteration pairs mapped to canonical lemmas and common phonetic variants.
- **OpenHinglish Lexicon:** Everyday chat vocabulary, natural abbreviations (`nhi`, `kr`, `bhot`, `smjh`), brands, and Indian cultural names (`Delhi`, `Diwali`).
- **Zero-Allocation Streaming Parser:** Custom streaming JSON reader loads 36k+ words directly into memory Trie without DOM allocations, preventing UI stutter on startup.
- **Contextual N-Grams:** 168 curated bigrams and 103 trigrams tailored for natural code-switching between English and Hinglish.
- **Dataset Attribution:** Full licensing and dataset provenance documented in [ATTRIBUTION.md](ATTRIBUTION.md).

### 2. 🎙️ Dedicated Voice Typing & Speech Recognition
- **Full-Height Neubrutalist Voice Panel:** Tactile overlay with continuous speech recognition and real-time live hypotheses display.
- **Live Decibel Visualizer:** Dynamic visualizer animating in response to real-time microphone RMS audio levels (`rmsDb`).
- **IME Permission Bridge:** Dedicated translucent `VoicePermissionActivity` ensuring smooth `RECORD_AUDIO` permission grants from within the IME service context.
- **Quick Controls:** Space bar, backspace, and one-tap return to keyboard directly in the voice panel.

### 3. 🔍 Instant Emoji Search & Discovery Chips
- **Real-Time Search Bar:** Instant query matching across hundreds of emoji keywords.
- **Popular Discovery Chips:** Quick-tap chips for rapid discovery: *Smile, Love, Fire, Party, Laugh, Thumbs, Sad, Cool, Cat, Dog, Food, Coffee, Star, Car, Heart, Pray, Music, Money, Flex, Work, Game, Check*.
- **Multi-Tier Search Algorithm:** Prioritizes exact keyword matches, prefix matches, tokenized substring matches, and category fallbacks with haptic touch feedback.

### 4. 🎨 Custom Theme Studio & 6 New Palettes
- **In-App Theme Studio:** Interactive customization allowing users to customize every color token: key background, accent key, action key, primary text, borders, shadows, and suggestions.
- **Dynamic Hex Color Picker:** Real-time color modification with instant live preview.
- **6 New Handcrafted Palettes:**
  - **Cyberpunk:** Deep black with neon cyan (`#00F0FF`) and hot magenta (`#FF0055`).
  - **Lavender Dream:** Soft lilac with royal violet (`#C084FC`) and deep plum (`#581C87`).
  - **Forest Glow:** Deep dark forest green (`#052E16`) with mint accents (`#10B981`).
  - **Retro Pop:** Warm cream (`#FEF3C7`) with vintage amber (`#F59E0B`) and cherry red (`#DC2626`).
  - **Sunset Horizon:** Dusk indigo (`#1E1B4B`) with radiant peach (`#FB923C`) and coral pink (`#F43F5E`).
  - **Monochrome Sharp:** High-contrast pitch black (`#000000`), pure white (`#FFFFFF`), and charcoal (`#374151`).

### 5. 📋 Comprehensive Clipboard Manager Overhaul
- **Action Toolbar:** Scrollable action bar with quick shortcuts: *Paste Latest, Clear All, Pin/Unpin, Multi-Select, Deduplicate*.
- **Category Filter Tabs:** Instant filtering by *All, Pinned, URLs, OTP / Codes, Emails, Phone Numbers*.
- **Smart Privacy Shield:** Automatically ignores clipboard copies when typing in password fields; heuristic detection for OTPs, credit cards, and API secrets.
- **URL Sanitization & Clean-up:** Strips UTM tracking parameters (`utm_source`, `fbclid`, `gclid`) on demand.

### 6. ⚡ Smart Typing, Auto-Correction & Suggestions
- **Granular Settings Controls:** Dedicated toggles for predictive Auto-Suggestions and Spacebar Auto-Correction.
- **Smart Punctuation Snapping:** Automatically cleans up whitespace before punctuation marks (`.`, `,`, `?`, `!`) and adds trailing spacing.
- **Dynamic User Learning:** Safely adapts to the user's typing frequency while maintaining strict offline privacy.

---

## 🧪 Testing & Verification

- **DictionaryProviderTest:** Validates unified 36k vocabulary loading, Hinglish transliteration variants, bigram/trigram predictions, and auto-correction.
- **InputTypeDetectorTest:** Thorough test suite verifying detection across password fields, email, URL, numeric, phone, and standard text fields.
- **ClipboardRepositoryTest & ClipboardUtilsTest:** Verifies Mutex deduplication, Luhn checksum credit card detection, sensitive token masking, and URL cleaning.
- **CustomThemeConfigTest:** Verifies JSON serialization/deserialization and color scheme mapping.
- **EmojiDataTest:** Tests multi-stage emoji search algorithms, popular search tags, and category indexing.

---

## 📁 Key File Changes

- `app/src/main/assets/clicksy_dictionary.json`: Bundled 36k+ English + Hinglish dictionary.
- `app/src/main/java/.../ClicksyDictionaryRepository.kt`: Asynchronous dictionary repository.
- `app/src/main/java/.../ClicksyJsonParser.kt`: Streaming JSON dictionary parser.
- `app/src/main/java/.../HinglishDictionaryData.kt`: Curated Romanized Hindi vocabulary & n-grams.
- `app/src/main/java/.../VoiceInputPanel.kt` & `VoicePermissionActivity.kt`: Voice typing subsystem.
- `app/src/main/java/.../EmojiSearchPanel.kt`: Dedicated emoji search interface.
- `app/src/main/java/.../CustomThemeConfig.kt`: User custom theme persistence.
- `tools/`: Data preparation scripts (`build_clicksy_dictionary.py`, `download_sources.py`).
- `ATTRIBUTION.md`: Dataset licensing and provenance notices.
- `gradlew.bat`: Added Gradle wrapper script for Windows.
