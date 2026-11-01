# qa-engineer report — home-chip-arrival (session D: the loop-2 fixes for B-1, B-1b, B-2, B-3, off the bench)
Date: 2026-10-31 (started 2026-10-24; resumed after the usage-limit reset)
Branch: bugfix/home-chip-arrival (report only; the producer commits). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-home`, head `278c9ef`. Arm B is `5708d95`. `git diff --stat 5708d95 HEAD -- app/src/main` is empty: the commits since then are a test-only change and docs.
Skills loaded: ergoz-soft:standards (role qa-engineer; the product snapshot is 0.5.1/0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md`), android-emulator-skill, android-testing
Ran in parallel with: the home C11 test-fix developer, colour-vision QA on `ergoz_studio` (`emulator-5620`, up during both API 36 runs), talkback-board developer loop 3, and other branches' Gradle builds
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** `agents/qa-engineer.md` pins fable at high. The Fable limit is reached, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The code was also written on Opus. Mitigation: every verdict below rests on a device artifact listed here. Every TalkBack verdict also rests on TalkBack's own VERBOSE log (`SpeechControllerImpl: Speaking fragment`), which gives each utterance with a timestamp.
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot; no `.planning/recall/RECALL.md`)
Inputs read in full: `tech-lead-defect-ruling.md`, `qa-engineer-device.md`, `code-auditor-loop2.md`; `docs/ux-spec.md` criteria 18g, 26b and 29

## Summary
- **B-1 is fixed on API 36.** In dark three-button mode the bar scrim is `(26,25,37)`, L 0.010, with white buttons at 17.36:1. Neither `LIGHT_NAVIGATION_BARS` nor `FORCE_LIGHT_NAVIGATION_BARS` appears anywhere in `dumpsys window`, on Home, gameplay or the tutorial. Arm A `f7dd1fe`, read on the same device, still gives `(205,204,208)` with `FORCE_LIGHT_NAVIGATION_BARS`.
- **B-1b is confirmed on a device and is fixed.** On API 27, arm B's bar matches arm A's pixel for pixel in both modes: opaque black, light buttons at 6.49:1. The old arm B `7448713` shows the defect: in light mode it sets `LIGHT_NAVIGATION_BAR` (`vsysui=0x2610`) and draws dark buttons on black, at 1.10:1.
- **B-2 is fixed (18g a, b, c, light and dark).** Focus speaks the whole-minute sentence once. Up to 42 s of rest bring no repeat. A rest across a minute boundary brings exactly one more utterance, the new minute.
- **B-3 is fixed (26b).** On Classic and Challenge, light and dark, the order is: back → title → timer → Undo → Hint → Show moves → Restart. Home and the tutorial read as before.
- **VERDICT: PASS.** No bug was filed. One observation (O-1) and the I4 glance are recorded below. Neither is a defect.

