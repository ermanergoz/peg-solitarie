---
phase: 06-platform-foundation-build-hardening
plan: 04
subsystem: platform
tags: [platform-seam, play-games, plat-03, sealed-type, no-op-stub]
requires:
  - "RewardOutcome (plan 06-01) — the sealed-type precedent and the stub-contrast prose this plan restates against"
provides:
  - "GamesSignInState — sealed observable Play Games sign-in state (Unknown/SignedOut/SignedIn)"
  - "GamesService.session — observable sign-in state on the seam"
  - "GamesService.unlockAchievement / incrementAchievement — fire-and-forget achievements"
  - "GamesService.showLeaderboard / showAchievements — stock Play Games UI intents, suspend, no Activity"
  - "NoOpGamesService — silently-degrading stub for every new member, permanently SignedOut"
affects:
  - "Phase 11 (PGS v2 implementation) — binds a real GamesService against this contract"
  - "Phase 12 (achievements/leaderboards) — consumes unlockAchievement/incrementAchievement/session"
  - "plan 06-05 PlatformBoundaryTest — structurally enforces the no-Activity/no-SDK-type rule set here"
tech-stack:
  added: []
  patterns:
    - "sealed interface for states carrying a payload (GamesSignInState), enum for payload-free states (GameStatus)"
    - "private MutableStateFlow + .asStateFlow() so the mutable handle is never leaked through the seam"
    - "suspend on UI-intent methods so a later impl can await a foreground Activity with a timeout"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
decisions:
  - "session reports SignedOut, never Unknown — a stub that never answers would leave a future surface waiting on a resolution that never arrives"
  - "No fail-open third answer on the games seam (unlike RewardOutcome.Unavailable): a missing leaderboard costs the player nothing, so declining is always safe"
  - "SignedIn split across lines against the plan's single-line grep criterion — ktlint_official class-signature forces multiline for any constructor parameter"
metrics:
  duration: ~12 min
  completed: 2026-08-27
  tasks: 2
  commits: 3
  tests-added: 7
  tests-total: 316
---

# Phase 6 Plan 04: GamesService Seam Extension Summary

Extended the `GamesService` platform seam (PLAT-03) with an observable sealed sign-in
state, two fire-and-forget achievement methods, and the two stock Play Games UI intents —
all implemented as silent no-ops that keep the stub reporting "not connected".

## What Was Built

**`GamesSignInState`** — a sealed interface with exactly three members: `Unknown` (the
pre-first-check state, explicitly documented as *not* a failure), `SignedOut`, and
`SignedIn(playerId)`. It is the repo's second sealed type, after `RewardOutcome` from plan
06-01. The KDoc records *why* it is sealed rather than an enum: `SignedIn` carries a
payload the other two members have no meaning for, which is the exact inverse of the
reason `GameStatus` stays an `enum`. `playerId` is a plain `String`, so observing the
session leaks no Play Games SDK type.

**`GamesService`** gained five additive members. `signIn()` and `submitScore(...)` were
left untouched:

| Member | Shape | Why |
|--------|-------|-----|
| `session` | `val StateFlow<GamesSignInState>` | Surfaces react to sign-in resolving asynchronously instead of holding a stale answer from one `signIn()` call |
| `unlockAchievement(id)` | non-suspend | Matches the existing fire-and-forget `submitScore` precedent |
| `incrementAchievement(id, steps)` | non-suspend | Same; two params so ktlint's forced one-per-line wrapping applies |
| `showLeaderboard(id)` | `suspend`, returns `Unit` | PGS v2 returns no result the app must act on; `suspend` lets Phase 11 await a foreground Activity with a timeout |
| `showAchievements()` | `suspend`, returns `Unit` | Same |

**`NoOpGamesService`** implements all five silently. `session` is a private
`MutableStateFlow(SignedOut)` exposed via `.asStateFlow()`; the other four are `= Unit`.

## Key Decisions

**`SignedOut`, not `Unknown`, for the stub.** `Unknown` means "the first check has not
answered yet". A stub that reported it would never answer, leaving any future surface
waiting forever on a resolution that cannot come. `SignedOut` is the honest terminal
answer: Play Games genuinely is not wired up.

**The games seam gets no fail-open third answer.** `RewardOutcome` needs `Unavailable`
because a missing ad would otherwise cost the player an assist (ADS-03). A missing
leaderboard costs the player nothing, so declining is always the safe answer and no
equivalent branch is warranted. This asymmetry is now recorded in both stub KDocs so a
later author does not "fix" the inconsistency.

## Scoping Constraints Honored

- **Saved Games stayed off the interface.** The Phase-13 scoping sentence
  (`Cloud save (Saved Games) is explicitly out of scope here — it arrives as a second
  data source behind ProgressRepository, not as a method on this seam`) survives verbatim.
- **No sign-out, no sign-in wall.** PGS v2 signs the player in automatically and exposes
  no app-facing sign-out; the interface documents this rather than inventing a v1-style
  gate.
- **No `Activity`, no SDK type.** Verified by grep as an acceptance criterion.

## Stale Claim Corrected

`GamesService.kt`'s KDoc claimed Slice 2 "swaps a single Hilt binding … and
leaderboards/achievements light up with no gameplay or UI change". That is false: the
stock UI intents resolve to an `Intent` that something must start, so the real
implementation also depends on the PLAT-01 foreground-Activity bridge. The KDoc now states
what the swap *does* guarantee — that the dependency stops at the seam, because the
interface names no Activity type and no Play Games SDK type, so no ViewModel can obtain
one through it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `data class SignedIn` reformatted to multiline**

