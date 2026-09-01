package com.example.vaultcalc.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.data.crypto.VaultStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.DecimalFormat

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val securityManager: VaultSecurityManager,
    private val vaultStorageManager: com.example.vaultcalc.data.crypto.VaultStorageManager
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState(isPinSet = securityManager.isPinSet(), requiresDirectorySelection = !vaultStorageManager.hasDirectorySelected()))
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private var currentInput = ""
    private var isResult = false
    private var setupPin: String? = null

    init {
        // Initialize state
        _state.value = _state.value.copy(
            isPinSet = securityManager.isPinSet(), requiresDirectorySelection = !vaultStorageManager.hasDirectorySelected(),
            isConfirmingPin = false
        )
    }

    fun submitRecoveryAnswer(answer: String) {
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
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            securityManager.changePinWithRecovery(newPin)
            _state.value = _state.value.copy(
                isSettingNewPin = false,
                displayValue = "0",
                navigateToVault = true,
                isPinSet = true
            )
        }
    }

    fun onDirectorySelected(uri: android.net.Uri?) {
        if (uri != null) {
            vaultStorageManager.vaultUri = uri
            _state.value = _state.value.copy(
                requiresDirectorySelection = false,
                isPinSet = securityManager.isPinSet()
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

    fun onAction(action: CalculatorAction) {
        if (_state.value.requiresDirectorySelection) return

        when (action) {
            is CalculatorAction.ButtonPress -> {
                handleInput(action.symbol)
            }
        }
    }

    private fun handleInput(symbol: String) {
        when (symbol) {
            "AC", "C" -> {
                currentInput = ""
                isResult = false
                setupPin = null
                _state.value = _state.value.copy(displayValue = "0", isConfirmingPin = false)
            }
            "DEL" -> {
                if (isResult) {
                    currentInput = ""
                    isResult = false
                } else if (currentInput.isNotEmpty()) {
                    currentInput = currentInput.dropLast(1)
                }
                _state.value = _state.value.copy(displayValue = if (currentInput.isEmpty()) "0" else currentInput)
            }
            "+/-" -> {
                if (currentInput.isNotEmpty() && currentInput != "0") {
                    currentInput = if (currentInput.startsWith("-")) {
                        currentInput.substring(1)
                    } else {
                        "-$currentInput"
                    }
                    _state.value = _state.value.copy(displayValue = currentInput)
                }
            }
            "%" -> {
                 if (currentInput.isNotEmpty() && currentInput != "0" && currentInput.toDoubleOrNull() != null) {
                     val num = currentInput.toDouble()
                     currentInput = (num / 100).toString()
                     isResult = true
                     _state.value = _state.value.copy(displayValue = currentInput)
                 }
            }
            "=" -> {
                if (currentInput == "11223344") {
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

                if (!securityManager.isPinSet() && currentInput.length == 4 && currentInput.all { it.isDigit() }) {
                    if (setupPin == null) {
                        setupPin = currentInput
                        currentInput = ""
                        _state.value = _state.value.copy(displayValue = "0", isConfirmingPin = true)
                    } else if (setupPin == currentInput) {
                        securityManager.setPin(currentInput)
                        _state.value = _state.value.copy(
                            navigateToVault = true,
                            isPinSet = true,
                            isConfirmingPin = false
                        )
                        currentInput = ""
                        setupPin = null
                    } else {
                        // mismatch, start over
                        setupPin = null
                        currentInput = ""
                        _state.value = _state.value.copy(displayValue = "0", isConfirmingPin = false)
                    }
                } else {
                    val unlocked = checkVaultAccess()
                    if (!unlocked) {
                        calculateResult()
                    }
                }
            }
            else -> {
                // Map symbols back to operators for calculation
                val internalSymbol = when (symbol) {
                    "÷" -> "/"
                    "×" -> "*"
                    "−" -> "-"
                    else -> symbol
                }

                val isOperator = { s: String -> s == "/" || s == "*" || s == "-" || s == "+" }

                if (isResult) {
                    if (internalSymbol.all { it.isDigit() } || internalSymbol == ".") {
                        currentInput = internalSymbol
                    } else {
                        currentInput += internalSymbol
                    }
                    isResult = false
                } else {
                    if (_state.value.displayValue == "0" && internalSymbol != ".") {
                        currentInput = internalSymbol
                    } else if (currentInput.isNotEmpty() && isOperator(currentInput.last().toString()) && isOperator(internalSymbol)) {
                        // Prevent consecutive operators by replacing the last one
                        currentInput = currentInput.dropLast(1) + internalSymbol
                    } else {
                        currentInput += internalSymbol
                    }
                }

                // Display user-friendly string (convert internally stored symbols to display symbols if they are at the end)
                val displayStr = currentInput
                    .replace("/", "÷")
                    .replace("*", "×")
                    .replace("-", "−")

                _state.value = _state.value.copy(displayValue = displayStr)
            }
        }
    }

    private fun calculateResult() {
        try {
            val result = eval(currentInput)
            if (result.isInfinite() || result.isNaN()) {
                throw ArithmeticException("Division by zero")
            }
            // format to remove .0 if it's a whole number
            val df = DecimalFormat("#.##########")
            val formattedResult = df.format(result)
            currentInput = formattedResult
            isResult = true
            _state.value = _state.value.copy(displayValue = formattedResult.replace("-", "−"))
        } catch (e: Exception) {
            _state.value = _state.value.copy(displayValue = "Error")
            currentInput = ""
            isResult = true
        }
    }

    // A simple recursive descent parser to evaluate math expressions with precedence and parentheses
    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }

    private fun checkVaultAccess(): Boolean {
        if (securityManager.isPinSet()) {
            if (securityManager.verifyPin(currentInput)) {
                _state.value = _state.value.copy(navigateToVault = true)
                currentInput = ""
                return true
            }
        }
        return false
    }

    fun onVaultNavigated() {
        _state.value = _state.value.copy(navigateToVault = false, displayValue = "0")
    }
}

data class CalculatorState(
    val displayValue: String = "0",
    val navigateToVault: Boolean = false,
    val isPinSet: Boolean = false,
    val isConfirmingPin: Boolean = false,
    val isRecovering: Boolean = false,
    val recoveryQuestion: String = "",
    val isSettingNewPin: Boolean = false,
    val requiresDirectorySelection: Boolean = false
)

sealed class CalculatorAction {
    data class ButtonPress(val symbol: String) : CalculatorAction()
}
