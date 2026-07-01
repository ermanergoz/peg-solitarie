# Phase 3: Challenge Generation & Difficulty Curve - Research

**Researched:** 2026-07-01
**Domain:** Deterministic, guaranteed-solvable procedural puzzle generation (reverse-generation) in a pure-Kotlin domain layer, wired behind an existing `LevelRepository` seam into a Compose/MVVM Android app
**Confidence:** HIGH (algorithm design and codebase seams verified against committed source; determinism guaranteed-by-construction plus a golden-master safety net; only difficulty *knob values* are explicitly deferred to playtest)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Progression flow**
- **D-01: Solve → Results → "Next level".** A solved Challenge level goes to the Phase-2 Results screen (stars/score/time). Results shows a **"Next level"** action that advances to level N+1. Results must become mode/context aware (Classic vs Challenge) to conditionally show "Next level". (Classic deliberately has no next-board action — quick task 260701-n9t.)
- **D-02: Must solve to advance.** Only a 1-peg solve unlocks level N+1. No skip in this phase. Levels are guaranteed solvable, so it is a fair ladder-of-mastery.
- **D-03 (Claude's discretion, default): show "Level N" during play** in the Challenge gameplay top bar.
- **D-04 (deferred behavior, default): Challenge starts at level 1 each session this phase.** Resume-from-highest is CHAL-05/PERS-02 (Phase 5). Track current level in a way Phase 5 can back with persistence (a `ChallengeProgress` seam); **no DataStore persistence required in Phase 3**.

**Difficulty curve feel (intent — exact knob values are playtested, NOT set now)**
- **D-05: Gentle start, smooth steady climb.** Levels ~1-10 easy/tutorial-ish (small boards, short solutions); difficulty then increases smoothly and continuously forever (no hard tier walls, no early plateau).
- **D-06: Solution length is the primary difficulty lever.** Raise required winning-line length (depth of planning) first; grow footprint size later and more slowly. Difficulty reads as "trickier," not merely "bigger." Maps to `tier.solutionLength` in the reverse-generation loop.
- **D-07: Difficulty knobs are a data-driven table** (`DifficultyTier`), tuned by playtest before `generatorVersion` is frozen. Knob VALUES (solution-length floor per range, footprint sizes by range, density/spread thresholds, freeform-introduction level) are explicitly **NOT** set in this context — a playtest-iteration loop per the roadmap risk note.

**Board shapes**
- **D-08: Canonical footprints early, freeform shapes later.** Early levels generate onto known-good canonical square footprints (English-33 cross, European-37, Diamond-41, Wiegleb-45, and smaller variants), reusing existing generalized `BoardGeometry`/rendering. Freeform shapes (arbitrary connected shapes within an NxN bounding grid) are introduced at higher levels as a variety lever.
- **D-09: Freeform validity guardrails (technical constraint, Claude handles).** Freeform boards MUST pass validity gates: connected (no isolated cells / unreachable regions), ≤ 64 in-play cells (domain `Board` is a `Long` bitmask), and expressed on the existing square-grid coordinate system so `BoardGeometry` and `BoardCanvas` render them unchanged. Prefer symmetric/aesthetically-intentional shapes where cheap.

**Stuck & scoring**
- **D-10: Stuck → Results → Retry (no "Next level").** A stuck/dead-end terminal state (2+ pegs, no legal moves) routes to Results showing the outcome with **Retry** (reload the same deterministic board) and **Home** — but NOT "Next level" (level wasn't solved). (Hints/undo that would rescue a stuck board are Phase 4.)
- **D-11: Level-scaled score.** Reuse the star model — a 1-peg solve is 3★ (stars near-constant in Challenge since solving is required to advance; a stuck finish shows fewer stars but does not advance). The **numeric score is scaled by level/difficulty** (a difficulty-aware multiplier over the Phase-2 `ScoreCalculator` base). Small, additive scoring change — do NOT re-base stars onto speed/efficiency in this phase.

### Claude's Discretion
- Level-number display (D-03) and level-1 start (D-04) are sensible defaults the user did not object to.
- Exact difficulty knob values (D-07), the freeform-introduction level (D-08), and freeform symmetry preferences (D-09) are left to implementation + playtest.
- Whether Results gains a `mode`/`showNextLevel` flag vs a separate Challenge-results route is an implementation choice for the planner (D-01/D-10 define the behavior).

### Deferred Ideas (OUT OF SCOPE)
- **Continue from highest Challenge level reached / resume** (CHAL-05, PERS-02) — Phase 5. Phase 3 tracks current level in-memory only.
- **Hints (recorded-line + solver fallback) and undo** (ASST-01..03) — Phase 4. These would rescue a stuck board; until then, stuck → Retry (D-10).
- **Leaderboards on highest level / best scores** — Slice 2. Enabled BY this phase's determinism, not built now.
- **Daily challenges** — deferred (Slice 2+).
- **Re-basing stars on speed/move-efficiency** — not this phase; D-11 keeps the star model, scales only the numeric score.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CHAL-01 | Player can play sequential Challenge levels numbered from 1 | `ChallengeProgress` in-memory holder + Challenge nav route carrying `levelNumber` (§Wiring); `LevelGenerator.generate(1)` onward (§Reverse-Generation) |
| CHAL-02 | Each Challenge level procedurally generated and guaranteed solvable | Reverse-generation from the solved state is solvable **by construction** and records the winning line (§Reverse-Generation, §Pattern 2) |
| CHAL-03 | Challenge level N deterministic — same board every time (carries `generatorVersion`) | Portable seed = `stableSeed(N)`, sorted candidate enumeration, single-threaded (§Determinism); locked by golden-master test for levels 1/2/10/100/1000 (§Validation Architecture); `generatorVersion` stamp (§Versioned Contract) |
| CHAL-04 | Difficulty scales with level number via board size/shape, peg density, starting pattern | Data-driven `DifficultyTier` table keyed by level ranges (§Difficulty Tier Table); solution-length primary lever (D-06), footprint size/freeform secondary (D-05/D-08) |
</phase_requirements>

## Summary

This phase is the project's **#1 risk**, but the hard part is almost entirely a *pure-Kotlin domain-layer algorithm problem* built on top of already-solid, verified seams. The existing `RulesEngine` already enumerates forward moves in a **deterministically sorted order** and operates on an immutable `Long`-bitmask `Board`; the Phase-1 golden-master harness (`BoardCodec` + `GoldenMasterAssert`, with an `UPDATE_GOLDEN` opt-in and an authoring-solver precedent) is designed to be reused verbatim; and the `LevelRepository` → `ChallengeLevelSource` seam already exists with a throwing stub that Phase 3 replaces. The generator does not need any new external dependency — `kotlin.random.Random(seed)` from the stdlib is the whole RNG story.

The correct architecture is **reverse-generation**: start from the solved 1-peg board and apply "un-jump" moves (each adds exactly one peg) driven by a level-seeded PRNG, until the recorded reverse-walk reaches the tier's target solution length. The starting board is solvable by construction and the winning line falls out for free as the reversed move list (which Phase 4 will consume for hints). Determinism is achieved by (a) a portable `stableSeed(levelNumber)` bit-mixing function, (b) enumerating reverse-move candidates into a sorted `List` before every PRNG draw, (c) single-threaded synchronous generation, and (d) locking the output with a golden-master CI test **before any difficulty tuning begins**. Quality gates (min solution length, spatial spread, branching-factor floor, forced-line rejection, symmetry-normalized dedup) reject-and-reseed using a *deterministic* salted sub-seed sequence so rejection never breaks determinism.

**Primary recommendation:** Build the generator as a pure-`:domain` `LevelGenerator` object that composes the existing `RulesEngine`, ships a `generatorVersion` stamp and a data-driven `DifficultyTier` table from day one, and is locked by a reused golden-master fixture set for levels 1/2/10/100/1000 **as the very first deliverable** — before any knob values are chosen. Model reverse-moves as ordinary forward `Move`s applied in reverse, so the recorded winning line needs no conversion and replays directly through `RulesEngine.applyMove`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Reverse-generation algorithm (`generate(levelNumber)`) | Domain (`:domain` generator) | — | Pure algorithm, zero Android deps, highest-value JVM test target (QUAL-01/04) |
| Deterministic seeding (`stableSeed`, RNG) | Domain (generator) | — | Portability is a pure-Kotlin concern; must not touch platform APIs |
| Inverse-move ("un-jump") math | Domain (generator) | Domain (`RulesEngine`) | Reuse RulesEngine's forward move math; keep forward+reverse rules co-located to avoid divergence |
| Solvability guarantee | Domain (by construction) | Domain (`Solver`, optional/test-time) | Reverse-generation guarantees it; a Solver is only needed for offline validation/Phase-4 fallback |
| `DifficultyTier` table + `generatorVersion` | Domain (data/config in generator package) | — | Data-driven constants (QUAL-03), frozen contract; no Android needed |
| Freeform footprint validity (connected, ≤64) | Domain (generator) | — | Operates on `BoardSpec`/`Position`; pure geometry/graph checks |
| Golden-master determinism test | Domain **test** source set | — | Reuses `BoardCodec`/`GoldenMasterAssert`; runs on JVM in CI |
| `ChallengeLevelSource` (level# → seed → generator) | Data (`:app` data) | Domain (generator) | Repository pattern: maps request to the domain generator behind `LevelRepository` |
| `ChallengeProgress` (current level N, advance) | Data/UI holder (`:app`) | — | In-memory this phase; Phase-5-persistable seam |
| Challenge nav route + gameplay reuse | UI (`:app` navigation + game) | — | New route carrying `levelNumber`; reuses Phase-2 `GameViewModel`/`BoardCanvas` |
| Challenge-aware Results (Next level vs Retry) | UI (`:app` results) | — | Add mode/`showNextLevel` awareness (D-01/D-10) |
| Level-scaled numeric score | Domain (`ScoreCalculator`) | UI (Results) | Additive multiplier over the pure scoring function (D-11) |

## Standard Stack

No new external dependencies are required for this phase. Everything the generator needs is either the Kotlin standard library or already-present, already-pinned project infrastructure.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `kotlin.random.Random` | Kotlin 2.3.20 stdlib | Seeded PRNG for the reverse walk and rejection salts | `Random(seedLong)` returns a `XorWowRandom` with a **specified, deterministic** algorithm — reproducible for a fixed seed on a fixed stdlib. `[CITED: kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/]` |
| `RulesEngine` (existing `:domain`) | in-repo | Forward-move authority reused for inverse-move enumeration | Already deterministically sorted; single source of jump rules (avoids forward/reverse divergence) `[VERIFIED: codebase]` |
| Immutable `Board` (`Long` bitmask, existing) | in-repo | Board state, free structural `equals`/`hashCode` for dedup + snapshotting | ≤64 cells; `copy`-based transforms never mutate (Pitfall 6) `[VERIFIED: codebase]` |
| JUnit4 + Google Truth | 4.13.2 / 1.4.x | Pure-JVM domain tests incl. golden-master | Already the domain test stack `[VERIFIED: codebase — build gate green]` |
| `BoardCodec` + `GoldenMasterAssert` (existing test harness) | in-repo | Byte-stable snapshot + committed-fixture assertion with `UPDATE_GOLDEN` opt-in | Explicitly designed to be reused verbatim by Phase 3 `[VERIFIED: codebase]` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `kotlinx-coroutines-test` (`runTest`) | 1.11.0 | ViewModel/repository coroutine tests for the Challenge flow | Testing `ChallengeProgress`/VM advance-on-solve |
| Turbine | 1.2.1 | Assert Challenge `UiState` emissions | If a Challenge ViewModel exposes a `StateFlow` beyond the reused `GameViewModel` |
| Hilt (`@Binds`, existing `DataModule`) | 2.60 | Swap `StubChallengeLevelSource` → real impl | The binding change is one line in `DataModule` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `kotlin.random.Random(seed)` | `java.util.Random(seed)` | `java.util.Random` is a JDK-specified linear-congruential generator (also portable); `kotlin.random.Random` is idiomatic and equally deterministic. Either is fine — the golden-master test is the ultimate lock. **Do not** use `Random.Default`/`ThreadLocalRandom` (unseeded, non-reproducible). |
| Reverse-generation (by construction) | Generate-then-Solver-check | REJECTED (Anti-Pattern 3): peg solitaire is NP-complete; online solvability checking is exponential and makes generation latency unbounded and non-deterministic. |
| `String.hashCode()` for seed | portable bit-mix of `N.toLong()` | `String.hashCode` is stable across JVMs but couples the seed to a string form and invites accidental non-portable custom hashing; an explicit splitmix-style mix of the `Int` level number is clearer and provably portable. |

**Installation:** None. (No `npm`/`pip`/`cargo` equivalent — Gradle version catalog unchanged; the generator is new source in `:domain`.)

## Package Legitimacy Audit

**Not applicable — this phase installs no external packages.** All code is new source in the existing `:domain` and `:app` modules using the standard library and already-pinned, already-verified project dependencies (Kotlin 2.3.20 stdlib, JUnit4, Truth, Hilt, coroutines-test, Turbine — all present and green in the current build gate). No registry lookup, slopcheck, or postinstall audit is warranted.

## Reverse-Generation: The Core Mechanism

### Inverse-move ("un-jump") definition, modeled as a forward `Move` applied in reverse

The cleanest, least error-prone representation reuses the existing `Move(from, over, to)` type and the existing forward math — **no separate `ReverseMove` type or `toForwardMove()` conversion is needed.**

A **forward** move `m = Move(from, over, to)` requires `from=Peg, over=Peg, to=Empty` and produces `from=Empty, over=Empty, to=Peg`. Its **inverse** (an "un-jump" that adds a peg) requires the *post*-state and restores the *pre*-state:

```kotlin
// Source: derived from RulesEngine.applyMove (verified codebase); net +1 peg.
// A Move m is "reverse-legal" on `board` iff, treating m as a forward move,
// the board currently looks like m's RESULT:  to=Peg, over=Empty, from=Empty
// (all three in-spec, colinear, orthogonal, spacing 2).
private fun isReverseLegal(board: Board, m: Move): Boolean =
    m.from in board.spec && m.over in board.spec && m.to in board.spec &&
        board.cellAt(m.to) == Cell.Peg &&
        board.cellAt(m.over) == Cell.Empty &&
        board.cellAt(m.from) == Cell.Empty

// Apply the inverse: to→Empty, over→Peg, from→Peg (exactly the reverse of applyMove).
private fun applyReverse(board: Board, m: Move): Board =
    board.withoutPeg(m.to).withPeg(m.over).withPeg(m.from)
```

Because a recorded reverse-move is *already a forward `Move`*, the winning line is simply the recorded reverse list **reversed** — and each entry replays directly through the unchanged `RulesEngine.applyMove`. This is the single most important design simplification in the phase: it eliminates a whole class of forward/reverse conversion bugs and makes the solution trivially replayable and snapshot-able with the existing `BoardCodec.encodeMoves`.

### Candidate enumeration MUST be deterministically sorted (the determinism linchpin)

`RulesEngine.legalMoves` sorts by `(from.row, from.col, to.row, to.col)`. The reverse-candidate enumerator must do the **same** so the PRNG index maps to the same move on every run and every device (Pitfall 1). Enumerate over `board.spec.inPlay` (already a stable list), for each in-play peg and each of the four fixed `DIRECTIONS`, and sort the resulting `List<Move>` with the identical comparator. **Never draw a PRNG index against a `Set`/`HashMap`/`buildSet`.**

```kotlin
// Source: mirrors RulesEngine.legalMoves ordering (verified codebase).
private fun legalReverseMoves(board: Board): List<Move> =
    buildList {
        for (to in board.spec.inPlay) {                 // `to` is the peg being un-jumped
            if (board.cellAt(to) != Cell.Peg) continue
            for (d in REVERSE_DIRECTIONS) {             // same 4 orthogonal vectors as RulesEngine
                val over = Position(to.row + d.rowDelta, to.col + d.colDelta)
                val from = Position(to.row + 2 * d.rowDelta, to.col + 2 * d.colDelta)
                val m = Move(from = from, over = over, to = to)
                if (isReverseLegal(board, m)) add(m)
            }
        }
    }.sortedWith(compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }))
```

### The generate loop

```kotlin
// Source: adapted from .planning/research/ARCHITECTURE.md Pattern 3 (HIGH-confidence in-repo research),
// grounded in the verified Board/BoardSpec/RulesEngine APIs.
fun generate(levelNumber: Int): GeneratedLevel {
    val tier = DifficultyTiers.forLevel(levelNumber)            // data-driven (D-07)
    var attempt = 0
    while (true) {
        val seed = stableSeed(levelNumber, salt = attempt)      // deterministic per attempt
        val rng = Random(seed)
        val footprint = pickFootprint(tier, rng)                // canonical early, freeform later (D-08)
        val target = pickTarget(footprint, tier, rng)           // final single-peg location
        var board = Board(spec = footprint, pegs = 0L).withPeg(target)
        val reverse = ArrayList<Move>(tier.solutionLength)
        while (reverse.size < tier.solutionLength) {
            val candidates = legalReverseMoves(board)
            if (candidates.isEmpty()) break                     // dead-end → this attempt fails a gate
            val m = candidates[rng.nextInt(candidates.size)]
            board = applyReverse(board, m)
            reverse += m
        }
        val startingBoard = board
        val solution = reverse.asReversed().toList()            // already forward Moves
        if (passesQualityGates(startingBoard, solution, tier)) {
            return GeneratedLevel(
                levelNumber = levelNumber,
                startingBoard = startingBoard,
                solution = solution,
                generatorVersion = GENERATOR_VERSION,
            )
        }
        attempt++
        if (attempt >= MAX_ATTEMPTS) return deterministicFallback(levelNumber, tier)  // see §Quality Gates
    }
}
```

**Guarantees:** solvable by construction; deterministic (same `levelNumber` → same board, verified by golden-master); difficulty scales via `tier`. `[CITED: .planning/research/ARCHITECTURE.md §Pattern 3]`

## Determinism (CHAL-03 — the primary risk)

### Portable seed derivation

Do **not** seed from `System.nanoTime`, identity `hashCode`, `UUID`, or `Random.Default`. Use an explicit, value-stable bit-mix of the level number (and the rejection salt). A splitmix64 finalizer is the standard portable choice:

```kotlin
// Source: splitmix64 finalizer (well-known constants); pure arithmetic → identical on JVM/ART.
private fun stableSeed(levelNumber: Int, salt: Int): Long {
    var z = (levelNumber.toLong() shl SALT_SHIFT) xor salt.toLong() + GOLDEN_GAMMA
    z = (z xor (z ushr 30)) * MIX_1
    z = (z xor (z ushr 27)) * MIX_2
    return z xor (z ushr 31)
}
```

All numeric constants (`GOLDEN_GAMMA`, `MIX_1`, `MIX_2`, shifts) live in a scoped constants object (QUAL-03), not inline. The **exact** mixing function becomes part of the frozen `generatorVersion` contract — changing it rewrites every level (Pitfall 2).

### The four determinism rules (Pitfall 1, verified against the golden-master approach)

1. **Sorted candidate lists before every PRNG draw** — never index into an unordered collection (§enumeration above).
2. **Specified-algorithm RNG** — `kotlin.random.Random(seedLong)`; never `Random.Default`/`ThreadLocalRandom`.
3. **Portable seed** — `stableSeed` bit-mix above; no string hashing, no system time.
4. **Single-threaded, synchronous generation** per level — no coroutine dispatch, no parallelism inside `generate`.

**RNG portability note `[ASSUMED → mitigated]`:** `kotlin.random.Random(Long)`'s `XorWowRandom` sequence is deterministic for a fixed seed and stdlib version; a future *Kotlin stdlib* change to the algorithm is the only realistic drift vector, and the golden-master test catches it immediately in CI. Treat the RNG choice as locked once the fixtures are committed. (If maximum insulation is wanted, a tiny in-repo splitmix64 `nextInt` removes even the stdlib dependency — optional; the golden-master already makes this moot.)

### Lock it FIRST (roadmap risk note)

The golden-master fixtures for levels **1, 2, 10, 100, 1000** must be committed and CI-green **before any `DifficultyTier` knob values are chosen or tuned.** No generator code ships without the determinism lock. See §Validation Architecture.

## Quality Gates (Pitfall 3 — reject degenerate boards WITHOUT breaking determinism)

Reverse-generation guarantees *a* solution exists, not that the puzzle is *good*. Apply gates in the generator's validation step; on failure, **reject and reseed with a deterministic salt** (the `attempt` counter feeding `stableSeed`), so the whole process stays reproducible.

| Gate | Check | Purpose |
|------|-------|---------|
| Min solution length | `solution.size >= tier.minSolutionLength` | Non-trivial depth (D-06 primary lever) |
| Peg-density bounds | `startingBoard.pegCount` within `tier.pegRange` (not near-full, not near-empty) | Avoids "already almost solved" and "no room to move" |
| Branching-factor floor | `RulesEngine.legalMoves(startingBoard).size >= tier.minOpeningMoves` | Opening isn't a single forced move |
| Forced-line rejection | Reject if the first *K* moves of the recorded solution are each the *only* legal move | Degenerate forced line feels scripted |
| Spatial spread | Bounding-box / centroid-spread metric ≥ `tier.minSpread` | Pegs not all clumped in one corner |
| Symmetry-normalized dedup | Canonical-form hash (min `pegs` mask under the footprint's dihedral group) not seen in a recent window | "Unique in the player's sequence" (the cheaper, intended meaning of unique) |

**Reuse precedent:** the committed `AuthoringSolver` already computes the exact **symmetry-canonical `pegs` key** (min mask under the D4 dihedral transforms of the bounding box) — lift that canonicalization for the dedup gate rather than re-deriving it. `[VERIFIED: codebase — AuthoringSolver.canonical/symmetryPermutations]`

**Bounded, deterministic fallback:** cap attempts (`MAX_ATTEMPTS`). If exhausted, do something *deterministic* — e.g. accept the best-scoring attempt seen, or deterministically reduce `tier.solutionLength` by one and retry — never loop unboundedly and never fall back to a non-deterministic source. Whatever the fallback, the golden-master fixture captures its output, so it is locked too.

**"Unique" is per-player-sequence, not global** (Pitfall 3): enforce dedup within a sliding window, not across the entire infinite set. Design tiny footprints to appear only in the first handful of levels (their state space is small; birthday collisions are real).

## Versioned, Frozen Generator Contract (CHAL-04 / Pitfall 2)

- **`GENERATOR_VERSION`** is a `const val` stamped onto every `GeneratedLevel` from day one. It flows through the data layer so a persisted resume (Phase 5) and future leaderboards (Slice 2) can detect a generator change.
- **Difficulty parameters are data-driven** (`DifficultyTier` table) so *all* tuning happens **before** the version is frozen.
- **After first public release, never mutate v1 in place.** A future change ships as a parallel `generatorV2` with a migration policy (e.g. v2 only for levels above the player's current max, or a new "season"). This is a Phase-5/Slice-2 concern but the *field must exist now*.

The model needs a field the current `Level` data class lacks. **Design decision for the planner** (see §Open Questions Q1): add a domain `GeneratedLevel(levelNumber, startingBoard, solution, generatorVersion)` in `:domain`, and either extend the data-layer `Level` (add nullable `solution`/`generatorVersion`) or return `GeneratedLevel` through a widened repository method. The `solution` must reach Phase 4 (hints) and the `generatorVersion` must reach Phase 5 (resume).

## Difficulty Tier Table (D-05/D-06/D-07 — VALUES deferred to playtest)

`DifficultyTier` is a data-driven table keyed by level ranges, resolved by `DifficultyTiers.forLevel(levelNumber)`. **The structure (knobs) is designed now; the values are chosen by playtest before freeze.**

Recommended knobs (fields), each a named constant, no inline literals (QUAL-03):

| Knob | Type | Drives | D-ref |
|------|------|--------|-------|
| `solutionLength` (target) + `minSolutionLength` (gate floor) | Int | **Primary** difficulty lever — depth of planning | D-06 |
| `footprintPool` | list of eligible footprints (canonical ids; freeform enabled above a threshold level) | Board size/shape variety | D-05/D-08 |
| `pegRange` | IntRange | Starting peg density bounds | D-04(spec)/gate |
| `minOpeningMoves` | Int | Branching-factor floor | gate |
| `minSpread` | Float/Int | Spatial-spread floor | gate |
| `freeformIntroLevel` | Int | Level at/after which freeform shapes may appear | D-08 |

**Curve shape (D-05/D-06):** a smooth, continuous function of `levelNumber` (e.g. solution length grows first and steadily; footprint size grows later and more slowly), **not** hard tier walls. Implement `forLevel` as a continuous mapping (interpolate knobs over ranges) rather than a stepped lookup, so difficulty reads as "endlessly, smoothly trickier." Exact growth rates are the playtest loop output — do not hardcode a guess as if final.

## Freeform Footprint Validity (D-09)

Freeform footprints are arbitrary connected shapes on the square grid, introduced at higher levels. Guardrails, all pure geometry/graph checks operating on a candidate `List<Position>`:

1. **≤ 64 in-play cells** — hard cap (the `Long` bitmask; `BoardSpec.size <= 64`).
2. **Connected** — orthogonal flood-fill (BFS/DFS over 4-neighbor adjacency) from any cell must reach every in-play cell; reject if any isolated cell/region exists.
3. **Square-grid coordinates only** — positions on the existing `(row, col)` lattice so `BoardGeometry.forCanvas` (which derives `gridDimension = max(maxRow, maxCol)+1`) and `BoardCanvas` render them **unchanged** `[VERIFIED: codebase — BoardGeometry.gridDimensionOf]`. Confirmed: geometry is fully general over any `BoardSpec` up to the grid it spans; no rendering change is needed for new shapes.
4. **Reverse-generable** — a footprint that admits no reverse walk to `tier.solutionLength` fails the quality gates and reseeds; connectivity plus a sane peg range makes this rare.
5. **Prefer symmetric/intentional shapes where cheap** (D-09) — bias `pickFootprint` toward shapes with dihedral symmetry (aesthetics); the canonicalization machinery already exists for scoring symmetry cheaply.

**Grid-dimension caution:** `BoardGeometry` sizes cells by `min(width,height)/gridDimension`. A freeform shape whose bounding box is much larger than its cell count will render small/sparse. Keep freeform bounding boxes proportionate (a `tier` knob can cap bounding-box dimension).

## Wiring (Integration — the vertical slice)

The MVP mode requires **thin end-to-end slices**, not horizontal layers. A viable first slice: *generator with golden-master lock → `ChallengeLevelSource` behind `LevelRepository` → Challenge entry from Home → play level 1 → Results with "Next level" → level 2.* Difficulty tuning is a later slice once the lock is green.

| Seam | Current state (verified) | Phase-3 change |
|------|--------------------------|----------------|
| `ChallengeLevelSource.load(levelNumber): Level` | interface exists; `StubChallengeLevelSource` **throws** `[VERIFIED]` | Replace with real impl wrapping the domain `LevelGenerator`; map level# → `stableSeed` → `generate` → `Level` (+solution/version) |
| `DataModule` Hilt binding | `@Binds StubChallengeLevelSource` `[VERIFIED]` | Rebind to the real `ChallengeLevelSource` impl (one line) |
| `DomainModule` | provides `RulesEngine`, `ScoreCalculator`, `TimeSource` `[VERIFIED]` | Provide `LevelGenerator` (object or class) if injected; it composes `RulesEngine` |
| Nav `Routes.kt` | `Gameplay(levelId: String)`, `Results(levelId, stars, pegsRemaining, elapsedSeconds, score)` `[VERIFIED]` | Add a Challenge gameplay route carrying `levelNumber: Int`; extend Results with mode/`showNextLevel` + `nextLevelNumber` (or a parallel Challenge-results path — planner's choice per D-01/D-10) |
| `GameViewModel` | loads board via `levelRepository.loadClassic(levelId)`; reads `Gameplay(levelId)` `[VERIFIED]` | Must load Challenge boards via `loadChallenge(levelNumber)`. It is currently **Classic-specific**; generalize the load path (unified `LevelRef`, or a Challenge branch/second VM). See Q2. |
| `GameViewModel.performJump` finish → `_finished` (`ScoreResult`) → `GameRoute` `LaunchedEffect` → `Results` `[VERIFIED]` | On Won → carry Challenge context so Results shows "Next level"→N+1; on Stuck → Retry only (D-10) |
| `HomeScreen` `ChallengeTile` | disabled, "Coming soon" badge, no route `[VERIFIED]` | Enable; navigate into Challenge at the current `ChallengeProgress` level (level 1 this phase, D-04) |
| `ChallengeProgress` | does not exist | New in-memory holder (current level N, advance-on-solve); **Phase-5-persistable seam**, no DataStore now (D-04) |
| `ScoreCalculator.evaluate(...)` | pure; `score()` = peg base + decayed time bonus `[VERIFIED]` | Add level-scaled numeric score: a difficulty/level multiplier applied over the base (keep stars unchanged, D-11). Keep the multiplier a *pure* parameter passed in — do not couple `ScoreCalculator` to level state. |

**Anti-pattern to avoid (verified in-repo guidance):** do NOT let `if (challenge) … else …` branching leak into UI/use-cases (Anti-Pattern 2). Routing stays inside `LevelRepositoryImpl`. The UI speaks a `LevelRef`/route; the repository picks the source.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Jump rule / legal-move math | A second reverse-rules engine | Compose `RulesEngine` (reverse = its inverse) | One source of jump truth; forward/reverse divergence is a subtle correctness bug (Integration boundary note) |
| Board snapshot / diffable encoding | New serializer for fixtures | `BoardCodec.encodeBoard`/`encodeMoves` | Byte-stable, human-diffable, already the golden-master contract |
| Golden-fixture assert + update workflow | New assert helper | `GoldenMasterAssert.assertMatches` + `UPDATE_GOLDEN` | Handles missing fixture, `.actual` sidecar, opt-in regen |
| Symmetry canonical form (dedup) | New dihedral canonicalizer | Lift `AuthoringSolver.canonical`/`symmetryPermutations` | Already correct and tested for these footprints |
| PRNG | Custom LCG from memory | `kotlin.random.Random(seed)` (or a tiny documented splitmix64) | Specified, deterministic; custom RNGs are a determinism footgun |
| Online solvability check | Generate-then-solve retry loop | Reverse-generation (solvable by construction) | NP-complete search; unbounded/non-deterministic latency (Anti-Pattern 3) |
| Board bitmask ops | New occupancy structure | Existing `Board` (`withPeg`/`withoutPeg`/`pegCount`) | Immutable, `copy`-based, StateFlow-safe (Pitfall 6) |

**Key insight:** nearly every "hard" primitive this phase needs (sorted move enumeration, immutable bitmask board, byte-stable snapshotting, golden-master asserting, symmetry canonicalization) **already exists and was built with Phase 3 explicitly in mind.** The genuinely new code is small: the reverse-walk loop, `stableSeed`, the `DifficultyTier` table, the quality gates, and the wiring.

## Common Pitfalls

### Pitfall 1: Non-deterministic generation despite a "seeded" PRNG
**What goes wrong:** Level N differs across devices/runs/refactors; retry/resume/leaderboard fairness silently breaks.
**Why:** PRNG draws indexed against unordered collections; non-portable seed; parallelism.
**How to avoid:** Sorted candidate lists, `Random(stableSeed)`, single-threaded (§Determinism). **Golden-master test is the single most valuable test in the project.**
**Warning signs:** any `HashSet`/`buildSet`/`Map` iteration feeding a PRNG index; a test that passes locally but flakes in CI.

### Pitfall 2: Changing the generator after release rewrites every level
**What goes wrong:** A post-launch knob tweak changes level N for everyone; resumes/best-times/leaderboards now refer to a different board.
**How to avoid:** `generatorVersion` from day one; data-driven knobs tuned pre-freeze; never mutate v1 in place; persist the *board*, not just N (Phase 5).
**Warning signs:** "improve difficulty" scheduled post-launch with no version gate; no `generatorVersion` field.

### Pitfall 3: Trivial / degenerate / duplicate boards
**What goes wrong:** Solvable but obvious (near-linear), clumped, or colliding across seeds.
**How to avoid:** Quality gates + deterministic reseed (§Quality Gates); symmetry-normalized dedup within a window; tiny footprints only for the first few levels.
**Warning signs:** no rejection loop; no dedup; playtest "these all feel the same."

### Pitfall 6: `Board` mutated in place → StateFlow doesn't recompose
**Already prevented** by the immutable `Long`-bitmask `Board` `[VERIFIED]`. Keep the generator purely `copy`-based (it is, via `withPeg`/`withoutPeg`). Do not introduce a mutable scratch board that leaks into UI state.

### Pitfall (generation latency): rejection loop with weak seeding
**What goes wrong:** Slow level load or an infinite loop when gates are tight on a small footprint.
**How to avoid:** Bounded `MAX_ATTEMPTS` + deterministic fallback; cap footprint size; keep gates achievable for the tier's footprint pool.

### Domain purity leak (QUAL-01)
**What goes wrong:** `android.*`/`Log`/`Context`/`Dispatcher` creeping into the generator kills JVM-testability.
**How to avoid:** Generator lives in `:domain`, zero Android imports; the `DomainPurityTest` boundary gate already enforces this `[VERIFIED: codebase]`. Seeds/level numbers are plain `Int`/`Long`; no platform APIs.

## Code Examples

### Reusing the golden-master harness for generator determinism (test source set)
```kotlin
// Source: pattern of the existing GoldenMasterTest (verified codebase), applied to generated levels.
class ChallengeGeneratorGoldenTest {
    @Test fun level_1_2_10_100_1000_are_deterministic() {
        listOf(1, 2, 10, 100, 1000).forEach { n ->
            val level = LevelGenerator.generate(n)
            val encoded = buildString {
                append("level=").append(n).append('\n')
                append("generatorVersion=").append(level.generatorVersion).append('\n')
                append(BoardCodec.encodeBoard(level.startingBoard)).append('\n')
                append("solution:\n").append(BoardCodec.encodeMoves(level.solution))
            }
            GoldenMasterAssert.assertMatches("challenge_level_$n.txt", encoded)
        }
    }
}
```

### Solvability-by-construction invariant (test)
```kotlin
// Every generated level's recorded solution must fold its starting board to one peg,
// with each step legal on the replayed board — mirrors SolutionTest.assertReplayReducesToOnePeg.
@Test fun everyGeneratedSolutionReducesToOnePeg() {
    (SAMPLE_LEVELS).forEach { n ->
        val level = LevelGenerator.generate(n)
        val finalBoard = level.solution.fold(level.startingBoard) { b, m ->
            assertThat(m in RulesEngine.legalMoves(b)).isTrue()   // legal at each step
            RulesEngine.applyMove(b, m)
        }
        assertThat(finalBoard.pegCount).isEqualTo(1)              // solved
    }
}
```

## State of the Art

| Old Approach | Current Approach | When | Impact |
|--------------|------------------|------|--------|
| Generate random board, run solver to verify | Reverse-generation (solvable by construction) | established technique | No exponential online search; free recorded solution |
| Unseeded/`nanoTime` seeding | Portable `stableSeed(N)` + specified RNG | — | Cross-device reproducibility |
| Solver-heavy generation | Solver reserved for offline validation / Phase-4 fallback only | this phase | Keeps generation O(reverse-walk length) |

**Deprecated/outdated for this phase:**
- Generate-then-check-solvability loops — rejected (Anti-Pattern 3).
- Any per-level Solver call on the generation path — the recorded line makes it unnecessary here (Solver is a Phase-4 concern).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `kotlin.random.Random(Long)` (XorWow) is stable for a fixed seed across JVM/ART and the pinned Kotlin 2.3.20 stdlib | Determinism | LOW — golden-master test catches any drift immediately in CI; mitigated by committing fixtures before tuning, or by an in-repo splitmix64 |
| A2 | The recorded reverse list, reversed, replays directly through `RulesEngine.applyMove` with no conversion | Reverse-Generation | LOW — provable from `applyMove`/`applyReverse` symmetry; covered by the solvability invariant test |
| A3 | `BoardGeometry`/`BoardCanvas` render arbitrary connected freeform footprints unchanged (they generalize over `BoardSpec`) | Freeform | LOW — verified `gridDimensionOf` derives from spec bounds; add a Roborazzi/geometry test for a sample freeform to confirm |
| A4 | The difficulty curve should be a *continuous* mapping over level number rather than stepped tiers | Difficulty Tier Table | MEDIUM — this is a design interpretation of D-05 "smooth, no hard tier walls"; confirm with user during planning/playtest |
| A5 | `ChallengeProgress` can be a plain in-memory holder (no persistence) this phase | Wiring | LOW — explicitly stated by D-04 |

**These A-items should be surfaced to the planner; A4 in particular is a design choice the user may want to confirm.**

## Open Questions (RESOLVED)

1. **Where does the recorded `solution` + `generatorVersion` live in the data model?**
   - Known: current data-layer `Level` has `id/displayName/spec/initialBoard/holeCount` — no solution/version field `[VERIFIED]`. `LevelRepository.loadChallenge(levelNumber): Level`.
   - Unclear: extend `Level` (nullable `solution`/`generatorVersion`) vs. return a richer `GeneratedLevel` vs. a widened repo method.
   - Recommendation: introduce a `:domain` `GeneratedLevel(levelNumber, startingBoard, solution, generatorVersion)`; carry `solution`+`generatorVersion` through to the ViewModel now (even if unused until Phase 4/5) so the seam exists. Planner decides the exact repo signature.
   - **RESOLVED: nullable `solution`/`generatorVersion` extension to data-layer `Level` (03-03 Task 1).**

2. **How does gameplay load a Challenge board given `GameViewModel` is Classic-specific today?**
   - Known: `GameViewModel` reads `Gameplay(levelId).levelId` and calls `loadClassic` `[VERIFIED]`.
   - Unclear: unify under a `LevelRef` sealed type (Classic(id)/Challenge(n)) and one route, vs. a second Challenge route + a load branch, vs. a separate Challenge VM.
   - Recommendation: a unified `LevelRef` in the route arg keeps one `GameViewModel` and matches ARCHITECTURE.md Pattern 2; but a second route + branch is a smaller diff. Planner's call (D-01/D-10 leave Results structure open too).
   - **RESOLVED: single `GameViewModel` with a `challengeLevel: Int?` route arg branch (03-04 Task 1).**

3. **Exact difficulty knob values** — explicitly a playtest-iteration loop (D-07). Not resolvable by research. The generator + golden-master + tier *structure* ship first; values are tuned before `generatorVersion` freeze.
   - **RESOLVED (deferred per D-07): the data-driven `DifficultyTier` structure ships now; knob VALUES are a playtest-iteration loop tuned before `generatorVersion` is frozen.**

## Environment Availability

No new external tools/services. The phase is pure JVM/Kotlin + Gradle + the existing test stack, all present and green in the current build gate. Step 2.6: no blocking dependencies.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK / Gradle toolchain | domain build + tests | ✓ | JDK 17 / Gradle 9.5.1 (per CLAUDE.md) | — |
| JUnit4 + Truth | golden-master + invariant tests | ✓ | 4.13.2 / 1.4.x | — |
| Kotlin stdlib `kotlin.random` | seeded RNG | ✓ | 2.3.20 | in-repo splitmix64 (optional) |

## Validation Architecture

> `workflow.nyquist_validation` is `true` `[VERIFIED: .planning/config.json]` — this section applies.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Google Truth 1.4.x (pure-JVM `:domain`); Robolectric/Compose UI test in `:app` for wiring |
| Config file | Gradle version catalog (`gradle/libs.versions.toml`); no separate test runner config |
| Quick run command | `./gradlew :domain:test` |
| Full suite command | `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` (the established Phase-1/2 gate) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHAL-03 | Levels 1/2/10/100/1000 byte-identical to committed fixtures | golden-master (JVM) | `./gradlew :domain:test --tests '*ChallengeGeneratorGoldenTest*'` | ❌ Wave 0 |
| CHAL-02 | Every sampled generated level's solution folds to one peg, each step legal | invariant (JVM) | `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` | ❌ Wave 0 |
| CHAL-03 | Regenerating level N twice yields identical `Board.pegs` + solution | determinism (JVM) | same suite | ❌ Wave 0 |
| CHAL-04 | Difficulty knobs monotonic/continuous over level number; freeform gated by intro level | tier table (JVM) | `./gradlew :domain:test --tests '*DifficultyTierTest*'` | ❌ Wave 0 |
| CHAL-04 | Quality gates reject trivial/degenerate/duplicate boards deterministically | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ❌ Wave 0 |
| CHAL-01 | Freeform footprints are connected + ≤64 cells | validity (JVM) | same suite | ❌ Wave 0 |
| CHAL-01 | Advance-on-solve → level N+1; stuck → Retry only (no Next) | ViewModel/nav (Robolectric) | `./gradlew :app:test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (fast JVM; includes golden-master + invariants).
- **Per wave merge:** full gate `ktlintCheck detekt :domain:test :app:test assembleDebug`.
- **Phase gate:** full suite green before `/gsd:verify-work`; **golden-master fixtures committed and green BEFORE any difficulty-knob tuning task starts** (roadmap risk note — non-negotiable ordering).

### Wave 0 Gaps
- [ ] `ChallengeGeneratorGoldenTest.kt` + committed fixtures `challenge_level_{1,2,10,100,1000}.txt` — covers CHAL-03 (**must land first**)
- [ ] `ChallengeSolvabilityTest.kt` — solvability-by-construction invariant, covers CHAL-02
- [ ] `DifficultyTierTest.kt` — tier structure/monotonicity, covers CHAL-04
- [ ] `QualityGateTest.kt` — reject-and-reseed determinism, covers CHAL-04/Pitfall 3
- [ ] Freeform validity tests (connected, ≤64) — covers CHAL-01/D-09
- [ ] `:app` Challenge wiring test (advance-on-solve, stuck→Retry) — covers CHAL-01/D-01/D-10
- [ ] Reuse (no new infra): `BoardCodec`, `GoldenMasterAssert`, `UPDATE_GOLDEN` workflow already exist

## Security Domain

> `security_enforcement` key absent in config → treated as enabled. This is an **offline, single-player, no-network, no-auth, no-PII** puzzle game; most ASVS categories are N/A. The relevant surface is input validation of untrusted nav args and generator integrity.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No accounts/sign-in in Slice 1 |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No multi-user/authz |
| V5 Input Validation | yes | Validate `levelNumber` from nav args: reject/clamp `< 1`; the generator must not crash or hang on adversarial N. `BoardGeometry.positionAt` already bounds-checks taps `[VERIFIED]`. |
| V6 Cryptography | no | No secrets; the "seed" is not a security secret. Do NOT treat determinism as tamper-proofing. |

### Known Threat Patterns for this stack
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Adversarial/out-of-range `levelNumber` nav arg | Tampering/DoS | Validate + clamp at the route/VM boundary; bounded `MAX_ATTEMPTS` so generation cannot hang |
| Local save tampering to inflate progress (future) | Tampering | Out of scope for single-player Slice 1; if leaderboards matter (Slice 2), validate server-side, don't obfuscate locally (Pitfall — Security/Integrity) |
| Generator change silently altering "level N" | Integrity | `generatorVersion` stamp + frozen contract (Pitfall 2) |

## Project Constraints (from CLAUDE.md)

Directives the planner must honor (same authority as locked decisions):
- **Pure-Kotlin domain, zero Android deps** — `LevelGenerator`/`DifficultyTier`/seed/gates live in `:domain`; enforced by the existing `DomainPurityTest` boundary gate (QUAL-01).
- **No magic numbers/strings** — seed constants, tier knobs, gate thresholds, `generatorVersion` all as scoped, KDoc'd `const val`s split by scope (QUAL-03). The `DifficultyTier` table lives in data/config, not inline literals.
- **dimens for all UI dp/sp** — any new Challenge UI (top-bar "Level N", Results "Next level" button) uses `res/values/dimens.xml`, no hardcoded dp/sp.
- **detekt + ktlint pass as a build gate** — including `MagicNumber` (see ScoreParams precedent).
- **MVVM + StateFlow, state hoisting, stateless composables** — Challenge UI follows the Phase-2 conventions (Route → Screen → leaf; read-only `StateFlow`; one-shot nav via Channel).
- **Gitflow strictly** — work on a `feature/*` (config `gsd/phase-3-…`) branch off `develop`; no direct commits to `main`/`develop`.
- **Reuse, don't reimplement** — `RulesEngine`, `Board`, `BoardCodec`, `GoldenMasterAssert`, `AuthoringSolver` canonicalization, `ScoreCalculator`.

## Sources

### Primary (HIGH confidence)
- Codebase (verified this session): `RulesEngine.kt`, `Board.kt`, `BoardSpec.kt`, `Move.kt`, `Boards.kt`, `ScoreCalculator.kt`/`ScoreParams.kt`/`ScoreResult.kt`, `BoardCodec.kt`, `GoldenMasterAssert.kt`, `GoldenMasterTest.kt`, `AuthoringSolver.kt`/`SolutionTest.kt`/`RecordedSolutions.kt`, `LevelRepository.kt`, `ChallengeLevelSource.kt`, `ClassicLevelSource.kt`, `DataModule.kt`/`DomainModule.kt`, `Routes.kt`, `AppNavHost.kt`, `GameViewModel.kt`/`GameUiState.kt`/`GameRoute.kt`, `BoardGeometry.kt`, `HomeScreen.kt`, `ResultsViewModel.kt`/`ResultsUiState.kt`
- `.planning/research/ARCHITECTURE.md` §Pattern 3 (reverse-generation), §Pattern 2 (one repository), Anti-Patterns 2/3/4 — HIGH
- `.planning/research/PITFALLS.md` Pitfalls 1/2/3/6 + "Looks Done But Isn't" — HIGH
- `.planning/REQUIREMENTS.md` (CHAL-01..04, QUAL-01/03/04), `.planning/ROADMAP.md` §Phase 3 Risk note, `.planning/PROJECT.md` Key Decisions, `.planning/STATE.md` — HIGH
- CONTEXT.md (D-01..D-11) — the locked contract

### Secondary (MEDIUM confidence)
- `kotlin.random.Random(Long)` determinism — `[CITED: kotlinlang.org/api/latest/jvm/stdlib/kotlin.random/-random/]` (training knowledge; the golden-master test is the enforcing lock)
- Peg-solitaire GF(4) invariant / NP-completeness / pagoda functions — cited in PITFALLS.md sources (Bell 2007; Jaap Scherphuis) — relevant to *why* reverse-generation (not search) is used

### Tertiary (LOW confidence)
- None material — the algorithm is guaranteed by construction and every claim is either verified in-repo or backed by the enforcing golden-master test.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new deps; all reuse verified in-repo.
- Architecture (reverse-generation, determinism, wiring): HIGH — verified against committed source and in-repo HIGH-confidence research; solvability is by construction.
- Difficulty knob VALUES: N/A — explicitly deferred to a playtest loop (D-07); only the *structure* is designed here.
- Pitfalls: HIGH — cross-referenced with the in-repo PITFALLS catalogue and the existing immutable-board/sorted-enumeration guards.

**Research date:** 2026-07-01
**Valid until:** ~2026-08-01 (stable; the only external variable is the Kotlin stdlib RNG, and the golden-master test makes even that safe once fixtures are committed)
</content>
</invoke>
