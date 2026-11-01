# qa-engineer report — candy-backdrop: closing BD-23 (d) (a desk check, then one bench visit for battery saver)
Date: 2026-11-01
Branch: feature/candy-backdrop (report only). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-candy`, HEAD `01c3669`. APK under test: J-5's arm C `armC-0c45666.apk`, sha256 `3e9740164951…ed96b` (rechecked on disk and on the device). `git diff --name-only 0c45666 HEAD`, excluding `.planning` and `docs`, lists only `BackdropReducedMotionTest.kt` and `BackdropSceneOpenTest.kt`. Both are tests, so the APK is still the product at HEAD.
Skills loaded: ergoz-soft:standards (role qa-engineer; the rules snapshot imported by CLAUDE.md: studio, reporting, gitflow, testing, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, `.claude/rules/lessons.md`). The bench procedure and helpers were reused from J-5 (android-emulator-skill's commands, through adb directly).
Ran in parallel with: none. No Gradle ran in this session, because no build was needed.
Ran on: claude-opus-5-5, effort low (the harness's setting for this dispatch). The agent file pins fable at high; founder answer Q-019 puts Fable-pinned roles on Opus.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)
Authority: `docs/ux/candy-backdrop.md` BD-23 (d) as amended by B-11c (`14ce6d8`); `.planning/reports/candy-backdrop/ux-designer-bd23d.md`; `qa-engineer-j5.md`.

## Summary
- **Desk, item (2): confirmed.** I recomputed the ten values of D from the recordings, using the integer channel values rather than the 2-decimal series. They are 486–733 ms, all ≥ 350 ms, and all ten runs are resolved. The ux-designer's 486–725 ms is right on the verdict and the minimum. Seven of its values are off by 3–24 ms because of the rounding in the series (table below). (2)(a), measured from the running extreme, is ≤ 2 code values in every run.
- **Desk, item (6): PASS, 4/4.** I fixed the threshold before reading: the line's own value of 6 samples by 6 code values, over the app area y 142–2360. The scene arrives fully in on Home frame #2 in every run. No frame between holds an intermediate value, and none of the frames before the scene changes a single app-area sample.
- **Bench, battery saver: FAIL, on item (4) in 1 of 4 runs.** In `bs-light-2` the scene is at 1 **2.045 s** after Home's first recorded frame, against ≤ 2.0 s. Items (1), (2) and (3) hold in all four runs, and the scene is the still rest pose.
- **Platform fact:** this bench cannot show the light theme in battery saver. Android forces the dark theme there ("Dark theme — Temporarily disabled due to Battery Saver"). Both the light-setting runs and the dark-setting runs render the dark theme.
- **The bench is restored as found:** APK `b2ee555f…`, scales 0.0, light, battery saver off, and data byte-identical. I ran `sync`, then `emu kill`.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/qa-engineer-bd23d-close.md` — this report.

Artifacts are under `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/qa/candy-backdrop/bd23d-close/`, which is git-ignored and 10 MB in total:

| Path | Size (bytes) | What it shows |
|---|---|---|
| `pace.py` | small | Item (2) by B-11c. It uses the same P (727, 2302), H, T, C_sky, C_full and channel as J-5's `bd23d.py`. A is the first frame from T with f ≥ 0.50. "First at 1" is the first frame within 2 code values of C_full, and B is the frame before it. It also reports (2)(a) against the running extreme |
| `pace-j5.txt` | 2,045 | The desk read of item (2) on the ten J-5 recordings (table below) |
| `threshold.txt` | 552 | The item-(6) noise threshold, written before any item-(6) read on this visit |
| `home6.py`, `read-s0-home6.txt` | small / 922 | The item-(6) re-read of J-5's `s0-*` recordings by B-11c's Home-frame rule |
| `rec.sh`, `frames.py`, `bd23d.py` | small | Copied unchanged from J-5's `bd23d/`. They record one cold start, and read items (1)–(4) |
| `rest.py` | small | Compares each battery-saver run's last frame with the scale-0 rest pose (J-5 `s0-dark-1`) over the app area, on a 6 px grid |
| `bs-light-1.mp4`, `bs-light-2.mp4` | 2,064,708 / 2,264,897 | Battery saver, system light setting (rendered dark), one cold start each, 10 s windows |
| `bs-dark-1.mp4`, `bs-dark-2.mp4` | 2,842,118 / 2,835,939 | Battery saver, system Dark theme on, one cold start each |
| `bs-*/pts.txt`, `launch-bs-*.txt` | small | Each recording's frame presentation times, and `am start -W` (all COLD) |
| `read-bs.txt` | 4,244 | The full read of the four battery-saver runs: `bd23d.py`, `pace.py` and `rest.py` |
| `battery-saver-on.png` | 109,437 | Settings › Battery Saver › "Use Battery Saver" on. The system UI turns dark |
| `dark-theme-forced-by-saver.png` | 136,372 | Accessibility › Color & motion during battery saver. Dark theme is on and greyed out: "Temporarily disabled due to Battery Saver" |
| `dark-theme-on.png`, `remove-animations-off.png`, `color-motion-restored.png` | ~141 K each | The toggles set and restored through the UI |
| `bench-state-start.txt`, `bench-state-final.txt`, `db-start/`, `db-end/`, `boot.txt`, `emulator.log` | small | The bench as found and as left. Boot 12:15:01 → 12:15:13 |

