# qa-engineer report — phase 13 device pass re-run (fix verification + UAT 10-13)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (28d1821..28d1821 — nothing committed by QA)
Skills loaded: ergoz-soft:standards (role qa-engineer). Rules read from plugin copy 0.1.1 because the repo snapshot is 0.1.0: studio.md, reporting.md, testing.md, gitflow.md, skills-policy.md, logging.md, memory.md; `.claude/rules/lessons.md`. android-emulator-skill / android-testing were not invoked: the brief named `tools/ui.py`, `robot.py`, `prefs.py`, `levels.sh`, and no test was written.
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; no `.planning/recall/RECALL.md`)

> Saved by the producer from the qa-engineer's final message: the agent's harness did not let it
> write the report file itself. Content is the agent's, unedited apart from this note.

## Summary
The `getConflict` fix works: both devices now record stage `COMMITTED` with pending `False`. Cross-device convergence does not work reliably. Pixel_9a's unique progress never reached Pixel_9. Pixel_9's progress reached Pixel_9a once, through a GMS-reported conflict, and then stopped arriving. The app reports `COMMITTED` throughout, so its own bookkeeping cannot see the failure. New finding: GMS logs the cover cap, so RESEARCH A1's accounting assumption is wrong.

## Build under test
`com.erman.pegsolitarie` 3.0.0 debug at commit 691015f. The device `base.apk` sha256 prefix is `795919ce5d52290f` on both; installed 02:55:40 (Pixel_9a) and 02:55:50 (Pixel_9).

## Part 1 — fix verification: PASS on both
30 s after one signed-in, online Challenge solve:

| | Pixel_9a `emulator-5554` | Pixel_9 `emulator-5556` |
|---|---|---|
| Level solved | 7 | 6 |
| `play_games_cloud_last_push_stage` | `COMMITTED` | `COMMITTED` |
| `play_games_cloud_push_pending` | `False` | `False` |
| `play_games_cloud_last_push_status` | absent | absent |
| `play_games_submissions_pending` | `False` | `False` |

Both devices had already flipped from `FAILED_OTHER` / pending `True` to `COMMITTED` / `False` about 35 s after the first launch of the new build, before any solve. No status code was recorded, so no lookup was needed. Limit: DataStore skips a write whose values are unchanged, so the post-solve read cannot prove the solve-time push re-wrote the key. The `FAILED_OTHER` → `COMMITTED` change on first launch of the new build is the proof. Evidence: `01-part1-prefs-emulator-5554.txt`, `01-part1-prefs-emulator-5556.txt`.

## Test 10 — Restore on a new device (CLOUD-01)
result: **fail** · 2026-09-20 · Pixel_9a + Pixel_9 · build 691015f

- **Pixel_9a before the wipe:** English-33 re-solved by the robot (31 moves, 3/3, "New best!"), Challenge at level 8, stage `COMMITTED`, force-stopped at 03:01:13. Levels 1–7 had been solved in earlier sessions.
- **Pixel_9 setup:** `pm clear`, launch, consent "Do not consent", tutorial "Got it", then "Sign in to Play Games?" → Sign in at 03:01:59. GMS logged `signIn(): Returning SUCCESS`.
- **Restore timing:** 20 frames were captured, at 0.2–1.7 s and again at 7–17 s. Home read "Continue — Level 1" at 0.5 s and "Continue — Level 7" at 1.0 s.
- **What was restored:** Level 7, English-33 0 of 3, all Classic boards 0 of 3. That is Pixel_9's own pre-wipe progress. Pixel_9a's level 8 and English-33 3/3 were missing. State was unchanged after relaunches at 03:04, 03:06, 03:07 and 03:14.
- **D-03 negatives held:** no toast, dialog, snackbar or Settings sync row; no "New best!" or Results on launch.
- **"Welcome back" popup:** not in any captured frame. GMS logged `returning true for shouldShowWelcomePopup`. The 1.7–7 s window was not captured because a UI dump ran then, so its timing is not assessed.

evidence: `10-p9a-classic-before-s.png`, `11-p9a-english33-result-s.png`, `12-p9a-home-before-forcestop-s.png`, `20-p9-signin-frame01/03/07/12/13/20-s.png`, `21-p9-home-after-signin-s.png`, `22-p9-classic-after-signin-s.png`, `71-p9-gms-snapshot-lines.masked.txt`

## Test 11 — Two-device union (CLOUD-02, CLOUD-04 / D-04)
result: **fail** (D-04 sub-check pass) · 2026-09-20 · Pixel_9a + Pixel_9 · build 691015f

