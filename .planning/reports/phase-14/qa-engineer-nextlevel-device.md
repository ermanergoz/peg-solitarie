# qa-engineer report — phase 14 device verification
Date: 2026-09-21
Branch: feature/phase-14-prerelease-fixes (4561e67..100e69d; HEAD `100e69d`)
Build under test: debug APK from `100e69d`, sha256 `5ddb5ddfb0afdd37…`, 20,637,687 bytes
Devices: AVD `Pixel_9_clean` (emulator-5554, "A") and AVD `Pixel_9b_clean` (emulator-5556, "B"), both signed in to the one tester account, both booted `-no-snapshot-load -no-snapshot-save`. `Pixel_9a` was never booted, touched or reset.
Skills loaded: ergoz-soft:standards (role qa-engineer) — plugin copies of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `testing.md`, `puzzle-design.md`, plus the repo snapshot `.claude/rules/lessons.md` (snapshot v0.5.1 is older than plugin v0.5.2, so the plugin copies were read). `android-emulator-skill` scripts were not used; the repo's own `.planning/qa/tools/` harness covers this app's screens and was used and extended instead.
Ran in parallel with: the producer's `./gradlew check` (finished before I built)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Item 1 PASS at a genuine merge gap of 5.** A held an in-progress Challenge level 17 while B's ladder ran to 22; after the merge A's head read 22, the resumed board was **pixel-identical** to the board as left, and finishing it served **level 22** — the merged ladder head — not 18. The ladder never regressed on either device (A 17→22→25→26, B 17→21→22→25→26).
- The ruling's §E "Retry" case could **not** be exercised as written: a stuck Challenge board never reaches Results (`GameViewModel` deliberately emits no finish on Stuck), so the Results "Retry" branch is unreachable from gameplay. I ran the device-reachable equivalent — re-entering a behind-head level via the in-game Restart, at a gap of 3 — and "Next level" served the head (25). Recorded as a gap, not a FAIL.
- **Item 2 PASS.** The picker opens and lists one legible entry; this is also the standing CLOUD-03 evidence, re-measured on this build.
- D-03 held throughout: no app toast, dialog, snackbar or Settings sync row at any point — including a 20 s screen recording of a merge launch, whose only overlay was Google's stock greeting chip. "Next level" carries no number (`text='Next level'` at every one of the 12 Results screens seen).
- Two environment faults, neither an app defect: emulator DNS broke mid-session (GMS auth `NETWORK_ERROR`, fixed by rebooting both AVDs with `-dns-server`), and a `Pixel_9` emulator I did not start appeared at 01:00:06.

## Item 1 — D-04a "Next level" after a merged resume — **PASS** (merge gap achieved: **5**)

Staging (round 1):

| Step | Device | Observation |
|---|---|---|
| Baseline | A, B | `challenge_current_level = 17` on both, signed in, `push_stage = COMMITTED` |
| Leave in progress | A | Level 17, one move played, elapsed 00:20, board `.o.oo..` (3 pegs); Home reads "Continue Challenge — Pick up Level 17 where you left off"; app force-stopped |
| Advance | B | Solved 17, 18, 19, 20, 21 → `challenge_current_level = 21`, then 22 |
| Server confirmed | B | `pull_change_pending = False`, `pull_last_modified_at = 1789942017960` — B's head-22 commit is on the server |
| **Merge** | A | First launch: `challenge_current_level` 17 → **22**, `conflicts_resolved = 0`, no prompt |

Post-merge observations on A:
- Home: `Continue Challenge / Pick up Level 17 where you left off` **and** `Challenge / Continue — Level 22` — the in-progress game kept, the ladder jumped underneath it (D-04). Screenshot `05-A-home-after-merge-s.png`.
- Resume: header `Level 17`, board read `.o.oo..` (3 pegs) — and a pixel diff of the full-resolution screenshots gives **0 differing pixels out of 2,052,000** over the board region; the only difference is 726 px in the top bar, the elapsed-time digits. D-04 board fidelity is exact, not approximate.
- Finish (2 moves): Results shows `Perfect!`, SCORE 5090, TIME 00:21, PEGS LEFT 1, and **no "New best!"** — correct, the level was already at its best from B; a merge is not a play.
- Action label: `text='Next level'`, **no number**.
- **Tap "Next level" → an interstitial (D-06, ad-eligible) → Close → `Level 22`.** Not 18. `09-A-next-level-served-s.png`.
- `challenge_current_level` stayed 22 across the whole sequence.

