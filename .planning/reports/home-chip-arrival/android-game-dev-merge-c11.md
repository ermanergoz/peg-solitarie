# android-game-dev report — home-chip-arrival (C11: develop merge-in)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (5708d95..5708d95: the merge commit alone, parents 26c5618 and f7dd1fe)
Skills loaded: ergoz-soft:standards (android-game-dev; the snapshot 0.6.2 is older than the plugin 1.0.0, so the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing were read in full, plus the product's lessons.md), gitflow, superpowers:test-driven-development, superpowers:verification-before-completion
Ran in parallel with: licences-screen QA re-run, colour-vision QA, talkback-board tech-lead and ux rulings, play-upload developer (shared host; my Gradle runs in this worktree ran one at a time; no `--stop`, no device or emulator)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Read in full: `.planning/reports/home-chip-arrival/tech-lead-defect-ruling.md`, `code-auditor-loop2.md`, `android-game-dev.md`, `android-game-dev-loop2.md`; `/Users/ermanergoz/Desktop/PegSolitarie-dev/.planning/reports/reduced-motion-live-gate/tech-lead-signoff.md`
Lessons applied: none (the snapshot holds 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer.

## Summary
- `develop` `f7dd1fe` is merged with `--no-ff` as `5708d95`. There was no textual conflict, and the commit has one trailer.
- **Check: FAIL** (exit 1, 11m 26s). `:app` ran 1,262 tests and 2 failed. Both are this branch's Home tests that turn animations off through `Settings.Global`, which RM's gate no longer reads.
- This is a semantic conflict with `develop`. As the dispatch asks for a code conflict, I stopped and edited no test.
- Everything else in `check` is green, and the 19 named classes (101 tests) are green under a forced rerun.
- The APK sha256 is `3f66c5b6…21332f7`. Arm A is `f7dd1fe`. Arm B is not established while the Check is red.

## Outputs
- Merge commit `5708d952b27ca28f899d828905bca97fb7f9c6b0` in `/Users/ermanergoz/Desktop/PegSolitarie-home`. Its tree `216b4f38…` equals the `git merge-tree` preview.
- The debug APK at `/Users/ermanergoz/Desktop/PegSolitarie-home/app/build/outputs/apk/debug/app-debug.apk`, sha256 `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`.
- Logs, not committed, in `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/892cca18-9d7b-4228-9dc6-998ff1aadd3e/scratchpad/home-c11/`: `check-merge.log`, `check-dryrun.log`, `rest.log`, `named.log`, `repro.log`, `apk.log`.

## The merge
- **One file changed on both sides since `6aa7624`:** `app/src/test/.../ui/game/GameFlowTest.kt`. Git merged it automatically, and `git show --cc 5708d95` prints nothing, so there is no conflict hunk to re-read.
  - develop's hunks there are RM's harness switch to `SystemMotionScale`.
  - This branch's timer match, which the ruling says stays unedited, is unchanged. It moved from :129 to :132–136.
- **develop's code:** `ui/theme/Clay.kt`, `GameScreen.kt`, `TutorialScreen.kt`, `SettingsRepository.kt`, `SettingsScreen.kt`, `SettingsUiState.kt`, `app/build.gradle.kts`, plus RM's tests.
  - `Clay.kt` is in a start-path directory, so the merge-in had to come before the sitting (ruling §4.3).
- **This branch's code:** its 11 `app/src/main` files. The two sides' code is disjoint.

## The red, diagnosed and not fixed
| Case | Failure |
|---|---|
| `BoardCanvasFirstFrameTest` › `theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab` (:94 → :114) | "peg ink inside home_stage_art in the first frame, in px / expected to be greater than: 0 / but was: 0" |
| `HomeChipArrivalTest` › `withAnimationsOffTheRoomIsOpenInTheNextFrame` (:179 → :473) | "every frame after the emission, the first included / expected: 1 / but was: 15": the room opens 0 → 21 → 36 → … → 96 dp |

- **Cause.**
  - Since RM, `rememberAnimationsEnabled()` (`Clay.kt:219-221`) reads only `coroutineContext[MotionDurationScale]`, and it reads "on" when no scale is present.
  - Both cases use a plain `createComposeRule()` and write `Settings.Global.ANIMATOR_DURATION_SCALE = 0f` (`BoardCanvasFirstFrameTest.kt:85-89`, `HomeChipArrivalTest.kt:170-174`).
  - RM's `AnimationsEnabledGateWithoutScaleTest`, which is green here, asserts the gate reads `true` in exactly that setup.
  - So the stage art's entrance plays and blanks the first frame, and the chip room animates instead of opening at once.
- **Why RM's retarget missed them.** Both files were added on this branch (`ea094e2`, `98b66d7`), after RM's cut at `6aa7624`. RM's ruling retargeted only the seven files that existed there.
- **Green before the merge:** code-auditor loop 2's `--rerun-tasks` Check at `1e2bc3d`'s code passed 1,251/1,251.
- **Deterministic:** re-run alone, the same two cases fail with the same messages.
- **Reach:** no other test in the merged tree writes a `Settings.Global` animation scale, apart from `AnimationsEnabledGateWithoutScaleTest`, which does it on purpose. So no third test is passing without testing anything.
- **Production:** these failures do not point to a production bug. On a device the gate reads the window's own scale, and RM's QA passed on a device.
  - Until a remedy lands, Home's reduced-motion path has no passing JVM test on the merged tree.
  - Session S's 15v recordings with animations off are its device half.
- **Remedy (proposed, not applied).**
  - Two test files change; the assertions stay as they are.
  - Each case takes RM's `SystemMotionScale` as the rule's `effectContext` and turns it off before composing, the way `GameFlowTest` now does (`2b98c5c`).
  - The then-unused `ANIMATIONS_OFF` constants and `Settings` imports are dropped.
  - Both files already use the v1 `createComposeRule`.
- **Reviewer probe:** with the scale at 0, force the gate on. Each case should still go red, because the animation clock now reads the same 0.

## Arms for the sitting
- **Arm A: `f7dd1fe`**, which is `git merge-base 5708d95 develop`.
  - develop has since moved to `3ab1cae`, which adds only `.planning/reports/STATUS.md`.
  - The merge-base is unchanged, and a `git merge-tree` against `3ab1cae` is clean.
- **Arm B: not established while the Check is red.**
  - `5708d95` is the last commit that changes the APK, sha256 `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`.
  - A test-only remedy changes nothing that goes into the APK, so it would leave this APK as arm B's.
  - Whether the sitting waits for the remedy's green Check is the producer's call.

## Checks
- `git merge-tree --write-tree HEAD develop`, before the merge — pass: tree `216b4f3`, exit 0.
- `git merge --no-ff develop -m "chore(home): merge develop" -m "Co-Authored-By: Claude Fable 5.1 &lt;noreply@anthropic.com&gt;"` — pass:
  - exit 0, with `GameFlowTest.kt` the only automatic three-way merge;
  - the tree equals the preview;
  - one `Co-Authored-By` line and no `Claude-Session` line. This is the studio trailer from rules/gitflow.md, although the work ran on Opus.
- `./scripts/check.sh --max-workers=4` at `5708d95` — **fail**:
  - exit 1, BUILD FAILED in 11m 26s (22:39:01–22:50:27);
  - 128 classes, 1,262 tests, 2 failed, 0 errors, 0 skipped;
  - green before Gradle stopped: ktlint, detekt, `lintAnalyze*` and `:domain:depthSweep`;
  - 26 `w:` lines, 0 of them from `src/main`.
- `./gradlew --max-workers=4 check --dry-run` (135 tasks) — Gradle stopped at the first failure, so these were never reached:
  - `:app:lint`, `lintDebug` and `lintReportDebug`;
  - `:domain:test`, `fuzz`, `koverVerify`, `detekt`, `ktlintCheck`, and the two literal checks (`dpSpLiteralCheck`, `gamesIdLiteralCheck`).
- `./gradlew --max-workers=4 :domain:check :app:lint` at `5708d95` — pass:
  - exit 0, 11m 7s (22:54:37–23:05:44);
  - `:domain:test` up to date: 294 tests in 33 classes, 0 failures;
  - `fuzz` up to date: 1/1, and no `fuzz-failures.txt`;
  - `koverVerify`, `depthSweep` (1/1) and the literal checks ran green;
  - lint: 64 warnings and 0 errors, the same as loop 2.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --rerun --tests &lt;the 19 classes, fully qualified&gt;` — pass:
  - exit 0, 24 s (23:06:17–23:06:42); the task executed and wrote 19 fresh XML files;
  - 101 tests, 0 failures, errors or skips.
  - RM's 13 classes (74 tests): CueRotation 4, Sound 13, GameFlow 8, HintNotice 20, WrongTapLiveMotion 2, WrongTapReducedMotion 6, Gate 2, GateWithoutScale 1, ResultsStarsLiveMotion 1, ResultsCelebrationSound 7, TutorialHaptics 4, TutorialSound 5, TutorialCompletionStar 1.
  - This branch's timer and window-theme classes (27 tests): ElapsedTimeSpeech 1, GameStatusStrip 10, GameScreenChrome 6, TimerDigits 4, WindowTheme 4, WindowThemeApi35Files 2.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --rerun --tests '…BoardCanvasFirstFrameTest' --tests '…HomeChipArrivalTest'` — fail, as expected: exit 1; 15 tests, the same 2 failed.
- `./gradlew --max-workers=4 :app:assembleDebug` at `5708d95` on a clean tree — pass, exit 0. Then:
  - `shasum -a 256` gives `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7` (22,023,475 bytes, built 23:07:36);
  - `aapt2 dump badging`: `com.erman.pegsolitarie`, versionCode 30000, versionName 3.0.0, targetSdk 36;
  - `aapt2 dump resources`: the `(v35)` and `(night-v35)` themes are present and no v27 theme is.
- End state — `git status` is clean, no build lock is held, HEAD is `5708d95`.

## Open issues / risks
1. **The merged tree is red.** The branch cannot merge, and arm B is not settled, until the two cases are retargeted and a full Check passes. RM's equivalent retarget needed a tech-lead test ruling, so I did not do this one myself (standing rule 4).
2. **No passing JVM test of Home's reduced-motion path** on the merged tree until then.
3. **The second-to-merge hunk re-read** (§14.1; RM sign-off §3) has no conflict hunk to read.
4. **Arm A is the merge-base at the sitting.** Re-check it before session S; a code change on `develop` before then means another merge-in.

## Handoff
Next: producer —
1. Write this report to the path above and commit it alone.
2. Route the red: tech-lead (confirm the retarget) → android-game-dev in fix mode (two test files, then a full `./scripts/check.sh`) → code-auditor loop 3 (the remedy diff and this merge, with the probe above).
3. Then QA session D on the arm B APK, checked by sha256; then session S; then the tech-lead sign-off (ruling §4.4–§4.5), which reads this report.
