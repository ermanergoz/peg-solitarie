# tech-lead report — candy-backdrop: ruling on J-2's second VOID
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; HEAD `3dda88d`, whose code equals `d029950`; develop = merge-base = `d251c3b`)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, testing, compose, kotlin-android, dispatch, stacks, gitflow, skills-policy, logging, memory, puzzle-design and play-store in full, plus `.claude/rules/lessons.md`), android-architecture, android-gradle-logic, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh. The founder's Q-019 answer puts Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Ruling (a): go to J-4 now.** I do not re-judge J-2's line. That instrument is VOID and stays VOID. But the saved framestats already show a product cost that belongs to arm B's APK. The null control, which runs B's APK, reproduces it, arm A does not have it, and both attempts show it:
  - a one-off hitch of about **+220 ms** in the first five frames after Home's first paint (B 566.8 / 591.6 ms against A 350.4 / 367.1 ms; null floor 14.4 / 38.8 ms);
  - most of it is on the RenderThread in frame 1, which is the backdrop scene's first draw (B 263 / 290 ms against A 124 / 132 ms).
- The miss count was the wrong lens. On this bench almost every early frame misses its deadline in every arm, so the count saturates at 10–12. The cost shows up in the time those frames take.
- **What I specify:** the developer's time-boxed `compose-performance-audit` (J-4a). After my ruling on the change, it is re-measured with **J-5**, a corrected instrument that replaces J-2's 16-frame rule.
- **15f (J-3) PASS** closes that condition.
- **VERDICT: HOLD.** The only open condition is BD-23 (c), now on the J-4 path. The founder fallback stays as before, so the week is not at risk (J-4c).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-j2-ruling.md` — this ruling, J-4a (the audit task) and J-5 (the re-run instrument).
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-signoff.md` — an addendum at the end updates the sign-off table: 15f met, BD-23 (c) on J-4, verdict HOLD.

## 1. What the evidence shows

### 1.1 Why this is not a re-judgement
- J-2's line (deadline misses in the first 16 frames) has no verdict, and it will get none. I do not apply a new line to old numbers.
- What I use the old data for is choosing the path among (a), (b) and (c). That is exactly what J-2's "a second VOID comes to me with the numbers" asks for.
- Every figure below is computed from QA's saved dumps (`.planning/qa/candy-backdrop/jank2/attempt-*/samples/`). I read them the way J-2 did: columns by name, the MainActivity window's `PROFILEDATA`, rows ordered by `IntendedVsync`, and `Flags` = 0.

### 1.2 Per-frame phases (both attempts pooled; samples with ≥ 16 `Flags=0` rows; medians in ms)
Column meanings:
- **start delay** = `HandleInputStart − IntendedVsync`, which is main-thread time before the frame starts;
- **record** = `SyncQueued − DrawStart`;
- **RT** = `SwapBuffers − SyncStart`, the RenderThread.

