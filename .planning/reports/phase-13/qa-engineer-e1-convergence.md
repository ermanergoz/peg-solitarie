# qa-engineer report — phase 13 experiment E1
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (c2c1006..c2c1006 — nothing committed by QA, no tracked file changed)
Skills loaded: ergoz-soft:standards (role qa-engineer). The repo rules snapshot is 0.1.0 and the plugin is 0.1.1, so I read the plugin copies of studio.md, reporting.md and testing.md, plus `.claude/rules/lessons.md`. I did not read gitflow.md, skills-policy.md, logging.md or memory.md, because this run made no commits, skill choices or memory writes. I did not invoke android-emulator-skill or android-testing: the brief named `tools/ui.py`, `robot.py` and `prefs.py`, and no test was written.
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; `.planning/recall/RECALL.md` does not exist)

This report is in the final message only; my harness forbids writing report files. The intended path is `.planning/reports/phase-13/qa-engineer-e1-convergence.md`.

## Summary
- Neither marker arrived on the other device within 15 minutes. Marker A, solved on Pixel_9a, was still missing on Pixel_9 at +35 minutes. Marker B, solved on Pixel_9, was missing on Pixel_9a at +15 minutes. E1 matches row 3 of the outcome table, "Neither arrives within 15 minutes — H1 or configuration", so the next step is to build E2.
- H3's prediction failed. Pixel_9a logged 0 `Found conflict` and 0 `Inconsistent DB` lines on all 5 of its launches. Pixel_9 also logged none on its 4 launches.
- The failure is now the same in both directions. Pixel_9's levels 10–12 and Wiegleb-45 never reached Pixel_9a. Pixel_9a's English-33 and Asymmetric never reached Pixel_9.
- Every launch and every solve produced one GMS snapshot commit, visible as the cover-size warning. Prefs read `COMMITTED` / pending `False` on both devices at the start and the end.
- `dumpsys content` on both devices shows 0 active syncs, 0 pending syncs and no games sync run during the experiment.

## Timeline
All times are host clock. Pixel_9a's clock runs 9 s behind the host, and Pixel_9's clock equals the host.

**Start, 04:02.**

| | Pixel_9a | Pixel_9 |
|---|---|---|
| Challenge ladder | level 9 | level 12 |
| English-33 | 3/3 | 0/3 |
| European-37 | 3/3 | 3/3 |
| Asymmetric 3-3-2-2 | 0/3 | 0/3 |
| Diamond-41 | 0/3 | 0/3 |
| Wiegleb-45 | 0/3 | 0/3 |
| Push prefs | `COMMITTED`, pending `False` | `COMMITTED`, pending `False` |

`play_games_merged_player_id` is identical on both devices, so both are on the same account.

**Step 1 — Pixel_9a relaunched twice while Pixel_9 was force-stopped (from 04:02:47).**

| Launch | Host time | `Found conflict` | `Inconsistent DB` | Commits | Result |
|---|---|---|---|---|---|
| 1 | 04:02:48 | 0 | 0 | 1 (device 04:02:44) | level 9, catalog unchanged |
| 2 | 04:03:45 | 0 | 0 | 1 (device 04:03:42) | level 9, catalog unchanged |

I watched each launch for 40 s before counting.

**Step 2 — marker A is Asymmetric 3-3-2-2, solved on Pixel_9a.**
- `robot.py --read-only` read 38 pegs in 39 cells, which is correct. The DFS took 23.2 s and the solve was 37 moves.
- The solve finished at 04:06:14. Results showed "Perfect!", "New best!", score 8250, 1 peg left.
- GMS logged the commit at device 04:06:09, which is about 04:06:18 host.
- The app stayed in the foreground on Results, with the same pid, until the 04:27:38 relaunch. No force-stop was issued in that time.
- `dumpsys content` at +17 s showed auto sync on, data connected, 0 active syncs and 0 pending syncs. The last `com.google.android.gms.games.background` sync was 02:15:06, the last `com.google.android.gms.games` sync was 2026-09-15, and the Backoff column was empty.

