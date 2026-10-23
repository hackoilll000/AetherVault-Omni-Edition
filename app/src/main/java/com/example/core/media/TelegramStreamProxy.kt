package com.example.core.media

import android.content.Context
import android.util.Log
import com.example.core.database.VaultFile
import com.example.core.sync.CloudUploadPipeline
import com.example.core.telegram.TelegramBotClient
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Local Streaming HTTP Proxy for Media3 / ExoPlayer
 * Binds to 127.0.0.1 and translates ExoPlayer HTTP Byte-Range requests (RFC 7233)
 * into partial content streams (206 Partial Content).
 *
 * Supports:
 * - Local cached files with zero RAM latency
 * - Direct Telegram Bot API stream fetches with Range translation
 * - Smooth random-access seeking without waiting for full 2GB downloads
 */
object TelegramStreamProxy {
    private const val TAG = "TelegramStreamProxy"
    private var serverSocket: ServerSocket? = null
    private var proxyPort: Int = 0
    private var proxyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Cache of active file metadata for rapid stream lookup
    private val activeStreams = ConcurrentHashMap<String, StreamMetadata>()

    data class StreamMetadata(
        val fileId: String,
        val fileName: String,
        val mimeType: String,
        val totalSize: Long,
        val localFile: File?,
        val botToken: String,
        val telegramFileId: String
    )

    @Synchronized
    fun startProxy(context: Context): Int {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            return proxyPort
        }

