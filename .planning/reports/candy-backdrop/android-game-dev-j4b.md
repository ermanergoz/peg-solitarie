# android-game-dev report — candy-backdrop: J-4b, open the scene after Home's entrance
Date: 2026-11-01
Branch: feature/candy-backdrop (8fad1b0..eee370e)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus `.claude/rules/lessons.md`), superpowers:test-driven-development, compose-skill, compose-ui, android-testing, gitflow
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- J-4b is built test-first in `ui/backdrop/` only. The gate now waits `BACKDROP_SCENE_OPEN_MS` (440 ms at scale 1) of animator-scaled time on a throwaway tween. The scene's layer then fades 0 → 1 over `BACKDROP_SCENE_FADE_MS` (700 ms), linear, with `ModulateAlpha`, once per host start.
- With animations off, nothing changes from before: the gate flips on frame 2, and the scene is drawn whole on frame 3.
- The five J-4a tests went red first. Tests 1 and 5 also run at animator scale 2. Both ruled test amendments are made. R-1 is in its own commit with its guard test.
- Develop `452f187` is merged in. It brought no `app/` change, and its one conflict was in a docs front-matter line.
- Check green on `eee370e` (`./scripts/check.sh --wait --max-workers=4`, 11 min 7 s). BD-01 commands 1–3 print nothing. §3's two 15e conditions hold.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/backdrop/BackdropMotion.kt` — `HOME_LAST_ENTRANCE_ORDER` (5, with KDoc citing Home's orders), `BACKDROP_SCENE_OPEN_MS` (derived, never 440) and `BACKDROP_SCENE_FADE_MS` (defined as `ClayAnim.NAV_TRANSITION_MS`).
- `app/src/main/java/com/app/pegsolitaire/ui/backdrop/BackdropSceneReady.kt` — the open. Inside the existing `LaunchedEffect(Unit)`, a throwaway `Animatable` runs `tween(BACKDROP_SCENE_OPEN_MS, LinearEasing)`, then sets the flag. There is no `withFrameNanos` first, so the tween's first frame is the gate's first awaited frame, and at scale 0 it ends on that frame (A1).
- `app/src/main/java/com/app/pegsolitaire/ui/backdrop/SceneBackdrop.kt`:
  - `rememberSceneAlpha`: the fade `Animatable` starts at 1 if the flag is already true at first composition, otherwise at 0 (A3).
  - `LaunchedEffect(ready)`: it reads the reduced-motion gate once at its start, then animates or snaps.
  - The reader returns 1 while animations are off.
  - The scene node's `graphicsLayer { alpha = alpha(); compositingStrategy = ModulateAlpha }` (A4).
  - The test-only `BackdropSceneAlphaKey`, whose value is that same reader.
  - R-1: `LaunchedEffect(density)`.
- `app/src/test/java/com/app/pegsolitaire/ui/backdrop/BackdropSceneOpenTest.kt` — the five red-first tests, as 7 methods (test 1 and test 5 at scale 1 and 2).
- `app/src/test/java/com/app/pegsolitaire/ui/backdrop/BackdropFirstFrameTest.kt` and `BackdropSettleTest.kt` — the two ruled amendments (J-4b §4).
- `app/src/test/java/com/app/pegsolitaire/ui/backdrop/BackdropReducedMotionTest.kt` — R-1's guard test, added. No existing line of the file changed.
- `app/src/test/java/com/app/pegsolitaire/testing/BackdropTestHost.kt` — three helpers: `advanceOneFrame()`, `framesToSceneOpen(frameMs, scale)` and `sceneAlphas()`.
- `app/src/test/java/com/app/pegsolitaire/testing/SystemMotionScale.kt` — `slowAnimationsBy(factor)`, for the scale-2 runs.
- This report. Scratch, git-ignored and never committed, under `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/7be27af9-9fc5-49d9-b194-5a5598b4253b/scratchpad/j4b/`: the run logs (`red0.log`, `red1.log`, `red2.log`, `green1.log`–`green4.log`, `mutant-*.log`, `r1-*.log`, `check-full.log`), `mutants.py`, and the reverted probe's output `probe-out.txt`.

Commits: `8fad1b0` (feature, tests and amendments), `635c9b8` (R-1), `eee370e` (the develop merge).