**Step 3 — Pixel_9 relaunches after marker A.** Each launch was read 25 s after it started.

| Offset | Host time | Asymmetric on Pixel_9 | English-33 | Ladder | Conflicts |
|---|---|---|---|---|---|
| +2 min | 04:08:13 | 0/3 | 0/3 | 12 | 0 |
| +5 min | 04:11:14 | 0/3 | 0/3 | 12 | 0 |
| +15 min | 04:21:15 | 0/3 | 0/3 | 12 | 0 |
| +35 min (extra, not in the spec) | 04:41:23 | 0/3 | 0/3 | 12 | 0 |

Each launch produced one commit.

**Step 4 — marker B is Wiegleb-45, solved on Pixel_9.**
- The robot read 44 pegs in 45 cells. The DFS took 143.6 s and the solve was 43 moves.
- The solve finished at 04:25:38. Results showed "Perfect!", "New best!", score 8760, 1 peg left. GMS logged the commit at 04:25:40.
- The app stayed in the foreground on Results, with the same pid, until the 04:41:23 extra relaunch.
- `dumpsys content` at +16 s showed 0 active syncs and 0 pending syncs. The last games and games.background syncs were at 01:30, and the Backoff column was empty.

Pixel_9a relaunches after marker B:

| Offset | Host time | Wiegleb-45 on Pixel_9a | Ladder | Conflicts | `Inconsistent DB` |
|---|---|---|---|---|---|
| +2 min | 04:27:38 | 0/3 | 9 | 0 | 0 |
| +5 min | 04:30:38 | 0/3 | 9 | 0 | 0 |
| +15 min | 04:40:38 | 0/3 | 9 | 0 | 0 |

**End, 04:42.**

| | Pixel_9a | Pixel_9 |
|---|---|---|
| Challenge ladder | level 9 | level 12 |
| English-33 | 3/3 | 0/3 |
| European-37 | 3/3 | 3/3 |
| Asymmetric 3-3-2-2 | 3/3 | 0/3 |
| Diamond-41 | 0/3 | 0/3 |
| Wiegleb-45 | 0/3 | 3/3 |

Prefs are unchanged on both devices.

**The whole continuous capture, masked:**
```
P9a 04:02:44.216 W/PlayGamesServices[GamesService]: Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled
P9a 04:03:42.736 (same line)   04:06:09.566 (same, marker A)   04:27:35.855 (same)   +2 more at the 04:30 and 04:40 launches
P9  04:08:16.079 (same line)   04:11:16.113   04:21:19.661   04:25:40.055 (marker B)   +1 at 04:41
```
Pixel_9a produced 6 `PlayGamesServices` lines and Pixel_9 produced 5. All 11 are the cover warning. No other `PlayGamesServices` line, conflict line or error line appeared on either device.

## Outcome table match
- E1 matches row 3: neither marker arrives within 15 minutes, meaning H1 or configuration, and the next step is to build E2.
- E1 does not match row 1 (H3). The condition "conflicts on every Pixel_9a open" was false on 5 of 5 launches, and marker B did not arrive.
- E1 does not match row 2 (H2). Nothing arrived within 15 minutes, and marker A had not arrived by 35 minutes. Pixel_9's levels 10–12 have now been absent from Pixel_9a for over 80 minutes across many pulls.

