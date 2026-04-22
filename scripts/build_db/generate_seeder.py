#!/usr/bin/env python3
"""
scripts/build_db/generate_seeder.py
────────────────────────────────────
Phase 2 expansion — DatabaseSeeder.kt code generator

Reads:
    data/processed/schemes.json
    data/processed/eligibility_rules.json
    data/processed/helplines.json   (optional)

Writes:
    android/app/src/main/kotlin/com/pocketsarkar/db/DatabaseSeeder.kt

Usage:
    # Step 1 — build processed JSON from your batch files first:
    python scripts/build_db/build_db.py

    # Step 2 — generate the Kotlin seeder:
    python scripts/build_db/generate_seeder.py

    # Or do both in one go:
    python scripts/build_db/build_db.py && python scripts/build_db/generate_seeder.py
"""

import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT         = Path(__file__).resolve().parent.parent.parent
PROCESSED    = ROOT / "data" / "processed"
SEEDER_OUT   = (
    ROOT
    / "android/app/src/main/kotlin/com/pocketsarkar/db/DatabaseSeeder.kt"
)

SCHEMES_FILE  = PROCESSED / "schemes.json"
RULES_FILE    = PROCESSED / "eligibility_rules.json"
HELPLINES_FILE = PROCESSED / "helplines.json"   # optional


# ── Kotlin string escaping ────────────────────────────────────────────────────

def esc(value) -> str:
    """Escape a value for a Kotlin string literal."""
    if value is None:
        return "null"
    s = str(value)
    s = s.replace("\\", "\\\\")
    s = s.replace('"', '\\"')
    s = s.replace("\n", "\\n")
    s = s.replace("\r", "\\r")
    s = s.replace("\t", "\\t")
    s = s.replace("$", "\\$")
    return f'"{s}"'


def esc_nullable(value) -> str:
    if value is None or value == "" or value == "null":
        return "null"
    return esc(value)


def kt_bool(value) -> str:
    if isinstance(value, bool):
        return "true" if value else "false"
    return "true" if str(value).lower() in ("true", "1", "yes") else "false"


def kt_float(value) -> str:
    try:
        return f"{float(value):.4f}f"
    except (TypeError, ValueError):
        return "1.0f"


def kt_long(value) -> str:
    try:
        return str(int(value)) + "L"
    except (TypeError, ValueError):
        return str(int(datetime.now(timezone.utc).timestamp() * 1000)) + "L"


# ── Kotlin block builders ─────────────────────────────────────────────────────

def scheme_to_kt(s: dict) -> str:
    lines = [
        "        Scheme(",
        f"            id = {esc(s['id'])},",
        f"            nameEn = {esc(s.get('nameEn', ''))},",
        f"            nameHi = {esc(s.get('nameHi', ''))},",
        f"            nameLocal = {esc_nullable(s.get('nameLocal'))},",
        f"            category = {esc(s.get('category', 'general'))},",
        f"            ministryEn = {esc(s.get('ministryEn', 'Government of India'))},",
        f"            descriptionEn = {esc(s.get('descriptionEn', ''))},",
        f"            descriptionHi = {esc(s.get('descriptionHi', ''))},",
        f"            benefitAmount = {esc_nullable(s.get('benefitAmount'))},",
        f"            benefitType = {esc(s.get('benefitType', 'service'))},",
        f"            targetStates = {esc(s.get('targetStates', 'ALL'))},",
        f"            targetGender = {esc(s.get('targetGender', 'ALL'))},",
        f"            targetCategory = {esc(s.get('targetCategory', 'ALL'))},",
        f"            portalUrl = {esc_nullable(s.get('portalUrl'))},",
        f"            helplineNumber = {esc_nullable(s.get('helplineNumber'))},",
        f"            confidenceScore = {kt_float(s.get('confidenceScore', 1.0))},",
        f"            lastVerifiedEpoch = {kt_long(s.get('lastVerifiedEpoch'))},",
        f"            isActive = {kt_bool(s.get('isActive', True))}",
        "        )",
    ]
    return "\n".join(lines)


def rule_to_kt(r: dict) -> str:
    lines = [
        "        EligibilityRule(",
        f"            schemeId = {esc(r['schemeId'])},",
        f"            field = {esc(r['field'])},",
        f"            operator = {esc(r['operator'])},",
        f"            value = {esc(r['value'])},",
        f"            labelEn = {esc(r.get('labelEn', ''))},",
        f"            labelHi = {esc(r.get('labelHi', ''))}",
        "        )",
    ]
    return "\n".join(lines)


def helpline_to_kt(h: dict) -> str:
    lines = [
        "        HelplineNumber(",
        f"            id = {esc(h['id'])},",
        f"            nameEn = {esc(h.get('nameEn', ''))},",
        f"            nameHi = {esc(h.get('nameHi', ''))},",
        f"            number = {esc(h.get('number', ''))},",
        f"            category = {esc(h.get('category', 'general'))},",
        f"            states = {esc(h.get('states', 'ALL'))},",
        f"            available24x7 = {kt_bool(h.get('available24x7', False))},",
        f"            isTollFree = {kt_bool(h.get('isTollFree', True))}",
        "        )",
    ]
    return "\n".join(lines)


