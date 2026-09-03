---
phase: 11
slug: play-games-session
status: planned
nyquist_compliant: true
wave_0_complete: true
created: 2026-09-03
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Robolectric + Compose UI Test (`ui-test-junit4`) + Turbine 1.2.1 + Google Truth 1.4.x + MockK 1.14.x (all already on the test graph) |
| **Config file** | `app/build.gradle.kts` (test dependencies; `Config(sdk = [ROBOLECTRIC_SDK])` pattern in `app/src/test`) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.platform.games.*"` |
| **Full suite command** | `./gradlew check --offline` |
| **Estimated runtime** | ~180 seconds (full suite); ~40 seconds (scoped quick run) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.platform.games.*"` plus the ViewModel/Screen test package the task touched
- **After every plan wave:** Run `./gradlew check --offline`
- **Before `/gsd:verify-work`:** Full suite must be green, plus `./gradlew detekt ktlintCheck` (both wired into `check`)
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 11-01-T2 | 11-01 | 1 | PGS-01 / PGS-03 | T-11-01 | Opt-in flags persist across process restarts; decline is permanent | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesOptInRepositoryTest"` | ✅ (created by plan) | ⬜ pending |
| 11-01-T1 | 11-01 | 1 | PGS-02 | T-11-02 | `SignedOut`/no-op stub leaves every existing surface unchanged — no crash, no dependent UI | unit | `./gradlew :app:testDebugUnitTest --tests "*.NoOpGamesServiceTest"` | ✅ (extend) | ⬜ pending |
| 11-01-T3 | 11-01 | 1 | PGS-01/02/03 | T-11-03 | Shared FakeGamesService compiles and can simulate success/decline/error `signIn()` outcomes | compile | `./gradlew :app:compileDebugUnitTestKotlin` | ✅ (created by plan) | ⬜ pending |
| 11-02-T2 | 11-02 | 2 | PGS-01 | T-11-04/T-11-06 | Opted-in player's bootstrap reaches the SDK; non-opted-in player's never touches it; session never stuck at Unknown | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesServiceTest"` | ✅ (created by plan) | ⬜ pending |
| 11-02-T3 | 11-02 | 2 | PLAT-07 | T-11-07 | `com.google.android.gms.games.*` referenced only inside `platform/games/`; real binding does not break the boundary gate | unit (boundary gate) | `./gradlew :app:testDebugUnitTest --tests "*.PlatformBoundaryGateTest"` | ✅ | ⬜ pending |
| 11-03-T1 | 11-03 | 2 | PGS-01 | T-11-09 | Home dialog shown exactly once, only after `onboardingVisible == false`, never after `play_games_prompt_answered == true` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend) | ⬜ pending |
| 11-03-T1 | 11-03 | 2 | PGS-01 | T-11-08 | Accept sets both flags and calls `signIn()`; decline sets both flags without calling `signIn()` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend) | ⬜ pending |
| 11-03-T2 | 11-03 | 2 | PGS-01 | T-11-10 | PlayGamesPromptDialog renders and forwards accept/decline exactly once each; outside-dismiss maps to decline | Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesPromptDialogTest"` | ✅ (created by plan) | ⬜ pending |
| 11-03-T3 | 11-03 | 2 | PGS-01 | T-11-10 | Home's snackbar shows only on a signIn() throw, keyed on playGamesSignInErrorTick | Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.HomeScreenTest"` | ✅ (extend) | ⬜ pending |
| 11-04-T1 | 11-04 | 3 | PGS-02/PGS-03 | T-11-13 | `gamesSignInState` mirrors `GamesService.session`; `onSignInToPlayGames()` sets D-19 flags and calls `signIn()` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` | ✅ (extend) | ⬜ pending |
| 11-04-T2 | 11-04 | 3 | PGS-02 | T-11-11 | Settings section absent while `Unknown`, sign-in row while `SignedOut`, "Signed in as X" (or D-14 fallback) while `SignedIn` | Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.SettingsPlayGamesTest"` | ✅ (created by plan) | ⬜ pending |
| 11-04-T2 | 11-04 | 3 | PGS-03 | T-11-12 | Tapping the Settings row calls `signIn()` and sets both D-19 flags; SDK error surfaces the D-10 snackbar, plain decline does not | Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.SettingsPlayGamesTest"` | ✅ (created by plan) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Populated by the planner (2026-09-03) once PLAN.md files existed — see 11-01..11-04-PLAN.md.*

---

## Wave 0 Requirements

Wave 0 gaps are closed inline (TDD-style, RED-then-GREEN within the producing task) rather than
as a separate pre-wave — each gap is listed with the plan/task that closes it:

- [x] `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt` — PGS-01 opted-in-gated bootstrap, `Unknown → SignedOut`/`SignedIn` transitions, D-14 null-displayName fallback — closed by **11-02-T2**
- [x] `app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt` — D-17 persistence contract — closed by **11-01-T2**
- [x] A test-only fake `GamesService` (distinct from `NoOpGamesService`) that emits all three `GamesSignInState` values and simulates success / silent-decline / SDK-error for `signIn()` — closed by **11-01-T3** (`FakeGamesService`)
- [x] Compose UI test for `PlayGamesPromptDialog` (outside-tap dismiss, back dismiss, both buttons) — closed by **11-03-T2**
- [x] Compose UI test for the Settings Play Games section's three-state rendering — closed by **11-04-T2** (`SettingsPlayGamesTest.kt`)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real silent sign-in on launch for an opted-in player, and the stock "Welcome back" popup | PGS-01 | Needs a Play Console PGS project, OAuth credentials for both SHA-1s, and a tester account — none exist yet (D-04) | Once the Console project exists: install a debug build on an emulator with Play Games, accept the Home prompt, sign in, kill and relaunch, confirm the popup appears with no dialog and no pause before a level can start |
| `signIn()` user-cancel vs SDK-error distinction (snackbar only on error) | PGS-03 | Google does not document the `Task<AuthenticationResult>` failure shape for user cancel (Research Open Question 2 / A2) | On the same emulator: tap the Settings row and dismiss the account chooser (expect silence), then sign in with an unregistered account (expect the snackbar) |
| Sign-in chooser and welcome popup do not trip the Phase 10 interstitial foreground cadence | INT (Phase 10) | Requires a live SDK Activity in-process | With interstitial logging on, trigger manual sign-in and confirm no spurious foreground/background transition is logged |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (every task across all four plans carries its own `<automated>` command)
- [x] Wave 0 covers all MISSING references (closed inline per-task, see Wave 0 Requirements above)
- [x] No watch-mode flags
- [x] Feedback latency < 180s (scoped `testDebugUnitTest --tests` commands; full `check --offline` only at wave/plan boundaries)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** plans 11-01..11-04 created 2026-09-03; ready for `/gsd:execute-phase 11`.
