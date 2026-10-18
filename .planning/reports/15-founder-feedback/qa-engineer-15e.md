# qa-engineer report — 15-founder-feedback: sign-off condition H-3 (criterion 15e on Pixel_9a_bench, F-2 on the device, the removal-cue re-check, the light founder screenshots)
Date: 2026-10-18
Branch: feature/phase-15-ux-refresh (committed by the producer). Arm A `c3d3437` (merge-base with `develop`), arm B `ee16a2a` (the branch's code head). HEAD at the session was `e9c870b`; `ee16a2a..e9c870b` touches `.planning/` only.
Skills loaded: ergoz-soft:standards (role qa-engineer). The product's snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android, compose. Also the product's `.claude/rules/lessons.md`. Then android-emulator-skill and android-testing.
Ran in parallel with: candy-backdrop tech-lead fit ruling (docs only), tutorial haptics ruling (docs only), store-copy drafts (docs only)
Ran on: claude-opus-5-5 at effort high (the agent file's effort). **Staffing deviation:** the qa-engineer is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17) in `.planning/questions/FOUNDER.md`. I ran every measurement myself.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

- **15e: PASS.** Delta 33.0 ms against a 100 ms threshold. Floor 3.0 ms.
- **F-2 on the device: PASS.**
- **Removal-cue re-check (S1–S8 F-2): PASS.**
- **Screenshots: recaptured.** All eight light shots.

## Summary
- **15e PASS** on `Pixel_9a_bench`. Arm B's median is 424.0 ms and arm A's is 391.0 ms, so the delta is 33.0 ms. The threshold is `max(2 × 3.0, 100)` = 100 ms. Every precondition held, precondition 9 included: Data-Free read 13.9 GB at the start and 13.8 GB at the end.
- **F-2 PASS on all six screens.** In light mode the status-bar icons are dark (`#000000`, 18.8–20.1:1). In dark mode they are light (`#ffffff`, 16.1–17.7:1). Arm A, the positive control, draws `#ffffff` on `#fbf9ff` in light mode (1.04:1). The bottom 48 dp is pixel-identical between arm A and arm B in gesture and three-button modes, in both themes.
- **Removal cue PASS** with animations off. A gameplay jump logs Jump and then PegRemove 63 ms later. A tutorial jump logs the same pair 68 ms apart.
- **Two deviations, both recorded below.** This ran on Opus, not the pinned Fable (Q-019). `avdmanager` is not installed, so I wrote the AVD from the Pixel_9a's own files; P-1.2's config.ini diff shows only the name and the data partition differ.
- I left `Pixel_9a_bench` running (`emulator-5554`), seeded, with arm B installed.

## Instrument and builds
- **Pixel_9a shut down first.** It was running as `emulator-5554`; I stopped it cleanly at 01:36 with `adb emu kill` and touched nothing on it. Its data was not wiped, cleared or deleted.
- **The bench AVD** (`~/.android/avd/Pixel_9a_bench.{ini,avd}`, created 01:38). `avdmanager` is not on this host (`which avdmanager` finds nothing; the SDK has no `cmdline-tools`). The `android` CLI's `emulator create` takes only a profile name, not a name or an image. So I wrote the two files `avdmanager` writes:
  - `config.ini` is `Pixel_9a.avd/config.ini` with three keys changed: `AvdId`, `avd.ini.displayname` and `disk.dataPartition.size` (6G → 16G).
  - The `.ini` differs only in `path` and `path.rel`.
  - The diff is identical after both boots. It is quoted in the evidence file. P-1.2 allows only the name, the paths and the data partition to differ, and nothing else differs.
  - **Deviation D-1, for the tech-lead:** this is P-1.1 by other means. If the tech-lead wants the literal `avdmanager` route, the sitting must be re-run after `cmdline-tools` is installed.
- **P-1 steps 3–8:**
  - **3.** The cold boot reached the launcher already provisioned (`device_provisioned=1`, `user_setup_complete=1`), with Accounts: 0.
  - **4.** `bg-dexopt-job` ran to "Job finished" (48 s).
  - **5.** I installed arm B and passed the first-run surfaces: consent → Consent, tutorial → Skip, "Sign in to Play Games?" → Not now. On relaunch, Home appeared with no first-run surface.
  - **6.** I seeded loop 3's ladder (200 rows, one Classic save; Room identity hash `28f7b5a4…` = `2.json`) and set `challenge_current_level` 111. Home read "Stars, 401".
  - **7.** "Level 111" ink measured **81.52 dp**, a PASS.
  - **8.** I shut the emulator down and cold-booted it with `-no-snapshot-load -no-snapshot-save`. I added `-no-snapshot-load` so the boot was cold, since the AVD has `fastboot.forceFastBoot=yes`. It then idled on the launcher for 3 min.
- **Builds:** each arm in its own detached worktree (`PegSolitarie-h3-armA`, `-armB`, with `local.properties` copied), built with `./gradlew --no-daemon -q :app:assembleDebug`, then `--stop`. Both worktrees were removed afterwards and pruned.
  - A = `8d8e3a5a…3761`. It equals loop 3's `develop` build byte for byte.
  - B = `52225874…4f40`.
  - Both are signed with `8a4e64a3…c1a4a6`.
  - `aapt2` shows arm B's `Theme.PegSolitaire` carries `windowLightStatusBar` true, and false under `(night)`. Arm A has neither.
- **Quiet host:** the producer declared it.
  - Load at session start (01:35): `1.32 1.64 2.35`.
  - Load at session end (02:02): `2.70 2.82 2.79`.
  - No Gradle or Kotlin process ran during the sitting.

## 15e — PASS
The full tables (§6.5's field shape, the preconditions with values, the config.ini diff and both Data-Free readings) are in `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, "Criterion 15e — the sitting of record". Raw data is in `h3/sitting/`.

| | N1 | N2 | Arm A | Arm B |
|---|---|---|---|---|
| Recorded samples | 20 | 20 | 20 | 20 |
| min / median / max (ms) | 385 / **438.5** / 516 | 342 / **435.5** / 495 | 343 / **391.0** / 432 | 378 / **424.0** / 505 |

- floor = 3.0 ms; threshold = max(6, 100) = **100 ms**; delta = **33.0 ms**. That is a **PASS**, far from the VOID line (150 ms).
- **Preconditions 1–9 all held:**
  - one AVD, one emulator, no Gradle;
  - load 1.13 before and 1.84 after, against a ceiling of 5.0;
  - all 20 swaps' installed sha256 matched;
  - the schema is identical;
  - `SUM(stars)` read 401 after the sitting;
  - Data-Free was 85 % at the start and 84 % at the end;
  - every sample was `LaunchState: COLD`.
- **Precondition 5, read honestly.** `compile -m speed -f` ran after every install. ART still leaves a debuggable APK at `[status=verify] [reason=cmdline]`, and I read that state on both arms (`dexopt-ours-after.txt`, `dexopt-armA-after-compile.txt`). So the two arms share one ART state, which is what the precondition protects. My driver's per-swap `dexopt` column is wrong: it captured the first package in `dumpsys`, not ours. It is not used anywhere.
- **The fonts' blocking load** is inside the 33 ms: arm B carries the bundled faces and arm A does not. The FAIL path (fonts back to the tech-lead) is not triggered.

## F-2 on the device — PASS
Readings are taken from the status-bar band (y 0–141 px). The background is the band's most common colour. The icon value is the ink pixel with the highest contrast against it, in WCAG ratio. All readings are on arm B in gesture navigation.

| Screen | Light: bg / icon / ratio | Dark: bg / icon / ratio |
|---|---|---|
| Home (fresh, and returning) | `#f4f1fa` / `#000000` / 18.81:1 | `#1b1626` / `#ffffff` / 17.66:1 |
| Gameplay (Classic resume; Challenge 111) | `#f4f1fa` / `#000000` / 18.81:1 | `#1b1626` / `#ffffff` / 17.66:1 |
| Classic catalogue | `#fbf9ff` / `#000000` / 20.10:1 | `#241e31` / `#ffffff` / 16.09:1 |
| Results | `#fbf9ff` / `#000000` / 20.10:1 | `#241e31` / `#ffffff` / 16.09:1 |
| Settings | `#fbf9ff` / `#000000` / 20.10:1 | `#241e31` / `#ffffff` / 16.09:1 |
| Tutorial (step 1) | `#f4f1fa` / `#000000` / 18.81:1 | `#1b1626` / `#ffffff` / 17.66:1 |
| *Arm A, Home: the positive control* | `#fbf9ff` / `#ffffff` / **1.04:1** | `#241e31` / `#ffffff` / 16.09:1 |

**The navigation bar is unchanged.** I compared the bottom 48 dp (126 px) of Home, arm A against arm B:

| Mode | Theme | bg / handle or buttons / ratio (both arms) | A vs B |
|---|---|---|---|
| Gesture | light | `#f4f1fa` / `#626064` / 5.57:1 | mean \|ΔRGB\| 0.00, 0 px differ |
| Gesture | dark | `#1b1626` / `#ededee` / 15.09:1 | 0.00, 0 px |
| Three-button | light | `#f9f8fa` / `#666666` / 5.42:1 | 0.00, 0 px |
| Three-button | dark | `#cdccd0` / `#666666` / 3.59:1 | 0.00, 0 px |

- The gesture handle adapts to the theme on its own.
- The three-button bar keeps the platform scrim. In dark mode that scrim is light grey in both arms (O-1).
- I switched the mode with `cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton`, then back to `…gestural`. The bench is back on gestural.

## Removal-cue re-check (S1–S8 F-2) — PASS
- **Setup:** animations 0/0/0, read from `settings get global` inside the helper. I took the `dumpsys audio` player-event log before and after each tap pair.
- **Attribution:** `piid:935` is `type:android.media.SoundPool`, `package:com.erman.pegsolitarie`, uid 10223.

```
gameplay jump (Classic, peg (3,3) over (3,2) into (3,1); the board changed, h3/audio/cmp-jump.png)
10-18 01:57:21:210 player piid:935 event:started     <- Select (tap on the peg)
10-18 01:57:22:462 player piid:935 event:started     <- Jump
10-18 01:57:22:525 player piid:935 event:started     <- PegRemove, +63 ms
tutorial jump (step 1's guided jump; the tutorial advanced to "Step 2 of 3")
10-18 01:59:24:364 player piid:935 event:started     <- Jump
10-18 01:59:24:432 player piid:935 event:started     <- PegRemove, +68 ms
```

- **Gameplay:** before the S9 fix the same probe logged only Select and Jump (`phase-15/qa-engineer.md`, F-2). Now each jump logs two events about 60 ms apart.
- **Tutorial:** the select tap is silent, as 7d requires.

## Founder screenshots — the eight light shots, recaptured
- **Where:** `.planning/qa/15-founder-feedback/founder/`, overwritten in place.
- **What they come from:** arm B (`52225874…`), the install that passed the AC-F7 discriminator in this session. They were taken on `Pixel_9a_bench` at 1080×2424, font 1.0, in gesture navigation.
- **Same scenes as before:** every shot was compared side by side with the file it replaces (`h3/walk/cmp-*.png`).
- **The status bar now shows dark icons in every one of them.**

| Path | Size | What it shows |
|---|---|---|
| `founder/01-home-fresh-light.png` | 199 KB | Home on a fresh profile after the first run: Stars 0, Boards 0/5, Level 1, no chip, the full stage |
| `founder/03-home-returning-light.png` | 199 KB | Returning player: Stars 401, Level 111, the Continue Classic chip, the stage at one-chip size |
| `founder/05-game-midgame-light.png` | 265 KB | Classic English-33 resumed mid-game, timer 05:12, Show-moves off |
| `founder/07-classic-catalogue-light.png` | 195 KB | The Classic catalogue, five boards with star pips |
| `founder/09-challenge-level111-light.png` | 104 KB | Challenge "Level 111", fresh board, 00:00 |
| `founder/11-results-light.png` | 70 KB | Results after solving Challenge level 1: "Perfect!", three stars, 5240, 00:06, 1 peg left |
| `founder/13-settings-light.png` | 164 KB | Settings, with the Play Games row signed out ("Sign in to Play Games"), because the bench has no account |
| `founder/15-tutorial-step1-light.png` | 87 KB | Tutorial step 1 of 3, with the guided jump and its caption |

The eight dark files (02…16) are loop 3's, unchanged. They were already correct, since dark mode never had F-2.

## Findings
- **No FAIL items.**
- **O-1 (low, pre-existing, unchanged by F-2; for the ux-designer):** in dark mode, the three-button navigation bar draws a light grey scrim (`#cdccd0`) with grey buttons (`#666666`, 3.59:1) under the dark app. That is a light band at the foot of every dark screen (`h3/f2/armA-navbar-sheet.png`, row 4). Arm A shows it identically, so it is not a regression. Decision 3 left the navigation bar alone on purpose. Whether a dark three-button bar is wanted is a design question, not a defect against any criterion.
- **O-2:** the absolute TotalTime here (≈ 0.4 s) is about half of what the Pixel_9a gave in earlier sittings. It is evidence about this host and AVD, not a phone, and not a reading of the ≤ 2 s bar (§6.5, D-03j).

## Outputs
- `.planning/reports/15-founder-feedback/qa-engineer-15e.md`: this report. Not committed.
- `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`: a new subsection at the head of `## Cold-start measurement`. It holds the instrument, the config.ini diff, the builds, the nine preconditions, the null and arm tables, both Data-Free readings, the verdict and the precision caveat. The 2026-09-28 text is kept below it, retitled as the superseded record. Not committed; the producer commits it.
- `.planning/qa/15-founder-feedback/founder/{01,03,05,07,09,11,13,15}-*-light.png`: overwritten, indexed above.
- **Artifacts** (git-ignored), under `.planning/qa/15-founder-feedback/h3/` (49 MB, 136 files):
  - `build/armA-c3d3437.apk` (20.6 MB) and `build/armB-ee16a2a.apk` (21.0 MB): the two arms. `build/arm{A,B}-assemble.log` (250 B each).
  - `bench/config.ini.diff` (191 B), `bench/ini.diff` (182 B) and `bench/config.ini.created` (1.4 KB): the instrument. `bench/bg-dexopt-1.txt`.
  - `bench/firstboot.png` (1.4 MB): the launcher, no setup wizard.
  - `bench/firstrun-{1..5}.{png,xml}`: consent, tutorial, Play Games prompt, Home, and a clean relaunch.
  - `bench/seeded-home.{png,xml}`: "Stars, 401".
  - `bench/acf7-level111.{png,xml,txt}`: 81.52 dp.
  - `bench/final-state.{png,xml}`: the state I left the bench in.
  - `sitting/samples.csv` (4.4 KB): 100 samples. `sitting/swaps.csv` (3.2 KB): 20 sha checks. `sitting/preconditions.txt` (1.1 KB): both uptimes, Data-Free, device and host. `sitting/verdict.txt` (0.9 KB).
  - `sitting/device-state.txt`, `sitting/dexopt-ours-after.txt`, `sitting/dexopt-armA-after-compile.txt`, `sitting/stars-after.txt` and `sitting/after-db/a.db` (the post-sitting database).
  - `f2/arm{A,B}-home-{light,dark}-{gesture,3button}.{png,xml}`: the 16 navigation-bar captures (84–198 KB each).
  - `f2/statusbar-sheet-armB.png` (82 KB): all 15 status bars, light/dark.
  - `f2/armA-navbar-sheet.png`, `f2/armA-home-light-statusbar-enhanced.png`, `f2/{founder-light,dark}-statusbars.txt` and `f2/navbar-overlays-found.txt`.
  - `walk/*-{light,dark}.{png,xml}`: every screen in both themes. `walk/cmp-{01..15}.png`: each new founder shot beside the old one. `walk/level1-start.png`.
  - `audio/{game,tutorial}-jump-{before,after}.txt` (≈ 44 KB each): the `dumpsys audio` dumps. `audio/*-new-events.txt`: the added events. `audio/cmp-jump.png` and `audio/*-after-jump.{png,xml}`.
  - `seed/fresh-profile.tar` (172 KB): the first-run profile. `seed/ladder-classic.db`, `seed/{fresh,level111,level1,current}-prefs.pb`, `seed/elapsed/` and `seed/before-fresh-restore.tar`.
  - `logs/logcat-app.txt` (500 lines): the app's last process. `logs/logcat-crash.txt` (0 B). `logs/app-EW-summary.txt`. `logs/emulator-boot{1,2-sitting}.log` and `logs/session-start.txt`.
  - **Helper scripts (never committed):** `tools/sitting.sh` (the 15e driver), `tools/stats.py` (floor, threshold, verdict), `tools/bars.py` (status- and navigation-bar readings), `tools/audiojump.sh`, `tools/darkpair.sh`, and `tools/set_elapsed.sh` (adapted from loop 3). I also reused loop 3's `shot.sh`, `tap.py`, `nodes.py`, `inkbox.py`, `prefs.py`, `setstate.sh` and `sheet.py` in place.

## Checks
- `git -C PegSolitarie-p15 merge-base develop HEAD` — `c3d3437` (= `develop`). `git log -1 -- . ':!.planning' ':!docs'` — `ee16a2a`. `git diff --stat ee16a2a HEAD -- . ':!.planning'` — empty.
- `git diff --stat c3d3437 ee16a2a -- app/schemas` — empty. `git diff c3d3437 ee16a2a -- app/src/main | grep -E '^[+-].*PreferencesKey'` — 0 lines.
- `./gradlew --no-daemon -q :app:assembleDebug` in both detached worktrees — pass (exit 0, 30 s and 28 s). `./gradlew --stop` in both. `pgrep -fl 'GradleDaemon|KotlinCompileDaemon|gradlew'` — empty.
- `shasum -a 256`, `apksigner verify --print-certs` and `aapt2 dump resources` on both APKs — the values above.
- `adb -s emulator-5554 emu kill` (Pixel_9a) — pass. `pgrep qemu-system` — empty.
- `emulator -avd Pixel_9a_bench -port 5554 -no-snapshot-load -no-boot-anim` — booted. `adb shell cmd package bg-dexopt-job` — "Job finished".
- `emulator -avd Pixel_9a_bench -port 5554 -no-snapshot-load -no-snapshot-save -no-boot-anim` — the sitting boot, `sys.boot_completed=1` at 01:42:45.
- `bash h3/tools/sitting.sh` — pass. 20 blocks, 100 samples, 20/20 sha matches, DONE at 01:52:21.
- `python3 h3/tools/stats.py h3/sitting/samples.csv` — `floor=3.0 threshold=100 delta=33.0 verdict: PASS`.
- `sqlite3` on the pulled database — `rows=200 sum_stars=401`.
- `adb shell dumpsys diskstats | grep Data-Free` — 85 % free at the start, 84 % at the end (13.8 GB at session end).
- `uptime` — session start `1.32`; sitting before `1.13`, after `1.84`; session end `2.70`.
- `/usr/bin/python3 h3/tools/bars.py status|nav …` — the F-2 tables above.
- `bash h3/tools/audiojump.sh …` — 3 events (gameplay) and 2 events (tutorial), as quoted.
- `adb logcat -d -b crash` — empty. App log excerpt (`logs/logcat-app.txt`, `logs/app-EW-summary.txt`; platform noise only, no FATAL, no app exception):
```
10-18 02:01:13.449 20880 20886 W an.pegsolitarie: userfaultfd: MOVE ioctl seems unsupported: Connection timed out
 985 E an.pegsolitarie: Failed to query component interface for required system resources: 6   (media codec probe, every SoundPool start)
 120 E AppOps  : attributionTag  not declared in manifest of com.erman.pegsolitarie
  22 W AlarmManager: Package com.erman.pegsolitarie, uid 10223 lost permission to set exact alarms!
```
- **The Check command was not run.** This dispatch is a device sitting on a quiet host, and the code under test has not changed since the H-2 and R-1 commits whose Check is on record. Running Gradle's `check` would itself break P-4.
- **System changes on the bench, a device used for nothing else (P-1):**
  - `settings put global` for the three animation scales: 1.0/1.0/unset → 0/0/0. Left at 0, because §6.5 precondition 1 needs them off for every sitting.
  - `svc power stayon true`: `stay_on_while_plugged_in` 1 → 15. Left.
  - `wm size reset` and `wm density reset`: no change.
  - `cmd uimode night` yes ↔ no: restored to no.
  - `cmd overlay enable-exclusive` three-button ↔ gestural: restored to gestural.
  - The app's own data: seeded, fresh profile restored, re-seeded.
- **Not touched:** Pixel_9a (no wipe, clear, delete or install), no other package, no radio toggle, no `settings put secure`.

## Open issues / risks
- **D-1, the AVD was not made by `avdmanager`.** The P-1.2 diff stands in for it. The tech-lead rules whether that suffices; a re-run needs `cmdline-tools` installed first.
- **H-1 recorded a FAIL** (`a8b07fb`, "three test defects") before this dispatch. If its fix is test-only, the sitting stands, per Decision 1's "What stays valid". If the fix touches a start-path file (`App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, `res/values*/themes.xml`, `AndroidManifest.xml`), 15e must be re-sat on the bench.
- **The bench's state** (the candy sitting will find it this way):
  - running on `emulator-5554`;
  - arm B installed;
  - the ladder seeded: Stars 401, Level 111, one Classic save;
  - consent given, onboarding seen, the Play Games prompt answered "Not now";
  - animations 0/0/0, light mode, gesture navigation;
  - no Google account;
  - Data-Free 13.8 GB.
- `.planning/qa/` holds 149 MB with this session's 49 MB (41.6 MB of it the two APKs). The producer deletes `h3/` after the Done line.

## Handoff
Next: producer —
- H-3 closes: 15e PASS, every device check PASS, and the eight light shots are recaptured. H-4 (showing the founder the 16 shots) can use them.
- Commit `15-BUILD-EVIDENCE.md` and this report.
- Pass D-1 (the hand-made AVD) and O-1 (the dark three-button scrim) to the tech-lead and the ux-designer for the record. Neither blocks.
- The HOLD's remaining condition is H-1.
- `Pixel_9a_bench` is still running on `emulator-5554`. Pixel_9a is shut down, and nothing on it was changed.
