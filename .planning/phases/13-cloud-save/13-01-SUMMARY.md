---
phase: 13-cloud-save
plan: 01
subsystem: domain
tags: [kotlin, pure-domain, join-semilattice, monotonic-merge, junit4, truth, cloud-save]

# Dependency graph
requires:
  - phase: 05-persistence
    provides: "DefaultProgressRepository.recordResultInTransaction — the per-field max/min/max merge extracted here, and ProgressRepositoryTest, the regression net that proves the extraction inert"
  - phase: 03-challenge-mode
    provides: "ChallengeProgress.advancePast (max ladder) and ChallengeLevelBounds.MIN — the ladder half and its floor"
  - phase: 12-play-games-achievements
    provides: "AchievementRules / ProgressAggregate / AchievementThresholds — the pure-object, defaulted-model and scoped-constants shapes copied verbatim"
provides:
  - "ProgressMerge — the one named monotonic merge (max stars / min time / max score per level, max ladder, max playedTime), proven a join-semilattice by JVM tests"
  - "CloudProgress / LevelKey / LevelBest / ProgressMode — the pure :domain models defining exactly what leaves the device"
  - "ProgressMergeLimits — the five merge identities, NO_TIME_RECORDED pinned at Long.MAX_VALUE"
  - "ProgressMerge.dropLowestChallenge — D-05's trim policy, Classic- and ladder-safe, numerically ordered"
  - "A local recordResult path that runs the identical merge the cloud path will run"
affects: [13-02, 13-03, 13-04, 13-05, 13-09, cloud-save, conflict-resolution, saved-games]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Algebraic-law testing with a seeded kotlin.random.Random loop instead of a property-testing library"
    - "Merge identities as a scoped :domain constants object, so local and cloud paths cannot use different zeroes"
    - "Domain enum (ProgressMode) kept distinct from both the on-disk column value and the wire token"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/CloudProgress.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeLimits.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMerge.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt

key-decisions:
  - "LevelBest carries only the three merged fields; mode and levelId live solely in the LevelKey that indexes it, so the two cannot disagree"
  - "completedAt is excluded from the merge and from LevelBest — it means 'last improved on THIS device' and syncing it would break idempotence"
  - "NO_TIME_RECORDED = Long.MAX_VALUE, never 0L: zero is the min-merge's absorbing element and would permanently pin a level's best time at zero across devices"
  - "A first solve takes this play verbatim rather than merging against a fabricated identity row — equal by the identity law, but obviously lossless"
  - "Challenge level ids are ordered numerically for the trim; an unparseable id sorts last so a malformed row is dropped only after every well-formed one"
  - "No property-testing library added — seeded Random loops keep :domain's dependency set at junit + truth"

patterns-established:
  - "Algebraic-law test file: private randomProgress(Random) generator over a narrow id pool (so two sides genuinely collide on keys) plus private const LAW_SEED / LAW_ITERATIONS"
  - "Merge identities live in :domain and are asserted by value, so a future tidy-up cannot silently redefine one"

requirements-completed: [CLOUD-02]

# Metrics
duration: 34min
completed: 2026-09-15
---

# Phase 13 Plan 01: Pure Progress Merge Summary

**The per-field monotonic merge that has shipped inside `DefaultProgressRepository` since v1.0 is now `ProgressMerge` in `:domain`, machine-proven commutative/associative/idempotent/identity-respecting over 1,000 seeded random inputs — before a single line of `SnapshotsClient` code exists.**

## Performance

- **Duration:** 34 min
- **Started:** 2026-09-15T00:00:00Z
- **Completed:** 2026-09-15T00:34:00Z
- **Tasks:** 2 of 2
- **Files modified:** 5 (4 created, 1 modified)

## Accomplishments

