package com.example.vaultcalc.data.browser

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserRepository @Inject constructor(
    private val browserDao: BrowserDao
) {
    fun getBookmarks(): Flow<List<Bookmark>> = browserDao.getBookmarks()

    suspend fun addBookmark(url: String, title: String) {
        browserDao.insertBookmark(Bookmark(url = url, title = title))
    }

    suspend fun removeBookmark(bookmark: Bookmark) {
        browserDao.deleteBookmark(bookmark)
    }

    suspend fun removeBookmark(url: String) {
        browserDao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> = browserDao.isBookmarked(url)

    fun getHistory(): Flow<List<HistoryItem>> = browserDao.getHistory()

    suspend fun addHistoryItem(url: String, title: String) {
        browserDao.insertHistoryItem(HistoryItem(url = url, title = title))
    }

    suspend fun clearHistory() {
        browserDao.clearHistory()
    }
}
