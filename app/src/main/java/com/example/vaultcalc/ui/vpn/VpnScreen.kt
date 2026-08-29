package com.example.vaultcalc.ui.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.vpn.VpnLocation
import com.example.vaultcalc.vpn.VpnState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnScreen(
    onNavigateBack: () -> Unit,
    viewModel: VpnViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val availableLocations = viewModel.availableLocations

    // Launcher for VPN permission request
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure VPN") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (vpnState) {
                        VpnState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                        VpnState.ERROR -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status: ${vpnState.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Location: ${selectedLocation.displayName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Connect/Disconnect Button
            Button(
                onClick = {
                    when (vpnState) {
                        VpnState.DISCONNECTED, VpnState.ERROR -> {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                            } else {
                                viewModel.connect()
                            }
                        }
                        VpnState.CONNECTED -> {
                            viewModel.disconnect()
                        }
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = vpnState == VpnState.DISCONNECTED || vpnState == VpnState.CONNECTED || vpnState == VpnState.ERROR
            ) {
                Text(
                    text = if (vpnState == VpnState.CONNECTED) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Location List
            Text(
                text = "Available Locations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableLocations) { location ->
                    ListItem(
                        headlineContent = { Text(location.displayName) },
                        trailingContent = {
                            if (location == selectedLocation) {
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        },
                        modifier = Modifier.clickable(
                            enabled = vpnState == VpnState.DISCONNECTED || vpnState == VpnState.ERROR
                        ) {
                            viewModel.selectLocation(location)
                        }
                    )
                    Divider()
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Note: This is a secure local dev tunnel. No real external server is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