- **CLOUD-02 is now an algebraic fact, not an aspiration.** `ProgressMerge.merge` is a join-semilattice over `CloudProgress`: commutative, associative, idempotent, with `CloudProgress.EMPTY` as its identity. Because the result is independent of merge order, delivery count and which device merged first, no sync ordering, duplicate delivery or replayed conflict can lose a field — every device converges on the same union.
- **One merge, not three.** Local solve recording, the forthcoming cloud import and the Saved Games conflict loop now all route through the same named function. Recording a solve locally and receiving that solve from another device are literally the same operation.
- **Pitfall 10 is closed by construction and by assertion.** `NO_TIME_RECORDED = Long.MAX_VALUE` is the only value for which `min(x, identity) == x` holds across the whole `Long` range; the test asserts the constant's value directly, so a future "tidy-up" to `0L` breaks the build rather than silently pinning every level's best time at zero forever.
- **D-05's trim is safe by shape.** `dropLowestChallenge` structurally cannot remove a `CLASSIC` key or touch `ladder`/`playedTimeMs`, and it orders Challenge ids numerically so `"9"` cannot outrank `"17"`.
- **`:domain` purity held.** `domain/build.gradle.kts` is byte-identical; the module still declares zero production dependencies, and `DomainPurityTest` is green.

## Task Commits

1. **Task 1 (RED): failing algebraic-law spec** — `a317c7a` (test)
2. **Task 1 (GREEN): pure merge, models and limits** — `e9c842d` (feat)
3. **Task 2: route local recordResult through ProgressMerge** — `8694a92` (refactor)

No REFACTOR commit: the GREEN implementation needed no cleanup pass.

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/CloudProgress.kt` — `ProgressMode`, `LevelKey`, `LevelBest`, `CloudProgress`. Every `CloudProgress` property is defaulted, so a fresh player is exactly `CloudProgress()` and `EMPTY` doubles as the merge identity. This class is the complete definition of what leaves the device; the resume snapshot and `play_session` telemetry are deliberately absent (CLOUD-04).
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeLimits.kt` — the five identities (`NO_STARS`, `NO_SCORE`, `NO_TIME_RECORDED`, `DEFAULT_LADDER`, `NO_PLAYED_TIME`), each KDoc'd with why that value rather than another.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMerge.kt` — `merge(LevelBest, LevelBest)`, `merge(CloudProgress, CloudProgress)`, `dropLowestChallenge(CloudProgress, Int)`.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeTest.kt` — 13 tests: five algebraic laws over 1,000 seeded pairs/triples, the `NO_TIME_RECORDED` identity pin, the one-sided-key survival case, the `DEFAULT_LADDER`/`ChallengeLevelBounds.MIN` equality check, and five trim properties.
- `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt` — `recordResultInTransaction` builds a `LevelBest` for this play and joins it with the previous row via `ProgressMerge.merge`; `NO_STARS`/`NO_SCORE` deleted from the file-private `ProgressConstants` and the bare `Long.MAX_VALUE` removed.

## Decisions Made

- **`LevelBest` is narrower than the RESEARCH sketch.** The sketch carried `mode` and `levelId` on the value as well as on the key. Both are dropped: a map entry whose key said Challenge 42 and whose value said Classic English-33 would otherwise be representable, and nothing would catch it.
- **`bestAt` fails loudly on an impossible key.** The private helper `error(...)`s when neither side holds a key. The branch is unreachable (keys are drawn from the union of both key sets) and exists so a future caller cannot quietly pass a foreign key and receive a null.
- **`ProgressConstants`' remit narrowed rather than the object deleted.** `ANY_STARS` and `MILLIS_IN_SECOND` are genuinely repository-local (query narrowing and a unit conversion), so the object stays with a KDoc explaining that the merge floors moved to `:domain`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Purity grep tripped on KDoc prose**

