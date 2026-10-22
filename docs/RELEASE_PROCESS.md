# AetherVault Release Engineering & Build Guide

**Target Gradle**: 8.x + AGP  
**Target Kotlin**: 2.x  
**JDK Requirement**: JDK 17 / JDK 21

---

## Reproducible Build Commands

### 1. Execute Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### 2. Build Debug Universal APK
```bash
gradle assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Build Production Android App Bundle (AAB)
```bash
gradle :app:bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

---

## Release Checksums Verification
Run SHA-256 verification after every release build:
```bash
sha256sum app-debug.apk releases/AetherVault-Universal-Debug.apk
```
