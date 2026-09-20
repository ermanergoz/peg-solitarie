# qa-engineer report — phase 13 experiment E2

**Reading: the data gets as far as the device's own Play services cache and no further.** On every open, each device gets back only its own last write, with the "not yet uploaded" flag set. The other device's write never arrived, even 11–13 minutes after a commit. That points at the Saved Games upload, or the project's Saved Games configuration, and away from the app's merge code.

- **Never leaves the device:** about 75% confidence.
- **Leaves but is not served back:** about 20%.
- **Served but not merged:** under 5%. The probe runs before any app-side merge, and it shows the opened snapshot is the device's own, with 0 conflicts.
- **What holds this back:**
  - I never saw `change_pending = False` on either device, so I cannot show that the flag ever clears on these images.
  - The one successful transfer at 03:12 in the earlier pass is still unexplained.

Date: 2026-09-20
Branch: feature/phase-13-cloud-save (88c2fd7..88c2fd7 — QA committed nothing; `git status --short` is clean)
Skills loaded: ergoz-soft:standards (role qa-engineer). The repo rules snapshot is 0.1.0 and the plugin is 0.1.1, so I read the plugin copies of studio.md, reporting.md and testing.md, plus `.claude/rules/lessons.md`. I did not invoke android-emulator-skill or android-testing: the brief named `tools/ui.py`, `prefs.py` and `robot.py`, and no test was written.
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; `.planning/recall/RECALL.md` does not exist)

My harness forbids writing report files, so this message is the full report. The intended path is `.planning/reports/phase-13/qa-engineer-e2-probe.md`.

## Summary
- Every one of the 13 reads that followed a launch showed `opened=True`, `change_pending=True`, `conflicts_resolved=0`. `last_modified_at` equalled that device's own last commit every time, and the flag never cleared.
- **C, in one sentence:** no — the reader's `last_modified_at` never advanced to the writer's commit time, in either direction, at +2 minutes or at +11–13 minutes.
- The push guard works. Five launches with nothing new to push committed nothing. Commits no longer show up in logcat, so I inferred them from whether `last_modified_at` moved on the next launch.
- The picker works on both devices and needed no retry. Each lists exactly one entry, and the entry describes that device's own data, so the two pickers disagree.
- Configuration is identical on both installs: same APK hash, same APP_ID, same signing certificate and same Google account. Play services core differs (26.15.33 on Pixel_9a, 26.08.34 on Pixel_9). Pixel_9's Play Games app does not list the game under "Continue playing", while Pixel_9a's does.

## A. Probe keys per read
Times are host clock. Pixel_9a's clock runs 9 s behind the host, and Pixel_9's clock equals the host. `play_games_cloud_last_push_status` was absent on every read on both devices.

**Pixel_9a (emulator-5554)**

| Read | Launch → read (host) | push_pending | last_push_stage | pull_opened | change_pending | conflicts_resolved | last_modified_at |
|---|---|---|---|---|---|---|---|
| 00 baseline, new build not yet launched | – → 04:45:03 | False | COMMITTED | absent | absent | absent | absent |
| A1 | 04:45:27 → 04:46:12 | False | COMMITTED | True | True | 0 | 1789868436695 = 04:40:36.695 |
| A2 | 04:48:32 → 04:49:17 | False | COMMITTED | True | True | 0 | 1789868436695 (unchanged) |
| A3 | 04:51:40 → 04:52:25 | False | COMMITTED | True | True | 0 | 1789868436695 (unchanged) |
| C1 writer, level 9 solved 04:53:06, no relaunch | – → 04:54:10 | False | COMMITTED | True | True | 0 | 1789868436695 (keys come from the A3 pull) |
| C2 reader, after Pixel_9's 04:55 write | 04:56:19 → 04:57:05 | False | COMMITTED | True | True | 0 | 1789869180912 = 04:53:00.912 (its own solve) |
| D picker launch | 04:58:07 → 04:58:50 | False | COMMITTED | True | True | 0 | 1789869180912 |
| F1 | 05:06:05 → 05:06:51 | False | COMMITTED | True | True | 0 | 1789869180912 |

