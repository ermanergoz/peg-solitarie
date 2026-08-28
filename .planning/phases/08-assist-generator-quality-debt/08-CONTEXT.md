# Phase 8: Assist & Generator Quality Debt - Context

**Gathered:** 2026-08-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Pure `:domain` tech-debt payoff, SDK-free, sequenced early to absorb the Play Console / AdMob propagation wait. Three requirements:

- **DEBT-01** — the hint solver returns a real winning move within its deadline on hard early positions of the large Classic boards (European-37, Asymmetric-39, Wiegleb-45) instead of folding to the "no winning move" rescue banner.
- **DEBT-02** — Diamond-41's shipped starting position gets a committed, CI-replayed proof of its solvability claim. **The roadmap/STATE wording for this requirement is stale**: the code ships the canonical center-empty start, and Phase 2 proved (GF(4) colour invariant, confirmed by search) that it can NEVER reduce to one peg — the floor is ~5 pegs. "Proven solvable" is redefined by this discussion as *the fewest-pegs floor F is proven reached and proven optimal* (see decisions).
- **DEBT-03** — the Nyquist validation coverage gaps carried from v1.0 phases 3 and 4 are backfilled and green in `check`.

The phase goal — *"the hint system tells the truth on every shipped board"* — includes Diamond-41, whose hint today can only ever say "no winning move" because the solver targets one peg. This discussion extends the hint's definition of "winning" per board (fewest-pegs floor on Diamond) and re-anchors Diamond's star bands on the proven floor. DEBT-01 and DEBT-02 remain one unit of work: the stronger pruning that fixes the deadline envelope is what makes the Diamond floor proof reachable.

</domain>

<decisions>
## Implementation Decisions

### Diamond-41 hint meaning
- **D-01:** The hint on Diamond-41 targets the **fewest-pegs floor**, not one peg. The solver gains a per-board target peg count; on Diamond, a hint points to a move on a line reaching the floor F. The recorded floor-line fixture from DEBT-02 may double as the hint's recorded line (same recorded-line-first pattern as Challenge).
- **D-02:** The "no winning move" rescue banner on Diamond appears when the **floor becomes unreachable** — symmetric with the cross boards (hint targets the perfect finish; a blunder that forfeits it triggers the banner + rescue-undo). Solver semantics stay "find a line to a fixed target"; only the target count differs per board. No optimizer/"best-from-here" semantics.
- **D-03:** **Per-board star thresholds land in this phase.** Diamond-41 gets star bands anchored on the proven floor F; the other four boards keep the global bands (≤1→3★, ≤3→2★, else 1★) unchanged.
- **D-04:** Diamond's bands **mirror the global shape**: F→3★, F+1..F+2→2★, worse→1★ (same 1/2-wide band feel as the cross boards, re-anchored).