## Red first (each red quoted)
Each red is from `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropSceneOpenTest' --console=plain --max-workers=4`.
- **Red 0, all five tests, before any production line** (`red0.log`): `e: …/BackdropSceneOpenTest.kt:68:39 Unresolved reference 'BACKDROP_SCENE_OPEN_MS'.`, `…:71:55 Unresolved reference 'BACKDROP_SCENE_FADE_MS'.` and `…/BackdropTestHost.kt:31:41 Unresolved reference 'BackdropSceneAlphaKey'.`
- **Red 1, the API declared but nothing using it** (the tokens, and the key declared and never set), so the gate behaves as at `cf0c5a4` (`red1.log`, `red2.log`):
  - Test 1, `theSceneStaysOffThroughHomesEntranceCascade`: `layers 16 ms after the gate's first frame, at animator scale 1.0 / unexpected (2): Waves, Lights / expected: [Sky] / but was: [Sky, Waves, Lights]`. At scale 2 the message is the same, with `at animator scale 2.0`.
  - Test 2, `theSceneOpensAtTheCascadesEndAndFadesInOverItsToken`: `the open, in ms after the gate's first frame / expected to be at least: 440 / but was: 16`.
  - Test 3, `withAnimationsOffTheSceneOpensOnTheSecondFrameWithNoFade`: `the scene's alpha on frame 3 / missing (1): 1.0 / expected: [1.0] / but was: []`. Its first version was red on `layers on frame 2 … but was: [Sky]`. See decision D3.
  - Test 4, `aNavigationAfterTheOpenFadesTheSkyButNotTheSceneAgain`: `the scene's alpha on Home after the fade / missing (1): 1.0 / expected: [1.0] / but was: []`.
  - Test 5, `theOpenDelayCoversHomesLongestEntranceCascade`: red 0, then green as soon as the tokens existed, because it tests the token against Home. To watch it bite, I ran a mutant with `HOME_LAST_ENTRANCE_ORDER = 4` (an open at 400 ms), then reverted it: `pixels that change after the open's frame, at animator scale 1.0 / expected: 0 / but was: 35871`, and the same at scale 2.0.
- **Mutants after green.** Each was applied by `mutants.py` (or inline for M6 and M7), and the sources were restored with `shasum -a 256 -c` OK each time:
  - **M5, A1 broken (the open counts unscaled frame time):** `atAnimatorScaleTwo…StaysOff…` is red with `layers 464 ms after the gate's first frame, at animator scale 2.0 / unexpected (2): Waves, Lights`, and test 3 is red with `layers on frame 3 / missing (2): Waves, Lights`.
  - **M3, A3 broken (no reader for animations off; the fade always animates):** test 3 is red with `the scene's alpha on frame 3 … expected: 1.0 / but was: 0.0`.
  - **M1, once-per-host-start broken (every navigation fades the scene again):** test 4 is red with `Home to the catalogue, frame 0: scene alphas other than 1 / expected to be empty / but was: [0.0]`.
  - **M6, the §16 #28 strand (the cross-fade effect keyed on the gate, with no promotion):** the R-1 test is red with `scenes still composed a cross-fade's length later / expected to be empty / but was: [1.0]`.
  - **M7, a cross-fade that ignores the new scale:** the R-1 test is red with `pixels two frames after the switch that differ from the settled catalogue / expected: 0 / but was: 131120`.

## A6: where the open lands, and steady state
The numbers come from the tests' own bounds and from a temporary probe in J-4a's form, run once and then reverted. The probe was counters in `SceneBackdrop.kt` plus `J4bProbeTest`, at `w411dp-h923dp-420dpi`, NATIVE, light theme. Its output is `probe-out.txt`, and the probe was removed: `shasum -c` OK, and `git diff --quiet HEAD -- app` exits 0.

All frame numbers below are on the test clock, whose frames are 16 ms. Frame 1 is the host's first frame. Frame 2 is the gate's first awaited frame, and it is t = 0 of the open's clock.

**Test 1 at scale 1.**
- The gate's tween ends on **frame 30**: 448 ms of frame-clock time after frame 2, which is 464 ms after frame 1. That is the first frame at or past 440 ms.
- The scene is composed on **frame 31**, at alpha 0. The cache builds there, once.
- On every frame through frame 29 (432 ms), the test pins {Sky}, AtRest and 0 clock frames awaited.

