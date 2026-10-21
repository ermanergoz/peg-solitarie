# Store listing — Peg Solitaire 3.0.0 (Google Play, `com.erman.pegsolitarie`)

Drafted by the release-manager on 2026-10-18 for Q-016 (founder, 2026-10-17): the studio drafts the
short and full descriptions and the release notes, and the founder approves them at Gate 2. **Nothing
here is live.** The founder pastes it into the Play Console. A gated upload script,
`scripts/play-upload.sh` with `fastlane supply`, is planned to upload it once the publish approval
exists, but that script does not exist yet on `develop` or on any branch; it is owed on a `feature/*`
branch before Gate 2 (`docs/release-checklist.md` item 1.7 and the note under its §4).

The texts follow `fastlane supply`'s metadata layout, so the files can be uploaded as they are:

| File | Play Console field | Limit | Length |
|---|---|---|---|
| `en-US/title.txt` | App name | 30 | 13 |
| `en-US/short_description.txt` | Short description | 80 | 76 |
| `en-US/full_description.txt` | Full description | 4,000 | 2,222 |
| `en-US/changelogs/30000.txt` | Release notes ("What's new"), versionCode 30000 | 500 | 427 |
| `tr-TR/title.txt` | App name (Turkish translation) | 30 | 13 |
| `tr-TR/short_description.txt` | Short description (Turkish) | 80 | 71 |
| `tr-TR/full_description.txt` | Full description (Turkish) | 4,000 | 2,521 |
| `tr-TR/changelogs/30000.txt` | Release notes (Turkish), versionCode 30000 | 500 | 447 |

The tr-TR texts were drafted on 2026-10-21 for Q-022 (founder, 2026-10-18: the studio drafts tr-TR for
approval at Gate 2). They translate the en-US texts line for line, with one addition: the full
description ends with "Oyunun arayüzü İngilizcedir." ("The game's interface is in English."), because the
app has no Turkish strings (no `values-tr` on `develop` or any open branch) and the listing should not
suggest otherwise. The founder may delete that line at Gate 2. Button names are given with the app's
English label in brackets, e.g. "İpucu (Hint)", "Ayarlar'dan (Settings)", so a Turkish player can find
them. The title stays "Peg Solitaire": the live Turkish listing uses that title too (public page,
`hl=tr&gl=TR`, read 2026-10-21). "Piyon" (peg), "delik" (hole) and "tahta" (board) are the words the
founder's own live Turkish text uses.

Lengths are Unicode characters, counted without the file's final newline (`python3`, `len()`).

The App content answers (Data safety, Ads, Target audience, Content rating, Advertising ID) are drafted in
`app-content.md`.

## What the texts describe

Only what 3.0.0 ships: `CHANGELOG.md` §3.0.0, ROADMAP phases 6–15, `THIRD_PARTY.md`. Every line of the
release notes maps to a CHANGELOG bullet. Some lines depend on branches that have not merged yet. **If a
branch does not reach `release/3.0.0`, delete its lines at the release cut** (checklist item 2.4a).
`feature/phase-15-ux-refresh` merged into `develop` at c9228a9, so its lines (the "Learn by playing"
section, the game-controls bullet and the tutorial release-note line, and their tr-TR equivalents) are
no longer conditional:

| Branch | Full description | Release notes |
|---|---|---|
| `feature/candy-backdrop` (backdrop direction B, colour-blind palette) | the "colorblind palette" bullet; the "glossy new look" bullet | "A glossy new look and a colorblind palette option" |
| `feature/audio-review` | nothing (sound itself is on `develop`; this branch only changes the files) | nothing |

The same branches own these tr-TR lines. Delete them together with their en-US lines:

| Branch | tr-TR full description | tr-TR release notes |
|---|---|---|
| `feature/candy-backdrop` | the "Ayarlar'da renk körlüğüne uygun bir renk paleti seçeneği…" bullet; the "Parlak yeni bir görünüm…" bullet | "Parlak yeni bir görünüm ve renk körlüğüne uygun palet seçeneği" |
| `feature/audio-review` | nothing | nothing |

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

The tr-TR texts follow the same rules in Turkish: no şeker or şekerleme (candy, sugar), tatlı (sweet),
jöle or jel (jelly, gel), soda, ezmek (crush), destan (saga), kral (King), lezzetli, nefis, enfes or
ilahi (callouts), no "gibi", "benzeri", "esinlenen", "hayranları" or "tarzı" comparison, no "dokunuş"
(twist) phrase, no kart, iskambil or keçe (cards, felt). "Solitaire" appears only in "Peg Solitaire".
The English grep above also runs on them, so the Turkish copy avoids "gel" even inside other words
(no "geleneksel", "gelir"). Both commands must print nothing:

```sh
LC_ALL=en_US.UTF-8 grep -inE 'candy|crush|saga|sugar|sweet|jelly|soda|king|gel|twist|tasty|delicious|divine|like |inspired|fans of|style|activision|microsoft|tiffi|toffee|yeti|odus|cards?|felt' docs/store/tr-TR/*.txt docs/store/tr-TR/changelogs/*.txt
LC_ALL=en_US.UTF-8 grep -inE 'şeker|seker|bonbon|lolipop|karamel|çikolata|şurup|tatlı|tatli|jöle|jole|jel|soda|ezme|ezil|ezer|destan|kral|lezzet|nefis|enfes|leziz|ilahi|şahane|dokunuş|farklı bir yorum|bükül|gibi|benzer|esinlen|ilham|hayran|tarz|kart|iskambil|keçe|çuha|yeşil masa' docs/store/tr-TR/*.txt docs/store/tr-TR/changelogs/*.txt
```

## Languages on the live listing

Read from the public page on 2026-10-18, with `hl=` set to 16 languages:

- **en-US** is the default listing. Its text is the old app's ("This is a board game for one player…",
  updated 2026-02-18).
- **Turkish has its own text.** It is not a translation of the English ("Peg Solitaire, kendi kendine
  oynanır…", which ends "5 farklı oyun modu bulunmaktadır ve koyu tema desteklenir"). So a tr-TR
  translation is very likely in the Console. It describes the old app, so it has to be replaced or
  removed for 3.0.0, or Turkish players see the old copy. The founder chose a studio draft (Q-022,
  2026-10-18): it is in `tr-TR/`, for approval at Gate 2. The live Turkish title is "Peg Solitaire"
  (re-read 2026-10-21), so the draft keeps it.
- The other 14 languages tried (en-GB, de, fr, es, it, pt-BR, ru, ja, ko, zh-CN, ar, hi, nl, pl) show the
  English text, translated. The public page cannot tell whether these are translations in the Console or
  Play's automatic translation. The founder checks in Play Console → Grow → Store presence → Store
  listings → Translations. A saved translation there also describes the old app, and must be replaced or
  removed.

## Screenshots (captured later, from the release candidate)

Plain captures of the 3.0.0 release candidate on a phone, with no added art, text overlay or frame
(candy-backdrop verdict §3d; Play Metadata policy). Nothing is drawn behind a capture. They are
captured after the backdrop merges (the phase-15 refresh is on `develop` since c9228a9). Recommended, in listing order:

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
