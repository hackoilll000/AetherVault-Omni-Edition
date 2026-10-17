# AetherVault Omni Edition (TG_PRIVATE)
### Infinite Cloud • Zero-Knowledge • Encrypted Telegram Cloud Vault

[![Android CI](https://img.shields.io/badge/Android-Kotlin_%26_Jetpack_Compose-blue?style=for-the-badge&logo=android)](https://developer.android.com)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-success?style=for-the-badge&logo=shield)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
[![Backend](https://img.shields.io/badge/Backend-Telegram_Bot_Swarm-blueviolet?style=for-the-badge&logo=telegram)](https://core.telegram.org/bots/api)
[![Release APK](https://img.shields.io/badge/Download-Release_APK-orange?style=for-the-badge&logo=android)](releases/AetherVault-Omni-v1.0.0-Debug.apk)

---

## 📱 Direct APK Download

You can download the production-ready release APK directly from this repository:
📥 **[Download AetherVault Omni v1.0.0 (Debug APK)](releases/AetherVault-Omni-v1.0.0-Debug.apk)** *(~12 MB)*

---

## 🌟 Overview

**AetherVault Omni Edition** is a state-of-the-art, zero-knowledge personal cloud storage and vault application for Android. It transforms your private Telegram channel and Telegram Bot API into an infinite, lightning-fast, and unconditionally secure cloud storage backend without relying on third-party servers, subscription databases, or cloud providers.

All files (documents, media, archives, code snippets) are encrypted **locally on your device** using **AES-256-GCM** with unique per-file cryptographic keys before being chunked into 19MB Telegram messages and transmitted across your private bot swarm. Your master password and encryption keys never leave your device.

---

## 📸 Complete App UI & Interface Gallery

### 1. Vault Dashboard & Telemetry HUD
![Dashboard Mockup](screenshots/dashboard.svg)
*Real-time bot swarm health monitoring, live transfer speeds, storage usage metrics, and quick vault action triggers.*

### 2. Encrypted Drive Explorer & Virtual Folders
![Drive Explorer Mockup](screenshots/drive.svg)
*Hierarchical folder management, breadcrumb navigation, custom folder coloring, batch file operations, and instant cloud deletion.*

### 3. Photos Timeline & SHA-256 Integrity Verification
![Photos Timeline Mockup](screenshots/photos.svg)
*Auto-indexed photo and video stream with SHA-256 cryptographic verification and the "Free Up Space" local storage cleaner.*

### 4. Appearance & HUD Studio
![Appearance Studio Mockup](screenshots/appearance.svg)
*Multi-UI theme engine (Liquid Obsidian, Cyberpunk Neon, Minimalist Quartz), fluid corner radius controls, telemetry toggle, and English/Hindi language switcher.*

---

## ⚙️ Core Functions & How They Work

### 1. Zero-Knowledge Client-Side Encryption (AES-256-GCM)
- **How it works:** When you select any file, the app generates a secure cryptographic key and slices the file into optimal 19MB binary chunks. Each chunk is encrypted locally using AES-256-GCM authenticated encryption.
- **Privacy Guarantee:** Even if Telegram servers or bots are compromised, data remains completely unreadable cipher-text without your master password or Zero-Knowledge share ticket.

### 2. Telegram Bot Swarm & Channel Engine
- **How it works:** AetherVault supports multi-bot load balancing (Bot Swarm) to distribute chunk uploads across multiple Telegram bot tokens and channel IDs.
- **Connection Test Ping:** Instantly verifies channel write permissions and bot token validity with real-time feedback.

### 3. Encrypted Vault Drive & Virtual Folders
- **How it works:** Organize your cloud storage into a rich hierarchical folder structure.
- **Features:** Create folders with custom color accents, navigate sub-folders with breadcrumb trails, rename/delete folders, move single or batch files across folders, and perform instant permanent deletion (which removes both local records and remote Telegram cloud messages simultaneously).

### 4. Photos Timeline & SHA-256 Integrity Verification
- **How it works:** Automatically indexes photos and videos with high-performance grid views.
- **Features:** Cryptographic SHA-256 verification checks ensure your cloud media is intact. Use the "Free Up Space" utility to safely clear local device copies once verified in Telegram Cloud.

### 5. TeleDrop (Instant P2P Snippets & Text Sharing)
- **How it works:** A lightning-fast clipboard and text snippet drop zone. Type, paste, or drop code snippets, passwords, and notes to instantly sync them across your private Telegram drop channel.

### 6. Appearance & HUD Studio (Multi-UI Theme Engine)
- **How it works:** Fully customizable visual workspace featuring themes, HUD telemetry overlay, and dual-language support.

### 7. Security & Credential Protection
- **How it works:** Master PIN / Password vault lock with quick biometric unlock options.
- **Masking:** All sensitive bot tokens and channel IDs are automatically masked with bullets (`••••••••`) with eye-toggle reveal controls to protect your credentials from over-the-shoulder visibility.

---

## 🛠️ Architecture & Tech Stack

- **Language:** 100% Kotlin (Coroutines & Flows)
- **UI Framework:** Jetpack Compose & Material Design 3 (M3)
- **Local Persistence:** Room Database (SQLite) with encrypted local preferences
- **Network / API:** OkHttp & Retrofit communicating directly with Telegram Bot API
- **Concurrency:** Kotlin Coroutines (`Dispatchers.IO`) for seamless background chunking and streaming

---

## 🚀 Building From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AetherVault-Omni-Edition.git
   ```
2. Open the project in Android Studio (Iguana / Jellyfish or newer).
3. Build the project or generate the APK:
   ```bash
   gradle assembleDebug
   ```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with precision engineering and high-end design aesthetics.*
