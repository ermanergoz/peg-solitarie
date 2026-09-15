---
status: partial
phase: 11-play-games-session
source: [11-VERIFICATION.md, 12-08-PLAN.md]
started: 2026-09-03T22:20:00Z
updated: 2026-09-15T04:00:00Z
---

## Current Test

Phase 12 tests 4–9 all pass. Phase 11 tests 1–3 executed 2026-09-15: test 1 now PASSES after quick task 260915-51n, test 2 remains blocked, test 3 remains partial. Historical note, kept because it explains the shape of the fix: the first run of test 1 saw a cold relaunch after a successful explicit sign-in resolve to signed-out on BOTH debug and minified builds. The cause was not Play Services brokering an unauthenticated session — it was `PlayGamesSdk.initialize` running too late (from `MainActivity.onCreate`) for the SDK's automatic authenticator to ever fire; see test 1's result line.

Session note (2026-09-15): the first debug run could not sign in at all — root cause was the debug build shipping Play Games project ID `"0"` (AdMob resolver hard-wire, DEF-06-01), fixed as quick task 260915-3jd before tests 4–8 were run. Design observation from test 4: the first English-33 solve (3 stars, 28 hints, 0 undos, centre finish) legitimately unlocked five achievements at once (First Clear, Three Stars, No Take-Backs, Dead Center, English Cleared). User decision 2026-09-15: keep the rules as designed (D-01/D-02); no change.

## Tests

### 1. Real silent sign-in and "Welcome back" popup on relaunch (PGS-01)
Install a debug build on an emulator with Play Games and Google Play services, once a PGS Play Console project, OAuth credentials for both debug and release SHA-1s, and a tester account exist (D-04). Accept the Home "Sign in to Play Games?" prompt, confirm sign-in completes, kill the process and relaunch.
expected: The stock Play Games "Welcome back" popup appears at launch with no app-owned dialog, no wall and no pause before a level can start.
result: PASS (2026-09-15, emulator-5554 Pixel_9a AVD / Android 16, debug build from `bugfix/phase-11-silent-sign-in-on-relaunch` 5ddd120, tester account already opted in). Kill + relaunch with no taps at all: `am force-stop` → `logcat -c` → launcher intent, and within ~2 s Play Services logged `GamesConnectSignInOp: signIn(%s): Returning SUCCESS`, then `SignInPerformer: Found existing game run` and `Connection from [%s], returning true for shouldShowWelcomePopup` — the silent sign-in and the stock welcome popup, neither of which the earlier build produced. Opening Settings (the only tap in the run) showed the Play Games section reading `Signed in as …` with the tester's gamertag, plus the Leaderboards and Achievements rows. Shipped fix (quick task 260915-51n): `PlayGamesSdk.initialize` moved to `App.onCreate` via the new `GamesService.initializeIfOptedIn()` seam, still gated on the persisted `play_games_opted_in` flag and with `PlayGamesInitProvider` still removed — so the SDK's `AutomaticGamesAuthenticator` is registered before the first Activity is created and its one automatic attempt fires on `MainActivity`'s `onActivityCreated`, while a player who never opted in is still never reached by the SDK (D-07/D-17). Earlier root-cause evidence, retained: from the v2 SDK bytecode (`zzbw.zza` / `zzar` / `zzbq`), `initialize` starts an `AutomaticGamesAuthenticator` whose ONLY trigger is `Application.ActivityLifecycleCallbacks.onActivityCreated` of an Activity created AFTER initialize, and `isAuthenticated()` merely returns that authenticator's cached state — it never contacts Play Services itself. With the old code initializing from a coroutine dispatched out of `MainActivity.onCreate`, i.e. after the only Activity already existed, the automatic attempt never ran on a cold start and the silent check truthfully answered not-authenticated; relaunching `MainActivity` with CLEAR_TASK against a live process (a second `onActivityCreated`) made the SDK sign in silently, which is what identified the trigger.

