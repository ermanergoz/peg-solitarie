# Phase 4: Assist — Hint & Undo - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-02
**Phase:** 4-assist-hint-undo
**Areas discussed:** Hint presentation, Hint scope, Undo behavior, "No winning move" state

---

## Hint presentation

**Q1 — What happens on the board when a winning move is found?**

| Option | Description | Selected |
|--------|-------------|----------|
| Highlight source peg only | Pulse the peg to move; player picks destination | |
| Highlight source + destination | Glow peg A + target hole B, faint A→B arrow; player taps to perform | ✓ |
| Auto-perform the jump | Play the move automatically with animation | |

**Q2 — How to disambiguate from the existing legal-dots "hints" toggle?**

| Option | Description | Selected |
|--------|-------------|----------|
| Rename dots → "Show possible moves", keep "Hint" for winning move | Cleanest separation | ✓ |
| Keep both as-is, distinct controls | Two things both called "hint" | |
| You decide | Claude picks clearest naming | |

**User's choice:** Source+destination highlight; rename dots toggle to "Show possible moves".
**Notes:** Hint should nudge, not autopilot — player still executes the move.

---

## Hint scope

**Q1 — Classic too, or Challenge only?**

| Option | Description | Selected |
|--------|-------------|----------|
| Both Classic and Challenge | Challenge = recorded line → Solver; Classic = Solver only | ✓ |
| Challenge only this phase | Hint only where the recorded line is cheap/guaranteed | |
| You decide | Base on Solver capability | |

**Q2 — Solver budget miss on an actually-solvable position?**

| Option | Description | Selected |
|--------|-------------|----------|
| Say "couldn't find one" — distinct from unsolvable | Softer separate message | |
| Treat budget miss as "no winning move" | Single failure path | ✓ |
| You decide | Tune budget so misses are rare | |

**User's choice:** Both modes; budget miss = "no winning move".
**Notes:** Flagged planning constraint — since Classic is Solver-only and a miss reads as unsolvable, the Solver budget must be high enough that solvable full-size classic positions never hit it.

---

## Undo behavior

**Q1 — Undo depth?**

| Option | Description | Selected |
|--------|-------------|----------|
| Multi-step (repeatable, back to start) | Walk back move-by-move; no redo | ✓ |
| Single undo (last move only) | Revert most recent only | |
| You decide | moveHistory supports either | |

**Q2 — Rescue a Stuck/terminal board?**

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — offer Undo from the stuck state before Results | Back up and keep playing | ✓ |
| Mid-game only; stuck still → Results/Retry | Phase-3 D-10 unchanged | |
| You decide | Fit existing terminal handling | |

**Q3 — Undo effect on timer/score?**

| Option | Description | Selected |
|--------|-------------|----------|
| Timer keeps running; no penalty | Rewind board only; time keeps counting | ✓ |
| You decide | Set during planning | |

**User's choice:** Multi-step repeatable (no redo); rescues Stuck board; timer keeps running, no penalty.
**Notes:** Modifies Phase-3 D-10 — Stuck no longer auto-navigates to Results; shows in-board rescue. Won still → Results.

---

## "No winning move" state

**Q1 — How surfaced?**

| Option | Description | Selected |
|--------|-------------|----------|
| Inline banner on the board | Non-blocking; board stays interactive | ✓ |
| Modal dialog | Interrupts, dims board | |
| You decide | Fit existing overlay patterns | |

**Q2 — Actions offered?**

| Option | Description | Selected |
|--------|-------------|----------|
| Undo + Restart | Spec wording; Home via top bar | |
| Undo + Restart + Home | All three in the state | ✓ |
| You decide | Choose during planning | |

**Q3 — Solver "thinking" feedback?**

| Option | Description | Selected |
|--------|-------------|----------|
| Brief "thinking…" indicator on Hint control | Spinner/disabled while Solver runs | ✓ |
| Nothing special — just reveal when ready | No spinner | |
| You decide | Add only if latency warrants | |

**User's choice:** Inline non-blocking banner; actions Undo · Restart · Home; brief thinking indicator on the Hint control.

---

## Claude's Discretion

- Solver algorithm, memoization, and concrete node/time budget values (subject to the completeness constraint above).
- Exact Hint/Undo button placement and iconography; the precise renamed-toggle label.
- Whether the recorded solution is threaded via GameViewModel directly or a GetHintUseCase.
- Exact AdGateway method signature / reason enum.
- Whether Won ever offers Undo (default: no).

## Deferred Ideas

- Rewarded-ad gating + real AdMob impl — Slice 2 (only AdGateway interface + auto-grant stub built now).
- Redo — out of scope.
- Persisting move history / resume with undo intact — Phase 5 (PERS-03).
- Interstitial "retry between levels" ads — Slice 2.
- Richer hint animation polish — Slice-2 visual pass (POLI-01).
