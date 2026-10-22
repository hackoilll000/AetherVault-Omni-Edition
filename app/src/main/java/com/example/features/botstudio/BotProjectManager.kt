package com.example.features.botstudio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class BotProjectManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aethervault_bot_studio", Context.MODE_PRIVATE)

    private val _projectsFlow = MutableStateFlow<List<BotProject>>(emptyList())
    val projectsFlow: StateFlow<List<BotProject>> = _projectsFlow.asStateFlow()

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        val jsonStr = prefs.getString("bot_projects_json", "[]") ?: "[]"
        val projects = mutableListOf<BotProject>()
        runCatching {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                projects.add(parseProjectFromJson(obj))
            }
        }
        if (projects.isEmpty()) {
            val defaultProject = BotProject(
                name = "Sample Telegram Echo Bot",
                description = "Self-hosted Python bot template for Termux & Linux"
            )
            projects.add(defaultProject)
            saveProjectsToStorage(projects)
        }
        _projectsFlow.value = projects
    }

    private fun parseProjectFromJson(obj: JSONObject): BotProject {
        val envList = mutableListOf<BotEnvVar>()
        if (obj.has("envVars")) {
            val envArr = obj.getJSONArray("envVars")
            for (j in 0 until envArr.length()) {
                val envObj = envArr.getJSONObject(j)
                envList.add(
                    BotEnvVar(
                        key = envObj.optString("key", ""),
                        value = envObj.optString("value", ""),
                        isSecret = envObj.optBoolean("isSecret", true)
                    )
                )
            }
        }

        var aiConfig = AiConfig()
        if (obj.has("aiConfig")) {
            val aiObj = obj.getJSONObject("aiConfig")
            aiConfig = AiConfig(
                enabled = aiObj.optBoolean("enabled", false),
                provider = aiObj.optString("provider", "Gemini"),
                apiKey = aiObj.optString("apiKey", ""),
                modelName = aiObj.optString("modelName", "gemini-1.5-flash"),
                systemPrompt = aiObj.optString("systemPrompt", "You are a helpful Telegram assistant."),
                allowDatabaseTool = aiObj.optBoolean("allowDatabaseTool", false),
                allowWebSearchTool = aiObj.optBoolean("allowWebSearchTool", false)
            )
        }

        var dbConfig = DatabaseConfig()
        if (obj.has("databaseConfig")) {
            val dbObj = obj.getJSONObject("databaseConfig")
            dbConfig = DatabaseConfig(
                enabled = dbObj.optBoolean("enabled", false),
                dbType = dbObj.optString("dbType", "SQLite"),
                host = dbObj.optString("host", "127.0.0.1"),
                port = dbObj.optInt("port", 5432),
                dbName = dbObj.optString("dbName", "app_db"),
                username = dbObj.optString("username", "bot_user"),
                passwordSecret = dbObj.optString("passwordSecret", ""),
                readOnlyMode = dbObj.optBoolean("readOnlyMode", true),
                allowedTables = dbObj.optString("allowedTables", "users, logs")
            )
        }

        return BotProject(
            id = obj.optString("id", System.currentTimeMillis().toString()),
            name = obj.optString("name", "Telegram Bot"),
            description = obj.optString("description", ""),
            botTokenSecret = obj.optString("botTokenSecret", ""),
            channelId = obj.optString("channelId", ""),
            pythonScript = obj.optString("pythonScript", DEFAULT_PYTHON_ECHO_SCRIPT),
            envVars = envList,
            status = runCatching { BotStatus.valueOf(obj.optString("status", BotStatus.SAVED.name)) }.getOrDefault(BotStatus.SAVED),
            aiConfig = aiConfig,
            databaseConfig = dbConfig,
            createdAtTimestamp = obj.optLong("createdAtTimestamp", System.currentTimeMillis()),
            lastModifiedTimestamp = obj.optLong("lastModifiedTimestamp", System.currentTimeMillis())
        )
    }

    private fun saveProjectsToStorage(projects: List<BotProject>) {
        val jsonArray = JSONArray()
        for (p in projects) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("botTokenSecret", p.botTokenSecret)
            obj.put("channelId", p.channelId)
            obj.put("pythonScript", p.pythonScript)
            obj.put("status", p.status.name)
            obj.put("createdAtTimestamp", p.createdAtTimestamp)
            obj.put("lastModifiedTimestamp", p.lastModifiedTimestamp)

            val envArr = JSONArray()
            for (e in p.envVars) {
                val eObj = JSONObject()
                eObj.put("key", e.key)
                eObj.put("value", e.value)
                eObj.put("isSecret", e.isSecret)
                envArr.put(eObj)
            }
            obj.put("envVars", envArr)

            val aiObj = JSONObject()
            aiObj.put("enabled", p.aiConfig.enabled)
            aiObj.put("provider", p.aiConfig.provider)
            aiObj.put("apiKey", p.aiConfig.apiKey)
            aiObj.put("modelName", p.aiConfig.modelName)
            aiObj.put("systemPrompt", p.aiConfig.systemPrompt)
            aiObj.put("allowDatabaseTool", p.aiConfig.allowDatabaseTool)
            aiObj.put("allowWebSearchTool", p.aiConfig.allowWebSearchTool)
            obj.put("aiConfig", aiObj)

            val dbObj = JSONObject()
            dbObj.put("enabled", p.databaseConfig.enabled)
            dbObj.put("dbType", p.databaseConfig.dbType)
            dbObj.put("host", p.databaseConfig.host)
            dbObj.put("port", p.databaseConfig.port)
            dbObj.put("dbName", p.databaseConfig.dbName)
            dbObj.put("username", p.databaseConfig.username)
            dbObj.put("passwordSecret", p.databaseConfig.passwordSecret)
            dbObj.put("readOnlyMode", p.databaseConfig.readOnlyMode)
            dbObj.put("allowedTables", p.databaseConfig.allowedTables)
            obj.put("databaseConfig", dbObj)

            jsonArray.put(obj)
        }
        prefs.edit().putString("bot_projects_json", jsonArray.toString()).apply()
    }

    fun saveProject(project: BotProject) {
        val current = _projectsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        val updatedProject = project.copy(lastModifiedTimestamp = System.currentTimeMillis())
        if (index >= 0) {
            current[index] = updatedProject
        } else {
            current.add(updatedProject)
        }
        _projectsFlow.value = current
        saveProjectsToStorage(current)
        appendLog("Saved bot project '${project.name}' successfully.")
    }

    fun deleteProject(projectId: String) {
        val current = _projectsFlow.value.filterNot { it.id == projectId }
        _projectsFlow.value = current
        saveProjectsToStorage(current)
        appendLog("Deleted project ID: $projectId.")
    }

    fun validatePythonScript(script: String): Pair<Boolean, String> {
        val openParens = script.count { it == '(' } - script.count { it == ')' }
        val openBrackets = script.count { it == '[' } - script.count { it == ']' }
        val openBraces = script.count { it == '{' } - script.count { it == '}' }

        if (openParens != 0) return Pair(false, "Syntax Error: Unmatched parentheses '()'")
        if (openBrackets != 0) return Pair(false, "Syntax Error: Unmatched square brackets '[]'")
        if (openBraces != 0) return Pair(false, "Syntax Error: Unmatched curly braces '{}'")

        val lines = script.lines()
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("def ") || trimmed.startsWith("async def ") || trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ") || trimmed.startsWith("class ")) {
                if (!trimmed.endsWith(":")) {
                    return Pair(false, "Syntax Warning at Line ${index + 1}: Missing colon ':' at end of statement block")
                }
            }
        }

        return Pair(true, "Python Script Syntax Check Passed. Ready for Termux / Server deployment.")
    }

    fun generateTermuxSetupScript(project: BotProject): String {
        return """
#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# AetherVault Bot Studio - Termux Deployment Script
# Project: ${project.name}
# =======================================================

echo "🚀 Setting up AetherVault Python Bot Environment in Termux..."

pkg update && pkg upgrade -y
pkg install python python-pip git -y

mkdir -p ~/aethervault_bots/${project.name.lowercase().replace(" ", "_")}
cd ~/aethervault_bots/${project.name.lowercase().replace(" ", "_")}

cat << 'EOF' > requirements.txt
python-telegram-bot>=20.0
requests
google-generativeai
python-dotenv
EOF

pip install -r requirements.txt

cat << 'EOF' > .env
BOT_TOKEN=${project.botTokenSecret}
CHANNEL_ID=${project.channelId}
AI_API_KEY=${project.aiConfig.apiKey}
DB_HOST=${project.databaseConfig.host}
EOF

cat << 'EOF' > bot.py
${project.pythonScript}
EOF

echo "✅ Setup Complete! Starting bot in background..."
python bot.py
""".trimIndent()
    }

    fun generateEnvTemplate(project: BotProject): String {
        val sb = StringBuilder()
        sb.appendLine("# AetherVault Bot Studio Environment Variables")
        sb.appendLine("BOT_TOKEN=${redactSecret(project.botTokenSecret)}")
        sb.appendLine("TELEGRAM_CHANNEL_ID=${project.channelId}")
        if (project.aiConfig.enabled) {
            sb.appendLine("AI_PROVIDER=${project.aiConfig.provider}")
            sb.appendLine("AI_API_KEY=${redactSecret(project.aiConfig.apiKey)}")
            sb.appendLine("AI_MODEL=${project.aiConfig.modelName}")
        }
        if (project.databaseConfig.enabled) {
            sb.appendLine("DB_TYPE=${project.databaseConfig.dbType}")
            sb.appendLine("DB_HOST=${project.databaseConfig.host}")
            sb.appendLine("DB_PORT=${project.databaseConfig.port}")
            sb.appendLine("DB_NAME=${project.databaseConfig.dbName}")
            sb.appendLine("DB_USER=${project.databaseConfig.username}")
        }
        for (env in project.envVars) {
            val valStr = if (env.isSecret) redactSecret(env.value) else env.value
            sb.appendLine("${env.key}=$valStr")
        }
        return sb.toString()
    }

    fun appendLog(message: String) {
        val sanitized = redactAllSecretsInText(message)
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$timestamp] $sanitized"
        val currentLogs = _logsFlow.value.toMutableList()
        currentLogs.add(0, entry)
        if (currentLogs.size > 200) currentLogs.removeAt(currentLogs.lastIndex)
        _logsFlow.value = currentLogs
    }

    private fun redactSecret(secret: String): String {
        if (secret.isBlank()) return ""
        if (secret.length <= 8) return "********"
        return secret.take(4) + "..." + secret.takeLast(4)
    }

    private fun redactAllSecretsInText(text: String): String {
        var result = text
        for (p in _projectsFlow.value) {
            if (p.botTokenSecret.isNotBlank() && p.botTokenSecret.length > 5) {
                result = result.replace(p.botTokenSecret, redactSecret(p.botTokenSecret))
            }
            if (p.aiConfig.apiKey.isNotBlank() && p.aiConfig.apiKey.length > 5) {
                result = result.replace(p.aiConfig.apiKey, redactSecret(p.aiConfig.apiKey))
            }
            if (p.databaseConfig.passwordSecret.isNotBlank() && p.databaseConfig.passwordSecret.length > 5) {
                result = result.replace(p.databaseConfig.passwordSecret, redactSecret(p.databaseConfig.passwordSecret))
            }
        }
        return result
    }

    fun loadTemplate(type: BotTemplateType): String {
        return when (type) {
            BotTemplateType.ECHO_BOT -> DEFAULT_PYTHON_ECHO_SCRIPT
            BotTemplateType.FAQ_BOT -> """
# AetherVault Bot Studio - FAQ & Command Bot
import os, sys, logging
from telegram import Update
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes

BOT_TOKEN = os.getenv("BOT_TOKEN")

async fun help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("📋 AetherVault Commands:\n/start - Greeting\n/faq - Frequently Asked Questions\n/rules - Usage Policies")

async fun faq_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("❓ FAQ:\nQ: Is storage private?\nA: Yes, encrypted via AES-256 on your own Telegram bot/channel!")

def main():
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", help_command))
    app.add_handler(CommandHandler("faq", faq_command))
    app.run_polling()

if __name__ == "__main__":
    main()
""".trimIndent()
            BotTemplateType.MODERATION_BOT -> """
# AetherVault Bot Studio - Group Moderation Bot
import os, logging
from telegram import Update
from telegram.ext import ApplicationBuilder, MessageHandler, filters, ContextTypes

BOT_TOKEN = os.getenv("BOT_TOKEN")
SPAM_KEYWORDS = ["http://spam.link", "free crypto", "telegram.me/scam"]

async fun filter_spam(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text.lower()
    if any(keyword in text for keyword in SPAM_KEYWORDS):
        await update.message.delete()
        await update.message.reply_text("⚠️ Warning: Spam message removed automatically.")

def main():
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(MessageHandler(filters.TEXT & (~filters.COMMAND), filter_spam))
    app.run_polling()

if __name__ == "__main__":
    main()
""".trimIndent()
            BotTemplateType.REMINDER_BOT -> """
# AetherVault Bot Studio - Scheduled Reminder Bot
import os, asyncio
from telegram import Update
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes

BOT_TOKEN = os.getenv("BOT_TOKEN")

async fun set_timer(update: Update, context: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_message.chat_id
    await update.message.reply_text("⏱️ Timer set for 10 seconds...")
    await asyncio.sleep(10)
    await context.bot.send_message(chat_id=chat_id, text="⏰ Time's up! Reminder from AetherVault.")

def main():
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("timer", set_timer))
    app.run_polling()

if __name__ == "__main__":
    main()
""".trimIndent()
            BotTemplateType.AI_ASSISTANT -> """
# AetherVault Bot Studio - Gemini AI Assistant Bot
import os, logging
import google.generativeai as genai
from telegram import Update
from telegram.ext import ApplicationBuilder, MessageHandler, filters, ContextTypes

BOT_TOKEN = os.getenv("BOT_TOKEN")
AI_API_KEY = os.getenv("AI_API_KEY")

genai.configure(api_key=AI_API_KEY)
model = genai.GenerativeModel('gemini-1.5-flash')

async fun ai_reply(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_prompt = update.message.text
    response = model.generate_content(user_prompt)
    await update.message.reply_text(response.text)

def main():
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(MessageHandler(filters.TEXT & (~filters.COMMAND), ai_reply))
    app.run_polling()

if __name__ == "__main__":
    main()
""".trimIndent()
            BotTemplateType.WEBHOOK_INTEGRATION -> """
# AetherVault Bot Studio - Webhook Listener Bot
import os
from flask import Flask, request
import requests

BOT_TOKEN = os.getenv("BOT_TOKEN")
CHANNEL_ID = os.getenv("CHANNEL_ID")
app = Flask(__name__)

@app.route('/webhook', methods=['POST'])
def webhook():
    data = request.json
    msg = f"🔔 Webhook Event Received:\n{data}"
    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    requests.post(url, json={"chat_id": CHANNEL_ID, "text": msg})
    return {"status": "ok"}, 200

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
""".trimIndent()
        }
    }
}
