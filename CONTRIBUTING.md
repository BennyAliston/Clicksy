# Contributing to Clicksy Keyboard

Thank you for your interest in contributing to **Clicksy Keyboard**! We welcome bug reports, feature suggestions, documentation improvements, and pull requests.

---

## 🛠️ Development Setup

1. **Prerequisites:**
   - Android Studio (Koala, Ladybug, or newer)
   - JDK 17 or JDK 21
   - Android SDK with API 35 compiled SDK (API 26+ runtime)

2. **Fork and Clone:**
   ```bash
   git clone https://github.com/<your-username>/Clicksy.git
   cd Clicksy
   ```

3. **Open in Android Studio:**
   - Open Android Studio and select **Open**.
   - Browse to the cloned directory and allow Gradle to sync.

4. **Run Unit Tests:**
   - On Linux/macOS:
     ```bash
     ./gradlew test
     ```
   - On Windows:
     ```powershell
     .\gradlew.bat test
     ```

---

## 🎨 Design Guidelines: Neubrutalism

Clicksy strictly follows a tactile **Neubrutalist** aesthetic. When contributing UI or themes, please adhere to these core rules:
- **Hard Borders:** 2dp solid, crisp black or high-contrast borders.
- **Offset Shadows:** Solid, unblurred 2dp–4dp shadows (no blurry elevation halos or gradient fades).
- **100% Opacity:** All panels, keys, and popups must have 100% opaque backgrounds to prevent window bleeding.
- **Tactile Feedback:** Every interactive element should provide haptic and/or audio feedback consistent with user settings.

---

## 🌿 Branching & Git Workflow

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. Follow clear, descriptive commit messages:
   - `feat: add swipe-to-delete gesture in clipboard`
   - `fix: resolve voice typing timeout on Samsung devices`
   - `docs: update setup guide for Windows`

3. Make sure all unit tests pass before submitting your PR:
   ```bash
   ./gradlew test
   ```

---

## 📬 Submitting a Pull Request

1. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request against the `main` branch of `BennyAliston/Clicksy`.
3. Provide a clear summary of your changes, referencing any open issues.
4. Include screenshots or screen recordings for any UI changes.

---

## 🐛 Reporting Issues

- Use the [GitHub Issues](https://github.com/BennyAliston/Clicksy/issues) tab to report bugs or request features.
- Please include:
  - Device model & Android OS version
  - Clicksy app version
  - Steps to reproduce the bug
  - Expected vs. actual behavior
  - Logcat output (if applicable)

---

## 📄 License

By contributing to Clicksy, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
