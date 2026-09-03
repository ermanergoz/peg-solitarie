---
phase: 11
slug: play-games-session
status: draft
nyquist_compliant: false
wave_0_complete: false
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
| TBD | TBD | 0 | PGS-01 | — | Opted-in player's bootstrap reaches the SDK; non-opted-in player's never touches it | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesServiceTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | PGS-01 / PGS-03 | — | Opt-in flags persist across process restarts; decline is permanent | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesOptInRepositoryTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | PGS-01 | — | Home dialog shown exactly once, only after `onboardingVisible == false`, never after `play_games_prompt_answered == true` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend) | ⬜ pending |
| TBD | TBD | 1 | PGS-01 | — | Accept sets both flags and calls `signIn()`; decline sets both flags without calling `signIn()` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend) | ⬜ pending |
| TBD | TBD | 1 | PGS-02 | — | `SignedOut` leaves every existing surface unchanged — no crash, no dependent UI | unit | `./gradlew :app:testDebugUnitTest --tests "*.NoOpGamesServiceTest"` | ✅ | ⬜ pending |
| TBD | TBD | 1 | PGS-02 | — | Settings section absent while `Unknown`, sign-in row while `SignedOut`, "Signed in as X" while `SignedIn` | unit (ViewModel) + Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` and `--tests "*.SettingsScreenTest"` | ✅ VM (extend) / ❌ W0 UI | ⬜ pending |
| TBD | TBD | 1 | PGS-03 | — | Tapping the Settings row calls `signIn()` and sets both D-19 flags | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` | ✅ (extend) | ⬜ pending |
| TBD | TBD | 1 | PGS-03 | — | SDK-reported `signIn()` error surfaces the D-10 snackbar; plain decline does not | unit (ViewModel) + Compose UI | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` | ❌ W0 (fake `GamesService`) | ⬜ pending |
| TBD | TBD | 1 | PLAT-07 | — | `com.google.android.gms.games.*` referenced only inside `platform/games/` | unit (boundary gate) | `./gradlew :app:testDebugUnitTest --tests "*.PlatformBoundaryGateTest"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Task IDs are filled by the planner once PLAN.md files exist.*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt` — PGS-01 opted-in-gated bootstrap, `Unknown → SignedOut`/`SignedIn` transitions, D-14 null-displayName fallback (fake `Activity` + `ForegroundActivityProvider`)
- [ ] `app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt` — D-17 persistence contract, modeled on the existing onboarding-flag repository test
- [ ] A test-only fake `GamesService` (distinct from `NoOpGamesService`) that emits all three `GamesSignInState` values and simulates success / silent-decline / SDK-error for `signIn()`
- [ ] Compose UI test for `PlayGamesPromptDialog` (outside-tap dismiss, back dismiss, both buttons) and the Settings Play Games section's three-state rendering — plain `createComposeRule()` semantics tests in the style of `GameScreenAdOfferTest.kt`; no Roborazzi setup

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real silent sign-in on launch for an opted-in player, and the stock "Welcome back" popup | PGS-01 | Needs a Play Console PGS project, OAuth credentials for both SHA-1s, and a tester account — none exist yet (D-04) | Once the Console project exists: install a debug build on an emulator with Play Games, accept the Home prompt, sign in, kill and relaunch, confirm the popup appears with no dialog and no pause before a level can start |
| `signIn()` user-cancel vs SDK-error distinction (snackbar only on error) | PGS-03 | Google does not document the `Task<AuthenticationResult>` failure shape for user cancel (Research Open Question 2 / A2) | On the same emulator: tap the Settings row and dismiss the account chooser (expect silence), then sign in with an unregistered account (expect the snackbar) |
| Sign-in chooser and welcome popup do not trip the Phase 10 interstitial foreground cadence | INT (Phase 10) | Requires a live SDK Activity in-process | With interstitial logging on, trigger manual sign-in and confirm no spurious foreground/background transition is logged |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
