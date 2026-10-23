package com.example.features.remotefetch

import android.content.Context
import android.util.Log
import com.example.core.telegram.TelegramMultipartUploader
import com.example.core.telegram.TelegramUploadResult
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient Remote Fetch & Telegram Cloud Transfer Engine
 * Streams direct HTTP/HTTPS files and pipes them to Telegram channel with:
 * - 60s+ connect/read/write timeouts
 * - Automatic chunking (19 MB per part) to safely respect Telegram Bot API 50 MB limits
 * - Resilient error handling (HTTP 400, 403, 413, 429)
 * - Real-time byte progress tracking and job cancellation
 */
class RemoteFetchWorker(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _jobsFlow = MutableStateFlow<List<RemoteFetchJob>>(emptyList())
    val jobsFlow: StateFlow<List<RemoteFetchJob>> = _jobsFlow.asStateFlow()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "RemoteFetchWorker"
        const val CHUNK_SIZE_BYTES = 19 * 1024 * 1024 // 19MB chunks safe for 50MB Bot limit
    }

    private fun getCredentials(userProvidedChannelId: String): Pair<String, Long> {
        val prefs = context.getSharedPreferences("encrypted_vault_prefs", Context.MODE_PRIVATE)
        val botToken = prefs.getString("master_bot_token", "")?.trim() ?: ""

        val rawChan = if (userProvidedChannelId.isNotBlank()) {
            userProvidedChannelId.trim()
        } else {
            prefs.getLong("channel_id", 0L).toString()
        }

        val formatted = TelegramMultipartUploader.formatChatId(rawChan)
        val channelId = formatted.toLongOrNull() ?: 0L
        return Pair(botToken, channelId)
    }

    fun submitFetchJob(sourceUrl: String, customFilename: String, channelIdInput: String): RemoteFetchJob? {
        val (isValid, errorMsg) = UrlSecurityValidator.validateUrl(sourceUrl)
        val (botToken, defaultChannelId) = getCredentials(channelIdInput)

        val targetChannelStr = if (channelIdInput.isNotBlank()) {
            TelegramMultipartUploader.formatChatId(channelIdInput)
        } else {
            defaultChannelId.toString()
        }

        if (!isValid) {
            val failedJob = RemoteFetchJob(
                sourceUrl = sourceUrl,
                customFilename = customFilename,
                destinationChannelId = targetChannelStr,
                status = FetchJobStatus.FAILED,
                errorMessage = errorMsg
            )
            val current = _jobsFlow.value.toMutableList()
            current.add(0, failedJob)
            _jobsFlow.value = current
            return failedJob
        }

        val filename = customFilename.ifBlank { UrlSecurityValidator.extractFilenameFromUrl(sourceUrl) }
        val newJob = RemoteFetchJob(
            sourceUrl = sourceUrl,
            customFilename = filename,
            destinationChannelId = targetChannelStr,
            status = FetchJobStatus.QUEUED
        )

        val current = _jobsFlow.value.toMutableList()
        current.add(0, newJob)
        _jobsFlow.value = current

        val jobHandle = scope.launch {
            executeTransfer(newJob, botToken, targetChannelStr.toLongOrNull() ?: defaultChannelId)
        }
        activeJobs[newJob.jobId] = jobHandle

        return newJob
    }

    private suspend fun executeTransfer(job: RemoteFetchJob, botToken: String, channelId: Long) {
        if (botToken.isBlank()) {
            updateJob(job.jobId) {
                it.copy(
                    status = FetchJobStatus.FAILED,
                    errorMessage = "Telegram Bot Token is missing. Please configure bot token in Onboarding or Settings."
                )
            }
            return
        }

        if (channelId == 0L) {
            updateJob(job.jobId) {
                it.copy(
                    status = FetchJobStatus.FAILED,
                    errorMessage = "Invalid Telegram Channel ID. Please provide a valid supergroup channel ID (e.g. -1004348906769)."
                )
            }
            return
        }

        updateJob(job.jobId) { it.copy(status = FetchJobStatus.VALIDATING_URL) }

        try {
            val request = Request.Builder()
                .url(job.sourceUrl)
                .header("User-Agent", "AetherVault-RemoteTransfer/2.4")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateJob(job.jobId) {
                        it.copy(
                            status = FetchJobStatus.FAILED,
                            errorMessage = "Remote source server returned HTTP ${response.code} ${response.message}"
                        )
                    }
                    return
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from remote server")
                val totalLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                updateJob(job.jobId) {
                    it.copy(
                        status = FetchJobStatus.DOWNLOADING,
                        totalBytes = if (totalLength > 0) totalLength else 0L
                    )
                }

                val cacheDir = File(context.cacheDir, "remote_fetch_staging")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val stagingFile = File(cacheDir, "${job.jobId}_staging.tmp")

                try {
                    // Stream remote data to temporary staging buffer while reporting progress
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    FileOutputStream(stagingFile).use { output ->
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (!coroutineContext.isActive) {
                                stagingFile.delete()
                                return
                            }
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            val progress = if (totalLength > 0) {
                                (totalDownloaded.toFloat() / totalLength.toFloat()) * 50f // 0-50% download
                            } else {
                                25f
                            }
                            updateJob(job.jobId) {
                                it.copy(
                                    downloadedBytes = totalDownloaded,
                                    uploadProgressPercent = progress
                                )
                            }
                        }
                    }

                    // Download completed, now pipe to Telegram
                    updateJob(job.jobId) {
                        it.copy(
                            status = FetchJobStatus.UPLOADING_TO_TELEGRAM,
                            downloadedBytes = totalDownloaded,
                            totalBytes = totalDownloaded,
                            uploadProgressPercent = 50f
                        )
                    }

                    val finalFileSize = stagingFile.length()

                    if (finalFileSize <= 45 * 1024 * 1024L) {
                        // Single file upload
                        val uploadResult = TelegramMultipartUploader.uploadRawFile(
                            botToken = botToken,
                            channelId = channelId,
                            file = stagingFile,
                            originalName = job.customFilename,
                            mimeType = "application/octet-stream",
                            caption = "Remote Ingest: ${job.customFilename} (${formatSize(finalFileSize)})"
                        )

                        when (uploadResult) {
                            is TelegramUploadResult.Success -> {
                                updateJob(job.jobId) {
                                    it.copy(
                                        status = FetchJobStatus.COMPLETED,
                                        uploadProgressPercent = 100f,
                                        telegramMessageId = uploadResult.messageId.toLong()
                                    )
                                }
                            }
                            is TelegramUploadResult.RateLimited -> {
                                updateJob(job.jobId) {
                                    it.copy(
                                        status = FetchJobStatus.FAILED,
                                        errorMessage = "Telegram Rate Limited (Flood Wait). Retry in ${uploadResult.retryAfterSeconds}s"
                                    )
                                }
                            }
                            is TelegramUploadResult.Error -> {
                                updateJob(job.jobId) {
                                    it.copy(
                                        status = FetchJobStatus.FAILED,
                                        errorMessage = uploadResult.message
                                    )
                                }
                            }
                        }
                    } else {
                        // File > 45MB: split into 19MB chunk uploads
                        val totalChunks = ((finalFileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt()
                        var lastMessageId = 0L
                        var allSuccess = true
                        var failureReason = ""

                        java.io.FileInputStream(stagingFile).use { chunkInput ->
                            val chunkBuf = ByteArray(CHUNK_SIZE_BYTES)
                            for (chunkIdx in 0 until totalChunks) {
                                if (!coroutineContext.isActive) {
                                    stagingFile.delete()
                                    return
                                }
                                val chunkRead = chunkInput.read(chunkBuf)
                                if (chunkRead <= 0) break

                                val chunkBytes = if (chunkRead == CHUNK_SIZE_BYTES) chunkBuf else chunkBuf.copyOf(chunkRead)
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
                                        updateJob(job.jobId) {
                                            it.copy(
                                                uploadProgressPercent = uploadPercent,
                                                telegramMessageId = lastMessageId
                                            )
                                        }
                                    }
                                    is TelegramUploadResult.RateLimited -> {
                                        allSuccess = false
                                        failureReason = "Telegram Rate Limited during chunk $chunkIdx. Retry after ${uploadResult.retryAfterSeconds}s"
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
                            updateJob(job.jobId) {
                                it.copy(
                                    status = FetchJobStatus.COMPLETED,
                                    uploadProgressPercent = 100f,
                                    telegramMessageId = lastMessageId
                                )
                            }
                        } else {
                            updateJob(job.jobId) {
                                it.copy(
                                    status = FetchJobStatus.FAILED,
                                    errorMessage = failureReason
                                )
                            }
                        }
                    }

                } finally {
                    stagingFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing remote transfer for job ${job.jobId}", e)
            updateJob(job.jobId) {
                it.copy(
                    status = FetchJobStatus.FAILED,
                    errorMessage = e.message ?: "Transfer encountered unexpected error"
                )
            }
        } finally {
            activeJobs.remove(job.jobId)
        }
    }

    private fun updateJob(jobId: String, transform: (RemoteFetchJob) -> RemoteFetchJob) {
        val current = _jobsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.jobId == jobId }
        if (index >= 0) {
            current[index] = transform(current[index])
            _jobsFlow.value = current
        }
    }

    fun cancelJob(jobId: String) {
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        updateJob(jobId) {
            it.copy(
                status = FetchJobStatus.CANCELLED,
                errorMessage = "Job cancelled by user"
            )
        }
    }

    fun removeJob(jobId: String) {
        cancelJob(jobId)
        _jobsFlow.value = _jobsFlow.value.filterNot { it.jobId == jobId }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}
