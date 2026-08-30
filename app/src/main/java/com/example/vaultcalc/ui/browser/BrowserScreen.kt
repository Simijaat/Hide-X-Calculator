package com.example.vaultcalc.ui.browser

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.vaultcalc.ui.theme.AddressBarGray
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: (String?) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showTabsDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())

    val isBookmarked = bookmarks.any { it.url == activeTab?.url }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val homeUrl = "https://duckduckgo.com"
                    urlInput = homeUrl
                    viewModel.updateCurrentTabUrl(homeUrl)
                    webViewRef?.loadUrl(homeUrl)
                }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(AddressBarGray, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                var finalUrl = urlInput
                                if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                    finalUrl = "https://duckduckgo.com/?q=${urlInput.replace(" ", "+")}"
                                }
                                viewModel.updateCurrentTabUrl(finalUrl)
                                webViewRef?.loadUrl(finalUrl)
                            }
                        )
                    )

                    IconButton(onClick = {
                        if (isBookmarked) {
                            activeTab?.url?.let { viewModel.removeBookmark(it) }
                        } else {
                            activeTab?.url?.let { viewModel.addBookmark(it, activeTab.title) }
                        }
                    }) {
                        Icon(
                            if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { viewModel.addNewTab() }) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                        .clickable { showTabsDialog = true }
                ) {
                    Text("${tabs.size}", style = MaterialTheme.typography.labelSmall)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Back") },
                            onClick = { webViewRef?.goBack(); showMenu = false },
                            enabled = webViewRef?.canGoBack() == true
                        )
                        DropdownMenuItem(
                            text = { Text("Forward") },
                            onClick = { webViewRef?.goForward(); showMenu = false },
                            enabled = webViewRef?.canGoForward() == true
                        )
                        if (activeTab?.isLoading == true) {
                            DropdownMenuItem(
                                text = { Text("Stop Loading") },
                                onClick = { webViewRef?.stopLoading(); showMenu = false }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Reload") },
                                onClick = { webViewRef?.reload(); showMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            onClick = { onNavigateToDownloads(null); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            onClick = { showBookmarksDialog = true; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = { showHistoryDialog = true; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (viewModel.isAdBlockingEnabled) "Disable AdBlock" else "Enable AdBlock") },
                            onClick = {
                                viewModel.toggleAdBlocking()
                                showMenu = false
                                webViewRef?.reload()
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (activeTab?.isLoading == true) {
                LinearProgressIndicator(
                    progress = activeTab.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            key(activeTab?.id) {
                activeTab?.let { tab ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                }

                                val adBlockManager = com.example.vaultcalc.di.BrowserEntryPointAccessor.getAdBlockManager(ctx)

                                webViewClient = SecureWebViewClient(
                                    adBlockManager = adBlockManager,
                                    onPageStarted = { url ->
                                        urlInput = url
                                        viewModel.updateCurrentTabUrl(url)
                                    },
                                    onPageFinished = { url, title ->
                                        urlInput = url
                                        if (title != null) viewModel.updateCurrentTabTitle(title)
                                        viewModel.addHistory(url, title ?: "Unknown")
                                    }
                                )

                                webChromeClient = SecureWebChromeClient(
                                    onProgressChanged = { progress ->
                                        viewModel.updateCurrentTabProgress(progress)
                                    },
                                    onTitleReceived = { title ->
                                        viewModel.updateCurrentTabTitle(title)
                                    }
                                )

                                setDownloadListener { url, _, _, _, _ ->
                                    onNavigateToDownloads(url)
                                }

                                loadUrl(tab.url)
                            }
                        }
                    )
                }
            }
        }

        if (showTabsDialog) {
            AlertDialog(
                onDismissRequest = { showTabsDialog = false },
                title = { Text("Open Tabs") },
                text = {
                    LazyColumn {
                        items(tabs) { tab ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.switchTab(tab.id)
                                        showTabsDialog = false
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = tab.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = tab.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.closeTab(tab.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Tab")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTabsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showBookmarksDialog) {
            AlertDialog(
                onDismissRequest = { showBookmarksDialog = false },
                title = { Text("Bookmarks") },
                text = {
                    LazyColumn {
                        items(bookmarks) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateCurrentTabUrl(bookmark.url)
                                        webViewRef?.loadUrl(bookmark.url)
                                        showBookmarksDialog = false
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = bookmark.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = bookmark.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.removeBookmark(bookmark.url) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarksDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("History")
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    }
                },
                text = {
                    LazyColumn {
                        items(history) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateCurrentTabUrl(historyItem.url)
                                        webViewRef?.loadUrl(historyItem.url)
                                        showHistoryDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = historyItem.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = historyItem.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
