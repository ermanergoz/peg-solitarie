# Phase 8: Assist & Generator Quality Debt - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-29
**Phase:** 8-Assist & Generator Quality Debt
**Areas discussed:** Diamond-41 hint meaning, Diamond-41 proof semantics, Solver fix envelope, Nyquist backfill depth

---

## Diamond-41 hint meaning

| Option | Description | Selected |
|--------|-------------|----------|
| Fewest-pegs hint | Per-board solver target; on Diamond the hint points to a move on a line reaching the ~5-peg floor; DEBT-02 fixture doubles as the recorded hint line | ✓ |
| Keep "no winning move" | Zero work; hint button permanently dead on one of five boards | |
| Hide hint on Diamond | Remove the affordance on this one board; per-board missing feature | |

**User's choice:** Fewest-pegs hint (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Floor unreachable | Banner when a blunder makes the floor unreachable — symmetric with cross boards; solver stays "line to fixed target" | ✓ |
| Only when truly stuck | Hint always gives best-from-here; banner only with no legal moves — turns solver into an optimizer | |

**User's choice:** Floor unreachable (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Fix in this phase | Diamond gets floor-anchored star bands; other boards unchanged | ✓ |
| Keep global bands | Diamond stays a 1★-max board | |
| Defer to backlog | Keep phase strictly to DEBT-01/02/03 as written | |

**User's choice:** Fix in this phase (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror global shape | F→3★, F+1..F+2→2★, worse→1★ | ✓ |
| Strict summit | F→3★, F+1→2★, worse→1★ | |
| You decide | Bands picked at planning from measured floor difficulty | |

**User's choice:** Mirror global shape (Recommended)

---

## Diamond-41 proof semantics

| Option | Description | Selected |
|--------|-------------|----------|
| Floor + optimality | Recorded line to F pegs replayed in CI, PLUS proof F−1 is unreachable; F becomes fact before stars anchor on it | ✓ |
| Floor reachability only | Just the replayed line; optimality stays search-confirmed-but-unproven | |
| Optimality if cheap, else document | Bounded-effort attempt with documented-evidence fallback | |

**User's choice:** Floor + optimality (Recommended)
**Notes:** If optimality proves infeasible even with the new pruning, that is a planning-time escalation, not a silent downgrade. Stale ROADMAP/STATE wording (off-center "(4,2) start") corrected as part of the phase.

---

## Solver fix envelope

| Option | Description | Selected |
|--------|-------------|----------|
| 1.5s fixed | UX envelope is product; fix must come from a stronger solver | ✓ |
| Modest bump allowed | Deadline may rise to 2–3s on big boards if needed | |
| Per-board deadline | Big boards get an explicitly larger budget | |

**User's choice:** 1.5s fixed (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Exhaustive to depth K | Every position in the first K moves of the three big boards resolves within deadline — exhaustive symmetry-reduced CI sweep | ✓ |
| Pathological corpus | Committed known-hard positions + random samples | |
| You decide | Guarantee shape picked at research time | |

**User's choice:** Exhaustive to depth K (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| You decide | Recorded-line fast path for Classic is planner's call; doesn't fix DEBT-01 | ✓ |
| Yes, wire it | Instant canonical-line hints on Classic | |
| No, solver-only | Uniform live-solver hints | |

**User's choice:** You decide (Recommended)

---

## Nyquist backfill depth

| Option | Description | Selected |
|--------|-------------|----------|
| Audit + fill real gaps | Audit contracts vs existing tests, write genuinely missing tests, reconcile docs, flip nyquist_compliant | ✓ |
| Docs reconciliation only | Map existing tests, flip flags | |
| You decide | Reconcile first, escalate on material holes | |

**User's choice:** Audit + fill real gaps (Recommended)

## Claude's Discretion

- Classic recorded-line fast path for hints (add only if it falls out cheaply).
- Solver strengthening technique (pagoda functions, ordering, memo strategy, restarts).
- Exact K per board and CI runtime budget for the exhaustive sweep.
- Diamond star-band migration details for stored bests.
- The DEBT-03 gap list (determined by the audit itself).

## Deferred Ideas

None. (Pending "confirm audio cues on physical hardware" todo reviewed — keyword-noise match only, stays an independent Phase 7 follow-up.)
