import zipfile, os

# Read backup file list from zip
backup_files = set()
with zipfile.ZipFile(r"D:\BeijiXing-AI-backend.zip", "r") as zf:
    for entry in zf.namelist():
        if entry.endswith(".java") and "target" not in entry:
            # Normalize: remove the leading bx-xx/ prefix, use /
            # entry looks like: bx-ai/src/main/java/com/beijixing/...
            backup_files.add(entry.replace("\\", "/"))

print(f"Backup java files (src only): {len(backup_files)}")

# Read current file list
current_files = set()
for root, dirs, files in os.walk(r"D:\BeijiXing-AI\backend"):
    for f in files:
        if f.endswith(".java") and "src\\main\\java" in root:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, r"D:\BeijiXing-AI\backend")
            current_files.add(rel.replace("\\", "/"))

print(f"Current java files (src only): {len(current_files)}")

# Compare
deleted = sorted(backup_files - current_files)
added = sorted(current_files - backup_files)
common = sorted(backup_files & current_files)

print(f"\nCommon files: {len(common)}")
print(f"Deleted from current (in backup but not present): {len(deleted)}")
print(f"Added since backup (present but not in backup): {len(added)}")

if deleted:
    print("\n=== DELETED ===")
    for f in deleted:
        print(f"  - {f}")

if added:
    print("\n=== ADDED ===")
    for f in added:
        print(f"  + {f}")