**Adaptation from the script.** The ladders were already past the scripted levels. Pixel_9a was staged with English-33 3/3 and level 8, leaving level 8 in progress with `robot.py --leave 2` (one move played, 3 pegs at (4,2), (5,3), (5,4)). On Pixel_9: European-37 solved (35 moves, 3/3) and Challenge levels 7 and 8, reaching level 9. Expected union: level 9, English-33 3/3, European-37 cleared on both.

- **Pixel_9a after relaunch (03:12):** GMS logged `Found conflict for snapshot progress` twice in one launch. Home showed "Continue — Level 9", English-33 3/3, European-37 3/3. The union arrived.
- **Pixel_9 after relaunches at 03:12 and 03:14:** level 9, European-37 3/3, English-33 still 0 of 3. No conflict was ever logged on Pixel_9, and Pixel_9a's progress never arrived.
- **No prompt** appeared on either device.
- **D-04 PASS:** after the merge Pixel_9a showed "Continue Challenge — Pick up Level 8 where you left off" next to "Continue — Level 9". Resume opened level 8 with exactly the 3 pegs as left. Finishing it showed Results without "New best!" (correct: merged best 5280 against 5240 for this solve). After a test interstitial, "Next level" served level 9.
- **GMS noise on Pixel_9a:** on each conflict it logs `E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4` with a stack inside `PlayGamesContentProvider.delete`. Pixel_9 never logs it.

evidence: `30-p9-european37-start-s.png`, `31-p9-european37-result-s.png`, `32-p9-L8-result-s.png`, `33-p9a-L8-inprogress-left-s.png`, `40-emulator-5554-home-after-merge-s.png`, `40-emulator-5556-home-after-merge-s.png`, `41-emulator-5554-classic-after-merge-s.png`, `41-emulator-5556-classic-after-merge-s.png`, `42-p9-classic-after-2nd-relaunch-s.png`, `45-p9a-L8-resumed-after-merge-s.png`, `47-p9a-level-served-after-L8-s.png`, `70-p9a-gms-snapshot-lines.masked.txt`

Log excerpt, Pixel_9a, masked:
```
03:12:24.936 I/PlayGamesServices[PgsSnapshotAgent]: Found conflict for snapshot progress
03:12:26.321 E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4
03:12:37.097 I/PlayGamesServices[PgsSnapshotAgent]: Found conflict for snapshot progress
03:12:37.611 E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4
03:12:37.621 W/PlayGamesServices[GamesService]: Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled
```

## Test 12 — Legible listing entry (CLOUD-03)
result: **blocked** · 2026-09-20 · Pixel_9a · build 691015f

- **No Saved games UI.** Play Games app 2025.09.66390 → Peg Solitaire shows Play, Achievements (10 / 20), Leaderboards, one achievement recommendation, and an overflow menu with Hide game / Uninstall / Settings / Help & feedback. There is no Saved games section, although commits now succeed. Settings has only "Delete Play Games account & data", which was not opened.
- **Picker intent:** `am start -a com.google.android.gms.games.SHOW_SELECT_SNAPSHOT` from the shell does not resolve, and the app does not expose the picker. Description line, played time and cover were therefore not read.
- **RESEARCH A1 — partly measured, no code change needed.** On every commit, on both devices, GMS logs `Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled`. The cap on these devices is 819,200 bytes. The 512×512 cover is accounted at its raw 4-byte-per-pixel size (1,048,576), not its PNG size: A1's assumption is wrong. GMS scales the image instead of failing the commit, so this is not the sync failure. For the developer to confirm: the documented remedy `COVER_PX` → 384 gives 589,824 bytes, which fits. PNG size of the committed cover: not measured (not obtainable without code or a debugger).

evidence: `50-p9a-playgames-detail-s.png`, `51-p9a-playgames-detail-bottom-s.png`, `52-p9a-playgames-overflow-s.png`

## Test 13 — Network kill degrades silently (CLOUD-05)
result: **partial** · 2026-09-20 · Pixel_9, re-check on Pixel_9a · build 691015f

- **Offline half PASS.** `svc wifi disable; svc data disable` gave `Active default network: none`. Offline solves: levels 9 and 10, a force-stop and relaunch (Home "Continue — Level 11"), then level 11 (Home level 12). Results were identical to online; the Classic catalog was normal. No error wall, snackbar, dialog or retry prompt, and 0 FATAL lines in logcat. Prefs offline: `play_games_submissions_pending = True`; cloud push `COMMITTED`, pending `False` — GMS caches the commit locally, and the store's KDoc calls that intended.
- **Reconvergence half FAIL.** Network restored at 03:18 (ping to googleapis OK). Pixel_9 relaunched; submissions drained to `False`. Pixel_9a was relaunched at 03:19, 03:20, 03:22 and 03:24, after a further Pixel_9 push at 03:23. It stayed at "Continue — Level 9" every time.

