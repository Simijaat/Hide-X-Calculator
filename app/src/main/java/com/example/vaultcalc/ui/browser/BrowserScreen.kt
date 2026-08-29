package com.example.vaultcalc.ui.browser

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }


    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(end = 8.dp),
                        singleLine = true,
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
                        ),
                        placeholder = { Text("Search or enter address") }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Browser")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { webViewRef?.goBack() }, enabled = webViewRef?.canGoBack() == true) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                }
                IconButton(onClick = { webViewRef?.goForward() }, enabled = webViewRef?.canGoForward() == true) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go Forward")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (activeTab?.isLoading == true) {
                    IconButton(onClick = { webViewRef?.stopLoading() }) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Loading")
                    }
                } else {
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
                IconButton(onClick = { viewModel.addNewTab() }) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
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
                            // Fetch AdBlockManager safely inside compose via EntryPoint logic or manually inject
                            // For simplicity in AndroidView, we can get it from the application context if we cast,
                            // but actually we should just provide it via Hilt EntryPoint.
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

                                loadUrl(tab.url)
                            }
                        }
                    )
                }
            }
        }
    }
}
