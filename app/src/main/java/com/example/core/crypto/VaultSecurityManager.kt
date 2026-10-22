package com.example.core.crypto

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class LockPolicy(val displayNameEn: String, val displayNameHi: String) {
    STAY_UNLOCKED(
        displayNameEn = "Stay Unlocked (Persistent Login)",
        displayNameHi = "हमेशा अनलॉक रखें (स्थायी लॉगिन)"
    ),
    LOCK_AFTER_INACTIVITY(
        displayNameEn = "Lock After Inactivity (15 mins)",
        displayNameHi = "15 मिनट निष्क्रियता के बाद लॉक करें"
    ),
    ALWAYS_REQUIRE_AUTH(
        displayNameEn = "Always Require Password",
        displayNameHi = "हमेशा पासवर्ड मांगें"
    )
}

/**
 * VaultSecurityManager:
 * - Enterprise-grade salted PBKDF2-HMAC-SHA256 password hashing (10,000 iterations).
 * - Zero hardcoded credentials or bypasses.
 * - Seamless migration from legacy Java hashCode() values.
 * - Persistent Login (Remember Me) session engine supporting customizable inactivity timeouts.
 */
class VaultSecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aethervault_security", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    companion object {
        private const val ITERATIONS = 10_000
        private const val KEY_LENGTH = 256
        private const val PREF_MASTER_HASH = "master_hash_v2"
        private const val PREF_LEGACY_HASH = "master_hash"
        private const val PREF_DECOY_PIN_HASH = "decoy_pin_hash_v2"
        private const val PREF_LOCK_POLICY = "lock_policy"
        private const val PREF_SESSION_TOKEN = "session_token"
        private const val PREF_LAST_ACTIVE_TIME = "last_active_time"
        private const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    }

    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return "${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    private fun verifyPasswordHash(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = hexToBytes(parts[0])
        val expectedHash = hexToBytes(parts[1])

        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val testHash = factory.generateSecret(spec).encoded

        if (testHash.size != expectedHash.size) return false
        var diff = 0
        for (i in testHash.indices) {
            diff = diff or (testHash[i].toInt() xor expectedHash[i].toInt())
        }
        return diff == 0
    }

    fun setMasterPassword(password: String) {
        val hashed = hashPassword(password)
        prefs.edit()
            .putString(PREF_MASTER_HASH, hashed)
            .remove(PREF_LEGACY_HASH)
            .apply()
    }

    fun setDecoyPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove(PREF_DECOY_PIN_HASH).remove("decoy_pin").apply()
        } else {
            val hashed = hashPassword(pin.trim())
            prefs.edit()
                .putString(PREF_DECOY_PIN_HASH, hashed)
                .remove("decoy_pin")
                .apply()
        }
    }

    /**
     * Verifies master password or decoy PIN.
     * Returns:
     * - Pair(true, false) for primary vault success
     * - Pair(true, true) for stealth decoy vault success
     * - Pair(false, false) for authentication failure
     */
    fun verifyUnlock(inputPass: String, fallbackLegacyPrefs: SharedPreferences): Pair<Boolean, Boolean> {
        val trimmed = inputPass.trim()
        if (trimmed.isEmpty()) return Pair(false, false)

        // 1. Check Decoy PIN first (PBKDF2 or legacy)
        val storedDecoyV2 = prefs.getString(PREF_DECOY_PIN_HASH, null)
        val legacyDecoy = fallbackLegacyPrefs.getString("decoy_pin", null)
        if (storedDecoyV2 != null && verifyPasswordHash(trimmed, storedDecoyV2)) {
            createSessionToken()
            return Pair(true, true) // Decoy unlocked
        } else if (legacyDecoy != null && trimmed == legacyDecoy) {
            // Upgrade legacy decoy to v2
            setDecoyPin(trimmed)
            createSessionToken()
            return Pair(true, true)
        }

        // 2. Check Master Password (PBKDF2 or legacy hash)
        val storedMasterV2 = prefs.getString(PREF_MASTER_HASH, null)
        val legacyHash = fallbackLegacyPrefs.getString(PREF_LEGACY_HASH, null)

        if (storedMasterV2 != null) {
            if (verifyPasswordHash(trimmed, storedMasterV2)) {
                createSessionToken()
                return Pair(true, false) // Primary unlocked
            }
        } else if (!legacyHash.isNullOrBlank()) {
            // Check legacy hashCode() format safely
            if (trimmed.hashCode().toString() == legacyHash) {
                // Auto-upgrade to PBKDF2 v2
                setMasterPassword(trimmed)
                fallbackLegacyPrefs.edit().remove(PREF_LEGACY_HASH).apply()
                createSessionToken()
                return Pair(true, false)
            }
        }

        return Pair(false, false)
    }

    // --- Persistent Login & Session Management ---

    fun getLockPolicy(): LockPolicy {
        val name = prefs.getString(PREF_LOCK_POLICY, LockPolicy.STAY_UNLOCKED.name)
        return runCatching { LockPolicy.valueOf(name ?: LockPolicy.STAY_UNLOCKED.name) }.getOrDefault(LockPolicy.STAY_UNLOCKED)
    }

    fun setLockPolicy(policy: LockPolicy) {
        prefs.edit().putString(PREF_LOCK_POLICY, policy.name).apply()
    }

    fun isSessionValid(): Boolean {
        val token = prefs.getString(PREF_SESSION_TOKEN, null) ?: return false
        if (token.isBlank()) return false

        return when (getLockPolicy()) {
            LockPolicy.STAY_UNLOCKED -> true
            LockPolicy.LOCK_AFTER_INACTIVITY -> {
                val lastActive = prefs.getLong(PREF_LAST_ACTIVE_TIME, 0L)
                val now = System.currentTimeMillis()
                (now - lastActive) < INACTIVITY_TIMEOUT_MS
            }
            LockPolicy.ALWAYS_REQUIRE_AUTH -> false
        }
    }

    fun createSessionToken(): String {
        val token = "sess_${System.currentTimeMillis()}_${random.nextLong()}"
        prefs.edit()
            .putString(PREF_SESSION_TOKEN, token)
            .putLong(PREF_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
        return token
    }

    fun recordActivity() {
        prefs.edit().putLong(PREF_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(PREF_SESSION_TOKEN)
            .remove(PREF_LAST_ACTIVE_TIME)
            .apply()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
