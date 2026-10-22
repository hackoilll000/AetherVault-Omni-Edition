package com.example.features.remotefetch

import java.net.URI
import java.util.UUID

enum class FetchJobStatus {
    QUEUED,
    VALIDATING_URL,
    DOWNLOADING,
    UPLOADING_TO_TELEGRAM,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class RemoteFetchJob(
    val jobId: String = "fetch_${UUID.randomUUID().toString().take(8)}",
    val sourceUrl: String,
    val customFilename: String = "",
    val destinationChannelId: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val uploadProgressPercent: Float = 0f,
    val status: FetchJobStatus = FetchJobStatus.QUEUED,
    val errorMessage: String? = null,
    val telegramMessageId: Long? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

object UrlSecurityValidator {
    private val BLOCKED_HOSTS = listOf(
        "localhost", "127.0.0.1", "0.0.0.0", "::1", "169.254.169.254"
    )

    fun validateUrl(urlStr: String): Pair<Boolean, String> {
        val trimmed = urlStr.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "URL cannot be empty")
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return Pair(false, "Only HTTP and HTTPS URLs are permitted")
            }

            val host = uri.host?.lowercase() ?: return Pair(false, "Invalid host in URL")
            if (BLOCKED_HOSTS.contains(host) || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.16.")) {
                return Pair(false, "Security Error: Localhost and private IP addresses are blocked (SSRF Protection)")
            }

            Pair(true, "URL is valid and safe for remote cloud transfer")
        } catch (e: Exception) {
            Pair(false, "Malformed URL: ${e.localizedMessage}")
        }
    }

    fun extractFilenameFromUrl(urlStr: String): String {
        return try {
            val uri = URI(urlStr.trim())
            val path = uri.path ?: ""
            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                lastSegment
            } else {
                "downloaded_file_${System.currentTimeMillis()}.bin"
            }
        } catch (e: Exception) {
            "downloaded_file_${System.currentTimeMillis()}.bin"
        }
    }
}
