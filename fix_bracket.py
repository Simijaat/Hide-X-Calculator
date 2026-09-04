import re

with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'r') as f:
    content = f.read()

# the patch cut out DropdownMenu { without the closing bracket, and also the Box { that wrapped it
# Actually the replacement string added exactly:
"""
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(Color(0xFF2C2C2C))
                                ) {
"""

# BUT it also wrapped the Row in a Box. Let's append an extra } right before the end of the `if (activeTab != null && activeTab.url.isEmpty())`'s `else` block top bar Row/Box section.

# Wait, let's just add an extra '}' right at the end of the file or at the end of the function if it's simpler,
# or just append a '}' before `if (activeTab?.isLoading == true) {`

target = "if (activeTab?.isLoading == true) {"

if target in content:
    content = content.replace(target, "} " + target, 1)

with open('app/src/main/java/com/example/vaultcalc/ui/browser/BrowserScreen.kt', 'w') as f:
    f.write(content)
