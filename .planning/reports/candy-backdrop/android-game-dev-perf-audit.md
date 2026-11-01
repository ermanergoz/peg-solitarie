# android-game-dev report — candy-backdrop: J-4a performance audit
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; HEAD `52ae3c4`, whose code equals `d029950`; no product edit, temporary probes reverted)
Skills loaded: compose-performance-audit, ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, testing, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android and compose in full, plus `.claude/rules/lessons.md`), compose-skill, compose-ui
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- J-2's settle excess (S, B − A = +228 ms on kept samples) is **all RenderThread draw-issue time in frames 1–3** (+222 ms). Frame 3's "main-thread" delay is the UI thread blocked in frame 2's sync behind frame 1's draw. The cache build costs 0.2 ms and runs once.
- Frame 1 is the first draw of the scene **and** of Home's hero, stage and ribbon, whose entrance alpha is 0 until then. 15e (animations off, so all of B's Home draws on frame 1 while the scene waits) puts B's non-scene additions at ≤ 45 ms. By subtraction, **the scene owns ≈ 120–180 ms** of the excess.
- Only B allocates 3 × 2.50 MiB stencil attachments, two more scratch render targets (+8.4 MB) and more buffer objects. B's only non-convex paths are the scene's two lips (1617 × 282 px) and Home's five finish lips.
- **Proposal: open the scene's gate when Home's entrance cascade ends (440 ms on the Compose frame clock), and fade the scene in once per host start.** All in `ui/backdrop/`. Expected J-5 saving ≈ **150 ms (120–180)**. It **moves** the scene's first-draw cost onto the fade's first visible frame of a settled Home. What the player sees changes, so the ux-designer reviews it before merge.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/android-game-dev-perf-audit.md` — this report.
- Scratch, git-ignored, never committed, under `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/7be27af9-9fc5-49d9-b194-5a5598b4253b/scratchpad/j4a/`:
  - the analysis scripts `frames.py`, `gpucache.py`, `coldwarm.py`, `frametime.py`;
  - the reverted probes `probes.diff` and `J4aProbeTest.kt.txt`;
  - their output `probe-out.txt`;
  - the Gradle logs `gradle-run1.log` and `gradle-run2.log`.

## Checks
- `python3 -I <scratch>/j2/phase.py .planning/qa/candy-backdrop/jank2` and `… settle.py …` — pass (they reproduce the tech-lead's §1.2 and §1.3).
- `python3 -I <scratch>/j4a/frames.py .planning/qa/candy-backdrop/jank2` — pass (table 1).
- `python3 -I <scratch>/j4a/gpucache.py …`, `coldwarm.py …`, `frametime.py …` — pass (table 2 and §1 findings 5–6).
- `uptime` before each Gradle call: 3.02, then 10.09 (Spotlight's `mds_stores`). Both were < 12. No emulator ran.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.backdrop.J4a*' --console=plain`, twice, with the temporary probes — pass (BUILD SUCCESSFUL in 27 s, then 17 s; output in §2).
- Revert: `git checkout -- <SceneBackdrop.kt> <Finish.kt>`, then `rm J4aProbeTest.kt`; `shasum -a 256 -c pre-probe.sha` — pass (both OK); `git status --porcelain --untracked-files=all` — pass (empty); `git diff --quiet HEAD -- app` — pass (exit 0).
- Not run: `./gradlew check`. The tree is byte-identical to HEAD, and nothing in `app/` changed. `.planning/log/` was not read.

