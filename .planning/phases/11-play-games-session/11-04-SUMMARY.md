---
phase: 11-play-games-session
plan: 04
subsystem: ui
tags: [play-games, settings, compose, snackbar, mvvm, tdd]

# Dependency graph
requires:
  - phase: 11-play-games-session
    plan: 01
    provides: "GamesService.session / signIn() return-vs-throw contract, GamesSignInState.SignedIn.displayName (D-07/D-14), open PlayGamesOptInRepository, FakeGamesService"
  - phase: 11-play-games-session
    plan: 03
    provides: "play_games_signin_error (the one shared D-10/D-18 failure string), the LaunchedEffect(tick) snackbar idiom, HomeViewModel.onAcceptPlayGamesPrompt as the handler shape to mirror"
  - phase: 09-consent-rewarded-ads
    provides: "SettingsSectionHeader / SettingsLinkRow / PrivacySection, ClaySnackbarHost on the Settings Scaffold, the *_ORDER clayEntrance stagger convention"
provides:
  - "SettingsUiState.gamesSignInState — the collected session, defaulting Unknown so the section is absent before the seam answers (D-11)"
  - "SettingsUiState.playGamesSignInErrorTick — the D-10 one-shot error ordinal on Settings"
  - "SettingsViewModel.onSignInToPlayGames() — the permanent manual sign-in entry point (D-10/D-19)"
  - "SettingsScreen.PlayGamesSection — the three-state section between General and Privacy (D-06/D-08/D-09/D-11/D-14)"
  - "PLAY_GAMES_HEADER_ORDER / PLAY_GAMES_ROW_ORDER — the stagger slots Phase 12's rows extend"
  - "FakePlayGamesOptInRepository in data/ — the shared D-17 double, now used by both ui.home and ui.settings"
affects: [12-leaderboards-achievements]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Guard-then-exhaustive-when: an early return for the hidden state, then a sealed when with no else, so the hidden case is unconditional and a future member is a compile error"
    - "One section, two identities: the same row composable renders both an offer and a statement, so signing in changes what a row says rather than what kind of thing it is"
    - "Section insertion as renumbering: entrance orders describe position in the column, so a new section shifts every constant below it"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/FakePlayGamesOptInRepository.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt

key-decisions:
  - "The Unknown guard is an early return placed before the header, not an if wrapped around the section body — the hidden state is then unconditional and cannot be half-implemented as a headed empty section"
  - "The when over GamesSignInState carries no else and still lists Unknown, so adding a fourth member breaks this build instead of silently shipping an invisible section"
  - "onSignInToPlayGames persists setOptedIn() unconditionally and before signIn(), so a cancelled Google chooser cannot resurrect the retired Home prompt (D-19)"
  - "The signed-in row reuses SettingsLinkRow with an empty onClick rather than a bespoke read-only composable, keeping the two states exact visual peers"
  - "FakePlayGamesOptInRepository was extracted from HomeViewModelTest into data/ rather than copied — the D-19 claim under test is that both entry points write the SAME flags, which a duplicated double would let drift"
  - "Section ordering is asserted through laid-out positionInRoot coordinates, not by reading the source, so D-08's placement survives a refactor that reorders the Column"

patterns-established:
  - "Guard-then-exhaustive-when for a section whose absence is a requirement: the early return owns 'hidden', the sealed when owns 'which row', and neither can absorb the other's job"
  - "Shared test doubles live beside the type they fake once a second package needs them (FakeGamesService, now FakePlayGamesOptInRepository) — a file-private copy per consumer is a drift contract"
  - "A one-shot error ordinal is threaded into an existing SnackbarHostState rather than adding a second host, so two failure paths on one screen queue instead of overlapping"

requirements-completed: [PGS-02, PGS-03]

# Metrics
duration: 47min
completed: 2026-09-04
---

# Phase 11 Plan 04: Settings Play Games Section Summary

**Settings now carries a Play Games section that is wholly absent until the session resolves, offers a working manual sign-in while signed out, and states the player's own name (or the D-14 fallback) once they are in — every state proven against `FakeGamesService`, with no Play Console project needed.**

