import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'r') as f:
    content = f.read()

delete_func = """    fun deletePhoto(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = storageManager.deletePhoto(fileName)
            if (success) {
                storageManager.deletePhotoMetadata(fileName)
                _photos.value = storageManager.listPhotos()
            }
        }
    }"""
content = re.sub(r'    fun deletePhoto\(fileName: String\) \{.*?    \}', delete_func, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'w') as f:
    f.write(content)
