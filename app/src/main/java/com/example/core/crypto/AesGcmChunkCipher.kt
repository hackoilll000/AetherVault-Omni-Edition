package com.example.core.crypto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * AetherVault Zero-Knowledge Cryptography Engine
 * Implements deterministic 19MB chunking, binary frame header ("AVLT"),
 * AES-256-GCM envelope encryption, and Argon2id/PBKDF2 key derivation.
 */
object AesGcmChunkCipher {
    // 19 MB plaintext chunk size (19,922,944 bytes)
    const val CHUNK_PLAINTEXT_SIZE = 19_922_944
    const val GCM_TAG_LENGTH_BITS = 128
    const val GCM_IV_LENGTH_BYTES = 12
    const val KEY_SIZE_BYTES = 32 // 256-bit AES
    const val PBKDF2_ITERATIONS = 120_000

    private val MAGIC_HEADER = byteArrayOf(0x41, 0x56, 0x4C, 0x54) // "AVLT"
    private const val PROTOCOL_VERSION: Byte = 0x01

    private val secureRandom = SecureRandom()

    /**
     * Derives a 256-bit Master Key from a user password and salt using PBKDF2-HMAC-SHA256
     */
    fun deriveMasterKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BYTES * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Generates a cryptographically random 256-bit key
     */
    fun generateRandomKey(): ByteArray {
        val key = ByteArray(KEY_SIZE_BYTES)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Generates a 16-byte random cryptographic salt
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Generates a 12-byte random IV/Nonce for GCM
     */
    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        return iv
    }

    /**
     * Encrypts a plaintext payload using AES-256-GCM.
     * Binary frame output format:
     * - Bytes 0..3: "AVLT" (Magic)
     * - Byte 4: Protocol version (0x01)
     * - Bytes 5..8: Chunk index (UInt32 Big-Endian)
     * - Bytes 9..20: IV / Nonce (12 bytes)
     * - Remaining: Ciphertext + 16-byte GCM Auth Tag
     */
    fun encryptChunk(
        plaintext: ByteArray,
        fileKey: ByteArray,
        chunkIndex: Int,
        fileId: String
    ): ByteArray {
        val iv = generateIv()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(fileKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        // Bind Additional Authenticated Data (AAD) to prevent chunk-swapping attacks
        val aad = "$fileId:$chunkIndex".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        val ciphertextWithTag = cipher.doFinal(plaintext)

        val output = ByteArrayOutputStream(4 + 1 + 4 + GCM_IV_LENGTH_BYTES + ciphertextWithTag.size)
        // 1. Magic
        output.write(MAGIC_HEADER)
        // 2. Version
        output.write(PROTOCOL_VERSION.toInt())
        // 3. Chunk Index (Big-Endian UInt32)
        val indexBuf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(chunkIndex).array()
        output.write(indexBuf)
        // 4. IV
        output.write(iv)
        // 5. Ciphertext with GCM Tag
        output.write(ciphertextWithTag)

        return output.toByteArray()
    }

    /**
     * Decrypts an encrypted chunk formatted with the "AVLT" binary frame header.
     */
    fun decryptChunk(
        frameData: ByteArray,
        fileKey: ByteArray,
        expectedChunkIndex: Int,
        fileId: String
    ): ByteArray {
        require(frameData.size >= 4 + 1 + 4 + GCM_IV_LENGTH_BYTES + 16) {
            "Frame size too small for AVLT chunk: ${frameData.size}"
        }

        // Validate Magic Header
        for (i in 0..3) {
            require(frameData[i] == MAGIC_HEADER[i]) { "Invalid AVLT magic header" }
        }

        val version = frameData[4]
        require(version == PROTOCOL_VERSION) { "Unsupported protocol version: $version" }

        val indexBuf = ByteBuffer.wrap(frameData, 5, 4).order(ByteOrder.BIG_ENDIAN)
        val chunkIndex = indexBuf.int
        require(chunkIndex == expectedChunkIndex) {
            "Chunk index mismatch: expected $expectedChunkIndex, got $chunkIndex"
        }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(frameData, 9, iv, 0, GCM_IV_LENGTH_BYTES)

        val cipherOffset = 9 + GCM_IV_LENGTH_BYTES
        val cipherLength = frameData.size - cipherOffset

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(fileKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val aad = "$fileId:$chunkIndex".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        return cipher.doFinal(frameData, cipherOffset, cipherLength)
    }

    /**
     * Encrypts the Per-File Key using the Master Key (Envelope Encryption)
     */
    fun encryptFileKey(fileKey: ByteArray, masterKey: ByteArray): ByteArray {
        val iv = generateIv()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(fileKey)
        return ByteBuffer.allocate(iv.size + encrypted.size)
            .put(iv)
            .put(encrypted)
            .array()
    }

    /**
     * Decrypts the Per-File Key using the Master Key
     */
    fun decryptFileKey(encryptedPayload: ByteArray, masterKey: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        val cipherLength = encryptedPayload.size - GCM_IV_LENGTH_BYTES
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encryptedPayload, GCM_IV_LENGTH_BYTES, cipherLength)
    }

    /**
     * Encrypts generic string or bytes with a key using AES-GCM
     */
    fun encryptSimple(data: ByteArray, key: ByteArray): String {
        val iv = generateIv()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherText = cipher.doFinal(data)
        val combined = ByteBuffer.allocate(iv.size + cipherText.size)
            .put(iv)
            .put(cipherText)
            .array()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptSimple(base64Data: String, key: ByteArray): ByteArray {
        val raw = Base64.decode(base64Data, Base64.NO_WRAP)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(raw, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(raw, GCM_IV_LENGTH_BYTES, raw.size - GCM_IV_LENGTH_BYTES)
    }

    /**
     * Generates a self-contained Zero-Knowledge Share Ticket string (URI scheme `aethervault://share?ticket=...`)
     */
    fun createShareTicket(
        fileId: String,
        fileName: String,
        fileSize: Long,
        fileKey: ByteArray,
        messageIds: List<Int>,
        expiresAt: Long = 0L
    ): String {
        val keyB64 = Base64.encodeToString(fileKey, Base64.NO_WRAP)
        val msgsStr = messageIds.joinToString(",")
        val json = """{"fid":"$fileId","n":"$fileName","s":$fileSize,"k":"$keyB64","m":"$msgsStr","exp":$expiresAt}"""
        val ticketB64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return "aethervault://share?ticket=$ticketB64"
    }

    data class DecodedShareTicket(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val fileKey: ByteArray,
        val messageIds: List<Int>,
        val expiresAt: Long
    )

    fun parseShareTicket(ticketUri: String): DecodedShareTicket? {
        return runCatching {
            val ticketParam = if (ticketUri.contains("ticket=")) {
                ticketUri.substringAfter("ticket=").substringBefore("&")
            } else {
                ticketUri
            }
            val jsonBytes = Base64.decode(ticketParam, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp > 0 && System.currentTimeMillis() > exp) {
                return null
            }
            val keyB64 = json.getString("k")
            val keyBytes = Base64.decode(keyB64, Base64.NO_WRAP)
            val msgsStr = json.optString("m", "")
            val msgs = if (msgsStr.isNotBlank()) msgsStr.split(",").mapNotNull { it.toIntOrNull() } else emptyList()
            DecodedShareTicket(
                fileId = json.getString("fid"),
                fileName = json.getString("n"),
                fileSize = json.getLong("s"),
                fileKey = keyBytes,
                messageIds = msgs,
                expiresAt = exp
            )
        }.getOrNull()
    }
}