**Pixel_9 (emulator-5556)**

| Read | Launch → read (host) | push_pending | last_push_stage | pull_opened | change_pending | conflicts_resolved | last_modified_at |
|---|---|---|---|---|---|---|---|
| 00 baseline | – → 04:45:03 | False | COMMITTED | absent | absent | absent | absent |
| A1 | 04:45:27 → 04:46:12 | False | COMMITTED | True | True | 0 | 1789868485987 = 04:41:25.987 |
| A2 | 04:48:32 → 04:49:17 | False | COMMITTED | True | True | 0 | unchanged |
| A3 | 04:51:40 → 04:52:25 | False | COMMITTED | True | True | 0 | unchanged |
| C1 reader, after Pixel_9a's 04:53 write | 04:54:11 → 04:54:56 | False | COMMITTED | True | True | 0 | 1789868485987 (did not advance) |
| C2 writer, level 12 solved 04:55:15, no relaunch | – → 04:56:18 | False | COMMITTED | True | True | 0 | 1789868485987 (keys come from the C1 pull) |
| D picker launch | 04:57:23 → 04:58:06 | False | COMMITTED | True | True | 0 | 1789869319601 = 04:55:19.601 (its own solve) |
| F1 | 05:06:05 → 05:06:51 | False | COMMITTED | True | True | 0 | 1789869319601 |

**Comparison with the E1 commit times**
- **Pixel_9a sees its own write.** 04:40:36.7 is the commit from its own last E1 launch (host 04:40:38, device 04:40:29).
- **Pixel_9 sees its own write.** 04:41:26.0 is the commit from its own E1 "+35 extra" launch at 04:41:23.
- **Neither sees the other's.** Pixel_9a never shows 04:41:26, which is later than its own value. Pixel_9 never shows Pixel_9a's later 04:53:01.
- **The timestamp is stamped by the device, not the server.** On the host clock, Pixel_9a's 04:40:36.7 would fall before its launch at 04:40:38, which is impossible. On the device clock it is launch + 7.7 s, which fits. So `last_modified_at` by itself shows only a local commit. It says nothing about whether the server received it.
- **`change_pending` never cleared.** It was True on all 13 reads that followed a launch, up to 13 minutes 50 s after the device's last commit.
  - Both devices were online throughout: the network was validated, `www.googleapis.com` answered a ping, data saver was off and device idle was ACTIVE.
- **`last_modified_at` never moved except after that device's own solve.**

## B. Push guard
- **Logcat no longer shows commits.** There were 0 `PlayGamesServices` lines, 0 "cover too large" warnings, 0 `Found conflict` and 0 `Inconsistent DB` on every ordinary launch (A1–A3, C, F1). Each launch window held about 1,350–2,240 log lines. The cover warning is gone as expected, and no other Play services line marks a commit.
- **Indicator I used:** `last_modified_at` on the next launch. A launch that committed would move it to roughly launch + 3–8 s.
- **Launches with nothing new committed nothing.**
  - A1, A2 and A3 on both devices were each checked by the next read, and the value stayed put.
  - The 04:54 and 04:57 launches on Pixel_9 were checked the same way: the value moved only to the 04:55:19 solve time, not to any launch time.
  - The 04:56 and 04:58 launches on Pixel_9a were checked the same way, with the same result.
  - In E1, the old build committed once per launch.
- **Solves still commit.** Both solves moved that device's own `last_modified_at`: 04:53:00.9 on Pixel_9a and 04:55:19.6 on Pixel_9.
- **Limit of the indicator:** each F1 launch was the last on its device, so nothing can show whether it committed.

## C. Cross-device observation

