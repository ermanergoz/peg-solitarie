# qa-engineer report — release-3.0.0: the D-15 cold-start probe (tech-lead triage §1.4)
Date: 2026-11-01
Branch: release/3.0.0 (report only). The build is release head `fb66351`. `cb10b48` landed during the probe and touches only `.planning/release/`, `.planning/reports/` and `docs/` (`git diff --stat fb66351 cb10b48`), so the code under test equals the release head and `a7c52ab`.
Skills loaded: ergoz-soft:standards (role qa-engineer; game; android-compose-game). The snapshot `.claude/rules/VERSION` is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, testing, kotlin-android, compose, puzzle-design and play-store in full, plus `.claude/rules/lessons.md`. Skills: android-emulator-skill, android-testing.
Ran in parallel with: none. The producer held every other agent's Gradle during the bench visit. I checked the host on every launch (Checks).
Ran on: claude-opus-5-5 at effort high (policy). The agent file pins fable at high; FOUNDER.md Q-019 answer 3 puts Fable-pinned roles on Opus until 3.0.0 ships. This probe reviews no code, and every number below is read from a device capture.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)
Authority: `.planning/reports/release-3.0.0/tech-lead-device-pass-triage.md` §1.4 (the pre-registered probe); the method of `qa-engineer-device-pass.md` (D-15) and the bench procedure of `candy-backdrop/qa-engineer-15e.md`.

## Summary
- **VERDICT: FAIL**, on the third clause of the pass line. 8 of the 12 OFF launches have a Davey frame ≥ 700 ms (716–933 ms). All 8 are in blocks 3 and 5.
- **Clause 1 PASS:** median OFF **1,072 ms** ≤ median ON **1,018 ms** + 100 = 1,118 ms.
- **Clause 2 PASS:** every OFF launch is under 2 s (max 1,260 ms). The 4.5 s starts of the original D-15 run did **not** reproduce.
- **VOID:** OFF 0 of 12; ON 1 of 12 (b2 s5, interval 388 ms against a limit of 258 ms). The probe is not VOID.
- Under §1.3 this FAIL makes D-15 **Major**, and it blocks the merge to main. I report it once and did not re-run.
- **The bench is restored as found.** It has the resting APK `b2ee555f…`, scales 0.0, and app data byte-identical at start and end. I ran `sync`, then `emu kill`, and removed the scratch worktree.

## Outputs
- `.planning/reports/release-3.0.0/qa-engineer-d15-probe.md`: this report, the only file committed.
- Artifacts, git-ignored, under `.planning/qa/release-3.0.0/d15-probe/` (about 51 MB, about 44 MB of it the two APKs; sizes in bytes):

| Path | Size | What it shows |
|---|---|---|
| `app-debug-fb66351.apk`, `apk.sha256` | 22,240,704 / 172 | The probe build, `:app:assembleDebug` at `fb66351`. sha256 `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b` |
| `installed-sha.txt`, `art.txt` | 160 / 54 | The installed `base.apk` sha matches the build. ART `arm64: [status=verify] [reason=cmdline] [primary-abi]` after `compile -m speed -f` |
| `bench-resting-armB-599c88d.apk` | 22,224,320 | The bench's resting APK (15e arm B, sha `b2ee555f…`), copied from the candy 15e folder for the restore |
| `build.txt`, `build-debug.log` | 170 / 3,710 | The build at 13:34, `BUILD SUCCESSFUL in 3s`, load before and after |
| `pre.txt`, `boot.txt`, `emulator.log` | 132 / 44 / 13,264 | Host before the boot; boot completed 13:35:28 |
| `bench-state-start.txt`, `bench-state-end.txt` | 961 / 962 | Bench settings, installed sha, `Data-Free` and accounts at start and end |
| `db-start/`, `db-after-probe/`, `db-end/` | 4 files each | App data pulls. All are byte-identical to each other and to 15e's `db-final/` |
| `timeline.txt` | 1,370 | Each switch time, each settle read of the three scales, and the host at every settle |
| `toggle-*.png` | about 141 KB each | Color & motion › Remove animations after each of the 6 switches |
| `launches.csv` | 5,254 | All 30 launches: TotalTime, WaitTime, Displayed, Start proc and clns-9 times, interval, max Davey, scale, host line |
| `blocks/b<N>-<cond>-s<i>.log` | 5.4 MB (30 files) | The `logcat -d -v threadtime` of each launch, cleared before it |
| `stats.txt` | 827 | The statistics and verdict that `stats.py` prints |
| `closing.txt` | 177 | Closing host line and kill time |
| `block.sh`, `toggle.sh`, `stats.py`, `state.sh`, `snap.sh`, `quiet.sh`, `hostq.sh` | small | Session helpers, never committed. `state.sh`, `snap.sh`, `quiet.sh` and `hostq.sh` are copied from 15e; the others are new |

