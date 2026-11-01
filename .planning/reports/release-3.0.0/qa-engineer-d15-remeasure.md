# qa-engineer report — release-3.0.0: the guarded D-15 re-measure (tech-lead-d15-ruling.md §2)
Date: 2026-11-01
Branch: release/3.0.0 (report only). The build is release head `c84f1fa`. `git diff --stat fb66351 c84f1fa` outside `.planning` and `docs` is empty. `34ea294` landed during the run and touches docs and planning only (`git diff --stat c84f1fa 34ea294` outside `.planning` and `docs` is empty).
Skills loaded: ergoz-soft:standards (role qa-engineer; game; android-compose-game). I read the rules snapshot `.claude/rules/` (VERSION 0.6.2), which CLAUDE.md imports: studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android and compose, plus `lessons.md`. Skills: android-emulator-skill, android-testing.
Ran in parallel with: none. The producer held all other Gradle and emulator work. I checked the host on every launch (see Checks).
Ran on: claude-opus-5-5 at effort high (policy, FOUNDER.md Q-019 answer 3). The agent file pins fable at high. The harness names the model but not the effort.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)
Authority: `tech-lead-d15-ruling.md` §2 (pre-registered). Method inputs: `qa-engineer-d15-probe.md` (§1.4 method) and `android-game-dev-d15-trace.md` (helpers only).

## Summary
- **VERDICT: PASS, on attempt 2.** Attempt 1 was VOID. Under §2 that allowed one re-attempt, and attempt 2 is valid with 4 of 12 kept launches VOID in each condition (the limit is more than 4).
- **Clause 1 PASS:** median OFF **465.5 ms** ≤ median ON **440.5 ms** + 100 ms (= 540.5 ms).
- **Clause 2 PASS:** every OFF launch is under 2 s. The longest was **508 ms**.
- **Clause 3 PASS:** no OFF launch logged a Davey frame. The longest OFF frame is therefore below the 700 ms Davey threshold; this method cannot read its exact length. No launch in either attempt logged a Davey, in either condition.
- **The nap guard never fired.** No launch window in either attempt held a priority-4 qemu thread. All VOIDs came from Guard 2's post-block gate: the first post-block sha256 run read 51–57 ms. See Open issue 1, which is a design note on that gate.
- **The bench is restored as found:** resting APK `b2ee555f…` compiled `-m speed -f`, scales 0.0/0.0/0.0, app data byte-identical. I ran `sync`, then `emu kill`, and removed the scratch worktree.

## Method (§2 as run)
**Instrument.**
- `Pixel_9a_bench`, the only emulator. Every boot used exactly `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`: 13 cold boots across the two attempts, plus the restore boot's kill.
- No screenrecord or TalkBack ran. uiautomator ran only inside `toggle.sh`, which runs after a block's post gate and before the kill, so never between a boot and the end of its block.

**Host.**
- Load (`uptime`): 2.63 at the start (14:58), 1.42 at the close (15:48). Both are ≤ 5.0.
- `hostq.sh` read `quiet=yes` and `emulators=1` on all 60 launch readings. No GradleWrapperMain ran, and no busy Gradle or Kotlin daemon. The highest load1 that primon logged was 4.42, during the build.
- **Mac lock state:** unreadable from this session, because `CGSessionCopyCurrentDictionary` returned an empty dictionary. As a proxy I read `HIDIdleTime`: 9,030 s at the start (no input for 2.5 h) and 347 s at the close. So someone used the Mac at about 15:42:34, during attempt 2's block 5 idle. This is informational only, as §2 says; the guards decide.

**Build.**
- A detached worktree `/Users/ermanergoz/Desktop/PegSolitarie-d15remeasure` at `c84f1fa`, with `local.properties` copied from the main repo, built before the first boot.
- `./gradlew --console=plain :app:assembleDebug`: BUILD SUCCESSFUL in 8 s. Then `./gradlew --stop` ("1 Daemon stopped").
- APK sha256 `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b`, equal to the expected `3e974016…c4ed96b`.

