# android-game-dev report — 15-founder-feedback, slice S11 fix round 1 (F-1, F-2, F-3, 18d)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (771d81b..86bfecf, plus this report's commit; worktree `/Users/ermanergoz/Desktop/PegSolitarie-p15`)
Skills loaded: ergoz-soft:standards (role: android-game-dev; the plugin's 0.6.2 rule files were diffed over the 0.5.1 snapshot and read where they differ: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing), compose-skill, compose-ui, android-testing, superpowers:test-driven-development (with its writing-good-tests reference), gitflow
Ran on: claude-opus-5-5 (Opus 5.5) at effort max (agent file `model: opus`, `effort: max`)
Fan-out: none
Ran in parallel with: none (the only Gradle user in this worktree for the whole round)
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
All four findings are fixed. Each new or changed test was first watched fail against the named defect or mutation, then restored.
- **F-1:** pixel probes for each dock disc's fill, ring and shadow, in every look each control can reach.
- **F-2:** 18b reads the back chip and the dock slab's paint.
- **F-3:** 15n and 15o hold the hero row to its layout.
- **18d:** the timer sets its digits in fixed-width cells, and a JVM test in the shipped Fredoka proves the pill width holds. This is the only production change.

`./gradlew check` is green, and the build is installed on emulator-5560.

## Outputs
- `app/src/test/java/com/app/pegsolitaire/ui/game/DockOnMarkAndSlashTest.kt` (771d81b) — **F-1**. Three new tests read every control in every look it can reach, against §4.4.1's table typed into the test (never read from `DockLook.recipe`):
  - `everyControlFillsItsDiscWithTheToneItsLookRequires`: the fill between the glyph and the rim, to one level, since `primaryContainer` and the slab are only 4 to 6 levels apart.
  - `onlyShowMovesOffRingsItsDisc`: the middle of the 2dp ring.
  - `onlyTheRaisedLooksCastAShadowOntoTheSlab`: the darkening summed over the 2dp band below the rim.
- `app/src/test/java/com/app/pegsolitaire/testing/RenderedWindow.kt` (771d81b, new) — the shared render harness:
  - `renderedWindow()` is the flat `View.draw` render the dock test already used, moved here unchanged.
  - `renderedWindowWithShadows()` is Robolectric's hardware `PixelCopy` of the window. It is the only render that draws elevation shadows: in the software render every "below the rim" pixel read the bare slab, raised or not.
- `app/src/test/java/com/app/pegsolitaire/testing/SystemBarInsets.kt` (771d81b) — `composeHost()` is now `internal`, so both harness files use one host lookup.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameStatusStripTest.kt` (af89ff0, 86bfecf):
  - **F-2:** 18b keeps its two tag readings and adds the back chip's top edge (within 8dp of the status bar, 18b's device clause) and the row where the dock slab's paint ends, scanned in the flat render.
  - **18d:** `theTimerPillKeepsOneWidthAsItTicksInTheShippedFace` sets the timer in the bundled Fredoka. It first proves the face's digits really are proportional in this render, then that the pill width is equal (±0.5dp) at 00:00, 08:08, 11:11 and 59:59.
  - 18a and 18e now read every timer cell's text layout, because the timer's own node no longer has one. Every assertion is kept, now applied per cell, and 18a also checks that the cells spell the clock the node carries.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeHeroRowTest.kt` (14b1812) — **F-3**. A layout helper, `assertNothingElseTakesRoomBesideOrAboveTheTitle`, now runs in all five hero-row tests:
  - the title and the gear chip tile the row (title start at the row start, title end at the chip start, chip end at the row end);
  - the row starts one column inset below the scroll viewport's top edge.
- `app/src/main/java/com/app/pegsolitaire/ui/game/TimerDigits.kt` (86bfecf, new) — **18d fix**:
  - every digit is set in a cell as wide as the widest digit of the resolved face, measured in the layout pass;
  - the colon keeps its own width, and cells are placed left to right in every layout direction;
  - one node keeps the clock's text plus the caller's `elapsed_time` description, and the cells are `hideFromAccessibility()`, so TalkBack meets the same node the single `Text` was;
  - light and dark `@Preview`s show "11:11" over "22:22" (one width in Fredoka).
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt` (86bfecf) — `TimerPill` uses `TimerDigits`, with the same style (`headlineSmall.withTabularFigures()`), colour and description. The two KDocs that said tabular figures keep the width now name the cells.
- `app/src/test/java/com/app/pegsolitaire/ui/game/TimerDigitsTest.kt` (86bfecf, new) — a Fredoka held back until after the first frame, checked against the identical Fredoka loaded up front. The late clock must end in the same cells, which catches a cell width remembered in composition. See Open issues for this harness's limit.
- `app/src/test/resources/fixtures/fonts/Fredoka-Variable.ttf`, `Fredoka-OFL.txt`, `README.md` and `app/src/test/java/com/app/pegsolitaire/testing/ShippedFace.kt` (f074c13) — the test-only font fixture:
  - the binary is QA's Google Fonts file, unmodified (sha256 `2ba02e68…`), with its OFL-1.1 notice under the font's own copyright line and a provenance note;
  - it is on the unit-test classpath only, never in `src/main`.

## Checks
- `./gradlew check --offline` on 86bfecf — **pass**: `BUILD SUCCESSFUL in 11m 7s`, `98 actionable tasks: 19 executed, 79 up-to-date`.
  - `:app:testDebugUnitTest` executed: app 1,049 tests (the auditor's 1,044 plus 5 new), 0 failures, 0 errors, 0 skipped. Domain 274 / 0 / 0.
  - detekt: `0 number of total findings`. ktlint: pass. dpSpLiteralCheck: pass.
  - lint: `0 errors, 92 warnings`, none in a file this round touched (all version, typo and pre-existing `UnusedResources` warnings in `App.kt`, `HomeBlobs.kt`, `dimens.xml`, `font_certs.xml`, `strings.xml` and the Gradle files).
- An earlier full `./gradlew check --offline`, before a KDoc-only edit to `TimerDigits.kt` — pass, `BUILD SUCCESSFUL in 11m 7s`, the same 1,049 / 274 counts.
- `ANDROID_SERIAL=emulator-5560 ./gradlew :app:installDebug --offline` — pass. `Installed on 1 device`. `dumpsys package` shows `lastUpdateTime=2026-09-29 05:16:01`. The app was not launched and the emulator was not restarted.
- `git diff cd25522..HEAD -- . ':!.planning' | grep '^+' | grep -E '@Suppress|@Ignore|@Disabled|!!|baseline|SuppressLint|ktlint-disable|detekt:disable|OptIn'` — 1 hit, the prose phrase "design baseline" in a KDoc. No code hits.
- Removed `@Test` lines in `app/src/test`: 0. Added: 5. No build, detekt, lint or baseline config touched. No new dependency.
- **Mutation runs.** Each ran in this worktree and was restored. `git status` showed `app/src/main` clean after each committed-file mutation, and `cmp` against a backup confirmed each restore of an uncommitted file. Every run recompiled.

| # | Mutation (production code) | Finding | Result |
|---|---|---|---|
| F | `DockFill.Pale -> scheme.primary`; ring colour ignores `recipe.ring` | F-1 | red 2/7 `DockOnMarkAndSlashTest`: "Undo, Available: the fill: expected #FFEDE4FF, read #FF7C3AED"; "Undo, Available: the ring: expected #FFEDE4FF, read #FF635F69" |
| F2 | ring never drawn (always transparent) | F-1 | red 1/7: "Show possible moves, Off: the ring: expected #FF635F69, read #FFF1EAFB" |
| F3 | shadow ignores `lift` (every disc raised) | F-1 | red 1/7: "Undo, Unavailable: the slab under a flat disc is darkened by … at most 1, but was 18" |
| F3′ | every disc flat (`lift * FLAT`) | F-1 (other half) | red 1/7: "Undo, Available: … at least 6, but was 0" |
| J | the auditor's J, re-run after the harness move | existing 21e tests | red 3/7, as the auditor recorded |
| B | both insets after `.testTag(...)` | F-2 | red 18b: "the back chip's top edge … at most 32.0, but was 48.0" (plus 18c ×2 as before) |
| B-dock | the dock's `navigationBarsPadding()` after its tag, alone | F-2 | red 18b only: "where the dock slab's paint ends … expected 576.0, but was 528.0". 18c stays green, so before this round the whole class passed this defect |
| A | the auditor's A | 18b, 18c | red, as before |
| H | decorative `Icon(contentDescription = null)` between title and gear | F-3 | red 5/5 `HomeHeroRowTest`: "the title's end edge against the gear chip's start edge … expected 304.0, but was 280.0" |
| H2 | 120dp decorative `Icon(contentDescription = null)` above the hero row | F-3 | red 5/5: "how far below the column's top edge the row starts … expected 8.0, but was 136.0" |
| — | the original code (plain `Text`), with the new 18d test | 18d | red: "the pill at 08:08 … expected 86.0, but was 84.0" (the guard passed: the face is proportional) |
| C | pill `height(40dp)`; `withTabularFigures()` removed (fix in place) | 18a, 18e | red: 18a "expected: tnum, but was: null"; 18e "the timer is cut short vertically" |
| D1 | cells sized to their own digit (no fixed cell) | 18d | red 18d only |
| D2 | cell width `remember(style)`-ed in composition | late face | red `TimerDigitsTest` only: "the late clock at 08:08 … expected 61.666668, but was 61.0" |

## Open issues / risks
- **18d on a device still governs.** The JVM test uses the binary Google Fonts serves, but the qa-engineer should confirm the pill width at the four readings on a physical phone where Fredoka resolves. It never resolves on this studio's emulators (QA's O-1), so emulator-5560 draws the fallback, whose digits are already tabular.
- **The JVM harness does not show a late font being re-laid out.** Under Robolectric, text never re-lays out when an async face lands after the first frame; I checked this with a plain `Text` as a control, and it held even after forcing snapshot apply notifications, a draw, and a looper advance.
  - `TimerDigitsTest` therefore has its host read the face's resolution state, which recomposes it on arrival and stands in for the device's next frame. The class KDoc says so.
  - What it pins is the layout's own part: once laid out again, the cells are the late face's, not a width kept from the fallback (mutation D2).
  - On a device, a cell whose width changes (the colon alone goes from 19px to 17px at xxhdpi) re-measures the clock.
- **THIRD_PARTY.md: a decision for the producer or release-manager.**
  - The file does not exist on this branch. It lives on the unmerged `feature/phase-14-release-docs`, whose scope rule ("reaches a player's device") excludes test-only files.
  - The fixture carries its OFL notice and a provenance README, so I added no row.
  - `rules/ip-policy.md`'s one-row-per-asset rule may still call for one when that file merges.
- **FYI for the ux-designer and tech-lead (not in scope):** other `withTabularFigures()` users set in Fredoka are equally non-tabular. These are the Home stat-pill values and the Results time and score (`titleMedium`). They do not tick while shown, so nothing jitters.
- **Process note, for transparency.** During one mutation run I restored an uncommitted production file (`GameStatusStrip.kt`) with `git checkout --`, which discarded my uncommitted `TimerPill` change.
  - I re-applied it at once and verified the diff.
  - The next mutation (D1) had run against the reverted file, so I re-ran it against the restored fix; the D1 row is that re-run.
  - The mutation C result had been taken before the restore and stands.
- **Lesson candidates for the retro:**
  - (a) a root `fonts/` directory on the Robolectric test classpath shadows the platform fonts Robolectric loads, and every Robolectric test in the module then fails with `ExceptionInInitializerError` in `Typeface`;
  - (b) restore a mutation of an uncommitted file from a backup copy, never with `git checkout`;
  - (c) elevation shadows need the hardware `PixelCopy` render, since the software `View.draw` never draws them.

## Handoff
Next: code-auditor — re-audit the test delta 771d81b..14b1812 (F-1, F-2, F-3) and the fix 86bfecf with its fixture f074c13 (18d). Mutations F, F2, F3, B, H and H2 are the ones to re-run.
Next: qa-engineer — re-run 18d on a physical phone where Fredoka resolves (pill width at 00:00, 08:08, 11:11, 59:59, ±0.5dp) and check the timer still announces "Elapsed time" under TalkBack. The build is installed on emulator-5560 for 18a to 18c, 18e and a regression pass.
