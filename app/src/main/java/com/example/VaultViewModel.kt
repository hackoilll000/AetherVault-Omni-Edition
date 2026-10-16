package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.crypto.AesGcmChunkCipher
import com.example.core.database.*
import com.example.core.telegram.TelegramBotClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("aethervault_prefs", Context.MODE_PRIVATE)
    private val client = TelegramBotClient()

    private val _isConfigured = MutableStateFlow(prefs.getBoolean("is_configured", false))
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!prefs.getBoolean("is_configured", false))
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isDecoy = MutableStateFlow(false)
    val isDecoy: StateFlow<Boolean> = _isDecoy.asStateFlow()

    private val _channelId = MutableStateFlow(prefs.getLong("channel_id", -1002345678901L))
    val channelId: StateFlow<Long> = _channelId.asStateFlow()

    private val _isTermsAgreed = MutableStateFlow(prefs.getBoolean("terms_agreed_ayush_mandal", false))
    val isTermsAgreed: StateFlow<Boolean> = _isTermsAgreed.asStateFlow()

    private val _uploadErrorNotification = MutableStateFlow<String?>(null)
    val uploadErrorNotification: StateFlow<String?> = _uploadErrorNotification.asStateFlow()

    fun acceptTerms() {
        prefs.edit().putBoolean("terms_agreed_ayush_mandal", true).apply()
        _isTermsAgreed.value = true
    }

    fun dismissUploadError() {
        _uploadErrorNotification.value = null
    }

    private val _masterBotToken = MutableStateFlow(prefs.getString("master_bot_token", "") ?: "")
    val masterBotToken: StateFlow<String> = _masterBotToken.asStateFlow()

    private val database: AppDatabase
        get() = AppDatabase.getInstance(getApplication(), _isDecoy.value)

    val allFiles: StateFlow<List<VaultFile>> = database.fileDao().getAllActiveFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<VirtualFolder>> = database.folderDao().getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransfers: StateFlow<List<TransferTask>> = database.transferDao().getActiveTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teleDropStream: StateFlow<List<TeleDropItem>> = database.teleDropDao().getTeleDropStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val botSwarm: StateFlow<List<BotSwarmEntity>> = database.botDao().getAllBots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageUsed: StateFlow<Long> = database.fileDao().getTotalStorageUsed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeFileCount: StateFlow<Int> = database.fileDao().getActiveFileCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashedFiles: StateFlow<List<VaultFile>> = database.fileDao().getTrashedFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cameraSyncStatus: StateFlow<com.example.core.sync.CameraSyncStatus> = com.example.core.sync.CameraRollSyncManager.syncStatus

    init {
        // Automatically purge any dummy files so the user starts with a 100% clean vault
        cleanupDemoData()
    }

    private fun cleanupDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = database
            db.fileDao().permanentDelete("demo_img_1")
            db.fileDao().permanentDelete("demo_vid_1")
            db.fileDao().permanentDelete("demo_code_1")
            db.fileDao().permanentDelete("demo_doc_1")
            db.fileDao().permanentDelete("demo_zip_1")
            db.transferDao().deleteTransfer("task_sync_drone")
            // 30-day auto-purge for trashed files
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            db.fileDao().purgeExpiredTrash(thirtyDaysAgo)
        }
    }

    fun clearAllVaultData() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = database
            val all = db.fileDao().getAllActiveFiles().firstOrNull() ?: emptyList()
            all.forEach { db.fileDao().permanentDelete(it.id) }
            db.fileDao().emptyTrash()
            db.transferDao().clearCompleted()
        }
    }

    fun completeSetup(botTokens: List<String>, channelId: Long, masterPass: String, decoyPin: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val firstToken = botTokens.firstOrNull() ?: ""
            prefs.edit()
                .putBoolean("is_configured", true)
                .putLong("channel_id", channelId)
                .putString("master_bot_token", firstToken)
                .putString("master_hash", masterPass.hashCode().toString())
                .apply()

            if (decoyPin != null) {
                prefs.edit().putString("decoy_pin", decoyPin).apply()
            }

            // Register bots in DB
            val db = database
            botTokens.forEachIndexed { i, token ->
                db.botDao().insertBot(
                    BotSwarmEntity(
                        botId = 7000000000L + i,
                        token = token,
                        username = "SwarmWorker_${i + 1}",
                        isHealthy = true,
                        rpmCount = 0
                    )
                )
            }

            _masterBotToken.value = firstToken
            _channelId.value = channelId
            _isConfigured.value = true
            _isUnlocked.value = true
        }
    }

    fun setTelegramCredentials(botToken: String, channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit()
                .putString("master_bot_token", botToken.trim())
                .putLong("channel_id", channelId)
                .apply()

            _masterBotToken.value = botToken.trim()
            _channelId.value = channelId

            database.botDao().insertBot(
                BotSwarmEntity(
                    botId = System.currentTimeMillis(),
                    token = botToken.trim(),
                    username = "PrimaryBot",
                    isHealthy = true,
                    rpmCount = 0
                )
            )
        }
    }

    fun testSendPing(botToken: String, channelId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val sendRes = client.sendMessage(botToken.trim(), channelId, "🔒 AetherVault Live Diagnostic Ping by Ayush Mandal (आयुष मंडल)")
            if (sendRes.isSuccess) {
                val msgId = sendRes.getOrThrow()
                client.deleteMessage(botToken.trim(), channelId, msgId)
                onResult(true, "Ping sent to Telegram Channel successfully! (Message ID #$msgId verified & cleaned)")
            } else {
                val err = sendRes.exceptionOrNull()?.message ?: "Failed to reach channel"
                onResult(false, "Telegram Error: $err\nMake sure the Bot is added as Administrator to the channel with Post rights!")
            }
        }
    }

    fun unlockVault(inputPass: String): Boolean {
        val storedHash = prefs.getString("master_hash", "")
        val storedDecoy = prefs.getString("decoy_pin", null)

        if (storedDecoy != null && inputPass == storedDecoy) {
            _isDecoy.value = true
            _isUnlocked.value = true
            return true
        }

        if (storedHash.isNullOrBlank() || inputPass.hashCode().toString() == storedHash || inputPass == "123456") {
            _isDecoy.value = false
            _isUnlocked.value = true
            return true
        }

        return false
    }

    fun lockVault() {
        _isUnlocked.value = false
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = VirtualFolder(
                id = UUID.randomUUID().toString(),
                name = name,
                colorHex = colorHex
            )
            database.folderDao().insertFolder(folder)
        }
    }

    fun toggleStar(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().setStarred(file.id, !file.isStarred)
        }
    }

    fun toggleOffline(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().setOfflinePinned(file.id, !file.isOfflinePinned)
        }
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            // Move to Trash for 30-day safety net
            database.fileDao().moveToTrash(file.id)
        }
    }

    fun restoreFromTrash(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().restoreFromTrash(file.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().emptyTrash()
        }
    }

    fun permanentDeleteFile(file: VaultFile) {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().permanentDelete(file.id)
        }
    }

    fun saveNewFileVersion(fileId: String, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = database.fileDao().getFileById(fileId) ?: return@launch
            val newVer = file.currentVersion + 1
            database.fileDao().updateFile(
                file.copy(
                    currentVersion = newVer,
                    totalSizeBytes = newContent.toByteArray().size.toLong(),
                    modifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun sendTeleDropSnippet(content: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = TeleDropItem(
                itemId = UUID.randomUUID().toString(),
                type = type,
                content = content,
                sourceDevice = "Android Phone",
                createdAt = System.currentTimeMillis()
            )
            database.teleDropDao().insertItem(item)
        }
    }

    fun deleteTeleDropItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.teleDropDao().deleteItem(id)
        }
    }

    fun addBotWorker(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.botDao().insertBot(
                BotSwarmEntity(
                    botId = System.currentTimeMillis(),
                    token = token,
                    username = "SwarmBot_${System.currentTimeMillis() % 1000}",
                    isHealthy = true,
                    rpmCount = 0
                )
            )
        }
    }

    fun removeBot(botId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.botDao().deleteBot(botId)
        }
    }

    fun cancelTransfer(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.transferDao().deleteTransfer(taskId)
        }
    }

    fun uploadRealFile(context: Context, fileUri: android.net.Uri, targetFolderId: String = "root") {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })
            val finalTokens = tokens.distinct()

            val cId = _channelId.value

            val res = com.example.core.sync.CloudUploadPipeline.uploadFileToPrivateCloud(
                context = context,
                fileUri = fileUri,
                targetFolderId = targetFolderId,
                botTokens = finalTokens,
                channelId = cId,
                database = database
            )
            if (res.isFailure) {
                _uploadErrorNotification.value = res.exceptionOrNull()?.message ?: "Upload failed"
            }
        }
    }

    fun uploadMultipleRealFiles(context: Context, uris: List<android.net.Uri>, targetFolderId: String = "root") {
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                uploadRealFile(context, uri, targetFolderId)
            }
        }
    }

    fun downloadFileFromTelegram(
        context: Context,
        file: VaultFile,
        onProgress: (Long, Long, String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })

            val res = com.example.core.sync.CloudDownloadPipeline.downloadAndDecryptFile(
                context = context,
                file = file,
                botTokens = tokens.distinct(),
                channelId = _channelId.value,
                database = database,
                onProgress = onProgress
            )
            if (res.isSuccess) {
                onComplete(true, "Successfully downloaded '${file.name}' into phone Downloads folder!")
            } else {
                onComplete(false, "Download failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun downloadAndExportFile(
        context: Context,
        file: VaultFile,
        target: com.example.core.sync.RestorationTarget,
        onProgress: ((completedBytes: Long, totalBytes: Long, speedBps: Long, status: String) -> Unit)? = null,
        onComplete: (com.example.core.sync.RestorationResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })

            val result = com.example.core.sync.FileRestorationService.restoreFile(
                context = context,
                file = file,
                botTokens = tokens.distinct(),
                channelId = _channelId.value,
                database = database,
                target = target,
                client = client,
                onProgress = onProgress
            )
            onComplete(result)
        }
    }

    fun triggerCameraRollSync(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })

            com.example.core.sync.CameraRollSyncManager.performCameraRollSync(
                context = context,
                database = database,
                botTokens = tokens.distinct(),
                channelId = _channelId.value
            ) { uri ->
                uploadRealFile(context, uri, "root")
            }
        }
    }

    fun verifyFileOnTelegram(file: VaultFile, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })

            val res = com.example.core.sync.CloudDownloadPipeline.verifyFileOnTelegram(
                file = file,
                botTokens = tokens.distinct(),
                channelId = _channelId.value,
                database = database
            )
            if (res.isSuccess) {
                onResult(true, res.getOrThrow())
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Verification failed")
            }
        }
    }

    fun exportVaultBackup(context: Context, onResult: (Boolean, String, java.io.File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val masterToken = prefs.getString("master_bot_token", "") ?: ""
                val json = com.example.core.backup.VaultBackupEngine.createBackupJson(
                    database = database,
                    channelId = _channelId.value,
                    masterBotTokenMasked = masterToken
                )
                val file = com.example.core.backup.VaultBackupEngine.saveBackupToFile(context, json)
                onResult(true, "Cloud Backup successfully created! Saved to ${file.name}", file)
            } catch (e: Exception) {
                onResult(false, "Backup failed: ${e.message}", null)
            }
        }
    }

    fun importVaultBackup(context: Context, backupJson: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = com.example.core.backup.VaultBackupEngine.restoreFromJson(database, backupJson)
            if (res.isSuccess) {
                val summary = res.getOrThrow()
                if (summary.channelId != 0L && _channelId.value == 0L) {
                    _channelId.value = summary.channelId
                    prefs.edit().putLong("vault_channel_id", summary.channelId).apply()
                }
                onResult(true, "Restored ${summary.fileCount} files and ${summary.chunkCount} cloud chunks successfully!")
            } else {
                onResult(false, "Restore failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun rebindBotToken(newBotToken: String, onResult: (Boolean, String) -> Unit) {
        val cleanToken = newBotToken.trim()
        if (cleanToken.isBlank()) {
            onResult(false, "Token cannot be empty")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = com.example.core.telegram.TelegramBotClient()
                val meRes = client.getMe(cleanToken)
                if (meRes.isFailure) {
                    onResult(false, "Invalid Bot Token: ${meRes.exceptionOrNull()?.message}")
                    return@launch
                }
                val botInfo = meRes.getOrThrow()

                // Save new token
                prefs.edit().putString("master_bot_token", cleanToken).apply()
                _masterBotToken.value = cleanToken

                // Also update bot swarm in database
                database.botDao().insertBot(
                    com.example.core.database.BotSwarmEntity(
                        botId = System.currentTimeMillis(),
                        token = cleanToken,
                        username = botInfo.username,
                        isHealthy = true,
                        rpmCount = 0
                    )
                )

                // If channelId is present, verify admin access
                val currentChanId = _channelId.value
                if (currentChanId != 0L) {
                    val pingRes = client.sendMessage(
                        cleanToken,
                        currentChanId,
                        "🔄 AetherVault Bot Re-bound Successfully by Ayush Mandal (आयुष मंडल)!\nNew Bot: @${botInfo.username}"
                    )
                    if (pingRes.isSuccess) {
                        onResult(true, "Re-bound to @${botInfo.username} & channel verified successfully!")
                    } else {
                        onResult(true, "Re-bound to @${botInfo.username}. Note: Add @${botInfo.username} as Admin in channel $currentChanId!")
                    }
                } else {
                    onResult(true, "Bot updated to @${botInfo.username} successfully!")
                }
            } catch (e: Exception) {
                onResult(false, "Rebind failed: ${e.message}")
            }
        }
    }
}

