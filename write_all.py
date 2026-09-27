"""Master generator for all Pocketmind module files."""
import os, sys
B = r"e:\Projects\pocketmind"
def w(path, content):
    fp = os.path.join(B, path.replace("/", os.sep))
    os.makedirs(os.path.dirname(fp), exist_ok=True)
    with open(fp, "w", encoding="utf-8") as f:
        f.write(content)
    print("Written:", path)

# Files will be imported from write_part*.py
