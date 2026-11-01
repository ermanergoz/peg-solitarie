# qa-engineer report — home-chip-arrival (session S: the 15e re-sit, 15v, 15w, the bench 29 read)
Date: 2026-10-31
Branch: bugfix/home-chip-arrival (report only; the producer commits). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-home`, head `9b23df5`. Arm B `5708d95`, arm A `f7dd1fe`.
Skills loaded: ergoz-soft:standards (role qa-engineer; the product snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; `.claude/rules/lessons.md`), android-emulator-skill, android-testing
Ran in parallel with: none (P-4 declared by the producer; host checked, see Checks)
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** `agents/qa-engineer.md` pins fable at high. Founder answer Q-019 (option 3, re-confirmed) puts every Fable-pinned role on Opus until 3.0.0 is published, and the code was also written on Opus. Mitigation: every verdict below rests on a device artifact listed here.
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot; no `.planning/recall/RECALL.md`)
Authority: `tech-lead-defect-ruling.md` §4.3 and §4.4 (session S), `tech-lead-device-ruling.md` C-1 to C-10 re-keyed to arm B `5708d95`.
Inputs read in full: `tech-lead-defect-ruling.md`, `tech-lead-device-ruling.md`, `qa-engineer.md`, `qa-engineer-device.md`, `qa-engineer-device2.md`, `tech-lead-brief.md`; `docs/ux-spec.md` §6.5 (preconditions, protocol, verdict), criteria 15e, 15v, 15w, 29.

## Summary
- **15e PASS** on `Pixel_9a_bench`, first attempt: delta **25.5 ms** (B `5708d95` 406.5, A `f7dd1fe` 381.0), floor **18.5 ms**, threshold 100 ms. All nine preconditions held; the host was quiet and the bench the only emulator on all 37 readings.
- **15v PASS**, all eight recordings (one save and two; animations off and on; native and 360×640 dp), parts 1, 2 (with the closing chip's shadow) and 4. Part 3 was not exercised: in every recording the chips landed after the cascade had settled.
- **15w (device half) PASS**, light and dark. **29 (2) dark three-button Home PASS**: scrim L 0.010, buttons 17.36:1, and no light-navigation flag on the app's window.
- **Found and fixed before the sitting:** the bench had lost its Classic save. The last session's restore had not reached the disk before the bench was killed. I restored it from the recorded resting data, and this session ran `sync` before `emu kill` (Open issue 1).
- The bench was restored and shut down (22:04:36). **VERDICT: PASS.**

## 15e — the re-sit (§6.5 field shape)
**PASS** — one attempt, `sitting2/sitting/attempt-1/`, 21:41:52–21:48:03 on 2026-10-31, on the cold boot of 21:36:47.

**Arms.** A = `f7dd1fe` = `git merge-base develop HEAD` (develop is `8047549`); APK `device2/armA-f7dd1fe.apk`, sha256 `8794ff4543d151b17909bc932bd40e96924e95b42e3bf1d3db48e62a66eca3f7`. B = `5708d95`; APK `armB-5708d95.apk`, sha256 `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`. Neither was rebuilt. Merge-in: none owed — `git diff --stat f7dd1fe develop -- . ':!.planning' ':!docs' ':!CLAUDE.md' ':!AGENTS.md'` lists only `scripts/play-upload.sh`, not an APK input.

**Null control** (arm B's APK in both positions, blocks N1/N2 interleaved ×5, first of each block discarded):

| Label | n kept | min | median | max | non-COLD | discarded (first of block) |
|---|---|---|---|---|---|---|
| N1 | 20 | 370 | **396.5** | 488 | 0 | 572, 490, 485, 504, 518 |
| N2 | 20 | 373 | **415.0** | 466 | 0 | 495, 499, 472, 492, 465 |

N1 kept: 488, 464, 402, 423, 370, 388, 391, 391, 381, 405, 392, 406, 405, 387, 387, 384, 401, 379, 423, 413. N2 kept: 459, 411, 417, 416, 428, 384, 392, 446, 466, 423, 384, 438, 398, 420, 373, 411, 414, 428, 408, 394.

**Arms** (A/B interleaved in blocks of five, first of each block discarded, `am force-stop` + 2 s between samples):

| Arm | n kept | min | median | max | non-COLD | discarded |
|---|---|---|---|---|---|---|
| A `f7dd1fe` | 20 | 339 | **381.0** | 408 | 0 | 451, 463, 473, 465, 456 |
| B `5708d95` | 20 | 372 | **406.5** | 447 | 0 | 457, 481, 499, 508, 502 |

A kept: 407, 401, 396, 339, 368, 408, 391, 376, 367, 368, 397, 362, 386, 372, 371, 388, 356, 395, 376, 388. B kept: 412, 396, 383, 407, 409, 416, 424, 447, 430, 437, 405, 403, 441, 406, 391, 372, 399, 384, 395, 420. (`am start -W` TotalTime, ms.)

**Floor and threshold.** floor = |396.5 − 415.0| = **18.5 ms** (VOID line 150 ms: not void). Threshold = max(2 × 18.5, 100) = **100 ms**. Delta = 406.5 − 381.0 = **25.5 ms ≤ 100 ms → PASS.**

**Preconditions, with values:**
1. Bench `Pixel_9a_bench`, API 36 (`emu avd name`, `getprop ro.build.version.sdk` = 36); debug variant (both APKs are `assembleDebug` outputs); OS animations 0/0/0 at the start of the sitting. P-1.2: `config.ini` differs from `Pixel_9a.avd` only in `AvdId`, `avd.ini.displayname`, `disk.dataPartition.size` (6G → 16G); the `.ini` files only in `path` and `path.rel` (`sitting2/sitting/pre.txt`). Not void.
2. Exactly one emulator: `pgrep -f qemu-system` = 1 (`Pixel_9a_bench`) on all 37 monitor readings (10 s apart) and at both ends; `adb devices` empty before the boot.
3. No concurrent Gradle: 0 `GradleWrapperMain` on every reading; three idle JVMs left from earlier sessions (a Kotlin daemon from 19:42, a Gradle 9.5.1 daemon from 20:01 and its worker from 20:02) read 0.0 % CPU and never crossed 3 % (`monitor.log`: 0 of 37 lines not quiet).
4. `uptime` before (launcher idle from 21:37:4x, 4 min 10 s before the first sample): `2.66 2.68 3.46`; after (60 s after the last sample, app force-stopped): `1.82 2.36 3.03`. Both ≤ 5.0 (10 logical cores ÷ 2).
5. `compile -m speed -f` after each swap; ART status `arm64: [status=verify] [reason=cmdline]` on all 20 swaps, both arms alike.
6. Component resolved from the device per block: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
7. Swaps only at block boundaries (`install -r -d --no-incremental`, compile, force-stop); installed `base.apk` sha256 equal to the arm's on 20/20 swaps (`swaps.csv`). Schema: `git diff f7dd1fe 5708d95 -- app/schemas` empty; no `PreferencesKey` line in `git diff f7dd1fe 5708d95 -- app/src/main`.
8. Ladder identical on both arms: 200 Challenge rows, `SUM(stars)` 401, `challenge_current_level` 111, one Classic save (re-established before the sitting; see Open issue 1).
9. `Data-Free` 13 568 964K at the start, 13 349 188K at the end; both ≥ 4 GB.

**D-2's forward rule:** not engaged; this was the first attempt on the boot.

## 15v — eight recordings
All eight on arm B `3f66c5b6…` (read back from `base.apk` into each `conditions.txt`), in gesture navigation and light mode. Each is a cold start from the launcher, recorded with `screenrecord` and read frame by frame from first paint until the chips had settled. Each recording passed the host check before and after it: no Gradle wrapper, no busy daemon, exactly one emulator, load < 5. One save = the recorded resting data (`device/db-start`); two saves = `device/two-saves` (adds a Challenge save on level 111). The animation scales were only read in the driver; I set them through Settings → Accessibility → Color & motion → Remove animations, never with `settings put global`.

| Recording | Frames | First Home frame | Chips arrive | Result |
|---|---|---|---|---|
| nat-1save-off | 28 | 18 (2.005 s) | frame 23, one step | PASS |
| nat-2save-off | 22 | 14 (2.006 s) | frame 18, both chips, one step | PASS |
| s360-1save-off | 26 | 16 (2.090 s) | frame 18, one step | PASS |
| s360-2save-off | 22 | 14 (1.977 s) | frame 18, one step | PASS |
| nat-1save-on | 121 | 47 (3.059 s) | frames 59–61 | PASS |
| nat-2save-on | 135 | 47 (3.194 s) | frames 55–61 | PASS |
| s360-1save-on | 135 | 52 (3.436 s) | frames 58–62 | PASS |
| s360-2save-on | 144 | 49 (2.961 s) | frames 62–67 | PASS |

**Part 1 (off, the position read): PASS on all four.** The title, the gear and both tiles were compared, box by box, with the recording's last frame (`crop.txt`). The 99th-percentile difference is ≤ 5 in the first two Home frames, which is encoder settling, and **0** in every later frame, the chip-arrival frame included. Profile matching gives dy = 0 with residual 0 for the title, the gear and both tiles in every Home frame (`report2.txt`). The ribbon moves only upward, by the chips' room, and only in the arrival frame: on nat-1save, top +253 px before and 0 from frame 23.

**Part 2 (on, the motion read): PASS on all four.**
- **Two motions only.** First, the entrance cascade (bands fading and sliding in from first paint). Second, the chips' arrival: the room opens, the stage shrinks, the ribbon glides up and the chip fades in (`nat-2save-on/strip-firstpaint.png`, frames 46–61).
- **Nothing else moves.** From the first frame after the cascade, the title, the gear and both tiles differ from the last frame by p99 ≤ 5 through the whole arrival: nat-1save-on from frame 57, nat-2save-on from 52, s360-1save-on from 53, s360-2save-on from 60.
- **The ribbon's glide** is monotonic upward, never past its target: s360-2save-on +256 → +120 → +44 → +28 → +2 → 0 px.
- **Settled frames equal the off recordings.** I matched each element of the on recording's settled screencap against the off one's, at dy −6..+6 (`15v/settled-off-vs-on.txt`). Every element — title, gear, ribbon, each chip, both tiles — gives **dy = 0** in all four pairs, with mean absolute difference 0.00–0.04; the gear reads 0.9–1.0 because its translucent circle shows the drifting blob.
- **The closing chip's shadow (auditor finding 4): PASS by eye.** `15v/shadow-check.png` stretches the contrast of nat-1save-on frames 58–64 and nat-2save-on frames 56–61. While the room opens, the chip's soft shadow is present along its left side and its top, and only its bottom is cut, by the bottom-only clip. The settle frame shows the same shadow with the bottom added. Nothing pops at the sides or the top.

**Part 3: not exercised.** In all four on recordings the cascade had settled before the room started to open (nat-1save-on: cascade frames 47–57, room 59–61). No band restarted its entrance.

**Part 4: PASS on all eight.** In every frame that draws the stage slab, its pegs are drawn at the same alpha. Example: nat-2save-on frame 47, the first paint, shows the slab and the pegs faded together (`report2.txt` stage slab 8, peg 95). In every frame where tile 1's surface is drawn (surf ≥ 15), its art is drawn (art 82–157). The frames with surf 4–5 are before the tile's entrance; there both read near zero, or rise together (nat-1save-on 47–48: 4–5 against 35–37). In s360-2save the stage is below the art minimum with both chips (no slab art, F-25), as designed.

**O-1 (observation, not a criterion failure): the device drops frames during the cascade.** In nat-1save-on, frames 51–56 (3.122–3.358 s, about 236 ms) are identical, and frame 57 jumps to the cascade's end. s360-2save-on does the same in frames 53–59, and s360-1save-on's cascade was not captured at all: its first Home frame is already settled. The whole app froze, blobs included, so the device's renderer stalled; it is not a layout move. The bench has 2 GB of RAM, and `top` during nat-1save-on read swap at 1.45 of 1.53 GB with 93 MB free. The host was quiet. No band ended anywhere but its settled place.

## 15w — device half
**PASS, light and dark.** The bench was set to a 360×640 dp window (`wm size 1080x1920`, `wm density 480`) and font scale 2.0 (`settings put system font_scale 2.0`), with one save, and Home was cold-launched in each mode. "Stars", "Boards" and "Level" each read whole on one line, with the values 401, 0/5 and 111 under them (`15w/15w-ribbons.png`). The pills are stacked (label over value, no glyph) at one height and one width: the bounds are `[24,387][352,671]`, `[376,387][704,671]` and `[728,387][1056,671]` in both modes (`15w-360-fs2-night-{no,yes}.xml`). Restored: font scale 1.0, `wm size`/`wm density` reset.

## 29 (2) — one dark three-button read on the bench
**PASS.** API 36.1 bench, dark mode, three-button navigation, Home cold-launched:
- **Bar:** scrim `(26,25,37)`, L **0.010** (≤ 0.18), sampled at four points between and beside the buttons; buttons `(255,255,255)` at **17.36:1** (≥ 3:1); status-bar icons white on `(27,22,38)`, 17.7:1 (`29/analysis.txt`). These are the same pixels session D read on `Pixel_9_clean`.
- **Flags:** `dumpsys window` (`29/threebutton-night_yes-home-dumpsys-window.txt`) shows `FORCE_LIGHT` 0 times. The app's window carries no `vsysui`/`apr` line, so its appearance is empty. `LIGHT_NAVIGATION_BAR` appears six times, all on three Settings windows left in the back stack from the light-mode Remove-animations toggle (`SubSettings`, `AccessibilitySettingsActivity`, `Settings`), not on the app's window.

Restored: gesture navigation, light mode.

## Outputs
- This report. Session artifacts (git-ignored) under `.planning/qa/home-chip-arrival/sitting2/`, 245 MB:
  - `sitting/pre.txt` (≈2 KB): P-1.2 `config.ini` and `.ini` diffs, host state before the boot, idle JVMs, `adb devices`, the launcher-idle and run-start times.
  - `sitting/attempt-1/` (the sitting of record): `samples.csv` (100 launches, ≈5 KB), `swaps.csv` (20 swaps with sha, ART status and host line, ≈5 KB), `monitor.log` (37 host readings, ≈3 KB), `before.txt`, `after.txt`, `stats.txt`, `null.log`, `arms.log`. `quiet-poll.log`: the pre-attempt host polls.
  - `boot.txt`, `emulator.log`: the cold boot (21:36:19 → `sys.boot_completed=1` 21:36:47) and the shutdown (22:04:36).
  - `bench-state-start.txt`, `bench-state-end.txt` (≈1 KB each): the bench's settings, installed sha256, `Data-Free`, accounts.
  - `db-start/` (the data as found, Classic save missing), `db-reseeded/` (after the restore; its `.db` was later checkpointed by a host `sqlite3` read, so it is not byte-for-byte the device's), `db-end/` (pulled after the final restore; `.db`, `.db-wal` and prefs byte-identical to `device/db-start`).
  - `15v/<window>-<n>save-<off|on>/` × 8 (196 MB in all): `<tag>.mp4`, `frames/`, `pts.txt`, `launch.txt`, `settled.png`, `ui.xml`, `conditions.txt`, `report.txt` (the old `an15v.py`), `report2.txt` (`an15v2.py`), `crop.txt` (`crop15v.py`), `sheet-*.png`; `nat-2save-on/strip-firstpaint.png` and `s360-2save-on/strip-firstpaint.png` (the first 16 Home frames). `15v/settled-off-vs-on.txt` (`settledcmp.py`), `15v/shadow-check.png` (finding 4), `15v/<pair>-diff-off-vs-on-settled.png` and `settled-diff-sheet.png`, `15v/settled-edges.txt` (`edge15v.py`, superseded by `settledcmp.py`: blob-sensitive).
  - `15v-void/` (47 MB), not evidence. `nat/`, `s360/`: four off recordings that zsh passed with fused arguments, made in the wrong window and state. `nat-1save-on-loaded-host/`: the first on recording, after which the host read load 5.34, so it was re-made.
  - `15w/15w-360-fs2-night-{no,yes}.png` (≈200 KB) and `.xml`, `15w-ribbons.png`.
  - `29/threebutton-night_yes-home.png` (≈200 KB), `…-dumpsys-window.txt`, `analysis.txt`.
  - `a11y-motion-before.png`, `a11y-motion-animations-on.png`, `a11y-motion-restored.png`: the Remove-animations switch at each step. `dev-1.png`: a stray capture (the app, not Developer options).
  - Helpers: `state.sh`, `quiet.sh`, `snap.sh`, `stats.py`, `record.sh` (copied); `hostq.sh`, `sitting.sh`, `run.sh` (re-keyed to `emulator-5584`, arms `f7dd1fe`/`5708d95`, one-emulator test); `rec1.sh` (scales read-only, waits for a quiet host); `an15v.py`, `bars_an.py`, `launch.sh` (copied); `an15v2.py`, `crop15v.py`, `settle15v.py`, `edge15v.py`, `settledcmp.py`, `tapt.sh` (new).
- **Log excerpts** (at most 20 lines):
  ```
  N1: n=20 min=370 median=396.5 max=488 non-cold=0
  N2: n=20 min=373 median=415.0 max=466 non-cold=0
  A: n=20 min=339 median=381.0 max=408 non-cold=0
  B: n=20 min=372 median=406.5 max=447 non-cold=0
  floor=|396.5-415.0|=18.5 threshold=max(2*floor,100)=100.0
  delta=median_B-median_A=406.5-381.0=25.5 verdict=PASS
  monitor not-quiet lines: 0 of 37
  null,1,N1,3f66c5b6…,3f66c5b6…,yes,"arm64: [status=verify] [reason=cmdline] [primary-abi]","21:41:52 load1=2.66 … quiet=yes",21:41:53
  arms,10,B,3f66c5b6…,3f66c5b6…,yes,"arm64: [status=verify] [reason=cmdline] [primary-abi]","21:46:48 load1=2.51 … quiet=yes",21:46:48
  nat-1save-off vs nat-1save-on: title dy=+0 …; ribbon(Stars) dy=+0 …; chipC dy=+0 …; tile1 dy=+0 (mad 0.00); tile2 dy=+0 (mad 0.02)
  threebutton-night_yes-home: scrim (26, 25, 37) L=0.010 …; button extreme (255, 255, 255) 17.36:1
  (bench, as found) databases/peg_solitaire.db-wal 0 bytes, peg_solitaire.db-shm 0 bytes, dated 2026-10-24 19:21; resume_snapshot 0 rows
  ```

## Checks
- `git rev-parse HEAD` — `9b23df5`; `git status --short` — clean.
- `git merge-base develop HEAD` — `f7dd1fe` (arm A); `develop` = `8047549`.
- `git diff --stat 5708d95 HEAD -- app/src/main` — pass: empty. `-- . ':!.planning'` lists only `BoardCanvasFirstFrameTest.kt` and `HomeChipArrivalTest.kt` (the C11 test-only fix), so the sitting stands on `5708d95`'s APK (C-9).
- `git diff --stat f7dd1fe develop -- . ':!.planning' ':!docs' ':!CLAUDE.md' ':!AGENTS.md'` — only `scripts/play-upload.sh`: no merge-in owed.
- `git diff --stat f7dd1fe 5708d95 -- app/schemas` — pass: empty. `git diff f7dd1fe 5708d95 -- app/src/main | grep -c PreferencesKey` — 0.
- `git diff --name-only f7dd1fe 5708d95 -- app/src/main` — the brief's files plus the v35 pair, no v27 pair. Listed for the record; I did not review it.
- `shasum -a 256 armB-5708d95.apk` — pass: `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`.
- `shasum -a 256 device2/armA-f7dd1fe.apk` — `8794ff4543d151b17909bc932bd40e96924e95b42e3bf1d3db48e62a66eca3f7`. `qa-engineer-device2.md` records only `8794ff45…ca3f7`, not the full hash the dispatch mentions. The prefix and suffix match, and the file's date (2026-10-24 23:33) is session D's build time. Not rebuilt.
- Quiet host before the sitting, at 21:34:58 and 21:36:19:
  - `ps` for emulators: 0 `qemu-system`, only two 13-day-old `crashpad_handler`s; `adb devices` empty.
  - `ps` for Gradle/Kotlin: no `GradleWrapperMain`. Three idle JVMs: Kotlin daemon pid 55944 (since 19:42), Gradle 9.5.1 daemon 85888 (since 20:01) and its worker 87878 (since 20:02), all at 0.0 % CPU.
  - Load 3.71: Spotlight's `mds_stores` at ≈100 % CPU.
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass: `emu avd name` = `Pixel_9a_bench`, SDK 36.
- `state.sh emulator-5584` (C-2/C-3) — installed `base.apk` was `9d507a88…2decaa`, the old arm B `7448713`, as the bench was left on 2026-10-24. `Accounts: 0`. The only settings difference from `device/bench-state-end.txt` is `Data-Free`.
- `snap.sh save` + `sqlite3` (C-3) — ladder 200 rows, `SUM(stars)` 401; prefs byte-identical to `device/db-start` (`challenge_current_level` 111); **`resume_snapshot` 0 rows: the Classic save was missing** (Open issue 1). After `snap.sh restore device/db-start` + `sync`: `classic` row present, its `jsonBlob` sha256 equal to `device/db-start`'s (`f9b3daf8…`).
- `run.sh` → `sitting.sh null`, `sitting.sh arms`, `stats.py` — **PASS** (15e section).
- `rec1.sh` ×8, `an15v.py`, `an15v2.py`, `crop15v.py`, `settledcmp.py` — 15v as above.
- Settings → Accessibility → Color & motion → Remove animations: off gave `1.0 1.0 1.0`, on gave `0.0 0.0 0.0` (`settings get global`, read only).
- 15w: `wm size 1080x1920`, `wm density 480`, `settings put system font_scale 2.0`, `cmd uimode night no|yes`, `uiautomator dump` — PASS.
- 29: `cmd overlay enable-exclusive --category …navbar.threebutton`, `dumpsys window`, `bars_an.py` — PASS.
- Not run: the Check (`./gradlew check`) or any Gradle build. This is a device session; the Check is the developer's and the auditor's gate, and the dispatch forbade a build. TalkBack was not used (18g/26b were session D's). `.planning/log/` was not read.

## Restore record (C-8)
Final state, from `bench-state-end.txt` at 22:04:13:
- arm B `3f66c5b6…21332f7` installed, versionName 3.0.0;
- animations 0/0/0 (stored as `0.0`, written by the Remove-animations switch; it was `0` before);
- font scale 1.0; light mode; gesture navigation;
- `wm size` 1080×2424 and `wm density` 420 (physical, no override);
- TalkBack off (`enabled_accessibility_services` null, `accessibility_enabled` 0); `Accounts: 0`;
- `Data-Free` 13 351 672K;
- the ladder and the one Classic save restored from `device/db-start`. The pull after the restore is byte-identical, `.db`, `.db-wal` and prefs.

Every system change in this session, and what happened to it:
- Arms A and B installed per block; the sitting ended on arm B.
- Remove animations off, then on again: the scales went 0 → 1.0 → 0.0.
- `wm size`/`wm density` 360×640 dp: reset.
- `font_scale` 2.0: back to 1.0.
- `cmd uimode night yes`: back to `no`.
- Navigation overlay to three-button: back to `gestural`.
- The app data was changed by the 15v snapshots: restored.
- Three Settings activities stayed in recents; the shutdown cleared them (no snapshot is saved).

`adb shell sync`, then `adb -s emulator-5584 emu kill` at 22:04:36. No `qemu-system` process remained, and `adb devices` was empty. I touched no other AVD and not the GM6.

## Open issues / risks
1. **The bench's Classic save was lost between sessions (process finding; retro).** As found: `databases/peg_solitaire.db-wal` and `-shm` were 0 bytes, dated 2026-10-24 19:21, and `resume_snapshot` had 0 rows. The main `.db` was byte-identical to `device/db-start/`. Session 1 restored `db-start/` at C-8, and its `db-end/` pull at 19:21 still had the 8 272-byte WAL; it then ran `emu kill` at 19:21:27. The likely cause is that the writes never reached the disk image before the kill. I re-established precondition 8 from the recorded resting data before the sitting. This session ran `adb shell sync` before `emu kill`. **Proposed for the retro (R-3d's process lesson):** every bench restore ends with `adb shell sync` before `emu kill`, and the next session's C-3 check reads `resume_snapshot`, not only the ladder.
2. **O-1, dropped frames during the cascade on the bench.** The 2 GB AVD swaps (1.45 of 1.53 GB). The stalls did not hide part 4 (first-paint frames were captured in three of the four on recordings) or part 2's settled read. They are why s360-1save-on shows no cascade frames. This is the instrument's limit, not a product defect; I note it for the tech-lead.
3. **15v part 3 was not exercised.** No recording had the chips land during the cascade. That case stays covered on the JVM (15u) and by its design; the device did not produce it.
4. **`settings put global` was avoided** (the role's PROHIBITED list). The animation scales were switched through Accessibility → Remove animations, which writes `0.0` where the bench had `0`. The meaning is the same; the string differs from `bench-state-after.txt`.
5. **Staffing:** Opus under Q-019, the same model as the developer (header).

## Handoff
Next: producer —
- Commit this report alone (`docs(15): record the home session S bench checks (PASS)`).
- The tech-lead's sign-off (`tech-lead-defect-ruling.md` §4.5) can now proceed: it cites this report for 15e, 15v, 15w and the bench 29 read, and `qa-engineer-device2.md` for 18g, 26b and 29's matrix. It may note O-1 and Open issue 3.
- For the 3.0.0 retro: Open issue 1 (the `sync` before `emu kill`), with R-3's bench findings.
- The bench is down; the producer may release P-4.

VERDICT: PASS
