package com.example.core.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class CameraSyncStatus(
    val isSyncing: Boolean = false,
    val totalPending: Int = 0,
    val syncedCount: Int = 0,
    val currentFileName: String = "",
    val message: String = "Idle"
)

object CameraRollSyncManager {
    private const val TAG = "CameraRollSync"
    private const val PREFS_NAME = "aethervault_camera_sync"
    private const val KEY_AUTO_SYNC = "auto_camera_sync_enabled"

    private val _syncStatus = MutableStateFlow(CameraSyncStatus())
    val syncStatus: StateFlow<CameraSyncStatus> = _syncStatus.asStateFlow()

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC, false)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    /**
     * Scans DCIM / MediaStore for photos and videos, checks if they already exist in the Vault,
     * and automatically uploads any new media to Telegram Cloud.
     */
    suspend fun performCameraRollSync(
        context: Context,
        database: AppDatabase,
        botTokens: List<String>,
        channelId: Long,
        onFileUploaded: suspend (Uri) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (_syncStatus.value.isSyncing) return@withContext

        _syncStatus.value = CameraSyncStatus(isSyncing = true, message = "Scanning Camera Roll...")

        try {
            val existingFiles = database.fileDao().getAllFilesList()
            val existingNames = existingFiles.map { it.name }.toSet()

            val pendingUris = mutableListOf<Pair<Uri, String>>()

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            // Scan Images
            val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                imageUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                var count = 0
                while (cursor.moveToNext() && count < 25) { // batch up to 25 recent items per sync
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "camera_photo_$id.jpg"
                    if (!existingNames.contains(name)) {
                        val contentUri = Uri.withAppendedPath(imageUri, id.toString())
                        pendingUris.add(Pair(contentUri, name))
                        count++
                    }
                }
            }

            // Scan Videos
            val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                videoUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                var count = 0
                while (cursor.moveToNext() && count < 10) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "camera_video_$id.mp4"
                    if (!existingNames.contains(name)) {
                        val contentUri = Uri.withAppendedPath(videoUri, id.toString())
                        pendingUris.add(Pair(contentUri, name))
                        count++
                    }
                }
            }

            if (pendingUris.isEmpty()) {
                _syncStatus.value = CameraSyncStatus(
                    isSyncing = false,
                    message = "✓ All Camera Photos & Videos are 100% Backed Up"
                )
                return@withContext
            }

            _syncStatus.value = CameraSyncStatus(
                isSyncing = true,
                totalPending = pendingUris.size,
                syncedCount = 0,
                message = "Found ${pendingUris.size} new items to encrypt & backup"
            )

            var synced = 0
            for ((uri, name) in pendingUris) {
                _syncStatus.value = CameraSyncStatus(
                    isSyncing = true,
                    totalPending = pendingUris.size,
                    syncedCount = synced,
                    currentFileName = name,
                    message = "Backing up: $name (${synced + 1}/${pendingUris.size})"
                )

                try {
                    onFileUploaded(uri)
                    synced++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing item $name: ${e.message}")
                }
            }

            _syncStatus.value = CameraSyncStatus(
                isSyncing = false,
                syncedCount = synced,
                totalPending = pendingUris.size,
                message = "✓ Successfully backed up $synced new media to Telegram Cloud"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera roll sync error", e)
            _syncStatus.value = CameraSyncStatus(
                isSyncing = false,
                message = "Sync paused: ${e.message}"
            )
        }
    }
}
