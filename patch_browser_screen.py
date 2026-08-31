import re

with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'r') as f:
    content = f.read()

# Change default search engine to Google
if 'val homeUrl = "https://duckduckgo.com"' in content:
    content = content.replace(
        'val homeUrl = "https://duckduckgo.com"',
        'val homeUrl = "https://www.google.com"'
    )

with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'w') as f:
    f.write(content)
