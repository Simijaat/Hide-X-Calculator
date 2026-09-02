package com.example.vaultcalc.ui.browser

import android.annotation.SuppressLint
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.data.browser.BrowserTab
import com.example.vaultcalc.data.browser.HistoryItem
import com.example.vaultcalc.download.resolver.DownloadOption
import com.example.vaultcalc.download.resolver.YoutubeDLResolver
import com.example.vaultcalc.ui.theme.AppBlack
import kotlinx.coroutines.launch

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
    var showTabsScreen by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val recentHistory by viewModel.recentSearchHistory.collectAsState(initial = emptyList())

    val isBookmarked = bookmarks.any { it.url == activeTab?.url }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDownloadSheet by remember { mutableStateOf(false) }
    var downloadOptions by remember { mutableStateOf<List<DownloadOption>>(emptyList()) }
    var isResolving by remember { mutableStateOf(false) }

    BackHandler(enabled = activeTab != null && activeTab.url.isNotEmpty()) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.updateCurrentTabUrl("")
            urlInput = ""
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
        return
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            if (activeTab != null && activeTab.url.isNotEmpty()) {
                BrowserTopBar(
                    urlInput = urlInput,
                    onUrlChange = { urlInput = it },
                    onSearch = { query ->
                        urlInput = formatSearchUrl(query)
                        viewModel.updateCurrentTabUrl(urlInput)
                        showMenu = false
                    },
                    progress = activeTab.progress,
                    isBookmarked = isBookmarked,
                    onBookmarkClick = {
                        if (isBookmarked) viewModel.removeBookmark(activeTab.url)
                        else viewModel.addBookmark(activeTab.url, activeTab.title)
                    }
                )
            }
        },
        bottomBar = {
            if (activeTab != null && activeTab.url.isNotEmpty()) {
                NavigationBar(
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            if (webViewRef?.canGoBack() == true) {
                                webViewRef?.goBack()
                            } else {
                                viewModel.updateCurrentTabUrl("")
                                urlInput = ""
                            }
                        },
                        icon = { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            if (webViewRef?.canGoForward() == true) {
                                webViewRef?.goForward()
                            }
                        },
                        icon = { Icon(Icons.Default.ArrowForward, contentDescription = "Forward") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showTabsScreen = true },
                        icon = {
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
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigateToDownloads(null) },
                        icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showMenu = true },
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab != null && activeTab.url.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            isResolving = true
                            downloadOptions = YoutubeDLResolver.resolve(activeTab.url)
                            isResolving = false
                            if (downloadOptions.isNotEmpty()) {
                                showDownloadSheet = true
                            } else {
                                Toast.makeText(context, "No download options found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = "Download Video", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (activeTab == null || activeTab.url.isEmpty()) {
                BrowserHomePage(
                    recentHistory = recentHistory,
                    urlInput = urlInput,
                    onUrlInputChange = { urlInput = it },
                    onSearch = { query ->
                        urlInput = formatSearchUrl(query)
                        viewModel.updateCurrentTabUrl(urlInput)
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    onShowTabs = { showTabsScreen = true },
                    tabsCount = tabs.size,
                    onNavigateBack = onNavigateBack
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            }
                            webViewClient = com.example.vaultcalc.ui.browser.SecureWebViewClient(
                                onPageStarted = { url -> viewModel.updateCurrentTabUrl(url) },
                                onPageFinished = { url, title ->
                                    viewModel.updateCurrentTabTitle(title ?: "Unknown")
                                    viewModel.addHistory(url, title ?: "Unknown")
                                },
                                onError = { }
                            )
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    viewModel.updateCurrentTabProgress(newProgress)
                                }
                            }
                            webViewRef = this
                            loadUrl(activeTab.url)
                        }
                    },
                    update = { view ->
                        if (view.url != activeTab.url) {
                            view.loadUrl(activeTab.url)
                        }
                    }
                )
            }

            if (showMenu) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2C)).align(Alignment.TopEnd)
                ) {
                    DropdownMenuItem(
                        text = { Text("Bookmarks", color = Color.White) },
                        onClick = {
                            showMenu = false
                            showBookmarksDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("History", color = Color.White) },
                        onClick = {
                            showMenu = false
                            showHistoryDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Downloads", color = Color.White) },
                        onClick = {
                            showMenu = false
                            onNavigateToDownloads(null)
                        }
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

        if (showDownloadSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDownloadSheet = false },
                containerColor = Color(0xFF1E1E1E)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Select Download Quality", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(downloadOptions) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.enqueueDownload(option, activeTab!!.url)
                                        showDownloadSheet = false
                                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option.quality, color = Color.White)
                                Text("${option.sizeBytes / (1024 * 1024)} MB", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserTopBar(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    progress: Int,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Secure", tint = Color.Green, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSearch(urlInput) }),
                trailingIcon = {
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            if (isBookmarked) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color.Yellow else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
        if (progress < 100) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color.Blue,
                trackColor = Color.Transparent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHomePage(
    recentHistory: List<HistoryItem>,
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
