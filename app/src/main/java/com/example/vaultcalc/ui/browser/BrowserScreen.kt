package com.example.vaultcalc.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.data.browser.BrowserTab

val AppBlack = Color(0xFF121212)

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
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val recentSearchHistory by viewModel.recentSearchHistory.collectAsState(initial = emptyList())

    val activeTab = tabs.find { it.id == activeTabId }

    var urlInput by remember { mutableStateOf(activeTab?.url ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTabsScreen by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val tabStates = remember { mutableStateMapOf<String, android.os.Bundle>() }
    var currentTabId by remember { mutableStateOf(activeTabId) }

    LaunchedEffect(activeTabId) {
        val tab = tabs.find { it.id == activeTabId }
        urlInput = tab?.url ?: ""
    }

    BackHandler {
        if (showTabsScreen) {
            showTabsScreen = false
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onNavigateBack() // Interpret system back as minimize
        }
    }

    if (showTabsScreen) {
        TabsScreen(
            tabs = tabs,
            activeTabId = activeTabId,
            onCloseTab = { viewModel.closeTab(it) },
            onSwitchTab = {
                viewModel.switchTab(it)
                showTabsScreen = false
            },
            onAddTab = {
                viewModel.addNewTab("")
                showTabsScreen = false
            },
            onBack = { showTabsScreen = false }
        )
    } else {
        Scaffold(
            containerColor = AppBlack,
            snackbarHost = {
                if (errorMessage != null) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { errorMessage = null }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(errorMessage!!)
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (activeTab != null && activeTab.url.isEmpty()) {
                    BrowserHomePage(
                        recentHistory = recentSearchHistory,
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onSearch = { query ->
                            val formatted = formatSearchUrl(query)
                            viewModel.updateCurrentTabUrl(formatted)
                        },
                        onClearHistory = { viewModel.clearHistory() },
                        onShowTabs = { showTabsScreen = true },
                        tabsCount = tabs.size,
                        onNavigateBack = onNavigateBack // Pass minimize intent
                    )
                } else {
                    // Advanced Browser Top Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(56.dp)
                            .background(
                                color = Color(0xFF2C2C2E),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF3A3A3C),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = Color(0xFF30D158),
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .size(16.dp)
                            )

                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val formatted = formatSearchUrl(urlInput)
                                        viewModel.updateCurrentTabUrl(formatted)
                                        webViewRef?.loadUrl(formatted)
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            text = "Search or type URL",
                                            color = Color.Gray,
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = { showTabsScreen = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, Color.White, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabs.size.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(Color(0xFF2C2C2C))
                                ) {
                                DropdownMenuItem(
                                    text = { Text("Minimize", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White) },
                                    onClick = { onNavigateBack(); showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Back", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) },
                                    onClick = { webViewRef?.goBack(); showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Forward", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White) },
                                    onClick = { webViewRef?.goForward(); showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reload", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) },
                                    onClick = { webViewRef?.reload(); showMoreMenu = false }
                                )

                                val isBookmarked = bookmarks.any { it.url == activeTab?.url }
                                DropdownMenuItem(
                                    text = { Text("Save", color = Color.White) },
                                    leadingIcon = {
                                        Icon(
                                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    },
                                    onClick = {
                                        activeTab?.let {
                                            if (isBookmarked) {
                                                viewModel.removeBookmark(it.url)
                                            } else {
                                                viewModel.addBookmark(it.url, it.title)
                                            }
                                        }
                                        showMoreMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Bookmarks", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null, tint = Color.White) },
                                    onClick = { showBookmarksDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("History", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color.White) },
                                    onClick = { showHistoryDialog = true; showMoreMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Downloads", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.White) },
                                    onClick = { onNavigateToDownloads(null); showMoreMenu = false }
                                )
                            }
                        }
                    }

                    }
                    if (activeTab?.isLoading == true) {
                        LinearProgressIndicator(
                            progress = activeTab.progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent,
                        )
                    }

                    AndroidView(
                        update = { webView ->
                            webViewRef = webView
                            if (currentTabId != activeTabId) {
                                currentTabId?.let { oldId ->
                                    val bundle = android.os.Bundle()
                                    webView.saveState(bundle)
                                    tabStates[oldId] = bundle
                                }
                                currentTabId = activeTabId
                                val state = tabStates[activeTabId]
                                if (state != null) {
                                    webView.restoreState(state)
                                } else if (activeTab != null && activeTab.url.isNotEmpty()) {
                                    webView.loadUrl(activeTab.url)
                                }
                            } else if (activeTab != null && activeTab.url.isNotEmpty() && (webView.url == null || webView.url == "about:blank")) {
                                webView.loadUrl(activeTab.url)
                            }

                            // Background color fix for white screen
                            webView.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
                        },
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false

                                setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
                                webViewClient = SecureWebViewClient(
                                    onPageStarted = { url ->
                                        viewModel.updateCurrentTabUrl(url)
                                        viewModel.updateCurrentTabProgress(0)
                                    },
                                    onPageFinished = { url, title ->
                                        if (url != "about:blank") {
                                            urlInput = url
                                        }
                                        if (title != null) viewModel.updateCurrentTabTitle(title)
                                        if (url != "about:blank") {
                                            viewModel.addHistory(url, title ?: "Unknown")
                                        }
                                    },
                                    onError = { error ->
                                        errorMessage = error
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
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showBookmarksDialog) {
            SimpleListDialog("Bookmarks", bookmarks.map { it.title to it.url },
                onDismiss = { showBookmarksDialog = false },
                onSelect = { url ->
                    viewModel.updateCurrentTabUrl(url)
                    showBookmarksDialog = false
                },
                onClear = null
            )
        }

        if (showHistoryDialog) {
            SimpleListDialog("History", history.map { it.title to it.url },
                onDismiss = { showHistoryDialog = false },
                onSelect = { url ->
                    viewModel.updateCurrentTabUrl(url)
                    showHistoryDialog = false
                },
                onClear = { viewModel.clearHistory() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHomePage(
    recentHistory: List<com.example.vaultcalc.data.browser.HistoryItem>,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onShowTabs: () -> Unit,
    tabsCount: Int,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppBlack).padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar area for Home Page
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                var showHomeMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showHomeMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showHomeMenu,
                    onDismissRequest = { showHomeMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2C))
                ) {
                    DropdownMenuItem(
                        text = { Text("Minimize", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White) },
                        onClick = { onNavigateBack(); showHomeMenu = false }
                    )
                }
            }

            IconButton(onClick = onShowTabs) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabsCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Text(
            text = "G O O G L E",
            fontSize = 42.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Advanced Home Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    color = Color(0xFF2C2C2E),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3A3A3C),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray,
                    modifier = Modifier.padding(end = 12.dp)
                )

                BasicTextField(
                    value = urlInput,
                    onValueChange = onUrlInputChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = { onSearch(urlInput) }
                    ),
                    decorationBox = { innerTextField ->
                        if (urlInput.isEmpty()) {
                            Text(
                                text = "Search or type web address",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                if (urlInput.isNotEmpty()) {
                    IconButton(
                        onClick = { onUrlInputChange("") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent History", style = MaterialTheme.typography.titleMedium, color = Color.White)
            TextButton(onClick = onClearHistory) {
                Text("Clear", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(recentHistory) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSearch(item.url) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text(item.url, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onCloseTab: (String) -> Unit,
    onSwitchTab: (String) -> Unit,
    onAddTab: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppBlack,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF1E1E1E)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tabs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Done", tint = Color.White)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTab, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "New Tab", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tabs) { tab ->
                val isSelected = tab.id == activeTabId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { onSwitchTab(tab.id) }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (tab.title.isEmpty() || tab.url.isEmpty()) "New Tab" else tab.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (tab.url.isEmpty()) "Google Search" else tab.url, color = Color.Gray, maxLines = 1, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { onCloseTab(tab.id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleListDialog(
    title: String,
    items: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onClear: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2C),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White)
                if (onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Red)
                    }
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(items) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.second) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(item.first, color = Color.White, maxLines = 1)
                        Text(item.second, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

fun formatSearchUrl(input: String): String {
    if (input.isBlank()) return ""
    return if (input.startsWith("http://") || input.startsWith("https://")) {
        input
    } else if (input.contains(".") && !input.contains(" ")) {
        "https://$input"
    } else {
        "https://www.google.com/search?q=${input.replace(" ", "+")}"
    }
}