**Excerpt** (≤ 20 lines; from `blocks/b1-OFF-s2.log`, `b5-OFF-s5.log` and `b4-ON-s3.log`, with long tails cut):
```
13:39:09.281 ActivityManager: Start proc 4342:com.erman.pegsolitarie …           [b1 OFF s2]
13:39:09.344 nativeloader (4342): Configuring clns-9 for other apk …base.apk       interval 63 ms
13:39:09.758 ActivityTaskManager: Displayed …MainActivity: +492ms                 no Davey
13:55:06.320 ActivityManager: Start proc 10688:com.erman.pegsolitarie …          [b5 OFF s5]
13:55:06.428 nativeloader (10688): Configuring clns-9 for other apk …base.apk     interval 108 ms
13:55:07.554 ActivityTaskManager: Displayed …MainActivity: +1s260ms
13:55:07.568 HWUI (10688): Davey! duration=916ms
13:55:07.627 HWUI (10688): Davey! duration=933ms
13:51:05.588 ActivityManager: Start proc 8824:com.erman.pegsolitarie …           [b4 ON s3]
13:51:05.717 nativeloader (8824): Configuring clns-9 for other apk …base.apk      interval 129 ms
13:51:07.360 ActivityTaskManager: Displayed …MainActivity: +1s795ms
13:51:07.378 HWUI (8824): Davey! duration=1366ms
13:51:07.412 HWUI (8824): Davey! duration=1380ms
stats: OFF n=12 median=1072.0 max=1260 davey>=700: 8 | ON n=11 median=1018 davey>=700: 3
clause1 True, clause2 True, clause3 False -> verdict: FAIL
```

## The probe (§1.4 field by field)
**Instrument.**
- `Pixel_9a_bench` (`emu avd name`), SDK 36, cold boot: `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`. Boot completed at 13:35:28.
- It was the only emulator on all 30 launch readings and at close (`hostq.sh`: `emulators=1 [Pixel_9a_bench]`).
- `ergoz_studio`, `Pixel_9a` and `Pixel_9` were never started.
- No screenrecord, TalkBack or uiautomator call ran during timed launches. uiautomator ran only inside the switch, before each 180 s wait.

**Host.**
- Before the build: three idle JVMs at 0.0 % CPU (a Gradle 9.5.1 daemon, the Kotlin daemon and a worker), the same picture 15e recorded.
- After my build, `./gradlew --stop` printed "1 Daemon stopped", and no Gradle or Kotlin JVM remained.
- `GradleWrapperMain` count was 0 and busy daemons were none on all 30 readings.
- **Load (`uptime`):**
  - 4.90 before the boot;
  - 1.69 at the block-1 settle read;
  - 4.18 at the end of block 6;
  - 3.52 at close.

  Start and end are both ≤ 5.0.
- **During block 2, load1 read 5.46–5.63** on 4 readings (`quiet=no` on `hostq.sh`'s < 5 rule), with no Gradle and the bench alone. §1.4 states load only at start and end, so this voids nothing. I record it (Open issue 2).

**Build.**
- A detached worktree `/Users/ermanergoz/Desktop/PegSolitarie-d15probe` at `fb66351`, made before the boot.
- `local.properties` was copied from the main repo, because the release worktree has none.
- `./gradlew --console=plain :app:assembleDebug`: pass. The APK sha256 is `3e974016…c4ed96b`. versionName 3.0.0, versionCode 30000.
- The worktree was removed with `git worktree remove --force` and pruned at the end.

**Install.**
- `adb install -r -d --no-incremental`, then `cmd package compile -m speed -f com.erman.pegsolitarie`.
- ART: `arm64: [status=verify] [reason=cmdline] [primary-abi]`, read once.
- The installed `base.apk` sha equals the build's.
- Saved state: the bench's resting data, with its one Classic save. Its hash is identical before and after (`db-start` = `db-after-probe` = `db-end`, and equal to 15e's `db-final`).

