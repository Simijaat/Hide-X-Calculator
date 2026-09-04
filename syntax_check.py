import sys

with open('app/src/main/java/com/example/vaultcalc/ui/calculator/CalculatorViewModel.kt', 'r') as f:
    content = f.read()

brace_count = 0
for i, char in enumerate(content):
    if char == '{':
        brace_count += 1
    elif char == '}':
        brace_count -= 1

    if brace_count < 0:
        print(f"Extra closing brace at position {i}")
        break

if brace_count > 0:
    print(f"Missing {brace_count} closing braces")
elif brace_count == 0:
    print("Braces match")
