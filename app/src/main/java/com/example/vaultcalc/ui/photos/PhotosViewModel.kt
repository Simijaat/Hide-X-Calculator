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
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val encryptedBytes = securityManager.encryptPhoto(bytes)
                        if (encryptedBytes != null) {
                            val fileName = "IMG_${UUID.randomUUID()}.vphoto"
                            storageManager.savePhoto(fileName, encryptedBytes)
                            success = true
                        }
                    }
                    if (success) {
                        try {
                            android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
                        } catch (e: Exception) {
                            try {
                                context.contentResolver.delete(uri, null, null)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
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

    fun decryptPhoto(fileName: String): ByteArray? {
        val encryptedData = storageManager.readPhoto(fileName) ?: return null
        return securityManager.decryptPhoto(encryptedData)
    }

    fun deletePhoto(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = storageManager.deletePhoto(fileName)
            if (success) {
                _photos.value = storageManager.listPhotos()
            }
        }
    }

    fun exportPhoto(fileName: String, destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val decryptedBytes = decryptPhoto(fileName)
                if (decryptedBytes != null) {
                    context.contentResolver.openOutputStream(destUri)?.use { stream ->
                        stream.write(decryptedBytes)
                    }
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