evidence: `60-p9-offline-L9-result-s.png`, `61-p9-offline-L10-result-s.png`, `62-p9-offline-relaunch-home-s.png`, `63-p9-offline-L11-result-s.png`, `64-p9-offline-classic-s.png`, `65-p9-online-relaunch-home-s.png`, `66-p9a-after-p9-offline-solves-s.png`, `90-final-prefs-emulator-5554.txt`, `90-final-prefs-emulator-5556.txt`

## Outputs
- `.planning/qa/phase-13-rerun/` — 32 downsized screenshots, 2 masked log excerpts, 4 prefs dumps (2.0 MB, git-ignored). Raw logcats and full-size frames were deleted.
- `.planning/reports/phase-13/tools/ui.py` — `shot` now honours a `QA_OUT` environment variable for its output directory and still defaults to `evidence/`. The only modified tracked file.
- `robot.py` is unchanged. European-37 reads correctly (36 pegs, 37 cells); the DFS takes about 19 s and the solve is 35 moves.

## Checks
- `adb devices` — pass (both `device`; host load 2.7–6.5; no emulator incident; Gradle not run)
- `adb -s <serial> shell sha256sum <base.apk>` — pass (`795919ce5d52290f…` on both)
- `python3 tools/prefs.py <serial>` 30 s after a solve — pass on both (`COMMITTED`, pending `False`)
- `python3 tools/robot.py emulator-5556` on European-37 — pass
- `robot.py emulator-5554 --leave 2`, then resume after the merge — pass (D-04)
- UI dumps of Home and Classic on both devices after relaunches — fail for union (Pixel_9 English-33 0/3; Pixel_9a stuck at level 9 against Pixel_9's 12)
- `adb logcat -d | grep -c "FATAL EXCEPTION"` while offline — pass (0)
- grep for email / player-id / URL / gamer-tag patterns over the text artifacts — pass (0 hits)
- Status-code lookup — not run (no status recorded)
- `./gradlew check` — not run (forbidden by the brief; no code changed)
- Physical device — not run (none attached)

## Open issues / risks
- **BUG (high) — cross-device sync does not converge although every push reports `COMMITTED`.** Steps: tests 10, 11 and 13. Expected: both devices hold the union after a relaunch. Actual: Pixel_9a's progress never reached Pixel_9; Pixel_9's progress reached Pixel_9a once (03:04–03:12, via a conflict) and then stopped (levels 10–12). Cause not determined: the server is not visible, the app logs nothing, and GMS logs only the lines quoted above. Candidates: (a) Pixel_9a's GMS state — it alone logs `Inconsistent DB`; its disk was rolled back in the first pass, and the old build leaked a snapshot handle on every push; (b) GMS upload lag after offline commits; (c) an app defect in the load → resolve → discard → re-open → commit sequence. A clean third device would separate these.
- **Observability gap.** `COMMITTED` means "GMS accepted the commit locally", not "the server has it". Pending `False` is therefore not evidence of convergence.
- **Behaviour to confirm with tech-lead.** Every launch pushes once a payload exists. The ProgressSubmitter KDoc says this is intended. With GMS scaling a 1 MB bitmap on each push, it may be worth a look.
- **A1 is wrong.** See test 12; one-constant remedy.
- **Test 12 cannot be run as written** on the current Play Games app.
- **Device state changed — Pixel_9a:** English-33 re-solved; levels 7 and 8 solved (8 first left in progress, then finished); a 0-move level 9 game left open, ladder at 9; one test interstitial closed; Play Games app opened read-only and its Settings screen viewed; app force-stopped and relaunched about 8 times.
- **Device state changed — Pixel_9:** app data wiped with `pm clear` and signed in again; consent "Do not consent"; European-37 and Challenge levels 6–11 solved, ladder at 12; wifi and mobile data disabled then re-enabled; app force-stopped and relaunched about 8 times.
- No account, GMS-data or Play Console change. Both emulators are left running and online.
- **Privacy.** The gamer tag and the per-app player id appeared in tool output only, not in any file. The Play Games captures 50–52 show only the public developer name; the Settings screen with the tag and email was not captured.

## Handoff
VERDICT: FAIL (fix verified; CLOUD-01/02 and the CLOUD-05 reconvergence fail on device)

Next: tech-lead — triage the non-convergence, and decide whether a clean third device is needed before routing to android-game-dev. Also route the A1 cover-size finding. Producer — transcribe the results into `11-HUMAN-UAT.md` (10 fail, 11 fail with D-04 pass, 12 blocked, 13 partial), and raise the two founder items.
