package com.example.core.sync

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * TransferCoordinator:
 * Coordinates active background upload and download coroutine Jobs,
 * ensuring real thread cancellation, resource cleanup, and prevention of race conditions.
 */
object TransferCoordinator {
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val cancelledTasks = ConcurrentHashMap.newKeySet<String>()

    fun register(taskId: String, job: Job) {
        cancelledTasks.remove(taskId)
        activeJobs[taskId] = job
        job.invokeOnCompletion {
            activeJobs.remove(taskId)
        }
    }

    fun cancel(taskId: String): Boolean {
        cancelledTasks.add(taskId)
        val job = activeJobs.remove(taskId)
        return if (job != null && job.isActive) {
            job.cancel()
            true
        } else {
            false
        }
    }

    fun isCancelled(taskId: String): Boolean {
        return cancelledTasks.contains(taskId)
    }

    fun clean(taskId: String) {
        activeJobs.remove(taskId)
        cancelledTasks.remove(taskId)
    }

    fun getActiveTaskCount(): Int = activeJobs.size
}