## Performance

- **Duration:** ~47 min (excludes two full `check --offline` runs at ~11 min each)
- **Tasks:** 2 of 2
- **Files:** 11 (2 created, 9 modified)
- **Commits:** 3 (`e80c70c` RED, `5867df1` GREEN, `7eee3ca` the section)

## Accomplishments

- **The hidden state is structurally hidden, not conditionally hidden.** `PlayGamesSection`'s first statement is `if (gamesSignInState == GamesSignInState.Unknown) return`, placed *before* the header rather than as an `if` around the row. The tempting shape — header always, row conditionally — passes a casual read and ships exactly what D-11 forbids: a labelled section with nothing under it and an `Arrangement.spacedBy` gap where a row will later be. `SettingsPlayGamesTest` asserts the header's absence as its own test, not as a side effect of the row's.
- **A future `GamesSignInState` member cannot ship an invisible section.** The `when` has no `else` and still lists `Unknown` (unreachable past the guard, kept solely to hold the exhaustiveness gate open). Adding a fourth member to the sealed interface in Phase 12 or 13 becomes a compile error in this file rather than a branch that silently falls through.
- **The opt-in is persisted before the SDK is touched, and unconditionally.** `onSignInToPlayGames()` awaits `setOptedIn()` and only then calls `signIn()`. Persisting after the call — or only on success — would let a player who taps the row and then dismisses Google's chooser fall back onto the un-opted-in launch branch, with the one-time Home prompt they meant to retire returning next launch. Two tests pin this: one on the happy path, one specifically on the decline path (`aDeclinedSignInStillPersistsTheOptIn`).
- **D-10's decline-versus-error split reads identically on both entry points.** `onSignInToPlayGames()` is deliberately the same `runCatching { signIn() }.onFailure { tick++ }` shape as `HomeViewModel.onAcceptPlayGamesPrompt()`, against the same `play_games_signin_error` string resource plan 11-03 authored. One seam contract, one reading of it — a `false` return stays silent on both surfaces, and a throw raises the same sentence.
- **D-08's placement is asserted from the laid-out UI, not from the source.** `thePlayGamesSectionSitsBetweenGeneralAndPrivacy` compares the three headers' `positionInRoot.y`. A refactor that reorders the `Column` — or forgets that Phase-9 D-14 pins Privacy to the bottom — fails here rather than being caught by eye in review.
- **Entrance orders were renumbered, not appended.** Inserting the section between General and Privacy shifted the three Privacy constants to 5/6/7. Appending 6/7 to the end would have kept the diff small and silently broken the one invariant the constants exist to keep: the `clayEntrance` cascade must read strictly top-to-bottom, so an element cannot fade in above one that arrived before it.

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 (RED) | Failing session + sign-in coverage | `e80c70c` | `FakePlayGamesOptInRepository.kt`, `SettingsViewModelTest.kt`, `HomeViewModelTest.kt` |
| 1 (GREEN) | Session collector + `onSignInToPlayGames()` | `5867df1` | `SettingsUiState.kt`, `SettingsViewModel.kt`, 3 test harnesses |
| 2 | `PlayGamesSection`, strings, route, UI test | `7eee3ca` | `SettingsScreen.kt`, `SettingsRoute.kt`, `strings.xml`, `SettingsPlayGamesTest.kt`, `SettingsPrivacyTest.kt` |

## TDD Gate Compliance

Task 1 ran the full cycle. RED (`e80c70c`) was verified failing — `:app:compileDebugUnitTestKotlin` rejected `onSignInToPlayGames`, `gamesSignInState` and `playGamesSignInErrorTick` as unresolved before any production change existed. GREEN (`5867df1`) turned it green. No REFACTOR commit: nothing needed cleaning up afterwards.

Task 2 is not marked `tdd="true"` in the plan and was executed as a normal implement-then-test task, with `SettingsPlayGamesTest` written in the same commit as the section it covers.

