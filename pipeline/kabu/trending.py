"""Picks the week's 10 most-covered stocks, at most 5 per industry. Plain code, so the pick can be reproduced."""
from __future__ import annotations

import collections
import re

from . import news
from .common import CONFIG, read_json
from .companies import industry_group

# Broad searches whose headlines name many companies; the names found become candidates
BROAD_QUERIES = ["株価 急騰", "株価 急落", "ストップ高", "ストップ安", "上方修正", "下方修正",
                 "TOB", "決算 発表 株", "自社株買い", "増配", "年初来高値", "年初来安値"]
CANDIDATES = 60
PICKS = 10
PER_INDUSTRY = 5


def aliases() -> dict[str, list[str]]:
    """Other names news uses for a company (code → names), from config/aliases.json."""
    return read_json(CONFIG / "aliases.json", {})


def find_companies(headline: str, names: dict[str, str]) -> list[str]:
    """Codes of the companies a headline names. Longest names win, so ヤマハ発動機 isn't also counted as ヤマハ."""
    found, taken = [], []
    for code, name in sorted(names.items(), key=lambda kv: len(kv[1]), reverse=True):
        start = headline.find(name)
        if start < 0:
            continue
        span = range(start, start + len(name))
        if any(set(span) & set(t) for t in taken):
            continue
        taken.append(span)
        found.append(code)
    return found


def match_names(companies: dict[str, dict]) -> dict[str, str]:
    """Names safe to look for in headlines: 3+ characters, plus listed aliases; ambiguous words are skipped."""
    skip = set(read_json(CONFIG / "ambiguous_names.json", []))
    names = {code: c["name"] for code, c in companies.items() if len(c["name"]) >= 3 and c["name"] not in skip}
    for code, extra in aliases().items():
        for i, alias in enumerate(extra):
            names[f"{code}#{i}"] = alias
    return names


# A short katakana name (ライオン, ヒップ) is also an everyday word: count only business headlines
SHORT_KANA = re.compile(r"[ァ-ヶー・]{1,4}")
BUSINESS = re.compile(r"株|決算|業績|売上|利益|営業益|純利益|増益|減益|社長|上場|買収|提携|増配|減配|億円|発表|修正|新製品|出店|工場")


def headline_filter(code: str, names: dict[str, str]):
    """True when a headline names this company, not a longer name that contains it (ヤマト in ヤマト運輸)
    and, for a name that is also an everyday word, when the headline is about business."""
    name = names.get(code, "")
    short = bool(SHORT_KANA.fullmatch(name))

    def about(title: str) -> bool:
        if code not in {c.split("#")[0] for c in find_companies(title, names)}:
            return False
        return not short or bool(BUSINESS.search(title))
    return about


def candidates(companies: dict[str, dict], disclosures: list[dict]) -> list[str]:
    """About 60 codes worth counting properly: named in broad market news, or with an important announcement this week."""
    names = match_names(companies)
    score: collections.Counter[str] = collections.Counter()
    for query in BROAD_QUERIES:
        for article in news.search(query):
            for code in find_companies(article["title"], names):
                code = code.split("#")[0]
                if headline_filter(code, names)(article["title"]):
                    score[code] += 2
    for d in disclosures:
        if d["important"] and d["code"] in companies:
            score[d["code"]] += 1
    return [code for code, _ in score.most_common(CANDIDATES)]


def pick(counts: dict[str, int], companies: dict[str, dict]) -> list[str]:
    """Top codes by outlet count, skipping any industry that already has 5."""
    per_group: collections.Counter[str] = collections.Counter()
    out = []
    for code in sorted(counts, key=lambda c: (-counts[c], c)):
        group = industry_group(code, companies[code]["sector"])
        if per_group[group] >= PER_INDUSTRY:
            continue
        per_group[group] += 1
        out.append(code)
        if len(out) == PICKS:
            break
    return out
