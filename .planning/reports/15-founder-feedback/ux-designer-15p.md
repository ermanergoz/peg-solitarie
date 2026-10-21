# ux-designer report — 15p gap reading (qa-engineer E-1, loop 1) and the real-face fit check
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (parent 60d8bc7; one commit, the one that carries this report)
Skills loaded: ergoz-soft:standards (role ux-designer; rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose. The 0.5.1 snapshot is older than plugin 0.6.2, so I also read the plugin copies of studio.md, reporting.md, compose.md and dispatch.md), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: the tech-lead's font-fix ruling (Q-013). I did not read its report.
Ran on: Opus (claude-opus-5-5); the agent file pins fable at effort xhigh. **Staffing deviation:** Fable is exhausted, so this ran on Opus under the founder's Q-014 answer (`.planning/questions/FOUNDER.md`), which is scoped to phase 15.
Fan-out: none. The only helpers were two Explore reads (spec line map; composable paddings and styles), one general-purpose font measurement and one general-purpose commit. None of them names a studio role.
Loops: 0
Lessons applied: none (the snapshot has 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

## Summary
**Ruling (D-03m):** 15p is measured between **visible** elements. "Visible element" is now defined once: the layout bounds of drawn surfaces and text, never an undrawn box, glyph ink or a shadow.
- On that reading, the audited builds fail 15p.1: hero→art is 27.4dp against a 24dp limit.
- The cause is the spec, not the build. My 72dp row floor put 12dp of air below the 48dp gear.
**Re-specified:** `home_hero_row_min_height` is retired. The row is as tall as its tallest child, so the gap is the 16dp band gap and the stage gains 24dp. The gear keeps its 48dp target.
**Real faces:** the ruling holds in Fredoka. The title is 205.7dp wide against 205.5dp in Roboto (296dp of room at 360dp width), and its 44dp line box is under the gear's 48dp in either face. No §4.2/§4.3 fit breaks at default scale; three low risks are listed below.

## The ruling, in brief (the spec has the full text: D-03m in §3, and the last subsection of §4.2.4)
- **Why the visible reading governs:**
  1. The criterion itself says "visible".
  2. Q-004 is about a gap the eye sees. On `s11-home.png` the band above the board reads deeper than the 16dp gaps below it.
  3. The box exists only in a JVM test, so the device and JVM readings would keep disagreeing.
- **Why the fix is "no floor" rather than a 48dp floor:**
  - Any floor taller than the tallest visible child is an invisible band by construction.
  - A floor equal to the gear only repeats `IconButton`'s own guarantee.
  - With no floor, the row's box and its visible extent are the same, so the two readings cannot drift apart again.
- **Projected effect on QA's devices (projections, not readings; the re-walk governs):**
  - Pixel_9: hero→art goes 27.4 → **16dp**. Stage art goes 289/216/144 → ≈307/234/162dp (no chip, one chip, both chips).
  - 360×640dp with both chips, real insets: the gear→ribbon blank goes 49.0 → ≈55dp. That is inside the compact bound of < 64dp, and the bound is now exact.
- **Robolectric (360×640dp):** 15k's overflow falls from ≈72 to ≈48dp. The fold still cuts through the second tile (`484..600`).
  - §4.2.3's "every one of those 72dp is a readout, a target or a route" was wrong by these 24dp. It now carries a pointer.
- **6.7-inch table, recomputed in §4.2.4:** 15g's midpoint holds in every state (the first tile's top is at 485–548dp against 421). One-chip art is 164–227dp, so part 1's ≥ 120dp holds.

## Fredoka / Nunito fit check (§4.2 and §4.3), at 360dp width, default font scale
Method:
- Widths are HarfBuzz-shaped, kerning applied, em × size, plus M3 letter spacing, from the OFL binaries.
- Fredoka is instanced from the QA copy, which is byte-identical to `fixtures/fonts/Fredoka-Variable.ttf`. Nunito v3.602 and Roboto v3.015 come from google/fonts.
- The paddings come from the composables: `HomeScreen.kt:469-533` and `:640-701`, `HomeProgressRibbon.kt:235-249`, `GameStatusStrip.kt:96-217`, `TimerDigits.kt:74-105`.
- Every line box is set by the M3 line height, so heights do not depend on the face.

