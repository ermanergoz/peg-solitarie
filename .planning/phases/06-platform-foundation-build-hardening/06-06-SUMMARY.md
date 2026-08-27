---
phase: 06-platform-foundation-build-hardening
plan: 06
subsystem: security-hardening
tags: [DEBT-04, WARN-03, D-13, D-15, clamping, untrusted-input, domain-purity]
requires:
  - "06-01 (RewardOutcome seam — GameViewModel was already reshaped by it)"
  - "06-02 (06-SECURITY-EVIDENCE.md and its WARN-03 placeholder heading)"
provides:
  - "ChallengeLevelBounds — the single :domain definition of the valid Challenge level range plus a fail-safe clamp"
  - "A clamped GameViewModel.challengeLevel getter (the value that escapes into the Results route, the DB key and 'Next level')"
  - "A clamped ResultsViewModel boundary covering all four challengeLevel reads"
  - "Committed closure evidence for both DEBT-04 advisories"
affects:
  - "Any future consumer of GameViewModel.challengeLevel — it is now always the level actually played"
  - "Phase 9+ Challenge work: the level range is now one constant to change, not three"
tech-stack:
  added: []
  patterns:
    - "Pure :domain policy object with a clamp helper (the ScoreParams / DifficultyTiers idiom)"
    - "One clamped value read by every level-keyed site, rather than a coerceIn per site"
key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/ChallengeLevelBounds.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeLevelBoundsTest.kt
  modified:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeLevelSource.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - .planning/phases/06-platform-foundation-build-hardening/06-SECURITY-EVIDENCE.md
decisions:
  - "Fixed the producer (GameViewModel), not the consumer (GameRoute) as the advisory suggested — a consumer-side guard leaves the next consumer to repeat it"
  - "Clamped ResultsViewModel as a first-class boundary, not defence-in-depth: Results is reachable in AppNavHost without going through GameRoute"
  - "ChallengeLevelBounds.MAX delegates to GeneratorConstants.MAX_CHALLENGE_LEVEL rather than relocating it, leaving the frozen-generator-contract file untouched"
  - "DifficultyTiers.Curve.MIN_LEVEL deliberately not absorbed — a playtest-tunable curve parameter, not the lowest legal level; the exclusion is documented in the new object's KDoc"
metrics:
  duration: ~35m
  completed: 2026-08-27
  tasks: 3
  commits: 5
  tests: "309 → 320 JVM tests, all green"
---

# Phase 6 Plan 6: WARN-03 Closure & Challenge-Level Bound Consolidation Summary

Closed security advisory WARN-03 by clamping the two independent nav-route boundaries that
could put a forged Challenge level into the results database, and consolidated the
three-times-duplicated level bound into a single pure-`:domain` `ChallengeLevelBounds`
policy object.

## What Was Built

**`ChallengeLevelBounds` (`:domain`, D-15).** `MIN`, a `MAX` that *delegates* to
`GeneratorConstants.MAX_CHALLENGE_LEVEL`, and `clamp(Int)`. Pure Kotlin, zero Android
dependencies. Three duplicate constants were absorbed (`LevelGenerator.MIN_LEVEL`,
`ChallengeLevelSource.MIN_CHALLENGE_LEVEL`, `GameViewModel.MIN_CHALLENGE_LEVEL`); the
consolidation grep now returns exactly one line — the deliberately-excluded
`DifficultyTiers.Curve.MIN_LEVEL`.

**The WARN-03 fix.** `GameViewModel.challengeLevel` is now
`get() = activeChallengeLevel`. `ResultsViewModel` holds one clamped private
`challengeLevel` read by all four sites, including the database level key and the
`uiState` value that `ResultsActions` turns into `onNextLevel(challengeLevel + 1)`.

**Nine regression cases** across four test files, all asserting on observable outcomes
(the exposed value, the recorded `levelId`) and never on a private field.

## Key Decisions

**The fix is subtraction, not a new guard.** `GameViewModel` already held the correct value:
`activeChallengeLevel`, whose own KDoc read *"Everything level-keyed after construction
reads THIS."* The public getter was the single thing that did not. Delegating to it restores
an existing invariant rather than adding a third clamp expression that could later drift out
of sync with the other two. The plan called this out and it held up on reading the code.

**The advisory understated the problem; `ResultsViewModel` is a first-class boundary.** The
advisory framed the Results-side clamp as following from the getter fix. It does not:
`Results` is a typed route in `AppNavHost` reachable *without* going through `GameRoute`, so
its arg is untrusted independently. Fixing only the getter would have left
`ProgressRepository.recordResult(levelId = …)` reachable with a forged value. Both
boundaries are treated as primary, each with its own regression coverage.

**Fixed the producer, not the consumer.** The advisory's suggested fix was "have `GameRoute`
read the clamped value". Guarding in the consumer leaves every future consumer to remember
to repeat it; fixing the producer covers the Results route, the screen title, and every
reader not yet written. `GameRoute.kt` was not modified.

**`MAX` delegates rather than moves.** `GeneratorConstants.MAX_CHALLENGE_LEVEL` carries a
load-bearing KDoc in the frozen-generator-contract file and is referenced by name in several
places. Adding a delegating sibling keeps one ceiling value without touching that file —
`git diff --stat` on `GeneratorConstants.kt` and `DifficultyTiers.kt` is empty, as the plan
required.

**`Curve.MIN_LEVEL` stays.** It is the origin of the difficulty ramp
(`fromStart = level - MIN_LEVEL`) inside an explicitly playtest-tunable knob block — a
*curve* parameter, semantically distinct from "the lowest legal level number", and the two
must be tunable independently. The new object's KDoc states this so a later reader does not
file it as an oversight.