## (1) Desk check

### Item (2), recomputed at full precision
Channel and C values are code values, and times are seconds after H. ε = 2/56 = 0.0357 in light and 2/75 = 0.0267 in dark.

| Run | A: t, C, f | B: t, C, f | first at 1 | t_B − t_A | **D (QA)** | D (ux) | plain pace | (2)(a) max back |
|---|---|---|---|---|---|---|---|---|
| light-1 | 0.814, 158, 0.643 | 0.996, 146, 0.857 | 1.063 | 181 ms | **634** | 625 | 845 | 0 |
| light-2 | 0.715, 166, 0.500 | 1.081, 141, 0.946 | 1.148 | 367 ms | **708** | 702 | 821 | 0 |
| light-3 | 0.832, 161, 0.589 | 1.065, 145, 0.875 | 1.132 | 233 ms | **653** | 645 | 816 | 0 |
| light-4 | 0.751, 165, 0.518 | 0.951, 146, 0.857 | 1.048 | 200 ms | **486** | 486 | 589 | 0 |
| light-5 | 0.780, 164, 0.536 | 1.029, 143, 0.911 | 1.097 | 249 ms | **558** | 564 | 665 | 0 |
| darkL-1 | 1.197, 109, 0.520 | 1.488, 140, 0.933 | 1.657 | 291 ms | **623** | 628 | 704 | 2 |
| darkL-2 | 1.400, 110, 0.533 | 1.635, 131, 0.813 | 1.784 | 235 ms | **705** | 705 | 839 | 2 |
| darkL-3 | 1.384, 110, 0.533 | 1.727, 141, 0.947 | 1.959 | 342 ms | **733** | 725 | 828 | 2 |
| darkL-4 | 1.344, 112, 0.560 | 1.611, 137, 0.893 | 1.731 | 267 ms | **690** | 697 | 801 | 0 |
| darkL-5 | 1.424, 120, 0.667 | 1.545, 131, 0.813 | 1.715 | 120 ms | **602** | 626 | 821 | 0 |

- **(2)(b) PASS, 10/10.**
  - D ranges from 486 to 733 ms, against ≥ 350.
  - No run is unresolved: in every run B is after A, and the shortest gap, t_B − t_A, is 120 ms against the 50 ms minimum.
  - A, B and "first at 1" fall on the same frames the ux-designer chose in all ten runs.
  - Seven values differ by 3–24 ms. The largest is `darkL-5`, 602 against 626, which comes from the 2-decimal rounding in `read-*.txt`. The minimum, 486 (`light-4`), is identical.
- **(2)(a) PASS, 10/10.** I measured from the open (T) against the furthest value reached, as the restated line says, rather than frame to frame as J-5 did. The largest move back is 2 code values: `darkL-1`, `darkL-2` and `darkL-3`, the steady front wave at P. That is within tolerance.

### Item (6), under the amended Home-frame definition
- **Threshold, fixed before reading** (`threshold.txt`, 12:16):
  - App area: y 142–2360. On this bench, `dumpsys window` gives statusBars [0,0][1080,142] and navigationBars from y 2361.
  - Grid: 6 px.
  - A sample moves when any channel changes by more than 6 code values against the previous Home frame.
  - A Home frame is one where more than 6 samples move. That is the line's own value, neither tightened nor loosened.
- **Result:** in all four runs the scene's frame is Home frame #2, with no recorded frame at an intermediate fraction.

