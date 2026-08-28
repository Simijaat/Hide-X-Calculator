package com.example.vaultcalc.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PREF_NAME = "secure_vault_prefs"
    private val KEY_PIN_HASH = "vault_pin_hash"
    private val KEY_PIN_SALT = "vault_pin_salt"

    // Security constants
    private val ITERATIONS = 100000
    private val KEY_LENGTH = 256
    private val INACTIVITY_TIMEOUT_MS = 2 * 60 * 1000L // 2 minutes

    private var lastActivityTime = System.currentTimeMillis()

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
        return sharedPreferences.contains(KEY_PIN_HASH) && sharedPreferences.contains(KEY_PIN_SALT)
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPin(pin, salt)

        sharedPreferences.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()

        updateActivity()
        _isVaultUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedSalt64 = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash64 = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false

        val storedSalt = Base64.decode(storedSalt64, Base64.NO_WRAP)
        val storedHash = Base64.decode(storedHash64, Base64.NO_WRAP)

        val computedHash = hashPin(pin, storedSalt)

        val isValid = MessageDigest.isEqual(storedHash, computedHash)
        if (isValid) {
            updateActivity()
            _isVaultUnlocked.value = true
        }
        return isValid
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkInactivity() {
        if (isVaultUnlocked.value && System.currentTimeMillis() - lastActivityTime > INACTIVITY_TIMEOUT_MS) {
            lockVault()
        }
    }
}
