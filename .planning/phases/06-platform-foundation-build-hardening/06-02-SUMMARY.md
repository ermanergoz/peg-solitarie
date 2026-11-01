---
phase: 06-platform-foundation-build-hardening
plan: 02
subsystem: security
tags: [resume-snapshot, kotlinx-serialization, untrusted-input, regression-test, junit, truth]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    provides: "SnapshotMapper / ResumeSnapshotDto resume codec and its fail-safe decode path; the 05-SECURITY.md advisory that named WARN-02"
provides:
  - "Coordinate bound at the resume-snapshot decode boundary (WARN-02 closed)"
  - "Six regression cases proving forged footprint coordinates cannot reach a Board"
  - "06-SECURITY-EVIDENCE.md — the committed DEBT-04 closure record, with a WARN-03 placeholder for plan 06-06"
affects: [06-06-warn-03-route-clamp, 13-cloud-save, phase-14-release-audit]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Fail-safe bound at an untrusted-input boundary: reject to null, let the existing whole-snapshot check escalate, never throw"
    - "Security advisory closure evidence committed as a phase artifact alongside the fix"

key-files:
  created:
    - .planning/phases/06-platform-foundation-build-hardening/06-SECURITY-EVIDENCE.md
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
    - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt

key-decisions:
  - "Bound is 0 until MAX_BOARD_POSITIONS (0..63), reusing the existing Long.SIZE_BITS constant — no new magic number, and no fail-closed risk on a future larger board"
  - "targetRow/targetCol are verified against the pre-existing `target in positions` guard rather than double-guarded, so there is no second bound that can drift"
  - "Tests assert at the public encode/decode/fromDto boundary via the existing restore() helper; no test reaches the internal toPositionOrNull"
  - "The plan's `decodeOrNull` symbol does not exist in this codebase — the real public boundary is decode + fromDto, which restore() already composes"

patterns-established:
  - "Coordinate/value bounds live in the tuple decode helper, one level below the validator, so target and footprint share one definition of 'plausible'"
  - "A security regression test must be observed RED against the unfixed code before it counts as proof (D-13)"

requirements-completed: [DEBT-04]

# Metrics
duration: 24min
completed: 2026-08-27
---

# Phase 6 Plan 02: WARN-02 Resume-Snapshot Coordinate Bound Summary

**Forged resume-snapshot footprint coordinates are now rejected at the decode boundary against the existing `MAX_BOARD_POSITIONS` constant, proven by six regression cases that were observed failing before the fix landed.**

## Performance

- **Duration:** ~24 min
- **Started:** 2026-08-27T00:00:00Z (worktree spawn)
- **Completed:** 2026-08-27
- **Tasks:** 2 (3 commits — TDD RED → GREEN → regression set)
- **Files modified:** 3 (2 modified, 1 created)

## Accomplishments

- `toPositionOrNull` now rejects any row or col outside `0 until MAX_BOARD_POSITIONS`, closing the gap the v1.0 audit named: a blob containing `[-5, 3]` or `[1073741824, 0]` previously built a degenerate off-canvas `Board`.
- The three rejection cases were written first and **observed failing** against the unbounded decoder (`eb706d1`), which is what makes them regression tests rather than restatements of current behaviour. D-13 requires exactly this.
- The `[63, 63]` boundary case pins the bound open: a tighter, current-board-sized bound would fail **closed** on a future larger board and silently destroy a player's in-progress game (T-06-05).
- `SnapshotMapper.kt` is byte-identical to HEAD. The fix sits one level below it, in the tuple decode helper, and `positions.size == inPlay.size` at `SnapshotMapper.kt:152` does the escalation to whole-snapshot rejection for free.
- WARN-02 closure evidence is committed, with a WARN-03 placeholder section for plan 06-06 to fill in.

## Task Commits

1. **Task 1: Bound decoded coordinates in `toPositionOrNull`** (tdd)
   - RED — `eb706d1` (`test`): four cases added; the three forgery cases fail (`15 tests completed, 3 failed`)
   - GREEN — `de5a1c3` (`fix`): the bound, its KDoc, and the fail-safe rationale
