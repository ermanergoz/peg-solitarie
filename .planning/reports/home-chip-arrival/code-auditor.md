# code-auditor report — home-chip-arrival
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (report only); under review `git diff 4ff4ec6...c388993`
Skills loaded: ergoz-soft:standards (code-auditor; plugin 1.0.0 rule files read in full), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none of my own (host shared, load up to ~22)
Ran on: claude-opus-5-5 at effort xhigh — **staffing deviation**: pinned fable; Fable limit reached; Q-019 answer 3. Author also on Opus; mitigations: fresh agent, own `--rerun-tasks` Check, own mutation probes, every red-first claim re-run.
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: returned as text (the harness refuses report files from subagents); condensed by the producer
> with every finding, ruling, probe and check kept.

## Summary
- D-03n's measure policy holds the tiles still on all six 15s windows and moves them by exactly the shortfall in 15t. D-03o, D-05e, D-05f and INFO rows 1, 3, 4, 5 are correct and each was red first.
- `app/src/main` changes exactly the brief §4 list less `Clay.kt`/`Theme.kt` (C9 struck). No frozen file changed; nothing suppressed.
- **FAIL on finding 1**; two test-only gaps (findings 2, 3).

## Findings
1. **MEDIUM, must fix (production, before the sitting): a leaving chip is still a live control.** `ui/home/HomeBandColumn.kt:302-305` latches the last value and keeps the chip composed while its room closes; `HomeScreen.kt:482`/`:506` build `onResume` from the latched value; `:547` keeps the Card's `onClick` live. For up to 240 ms after its save is gone, a tap or TalkBack click resumes the stale target — the phantom Continue WR-02/CR-01 removed and sign-off Decision 3 rejected. Challenge: launches the replaced ladder's stamped level; a win then calls `ChallengeProgress.advancePast` → `mergeLadder(level + 1)`, merging the old device ladder into the cloud progress the player just chose. Evidence: throwaway `ProbeLeavingChipTest` at `w412dp-h915dp` passed — four frames after the emission with no Challenge save, "Continue Challenge" had an `OnClick` and invoking it called `onResumeChallenge(9)`. **Fix:** while `value == null` the slot shows the chip inert — no click, no semantics action, not a focus stop; add a test that a closing chip neither resumes nor is clickable.
2. **LOW, must fix (test-only): 15w's "nothing is cut" cannot fail.** `HomeRibbonShapeTest.kt:209-226`; `HomeProgressRibbon.kt:322-323`, `:337-338` set `softWrap = false` with default `Clip`, so `lineCount == 1` always holds and Column-coerced bounds always sit inside the pill. Probe P13 (labels clipped to 20 dp) stayed 12/12 + 17/17 green. **Fix:** assert `!layoutOf(text).hasVisualOverflow` for every label and value.
3. **LOW, should fix (test-only): 15t does not pin the footer's give-up.** `HomeChipArrivalTest.kt:244-269` vs `HomeBandColumn.kt:139`. Probe P5 (`val footer = footerWithNoChip`) left all 140 `ui.home` tests green. **Fix:** assert the footer (or the overflow) against `min(S₀/5, max(0, S₀ − R))`.
4. **LOW, recommended (production, cosmetic; with finding 1): the chip's shadow pops.** `HomeBandColumn.kt:310-318` — `clipRect(bottom = size.height * open)` also clips left/top/right, so the 6 dp `clayShadow` is cut for 240 ms and appears whole in the settle frame. **Fix:** clip only the bottom edge. QA's 15v part 2 should watch for it.

INFO (accepted): I1 room first moves in frame 3 after the write (Animatable start latency; nothing shows in frames 1–2); I2 a few two-element lists per frame while a room moves, none in steady state; I3 `single()`/two-tile destructuring assumptions, guarded by every Home test; I4 a late chip's 16 dp slide leaves ≤ 16 dp at alpha ≤ 0.17 for 1–2 frames (15v part 2 is the read); I5 `ContinueChips` takes the whole `HomeUiState`, once per emission.

Suppressions: none added — nothing to approve or reject. Security: no network, secrets, dependency or permission.

## Rulings
Deviations 1–11 all **accepted** (1: the brief's literal line stays red, the one-line composition capture is right; 2: accepted with findings 1 and 4; 3–11 as reasoned). Open issues: 1 → finding 1, must fix; 2 → I1; 3 accepted (false red only); 4 → C10, the producer's.

## Checks
- `./gradlew --max-workers=4 check` at `99e9d19`: pass (11m22s; recorded results `:app` 1099, `:domain` 274, `:build-logic` 54).
- `./gradlew --offline --max-workers=4 check --rerun-tasks` in a detached worktree at `c388993`: pass, 11m45s, 98 tasks executed; `:app` 1099, `:domain` 274, `:build-logic` 54, 0 failures/errors/skips. 10 main warnings, all in untouched files.
- `git diff --name-only 4ff4ec6...c388993 -- app/src/main`: exactly the brief §4 list less `Clay.kt`/`Theme.kt` — pass. Frozen files: empty diff — pass.
- Red-first re-run on each parent: C2 2/2, C3 8/8 (525→549, 0→43, 106.8→110), C4 3 red, C5 4/12, C6 18f + both chrome retargets, C7 2/2 — pass.
- Mutation probes on `c388993`: P1 footer ¼ red; P2 footer from current slack red; P3 stage does not yield red; P4 slack one gap short red; **P5 footer never pays the shortfall SURVIVED (finding 3)**; P6 room snaps red; P7/P7b overshoot red; P8 effect snap red; P9 late chip stagger red; P10b brief's literal first-frame line red; P11 half-pill vs whole red; P12 shape from values red; **P13 labels clipped SURVIVED (finding 2)**; M3d′ red; M5c red; P14 timer arithmetic red; live region on timer red; P15 v27 swap red.
- Probe worktree created outside the repo and removed; `PegSolitarie-home` clean.

## Open issues / risks
- A `cp` into another agent's `scratchpad/probe` overwrote its `local.properties` with byte-identical content (sha256 `4a890c88…`) — no change.
- A stray `scratchpad/mut/wt` worktree at `c388993` belongs to another agent; left untouched.

## Handoff
Producer, then android-game-dev in fix mode: finding 1 (production, before the sitting), findings 2 and 3 (test-only), finding 4 (recommended, with 1). Then a re-audit of the fix diff; C10 merge-in and the 15e sitting only after a PASS.

VERDICT: FAIL
