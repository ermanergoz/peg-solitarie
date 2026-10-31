# qa-engineer report — home-chip-arrival (15e sitting; device checks)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (report only; nothing committed — the producer commits this report and the 15-BUILD-EVIDENCE.md section)
Skills loaded: ergoz-soft:standards (role qa-engineer; the product snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; `.claude/rules/lessons.md`), android-emulator-skill
Ran in parallel with: talkback-board (`PegSolitarie-tb`), reduced-motion-live-gate (`PegSolitarie-rm`), licences-screen (`PegSolitarie-lic`) — their Gradle runs voided four attempts
Ran on: claude-opus-5-5 at effort high — **staffing deviation**: the agent file pins fable; the dispatch staffed Opus under Q-019 because the Fable limit is reached (Q-019's 2026-10-23 addendum had returned supervising roles to fable). This review therefore runs on the developer's own model (rules/studio.md, Model and effort); mitigation: the Check-independent device measurement below, run by the reviewer.
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot; no `.planning/recall/RECALL.md`)

## Summary
- **15e PASS** on `Pixel_9a_bench`: delta 16.0 ms (B 398.5, A 382.5), floor 6.0 ms, threshold 100 ms; all nine preconditions held, P-1.2 diff clean, `Data-Free` 13.60 / 13.53 GB. Arm A `6aa7624`, arm B `7448713`.
- It took five attempts: attempts 1–4 were **VOID** because another worktree started Gradle inside each one (precondition 3); attempt 5 (13:05:57–13:12:02) was quiet throughout.
- **The device checks were not run: 15v (parts 1–4), 15w's device half, 18g, 26b and 29 (2) are BLOCKED.** This session stopped after the sitting because its context budget ran out (the harness warned at 94 %). One 15v recording was made (native, one save, animations off) and not read. The bench is in its resting state, unchanged from the sitting.
- **Found before the sitting:** the bench's ladder and save were gone. Since 2026-10-18 something had replaced the APK (`dbe100e2…`, neither arm) and the app data (0 `level_result` rows, no save). I re-seeded them (precondition 8).

## Outputs
- `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` — new section "Criterion 15e — `bugfix/home-chip-arrival`, 2026-10-24 — PASS": builds, the nine preconditions with values, the null table, both arm tables, P-1.2 diff, both `Data-Free` readings.
- This report.
- Artifacts (git-ignored, `.planning/qa/home-chip-arrival/`, 47 MB):
  - `armA-6aa7624.apk` (20 MB), `armB-7448713.apk` (20 MB): the two arms, sha256 `35effc51…db64a3d` and `9d507a88…2decaa`.
  - `build-armA.log`, `build-armB.log`: both `BUILD SUCCESSFUL`, `EXIT 0`.
  - `sitting/pre.txt`: P-1.2 diffs, start `Data-Free`, cold boot time, post-boot device state, host state.
  - `sitting/attempt-5/` (the sitting of record): `samples.csv` (100 launches), `swaps.csv` (20 swaps with sha, ART status and host line), `monitor.log` (37 host readings at 10 s), `before.txt`, `after.txt`, `stats.txt`.
  - `sitting/attempt-1..4/`: the voided attempts, same files.
  - `sitting/db-after/`: the database and preferences pulled after the sitting.
  - `quiet-poll.log`: every host poll, 10:07–13:05.
  - `bench-state-before.txt`, `bench-state-after.txt`: the bench's settings at both ends.
  - `seed-home.png`, `seed-home-relaunch.png` (≈200 KB each): Home after seeding, with "Stars 401", "Level 111" and the Continue Classic chip.
  - `seed-one-save/`: the seeded data, kept for restores.
  - `15v/nat-1save-off/` (5.3 MB): the one recording, 22 frames, **unread**.
  - Helpers: `state.sh`, `quiet.sh`, `seed.sh`, `setpref.py`, `snap.sh`, `sitting.sh`, `stats.py`, `run.sh`, `record.sh`.

