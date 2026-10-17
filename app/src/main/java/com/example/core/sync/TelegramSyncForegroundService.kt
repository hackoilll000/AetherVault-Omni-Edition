package com.example.core.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class TelegramSyncForegroundService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "aethervault_sync_channel"
        const val NOTIFICATION_ID = 9991
        const val ACTION_START_SYNC = "com.example.ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "com.example.ACTION_STOP_SYNC"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SYNC) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification("AetherVault Cloud Sync Active • Encrypted Backbone"))
            startLongPollingLoop()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AetherVault Telegram Backbone Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains encrypted live sync and push with Telegram Bot API"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AetherVault Omni Sync")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLongPollingLoop() {
        serviceScope.launch {
            val prefs = getSharedPreferences("aethervault_prefs", MODE_PRIVATE)
            var lastOffset = prefs.getLong("telegram_update_offset", 0L)

            while (isRunning && isActive) {
                try {
                    val token = prefs.getString("master_bot_token", "") ?: ""
                    if (token.isBlank() || token.length < 15) {
                        delay(10000L)
                        continue
                    }

                    val urlStr = "https://api.telegram.org/bot$token/getUpdates?offset=$lastOffset&timeout=25"
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 30000
                    connection.readTimeout = 35000
                    connection.requestMethod = "GET"

                    if (connection.responseCode == 200) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        if (responseText.contains("\"update_id\":")) {
                            val regex = "\"update_id\":([0-9]+)".toRegex()
                            val matches = regex.findAll(responseText)
                            val maxId = matches.map { it.groupValues[1].toLong() }.maxOrNull()
                            if (maxId != null) {
                                lastOffset = maxId + 1
                                prefs.edit().putLong("telegram_update_offset", lastOffset).apply()
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    delay(15000L)
                }
                delay(3000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceJob.cancel()
        super.onDestroy()
    }
}