2. **Task 2: Six WARN-02 regression cases + closure evidence** — `86922c8` (`test`)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` — `toPositionOrNull` (`:127`) reads row/col into locals and returns `null` when either falls outside `0 until MAX_BOARD_POSITIONS`; KDoc names WARN-02, the `Long`-peg-mask derivation of the bound, and why a tighter bound was rejected.
- `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt` — six new `@Test` methods, a `forgedFootprint()` helper that drops the move history so footprint validation is the only thing that can reject a blob, six new named constants, and a class KDoc that now names WARN-02 alongside T-05-03 / T-05-04.
- `.planning/phases/06-platform-foundation-build-hardening/06-SECURITY-EVIDENCE.md` — advisory location, bound + rationale, fix site, the six test names, the command, and the `## WARN-03` placeholder.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapperTest"` | green — **11 → 17** tests |
| `./gradlew check --offline` | **BUILD SUCCESSFUL** — 305 JVM tests (231 `:app` + 74 `:domain`) |
| Baseline preserved (D-19) | 305 − 6 new = **299**, exactly the phase-start reference |
| Pre-existing tests modified to accommodate the change | **none** |
| `grep -c MAX_BOARD_POSITIONS ResumeSnapshotDto.kt` | 4 (≥ 2 required) |
| `grep -q WARN-02 ResumeSnapshotDto.kt` | present |
| `git diff --stat … SnapshotMapper.kt` (vs. plan base) | empty — untouched |
| `grep -c toPositionOrNull SnapshotMapperTest.kt` | **0** — no test reaches past the public boundary |
| detekt + ktlint | green |

## Decisions Made

