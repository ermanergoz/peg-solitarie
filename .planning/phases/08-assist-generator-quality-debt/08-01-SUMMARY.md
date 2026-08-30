---
phase: 08-assist-generator-quality-debt
plan: 01
subsystem: domain
tags: [kotlin, board-model, boardspec, diamond41, gf4-invariant, junit4, truth]

# Dependency graph
requires:
  - phase: 03-challenge-generator
    provides: FreeformFootprints / LevelGenerator constructing BoardSpecs at runtime
  - phase: 07-classic-catalog
    provides: the five shipped Classic footprints in Boards.kt
provides:
  - "BoardSpec.minimumPegs — a validated per-board 'perfect finish' peg target, defaulting to 1"
  - "BoardSpec.footprint — the sorted in-play list serving as the board's structural identity key"
  - "BoardConstants (MINIMUM_PEGS_DEFAULT, DIAMOND41_FEWEST_PEGS_FLOOR)"
  - "Boards.CLASSIC_SPECS — the five shipped footprints in D-18 ascending hole-count order"
  - "Boards.Diamond41 carrying its proven fewest-pegs floor of 2"
  - "Corrected Diamond-41 floor KDocs in domain/src/main (Pitfall 7 surface #1)"
affects: [solver-goal-test, pagoda-generalisation, diamond-floor-fixture, star-bands, opening-book, resume-snapshot-mapper]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Per-board targets are properties of a FOOTPRINT, never of a level id — :domain stays ignorant of ClassicLevelIds"
    - "Structural identity via a sorted List<Position> key rather than referential identity on a spec instance"
    - "Scoped model-layer constants object (BoardConstants), mirroring GeneratorConstants / ScoreParams"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardConstants.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/model/BoardSpecTest.kt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardSpec.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt

key-decisions:
  - "minimumPegs is the THIRD, last constructor parameter with a default, so all ~20 existing BoardSpec(...) call sites compile unchanged"
  - "footprint replaces the inline sort inside indexOf, so the (row, col) sort happens exactly once and the bit assignment is provably unchanged (golden masters byte-identical)"
  - "The 1..size validation is unconditional; the zero-cell loading placeholder in :app was fixed instead of carving out an exemption in the domain invariant"
  - "Diamond-41's floor constant is FROZEN by two proofs and must never be edited to make a test pass — D-05's escalation clause fires instead"

patterns-established:
  - "Footprint-keyed catalog lookup: Boards.CLASSIC_SPECS.associateBy { it.footprint } recognises a structurally-rebuilt spec"
  - "Assert BOTH the named constant and its literal value in tests that lock a frozen number — the constant proves the wiring, the literal proves the arithmetic"

requirements-completed: [DEBT-01, DEBT-02]

# Metrics
duration: 9 min
completed: 2026-08-28
---

# Phase 8 Plan 01: BoardSpec Per-Board Target and Footprint Key Summary

