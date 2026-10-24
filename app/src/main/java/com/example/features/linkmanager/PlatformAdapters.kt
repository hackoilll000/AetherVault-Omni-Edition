package com.example.features.linkmanager

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.regex.Pattern

/**
 * Common interface for platform-specific link resolvers
 */
interface PlatformAdapter {
    val platform: SupportedPlatform
    val displayName: String
    fun canHandle(uri: URI): Boolean
    suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource>
}

/**
 * 1. Direct HTTP(S) File & Adaptive Stream Adapter (Baseline Universal Adapter)
 */
class DirectFileAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.DIRECT_HTTP
    override val displayName: String = "Direct HTTP(S) File / Stream"

    private val directExtensions = mapOf(
        // Video
        "mp4" to ("video/mp4" to LinkContentType.DIRECT_VIDEO),
        "mkv" to ("video/x-matroska" to LinkContentType.DIRECT_VIDEO),
        "webm" to ("video/webm" to LinkContentType.DIRECT_VIDEO),
        "mov" to ("video/quicktime" to LinkContentType.DIRECT_VIDEO),
        "3gp" to ("video/3gpp" to LinkContentType.DIRECT_VIDEO),
        // Audio
        "mp3" to ("audio/mpeg" to LinkContentType.DIRECT_AUDIO),
        "wav" to ("audio/wav" to LinkContentType.DIRECT_AUDIO),
        "m4a" to ("audio/mp4" to LinkContentType.DIRECT_AUDIO),
        "flac" to ("audio/flac" to LinkContentType.DIRECT_AUDIO),
        "ogg" to ("audio/ogg" to LinkContentType.DIRECT_AUDIO),
        "aac" to ("audio/aac" to LinkContentType.DIRECT_AUDIO),
        // Image
        "jpg" to ("image/jpeg" to LinkContentType.DIRECT_IMAGE),
        "jpeg" to ("image/jpeg" to LinkContentType.DIRECT_IMAGE),
        "png" to ("image/png" to LinkContentType.DIRECT_IMAGE),
        "webp" to ("image/webp" to LinkContentType.DIRECT_IMAGE),
        "gif" to ("image/gif" to LinkContentType.DIRECT_IMAGE),
        // Document & Archive
        "pdf" to ("application/pdf" to LinkContentType.DOCUMENT),
        "zip" to ("application/zip" to LinkContentType.ARCHIVE),
        "tar" to ("application/x-tar" to LinkContentType.ARCHIVE),
        "gz" to ("application/gzip" to LinkContentType.ARCHIVE),
        "7z" to ("application/x-7z-compressed" to LinkContentType.ARCHIVE),
        // Application
        "apk" to ("application/vnd.android.package-archive" to LinkContentType.APK_PACKAGE),
        // Streams
        "m3u8" to ("application/x-mpegURL" to LinkContentType.HLS_STREAM),
        "mpd" to ("application/dash+xml" to LinkContentType.DASH_STREAM)
    )

    override fun canHandle(uri: URI): Boolean {
        // Fallback universal handler for any HTTP/HTTPS URL
        return uri.scheme?.lowercase() in listOf("http", "https")
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        try {
            val path = uri.path ?: ""
            val ext = path.substringAfterLast('.', "").lowercase().substringBefore('?')
            val filenameFromPath = path.substringAfterLast('/').ifBlank { "downloaded_file.bin" }

            // Inspect HTTP headers via lightweight HEAD request
            var detectedContentType = directExtensions[ext]?.first ?: "application/octet-stream"
            var contentTypeEnum = directExtensions[ext]?.second ?: LinkContentType.UNKNOWN
            var contentLength: Long? = null
            var finalFilename = filenameFromPath

            try {
                val headReq = Request.Builder()
                    .url(rawUrl)
                    .head()
                    .header("User-Agent", "AetherVault-UniversalLinkManager/2.4")
                    .build()

                client.newCall(headReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val headerMime = response.header("Content-Type")?.substringBefore(';')
                        if (!headerMime.isNullOrBlank() && headerMime != "text/html") {
                            detectedContentType = headerMime
                            contentTypeEnum = mapMimeToType(headerMime, ext)
                        }

                        val length = response.header("Content-Length")?.toLongOrNull()
                        if (length != null && length > 0) {
                            contentLength = length
                        }

                        val contentDisp = response.header("Content-Disposition")
                        if (!contentDisp.isNullOrBlank() && contentDisp.contains("filename=")) {
                            val extracted = contentDisp.substringAfter("filename=").trim('"', '\'', ' ')
                            if (extracted.isNotBlank()) finalFilename = extracted
                        }
                    }
                }
            } catch (e: Exception) {
                // If HEAD fails, proceed with path-based detection
            }

            val isMedia = contentTypeEnum in listOf(
                LinkContentType.DIRECT_VIDEO,
                LinkContentType.DIRECT_AUDIO,
                LinkContentType.HLS_STREAM,
                LinkContentType.DASH_STREAM
            )

            val capabilities = LinkCapabilities(
                canStream = isMedia,
                canDownloadLocally = true,
                canUploadToTelegram = true,
                canOpenInApp = true,
                requiresAuthorization = false
            )

            val resource = ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = rawUrl,
                platform = SupportedPlatform.DIRECT_HTTP,
                contentType = contentTypeEnum,
                title = finalFilename,
                description = "Direct accessible remote file on ${uri.host ?: "remote host"}",
                fileSizeBytes = contentLength,
                mimeType = detectedContentType,
                suggestedFilename = finalFilename,
                capabilities = capabilities,
                metadataMap = mapOf(
                    "Host" to (uri.host ?: "unknown"),
                    "MIME" to detectedContentType,
                    "Length" to (contentLength?.toString() ?: "Unknown")
                )
            )
            Result.success(resource)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapMimeToType(mime: String, ext: String): LinkContentType {
        return when {
            mime.startsWith("video/") -> LinkContentType.DIRECT_VIDEO
            mime.startsWith("audio/") -> LinkContentType.DIRECT_AUDIO
            mime.startsWith("image/") -> LinkContentType.DIRECT_IMAGE
            mime == "application/pdf" -> LinkContentType.DOCUMENT
            mime == "application/vnd.android.package-archive" || ext == "apk" -> LinkContentType.APK_PACKAGE
            mime.contains("mpegurl") || ext == "m3u8" -> LinkContentType.HLS_STREAM
            mime.contains("dash") || ext == "mpd" -> LinkContentType.DASH_STREAM
            mime.contains("zip") || mime.contains("tar") || mime.contains("compressed") -> LinkContentType.ARCHIVE
            else -> LinkContentType.DOCUMENT
        }
    }
}

