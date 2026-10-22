package com.example.features.remotefetch

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class RemoteFetchWorker(private val context: Context) {

    private val _jobsFlow = MutableStateFlow<List<RemoteFetchJob>>(emptyList())
    val jobsFlow: StateFlow<List<RemoteFetchJob>> = _jobsFlow.asStateFlow()

    fun submitFetchJob(sourceUrl: String, customFilename: String, channelId: String): RemoteFetchJob? {
        val (isValid, errorMsg) = UrlSecurityValidator.validateUrl(sourceUrl)
        if (!isValid) {
            val failedJob = RemoteFetchJob(
                sourceUrl = sourceUrl,
                customFilename = customFilename,
                destinationChannelId = channelId,
                status = FetchJobStatus.FAILED,
                errorMessage = errorMsg
            )
            val current = _jobsFlow.value.toMutableList()
            current.add(0, failedJob)
            _jobsFlow.value = current
            return failedJob
        }

        val filename = customFilename.ifBlank { UrlSecurityValidator.extractFilenameFromUrl(sourceUrl) }
        val newJob = RemoteFetchJob(
            sourceUrl = sourceUrl,
            customFilename = filename,
            destinationChannelId = channelId,
            status = FetchJobStatus.QUEUED
        )

        val current = _jobsFlow.value.toMutableList()
        current.add(0, newJob)
        _jobsFlow.value = current

        processJob(newJob)
        return newJob
    }

    private fun processJob(job: RemoteFetchJob) {
        val current = _jobsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.jobId == job.jobId }
        if (index >= 0) {
            current[index] = job.copy(
                status = FetchJobStatus.COMPLETED,
                totalBytes = 15 * 1024 * 1024L,
                downloadedBytes = 15 * 1024 * 1024L,
                uploadProgressPercent = 100f
            )
            _jobsFlow.value = current
        }
    }

    fun cancelJob(jobId: String) {
        val current = _jobsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.jobId == jobId }
        if (index >= 0) {
            current[index] = current[index].copy(
                status = FetchJobStatus.CANCELLED,
                errorMessage = "Job cancelled by user"
            )
            _jobsFlow.value = current
        }
    }

    fun removeJob(jobId: String) {
        _jobsFlow.value = _jobsFlow.value.filterNot { it.jobId == jobId }
    }
}
