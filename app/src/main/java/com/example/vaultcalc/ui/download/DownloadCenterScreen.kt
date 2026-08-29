package com.example.vaultcalc.ui.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.data.download.DownloadState
import com.example.vaultcalc.data.download.DownloadTask
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCenterScreen(
    onNavigateBack: () -> Unit,
    sharedUrlFlow: StateFlow<String?>?,
    onSharedUrlHandled: () -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val downloads by viewModel.allDownloads.collectAsState(initial = emptyList())
    val isResolving by viewModel.isResolving.collectAsState()
    val resolvedOptions by viewModel.resolvedOptions.collectAsState()

    // Check if there is a shared URL when entering the screen
    LaunchedEffect(sharedUrlFlow) {
        sharedUrlFlow?.collect { url ->
            if (!url.isNullOrBlank()) {
                viewModel.resolveUrl(url)
                onSharedUrlHandled()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Center") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // URL Input Section
            var urlInput by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Paste URL here") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.resolveUrl(urlInput)
                            urlInput = ""
                        }
                    },
                    enabled = !isResolving && urlInput.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            Divider()

            // List Section
            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active downloads", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(downloads, key = { it.id }) { task ->
                        DownloadItem(
                            task = task,
                            onPause = { viewModel.pauseDownload(task.id) },
                            onResume = { viewModel.resumeDownload(task.id) },
                            onCancel = { viewModel.cancelDownload(task.id) },
                            onDelete = { viewModel.deleteDownload(task) },
                            onExport = { viewModel.exportDownload(task) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Resolver Dialog
        if (isResolving || resolvedOptions.isNotEmpty()) {
            ResolverDialog(
                options = resolvedOptions,
                isResolving = isResolving,
                onDismiss = { viewModel.clearResolvedOptions() },
                onOptionSelected = { option ->
                    viewModel.startDownload(option)
                }
            )
        }
    }
}

@Composable
fun DownloadItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.fileName,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            val stateText = when (task.state) {
                DownloadState.QUEUED -> "Queued"
                DownloadState.RESOLVING -> "Resolving..."
                DownloadState.DOWNLOADING -> "Downloading..."
                DownloadState.PAUSED -> "Paused"
                DownloadState.COMPLETED -> "Completed"
                DownloadState.FAILED -> "Failed: ${task.errorReason ?: "Unknown error"}"
                DownloadState.CANCELLED -> "Cancelled"
            }

            Text(
                text = stateText,
                style = MaterialTheme.typography.bodySmall,
                color = when (task.state) {
                    DownloadState.FAILED -> MaterialTheme.colorScheme.error
                    DownloadState.COMPLETED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PAUSED) {
                val progress = if (task.totalBytes > 0) {
                    task.downloadedBytes.toFloat() / task.totalBytes.toFloat()
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                val downloadedMb = task.downloadedBytes / (1024.0 * 1024.0)
                val totalMb = if (task.totalBytes > 0) task.totalBytes / (1024.0 * 1024.0) else 0.0
                val progressText = if (totalMb > 0) {
                    String.format("%.2f / %.2f MB", downloadedMb, totalMb)
                } else {
                    String.format("%.2f MB", downloadedMb)
                }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (task.state) {
                    DownloadState.DOWNLOADING -> {
                        IconButton(onClick = onPause) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, "Pause") // Quick fix, Pause is not in Default
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                    DownloadState.PAUSED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, "Resume")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                    DownloadState.COMPLETED -> {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.Share, "Export")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                    else -> {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }
}