/**
 * 2. Google Drive & Google Docs Workspace Adapter
 */
class GoogleDriveAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.GOOGLE_DRIVE
    override val displayName: String = "Google Drive"

    override fun canHandle(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == "drive.google.com" || host == "docs.google.com"
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        val path = uri.path ?: ""
        val host = uri.host?.lowercase() ?: ""

        // Case A: Workspace Documents (Docs, Sheets, Slides)
        val docMatch = Regex("/(document|spreadsheets|presentation)/d/([a-zA-Z0-9_-]+)").find(path)
        if (docMatch != null) {
            val docType = docMatch.groupValues[1]
            val docId = docMatch.groupValues[2]

            val (exportFormat, exportMime, filenameExt) = when (docType) {
                "document" -> Triple("pdf", "application/pdf", "pdf")
                "spreadsheets" -> Triple("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
                "presentation" -> Triple("pdf", "application/pdf", "pdf")
                else -> Triple("pdf", "application/pdf", "pdf")
            }

            val exportUrl = "https://docs.google.com/$docType/d/$docId/export?format=$exportFormat"
            val title = "Google_${docType.replaceFirstChar { it.uppercase() }}_$docId"
            val filename = "$title.$filenameExt"

            return@withContext Result.success(
                ResolvedLinkResource(
                    rawUrl = rawUrl,
                    normalizedUrl = exportUrl,
                    platform = SupportedPlatform.GOOGLE_DRIVE,
                    contentType = LinkContentType.WORKSPACE_DOC,
                    title = title,
                    description = "Google Workspace document exportable to $exportFormat",
                    mimeType = exportMime,
                    suggestedFilename = filename,
                    capabilities = LinkCapabilities(
                        canStream = false,
                        canDownloadLocally = true,
                        canUploadToTelegram = true,
                        canOpenInApp = true
                    ),
                    metadataMap = mapOf(
                        "DocumentId" to docId,
                        "Type" to docType,
                        "ExportFormat" to exportFormat
                    )
                )
            )
        }

        // Case B: Google Drive Folder
        val folderMatch = Regex("/drive/folders/([a-zA-Z0-9_-]+)").find(path)
        if (folderMatch != null) {
            val folderId = folderMatch.groupValues[1]
            return@withContext Result.success(
                ResolvedLinkResource(
                    rawUrl = rawUrl,
                    normalizedUrl = rawUrl,
                    platform = SupportedPlatform.GOOGLE_DRIVE,
                    contentType = LinkContentType.DRIVE_FOLDER,
                    title = "Google Drive Folder ($folderId)",
                    description = "Google Drive folder container. Individual files can be managed inside the Drive explorer.",
                    suggestedFilename = "drive_folder_$folderId",
                    capabilities = LinkCapabilities(
                        canStream = false,
                        canDownloadLocally = false,
                        canUploadToTelegram = false,
                        canOpenInApp = true,
                        restrictionNotice = "Folder downloads require recursive file synchronization through the Google Drive client."
                    ),
                    metadataMap = mapOf("FolderId" to folderId)
                )
            )
        }

        // Case C: Standard Google Drive File
        val fileMatch = Regex("/file/d/([a-zA-Z0-9_-]+)").find(path)
            ?: Regex("[?&]id=([a-zA-Z0-9_-]+)").find(uri.query ?: "")

        val fileId = fileMatch?.groupValues?.get(1) ?: "unknown_file"
        val directDownloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
        val filename = "Drive_File_$fileId.bin"

        Result.success(
            ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = directDownloadUrl,
                platform = SupportedPlatform.GOOGLE_DRIVE,
                contentType = LinkContentType.DOCUMENT,
                title = "Google Drive File ($fileId)",
                description = "Publicly accessible Google Drive file resource",
                suggestedFilename = filename,
                capabilities = LinkCapabilities(
                    canStream = false,
                    canDownloadLocally = true,
                    canUploadToTelegram = true,
                    canOpenInApp = true
                ),
                metadataMap = mapOf(
                    "DriveFileId" to fileId,
                    "ExportUrl" to directDownloadUrl
                )
            )
        )
    }
}