## Per-item results
| § 4.4 item | Case | Result | Evidence (under `.planning/qa/home-chip-arrival/device2/`) |
|---|---|---|---|
| 0 | APK identity | **held** | `armB-5708d95.apk` file sha256 `3f66c5b6…21332f7`. The installed `base.apk` gave the same hash on API 36 (twice) and on API 27. |
| 0 | TTS health | **pass** | `tb-tts-health.txt`: "Sucessfully bound to com.google.android.tts", "Connected to TTS engine" (Google TTS `20260914.02_p2`), TalkBack 16.0.0.738667889 bound with FEEDBACK_SPOKEN. The speech overlay shows (`tb-on.png`). |
| 1 · 29 (2) | Three-button, **dark**, Home / gameplay / tutorial | **PASS** | `29-api36/threebutton-night_yes-*.png`. Scrim `(26,25,37)` L **0.010** (≤ 0.18); buttons `(255,255,255)` **17.36:1** (≥ 3:1). `*-dumpsys-window.txt`: `LIGHT_NAVIGATION_BAR` 0 hits, `FORCE_LIGHT` 0 hits in all three files; no `mLastAppearance` line, so the appearance is empty. |
| 1 · 29 (2) | Three-button, light, all three screens | **PASS** | Scrim `(254,254,255)` L 0.992; buttons `(102,102,102)` 5.70:1. `mLastAppearance=LIGHT_STATUS_BARS LIGHT_NAVIGATION_BARS`. See O-1. |
| 1 · 29 (2) | Gesture handle, light / dark, all three screens | **PASS** | Light: handle `(98,96,100)`, 5.57:1 on game and tutorial, 4.93:1 on Home's tinted art. Dark: `(237,237,238)`, 15.09:1, and 12.93:1 on Home. |
| 1 · 29 (2) | Status-bar icons | **PASS (as H-2 left them)** | Dark icons on light (`(93,89,100)`–`(98,96,100)`, 5.6–6.1:1; arm A identical at 6.1:1). White on dark, 17.7:1. Light status-bar flag present in light mode and absent in dark. |
| 1 · 29 (2) | Arm A `f7dd1fe` on the same device | reference | `29-api36-armA/`: dark `(205,204,208)` L 0.607, buttons 3.59:1, `mLastAppearance=LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS`. Light `(249,248,250)`, 5.42:1. This is B-1 as found, so the fix is the difference. |
| 1 · I4 | Dark three-button cold launch, starting window | **glance: no defect** | `i4/i4-{1,2}.mp4`, `i4/sheet-{1,2}.png`, `i4/full/threebutton-night_yes-i4-f33.png`. The starting window is near-white `(247,247,247)` for about 2 s (accepted by the ruling's rejection of option (a)). Over it the bar is light with **dark** buttons, `(97,97,97)` at 5.78:1, so it is legible. The dark Home then fades in with the dark bar. No light-on-light frame. |
| 2 · 29 below 35 | API 27, three-button, **light**, Home + gameplay | **PASS** | `29-api27/armB-night_no-{home,game}.png`: black bar, light buttons 6.49:1, `vsysui=0x2600` (no `0x10`). The bar region is **pixel-identical to arm A** (`analysis.txt`: `identical A vs B: True`). |
| 2 · 29 below 35 | API 27, three-button, **dark**, Home + gameplay | **PASS** | `29-api27/armB-night_yes-*`: same black bar and light buttons, `vsysui=0x600`; pixel-identical to arm A. |
| 2 · B-1b record | Old arm B `7448713`, API 27, light | **B-1b confirmed (fixed in arm B)** | `29-api27/oldB7448713-night_no-*`: `vsysui=0x2610` (`LIGHT_NAVIGATION_BAR`), dark buttons `(16,16,16)` on black, **1.10:1**. Dark mode is unaffected (`0x600`, 6.49:1). |
| 3 · 18g (a) | Focus moves to the running timer | **PASS** (light and dark) | Light 20:05:39.591 "Elapsed time, under a minute" (pill 00:10). Dark 20:14:07.739 "Elapsed time, under a minute" (pill 00:05). Light at 01:50: "Elapsed time, 1 minute". Classic resume at 02:26 (26b walk): "Elapsed time, 2 minutes". |
| 3 · 18g (b) | 10 s of rest within one minute | **PASS** (light and dark) | Light: no utterance from 20:05:39.6 to the end of the 13 s window (`18g/light-ab/`, `18g/logcat-light-ab.txt`). Dark: none from 20:14:07.7 until the next deliberate refocus at 20:14:49.6, which is **42 s** of rest (`18g/dark-ab/`, `18g/logcat-dark.txt`). |
| 3 · 18g (c) | Rest across a minute boundary | **PASS** (light and dark) | **Dark, focus at 00:50:** "under a minute" at 20:14:52.196, then **one** "Elapsed time, 1 minute" at 20:15:01.939, then nothing to 01:13 (`18g/dark-c/`). **Light, focus at 01:50:** "1 minute" at 20:07:19.722, then one "Elapsed time, 2 minutes" at 20:07:28.959, then nothing to about 02:13 (`18g/light-c/`). |
| 4 · 26b | Gameplay, **Classic**, light | **PASS** | `26b/classic-light/` and its speech log: Navigate back, Button → Peg Solitaire, Heading → **Elapsed time, under a minute** → Undo, Button. disabled → Hint, Button → On, Show possible moves, Switch → Restart Board, Button → end of list → wraps to Navigate back. |
| 4 · 26b | Gameplay, **Classic**, dark | **PASS** | `26b/classic-dark/`, 20:13:02–20:13:31: the same order, with the timer saying "Elapsed time, 2 minutes" at 02:26. |
| 4 · 26b | Gameplay, **Challenge**, light | **PASS** | `26b/challenge-light/`, 20:08:12–20:08:31: Navigate back → "Challenge level 25", Heading → Elapsed time, under a minute → Undo → Hint → Show possible moves → Restart Board → wraps. |
| 4 · 26b | Gameplay, **Challenge**, dark | **PASS** | `26b/challenge-dark/`, 20:16:19–20:16:50: the same order. |
| 4 · 26b | Home, light | **PASS** | `26b/home-light/`: Peg Solitaire, Heading → Settings, Button → Stars, 87 → Boards, 4/5 → Level, 26 → Continue Classic + subtitle → Continue Challenge + subtitle → Classic, 5 boards → Challenge, Continue — Level 26 → end → wraps to the title. The stage, its art and the footer are never a stop. |
| 4 · 26b | Home, dark | **PASS** | `26b/home-dark/`, 20:10:56–20:11:40: the same order. The first dark walk (`26b/home-dark-run1/`) began on Stars after a disturbed opening touch. Its own wrap still read title → Settings → Stars, and the clean re-walk is the record. |
| 4 · 26b | Tutorial, light / dark | **PASS** | `26b/tutorial-{light,dark}/`: Step 1 of 3 → Skip tutorial, Button → "Tap a peg, then tap the empty hole two cells away." → end → wraps. The board stop is absent, which is not a failure on this build (26b's 2026-10-23 note). |
| — | Leave each AVD as found and shut it down | **done** | See the restore record. |

## Bugs filed
None.

**O-1 (observation, not a defect; for the tech-lead's sign-off).** On API 36 in light three-button mode, arm B's bar is `(254,254,255)`. Arm A's is `(249,248,250)`, with buttons at 5.70:1 against 5.42:1. The cause: the window no longer carries `FORCE_LIGHT_NAVIGATION_BARS`, so the platform's contrast scrim replaces the `#fafafa`-derived one. The ruling predicted this ("the light scrim with dark buttons, as today"). The bar is still light with dark buttons, as 29 (2) requires.

## Outputs
- This report. Session artifacts are git-ignored, under `.planning/qa/home-chip-arrival/device2/` (339 MB):
  - `29-api36/` (3.1 MB): 12 captures `{threebutton,gestural}-night_{no,yes}-{home,game,tutorial}.png` (≈120–280 KB each), one `*-dumpsys-window.txt` per capture (≈60 KB), `flags.txt`, `analysis.txt` (scrim, button, handle and icon colours and ratios), `sheet-threebutton.png`, `sheet-gestural.png`.
  - `29-api36-armA/` (0.5 MB): arm A, three-button Home in light and dark, with dumpsys and `analysis.txt`. B-1 still present on arm A.
  - `29-api36-run0-interrupted/`, `29-api36-run1-sysserver-restart/`, `29-api36-run2-tutorial-missed/` (12 MB in all): superseded runs, kept and not used for any verdict. Run 0 was cut by the usage limit. In run 1, `system_server` restarted under host load (load average 77; `ANR in … NexusLauncherActivity`, then a restart at 19:47:27), and later taps landed on the launcher. In run 2 my tutorial path from gameplay left the app. The tutorial cases were re-taken alone by `tut.sh`.
  - `i4/` (8.8 MB): two dark cold-launch recordings (`i4-1.mp4` 0.9 MB, `i4-2.mp4` 1.5 MB), their frames, `sheet-{1,2}.png`, a full-resolution frame, and `analysis.txt`.
  - `29-api27/` (3.7 MB): arms A, B and old B, on Home and gameplay, light and dark, each with dumpsys; `flags.txt`, `analysis.txt` and `sheet.png`.
  - `29-api27-tutorial-first-run/` (1.6 MB): the first API 27 pass. On a fresh install the app opened the onboarding tutorial, so these captures show the tutorial. They give the same bar results, but are not the Home/gameplay record.
  - `18g/` (101 MB): `light-ab/`, `light-c/`, `dark-ab/`, `dark-c/`. Each holds continuous captures, `times.txt`, `t0.txt` and `sheet.png` (the timer pill plus the speech overlay per changed frame). Also `logcat-light-ab.txt`, `logcat-light-c.txt`, `logcat-dark.txt` (TalkBack VERBOSE) and `dark-move-t0.txt`.
  - `26b/` (160 MB): one folder per walk (`home-{light,dark}`, `classic-{light,dark}`, `challenge-{light,dark}`, `tutorial-{light,dark}`), each with burst frames per swipe, `sheet-*.png` and `ocr.txt`. Also `speech-log-1.txt` and `speech-log-2.txt`, TalkBack's every utterance with time, 117 and 265 lines. Superseded and unused: `home-dark-run1/`, `classic-light-missed-still-home/`, `classic-dark-misfire-was-home/`.
  - Device state: `p9-state-before.txt`, `p9-state-boot2.txt`, `p9-state-after.txt` (≈1 KB each); `p27-state-before.txt`, `p27-state-after.txt` (≈0.7 KB); `p9-logcat-end.txt` (24 MB, the API 36 logcat at the end); `emulator-p9.log`, `emulator-p9-boot2.log`, `emulator-p27.log`.
  - TalkBack setup: `tb-devsettings-before.png`, `tb-devsettings-check.png`, `tb-allow-dialog.png`, `tb-on.png`, `tb-off-dialog.png`, `tb-off.png`, `tb-tts-health.txt`; working frames `tb-*.png`, `home-first.png`, `game-first.png`.
  - `armA-f7dd1fe.apk` (21 MB, sha256 `8794ff45…ca3f7`), built at `f7dd1fe` in a detached scratchpad worktree with `./gradlew --offline --max-workers=4 :app:assembleDebug`, not in `PegSolitarie-home`.
  - Helpers: `state.sh`, `launch.sh`, `tree.sh`, `bars.sh`, `tut.sh`, `bars_an.py`, `sheet.py`, `armcmp.sh`, `i4.sh`, `sys.sh`, `b1b.sh`, `tbswipe.sh`, `walk2.sh` (both copied from session 1), `g18.sh`, `totut.sh`, `speech.sh`, `walkocr.sh`, `ocr.swift` (macOS Vision OCR of the speech overlay; its binary was deleted).
- **Log excerpts** (at most 20 lines):
  ```
  (API 36, arm B, dark three-button)  grep -c LIGHT_NAVIGATION_BAR → 0 ; grep -c FORCE_LIGHT → 0   (home, game, tutorial)
  (API 36, arm A, dark three-button)  mLastAppearance=LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS
  (API 27) armA light vsysui=0x2600 | armB light vsysui=0x2600 | oldB7448713 light vsysui=0x2610 | all dark vsysui=0x600
  20:14:05.179 Navigate back
  20:14:07.739 Elapsed time, under a minute      <- 18g (a), dark, pill 00:05
  20:14:49.629 Navigate back                     <- first utterance after 42 s of rest (b)
  20:14:52.196 Elapsed time, under a minute      <- refocus at 00:50
  20:15:01.939 Elapsed time, 1 minute            <- 18g (c): one utterance at the boundary, nothing after
  20:07:19.722 Elapsed time, 1 minute            <- light, refocus at 01:50
  20:07:28.959 Elapsed time, 2 minutes           <- light (c)
  20:13:07.656 Peg Solitaire / 08.820 Heading / 10.991 Elapsed time, 2 minutes / 14.197 Undo   <- 26b Classic dark
  20:00:09.099 I TextToSpeech: Connected to TTS engine
  ```

## Checks
- `shasum -a 256 .planning/qa/home-chip-arrival/armB-5708d95.apk` — pass: `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`, present on 2026-10-24 and again on 2026-10-31.
- `adb shell sha256sum <base.apk>` after each install of arm B — pass: the same hash on `Pixel_9_clean` (API 36) and `Pixel` (API 27).
- `git diff --stat 5708d95 HEAD -- app/src/main` — pass: empty (head `278c9ef`).
- `git worktree add --detach <scratchpad>/armA-wt f7dd1fe` + `./gradlew --offline --max-workers=4 :app:assembleDebug` — pass: BUILD SUCCESSFUL in 3 s (cache). The worktree was gone from `git worktree list` when I came to remove it; `git worktree prune` ran.
- `emulator -avd Pixel_9_clean -port 5640 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass. Booted 2026-10-24 23:33 and again 2026-10-31 19:42; `emu avd name` = `Pixel_9_clean`; SDK 36.
- `emulator -avd Pixel -port 5650 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass: booted 20:19; SDK 27.
- `bars.sh` + `tut.sh` + `bars_an.py` (29 (2) matrix) — pass, as in the table.
- `adb shell dumpsys window` (API 36) and `dumpsys window windows` (API 27) — the flags, as in the table.
- TalkBack enabled and disabled through **Settings → Accessibility → TalkBack → Use TalkBack** only: Allow, then "Turn off TalkBack? → Turn off" with TalkBack gestures. Gestures were sent through `adb emu event mouse` (session 1's method). `adb shell input tap` was used only for the two jump taps and Restart: it passes through TalkBack's touch explorer on this image. No `uiautomator dump` ran while TalkBack was on (from 20:00:09 to the turn-off at ≈20:18).
- `adb logcat -d | grep 'SpeechControllerImpl: Speaking fragment'` — the utterance record behind 18g and 26b.
- Not run: the Check, or any Gradle build in `PegSolitarie-home`. These are device checks, and the build was off-limits there.

## Restore record
**`Pixel_9_clean` (API 36).** `p9-state-after.txt` is identical to `p9-state-before.txt`, apart from the date line (`diff` printed nothing):
- night mode no; gestural navigation (`navigation_mode` 2);
- the app not installed (it was absent at the start, and `adb uninstall` removed it);
- animations 1.0; font scale 1.0; `enabled_accessibility_services` empty and `accessibility_enabled` 0, as found;
- developer options on, as found and as asked; `Accounts: 1`, as found.

TalkBack's developer settings were found with "Display speech output" on, "Handle gestures" on, "Explore by touch" on and log level VERBOSE. I read and changed none of them; `tb-devsettings-check.png` matches `tb-devsettings-before.png` byte for byte.

**System changes on `Pixel_9_clean` and what happened to each:**
- `cmd uimode night` — back to `no`. Between the two boots it persisted as `yes`, because the AVD writes userdata on a cold boot too; reset at the end.
- `cmd overlay enable-exclusive … navbar.threebutton/gestural` — back to `gestural`.
- TalkBack on and off through the Settings UI.
- Arm A installed for comparison, then arm B reinstalled; the app was uninstalled at the end.
- `system_server` restarted once at 19:47:27 under host load. That was the platform's own watchdog, not a change I made.

**`Pixel` (API 27).**
- The app was installed (arms A, B and old B) and then uninstalled.
- `cmd uimode night yes/no` — back to `no`.
- `p27-state-after.txt` differs from before in one line: `secure ui_night_mode` was `null` and is now `1` (MODE_NIGHT_NO). The two mean the same thing. Putting back `null` would need `settings delete/put` on a secure setting, which is prohibited, so I left it.

Both AVDs were shut down with `adb emu kill` (Pixel_9_clean at ≈20:18:40, Pixel at ≈20:27). `adb devices` was empty afterwards. I touched neither the GM6 phone nor `ergoz_studio`, nor any other emulator, and installed on no other serial.

## Open issues / risks
1. **Play Games cloud data on `Pixel_9_clean`.** The AVD's Google account (`ThankfulRock150`) restored cloud progress on install: Stars 87, Level 26, a Challenge save at level 25. My sessions made one jump on English-33 (twice, after a Restart) and resumed the Challenge level-25 save. I completed no level. The app may have synced the Classic resume snapshot to that test account's Saved Games. Uninstalling removed the local data only.
2. **Host load.** `ergoz_studio` and Gradle builds from other sessions ran during this session. On API 36 this caused a `system_server` restart, which voided run 1, and one disturbed TalkBack opening (`home-dark-run1`). Every verdict comes from a clean run, and every TalkBack verdict is backed by TalkBack's own timestamped log.
3. **The 18g (c) light read used the 01:50 → 02:00 boundary**, not 00:50. The dark read used 00:50 → 01:00, as the ruling words it. Both show exactly one utterance at the boundary.
4. **Not in this session:** session S (15e re-sit, 15v, 15w, the bench 29 read), as dispatched. Tutorial completion ("Got it") was not walked: 26b's tutorial line was read up to the caption, as session 1 did in dark.
5. **I4 for the sitting:** the dark-mode starting window is near-white for about 2 s on a cold launch. The ruling accepted this. The bar over it is legible, with dark buttons at 5.78:1.

## Handoff
Next: producer —
- No FAIL, so nothing goes back to the tech-lead before the sitting. Its sign-off may note O-1 and I4.
- Session S can be dispatched on the bench with arm B `5708d95` (`3f66c5b6…`). Clear the host of every other emulator and Gradle build first (P-4).
- Commit this report alone.

VERDICT: PASS