        try {
            val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            proxyPort = socket.localPort

            proxyScope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        launch {
                            handleClientConnection(context, clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.w(TAG, "Socket accept error: ${e.message}")
                        }
                    }
                }
            }
            Log.i(TAG, "TelegramStreamProxy started on port $proxyPort")
            return proxyPort
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TelegramStreamProxy", e)
            return 0
        }
    }

    fun registerStream(
        context: Context,
        file: VaultFile,
        botToken: String
    ): String {
        val port = startProxy(context)
        val localCached = CloudUploadPipeline.getLocalCachedFile(context, file.id, file.name)
        val hasLocal = localCached.exists() && localCached.length() > 0

        val telegramFileId = ""

        val meta = StreamMetadata(
            fileId = file.id,
            fileName = file.name,
            mimeType = file.mimeType.ifBlank { "video/mp4" },
            totalSize = if (hasLocal) localCached.length() else file.totalSizeBytes,
            localFile = if (hasLocal) localCached else null,
            botToken = botToken,
            telegramFileId = telegramFileId
        )
        activeStreams[file.id] = meta

        return "http://127.0.0.1:$port/stream/${file.id}"
    }

    private suspend fun handleClientConnection(context: Context, socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val outputStream = BufferedOutputStream(s.getOutputStream())

                val requestLine = reader.readLine() ?: return@use
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@use

                val method = tokens[0].uppercase()
                val path = tokens[1]

                // Parse Range Header
                var rangeStart = 0L
                var rangeEnd = -1L
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        val rangeSpec = line.substringAfter("bytes=").trim()
                        val parts = rangeSpec.split("-")
                        rangeStart = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                        if (parts.size > 1 && parts[1].isNotBlank()) {
                            rangeEnd = parts[1].toLongOrNull() ?: -1L
                        }
                    }
                    line = reader.readLine()
                }

                val fileId = path.substringAfterLast("/").substringBefore("?")
                val meta = activeStreams[fileId]

                if (meta == null) {
                    send404(outputStream)
                    return@use
                }

                // If local cached file is present, serve directly using RandomAccessFile
                if (meta.localFile != null && meta.localFile.exists()) {
                    serveLocalFile(meta.localFile, meta.mimeType, method, rangeStart, rangeEnd, outputStream)
                } else if (meta.botToken.isNotBlank() && meta.telegramFileId.isNotBlank()) {
                    serveTelegramRemoteFile(meta, method, rangeStart, rangeEnd, outputStream)
                } else {
                    send404(outputStream)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Proxy client connection closed: ${e.message}")
        }
    }

    private fun serveLocalFile(
        file: File,
        mimeType: String,
        method: String,
        requestedStart: Long,
        requestedEnd: Long,
        out: OutputStream
    ) {
        val fileLength = file.length()
        val start = requestedStart.coerceIn(0L, fileLength - 1)
        val end = if (requestedEnd in start until fileLength) requestedEnd else fileLength - 1
        val contentLength = end - start + 1

        val writer = PrintWriter(out)
        val isPartial = requestedStart > 0 || requestedEnd >= 0

        if (isPartial) {
            writer.print("HTTP/1.1 206 Partial Content\r\n")
            writer.print("Content-Range: bytes $start-$end/$fileLength\r\n")
        } else {
            writer.print("HTTP/1.1 200 OK\r\n")
        }
        writer.print("Content-Type: $mimeType\r\n")
        writer.print("Content-Length: $contentLength\r\n")
        writer.print("Accept-Ranges: bytes\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()

        if (method != "HEAD") {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(64 * 1024)
                var bytesRemaining = contentLength
                while (bytesRemaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    bytesRemaining -= read
                }
                out.flush()
            }
        }
    }

    private fun serveTelegramRemoteFile(
        meta: StreamMetadata,
        method: String,
        requestedStart: Long,
        requestedEnd: Long,
        out: OutputStream
    ) {
        // Resolve Telegram file path
        val botClient = TelegramBotClient()
        val getFileUrl = "https://api.telegram.org/bot${meta.botToken}/getFile?file_id=${meta.telegramFileId}"
        val req = Request.Builder().url(getFileUrl).get().build()

        val filePath = try {
            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                val json = org.json.JSONObject(bodyStr)
                if (json.optBoolean("ok")) {
                    json.getJSONObject("result").getString("file_path")
                } else null
            }
        } catch (e: Exception) {
            null
        }

        if (filePath == null) {
            send404(out)
            return
        }

        val directStreamUrl = "https://api.telegram.org/file/bot${meta.botToken}/$filePath"
        val streamReqBuilder = Request.Builder().url(directStreamUrl)

        val isPartial = requestedStart > 0 || requestedEnd >= 0
        if (isPartial) {
            val rangeHeader = if (requestedEnd > 0) "bytes=$requestedStart-$requestedEnd" else "bytes=$requestedStart-"
            streamReqBuilder.header("Range", rangeHeader)
        }

        try {
            httpClient.newCall(streamReqBuilder.build()).execute().use { streamResp ->
                val streamBody = streamResp.body
                if (!streamResp.isSuccessful || streamBody == null) {
                    send404(out)
                    return
                }

                val remoteContentLength = streamBody.contentLength()
                val totalLength = if (meta.totalSize > 0) meta.totalSize else remoteContentLength

                val writer = PrintWriter(out)
                if (isPartial) {
                    writer.print("HTTP/1.1 206 Partial Content\r\n")
                    val actualEnd = if (requestedEnd > 0) requestedEnd else (requestedStart + remoteContentLength - 1)
                    writer.print("Content-Range: bytes $requestedStart-$actualEnd/$totalLength\r\n")
                } else {
                    writer.print("HTTP/1.1 200 OK\r\n")
                }
                writer.print("Content-Type: ${meta.mimeType}\r\n")
                if (remoteContentLength > 0) {
                    writer.print("Content-Length: $remoteContentLength\r\n")
                }
                writer.print("Accept-Ranges: bytes\r\n")
                writer.print("Connection: close\r\n\r\n")
                writer.flush()

                if (method != "HEAD") {
                    streamBody.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                        }
                        out.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error piping telegram stream: ${e.message}")
        }
    }

    private fun send404(out: OutputStream) {
        val writer = PrintWriter(out)
        writer.print("HTTP/1.1 404 Not Found\r\n")
        writer.print("Content-Type: text/plain\r\n")
        writer.print("Content-Length: 9\r\n\r\n")
        writer.print("Not Found")
        writer.flush()
    }

    fun stopProxy() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
