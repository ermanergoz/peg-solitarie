---
phase: 08-assist-generator-quality-debt
plan: 07
subsystem: scoring
tags: [kotlin, star-bands, diamond41, boardspec, resume-snapshot, kotlinx-serialization, junit4, truth, turbine]

# Dependency graph
requires:
  - phase: 08-assist-generator-quality-debt
    provides: "08-01 — BoardSpec.minimumPegs / BoardSpec.footprint, BoardConstants, Boards.CLASSIC_SPECS"
  - phase: 08-assist-generator-quality-debt
    provides: "08-06 — the PROVEN floor F = 2 (committed diamond41.txt witness + exhaustive GF(4) optimality)"
  - phase: 02-gameplay-scoring
    provides: "ScoreCalculator/ScoreParams and the one-shot finished-event contract"
  - phase: 05-persistence-resume
    provides: "SnapshotMapper's fail-closed decode boundary and the ResumeSnapshotDto on-disk format"
provides:
  - "ScoreCalculator.stars/evaluate anchored on the finishing board's own minimumPegs, defaulted to reproduce the global bands exactly"
  - "ScoreParams.TWO_STAR_BAND_WIDTH — the band SHAPE, with TWO_STAR_MAX_PEGS derived from it"
  - "GameViewModel threads nextBoard.spec.minimumPegs into the single production scoring call site"
  - "GameViewModel.statusFor's win test re-anchored on the per-board target (deviation — see below)"
  - "ResumeSnapshotDto.minimumPegs — a nullable, backward-compatible on-disk target"
  - "SnapshotMapper target resolution: validated when present, catalog-resolved when absent, refused when forged"
affects: [08-05-bitboard-solver, 08-09-opening-book, 08-10-opening-book, results-ui, play-games-leaderboards]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Band SHAPE as a constant, band ANCHOR as a parameter: one formula replaces a per-board threshold table"
    - "Nullable-with-default on-disk field where absent means 'predates the field', resolved from the catalog rather than defaulted"
    - "Fail-closed range validation before domain construction, so a require() can never become a crash on untrusted input"

key-files:
  created: []
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
    - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt

key-decisions:
  - "statusFor's win test was re-anchored on spec.minimumPegs (deviation): without it the threaded target is dead code for the only board it exists for, because a Diamond-41 two-peg finish was classified Stuck and emitted no finished event at all"
  - "The 5-peg Diamond behaviour is asserted as what actually happens — Stuck, no finished event, 1 star if it were scored — because emitting on Stuck would contradict D-06's rescuable dead end"
  - "The whole snapshot is refused for an out-of-range minimumPegs rather than clamped: clamping would repair a tampered blob into a playable one"
  - "The legacy-blob lookup key is BoardSpec.footprint, never a second sort written in the mapper — a duplicate comparator is the determinism hazard 08-01 warned about"
  - "score is NOT re-based on minimumPegs — D-11's independent-axes rule means a Diamond finish's number does not move by a point"

patterns-established:
  - "Consistency assertions pass the SAME per-board target the production code threads, so they cannot silently go tautological when a parameter is added"
  - "A legacy on-disk payload is exercised by a hand-written envelope that genuinely lacks the new key, with the footprint derived from the real board so a typo cannot flip which branch is under test"

requirements-completed: [DEBT-02]

# Metrics
duration: 12 min
completed: 2026-08-29
---

# Phase 8 Plan 07: Diamond-41 Star Band Re-Anchor and Resume Target Summary

**Star bands are now derived from the finishing board's own proven perfect finish rather than a global literal 1 — so Diamond-41's two-peg floor earns 3 stars and counts as a win end to end — and that target survives process death, including for blobs written before the field existed, while a forged one refuses the snapshot outright.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-08-29T00:04:55Z
- **Completed:** 2026-08-29T00:16:54Z
- **Tasks:** 3 (6 commits — all three tasks ran a full RED/GREEN cycle)
- **Files modified:** 9 (0 created)

## Accomplishments

