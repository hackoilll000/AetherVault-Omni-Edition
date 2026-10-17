package com.example.core.backup

import android.content.Context
import android.util.Base64
import com.example.core.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class VaultBackupSummary(
    val exportedAt: Long,
    val channelId: Long,
    val fileCount: Int,
    val chunkCount: Int,
    val folderCount: Int = 0,
    val owner: String = "TRB FireOps (@trbfireops)"
)

object VaultBackupEngine {

    /**
     * Generates a portable, 1-tap migration code (AVCODE-...) that can be pasted
     * on any device or new bot setup to instantly restore all files, folders, and channel state.
     */
    suspend fun generateMigrationCode(
        database: AppDatabase,
        channelId: Long,
        masterBotTokenMasked: String = ""
    ): String = withContext(Dispatchers.IO) {
        val json = createBackupJson(database, channelId, masterBotTokenMasked)
        val b64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        "AVCODE-$b64"
    }

    suspend fun createBackupJson(
        database: AppDatabase,
        channelId: Long,
        masterBotTokenMasked: String = ""
    ): String = withContext(Dispatchers.IO) {
        val files = database.fileDao().getAllFilesList()
        val chunks = database.chunkDao().getAllChunksList()
        val folders = database.folderDao().getAllFoldersList()

        val rootJson = JSONObject()
        rootJson.put("format", "AETHER_VAULT_BACKUP_V2")
        rootJson.put("app", "AetherVault Omni Edition")
        rootJson.put("owner", "TRB FireOps (@trbfireops)")
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("channelId", channelId)
        rootJson.put("botTokenMasked", if (masterBotTokenMasked.length > 8) masterBotTokenMasked.take(6) + "..." else "")

        // Virtual Folders
        val foldersArray = JSONArray()
        for (f in folders) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("parentId", f.parentId ?: "")
            fObj.put("name", f.name)
            fObj.put("colorHex", f.colorHex)
            fObj.put("iconName", f.iconName)
            fObj.put("isStarred", f.isStarred)
            fObj.put("createdAt", f.createdAt)
            fObj.put("updatedAt", f.updatedAt)
            foldersArray.put(fObj)
        }
        rootJson.put("folders", foldersArray)

