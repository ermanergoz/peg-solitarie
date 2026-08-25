---
phase: 01-domain-foundation-playable-board
plan: 02
subsystem: domain
tags: [kotlin, pure-jvm, domain-model, rules-engine, bitmask, golden-master, tdd, junit4, truth]

# Dependency graph
requires:
  - 01-01 — pure-JVM :domain module + JUnit4/Truth wired + detekt/ktlint/ArchUnit gates
provides:
  - Immutable English-33 domain model (Position, Cell, GameStatus, Move, BoardSpec, Board, GameState)
  - Board backed by a Long peg-bitmask behind cellAt/withPeg/withoutPeg/pegCount accessors
  - Boards.English33 spec + Boards.english33Initial (center-empty, 32 pegs) — D-01
  - RulesEngine: legalMoves (deterministic sorted) / applyMove / isGameOver / pegCount
  - Reusable golden-master harness (BoardCodec + GoldenMasterAssert) + 4 committed fixtures
affects: [01-03 GameViewModel/UiState, 01-04 BoardCanvas, 01-05 game-flow, 03 generator/solver (reuses harness verbatim)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Long peg-bitmask board encapsulated behind Board accessors (free equals/hashCode, swappable representation)
    - Deterministic sorted move enumeration (from.row,from.col,to.row,to.col) established now to de-risk Phase-3 determinism
    - Scoped layout constants (English33Layout) + named direction/span constants — zero magic numbers in domain logic
    - Golden-master codec (ASCII grid + pegs-hex) with UPDATE_GOLDEN opt-in regeneration

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Position.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Cell.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/GameStatus.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Move.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardSpec.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Board.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/GameState.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/rules/RulesEngine.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/model/BoardTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/rules/RulesEngineTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/BoardCodec.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/GoldenMasterAssert.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/GoldenMasterTest.kt
    - domain/src/test/resources/golden/english33_initial.txt
    - domain/src/test/resources/golden/english33_initial_moves.txt
    - domain/src/test/resources/golden/english33_scripted_line.txt
    - domain/src/test/resources/golden/english33_terminal.txt
  modified: []
  removed:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/Placeholder.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/PlaceholderTest.kt

key-decisions:
  - "Board occupancy is a single Long peg-bitmask over BoardSpec's stable (row,col)-sorted bit indices — encapsulated behind cellAt/withPeg/withoutPeg so the representation stays swappable"
  - "RulesEngine.legalMoves returns a deterministically sorted List (from.row,from.col,to.row,to.col) — establishes the stable-enumeration discipline Phase 3's generator determinism depends on"
  - "Golden-master fixtures generated once via UPDATE_GOLDEN=true then committed; a normal run never overwrites them and writes a .actual file on mismatch"
  - "Removed the Plan-01 Placeholder (its stated successor — the real model — now exists)"

requirements-completed: [GAME-01, GAME-02, QUAL-04]

# Metrics
duration: 30min
completed: 2026-06-28
---

# Phase 1 Plan 02: Pure-Kotlin Domain Core & Golden-Master Harness Summary

**The pure-JVM English-33 domain — immutable `Board` over a `Long` peg-bitmask plus a stateless `RulesEngine` (deterministic sorted `legalMoves`, immutable `applyMove`, `isGameOver`) — is fully unit-tested and locked by a reusable golden-master harness over four committed fixtures, with zero Android dependencies.**

## Performance

- **Duration:** ~30 min
- **Tasks:** 3 (2 TDD + 1 harness)
- **Files:** 18 created, 2 removed
- **Commits:** 6 task commits (RED/GREEN pairs + style + harness) + final docs commit

## Accomplishments

- **Immutable domain model (GAME-01 foundation):** `Position`/`Cell`/`GameStatus`/`Move`/`GameState` as immutable `data class`/`enum`; `BoardSpec` assigns each in-play position a stable `(row,col)`-sorted bit index; `Board` backs occupancy with a single `Long` and exposes only `cellAt`/`withPeg`/`withoutPeg`/`pegCount`. `withPeg(p).withoutPeg(p)` round-trips to an equal board and never mutates in place (T-02-02 mitigated by test).
- **Canonical English-33 (D-01):** `Boards.English33` is the 7x7 lattice minus the four 2x2 corners (33 holes); `Boards.english33Initial` is center-empty with `pegCount == 32`. All geometry literals live in a scoped `English33Layout` object — no magic numbers.
- **RulesEngine (GAME-01, GAME-02):** `legalMoves` enumerates orthogonal spacing-2 jumps over a fixed direction order and returns a deterministically sorted list; `applyMove = withoutPeg(from).withoutPeg(over).withPeg(to)` returning a new board; `isGameOver = legalMoves().isEmpty()`. Initial position yields exactly the 4 expected center-bound moves in stable order.
- **Golden-master harness (QUAL-04):** `BoardCodec` (row-major ASCII grid + `pegs` hex), `GoldenMasterAssert` (fixture compare, `.actual` on mismatch, `UPDATE_GOLDEN` opt-in), and `GoldenMasterTest` snapshotting the 4 Phase-1 targets (initial board, sorted legalMoves, a 5-step scripted line, and the played-out terminal + `gameOver`/`pegCount`). Phase 3 plugs generator output into this unchanged.
- **`:domain` stays pure-JVM:** the `:app` ArchUnit `DomainPurityTest` is green; full gate `./gradlew ktlintCheck detekt :domain:test test` exits 0.

## Task Commits

1. **Task 1 — Immutable English-33 model** — RED `cccf2b6` (test), GREEN `a3055ea` (feat, incl. Placeholder removal)
2. **Task 2 — RulesEngine** — RED `0468283` (test), GREEN `b0dcee0` (feat); `c3e8477` (style — ktlint signature formatting surfaced by the wave gate)
3. **Task 3 — Golden-master harness + fixtures** — `420ae05` (test)

## Decisions Made

- **`Long` peg-bitmask board:** chosen for free collision-free `equals`/`hashCode` (golden-master diffing), trivial deterministic serialization, and forward-compat with the Phase-3 Solver/generator — encapsulated behind `Board` accessors so it remains swappable.
- **Deterministic sorted enumeration now:** Phase 1 has no PRNG, but sorting `legalMoves` is the cheapest de-risking of the Phase-3 determinism contract and makes the move-list fixture stable (RESEARCH Pitfall 1).
- **Greedy first-move scripted/terminal lines:** the scripted-line and terminal fixtures are produced by repeatedly applying `legalMoves().first()` — fully deterministic (because enumeration is sorted) and reproducible, terminating at 7 pegs.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] KDoc block comment closed early by a `**/test/**` glob**
- **Found during:** Task 1 (GREEN compile)
- **Issue:** A KDoc in `BoardTest.kt` referenced the detekt exclude glob `**/test/**`, whose embedded `*/` prematurely terminated the block comment → Kotlin syntax error.
- **Fix:** Reworded the comment to "the test source set" (no glob), preserving the intent.
- **Files modified:** domain/src/test/kotlin/com/app/pegsolitaire/domain/model/BoardTest.kt
- **Committed in:** `a3055ea`

