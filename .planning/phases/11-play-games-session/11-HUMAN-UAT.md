---
status: partial
phase: 11-play-games-session
source: [11-VERIFICATION.md]
started: 2026-09-03T22:20:00Z
updated: 2026-09-03T22:20:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Real silent sign-in and "Welcome back" popup on relaunch (PGS-01)
Install a debug build on an emulator with Play Games and Google Play services, once a PGS Play Console project, OAuth credentials for both debug and release SHA-1s, and a tester account exist (D-04). Accept the Home "Sign in to Play Games?" prompt, confirm sign-in completes, kill the process and relaunch.
expected: The stock Play Games "Welcome back" popup appears at launch with no app-owned dialog, no wall and no pause before a level can start.
result: [pending]

### 2. Settings sign-in row: cancel is silent, genuine error shows the snackbar (PGS-03 / D-10)
On the same emulator, tap the Settings "Sign in to Play Games" row and dismiss the account chooser without picking an account; then tap it again and complete sign-in with an account not registered as a tester.
expected: Dismissing the chooser is silent (no snackbar, row stays); an unregistered or erroring account shows the "Couldn't sign in to Play Games" snackbar. This confirms or corrects the `CommonStatusCodes.CANCELED`-based classification in `PlayGamesService.signIn()` marked `[UNVERIFIED — D-04]`.
result: [pending]

### 3. Play Games chooser and popup do not trip interstitial foreground cadence
With Phase 10 interstitial foreground-cadence logging on, trigger the manual Settings sign-in row (or the Home prompt's "Sign in" action) and watch for spurious foreground/background transitions.
expected: The Play Games account chooser and the "Welcome back" popup do not trip `AppForegroundTracker`'s interstitial cadence.
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
