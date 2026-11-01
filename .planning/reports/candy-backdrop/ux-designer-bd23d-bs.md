# ux-designer report — candy-backdrop: BD-23 (d), the battery-saver result (item (4)'s anchor, and the count)
Date: 2026-11-01
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the rules CLAUDE.md imports from `.claude/rules/`: studio, reporting, gitflow, testing, ip-policy, skills-policy, stacks, logging, memory, puzzle-design, play-store, kotlin-android, compose, lessons, audio; and the plugin's dispatch.md read in full), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort low (the harness's setting for this dispatch; the agent file pins fable, and Q-019 puts Fable-pinned roles on Opus until 3.0.0 ships)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **What item (4) was meant to bound:** the backdrop's own path to fully in: the 440 ms open, at most ≈ 290 ms of stall and the 700 ms fade, ≈ 1.43 s in all, plus ≈ 0.57 s of headroom for the bench.
- **Why the anchor was wrong:** it assumed that Home's cascade ends 0.44 s after Home's first recorded frame. On the bench, the system splash cross-fade and Home's frame-1 → frame-2 stall come before the cascade's end. The splash is not part of the app's fade.
- **The correction (B-11d):** (4) is now counted from T, the moment the Challenge tile settles (item (1)'s registered definition), with a bound of 2.0 − 0.44 = **1.56 s**.
- **`bs-light-2` is a line defect, not a product finding.** It reads 1.075 s after T. No product change: the fade stays 700 ms linear in battery saver.
- **The battery-saver count, restated:** four cold starts, two per *system theme setting*, with the rendered theme recorded. Where the platform forces dark, no light run is owed. QA's four runs meet it. **No new device run is needed.**

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`, amended in six places:
  - the front-matter `revised` line;
  - §1's status-table row for B-11;
  - a dated pointer at the end of B-11c;
  - a new **B-11d** after B-11c;
  - BD-23: the criterion cell's amendment note;
  - BD-23 (d)'s pass line: the restatement note, item (4) and the battery-saver clause.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-bd23d-bs.md` — this report.

## The ruling

### What item (4)'s anchor and bound were meant to be
- **The bound came from B-11b's own budget:**
  - the open at 440 ms of animator time after the host's first frame;
  - a first-draw stall of up to ≈ 290 ms;
  - a 700 ms linear fade.

  That is ≈ 1.43 s. The 2.0 s line gave ≈ 0.57 s over it for emulator frame pacing.
- **What it is for:** to fail an open that waits on something past the cascade, a fade longer than the token, and a fade that stalls or never finishes.
- **What the anchor assumed:** that the app's clock starts at Home's first recorded frame (H) and runs in real time from there, so that the cascade ends at H + 0.44 s.
- **What the data shows instead:** T − H is 0.30–0.41 s in J-5's light runs, 0.58–0.96 s in its dark runs, and 0.52–0.97 s in battery saver. Two things that are not the backdrop's sit in that gap:
  - **The system's starting window:** the splash and its cross-fade. In `bs-light-2`, H to Hs is ≈ 0.22 s (2.045 − 1.823).
  - **Home's cold start:** the bench's frame-1 → frame-2 stall, 0.40–0.57 s in J-5's scale-0 runs. No animation advances before it ends.

  Both belong to 15e, BD-23 (b) and D-03j.
- **Is the system splash part of the app's fade? No.** The system draws the splash and its exit cross-fade and times them. The app's fade starts at the open, after the cascade.
- **Why T, not Hs:**
  - Hs removes only the splash, and the frame-1 → frame-2 stall still lands in (4).
  - T is the device's reading of the cascade's end, which is the moment B-11b opens the scene. It was registered in `bd23d.py` before any data, as item (1)'s moment.
- **The bound from T:** 1.56 s, the old allowance less the product's 0.44 s constant. It keeps the old line's exact headroom on the part the backdrop owns.

### `bs-light-2`: a line defect, not a product finding
- **Its fade ran on the token.** D is 823 ms. The scene is at 1 1.075 s after T, against `darkL-1`'s 1.073 s without battery saver.
- **Where its 2.045 s came from:** T fell 0.970 s after H.
- **The old verdict hung on one code value.** At 1.996 s the reading was 144, against `C_full` = 147, where J-5's scale-0 dark rest pose read 145 at the same pixel.
- **No product change.** A shorter fade in battery saver would save about 20 frames once per host start. In exchange it would weaken the pace that (2) and (3) guard, and the cold-start fade would no longer match B-13a's navigation cross-fade. B-11b's battery-saver bullet stands as written.

### The battery-saver count
- On the API 36 Pixel image, battery saver forces the system's dark theme, so light cannot be reached in battery saver.
- **Restated:** four cold starts, two with the system setting light and two with it dark, with the rendered theme recorded. Where the platform forces dark, all four are dark runs and no light run is owed.
- Battery saver changes the backdrop's motion, not its colours. Light's open and fade at scale 1 are covered by J-5's five light runs.

## BD-23 (d) on the existing data, by the restated line
Item (4) is "fully in after T", computed as QA's "at 1 after H" minus QA's T − H (both from `qa-engineer-j5.md` and `qa-engineer-bd23d-close.md`). The bound is ≤ 1.56 s.

| Run | at 1 after H | T − H | **at 1 after T** |
|---|---|---|---|
| light-1 | 1.063 | 0.350 | 0.713 |
| light-2 | 1.148 | 0.298 | 0.850 |
| light-3 | 1.132 | 0.414 | 0.718 |
| light-4 | 1.048 | 0.316 | 0.732 |
| light-5 | 1.097 | 0.380 | 0.717 |
| darkL-1 | 1.657 | 0.584 | 1.073 |
| darkL-2 | 1.784 | 0.861 | 0.923 |
| darkL-3 | 1.959 | 0.699 | 1.260 |
| darkL-4 | 1.731 | 0.893 | 0.838 |
| darkL-5 | 1.715 | 0.963 | 0.752 |
| bs-light-1 (rendered dark) | 1.953 | 0.938 | 1.015 |
| bs-light-2 (rendered dark) | 2.045 | 0.970 | 1.075 |
| bs-dark-1 | 1.564 | 0.592 | 0.972 |
| bs-dark-2 | 1.803 | 0.515 | 1.288 |

- **(4) PASS, 14/14.** The maximum is 1.288 s (`bs-dark-2`), with 0.27 s of margin. The verdict holds for any bound ≥ 1.29 s.
- **Battery-saver clause PASS** on the restated count: four runs, two per system setting, all rendered dark. QA reads:
  - (1) 4/4;
  - (2) 4/4, with D 629–879 ms;
  - (3) 4/4, with the largest step 0.364;
  - (4) 4/4 as above;
  - the rest pose holds.
- **Items (1)–(3) and (5)–(7):** PASS as QA's two reports read them. That includes (2) at 10/10 (D 486–733 ms, QA's recomputation) and (6) at 4/4 under the Home-frame rule.

