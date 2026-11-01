# qa-engineer report — colour-vision (smoke pass on the merged head, then the 15e sitting)
Date: 2026-11-01
Branch: feature/colour-vision (report only). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-cv`, head `155045e`. Its code equals `fddc8e8`: `git diff --quiet fddc8e8 HEAD -- . ':(exclude).planning'` exits 0. Arm A is `305f741`, arm B is `155045e`.
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing in full. logging, puzzle-design and play-store are identical to the snapshot already loaded. I also read `.claude/rules/lessons.md`. I drove the device with `adb` and the session helpers, as below.
Ran in parallel with: the talkback QA session on `ergoz_studio`, which held the AVD until its free flag at 04:55. Other sessions shared the host: a licences `depthSweep` Gradle worker voided sitting attempt 1, and another session booted and shut `Pixel_9_clean` (04:42–≈04:55, before the sitting).
Ran on: claude-opus-5-5 at effort high. The pin is fable at high; founder answer Q-019 puts every Fable-pinned role on Opus until 3.0.0 is published. Every verdict below rests on a device capture, or on a number read from one.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-merge-ruling.md` Ruling 2 (the eight-step smoke, the device choice, the fallback, and brief §7.3 as amended); `tech-lead-brief.md` §9 (the arms, the precondition-7 key reading, the verdict line); `home-chip-arrival/tech-lead-device-ruling.md` §2 (the forward rule on re-attempting on the same boot); the field shape of `home-chip-arrival/qa-engineer-sitting2.md`.

