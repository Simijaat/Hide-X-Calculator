with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'r') as f:
    content = f.read()

if 'import androidx.compose.foundation.text.BasicTextField' not in content:
    content = content.replace('import androidx.compose.foundation.text.KeyboardOptions',
                              'import androidx.compose.foundation.text.BasicTextField\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.graphics.SolidColor')

with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'w') as f:
    f.write(content)
