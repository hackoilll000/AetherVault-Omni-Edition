# Changelog

All notable changes to AetherVault Omni Edition are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.4.0-OMNI] - 2026-10-24

### Added
- **Android Media3 ExoPlayer Engine (`1.4.1`)**: Full hardware-accelerated playback in `MediaPlayerScreen.kt` supporting MP4, MKV, WebM, MP3, WAV, AAC, M4A, FLAC, and OGG.
- **Local HTTP Byte-Range Stream Proxy (`TelegramStreamProxy.kt`)**: Loopback HTTP server (`127.0.0.1`) converting ExoPlayer byte-range requests (`Range: bytes=X-Y`) to `HTTP 206 Partial Content` with `Content-Range` headers for instant, scrubbable media streaming without full-file pre-download.
- **WASM & Web Stream Interception (`WasmWebWorkstationView.kt`)**: Android `DownloadListener` and JavaScript bridge `AetherVaultNative.onBlobCaptured(...)` intercepting WebAssembly binary blobs and routing them to native cache and Telegram storage.
- **Standalone Remote Leech Worker (`scripts/remote_leech_worker.py`)**: Python worker for VPS/server execution to download remote direct links in 19 MB parts and ingest directly into Telegram supergroups.
- **Official Orion Store Publication Asset**: Original SVG badge `assets/orion-store-badge.svg` and store documentation `docs/releases/ORION_STORE.md`.
- **Comprehensive Audit Suite**: `docs/audit/BASELINE.md`, `docs/audit/BUG_REGISTER.md`, `docs/audit/MEDIA_PLAYER_AUDIT.md`, `docs/testing/MEDIA_PLAYER_TEST_PLAN.md`, `docs/audit/MASTER_AUDIT_REPORT.md`, `docs/audit/TEST_RESULTS.md`, and `docs/audit/RELEASE_READINESS.md`.

### Changed
- **RemoteFetchWorker**: Replaced mock 15 MB simulation with real OkHttp streaming engine with 60s+ connect/read/write timeouts, automatic 19 MB chunking, and real-time progress callbacks.
- **Telegram Channel ID Parsing**: Enhanced `TelegramMultipartUploader.formatChatId()` to reliably normalize `-100...` supergroups (including `-1004348906769`).
- **Granular Error Reporting**: Caught and mapped Telegram HTTP 400 (Bad Request), 403 (Forbidden / Missing Admin Rights), 413 (Payload Too Large), and 429 (Flood Wait).
- **CI / GitHub Actions Workflows**: Upgraded `.github/workflows/build-and-release.yml` and `.github/workflows/distribution-check.yml` to `gradle/actions/setup-gradle@v4` with JVM tests, artifact verification, and SHA-256 generation.

### Fixed
- **UI Button Overlap**: Scoped `M3StackedFabs` to `MainNavTab.DRIVE`, preventing floating squircle buttons from overlapping bottom input bars in TeleDrop, Media Player, and Workstation screens.
- **TeleDrop Bottom Bar**: Refactored into a spaced `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` with dedicated slots for attachment '+', clipboard paste, text field, and send button.
- **Experimental API Compilation Errors**: Resolved `@file:kotlin.OptIn` scoping across Media3 and Material 3 composables.

### Security
- Reinforced zero-knowledge AES-256-GCM encryption with unique per-chunk IV generation.
- Verified zero hardcoded bot tokens or personal channel IDs in tracked source code.
