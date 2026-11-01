# code-auditor report — candy-backdrop: J-4b, the scene opens after Home's entrance
Date: 2026-11-01
Branch: feature/candy-backdrop (d029950..eee370e; code under review `8fad1b0`, `635c9b8` and the merge `eee370e`; reviewed at HEAD `0c45666`, whose `app/` tree equals `eee370e`'s. The tech-lead's `d9630a0` landed mid-review and is docs only: one new report, no code. I did not read it, so my D1 and D5 views below are my own.)
Skills loaded: ergoz-soft:standards (role code-auditor). The worktree snapshot is v0.6.2 and the plugin v1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing in full. logging, puzzle-design and play-store are byte-identical to the snapshot, which CLAUDE.md imports. I also read `.claude/rules/lessons.md`. Then code-reviewer, security-reviewer, compose-skill and compose-performance-audit.
Ran in parallel with: none named by the dispatch. The tech-lead was ruling D1 and D5 concurrently.
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable, and the developer wrote this code on Opus, so the review ran on the author's model. The founder's Q-019 answer (option 3) allows this. I applied its mitigations: I ran the Check myself with the app tests forced, re-ran ktlint and detekt by force, and re-applied mutants of my own rather than relying on the developer's results.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- The production change meets A1, A2, A4, A6 and R-1. A3 is met in code, and so are both §3 15e conditions and both §4 test amendments (setup lines only). The merge's hand-resolved hunk is correct. The Check is green: 1,640 app tests and 294 domain tests, 0 failures.
- Every mutant the dispatch named is caught: an open on frame 2 with animations on, the fade removed, and an unscaled delay (caught by the scale-2 test alone when the scale-0 path is kept). The R-1 undo is not caught by any test, as the developer disclosed. §16 #28 enforces that rule by review.
- My own probe shows that inside the real host, Home's last band settles on the gate's open frame at scale 1 and at scale 2. The scene is composed one frame later, so A6's "first visible draw follows the cascade's end" holds where it ships.
- **F1, must fix (test only).** A3's first branch, "starts at 1 when the flag is already true at its first composition", has no test. A one-token mutant survives all 1,640 app tests. Under that mutant every `BackdropPreviewHost` preview (BD-25) would draw the scene at alpha 0.
- **VERDICT: FAIL** on F1 alone. The fix is one small test, and no production line changes.

## Findings
- `app/src/main/java/com/app/pegsolitaire/ui/backdrop/SceneBackdrop.kt:163` — **F1 (must fix, test only).** The A3 branch `Animatable(if (ready) SCENE_SHOWN else SCENE_HIDDEN)` is untested.
  - **The mutant.** Mutant ME replaces it with `Animatable(SCENE_HIDDEN)`. It passes the full app suite: `:app:testDebugUnitTest` ran 1,640 tests with 0 failed, stamped 08:15Z.
  - **Why the suite misses it.** The ruling's A3 requires the scene to start at 1 when the gate is already open (`AlwaysReady`: previews and component tests). `BackdropTestHost` passes `sceneReady = mutableStateOf(true)`, and `BackdropPreviewHost` (`BackdropPreviewHost.kt:33`) takes the `AlwaysReady` default. Under ME, both start the scene at 0 and fade it in. The pixel suites still pass because they idle through the 700 ms fade.
  - **What breaks.** A static preview never advances a frame, so every BD-25 screen preview would show no waves and no lights.
  - **The fix.** Add one test, for example in `BackdropSceneOpenTest`: `BackdropTestHost(Landscape)` with `autoAdvance = false`.
    - Assert `sceneAlphas()` = [1.0] after the first frame.
    - Assert it stays [1.0] on every frame for `framesFor(BACKDROP_SCENE_FADE_MS)`.
    - It must go red on ME.

No other finding. The INFO items are under Open issues.

## What I verified against the requirements
**A1 (scaled open)** — met.
- `BackdropSceneReady.kt:40-45` runs a throwaway `Animatable(0f).animateTo(1f, tween(BACKDROP_SCENE_OPEN_MS, LinearEasing))` inside the existing `LaunchedEffect(Unit)`. There is no `withFrameNanos` first, no wall clock and no gate key.
- At scale 0 the tween ends on its first awaited frame: test 3 and mutant MC both show it.
- At scale 2 it waits 880 ms: mutant MC2 shows it.

