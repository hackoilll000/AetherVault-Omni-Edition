package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VirtualFolder::class,
        VaultFile::class,
        FileVersion::class,
        FileChunk::class,
        MediaMetadata::class,
        TeleDropItem::class,
        TransferTask::class,
        BotSwarmEntity::class,
        VaultAlbum::class,
        AlbumFileMap::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun chunkDao(): ChunkDao
    abstract fun teleDropDao(): TeleDropDao
    abstract fun transferDao(): TransferDao
    abstract fun botDao(): BotDao
    abstract fun mediaDao(): MediaDao
    abstract fun albumDao(): AlbumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var DECOY_INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, isDecoy: Boolean = false): AppDatabase {
            return if (isDecoy) {
                DECOY_INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aethervault_decoy.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    DECOY_INSTANCE = instance
                    instance
                }
            } else {
                INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aethervault_primary.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
