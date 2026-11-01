# android-game-dev report — release-3.0.0: D-15 diagnosed with traces (no product edit)
Date: 2026-11-01
Branch: release/3.0.0 (report only; built at HEAD `3267573`, whose code equals `fb66351`, the probe build: same APK sha256 `3e974016…c4ed96b`)
Skills loaded: ergoz-soft:standards (role android-game-dev; game; android-compose-game). The snapshot `.claude/rules/VERSION` is 0.5.1 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, and `.claude/rules/lessons.md`. Skills: superpowers:test-driven-development, gitflow, android-emulator-skill, compose-performance-audit.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-device-pass-triage.md` §1.3 (the trace route), `qa-engineer-d15-probe.md` (FAIL, Davey clause).

## Summary
- **Verdict: an emulator/host artefact, not an app defect.** No code fix is proposed, so there is no red-first test.
- **The slow regime is macOS App Nap.** The Mac's screen was locked, with the user idle about 2 h. After the guest played its first touch sound, every thread of the emulator process dropped to macOS priority 4 (darwin-bg) within 4 s and stayed there. The guest's raw CPU speed then halved while the guest itself was idle. The trigger was **a Settings visit that changed no setting**. In one boot, at scale 0.0 throughout, OFF cold starts went from median 497 ms with no Davey to median 1,374 ms with Davey 5 of 5.
- **The long frame is frame 1.** It holds the main-thread first composition, measure and layout (≈180 ms un-napped), then a wait on RenderThread. OFF's RenderThread compiles 23 Skia GL programs on frame 1, against ON's 4. ON compiles the rest after first display, in its entrance frames, and in total compiles more. Same regime, OFF frame 1 = ON + ≈50 ms un-napped, or + ≈70–90 ms napped. Only napped does OFF cross 700 ms.
- **The reduced-motion gate costs 0.5–1.2 % of main-thread samples in both conditions.** No binder, GC, lock or in-guest CPU contention explains a slow start. Un-napped OFF frame 1 is 280–312 ms, and TTID is 458–500 ms.
- **The bench is restored as found:** APK `b2ee555f…` `-m speed -f`, scales 0.0, data byte-identical. A decisive re-measure with a nap guard is proposed (§6).

## 1. Method
- **Instrument:** `Pixel_9a_bench` (4 vCPU, 2 GB, API 36 Play image, gfxstream, GLES on the host). It was the only emulator, cold-booted 3 times with QA's flags (`-port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`).
- **Build:** the debug APK of `3267573`, built before the first boot in a detached scratch worktree with `local.properties` copied, using `--no-daemon`. No daemon was left, and `--stop` was never run. It was installed with `install -r -d --no-incremental` and `compile -m speed -f`. ART reads `[status=verify]`: the APK is debuggable, so ART does not AOT-compile it. The main thread runs in the switch interpreter (`ExecuteSwitchImplCpp` 99 % inclusive) with JIT.
- **Switching:** Remove animations was switched only through Settings › Accessibility › Color & motion, using QA's `toggle.sh`. The wait after each switch was 180 s.
- **Phases:** each phase ran 2 plain launches (QA's `am start -W` and logcat Davey method; the first is discarded), then 3 Perfetto-traced launches, then 1 simpleperf launch.
- **Perfetto config:** sched (compact) with blocked_reason, vmscan, lmk and rss; atrace gfx, view, am, wm, dalvik, binder_driver, input, res, ss, hal, aidl, pm and memreclaim, plus `atrace_apps` com.erman.pegsolitarie; process_stats; sys_stats (meminfo, vmstat, cpu) at 250 ms; SurfaceFlinger frametimeline. Each trace is 9 s.
- **simpleperf:** `simpleperf record --app … -e cpu-clock -g -f 1000` resolves the Java frames, composables included. Perfetto's `linux.perf` callstacks came out empty on this user image, so `P0a-…-p1` is a plain trace.
- **Host:** `primon.sh` logged the qemu process's per-thread priority histogram (`ps -M`) every second from 14:25:56 to the end. `calib.sh` timed sha256 of 64 MiB of zeros in the guest, three runs, before and after every phase.

## 2. What the long frame is doing (Perfetto)
Frame 1 is the first `Choreographer#doFrame` that draws. On RenderThread it is the first `DrawFrames`. Values are in ms, as a range or a median.

