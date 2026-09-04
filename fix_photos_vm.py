import re

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'r') as f:
    content = f.read()

# Fix stray brackets and duplicate block
content = content.replace("""                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }""", """                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }""")

with open('app/src/main/java/com/example/vaultcalc/ui/photos/PhotosViewModel.kt', 'w') as f:
    f.write(content)
