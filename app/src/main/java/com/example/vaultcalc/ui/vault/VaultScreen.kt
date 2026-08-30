package com.example.vaultcalc.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vaultcalc.ui.theme.AppBlack

data class VaultApp(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToVpn: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToAudio: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Vault", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock & Go Back", tint = Color.White)
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

        val apps = listOf(
            VaultApp("Browser", Icons.Default.Search, Color(0xFF1E88E5), onNavigateToBrowser),
            VaultApp("VPN", Icons.Default.Settings, Color(0xFF43A047), onNavigateToVpn),
            VaultApp("Downloads", Icons.Default.KeyboardArrowDown, Color(0xFF8E24AA), onNavigateToDownloads),
            VaultApp("Notes", Icons.Default.Edit, Color(0xFFFBC02D), onNavigateToNotes),
            VaultApp("Photos", Icons.Default.AccountBox, Color(0xFFE53935), onNavigateToPhotos),
            VaultApp("Videos", Icons.Default.PlayArrow, Color(0xFF3949AB), onNavigateToVideos),
            VaultApp("Documents", Icons.Default.List, Color(0xFF00ACC1), onNavigateToDocuments),
            VaultApp("Audio", Icons.Default.Notifications, Color(0xFFFF7043), onNavigateToAudio)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(apps) { app ->
                VaultAppIcon(app)
            }
        }
    }
}

@Composable
fun VaultAppIcon(app: VaultApp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { app.onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(brush = Brush.verticalGradient(colors = listOf(app.color.copy(alpha = 0.6f), app.color)))
        ) {
            Icon(
                imageVector = app.icon,
                contentDescription = app.title,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
