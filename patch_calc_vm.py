import re

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

# Add logic for intercepting `11223344=`
if '"11223344"' not in content:
    content = content.replace(
        '''                if (!securityManager.isPinSet() && currentInput.length >= 4 && currentInput.all { it.isDigit() }) {''',
        '''                if (currentInput == "11223344") {
                    val question = securityManager.getSecurityQuestion()
                    if (question != null) {
                        _state.value = _state.value.copy(
                            isRecovering = true,
                            recoveryQuestion = question,
                            displayValue = "0"
                        )
                        currentInput = ""
                        return
                    }
                }
                
                if (!securityManager.isPinSet() && currentInput.length >= 4 && currentInput.all { it.isDigit() }) {'''
    )
    
    content = content.replace(
        '''data class CalculatorState(
    val displayValue: String = "0",
    val navigateToVault: Boolean = false,
    val isPinSet: Boolean = false,
    val isConfirmingPin: Boolean = false
)''',
        '''data class CalculatorState(
    val displayValue: String = "0",
    val navigateToVault: Boolean = false,
    val isPinSet: Boolean = false,
    val isConfirmingPin: Boolean = false,
    val isRecovering: Boolean = false,
    val recoveryQuestion: String = "",
    val isSettingNewPin: Boolean = false
)'''
    )

    content = content.replace(
        '''    fun onAction(action: CalculatorAction) {''',
        '''    fun submitRecoveryAnswer(answer: String) {
        if (securityManager.verifyRecoveryAnswer(answer)) {
            _state.value = _state.value.copy(
                isRecovering = false,
                isSettingNewPin = true
            )
        } else {
            _state.value = _state.value.copy(
                isRecovering = false,
                displayValue = "Error"
            )
        }
    }

    fun submitNewPin(newPin: String) {
        if (newPin.length >= 4) {
            securityManager.changePinWithRecovery(newPin)
            _state.value = _state.value.copy(
                isSettingNewPin = false,
                displayValue = "0",
                navigateToVault = true,
                isPinSet = true
            )
        }
    }

    fun cancelRecovery() {
        _state.value = _state.value.copy(
            isRecovering = false,
            isSettingNewPin = false,
            displayValue = "0"
        )
        currentInput = ""
    }

    fun onAction(action: CalculatorAction) {'''
    )

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'w') as f:
    f.write(content)
