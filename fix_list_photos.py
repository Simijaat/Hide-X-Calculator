import re

with open('app/src/main/java/com/example/vaultcalc/data/crypto/VaultStorageManager.kt', 'r') as f:
    content = f.read()

# Make listPhotos ignore .meta files and .bin properly
list_photos_new = """    fun listPhotos(): List<String> {
        val photosDir = getPhotosDir()
        return photosDir.listFiles()?.mapNotNull { it.name }?.filter { it.endsWith(".bin") || it.endsWith(".vphoto") } ?: emptyList()
    }"""
content = re.sub(r'    fun listPhotos\(\): List<String> \{.*?    \}', list_photos_new, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/vaultcalc/data/crypto/VaultStorageManager.kt', 'w') as f:
    f.write(content)
