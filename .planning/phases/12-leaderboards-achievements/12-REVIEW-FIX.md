---
phase: 12-leaderboards-achievements
fixed_at: 2026-09-14T00:00:00Z
review_path: .planning/phases/12-leaderboards-achievements/12-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 12: Code Review Fix Report

**Fixed at:** 2026-09-14
**Source review:** `.planning/phases/12-leaderboards-achievements/12-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (CR-01, WR-01, WR-02, WR-03 — IN-01..IN-06 out of scope)
- Fixed: 4
- Skipped: 0

All work was done in an isolated git worktree on a temporary branch and
fast-forwarded onto `feature/phase-12-leaderboards-achievements`. Each finding is
one atomic commit.

## Verification

Per-commit gates (all `--offline`): `:app:compileDebugKotlin`,
`:app:compileDebugUnitTestKotlin`, `:domain:compileTestKotlin`, the affected test
classes, then `detekt ktlintCheck`.

**Final full run** — `./gradlew --offline :domain:test :app:testDebugUnitTest detekt ktlintCheck`:

```
BUILD SUCCESSFUL
956 tests, 0 failures, 0 errors
```

`ktlintFormat` was never needed; `ktlintCheck` passed first time on every commit.
`DomainPurityTest` and the platform-boundary gates stayed green, so `:domain`
remains Android-free.

## Fixed Issues

### CR-01: Diamond-41 wins are never recorded

**Files modified:** `app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/game/GameFinish.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt`,
`domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardConstants.kt`,
`app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`

**Commit:** `80a4df7`

**Applied fix:**
- `GameFinish` gained `minimumPegs`, populated from `nextBoard.spec.minimumPegs` —
  the same value the stars were already banded against. `ScoreResult` was left
  alone: it is a scoring type and the review's `finish.result.minimumPegs` sketch
  would have put an achievement-facing fact in it.
- `Results` route gained `minimumPegs: Int = BoardConstants.MINIMUM_PEGS_DEFAULT`
  (i.e. 1), so every pre-Phase-12 call site and any restored older back stack keeps
  its previous meaning. `GameRoute` is the only producer.
- `ResultsViewModel` clamps it once at the nav boundary, the way `challengeLevel`
  is clamped, and derives a single `isSolved = pegsRemaining <= minimumPegs` used by
  the record/submit gate, by `tierFor`'s Challenge-stuck branch, and by
  `ResultsUiState.isSolved`/`showNextLevel`. Both `PEGS_REMAINING_SOLVED` constants
  were deleted, which also closes IN-06 as a side effect.

**Deliberate deviation from the review's fix sketch — read this.** The review
offered `minimumPegs.coerceIn(1, pegsRemaining.coerceAtLeast(1))` as the clamp.
That clamp is unsound: it makes *every* forged value satisfy
`pegsRemaining <= minimumPegs`, so a forged arg on the independently-reachable
`Results` route would declare a barely-played board solved and earn it a
`level_result` row, a `play_session` row and a leaderboard submission. The review's
own alternative was taken instead — a new
`BoardConstants.HIGHEST_MINIMUM_PEGS` (derived from
`DIAMOND41_FEWEST_PEGS_FLOOR`, the highest floor any shipped board declares) is the
ceiling, and `MINIMUM_PEGS_DEFAULT` the floor. Two regression tests pin both ends.

**Tests added:** `aPerfectDiamond41FinishAtItsTwoPegFloorIsRecordedAndSubmittedLikeAnyOtherSolve`
(records once, mode classic / levelId diamond41, then `onResultRecorded()`, in that
order), `aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded` (the one-peg Challenge
board with 2 pegs left still records nothing),
`aForgedHugeMinimumPegsArgCannotDeclareABarelyPlayedBoardSolved`,
`aForgedNonPositiveMinimumPegsArgStillLetsAGenuineSolveBank`.

> **Status: fixed — requires human verification.** This is a logic change to a
> gate that decides what is persisted and submitted. The unit suite is green and
> the four new cases pin the intended behaviour, but confirm on-device that a real
> Diamond-41 solve now produces a `level_result` row, a `play_session` row, star
> pips in the Classic catalog, and a `CLEARED_DIAMOND41` unlock.

### WR-01: Backfill trigger read the pending flag outside the pass mutex

**Files modified:** `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt`,
`app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt`

**Commit:** `4807c8a`

**Applied fix:** `submitAll` takes `onlyIfPending: Boolean` and reads
`syncStore.pending.first()` inside `passMutex`, after the session gate (so a
signed-out trigger still sets the flag whatever its reason for running). `start()`'s
collector passes `true`, `onResultRecorded()` passes `false`.

**Test added:** `aSignInThatLandsWhileTheFlagWriteIsStillInFlightStillRunsItsBackfillOnce`
— the fake store's `setPending` parks on a `CompletableDeferred`, the session flips
to `SignedIn` while the write is held, and exactly one pass runs and really submits.
**Confirmed as a genuine regression guard:** with the collector's ordering reverted
the new test fails and the other 19 still pass. All 20 `ProgressSubmitterTest` cases
are green with the fix.

### WR-02: Submissions waited up to 10 s per call for a foreground Activity

**Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`,
`app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`

