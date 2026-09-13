---
status: partial
phase: 11-play-games-session
source: [11-VERIFICATION.md, 12-08-PLAN.md]
started: 2026-09-03T22:20:00Z
updated: 2026-09-13T19:48:41Z
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
expected: Dismissing the chooser is silent (no snackbar, row stays); an unregistered or erroring account shows the "Couldn't sign in to Play Games" snackbar. This confirms or corrects the cancel classification in `PlayGamesService.signIn()` marked `[UNVERIFIED — D-04]`: both `CommonStatusCodes.CANCELED` (16) and the sign-in-specific `SIGN_IN_CANCELLED` (12501, `GamesConstants.SIGN_IN_CANCELLED_STATUS_CODE`) are treated as a silent decline (WR-08). Note which of the two codes the dismissed chooser actually reports, so the set can be narrowed or widened from observation rather than guesswork.
result: [pending]

### 3. Play Games chooser and popup do not trip interstitial foreground cadence
With Phase 10 interstitial foreground-cadence logging on, trigger the manual Settings sign-in row (or the Home prompt's "Sign in" action) and watch for spurious foreground/background transitions.
expected: The Play Games account chooser and the "Welcome back" popup do not trip `AppForegroundTracker`'s interstitial cadence.
result: [pending]

## Phase 12 Tests — Leaderboards & Achievements

Every test below requires `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` to have been executed in full — 7 leaderboards created, 20 achievements created, all 20 icons uploaded, the Play Games Services settings published, and all 27 IDs recorded in an untracked `local.properties` — plus a device or emulator with Play Games and an account **on the tester list**. None of these six is expressible as a JVM test: five need a Google-rendered surface or a real Play Games backend, and the sixth needs a minified build.

### 4. Stock unlock popup appears on the first solve (PGSA-04 / D-16)
Signed in as a tester on a fresh install, solve any level in either mode.
expected: The stock Play Games unlock popup for **First Clear** slides in over the Results screen in its **default position**, with no app-owned toast, dialog or snackbar accompanying it, and it neither blocks nor delays anything on Results. Interpretation: the app calls `unlock()` and nothing else — Play Games v2 exposes **no popup API at all** (`PlayGames` has no `getGamesClient`, so `setGravityForPopups` / `setViewForPopups` are unreachable), so D-16 costs zero lines of code and this test is the only way to observe it. If no popup appears, check that the icon for **First Clear** was uploaded before investigating anything else — a missing icon is the documented cause of a silent no-render (12-RESEARCH A2).
result: [pending]

### 5. Both Settings rows open the stock Play Games UIs (PGSL-06 / PGSA-05 / D-14)
Signed in, open Settings and tap each of the two new rows in the Play Games section.
expected: **Leaderboards** opens the Play Games **all-leaderboards picker** — the list showing all seven boards, not a single board's view (`showLeaderboards()` is no-arg, D-14) — and **Achievements** opens the Play Games achievements UI showing all 20, with the earned ones unlocked and the six incremental ones displaying a progress bar. Back returns to Settings in both cases, with no crash and no duplicate Activity left on the stack.
result: [pending]

### 6. A real score lands on all three leaderboards after one solve (PGSL-01 / PGSL-02 / PGSL-03)
Signed in, solve one Classic board (English-33 is the simplest), then reopen the leaderboards picker; afterwards solve one Challenge level and reopen it again.
expected: The player's entry is present on the **English-33 Best Score** board carrying the score Results displayed; after the Challenge solve, entries appear on **Challenge Ladder** (the level number) and **Challenge Total Score** (the sum of Challenge bests). Diagnosis path per PITFALLS.md Pitfall 7: an empty board with no error is the signature of a tester-list or OAuth-client-origin problem, not a code bug — recheck 12-OPERATOR-CHECKLIST Step 0 items 3, 4 and 6 before opening a Kotlin file.
result: [pending]

### 7. Offline-then-sign-in backfill submits bests exactly once (PGSL-05 / D-10 / D-11)
On a signed-out install, solve two or three levels including at least one Classic board and one Challenge level. Then sign in from Settings, and afterwards relaunch the app with nothing new solved.
expected: Within a few seconds of the session resolving to signed in, the leaderboards show those bests and the achievements earned offline (at minimum **First Clear** and the relevant **Cleared** board unlock) appear unlocked — from a single backfill pass, with no duplicate popups and no repeated submissions on subsequent launches once the pending flag has cleared. The relaunch with nothing new solved must make no further submissions at all.
result: [pending]

### 8. Both Play Games rows are hidden while signed out (D-15 / PGS-02)
On a signed-out install, or before accepting the Home opt-in prompt, open Settings; then sign in without leaving the screen.
expected: The Play Games section shows only the sign-in row — neither **Leaderboards** nor **Achievements** is present, and neither is greyed out; they are absent. Signing in reveals both without needing an app restart. Nothing in the section is broken or erroring, matching PGS-02's hide-don't-break rule.
result: [pending]

### 9. Minified release build needs no new R8 keep rules (Pitfall 12)
Run `./gradlew :app:assembleRelease` with all 27 IDs configured (so `verifyReleaseGamesIds` passes) and with network available for at least one warm run (so `compose-group-mapping` resolves), then inspect `app/build/outputs/mapping/release/missing_rules.txt`, then install the minified release on a device and re-run test 5 against it.
expected: The build succeeds and `missing_rules.txt` contains **no entry naming `com.google.android.gms.games`**. The `play-services-games-v2:22.0.0` AAR ships no `proguard.txt` of its own and its SafeParcelable / `@Keep` / DynamiteApi reflection is covered only transitively by `play-services-basement`, so this phase's first `LeaderboardsClient`, `AchievementsClient` and `Intent`-returning calls are what would expose the gap. If a rule is needed, add it to `app/proguard-rules.pro` with a comment explaining why, and spell out `{ <init>(); }` for any reflectively-instantiated class — AGP 9.1.0 runs R8 in strict full mode. Test 5 must then still pass against the installed minified build.
result: [pending]

## Summary

total: 9
passed: 0
issues: 0
pending: 9
skipped: 0
blocked: 0

## Gaps
