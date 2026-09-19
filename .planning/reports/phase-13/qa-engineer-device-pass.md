# qa-engineer report — phase 13 device pass (plan 13-11 task 2, UAT tests 10-13)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (d2544c1..d2544c1 — nothing committed by QA, on the coordinator's instruction; report, evidence and tool changes are left uncommitted in the working tree)
Skills loaded: ergoz-soft:standards (role qa-engineer; rules reporting.md, testing.md, lessons.md from `.claude/rules/`, VERSION 0.1.0 = plugin 0.1.0). android-emulator-skill scripts were not used: the brief supplied `tools/ui.py` and `tools/robot.py` and told me to use those.
Ran in parallel with: none
Loops: 0
Lessons applied: none (lessons.md lists 0 active lessons; no `.planning/recall/RECALL.md` exists)

## Summary
Cloud save did nothing observable on either emulator: a wiped, signed-in Pixel_9 restored nothing, and no Saved games entry exists in the Play Games app. Tests 10, 11, 12 are **blocked**, test 13 is **partial** (offline half passes, reconvergence half blocked).
The app's own bookkeeping isolates the failure to the Saved Games call: on both devices, online and signed in, `play_games_submissions_pending` drains to `False` (leaderboards/achievements reach Google) while `play_games_cloud_push_pending` stays `True` (`CloudSaveStore.save` returned false). That is the presentation 13-OPERATOR-CHECKLIST Step 0 predicts for an un-enabled/unpublished Saved Games toggle. No log line proves it: GMS logs nothing about snapshots even at VERBOSE and the app logs nothing by design, so an app-side defect in the save path is not excluded.
Two things went wrong in the session and are reported below: both emulators were killed by the emulator hang detector under host load ~80, and my restart of Pixel_9a loaded a stale quickboot snapshot that rolled its disk back to 2026-09-15 (older APK, producer's session state lost). I reinstalled the current APK on Pixel_9a and redid the solves.

## Build under test
`com.erman.pegsolitarie` 3.0.0 (versionCode 3), debug. The APK on Pixel_9 (installed 2026-09-20 00:52) is byte-identical (sha256 `7dfae8ec…53522c`) to `app/build/outputs/apk/debug/app-debug.apk`, built 2026-09-16 00:40 at 2e1ca2c. `git log --since="2026-09-16 00:40" -- app/ gradle/ build.gradle.kts` is empty, so it is source-equivalent to 323f76d. Pixel_9a ran an older 2026-09-15 APK between 01:53 and 02:07 (see Open issues); the same `app-debug.apk` was installed on it at 02:07:43 with `adb install -r`, and only observations after that time are used for verdicts on Pixel_9a.

## Test 10 — Restore on a new device (CLOUD-01)
result: **blocked** — restore not observed; Saved Games writes are rejected; Play Console toggle unverified
date: 2026-09-20 · AVDs: Pixel_9a (emulator-5554), Pixel_9 (emulator-5556) · build: as above
observed:
- Pixel_9 precondition: Google Play Games installed from the Play Store (Play Pass upsell dismissed with "Not now"; install took ~12 min under host load). After that the app signed in silently on relaunch: Settings "Signed in as <gamer tag>".
- Pixel_9a, signed in: Challenge 1-3 and English-33 solved by the robot (old APK, 01:59-02:01), then Challenge 4 solved again on the current APK at 02:09. Home read "Continue — Level 5", English-33 3 of 3 stars. App force-stopped.
- Pixel_9: `pm clear`, launch, UMP consent declined, tutorial dismissed, "Sign in to Play Games?" → Sign in (02:03:27). GMS log: `signIn(): Returning SUCCESS`, `Successfully brokered GamesService`. Settings: signed in as the same gamer tag; DataStore recorded `play_games_merged_player_id` = the same per-app player id as Pixel_9a, so the app's pull pass ran.
- 6 s, 20 s, 90 s after sign-in and again after a force-stop + relaunch: Home still "Continue — Level 1", Classic catalog 0 of 3 stars on all five boards. Nothing restored. (The first attempt at 01:30, before the wipe, showed the same.)
- D-03 negative checks: no toast, dialog, snackbar or Settings sync row appeared at any point; no "New best!"/Results shown on launch. I did not catch the stock "Welcome back" popup in the 6 s screenshot (GMS logged `returning true for shouldShowWelcomePopup`), so its timing relative to a restore could not be assessed.
- Discriminating evidence: after a signed-in, online solve on Pixel_9a (current APK) `play_games_cloud_push_pending = True` while `play_games_submissions_pending = False`. Per `ProgressSubmitter.pushIfNeeded` the flag is written as `!committed`, so `CloudSaveStore.save` returned false with network, auth and the PGS session demonstrably working (achievement progress in the Play Games app moved 4% → 6%). With no snapshot ever committed there is nothing for Pixel_9 to restore.
- Classification: **blocked on the unverified Play Console toggle**, not filed as an app defect. Caveat: `PlayGamesCloudSaveStore` folds every failure (no foreground activity, open failure, size cap, commit failure) into the same `false`, and GMS printed no snapshot line at `log.tag.PlayGamesServices=VERBOSE`, so the logs cannot rule out a defect in the save path. Confirming Step 0 and re-running is the cheapest way to separate the two.
evidence: `evidence/00…02-p9-playgames-install*-s.png`, `11-p9-settings-signed-in-s.png`, `41…44-p9a-*-result-s.png`, `47-p9a-L4-result-newbuild-s.png`, `50…56-p9-*-s.png`, `58-p9-home-after-relaunch-s.png`, `57-p9-logcat-fresh-signin.filtered.txt`, `48-p9a-logcat-L4-solve-newbuild.filtered.txt`, `90-final-datastore-flags.txt`

## Test 11 — Two-device union (CLOUD-02, CLOUD-04 / D-04)
result: **blocked** — not run
date: 2026-09-20 · AVDs: Pixel_9a, Pixel_9 · build: as above
observed: The union needs a snapshot that commits; test 10 shows none does (`play_games_cloud_push_pending = True` on both devices after online relaunches). Final local states are disjoint and unmerged: Pixel_9a Challenge level 5 + English-33 3 stars; Pixel_9 Challenge level 4, no Classic stars — after relaunching both, neither moved toward the other. I did not solve European-37 or levels 6-8, and did not stage the D-04 in-progress game: without a merge there is nothing to assert, and solving more would only have produced local-only results that look like progress. `robot.py` remains unverified on European-37.
evidence: `evidence/69-p9-online-relaunch-home-s.png`, `70-p9a-relaunch-home-s.png`, `90-final-datastore-flags.txt`

## Test 12 — Legible listing entry (CLOUD-03)
result: **blocked** — no Saved games entry exists
date: 2026-09-20 · AVD: Pixel_9a · build: as above
observed: Play Games app → Library → Peg Solitaire shows Play, "7 / 20" Achievements, Leaderboards and an achievement recommendation; scrolling finds nothing else, and the overflow menu has only Help & feedback / Hide game / Settings / Uninstall. There is no Saved games section or entry, so description line, played time and cover could not be read. Consistent with no snapshot ever having been committed (test 10).
RESEARCH A1: `getMaxCoverImageSize()` — **not measured**: it is a `SnapshotsClient` call, the app neither logs nor displays it, and obtaining it needs a code change or a debugger, both outside this task. PNG size of the cover actually committed — **not measured on device**: no commit succeeded. Closest available number: `CoverImageSourceTest` (Robolectric NATIVE graphics, 5 tests, 0 failures today) asserts the 512x512 cover's PNG size under its ceiling; the KDoc in `CoverImageSource.kt` records that measurement as 35,907 bytes. I did not re-derive that figure myself.
evidence: `evidence/30-p9a-playgames-game-detail-s.png`, `80…82-p9a-playgames-*-s.png`

## Test 13 — Network kill degrades silently (CLOUD-05)
result: **partial** — offline half PASS; drain/reconvergence half blocked
date: 2026-09-20 · AVD: Pixel_9 (emulator-5556), re-check on Pixel_9a · build: as above
observed:
- `svc wifi disable; svc data disable` → `Active default network: none`, ping "Network is unreachable".
- Solved Challenge 1 and 2 offline: Results showed "Perfect!", "New best!", score 5280, time, pegs left, Next level / Home — identical to the online Results on Pixel_9a. Force-stop + relaunch offline: Home "Continue — Level 3". Solved level 3: Home "Continue — Level 4"; Classic catalog normal. No error wall, snackbar, dialog or retry prompt on any screen; no FATAL in logcat. Settings offline still reads "Signed in as <gamer tag>" with the standing "Leaderboards, achievements and cloud save" row — that row is permanent Settings copy, not a failure hint.
- Offline the app queued both halves: `play_games_submissions_pending = True`, `play_games_cloud_push_pending = True`.
- Network restored (ping to www.googleapis.com OK), relaunch, 45 s: `play_games_submissions_pending = False` (leaderboard/achievement backlog drained) but `play_games_cloud_push_pending = True` (cloud push still refused). Pixel_9a relaunched afterwards: still its own level 5, no pickup of anything from Pixel_9. Reconvergence not observed — same blocker as test 10.
evidence: `evidence/60…68-p9-offline-*-s.png`, `69-p9-online-relaunch-home-s.png`, `70-p9a-relaunch-home-s.png`, `90-final-datastore-flags.txt`

## Outputs
- `.planning/reports/phase-13/qa-engineer-device-pass.md` — this report
- `.planning/reports/phase-13/evidence/` — 41 downsized screenshots (900 px), 7 filtered logcat excerpts, 1 DataStore flag dump (3.1 MB). Raw logcats were deleted after filtering: they carried player ids, the PGS project number and full ad-request URLs with consent strings. Filtered files keep only sign-in / Games / auth-token lines with ids masked.
- `.planning/reports/phase-13/tools/ui.py` — fixed: `dump` silently returned the previous screen when `uiautomator dump` failed (stale `/sdcard/u.xml`; it handed me Pixel_9a's Classic catalog while the Play Games app was in front). Now deletes first, retries 4x, exits 3 on failure. `shot` now writes to `evidence/`.
- `.planning/reports/phase-13/tools/prefs.py` — new: reads `play_games_*` / `challenge_*` entries from the app's DataStore via `run-as` (debug builds). This is the only device-side signal that separates "save refused" from "save never attempted".
- `.planning/reports/phase-13/tools/levels.sh` — new: "Next level" + robot loop, stops on any unrecognised screen.
- `robot.py` unchanged; its read was checked against a screenshot on Pixel_9 light theme (7x7 cross) and is correct there.

## Checks
- `adb -s emulator-5556 shell pm list packages | grep play.games` — pass (`com.google.android.play.games` present after Play Store install)
- `python3 tools/prefs.py emulator-5554` after online signed-in solve, current APK — fail for cloud (`play_games_cloud_push_pending = True`; `play_games_submissions_pending = False`)
- `python3 tools/prefs.py emulator-5556` after network restore + relaunch — fail for cloud (same pair of values)
- `python3 tools/ui.py emulator-5556 dump` on Home / Classic after wipe + sign-in — fail for restore ("Continue — Level 1", five boards at 0 of 3 stars)
- `adb -s emulator-5556 shell "svc wifi disable; svc data disable"` + 3 offline solves + offline relaunch — pass (no error UI, progress recorded locally, no FATAL)
- `shasum -a 256` of Pixel_9's pulled `base.apk` vs `app/build/outputs/apk/debug/app-debug.apk` — pass (identical)
- `./gradlew :app:testDebugUnitTest --tests "*CoverImageSourceTest*" --offline -q` — pass (5 tests, 0 failures, 0 errors)
- `./gradlew check` — not run: no app code changed in this task and the host was already starving the emulators
- `log_monitor.py`, emulator shutdown — not run: brief's tools used instead; both emulators left running for the re-run

## Open issues / risks
- **Founder action needed — Play Console Step 0.** Confirm Saved Games is enabled on the PGS project **and** the PGS configuration is published (13-OPERATOR-CHECKLIST Step 0). Until then tests 10, 11, 12 and half of 13 cannot produce a verdict. If Step 0 turns out to be already done and published, the `cloud_push_pending = True` evidence becomes an app defect in `PlayGamesCloudSaveStore.save` and should go to the developer with this report.
- **Observability gap (for tech-lead).** The save path returns one undifferentiated `false` and logs nothing by policy; GMS logs nothing either. A device pass cannot tell toggle-off from a code bug. A debug-only, non-PII reason code would close this; I made no code change.
- **Pixel_9a was rolled back by my restart.** Both qemu processes were killed by the emulator's hang detector (crash reports 01:41:18 and 01:46:45, host load average ~80 from Firefox and Android Studio). I restarted Pixel_9a without `-no-snapshot-load`; it loaded the `default_boot` quickboot snapshot and the guest disk came back as of 2026-09-15: older APK, producer's sign-in/levels 1-3 session gone, English-33 3 stars present (so that star rating is old local data, answering the brief's "origin unknown"). Pixel_9 was restarted with `-no-snapshot-load` and kept its state. Pixel_9a now runs with `-no-snapshot-load -no-snapshot-save -dns-server 8.8.8.8` (the first restart also had no working DNS). Solves done on Pixel_9a between 01:59 and 02:01 ran on the old APK and are not used as cloud evidence.
- **Device state I changed:** Play Games installed on Pixel_9; app data wiped on Pixel_9; UMP consent answered "Do not consent" on Pixel_9; current debug APK reinstalled on Pixel_9a; `log.tag.PlayGamesServices`/`SnapshotAgent` set to VERBOSE on both (resets on reboot); wifi/data on Pixel_9 disabled then re-enabled.
- **Captures to review before committing (gamer tag visible):** `evidence/11-p9-settings-signed-in-s.png`, `21-p9a-settings-after-restart-s.png`, `54-p9-settings-after-signin-s.png`, `65-p9-offline-settings-s.png`. No capture contains the Google account email that I could find: Play Store screenshots `00…02` show only the avatar initial; Play Games captures `30`, `80…82` show the public developer name. Filtered logs have player ids and the project number masked.
- Pixel_9 also carries an unrelated package `com.erman.pegsolitaire` (different spelling); not touched.
- The stock "Welcome back" popup was not captured; whoever re-runs test 10 should screenshot within ~2 s of sign-in.

## Handoff
Next: producer — transcribe results into 11-HUMAN-UAT.md (10 blocked, 11 blocked, 12 blocked, 13 partial), review the four gamer-tag screenshots, commit, and raise the Step 0 confirmation with the founder. Then tech-lead — decide whether the save-path observability gap needs a task before the re-run. QA re-runs tests 10-13 once Step 0 is confirmed.