# ── Seeder version key ────────────────────────────────────────────────────────

def seeder_version_key(schemes: list) -> str:
    """Bump version key when scheme count changes so seeder re-runs on update."""
    return f"db_seeded_v{len(schemes)}"


# ── Main ──────────────────────────────────────────────────────────────────────

def load_json(path: Path, label: str) -> list:
    if not path.exists():
        print(f"❌  {label} not found at {path}")
        print(f"    Run:  python scripts/build_db/build_db.py  first.")
        sys.exit(1)
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if isinstance(data, dict):
        # build_db.py might wrap in {"schemes": [...]}
        return data.get("schemes") or data.get("rules") or data.get("helplines") or []
    return data


def main():
    print("=" * 60)
    print("  Pocket Sarkar — DatabaseSeeder.kt Generator")
    print("=" * 60)

    schemes   = load_json(SCHEMES_FILE,  "schemes.json")
    rules     = load_json(RULES_FILE,    "eligibility_rules.json")
    helplines = []
    if HELPLINES_FILE.exists():
        helplines = load_json(HELPLINES_FILE, "helplines.json")
    else:
        print("ℹ️   helplines.json not found — keeping existing helplines in seeder.")

    print(f"  Schemes   : {len(schemes)}")
    print(f"  Rules     : {len(rules)}")
    print(f"  Helplines : {len(helplines)}")

    version_key = seeder_version_key(schemes)
    generated_at = datetime.now().strftime("%Y-%m-%d %H:%M")

    scheme_blocks   = ",\n\n".join(scheme_to_kt(s) for s in schemes)
    rule_blocks     = ",\n\n".join(rule_to_kt(r) for r in rules)
    helpline_blocks = ",\n\n".join(helpline_to_kt(h) for h in helplines) if helplines else ""

    # If no helplines in processed data, preserve a minimal placeholder
    if not helplines:
        helpline_list_kt = "        // No helplines in processed data — add to data/processed/helplines.json"
    else:
        helpline_list_kt = helpline_blocks

    kt = f"""\
package com.pocketsarkar.db

import android.content.Context
import android.util.Log
import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DatabaseSeeder — AUTO-GENERATED by scripts/build_db/generate_seeder.py
 * Generated : {generated_at}
 * Schemes   : {len(schemes)}
 * Rules     : {len(rules)}
 * Helplines : {len(helplines)}
 *
 * DO NOT EDIT BY HAND.
 * To regenerate:
 *   python scripts/build_db/build_db.py
 *   python scripts/build_db/generate_seeder.py
 */
object DatabaseSeeder {{

    private const val TAG = "DatabaseSeeder"
    private const val PREFS_NAME = "pocket_sarkar_prefs"
    private const val KEY_SEEDED = "{version_key}"

    suspend fun seedIfNeeded(context: Context, dao: SchemeDao) = withContext(Dispatchers.IO) {{
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) {{
            Log.d(TAG, "DB already seeded, skipping.")
            return@withContext
        }}

        Log.i(TAG, "Seeding DB with government schemes...")

        dao.insertSchemes(ALL_SCHEMES)
        dao.insertRules(ALL_RULES)
        dao.insertHelplines(ALL_HELPLINES)

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        Log.i(TAG, "Seeded ${{ALL_SCHEMES.size}} schemes, ${{ALL_RULES.size}} rules, ${{ALL_HELPLINES.size}} helplines.")
    }}

    // ──────────────────────────────────────────────────────────────────────────
    // SCHEMES ({len(schemes)} total)
    // ──────────────────────────────────────────────────────────────────────────

    private val ALL_SCHEMES = listOf(

{scheme_blocks}

    )

    // ──────────────────────────────────────────────────────────────────────────
    // ELIGIBILITY RULES ({len(rules)} total)
    // ──────────────────────────────────────────────────────────────────────────

    private val ALL_RULES = listOf(

{rule_blocks}

    )

    // ──────────────────────────────────────────────────────────────────────────
    // HELPLINE NUMBERS ({len(helplines)} total)
    // ──────────────────────────────────────────────────────────────────────────

    private val ALL_HELPLINES = listOf(

{helpline_list_kt}

    )
}}
"""

    SEEDER_OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(SEEDER_OUT, "w", encoding="utf-8") as f:
        f.write(kt)

    print(f"\n✅  Written to {SEEDER_OUT}")
    print(f"    Version key: {version_key}")
    print("\n📋  Next steps:")
    print("    1. Open Android Studio → Build → Clean Project")
    print("    2. Build → Make Project   (KSP will re-process Room entities)")
    print("    3. Run app on device — DB will reseed automatically on first launch")


if __name__ == "__main__":
    main()