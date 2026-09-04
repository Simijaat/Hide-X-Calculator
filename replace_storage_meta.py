import re

with open('app/src/main/java/com/example/vaultcalc/data/crypto/VaultStorageManager.kt', 'r') as f:
    content = f.read()

# Add savePhotoMetadata and getPhotoMetadata
meta_funcs = """
    fun savePhotoMetadata(fileName: String, originalName: String, originalPath: String) {
        val metaFile = File(getPhotosDir(), "$fileName.meta")
        val json = JSONObject().apply {
            put("originalName", originalName)
            put("originalPath", originalPath)
        }
        try {
            FileOutputStream(metaFile).use { stream ->
                stream.write(json.toString().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPhotoMetadata(fileName: String): PhotoMetadata? {
        val metaFile = File(getPhotosDir(), "$fileName.meta")
        if (!metaFile.exists()) return null
        return try {
            val content = FileInputStream(metaFile).bufferedReader().readText()
            val json = JSONObject(content)
            PhotoMetadata(
                originalName = json.getString("originalName"),
                originalPath = json.getString("originalPath")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhotoMetadata(fileName: String) {
        val metaFile = File(getPhotosDir(), "$fileName.meta")
        if (metaFile.exists()) metaFile.delete()
    }
"""

# Insert before class end
content = content.replace("    fun deletePhoto(fileName: String): Boolean {\n        val file = File(getPhotosDir(), fileName)\n        return file.delete()\n    }\n}", "    fun deletePhoto(fileName: String): Boolean {\n        val file = File(getPhotosDir(), fileName)\n        return file.delete()\n    }\n" + meta_funcs + "\n}")

# Add data class PhotoMetadata
content += "\n\ndata class PhotoMetadata(val originalName: String, val originalPath: String)"

with open('app/src/main/java/com/example/vaultcalc/data/crypto/VaultStorageManager.kt', 'w') as f:
    f.write(content)
