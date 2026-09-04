import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'r') as f:
    content = f.read()

# Replace exportPhoto
new_export = """    fun exportPhoto(fileName: String) {
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
    }"""

content = re.sub(r'    fun exportPhoto\(fileName: String, destUri: Uri\) \{.*?    \}', new_export, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosScreen.kt', 'r') as f:
    content = f.read()

# Remove exportLauncher from PhotoViewerOverlay
content = re.sub(r'    val exportLauncher = rememberLauncherForActivityResult\(.*?    \}\n', '', content, flags=re.DOTALL)

# Update IconButton click logic
content = re.sub(r'viewModel.setExpectingExternalActivity\(true\)\s*exportLauncher.launch\("Exported_\$fileName.jpg"\)', 'viewModel.exportPhoto(fileName)\n                            onClose()', content)


with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosScreen.kt', 'w') as f:
    f.write(content)
