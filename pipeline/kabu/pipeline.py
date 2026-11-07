"""The jobs GitHub Actions runs.

daily   – every morning: company list, announcements, rule-based scores → out/ (GitHub Pages)
weekly  – Saturday early: count coverage, pick the 10, write dossiers/<date>/ for the Claude routine
merge   – after the routine pushed evaluations/<date>.json: check it and publish out/weekly/
"""
from __future__ import annotations

import argparse
import sys
from datetime import date, timedelta

from . import companies as company_list
from . import news, tdnet, trending
from .common import OUT, ROOT, STATE, now_jst, read_json, today_jst, write_json
from .validate import validate_evaluations

DISCLOSURE_DAYS = 30


def _status(ok: bool, previous: dict | None) -> dict:
    """Source health for index.json; keeps the first failing day so the app can say 'since …'."""
    today = today_jst()
    if ok:
        return {"ok": True, "last_ok": today, "failing_since": ""}
    previous = previous or {}
    return {"ok": False, "last_ok": previous.get("last_ok", ""),
            "failing_since": previous.get("failing_since") or today}


def daily() -> int:
    old_index = read_json(OUT / "index.json", {}) or {}
    old_sources = old_index.get("sources", {})
    sources = {}

    try:
        # The list changes monthly; refresh it once a week
        companies = company_list.refresh() if now_jst().weekday() == 0 or not (STATE / "companies.json").exists() else company_list.load()
        sources["companies"] = _status(True, None)
    except Exception as error:  # keep yesterday's list
        print("companies failed:", error, file=sys.stderr)
        companies = company_list.load()
        sources["companies"] = _status(False, old_sources.get("companies"))

    # Announcements: keep a rolling 30 days in state/, fetching only the days not stored yet (plus today)
    store = read_json(STATE / "disclosures.json", {}) or {}
    tdnet_ok = True
    for back in range(DISCLOSURE_DAYS):
        day = date.fromisoformat(today_jst()) - timedelta(days=back)
        key = day.isoformat()
        if key in store and back > 1:
            continue
        try:
            store[key] = tdnet.fetch_day(day)
        except Exception as error:
            print("tdnet failed for", key, error, file=sys.stderr)
            tdnet_ok = False
    cutoff = (date.fromisoformat(today_jst()) - timedelta(days=DISCLOSURE_DAYS)).isoformat()
    store = {k: v for k, v in store.items() if k > cutoff}
    write_json(STATE / "disclosures.json", store, compact=True)
    sources["tdnet"] = _status(tdnet_ok, old_sources.get("tdnet"))

    by_code: dict[str, list[dict]] = {}
    for day in sorted(store, reverse=True):
        for d in store[day]:
            by_code.setdefault(d["code"], []).append(d)

    # Prices, EDINET filings and fundamentals need keys kept in Actions secrets; until then they stay empty
    for key in ("prices", "edinet", "financials"):
        sources[key] = old_sources.get(key) or {"ok": True, "last_ok": "", "failing_since": ""}

    stocks = {}
    for code, c in companies.items():
        mine = by_code.get(code, [])
        important = [d for d in mine if d["important"]][:3]
        stocks[code] = {
            "name": c["name"],
            "sector": c["sector"],
            "fundamental": None,
            "investor": None,
            "disclosures": [{"date": d["date"], "title": d["title"], "url": d["url"]} for d in important],
            "bad": any(d["bad"] for d in mine[:5]),
        }
    as_of = today_jst()
    write_json(OUT / "scores_all.json", {"as_of": as_of, "stocks": stocks}, compact=True)

    weekly_path = old_index.get("files", {}).get("weekly", "weekly/latest.json")
    if not (OUT / weekly_path).exists():
        write_json(OUT / "weekly/latest.json", {"date": "", "next_date": next_saturday(), "items": []})
        weekly_path = "weekly/latest.json"
    if not (OUT / "track.json").exists():
        write_json(OUT / "track.json", {"rows": []})

    write_json(OUT / "index.json", {
        "schema_version": 1,
        "generated_at": now_jst().isoformat(timespec="seconds"),
        "as_of": as_of,
        "sources": sources,
        "files": {"weekly": weekly_path, "scores": "scores_all.json", "track": "track.json"},
    })
    failing = [k for k, v in sources.items() if not v["ok"]]
    print("daily done; failing:", failing or "none")
    return 0


