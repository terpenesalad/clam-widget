"""Fetch CLAM's Steam wishlist numbers and write data/wishlists.json for the phone widget.

Runs inside GitHub Actions. Needs two repository secrets:
  STEAM_FINANCIAL_KEY  - Steamworks Financial API key
  STEAM_APP_ID         - CLAM's Steam App ID
"""

import json
import os
import time
import urllib.parse
import urllib.request
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

API = "https://partner.steam-api.com/IPartnerFinancialsService/GetAppWishlistReporting/v001/"
TITLE = "CLAM WISHLISTS"
LOCAL_TZ = ZoneInfo("Australia/Sydney")
REFETCH_DAYS = 7  # Steam sometimes revises recent days, so re-check the last week

KEY = os.environ["STEAM_FINANCIAL_KEY"].strip()
APP_ID = os.environ["STEAM_APP_ID"].strip()

DATA_DIR = Path("data")
HISTORY_FILE = DATA_DIR / "history.json"
OUTPUT_FILE = DATA_DIR / "wishlists.json"


def fetch(day: date) -> dict:
    query = urllib.parse.urlencode({"key": KEY, "appid": APP_ID, "date": day.isoformat()})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(f"{API}?{query}", timeout=30) as resp:
                return json.load(resp).get("response", {})
        except Exception as err:  # retry on rate limits / blips
            if attempt == 3:
                raise RuntimeError(f"Steam request failed for {day}: {err}") from None
            time.sleep(2 * (attempt + 1))
    return {}


def net(entry: dict) -> int:
    # Outstanding wishlists = adds - deletes - purchases - gifts
    return (
        entry.get("adds", 0)
        - entry.get("deletes", 0)
        - entry.get("purchases", 0)
        - entry.get("gifts", 0)
    )


def signed(n: int) -> str:
    return f"+{n:,}" if n >= 0 else f"{n:,}"


def main() -> None:
    DATA_DIR.mkdir(exist_ok=True)
    history = json.loads(HISTORY_FILE.read_text()) if HISTORY_FILE.exists() else {}

    yesterday = datetime.now(timezone.utc).date() - timedelta(days=1)  # Steam uses GMT dates

    first = fetch(yesterday)
    if not first:
        raise SystemExit(
            "Steam returned an empty response. Check the key is a Financial API key "
            "and the App ID is correct."
        )
    min_date = date.fromisoformat(first.get("app_min_date", yesterday.isoformat()))

    recent_cutoff = yesterday - timedelta(days=REFETCH_DAYS)
    debug = {}
    day = min_date
    while day <= yesterday:
        key = day.isoformat()
        if key not in history or day > recent_cutoff:
            resp = first if day == yesterday else fetch(day)
            summary = resp.get("wishlist_summary")
            if day > recent_cutoff:
                debug[key] = resp
            if summary is not None:
                history[key] = {
                    "adds": summary.get("wishlist_adds", 0),
                    "deletes": summary.get("wishlist_deletes", 0),
                    "purchases": summary.get("wishlist_purchases", 0),
                    "gifts": summary.get("wishlist_gifts", 0),
                }
            if day != yesterday:
                time.sleep(0.3)
        day += timedelta(days=1)

    (DATA_DIR / "debug.json").write_text(json.dumps(debug, indent=1))
    HISTORY_FILE.write_text(json.dumps(dict(sorted(history.items())), indent=1))

    dates = sorted(history)
    if not dates:
        raise SystemExit("No wishlist data available yet.")
    latest = date.fromisoformat(dates[-1])
    total = sum(net(v) for v in history.values())
    latest_net = net(history[dates[-1]])
    week_net = sum(
        net(v) for k, v in history.items() if date.fromisoformat(k) > latest - timedelta(days=7)
    )

    output = {
        "title": TITLE,
        "total": total,
        "total_text": f"{total:,}",
        "latest_day": latest.isoformat(),
        "latest_day_text": f"{latest.day} {latest.strftime('%b')}",
        "latest_change": latest_net,
        "latest_change_text": signed(latest_net),
        "week_change": week_net,
        "week_change_text": signed(week_net),
        "checked_at": datetime.now(LOCAL_TZ).strftime("%H:%M"),
    }
    OUTPUT_FILE.write_text(json.dumps(output, indent=2))
    print(json.dumps(output, indent=2))


if __name__ == "__main__":
    main()