## Checks
- **Arithmetic** — pass. I subtracted the 14 rows above by hand from QA's two tables. I checked 2.045 − 0.970 = 1.075, and 1.803 − 0.515 = 1.288 (the maximum).
- **Bound derivation** — pass. 2.0 − 0.44 = 1.56 s. The open, the worst stall and the fade come to 1.43 s from H, or 0.99 s from T, so the headroom over the backdrop's own part is unchanged.
- **Spec consistency, by reading** — pass. I re-read B-11b, B-11c, B-13a, §6.3 item 5, §7 and BD-23 (a)–(e) against B-11d:
  - (1)'s T is the same moment (4) now uses;
  - (a) still owns the duration and (e) the cascade's length;
  - no test name or oracle in §12 changes.
- **`Edit` on `docs/ux/candy-backdrop.md`, 6 edits** (the B-11c pointer and B-11d went in as one) — pass. Each old string matched exactly once.
- **`./gradlew check`** — not run. This role has no Bash, and the change is to documents only.
- **Device** — not run, and not needed. The existing recordings answer every item.

## Open issues / risks
1. **The new anchor and bound were set after the data was seen**, as QA flagged for Hs. Two things limit the risk:
   - The bound is the old line's own allowance moved by the product's constant (0.44 s), not fitted to the runs.
   - T is a definition registered before any data.

   The code-auditor may weigh this.
2. **In dark, the open lags T** by 0.18–0.61 s (J-5's (1) note). This includes `bs-dark-2`, whose fade starts ≈ 0.59 s after T. That is more than J-5's measured RT_late (≈ 50 ms) explains. It sits inside (4)'s allowance, and B-11b's computed trigger accepts it, but it is worth one look on a physical phone in the 3.0.0 device pass. It is informative and not a blocker.
3. **The forced dark theme comes from the platform.** On an OEM build that keeps the light theme in battery saver, the light-setting runs would render light. The restated count already covers that case, and nothing in the backdrop's battery-saver path depends on colour.
4. **No founder question.** This is a measurement correction, not art direction.

## Handoff
Next: producer — commit `docs/ux/candy-backdrop.md` and this report on `feature/candy-backdrop`. Treat BD-23 (d) as closed: PASS on the existing data, and no new bench visit.
Next: qa-engineer — a desk confirmation, with no device: re-derive item (4) as "at 1 after T" for the 14 runs from `read-*.txt` and `read-bs.txt`, and confirm or correct the table above (standing rule 1).
Next: code-auditor — no new diff to review. You may weigh Open issue 1, the post-data re-anchoring, in your line on BD-23.

**VERDICT (BD-23 (d) overall, on the existing data): PASS** — items (1)–(7) PASS, with (4) at 14/14 and at most 1.288 s after T against ≤ 1.56 s. The battery-saver clause PASSES: four runs, two per system theme setting, all rendered dark, (1)–(4) at 4/4 and the rest pose held. `bs-light-2` is a line defect, and the product does not change.