## 1. Where the time goes: A against B, from J-2's dumps
**Method.** The same as J-5:
- the MainActivity window's `PROFILEDATA`, columns read by name;
- rows ordered by `IntendedVsync`, keeping `Flags` = 0 and `FrameCompleted > IntendedVsync`;
- kept samples only: A 24, B 24, N 36 (B's APK).

**F\*** is the window's first frame (`Flags` ≠ 0, before ok0). The §1.2 table does not show it.

Frame names map as follows:
- F\* is Home's **frame 1** in the spec's terms, the one 15e and TTID measure.
- ok0, which §1.2 calls "frame 0", is the spec's frame 2: the gate flips there.
- ok1 is frame 3, where the scene first draws. That is within BD-23 (a)'s "two frames".

Medians are in ms. RT is split into **sync** (`SyncStart → IssueDrawCommandsStart`) and **issue** (`IssueDrawCommandsStart → SwapBuffers`).

**Table 1.**

| Frame | Start delay A / B | Record A / B | Sync wait (`SyncQueued → SyncStart`) A / B | RT sync A / B | RT issue A / B | RT B − A |
|---|---|---|---|---|---|---|
| F* | 23.7 / 19.6 | 31.2 / 37.4 | 0.5 / 0.6 | 0.3 / 0.3 | 32.0 / 38.8 | +6.6 |
| ok0 | 588.6 / 597.8 | 13.3 / 5.5 | 0.2 / 0.2 | 0.1 / 0.1 | 1.0 / 0.6 | −0.3 |
| ok1 | 43.6 / 38.1 | 3.6 / 9.3 | 0.2 / 0.1 | 0.4 / 0.1 | 126.9 / **270.1** | **+143.2** |
| ok2 | 46.0 / 70.3 | 15.5 / 15.4 | 28.3 / **173.2** | 0.1 / 0.3 | 15.2 / 51.2 | +36.7 |
| ok3 | 142.7 / **297.7** | 22.6 / 19.3 | 0.1 / 0.2 | 0.2 / 0.1 | 3.3 / 33.3 | +29.2 |
| ok4 | 76.2 / 70.8 | 16.6 / 47.1 | 0.1 / 0.2 | 0.1 / 0.1 | 2.4 / 5.1 | +3.1 |
| ok5 | 66.0 / 81.8 | 38.0 / 16.1 | 0.2 / 0.1 | 0.2 / 0.1 | 9.1 / 7.2 | −1.8 |
| ok6–7 | 43 / 18; 36 / 22 | 30 / 19; 18 / 18 | ≈ 0.2 | ≈ 0.1 | 7.4 / 7.5; 3.7 / 4.6 | +0.4; +1.0 |

N matches B: ok1 RT issue 291.9, ok2 sync wait 187.4, ok3 start delay 303.9.

**What the table shows:**
1. **The excess is draw issue, not upload.** RT sync is ≤ 0.4 ms on every frame in every arm. No texture or bitmap upload sits on this path. The time goes into issuing GL work: program creation, path rendering and GPU resource allocation.
2. **Frame 3's start delay is a knock-on.** When ok2 queued its sync, the RT was still issuing ok1: it stayed busy 169 ms past ok2's `SyncQueued` (A: 24). So the UI thread waited 173 ms in sync (A: 28). After ok2's sync, the UI thread was free for 70.7 ms before ok3 began (A: 47.0). Of ok3's +155 ms start delay, about 145 ms is that wait and about 24 ms is idle gap. The tech-lead's "+161 ms main thread" is frame 1's RT cost propagating, not main-thread work.
3. **The sums close.** RT(ok1 + ok2 + ok3) is B 368.6 against A 146.5, so +222. S is B 578.4 against A 350.8, so +228. The whole S excess is RT issue time in frames 1–3.
4. **ok4's record excess (+30 ms) is not the scene.** It is the same event as A's 38 ms on ok5: the progress values and chips arriving. It lands one frame index earlier in B because B's early frames are slower. Under J-5 it cancels once C's early frames run like A's.
5. **Frame 1 is everyone's first content draw.** The frame-clock times after ok0 (`FrameStartTime`, which animations read) are:

   | Frame | A | B |
   |---|---|---|
   | ok1 | +54 | +44 |
   | ok2 | +113 | +114 |
   | ok3 | +252 | +437 |

   `clayEntrance` starts each band at alpha 0. The hero, stage, ribbon and gear (order 0 and 1) cross 0 on ok1. The Classic tile (order 2, delay 80 ms) crosses on ok2, and the Challenge tile (order 3, delay 120 ms) on ok3. A's own RT agrees: ok0 is 1.0 ms and ok1 is 126.9 ms. So frame 1 carries the scene's first draw **and** the first draw of B's finished bands.
6. **The shader cache does not split compile from allocation.** Each block starts with `install -r`, which clears `code_cache` and with it HWUI's persistent shader cache. Comparing RT1 on that cold first launch against warm launches 2–5 gives A 129.5 / 127.0, B 330.1 / 263.3 and N 297.0 / 288.4. Inconclusive.

**Table 2. GPU caches at dump time** (every non-warm-up sample; medians).

| | A (30) | B (30) | N (45) |
|---|---|---|---|
| Other › StencilAttachment | **absent in 30/30** | **7.50 MB, 3 entries (30/30)** | 7.50 MB, 3 entries (45/45) |
| Scratch › RenderTarget | 7.03 MB, 7 entries | 15.47 MB, 9 entries | 15.47 MB, 9 entries |
| Scratch › Buffer Object | 0.14 MB, 4 entries | 0.27 MB, 8 entries | 0.27 MB, 8 entries |
| Other › Buffer Object | 2 entries | 4 entries | 4 entries |
| Total GPU memory | 9.23 MB | 25.35 MB | 25.35 MB |

B needs stencil-backed GPU work that A never does. There are three equal stencil attachments of 2.50 MiB each, one per buffer or frame in flight. They fit ok2's and ok3's extra RT time (+37, +29) as one allocation per new buffer. That excess also fits the gel tiles' first draws on those frames. The split cannot be made off-device.

## 2. JVM measurements (temporary probes, reverted)
**Setup.**
- Robolectric `GraphicsMode.NATIVE`, sdk 34, qualifier `w411dp-h923dp-420dpi`, which gives 1078 × 2422 px (1080 × 2424 rounded by the qualifier).
- Counters were added to `SceneBackdrop.kt`'s cache and draw blocks and to `Finish.kt`'s lip build. They are now removed (Checks).

**Results.**
- **Cache build.** The parts are `buildSceneModel`, the `WaveDrawing.of` PathOps and `LightDrawing.of`. Warm medians over 20 builds:

  | Density | Model | Waves (PathOp) | Lights | Total | Max |
  |---|---|---|---|---|---|
  | Landscape | 0.02 ms | 0.16 ms | 0.01 ms | 0.19–0.20 ms | 1.19 ms |
  | Quiet | 0.02 ms | 0.18–0.19 ms | 0.01 ms | 0.20–0.21 ms | 1.17 ms |

  The cold first build in the JVM took 0.23 ms. The upper bound is about 1 ms, so the cache build is **not a cause**. ok1's +6 ms record also holds the scene's and the clock driver's first composition.
- **Built once per size, density, theme and direction, never per frame.** The live host (`AppNavHost` with stand-ins, `CountingFrames`) ran 240 frames: the clock went from 0 to 3,840 ms, with 241 distinct draw times and 482 draws, on **one** build.
  - Redraws with nothing changed: 0 rebuilds.
  - Dark theme: +1. RTL: +1.
  - With the gate closed on frame 1: 0 builds and 0 draws.
  - Code review agrees. Time is read only in draw, and `SceneBackdrop` recomposes only when the motion state changes. The draw block allocates nothing.
- **The first backdrop frame draws:**
  - 6 `drawPath`: fill, lip and sheen for each of 2 waves;
  - 9 `drawCircle`: all nine lights are visible at t = 0;
  - the sky's one `drawRect`, already present on frame 1.

  Brushes per build:

  | Count | Brush | Used for |
  |---|---|---|
  | 2 | linear gradient, 3 stops | wave fills |
  | 2 | elliptical radial gradient, 2 stops plus a local matrix | sheens |
  | 9 | radial gradient, 4 stops | lights |
  | 1 | solid colour | lips |

  That makes 3 new gradient structures. The sky's 5-stop linear gradient is a fourth, on frame 1.
- **Paths, all PathOp outputs.**

  | Path | Kind | Bounds (px) |
  |---|---|---|
  | Landscape wave shapes | convex | 1617 × 1246 and 1617 × 931 |
  | Lips | **non-convex** (≈ 115 points at 0.5 px) | 1617 × 282 |
  | Sheens | convex | 906 × 63 |
  | Quiet front wave shape | non-convex: its ellipse dips below the rectangle | 1617 × 543 |

- **Finish lips on Home** (B only, real sizes, animations off). Every one is a non-convex PathOp `Difference`:

  | Container | Lip bounds (px) |
  |---|---|
  | GearChip | 126 × 66 |
  | StageArt | 767 × 89 |
  | RibbonPill (×3) | 318 × 58 |
  | ContinueChip (when shown) | 994 × 79 |

- **CPU raster per frame** (software canvas, host): sky only 7.6 ms; sky plus scene 14.3–14.5 ms. The scene's share is about 6.8 ms.
- **Bitmap trial (candidate 4).** Each wave's rest pose was rasterised into a bitmap covering its whole travel.

  | Density | Bitmaps | Memory | Raster time |
  |---|---|---|---|
  | Landscape | 1204 × 1257 and 1184 × 942 | 10.5 MB | 7.7–8.0 ms |
  | Quiet | — | 5.8 MB | 4.4 ms |

  Composited at whole-pixel origins against the vector draw, the maximum channel difference is 16 (light) and 26 (dark). Only 338 and 616 pixels of 2.61 M differ by more than 2/255, all on anti-aliased edges.

## 3. Attribution and ranked causes
**Attribution through 15e.** 15e ran with OS animations at 0.0, so `clayEntrance` started at full alpha. Everything B adds to Home therefore drew on frame 1, while the scene still waited behind the gate:
- the finish lips (stage, pills, gear), gloss, rims and tinted glows;
- the gel tiles;
- the sky.

There, B − A TotalTime was **+44.5 ms** (floor 6.5). That bounds all of B's non-scene first draws at about 45 ms.

If the three stencils belong to the finishes rather than the scene, two more allocations land on the finishes in J-2 (about 30 ms each, as on ok2 and ok3). So the finishes' share f is ≤ about 105 ms, and **the scene's share s = 228 − f ≈ 120–180 ms**, about 150 central. One caveat: this assumes a finish costs the same drawn inside the entrance's alpha layers as drawn on the window.

**Ranked causes:**
1. **The scene's first GPU draw on frame 1: ≈ 120–180 ms of RT issue.** It draws two full-width non-convex lips, two convex bodies and two sheens, and nine radial lights, with three new gradient structures and the GPU resources only B allocates. On this bench it lands with the first draw of Home's bands.
2. **Knock-on: frame 2's sync blocked 145 ms behind frame 1.** This is the "+155 ms main thread" before frame 3, and it disappears with cause 1.
3. **Frames 2–3, RT +37 and +29.** These are the per-buffer stencil allocations, the gel tiles' first draws (orders 2 and 3), or both. The split is unknown off-device.
4. **Home's own new finishes: ≤ about 45 ms,** plus up to about 60 ms more if they own the stencils. This is outside `ui/backdrop/`.
5. **Not causes:** the cache build (0.2 ms, once), steady state (from ok4 on, B ≤ A + 3 ms of RT), and record (+6 ms on ok1).

## 4. The candidates against the measured excess
| Candidate | Saved from S (bench) | Why |
|---|---|---|
| 1. Open after the entrance, fade in | **s ≈ 120–180, moved** | The first visible scene frame lands at ok5 or later even in the earliest A-timed sample (§5). The cost lands on a settled Home at near-zero scene alpha. It changes B-11a's timing |
| 2. Pre-warm while invisible | ≈ 0 | HWUI skips alpha-0 nodes, so a warm-up must really draw. Before the gate that means frame 1, which is protected; after it, frames 1–4, which are inside S |
| 3. Simpler first pose | part, paid again at the switch | The full pose's first draw still pays its programs and geometry when it switches in. It adds two code paths |
| 4a. Cheaper geometry (oval clip-outs instead of PathOp lips) | uncertain, about 0–90 | It depends on Skia's renderer choice, which cannot be seen off-device. Home's finish lips keep the same class of path, and the gradient programs stay |
| 4b. Rest pose as bitmaps | s − about 35–50, so about 70–145 | It adds 10.5 MB (Landscape) or 5.8 MB (Quiet) plus the upload, and 7.7 ms of UI-thread raster per build, which recurs on every Home ↔ game navigation (about 10–25 ms on a phone). It breaks §7's "never on Home's start path, at any frame: … a bitmap", and it softens edges by a sub-pixel in motion |

## 5. The proposed change (one): open the scene at the end of Home's entrance, and fade it in
**Files, all in `ui/backdrop/`:**
- `BackdropSceneReady.kt`. `rememberBackdropSceneReady()` keeps its signature, its `State<Boolean>` and its closed first frame. After that first frame it waits on the **Compose frame clock** (`withFrameNanos`, the clock `clayEntrance`'s tweens run on) until `BACKDROP_SCENE_OPEN_MS` has passed since its first awaited frame, then sets the flag. With animations off (`rememberAnimationsEnabled()` false), it opens on frame 2 exactly as today.
- `BackdropMotion.kt`, two tokens:
  - `BACKDROP_SCENE_OPEN_MS = ClayAnim.ENTRANCE_MS + 5 × ClayAnim.ENTRANCE_STAGGER_MS = 440 ms`. That is the end of Home's longest cascade: hero 0, ribbon 1, both chips 2–3, tiles 4–5.
  - `BACKDROP_SCENE_FADE_MS`, the ux-designer's value. I suggest `NAV_TRANSITION_MS`, the sky's cross-fade.
- `SceneBackdrop.kt`.
  - An `Animatable` owned by `SceneBackdrop` runs from 0 to 1 when the flag opens. `SceneBackdrop` lives as long as the host, so the fade runs **once per host start** and never again on navigation, where the density cross-fade stays the only fade.
  - The scene node's existing `graphicsLayer` reads it with `CompositingStrategy.ModulateAlpha`, so no full-window offscreen buffer is allocated.
  - Outside the host (previews, component tests) the default `AlwaysReady` gives alpha 1.

**Not touched:** `navigation/` (`AppNavHost` is unchanged), `ui/home/` and `ui/theme/` (`ClayAnim` and `rememberAnimationsEnabled` are only read). Frame 1 is unchanged, so **15e is not owed**. The clock and the battery-saver read stay behind the same flag and start at the open.

**Expected saving.** On J-5's S, the scene's whole share, **≈ 150 ms (120–180)**. The residual C − A is then the finishes' share, expected at +45 to +105 ms (about +75 central) against T = 100.

**Where the moved cost lands.** On A's timing, which C's early frames should match, 440 ms of frame time is reached at ok6 (median; range ok2–ok8).
- The flag set on that frame k recomposes the scene on k + 1 at alpha 0. HWUI skips that frame.
- The first visible draw is k + 3, so ok5 or later in every sample, and about ok9 typically.
- Its RT cost is up to about 120–180 ms on this bench, less if Home's finishes have already allocated shared resources. It falls on a still screen: the cascade has ended, and the scene is at about one frame's share of its fade.
- On this bench the fade may read as stepped. A tap inside that window is delayed once per cold start. Nothing moving is interrupted.

**Tests, red first** (names read as sentences):
1. `theSceneStaysOffThroughHomesEntranceCascade`. Before `BACKDROP_SCENE_OPEN_MS` the layers are {Sky}, no clock frame has been awaited and the motion is AtRest. **Red today**, because the scene opens on frame 2.
2. `theSceneOpensAtTheCascadesEndAndFadesInOverItsToken`. A test-only `BackdropSceneAlphaKey`, beside the three existing hooks, goes 0 → 1 over `BACKDROP_SCENE_FADE_MS` on the test clock and stays at 1. **Red today.**
3. `withAnimationsOffTheSceneOpensOnTheSecondFrameWithNoFade` (`SystemMotionScale(OFF)`). Green today; it guards reduced motion.
4. `aNavigationAfterTheOpenFadesTheSkyButNotTheSceneAgain`. Guards the once-per-start rule.
5. `theOpenDelayCoversHomesLongestEntranceCascade`. The real `HomeScreen`, with both Continue chips at first composition, under the test clock: the last tile's pixels are stable from `BACKDROP_SCENE_OPEN_MS` on. It fails if Home lengthens its cascade.
- **Ruled test edits** (amended, not weakened):
  - `BackdropFirstFrameTest`: "within two frames" becomes "at the open".
  - `BackdropSettleTest.start()`: advance past the open instead of 3 frames.

**Effort:** about one developer-day (about ½ day for the code in three files, about ½ day for the tests), plus the ux-designer's spec amendment.

**What the player sees changes: yes.**
- The waves and lights now appear about 0.44 s after Home's first frame and fade in.
- The spec has them "from the second frame, with no fade of their own" (B-11a; §7 "The deferral"; the motion table's row "The waves' and lights' first appearance").
- So the producer sends this to the ux-designer before merge.
- A game screen restored after process death also opens 440 ms late with the fade, which is the ux-designer's call to accept.

## Open issues / risks
1. **J-5 can still FAIL on the finishes' share.** If f is about 105 ms (the three stencils being the finishes'), C − A stays above T = 100 with **any** backdrop-only change. The rest is then `glossFinish`'s lips and the gel tiles (`ui/theme/`, `ui/home/`), and J-4c's founder question applies.
2. **It moves the cost; it does not remove it.** J-5's S cannot see a cost past row 4. I suggest J-5 add an informative read, **RTscene = max(`SwapBuffers − SyncStart`) over rows 5–15**, for C and for A. That instrument is the tech-lead's decision.
3. **The trigger is computed, not observed.** It runs on the entrance's own clock and constants, and test 5 pins it to Home's real cascade. An observed signal, reported by `HomeScreen` and provided by the host, would touch `ui/home/` and `navigation/`, and so make 15e owed again.
4. **`ModulateAlpha` during the fade.** The back wave shows through the front wave's translucent parts slightly differently from a group fade, for the fade's length only. The group fade would cost a 10.5 MB full-window layer allocated on the open frame. This is the ux-designer's look.
5. **If the stall is unacceptable** once measured, removal options are 4a and 4b (§4). 4b needs §7's bitmap rule amended. Both belong on the 3.0.1 `FrameTimingMetric` list with phone numbers. Phone cost is still unmeasured; this is the tech-lead's caveat, unchanged.
6. **The JVM window is 1078 × 2422 px**, from qualifier rounding; the sizes above are at that window.
7. **Trailer.** This report's commit carries the session's attribution lines, as the branch's latest commit `52ae3c4` does. rules/gitflow.md 1.0.0 asks for the single Fable trailer instead. It is the same open question the branch's other reports raise.

## Handoff
Next: tech-lead — rule J-4b on §5:
- the trigger (computed or observed), and the tokens;
- J-5's informative RTscene read (Open issue 2).

Then the producer:
- dispatch the ux-designer on the timing and fade amendment of B-11a, §7 and the motion table, and on the `ModulateAlpha` look, before merge;
- dispatch me for J-4b, test-first.