**A2 (tokens)** — met.
- `BackdropMotion.kt:58` is `HOME_LAST_ENTRANCE_ORDER = 5`, with KDoc naming Home's orders.
- `:66` derives `BACKDROP_SCENE_OPEN_MS` from `ClayAnim`, and `:72` sets `BACKDROP_SCENE_FADE_MS = ClayAnim.NAV_TRANSITION_MS`.
- `grep -rn 'import com.app.pegsolitaire.ui.home' app/src/main/java/com/app/pegsolitaire/ui/backdrop/` prints nothing.

**A3 (one fade, from where the gate is)** — met in code, but the first branch is untested (F1).
- 0 → 1 runs only on an observed false → true flip: `LaunchedEffect(ready)`, `SceneBackdrop.kt:164`.
- With animations off, the reader returns 1 (`:172`), and test 3 pins alpha 1 on frame 3.
- The test key holds the same lambda the layer calls (`:221-223`, `:254`).
- The fade's effect is keyed on the flag only.

**A4 (`ModulateAlpha`)** — met at `SceneBackdrop.kt:223`. No JVM test can see a compositing strategy. I accept it by review, since the ruling accepted it on technical grounds.

**A5 (replay on recreation)** — accepted behaviour. There is no `rememberSaveable`, and the flag is plain `remember` in the host.

**A6** — the developer's frame timeline checks against my own runs.
- `framesToSceneOpen(16)` = 28. The tween ends on frame 30 (448 ms after frame 2), and the scene is composed on frame 31.
- My host probe printed `open=28 firstSettledFrameAfterGateStart=28 firstSceneFrameAfterGateStart=29` at scale 1, and `55 / 55 / 56` at scale 2.
- `git diff d029950..eee370e -- …/SceneBackdrop.kt` touches no line inside `drawWithCache`/`onDrawBehind`. Only the modifier chain around it changed (the `graphicsLayer` block and the trailing `.semantics`).
- The alpha is read only in the layer block and in the test reader, never in composition, so the fade recomposes nothing.
- I did not re-run the developer's reverted counter probe. The steady-state claim (layer block 0 runs after the fade) follows from the code: after the `Animatable` settles, no snapshot write reaches the block.

**R-1** — met in code. `SceneBackdrop.kt:105` is `LaunchedEffect(density)`, and the gate is captured once at launch.
- No `LaunchedEffect` in the three changed files is keyed on the gate or a gate-derived value.
- `BackdropClock.kt:123`'s `runs` key predates this work and is outside the diff. The developer recorded it.

**§3 condition 1** — holds.
- `git diff --name-only d029950..eee370e -- app/src/main` lists only `ui/backdrop/BackdropMotion.kt`, `BackdropSceneReady.kt` and `SceneBackdrop.kt`.
- `git diff --quiet d029950 eee370e -- app/src/main/java/com/app/pegsolitaire/navigation/` exits 0, and `AppNavHost.kt:170` is still `val sceneReady = rememberBackdropSceneReady()`.

**§3 condition 2** — holds. Frame 1 gains no system read, registration, decode, asset or bitmap. It gains:
- one remembered `Animatable` and one remembered reader lambda;
- `LaunchedEffect(ready)`, which returns at once while `ready` is false;
- inside the existing effect, an `Animatable` constructed after apply, whose scale read is the same `MotionDurationScale` that `rememberAnimationsEnabled()` already reads on frame 1.

**§4 `BackdropFirstFrameTest`** — amended, not weakened.
- The frame-1 assertions are untouched. The diff only adds lines after them, plus KDoc.
- The added loop asserts {Sky}, AtRest and 0 awaited on every frame from frame 2 through frame 29, the last one before the open. The count is computed from the token and the measured frame time.
- The original two-frame upper bound and its `containsExactly(Sky, Waves, Lights)` are unchanged, apart from the message.

**§4 `BackdropSettleTest.start()`** — amended, not weakened.
- Only the three `advanceTimeByFrame()` lines changed, into `advanceOneFrame()` plus `framesToSceneOpen + FRAMES_PAST_OPEN (2)`. That ends on frame 32, two frames past the open on frame 30, the same offset as frame 4 against frame 2 before.
- No assertion changed.
- No other existing test file was edited except `BackdropReducedMotionTest`, which only gains the R-1 test and its imports.

**Merge `eee370e`** — correct.
- `git diff-tree --cc eee370e` shows one combined hunk, `docs/ux-spec.md:7`. The merge base (`d251c3b`) has `revised: 2026-10-24`. Ours replaced it with `2026-10-31 (15m pointer, B-19a)`, and develop appended `2026-11-01 (pre-release folds…)`. The resolution keeps all three entries in date order.
- `git diff --name-only 635c9b8 eee370e` outside `.planning/` and `docs/` is `AGENTS.md` and `CLAUDE.md` only, so the merge brought no code.