**Conditions.**
- OFF = scales 0.0 and ON = 1.0. Every switch was made through Settings › Accessibility › Color & motion › Remove animations, never `settings put`.
- After each switch: 180 s with the launcher idle, then a read-only `settings get global` of all three scales (`timeline.txt`).
- Block 1 had no switch before it: the bench booted at 0.0, so I gave it the same 180 s idle settle after install.

| Block | Cond | Switched | Settled read | Scales |
|---|---|---|---|---|
| 1 | OFF | — (booted at 0.0) | 13:39:10 | 0.0/0.0/0.0 |
| 2 | ON | 13:40:15 | 13:43:21 | 1.0/1.0/1.0 |
| 3 | OFF | 13:44:10 | 13:47:10 | 0.0/0.0/0.0 |
| 4 | ON | 13:47:59 | 13:50:59 | 1.0/1.0/1.0 |
| 5 | OFF | 13:51:47 | 13:54:47 | 0.0/0.0/0.0 |
| 6 | ON | 13:55:34 | 13:58:34 | 1.0/1.0/1.0 |

**Per launch.**
- Each launch ran `am force-stop`, waited 2 s, ran `logcat -c`, then `am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
- 3 s later, the log was dumped.
- All 30 launches were COLD.
- Davey lines were taken for the app pid only.
- The interval runs from `Start proc <pid>:com.erman.pegsolitarie` to that pid's `Configuring clns-9 for other apk …com.erman.pegsolitarie…/base.apk`.

**All launches** (TotalTime ms / framework interval ms / max Davey ms; † = the discarded first of the block):

| Block | s1† | s2 | s3 | s4 | s5 |
|---|---|---|---|---|---|
| 1 OFF | 548 / 73 / – | 492 / 63 / – | 499 / 57 / – | 502 / 83 / – | 446 / 57 / – |
| 2 ON | 1117 / 162 / – | 1018 / 130 / – | 975 / 110 / – | 1473 / 129 / 1150 | 1269 / **388 VOID** / – |
| 3 OFF | 1696 / 204 / 1066 | 1079 / 134 / **737** | 1082 / 104 / **750** | 1075 / 122 / **766** | 1069 / 105 / **766** |
| 4 ON | 1025 / 124 / – | 1044 / 118 / – | 1795 / 129 / 1380 | 968 / 100 / – | 1077 / 129 / – |
| 5 OFF | 1016 / 97 / 721 | 1115 / 105 / **800** | 1078 / 107 / **750** | 1030 / 89 / **733** | 1260 / 108 / **933** |
| 6 ON | 1774 / 227 / – | 1072 / 153 / – | 1010 / 98 / – | 986 / 86 / – | 972 / 129 / 749 |

**Guest-speed control.**
- The median interval of the 12 kept ON launches is 129 ms, so the VOID limit is 258 ms.
- VOID: ON b2 s5 (388 ms), 1 of 12. OFF: none.
- Neither condition has more than 4 VOID, so the probe stands. No re-attempt was owed.

**Pass line, over the non-VOID kept launches.**

| Clause | Value | Result |
|---|---|---|
| median OFF ≤ median ON + 100 ms | OFF 1,072.0 (n 12) vs ON 1,018 (n 11) + 100 = 1,118 | PASS |
| every OFF TotalTime < 2,000 ms | max 1,260 | PASS |
| no OFF Davey ≥ 700 ms | 8 of 12: b3 s2–s5 (737, 750, 766, 766), b5 s2–s5 (800, 750, 733, 933) | **FAIL** |

**Verdict: FAIL.** The line binds, and I did not re-run.

**Time box.** 45 min from the 13:35:28 boot gives a deadline of 14:20:28. The last timed launch ended at 13:59:08 (about 24 min), and the bench was killed at 14:00:03.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-release rev-parse HEAD` — pass: `fb66351` at the build.
- `git … diff --stat fb66351 cb10b48` — pass: the new head touches docs and planning only.
- `git … worktree add --detach /Users/ermanergoz/Desktop/PegSolitarie-d15probe fb66351`, then `./gradlew --console=plain :app:assembleDebug` — pass (BUILD SUCCESSFUL in 3s).
- `./gradlew --stop` — pass ("1 Daemon stopped"; no Gradle or Kotlin JVM remained).
- `shasum -a 256 app-debug-fb66351.apk` — `3e9740164951d12f9e39741fab320323d5e13098cba11a4ce9eccd0ebc4ed96b`.
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass (boot completed 13:35:28; `emu avd name` = Pixel_9a_bench).
- `adb -s emulator-5584 install -r -d --no-incremental …`; `cmd package compile -m speed -f com.erman.pegsolitarie` — pass. ART status as above; installed sha matches.
- `block.sh <n> OFF|ON` ×6 — pass: 30 launches, all COLD.
- `toggle.sh` ×7 (6 condition switches and the restore) — pass: the switch read the wanted state after each tap.
- `python3 -I stats.py` — FAIL (clause 3).
- `snap.sh save` and `cmp` against `db-start` — pass (byte-identical after the probe and at the end).
- `diff bench-state-start.txt bench-state-end.txt` — only uptime, the `/data/app` path and `Data-Free` differ. The sha `b2ee555f…`, the scales 0.0, light mode, font scale, gesture nav, `wm size`/`density`, no accessibility service and 0 accounts are all unchanged.
- `adb shell sync`; `adb emu kill` — pass. No `qemu-system` remained, and `adb devices` was empty.
- `git worktree remove --force …-d15probe`; `git worktree prune` — pass.
- Not run: the Check (no code changed; the release Check is the release-manager's). `.planning/log/` was not read.

## Open issues / risks
1. **D-15 FAIL → Major, blocking the merge to main** (§1.3). The breach is the Davey clause alone: OFF first frames of 716–933 ms in blocks 3 and 5. The 4.5 s starts did not recur. What the numbers show, for the tech-lead's ruling, not as a re-judgement:
   - **Block 1 OFF ran fast.** It came before any switch (TotalTime 446–502 ms, intervals 57–83 ms, no Davey), which matches 15e's 455 ms.
   - **Every block after the first switch ran at about 1 s, in both conditions.** Intervals were about 90–160 ms. ON had Daveys too: 1,150 ms (b2 s4), 1,380 ms (b4 s3) and 749 ms (b6 s5), so 3 of 11 ON launches against 8 of 12 OFF.
   - **The guest-speed control did not flag this regime.** Its limit is relative to ON, and every ON block came after the first switch. The OFF Daveys cluster in the OFF blocks, though, and are steadier (8 of 8 kept post-switch OFF launches against 3 of 11 ON).
   - §1.3's route is next: the android-game-dev's Perfetto trace of a scales-0 cold start on the bench after the toggle wait, then the tech-lead's ruling.
2. **Host load rose above 5.0 during block 2** (load1 5.46–5.63; no Gradle; the bench alone). The start and end loads were within the line, and §1.4 voids nothing for it. Block 2 is ON, and only its s5 is VOID, on the interval rule.
3. **Davey capture window.** Each log was dumped 3 s after `am start -W` returned. A Davey later than that would be missed, which could only add OFF failures, never remove them.
4. **`local.properties`** came from the main repo, because the release worktree has none. It holds Play Games IDs. The original D-15 build in the release worktree ran without one. This does not touch the start path's framework phases, and I record it for completeness.
5. **Model and trailer.** The run was on Opus under Q-019 (header). The commit uses this branch's recent trailer convention, as the session's harness gives it, which is the same open question that 15e and the triage record.

**System changes on `Pixel_9a_bench`, and their fate:**
- The probe APK was installed, then replaced by the resting APK `b2ee555f…`, which was compiled with `-m speed -f` as 15e left it. The final sha is unchanged against the start.
- Remove animations was switched 6 times through the Settings UI and then back on. Final scales are 0.0/0.0/0.0, as found. Reduce blur effects and Dark theme read off throughout (`toggle-restore-scales-0.png`).
- App data is byte-identical at start and end.
- `/sdcard/ui.xml` was deleted.
- `sync`, then `emu kill`. No other AVD was touched.

## Handoff
Next: producer —
- **tech-lead:** D-15 probe FAIL on the Davey clause. Under §1.3 it is Major and blocks the merge to main. Open issue 1 holds the observations for your ruling and the Perfetto route. D-13's fold-in follows §2.4 if a code loop opens.
- **android-game-dev (through the producer, on the tech-lead's ruling):** the scales-0 Perfetto cold start on `Pixel_9a_bench` after the 180 s toggle wait.
- The bench is down and restored. The scratch worktree is removed. P-4 (the Gradle hold) may be released.

VERDICT: FAIL — median OFF 1,072 ms, median ON 1,018 ms (+54 ms, within 100); max OFF 1,260 ms (< 2 s); 8 of 12 OFF launches with a Davey ≥ 700 ms (max 933 ms); 1 ON launch VOID, 0 OFF.