/**
 * 3. YouTube Platform Adapter (Explicit Capability Boundaries)
 */
class YouTubeAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.YOUTUBE
    override val displayName: String = "YouTube"

    override fun canHandle(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(uri)

        // Query official YouTube oEmbed API for legal metadata and thumbnail
        var title = "YouTube Video ($videoId)"
        var author = "YouTube Creator"
        var thumbUrl: String? = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

        if (videoId != "unknown") {
            try {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                val req = Request.Builder().url(oEmbedUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        title = json.optString("title", title)
                        author = json.optString("author_name", author)
                        thumbUrl = json.optString("thumbnail_url", thumbUrl)
                    }
                }
            } catch (e: Exception) {
                // Keep fallback metadata
            }
        }

        val capabilities = LinkCapabilities(
            canStream = false,
            canDownloadLocally = false,
            canUploadToTelegram = false,
            canOpenInApp = true,
            isRestrictedOrDrm = true,
            restrictionNotice = "YouTube stream extraction blocked. YouTube Terms of Service require YouTube Premium for offline access. Use 'Open in Official App' for playback."
        )

        Result.success(
            ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = "https://www.youtube.com/watch?v=$videoId",
                platform = SupportedPlatform.YOUTUBE,
                contentType = LinkContentType.DIRECT_VIDEO,
                title = title,
                description = "Channel: $author • YouTube Media",
                thumbnailUrl = thumbUrl,
                suggestedFilename = "YouTube_${videoId}.mp4",
                capabilities = capabilities,
                metadataMap = mapOf(
                    "VideoId" to videoId,
                    "Author" to author,
                    "Compliance" to "Google ToS Compliant - DRM Protected"
                )
            )
        )
    }

    private fun extractVideoId(uri: URI): String {
        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""

        if (host == "youtu.be") {
            return path.trim('/').substringBefore('?')
        }

        if (path.contains("/shorts/")) {
            return path.substringAfter("/shorts/").substringBefore('/')
        }

        val query = uri.query ?: ""
        val vMatch = Regex("[?&]v=([a-zA-Z0-9_-]{11})").find("?$query")
        return vMatch?.groupValues?.get(1) ?: "unknown"
    }
}

