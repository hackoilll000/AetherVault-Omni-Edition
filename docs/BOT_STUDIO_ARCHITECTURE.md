# Python Bot Studio Architecture Specification

This document details the architectural design of AetherVault's **Python Bot Studio**.

---

## 1. Executive Overview

Python Bot Studio provides an on-device environment for designing, configuring, testing, and exporting Python Telegram bots.

```
+-------------------------------------------------------------+
|                     AetherVault Android App                 |
|  +-------------------------------------------------------+  |
|  |                Bot Studio UI & Control Panel          |  |
|  |  [Editor] [Syntax Check] [Termux Deployer] [AI Config]|  |
|  +---------------------------+---------------------------+  |
|                              |                              |
|                              v                              |
|             +---------------------------------+             |
|             | BotProjectManager & Storage     |             |
|             |  - Credential Redaction         |             |
|             |  - Termux Script Generator      |             |
|             |  - JSON/ZIP Package Exporter    |             |
|             +---------------------------------+             |
+------------------------------+------------------------------+
                               | (Local Export / Shell Exec)
                               v
+-------------------------------------------------------------+
|               Isolated User Execution Environment           |
|            (Termux / User VPS / Self-Hosted Docker)         |
|  +-------------------------------------------------------+  |
|  |                  Python 3 Runtime                     |  |
|  |  - python-telegram-bot (v20+)                         |  |
|  |  - Optional Gemini AI / Database Connectors           |  |
|  +---------------------------+---------------------------+  |
+------------------------------|------------------------------+
                               |
                               v
                 Official Telegram Bot API
               (https://api.telegram.org)
```

---

## 2. Core Components

### 2.1 Bot Project Manager (`BotProjectManager.kt`)
- **Persistence**: Projects are saved locally in encrypted application storage.
- **Syntax Validator**: Performs structural linting for matching brackets, colons, and Python block definitions.
- **Log Sanitizer**: Intercepts console logs and redacts bot tokens, API keys, and passwords before UI rendering.

### 2.2 Template Library
Included templates:
1. **Echo Bot**: Baseline message echoing template.
2. **FAQ Bot**: Custom slash command handler with interactive FAQ answers.
3. **Group Moderation Bot**: Automated spam keyword detection and deletion.
4. **Scheduled Reminder Bot**: Async task scheduling for periodic alerts.
5. **AI Assistant Bot**: Direct integration with Google Gemini / OpenAI.
6. **Webhook Bot**: HTTP endpoint listener forwarding events to Telegram.

---

## 3. Security & Runtime Isolation

1. **Main Process Isolation**: Arbitrary user Python code is NEVER executed inside the main Android application process.
2. **Termux / Container Sandbox**: Code execution takes place inside Termux's unprivileged user environment or isolated Docker containers.
3. **Secret Redaction**: Any text matching token patterns (`[0-9]+:[A-Za-z0-9_-]+`) is redacted automatically.