def next_saturday() -> str:
    today = date.fromisoformat(today_jst())
    return (today + timedelta(days=(5 - today.weekday()) % 7 or 7)).isoformat()


def weekly() -> int:
    companies = company_list.load()
    store = read_json(STATE / "disclosures.json", {}) or {}
    week_ago = (date.fromisoformat(today_jst()) - timedelta(days=7)).isoformat()
    recent = [d for day, items in store.items() if day > week_ago for d in items]

    alias = trending.aliases()
    names = trending.match_names(companies)
    counts, coverage = {}, {}
    for code in trending.candidates(companies, recent):
        result = news.coverage(companies[code]["name"], alias.get(code), trending.headline_filter(code, names))
        counts[code], coverage[code] = result["count"], result
    picks = trending.pick(counts, companies)

    day = today_jst()
    folder = ROOT / "dossiers" / day
    for code in picks:
        c = companies[code]
        write_json(folder / f"{code}.json", {
            "code": code,
            "name": c["name"],
            "sector": c["sector"],
            "market": c["market"],
            "media_count": counts[code],
            "news": news.group_stories(coverage[code]["articles"], limit=5),
            "headlines": [a["title"] for a in coverage[code]["articles"][:20]],
            "disclosures": [d for d in recent if d["code"] == code][:10],
            # Filled once the keyed sources run: price, PER/PBR vs sector, fundamentals, large holdings
            "calc": {"fundamental": None, "investor": None},
        })
    write_json(folder / "picks.json", {"date": day, "codes": picks, "counts": {c: counts[c] for c in picks}})
    print("weekly picks:", [(c, companies[c]["name"], counts[c]) for c in picks])
    return 0


def merge(day: str) -> int:
    """Combine dossiers and the routine's evaluations into out/weekly/<day>.json and point index.json at it."""
    folder = ROOT / "dossiers" / day
    picks = read_json(folder / "picks.json")
    evaluations = read_json(ROOT / "evaluations" / f"{day}.json")
    errors = validate_evaluations(evaluations, picks["codes"] if picks else [])
    if errors:
        print("evaluations rejected:\n" + "\n".join(errors), file=sys.stderr)
        return 1

    index = read_json(OUT / "index.json", {}) or {}
    previous = read_json(OUT / index.get("files", {}).get("weekly", "weekly/latest.json"), {}) or {}
    before = {i["code"]: i for i in previous.get("items", [])}

    by_code = {e["code"]: e for e in evaluations["items"]}
    items = []
    for rank, code in enumerate(picks["codes"], 1):
        dossier = read_json(folder / f"{code}.json")
        e = by_code[code]
        old = before.get(code)
        if old is None:
            badge = "新"
        elif old["power"] != e["power"]:
            badge = f"力 {old['power']}→{e['power']}"
        elif old["cheapness"] != e["cheapness"]:
            badge = f"割安さ {old['cheapness']}→{e['cheapness']}"
        else:
            badge = ""
        items.append({
            "rank": rank, "code": code, "name": dossier["name"], "sector": dossier["sector"],
            "media_count": dossier["media_count"], "badge": badge,
            "power": e["power"], "cheapness": e["cheapness"],
            "headline": e["headline"], "summary": e["summary"],
            "weakness": e["weakness"], "experts": e["experts"],
            "calc": dossier["calc"],
            "news": [{**n, "gist": gist} for n, gist in zip(dossier["news"][:3], e.get("news_gists", []) + [""] * 3)],
            "disclosures": [{"date": d["date"], "title": d["title"], "url": d["url"]}
                            for d in dossier["disclosures"] if d.get("important")][:3],
        })
    weekly_path = f"weekly/{day}.json"
    write_json(OUT / weekly_path, {"date": day, "next_date": (date.fromisoformat(day) + timedelta(days=7)).isoformat(), "items": items})
    index.setdefault("files", {})["weekly"] = weekly_path
    write_json(OUT / "index.json", index)
    print("published", weekly_path)
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="kabu")
    sub = parser.add_subparsers(dest="job", required=True)
    sub.add_parser("daily")
    sub.add_parser("weekly")
    merge_parser = sub.add_parser("merge")
    merge_parser.add_argument("day")
    args = parser.parse_args(argv)
    if args.job == "daily":
        return daily()
    if args.job == "weekly":
        return weekly()
    return merge(args.day)


if __name__ == "__main__":
    sys.exit(main())
