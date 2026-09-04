import re

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

# Lines 62, 63, 64 is:
#    }
#
#    fun cancelRecovery() {

content = content.replace("    }\n\n    fun cancelRecovery() {", "    fun cancelRecovery() {")

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)
