# qa-engineer report — phase 13 upload baseline

**A healthy device uploads: on `Pixel_9_clean` `change_pending` went `false` against that device's own brand-new commit within 60 s and stayed there for 23 minutes and five relaunches — the first proof in this investigation that a write from a device reaches the server. A conflict did not break it, because I could not manufacture one on the clean device; but the clean device's write *did* reach the wedged control as a conflict, and the control resolved it, logged `Inconsistent DB` again, and is still unable to upload.**

Date: 2026-09-20
Branch: feature/phase-13-cloud-save (58f30ec..58f30ec — QA committed nothing; `git status --short` clean, this report left uncommitted)
Skills loaded: ergoz-soft:standards (role qa-engineer). The repo rules snapshot is `.claude/rules/VERSION` 0.1.0 and the plugin is 0.1.1, so I read the plugin copies of studio.md, reporting.md, testing.md, logging.md, memory.md and kotlin-android.md, plus the repo `.claude/rules/lessons.md`. `android-emulator-skill` and `android-testing` were not invoked: the brief pins the existing session helpers (`launch.sh`, `pread.sh`, `mask.sh`, `tools/ui.py`, `robot.py`, `prefs.py`), no APK was built or installed, and no test was written — recorded as a deliberate substitution.
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Step 1 — yes.** The clean device solved Challenge level 9 at 16:42:58, committed at 16:43:00.071, and on the next launch reported `change_pending = false` with `last_modified_at` equal to **its own** 16:43:00.071 commit. Held across +60 s, +3 min, +10 min, +12 min and +23 min, five force-stop/relaunch cycles, `conflicts_resolved = 0` every time.
- **Step 2 — yes, and it arrives as a conflict.** `Pixel_9a`'s single relaunch at 16:53 produced its **first conflict in 13½ hours** (`conflicts_resolved` 0 → 1), the `Found conflict` + `Inconsistent DB:null/4` pair from 03:12 reproduced exactly, and a new resolution commit at 16:52:59.692. `change_pending` is still `true`: it received the write and still cannot send one.
- **Step 3 — not manufacturable, and I did not fake it.** A genuine server-side divergence needs a second device that can upload. The only other device is the wedged control, and I verified at 16:55 that its 16:52:59 resolution commit had **not** reached the server. I stopped at that point.
- **Step 4 — CLOUD-01 re-verified independently**, from 03:14:22.272, with zero app-owned UI. The only sync-related pixel in the whole session is Google's own greeting chip, visible at t≈1.5 s and gone by t≈2.5 s.
- **New fact that overturns a live hypothesis:** the clean device now runs Play services **26.34.36** (self-updated 16:33:25, before the solve) — **newer** than the wedged `Pixel_9a`'s 26.15.33. The "both wedged devices run GMS 26.x, the working one runs 25.07.34" pattern in `tech-lead-conflict-wedge.md:87` is falsified.

## Devices and configuration

| | `emulator-5554` = Pixel_9a (wedged control) | `emulator-5556` = Pixel_9_clean |
|---|---|---|
| Product / build id | `sdk_gphone16k_arm64`, BE4B.251210.005 | `sdk_gphone64_arm64`, BP22.250325.006 |
| Play services core | 26.15.33 (`lastUpdateTime` 2026-04-23) | **26.34.36** (`lastUpdateTime` **2026-09-20 16:33:25**) |
| Installed `base.apk` sha256 (first 16) | `1c480a55cc614da0` | `1c480a55cc614da0` (identical) |
| Network | VALIDATED | VALIDATED |
| Clock vs host | ~9 s behind | equal |

All `last_modified_at` values below are decoded from epoch millis and are therefore stamped by the writing device's clock (E2's finding, `qa-engineer-e2-probe.md:61`). Pixel_9a's values read ~9 s early in host terms.

## Step 1 — upload baseline on the clean device (no conflict)

Solve: Challenge **level 9**, `robot.py emulator-5556`, 4 pegs / 3 moves, "Perfect!", 5280, 1 peg left, ladder 9 → 10. Host 16:42:52 → 16:42:58.

