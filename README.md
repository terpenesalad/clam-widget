# CLAM Wishlists widget

A dark, minimal Android home-screen widget showing CLAM's Steam wishlist total.

GitHub checks Steam every 6 hours and saves the numbers to `data/wishlists.json`.
The KWGT app on your phone reads that file and shows it as a widget.

## 1. Get your Steam key (5 min)

1. Go to partner.steamgames.com → **Users & Permissions** → **Manage Groups**.
2. Click **Create new group** → choose **Financial API Group**.
3. Copy the key shown on the group page. Keep it private.
4. Note CLAM's App ID (the number in your Steam store page URL).

## 2. Set up GitHub (10 min)

1. On github.com, create a new **public** repository called `clam-widget`.
   (Public is needed so your phone can read the number. Your key stays hidden;
   only the wishlist numbers are visible.)
2. Upload these files, keeping the folder structure:
   - `update_wishlists.py`
   - `.github/workflows/update-wishlists.yml`
   - `README.md`
   Tip: on the upload page you can drag the whole unzipped folder in.
3. Go to **Settings** → **Secrets and variables** → **Actions** → **New repository secret**. Add two:
   - `STEAM_FINANCIAL_KEY` = your key
   - `STEAM_APP_ID` = CLAM's App ID
4. Go to **Settings** → **Actions** → **General** → **Workflow permissions** →
   choose **Read and write permissions** → **Save**.
5. Go to the **Actions** tab → **Update CLAM wishlists** → **Run workflow**.
   The first run downloads your full history, so it may take a few minutes.
6. When it's green, open `data/wishlists.json` and check the total looks right.

Your widget URL is:

```
https://raw.githubusercontent.com/YOUR_GITHUB_NAME/clam-widget/main/data/wishlists.json
```

## 3. Build the widget in KWGT (10 min)

1. Install **KWGT Kustom Widget Maker** from the Play Store.
2. Long-press your home screen → **Widgets** → **KWGT** → drag the **4x2** widget out.
3. Tap the empty widget to open the editor. Pick **Create** / empty.
4. **Background:** tap **+** → **Shape**. Set Width and Height to fill the widget,
   Corners **14**, Color **#1F1F1F** (the same dark grey as JellyMusic's cards).
5. Tap **+** → **Stack Group**. Inside it, set Orientation **Vertical**, Align **Left**,
   Padding **20**, and add three **Text** items:

| Text item | Text (paste exactly) | Font / size | Color |
|---|---|---|---|
| Label | `CLAM WISHLISTS` | Outfit Bold, 11, letter spacing 2 | `#8C8C8C` |
| Number | `$wg("YOUR_URL", json, ".total_text")$` | Outfit Bold, 52 | `#FFFFFF` |
| Change | `[c=#F5C04A]$wg("YOUR_URL", json, ".latest_change_text")$[/c] yesterday  ·  [c=#F5C04A]$wg("YOUR_URL", json, ".week_change_text")$[/c] this week` | Outfit Medium, 12 | `#8C8C8C` |

   The `[c=...]` bits make only the +numbers JellyMusic's warm yellow; everything
   else stays white and grey. Outfit is in KWGT's font picker under Google Fonts.
   If you can't find it, Montserrat is a close match.

   Replace `YOUR_URL` with your widget URL from step 2.
6. Tap **Save** (top right). Done.

## Good to know

- Steam publishes wishlist numbers once a day, for the previous day (GMT).
  So "yesterday" really means the latest full day Steam has reported.
- The total is calculated as adds − deletes − purchases − gifts since your
  store page went up. If it's slightly different from the Steamworks dashboard,
  let Claude know and it can be adjusted.
- If GitHub ever pauses the schedule (it can after long inactivity), open the
  **Actions** tab and re-enable it.
