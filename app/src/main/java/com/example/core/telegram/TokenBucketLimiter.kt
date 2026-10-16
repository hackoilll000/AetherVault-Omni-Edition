package com.example.core.telegram

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Token-Bucket Rate Limiter per Bot
 * Enforces maximum 18 requests per minute ceiling and handles 429 cooldowns
 */
class TokenBucketLimiter(
    val botId: Long,
    private val maxTokens: Int = 18,
    private val refillIntervalMs: Long = 60_000L
) {
    private var availableTokens: Int = maxTokens
    private var lastRefillTime: Long = System.currentTimeMillis()
    private var cooldownUntil: Long = 0L
    private val mutex = Mutex()

    suspend fun tryAcquire(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now < cooldownUntil) {
            return false
        }
        refill(now)
        if (availableTokens > 0) {
            availableTokens--
            return true
        }
        return false
    }

    suspend fun setCooldown(seconds: Long) = mutex.withLock {
        cooldownUntil = System.currentTimeMillis() + (seconds * 1000L)
        availableTokens = 0
    }

    suspend fun isAvailable(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now < cooldownUntil) return false
        refill(now)
        return availableTokens > 0
    }

    private fun refill(now: Long) {
        val elapsed = now - lastRefillTime
        if (elapsed >= refillIntervalMs) {
            availableTokens = maxTokens
            lastRefillTime = now
        }
    }
}
