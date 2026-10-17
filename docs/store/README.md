# Store listing — Peg Solitaire 3.0.0 (Google Play, `com.erman.pegsolitarie`)

Drafted by the release-manager on 2026-10-18 for Q-016 (founder, 2026-10-17): the studio drafts the
short and full descriptions and the release notes, and the founder approves them at Gate 2. **Nothing
here is live.** The founder pastes it into the Play Console, or `scripts/play-upload.sh` uploads it
with `fastlane supply` once the publish approval exists.

The texts follow `fastlane supply`'s metadata layout, so the files can be uploaded as they are:

| File | Play Console field | Limit | Length |
|---|---|---|---|
| `en-US/title.txt` | App name | 30 | 13 |
| `en-US/short_description.txt` | Short description | 80 | 76 |
| `en-US/full_description.txt` | Full description | 4,000 | 2,222 |
| `en-US/changelogs/30000.txt` | Release notes ("What's new"), versionCode 30000 | 500 | 427 |

Lengths are Unicode characters, counted without the file's final newline (`python3`, `len()`).

The App content answers (Data safety, Ads, Target audience, Content rating, Advertising ID) are drafted in
`app-content.md`.

## What the texts describe

Only what 3.0.0 ships: `CHANGELOG.md` §3.0.0, ROADMAP phases 6–15, `THIRD_PARTY.md`. Every line of the
release notes maps to a CHANGELOG bullet. Three lines depend on branches that have not merged yet. **If a
branch does not reach `release/3.0.0`, delete its lines at the release cut** (checklist step 1.4):

| Branch | Full description | Release notes |
|---|---|---|
| `feature/phase-15-ux-refresh` | the "Learn by playing" section; the "Undo, Hint and the other game controls…" bullet | "A hands-on tutorial, a fuller Home screen and game controls within thumb reach" |
| `feature/candy-backdrop` (backdrop direction B, colour-blind palette) | the "colorblind palette" bullet; the "glossy new look" bullet | "A glossy new look and a colorblind palette option" |
| `feature/audio-review` | nothing (sound itself is on `develop`; this branch only changes the files) | nothing |

The backdrop is described only in neutral terms ("glossy", "soft light"), as Q-021's direction B requires.

## Word rules

Every text passes the research-analyst's word rules (`.planning/reports/candy-backdrop/research-analyst.md`,
§3a and addendum A.4, on `feature/candy-backdrop`):

- None of these words appears: candy, crush, saga, sugar, sweet, jelly, soda, King, gel, twist. No
  comparison, no "games like", "for fans of" or "inspired by". No celebration callouts.
- "Solitaire" appears only in the title "Peg Solitaire", the game's generic name. It is never next to a
  sweet word or a "twist" phrase. There are no playing cards and no green felt.
- No ranking, testimonial or price claim.
- The text is en-US, so it spells "neighboring" and "colorblind", which is also how the app's own
  setting is labelled.

Re-run the check after any edit, from the repo root. It must print nothing:

```sh
grep -inE 'candy|crush|saga|sugar|sweet|jelly|soda|king|gel|twist|tasty|delicious|divine|like |inspired|fans of|style|activision|microsoft|tiffi|toffee|yeti|odus|cards?|felt' docs/store/en-US/*.txt docs/store/en-US/changelogs/*.txt
```

## Languages on the live listing

Read from the public page on 2026-10-18, with `hl=` set to 16 languages:

- **en-US** is the default listing. Its text is the old app's ("This is a board game for one player…",
  updated 2026-02-18).
- **Turkish has its own text.** It is not a translation of the English ("Peg Solitaire, kendi kendine
  oynanır…", which ends "5 farklı oyun modu bulunmaktadır ve koyu tema desteklenir"). So a tr-TR
  translation is very likely in the Console. It describes the old app, so it has to be replaced or
  removed for 3.0.0, or Turkish players see the old copy. This draft has no Turkish text. The founder
  either writes it, asks the studio for a tr-TR draft, or deletes the translation.
- The other 14 languages tried (en-GB, de, fr, es, it, pt-BR, ru, ja, ko, zh-CN, ar, hi, nl, pl) show the
  English text, translated. The public page cannot tell whether these are translations in the Console or
  Play's automatic translation. The founder checks in Play Console → Grow → Store presence → Store
  listings → Translations. A saved translation there also describes the old app, and must be replaced or
  removed.

## Screenshots (captured later, from the release candidate)

Plain captures of the 3.0.0 release candidate on a phone, with no added art, text overlay or frame
(candy-backdrop verdict §3d; Play Metadata policy). Nothing is drawn behind a capture. They are
captured after the backdrop and the phase-15 refresh have merged. Recommended, in listing order:

1. **Home**, light theme: the title band, the progress ribbon, the Classic and Challenge cards and the
   backdrop.
2. **Classic, English-33, mid-game**, light theme: a selected peg with its legal targets and the action
   dock.
3. **Challenge level**, light theme, with the level chip and a hint shown on the board.
4. **Results**: three stars, the score and the best time.
5. **Classic catalogue**: the five boards with their star pips.
6. **Tutorial**, a step in progress.
7. **Gameplay in the dark theme.**
8. *(optional)* **Gameplay with the colorblind palette on.**

At least 4 are required; 1–4 are the minimum set. The Play Games sign-in, achievement and leaderboard
screens are Google's UI, so they are left out. An ad never appears in a capture.

## Other listing fields (unchanged here)

- Listing icon 512×512 and feature graphic 1024×500: studio own work, recorded in `THIRD_PARTY.md` §7.
  The feature graphic's text is "Peg Solitaire" and "Classic boards, endless fun!". It passes the word
  rules.
- Privacy policy URL: `https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` (live, last
  updated 2026-09-21).
- Website: `https://ermanergoz.github.io` (live on the listing since 2026-09-21).
- Category: Games → Puzzle on the live page (`store/apps/category/GAME_PUZZLE`, 2026-10-18). It stays.
  The live content rating reads "Everyone"; `app-content.md` expects the same result for 3.0.0.
