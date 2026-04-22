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
    python3 scripts/build_db/build_db.py --refresh-dates   # reset all confidence to 1.0

Output:
    data/processed/schemes.json
    data/processed/eligibility_rules.json
    data/processed/helplines.json
    data/processed/stats.txt
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT          = Path(__file__).resolve().parent.parent.parent
RAW_DIR       = ROOT / "data" / "raw" / "central_schemes"
PROCESSED_DIR = ROOT / "data" / "processed"
PROCESSED_DIR.mkdir(parents=True, exist_ok=True)


# ── Confidence decay ──────────────────────────────────────────────────────────

def compute_confidence(last_verified_iso: str, refresh: bool) -> float:
    """Decay 0.01 per week from last verified date. Floor at 0.0.
    If --refresh-dates is set, always return 1.0 and treat data as fresh today.
    """
    if refresh:
        return 1.0
    try:
        verified = datetime.fromisoformat(last_verified_iso.replace("Z", "+00:00"))
        now = datetime.now(timezone.utc)
        weeks_elapsed = (now - verified).days / 7
        score = max(0.0, round(1.0 - 0.01 * weeks_elapsed, 4))
        return score
    except Exception:
        return 1.0


# ── Normalisation ─────────────────────────────────────────────────────────────

def normalise_scheme(raw: dict, refresh: bool) -> dict:
    """Map raw scraped keys to canonical Scheme entity fields."""
    now_iso = datetime.now(timezone.utc).isoformat()
    last_verified = now_iso if refresh else raw.get("last_verified", now_iso)

    return {
        "id":               raw["id"],
        "nameEn":           raw.get("name_en", ""),
        "nameHi":           raw.get("name_hi", ""),
        "nameLocal":        raw.get("name_local"),
        "category":         raw.get("category", "general"),
        "ministryEn":       raw.get("ministry_en", "Government of India"),
        "descriptionEn":    raw.get("description_en", ""),
        "descriptionHi":    raw.get("description_hi", ""),
        "benefitAmount":    raw.get("benefit_amount"),
        "benefitType":      raw.get("benefit_type", "service"),
        "targetStates":     raw.get("target_states", "ALL"),
        "targetGender":     raw.get("target_gender", "ALL"),
        "targetCategory":   raw.get("target_category", "ALL"),
        "portalUrl":        raw.get("portal_url"),
        "helplineNumber":   raw.get("helpline_number"),
        "confidenceScore":  compute_confidence(last_verified, refresh),
        "lastVerifiedEpoch": int(
            datetime.now(timezone.utc).timestamp() * 1000
            if refresh
            else datetime.fromisoformat(
                last_verified.replace("Z", "+00:00")
            ).timestamp() * 1000
        ),
        "isActive": raw.get("is_active", True),
    }


def normalise_rule(raw: dict) -> dict:
    return {
        "schemeId": raw["scheme_id"],
        "field":    raw["field"],
        "operator": raw["operator"],
        "value":    str(raw["value"]),
        "labelEn":  raw.get("label_en", ""),
        "labelHi":  raw.get("label_hi", ""),
    }


def normalise_helpline(raw: dict) -> dict:
    return {
        "id":           raw["id"],
        "nameEn":       raw.get("name_en", raw.get("nameEn", "")),
        "nameHi":       raw.get("name_hi", raw.get("nameHi", "")),
        "number":       raw.get("number", ""),
        "category":     raw.get("category", "general"),
        "states":       raw.get("states", "ALL"),
        "available24x7": raw.get("available_24x7", raw.get("available24x7", False)),
        "isTollFree":   raw.get("is_toll_free", raw.get("isTollFree", True)),
    }


# ── Built-in helplines fallback ───────────────────────────────────────────────
# These are the 20 core national helplines. They are merged with any helplines
# found in the raw batch files. Batch files take precedence on duplicate id.

