# ux-designer report — candy-backdrop: BD-23 (d), the two readings of items (2) and (6)
Date: 2026-11-01
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product snapshot (0.6.2) is older than the plugin (1.0.0), so I read the plugin copies of these rules in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch. I also read the worktree's `.claude/rules/lessons.md` (0 lessons); `.planning/recall/RECALL.md` does not exist. Then ui-ux-pro-max, android-accessibility and compose-skill.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort low (the harness's setting for this dispatch; the agent file pins fable, and Q-019 puts Fable-pinned roles on Opus until 3.0.0 ships)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Reading (a), the tolerance:** "±2/255" means 2 code values on the measured channel, as in BD-21. The qa-engineer read it correctly; the literal reading (2/255 of the fraction) is a unit error.
- **Reading (b), item (6):** "Home's second recorded frame" counts only frames in which Home's own content changes. The qa-engineer read it correctly; a frame that changes only the status bar is not a Home frame.
- **Item (2) was unsound, so I corrected it.** I had derived its ≥ 500 ms bound by counting the first-draw stall as ramp time, which is wrong. The bound contradicted B-11b's own accepted "visible 370 ms ramp", and its span moved with the recorder's frames. The new (2) measures the ramp's pace on its upper half, a lower bound that neither the stall nor the recorder's cadence can shorten, and requires ≥ 350 ms.
- **Recorded as B-11c**, a new ruling in `docs/ux/candy-backdrop.md` §1.2, with BD-23 (d) restated in §11.
- **Verdict on QA's existing data:** BD-23 (d) PASSES items (1)–(7), 10/10 on (2). The battery-saver clause was not run and is still owed. No new run is needed for (1)–(7).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`, amended in four places:
  - the front-matter `revised` line;
  - §1's status-table row for B-11;
  - a new **B-11c** after B-11b;
  - BD-23: the criterion cell's amendment note, and (d)'s pass line restated.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-bd23d.md`: this report.

## The ruling

### (a) Item (2)'s tolerance
- **What the line meant.** "±2/255" is two code values on the 8-bit channel measured. That is the notation BD-21 uses ("±2/255 per channel"). On the fraction it is ε = 2/|C_full − C_sky|: 0.036 in light and 0.027 in dark on QA's probe. QA's reading is right.
- **The literal reading is a unit error.** The fraction has no 255 in it, and 2/255 of the fraction is under half a code value on this probe, below the codec's noise.
- **The reading was not the real problem.** On the same run, `light-1` reads 483 ms under one band and 614 ms under the other. A pass line whose verdict moves with the band is unsound.

### Why item (2) is unsound, and what caused `light-1`
- **The stall, not the recorder.** QA attributed the 17 ms shortfall to the recorder. The recorder can only lengthen that span: its ends are the last frame still at 0 and the first frame already at 1, so a gap at either end adds time. The cause is B-11b's accepted first-draw stall.
  - While the fade's first visible frame is being drawn, the screen keeps showing the frame before the open, and the fade's clock keeps running.
  - `light-1`'s tail runs at the 700 ms pace: 0.46 → 0.96 in 367 ms, which is ≈ 0.73 s for a full rise.
  - Extrapolated back, the fade's clock started ≈ 0.38 s after H. That is ≈ 0.2 s before its first visible frame (0.580 s), a stall inside B-11b's 120–290 ms.
- **My own arithmetic error.** At the worst accepted stall, a 700 ms fade shows ≈ 700 − 290 − 2 × 25 ≈ 370 ms of visible rise in light, and ≈ 390 ms in dark.
  - B-11b says this itself: "the rest is still a visible 370 ms ramp".
  - My B-11a report's check ("the visible rise spans ≈ 690 ms against ≥ 500") counted the stall as time on the ramp. That was wrong, and it is where the 500 ms came from.
- **The old line could also pass falsely.** A 300 ms fade with a 200 ms recorder gap beside it would have read ≥ 500 ms.

### The correction
- **What (2) now measures.** Take A, the first recorded frame at fraction ≥ 0.50, and B, the last recorded frame before the first frame at 1. Then D = (t_B − t_A) / (f_B − f_A + 2ε) must be ≥ 350 ms.
- **Why ≥ 0.50.** Frames at 0.50 or above lie past B-11b's worst stall, whose first post-stall frame is at ≈ 0.44–0.47.
- **Why the recorder cannot bias it.** Both times are real presentation times, so the cadence decides only which frames exist, never the time between them. The ±ε noise is counted against the pass, so D is a lower bound on the true pace.
- **What 350 passes and fails.** It is half the token's pace. Any two frames ≥ 50 ms apart on a 700 ms ramp reach it. No linear fade of ≤ 350 ms passes, and neither does the rejected 240 ms decelerate (its tail reads ≈ 190–235 ms).
- **What the device leaves to the test.** The exact duration is BD-23 (a)'s, on the test clock.
- **A run that cannot be resolved.** Fewer than 50 ms of recorded ramp above 0.50 makes the run unresolved, and it is replaced by another run. Three unresolved runs in a row make (2) VOID.
- **The other items.** (3) and (4) stand. A sparse recorder can only make a step larger or "fully in" later, so it can cause a false fail but never a false pass. (1) is reworded in code values. The counts are set for (5)–(7) and for battery saver.

### (b) Item (6)
- **What the line meant.** Home's second frame, with only frames in which Home's content changes counting as Home's. QA's reading is right: a status-bar-only frame was drawn by the system, not by Home.
- **The corrected wording** defines a Home frame by a change in the app's area (between `statusInset` and the navigation bar) beyond a noise threshold that QA fixes before reading. That threshold is QA's own `appframes.py` rule: more than 6 samples moving by more than 6 code values.
- **What the device leaves to the test.** Whether the scene lands on rendered frame 2 or 3 is BD-23 (a)'s, because the recorder writes nothing while Home is unchanged.

## BD-23 (d) on QA's existing data, by the corrected line
D for item (2), computed from QA's per-frame series in `.planning/qa/candy-backdrop/j5/bd23d/read-light.txt` and `read-dark.txt`.
- **The series are complete over the ramp.** For `light-1`, `light-2`, `darkL-1` and `darkL-2`, the frame indices f(last-0)..f(first-1) match the number of series entries.
- **ε:** 2ε = 0.0714 in light and 0.0533 in dark.
- **Plain pace:** the same ratio without the noise term.

| Run | A (s, f) | B (s, f) | t_B − t_A | D (≥ 350) | plain pace | old span |
|---|---|---|---|---|---|---|
| light-1 | 0.814, 0.64 | 0.996, 0.86 | 182 ms | **625** | 827 | 483 |
| light-2 | 0.715, 0.50 | 1.081, 0.95 | 366 ms | **702** | 813 | 663 |
| light-3 | 0.832, 0.59 | 1.065, 0.88 | 233 ms | **645** | 803 | 549 |
| light-4 | 0.751, 0.52 | 0.951, 0.86 | 200 ms | **486** | 588 | 563 |
| light-5 | 0.780, 0.54 | 1.029, 0.91 | 249 ms | **564** | 673 | 535 |
| darkL-1 | 1.197, 0.52 | 1.488, 0.93 | 291 ms | **628** | 710 | 609 |
| darkL-2 | 1.400, 0.53 | 1.635, 0.81 | 235 ms | **705** | 839 | 548 |
| darkL-3 | 1.384, 0.53 | 1.727, 0.95 | 343 ms | **725** | 817 | 798 |
| darkL-4 | 1.344, 0.56 | 1.611, 0.89 | 267 ms | **697** | 809 | 659 |
| darkL-5 | 1.424, 0.67 | 1.545, 0.81 | 121 ms | **626** | 864 | 654 |

- **(2) PASS, 10/10.**
  - (a): QA found no decrease beyond 2 code values after the open in any run.
  - (b): the minimum D is 486 ms (`light-4`). Every run is resolved, with every A–B pair ≥ 121 ms apart.
- **The plain pace is ≈ 0.6–0.86 s:** the 700 ms token, within the 2-decimal rounding of the series.
- **(1), (3), (4), (5), (7) PASS** as QA read them. Their meaning is unchanged, and QA's per-run readings already use code values.
- **(6) PASS** on QA's reading, which is now the line's wording.
  - In all four runs, the recorded frames between Home's first frame and the scene's frame change at most 6 samples by at most 6 levels, so they are not Home frames.
  - The scene arrives in one step.
- **Battery saver: NOT RUN.** Two cold starts per theme are owed (B-11c).

## Checks
- **Spec consistency, by reading** — pass.
  - I re-read B-11b, BD-21, BD-23 (a)–(e) and §12 against B-11c.
  - B-11b's prose already states the ≈ 370 ms visible ramp, so B-11c agrees with it.
  - (a)'s test still owns the duration and the frame count.
  - No test name or oracle in §12 changes.
- **D arithmetic** — pass. I computed it by hand from QA's two read files, for all ten runs (table above).
  - Rounding to 2 decimals in the series moves D by at most about ±7%. The minimum, 486 ms, stays above 350 under that error.
- **Bound derivation** — pass.
  - On a 700 ms linear ramp, a pair 50 ms apart gives D = 50 / (0.0714 + 0.0714) = 350 ms in light, and less is needed in dark.
  - A linear fade of P ≤ 350 ms gives D = Δt / (Δt/P + 2ε) < P, so it can never pass.
- **`Edit` on `docs/ux/candy-backdrop.md`, 5 edits** — pass; each old string matched exactly once.
- **`./gradlew check`** — not run. This role has no Bash, and the change is to documents only.
- **Device** — not run. The existing data answers (1)–(7).

## Open issues / risks
1. **Battery saver** ("(1)–(4) hold at the rest pose") is unrun: two cold starts per theme. Until it passes, BD-23 (d) is not closed. It is a short emulator sitting with no Gradle, and it can share any other bench visit.
2. **The D values are my desk computation from QA's readings.** Under standing rule 1, the qa-engineer should re-derive them from the same files, or from the kept recordings with `bd23d.py` extended by the new (2)(b). That needs no device. The recordings must stay under `.planning/qa/candy-backdrop/j5/` until then.
3. **The 350 ms bound was set after the data was seen.** It is derived from B-11b alone: the 700 ms token and the 300 ms pop line. The verdict is the same for any bound from 300 to 486 ms.
4. **My earlier error.** The B-11a report's "(2) still holds … ≈ 690 ms" check was wrong (above). That report is a dated record and is not edited. B-11c records the correction.
5. **Item (6) cannot tell an unscaled 440 ms open from the bench's frame-1 → frame-2 stall.** On the bench the scene arrives 0.40–0.57 s after H. BD-23 (a)'s "animations off: present within two `advanceTimeByFrame()`" test is what rules that out. The code-auditor should confirm it is green and unweakened, as already asked in the B-11a handoff.
6. **No founder question.** This is a measurement correction, not art direction.

## Handoff
Next: producer — commit `docs/ux/candy-backdrop.md` and this report on `feature/candy-backdrop`. Treat BD-23 (d) as PASS on (1)–(7), with battery saver owed.
Next: qa-engineer — a desk check with no device: re-derive (2)(b)'s D for the ten runs from `read-light.txt` and `read-dark.txt` (or the kept recordings) by the restated line, and confirm or correct the table above. At the next bench visit, run battery saver: two cold starts per theme, (1)–(4) at the rest pose.
Next: code-auditor — no new diff to review. Confirm that BD-23 (a)'s animations-off and fade-duration assertions in `BackdropSceneOpenTest` are present and unweakened, because the device line now defers both to them.

## The corrected wording, verbatim (BD-23 (d), from `docs/ux/candy-backdrop.md` §11)

**Tolerance:** every "±2/255" in (d) is 2 code values on the measured channel, which is ε = `2 / |C_full − C_sky|` on the fraction. "At 0" means within 2 code values of `C_sky`, and "at 1" means within 2 code values of `C_full`.

**(2)** **never back, and a gradual ramp.** (a) From the open to the end of the recording, the measured channel never moves back toward `C_sky` by more than 2 code values from the furthest value it has reached. (b) Let A be the first recorded frame at fraction ≥ 0.50, and B the last recorded frame before the first frame at 1. The ramp's pace `D = (t_B − t_A) / (f_B − f_A + 2ε)` is ≥ 350 ms. If B is not after A, or `t_B − t_A` < 50 ms, the run is unresolved for (2): it neither passes nor fails, and another cold start in the same theme replaces it. Three unresolved runs in a row make (2) VOID, never a pass. D is a lower bound on the pace the player saw, and neither the first-draw stall nor the recorder's cadence can shorten it. The fade's exact duration is (a)'s test;

**(6)** at animator scale 0, two cold starts per theme: the scene goes from 0 to 1 in one recorded frame, with no recorded frame between 0 and 1, and that frame is Home's second frame on the recording. Home's frames are its first recorded frame and each later recorded frame in which the app's area changes. The app's area is the window between `statusInset` and the navigation bar's top edge, sampled on a grid of ≤ 6 px pitch. A frame changes it when more than 6 samples move by more than 6 code values from the previous Home frame. QA fixes that noise threshold before reading, and may tighten it but never loosen it. A recorded frame that changes only the status bar or the navigation bar is not a Home frame. Whether that frame is Home's rendered frame 2 or 3 is (a)'s test;

**BD-23 (d) verdict on the existing data: PASS on items (1)–(7), (2) at 10/10 with D 486–725 ms against ≥ 350 ms. The battery-saver clause is not run and is owed (two cold starts per theme). (d) closes when it passes.**