### §E — "Retry" on the Results screen of a level behind the head — **adapted, PASS at gap 3**

The case as written is **not reachable on a device**. `GameViewModel.freezeAndFinishIfTerminal` returns before emitting for `GameStatus.Stuck` ("Only Won emits the one-shot finish … Stuck is rescuable via Undo (D-06) and deliberately emits nothing"), so a stuck board shows the in-game rescue bar `No moves left — undo or restart` (Undo / Restart / Home) and never navigates to Results. The `ResultsActions` Retry branch is therefore defensive UI. I verified this on the device by driving level 22 into a dead end on purpose (new tool `deadend.py`) — screenshot `14-A-L22-stuck-results-s.png` shows the rescue bar, not Results.

The equivalent journey the ruling is actually about — *re-entering the same behind-head level and solving it* — was run at a gap of 3:

| Step | Observation |
|---|---|
| A leaves level 22 in progress, B solves 22, 23, 24 → head 25, commit confirmed on the server (`pull_change_pending = False`, `pull_last_modified_at = 1789942404968`) | |
| A relaunches | `challenge_current_level` 22 → **25**; Home: "Pick up Level 22 where you left off" + "Continue — Level 25" |
| Resume 22, force a dead end, tap **Restart** | Level 22 reloads fresh (4 pegs, 00:00) — the same deterministic board, i.e. the Retry semantics |
| Solve it | Results: `Perfect!`, SCORE 10560, PEGS LEFT 1, no "New best!", `text='Next level'` |
| Tap "Next level" | **`Level 25`** — the head |

### D-03 negatives (checked at every step)
- Every `uiautomator` dump after each merge launch, each resume, each Results and each Home contained **no** toast, dialog, snackbar or banner.
- Settings (inspected by dump only, never screenshotted): General (Sound, Haptics) · Play Games (`Signed in as <gamer tag>` / "Leaderboards, achievements and cloud save", Leaderboards, Achievements) · Privacy (Privacy policy, Manage privacy choices). **No "last synced" / sync-status row.**
- A third merge (head 25 → 26) was run purely to record the launch: `screenrecord --time-limit 20` + `ffmpeg fps=2` → 40 frames, each diffed against the settled Home frame. The only overlay in any frame is Google's stock "Welcome back" greeting chip (allowed — it is the stock signal D-03 names); the bottom third of the peak-difference frames shows the Challenge tile and nothing else. The recording and frames carried the gamer tag and were **deleted from disk immediately after inspection**, along with `/sdcard/merge.mp4` on the device.

### At-head behaviour unchanged
B solved 17→24 at its own head eight consecutive times; "Next level" served N+1 every time (18, 19, 20, 21, 22, 23, 24, 25). The `max(head, N+1)` rule degenerates correctly.

## Item 2 — Debug snapshot picker opens from the Activity's own scope (L-2) — **PASS**

`adb -s emulator-5554 shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true` (after a force-stop) opened Google's own screen within ~10 s, twice, from a cold start. Nothing was tapped except the system Back button.

- Title: **`Saved games (debug)`** (our `PICKER_TITLE`), resources `com.google.android.gms.optional_games:id/…` — Google's UI, not ours.
- **Exactly one entry.** Description line: **`Challenge level 25 · 84 stars · 4/5 Classic boards`** — matching the app's ladder head (25) and Classic progress (4/5) at that moment.
- **Played time: `1 minute`** (`…:id/duration`). Save time: `Today, 1:17 AM`.
- **Cover art present and unbroken**: the launcher-icon English-33 board design (pink pegs on the purple cross), filling the card image. `18-A-snapshot-picker-s.png`.
- **Viewer-only confirmed on screen**: the only controls are `Select`, `Refresh` and `Navigate up` — no Add button and no Delete, matching `allowAddButton = false, allowDelete = false`.
- Back returned to Home with the app intact; 0 FATAL in logcat; relaunching without the extra does not open it.
- The `onNewIntent` call site (the second one L-2 changed) also works, but needs `-f 0x30000000` (`NEW_TASK|SINGLE_TOP`) — see Open issues.

Note on what this does and does not prove: it proves the picker still launches after the scope change, from both `onCreate` and `onNewIntent`. It does not directly observe that the wait now dies with the Activity, because the session resolves in about a second on a warm, signed-in device, leaving no race window to exploit; that binding is covered statically by the auditor and by `DebugSnapshotPickerEntryTest`.

