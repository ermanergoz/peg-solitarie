# ux-designer report — 15-founder-feedback: the tech-lead's §6.5 font and 15e rulings folded in; D-03m checked by an agent that did not write it
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (one commit, the one that carries this report; the branch head was 51e7158 when this check began)
Skills loaded: ergoz-soft:standards (role ux-designer; rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose from the product's 0.5.1 snapshot, plus the plugin 0.6.2 copies of studio.md, reporting.md, compose.md and dispatch.md, which are newer), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: the developer's D-03m and font-bundle implementation in this worktree. I did not read its reports, and I touched no code.
Ran on: Opus 5.5 (claude-opus-5-5). The agent file pins fable at effort xhigh. **Staffing deviation:** Fable is exhausted, so this ran on Opus under the founder's Q-014 answer (`.planning/questions/FOUNDER.md`), which is scoped to phase 15. D-03m's author also ran on Opus. The check's independence therefore rests on this being a fresh agent that did not write D-03m, and on measuring the fonts with its own script rather than the author's.
Fan-out: none. There were two helpers, and neither names a studio role: a general-purpose font measurement (its own script, run on scratch copies) and a general-purpose commit of the two files.
Loops: 0
Lessons applied: none (the snapshot carries 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

Inputs read: `tech-lead-fonts.md`; `docs/ux-spec.md`; `docs/ui/DESIGN-SYSTEM.md` (§Typography only); `.planning/questions/FOUNDER.md`; `qa-engineer.md` (E-1, 15p and 15k rows, the 15e table). I also read `ux-designer-15p.md`, the author's report, because the dispatch named it. Standing rule 1 would normally keep an author's reasoning from its reviewer, so the verdict below rests on the spec text and on my own measurements, not on that report. Not read: `android-game-dev*.md`, `.planning/log/`.

## Summary
- **Task 1 (§6.5):** the tech-lead's wording is applied as given, dated 2026-09-29, with no decision rewritten in place. The pieces are: the font exception to Home's first-frame hard rules, with its table row and degradation note; preconditions 1, 4 and 7, keeping the superseded wording legible; criterion 15e; the §6.3 `tnum` note; §9's font row and opening paragraph; and 18d's discriminator clause. I also added a D-03g pointer and two reconciliations the ruling implies.
- **Task 2 (D-03m): FAIL on text only, fixed in this commit.** The ruling resolves 15p.1 on the visible reading. Its arithmetic holds in the static Fredoka SemiBold that will ship, measured independently. §13 and §16 are consistent, 15k is still coherent, and nothing contradicts D-03l, Q-004 or Q-005.
- What failed was coverage: seven passages still stated the retired 72dp row or the ≈72dp overflow as current. Beyond that, §15 still listed Q-005 as open, and the real-face check cites a fixture the font ruling deletes. All three are fixed with dated pointers.
- **Nothing in the developer's in-flight D-03m work has to change.**

## Task 1 — the tech-lead's rulings in §6.5 and around it
Everything in the tech-lead's report headed "Spec text for the ux-designer" is applied as given, except the `DESIGN-SYSTEM.md` text (see Open issues).

| Where | Change |
|---|---|
| §6.5 "Hard rules for Home's first frame" | The rules sentence now stands alone, words unchanged. The tech-lead's dated blockquote, "One named exception: the app's type faces", follows it verbatim, and then "Every visual this refresh adds is one of:". |
| §6.5 first-frame table | The tech-lead's "Type faces (Fredoka ×2, Nunito ×3, `res/font`)" row is added verbatim. |
| §6.5 degradation path | The tech-lead's "*(2026-09-29.)* If 15e fails on the build that first bundles the faces…" is appended after the last degradation paragraph, the D-03l amendment. |
| §6.5 precondition 1 | Appended, marked *(Added 2026-09-29.)*: "A sitting on any other AVD is VOID; the `Pixel_9` is not a substitute (tech-lead-fonts.md, Ruling 2)." |
| §6.5 precondition 4 | Replaced by the tech-lead's text: before/after readings at defined moments, 1-minute load ≤ half the logical cores, 5.0 here. The first wording stays beneath it, marked superseded, as the record. |
| §6.5 precondition 7 | Replaced by the tech-lead's text: arms swapped only at block boundaries, sha256 of `base.apk` after each swap, the null control swaps the same way, valid only while the Room schema and DataStore keys match. The first wording stays beneath it, marked superseded. |
| Criterion 15e | "both arms installed for the whole sitting" becomes the tech-lead's phrase, and the host-load clause is added after "…anywhere on the host". A dated note quotes the old phrase so it stays legible. |
| §6.3 | The tech-lead's dated note is appended verbatim: Fredoka has no `tnum`, and Nunito's digits are tabular at 600. |
| §9 | The tech-lead's "*(Amended 2026-09-29.)* The fonts are the exception…" follows the section's opening statement. I read "the section's first sentence" as the opening paragraph, because the amendment excepts that paragraph's bold claim, so it sits after that claim. The old font row is kept, marked "superseded 2026-09-29 by the next row". The tech-lead's bundled-fonts row follows it verbatim. |
| Criterion 18d | The tech-lead's optional clause is applied: "— the shipped font confirmed on that install by the discriminators in `tech-lead-fonts.md` —", dated in the trailing parenthesis. |
| D-03g (§3) | A dated one-line pointer to the exception, because house style gives the amended decision a pointer. |
| §16 | One dated line: PLAT-08 (C1) gains the faces as a named budget item, CLAS-02 and SCOR-02 gain 18d's clause, and one OFL-1.1 asset enters the artifact. |
| Front matter | A ninth-revision preamble. |

Two reconciliations that go beyond the tech-lead's text. Both are notes, and they only apply the ruling.
1. **Precondition 1 and 15e both say `:app:installDebug`, but precondition 3 forbids any Gradle process during the sitting.** Under Ruling 2's procedure the arms are built beforehand with `:app:assembleDebug` and installed at block boundaries with `adb install`. A dated note after the preconditions says `:app:installDebug` names the debug variant, not a step run during the sitting.
2. **15e still said "Phase 15 closes with 15e recorded as `:NOT RUN`".** Ruling 2 (and AC-F8) runs 15e once on this branch's head, after D-03m and the fonts. The follow-up slices landed inside phase 15, so that sentence no longer describes the close. A dated pointer goes in 15e's amendment note and under §6.5's "Which builds this gate applies to" table.

## Task 2 — the check of D-03m (commit a8bfe8e)
**Verdict: FAIL on text only, fixed in this commit.** After the fixes the amendment passes. The ruling and its numbers are sound.

### What was checked, and the result
| Question | Result |
|---|---|
| Does D-03m resolve 15p.1 on the visible reading? | **Yes.** Without the floor, the row's box is its gear, which is 48dp and taller than the 44dp title line in either face. Hero→art is therefore the band gap. On the 412×915 baseline (843dp usable) the art fills its stage in all three chip states: its edge is ≤ 302dp, under the 320dp cap and the 380dp content width. So every visible gap is 16dp ≤ 24dp. Pixel_9 projection: the gear bottom is 54.1 + 16 + 48 = 118.1dp and hero→art is 16dp, down from 27.4dp. |
| Does the corrected 6.7-inch table hold? | **Yes, every cell** (all ranges recomputed by hand). No chip: fixed 440–500, slack 343–403, stage 257–302, footer 86–101, first tile top 485–498. One chip: 540–624 / 219–303 / 164–227 / 55–76 / 516–523. Both chips: 640–748 / 95–203 / 71–152 / 24–51 / 547–548. The midpoint is 421.5, so 15g holds with the tiles 6dp higher (24 − 18). Part 1's ≥ 120dp one-chip art holds (164–227dp). |
| Do the narrow-class numbers hold? | **Yes.** Column positions 8..56 · stage 64 (0) · ribbon 72..128 · chips 136..244 and 252..360 · tiles 368..476 and 484..600, total 616dp, 48dp over 568dp. The fold at 568 cuts the Challenge tile (484 < 568 < 600). 360×800dp: slack 112, stage 84. QA's device: slack 28 + 24 = 52, stage 39 < 48 so no art, gear→ribbon 8 + 39 + 8 = 55dp < 64dp. |
| Does the arithmetic hold in the real faces? | **Yes, in the static Fredoka SemiBold that ships** (sha256 `021e30b2…d7688e43a8`, equal to the tech-lead's D1). Measured independently: "Peg Solitaire" = 5715/1000 em (kerned), the same as the VF at wght 600. That is 205.74dp at 36sp, and it stops fitting 296dp above 51.79dp. Typo metrics are 974 / −236, gap 0, `USE_TYPO_METRICS` set: 1.21em, or 43.56dp at 36sp, inside the 44sp line. Android's scale-2.0 table (API 35 source) maps 36sp to 43.31dp and 44sp to 50.40dp. Title room is 296dp at 360dp width (344 − 48) and 332dp at 412dp (380 − 48). |
| Is the §13 D-03m retarget table consistent? | **Yes.** The 28dp watch-it-fail figure is 12 + 16 on `w412dp-h915dp` with insets. "Row height = tallest child ±0.5dp" holds at 1.0 (48, the gear) and at 2.0 (≈50.4, the title line). The 15k row keeps the ≤ 120dp bound and does not pin a number. |
| Is §16 consistent? | **Yes.** One token retired, none added, no string or colour change, which matches §6.1's retirement row. |
| Is 15k still coherent? | **In substance, yes.** Parts 1 to 5 hold with a 48dp overflow. **One stale figure** in part 2's parenthetical ("it is ≈72dp over") is fixed; see F-1. |
| Does D-03m contradict D-03l, Q-004 or Q-005? | **No.** D-03m amends D-03l's row floor only, and its pointers are in place. Q-004's three items stay answered: no logo, title start-aligned in one row with the gear, and no hole (every visible gap is now the band gap). Q-005 was answered 2026-09-29 as option 1, the board stage. D-03m keeps option 1 exactly and gives the stage ¾ of the recovered 24dp. The one visible difference, title and gear 12dp nearer the status bar, does not touch any of the three things Q-004 selected. |

### Findings (all fixed in this commit, in house style)
- **F-1: seven passages still stated the retired 72dp row or the ≈72dp overflow as current fact.** D-03m corrected the arithmetic in §4.2.3 but not where it is restated. The stale passages were:
  - D-03k in §3: "72dp over — and all 72dp are in the hero's 48dp gear…";
  - §4.2.2's D-03k note, "still scrolls by ≈72dp";
  - §4.2.2's "This branch was taken", "the remaining 72dp cannot be found";
  - §4.2.4's compact-set paragraph, "one row, 72dp minimum";
  - §12's D-03k note, "by ~72dp";
  - criterion 15h's retirement note, "every one of those 72dp is…";
  - **criterion 15k part 2**, "it is ≈72dp over".

  Fix: six dated pointers, one of them covering both §4.2.2 passages. None changes a ruling. The 15h pointer adds that 15h is still false at 48dp, so it stays retired.
- **F-2: §15 still recorded Q-005 as "open".** It was answered 2026-09-29 as option 1. The answer is recorded under Q-005 and in the section's intro, with a line saying D-03m changes nothing in option 1.
- **F-3: D-03m's real-face check cites `fixtures/fonts/Fredoka-Variable.ttf`, which the font ruling deletes (T5).** The figures do not change, but their source would dangle. A dated note under "The ruling in the real faces" re-anchors them on the bundled static file, with the independently measured values above.

### Observations (not D-03m's, not fixed here)
- **O-1: the < 80dp blank bound fails on tall tablets.** D-03l's "the largest blank … on any window" is < 80dp only while the stage is ≤ 416dp. Above that, the art is capped at 320dp and centred, so the blank is (stage − 320)/2 + 16. On an 800×1280dp portrait tablet (≈1200dp usable, no chip) the stage is ≈570dp, which leaves ≈141dp above and below the art. 15p's grid covers phones only, and every phone window I computed stays under the bound. Q-004 was about phones, so this is not a founder question. The producer can route it back to this role for a ruling, for example capping the stage's share on tall windows.
- **O-2: a slightly larger blank on QA's 360×640 device.** The largest blank goes from 49 to 55dp: the stage takes 18dp more slack but still draws no art. It stays inside the < 64dp compact bound, and D-03m states it. It may be visible in the founder's Q-013 screenshots of a small window.
- **O-3: §4.2.3's "Note where 640 lands" and §13's "within a few dp of 640" now read 616.** The conclusion, that short-window tests need insets, only gets stronger, so I made no edit.

## Outputs
- `docs/ux-spec.md` — amended. Every change is dated 2026-09-29 and nothing is rewritten in place. The changes are: the ninth-revision preamble; the D-03g and D-03k pointers (§3); §4.2.2 and §4.2.4 pointers and the re-anchored real-face note; §6.3 note; §6.5 exception, row, degradation note, preconditions 1/4/7 and the precondition-1 reconciliation, and the "Which builds" pointer; §9 row and sentence; §12 pointer; §14 15e, 15h, 15k and 18d; §15 Q-005 answer; §16 line.
- `.planning/reports/15-founder-feedback/ux-designer-6-5-and-d03m-check.md` — this report.
- Scratch only, never committed: the helper's `check-d03m/measure_d03m.py` and `out.txt` in the session scratchpad.

## Checks
- `hb-shape --font-size=1000 --output-format=json [--variations=wght=600,wdth=100 | wght=500,wdth=100] [--features=tnum] <font> '<text>'` (HarfBuzz 14.4.0, run by the helper, 151 calls) — pass. Fredoka SemiBold static "Peg Solitaire" = 5715 = the VF at wght 600. Medium: "1" = 388, "2" = 587. `tnum` changes nothing. Nunito digits are all 600, and its em dash is 1000.
- fontTools 4.66.0 `TTFont` reads of `head`, `hhea`, `OS/2`, `fvar` and the `GSUB`/`GPOS` FeatureList — pass. Fredoka: upem 1000, typo 974/−236/0, `USE_TYPO_METRICS` set, no `tnum` in GSUB. Nunito: typo 1011/−353, `USE_TYPO_METRICS` set.
- `shasum -a 256` of the scratch copies of the five static files — pass: all equal the tech-lead's D1 hashes. `app/src/main/res/font/` does not exist in the worktree yet; D1 is still in flight.
- `~/Library/Android/sdk/sources/android-35/android/content/res/FontScaleConverterFactory.java:129-137`, the scale-2.0 table — pass: 36sp → 43.31dp and 44sp → 50.40dp.
- Hand re-derivation of every D-03m number (the 6.7-inch table, the narrow-class positions, the Pixel_9 and 360×640 projections, and §13's 28dp) — pass. Figures are in the table above.
- Rendering check: I re-read every edited region of `docs/ux-spec.md` (list continuation paragraphs, blockquotes, table rows) — pass.
- `./gradlew check` — not run. The dispatch forbids Gradle, and this change touches only the spec and this report.
- `git add` of the two paths, then `git commit --only` of them (by the commit helper) — see the commit that carries this report.

## Open issues / risks
- **The `docs/ui/DESIGN-SYSTEM.md` §Typography text in the tech-lead's report is not applied.** The dispatch commits two files only, and a half-applied edit would sit uncommitted in the developer's worktree. Until it lands, that file's "downloadable fonts … `ui-text-google-fonts`" sentence is wrong. The ninth-revision preamble says the spec's §6.3 and §9 supersede it. The producer should dispatch the edit, which belongs to the ux-designer.
- **This ninth revision was written on Opus under Q-014.** Under the 0.6.2 rules it needs a check by a fresh agent that did not write it, as D-03m had.
- **O-1 (the tablet blank)** is open for a later ruling, and it is not a founder question.
- **Every D-03m dp is still a projection or Robolectric arithmetic.** QA's re-walk in the real faces (the tech-lead's discriminators first) is what closes 15p, 15k and the rest.

## Handoff
Next: producer —
- **Developer (android-game-dev):** no change to the in-flight D-03m or font work. D-03m's contract is unchanged. The new pointers correct explanatory figures only, and §13 already says a 15k test keeps the ≤ 120dp bound and pins no overflow value. Building for 15e: the arms are built with `:app:assembleDebug` and installed at block boundaries (§6.5 note after the preconditions).
- **qa-engineer:** run the re-walk and 15e by the amended §6.5: `Pixel_9a` only, load ≤ half the logical cores at the defined moments, arms swapped at block boundaries with sha256 recorded, arm B = the head carrying both D-03m and the fonts.
- **ux-designer (fresh agent):** apply the tech-lead's `DESIGN-SYSTEM.md` §Typography text. Separately, check this ninth revision.
- **Code-auditor / tech-lead:** nothing new from this report.