## Test Coverage

| Suite | Tests | Covers |
|-------|-------|--------|
| `SettingsViewModelTest` | 16 (9 new) | `Unknown`/`SignedOut`/`SignedIn` mirroring, a later session change, opt-in persistence ordering, throw→tick, `false`→silence, a second failure bumping the ordinal again |
| `SettingsPlayGamesTest` | 11 | Section absent while `Unknown` (and the other two sections unaffected), D-09's exact copy while `SignedOut`, tap reaching the handler once, named + fallback signed-in rows, sign-in row gone once signed in, signed-in row non-tappable, D-08 ordering by laid-out position, the tick-driven snackbar, tick 0 silent, and the row surviving a failed attempt (D-13) |

`./gradlew check --offline` is green in full, including detekt, ktlint, ArchUnit `PlatformBoundaryTest` and Android Lint. The Settings section names only `GamesSignInState` — no Play Games SDK type reaches `ui/`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Widened `SettingsViewModel` constructor broke three sibling test harnesses**
- **Found during:** Task 1
- **Issue:** Adding `gamesService` and `optInRepository` broke every positional `SettingsViewModel(...)` call site in `SettingsViewModelSoundTest` (8 sites) and `SettingsPrivacyViewModelTest` (1 site). None are named in the plan's `files_modified`, but the test source set cannot compile without them.
- **Fix:** Each file gained a local `viewModelFor(...)` factory defaulting the two new collaborators to quiet doubles, rather than eight inline five-argument calls. The inline form was tried first and tripped detekt's `MaxLineLength` on eight lines, which is what pushed it to the factory — the better shape anyway, and the same one `SettingsViewModelTest` already uses.
- **Files modified:** `SettingsViewModelSoundTest.kt`, `SettingsPrivacyTest.kt`
- **Commit:** `5867df1`

**2. [Rule 3 - Blocking] `SettingsScreen`'s new required parameter broke `SettingsPrivacyTest`**
- **Found during:** Task 2
- **Issue:** `onSignInToPlayGames` has no default (matching every other handler on this screen), so `SettingsPrivacyTest.setContent` no longer compiled.
- **Fix:** Passed a no-op. Its assertions are unaffected — with the default `SettingsUiState`, `gamesSignInState` is `Unknown`, so the Play Games section is absent and the Privacy tests see the screen they always saw.
- **Files modified:** `SettingsPrivacyTest.kt`
- **Commit:** `7eee3ca`

### Discretionary Choices the Plan Delegated

**`FakePlayGamesOptInRepository` was extracted, not duplicated.** The plan offered "mirror plan 11-03's `HomeViewModelTest` fake, or share one if a natural common test-fixture location exists — your call." It was moved from `HomeViewModelTest.kt` (where it was file-private) to `app/src/test/java/com/app/pegsolitaire/data/FakePlayGamesOptInRepository.kt`, and `HomeViewModelTest` now imports it. The reasoning is the codebase's own, already written down twice: `FakeGamesService`'s KDoc explains it sits in `platform.games` precisely because `ui.home` and `ui.settings` both need it, and `FakeConsentGateway`'s KDoc says one fake "keeps those three from drifting apart." That argument applies with more force here — the D-19 claim under test is that both entry points write the *same* pair of flags, which two independent doubles would be free to disagree about. `HomeViewModelTest`'s own `NoopPreferencesDataStore` stayed put; `FakeOnboardingRepository` still uses it.

**Order of the `when` branches.** `SignedIn` is listed before `SignedOut`, against the plan's prose ordering. No behavioural difference; it puts the two-line `SignedOut` branch adjacent to the `Unknown -> Unit` line the comment explains.

## Known Stubs

None. Every row this plan renders is driven by real collected state, and the sign-in path reaches the real `PlayGamesService` bound by plan 11-02 at runtime.

## Threat Flags