## Checks
- `git merge-base develop bugfix/home-chip-arrival` — `6aa7624` (arm A). `git diff --stat 7448713 5a38f49` — only the developer report, so building at `5a38f49` builds arm B's code.
- `git diff --stat 6aa7624 7448713 -- app/schemas` — empty. `git diff 6aa7624 7448713 -- app/src/main | grep PreferencesKey` — no match, so no DataStore key changed.
- `./gradlew --no-daemon :app:assembleDebug` (arm A in a detached scratch worktree; arm B in `PegSolitarie-home`) — pass, both. I did not run the Check: this is a sitting, and the Check is the developer's gate.
- `apksigner verify --print-certs` — pass: one signer, `8a4e64a3…c1a4a6`, on both arms. `aapt2 dump resources` — pass: arm B carries the `(v27)` and `(night-v27)` theme styles; arm A does not.
- `diff Pixel_9a.avd/config.ini Pixel_9a_bench.avd/config.ini` — pass: only `AvdId`, `avd.ini.displayname` and `disk.dataPartition.size` differ. `diff Pixel_9a.ini Pixel_9a_bench.ini` — pass: only `path` and `path.rel` differ.
- `quiet.sh` host poll, before the sitting and every 10 s during it — attempt 5 was quiet throughout. Attempts 1–4 were not:

  | Attempt | Start | First busy reading | Busy process | Verdict |
  |---|---|---|---|---|
  | 1 | 10:44:24 | 10:47:46 | `PegSolitarie-rm` | VOID (precondition 3) |
  | 2 | 11:23:57 | 11:24:48 | `PegSolitarie-tb` `depthSweep` | VOID |
  | 3 | 11:37:22 | 11:37:33 | `tb-probe` | VOID (also floor 183.5 ms) |
  | 4 | 12:48:00 | 12:49:31 | another build | VOID |

  Their arm numbers are not evidence.
- `sitting.sh null` then `sitting.sh arms`, attempt 5 — **PASS**:
  - null medians N1 402.0 ms, N2 396.0 ms, so floor 6.0 ms;
  - arm medians A 382.5 ms, B 398.5 ms, so delta 16.0 ms against a threshold of 100 ms;
  - 100/100 launches `COLD`; 20/20 swaps sha-matched; ART `[status=verify] [reason=cmdline]` on both arms.
- `uptime` — before `2.31 2.69 3.05`, after `1.51 2.15 2.69`, both ≤ 5.0.
- `dumpsys diskstats` — `Data-Free` 13600820K at the start and 13533576K at the end, both ≥ 4 GB.
- Ladder after the sitting — `sqlite3` on the pulled database: 200 rows, `SUM(stars)` 401, one `classic` resume row, `challenge_current_level` 111.
- **15v, 15w device half, 18g, 26b, 29 (2) — NOT RUN (blocked; see Open issues).**

## Open issues / risks
1. **BLOCKED: the device checks.** 15v parts 1–4 (part 2 with the closing chip's shadow, auditor finding 4), 15w at 200 % in light and dark, 18g, 26b and 29 (2) were not run. 15v says it runs "only after the branch's own 15e sitting, in the same session". That session has ended, so the tech-lead must rule how to proceed:
   - (a) accept a fresh QA session on the bench that runs 15v without re-sitting, since the APK is unchanged (arm B `9d507a88…` is still installed); or
   - (b) re-sit 15e, then run the checks.

   The other checks may run on `Pixel_9_clean` or the GM6. They are not timing checks.
2. **D-2, the boot.** The bench was cold-booted once for this session, at 10:13:54. The sitting of record ran after four voided attempts on that boot, so it was not on a fresh boot. The same-attempt null control (floor 6.0 ms) is the protocol's guard against that drift. I record it for the tech-lead rather than rule on it.
3. **The bench was not as the brief described.** The brief expected phase 15's arm B, the ladder and the save. I found APK `dbe100e2…`, an empty ladder and no save; the app data was dated 2026-10-21 21:25. I re-seeded:
   - 200 rows, written with `seed.sh`;
   - `challenge_current_level` 111, written with `setpref.py`;
   - one Classic save, made by one jump on English-33.

   The tool that last used the bench is unknown. It did not honour the reservation. **The producer should find out who installed `dbe100e2…`.**
4. **The quiet host was not held.** Four of five attempts were voided by `PegSolitarie-tb`, `PegSolitarie-rm` and `PegSolitarie-lic` builds. Before the next sitting (candy), the producer should declare P-4 and actually hold builds.
5. **The bench's resting state now** (`bench-state-after.txt`):
   - running on `emulator-5554`, arm B `9d507a88…` installed;
   - the ladder seeded: Stars 401, Level 111, one Classic save;
   - animations 0/0/0, font 1.0, light mode, gesture navigation;
   - `wm size` and `wm density` at the physical values; TalkBack off; Accounts: 0;
   - `Data-Free` 13.53 GB.

   I changed no setting. The GM6 was not touched.

## Handoff
Next: producer —
- Commit the 15-BUILD-EVIDENCE.md section and this report.
- The 15e PASS can go to the tech-lead now.
- Route Open issue 1 to the tech-lead (re-sit, or run the checks without one) and re-dispatch QA for the device checks. Their verdicts are still owed before sign-off.
- Look into Open issue 3, the bench reservation.

VERDICT: PASS WITH BLOCKED ITEMS