**Test 1 at scale 2.**
- The tween ends on **frame 57**, 880 ms after frame 2.
- The test pins the closed state through frame 56 (864 ms) and the scene present by frame 58.

**Test 5.**
- The window is captured on the open's frame: **frame 30 (448 ms)** at scale 1, and **frame 57 (880 ms)** at scale 2.
- Home's whole window is pixel-identical from that frame on. Against the next frame and against a further 440 ms (or 880 ms) later, the count of changed pixels is 0. The Challenge tile, the last band at order 5, is wholly in the window.

**The first visible draw follows the cascade's end.**
- Home's last band finishes its tween on frame 30, the same frame on which the gate's tween ends, because both run on the same clock and scale from frame 2.
- The scene's first draw is on frame 31, at alpha 0. HWUI skips a draw at alpha 0 on a device.
- The first draw at alpha above 0 is **frame 33**, 496 ms after frame 2, at alpha 0.0229 (that is, 16/700).
- The fade completes on frame 76, 720 ms after the scene's first composition.

**Steady state is unchanged.**
- The cache built **once**, at the scene's first composition. It was still 1 build at the fade's end, and still 1 after **242** more frames: the clock went from 704 to 4,576 ms, with 242 distinct clock times.
- Draws ran twice per frame, the same 2:1 ratio as J-4a's run (482 draws in 240 frames).
- The new layer block ran 46 times, once per fade frame, and **0 times in the 242 steady frames**. After the fade, a frame re-runs only the draw, as before J-4b.
- `git diff d029950..HEAD -- …/SceneBackdrop.kt` changes no line inside `onDrawBehind`, only the modifier chain around it. The draw block therefore still allocates nothing (J-4a's code review).
- The layer block assigns a `Float` read from the remembered reader, and it does not run in steady state.

## The ruled test amendments (J-4b §4), and every other edited test
- **`BackdropFirstFrameTest`.**
  - The frame-1 half is unchanged, word for word.
  - The second half now asserts {Sky}, AtRest and 0 awaited on each of `framesToSceneOpen(frameMs)` frames from frame 2. `frameMs` is measured on the test clock with `advanceOneFrame()`, and the count is computed from `BACKDROP_SCENE_OPEN_MS`.
  - Then come the unchanged `repeat(FRAMES_TO_SCENE)` and the unchanged `containsExactly(Sky, Waves, Lights)`. The message gains "of the open".
- **`BackdropSettleTest.start()`.**
  - The three `advanceTimeByFrame()` calls become `advanceOneFrame()` plus `framesToSceneOpen(frameMs) + FRAMES_PAST_OPEN` (2). Before, start() ran two frames past the flip; now it runs two frames past the open.
  - No assertion and no other line in the file changed. Of the six tests, the two red before the edit were `theBackdropSettlesAfterTheIdleDelayAndThenAwaitsNoFrame` and `batterySaverRestsTheBackdrop` (`running on Home / expected: Running / but was: AtRest`).
- **Every other existing test passed unedited**, `SkyContinuityTest` and `BackdropReducedMotionTest`'s three existing tests included. So no setup-only edit beyond §4 was needed.
- `BackdropReducedMotionTest` gains one new test (R-1) and imports. No existing line changed.

## Decisions for review
- **D1 — Where the alpha hook lives.** It is on the scene node, under the root's `clearAndSetSemantics`, and not on the root.
  - Tests find it with `useUnmergedTree = true`: the unmerged tree includes replaced children (`SemanticsNode.getChildren(includeReplacedSemantics = !mergingEnabled)`, ui-android 1.11.3). Accessibility's `replacedChildren` excludes it, so TalkBack sees nothing new.
  - Why not the root: on the root, `BackdropSemanticsTest`'s exact key set (BD-15) would have to gain the key. That is an assertion change, which J-4b §4 reserves to the tech-lead.
  - Against this choice: §12's "one semantics node on the backdrop's root", and BD-23 (a)'s "beside the three existing ones", if "beside" means "on the root".
  - If the tech-lead wants it on the root, it is a two-line move, plus the key added to `BackdropSemanticsTest`'s set.
- **D2 — The key's value is a reader, `() -> Float`.** It is the very lambda the layer calls ("the test reads the source", A3). The backdrop does not recompose for the fade, which is the `BackdropClockProbe` idiom and `rememberChipRoom`'s `fraction` idiom.
- **D3 — Test 3 pins frame 3 for the scene, which is how it was before B-11b.** Red 1 on the gate as at `cf0c5a4` showed the flag flips on frame 2 and the scene is composed on frame 3. That is the J-4a audit's naming: ok0 is frame 2, where the gate flips, and ok1 is frame 3, where the scene draws. "Opens on frame 2" is therefore pinned as {Sky} after one frame and the whole scene, at alpha 1, after two.
- **D4 — I corrected test 4 while it was still uncommitted.** Its first version wanted a scene alpha on every frame of the return to Home.
  - On the return's first frame, the root's layers already name the target density, while the incoming slot is composed one frame later. That is R1's existing behaviour.
  - It now asserts that every scene node present reads 1, and that one is present within two frames. M1 shows it still catches a second fade.
- **D5 — What R-1's "settled within two frames" measures.**
  - Under `LaunchedEffect(density)`, the running `animateTo` reads `durationScale` before it awaits each frame. So the switch takes effect on the 2nd frame: the incoming wash is at alpha 1 and opaque, and the window equals the settled catalogue. The slot clean-up recomposes on the **3rd** frame. With the old gate key it was the 2nd.
  - The guard test asserts the visible settle within two frames (pixels) and nothing stranded a cross-fade later (structure).
  - If "settled" is meant structurally, R-1 fails it by one frame. R-1 is then `635c9b8` alone, to revert into 3.0.1 under the ruling's rider clause.
- **D6 — Effects started on frame 1.** `LaunchedEffect(ready)` starts on frame 1 and returns at once. Remembering the fade `Animatable` on frame 1 is allowed under §3 condition 2. Neither reads, registers, decodes or loads anything.

## 15e (J-4b §3): not owed
- **Condition 1.** `git diff --name-only d029950..HEAD -- app/src/main` lists only `ui/backdrop/BackdropMotion.kt`, `BackdropSceneReady.kt` and `SceneBackdrop.kt`. `git diff --quiet d029950 HEAD -- app/src/main/java/com/app/pegsolitaire/navigation/` exits 0, and `AppNavHost.kt:170` is still `val sceneReady = rememberBackdropSceneReady()`.
- **Condition 2.** Frame 1 gains one remembered `Animatable`, one remembered reader lambda and one `LaunchedEffect(ready)` that returns at once. `rememberAnimationsEnabled()` was already read on frame 1.
  - There is no system read, registration, decode, asset or bitmap.
  - The battery-saver read and the clock remain behind the flag: the probe shows the clock's first awaited frame on frame 31.
  - `grep -rn 'import com.app.pegsolitaire.ui.home' …/ui/backdrop/` prints nothing (A2).

## Merge of develop
- `git diff --name-only d251c3b develop` outside `.planning/` and `docs/` lists only `AGENTS.md` and `CLAUDE.md`. There is no `app/`, `build-logic/`, Gradle or settings change.
- `git merge --no-ff develop` produced `eee370e`. Its one conflict was `docs/ux-spec.md`'s front-matter `revised:` line, where both sides had added a record to `2026-10-24`. I resolved it as the union in date order: `revised: 2026-10-24; 2026-10-31 (15m pointer, B-19a); 2026-11-01 (pre-release folds: …)`.
- The code-auditor re-reads that hunk. I amended the merge commit's message only (`tree unchanged` checked), so that the trailers end it.

## Checks
- `uptime` before every Gradle call: 3.0 to 5.9, always below 12. No emulator ran, and `./gradlew --stop` was never used.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropSceneOpenTest' --console=plain --max-workers=4`:
  - red 0: fail (compile, quoted above);
  - red 1: fail (5 of 7);
  - red 2 with the order-4 mutant: fail (7 of 7);
  - after the gate: fail (3 of 7, alpha only);
  - after the fade: pass (7 of 7).
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.*' --console=plain --max-workers=4`:
  - after the fade, before the §4 amendments: fail (53 tests, 4 failed). The failures were `BackdropFirstFrameTest`'s one test, the two `BackdropSettleTest` tests named above, and test 4 before D4.
  - after the amendments and D4: pass (53 of 53).
  - after R-1, with the guard test's first, structural form: fail (54 tests, 1 failed: `scenes composed two frames after the switch … but was: [1.0]`; see D5).
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.BackdropReducedMotionTest' --console=plain --max-workers=4`, with the guard test in its final form: pass (4 of 4) on R-1. It also passes (4 of 4) on `8fad1b0`'s gate-keyed `SceneBackdrop.kt`, put back temporarily and then restored.
- Mutants M5, M3, M1 (`mutants.py`), M6 and M7: each red where stated above. Each was restored, with `shasum -a 256 -c` OK.
- `./gradlew :app:ktlintCheck :app:detekt --continue --console=plain --max-workers=4`: pass, after wrapping 5 test lines that were over 120 characters.
- `./scripts/check.sh --wait --max-workers=4` on `eee370e`: pass (`BUILD SUCCESSFUL in 11m 7s`, exit 0; 107 actionable tasks, 21 executed). In detail:
  - `:app:testDebugUnitTest` executed: 1,640 tests in 194 classes, 0 failures, 0 errors, 0 skipped, run 10:47:52–10:49:33 (JUnit XML).
  - `:app:lint` executed and passed.
  - `:app:detekt` and `:app:ktlintCheck` were up-to-date on inputs that had passed.
  - `:domain:test` was up-to-date (294 tests, 0 failures; `:domain` is unchanged by this work), and `:domain:koverVerify` passed.
- BD-01 command 1 (`grep -rniwE --exclude=README.md 'candy|…|microsoft' app/src/main/res/values*/strings.xml app/src/main/assets docs/store`): pass, no output (exit 1).
- BD-01 command 2 (`grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store`): pass, no output (exit 1).
- BD-01 command 3 (`git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'`): pass, no output (exit 1).
- `git diff d029950..HEAD -- app | grep -nE '^\+.*(@Suppress|@Ignore|baseline|!!)'`: pass. Its one hit is the word "baseline" in a test comment ("The 6.7-inch baseline window"). No suppression, baseline, `@Ignore` or `!!` was added.
- `grep -nE 'delay\(|System\.(nanoTime|currentTimeMillis)|SystemClock' app/src/main/java/com/app/pegsolitaire/ui/backdrop/*.kt`: pass, no output.
- `git diff --quiet d251c3b 452f187 -- . ':(exclude).planning' ':(exclude)docs' ':(exclude)CLAUDE.md' ':(exclude)AGENTS.md'`: pass (exit 0). Arm A's code equals J-2's arm A.
- Not read: `.planning/log/`.

## Open issues / risks
1. **D1, the alpha hook's location**, is the tech-lead's call (root or scene node). It changes no behaviour.
2. **D5, R-1's structural settle on the 3rd frame**: visible on the 2nd. If that is not acceptable, revert `635c9b8` and file R-1 for 3.0.1.
3. **Observed, not touched.** `BackdropClockDriver`'s `LaunchedEffect(live, runs, lifecycle, frames)` (`BackdropClock.kt:123`) is keyed on `runs = animationsEnabled && !powerSave`, a value derived from the gate. It is pre-J-4b and outside R-1's scope.
   - Its restart on a flip is the intended way it stops the clock, and no decorative animation is stranded.
   - It is listed so the §16 #28 grep's known blind spot ("a gate-derived local escapes it") has it on record.
4. **J-5 can still FAIL on Home's finishes** (J-4a Open issue 1; J-4c is the plan). The moved draw lands about 0.5 s after frame 2 on the bench and is J-5's RT_late.
5. **Trailer.** These commits carry the harness's attribution lines (the true model), as `24fb33c` and `cf0c5a4` do. The plugin's 1.0.0 rules/gitflow.md asks for the single Fable trailer instead. This is the same open question the branch's other reports raise, and it is the producer's to settle.

## Handoff
Next: code-auditor — review `d029950..eee370e` for code (three `ui/backdrop/` main files and seven test files). Points to check:
- §3's two conditions;
- §4's amendments, with only setup lines changed;
- D1 to D6;
- the merge commit's `docs/ux-spec.md` hunk.

Next: qa-engineer — run J-5 as amended in J-4b §5 (`sleep 2`, RT_late, the stencil split). Arm B builds this branch's head after this report's commit; its `app/` tree equals `eee370e`'s. Arm A builds `452f187` (`git merge-base HEAD develop`), whose code equals `d251c3b`. Also run BD-23 (d) on the device: light and dark, at least 5 cold starts each.

Next: tech-lead — rule on D1 and D5; sign-off addendum after J-5.
