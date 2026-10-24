package com.example.features.linkmanager

import java.util.UUID

/**
 * Recognized source platforms for the Universal Link Manager
 */
enum class SupportedPlatform(val displayName: String, val iconName: String) {
    DIRECT_HTTP("Direct File / Stream", "cloud_download"),
    GOOGLE_DRIVE("Google Drive", "add_to_drive"),
    YOUTUBE("YouTube", "smart_display"),
    INSTAGRAM("Instagram", "photo_camera"),
    FACEBOOK("Facebook", "share"),
    GOOGLE_PLAY("Google Play Store", "shop"),
    GENERIC_WEB("Generic Webpage", "public"),
    UNSUPPORTED("Unsupported / Blocked", "block")
}

/**
 * Categorization of the resolved remote resource
 */
enum class LinkContentType(val label: String) {
    DIRECT_VIDEO("Direct Video"),
    DIRECT_AUDIO("Direct Audio"),
    DIRECT_IMAGE("Direct Image"),
    DOCUMENT("Document"),
    ARCHIVE("Archive / Compressed"),
    APK_PACKAGE("Android Application Package (APK)"),
    HLS_STREAM("HLS Adaptive Stream (.m3u8)"),
    DASH_STREAM("DASH Adaptive Stream (.mpd)"),
    WORKSPACE_DOC("Google Workspace Document"),
    DRIVE_FOLDER("Cloud Folder"),
    SOCIAL_POST("Social Media Post / Reel"),
    APP_LISTING("Application Store Listing"),
    WEB_PAGE("Web Document"),
    UNKNOWN("Unknown Content")
}

/**
 * Explicit capability boundaries for the resolved resource
 */
data class LinkCapabilities(
    val canStream: Boolean = false,
    val canDownloadLocally: Boolean = false,
    val canUploadToTelegram: Boolean = false,
    val canOpenInApp: Boolean = true,
    val requiresAuthorization: Boolean = false,
    val authorizationType: String? = null,
    val isRestrictedOrDrm: Boolean = false,
    val restrictionNotice: String? = null
)

/**
 * Normalized representation of a resolved remote resource
 */
data class ResolvedLinkResource(
    val rawUrl: String,
    val normalizedUrl: String,
    val platform: SupportedPlatform,
    val contentType: LinkContentType,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val fileSizeBytes: Long? = null,
    val mimeType: String = "application/octet-stream",
    val suggestedFilename: String,
    val capabilities: LinkCapabilities,
    val metadataMap: Map<String, String> = emptyMap()
)

/**
 * Strict state model for Link Manager transfers
 */
enum class TransferStage(val userLabel: String) {
    QUEUED("Queued"),
    VALIDATING("Validating Link"),
    RESOLVING("Resolving Platform"),
    WAITING_FOR_AUTHORIZATION("Waiting for Authorization"),
    FETCHING_METADATA("Fetching Metadata"),
    READY("Ready for Action"),
    STREAMING("Streaming Preview"),
    DOWNLOADING("Downloading Content"),
    VERIFYING("Verifying Integrity"),
    UPLOADING("Uploading to Telegram"),
    RETRY_WAIT("Retrying after Error"),
    CANCEL_REQUESTED("Cancelling"),
    COMPLETED("Transfer Completed"),
    FAILED("Transfer Failed"),
    CANCELLED("Transfer Cancelled")
}

enum class TransferDestination(val label: String) {
    LOCAL_STORAGE("Local Device Storage"),
    TELEGRAM_CLOUD("Telegram Private Channel")
}

/**
 * Active or completed transfer record
 */
data class LinkTransferTask(
    val taskId: String = "link_task_${UUID.randomUUID().toString().take(8)}",
    val resource: ResolvedLinkResource,
    val destination: TransferDestination,
    val destinationDetails: String,
    val stage: TransferStage = TransferStage.QUEUED,
    val downloadProgressPercent: Float = 0f,
    val uploadProgressPercent: Float = 0f,
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val telegramMessageId: Long? = null,
    val localSavedUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
