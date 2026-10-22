#!/usr/bin/env python3
import os
import sys
import json

def check_file(path, label):
    if os.path.exists(path):
        print(f"[✓ PASS] Found {label}: {path}")
        return True
    else:
        print(f"[✗ FAIL] Missing {label}: {path}")
        return False

def validate():
    print("=" * 70)
    print("AETHERVAULT MULTI-STORE DISTRIBUTION VALIDATOR")
    print("=" * 70)

    all_passed = True

    # Check Required Docs
    docs = [
        ("docs/DISTRIBUTION_READINESS.md", "Distribution Readiness Matrix"),
        ("docs/STORE_REQUIREMENTS_SOURCES.md", "Store Requirements Policy Sources"),
        ("docs/PRIVACY_AND_DATA_HANDLING.md", "Privacy & Data Handling Spec"),
        ("docs/RELEASE_PROCESS.md", "Release Process Guide"),
        ("docs/SIGNING_AND_KEY_MANAGEMENT.md", "Signing & Keystore Guide")
    ]
    for path, label in docs:
        if not check_file(path, label):
            all_passed = False

    # Check Common Metadata
    short_desc_path = "distribution/metadata/common/short-description.txt"
    if check_file(short_desc_path, "Short Description"):
        with open(short_desc_path, "r", encoding="utf-8") as f:
            content = f.read().strip()
            length = len(content)
            if length <= 80:
                print(f"  └─ Length ({length} chars) <= 80 chars [Google Play Compliant]")
            else:
                print(f"  └─ [EXCEEDED] Length ({length} chars) > 80 chars!")
                all_passed = False

    full_desc_path = "distribution/metadata/common/full-description.md"
    if check_file(full_desc_path, "Full Description"):
        with open(full_desc_path, "r", encoding="utf-8") as f:
            content = f.read().strip()
            length = len(content)
            if length <= 4000:
                print(f"  └─ Length ({length} chars) <= 4000 chars [Google Play Compliant]")
            else:
                print(f"  └─ [EXCEEDED] Length ({length} chars) > 4000 chars!")
                all_passed = False

    # Check Orion Store Metadata
    orion_meta = "distribution/metadata/orion-store/metadata.json"
    if check_file(orion_meta, "Orion Store Metadata"):
        try:
            with open(orion_meta, "r", encoding="utf-8") as f:
                data = json.load(f)
                if "app_id" in data and "repository" in data:
                    print("  └─ Orion Store metadata format valid")
        except Exception as e:
            print(f"  └─ [ERROR] Invalid JSON: {e}")
            all_passed = False

    # Check Release Artifacts
    apk_path = "app-debug.apk"
    if check_file(apk_path, "Universal Debug APK"):
        size_mb = os.path.getsize(apk_path) / (1024 * 1024)
        print(f"  └─ APK Size: {size_mb:.2f} MB")

    print("=" * 70)
    if all_passed:
        print("[SUCCESS] All distribution compliance checks PASSED!")
        sys.exit(0)
    else:
        print("[FAILURE] Validation errors detected!")
        sys.exit(1)

if __name__ == "__main__":
    validate()