| Regime / condition (traces) | TTID | frame 1 on main | main Running in it | main asleep on RT sync | RT frame 1 | shader compile in RT frame 1 | programs | RT shader compile, whole start |
|---|---|---|---|---|---|---|---|---|
| un-napped OFF (P0a, P0b, Q0; n 9) | 458–500 | 280–312 (288) | 170–201 | 82–101 | 80–99 | 68–87 | **23** | 99–121 |
| un-napped ON (R0; n 3) | 412–471 | 229–255 (239) | 181–192 | 24–27 | 20–24 | 14 | **4** | 157–163 |
| napped OFF (P2, Q1; n 6) | 1,081–1,863 | 684–1,130 | 465–816 | 170–415 | 164–394 | 140–344 | 23 | 212–426 |
| napped ON (P1; n 3) | 988–1,735 | 610–1,310 | 492–1,121 | 46–53 | 37–42 | 22–26 | 4 | 363–468 |

- **Main thread, frame 1.** The work is `traversal`:
  - `Compose:initializeView` 9–37;
  - the first composition, `Compose:recompose` 45–80;
  - `AndroidOwner:onMeasure` 72–85 (11 `TextStringSimpleNode::measure`);
  - layout 11–18;
  - `Record View#draw()` 10–16, all un-napped;
  - then it sleeps while RenderThread draws frame 1 (`ViewRootImpl` waits on the first draw).

  Main-thread Running time in frame 1 is the same in both conditions (OFF median 183, ON 191). simpleperf puts it in `MainActivity.onCreate` → `AppThemeRoot` → `AppNavHost` → `HomeBands` / `HomeBandColumn` measure. The profile has the same shape in every phase.
- **RenderThread, frame 1, is the OFF/ON difference.** In `Q0-…-t3` (OFF) frame 1 compiles 23 programs in 71 ms:
  - FillRectOp ×5;
  - CircleOp ×4;
  - CircularRRectOp ×4;
  - FillRRectOp ×2;
  - AtlasTextOp ×2;
  - DrawAtlasPathOp ×2;
  - PathStencilCoverOp, TriangulatingPathOp, ShadowCircularRRectOp and TextureOp, one each.

  In `R0-…-t2` (ON) frame 1 compiles only 4 FillRectOp programs (14 ms). The reason: `clayEntrance` starts every Home item at alpha 0, so RenderThread skips those layers. ON pays the rest after `Displayed`, in its entrance frames: 72 ms (15 programs), 40 ms (8), 19, 7, 16, 10 and 25 ms. Every cold start misses the shader cache (`cache_miss` under each `shader_compile`). The compiles run through the emulator's host-GL translation (`driver_compile_shader`, `driver_link_program`).
- **Not the cause:**
  - Other guest processes took 4–12 % of the 4 vCPUs during frame 1.
  - The app's `Jit thread pool` ran almost all of frame 1 on its own vCPU.
  - GC, binder and lock-contention slices in the start window are minor.
  - The main thread's Runnable time in frame 1 was 11–32 ms un-napped and 31–115 ms napped, so there is no CPU starvation inside the guest.
  - The UMP/GMA threads and `WebViewChromium.init` start after frame 1, in both conditions alike.
- **Cross-check against the probe's own logs.** The Davey lines of the 8 kept OFF FAILs decompose, by FrameInfo, to traversal 476–681 ms and RenderThread 159–218 ms. The ON Davey cases have RenderThread 31–76 ms. These match the napped rows above.