### Diamond-41 proof semantics (DEBT-02 redefined)
- **D-05:** The committed fixture must prove **floor reachability AND optimality**: (a) a recorded move line from the canonical center start to F pegs, replayed through the unchanged `RulesEngine` in CI; (b) a proof that F−1 is unreachable (exhaustive symmetry-reduced search under the phase's stronger pruning, or an invariant argument). Since stars anchor on F, F must be a fact, not a best-known. **Risk accepted knowingly:** if the optimality proof turns out infeasible even with the new pruning, that is a planning-time escalation, not a silent downgrade.
- **D-06:** As part of this phase, correct the stale documentation trail: ROADMAP.md's DEBT-02 wording and STATE.md's carried blocker still describe an "off-center (4,2) start" / one-peg solvability — the code shipped the center start with the GF(4) impossibility documented (`Boards.kt`, `SolutionTest.kt`). Planning docs must match the code's reality.

### Solver fix envelope (DEBT-01)
- **D-07:** The **1.5s hint deadline is fixed** — the UX envelope is part of the product. Truthful hints must come from a stronger solver (real pagoda-function bounds — the current `Pagoda` object is only a mod-3 colour-parity check, not a resource-count prune — better move ordering, memo reuse, or equivalent), never from making the player wait longer. No deadline bump, no per-board deadline.
- **D-08:** The DEBT-01 guarantee is **exhaustive to depth K**: every position reachable within the first K moves (K chosen per board to keep CI feasible, e.g. 4–6) of European-37, Asymmetric-39, and Wiegleb-45 must resolve within the deadline — verified by an exhaustive, symmetry-reduced CI sweep, not a sampled corpus. K and the sweep's CI runtime budget are research/planning outputs.

### Nyquist backfill (DEBT-03)
- **D-09:** **Audit + fill real gaps**: audit the phase 3/4 VALIDATION.md contracts against the tests that actually exist, write tests for any behavior genuinely uncovered, then reconcile the docs and flip `nyquist_compliant: true`. Not a docs-only reconciliation — "green in check" must mean real coverage. (The named Wave-0 test files all exist; the contracts were left as drafts with unmapped tasks.)

### Claude's Discretion
- **Classic recorded-line fast path:** whether Classic hints also use the committed one-peg solutions as an instant recorded-line-first path while the player is on the canonical line. It does not fix DEBT-01 (deviated positions still need the solver); add it only if it falls out cheaply from making the fixtures production-visible for Diamond's hint line.
- **Solver strengthening technique**: which combination of real pagoda functions, move-ordering improvements, memoisation strategy, restart policy etc. achieves the depth-K guarantee — researcher/planner territory.
- **Exact K per board** and the CI runtime budget for the exhaustive sweep.
- **Diamond star-band migration details**: stored bests only improve via the existing upsert-on-better path; whether any stored-stars re-derivation is possible/needed is planner's call (pegs-remaining may not be persisted per best).
- **DEBT-03 gap list**: which behaviors are genuinely uncovered is determined by the audit itself.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & scope
- `.planning/REQUIREMENTS.md` — DEBT-01, DEBT-02, DEBT-03 (this phase's entire requirement set).
- `.planning/ROADMAP.md` — Phase 8 goal + success criteria. **Caveat (D-06):** the DEBT-02 success criterion wording ("proven solvable by a committed recorded-solution fixture") predates the center-start/GF(4) reality; this CONTEXT.md's D-05 is the operative definition.
- `.planning/STATE.md` — carried blockers section still describes the stale "(4,2) start"; corrected by this phase per D-06.
- `.planning/PROJECT.md` — Key Decisions table row "Solver = memoised symmetry-canonical DFS + pagoda pruning, 1.5s deadline" is flagged ⚠️ Revisit — this phase is that revisit.

### Solver & hint code (DEBT-01, D-01/D-02/D-07/D-08)
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt` — the DFS engine: symmetry-canonical memo (`VisitedSet`), inward-jitter deterministic restarts, one-peg target hardcoded as `SOLVED_PEG_COUNT`.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverBudget.kt` — 1.5s `DEFAULT_DEADLINE` (fixed per D-07), 40M `DEFAULT_MAX_NODES`, the SolverBudgetTest 4x-headroom contract.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt` — currently a mod-3 colour-parity necessary condition ONLY (single-peg terminal parity); both its prune strength and its one-peg assumption are touched by this phase.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt` — recorded-line-first → solver-fallback pipeline the per-board target threads through.

### Boards, scoring & fixtures (DEBT-02, D-03/D-04/D-05)
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` — Diamond-41 canonical center start + in-code GF(4) documentation.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt` + `ScoreParams.kt` — global star bands (`stars(pegsRemaining)`) that D-03/D-04 make per-board.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt` — the D-19 replay pattern the Diamond floor fixture extends; documents "no one-peg fixture by design".
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt` + `RecordedSolutions.kt` — fixture generation (`UPDATE_GOLDEN=true`) and decoding infrastructure to reuse.
- `domain/src/test/resources/golden/solutions/` — committed one-peg lines for english33/european37/asymmetric39/wiegleb45.

### Validation backfill (DEBT-03, D-09)
- `.planning/milestones/v1.0-phases/03-challenge-generation-difficulty-curve/03-VALIDATION.md` — draft contract, `nyquist_compliant: false`, unmapped per-task rows.
- `.planning/milestones/v1.0-phases/04-assist-hint-undo/04-VALIDATION.md` — same state for phase 4.
- `.planning/milestones/v1.0-MILESTONE-AUDIT.md` — the audit that recorded phases 3/4 as partial and carried the debt.

### Prior art & proofs
- User's original game (source of truth for rules/scoring): https://github.com/ermanergoz/peg-solitarie — Diamond is scored on fewest pegs remaining; game-over = no legal moves.
- Phase 2 GF(4) finding (memory + `Boards.kt`/`SolutionTest.kt` KDoc): center-start Diamond-41 cannot reach one peg; search-confirmed floor ~5 pegs. Never demand a one-peg solution for the diamond.

### Project standards
- `CLAUDE.md` — no magic numbers (per-board targets/bands become named, scoped constants), detekt/ktlint/purity gates, Gitflow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AuthoringSolver` + `RecordedSolutions` + `BoardCodec` + `UPDATE_GOLDEN` workflow** — the whole fixture-generation/replay pipeline exists; the Diamond floor fixture is an extension (target F pegs instead of 1), not new infrastructure.
- **`SolutionTest` replay pattern** — fold committed line through `RulesEngine`, assert terminal peg count; directly reusable for the floor fixture.
- **`Solver`'s `VisitedSet` + symmetry canonicalization** — the memory-lean memo and D4 symmetry folding are the foundation the stronger pruning builds on.
- **`GetHintUseCase` recorded-line-first guard** — the legality-checked recorded-line path is exactly what a production Diamond floor line (and optionally Classic lines) would flow through.

### Established Patterns
- **Domain purity is CI-enforced** — everything in this phase is pure `:domain` JVM work; no Android, no coroutines (cancellation stays a `shouldCancel` lambda).
- **Golden-master discipline** — fixtures are generated by tooling against real `Boards.*` values, never hand-transcribed; regenerated only under `UPDATE_GOLDEN=true`.
- **`SolverBudgetTest` headroom contract** — the node cap must stay ≥4x the largest observed tractable search; the depth-K sweep will re-measure this corpus.
- **Scoped constants** — per-board targets and star bands follow the `ScoreParams`/`GeneratorConstants` per-scope constants-object convention.

### Integration Points
- `Solver.firstWinningMove` / `WinningLineSearch` — target peg count parameterization (currently `SOLVED_PEG_COUNT = 1` hardcoded); `Pagoda.mayBeSolvable` assumes a single-peg terminal and must respect the per-board target too.
- `GetHintUseCase` → `GameViewModel` — the caller decides the board's target and (optionally) supplies the Diamond recorded line; the `noWinningMove` UiState flag and rescue banner stay unchanged in the UI.
- `ScoreCalculator.stars(pegsRemaining)` — gains a per-board dimension; all call sites (GameViewModel/Results) pass board identity or a board-derived star policy.
- Room `LevelResult` bests — old Diamond bests recorded under the 1★ cap upgrade naturally via the existing upsert-on-better path on next play.

</code_context>

<specifics>
## Specific Ideas

- "Hints tell the truth on every shipped board" now literally includes Diamond-41: the hint's definition of *winning* becomes per-board (one peg on the cross boards, the proven floor F on the diamond), and the rescue banner means the same thing everywhere — *the perfect finish is no longer reachable*.
- The floor F is a **proven fact** before stars anchor on it (D-05): reachability by committed replayed line, optimality by exhausted search or invariant.
- DEBT-01's acceptance is a **universal claim bounded by depth**, not a vibe: exhaustive over all positions in the first K moves of the three big boards, inside the fixed 1.5s deadline.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. (The pending "confirm audio cues on physical hardware" todo matched only on keyword noise (score 0.2) and remains an independent Phase 7 follow-up, untouched by this phase.)

</deferred>

---

*Phase: 8-Assist & Generator Quality Debt*
*Context gathered: 2026-08-29*
