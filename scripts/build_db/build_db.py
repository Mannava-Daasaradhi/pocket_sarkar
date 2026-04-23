#!/usr/bin/env python3
"""
build_db.py — Pocket Sarkar scheme database builder.

Usage:
    python scripts/build_db/build_db.py --input data/raw/central_schemes/schemes.json
    python scripts/build_db/build_db.py  # uses default input dir

Outputs: data/schemes/sqlite/pocket_sarkar.db
"""

import argparse
import json
import os
import sqlite3
import sys
from pathlib import Path

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR   = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_INPUT_DIR = PROJECT_ROOT / "data" / "raw" / "central_schemes"
OUTPUT_DIR   = PROJECT_ROOT / "data" / "schemes" / "sqlite"
OUTPUT_DB    = OUTPUT_DIR / "pocket_sarkar.db"

# ── Schema ────────────────────────────────────────────────────────────────────
DDL_SCHEMES = """
CREATE TABLE IF NOT EXISTS schemes (
    id                TEXT PRIMARY KEY,
    nameEn            TEXT NOT NULL,
    nameHi            TEXT NOT NULL,
    nameLocal         TEXT,
    category          TEXT NOT NULL,
    ministryEn        TEXT NOT NULL,
    descriptionEn     TEXT NOT NULL,
    descriptionHi     TEXT NOT NULL,
    benefitAmount     TEXT,
    benefitType       TEXT NOT NULL,
    targetStates      TEXT NOT NULL DEFAULT 'ALL',
    targetGender      TEXT NOT NULL DEFAULT 'ALL',
    targetCategory    TEXT NOT NULL DEFAULT 'ALL',
    maxIncomeLPA      REAL NOT NULL DEFAULT 0.0,
    minAge            INTEGER NOT NULL DEFAULT 0,
    maxAge            INTEGER NOT NULL DEFAULT 0,
    casteEligibility  TEXT NOT NULL DEFAULT '[]',
    documentsRequired TEXT NOT NULL DEFAULT '[]',
    portalUrl         TEXT,
    helplineNumber    TEXT,
    confidenceScore   REAL NOT NULL DEFAULT 1.0,
    lastVerifiedEpoch INTEGER NOT NULL,
    isActive          INTEGER NOT NULL DEFAULT 1
)
"""

DDL_ELIGIBILITY_RULES = """
CREATE TABLE IF NOT EXISTS eligibility_rules (
    ruleId    INTEGER PRIMARY KEY AUTOINCREMENT,
    schemeId  TEXT NOT NULL,
    field     TEXT NOT NULL,
    operator  TEXT NOT NULL,
    value     TEXT NOT NULL,
    labelEn   TEXT NOT NULL,
    labelHi   TEXT NOT NULL,
    FOREIGN KEY (schemeId) REFERENCES schemes(id) ON DELETE CASCADE
)
"""

DDL_FTS = """
CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
    nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
    content='schemes',
    content_rowid='rowid'
)
"""

# ── Helpers ───────────────────────────────────────────────────────────────────

def load_schemes_from_file(path: Path) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return data if isinstance(data, list) else data.get("schemes", [])


def load_schemes_from_dir(directory: Path) -> list[dict]:
    schemes = []
    for json_file in sorted(directory.glob("*.json")):
        schemes.extend(load_schemes_from_file(json_file))
    return schemes


