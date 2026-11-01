# qa-engineer report — talkback-board (TalkBack device re-run, tech-lead ruling §11.3)
Date: 2026-11-01
Branch: feature/talkback-board (report only; built at head 84d4a4f, code = 402e5af)
Skills loaded: ergoz-soft:standards (role qa-engineer; product snapshot 0.6.2 < plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing were read in full; logging, puzzle-design and play-store are identical to the snapshot; the product's .claude/rules/lessons.md), android-emulator-skill, android-testing. **Order, recorded:** the two Android skills were invoked after the device session, not before it. The session reused my own helper scripts from session 2 and needed no command from those skills.
Ran in parallel with: code-auditor merge review (talkback-board, reading the same worktree)
Ran on: claude-opus-5-5 at effort high. Staffing deviation: agents/qa-engineer.md pins fable. Founder answer Q-019 (option 3) keeps Fable-pinned roles on Opus until 3.0.0. Every verdict below rests on a device artifact.
Fan-out: none
Loops: 0 (re-run of the device FAIL in `qa-engineer.md`, which this report supersedes)
Lessons applied: none (the snapshot holds 0 active lessons; .planning/recall/RECALL.md is absent)

## Summary
- Scope followed the founder's reduced-QA directive: defects B-1 to B-9 as ruled, plus one smoke pass. No light/dark matrix and no 15e sitting. Emulator `ergoz_studio` only, cold boot, TalkBack 16.0.0.738667889, Google TTS 20241125.02_p2, healthy throughout (`Crashed services:{}`).
- **Fixed on device:** B-2 (Got it focused 3 of 3, on the menu route too), B-3 (26b on the tutorial and gameplay, light and dark, plus A2.6), B-4 (summary once, no column read, nothing on rotation or theme change, 2 wins), B-6 (D-TBc: the summary comes first, 2 of 2), B-7, B-9 (3 of 3). B-5 meets the D-TBb floor on gameplay (3 of 3), and the reading control lands on the destination.
- **Still failing (verdict FAIL):**
  - **B-1 (residual, Major).** A tutorial step's line is lost whenever TalkBack speaks the clearing jump's own line first. By double-tap this happened 1 of 6 times. Through TalkBack's menu it happened 2 of 2 times, because there the jump line is always spoken. Every other tutorial line now works.
  - **B-8 (Minor).** A confirmed banner Restart puts focus on "Navigate back", 0 of 3 on the board stop. The ruling's no-code fallback applies.
  - **A2.4's fallback is triggered.** "Step 1 of 3" is heard twice on each of 3 clean openings.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/qa-engineer-device2.md` — this report. It is committed on the branch alone.
- `.planning/qa/talkback-board/session3/` — this session's artifacts (git-ignored, 62 MB, 214 files; index under Artifacts). Nothing is committed. The screenshots show no gamer tag, because Play Games sign-in was answered "Not now" each time.

## Build, device and instrument
- Build: `./gradlew :app:assembleDebug` ran in a detached scratch worktree at `84d4a4f`, so the code-auditor's worktree was not touched. The worktree was removed afterwards. `app-debug.apk` is 21,394,921 B, sha256 `11161303ee8f3dd11b028d32f828719e1246789338a67c508f4e992d99a93936`. The Build command (`assembleRelease`) needs the PLAY_UPLOAD_* secrets, so the debug APK was used, as the ruling's §11.3 specifies.
- Precondition met: the tip contains `develop` at `305f741` with `bugfix/home-chip-arrival`. So the timer's position and value are judged here as this build's: "Elapsed time, under a minute" and "Elapsed time, 4 minutes".
- Device: `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim`. No uiautomator dump was taken (26a). Not touched: Pixel_9, Pixel_9a, Pixel_9a_bench, and the attached phone.
- TTS precondition (brief §10.2): a swipe on the launcher spoke "Sun, Nov 1" in the speech overlay (`tts-01-home-swipe.png`). `dumpsys accessibility` showed TalkBack bound with `Crashed services:{}` at both start and end. The session is not VOID.
- Drive: `adb emu` console `event mouse` TalkBack gestures. Speech was read from TalkBack's verbose log (`SpeechControllerImpl: Speaking fragment`), with the speech-output overlay in the screenshots.
- **Instrument fault, recorded.** At 03:41:55 the emulator's `system_server` died (`DeadSystemException` in the crash buffer). Host load was 47 on 10 cores at the time, from other worktrees' Gradle runs, and guest load was 87. The app was not in use. Everything in the first attempt after 03:41 is discarded. I waited for the load to fall (host 1-minute average under 14, guest under 6), cleared the app's data and started run 1 again from a fresh state at 03:49. One sample from before the fault (03:36, T1 by double-tap: jump line spoken, step-2 line lost) is listed as a recorded sample, not counted.

## Per-defect verdict table
| Defect (ruling §11.3 item) | Verdict | Device evidence (from `session3/speech-log.txt`) |
|---|---|---|
| B-1 — tutorial channel after a step swap (item 1, TB-9) | **FAIL (residual)** | **Fixed:** rejected taps 6 of 6 on T2 and T3 ("No peg here." / "Not a jump target." / "Put down the selected peg first."). Coach spoken as "Hint: …" on every escalation, never as a cell's text. T3 jump lines 6 of 6. Done line 3 of 3. **Not fixed:** step lines. By double-tap, 5 of 6 (r1 T1 ✓, r1 T2 ✓, r2 T1 ✓, **r2 T2 ✗**, r4 T1 ✓, r4 T2 ✓). Through the menu, **0 of 2** (T1, T2). The step line is lost exactly when the clearing jump's own line is spoken; when the step line replaces it, the step line is spoken. See B-1 residual below. |
| B-2 — completion → "Got it" | PASS | Run 1 (light, double-tap), run 2 (dark, double-tap) and run 3 (dark, menu action): done line, then "Got it \| Button" focused, 3 of 3 (`r1-15-complete.png`, `r2-T3-complete.png`, `m-06-T3-complete-menu.png`) |
| B-3 — board stop in the 26b order (item 2) | PASS | Tutorial, light and dark: pips → "Skip tutorial" → "Board, 2 pegs left" → row 1's cells → caption (`26b-tut-light-n1..6`, `26b-tut-dark-n1..3`). Gameplay, dark (Challenge 1): back → "Challenge level 1, Heading" → "Elapsed time, under a minute" → "Board, 4 pegs left" → "Row 1, column 3"; last cell → "Undo" (`26b-c1-dark-*`). Gameplay, light (English-33): the same, ending "Board, 32 pegs left" → row 1 col 3; last cell → Undo (`26b-e33-light-*`). A2.6 on a `wm size 1440x1900` portrait window (548 dp wide) with the banner raised: title → timer → board stop → cells to row 6 → "No moves left — undo or restart" → "Undo" (`a26-wide.png`, `a26-order` lines). Row 7 sits under the banner and is unreachable (B-10 family, not judged). |
| B-4 / TB-8 — Results (item 3) | PASS | Challenge 1 (double-tap win): "Perfect! — 3 of 3 stars, 1 peg left." once, then focus "Navigate back"; no column read (`b4-c1-results.png`). Rotation there and back: only "Landscape"/"Portrait", the window name and focus (`b4-c1-rotated`, `b4-c1-back-portrait`). Theme change dark→light: the window name and focus only (`b4-c1-theme-light.png`). English-33 (action win): the summary once and in full; rotation there and back spoke nothing from the app (`b4-e33-*`). Count-up: no reads in either case. |
| B-6 — winning jump (item 4, D-TBc) | PASS | Challenge 1 by double-tap: the summary was the first app line, 1.2 s after the tap. English-33 by reading-control action: the summary was the first app line, once and in full. Observation O-1 below covers TalkBack's own focus read of "Perfect!" just before it. |
| B-7 — selection spoken twice (item 5) | PASS | Every selection gave one utterance, the cell's re-read ("Row 5, column 3, peg, selected, 1 jump"), with no "Selected …" line, on gameplay and in the tutorial. The put-down is still spoken: "Peg put down, row 5, column 3." (`b7-c1-select`, `b7-c1-putdown`) |
| B-8 — banner Restart confirmed in dialog (item 6) | **FAIL** (pre-ruled fallback) | Level 2, (5,4)→(5,6), Hint → "No winning move — undo or restart"; banner Restart → "Restart Board". Each time: "Board restarted. 4 pegs left." → "Peg Solitaire" (window) → focus **"Navigate back \| Button"**, 0 of 3 on the board stop. No later move came within 6 s (`b8-1..3-confirmed.png`). Declined: focus returns to the banner's Restart and nothing moves (`b8-declined.png`, PASS). |
| B-9 — landscape banner Undo (item 7) | PASS | 3 of 3: "Undid row 7, column 4 to row 5, column 4. 27 pegs left." then "Board, 27 pegs left" focused. No title flash was heard (`b9-1..3-undo.png`). The landscape clip (rows 3–5) is recorded under B-10, not judged. |
| B-5 — jump action routes (item 8, D-TBb) | PASS at the floor (gameplay); tutorial menu route FAIL via B-1 | **Gameplay, menu route, 3 of 3 at the floor:** the line in full; the menu closes about 7–9 s later; focus rests on the source, "Row 6, column 3, empty", "Row 4, column 6, empty", "Row 4, column 3, empty"; no second move within 8 s. The source fragment and the destination read never both occur, so the half-second bound does not apply. F4's window-focus wait does not bring the menu route to the destination. **Reading control, 2 of 2 full waits** (plus 2 partials cut by my next gesture): "Row 5, column 1, empty" (a 0.2 s content-change fragment) → the jump line in full → the destination focus "Row 5, column 3, peg, 1 jump" / "Row 4, column 3, peg, no jumps". **Tutorial, menu route:** T1 and T2 clearing jumps land on the new board's **board stop** (the requirement, not just the floor), but the step line is missing (B-1 residual), so both fail the floor's first condition. T3's completing jump via the menu: done line, then "Got it" (the requirement). |
| A2.4 — "Step 1 of 3" heard twice (item 1) | **Fallback triggered** | 3 of 3 clean openings (Settings › How to play; light ×1, dark ×2): "Step 1 of 3. Tap a peg, then tap the empty hole two cells away." then TalkBack's first focus "Step 1 of 3" on the pips (`op1-light-open-12s`, `op2-dark-open-12s`, `op3-dark-open-12s`). Per the ruling, the pre-ruled one-commit fallback (the first step's opening line speaks the caption alone) goes to the developer. |
| L1-2 — hardware keyboard, TalkBack on (item 10) | BLOCKED | `event send EV_KEY:KEY_TAB` ×5 on gameplay produced no focus change and no speech. Recorded as before; it does not hold the merge. |

