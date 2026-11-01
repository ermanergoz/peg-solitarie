# qa-engineer report — candy-backdrop: the J-2 jank re-run and the J-3 15f check (one bench visit)
Date: 2026-11-01
Branch: feature/candy-backdrop (report only). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-candy`, HEAD `dd18270`. Its code equals `599c88d` and `d029950`: `git diff --quiet <sha> HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0 for both. Arm A is `d251c3b` = `develop` = `git merge-base HEAD develop`.
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, testing and gitflow in full. puzzle-design, play-store, kotlin-android, compose, skills-policy, logging, memory and `.claude/rules/lessons.md` were read from the session's rules snapshot. The procedure was reused from `qa-engineer-15e.md` (android-emulator-skill's commands, through adb directly).
Ran in parallel with: none
Ran on: claude-opus-5-5 (the harness's model). The agent file pins fable at high; founder answer Q-019 puts Fable-pinned roles on Opus until 3.0.0 ships.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-signoff.md` J-2 (the corrected (c)) and J-3 (15f); `qa-engineer-15e.md` (procedure, preconditions, bench restore); `tech-lead-direction-b.md` (BD-23); `docs/ux-spec.md` criterion 15f and §6.5's 15f note.

## Summary
- **J-2: VOID, twice.** Attempt 1 (boot 09:13) and the rule's one re-attempt on a fresh cold boot (09:22) both ended with every label short of its kept target. The cause is J-2's own invalid rule: a sample with fewer than 16 `Flags=0` frames is invalid, and this bench gives about one such sample per block. Attempt 1 kept N1 7/8, N2 6/8, A 11/12 and B 9/12, after one block (N1, block 3) went void and was re-run once. Attempt 2 kept N1 6/8, N2 7/8, A 10/12 and B 10/12, with no block void. Under J-2, a second VOID goes to the tech-lead with the numbers (below). I made no third attempt.
- **J-3 / 15f: PASS** on arm B, from three recorded cold starts read frame by frame. Home's first frame shows the ribbon with "—" placeholders (Stars, Boards; Level reads 111 at once). There is no spinner, no blank band and no blocked screen. When 401 and 0/5 arrive, no pill moves: the pill boxes are identical to the pixel in height and in top, bottom and right edges.
- Every J-2 precondition held on both attempts. Start and end `uptime` were ≤ 5.0. Mid-block load readings went above 5 with no Gradle or Kotlin process (Open issue 2).
- The bench was restored: data byte-identical at start, ends and final; scales `0.0 0.0 0.0`; settings unchanged; `sync`, then `emu kill`.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/qa-engineer-jank2.md`: this report.
- Artifacts, git-ignored and never committed, under `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/qa/candy-backdrop/jank2/` (39 MB by `du`; sizes are in bytes). **The framestats dumps are kept for J-4's audit.**

| Path | Size | What it shows |
|---|---|---|
| `pre.txt` | 801 | P-1.2: `config.ini` and `.ini` diffs against `Pixel_9a.avd`, the same three and two keys as 15e; host before the boot; both APK sha256s re-verified |
| `bench-state-start.txt`, `db-start/` | 961 / 4 files | Bench at the first boot: arm B installed, scales 0.0, light, font 1.0, gestural, 1080×2424 @ 420, 0 accounts. The data is byte-identical to `15e/db-start/` |
| `attempt-1/samples/` | 66 files, 1.4 MB with the folder | One full `dumpsys gfxinfo … framestats` per launch, `b<block>-<label>-<n>.txt` (blocks 1–13) |
| `attempt-1/launches.csv`, `swaps.csv`, `values.csv`, `monitor.log` | ~3 KB / ~4 KB / ~7 KB / ~2 KB | Per launch: LaunchState, TotalTime, dump. Per swap: sha match, ART, component, host. Per sample: Flags=0 rows, the value, validity, p50/p90, miss indices, summary lines, `Pipeline=`. Host readings |
| `attempt-1/stats-attempt1.txt`, `run-pre.txt`, `run-post.txt`, `conditions.txt`, `top-0920.txt` | small | Verdict output; start and end `uptime`; scales 1.0 1.0 1.0; the host's top CPU at 09:20 |
| `attempt-1/remove-animations-off.png`, `remove-animations-restored-attempt1.png` | 142,382 / ~141 K | Accessibility › Color & motion: Remove animations off, then back on |
| `attempt-1/boot.txt`, `emulator.log`, `db-end/`, `bench-state-end.txt`, `closing.txt` | small | Boot 09:13:18 → 09:13:30; data at the end (byte-identical); bench down |
| `attempt-2/…` | 0.9 MB | The same set for the re-attempt (blocks 1–12, boot 09:22:21 → 09:22:32), plus `db-final/`, `bench-state-final.txt`, `conditions-end.txt` (0.0 0.0 0.0) and `top-0928.txt` |
| `15f/coldstart-1.mp4`, `-2.mp4`, `-3.mp4` | 748,523 / 801,720 / 825,581 | J-3: three cold starts of arm B, `screenrecord` at 20 Mb/s |
| `15f/f1/`, `f2/`, `f3/` (`f_NNN.png` and `pts.txt`) | 36 MB in all | Every recorded frame (40, 35 and 33 frames) with its presentation time |
| `15f/sheet-<run>-<n>.png` | ~1 MB each | 6×2 contact sheets of those frames, in order |
| `15f/launch-<n>.txt`, `pillcheck.py`, `pillcheck.txt` | 34 each / 1,207 / 516 | Each run's LaunchState and TotalTime; the pill-box measurement and its output |
| `j2.sh`, `j2stats.py`, `info.py`, `info.txt` | 2,545 / 4,950 / 836 / 1,232 | The J-2 driver, the J-2 statistics (written before the boot), and the informative summaries |
| `hostq.sh`, `quiet.sh`, `state.sh`, `snap.sh`, `tap.sh` | small | Copied from `15e/`; `tap.sh` is new, a uiautomator text tap for the Accessibility switch |

**Excerpts** (at most 20 lines):
```
attempt-1 (boot 09:13): block 3 N1: kept values [13] invalid 3  BLOCK VOID  -> re-run as block 13
  N1: n=7 (target 8) values=[9, 10, 10, 11, 12, 13, 15] median=11
  N2: n=6 (target 8) values=[10, 10, 11, 11, 12, 13] median=11.0
  A:  n=11 (target 12) values=[7, 8, 9, 9, 10, 10, 10, 11, 11, 12, 12] median=10
  B:  n=9 (target 12) values=[10, 11, 11, 12, 12, 12, 12, 12, 14] median=12
  VERDICT: VOID (short labels: ['N1', 'N2', 'A', 'B'])
attempt-2 (boot 09:22): no block void
  N1: n=6 (target 8) values=[9, 11, 11, 11, 12, 13] median=11.0
  N2: n=7 (target 8) values=[8, 9, 9, 9, 10, 11, 13] median=9
  A:  n=10 (target 12) values=[8, 8, 9, 9, 10, 10, 10, 10, 10, 11] median=10.0
  B:  n=10 (target 12) values=[9, 9, 10, 11, 11, 11, 11, 12, 13, 13] median=11.0
  VERDICT: VOID (short labels: ['N1', 'N2', 'A', 'B'])
rule: FrameCompleted > FrameDeadline        pipeline lines: {'Pipeline=Skia (OpenGL)'}
15f pills (x0,x1,y0,y1 px), "—" frame vs values frame:
  run2 f_030 [(42,359,1252,1474),(382,698,1252,1474),(720,1037,1252,1474)]
  run2 f_031 [(42,359,1252,1474),(382,698,1252,1474),(720,1037,1252,1474)]
```

## Checks
- `git rev-parse --short HEAD develop` gives `dd18270` and `d251c3b`. `git merge-base HEAD develop` gives `d251c3b…`. `git log d251c3b..develop` is empty, so `develop` gained no start-path commit and 15e is not owed again. Pass.
- `git diff --quiet 599c88d HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0, and the same against `d029950` exits 0. Arm B's code is `d029950`. Pass.
- `shasum -a 256 .planning/qa/candy-backdrop/15e/arm*.apk` gives `aa4d6e2d8189…a053` and `b2ee555f0ccc…eeac`, both as registered. No rebuild, and no Gradle at any point in the visit. Pass.
- Host before the boot (09:12): `adb devices` empty; no `qemu-system`; no Gradle, Kotlin or worker process (`pgrep -fl 'GradleWrapperMain|GradleDaemon|KotlinCompileDaemon|GradleWorkerMain'` printed nothing); load `4.39 3.58 3.22`, with Spotlight's `mds_stores` at ≈130 % CPU.
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`, twice. `emulator.log`: `vulkan_mode_selected:lavapipe gles_mode_selected:host`. `Pixel_9a`, `Pixel_9` and `ergoz_studio` were never started.
- `bash j2.sh WA:A WB:B N1:B N2:B N1:B N2:B`, then `bash j2.sh A:A B:B A:A B:B A:A B:B`, per attempt; attempt 1 also ran the void block again with `bash j2.sh N1:B`. rc 0 every time.
- `python3 -I j2stats.py`: VOID, then VOID (excerpts).
- `bash 15f/rec.sh 1|2|3`, then `ffmpeg -i coldstart-<n>.mp4 -fps_mode passthrough f<n>/f_%03d.png`, then `python3 -I pillcheck.py 1220 1520 <frames>`: 15f PASS.
- `snap.sh … save` and `cmp` against `db-start` after each attempt and at the end: all four files identical each time. Pass.
- Not run: the Check (`./gradlew check`). This was a device visit with no Gradle allowed. `.planning/log/` was not read.

## J-2 — the corrected jank block (BD-23 (c))
**VOID on attempt 1. VOID on the one re-attempt.** Per J-2's verdict rule, this comes to the tech-lead with the numbers.

**Instrument.**
- `Pixel_9a_bench`, API 36, the debug APKs of 15e, a fresh cold boot each attempt.
- Remove animations was switched off through Accessibility only (uiautomator tap; screenshots saved). The scales read `1.0 1.0 1.0` on both attempts (`conditions.txt`). They were restored afterwards to `0.0 0.0 0.0` (`conditions-end.txt`).
- Nothing else ran on either boot before the block. J-3 ran on attempt 2's boot, after the block.

**Value rule** (one rule for the whole run): **`FrameCompleted > FrameDeadline`**.
- The header carries `FrameDeadline`, and columns were read by name.
- The source was the `---PROFILEDATA---` block under `Window: com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
- Rows were ordered by `IntendedVsync`, rows with `Flags ≠ 0` were dropped, and the first 16 were taken.

**Sequence, per attempt.** Blocks of five launches each:
- W-A, W-B (warm-up; all ten recorded, none kept);
- N1, N2, N1, N2 (all on arm B's APK);
- A, B, A, B, A, B.

The first launch of each block was discarded. Each launch ran `am force-stop`; `sleep 2`; `am start -W`; `sleep 1`; `dumpsys gfxinfo com.erman.pegsolitarie framestats` (saved whole). Each block began with `install -r -d --no-incremental`, `compile -m speed -f`, an ART dump, an installed `base.apk` sha check and the component resolved from the device.

**Preconditions, with values:**
1. **Remove animations** (inverted for this block): set off through Accessibility, scales `1.0 1.0 1.0`; restored to `0.0 0.0 0.0` after each attempt. No `settings put` was used.
2. **One emulator:** `emulators=1 [Pixel_9a_bench]` on all 50 monitor readings (26 + 24).
3. **No Gradle or Kotlin:** `wrappers=0 busy=[]` on all 50 readings.
4. **`uptime` ≤ 5.0 at the start and at the end.** Attempt 1: `2.43` at 09:14:33 and `3.73` at 09:21:08. Attempt 2: `3.73` at 09:23:09 and `3.30` at 09:29:25 (load1, as in 15e's `quiet.sh`; the end readings were taken 60 s after the last sample, as in 15e). Held. Mid-run, load1 went from 2.32 to 7.89 (Open issue 2).
5. **ART:** identical on all 25 swaps: `arm64: [status=verify] [reason=cmdline] [primary-abi]`.
6. **Component:** resolved from the device on every swap: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
7. **Swaps:** only at block boundaries; the installed `base.apk` sha matched its arm on 25/25 swaps (`swaps.csv`).
8. **App data:** byte-identical to `db-start` (itself identical to `15e/db-start`) at the start, after each attempt (`db-end`) and at the end (`db-final`), on all four files.
9. **`Data-Free`:** 12,841,392K at the start, 12,846,188K after attempt 1 and 12,844,668K at the end. All ≥ 4 GB.

Every launch was COLD (125/125: 65 in attempt 1 with its re-run block, 60 in attempt 2).

**Invalid samples (fewer than 16 `Flags=0` rows; no other reason occurred).** The value given is TotalTime ms / Flags=0 rows; "d" marks a discarded first launch.

| Attempt | Invalid kept samples | Void blocks |
|---|---|---|
| 1 | N1 b3: 1949/6, 2107/6, 1638/6; N2 b4: 1068/4; N2 b6: 978/5; A b7: 895/10; B b8: 967/5; B b10: 1037/4; B b12: 1401/14; N1 b13 (re-run): 1356/13 | **b3 (N1)**, 3 invalid. Re-run once as b13: 1 invalid, not void |
| 2 | N1 b3: 985/6; N2 b4: 920/7; N1 b5: 988/5; A b7: 908/8; B b8: 1336/7; B b10: 1321/8; A b11: 1510/11 | none |

Short samples still occur in the fast regime (TotalTime ≈ 0.9–1.5 s), and on both arms and on the null. In attempt 1 the slow regime (TotalTime ≈ 1.9–2.4 s, 5–9 frames, all janky) filled both warm-up blocks and block 3, then faded. In attempt 2 it was gone after the warm-up, but about one sample per block still rendered only 3–14 frames in the window. With 12 kept per arm required and no tolerance for a lost sample, one short sample in any arm block VOIDs the run.

**The numbers, for the tech-lead.** These are not a verdict: neither attempt reached its targets, so no floor, T or delta is defined under J-2.

| | N1 median (n) | N2 median (n) | A median (n) | B median (n) |
|---|---|---|---|---|
| Attempt 1 | 11 (7) | 11.0 (6) | 10 (11) | 12 (9) |
| Attempt 2 | 11.0 (6) | 9 (7) | 10.0 (10) | 11.0 (10) |

**Informative only** (`info.txt`; medians over the kept samples, with p50/p90 over the valid ones):

| | TotalTime ms | Frames rendered | p50 / p90 frame ms over the 16 | Misses at frame index 2 (valid samples) |
|---|---|---|---|---|
| A, attempt 1 / 2 | 1089.5 / 987.5 | 28.5 / 33.0 | 83.7 / 208.5; 71.4 / 228.3 | 7 of 11; 3 of 10 |
| B, attempt 1 / 2 | 1035.5 / 1084.0 | 24.0 / 23.0 | 67.4 / 417.5; 67.2 / 392.4 | 9 of 9; 10 of 10 |
| N (B's APK), attempt 1 / 2 | 1049.5–1328.5 / 1039.5–1072.5 | 23.5–24 / 25–26 | ≈67 / ≈390–420 | 13 of 14; 12 of 13 |

- **`Pipeline=`:** `Skia (OpenGL)` on every sample. HWUI rendered on OpenGL (host GLES), not on the lavapipe Vulkan ICD.
- **Per-index miss counts** for every label and attempt are in `info.txt`. The per-sample miss indices are in `values.csv`.
- **On every sample, the first 6 frames (indices 0–5) miss on nearly all of A's and B's runs.** B (and the null, which runs B's APK) also misses index 2 on every valid sample. A misses it on 3 of 10 and 7 of 11. B's p90 is about twice A's. This matches J-4's "scene-ready frame (indices 2–3)" pattern. That is an observation for the developer's audit, not a verdict.

## J-3 — 15f (criterion 15f; `docs/ux-spec.md` §6.5 note)
**PASS.** It ran on attempt 2's boot, after the jank block, with scales restored to `0.0 0.0 0.0` and arm B installed (`b2ee555f…`). It took three cold starts, each with `adb shell screenrecord` started 1 s before `am start -W`. There was no timing gate.

| Run | Launch | First Home frame | Last "—" frame | Values frame | Classic chip lands |
|---|---|---|---|---|---|
| 1 | COLD 2621 ms | f_023, 3.367 s | f_033, 3.899 s | f_034, 3.975 s | f_038, 4.246 s |
| 2 | COLD 2166 ms | f_022, 3.003 s | f_030, 3.474 s | f_031, 3.524 s | f_032, 3.853 s |
| 3 | COLD 1338 ms | f_020, 2.258 s | f_028, 2.466 s | f_029, 2.493 s | f_030, 2.676 s |

(Times are the recording's presentation times, from `f<n>/pts.txt`.)

- **First paint.** In every run the first Home frame is complete: the title, gear, stage with pegs, ribbon and both tiles, with the Challenge tile already showing "Continue — Level 111". The ribbon shows Stars "—", Boards "—" and Level "111". Before it, the frames show only the system splash (the launcher icon on the window background), which is the OS splash, not an app spinner. There is no spinner, no blank band and no blocked screen in any frame.
- **The values arrive with no movement.** "401" and "0/5" replace the dashes in one frame. The pill boxes, measured at full resolution (`pillcheck.txt`, bright-fill threshold), are identical across the arrival in all three runs: `(42,359,1252,1474)`, `(382,698,1252,1474)`, `(720,1037,1252,1474)` px, so the heights, tops, bottoms and right edges match. The middle pill's left edge reads 381 on first paint and 382 a few frames later in all three runs. In runs 2 and 3 that change happens before the values arrive, when the backdrop scene first draws behind the ribbon. It is the threshold meeting the scene's edge (1 px ≈ 0.4 dp), not the emission.
- **After the values.** The Continue Classic chip arrives. The stage gives up its room and the ribbon rides up on its edge, while the mode tiles hold still. That is D-03n's ruled behaviour, separate from 15f, and it is recorded here only so the frames read in full.

## Open issues / risks
1. **J-2 VOID twice (to the tech-lead, per J-2).**
   - The invalid rule (< 16 frames means invalid) fires about once per block on this bench, on both arms and on the null, even outside the slow regime. With exact kept targets and no slack, the instrument cannot reach a verdict here as written.
   - The ruling is the tech-lead's: for example, more launches per block, or a kept target of "at least n valid", registered before any new data, or a different frame source (`FrameTimingMetric`, already on the 3.0.1 list).
   - I did not re-judge anything, and I made no third attempt.
   - The informative table above suggests, but does not show, the J-4 pattern on B (index 2, p90).
2. **Host load during the blocks.** Start and end `uptime` held (≤ 5.0), which is J-2's condition. Inside the blocks, load1 reached 7.89 (attempt 1) and 7.76 (attempt 2). 27 of the 50 readings were ≥ 5. No Gradle, Kotlin or second emulator ran. The top processes were macOS's Spotlight `mds_stores` (102 % CPU at 09:20) and `mediaanalysisd` (70 % at 09:28) (`top-*.txt`). J-2 has no mid-run load clause, so this does not void the run by itself. It may feed the short-sample rate, and the tech-lead may want a mid-run clause, or Spotlight excluded for the AVD folders, before the next sitting.
3. **The bench's resting install is still arm B** (`b2ee555f…`), as 15e left it. Every other setting is unchanged.
4. **Staffing and trailer:** Opus under Q-019 (header). This report's commit carries rules/gitflow.md's single Fable trailer, as the branch's other commits do. That is the same open question the earlier reports raise.

**System changes on `Pixel_9a_bench`, and their fate:**
- Arms A and B were installed in turn, per block; the bench ends on arm B, as found.
- Remove animations went off (scales → 1.0) and back on (→ 0.0) on each boot, through Accessibility only.
- The data is byte-identical to the start, and settings are unchanged against `bench-state-start.txt` (only uptime, `Data-Free` and the APK path differ).
- `adb shell sync`, then `adb -s emulator-5584 emu kill`, after each attempt. No `qemu-system` process remains, and `adb devices` is empty.
- No other AVD was touched.

## Handoff
Next: tech-lead — J-2's second VOID, with the numbers above (the per-attempt medians, the invalid-sample table and the informative frame data). The rule to read is J-2's "a second VOID comes to me". 15f (J-3) is **PASS**. The saved framestats under `.planning/qa/candy-backdrop/jank2/attempt-*/samples/` are there for J-4's audit if one is ordered.

Next: producer —
- This report is committed on the branch, alone.
- The bench is down, and no Gradle ran. P-4 may be released.
- Don't delete `.planning/qa/candy-backdrop/jank2/` until the tech-lead has ruled, since the dumps may feed J-4.

VERDICT: J-2 VOID; 15f PASS
