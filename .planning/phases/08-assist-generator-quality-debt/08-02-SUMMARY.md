---
phase: 08-assist-generator-quality-debt
plan: 02
subsystem: testing
tags: [kotlin, solver, bitmask, symmetry, canonicalisation, property-testing, junit4, truth]

# Dependency graph
requires:
  - phase: 04-solver-assist
    provides: the production Solver / WinningLineSearch whose private canonicalisation copy is folded away here
  - phase: 07-challenge-generator
    provides: BoardCanonical, the generator's symmetry-normalised dedup gate
provides:
  - "One symmetry-canonicalisation implementation repo-wide (BoardCanonical), replacing three verbatim copies"
  - "BoardCanonical.chunkTables / ChunkTables — per-footprint [sym][chunk][byte] lookup tables with a chunk count derived from spec.size"
  - "BoardCanonical.canonicalFast — the allocation-free chunked canonicaliser the bitboard rewrite needs"
  - "A bounded (6-footprint), access-ordered, structurally-keyed table cache"
  - "CanonicalTest — peg-count-preserving / idempotent / agrees-with-naive property net over all five Classic footprints, with the Pitfall 4 high-bit cases pinned"
affects: [08-04-solver-tables, 08-05-bitboard-solver, 08-10-opening-book]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Chunked byte-table permutation of a Long bitmask (chunk count derived from footprint size, never hardcoded)"
    - "Structural footprint key (List<Position>) rather than referential identity for per-spec memoisation"
    - "Bounded access-ordered LinkedHashMap cache for per-spec precomputed tables"
    - "Seeded-corpus property tests as the gate that licenses swapping a hot inner loop"

key-files:
  created:
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt

key-decisions:
  - "Chunk count is derived as (spec.size + 7) / 8 at table-build time — the literal Pitfall 4 mitigation, and CanonicalTest was verified to FAIL (4 of 6 tests) when it is hardcoded to 5"
  - "The table cache is keyed on a structural List<Position> footprint and bounded at 6 entries, because Challenge generates a fresh footprint per level and each entry costs up to 96 KB"
  - "The footprint key is derived locally inside BoardCanonical rather than read from BoardSpec.footprint, because that property lands in sibling wave-1 plan 08-01 and does not exist at this plan's base commit"
  - "CanonicalTest declares its own five-footprint list for the same reason (Boards.CLASSIC_SPECS is an 08-01 deliverable)"
  - "Thread safety uses @Synchronized rather than any coroutine primitive — :domain is kotlin-jvm only (QUAL-01 purity)"
  - "Task 3 is a pure extraction: goal test, restart loop, move ordering, budget accounting, VisitedSet and Solver.lastVisited semantics are all untouched (the rewrite belongs to plan 08-05)"

patterns-established:
  - "Pitfall-naming KDoc: the chunkTables KDoc states the observed corruption (bit 40 dropped -> Diamond-41 'reaching' one peg, which GF(4) forbids) so the next reader cannot re-introduce it"
  - "Deliberate-break verification: a property net is only accepted once it has been shown to fail against the exact bug it exists to catch"

requirements-completed: [DEBT-01]

# Metrics
duration: 8 min
completed: 2026-08-28
---

# Phase 8 Plan 02: Canonicalisation Consolidation Summary

**Three verbatim copies of `symmetryPermutations`/`canonical` collapsed into one `BoardCanonical`, extended with a chunked byte-table fast path whose chunk count is derived from `spec.size`, and pinned by a seeded property net across all five Classic footprints.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-08-28T23:19:17Z
- **Completed:** 2026-08-28T23:27:39Z
- **Tasks:** 3
- **Files modified:** 4 (1 created, 3 modified)

## Accomplishments