**Security and policy.**
- No new dependency, so `THIRD_PARTY.md` is unchanged and needs nothing.
- No network, secret, `@Suppress`, `@Ignore`, baseline or `!!`. **No suppression to approve or reject.**
- Test names read as sentences, and the tests are deterministic: test clock, no sleeps.

## Views on D1 and D5 (non-binding; the tech-lead rules)
**D1, the alpha hook on the scene node: I agree with the developer's choice.**
- The reader belongs to the node whose layer draws with it.
- It exists only while a scene is composed, so `sceneAlphas()` gives one entry per slot and none before the open. Tests 2 and 4 and the R-1 test use exactly that.
- On the root, BD-15's exact key set would need an assertion change.
- The production cost is one extra semantics node under a `clearAndSetSemantics` root. Its lambda identity changes only when the gate flips. Accessibility's replaced-children traversal does not reach it, so BD-15's TalkBack half on the device (QA) is the real check.
- "Beside the three existing ones" in BD-23 (a) reads to me as "another test-only hook on the backdrop", which this is.

**D5, R-1 settling structurally on the third frame: acceptable.**
- §16 #28's "within two frames" is about a running animation taking the new scale, and it does: frame 2 is pixel-identical to the settled catalogue.
- The slot clean-up a frame later is the effect's tail after `animateTo` returns. It draws a fully covered landscape slot once and strands nothing.
- If the tech-lead wants "settled" to be structural, I suggest pinning the actual bound in the test, sceneAlphas empty on the third frame, rather than reverting R-1. The current structural assertion runs a whole cross-fade later (`BackdropReducedMotionTest.kt:135-144`), which is looser than the behaviour (INFO 2).

## Mutants and probes (throwaway `git worktree add --detach 0c45666` under the scratchpad, removed afterwards)
Each command was `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*'`, 56 tests: the branch's 54 plus my 2 probe tests. After each run, `git checkout -- app` and `git diff --quiet -- app` reported "restored".

