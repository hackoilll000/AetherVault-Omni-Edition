package com.example.core.artifact

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class ArtifactChunkSpec(
    val chunkIndex: Int,
    val telegramMessageId: Long,
    val telegramFileId: String,
    val rawSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val sha256Checksum: String,
    val ivHex: String
)

data class ArtifactMetadata(
    val artifactId: String,
    val name: String,
    val version: String,
    val totalSizeBytes: Long,
    val totalChunks: Int,
    val fullPackageSha256: String,
    val executableRelativePath: String,
    val saveDirectoryRelativePath: String = "saves",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val generationNumber: Long = 1L,
    val isPinned: Boolean = false
)

data class ArtifactManifest(
    val metadata: ArtifactMetadata,
    val chunks: List<ArtifactChunkSpec>
) {
    fun verifyChunkIntegrity(chunkIndex: Int, chunkData: ByteArray): Boolean {
        val spec = chunks.find { it.chunkIndex == chunkIndex } ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        val calculatedHex = digest.digest(chunkData).joinToString("") { "%02x".format(it) }
        return calculatedHex.equals(spec.sha256Checksum, ignoreCase = true)
    }

    fun verifyFullPackageIntegrity(reconstructedData: ByteArray): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val calculatedHex = digest.digest(reconstructedData).joinToString("") { "%02x".format(it) }
        return calculatedHex.equals(metadata.fullPackageSha256, ignoreCase = true)
    }

    fun toJson(): String {
        val root = JSONObject()
        val metaObj = JSONObject()
        metaObj.put("artifactId", metadata.artifactId)
        metaObj.put("name", metadata.name)
        metaObj.put("version", metadata.version)
        metaObj.put("totalSizeBytes", metadata.totalSizeBytes)
        metaObj.put("totalChunks", metadata.totalChunks)
        metaObj.put("fullPackageSha256", metadata.fullPackageSha256)
        metaObj.put("executableRelativePath", metadata.executableRelativePath)
        metaObj.put("saveDirectoryRelativePath", metadata.saveDirectoryRelativePath)
        metaObj.put("createdAtTimestamp", metadata.createdAtTimestamp)
        metaObj.put("generationNumber", metadata.generationNumber)
        metaObj.put("isPinned", metadata.isPinned)
        root.put("metadata", metaObj)

        val chunkArr = JSONArray()
        for (c in chunks) {
            val cObj = JSONObject()
            cObj.put("chunkIndex", c.chunkIndex)
            cObj.put("telegramMessageId", c.telegramMessageId)
            cObj.put("telegramFileId", c.telegramFileId)
            cObj.put("rawSizeBytes", c.rawSizeBytes)
            cObj.put("encryptedSizeBytes", c.encryptedSizeBytes)
            cObj.put("sha256Checksum", c.sha256Checksum)
            cObj.put("ivHex", c.ivHex)
            chunkArr.put(cObj)
        }
        root.put("chunks", chunkArr)
        return root.toString(2)
    }

    companion object {
        fun calculateSha256(data: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(data).joinToString("") { "%02x".format(it) }
        }

        fun fromJson(jsonStr: String): ArtifactManifest {
            val root = JSONObject(jsonStr)
            val metaObj = root.getJSONObject("metadata")
            val metadata = ArtifactMetadata(
                artifactId = metaObj.getString("artifactId"),
                name = metaObj.getString("name"),
                version = metaObj.getString("version"),
                totalSizeBytes = metaObj.getLong("totalSizeBytes"),
                totalChunks = metaObj.getInt("totalChunks"),
                fullPackageSha256 = metaObj.getString("fullPackageSha256"),
                executableRelativePath = metaObj.getString("executableRelativePath"),
                saveDirectoryRelativePath = metaObj.optString("saveDirectoryRelativePath", "saves"),
                createdAtTimestamp = metaObj.optLong("createdAtTimestamp", System.currentTimeMillis()),
                generationNumber = metaObj.optLong("generationNumber", 1L),
                isPinned = metaObj.optBoolean("isPinned", false)
            )

            val chunkArr = root.getJSONArray("chunks")
            val chunks = mutableListOf<ArtifactChunkSpec>()
            for (i in 0 until chunkArr.length()) {
                val cObj = chunkArr.getJSONObject(i)
                chunks.add(
                    ArtifactChunkSpec(
                        chunkIndex = cObj.getInt("chunkIndex"),
                        telegramMessageId = cObj.getLong("telegramMessageId"),
                        telegramFileId = cObj.getString("telegramFileId"),
                        rawSizeBytes = cObj.getLong("rawSizeBytes"),
                        encryptedSizeBytes = cObj.getLong("encryptedSizeBytes"),
                        sha256Checksum = cObj.getString("sha256Checksum"),
                        ivHex = cObj.getString("ivHex")
                    )
                )
            }
            return ArtifactManifest(metadata, chunks)
        }
    }
}
