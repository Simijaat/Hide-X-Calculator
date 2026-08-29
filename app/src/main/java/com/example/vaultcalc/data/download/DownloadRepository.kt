package com.example.vaultcalc.data.download

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {
    val allDownloads: Flow<List<DownloadTask>> = downloadDao.getAllDownloads()

    suspend fun getDownloadById(id: String): DownloadTask? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun insertTask(task: DownloadTask) {
        downloadDao.insert(task)
    }

    suspend fun updateTask(task: DownloadTask) {
        downloadDao.update(task)
    }

    suspend fun deleteTask(task: DownloadTask) {
        downloadDao.delete(task)
    }

    suspend fun updateTaskState(id: String, state: DownloadState) {
        downloadDao.updateState(id, state)
    }

    suspend fun updateTaskProgress(id: String, bytes: Long) {
        downloadDao.updateProgress(id, bytes)
    }

    suspend fun updateTaskStateWithError(id: String, state: DownloadState, errorReason: String?) {
        downloadDao.updateStateWithError(id, state, errorReason)
    }
}
