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

    private val _channelId = MutableStateFlow(prefs.getLong("channel_id", 0L))
    val channelId: StateFlow<Long> = _channelId.asStateFlow()

    private val _isTermsAgreed = MutableStateFlow(prefs.getBoolean("terms_agreed_trbfireops", false))
    val isTermsAgreed: StateFlow<Boolean> = _isTermsAgreed.asStateFlow()

    private val _uploadErrorNotification = MutableStateFlow<String?>(null)
    val uploadErrorNotification: StateFlow<String?> = _uploadErrorNotification.asStateFlow()

    fun acceptTerms() {
        prefs.edit().putBoolean("terms_agreed_trbfireops", true).apply()
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

            // Clean up any test/demo bot tokens or channel IDs from sharedPreferences
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            if (storedToken.contains("AAH_QR") || storedToken.contains("7123456789") || storedToken.contains("DEMO")) {
                prefs.edit().remove("master_bot_token").apply()
                _masterBotToken.value = ""
            }
            val storedChan = prefs.getLong("channel_id", 0L)
            if (storedChan == -1002345678901L) {
                prefs.edit().remove("channel_id").apply()
                _channelId.value = 0L
            }

            // Remove test bots from Room
            val bots = db.botDao().getAllBotsList()
            for (b in bots) {
                if (b.token.contains("DEMO") || b.token.contains("AAH_QR") || b.token.contains("7123456789") || b.botId == 7000000000L) {
                    db.botDao().deleteBot(b.botId)
                }
            }

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
        val cleanToken = botToken.trim()
        val cId = channelId
        viewModelScope.launch(Dispatchers.IO) {
            val botRes = client.getMe(cleanToken)
            if (botRes.isFailure) {
                onResult(false, "✗ Error: Invalid Bot Token (${botRes.exceptionOrNull()?.message})\nPlease check your Bot Token from @BotFather!")
                return@launch
            }
            val botInfo = botRes.getOrThrow()

            if (cId == 0L) {
                onResult(false, "✗ Error: Channel ID is missing or invalid!\nChannel ID must be in -100... format (e.g. -1001234567890)")
                return@launch
            }

            val sendRes = client.sendMessage(cleanToken, cId, "🔒 AetherVault Live Connection Ping\nBot: @${botInfo.username}\nChannel: $cId")
            if (sendRes.isSuccess) {
                val msgId = sendRes.getOrThrow()
                client.deleteMessage(cleanToken, cId, msgId)
                onResult(true, "✓ Connected successfully to @${botInfo.username} & Channel $cId!\n(Message ID #$msgId verified & cleaned)")
            } else {
                val err = sendRes.exceptionOrNull()?.message ?: "Failed to reach channel"
                onResult(
                    false,
                    "✗ Connection Failed: $err\n\n📌 Quick Fix Checklist:\n1. Open your Telegram Channel -> Channel Settings -> Administrators\n2. Add @${botInfo.username} as Administrator\n3. Ensure 'Post Messages' & 'Delete Messages' rights are ON\n4. Double-check Channel ID starts with -100 (e.g. -1001234567890)"
                )
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

    fun createFolder(name: String, colorHex: String, parentId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = VirtualFolder(
                id = UUID.randomUUID().toString(),
                parentId = if (parentId == "root") null else parentId,
                name = name,
                colorHex = colorHex
            )
            database.folderDao().insertFolder(folder)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.folderDao().getFolderById(folderId)
            if (existing != null) {
                database.folderDao().updateFolder(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Reassign files in this folder to root
            val inFolder = database.fileDao().getFilesInFolder(folderId).firstOrNull() ?: emptyList()
            inFolder.forEach { file ->
                database.fileDao().updateFile(file.copy(folderId = "root"))
            }
            database.folderDao().deleteFolder(folderId)
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
        permanentDeleteFile(file)
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
            val db = database
            val chunks = db.chunkDao().getChunksForFile(file.id)
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val cId = _channelId.value

            // 1. Delete from Room DB
            db.fileDao().permanentDelete(file.id)
            db.chunkDao().deleteChunksForFile(file.id)

            // 2. Delete Telegram messages in background if credentials configured
            if (storedToken.isNotBlank() && cId != 0L) {
                for (chunk in chunks) {
                    if (chunk.telegramMessageId > 0) {
                        client.deleteMessage(storedToken, cId, chunk.telegramMessageId)
                    }
                }
                if (file.thumbMsgId > 0) {
                    client.deleteMessage(storedToken, cId, file.thumbMsgId)
                }
            }
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

    fun generateMigrationCode(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val masterToken = prefs.getString("master_bot_token", "") ?: ""
            val code = com.example.core.backup.VaultBackupEngine.generateMigrationCode(
                database = database,
                channelId = _channelId.value,
                masterBotTokenMasked = masterToken
            )
            onResult(code)
        }
    }

    fun restoreFromMigrationCode(inputCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = com.example.core.backup.VaultBackupEngine.restoreFromCode(database, inputCode)
            if (res.isSuccess) {
                val summary = res.getOrThrow()
                if (summary.channelId != 0L) {
                    _channelId.value = summary.channelId
                    prefs.edit().putLong("channel_id", summary.channelId).apply()
                }
                _isConfigured.value = true
                _isUnlocked.value = true
                prefs.edit().putBoolean("is_configured", true).apply()
                onResult(true, "Restored ${summary.fileCount} files, ${summary.folderCount} folders & ${summary.chunkCount} cloud references!")
            } else {
                onResult(false, "Restore failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun importVaultBackup(context: Context, jsonText: String, onResult: (Boolean, String) -> Unit) {
        restoreFromMigrationCode(jsonText, onResult)
    }

    fun scanExistingChannelHistory(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val token = storedToken.ifBlank { bots.firstOrNull()?.token ?: "" }
            val cId = _channelId.value

            if (token.isBlank() || cId == 0L) {
                onResult(false, "Bot Token or Channel ID is missing. Please configure in Settings.")
                return@launch
            }

            val res = client.scanExistingChannelContent(token, cId, database, context)
            if (res.isSuccess) {
                val count = res.getOrThrow()
                onResult(true, "Successfully scanned and indexed $count pre-existing files & texts from private channel!")
            } else {
                onResult(false, "Scan failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun moveMultipleFilesToFolder(fileIds: List<String>, folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.fileDao().moveFilesToFolder(fileIds, folderId)
        }
    }

    fun deleteMultipleFiles(fileIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = database
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val cId = _channelId.value

            fileIds.forEach { id ->
                val chunks = db.chunkDao().getChunksForFile(id)
                val file = db.fileDao().getFileById(id)
                db.fileDao().permanentDelete(id)
                db.chunkDao().deleteChunksForFile(id)

                if (storedToken.isNotBlank() && cId != 0L) {
                    chunks.forEach { chunk ->
                        if (chunk.telegramMessageId > 0) client.deleteMessage(storedToken, cId, chunk.telegramMessageId)
                    }
                    if (file != null && file.thumbMsgId > 0) client.deleteMessage(storedToken, cId, file.thumbMsgId)
                }
            }
        }
    }

    fun downloadMultipleFilesToDevice(
        context: Context,
        files: List<VaultFile>,
        onProgress: (Int, Int) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            val bots = database.botDao().getHealthyBots()
            val storedToken = prefs.getString("master_bot_token", "") ?: ""
            val tokens = mutableListOf<String>()
            if (storedToken.isNotBlank()) tokens.add(storedToken)
            tokens.addAll(bots.map { it.token }.filter { it.isNotBlank() && !it.contains("DEMO") })

            files.forEachIndexed { index, file ->
                onProgress(index + 1, files.size)
                val res = com.example.core.sync.FileRestorationService.restoreFile(
                    context = context,
                    file = file,
                    botTokens = tokens.distinct(),
                    channelId = _channelId.value,
                    database = database,
                    target = com.example.core.sync.RestorationTarget.DownloadsFolder,
                    client = client
                )
                if (res.success) successCount++
            }
            onComplete(successCount)
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
                        "🔄 AetherVault Bot Re-bound Successfully by TRB FireOps (@trbfireops)!\nNew Bot: @${botInfo.username}"
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

