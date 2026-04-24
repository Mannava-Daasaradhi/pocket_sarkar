#!/usr/bin/env python3
"""
Download Gemma 4 E4B LiteRT-LM model for Pocket Sarkar.

Usage:
    python download_model.py --model e4b
    python download_model.py --model e4b --push-to-device   # adb push after download
"""

import argparse
import hashlib
import os
import subprocess
import sys

MODELS = {
    "e4b": {
        "repo": "litert-community/gemma-4-E4B-it-litert-lm",
        "filename": "gemma-4-E4B-it-litert-lm.litertlm",
        "size_gb": 3.65,
        # SHA256 will be verified if HF provides it; skip check if None
        "sha256": None,
    }
}

DEVICE_PATH = "/sdcard/Android/data/com.pocketsarkar/files/models/"


def download_hf(repo: str, filename: str, dest_dir: str) -> str:
    """Download a file from HuggingFace Hub using huggingface_hub."""
    try:
        from huggingface_hub import hf_hub_download
    except ImportError:
        print("Installing huggingface_hub…")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "huggingface_hub", "tqdm"])
        from huggingface_hub import hf_hub_download

    print(f"\nDownloading {filename} from {repo}…")
    print(f"File size: ~{MODELS['e4b']['size_gb']} GB — this will take a while.\n")

    path = hf_hub_download(
        repo_id=repo,
        filename=filename,
        local_dir=dest_dir,
        local_dir_use_symlinks=False,
    )
    return path


def verify_sha256(filepath: str, expected: str) -> bool:
    print("Verifying SHA256…")
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            sha.update(chunk)
    actual = sha.hexdigest()
    if actual != expected:
        print(f"  FAIL — expected {expected}")
        print(f"         got      {actual}")
        return False
    print("  OK")
    return True


def adb_push(local_path: str):
    print(f"\nPushing to device at {DEVICE_PATH}…")
    subprocess.run(["adb", "shell", f"mkdir -p {DEVICE_PATH}"], check=True)
    subprocess.run(["adb", "push", local_path, DEVICE_PATH], check=True)
    print("Done — model is on device.")


def main():
    parser = argparse.ArgumentParser(description="Download Pocket Sarkar model")
    parser.add_argument("--model", choices=list(MODELS.keys()), default="e4b")
    parser.add_argument(
        "--push-to-device",
        action="store_true",
        help="Push to Android device via adb after download",
    )
    parser.add_argument(
        "--dest",
        default="android/app/src/main/assets/models",
        help="Local destination directory",
    )
    args = parser.parse_args()

    info = MODELS[args.model]
    os.makedirs(args.dest, exist_ok=True)

    dest_path = os.path.join(args.dest, info["filename"])
    if os.path.exists(dest_path):
        size_mb = os.path.getsize(dest_path) / (1024 * 1024)
        print(f"Model already exists at {dest_path} ({size_mb:.0f} MB)")
    else:
        dest_path = download_hf(info["repo"], info["filename"], args.dest)

    if info["sha256"]:
        if not verify_sha256(dest_path, info["sha256"]):
            sys.exit(1)
    else:
        print("(SHA256 checksum not available for this model — skipping)")

    if args.push_to_device:
        adb_push(dest_path)
    else:
        print(f"\nModel saved to: {dest_path}")
        print("\nTo push to Android device/emulator, run:")
        print(f"  adb shell mkdir -p {DEVICE_PATH}")
        print(f"  adb push \"{dest_path}\" {DEVICE_PATH}")


if __name__ == "__main__":
    main()