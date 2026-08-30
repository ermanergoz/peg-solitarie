---
phase: 09-consent-rewarded-ads
plan: 01
subsystem: domain
tags: [assist-allowance, rewarded-ads, persistence, kotlinx-serialization, pure-kotlin]

# Dependency graph
requires:
  - phase: 05-persistence
    provides: ResumeSnapshotDto / SnapshotMapper resume codec (replay-based restore, fail-safe decode)
  - phase: 08-assist-generator-quality-debt
    provides: the `domain/assist` package (OpeningBook) and the reject-don't-clamp precedent for stored values
provides:
  - AssistAllowance immutable value type with separate hint/undo counters (D-01)
  - AssistAllowanceLimits — the single definition of FREE_HINTS=3 / FREE_UNDOS=5 (D-02)
  - AssistKind enum, the :domain-side counterpart to app's AdReason
  - AssistAllowance.fromStoredOrFull — fail-open codec resolution for untrusted stored counters
  - freeHintsRemaining / freeUndosRemaining on ResumeSnapshotDto, round-tripped by SnapshotMapper
  - RestoredGame.allowance — non-null by construction
affects: [09-03, 09-06, 09-08, rewarded-ads, gameplay-viewmodel, resume]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scoped constants object (ScoreParams shape) for the free-assist limits"
    - "Fail-open codec resolution: reject out-of-range stored values in favour of a default, never clamp, never refuse the snapshot"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
    - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt

key-decisions:
  - "AssistKind duplicates AdReason deliberately — :domain cannot depend on app code (DomainPurityTest), so GameViewModel owns the mapping"
  - "fromStoredOrFull rejects out-of-range stored counters in favour of the full default rather than clamping (T-09-01)"
  - "A forged allowance counter must NOT refuse the resume, the opposite of minimumPegs — refusing converts tampering into progress loss for honest players (T-09-02)"
  - "A stored 0 is in range and is preserved, so backgrounding the app is not a free refill"
  - "toDto's two new parameters default to null so GameViewModel compiles unchanged; plan 09-08 supplies the real values"

patterns-established:
  - "Fail-open vs fail-closed persistence: a missing fact the game needs (minimumPegs) refuses the snapshot; a missing generosity the player gets (allowance) defaults to full"
  - "Limits named exactly once in the codebase; tests assert against the constants, dialog copy formats them via %1$d"

requirements-completed: [ADS-02]

# Metrics
duration: 17 min
completed: 2026-08-30
---

# Phase 9 Plan 01: Assist Allowance & Snapshot Persistence Summary

**Pure-Kotlin `AssistAllowance` policy — separate 3-hint / 5-undo counters with refill-to-full and a zero floor — persisted through the resume snapshot behind a fail-open codec that degrades forged counters without ever refusing a restore.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-08-30T22:56:43Z
- **Completed:** 2026-08-30T23:14:36Z
- **Tasks:** 2 (both TDD, 4 commits)
- **Files modified:** 5 (2 created, 3 modified)

## Accomplishments

- `AssistAllowance` in `:domain` as a pure immutable value type: `remaining`/`hasFree`/`spend`/`refill` plus the `fromStoredOrFull` companion, with zero Android surface (domain-purity gate green).
- `AssistAllowanceLimits.FREE_HINTS = 3` / `FREE_UNDOS = 5` are the only statement of those two numbers in the codebase (D-02); the tests assert against the constants rather than restating the literals.
- Both counters now survive process death: `ResumeSnapshotDto` carries two nullable fields, `SnapshotMapper` encodes them and resolves them on decode, and `RestoredGame.allowance` is non-null by construction.
- The security asymmetry with `minimumPegs` is implemented *and* documented at both the field and the call site: a forged target refuses the whole snapshot, a forged counter degrades to the default and the game still restores.

## Task Commits

1. **Task 1: AssistAllowance pure domain policy** — `87735a4` (test, RED) → `8558891` (feat, GREEN)
2. **Task 2: Resume-snapshot allowance persistence** — `44ccf24` (test, RED) → `79a757b` (feat, GREEN)

