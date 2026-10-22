#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# AetherVault Bot Studio - Bot Startup Runner
# =======================================================

if [ ! -f "bot.py" ]; then
    echo "❌ Error: bot.py not found in current directory!"
    exit 1
fi

if [ -f "requirements.txt" ]; then
    pip install -r requirements.txt
fi

echo "🤖 Starting AetherVault Telegram Bot..."
python bot.py