def insert_scheme(cur: sqlite3.Cursor, s: dict) -> None:
    cur.execute("""
        INSERT OR REPLACE INTO schemes (
            id, nameEn, nameHi, nameLocal, category, ministryEn,
            descriptionEn, descriptionHi, benefitAmount, benefitType,
            targetStates, targetGender, targetCategory,
            maxIncomeLPA, minAge, maxAge,
            casteEligibility, documentsRequired,
            portalUrl, helplineNumber,
            confidenceScore, lastVerifiedEpoch, isActive
        ) VALUES (
            :id, :nameEn, :nameHi, :nameLocal, :category, :ministryEn,
            :descriptionEn, :descriptionHi, :benefitAmount, :benefitType,
            :targetStates, :targetGender, :targetCategory,
            :maxIncomeLPA, :minAge, :maxAge,
            :casteEligibility, :documentsRequired,
            :portalUrl, :helplineNumber,
            :confidenceScore, :lastVerifiedEpoch, :isActive
        )
    """, {
        "id":                s["id"],
        "nameEn":            s["nameEn"],
        "nameHi":            s.get("nameHi", s["nameEn"]),
        "nameLocal":         s.get("nameLocal"),
        "category":          s["category"],
        "ministryEn":        s["ministryEn"],
        "descriptionEn":     s["descriptionEn"],
        "descriptionHi":     s.get("descriptionHi", s["descriptionEn"]),
        "benefitAmount":     s.get("benefitAmount"),
        "benefitType":       s.get("benefitType", "cash"),
        "targetStates":      s.get("targetStates", "ALL"),
        "targetGender":      s.get("targetGender", "ALL"),
        "targetCategory":    s.get("targetCategory", "ALL"),
        "maxIncomeLPA":      s.get("maxIncomeLPA", 0.0),
        "minAge":            s.get("minAge", 0),
        "maxAge":            s.get("maxAge", 0),
        "casteEligibility":  s.get("casteEligibility", "[]"),
        "documentsRequired": s.get("documentsRequired", "[]"),
        "portalUrl":         s.get("portalUrl"),
        "helplineNumber":    s.get("helplineNumber"),
        "confidenceScore":   s.get("confidenceScore", 1.0),
        "lastVerifiedEpoch": s.get("lastVerifiedEpoch", 1776842111850),
        "isActive":          1 if s.get("isActive", True) else 0,
    })

    for rule in s.get("eligibilityRules", []):
        cur.execute("""
            INSERT INTO eligibility_rules
                (schemeId, field, operator, value, labelEn, labelHi)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (
            s["id"],
            rule["field"],
            rule["operator"],
            rule["value"],
            rule.get("labelEn", f"{rule['field']} {rule['operator']} {rule['value']}"),
            rule.get("labelHi", rule.get("labelEn", "")),
        ))


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description="Build Pocket Sarkar SQLite DB")
    parser.add_argument(
        "--input", "-i",
        type=Path,
        default=None,
        help="Path to a single JSON file or directory of JSON files. "
             f"Defaults to {DEFAULT_INPUT_DIR}"
    )
    parser.add_argument(
        "--output", "-o",
        type=Path,
        default=OUTPUT_DB,
        help=f"Output .db path. Defaults to {OUTPUT_DB}"
    )
    args = parser.parse_args()

    input_path: Path = args.input or DEFAULT_INPUT_DIR
    output_path: Path = args.output

    # Load schemes
    if input_path.is_file():
        schemes = load_schemes_from_file(input_path)
    elif input_path.is_dir():
        schemes = load_schemes_from_dir(input_path)
    else:
        print(f"ERROR: input path does not exist: {input_path}", file=sys.stderr)
        sys.exit(1)

    if not schemes:
        print("ERROR: no schemes found in input.", file=sys.stderr)
        sys.exit(1)

    # Prepare output dir
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    # Build DB
    con = sqlite3.connect(output_path)
    cur = con.cursor()
    cur.executescript("PRAGMA foreign_keys = ON;")

    cur.execute(DDL_SCHEMES)
    cur.execute(DDL_ELIGIBILITY_RULES)
    cur.execute(DDL_FTS)

    inserted = 0
    errors   = 0
    for s in schemes:
        try:
            insert_scheme(cur, s)
            inserted += 1
        except Exception as e:
            print(f"  WARN: skipped {s.get('id', '?')} — {e}", file=sys.stderr)
            errors += 1

    # Rebuild FTS index
    cur.execute("INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')")

    con.commit()
    con.close()

    print(f"✓ {inserted} schemes inserted, {errors} skipped")
    print(f"✓ FTS5 index rebuilt")
    print(f"✓ Output: {output_path}")

    if inserted < 50:
        print(f"WARNING: only {inserted} schemes — spec requires 50+", file=sys.stderr)


if __name__ == "__main__":
    main()