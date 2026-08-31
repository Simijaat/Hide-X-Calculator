import re

with open('app/src/main/java/com/example/vaultcalc/ui/notes/NotesViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val decryptedJson = VaultCryptoManager.decryptData(cipherText, iv, securityManager.activeMasterKey!!)',
    'val decryptedJson = String(VaultCryptoManager.decryptData(cipherText, iv, securityManager.activeMasterKey!!))'
)
content = content.replace(
    'val (cipherText, iv) = VaultCryptoManager.encryptData(jsonArray.toString(), securityManager.activeMasterKey!!)',
    'val (cipherText, iv) = VaultCryptoManager.encryptData(jsonArray.toString().toByteArray(), securityManager.activeMasterKey!!)'
)

with open('app/src/main/java/com/example/vaultcalc/ui/notes/NotesViewModel.kt', 'w') as f:
    f.write(content)
