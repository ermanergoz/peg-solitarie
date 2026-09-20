# qa-engineer report — phase 13 cloud save refusal reason (diagnostic device run)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (e5a5ec1..e5a5ec1 — nothing committed by QA, on the coordinator's instruction; this report and four evidence files are left uncommitted)
Skills loaded: ergoz-soft:standards (role qa-engineer). Rules read from the plugin copy 0.1.1 because the repo snapshot is 0.1.0: studio.md, reporting.md, testing.md, logging.md, memory.md, skills-policy.md; `.claude/rules/lessons.md`. Not read for this task: gitflow.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md (no commit, no design, store or code work here). android-emulator-skill / android-testing not invoked: the brief supplied `tools/ui.py`, `robot.py`, `prefs.py` and told me to use those; no test was written.
Ran in parallel with: none
Loops: 0
Lessons applied: none (lessons.md lists 0 active lessons; no `.planning/recall/RECALL.md` exists)

## Summary
Both devices, signed in and online, record the same thing after every push: `play_games_cloud_push_pending = True`, `play_games_cloud_last_push_stage = FAILED_OTHER`, and **no** `play_games_cloud_last_push_status` key. There is no status code to interpret.
`FAILED_OTHER` without a status means a non-`ApiException` was thrown outside the three staged SDK awaits. I attached `jdb` to the debuggable app during a solve on each device and caught it: `java.lang.IllegalStateException` thrown by `SnapshotsClient$DataOrConflict.getConflict()` at `PlayGamesCloudSaveStore.kt:296` (`val conflict = result.conflict ?: break`), identical stack on both devices.
The pinned SDK (play-services-games-v2 22.0.0) throws `"getConflict called when there is no conflict."` whenever `isConflict()` is false — verified by disassembling the class. The code and its unit-test mock both assume it returns `null`.
Reading: **app defect, high confidence.** `open()` succeeds on both devices (the exception is raised on the value `open` returned), so sign-in, the Drive appfolder grant and the Saved Games backend configuration are all working at least as far as opening the snapshot. `load` goes through the same function, which also explains why nothing was ever restored.

## Per-device values (verbatim from `python3 tools/prefs.py <serial>`)
Both apps showed Settings "Signed in as <gamer tag>" before the solve; network up on both (`Active default network` 100 / 103).

| | Pixel_9a `emulator-5554` | Pixel_9 `emulator-5556` |
|---|---|---|
| Before relaunch (new APK installed 02:36, not yet started) | `play_games_cloud_push_pending = True`; no stage key; no status key | same |
| ~35 s after force-stop + relaunch, no solve (02:38:24 / 02:38:27) | `play_games_cloud_push_pending = True` · `play_games_cloud_last_push_stage = FAILED_OTHER` · status key absent | identical |
| 30 s after robot solve of Challenge 5 (9a) / 4 (9), on Results (02:39:44) | `play_games_cloud_push_pending = True` · `play_games_cloud_last_push_stage = FAILED_OTHER` · status key absent | identical |
| After second solve with jdb attached, Challenge 6 (9a) / 5 (9) (02:44:46) | `play_games_cloud_push_pending = True` · `play_games_cloud_last_push_stage = FAILED_OTHER` · status key absent | identical |

`play_games_submissions_pending = False` on both throughout (leaderboards/achievements still reach Google). Step 2 of the brief (force-stop, relaunch, 45 s, re-read) was not needed as a tie-breaker: the devices agree and the stage key is present; the relaunch row above is the equivalent read anyway.
Limit: DataStore skips a write whose values are unchanged, so the prefs file alone cannot prove the post-solve push re-wrote the stage. The jdb capture does prove a push ran at solve time on each device and failed the same way.

## Status code
None recorded on either device, so there was nothing to look up and I did not verify any `GamesClientStatusCodes` / `CommonStatusCodes` table. Per `PlayGamesCloudSaveStore.statusCodeOf` the key is written only when the failure is an `ApiException`; an absent key with `FAILED_OTHER` means the throwable was something else, which the jdb capture confirms.