What the evidence does not show:
- **Whether the fault is in the app or in GMS/configuration.** Every open returns with no conflict, and every read returns only that device's own data. That fits a server that receives nothing from either device. It also fits two devices that are not addressing the same server object. Matching player ids argue against the second reading but do not exclude it.
- **H3 is weakened, not excluded.** Pixel_9a's GMS could be damaged in a way that no longer produces conflict lines. But Pixel_9's writes also fail to reach Pixel_9a, and both devices now behave identically, so a Pixel_9a-only explanation no longer covers the observations.
- **`dumpsys content`.** It is unclear whether Play Games snapshot upload goes through SyncManager at all on these images. If it does, no sync was requested after any of the 11 commits. If it does not, the dump is uninformative. GMS logged no network or upload error on either device.
- **`SYNC_OP_STATE_INVALID: NOT_SYNCABLE`.** Pixel_9a logged two of these lines at device 04:06:22. That is the same second as my `dumpsys content` call, and the lines did not recur at any other commit. I treat them as an artifact of the dump.
- **GMS versions differ.** Pixel_9a runs 26.15.33 and Pixel_9 runs 26.08.34. This is recorded only; it has no demonstrated effect.
- **The 03:12 transfer.** One transfer did succeed in the earlier pass, arriving on Pixel_9a through two conflicts. E1 did not reproduce it and does not explain it.

Confidence:
- High that neither marker arrived and that there were 0 conflicts. The UI dumps and screenshots agree and the log capture ran continuously.
- High that H3's stated prediction is false.
- Medium (about 65%) that the cause is common to both devices (app or project/configuration) and not a per-device GMS fault.
- Low on which of H1 or configuration it is. E1 cannot see the server. E2's `changePending`, `lastModifiedAt` and `writtenByThisDevice` fields are what can separate them.

## Outputs
All in `/Users/ermanergoz/Desktop/PegSolitarie/.planning/qa/phase-13-e1/` (git-ignored, 1.3 MB total). Raw logcats, full-size screenshots and raw `dumpsys` output were deleted.
- `00-start-prefs-emulator-5554.txt`, `00-start-prefs-emulator-5556.txt`, `90-end-prefs-emulator-5554.txt`, `90-end-prefs-emulator-5556.txt` (about 250 B each) — prefs at start and end, player id masked.
- `01-start-classic-emulator-5554-s.png`, `01-start-classic-emulator-5556-s.png` (about 100 KB each) — starting catalogs.
- `10-step1-launch1-emulator-5554-classic-s.png`, `11-step1-launch2-emulator-5554-classic-s.png` (106 KB each) — step 1.
- `20-p9a-markerA-result-s.png` (55 KB), `40-p9-markerB-result-s.png` (51 KB) — marker Results screens.
- `21-p9a-dumpsys-content-games.masked.txt` (8 KB), `41-p9-dumpsys-content-games.masked.txt` (5 KB) — sync state during each foreground window, account masked.
- `30-A-plus2-emulator-5556-classic-s.png`, `31-A-plus5-emulator-5556-classic-s.png`, `32-A-plus15-emulator-5556-classic-s.png`, `60-extra-A-plus35-emulator-5556-classic-s.png` (100 KB each) — Pixel_9 catalog after marker A.
- `50-B-plus2-emulator-5554-classic-s.png`, `51-B-plus5-emulator-5554-classic-s.png`, `52-B-plus15-emulator-5554-classic-s.png` (106 KB each) — Pixel_9a catalog after marker B; I inspected the +15 frame by eye.
- `80-logcat-emulator-5554.masked.txt` (1.4 KB), `80-logcat-emulator-5556.masked.txt` (0.8 KB) — the full filtered capture, masked.
- `relaunch.sh` (1.4 KB) — session helper: force-stop, launch, wait, read Home and Classic, count GMS lines since the launch.