- **Found during:** Task 1 (acceptance-criteria check)
- **Issue:** The acceptance gate `grep -rn "kotlinx.serialization\|@Serializable\|android\.\|androidx\." domain/…/progress/` must return 0 hits. It returned 2 — both in a `CloudProgress` KDoc paragraph that *named* those tokens in order to state they are deliberately unused. No code was impure; the gate is (correctly) a blunt textual instrument.
- **Fix:** Reworded the paragraph to convey the same constraint without restating the forbidden vocabulary, and added an explicit note that a purity gate greps this package so the vocabulary must stay out, prose included.
- **Files modified:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/CloudProgress.kt`
- **Verification:** grep now returns 0; `:app:testDebugUnitTest --tests "*DomainPurityTest*"` green.
- **Committed in:** `e9c842d`

**2. [Rule 3 - Blocking] ktlint `chain-method-continuation` on a test assertion**

- **Found during:** Task 1 (`:domain:ktlintCheck`)
- **Issue:** `assertThat(trimmed.bests.keys.map { it.mode }.toSet())` violated ktlint_official's newline-before-`.` rule for a chain containing a lambda.
- **Fix:** Extracted the value into a named `survivingModes` local built with `mapTo(mutableSetOf())`, which also reads better as an assertion subject.
- **Files modified:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeTest.kt`
- **Verification:** `./gradlew :domain:detekt :domain:ktlintCheck` exits 0.
- **Committed in:** `e9c842d`

**3. [Rule 3 - Blocking] Worktree missing `local.properties`**

- **Found during:** Task 1 (running `:app:testDebugUnitTest --tests "*DomainPurityTest*"`)
- **Issue:** `local.properties` is gitignored, so the parallel-execution worktree had none and every `:app` Gradle task failed with "SDK location not found".
- **Fix:** Wrote a minimal `local.properties` containing only `sdk.dir`. Play Games and AdMob IDs were deliberately *not* copied — debug builds fall back to their committed placeholders, which is all these unit tests need. The file is gitignored and is **not** part of any commit.
- **Files modified:** none tracked.
- **Verification:** `git status --short` clean after each commit; `git diff --cached --name-only` confirmed only intended files staged.
- **Committed in:** n/a (untracked, environment-only)

---

**Total deviations:** 3 auto-fixed (3 × Rule 3 — blocking issues).
**Impact on plan:** None on scope or behaviour. Two were verification-gate mechanics and one was worktree environment setup. No production logic changed as a result.

## Issues Encountered

**The plan's "replace the bare `Long.MAX_VALUE` with `ProgressMergeLimits.NO_TIME_RECORDED`" instruction is unreachable given its own stronger instruction.** The plan also says a first solve must take `thisPlay` verbatim and must not merge against a fabricated identity row. Honouring the second makes the three "no previous row" floors (`NO_STARS`, `NO_SCORE`, `Long.MAX_VALUE`) have no use site at all, so there is nothing left to replace. The stronger instruction was followed and the floors were deleted outright — a strictly better outcome than re-pointing them, and it satisfies the machine-checkable criterion (`grep -c "Long.MAX_VALUE"` returns 0) exactly. The identities still exist, stated once, in `ProgressMergeLimits`, where the cloud path will read them.

## Forward-Facing API (not stubs)

`ProgressMerge.dropLowestChallenge` and `merge(CloudProgress, CloudProgress)` have no production call site yet — the trim is called by plan 13-03's `CloudProgressCodec.encodeWithinCap` and the whole-snapshot merge by 13-04's import path. Both are **fully implemented and fully tested**, not placeholders: there is no hardcoded return, no TODO and no empty collection standing in for real data. Proving them before the SDK exists is the ROADMAP's explicit sequencing mandate, so a call-site-free window is the intended state at the end of this plan, not an omission.

## Known Stubs

None.

## Threat Flags

None. This plan is pure in-process computation over already-stored local data and crosses no trust boundary; the first untrusted-input boundary arrives in plan 13-03's decoder. All three `mitigate` dispositions in the plan's threat register are discharged:

