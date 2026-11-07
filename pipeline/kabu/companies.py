"""The JPX list of listed companies: code, name, market and 33-sector name. ETFs, REITs and PRO Market are left out."""
from __future__ import annotations

import io
import unicodedata

import openpyxl

from .common import CONFIG, STATE, get, read_json, write_json

JPX_URL = "https://www.jpx.co.jp/markets/statistics-equities/misc/tvdivq0000001vg2-att/data_j.xlsx"
SKIP_MARKETS = ("ETF", "REIT", "PRO Market", "出資証券")


def parse(xlsx: bytes) -> dict[str, dict]:
    sheet = openpyxl.load_workbook(io.BytesIO(xlsx), read_only=True).active
    out = {}
    for row in list(sheet.iter_rows(values_only=True))[1:]:
        _date, code, name, market, _s33code, sector33, *_ = row
        if not code or not market or any(s in str(market) for s in SKIP_MARKETS) or sector33 in (None, "-"):
            continue
        # NFKC turns the list's full-width Latin letters (Ｖｅｒｉｔａｓ) into what news headlines use
        out[str(code)] = {"name": unicodedata.normalize("NFKC", str(name)).strip(), "market": str(market), "sector": str(sector33)}
    return out


def refresh() -> dict[str, dict]:
    companies = parse(get(JPX_URL).content)
    if len(companies) < 3000:
        raise RuntimeError(f"JPX list looks wrong: only {len(companies)} companies")
    write_json(STATE / "companies.json", companies)
    return companies


def load() -> dict[str, dict]:
    return read_json(STATE / "companies.json", {}) or refresh()


def industry_group(code: str, sector: str) -> str:
    """The group used for the 'max 5 per industry' rule: the 33 sectors, with semiconductor makers pooled."""
    semis = read_json(CONFIG / "semiconductor_codes.json", [])
    return "半導体関連" if code in semis else sector
