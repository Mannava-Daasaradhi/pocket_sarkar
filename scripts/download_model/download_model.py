#!/usr/bin/env python3
"""
scripts/download_model/download_model.py
─────────────────────────────────────────
Phase 3 — Gemma 4 Model Downloader

Downloads the MediaPipe-format Gemma 4 model from Kaggle and places it
where GemmaEngine.kt expects it.

Usage:
    python scripts/download_model/download_model.py --model e4b-int4
    python scripts/download_model/download_model.py --model 26b --output /custom/path/

Model sizes (approximate):
    e4b-int4   ~2.5 GB   → Android on-device (MediaPipe .task)
    26b        ~16 GB    → Raspberry Pi / server (GGUF via Ollama)

Prerequisites:
    pip install kaggle tqdm requests
    Set KAGGLE_USERNAME and KAGGLE_KEY env vars (or place ~/.kaggle/kaggle.json).
    Accept Gemma licence at: https://www.kaggle.com/models/google/gemma
"""

import argparse
import hashlib
import json
import os
import shutil
import sys
import zipfile
from pathlib import Path

try:
    import requests
    from tqdm import tqdm
except ImportError:
    print("Missing dependencies. Run:  pip install requests tqdm kaggle")
    sys.exit(1)

# ── Model catalogue ───────────────────────────────────────────────────────────

MODELS = {
    "e4b-int4": {
        "description": "Gemma 4 E4B INT4 — Android on-device via MediaPipe",
        "kaggle_handle": "google/gemma/mediapipe/gemma-4-e4b-it-gpu-int4",
        "filename":      "gemma4-e4b-it-int4.task",
        "size_hint":     "~2.5 GB",
        "target_subdir": "android/app/src/main/assets/models",
        "format":        "mediapipe_task",
        "note": (
            "Place the .task file at:\n"
            "  android/app/src/main/assets/models/gemma4-e4b-it-int4.task\n"
            "OR push it to the device at:\n"
            "  /sdcard/Android/data/com.pocketsarkar/files/models/gemma4-e4b-it-int4.task\n"
            "(External storage path is preferred for large files — avoids APK size limits.)"
        ),
    },
    "26b": {
        "description": "Gemma 4 26B — Raspberry Pi / demo server via Ollama (GGUF)",
        "kaggle_handle": "google/gemma/gguf/gemma-4-27b-it-q4_k_m",
        "filename":      "gemma-4-27b-it-q4_k_m.gguf",
        "size_hint":     "~16 GB",
        "target_subdir": "deployment/ollama/models",
        "format":        "gguf",
        "note": (
            "Place the .gguf file in deployment/ollama/models/\n"
            "Then run:  ollama create gemma4:26b -f deployment/ollama/Modelfile"
        ),
    },
}

ROOT = Path(__file__).resolve().parent.parent.parent


# ── Helpers ───────────────────────────────────────────────────────────────────

def check_kaggle_credentials():
    """Verify Kaggle API credentials are available."""
    kaggle_json = Path.home() / ".kaggle" / "kaggle.json"
    has_env = os.environ.get("KAGGLE_USERNAME") and os.environ.get("KAGGLE_KEY")
    has_file = kaggle_json.exists()

    if not has_env and not has_file:
        print("\n❌  Kaggle credentials not found.")
        print("    Option 1 — Environment variables:")
        print("        export KAGGLE_USERNAME=your_username")
        print("        export KAGGLE_KEY=your_api_key")
        print("    Option 2 — Credential file:")
        print("        Download kaggle.json from https://www.kaggle.com/settings → API")
        print("        Place it at ~/.kaggle/kaggle.json")
        print("    Then accept the Gemma licence at:")
        print("        https://www.kaggle.com/models/google/gemma")
        sys.exit(1)


def md5_file(path: Path, chunk=1 << 20) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(chunk), b""):
            h.update(block)
    return h.hexdigest()