## What threw (evidence/93, evidence/94 — same on both devices)
```
Exception occurred: java.lang.IllegalStateException (to be caught at: ...BaseContinuationImpl.resumeWith())
  [1] com.google.android.gms.games.SnapshotsClient$DataOrConflict.getConflict (play-services-games-v2@@22.0.0:1)
  [2] ...platform.games.save.PlayGamesCloudSaveStore.resolveToOpenSnapshot (PlayGamesCloudSaveStore.kt:296)
  [3] ...platform.games.save.PlayGamesCloudSaveStore.openResolved (PlayGamesCloudSaveStore.kt:256)
```
SDK source of truth (`javap -c` on `classes.jar` from the Gradle-cached `play-services-games-v2-22.0.0.aar`):
```
getConflict(): invokevirtual isConflict:()Z ; ifne 17
  new java/lang/IllegalStateException ; ldc "getConflict called when there is no conflict." ; athrow
```
`PlayGamesCloudSaveStore.kt:294-296` comments that `getConflict()` is "null exactly when `isConflict()` is false"; `PlayGamesCloudSaveStoreTest.kt:583-586` stubs the mock the same way (`every { result.conflict } returns null`), which is why 873 green unit tests never saw it. Line 316 (`result.conflict ?: return result.data`) has the same problem.
No other exception of the caught classes (`RuntimeException`, `IOException`, `ApiException`, `Error`) came from app or Play Games code during either solve; the only other hits were inside the ads SDK / its OkHttp thread.

