# AetherVault Omni Edition — Audit Baseline

## 1. System & Environment Baseline

- **Repository:** `https://github.com/hackoilll000/AetherVault-Omni-Edition`
- **Application Name:** AetherVault Omni Edition
- **Namespace:** `com.example`
- **Application ID:** `com.aistudio.aethervault.weathertracker` / `com.aistudio.aethervault`
- **Target OS / Architecture:** Android 7.0+ (API 24 to API 36, Android 16 Preview)
- **Primary Toolchain:**
  - **Android Gradle Plugin (AGP):** `9.1.1`
  - **Kotlin:** `2.2.10`
  - **Jetpack Compose BOM:** `2024.09.00`
  - **Kotlin Symbol Processing (KSP):** `2.3.5`
  - **Android Media3 (ExoPlayer):** `1.4.1`
  - **OkHttp / Retrofit:** `4.10.0` / `2.12.0`
  - **Room Database:** `2.7.0`
  - **Java SDK:** OpenJDK 17 LTS (headless container environment)

---

## 2. Environment Constraints & Verification Scope

1. **Local JVM Testing (Robolectric & JUnit 4)**:
   - Full JVM test suite executed via `gradle :app:testDebugUnitTest`.
   - Instrumented tests requiring active hardware emulators or ADB (`androidTest/`) cannot execute in this container environment.
2. **Deterministic Build Pipeline**:
   - `compile_applet` and `gradle assembleDebug` run directly on Android toolchains.
   - Output binary produced: `release_apk/AetherVault-Omni-v2.4-debug.apk` and `app/build/outputs/apk/debug/app-debug.apk`.

---

## 3. Subsystem Inventory

| Module / Package | Subsystem Function | Key Files |
| :--- | :--- | :--- |
| `com.example.core.telegram` | Telegram Bot API client & multipart streaming | `TelegramBotClient.kt`, `TelegramMultipartUploader.kt` |
| `com.example.core.crypto` | AES-256-GCM encryption & PBKDF2 key derivation | `AesGcmChunkCipher.kt`, `KeyDerivation.kt` |
| `com.example.core.media` | Local RFC 7233 byte-range streaming proxy | `TelegramStreamProxy.kt` |
| `com.example.features.media` | Media3 ExoPlayer video/audio player | `MediaPlayerScreen.kt` |
| `com.example.features.remotefetch` | Direct HTTP streaming & cloud ingest | `RemoteFetchWorker.kt`, `RemoteFetchScreen.kt` |
| `com.example.features.workstation` | WASM / Web blob interception & viewer | `WasmWebWorkstationView.kt`, `WorkstationViewerScreen.kt` |
| `com.example.features.teledrop` | Real-time drop stream & input dock | `TeleDropScreen.kt` |
| `com.example.core.database` | Room persistence layer | `AppDatabase.kt`, `Entities.kt`, `Daos.kt` |
| `com.example.ui.theme` | Material 3 Liquid Glass & neon palettes | `Theme.kt`, `Color.kt` |

---

## 4. Initial Build & Pipeline Defect Reproduction

Prior to refactoring, three primary failure vectors existed:
1. **CI Pipeline Stalls**:
   - GitHub Actions workflows called `chmod +x gradlew` without `gradlew` present on clean checkouts.
   - KSP / Kotlin compiler initialization exceptions caused by mismatched `@file:OptIn` scope and Experimental Material 3 APIs.
2. **Media Player Glitch**:
   - Media playback in `MediaPlayerScreen.kt` was a non-functional static mockup with fake duration calculations.
   - Remote streaming lacked byte-range support, preventing seek and scrub capabilities on files stored in Telegram.
3. **UI Button Collision**:
   - Floating squircle FABs (`M3StackedFabs`) rendered unconditionally at `Alignment.BottomEnd`, floating over the input bar and send button on TeleDrop and player screens.
