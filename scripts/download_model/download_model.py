#!/usr/bin/env python3
"""
Download Gemma 4 E4B LiteRT-LM model for Pocket Sarkar.

Usage:
    python download_model.py --model e4b
    python download_model.py --model e4b-int4      # alias for e4b (same file)
    python download_model.py --model e4b --push-to-device   # adb push after download

The model file is placed in android/app/src/main/assets/models/ (git-ignored).
For testing on a physical device or emulator, use --push-to-device or run:
    adb shell mkdir -p /sdcard/Android/data/com.pocketsarkar/files/models/
    adb push <model_file> /sdcard/Android/data/com.pocketsarkar/files/models/
"""

import argparse
import hashlib
import os
import subprocess
import sys

MODELS = {
    "e4b": {
        "repo":     "litert-community/gemma-4-E4B-it-litert-lm",
        "filename": "gemma-4-E4B-it-litert-lm.litertlm",
        "size_gb":  3.65,
        # SHA256 will be verified if provided; None = skip check
        "sha256":   None,
    },
    # e4b-int4 is the same HuggingFace model — kept as an alias so callers
    # using the --model e4b-int4 flag from the Phase 3 spec still work.
    "e4b-int4": {
        "repo":     "litert-community/gemma-4-E4B-it-litert-lm",
        "filename": "gemma-4-E4B-it-litert-lm.litertlm",
        "size_gb":  3.65,
        "sha256":   None,
    },
}

DEVICE_PATH = "/sdcard/Android/data/com.pocketsarkar/files/models/"


def ensure_deps():
    """Install huggingface_hub and tqdm if not already present."""
    pkgs = []
    try:
        import huggingface_hub  # noqa: F401
    except ImportError:
        pkgs.append("huggingface_hub")
    try:
        import tqdm  # noqa: F401
    except ImportError:
        pkgs.append("tqdm")
    if pkgs:
        print(f"Installing missing packages: {', '.join(pkgs)} …")
        subprocess.check_call([sys.executable, "-m", "pip", "install", *pkgs])


def download_hf(repo: str, filename: str, dest_dir: str) -> str:
    """Download a file from HuggingFace Hub with tqdm progress bar."""
    from huggingface_hub import hf_hub_download
    from tqdm import tqdm

    print(f"\nDownloading {filename}")
    print(f"  Repo  : {repo}")
    print(f"  Size  : ~{MODELS['e4b']['size_gb']} GB — this will take a while.\n")

    # hf_hub_download prints its own progress if tqdm is installed;
    # we wrap in a tqdm context so the bar shows even for older hub versions.
    with tqdm(unit="B", unit_scale=True, unit_divisor=1024, desc=filename) as _:
        path = hf_hub_download(
            repo_id=repo,
            filename=filename,
            local_dir=dest_dir,
            local_dir_use_symlinks=False,
        )
    return path


def verify_sha256(filepath: str, expected: str) -> bool:
    """Stream-hash the file and compare against the expected digest."""
    from tqdm import tqdm

    print("Verifying SHA256 checksum …")
    sha = hashlib.sha256()
    file_size = os.path.getsize(filepath)
    with open(filepath, "rb") as f:
        with tqdm(total=file_size, unit="B", unit_scale=True, desc="hashing") as bar:
            for chunk in iter(lambda: f.read(65536), b""):
                sha.update(chunk)
                bar.update(len(chunk))
    actual = sha.hexdigest()
    if actual != expected:
        print(f"  FAIL — expected {expected}")
        print(f"         got      {actual}")
        return False
    print(f"  OK  — {actual}")
    return True


def adb_push(local_path: str):
    print(f"\nPushing model to device at {DEVICE_PATH} …")
    subprocess.run(["adb", "shell", f"mkdir -p {DEVICE_PATH}"], check=True)
    subprocess.run(["adb", "push", local_path, DEVICE_PATH], check=True)
    print("Done — model is on device.")


def main():
    parser = argparse.ArgumentParser(description="Download Pocket Sarkar Gemma model")
    parser.add_argument(
        "--model",
        choices=list(MODELS.keys()),
        default="e4b",
        help="Model variant to download (e4b and e4b-int4 are the same file)",
    )
    parser.add_argument(
        "--push-to-device",
        action="store_true",
        help="Push model to Android device/emulator via adb after download",
    )
    parser.add_argument(
        "--dest",
        default="android/app/src/main/assets/models",
        help="Local destination directory (default: android/app/src/main/assets/models)",
    )
    args = parser.parse_args()

    ensure_deps()

    info = MODELS[args.model]
    os.makedirs(args.dest, exist_ok=True)

    dest_path = os.path.join(args.dest, info["filename"])
    if os.path.exists(dest_path):
        size_mb = os.path.getsize(dest_path) / (1024 * 1024)
        print(f"Model already exists at {dest_path} ({size_mb:.0f} MB) — skipping download.")
    else:
        dest_path = download_hf(info["repo"], info["filename"], args.dest)
        print(f"\nSaved to: {dest_path}")

    if info["sha256"]:
        if not verify_sha256(dest_path, info["sha256"]):
            print("\nERROR: Checksum mismatch — delete the file and re-download.")
            sys.exit(1)
    else:
        print("(SHA256 not available for this model build — skipping checksum)")

    if args.push_to_device:
        adb_push(dest_path)
    else:
        print("\n── Next steps ───────────────────────────────────────────────────")
        print("Push to emulator / device:")
        print(f"  adb shell mkdir -p {DEVICE_PATH}")
        print(f'  adb push "{dest_path}" {DEVICE_PATH}')
        print("─────────────────────────────────────────────────────────────────")


if __name__ == "__main__":
    main()