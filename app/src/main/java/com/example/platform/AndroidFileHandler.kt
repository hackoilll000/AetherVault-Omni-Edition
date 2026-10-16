package com.example.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class SelectedFileMeta(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extension: String
)

object AndroidFileHandler {
    private const val TAG = "AndroidFileHandler"

    fun getFileMetadata(context: Context, uri: Uri): SelectedFileMeta {
        var name = ""
        var size = 0L

        // 1. Query display name from ContentResolver
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ContentResolver query failed for $uri: ${e.message}")
        }

        // 2. Fallback name from URI path segments
        if (name.isBlank() || name.startsWith("vault_file_")) {
            val lastSegment = uri.lastPathSegment ?: ""
            val cleanSegment = if (lastSegment.contains('/')) lastSegment.substringAfterLast('/') else lastSegment
            if (cleanSegment.isNotBlank() && cleanSegment != "raw") {
                name = cleanSegment
            }
        }

        // 3. Fallback for file:// scheme
        if (name.isBlank() && uri.scheme == "file") {
            uri.path?.let { p ->
                val f = java.io.File(p)
                if (f.name.isNotBlank()) name = f.name
                if (size == 0L && f.exists()) size = f.length()
            }
        }

        // 4. Initial MIME type query
        var mimeType = context.contentResolver.getType(uri) ?: ""
        var ext = if (name.contains('.')) name.substringAfterLast('.', "").lowercase() else ""

        // 5. Derive MIME type from extension if missing or octet-stream
        if (mimeType.isBlank() || mimeType == "application/octet-stream") {
            if (ext.isNotEmpty()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: ""
            }
        }

        // 6. Magic header probe for exact format detection if still unknown
        if (mimeType.isBlank() || mimeType == "application/octet-stream" || ext.isEmpty()) {
            val detected = probeFormatFromStream(context, uri)
            if (detected != null) {
                if (mimeType.isBlank() || mimeType == "application/octet-stream") {
                    mimeType = detected.first
                }
                if (ext.isEmpty()) {
                    ext = detected.second
                }
            }
        }

        // 7. Default extension and mime if completely unresolved
        if (ext.isEmpty()) {
            ext = when {
                mimeType.startsWith("image/") -> mimeType.substringAfter("image/", "jpg")
                mimeType.startsWith("video/") -> mimeType.substringAfter("video/", "mp4")
                mimeType.startsWith("audio/") -> mimeType.substringAfter("audio/", "mp3")
                mimeType.startsWith("text/") -> "txt"
                mimeType == "application/pdf" -> "pdf"
                else -> "bin"
            }
        }

        if (name.isBlank() || name == "vault_file" || name.startsWith("vault_file_")) {
            name = "file_${System.currentTimeMillis()}.$ext"
        } else if (!name.contains('.')) {
            name = "$name.$ext"
        }

        if (mimeType.isBlank()) {
            mimeType = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "mp4", "m4v" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "ogg" -> "audio/ogg"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "json" -> "application/json"
                "md" -> "text/markdown"
                "csv" -> "text/csv"
                "zip" -> "application/zip"
                "apk" -> "application/vnd.android.package-archive"
                else -> "application/octet-stream"
            }
        }

        // Ensure file size is positive
        if (size <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong().coerceAtLeast(1L)
                }
            } catch (_: Exception) {}
        }

        return SelectedFileMeta(
            uri = uri,
            name = name,
            sizeBytes = size,
            mimeType = mimeType,
            extension = ext
        )
    }

    private fun probeFormatFromStream(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(32)
                val read = stream.read(header)
                if (read < 4) return null

                // JPEG: FF D8 FF
                if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
                    return Pair("image/jpeg", "jpg")
                }
                // PNG: 89 50 4E 47
                if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
                    return Pair("image/png", "png")
                }
                // PDF: %PDF
                if (header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() && header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()) {
                    return Pair("application/pdf", "pdf")
                }
                // GIF: GIF8
                if (header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()) {
                    return Pair("image/gif", "gif")
                }
                // ZIP / APK: PK 03 04
                if (header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                    return Pair("application/zip", "zip")
                }
                // MP4: ftyp at offset 4
                if (read >= 8 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() && header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()) {
                    return Pair("video/mp4", "mp4")
                }
                // MP3: ID3 or FF FB
                if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                    return Pair("audio/mpeg", "mp3")
                }
                // Plaintext check (all bytes are printable ASCII or UTF-8 whitespace)
                var isText = true
                for (i in 0 until read) {
                    val b = header[i].toInt() and 0xFF
                    if (b < 0x09 || (b in 0x0E..0x1F && b != 0x1B)) {
                        isText = false
                        break
                    }
                }
                if (isText) {
                    return Pair("text/plain", "txt")
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening input stream for $uri", e)
            null
        }
    }

    /**
     * Generates a 32x32 WebP micro-thumbnail stored directly in the database
     * for instant scrolling and preview without network roundtrips.
     */
    fun generateMicroThumbnail(context: Context, uri: Uri, mimeType: String): ByteArray? {
        if (!mimeType.startsWith("image/")) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                val original = BitmapFactory.decodeStream(stream, null, options) ?: return null
                val scaled = Bitmap.createScaledBitmap(original, 64, 64, true)
                val out = ByteArrayOutputStream()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, out)
                } else {
                    @Suppress("DEPRECATION")
                    scaled.compress(Bitmap.CompressFormat.WEBP, 75, out)
                }
                scaled.recycle()
                original.recycle()
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not generate micro-thumbnail: ${e.message}")
            null
        }
    }
}
