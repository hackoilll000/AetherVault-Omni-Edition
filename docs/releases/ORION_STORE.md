# Orion Store Publication & Distribution Documentation

## 1. Overview & Official Listing

AetherVault Omni Edition is officially indexed and distributed via the **Orion Store** ecosystem.

- **Canonical Listing URL:** [View on Orion Store](https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition&icon=https%3A%2F%2Fraw.githubusercontent.com%2Fhackoilll000%2FAetherVault-Omni-Edition%2Fmain%2Fassets%2Fapp_icon.png)
- **App Identifier:** `aethervault-omni-edition`
- **Application Package / Namespace:** `com.aistudio.aethervault`
- **Canonical App Icon Asset:** `https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/app_icon.png`

---

## 2. Publication Status & Metadata Provenance

| Metric | Verified Value | Verification Status |
| :--- | :--- | :--- |
| **Catalog Listing** | Active redirect & app sheet | Confirmed via Orion-Data repository |
| **Primary Category** | Tools & Cloud Productivity | Documented in store metadata |
| **Declared License** | Source-Available v1.0 | Aligned with repository root license |
| **Target SDK / Min SDK** | Android API 36 / Android API 24 (Android 7.0+) | Verified in `app/build.gradle.kts` |
| **Store Version Code** | `300` (`3.0.0-OMNI`) | Documented in `distribution/metadata/orion-store/metadata.json` |

*Note: Orion Store operates as a decentralized, source-linked application distributor. Publication indicates the project's metadata and distribution endpoints are validated by Orion's catalog schemas. It does not imply proprietary third-party commercial endorsement.*

---

## 3. Installation Routes

Users can install AetherVault Omni Edition through either of the following paths:

1. **Direct Orion Store Client / Web Ingestion**:
   - Access the canonical redirect: `https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition`
   - Orion Store client automatically resolves the latest cryptographically signed release artifact.
2. **Direct GitHub Releases**:
   - Download verified standalone APKs directly from the [GitHub Releases](https://github.com/hackoilll000/AetherVault-Omni-Edition/releases) page.
   - Always verify the SHA-256 checksum matching the published release notes prior to installation.

---

## 4. Differences Between Store Builds & GitHub Artifacts

| Feature | Orion Store Build | GitHub Tagged Release | Local Debug Build (`app-debug.apk`) |
| :--- | :--- | :--- | :--- |
| **Signing Key** | Standard Release Key | Production / CI Release Key | Android Debug Keystore |
| **ProGuard / R8** | Enabled (Optimized & Obfuscated) | Enabled | Disabled |
| **Crash Logging** | User-consented local logcat only | Local logcat only | Verbose developer diagnostics |
| **Telemetry / Tracking** | Zero (Zero-Knowledge Architecture) | Zero | Zero |

---

## 5. Support & Defect Reporting

- **Issue Tracker:** [GitHub Issues](https://github.com/hackoilll000/AetherVault-Omni-Edition/issues)
- **Security Inquiries:** Review `SECURITY.md` for zero-disclosure security reports.
- **Verification Protocol:** Whenever a new version is tagged in GitHub (`v*`), the automated workflow runs `python3 scripts/validate_distribution.py` to ensure Orion Store metadata schemas, checksums, and release assets remain in strict synchronization.