| Surface | Text, style | Room (dp) | Roboto (dp) | Real face (dp) | Verdict |
|---|---|---:|---:|---:|---|
| Hero title | "Peg Solitaire", `displaySmall` Fredoka 600, 36sp | 296 | 205.5 | **205.7** | Fits, 90dp spare. One line even at 200% (≈247.5dp on API 34+) |
| Strip title | "Peg Solitaire", `titleLarge` Fredoka 500, 22sp | 178 (164 at 100:00) | 124.8 | **125.1** | Fits. §4.3.1's ≈150dp estimate was conservative |
| Strip title | "Level 1234" | 178 | 107.2 | **105.0** | Fits |
| Timer pill | 4 cells × widest digit ("2", 0.587em) + colon + 2 × 12dp | — | 84.9 | **85.9** | Same budget. 18d is still owed on a device |
| Compact pill | "Boards" (`labelMedium` Nunito 600, 12sp) + 4 + "5/5" (`titleMedium` Fredoka 500, 16sp) | 93.3 | 69.9 | **70.6** | One line. 15m's ≤ 56dp holds (pill 40dp) |
| Compact pill | "Stars" + "1234" | 93.3 | 71.8 | **69.2** | One line |
| Continue chip, Challenge | "Pick up Level 2 where you left off", `bodyMedium` Nunito 400, 14sp | 268 | 213.2 | **217.2** | One line. At level 1234: 243.2dp (Roboto 237.6) |
| Continue chip, Classic | "Pick up your saved board" | 256 | 161.8 | **163.1** | One line |
| Continue chip titles | "Continue Challenge", `titleMedium` Fredoka 500 | 268 | 142.1 | **143.4** | One line |
| Challenge tile level chip | "Continue — Level N", `bodyMedium` Nunito 400 | 164 | N=2: 121.8; N=1234: 146.2; N=12345: 154.3 | N=2: **127.5**; N=1234: **153.4**; N=12345: **162.1** | **Tight** (risk R-1) |

Vertical metrics:
- Fredoka is 1.210em (asc 0.974, desc −0.236, `USE_TYPO_METRICS`). That fits every Fredoka role's line height: `displaySmall` 1.222em, `titleLarge` 1.273em, `headlineSmall` 1.333em, `titleMedium` 1.5em.
- Nunito is 1.364em, which is 0.03em more than `labelMedium`'s and `bodySmall`'s 1.333em line heights. Its glyph ink tops out at 0.90em, so nothing clips.

**Risks the real faces introduce or keep (none breaks a criterion at default scale):**
- **R-1 (low, new with Nunito): the Challenge tile's level chip at 360dp width.**
  - Nunito's em dash is 1.00em, against Roboto's 0.77em, and its digits are 0.600em.
  - A 5-digit ladder level therefore leaves 1.9dp of room, against 9.7dp in Roboto. A 6-digit level wraps in Nunito and fits in Roboto.
  - A wrap truncates nothing: the chip has no `maxLines`, so the tile grows one line, ≈20dp, and oracle 1 stays whole.
  - Not ruled. If the founder or QA judge a 10,000+ level reachable, the lever is the chip's own padding, not the string.
