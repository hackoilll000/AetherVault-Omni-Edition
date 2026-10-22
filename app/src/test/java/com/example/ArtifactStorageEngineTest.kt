package com.example

import com.example.core.artifact.*
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ArtifactStorageEngineTest {

    private lateinit var testDir: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        testDir = File(context.cacheDir, "artifact_test_root_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @Test
    fun testArtifactManifestSerializationAndChecksum() {
        val dataChunk1 = "Hello World Chunk 1".toByteArray()
        val dataChunk2 = "Hello World Chunk 2".toByteArray()

        val checksum1 = ArtifactManifest.calculateSha256(dataChunk1)
        val checksum2 = ArtifactManifest.calculateSha256(dataChunk2)

        val fullData = dataChunk1 + dataChunk2
        val fullChecksum = ArtifactManifest.calculateSha256(fullData)

        val spec1 = ArtifactChunkSpec(0, 101L, "file_101", dataChunk1.size.toLong(), dataChunk1.size.toLong() + 16, checksum1, "001122334455")
        val spec2 = ArtifactChunkSpec(1, 102L, "file_102", dataChunk2.size.toLong(), dataChunk2.size.toLong() + 16, checksum2, "66778899aabb")

        val metadata = ArtifactMetadata(
            artifactId = "art_123",
            name = "Test Artifact App",
            version = "1.0.0",
            totalSizeBytes = fullData.size.toLong(),
            totalChunks = 2,
            fullPackageSha256 = fullChecksum,
            executableRelativePath = "main.sh"
        )

        val manifest = ArtifactManifest(metadata, listOf(spec1, spec2))

        assertTrue(manifest.verifyChunkIntegrity(0, dataChunk1))
        assertTrue(manifest.verifyChunkIntegrity(1, dataChunk2))
        assertTrue(manifest.verifyFullPackageIntegrity(fullData))

        val json = manifest.toJson()
        val deserialized = ArtifactManifest.fromJson(json)

        assertEquals("art_123", deserialized.metadata.artifactId)
        assertEquals(2, deserialized.chunks.size)
        assertEquals(checksum1, deserialized.chunks[0].sha256Checksum)
    }

    @Test
    fun testArtifactStagingEngineAndPathSafety() {
        val stagingEngine = ArtifactStagingEngine(testDir)
        val safeFile = File(testDir, "app/package.bin")
        assertTrue(stagingEngine.isPathSafe(safeFile))

        val unsafeFile = File("/etc/passwd")
        assertFalse(stagingEngine.isPathSafe(unsafeFile))
    }

    @Test
    fun testSaveStateSyncEngineDeltaDetection() {
        val syncEngine = SaveStateSyncEngine(testDir)
        val saveFolder = File(testDir, "user_save")
        saveFolder.mkdirs()

        val file1 = File(saveFolder, "savegame1.dat")
        file1.writeText("Level 1 Score: 100")

        val entries = syncEngine.scanSaveDirectory(saveFolder)
        assertEquals(1, entries.size)

        val snapshot1 = syncEngine.createSnapshot("app_test", 1L, saveFolder)
        assertFalse(syncEngine.detectDeltas(entries, snapshot1))

        // Modify file
        file1.writeText("Level 2 Score: 500")
        val updatedEntries = syncEngine.scanSaveDirectory(saveFolder)
        assertTrue(syncEngine.detectDeltas(updatedEntries, snapshot1))
    }

    @Test
    fun testArtifactCacheManagerLruEviction() {
        val cacheManager = ArtifactCacheManager(testDir, maxCacheSizeBytes = 1000)

        val dir1 = File(testDir, "app1").apply { mkdirs() }
        val dir2 = File(testDir, "app2").apply { mkdirs() }

        cacheManager.registerCacheItem("app1", dir1, sizeBytes = 600, isPinned = false)
        cacheManager.registerCacheItem("app2", dir2, sizeBytes = 600, isPinned = false)

        val evicted = cacheManager.evictLruCacheIfNeeded()
        assertTrue(evicted.contains("app1"))
        assertFalse(dir1.exists())
    }
}