## Checks
- `adb devices` — pass (both `device`; host load 4.4; no emulator incident; Gradle not run).
- `python3 tools/prefs.py <serial>` at start and end — pass (`COMMITTED`, pending `False`, submissions pending `False` on both, unchanged).
- `ui.py dump` of the Classic catalog on both devices before the run — pass (Asymmetric, Diamond-41 and Wiegleb-45 were 0/3 on both).
- Step 1, `grep -c "Found conflict"` and `grep -c "Inconsistent DB"` per Pixel_9a launch — 0/0 and 0/0, so the H3 prediction is not met.
- `robot.py emulator-5554` on Asymmetric — pass (38/39 read, 37 moves, 3/3, "New best!").
- `robot.py emulator-5556` on Wiegleb-45 — pass (44/45 read, 43 moves, 3/3, "New best!").
- `relaunch.sh emulator-5556` at +2, +5, +15 and +35 minutes after marker A — fail for convergence (Asymmetric 0/3 each time).
- `relaunch.sh emulator-5554` at +2, +5 and +15 minutes after marker B — fail for convergence (Wiegleb-45 0/3, ladder 9 each time).
- `dumpsys activity activities | grep topResumedActivity` plus `pidof` during each foreground window — pass (same pid, app resumed, screen-off timeout is max).
- `adb shell dumpsys content | grep -i -B2 -A8 games` on both devices — info (0 active, 0 pending, no backoff value, no games sync during the run).
- `logcat -d --pid=<gms pid> '*:W'` since 04:00 on both devices — info (the same warning set on both; nothing games-specific except the cover line).
- `grep -lE "@gmail|a_[0-9]{10}"` over all artifacts — pass (0 files). See the privacy incident under Open issues / risks.
- `git status --short` — pass (clean).
- `./gradlew check` — not run (forbidden by the brief; no code changed).
- Physical device — not run (none attached).

## Open issues / risks
- **BUG (high, unchanged) — cross-device sync does not converge.**
  - The failure is now reproduced in both directions, with zero conflicts and every push reported as `COMMITTED`.
  - To reproduce: follow steps 2–4 above.
  - Expected: the marker shows 3/3 on the other device after a relaunch.
  - Actual: it is absent at +2, +5 and +15 minutes, and at +35 minutes for marker A.
- **Privacy incidents, both corrected.** Nothing reached the report or any remaining file.
  - **Player id written to disk.** My first `prefs.py | tee` wrote the per-app player id into two artifact files. I masked them in place within a minute.
  - **Email in tool output.** One `grep` over the raw `dumpsys content` printed the account email into my tool output, not into a file. I replaced the raw dump with a masked excerpt and deleted it. If the studio log records tool output, the producer should be aware of this.
- **Device state changed — Pixel_9a.**
  - Asymmetric 3-3-2-2 is now solved at 3/3 with best 8250.
  - The app was force-stopped and relaunched 5 times and is left running on Home.
  - The level-9 in-progress game is untouched.
- **Device state changed — Pixel_9.**
  - Wiegleb-45 is now solved at 3/3 with best 8760.
  - The app was force-stopped once at the start and relaunched 4 times, and is left running on Home.
- No account, GMS-data, settings or Play Console change was made. No `pm clear` was run and the network was not toggled. Both emulators are still running and online.
- **Four unconverged markers are in place for an E2 build:**
  - Pixel_9a holds two that Pixel_9 lacks: English-33 and Asymmetric.
  - Pixel_9 holds two that Pixel_9a lacks: Wiegleb-45 and ladder levels 10–12.
  - Diamond-41 is 0/3 on both.
- **Every launch commits a 1 MB-cover snapshot.** There were 11 commits in 40 minutes, 9 of them from launches that solved nothing. If the server is receiving these, the churn works against diagnosis, which supports the tech-lead's push guard.

## Handoff
VERDICT: FAIL (experiment completed as specified; convergence fails in both directions; outcome row 3)

Next: tech-lead — E1 gives "neither arrives", and the H3 prediction is false (0 conflicts on 5 of 5 Pixel_9a launches). Per your table, build E2 before anything else. With H3 weakened, F-3 moves up as you stated, and I suggest adding a configuration check to the triage: whether both installs resolve to the same Play Games project and the same snapshot namespace, and whether Saved Games is enabled for that project. I would not raise the founder question for a clean signed-in AVD on H3 grounds yet, because E1 does not show H3.