**A third, quieter path was covered.** A resume snapshot's provenance stamp beats the route
arg (WR-03), and stored state on a rooted device or in a restored backup is no more trusted
than a nav arg. It was already clamped on assignment; a new case forges the stored stamp to
`Int.MAX_VALUE` and pins that, so the getter cannot be un-clamped again through that door.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] A pre-existing test asserted the vulnerability**

- **Found during:** Task 2
- **Issue:** `GameViewModelTest.challengeLevelBelowOneIsClampedWithoutCrashing` asserted
  `viewModel.challengeLevel == 0` — i.e. it encoded the unclamped behaviour as correct while
  its own name claimed the opposite. It was one of the five RED failures.
- **Fix:** the assertion now reads `isEqualTo(ChallengeLevelBounds.MIN)`, matching the test's
  name and the advisory. No other pre-existing assertion was modified.
- **Note on the acceptance criterion:** Tasks 2 and 3 both say "no pre-existing test modified
  to accommodate the change". This edit is the opposite of accommodation — the test was
  wrong, and leaving it would have meant the fix could not ship. Recorded in
  `06-SECURITY-EVIDENCE.md` so it is visible at review rather than buried in a diff.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`
- **Commit:** `1e66039`

**2. [Rule 3 - Blocking] The `Int.MAX_VALUE` spec would have run the real generator 10,000 times**

- **Found during:** Task 3 test design
- **Issue:** `GameViewModelTest`'s `FakeLevelRepository` routes Challenge loads through the
  real `LevelGenerator`, which replays the deterministic chain from level 1. A forged
  `Int.MAX_VALUE` arg clamps to `MAX_CHALLENGE_LEVEL` (10,000), so the ViewModel would have
  asked it for ten thousand reverse walks to prove a one-line getter.
- **Fix:** added a `levelRepository` parameter (defaulted to the existing fake, so no
  existing call site changed) and a `RecordingChallengeLevelRepository` that serves a fixed
  board for any level. It does double duty: recording the requested level lets the spec
  assert the WARN-03 invariant *directly* — the level exposed equals the level whose board
  was loaded.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`
- **Commit:** `1e66039`

### Ordering Deviation (no scope change)

The plan put the regression tests in Task 3 and the fix in Task 2, but both tasks are marked
`tdd="true"` — writing the fix first would have made the RED observation impossible. The
tests were therefore written and committed as Task 2's RED step (`1e66039`), the fix as its
GREEN step (`7548b99`), and Task 3 reduced to the evidence document (`a10317a`) plus the
`GameViewModelResumeTest` extension that shipped in the same RED batch. All of Task 3's
acceptance criteria are met; only the commit boundary moved.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :domain:test` | green — no existing test modified, so generated levels are provably unchanged |
| `./gradlew check` | green |
| Total JVM tests | **320** (baseline 309; +4 `:domain`, +7 `:app`) — criterion was ≥313 |
| `grep -rn "const val MIN_CHALLENGE_LEVEL\|const val MIN_LEVEL" app/src/main domain/src/main` | 1 line (`DifficultyTiers.kt:82`, the deliberate exclusion) |
| `grep -c "coerceIn" GameViewModel.kt` | 0 |
| `grep -c "args.challengeLevel" ResultsViewModel.kt` | 1 (the clamp's own input) |
| `git diff --stat` on `GeneratorConstants.kt`, `DifficultyTiers.kt` | empty — untouched |
| detekt + ktlint (incl. `MagicNumber`) | green |
| RED observed before GREEN | 5 failures at `1e66039`, all passing at `7548b99` |

## Commits

| Commit | Type | What |
|--------|------|------|
| `a2f0610` | test | RED spec for `ChallengeLevelBounds` |
| `54aba29` | refactor | `ChallengeLevelBounds` + the D-15 consolidation |
| `1e66039` | test | RED regression specs at both WARN-03 boundaries |
| `7548b99` | fix | The WARN-03 clamps |
| `a10317a` | docs | WARN-03 closure evidence, DEBT-04 closed |

## Threat Model Outcomes

| Threat ID | Disposition | Outcome |
|-----------|-------------|---------|
| T-06-19 | mitigate | Getter delegates to `activeChallengeLevel`; 4 route-arg cases (3 RED-first) |
| T-06-20 | mitigate | One clamped value at all four `ResultsViewModel` sites; 3 cases (2 RED-first) asserting on the recorded `levelId` and `uiState` |
| T-06-21 | mitigate | D-15 consolidation; enforced by the one-line consolidation grep |
| T-06-22 | accept | `clamp` is fail-safe — the nearest legal level is played, nothing crashes or is refused |

No new security surface was introduced: no new endpoint, no new file access, no schema
change. The stored format is unchanged (the clamp only affects which *key* a result is
written under, and only for values that were never legal), so there is no Room migration and
no schema version bump.

## Known Stubs

None.

## Self-Check: PASSED

All 4 claimed files exist on disk; all 5 claimed commit hashes exist in `git log`.
`./gradlew check` green at 320 tests with a clean working tree.

## For Future Phases

- The Challenge level range is now **one** constant object. Raising the ceiling (the KDoc
  calls it "product-tunable upward if progression ever approaches it") is a single edit in
  `GeneratorConstants`, and every clamp site follows automatically.
- `ChallengeLevelBounds` is the model for D-04's Phase 9 `AssistAllowance`: a pure `:domain`
  policy object with zero Android deps, unit-tested on the JVM.
- Phase 6's remaining `:domain` budget is spent. D-15 was the one deliberate `:domain` touch
  in this phase and it was not extended beyond the consolidation.
