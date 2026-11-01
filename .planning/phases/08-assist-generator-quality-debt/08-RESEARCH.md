# Phase 8: Assist & Generator Quality Debt - Research

**Researched:** 2026-08-29
**Domain:** Combinatorial search (peg-solitaire solving), invariant proofs, golden-fixture test architecture — all pure `:domain` Kotlin
**Confidence:** HIGH on the measured algorithmic findings; MEDIUM on the recommended production architecture (it revises two CONTEXT assumptions and needs planner/user acknowledgement)

> **Two CONTEXT assumptions are contradicted by measurement in this session. Read the Summary before planning.**

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Diamond-41 hint meaning**
- **D-01:** The hint on Diamond-41 targets the **fewest-pegs floor**, not one peg. The solver gains a per-board target peg count; on Diamond, a hint points to a move on a line reaching the floor F. The recorded floor-line fixture from DEBT-02 may double as the hint's recorded line (same recorded-line-first pattern as Challenge).
- **D-02:** The "no winning move" rescue banner on Diamond appears when the **floor becomes unreachable** — symmetric with the cross boards (hint targets the perfect finish; a blunder that forfeits it triggers the banner + rescue-undo). Solver semantics stay "find a line to a fixed target"; only the target count differs per board. No optimizer/"best-from-here" semantics.
- **D-03:** **Per-board star thresholds land in this phase.** Diamond-41 gets star bands anchored on the proven floor F; the other four boards keep the global bands (≤1→3★, ≤3→2★, else 1★) unchanged.
- **D-04:** Diamond's bands **mirror the global shape**: F→3★, F+1..F+2→2★, worse→1★ (same 1/2-wide band feel as the cross boards, re-anchored).

