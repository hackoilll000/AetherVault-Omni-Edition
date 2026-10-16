package com.example.core.telegram

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TelegramUploadResult {
    data class Success(val messageId: Int, val fileId: String) : TelegramUploadResult()
    data class RateLimited(val retryAfterSeconds: Int) : TelegramUploadResult()
    data class Error(val message: String, val statusCode: Int = 0) : TelegramUploadResult()
}

object TelegramMultipartUploader {
    private const val TAG = "TelegramUploader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val octetMediaType = "application/octet-stream".toMediaTypeOrNull()

    /**
     * Uploads an unencrypted original file directly to Telegram with original filename and MIME type.
     */
    fun uploadRawFile(
        botToken: String,
        channelId: Long,
        file: File,
        originalName: String,
        mimeType: String,
        caption: String = ""
    ): TelegramUploadResult {
        val url = "https://api.telegram.org/bot$botToken/sendDocument"

        val formattedChatId = if (channelId > 0) {
            val s = channelId.toString()
            if (s.startsWith("100")) "-$s" else "-100$s"
        } else {
            channelId.toString()
        }

        val mediaType = mimeType.toMediaTypeOrNull() ?: octetMediaType

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", formattedChatId)
            .addFormDataPart("disable_notification", "true")
            .apply {
                if (caption.isNotBlank()) {
                    addFormDataPart("caption", caption)
                }
            }
            .addFormDataPart(
                "document",
                originalName,
                file.asRequestBody(mediaType)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Response code: ${response.code} for $originalName")

                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        val json = runCatching { JSONObject(bodyStr) }.getOrNull()
                        val retryAfter = json?.optJSONObject("parameters")?.optInt("retry_after", 5) ?: 5
                        return TelegramUploadResult.RateLimited(retryAfter)
                    }
                    val errorDesc = runCatching { JSONObject(bodyStr).optString("description") }.getOrNull() ?: bodyStr
                    return TelegramUploadResult.Error("HTTP ${response.code}: $errorDesc", response.code)
                }

                val json = JSONObject(bodyStr)
                if (json.optBoolean("ok", false)) {
                    val result = json.getJSONObject("result")
                    val messageId = result.getInt("message_id")
                    val doc = result.optJSONObject("document")
                    val video = result.optJSONObject("video")
                    val audio = result.optJSONObject("audio")
                    val photoArr = result.optJSONArray("photo")
                    val fileId = doc?.optString("file_id")
                        ?: video?.optString("file_id")
                        ?: audio?.optString("file_id")
                        ?: photoArr?.optJSONObject(photoArr.length() - 1)?.optString("file_id")
                        ?: ""
                    TelegramUploadResult.Success(messageId, fileId)
                } else {
                    val desc = json.optString("description", "Unknown Telegram Error")
                    TelegramUploadResult.Error("API Error: $desc")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for $originalName", e)
            TelegramUploadResult.Error("Network error: ${e.message ?: "Failed to upload file"}")
        }
    }

    /**
     * Uploads a chunk file to the Telegram Channel using multipart/form-data
     */
    fun uploadChunkFile(
        botToken: String,
        channelId: Long,
        chunkFile: File,
        chunkIndex: Int,
        totalChunks: Int
    ): TelegramUploadResult {
        return uploadRawFile(
            botToken = botToken,
            channelId = channelId,
            file = chunkFile,
            originalName = chunkFile.name,
            mimeType = "application/octet-stream",
            caption = "Part ${chunkIndex + 1}/$totalChunks"
        )
    }

    /**
     * Uploads bytes directly without writing to intermediate file
     */
    fun uploadChunkBytes(
        botToken: String,
        channelId: Long,
        bytes: ByteArray,
        chunkIndex: Int,
        totalChunks: Int
    ): TelegramUploadResult {
        val url = "https://api.telegram.org/bot$botToken/sendDocument"

        val formattedChatId = if (channelId > 0) {
            val s = channelId.toString()
            if (s.startsWith("100")) "-$s" else "-100$s"
        } else {
            channelId.toString()
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", formattedChatId)
            .addFormDataPart("disable_notification", "true")
            .addFormDataPart("caption", "Part #${chunkIndex + 1}/$totalChunks")
            .addFormDataPart(
                "document",
                "chunk_${chunkIndex}.bin",
                bytes.toRequestBody(octetMediaType)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        val json = runCatching { JSONObject(bodyStr) }.getOrNull()
                        val retryAfter = json?.optJSONObject("parameters")?.optInt("retry_after", 5) ?: 5
                        return TelegramUploadResult.RateLimited(retryAfter)
                    }
                    val errorDesc = runCatching { JSONObject(bodyStr).optString("description") }.getOrNull() ?: bodyStr
                    return TelegramUploadResult.Error("HTTP ${response.code}: $errorDesc", response.code)
                }

                val json = JSONObject(bodyStr)
                if (json.optBoolean("ok", false)) {
                    val result = json.getJSONObject("result")
                    val messageId = result.getInt("message_id")
                    val doc = result.getJSONObject("document")
                    val fileId = doc.getString("file_id")
                    TelegramUploadResult.Success(messageId, fileId)
                } else {
                    TelegramUploadResult.Error("API Error: ${json.optString("description")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Byte upload failed for chunk $chunkIndex", e)
            TelegramUploadResult.Error("Network error: ${e.message}")
        }
    }
}
