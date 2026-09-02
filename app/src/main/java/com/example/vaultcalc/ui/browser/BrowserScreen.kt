package com.example.vaultcalc.ui.browser

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.ui.theme.AppBlack
import com.example.vaultcalc.data.browser.BrowserTab

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
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showTabsScreen by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val recentHistory by viewModel.recentSearchHistory.collectAsState(initial = emptyList())

    val isBookmarked = bookmarks.any { it.url == activeTab?.url }


    // Handle back button presses to navigate back in WebView history if possible
    BackHandler(enabled = webViewRef?.canGoBack() == true && activeTab != null && activeTab.url.isNotEmpty()) {
        webViewRef?.goBack()
    }

    // Full screen Tabs Switcher overlay
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
        return
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            if (activeTab != null && activeTab.url.isNotEmpty()) {
                Surface(
                    color = AppBlack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            // Premium pill-shaped address bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF2C2C2C))
                                    .clickable { /* Could open a search dialog here, for now just show URL */ }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure",
                                        tint = Color(0xFF34C759), // iOS green
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeTab.url,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            webViewRef?.reload()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            IconButton(onClick = { showTabsScreen = true }) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(2.dp, Color.White, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabs.size.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(Color(0xFF2C2C2C))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Bookmarks", color = Color.White) },
                                        onClick = { showBookmarksDialog = true; showMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("History", color = Color.White) },
                                        onClick = { showHistoryDialog = true; showMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isBookmarked) "Remove Bookmark" else "Add Bookmark", color = Color.White) },
                                        onClick = {
                                            activeTab.url.let { url ->
                                                if (url.isNotEmpty()) {
                                                    if (isBookmarked) {
                                                        viewModel.removeBookmark(url)
                                                    } else {
                                                        viewModel.addBookmark(url, activeTab.title)
                                                    }
                                                }
                                            }
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        // Premium progress bar (thin, iOS style)
                        if (activeTab.isLoading) {
                            LinearProgressIndicator(
                                progress = activeTab.progress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = Color(0xFF0A84FF), // iOS Blue
                                trackColor = Color.Transparent
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // Render specific tab content (Home Page OR WebView)
            key(activeTab?.id) {
                if (activeTab != null) {
                    if (activeTab.url.isEmpty()) {
                        BrowserHomePage(
                            recentHistory = recentHistory,
                            urlInput = urlInput,
                            onUrlInputChange = { urlInput = it },
                            onSearch = { query ->
                                val finalUrl = formatSearchUrl(query)
                                viewModel.updateCurrentTabUrl(finalUrl)
                                webViewRef?.loadUrl(finalUrl)
                            },
                            onClearHistory = { viewModel.clearHistory() },
                            onShowTabs = { showTabsScreen = true },
                            tabsCount = tabs.size,
                            onNavigateBack = onNavigateBack
                        )
                    } else {
                        AndroidView(
                            update = { webView ->
                                webViewRef = webView
                                val currentUrl = webView.url ?: ""
                                val lastLoadedUrl = webView.getTag(android.R.id.text1) as? String ?: ""
                                if (activeTab.url.isNotEmpty() && activeTab.url != lastLoadedUrl && activeTab.url != currentUrl) {
                                    webView.setTag(android.R.id.text1, activeTab.url)
                                    webView.loadUrl(activeTab.url)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewRef = this
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        allowFileAccess = false
                                        allowContentAccess = false
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    }



                                    webViewClient = SecureWebViewClient(

                                        onPageStarted = { url ->
                                            if (url != "about:blank") {
                                                urlInput = url
                                                viewModel.updateCurrentTabUrl(url)
                                            }
                                            errorMessage = null
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

                                    // Ensure web view tag matches loaded URL initially
                                    setTag(android.R.id.text1, activeTab.url)
                                    loadUrl(activeTab.url)
                                }
                            }
                        )
                    }
                }
            }

            // Error Overlay
            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.Red.copy(alpha = 0.8f)).padding(8.dp).align(Alignment.TopCenter)
                ) {
                    Text("Connection Error: $msg", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Dialogs
        if (showBookmarksDialog) {
            SimpleListDialog("Bookmarks", bookmarks.map { it.title to it.url },
                onDismiss = { showBookmarksDialog = false },
                onSelect = { url ->
                    viewModel.updateCurrentTabUrl(url)
                    webViewRef?.loadUrl(url)
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
                    webViewRef?.loadUrl(url)
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
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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

        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlInputChange,
            placeholder = { Text("Search or type web address", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            trailingIcon = {
                IconButton(onClick = { onSearch(urlInput) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSearch(urlInput) })
        )

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
