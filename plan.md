1. **Analyze Requirements**: 
The user wants to implement everything described in `WORK.txt` as done, but which isn't actually in the repo.
I reviewed `WORK.txt` from PR12 & PR13:
- Browser Default search engine to Google (PR12).
- SettingsScreen & SettingsViewModel (PR12) - to manage PIN and Security Question.
- NotesScreen & NotesViewModel (PR12) - encrypted notes app.
- `MANAGE_EXTERNAL_STORAGE` permission (PR12).
- Intercept `11223344=` in calculator for PIN reset recovery flow.
2. **Implementation**:
- Modified `app/src/main/AndroidManifest.xml` to add `MANAGE_EXTERNAL_STORAGE`.
- Modified `BrowserScreen.kt` to change `homeUrl` to `https://www.google.com`.
- Created `SettingsScreen.kt` and `SettingsViewModel.kt` to allow changing PIN and security question.
- Created `NotesScreen.kt` and `NotesViewModel.kt` using `VaultCryptoManager` to encrypt/decrypt JSON array to `getExternalFilesDir`.
- Modified `CalculatorViewModel.kt` and `CalculatorScreen.kt` to handle entering `11223344` followed by `=`, entering recovery question answer, and setting new PIN.
- Modified `VaultCalcNavigation.kt` and `VaultScreen.kt` to link these new screens.
3. **Validation**:
- Ran `./gradlew clean test lint check assembleDebug`. Fixed a compilation error regarding `VaultCryptoManager` string vs byte array in `NotesViewModel.kt`. Build succeeds.
4. **Pre-commit**: 
- Follow instructions.
5. **Push and PR**:
- Call submit tool to push changes to branch and create a PR.
