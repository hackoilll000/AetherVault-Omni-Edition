package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.crypto.VaultSecurityManager
import com.example.core.crypto.LockPolicy
import com.example.core.sync.TransferCoordinator
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AetherVault", appName)
    }

    @Test
    fun `test VaultSecurityManager PBKDF2 password hashing and verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = VaultSecurityManager(context)

        val masterPass = "MySecureMasterPass2026!"
        val hash = securityManager.hashPassword(masterPass)

        assertTrue("Hash should contain salted delimiter", hash.contains(":"))
        assertNotEquals("Hash should not equal raw password", masterPass, hash)

        // Save master hash
        val prefs = context.getSharedPreferences("aethervault_security", Context.MODE_PRIVATE)
        prefs.edit().putString("master_hash_v2", hash).apply()

        // Verify valid password
        val (unlocked, isDecoy) = securityManager.verifyUnlock(masterPass, context.getSharedPreferences("aethervault_prefs", Context.MODE_PRIVATE))
        assertTrue("Correct master password should unlock vault", unlocked)
        assertFalse("Primary password should not trigger decoy mode", isDecoy)

        // Verify invalid password failure
        val (badUnlocked, _) = securityManager.verifyUnlock("WrongPassword123", context.getSharedPreferences("aethervault_prefs", Context.MODE_PRIVATE))
        assertFalse("Wrong password should be rejected", badUnlocked)

        // Verify hardcoded bypass "123456" fails
        val (bypassUnlocked, _) = securityManager.verifyUnlock("123456", context.getSharedPreferences("aethervault_prefs", Context.MODE_PRIVATE))
        assertFalse("Bypass '123456' must fail when not the set password", bypassUnlocked)
    }

    @Test
    fun `test VaultSecurityManager legacy hashCode migration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = VaultSecurityManager(context)

        val rawPass = "LegacySecretPass"
        val legacyHash = rawPass.hashCode().toString()

        val legacyPrefs = context.getSharedPreferences("aethervault_prefs", Context.MODE_PRIVATE)
        legacyPrefs.edit().putString("master_hash", legacyHash).apply()

        // Verify unlock auto-migrates legacy hash to salted PBKDF2
        val (unlocked, _) = securityManager.verifyUnlock(rawPass, legacyPrefs)
        assertTrue("Legacy password should unlock and migrate", unlocked)

        val secPrefs = context.getSharedPreferences("aethervault_security", Context.MODE_PRIVATE)
        val migratedHash = secPrefs.getString("master_hash_v2", null)
        assertNotNull("Legacy hash should be migrated to master_hash_v2", migratedHash)
        assertTrue("Migrated hash should use PBKDF2 format", migratedHash!!.contains(":"))
    }

    @Test
    fun `test VaultSecurityManager persistent login session validity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = VaultSecurityManager(context)

        securityManager.setLockPolicy(LockPolicy.STAY_UNLOCKED)
        val sessionToken = securityManager.createSessionToken()
        assertNotNull("Session token should be generated", sessionToken)

        assertTrue("Session should be valid when lock policy is STAY_UNLOCKED", securityManager.isSessionValid())

        securityManager.clearSession()
        assertFalse("Cleared session should be invalid", securityManager.isSessionValid())
    }

    @Test
    fun `test TransferCoordinator cancellation and job registry`() {
        val taskId = "test_task_chunk_101"
        val testJob = Job()
        TransferCoordinator.register(taskId, testJob)

        assertFalse("Task should not be marked cancelled initially", TransferCoordinator.isCancelled(taskId))

        TransferCoordinator.cancel(taskId)
        assertTrue("Task should be marked cancelled after cancellation", TransferCoordinator.isCancelled(taskId))

        TransferCoordinator.clean(taskId)
        assertFalse("Cleaned task should reset status", TransferCoordinator.isCancelled(taskId))
    }
}
