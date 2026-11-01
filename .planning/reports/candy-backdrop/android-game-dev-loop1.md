# android-game-dev report — candy-backdrop, loop 1 (the code-auditor's F1–F4, and B-22a)
Date: 2026-11-01
Branch: feature/candy-backdrop (3407bfa..38daff4, on 2956bfa; `develop` not merged in, as dispatched)
Skills loaded: ergoz-soft:standards (role android-game-dev). The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing. I also read the product's `.claude/rules/lessons.md`. Other skills: superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: the ux-designer's B-22a ruling (docs only; the producer committed it as 2956bfa before this loop's first commit), and emulator sessions on the host that the dispatch did not name
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **F1 (high) is fixed, test-first.** A frame at animator scale 0 now leaves the backdrop's time unchanged. The red ran on the real `AppNavHost`, turning animations on, then off for two frames, then on again: the time read `9223372036854775807`. After the fix the time resumes from 4,960 ms and moves again, and the clock reports Running.
- **B-22a is built, test-first.** On the live clock, a gel tile draws its sheen only while the clock is Running. The red ran with the rule absent: the settle began inside a pass, and at rest Classic's lip still differed from the no-sheen tile by 560 pixels. It still differed by 560 pixels 10 s later.
- **F2:** every screen preview the auditor named, and Home's compact preview, now goes through `BackdropPreviewHost` with a stated window. Each of Home's two states now shows in light and in dark.
- **F3:** the three constants moved to `BackdropMetrics` and `BackdropMotion`. **F4:** the `ModifierParameter` lint warning is gone.
- **The full Check is green.** `./scripts/check.sh --wait --max-workers=4` on 38daff4 gave BUILD SUCCESSFUL in 11m 9s. All 1,297 app tests ran and passed, with no failures and none skipped.

## Outputs
Production (`app/src/main/java/com/app/pegsolitaire/`):
- `ui/backdrop/BackdropClock.kt`:
  - **F1:** `advanceTime(dtMs, scale)` moves the time only when the scale is above `NO_SCALE` (0) and finite.
  - **B-22a:** `BackdropClock.showsSheen` defaults to `!isStatic`. `LiveBackdropClock` overrides it with `!static && motionState == Running`.
- `ui/home/GelTile.kt`:
  - **B-22a:** the derived sheen value is hidden unless `clock.showsSheen` is true.
  - **F3:** the brush uses `BackdropMetrics.GEL_SHEEN_CENTRE`.
- **F3, the constants' new homes:**
  - `ui/backdrop/BackdropMetrics.kt`: `WAVE_FRONT_ALPHA = 1f` (§3.2) and `GEL_SHEEN_CENTRE = 0.5f` (§3.5.2).
  - `ui/backdrop/BackdropMotion.kt`: `GEL_SHEEN_CLASSIC_DELAY_MS = 0L` (§3.5.2).
  - `ui/backdrop/BackdropGeometry.kt` and `ui/home/HomeScreen.kt` lose their private copies.
- **F2, the previews:**
  - `ui/backdrop/BackdropPreviewHost.kt`: new shared window constants `BACKDROP_PREVIEW_WIDTH_DP` (412) and `BACKDROP_PREVIEW_HEIGHT_DP` (915).
  - `ui/backdrop/SceneBackdrop.kt`: uses those constants and drops its own private copies.
  - `ui/home/HomeScreen.kt`:
    - `HomeScreenFirstFramePreview` and `HomeScreenBusiestPreview`, each in light and dark at 412×915;
    - the compact preview at 360×640;
    - all of them through the host at Landscape.
  - `ui/home/HomeHero.kt`: 360×640, Landscape.
  - `ui/game/GameStatusStrip.kt`: 360×640, Quiet.
  - `ui/onboarding/TutorialScreen.kt`: light and dark at 412×915, Sparse.

Tests (`app/src/test/java/com/app/pegsolitaire/`):
- `ui/backdrop/BackdropReducedMotionTest.kt`: new test `turningAnimationsOffAndOnAgainWhileTheBackdropMovesResumesItsClock` (F1).
- `ui/backdrop/BackdropSettleTest.kt`: new test `aSettleThatBeginsMidPassEndsTheGelSheen` (B-22a, BD-16 (c)), plus its helpers `RealHome`, `classicLipRun`, `pixelsIn` and `differingFrom`.
- `ui/backdrop/BackdropMetricsTest.kt`: new test `theFrontWavesAlphaTheSheensPeakAndClassicsLeadAreTheSpecs` (F3).
- `testing/BackdropTestHost.kt`:
  - `AppNavHostWithStandIns` takes an optional `home` slot;
  - new `BackdropClockProbe`;
  - **F4:** `StandIn`'s modifier defaults to `Modifier`, and the four call sites that relied on the old default pass `Modifier.fillMaxSize()`.
- No existing assertion was edited or removed. `git diff 12136cb..HEAD -- app/src/test | grep '^-'` lists only the test host's helper lines and one KDoc line.

## Commits
| Commit | Kind | What |
|---|---|---|
| 3407bfa | red | F1's test, and the host's Home slot and clock probe |
| de179f7 | test fix | F1's watch bound widened by one 16 ms test frame (note 1) |
| 915cd80 | green | F1: a frame at scale 0 leaves the time unchanged |
| 5f78cd4 | red → green | F3: the metrics test, then the move (the red is a compile error, below) |
| c81db4f | red | B-22a's test |
| 8fbe40e | green | B-22a: `showsSheen` |
| 316fe5b | refactor | detekt: `run` had reached cyclomatic complexity 16 (limit 14) with F1's guard, so the time step moved into `advanceTime`. No suppression |
| 2e277d8 | test style | B-22a's pixel reader wrapped to the 120-column limit |
| 3747bdf | fix | F4 |
| 38daff4 | fix | F2 |

Every commit ends with one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, as plugin rules/gitflow.md v1.0.0 requires. See Open issue 1.

## Reds (each watched failing before its fix, then re-run against the final tests)
**F1.** Command: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropReducedMotionTest'`, run on 3407bfa (1 of 3 failed).
```
time once animations are back on, no jump past before the toggle (4960 ms)
expected to be at most: 5060
but was               : 9223372036854775807
```
- **Re-run against the final test:** I removed the `scale > NO_SCALE && scale.isFinite()` guard and got the same message, byte for byte, then reverted it.
- **Green:** time resumes at 4,960 ms and moves over the next 5 s. `isStatic` is false and `backdropMotion` reads Running.

**B-22a.** Command: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropSettleTest'`, run on c81db4f (1 of 6 failed).
- **Every guard held:**
  - the control was Running, inside the pass, and its lip differed from the reference;
  - the settle began at 68,223 ms, inside the pass;
  - the clock came to rest at 68,959 ms, still inside the pass.
- **Rule absent, the AtRest frame differs:**
```
pixels of Classic's lip that differ from the tile drawn with no sheen
but was : {the frame the settle begins (68223 ms)=1366, settling, frame 10 (68382 ms)=1365,
  settling, frame 20 (68534 ms)=1364, settling, frame 30 (68669 ms)=1343, settling, frame 40 (68773 ms)=1012,
  settling, frame 50 (68845 ms)=819, settling, frame 60 (68892 ms)=706, settling, frame 70 (68922 ms)=638,
  settling, frame 80 (68941 ms)=599, settling, frame 90 (68952 ms)=575, settling, frame 100 (68957 ms)=565,
  settling, frame 110 (68959 ms)=560, settling, frame 120 (68959 ms)=560,
  at rest (68959 ms)=560, at rest, 10000 ms later=560}
expected every entry = 0
```
- **Re-run against the final test:** I removed the `LiveBackdropClock.showsSheen` override, so the clock fell back to the default `!isStatic`. The map was identical, then I reverted it.
- **Green:** every entry is 0.

**F3.** Command: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropMetricsTest'`, before the move. It failed to compile with `Unresolved reference 'WAVE_FRONT_ALPHA'`, `'GEL_SHEEN_CENTRE'` and `'GEL_SHEEN_CLASSIC_DELAY_MS'`: the holders lacked the constants. After the move it is green, 7 of 7.

### How the B-22a test is built (the spec's four steps)
- **The host.** The real `AppNavHost` (`AppNavHostWithStandIns` with the real `HomeScreen` in its Home slot), inside `AppTheme(light)`, at `w412dp-h915dp`, under `@GraphicsMode(NATIVE)`. Frames come from the test clock through `CountingFrames`.
- **The reference.** The same Classic tile, drawn after Home's entrance with `LocalBackdropClock` re-provided as `StaticBackdropClock`. The reference is read in the same run.
- **The read area: the lip's straight run.** It spans x from `gel_lip_inset + gel_lip_corner` to the matching inset on the right, and y over `gel_lip_top..gel_lip_bottom`. Every pixel there lies inside both the lip and the opaque tile body. The backdrop never shows through the tile's 28dp corner, so its motion cannot leak into the comparison.
- **The timing.**
  - The player's last input is given at clock time 8,200 ms, which is 82% into the 10 s loop; the pass starts at 75%.
  - The settle therefore begins 60 s later at the same point of the loop.
  - The 2 s ease adds about 752 ms of clock time. I computed that from `FastOutSlowIn` on 16 ms frames, and the run measured 736 ms. So the clock comes to rest inside the same pass.
  - The input goes through `clock.onInput()`, which is what the host's observer calls on a pointer-down. The observer itself is covered by `aTapWakesItAndStillReachesTheBoard`.
- **The comparisons.** The first Settling frame, every 10th frame through the ease, the AtRest frame, and AtRest again 10 s later. All are reported together as one map.

### Notes, so nothing reads as hidden
1. **One test bound was corrected before green (de179f7).** F1's "moved at most real time" bound first read `isAtMost(5000)`, and the green run measured 5,008 ms. The Compose test clock runs frames on a 16 ms grid, so a 5,000 ms watch spans up to 5,016 ms. The bound is now `IDLE_WATCH_MS + TEST_FRAME_MS`. The F1 assertions themselves did not change: finite time, no jump, moving again, not static, Running.
2. **One Gradle run started above the load gate.** The first F1 green attempt started at a 1-minute load of 21.8, after an `uptime` check but before I had scripted the gate. From then on every Gradle run went through a script that waits for a 1-minute load below 12, and the Check started at 2.6.
3. **No new `@Suppress`, `@Ignore`, `!!`, `@OptIn` or baseline.** `git diff develop...HEAD -- app/src | grep -cE '^\+.*(@Suppress|@Ignore|!!|@OptIn)'` prints 0. In this loop's diff, "baseline" appears only in KDoc prose ("the design baseline's window").

## BD-01's commands (amended in 2956bfa), from the repository root on 38daff4
- `grep -rniwE --exclude=README.md 'candy|candies|crush|saga|candyland|tiffi|toffee|yeti|odus|king|activision|microsoft' app/src/main/res/values*/strings.xml app/src/main/assets docs/store` prints **zero lines** (exit 1). `docs/store/` holds `README.md`, `app-content.md`, `en-US/` and `tr-TR/`.
- `grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store` prints **zero lines** (exit 1).
- `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` prints **zero lines** (exit 1).

## Checks
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropReducedMotionTest'` on 3407bfa — **fail, as intended** (F1 red, above).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*'` after 915cd80 — pass (43 tests, 0 failures).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropMetricsTest'` before the F3 move — **fail, as intended** (compile red, above).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*' --tests 'com.app.pegsolitaire.ui.home.GelTileTest'` after 5f78cd4 — pass (49 tests, 0 failures).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropSettleTest'` on c81db4f — **fail, as intended** (B-22a red, above; 1 of 6 failed).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*' --tests 'com.app.pegsolitaire.ui.home.GelTileTest' --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'` after 8fbe40e — pass (67 tests, 0 failures).
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*'`:
  - first run, **fail**: detekt's `CyclomaticComplexMethod` on `LiveBackdropClock.run` (16 > 14); ktlint and the tests passed;
  - after 316fe5b: pass (45 tests).
- `./gradlew --max-workers=4 --continue :app:compileDebugKotlin :app:ktlintCheck :app:detekt` after the F2 edits — pass. `compileDebugKotlin`, `ktlintMainSourceSetCheck` and `detekt` all executed.
- Both reds re-applied in one run against the final tests, then reverted: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…BackdropReducedMotionTest' --tests '…BackdropSettleTest'` — **fail, as intended** (2 of 9, the messages above). `git status --porcelain` was empty after the revert.
- **`./scripts/check.sh --wait --max-workers=4` on 38daff4 — pass:**
  - "BUILD SUCCESSFUL in 11m 9s", "104 actionable tasks: 25 executed, 2 from cache, 77 up-to-date";
  - `:app:testDebugUnitTest` executed rather than coming from the cache: 1,297 tests (1,294 plus this loop's 3), 0 failures, 0 skipped;
  - `HomeLayoutTest` 17 of 17, `NavFlowTest` 14 of 14, `BackdropSettleTest` 6 of 6, `BackdropReducedMotionTest` 3 of 3;
  - ktlint, detekt, `dpSpLiteralCheck`, lint, `koverVerify` and `:domain:depthSweep` green.
- **Lint report `app/build/reports/lint-results-debug.xml`** (written by this Check at 04:13) — pass:
  - **`ModifierParameter` is gone (F4).** This branch now adds no lint warning.
  - Every remaining warning is in a file this branch does not touch: `strings.xml`, `App.kt`, `audio/AudioRouteClassifier.kt`, `gradle/`. Or it is in `develop`'s `dimens.xml` lines 16–23, while the branch's own `dimens.xml` hunks start at line 239.
- **BD-05:** `git diff 12136cb..HEAD -- app/src/main | grep -E '^\+' | grep -E '0x|[0-9]\.dp|[0-9]\.sp|Color\('` prints nothing (exit 1).

## Open issues / risks
1. **Commit trailer.** My commits carry plugin rules/gitflow.md v1.0.0's single `Claude Fable 5.1` trailer, as my loop-0 commits did. The code was written on Opus, and `Ran on:` says so. The producer's commits on this branch (12136cb, 2956bfa) and the auditor's carry the harness's lines instead. The auditor's Open issue 3 already asks the producer to settle which form the studio wants.
2. **`HomeStage`'s preview still draws on `background`.** F2 names screen previews, and `HomeStage` is a component whose art is its own opaque, finished slab. If the auditor wants it on the host too, it is a three-line change.
3. **B-22a's wake behaviour, accepted by the ux-designer (their Open issue 1).** After a wake, once the clock is Running again inside a pass, the rest of that pass is drawn, so a glint can appear mid-lip. QA judges it on the device.
4. **The develop merge-in is not done in this loop, as dispatched.** The auditor's M1–M4 apply when it is: the 15m oracle merge with its reds, the four content conflicts, the other pixel reads, and the full Check. colour-vision's §10.3 duties also apply then.
5. **A deprecation warning in test sources.** `createComposeRule` is deprecated in favour of the `v2` API. Every Compose suite on `develop` uses it, warnings in test sources are not gated, and this loop changed nothing about it.
6. **Device rows for QA.** First, F1: with the app resident, turn "Remove animations" on and then off, and the backdrop must move again. Second, B-22a: on Home after the 60 s settle, neither tile's lip may show a glint. Both are in addition to A10's Device rows, BD-02's captures and the 15e sitting.

## Handoff
Next: producer —
1. **code-auditor (loop 1 re-review)** of 3407bfa..38daff4:
   - F1's test and its red;
   - B-22a's test, its control and guards, and its red;
   - F2's previews and F3's holders;
   - F4's lint report;
   - BD-01's amended commands 1 and 2.
2. **qa-engineer:**
   - Open issue 6's two device checks;
   - A10's Device rows, BD-02's captures and the 15e sitting, as already owed.
3. **android-game-dev:** the `develop` merge-in under M1–M4, once colour-vision lands.
4. **tech-lead:** sign-off after the auditor's and QA's reports.