**`BoardSpec` now carries a validated per-board `minimumPegs` target (1 everywhere except Diamond-41's proven floor of 2) plus a sorted `footprint` structural identity key, with the five shipped Classic footprints published as `Boards.CLASSIC_SPECS`.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-08-28T23:19:01Z
- **Completed:** 2026-08-28T23:28:06Z
- **Tasks:** 3
- **Files modified:** 5 (2 created, 3 modified)

## Accomplishments

- **Unblocked the whole phase without a single call-site signature change.** `Solver`, `Pagoda`, `ScoreCalculator` and the opening book all already receive a `Board`, and `Board.spec` is right there — so every downstream plan reads `spec.minimumPegs` / `spec.footprint` with no new plumbing.
- **`BoardSpec.footprint` is now the single sort.** The old inline `sortedWith(compareBy(...))` inside `indexOf`'s initializer was hoisted into a public `footprint` property that `indexOf` derives from. `GoldenMasterTest` stayed green (5/5), proving the bit-index assignment — and therefore every recorded `Long` bitmask — did not shift.
- **Diamond-41's stale "floor is several pegs" claim is gone from `domain/src/main`.** Replaced with the proven `F = 2`: the GF(4) colour invariant excludes one peg exhaustively (0 of 41 single-peg positions pass), and two is reached by the golden line landing in 08-06. That is Pitfall 7 surface #1 of three.
- **`init { require(minimumPegs in 1..size) }`** closes T-08-01 as defence in depth: no path can smuggle a target that makes the solver's `pegCount <= minimumPegs` goal test trivially true and leave every hint permanently `null`.

## Task Commits

1. **Task 1 (RED): failing spec for minimumPegs and footprint** — `9e5e513` (test)
2. **Task 1 (GREEN): BoardConstants + BoardSpec target and footprint key** — `bc214e3` (feat)
3. **Deviation fix: well-formed loading placeholder footprint** — `c2febb9` (fix)
4. **Task 2: Diamond-41 target F=2, CLASSIC_SPECS catalog, corrected floor KDocs** — `198f90d` (feat)
5. **Task 3: lock per-board targets, catalog and freeform default** — `be54b4e` (test)

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardConstants.kt` — **created.** Scoped model-layer constants: `MINIMUM_PEGS_DEFAULT = 1` (D-01) and `DIAMOND41_FEWEST_PEGS_FLOOR = 2` (D-05), the latter KDoc'd with both halves of its proof and an explicit FROZEN warning.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardSpec.kt` — **modified.** Third defaulted `minimumPegs` parameter, public `footprint` structural key, `indexOf` derived from it, `init` validation block.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` — **modified.** `Diamond41` passes `minimumPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`; new `CLASSIC_SPECS` catalog; three KDoc surfaces corrected (object KDoc, `Diamond41Layout` KDoc, `startVacancy` KDoc).
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/model/BoardSpecTest.kt` — **created.** 14 tests: 8 locking the target/footprint contract, 6 locking the shipped catalog and the freeform default.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt` — **modified (deviation).** The loading placeholder's zero-cell spec became the smallest well-formed footprint.

## Decisions Made

- **`minimumPegs` is the last constructor parameter with a default.** Every one of the ~20 existing `BoardSpec(...)` sites — `Boards.kt` x5, `SnapshotMapper`, `FreeformFootprints`, `GameUiState`, `OnboardingOverlay`, and ~12 test sites — compiles untouched. Verified by running both full unit-test suites.
- **`footprint` replaces the sort rather than duplicating it.** A second `sortedWith` would have been a silent determinism hazard: two sorts that could drift apart would break the `Long`-bitmask contract the golden masters lock. There is now exactly one (`grep -c` returns 1, an explicit acceptance criterion).
- **The `1..size` invariant is unconditional.** When it turned out that `:app`'s zero-cell loading sentinel violated it, the choice was between weakening the domain invariant with an empty-footprint carve-out or fixing the sentinel. Fixing the sentinel keeps the invariant absolute and meaningful ("every `BoardSpec` admits an achievable finish"), and the placeholder is never rendered or played.
- **Tests assert both the named constant and the literal.** `Boards.Diamond41.minimumPegs` is asserted equal to `BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR` *and* to `2` — the first proves the wiring, the second proves the frozen value, and together they mean a future edit to the constant cannot silently pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Zero-cell loading placeholder tripped the new `BoardSpec` validation**

- **Found during:** Task 2 (running the plan-level `:app:testDebugUnitTest` verification)
- **Issue:** `GameUiState.EMPTY_SPEC = BoardSpec(inPlay = emptyList(), target = null)` produced `size == 0`, so `require(minimumPegs in 1..0)` threw inside the companion object's static initializer. This cascaded to `ExceptionInInitializerError` → `NoClassDefFoundError` in **102 of 358 `:app` tests**, all from that single root cause. Not a test-only problem: the same throw would have crashed the Challenge screen the moment `GameViewModel` emitted `loading()`, so this was a genuine runtime regression the plan's design introduced.
- **Fix:** Replaced the zero-cell sentinel with the smallest well-formed footprint (one cell at `(0,0)`, still `pegs = 0L`) and renamed it `PLACEHOLDER_SPEC`, with KDoc explaining why zero cells is not a legal footprint. The board is never rendered while `isLoading` (`GameScreen.kt:171` gates on it) and `pegCount` still reports 0, so there is no visual or semantic change.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest` — 358/358 pass (was 256/358).
- **Committed in:** `c2febb9`

**2. [Rule 3 - Blocking] Missing `local.properties` in the worktree**

- **Found during:** Task 2 verification
- **Issue:** `:app:testDebugUnitTest` could not configure — "SDK location not found". `local.properties` is gitignored, so the worktree checkout did not carry the main checkout's copy.
- **Fix:** Wrote the worktree's own `local.properties` pointing at the same `sdk.dir`. **Not committed** — the file is gitignored by design and is per-machine.
- **Files modified:** `local.properties` (untracked, intentionally)
- **Verification:** `:app:testDebugUnitTest` configures and runs.
- **Committed in:** n/a (gitignored)

### Out-of-scope, deferred (not fixed)

Pitfall 7's stale-floor claim survives on two **test-source** surfaces this plan does not own:
`AuthoringSolver.kt:239` and `SolutionTest.kt:23`. Both live in the recorded-solution fixture code
that plan **08-06** rewrites when it commits `diamond41.txt`; correcting them there avoids touching
those files twice and lets the wording reference a fixture that actually exists. Logged in
`.planning/phases/08-assist-generator-quality-debt/deferred-items.md`.

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking) + 1 logged and deferred.
**Impact on plan:** Both auto-fixes were necessary for correctness — deviation 1 in particular caught a real crash the plan's own design would have shipped. No scope creep: the `:app` change is one private sentinel property.

