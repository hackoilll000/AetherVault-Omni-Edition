package com.example.core.artifact

import java.io.File

data class LaunchSessionState(
    val artifactId: String,
    val executableFile: File,
    val processId: Long,
    val isRunning: Boolean,
    val exitCode: Int? = null,
    val startTimeTimestamp: Long = System.currentTimeMillis()
)

class DesktopProcessLauncher(private val cacheManager: ArtifactCacheManager) {

    private val activeSessions = mutableMapOf<String, LaunchSessionState>()

    fun launchExecutable(artifactId: String, executableFile: File, workingDir: File? = null): LaunchSessionState {
        if (!executableFile.exists()) {
            throw IllegalArgumentException("Executable file does not exist: ${executableFile.absolutePath}")
        }

        cacheManager.acquireActiveLease(artifactId)

        val pb = ProcessBuilder(executableFile.absolutePath)
        pb.directory(workingDir ?: executableFile.parentFile)
        val process = pb.start()

        val pid = try {
            val field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            field.getLong(process)
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback mock process ID on environments without pid field
        }

        val session = LaunchSessionState(
            artifactId = artifactId,
            executableFile = executableFile,
            processId = pid,
            isRunning = true
        )
        activeSessions[artifactId] = session
        return session
    }

    fun checkSessionStatus(artifactId: String): LaunchSessionState? {
        val session = activeSessions[artifactId] ?: return null
        return session
    }

    fun terminateSession(artifactId: String) {
        val session = activeSessions[artifactId]
        if (session != null) {
            cacheManager.releaseActiveLease(artifactId)
            activeSessions[artifactId] = session.copy(isRunning = false, exitCode = 0)
        }
    }
}