BUILTIN_HELPLINES = [
    {"id": "POLICE_100",      "nameEn": "Police Emergency",            "nameHi": "पुलिस आपातकाल",            "number": "100",           "category": "police",          "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "AMBULANCE_108",   "nameEn": "Ambulance",                   "nameHi": "एम्बुलेंस",                 "number": "108",           "category": "health",          "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "FIRE_101",        "nameEn": "Fire Brigade",                "nameHi": "दमकल",                      "number": "101",           "category": "emergency",       "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "WOMEN_1091",      "nameEn": "Women Helpline",              "nameHi": "महिला हेल्पलाइन",           "number": "1091",          "category": "women",           "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "CHILD_1098",      "nameEn": "Childline",                   "nameHi": "चाइल्डलाइन",                "number": "1098",          "category": "child",           "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "SENIOR_14567",    "nameEn": "Senior Citizen Helpline",     "nameHi": "वरिष्ठ नागरिक हेल्पलाइन",  "number": "14567",         "category": "senior",          "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "LABOUR_1800111",  "nameEn": "Labour Helpline",             "nameHi": "श्रम हेल्पलाइन",            "number": "1800-11-8500",  "category": "labour",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "LEGAL_15100",     "nameEn": "NALSA Legal Aid",             "nameHi": "NALSA कानूनी सहायता",       "number": "15100",         "category": "legal",           "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "KISAN_155261",    "nameEn": "Kisan Call Centre",           "nameHi": "किसान कॉल सेंटर",           "number": "155261",        "category": "agriculture",     "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "AYUSHMAN_14555",  "nameEn": "Ayushman Bharat Helpline",    "nameHi": "आयुष्मान भारत हेल्पलाइन",  "number": "14555",         "category": "health",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "UJJWALA_18002666","nameEn": "PM Ujjwala Helpline",         "nameHi": "PM उज्ज्वला हेल्पलाइन",    "number": "1800-266-6696", "category": "energy",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "RATION_14445",    "nameEn": "PDS / Ration Helpline",       "nameHi": "PDS / राशन हेल्पलाइन",     "number": "14445",         "category": "food",            "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "ESHRAM_14434",    "nameEn": "e-Shram Helpline",            "nameHi": "ई-श्रम हेल्पलाइन",         "number": "14434",         "category": "labour",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "CYBER_1930",      "nameEn": "Cyber Crime Helpline",        "nameHi": "साइबर अपराध हेल्पलाइन",    "number": "1930",          "category": "legal",           "states": "ALL", "available24x7": True,  "isTollFree": True},
    {"id": "WATER_1916",      "nameEn": "Jal Jeevan Helpline",         "nameHi": "जल जीवन मिशन हेल्पलाइन",  "number": "1916",          "category": "water_sanitation","states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "MENTAL_iCall",    "nameEn": "iCall Mental Health",         "nameHi": "iCall मानसिक स्वास्थ्य",   "number": "9152987821",    "category": "health",          "states": "ALL", "available24x7": False, "isTollFree": False},
    {"id": "DISABILITY_1800", "nameEn": "Divyang Helpline",            "nameHi": "दिव्यांग हेल्पलाइन",       "number": "1800-11-0031",  "category": "health",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "EDUCATION_1800111","nameEn": "Samagra Shiksha Helpline",   "nameHi": "समग्र शिक्षा हेल्पलाइन",   "number": "1800-11-2001",  "category": "education",       "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "PF_1800118005",   "nameEn": "EPFO PF Helpline",            "nameHi": "EPFO PF हेल्पलाइन",        "number": "1800-118-005",  "category": "labour",          "states": "ALL", "available24x7": False, "isTollFree": True},
    {"id": "MUDRA_1800111",   "nameEn": "MUDRA Loan Helpline",         "nameHi": "मुद्रा लोन हेल्पलाइन",     "number": "1800-180-1111", "category": "financial",       "states": "ALL", "available24x7": False, "isTollFree": True},
]


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Build processed scheme DB from raw JSON batches.")
    parser.add_argument(
        "--refresh-dates",
        action="store_true",
        help="Reset all lastVerified to today and set confidenceScore=1.0 for all schemes. "
             "Use this when your batch JSON files have old dates.",
    )
    args = parser.parse_args()

    if args.refresh_dates:
        print("ℹ️   --refresh-dates: all confidence scores will be set to 1.0")

    raw_files = sorted(RAW_DIR.glob("*.json"))
    if not raw_files:
        print(f"No JSON files found in {RAW_DIR}")
        print("Add scraped scheme JSON files there and re-run.")
        sys.exit(0)

    all_schemes   = []
    all_rules     = []
    all_helplines = {}   # id → helpline dict, batch files override built-ins

    # Seed with built-in helplines first
    for h in BUILTIN_HELPLINES:
        all_helplines[h["id"]] = normalise_helpline(h)

    seen_ids = set()

    for path in raw_files:
        print(f"Processing {path.name}...")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)

        schemes    = data if isinstance(data, list) else data.get("schemes", [])
        rules_raw  = data.get("eligibility_rules", []) if isinstance(data, dict) else []
        helplines_raw = data.get("helplines", []) if isinstance(data, dict) else []

        for raw in schemes:
            sid = raw.get("id")
            if not sid:
                print(f"  WARNING: scheme missing 'id', skipping: {raw.get('name_en','?')}")
                continue
            if sid in seen_ids:
                print(f"  WARNING: duplicate id '{sid}', skipping")
                continue
            seen_ids.add(sid)
            all_schemes.append(normalise_scheme(raw, args.refresh_dates))

        for raw in rules_raw:
            all_rules.append(normalise_rule(raw))

        for raw in helplines_raw:
            hid = raw.get("id")
            if hid:
                all_helplines[hid] = normalise_helpline(raw)

    helplines_list = list(all_helplines.values())

    # ── Write outputs ─────────────────────────────────────────────────────────
    out_schemes   = PROCESSED_DIR / "schemes.json"
    out_rules     = PROCESSED_DIR / "eligibility_rules.json"
    out_helplines = PROCESSED_DIR / "helplines.json"
    out_stats     = PROCESSED_DIR / "stats.txt"

    with open(out_schemes,   "w", encoding="utf-8") as f:
        json.dump(all_schemes,    f, ensure_ascii=False, indent=2)
    with open(out_rules,     "w", encoding="utf-8") as f:
        json.dump(all_rules,      f, ensure_ascii=False, indent=2)
    with open(out_helplines, "w", encoding="utf-8") as f:
        json.dump(helplines_list, f, ensure_ascii=False, indent=2)

    stale = [s for s in all_schemes if s["confidenceScore"] < 0.6]
    stats = (
        f"Build completed: {datetime.now().isoformat()}\n"
        f"Total schemes:   {len(all_schemes)}\n"
        f"Total rules:     {len(all_rules)}\n"
        f"Total helplines: {len(helplines_list)}\n"
        f"Stale (<0.6):    {len(stale)}\n"
        f"Categories:      {sorted(set(s['category'] for s in all_schemes))}\n"
    )
    with open(out_stats, "w") as f:
        f.write(stats)

    print("\n" + stats)
    print(f"Output written to {PROCESSED_DIR}/")


if __name__ == "__main__":
    main()