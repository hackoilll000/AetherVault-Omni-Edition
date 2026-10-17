package com.example.core.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Robust Telegram Bot API Client for AetherVault
 * Handles bot authentication, channel auto-discovery, chunk streaming,
 * and the self-healing Cross-Bot MessageIdResolver.
 */
class TelegramBotClient(
    private val customBaseUrl: String = "https://api.telegram.org"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val octetMediaType = "application/octet-stream".toMediaTypeOrNull()

    companion object {
        /**
         * Converts user raw input (e.g. "1001234567890", "1234567890", "https://t.me/c/1234567890/123", "-1001234567890")
         * into valid negative Telegram Channel ID format (-1001234567890L)
         */
        fun normalizeChannelId(rawInput: String): Long {
            val clean = rawInput.trim()
            if (clean.isBlank()) return 0L

            // Match post link e.g. https://t.me/c/1234567890/55
            val linkMatch = Regex("/c/([0-9]+)").find(clean)
            if (linkMatch != null) {
                val numStr = linkMatch.groupValues[1]
                return "-100$numStr".toLongOrNull() ?: 0L
            }

            // Remove unwanted characters except numbers and minus sign
            val digits = clean.filter { it.isDigit() || it == '-' }
            if (digits.isBlank() || digits == "-") return 0L

            return when {
                digits.startsWith("-100") -> digits.toLongOrNull() ?: 0L
                digits.startsWith("100") && digits.length >= 13 -> "-$digits".toLongOrNull() ?: 0L
                digits.startsWith("-") -> {
                    val num = digits.removePrefix("-")
                    if (num.startsWith("100")) "-$num".toLongOrNull() ?: 0L
                    else "-100$num".toLongOrNull() ?: 0L
                }
                else -> "-100$digits".toLongOrNull() ?: 0L
            }
        }
    }

    data class BotInfo(
        val botId: Long,
        val username: String,
        val firstName: String
    )

    data class ChannelInfo(
        val chatId: Long,
        val title: String,
        val pinnedMessageId: Int?,
        val pinnedMessageText: String?
    )

    data class UploadResult(
        val messageId: Int,
        val fileId: String
    )

    /**
     * Verifies bot credentials via getMe
     */
    suspend fun getMe(token: String): Result<BotInfo> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/getMe"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val info = BotInfo(
                        botId = result.getLong("id"),
                        username = result.optString("username", "UnknownBot"),
                        firstName = result.optString("first_name", "Bot")
                    )
                    Result.success(info)
                } else {
                    val description = json.optString("description", "HTTP ${response.code}")
                    Result.failure(IOException("getMe failed: $description"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stateless channel check & Pinned Manifest reading via getChat
     */
    suspend fun getChat(token: String, chatId: Long): Result<ChannelInfo> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/getChat?chat_id=$chatId"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val pinnedMsg = result.optJSONObject("pinned_message")
                    val info = ChannelInfo(
                        chatId = result.getLong("id"),
                        title = result.optString("title", "Private Vault Channel"),
                        pinnedMessageId = pinnedMsg?.optInt("message_id"),
                        pinnedMessageText = pinnedMsg?.optString("text")
                    )
                    Result.success(info)
                } else {
                    val description = json.optString("description", "HTTP ${response.code}")
                    Result.failure(IOException("getChat failed: $description"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Used ONLY during initial #connect wizard (max 10 seconds)
     */
    suspend fun pollConnectUpdate(token: String): Result<Pair<Long, String>?> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/getUpdates?limit=20&timeout=2"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val updates = json.getJSONArray("result")
                    for (i in 0 until updates.length()) {
                        val update = updates.getJSONObject(i)
                        val post = update.optJSONObject("channel_post") ?: update.optJSONObject("message")
                        if (post != null) {
                            val text = post.optString("text", "")
                            if (text.trim().equals("#connect", ignoreCase = true)) {
                                val chat = post.getJSONObject("chat")
                                val chatId = chat.getLong("id")
                                val title = chat.optString("title", "AetherVault Private Channel")
                                val msgId = post.getInt("message_id")
                                // Attempt to delete test message so channel stays pristine
                                deleteMessage(token, chatId, msgId)
                                return@withContext Result.success(Pair(chatId, title))
                            }
                        }
                    }
                    Result.success(null)
                } else {
                    Result.failure(IOException("getUpdates failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads an encrypted chunk or document to the channel
     */
    suspend fun uploadDocument(
        token: String,
        chatId: Long,
        filename: String,
        bytes: ByteArray,
        caption: String = ""
    ): Result<UploadResult> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/sendDocument"
        val fileBody = bytes.toRequestBody(octetMediaType)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId.toString())
            .addFormDataPart("disable_notification", "true")
            .addFormDataPart("caption", caption)
            .addFormDataPart("document", filename, fileBody)
            .build()

        val request = Request.Builder().url(url).post(multipartBody).build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val messageId = result.getInt("message_id")
                    val doc = result.getJSONObject("document")
                    val fileId = doc.getString("file_id")
                    Result.success(UploadResult(messageId, fileId))
                } else {
                    val desc = json.optString("description", "Upload failed")
                    Result.failure(IOException("Upload error ($desc)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves a file_id for Bot-B using forwardMessage when Bot-A's file_id is missing or incompatible
     */
    suspend fun resolveCrossBotFileId(
        token: String,
        chatId: Long,
        telegramMessageId: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val forwardUrl = "$customBaseUrl/bot$token/forwardMessage"
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("from_chat_id", chatId)
            put("message_id", telegramMessageId)
            put("disable_notification", true)
        }
        val request = Request.Builder()
            .url(forwardUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val tempMsgId = result.getInt("message_id")
                    val doc = result.optJSONObject("document")
                    val fileId = doc?.optString("file_id") ?: ""

                    // Immediately delete the temporary forward so channel remains pristine
                    deleteMessage(token, chatId, tempMsgId)

                    if (fileId.isNotBlank()) {
                        Result.success(fileId)
                    } else {
                        Result.failure(IOException("No document found in forwarded message #$telegramMessageId"))
                    }
                } else {
                    Result.failure(IOException("forwardMessage failed: ${json.optString("description")}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads file bytes using Telegram Bot API
     */
    suspend fun downloadFileBytes(token: String, fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        // Step 1: getFile
        val getFileUrl = "$customBaseUrl/bot$token/getFile?file_id=$fileId"
        val request = Request.Builder().url(getFileUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok")) {
                    return@withContext Result.failure(IOException("getFile failed: ${json.optString("description")}"))
                }
                val filePath = json.getJSONObject("result").getString("file_path")

                // Step 2: Download raw stream
                val streamUrl = "$customBaseUrl/file/bot$token/$filePath"
                val streamRequest = Request.Builder().url(streamUrl).get().build()
                client.newCall(streamRequest).execute().use { streamResponse ->
                    if (!streamResponse.isSuccessful) {
                        return@withContext Result.failure(IOException("Stream download failed: HTTP ${streamResponse.code}"))
                    }
                    val data = streamResponse.body?.bytes() ?: ByteArray(0)
                    Result.success(data)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(token: String, chatId: Long, text: String): Result<Int> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/sendMessage"
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
        }
        val request = Request.Builder().url(url).post(json.toString().toRequestBody(jsonMediaType)).build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val res = JSONObject(bodyStr)
                if (response.isSuccessful && res.optBoolean("ok")) {
                    val msgId = res.getJSONObject("result").getInt("message_id")
                    Result.success(msgId)
                } else {
                    Result.failure(IOException(res.optString("description", "Failed to send message")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(token: String, chatId: Long, messageId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/deleteMessage?chat_id=$chatId&message_id=$messageId"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pinChatMessage(token: String, chatId: Long, messageId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/pinChatMessage"
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("disable_notification", true)
        }
        val request = Request.Builder().url(url).post(json.toString().toRequestBody(jsonMediaType)).build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val res = JSONObject(bodyStr)
                Result.success(response.isSuccessful && res.optBoolean("ok"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessageText(token: String, chatId: Long, messageId: Int, newText: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/editMessageText"
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("text", newText)
        }
        val request = Request.Builder().url(url).post(json.toString().toRequestBody(jsonMediaType)).build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val res = JSONObject(bodyStr)
                Result.success(response.isSuccessful && res.optBoolean("ok"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scans pre-existing channel messages and files:
     * Discovers previous documents, videos, photos, audios, and text posts,
     * and saves them to local database so they can be viewed and streamed offline!
     */
    suspend fun scanExistingChannelContent(
        token: String,
        channelId: Long,
        database: com.example.core.database.AppDatabase,
        context: android.content.Context
    ): Result<Int> = withContext(Dispatchers.IO) {
        val url = "$customBaseUrl/bot$token/getUpdates?limit=100&timeout=2"
        val request = Request.Builder().url(url).get().build()
        var discoveredCount = 0

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (response.isSuccessful && json.optBoolean("ok")) {
                    val updates = json.getJSONArray("result")
                    for (i in 0 until updates.length()) {
                        val update = updates.getJSONObject(i)
                        val post = update.optJSONObject("channel_post") ?: update.optJSONObject("message")
                        if (post != null) {
                            val msgId = post.getInt("message_id")
                            val postChat = post.optJSONObject("chat")
                            val postChatId = postChat?.optLong("id", 0L) ?: 0L

                            val fileId = java.util.UUID.randomUUID().toString()

                            // Check Document
                            val doc = post.optJSONObject("document")
                            if (doc != null) {
                                val docName = doc.optString("file_name", "Document_$msgId")
                                val docMime = doc.optString("mime_type", "application/octet-stream")
                                val docSize = doc.optLong("file_size", 0L)
                                val tgFileId = doc.getString("file_id")
                                val ext = docName.substringAfterLast('.', "")

                                val vaultFile = com.example.core.database.VaultFile(
                                    id = fileId,
                                    name = docName,
                                    extension = ext,
                                    mimeType = docMime,
                                    totalSizeBytes = docSize,
                                    isOfflinePinned = true
                                )
                                val chunk = com.example.core.database.FileChunk(
                                    id = "${fileId}_0",
                                    fileId = fileId,
                                    versionId = "v1",
                                    chunkIndex = 0,
                                    telegramMessageId = msgId,
                                    uploaderBotId = 0L,
                                    botFileIdsJson = "{\"primary\":\"$tgFileId\"}",
                                    chunkSizeBytes = docSize.toInt()
                                )
                                database.fileDao().insertFile(vaultFile)
                                database.chunkDao().insertChunk(chunk)
                                discoveredCount++
                                continue
                            }

                            // Check Video
                            val video = post.optJSONObject("video")
                            if (video != null) {
                                val vidName = video.optString("file_name", "Video_$msgId.mp4")
                                val vidMime = video.optString("mime_type", "video/mp4")
                                val vidSize = video.optLong("file_size", 0L)
                                val tgFileId = video.getString("file_id")

                                val vaultFile = com.example.core.database.VaultFile(
                                    id = fileId,
                                    name = vidName,
                                    extension = "mp4",
                                    mimeType = vidMime,
                                    totalSizeBytes = vidSize,
                                    isOfflinePinned = true
                                )
                                val chunk = com.example.core.database.FileChunk(
                                    id = "${fileId}_0",
                                    fileId = fileId,
                                    versionId = "v1",
                                    chunkIndex = 0,
                                    telegramMessageId = msgId,
                                    uploaderBotId = 0L,
                                    botFileIdsJson = "{\"primary\":\"$tgFileId\"}",
                                    chunkSizeBytes = vidSize.toInt()
                                )
                                database.fileDao().insertFile(vaultFile)
                                database.chunkDao().insertChunk(chunk)
                                discoveredCount++
                                continue
                            }

                            // Check Photo
                            val photoArr = post.optJSONArray("photo")
                            if (photoArr != null && photoArr.length() > 0) {
                                val bestPhoto = photoArr.getJSONObject(photoArr.length() - 1)
                                val photoSize = bestPhoto.optLong("file_size", 0L)
                                val tgFileId = bestPhoto.getString("file_id")
                                val photoName = "Photo_$msgId.jpg"

                                val vaultFile = com.example.core.database.VaultFile(
                                    id = fileId,
                                    name = photoName,
                                    extension = "jpg",
                                    mimeType = "image/jpeg",
                                    totalSizeBytes = photoSize,
                                    isOfflinePinned = true
                                )
                                val chunk = com.example.core.database.FileChunk(
                                    id = "${fileId}_0",
                                    fileId = fileId,
                                    versionId = "v1",
                                    chunkIndex = 0,
                                    telegramMessageId = msgId,
                                    uploaderBotId = 0L,
                                    botFileIdsJson = "{\"primary\":\"$tgFileId\"}",
                                    chunkSizeBytes = photoSize.toInt()
                                )
                                database.fileDao().insertFile(vaultFile)
                                database.chunkDao().insertChunk(chunk)
                                discoveredCount++
                                continue
                            }

                            // Check Audio / Voice
                            val audio = post.optJSONObject("audio") ?: post.optJSONObject("voice")
                            if (audio != null) {
                                val audName = audio.optString("file_name", "Audio_$msgId.mp3")
                                val audMime = audio.optString("mime_type", "audio/mpeg")
                                val audSize = audio.optLong("file_size", 0L)
                                val tgFileId = audio.getString("file_id")

                                val vaultFile = com.example.core.database.VaultFile(
                                    id = fileId,
                                    name = audName,
                                    extension = "mp3",
                                    mimeType = audMime,
                                    totalSizeBytes = audSize,
                                    isOfflinePinned = true
                                )
                                val chunk = com.example.core.database.FileChunk(
                                    id = "${fileId}_0",
                                    fileId = fileId,
                                    versionId = "v1",
                                    chunkIndex = 0,
                                    telegramMessageId = msgId,
                                    uploaderBotId = 0L,
                                    botFileIdsJson = "{\"primary\":\"$tgFileId\"}",
                                    chunkSizeBytes = audSize.toInt()
                                )
                                database.fileDao().insertFile(vaultFile)
                                database.chunkDao().insertChunk(chunk)
                                discoveredCount++
                                continue
                            }

                            // Check Text Post
                            val text = post.optString("text", "")
                            if (text.isNotBlank() && !text.startsWith("#connect")) {
                                val textName = "Server_Post_$msgId.txt"
                                val textBytes = text.toByteArray(Charsets.UTF_8)
                                val localCache = com.example.core.sync.CloudUploadPipeline.getLocalCachedFile(context, fileId, textName)
                                localCache.writeText(text)

                                val vaultFile = com.example.core.database.VaultFile(
                                    id = fileId,
                                    name = textName,
                                    extension = "txt",
                                    mimeType = "text/plain",
                                    totalSizeBytes = textBytes.size.toLong(),
                                    isOfflinePinned = true
                                )
                                database.fileDao().insertFile(vaultFile)
                                discoveredCount++
                            }
                        }
                    }
                }
            }
            Result.success(discoveredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
