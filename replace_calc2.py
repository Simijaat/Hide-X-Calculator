with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "private val _state = MutableStateFlow(CalculatorState(isPinSet = securityManager.isPinSet(), requiresDirectorySelection = !vaultStorageManager.hasDirectorySelected()))",
    "private val _state = MutableStateFlow(CalculatorState(isPinSet = securityManager.isPinSet()))"
)

# Remove onDirectorySelected function entirely
import re
content = re.sub(r'    fun onDirectorySelected\(uri: android\.net\.Uri\?\) \{.*?    \}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)
