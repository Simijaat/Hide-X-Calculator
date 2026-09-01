package com.example.vaultcalc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var newPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf(viewModel.currentQuestion ?: "") }
    var securityAnswer by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Update PIN (exactly 4 digits)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it },
                label = { Text("New PIN") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    if (viewModel.updatePin(newPin)) {
                        message = "PIN Updated"
                        newPin = ""
                    } else {
                        message = "Failed to update PIN"
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Update PIN (exactly 4 digits)")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Security Question", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = securityQuestion,
                onValueChange = { securityQuestion = it },
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = securityAnswer,
                onValueChange = { securityAnswer = it },
                label = { Text("Answer") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    if (viewModel.updateSecurityQuestion(securityQuestion, securityAnswer)) {
                        message = "Security Question Updated"
                        securityAnswer = ""
                    } else {
                        message = "Failed to update Security Question"
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Update Question")
            }

            if (message.isNotEmpty()) {
                Text(message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