| Writer | Commit (its own clock) | Reader relaunched (host) | Reader's `last_modified_at` | Advanced? |
|---|---|---|---|---|
| Pixel_9a, level 9 → 10 | 04:53:00.9 | Pixel_9 at 04:54:11 (+1 min) | 04:41:26.0 | No |
| Pixel_9a, level 9 → 10 | 04:53:00.9 | Pixel_9 at 04:57:23 and 05:06:05 (+4 and +13 min) | 04:55:19.6 (its own) | No |
| Pixel_9, level 12 → 13 | 04:55:19.6 | Pixel_9a at 04:56:19, 04:58:07 and 05:06:05 (+1, +3 and +11 min) | 04:53:00.9 (its own) | No |

Neither device ever saw the other's write. The Classic catalog was re-read at 04:59 and is still unconverged:

| Board | Pixel_9a | Pixel_9 |
|---|---|---|
| English-33 | 3 | 0 |
| European-37 | 3 | 3 |
| Asymmetric 3-3-2-2 | 3 | 0 |
| Diamond-41 | 0 | 0 |
| Wiegleb-45 | 0 | 3 |
| Challenge ladder | level 10 | level 13 |

## D. Snapshot picker
The picker appeared within 20 s of force-stop plus `am start` on both devices, on the first try. Google's screen is `InGameUiActivity`, titled "Saved games (debug)". No gamer tag or account is shown on it, so I saved screenshots. I did not tap Select, and I did not tap Refresh.

| | Pixel_9a | Pixel_9 |
|---|---|---|
| Entries | 1 | 1 |
| Description | Challenge level 10 · 36 stars · 3/5 Classic boards | Challenge level 13 · 42 stars · 2/5 Classic boards |
| Played time | 4 minutes | 1 minute |
| Last modified | Today, 4:53 AM | Today, 4:55 AM |
| Cover | present: the board art, pink pegs on purple, not scaled or broken | present: the same art |
| Matches the device's own app? | yes: level 10; 3 Classic boards at 3 stars; 9 + 27 = 36 | yes: level 13; 2 Classic boards at 3 stars; 6 + 36 = 42 |

- **Comparison:** the two devices do not list the same entry. Each lists one entry under the same title slot, carrying only its own data and its own save time.
- **CLOUD-03 substitute:** each description is accurate for the device it is on.
- **Picker log line:** the picker call logs `W PlayGamesServices[SnapshotsClient]: You are using the deprecated Play Games Services v1 SDK…` from the app process, once per device. It appears only on the picker path and never on an ordinary launch. I record it but draw nothing from it.

## E. Configuration

| Item | Pixel_9a | Pixel_9 |
|---|---|---|
| Installed base.apk sha256 (first 16) | 1c480a55cc614da0 | 1c480a55cc614da0 |
| `games.APP_ID` meta-data → resource | @0x7f0f004d; 12 digits; sha256 of the value 8ace46a7ed27 | identical |
| Signing signature | cf2c789a | cf2c789a |
| Device Google account | 1 account, hash 042fa693c7 | 1 account, the same hash |
| Play services core | 26.15.33 (261533035) | 26.08.34 (260834035) |
| Play Games app | 2025.09.66390 | 2025.09.66390 |
| Play Store | 53.2.23 | 50.5.23 |
| System image | sdk_gphone16k_arm64, BE4B.251210.005 | sdk_gphone64_arm64, BP22.250325.006 |

- **How I read the APP_ID:** I pulled the APKs and read the manifest with `aapt2`. I compared the value by hash only and never printed it.
- **Play Games app sign-in:** both devices show the Home/Library/Profile tabs with no "Sign in" prompt, so both are signed in. I did not open Profile, so I did not compare the account inside the Play Games app directly. The matching device-account hash and E1's matching merged player id stand in for that comparison.
- **Difference in the Play Games Library:**
  - Pixel_9a's "Continue playing" lists "Peg Solitaire". Its detail page shows "Just played" and 13/20 achievements.
  - Pixel_9's "Continue playing" lists only the three built-in games. The game appears on Pixel_9 only as the Play Store listing under "Install and play again". This was the same on two reads.
  - Pixel_9's Play Games app also showed its notifications permission prompt, which suggests it had not been opened before.