None. The three threats the plan registered (`T-11-11` display-name disclosure, `T-11-12` dark-pattern nagging, `T-11-13` blocking composition) are all mitigated as specified and documented inline in `PlayGamesSection`'s KDoc: the display name is plain `stringResource`-formatted `Text`, never logged and never parsed; the row carries no dismiss counter or re-prompt logic and renders identically on every visit; and `onSignInToPlayGames()` returns immediately with the suspending round trip confined to `viewModelScope`. No new endpoints, auth paths, file access or schema changes were introduced.

## Requirements

`requirements-completed: [PGS-02, PGS-03]` — this plan's frontmatter requirements, now satisfied in code:

- **PGS-02** ("every feature works fully when sign-in is unavailable or declined, with social surfaces hidden rather than erroring"): the only Play-Games-dependent surfaces that exist today are the Home opt-in prompt (11-03) and this Settings section. The section is absent while `Unknown`, and every non-signed-in cause — declined, never opted in, no Play services, offline — collapses to `SignedOut` and renders a row that works or a snackbar that explains (D-12). `theOtherTwoSectionsStillRenderWhileThePlayGamesSectionIsHidden` pins that nothing else on the screen is collateral.
- **PGS-03** ("player can sign in manually from Settings when automatic sign-in did not happen"): the row exists, is tappable only while signed out, persists the opt-in and reaches `GamesService.signIn()`.

**PGS-01** was recorded by plan 11-03. Note that `REQUIREMENTS.md` still shows all three as `Pending`: the observed convention in this repo is that requirement checkboxes and the traceability table are flipped by the **phase-completion** step (see `efbc85c docs(phase-10): complete phase execution`, which marked INT-01..04), not by a plan executor mid-phase. This plan therefore records the claim in frontmatter and leaves the shared file to the orchestrator rather than racing it.

### Honest limits on those claims

Both are complete **in code and under JVM test**, and neither has been exercised against a live Play Games backend, because none exists yet (D-04). Concretely, today `isAuthenticated()` answers `false` for every account on every device, so a real tap on this row will surface the D-10 snackbar rather than sign anyone in. That is correct behaviour for the current world, but it means the signed-in half of PGS-03 — and the `SignedIn` row this plan renders — has been proven only against `FakeGamesService`.

This is pre-release debt, not silent omission, and it is already tracked in `11-VALIDATION.md`'s Manual-Only Verifications table and in ROADMAP External Prerequisites. Two items are specific to this plan:

1. With a real signed-in account, confirm the Settings row shows the player's actual Play Games display name — and confirm the D-14 fallback path is reachable rather than theoretical.
2. Confirm the D-10 snackbar fires on a genuine error and stays silent on a plain chooser dismissal. This is the same observation that promotes `PlayGamesService.signIn()`'s `[UNVERIFIED — D-04]` classification to a confirmed contract; the Settings row is now a second place that classification is visible from.

## Self-Check: PASSED

- All 7 asserted files present on disk (2 created, 5 of the modified set spot-checked).
- All 3 commit hashes present in `git log`: `e80c70c`, `5867df1`, `7eee3ca`.
- `SettingsPlayGamesTest` reports 11 tests, 0 failures, 0 skipped (plan required ≥5).
- All four of Task 1's and all four of Task 2's `acceptance_criteria` greps verified passing.
- `./gradlew check --offline`: BUILD SUCCESSFUL.

## Next Phase Readiness

Phase 11's UI surface is complete and Phase 12 has a place to land:

- **Phase 12's leaderboard and achievement rows** go into this same `PlayGamesSection` (D-08), inside the existing `SignedIn` branch, using the `PLAY_GAMES_ROW_ORDER` slot as their starting point. They will need orders inserted after it, which shifts the Privacy constants again — the KDoc on that constant block now says so explicitly.
- **`GamesService`'s five Phase-12 methods** remain no-ops in `PlayGamesService`; nothing in `ui/` calls them yet, and this plan added no call sites.
- **The one blocker for Phase 12 is unchanged and operator-owned:** the Play Console PGS project, OAuth credentials for both SHA-1s, and a tester account. Leaderboard submission cannot be verified — even manually — until those exist.