        // Files array
        val filesArray = JSONArray()
        for (f in files) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("folderId", f.folderId)
            fObj.put("name", f.name)
            fObj.put("extension", f.extension)
            fObj.put("mimeType", f.mimeType)
            fObj.put("totalSizeBytes", f.totalSizeBytes)
            fObj.put("sha256Hash", f.sha256Hash)
            fObj.put("encryptedFileKeyB64", Base64.encodeToString(f.encryptedFileKey, Base64.NO_WRAP))
            fObj.put("currentVersion", f.currentVersion)
            fObj.put("isStarred", f.isStarred)
            fObj.put("isOfflinePinned", f.isOfflinePinned)
            fObj.put("createdAt", f.createdAt)
            fObj.put("modifiedAt", f.modifiedAt)
            filesArray.put(fObj)
        }
        rootJson.put("files", filesArray)

        // Chunks array (contains Telegram message ID & file IDs needed to download even if bot changes)
        val chunksArray = JSONArray()
        for (c in chunks) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("fileId", c.fileId)
            cObj.put("versionId", c.versionId)
            cObj.put("chunkIndex", c.chunkIndex)
            cObj.put("telegramMessageId", c.telegramMessageId)
            cObj.put("uploaderBotId", c.uploaderBotId)
            cObj.put("botFileIdsJson", c.botFileIdsJson)
            cObj.put("chunkSizeBytes", c.chunkSizeBytes)
            chunksArray.put(cObj)
        }
        rootJson.put("chunks", chunksArray)

        rootJson.toString(2)
    }

    suspend fun saveBackupToFile(context: Context, backupJson: String): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir, "aethervault_backup_${System.currentTimeMillis()}.json")
        file.writeText(backupJson)
        file
    }

    /**
     * Restores vault catalog from either raw JSON or an "AVCODE-..." string.
     */
    suspend fun restoreFromCode(
        database: AppDatabase,
        inputCode: String
    ): Result<VaultBackupSummary> = withContext(Dispatchers.IO) {
        val clean = inputCode.trim()
        val jsonString = if (clean.startsWith("AVCODE-")) {
            val rawB64 = clean.removePrefix("AVCODE-")
            try {
                val bytes = Base64.decode(rawB64, Base64.NO_WRAP or Base64.URL_SAFE)
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Invalid AVCODE Base64 format: ${e.message}"))
            }
        } else {
            clean
        }

        restoreFromJson(database, jsonString)
    }

    suspend fun restoreFromJson(
        database: AppDatabase,
        jsonString: String
    ): Result<VaultBackupSummary> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val channelId = root.optLong("channelId", 0L)
            val owner = root.optString("owner", "TRB FireOps (@trbfireops)")

            // Restore virtual folders if present
            val restoredFolders = mutableListOf<VirtualFolder>()
            val foldersArray = root.optJSONArray("folders")
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    val fObj = foldersArray.getJSONObject(i)
                    val parent = fObj.optString("parentId", "").takeIf { it.isNotBlank() }
                    restoredFolders.add(
                        VirtualFolder(
                            id = fObj.getString("id"),
                            parentId = parent,
                            name = fObj.getString("name"),
                            colorHex = fObj.optString("colorHex", "#00F0FF"),
                            iconName = fObj.optString("iconName", "folder"),
                            isStarred = fObj.optBoolean("isStarred", false),
                            createdAt = fObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = fObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore files
            val filesArray = root.getJSONArray("files")
            val restoredFiles = mutableListOf<VaultFile>()
            for (i in 0 until filesArray.length()) {
                val fObj = filesArray.getJSONObject(i)
                val keyBytes = try {
                    Base64.decode(fObj.optString("encryptedFileKeyB64", ""), Base64.NO_WRAP)
                } catch (_: Exception) {
                    ByteArray(0)
                }
                val file = VaultFile(
                    id = fObj.getString("id"),
                    folderId = fObj.optString("folderId", "root"),
                    name = fObj.getString("name"),
                    extension = fObj.optString("extension", ""),
                    mimeType = fObj.optString("mimeType", "application/octet-stream"),
                    totalSizeBytes = fObj.optLong("totalSizeBytes", 0L),
                    sha256Hash = fObj.optString("sha256Hash", ""),
                    encryptedFileKey = keyBytes,
                    currentVersion = fObj.optInt("currentVersion", 1),
                    isStarred = fObj.optBoolean("isStarred", false),
                    isOfflinePinned = fObj.optBoolean("isOfflinePinned", false),
                    createdAt = fObj.optLong("createdAt", System.currentTimeMillis()),
                    modifiedAt = fObj.optLong("modifiedAt", System.currentTimeMillis())
                )
                restoredFiles.add(file)
            }

            // Restore chunks
            val chunksArray = root.getJSONArray("chunks")
            val restoredChunks = mutableListOf<FileChunk>()
            for (i in 0 until chunksArray.length()) {
                val cObj = chunksArray.getJSONObject(i)
                val chunk = FileChunk(
                    id = cObj.getString("id"),
                    fileId = cObj.getString("fileId"),
                    versionId = cObj.optString("versionId", "v1"),
                    chunkIndex = cObj.getInt("chunkIndex"),
                    telegramMessageId = cObj.getInt("telegramMessageId"),
                    uploaderBotId = cObj.optLong("uploaderBotId", 0L),
                    botFileIdsJson = cObj.optString("botFileIdsJson", "{}"),
                    chunkSizeBytes = cObj.getInt("chunkSizeBytes"),
                    ivNonce = ByteArray(0),
                    authTag = ByteArray(0)
                )
                restoredChunks.add(chunk)
            }

            // Insert into Room
            if (restoredFolders.isNotEmpty()) {
                database.folderDao().insertFolders(restoredFolders)
            }
            database.fileDao().insertFiles(restoredFiles)
            database.chunkDao().insertChunks(restoredChunks)

            Result.success(
                VaultBackupSummary(
                    exportedAt = exportedAt,
                    channelId = channelId,
                    fileCount = restoredFiles.size,
                    chunkCount = restoredChunks.size,
                    folderCount = restoredFolders.size,
                    owner = owner
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
