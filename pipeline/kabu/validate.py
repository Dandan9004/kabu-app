"""Checks the routine's evaluations before they are published. Run inside the routine too, so it can retry.

python -m kabu.validate evaluations/2026-11-15.json dossiers/2026-11-15/picks.json
"""
from __future__ import annotations

import statistics
import sys
from pathlib import Path

import jsonschema

from .common import ROOT, read_json

SCHEMA = ROOT / "schema" / "evaluation.schema.json"
POWER_ROLES = {"事業の目利き", "決算の読み手", "成長の見張り", "業界と景気", "株主目線"}
# Words that read as buy/sell advice; the app must stay a reference
ADVICE_WORDS = ("買い", "売り", "おすすめ", "推奨", "持つべき", "買うべき", "売るべき", "狙い目")


def validate_evaluations(data, codes: list[str]) -> list[str]:
    if data is None:
        return ["evaluations file is missing"]
    errors = [f"{'/'.join(map(str, e.path))}: {e.message}"
              for e in jsonschema.Draft202012Validator(read_json(SCHEMA)).iter_errors(data)]
    if errors:
        return errors

    seen = [item["code"] for item in data["items"]]
    if sorted(seen) != sorted(codes):
        errors.append(f"codes {sorted(seen)} do not match the picks {sorted(codes)}")

    for item in data["items"]:
        code = item["code"]
        roles = {e["role"]: e for e in item["experts"]}
        if len(roles) != 6:
            errors.append(f"{code}: each of the six experts must appear once")
            continue
        for role, expert in roles.items():
            want = "power" if role in POWER_ROLES else "cheapness"
            if expert["tag"] != want:
                errors.append(f"{code}: {role} must be tagged {want}")
        # まとめ役 decides 会社の力, but it may not drift far from the five experts behind it
        mean = statistics.mean(roles[r]["score"] for r in POWER_ROLES)
        if abs(item["power"] - mean) > 1:
            errors.append(f"{code}: power {item['power']} is more than 1 away from the experts' mean {mean:.1f}")
        if abs(item["cheapness"] - roles["値段の番人"]["score"]) > 1:
            errors.append(f"{code}: cheapness is more than 1 away from 値段の番人")
        text = " ".join([item["headline"], item["summary"], *item["weakness"]["points"], *(e["why"] for e in item["experts"])])
        for word in ADVICE_WORDS:
            if word in text:
                errors.append(f"{code}: uses the advice word 「{word}」")

    # Scores bunched together say nothing; ask for a re-think rather than publish ten 4s
    powers = [item["power"] for item in data["items"]]
    if len(powers) >= 5 and len(set(powers)) == 1:
        errors.append(f"every stock got power {powers[0]}; spread the scores using the score guide")
    return errors


def main(argv: list[str]) -> int:
    evaluations = read_json(Path(argv[0]))
    picks = read_json(Path(argv[1])) or {}
    errors = validate_evaluations(evaluations, picks.get("codes", []))
    if errors:
        print("\n".join(errors))
        return 1
    print("ok")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
