# code-auditor report — candy-backdrop (direction B build), loop 1 re-review
Date: 2026-11-01
Branch: feature/candy-backdrop (3407bfa..38daff4, on the ux-designer's 2956bfa; reviewed as `12136cb..a81e23c`, where a81e23c is the developer's report; `develop` not merged in, as dispatched)
Skills loaded: ergoz-soft:standards (role code-auditor. The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing. Also the product's `.claude/rules/lessons.md`.), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none named by the dispatch (other worktrees on the host were live: colour-vision, talkback, licences and others)
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort pin; the harness names no effort). **Staffing deviation:** `agents/code-auditor.md` pins fable, and rule 1 says code is reviewed on a model above its author's. The developer wrote this code on Opus (`android-game-dev-loop1.md:6`), so this review ran on the author's model. That is allowed by the founder's Q-019 answer, option 3, re-confirmed by its 2026-10-24 addendum (`.planning/questions/FOUNDER.md:108-111`: "every Fable-pinned role runs on Opus, each report recording the deviation, with the same mitigations"). The mitigations were applied: I ran the Check myself with the app tests forced to re-run, and I re-applied both reds and three mutations of my own rather than relying on the developer's recorded results.
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **F1, F3, F4 and B-22a are fixed and proven.** I re-applied the F1 red and the B-22a red in a throwaway worktree. Each failed with exactly the developer's message: `9223372036854775807`, and a lip that stays 560 px off. Three mutations of my own also fail: a `>=` boundary on the scale guard, a sheen shown while Settling, and the tile reading `isStatic` again. de179f7 does not weaken F1, and 316fe5b is an extraction, not a suppression.
- **The Check is green.** The app tests were forced to re-run: 1,297 tests, 0 failures, 0 skipped. The developer added no suppression, baseline, `@Ignore` or `!!`. BD-01 commands 1–3 print zero lines.
- **F2 is only partly met (G1, must fix).** `GameScreen`'s `Scaffold` is transparent on this branch. Its four previews in `ui/game/GameScreenPreviews.kt` still draw over nothing, so BD-25 does not hold for gameplay. **My loop-0 report missed this file.** The developer fixed every preview I named.
- The verdict is FAIL on G1 alone. G1 is a small change. It can go into the `develop` merge-in, which needs a fresh review round anyway (M1–M4 of loop 0).

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/code-auditor-loop1.md — this report.
- No source file was edited. Every mutation ran in a throwaway `git worktree add --detach` of a81e23c under the session scratchpad. I removed it afterwards: `git worktree list` no longer shows it, and the branch's `git status --porcelain` is empty.

## Checks
- `uptime` before every Gradle run: the 1-minute load was 3.12, 7.94, 3.46, 5.48 and 3.3–5.5 at each start, all below 12.
- **The Check** — **pass.**
  - Command, at a81e23c: `./scripts/check.sh --wait --max-workers=4 :app:testDebugUnitTest --rerun`. That is `./gradlew check` under the worktree lock, with the app test task forced to execute.
  - Result: "BUILD SUCCESSFUL in 11m 11s" and "104 actionable tasks: 12 executed, 92 up-to-date".
  - The result XMLs are stamped 2026-11-01T01:27–01:28Z, which is this run. They hold **1,297 tests, 0 failures, 0 errors, 0 skipped**:
    - `BackdropReducedMotionTest`: 3 of 3;
    - `BackdropSettleTest`: 6 of 6;
    - `BackdropMetricsTest`: 7 of 7;
    - `HomeLayoutTest`: 17 of 17.
- **Lint report** `app/build/reports/lint-results-debug.xml` — **pass, F4 met.**
  - No `ModifierParameter` remains.
  - Every remaining warning is in `gradle/`, `App.kt`, `AudioRouteClassifier.kt`, `strings.xml` or `dimens.xml` lines 16/21/23. The `dimens.xml` lines are identical on `develop`, and their dimens (`spacing_xl`, `board_min_touch_target`, `dialog_padding`) are unreferenced on `develop` too.
  - The branch adds no lint warning.
