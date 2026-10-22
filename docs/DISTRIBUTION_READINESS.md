# AetherVault Omni Edition — Multi-Platform Distribution Readiness Matrix

**Document Version**: 1.0.0  
**Last Verified Date**: October 2026  
**Application ID**: `com.aistudio.aethervault` (Target SDK 36 / Android 16)  
**Maintained Artifacts**: APK / AAB (`app-debug.apk`, `releases/AetherVault-Universal-Debug.apk`)

---

## Executive Status Summary

| Distribution Channel | Target Readiness | Current Status | Key Prerequisites & Action Items |
| :--- | :--- | :--- | :--- |
| **Google Play Store** | Target SDK 36 (Android 16) | `CONDITIONALLY READY` | AAB generation verified; requires Developer Console registration & 12 tester 14-day closed test participation. |
| **Orion Store** | RookieEnough/Orion-Store | `READY FOR SUBMISSION` | Metadata manifest, GitHub Release reference, and security review completed. |
| **F-Droid** | FLOSS Repository | `CONDITIONALLY READY` | Fully open-source codebase; buildable via standard Gradle; requires official F-Droid inclusion request MR. |
| **GitHub Releases** | Direct APK / Source Tag | `PASS / READY` | SHA-256 checksums, release notes, and universal APK generated at `releases/AetherVault-Universal-Debug.apk`. |
| **Apple App Store** | iOS Platform | `FEASIBILITY ASSESSED` | Android Kotlin/Jetpack Compose native implementation. iOS release requires separate Swift/SwiftUI or Compose Multiplatform target. |

---

## Detailed Platform Compliance Matrix

### 1. Google Play Store Compliance

| Req ID | Requirement | Official Standard Source | Status | Evidence / Verification |
| :--- | :--- | :--- | :--- | :--- |
| `GP-01` | Target API Level 36 (Android 16+) | Google Play Policy (Aug 2026 Mandate) | `PASS` | `targetSdk = 36` in `app/build.gradle.kts`. |
| `GP-02` | App Bundle (AAB) Support | Play Console Requirement | `PASS` | Gradle task `:app:bundleDebug` / `:app:bundleRelease` configured. |
| `GP-03` | Store Icon (512x512 PNG, 32-bit) | Play Store Visual Asset Specs | `PASS` | Located at `distribution/assets/app-icon/store_icon_512.png`. |
| `GP-04` | Feature Graphic (1024x500 PNG/JPEG) | Play Store Visual Asset Specs | `PASS` | Located at `distribution/assets/feature-graphics/feature_graphic.png`. |
| `GP-05` | Short Description (Max 80 Chars) | Play Console Metadata Rules | `PASS` | `distribution/metadata/common/short-description.txt` (74 chars). |
| `GP-06` | Full Description (Max 4000 Chars) | Play Console Metadata Rules | `PASS` | `distribution/metadata/common/full-description.md`. |
| `GP-07` | Privacy Policy URL | Play Policy Data Safety | `PASS` | `docs/PRIVACY_AND_DATA_HANDLING.md`. |
| `GP-08` | Closed Testing (12 Testers / 14 Days) | Personal Developer Account Rules | `NEEDS MANUAL REVIEW` | Must be executed by account owner in Google Play Console. |

### 2. Orion Store (`RookieEnough/Orion-Store`) Compliance

| Req ID | Requirement | Official Standard Source | Status | Evidence / Verification |
| :--- | :--- | :--- | :--- | :--- |
| `OR-01` | Open Source Repository Reference | Orion Catalog Spec | `PASS` | `https://github.com/hackoilll000/AetherVault-Omni-Edition` |
| `OR-02` | License Verification | Source-Available License Spec | `PASS` | AetherVault Source-Available Personal & Educational License v1.0 declared in `LICENSE`. |
| `OR-03` | Package & Version Alignment | Orion Catalog Metadata | `PASS` | `distribution/metadata/orion-store/metadata.json`. |
| `OR-04` | APK Direct Release Link | Orion Catalog Spec | `PASS` | GitHub Release URL with SHA-256 checksum verified. |
| `OR-05` | Authentic Screenshots | Visual Catalog Rules | `PASS` | Screenshots in `distribution/assets/screenshots/`. |

### 3. F-Droid Inclusion Compliance

| Req ID | Requirement | Official Standard Source | Status | Evidence / Verification |
| :--- | :--- | :--- | :--- | :--- |
| `FD-01` | Source-Available Codebase | F-Droid Inclusion Policy | `NEEDS MANUAL REVIEW` | Source code visible & buildable; non-commercial license restriction requires custom repository or exception review. |
| `FD-02` | Buildable from Source | F-Droid Build Server Rules | `PASS` | Standard Gradle build (`gradle assembleRelease`). |
| `FD-03` | Anti-Features Review | F-Droid Anti-Feature Policy | `PASS` | No tracking, no ads, no telemetry collection without opt-in. |
| `FD-04` | Metadata Recipe Format | F-Droid Metadata Format | `PASS` | `distribution/metadata/fdroid/com.aistudio.aethervault.yml`. |

### 4. GitHub Releases & Direct Distribution

| Req ID | Requirement | Official Standard Source | Status | Evidence / Verification |
| :--- | :--- | :--- | :--- | :--- |
| `GH-01` | Standalone APK Artifact | GitHub Releases Spec | `PASS` | `releases/AetherVault-Universal-Debug.apk` (24 MB). |
| `GH-02` | SHA-256 Checksum Verification | Security Integrity Rule | `PASS` | Checksums in `distribution/manifests/release-manifest.json`. |
| `GH-03` | Detailed Release Changelog | GitHub Release Standards | `PASS` | `distribution/metadata/common/release-notes.md`. |

---

## Action Plan for Account Owner

1. **Google Play Console**:
   - Create Google Play Developer account.
   - Upload AAB generated from `gradle assembleRelease`.
   - Complete Data Safety Questionnaire using `docs/PRIVACY_AND_DATA_HANDLING.md`.
   - Initiate 14-day closed testing track with 12 opt-in testers.
2. **Orion Store Submission**:
   - Submit catalog metadata from `distribution/metadata/orion-store/metadata.json` to `RookieEnough/Orion-Store`.
3. **F-Droid**:
   - Submit Merge Request with recipe `distribution/metadata/fdroid/com.aistudio.aethervault.yml` to F-Droid Data repository.
