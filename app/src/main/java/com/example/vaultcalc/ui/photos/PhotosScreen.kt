package com.example.vaultcalc.ui.photos

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.ui.theme.AppBlack
import com.example.vaultcalc.ui.theme.DarkGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedPhoto by remember { mutableStateOf<String?>(null) }


    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotos(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setExpectingExternalActivity(true)
                        photoPickerLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Photos", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBlack,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = AppBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(AppBlack)) {
            if (photos.isEmpty() && !isLoading) {
                Text(
                    "No photos found.\nTap + to add some.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(photos) { photoName ->
                        PhotoThumbnail(
                            fileName = photoName,
                            viewModel = viewModel,
                            onClick = { selectedPhoto = photoName }
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    selectedPhoto?.let { fileName ->
        PhotoViewerOverlay(
            fileName = fileName,
            viewModel = viewModel,
            onClose = { selectedPhoto = null }
        )
    }
}

@Composable
fun PhotoThumbnail(
    fileName: String,
    viewModel: PhotosViewModel,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(fileName) {
        val bmp = viewModel.loadThumbnail(fileName, 300, 300)
        bitmap = bmp?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(DarkGray)
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerOverlay(
    fileName: String,
    viewModel: PhotosViewModel,
    onClose: () -> Unit
) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }


    LaunchedEffect(fileName) {
        val bmp = viewModel.loadThumbnail(fileName, 300, 300)
        bitmap = bmp?.asImageBitmap()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.exportPhoto(fileName)
                            onClose()
                        }) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export (Unhide)", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Full Screen Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator(color = Color.White)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Photo Permanently?") },
                text = { Text("This action cannot be undone. The photo will not go to any recycle bin.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePhoto(fileName)
                        showDeleteConfirm = false
                        onClose()
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