- **Suppressions** — **nothing to approve or reject.**
  - `git diff 12136cb..38daff4 -- app/src | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|baseline|detekt|ktlint-disable|noinspection|lint:)'` matches only seven KDoc lines that say "baseline window".
  - `git diff --name-status 12136cb..38daff4 | grep -iE 'baseline|detekt|lint'` prints nothing, so the detekt config and its `CyclomaticComplexMethod` limit are unchanged.
- **316fe5b** — an extraction, not a suppression.
  - The guarded time step moved verbatim from `run` into `private fun advanceTime(dtMs, scale)` (`BackdropClock.kt:213-221`), and its KDoc moved with it.
  - The condition `scale > NO_SCALE && scale.isFinite()` is byte-identical, and `run`'s loop is otherwise unchanged.
  - detekt is green in the Check above.
- **de179f7** — does not weaken F1.
  - Its only change is the last bound, "time moved … at most real time", from `isAtMost(IDLE_WATCH_MS)` to `isAtMost(IDLE_WATCH_MS + TEST_FRAME_MS)` (5,000 → 5,016 ms). One 16 ms test frame of slack is a fair allowance for the frame grid.
  - F1's own assertions are unchanged and still bite: the resumed time is ≥ the time before the toggle and ≤ that plus 100 ms; the time moves again (> 0); the clock is not static; and its semantics read Running.
  - The infinite value is caught first, by "no jump past". That assertion was never touched.
- **BD-01, the amended commands from the repository root at a81e23c:**
  - (1) `grep -rniwE --exclude=README.md 'candy|…|microsoft' app/src/main/res/values*/strings.xml app/src/main/assets docs/store` prints **zero lines** (exit 1).
  - (2) `grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store` prints **zero lines** (exit 1).
  - (3) `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` prints **zero lines** (exit 1).
  - `docs/store/` holds `README.md`, `app-content.md`, and `en-US/` and `tr-TR/` (titles, descriptions, `changelogs/30000.txt`). Only the README is excluded.
  - `find app/src/main/res app/src/main/assets -name README.md` prints nothing, so the exclusion hides no app text.
- **BD-05:** `git diff 12136cb..38daff4 -- app/src/main | grep -E '^\+' | grep -E '0x|[0-9]\.dp|[0-9]\.sp|Color\('` prints nothing (exit 1).
- **Mutations**, in the throwaway worktree. Each was compiled and run with `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…BackdropReducedMotionTest' --tests '…BackdropSettleTest'`, then reverted with `git checkout -- app`.
  - **Run 1, the developer's two reds re-applied:**
    - F1's guard made always true: `if (scale >= NO_SCALE || scale < NO_SCALE)`, which keeps `scale` used and is true at 0. The `LiveBackdropClock.showsSheen` override was deleted, so the clock falls back to `!isStatic`.
    - Result: **2 of 9 fail, as intended.**
    - `turningAnimationsOffAndOnAgain…`: "time once animations are back on, no jump past before the toggle (4960 ms) / expected to be at most: 5060 / but was: 9223372036854775807".
    - `aSettleThatBeginsMidPassEndsTheGelSheen`: the settle-begin frame (68,223 ms) differs by 1,366 px. The Settling samples go 1,365 → 560, AtRest (68,959 ms) differs by 560 px, and AtRest 10 s later still by 560 px. The map is identical to the developer's.
  - **Run 2, my own:**
    - The boundary mutation `scale >= NO_SCALE && scale.isFinite()` fails F1's test with the same `9223372036854775807`.
    - `showsSheen = !static && motionState != AtRest`, which shows the sheen while Settling, fails B-22a's test. It fails on the settle-begin frame and on every Settling sample (1,366 → 560), while both AtRest entries pass. So the test checks the Settling half of B-22a, not only the rest frame.
  - **Run 3, my own:** `GelTile.kt:70` reads `!clock.isStatic` again while the clock keeps its override. B-22a's test fails (Settling frame 120 and both AtRest entries at 560 px), and `GelTileTest` stays 5 of 5. So the tile, not only the clock, is held.
- `git worktree remove --force <probe>` was run, and `git worktree list | grep -c candy-l1` prints 0.

## Findings

