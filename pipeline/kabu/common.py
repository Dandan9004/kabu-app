"""Shared helpers: HTTP with a browser-like name, paths, JST dates, JSON in and out."""
from __future__ import annotations

import json
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

import requests

JST = timezone(timedelta(hours=9))
ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "config"
# Everything published to GitHub Pages is written under out/ (the gh-pages branch). The working state kept
# between runs (company list, 30 days of announcements) rides along in out/_state/: public data only.
OUT = ROOT / "out"
STATE = OUT / "_state"

# JPX answers 403 without a browser-like User-Agent
HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) kabu-app-pipeline"}

_session = requests.Session()
_session.headers.update(HEADERS)


def get(url: str, *, params: dict | None = None, timeout: int = 30, tries: int = 3) -> requests.Response:
    """GET with a few retries; raises on the last failure."""
    for attempt in range(tries):
        try:
            response = _session.get(url, params=params, timeout=timeout)
            if response.status_code == 404:
                return response
            response.raise_for_status()
            return response
        except requests.RequestException:
            if attempt == tries - 1:
                raise
            time.sleep(2 * (attempt + 1))
    raise AssertionError("unreachable")


def now_jst() -> datetime:
    return datetime.now(JST)


def today_jst() -> str:
    return now_jst().date().isoformat()


def read_json(path: Path, default=None):
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, obj, *, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(obj, ensure_ascii=False, separators=(",", ":") if compact else None,
                      indent=None if compact else 1)
    path.write_text(text, encoding="utf-8")


def code4(raw) -> str:
    """JPX gives 1301 or '130A'; TDnet gives 5 characters ('13010'). Both become '1301' / '130A'."""
    text = str(raw).strip()
    return text[:4] if len(text) == 5 and text.endswith("0") else text