/**
 * 4. Instagram Platform Adapter
 */
class InstagramAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.INSTAGRAM
    override val displayName: String = "Instagram"

    override fun canHandle(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == "instagram.com" || host.endsWith(".instagram.com") || host == "instagr.am"
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        val path = uri.path ?: ""
        val shortcode = Regex("/(p|reel|tv|stories)/([a-zA-Z0-9_-]+)").find(path)?.groupValues?.get(2) ?: "post"

        val capabilities = LinkCapabilities(
            canStream = false,
            canDownloadLocally = false,
            canUploadToTelegram = false,
            canOpenInApp = true,
            isRestrictedOrDrm = true,
            restrictionNotice = "Instagram media is protected by Meta terms and user account privacy controls. Direct unauthorized stream extraction is restricted."
        )

        Result.success(
            ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = rawUrl,
                platform = SupportedPlatform.INSTAGRAM,
                contentType = LinkContentType.SOCIAL_POST,
                title = "Instagram Post ($shortcode)",
                description = "Meta Instagram Social Media Post",
                suggestedFilename = "Instagram_${shortcode}.bin",
                capabilities = capabilities,
                metadataMap = mapOf("Shortcode" to shortcode)
            )
        )
    }
}

/**
 * 5. Facebook Platform Adapter
 */
class FacebookAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.FACEBOOK
    override val displayName: String = "Facebook"

    override fun canHandle(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch" || host == "fb.com"
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        val capabilities = LinkCapabilities(
            canStream = false,
            canDownloadLocally = false,
            canUploadToTelegram = false,
            canOpenInApp = true,
            isRestrictedOrDrm = true,
            restrictionNotice = "Facebook content requires authenticated Meta sessions and privacy authorization. Direct unauthorized extraction is restricted."
        )

        Result.success(
            ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = rawUrl,
                platform = SupportedPlatform.FACEBOOK,
                contentType = LinkContentType.SOCIAL_POST,
                title = "Facebook Video / Post",
                description = "Meta Facebook Social Resource",
                suggestedFilename = "Facebook_Resource.bin",
                capabilities = capabilities,
                metadataMap = mapOf("Host" to (uri.host ?: "facebook.com"))
            )
        )
    }
}

/**
 * 6. Google Play Store Listing Adapter
 */
class GooglePlayAdapter : PlatformAdapter {
    override val platform: SupportedPlatform = SupportedPlatform.GOOGLE_PLAY
    override val displayName: String = "Google Play Store"

    override fun canHandle(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return (host == "play.google.com" || host.endsWith(".play.google.com")) && (uri.path?.contains("/store/apps/") == true)
    }

    override suspend fun resolve(rawUrl: String, uri: URI, client: OkHttpClient): Result<ResolvedLinkResource> = withContext(Dispatchers.IO) {
        val query = uri.query ?: ""
        val pkgMatch = Regex("[?&]id=([a-zA-Z0-9_.]+)").find("?$query")
        val packageId = pkgMatch?.groupValues?.get(1) ?: "unknown.package"

        val capabilities = LinkCapabilities(
            canStream = false,
            canDownloadLocally = false,
            canUploadToTelegram = false,
            canOpenInApp = true,
            isRestrictedOrDrm = true,
            restrictionNotice = "Google Play Store listing. Google Play binaries are dynamically signed and delivered via Play Core. Direct APK download requires an independently supplied APK source (e.g. GitHub Releases / APK mirror)."
        )

        Result.success(
            ResolvedLinkResource(
                rawUrl = rawUrl,
                normalizedUrl = rawUrl,
                platform = SupportedPlatform.GOOGLE_PLAY,
                contentType = LinkContentType.APP_LISTING,
                title = "Play Store: $packageId",
                description = "Official Google Play application listing",
                suggestedFilename = "${packageId}.apk",
                capabilities = capabilities,
                metadataMap = mapOf("PackageId" to packageId)
            )
        )
    }
}
