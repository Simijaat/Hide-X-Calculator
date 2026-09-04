import re

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

# Remove requiresDirectorySelection entirely
content = content.replace("    val requiresDirectorySelection: Boolean = false", "")
content = content.replace("requiresDirectorySelection = false,", "")
content = content.replace("requiresDirectorySelection = !vaultStorageManager.hasDirectorySelected(),", "")
content = content.replace("if (_state.value.requiresDirectorySelection) return", "")
content = content.replace("requiresDirectorySelection = false\n", "")

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'r') as f:
    content = f.read()

# Remove requiresDirectorySelection dialog
content = re.sub(r'    val documentTreeLauncher = rememberLauncherForActivityResult.*?if \(state.requiresDirectorySelection\) \{.*?\n    \}', '', content, flags=re.DOTALL)
# Remove the unused variable import if any
content = content.replace("    val documentTreeLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.OpenDocumentTree()\n    ) { uri ->\n        viewModel.onDirectorySelected(uri)\n    }\n", "")


with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'w') as f:
    f.write(content)
