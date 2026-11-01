# qa-engineer report — candy-backdrop: the final 15e sitting (BD-23 (b)), the jank block (BD-23 (c)), and the BD-09 desk re-read
Date: 2026-11-01
Branch: feature/candy-backdrop (report only). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-candy`, head `599c88d`. Its code equals `d029950`: `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0. Arm A is `d251c3b`, arm B is `599c88d`.
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, testing, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android and compose in full, plus `.claude/rules/lessons.md`. Also loaded: android-emulator-skill, android-testing.
Ran in parallel with: none. The producer paused all other agents' Gradle work (P-4). I checked the host before and during the sitting (Checks).
Ran on: claude-opus-5-5 (the harness's model). The agent file pins fable at high; founder answer Q-019 puts Fable-pinned roles on Opus until 3.0.0 ships. Every verdict below rests on a device capture or a number read from one.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-direction-b.md` A6 (BD-23 (b) and its validity clause) and the A10 row for BD-23 (b), (c); `tech-lead-architecture.md` R3 (the sitting, and the BD-23 (c) jank block: its protocol and pass line) and D8; `tech-lead-merge-ruling.md` R-3; `docs/ux-spec.md` §6.5, second edition (nine preconditions, null control, threshold and verdict); the field shape of `colour-vision/qa-engineer-15e.md` and `home-chip-arrival/qa-engineer-sitting2.md`; `ux-designer-bd09.md` and the amended BD-09 (B-09b) in `docs/ux/candy-backdrop.md`.

## Summary
- **15e (BD-23 (b)): PASS** on `Pixel_9a_bench`, first attempt on the 08:44 cold boot. Delta **+44.5 ms** (B 455.0, A 410.5), floor **6.5 ms**, threshold **100 ms**. All nine preconditions held, and the monitor read a quiet host with the bench as the only emulator on all 38 readings.
- **Jank block (BD-23 (c)): FAIL against its pass line.** Median Janky frames were B **12.0** and A **10.5**, against the line `median_B ≤ median_A + 1` = 11.5. There were 12 kept samples per arm, the host was quiet on all 18 readings, and every sample was COLD. Blocks 1–2 ran in a slow regime (TotalTime ≈ 1.9–2.4 s, about 6 frames, all of them janky). Blocks 3–6 ran at ≈ 1.0 s and about 30 frames (Open issue 1). I did not re-run it: nothing off the device voided it, and the line has no VOID clause.
- **BD-09 (B-09b): PASS** on all eight screens in both themes, read from existing captures. One new capture was needed, the light tutorial, and I took it on the bench after the closing readings. In every capture the strip is exactly `background` (0 off pixels, maximum deviation 0/255). The clock reads 5.57:1 in light and 17.66:1 in dark; the bench's clock reads 18.81:1. The status icons read at least 3.27:1.
- The bench data is byte-identical at the start, after the sitting and at the end. The animation scales were restored to 0.0. I ran `sync`, then `emu kill`. Both arm worktrees were removed.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/qa-engineer-15e.md`: this report.
- Artifacts, git-ignored and never committed, under `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/qa/candy-backdrop/15e/` (43 MB by `du`, of which about 42 MB is the two APKs; sizes are in bytes):

| Path | Size | What it shows |
|---|---|---|
| `armA-d251c3b.apk` | 22,124,904 | Arm A, `:app:assembleDebug` at `d251c3b`, sha256 `aa4d6e2d8189fb3dbacee50625684a22f6b2c5bfe4bbc7f9f0116a22e57fa053` |
| `armB-599c88d.apk` | 22,224,320 | Arm B, `:app:assembleDebug` at `599c88d`, sha256 `b2ee555f0ccc4fb2ace08c10d8f0c5e1e3d234f59d6744a73eee031b4138eeac` |
| `build-armA.log`, `build-armB.log` | 3,833 / 3,811 | Both `BUILD SUCCESSFUL` (08:43), each in its own detached worktree |
| `sitting/pre.txt` | 679 | P-1.2: `config.ini` and `.ini` diffs against `Pixel_9a.avd`; host at 08:44; launcher idle from 08:44:58; run start |
| `boot.txt`, `emulator.log` | 44 / 8,798 | Cold boot at 08:44:12, `sys.boot_completed` at 08:44:24 |
| `bench-state-start.txt`, `bench-state-end.txt`, `bench-state-final.txt` | 961 / 962 / 962 | Bench settings, installed sha, `Data-Free` and accounts at the start, after the jank block, and after the BD-09 capture |
| `db-start/`, `db-end/`, `db-final/` | 4 files each | App data pulls. All three are byte-identical to `home-chip-arrival/…/device/db-start/` |
| `sitting/attempt-1/` | — | **The sitting of record.** `samples.csv` (100 launches, 3,805), `swaps.csv` (20 swaps, 5,260), `monitor.log` (38 readings, 3,002), `before.txt`, `after.txt`, `stats.txt` (889), `null.log`, `arms.log` |
| `quiet-poll.log` | 158 | The two host polls before the attempt |
| `jank/` | — | BD-23 (c): `jank.csv` (30 samples, 1,935), `swaps.csv` (6 swaps, 1,765), `monitor.log` (18 readings, 1,422), `stats.txt` (444), `conditions.txt` (scales 1.0/1.0/1.0), `run.log` |
| `jank-pre.txt` | 144 | Host and `uptime` before the jank block |
| `jank-remove-animations-off.png`, `jank-remove-animations-restored.png` | 142,669 / 141,982 | Accessibility › Color & motion: Remove animations off for the block, then on again |
| `closing.txt` | 211 | Closing `uptime`, host line, key count, shutdown time |
| `bd09.py`, `bd09-light.txt`, `bd09-dark.txt` | 2,173 / 9,762 / 7,546 | BD-09 desk read of the existing captures in `.planning/qa/candy-backdrop/` |
| `bd09-tutorial1-light-bench.png`, `-b.png`, `bd09-tutorial-light-band.png`, `bd09-tutorial-light.txt` | 115,328 / 115,469 / 7,503 / 1,232 | The light tutorial, settled (step 1 of 3, two captures 3 s apart); its strip crop; its reading |
| `run.sh`, `sitting.sh`, `hostq.sh`, `quiet.sh`, `state.sh`, `snap.sh`, `stats.py` | small | Copied from colour-vision's `15e/` and re-keyed to the two APKs |
| `jank.sh`, `jstats.py` | 2,519 / 1,025 | New: the jank driver and its statistics, as R3 states them |

**Excerpts** (at most 20 lines):
```
15e attempt-1:
N1: n=20 min=424 median=456.0 max=493 non-cold=0
N2: n=20 min=414 median=462.5 max=535 non-cold=0
A: n=20 min=350 median=410.5 max=462 non-cold=0
B: n=20 min=409 median=455.0 max=473 non-cold=0
floor=|456.0-462.5|=6.5 threshold=max(2*floor,100)=100.0
delta=median_B-median_A=455.0-410.5=44.5 verdict=PASS
monitor not-quiet lines: 0 of 38
ART on 20/20 swaps: arm64: [status=verify] [reason=cmdline] [primary-abi]
jank (scales 1.0 1.0 1.0):
A: n=12 janky min=7 median=10.5 max=12 non-cold=0
B: n=12 janky min=5 median=12.0 max=18 non-cold=0
median_B=12.0 median_A+1=11.5 verdict=FAIL
jank monitor not-quiet lines: 0 of 18
BD-09 light: strip off(>1)=0 maxdev=0 on 16 captures; clock core (98,96,100) 5.57:1; icons 5.57:1
BD-09 dark:  strip off(>1)=0 maxdev=0 on 12 captures; clock and icons (255,255,255) 17.66:1
BD-09 tutorial light (bench): off(>1)=0; clock (0,0,0) 18.81:1; signal bars (135,133,138) 3.27:1
```

## Checks
- `git rev-parse HEAD` gives `599c88d`. `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0. Pass.
- `git rev-parse develop` and `git merge-base HEAD develop` both give `d251c3b`, so `develop` has not moved and arm A is `d251c3b`. Pass.
- `git diff --stat d251c3b HEAD -- app/schemas` is empty. `git diff d251c3b HEAD -- app/src/main | grep -cE '^[-+].*(PreferencesKey|@Entity|@Database)'` gives 0. Both arms share the schema and the keys, which is A6's validity clause. Pass.
- `git diff --name-only d251c3b HEAD`, filtered to the start-path list, gives `navigation/`, `ui/home/` and `ui/theme/` files, so the sitting is owed.
- `git worktree add --detach ../PegSolitarie-armA-candy d251c3b` and `… ../PegSolitarie-armB-candy 599c88d`, with `local.properties` copied in. Then `./gradlew --console=plain :app:assembleDebug` in each (load 2.55 at the start). Pass, 3 s and 5 s, from the build cache. Then `./gradlew --stop` ("1 Daemon stopped"). After that no Gradle or Kotlin JVM remained, and there was no further Gradle until the end. Both worktrees were removed with `git worktree remove --force` and pruned.
- `shasum -a 256` of both APKs: as in Outputs. The installed `base.apk` read back from the device matched its arm on 20/20 sitting swaps and 6/6 jank swaps.
- Host before the boot (08:43): no `qemu-system`; `adb devices` empty; three idle JVMs at 0.0 % CPU, then none after `--stop`.
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`: pass. `emu avd name` = `Pixel_9a_bench`, SDK 36.
- `state.sh`, `snap.sh save db-start` and `sqlite3` on a copy:
  - `level_result`: challenge 200 rows, `SUM(stars)` 401;
  - `resume_snapshot`: 1 row (`classic`);
  - prefs without `colorblind_palette_on`;
  - all four files byte-identical to home-chip's `device/db-start`.

  The installed APK as found was colour-vision's arm B `cf7696f6…6a82`, as expected. Pass.
- `run.sh <deadline>` → attempt 1 CLEAN; `stats.py` → PASS.
- `jank.sh` → rc 0; `jstats.py` → FAIL (BD-23 (c) section).
- `bd09.py light|dark 142 <captures>`: the band height of 142 px is read from the bench's `dumpsys window` (`statusBars frame=[0,0][1080,142]`). The ergoz_studio captures are the same 1080×2424 at 420 dpi. Pass (BD-09 section).
- Not run: the Check (`./gradlew check`). This is a device session; the Check belongs to the developer, the auditor and gsd. `.planning/log/` was not read.

## 15e — the sitting (§6.5 field shape)
**PASS.** Attempt 1, `15e/sitting/attempt-1/`, 08:47:30–08:53:46 on 2026-11-01, on the cold boot of 08:44:12 (boot completed 08:44:24).

**Arms.**
- A = `d251c3b` = `git merge-base HEAD develop`; `develop` = `d251c3b`. APK `armA-d251c3b.apk`, sha256 `aa4d6e2d8189fb3dbacee50625684a22f6b2c5bfe4bbc7f9f0116a22e57fa053`.
- B = `599c88d` (code = `d029950`). APK `armB-599c88d.apk`, sha256 `b2ee555f0ccc4fb2ace08c10d8f0c5e1e3d234f59d6744a73eee031b4138eeac`.
- Both are `:app:assembleDebug` builds, each in its own detached worktree. Both were built before the bench booted.

**Null control** (arm B's APK in both positions, blocks N1/N2 interleaved ×5, the first of each block discarded):

| Label | n kept | min | median | max | non-COLD | discarded (first of block) |
|---|---|---|---|---|---|---|
| N1 | 20 | 424 | **456.0** | 493 | 0 | 530, 554, 525, 533, 573 |
| N2 | 20 | 414 | **462.5** | 535 | 0 | 566, 485, 506, 555, 541 |

N1 kept: 493, 424, 453, 449, 471, 472, 444, 433, 456, 444, 456, 438, 452, 460, 468, 457, 450, 472, 464, 468. N2 kept: 515, 484, 495, 414, 520, 454, 497, 441, 457, 447, 450, 432, 520, 535, 508, 435, 462, 468, 456, 463.

**Arms** (A/B interleaved in blocks of five, the first of each block discarded, `am force-stop` + 2 s between samples):

| Arm | n kept | min | median | max | non-COLD | discarded |
|---|---|---|---|---|---|---|
| A `d251c3b` | 20 | 350 | **410.5** | 462 | 0 | 478, 497, 484, 485, 484 |
| B `599c88d` | 20 | 409 | **455.0** | 473 | 0 | 523, 517, 552, 517, 532 |

A kept: 424, 402, 408, 399, 414, 415, 408, 400, 422, 416, 399, 350, 403, 376, 389, 462, 421, 417, 421, 413. B kept: 442, 471, 433, 466, 448, 459, 458, 443, 466, 473, 452, 440, 453, 409, 442, 471, 432, 457, 461, 458. (`am start -W` TotalTime, ms.)

**Floor and threshold.** floor = |456.0 − 462.5| = **6.5 ms**, far below the 150 ms VOID line. Threshold = max(2 × 6.5, 100) = **100 ms**. Delta = 455.0 − 410.5 = **44.5 ms ≤ 100 ms → PASS.**

**Preconditions, with values:**
1. **Instrument.** `Pixel_9a_bench`, API 36 (`emu avd name`; `sdk: 36`); debug variant. OS animations `0.0/0.0/0.0` at the start (`bench-state-start.txt`). P-1.2: `config.ini` differs from `Pixel_9a.avd` only in `AvdId`, `avd.ini.displayname` and `disk.dataPartition.size` (6G → 16G). The `.ini` files differ only in `path` and `path.rel` (`sitting/pre.txt`). Not void.
2. **Exactly one emulator.** `pgrep -f qemu-system` = 1 (`Pixel_9a_bench`) on all 38 monitor readings and at both ends. `adb devices` was empty before the boot. `Pixel_9a`, `Pixel_9` and `ergoz_studio` were never started.
3. **No concurrent Gradle.** 0 `GradleWrapperMain`, and no Gradle or Kotlin daemon or worker at all, on every reading (`monitor.log`: 0 of 38 not quiet). My own arm builds finished at 08:43, before the boot, and the daemon was stopped.
4. **Load.** `uptime` before (the launcher idle since 08:44:58, 2 min 32 s before the first sample): `1.65 2.80 3.03`. After (60 s after the last sample, app force-stopped): `1.82 2.43 2.79`. Both ≤ 5.0 (10 cores ÷ 2).
5. **ART.** `compile -m speed -f` after each swap. ART status `arm64: [status=verify] [reason=cmdline] [primary-abi]` on all 20 swaps, both arms alike.
6. **Component.** Resolved from the device in each block: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
7. **Swaps and schema.** Swaps only at block boundaries (`install -r -d --no-incremental`, compile, force-stop). The installed `base.apk` sha256 equalled its arm's on 20 of 20 swaps (`swaps.csv`). `git diff d251c3b 599c88d -- app/schemas` is empty, and no preference key, entity or database line is added or removed. `colorblind_palette_on` was absent at both ends (count 0); both arms carry the key's code from `develop`.
8. **Ladder.** Identical on both arms: 200 Challenge rows, `SUM(stars)` 401, `challenge_current_level` 111, one Classic save. The data is byte-identical to home-chip's recorded resting data at the start (`db-start`), after the jank block (`db-end`) and at the end (`db-final`), on all four files.
9. **Free space.** `Data-Free` 12 844 104K at the start, 12 822 364K after the sitting, 12 843 660K at the end. All ≥ 4 GB.

**D-2's forward rule:** not engaged; this was the first attempt on the boot.

**15f:** not re-observed at this visit (R3 lists it as re-observed "at the same visit"). It is unaffected by construction, and the founder's reduced-QA directive kept this dispatch to 15e, the jank block and BD-09. See Open issue 3.

## BD-23 (c) — the jank block (R3; D8)
**FAIL against its pass line** `median_B ≤ median_A + 1`. The block ran 08:55:43–08:58:39, after the 15e sitting and separate from it, on the same boot.

**Conditions.**
- Animations were **on**: Accessibility › Color & motion › Remove animations switched off, and the scales read `1.0 1.0 1.0` (`jank/conditions.txt`). Afterwards the switch went back on, and the scales read `0.0 0.0 0.0`.
- The host was quiet on 18 of 18 readings, with the bench the only emulator. Load was 1.57 at the start and 4.08 at the last block's start.
- Six blocks, A/B/A/B/A/B, of five samples each, the first of each block discarded, so 12 kept per arm.
- Swaps only at block boundaries, sha matched 6/6, ART status identical on all swaps.
- Per sample: `am start -W`, `sleep 1`, then the "Janky frames" line of `dumpsys gfxinfo com.erman.pegsolitarie`, then `am force-stop` and 2 s.

| Arm | n kept | min | median | max | non-COLD | discarded |
|---|---|---|---|---|---|---|
| A `d251c3b` | 12 | 7 | **10.5** | 12 | 0 | 6, 13, 8 |
| B `599c88d` | 12 | 5 | **12.0** | 18 | 0 | 5, 5, 5 |

Per block, as janky / total frames (TotalTime ms), the first of each block discarded:

| Block | Samples |
|---|---|
| 1 A | 6/6 (2285), 7/7 (2062), 7/7 (2117), 9/9 (1877), 8/9 (1765) |
| 2 B | 5/5 (2436), 5/6 (1952), 6/6 (1900), 5/5 (1889), 6/6 (1891) |
| 3 A | 13/28 (1860), 7/7 (1126), 12/35 (1003), 11/30 (1087), 12/32 (984) |
| 4 B | 5/5 (1368), 18/22 (1011), 15/23 (1035), 14/21 (930), 12/31 (1616) |
| 5 A | 8/9 (1177), 10/28 (990), 12/32 (992), 12/28 (988), 12/36 (1686) |
| 6 B | 5/5 (1157), 11/26 (993), 14/21 (1056), 12/23 (1222), 12/31 (1216) |

**Result.** median_B 12.0 > median_A + 1 = 11.5 → **FAIL**, by 0.5 frame.
- I did not re-run. No off-device cause appeared: the host stayed quiet, there was no foreign process or Gradle, and `Data-Free` was stable. R3 gives this block no VOID clause, and the dispatch's re-attempt rule covers only off-device causes. A re-run would shop for a pass.
- What the numbers show, for the tech-lead's ruling, not as a re-judgement:
  - **Blocks 1–2 ran in a slow regime.** Both arms read about 2 s TotalTime, so the 1 s read saw only 5–9 frames, all janky. Blocks 3–6 read about 1 s and 21–36 frames.
  - Each arm has 4 of its 12 kept samples in that regime, so the interleave held. But these are counts of frames in a fixed 1 s window after `am start -W` returns, so they move with the launch duration as well as with jank.
  - In blocks 3–6 alone, A's kept medians were 12 (7, 10, 11, 12, 12, 12, 12, 12) and B's 13 (11, 12, 12, 12, 14, 14, 15, 18). B rendered fewer frames in that window (about 21–31 against about 28–36), so its janky share is higher.
  - The cause of the regime change is unknown. It began after the Remove-animations switch went off just before block 1.

## BD-09 — the status strip (B-09b), desk re-read
The method: in each capture, I compared the band `[0, 142)` px (the bench's `statusBars` inset; 0.059 H) against `background` outside the system's glyph boxes. A glyph box is a column cluster of pixels deviating more than 1/255, padded 3 px. A glyph's core is the pixel with the highest WCAG contrast against the strip. The captures are those named in `qa-engineer-device.md` and `ux-designer-bd09.md`, all from `ergoz_studio` (1080×2424, 420 dpi), plus the one new bench capture.

| Screen | Light capture(s) | Dark capture(s) | Strip vs `background` (±1/255) | Clock core | Status icons (min core) | Result |
|---|---|---|---|---|---|---|
| Home | `03`, `02-…-1`, `02-…-3`, `11`, `16` | `20-…-1`, `20-…-2`, `21-…-nosheen-ref` | 0 off px, max dev 0, at rest and in motion | light (98,96,100) 5.57:1; dark (255,255,255) 17.66:1 | 5.57 / 17.66 | PASS |
| Gameplay (English-33, Challenge) | `06`, `07-…-1`, `08`, `09-…-a`, `14` | `22-…-selection-motion-1`, `22-…-2`, `28` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |
| Tutorial | **`15e/bd09-tutorial1-light-bench.png`** and `-b` (new; `01` is the pre-draw first frame and is not read) | `27` | 0 off, max dev 0 | light (bench) (0,0,0) 18.81:1; dark 17.66:1 | light (bench) signal bars (135,133,138) **3.27:1**, Wi-Fi and battery 18.81; dark 17.66 | PASS |
| Catalogue | `05`, `10` | `23` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |
| Results | `15` | `29` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |
| Settings | `17` | `24` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |
| Licences | `18` | `25` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |
| Licence text | `19` | `26` | 0 off, max dev 0 | 5.57 / 17.66 | 5.57 / 17.66 | PASS |

- **The icon appearance** is dark in light and light in dark on every capture, as B-09b's third clause asks.
- **The measured values match the ux-designer's expected ones:** light 5.57:1 (60 % black icons) and dark 17.6:1.
- **The bench's system UI draws opaque black icons in light.** Its clock reads 18.81:1, the "opaque black" value B-09b names. The one grey glyph is the dimmed signal-bar icon (no service on the AVD), and its core reads 3.27:1, at or above 3:1.
- **The light tutorial capture was taken on the bench** after the 15e and jank closing readings, as the dispatch allows: Settings › How to play, step 1 of 3, settled, two captures 3 s apart. I then left with Back, without finishing, and the data was byte-identical afterwards (`db-final`).

## Open issues / risks
1. **BD-23 (c) FAIL by 0.5 frame, with an instrument question (to the tech-lead).**
   - The pre-registered line gives FAIL, and I report it as FAIL.
   - The block's first two blocks ran about twice as slow as the rest, and the 1 s read counts frames in a window whose fill depends on launch length.
   - Whether that calls for a ruling on the instrument (for example, discarding a warm-up pair of blocks after the scale switch, or counting janky frames over a fixed number of frames) or for spec §7's degradation path is the tech-lead's call. Any new line applies only to a fresh block, never to these numbers.
   - The 15e verdict does not depend on this block (R3).
2. **15e's delta is positive (+44.5 ms).** Arm B launches about 45 ms slower than arm A on this instrument. That is inside the 100 ms threshold, so it passes, and the null floor was tight (6.5 ms). I record it so the tech-lead can see the size of the cost.
3. **15f was not re-observed** (R3, "at the same visit"). It is unaffected by construction. Reduced QA kept this dispatch to the three named items. If the tech-lead needs it before the sign-off, it is a short visual check on the bench, with no timing.
4. **The bench's resting install changed** from colour-vision's arm B (`cf7696f6…`) to this branch's arm B (`b2ee555f…`), since the jank block ended on it. It is a sitting's own install, not a foreign one.
5. **Staffing:** Opus under Q-019, the same model as the developer (header). **Commit trailer:** this report's commit carries rules/gitflow.md v1.0.0's single `Co-Authored-By: Claude Fable 5.1` trailer, as the branch's other commits do, though it ran on Opus. That is the same open question the developer's and tech-lead's reports raise.

**System changes on `Pixel_9a_bench`, and their fate:**
- Arms A and B were installed in turn, per block; the bench ends on arm B `b2ee555f…`.
- Remove animations went off for the jank block (scales 0.0 → 1.0), then back on (→ 0.0). The change was made through Accessibility only, never `settings put global`.
- The app's Settings and tutorial were opened for the BD-09 capture, then left; the data is unchanged.
- Settings are unchanged against the start, apart from uptime, `Data-Free` and the installed sha: light mode, font scale 1.0, gesture navigation, physical `wm size` and `wm density`, no accessibility service, 0 accounts.
- `adb shell sync`, then `adb -s emulator-5584 emu kill`. No `qemu-system` process remained, and `adb devices` was empty.
- I touched no other AVD.

## Handoff
Next: tech-lead — the sign-off reads this report:
- **15e (BD-23 (b)): PASS.**
- **BD-09: PASS**, under B-09b, on all eight screens in both themes.
- **BD-23 (c): FAIL**, on its literal line, with the instrument observation in Open issue 1, for your ruling: spec §7's degradation path, or a corrected jank instrument run fresh.
- Open issues 2 and 3 are informative.

Next: producer —
- This report is committed on the branch, alone.
- The bench is down, and the arm worktrees are removed. P-4 may be released.
- If `develop` gains a start-path commit before the merge, the sitting is owed again.

VERDICT: 15e PASS; jank (BD-23 (c)) FAIL; BD-09 PASS
