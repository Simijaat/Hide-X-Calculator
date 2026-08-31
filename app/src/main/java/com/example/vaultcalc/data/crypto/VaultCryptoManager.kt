package com.example.vaultcalc.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoManager {
    private const val ALGORITHM_AES = "AES"
    private const val CIPHER_AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATIONS = 100000
    private const val KDF_KEY_LENGTH = 256

    fun generateRandomSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun generateMasterKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(ALGORITHM_AES)
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, KDF_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val secretBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretBytes, ALGORITHM_AES)
    }

    fun encryptMasterKey(masterKey: SecretKey, derivedKey: SecretKey): Pair<ByteArray, ByteArray> {
        return encryptData(masterKey.encoded, derivedKey)
    }

    fun decryptMasterKey(encryptedMasterKey: ByteArray, iv: ByteArray, derivedKey: SecretKey): SecretKey {
        val decryptedBytes = decryptData(encryptedMasterKey, iv, derivedKey)
        return SecretKeySpec(decryptedBytes, ALGORITHM_AES)
    }

    fun encryptData(data: ByteArray, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(CIPHER_AES_GCM)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encryptedData = cipher.doFinal(data)
        return Pair(encryptedData, iv)
    }

    fun decryptData(encryptedData: ByteArray, iv: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encryptedData)
    }
}