- **R-2 (low, affects the spec's text only): Fredoka has no `tnum`.**
  - `withTabularFigures()` does nothing in Fredoka. The timer is covered by `TimerDigits`' cells; the F-C test gap stays with the developer.
  - §6.3's sentence "every numeric stat value use `withTabularFigures()` so digits never shift width" is false for the ribbon values in the shipped face. Nothing moves at default scale, because the pill widths are fixed by weight and the values do not tick.
  - I left §6.3 unamended: it is outside this dispatch.
- **R-3 (pre-existing, not changed by the faces): O-2.** At 200% on the compact-narrow pill, the value is measured first and the label gets ≈46.6dp.
  - "Stars" (≈62dp) and "Boards" (≈83dp) in Nunito therefore still break mid-word, as they did in Roboto.
  - The same mechanism can make a value's first emission ("—" 0.68em → "401" 1.52em) move a label from one line to two at 200%. Criterion 15f would see that as a layout shift.
  - Still open. Not ruled here.

## Outputs
- `docs/ux-spec.md` — amended; every change is dated 2026-09-29 and nothing is rewritten in place:
  - Front matter: `revised` date.
  - Eighth-revision preamble.
  - **D-03m** (§3), plus a pointer on D-03l.
  - Pointers in §4.2.3 and at four places in §4.2.4.
  - New subsection in §4.2.4: "15p's gap reading, ruled — the hero row loses its floor". It holds the ruling, the visible-element definition, the re-specification, the corrected 6.7-inch table, the narrow-class and device projections, the real-face check and the owed tests.
  - §6.1: one token retired.
  - §13: a D-03m retarget table.
  - §14: pointer under criterion 15p.
  - §16: a line.
- `.planning/reports/15-founder-feedback/ux-designer-15p.md` — this report.
- Scratch files, not committed: `fontmetrics.md`, `fontmetrics.json` and `fontmetrics.py` in the session scratchpad (`/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/3af61d76-2149-4d9a-b93b-7ce2a7b38d20/scratchpad/`).

## Checks
- `fmvenv/bin/python fontmetrics.py` (scratchpad; fontTools 4.66.0, uharfbuzz 0.56.2 / HarfBuzz 14.5.0) — pass. Nine instances measured (Fredoka 500/600, Nunito 400/600/700, Roboto 400/500/600/700), with no missing glyph.
- Cross-check against QA's `18d/fredoka/digit-advances.txt` — pass. The Fredoka 500 digit advances match exactly.
- Title width sanity check on `s11-home.png` (Roboto) — pass. The ink runs ≈16.6–219dp, so ≈202dp wide, against a 205.5dp shaped advance; the side bearings explain the gap.
- Re-derived by hand, recorded in §4.2.4:
  - the 6.7-inch table, for all three states at both ends of each band range;
  - the narrow-class positions;
  - the Pixel_9 and 360×640 projections from QA's device numbers.
- `./gradlew check` — not run. The dispatch says not to run Gradle, and this change is spec-only; no code is touched.
- The commit (`git add` by path of the two files, `git commit`) — see the commit that carries this report.

## Open issues / risks
- **The build still fails 15p.1 under this ruling until the developer lands D-03m.** That means removing `heightIn(min = home_hero_row_min_height)` (`HomeHero.kt:73`), deleting the token from `dimens.xml`, and retargeting the tests in §13's new D-03m table. `HomeStageTest` currently measures from the row's box, and a test must be watched failing against the old floor before it is trusted.
- **Every dp here is a projection or a Robolectric/JVM figure.** QA's re-walk of 15p in the real faces (owed under Q-013) is the reading that closes it.
- **R-1, R-2 and R-3** are above. None is ruled in this dispatch.
- **Review of this amendment.** studio.md says a spec written on fable is checked by a fresh fable agent that did not write it. This amendment was written on Opus under Q-014, so the producer decides who checks it: a fresh agent that did not write it.
- **Not in this dispatch, still open for the ux-designer:**
  - loop-2 **E-1**, 15e precondition 7 "both arms installed", which waits on the tech-lead's §6.5 wording, as the dispatch says;
  - loop-2 **O-A**, whether TalkBack speaks the timer's value or only "Elapsed time";
  - loop-1 **O-2** (R-3 above) and **O-4**, which is information only.

## Founder question
None. The ruling is not a matter of taste: it follows from the criterion's own word ("visible") and the founder's Q-004 complaint. The founder will see one visible difference in the Q-013 real-face screenshots, and should be told it is deliberate: the title and gear sit 16dp below the status bar instead of 28dp, and the board is ≈18dp larger.

## Handoff
Next: producer —
- **Developer (android-game-dev):** land D-03m (§4.2.4's last subsection, §6.1, §13), preferably in the same fix round as the Q-013 font fix, so that one device re-walk covers both.
- **qa-engineer:** re-walk 15p in Fredoka/Nunito on the visible reading (plus 15k, 15o and 18a as already owed), and note R-1 if a 5-digit level can be seeded.
- **Code-auditor:** review the diff.
- **Spec review:** arrange a check of this amendment by a fresh agent that did not write it.