- `BoardCanonical` now offers both the naive `canonical(pegs, symmetries)` and the chunked `canonicalFast(pegs, tables)`, with `chunkTables(spec)` building `[symmetry][chunk][byteValue] -> Long` tables once per footprint. Chunk count is `(spec.size + BITS_PER_CHUNK - 1) / BITS_PER_CHUNK` — never a literal.
- The table cache is an access-ordered `LinkedHashMap<List<Position>, ChunkTables>` with `removeEldestEntry` capped at `MAX_CACHED_FOOTPRINTS = 6`, so Challenge's per-level fresh footprints cannot grow it without bound at ~96 KB per entry.
- `CanonicalTest` proves, over a fixed-seed 2000-mask corpus per footprint, that canonicalisation is peg-count preserving, idempotent, and byte-identical to the naive path — plus explicit single-bit regressions for Diamond-41 bit 40 and Wiegleb-45 bits 40..44 (the exact masks the Pitfall 4 bug zeroed).
- The property net was **verified to actually catch the bug**: hardcoding `chunkCount = 5` fails 4 of the 6 tests (`theFastPathIsExactlyTheNaivePath`, `everyGoldenSolutionPrefixCanonicalisesWithAPreservedPegCount`, `canonicalisationPreservesThePegCountOnEveryFootprint`, `theHighestBitsOfTheLargestFootprintsSurviveCanonicalisation`).
- `Solver.kt` and `AuthoringSolver.kt` lost their private copies and now delegate; `grep -rn 'fun symmetryPermutations' domain/src app/src` and `grep -rn 'fun canonical(' domain/src app/src` each return exactly one line, both in `BoardCanonical.kt`.
- Independent cross-check of the RESEARCH board-facts table: `symmetryPermutations(Boards.Asymmetric39)` is order **2** (identity + anti-transpose), the other four footprints are order 8. Relevant to node-budget headroom in plan 08-05.

## Task Commits

1. **Task 1: chunked canonicalisation fast path with a derived chunk count** — `59cb1ef` (feat)
2. **Task 2: property-test canonicalisation across every shipped footprint** — `c893a95` (test)
3. **Task 3: delete the two private copies and delegate to BoardCanonical** — `d7e8f13` (refactor)

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt` — the single canonicalisation implementation; gained `ChunkTables`, `chunkTables(spec)`, `canonicalFast(pegs, tables)`, the bounded structural-key cache, and a rewritten object KDoc naming its four consumers.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt` — 6 JUnit4 + Truth tests forming the correctness net; class KDoc records why the file exists (three drifting copies, a dropped high bit, a GF(4)-impossible one-peg Diamond result).
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt` — `WinningLineSearch` holds `canonicalTables` and calls `BoardCanonical.canonicalFast`; ~65 lines of duplicated logic deleted; search semantics untouched.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt` — same delegation; ~60 lines of duplicated logic deleted.

## Decisions Made

See `key-decisions` in the frontmatter. The load-bearing ones:

- **Derived chunk count, KDoc'd against Pitfall 4.** The KDoc on `chunkTables` states the failure mode in full (five chunks cover bits 0-39, dropping bit 40 on Diamond-41 and bits 40-44 on Wiegleb-45, letting a search "reach" states the GF(4) invariant forbids) so a future reader cannot re-introduce it as a "simplification".
- **Structural footprint key, bounded cache.** Referential identity would miss on a spec rebuilt from a resume blob; an unbounded map would leak ~96 KB per generated Challenge level.
- **Out-of-footprint bits contribute nothing.** In `buildChunkTable`, source bits past the end of the permutation (the tail of the last chunk when `spec.size` is not a multiple of 8) map to 0. They can never be set in a real position mask; `canonicalFast`'s KDoc scopes its naive-equivalence claim to masks whose set bits lie inside the footprint, which is exactly what `CanonicalTest` samples.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `BoardSpec.footprint` does not exist at this plan's base commit**

