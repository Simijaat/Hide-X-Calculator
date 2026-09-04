import re

with open('app/src/main/java/com/example/vaultcalc/data/crypto/VaultStorageManager.kt', 'w') as f:
    f.write("""package com.example.vaultcalc.data.crypto

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vaultDir: File
        get() {
            val dir = File(Environment.getExternalStorageDirectory(), ".SystemConfig/.vault")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val nomedia = File(dir, ".nomedia")
            if (!nomedia.exists()) {
                nomedia.createNewFile()
            }
            return dir
        }

    // Backward compatibility for ViewModel
    var vaultUri: Uri? = null

    fun hasDirectorySelected(): Boolean = true

    private fun getConfigFile(): File {
        val file = File(vaultDir, "config.vdata")
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    fun hasConfig(): Boolean {
        return File(vaultDir, "config.vdata").exists()
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

        val file = getConfigFile()
        try {
            FileOutputStream(file).use { stream ->
                val obfuscated = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                stream.write(obfuscated.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadConfig(): VaultConfig? {
        val file = File(vaultDir, "config.vdata")
        if (!file.exists()) return null

        return try {
            val content = FileInputStream(file).bufferedReader().readText()
            if (content.isEmpty()) return null

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
        val file = File(vaultDir, fileName)
        try {
            FileOutputStream(file).use { stream ->
                stream.write(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readDataFromFile(fileName: String): ByteArray? {
        val file = File(vaultDir, fileName)
        if (!file.exists()) return null
        return try {
            FileInputStream(file).use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fileExists(fileName: String): Boolean {
        return File(vaultDir, fileName).exists()
    }

    // Photo specific methods
    private fun getPhotosDir(): File {
        val dir = File(vaultDir, "Photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listPhotos(): List<String> {
        val photosDir = getPhotosDir()
        return photosDir.listFiles()?.mapNotNull { it.name }?.filter { !it.startsWith(".") } ?: emptyList()
    }

    fun savePhoto(fileName: String, data: ByteArray) {
        val file = File(getPhotosDir(), fileName)
        try {
            FileOutputStream(file).use { stream ->
                stream.write(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPhoto(fileName: String, destUri: Uri): Boolean {
        val file = File(getPhotosDir(), fileName)
        if (!file.exists()) return false
        return try {
            FileInputStream(file).use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPhotoUri(fileName: String): Uri? {
        val file = File(getPhotosDir(), fileName)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun readPhoto(fileName: String): ByteArray? {
        val file = File(getPhotosDir(), fileName)
        if (!file.exists()) return null
        return try {
            FileInputStream(file).use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhoto(fileName: String): Boolean {
        val file = File(getPhotosDir(), fileName)
        return file.delete()
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
""")
