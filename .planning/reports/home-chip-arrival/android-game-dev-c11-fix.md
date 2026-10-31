# android-game-dev report — home-chip-arrival (C11 fix: the two red Home tests retargeted)
Date: 2026-10-31
Branch: bugfix/home-chip-arrival (6cbf333..6cbf333: the fix commit alone, parent 91ca4c2)
Skills loaded: ergoz-soft:standards (android-game-dev; the snapshot 0.6.2 is older than the plugin 1.0.0, so the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing were read in full, plus the worktree's lessons.md), gitflow, superpowers:test-driven-development (with its writing-good-tests.md), android-testing, compose-skill, compose-ui
Ran in parallel with: QA device session D (it installs the preserved arm B APK), and other branches' work on the shared host. My Gradle runs in this worktree ran one at a time, with no `--stop`, no device and no emulator
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Read in full: `.planning/reports/home-chip-arrival/tech-lead-c11-ruling.md`, `.planning/reports/home-chip-arrival/android-game-dev-merge-c11.md`
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Arm B's APK was preserved before any Gradle run, as `.planning/qa/home-chip-arrival/armB-5708d95.apk`, sha256 `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`.
- `6cbf333` is §1's one test-only commit, exactly per the table. Both classes are green under `--rerun`: 15/15.
- The probe gave 2 of 15 red, with the messages §1 predicts. The chip room shows **2** distinct frames, not 15, so the clock reads the provided scale.
- `./scripts/check.sh --max-workers=4` is green: 1,262 tests and 0 failures. A full test compile prints 43 `w:` lines at both `5708d95`'s sources and the fix, 0 of them from `src/main`.
- §4's diff condition exits 0, so arm B stays `5708d95`'s APK.

## Outputs
- `6cbf333f82f6669b818be85e02001f8d03d8e6ed`, `test(home): drive reduced motion through the composition's scale`, in `/Users/ermanergoz/Desktop/PegSolitarie-home`. It touches 2 files, +10 −22:
  - `app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasFirstFrameTest.kt`: `private val motion = SystemMotionScale()` above the rule, and the rule is now `createComposeRule(effectContext = motion)`. In `theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab`, the write became `motion.turnAnimationsOff()`, in place, as the first statement. The `Settings` and `RuntimeEnvironment` imports, and `ANIMATIONS_OFF` with its KDoc, are gone, and the `SystemMotionScale` import is added.
  - `app/src/test/java/com/app/pegsolitaire/ui/home/HomeChipArrivalTest.kt`: the same field and rule. In `withAnimationsOffTheRoomIsOpenInTheNextFrame`, `motion.turnAnimationsOff()` replaces the write, in place, as the first statement, before `renderWithThePausedClock()`. The same removals and the same import.
  - Unchanged: all 15 test names; every other line of every body; every helper, assertion and KDoc. No test was added or removed, and no file outside the table changed.
  - No KDoc on the new field: the table lists none, and 4 of the 10 existing `SystemMotionScale` fields carry none (for example `AnimationsEnabledGateTest`).
- `/Users/ermanergoz/Desktop/PegSolitarie-home/.planning/qa/home-chip-arrival/armB-5708d95.apk`: arm B, git-ignored, 22,023,475 bytes, sha256 `3f66c5b6…21332f7`.
- Logs, not committed, in `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/892cca18-9d7b-4228-9dc6-998ff1aadd3e/scratchpad/home-c11-fix/`: `r1-retarget.log`, `r2-probe.log`, `check.log` (with `check.status`, `check.times` and `check.head`), `wA-5708d95.log`, `wB-6cbf333.log` and `commit-msg.txt`.
- This report, committed alone.

## Checks
All commands ran in `/Users/ermanergoz/Desktop/PegSolitarie-home`.

**1. §4's APK copy, made first.** A usage limit cut this step on 2026-10-24 after the copy and the two edits, and it resumed on 2026-10-31. No Gradle run happened in between: the head and the diff were unchanged on resume.
- `cp -p app/build/outputs/apk/debug/app-debug.apk .planning/qa/home-chip-arrival/armB-5708d95.apk`, then `shasum -a 256` on both — pass.
  - Made 2026-10-24 23:32:06 +03, before any Gradle run.
  - Both files hash `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`: 22,023,475 bytes, built 23:07:36.
  - `git check-ignore -v` gives `.gitignore:47:.planning/qa/`.
- On resume, 2026-10-31 19:41:38, `shasum -a 256 .planning/qa/home-chip-arrival/armB-5708d95.apk` — pass: the same hash.

**2. The footprint.**
- `grep -nE 'Settings\.|import android.provider.Settings|RuntimeEnvironment|ANIMATIONS_OFF|ANIMATOR_DURATION_SCALE'` over the two files — pass: no match, exit 1.
- `grep -c '@Test'` — 2 + 13 = 15.
- `git show --stat HEAD` — pass: only the two files, 10 insertions and 22 deletions.
- `git show -U0 6cbf333 | grep -E '^[-+].*(assert|containsExactly|isEqualTo|hasSize|isEmpty|isGreaterThan|isNotEqualTo)'` — pass: prints nothing, exit 1.
- `git log -1 --format=%B | grep -c 'Co-Authored-By'` — pass: 1. It is the last line, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, and there are 0 `Claude-Session` lines. This is the studio trailer of rules/gitflow.md, although the work ran on Opus. No git hook is installed, so the message is exactly as written.

**3. The retarget, before the commit.** `./gradlew --max-workers=4 :app:testDebugUnitTest --rerun --tests 'com.app.pegsolitaire.ui.components.BoardCanvasFirstFrameTest' --tests 'com.app.pegsolitaire.ui.home.HomeChipArrivalTest'` — pass: exit 0, BUILD SUCCESSFUL in 52 s.
- XML written at 2026-10-31T16:42Z: BoardCanvasFirstFrameTest 2/2, HomeChipArrivalTest 13/13. 0 failures, errors or skips.
- The incremental test compile printed 2 `w:` lines: the v1 deprecation at `BoardCanvasFirstFrameTest.kt:60:23` and at `HomeChipArrivalTest.kt:93:23`. That is the one warning each file had before, on the call it replaces.

**4. The probe, never committed.**
- *The edit:* `rememberAnimationsEnabled()`'s body (`Clay.kt:219-220`) became `return true`; `git diff --stat` showed 1 file, +1 −2. The whole body went, so no unused local trips `src/main`'s warnings-as-errors. Each case still turns the scale to 0.
- *The run:* the command from check 3 — exit 1, as expected. BUILD FAILED in 20 s, "15 tests completed, 2 failed". Exactly the two cases failed; the other 13 were green.
  - `BoardCanvasFirstFrameTest › theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab`, at `:111` from `:91`: "peg ink inside home_stage_art in the first frame, in px / expected to be greater than: 0 / but was: 0".
  - `HomeChipArrivalTest › withAnimationsOffTheRoomIsOpenInTheNextFrame`: "the Classic chip arriving: every frame after the emission, the first included / value of: iterable.size() / expected: 1 / but was: 2".
- *The 2 distinct frames:*
  - first, ribbon top 446 dp, room 0 dp, 1 chip;
  - then the settled frame, ribbon top 350 dp, room 96 dp, 1 chip.
  - So the room's tween ended at its first animation frame. That is §1's expected 2 or 3, not C11's 15.
- `git checkout -- app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt`; `git diff --quiet HEAD` — pass: exit 0, the gate's two lines are back.

**5. The Check.** `./scripts/check.sh --max-workers=4` at `6cbf333` — pass.
- Exit 0, BUILD SUCCESSFUL in 11m 26s (19:44:39–19:56:06). 104 actionable tasks: 19 executed, 7 from cache, 78 up to date.
- `:app:testDebugUnitTest` executed: 128 classes, **1,262 tests, 0 failures**, 0 errors, 0 skipped. Every XML file is from 19:47:25.
- `:domain:test` up to date: 294 tests in 33 classes, 0 failures. `fuzz` up to date, and there is no `fuzz-failures.txt` in `build/`, `app/build/` or `domain/build/`.
- Executed and green: `:domain:koverVerify`, `:domain:depthSweep`, the literal checks (`dpSpLiteralCheck`, `gamesIdLiteralCheck`) in both modules, and `:app:detekt` and `:app:ktlintCheck`.
- `:app:lint`, `lintDebug` and `lintAnalyzeDebugUnitTest` executed. `lintReportDebug` was up to date, so the analysis came out unchanged: 0 errors and 64 warnings, the same as loop 2 and the merge-in.
- The log has 0 `w:` and 0 `e:` lines, because `compileDebugUnitTestKotlin` was UP-TO-DATE (compiled in checks 3–4) and `compileDebugKotlin` came FROM-CACHE. So check 6 measures the warnings.

**6. The test compilation's `w:` count, A/B, full compiles.** Each side ran `./gradlew --max-workers=4 :app:compileDebugUnitTestKotlin --rerun`.
- *A (`5708d95`'s sources):* `git show 5708d95:<file> > <file>` for the two files; `git diff --quiet 5708d95 -- app/src` exit 0.
  - Pass: 15 s, 43 `w:` lines, all distinct, 0 from `src/main`.
  - 42 are the v1 `createComposeRule` deprecation; 1 is the reflection warning at `GameScreenSoundTest.kt:304`.
  - The two files contribute one each, at `:59:23` and `:92:23`.
- *Restore:* `git checkout HEAD -- <the two files>`; `git diff --quiet HEAD` exit 0.
- *B (`6cbf333`):* pass: 23 s, 43 `w:` lines with the same split, 0 from `src/main`.
  - The two files contribute one each, at `:60:23` and `:93:23`.
  - The other 41 lines are byte-identical to A, and the message multiset is identical.
  - **Unchanged: 43 = 43.** The worktree ends clean, with the test classes compiled at HEAD.

**7. §4's diff condition.** `git diff --quiet 5708d95 HEAD -- . ':(exclude)app/src/test' ':(exclude).planning'` — pass: exit 0 at `6cbf333`. `git diff --stat 5708d95 HEAD` lists the two test files and two `.planning` reports.

**8. Supporting reads.**
- `git grep -n ANIMATOR_DURATION_SCALE HEAD -- app/src/test` — read: only `AnimationsEnabledGateWithoutScaleTest.kt:29` (on purpose) and `SystemMotionScale.kt:16`'s KDoc.
- `git rev-parse --short develop`; `git diff --name-only f7dd1fe develop`; `git merge-base HEAD develop` — read: `develop` is at `ae8bd2d`, and since `f7dd1fe` it changes only `.planning/reports/STATUS.md`. The merge-base is still `f7dd1fe`.

## Open issues / risks
1. **The `w:` baseline §1 and §2(d) compare against.** It is 43, not the merge-in report's 26.
   - The 26 came from an *incremental* `compileDebugUnitTestKotlin`: 13 distinct lines, each printed twice, all from files the merge changed. Neither of these two files is among them, although both called the deprecated v1 rule then.
   - A Check whose test compile is up to date prints 0, as check 5 shows.
   - The comparable figure is a full compile, 43 at both sides (check 6). I expect the auditor's `--rerun-tasks` Check to show those 43, all from `app/src/test`.
   - Whether 43 replaces 26 as the figure is for the tech-lead and the auditor. I report the measurement only.
2. **Arm A** is still `f7dd1fe`: `develop` `ae8bd2d` has gained only `STATUS.md`. Under §4, the producer re-checks this before session S.
3. **The interruption** (check 1) cost nothing: the copy, the edits and the head were all verified on resume before any Gradle run.
4. **Q-019:** the fix ran on Opus, as did the ruling. Loop 3's own P1–P3 are the mitigation.

## Handoff
Next: producer — (1) Read this report; it is committed alone. (2) Dispatch the code-auditor's loop 3 (§2) on `26c5618..6cbf333`: the merge `5708d95` and the fix. (3) Session D can use the preserved APK now. (4) Session S waits for loop 3's PASS, on a quiet host.
Next: code-auditor — §2 (a)–(d) and P1–P3 at `6cbf333`. Check (a) against §1's table with the greps in check 2. For (d)'s `w:` count, expect 43 from a full compile (open issue 1).
Next: qa-engineer — session D on `.planning/qa/home-chip-arrival/armB-5708d95.apk`, checked by sha256 `3f66c5b6…21332f7`. It stands for the fix head, because §4's diff condition exits 0.
Next: tech-lead (a fresh one) — the sign-off, reading this report (§1, "What the developer reports"), and a ruling on open issue 1's `w:` figure.
