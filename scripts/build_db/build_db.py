#!/usr/bin/env python3
"""
scripts/build_db/build_db.py
────────────────────────────
Phase 2 — Scheme Database Build Script

Reads raw JSON files from data/raw/central_schemes/,
normalises them, and outputs data/processed/schemes.json
ready for import into the Android DatabaseSeeder or a
pre-populated SQLite asset.

Usage:
    python3 scripts/build_db/build_db.py

Output:
    data/processed/schemes.json
    data/processed/eligibility_rules.json
    data/processed/helplines.json
    data/processed/stats.txt
"""

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RAW_DIR = ROOT / "data" / "raw" / "central_schemes"
PROCESSED_DIR = ROOT / "data" / "processed"
PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

# ── Confidence decay ──────────────────────────────────────────────────────────

def compute_confidence(last_verified_iso: str) -> float:
    """Decay 0.01 per week from last verified date. Floor at 0.0."""
    try:
        verified = datetime.fromisoformat(last_verified_iso.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)
        weeks_elapsed = (now - verified).days / 7
        score = max(0.0, round(1.0 - 0.01 * weeks_elapsed, 4))
        return score
    except Exception:
        return 1.0  # Default to fresh if date is missing


# ── Normalisation ─────────────────────────────────────────────────────────────

def normalise_scheme(raw: dict) -> dict:
    """Map raw scraped keys to canonical Scheme entity fields."""
    last_verified = raw.get("last_verified", datetime.now(timezone.utc).isoformat())
    return {
        "id": raw["id"],
        "nameEn": raw.get("name_en", ""),
        "nameHi": raw.get("name_hi", ""),
        "nameLocal": raw.get("name_local"),
        "category": raw.get("category", "general"),
        "ministryEn": raw.get("ministry_en", "Government of India"),
        "descriptionEn": raw.get("description_en", ""),
        "descriptionHi": raw.get("description_hi", ""),
        "benefitAmount": raw.get("benefit_amount"),
        "benefitType": raw.get("benefit_type", "service"),
        "targetStates": raw.get("target_states", "ALL"),
        "targetGender": raw.get("target_gender", "ALL"),
        "targetCategory": raw.get("target_category", "ALL"),
        "portalUrl": raw.get("portal_url"),
        "helplineNumber": raw.get("helpline_number"),
        "confidenceScore": compute_confidence(last_verified),
        "lastVerifiedEpoch": int(
            datetime.fromisoformat(
                last_verified.replace("Z", "+00:00")
            ).timestamp() * 1000
        ),
        "isActive": raw.get("is_active", True),
    }


def normalise_rule(raw: dict) -> dict:
    return {
        "schemeId": raw["scheme_id"],
        "field": raw["field"],
        "operator": raw["operator"],
        "value": str(raw["value"]),
        "labelEn": raw.get("label_en", ""),
        "labelHi": raw.get("label_hi", ""),
    }


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    raw_files = sorted(RAW_DIR.glob("*.json"))
    if not raw_files:
        print(f"No JSON files found in {RAW_DIR}")
        print("Add scraped scheme JSON files there and re-run.")
        print("Expected format: list of scheme objects per file.")
        sys.exit(0)

    all_schemes = []
    all_rules = []
    seen_ids = set()

    for path in raw_files:
        print(f"Processing {path.name}...")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)

        schemes = data if isinstance(data, list) else data.get("schemes", [])
        rules_raw = data.get("eligibility_rules", []) if isinstance(data, dict) else []

        for raw in schemes:
            sid = raw.get("id")
            if not sid:
                print(f"  WARNING: scheme missing 'id', skipping: {raw.get('name_en','?')}")
                continue
            if sid in seen_ids:
                print(f"  WARNING: duplicate id '{sid}', skipping")
                continue
            seen_ids.add(sid)
            all_schemes.append(normalise_scheme(raw))

        for raw in rules_raw:
            all_rules.append(normalise_rule(raw))

    # Write outputs
    out_schemes = PROCESSED_DIR / "schemes.json"
    out_rules = PROCESSED_DIR / "eligibility_rules.json"
    out_stats = PROCESSED_DIR / "stats.txt"

    with open(out_schemes, "w", encoding="utf-8") as f:
        json.dump(all_schemes, f, ensure_ascii=False, indent=2)

    with open(out_rules, "w", encoding="utf-8") as f:
        json.dump(all_rules, f, ensure_ascii=False, indent=2)

    stale = [s for s in all_schemes if s["confidenceScore"] < 0.6]
    stats = (
        f"Build completed: {datetime.now().isoformat()}\n"
        f"Total schemes:   {len(all_schemes)}\n"
        f"Total rules:     {len(all_rules)}\n"
        f"Stale (<0.6):    {len(stale)}\n"
        f"Categories:      {sorted(set(s['category'] for s in all_schemes))}\n"
    )
    with open(out_stats, "w") as f:
        f.write(stats)

    print("\n" + stats)
    print(f"Output written to {PROCESSED_DIR}/")


if __name__ == "__main__":
    main()