| Read | Launch (host) | Read (host) | Δ from commit | `change_pending` | `last_modified_at` (decoded) | `conflicts_resolved` | `push_pending` | `_last_push_stage` |
|---|---|---|---|---|---|---|---|---|
| 00 baseline, no launch | – | 16:41:21 | – | **False** | 03:14:22.272 (server, stale) | 0 | *key absent* | *key absent* |
| s1-a post-solve, no relaunch | – | 16:43:16 | +0:16 | False | 03:14:22.272 (keys are from the 16:31 pull) | 0 | **False** | **COMMITTED** |
| s1-b | 16:44:08 | 16:44:53 | **+1:53** | **False** | **16:43:00.071 — this device's own new commit** | 0 | False | COMMITTED |
| s1-c | 16:45:23 | 16:46:08 | +3:08 | False | 16:43:00.071 (unchanged — push guard held) | 0 | False | COMMITTED |
| s1-d | 16:52:10 | 16:52:55 | +9:55 | False | 16:43:00.071 | 0 | False | COMMITTED |
| s3-a | 16:55:06 | 16:55:51 | +12:51 | False | 16:43:00.071 | 0 | False | COMMITTED |
| s3-b | 17:06:09 | 17:06:55 | +23:55 | False | 16:43:00.071 | 0 | False | COMMITTED |

**Interpretation, stated explicitly as the brief requires.** Yes — `change_pending` is `false` with `last_modified_at` equal to THIS device's own new commit (16:43:00.071, produced by the 16:42:58 solve), on the first read after the write and on every read for the next 23 minutes. By the SDK's own documentation of `hasChangePending()` (*"Once all changes have been flushed to the server, this will return false"*, quoted at `tech-lead-conflict-wedge.md:34`), that is the flush happening. **This is the first observation in the whole investigation of a device write reaching the server.** Step 2 corroborates it independently and from the other side.

Three secondary facts from the same run:
- **The push keys were born here.** Before the solve the clean device had no `play_games_cloud_push_pending` and no `play_games_cloud_last_push_stage` at all — it had genuinely never written. After the solve both exist, at `False` / `COMMITTED`.
- **The push guard held on a healthy device too.** Four launches after the write committed nothing: `last_modified_at` never moved off 16:43:00.071.
- **The launches were quiet.** 0 `Found conflict`, 0 `Inconsistent DB`, 0 `cover image is too large`, 0 snapshot lines on each of the five clean-device launches (~1,300–1,600 log lines each). The only `PlayGamesServices` lines are 30 `UiUtils: AppVersion: Cannot find the installed destination app` warnings per launch — the Play Games companion app is not installed on this AVD.

## Step 2 — the clean device's write reaches the wedged control

One relaunch of `Pixel_9a` only, at 16:53:04, read at 16:53:49.

| Key | Pixel_9a baseline 16:41:21 (no launch) | Pixel_9a after the single relaunch, 16:53:49 | Pixel_9_clean 17:06:55 |
|---|---|---|---|
| `..._pull_opened` | True | True | True |
| `..._pull_change_pending` | **True** | **True** (still) | **False** |
| `..._pull_conflicts_resolved` | 0 | **1** (0 → 1) | 0 |
| `..._pull_last_modified_at` | 04:53:00.912 (its own, since 04:53) | **16:52:59.692 (new, stamped this launch)** | 16:43:00.071 (its own) |
| `..._cloud_push_pending` | False | False | False |
| `..._cloud_last_push_stage` | COMMITTED | COMMITTED | COMMITTED |
| `challenge_current_level` | 10 | 10 | 10 |

**Answer: Pixel_9a now sees the clean device's level — it arrived as a conflict, not as a plain newer copy, and its screens are unchanged because the payload was a subset.**

- `conflicts_resolved` went 0 → 1 on the first launch after the clean device's 16:43 write. Pixel_9a had logged **zero** conflicts on every launch since 03:12: 13 reads in E2 (04:45–05:07), G1 at 14:34, G2 at 16:11. A Saved Games conflict means the server's version advanced under a device that holds an un-flushed local change — which is precisely Pixel_9a's state and precisely what the clean device did at 16:43. Confidence that this is the clean device's write arriving: high (~90%); the residual is that I never read the server's bytes directly, only the conflict signal and the resolution.
- `last_modified_at` moved for the first time since 04:53, to 16:52:59.692 — stamped during this launch (the `Found conflict` line is at device 16:52:59.533, 0.16 s earlier).
- `change_pending` is **still True**. Pixel_9a received the server's copy, ran the app's conflict path, merged and committed — and its own commit still has not left the device. It is not un-wedged.
- **Home / Classic are unchanged and nothing was lost.** `Continue — Level 10`; English-33 3/3, European-37 3/3, Asymmetric 3-3-2-2 3/3, Diamond-41 0/3, Wiegleb-45 0/3 — identical to its pre-existing state. That is the expected union outcome: the clean device's payload (level 10, European-37 3/3) is a strict subset of Pixel_9a's. It is also the first field observation of the CLOUD-02 merge running against a genuinely foreign server copy without dropping a key.

