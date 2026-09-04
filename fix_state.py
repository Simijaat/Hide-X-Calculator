with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""    val isSettingNewPin: Boolean = false,

)""",
"""    val isSettingNewPin: Boolean = false
)""")

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)
