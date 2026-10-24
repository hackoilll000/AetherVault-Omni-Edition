# Security Policy

## Supported Versions

| Version | Supported | Security Maintenance |
| :--- | :--- | :--- |
| 2.4.x-OMNI | :white_check_mark: | Active Security Patches & Fixes |
| 2.x | :white_check_mark: | Critical CVE Fixes Only |
| < 2.0 | :x: | Unsupported |

---

## Zero-Knowledge Architecture & Threat Model

AetherVault Omni Edition enforces an on-device Zero-Knowledge security model:
1. **End-to-End Client Encryption**: All files and chunks are encrypted locally using AES-256-GCM before transmission to Telegram servers.
2. **Master Key Derivation**: Keys are derived using PBKDF2 with SHA-256 (100,000+ iterations) and stored in Android Keystore / encrypted SharedPreferences.
3. **No Central Key Server**: Telegram servers only store encrypted binary payloads; Telegram cannot view file contents, names, or metadata.
4. **Credential Isolation**: Telegram Bot Tokens and Channel IDs are stored exclusively on-device in private application sandboxes. No developer server ever receives or logs user tokens.

---

## Reporting a Vulnerability

If you discover a security vulnerability or cryptographic flaw in AetherVault Omni Edition:
1. **Do NOT open a public issue.**
2. Send a confidential report to `security@aethervault.local` or contact the lead architect via private channel.
3. Include:
   - Vulnerability classification (e.g. key derivation weakness, replay attack, SSRF in RemoteFetch).
   - Minimal reproduction proof-of-concept.
   - Potential impact assessment.
4. We acknowledge receipt within 48 hours and work with you on coordinated disclosure.