## Summary
- **Smoke: PASS** on `ergoz_studio` with arm B's APK (`cf7696f6…6a82`). All eight steps show what the ruling names, and logcat has no crash and no ANR. The AVD was free at 04:55 (flag file), so the fallback was not used. I booted the AVD, restored it and shut it down (`sync`, then `emu kill`).
- **15e: PASS** on `Pixel_9a_bench`. Delta **−4.0 ms** (B 393.0, A 397.0), floor **4.0 ms**, threshold **100 ms**. All nine preconditions held, and the bench was the only emulator on all 37 monitor readings.
- **The sitting of record is attempt 2 on the 05:06 cold boot.** Attempt 1 was voided by precondition 3: the licences worktree's `depthSweep` Gradle worker ran at ≈100 % CPU from 05:11:16, 80 s into the attempt. It was re-attempted under the forward rule (off-device cause, state re-verified, launcher idle ≥ 3 min, its own null control). Attempt 1's numbers are not evidence. They also computed no FAIL (delta −6.0 ms), so the re-attempt hid no adverse result.
- The key `colorblind_palette_on` was absent at both ends (count 0), and `app/schemas` shows no diff. The bench data is byte-identical at start and end. The bench is down.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/qa-engineer-15e.md`: this report.
- Artifacts, git-ignored and never committed, under `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/qa/colour-vision/`. `smoke/` is 10 MB and `15e/` is 41 MB, 40 MB of which is the two APKs.

| Path | Size (B) | What it shows |
|---|---|---|
| `15e/armA-305f741.apk` | 21,318,269 | Arm A, `:app:assembleDebug` at `305f741`, sha256 `e869a977004cfe90def7f34653721fa8d6f4989566033f4874ee17043b185e1a` |
| `15e/armB-155045e.apk` | 21,351,953 | Arm B, `:app:assembleDebug` at `155045e`, sha256 `cf7696f676cfd9e235e4c5d202e8a9313d8374b1f79689ad6b38578c239b6a82`. Used for both the smoke and the sitting |
| `15e/build-armA.log`, `15e/build-armB.log` | 3,980 / 3,627 | Both `BUILD SUCCESSFUL` (04:31:47 and 04:32:00) |
| `smoke/state-before.txt`, `smoke/state-after.txt` | 658 / 658 | `ergoz_studio` before and after. They differ only in device uptime and `Data-Free` (−1.7 MB) |
| `smoke/1g-cold-settled.png` | 211,148 | Step 1: cold launch (COLD, 1642 ms) to Home. The board art is drawn, and the Challenge chip "Continue — Level 1" is in place |
| `smoke/1h-cold-with-save.png` | 202,865 | Step 1 again, with a Classic save (COLD, 855 ms): the "Continue Classic" chip has arrived above the tiles |
| `smoke/2b-settings-on.png` | 181,873 | Step 2: the Colorblind palette row comes directly after Haptics and above Play Games, switched on |
| `smoke/prefs-after-on.txt`, `prefs-after-off.txt` | 85 / 298 | `strings` of `app_prefs.preferences_pb`: `colorblind_palette_on` is written by the switch |
| `smoke/3c-e33-held.png`, `3c-zoom.png` | 280,001 / 73,418 | Step 3: English-33 with a held peg. The ring has its white inner band (halo); the target dot is opaque `(124,58,237)` |
| `smoke/4a-hint.png`, `4a-zoom.png` | 278,559 / 143,273 | Step 4: both hint rings amber `(180,83,9)`, dashed, 8 dashes each |
| `smoke/5a-dark-hint.png`, `5a-zoom.png` | 277,382 / 145,646 | Step 5: after `cmd uimode night yes` the activity was recreated (events log below). The board returns dark with the hint up and both rings dashed amber `(251,191,36)`, 8 dashes each |
| `smoke/6c-remove-anim-on.png`, `6o-remove-anim-off.png` | 102,093 / — | Step 6: Accessibility › Color and motion › Remove animations, on, then off |
| `smoke/6j-tut-done.png`, `6j-star-zoom.png`, `6k-tut-done-later.png` | 87,967 / 8,811 / — | Step 6: the tutorial's completion star at animator scale 0. Amber fill with a dark outline (the palette's earned look). It is settled: its 200×200 box is pixel-identical in a capture ≈1 s later |
| `smoke/7c-results.png`, `7c-results-later.png` | 84,037 / — | Step 7: Results after an English-33 win (Perfect!, 1 peg). The three stars are amber with an outline, pixel-identical ≈1 s later |
| `smoke/7d-catalogue.png`, `7d-pips-zoom.png` | 199,611 / 4,081 | Step 7: catalogue pips. English-33 "3 of 3 stars" in the palette's look |
| `smoke/8b-settings-off.png` | 182,320 | Step 8: palette switched off |
| `smoke/8c-e33-held-off.png`, `8c-zoom.png`, `8d-hint-off.png`, `8d-zoom.png` | 278,601 / — / 280,252 / — | Step 8: back on the board in the same process (pid 4255 throughout), standard marks: no halo, translucent dot `(188,158,241)`, solid amber hint ring `(245,166,35)` (360° amber, 0 gaps) |
| `smoke/logcat.txt` | 80,105 | The app's last 500 log lines (`--pid`). No FATAL or ANR. 10 `E` lines, all environment noise (hiddenapi, MESA, MediaCodec, WebView seed) |
| `smoke/s/`, `smoke/dumps/` | 2.2 MB / 696 KB | 700 px previews and uiautomator dumps of every capture. The unlisted captures (`1-…1e`, `3a`, `3b`, `6a`, `6b`, `6d`–`6i`, `6l`–`6n`, `7a`, `7b`, `8a`) are navigation. `1f-cold-early.png` is not evidence: a zsh quoting slip meant the relaunch it should show never ran |
| `smoke/a`, `shot.sh`, `crop.py`, `dashes.py`, `tut3.sh`, `play.py`, `e33-win.moves` | small | Session helpers (`play.py` and the moves are copied from `run2/`) |
| `15e/sitting/pre.txt` | 1,138 | P-1.2 `config.ini` and `.ini` diffs, and the host at 05:06 |
| `15e/boot.txt`, `15e/emulator.log` | — | Cold boot 05:06:11 → `sys.boot_completed` 05:06:31. Shut down 05:31:34 |
| `15e/bench-state-start.txt`, `bench-state-pre-attempt2.txt`, `bench-state-end.txt` | 961 / — / 962 | Bench settings, installed sha, `Data-Free`, accounts |
| `15e/db-start/`, `db-pre-attempt2/`, `db-end/` | 96 KB each | App data pulls. All three are byte-identical to `home-chip-arrival/…/device/db-start/` |
| `15e/sitting/attempt-1/` | — | **VOID** (precondition 3). `monitor.log`: 30 of 38 lines not quiet. `stats.txt`: 892 B |
| `15e/sitting/attempt-2/` | — | **Sitting of record.** `samples.csv` (100 launches, 3,805 B), `swaps.csv` (20 swaps, 5,260 B), `monitor.log` (37 readings, 2,923 B), `before.txt`, `after.txt`, `stats.txt` (889 B), `null.log`, `arms.log` |
| `15e/quiet-poll.log` | 4,789 | Host polls before each attempt |
| `15e/closing.txt` | — | End key count, end `uptime`, shutdown time |
| `15e/run.sh`, `sitting.sh`, `hostq.sh`, `quiet.sh`, `state.sh`, `snap.sh`, `stats.py` | small | Copied from home-chip's `sitting2/` and re-keyed to the two APKs. `run.sh` was changed to make one attempt per launch and to number attempts from an argument |

**Excerpts** (at most 20 lines):
```
smoke step 5 (events log): 04:59:32.755 wm_on_destroy_called [..MainActivity,performDestroy]
                           04:59:32.807 wm_on_create_called  [..MainActivity,performCreate]