| Frame | A total | B total | N (B's APK) total | A start delay | B start delay | A record | B record | A RT | B RT | N RT |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | 643 | 628 | 651 | 610 | 600 | 12.3 | 5.3 | 0.9 | 0.8 | 0.8 |
| 1 | 201 | **367** | 367 | 49 | 37 | 3.4 | 9.3 | 122.6 | **279.1** | 270.0 |
| 2 | 167 | **367** | 367 | 48 | 67 | 15.7 | 17.2 | 14.9 | **48.3** | 49.5 |
| 3 | 240 | **417** | 417 | 150 | **311** | 21.1 | 21.0 | 3.6 | 32.7 | 34.5 |
| 4 | 134 | 167 | 152 | 77 | 77 | 21.7 | **42.9** | 3.0 | 5.9 | 6.1 |
| 5 | 134 | 117 | 127 | 78 | 72 | 17.1 | 17.2 | 8.7 | 8.1 | 7.2 |
| 6 | 116 | 67 | 67 | 37 | 19 | 23.6 | 18.0 | 7.5 | 6.8 | 5.8 |
| 7 | 84 | 67 | 67 | 33 | 23 | 18.4 | 19.2 | 4.4 | 4.6 | 5.2 |

(A n = 31, B n = 28, N n = 32 samples.)

Reading the table:
- **Frame 0.** It is the same in every arm, and so is the launch's TotalTime. R3's deferral works: the scene is never on the first frame, so TTID is untouched. This is consistent with 15e's +44.5 ms.
- **Frames 1–3.** B's excess sits here:
  - RenderThread: +156 ms on frame 1 and +33 ms on frame 2. This is the first draw of the waves and lights. On Skia OpenGL, that means rasterising or tessellating full-window non-convex paths (the PathOp `lip` and `sheenPath`) and compiling the gradient programs.
  - Main thread: a further +161 ms of start delay before frame 3 (blocked on sync, or the scene's first composition and cache build).
  - UI-thread record: only +6 to +21 ms. The PathOp build in `drawWithCache` is therefore a minor share, not the main cost. This revises J-4's first guess.
- **Frames 5 onward.** B is no slower than A. There is **no steady-state jank**: B settles at 67 ms frames, and A runs at 84–116 ms. The lower frame count B shows in the one-second window is the hitch, not the animation's per-frame cost. This retires J-1's warning.

### 1.3 The one number that separates the arms: settle time
**S** = `FrameCompleted(ok[4]) − FrameCompleted(ok[0])`, where ok holds the complete `Flags=0` rows.

| Attempt | N1 | N2 | floor | A | B | B − A |
|---|---|---|---|---|---|---|
| 1 | 567.8 (12) | 582.2 (7) | 14.4 | 350.4 (12) | 566.8 (10) | **+216.4** |
| 2 | 558.8 (8) | 597.6 (8) | 38.8 | 367.1 (12) | 591.6 (12) | **+224.5** |

What the table shows:
- The difference is about 15× the null floor, and it is the same on both boots.
- The RenderThread time of frame 1 gives the same picture: B − A is +139.5 / +158.1 ms, against null floors of 13.2 / 14.4 ms.
- The S rule needs only 5 complete frames. That would have met J-5's kept targets below on both attempts: A 12/12 and 12/12, B 10/12 and 12/12, N2 7/8 and 8/8.

I registered J-5 after seeing this data, so this check proves only that the instrument is feasible. It is not a verdict on B.

### 1.4 Why (a) and not (b) or (c)
- **Not (b).** Another measurement of arm B would only confirm, on a fixed instrument, what the dumps already show twice with a null control. The bench time is better spent measuring the fix.
- **Not (c), yet.**
  - The cost is the product's. It is attributable to B's APK and lands exactly when Home's entrance plays, which is the game's first impression.
  - The `compose-performance-audit` is owed before release anyway (rules/compose.md), so J-4a adds no new work to the release, only an earlier start.
  - The likely fixes are small and confined to `ui/backdrop/`.
  - (c) remains the founder's fallback under J-4c, with this cost measured and recorded.
- **Caveat, stated once.** The bench renders on `Pipeline=Skia (OpenGL)` through the emulator's host-GLES translation. First-draw shader and path costs on a phone GPU with a persistent shader cache are probably smaller. "Emulator only" cannot show that either way, so the decision is taken on the bench's numbers.

## 2. J-4a — the developer's audit (android-game-dev, opus; time box: half a day; no product edit before my ruling)
**Skills.** `compose-performance-audit`, then `compose-skill` and `compose-ui`.

**Inputs:**
- this report's §1;
- `tech-lead-signoff.md` (J-2, J-4);
- the saved dumps under `.planning/qa/candy-backdrop/jank2/attempt-1/samples/` and `attempt-2/samples/`, with each attempt's `launches.csv` (the `apk` and `label` columns map each dump to its arm; N is B's APK);
- the code under `app/src/main/java/com/app/pegsolitaire/ui/backdrop/`, in particular:
  - `SceneBackdrop.kt` (`BackdropSlot`, `BackdropScene`'s `graphicsLayer().drawWithCache`, `WaveDrawing.of`, `waveShape`, `LightDrawing.of`);
  - `BackdropSceneReady.kt` (the R3 gate);
  - `AppNavHost.kt`, where the gate is provided.

**Measure and report (with numbers):**
1. **Framestats, per frame index 0–7, A against B.** Use the columns named in §1.2, and split RT further into `SyncStart → IssueDrawCommandsStart` (texture upload and sync) and `IssueDrawCommandsStart → SwapBuffers` (draw issue). For frame 3's start delay, compare `SyncQueued` and `SyncStart` of frame 2 to see whether the main thread was blocked on the RenderThread.
2. **On the JVM**, no device:
   - Time `buildSceneModel` plus `WaveDrawing.of` / `LightDrawing.of` for each `BackdropDensity` at 1080×2424, as an upper bound on the cache build.
   - Show with an existing or new test that the `drawWithCache` block builds once per size, density, theme and direction, and never per animation frame.
   - Count the paths, their kind (convex or not, PathOp output or not), the distinct brushes (gradient programs) and the draws per frame.
3. **Map each candidate to the measured excess.** The RT excess on frame 1 is about 156 ms and the excess before frame 3 is about 161 ms. State which share each candidate removes, and why.

**Candidates to evaluate.** The developer proposes, and may add others:
1. **Defer the scene's first draw.** Open the gate after Home's entrance settles, rather than one frame after first paint, and fade the scene in. The hitch then lands on a still screen. This is the cheapest change. It moves the cost and does not remove it. Name the trigger (an entrance-end signal, never a wall-clock delay; tests stay deterministic).
2. **Pre-warm off the visible path.** Prepare the cache's paths and brushes, or draw the scene once while invisible, before the gate opens. The cost must not move into frame 0, because TTID and 15e are protected.
3. **Simplify the first pose.** Draw the first frames with convex primitives only (ovals and rects, no PathOp `lip` or `sheen`), and switch to the full pose once warm.
4. **Cheaper geometry.** Replace the PathOp `Difference` and `Intersect` results with draw-order or clip equivalents that rasterise faster, or rasterise the rest pose once to a bitmap. If a bitmap, state its memory and upload cost.

**Deliverable.** `.planning/reports/candy-backdrop/android-game-dev-perf-audit.md`, with:
- the metrics table;
- the ranked causes;
- one proposed change with its files, its expected recovery in ms, the tests it adds or changes, and whether it touches `navigation/`, `ui/home/` or `ui/theme/` (if it does, 15e is owed again);
- whether what the player sees changes (timing or first pose). If it does, the producer sends it to the ux-designer before merge.

## 3. J-4b — after my ruling: the change and J-5
- The developer implements the change I accept, test-first, on `feature/candy-backdrop`, with the Check green.
- The code-auditor reviews the diff.
- QA runs **J-5** once, at one bench visit, then 15e as well if the start path changed.

### J-5 — the corrected instrument (replaces J-2's value, invalid rule and line; everything else in J-2 stands)
**Bench, APKs, preconditions, Remove animations handling and the per-launch commands:** as in J-2, with two changes:
- arm **C** is the changed build (debug, built as in R3, sha recorded);
- arm **A** stays `armA-d251c3b.apk`.

The build stays debug so the numbers compare with J-2. The cost sits on the RenderThread, which R8 does not change.

**Sequence:** W-A, W-C; N1, N2, N1, N2 (all on C's APK); A, C, A, C, A, C. The first launch of each block is discarded.

**Value per sample.**
- From the MainActivity window's `PROFILEDATA`, columns by name, take rows with `Flags` = 0 **and** `FrameCompleted > IntendedVsync` (this drops in-flight rows), ordered by `IntendedVsync`.
- **S = FrameCompleted(row 4) − FrameCompleted(row 0)**, in ms.
- Informative: RT1 = `SwapBuffers − SyncStart` of row 1, and TotalTime.

**Validity.**
- A sample is valid when the launch is COLD, the dump is readable, and it has at least **5** such rows.
- Kept targets are **at least 10 of 12 per arm, and at least 6 of 8 per null label**.
- A label that falls short gets one extra block, appended once. If it is still short, the run is VOID.
- Mid-run load readings are recorded, informative only. The interleaved null control carries the host noise.

**Line.**
- floor = |median S(N1) − median S(N2)|.
- T = max(**100 ms**, 2 × floor). 100 ms is the threshold class 15e already uses for this product's start path.
- **PASS** when median S(C) − median S(A) ≤ T.
- **VOID** when floor > 100 ms. QA makes one re-attempt on a fresh boot.
- **FAIL** otherwise. QA does not re-run a FAIL.

For reference only: on J-2's dumps, arm B is +216 / +225 ms.

**Output:** `.planning/reports/candy-backdrop/qa-engineer-j5.md`, with artifacts under `.planning/qa/candy-backdrop/j5/`.

## 4. J-4c — the week's stop rule
The producer puts **one** founder question (`Q-<NNN>`) if either of these happens:
- J-4a finds no change worth at least about 120 ms of recovery within about one developer-day;
- J-5 FAILs or VOIDs twice.

The question:

> Ship 3.0.0 with the measured cost (a one-off hitch of about +220 ms in the first five frames after Home's first paint, on the emulator; TTID +44.5 ms; no steady-state jank; phone cost unmeasured), or slip.

**My recommendation for that question: ship.**
- The cost is one-off, visible only during the entrance, and absent from frame 0 and from steady state.
- It is measured on an emulator's GL translation.
- Phone frame timing goes on the 3.0.1 list as a Macrobenchmark `FrameTimingMetric` run, which is already filed.

The branch does not merge on a FAIL without that answer. J-4a and J-4b run alongside the release-branch preparation and do not block it.

## Checks
- `git rev-parse --abbrev-ref HEAD; git rev-parse --short develop; git merge-base HEAD develop` — pass (`feature/candy-backdrop`, `d251c3b`, `d251c3b`).
- `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` — pass (exit 0; the code under review is still arm B).
- `python3 -I <scratchpad>/j2/phase.py .planning/qa/candy-backdrop/jank2` — pass (the §1.2 table; reads the saved dumps only).
- `python3 -I <scratchpad>/j2/settle.py .planning/qa/candy-backdrop/jank2` — pass (the §1.3 table, with in-flight rows dropped; the earlier run without that filter showed negative S on 5 short samples, all rows with `FrameCompleted` = 0, which is why J-5 drops them).
- Read `qa-engineer-jank2.md`, `tech-lead-signoff.md`, `jank2/info.txt`, `jank2/j2stats.py`, `attempt-2/values.csv` (head), and `SceneBackdrop.kt:85–215` — pass.
- Not run: the Check, Gradle and any emulator. The scripts are git-ignored scratch, not committed. `.planning/log/` was not read.

## Open issues / risks
1. **Phone cost unknown.** The cost is measured only on `Skia (OpenGL)` through emulator GL translation. This goes into the Q-PUBLISH-3.0.0 material either way, beside the +44.5 ms TTID.
2. **Candidate 1 moves the cost and does not remove it.** If it is chosen, J-5 measures the entrance window, so a hitch moved past frame 4 would read as a PASS. The developer's report must say where the moved cost lands. I will require the ux-designer's look at the fade-in if that candidate is chosen.
3. **J-3 / 15f PASS** closes that condition, with no deviation.
4. **Host load.** Spotlight's `mds_stores` and `mediaanalysisd` load the host mid-run. This is not a void clause in J-5, because the null control absorbs it. Excluding the AVD folders from Spotlight before the visit is a cheap improvement for the producer to arrange.
5. **Staffing and trailer:** Opus under Q-019. The commit trailer question is the same open one the branch's other reports raise.

## Handoff
Next: producer — dispatch android-game-dev for **J-4a** (§2), with the report at `.planning/reports/candy-backdrop/android-game-dev-perf-audit.md`. Then:
- re-dispatch me with that report for the ruling on the change (J-4b);
- keep `.planning/qa/candy-backdrop/jank2/`, because the audit reads it;
- keep the J-4c stop rule in view for the week's schedule;
- add Open issue 1 to the Q-PUBLISH-3.0.0 material.

**Ruling: (a)**

VERDICT: HOLD
