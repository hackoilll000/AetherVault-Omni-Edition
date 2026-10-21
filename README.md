<div align="center">
  <img src="https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/app_icon.svg" width="120" height="120" alt="AetherVault Omni App Icon" style="border-radius: 28px;">
  <h1>AetherVault Omni Edition (TG_PRIVATE)</h1>
  <h3>Infinite Cloud • Zero-Knowledge • Encrypted Telegram Cloud Vault</h3>
</div>

[![Android CI](https://img.shields.io/badge/Android-Kotlin_%26_Jetpack_Compose-blue?style=for-the-badge&logo=android)](https://developer.android.com)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-success?style=for-the-badge&logo=shield)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
[![Backend](https://img.shields.io/badge/Backend-Telegram_Bot_Swarm-blueviolet?style=for-the-badge&logo=telegram)](https://core.telegram.org/bots/api)
[![YouTube Channel](https://img.shields.io/badge/YouTube-TRB_FireOps-red?style=for-the-badge&logo=youtube)](https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0)
[![Release APK](https://img.shields.io/badge/Download-Release_APK-orange?style=for-the-badge&logo=android)](releases/AetherVault-Omni-v1.0.0-Debug.apk)

---

## 👨‍💻 Creator & Author Information

- **Developer & Lead Architect:** Ayush Mondal (AJPM)
- **Role:** Creator, Ethical Hacker & Cybersecurity Educator
- **YouTube Channel:** [TRB FireOps (@trbfireops)](https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0)
- **Publication Date:** October 17, 2026
- **Team / Organization:** AJPM Developer Team

---

## ⚠️ महत्वपूर्ण कानूनी डिस्क्लेमर (Legal Disclaimer & Usage Notice)

> **हिंदी (Hindi):**
> यह ऐप सिर्फ और सिर्फ पर्सनल यूज़ और एजुकेशनल डेमो के लिए बनाया गया है। हमने यह ऐप केवल सीखने और फन (Fun-Fun) के उद्देश्य से बनाया है ताकि यह दिखाया जा सके कि मोबाइल से टेलीग्राम बॉट को सर्वर की तरह कैसे ऑपरेट किया जा सकता है। हम किसी भी प्रकार से टेलीग्राम (Telegram) की नीतियों या गाइडलाइंस का उल्लंघन नहीं कर रहे हैं। इस ऐप का किसी भी प्रकार से गलत, गैर-कानूनी या अनुचित इस्तेमाल करने पर डेवलपर्स पर कोई दोष या जिम्मेदारी नहीं होगी। इस ऐप को यूज़ करना पूर्णतः आपकी स्वयं की जिम्मेदारी (Your Responsibility) है।
>
> **English:**
> This software is strictly developed for PERSONAL USE, experimental research, and educational demonstration purposes. It serves solely as an architectural prototype demonstrating how to manage bot workflows from an Android mobile client. This project does NOT violate, bypass, or advocate the breach of Telegram Terms of Service or API policies. The authors and developers assume NO LIABILITY or responsibility whatsoever for any misuse, unintended activity, or improper conduct by end users. Use of this application is at your sole discretion and responsibility.

---

## 📱 Direct APK Download

You can download the production-ready release APK directly from this repository:
📥 **[Download AetherVault Omni v1.0.0 (Debug APK)](releases/AetherVault-Omni-v1.0.0-Debug.apk)** *(~12 MB)*

---

## 🌟 Overview & Architecture

**AetherVault Omni Edition** is a state-of-the-art, zero-knowledge personal cloud storage and vault application built for Android using Kotlin and Jetpack Compose. Designed and engineered by **Ayush Mondal (AJPM)**, it transforms your private Telegram channel and Telegram Bot API into an infinite, lightning-fast, and unconditionally secure cloud storage backend without relying on third-party servers, subscription databases, or cloud providers.

All files (documents, media, archives, code snippets) are encrypted **locally on your device** using **AES-256-GCM** with unique per-file cryptographic keys before being chunked into 19MB Telegram messages and transmitted across your private bot swarm. Your master password and encryption keys never leave your device.

---

## 🎨 Advanced UI & UX (Liquid Glass & Material 3)

AetherVault features an immaculate, hyper-polished user interface inspired by futuristic iOS design and advanced cyberpunk aesthetics:

1. **Moving Aurora Background Layer:** A fixed animated canvas in the root background featuring three slow-drifting luminous glowing color orbs (`Cyan #00F2FE`, `Electric Blue #4FACFE`, and `Deep Violet #7F00FF`) with heavy Gaussian blur (`blur(90px)`).
2. **True Glassmorphism Cards:** All cards replace solid black fills with translucent frosted glass (`background: rgba(255, 255, 255, 0.06)`) combined with real backdrop blur (`backdrop-filter: blur(28px) saturate(190%)`) so the drifting aurora colors shine through dynamically.
3. **Liquid Rim Light (Border Highlighting):** Every card and button features a subtle `1px` luminous gradient border edge simulating light reflection over polished glass.
4. **Theme Engine:** Switch instantly between multiple immersive UI themes:
   - *Liquid Obsidian* (Deep dark luxury obsidian with neon cyan accents)
   - *Cyberpunk Neon* (Vibrant cyberpunk electric pink and high-contrast violet)
   - *Minimalist Quartz* (Clean ultra-light silver glass)

---

## 📸 Complete App UI & Interface Gallery (Assets & Raw URLs)

### 1. Vault Dashboard & Telemetry HUD
![Dashboard Mockup](https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_dashboard.svg)
- **Header:** Live connection status indicator (Connected / Disconnected dot), quick navigation tabs, and system status.
- **Bot Swarm Health Card:** Real-time ping latency, active bot count, and encrypted packet throughput.
- **Storage Ring Meter:** Visual MB Used vs Total Cloud Capacity ring meter.
- **Quick Action Buttons:** *Upload File*, *New Virtual Folder*, *TeleDrop Sync*, and *Vault Security Lock*.

### 2. Encrypted Drive Explorer & Virtual Folders
![Drive Explorer Mockup](https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_drive.svg)
- **Breadcrumb Navigation:** Clean breadcrumb trail (`Home / Folder Name`) with instant tap-to-return navigation.
- **Folder Grid & List Cards:** Custom color accents, icon selectors, item counts, rename dialogs, and instant delete buttons.
- **Move to Folder:** Batch or single-file context menu with "Move to Folder" picker.
- **File Management:** Encrypted file preview, SHA-256 integrity verification, offline pinning, and remote Telegram message deletion.

### 3. Photos Timeline & SHA-256 Integrity Verification
![Photos Timeline Mockup](https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_photos.svg)
- **Media Stream:** Auto-indexed photo and video gallery grid with instant thumbnail loading via Coil.
- **Integrity Badge:** Cryptographic SHA-256 hash validation badge for every photo.
- **Free Up Space Utility:** One-tap local cache and original photo cleanup once safely uploaded to Telegram Cloud.

---

## 🔗 Direct Raw Asset URLs for Submission
- **App Icon (`assets/app_icon.svg`):** `https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/app_icon.svg`
- **Dashboard Screenshot (`assets/screenshot_dashboard.svg`):** `https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_dashboard.svg`
- **Drive Explorer Screenshot (`assets/screenshot_drive.svg`):** `https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_drive.svg`
- **Photos Timeline Screenshot (`assets/screenshot_photos.svg`):** `https://raw.githubusercontent.com/hackoilll000/AetherVault-Omni-Edition/main/assets/screenshot_photos.svg`

---

## 🚀 Building From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/hackoilll000/AetherVault-Omni-Edition.git
   ```
2. Open the project in Android Studio (Iguana / Jellyfish or newer).
3. Build the project or generate the APK:
   ```bash
   gradle assembleDebug
   ```

---

## 📄 License & Attribution

- **Creator:** Ayush Mondal (AJPM)
- **YouTube:** [TRB FireOps (@trbfireops)](https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0)
- **Published:** October 2026
- **License:** Distributed under the MIT License. See `LICENSE` for more information.

---
*Engineered by Ayush Mondal (AJPM) • Powered by Telegram Bot Swarm & AES-256-GCM.*
