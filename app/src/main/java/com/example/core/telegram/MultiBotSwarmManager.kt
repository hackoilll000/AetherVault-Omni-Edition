package com.example.core.telegram

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BotWorker(
    val botId: Long,
    val token: String,
    val username: String,
    val limiter: TokenBucketLimiter,
    var activeJobs: Int = 0
)

class MultiBotSwarmManager(
    private val client: TelegramBotClient = TelegramBotClient()
) {
    private val workers = mutableListOf<BotWorker>()
    private val mutex = Mutex()

    suspend fun registerBot(token: String): Result<BotWorker> {
        val infoResult = client.getMe(token)
        if (infoResult.isFailure) {
            return Result.failure(infoResult.exceptionOrNull() ?: Exception("Failed to verify bot token"))
        }
        val info = infoResult.getOrThrow()
        mutex.withLock {
            val existing = workers.find { it.botId == info.botId }
            if (existing != null) {
                return Result.success(existing)
            }
            val worker = BotWorker(
                botId = info.botId,
                token = token,
                username = info.username,
                limiter = TokenBucketLimiter(info.botId)
            )
            workers.add(worker)
            return Result.success(worker)
        }
    }

    suspend fun removeBot(botId: Long) = mutex.withLock {
        workers.removeAll { it.botId == botId }
    }

    suspend fun getAvailableWorker(): BotWorker? = mutex.withLock {
        val eligible = workers.filter { it.limiter.isAvailable() }
        val worker = eligible.minByOrNull { it.activeJobs } ?: return null
        if (worker.limiter.tryAcquire()) {
            worker.activeJobs++
            return worker
        }
        return null
    }

    suspend fun releaseWorker(worker: BotWorker) = mutex.withLock {
        worker.activeJobs = (worker.activeJobs - 1).coerceAtLeast(0)
    }

    suspend fun markRateLimited(worker: BotWorker, retryAfterSeconds: Long) = mutex.withLock {
        worker.limiter.setCooldown(retryAfterSeconds)
        worker.activeJobs = (worker.activeJobs - 1).coerceAtLeast(0)
    }

    fun getWorkerCount(): Int = workers.size

    fun getWorkers(): List<BotWorker> = workers.toList()
}