### Must fix (send back to the developer)
1. **G1 — MEDIUM — BD-25 does not hold for the gameplay screen's previews.**
   - **Where:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreenPreviews.kt:19-29` and `:53-63`, the light and dark `GameScreenPreview` and `GameScreenUndecidedNoticePreview`.
   - **What is wrong:** they render `AppTheme { GameScreen(…) }` straight onto the IDE canvas. This branch makes `GameScreen`'s `Scaffold` transparent (`containerColor = Color.Transparent` in `GameScreen.kt`, BD-14), so these previews now show no backdrop and no background. The dark ones put `onBackground` text on whatever the canvas is.
   - **The rule:** BD-25 says "Screen previews go through `BackdropPreviewHost` with the clock fixed", and the tech-lead's A10 row says the same. The file is unchanged since `develop` (last touched in 45f2b96). That is why neither my loop-0 F2 list nor the developer's fix reached it: it is my omission, not the developer's.
   - **What the fix must give:** each of those previews goes through `BackdropPreviewHost(BackdropDensity.Quiet)` at a stated window, light and dark, as F2 did for `GameStatusStrip`. I do not prescribe the code.
   - **Merge note:** `develop` may have edited this file since the merge base, so it can land in the merge-in.

### Verified fixed
- **F1 — fixed** (`BackdropClock.kt:202`, `:213-221`).
  - A frame whose scale is not above 0, or not finite, leaves `timeExact` and `time` untouched. That covers both the `+∞` path (dt > 0) and the NaN path (0/0).
  - The ease, the idle timer and `lastNanos` still advance on unscaled frame time. That is unchanged and right, because the idle delay is in real seconds.
  - On re-entry `run` restarts with `lastNanos = NO_FRAME`, so the first frame back adds no jump. The test asserts this on the real `AppNavHost` (ON→OFF for two frames→ON) and on the semantics hook.
  - The two reds above show the test bites at the guard's boundary.
- **B-22a — built to the ruling** (`BackdropClock.kt:62`, `:154`; `GelTile.kt:68-70`).
  - **Sheen states:** `LiveBackdropClock.showsSheen = !static && motionState == Running`, so no sheen is drawn while Settling (the ease to 0, the wake ease, and a resume's ease) or AtRest. The interface default `!isStatic` keeps `FixedBackdropClock` drawing its pose and `StaticBackdropClock` drawing none, which is what B-22a's "Unchanged" bullet asks.
  - **The settle-begin frame:** `motionState` is set to Settling at `:195` inside the same frame callback that starts the ease, before the coroutine suspends again. So the frame the settle begins is already drawn without the sheen. The test reads that frame (1,366 px under the red, 0 green).
  - **Recomposition and draw:** the `derivedStateOf` reads `showsSheen` first and does not read `timeMs` when it is false. So a settling or settled tile subscribes to no per-frame state, and `motionState`'s same-value writes each frame do not invalidate. Nothing new is allocated in a draw lambda.
  - **The test** has the four parts the spec asks for:
    - the control (Running, inside the pass, its lip differing from the reference);
    - a settle that begins inside the pass, with a guard;
    - an AtRest frame still inside the pass, so a frozen sheen would show;
    - a same-run reference drawn with `StaticBackdropClock`.
- **F2 — met for every preview I named.** `HomeScreen.kt` (first frame and busiest, light and dark, at 412×915; compact at 360×640), `HomeHero.kt` (360×640), `GameStatusStrip.kt` (360×640, Quiet) and `TutorialScreen.kt` (light and dark at 412×915, Sparse) all go through `BackdropPreviewHost`. The developer asked about `HomeStage`'s preview (Open issue 2): no change is asked. It is a component, not a screen, and its art is its own opaque, finished slab. BD-25 names screen previews.
- **F3 — fixed.**
  - `BackdropMetrics.WAVE_FRONT_ALPHA` (`BackdropMetrics.kt:112`) and `BackdropMetrics.GEL_SHEEN_CENTRE` (`:174`) are ratios. `BackdropMotion.GEL_SHEEN_CLASSIC_DELAY_MS` (`BackdropMotion.kt:34`) is a duration.
  - The private copies are gone from `BackdropGeometry.kt`, `GelTile.kt` and `HomeScreen.kt`.
  - `BackdropMetricsTest.theFrontWavesAlphaTheSheensPeakAndClassicsLeadAreTheSpecs` pins the values.
- **F4 — fixed.** `StandIn`'s modifier defaults to `Modifier` (`BackdropTestHost.kt:147`). The four call sites that relied on the old default now pass `Modifier.fillMaxSize()`, so the stand-ins' tap regions are unchanged.

### Information (no change asked)
2. **B-22a's test samples the ease, not every frame.** It reads the settle-begin frame, every 10th Settling frame, AtRest, and AtRest 10 s later. B-22a's text says "from that frame on". The production condition is one state compare that is constant across Settling, and my Settling mutation fails on every sample. So the sampling misses no realistic defect, and it spares about 110 window captures. Accepted.
3. **F1's guard also skips a negative scale.** That is right: no platform scale is negative. A positive but tiny scale (well below the 0.5 minimum that Developer options offers) would still divide. That is not reachable from Settings, and the result stays finite. No change asked.
4. **B-22a's wake behaviour.** After a wake, the rest of a pass under way may pop in mid-lip. The ux-designer accepted this (`ux-designer-auditor-notes.md`, Open issue 1). It is QA's to judge on the device.

### Review rows re-ticked for this loop's diff
- **BD-03 / A8 C5:** the sheen is still clipped to the lip (`GelTile.kt`'s one `clipPath`), and the brush is unchanged except that its stop now comes from `BackdropMetrics`.
- **BD-04:** nothing under `res/drawable*`, `res/mipmap*`, `res/raw` or `assets/`, no image file, and no catalog or build-file edit in `12136cb..a81e23c` (`git diff --stat 2956bfa..38daff4` lists 15 Kotlin files only). No new dependency, so no `THIRD_PARTY.md` row is owed.
- **BD-16 (c), B-22a:** the test half is met, as above. The device half is QA's.
- **BD-19:** this loop edits no test that predates the branch. Its test edits are the new tests, the helpers in `BackdropTestHost.kt` (the optional `home` slot, `BackdropClockProbe`, F4), and the de179f7 bound inside this loop's own new test. 15m's oracle is untouched since my loop-0 line for e07b105.
- **Security:** no network, no permission, no secret, no new receiver, no new input surface. Clean.

## Open issues / risks
1. **Model deviation (Q-019):** this review ran on the author's model. Mitigated by the forced test re-run and my own mutations.
2. **I stopped the Gradle daemons when I removed the probe** (`./gradlew --stop` in the probe, at about 04:50 local). `--stop` reaches every daemon of that Gradle version on the host, not only the probe's. `ps` showed no Gradle process right afterwards, but a build in another worktree that failed at that minute with a stopped daemon should simply be re-run. The producer may pass this on.
3. **Commit trailer:** as in loop 0, my commit carries the harness's attribution lines. The developer's commits carry plugin rules/gitflow.md v1.0.0's `Claude Fable 5.1` trailer on Opus-written code. The producer still has to settle the form (loop 0 Open issue 3; the developer's Open issue 1).
4. **The `develop` merge-in is still owed under loop 0's M1–M4.** That covers the 15m oracle with develop's full-pill read moved onto frame 2 and its reds re-shown, the four content conflicts, the other pixel reads, and the full Check. The merged oracle needs a fresh auditor line.
5. **Device rows stay with QA:**
   - F1 on the device: with the app resident, "Remove animations" ON→OFF, and the backdrop moves again.
   - B-22a on the device: no glint on either tile's lip after Home's 60 s settle.
   - A10's Device rows, BD-02's captures (then this role's BD-02 line), and the 15e sitting.

## Handoff
Next: producer —
1. **android-game-dev:** fix G1 (gameplay previews through `BackdropPreviewHost`). It may ride the `develop` merge-in under loop 0's M1–M4. Bring back the merged 15m oracle with its reds for a fresh auditor line, and the full Check.
2. **code-auditor:** loop 2, on G1 and the merge-in.
3. **qa-engineer:** Open issue 5's device checks.
4. **tech-lead:** sign-off waits on loop 2 of this review and QA's report.

VERDICT: FAIL
