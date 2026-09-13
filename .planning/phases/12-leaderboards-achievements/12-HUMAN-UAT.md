---
status: partial
phase: 12-leaderboards-achievements
source: [12-VERIFICATION.md]
started: 2026-09-13T20:04:08Z
updated: 2026-09-13T21:51:27Z
---

## Current Test

[awaiting human testing]

## Tests

All six items require a live Play Console project configured per
`12-OPERATOR-CHECKLIST.md` (7 leaderboards, 20 achievements, 20 icons, 27 IDs
recorded in `local.properties`). They mirror tests 4–9 in
`.planning/phases/11-play-games-session/11-HUMAN-UAT.md`; a pass recorded in
either file counts for both.

### 1. Stock unlock popup appears on the first solve (PGSA-04 / D-16)
expected: Signed in as a tester on a fresh install, solving any level in either mode slides the stock Play Games unlock popup for "First Clear" over the Results screen in its default position, with no app-owned toast, dialog or snackbar, and without blocking or delaying Results.
result: [pending]

### 2. Both Settings rows open the stock Play Games UIs (PGSL-06 / PGSA-05 / D-14)
expected: Signed in, tapping "Leaderboards" in Settings opens the stock all-leaderboards picker listing all seven boards; tapping "Achievements" opens the stock achievements UI listing all 20, with earned ones unlocked and incremental ones showing progress bars. Back returns to Settings cleanly.
result: [pending]

### 3. A real score lands on all three leaderboards after one solve (PGSL-01 / PGSL-02 / PGSL-03)
expected: After solving one Classic board, the English-33 leaderboard shows the submitted score; after solving one Challenge level, Challenge Ladder and Challenge Total Score both show entries.
result: [pending]

### 4. Offline-then-sign-in backfill submits bests exactly once (PGSL-05 / D-10 / D-11)
expected: On a signed-out install, solve 2–3 levels including one Classic and one Challenge, then sign in from Settings. Within seconds the offline bests and achievements appear from a single backfill pass and the pending flag clears; a subsequent clean relaunch with nothing new solved makes no further submissions.
result: [pending]

### 5. Both Play Games rows are hidden while signed out (D-15 / PGS-02)
expected: On a signed-out install (or before accepting the Home opt-in prompt), Settings shows only the sign-in row; neither Leaderboards nor Achievements is present or greyed out. Signing in without leaving the screen reveals both with no restart.
result: [pending]

### 6. Minified release build needs no new R8 keep rules (Pitfall 12)
expected: `./gradlew :app:assembleRelease` with all 27 IDs configured succeeds; `missing_rules.txt` names no `com.google.android.gms.games` class; installing the minified build and re-running test 2 still passes.
result: [pending]

### 7. A perfect Diamond-41 finish is recorded and unlocks CLEARED_DIAMOND41 (CR-01 fix, 12-REVIEW-FIX.md)
expected: On a debug build (no Play Console needed for the first three checks), finish Diamond-41 with two pegs remaining. The Results screen shows PERFECT, the Classic catalog shows star pips and "New best!" for Diamond-41, and a `level_result` row plus a `play_session` row exist for `diamond41`. With a Play Console project, `CLEARED_DIAMOND41` unlocks and the Diamond-41 leaderboard receives the score.
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps
