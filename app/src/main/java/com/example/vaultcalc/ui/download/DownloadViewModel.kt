package com.example.vaultcalc.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.download.DownloadState
import com.example.vaultcalc.data.download.DownloadTask
import com.example.vaultcalc.download.engine.DownloadManager
import com.example.vaultcalc.download.resolver.DownloadOption
import com.example.vaultcalc.download.resolver.LinkResolverManager
import com.example.vaultcalc.download.storage.DownloadStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val resolverManager: LinkResolverManager,
    private val downloadManager: DownloadManager,
    private val storageManager: DownloadStorageManager
) : ViewModel() {

    val allDownloads = downloadManager.allDownloads

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolvedOptions = MutableStateFlow<List<DownloadOption>>(emptyList())
    val resolvedOptions: StateFlow<List<DownloadOption>> = _resolvedOptions.asStateFlow()

    private val _originalUrl = MutableStateFlow<String?>(null)

    fun resolveUrl(url: String) {
        viewModelScope.launch {
            _isResolving.value = true
            _originalUrl.value = url
            try {
                val options = resolverManager.resolve(url)
                _resolvedOptions.value = options
            } catch (e: Exception) {
                _resolvedOptions.value = emptyList()
            } finally {
                _isResolving.value = false
            }
        }
    }

    fun clearResolvedOptions() {
        _resolvedOptions.value = emptyList()
        _originalUrl.value = null
    }

    fun startDownload(option: DownloadOption) {
        viewModelScope.launch {
            val url = _originalUrl.value ?: option.url
            downloadManager.enqueueDownload(option, url)
            clearResolvedOptions()
        }
    }

    fun pauseDownload(taskId: String) {
        downloadManager.pauseDownload(taskId)
    }

    fun resumeDownload(taskId: String) {
        downloadManager.resumeDownload(taskId)
    }

    fun cancelDownload(taskId: String) {
        downloadManager.cancelDownload(taskId)
    }

    fun deleteDownload(task: DownloadTask) {
        viewModelScope.launch {
            downloadManager.deleteDownload(task)
        }
    }

    fun exportDownload(task: DownloadTask) {
        viewModelScope.launch {
            if (task.state == DownloadState.COMPLETED && task.filePath != null) {
                storageManager.exportToPublicDownloads(task.filePath, task.fileName, task.mimeType)
            }
        }
    }
}