### B-1 residual — a tutorial step's line is lost when the clearing jump's line is spoken first (Major; TB-9, D-TBb floor)
Steps: Settings › How to play (or a fresh install, after the UMP form), with TalkBack on. In step 1, select (1,1) and jump to (1,3) with **TalkBack's menu** (swipe up then right → Actions → "Jump right to row 1, column 3"). Repeat in step 2 with (1,2) → "Jump down to row 3, column 2".
Expected (§11.1, "What is spoken"; D-TBb floor condition 1): the jump's line, unless replaced by the step's line; then "Step 2 of 3. Jumps work …" (or "Step 3 of 3. …") once and in full; then the board stop.
Actual: the jump's line is spoken in full, then the window, then "Board, 2 pegs left" (or 5) and the coach. The step line is never spoken. This happened 2 of 2 times through the menu, and 1 of 6 times by double-tap, the same way, whenever TalkBack spoke the jump line rather than the destination cell's re-read.
```
04:04:58.919 talkback_229 done Row 1, column 1 to row 1, column 3. Removed row 1, column 2. 1 peg left.   [CONTENT_CHANGED]  (menu action)
04:05:05.675 talkback_230 done Peg Solitaire                                                            [WINDOWS_CHANGED]
04:05:06.866 talkback_231 done Board, 2 pegs left                                                       [FOCUSED]
04:05:08.847 talkback_232 done Hint: row 1, column 2, jump down to row 3, column 2.                     [CONTENT_CHANGED]
04:00:37.182 talkback_165 done Row 1, column 2 to row 3, column 2. Removed row 2, column 2. 1 peg left. [CONTENT_CHANGED]  (double-tap, run 2)
04:00:44.164 talkback_166 done Board, 5 pegs left                                                       [FOCUSED]
--- the passing shape, for contrast (run 1 T2):
03:51:58.532 talkback_52  done Row 3, column 2, peg, no jumps                                           [CONTENT_CHANGED]
03:51:59.450 talkback_53  done Step 3 of 3. Keep jumping. Clear the board down to one peg.             [CONTENT_CHANGED]
03:52:06.223 talkback_54  done Board, 5 pegs left                                                       [FOCUSED]
```
Lead, not a finding. While the jump line is being spoken (about 7 s on this emulator's TTS), TalkBack logs 4–5 "Queued speech item" entries, and only the board stop's focus is spoken after it (`speech-raw.log` 04:00:37–04:00:44). The step line is queued and then dropped. The channel's timed clear, or the step's board swap, during the long utterance would fit this. It is a race, and its frequency depends on TTS speed, so a phone may show it less often than the emulator does.

## Smoke pass (one, per the founder's directive)
| Item | Result | Evidence |
|---|---|---|
| TB-1 no-op double-tap (gameplay, tutorial) | PASS | double-tap on "Board, 4 pegs left" / "Board, 2 pegs left": nothing spoken, board unchanged (`tb1-game-boardstop-dtap`, `tb1-tut-boardstop-dtap`) |
| TB-4 three rejected lines (gameplay) | PASS | "No peg here.", "Put down the selected peg first.", "Not a jump target." (`tb4-c1-*`) |
| TB-6 stranding pair | PASS | "No moves left — undo or restart" then "Row 7, column 4 to row 5, column 4. Removed row 6, column 4. 26 pegs left.", each once, neither cut (`tb6-strand.png`) |
| TB-7 dock and portrait banner Undo | PASS | dock Undo: "Undid …" with focus kept; Hint: "Hint: row 4, column 2, …" with focus kept; Restart: "Board restarted. 32 pegs left.", focus kept. Banner Undo (portrait): "Undid … 27 pegs left." then "Board, 27 pegs left" (`tb7-*`) |
| TB-10 by ear | PASS | Challenge 1 to Results (menu, reading control, double-tap); English-33, 31 moves by reading-control actions to "Perfect!" with 1 peg (`tb10-m1..30`, `b4-e33-results.png`) |
| TB-11 TalkBack off | PASS | finger select + jump (2,4)→(4,4) works (`tb11-01-finger-jump.png`). `input keyevent KEYCODE_TAB` ×5 moves through the chrome and dock (Undo at Tab 2, Restart at Tab 5), never a cell or the board stop (`tb11-tab1..5.png`) |
| TB-9 "no wrong/invalid/error" | PASS | 0 of 822 utterances |
| TB-9 UMP over step 1 (fresh install) | Recorded, not judged | 2 of 2 fresh installs: the consent form opens about 3 s in, before the step-1 line is spoken, and the line is not spoken again afterwards (`r1-00-open`, `r2-ump`). This is the routed 3.0.1 consent-timing item. |

## Observations (not counted in the verdict)
- **O-1.** On the English-33 Results (reached by an action), TalkBack's own first focus is the headline "Perfect!" (0.75 s), immediately followed by the summary "Perfect! — 3 of 3 stars, 1 peg left." So "Perfect!" is heard twice. On Challenge 1 (reached by a double-tap), the first focus was "Navigate back" after the summary. D-TBc's "first line the app speaks" is met in both cases. Routed to the ux-designer for awareness.
- **O-2.** The banner's region re-announces "No moves left — undo or restart" on every rotation and on `wm size` (the known 3.0.1 item from hint-undecided).
- **O-3.** B-8's landing moved from the timer (session 2) to "Navigate back", because B-3 put the back button first in the order.
- **O-4.** Driving fault: in landscape, my console swipes reached TalkBack rotated, which changed TalkBack's reading control to Characters, then to Speech rate. I cycled it back to Actions with up-then-down gestures. Nothing was activated under Speech rate, so the TTS rate is unchanged.

## Session changes on ergoz_studio and their restoration
| Change | Restored |
|---|---|
| TalkBack on (Settings UI) | Off through the Settings UI ("Turn off"). `enabled_accessibility_services` empty, `touch_exploration_enabled` 0 |
| TalkBack POST_NOTIFICATIONS prompt answered "Don't allow" ×2 | Not restorable by setting: now granted=false with USER_SET\|USER_FIXED. Recorded |
| Dark theme on (Display & touch toggle), then off | `ui_night_mode` 1 as found, "Night mode: no" |
| `adb emu rotate` (auto-rotate setting untouched) | ROTATION_0 |
| `wm size 1440x1900` | `wm size reset`. Physical size 1080x2424 |
| App installed, `pm clear` ×2, played | Uninstalled. `pm list packages` has no peg package |
| TalkBack reading control (Actions/Characters/Speech rate cycling) | Left on Actions. TalkBack-internal |
`initial-state.txt` and `final-state.txt` are identical (`diff` empty). Shutdown: `adb shell sync`, then `adb -s emulator-5580 emu kill`. The quick-boot snapshot was neither loaded nor saved.

## Checks
- `git worktree add --detach <scratchpad>/qa-tb-build 84d4a4f` — pass. Removed with `git worktree remove --force` after the session.
- `./gradlew :app:assembleDebug --max-workers=4 --console=plain -q` (in the scratch worktree) — pass (exit 0; APK sha256 `11161303…a93936`)
- `./gradlew check` — **not re-run by QA.** The branch's Check at `402e5af` is green on record (`android-game-dev-merge-home.md`: `./scripts/check.sh` BUILD SUCCESSFUL in 11m 9s, exit 0), and `84d4a4f` adds only `.planning/`. The code-auditor runs its own.
- `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass (boot_completed=1 after 20 s)
- `adb -s emulator-5580 install -r app-debug.apk` — pass. `adb -s emulator-5580 uninstall com.erman.pegsolitarie` — pass
- `adb -s emulator-5580 shell dumpsys accessibility` — TalkBack bound, `Crashed services:{}` (start and end)
- `adb -s emulator-5580 logcat -d -b crash` — system_server death at 03:41:55 (instrument fault, above). No app crash
- `adb -s emulator-5580 logcat -d --pid=9555 | tail -n 500` — 500 lines, 0 FATAL/AndroidRuntime/Exception (only GoogleMobileAds/MediaCodec E lines from the test ad SDK)
- `adb -s emulator-5580 shell sync; adb -s emulator-5580 emu kill` — pass (`adb devices` empty)

Log excerpt (`logcat.txt`, app pid 9555):
```
11-01 04:42:48.526  9555  9865 E GoogleMobileAds: GMA(BG) 1: Refused to get unsafe header "X-Afma-Gcache-IsGcacheHit" ...
11-01 04:42:48.682  9555  9665 E MediaCodec: Media Quality Service not found.
11-01 04:42:48.690  9555 13773 E an.pegsolitarie: Failed to query component interface for required system resources: 6
```

## Artifacts (`/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/qa/talkback-board/session3/`)
| Path | Size | What it shows |
|---|---|---|
| `speech-raw.log` | 3.2 MB | TalkBack verbose lines: every gesture, utterance start, completion (true = done, false = CUT), queue entries |
| `speech-log.txt` | 85 KB | 822 utterances, one per line: time, id (`#1` = after TalkBack's restart), done/CUT, text, event |
| `logcat.txt` | 141 KB | app-pid logcat, last 500 lines; no crash |
| `initial-state.txt`, `final-state.txt` | 620 B each | settings, rotation, size, night mode, packages, TTS/TalkBack versions; identical |
| `tts-dumpsys-start.txt`, `emulator-boot.log`, `build-debug.txt` | ≤9 KB | TalkBack bound, no crashed service; boot; build output |
| `setup-*.png`, `tts-01-home-swipe.png`, `99-final.png`, `off-*.png` | 9 files | TalkBack on, TTS check, TalkBack off, final launcher |
| `t1-*.png` | 5 files | the attempt before the instrument fault (not counted) |
| `r1-*.png`, `r2-*.png`, `m-*.png`, `r4-*.png`, `op1..3-*.png`, `26b-tut-*.png`, `tb1-tut-*.png` | about 80 files | tutorial runs 1 (light), 2 (dark), 3 (dark, menu route), extra step-change samples, three clean openings, tutorial 26b |
| `26b-c1-dark-*`, `26b-e33-light-*`, `a26-*` | 16 files | gameplay 26b dark/light; the A2.6 wide portrait window with the banner |
| `b4-*`, `b5-*`, `b7-*`, `b8-*`, `b9-*`, `tb4-*`, `tb6-*`, `tb7-*`, `tb10-*`, `tb11-*`, `l12-*`, `rc-*` | about 90 files | per-defect and smoke captures named by item |
| `tools/` | 19 helper scripts | `tb.py`, `t` (console gestures), `cap.sh`, `quiet.sh`, `speech.py`, `shot.sh`, `state.sh`, `tc.sh`/`gc.sh` (cell touch), `tut.sh`, `ump.sh`, `menu.sh`, `b8.sh`, `b9.sh`, `play.sh`, `g.sh`, `plan33.py`, `solve33.py`, `strand.py`. Session helpers, never committed |
| `plan33.txt`, `solution33.txt`, `planstrand.txt`, `strand.txt`, `planc1.txt` | <4 KB | move plans copied from session 2 |

## Open issues / risks
1. **B-1 residual is a race.** Its rate depends on TTS speed: about 7 s for a jump line on this emulator. The menu route loses the step line every time here. A phone pass may show it less often, but the loss itself is reproducible.
2. **B-8** lands on "Navigate back" 3 of 3. The ruling's §8 fallback applies: the ux-designer amends TB-7's row for a dialog-confirmed banner Restart to TalkBack's placement, and no code is written.
3. **A2.4's fallback commit is owed** to the developer (3 of 3 openings).
4. **B-5:** the menu route sits at the D-TBb floor on gameplay. F4's wait did not reach the destination there, so the floor ships, as D-TBb records.
5. **One emulator image (API 36, TalkBack 16.0) under heavy host load, with one system_server restart.** Timing-dependent results (B-1 residual, B-5) need the release's phone sitting, as the ruling already plans.
6. L1-2 stays BLOCKED (platform limit accepted by the code-auditor).
7. TalkBack's notification permission on ergoz_studio now reads USER_FIXED (denied). This is harmless, but a later session will not see the prompt.

## Handoff
Next: tech-lead (through the producer). VERDICT: FAIL, so the branch does not merge yet.
- **B-1 residual** (Major): the developer needs a fix so that a step's line survives a jump line that TalkBack is already speaking, on the menu route and by double-tap.
- **A2.4's pre-ruled fallback**: one developer commit, so that the first step's opening line speaks the caption alone.
- **B-8**: the pre-ruled fallback, a ux-designer amendment of TB-7's dialog-confirmed banner Restart row. No code.
- After those, a narrow device re-check: tutorial step lines on the double-tap and menu routes (×3 each), and the step-1 opening (×3).
- Observations O-1 to O-4 go to the producer for routing.

VERDICT: FAIL