Masked log excerpt, `emulator-5554`, the whole of the conflict window (full stack in `s2-p9a-gms-lines.masked.txt`):

```
16:52:59.533 I/PlayGamesServices[PgsSnapshotAgent]: Found conflict for snapshot progress
16:53:00.236 E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4
16:53:00.236 E/PlayGamesServices[ExtendedSnapshotsHelper]: java.lang.Throwable
16:53:00.236   at m26.hou.f(:com.google.android.gms.optional_games@261533017@26.15.33 (100000-0):11)
16:53:00.236   at com.google.android.gms.games.provider.PlayGamesContentProvider.a(...:366)
16:53:00.236   at m26.dyq.delete(...:47)
16:53:00.236   at android.content.ContentProvider.delete(ContentProvider.java:2061)
16:53:00.236   at android.content.ContentProviderOperation.apply(ContentProviderOperation.java:352)
16:53:00.236   at m26.eyb.s(...:48)  /  m26.ifv.g(...:330)  /  m26.ifu.handleMessage(...:581)
```

The 03:12 signature reproduced exactly, with one difference: **one** `Found conflict` this time, against two at 03:12.

## Step 3 — the conflict causation test: NOT MANUFACTURABLE

**I cannot manufacture a genuine server-side divergence with the devices available, and I did not fake one. I stopped here rather than running a conflict-shaped test that is not a conflict.**

The reasoning, with the observation that settles it:

1. A conflict requires the server's copy to advance under a device that is holding an un-flushed local change. Taking the clean device offline, solving two levels and coming back online gives it pending local changes against a server copy that has **not** moved — GMS resolves that as an ordinary flush, not a conflict. Running it would produce a green "no conflict, uploaded fine" result that proves nothing about the conflict path and would read, wrongly, as a causation test.
2. The only second writer available is the wedged `Pixel_9a`, and the brief forbids writing to or solving on the control.
3. Not an assumption — measured: after Pixel_9a's 16:52:59.692 resolution commit, I relaunched the **clean** device at 16:55:06 (a read on the permitted device) and it still reported `last_modified_at = 16:43:00.071`, its own write. **Pixel_9a's commit did not reach the server**, so it cannot be used to move it. Re-checked at 17:06:55 — unchanged.

What this means for F-3/F-4: **the app's conflict path is neither convicted nor exonerated by this session.** What I can state as fact is the adjacent pair:
- A device that has **never** hit a conflict (`conflicts_resolved = 0` on every read of its life) uploads correctly on the same APK, the same account and the same snapshot record.
- A device that hits a conflict runs the app's resolution path, logs `Inconsistent DB`, and remains unable to upload — but it was already unable to upload *before* that conflict, so this is correlation, not causation.

The experiment that would convict it needs **two devices that can both upload**. There is exactly one today.

## Step 4 — CLOUD-01 evidence for the UAT

**What was restored, and from where.** `Pixel_9_clean` is a new AVD with its own `userdata-qemu.img` created 16:31, no app data and no tutorial. On first launch it restored, from the server snapshot last written at **03:14:22.272 on 2026-09-20**:

- Challenge ladder at **level 9** (`Continue — Level 9` on Home)
- **European-37 3/3**
- English-33 0/3, Asymmetric 3-3-2-2 0/3, Diamond-41 0/3, Wiegleb-45 0/3

I re-verified this independently of the producer's finding, before touching the device: my own `00-baseline` read at 16:41:21 shows `..._pull_opened = True`, `..._pull_change_pending = False`, `..._pull_conflicts_resolved = 0`, `..._pull_last_modified_at = 1789863262272` → **03:14:22.272**, `challenge_current_level = 9`, and **no `play_games_cloud_push_pending` and no `play_games_cloud_last_push_stage` keys at all** — the device had never written, so every byte on screen came from the server. Screenshots `s4-01` (Classic) and `s4-02` (Home) are that state.

**D-03 negatives — nothing app-owned appeared.** Across the whole clean-device session (the restored launch plus seven force-stop/relaunch cycles, one solve and one Results screen) I saw:

