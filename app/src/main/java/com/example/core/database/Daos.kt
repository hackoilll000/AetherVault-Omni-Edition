package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM virtual_folders WHERE isTrashed = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<VirtualFolder>>

    @Query("SELECT * FROM virtual_folders WHERE parentId = :parentId AND isTrashed = 0 ORDER BY name ASC")
    fun getFoldersInParent(parentId: String): Flow<List<VirtualFolder>>

    @Query("SELECT * FROM virtual_folders WHERE parentId IS NULL AND isTrashed = 0 ORDER BY name ASC")
    fun getRootFolders(): Flow<List<VirtualFolder>>

    @Query("SELECT * FROM virtual_folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): VirtualFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VirtualFolder)

    @Update
    suspend fun updateFolder(folder: VirtualFolder)

    @Query("UPDATE virtual_folders SET isTrashed = 1 WHERE id = :id")
    suspend fun trashFolder(id: String)

    @Query("SELECT * FROM virtual_folders")
    suspend fun getAllFoldersList(): List<VirtualFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<VirtualFolder>)

    @Query("DELETE FROM virtual_folders WHERE id = :id")
    suspend fun deleteFolder(id: String)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE isTrashed = 0 ORDER BY modifiedAt DESC")
    fun getAllActiveFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE folderId = :folderId AND isTrashed = 0 ORDER BY modifiedAt DESC")
    fun getFilesInFolder(folderId: String): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE (mimeType LIKE 'image/%' OR mimeType LIKE 'video/%') AND isTrashed = 0 ORDER BY modifiedAt DESC")
    fun getMediaFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE isStarred = 1 AND isTrashed = 0 ORDER BY modifiedAt DESC")
    fun getStarredFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE isOfflinePinned = 1 AND isTrashed = 0 ORDER BY modifiedAt DESC")
    fun getOfflineFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM files WHERE id = :fileId LIMIT 1")
    suspend fun getFileById(fileId: String): VaultFile?

    @Query("SELECT * FROM files WHERE sha256Hash = :hash LIMIT 1")
    suspend fun getFileByHash(hash: String): VaultFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<VaultFile>)

    @Update
    suspend fun updateFile(file: VaultFile)

    @Query("UPDATE files SET isStarred = :isStarred WHERE id = :fileId")
    suspend fun setStarred(fileId: String, isStarred: Boolean)

    @Query("UPDATE files SET isOfflinePinned = :isPinned WHERE id = :fileId")
    suspend fun setOfflinePinned(fileId: String, isPinned: Boolean)

    @Query("UPDATE files SET isTrashed = 1, trashedAt = :timestamp WHERE id = :fileId")
    suspend fun moveToTrash(fileId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE files SET isTrashed = 0, trashedAt = 0 WHERE id = :fileId")
    suspend fun restoreFromTrash(fileId: String)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun permanentDelete(fileId: String)

    @Query("DELETE FROM files WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("UPDATE files SET folderId = :targetFolderId WHERE id IN (:fileIds)")
    suspend fun moveFilesToFolder(fileIds: List<String>, targetFolderId: String)

    @Query("UPDATE files SET isTrashed = 1, trashedAt = :timestamp WHERE id IN (:fileIds)")
    suspend fun moveMultipleToTrash(fileIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM files WHERE id IN (:fileIds)")
    suspend fun permanentDeleteMultiple(fileIds: List<String>)

    @Query("DELETE FROM files WHERE isTrashed = 1 AND trashedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM files WHERE isTrashed = 0")
    fun getActiveFileCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalSizeBytes), 0) FROM files WHERE isTrashed = 0")
    fun getTotalStorageUsed(): Flow<Long>

    @Query("SELECT * FROM files")
    suspend fun getAllFilesList(): List<VaultFile>
}

@Dao
interface ChunkDao {
    @Query("SELECT * FROM file_chunks WHERE fileId = :fileId ORDER BY chunkIndex ASC")
    suspend fun getChunksForFile(fileId: String): List<FileChunk>

    @Query("SELECT * FROM file_chunks WHERE fileId = :fileId ORDER BY chunkIndex ASC")
    fun observeChunksForFile(fileId: String): Flow<List<FileChunk>>

    @Query("SELECT * FROM file_chunks")
    suspend fun getAllChunksList(): List<FileChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: FileChunk)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<FileChunk>)

    @Query("DELETE FROM file_chunks WHERE fileId = :fileId")
    suspend fun deleteChunksForFile(fileId: String)
}

@Dao
interface TeleDropDao {
    @Query("SELECT * FROM teledrop_stream ORDER BY createdAt DESC")
    fun getTeleDropStream(): Flow<List<TeleDropItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TeleDropItem)

    @Query("DELETE FROM teledrop_stream WHERE itemId = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM teledrop_stream")
    suspend fun clearAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_queue ORDER BY updatedAt DESC")
    fun getAllTransfers(): Flow<List<TransferTask>>

    @Query("SELECT * FROM transfer_queue WHERE status = 'ACTIVE' OR status = 'QUEUED' ORDER BY updatedAt DESC")
    fun getActiveTransfers(): Flow<List<TransferTask>>

    @Query("SELECT * FROM transfer_queue WHERE taskId = :taskId LIMIT 1")
    suspend fun getTransferById(taskId: String): TransferTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransfer(task: TransferTask)

    @Query("UPDATE transfer_queue SET status = :status, updatedAt = :time WHERE taskId = :taskId")
    suspend fun updateStatus(taskId: String, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE transfer_queue SET completedChunks = :completed, bytesTransferred = :bytes, speedBps = :speed, updatedAt = :time WHERE taskId = :taskId")
    suspend fun updateProgress(taskId: String, completed: Int, bytes: Long, speed: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM transfer_queue WHERE taskId = :taskId")
    suspend fun deleteTransfer(taskId: String)

    @Query("DELETE FROM transfer_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}

@Dao
interface BotDao {
    @Query("SELECT * FROM bot_swarm ORDER BY botId ASC")
    fun getAllBots(): Flow<List<BotSwarmEntity>>

    @Query("SELECT * FROM bot_swarm ORDER BY botId ASC")
    suspend fun getAllBotsList(): List<BotSwarmEntity>

    @Query("SELECT * FROM bot_swarm WHERE isHealthy = 1 AND cooldownUntil <= :now ORDER BY rpmCount ASC")
    suspend fun getHealthyBots(now: Long = System.currentTimeMillis()): List<BotSwarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBot(bot: BotSwarmEntity)

    @Query("UPDATE bot_swarm SET isHealthy = :isHealthy, cooldownUntil = :cooldown WHERE botId = :botId")
    suspend fun updateHealth(botId: Long, isHealthy: Boolean, cooldown: Long)

    @Query("UPDATE bot_swarm SET rpmCount = rpmCount + 1 WHERE botId = :botId")
    suspend fun incrementRpm(botId: Long)

    @Query("UPDATE bot_swarm SET rpmCount = 0")
    suspend fun resetRpmCounters()

    @Query("DELETE FROM bot_swarm WHERE botId = :botId")
    suspend fun deleteBot(botId: Long)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_metadata WHERE fileId = :fileId LIMIT 1")
    suspend fun getMetadata(fileId: String): MediaMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: MediaMetadata)
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<VaultAlbum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: VaultAlbum)

    @Query("DELETE FROM albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: String)
}
