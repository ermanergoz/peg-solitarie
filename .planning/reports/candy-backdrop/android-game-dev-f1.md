# android-game-dev report — candy-backdrop: J-4b F1 (A3's open-gate start) and the D5 guard line
Date: 2026-11-01
Branch: feature/candy-backdrop (eef1f46..fd45fd0)
Skills loaded: ergoz-soft:standards (role android-game-dev; the worktree snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, testing, kotlin-android and compose in full; puzzle-design and play-store are byte-identical to the snapshot; `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow, compose-skill, android-testing
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- F1 closed, test only: `withTheGateAlreadyOpenTheSceneIsWholeFromItsFirstFrameAndNeverFades` in `BackdropSceneOpenTest` pins A3's first branch. Under `BackdropTestHost(Landscape)` with the gate already open and animations on, `sceneAlphas()` is [1.0] on the first frame and on every frame through `framesFor(BACKDROP_SCENE_FADE_MS)`. It is red on the auditor's mutant ME.
- D5's optional line, owed now that a fix round is open: the R-1 guard asserts `sceneAlphas()` is empty on frame `FRAMES_TO_SETTLE + 1` (the 3rd) after the switch. It is red on the strand mutant M6 and on a late clean-up mutant M8, and M8 passes the pre-edit guard.
- No production line changed. The Check is green: 1,641 app tests (1,640 plus 1 new), 0 failures. BD-01 commands 1–3 print nothing.

## Outputs
- `app/src/test/java/com/app/pegsolitaire/ui/backdrop/BackdropSceneOpenTest.kt` — the new test and two imports (`BackdropTestHost`, `AppTheme`). Commit `eef1f46`.
- `app/src/test/java/com/app/pegsolitaire/ui/backdrop/BackdropReducedMotionTest.kt` — in `turningAnimationsOffMidCrossFadeSettlesItWithinTwoFrames`: one more frame after the two-frame capture, then `sceneAlphas()` read on that 3rd frame and asserted empty. The KDoc says so. No existing assertion changed. The "settled" capture now runs a cross-fade after the 3rd frame instead of the 2nd. Commit `fd45fd0`.
- Scratch (git-ignored, never committed) under `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/7be27af9-9fc5-49d9-b194-5a5598b4253b/scratchpad/f1/`: `mutate.py`, `run-mutant.sh`, the logs (`mutant-ME.log`, `mutant-M6-new.log`, `mutant-M8-new.log`, `mutant-M6-old.log`, `mutant-M8-old.log`, `green-head.log`, `lint.log`, `check-full.log`) and `pre-mutant.sha`.

## Red first (each red quoted; every mutant applied by `mutate.py` to `SceneBackdrop.kt` and restored with `git checkout`, then `shasum -a 256 -c pre-mutant.sha` printed OK)
- **ME**, `SceneBackdrop.kt:163` set to `Animatable(SCENE_HIDDEN)`. Command: `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropSceneOpenTest'`. Result: 8 tests, 1 failed, the new test: `the scene's alpha on the first frame / value of: iterable.onlyElement() / expected: 1.0 / but was : 0.0`. The other 7 pass, as the auditor found.
- **M6**, the strand. I rebuilt it from the J-4b report's description ("the cross-fade effect keyed on the gate, with no promotion"), since the exact edit was not recorded: `LaunchedEffect(density, animationsEnabled)`, with the restart's promotion (`above?.let { below = it }`, `above = null`) and the off branch's `below = density` removed. On the edited guard it is red at `BackdropReducedMotionTest.kt:149`: `scenes still composed on frame 3 after the switch / expected to be empty / but was: [1.0]`. On the pre-edit guard it is red at `:144`: `scenes still composed a cross-fade's length later / … but was: [1.0]`. That is the J-4b report's M6 failure word for word, so this is the same strand.
- **M8**, a late clean-up: `repeat(4) { withFrameNanos { } }` between `fade.animateTo(…)` and `below = density`. On the edited guard it is red at `:149`: `scenes still composed on frame 3 after the switch / expected to be empty / but was: [1.0]`. On the pre-edit guard it passes (4 of 4). This is the auditor's INFO 2: the new line tightens the bound from a cross-fade's length to one frame past the visible settle.
- Each guard run was `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropReducedMotionTest'`. For the pre-edit runs I put the HEAD copy of the test file back in place, then restored my edit and checked its hash: OK.

## Checks
- `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests '…BackdropSceneOpenTest' --tests '…BackdropReducedMotionTest'` on the unmutated head — pass (8 of 8 and 4 of 4, 0 skipped).
- `./gradlew --max-workers=4 --console=plain :app:runKtlintCheckOverTestSourceSet :app:detekt` — pass ("BUILD SUCCESSFUL in 6s").
- `./scripts/check.sh --wait --max-workers=4 --console=plain` at `fd45fd0`, clean tree, 08:57:21Z–09:08:29Z — pass. Output: "BUILD SUCCESSFUL in 11m 7s", "107 actionable tasks: 15 executed, 92 up-to-date", exit 0.
  - `:app:testDebugUnitTest` executed. Its XMLs, stamped 08:57–08:59Z, hold 1,641 tests in 194 classes, with 0 failures, 0 errors and 0 skipped.
  - `:app:lintDebug`, `:app:ktlintTestSourceSetCheck`, `:domain:depthSweep` and `:domain:koverVerify` executed.
  - `:app:detekt` was up to date from the run above, on the same sources. `:domain:test` was up to date: no domain file changed (294 tests, 0 failures).
- BD-01 command 1, `grep -rniwE --exclude=README.md 'candy|candies|crush|saga|candyland|tiffi|toffee|yeti|odus|king|activision|microsoft' app/src/main/res/values*/strings.xml app/src/main/assets docs/store` — pass (no output, exit 1).
- BD-01 command 2, `grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store` — pass (no output, exit 1).
- BD-01 command 3, `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` — pass (no output, exit 1).
- `git diff b965499..HEAD -- app | grep -nE '^\+.*(@Suppress|@Ignore|baseline|!!|Thread\.sleep|System\.currentTimeMillis|Random\()'` — pass (no output). `git diff --name-only b965499..HEAD -- app/src/main` lists nothing.
- `uptime` before each Gradle run: the 1-minute load stayed between 3.2 and 5.6, below 12. No emulator was used, `./gradlew --stop` was never run, and `.planning/log/` was not read.

## Open issues / risks
1. M6 was rebuilt from its description, because its exact edit is not on record. It reproduces the recorded failure exactly (above).
2. Trailer: these commits carry the single `Co-Authored-By: Claude Fable 5.1` trailer. That is what the worktree's 0.6.2 snapshot (`.claude/rules/gitflow.md:27-28`, imported by CLAUDE.md) and the plugin's 1.0.0 gitflow ask for. Earlier developer commits on the branch carry the harness's lines. The producer still has to settle this for the branch (auditor Open issue 6).

## Handoff
Next: code-auditor — re-check F1 only. That covers `eef1f46`'s test (red on ME, green on head) and `fd45fd0`'s D5 line (red on M6 and M8, M8 green on the pre-edit guard), on this report's Check.
Next: qa-engineer — nothing new on the device: the change is to tests only.
