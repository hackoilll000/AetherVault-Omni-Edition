package com.example.core.artifact

import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

data class SaveFileEntry(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val lastModifiedTimestamp: Long
)

data class SaveStateSnapshot(
    val snapshotId: String,
    val artifactId: String,
    val versionNumber: Long,
    val timestamp: Long,
    val fileEntries: List<SaveFileEntry>,
    val encryptedSnapshotChecksum: String
)

enum class SyncConflictPolicy {
    PRESERVE_LOCAL,
    OVERWRITE_REMOTE,
    FAIL_SAFE_ABORT
}

class SaveStateSyncEngine(private val savesRootDirectory: File) {

    init {
        if (!savesRootDirectory.exists()) {
            savesRootDirectory.mkdirs()
        }
    }

    fun scanSaveDirectory(appSaveDir: File): List<SaveFileEntry> {
        if (!appSaveDir.exists()) return emptyList()
        val entries = mutableListOf<SaveFileEntry>()
        val rootPath = appSaveDir.canonicalPath

        appSaveDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relPath = file.canonicalPath.removePrefix(rootPath).removePrefix(File.separator)
            val checksum = calculateFileSha256(file)
            entries.add(
                SaveFileEntry(
                    relativePath = relPath,
                    sizeBytes = file.length(),
                    sha256Checksum = checksum,
                    lastModifiedTimestamp = file.lastModified()
                )
            )
        }
        return entries
    }

    fun detectDeltas(currentEntries: List<SaveFileEntry>, previousSnapshot: SaveStateSnapshot?): Boolean {
        if (previousSnapshot == null) return currentEntries.isNotEmpty()
        if (currentEntries.size != previousSnapshot.fileEntries.size) return true

        val prevMap = previousSnapshot.fileEntries.associateBy { it.relativePath }
        for (curr in currentEntries) {
            val prev = prevMap[curr.relativePath] ?: return true
            if (curr.sha256Checksum != prev.sha256Checksum) return true
        }
        return false
    }

    fun createSnapshot(artifactId: String, versionNumber: Long, appSaveDir: File): SaveStateSnapshot {
        val entries = scanSaveDirectory(appSaveDir)
        val combinedHashes = entries.joinToString("") { it.sha256Checksum }
        val digest = MessageDigest.getInstance("SHA-256")
        val snapshotChecksum = digest.digest(combinedHashes.toByteArray()).joinToString("") { "%02x".format(it) }

        return SaveStateSnapshot(
            snapshotId = "snap_${artifactId}_${System.currentTimeMillis()}",
            artifactId = artifactId,
            versionNumber = versionNumber,
            timestamp = System.currentTimeMillis(),
            fileEntries = entries,
            encryptedSnapshotChecksum = snapshotChecksum
        )
    }

    private fun calculateFileSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
