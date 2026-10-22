package com.example.core.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.core.database.*
import com.example.core.telegram.TelegramMultipartUploader
import com.example.core.telegram.TelegramUploadResult
import com.example.platform.AndroidFileHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object CloudUploadPipeline {
    private const val TAG = "CloudUploadPipeline"
    private const val MAX_SINGLE_FILE_BYTES = 48 * 1024 * 1024L // 48MB Telegram Bot API limit

    fun getLocalCachedFile(context: Context, fileId: String, fileName: String): File {
        val dir = File(context.cacheDir, "vault_files").apply { mkdirs() }
        return File(dir, "${fileId}_$fileName")
    }

    suspend fun uploadFileToPrivateCloud(
        context: Context,
        fileUri: Uri,
        targetFolderId: String = "root",
        botTokens: List<String>,
        channelId: Long,
        database: AppDatabase,
        onProgress: ((completedChunks: Int, totalChunks: Int, bytesDone: Long, totalBytes: Long, speedBps: Long, status: String) -> Unit)? = null
    ): Result<VaultFile> = withContext(Dispatchers.IO) {
        val meta = AndroidFileHandler.getFileMetadata(context, fileUri)
        val fileId = UUID.randomUUID().toString()

        // 1. Stage raw unencrypted file in local cache
        val localVaultFile = getLocalCachedFile(context, fileId, meta.name)
        try {
            AndroidFileHandler.openInputStream(context, fileUri)?.use { input ->
                FileOutputStream(localVaultFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy input stream to local cache", e)
            return@withContext Result.failure(Exception("Cannot read file: ${e.message}"))
        }

        val totalBytes = localVaultFile.length().coerceAtLeast(1L)
        val isSingleFile = totalBytes <= MAX_SINGLE_FILE_BYTES
        val totalChunks = if (isSingleFile) 1 else ((totalBytes + MAX_SINGLE_FILE_BYTES - 1) / MAX_SINGLE_FILE_BYTES).toInt().coerceAtLeast(1)

        val taskId = "upload_$fileId"
        val transferTask = TransferTask(
            taskId = taskId,
            fileId = fileId,
            fileName = meta.name,
            localPath = localVaultFile.absolutePath,
            targetFolderId = targetFolderId,
            direction = "UPLOAD",
            status = "ACTIVE",
            completedChunks = 0,
            totalChunks = totalChunks,
            speedBps = 0L,
            bytesTransferred = 0L,
            totalBytes = totalBytes,
            updatedAt = System.currentTimeMillis()
        )
        database.transferDao().upsertTransfer(transferTask)

        val microThumb = AndroidFileHandler.generateMicroThumbnail(context, fileUri, meta.mimeType)

        val validTokens = botTokens.filter { it.isNotBlank() && !it.contains("DEMO") }
        if (validTokens.isEmpty()) {
            val errMsg = "Telegram Bot Token is not configured! Please open Settings -> Telegram Server & Channel Settings to connect your Bot and Channel."
            database.transferDao().updateStatus(taskId, "ERROR")
            throw java.io.IOException(errMsg)
        }

        var currentTokenIndex = 0

        try {
            if (isSingleFile) {
                // Upload whole unencrypted original file directly with real name and MIME type
                onProgress?.invoke(0, 1, 0L, totalBytes, 0L, "Uploading ${meta.name} directly to Telegram...")
                var uploadSuccess = false
                var attempts = 0
                var lastErrorMessage = ""
                var telegramMsgId = 0
                var telegramFileId = ""

                while (!uploadSuccess && attempts < 3) {
                    if (TransferCoordinator.isCancelled(taskId)) {
                        database.transferDao().updateStatus(taskId, "CANCELLED")
                        localVaultFile.delete()
                        return@withContext Result.failure(kotlinx.coroutines.CancellationException("Upload cancelled by user"))
                    }

                    attempts++
                    val activeToken = validTokens[currentTokenIndex % validTokens.size]
                    val res = TelegramMultipartUploader.uploadRawFile(
                        botToken = activeToken,
                        channelId = channelId,
                        file = localVaultFile,
                        originalName = meta.name,
                        mimeType = meta.mimeType,
                        caption = meta.name
                    )
                    when (res) {
                        is TelegramUploadResult.Success -> {
                            telegramMsgId = res.messageId
                            telegramFileId = res.fileId
                            uploadSuccess = true
                            Log.d(TAG, "Uploaded unencrypted file ${meta.name} (Msg ID: $telegramMsgId, File ID: $telegramFileId)")
                        }
                        is TelegramUploadResult.RateLimited -> {
                            currentTokenIndex++
                            kotlinx.coroutines.delay(res.retryAfterSeconds * 1000L)
                        }
                        is TelegramUploadResult.Error -> {
                            lastErrorMessage = res.message
                            currentTokenIndex++
                        }
                    }
                }

                if (TransferCoordinator.isCancelled(taskId)) {
                    database.transferDao().updateStatus(taskId, "CANCELLED")
                    localVaultFile.delete()
                    return@withContext Result.failure(kotlinx.coroutines.CancellationException("Upload cancelled by user"))
                }

                if (!uploadSuccess) {
                    val errMsg = "Upload failed to Telegram channel ($channelId): $lastErrorMessage"
                    database.transferDao().updateStatus(taskId, "ERROR")
                    throw java.io.IOException(errMsg)
                }

                // Record chunk record
                val botFileIdsJson = "{\"primary\":\"$telegramFileId\"}"
                val chunkEntity = FileChunk(
                    id = "${fileId}_0",
                    fileId = fileId,
                    versionId = "v1",
                    chunkIndex = 0,
                    telegramMessageId = telegramMsgId,
                    uploaderBotId = 0L,
                    botFileIdsJson = botFileIdsJson,
                    chunkSizeBytes = totalBytes.toInt(),
                    ivNonce = ByteArray(0),
                    authTag = ByteArray(0)
                )
                database.chunkDao().insertChunk(chunkEntity)
            } else {
                // Large multi-part file: split raw unencrypted bytes into clean parts
                val buffer = ByteArray(MAX_SINGLE_FILE_BYTES.toInt())
                var chunkIndex = 0
                var bytesProcessed = 0L
                val fis = java.io.FileInputStream(localVaultFile)
                fis.use { input ->
                    while (true) {
                        if (TransferCoordinator.isCancelled(taskId)) {
                            database.transferDao().updateStatus(taskId, "CANCELLED")
                            localVaultFile.delete()
                            return@withContext Result.failure(kotlinx.coroutines.CancellationException("Upload cancelled by user"))
                        }

                        val bytesRead = input.read(buffer)
                        if (bytesRead <= 0) break

                        val partFile = File(context.cacheDir, "part_${fileId}_$chunkIndex.${meta.extension}")
                        FileOutputStream(partFile).use { fos ->
                            fos.write(buffer, 0, bytesRead)
                        }

                        val activeToken = validTokens[currentTokenIndex % validTokens.size]
                        val partName = "${meta.name}.part${chunkIndex + 1}"
                        val res = TelegramMultipartUploader.uploadRawFile(
                            botToken = activeToken,
                            channelId = channelId,
                            file = partFile,
                            originalName = partName,
                            mimeType = meta.mimeType,
                            caption = partName
                        )
                        partFile.delete()

                        if (res is TelegramUploadResult.Success) {
                            val botFileIdsJson = "{\"primary\":\"${res.fileId}\"}"
                            val chunkEntity = FileChunk(
                                id = "${fileId}_$chunkIndex",
                                fileId = fileId,
                                versionId = "v1",
                                chunkIndex = chunkIndex,
                                telegramMessageId = res.messageId,
                                uploaderBotId = 0L,
                                botFileIdsJson = botFileIdsJson,
                                chunkSizeBytes = bytesRead,
                                ivNonce = ByteArray(0),
                                authTag = ByteArray(0)
                            )
                            database.chunkDao().insertChunk(chunkEntity)
                        } else if (res is TelegramUploadResult.Error) {
                            throw java.io.IOException("Part upload failed: ${res.message}")
                        }
                        bytesProcessed += bytesRead
                        chunkIndex++
                        onProgress?.invoke(chunkIndex, totalChunks, bytesProcessed, totalBytes, 0L, "Uploaded part $chunkIndex/$totalChunks")
                    }
                }
            }

            // Create VaultFile in database with REAL metadata
            val vaultFile = VaultFile(
                id = fileId,
                folderId = targetFolderId,
                name = meta.name,
                extension = meta.extension,
                mimeType = meta.mimeType,
                totalSizeBytes = totalBytes,
                sha256Hash = "",
                encryptedFileKey = ByteArray(0), // Raw unencrypted file
                microThumbWebp = microThumb,
                thumbMsgId = 0,
                currentVersion = 1,
                isStarred = false,
                isOfflinePinned = true,
                isTrashed = false,
                trashedAt = 0L,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                hlcTimestamp = System.currentTimeMillis().toString()
            )

            database.fileDao().insertFile(vaultFile)
            database.transferDao().updateProgress(taskId, totalChunks, totalBytes, 0L)
            database.transferDao().updateStatus(taskId, "COMPLETED")
            TransferCoordinator.clean(taskId)

            onProgress?.invoke(totalChunks, totalChunks, totalBytes, totalBytes, 0L, "Complete")
            Result.success(vaultFile)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for ${meta.name}", e)
            val isCancelled = e is kotlinx.coroutines.CancellationException || TransferCoordinator.isCancelled(taskId)
            database.transferDao().updateStatus(taskId, if (isCancelled) "CANCELLED" else "ERROR")
            TransferCoordinator.clean(taskId)
            localVaultFile.delete()
            Result.failure(e)
        }
    }
}
