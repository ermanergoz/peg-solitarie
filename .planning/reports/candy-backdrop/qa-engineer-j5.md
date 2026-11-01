# qa-engineer report — candy-backdrop: J-5 (the corrected settle measurement) and BD-23 (d) (the scene's open and fade), one bench visit
Date: 2026-11-01
Branch: feature/candy-backdrop (report only). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-candy`, HEAD `377007a`. Arm C = `0c45666` (`git diff --quiet 0c45666 HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0). Arm A = `452f1873f0b06b0b2f40c6cb2782f0cbe1297196` = `git merge-base HEAD develop` = `develop`; its code equals `d251c3b` (diff excluding `.planning`, `docs`, `CLAUDE.md`, `AGENTS.md` exits 0).
Skills loaded: ergoz-soft:standards (role qa-engineer; the session's rules snapshot imported by CLAUDE.md: studio, reporting, gitflow, testing, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, `.claude/rules/lessons.md`). The bench procedure and helper scripts were reused from `qa-engineer-jank2.md` and `qa-engineer-15e.md` (android-emulator-skill's commands, through adb directly).
Ran in parallel with: none. The producer held every other agent's Gradle for the visit; I recorded the host on every block (Checks).
Ran on: claude-opus-5-5, effort low (the harness's setting for this dispatch). The agent file pins fable at high; founder answer Q-019 puts Fable-pinned roles on Opus until 3.0.0 ships.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-j2-ruling.md` §3 (J-5); `tech-lead-j4b-ruling.md` §5 (J-5 amendments: `sleep 2`, the ≥ 120-row invalidity, RT_late, the stencil split); `docs/ux/candy-backdrop.md` BD-23 (d) and B-11b; `qa-engineer-jank2.md` and `qa-engineer-15e.md` (procedure, preconditions, restore).

## Summary
- **J-5: PASS on the rule's one re-attempt.** Attempt 1 (boot 11:19) was **VOID**: floor 269.4 ms > 100 ms. The null blocks ran in the bench's slow regime (N1 median 1,022.4 ms against N2 753.0 ms). Attempt 2 (fresh cold boot 11:28) gave median S(C) 458.2 ms against S(A) 400.5 ms, so **Δ = +57.7 ms**. The floor was 30.2 ms and T = 100 ms. Every label met its kept target: 12/12, 12/12, 8/8 and 8/8, and all 120 launches were COLD.
- **RT_late (informative):** C median 49.0 ms against A 32.7 ms. That is much smaller than the 120–290 ms the J-4b ruling expected. **Stencil split (informative):** the rows 2/3 RenderThread time is C 36.2 / 21.1 ms against A 13.9 / 10.3 ms, about two-thirds and one-third of J-2's B excess. A `StencilAttachment` appears in every C dump (12/12) and in no A dump (0/12).
- **BD-23 (d): FAIL against its pass line, on one item in one sample.** On the cold start `light-1`, the fade's rise from 0 to 1 spans **483 ms**, against the ≥ 500 ms in item (2). The other 9 cold starts span 535–798 ms. Every other item passes in every sample: (1), (3), (4), (5) and (7) as read, and (6) under the reading stated below. Battery saver was not run (Open issue 3).
- The bench was restored. The app data is byte-identical to the start. The scales are back to `0.0 0.0 0.0`, the theme is light, and arm B (`b2ee555f…`) is reinstalled, as found. I ran `sync`, then `emu kill`. Both scratch worktrees were removed and no Gradle ran after the builds.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/qa-engineer-j5.md`: this report, the only file committed.
- The artifacts are git-ignored and never committed. They live under `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/qa/candy-backdrop/j5/`, 89 MB by `du` after the extracted PNG frames were deleted (all of them can be re-extracted from the kept recordings). Sizes are in bytes.

| Path | Size | What it shows |
|---|---|---|
| `armA-452f187.apk`, `armC-0c45666.apk`, `apk-sha.txt` | 22,124,904 / 22,240,704 / 330 | The two arms, built with `:app:assembleDebug`. A `aa4d6e2d…a053`, which is byte-identical to J-2's `15e/armA-d251c3b.apk`. C `3e974016…d96b` |
| `build-armA.log`, `build-armC.log` | small | `BUILD SUCCESSFUL in 3s` / `in 5s` |
| `host-prebuild.txt`, `pre.txt` | small | The host before the builds and before the boot. P-1.2: `config.ini` and `.ini` diffs against `Pixel_9a.avd`, the same keys as 15e and J-2 |
| `boot.txt`, `emulator.log`, `bench-state-start.txt`, `db-start/` | small | Boot 11:19:33 → 11:19:45. The bench as found: arm B, scales 0.0, light, font 1.0, gestural, 1080×2424 @ 420. The data is byte-identical to `15e/db-start/` |
| `attempt-1/samples/` (60 dumps), `launches.csv`, `swaps.csv`, `values.csv`, `stats.txt`, `monitor.log` | 1.7 MB in all | J-5 attempt 1 (VOID): one full `dumpsys gfxinfo … framestats` per launch; per-sample S, RT1–3, RT_late, stencil and pipeline |
| `attempt-1/conditions*.txt`, `remove-animations-*.png`, `run-pre/post.txt`, `top-*.txt`, `db-end/` | small | Scales `1.0 1.0 1.0`, then `0.0 0.0 0.0`. Host readings. Data byte-identical after attempt 1 |
| `attempt-2/…` | 1.6 MB | The same set for the re-attempt (the run of record), plus `boot.txt` (11:28:34 → 11:28:45), `pre-boot.txt` and `emulator.log` |
| `j5.sh`, `j5stats.py` | 2,600 / 4,900 | The J-5 driver (J-2's, with `sleep 2`, arms A/C), and the statistics, written before the boot. I dry-ran the statistics on J-2's attempt-2 dumps: they reproduce the tech-lead's §1.3 (A 367.1, B 591.6, floor 38.8) |
| `bd23d/light-1..5.mp4`, `darkL-1..5.mp4` | 2.0–2.5 MB each | BD-23 (d): five recorded cold starts per theme, animations on, `screenrecord` at 20 Mb/s |
| `bd23d/<run>/pts.txt`, `launch-<run>.txt` | small | Each recorded frame's presentation time, and each run's LaunchState and TotalTime |
| `bd23d/dark-1..5.mp4` | 0.9–1.6 MB | Discarded: the 6 s recording window ended before the dark fade finished, so C_full cannot be read (Checks) |
| `bd23d/tap-dark-1.mp4`, `tap-dark-2.mp4`, `-end.png`, `-top.txt` | ~3 MB / 230 K | Item (5). Run 1's tap landed before the open. Run 2's landed during the fade, and the catalogue opened |
| `bd23d/return-dark.mp4`, `-pts.txt`, `-end.png` | 2,119,459 / 747 / 361,183 | Item (7): catalogue → Home |
| `bd23d/s0-light-1..2.mp4`, `s0-dark-1..2.mp4`, `s0-dark-2-frames.png` | 0.6–0.8 MB / 167,419 | Item (6), at animator scale 0. The PNG shows Home's frames 1–2 and the frames either side of the scene's appearance |
| `bd23d/pair-preopen-full.png`, `sheet-light-1.png`, `sheet-darkL-1.png` | 437,091 / ~1.4 MB each | The frame before the open next to the fully-in frame (light, dark). Contact sheets of two runs |
| `bd23d/read-light.txt`, `read-dark.txt`, `read-s0-*.txt` | ~3 K each | The per-run readings quoted below |
| `bd23d/rec.sh`, `tapfade.sh`, `frames.py`, `bd23d.py`, `s0.py`, `appframes.py` | small | The recording and analysis helpers |
| `bd23d/dark-theme-on.png`, `dark-theme-off.png`, `remove-animations-on.png` | ~140–190 K | Settings › Accessibility › Color & motion: the theme and Remove animations toggles, set and restored through the UI |
| `bench-state-final.txt`, `db-end/`, `db-final/`, `closing.txt` | small | The bench at the end. `db-end` differs from the start in `app_prefs` (`app_last_foreground_at` was added by the BD-23 runs); it was restored, and `db-final` is byte-identical |
| `hostq.sh`, `quiet.sh`, `state.sh`, `snap.sh`, `tap.sh` | small | Copied from `jank2/` |

**Excerpts** (at most 20 lines):
```
attempt-1: N1 n=8 median=1022.4  N2 n=8 median=753.0  A n=12 median=371.1  C n=12 median=466.0
  floor=|1022.4-753.0|=269.4 T=538.7 delta=94.8   VERDICT: VOID (floor > 100)
attempt-2: N1 n=8 median=457.2  N2 n=8 median=487.4  A n=12 median=400.5  C n=12 median=458.2
  floor=|457.2-487.4|=30.2 T=max(100,2*floor)=100.0 delta=median_C-median_A=458.2-400.5=57.7
  VERDICT: PASS          pipeline lines: {'Pipeline=Skia (OpenGL)'}
  info A: TotalTime 1006.5 RT1 139.7 RT2 13.9 RT3 10.3 RT_late 32.7 (idx 45.5, +1325.4 ms) stencil none x12
  info C: TotalTime 1018.5 RT1 208.1 RT2 36.2 RT3 21.1 RT_late 49.0 (idx 26.5, +1024.5 ms) stencil 7.50 MB (3) x7, 7.56 MB (4) x5
light-1: H=f41@2.896s T(tile settled)=f48@3.246s C_sky=(225,194,240) C_full=(205,138,230) ch=G
  (2) rise last-0 f52@3.476 -> first-1 f60@3.959 span=483 ms -> FAIL
  series: 0.516:0.00 0.580:0.02 0.597:0.12 0.647:0.27 0.668:0.39 0.696:0.46 0.814:0.64 0.898:0.75 0.996:0.86 1.063:0.96 1.096:0.96 1.130:1.00
s0-dark-2: Home frame 1 f17@2.085 (sky); app content unchanged f18–f29 (status bar only); f30@2.656 scene fully in; no intermediate fraction
tap-dark-2: fraction 0.05 at 4.776 s, 0.23 at 4.828 s, navigation under way at 4.828 s; end screen: "Classic" catalogue
```

## Checks
- **Git:**
  - `git log -1 452f187…` is the `develop` merge.
  - `git merge-base HEAD develop` and `git rev-parse develop` both give `452f1873…`.
  - The code-equality diffs for A against `d251c3b` and for C against HEAD exit 0. Pass.
  - `git diff --name-only d029950 0c45666 -- app/src/main` gives only `ui/backdrop/BackdropMotion.kt`, `BackdropSceneReady.kt` and `SceneBackdrop.kt`.
- **Host before the builds (11:18):** load `3.21`. No wrapper ran, and no daemon or worker was busy. Two idle JVMs were present (a Gradle and a Kotlin daemon, 0 %). `adb devices` was empty.
- **Builds:** `git worktree add --detach ../PegSolitarie-j5-armA 452f187…` and `../PegSolitarie-j5-armC 0c45666`, with `local.properties` copied in. `./gradlew --console=plain :app:assembleDebug` in each passed, in 3 s and 5 s, from the build cache. Then `./gradlew --stop` ("1 Daemon stopped"), `git worktree remove --force` on both, and `git worktree prune`. No Gradle ran after 11:18.
- **Hashes:** `shasum -a 256` gives A `aa4d6e2d…a053` and C `3e974016…d96b`. The installed `base.apk` matched its arm on 12/12 swaps in each attempt (`swaps.csv`).
- **Emulator:** `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`, twice. `emulator.log` shows `vulkan_mode_selected:lavapipe gles_mode_selected:host`. `Pixel_9a` and `Pixel_9` were never started.
- **J-5 runs:** `bash j5.sh WA:A WC:C N1:C N2:C N1:C N2:C`, then `bash j5.sh A:A C:C A:A C:C A:A C:C`, per attempt; rc 0 every time. `python3 -I j5stats.py`: VOID, then PASS.
- **J-5 preconditions, both attempts:**
  - every launch COLD (60/60 each);
  - ART `arm64: [status=verify] [reason=cmdline] [primary-abi]` on 12/12 swaps;
  - component `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`;
  - `emulators=1 [Pixel_9a_bench]` and `wrappers=0 busy=[]` on all 24 monitor readings of each attempt;
  - Remove animations off through Accessibility only, with scales `1.0 1.0 1.0`, and restored after attempt 1 (`0.0 0.0 0.0`);
  - no PROFILEDATA block held ≥ 120 rows (the maximum was 88);
  - no sample invalid.
- **J-5 host load.** Start and end `uptime`: attempt 1 `3.32` / `3.81`, attempt 2 `2.63` / `3.26`. Mid-run load1 ranged 2.42–6.40. It was ≥ 5 on 9 of 24 readings in attempt 1 and 3 of 24 in attempt 2. These readings are informative only under J-5. The top process was Spotlight's `mds_stores` (78–148 % CPU, `top-*.txt`).
- **BD-23 (d) recordings:**
  - `bash bd23d/rec.sh <run> [secs]`, then `python3 -I bd23d.py <run>`, `s0.py <run> <C_sky> <C_full>` and `appframes.py <run> <from> <to>` (readings below).
  - The first dark set (`dark-1..5`, 6 s windows) was re-recorded as `darkL-1..5` with 10 s windows. Dark cold starts took 1.8–2.8 s, and the 6 s window ended before the fade finished, which left C_full unreadable. That is an instrument fault, not a product reading. I judged none of those five.
- **Bench restore:**
  - `snap.sh save db-end`: `app_prefs` differed (`app_last_foreground_at` added). Then `snap.sh restore db-start` and `snap.sh save db-final`: all four files identical to `db-start`.
  - `state.sh` at the end differs from the start only in uptime, the APK install path and `Data-Free`. The sha is `b2ee555f…`, the scales are 0.0, `Night mode: no`, and font 1.0.
  - `adb shell sync`, then `emu kill`. No `qemu-system` remained and `adb devices` was empty.
- **Not run:** the Check (`./gradlew check`). This was a device visit with no Gradle allowed after the builds; the Check belongs to the developer, the auditor and gsd. `.planning/log/` was not read.

## J-5 — the corrected settle measurement (J-2 ruling §3, amended by J-4b §5)
**VERDICT: PASS (attempt 2, the rule's one re-attempt after attempt 1's VOID).**

**Instrument.**
- `Pixel_9a_bench`, API 36, debug APKs, a fresh cold boot per attempt, `Pipeline=Skia (OpenGL)` on every sample.
- Sequence: W-A, W-C; N1, N2, N1, N2 (all on C's APK); A, C, A, C, A, C; blocks of five, with the first launch of each block discarded.
- Per launch: `am force-stop`; `sleep 2`; `am start -W`; **`sleep 2`**; `dumpsys gfxinfo com.erman.pegsolitarie framestats`, saved whole.
- Value: from the MainActivity window's `PROFILEDATA`, columns by name, take the rows with `Flags` = 0 and `FrameCompleted > IntendedVsync`, ordered by `IntendedVsync`. Then **S = FrameCompleted(row 4) − FrameCompleted(row 0)**.
- Valid: COLD, readable, at least 5 such rows, and fewer than 120 rows in the block.

**Attempt 1 (boot 11:19:33), VOID.**

| Label | n kept | S median (ms) | S values (ms) |
|---|---|---|---|
| N1 | 8 | **1,022.4** | 433.5, 865.4, 946.1, 974.4, 1,070.3, 1,218.3, 1,282.9, 1,286.9 |
| N2 | 8 | **753.0** | 364.5, 437.3, 483.1, 549.7, 956.2, 967.9, 1,014.7, 1,250.5 |
| A | 12 | 371.1 | 337.4 … 733.5 |
| C | 12 | 466.0 | 383.1 … 880.4 |

- floor = |1,022.4 − 753.0| = **269.4 ms > 100 ms → VOID**. Its Δ (+94.8 ms) carries no verdict.
- The cause is the bench's slow regime: TotalTime 1.4–2.0 s and RT1 350–550 ms on the null blocks 3–5, which then faded by block 6. This is the same regime J-2's attempt 1 met.

**Attempt 2 (fresh cold boot 11:28:34), the run of record.**

| Label | n kept (target) | S median (ms) | S values, sorted (ms) |
|---|---|---|---|
| N1 | 8 (≥ 6) | **457.2** | 366.6, 411.9, 432.9, 433.4, 481.0, 495.4, 518.0, 533.4 |
| N2 | 8 (≥ 6) | **487.4** | 383.2, 416.7, 450.1, 462.9, 511.8, 517.9, 534.9, 900.4 |
| A `452f187` | 12 (≥ 10) | **400.5** | 348.9, 356.8, 366.4, 366.4, 383.2, 384.2, 416.8, 417.1, 433.7, 466.7, 466.9, 526.0 |
| C `0c45666` | 12 (≥ 10) | **458.2** | 373.0, 383.3, 395.8, 403.6, 439.3, 449.6, 466.7, 500.0, 519.4, 535.4, 550.2, 573.2 |

- floor = |457.2 − 487.4| = **30.2 ms**, and T = max(100, 60.4) = **100 ms**.
- Δ = 458.2 − 400.5 = **+57.7 ms ≤ 100 → PASS**.
- No extra block was needed, and there were no invalid samples.
- For reference, J-2's arm B was +216 / +225 ms on the same measure.

**Informative only (never in the verdict).** Medians over the kept samples, attempt 2, with attempt 1 in brackets:

| | TotalTime | RT1 | RT row 2 | RT row 3 | RT_late | RT_late row index | RT_late offset from row 0 |
|---|---|---|---|---|---|---|---|
| A | 1,006.5 (967.5) | 139.7 (113.6) | 13.9 (17.2) | 10.3 (5.4) | **32.7** (42.8) | 45.5 (50.5) | 1,325 ms (1,424) |
| C | 1,018.5 (989.0) | 208.1 (203.2) | 36.2 (39.7) | 21.1 (23.0) | **49.0** (50.0) | 26.5 (25.0) | 1,025 ms (963) |

- **RT_late:** C's median is **49.0 ms**, against A's 32.7 ms, a difference of +16 ms. In 6 of C's 12 samples, the late maximum sits at rows 7–10, about 590–640 ms after row 0. That is where the open falls (440 ms of animator time plus the stalled frames). There it reads 36–59 ms. In the other six it sits later, at rows 43–62, at 43–72 ms. This is well below J-4b's expected 120–290 ms. In the Q-PUBLISH material it reads as "one stall of about 50 ms on a still Home, about 0.6 s after the entrance".
- **Stencil split:**
  - C's rows 2 / 3 take 36.2 / 21.1 ms, against A's 13.9 / 10.3 ms, an excess of **+22.3 / +10.8 ms**. J-2's B over A was +33.4 / +29.1 ms (the tech-lead's §1.2 medians).
  - `StencilAttachment`: in no A dump (0/12, both attempts). In every C dump: 7.50 MB (3 entries) in 7 and 7.56 MB (4 entries) in 5 (attempt 1: 8 and 4).
  - Reading, per J-4b §5.3: C's rows 2–3 fall part of the way toward A, not all of it. The scene is not drawn on those rows in C, so the remaining +22 / +11 ms, and RT1's +68 ms, belong to Home's own finishes. The stencil line by itself cannot separate the two sources, because a dump taken 2 s after launch includes the scene's draw.

## BD-23 (d) — the open and the fade, on the device (B-11b)
**VERDICT: FAIL against the pass line, on item (2) for one of the ten cold starts (`light-1`: 483 ms against ≥ 500 ms). Every other item passes.**

**Conditions.**
- Arm C installed. Animations on: Remove animations off, scales `1.0 1.0 1.0`.
- Cold starts: `am force-stop`, 2 s, then `am start -W`, with `screenrecord` started 1 s earlier.
- Light: the bench's resting theme. Dark: Settings › Accessibility › Color & motion › Dark theme, through the UI, and restored the same way.
- `screenrecord` writes a frame only when the composition changes. Its interval is 16 ms at best and 17–420 ms on this bench, so "60 fps" is the display rate, not a frame per 16.7 ms (Open issue 2).

**Sample pixel P = (727, 2302) px.**
- It is on the front wave in Home's footer: 120 px below the Challenge tile's bottom edge (about 2,057 px) and above the gesture handle (about 2,385 px).
- Its column (x = 727) is clear of every container's footprint below the tiles, and of the lights' columns. I located the lights on the fully-in frame at about x 30–140, 270–510, 500–580, 800–860 and 920–1,050 px, and checked them across the settled frames: P's value is constant on every settled frame of every run.
- The channel is the one with the largest |C_full − C_sky|:
  - light: G, C_sky (225, 194, 240), C_full (205, 138, 230), Δ = −56;
  - dark: B, C_sky (53, 13, 70), C_full (103, 33, 145), Δ = +75.
- **Tolerance reading.** I read "within ±2/255" as ±2 code values on that channel: fraction tolerance 2/56 in light and 2/75 in dark.
  - I chose this after seeing `light-1`'s raw pixel trace, and before computing any item.
  - After that run's item (2) failed, I briefly applied the literal reading instead (±2/255 on the fraction itself). Under it every light run passes (2), and `light-1` spans 614 ms.
  - I went back to the code-value reading, for two reasons. On this probe one code value is already 1/56 to 1/75 of the fraction, so a fraction tolerance of 2/255 would allow no noise at all. And it was the reading I fixed before any result.
  - Both readings are recorded here (Open issue 1).

**Definitions in `bd23d.py`.**
- **H**, Home's first recorded frame: the first frame, after the system splash's first white frame, where P leaves white.
- **Hs**: the first frame where P is within 2 levels of C_sky. The 0–6 frames between H and Hs are the system splash's cross-fade into the window. In dark that blend reads as a positive fraction, because the splash's white sits on the C_full side of the dark sky.
- **T**, the Challenge tile settled: the first frame from which four interior tile probes, (300|776, 1830|2035), equal their final values within ±3 to the end of the recording.
- **C_sky** = P at T. **C_full** = P on the last frame.

**Per-sample results.** Times are seconds of recording, measured from H. "Rise" runs from the last frame at fraction 0 to the first frame at 1.

| Run | TotalTime | T − H | (1) max f on [Hs, T]; margin and footer probes | (2) drops / rise span | (3) max step | (4) fully in after H |
|---|---|---|---|---|---|---|
| light-1 | 1,790 | 0.350 | 0.000; ≤ 3 levels | 0 / **483 ms ✗** | 0.179 | 1.063 |
| light-2 | 1,294 | 0.298 | 0.000; ≤ 3 | 0 / 663 | 0.286 | 1.148 |
| light-3 | 1,172 | 0.414 | 0.000; ≤ 4 | 0 / 549 | 0.125 | 1.132 |
| light-4 | 1,546 | 0.316 | 0.000; ≤ 3 | 0 / 563 | 0.232 | 1.048 |
| light-5 | 1,853 | 0.380 | 0.000; ≤ 2 | 0 / 535 | 0.232 | 1.097 |
| darkL-1 | 2,355 | 0.584 | 0.000; ≤ 1 | 0 / 609 | 0.333 | 1.657 |
| darkL-2 | 2,274 | 0.861 | 0.000; ≤ 1 | 0 / 548 | 0.187 | 1.784 |
| darkL-3 | 2,230 | 0.699 | 0.000; ≤ 1 | 0 / 798 | 0.253 | 1.959 |
| darkL-4 | 2,065 | 0.893 | 0.000; ≤ 3 | 0 / 659 | 0.200 | 1.731 |
| darkL-5 | 2,091 | 0.963 | 0.000; ≤ 3 | 0 / 654 | 0.267 | 1.715 |

- **(1) PASS, 10/10.**
  - P sits exactly at C_sky from Hs through T, and on to the first frame of the open, in every run. The open's first non-zero frame comes 0.18–0.25 s (light) and 0.18–0.61 s (dark) after T.
  - "No wave or light pixel anywhere": 47 probes in the side margins (x = 8 and 1,072, y = 200–2,300) and on the footer row (y = 2,385) deviate by at most 4 levels across the sky-stable frames before T. That is codec noise; a wave would move them by about 20–75.
  - `pair-preopen-full.png` shows the frame before the open, sky only, next to the fully-in frame, for light and dark.
- **(2) FAIL on light-1; the other nine pass.**
  - No run shows a decrease beyond 2 levels after T. In `darkL-2` the steady front wave moves P by exactly 2 levels after it is fully in, which is within tolerance.
  - `light-1`'s rise reaches 0.02 at 0.580 s and 0.964 (2 levels from full) at 1.063 s. That is a 483 ms span, 17 ms short. The exact 1.00 arrives at 1.130 s, and the exact-0 to exact-1 span is 614 ms.
  - The fade is linear over 700 ms, so the shortfall comes from the recorder's sparse frames. The last frame within 2 levels of the sky and the first within 2 levels of full are 483 ms apart, while the two exact ends are 614 ms apart. It is not a shorter fade. I still report the verdict the line gives.
- **(3) PASS, 10/10.** The largest single step is 0.333 (`darkL-1`, about 30 ms after a 125 ms recorder gap). No step exceeds 0.50, so there is no pop.
- **(4) PASS, 10/10.** The scene is fully in 1.05–1.15 s (light) and 1.66–1.96 s (dark) after H. The worst case is `darkL-3` at 1.959 s, 41 ms inside the 2.0 s line. Dark cold starts on this bench are 0.5–1 s slower to settle the tile (T − H 0.58–0.96 s against light's 0.30–0.41 s).
- **(5) PASS (dark, `tap-dark-2`).** I tapped the Classic tile (540, 1640) 1.1 s after `am start -W` returned. P read 0.05 at 4.776 s and 0.23 at 4.828 s, and the navigation's cross-fade was under way by 4.828 s, so the tap landed during the fade. The end screen is the Classic catalogue (`tap-dark-2-end.png`). The first try (`tap-dark-1`, a 0.5 s delay) landed before the open: the navigation started with P still at 0. It also opened the catalogue, but it does not count for (5).
- **(6) PASS, on the reading below (light ×2, dark ×2, animator scale 0).**
  - In every run P goes from C_sky to C_full in one recorded frame, with no intermediate fraction.
  - The recorded frames between Home's first frame and that frame change only the status bar: the sampled app area (y 150–2,330, every 6 px) differs by at most 6 levels on at most 6 samples (`appframes.py`). The next frame that changes the app's content is the one with the scene fully in. It comes 0.46–0.57 s later in light and 0.40–0.57 s later in dark (the bench's frame-1 to frame-2 main-thread stall).
  - So the scene is fully present on Home's second frame, counting the frames in which Home's own content changes. Counted literally, "Home's second recorded frame" is a status-bar-only frame, and on that count (6) would fail (Open issue 1).
- **(7) PASS (dark, `return-dark`).** On Back from the catalogue, P rises monotonically from the catalogue's (26, 13, 53) to C_full over 1.049 → 1.612 s (the 700 ms cross-fade), then holds at C_full for 2.26 s to the end of the recording. It never returns to C_sky, and no second delayed fade follows.
- **Battery saver:** not run. Battery saver is not among this dispatch's items, and turning it on through the device's settings is a system change I would rather leave to an explicit instruction (Open issue 3).

## Open issues / risks
1. **BD-23 (d)'s pass line has two readings that decide the verdict** (for the ux-designer, who owns the line):
   - item (2)'s tolerance: ±2 code values (my reading; `light-1` fails at 483 ms) against ±2/255 on the fraction (every run passes, `light-1` at 614 ms);
   - item (6)'s "Home's second recorded frame": counting only frames that change Home's content (my reading; pass) against counting every compositor frame, including status-bar-only ones (fail).

   I did not re-run `light-1` or add samples. The failing sample is 17 ms short, it comes from the recorder's frame spacing, and the fade itself is linear and unchanged. If the owner rules that the line is contradictory, the verdict above still stands, marked `spec contradiction`.
2. **The recorder is not 60 fps on this bench.** `screenrecord` writes frames only on change, at intervals of 16–420 ms. Every time in BD-23 (d) is a recording time, and has the resolution of the frame gaps around it.
3. **Battery saver** ("in battery saver, (1)–(4) hold at the rest pose") was not covered at this visit. The producer can order it, through the Settings UI, at a later sitting.
4. **J-5 attempt 1 VOID.** The bench's slow regime (TotalTime about 1.4–2.0 s) ran through three null blocks while Spotlight's `mds_stores` took 78–148 % CPU. The re-attempt on a fresh boot was clean. Excluding the AVD folders from Spotlight (J-2 ruling, Open issue 4) is still the cheap fix.
5. **The informative residue on frame 1:** C's RT1 is +68 ms over A's, and rows 2–3 are +22 / +11 ms. The scene does not draw there, so this is the start-path cost of Home's finishes, consistent with 15e's +44.5 ms TTID. RT_late is about 50 ms, not 120–290. Both figures are for the Q-PUBLISH-3.0.0 material.
6. **Trailer:** this commit carries the session's attribution lines, the same open question as the branch's other reports.

**System changes on `Pixel_9a_bench`, and what became of them:**
- Arms A and C were installed in turn. The bench ends on arm B (`b2ee555f…`), as found.
- Remove animations went off and on again through Accessibility, twice, and ends on (scales 0.0).
- Dark theme went on and off through Accessibility › Color & motion, and ends off.
- `app_last_foreground_at` was written by the BD-23 runs and removed by restoring `db-start`; the data is byte-identical.
- `sync`, then `emu kill`, after each boot. No other AVD was touched.

## Handoff
Next: producer —
- **J-5 PASS** (Δ +57.7 ms ≤ 100; floor 30.2; attempt 2 after attempt 1's VOID): re-dispatch the tech-lead with this report for the sign-off addendum (J-4b ruling, Handoff 4).
- **BD-23 (d) FAIL** on item (2) for one of the ten cold starts, by 17 ms, under the code-value reading: send Open issue 1 to the ux-designer, who rules on the line's readings. QA does not re-judge old numbers against a new line; a corrected line is judged on a new run.
- **For the Q-PUBLISH-3.0.0 material:** RT_late (C about 49 ms, A about 33 ms) and the frame-1 residue (Open issue 5).
- The bench is down and the host's Gradle hold can be lifted. The dumps and recordings stay under `.planning/qa/candy-backdrop/j5/` until the tech-lead and the ux-designer have ruled.

VERDICT: J-5 PASS; BD-23 (d) FAIL (item (2), light-1, 483 ms < 500 ms; all other items PASS)
RT_late (informative): C 49.0 ms, A 32.7 ms (medians, attempt 2)
Stencil split (informative): RT rows 2/3 C 36.2/21.1 ms, A 13.9/10.3 ms; StencilAttachment C 12/12 (7.50–7.56 MB), A 0/12
