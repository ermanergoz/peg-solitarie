---
phase: 07-sound
plan: 04
subsystem: results-celebration-audio
tags: [audio, results, celebration, launched-effect, saved-state-latch, reduced-motion]
requires:
  - "SoundPlayer.playStarPop / SoundParams.starPopRate — the star-pop ladder from plan 07-02"
  - "SoundParams.CELEBRATION_LEAD_IN_MS / STAR_POP_STAGGER_MS — the celebration timings (07-02)"
  - "LocalSoundPlayer + FakeSoundPlayer — the Compose seam and recording double (07-02)"
  - "ClayAnim.STAR_STAGGER_MS — the visual star stagger the audio lands with"
  - "recordSolvedPlayOnce() — the SavedStateHandle play-once idiom this plan clones"
provides:
  - "ResultsUiState.playCelebration — the D-16 one-shot celebration latch"
  - "ResultsViewModel.claimCelebrationPlayOnce() + CELEBRATED_KEY"
  - "StarPopSounds — the staggered rising-pitch pop sequence, called from StarRow"
affects:
  - "app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt (StarRow gained a parameter)"
tech-stack:
  added: []
  patterns:
    - "SavedStateHandle-backed play-once latch surfaced as a UiState flag (second instance of the idiom)"
    - "LaunchedEffect sequence keyed on (latch, count), spaced by delay() rather than by an animation"
    - "Robolectric + frozen mainClock (autoAdvance = false) to assert audio timing deterministically"
key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsCelebrationSoundTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsCelebration.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
decisions:
  - "The celebration latch is a SECOND SavedStateHandle key, not a reuse of RECORDED_KEY — the record guard is set only for a solved play, while every result celebrates its stars, so sharing one key would silence the stuck one-star finish"
  - "StarPopSounds deliberately breaks ResultsCelebration.kt's own animator-duration-scale symmetry (D-13); the file-header comment was rewritten so it no longer claims otherwise"
  - "Audio timing is asserted against a frozen mainClock with explicit advanceTimeBy, so the D-14 lead-in gap and the D-13 stagger are pinned facts rather than sleeps"
metrics:
  duration: ~35 min
  completed: 2026-08-28
---

# Phase 7 Plan 04: Results Celebration Pops Summary

The Results screen now has the ceremony half of D-14: after a deliberate gap past the win
stinger that fired on the board, one pop per earned star climbs a ladder whose top note
only a 3-star finish ever plays — in step with the stars landing, still sequenced when the
OS says "no animations", and silent on every re-entry after the first.

## What Was Built

**Task 1 — the D-16 play-once latch** (`3174e0b`).

`ResultsUiState` gained `playCelebration: Boolean = false`, documented in the file's
`@property` voice beside `newBests` — its nearest relative, since both are one-shot cues a
recomposition cannot re-fire. The difference is scope, and the KDoc says so: `newBests`
survives a recomposition, `playCelebration` survives the *ViewModel*.

`ResultsViewModel.claimCelebrationPlayOnce()` is `recordSolvedPlayOnce()`'s shape with its
condition removed and its ordering discipline kept. It reads `CELEBRATED_KEY` from the
`SavedStateHandle`, returns if the claim is already made, otherwise sets the key **and
then** raises the flag. That ordering is the whole mechanism: both re-entry paths D-16
names — a process-death restore and a back navigation onto the same Results entry — arrive
as a second construction over the same handle, and a second instance can only observe a
claim that was written synchronously in `init`, never inside a launched coroutine.

`CELEBRATED_KEY` is deliberately a *second* key. `RECORDED_KEY` guards the bests merge and
is set only for a solve (D-08); a stuck one-star Challenge finish never sets it. Sharing one
key would therefore have made the stuck result celebrate forever *and* the solved result's
first pop depend on a database write. The two guards are independent and asserted to be.

Five new `ResultsViewModelTest` cases (**21 total**, up from 16): first entry claims it; a
second ViewModel over the same handle does not — with no `runCurrent()` between the two
constructions, which is what proves the write is synchronous; a stuck one-star result still
celebrates while recording nothing; the two latches are independent in both directions; and
a solved re-entry is silent while still recording exactly once.

**Task 2 — the rising sequence** (`fff6c01`).

`StarPopSounds(earned, play)` is an `internal @Composable` in `ResultsCelebration.kt` that
emits no UI. It reads `LocalSoundPlayer.current` and `rememberAnimationsEnabled()`, then
runs a `LaunchedEffect(play, earned)` that returns immediately unless the latch is set,
waits `SoundParams.CELEBRATION_LEAD_IN_MS`, and fires `repeat(earned) { playStarPop(step) }`
with a `delay` between rungs.

The stagger is the interesting line. With animations on it is `ClayAnim.STAR_STAGGER_MS` —
the very token `EarnedStar` passes to `rememberPopIn`, so each pop lands *with* its star
rather than near it. With animations off it is `SoundParams.STAR_POP_STAGGER_MS`. Neither
value is duplicated; `120` does not appear in the file at all.

`StarRow` gained a `playCelebration` parameter and calls the helper. The flag is hoisted
from `ResultsUiState` and explicitly not `remember`ed — a `remember` survives neither of
the two paths the latch exists to cover, so the row stays stateless and the latch stays in
the one place that outlives the composition.

