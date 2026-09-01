package com.example.vaultcalc.ui.settings

import androidx.lifecycle.ViewModel
import com.example.vaultcalc.data.security.VaultSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityManager: VaultSecurityManager
) : ViewModel() {

    val currentQuestion = securityManager.getSecurityQuestion()

    fun updatePin(newPin: String): Boolean {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
             return securityManager.changePinWithRecovery(newPin)
        }
        return false
    }

    fun updateSecurityQuestion(question: String, answer: String): Boolean {
        if (question.isNotBlank() && answer.isNotBlank()) {
            return securityManager.changeSecurityQuestion(question, answer)
        }
        return false
    }
}
