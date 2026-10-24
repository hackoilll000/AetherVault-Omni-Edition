package com.example.features.linkmanager

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.core.telegram.TelegramMultipartUploader
import com.example.core.telegram.TelegramUploadResult
import com.example.features.diagnostics.DiagnosticManager
import com.example.features.diagnostics.DiagnosticSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Execution Coordinator for Universal Link Manager
 * Coordinates resolving, local SAF downloads, and Telegram cloud transfers.
 */
class UniversalLinkCoordinator(
    private val context: Context,
    private val diagnosticManager: DiagnosticManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasksFlow = MutableStateFlow<List<LinkTransferTask>>(emptyList())
    val tasksFlow: StateFlow<List<LinkTransferTask>> = _tasksFlow.asStateFlow()

    private val _resolvedResourceFlow = MutableStateFlow<ResolvedLinkResource?>(null)
    val resolvedResourceFlow: StateFlow<ResolvedLinkResource?> = _resolvedResourceFlow.asStateFlow()

    private val _isResolvingFlow = MutableStateFlow(false)
    val isResolvingFlow: StateFlow<Boolean> = _isResolvingFlow.asStateFlow()

    private val _resolveErrorFlow = MutableStateFlow<String?>(null)
    val resolveErrorFlow: StateFlow<String?> = _resolveErrorFlow.asStateFlow()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "UniversalLinkCoordinator"
        const val CHUNK_SIZE_BYTES = 19 * 1024 * 1024 // 19MB parts for Telegram Bot API
    }

    /**
     * Resolves metadata and capabilities for a submitted link
     */
    fun resolveUrl(url: String) {
        if (url.isBlank()) return
        _isResolvingFlow.value = true
        _resolveErrorFlow.value = null
        _resolvedResourceFlow.value = null

        scope.launch {
            diagnosticManager?.addLog(
                DiagnosticSeverity.INFO,
                "LinkManager",
                "Resolving link: ${url.take(60)}..."
            )

            val result = UniversalLinkRegistry.resolve(url)
            _isResolvingFlow.value = false

            if (result.isSuccess) {
                val res = result.getOrNull()
                _resolvedResourceFlow.value = res
                diagnosticManager?.addLog(
                    DiagnosticSeverity.INFO,
                    "LinkManager",
                    "Resolved resource: ${res?.title} [Platform: ${res?.platform?.displayName}]"
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to resolve resource"
                _resolveErrorFlow.value = err
                diagnosticManager?.addLog(
                    DiagnosticSeverity.WARNING,
                    "LinkManager",
                    "Resolution error: $err"
                )
            }
        }
    }

    /**
     * Executes local storage download
     */
    fun startLocalDownload(
        resource: ResolvedLinkResource,
        customFilename: String,
        userSelectedSafUri: Uri? = null
    ): LinkTransferTask {
        val filename = customFilename.ifBlank { resource.suggestedFilename }
        val destinationPath = userSelectedSafUri?.toString() ?: "Device Downloads/$filename"

        val newTask = LinkTransferTask(
            resource = resource,
            destination = TransferDestination.LOCAL_STORAGE,
            destinationDetails = destinationPath,
            stage = TransferStage.QUEUED
        )

        updateTaskList { add(0, newTask) }

        val job = scope.launch {
            executeLocalDownload(newTask, filename, userSelectedSafUri)
        }
        activeJobs[newTask.taskId] = job

        return newTask
    }

    private suspend fun executeLocalDownload(
        task: LinkTransferTask,
        filename: String,
        safUri: Uri?
    ) {
        updateTask(task.taskId) { it.copy(stage = TransferStage.DOWNLOADING) }

        try {
            val downloadUrl = task.resource.normalizedUrl
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "AetherVault-UniversalLinkManager/2.4")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateTask(task.taskId) {
                        it.copy(
                            stage = TransferStage.FAILED,
                            errorMessage = "Source server returned HTTP ${response.code} ${response.message}"
                        )
                    }
                    return
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                val outputStream = if (safUri != null) {
                    context.contentResolver.openOutputStream(safUri)
                        ?: throw IllegalStateException("Failed to open Storage Access Framework output stream")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetFile = File(downloadsDir, filename)
                    FileOutputStream(targetFile)
                }

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalTransferred = 0L

                outputStream.use { out ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            updateTask(task.taskId) { it.copy(stage = TransferStage.CANCELLED) }
                            return
                        }
                        out.write(buffer, 0, bytesRead)
                        totalTransferred += bytesRead

                        val percent = if (totalLength > 0) {
                            (totalTransferred.toFloat() / totalLength.toFloat()) * 100f
                        } else {
                            50f
                        }

                        updateTask(task.taskId) {
                            it.copy(
                                transferredBytes = totalTransferred,
                                totalBytes = if (totalLength > 0) totalLength else totalTransferred,
                                downloadProgressPercent = percent
                            )
                        }
                    }
                }

                updateTask(task.taskId) {
                    it.copy(
                        stage = TransferStage.COMPLETED,
                        transferredBytes = totalTransferred,
                        totalBytes = totalTransferred,
                        downloadProgressPercent = 100f,
                        localSavedUri = safUri?.toString() ?: filename
                    )
                }

                diagnosticManager?.addLog(
                    DiagnosticSeverity.INFO,
                    "LinkManager",
                    "Local download completed: $filename ($totalTransferred bytes)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local download failed for ${task.taskId}", e)
            updateTask(task.taskId) {
                it.copy(
                    stage = TransferStage.FAILED,
                    errorMessage = e.message ?: "Download failed unexpectedly"
                )
            }
        } finally {
            activeJobs.remove(task.taskId)
        }
    }

    /**
     * Executes Telegram cloud transfer
     */
    fun startTelegramUpload(
        resource: ResolvedLinkResource,
        customFilename: String,
        channelIdInput: String
    ): LinkTransferTask {
        val (botToken, defaultChannelId) = getCredentials(channelIdInput)
        val targetChannelStr = if (channelIdInput.isNotBlank()) {
            TelegramMultipartUploader.formatChatId(channelIdInput)
        } else {
            defaultChannelId.toString()
        }

        val filename = customFilename.ifBlank { resource.suggestedFilename }

        val newTask = LinkTransferTask(
            resource = resource,
            destination = TransferDestination.TELEGRAM_CLOUD,
            destinationDetails = "Channel $targetChannelStr",
            stage = TransferStage.QUEUED
        )

        updateTaskList { add(0, newTask) }

        val job = scope.launch {
            executeTelegramUpload(
                task = newTask,
                filename = filename,
                botToken = botToken,
                channelId = targetChannelStr.toLongOrNull() ?: defaultChannelId
            )
        }
        activeJobs[newTask.taskId] = job

        return newTask
    }

    private suspend fun executeTelegramUpload(
        task: LinkTransferTask,
        filename: String,
        botToken: String,
        channelId: Long
    ) {
        if (botToken.isBlank()) {
            updateTask(task.taskId) {
                it.copy(
                    stage = TransferStage.FAILED,
                    errorMessage = "Telegram Bot Token is not configured. Setup bot token in Settings."
                )
            }
            return
        }

        if (channelId == 0L) {
            updateTask(task.taskId) {
                it.copy(
                    stage = TransferStage.FAILED,
                    errorMessage = "Invalid Telegram Channel ID. Provide a valid -100... supergroup ID."
                )
            }
            return
        }

        updateTask(task.taskId) { it.copy(stage = TransferStage.DOWNLOADING) }

        val cacheDir = File(context.cacheDir, "link_manager_staging")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val stagingFile = File(cacheDir, "${task.taskId}_stage.tmp")

        try {
            val downloadUrl = task.resource.normalizedUrl
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "AetherVault-UniversalLinkManager/2.4")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateTask(task.taskId) {
                        it.copy(
                            stage = TransferStage.FAILED,
                            errorMessage = "Remote source server returned HTTP ${response.code} ${response.message}"
                        )
                    }
                    return
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalDownloaded = 0L

                FileOutputStream(stagingFile).use { out ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            stagingFile.delete()
                            updateTask(task.taskId) { it.copy(stage = TransferStage.CANCELLED) }
                            return
                        }
                        out.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val percent = if (totalLength > 0) {
                            (totalDownloaded.toFloat() / totalLength.toFloat()) * 50f // 0-50% download
                        } else {
                            25f
                        }

                        updateTask(task.taskId) {
                            it.copy(
                                transferredBytes = totalDownloaded,
                                totalBytes = if (totalLength > 0) totalLength else totalDownloaded,
                                downloadProgressPercent = percent
                            )
                        }
                    }
                }

                // Stage 2: Upload to Telegram
                updateTask(task.taskId) {
                    it.copy(
                        stage = TransferStage.UPLOADING,
                        downloadProgressPercent = 50f
                    )
                }

                val finalFileSize = stagingFile.length()

                if (finalFileSize <= 45 * 1024 * 1024L) {
                    val uploadResult = TelegramMultipartUploader.uploadRawFile(
                        botToken = botToken,
                        channelId = channelId,
                        file = stagingFile,
                        originalName = filename,
                        mimeType = task.resource.mimeType,
                        caption = "[Universal Link] $filename (${formatBytes(finalFileSize)})"
                    )

                    when (uploadResult) {
                        is TelegramUploadResult.Success -> {
                            updateTask(task.taskId) {
                                it.copy(
                                    stage = TransferStage.COMPLETED,
                                    uploadProgressPercent = 100f,
                                    telegramMessageId = uploadResult.messageId.toLong()
                                )
                            }
                            diagnosticManager?.addLog(
                                DiagnosticSeverity.INFO,
                                "LinkManager",
                                "Uploaded $filename to Telegram message #${uploadResult.messageId}"
                            )
                        }
                        is TelegramUploadResult.RateLimited -> {
                            updateTask(task.taskId) {
                                it.copy(
                                    stage = TransferStage.FAILED,
                                    errorMessage = "Telegram Rate Limited (Flood Wait). Retry after ${uploadResult.retryAfterSeconds}s"
                                )
                            }
                        }
                        is TelegramUploadResult.Error -> {
                            updateTask(task.taskId) {
                                it.copy(
                                    stage = TransferStage.FAILED,
                                    errorMessage = uploadResult.message
                                )
                            }
                        }
                    }
                } else {
                    // Chunked multipart upload for files > 45MB
                    val totalChunks = ((finalFileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt()
                    var lastMessageId = 0L
                    var allSuccess = true
                    var failureReason = ""

                    java.io.FileInputStream(stagingFile).use { chunkInput ->
                        val chunkBuf = ByteArray(CHUNK_SIZE_BYTES)
                        for (chunkIdx in 0 until totalChunks) {
                            if (!coroutineContext.isActive) {
                                stagingFile.delete()
                                updateTask(task.taskId) { it.copy(stage = TransferStage.CANCELLED) }
                                return
                            }
                            val read = chunkInput.read(chunkBuf)
                            if (read <= 0) break

                            val chunkBytes = if (read == CHUNK_SIZE_BYTES) chunkBuf else chunkBuf.copyOf(read)
                            val uploadResult = TelegramMultipartUploader.uploadChunkBytes(
                                botToken = botToken,
                                channelId = channelId,
                                bytes = chunkBytes,
                                chunkIndex = chunkIdx,
                                totalChunks = totalChunks
                            )

                            when (uploadResult) {
                                is TelegramUploadResult.Success -> {
                                    lastMessageId = uploadResult.messageId.toLong()
                                    val uploadPercent = 50f + ((chunkIdx + 1).toFloat() / totalChunks.toFloat()) * 50f
                                    updateTask(task.taskId) {
                                        it.copy(
                                            uploadProgressPercent = uploadPercent,
                                            telegramMessageId = lastMessageId
                                        )
                                    }
                                }
                                is TelegramUploadResult.RateLimited -> {
                                    allSuccess = false
                                    failureReason = "Rate limited during chunk $chunkIdx. Wait ${uploadResult.retryAfterSeconds}s"
                                    break
                                }
                                is TelegramUploadResult.Error -> {
                                    allSuccess = false
                                    failureReason = uploadResult.message
                                    break
                                }
                            }
                        }
                    }

                    if (allSuccess) {
                        updateTask(task.taskId) {
                            it.copy(
                                stage = TransferStage.COMPLETED,
                                uploadProgressPercent = 100f,
                                telegramMessageId = lastMessageId
                            )
                        }
                    } else {
                        updateTask(task.taskId) {
                            it.copy(
                                stage = TransferStage.FAILED,
                                errorMessage = failureReason
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram transfer failed for ${task.taskId}", e)
            updateTask(task.taskId) {
                it.copy(
                    stage = TransferStage.FAILED,
                    errorMessage = e.message ?: "Transfer encountered unexpected error"
                )
            }
        } finally {
            stagingFile.delete()
            activeJobs.remove(task.taskId)
        }
    }

    private fun getCredentials(channelInput: String): Pair<String, Long> {
        val prefs = context.getSharedPreferences("encrypted_vault_prefs", Context.MODE_PRIVATE)
        val botToken = prefs.getString("master_bot_token", "")?.trim() ?: ""

        val rawChan = if (channelInput.isNotBlank()) {
            channelInput.trim()
        } else {
            prefs.getLong("channel_id", 0L).toString()
        }

        val formatted = TelegramMultipartUploader.formatChatId(rawChan)
        val channelId = formatted.toLongOrNull() ?: 0L
        return Pair(botToken, channelId)
    }

    private fun updateTask(taskId: String, transform: (LinkTransferTask) -> LinkTransferTask) {
        val current = _tasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.taskId == taskId }
        if (index >= 0) {
            current[index] = transform(current[index])
            _tasksFlow.value = current
        }
    }

    private fun updateTaskList(block: MutableList<LinkTransferTask>.() -> Unit) {
        val current = _tasksFlow.value.toMutableList()
        current.block()
        _tasksFlow.value = current
    }

    fun cancelTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTask(taskId) {
            it.copy(
                stage = TransferStage.CANCELLED,
                errorMessage = "Cancelled by user"
            )
        }
    }

    fun removeTask(taskId: String) {
        cancelTask(taskId)
        _tasksFlow.value = _tasksFlow.value.filterNot { it.taskId == taskId }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}
