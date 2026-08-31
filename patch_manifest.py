import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

if '<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />' not in content:
    content = content.replace(
        '<uses-permission android:name="android.permission.INTERNET" />',
        '<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />'
    )

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
