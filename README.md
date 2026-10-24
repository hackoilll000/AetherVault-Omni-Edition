<div align="center">
  <img src="https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/app_icon.png" width="128" height="128" alt="AetherVault Omni App Icon" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.25);">
  <h1>AetherVault Omni Edition (Tele Drive)</h1>
  <h3>Google Material Design 3 • Infinite Telegram Cloud Storage • Zero-Knowledge Encryption • Android Media3 ExoPlayer • Python Bot Studio • Self-Hosted Automation</h3>

  <p align="center">
    <a href="https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition&icon=https%3A%2F%2Fraw.githubusercontent.com%2Fhackoilll000%2FAetherVault-Omni-Edition%2Fmain%2Fassets%2Fapp_icon.png">
      <img src="assets/orion-store-badge.svg" alt="Available on Orion Store" height="42">
    </a>
  </p>
</div>

[![Android CI](https://img.shields.io/badge/Android-Kotlin_%26_Jetpack_Compose-blue?style=for-the-badge&logo=android)](https://developer.android.com)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-success?style=for-the-badge&logo=shield)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
[![Backend](https://img.shields.io/badge/Backend-Telegram_Bot_Swarm-blueviolet?style=for-the-badge&logo=telegram)](https://core.telegram.org/bots/api)
[![Media Engine](https://img.shields.io/badge/Media-Media3_ExoPlayer_1.4.1-cyan?style=for-the-badge&logo=google)](https://developer.android.com/media/media3)
[![Orion Store](https://img.shields.io/badge/Store-Orion_Store_Listing-purple?style=for-the-badge)](https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition&icon=https%3A%2F%2Fraw.githubusercontent.com%2Fhackoilll000%2FAetherVault-Omni-Edition%2Fmain%2Fassets%2Fapp_icon.png)
[![License](https://img.shields.io/badge/License-Source--Available_Personal_Use_Only-red?style=for-the-badge)](LICENSE)

---

## 🌟 Officially Available on Orion Store

AetherVault Omni Edition is officially published on the **Orion Store**. Visit the verified listing to view the app card and access the verified installation route:

👉 **[View AetherVault Omni Edition on Orion Store](https://rookieenough.github.io/Orion-Data/redirect.html?id=aethervault-omni-edition&icon=https%3A%2F%2Fraw.githubusercontent.com%2Fhackoilll000%2FAetherVault-Omni-Edition%2Fmain%2Fassets%2Fapp_icon.png)**

For detailed store compliance and verification criteria, refer to [`docs/releases/ORION_STORE.md`](docs/releases/ORION_STORE.md).

---

## 🔍 Search Tags & Indexing Keywords
`android-cloud-storage` `telegram-file-vault` `zero-knowledge-encryption` `aes-256-gcm` `jetpack-compose-ui` `material-design-3` `exoplayer-media3` `streaming-proxy` `python-bot-studio` `termux-self-hosting` `artifact-storage-engine` `orion-store` `aethervault`

---

## 👨‍💻 Creator & Architectural Leadership

- **Developer & Lead Architect:** AYUSH MONDAL (Class 9th, 14 years old in 2026)
- **Role:** Creator, Ethical Hacker & Cybersecurity Educator
- **YouTube Channel:** [TRB FireOps (@trbfireops)](https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0)
- **Publication Date:** October 2026
- **Team / Organization:** TRB FireOps / AJPM Developer Team

---

## 📥 Recommended Release APK to Download

For the best, most stable, and feature-complete performance across all Android devices (Android 7.0 to Android 16+), download the universal build:

### 🌟 **[Download Recommended APK: AetherVault-Omni-v2.4-debug.apk](release_apk/AetherVault-Omni-v2.4-debug.apk)** *(Size: 27 MB)*
*(Alternative link: `releases/AetherVault-Universal-Debug.apk`)*

> **Note:** The compiled APK includes the complete Media3 ExoPlayer engine, RFC 7233 partial-content HTTP streaming proxy, WASM interceptor, Python Bot Studio, Termux deployment tools, and AES-256-GCM cipher drivers bundled in a single universal APK.

---

## ⚖️ Legal Disclaimer & Usage Policy

> **Compliance & Responsible Conduct Notice:**
> This software is strictly developed for PERSONAL USE, experimental research, and educational demonstration purposes. It serves solely as an architectural prototype demonstrating how to manage serverless bot workflows and encrypted storage directly from an Android client. This project does NOT violate, bypass, or advocate breach of Telegram Terms of Service or API policies. The authors and developers (Ayush Mondal / AJPM / TRB FireOps) assume NO LIABILITY or responsibility whatsoever for any misuse, unintended activity, or improper conduct by end users. Use of this application is entirely at your sole discretion and responsibility.

---

## 🚀 Major Systems & Architecture Overview

### 1. 🎬 Media3 ExoPlayer & Local Byte-Range Stream Proxy (`TelegramStreamProxy`)
- **Android Media3 (1.4.1)**: Full hardware-accelerated playback for MP4, MKV, WebM, MP3, WAV, AAC, M4A, FLAC, and OGG formats.
- **Local HTTP Proxy Engine (`127.0.0.1`)**: Translates ExoPlayer `Range: bytes=start-end` requests into `HTTP 206 Partial Content` with `Content-Range` headers.
- **Zero Full-Download Waiting**: Stream and seek directly into multi-gigabyte video files stored on Telegram without pre-downloading the whole file.
- **Custom Player UI Overlay**: Scrubbable seek bar, current/duration timestamps, $\pm10$s skip buttons, buffering indicator, and error retry state.

### 2. ⚡ Resilient Remote Fetch & Chunking Pipe (`RemoteFetchWorker`)
- **OkHttp Streaming Pipe**: Downloads direct HTTP/HTTPS files with 60s+ connect/read/write timeouts.
- **19 MB Safe Chunking**: Automatically fragments large files into 19 MB parts, safely adhering to Telegram’s 50 MB Bot API ceiling.
- **Granular Error Reporting**: Intercepts Telegram 400 Bad Request, 403 Forbidden (missing admin rights), 413 Payload Too Large, and 429 Flood Wait (`retry_after`).
- **Cloud / VPS Leech Script (`scripts/remote_leech_worker.py`)**: Standalone Python worker to ingest multi-GB remote files directly into Telegram channels on remote servers.

### 3. 🌐 In-App Web & WASM Stream Interception (`WasmWebWorkstationView`)
- **Native `DownloadListener`**: Captures standard browser file downloads and resolves content disposition.
- **JavaScript Blob Interception Bridge**: Automatically hooks `URL.createObjectURL(blob)` and `<a>` download clicks for WebAssembly binary outputs, transferring them directly into native cache and the Telegram upload queue.

### 4. 🎨 Liquid Glass & Material Design 3 Theme Engine
- **9 High-Contrast Themes**: Material You Monet, Obsidian Gold, Titanium Ice, Pure AMOLED, High Contrast, Alabaster Clean, Frosted Glass, Nothing Dot-Matrix, and Custom User Palette.
- **WCAG AA Contrast Validator**: Live relative luminance contrast ratio calculation (`calculateContrastRatio()`).
- **Separated Bottom Bar Navigation**: Clean, non-overlapping `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` with dedicated attach '+', paste, text input, and send actions.

### 5. 🐍 Self-Hosted Python Bot Studio & Termux Deployer
- **Zero Developer-Server Backend**: 100% user-owned bot execution architecture.
- **Bot Starter Templates**: Pre-configured templates for Echo Bot, FAQ Bot, Group Moderation Bot, Scheduled Reminder Bot, Gemini AI Assistant Bot, and Webhook Automation Bot.
- **Termux Deployment Manager**: Automatic bash script generator (`termux_start_bot.sh`) and requirements generator.

### 6. 🔒 Multi-Bot Telegram Cloud Storage & Encryption
- **AES-256-GCM Encryption**: On-device KeyStore encryption with unique per-chunk IV offsets.
- **Multi-Bot Swarm Engine**: Traffic distribution across multiple Telegram bots with rate-limit handling on `FLOOD_WAIT_X`.
- **TeleDrop Stream**: Live channel snippet sharing, code pasting, and 120Hz smooth offline synchronization.

---

## 🏛️ Architectural Clarification: Passive Cloud Storage vs. Interactive Cloud Gaming

| Characteristic | Passive Storage & Streaming (Supported) | Interactive Cloud Gaming (Not Feasible on Telegram) |
| :--- | :--- | :--- |
| **Primary Workload** | Static chunk storage, sequential file transfer, media segmenting. | Real-time 3D GPU rendering, physics computation, dynamic scene generation. |
| **Latency Budget** | Tolerates 500 ms – 3,000 ms buffering delay. | Strict **< 20 ms** round-trip latency (input $\rightarrow$ render $\rightarrow$ display). |
| **Transport Protocol** | HTTP/1.1 or HTTP/2 chunked POST / GET requests over Telegram API. | Low-latency WebRTC / UDP with dynamic frame dropping and jitter buffers. |
| **Telegram Capability** | **Excellent**: Unlimited free cloud storage across channels up to 2 GB per part (4 GB with Premium). | **Infeasible**: Telegram is a message and document broker, not an active GPU compute hypervisor. Running AAA games directly on Telegram without external compute infrastructure is technically impossible. |
| **AetherVault Approach** | Artifact staging: download, verify SHA-256, decrypt, stage locally, launch on local device, sync saves. | Requires a dedicated remote host with an active GPU (e.g., Sunshine/Moonlight, GeForce NOW) streaming video frames via WebRTC. |

---

## 🛠️ Building & Verifying From Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/hackoilll000/AetherVault-Omni-Edition.git
   cd AetherVault-Omni-Edition
   ```
2. **Execute Local Unit & Robolectric Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **Assemble the Debug Universal APK:**
   ```bash
   gradle assembleDebug
   ```
   *Output APK will be located at `app/build/outputs/apk/debug/app-debug.apk` and `release_apk/AetherVault-Omni-v2.4-debug.apk`.*

---

## 📄 License & Intellectual Property

- **Creator & Lead Architect:** Ayush Mondal (AJPM / TRB FireOps)
- **License Type:** **AetherVault Source-Available Personal & Educational License v1.0** (See [LICENSE](LICENSE))
- **Permitted Uses:** Source code viewing, educational audit, self-hosted personal research, and local compilation for non-commercial use.
- **Restrictions:** Sublicensing, commercial monetization, unauthorized app store re-publishing (Google Play, Orion Store, F-Droid, Apple App Store), or trademark infringement without express written permission are strictly prohibited.

---
*Engineered by Ayush Mondal (AJPM) • Powered by Jetpack Compose, Media3 ExoPlayer, Python Bot Studio & Telegram Cloud Storage.*
