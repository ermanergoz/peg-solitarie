# Device pass brief — phase 13, plan 13-11 task 2 (UAT tests 10-13)

Date: 2026-09-20   Build: 323f76d (debug, `com.erman.pegsolitarie`, activity `com.app.pegsolitaire.MainActivity`)
Devices: `emulator-5554` = AVD Pixel_9a, `emulator-5556` = AVD Pixel_9. adb: `~/Library/Android/sdk/platform-tools/adb`.

## Task
Run `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests 10, 11, 12, 13 exactly as written and
report each as pass / fail / partial / blocked with evidence (screenshots, UI dumps, log lines).
A blocked test is an honest outcome; a test recorded as passing that was not observed is not.
Do not change app code, do not edit 11-HUMAN-UAT.md (the producer transcribes your report).

## Tools (in `.planning/reports/phase-13/tools/`)
- `ui.py <serial> dump | tap "<text>" | shot <name>` — uiautomator dump / tap by text or content-desc
  (substring match, first hit: the Play Games prompt's title also contains "Sign in", so tap its
  button by coordinates 540,1368 instead) / screenshot into the tools dir.
- `robot.py <serial> [--read-only] [--leave N]` — with a game screen open: reads the board from a
  screenshot, solves it to one peg by DFS, taps the moves. Verified on Challenge levels 1-3
  (dark theme, 7x7 cross). Unverified on the light theme (Pixel_9 is light) and on European-37 —
  check `--read-only` output against a screenshot first and fix the script if the read is wrong.
  `--leave N` stops N moves early (for the D-04 in-progress sub-check).

## State found by the producer (2026-09-20, observed, not assumed)
- Pixel_9a: was signed out; explicit sign-in from Settings succeeded (Settings showed the "Signed in as <gamer tag>" row).
  Challenge levels 1-3 solved by the robot this session (tile was "Continue — Level 1" before).
  English-33 already showed 3 of 3 stars in the Classic catalog before any Classic play this
  session — origin unknown (older local data or a cloud restore at sign-in); do not count it as
  evidence either way. App was force-stopped ~20 s after the last solve.
- Pixel_9: app data is fresh (tutorial + Play Games prompt shown). Sign-in FAILED: the AVD has no
  Play Games app (`com.google.android.play.games` absent; logcat: "PlayGamesServices[UiUtils]:
  Cannot find the installed destination app", then "SignInPerformer: ... unresolvable error ...
  timeout"). It has the Play Store and one Google account. Installing Google Play Games from the
  Play Store on this emulator is authorised; if the install or sign-in still cannot be made to
  work, tests 10, 11 and 13's reconvergence half are blocked — say so.
- Play Console precondition (13-OPERATOR-CHECKLIST.md Step 0: Saved Games enabled AND the PGS
  configuration published) has NOT been confirmed by anyone. It cannot be checked from a device
  except by outcome. If restores silently do nothing on a signed-in second device, report tests
  as blocked on the unverified toggle, not as app defects, and quote any GMS log lines that
  mention snapshots.
- The host was heavily loaded earlier (System UI ANR on Pixel_9a, fixed by a reboot). If a
  device shows a black screen with keyguard stuck, `adb -s <serial> reboot`.
- Interstitial test ads may appear between levels; close them and continue.

## Test 12 extras
Record the description line, played time and cover presence from Play Games app → this game →
Saved games (Pixel_9a has the Play Games app). For RESEARCH A1 also report the PNG-compressed
byte size of the committed 512x512 cover and `getMaxCoverImageSize()` if obtainable without code
changes; if not obtainable, say "not measured" and why.

## Report
Write `.planning/reports/phase-13/qa-engineer-device-pass.md` in the rules/reporting.md format,
one section per test with result, date, AVD, build and evidence paths.
