1. **Restore Full SAF Logic**: I need to bring back the exact `VaultStorageManager` that uses `DocumentFile`, `SharedPreferences` to save the selected URI, and takes the `Context`. The user explicitly wants the "Folder Select" prompt to appear on a fresh install and the password logic to work perfectly.
2. **Setup PIN Flow**: Ensure `isPinSet` reflects whether the config actually exists, and that `requiresDirectorySelection` accurately gates the entire calculator logic until a folder is picked. Ensure saving a PIN directly writes it securely using PBKDF2 to the selected folder as a `.vdata` hidden file.
3. Validate with `./gradlew clean check assembleDebug`.
4. Pre-commit & push to Main.
