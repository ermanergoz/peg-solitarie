# qa-engineer report — phase 13 final UAT

**Test 10 PASS · Test 11 PASS (D-04 sub-check partial) · Test 12 PASS · Test 13 PASS.** All four phase-13 UAT
tests ran to completion on two healthy devices for the first time. The headline finding is not a test result:
a brand-new clean device (`Pixel_9b_clean`, GMS **26.34.36**) resolved a genuine conflict, logged the exact
`Inconsistent DB:null/4` wedge signature inside `PlayGamesContentProvider.delete`, **and went on to upload
normally** — `change_pending` returned to `false` against its own new commit. The app's conflict path (F-3/F-4)
is therefore exonerated for the fourth time, and this time with the wedge's own log signature present.

Date: 2026-09-20
Branch: feature/phase-13-cloud-save (bc93869..bc93869 — QA committed nothing; `git status --short` clean, this report left uncommitted for the producer)
Skills loaded: ergoz-soft:standards (role qa-engineer). The repo rules snapshot is `.claude/rules/VERSION` 0.1.0 and the plugin is 0.4.0, so I read the plugin copies of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md and testing.md, plus the repo `.claude/rules/lessons.md`. `android-emulator-skill` and `android-testing` were **not** invoked — a deliberate, recorded substitution: the brief pins the existing session helpers (`launch.sh`, `pread.sh`, `mask.sh`, `tools/ui.py`, `robot.py`, `prefs.py`), no APK was built or installed, no emulator was booted or shut down, and no test was authored.
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Test 10 (CLOUD-01) PASS.** A `pm clear`-ed `Pixel_9b_clean` restored **Challenge level 11 and English-33 3/3 — neither of which it had ever held** — from `Pixel_9_clean`'s proven 17:24:42.612 commit. D-03 negatives all held.
- **Test 11 (CLOUD-02/CLOUD-04) PASS.** Full union in **both** directions on the first relaunch of each device, nothing lost on either side, no prompt, **zero conflicts**. D-04's board-preservation and no-ladder-regression halves pass exactly; the third clause ("Next level" serves the merged level) does **not** hold when the gap is > 1 — filed as a low-severity bug.
- **Test 12 (CLOUD-03) PASS.** Both devices list the **same single entry**, identical description `Challenge level 13 · 48 stars · 4/5 Classic boards`, same played time, same last-modified, same unbroken cover art. Every number cross-checks against the app.
- **Test 13 (CLOUD-05) PASS.** Offline half clean (three offline solves + an offline relaunch, no error wall/snackbar/dialog/retry prompt, 0 FATAL). Reconvergence half passes **in both directions**, through two genuine conflicts.
- **The causation evidence the investigation was missing.** Two real conflicts fired, one on each device. Both resolved. `Pixel_9_clean` settled to `change_pending = false` in ~4 min; `Pixel_9b_clean` logged `Inconsistent DB` and still uploaded afterwards. **A conflict does not wedge a device.**

## Devices and configuration

| | `emulator-5556` = Pixel_9_clean | `emulator-5558` = Pixel_9b_clean | `emulator-5554` = Pixel_9a |
|---|---|---|---|
| Product / build id | `sdk_gphone64_arm64`, BP22.250325.006 | `sdk_gphone64_arm64`, BP22.250325.006 | not touched |
| Play services core | **26.34.36** | **26.34.36** | not touched |
| `base.apk` sha256 (first 16) | `1c480a55cc614da0` | `1c480a55cc614da0` | — |
| Network | VALIDATED | VALIDATED (except the deliberate Test-13 window) | — |
| Clock vs host | equal | equal | — |

`emulator-5554` was **never addressed**: no command in this session carried `-s emulator-5554`. The only place it
appears is the output of `adb devices -l`. It was not launched, read, written or screenshotted.

All `last_modified_at` values are decoded from epoch millis and are stamped by the writing device's clock; both
clean devices are on host time, so the decoded values are directly comparable.

### Every probe read of the session