### 2. Settings sign-in row: cancel is silent, genuine error shows the snackbar (PGS-03 / D-10)
On the same emulator, tap the Settings "Sign in to Play Games" row and dismiss the account chooser without picking an account; then tap it again and complete sign-in with an account not registered as a tester.
expected: Dismissing the chooser is silent (no snackbar, row stays); an unregistered or erroring account shows the "Couldn't sign in to Play Games" snackbar. This confirms or corrects the cancel classification in `PlayGamesService.signIn()` marked `[UNVERIFIED — D-04]`: both `CommonStatusCodes.CANCELED` (16) and the sign-in-specific `SIGN_IN_CANCELLED` (12501, `GamesConstants.SIGN_IN_CANCELLED_STATUS_CODE`) are treated as a silent decline (WR-08). Note which of the two codes the dismissed chooser actually reports, so the set can be narrowed or widened from observation rather than guesswork.
result: partial / blocked (2026-09-15). The cancel path cannot be produced on this emulator: the single account is already authorised for the game, so `signIn()` never shows a chooser (`Found existing game run` → immediate success). The error path cannot be produced either: the Play Games project is published to everyone, so no account is 'unregistered'. `CANCELED` vs `SIGN_IN_CANCELLED` remains `[UNVERIFIED — D-04]`. Needs a second Google account on the device that has never authorised the game, or revoking the game's access in the Play Games app, to observe the chooser dismissal code.

