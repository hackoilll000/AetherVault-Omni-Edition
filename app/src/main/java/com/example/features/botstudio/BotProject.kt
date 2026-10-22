package com.example.features.botstudio

import java.util.UUID

enum class BotStatus {
    SAVED,
    VALIDATING,
    READY_FOR_TERMUX,
    RUNNING,
    STOPPED,
    FAILED
}

enum class BotTemplateType(val displayName: String, val description: String) {
    ECHO_BOT("Echo Bot", "Simple echo bot that replies to messages"),
    FAQ_BOT("FAQ & Command Bot", "Bot that answers standard questions with custom commands"),
    MODERATION_BOT("Group Moderation Bot", "Deletes spam messages and manages user warnings"),
    REMINDER_BOT("Scheduled Reminder Bot", "Sends periodic reminders and alerts to Telegram chat"),
    AI_ASSISTANT("AI Assistant Bot", "Connects to Gemini/OpenAI API with tool permissions"),
    WEBHOOK_INTEGRATION("Webhook Automation Bot", "Listens for external HTTP webhooks and forwards notifications")
}

data class BotEnvVar(
    val key: String,
    val value: String,
    val isSecret: Boolean = true
)

data class AiConfig(
    val enabled: Boolean = false,
    val provider: String = "Gemini", // Gemini, OpenAI, Anthropic
    val apiKey: String = "",
    val modelName: String = "gemini-1.5-flash",
    val systemPrompt: String = "You are a helpful Telegram assistant.",
    val allowDatabaseTool: Boolean = false,
    val allowWebSearchTool: Boolean = false
)

data class DatabaseConfig(
    val enabled: Boolean = false,
    val dbType: String = "SQLite", // SQLite, PostgreSQL, MySQL
    val host: String = "127.0.0.1",
    val port: Int = 5432,
    val dbName: String = "app_db",
    val username: String = "bot_user",
    val passwordSecret: String = "",
    val readOnlyMode: Boolean = true,
    val allowedTables: String = "users, logs, messages"
)

data class BotProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Telegram Bot",
    val description: String = "Self-hosted Python bot for Telegram",
    val botTokenSecret: String = "",
    val channelId: String = "",
    val pythonScript: String = DEFAULT_PYTHON_ECHO_SCRIPT,
    val envVars: List<BotEnvVar> = listOf(),
    val status: BotStatus = BotStatus.SAVED,
    val aiConfig: AiConfig = AiConfig(),
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val lastModifiedTimestamp: Long = System.currentTimeMillis()
)

val DEFAULT_PYTHON_ECHO_SCRIPT = """
# AetherVault Bot Studio - Echo Bot Starter Template
# Target Runtime: Python 3.10+ (Termux / Linux / macOS / Windows)

import os
import sys
import logging
from telegram import Update
from telegram.ext import ApplicationBuilder, CommandHandler, MessageHandler, filters, ContextTypes

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)

BOT_TOKEN = os.getenv("BOT_TOKEN")

if not BOT_TOKEN:
    print("ERROR: BOT_TOKEN environment variable not found! Configure it in Bot Studio.")
    sys.exit(1)

async fun_start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_name = update.effective_user.first_name if update.effective_user else "User"
    await update.message.reply_text(f"👋 Hello {user_name}! AetherVault Self-Hosted Bot is online!")

async fun_echo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text
    await update.message.reply_text(f"🔁 Echo: {text}")

def main():
    print("Starting AetherVault Self-Hosted Bot...")
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", fun_start))
    app.add_handler(MessageHandler(filters.TEXT & (~filters.COMMAND), fun_echo))
    print("Bot is polling updates. Press Ctrl+C to stop.")
    app.run_polling()

if __name__ == "__main__":
    main()
""".trimIndent()