- **Achievement cross-check (weak):** Pixel_9a's Play Games shows "Constellation" (250 stars) at 16%. Pixel_9's 42 stars work out to 16.8%, and Pixel_9a's own 36 stars to 14.4%. If the achievement tracks total stars, the server is carrying Pixel_9's progress to Pixel_9a for this project. That would mean the account, the project and the network all work, and only Saved Games does not. I did not check how the achievement is computed.
- **Games sync on Pixel_9:** opening the Play Games app on Pixel_9 started a `GamesSyncAdapter` sync (04:59:42, 4.3 s). The F1 read seven minutes later still showed `change_pending=True` and Pixel_9's own data. No such sync line appeared on Pixel_9a.

## Outputs
All under `/Users/ermanergoz/Desktop/PegSolitarie/.planning/qa/phase-13-e2/` (git-ignored, 472 KB). Raw logcats, pulled APKs and raw UI dumps were deleted.
- `00-baseline-*`, `A1-*`, `A2-*`, `A3-*`, `C1-*`, `C2-*`, `D-*after-picker-launch*`, `F1-*` `-prefs.txt` (18 files, 0.3–0.6 KB each) — masked key reads with host and device time and the decoded timestamp.
- `C1-p9a-level9-result(-s).png` (72 KB / 50 KB), `C2-p9-level12-result(-s).png` (66 KB / 47 KB) — the two Results screens ("Perfect!", 5280, 1 peg).
- `D-p9a-picker-s.png` (83 KB), `D-p9-picker-s.png` (54 KB) — the picker on each device.
- `D-p9a-picker-dump.masked.txt`, `D-p9-picker-dump.masked.txt` (1.8 KB each) — the picker fields as text.
- `80-logcat-emulator-5554.masked.txt` (0.8 KB), `80-logcat-emulator-5556.masked.txt` (2.9 KB) — every `PlayGamesServices` and `GamesSyncAdapter` line of the session, masked.
- `launch.sh`, `pread.sh`, `mask.sh` — session helpers. `pread.sh` masks before anything touches disk.

Log excerpt, masked, from Pixel_9:
```
04:57:25.341 W PlayGamesServices[SnapshotsClient]: You are using the deprecated Play Games Services v1 SDK. ... <url>
04:59:42.172 I PlayGamesServices[GamesSyncAdapter]: Starting sync for 2b27bb49
04:59:46.513 I PlayGamesServices[GamesSyncAdapter]: Sync duration for 2b27bb49: 4258
```