def download_with_kaggle_api(handle: str, dest_dir: Path, filename: str) -> Path:
    """
    Download a Kaggle model variation using the Kaggle HTTP API directly.
    handle format: "owner/model/framework/variation"
    """
    try:
        import kaggle  # noqa: F401 — triggers credential setup
    except ImportError:
        print("kaggle package not found. Run:  pip install kaggle")
        sys.exit(1)

    parts = handle.split("/")
    if len(parts) != 4:
        print(f"❌  Invalid Kaggle handle: {handle}")
        sys.exit(1)

    owner, model, framework, variation = parts
    dest_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n📥  Downloading via Kaggle API...")
    print(f"    Handle   : {handle}")
    print(f"    Dest     : {dest_dir}")

    # Use kaggle CLI as subprocess (most reliable across API versions)
    import subprocess
    cmd = [
        sys.executable, "-m", "kaggle", "models", "instances", "versions",
        "download",
        f"{owner}/{model}/{framework}/{variation}",
        "--path", str(dest_dir),
        "--untar",
    ]
    result = subprocess.run(cmd, capture_output=False)
    if result.returncode != 0:
        print(f"\n❌  Kaggle download failed (exit {result.returncode})")
        print("    Make sure you have accepted the model licence at:")
        print("    https://www.kaggle.com/models/google/gemma")
        sys.exit(1)

    # Find the downloaded file (Kaggle may unpack into subdirs)
    candidates = list(dest_dir.rglob(f"*.task")) + list(dest_dir.rglob(f"*.gguf"))
    if not candidates:
        # Fallback: look for any large binary
        candidates = [p for p in dest_dir.rglob("*") if p.is_file() and p.stat().st_size > 100_000_000]

    if candidates:
        downloaded = candidates[0]
        final = dest_dir / filename
        if downloaded != final:
            shutil.move(str(downloaded), str(final))
        return final

    print("⚠️   Download completed but could not locate model file. Check:", dest_dir)
    return dest_dir / filename


def verify_and_report(model_path: Path, model_info: dict):
    if not model_path.exists():
        print(f"\n⚠️   File not found at {model_path}")
        return

    size_mb = model_path.stat().st_size / (1024 * 1024)
    print(f"\n✅  Model downloaded successfully!")
    print(f"    Path : {model_path}")
    print(f"    Size : {size_mb:.1f} MB")
    print(f"\n📋  Next steps:")
    print(f"    {model_info['note']}")


def push_to_device(model_path: Path, filename: str):
    """Optional: push model to Android device via ADB."""
    import subprocess
    device_path = f"/sdcard/Android/data/com.pocketsarkar/files/models/{filename}"
    print(f"\n📱  Pushing to Android device via ADB...")
    print(f"    {model_path} → {device_path}")

    # Create the directory on device
    subprocess.run(["adb", "shell", "mkdir", "-p",
                    "/sdcard/Android/data/com.pocketsarkar/files/models/"],
                   check=False)

    result = subprocess.run(["adb", "push", str(model_path), device_path])
    if result.returncode == 0:
        print("✅  Pushed to device successfully!")
        print(f"    GemmaEngine will find it at getExternalFilesDir(null)/models/{filename}")
    else:
        print("❌  ADB push failed. Is your device connected with USB debugging enabled?")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Download Gemma 4 model for Pocket Sarkar",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="\n".join(
            f"  --model {k:10s}  {v['description']}  ({v['size_hint']})"
            for k, v in MODELS.items()
        ),
    )
    parser.add_argument(
        "--model",
        choices=list(MODELS.keys()),
        required=True,
        help="Which model to download",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Override destination directory (default: auto-detected from repo root)",
    )
    parser.add_argument(
        "--push-to-device",
        action="store_true",
        help="After download, push to connected Android device via ADB (e4b-int4 only)",
    )
    parser.add_argument(
        "--skip-download",
        action="store_true",
        help="Skip download and just print instructions (useful for CI)",
    )
    args = parser.parse_args()

    info = MODELS[args.model]
    dest_dir = args.output or (ROOT / info["target_subdir"])

    print("=" * 60)
    print("  Pocket Sarkar — Model Downloader")
    print("=" * 60)
    print(f"  Model   : {args.model}")
    print(f"  Format  : {info['format']}")
    print(f"  Size    : {info['size_hint']}")
    print(f"  Dest    : {dest_dir}")
    print("=" * 60)

    if args.skip_download:
        print("\nℹ️   --skip-download set. Instructions only:\n")
        print(info["note"])
        return

    # Warn about APK size for assets path
    if "assets/models" in str(dest_dir) and args.model == "e4b-int4":
        print("\n⚠️   WARNING: Placing a 2.5 GB file in android/app/src/main/assets/")
        print("    will make the APK huge and may exceed Play Store limits.")
        print("    For development, prefer --push-to-device to push directly to device.")
        print("    Production: serve via DownloadManager or ship as OBB expansion file.")
        confirm = input("\n    Continue anyway? [y/N]: ").strip().lower()
        if confirm != "y":
            print("\nAborted. Run with --push-to-device to push to a connected device instead.")
            sys.exit(0)

    check_kaggle_credentials()

    model_path = download_with_kaggle_api(
        handle=info["kaggle_handle"],
        dest_dir=dest_dir,
        filename=info["filename"],
    )

    verify_and_report(model_path, info)

    if args.push_to_device and args.model == "e4b-int4" and model_path.exists():
        push_to_device(model_path, info["filename"])


if __name__ == "__main__":
    main()