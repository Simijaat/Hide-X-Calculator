package com.example.vaultcalc.data.security

import android.content.Context
import com.example.vaultcalc.data.crypto.VaultCryptoManager
import com.example.vaultcalc.data.crypto.VaultStorageManager
import javax.crypto.SecretKey

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
    @ApplicationContext private val context: Context,
    private val vaultStorageManager: com.example.vaultcalc.data.crypto.VaultStorageManager
) {
    private val PREF_NAME = "secure_vault_prefs"
    private val KEY_PIN_HASH = "vault_pin_hash"
    private val KEY_PIN_SALT = "vault_pin_salt"

    // Security constants
    private val ITERATIONS = 5000
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

    // We store the master key in memory once unlocked
    var activeMasterKey: SecretKey? = null
        private set

    var isExpectingExternalActivity = false

    fun isPinSet(): Boolean {
        return vaultStorageManager.hasConfig() || (sharedPreferences.contains(KEY_PIN_HASH) && sharedPreferences.contains(KEY_PIN_SALT))
    }

    fun setPinAndRecovery(pin: String, question: String, answer: String) {
        val pinSalt = VaultCryptoManager.generateRandomSalt()
        val recoverySalt = VaultCryptoManager.generateRandomSalt()

        val masterKey = activeMasterKey ?: VaultCryptoManager.generateMasterKey()

        val pinDerivedKey = VaultCryptoManager.deriveKey(pin, pinSalt)
        val recoveryDerivedKey = VaultCryptoManager.deriveKey(answer.trim().lowercase(), recoverySalt)

        val (encryptedMasterKeyWithPin, pinIv) = VaultCryptoManager.encryptMasterKey(masterKey, pinDerivedKey)
        val (encryptedMasterKeyWithRecovery, recoveryIv) = VaultCryptoManager.encryptMasterKey(masterKey, recoveryDerivedKey)

        val pinHash = hashPin(pin, pinSalt)

        vaultStorageManager.saveConfig(
            pinSalt = pinSalt,
            recoverySalt = recoverySalt,
            encryptedMasterKeyWithPin = encryptedMasterKeyWithPin,
            pinIv = pinIv,
            encryptedMasterKeyWithRecovery = encryptedMasterKeyWithRecovery,
            recoveryIv = recoveryIv,
            securityQuestion = question,
            pinHash = pinHash
        )

        activeMasterKey = masterKey
        updateActivity()
        _isVaultUnlocked.value = true
    }

    fun setPin(pin: String) {
        setPinAndRecovery(pin, "What is your favorite color?", "blue")
    }

    fun verifyPin(pin: String): Boolean {
        if (!vaultStorageManager.hasConfig() && sharedPreferences.contains(KEY_PIN_HASH)) {
            // Migrate old format to new format
            val storedSalt64 = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return false
            val storedHash64 = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false

            val storedSalt = android.util.Base64.decode(storedSalt64, android.util.Base64.NO_WRAP)
            val storedHash = android.util.Base64.decode(storedHash64, android.util.Base64.NO_WRAP)

            val computedHash = hashPin(pin, storedSalt)

            val isValid = MessageDigest.isEqual(storedHash, computedHash)
            if (isValid) {
                // Pin is correct, generate new master key and save in new format
                setPinAndRecovery(pin, "What is your favorite color?", "blue") // Provide default
                // Clear old prefs to avoid remigration
                sharedPreferences.edit().clear().apply()
                return true
            }
            return false
        }

        val config = vaultStorageManager.loadConfig() ?: return false
        val computedHash = hashPin(pin, config.pinSalt)
        val isValid = MessageDigest.isEqual(config.pinHash, computedHash)

        if (isValid) {
            try {
                val derivedKey = VaultCryptoManager.deriveKey(pin, config.pinSalt)
                activeMasterKey = VaultCryptoManager.decryptMasterKey(config.encryptedMasterKeyWithPin, config.pinIv, derivedKey)
                updateActivity()
                _isVaultUnlocked.value = true
                return true
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    fun getSecurityQuestion(): String? {
        return vaultStorageManager.loadConfig()?.securityQuestion
    }

    fun verifyRecoveryAnswer(answer: String): Boolean {
        val config = vaultStorageManager.loadConfig() ?: return false
        try {
            val derivedKey = VaultCryptoManager.deriveKey(answer.trim().lowercase(), config.recoverySalt)
            activeMasterKey = VaultCryptoManager.decryptMasterKey(config.encryptedMasterKeyWithRecovery, config.recoveryIv, derivedKey)
            // Valid if decryption succeeds without throwing AEADBadTagException
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun changePinWithRecovery(newPin: String): Boolean {
        val config = vaultStorageManager.loadConfig() ?: return false
        if (activeMasterKey == null) return false
        // We preserve the existing recovery answer/question, re-derive with new pin.
        val pinSalt = VaultCryptoManager.generateRandomSalt()
        val pinDerivedKey = VaultCryptoManager.deriveKey(newPin, pinSalt)
        val (encryptedMasterKeyWithPin, pinIv) = VaultCryptoManager.encryptMasterKey(activeMasterKey!!, pinDerivedKey)
        val pinHash = hashPin(newPin, pinSalt)

        vaultStorageManager.saveConfig(
            pinSalt = pinSalt,
            recoverySalt = config.recoverySalt,
            encryptedMasterKeyWithPin = encryptedMasterKeyWithPin,
            pinIv = pinIv,
            encryptedMasterKeyWithRecovery = config.encryptedMasterKeyWithRecovery,
            recoveryIv = config.recoveryIv,
            securityQuestion = config.securityQuestion,
            pinHash = pinHash
        )
        return true
    }

    fun changeSecurityQuestion(newQuestion: String, newAnswer: String): Boolean {
        val config = vaultStorageManager.loadConfig() ?: return false
        if (activeMasterKey == null) return false

        val recoverySalt = VaultCryptoManager.generateRandomSalt()
        val recoveryDerivedKey = VaultCryptoManager.deriveKey(newAnswer.trim().lowercase(), recoverySalt)
        val (encryptedMasterKeyWithRecovery, recoveryIv) = VaultCryptoManager.encryptMasterKey(activeMasterKey!!, recoveryDerivedKey)

        vaultStorageManager.saveConfig(
            pinSalt = config.pinSalt,
            recoverySalt = recoverySalt,
            encryptedMasterKeyWithPin = config.encryptedMasterKeyWithPin,
            pinIv = config.pinIv,
            encryptedMasterKeyWithRecovery = encryptedMasterKeyWithRecovery,
            recoveryIv = recoveryIv,
            securityQuestion = newQuestion,
            pinHash = config.pinHash
        )
        return true
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun encryptPhoto(data: ByteArray): ByteArray? {
        val key = activeMasterKey ?: return null
        val (encryptedData, iv) = VaultCryptoManager.encryptData(data, key)
        // Prepend IV to encrypted data so we can decrypt it later
        return iv + encryptedData
    }

    fun decryptPhoto(encryptedDataWithIv: ByteArray): ByteArray? {
        val key = activeMasterKey ?: return null
        if (encryptedDataWithIv.size <= 12) return null
        val iv = encryptedDataWithIv.copyOfRange(0, 12)
        val encryptedData = encryptedDataWithIv.copyOfRange(12, encryptedDataWithIv.size)
        return try {
            VaultCryptoManager.decryptData(encryptedData, iv, key)
        } catch (e: Exception) {
            null
        }
    }

    fun lockVault() {
        if (isExpectingExternalActivity) return
        _isVaultUnlocked.value = false
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkInactivity() {
        // Now only used if desired; logic moved to onPause
        if (isVaultUnlocked.value && System.currentTimeMillis() - lastActivityTime > INACTIVITY_TIMEOUT_MS) {
            lockVault()
        }
    }
}