| Run | H | Scene fully in | Samples moved on the scene's frame | Most samples moved on any frame in between |
|---|---|---|---|---|
| s0-light-1 | f23 | f31, +0.434 s | 15,173 | 0 |
| s0-light-2 | f22 | f29, +0.234 s | 15,173 | 0 |
| s0-dark-1 | f23 | f34, +0.398 s | 15,482 | 0 |
| s0-dark-2 | f17 | f30, +0.571 s | 15,482 | 0 |

- The status-bar-only frames J-5 saw now fall outside the area entirely: the area starts below the status bar's 142 px.
- **(6) PASS, 4/4.**

## (2) Bench: the battery-saver clause
- **Setup:**
  - `Pixel_9a_bench` on emulator-5584, booted with `-no-snapshot-load -no-snapshot-save -no-boot-anim`. The battery reported "AC powered: false", so battery saver could be switched on.
  - Arm C installed with `install -r`.
  - Animations on, through Accessibility › Color & motion › Remove animations off. The scales read 1.0.
  - Battery saver on, through Settings › Battery Saver › Use Battery Saver. `low_power` read 1.
- **Recording:** one cold start per recording with `rec.sh`: `am force-stop`, then `screenrecord` with a 10 s window at 20 Mb/s, then `am start -W`. All four were COLD.
- **Light could not be recorded.** With the system on light, battery saver forces the dark theme. `C_sky` read (53, 13, 70), the dark sky, and `dark-theme-forced-by-saver.png` shows the toggle greyed out.
- So "light" below means the user's light setting with dark rendered by force. For "dark", I turned battery saver off, turned Dark theme on through the UI, turned battery saver back on, and recorded two runs.

| Run | TotalTime | T − H | (1) max fraction before T; margin probes | (2)(a) back | (2)(b) A→B, D | (3) max step | (4) at 1 after H | Rest pose (last frame against scale 0) |
|---|---|---|---|---|---|---|---|---|
| bs-light-1 | 4,287 ms | 0.938 s | 0.000; ≤ 1 | 0 | 1.653→1.800, **629 ms** | 0.273 | **1.953 s** | 41 / 66,600 samples > 6 cv, max 12; no frame after 7.15 s of 10 |
| bs-light-2 | 2,941 ms | 0.970 s | 0.000; ≤ 3 | 0 | 1.729→1.996, **823 ms** | 0.364 | **2.045 s → FAIL** | 15 / 66,600, max 8; no frame after 6.00 s |
| bs-dark-1 | 2,264 ms | 0.592 s | 0.000; ≤ 1 | 0 | 1.129→1.529, **879 ms** | 0.130 | **1.564 s** | 25 / 66,600, max 12; no frame after 4.82 s |
| bs-dark-2 | 2,130 ms | 0.515 s | 0.000; ≤ 1 | 0 | 1.401→1.749, **743 ms** | 0.182 | **1.803 s** | 47 / 66,600, max 17; no frame after 4.99 s |

Common values in all four runs: channel B, C_sky = 70, C_full = 147, ε = 0.026.

- **(1) PASS, 4/4.** P stays at 0 until the Challenge tile has settled, and the margin probes move by ≤ 3 code values, so no wave or light pixel shows.
- **(2) PASS, 4/4.**
  - All four runs are resolved.
  - D is 629–879 ms, against ≥ 350.
  - There is no move back at all.
- **(3) PASS, 4/4.** The largest step is 0.364, against ≤ 0.50.
- **(4) FAIL, 3/4.** `bs-light-2` is at 1 at 2.045 s, 45 ms over the 2.0 s line. `bs-light-1` passes at 1.953 s, with 47 ms to spare.
- **Rest pose: holds.**
  - The fade ends on the scale-0 rest pose: ≤ 47 of 66,600 samples differ by more than 6 code values, which is codec noise at the wave edges.
  - After the scene is fully in, the recorder writes no frame for the 3–5 s left in its window, so the scene is still and runs no clock.

Evidence for `bs-light-2` (4), from `read-bs.txt`. Times are s after H, and C is the B channel, with C_full = 147:

```
bs-light-2: LaunchState: COLD TotalTime: 2941
bs-light-2: frames=67 H=f46@3.958s T(tile settled)=f57@4.929s (0.970s after H) C_sky=(53, 13, 70) C_full=(103, 33, 147) ch=B delta=77
  (4) fully in 2.045 s after H -> FAIL
  series: 1.282:0.00 1.318:0.00 1.495:0.36 1.599:0.45 1.729:0.69 1.778:0.69 1.862:0.82 1.912:0.87 1.996:0.96 2.045:1.00
bs-light-2: ... B=f66@1.996s C=144 f=0.9610 | first-at-1 f67@2.045s C=147
```

