# qa-engineer report — talkback-board (TalkBack device pass, session of record)
Date: 2026-10-24
Branch: feature/talkback-board (report only; head b8aa8c4, last code commit 75a03be)
Skills loaded: ergoz-soft:standards (role qa-engineer; product snapshot 0.5.1 < plugin 1.0.0, so plugin 1.0.0 copies read: studio, reporting, testing, gitflow, skills-policy, logging, memory, kotlin-android, compose; puzzle-design and play-store identical to the snapshot; product .claude/rules/lessons.md), android-emulator-skill, android-testing
Ran in parallel with: home-chip-arrival tech-lead ruling, licences-screen auditor and ux ruling, reduced-motion ux ruling, colour-vision auditor/tech-lead/ux rulings
Ran on: claude-opus-5-5 at effort high — staffing deviation: agents/qa-engineer.md pins fable at high; Fable limit reached, founder answer Q-019 option 3 puts Fable-pinned roles on Opus until 3.0.0 is published; the code under test was also written on Opus. Every verdict below rests on a device artifact.
Fan-out: none
Loops: 0
Lessons applied: none (snapshot holds 0 active lessons; .planning/recall/RECALL.md absent)

## Summary
- Instrument: AVD `ergoz_studio` (API 36 google_apis_playstore, 1080×2424, 420 dpi), cold boot on emulator-5580; TalkBack 16.0.0.738667889, Google TTS 20241125 healthy all session. Driven only with `adb emu event mouse` (console) TalkBack gestures; speech read from TalkBack's "Display speech output" overlay (screenshots) and the same utterances in TalkBack's verbose log (`SpeechControllerImpl: Speaking fragment`).
- **The suspected Major is CONFIRMED and is wider than suspected.** In the tutorial, once the first step's board has been replaced, the board's spoken channel never again speaks its own line: steps 2 and 3 lines 0 of 6, completion line 0 of 3, rejected-tap lines 0 of 4, jump lines 0 of 11, and every coach escalation is spoken as some cell's description ("Row 3, column 2, empty, hint, land here") instead of "Hint: …" (7 of 7). Before the first step change the same lines are spoken. Completion never moves focus to "Got it" (0 of 3).
- Gameplay is largely good: all TB-6 lines, rejected lines, undo, hint, restart, stranding pair, banner moves and two whole games by ear (English-33, Challenge level 1). Failing there: the traversal order (board stop before Navigate back on gameplay, before the pips in the tutorial — 26b), Results' live region re-reading the whole screen after every count-up, the winning jump's line, jump-by-action focus through TalkBack's menu, and the confirmed no-win banner Restart landing focus on the timer.
- **VERDICT: FAIL** — four Major and five Minor items, each with steps and evidence below.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/qa-engineer.md` — this report (not committed; the producer commits).
- `.planning/qa/talkback-board/session2/` — this session's artifacts (git-ignored, 49 MB, 268 files; index below).
- `.planning/qa/talkback-board/prior-unread/` — the earlier session's 14 files, moved there unread as instructed; not evidence.

## Host chore before the session (emulator-5556, AVD Pixel_9b_clean) — done through the UI only
| Change | Before | After | How |
|---|---|---|---|
| "Allow Android Accessibility Suite to send you notifications?" | 5 stacked GrantPermissionsActivity tasks (t88…), Allow focused | all 5 answered **Don't allow**; TalkBack POST_NOTIFICATIONS granted=false (USER_SET) | explore-by-touch on "Don't allow" + double-tap, ×5 |
| TalkBack | on | **off** (Use TalkBack → Turn off) | explore + double-tap |
| Timing controls › Time to take action | 2 minutes (secure timeouts 120000) | **Default** (both 0) | `input tap` after TalkBack was off |
| Emulator | running | shut down, `adb -s emulator-5556 emu kill` (producer-authorised) | — |
Evidence: `session2/chore/00-before.png` (80 KB, the stuck dialog), `chore/05-dialogs-answered.png` (120 KB, TalkBack page after the 5th answer), `chore/07-talkback-off.png`, `chore/09-time-to-take-action.png` (2 minutes selected), `chore/10-default.png` (116 KB).

## Build and instrument
- `./gradlew :app:assembleRelease` (the Build command) fails without `PLAY_UPLOAD_*` secrets, so the debug APK was used as the producer specified: `./gradlew :app:assembleDebug` — up to date at b8aa8c4 (b8aa8c4 and 1d8e09b are docs-only over 75a03be), `app-debug.apk` 21.4 MB, sha256 50dd9bf57c355acf…, package `com.erman.pegsolitarie`.
- Device choice deviation: a real phone (GM6, PJBQV4USFAHEKBRS) is attached; the agent file prefers it, the producer's dispatch forbids touching it. ergoz_studio was used; no other serial was installed on or driven.
- TTS precondition (brief §10.2): swipe on the launcher spoke "Sat, Oct 24" in the overlay (`tts-01-home-swipe.png`); `dumpsys accessibility` — TalkBack bound, `Crashed services:{}` at start and end. Not VOID.

## Per-item table
| Item | Result | Evidence (artifact; spoken lines from `session2/speech-log.txt`) |
|---|---|---|
| TB-1 board stop (gameplay) | PASS | From the timer, next → "Board, 32 pegs left" + "Double-tap to activate"; double-tap: nothing spoken, board, timer 00:00 and disabled Undo unchanged (`g-07`, `g-08-boardstop-dtap.png`) |
| TB-1 board stop (tutorial) | FAIL (B-3) | Swiping forward from Skip lands on "Row 1, column 1 …", never on the board stop; the board stop sits before the pips (`tut-05-swipe1..4`, `tut-06-prev1..4`) |
| TB-1 read-only boards | PASS | Touch on Home's stage board: no stop, nothing spoken; tile art is part of the tile stop "Classic, 5 boards" (`tb1-home-stage-touch.png`, `tb1-home-tile-art-touch.png`) |
| TB-2 one stop per cell | PASS portrait; see B-10 landscape | English-33: 33 cell stops row-major, first "Row 1, column 3, peg, no jumps", centre "Row 4, column 4, empty" (`g-06-order-light.png`, dark `dark-06-order.png`); tutorial cells row-major |
| TB-3 selection, jumps, targets, hints | PASS | "Row 2, column 4, peg, selected, 1 jump"; "Row 4, column 4, empty, jump target, hint, land here"; "Row 4, column 2, peg, 1 jump, hint, move this peg" (`g-20`, `g-21`) |
| TB-4 double-tap = tap, labels, rejected lines (gameplay) | PASS | select / put down / jump / three rejections all by double-tap; "Double-tap to jump here", "Double-tap to select"; "No peg here." "Put down the selected peg first." "Not a jump target." (`g-13`, `g-15`, `g-16`) |
| TB-4 rejected lines (tutorial) | FAIL (B-1) | Step 1: "No peg here." spoken twice; after the step change: 0 of 4 rejected taps spoken (`t2-08`, `t2-09`, `t3-07`, `t3-09`) |
| TB-5 one action per legal jump, order | PASS | (1,1) on T3 lists "Jump down to row 3, column 1" then "Jump right to row 1, column 3"; actions make the move (`tut-16-action-right.png`, `tb10-e33-m*`) |
| TB-6 lines on gameplay | PASS except B-6, B-7 | selection, put-down "Peg put down, row 5, column 4.", jump by tap and by action, undo, hint, restart, banner (`g-11`, `g-17`, `g-18`, `g-19`, `g-21`, `g-23`, `g-26`) |
| TB-6 stranding jump | PASS | "No moves left — undo or restart" then "Row 7, column 4 to row 5, column 4. Removed row 6, column 4. 26 pegs left.", each once, neither cut — 3 of 3 (light ×2, dark) (`g-28-strand.png`, `dark-strand-m6.png`) |
| TB-6 / TB-9 lines in the tutorial | FAIL (B-1) | see B-1 |
| TB-7 dock Undo/Hint/Restart keep focus | PASS | Undo stays focused after the undo line; Hint re-reads "Hint, Button"; Restart refocused after its dialog (`g-18-undo.png`, `g-19`, `g-23`) |
| TB-7 banner Undo/Restart → board stop (stuck banner, portrait) | PASS | "Undid … 27 pegs left." then "Board, 27 pegs left"; "Board restarted. 32 pegs left." then "Board, 32 pegs left" (`g-31-banner-undo.png`, `g-34-banner-restart.png`, `nw-00-undo.png`) |
| TB-7 banner Restart confirmed in dialog (no-win banner) | FAIL (B-8) | "Board restarted. 4 pegs left." then focus on "Elapsed time" (`nw-06-confirmed.png`); declined dialog moves nothing (`nw-05-declined.png`, PASS) |
| TB-7 jump by action → destination | FAIL via TalkBack menu (B-5); PASS via Actions reading control | menu: focus back on source "Row 2, column 4, empty" 3 of 3 (`g-12-jump-action.png`, `tut-17-after-action-7s.png`, `t2-03-after-T1-action-9s.png`); reading control: line, then "Row 4, column 4, peg, no jumps" 33 of 33 (`g-26-rc-jump.png`, `tb10-e33-m1..31`) |
| TB-7 tutorial step 2/3 → board stop | PASS by double-tap (6 of 6); FAIL after a menu action (B-5) | "Board, 2 pegs left" / "Board, 5 pegs left" focused (`tut-10`, `t2-05`, `t3-06`, `t3-10`); after T1 by menu action focus landed on step 2's (1,1) cell (`t2-03-after-T1-action-9s.png`) |
| TB-7 completion → "Got it" | FAIL (B-2) | 0 of 3: focus stays on the last destination cell (`t2-15-complete-later.png`, `t3-12-complete.png`) |
| TB-7 banner rising moves no focus | PASS | after each stranding jump focus stayed on the destination cell |
| TB-7 keyboard, touch exploration off | PASS | Tab cycles Navigate back → Hint → Show moves → Restart → Navigate back, never a cell or the board stop (`kbo-00`, `kbo-tab1..8.png`) |
| TB-8 star row one stop | PASS | "3 of 3 stars", next stop "New best!" (`tb8-starrow.png`) |
| TB-8 summary once | FAIL (B-4) | English-33: "Perfect! — 3 of 3 stars, 1 peg left." then the whole column read; Challenge 1: summary not spoken, whole column read twice (`tb8-results-e33.png`, `tb10-c1-win.png`) |
| TB-8 rotation speaks no second summary | PASS (summary) / FAIL (column re-read, B-4) | rotation and theme change: no summary, but the whole column is read twice (`tb8-results-rotated.png`, `tb8-results-back-portrait.png`, `dark-03-app.png`) |
| TB-9 tutorial by ear | FAIL (B-1, B-2, B-3) | completable with TalkBack gestures only (3 runs), but steps 2–3 lines, completion line, Got-it focus and post-step-1 announcements missing |
| TB-9 "Step 1 of 3" heard twice (A2.4) | NOT observed — F-42 fallback not needed | the pip row is never auto-read on opening; TalkBack's first focus is the board stop (B-3) |
| TB-9 no "wrong/invalid/error" | PASS | none in 773 utterances |
| TB-10 English-33 by ear | PASS (operable) | 31 moves by jump actions, Results "Perfect!" 1 peg (`tb10-e33-m1..31.png`, `tb8-results-e33.png`); winning move's line missing (B-6) |
| TB-10 Challenge level 1 by ear | PASS (operable) | Hint ×3, two action jumps, last by double-taps, Results (`tb10-c1-*.png`); summary missing (B-4), winning line missing (B-6) |
| TB-11 sighted play untouched | PASS | TalkBack off: finger select + jump on Level 2 work, 0 new speech lines (`tb11-01`, `tb11-02-finger-jump.png`); keyboard row above |
| TB-12 | JVM/auditor item | device: hint lines only after Hint or a coach escalation |
| 26a control case | PASS | Classic catalogue back arrow: one stop "Navigate back, Button" (`g-03-catalog.png`) |
| 26b gameplay order, light and dark | FAIL (B-3) | Restart → wrap → "Elapsed time" → "Board, 32 pegs left" → "Navigate back" → "Peg Solitaire, Heading" → 33 cells → [banner] → Undo → Hint → Show moves → Restart (`g-06-order-light.png`, `dark-06-order.png`) |
| 26b banner follows the last cell (portrait light, dark) | PASS | last cell → "No moves left — undo or restart" → Undo → Restart → Home → dock Undo (`g-29-banner-order-1..8`, `dark-07-banner-order.png`) |
| 26b landscape read (A2.6) | PASS for the banner pin; see B-10 | landscape: last reachable cell (row 5) → banner → its three buttons → dock (`land-03-order.png`) |
| 23d Undecided notice | NOT RUN | no deterministic way to make Hint time out on the device in this session; the board channel never carried the banner's text in 3 strandings |

## Device questions (A1.2 item 9)
1. **TalkBack follows input focus, per move:** jump by action — yes through the Actions reading control, no through TalkBack's menu (TalkBack restores focus to the menu's origin node when the dialog closes); tutorial step 2/3 → board stop — yes after double-taps, no after a menu action; banner Undo/Restart → board stop — yes (stuck banner, portrait), no for Restart confirmed in its dialog (lands on the timer); completion → "Got it" — no, 0 of 3.
2. **Announcement then focus:** yes where both arrive (banner Undo/Restart, reading-control jump: line spoken in full, then the stop; no truncation from the 200 ms gap). Tutorial step changes: the focus is spoken, the step line never is. Landscape banner Undo: focus flashed to "Peg Solitaire" before the board stop and the undo line was lost (B-9).
3. **Re-read of a changed focused cell:** **yes**, every time — e.g. "Row 2, column 4, peg, selected, 1 jump" then "Selected row 2, column 4, 1 jump." (`g-11-select-4s.png`). The pre-ruled answer (A1.2 item 7) applies (B-7).
4. **Stranding jump's two lines:** each once, banner first, neither cut — 3 of 3.
5. **TTS health:** healthy throughout (TTS 20241125.02_p2; no crash).

## Bugs
**B-1 (Major) — tutorial: after the first step's board is replaced, no board line is spoken again.** TB-9, TB-6, TB-4 amended.
Steps: fresh install (or Settings › How to play) with TalkBack on; wait for "Step 1 of 3. …"; clear T1 with two double-taps (select (1,1), double-tap (1,3)); in step 2 wait 6 s, double-tap the empty (1,1), select (1,2), double-tap the empty (1,3), double-tap (3,2); in step 3 make any jump; finish.
Expected: "Row 1, column 1 to row 1, column 3. Removed row 1, column 2. 1 peg left.", "Step 2 of 3. Jumps work up, down, left and right — never diagonally.", "Hint: row 1, column 2, jump down to row 3, column 2.", "No peg here.", "Not a jump target.", each jump's line, "Step 3 of 3. …", "That's the whole game. Go clear a board."
Actual (run 3, dark, `t3-06`, `t3-07`, `t3-09`, `t3-10`, `t3-12`): only focus speech and cell re-reads; the coach fires as a cell description. Same in runs 1 and 2 (light). Counts over the three runs after the first step change: step lines 0 of 6, done line 0 of 3, rejected lines 0 of 4, jump lines 0 of 11, coach 7 of 7 spoken as a cell description and 0 as "Hint: …". The T1-clearing jump by double-tap also lost its line (0 of 2; by menu action 1 of 1). Before the first step change the channel works (step-1 line 2 of 3 openings; "No peg here." ×2; "Hint: row 1, column 1, …" 3 of 3). Gameplay changing level (Challenge 1 → 2, new screen) keeps working, so the loss follows the in-place step board swap. Lead, not a finding: a live-region change spoken as a cell's description suggests TalkBack is resolving the event to a node that is now a cell.
```
21:46:28.278 talkback_897 done Row 1, column 3, empty, jump target, hint, land here [FOCUSED]
21:46:38.144 talkback_899 CUT  Row 1, column 3, peg, no jumps                     [CONTENT_CHANGED]
21:46:39.026 talkback_900 done Board, 2 pegs left                                  [FOCUSED]
21:46:42.939 talkback_902 done Row 3, column 2, empty, hint, land here             [CONTENT_CHANGED]
21:47:03.200 talkback_903 done Row 1, column 1, empty                              [FOCUSED]   (double-tap: nothing)
21:47:36.997 talkback_908 done Row 1, column 3, empty                              [FOCUSED]   (double-tap: nothing)
21:51:05.316 talkback_940 done Row 1, column 3, peg, no jumps                      [CONTENT_CHANGED]  (last jump; no done line)
```
Also seen: run 2's opening spoke no step line at all (`t2-00-open-12s.png`).

**B-2 (Major) — tutorial completion never moves focus to "Got it".** TB-9, TB-7. Steps: finish T3 by double-tap (runs 1–3) — Expected: done line, then focus on "Got it". Actual: focus stays on the last destination cell "Row 1, column 3, peg, no jumps"; "Got it" is reached only by swiping past the cells and the caption (`t2-15-complete-later.png`, `t2-16-next8.png`, `t3-12-complete.png`).

**B-3 (Major) — the board stop is out of the 26b order on both boards.** 26b, TB-1 (tutorial), TB-9. Steps: TalkBack on, swipe forward through the gameplay screen and through the tutorial. Expected: gameplay back → title → timer → board stop → cells → [banner] → dock; tutorial pips → Skip → board stop → cells → caption. Actual: gameplay "Elapsed time" (first stop) → "Board, 32 pegs left" → "Navigate back" → "Peg Solitaire, Heading" → cells → …; tutorial "Board, 2 pegs left" → "Step 1 of 3" → "Skip tutorial" → cells. The cells stay in place; only the board stop jumps ahead, and in the tutorial TalkBack's first focus on opening is the board stop. Light and dark identical (`g-06-order-light.png`, `dark-06-order.png`, `tut-05-swipe1..4.png`, `tut-06-prev1..4.png`). Lead: the slab's traversal index (`GameTraversal.BOARD_BEFORE_BANNER`, −1) and the slab's own `isTraversalGroup` do not appear to stay inside their groups on device.

**B-4 (Major) — Results' live region re-reads the whole screen, and the summary is not reliable.** TB-8, TB-6. Steps: win English-33 (by jump action) and Challenge level 1 (by double-tap); then rotate and change the theme on Results. Expected: "Perfect! — 3 of 3 stars, 1 peg left." once, nothing more. Actual: English-33 — summary, then the whole column "Perfect! | 3 of 3 stars | 3100 | SCORE | 19:08 | TIME | 1 | PEGS LEFT | Replay | Button | Catalog | Button | Home | Button" (≈16 s); Challenge 1 — no summary, the whole column twice; each rotation or theme change — the whole column twice, once with the count-up's mid value (3067, 2895) and once with the final value (`tb8-results-e33.png`, `tb10-c1-win.png`, `tb8-results-rotated.png`, `tb8-results-back-portrait.png`, `dark-03-app.png`). The auditor's open risk (live region on the count-up column) is confirmed.

**B-5 (Major, needs a ux-designer/tech-lead ruling) — jump by action through TalkBack's menu leaves focus on the source.** TB-7, device question 1. Steps: focus a peg, swipe up-then-right (TalkBack menu) → Actions → "Jump down to row 4, column 4" → double-tap. Expected: focus on the destination. Actual: the jump line is spoken, then ~7 s later, as the Actions window closes, TalkBack restores focus to the source "Row 2, column 4, empty. Out of list" (3 of 3: `g-12-jump-action.png`, `tut-17-after-action-7s.png`; and in the tutorial the step-2 board-stop move is lost the same way, `t2-03-after-T1-action-9s.png`). Through the Actions reading control (swipe down, double-tap) the move works 33 of 33. F-TB-1's fallback was written for "TalkBack does not follow"; here it follows on one TalkBack route and not the other, so the spec owner should rule.

**B-6 (Minor) — a winning jump's line is never spoken.** TB-6 / §11.1 "When a jump wins, the jump's line is followed by Results' summary". Steps: make the last move of English-33 (action) or Challenge 1 (double-tap). Actual: Results opens and only Results speaks (`tb10-e33-m31.png`, `tb10-c1-win.png`). 0 of 2.

**B-7 (Minor, pre-ruled) — TalkBack re-reads the focused cell on selection, so the selection is spoken twice.** TB-6 amended, A1.2 item 7. Steps: double-tap a peg. Actual: "Row 2, column 4, peg, selected, 1 jump" then "Selected row 2, column 4, 1 jump." (`g-11-select-4s.png`, `g-14`, `g-20`). Pre-ruled one-commit answer: drop the selection line while touch exploration is on.

**B-8 (Minor) — no-winning-move banner: Restart confirmed in its dialog puts focus on the timer.** TB-7 ("never at the top of the screen after a board action"). Steps: Level 2, play (5,4)→(5,6), Hint → "No winning move — undo or restart"; banner Restart → "Restart Board". Actual: "Board restarted. 4 pegs left." then "Peg Solitaire" (window) and focus "Elapsed time" (`nw-06-confirmed.png`). The stuck banner's immediate Restart is fine (`g-34`).

**B-9 (Minor) — landscape banner Undo: the undo line is lost and focus flashes to the title.** Device question 2, auditor L1-2's symptom without a keyboard. Steps: strand English-33, rotate to landscape, banner Undo, double-tap. Actual: "Peg Solitaire" (cut) then "Board, 27 pegs left"; no "Undid …" line (`land-05-undo.png`, `land-05-undo-1s.png`). Portrait: 3 of 3 correct.

## Observations outside the diff (for the producer to route; not counted in the verdict)
- **B-10 landscape layout:** on a 1080×2424 phone in landscape the board is clipped — only rows 3–5 show, the banner and dock cover the rest — and TalkBack reaches only those 21 cells (`land-00.png`, `land-03-order.png`, `land-05-undo.png`). The layout files are not in this branch's diff as far as the brief lists; whether develop does the same was not checked here.
- The timer stop speaks "Elapsed time" with no value (§4.3.2 / D-05d's `elapsed_time_spoken` is not on this tree).
- The rescue banner's region re-announces "No moves left — undo or restart" on rotation (`land-00.png`; 23k, hint-undecided's).
- On a fresh install the UMP consent form opens ~6 s into the tutorial, over the step-1 board, and takes TalkBack's focus (`tut-01-open-6s.png`).
- Results' count-up restarts on every rotation/theme change (3067, 2895 mid-values above) — the auditor's routed `StarPopSounds` replay item is the same family.

## Not run / blocked
- **Auditor L1-2 hardware-keyboard check: BLOCKED.** With TalkBack on, `adb shell input keyevent KEYCODE_TAB` hung (killed after ~3 min); the console's `event send EV_KEY:KEY_TAB` moved focus once ("Navigate back", then "Board, 2 pegs left") and then produced no further focus change. B-9 is the closest touch-mode evidence.
- 23d (Undecided notice), and TB-1's "Board, 1 peg left" (Results replaces the board on the winning move).

## Session faults (mine) and system changes on ergoz_studio
- At 20:40:50 a `uiautomator dump` (diagnostic) registered UiAutomation, which suspended TalkBack ("TalkBack off"/"TalkBack on" in the log); TalkBack's notification prompt came back and my next double-tap granted it. Run 1 data after 20:40:50 was discarded; runs 2 and 3 repeat everything. No dump was used after that, and none is evidence (26a).
- Changes and their restoration (initial vs final state: `initial-state.txt`, `final-state.txt`):
  | Change | Restored |
  |---|---|
  | TalkBack on (Settings UI) | off via Settings UI; enabled_accessibility_services empty, touch_exploration_enabled 0 |
  | TalkBack shortcut accidentally switched on (accessibility button) | switched off via the same toggle; accessibility_button_targets empty |
  | TalkBack POST_NOTIFICATIONS: answered Don't allow, later granted by the stray double-tap | revoked in App notifications UI: granted=false; residual flag USER_SET (initial had none) |
  | Dark theme on (Display & touch) | off via the same toggle; ui_night_mode 1 as found |
  | Rotation by `adb emu rotate` (auto-rotate setting untouched) | back to ROTATION_0 |
  | App installed, `pm clear` once, played | uninstalled (`pm list packages` has no peg package) |
  | TalkBack's own reading-control selection moved (Actions/Characters) | TalkBack-internal; not restorable by setting, recorded |
  | "Display speech output" on, log level VERBOSE | already so at boot; untouched |
- Booted with `-no-snapshot-load -no-snapshot-save` (quick-boot snapshot untouched); shut down with `adb -s emulator-5580 emu kill`. Not touched: GM6, Pixel_9a_bench, Pixel_9_clean (emulator-5602, another session's), any other AVD.

## Checks
- `./gradlew :app:assembleRelease --console=plain -q` — fail (missing PLAY_UPLOAD_KEY_PASSWORD / PLAY_UPLOAD_STORE_PASSWORD; secrets not in this shell)
- `./gradlew :app:assembleDebug --console=plain -q` — pass (up to date, 4.6 s; APK sha256 50dd9bf57c355acf…)
- `./scripts/check.sh --max-workers=4 --console=plain` at b8aa8c4 — pass (BUILD SUCCESSFUL in 11m 8s, exit 0; 104 tasks, 11 executed; `check.txt`)
- `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass (boot_completed=1)
- `adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk` — pass; `adb -s emulator-5580 uninstall com.erman.pegsolitarie` — pass
- `adb -s emulator-5580 shell dumpsys accessibility` — TalkBack bound, `Crashed services:{}`
- `adb -s emulator-5580 logcat -d --pid=7187 | tail -n 500` — 239 lines, 0 FATAL/AndroidRuntime/Exception (app pid; `logcat.txt`)
- `adb -s emulator-5556 emu kill`, `adb -s emulator-5580 emu kill` — pass

