# research-analyst report — turkish-locale: Turkish-capable heading font candidates (Q-038 follow-up)
Date: 2026-11-01
Branch: feature/turkish-locale (report only)
Skills loaded: ergoz-soft:standards (role: research-analyst; the snapshot is 0.6.2 against plugin 1.0.0, so the plugin copies were read in full: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md; the product's .claude/rules/lessons.md), research-open-source, research-competitive, market-research
Ran in parallel with: the android-game-dev's loop-1 fixes on this branch, in this worktree (none of its files touched)
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot carries 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- Founder's Q-038 answer: "Change the font for all languages." I prepared an offline specimen page with 12 cards: Fredoka as it is today (the gap shown as Android draws it), Fredoka with the five Turkish letters added, nine alternative faces, and Nunito as a no-new-file baseline. Each card renders the app's headings at the app's sizes and colours, in light and dark.
- Every face is SIL OFL 1.1, and none is Apache. Every alternative covers all 12 Turkish letters by a cmap read. **IP verdict: CLEAN for all** (no escalation). The rename rule applies only to Quicksand, Comfortaa and Varela Round, which declare Reserved Font Names, and only if they are subset.
- **Fredoka with composite Ğ ğ İ Ş ş is feasible and proven in a prototype.** It keeps the approved look exactly: no original glyph, advance or cmap entry changed, and all 237 English strings shape identically. It adds 908 bytes in total. It passes OTS and kerns like the base letters. Fredoka declares no RFN, so the modified build may keep the name.
- **Recommendation: Fredoka + Turkish composites.** It meets the answer's aim (one heading face in every language) at no visual cost. If the founder wants a new face anyway: Sour Gummy (closest proportions, 116.5 kB), then Baloo 2 (chunkiest, but 840 kB unless subset).
- Rejected on fit: Varela Round (one weight only), M PLUS Rounded 1c (6.96 MB, no 600 weight, murky upstream).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/.planning/reports/turkish-locale/research-analyst-fonts.md`: this report, the only file committed.
- `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/9dd8f198-64da-4c3c-9d0e-61f87d38dc05/scratchpad/font-specimen.html`: the specimen for the founder, 468,096 B, sha256 `7d2a6ac3…0930465`.
  - Title "Heading Font Candidates". 24 base64 WOFF2 subsets, so it works offline; no external scripts.
  - Colours are tokens on `:root`, with dark under `@media (prefers-color-scheme: dark)` guarded by `:root:not([data-theme="light"])` and again under `:root[data-theme="dark"]`; `body` has an explicit background.
  - 16px gutter. No horizontal overflow at 320 and 360 px. Not committed.
- Prototype fonts, not committed and not for shipping as they are:
  - `…/scratchpad/work/fredoka-tr/Fredoka-Medium-TR-dieresis.ttf` (50,388 B, sha256 `e8848dd6…5e32a0`)
  - `…/scratchpad/work/fredoka-tr/Fredoka-SemiBold-TR-dieresis.ttf` (50,352 B, `00359186…1ee38b`)
  - The `-anchor` builds beside them are the rejected İ placement.
- Prototype tooling in `…/scratchpad/tools/`: `build_fredoka_tr.py`, `verify_composite.py`, `coverage.py`, `anchors.py`, `build_specimen.py`. All are kept outside the download directories and run with `python -I`. The downloads sit in `…/scratchpad/fonts/<family>/`, one fresh directory each.

## Candidates

All files are the official distributions: the Google Fonts download manifest (`fonts.google.com/download/list?family=…`, static instances) and the Open Runde v1.0.1 GitHub release. The Fredoka download is byte-identical to the shipped `res/font` files (sha256 `8143bd54…`, `021e30b2…`).

**Coverage** is a fontTools cmap read of each file for ç Ç ğ Ğ ı İ ö Ö ş Ş ü Ü and for the 91 distinct characters of the app's `values/` and `values-tr/` strings. "90/91" everywhere means one missing character, U+2011 (see Open issues).

**Size** is the two files as they would ship unmodified (kB = 1,000 B). Today's Fredoka pair is 99.8 kB.

| Candidate | Licence (URL) | RFN | Coverage (cmap) | Size, 2 weights | IP verdict | Fit |
|---|---|---|---|---|---|---|
| Fredoka v2.001 (today) | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/fredoka/OFL.txt | none | Turkish **7/12**: missing Ğ ğ İ Ş ş; app 86/91 | 49.9 + 49.9 = 99.8 kB | CLEAN (2026-10-18 verdict) | reference: the gap |
| **Fredoka + TR composites** (prototype) | same; a Modified Version under OFL-1.1 | none: the name may stay (OFL-FAQ 5.6, 5.7, 3.8) | **12/12**; app 90/91 | 50.4 + 50.4 = 100.7 kB (+0.9 kB) | **CLEAN as a Modified Version** (conditions below) | **recommended** |
| Baloo 2 v1.700 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/baloo2/OFL.txt | none | 12/12; 90/91; has `latn/TRK`, `tnum` | 420.3 + 420.2 = 840.5 kB (Latin subset ≈ 114.9 kB, allowed) | CLEAN | viable: chunkiest; x-height 0.46 em vs Fredoka's 0.50 |
| Sour Gummy v1.000 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/sourgummy/OFL.txt | none | 12/12; 90/91 | 58.2 + 58.3 = 116.5 kB | CLEAN | viable: same cap/x-height as Fredoka (0.70/0.50 em) |
| Quicksand v3.006 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/quicksand/OFL.txt | **"Quicksand"** | 12/12; 90/91 | 78.9 + 78.8 = 157.8 kB | CLEAN unmodified; a subset must be renamed (OFL §3) | viable, lighter strokes |
| Comfortaa v3.105 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/comfortaa/OFL.txt | **"Comfortaa"** | 12/12; 90/91 | 111.3 + 111.4 = 222.8 kB | CLEAN unmodified; subset → rename | possible: wide, distinctive |
| Varela Round v3.010 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/varelaround/OFL.txt | **"Varela", "Varela Round"** | 12/12; 90/91 | 132.2 kB (one file) | CLEAN unmodified; subset → rename | **not recommended**: Regular only, no 500/600 |
| M PLUS Rounded 1c v1.059 | OFL (google/fonts `METADATA.pb`; the download's OFL.txt has no copyright line; name ID 0 reads "Copyright 2016 The Rounded M+ Project Authors.") | none | 12/12; 91/91 | 3,423.5 + 3,533.3 kB = 6.96 MB | CLEAN, with a provenance caveat (upstream rated "Low" confidence by Google Fonts' `upstream_info.md`) | **not recommended**: size, 500/700 only |
| Mitr v1.003 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/mitr/OFL.txt | none | 12/12; 90/91 | 219.3 + 223.0 = 442.3 kB | CLEAN | possible: widest figures (0.74 em) |
| Rubik v2.300 | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/rubik/OFL.txt | none | 12/12; 90/91 | 208.2 + 208.2 = 416.4 kB | CLEAN | possible: less rounded, neutral |
| Open Runde v1.001 (OTF/CFF) | OFL-1.1, https://github.com/lauridskern/open-runde/blob/main/LICENSE.txt (Inter's copyright line; Open Runde is a renamed Inter derivative) | none in its licence | 12/12; 91/91 | 323.7 + 315.9 = 639.6 kB | CLEAN | possible: SF-Rounded-like; the author warns that the auto-rounding leaves some glyphs off |
| Nunito v3.602, 600/700 everywhere | OFL-1.1, https://github.com/google/fonts/blob/main/ofl/nunito/OFL.txt | none | 12/12; 90/91 | already shipped: +0 kB | CLEAN (2026-10-18) | baseline: no display face |

**How the matrix applies.** OFL-1.1 has no row in `rules/ip-policy.md`. I reuse the 2026-10-18 verdict, recorded in the THIRD_PARTY.md note and `.planning/reports/phase-14/research-analyst-font-licences.md`. OFL sits beside the MPL-2.0 row ("yes (file-level copyleft)"), with the copyleft limited to the font files. Bundling in a sold game is allowed (OFL-FAQ 1.4, 1.20), so no candidate is an escalation.

The conditions for whichever face is adopted:
- §1: the font is never sold alone.
- §2: copyright notice and licence text in every copy, as `assets/licenses/<Family>-OFL.txt`.
- §3: an RFN font must be renamed if subset or otherwise modified (OFL-FAQ 2.5, 2.6).
- §5: the font stays under OFL-1.1.

**Trademark.** No font name is used as product branding; the names appear only in attribution, so no trademark question arises.

**Upstream activity**, for the record (GitHub `pushed_at`):

| Family | Last push | Note |
|---|---|---|
| Open Runde | 2026-07-25 | |
| Sour Gummy | 2024-09-04 | |
| Nunito | 2025-02-26 | repo archived |
| Rubik | 2023-06-22 | repo archived |
| Varela Round | 2023-01-18 | |
| Quicksand | 2021-01-15 | |
| Fredoka | 2021-11-11 | |
| Baloo 2 | 2020-01-28 | |
| Comfortaa | 2017-11-08 | |
| Mitr | 2015-11-30 | |

Static font files need no maintenance, so none of these dates is a defect.

**No upstream Fredoka fix is coming.** The google/fonts issue #2411, "Fredoka One missing Latin Extended", has been open since 2020-04-07, with no linked PR.

## Fredoka + Turkish composites: feasibility and evidence

**Why it works.** Fredoka already draws the needed parts: G g I S s, the combining breve `uni0306`, the dot above `uni0307` and the cedilla `uni0327`. Its own accented letters are composites of exactly this kind: Ç = C + `uni0327` at (233, 13), Š = S + `uni030C` at (42, 200), Ö = O + `uni0308` at (169, 200). Capitals carry the mark +200 units, and the cap height is 700 against an x-height of 500.

**Placement rules**, all taken from the font itself:
- **Breve:** the GPOS mark-to-base anchors. For symmetric marks they match the font's own composites within 1–2 units: every circumflex, caron and dieresis (Ö exactly).
- **İ dot:** sits on the row of Fredoka's own Ï dots (ink bottom 756), centred on the stem. The anchor placement floats the dot about 92 units higher, to 973 (Medium) and 999 (SemiBold); the SemiBold top exceeds the font's 974 ascent. Rendered side by side, the anchor version looks wrong next to Ö Ü; it is rejected.
- **Cedilla:** it has no GPOS anchor, which is why a shaper composing Ş from S + U+0327 leaves the cedilla detached; HarfBuzz rendered exactly that defect from the original file. The cedilla's neck is centred under the letter's lowest point, with the same stroke overlap as Fredoka's own Ç (caps) and ç (lower case).

| Glyph | Components | Medium offset | SemiBold offset |
|---|---|---|---|
| Gbreve U+011E | G + uni0306 | (191, 200) | (187, 200) |
| gbreve U+011F | g + uni0306 | (103, 0) | (99, 0) |
| Idotaccent U+0130 | I + uni0307 | (6, 100) | (−6, 100) |
| Scedilla U+015E | S + uni0327 | (137, 8) | (125, 10) |
| scedilla U+015F | s + uni0327 | (106, −4) | (96, −4) |

Each new glyph gets:
- the base letter's advance,
- lsb equal to the composite's xMin,
- GDEF class "base",
- the base letter's component flags pattern, the same as the font's own composites,
- every pair-kerning entry and class of its base letter (201 entries copied per weight).

**Verified** on both weights:
- **Coverage:** the cmap now covers 12/12 Turkish letters and 90/91 app characters. Each of the five letters shapes to one glyph (gids 327–331, no decomposition).
- **Kerning:** identical to the base letters, e.g. `Sy` and `Şy` both advance 510, `Gy` and `Ğy` 667, `gy` and `ğy` 535.
- **Original font untouched:** `verify_composite.py` finds no original glyph with a changed outline or metrics, no cmap entry removed or remapped, and exactly five cmap additions. GSUB, OS/2, STAT, gasp and prep are byte-identical; hhea differs only in `numberOfHMetrics` (327 → 332). Line metrics therefore do not move.
- **English unchanged:** all 237 English strings shape byte-identically, glyph ids and advances, in the original and the prototype.
- **OTS:** `ots-sanitize` passes both files. As a control, it rejects a truncated copy with exit 1.
- **Renders:** `hb-view` renders at 72–260 px (`…/scratchpad/work/render/`). The breve sits like the caron on Š, the İ dot aligns with Ö Ü Ï, and the Ş/ş cedillas match Ç/ç. The specimen's Chrome rendering shows the same.
- **Two side effects of fontTools' recalculation**, both pre-existing quirks of the original Fredoka and not caused by the new glyphs:
  - `head.yMax` 974 → 975 in Medium: the original's Aring already reached 975.
  - `head.flags` bit 1 cleared: three original glyphs (Iacute, ellipsis, guillemotleft) already had lsb one unit off xMin.
  - A production build may restore the original values to stay byte-close. Neither affects line metrics.

**RFN and licence rule for the modified build.** Fredoka's OFL.txt and name ID 0 declare no Reserved Font Name, and no name is reserved by default (OFL-FAQ 5.7). A name change is needed only "if there are declared RFNs" (3.8), so the build may stay "Fredoka". Conditions:
- keep the copyright notice and the OFL text (the existing `assets/licenses/Fredoka-OFL.txt` covers it);
- distribute the build under OFL-1.1 (§5);
- mark the modification. The prototype appends to name IDs 3 and 5 ("…; TR composites (prototype)"); the shipped build should say what it is.
- THIRD_PARTY.md's Fredoka row then reads "modified: five composite glyphs added (Ğ ğ İ Ş ş), built by <script>", with input and output sha256.

**What adoption would take.** Nothing here is done; product code and assets belong to the developer after the ruling (standing rules 1 and 4).
- **tech-lead:** rules the integration. D-16's M-1 bool goes back to `true`, the path pre-ruled in tech-design §11.1 for a Q-038 answer 3. The tech-lead also decides whether `BrandTypography` stays and where the build script lives.
- **android-game-dev:**
  - commits the script with pinned input hashes, and the two outputs in place of the shipped files (same `R.font` ids);
  - turns `BundledFontGlyphCoverageTest`'s Fredoka check into a full Turkish plus app-text check, with U+2011 excepted as for Nunito;
  - removes the Turkish-only Nunito heading scale if the tech-lead so rules.
- **code-auditor:** reads the script and re-runs the verification (OTS, unchanged original glyphs, English shaping parity).
- **qa-engineer:** spec §8 C-2 on a device in Turkish, where Minikin, not Chrome, is the judge.
- **research-analyst:** writes the THIRD_PARTY.md row at adoption.

## Recommendation
**Fredoka + Turkish composites**, for three reasons:
- It meets the founder's answer: one heading face in every language. Turkish no longer differs from English, and D-16's split goes away.
- Nothing approved changes. English is byte-identical in shaping, and +908 B is the whole cost.
- The licence is clean. With no RFN, the build needs no new licence text, no new file family and no 15e startup risk from a new or larger font. The same two `R.font` resources stay.

If the founder wants a different face anyway:
- **Sour Gummy:** Fredoka's exact proportions, softer and bouncier shapes, 116.5 kB, no RFN.
- **Baloo 2:** the chunkiest; reads smaller. Subset it to Latin (≈ 114.9 kB; allowed, no RFN) rather than shipping 840 kB of Devanagari.

Any new face also needs, from the tech-lead and android-game-dev: new oracles (T1/T2), the brand-surface discriminators, a 15e sitting and the licence text in the APK.

### Founder question: draft for the producer to queue (the id is the producer's to assign)

    ## Q-<NNN> — Heading font: pick from the specimen   (open)
    Date: 2026-11-01    Asked by: research-analyst (via the producer)
    Context: Following Q-038 ("change the font for all languages"), font-specimen.html shows the app's headings in 12 options, light and dark. Fredoka can keep its exact look and gain the five missing Turkish letters (Ğ ğ İ Ş ş) as composites built from its own parts: +0.9 kB, licence-clean, English unchanged.
    Options: 1) Fredoka + Turkish letters (same look, every language) 2) Sour Gummy 3) Baloo 2 4) another face from the page 5) Nunito headings everywhere (no new file)
    Recommendation: research-analyst — 1. One heading face in every language, the approved look kept exactly, no new font family; a device check by QA confirms it before release.
    Blocks: nothing
    Answer:

## Checks
`SP` below is `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/9dd8f198-64da-4c3c-9d0e-61f87d38dc05/scratchpad`; `PY` is `$SP/tools/venv/bin/python` (fontTools 4.60.1, Pillow 13.0.0, opentype-sanitizer). HarfBuzz is 14.4.0, Chrome 156.0.8078.79.
- `curl -s "https://fonts.google.com/download/list?family=<Family>"` for 10 families, then `curl -f` of the listed static files into `$SP/fonts/<family>/`, plus the Open Runde v1.0.1 release zip — pass. All files were fetched; the OFL texts were taken from each manifest.
- `shasum -a 256 $SP/fonts/fredoka/*.ttf app/src/main/res/font/fredoka_*.ttf` — pass: the download equals the shipped files (`8143bd54…`, `021e30b2…`). The Fredoka OFL.txt equals `assets/licenses/Fredoka-OFL.txt` (`782c3137…`).
- `$PY -I $SP/tools/app_charset.py … values/strings.xml values-tr/strings.xml` — pass: 91 distinct characters, 18 of them non-ASCII.
- `$PY -I $SP/tools/coverage.py app_charset.txt coverage.json <23 font files>` — pass:
  - Fredoka misses exactly Ğ ğ İ Ş ş (and U+2011).
  - Every other file covers 12/12 Turkish; all lack only U+2011 except M PLUS and Open Runde (91/91).
- `head -1` of each OFL.txt and `grep -c 'Reserved Font Name'` — pass: RFNs only in Quicksand, Comfortaa and Varela Round. The google/fonts OFL.txt URLs return 200, except M PLUS, which has none (`METADATA.pb` says `license: "OFL"`).
- WebFetch of https://openfontlicense.org/ofl-faq/ (OFL-FAQ 1.1-update7) — items 5.6, 5.7, 3.8, 2.5, 2.6, 1.4 and 1.20 cited above.
- WebFetch of https://github.com/google/fonts/issues/2411 — open since 2020-04-07, no fix linked.
- `$PY -I $SP/tools/anchors.py <Fredoka-Medium|SemiBold>.ttf` — pass: anchors match the font's own symmetric-mark composites within 2 units; the cedilla has no anchor.
- `$PY -I $SP/tools/build_fredoka_tr.py <in> <out> --idot=dieresis|anchor` × 2 weights — pass: 5 glyphs added, 201 kerning entries copied per weight.
- `hb-shape --no-glyph-names` of 'YŞ', 'Şy', 'Ğy', 'şy', 'ğy' and their base pairs on the prototype — pass: identical advances.
- `$PY -I -m ots <prototype>.ttf /dev/null` — pass ×2 ("File sanitized successfully!"); a truncated copy fails with exit 1.
- `$PY -I $SP/tools/verify_composite.py <original> <prototype>` × 2 — pass: no original glyph changed; only five cmap additions.
- `hb-shape` of all 237 English strings with the original and the prototype — pass: 0 differences per weight.
- `hb-view` renders (NFD shaping of the original, composites, words, 260 px zoom), each read by eye — pass. The dieresis-row İ is kept; the anchor İ is rejected.
- `$PY -I $SP/tools/build_specimen.py $SP` — pass: 468,096 B, 24 faces.
- Headless Chrome `--dump-dom` probe at 1200 px and in 360 px and 320 px iframes — pass: `scrollWidth == clientWidth` at each width, 24/24 faces `loaded`, no element past the right edge.
- Headless Chrome screenshots (desktop dark page, forced light page, phone 360 px, 2× zoom of the "today" and composite cards), each read by eye — pass. The "today" card draws Ş ğ İ in the system font, as a Turkish Android phone does, and the composite card keeps them in Fredoka.
- Page rules scan — pass: no hex, rgb or hsl colour outside `:root` blocks; title is 3 words; one inline script with no `src`; no external resource; both dark-mode guards present; `body` background set.
- `./gradlew check` — not run: this task commits a report only and changes no code or resource.

## Open issues / risks
- **The device is the judge, not Chrome.** Android's Minikin picks fonts by cmap, which the cmap reads predict, but the composite build still needs QA's C-2 read on a phone. Line breaks and anti-aliasing on the page may differ slightly from the device.
- **U+2011 (non-breaking hyphen) is missing from Nunito, the shipped body face, and from most candidates.** The app uses it once, in the English `licenses_psl_parts` ("2023‑02‑15"), a body string that already falls back to Roboto. This predates this branch and touches no heading. Through the producer, the ux-designer and tech-lead may want a plain hyphen there; it is not mine to change.
- **The prototype is a binary patch, not a source build.** That is deliberate: rebuilding from upstream sources with a current fontmake would change far more than five glyphs. The script, its pinned input hashes and `verify_composite.py` make it reproducible and reviewable. The tooling lives only in the session scratchpad; the offsets table above is the recipe of record if the scratchpad is gone.
- **Sour Gummy is young** (v1.000, upstream last pushed 2024-09). **Open Runde's** author warns the automatic rounding leaves some glyphs off. **M PLUS Rounded 1c's** upstream is uncertain. These are risks only if the founder picks one of them.
- **Commit trailer:** this commit carries the two lines the dispatch asked for (`Co-Authored-By: Claude Opus 5.5`, `Claude-Session:`), as the other commits on this branch do. The plugin's rules/gitflow.md 1.0.0 names a different single trailer (tech-design §16 #37); the producer owns the discrepancy.
- **The research-open-source and research-competitive skills' intake gates were not run.** They ask the user questions (AskUserQuestion) and save to `artifacts/research/`. Only the producer addresses the founder (studio.md, Hierarchy), and the dispatch allows exactly one file in this worktree. This report is the record.

## Handoff
Next: producer — show `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/9dd8f198-64da-4c3c-9d0e-61f87d38dc05/scratchpad/font-specimen.html` to the founder and queue the drafted question (non-blocking). Route the answer:
- tech-lead: the integration ruling (for option 1: D-16's bool back to `true`, the script's home, `BrandTypography`'s fate);
- android-game-dev: the build after the ruling;
- code-auditor: the script and font review;
- qa-engineer: C-2 on a device;
- research-analyst: the THIRD_PARTY.md row at adoption.
Pass the U+2011 note to the ux-designer.
