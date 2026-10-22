# AetherVault Privacy & Data Handling Specification

**Last Updated**: October 2026  
**Application Name**: AetherVault Omni Edition  
**Data Architecture**: Zero-Knowledge Client-Side Encryption (AES-256-GCM + Salted PBKDF2)

---

## Data Inventory & Privacy Classification

AetherVault is engineered with a **Strict Zero-Knowledge Architecture**. User data, master encryption keys, plaintext file contents, and filenames never leave the local Android device unencrypted.

### 1. Local Data Storage (On-Device Only)

| Data Type | Storage Mechanism | Encryption Status | Purpose |
| :--- | :--- | :--- | :--- |
| **Master Password / PIN** | Encrypted `SharedPreferences` | PBKDF2-HMAC-SHA256 Salted Hash (10,000 Iterations) | Authentication & AES-256 Key Derivation |
| **File Catalog Metadata** | Room SQLite Database (`app_database.db`) | Local Isolation & Stealth Decoy Separation | Offline Search, Virtual Folders & File History |
| **Encrypted File Cache** | Android Private App Cache (`cache/`) | Temporary AES-256-GCM Frames | Staging file chunks during upload/download |

### 2. Remote Data Transmission (Telegram Bot API)

| Data Sent | Destination | Encryption Status | Third-Party Visibility |
| :--- | :--- | :--- | :--- |
| **19 MB Encrypted Frames** | Private Telegram Channel | AES-256-GCM (256-bit Key) | Telegram servers see raw encrypted binary noise |
| **Bot API Requests** | `https://api.telegram.org` | HTTPS / TLS 1.3 | Standard Telegram Bot API transport security |

### 3. Data Collection Declaration (Google Play Data Safety)

* **Data Collected**: NONE. The app collects zero telemetry, zero analytics, zero advertising IDs, and zero personally identifiable information (PII).
* **Data Shared**: NONE. Data is transmitted exclusively to the user's self-configured private Telegram channel.
* **Account Deletion**: Users can purge all local databases and reset credential tokens instantly via **Settings -> Clear Local Cache / Reset Vault**.
