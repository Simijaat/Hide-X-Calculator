import re

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'r') as f:
    content = f.read()

if 'RecoveryDialog' not in content:
    content = content.replace(
        '''import androidx.compose.ui.Alignment''',
        '''import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember'''
    )

    content = content.replace(
        '''    val state by viewModel.state.collectAsState()''',
        '''    val state by viewModel.state.collectAsState()
    
    val (recoveryAnswer, setRecoveryAnswer) = remember { mutableStateOf("") }
    val (newPin, setNewPin) = remember { mutableStateOf("") }

    if (state.isRecovering) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRecovery() },
            title = { Text("Security Recovery") },
            text = {
                Column {
                    Text(state.recoveryQuestion)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = setRecoveryAnswer,
                        label = { Text("Answer") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.submitRecoveryAnswer(recoveryAnswer); setRecoveryAnswer("") }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRecovery(); setRecoveryAnswer("") }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.isSettingNewPin) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRecovery() },
            title = { Text("Set New PIN") },
            text = {
                Column {
                    Text("Enter a new PIN (min 4 digits)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = setNewPin,
                        label = { Text("New PIN") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.submitNewPin(newPin); setNewPin("") }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRecovery(); setNewPin("") }) {
                    Text("Cancel")
                }
            }
        )
    }'''
    )

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'w') as f:
    f.write(content)
