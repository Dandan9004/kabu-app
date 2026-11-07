"""Company announcements from the free TDnet list pages (Shift_JIS HTML, last 31 days, 100 per page)."""
from __future__ import annotations

import html
import re
from datetime import date

from .common import code4, get

BASE = "https://www.release.tdnet.info/inbs/"
ROW = re.compile(
    r'kjTime"[^>]*>(?P<time>[\d:]+)</td>.*?kjCode"[^>]*>(?P<code>\w+)</td>.*?kjName"[^>]*>(?P<name>.*?)</td>'
    r'.*?kjTitle"[^>]*>\s*<a href="(?P<pdf>[^"]+)"[^>]*>(?P<title>.*?)</a>',
    re.S,
)
TOTAL = re.compile(r"全(\d+)件")

# Titles that move a stock: forecast changes, results, buybacks, tender offers, dividends
IMPORTANT = re.compile(r"業績予想|決算短信|修正|自己株式|公開買付|配当|上場廃止|特別損失|合併|株式分割|資本業務提携")
CUT = re.compile(r"下方修正|減配|特別損失|上場廃止|不正|赤字")


def parse_page(text: str, day: date) -> list[dict]:
    out = []
    for m in ROW.finditer(text):
        title = html.unescape(re.sub(r"<[^>]+>", "", m["title"])).strip()
        out.append({
            "date": day.isoformat(),
            "time": m["time"],
            "code": code4(m["code"]),
            "name": html.unescape(m["name"]).strip(),
            "title": title,
            "url": BASE + m["pdf"],
            "important": bool(IMPORTANT.search(title)),
            "bad": bool(CUT.search(title)),
        })
    return out


def fetch_day(day: date) -> list[dict]:
    items, page = [], 1
    while True:
        response = get(f"{BASE}I_list_{page:03d}_{day:%Y%m%d}.html")
        if response.status_code == 404:
            break
        # The pages are UTF-8 today; older ones were Shift_JIS
        try:
            text = response.content.decode("utf-8")
        except UnicodeDecodeError:
            text = response.content.decode("cp932", errors="replace")
        rows = parse_page(text, day)
        items.extend(rows)
        total = TOTAL.search(text)
        if not rows or not total or len(items) >= int(total.group(1)):
            break
        page += 1
    return items