## Outputs
- `.planning/qa/phase-14-nextlevel/qa-shots/` (git-ignored, 4.0 MB, 27 screenshots). Key evidence:
  - `01-A-L17-inprogress-as-left-full.png` (89,002 B) — the board as left, full resolution
  - `05-A-home-after-merge-s.png` (55,776 B) — Home after the gap-5 merge: Continue Level 17 + head Level 22
  - `06-A-L17-resumed-full.png` (88,772 B) — the resumed board, pixel-identical to `01-…-full.png`
  - `07-A-L17-results-behind-head-s.png` (44,501 B) — Results of the behind-head solve; `Next level`, no number, no "New best!"
  - `09-A-next-level-served-s.png` (45,818 B) — **Level 22 served**
  - `14-A-L22-stuck-results-s.png` (52,119 B) — the stuck board's rescue bar (no Results, no Retry)
  - `16/17-A-…-s.png` (45,012 / 46,010 B) — the restart-and-solve at gap 3 → **Level 25 served**
  - `18-A-snapshot-picker-s.png` (84,150 B) — the CLOUD-03 picker entry
- `.planning/qa/tools/deadend.py` (new, 1.9 KB) — plays the shortest move sequence into a stuck position; nothing could deliberately fail a board before, and the D-10 path needs it.
- `.planning/qa/tools/ui.py` (edited) — `tap` now prefers an exact text/desc match over a substring one; `tap "Restart"` used to hit the banner "No moves left — undo or restart" and silently do nothing.
- No app source, test, Gradle or docs file was touched; `git status` is clean and nothing was staged or committed.

