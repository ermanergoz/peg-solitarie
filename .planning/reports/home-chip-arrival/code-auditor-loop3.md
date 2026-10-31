# code-auditor report — home-chip-arrival (loop 3: the develop merge-in and the C11 test fix)
Date: 2026-10-31
Branch: bugfix/home-chip-arrival (report only). Under review: `26c5618..6cbf333`, two commits: the merge `5708d95` (parents `26c5618`, `f7dd1fe`) and the fix `6cbf333`. Read and checked at `5d665c8`, which adds only the developer's report (`git diff --name-only 6cbf333 HEAD` lists that one file)
Skills loaded: ergoz-soft:standards (code-auditor; the snapshot 0.6.2 is older than plugin 1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus the worktree's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: QA device work on the shared host (an emulator, `Pixel_9_clean`, was running). My Gradle runs went one at a time: the Check first, then every probe after it. No `--stop`, no device and no emulator touched
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable. Founder answer Q-019 (option 3, re-confirmed) puts every Fable-pinned role on Opus until 3.0.0 is published, so this reviewer shares the developer's model. Mitigation: a fresh agent; my own `--rerun-tasks` Check; P1–P3 and three more probes of my own; my own A/B warning count; no recorded result taken on trust
Fan-out: none
Loops: 3
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **(a) The fix is §1's table, file by file.** Only the two files change. The field comes before the rule, and the call replaces the write in place as the first statement. The removed symbols are gone. No assertion, name, KDoc or helper changed. No suppression.
- **(b) The merge is intact.** It has no hand-resolved hunk, its tree equals `merge-tree`'s, it has one trailer, and both patch-ids on `GameFlowTest.kt` match the tech-lead's.
- **(c) The census holds.** There are 15 gate calls in 10 files (`develop`: 14 in 9). Exactly one effect is keyed on a gate-derived value, `HomeBandColumn.kt:273`, and it is checked against §3. Only `AnimationsEnabledGateWithoutScaleTest` writes the scale through `Settings.Global`. RM's 13 classes are green, 74 tests.
- **(d) My Check is green.** It ran 1,262 + 294 + 55 tests with 0 failures. Fuzz, kover and the literal checks are green, lint has 0 errors and 64 warnings, `src/main` has 0 `w:` lines, and the test compile has 43, the same as `5708d95`. §4's diff condition exits 0.
- **P1, P2 and P3 come out as ruled.** P3 has teeth: a strand mutant turns both its parts red. **VERDICT: PASS.**

## Findings
None to fix. INFO only:
- **I1. P3's part 1, as ruled, cannot see a stranded room. Only the switch back on, or part 2, shows one.**
  - Under mutant M1 (the off branch at `HomeBandColumn.kt:275` stops `openness` instead of snapping it), every off-phase frame of part 1 stayed settled. The off path reads `target`, not `openness` (`:281`), so the strand stays hidden while animations are off.
  - It shows only when animations come back on. My extra step 1b, "switch back on, every frame equals the settled one", went red: room 96 → 58 dp. Part 2 went red too: room 96 → 0 dp on the switch on.
  - None of P3 is committed. So no Check-gated test guards Home's live switch, and the guarantee §3 rests on ("openness == target", `:276`) is held by review and this probe only.
  - *Recommendation (tech-lead's call):* the 3.0.1 brief for §3's item commits P3's two parts, plus 1b, as the item's tests.
- **I2. The ruled 3.0.1 shape passes today's tests.** Probe H keyed the effect on `target` only, kept the tween guarded by `openness.value != target`, and kept the fraction reading `target` while off. Both classes (15) and P3 (2) stayed green, 17/17. This is a pre-check for the brief, not a request to change this branch.
- **I3. `HomeBandColumn.kt:280`'s `remember(present, animationsEnabled)` is keyed on the gate, but it is a `remember`, not an effect.**
  - §16 #28 says "No effect is keyed on the gate", so this is outside the rule as written.
  - It is the mechanism that lets the off path read the target in the same composition.
  - It is not counted in (c). I name it so the close sync's census sentence does not count it by mistake.
- **I4. `WrongTapFeedback.kt:173`'s `LaunchedEffect(arrived)` is not a gate key.**
  - `arrived` holds a value computed with the gate (`:167`). But it is memoised on `state.rejectedTapTick` alone (`:165`), so a gate switch never changes the effect's key.
  - This is RM's code, reviewed there. I record it as read and ruled out of the census, not as a finding.
- **I5. The `w:` figure (developer's Open issue 1).** I agree that the right figure is 43, from a full compile; the 26 came from an incremental compile.
  - My A/B full compiles: 43 at `5708d95`'s sources and 43 at `6cbf333`. Both are 42 v1 `createComposeRule` deprecations plus 1 reflection warning (`GameScreenSoundTest.kt:304`), with 0 from `src/main`.
  - The other 41 lines are identical. The two files contribute one each, moved down one line (`:59:23` → `:60:23`, `:92:23` → `:93:23`).
  - My Check log has 44 `w:` lines: those 43, plus one Kotlin-daemon diagnostic from the shared host ("Detected multiple Kotlin daemon sessions at"), which is not a source warning.
  - Ruling the figure is the tech-lead's.

## (a) The fix against §1's table
| §1 item | Holds | Evidence |
|---|---|---|
| Exactly two files under `app/src/test/java/com/app/pegsolitaire/` | yes | `git show --stat 6cbf333`: `BoardCanvasFirstFrameTest.kt`, `HomeChipArrivalTest.kt`, +10 −22 |
| `private val motion = SystemMotionScale()` **above** the rule; rule `createComposeRule(effectContext = motion)`, v1 import unchanged | yes | `BoardCanvasFirstFrameTest.kt:57` then `:59-60`; `HomeChipArrivalTest.kt:90` then `:92-93`; `import androidx.compose.ui.test.junit4.createComposeRule` at `:9` and `:16` |
| The write becomes `motion.turnAnimationsOff()`, in place, the first statement | yes | `BoardCanvasFirstFrameTest.kt:86`, before `autoAdvance = false` and `renderHome`; `HomeChipArrivalTest.kt:171`, before `renderWithThePausedClock()` |
| `Settings` and `RuntimeEnvironment` imports removed, `ANIMATIONS_OFF` and its KDoc removed, the `SystemMotionScale` import added | yes | the footprint grep finds no match (exit 1); the diff shows the 2 + 2 imports and both constants gone, and one import added per file |
| No assertion changes | yes | the assertion grep on `git show -U0 6cbf333` prints nothing (exit 1) |
| Test names, bodies, helpers and KDocs otherwise unchanged; no test added or removed | yes | the diff's only hunks are the ones above; `@Test` 2 + 13 = 15 |
| No `@Suppress`, `@Ignore`, `!!`, disable or baseline | yes | the `^\+` grep over the fix, and over the merge against its first parent, is empty; no baseline, `lint.xml` or detekt config file is in `26c5618..6cbf333` |
| Commit hygiene | yes | subject 64 characters, conventional; exactly one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, as the last line; 0 `Claude-Session` lines |

Per-test isolation: JUnit builds a new class instance per test, so each case gets a fresh `SystemMotionScale` at `ON`. The 13 siblings run with the gate on, as an absent element would have them, so no state leaks across cases.

## (b) The merge's integrity
- `git show --cc --format= 5708d95 | wc -l` gives 0, so there is no hand-resolved hunk.
- `git merge-tree --write-tree 26c5618 f7dd1fe` = `git rev-parse 5708d95^{tree}` = `216b4f38ae5e…`.
- The trailer: exactly one `Co-Authored-By` (Fable 5.1), the last line; 0 `Claude-Session` lines; subject `chore(home): merge develop`.
- The only file changed on both sides since the merge-base `6aa7624` is `GameFlowTest.kt`. Its patch-ids (`git patch-id --stable`):
  - `6aa7624..f7dd1fe` = `26c5618..5708d95` = `06473472d04d…` (RM's side intact);
  - `6aa7624..26c5618` = `f7dd1fe..5708d95` = `73d59c3251e5…` (this branch's side intact).
- `develop`'s own code (`Clay.kt`, `GameScreen.kt`, `TutorialScreen.kt`, `SettingsRepository.kt`, `SettingsScreen.kt`, `SettingsUiState.kt`, `app/build.gradle.kts` and RM's tests) was not reviewed again, per §2(b). The `build.gradle.kts` hunks only restructure comments and test inputs; no dependency, manifest or `THIRD_PARTY.md` change is in the range.

## (c) The semantic re-read and its census, at the fix head
1. **Gate call sites: 15 in 10 files** (`git grep 'rememberAnimationsEnabled()' 6cbf333 -- app/src/main`, without the definition). `f7dd1fe` has 14 in 9, and the one this branch adds is `HomeScreen.kt:399`.
2. **Effects keyed on the gate, or on a value derived from it: exactly one, `HomeBandColumn.kt:273`.**
   - I read every `LaunchedEffect` and `DisposableEffect` in the 10 files. I also followed every gate-derived value into the file it is passed to:
     - `rememberChipRoom` → `HomeBandColumn.kt`;
     - `rememberWrongTapFeedback` → `WrongTapFeedback.kt`;
     - `rememberSelectionPulse`, `runJumpAnimation`, `dockSpec`.
   - The others are keyed on `transition?.id`/`geometry`/`interactive`, `state.selected`/`dotRePops`, `coachTick`, `Unit`, `target`, `play`/`earned`, `raiseKey`, `context`, `arrived` (I4), `hostState`/`message` and `state.tutorialPending`. None is the gate or derived from it.
   - The gate-derived `animate`, `active` and the `HomeBlobs`/`UndecidedNoticeSlot` branches choose a spec or a structure. None is an effect key.
   - The `ChipRoom` objects flow only into `HomeBandColumn` and `ChipRoomSlot`, which key no effect on them.
   - **`:273` against §3:** the effect, the off branch's snap (`:274-275`), the guarded tween (`:276`) and the fraction that reads `target` while off (`:281`) are as §3 describes. P3 below is green, so §3's ruling stands: non-gating, recorded, 3.0.1.
3. **Tests that set the animator scale through `Settings.Global`:** only `AnimationsEnabledGateWithoutScaleTest.kt:27-29`, on purpose. The only other `ANIMATOR_DURATION_SCALE` match in `app/src/test` and `app/src/androidTest` is `SystemMotionScale.kt:16`'s KDoc. 12 test files now construct `SystemMotionScale()`.
4. **RM's 13 named classes (RM sign-off §3), green in my Check:**
   - `GameScreenCueRotationTest` 4, `GameScreenSoundTest` 13, `GameFlowTest` 8, `GameScreenHintNoticeTest` 20;
   - `BoardCanvasWrongTapLiveMotionTest` 2, `BoardCanvasWrongTapReducedMotionTest` 6;
   - `AnimationsEnabledGateTest` 2, `AnimationsEnabledGateWithoutScaleTest` 1;
   - `ResultsStarsLiveMotionTest` 1, `ResultsCelebrationSoundTest` 7;
   - `TutorialHapticsTest` 4, `TutorialSoundTest` 5, `TutorialCompletionStarLiveMotionTest` 1.
   - In all, 74 tests with 0 failures, errors or skips. Every timestamp falls inside my run. The two fixed classes are green too: `BoardCanvasFirstFrameTest` 2/2 and `HomeChipArrivalTest` 13/13.

## (d) My Check
`./scripts/check.sh --max-workers=4 --rerun-tasks` in `PegSolitarie-home` at `5d665c8`, whose code is identical to `6cbf333`'s. It ran 20:01:16–20:13:34 +03 and exited 0: BUILD SUCCESSFUL in 12m 17s, with 104 actionable tasks, all 104 executed.
- `:app:testDebugUnitTest`: 128 classes, **1,262 tests, 0 failures**, 0 errors, 0 skipped.
- `:domain:test`: 294 in 33 classes. `:build-logic:test`: 55 in 6 classes.
- `:domain:fuzz` 1/1 and `:domain:depthSweep` 1/1. No `fuzz-failures.txt` anywhere in the tree.
- `:domain:koverVerify`, `:app:detekt`, `:domain:detekt`, both `ktlintCheck`s, and `dpSpLiteralCheck` and `gamesIdLiteralCheck` in both modules all executed green.
- `:app:lint` and `lintDebug` executed: `lint-results-debug.xml` (written 20:05) has **0 errors** and 64 warnings, the same as loop 2.
- Kotlin: 0 `e:` lines and 0 `w:` lines from `src/main`. The test compilation has 43 `w:` lines, unchanged from `5708d95` (I5).
- §4's diff condition, `git diff --quiet 5708d95 HEAD -- . ':(exclude)app/src/test' ':(exclude).planning'`, exits 0 at `6cbf333` and at `5d665c8`. So arm B stays `5708d95`'s APK.
  - `.planning/qa/home-chip-arrival/armB-5708d95.apk` hashes `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`, before my Gradle runs and after them.
  - `app/build/outputs/apk/debug/app-debug.apk` has the same hash.

## Probes
All of these ran in the throwaway detached worktree `<scratchpad>/ca-home-l3/probe-wt` at `6cbf333`, with the untracked `local.properties` copied in. Each command was `./gradlew --offline --max-workers=4 :app:testDebugUnitTest --rerun --tests …`. After each one, `git checkout -- .` restored the tree. Nothing was committed.

| # | Mutation | Expected | Result |
|---|---|---|---|
| B0 | none | 15/15 green | **15/15 green** (2 + 13) |
| **P1** (ruled) | `rememberAnimationsEnabled()`'s body (`Clay.kt:220-221`) → `return true`; each case still turns the scale to 0 | 2 of 15 red; the chip room's count not 15 | **2 of 15 red, the other 13 green.** BoardCanvas: "peg ink inside home_stage_art in the first frame, in px / expected to be greater than: 0 / but was: 0". Chip room: "every frame after the emission, the first included / expected: 1 / but was: **2**": ribbon 446 dp / room 0 dp, then 350 / 96. So the clock reads the provided scale, and the tween ends at its first animation frame |
| **P2** (ruled) | `motion.turnAnimationsOff()` deleted from both cases | each red with C11's message | **Both red, C11's exact messages.** BoardCanvas "but was: 0"; chip room "expected: 1 / but was: **15**", room 0 → 21 → 36 → 48 → 58 → 66 → 73 → 78 → 83 → 87 → 90 → 92 → 94 → 95 → 96 dp. Each green rests on that one line |
| **P3** (ruled), part 1 | temporary test: animations on, Classic chip emitted, `START_FRAMES + 2` = 5 frames in, then `motion.turnAnimationsOff()` inside `runOnUiThread` with `Snapshot.sendApplyNotifications()` | from the 2nd frame after the switch, every frame = the settled one-chip frame; hero row and tiles at no-chip bounds throughout | **Green.** At the switch the room was mid-opening (frame 5: ribbon 398 dp, room 48 dp). From the **1st** frame after the switch all 21 read frames equal the settled frame (350 / 96 dp, 1 chip). Title, gear and both tiles held their no-chip bounds in all 27 frames. Step **1b** (mine, beyond the ruling): switched back on, all 21 frames stayed equal to the settled frame |
| **P3** (ruled), part 2 | the same test style: animations off, chip emitted, then `motion.turnAnimationsOn()` | the frame after the switch = the frame before; the closing passes `assertTheRoomMovesSmoothly` | **Green.** The room was open in the first frame after the emission. All 21 frames after the switch on equal the frame before (350 / 96 dp). The closing then animated 96 → 75 → 60 → … → 1 → 0 dp: it started in frame 3 and settled in frame 17, and `assertTheRoomMovesSmoothly` passed |
| M1 (P3's teeth) | P3, with `HomeBandColumn.kt:275`'s `openness.snapTo(target)` → `openness.stop()` (a stranded room) | P3 red | **Both parts red** (I1). Part 1's off phase stayed green; 1b went red: "frame 1 after the switch back on equals the settled frame … but was: ribbonTop=388.0.dp, room=58.0.dp". Part 2: "the frame after the switch on equals the frame before it … but was: ribbonTop=446.0.dp, room=0.0.dp" |
| H (pre-check) | §3's ruled 3.0.1 shape: `LaunchedEffect(target) { if (openness.value != target) animateTo(…) }`, the fraction unchanged | green | **17/17 green**: the two classes (15) and P3 (2) (I2) |
| W (A/B) | `:app:compileDebugUnitTestKotlin --rerun` with `5708d95`'s two test files (A, `git diff --quiet 5708d95 -- app/src` exit 0), then at `6cbf333` (B) | equal counts | **43 = 43**, 0 from `src/main`, 42 deprecations + 1 reflection each. The other 41 lines are identical (I5) |

## Rulings
- **Suppressions:** none added in the fix or in the merge. Nothing to approve or reject.
- **The developer's report:** every item §1 asks for ("What the developer reports") is present, and each number I re-measured agrees: the APK copy and its hash, the two-file footprint, the empty assertion grep, 15/15, the probe's 2 of 15 with 2 distinct frames, 1,262 / 0, the 43 `w:` lines and §4's exit 0.
- **The developer's Open issue 1:** I agree that 43 is the comparable figure (I5). The ruling is the tech-lead's.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-home/.planning/reports/home-chip-arrival/code-auditor-loop3.md` — this report, committed alone. No code changed.
- Logs, not committed, in `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/892cca18-9d7b-4228-9dc6-998ff1aadd3e/scratchpad/ca-home-l3/`:
  - `check.log`, `check.status`, `check.head`, `check.start`, `check.end`;
  - `wA.log` and `wB.log`;
  - `out/<probe>/` for B0, P1-gate-true, P2-no-setup, P3-live-switch, P3m-stop-not-snap and H-house-idiom, each holding `mutation.diff`, `gradle.log`, `status` and the XML results;
  - `p3-snippet.kt` (P3's throwaway test) and `probe.sh` (the runner).

## Checks
All of these ran in `/Users/ermanergoz/Desktop/PegSolitarie-home` unless a probe is named. `.planning/log/` was not read.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0): older, so the plugin's rule files were read.
- `git status --short`; `git diff --name-only 6cbf333 HEAD` — pass: clean; only the developer's report differs from the fix.
- `shasum -a 256` of the arm B copy and of `app-debug.apk`, before any Gradle run and after the last — pass: both `3f66c5b6…21332f7` each time.
- `git show --stat 6cbf333`; `git show 6cbf333 -- app/` — pass: two files, +10 −22, the hunks of §1's table only.
- `git show -U0 6cbf333 | grep -E '^[-+].*(assert|containsExactly|isEqualTo|hasSize|isEmpty|isGreaterThan|isNotEqualTo)'` — pass: no output, exit 1.
- `grep -nE 'Settings\.|import android.provider.Settings|RuntimeEnvironment|ANIMATIONS_OFF|ANIMATOR_DURATION_SCALE'` over the two files — pass: no match, exit 1. `grep -c '@Test'` gives 2 + 13.
- `git show 6cbf333 | grep -nE '^\+.*(@Suppress|@Ignore|!!|ktlint-disable|detekt|baseline|@Disabled)'`, and the same over `git diff 26c5618 5708d95 -- . ':!.planning' ':!docs'` — pass: both empty.
- Commit hygiene (`git log -1 --format=%s|%B`) for `5708d95` and `6cbf333` — pass: subjects of 26 and 64 characters; one trailer each, the last line; 0 `Claude-Session` lines.
- `git show --cc --format= 5708d95 | wc -l` — pass: 0.
- `git merge-tree --write-tree 26c5618 f7dd1fe` against `git rev-parse 5708d95^{tree}` — pass: both `216b4f38…`.
- `git diff <range> -- GameFlowTest.kt | git patch-id --stable` over the four ranges — pass: `06473472d04d` twice and `73d59c3251e5` twice.
- `comm -12` of the two sides' changed files since `6aa7624` — pass: `GameFlowTest.kt` only.
- `git grep -n 'rememberAnimationsEnabled()'` at `6cbf333` and at `f7dd1fe` — pass: 15 in 10, against 14 in 9.
- Effect and key read across the 10 gate files, `HomeBandColumn.kt` and `WrongTapFeedback.kt` (`grep -nE 'LaunchedEffect|DisposableEffect|SideEffect|produceState|rememberUpdatedState|snapshotFlow'`, then the code itself) — pass: only `HomeBandColumn.kt:273` is keyed on a gate-derived value.
- `git grep -n ANIMATOR_DURATION_SCALE 6cbf333 -- app/src/test app/src/androidTest`, and `git grep -nE 'Settings\.Global\.put|putFloat\('` — pass: only `AnimationsEnabledGateWithoutScaleTest` and the KDoc.
- `./scripts/check.sh --max-workers=4 --rerun-tasks` — **pass**: exit 0, 12m 17s, 104 of 104 tasks executed; the totals are in (d).
- The XML totals over `app/build/test-results/testDebugUnitTest`, `domain/build/test-results/{test,fuzz,depthSweep}` and `build-logic/build/test-results/test` (python `xml.etree`) — pass: 1,262 / 294 / 1 / 1 / 55, with 0 failures, errors or skips.
- `app/build/reports/lint-results-debug.xml` severities — pass: 64 warnings, 0 errors.
- `grep '^w: '` and `grep '^e: '` on the Check log — pass: 44 `w:` lines (43 from `src/test`, plus 1 daemon diagnostic), 0 from `src/main`, 0 `e:` lines.
- §4's diff condition at `6cbf333` and at `HEAD` — pass: exit 0 both times.
- `git worktree add --detach <scratchpad>/ca-home-l3/probe-wt 6cbf333`, then the probes B0, P1, P2, P3, M1, H and W as tabled — **all as expected**. Then `git worktree remove --force` and `git worktree prune` — pass: the worktree is gone, `PegSolitarie-home` is clean at `5d665c8`, and no `.gradle/ergoz-build.lock` is held.
- `git rev-parse --short develop`; `git diff --name-only f7dd1fe develop`; `git merge-tree --write-tree HEAD develop` — read: `develop` is at `8047549` (Open issue 1); the trial merge is clean, exit 0.

## Open issues / risks
1. **`develop` has moved to `8047549` since the developer's report (`ae8bd2d`).**
   - It adds `scripts/play-upload.sh`, a new 131-line file referenced by no Gradle script, `build-logic` or `app/src` file, plus play-upload reports and STATUS.md. So no APK input changes, and arm A's APK (`f7dd1fe`) is unaffected.
   - It is a code file all the same, which §4's arm A condition ("as long as `develop` gains no code") did not foresee. Whether it needs another merge-in before this branch merges, or before session S, is the producer's re-check (ruling §4, Open issue 5), with the tech-lead.
   - `git merge-tree --write-tree HEAD develop` is clean.
2. **Home's live switch has no committed test** (I1). §3's non-gating ruling rests on P3, which lives only in this report. The 3.0.1 brief can commit it (I2 shows the ruled fix passes it).
3. **Q-019.** The author, the ruling and this review all ran on Opus. The mitigations are those on the Ran on line.
4. **The device decides behaviour.** Session D runs on the preserved arm B APK, checked by sha256. Session S waits for this PASS, on a quiet host (ruling §4).

## Handoff
Next: producer —
1. commit this report alone;
2. re-check arm A against `develop` `8047549` (Open issue 1) before session S;
3. session D and then session S on `.planning/qa/home-chip-arrival/armB-5708d95.apk` (sha256 `3f66c5b6…21332f7`);
4. then the fresh tech-lead's sign-off, which reads this report.

Next: tech-lead (a fresh one) — the sign-off. The rulings it owes:
- the `w:` figure (I5: 43);
- whether the 3.0.1 brief for §3's item commits P3 (I1, I2);
- the close sync's census wording (I3, I4).

VERDICT: PASS
