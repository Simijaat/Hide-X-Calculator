import re

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

# Make it exactly 4 digits as user requested: '4 Digit Ka Hoga Always Password'
content = content.replace(
    'currentInput.length >= 4',
    'currentInput.length == 4'
)

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'Confirm your 4+ digit PIN',
    'Confirm your 4 digit PIN'
)
content = content.replace(
    'Set a 4+ digit PIN',
    'Set a 4 digit PIN'
)

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorScreen.kt', 'w') as f:
    f.write(content)
