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
    val owner: String = "Ayush Mandal (आयुष मंडल)"
)

object VaultBackupEngine {

    suspend fun createBackupJson(
        database: AppDatabase,
        channelId: Long,
        masterBotTokenMasked: String = ""
    ): String = withContext(Dispatchers.IO) {
        val files = database.fileDao().getAllFilesList()
        val chunks = database.chunkDao().getAllChunksList()

        val rootJson = JSONObject()
        rootJson.put("format", "AETHER_VAULT_BACKUP_V1")
        rootJson.put("app", "AetherVault Omni Edition")
        rootJson.put("owner", "Ayush Mandal (आयुष मंडल)")
        rootJson.put("license", "Zero-Knowledge Cloud Protocol")
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("channelId", channelId)
        rootJson.put("botTokenMasked", if (masterBotTokenMasked.length > 8) masterBotTokenMasked.take(6) + "..." else "")

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
            cObj.put("ivNonceB64", Base64.encodeToString(c.ivNonce, Base64.NO_WRAP))
            cObj.put("authTagB64", Base64.encodeToString(c.authTag, Base64.NO_WRAP))
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

    suspend fun restoreFromJson(
        database: AppDatabase,
        jsonString: String
    ): Result<VaultBackupSummary> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val channelId = root.optLong("channelId", 0L)
            val owner = root.optString("owner", "Ayush Mandal (आयुष मंडल)")

            val filesArray = root.getJSONArray("files")
            val restoredFiles = mutableListOf<VaultFile>()
            for (i in 0 until filesArray.length()) {
                val fObj = filesArray.getJSONObject(i)
                val keyBytes = try {
                    Base64.decode(fObj.getString("encryptedFileKeyB64"), Base64.NO_WRAP)
                } catch (e: Exception) {
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
                    createdAt = fObj.optLong("createdAt", System.currentTimeMillis()),
                    modifiedAt = fObj.optLong("modifiedAt", System.currentTimeMillis())
                )
                restoredFiles.add(file)
            }

            val chunksArray = root.getJSONArray("chunks")
            val restoredChunks = mutableListOf<FileChunk>()
            for (i in 0 until chunksArray.length()) {
                val cObj = chunksArray.getJSONObject(i)
                val iv = try {
                    Base64.decode(cObj.optString("ivNonceB64", ""), Base64.NO_WRAP)
                } catch (e: Exception) {
                    ByteArray(0)
                }
                val tag = try {
                    Base64.decode(cObj.optString("authTagB64", ""), Base64.NO_WRAP)
                } catch (e: Exception) {
                    ByteArray(0)
                }
                val chunk = FileChunk(
                    id = cObj.getString("id"),
                    fileId = cObj.getString("fileId"),
                    versionId = cObj.optString("versionId", "v1"),
                    chunkIndex = cObj.getInt("chunkIndex"),
                    telegramMessageId = cObj.getInt("telegramMessageId"),
                    uploaderBotId = cObj.optLong("uploaderBotId", 0L),
                    botFileIdsJson = cObj.optString("botFileIdsJson", "{}"),
                    chunkSizeBytes = cObj.getInt("chunkSizeBytes"),
                    ivNonce = iv,
                    authTag = tag
                )
                restoredChunks.add(chunk)
            }

            // Insert into Room
            database.fileDao().insertFiles(restoredFiles)
            database.chunkDao().insertChunks(restoredChunks)

            Result.success(
                VaultBackupSummary(
                    exportedAt = exportedAt,
                    channelId = channelId,
                    fileCount = restoredFiles.size,
                    chunkCount = restoredChunks.size,
                    owner = owner
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
