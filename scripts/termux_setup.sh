#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# AetherVault Bot Studio - Termux Environment Initializer
# =======================================================

echo "🚀 Initializing Termux Environment for AetherVault Bots..."

pkg update && pkg upgrade -y
pkg install python python-pip git -y

mkdir -p ~/aethervault_bots
cd ~/aethervault_bots

echo "✅ Termux environment initialized! Create your bot project in AetherVault Bot Studio."