- **Why it fails:**
  - At 1.996 s, P is 3 code values from C_full, which is outside the 2-value band. The next recorded frame, at 2.045 s, is at 1.
  - C_full = 147 is the settled value in all four battery-saver runs. J-5's scale-0 dark rest pose read 145 at P.
  - The verdict therefore turns on one code value. I judged it by the line as written, with C_full read after the fade, and did not re-run to replace the sample.
- **The cause is the time from H to the open, not the fade.** The tile settles 0.94–0.97 s after H in the two light-setting runs, against 0.52–0.59 s in the dark-setting runs.
  - In both light-setting runs, H falls on frames of the system splash cross-fade, 7 frames before P reaches the sky.
  - J-5's dark runs without battery saver already reached 1.959 s on this bench.

## Checks
- `shasum -a 256 j5/armC-0c45666.apk` — pass. It is `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b`, and the on-device `sha256sum` matched after the install.
- `git diff --name-only 0c45666 HEAD -- . ':(exclude).planning' ':(exclude)docs'` — pass. Only the two test files listed above changed.
- `python3 -I pace.py <j5/bd23d> <run>` ×10 — pass. D is 486–733 ms (`pace-j5.txt`).
- `python3 -I home6.py <j5/bd23d> s0-* …` ×4 — pass. The scene lands on Home frame #2 in 4/4 runs (`read-s0-home6.txt`).
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` — booted in 12 s. Host load was 3.3 at boot and 3.2–3.7 during the runs.
- `rec.sh bs-{light,dark}-{1,2} 10`, then `bd23d.py`, `pace.py` and `rest.py` — run. Items (1)–(3) pass 4/4 and (4) fails 1/4 (`read-bs.txt`).
- Restore:
  - Battery saver off, Dark theme off and Remove animations on, all through the UI.
  - `install -r found-base.apk`, the bench's own APK pulled at the start, sha `b2ee555f…`.
  - `snap.sh restore db-start`. `cmp` of `db-start` against `db-end` shows all 4 files identical, and `db-start` is identical to J-5's `db-final`.
  - Pass: `diff bench-state-start.txt bench-state-final.txt` differs only in uptime, the APK install path and Data-Free; `low_power=0`.
- `adb shell sync; adb emu kill` — pass. `adb devices` is empty and no `qemu-system` process remains.
- `./gradlew check` — not run. There is no code change and no build in this task.

## Open issues / risks
1. **Battery saver (4) fails by 45 ms in one of four runs**, and the verdict turns on one code value at the end of the fade. It goes to the ux-designer to rule whether "≤ 2.0 s after Home's first recorded frame" was meant to start counting during the system splash cross-fade. H is where P first leaves the splash white, J-5's registered definition, and the starting point for these runs. Counted from Hs, the first frame at sky, `bs-light-2` would be at 1.823 s. I did not re-judge by that reading: it was not registered before the data.
2. **"Two cold starts per theme" cannot be met in light.** On API 36 the system forces the dark theme while battery saver is on, so the product never shows its light theme in battery saver. The ux-designer should restate the count, for example "two cold starts with each system theme setting", or "dark only".
3. **(4) is near its line in dark on this bench even without battery saver** (J-5 `darkL-3`: 1.959 s). Any slower bench or busier host will push it over. That is worth weighing alongside issue 1.
4. A navigation slip, with no change made: one tap meant for Accessibility opened Settings › Battery › Adaptive Battery, because Settings resumed its previous task. Its switch stayed on, as found, and I backed out.

## Handoff
Next: producer — route Open issues 1 and 2 to the ux-designer. BD-23 (d) stays open on the battery-saver clause. Items (1)–(7) are closed, with item (2)'s D and item (6) confirmed by this desk check.

**VERDICT (BD-23 (d) overall): FAIL** — items (1)–(7) PASS, but the battery-saver clause fails.
**VERDICT (battery-saver clause): FAIL** — (4) is 2.045 s against ≤ 2.0 s in `bs-light-2`. Items (1)–(3) and the rest pose hold in 4/4 runs, and light is not reachable in battery saver on this platform.
