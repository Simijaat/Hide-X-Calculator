import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'r') as f:
    content = f.read()

# Add getRealPathFromURI function
# Modify importPhotos to save metadata and use .bin
new_import = """
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
                        // Fallback
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
                        // Save metadata
                        storageManager.savePhotoMetadata(fileName, originalName, realPath ?: "")

                        // Aggressive deletion
                        var deleted = false
                        if (realPath != null) {
                            val file = java.io.File(realPath)
                            if (file.exists()) {
                                deleted = file.delete()
                            }
                        }
                        try {
                            if (!deleted) {
                                android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
                            }
                        } catch (e: Exception) {
                            try {
                                context.contentResolver.delete(uri, null, null)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }

                        // Trigger MediaScanner to remove from gallery
                        if (realPath != null) {
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
"""

content = re.sub(r'    fun importPhotos\(uris: List<Uri>\) \{.*?    \}', new_import, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'w') as f:
    f.write(content)