## Artifacts (`/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/qa/talkback-board/session2/`)
| Path | Size | What it shows |
|---|---|---|
| `speech-raw.log` | 2.0 MB | TalkBack verbose lines: every gesture, utterance start and completion (true = done, false = cut) |
| `speech-log.txt` | ~80 KB | 773 utterances, one per line: time, id, done/CUT, text, event (built by `tools/speech.py`) |
| `logcat.txt` | 32 KB | app-pid logcat dump; no errors |
| `initial-state.txt`, `final-state.txt` | 4 KB each | ergoz_studio settings before and after |
| `build-release.txt`, `build-debug.txt`, `emulator-boot.log`, `check.txt` | ≤12 KB | command output |
| `chore/` | 12 files, 1.2 MB | emulator-5556 restoration |
| `setup-*.png`, `tts-01-home-swipe.png`, `00-initial.png`, `99-final.png` | 13 files | TalkBack setup, TTS check, start and end screens |
| `tut-*.png` (run 1), `t2-*.png` (run 2), `t3-*.png` (run 3, dark) | 40 / 24 / 17 files, 10 MB | tutorial passes; key: `tut-10-step2-7s.png` (coach spoken as a cell), `t2-15-complete-later.png`, `t3-12-complete.png` (no Got-it focus) |
| `g-*.png` | 47 files, 12 MB | gameplay light: order, TB-1, TB-4/6 sequence, banners |
| `dark-*.png` | 14 files | gameplay dark: order, strand, banner order |
| `land-*.png` | 7 files | landscape banner read and Undo |
| `tb10-e33-m1..31.png`, `tb10-c1-*.png`, `c-*.png`, `c2-*.png` | 45 files | TB-10 games |
| `tb8-*.png` | 6 files | Results, star row, rotations |
| `nw-*.png` | 8 files | no-winning-move banner, dialog decline/confirm |
| `kb-*.png`, `kbo-*.png`, `tb11-*.png` | 18 files | keyboard attempts with TalkBack on; Tab with TalkBack off; finger play |
| `off-*.png`, `restore-*.png` | 11 files | TalkBack off, dark theme off, notification permission revoked |
| `tools/` | 9 helper scripts | `tb.py` (console gestures), `cap.sh`, `quiet.sh`, `speech.py`, `g.sh`, `play.sh`, `plan33.py`, `solve33.py`, `strand.py` — session helpers, never committed |
| `solution33.txt`, `plan33.txt`, `planstrand.txt`, `planc1.txt`, `strand.txt` | <4 KB | move plans used by the helpers |

## Open issues / risks
- B-1 and B-2 are the TB-9 blockers; B-3 affects every screen with a board; B-4 affects every result. B-5 needs a spec ruling before a fix. B-7 is pre-ruled.
- Everything here is one emulator image (API 36, TalkBack 16.0). A phone pass is still worth one sitting before release, especially for B-5 and B-9, which depend on TalkBack's own focus restoration.
- The L1-2 keyboard check stays open.

## Handoff
Next: tech-lead — the review verdict is FAIL; the developer's fix loop needs B-1 to B-4 (Major), B-6 to B-9 (Minor), and a ruling on B-5 from the ux-designer (through the producer). The producer routes the observations outside the diff (landscape crop, timer value, banner re-announce on rotation, consent over the tutorial, count-up replay) and the open L1-2 keyboard check.
