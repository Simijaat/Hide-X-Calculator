import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'r') as f:
    content = f.read()

# Replace the extra braces
content = content.replace("        }\n    }\n        }\n    }\n        }\n    }\n\n    fun exportPhoto", "        }\n    }\n\n    fun exportPhoto")

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'w') as f:
    f.write(content)
