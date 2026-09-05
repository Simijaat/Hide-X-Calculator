package com.example.vaultcalc.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.browser.BrowserRepository
import com.example.vaultcalc.data.browser.BrowserTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val browserRepository: BrowserRepository
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val bookmarks = browserRepository.getBookmarks()
    val history = browserRepository.getHistory()
    val recentSearchHistory = browserRepository.getHistory().map { list -> list.take(10) }

    init {
        viewModelScope.launch {
            val savedTabs = browserRepository.getTabsSnapshot()
            if (savedTabs.isNotEmpty()) {
                _tabs.value = savedTabs
                addNewTab("")
            } else {
                addNewTab("")
            }
        }
    }

    fun addNewTab(url: String = "https://duckduckgo.com") {
        val newTab = BrowserTab(id = UUID.randomUUID().toString(), url = url)
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        viewModelScope.launch { browserRepository.saveTab(newTab) }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun closeTab(tabId: String) {
        _tabs.update { it.filter { tab -> tab.id != tabId } }
        if (_activeTabId.value == tabId) {
            _activeTabId.value = _tabs.value.lastOrNull()?.id
        }
        viewModelScope.launch {
            browserRepository.removeTab(tabId)
        }
        // Don't auto-add a new tab if they close the last one, to support direct minimize if needed. Or we can keep it as is, but request said: "minimize kerne ka button bnao". We'll handle minimize in the UI.
        if (_tabs.value.isEmpty()) {
            addNewTab()
        }
    }

    fun updateCurrentTabUrl(url: String) {
        val activeId = _activeTabId.value ?: return
        _tabs.update { currentTabs ->
            currentTabs.map {
                if (it.id == activeId) {
                    val updatedTab = it.copy(url = url)
                    viewModelScope.launch { browserRepository.saveTab(updatedTab) }
                    updatedTab
                } else it
            }
        }
    }

    fun updateCurrentTabTitle(title: String) {
        val activeId = _activeTabId.value ?: return
        _tabs.update { currentTabs ->
            currentTabs.map {
                if (it.id == activeId) {
                    val updatedTab = it.copy(title = title)
                    viewModelScope.launch { browserRepository.saveTab(updatedTab) }
                    updatedTab
                } else it
            }
        }
    }

    fun updateCurrentTabProgress(progress: Int) {
        val activeId = _activeTabId.value ?: return
        _tabs.update { currentTabs ->
            currentTabs.map {
                if (it.id == activeId) {
                    val updatedTab = it.copy(progress = progress, isLoading = progress < 100)
                    viewModelScope.launch { browserRepository.saveTab(updatedTab) }
                    updatedTab
                } else it
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            browserRepository.addBookmark(url, title)
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            browserRepository.removeBookmark(url)
        }
    }

    fun addHistory(url: String, title: String) {
        viewModelScope.launch {
            browserRepository.addHistoryItem(url, title)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            browserRepository.clearHistory()
        }
    }
}
