package com.example.vaultcalc.data.crypto

import android.os.Environment
import org.json.JSONObject
import java.io.File
import android.util.Base64

object VaultStorageManager {
    private val vaultDir: File by lazy {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docs, ".VaultCalc")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val configFile: File
        get() = File(vaultDir, "config.json")

    fun getNotesDir(): File {
        val dir = File(vaultDir, "Notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hasConfig(): Boolean = configFile.exists()

    fun saveConfig(
        pinSalt: ByteArray,
        recoverySalt: ByteArray,
        encryptedMasterKeyWithPin: ByteArray,
        pinIv: ByteArray,
        encryptedMasterKeyWithRecovery: ByteArray,
        recoveryIv: ByteArray,
        securityQuestion: String,
        pinHash: ByteArray // Added for quick pin verification without full master key decryption (same as before)
    ) {
        val json = JSONObject().apply {
            put("pinSalt", Base64.encodeToString(pinSalt, Base64.NO_WRAP))
            put("recoverySalt", Base64.encodeToString(recoverySalt, Base64.NO_WRAP))
            put("encryptedMasterKeyWithPin", Base64.encodeToString(encryptedMasterKeyWithPin, Base64.NO_WRAP))
            put("pinIv", Base64.encodeToString(pinIv, Base64.NO_WRAP))
            put("encryptedMasterKeyWithRecovery", Base64.encodeToString(encryptedMasterKeyWithRecovery, Base64.NO_WRAP))
            put("recoveryIv", Base64.encodeToString(recoveryIv, Base64.NO_WRAP))
            put("securityQuestion", securityQuestion)
            put("pinHash", Base64.encodeToString(pinHash, Base64.NO_WRAP))
        }
        configFile.writeText(json.toString())
    }

    fun loadConfig(): VaultConfig? {
        if (!configFile.exists()) return null
        return try {
            val json = JSONObject(configFile.readText())
            VaultConfig(
                pinSalt = Base64.decode(json.getString("pinSalt"), Base64.NO_WRAP),
                recoverySalt = Base64.decode(json.getString("recoverySalt"), Base64.NO_WRAP),
                encryptedMasterKeyWithPin = Base64.decode(json.getString("encryptedMasterKeyWithPin"), Base64.NO_WRAP),
                pinIv = Base64.decode(json.getString("pinIv"), Base64.NO_WRAP),
                encryptedMasterKeyWithRecovery = Base64.decode(json.getString("encryptedMasterKeyWithRecovery"), Base64.NO_WRAP),
                recoveryIv = Base64.decode(json.getString("recoveryIv"), Base64.NO_WRAP),
                securityQuestion = json.getString("securityQuestion"),
                pinHash = Base64.decode(json.getString("pinHash"), Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class VaultConfig(
    val pinSalt: ByteArray,
    val recoverySalt: ByteArray,
    val encryptedMasterKeyWithPin: ByteArray,
    val pinIv: ByteArray,
    val encryptedMasterKeyWithRecovery: ByteArray,
    val recoveryIv: ByteArray,
    val securityQuestion: String,
    val pinHash: ByteArray
)
