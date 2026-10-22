# AetherVault Self-Hosted & Termux Deployment Guide

This document provides step-by-step instructions for hosting your own Python Telegram bot using **Termux** on Android or any self-hosted Linux/macOS/Windows machine, without relying on developer-hosted servers.

---

## 1. Zero-Developer-Server Architecture

AetherVault is engineered so that **you own 100% of your bot infrastructure and credentials**:
- **No Developer Servers**: The application developer operates zero bot hosting servers, Python runners, or cloud databases.
- **Direct Telegram Connection**: Your bot communicates directly with Telegram's official Bot API servers (`https://api.telegram.org`).
- **Encrypted Local Storage**: Your credentials, bot tokens, and master passwords remain on your local device.

---

## 2. Termux Setup on Android

Termux allows you to execute your Python Telegram bots natively on your Android device.

### Prerequisites
- Android 7.0 or higher.
- Termux app installed (recommended from F-Droid or GitHub Releases).

### Step-by-Step Installation
1. **Open Termux** on your Android device.
2. **Copy & Run the Setup Command** generated directly from AetherVault's **Bot Studio -> Termux Deployer** tab:

```bash
pkg update && pkg upgrade -y
pkg install python python-pip git -y

mkdir -p ~/aethervault_bots
cd ~/aethervault_bots

cat << 'EOF' > requirements.txt
python-telegram-bot>=20.0
requests
google-generativeai
python-dotenv
EOF

pip install -r requirements.txt
```

3. **Configure Environment Variables**:
Create a `.env` file containing your Telegram Bot Token and credentials:
```bash
cat << 'EOF' > .env
BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
CHANNEL_ID=-1001234567890
AI_API_KEY=your_optional_gemini_api_key
EOF
```

4. **Start Your Bot**:
```bash
python bot.py
```

---

## 3. Managing Android Battery Optimization & Background Execution

Android's Doze Mode and vendor battery savers (Samsung, Xiaomi, OnePlus) may stop background processes after a period of inactivity.

To ensure continuous bot polling in Termux:
1. **Disable Battery Optimization for Termux**:
   - Go to `Settings -> Apps -> Termux -> Battery`.
   - Set to **Unrestricted** / **No Optimization**.
2. **Enable Termux Wake Lock**:
   - Run `termux-wake-lock` inside Termux to prevent CPU sleep.
3. **Use Termux:Boot (Optional)**:
   - Install `Termux:Boot` extension to automatically start your bot whenever your Android phone boots up.

---

## 4. Deploying to User-Controlled Servers / VPS

If you prefer 24/7 cloud availability without battery limits, you can deploy your exported Bot Studio project to any server (Docker, AWS, Hetzner, Raspberry Pi):

### Docker Deployment Example
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "bot.py"]
```

Run container:
```bash
docker build -t my-telegram-bot .
docker run -d --name my-bot --env-file .env my-telegram-bot
```

---

## 5. Security & Privacy Guarantees

- **Credential Isolation**: Bot tokens and secrets are never exported in cleartext in standard app backups.
- **Log Redaction**: Bot Studio automatically redacts API keys and tokens before rendering output logs.
- **Least-Privilege Execution**: Code executed via Termux runs within Termux's unprivileged user sandbox.
