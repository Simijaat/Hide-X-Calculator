package com.example.vaultcalc.download.engine

import android.content.Context
import android.content.Intent
import com.example.vaultcalc.data.download.DownloadRepository
import com.example.vaultcalc.data.download.DownloadState
import com.example.vaultcalc.data.download.DownloadTask
import com.example.vaultcalc.download.resolver.DownloadOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository
) {
    val allDownloads: Flow<List<DownloadTask>> = repository.allDownloads

    suspend fun enqueueDownload(option: DownloadOption, originalUrl: String) {
        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = taskId,
            originalUrl = originalUrl,
            resolvedUrl = option.url,
            fileName = option.title,
            mimeType = option.mimeType,
            totalBytes = option.sizeBytes,
            downloadedBytes = 0,
            state = DownloadState.QUEUED,
            filePath = null,
            createdAt = System.currentTimeMillis(),
            errorReason = null
        )
        repository.insertTask(task)

        // Start the foreground service to handle the download
        sendCommandToService(taskId, DownloadAction.START)
    }

    fun pauseDownload(taskId: String) {
        sendCommandToService(taskId, DownloadAction.PAUSE)
    }

    fun resumeDownload(taskId: String) {
        sendCommandToService(taskId, DownloadAction.RESUME)
    }

    fun cancelDownload(taskId: String) {
        sendCommandToService(taskId, DownloadAction.CANCEL)
    }

    suspend fun deleteDownload(task: DownloadTask) {
        // Stop it if it's running
        if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.QUEUED) {
             sendCommandToService(task.id, DownloadAction.CANCEL)
        }

        // Remove final file from storage
        task.filePath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }

        // Cleanup temp file manually here to prevent the race condition
        // where the service tries to clean it up after the DB record is gone.
        val vaultDir = File(context.filesDir, "vault_downloads")
        val tempFile = File(vaultDir, "${task.id}_${task.fileName}.part")
        if (tempFile.exists()) tempFile.delete()

        repository.deleteTask(task)
    }

    private fun sendCommandToService(taskId: String, action: DownloadAction) {
        val intent = Intent(context, SecureDownloadService::class.java).apply {
            putExtra(SecureDownloadService.EXTRA_TASK_ID, taskId)
            putExtra(SecureDownloadService.EXTRA_ACTION, action.name)
        }
        context.startForegroundService(intent)
    }
}