- **no toast, no dialog, no snackbar, no banner** of the app's own, at any point, including the launch that restored and the launch that uploaded;
- **no sync row in Settings.** The Settings screen's `Play Games` section is exactly four static rows: a sign-in row (`Signed in as <gamer tag>`) with the subtitle *"Leaderboards, achievements and cloud save"*, `Leaderboards` / *"See how your scores rank"*, `Achievements` / *"View the achievements you've earned"*. There is **no** sync status, no "last synced", no cloud-save toggle, no error row and no progress indicator. The word "cloud save" appears once, in a static descriptive subtitle, never as state.
- **no failure surface.** Even the wedged control, which has failed to upload for 13½ hours, shows nothing.

**Stock "Welcome back" popup — caught.** Google's own greeting chip appears at **t ≈ 1.5 s** after launch (avatar + `Hey there, <gamer tag>` + `Level 7`, top of screen, over the app's Home) and is **gone by t ≈ 2.5 s**. It is Google's UI, not the app's. Evidence: `s4-11-stock-playgames-greeting-redacted.png` — the tag and level text are blacked out by me; the unredacted captures at t=0.8 s and t=1.5 s were deleted, as was the Settings screenshot, because both render the gamer tag (see Privacy below).

## Hypothesis consequences (facts only; the tech-lead owns the conclusions)

1. **The GMS-version pattern is dead.** `tech-lead-conflict-wedge.md:87` rests on "both wedged devices run GMS 26.x (26.15.33, 26.08.34); the one device that works runs 25.07.34". Play services on `Pixel_9_clean` self-updated to **26.34.36** at **16:33:25**, nine minutes *before* the solve. The device that uploads is running the newest GMS of the three.
2. **The tech-lead's E3 has its answer, and it is the first row of the D table.** `change_pending` cleared to `false`. Per that table: uploads work on a clean device; the app, the project and the account are exonerated a third time; the wedge is device state. The GMS-26.x half of that row is not live (see 1).
3. **`COMMITTED` and the upload are now separable in the field.** `_last_push_stage = COMMITTED` with `push_pending = False` is present on all three devices including both wedged ones. The key that distinguishes them is `..._pull_change_pending` on the **next** launch, and on a healthy device it clears within 60 s. Any UI or test that reads `COMMITTED` as "uploaded" is wrong; `change_pending = false` on the following pull is the honest signal.
4. **The server is writable and serving.** It accepted a second write today (16:43:00.071), its first since 03:14:22.272, and served it onward to `Pixel_9a` within ten minutes.

## Outputs
All under `/Users/ermanergoz/Desktop/PegSolitarie/.planning/qa/phase-13-conflict/` (git-ignored, 1.6 MB total). `QA_OUT` pointed at `qa-shots/`.

- `00-baseline-emulator-5554-prefs.txt`, `00-baseline-emulator-5556-prefs.txt` (0.5 KB each) — both devices before I touched anything.
- `s1-a-postsolve-nolaunch-…`, `s1-b-plus60-…`, `s1-c-plus3min-…`, `s1-d-plus10min-…`, `s3-a-clean-after-p9a-conflict-…`, `s3-b-clean-plus23min-emulator-5556-prefs.txt` (0.5 KB each) — the six clean-device reads of step 1 / step 3.
- `s2-a-p9a-single-relaunch-emulator-5554-prefs.txt` (0.5 KB) — the control's one relaunch.
- `s2-p9a-gms-lines.masked.txt` (4.3 KB) — every `PlayGamesServices` line of that relaunch, masked; the full `Inconsistent DB` stack.
- `qa-shots/s4-01-clean-classic-restored(-s).png` (184/100 KB) — the restored Classic catalog, European-37 3/3 alone.
- `qa-shots/s4-02-clean-home(-s).png` (72/44 KB) — `Continue — Level 9`, no app-owned sync UI.
- `qa-shots/s1-01-clean-level9-start(-s).png`, `s1-02-clean-level9-result(-s).png` (96/48, 68/48 KB) — the solve and its Results screen ("Perfect!", 5280, 1 peg).
- `qa-shots/s2-01-p9a-home(-s).png`, `s2-02-p9a-classic(-s).png` (76/44, 184/108 KB) — the control after the conflict: level 10, three boards at 3/3, nothing lost.
- `qa-shots/s4-10-launch-t2.5/t4/t6(-s).png` (72/44 KB each) — the same launch at 2.5 s, 4 s, 6 s: the chip is gone, Home is clean.
- `qa-shots/s4-11-stock-playgames-greeting-redacted(-s).png` (76/32 KB) — the stock chip at t≈1.5 s with the gamer tag blacked out.
- `launch.sh`, `pread.sh`, `mask.sh` — session helpers, copied from `phase-13-e2` with the output directory changed. `pread.sh` masks before anything touches disk; I narrowed its mask relative to `mask.sh` (see Checks) and documented why in the script.
- `.planning/reports/phase-13/qa-engineer-upload-baseline.md` — this report (uncommitted).