## Issues Encountered

**Task 3's RED gate passed on first run.** Task 3 is `tdd="true"`, but the behaviour it locks (`Diamond41.minimumPegs`, `CLASSIC_SPECS`) was shipped by Task 2 immediately before it. Per the fail-fast rule I stopped and investigated rather than proceeding blind: the plan deliberately structures Task 3 as a *characterization/lock* task over Task 2's output, not as a behaviour-adding task, so an immediately-green suite is the correct and expected outcome. It was committed as a single `test(08-01)` commit with no separate GREEN step. See TDD Gate Compliance below.

## TDD Gate Compliance

- **Task 1** — full RED → GREEN cycle observed. RED (`9e5e513`) failed with `Unresolved reference 'footprint'` / `'minimumPegs'` / `BoardConstants` at `:domain:compileTestKotlin`; GREEN (`bc214e3`) turned all 8 tests green. No REFACTOR step was needed.
- **Task 3** — lock-only task, no RED phase possible (see Issues Encountered). Committed as `test(08-01)`.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test --tests '*BoardSpecTest*'` | PASS — 14 tests, 0 failures |
| `./gradlew :domain:test --tests '*GoldenMasterTest*'` | PASS — 5 tests, bit assignment unchanged |
| `./gradlew :domain:test` (full suite) | PASS |
| `./gradlew :app:testDebugUnitTest` (full suite) | PASS — 358 tests |
| `./gradlew ktlintCheck detekt` | PASS |
| `grep -c 'sortedWith(compareBy(' BoardSpec.kt` | `1` — sort not duplicated |
| `grep -c 'several pegs\|~5 pegs' Boards.kt` | `0` |
| `grep -c 'F = 2\|DIAMOND41_FEWEST_PEGS_FLOOR' Boards.kt` | `7` (>= 2 required) |

## Success Criteria

- [x] `Boards.Diamond41.minimumPegs == 2`; the other four Classic specs and every generated Challenge footprint report `1`
- [x] `Boards.CLASSIC_SPECS` exposes the five shipped footprints in ascending hole-count order with pairwise-distinct `footprint` keys
- [x] `BoardSpec.footprint` is structurally equal across two independently-constructed specs of the same footprint
- [x] `BoardSpec` rejects `minimumPegs` outside `1..size`
- [x] No file under `domain/src/main` still claims Diamond-41's floor is "several pegs"

## User Setup Required

None — no external service configuration required. (One per-machine note: a fresh worktree needs its own gitignored `local.properties` with `sdk.dir` before `:app` tasks will configure.)

## Requirements Note

`requirements-completed` is copied verbatim from the plan frontmatter. **DEBT-02 is only partly
discharged here** — this plan lands the per-board target and the frozen `F = 2` constant, but the
committed recorded-solution fixture that proves reachability arrives in plan **08-06**. The
orchestrator should not mark DEBT-02 complete in `REQUIREMENTS.md` until 08-06 lands.

## Next Phase Readiness

Every downstream plan's precondition is satisfied:

- **Solver goal test** — read `board.spec.minimumPegs` instead of the hardcoded `1`.
- **Pagoda generalisation** — the per-board target is on the spec the pruner already holds.
- **Diamond floor fixture (08-06)** — `DIAMOND41_FEWEST_PEGS_FLOOR` is the value to assert the replayed line terminates at. Also correct the two deferred KDoc surfaces there.
- **Star bands** — re-anchor on `spec.minimumPegs` rather than the literal 1.
- **Opening book (08-09/08-10) and resume path (08-07)** — `Boards.CLASSIC_SPECS.associateBy { it.footprint }` is the lookup; `SnapshotMapper` remains the fail-closed decode boundary that must validate `minimumPegs` before construction.

No blockers.

## Self-Check: PASSED

- All 5 key files verified present on disk.
- All 5 task commits verified in `git log`.
- No file deletions introduced across the plan (`git diff --diff-filter=D 22de8ec..HEAD` empty).
- No stubs, placeholders, or TODOs introduced.
- No threat flags: no new network, auth, file-access, or schema surface. The plan's `<threat_model>` dispositions T-08-01 (mitigated by the `init` require) and T-08-03 (mitigated by the freeform assertion in Task 3) are both implemented.
- STATE.md and ROADMAP.md untouched, per parallel-execution contract.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-28*
