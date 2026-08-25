# Phase 3: Challenge Generation & Difficulty Curve - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-01
**Phase:** 3-challenge-generation-difficulty-curve
**Areas discussed:** Progression flow, Difficulty curve feel, Board shapes, Stuck & scoring

---

## Progression flow — Advance on solve

| Option | Description | Selected |
|--------|-------------|----------|
| Results → tap Next level | Reuse Phase-2 Results screen + a "Next level" button to N+1 | ✓ |
| Auto-advance instantly | Skip Results, jump into N+1 for a fast loop | |
| Results, auto-continue after delay | Show Results briefly then auto-slide into N+1 | |

**User's choice:** Results → tap Next level
**Notes:** Challenge is the rightful home of the "next board/level" action removed from Classic in quick task 260701-n9t.

## Progression flow — Unlock rule

| Option | Description | Selected |
|--------|-------------|----------|
| Must solve to advance | Only a 1-peg solve unlocks N+1; stuck → retry same board | ✓ |
| Any finish advances | Any terminal state advances; stars reflect quality | |
| Must solve, but can skip | Solve unlocks; explicit Skip escape hatch when stuck | |

**User's choice:** Must solve to advance
**Notes:** Ladder-of-mastery; fair because levels are guaranteed solvable.

## Difficulty curve feel — Ramp shape

| Option | Description | Selected |
|--------|-------------|----------|
| Gentle start, steady climb | Easy first ~10, then smooth continuous increase forever | ✓ |
| Quick ramp to a plateau | Ramp fast over ~20 levels then increase slowly | |
| Gentle start, then hard wall tiers | Sharp steps at milestones (chapters/seasons) | |

**User's choice:** Gentle start, steady climb

## Difficulty curve feel — Primary lever

| Option | Description | Selected |
|--------|-------------|----------|
| Solution length first | Longer winning lines before bigger boards; "trickier not bigger" | ✓ |
| Board size first | Grow footprint as the main lever | |
| Pattern & density first | Vary starting empty-pattern/density before size | |

**User's choice:** Solution length first
**Notes:** Maps to `tier.solutionLength` as the main knob; footprint grows later/slower.

## Board shapes — Footprint

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse Classic canonical shapes | Generate onto known-good square footprints | |
| Freeform within a bounding grid | Arbitrary connected shapes in NxN | |
| Canonical early, freeform later | Canonical for early levels, freeform as a later variety lever | ✓ |

**User's choice:** Canonical early, freeform later
**Notes:** Freeform needs validity gates (connectivity, no isolated cells), ≤64 in-play cells (Long bitmask), square-grid geometry; introduction level is a playtested knob.

## Stuck & scoring — Stuck handling

| Option | Description | Selected |
|--------|-------------|----------|
| Stuck → Results → Retry | Dead-end goes to Results (no Next level); Retry + Home | ✓ |
| In-place 'stuck' prompt | In-game banner/dialog with Retry/Home on the board | |
| Retry only, no dead-end screen | Auto-reset the same board on stuck | |

**User's choice:** Stuck → Results → Retry
**Notes:** Hints/undo that would rescue a stuck board are Phase 4.

## Stuck & scoring — Scoring model

| Option | Description | Selected |
|--------|-------------|----------|
| Same as Classic | Reuse ScoreCalculator verbatim | |
| Level-scaled score | Keep stars (1-peg = 3★); scale numeric score by level/difficulty | ✓ |
| Stars on speed/efficiency | Re-base stars on time/move-efficiency vs recorded optimal | |

**User's choice:** Level-scaled score
**Notes:** Small additive change over Phase-2 ScoreCalculator; stars stay near-constant since solving is required, so the scaled numeric score is what differentiates ladder progress.

---

## Claude's Discretion

- Show "Level N" in the Challenge gameplay top bar (default, unobjected).
- Challenge starts at level 1 each session this phase; resume-from-highest is Phase 5 (CHAL-05/PERS-02).
- Exact difficulty knob values, the freeform-introduction level, and freeform symmetry preferences — implementation + playtest.
- Results `mode`/`showNextLevel` flag vs a separate Challenge-results route — planner's implementation choice.

## Deferred Ideas

- Continue-from-highest-level / resume (CHAL-05, PERS-02) → Phase 5.
- Hints + undo (ASST-01..03) → Phase 4 (what would rescue a stuck board).
- Leaderboards / daily challenges → Slice 2.
- Re-basing stars on speed/efficiency → not this phase.
