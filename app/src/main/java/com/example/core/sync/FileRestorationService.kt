package com.example.core.sync

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.core.crypto.AesGcmChunkCipher
import com.example.core.database.AppDatabase
import com.example.core.database.TransferTask
import com.example.core.database.VaultFile
import com.example.core.telegram.TelegramBotClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

sealed class RestorationTarget {
    object DownloadsFolder : RestorationTarget()
    object PhotoGallery : RestorationTarget()
    data class CustomSafUri(val uri: Uri) : RestorationTarget()
    object ShareIntent : RestorationTarget()
    object CacheForPreview : RestorationTarget()
}

data class RestorationResult(
    val success: Boolean,
    val restoredFile: File? = null,
    val contentUri: Uri? = null,
    val shareIntent: Intent? = null,
    val message: String
)

object FileRestorationService {
    private const val TAG = "FileRestorationService"

    /**
     * Downloads file chunks from Telegram, checks for legacy encryption or raw bytes,
     * reassembles the real file, and exports it to user-selected target (Downloads, Gallery, SAF, Share, Preview Cache).
     */
    suspend fun restoreFile(
        context: Context,
        file: VaultFile,
        botTokens: List<String>,
        channelId: Long,
        database: AppDatabase,
        target: RestorationTarget = RestorationTarget.DownloadsFolder,
        client: TelegramBotClient = TelegramBotClient(),
        onProgress: ((completedBytes: Long, totalBytes: Long, speedBps: Long, status: String) -> Unit)? = null
    ): RestorationResult = withContext(Dispatchers.IO) {
        // Check if file is already cached locally
        val cachedVaultFile = CloudUploadPipeline.getLocalCachedFile(context, file.id, file.name)
        if (cachedVaultFile.exists() && cachedVaultFile.length() > 0) {
            Log.d(TAG, "File ${file.name} found in local cache: ${cachedVaultFile.absolutePath}")
            onProgress?.invoke(cachedVaultFile.length(), file.totalSizeBytes, 0L, "File ready from cache")
            return@withContext exportToTarget(context, file, cachedVaultFile, target, shouldDeleteTemp = false)
        }

        val taskId = "dl_${UUID.randomUUID()}"
        val chunks = database.chunkDao().getChunksForFile(file.id).sortedBy { it.chunkIndex }

        if (chunks.isEmpty()) {
            return@withContext RestorationResult(
                success = false,
                message = "No chunk records found for ${file.name}"
            )
        }

        val validTokens = botTokens.filter { it.isNotBlank() && !it.contains("DEMO") }
        if (validTokens.isEmpty()) {
            return@withContext RestorationResult(
                success = false,
                message = "No valid Telegram bot token available. Configure in Settings."
            )
        }
        val primaryToken = validTokens.first()

        val formattedChannelId = if (channelId > 0) {
            val s = channelId.toString()
            (if (s.startsWith("100")) "-$s" else "-100$s").toLongOrNull() ?: channelId
        } else {
            channelId
        }

        // Register in Transfer Queue for HUD
        val transferTask = TransferTask(
            taskId = taskId,
            fileId = file.id,
            fileName = file.name,
            direction = "DOWNLOAD",
            status = "ACTIVE",
            completedChunks = 0,
            totalChunks = chunks.size,
            bytesTransferred = 0L,
            totalBytes = file.totalSizeBytes,
            updatedAt = System.currentTimeMillis()
        )
        database.transferDao().upsertTransfer(transferTask)

        // Temporary reassembly file
        val tempRestoreDir = File(context.cacheDir, "vault_reassembly").apply { if (!exists()) mkdirs() }
        val tempPlaintextFile = File(tempRestoreDir, "temp_${file.id}_${System.currentTimeMillis()}_${file.name}")

        val startTime = System.currentTimeMillis()
        var bytesProcessed = 0L

        try {
            FileOutputStream(tempPlaintextFile).use { fos ->
                chunks.forEachIndexed { idx, chunk ->
                    val chunkIdx = chunk.chunkIndex
                    val statusText = "Downloading ${file.name} (#${idx + 1}/${chunks.size})..."
                    onProgress?.invoke(bytesProcessed, file.totalSizeBytes, 0L, statusText)

                    // 1. Resolve Telegram File ID
                    var telegramFileId = runCatching {
                        JSONObject(chunk.botFileIdsJson).optString("primary")
                    }.getOrNull()?.takeIf { it.isNotBlank() }

                    // If missing or empty, resolve via cross-bot message forward
                    if (telegramFileId == null && chunk.telegramMessageId > 0) {
                        val resolveRes = client.resolveCrossBotFileId(primaryToken, formattedChannelId, chunk.telegramMessageId)
                        if (resolveRes.isSuccess && resolveRes.getOrThrow().isNotBlank()) {
                            telegramFileId = resolveRes.getOrThrow()
                            val updatedChunk = chunk.copy(
                                botFileIdsJson = "{\"primary\": \"$telegramFileId\"}"
                            )
                            database.chunkDao().insertChunk(updatedChunk)
                        }
                    }

                    // 2. Download chunk bytes from Telegram
                    val downloadedBytes: ByteArray = if (!telegramFileId.isNullOrBlank()) {
                        val downloadRes = client.downloadFileBytes(primaryToken, telegramFileId)
                        if (downloadRes.isSuccess) {
                            downloadRes.getOrThrow()
                        } else {
                            val reResolve = client.resolveCrossBotFileId(primaryToken, formattedChannelId, chunk.telegramMessageId)
                            if (reResolve.isSuccess && reResolve.getOrThrow().isNotBlank()) {
                                val retryRes = client.downloadFileBytes(primaryToken, reResolve.getOrThrow())
                                if (retryRes.isSuccess) {
                                    retryRes.getOrThrow()
                                } else {
                                    fetchFromLocalCacheOrThrow(context, file.id, chunkIdx, retryRes.exceptionOrNull()?.message)
                                }
                            } else {
                                fetchFromLocalCacheOrThrow(context, file.id, chunkIdx, downloadRes.exceptionOrNull()?.message)
                            }
                        }
                    } else {
                        fetchFromLocalCacheOrThrow(context, file.id, chunkIdx, "No telegram file_id recorded")
                    }

                    // 3. Check for legacy AVLT encrypted header
                    val isLegacyEncrypted = downloadedBytes.size > 17 &&
                            downloadedBytes[0] == 0x41.toByte() &&
                            downloadedBytes[1] == 0x56.toByte() &&
                            downloadedBytes[2] == 0x4C.toByte() &&
                            downloadedBytes[3] == 0x54.toByte()

                    val cleanBytes = if (isLegacyEncrypted && file.encryptedFileKey.isNotEmpty()) {
                        try {
                            AesGcmChunkCipher.decryptChunk(
                                frameData = downloadedBytes,
                                fileKey = file.encryptedFileKey,
                                expectedChunkIndex = chunkIdx,
                                fileId = file.id
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Legacy decryption failed, using raw bytes: ${e.message}")
                            downloadedBytes
                        }
                    } else {
                        downloadedBytes
                    }

                    // 4. Stream to output
                    fos.write(cleanBytes)
                    bytesProcessed += cleanBytes.size

                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000.0).coerceAtLeast(0.1)
                    val speed = (bytesProcessed / elapsedSec).toLong()

                    database.transferDao().updateProgress(
                        taskId = taskId,
                        completed = idx + 1,
                        bytes = bytesProcessed,
                        speed = speed
                    )

                    onProgress?.invoke(
                        bytesProcessed,
                        file.totalSizeBytes,
                        speed,
                        "Downloaded #${idx + 1}/${chunks.size} (${(bytesProcessed * 100 / file.totalSizeBytes.coerceAtLeast(1L)).toInt()}%)"
                    )
                }
            }

            database.transferDao().updateStatus(taskId, "COMPLETED")

            // Cache reassembled file in local vault cache for immediate viewing
            try {
                FileInputStream(tempPlaintextFile).use { input ->
                    FileOutputStream(cachedVaultFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write into vault_files cache: ${e.message}")
            }

            exportToTarget(context, file, tempPlaintextFile, target, shouldDeleteTemp = true)
        } catch (e: Exception) {
            Log.e(TAG, "Restoration failed for ${file.name}", e)
            database.transferDao().updateStatus(taskId, "ERROR")
            tempPlaintextFile.delete()
            RestorationResult(
                success = false,
                message = "Restoration failed: ${e.message}"
            )
        }
    }

    private fun exportToTarget(
        context: Context,
        file: VaultFile,
        sourceFile: File,
        target: RestorationTarget,
        shouldDeleteTemp: Boolean
    ): RestorationResult {
        val res = when (target) {
            is RestorationTarget.DownloadsFolder -> saveToDownloads(context, file, sourceFile)
            is RestorationTarget.PhotoGallery -> saveToGallery(context, file, sourceFile)
            is RestorationTarget.CustomSafUri -> saveToCustomUri(context, target.uri, sourceFile, file.name)
            is RestorationTarget.ShareIntent -> createShareIntent(context, file, sourceFile)
            is RestorationTarget.CacheForPreview -> {
                val cached = CloudUploadPipeline.getLocalCachedFile(context, file.id, file.name)
                if (sourceFile.absolutePath != cached.absolutePath) {
                    FileInputStream(sourceFile).use { input ->
                        FileOutputStream(cached).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                RestorationResult(
                    success = true,
                    restoredFile = cached,
                    message = "Cached for preview"
                )
            }
        }
        if (shouldDeleteTemp && sourceFile.exists() && sourceFile.parent?.contains("vault_reassembly") == true) {
            sourceFile.delete()
        }
        return res
    }

    private fun fetchFromLocalCacheOrThrow(context: Context, fileId: String, chunkIdx: Int, errorMsg: String?): ByteArray {
        val cacheDir = File(context.cacheDir, "vault_staging_$fileId")
        val stagingFile = File(cacheDir, "chunk_$chunkIdx.bin")
        if (stagingFile.exists()) {
            return stagingFile.readBytes()
        }
        val directFile = File(context.cacheDir, "vault_files")
        val matching = directFile.listFiles()?.firstOrNull { it.name.startsWith(fileId) }
        if (matching != null && matching.exists()) {
            return matching.readBytes()
        }
        throw java.io.IOException("Cannot download chunk #$chunkIdx from Telegram: ${errorMsg ?: "Unknown error"}")
    }

    private fun saveToDownloads(context: Context, file: VaultFile, tempFile: File): RestorationResult {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            var targetFile = File(downloadsDir, file.name)
            var counter = 1
            val baseName = file.name.substringBeforeLast('.')
            val extension = if (file.name.contains('.')) ".${file.name.substringAfterLast('.')}" else ""

            while (targetFile.exists()) {
                targetFile = File(downloadsDir, "$baseName ($counter)$extension")
                counter++
            }

            FileInputStream(tempFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(file.mimeType),
                null
            )

            RestorationResult(
                success = true,
                restoredFile = targetFile,
                message = "Saved to Downloads: ${targetFile.name}"
            )
        } catch (e: Exception) {
            RestorationResult(
                success = false,
                message = "Could not save to Downloads: ${e.message}"
            )
        }
    }

    private fun saveToGallery(context: Context, file: VaultFile, tempFile: File): RestorationResult {
        return try {
            val isVideo = file.mimeType.startsWith("video/")
            val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, file.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AetherVault")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertedUri = context.contentResolver.insert(contentUri, values)
                ?: throw java.io.IOException("MediaStore insertion returned null")

            context.contentResolver.openOutputStream(insertedUri)?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(insertedUri, values, null, null)
            }

            RestorationResult(
                success = true,
                contentUri = insertedUri,
                message = "Saved to Gallery (Pictures/AetherVault)!"
            )
        } catch (e: Exception) {
            saveToDownloads(context, file, tempFile)
        }
    }

    private fun saveToCustomUri(context: Context, targetUri: Uri, tempFile: File, fileName: String): RestorationResult {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            }
            RestorationResult(
                success = true,
                contentUri = targetUri,
                message = "Exported '$fileName' successfully!"
            )
        } catch (e: Exception) {
            RestorationResult(
                success = false,
                message = "SAF write failed: ${e.message}"
            )
        }
    }

    private fun createShareIntent(context: Context, file: VaultFile, tempFile: File): RestorationResult {
        return try {
            val shareDir = File(context.cacheDir, "decrypted_shares").apply { if (!exists()) mkdirs() }
            val shareFile = File(shareDir, file.name)

            FileInputStream(tempFile).use { input ->
                FileOutputStream(shareFile).use { output ->
                    input.copyTo(output)
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, shareFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = file.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            RestorationResult(
                success = true,
                restoredFile = shareFile,
                contentUri = uri,
                shareIntent = Intent.createChooser(intent, "Share File"),
                message = "Ready to share file"
            )
        } catch (e: Exception) {
            RestorationResult(
                success = false,
                message = "Failed to create share intent: ${e.message}"
            )
        }
    }
}