- **Diamond-41 is now scored truthfully, end to end.** A player who reaches `{(2,2),(5,5)}` — the terminal 08-06 proved reachable and 08-06 proved optimal — sees a win and 3 stars. Before this plan the same player saw the *rescue banner*, no Results screen, and no score at all.
- **The four cross boards are proven byte-identical, not asserted to be.** The non-regression test is an explicit loop over `0..45` (Wiegleb-45's hole count is the largest footprint that ships) asserting the pre-phase piecewise function at *every* peg count, under both the explicit default target and the omitted one.
- **The band shape cannot drift from its anchor.** `TWO_STAR_MAX_PEGS` is now `THREE_STAR_MAX_PEGS + TWO_STAR_BAND_WIDTH` rather than a standalone `3`, and a test asserts both the derivation and the literal it still evaluates to.
- **Pitfall 3 is closed on both sides.** A modern blob carries the target explicitly; a *legacy* blob resolves it from `Boards.CLASSIC_SPECS` by footprint equality. Without the second half, every already-stored Diamond-41 game would have silently reverted to a one-peg target — permanently `null` hints and mis-banded stars — and `SnapshotMapperTest` is emphatically not untouched (it gained 8 cases).
- **The forged-target path fails closed with a boundary control.** `0`, `-1` and `size + 1` each refuse the whole snapshot; `1` and `size` each still decode. The pair matters: without the inclusive-boundary control the rejection could have been an off-by-one that quietly rejects legitimate saves.
- **No Room migration, as required.** `LevelResultEntity` does not persist `pegsRemaining`, so stored Diamond stars cannot be re-derived; they upgrade through the existing upsert-on-better path on next play.

## Task Commits

1. **Task 1 (RED): failing spec for per-board star bands** — `2ec472b` (test)
2. **Task 1 (GREEN): derive star bands from the board's own perfect finish** — `992a009` (feat)
3. **Task 2 (RED): failing spec for Diamond-41 star bands through the VM** — `2e0f6ad` (test)
4. **Task 2 (GREEN): score a finish against the finishing board's own target** — `a55ed8c` (feat)
5. **Task 3 (RED): failing spec for the board target across resume** — `55a0112` (test)
6. **Task 3 (GREEN): preserve the board target across resume, fail closed on a forged one** — `a520549` (feat)

_No REFACTOR step was needed on any task._

## Files Created/Modified

- `domain/.../scoring/ScoreParams.kt` — **modified.** New `TWO_STAR_BAND_WIDTH = 2` KDoc'd as the band SHAPE (D-04); `TWO_STAR_MAX_PEGS` redefined as `THREE_STAR_MAX_PEGS + TWO_STAR_BAND_WIDTH`; `THREE_STAR_MAX_PEGS` KDoc now records that it is also the DEFAULT target and coincides with `BoardConstants.MINIMUM_PEGS_DEFAULT`.
- `domain/.../scoring/ScoreCalculator.kt` — **modified.** `stars(pegsRemaining, minimumPegs = ...)` with the band test `<= minimumPegs` / `<= minimumPegs + TWO_STAR_BAND_WIDTH`; `evaluate` threads the same parameter into `stars` and deliberately not into `score`. Both KDocs plus the class KDoc name D-03/D-04/DEBT-02 and explain why the signature takes a peg COUNT rather than a board identity. The body stays free of literals.
- `domain/src/test/.../scoring/ScoreCalculatorTest.kt` — **modified.** 9 → 16 tests: the `0..45` sweep, five Diamond band-boundary cases, the stars-vs-score independence case, and the derived-constant lock.
- `app/.../ui/game/GameViewModel.kt` — **modified.** `minimumPegs = nextBoard.spec.minimumPegs` at the `evaluate` call site with a comment naming D-03/D-04 and the resume-path reason; `statusFor` re-anchored (deviation 1) and the `SOLVED_PEG_COUNT` magic literal deleted.
- `app/src/test/.../ui/game/GameViewModelTest.kt` — **modified.** 47 → 49 tests. Two real-`Boards.Diamond41` gameplay cases plus two board helpers; the existing consistency assertion now passes the board's own target.
- `app/src/test/.../ui/game/GameFlowTest.kt` — **modified.** The matching consistency assertion threads `nearWin.spec.minimumPegs`.
- `app/.../data/resume/ResumeSnapshotDto.kt` — **modified.** `minimumPegs: Int? = null` with a KDoc stating the on-disk contract: absent means "predates per-board targets", never "the target is 1".
- `app/.../data/resume/SnapshotMapper.kt` — **modified.** `toDto` writes the target; `toBoardSpecOrNull` resolves and validates it before construction via a new `resolveMinimumPegsOrNull`; class KDoc extended with the new validation and the consequence a forged value would have had.
- `app/src/test/.../data/resume/SnapshotMapperTest.kt` — **modified.** 17 → 25 tests, plus a `legacyBlobFor` helper and a `snapshotOf(Board)` overload.

## Decisions Made

- **The band anchor is a parameter; the band width is a constant.** RESEARCH's Pattern 5 sketched a `DiamondStarParams` object with its own `THREE_STAR_MAX_PEGS`/`TWO_STAR_MAX_PEGS`. I implemented the one-formula form the same section recommends instead: a per-board constants object would have been a second copy of the band *shape* that could drift from the global one, which is exactly the failure the derived `TWO_STAR_MAX_PEGS` exists to prevent. One shape, one anchor per board.
- **`score` is untouched by the target.** D-11 makes stars and score independent axes, and a test asserts a Diamond 2-peg finish scores identically under both targets while its stars move 2 → 3. Re-basing the score would have silently changed every Diamond best.
- **The legacy-blob test derives its footprint from `Boards.diamond41Initial` rather than transcribing 41 coordinate pairs.** The plan asked for a hand-written literal; the load-bearing part of that request is that the payload genuinely *lacks* the `minimumPegs` key, which it does — the envelope is hand-written. Transcribing the footprint would have risked a typo silently converting the catalog-resolution test into the fallback test, which is the opposite of what it exists to prove. The KDoc records this reasoning.
- **The catalog lookup key comes from `BoardSpec.footprint`.** Writing a second `sortedWith(compareBy(...))` in the mapper would have been the exact determinism hazard 08-01's summary flags — two sorts that can drift apart break the `Long`-bitmask contract.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Diamond-41's proven perfect finish was classified as a FAILURE, making the whole re-anchor unreachable**

- **Found during:** Task 2 (writing the RED spec for the finished event)
- **Issue:** `GameViewModel.statusFor` decided a win with `board.pegCount == SOLVED_PEG_COUNT`, a private hardcoded `1`. A `finished` event — the *only* thing that carries a `ScoreResult` to Results — fires only on `GameStatus.Won`. So a Diamond-41 board at its proven two-peg floor was classified `Stuck`, showed the rescue banner, emitted nothing, and never reached a score. **The threaded `minimumPegs` would have been dead code for the only board in the catalogue that needs it**, and the plan's own success criterion ("a Diamond-41 finish at 2 pegs yields 3 stars end to end through `GameViewModel`") was unachievable. The RED run confirmed it precisely: the sole failure was the `GameStatus.Won` assertion.
- **Fix:** Re-anchored the win test on the same per-board target: `board.pegCount <= board.spec.minimumPegs -> GameStatus.Won`. `<=` rather than `==` so the status can never disagree with `ScoreCalculator.stars`, whose 3-star band is also `<=`. The `SOLVED_PEG_COUNT` literal was deleted (it was also a CLAUDE.md magic-number violation now that the per-board field exists), and the KDoc explains why one peg is the wrong test for this board.
- **Scope:** Byte-identical for the four cross boards and every generated Challenge footprint, all of which carry `minimumPegs = 1` — and a 0-peg board is unreachable, so `<=` and `==` coincide there. The `!isGameOver` guard still runs first, so a 2-peg Diamond board that still has a legal move keeps playing. The emission architecture is untouched: `Won` is still the only emitter and D-06's rescuable `Stuck` is intact.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest` — whole suite green, including the 47 pre-existing `GameViewModelTest` cases and `GameFlowTest`'s Stuck-banner flow test.
- **Committed in:** `a55ed8c` (part of the Task 2 commit — both changes are in one file and neither is green without the other)

**2. [Rule 3 - Blocking] detekt `TooManyFunctions` + `ReturnCount` on `SnapshotMapper`**

- **Found during:** Task 3 (`./gradlew check`)
- **Issue:** The first cut added two private helpers (12 functions, limit 11) and gave `toBoardSpecOrNull` three returns (limit 2).
- **Fix:** Folded the catalog lookup into `resolveMinimumPegsOrNull` (one function, two returns) and reshaped `toBoardSpecOrNull` to a single return. The reshape required care: the resolver constructs a `BoardSpec` to derive the lookup key, which a malformed footprint would make throw, so the call is deliberately short-circuited behind the well-formedness checks. A comment records that ordering constraint.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`
- **Verification:** `./gradlew check` green; the malformed-footprint rejection cases (`inPlay = emptyList()`, duplicates, forged coordinates) all still return `null` rather than throwing.
- **Committed in:** `a520549`

**3. [Rule 3 - Blocking] ktlint raw-string indentation in the new legacy-blob helper**

- **Found during:** Task 3 (`./gradlew check`)
- **Issue:** `standard:indent` and `standard:string-template-indent` on the multiline JSON literal.
- **Fix:** `./gradlew :app:ktlintFormat`.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`
- **Verification:** `./gradlew check` green.
- **Committed in:** `a520549`

**4. [Rule 3 - Blocking] Missing `local.properties` in the worktree**

- **Found during:** Setup (a known property of worktree execution, hit by 08-01, 08-02 and 08-06 too)
- **Issue:** `:app:*` Gradle tasks cannot configure — "SDK location not found".
- **Fix:** Copied the main checkout's `local.properties` into the worktree. **Not committed** — gitignored by design, per-machine.
- **Committed in:** n/a (gitignored)

---

**Total deviations:** 4 auto-fixed (1 missing-critical, 3 blocking)
**Impact on plan:** Deviation 1 is the consequential one and is squarely inside the plan's own goal — without it this plan would have shipped a correctly-threaded target that no player could ever observe. It touches one file the plan already owns, changes one comparison, and deletes a magic literal. No scope creep: no new state, no new constructor parameter, no new `GameUiState` field, and the terminal-event architecture is unchanged.

## Issues Encountered

- **The plan's 5-peg Diamond behaviour is not expressible as a `finished` event.** The plan asked for "the `finished` event emitted for a Diamond-41 game that ends with 5 pegs carries `stars == 1`". Five pegs is three above the floor, so such a board is a dead end *above* its target — `Stuck`, which D-06 deliberately keeps rescuable in-board and which emits nothing by design. Making it emit would have contradicted D-06 and broken the existing Stuck-banner tests, so I did not. The test asserts what actually happens instead — the board is `Stuck` at 5 pegs, `finished` stays silent, and the target the VM threads for that board bands 5 pegs at 1 star — with a KDoc naming D-06 as the reason. The acceptance criterion ("a test asserting a Diamond-41 finish at 5 pegs yields 1 star") is satisfied; the guarantee it delivers is that the re-anchor does not *inflate* a mediocre finish.
- **`SnapshotMapperTest` had 17 pre-existing cases, not the 18 the plan states.** All 17 still pass unchanged; the file now holds 25. Recording the discrepancy so a verifier checking the plan's literal number is not misled.

## TDD Gate Compliance

All three tasks ran a full RED → GREEN cycle, each with a `test(08-07)` commit preceding its `feat(08-07)` commit:

- **Task 1** — RED (`2ec472b`) failed at `:domain:compileTestKotlin` with 9 × `No parameter with name 'minimumPegs'` plus `Unresolved reference 'TWO_STAR_BAND_WIDTH'`. GREEN (`992a009`).
- **Task 2** — RED (`2e0f6ad`) compiled and failed at *runtime* on exactly one assertion: `reachingTheDiamondFloorIsAWinWorthThreeStars` at the `GameStatus.Won` check (55 of 56 tests passed). That is the strongest possible RED for this task — it named the real defect rather than a missing symbol. GREEN (`a55ed8c`).
- **Task 3** — RED (`55a0112`) failed at `:app:compileDebugUnitTestKotlin` with `Unresolved reference 'minimumPegs'` and 5 × `No parameter with name 'minimumPegs'`. GREEN (`a520549`).

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew check` | **PASS** (domain + app unit tests, ktlint, detekt, android lint) |
| `./gradlew :domain:test --tests '*ScoreCalculatorTest*'` | PASS — 16 tests, including the `0..45` sweep |
| `./gradlew :domain:test` (full suite) | PASS |
| `./gradlew :app:testDebugUnitTest --tests '*SnapshotMapperTest*'` | PASS — 25 tests, all 17 pre-existing unchanged |
| `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*' --tests '*GameFlowTest*' --tests '*ResultsViewModelTest*'` | PASS |
| `./gradlew :app:testDebugUnitTest` (full suite) | PASS |
| `./gradlew ktlintCheck detekt` | PASS |
| `grep -c 'const val TWO_STAR_BAND_WIDTH = 2' ScoreParams.kt` | `1` |
| `grep -v '^\s*\*' ScoreParams.kt \| grep -c 'TWO_STAR_MAX_PEGS = 3'` | `0` — derived, not a literal |
| `grep -v '^\s*\*' ScoreCalculator.kt \| grep -c 'levelId\|ClassicLevelIds'` | `0` — no level coupling |
| `grep -rn 'ScoreCalculator\.\|scoreCalculator\.' app/src/main` | 1 call site (+1 KDoc link), threading `nextBoard.spec.minimumPegs` |
| `grep -c 'val minimumPegs: Int? = null' ResumeSnapshotDto.kt` | `1` |
| `grep -c 'minimumPegs = initialBoard.spec.minimumPegs' SnapshotMapper.kt` | `1` |
| `grep -c 'CLASSIC_SPECS' SnapshotMapper.kt` | `3` |
| `grep -c 'in 1..positions.size' SnapshotMapper.kt` | `2` |
| `grep -rn 'Migration' app/src/main/java/com/app/pegsolitaire/data/db/` | Pre-existing KDoc only — **no migration added** |
| `git diff --diff-filter=D --name-only 00c6bb6..HEAD` | empty — no deletions |

## Success Criteria

- [x] `ScoreCalculator.stars`/`evaluate` take the board's target, defaulting to a value that reproduces today's global bands exactly across the full `0..45` domain
- [x] A Diamond-41 finish at 2 pegs yields 3 stars end to end through `GameViewModel`; at 5 pegs it yields 1 (and, correctly, never reaches Results — see Issues Encountered)
- [x] A resumed Diamond-41 game reports `minimumPegs == 2`, including from a blob written before this phase
- [x] A snapshot proposing `minimumPegs` outside `1..footprint size` is refused entirely, with no crash
- [x] No Room migration and no `ScoreCalculator` coupling to level ids was introduced

## Threat Model Coverage

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-08-21 (forged `minimumPegs` from disk) | mitigate | **Mitigated** — validated `in 1..positions.size` *before* `BoardSpec` construction (so the domain `require` can never surface as a crash on untrusted input); refused, never clamped. Tested with `0`, `-1` and `size + 1`, plus inclusive-boundary controls at `1` and `size`. |
| T-08-22 (legacy blob silently defaulting to 1) | mitigate | **Mitigated** — absence resolves from `Boards.CLASSIC_SPECS` by `footprint` equality before any fallback, exercised by a hand-written legacy Diamond-41 envelope with no `minimumPegs` key; the fallback itself is exercised by an explicit freeform-footprint case. `SnapshotMapperTest` gained 8 cases, so Pitfall 3's warning sign does not apply. |
| T-08-23 (inflated stars in `LevelResult` bests) | accept | **Accepted as planned** — the only path is a self-inflicted forged local blob, with no server and no leaderboard in this phase. Recorded for Phase 12, which reads bests for Play Games submission. |
| T-08-24 (Room migration risk) | avoid | **Avoided** — no migration written; asserted by grep. |
| T-08-SC (package-manager installs) | n/a | No packages installed. |

## Known Stubs

None. No placeholder values, TODO markers, hardcoded empty collections, or unwired data paths were introduced.

## Threat Flags

None. No new network, auth, or file-access surface. The one persistence-format change (`ResumeSnapshotDto.minimumPegs`) is inside the existing untrusted-blob trust boundary and is covered by T-08-21/T-08-22 above; it is a JSON field inside an existing column, so there is no Room schema change.

## User Setup Required

None — no external service configuration required. (Per-machine note, unchanged from 08-01/08-02/08-06: a fresh worktree needs its own gitignored `local.properties` with `sdk.dir` before `:app` tasks will configure.)

## Next Phase Readiness

- **DEBT-02's player-facing half is complete.** 08-01 landed the target, 08-06 proved the floor, and this plan makes both visible to a player: correct stars, a correct win, and a target that survives process death.
- **Hand-off to 08-05 (bitboard solver) — unchanged and still the critical one.** 08-04's summary flags that `Solver.SOLVED_PEG_COUNT` is still a hardcoded `1`, so a Diamond-41 *hint* remains slow and wrong until 08-05 lands. This plan does not touch `domain/solver/` (owned by 08-05 in a parallel worktree) and does not change that hand-off. Note for 08-05: `GameViewModel.statusFor` now uses `board.spec.minimumPegs`, which is the same shape the solver goal test should take.
- **Merge-conflict note:** the only file this plan shares with a sibling wave-3 plan is none — `ScoreParams.kt`, `ScoreCalculator.kt`, `GameViewModel.kt`, and the two resume files are all outside 08-05's `domain/solver/` scope.
- **For the phase verifier:** deviation 1 changes player-visible outcome semantics for exactly one board (a Diamond-41 two-peg dead end now routes to Results instead of showing the rescue banner). It is the intended consequence of D-03/D-04, but it was not named in `08-CONTEXT.md`, so it deserves an explicit look.
- No blockers.

## Self-Check: PASSED

- All 9 modified files verified present on disk.
- All 6 task commits verified in `git log 00c6bb6..HEAD`.
- No file deletions across the plan (`git diff --diff-filter=D` empty).
- No untracked files left behind (`git status --short` shows only the tracked modifications); `local.properties` is gitignored by design.
- All task `<acceptance_criteria>` re-run and passing; the plan-level `<verification>` block re-run and green.
- `STATE.md` and `ROADMAP.md` untouched, per the parallel-execution contract.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-29*