smoke dashes: 4a-hint (277,1187) dashes=8; (537,1187) dashes=8; 5a-dark-hint (277,1187) dashes=8; (537,1187) dashes=8
smoke step 8: 8d-hint-off (277,1187) amber degrees=360 dashes=0; dot (188,158,241) vs on (124,58,237)
smoke logcat: grep -cE 'FATAL EXCEPTION|ANR in com.erman' -> 0
15e attempt-1 (VOID): 05:11:17 busy=[43284:27.8%:PegSolitarie-lic 52619:102.5%:domain] quiet=no
  52619: java -DRUN_SWEEP=true …PegSolitarie-lic/domain/build/tmp/depthSweep/work (started 05:11:16)
15e attempt-2:
N1: n=20 min=368 median=400.5 max=424 non-cold=0
N2: n=20 min=358 median=404.5 max=449 non-cold=0
A: n=20 min=373 median=397.0 max=409 non-cold=0
B: n=20 min=369 median=393.0 max=424 non-cold=0
floor=|400.5-404.5|=4.0 threshold=max(2*floor,100)=100.0
delta=median_B-median_A=393.0-397.0=-4.0 verdict=PASS
monitor not-quiet lines: 0 of 37
ART status on 20/20 swaps: arm64: [status=verify] [reason=cmdline] [primary-abi]
```

## Checks
- `git rev-parse HEAD` gives `155045e`. `git diff --quiet fddc8e8 HEAD -- . ':(exclude).planning'` exits 0 — pass.
- `git rev-parse develop` and `git merge-base develop HEAD` both give `305f741` (arm A) — pass.
- `git worktree add --detach ../PegSolitarie-armA-cv 305f741` and `… ../PegSolitarie-armB-cv 155045e`, with `local.properties` copied in. Then `./gradlew :app:assembleDebug` in each, with load 9.5 and 11.0 at the start (< 12) — pass (22 s and 8 s). Both worktrees were removed with `git worktree remove --force` at the end.
- `shasum -a 256` of both APKs — as in Outputs. The installed `base.apk` read back from each device matched on every install and swap.
- `git diff --stat 305f741 155045e -- app/schemas` — pass: empty.
- `git diff --name-only 305f741 155045e -- app/src/main` filtered to the start path lists `MainActivity.kt` and six `ui/theme/` files, so the sitting is owed, as ruled.
- Smoke on `ergoz_studio`:
  - I polled for the free flag from 04:29; it appeared at 04:55.
  - `emulator -avd ergoz_studio -port 5620 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass. `emu avd name` = `ergoz_studio`.
  - `adb -s emulator-5620 install -r -d --no-incremental armB-155045e.apk` — Success.
  - Steps 1–8 — PASS (table below).
  - Restore: `adb uninstall com.erman.pegsolitarie` — Success. The third-party packages are back to `com.topjohnwu.magisk` only. Then `adb shell sync` and `adb emu kill`.