- **Found during:** Task 1 (chunkTables memoisation)
- **Issue:** The plan specifies memoising on `spec.footprint`, "the structural key added in plan 08-01". 08-01 is a **sibling wave-1 plan executing in a parallel worktree**, so `footprint` is absent from `BoardSpec` at this plan's base commit (`22de8ec`). Referencing it would not compile.
- **Fix:** Added a private `footprintKey(spec)` in `BoardCanonical` that derives the same structural key (`spec.inPlay.sortedWith(compareBy({ it.row }, { it.col }))` — byte-for-byte the sort `BoardSpec.indexOf` already performs, so it is the same list `08-01`'s `footprint` will expose). The map key type is still `List<Position>`, so the plan's acceptance criterion holds and the post-merge swap to `spec.footprint` is a one-line body change.
- **Files modified:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt`
- **Verification:** `./gradlew :domain:test` green; the cache is exercised by every `chunkTables` call in `CanonicalTest` and both solvers.
- **Committed in:** `59cb1ef` (Task 1 commit)

**2. [Rule 3 - Blocking] `Boards.CLASSIC_SPECS` does not exist at this plan's base commit**

- **Found during:** Task 2 (property corpus construction)
- **Issue:** The plan's behavior block iterates `Boards.CLASSIC_SPECS`. That catalog is also an 08-01 deliverable and is absent here.
- **Fix:** `CanonicalTest` declares its own named five-footprint list in its companion, with a KDoc stating that the shared catalog lands in sibling plan 08-01. Coverage is identical (all five Classic footprints), and the names make failures diagnosable.
- **Files modified:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt`
- **Verification:** `./gradlew :domain:test --tests '*CanonicalTest*'` green; all five footprints are asserted over.
- **Committed in:** `c893a95` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both are the same root cause — a wave-1 sibling plan owns two symbols this plan's text assumed were already present. Neither changes behaviour, coverage, or the shape of the delivered API; both leave a trivially-mergeable one-line follow-up once 08-01 lands. No scope creep.

## Issues Encountered

- **`:app` cannot build inside the worktree by default.** `./gradlew :app:compileDebugKotlin` failed with "SDK location not found" because `local.properties` is gitignored and therefore absent from a fresh worktree. Resolved by copying the main checkout's `local.properties` into the worktree (still gitignored, not committed); `./gradlew :app:testDebugUnitTest` then passed. This is an environment property of worktree execution, not a code issue.
- Two ktlint/detekt formatting fixes during authoring (a >120-char line in each new file, and kotlin-imports-last ordering in `CanonicalTest`). Both fixed before the respective commits; `ktlintCheck detekt` is green with **zero** `@Suppress` annotations added to any touched file.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test` (full suite: SolverTest, SolverBudgetTest, PagodaTest, CanonicalTest, SolutionTest, ChallengeGeneratorGoldenTest, QualityGateTest) | PASS |
| `./gradlew :domain:test --tests '*CanonicalTest*'` | PASS (6 tests) |
| `./gradlew ktlintCheck detekt` | PASS |
| `./gradlew :app:testDebugUnitTest` | PASS |
| `grep -rn 'fun symmetryPermutations' domain/src app/src` | exactly 1 (BoardCanonical.kt:36) |
| `grep -rn 'fun canonical(' domain/src app/src` | exactly 1 (BoardCanonical.kt:68) |
| Hardcoded `chunkCount = 5` outside comments | 0 occurrences |
| Deliberate `chunkCount = 5` break | CanonicalTest FAILS 4/6, restored to green |
| New `@Suppress` annotations | 0 |

## Threat Model Coverage

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-08-04 (chunk count drops high bits) | mitigate | Mitigated — chunk count derived from `spec.size`; bit-40 / bits-40..44 cases and the peg-count property assert it, and the mitigation was verified by deliberately breaking it. |
| T-08-05 (memo conflates distinct positions) | mitigate | Mitigated — `canonicalFast == canonical` over 2000 seeded masks per footprint, so the Task 3 extraction cannot have changed reachability. |
| T-08-06 (divergence between copies) | mitigate | Mitigated — exactly one definition of each function remains repo-wide. |
| T-08-SC (package-manager installs) | n/a | No packages installed. |

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan **08-05** (bitboard solver rewrite) has its prerequisite: one canonicaliser plus a property net that gates any replacement of the inner loop against the naive path.
- Plan **08-04** (`SolverTables`) can reuse `BoardCanonical.chunkTables` directly rather than adding a fourth copy — the `symChunks` slot in the RESEARCH Pattern 3 sketch is already built and cached here.
- **Post-merge follow-up (trivial, non-blocking):** once sibling plan 08-01 merges, swap `BoardCanonical.footprintKey(spec)` to return `spec.footprint`, and swap `CanonicalTest.classicSpecs` to `Boards.CLASSIC_SPECS`. Both are one-line changes with the property net already in place to catch any error.
- No blockers.

## Self-Check: PASSED

- `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt` — FOUND
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt` — FOUND
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt` — FOUND
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt` — FOUND
- Commit `59cb1ef` — FOUND
- Commit `c893a95` — FOUND
- Commit `d7e8f13` — FOUND
- All task `<acceptance_criteria>` re-run and passing; plan `<verification>` commands re-run and green.
- No stubs, placeholders, or TODO markers introduced.
- STATE.md / ROADMAP.md untouched (parallel worktree mode — orchestrator owns those writes).

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-28*
