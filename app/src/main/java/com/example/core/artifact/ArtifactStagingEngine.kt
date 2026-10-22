package com.example.core.artifact

import java.io.File
import java.io.FileOutputStream

sealed class StagingResult {
    data class Success(val stagedDirectory: File, val executableFile: File) : StagingResult()
    data class Error(val message: String, val cause: Throwable? = null) : StagingResult()
}

class ArtifactStagingEngine(private val stagingRoot: File) {

    init {
        if (!stagingRoot.exists()) {
            stagingRoot.mkdirs()
        }
    }

    fun isPathSafe(targetFile: File): Boolean {
        val canonicalRoot = stagingRoot.canonicalPath
        val canonicalTarget = targetFile.canonicalPath
        return canonicalTarget.startsWith(canonicalRoot)
    }

    fun checkAvailableDiskSpace(requiredBytes: Long, reserveBytes: Long = 100 * 1024 * 1024L): Boolean {
        val freeBytes = stagingRoot.usableSpace
        return freeBytes >= (requiredBytes + reserveBytes)
    }

    fun stageArtifact(
        manifest: ArtifactManifest,
        decryptedChunkProvider: (ArtifactChunkSpec) -> ByteArray
    ): StagingResult {
        if (!checkAvailableDiskSpace(manifest.metadata.totalSizeBytes)) {
            return StagingResult.Error("Insufficient disk space available for artifact staging. Required: ${manifest.metadata.totalSizeBytes} bytes.")
        }

        val tempDir = File(stagingRoot, "temp_${manifest.metadata.artifactId}_${System.currentTimeMillis()}")
        val finalDir = File(stagingRoot, "app_${manifest.metadata.artifactId}_gen${manifest.metadata.generationNumber}")

        if (!tempDir.exists()) tempDir.mkdirs()

        return try {
            val packageFile = File(tempDir, "package.bin")
            if (!isPathSafe(packageFile)) {
                return StagingResult.Error("Security Error: Path traversal attempt detected!")
            }

            FileOutputStream(packageFile).use { fos ->
                val sortedChunks = manifest.chunks.sortedBy { it.chunkIndex }
                for (chunkSpec in sortedChunks) {
                    val chunkData = decryptedChunkProvider(chunkSpec)
                    if (!manifest.verifyChunkIntegrity(chunkSpec.chunkIndex, chunkData)) {
                        tempDir.deleteRecursively()
                        return StagingResult.Error("Checksum Verification Failed for chunk ${chunkSpec.chunkIndex}!")
                    }
                    fos.write(chunkData)
                }
            }

            val executableFile = File(tempDir, manifest.metadata.executableRelativePath)
            if (!executableFile.exists()) {
                // If single binary package, mark packageFile executable
                packageFile.setExecutable(true)
            } else {
                executableFile.setExecutable(true)
            }

            // Atomic promotion
            if (finalDir.exists()) {
                finalDir.deleteRecursively()
            }
            val promoted = tempDir.renameTo(finalDir)
            if (!promoted) {
                // Fallback copy if cross-filesystem rename fails
                tempDir.copyRecursively(finalDir, overwrite = true)
                tempDir.deleteRecursively()
            }

            val stagedExec = File(finalDir, manifest.metadata.executableRelativePath).takeIf { it.exists() } ?: File(finalDir, "package.bin")
            StagingResult.Success(finalDir, stagedExec)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            StagingResult.Error("Artifact Staging Failed: ${e.localizedMessage}", e)
        }
    }
}