- Sitting on `Pixel_9a_bench`:
  - `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass.
  - `state.sh` and `snap.sh save` + `sqlite3` give: ladder 200 Challenge rows, `SUM(stars)` 401, one Classic `resume_snapshot` row, and prefs byte-identical to home-chip's `device/db-start` — pass.
  - `run.sh <deadline>` gave attempt 1 VOID. `run.sh <deadline> 1` gave attempt 2 CLEAN, and `stats.py` gave PASS.
  - After the closing readings, `adb shell sync` and `adb emu kill`.
- Not run: the Check (`./gradlew check`). This is a device session; the Check belongs to the developer, the auditor and gsd. `.planning/log/` was not read.

## Smoke pass (tech-lead ruling, item 2; `ergoz_studio`, arm B `cf7696f6…6a82`)
Light theme, font scale 1.0, animator scale 1, except where a step says otherwise. The install was fresh, so first-run onboarding came first: Skip tutorial, the ad consent form ("Do not consent"), and the Play Games prompt ("Not now").

| # | Step | Evidence | Result |
|---|---|---|---|
| 1 | Cold launch: Home draws its boards and the chip arrives | `1g-cold-settled.png`: COLD, board art drawn, Challenge chip "Continue — Level 1". `1h-cold-with-save.png`: with a Classic save, the "Continue Classic" chip has arrived | PASS |
| 2 | Settings: the Colorblind row is after Haptics; switch it on | `2b-settings-on.png`, and the dump order Haptics y1048 < Colorblind y1301 < Play Games y1512. The prefs now hold `colorblind_palette_on` | PASS |
| 3 | English-33, hold a peg: halo, opaque target dots | `3c-zoom.png`: white inner band inside the violet ring; the target dot is solid `(124,58,237)` | PASS |
| 4 | Hint: both rings amber, dashed, 8 dashes each | `4a-zoom.png`. `dashes.py` counts 8 and 8; amber `(180,83,9)` | PASS |
| 5 | Dark mode with the hint up: recreates, dashed rings return, no crash | `5a-zoom.png`. The events log shows destroy/create at 04:59:32. 8 and 8 dashes, amber `(251,191,36)`. No FATAL. The one frame was not judged, as ruled | PASS |
| 6 | Animator scale 0, How to play to the completion star: settled, earned look | `6c-remove-anim-on.png` set the scales to `0.0 0.0 0.0`. In `6j-star-zoom.png` the star is amber with an outline and pixel-identical ≈1 s later. Scales set back to `1.0 1.0 1.0` (`6o`) | PASS |
| 7 | Results with an earned star in the palette's look; then the catalogue's pips | `7c-results.png`: three outlined amber stars, settled. `7d-pips-zoom.png`: English-33's pips in the same look | PASS |
| 8 | Palette off: the standard marks return without a restart | `8b`, then `8c`/`8d`: no halo, translucent dot `(188,158,241)`, a solid ring. pid 4255 is unchanged since step 3 | PASS |

The scale for step 6 was set with Accessibility › Remove animations. It sets all three scales, not only the animator scale, and I did not use `settings put global` (PROHIBITED). While navigating I opened Color correction by mistake and left it without changing anything: `accessibility_display_daltonizer_enabled` read `0` before and after.

**System changes on `ergoz_studio`, and their fate:**
- Arm B was installed, then uninstalled.
- Night mode went yes, then back to no.
- Remove animations went on, then off (the scales went 1.0 → 0.0 → 1.0).
- The AVD was booted by this session and shut down by it.
- `state-before.txt` and `state-after.txt` differ only in uptime and `Data-Free`. `development_settings_enabled` is still `1`, as the last session left it.

## 15e — the sitting (§6.5 field shape)
**PASS.** Attempt 2, `15e/sitting/attempt-2/`, 05:25:02–05:31:10 on 2026-11-01, on the cold boot of 05:06:11 (boot completed 05:06:31).

**Arms.**
- A = `305f741` = `git merge-base develop feature/colour-vision`; `develop` = `305f741`. APK `armA-305f741.apk`, sha256 `e869a977004cfe90def7f34653721fa8d6f4989566033f4874ee17043b185e1a`.
- B = `155045e` (code = `fddc8e8`). APK `armB-155045e.apk`, sha256 `cf7696f676cfd9e235e4c5d202e8a9313d8374b1f79689ad6b38578c239b6a82`.
- Both are `:app:assembleDebug` builds, each in its own detached worktree.

**Null control** (arm B's APK in both positions, blocks N1/N2 interleaved ×5, the first of each block discarded):

| Label | n kept | min | median | max | non-COLD | discarded (first of block) |
|---|---|---|---|---|---|---|
| N1 | 20 | 368 | **400.5** | 424 | 0 | 465, 449, 452, 452, 472 |
| N2 | 20 | 358 | **404.5** | 449 | 0 | 483, 504, 465, 507, 496 |

N1 kept: 409, 406, 416, 416, 403, 384, 396, 368, 409, 401, 413, 370, 400, 424, 388, 396, 395, 420, 392, 391. N2 kept: 449, 390, 404, 399, 413, 420, 405, 401, 411, 409, 409, 412, 373, 403, 401, 414, 401, 431, 402, 358.

**Arms** (A/B interleaved in blocks of five, the first of each block discarded, `am force-stop` + 2 s between samples):

| Arm | n kept | min | median | max | non-COLD | discarded |
|---|---|---|---|---|---|---|
| A `305f741` | 20 | 373 | **397.0** | 409 | 0 | 495, 485, 498, 422, 466 |
| B `155045e` | 20 | 369 | **393.0** | 424 | 0 | 485, 481, 505, 478, 482 |

A kept: 398, 394, 401, 407, 395, 396, 390, 398, 400, 409, 404, 404, 407, 389, 392, 390, 396, 406, 373, 380. B kept: 399, 398, 385, 397, 394, 392, 407, 387, 388, 369, 399, 384, 392, 385, 400, 399, 391, 424, 377, 399. (`am start -W` TotalTime, ms.)

**Floor and threshold.** floor = |400.5 − 404.5| = **4.0 ms**, far below the 150 ms VOID line. Threshold = max(2 × 4.0, 100) = **100 ms**. Delta = 393.0 − 397.0 = **−4.0 ms ≤ 100 ms → PASS.**

**Preconditions, with values:**
1. **Instrument.** `Pixel_9a_bench`, API 36 (`emu avd name`; `sdk: 36`); debug variant. OS animations `0.0/0.0/0.0` at the start. P-1.2: `config.ini` differs from `Pixel_9a.avd` only in `AvdId`, `avd.ini.displayname` and `disk.dataPartition.size` (6G → 16G). The `.ini` files differ only in `path` and `path.rel` (`sitting/pre.txt`). Not void.
2. **Exactly one emulator.** `pgrep -f qemu-system` = 1 (`Pixel_9a_bench`) on all 37 monitor readings and at both ends. `ergoz_studio` had been shut down at 05:05:57, and `adb devices` was empty before the boot. `Pixel_9a` and `Pixel_9` were never started.
3. **No concurrent Gradle.** 0 `GradleWrapperMain`, and no Gradle or Kotlin daemon or worker above 3 % CPU, on every attempt-2 reading (`monitor.log`: 0 of 37 not quiet).
4. **Load.** `uptime` before attempt 2 (launcher idle since 05:16:16, about 8 min 46 s): `1.54 2.75 3.40`. After (60 s after the last sample, app force-stopped): `1.35 2.04 2.83`; at the closing readings: `2.02 2.15 2.86`. All ≤ 5.0.
5. **ART.** `compile -m speed -f` after each swap. ART status `arm64: [status=verify] [reason=cmdline] [primary-abi]` on all 20 swaps, both arms alike.
6. **Component.** Resolved from the device in each block: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
7. **Swaps and schema.** Swaps only at block boundaries (`install -r -d --no-incremental`, compile, force-stop). The installed `base.apk` sha256 equalled its arm's on 20 of 20 swaps (`swaps.csv`). `git diff 305f741 155045e -- app/schemas` is empty. Brief §9's key ruling: `strings files/datastore/app_prefs.preferences_pb | grep -c colorblind_palette_on` printed **0** at the start (05:06, and again before attempt 2) and **0** at the end (05:31). The store checked is `files/datastore/app_prefs.preferences_pb`.
8. **Ladder.** Identical on both arms: 200 Challenge rows, `SUM(stars)` 401, `challenge_current_level` 111, one Classic save. The data is byte-identical to home-chip's recorded resting data at the start, before attempt 2 and at the end (`db-start`, `db-pre-attempt2`, `db-end`; four files each).
9. **Free space.** `Data-Free` 13 352 668K at the start, 12 849 108K before attempt 2, 12 828 280K at the end. All ≥ 4 GB.

**Attempt 1, voided (D-2's forward rule engaged).** It ran 05:09:56–05:16:16 on the same boot.
- At 05:11:16 a Gradle test worker from `/Users/ermanergoz/Desktop/PegSolitarie-lic/domain` (`-DRUN_SWEEP=true`, `depthSweep`) started at ≈100 % CPU. It was not this session's.
- The monitor read 30 of 38 lines not quiet, and `run.sh` declared the attempt NOT CLEAN on precondition 3. The cause was off the device: no crash, ANR, dialog, foreign install or settings change, and `Data-Free` stayed ≥ 4 GB.
- Before attempt 2, by the forward rule:
  - the host was quiet on four consecutive 10-second polls (05:23:20–05:23:50), with the worker gone;
  - the bench state was re-read (`bench-state-pre-attempt2.txt`: settings unchanged; installed sha is arm B's from attempt 1's last block);
  - the data is byte-identical to `db-start`, and the key count is 0;
  - the launcher was idle ≥ 3 min, `uptime` was read again, and attempt 2 ran its own null control.
- For the record only, attempt 1's `stats.txt` computed floor 14.0 and delta −6.0 (no FAIL). Those numbers are not evidence.

**Restore and shutdown (bench).**
- Arm B `cf7696f6…6a82` stays installed, as the sitting ends on it. The previous resting install was home-chip's arm B `3f66c5b6…`; candy's sitting installs its own arms.
- The app data is unchanged. Settings are unchanged: animations 0.0, font scale 1.0, light, gesture navigation, physical `wm size` and `wm density`, no accessibility service, 0 accounts.
- `adb shell sync`, then `adb -s emulator-5584 emu kill` at 05:31:34. No `qemu-system` process remained, and `adb devices` was empty.
- The bench was used for nothing but the sitting; the ruling's smoke fallback was not needed.

## Open issues / risks
1. **Concurrent Gradle work on a shared host voids sittings.** A licences `depthSweep` voided attempt 1, as the RM and home-chip attempts were voided before it. The producer may want P-4 (pause other sessions' builds) declared before candy's sitting. This costs nothing in verdict terms here.
2. **The bench's resting install changed** from home-chip's arm B to colour-vision's arm B (`cf7696f6…`). Candy's C-2/C-3 state check will see this sha. It is a sitting's own install, not a foreign one.
3. **Smoke scope:** this was one pass, not a matrix, as ruled. The one-frame fixes (`61e96db`, `fddc8e8`) were not device-judged; their evidence is the JVM pixel tests. CV-AC14 stays not-run (non-blocking), as before.
4. **Staffing:** Opus under Q-019, the same model as the developer (header).

## Handoff
Next: producer —
- Commit this report (done on the branch, `docs(14): record the colour-vision smoke and 15e sitting`).
- The tech-lead's sign-off can proceed. It cites this report for the smoke (Ruling 2) and the 15e sitting (brief §9), beside `qa-engineer.md`.
- Both AVDs are down, and the arm worktrees are removed.
- If `develop` gains a start-path commit before the merge, the sitting is owed again (brief §9).

VERDICT: PASS (smoke: PASS; 15e: PASS)
