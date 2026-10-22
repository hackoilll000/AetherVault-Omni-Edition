package com.example.core.artifact

import java.io.File

data class CacheItemEntry(
    val artifactId: String,
    val directory: File,
    val sizeBytes: Long,
    var lastAccessedTimestamp: Long,
    var isPinned: Boolean = false,
    var activeSessionLeaseCount: Int = 0
)

class ArtifactCacheManager(
    private val cacheDirectory: File,
    val maxCacheSizeBytes: Long = 2000 * 1024 * 1024L, // 2 GB default quota
    val minFreeSpaceBytes: Long = 100 * 1024 * 1024L // 100 MB free reserve
) {

    private val cacheRegistry = mutableMapOf<String, CacheItemEntry>()

    fun registerCacheItem(artifactId: String, directory: File, sizeBytes: Long, isPinned: Boolean = false) {
        cacheRegistry[artifactId] = CacheItemEntry(
            artifactId = artifactId,
            directory = directory,
            sizeBytes = sizeBytes,
            lastAccessedTimestamp = System.currentTimeMillis(),
            isPinned = isPinned,
            activeSessionLeaseCount = 0
        )
    }

    fun acquireActiveLease(artifactId: String) {
        val item = cacheRegistry[artifactId]
        if (item != null) {
            item.activeSessionLeaseCount++
            item.lastAccessedTimestamp = System.currentTimeMillis()
        }
    }

    fun releaseActiveLease(artifactId: String) {
        val item = cacheRegistry[artifactId]
        if (item != null && item.activeSessionLeaseCount > 0) {
            item.activeSessionLeaseCount--
            item.lastAccessedTimestamp = System.currentTimeMillis()
        }
    }

    fun getTotalCacheSizeBytes(): Long {
        return cacheRegistry.values.sumOf { it.sizeBytes }
    }

    fun evictLruCacheIfNeeded(): List<String> {
        val evictedArtifacts = mutableListOf<String>()
        var currentTotal = getTotalCacheSizeBytes()
        val usableSpace = cacheDirectory.usableSpace

        if (currentTotal <= maxCacheSizeBytes && usableSpace >= minFreeSpaceBytes) {
            return evictedArtifacts
        }

        // Candidates for eviction: NOT pinned, activeSessionLeaseCount == 0, sorted by LRU
        val candidates = cacheRegistry.values
            .filter { !it.isPinned && it.activeSessionLeaseCount == 0 }
            .sortedBy { it.lastAccessedTimestamp }

        for (item in candidates) {
            if (currentTotal <= maxCacheSizeBytes && cacheDirectory.usableSpace >= minFreeSpaceBytes) {
                break
            }
            if (item.directory.exists()) {
                item.directory.deleteRecursively()
            }
            cacheRegistry.remove(item.artifactId)
            evictedArtifacts.add(item.artifactId)
            currentTotal -= item.sizeBytes
        }

        return evictedArtifacts
    }
}