### 3. Play Games chooser and popup do not trip interstitial foreground cadence
With Phase 10 interstitial foreground-cadence logging on, trigger the manual Settings sign-in row (or the Home prompt's "Sign in" action) and watch for spurious foreground/background transitions.
expected: The Play Games account chooser and the "Welcome back" popup do not trip `AppForegroundTracker`'s interstitial cadence.
result: pass by observation for the popup path (2026-09-15): the welcome/unlock popups are views attached to the app's own window (`PopupQueueManagerV2`), no Activity switch occurs, and no foreground/background transition was seen. The chooser path could not be exercised (see test 2). Note: the stock Leaderboards/Achievements screens are a GMS Activity (`InGameUiActivity`) and DO stop `MainActivity` — that is a real app-to-app transition, expected, and outside this test's scope.

## Phase 12 Tests — Leaderboards & Achievements

Every test below requires `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` to have been executed in full — 7 leaderboards created, 20 achievements created, all 20 icons uploaded, the Play Games Services settings published, and all 27 IDs recorded in an untracked `local.properties` — plus a device or emulator with Play Games and an account **on the tester list**. None of these six is expressible as a JVM test: five need a Google-rendered surface or a real Play Games backend, and the sixth needs a minified build.

### 4. Stock unlock popup appears on the first solve (PGSA-04 / D-16)
Signed in as a tester on a fresh install, solve any level in either mode.
expected: The stock Play Games unlock popup for **First Clear** slides in over the Results screen in its **default position**, with no app-owned toast, dialog or snackbar accompanying it, and it neither blocks nor delays anything on Results. Interpretation: the app calls `unlock()` and nothing else — Play Games v2 exposes **no popup API at all** (`PlayGames` has no `getGamesClient`, so `setGravityForPopups` / `setViewForPopups` are unreachable), so D-16 costs zero lines of code and this test is the only way to observe it. If no popup appears, check that the icon for **First Clear** was uploaded before investigating anything else — a missing icon is the documented cause of a silent no-render (12-RESEARCH A2).
result: pass (2026-09-15, emulator sdk_gphone16k_arm64 / Android 16, tester account, debug build f734f44+). The stock First Clear popup slid in over Results with no app-owned toast/dialog/snackbar. Observation for the record: one Play Services log line during the session read `The given popup location does not have enough height; skipping showing popup` followed by `clearing all requested popups` — the tester reported every expected surface, so noted, not raised as an issue.

### 5. Both Settings rows open the stock Play Games UIs (PGSL-06 / PGSA-05 / D-14)
Signed in, open Settings and tap each of the two new rows in the Play Games section.
expected: **Leaderboards** opens the Play Games **all-leaderboards picker** — the list showing all seven boards, not a single board's view (`showLeaderboards()` is no-arg, D-14) — and **Achievements** opens the Play Games achievements UI showing all 20, with the earned ones unlocked and the six incremental ones displaying a progress bar. Back returns to Settings in both cases, with no crash and no duplicate Activity left on the stack.
result: pass (2026-09-15). Leaderboards row opened the all-leaderboards picker listing all seven boards; Achievements row opened the achievements UI with all 20, earned ones unlocked and the six incremental ones showing progress. Back returned to Settings both times, no crash, no duplicate Activity.

### 6. A real score lands on all three leaderboards after one solve (PGSL-01 / PGSL-02 / PGSL-03)
Signed in, solve one Classic board (English-33 is the simplest), then reopen the leaderboards picker; afterwards solve one Challenge level and reopen it again.
expected: The player's entry is present on the **English-33 Best Score** board carrying the score Results displayed; after the Challenge solve, entries appear on **Challenge Ladder** (the level number) and **Challenge Total Score** (the sum of Challenge bests). Diagnosis path per PITFALLS.md Pitfall 7: an empty board with no error is the signature of a tester-list or OAuth-client-origin problem, not a code bug — recheck 12-OPERATOR-CHECKLIST Step 0 items 3, 4 and 6 before opening a Kotlin file.
result: pass (2026-09-15). Entry present on English-33 Best Score with the score Results displayed (6650); Challenge Ladder and Challenge Total Score populated after the Challenge solve.

### 7. Offline-then-sign-in backfill submits bests exactly once (PGSL-05 / D-10 / D-11)
On a signed-out install, solve two or three levels including at least one Classic board and one Challenge level. Then sign in from Settings, and afterwards relaunch the app with nothing new solved.
expected: Within a few seconds of the session resolving to signed in, the leaderboards show those bests and the achievements earned offline (at minimum **First Clear** and the relevant **Cleared** board unlock) appear unlocked — from a single backfill pass, with no duplicate popups and no repeated submissions on subsequent launches once the pending flag has cleared. The relaunch with nothing new solved must make no further submissions at all.
result: pass (2026-09-15). Offline solves backfilled once on sign-in (First Clear + English Cleared unlocked, bests posted); relaunch with nothing new solved made no further submissions and no duplicate popups.

### 8. Both Play Games rows are hidden while signed out (D-15 / PGS-02)
On a signed-out install, or before accepting the Home opt-in prompt, open Settings; then sign in without leaving the screen.
expected: The Play Games section shows only the sign-in row — neither **Leaderboards** nor **Achievements** is present, and neither is greyed out; they are absent. Signing in reveals both without needing an app restart. Nothing in the section is broken or erroring, matching PGS-02's hide-don't-break rule.
result: pass (2026-09-15). Signed out, the Play Games section showed only the sign-in row; both rows appeared on sign-in without a restart.

### 9. Minified release build needs no new R8 keep rules (Pitfall 12)
Run `./gradlew :app:assembleRelease` with all 27 IDs configured (so `verifyReleaseGamesIds` passes) and with network available for at least one warm run (so `compose-group-mapping` resolves), then inspect `app/build/outputs/mapping/release/missing_rules.txt`, then install the minified release on a device and re-run test 5 against it.
expected: The build succeeds and `missing_rules.txt` contains **no entry naming `com.google.android.gms.games`**. The `play-services-games-v2:22.0.0` AAR ships no `proguard.txt` of its own and its SafeParcelable / `@Keep` / DynamiteApi reflection is covered only transitively by `play-services-basement`, so this phase's first `LeaderboardsClient`, `AchievementsClient` and `Intent`-returning calls are what would expose the gap. If a rule is needed, add it to `app/proguard-rules.pro` with a comment explaining why, and spell out `{ <init>(); }` for any reflectively-instantiated class — AGP 9.1.0 runs R8 in strict full mode. Test 5 must then still pass against the installed minified build.
result: pass (2026-09-15). Build half: `:app:assembleRelease` with all 27 IDs configured succeeded (`verifyReleaseGamesIds` green), R8 wrote no `missing_rules.txt` at all, and the Play Games classes appear in `mapping.txt` (58 entries). Install half, driven over adb on the emulator: the minified APK (release build with Google's test ad units, signed with the debug key so the debug credential applies) installed at 3.7 MB, explicit sign-in from Settings succeeded (`Signed in as …`, both rows appeared), the Leaderboards row opened the all-leaderboards picker listing all seven boards, the Achievements row opened the achievements UI at 5/20 with the earned ones unlocked, Back returned to Settings both times, and the app task held exactly one Activity. No new keep rule needed; `app/proguard-rules.pro` unchanged.

## Summary

total: 9
passed: 7
issues: 0
pending: 0
blocked: 1
partial: 1
skipped: 0

## Gaps