| Id | Mutation | Result |
|---|---|---|
| base | none | pass, 56 of 56 |
| MA | the open on frame 2 with animations on (the pre-J-4b `withFrameNanos { }`) | **red**, 6: `theSceneStaysOffThroughHomesEntranceCascade`, `atAnimatorScaleTwoTheSceneStaysOffForTwiceTheOpenDelay`, `theSceneOpensAtTheCascadesEndAndFadesInOverItsToken`, `BackdropFirstFrameTest.homesFirstFrameDrawsOnlyTheSky` and both probes |
| MB | the fade removed (reader always 1) | **red**, 1: `theSceneOpensAtTheCascadesEndAndFadesInOverItsToken` |
| MC | an unscaled frame-time delay of 440 ms at every scale | **red**, 3: `atAnimatorScaleTwoTheSceneStaysOffForTwiceTheOpenDelay`, `withAnimationsOffTheSceneOpensOnTheSecondFrameWithNoFade` and probe scale2 |
| MC2 | an unscaled delay, but frame 2 at scale 0 (isolates the scale-2 guard) | **red**, 2: `atAnimatorScaleTwoTheSceneStaysOffForTwiceTheOpenDelay` and probe scale2 |
| MD | R-1 undone (`LaunchedEffect(density, animationsEnabled)`) | **green**, 56 of 56. As the developer reported, the guard is behavioural and the old key was benign. The no-gate-key rule is held by review (§16 #28) |
| ME | A3 broken (`Animatable(SCENE_HIDDEN)` always) | **green**, 56 of 56, and green on the full app suite (1,640 tests, 0 failed). This is **F1** |
| MK | `HOME_LAST_ENTRANCE_ORDER = 4` (an open at 400 ms) | **red**, 4: both `…OpenDelayCoversHomesLongestEntranceCascade` tests and both probes |

**Probe `AuditorHostCascadeProbeTest`** (scratchpad only, never committed).
- Setup: the real `HomeScreen` with both chips, inside `AppNavHostWithStandIns`, at `w412dp-h915dp`.
- It records the Challenge tile's `boundsInRoot().top` on each frame after the gate's first frame, then checks that it settles no later than the open and that the scene is composed after that.
- Result on the unmutated code: pass. Scale 1 printed `open=28 firstSettledFrameAfterGateStart=28 firstSceneFrameAfterGateStart=29`, and scale 2 printed `open=55 … 55 … 56`.
- The margin is zero frames, by construction: both are 440 ms tweens started on the same frame. A Home whose cascade started a frame late would end after the open. B-11b accepts that residual for late-arriving chips.

## Checks
- `uptime` before each Gradle run: the 1-minute load was 2.81, 4.28, 3.65, 2.80 and 4.93, and each mutant run logged its own load (4.70–6.41). All were below 12. No emulator was used, and `./gradlew --stop` was never run.
- `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun` at `0c45666` (`app/` = `eee370e`) — **pass**: "BUILD SUCCESSFUL in 11m 7s", "107 actionable tasks: 12 executed, 95 up-to-date", exit=0.
  - The result XMLs, stamped 08:00–08:02Z (this run), hold **1,640 tests in 194 classes, 0 failures, 0 errors, 0 skipped**:
    - `BackdropSceneOpenTest`: 7 of 7;
    - `BackdropReducedMotionTest`: 4 of 4;
    - `BackdropFirstFrameTest`: 1 of 1;
    - `BackdropSettleTest`: 6 of 6.
  - `:app:lint` executed. `:domain:depthSweep` and `:domain:koverVerify` executed.
- `./gradlew --max-workers=4 --console=plain :app:runKtlintCheckOverMainSourceSet --rerun :app:runKtlintCheckOverTestSourceSet --rerun :app:detekt --rerun :app:ktlintCheck` — **pass**: "BUILD SUCCESSFUL in 7s". All three forced tasks executed. Inside the Check they had been up-to-date.
- `git diff d029950..eee370e -- app | grep -nE '^\+.*(@Suppress|@Ignore|baseline|!!|Thread\.sleep|System\.currentTimeMillis|Random\()'` — pass. Its one hit is KDoc ("The 6.7-inch baseline window").
- `git diff --name-only d029950..eee370e -- app/src/main` — pass (three `ui/backdrop/` files). `git diff --quiet d029950 eee370e -- app/src/main/java/com/app/pegsolitaire/navigation/` gives exit 0, and `AppNavHost.kt:170` is unchanged.
- `git diff-tree --cc eee370e` — one combined hunk, `docs/ux-spec.md:7`, a correct union (above).
- The mutation batch and the probe — as tabled above. ME on the full suite: `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest` in a second throwaway worktree gave "BUILD SUCCESSFUL in 1m 19s", with the XMLs stamped 08:15Z (1,640 tests, 0 failed).
- `git worktree list` shows neither throwaway worktree. The branch's `git status --porcelain` was empty before this report.
- Not read: `.planning/log/`, and the tech-lead's `d9630a0` D-rulings.

## Open issues / risks
1. **F1** (above) must be fixed before PASS. The re-check is the new test, red on ME and green on the head.
2. **INFO: the R-1 guard checks the cleanup loosely.** It asserts "nothing stranded" a whole cross-fade after the switch. It catches M6 (a strand), but a clean-up delayed by several frames would also pass. See D5.
3. **INFO: no test catches an R-1 undo (MD).** That is consistent with §16 #28 ("held by review"), and the developer disclosed it. A future gate-keyed effect in `ui/backdrop/` needs a reviewer's eye, or the 3.0.1 #33 work could add a static check.
4. **INFO: the open and Home's cascade end on the same frame** (my probe). The ordering rests on the scene composing one frame after the flag flips. A Home that starts its cascade later than the host's frame 2 would overlap. B-11b accepts this for late chips.
5. **INFO: duplication.** `Bitmap.differingPixels` is a private copy in `BackdropSceneOpenTest.kt:277` and `BackdropReducedMotionTest.kt:173`. It could move into `testing/` beside `renderedWindow()`.
6. **Trailer.** The plugin's 1.0.0 `rules/gitflow.md` asks for the single Fable trailer. The product's 0.6.2 snapshot, which CLAUDE.md imports, does not. Like the branch's other recent commits, this report's commit carries the harness's attribution lines. The producer still has to settle this.

## Handoff
Next: android-game-dev (through the producer). Fix F1 with one test that pins A3's first branch, as described in Findings. It must be red on `Animatable(SCENE_HIDDEN)` and green on the head. Production code is unchanged. Optionally, tighten the R-1 guard's structural bound if the tech-lead's D5 ruling asks for it.
Next: code-auditor. Re-check F1 only: the new test, its red on ME, and the Check.
Next: tech-lead. Note my D1 and D5 views (above). They agree with "accept as built".

VERDICT: FAIL
