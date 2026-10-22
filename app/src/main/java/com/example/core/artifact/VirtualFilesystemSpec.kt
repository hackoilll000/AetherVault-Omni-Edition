package com.example.core.artifact

data class VirtualFileOffsetRequest(
    val artifactId: String,
    val chunkIndex: Int,
    val offsetBytes: Long,
    val lengthBytes: Int
)

data class VirtualFileReadResult(
    val data: ByteArray,
    val bytesRead: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

interface VirtualFilesystemAdapter {
    fun mountVirtualDrive(driveLetter: String): Boolean
    fun unmountVirtualDrive(): Boolean
    fun readOffset(request: VirtualFileOffsetRequest): VirtualFileReadResult
    fun isMounted(): Boolean
}

class WindowsWinFspVirtualDriveAdapter : VirtualFilesystemAdapter {
    private var mounted = false

    override fun mountVirtualDrive(driveLetter: String): Boolean {
        mounted = true
        return true
    }

    override fun unmountVirtualDrive(): Boolean {
        mounted = false
        return true
    }

    override fun readOffset(request: VirtualFileOffsetRequest): VirtualFileReadResult {
        if (!mounted) {
            return VirtualFileReadResult(ByteArray(0), 0, false, "Virtual drive is not mounted!")
        }
        return VirtualFileReadResult(ByteArray(request.lengthBytes), request.lengthBytes, true)
    }

    override fun isMounted(): Boolean = mounted
}