## Checks
- `adb devices -l` — pass (`emulator-5554`, `emulator-5556`, both `device`; no physical device attached).
- `pread.sh <serial> 00-baseline` on both, before any launch — pass (the baseline row of each table).
- `robot.py emulator-5556` on Challenge level 9 — pass (4 pegs, 3 moves, "Perfect!", 5280, 1 peg, ladder 9 → 10; solve 16:42:52–16:42:58).
- `launch.sh emulator-5556 s1-b-plus60 | s1-c-plus3min | s1-d-plus10min | s3-a-… | s3-b-…` (force-stop, `am start`, 45 s, masked `prefs.py`, log counts) — **pass, and this is the finding**: `change_pending = False` with `last_modified_at = 16:43:00.071` on all five.
- `launch.sh emulator-5554 s2-a-p9a-single-relaunch` — ran **once**, as instructed — pass as observation (`conflicts_resolved` 0 → 1, `change_pending` still True, new `last_modified_at`).
- `grep -c 'Found conflict' / 'Inconsistent DB' / 'cover image is too large'` per launch — pass (clean device: 0/0/0 on all five launches; control: **1/1**/0 on its one launch).
- `ui.py emulator-5556 dump` on Home, Classic, Settings and Results; `ui.py emulator-5554 dump` on Home and Classic — pass (the catalogs and the D-03 negatives above).
- Launch-burst screencap at t = 0.8 / 1.5 / 2.5 / 4 / 6 s on `emulator-5556` — pass (stock chip present at 1.5 s, absent at 2.5 s).
- `dumpsys package com.google.android.gms | grep versionName` and `firstInstallTime|lastUpdateTime` on both — pass, and material: 26.15.33 (2026-04-23) vs **26.34.36 (2026-09-20 16:33:25)**.
- `pm path` + `sha256sum base.apk` on both — pass (`1c480a55cc614da0` on both; same build as the whole phase-13 session).
- `dumpsys connectivity | grep VALIDATED` on both — pass (online throughout).
- `grep -rlEi '@gmail|@[a-z]+\.com|a_[0-9]{10}|ELLIDED:[0-9]|<the gamer tag, spelled out in the shell only>|[0-9]{11,}'` over every `.txt` and `.sh` artifact — pass (0 files). Screenshots checked by eye, one redacted and three deleted (below). The tag itself is deliberately not written here, not even as a grep pattern — my first draft of this line spelled it out and the final scan caught it.
- `git check-ignore -v .planning/qa/phase-13-conflict/x.txt` — pass (`.gitignore:45:.planning/qa/`).
- `git status --short` — pass (clean; I committed nothing and edited no app source, no Gradle file and no test).
- `./gradlew check` — **not run**, forbidden by the brief ("do NOT run Gradle"). No code changed in this session, so nothing here affects the tech-lead's standing conditional sign-off.
- `pm clear`, Play services data clearing, account change, Play Console, snapshot selection/deletion/Refresh in the picker, any install, any network toggle, any emulator restart — **none performed**.

## Open issues / risks

- **BUG (high, pre-existing, unchanged) — `Pixel_9a` cannot upload, and a conflict does not clear it.**
  - Reproduce: launch the app on `emulator-5554`, wait 45 s, read `play_games_cloud_last_pull_change_pending`.
  - Expected: `false` within ~60 s of a commit, as on `Pixel_9_clean`.
  - Actual: `true` continuously since ~03:14 today, across every launch including the 16:53 one that successfully **received and resolved** a foreign server copy.
  - Evidence: `s2-a-p9a-single-relaunch-emulator-5554-prefs.txt`, `s2-p9a-gms-lines.masked.txt`, `qa-shots/s2-01`, `s2-02`.
  - Scope after today: **confined to the two old AVD instances.** A third device on the same APK, account, project and snapshot record does not reproduce it.