## Checks
- `./gradlew check` — pass (**not run by me**; read from the producer's run at HEAD `100e69d`: `BUILD SUCCESSFUL in 11m 10s`, 98 actionable tasks, 0 `FAILED` lines).
- `./gradlew --max-workers=2 :app:assembleDebug --console=plain` — pass (`BUILD SUCCESSFUL in 47s`; run only after `pgrep -f "gradlew check"` showed the producer's build had finished).
- `~/Library/Android/sdk/emulator/emulator -avd Pixel_9_clean -no-snapshot-load -no-snapshot-save -port 5554` / same for `Pixel_9b_clean -port 5556` — pass (both `sys.boot_completed=1`).
- `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` (and 5556) — pass (`Success` on both; data preserved, no wipe).
- `python3 .planning/qa/tools/prefs.py <serial>` — pass; baseline `challenge_current_level = 17` on both, final `26` on both, `merged_player_id` identical (masked).
- `python3 .planning/qa/tools/robot.py <serial> [--leave N|--read-only]` — pass (12 boards solved/partially played; every Challenge board read as 33 cells / 4 pegs / 3-move solution).
- `python3 .planning/qa/tools/deadend.py emulator-5554` — pass (`dead end in 1 moves`, rescue bar reached).
- `./levels.sh emulator-5556 3 03-B` and `… 2 11-B` — pass, exit 0 (five levels solved, "Next level" = N+1 each time at the head).
- Pixel diff, `01-A-L17-inprogress-as-left-full.png` vs `06-A-L17-resumed-full.png` — pass (**0** board-region pixels differ at threshold 12; 726 top-bar pixels = the timer).
- `adb -s emulator-5554 shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true` — pass (picker opens; entry as reported).
- Same command with the app already running, no flags — **fail to open** (`result code=3`, delivered to top, nothing appears); with `-f 0x30000000` — pass. See Open issues.
- `adb -s emulator-5554 shell screenrecord --time-limit 20 …` + `ffmpeg -vf fps=2` over a merge launch — pass (no app-owned overlay in any of 40 frames).
- `adb -s <serial> logcat -d | grep -c FATAL` — **0** on both devices; a 20 s post-session window shows only `TaskPersister` and `ActivityTaskManager` system noise.
- Settings screen inspected by dump, masked, never screenshotted — pass (no sync row).
- `git status --porcelain` at start and end — clean; no `git add`/`commit` run.

### Logcat excerpt (device A, the 20 s window after the session, E/W/F only, app + system noise filtered)
```
E/TaskPersister(  663): File error accessing recents directory (directory doesn't exist?).
W/ActivityTaskManager(  663): Launch timeout has expired, giving up wake lock!
```
and, from the merge launch itself:
```
09-21 01:15:01.833  4776  4996 D ProfileInstaller: Installing profile for com.erman.pegsolitarie
09-21 01:20:34.815   663  1011 I ActivityTaskManager: START u0 {flg=0x10000000 cmp=com.erman.pegsolitarie/
                                 com.app.pegsolitaire.MainActivity (has extras)} with LAUNCH_MULTIPLE ... result code=3
```
(the `result code=3` line is the warm-path picker launch that did not open — Open issue 2.)

## Open issues / risks
1. **The Results "Retry" branch is unreachable from gameplay** (pre-existing, not this branch). Stuck emits no finish, so `ResultsActions`' D-10 Retry never renders on a device; the rescue bar handles it in-game. The ruling's §E clause 1(b) therefore has no device test, and `ChallengeNextLevelTest`'s retry invariant is the only cover. Not a blocker — the in-game Restart path I ran reaches the identical `onNextLevelTapped`. Worth a line to the game-designer/tech-lead so D-10's Results copy is not maintained as if it shipped.
2. **The adb command in `DebugSnapshotPickerEntry`'s KDoc only works from a cold start.** Against a running instance the bare `am start` reports "intent has been delivered to currently running top-most instance" and the picker does not appear; `-f 0x30000000` (`NEW_TASK|SINGLE_TOP`) makes the same launch open it. That is an AMS launch-mode nuance (`MainActivity` is `LAUNCH_MULTIPLE`), not an app defect, but the KDoc's documented command will look broken to the next QA session. One-line doc fix, developer's call.
3. **Emulator DNS failed mid-session.** About 25 minutes in, both AVDs stopped resolving hostnames (`ping: unknown host www.googleapis.com` while `ping 8.8.8.8` worked) and GMS logged ~1,100 `AuthPII … getToken() -> NETWORK_ERROR` lines for `drive.appdata + games_lite` in one second; B's commit sat at `change_pending = true` and would not upload. Toggling wifi and disabling private DNS did not help. Fixed by shutting both down cleanly and rebooting with `-dns-server 8.8.8.8,8.8.4.4` (same no-snapshot flags); all staged app data survived and every sync afterwards was immediate. **This is the most likely explanation of the phase-13 "GMS stopped uploading" runs** and is worth recording in `docs/qa-plan.md`: boot the AVDs with an explicit `-dns-server`, and treat a stuck `change_pending` as DNS until proven otherwise.
4. **An emulator I did not start is running**: `qemu-system-aarch64-headless -avd Pixel_9 -port 5560`, started 01:00:06 during my session, app not installed/running on it. I left it untouched. If it is another agent's, it shares the tester account and could push its own progress; merges take `max`, so it cannot lower any ladder, but whoever owns it should know it is up.
5. Play Games **app** (`com.google.android.play.games`) is installed on neither clean AVD — only GMS. Everything in this session worked through GMS, and the picker is precisely the workaround for the Play Games app having no Saved games screen. No action; noted so the next session does not go looking for it.
6. Gap-of-1 remains the UAT hazard the ruling named: this run's final incidental merge was gap 1 and would not have distinguished the fixed behaviour from the old one. Any future two-device ladder check should stage the gap by construction, as this one did.

## Verdict

**VERDICT: PASS** — both items, with evidence.
- Item 1 (D-04a at a merge gap ≥ 2): **PASS** at gap 5, plus a gap-3 re-entry case; D-04 resume fidelity exact; ladder monotonic; D-03 negatives held; "Next level" unnumbered.
- Item 2 (debug snapshot picker, L-2): **PASS**; CLOUD-03 evidence re-recorded on this build.
- §E as literally written: **not exercisable on a device** (open issue 1), covered by the equivalent path and the JVM tests.
- No high-severity bug was found; nothing here blocks the release branch.

## Handoff
Next: **tech-lead** — for sign-off on the device half of this branch's review, and to decide on the two small follow-ups (open issues 1 and 2), neither of which needs to land before the release branch. The producer should also fold open issue 3 (boot AVDs with `-dns-server`) into `docs/qa-plan.md`, update `11-HUMAN-UAT.md` test 11's result line from `PASS, D-04 sub-check PARTIAL` to PASS re-run at gap 5 on 2026-09-21, and delete `.planning/qa/phase-14-nextlevel/` after the STATUS.md Done line. Nothing was committed; the tree is clean.
