package com.example

import com.example.features.linkmanager.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class UniversalLinkManagerTest {

    @Test
    fun testSsrfProtectionRejectsLocalAndPrivateIps() {
        // Localhost
        val (localOk, localMsg) = UniversalLinkRegistry.validateLinkSecurity("http://localhost:8080/secret.txt")
        assertFalse(localOk)
        assertTrue(localMsg.contains("SSRF"))

        // Loopback 127.0.0.1
        val (loopOk, _) = UniversalLinkRegistry.validateLinkSecurity("http://127.0.0.1/admin")
        assertFalse(loopOk)

        // Private 10.x.x.x
        val (priv10Ok, _) = UniversalLinkRegistry.validateLinkSecurity("https://10.0.0.1/download.mp4")
        assertFalse(priv10Ok)

        // Private 192.168.x.x
        val (priv192Ok, _) = UniversalLinkRegistry.validateLinkSecurity("https://192.168.1.100/video.mkv")
        assertFalse(priv192Ok)

        // Unsupported scheme (file://, ftp://)
        val (fileOk, fileMsg) = UniversalLinkRegistry.validateLinkSecurity("file:///sdcard/Download/test.mp4")
        assertFalse(fileOk)
        assertTrue(fileMsg.contains("HTTP and HTTPS"))
    }

    @Test
    fun testValidPublicUrlsPassValidation() {
        val validUrl = "https://example.com/files/sample.mp4"
        val (isValid, msg) = UniversalLinkRegistry.validateLinkSecurity(validUrl)
        assertTrue(isValid)
        assertEquals("URL is valid", msg)
    }

    @Test
    fun testPlatformDetection() {
        assertEquals(
            SupportedPlatform.YOUTUBE,
            UniversalLinkRegistry.detectPlatform(URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        )
        assertEquals(
            SupportedPlatform.YOUTUBE,
            UniversalLinkRegistry.detectPlatform(URI("https://youtu.be/dQw4w9WgXcQ"))
        )
        assertEquals(
            SupportedPlatform.GOOGLE_DRIVE,
            UniversalLinkRegistry.detectPlatform(URI("https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/view"))
        )
        assertEquals(
            SupportedPlatform.INSTAGRAM,
            UniversalLinkRegistry.detectPlatform(URI("https://www.instagram.com/p/C-xyz123abc/"))
        )
        assertEquals(
            SupportedPlatform.FACEBOOK,
            UniversalLinkRegistry.detectPlatform(URI("https://www.facebook.com/reel/1234567890"))
        )
        assertEquals(
            SupportedPlatform.GOOGLE_PLAY,
            UniversalLinkRegistry.detectPlatform(URI("https://play.google.com/store/apps/details?id=com.google.android.apps.photos"))
        )
        assertEquals(
            SupportedPlatform.DIRECT_HTTP,
            UniversalLinkRegistry.detectPlatform(URI("https://example.com/archive.zip"))
        )
    }

    @Test
    fun testDirectFileAdapterCapabilities() = runBlocking {
        val adapter = DirectFileAdapter()
        val uri = URI("https://example.com/media/sample_video.mp4")
        assertTrue(adapter.canHandle(uri))

        val client = okhttp3.OkHttpClient()
        val result = adapter.resolve("https://example.com/media/sample_video.mp4", uri, client)
        assertTrue(result.isSuccess)
        val resource = result.getOrThrow()

        assertEquals(SupportedPlatform.DIRECT_HTTP, resource.platform)
        assertEquals(LinkContentType.DIRECT_VIDEO, resource.contentType)
        assertTrue(resource.capabilities.canStream)
        assertTrue(resource.capabilities.canDownloadLocally)
        assertTrue(resource.capabilities.canUploadToTelegram)
        assertEquals("sample_video.mp4", resource.suggestedFilename)
        assertEquals("video/mp4", resource.mimeType)
    }

    @Test
    fun testYouTubeAdapterCapabilitiesDiscloseToS() = runBlocking {
        val adapter = YouTubeAdapter()
        val uri = URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(adapter.canHandle(uri))

        val client = okhttp3.OkHttpClient()
        val result = adapter.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ", uri, client)
        assertTrue(result.isSuccess)
        val resource = result.getOrThrow()

        assertEquals(SupportedPlatform.YOUTUBE, resource.platform)
        assertEquals(LinkContentType.DIRECT_VIDEO, resource.contentType)
        // Check DRM and Policy boundaries: direct scraping is disallowed
        assertFalse(resource.capabilities.canDownloadLocally)
        assertFalse(resource.capabilities.canUploadToTelegram)
        assertTrue(resource.capabilities.isRestrictedOrDrm)
        assertNotNull(resource.capabilities.restrictionNotice)
        assertTrue(resource.capabilities.restrictionNotice!!.contains("Terms of Service"))
    }

    @Test
    fun testLinkTransferTaskLifecycle() {
        val dummyResource = ResolvedLinkResource(
            rawUrl = "https://example.com/data.bin",
            normalizedUrl = "https://example.com/data.bin",
            platform = SupportedPlatform.DIRECT_HTTP,
            contentType = LinkContentType.DOCUMENT,
            title = "data.bin",
            suggestedFilename = "data.bin",
            capabilities = LinkCapabilities(
                canDownloadLocally = true,
                canUploadToTelegram = true
            )
        )

        val task = LinkTransferTask(
            taskId = "test_task_123",
            resource = dummyResource,
            destination = TransferDestination.LOCAL_STORAGE,
            destinationDetails = "Downloads Folder",
            stage = TransferStage.QUEUED
        )

        assertEquals(TransferStage.QUEUED, task.stage)
        assertEquals("Queued", task.stage.userLabel)

        val activeTask = task.copy(
            stage = TransferStage.DOWNLOADING,
            downloadProgressPercent = 55.4f,
            transferredBytes = 55400L,
            totalBytes = 100000L
        )
        assertEquals(TransferStage.DOWNLOADING, activeTask.stage)
        assertEquals(55.4f, activeTask.downloadProgressPercent)

        val completedTask = activeTask.copy(stage = TransferStage.COMPLETED)
        assertEquals(TransferStage.COMPLETED, completedTask.stage)
    }
}
