# code-auditor report — phase-15 H-1 re-audit (loop 5): the test-only fix for F-1, F-2, F-3
Date: 2026-10-21
Branch: feature/phase-15-ux-refresh (887c97d..af08b86; audited `git diff 2e7094e..af08b86`, two test files; HEAD b6a6230 adds only the developer's report) — report only, committed by the producer
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android, compose; plus the product's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable. This ran on Opus, the author's model, under the founder's Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), `.planning/questions/FOUNDER.md`.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

VERDICT: PASS

## Summary
- **Deviation first:** this re-audit ran on Opus, not Fable, under Q-019. To compensate I trusted no recorded result. I re-ran the Check, and I re-ran every red-then-green claim myself as a mutation in a throwaway detached worktree. I read none of the developer's reports and nothing under `.planning/log/`.
- **Scope holds.** The four commits touch two test files only. No file under `app/src/main`, `domain` or the build changed. No suppression, `@Ignore`, baseline, `!!` or sleep was added.
- **F-1, F-2 and F-3 are closed.** Every new assertion goes red under its mutation, and each red repeats the developer's message: M1c on `w360dp-h560dp` (three red), M3b, M5, and a band below the tiles in the fit branch. The overflowing window's liveness guard fails when that window stops overflowing, exactly at its 16 dp boundary. The Check is green with every task executed.
- **Item 5:** I accept the developer's reading. The trailing gap and bottom padding are not a band, so a fold in them hides nothing. The spec says so itself (§4.2.2 clause 5, "The spacers are not bands"). The tolerance is also exact, so the ux-designer does not need to rule (§ Item 5).
- No finding is owed before merge. Four INFO items follow, two of them carried from loop 4.

## Findings
Severity: MAJOR or MINOR = must fix before merge; INFO = no action owed by this audit.

**No MAJOR or MINOR finding.**

- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:643,660-663` — INFO. The pixel check cannot see a glyph drawn as **text**: it skips every node that has text semantics.
  - Mutation M3d put `Text("★")` inline before the label. All 119 `ui.home` tests stayed green.
  - The shipped glyphs are `ImageVector` icons, and M3b/M3c (an `Icon` before the label or after the value) are red. So F-2 as filed is closed.
  - A cheap hardening, if wanted: assert that the pill's `textsWithin()` are exactly `[label, value]`.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:557,565-569` — INFO. Two assertions in `assertTheFoldLeavesNoFalseBottom` cannot fail in practice:
  - `unscrolled.top ≥ statusBarInset`, because clipped bounds never sit above the viewport;
  - the move branch's `scrolledTop < unscrolled.top`, because the last band never scrolls down.
  - Both are harmless. The branch's real content is the `assertIsDisplayed` / `top < fold` pair before the `if`, and M1c turns it red.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:167-177` — INFO. 15l's third window now tells the full set from the compact set by spacing (M5 red). The full **pill** at ≥ 400 dp is guarded only by accident.
  - Mutation M5c (compact pill on every window, spacing untouched) left HomeLayoutTest green.
  - Only `HomeStageTest.onTheTallPhoneTheStageHoldsABoardOfAtLeast120dpAndNoGapExceeds24dp` caught it, and only by 1 dp (`25 > 24`).
  - Given `compactPills = compact && narrow`, the spacing read does decide the set, so this is not a defect.
- Carried from loop 4, documentation only, outside this diff:
  - `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt:272-277,350-358` still says the narrow baseline is "~72dp over" and kept fitting by the compact set. The test file now states the measured truth (0 dp on NATIVE and on the device).
  - `docs/ux-spec.md:3805-3806` (15k part 2's parenthetical "≈48dp over") is LEGACY's reading. 15k part 5 already says the device numbers govern, so no criterion changes.
  - `HomeStageTest`'s class KDoc on LEGACY is still inverted (loop 4).

## Mutations (throwaway `git worktree add --detach b6a6230` under my scratchpad, removed afterwards)
All runs are NATIVE (the class's own `@GraphicsMode`). Each mutation was applied by a script with single-match anchors and restored from git before the next. "640" is `w360dp-h640dp`, "560" is `w360dp-h560dp`, and both have 24/48 insets.

| # | Production mutation | Red (message) | Green |
|---|---|---|---|
| M0 | none | — | 16/16, plus my probe: 640 overflow **0**, Challenge tile 466..562 against the 592 fold; 560 overflow **24**, tile 424..520 against 512; 412×915 gap 16, stage art present |
| M1c | hero row `heightIn(min = 172dp)` | **all three 560 tests**: "…contains 'Challenge' … is not displayed!"; part 1 "expected: 444.0.dp but was: 416.0.dp"; part 3 "at most: 120.0.dp but was: 148.0.dp" | the 640 tests: overflow 68, tile 548..644 cut by 592, Classic whole. Correct under 15k |
| M3b | the full pill's glyph inline before the label (`CompactPillContent`) | **`theNarrowClassPill…ComposesNoGlyph`**: "pill 0 paints 131 px … x 29..46, y 174..189 (px) / expected: 0 / but was: 131" | the other 15 |
| M3c | the same glyph trailing, after the value | the same test: 131 px, x 79..96 | the other 15 |
| M3d | `Text("★")` before the label | — | all 119 `ui.home` tests (INFO above) |
| M5 | compact set forced at ≥ 400 dp (`HomeBandSet.kt:82`, `\|\| !narrow`) | **`aWidePhoneKeepsTheFullBandSet…`**: "the band gap between the two mode tiles, in dp / expected: 16.0 / but was: 8.0" | the other 15 |
| M5c | `HomeProgressRibbon(compact = true)` on every window | `HomeStageTest` 15p only: "no chip: the widest gap … at most: 24.0.dp but was: 25.0.dp" | all of HomeLayoutTest (INFO above) |
| M10a | a 72 dp band directly below the tiles | **640 fit branch**: "every band is whole, so only the column's trailing space may lie past the fold / at most: 16.0.dp but was: 24.0.dp" | 560: overflow 104 ≤ 120, the tile is cut |
| M10b | a band wholly below the fold, behind a 64 dp gap (a genuine false bottom: tile 424..520 whole, band past 592) | **640 fit branch** (`48 > 16`), and 560 part 3 (`128 > 120`) | — |
| M10c | a one-line (~24 dp) text band below the tiles | — | all 20. Correct: at 640 it fits and the band is visible; nothing is hidden |
| LV1 | the stage removed (one gap fewer; 560 overflow 24 → **16**) | **the liveness guard in all three 560 tests**: "w360dp-h560dp keeps 15k's overflow branch live … expected to be greater than: 16.0.dp", plus expected 15g/stage-art reds | — |
| LV2 | the hero row removed (560 overflow → **0**) | **the guard in all three 560 tests**, plus expected lookup reds | — |
| M-edge | hero floor 116 (640: overflow 12, tile 492..588 whole) | — | all 20, the item-5 reading |
| M-edge120 | hero floor 120 (640: overflow 16, tile bottom exactly on 592) | — | all 20: fit branch, 16 ≤ 16 |
| M-edge121 | hero floor 121 (640: overflow 17, tile cut by 1 dp, 497..593) | — | all 20: move branch |

## Item 5 — the trailing-gap reading (ruling)
**Accepted. The ux-designer does not need to rule.**
1. **The spec's own terms support the reading.**
   - §4.2.2 clause 5 says "The spacers are not bands", and the footer is a weighted `Spacer`.
   - 15k part 2 fails "a fold landing in the gap **between two bands**", and below the Challenge tile there is no second band.
   - §4.2.3 defines a false bottom by what it does: "how a readout becomes hidden". A fold in the trailing 16 dp hides nothing.
2. **The tolerance is exact, not generous.** With production's structure, the column's overflow is the tile's bottom + 16 − fold.
   - So the tile is whole exactly when the overflow is ≤ 16 dp: M-edge120 is whole at 16 and M-edge121 is cut at 17, with no window between.
   - Any band past the fold pushes the overflow above 16 and turns the fit branch red: M10a and M10b.
   - A band that fits stays green, as it should: M10c.
3. **The literal reading would fail a correct layout.** My probe found HEAD itself on a real narrow window, `w360dp-h576dp` with the same insets: overflow **8 dp**, tile 424..520 whole against a 528 fold. Read literally, 15k would fail that layout for a fold that hides nothing.
4. **The tolerance is read, not typed.** It comes from `spacing_sm` × 2, the compact gap plus the bottom padding, through resources. If production's trailing space ever grew, the test would turn red, which is the safe direction.

## Standards review of the diff
- **Ruling items 1–4, as specified.**
  - `@GraphicsMode(NATIVE)` is on HomeLayoutTest (`:107`).
  - `renderHome` draws inside `AppTheme(darkTheme = false)` (`HomeTestFixture.kt:158`). That is the same theme `MainActivity.kt:152` uses, with night mode stated rather than read from the host.
  - Parts 2 and 3 are conditional, as 15k states them.
  - Each 15k test also runs on `w360dp-h560dp`, behind a liveness precondition (`:485-491`).
- **The KDoc numbers match my probe**: 640 has 30 dp clear and 0 overflow; 560 overflows 24 with the tile at 424..520 against 512; 412 has a 16 dp gap.
- **Part 4 is now conditional.** "No stage art" is asserted only when the column overflows (`:519`). That is §13's retarget verbatim ("no stage art on an overflowing column", `ux-spec.md:3496`). At 640 the stage measures 42 dp, under the 48 dp art minimum, so nothing changes on the device.
- **Determinism.** The pixel read uses the software render (`RenderedWindow.kt:35`): no shadows, exact fills, a ±1-per-channel tolerance and named constants.
  - The 20 dp medium corner leaves the 8 dp-deflated rectangle wholly on the surface fill.
  - There is no wall clock, no randomness and no sleep.
- **Test quality.**
  - The test names read as sentences.
  - The renames left no stale reference outside `.planning/`.
  - Constants are named, with derivations. ktlint and detekt pass in the Check.
- **Commits.**
  - Five conventional commits, subjects 55–68 characters.
  - Each carries exactly one trailer, the gitflow-mandated `Claude Fable 5.1` line that the hook adds.
- **Suppressions:** none added. `@Suppress` in `app/src/main` and `domain/src/main` is 12 at `2e7094e` and 12 at HEAD. There is no baseline file. **No suppression to approve or reject.**
- **Security and licences.** The diff is test-only: no dependency, permission, network or input surface. `THIRD_PARTY.md` correctly needs no row.

## Outputs
- `.planning/reports/15-founder-feedback/code-auditor-loop5.md` — this report. Not committed: the producer commits it. I edited no source file in the product tree.

## Checks
- `cd PegSolitarie-p15 && ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon --no-build-cache --rerun-tasks check` on b6a6230 — **pass**.
  - `BUILD SUCCESSFUL in 11m 37s`, exit 0, `98 actionable tasks: 98 executed`.
  - JUnit XML: app 1,067 (loop 4's 1,064 plus the three 560 tests), domain 274, `depthSweep` 1, build-logic 54. 0 failures, 0 errors, 0 skipped.
  - HomeLayoutTest ran 16 tests, all passing. Every Home suite passed, 102 tests in all.
  - The executed tasks include `:app:ktlintTestSourceSetCheck`, `:app:detekt`, `:app:lint`, the four literal gates, `:domain:depthSweep` and `:build-logic:test`.
- `git diff --name-only 2e7094e..b6a6230 -- app/src/main domain` — pass (0 files). The only paths outside `app/src/test` are under `.planning/`.
- `git diff 2e7094e..af08b86 | grep -E '^\+.*(@Suppress|@Ignore|assume|!!|GlobalScope|Thread\.sleep|System\.currentTimeMillis|Random\()'` — pass (no match).
- `git grep -c '@Suppress'` at `2e7094e` and at `b6a6230` — pass (12 and 12).
- Throwaway worktree, `./gradlew --offline -q :app:testDebugUnitTest --tests …` — M0, M1c, M3b, M3c, M3d, M5, M5c, M10a, M10b, M10c, LV1, LV2, M-edge, M-edge120 and M-edge121, results as tabled. Every red repeats the developer's message.
- `./gradlew --stop`, then `git worktree remove --force` and `git worktree prune` — `pgrep` finds no Gradle or Kotlin daemon, and `git worktree list` has no scratchpad entry. `PegSolitarie-p15` is clean at b6a6230. I did not touch the `Pixel_9a_bench` emulator.

## Open issues / risks
- **Staffing deviation.** This review shares the author's model (Q-019). I compensated by executing every check and mutation myself, but it does not satisfy rule 1. A Fable re-audit is owed when the policy allows.
- The four INFO items owe nothing before merge. The `HomeScreen.kt` KDoc lies in a start-path file under Decision 1, so any edit to it is the tech-lead's to schedule, never during an H-3 sitting.

## Handoff
Next: tech-lead, through the producer. H-1's test-only fix is PASS, with F-1, F-2 and F-3 closed. No QA re-walk is owed, since no production file changed. The sign-off may proceed on this branch as it stands at b6a6230.

VERDICT: PASS
