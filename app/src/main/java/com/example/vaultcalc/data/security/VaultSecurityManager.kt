package com.example.vaultcalc.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PREF_NAME = "secure_vault_prefs"
    private val KEY_PIN_HASH = "vault_pin_hash"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH)
    }

    fun setPin(pin: String) {
        // In a production app, we would hash this with bcrypt/argon2
        // For PR1 foundation, using encrypted shared prefs is sufficient
        sharedPreferences.edit().putString(KEY_PIN_HASH, pin).apply()
        _isVaultUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedPin = sharedPreferences.getString(KEY_PIN_HASH, null)
        val isValid = storedPin == pin
        if (isValid) {
            _isVaultUnlocked.value = true
        }
        return isValid
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }
}