## 3. The slow regime is App Nap of the emulator process
**Per-phase launches** (`launches.csv`; kept = s2–p1, n 5 each; "interval" is QA's Start proc → clns-9):

| Phase | Scale | qemu threads | sha256 64 MiB in guest | TotalTime median (range) | Davey ≥ 700 | interval median |
|---|---|---|---|---|---|---|
| P0a, 3 min after boot 1, before any switch | 0.0 | (not logged yet) | 35–44 | 501 (478–527) | 0/5 | 70 |
| P0b, 10 min after boot 1, still no switch | 0.0 | (not logged yet) | 35–43 | 481 (468–542) | 0/5 | 66 |
| P1, 180 s after the switch to ON | 1.0 | (4, inferred from speed) | 67–105 | 1,191 (991–1,754) | 2/5 | 135 |
| P2, 180 s after the switch to OFF | 0.0 | **4** throughout | 63–68 | 1,208 (1,084–1,960) | **5/5** | 120 |
| Q0, boot 2, before any switch or tap | 0.0 | **31** throughout | 34–43 | 497 (459–531) | 0/5 | 59 |
| Q1, boot 2, 180 s after a Settings visit with **no setting changed** | 0.0 | **4** from 14:37:58 | 66–73 | 1,374 (1,148–1,865) | **5/5** | 146 |
| R0, boot 3, after the switch to ON then a cold boot | 1.0 | **31** throughout | 35–45 | 470 (413–478) | 0/5 | 71 |

- **Q0 → Q1 is the decisive pair:** the same boot, scale 0.0 throughout, nothing changed. The only event between them was a visit to Accessibility › Color & motion (`click-Q1.png` shows the switch untouched).
  - At 14:37:55 the qemu process grew realtime audio threads (`56R`), the guest's touch sound.
  - At 14:37:58 every one of its 136 threads was at priority 4.
  - Everything after that ran twice as slow, OFF included.
- **The host was quiet and the guest idle.**
  - Host load1 was 2.28 at Q0 and 2.51 at Q1.
  - During each 180 s settle the guest was 2.4–4.8 % busy.
  - A guest-only sha256, independent of the app, doubled.
- **Why priority 4, and why it is App Nap:**
  - The screen was locked: `CGSSessionScreenIsLocked = true`, the loginwindow shield windows at layers 2001 and 2004, `HIDIdleTime` about 7,000 s, display sleep never.
  - The emulator is a GUI app (`lsappinfo`: type Foreground), occluded behind the shield.
  - At priority 4 its threads sit in the same band as `mediaanalysisd`. Default apps run at 31–46.
  - A guest sound lifts the nap while it plays. During the P2 switch, qemu went 4 → 31 at 14:27:28, with a 97R audio thread, and back to 4 at 14:27:34, as the switch ended.
  - `taskpolicy -B -p <qemu>` had no effect. That command clears only a setpriority darwin-bg, so the throttle comes through the app-role (App Nap) path.
  - Apple schedules background-band threads on the efficiency cores (the M4 has 4 P + 6 E). That is an inference, but it agrees with the measured 2× guest CPU speed.
- **The earlier records fit the same pattern:**
  - **The probe:** block 1 came before any switch, with intervals 57–83 ms and no Davey. Every block after the first switch ran slow in both conditions.
  - **15e:** the jank began after the switch.
  - **The device pass:** the starts were slow 52 s and 84 s after the switch and fast again at 154 s. TalkBack speech is guest audio, which lifts the nap. That is consistent, but no priority log exists for it.

## 4. The reduced-motion gate path
- **Its cost:**
  - `ClayKt.rememberAnimationsEnabled` is 0.5–1.2 % inclusive of main-thread samples in every profile, OFF and ON alike: Q0 0.80, P0b 0.73, Q1 1.23, P2 0.61, R0 0.58, P1 0.47 %.
  - Its one non-trivial callee is Compose's own `MotionDurationScaleImpl.startObservingSystemScaleFactor`, the first read.
  - `BackdropClockDriver` and `rememberBackdropSceneReady` are ≤ 0.5 %.
- **What OFF actually changes:**
  - `claySquish` is a no-op.
  - The scene opens on frame 2. Its 6 programs compile in the frame after frame 1, which is 33–37 ms un-napped and is not the long frame.
  - Frame 1 draws the settled Home. That is the spec's "content appears settled", and it is what moves 19 shader programs into frame 1.
- **One anomaly, recorded:** one napped simpleperf launch (`P2-…-p1`, TotalTime 1,960 ms) attributes 23 % of main-thread samples to `SceneBackdrop`. Its leaves are memset, memcmp, minikin `FontCollection::init` and `TextIndent.<clinit>`, reached through a snapshot-read observer. That is not a real call path from that composable: it is a garbled interpreter-frame unwind. No other profile or Perfetto trace shows it (≤ 1 % everywhere else).

## 5. Ruling input: artefact, not defect
- **The pass line's third clause measures this:** an OFF frame 1 about 50–90 ms longer than ON's, the cost of drawing the settled Home whole. On this host it lands above 700 ms only while the emulator is napped and everything runs 2–2.7× slower (main-thread Running ×2.6–2.8, frame-1 shader compile ×2.0–2.3). Un-napped, OFF frame 1 is at most 312 ms and TTID at most 500 ms.
- **ON is not cheaper overall.** It compiles more shader programs in total (157–163 ms against 99–121 ms un-napped) and spends more main-thread CPU (simpleperf main-thread samples: R0 1,037 against Q0 501).
- **No app code location, minimal fix or red-first test is proposed.** Moving frame-1 work out of the reduced-motion path would mean showing an unsettled first frame, which is against the spec.
- **On a phone it should shrink further:**
  - the persistent shader cache hits after the first launch (here every cold start misses);
  - native GL drivers compile faster;
  - a release build is not debuggable, so it is AOT-compiled.

  D-10, on a phone, stays UNVERIFIED as before.

## 6. Proposed decisive re-measure (for the tech-lead's ruling; the qa-engineer runs it)
§1.4 as written, with three changes:
1. **Nap guard per launch.** Read `ps -M -p $(pgrep -x qemu-system-aarch64)` just before `am start` and just after the log dump. Any thread at priority 4 makes the launch VOID (napped).
2. **An absolute guest-speed gate per block.** Before and after each block, take the guest's `head -c 67108864 /dev/zero | sha256sum` wall time. All three runs must be ≤ 50 ms, or the block is VOID. Un-napped it read 34–45 in 24 runs; napped, 63–140 in 32. The probe's relative interval rule could not fire, because every ON block was napped. An absolute interval ceiling of 100 ms would also have separated the two (59–71 against 120–146).
3. **Keep blocks un-napped while the Mac is locked.** After each UI switch, run `adb shell sync; adb emu kill`, cold-boot with the same flags, wait 180 s, and run the block with no taps in between. A guest sound re-arms the nap. Boot 3 validated this: the switch to ON, then a reboot, kept priority 31 throughout and gave 413–478 ms. The alternative is to run with the screen unlocked and the emulator window visible, which needs the founder at the Mac.

On my un-napped numbers the line would read OFF ≈ 481–497 against ON ≈ 470, so clause 1 passes (+11–27 ms). Clause 2 passes (max 542). For clause 3, frame 1 is at most 312 ms, so it passes.

## Outputs
- `.planning/reports/release-3.0.0/android-game-dev-d15-trace.md` — this report, the only file committed.
- Artifacts, git-ignored, under `.planning/qa/release-3.0.0/d15-trace/` (129 MB; traces and profiles gzip-compressed). trace_processor and ui.perfetto.dev open the `.gz` files directly.

| Path | Size | What it shows |
|---|---|---|
| `traces/<phase>-t1..t3.pftrace.gz` (21), `traces/P0a-OFF-pretoggle-p1.pftrace.gz` | 73 MB total | The Perfetto traces: (a) OFF after a switch with Davey: P2-t1..t3; OFF after a Settings visit: Q1-t1..t3. (b) ON after a switch: P1-t1..t3; ON after a switch plus reboot: R0-t1..t3. (c) OFF before any switch: P0a, P0b and Q0, t1..t3 each |
| `simpleperf/<phase>-p1.data.gz` (6), `simpleperf/*.out` | 3.9 MB | cpu-clock call-graph profiles of the app, one per phase |
| `frame1-table.csv` | 2.5 KB | The frame-1 row per trace (`tools/row.sql`): the source of §2 |
| `launches.csv`, `logs/<tag>.log` (42) | 6.9 KB, 8 MB | Every launch: TotalTime, Displayed, interval, Davey list, scale, host line; `logcat -d -v threadtime` per launch |
| `primon.txt` | 75 KB | qemu per-thread priority histogram and host load, every second from 14:25:56 (boot 1), and across boots 2 and 3 |
| `calib.txt`, `regime.txt`, `settle.txt`, `logs/*-cpuinfo.txt` | 2.5 KB, 0.8 KB, 1 KB | Guest sha256 calibration; host and guest during each 180 s settle; `dumpsys cpuinfo` |
| `timeline.txt`, `boot.txt`, `closing.txt` | 1.3 KB | Switch, click, reboot, phase and kill times |
| `toggle-*.png` (4), `click-Q1.png` | about 141 KB each | Color & motion after each switch, and the click-only visit with the switch untouched |
| `app-debug-3267573.apk`, `apk.sha256`, `installed-sha.txt`, `art.txt`, `art-end.txt` | 22 MB | The traced build, its hash, the ART status |
| `bench-resting-armB-599c88d.apk`, `bench-state-start.txt`, `bench-state-end.txt`, `db-start/`, `db-end/` | 22 MB | The restore |
| `tl.sh`, `phase.sh`, `calib.sh`, `waitmon.sh`, `primon.sh`, `regime.sh`, `click.sh`, `trace.cfg`, `trace-perf.cfg`, plus QA's `toggle.sh`, `state.sh`, `snap.sh`, `hostq.sh`, `quiet.sh` | small | Session helpers, never committed |
| `tools/` (`an.py`, `row.py`, `row.sql`, `ops.sql`, `rtframes.sql`, `samples.py`, `README.txt`) | small | The analysis. `README.txt` pins trace_processor_shell v59.1, mac-arm64, sha256 `8488edbe…48fdf4` |

**Excerpt** (≤ 20 lines; `primon.txt`, `calib.txt`, `launches.csv`, `timeline.txt`):
```
Q0-OFF-boot2-pretoggle-pre 14:36:40 sha256_64MiB_ms=[43 37 35] ... host_load={ 2.28 ...
Q0-OFF-boot2-pretoggle-t3 trace COLD total=459 interval=46 davey=[] scale=0.0
14:37:54 load1=2.27 qemu_pri=[31T:130 46T:3 97R:1 50R:1 37T:1 19T:1 ]
Q1 click-only (no setting changed) 14:37:54 scales=0.0
14:37:55 load1=2.25 qemu_pri=[31T:127 56R:4 46T:2 50R:1 37T:1 19T:1 ]
14:37:58 load1=2.25 qemu_pri=[4T:136 ]
Q1-OFF-boot2-afterclick-notoggle-pre 14:40:58 sha256_64MiB_ms=[66 66 69] ... host_load={ 2.51 ...
Q1-OFF-boot2-afterclick-notoggle-t1 trace COLD total=1148 interval=153 davey=[743 709 ] scale=0.0
Q1-OFF-boot2-afterclick-notoggle-s2 plain COLD total=1374 interval=146 davey=[916 915 ] scale=0.0
14:27:27 load1=2.44 qemu_pri=[4T:132 ]
14:27:28 load1=2.44 qemu_pri=[31T:132 46T:2 97R:1 50R:1 37T:1 19T:1 ]   <- P2 switch: touch sounds playing
14:27:34 load1=2.44 qemu_pri=[4T:138 ]                                   <- switch over: napped again
R0-ON-boot3-afterswitch-reboot-t2 trace COLD total=413 interval=55 davey=[] scale=1.0
frame 1, Q0-t3 (OFF): RT 82 ms, 23 shader_compile = 70.8 ms | R0-t2 (ON): RT 20 ms, 4 = 13.6 ms
```

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-release rev-parse HEAD` — pass: `32675739…`.
- `git … worktree add --detach /Users/ermanergoz/Desktop/PegSolitarie-d15trace 3267573`; `local.properties` copied; `./gradlew --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug` — pass ("BUILD SUCCESSFUL in 9s", load1 3.23 before). APK sha256 `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b`, identical to the probe's.
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` ×3 — pass: boot_completed at 14:08:03, 14:33:27 and 14:43:06. Only this emulator ran.
- `adb -s emulator-5584 install -r -d --no-incremental app-debug-3267573.apk`; `cmd package compile -m speed -f com.erman.pegsolitarie` — pass: ART `arm64: [status=verify] [reason=cmdline]`; installed sha matches.
- `toggle.sh false|true` ×4 (P1-ON, P2-OFF, R-ON, restore-OFF) — pass: the switch read the wanted state each time. `click.sh Q1` — pass: no setting changed, scales 0.0.
- `phase.sh` ×7: 42 cold launches, all `LaunchState: COLD`; 21 Perfetto traces plus 1; 6 simpleperf profiles — pass.
- `python3 -I tools/row.py traces/*-t?.pftrace` — pass: `frame1-table.csv`.
- `trace_processor_shell -q tools/ops.sql` and `-q tools/rtframes.sql` on `Q0-…-t3` and `R0-…-t2` — pass: the per-op compile lists in §2.
- `simpleperf report --children --sort symbol --tids <main>` on the device, for all 6 profiles; `simpleperf report-sample --show-callchain` for `P2-…-p1` — pass (§2, §4).
- `ps -M -p <qemu>`, `lsappinfo list`, the JXA `CGSessionCopyCurrentDictionary` / `CGWindowListCopyWindowInfo` queries, `ioreg -c IOHIDSystem` (HIDIdleTime) and `pmset -g` — pass: priority 4 while napped; screen locked; display sleep 0.
- `taskpolicy -B -p <qemu>` — exit 0, no effect: priority stayed 4 and the guest sha256 stayed 68–73 ms.
- `snap.sh … save db-end` and `cmp` against `db-start` — pass: all 4 files byte-identical.
- `diff bench-state-start.txt bench-state-end.txt` — only the uptime, the `/data/app` path and Data-Free differ. The sha is `b2ee555f…`, the scales 0.0/0.0/0.0, and night mode, font scale, gesture nav, wm size/density, accessibility services and accounts are unchanged.
- `adb shell sync`; `adb emu kill` — pass: no `qemu-system-aarch64` remained, and `adb devices` was empty at 14:50:11.
- `git … worktree remove --force …-d15trace`; `git … worktree prune` — pass.
- Not run: the Check, because no code changed. `.planning/log/` was not read.

## Open issues / risks
1. **D-15 stays FAIL until the tech-lead rules.** This report is input only. If the ruling accepts the artefact, §6 is the re-measure I would run first. Its expected outcome, on my un-napped data, is a PASS.
2. **"Priority 4 means efficiency cores" is inferred** from Apple's scheduling policy and the 2× guest slowdown. It was not observed per core: `powermetrics` needs root.
3. **App Nap as the mechanism is inferred:** locked screen, a GUI app, onset 3–4 s after guest audio, lifted while audio plays, not cleared by `taskpolicy -B`. The effect is observed directly. Why boot 1 stayed un-napped for 10 minutes before the first sound is not explained.
4. **This build is debuggable** (ART `verify`, switch interpreter, JIT busy through frame 1), the same as the probe's. A release build would shorten main-thread frame 1 in both conditions. That does not change the ruling.
5. **One anomalous simpleperf unwind** (`P2-…-p1`, §4) is recorded, not used.
6. **Commit trailer:** the commit follows this branch's recent convention (Opus trailer plus session line), as the prior reports record. The plugin 1.0.0 gitflow rule names a single Fable trailer; this is the open question already on file.

**System changes on `Pixel_9a_bench`, and their fate:**
- The traced APK was installed, then replaced by the resting `b2ee555f…`, compiled `-m speed -f` (ART `verify`, as before).
- Remove animations was switched 4 times through the Settings UI, plus one visit with no change. The final scales are 0.0/0.0/0.0, as found.
- 2 extra cold boots and shutdowns, each with `sync`.
- `/data/local/tmp/*-p1.data` was removed after pulling, and `/data/misc/perfetto-traces/` was left empty. `.studio/` and `dalvik-cache/` were pre-existing and left untouched.
- `/sdcard/ui.xml` was removed. App data is byte-identical.
- No `settings put`, no radio or HAL toggle, no force-stop of any other package.

**Host:**
- `taskpolicy -B` ran on the emulator process I launched, with no effect; the process has ended.
- trace_processor_shell v59.1 was downloaded to the session scratchpad, sha256 verified. It is not in the product.
- No Gradle daemon was left.

## Handoff
Next: producer —
- **tech-lead:** rule D-15 on §5. In my reading the probe's FAIL comes from an App-Napped emulator behind a locked screen, and frame 1 under reduced motion carries 50–90 ms of front-loaded shader compilation by design. Decide whether §6's re-measure is required before the merge to main.
- **qa-engineer (on the tech-lead's ruling):** the §6 re-measure. `primon.sh`, `calib.sh` and the reboot procedure are in `.planning/qa/release-3.0.0/d15-trace/`.
- **code-auditor:** nothing to review (no code changed).

**Diagnosis.** On the bench, D-15's ≥ 700 ms animations-off frame is the first frame of a cold start. In it the main thread composes, measures and lays out Home in the debug build's interpreter (≈180 ms when healthy). It then waits while RenderThread compiles 23 Skia GL programs to draw the settled Home in one frame (≈70 ms when healthy). With animations on, the entrance starts at alpha 0, so frame 1 compiles 4 and the rest come after first display. That is why OFF's frame 1 runs about 50 ms longer, which is the intended behaviour, not a defect. It crosses 700 ms only because the emulator was App-Napped by macOS. Behind the locked screen, the guest's first touch sound put every qemu thread at priority 4 within 4 s, which halved the guest's CPU speed for both conditions. A Settings visit that changed nothing turned the same boot's OFF starts from 497 ms with no Davey into 1,374 ms with a Davey on every launch. The reduced-motion gate, binder, GC, JIT contention and other guest processes are each too small to account for it. This is an emulator/host artefact. No app fix is proposed. A re-measure with a priority guard, an absolute guest-speed gate and a cold reboot after each switch would decide the clause on an un-napped bench.
