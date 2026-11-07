"""Counts how many different outlets covered a company in the past week, from Google News search RSS."""
from __future__ import annotations

import time
import xml.etree.ElementTree as ET
from collections.abc import Callable
from email.utils import parsedate_to_datetime
from urllib.parse import urlparse

from .common import CONFIG, JST, get, read_json

RSS = "https://news.google.com/rss/search"
# Google News search pauses: keep well under any limit
PAUSE_SECONDS = 1.6


def search(query: str) -> list[dict]:
    params = {"q": f"{query} when:7d", "hl": "ja", "gl": "JP", "ceid": "JP:ja"}
    root = ET.fromstring(get(RSS, params=params).content)
    out = []
    for item in root.iter("item"):
        source = item.find("source")
        published = item.findtext("pubDate")
        out.append({
            "title": (item.findtext("title") or "").strip(),
            "url": item.findtext("link") or "",
            "outlet": (source.text or "").strip() if source is not None else "",
            "domain": urlparse(source.get("url", "")).netloc.removeprefix("www.") if source is not None else "",
            "date": parsedate_to_datetime(published).astimezone(JST).date().isoformat() if published else "",
        })
    return out


def _excluded_domains() -> set[str]:
    return set(read_json(CONFIG / "news_excluded_domains.json", []))


def coverage(name: str, aliases: list[str] | None = None, about: Callable[[str], bool] | None = None) -> dict:
    """Distinct outlets that wrote about the company, minus reposting sites, press-release wires and auto-written stock pages.

    [about] decides whether a headline is really about this company (see trending.headline_filter).
    """
    excluded = _excluded_domains()
    about = about or (lambda title: name_in(title, name, aliases))
    articles = []
    for query in [f'"{name}"', *(f'"{a}"' for a in aliases or [])]:
        articles += search(query)
        time.sleep(PAUSE_SECONDS)
    kept = [a for a in articles if a["domain"] and a["domain"] not in excluded and about(a["title"])]
    outlets = {}
    for a in sorted(kept, key=lambda a: a["date"], reverse=True):
        outlets.setdefault(a["domain"], a)
    return {"count": len(outlets), "articles": list(outlets.values()), "capped": len(articles) >= 100}


def name_in(title: str, name: str, aliases: list[str] | None) -> bool:
    """The headline really names the company (Google matches loosely)."""
    return any(n in title for n in [name, *(aliases or [])])


def group_stories(articles: list[dict], limit: int = 3) -> list[dict]:
    """Rough story groups: headlines sharing a long chunk of text count as one story with several outlets."""
    stories: list[dict] = []
    for a in articles:
        key = a["title"].split(" - ")[0]
        for s in stories:
            if _similar(s["title"], key):
                s["outlets"].append(a["outlet"])
                break
        else:
            stories.append({"title": key, "date": a["date"], "url": a["url"], "outlets": [a["outlet"]]})
    stories.sort(key=lambda s: len(s["outlets"]), reverse=True)
    return [
        {"count": len(s["outlets"]), "date": s["date"], "title": s["title"], "url": s["url"],
         "outlets": "・".join(s["outlets"][:2]) + (f" ほか{len(s['outlets']) - 2}" if len(s["outlets"]) > 2 else "")}
        for s in stories[:limit]
    ]


def _similar(a: str, b: str, size: int = 8) -> bool:
    chunks = {a[i:i + size] for i in range(max(1, len(a) - size + 1))}
    return any(b[i:i + size] in chunks for i in range(max(1, len(b) - size + 1)))