`ResultsCelebrationSoundTest` is **7 Robolectric tests** driving the real `ResultsScreen`
with a recording `FakeSoundPlayer` installed through `CompositionLocalProvider`, over a
frozen `mainClock` (`autoAdvance = false`): the full three-note ladder in order, the 2-star
prefix asserted both positively and by the *absence* of `starPopRate(2)`, the 1-star single
pop, the lead-in gap (nothing has sounded 200ms in), total silence at 3 stars with the latch
false, the D-13 case (animator duration scale forced to 0 through the Robolectric settings
shadow — exactly one pop has landed 300ms in, proving a sequence rather than a burst, and
all three arrive in order), and a recomposition that changes an unrelated field replaying
nothing.

## Key Decisions

**Two keys, not one.** The cheap move was to reuse `RECORDED_KEY` — it already means "this
result has been dealt with". It is wrong for a reason worth writing down: that key is set
inside a `pegsRemaining == 1` guard, so it is *never* set for a stuck board. A stuck board
reaching Results would then re-claim the celebration on every single entry, which is the
exact bug D-16 exists to prevent, and the failure would only show on the outcome nobody
demos.

**The file's own header comment was wrong after this change, so it was rewritten.**
`ResultsCelebration.kt` opened by promising that *both* helpers respect the OS
animator-duration-scale gate. `StarPopSounds` does not, on purpose (D-13): audio that
collapses its stagger to zero is one muddy chord, not a reduced-motion accommodation. The
header now names the exception and points at the helper's KDoc, so the next reader does not
"fix" the asymmetry back into a bug.

**Timing asserted, not slept.** `mainClock.autoAdvance = false` plus explicit
`advanceTimeBy` turns "the first pop trails the stinger" and "the pops are staggered, not
simultaneous" into deterministic assertions. The two boundary probes (200ms, 300ms) sit
comfortably inside their windows — 250ms lead-in, 150ms fallback stagger — so the frame
quantisation `advanceTimeBy` applies cannot flip either one.

## Deviations from Plan

### Auto-fixed Issues

None. Both tasks were implemented as written; every acceptance criterion passed on its
first run.

### Process note

The plan marks both tasks `tdd="true"`. Both were executed RED-first — the tests were
written and run against the unmodified source before any implementation (Task 1: unresolved
`playCelebration`; Task 2: 6 of 7 tests failing, with only the "silent re-entry" case
passing vacuously) — but each task is a single commit rather than a `test`/`feat` pair. That
matches the phase's established history (plans 07-02 and 07-03 both commit one task at a
time) and keeps every commit in the branch compiling.

### Authentication Gates

None.

## Verification Performed

- `./gradlew :app:testDebugUnitTest --tests "…ResultsViewModelTest" --offline` — **21 tests,
  0 failures** (JUnit XML); all 16 pre-existing cases still pass, including
  `recreatingTheViewModelOverTheSameSavedStateRecordsOnlyOnce`.
- `./gradlew :app:testDebugUnitTest --tests "…ResultsCelebrationSoundTest" --tests
  "…ResultsScreenTest" --offline` — **7** and **5 tests**, 0 failures; `ResultsScreenTest` is
  unchanged and still green.
- `./gradlew :app:check --offline` — BUILD SUCCESSFUL: **329 JVM tests, 0 failures, 0
  errors**, plus detekt, ktlint (main + test source sets), `dpSpLiteralCheck`,
  `adIdLiteralCheck`, `audioFocusLiteralCheck` and lint.
- Structural gates, confirmed by grep:
  - `ResultsCelebration.kt` contains `internal fun StarPopSounds(`, `playStarPop(`,
    `CELEBRATION_LEAD_IN_MS`, and **both** `ClayAnim.STAR_STAGGER_MS` and
    `SoundParams.STAR_POP_STAGGER_MS` — and **zero** occurrences of the literal `120`.
  - The file-header comment names D-13 and no longer claims all helpers respect the gate.
  - `ResultsScreen.kt` contains `StarPopSounds(earned = earned`, `StarRow` takes
    `playCelebration`, sourced from `state.playCelebration`; `remember {` appears **0**
    times in the file, so nothing guards the celebration locally.
  - `ResultsViewModel.kt` contains `CELEBRATED_KEY = "results_celebrated"` and
    `claimCelebrationPlayOnce()`, called from the existing `init`; the assignment
    `savedStateHandle[CELEBRATED_KEY] = true` precedes the `_uiState.update`.
  - `grep -c 'RECORDED_KEY'` on `ResultsViewModel.kt` is **3**, identical to `HEAD~2` — the
    existing latch was extended around, never repurposed.

## Requirements

**SOUND-03 is complete.** Its first beat landed in plan 07-03 (the win stinger fires as the
winning move lands on the board, D-14); this plan lands the second (the star pops sequenced
in rising pitch to the existing Results celebration animation). Both halves are asserted
JVM-side. The perceptual judgement — whether the triad *sounds* like a celebration — is the
D-04 audit at phase verification, not a code claim.

**SOUND-04 stays open** — button cues are plan 07-05.

## Threat Flags

None. No network endpoint, auth path, schema change or permission is added. The plan's four
registered threats are each addressed as registered: T-07-02 (rate totality) by
`starPopRate`'s clamp plus `step` being bounded by `earned`, asserted by the ordered
three-rate test; T-07-10 (a tampered latch) accepted — the boolean's only reach is whether a
sound plays, and the bests merge keeps its own untouched guard; T-07-11 (unbounded sequence)
mitigated by `repeat(earned)` inside a `LaunchedEffect` keyed on `(play, earned)` and
cancelled with the composition, asserted by the no-replay-on-recomposition test; T-07-06
accepted — the effect dies with the screen and every pop is a sub-second one-shot.

## Known Stubs

None. Every declaration added is wired and exercised by a test.

## Self-Check: PASSED

All five modified and one created source file verified present on disk; both commit hashes
(`3174e0b`, `fff6c01`) verified present in `git log`.