**2. [Rule 3 - Blocking] detekt MaxLineLength + ktlint function-signature on new domain code**
- **Found during:** Task 3 wave-level gate (`detekt` + `ktlintCheck`)
- **Issue:** `GoldenMasterAssert.isUpdateEnabled()` exceeded MaxLineLength; ktlint's `function-signature` rule required multi-parameter signatures (`applyMove`, `assertMatches`) and a corner-position list to be wrapped. Per-task sampling is `:domain:test` only (per VALIDATION.md), so these surfaced at the wave gate as designed.
- **Fix:** Wrapped the long expression by hand; ran `:domain:ktlintFormat` to apply the canonical multi-line signature formatting.
- **Files modified:** GoldenMasterAssert.kt (hand), RulesEngine.kt + BoardTest.kt (ktlintFormat)
- **Committed in:** `c3e8477` (RulesEngine/BoardTest style), `420ae05` (GoldenMasterAssert in harness commit)

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking lint). No scope change; no must-have weakened.

## Authentication Gates

None — pure in-memory domain logic, no I/O, no network, no credentials.

## Known Stubs

None. The Plan-01 `Placeholder` stub was removed now that the real model exists.

## Threat Flags

None — no new security surface. The plan's `<threat_model>` dispositions hold: `RulesEngine` is the sole move authority (T-02-01 accept), and immutability is test-enforced (T-02-02 mitigate, `BoardTest.withPegThenWithoutPeg...`).

## Verification Evidence

- `./gradlew :domain:test` — all model, RulesEngine, and golden-master tests green.
- `./gradlew :domain:detekt` — green (no magic numbers in domain logic).
- `./gradlew ktlintCheck detekt :domain:test test` — exits 0, including the `:app` ArchUnit `DomainPurityTest` (`:domain` has zero `android.*`/`androidx.*`).
- Golden fixtures committed and stable: `GoldenMasterTest.reEncodingIsStableAcrossRepeatedRuns` asserts identical output across runs; fixtures were generated via `UPDATE_GOLDEN=true` then verified to pass on a normal run.

## Next Plan Readiness

- Plan 03 (`GameViewModel` + `GameUiState`) can consume `RulesEngine.legalMoves/applyMove/isGameOver` and `Boards.english33Initial` directly. Hilt + KSP are already applied (Plan 01 rework), so `@HiltViewModel` needs no further build setup.
- Phase 3 can reuse `BoardCodec` + `GoldenMasterAssert` verbatim, feeding generator output as new fixtures.

## Self-Check: PASSED

- All 18 created files exist on disk (verified); 2 placeholder files removed.
- All 6 task commits present (`cccf2b6`, `a3055ea`, `0468283`, `b0dcee0`, `c3e8477`, `420ae05`).
- `./gradlew ktlintCheck detekt :domain:test test` exits 0.

---
*Phase: 01-domain-foundation-playable-board*
*Completed: 2026-06-28*