- **Found during:** Task 2, at the `./gradlew check` gate
- **Issue:** `ktlintMainSourceSetCheck` failed with two `standard:class-signature`
  violations ("Newline expected after opening parenthesis" / "before closing
  parenthesis"). The `ktlint_official` code style set in `.editorconfig` forces a
  multiline class signature for *any* constructor parameter count, not just ≥2.
- **Fix:** Split `data class SignedIn(val playerId: String) : GamesSignInState` onto three
  lines with a trailing comma, matching the repo-wide convention (verified against
  `GameUiState.kt:65`).
- **Consequence:** This deliberately breaks the plan's Task 1 acceptance criterion
  `grep -q "data class SignedIn(val playerId: String)"`, which assumed a single-line form.
  ktlint is a mandatory CLAUDE.md build gate and is itself a Task 2 acceptance criterion
  (`./gradlew check` passes, including ktlintCheck), so the lint rule takes precedence.
  The behavioral criterion is unaffected — `SignedIn` still carries exactly one
  `playerId: String`.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt`
- **Commit:** `4c20556`

No other deviations. No authentication gates were encountered.

## TDD Gate Compliance

Both tasks are `tdd="true"`, and the plan itself notes the interface cannot compile until
the stub implements it. The cycle was run as three commits so each gate is bisectable:

| Gate | Commit | State |
|------|--------|-------|
| RED | `a42394d` `test(06-04)` | `:app:compileDebugUnitTestKotlin` **fails** — every new member unresolved |
| — | `e9e6b94` `feat(06-04)` (Task 1) | Interface correct; sole remaining error is `NoOpGamesService` not implementing it |
| GREEN | `4c20556` `feat(06-04)` (Task 2) | `./gradlew check` **green** |

RED was verified by an actual failing run, not assumed. No REFACTOR commit was needed.
Commits `a42394d` and `e9e6b94` do not compile by design — this is the unavoidable shape of
TDD when adding abstract members to an interface in a statically-typed language, and the
plan explicitly sanctions the sequencing ("the interface must be correct first").

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew check` (detekt, ktlint, no-dp/sp gate, ArchUnit purity, lint, all JVM tests) | **PASS** |
| `NoOpGamesServiceTest` | **9 tests, 0 failures** (was 2 → **7 new**, criterion was ≥5) |
| Total JVM tests | **316** = 242 app + 74 domain (309 baseline + 7) |
| `grep -q "Saved Games"` on `GamesService.kt` | **PASS** — Phase-13 scoping preserved |
| `! grep "android.app.Activity\|com.google.android.gms\|signOut"` on `GamesService.kt` | **PASS** |
| `grep -q "asStateFlow()"` on `NoOpGamesService.kt` | **PASS** — mutable flow not leaked |
| `grep -q "GamesSignInState.SignedOut"` on `NoOpGamesService.kt` | **PASS** |
| `! grep '\`true\`'` on `NoOpGamesServiceTest.kt` | **PASS** — stale Boolean contrast prose gone |
| File deletions in this plan's commits | **none** |

## Threat Model Coverage

| Threat ID | Disposition | How it is now enforced |
|-----------|-------------|------------------------|
| T-06-11 (Spoofing — `session`) | mitigated | Stub reports `SignedOut`. Three tests assert it: equals `SignedOut`, is **not** `Unknown`, and is **not** an instance of `SignedIn`. The last two are the ones that would catch a future author "helpfully" changing the initial value. |
| T-06-12 (EoP — surface shape) | mitigated | No Activity type and no Play Games SDK type on the interface, asserted by grep. Structural enforcement lands with `PlatformBoundaryTest` (plan 06-05). |
| T-06-13 (DoS — fire-and-forget) | mitigated | `unlockAchievement`/`incrementAchievement` are non-suspend and their KDoc carries `submitScore`'s exact contract: never blocks, never fails a solved level. |

## Player-Visible Change

**None**, as Phase 6 requires. `GamesService` had zero production call sites before this
plan and still has zero. The stub declines exactly as it did before, so every
Play-Games-dependent affordance stays hidden.

## Known Stubs

`NoOpGamesService` is a stub *by design* — it is this plan's deliverable, not deferred
work. It is documented in-file as the Slice-1 binding that Phase 11 replaces in
`PlatformModule`. No unintended stubs, placeholder text, or unwired data paths were
introduced.

## Notes for Phase 11

- `session` must transition `Unknown` → `SignedIn`/`SignedOut` once the first real check
  resolves. `Unknown` is the correct *initial* value for the real implementation, even
  though it is never correct for the stub.
- `showLeaderboard`/`showAchievements` are `suspend` specifically so the implementation can
  `awaitActivity()` with a timeout via `ForegroundActivityProvider` (plan 06-05) rather
  than threading an `ActivityResultLauncher` down from the Route.
- Both `show*` methods are contractually **silent** on failure. Do not add error surfacing
  without revisiting the KDoc — the player is deliberately never shown an error for a
  cosmetic surface they cannot reach.

## Self-Check: PASSED

All claimed files verified present on disk; all three commit hashes verified present in
`git log`. STATE.md and ROADMAP.md were **not** modified — the orchestrator owns those
writes after the wave completes.