No REFACTOR commits: both implementations were clean on first pass (detekt + ktlint green with no suppressions).

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt` — `AssistKind`, `AssistAllowanceLimits`, `AssistAllowance` and its `fromStoredOrFull` companion.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt` — 13 cases, one or more per behavior bullet.
- `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` — `freeHintsRemaining` / `freeUndosRemaining` with the "On-disk contract" KDoc.
- `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt` — `RestoredGame.allowance`, two defaulted `toDto` params, `fromDto` resolution.
- `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt` — 4 new cases (absent / stored round-trip / forged fail-open / legacy raw JSON).

## Decisions Made

- **`AssistKind` rather than reusing `AdReason`.** `AdReason` lives in `app`'s `platform/ads/` and `DomainPurityTest` forbids `:domain` depending on app code. The duplication is recorded in the enum's KDoc so a future reader does not "fix" it; `GameViewModel` maps between the two.
- **Reject, don't clamp.** `fromStoredOrFull` accepts a stored value only when `it in 0..limit`. Clamping a forged `9999` to the limit happens to produce the same number here, but clamping *asserts the stored value is meaningful* — a habit that would be wrong on the next tampered field.
- **Fail open, don't refuse.** Documented at the `fromDto` call site: refusing a resume over a bad counter trades at most three hints for the ability to destroy an honest player's progress with a one-byte edit.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**
- **Found during:** Task 1 (running the `DomainPurityTest` acceptance gate)
- **Issue:** `:app` Gradle tasks failed with "SDK location not found" — `local.properties` is gitignored (`.gitignore:7`) so it does not exist in a fresh worktree, blocking every `:app` verification in this plan.
- **Fix:** Wrote the worktree's own `local.properties` with the same `sdk.dir` as the main checkout. Environment-only; the file remains gitignored and is **not** part of any commit.
- **Files modified:** none tracked (`local.properties` is ignored)
- **Verification:** `./gradlew :app:testDebugUnitTest --offline --tests '*DomainPurityTest*'` then passed; full `check` green.
- **Committed in:** n/a — deliberately uncommitted

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Environment setup only. No production or test code changed as a result, and no scope creep.

## Issues Encountered

None. Both TDD cycles went RED → GREEN on the first attempt; no fix-attempt budget was consumed.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :domain:test --offline --tests '*AssistAllowanceTest*'` | PASS |
| `./gradlew :app:testDebugUnitTest --offline --tests '*DomainPurityTest*'` | PASS |
| `./gradlew :app:testDebugUnitTest --offline --tests '*SnapshotMapperTest*'` | PASS |
| `./gradlew :domain:test --offline` (full module suite) | PASS |
| `./gradlew check --offline` (detekt, ktlint, dp/sp, lint, depth sweep, all suites) | PASS in 11m 7s |
| `grep -n "android" AssistAllowance.kt` | no matches (purity) |
| `grep -c "FREE_HINTS\s*=\s*3\|FREE_UNDOS\s*=\s*5" AssistAllowance.kt` | 2 (each literal written exactly once) |
| `GameViewModel.kt` unmodified | confirmed via `git status` |

No `@Suppress` was added and the detekt baseline was not touched — the 3 and the 5 are named constants, so `MagicNumber` passes on its own terms.

## Threat Flags

None. The plan's threat register (T-09-01 through T-09-04) is fully mitigated in code and pinned by tests; no new security-relevant surface was introduced (no new endpoints, auth paths, file access, or schema changes — the two DTO fields are additive-nullable within the existing untrusted-blob boundary that already has a decode fail-safe).

## Known Stubs

None. `toDto`'s `freeHintsRemaining`/`freeUndosRemaining` parameters default to `null` by design, not as a placeholder: `null` is the real "not written" on-disk value that an older blob carries, and it resolves to a full allowance. Plan 09-08 wires `GameViewModel`'s live counters into the call site.

## User Setup Required

None — no external service configuration required. No packages were installed by this plan.

## Next Phase Readiness

- The allowance contract is final and JVM-proven, so wave-2 plans (09-03 dialogs, 09-06 gateway, 09-08 ViewModel wiring) can consume `AssistAllowance`, `AssistAllowanceLimits` and `AssistKind` directly without re-reading these files.
- `GameViewModel.kt:496`'s `toDto` call still passes no counters; 09-08 must supply them, otherwise every save silently writes a full allowance. This is the one live loose end.
- `RestoredGame` gained a **required** `allowance` property. Any plan constructing a `RestoredGame` outside `SnapshotMapper` must now supply it; nothing in the tree does today (full `check` green).

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-30*
