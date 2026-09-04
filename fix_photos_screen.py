import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosScreen.kt', 'r') as f:
    content = f.read()

# Fix the stray bracket from regex replacement
content = content.replace("    val context = LocalContext.current\n\n    }\n\n    LaunchedEffect(fileName)", "    val context = LocalContext.current\n\n    LaunchedEffect(fileName)")

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosScreen.kt', 'w') as f:
    f.write(content)
