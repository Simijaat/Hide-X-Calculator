package com.example.vaultcalc.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.security.VaultSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val securityManager: VaultSecurityManager
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private var currentInput = ""
    private var isResult = false
    private var setupPin: String? = null

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.ButtonPress -> {
                handleInput(action.symbol)
            }
        }
    }

    private fun handleInput(symbol: String) {
        when (symbol) {
            "C" -> {
                currentInput = ""
                isResult = false
                setupPin = null
                _state.value = _state.value.copy(displayValue = "0")
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
            "=" -> {
                if (!securityManager.isPinSet() && currentInput.length >= 4 && currentInput.all { it.isDigit() }) {
                    if (setupPin == null) {
                        setupPin = currentInput
                        currentInput = ""
                        _state.value = _state.value.copy(displayValue = "0")
                    } else if (setupPin == currentInput) {
                        securityManager.setPin(currentInput)
                        _state.value = _state.value.copy(navigateToVault = true)
                        currentInput = ""
                        setupPin = null
                    } else {
                        // mismatch, start over
                        setupPin = null
                        currentInput = ""
                        _state.value = _state.value.copy(displayValue = "0")
                    }
                } else {
                    checkVaultAccess()
                    if (!_state.value.navigateToVault) {
                        calculateResult()
                    }
                }
            }
            else -> {
                if (isResult) {
                    if (symbol.all { it.isDigit() } || symbol == ".") {
                        currentInput = symbol
                    } else {
                        currentInput += symbol
                    }
                    isResult = false
                } else {
                    if (_state.value.displayValue == "0" && symbol != ".") {
                        currentInput = symbol
                    } else {
                        currentInput += symbol
                    }
                }
                _state.value = _state.value.copy(displayValue = currentInput)
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
            val formattedResult = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                result.toString()
            }
            currentInput = formattedResult
            isResult = true
            _state.value = _state.value.copy(displayValue = formattedResult)
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

    private fun checkVaultAccess() {
        viewModelScope.launch {
            if (securityManager.isPinSet()) {
                if (securityManager.verifyPin(currentInput)) {
                    _state.value = _state.value.copy(navigateToVault = true)
                    currentInput = ""
                }
            }
        }
    }

    fun onVaultNavigated() {
        _state.value = _state.value.copy(navigateToVault = false, displayValue = "0")
    }
}

data class CalculatorState(
    val displayValue: String = "0",
    val navigateToVault: Boolean = false
)

sealed class CalculatorAction {
    data class ButtonPress(val symbol: String) : CalculatorAction()
}
