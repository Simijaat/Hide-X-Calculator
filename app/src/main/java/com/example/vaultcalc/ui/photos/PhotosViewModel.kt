package com.example.vaultcalc.ui.photos

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.crypto.VaultStorageManager
import com.example.vaultcalc.data.security.VaultSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: VaultStorageManager,
    private val securityManager: VaultSecurityManager
) : ViewModel() {

    private val _photos = MutableStateFlow<List<String>>(emptyList())
    val photos: StateFlow<List<String>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _photos.value = storageManager.listPhotos()
        }
    }



    fun importPhotos(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            for (uri in uris) {
                try {
                    var success = false
                    val realPath = getRealPathFromURI(context, uri)
                    var originalName = "unknown.jpg"
                    if (realPath != null) {
                        originalName = java.io.File(realPath).name
                    } else {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    originalName = cursor.getString(nameIndex)
                                }
                            }
                        }
                    }

                    val fileName = "IMG_${UUID.randomUUID()}.bin"

                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val encryptedBytes = securityManager.encryptPhoto(bytes)
                        if (encryptedBytes != null) {
                            storageManager.savePhoto(fileName, encryptedBytes)
                            success = true
                        }
                    }
                    if (success) {
                        storageManager.savePhotoMetadata(fileName, originalName, realPath ?: "")

                        var deleted = false
                        try {
                            val rows = context.contentResolver.delete(uri, null, null)
                            if (rows > 0) deleted = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        try {
                            if (!deleted) {
                                deleted = android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (realPath != null) {
                            val file = java.io.File(realPath)
                            try {
                                if (file.exists()) file.delete()
                            } catch (e: Exception) {}
                            android.media.MediaScannerConnection.scanFile(context, arrayOf(realPath), null, null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _photos.value = storageManager.listPhotos()
            _isLoading.value = false
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: android.database.Cursor? = null
        try {
            val proj = arrayOf(android.provider.MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }
    fun decryptPhoto(fileName: String): ByteArray? {
        val encryptedData = storageManager.readPhoto(fileName) ?: return null
        return securityManager.decryptPhoto(encryptedData)
    }

    fun deletePhoto(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = storageManager.deletePhoto(fileName)
            if (success) {
                storageManager.deletePhotoMetadata(fileName)
                _photos.value = storageManager.listPhotos()
            }
        }
    }

    fun exportPhoto(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val decryptedBytes = decryptPhoto(fileName)
                val meta = storageManager.getPhotoMetadata(fileName)
                if (decryptedBytes != null && meta != null && meta.originalPath.isNotEmpty()) {
                    val destFile = java.io.File(meta.originalPath)

                    // Recreate directory if it doesn't exist
                    destFile.parentFile?.mkdirs()

                    java.io.FileOutputStream(destFile).use { stream ->
                        stream.write(decryptedBytes)
                    }

                    android.media.MediaScannerConnection.scanFile(context, arrayOf(meta.originalPath), null, null)

                    storageManager.deletePhoto(fileName)
                    storageManager.deletePhotoMetadata(fileName)
                    _photos.value = storageManager.listPhotos()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    suspend fun loadThumbnail(fileName: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        val bytes = decryptPhoto(fileName) ?: return@withContext null
        decodeSampledBitmapFromByteArray(bytes, reqWidth, reqHeight)
    }

    suspend fun loadFullImage(fileName: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        val bytes = decryptPhoto(fileName) ?: return@withContext null
        decodeSampledBitmapFromByteArray(bytes, 2048, 2048)
    }

    fun setExpectingExternalActivity(expecting: Boolean) {
        securityManager.isExpectingExternalActivity = expecting
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