## Checks
- `adb devices`, `pm path` plus `sha256sum base.apk` on both — pass (1c480a55cc614da0 on both, versionName 3.0.0, updated 04:43).
- `launch.sh <serial> A1|A2|A3|F1` — force-stop, `am start`, 45 s wait, masked `prefs.py`, log counts — pass as observation (the tables above).
- `robot.py emulator-5554` on Challenge level 9 — pass (4 pegs in progress, 3 moves, "Perfect!", level → 10).
- `robot.py emulator-5556` on Challenge level 12 — pass (4 pegs in progress, 3 moves, "Perfect!", level → 13).
- Reader relaunch after each solve — **fail for convergence** (`last_modified_at` did not advance; the catalog is unchanged).
- Push guard, judged by `last_modified_at` on the next launch — pass (0 commits on launches with nothing new).
- Count of `cover image is too large` lines — pass (0, against 11 in E1).
- `am start … --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true` on both — pass (the picker shows 1 entry on each; the code-auditor's L-2 late-picker concern did not occur).
- `aapt2 dump xmltree` and `dump resources` on both pulled APKs, comparing the hashed APP_ID — pass (identical).
- `dumpsys package` for Play services core, Play Games and Play Store — info (core differs between the devices).
- Ping, `dumpsys connectivity`, `cmd netpolicy` and `dumpsys deviceidle` on both — pass (online, unrestricted).
- `grep -lE "@gmail|@[a-z]+\.com|a_[0-9]{10}|ELLIDED:[0-9]"` over all artifacts — pass (0 files). See the privacy incident below.
- `git status --short` — pass (clean).
- `./gradlew check` — not run (forbidden by the brief; no code changed).
- Physical device — none attached.

## Open issues / risks
- **BUG (high, unchanged) — cross-device sync does not converge.**
  - To reproduce: run part C above.
  - Expected: the reader's probe shows the writer's commit time and the merged catalog.
  - Actual: each device's open returns its own last commit with `change_pending=True`, even after 13 minutes online and, on Pixel_9, after a games sync had run.
  - The app reports `COMMITTED` with `push_pending=False`. `COMMITTED` therefore means "committed to the local Play services cache", not "on the server". Any UI or test that treats it as uploaded is wrong.
- **Hypotheses for the tech-lead.** I cannot verify these without the Play Console, which the brief forbids.
  - Saved Games may not be enabled for the Play Games project, or the change may not be published. The test account may lack permission for it.
  - The Pixel_9 Library difference may mean that device's game record is only half registered.
- **Privacy incidents.**
  - **Player id printed and written to disk.** My first `mask.sh` used `\b`, which BSD `sed -E` does not support. One `PlayGamesServices[PlayerAgent]` excerpt therefore printed a Play Games player id and an `ELLIDED:<n>` account hash into my tool output. The same lines sat in `80-logcat-emulator-5556.masked.txt` for about a minute. I fixed the mask, re-masked the file and scanned all artifacts (0 hits).
  - **Developer display name printed.** One Play Games detail-page dump printed the game's developer display name into tool output only. It reached no file.
  - If the studio log records tool output, the producer should know about both. Neither value appears in this report.
- **Scratchpad mistake.** My cleanup `rm -f $SP/*.txt $SP/*.apk` ran against the shared scratchpad. Besides my own files it deleted six files left by earlier agents: `diff.txt`, `diffstat.txt`, `jdb-5554.txt`, `jdb-5554b.txt`, `jdb-5556.txt` and `p9-base.apk`. No repo file was touched. They cannot be recovered; I should have deleted by exact name.
- **Device state changed — Pixel_9a.**
  - Challenge level 9 is solved, so the ladder is 9 → 10. Its level-9 in-progress game is gone.
  - The app was force-stopped and relaunched six times and is left running on Home.
  - I opened the Play Games app, went from the game detail page back to Library, and sent it to the background.
- **Device state changed — Pixel_9.**
  - Challenge level 12 is solved, so the ladder is 12 → 13.
  - The app was force-stopped and relaunched six times and is left running on Home.
  - I opened the Play Games app. Its notifications prompt appeared twice, and I dismissed it with Back both times without choosing. I switched between the Home and Library tabs.
  - Opening the Play Games app triggered one `GamesSyncAdapter` sync.
- I made no `pm clear`, no account or Play services data change, no Play Console access, no network toggle and no emulator restart. I made no snapshot selection, deletion or refresh.
- **Unconverged markers now in place:**
  - Pixel_9a alone holds English-33 and Asymmetric.
  - Pixel_9 alone holds Wiegleb-45 and ladder levels 10–12.
  - Each device now has a level-13-versus-10 difference plus its own 04:53 or 04:55 commit.
- **A discriminating step I left untried.** The picker's Refresh button would force Google's UI to reload from the server. I did not tap it, because it may alter the Play services cache and destroy the reproducible state. It is the tech-lead's call.
  - If a refreshed picker still shows only the device's own entry, the server does not hold the other device's data.

## Handoff
VERDICT: FAIL (E2 completed as specified; the push guard, cover and picker work; convergence still fails in both directions)

Next: tech-lead.
- The probe shows every open returning the device's own locally stamped commit with `hasChangePending()=true` and 0 conflicts.
- Look at why Play services never uploads: the Saved Games configuration and publish state for the project, tester permissions, and Pixel_9's missing game registration.
- The app's merge code is not where the data stops.
- Decide whether QA may tap the picker's Refresh, and whether to raise a founder question for a Play Console check.