**Install.**
- On boot 1 of each attempt: `install -r -d --no-incremental`, then `cmd package compile -m speed -f com.erman.pegsolitarie`. Attempt 2 got its own fresh install on its own boot 1.
- ART: `arm64: [status=verify] [reason=cmdline] [primary-abi]` after each install and at the end, for the resting APK.
- The installed `base.apk` sha equals the build's.

**Guards.**
- **Guard 1 (nap).** `primon2.sh` logged the qemu thread-priority histogram and load1 every second for the whole session, 2,717 samples. It is `primon.sh` with one change: it re-finds the qemu pid every second, so it survives the reboots. `pri.sh` also read the histogram just before each `am start` and just after each dump. A launch is VOID if any of its samples, from 2 s before `am start` to the dump, shows priority 4. There were 6–8 samples per launch window.
- **Guard 2 (speed gate).** `calib.sh` (the trace's helper, unchanged) ran three times before and three times after each block. A block is VOID if any of its six runs exceeds 50 ms.
- **Guard 3 (reboot).** After each block's post gate:
  1. switch Remove animations through Settings › Accessibility › Color & motion (`toggle.sh`; screenshot; read-only scale read);
  2. `adb shell sync`, then `adb emu kill`;
  3. cold boot, then wait for `boot_completed`;
  4. 180 s idle with no taps (`idle.sh`; read-only scale read);
  5. the pre gate, then the block.
- Block 1 of each attempt came after a cold boot at 0.0, then install and compile, then 180 s idle.

**Per launch.** As in §1.4:
- `am force-stop`, 2 s, `logcat -c`, then `am start -W … MainActivity`, and the log dump 3 s after `-W` returns.
- Davey lines are counted for the app pid only.
- The interval runs from Start proc to that pid's `Configuring clns-9`. It is recorded but voids nothing.
- All 60 launches were COLD.

## Attempt 1 — VOID (14:59:53 boot → 15:23:39; 23.8 min of the 75-min box)
| Block | Cond | Gate pre (ms) | Gate post (ms) | Gate | qemu priority in launch windows | Kept TotalTime (ms) | Davey |
|---|---|---|---|---|---|---|---|
| 1 | OFF | 36 35 35 | 49 37 36 | ok | 31 only | 435 492 457 457 | none |
| 2 | ON | 37 35 39 | **51** 45 49 | VOID | 31 only | 489 511 510 486 | none |
| 3 | OFF | 37 35 38 | 49 38 35 | ok | 31 only | 529 456 456 432 | none |
| 4 | ON | 37 35 39 | 48 42 45 | ok | 31 only | 502 445 436 390 | none |
| 5 | OFF | 38 38 34 | **51** 41 37 | VOID | 31 only | 538 430 444 470 | none |
| 6 | ON | 37 35 35 | 47 45 **57** | VOID | 31 only | 423 426 378 416 | none |

- ON had 8 of 12 kept launches VOID (blocks 2 and 6), which is more than 4, so **attempt 1 is VOID**. OFF had 4 of 12 VOID.
- These numbers are not judged. Attempt 1 gives no verdict.

## Attempt 2 — VALID (15:24:25 boot → 15:47:40; 23.3 min of its own 75-min box)
Before attempt 2's boot 1, I switched the bench, which attempt 1 had left at ON, to OFF through the Settings UI (`toggle-a2-pre-OFF.png`). Then `sync`, `emu kill`, and a fresh cold boot.

**All launches** (TotalTime ms / framework interval ms / max Davey; † = the discarded first launch of each block):

| Block | Cond | Gate pre | Gate post | s1† | s2 | s3 | s4 | s5 | Block |
|---|---|---|---|---|---|---|---|---|---|
| 1 | OFF | 41 34 35 | **56** 37 36 | 565 / 102 / – | 465 / 54 / – | 495 / 74 / – | 417 / 41 / – | 484 / 62 / – | VOID (slow guest) |
| 2 | ON | 37 34 35 | 48 44 46 | 507 / 103 / – | 466 / 82 / – | 423 / 48 / – | 453 / 53 / – | 453 / 73 / – | ok |
| 3 | OFF | 40 34 35 | 49 38 41 | 580 / 88 / – | 482 / 59 / – | 444 / 57 / – | 488 / 70 / – | 457 / 52 / – | ok |
| 4 | ON | 37 35 38 | 45 45 43 | 514 / 103 / – | 474 / 79 / – | 428 / 72 / – | 418 / 54 / – | 426 / 43 / – | ok |
| 5 | OFF | 39 35 37 | 49 37 36 | 527 / 93 / – | 474 / 58 / – | 508 / 60 / – | 444 / 42 / – | 452 / 51 / – | ok |
| 6 | ON | 37 47 36 | 47 **53** 47 | 612 / 135 / – | 559 / 81 / – | 429 / 58 / – | 524 / 84 / – | 553 / 81 / – | VOID (slow guest) |

**Guard columns per launch.**
- `pri_before` and `pri_after` read only 31T (100–139 threads), 46T or 47T, 20T, 19T, 37T, and the 97R, 55R and 61R audio and realtime threads. One read showed `0T:1` (b6 s1, after). No launch read priority 4. The full per-launch strings are in `attempt2/launches.csv`.
- **The only priority-4 sample in the whole session:** at 15:42:25, `4T:2` on 2 of 120 threads, for one second. That fell in attempt 2's block 5 idle (15:40:15–15:43:15), 51 s before that block's first `am start`, and within 10 s of the Mac input noted above. It is outside every launch window, so Guard 1 voids nothing.

**VOID count:** OFF 4 of 12 (block 1) and ON 4 of 12 (block 6). Neither condition has more than 4, so the attempt stands.

**Pass line, over the non-VOID kept launches (n = 8 per condition):**

| Clause | Value | Result |
|---|---|---|
| median OFF ≤ median ON + 100 ms | OFF 465.5 (444–508) vs ON 440.5 (418–474) + 100 = 540.5 | PASS |
| every OFF TotalTime < 2,000 ms | max 508 | PASS |
| no OFF Davey ≥ 700 ms | 0 Davey lines in any OFF log | PASS |

**Verdict: PASS.** For context only, not part of the verdict: all 12 kept OFF launches, VOID ones included, ran 417–508 ms with no Davey.

## Outputs
- `.planning/reports/release-3.0.0/qa-engineer-d15-remeasure.md`: this report, the only file committed.
- Artifacts, git-ignored, under `.planning/qa/release-3.0.0/d15-remeasure/` (55 MB, 44 MB of it the two APKs; sizes in bytes):

| Path | Size | What it shows |
|---|---|---|
| `app-debug-c84f1fa.apk`, `apk.sha256` | 22,240,704 / 176 | The build under test; sha `3e974016…c4ed96b` |
| `build.txt`, `build-debug.log` | 121 / 4,020 | Build 14:59:00–14:59:10, exit 0, load before and after, `--stop` |
| `installed-sha.txt`, `art.txt`, `art-a2.txt`, `art-end.txt` | 160 / 60 ×3 | Installed sha; ART `verify`/`cmdline` for attempts 1 and 2 and the resting APK |
| `pre.txt`, `closing.txt` | 168 / 209 | Host at the start (load, HIDIdleTime, pmset) and at the close (load, no qemu, no Gradle, `adb devices` empty) |
| `primon.txt` | 234,055 | Epoch, time, load1 and qemu priority histogram every second, 14:59:52–15:47:48. Guard 1's source |
| `attempt1/{launches.csv,calib.txt,timeline.txt,stats.txt}`, `attempt1/blocks/*.log` (30) | 5.6 MB total | Attempt 1: every launch with the guard columns, the gates, the switch/kill/boot/idle times, the scoring (VOID) |
| `attempt2/{launches.csv,calib.txt,timeline.txt,stats.txt}`, `attempt2/blocks/*.log` (30) | 5.4 MB total | Attempt 2, the same (PASS) |
| `toggle-*.png` (7) | about 141 KB each | Color & motion after each switch. b2–b6 are attempt 2's, because attempt 1's same-named shots were overwritten. Also `a2-pre-OFF` and `restore-OFF` |
| `emulator-*.log` (7) | about 9.4 KB each | Emulator stdout. `boot1` is attempt 1's; b2–b6 and `a2-b1-OFF` are attempt 2's (attempt 1's b2–b6 were overwritten) |
| `bench-state-start.txt`, `bench-state-end.txt`, `timeline-restore.txt` | 961 / 961 / 28 | Bench state at start and end; the restore scale read |
| `db-start/`, `db-after-probe/`, `db-end/` | 4 files each | App data: all byte-identical to each other and to the trace's `db-end/` |
| `bench-resting-armB-599c88d.apk` | 22,224,320 | The resting APK for the restore (sha `b2ee555f…`) |
| `primon2.sh`, `pri.sh`, `boot.sh`, `idle.sh`, `kill.sh`, `block.sh`, `cycle.sh`, `stats2.py`; copied `primon.sh`, `calib.sh`, `toggle.sh`, `state.sh`, `snap.sh`, `hostq.sh`, `quiet.sh` | small | Session helpers, never committed |

**Excerpt** (≤ 20 lines; from `attempt2/stats.txt`, `primon.txt`, `attempt2/calib.txt` and the b3 and b5 OFF logs):
```
block 1 OFF: gate pre=[41, 34, 35] post=[56, 37, 36] -> VOID (slow guest)
block 3 OFF: gate pre=[40, 34, 35] post=[49, 38, 41] -> ok
block 6 ON: gate pre=[37, 47, 36] post=[47, 53, 47] -> VOID (slow guest)
b6-ON-post 15:47:40 sha256_64MiB_ms=[47 53 47] guest_loadavg=[1.09 1.02 0.48 3/2415 5074] host_qemu_cpu=172.9
15:42:24 load1=2.34 qemu_pri=[31T:108 46T:4 97R:1 55R:1 37T:1 20T:1 19T:1 ]
15:42:25 load1=2.34 qemu_pri=[31T:108 4T:2 46T:2 97R:1 55R:1 37T:1 20T:1 19T:1 ]   <- only priority-4 sample (b5 idle)
15:42:26 load1=2.23 qemu_pri=[31T:110 46T:3 97R:1 55R:1 37T:1 19T:1 ]
15:35:27.777 ActivityTaskManager: Displayed …MainActivity for user 0: +482ms   [a2 b3 OFF s2, no Davey]
15:43:21.593 ActivityTaskManager: Displayed …MainActivity for user 0: +508ms   [a2 b5 OFF s3, no Davey]
OFF: kept=12 void=4 valid n=8 min=444 median=465.5 max=508 davey>=700=0
ON: kept=12 void=4 valid n=8 min=418 median=440.5 max=474 davey>=700=0
clause1 median OFF 465.5 <= median ON 440.5 + 100 = 540.5: True
clause2 every OFF < 2000: True (max 508)
clause3 no OFF Davey >= 700: True (max OFF davey 0)
verdict: PASS
```

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-release diff --stat fb66351 HEAD -- . ':(exclude).planning' ':(exclude)docs'` at `c84f1fa` — pass: empty.
- `git … worktree add --detach /Users/ermanergoz/Desktop/PegSolitarie-d15remeasure c84f1fa`; `./gradlew --console=plain :app:assembleDebug` — pass (BUILD SUCCESSFUL in 8s). `./gradlew --stop` — "1 Daemon stopped".
- `shasum -a 256 app-debug-c84f1fa.apk` — `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b` (expected value).
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` ×13 — pass: `boot_completed` within 10–11 s each time, and `emu avd name` = Pixel_9a_bench.
- `adb -s emulator-5584 install -r -d --no-incremental …`; `cmd package compile -m speed -f com.erman.pegsolitarie` ×2 — pass (ART `verify`/`cmdline`; installed sha matches).
- `toggle.sh` ×13 (5 switches per attempt, the attempt-2 pre-switch and the restore) — pass: each read the wanted state, and the scales then read 1.0 or 0.0 as intended, after the switch and again after the reboot and idle.
- `block.sh` ×12 — pass: 60 launches, all COLD; 0 not-quiet host readings.
- `python3 -I stats2.py attempt1` — VOID (ON 8 of 12 VOID). `python3 -I stats2.py attempt2` — PASS.
- `snap.sh save` and `cmp` against `db-start` — pass: byte-identical after the runs and at the end.
- `diff bench-state-start.txt bench-state-end.txt` — only the uptime, the `/data/app` path and `Data-Free` differ. The sha `b2ee555f…`, the scales 0.0, font scale, light mode, gesture nav, wm size and density, no accessibility service and 0 accounts are all unchanged.
- `adb shell sync`; `adb emu kill` — pass: no qemu left, and `adb devices` was empty at 15:48:21.
- `git … worktree remove --force …-d15remeasure`; `git … worktree prune` — pass.
- Not run: the Check, because no code changed and the release Check is the release-manager's. `.planning/log/` was not read.

## Open issues / risks
1. **Guard 2's post-block gate measures residual load from the launches, not only guest speed** (design note for the tech-lead and §4's standardisation; it changes nothing here).
   - All 5 VOID blocks across both attempts failed on the post gate, never the pre gate. Pre gates read 34–41 ms, plus one 47.
   - In 4 of the 5 VOID blocks the offender was the first post run (51–57 ms), taken right after the block's last force-stop. In b6 of attempt 2 it was the second run (53 ms).
   - At that moment the guest loadavg was 0.6–1.8 and host qemu CPU was 90–173 %, against 30–81 % before the block.
   - qemu read priority 31 throughout every one of these gates, so the instrument was not napped.
   - Before standardising Guard 2, a fixed settle before the post gate (for example 10 s) or the median of the three runs would separate a nap (63–140 ms in the trace) from this residue. I ran both attempts identically, as pre-registered, and changed nothing between them.
2. **Lock state was not readable** from this session. HIDIdleTime shows the Mac idle 2.5 h at the start and in use from about 15:42:34. That moment coincides with the lone 1-second priority-4 sample on 2 threads, which fell outside every launch window.
3. **Davey-based frame clause.** No Davey was logged in any of the 60 launches, so the longest OFF frame is known only to be under 700 ms, not its exact value. The trace's un-napped OFF frame 1 was 280–312 ms.
4. **Overwritten duplicates.** Attempt 1's b2–b6 toggle screenshots and emulator stdout logs were overwritten by attempt 2's same-named files. Attempt 1's timeline (switch reads and scale reads) and its launch logs are intact under `attempt1/`.
5. **Emulator only, debug build** (D-10 unchanged). No phone was measured.
6. **Commit trailer:** the commit follows this branch's recent convention, as the session's harness gives it. This is the open question already on file.

**System changes on `Pixel_9a_bench`, and their fate:**
- The probe APK was installed twice (once per attempt), then replaced by the resting APK `b2ee555f…`, compiled `-m speed -f`.
- Remove animations was switched 12 times through the Settings UI. The final state is on (scales 0.0/0.0/0.0), as found.
- 13 extra cold boots and shutdowns, each preceded by `sync`.
- App data was restored from `db-start` and is byte-identical. `/sdcard/ui.xml` was removed.
- No `settings put`, no radio or HAL toggle, no force-stop of another package.

**Host:**
- A background `primon2.sh` ran 14:59:52–15:47:48 and was killed at the close.
- The scratch worktree was removed. No Gradle daemon was left.

## Handoff
Next: producer —
- **tech-lead:** D-15 guarded re-measure PASS on attempt 2, after attempt 1 was VOID. The nap guard never fired. Gate-2 post-run residue caused all the VOIDs (Open issue 1, input for §4).
- **release-manager:** fill §3's D-15 line: OFF median 465.5 ms, ON median 440.5 ms, slowest OFF 508 ms, no frame ≥ 700 ms. Set READINESS D-15 to PASS (this report).
- The bench is down and restored, the scratch worktree is removed, and the Gradle/emulator hold may be released.

VERDICT: PASS — attempt 2 (attempt 1 VOID on Guard 2): median OFF 465.5 ms vs median ON 440.5 ms (+25 ms, within 100); max OFF 508 ms (< 2 s); no OFF Davey logged, so the longest OFF frame is < 700 ms; 4 of 12 kept launches VOID per condition, none napped.