- **The causation test for F-3/F-4 is still unrun and needs a second uploading device.** It cannot be run from the current device set. The cheapest route is one more clean AVD (one founder sign-in), which then also closes CLOUD-02 properly; that is the producer's and tech-lead's call, not mine.
- **The discriminating step I left untried, and recommend next: a second relaunch of `Pixel_9a`.** Its 16:53 launch is the first time the conflict path has run on it since 03:12, and its outcome is genuinely unknown — the resolution may have cleared the stuck state or may not. One more relaunch and one probe read answers it. I did not do it because the brief says "relaunch Pixel_9a ONCE"; it needs an explicit instruction.
- **`tech-lead-conflict-wedge.md`'s GMS-26.x pattern (line 87) should be struck**, and its E3 D-table row 1 should be read without it. I am not rewriting another role's report; flagging it for the producer.
- **Timestamp semantics, for whoever writes the UAT wording.** `last_modified_at` is device-stamped; on its own it still proves only a local commit. What proves an upload is `change_pending = false` on the *next* pull with `last_modified_at` equal to this device's own last commit — both halves are needed, and the pair is what makes the clean device distinguishable from the wedged ones.
- **`play_games_cloud_last_push_status` was absent on every read on both devices**, as in E2. Nothing failed with a status code.
- **Privacy — two near-misses, both contained, nothing reached a durable artifact.**
  - The Settings screen renders `Signed in as <gamer tag>`. I screenshotted it before realising, and **deleted both the full-size and downscaled files within the same minute**; no masked variant was kept, and the Settings evidence in this report is text I transcribed with the tag removed.
  - The stock Play Games greeting chip renders the gamer tag and Play Games level. The t=0.8 s and t=1.5 s captures were **deleted**; the kept `s4-11-…-redacted.png` has that region blacked out by me.
  - `ui.py … dump` printed the gamer tag into my tool output twice (Settings, and implicitly via the chip). It reached no file. If the studio log records tool output the producer should know. The tag appears nowhere in this report or in any artifact.
  - I also found and fixed a masking defect of my own: `mask.sh`'s project-number rule (`[0-9]{11,}`) eats the 13-digit epoch millis of `last_modified_at`, which is the value under test. My first two `pread.sh` runs therefore crashed rather than writing a wrong value. I narrowed `pread.sh`'s mask to player id / email / URL, documented the reason in the script, and re-ran; `mask.sh` itself is unchanged and still used for log excerpts.
- **Device state changed — `Pixel_9_clean` (emulator-5556).** Challenge level 9 solved, ladder 9 → 10; its level-9 game is gone. Seven force-stop/relaunch cycles. Left on Home. It has now written to the cloud once (16:43:00.071) and is the only device with a healthy uploader.
- **Device state changed — `Pixel_9a` (emulator-5554).** One force-stop/relaunch. It resolved one conflict and made one new local commit (16:52:59.692) — the app's own behaviour, not a write by me. No solve, no clear, no account or Play services change. I navigated Classic → back → Home. Left on Home at level 10; its catalog is unchanged and nothing was lost.
- **Scratchpad hygiene.** I removed only my own eight `c-*` files, by exact name (E2's lesson). Pre-existing files left by other agents were not touched.

## Handoff
VERDICT: PASS — the upload baseline is established (step 1), the cross-device direction clean → wedged is observed for the first time (step 2), step 3 is reported as not manufacturable rather than faked, and CLOUD-01 is re-verified with its D-03 negatives (step 4). PASS is scoped to this investigation: **CLOUD-02 stays HOLD** — it needs two devices that can both upload, and there is one.

Next: tech-lead.
1. Your **E3 has returned row 1**: `change_pending` cleared on the clean device. Uploads work; the wedge is device state on the two old AVDs.
2. **Strike the GMS-26.x pattern** from `tech-lead-conflict-wedge.md:87` — the uploading device runs 26.34.36, newer than either wedged one.
3. **Decide the two device questions:** (a) may QA relaunch `Pixel_9a` a second time to see whether the 16:53 conflict resolution un-wedged it; (b) is a second clean AVD (one founder sign-in) worth it — it is the only way to run CLOUD-02 and the only way to run the F-3/F-4 causation test.
4. **B (the F-3 fix) is still gated on nothing I found.** The clean device runs the same conflict-path code and uploads; nothing in this session argues for shipping it sooner or later than your sequencing says.
5. For the producer: `.planning/qa/phase-13-conflict/` (1.6 MB) is the artifact directory to delete after this task's STATUS.md Done line.
