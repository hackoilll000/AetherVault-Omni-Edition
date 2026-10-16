package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_folders")
data class VirtualFolder(
    @PrimaryKey val id: String,
    val parentId: String? = null,
    val name: String,
    val colorHex: String = "#00F0FF",
    val iconName: String = "folder",
    val isStarred: Boolean = false,
    val isTrashed: Boolean = false,
    val syncLocalPath: String? = null,
    val hlcTimestamp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["sha256Hash"]),
        Index(value = ["isStarred"]),
        Index(value = ["isTrashed"]),
        Index(value = ["modifiedAt"])
    ]
)
data class VaultFile(
    @PrimaryKey val id: String,
    val folderId: String = "root",
    val name: String,
    val extension: String = "",
    val mimeType: String = "application/octet-stream",
    val totalSizeBytes: Long = 0L,
    val sha256Hash: String = "",
    val encryptedFileKey: ByteArray = ByteArray(0),
    val microThumbWebp: ByteArray? = null,
    val thumbMsgId: Int = 0,
    val currentVersion: Int = 1,
    val isStarred: Boolean = false,
    val isOfflinePinned: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val hlcTimestamp: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultFile
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "file_versions")
data class FileVersion(
    @PrimaryKey val versionId: String,
    val fileId: String,
    val versionNumber: Int,
    val sizeBytes: Long,
    val sha256Hash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceName: String = "Android Device"
)

@Entity(
    tableName = "file_chunks",
    indices = [
        Index(value = ["fileId", "chunkIndex"], unique = true)
    ]
)
data class FileChunk(
    @PrimaryKey val id: String,
    val fileId: String,
    val versionId: String,
    val chunkIndex: Int,
    val telegramMessageId: Int,
    val uploaderBotId: Long,
    val botFileIdsJson: String = "{}",
    val chunkSizeBytes: Int,
    val ivNonce: ByteArray = ByteArray(0),
    val authTag: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunk
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(
    tableName = "media_metadata",
    indices = [
        Index(value = ["takenAtTimestamp"])
    ]
)
data class MediaMetadata(
    @PrimaryKey val fileId: String,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val takenAtTimestamp: Long = System.currentTimeMillis(),
    val cameraModel: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isMotionPhoto: Boolean = false,
    val motionVideoOffset: Long = 0L,
    val aiTagsJson: String = "[]",
    val ocrText: String = "",
    val faceClusterId: String = ""
)

@Entity(tableName = "teledrop_stream")
data class TeleDropItem(
    @PrimaryKey val itemId: String,
    val type: String, // TEXT, LINK, CODE, IMAGE, FILE
    val content: String,
    val sourceDevice: String = "Android",
    val telegramMsgId: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfer_queue")
data class TransferTask(
    @PrimaryKey val taskId: String,
    val fileId: String,
    val fileName: String,
    val localPath: String = "",
    val targetFolderId: String = "root",
    val direction: String, // UPLOAD, DOWNLOAD, SYNC
    val status: String = "QUEUED", // QUEUED, ACTIVE, PAUSED, COMPLETED, ERROR
    val completedChunks: Int = 0,
    val totalChunks: Int = 1,
    val speedBps: Long = 0L,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String = ""
)

@Entity(tableName = "bot_swarm")
data class BotSwarmEntity(
    @PrimaryKey val botId: Long,
    val token: String,
    val username: String,
    val isHealthy: Boolean = true,
    val rpmCount: Int = 0,
    val cooldownUntil: Long = 0L
)

@Entity(tableName = "albums")
data class VaultAlbum(
    @PrimaryKey val albumId: String,
    val title: String,
    val coverFileId: String? = null,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "album_files",
    indices = [
        Index(value = ["albumId", "fileId"], unique = true)
    ]
)
data class AlbumFileMap(
    @PrimaryKey val id: String,
    val albumId: String,
    val fileId: String,
    val addedAt: Long = System.currentTimeMillis()
)