**Diamond-41 proof semantics (DEBT-02 redefined)**
- **D-05:** The committed fixture must prove **floor reachability AND optimality**: (a) a recorded move line from the canonical center start to F pegs, replayed through the unchanged `RulesEngine` in CI; (b) a proof that F−1 is unreachable (exhaustive symmetry-reduced search under the phase's stronger pruning, or an invariant argument). Since stars anchor on F, F must be a fact, not a best-known. **Risk accepted knowingly:** if the optimality proof turns out infeasible even with the new pruning, that is a planning-time escalation, not a silent downgrade.
- **D-06:** As part of this phase, correct the stale documentation trail: ROADMAP.md's DEBT-02 wording and STATE.md's carried blocker still describe an "off-center (4,2) start" / one-peg solvability — the code shipped the center start with the GF(4) impossibility documented (`Boards.kt`, `SolutionTest.kt`). Planning docs must match the code's reality.

**Solver fix envelope (DEBT-01)**
- **D-07:** The **1.5s hint deadline is fixed** — the UX envelope is part of the product. Truthful hints must come from a stronger solver (real pagoda-function bounds — the current `Pagoda` object is only a mod-3 colour-parity check, not a resource-count prune — better move ordering, memo reuse, or equivalent), never from making the player wait longer. No deadline bump, no per-board deadline.
- **D-08:** The DEBT-01 guarantee is **exhaustive to depth K**: every position reachable within the first K moves (K chosen per board to keep CI feasible, e.g. 4–6) of European-37, Asymmetric-39, and Wiegleb-45 must resolve within the deadline — verified by an exhaustive, symmetry-reduced CI sweep, not a sampled corpus. K and the sweep's CI runtime budget are research/planning outputs.

**Nyquist backfill (DEBT-03)**
- **D-09:** **Audit + fill real gaps**: audit the phase 3/4 VALIDATION.md contracts against the tests that actually exist, write tests for any behavior genuinely uncovered, then reconcile the docs and flip `nyquist_compliant: true`. Not a docs-only reconciliation — "green in check" must mean real coverage. (The named Wave-0 test files all exist; the contracts were left as drafts with unmapped tasks.)

### Claude's Discretion
- **Classic recorded-line fast path:** whether Classic hints also use the committed one-peg solutions as an instant recorded-line-first path while the player is on the canonical line. It does not fix DEBT-01 (deviated positions still need the solver); add it only if it falls out cheaply from making the fixtures production-visible for Diamond's hint line.
- **Solver strengthening technique**: which combination of real pagoda functions, move-ordering improvements, memoisation strategy, restart policy etc. achieves the depth-K guarantee — researcher/planner territory.
- **Exact K per board** and the CI runtime budget for the exhaustive sweep.
- **Diamond star-band migration details**: stored bests only improve via the existing upsert-on-better path; whether any stored-stars re-derivation is possible/needed is planner's call (pegs-remaining may not be persisted per best).
- **DEBT-03 gap list**: which behaviors are genuinely uncovered is determined by the audit itself.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope. (The pending "confirm audio cues on physical hardware" todo matched only on keyword noise (score 0.2) and remains an independent Phase 7 follow-up, untouched by this phase.)
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| **DEBT-01** | The hint solver returns a winning move within its deadline for hard early positions on the large Classic boards, instead of folding to "no winning move" | §Measured Evidence (solver throughput + restart policy), §Pattern 2 (depth-K opening book), §Pattern 3 (bitboard search core). **Escalation:** measurement shows Wiegleb-45 is NOT solvable in 1.5s by solver strengthening alone — the book is load-bearing, not optional. |
| **DEBT-02** | Diamond-41's shipped starting position is proven solvable by a committed recorded-solution fixture replayed through the rules engine | §Measured Evidence (**F = 2, not ~5**), §Pattern 1 (per-board target on `BoardSpec`), §Pattern 4 (beam-search authoring tool). **Both halves of D-05 are now cheap:** reachability = one committed line; optimality = the *existing* GF(4) argument, since F−1 = 1. |
| **DEBT-03** | The validation coverage gaps carried from phases 3 and 4 are backfilled | §DEBT-03 Audit — all named Wave-0 files exist; the gap is contract reconciliation plus a small number of genuinely unmapped rows. |
</phase_requirements>

---

## Summary

This phase's technical content is a single question asked three ways: *how strong does a peg-solitaire search have to be, and what can be proven rather than searched?* I answered it empirically rather than from theory — building a tuned bitboard reference solver, an exhaustive depth-K sweep harness, a GF(4)/pagoda-LP invariant prover, and a beam search, and running them against the project's exact five board footprints. Three measurements overturn planning assumptions that are currently written into CONTEXT.md, ROADMAP.md, and the `Boards.kt` KDoc.

**First: Diamond-41's fewest-pegs floor is F = 2, not "~5".** A width-400 000 mobility-ordered beam search reaches a two-peg dead position from the canonical center start [VERIFIED: measured this session]. The "~5" in `Boards.kt`/`AuthoringSolver` KDoc and carried into CONTEXT D-05 came from randomized rollouts, which I reproduced (50 000 rollouts also plateau at 5) — rollouts are simply too weak a search to find the floor. This is a *large* simplification of DEBT-02, because **F−1 = 1, and one peg is already proven unreachable by the GF(4) colour invariant.** I independently confirmed the proof by exhaustive enumeration: of the 41 single-peg positions, **zero** satisfy the invariant the center start pins. So D-05(b), the optimality half everyone feared, needs no new search and no new pruning — it is the argument already shipped in `Pagoda.kt` and documented in `Boards.kt`. D-05(a), reachability, is one committed golden line generated by the beam search. DEBT-02 collapses from "possibly infeasible research project" to "ordinary golden-fixture task."

**Second: DEBT-01 as literally specified in D-08 is not reachable for Wiegleb-45 by making the solver stronger.** I built the strengthened solver D-07 asks for — bitboard move tables, chunked symmetry canonicalisation, zero per-node allocation — and measured **~15M nodes/s versus the shipped solver's ~0.27M nodes/s, a ~55× speedup** [VERIFIED: measured]. That is enough to bring European-37 and Asymmetric-39's full starts from 7.7s/timeout down to 33–235 ms. It is *not* enough for Wiegleb-45: an exhaustive depth-4 sweep with the fixed 1.5s deadline resolved **0 of 76** Wiegleb-45 positions, including the depth-0 start, which is known solvable (its one-peg line is a committed fixture). Even 865 million nodes across 400 restarts over 57 seconds failed. The honest conclusion is that no plausible constant-factor or pruning improvement closes a 40 000× gap. **The fix that does work is a committed depth-K opening book**: enumerate every position reachable in the first K moves (only 672 positions across all four cross boards at K=4), compute a provably-correct first winning move for each at authoring time with unbounded budget, and commit it as a golden fixture the hint serves instantly. This is the CONTEXT's own "recorded-line fast path" discretion item, generalised from one line to a frontier — and it makes DEBT-01 a *lookup guarantee* rather than a timing gamble.

**Third: real pagoda functions — D-07's leading suggestion — are not the lever.** I formulated the resource-count test as a linear program (maximise `Σp(target) − Σp(start)` subject to `p(a)+p(b) ≥ p(c)` over every jump triple) and ran it over every low-peg Diamond-41 target. It eliminates only about 40% of the candidates the GF(4) invariant leaves standing [VERIFIED: measured]. Pagoda pruning is real and sound, but on these boards the dominant costs are constant-factor throughput and a restart policy bug — **the shipped `Solver` gives its first attempt the entire 40M-node budget, so the 1.5s deadline always fires during attempt 0 and `MAX_ATTEMPTS = 40` is unreachable dead code in production** [VERIFIED: code read + measured]. Fixing that alone is worth more than pagoda.

**Primary recommendation:** Rebuild `Solver`'s inner loop on precomputed bitboard move tables with a small per-attempt node cap (~200k) so restarts actually happen inside the deadline; add a `minimumPegs` field to `BoardSpec` (default 1) to thread the per-board target through Solver/Pagoda/ScoreCalculator with no call-site churn; and commit two new golden fixtures — a Diamond-41 line to 2 pegs, and a depth-K opening book for the cross boards — both generated by a beam-search authoring tool under `UPDATE_GOLDEN=true`. Treat "Wiegleb-45 cannot meet D-08 by solver strength" as a planning-time escalation to surface to the user, exactly as D-05 anticipated for the Diamond.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Per-board win target (1 peg vs. floor F) | `:domain` model (`BoardSpec`) | — | The target is a property of the board footprint, not of the UI or data layer. Putting it on `BoardSpec` means `Solver`, `Pagoda` and `ScoreCalculator` all read it from the `Board` they already receive — no signature churn, no board-identity string leaking into `:domain`. |
| Winning-move search | `:domain` solver | — | Already correct. Stays pure Kotlin, no coroutines (`shouldCancel` lambda). |
| Depth-K opening book + Diamond floor line (data) | `:domain` **main** resources | `:domain` test (authoring) | Book lookup must run in production; book *generation* must not ship. Mirrors the existing `AuthoringSolver` (test) / `RecordedSolutions` (test) split, but the decoder moves to `main`. |
| Book/fixture generation | `:domain` **test** source set | — | `UPDATE_GOLDEN=true` only. Unbounded compute, large heap, never on a shipped classpath — the established `AuthoringSolver` discipline. |
| Star band selection | `:domain` scoring | `:app` (passes the `Board`/spec) | D-03/D-04 make stars a function of `(pegsRemaining, board)`. `ScoreCalculator` stays stateless and level-agnostic. |
| Persisted resume of a per-board target | `:app` data (`SnapshotMapper`) | — | `SnapshotMapper` rebuilds `BoardSpec` from the blob; a new spec field is a serialization-compat concern owned by the data layer. **See Pitfall 3.** |
| Hint orchestration / deadline | `:app` (`GameViewModel`) | — | Unchanged. The deadline stays a `shouldCancel` lambda built in the ViewModel. |
| Nyquist contract reconciliation | `.planning/` docs + existing test files | — | DEBT-03 is documentation + a small number of real tests; no production code. |

---

## Standard Stack

**This phase adds no new dependencies.** It is pure `:domain` Kotlin plus the existing JVM test stack. That is a deliberate finding, not an omission: every technique recommended below (bitboard search, beam search, GF(4) invariant, golden fixtures) is ~200 lines of plain Kotlin over primitives, and the project's own `AuthoringSolver`/`RecordedSolutions`/`BoardCodec` already provide the fixture pipeline.

### Core (already present — versions from `gradle/libs.versions.toml` via CLAUDE.md stack)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin/JVM | 2.3.20 | `:domain` module language | Pinned project-wide; `:domain` uses the `kotlin-jvm` convention plugin with zero Android deps [VERIFIED: `domain/build.gradle.kts`] |
| JUnit4 | 4.13.2 | Test runner | `domain/build.gradle.kts` declares only `libs.junit` + `libs.truth` [VERIFIED: file read] |
| Google Truth | 1.4.x | Assertions | Same |

### Supporting (existing project assets to reuse, not rebuild)

| Asset | Location | Purpose | When to Use |
|-------|----------|---------|-------------|
| `AuthoringSolver` | `domain/src/test/.../golden/solutions/` | `UPDATE_GOLDEN` fixture generation | Extend with a beam-search mode + a per-board target; do **not** replace |
| `RecordedSolutions` + `BoardCodec` | same package (test) | Fixture encode/decode | The book decoder is a `main`-source sibling of this |
| `VisitedSet` (open-addressing `long[]` memo) | duplicated in `Solver.kt` and `AuthoringSolver.kt` | Memory-lean transposition table | Already correct (~16 B/entry). Keep. |
| `BoardCanonical` | `domain/src/main/.../generator/` | Symmetry permutations, lifted from `AuthoringSolver` | A third copy of `symmetryPermutations` already exists here — consolidation opportunity |
| `Test { maxHeapSize = "4g" }` under `UPDATE_GOLDEN` | `domain/build.gradle.kts` | Authoring heap | Beam search at width 400k needs more — raise to 8g under the same flag |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Committed depth-K opening book | Stronger solver only (D-07's literal reading) | **Measured to fail on Wiegleb-45 by ~4 orders of magnitude.** Not a viable path. |
| Committed depth-K opening book | Raising the 1.5s deadline | Forbidden by D-07. |
| Beam search authoring tool | Existing jittered-DFS `AuthoringSolver` | DFS burned 865M nodes / 57s on Wiegleb-45 without solving it; beam search solved it to one peg at (4,4). DFS is the wrong tool for full-footprint starts. |
| `minimumPegs` field on `BoardSpec` | A `StarPolicy`/`TargetPolicy` passed through every call site | More plumbing (`Solver`, `Pagoda`, `ScoreCalculator`, `GameViewModel`, `ResultsViewModel` signatures), no purity benefit. Field wins — but see Pitfall 3 on serialization. |
| Real pagoda functions (LP-derived) | GF(4) invariant only (status quo) | Measured: pagoda kills only ~40% of the residual candidates GF(4) leaves. Worth adding *if cheap*, but it is not the critical path. Do not spend the phase's budget here. |
| Integer/LP pagoda generation at authoring time | Hand-derived pagoda catalogue | Would require an LP solver dependency in the test source set. Given the low measured payoff, **skip entirely.** |

**Installation:** none required.

---

## Package Legitimacy Audit

**Not applicable — this phase installs no external packages.**

Every recommended technique is implemented in first-party Kotlin against the existing `:domain` primitives. `domain/build.gradle.kts` declares exactly two test dependencies (`libs.junit`, `libs.truth`), both already pinned in the project version catalogue and already in use across 19 existing `:domain` test files. No `npm`/`pip`/`cargo`/Maven coordinate is introduced, so there is no slopsquatting surface and no `checkpoint:human-verify` install gate is needed.

**Packages removed due to slopcheck [SLOP] verdict:** none — no packages proposed.
**Packages flagged as suspicious [SUS]:** none.

If the planner later decides to pull in an LP solver for pagoda generation (explicitly **not** recommended above), that decision re-opens this section and must run the full legitimacy gate.

---

## Measured Evidence

> All numbers in this section were produced this session on the project's exact board footprints (regenerated from `Boards.kt`'s layout rules), on this machine (Apple Silicon, OpenJDK 17.0.15). They are reproducible from the scratchpad harnesses. Tagged **[VERIFIED: measured this session]**.

### Board facts (independent re-derivation of `Boards.kt`)

| Board | Holes | Start pegs | Symmetry order | Jump triples | GF(4) `(r+c)` parities | GF(4) `(r−c)` parities | One peg reachable? |
|-------|-------|-----------|----------------|--------------|------------------------|------------------------|--------------------|
| English-33 | 33 | 32 | 8 | 76 | `[0,1,1]` | `[0,1,1]` | yes |
| European-37 | 37 | 36 | 8 | 92 | `[1,0,1]` | `[1,1,0]` | yes |
| Asymmetric-39 | 39 | 38 | **2** | 92 | `[1,0,1]` | `[1,0,1]` | yes |
| **Diamond-41** | 41 | 40 | 8 | 100 | `[0,0,0]` → **GF(4) value 0** | `[0,0,0]` → **GF(4) value 0** | **NO — proven** |
| Wiegleb-45 | 45 | 44 | 8 | 108 | `[1,1,0]` | `[0,1,1]` | yes |

The Diamond's start has GF(4) invariant zero under *both* colourings; every single-peg position has a nonzero invariant (it is a power of ω). Exhaustive check over all 41 single-peg positions: **0 satisfy the invariant.** This is the D-05(b) optimality proof, already implemented in `Pagoda.kt` and documented in `Boards.kt`. It is corroborated independently by [MarkusThill/solitaire-diamond](https://github.com/MarkusThill/solitaire-diamond) — same 41-hole diamond, same orthogonal-only jumps — which states the center-empty configuration cannot be solved and an off-center start is required.

Note Asymmetric-39's symmetry order is **2**, not 8 — it gets a 2× memo reduction where the others get 8×. Relevant to any node-budget headroom assertion.

### Canonical positions reachable within the first K moves

| Board | d0 | d1 | d2 | d3 | d4 | d5 | d6 | Σ(≤4) | Σ(≤6) |
|-------|----|----|----|----|----|----|----|-------|-------|
| English-33 | 1 | 1 | 2 | 8 | 39 | 171 | 719 | **51** | 941 |
| European-37 | 1 | 3 | 10 | 47 | 251 | 1 248 | 5 915 | **312** | 7 475 |
| Asymmetric-39 | 1 | 2 | 7 | 35 | 188 | 907 | 4 150 | **233** | 5 290 |
| Diamond-41 | 1 | 1 | 4 | 17 | 84 | 426 | 2 073 | 107 | 2 606 |
| Wiegleb-45 | 1 | 1 | 3 | 11 | 60 | 297 | 1 428 | **76** | 1 801 |

(English-33's row reproduces the published depth table for the English central game — an external cross-check that the enumeration is correct.)

**Consequence:** the D-08 exhaustive frontier is *tiny*. K=4 over the three DEBT-01 boards is 621 positions; K=6 is 14 566. This is small enough to enumerate in a CI test in milliseconds — the cost is entirely in *deciding* each position.

### Solver throughput: shipped vs. tuned

| Implementation | Node rate | Source |
|----------------|-----------|--------|
| Shipped Kotlin `Solver` | **~0.27M nodes/s** | `SolverBudget.kt` KDoc, measured in v1.0 phase 04-02 [CITED: in-repo] |
| Tuned bitboard reference (this session) | **~15M nodes/s** (range 8.7–20.9M) | [VERIFIED: measured] |

The ~55× gap is pure constant factor. The shipped inner loop, per node, allocates a `List<Move>` of data-class objects, calls `sortedByDescending` with a fresh `Random.nextDouble()` per move (boxing `Double`s and allocating a comparator), copies a `Board` data class per move, runs `Pagoda` over all `spec.inPlay` with a `Math.floorMod` and a lambda call per cell, and canonicalises by iterating ~41 bits × 8 permutations. The reference replaces all of that with: precomputed `needMask`/`toMask`/`xorMask` `long[]` tables (a move is one `and`/`and`/`xor`), 8×6×256 precomputed byte-chunk permutation tables for canonicalisation, and `Long.bitCount` over precomputed colour-class masks for the parity check.

### Full-footprint starts under the tuned solver

Per-node randomised inward ordering (matching shipped `Solver` semantics):

| Board | best config | time | outcome |
|-------|-------------|------|---------|
| English-33 | single attempt | **7 ms** | solved |
| European-37 | cap 100k, 2 attempts | **33 ms** | solved |
| Asymmetric-39 | cap 500k, 3 attempts | **296 ms** | solved |
| **Wiegleb-45** | 400 attempts × 500k cap, 865M nodes | **56 835 ms** | **NOT solved** |

Per-attempt static ordering (cheaper per node) reaches European-37 in 179 ms and Asymmetric-39 in 59 ms, and also never solves Wiegleb-45 (244M nodes / 24.9 s in one attempt).

### The restart-policy defect

`Solver.searchWithRestarts` constructs each attempt with `budget.maxNodes = 40_000_000`. In production the 1.5 s deadline fires via `shouldCancel` during attempt 0, which sets `cancelled` and breaks the restart loop. **`MAX_ATTEMPTS = 40` is therefore unreachable in production** — the shipped solver performs exactly one unlucky dive and gives up [VERIFIED: `Solver.kt` lines 81–98 + measured behaviour]. Peg-solitaire DFS runtime is heavy-tailed (the class KDoc even documents this: "a SINGLE fixed move ordering can be 'unlucky'"), and the textbook response to heavy-tailed runtimes is many short restarts, not one long one. My measurements confirm it: at cap 100k European-37 solves in 33 ms across 2 attempts, versus 172 ms for the single unbounded attempt.

### D-08 exhaustive depth-4 sweep at the fixed 1.5 s deadline

Tuned solver, per-attempt cap 200k, restarting until the deadline:

| Board | positions (≤ d4) | resolved | **unresolved** | sweep wall time |
|-------|------------------|----------|----------------|-----------------|
| English-33 | 51 | 47 | 4 | 10.9 s |
| European-37 | 312 | 255 | 57 | 158.6 s |
| Asymmetric-39 | 233 | 207 | 26 | 73.2 s |
| **Wiegleb-45** | 76 | **0** | **76** | 114.0 s |

Caveat: this harness cannot distinguish "genuinely unwinnable" from "deadline miss", so some of European-37's 57 and Asymmetric-39's 26 are legitimate early blunders where "no winning move" is the *correct* answer. **Wiegleb-45 admits no such reading** — depth 0 is the shipped start whose one-peg line is a committed fixture, so all 76 are deadline misses. D-08 is unreachable for Wiegleb-45 by solver strength.

Also note the sweep wall times: even where the solver succeeds, a K=4 sweep costs ~6 minutes across four boards, dominated by deadline-burning positions. A K=6 sweep would be ~20× that. **A depth-K sweep is not a per-commit gate.** See §Validation Architecture.

### Diamond-41 floor

| Method | Best terminal peg count |
|--------|------------------------|
| 50 000 random rollouts (reproduces the "~5" in `Boards.kt` KDoc) | 5 |
| Width-400 000 mobility-ordered beam search | **2** |

Witness (canonical representative): pegs at **(2,6)** and **(5,3)**, reached in 38 moves. Both cells satisfy `(r+c) ≡ 2 (mod 3)` and `(r−c) ≡ 2 (mod 3)` — i.e. the two-peg terminal has GF(4) invariant zero under both colourings, exactly matching the start. Internally consistent.

**F = 2, and F − 1 = 1 is proven impossible. F is therefore an exact, proven fact with no further compute.**

### Pagoda / resource-count LP power (D-07's leading hypothesis)

For each candidate low-peg Diamond-41 target, I solved `max Σ_T p − Σ_S p` s.t. `p(a)+p(b) ≥ p(c)` for all 100 jump triples, `−1 ≤ p ≤ 1` (HiGHS via SciPy). A positive optimum is a certificate of unreachability.

| Target pegs | GF(4)-surviving canonical candidates | still unproven after pagoda LP | pagoda kill rate |
|-------------|--------------------------------------|-------------------------------|------------------|
| 1 | **0** | 0 | GF(4) alone suffices |
| 2 | 14 | 8 | 43% |
| 3 | 89 | 50 | 44% |
| 4 | 868 | 513 | 41% |

Pagoda functions are sound and would add *some* in-search pruning, but they do not decide these boards. This measurement is the evidentiary basis for de-prioritising D-07's "real pagoda-function bounds" suggestion relative to throughput, restart policy, and the opening book.

### Beam search as the authoring engine

| Board | target | result |
|-------|--------|--------|
| Diamond-41 | fewest pegs | **2 pegs** (the floor) |
| Wiegleb-45 | one peg | **1 peg at (4,4)** — the classic Wiegleb central game |

Width 400 000, ~8 GB heap, single-digit minutes. The same search that the jittered DFS could not complete in 57 s and 865M nodes. Beam search is *incomplete* (it can miss a solution and can never prove unsolvability), which is why it belongs in the authoring tool, not the production solver.

---

## Architecture Patterns

### System Architecture Diagram

```
                       PLAYER TAPS "HINT"
                              |
                              v
            +----------------------------------+
            |  GameViewModel (:app)            |
            |  - cancels any in-flight job     |
            |  - solverThinking = true         |
            |  - builds shouldCancel{deadline} |  <-- 1.5s, UNCHANGED (D-07)
            +----------------------------------+
                              | board, recordedLine?, movesPlayed
                              v
            +----------------------------------+
            |  GetHintUseCase (:domain)        |
            +----------------------------------+
                              |
       +----------------------+----------------------+---------------------+
       |                      |                      |                     |
       v                      v                      v                     v
 [1] Challenge          [2] OPENING BOOK       [3] Recorded line     [4] Solver
 recorded line          lookup (NEW)           fast path (NEW,        fallback
 (existing)                                    discretionary)
       |                      |                      |                     |
 solution[movesPlayed]  book[board.pegs]      classicLine[played]    bitboard DFS
 if legal               -> move | NO_WIN       if legal              w/ restarts
       |                      |                      |                     |
       +----------------------+----------------------+---------------------+
                              |
                              v
                    Move?  (null => "no winning move")
                              |
                              v
            +----------------------------------+
            |  GameUiState.noWinningMove       |
            |  -> rescue banner + undo         |  <-- UI UNCHANGED
            +----------------------------------+

  TARGET RESOLUTION (threaded everywhere, D-01/D-02/D-03):
      Board -> BoardSpec.minimumPegs   (1 for cross boards, 2 for Diamond-41)
        consumed by: Solver goal test | Pagoda terminal parity | ScoreCalculator bands

  AUTHORING TIME ONLY (UPDATE_GOLDEN=true, :domain test source set):
      Boards.* --> BeamSearchAuthoring --> golden/solutions/diamond41.txt
                                       --> golden/book/{board}-K{k}.txt
      (never on a shipped classpath; CI only REPLAYS/LOOKS UP)
```

Entry is the hint tap; the four resolution paths are tried in strict priority order, each cheaper than the next; the book is what converts DEBT-01 from a timing gamble into a lookup. The 1.5 s deadline governs only path [4].

### Recommended Project Structure

```
domain/src/main/kotlin/.../domain/
├── model/
│   └── BoardSpec.kt              # + minimumPegs: Int = 1   (the per-board target)
├── solver/
│   ├── Solver.kt                 # bitboard core + small per-attempt cap
│   ├── SolverTables.kt           # NEW: per-spec precomputed move/symmetry tables (cached)
│   ├── SolverBudget.kt           # + perAttemptMaxNodes; DEFAULT_DEADLINE unchanged
│   └── Pagoda.kt                 # generalised to minimumPegs
├── scoring/
│   ├── ScoreCalculator.kt        # stars(pegsRemaining, spec) — per-board bands
│   └── ScoreParams.kt            # + DiamondStarParams (scoped constants, QUAL-03)
└── assist/
    ├── OpeningBook.kt            # NEW (main): decode + lookup
    └── RecordedLines.kt          # NEW (main): Classic one-peg lines + Diamond floor line

domain/src/main/resources/golden/          # NEW — production-visible fixtures
├── solutions/{english33,european37,asymmetric39,wiegleb45,diamond41}.txt
└── book/{european37,asymmetric39,wiegleb45}-K{k}.txt

domain/src/test/kotlin/.../golden/solutions/
├── AuthoringSolver.kt            # + beam-search mode, + per-board target
├── BeamSearchAuthoring.kt        # NEW: width-parameterised beam search
├── OpeningBookAuthoring.kt       # NEW: enumerate depth<=K, solve each unbounded, emit
└── SolutionTest.kt               # + diamond41 floor replay, + book integrity test
```

### Pattern 1: Per-board target as a `BoardSpec` field

**What:** Add `val minimumPegs: Int = 1` to `BoardSpec`. `Boards.Diamond41` passes `2`; everything else defaults.

**When to use:** This is the single change that satisfies D-01, D-02, D-03 and D-04 without touching a single call-site signature — `Solver.firstWinningMove(board, …)` already receives the `Board`, and `Board.spec` is right there.

```kotlin
// domain/src/main/kotlin/.../model/BoardSpec.kt
class BoardSpec(
    val inPlay: List<Position>,
    val target: Position?,
    /**
     * The fewest pegs any line from this footprint's canonical start can reach — the
     * board's "perfect finish" (D-01). 1 for every cross board; 2 for Diamond-41,
     * whose GF(4) colour invariant forbids a single peg and whose floor of 2 is
     * proven (reachability by the committed golden line, optimality because F-1 = 1
     * is exactly what the invariant excludes). Freeform Challenge footprints are
     * solvable-by-construction to one peg, so the default is correct for them.
     */
    val minimumPegs: Int = MINIMUM_PEGS_DEFAULT,
) { /* unchanged body */ }
```

```kotlin
// domain/src/main/kotlin/.../solver/Solver.kt — goal test becomes target-aware
fun firstWinningMove(board: Board, budget: SolverBudget, shouldCancel: () -> Boolean = { false }): Move? {
    val target = board.spec.minimumPegs
    if (board.pegCount <= target || !Pagoda.mayBeSolvable(board)) { lastVisited = 0; return null }
    return searchWithRestarts(board, budget, shouldCancel)
}
```

**Anti-pattern:** do *not* key the target off a Classic level id string. `:domain` must not learn about `ClassicLevelIds`; that would invert the dependency the `kotlin-jvm`-only `domain/build.gradle.kts` exists to enforce.

### Pattern 2: Depth-K opening book (the DEBT-01 mechanism)

**What:** A committed map from board bitmask → first winning move (or an explicit "no winning move" marker), covering every position reachable within K moves of a Classic start.

**When to use:** This is what makes D-08's *universal* claim provable. A book entry is generated with unbounded compute and verified by replaying a complete winning line through `RulesEngine`, so a book hit is correct by construction — the deadline is irrelevant to it.

**Sizing** [VERIFIED: measured]: K=4 is 621 entries across European-37 / Asymmetric-39 / Wiegleb-45 (76 of them Wiegleb). Even un-canonicalised (×8 for the symmetry group) that is ~5 000 entries ≈ 100 KB of text. K=6 is 14 566 canonical / ~115 000 raw entries — meaningful APK weight and a symmetry-inverse move mapping. **Recommend K=4 with raw (non-canonical) masks**: it avoids the symmetry-inverse machinery entirely, is provably complete, and is ~100 KB.

```kotlin
// domain/src/main/kotlin/.../assist/OpeningBook.kt (sketch)
/**
 * Committed depth-K first-winning-move table for the Classic boards (DEBT-01).
 *
 * Each line is `<pegsMaskHex> <fromRow,fromCol>-<toRow,toCol>` or `<pegsMaskHex> -`
 * for a position with no winning line. Entries are GENERATED by
 * `OpeningBookAuthoring` under `UPDATE_GOLDEN=true`, where each move is validated by
 * replaying a full winning line through the unchanged `RulesEngine` — so a book hit is
 * a proof, not a guess, and needs no deadline. Positions deeper than K fall through to
 * the budgeted `Solver`, which is fast there because they hold far fewer pegs.
 */
object OpeningBook {
    fun firstWinningMove(board: Board): BookEntry?  // Hit(move) | NoWinningMove | null (miss)
}
```

**Critical correctness rule:** a book *miss* (position not in the book) must fall through to the Solver. A book *hit* recording "no winning move" is authoritative. Conflating the two would reintroduce the exact untruthfulness DEBT-01 exists to remove.

### Pattern 3: Bitboard search core

**What:** Replace per-node allocation with per-spec precomputed tables, cached once per `BoardSpec`.

```kotlin
// domain/src/main/kotlin/.../solver/SolverTables.kt (sketch)
internal class SolverTables private constructor(spec: BoardSpec) {
    val needMask: LongArray   // (from|over) bits that must be pegs
    val toMask: LongArray     // destination bit that must be empty
    val xorMask: LongArray    // from^over^to — applying a move is ONE xor
    val moveOrder: IntArray   // pre-sorted by inwardness, once
    val classPlus: LongArray  // 3 masks for (r+c) mod 3   -> Long.bitCount parity
    val classMinus: LongArray // 3 masks for (r-c) mod 3
    val symChunks: Array<Array<LongArray>> // [sym][chunk][byte] canonicalisation LUT
    companion object { fun forSpec(spec: BoardSpec): SolverTables /* memoised */ }
}

// hot loop, allocation-free:
if (pegs and needMask[m] == needMask[m] && pegs and toMask[m] == 0L) {
    search(pegs xor xorMask[m], depth + 1)
}
```

Measured effect: ~0.27M → ~15M nodes/s [VERIFIED: measured].

### Pattern 4: Beam search as the authoring engine

**What:** Level-synchronous best-first search. At each ply, expand the whole frontier, canonicalise, score by **mobility** (number of legal moves remaining), keep the top W plus a random tail for diversity.

**When to use:** authoring-time fixture generation *only* — Diamond's floor line and every book entry. Beam search cannot prove unsolvability, so it must never be the production "no winning move" authority.

```kotlin
// domain/src/test/kotlin/.../golden/solutions/BeamSearchAuthoring.kt (sketch)
// Keeping the top half strictly by mobility and sampling the remainder preserves
// diversity; a pure top-W beam collapses into one basin and stalls above the floor.
fun beamSolve(start: Board, targetPegs: Int, width: Int, seed: Long): List<Move>?
```

Measured: width 400 000 solved Wiegleb-45 to one peg and Diamond-41 to its two-peg floor, both in single-digit minutes — where the existing jittered DFS failed on Wiegleb-45 after 865M nodes.

### Pattern 5: Per-board star bands

```kotlin
// domain/src/main/kotlin/.../scoring/ScoreParams.kt — scoped, no magic numbers (QUAL-03)
object DiamondStarParams {
    /** Diamond-41's proven fewest-pegs floor F (DEBT-02): reachable, and F-1 = 1 is
     *  excluded by the GF(4) colour invariant. Bands mirror the global shape (D-04). */
    const val FLOOR_PEGS = 2
    const val THREE_STAR_MAX_PEGS = FLOOR_PEGS          // F      -> 3 stars
    const val TWO_STAR_MAX_PEGS = FLOOR_PEGS + 2        // F+1..F+2 -> 2 stars
}

// ScoreCalculator gains a band-source parameter derived from the board, NOT a level id.
fun stars(pegsRemaining: Int, minimumPegs: Int = ScoreParams.THREE_STAR_MAX_PEGS): Int
```

Note the numeric coincidence worth calling out in the plan: Diamond's bands become 3★ at ≤2, 2★ at ≤4 — barely different from the global ≤1 / ≤3. The player-visible change is small; the *truthfulness* change is large.

### Anti-Patterns to Avoid

- **Shipping the beam search in `:domain` main.** It is incomplete and cannot produce a truthful `null`. Authoring source set only, exactly like `AuthoringSolver` today.
- **Letting one solver attempt consume the whole deadline.** The current defect. Cap each attempt at ~200k distinct positions so restarts happen.
- **Hand-transcribing any fixture.** `SolutionTest`'s existing discipline (Pitfall 7 in its KDoc) — generate against real `Boards.*` values under `UPDATE_GOLDEN`.
- **Running the depth-K sweep as a per-commit gate.** Measured at ~6 minutes for K=4. It is a `check`-level or tagged test.
- **Treating a book miss as "no winning move".** See Pattern 2.
- **Deriving the Diamond target from the level id in `:app`.** Breaks `:domain` purity direction; also silently wrong on the resume path.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Proving Diamond-41 cannot reach F−1 | An exhaustive symmetry-reduced search of the Diamond state space | The **existing GF(4) invariant** in `Pagoda.kt` | F = 2 so F−1 = 1, and one peg is exactly what the invariant excludes. Verified: 0 of 41 single-peg positions pass. An exhaustive Diamond search would need ~10¹⁰ canonical positions / ~100 GB (Durango Bill reports ~128 GB for a comparable 41-hole exhaustion) — infeasible, and completely unnecessary. |
| Symmetry canonicalisation | A fourth copy of `symmetryPermutations` | Consolidate the three that already exist (`Solver.kt`, `AuthoringSolver.kt`, `generator/BoardCanonical.kt`) | Three verbatim copies is already a maintenance hazard; the bitboard rewrite is the moment to fix it. **I hit a real bug from exactly this class of duplication while researching** — a chunked canonicaliser that dropped bit 40 silently corrupted a Diamond search into "reaching" one peg. |
| Transposition table | `HashSet<Long>` | The existing `VisitedSet` | ~16 B/entry vs ~72 B boxed; the KDoc already explains it. Correct as-is. |
| Move legality in fixtures | Trusting a recorded move | `RulesEngine.legalMoves` re-validation on every replayed step | `SolutionTest` and `GetHintUseCase` already do this (threat T-04-CT). The book decoder must too. |
| Finding pagoda functions | An LP solver dependency in the test source set | Nothing — skip pagoda generation | Measured 41–44% kill rate on residual candidates. Not worth a dependency or the phase budget. |
| Deciding "is this early position winnable at all" at runtime | A stronger runtime solver | The authoring-time opening book | Measured: 0/76 Wiegleb-45 depth-≤4 positions resolve in 1.5 s at 15M nodes/s. Precomputation is the only mechanism that closes this. |

**Key insight:** every remaining hard problem in this phase is *offline-shiftable*. The player-facing deadline is 1.5 s; the authoring budget is unbounded. Every time the phase is tempted to make the runtime solver smarter, check first whether the answer can simply be precomputed and committed — the project already has the fixture pipeline to do it.

---

## Common Pitfalls

### Pitfall 1: Generalising `Pagoda` to `minimumPegs` silently destroys the prune — and un-mutes Diamond's solver
**What goes wrong:** `Pagoda.mayBeSolvable` currently asserts the terminal parity vector has weight exactly 1. For a T-peg terminal the admissible set is every parity vector with weight ≤ T and weight ≡ T (mod 2) — for T=2 that is 4 of 8 vectors instead of 1 of 8. The prune goes from strong to nearly vacuous.
**Why it happens:** the invariant genuinely carries less information about multi-peg terminals; this is mathematics, not a bug.
**Second-order consequence (the dangerous one):** today `Pagoda.mayBeSolvable` returns `false` for *every* Diamond-41 position, so `Solver.firstWinningMove` returns `null` in O(cells) with **zero search**. After the change, every Diamond hint launches a real, barely-pruned 1.5 s search. That is a new sustained CPU/battery/thermal load on a board that previously cost nothing.
**How to avoid:** make the Diamond floor line + book the *primary* Diamond hint path so the solver is the rare fallback, not the default. Verify `GameViewModel`'s existing single-in-flight `hintJob` cancellation still holds (it does today, `hintJob?.cancel()`), and add a `:domain` test asserting Diamond hint resolution is book-served on the canonical line.
**Warning signs:** Diamond hint latency jumps from instant to ~1.5 s; device warms during Diamond play.

### Pitfall 2: `SolverBudgetTest`'s 4× headroom contract will move under your feet
**What goes wrong:** the test asserts `DEFAULT_MAX_NODES >= 4 × maxObservedNodes` over a tractable corpus. Both the bitboard rewrite (different node accounting) and a new per-attempt cap change what "observed nodes" means.
**Why it happens:** the contract couples a *safety ceiling* to a *measured maximum*, and this phase changes the measurement.
**How to avoid:** decide explicitly whether the headroom assertion now applies to the per-attempt cap or the total. The KDoc's standing instruction — "RAISE this value if the measurement exceeds the 4x headroom — never lower the assertion" — must be honoured, and the reasoning re-documented rather than silently edited.
**Warning signs:** a plan task that edits `HEADROOM_FACTOR` or lowers `DEFAULT_MAX_NODES`.

### Pitfall 3: A new `BoardSpec` field breaks the resume path silently
**What goes wrong:** `SnapshotMapper.toBoardSpecOrNull()` reconstructs `BoardSpec(inPlay = positions, target = target)` from the persisted JSON blob — it does **not** re-resolve the spec from `Boards.*`. A `minimumPegs` field with a default of 1 means every resumed Diamond-41 game silently reverts to the one-peg target: the hint instantly says "no winning move" again, and stars are computed against the wrong bands.
**Why it happens:** the resume DTO is a structural copy of the footprint, not a reference to the catalogue entry.
**How to avoid:** either (a) add `minimumPegs` to `ResumeSnapshotDto` with validated decode and treat a legacy blob's absence as "re-resolve from the level id", or (b) have the resume path re-resolve the `BoardSpec` from `ClassicLevelSource` by level id and use the blob only for `pegs`/`moveHistory`. Option (b) is structurally sounder and removes a whole class of forged-footprint surface, but it is a bigger change. **This needs a planner decision and a `MigrationTest`/`SnapshotMapperTest` case either way.**
**Warning signs:** `SnapshotMapperTest` and `ResumeRepositoryTest` untouched by the plan while `BoardSpec` gains a field.
**Files:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt` (~line 145–160), `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`.

### Pitfall 4: Chunked symmetry canonicalisation that drops high bits
**What goes wrong:** the fast canonicaliser processes the 64-bit mask in 8-bit chunks. Handling only chunks 0–4 covers bits 0–39 — silently dropping bit 40 on the 41-hole Diamond and bits 40–44 on Wiegleb-45. The search then treats corrupted masks as real positions and can "reach" states that do not exist.
**Why it happens:** the chunk count is a function of `spec.size` and it is easy to hard-code for the board you tested first.
**How to avoid:** derive the chunk count from `spec.size` (`(size + 7) / 8`), and add a round-trip property test: for every board and a large sample of masks, `canonical(m)` must have the same `bitCount` as `m` and must be stable under re-application.
**Warning signs:** a search reports a terminal peg count that violates the GF(4) invariant. **I hit exactly this during research** — a Diamond beam search reported reaching one peg, which the invariant proves impossible.

### Pitfall 5: The depth-K sweep smuggled into the fast test gate
**What goes wrong:** measured K=4 sweep wall time is ~6 minutes across four boards (158.6 s for European-37 alone). `:domain:test` is the per-commit gate at "seconds".
**How to avoid:** gate the sweep behind a system property or a dedicated Gradle task wired into `check` but not into the per-commit `:domain:test` run — the same shape as the existing `UPDATE_GOLDEN` gating. Once the opening book exists the sweep is a *book verification* (fast lookup + line replay) rather than a solver race, which changes the economics entirely.
**Warning signs:** phase-3/4 VALIDATION contracts list `./gradlew :domain:test` as the per-commit gate; a sweep test in that source set breaks the ~seconds latency promise those contracts make.

### Pitfall 6: Beam-search fixtures that are not byte-stable
**What goes wrong:** the existing fixture discipline promises regenerated fixtures are byte-stable under a fixed seed. A beam search with a `HashSet` frontier and a random diversity tail is order-sensitive and can produce a different (still valid) line per run.
**How to avoid:** use an ordered frontier collection (`LinkedHashSet` over a deterministic expansion order) and a fixed seeded RNG; assert regeneration stability in the authoring test. Alternatively accept instability and assert only the *invariants* (every step legal, terminal peg count == F) rather than byte identity — but say so explicitly in the KDoc, because it weakens the golden-master contract the project relies on elsewhere.

### Pitfall 7: Correcting the stale docs incompletely (D-06)
**What goes wrong:** D-06 asks for the "(4,2) off-center start" / one-peg wording to be corrected. But research shows the correction is *larger* than D-06 anticipated: the "floor is several pegs" / "~5" claim in `Boards.kt`, `AuthoringSolver.kt` (`SolutionFixtures` KDoc) and `SolutionTest.kt` is also wrong — the floor is 2.
**How to avoid:** treat D-06 as covering four surfaces, not two: `.planning/ROADMAP.md` (DEBT-02 criterion), `.planning/STATE.md` (carried blocker, line 110), `.planning/PROJECT.md` (Key Decisions "⚠️ Revisit" row for the solver), **and** the three in-code KDocs asserting a ~5-peg floor.

---

## Code Examples

### Verifying the GF(4) invariant excludes a one-peg Diamond finish

```kotlin
// The argument already shipped in Pagoda.kt, stated as the DEBT-02 optimality proof.
// Colour each cell by (r+c) mod 3 and by (r-c) mod 3. Any jump touches all three
// colours of a colouring exactly once, so a jump flips ALL THREE class parities.
// The GF(4) value Sum(omega^colour) over pegs is therefore invariant.
//
//   Diamond-41 center start: (r+c) parities [0,0,0], (r-c) parities [0,0,0]
//     -> GF(4) value 0 under BOTH colourings
//   Any single-peg position:  exactly one class odd
//     -> GF(4) value omega^k != 0
//   => no single-peg position is reachable.                 [VERIFIED: exhaustive, 0/41]
//
// F = 2 is reachable (committed golden line), so F = 2 EXACTLY.
```

### Applying a move with precomputed bitboard tables

```kotlin
// Source: reference implementation measured this session (~15M nodes/s vs ~0.27M)
// Precomputed once per BoardSpec in SolverTables:
//   needMask[m] = bit(from) or bit(over)     // both must hold pegs
//   toMask[m]   = bit(to)                    // must be empty
//   xorMask[m]  = needMask[m] or toMask[m]   // applying the move is ONE xor
for (i in moveOrder.indices) {
    val m = moveOrder[i]
    if (pegs and tables.needMask[m] == tables.needMask[m] &&
        pegs and tables.toMask[m] == 0L
    ) {
        if (search(pegs xor tables.xorMask[m], depth + 1)) return true
    }
}
```

### Parity check without iterating cells

```kotlin
// Replaces the per-cell Math.floorMod loop in Pagoda with 6 popcounts.
// classPlus[i]  = OR of bits whose (row+col) mod 3 == i   (precomputed per spec)
// classMinus[i] = OR of bits whose (row-col) mod 3 == i
val movesFlipParity = ((pegs.countOneBits() - targetPegs) and 1) == 1
var oddClasses = 0
for (i in 0 until 3) {
    val odd = (pegs and tables.classPlus[i]).countOneBits() and 1 == 1
    if (odd xor movesFlipParity) oddClasses++
}
```

### Restart policy: cap the attempt, not the deadline

```kotlin
// The defect: every attempt gets budget.maxNodes (40M), so the 1.5s deadline always
// fires inside attempt 0 and MAX_ATTEMPTS = 40 is unreachable in production.
// The fix: a small per-attempt distinct-position cap so restarts actually happen.
//   measured: European-37 start  172ms (1 unbounded attempt) -> 33ms (2 x 100k cap)
//             Asymmetric-39 start 235ms                      -> 59ms (2 x 200k cap)
private const val DEFAULT_PER_ATTEMPT_MAX_NODES = 200_000L
```

---

## State of the Art

| Old Approach (v1.0) | Current Recommendation | Why It Changed | Impact |
|---------------------|------------------------|----------------|--------|
| One unbounded solver attempt per deadline | Many small capped attempts | Peg-solitaire DFS runtime is heavy-tailed; the shipped restart loop is unreachable in production | European-37 start 172 ms → 33 ms |
| `List<Move>` + per-node `sortedByDescending` + `Board.copy` | Precomputed bitboard tables, allocation-free loop | ~55× measured throughput gap | 0.27M → ~15M nodes/s |
| "Diamond-41's floor is ~5 pegs" (random rollouts) | **F = 2** (beam search) | Rollouts are far too weak to find the floor | DEBT-02 optimality proof becomes free (F−1 = 1, excluded by GF(4)) |
| "Stronger pruning will fix the deadline envelope" | Precomputed depth-K opening book | Measured: 0/76 Wiegleb-45 depth-≤4 positions resolve in 1.5 s even at 15M nodes/s | DEBT-01 becomes a lookup guarantee |
| Jittered DFS as the authoring solver | Mobility-ordered beam search | DFS failed Wiegleb-45 at 865M nodes/57 s; beam solved it | Fixture generation becomes reliable |
| "Real pagoda functions are the lever" (D-07) | Pagoda is optional polish | LP-verified: 41–44% kill rate on residual candidates | Redirects phase budget to throughput + book |

**Deprecated/outdated in this repo:**
- `Boards.kt` Diamond-41 KDoc — "search confirms the floor is several pegs" → the floor is **2**.
- `AuthoringSolver.kt` `SolutionFixtures` KDoc — same claim.
- `SolutionTest.kt` class KDoc — "Diamond-41 has NO one-peg fixture by design" is still true, but "the floor is several pegs" is not.
- `STATE.md` line 110 — "off-center (4,2) start" is stale (D-06 already flags this).
- `PROJECT.md` Key Decisions — "universal early-position completeness not achievable at usable deadline" is **confirmed correct for Wiegleb-45** and should be updated to say so precisely rather than removed.

---

## Runtime State Inventory

> Included because this phase changes a persisted scoring contract and a persisted board representation, even though it is not a rename.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| **Stored data** | Room `LevelResult` rows hold per-level `stars`/`bestScore`. Diamond-41 bests recorded under the old global bands (a 2-peg finish stored as 2★) will not retroactively become 3★. | Per CONTEXT (Claude's Discretion), the existing upsert-on-better path upgrades them on next play. **Verify** `LevelResult` does not persist `pegsRemaining` — if it does, a one-shot re-derivation is possible; if not, no migration is possible and that must be stated in the plan. Files: `app/src/main/java/com/app/pegsolitaire/data/` + `data/db/`. |
| **Live service config** | None — this phase is SDK-free and touches no external service. Verified: no Play Games / AdMob / network surface in `:domain`, and the app declares no `INTERNET` permission. | none |
| **OS-registered state** | None — no scheduled work, no notification channels, no widgets touched. | none |
| **Secrets/env vars** | `UPDATE_GOLDEN` is the only environment flag involved; it is a build-time authoring switch, not a secret. Unchanged in name and semantics. | none |
| **Build artifacts** | `domain/src/test/resources/golden/solutions/*.txt` (4 committed one-peg lines) are consumed only by tests today. Moving/copying them to `domain/src/main/resources/` for production visibility changes the packaged artifact. New `golden/book/` resources add ~100 KB at K=4. | Confirm the `:domain` JAR resource path is reachable from the app at runtime (it is a plain JVM module — `Class.getResourceAsStream`), and confirm R8 resource shrinking (landing in Phase 6) does not strip them. **Cross-phase note: Phase 6 introduces R8/minification for the first time — flag this to the Phase 6 owner.** |

---

## DEBT-03 Audit (D-09)

Performed this session against the two draft contracts.

**Phase 3 (`03-VALIDATION.md`, `nyquist_compliant: false`)** — every named Wave-0 file exists:

| Contract Wave-0 item | Exists? | File |
|---|---|---|
| `ChallengeGeneratorGoldenTest.kt` | ✅ | `domain/src/test/.../generator/ChallengeGeneratorGoldenTest.kt` |
| `ChallengeSolvabilityTest.kt` | ✅ | `domain/src/test/.../generator/ChallengeSolvabilityTest.kt` |
| `DifficultyTierTest.kt` | ✅ | `domain/src/test/.../generator/DifficultyTierTest.kt` |
| `QualityGateTest.kt` | ✅ | `domain/src/test/.../generator/QualityGateTest.kt` |
| `:app` Challenge wiring (`levelNumber` clamp, advance-on-solve, stuck→Retry) | ✅ | `GameViewModelTest.kt` (clamp at lines 134/188/1355/1398 — "forged `challengeLevel = 0` must clamp to level 1, T-03-09"), `GameViewModelChallengeAdvanceTest.kt`, `NavFlowTest.kt`, `ResultsViewModelTest.kt` (WARN-03 clamp, line 619) |
| *Not in the contract but present* | ➕ | `ChallengeLevelBoundsTest.kt`, `FreeformValidityTest.kt` |

**Phase 4 (`04-VALIDATION.md`, `nyquist_compliant: false`)** — same picture:

| Contract Wave-0 item | Exists? | File |
|---|---|---|
| `SolverTest` | ✅ | `domain/src/test/.../solver/SolverTest.kt` (3 tests: legal win-preserving move, null on unsolvable, null when solved) |
| `GetHintUseCaseTest` | ✅ | `domain/src/test/.../usecase/GetHintUseCaseTest.kt` (6 tests covering the full recorded-line → solver → null pipeline) |
| `GameViewModelTest` (hint/thinking/no-win/undo/rescue) | ✅ | `app/src/test/.../ui/game/GameViewModelTest.kt` |
| Compose UI / semantics for highlight, thinking, banner, undo | ✅ | `HintControlTest.kt`, `NoWinBannerTest.kt`, `UndoControlTest.kt` |
| *Not in the contract but present* | ➕ | `SolverBudgetTest.kt`, `PagodaTest.kt` |

**Conclusion:** DEBT-03 is **overwhelmingly a reconciliation task, not a coverage task.** The contracts' `Per-Task Verification Map` tables are literal `TBD` / `4-XX-XX` placeholders — the tests were written but never mapped back. The genuine work is:

1. Populate both `Per-Task Verification Map` tables with the *actual* task IDs from the archived v1.0 plans and the *actual* test file/command per row.
2. Add rows for the tests that exist but the contract never named (`ChallengeLevelBoundsTest`, `FreeformValidityTest`, `SolverBudgetTest`, `PagodaTest`).
3. Confirm each contract's `Validation Sign-Off` checkboxes against reality and flip `nyquist_compliant: true` + `wave_0_complete: true`.
4. Update `.planning/milestones/v1.0-MILESTONE-AUDIT.md` frontmatter (`nyquist.partial_phases: [03, 04]` → `compliant_phases: [01,02,03,04,05]`) and the Nyquist Coverage table.
5. **Genuine new coverage to consider:** the *only* behavior I could not find a direct test for is phase 3's contract row "Quality gates reject trivial/degenerate/**duplicate** boards deterministically" — `QualityGateTest.kt` exists but the planner should confirm the duplicate-rejection leg specifically. Treat as one small test, not a wave.

Both contracts declare the per-commit gate as `./gradlew :domain:test` (seconds). **Pitfall 5 applies:** if this phase's depth-K sweep lands in `:domain:test`, both freshly-signed-off contracts are immediately violated.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Diamond-41's floor F = 2 is the true minimum (beam search found 2; F=1 proven impossible, so F=2 is exact **provided the beam witness replays clean**) | Measured Evidence | **Low but load-bearing.** The witness was found by my scratchpad harness, not by the project's `RulesEngine`. The plan MUST include a task that regenerates the line through the real `RulesEngine` and asserts it terminates at 2 pegs before any star band is anchored on it. If the line does not reproduce, F reverts to unknown and D-05's escalation clause fires. |
| A2 | Every Wiegleb-45 position at depth ≤ K is actually winnable (so the book never needs to record an unprovable "no winning move") | Pattern 2 | If some early Wiegleb-45 position is genuinely dead, proving it requires exhausting a ~10¹¹-position space — infeasible. The book would have to record "unknown", which reintroduces an untruthful banner for that position. Cross boards are very forgiving early so this is unlikely, but it is unverified. |
| A3 | ~15M nodes/s carries over from the Java reference to Kotlin/JVM in `:domain` | Measured Evidence | Kotlin compiles to the same bytecode shapes; risk is low. But if it lands at only 5M nodes/s, European-37/Asymmetric-39 still fit the deadline (33–296 ms has 5–45× margin), so the conclusion is robust. |
| A4 | An opening book is acceptable under D-07 ("truthful hints must come from a stronger solver") | Pattern 2 | D-07's *intent* is clearly "don't make the player wait longer", which the book satisfies perfectly, and CONTEXT's discretion section already floats a recorded-line fast path. But the book goes beyond D-07's literal wording. **Recommend surfacing to the user before planning locks.** |
| A5 | `:domain` main resources are reachable at runtime and survive R8 | Runtime State Inventory | `:domain` is a plain JVM module so `getResourceAsStream` works; R8 resource shrinking arrives in Phase 6 and is the real risk. Mitigation: keep the book as a Kotlin source constant instead of a resource if shrinking proves troublesome (100 KB of Kotlin string constants is ugly but bulletproof). |
| A6 | Room `LevelResult` does not persist `pegsRemaining` (so no Diamond star re-derivation is possible) | Runtime State Inventory | Unverified — I did not read the entity. If it *does* persist it, a cheap one-shot migration could upgrade stored Diamond stars, which is strictly better UX. One file read closes this. |
| A7 | The ~40% pagoda LP kill rate on Diamond targets generalises to in-search pruning value on the cross boards | Measured Evidence | I measured pagoda against *Diamond terminals* specifically. Pagoda pruning *during* a cross-board search targeting a specific final cell could be materially stronger. This does not change the recommendation (the book is needed regardless) but the "pagoda is not the lever" claim is weaker for the cross boards than for the Diamond. |

---

## Open Questions (RESOLVED)

> All five questions were resolved during planning (2026-08-29):
> OQ1 — user explicitly approved **opening book + stronger solver** (full D-08 guarantee on all three boards; plans 08-08/09/10).
> OQ2 — answered by plan 08-08's authoring run + blocking checkpoint (non-zero undecided count escalates).
> OQ3 — resolved as `BoardSpec.minimumPegs` field with resume-path preservation (plans 08-01 + 08-07).
> OQ4 — **K = 4 with raw masks**, locked into plan 08-08.
> OQ5 — yes; consolidation into `BoardCanonical` with property test is plan 08-02.

1. **Does D-08's exhaustive guarantee survive contact with Wiegleb-45?**
   - What we know: 0 of 76 depth-≤4 Wiegleb-45 positions resolve in 1.5 s at ~15M nodes/s; the gap is ~4 orders of magnitude.
   - What's unclear: whether the user accepts an opening book as satisfying D-07/D-08, or prefers to narrow DEBT-01's scope to European-37 + Asymmetric-39 (both of which the strengthened solver genuinely fixes).
   - Recommendation: **escalate before planning locks**, exactly as D-05 instructs for the analogous Diamond risk. Present both options: (a) book → full D-08 guarantee on all three boards; (b) solver-only → D-08 on two of three, Wiegleb-45 documented as a permanent envelope.

2. **How many of European-37's 57 and Asymmetric-39's 26 unresolved depth-≤4 positions are genuinely unwinnable?**
   - What we know: my sweep harness cannot distinguish "proven unsolvable" from "deadline miss".
   - What's unclear: the true winnable/dead split, which determines whether the strengthened solver alone actually satisfies D-08 for those two boards.
   - Recommendation: the opening-book generation run answers this for free (unbounded search per position). Sequence book generation before the D-08 sweep test is written.

3. **`BoardSpec` field vs. resume re-resolution (Pitfall 3).**
   - What we know: `SnapshotMapper` rebuilds the spec structurally from the blob and would default `minimumPegs` to 1.
   - What's unclear: whether the planner prefers a DTO field + migration, or switching resume to re-resolve the spec from `ClassicLevelSource` by level id.
   - Recommendation: option (b) is structurally better and shrinks a forged-input surface, but it is a larger change touching `ResumeRepository`, `SnapshotMapper`, and their tests. Planner's call; **either way it needs an explicit task**, because the silent-default failure is invisible in normal testing.

4. **K = 4, 5, or 6?**
   - What we know: K=4 → 621 book entries across the three boards (~100 KB raw); K=6 → 14 566 canonical (~115 000 raw, and raw becomes impractical so symmetry-inverse move mapping is required).
   - Recommendation: **K = 4 with raw masks.** It is provably complete over the frontier, needs no symmetry-inverse machinery, and is cheap. Revisit only if playtesting shows players routinely deviate past move 4 and then hit a stale banner.

5. **Should the three copies of `symmetryPermutations` be consolidated in this phase?**
   - What we know: identical logic exists in `Solver.kt`, `AuthoringSolver.kt`, and `generator/BoardCanonical.kt`; the bitboard rewrite touches two of them; I hit a real correctness bug from this exact duplication class during research.
   - Recommendation: yes, but as an explicitly-scoped task with a canonicalisation property test, not as incidental drive-by refactoring inside a solver task.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | `:domain` JVM tests, authoring runs | ✓ | OpenJDK 17.0.15 (Homebrew) | — |
| Gradle wrapper + `kotlin-jvm`/`standards` convention plugins | Build + detekt/ktlint gate | ✓ | project-pinned (Kotlin 2.3.20, AGP 9.1.0) | — |
| detekt config | `check` gate | ✓ | `config/detekt/detekt.yml` | — |
| ktlint / `.editorconfig` | `check` gate | ✓ | `.editorconfig` present | — |
| `UPDATE_GOLDEN` heap bump | Fixture authoring | ✓ | `maxHeapSize = "4g"` in `domain/build.gradle.kts` | **Raise to 8g** — the beam search at width 400 000 needs it |
| ~8 GB free RAM on the authoring machine | Beam-search fixture generation | ✓ (verified this session) | — | Lower the beam width; Diamond reached 2 pegs and Wiegleb reached 1 peg at width 400 000, but narrower widths are untested |
| External network / SDKs | — | n/a | — | Phase is deliberately SDK-free (parallel with Phases 6–7) |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none blocking; the `UPDATE_GOLDEN` heap setting needs a one-line raise and is a plan task, not a blocker.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Google Truth 1.4.x (`:domain`, pure JVM); Robolectric/Compose UI in `:app` |
| Config file | Gradle version catalog + `domain/build.gradle.kts` (`UPDATE_GOLDEN` heap hook) — no separate runner config |
| Quick run command | `./gradlew :domain:test` |
| Full suite command | `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| DEBT-02 | Diamond-41's committed floor line replays legal through `RulesEngine` and terminates at exactly `Boards.Diamond41.minimumPegs` (= 2) | golden replay (JVM) | `./gradlew :domain:test --tests '*SolutionTest*'` | ✅ extend `SolutionTest.kt` |
| DEBT-02 | One peg is unreachable from the Diamond center start (the F−1 optimality half) — exhaustive over all 41 single-peg positions via the GF(4) invariant | invariant (JVM) | `./gradlew :domain:test --tests '*PagodaTest*'` | ✅ extend `PagodaTest.kt` |
| DEBT-02 / D-03 / D-04 | `ScoreCalculator.stars` uses Diamond bands (2→3★, 3–4→2★, ≥5→1★) and global bands elsewhere | unit (JVM) | `./gradlew :domain:test --tests '*ScoreCalculatorTest*'` | ✅ extend `ScoreCalculatorTest.kt` |
| DEBT-01 | Every position reachable within K moves of European-37 / Asymmetric-39 / Wiegleb-45 resolves to a *correct* answer (book hit or solver, verified by replaying a full winning line) | exhaustive sweep (JVM) | `./gradlew :domain:test --tests '*DepthSweepTest*' -PrunSweep=true` | ❌ Wave 0 |
| DEBT-01 | Opening book integrity: every entry's mask is a reachable position, every recorded move is legal on it, and the recorded line terminates at the board's `minimumPegs` | golden integrity (JVM) | `./gradlew :domain:test --tests '*OpeningBookTest*'` | ❌ Wave 0 |
| DEBT-01 | Solver still returns `null` only for genuinely unsolvable positions after the bitboard rewrite (non-regression on the existing corpus) | unit (JVM) | `./gradlew :domain:test --tests '*SolverTest*' --tests '*SolverBudgetTest*'` | ✅ exists |
| DEBT-01 / Pitfall 4 | Canonicalisation is bit-count preserving and idempotent on every board footprint | property (JVM) | `./gradlew :domain:test --tests '*CanonicalTest*'` | ❌ Wave 0 |
| D-01/D-02 | `GetHintUseCase` serves the Diamond floor line while on-line and returns `null` only once the floor is unreachable | unit (JVM) | `./gradlew :domain:test --tests '*GetHintUseCaseTest*'` | ✅ extend |
| Pitfall 3 | A resumed Diamond-41 game preserves `minimumPegs` (hint still book-served, stars still Diamond-banded) | unit (Robolectric) | `./gradlew :app:test --tests '*SnapshotMapperTest*' --tests '*ResumeRepositoryTest*'` | ✅ extend |
| DEBT-03 | Phase 3/4 contracts reconciled; `nyquist_compliant: true` | doc gate | manual + `./gradlew check` | n/a |

### Sampling Rate

- **Per task commit:** `./gradlew :domain:test` (seconds — the sweep is excluded by default, Pitfall 5)
- **Per wave merge:** `./gradlew ktlintCheck detekt :domain:test :app:test`
- **Phase gate:** full suite green **plus one explicit sweep run** (`-PrunSweep=true`, measured ~6 min at K=4 pre-book, far less post-book) before `/gsd:verify-work`
- **Authoring (not a gate):** `UPDATE_GOLDEN=true ./gradlew :domain:test` with `maxHeapSize = "8g"` — minutes, run deliberately, never in CI

### Wave 0 Gaps

- [ ] `DepthSweepTest.kt` — DEBT-01's D-08 universal claim; must be property-gated out of the fast run
- [ ] `OpeningBookTest.kt` — book integrity (every entry replays to `minimumPegs`)
- [ ] `CanonicalTest.kt` — bit-count-preserving / idempotent canonicalisation across all five footprints (Pitfall 4)
- [ ] `BeamSearchAuthoring.kt` + `OpeningBookAuthoring.kt` (test source set) — fixture generators
- [ ] Raise `UPDATE_GOLDEN` `maxHeapSize` 4g → 8g in `domain/build.gradle.kts`
- [ ] Gradle property/task wiring so the sweep runs in `check` but not in `:domain:test`

*Framework install: none — JUnit4 + Truth already declared in `domain/build.gradle.kts`.*

---

## Security Domain

`security_enforcement` is not disabled in `.planning/config.json`, so this section is included. The phase is pure `:domain` computation with no new I/O, so most ASVS categories are inapplicable by construction.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No identity surface; SDK-free phase |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No multi-user surface |
| **V5 Input Validation** | **yes** | Two new untrusted-ish inputs: (a) opening-book resource content, (b) the `minimumPegs` field arriving via the persisted resume blob. Both must be validated, not trusted. |
| V6 Cryptography | no | No crypto; never hand-roll — none needed |
| V12 Files & Resources | yes | Book/fixture resources are read from the app's own JAR, not user-supplied — but a malformed or truncated resource must fail closed |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Forged/stale resume blob supplies an out-of-range `minimumPegs` (e.g. 0 or negative), making the solver's goal test `pegCount <= target` trivially true and the hint always `null` — or making stars nonsensical | Tampering | Validate on decode: `minimumPegs in 1..spec.size`, reject the snapshot otherwise. The existing `SnapshotMapper` already fails closed on malformed footprints (`toBoardSpecOrNull`) — extend the same discipline. Mirrors the existing T-05-04 move-history guard. |
| Opening-book entry names an illegal move; a hint highlights a move gameplay would reject | Tampering / Integrity | Re-validate every book move against `RulesEngine.legalMoves` before returning it — identical to the existing `GetHintUseCase` recorded-branch guard (threat T-04-CT). **Never return a book move unchecked.** |
| Truncated/corrupt book resource parsed into partial entries | Tampering | Parse fail-closed: any malformed line makes the whole book unavailable (fall through to the Solver) rather than silently serving a partial book. |
| Unbounded solver work on a crafted board (Challenge freeform) after the Pagoda prune weakens for `minimumPegs > 1` | Denial of Service (T-04-SD) | Keep the node cap *and* the deadline; the new per-attempt cap tightens rather than loosens this. Confirm the freeform generator never emits `minimumPegs > 1` (it should keep the default of 1). |
| Book resource inflates APK / is stripped by R8 | Availability | Size-bound the book (K=4 ≈ 100 KB) and add a startup-agnostic integrity test; coordinate with Phase 6's R8 introduction. |

No new network, no new permissions, no new SDK surface. The domain-purity gate (`DomainPurityTest`, `PlatformBoundaryTest`) continues to enforce the architectural boundary and must stay green.

---

## Project Constraints (from CLAUDE.md)

| Directive | How this phase must comply |
|-----------|---------------------------|
| No magic numbers/strings in logic; constants **split by scope** | New values (`MINIMUM_PEGS_DEFAULT`, `DiamondStarParams.FLOOR_PEGS`, `DEFAULT_PER_ATTEMPT_MAX_NODES`, book `K`) go into per-scope objects alongside `ScoreParams`/`SolverBudget`, never inline. The existing `SOLVED_PEG_COUNT = 1` private constants in `Solver.kt` and `AuthoringSolver.kt` are replaced by the spec field, not duplicated a third time. |
| Game logic isolation — pure Kotlin `:domain`, no Android deps | Every solver/book/scoring change stays in `:domain`; `OpeningBook` must use `Class.getResourceAsStream`, **not** `android.content.res`. `DomainPurityTest` is the gate. |
| Clean Architecture / MVVM, state hoisting | No UI change is required by this phase — `GameUiState.noWinningMove` and the rescue banner are untouched (CONTEXT explicitly says so). Resist any temptation to add Diamond-specific UI. |
| dimens resource file for UI dimensions | Not applicable — no UI work. |
| detekt + ktlint passing as a build gate | The bitboard rewrite will produce long methods and high cyclomatic complexity in the search loop. **Expect detekt friction**; plan for it explicitly (extract table construction from the hot loop) rather than reaching for blanket `@Suppress`. |
| Gitflow strictly — `feature/*` off `develop` | Phase branch per `.planning/config.json`: `gsd/phase-8-assist-generator-quality-debt`. No direct commits to `develop`/`main`. |
| No coroutines across the `:domain` boundary | Cancellation stays the `shouldCancel: () -> Boolean` lambda. Do not introduce `suspend` in the solver. |

---

## Sources

### Primary (HIGH confidence)
- **Direct measurement, this session** — bitboard reference solver, exhaustive depth-K enumeration, 1.5 s deadline sweep, pagoda LP (SciPy/HiGHS), mobility-ordered beam search, all against the project's exact footprints regenerated from `Boards.kt`'s layout rules. This is the evidentiary backbone of the document.
- **In-repo code** — `Solver.kt`, `SolverBudget.kt`, `Pagoda.kt`, `GetHintUseCase.kt`, `Boards.kt`, `BoardSpec.kt`, `Board.kt`, `ScoreCalculator.kt`, `ScoreParams.kt`, `AuthoringSolver.kt`, `SolutionTest.kt`, `SolverBudgetTest.kt`, `SnapshotMapper.kt`, `ClassicLevelSource.kt`, `domain/build.gradle.kts`, `build.gradle.kts`.
- **In-repo planning docs** — `08-CONTEXT.md`, `REQUIREMENTS.md`, `ROADMAP.md`, `STATE.md`, `PROJECT.md`, `03-VALIDATION.md`, `04-VALIDATION.md`, `v1.0-MILESTONE-AUDIT.md`, `config.json`, `CLAUDE.md`.
- [jaapsch.net — Analysis of Peg Solitaire](https://www.jaapsch.net/puzzles/pegsolit.htm) — authoritative statement of the resource-count/pagoda definition and the three-colouring position-class invariant; corroborates the mathematics implemented in `Pagoda.kt`.

### Secondary (MEDIUM confidence)
- [MarkusThill/solitaire-diamond](https://github.com/MarkusThill/solitaire-diamond) — an independent Diamond-41 solver on the *same* 41-hole footprint with the *same* orthogonal-only jump rule. Independently confirms the center-empty start is not one-peg solvable, and independently arrives at bitboard representation + transposition tables + symmetry folding + corner-deferring move ordering.
- [George's Peg Solitaire Page (recmath.org)](http://recmath.org/pegsolitaire/) — George Bell's catalogue; source of the Asymmetric 3-3-2-2 board and general board classification.
- [Durango Bill — 41 Hole Peg Solitaire](http://www.durangobill.com/Peg41.html) — exhaustive-search scale reference (~128 GB RAM for a 41-hole exhaustion), which is the basis for the "full Diamond exhaustion is infeasible" claim. **Note:** his board's solvability results describe a different adjacency and must not be read as contradicting the GF(4) result for *this* project's orthogonal-jump Diamond.
- [Solving Peg Solitaire with Bidirectional BFIDA*](https://cdn.aaai.org/ojs/8143/8143-13-11670-1-2-20201228.pdf) — bidirectional search as the state-of-the-art alternative to the recommended book; noted as a rejected option (high complexity, not needed once the book exists).

### Tertiary (LOW confidence — not relied upon)
- [Bell, "Diamond Solitaire" (arXiv 0711.2749)](https://ar5iv.arxiv.org/html/0711.2749) — fetched, but it concerns *triangular-lattice* rhombus boards, not this project's square-lattice orthogonal diamond. **Not applicable**; recorded so a later reader does not re-chase it.
- Assorted search results on integer-programming formulations of peg solitaire — consistent with the pagoda LP I implemented directly, but not individually verified.

---

## Metadata

**Confidence breakdown:**
- **Measured algorithmic findings (F=2, ~15M nodes/s, 0/76 Wiegleb-45, depth tables, pagoda LP rates):** HIGH — produced by direct measurement this session against the project's exact footprints, with two independent cross-checks (the English-33 depth table matches the published sequence; the Diamond two-peg witness satisfies the GF(4) invariant).
- **Standard stack:** HIGH — no new dependencies; verified by reading `domain/build.gradle.kts`.
- **Architecture (per-board target on `BoardSpec`, opening book, beam authoring):** MEDIUM-HIGH — the mechanisms are proven, but the book goes beyond D-07's literal wording (A4) and the resume-serialization decision (Pitfall 3 / Open Question 3) is unresolved.
- **Pitfalls:** HIGH — Pitfalls 1, 3, 4 and 5 were each derived from reading the actual code or from a bug I hit while researching, not from general knowledge.
- **DEBT-03 audit:** HIGH — file-by-file verification against both contracts.
- **Wiegleb-45 escalation:** HIGH that the strengthened solver fails; MEDIUM on whether the user accepts the book as the remedy.

**Research date:** 2026-08-29
**Valid until:** ~2026-11-29 (90 days). The findings are mathematical and measurement-based rather than ecosystem-based, so they do not decay with library releases — they decay only if `Boards.kt` footprints or the 1.5 s deadline change.