| Label (host time) | Device | `change_pending` | `conflicts_resolved` | `last_modified_at` | ladder |
|---|---|---|---|---|---|
| 00-baseline 17:21:42 | 5556 | False | 0 | 16:43:00.071 | 10 |
| 00-baseline 17:21:42 | 5558 | False | 0 | 16:43:00.071 | 10 |
| t10-06 confirm-upload 17:26:45 | 5556 | **False** | 0 | **17:24:42.612 (own)** | 11 |
| t10-12 before-signin 17:29:28 | 5558 | *no keys at all* | – | – | – |
| t10-15 after-restore 17:30:33 | 5558 | False | 0 | 17:24:42.612 (5556's) | **11** |
| t11-04 after-own-solves 17:40:43 | 5558 | False | 0 | 17:24:42.612 | 11 |
| t11-09 after-own-solves 17:44:25 | 5556 | False | 0 | 17:24:42.612 | 13 |
| t11-10 merge-r1 17:45:22 | 5556 | False | **0** | 17:44:16.447 | 13 |
| t11-11 merge-r1 17:46:08 | 5558 | False | **0** | 17:44:16.447 | **13** |
| t11-15 after-D04-solve 17:48:18 | 5558 | False | 0 | 17:44:16.447 | 13 |
| t11-16 upload-recheck 17:49:57 | 5558 | False | 0 | **17:47:40.952 (own)** | 13 |
| t11-17 upload-recheck 17:50:44 | 5556 | False | 0 | 17:47:40.952 | 13 |
| t13-04 offline-relaunch 17:54:52 | 5558 | **True** (offline) | 0 | 17:53:46.488 | 15 |
| t13-08 online-after-solve 17:56:23 | 5556 | False | 0 | 17:47:40.952 | 14 |
| t13-10 back-online-r1 17:57:38 | 5558 | True | **1** | 17:56:58.358 | 16 |
| t13-12 post-conflict-r2 17:59:06 | 5558 | True | 0 | 17:56:58.358 | 16 |
| t13-13 reconverge-r1 18:00:01 | 5556 | True | **1** | 17:59:21.744 | **16** |
| t13-14 settle-r3 18:01:58 | 5558 | True | **1** (+`Inconsistent DB`) | 18:01:33.915 | 16 |
| t13-15 settle-r2 18:03:06 | 5556 | **False** | 0 | 17:59:21.744 (own) | 16 |
| t13-17 post-inconsistentdb-r4 18:04:57 | 5558 | True | 0 | 18:01:33.915 | 16 |
| t13-19 post-conflict-upload 18:08:08 | 5558 | **False** | 0 | **18:06:09.536 (own)** | 17 |
| t13-20 reconverge-r2 18:09:02 | 5556 | **False** | 0 | 18:06:09.536 (5558's) | **17** |

---

## Test 10 — Restore on a new device (CLOUD-01) — **PASS**

**Substitution, as the brief directs:** `Pixel_9b_clean` is the receiving device. Because it had already restored
once at 17:19, I made it receive something it had **never** held rather than re-restoring what it already had.

**Step 1 — give the server new content.** On `Pixel_9_clean` I solved **English-33** (0/3 → 3/3, "Perfect!", 1 peg,
score 7720, 00:38) and **Challenge level 10** (ladder 10 → 11). Before this, both devices were byte-for-byte
identical: level 10, European-37 3/3, every other board 0/3 (`t10-02`, `t10-03`).

**Step 2 — prove the write reached the server** (the brief's gate). Relaunch at 17:26:00, read at 17:26:45:
`change_pending = False` with `last_modified_at = 17:24:42.612` — **this device's own commit**, made at the moment
of the level-10 solve. Both halves of the honest upload signal are present.

**Step 3 — clear the receiver.** `pm clear com.erman.pegsolitarie` on `emulator-5558` only. Verified immediately
after: `files/datastore/` does not exist, and a probe read at 17:29:28 returned **no `play_games*` or `challenge*`
keys at all**. Play services was *not* cleared — `dumpsys package com.google.android.gms` still reports 26.34.36
on the same install. The fresh-install flow then ran in full: UMP ads-consent dialog → three-panel tutorial →
Home's "Sign in to Play Games?" prompt.

**Step 4 — sign in and observe.** Sign-in completed at 17:30:01. Within ~10 s, with no further taps:

| | before `pm clear` | after clear, before sign-in | after sign-in |
|---|---|---|---|
| Challenge | Level 10 | *(no data)* | **Level 11** |
| English-33 | 0/3 | *(no data)* | **3/3** |
| European-37 | 3/3 | *(no data)* | 3/3 |
| Asymmetric / Diamond / Wiegleb | 0/3 | *(no data)* | 0/3 |
| `last_modified_at` | 16:43:00.071 | *(absent)* | **17:24:42.612** |

Level 11 and English-33 3/3 are **content this device never held in its life** — it was at level 10 with
English-33 at 0/3 before the clear, and had no data at all after it. This is a genuine restore of another
device's write, not a re-read of its own. `conflicts_resolved = 0`, `change_pending = False`.

**D-03 negatives — all held.** Across the sign-in launch and two cold relaunches:
- **no toast, no dialog, no snackbar, no banner** of the app's own at any point;
- **no "New best!" and no Results screen on launch** — Home is the first and only screen (`t10-13`, `t10-20-t6`);
- **no sync row in Settings.** The Play Games section is exactly three rows and nothing else: the sign-in row
  (`Signed in as <redacted>`) with the static subtitle *"Leaderboards, achievements and cloud save"*,
  `Leaderboards` / *"See how your scores rank"*, `Achievements` / *"View the achievements you've earned"*. No
  sync status, no "last synced", no cloud-save toggle, no error row, no progress indicator
  (`t10-22-5558-settings-dump.masked.txt`).

**Stock greeting — caught and located in time.** The burst at 0.5/1.0/1.5/2.0/2.5/4/6 s found only the splash
and then a clean Home; GMS logged `returning true for shouldShowWelcomePopup` at **17:30:54.369**, i.e. t ≈ 6.8 s
on this AVD — *after* the last frame of the standard burst. A second launch captured at 6.5/7.5/8.5 s shows the
chip present at 7.5 s and gone by 8.5 s. It is Google's UI, not the app's.
*Correction for the record:* the predecessor's "the chip appears at t ≈ 1.5 s and is gone by 2.5 s" is not a
constant — on this device it lands at ≈ 7 s. Anyone hunting it with a 2-second burst will miss it and may wrongly
record "no popup".

**One observation that is not a phase-13 defect but belongs in the transcript:** after `pm clear`, the first
launch shows the **UMP ads-consent dialog** ("Publisher Test Ads … Consent / Do not consent / Manage options")
before the tutorial. That is the phase-10 consent flow behaving correctly on a fresh install; it is not an
app-owned cloud-save surface and does not touch D-03.

Evidence: `qa-shots/t10-01`…`t10-07` (sending device before/after), `t10-10-*` (7-frame first-launch burst),
`t10-11` (tutorial), `t10-13`, `t10-14` (restored Home + catalog), `t10-20-*` (7-frame relaunch burst),
`t10-21-*-redacted` (stock chip, gamer tag blacked out by me), `t10-22-…masked.txt` (Settings),
`t10-06`, `t10-12`, `t10-15` prefs.

---

## Test 11 — Two-device union (CLOUD-02, CLOUD-04 / D-04) — **PASS**, D-04 sub-check **partial**

**Staging — each device given progress the other lacked, in two dimensions.**

| | `Pixel_9_clean` (5556) | `Pixel_9b_clean` (5558) |
|---|---|---|
| Classic board solved | **Asymmetric 3-3-2-2** 3/3 (8250, 00:45) | **Wiegleb-45** 3/3 (8780, 00:52) |
| Ladder | 11 → **13** (solved levels 11 and 12) | stayed **11** |
| D-04 in-progress game | none | **Challenge level 11, one move played, 3 pegs left** |

**D-04 reference state**, recorded before the merge (`t11-03`):

```
  ...  
  ...  
......o
....oo.
.......
  ...  
  ...  
pegs 3 cells 33
```

**The merge.** One relaunch each — 5556 at 17:44:37, 5558 at 17:45:23. Result on **both** devices:

| | 5556 before | 5558 before | **both after** |
|---|---|---|---|
| Challenge | 13 | 11 | **13** |
| English-33 | 3/3 | 3/3 | 3/3 |
| European-37 | 3/3 | 3/3 | 3/3 |
| Asymmetric 3-3-2-2 | **3/3** | 0/3 | **3/3** |
| Diamond-41 | 0/3 | 0/3 | 0/3 |
| Wiegleb-45 | 0/3 | **3/3** | **3/3** |

**The union, in both directions, on the first relaunch of each, with nothing lost on either side and no prompt
on either device.** 5556 gained Wiegleb-45; 5558 gained Asymmetric and two ladder levels. This is the
observation CLOUD-02 has been held for.

**It happened with zero conflicts.** `conflicts_resolved = 0` on both, and `grep -c 'Found conflict'` = 0 on both
launches. The commit path picked up the other device's copy at commit time: 5556's 17:44:16.447 commit already
contained 5558's 17:40 Wiegleb-45 push, so no clobber of the kind reconstructed in
`tech-lead-conflict-wedge.md` B.3 occurred here.

**Both devices still upload after the merge.** 5558's D-04 solve committed at 17:47:40.952 and, on the next
relaunch, `change_pending = False` against that own commit; 5556 pulled the same value one minute later.

### D-04 sub-check — two clauses pass, one fails

The UAT's expected text has three assertions. Taken one at a time:

1. **"the Continue tile still resumes *that* level and the board is exactly as it was left" — PASS.** After the
   merge, Home still carried `Continue Challenge / Pick up Level 11 where you left off` alongside
   `Continue — Level 13`. Tapping it opened **Level 11** with the board **identical** to the reference above —
   same three pegs, same cells (`t11-13`).
2. **"the ladder does not regress" — PASS.** `challenge_current_level = 13` throughout, and after solving the
   resumed level 11 it was still **13**, not 12 (`t11-15`).
3. **"'Next level' then serves the merged level" — FAIL as written.** From the Results of the resumed level 11,
   **"Next level" served Level 12, not the merged head 13.** Level 12 had already been solved on the other
   device and is in the merged results, so the player is handed an already-completed level to replay.

The previous (superseded) run could not distinguish clauses 2 and 3: its in-progress level was 8 and the merged
head was 9, so 8+1 = 9 and both readings gave the same answer. With a gap of two (11 vs 13) they separate for the
first time. Severity **low**: no data loss, no crash, the ladder is intact, and `Continue — Level 13` on Home
still reaches the head. Filed below; the wording is the game-designer's to rule on.

Evidence: `qa-shots/t11-01`…`t11-08`, `t11-12-emulator-5556/5558-classic-merge-r1` (the union, both devices),
`t11-13` (D-04 resumed board), `t11-14` (Home after), prefs `t11-04`, `t11-09`, `t11-10`, `t11-11`, `t11-15`,
`t11-16`, `t11-17`.

---

## Test 12 — Legible listing entry (CLOUD-03) — **PASS**

The Play Games app still has no Saved games screen, so the debug picker was used on each device, as the brief
directs. **Nothing was tapped: no Select, no Delete, no Refresh.** The picker was left by the system Back key.

`adb -s <serial> shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true`
(force-stop first) resolved on the first attempt on both devices.

| | `Pixel_9_clean` (5556) | `Pixel_9b_clean` (5558) |
|---|---|---|
| Entries listed | **exactly one** | **exactly one** |
| Description line | `Challenge level 13 · 48 stars · 4/5 Classic boards` | `Challenge level 13 · 48 stars · 4/5 Classic boards` |
| Played time | `1 minute` (non-zero) | `1 minute` (non-zero) |
| Last saved | `Today, 5:47 PM` | `Today, 5:47 PM` |
| Cover art | launcher-icon English-33 design, unbroken | identical |
| Screen title | `Saved games (debug)` | `Saved games (debug)` |

**Both devices list the SAME entry with the same description — that is the real CLOUD-03 evidence the brief
asked for**, and it also independently corroborates CLOUD-02: two devices reading the same server record see one
snapshot, not two, and agree on its contents down to the minute.

**Every number cross-checks against the app** at the 17:47:40.952 commit:
- `Challenge level 13` = the ladder head on both devices ✔
- `4/5 Classic boards` = English-33, European-37, Asymmetric 3-3-2-2, Wiegleb-45 at 3/3; Diamond-41 at 0/3 ✔
- `48 stars` = 4 Classic boards × 3 (=12) + Challenge levels 1–12 × 3 (=36) ✔
- `Today, 5:47 PM` = `last_modified_at` 17:47:40.952 ✔

**RESEARCH A1 — a change from the earlier measurement, and it needs re-reading.** Across this whole session
(**nine distinct commits** on two devices) the string `Snapshot cover image is too large` appears **zero** times
in either device's logcat. The superseded run recorded it on *every* commit on `Pixel_9a` (GMS 26.15.33). Both
clean devices run GMS **26.34.36**. I am reporting this as an observation, not a conclusion: I did not obtain
`getMaxCoverImageSize()` (still not reachable without a code change or a debugger), so this does not prove the
cap changed — only that the warning is no longer emitted on the GMS these devices run. The cover renders
correctly and unscaled-looking in the picker on both. Whether `COVER_PX` → 384 is still worth doing is the
tech-lead's call, but the evidence that forced it has weakened.

Evidence: `qa-shots/t12-01-5556-picker`, `qa-shots/t12-02-5558-picker`. No gamer tag is rendered on this screen;
both captures are unredacted and were checked by eye before being kept.

---

## Test 13 — Network kill degrades silently (CLOUD-05) — **PASS**

**Offline half — PASS.** `svc wifi disable && svc data disable` on `Pixel_9b_clean`; verified
`Active default network: none`. Then, offline:
- solved **Challenge level 13**, then **14** via "Next level", then relaunched the app offline, then solved **15**;
- every solve recorded locally and Results behaved exactly as online ("Perfect!", "New best!", score, time, pegs);
- Home and the Challenge tile advanced normally (ladder 13 → 16);
- **no error wall, no snackbar, no dialog, no retry prompt**, and nothing in the UI hinting a cloud exists;
- **0 FATAL EXCEPTION** lines on either device for the whole session.

The offline relaunch read is the textbook degraded state: `change_pending = True` with
`last_modified_at = 17:53:46.488` and `_last_push_stage = COMMITTED` — committed locally, not flushed. Exactly
what the SDK documents.

**Reconvergence half — PASS, in both directions, through two real conflicts.** While 5558 was offline I advanced
5556 online (level 13 solved, ladder → 14) so the server genuinely diverged under the offline device. Then:

| Time | Event |
|---|---|
| 17:56:49 | network restored on 5558 (`Active default network: 103`, 15 VALIDATED) |
| 17:56:58.145 | 5558: `Found conflict for snapshot progress` — **a genuine conflict**, the first manufacturable one in this investigation |
| 17:56:58.358 | 5558: resolution commit. `conflicts_resolved` 0 → 1. **No `Inconsistent DB` on this one** |
| 18:00:01 | **5556 reads `challenge_current_level = 16`** — it received 5558's three offline solves. The resolution reached the server |
| 17:59:21.744 | 5556: its own conflict (`conflicts_resolved` 0 → 1) and resolution commit |
| 18:03:06 | **5556: `change_pending = False`** against its own 17:59:21.744 commit — a device that resolved a conflict uploads again |
| 18:01:34.557 | 5558: `Inconsistent DB:null/4` — **the wedge signature, on a brand-new device** (detail below) |
| 18:06:09.536 | 5558: solved level 16, new commit |
| 18:08:08 | **5558: `change_pending = False`** against that own commit — **it still uploads after the signature** |
| 18:09:02 | 5556 reads level 17 and `last_modified_at = 18:06:09.536` — converged both ways |

**Final state, identical on both devices:** Challenge level 17; English-33, European-37, Asymmetric 3-3-2-2 and
Wiegleb-45 all 3/3; Diamond-41 0/3; `change_pending = False`; same `last_modified_at`.

Evidence: `qa-shots/t13-01`…`t13-07`, `t13-18`, `t13-21-*-home-final`, `t13-22-*-classic-final`;
`t13-11-5558-conflict-gms-lines.masked.txt` (4.9 KB), `t13-16-5558-inconsistentdb-gms-lines.masked.txt` (9.0 KB);
prefs `t13-04`, `t13-08`, `t13-10`, `t13-12`, `t13-13`, `t13-14`, `t13-15`, `t13-17`, `t13-19`, `t13-20`.

---

## The F-3/F-4 causation evidence (the brief's standing instruction)

The brief told me to check after **every** conflict whether the device that resolved it can still upload, and to
stop and report if one became permanently unable to. **Neither device became unable to upload.** I am reporting
it here because it is the answer the investigation has been missing, not because it blocked anything.

**Two genuine conflicts fired, one per device.** Both resolved. The outcomes:

| | `Pixel_9_clean` conflict (17:59:21) | `Pixel_9b_clean` conflicts (17:56:58 and 18:01:33) |
|---|---|---|
| `Found conflict` logged | yes (1) | yes (1 each) |
| `Inconsistent DB:null/4` | **no** | **yes**, on the 18:01:33 one |
| Resolution commit made | yes | yes |
| Content reached the server | yes (5558 pulled it) | yes (5556 read level 16 at 18:00:01) |
| `change_pending` back to `false` afterwards | **yes**, 18:03:06 | **yes**, 18:08:08, after one new solve |
| Still uploading at end of session | **yes** | **yes** |

**The `Inconsistent DB` signature reproduced on a clean device, and it is not the wedge.** Masked excerpt,
`emulator-5558`, 2026-09-20 (full stack in `t13-16-5558-inconsistentdb-gms-lines.masked.txt`):

```
18:01:33.251 I/PlayGamesServices[PgsSnapshotAgent]: Found conflict for snapshot progress
18:01:34.557 E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4
18:01:34.557 E/PlayGamesServices[ExtendedSnapshotsHelper]: java.lang.Throwable
18:01:34.557   at m26.hoi.e(:com.google.android.gms.optional_games@263436017@26.34.36 (100000-0):11)
18:01:34.557   at com.google.android.gms.games.provider.PlayGamesContentProvider.a(...)
18:01:34.557   at m26.ebd.delete(:com.google.android.gms.optional_games@263436017@26.34.36 (100000-0):47)
18:01:34.557   at android.content.ContentProvider.delete(ContentProvider.java:2024)
18:01:34.557   at android.content.ContentProviderOperation.apply(ContentProviderOperation.java:352)
18:01:34.557   at m26.ebd.applyBatch(...)  /  m26.ibl.g(...):307  /  m26.ibk.handleMessage(...):592
```

Three facts follow, and they are facts rather than inferences:

1. **It is not tied to old AVDs or to GMS 26.15.33.** It reproduced on an AVD created today, on GMS **26.34.36**,
   on a device whose uploads had been proven healthy fifteen minutes earlier. The frame now reads
   `@263436017@26.34.36` where `Pixel_9a`'s read `@261533017@26.15.33` — same code path, newer GMS.
2. **It follows `Found conflict` by ~1.3 s and lives entirely inside GMS.** `PlayGamesContentProvider.delete` →
   `applyBatch` → `Handler.handleMessage`: GMS's own conflict bookkeeping, on GMS's own looper, before the app
   has resolved anything. This is direct field support for `tech-lead-conflict-wedge.md` A.4(iv), which put the
   match at ~20% on reading alone.
3. **The device that logged it uploaded again.** At 18:06:09.536 it made a new commit and by 18:08:08
   `change_pending` was `False` against that commit; `Pixel_9_clean` pulled the same value at 18:09:02.
   **`Inconsistent DB` is a symptom, not the wedge.**

**Consequence for B (the F-3 fix).** The tech-lead's sequencing decision (`B.2`: do not ship B before E3, because
editing the conflict path destroys the attribution) has now been paid off. The app's conflict path ran twice
today on healthy devices, once with the full wedge signature, and neither device wedged. The app's conflict
handling is **exonerated a fourth time** — this time not by elimination but by direct observation of the path
running with its signature present. B is hygiene, and can be sequenced on that basis.

**A nuance that matters for how `change_pending` is read.** Between 17:57 and 18:05 `Pixel_9b_clean` reported
`change_pending = True` across three launches — yet its payload had demonstrably already reached the server
(5556 read level 16 at 18:00:01). Each launch's push guard produced a fresh local commit that had not flushed at
read time, so the flag was true about a *newer* commit than the one whose content had landed.
**`change_pending = true` therefore does not mean "this device cannot upload"** — it means "this device's most
recent local commit has not flushed yet". Anything that reads it as an upload oracle over a short window, and
any re-reading of `Pixel_9a`'s history that rests on it, should be revisited with that in mind. I did not observe
it clear without a new write, but I only gave it 3.4 minutes across two launches before solving; that window is
too short to call it either way.

---

## Outputs
All under `/Users/ermanergoz/Desktop/PegSolitarie/.planning/qa/phase-13-uat/` (git-ignored, **8.9 MB**,
`git check-ignore` → `.gitignore:45:.planning/qa/`). `QA_OUT` pointed at `qa-shots/`.

- **22 `*-prefs.txt`** (~0.5 KB each) — every probe read in the table above, masked before touching disk.
- `t13-11-5558-conflict-gms-lines.masked.txt` (4.9 KB) — every `PlayGamesServices` line of the first conflict
  (17:56:58): `Found conflict`, resolution, **no** `Inconsistent DB`.
- `t13-16-5558-inconsistentdb-gms-lines.masked.txt` (9.0 KB) — the 18:01:33 conflict with the full
  `Inconsistent DB:null/4` stack on GMS 26.34.36. The load-bearing artifact of this session.
- `t10-22-5558-settings-dump.masked.txt` (1.8 KB) — the Settings hierarchy, sign-in row redacted; the D-03
  "no sync row" evidence.
- `qa-shots/` (8.8 MB, **50 full-size + 50 downscaled**):
  - Test 10: `t10-01`/`t10-02` (sender before), `t10-03` (receiver before), `t10-04`/`t10-05` (the two solves),
    `t10-07` (sender after), `t10-10-t{0.5…6}` (7-frame first-launch burst after clear), `t10-11` (tutorial),
    `t10-13`/`t10-14` (**restored Home at level 11 and catalog with English-33 3/3**),
    `t10-20-t{0.5…6}` (7-frame relaunch burst — D-03 negatives),
    `t10-21-*-redacted` ×3 (stock greeting at 6.5/7.5/8.5 s, gamer tag blacked out by me).
  - Test 11: `t11-01` (Wiegleb-45), `t11-02`/`t11-03` (D-04 board left in progress), `t11-05`…`t11-08`
    (Asymmetric, levels 11–12, the interstitial), `t11-12-emulator-5556/5558-classic-merge-r1`
    (**the union on both devices**), `t11-13` (**D-04 resumed board, identical**), `t11-14`.
  - Test 12: `t12-01-5556-picker`, `t12-02-5558-picker` — **the same single entry on both devices**.
  - Test 13: `t13-01` (offline Home), `t13-02`/`t13-03`/`t13-06` (three offline solves), `t13-05` (offline
    relaunch), `t13-07` (online solve on the other device), `t13-18` (post-conflict solve),
    `t13-21-*`/`t13-22-*` (final converged state, both devices).
- `launch.sh`, `pread.sh`, `mask.sh`, `burst.sh` — session helpers. The first three are copies of the
  `phase-13-e2` originals with the output directory changed (and `launch.sh`'s scratch filenames prefixed `u-`
  so my temporary files are distinguishable from other agents'). `burst.sh` is new: a timed launch-and-screencap
  ladder. `pread.sh` keeps the predecessor's narrowed mask so the 13-digit epoch millis survive.
- `.planning/reports/phase-13/qa-engineer-uat-final.md` — this report (uncommitted).

## Checks
- `adb devices -l` — pass (`emulator-5554`, `-5556`, `-5558` all `device`; no physical device attached, so the
  pre-release physical-device smoke does not apply today).
- `getprop ro.product.name` / `ro.build.id` / `date` on 5556 and 5558 — pass (`sdk_gphone64_arm64`,
  BP22.250325.006, clocks equal to host on both).
- `dumpsys package com.google.android.gms | grep versionName` on 5556 and 5558 — pass (**26.34.36 on both**).
- `pm path` + `sha256sum base.apk` on 5556 and 5558 — pass (`1c480a55cc614da0` on both; the brief's build).
- `dumpsys connectivity | grep -c VALIDATED` — pass (15 on both at start; 9 and `Active default network: none`
  on 5558 during the Test-13 offline window; 15 again after restore).
- `pread.sh <serial> 00-baseline` on both, before touching anything — pass (both `change_pending=False`,
  `last_modified_at=16:43:00.071`, level 10 — exactly the state the brief describes).
- `robot.py emulator-5556` on English-33 — pass (32 pegs/33 cells, 31 moves, "Perfect!", 7720, 1 peg, 00:38).
- `robot.py emulator-5558` on Wiegleb-45 — pass (44 pegs/45 cells, 43 moves solved in 304 s, 8780, 1 peg).
- `robot.py emulator-5556` on Asymmetric 3-3-2-2 — pass (38 pegs/39 cells, 37 moves, 8250, 1 peg).
- `robot.py <serial>` on Challenge levels 10–16 — pass (nine level solves across the two devices, all "Perfect!").
- `robot.py emulator-5558 --leave 2` then `--read-only` — pass (D-04 reference state captured; note that
  `--leave 3` on a 3-move level plays nothing, so `--leave 2` was used to get a genuinely mid-game board).
- `pm clear com.erman.pegsolitarie` on emulator-5558 — pass (`Success`; `files/datastore/` gone; probe returned
  zero keys). **Play services was not cleared** — verified still 26.34.36 on the same install immediately after.
- `launch.sh` ×16 across the two devices (force-stop, `am start`, 45 s, masked `prefs.py`, GMS log counts) —
  pass; the 22-row table above is its output.
- `burst.sh` ×2 (7 frames at 0.5–6 s) plus one 6.5/7.5/8.5 s ladder — pass (stock chip located at t ≈ 7 s).
- `am start … --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true` on both — pass, resolved first try on
  both after a force-stop. **No Select, Delete or Refresh was tapped**; exited with the system Back key.
- `grep -c 'Found conflict' / 'Inconsistent DB' / 'cover image is too large' / 'FATAL EXCEPTION'` per launch and
  over the full session — pass and material: session totals 5556 = 1/0/0/0, 5558 = 2/1/0/0.
- `ui.py <serial> dump` on Home, Classic, Results, Settings, the game screen and the picker, both devices — pass.
- `grep -rnEi '@gmail|@…\.(com|net|org)|[ag]_[0-9]{10,}|ELLIDED:[0-9]|Signed in as [A-Za-z0-9<]|Hey there'`
  over every `.txt` and `.sh` artifact — pass (one hit, and it is the literal string `Signed in as <redacted>`).
- `grep -rhoE '[0-9]{11,}'` over every artifact — pass (nine hits, all of them the epoch-millis
  `last_modified_at` values under test; no project number, no player id).
- `grep -rh player_id` over every artifact — pass (one distinct line, `= <masked>`).
- Screenshots reviewed by eye before keeping — pass (no Settings capture was ever taken; the three greeting
  captures were redacted in memory and written **already black-barred**, and the unredacted originals were
  deleted from the scratchpad in the same command).
- `git check-ignore -v .planning/qa/phase-13-uat/x.txt` — pass (`.gitignore:45:.planning/qa/`).
- `git status --short` / `git rev-parse --abbrev-ref HEAD` — pass (clean; `feature/phase-13-cloud-save`;
  head `bc93869`; no branch switch, no `git add`, no commit, no app source, Gradle file or test edited).
- `./gradlew check` — **not run**, forbidden by the brief ("do NOT run Gradle"). No code changed in this session,
  so nothing here alters the tech-lead's standing conditional sign-off, which remains conditional on the
  producer's full run.
- `emulator-5554` — **not touched**: no command in the session carried `-s emulator-5554`.
- Play services clear, account change, Play Console, any install, any emulator boot or shutdown, picker
  Select/Delete/Refresh, editing `11-HUMAN-UAT.md` — **none performed**.

## Open issues / risks

- **BUG (low, new) — "Next level" from a resumed older level serves level+1, not the merged ladder head.**
  - Reproduce: leave a Challenge game in progress at level N; merge in progress from another device that has
    reached N+2 or beyond; resume and solve level N; tap "Next level".
  - Expected (11-HUMAN-UAT test 11): "Next level" serves the merged level.
  - Actual: it serves level N+1 (here 12), which the merged progress already records as solved 3/3, so the
    player replays a completed level. The ladder itself does **not** regress (`challenge_current_level = 13`
    throughout) and `Continue — Level 13` on Home still reaches the head.
  - Evidence: `qa-shots/t11-13`, `t11-14`; prefs `t11-15-5558-after-D04-solve-emulator-5558-prefs.txt`.
  - Why it was never seen before: the superseded run's gap was exactly 1 (in-progress 8, merged 9), where
    N+1 and "the merged level" are the same number. A gap of 2 separates them.
  - Routing: the expected wording is the **game-designer**'s to rule on before it becomes a code change — it may
    be that N+1 is the intended behaviour and the UAT sentence is loose.
- **The `change_pending` oracle is weaker than the investigation has been treating it.** See the nuance at the
  end of the causation section: a device reported `change_pending = true` for eight minutes across three
  launches while its content had already reached the other device. The honest upload signal is
  `change_pending = false` **with `last_modified_at` equal to this device's own last commit** — the predecessor
  said this and it should be treated as binding, because the flag alone is now known to lag.
- **RESEARCH A1's evidence has weakened and the note should be updated, not just carried forward.** Zero
  `cover image is too large` lines across nine commits on GMS 26.34.36, against "every commit" on GMS 26.15.33.
  I did not measure `getMaxCoverImageSize()`, so this is not proof the cap changed. Whoever owns the
  `COVER_PX` → 384 decision should re-read it on a 26.34.x device before spending the change.
- **The stock greeting's timing is device-dependent (t ≈ 1.5 s on one AVD, t ≈ 7 s on another).** Any future
  capture plan should key off `shouldShowWelcomePopup` in logcat rather than a fixed burst window, or it will
  record a false negative.
- **Diamond-41 was deliberately not solved** and stays 0/3 on both devices. It is a fewest-pegs board and is not
  one-peg solvable, so `robot.py` (which solves to a single peg) cannot clear it. This is a limitation of the
  harness, not a product finding, and it is why "4/5 Classic boards" is the ceiling this session could reach.
- **The two conflicts were manufactured by the offline/online split, which is now a repeatable recipe:** take one
  device offline, solve on it, write from the other device, restore the network. That is the missing instrument
  from the predecessor's step 3 ("not manufacturable with one uploading device") and it should be written into
  `docs/qa-plan.md` as the standard conflict fixture.
- **Device state changed — `Pixel_9_clean` (emulator-5556).** English-33, Asymmetric 3-3-2-2 solved 3/3;
  Challenge levels 10–13 solved; ladder now 17. Nine force-stop/relaunch cycles. One conflict resolved. One
  interstitial test ad viewed and dismissed. Left on Home.
- **Device state changed — `Pixel_9b_clean` (emulator-5558).** **App data cleared once** (deliberate, Test 10),
  then re-consented to UMP, re-ran the tutorial and signed in again. Wiegleb-45 solved 3/3; Challenge levels 11,
  13, 14, 15, 16 solved; ladder now 17. Network disabled and re-enabled once. Two conflicts resolved, one with
  `Inconsistent DB`. Eleven force-stop/relaunch cycles. Left on Home.
- **`emulator-5554` (Pixel_9a) is untouched** and remains the wedged control for whatever forensics the tech-lead
  wants next.
- **Privacy — no incident this session, and one deliberate change of method.** The gamer tag, the account email
  and the player id appear in no artifact and nowhere in this report. Unlike the previous session I never took
  an unredacted capture of an identity-bearing surface: the greeting frames were black-barred **in memory before
  being written**, their originals were deleted in the same command, and I never screenshotted Settings at all —
  its evidence is a hierarchy dump with the sign-in row substituted to `<redacted>` by a filter in the same
  pipeline, so the tag never reached a file or my own output. Dumps of Home, Classic, Results and the picker
  carry no identity. The picker renders `Game: Peg Solitaire` and no player name.
- **Scratchpad hygiene.** Every temporary file I created is prefixed `u-` (`u-raw-*`, `u-<label>-*.raw.txt`) and
  the three unredacted greeting PNGs were deleted. Two background `adb logcat` processes were left running for
  the session; the producer may kill them. Pre-existing files from other agents were not touched.

## Handoff

**VERDICT: PASS** — all four phase-13 UAT tests (10, 11, 12, 13) executed as written on the two clean devices,
with a hands-on session and screenshots for every UI-affecting step. One low-severity bug found ("Next level"
after a merged resume), no high-severity bug, no crash, no data loss in either direction, and no device left
unable to upload. CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04 and CLOUD-05 all now have field evidence on two devices.

Next: **tech-lead**.
1. **CLOUD-02's HOLD can be lifted.** The union was observed in both directions, on the first relaunch of each
   device, with nothing lost and no prompt — and independently corroborated by both devices listing one identical
   snapshot entry in Test 12.
2. **F-3/F-4: the causation test finally ran, and it acquits the app.** Two real conflicts, one with the full
   `Inconsistent DB:null/4` stack on a brand-new device running GMS 26.34.36, and **both devices uploaded
   afterwards**. Your A.4(iv) reading (the signature is GMS-internal bookkeeping, ~20% match to the app's path)
   is now supported by field evidence rather than by elimination. B is hygiene; sequence it as such.
3. **Re-read `Pixel_9a`'s wedge with the weakened `change_pending` oracle** (Open issues, item 2). A stale
   `change_pending = true` is now known to persist for minutes on a device whose data *has* landed.
4. **RESEARCH A1 needs a fresh reading** before `COVER_PX` → 384 is spent: the warning that motivated it does not
   appear at all on GMS 26.34.36.
5. `docs/tech-design.md` still does not exist — this is now the fourth report to say so.

For the **producer**:
- The four result lines are ready to transcribe into `.planning/phases/11-play-games-session/11-HUMAN-UAT.md`
  tests 10–13. I did not edit that file.
- The "Next level" bug should be routed to the **game-designer** for a wording ruling before it becomes a code
  change.
- `.planning/qa/phase-13-uat/` (8.9 MB) is this task's artifact directory to delete after the STATUS.md Done
  line; `.planning/qa/phase-13-conflict/` and `.planning/qa/phase-13-e2/` from the earlier sessions are still on
  disk and count against the 200 MB budget.
- Two background `adb logcat` processes are still attached to 5556 and 5558.