## Implications (confidence stated)
- **App defect — high confidence.** Deterministic, same line on two devices, reproduced on 2 of 2 observed pushes, and explained by the SDK bytecode. It fires on the normal no-conflict path, i.e. on every push and every pull.
- **Account / grant state — not the cause of this failure (high confidence).** The exception is raised on the object returned by a completed `open(...).await()`; a missing grant or sign-in problem would have surfaced as `OPEN_FAILED` plus a status. GMS logs `Promoting to games_lite & drive_appfolder` and `Successfully authorized` on both.
- **Backend configuration — not the cause of this failure (medium-high confidence).** `open` with `createIfNotFound=true` returning normally means the Saved Games API accepted the request. What I cannot say: whether `commitAndClose` will succeed once line 296 is fixed — no run has ever reached it. A second, later failure (commit, cover size, A4 in the tech-lead's list) is still possible and the new stage/status keys will show it.
- **Side effect to hand to the developer:** the throw happens after `open` succeeded and before `save`'s `try/finally` takes ownership, so the opened handle is never closed — one leaked open snapshot per push and per pull. Opens kept succeeding in this session, so it has not (yet) turned into `OPEN_FAILED`, but it is the Pitfall 1 shape the file's own KDoc warns about.
- The previous pass's reading ("consistent with an un-enabled toggle") is superseded; the founder's Play Console confirmation and this capture agree.

## Outputs
- `.planning/reports/phase-13/qa-engineer-save-reason.md` — this report
- `.planning/reports/phase-13/evidence/91-p9a-logcat-push-savereason.filtered.txt` (4.9 KB, 35 lines) — Pixel_9a logcat 02:37-02:45, only Games / sign-in / snapshot / Drive / auth lines; URLs, player ids, long numbers, emails masked. Shows GMS sign-in brokering and `drive_appfolder` scope promotion; **contains no snapshot line at all** even with `PlayGamesServices`/`SnapshotAgent`/`GamesServiceBroker` at VERBOSE.
- `.planning/reports/phase-13/evidence/92-p9-logcat-push-savereason.filtered.txt` (4.9 KB, 35 lines) — same for Pixel_9.
- `.planning/reports/phase-13/evidence/93-p9a-jdb-push-exception.txt` (1.5 KB) — jdb exception + stack, Pixel_9a, solve at 02:42:22.
- `.planning/reports/phase-13/evidence/94-p9-jdb-push-exception.txt` (1.5 KB) — same, Pixel_9, solve at 02:43:44.
- No screenshots saved to the repo. Settings was read through the UI dump with the gamer tag masked at source; one ad screenshot went to the session scratchpad and was deleted. Raw logcats and the jdb helper (`jdbcatch.py`) stayed in the scratchpad, outside the repo.

## Checks
- `adb devices` — pass (emulator-5554, emulator-5556 both `device`; host load ~5, no emulator incident this run)
- `adb -s <serial> shell dumpsys package com.erman.pegsolitarie | grep lastUpdateTime` — pass (02:36:39 / 02:36:49, 3.0.0: the new build)
- `python3 tools/ui.py <serial> dump` on Settings, both — pass ("Signed in as <gamer tag>")
- `python3 tools/robot.py <serial>` Challenge 5 + 6 on 5554, 4 + 5 on 5556 — pass (Results "Perfect!", "New best!" each time)
- `python3 tools/prefs.py emulator-5554` / `emulator-5556`, 30 s after solve — fail for cloud (`cloud_push_pending = True`, stage `FAILED_OTHER`, status absent; identical on both)
- `jdb -attach` via `adb forward tcp:870x jdwp:<pid>` with `catch all java.lang.RuntimeException|java.io.IOException|...ApiException|java.lang.Error` during a solve, both devices — fail (IllegalStateException at `PlayGamesCloudSaveStore.kt:296`)
- `javap -c -p -cp classes.jar 'com.google.android.gms.games.SnapshotsClient$DataOrConflict'` — confirms `getConflict()` throws when `isConflict()` is false
- `grep -ciE "@|a_[0-9]|https?://" evidence/9[1-4]-*` — pass (only hit is the literal `games-v2@@22.0.0` artifact coordinate in the jdb files)
- `adb forward --list`; `adb logcat -d -b crash` — pass (no forward left; no crash; app processes alive on both)
- Status-code lookup — not run: no status code was recorded
- `./gradlew check` — not run: no code changed; out of scope for this diagnostic
- Not verified: that a commit succeeds once line 296 is fixed; anything on a physical device

## Open issues / risks
- **BUG (high) — cloud save and restore always fail: `DataOrConflict.getConflict()` used as a nullable.** Steps: signed in + online, solve any level (or just launch with a pending push). Expected: snapshot committed, `stage = COMMITTED`. Actual: `IllegalStateException` at `PlayGamesCloudSaveStore.kt:296`, `stage = FAILED_OTHER`, no status, pending stays True; `load` fails the same way so no restore. Evidence: `evidence/93…`, `94…`. The unit-test mock (`PlayGamesCloudSaveStoreTest.kt:583-586`) encodes the wrong SDK contract and needs correcting with the fix (mock should throw on `conflict` when `isConflict` is false). I changed no code.
- Leaked open snapshot handle per failed push/pull (see Implications) — same fix area.
- Device state I changed: two extra Challenge levels solved per device (9a now level 7, 9 now level 6); `log.tag.PlayGamesServices`, `SnapshotAgent`, `GamesServiceBroker` set to VERBOSE on both (resets on reboot); one test interstitial closed on Pixel_9a. No account, GMS data or Play Console change. Both emulators left running.
- jdb suspends the VM for ~0.2 s per caught exception; it did not alter the outcome (same prefs values with and without it attached).
- Rules conflict to flag for the producer: plugin `testing.md` 0.1.1 says QA artifacts live under git-ignored `.planning/qa/<task-slug>/` and are never committed; the brief told me to write to `.planning/reports/phase-13/evidence/`, which is not ignored. I followed the brief and left the files uncommitted; the producer decides where they end up. The repo rules snapshot is still 0.1.0.
- An untracked `.planning/reports/phase-13/code-auditor-cloud-save-outcome.md` was already in the tree; not mine, not touched.

## Handoff
VERDICT: FAIL (cloud save path; diagnostic complete)
Next: tech-lead — route the line-296 defect (and the handle leak, line 316, and the wrong test mock) to android-game-dev; this replaces A1/A2/A4 as the first thing to fix. Then qa-engineer — reinstall, solve once per device, read the stage/status keys again (expect `COMMITTED`, or a real stage + status code if a second failure sits behind this one), then re-run UAT tests 10-13.