**Commit:** `ffbe012`

**Applied fix:** `submitting()` now reads `activityProvider.current` and returns
`false` immediately when it is null, instead of `awaitForegroundActivity()`. This
covers all three submission paths (`submitScore`, `unlockAchievement`,
`setAchievementSteps`). `launchStockUi`'s wait is untouched, and the
`CancellationException` rethrow still precedes any classification.

**Tests updated:** the four cases that must reach the SDK now go through a new
`signedInServiceInForeground()` helper (the class-wide default leaves
`activityProvider.current` null, which is exactly the backgrounded case). **Test
added:** `aSubmissionWithNoForegroundActivityFailsAtOnceRatherThanWaitingForOne` —
all three paths return `false`, `testScheduler.currentTime` does not advance by a
single virtual millisecond, and `awaitActivity()` is never entered. 31
`PlayGamesServiceTest` cases green.

### WR-03: Assist-counter defaults documented as "fail-CLOSED" while behaving fail-open

**Files modified:** `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt`,
`app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`,
`app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`,
`app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt`,
`domain/src/main/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetry.kt`,
`app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`,
`app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`,
`domain/src/test/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetryTest.kt`

**Commit:** `027ea09`

**Applied fix, part (a) — prose only, no behaviour change.** D-09's "absent fields
on an old snapshot read as zero" is LOCKED and the `null → 0` default was not
touched anywhere. The four KDocs (`ResumeSnapshotDto.hintsUsed`/`undosUsed`,
`SnapshotMapper`'s `RestoredGame` / `NO_ASSISTS_RECORDED` / `fromDto`,
`GameViewModel.applyRestored`, the `Results` route) now state the true direction:
absent resolves to 0 per D-09, **fail-OPEN toward the achievement**, accepted
because the store is local-only and unsynced and the exposure is one self-awarded
badge.

**Applied fix, part (b) — negatives treated as tampering.**
`SnapshotMapper.fromDto` refuses the resume (returns `null`) for a negative counter,
via a new `hasForgedAssistCounter()` guard — consistent with its existing rules for
forged moves, footprints and minimum-peg targets, and safe because absence is
handled by a separate `null` branch that never reaches it. `orNoAssists()` lost its
`coerceAtLeast`, since a second answer to the same question is exactly the drift one
resolver exists to prevent. At the nav boundary, where refusing would cost an honest
player their recorded best, `SolveTelemetry.coerced` resolves a negative to a new
named constant `SolveTelemetry.UNACCOUNTED_ASSIST = 1` (the purity predicates are
`= 0` over `play_session`, so any non-zero value disqualifies) instead of 0. Forged
large counts still pass through verbatim. `:domain` gained no Android imports.

**Note on where the constant lives.** The instruction was to add it "in the existing
thresholds/constants object". There is no constants object in
`domain/telemetry/`, and putting it in `domain.achievement.AchievementThresholds`
would have made `SolveTelemetry` import the achievement package — the exact coupling
`SolveTelemetry`'s own KDoc forbids ("it is **not** an input to
`AchievementRules.evaluate`"). It therefore sits as a KDoc'd `const val` in
`SolveTelemetry`'s companion, beside `NONE`, which is the same kind of scoped policy
value. Flagging it in case you want it moved.

**Tests renamed and rewritten to the new behaviour:**
- `aForgedNegativeAssistCounterClampsToZeroWithoutRefusingTheResume` →
  `aForgedNegativeAssistCounterRefusesTheResumeLikeAnyOtherForgedField` (each
  counter alone, both together, plus the untouched control that still restores).
- `coercedClampsANegativeHintCountToZero` →
  `aForgedNegativeHintCountDisqualifiesPurityRatherThanResolvingToZero`;
  `coercedClampsANegativeUndoCountToZero` → the undo equivalent;
  `coercedClampsBothCountersIndependently` →
  `bothForgedNegativeCountersAreResolvedIndependentlyAndTheFinishFlagSurvives`.
  Each asserts `isNotEqualTo(0)` explicitly so a regression to zero cannot pass.
- `coercedOfZeroYieldsTheSameValueAsNone` kept, with a comment stating that the
  absent case DOES grant purity and that this is deliberate.
- Two knock-on updates in `ResultsViewModelTest`:
  `aNegativeCounterArgIsClampedToZeroRatherThanRefused` →
  `aNegativeCounterArgIsResolvedAgainstPurityRatherThanRefusedOrZeroed`, and
  `theTelemetryArgsAreInertForEverythingButSolveTelemetry`'s expectation.

## Skipped Issues

None.

## Out of scope (not attempted)

IN-01 (unreachable `null` branch in `submitLeaderboards`), IN-02
(`CLASSIC_BOARD_TOTAL` dead), IN-03 (`data` ↔ `platform.games` package cycle),
IN-04 (no in-flight guard on the stock-UI handlers), IN-05 (unchecked cast in
`app/build.gradle.kts`). IN-06 (duplicate `PEGS_REMAINING_SOLVED` constants) was
closed incidentally by CR-01, since folding the three solved checks into one
`isSolved` is what removed both constants.

---

_Fixed: 2026-09-14_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
