package com.example.vaultcalc.download.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.vaultcalc.R
import com.example.vaultcalc.data.download.DownloadRepository
import com.example.vaultcalc.data.download.DownloadState
import com.example.vaultcalc.data.download.DownloadTask
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class SecureDownloadService : LifecycleService() {

    @Inject
    lateinit var repository: DownloadRepository

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_ACTION = "action"
        const val CHANNEL_ID = "VaultDownloadChannel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(NOTIFICATION_ID, createNotification("Download Service Active", "Managing secure downloads..."))

        intent?.let {
            val taskId = it.getStringExtra(EXTRA_TASK_ID)
            val actionStr = it.getStringExtra(EXTRA_ACTION)

            if (taskId != null && actionStr != null) {
                val action = DownloadAction.valueOf(actionStr)
                handleAction(taskId, action)
            }
        }

        return START_STICKY
    }

    private fun handleAction(taskId: String, action: DownloadAction) {
        when (action) {
            DownloadAction.START -> startDownload(taskId)
            DownloadAction.PAUSE -> pauseDownload(taskId)
            DownloadAction.RESUME -> startDownload(taskId, isResume = true)
            DownloadAction.CANCEL -> cancelDownload(taskId)
        }
    }

    private fun startDownload(taskId: String, isResume: Boolean = false) {
        if (activeJobs.containsKey(taskId)) return // Already running

        val job = lifecycleScope.launch(Dispatchers.IO) {
            val task = repository.getDownloadById(taskId) ?: return@launch

            try {
                repository.updateTaskState(taskId, DownloadState.DOWNLOADING)

                // Set up storage directory
                val vaultDir = File(filesDir, "vault_downloads")
                if (!vaultDir.exists()) vaultDir.mkdirs()

                val finalFile = File(vaultDir, "${task.id}_${task.fileName}")
                val tempFile = File(vaultDir, "${task.id}_${task.fileName}.part")

                var downloaded = if (isResume && tempFile.exists()) tempFile.length() else 0L
                var append = isResume && downloaded > 0

                val connection = URL(task.resolvedUrl ?: task.originalUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (append) {
                    connection.setRequestProperty("Range", "bytes=$downloaded-")
                }

                connection.connect()

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                     repository.updateTaskStateWithError(taskId, DownloadState.FAILED, "HTTP $responseCode")
                     return@launch
                }

                // Crucial Check: If we asked to resume (append) but the server gave us 200 OK
                // instead of 206 Partial Content, the server ignored the Range header.
                // We MUST discard the existing temp file and start over to avoid corruption.
                if (append && responseCode == 200) {
                    append = false
                    downloaded = 0L
                    if (tempFile.exists()) tempFile.delete()
                }

                // Extract proper content length considering range
                val contentLengthStr = connection.getHeaderField("Content-Length")
                val totalLength = if (contentLengthStr != null) {
                    if (append) downloaded + contentLengthStr.toLong() else contentLengthStr.toLong()
                } else {
                    task.totalBytes
                }

                // Update task with total length if we didn't have it
                if (task.totalBytes <= 0 && totalLength > 0) {
                     repository.updateTask(task.copy(totalBytes = totalLength))
                }

                val input: InputStream = connection.inputStream
                val output = FileOutputStream(tempFile, append)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var lastUpdate = System.currentTimeMillis()

                while (isActive) {
                    bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    // Throttle DB updates to avoid thrashing
                    if (System.currentTimeMillis() - lastUpdate > 1000) {
                        repository.updateTaskProgress(taskId, downloaded)
                        lastUpdate = System.currentTimeMillis()
                        updateNotificationProgress(task.fileName, downloaded, totalLength)
                    }
                }

                output.flush()
                output.close()
                input.close()
                connection.disconnect()

                if (isActive) {
                    // Check completion integrity if we know the size
                    if (totalLength > 0 && downloaded < totalLength) {
                         repository.updateTaskStateWithError(taskId, DownloadState.FAILED, "Incomplete download")
                    } else {
                        // Success! Rename temp to final
                        if (tempFile.renameTo(finalFile)) {
                            val completedTask = task.copy(
                                state = DownloadState.COMPLETED,
                                downloadedBytes = downloaded,
                                totalBytes = totalLength.takeIf { it > 0 } ?: downloaded,
                                filePath = finalFile.absolutePath
                            )
                            repository.updateTask(completedTask)
                        } else {
                            repository.updateTaskStateWithError(taskId, DownloadState.FAILED, "File system error")
                        }
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Normal cancellation handled elsewhere
                    throw e
                }
                e.printStackTrace()
                repository.updateTaskStateWithError(taskId, DownloadState.FAILED, e.message)
            } finally {
                activeJobs.remove(taskId)
                checkStopService()
            }
        }
        activeJobs[taskId] = job
    }

    private fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTaskState(taskId, DownloadState.PAUSED)
            checkStopService()
        }
    }

    private fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        lifecycleScope.launch(Dispatchers.IO) {
            val task = repository.getDownloadById(taskId)
            if (task != null) {
                 repository.updateTaskState(taskId, DownloadState.CANCELLED)

                 // Cleanup temp file
                 val vaultDir = File(filesDir, "vault_downloads")
                 val tempFile = File(vaultDir, "${task.id}_${task.fileName}.part")
                 if (tempFile.exists()) tempFile.delete()
            }
            checkStopService()
        }
    }

    private fun checkStopService() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vault Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Secure download notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Standard Android download icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotificationProgress(fileName: String, current: Long, total: Long) {
        val max = if (total > 0) 100 else 0
        val progress = if (total > 0) ((current.toFloat() / total) * 100).toInt() else 0

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading: $fileName")
            .setContentText(if (total > 0) "$progress%" else "Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progress, total <= 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