| Threat ID | Mitigation | Evidence |
|-----------|-----------|----------|
| T-13-01 | `bestTimeMs` identity is `NO_TIME_RECORDED`, never `0L` | `emptyTimeIdentityIsNoTimeRecorded` + `mergeWithEmptyIsIdentity` green |
| T-13-02 | Trim structurally cannot drop a Classic row or mutate the scalars | `dropLowestChallengeKeepsEveryClassicRow` + `dropLowestChallengeKeepsLadderAndPlayedTime` green |
| T-13-03 | Extraction proven behaviour-preserving by an **unedited** regression net | `git diff --stat` of `ProgressRepositoryTest.kt` is empty; 13 + 8 tests green |
| T-13-SC | Zero new dependency coordinates | `git diff --stat domain/build.gradle.kts gradle/libs.versions.toml` empty |

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**Ready.** Plans 13-03, 13-04 and 13-05 can build against the exact names this plan contracted (`ProgressMode`, `LevelKey`, `LevelBest`, `CloudProgress`, `CloudProgress.EMPTY`, `ProgressMerge.merge` ×2, `ProgressMerge.dropLowestChallenge`, and the five `ProgressMergeLimits` identities).

Two things plan 13-03 inherits as obligations:

1. **Pin the wire tokens for `ProgressMode` in a codec test.** The enum is deliberately neither the on-disk column value nor the wire string; the two-way mapping must live once, in `data/cloud/`, and be asserted (Pitfall 11).
2. **Assert `ProgressMergeLimits.DEFAULT_LADDER == ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL`.** `:domain` cannot reference the `app`-module constant, so this plan could only assert the `:domain` half against `ChallengeLevelBounds.MIN`; the `app`-side half is 13-03's.

Also worth carrying forward: `encodeWithinCap`'s shrink loop is bounded precisely because `dropLowestChallenge` leaves Classic rows untouched — so the `challengeRows == 0` escape branch is load-bearing, not defensive padding, and needs its own test with an absurdly small `maxBytes`.

No blockers. The ROADMAP's sequencing mandate holds: `app/src/main/java/com/app/pegsolitaire/platform/games/save/` does not exist.

## Self-Check: PASSED

**Created files exist:**

- FOUND: `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/CloudProgress.kt`
- FOUND: `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeLimits.kt`
- FOUND: `domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMerge.kt`
- FOUND: `domain/src/test/kotlin/com/app/pegsolitaire/domain/progress/ProgressMergeTest.kt`

**Commits exist:** FOUND `a317c7a`, FOUND `e9c842d`, FOUND `8694a92`

**Verification commands actually run, with results:**

| Command | Result |
|---------|--------|
| `./gradlew :domain:test --tests "*ProgressMergeTest*"` | BUILD SUCCESSFUL — 13 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew :domain:test` (full suite) | BUILD SUCCESSFUL |
| `./gradlew :domain:detekt :domain:ktlintCheck` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | BUILD SUCCESSFUL — 13 + 8 tests, 0 failures, 0 errors |
| `./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*"` | BUILD SUCCESSFUL |
| `./gradlew :app:ktlintMainSourceSetCheck :app:detekt` | BUILD SUCCESSFUL |

**Acceptance-criteria gates:**

| Gate | Expected | Actual |
|------|----------|--------|
| `ProgressMergeTest` executed count | ≥ 12 | 13 |
| `grep -c "object ProgressMerge"` | 1 | 1 |
| `grep -c "fun merge("` in `ProgressMerge.kt` | 2 | 2 |
| `grep -c "fun dropLowestChallenge("` | 1 | 1 |
| `grep -c "NO_TIME_RECORDED = Long.MAX_VALUE"` | 1 | 1 |
| `grep -c "val EMPTY = CloudProgress()"` | 1 | 1 |
| `git diff --stat domain/build.gradle.kts` | empty | empty |
| purity grep over `domain/…/progress/` | 0 | 0 |
| `git diff --stat …/ProgressRepositoryTest.kt` | empty | empty |
| `grep -c "ProgressMerge.merge"` in `ProgressRepository.kt` | ≥ 1 | 1 |
| `grep -c "Long.MAX_VALUE"` in `ProgressRepository.kt` | 0 | 0 |
| `ls app/…/platform/games/save` | absent | No such file or directory |
| `git diff --diff-filter=D` per commit | no deletions | none |

---
*Phase: 13-cloud-save*
*Completed: 2026-09-15*
