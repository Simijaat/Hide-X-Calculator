package com.example.vaultcalc.data.crypto

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("vault_storage_prefs", Context.MODE_PRIVATE)

    var vaultUri: Uri?
        get() {
            val uriString = prefs.getString("vault_uri", null)
            return if (uriString != null) Uri.parse(uriString) else null
        }
        set(value) {
            prefs.edit().putString("vault_uri", value?.toString()).apply()
            value?.let { uri ->
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                    val dir = DocumentFile.fromTreeUri(context, uri)
                    if (dir != null && dir.findFile(".nomedia") == null) {
                        dir.createFile("*/*", ".nomedia")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    fun hasDirectorySelected(): Boolean = vaultUri != null

    private fun getConfigFile(): DocumentFile? {
        val uri = vaultUri ?: return null
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return null
        var file = dir.findFile("config.vdata")
        if (file == null) {
            file = dir.createFile("application/octet-stream", "config.vdata")
        }
        return file
    }

    fun hasConfig(): Boolean {
        val uri = vaultUri ?: return false
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return false
        return dir.findFile("config.vdata") != null
    }

    fun saveConfig(
        pinSalt: ByteArray,
        recoverySalt: ByteArray,
        encryptedMasterKeyWithPin: ByteArray,
        pinIv: ByteArray,
        encryptedMasterKeyWithRecovery: ByteArray,
        recoveryIv: ByteArray,
        securityQuestion: String,
        pinHash: ByteArray
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

        val file = getConfigFile() ?: return
        try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                val obfuscated = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                stream.write(obfuscated.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadConfig(): VaultConfig? {
        val uri = vaultUri ?: return null
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return null
        val file = dir.findFile("config.vdata") ?: return null

        return try {
            val content = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return null

            val decodedJson = String(Base64.decode(content, Base64.NO_WRAP))
            val json = JSONObject(decodedJson)
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

    fun writeDataToFile(fileName: String, data: ByteArray) {
        val uri = vaultUri ?: return
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return
        var file = dir.findFile(fileName)
        if (file == null) {
            file = dir.createFile("application/octet-stream", fileName)
        }
        if (file == null) return
        try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                stream.write(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readDataFromFile(fileName: String): ByteArray? {
        val uri = vaultUri ?: return null
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return null
        val file = dir.findFile(fileName) ?: return null
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fileExists(fileName: String): Boolean {
        val uri = vaultUri ?: return false
        val dir = DocumentFile.fromTreeUri(context, uri) ?: return false
        return dir.findFile(fileName) != null
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