- **Bound = `0 until MAX_BOARD_POSITIONS`.** Reuses the constant that already exists because a `Board`'s peg mask is one `Long`. A 64-cell footprint could in principle be a 1×64 line, so 63 is the largest coordinate any legal board could carry. CLAUDE.md's no-magic-numbers rule is satisfied without adding anything.
- **The `63` constant in the *test* is a literal, not a reference to `MAX_BOARD_POSITIONS`.** Deriving the test's expectation from the implementation's constant would make the test silently follow a future change to the bound. A regression test must break when the bound moves; its KDoc records that reasoning.
- **Forgery cases drop the move history.** Appending an out-of-range coordinate shifts `BoardSpec`'s sorted bit indices, which could make a stored move illegal and reject the blob for the *wrong reason*. `forgedFootprint()` clears `moves` so footprint validation is the only possible cause of rejection — the same technique the existing peg-mask test already uses.
- **`[63, 63]` keeps its move history.** It sorts after every English-33 hole, so it takes the last bit index and leaves the stored peg mask meaning exactly what it meant before — which makes the full replay a stronger assertion than an empty one.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created `local.properties` in the worktree**
- **Found during:** Task 1 (baseline test run)
- **Issue:** `local.properties` is gitignored, so the fresh worktree had none and every Gradle task failed with "SDK location not found". No test could run.
- **Fix:** Wrote the worktree's own `local.properties` with the same `sdk.dir` as the main checkout.
- **Files modified:** `local.properties` (gitignored — deliberately **not** committed)
- **Verification:** `./gradlew :app:testDebugUnitTest` resolves and runs.
- **Committed in:** n/a — gitignored by design (D-08's "IDs and machine paths are never committed" rule applies).

### Plan-Text Corrections

**2. [Rule 3 - Blocking] `SnapshotMapper.decodeOrNull` does not exist**
- **Found during:** Task 2 (writing the assertions)
- **Issue:** PLAN.md, RESEARCH.md and VALIDATION.md all name `decodeOrNull` as "the public decode boundary", including the acceptance criterion `grep -c "decodeOrNull" SnapshotMapperTest.kt` ≥ 6. There is no such function at HEAD. The public API is `decode(blob)` (JSON parse only — it accepts a forged coordinate, because the value *is* a structurally valid `Int`) plus `fromDto(dto, rulesEngine)` (the validator). Asserting on `decode` alone would have produced six tests that pass on the unfixed code and prove nothing.
- **Fix:** Asserted through the test file's existing `restore()` helper — `decode(encode(dto))?.let { fromDto(it, RulesEngine) }` — which is the full on-disk round trip and therefore the real boundary the plan meant. The literal grep criterion is unsatisfiable; the semantically equivalent check is `grep -c "restore(" SnapshotMapperTest.kt` → **10**, with `grep -c toPositionOrNull` → **0** (the criterion that actually carries the intent: no test reaches past the public boundary).
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`
- **Verification:** All three forgery cases observed RED against the unfixed decoder, then GREEN after it — which is only possible because the assertions run through `fromDto`.
- **Committed in:** `eb706d1`, `86922c8`

**3. [Plan structure] Task/test split shifted to preserve a real RED gate**
- **Found during:** Task 1
- **Issue:** The plan puts the implementation in Task 1 and *all six* tests in Task 2. Executed literally, every test in Task 2 would be written against already-fixed code and could never be observed failing — which is precisely the "fix without proof" that D-13 rejects.
- **Fix:** Task 1's four `<behavior>` cases were written first as the RED gate (3 genuinely fail, `[63, 63]` passes on both sides by design since it is the fail-**closed** guard); Task 2 added the remaining two cases (target out-of-range, English-33 non-regression), the class KDoc, and the evidence document. Six new tests total, matching the plan's `<behavior>` set exactly.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`
- **Verification:** `15 tests completed, 3 failed` at RED; 17/17 green at GREEN.
- **Committed in:** `eb706d1`, `de5a1c3`, `86922c8`

---

**Total deviations:** 3 (1 blocking environment fix, 1 blocking plan-text correction, 1 task-ordering correction)
**Impact on plan:** No scope change. Every file the plan named was produced; every `<behavior>` case was implemented. The two corrections exist because following the plan's literal text would have produced tests that could not fail, defeating D-13's whole point.

## TDD Gate Compliance

| Gate | Commit | Evidence |
|------|--------|----------|
| RED | `eb706d1` (`test`) | `15 tests completed, 3 failed` — the three forgery cases fail against the unbounded decoder |
| GREEN | `de5a1c3` (`fix`) | 17/17 green; `--tests "*ResumeRepositoryTest"` also green |
| REFACTOR | — | Not needed; the guard is five lines and the sibling `toMoveOrNull` already established the shape |

The GREEN commit is typed `fix` rather than `feat` because the change corrects a security defect in shipped v1.0 behaviour rather than adding a feature. Gate ordering (test → implementation) is intact.

## Threat Flags

None. This plan removed attack surface rather than adding any: no new endpoint, no auth path, no file access, no schema change. The stored blob format is unchanged, so there is no Room migration and no schema version bump — a v1.0 snapshot on a user's device still decodes.

## Issues Encountered

- The naive forgery construction (append `[-5, 3]`, keep the move history) rejects for an *ambiguous* reason: the out-of-range coordinate sorts first in `BoardSpec`, shifting every English-33 bit index by one and potentially making a stored move illegal at replay. Resolved by clearing `moves` in `forgedFootprint()`, so a green test can only mean "footprint validation rejected it".

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **DEBT-04 is half closed.** WARN-02 is done and evidenced; plan **06-06** owns WARN-03 (`GameViewModel.kt:125`'s unclamped `challengeLevel` getter reaching `Results` on the non-restore path) and must fill in the `## WARN-03` section already stubbed in `06-SECURITY-EVIDENCE.md`. That plan depends on D-15's consolidated `ChallengeLevelBounds` in `:domain`.
- **Note for whoever runs the D-12 size/cold-start baseline:** a fresh worktree needs its own `local.properties` before any Gradle task will resolve.
- No blockers. `./gradlew check --offline` is green (D-18's enforced invariant), and the 299-test reference from D-19 is preserved exactly, plus six.

## Self-Check: PASSED

- Files claimed created/modified — all present: `ResumeSnapshotDto.kt`, `SnapshotMapperTest.kt`, `06-SECURITY-EVIDENCE.md`, `06-02-SUMMARY.md`
- Commits claimed — all present in `git log`: `eb706d1`, `de5a1c3`, `86922c8`
- No tracked file deleted by any commit in this plan (`git diff --diff-filter=D` vs. plan base is empty)
- `STATE.md` and `ROADMAP.md` deliberately untouched — the orchestrator owns those writes

---
*Phase: 06-platform-foundation-build-hardening*
*Completed: 2026-08-27*
