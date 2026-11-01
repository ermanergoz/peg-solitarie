# qa-engineer report — talkback-board (device-3 narrow re-check, tech-lead device-2 ruling §5)
Date: 2026-11-01
Branch: feature/talkback-board (report only; built at `fc9b6c3`, the last code commit; the tip `fb31018` adds only `.planning/reports/`)
Skills loaded: ergoz-soft:standards (role qa-engineer; product snapshot 0.6.2 < plugin 1.0.0, so the plugin copies of studio, reporting, testing, gitflow, skills-policy, memory, kotlin-android and compose were read in full; logging, ip-policy, puzzle-design and play-store are identical to the snapshot; the product's .claude/rules/lessons.md), android-emulator-skill, android-testing
Ran in parallel with: code-auditor loop 5 (talkback-board, same worktree; I touched only this report there)
Ran on: claude-opus-5-5 at effort high. Staffing deviation: agents/qa-engineer.md pins fable. Founder answer Q-019 (option 3) keeps Fable-pinned roles on Opus until 3.0.0. Every verdict below rests on a device artifact.
Fan-out: none
Loops: 0 (third device pass on this branch; follows `qa-engineer-device2.md`)
Lessons applied: none (the snapshot holds 0 active lessons; .planning/recall/RECALL.md is absent)

## Summary
- Scope is the binding TB-9 "Device-3 pass line" (`docs/ux-spec.md`), read with D-TBe, D-TBb item 3 and D-TBd. Emulator `ergoz_studio` only, cold boot, default TalkBack speech rate, TalkBack 16.0.0.738667889, Google TTS 20241125.02_p2.
- **Opening ×3: PASS 3 of 3.** The caption alone, then "Step 1 of 3" once, from the pips' first focus. No app focus move.
- **Step lines ×6: PASS 6 of 6, zero lost.** On both routes: "Board, N pegs left" focused, then the step line once and in full, then nothing but the coach hint about 7 s later. The clearing jump's own line was not spoken on any route (D-TBe item 1).
- **Completion: PASS 3 of 3** (2 by double-tap, 1 through the menu). The done line, then "Got it".
- B-8 not re-read (ruling §5 item 5, D-TBd). Gameplay and the earlier PASS rows were not re-run. No run failed, so no run was repeated. **VERDICT: PASS.**

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/qa-engineer-device3.md` — this report. It is committed on the branch alone.
- `.planning/qa/talkback-board/session4/` — this session's artifacts (git-ignored, 18 MB, 66 entries; index under Artifacts). Nothing is committed.

## Build, device and instrument
- Build: `git worktree add --detach <scratchpad>/qa-tb3-build fc9b6c3`, `local.properties` copied in, then `./gradlew :app:assembleDebug`. Host 1-minute load was 3.23 when the build started (below 12). The worktree was removed afterwards. `app-debug.apk` is 21,394,921 B, sha256 `f75152781267dcccc393a09301a218fa620e6921dda91b4f299fe425519dae65`.
- **On the device:** `sha256sum` of the installed `base.apk` printed the same hash, `f75152781267…19dae65`.
- `fc9b6c3..fb31018` touches only `.planning/reports/talkback-board/` (the developer's and code-auditor's reports), so this APK is the branch tip's code.
- Device: `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim`. API 36, 1080x2424 at 420 dpi, light theme. `tts_default_rate` unset, so TalkBack ran at its default rate. TalkBack's own speech rate was not touched.
- Not touched: Pixel_9, Pixel_9a, Pixel_9a_bench, and any attached phone.
- Set-up with TalkBack off: launch, UMP form "Do not consent", tutorial "Skip tutorial", Play Games "Not now", app Settings. Then TalkBack on through Settings › Accessibility › TalkBack › Use TalkBack › Allow. Doing the set-up first kept the UMP form from landing on step 1, which is the routed 3.0.1 consent-timing item.
- TTS precondition: TalkBack spoke "TalkBack" and then "Navigate up | Button" as soon as it came on, each completed (`tts-01-talkback-on.png`). `dumpsys accessibility` showed TalkBack bound with `Crashed services:{}`.
- Drive: TalkBack gestures sent as `adb emu` console `event mouse` (session 3's helpers, copied into `session4/tools/`). Speech was read from TalkBack's verbose log (`SpeechControllerImpl: Speaking fragment`, plus `Received utterance completed … returned true|false`).
- **Host load (1-minute average on 10 cores)**, recorded at each clearing jump and each opening:

| Point | Host load |
|---|---|
| build start | 3.23 |
| opening 1 | 6.91 |
| run 1 T1 (double-tap) | **21.87** (a spike from other worktrees' work; recorded, see below) |
| run 1 T2 (double-tap) | 7.38 (I waited for the load to fall below 14 first) |
| run 1 completion | 6.65 |
| opening 2 | 6.65 |
| run 2 T1 (menu) | 5.30 |
| run 2 T2 (menu) | 5.53 |
| run 2 completion (menu) | 4.27 |
| opening 3 | 3.46 |
| run 3 T1 (double-tap) | 3.75 |
| run 3 T2 (menu) | 3.22 |
| run 3 completion | 5.04 |

Run 1 T1 ran at a host load of 21.87. The spike came between my previous capture and the jump. The ruling asks for a start below 14, and the session started at 6.91. That sample **passed**. Heavier load stretches the timing F-B1 relies on, so a pass under it is the harder case. It is counted, and the spike is recorded here. The guest's load was 0.80 at the start.

## Per-run table (from `session4/speech-log.txt`)
Each step-line row is the clearing jump's `dtap` (or the menu action's activation) followed by every utterance up to the next gesture of mine. "done" means TalkBack reports the utterance completed (true); "CUT" means it reports false.

### Opening ×3 (Settings › How to play, TalkBack on)
| Opening | Caption alone, once, in full | "Step 1 of 3" | App focus move | Verdict |
|---|---|---|---|---|
| 1 (05:51:02.7) | 05:51:03.093 "Tap a peg, then tap the empty hole two cells away." done | once, 05:51:06.809, `ACCESSIBILITY_FOCUSED` (the pips) | none | PASS |
| 2 (06:03:23.1) | 06:03:23.404, same text, done | once, 06:03:27.052, pips focus | none | PASS |
| 3 (06:11:03.3) | 06:11:03.570, same text, done | once, 06:11:07.231, pips focus | none | PASS |

In all three, the next line is the step-1 coach hint, "Hint: row 1, column 1, jump right to row 1, column 3.", about 5 s after opening. It is not judged here. **3 of 3.**

### Step lines ×6
| Run / step | Route | What TalkBack said after the clearing jump, in order | Focus rests on | Verdict |
|---|---|---|---|---|
| r1 T1 | double-tap | 26.125 "Row 1, column 3, peg, no jumps" (TalkBack's re-read, CUT, not judged) → 27.069 "Board, 2 pegs left" (focus, done) → 29.115 "Step 2 of 3. Jumps work up, down, left and right — never diagonally." (done) → 36.349 hint | board stop | PASS |
| r1 T2 | double-tap | 11.719 "Row 3, column 2, peg, no jumps" (re-read, CUT) → 12.592 "Board, 5 pegs left" (focus, done) → 14.734 "Step 3 of 3. Keep jumping. Clear the board down to one peg." (done) → 20.576 hint | board stop | PASS |
| r2 T1 | menu | 12.523 "Peg Solitaire" (the menu closing, not judged) → 13.720 "Board, 2 pegs left" (focus, done) → 15.733 "Step 2 of 3. …" (done) → 22.890 hint | board stop | PASS |
| r2 T2 | menu | 12.496 "Peg Solitaire" → 13.698 "Board, 5 pegs left" (focus, done) → 15.790 "Step 3 of 3. …" (done) → 21.621 hint | board stop | PASS |
| r3 T1 | double-tap | 06.605 re-read (CUT) → 07.495 "Board, 2 pegs left" (focus, done) → 09.493 "Step 2 of 3. …" (done) → 16.634 hint | board stop | PASS |
| r3 T2 | menu | 10.189 "Peg Solitaire" → 11.392 "Board, 5 pegs left" (focus, done) → 13.460 "Step 3 of 3. …" (done) → 19.229 hint | board stop | PASS |

**6 of 6, zero lost.** In every case:
- the step line is spoken once and in full, about 2 s after the board stop's focus;
- the hint comes 5.8–7.2 s after the step line starts, never before it and never in its place;
- no focus event follows except my own next explore-touch;
- no stop is read with text from before the swap.

On the menu route the focus landed on the board stop itself, which meets D-TBb item 1's requirement, not only its floor.

The clearing jump's own line ("Row 1, column 1 to row 1, column 3. …") was spoken 0 of 6 times, as D-TBe item 1 rules. Step 3's other jumps kept their lines, 9 of 9: "… 4 pegs left.", "… 3 pegs left.", "… 2 pegs left." in each run.

The passing session-3 menu case, for contrast with session 3's failure (`qa-engineer-device2.md`, 04:04:58: jump line → window → board stop, step line lost):
```
06:04:12.523  talkback_90   done  Peg Solitaire                                                          [TYPE_WINDOWS_CHANGED]
06:04:13.720  talkback_91   done  Board, 2 pegs left                                                     [TYPE_VIEW_ACCESSIBILITY_FOCUSED]
06:04:15.733  talkback_92   done  Step 2 of 3. Jumps work up, down, left and right — never diagonally.  [TYPE_WINDOW_CONTENT_CHANGED]
06:04:22.890  talkback_94   done  Hint: row 1, column 2, jump down to row 3, column 2.                  [TYPE_WINDOW_CONTENT_CHANGED]
```

### Completion ×3
| Run | Completing jump | After it | Verdict |
|---|---|---|---|
| r1 | double-tap | 06:02:19.956 re-read (done) → 20.865 "That's the whole game. Go clear a board." (done) → 25.896 "Got it \| Button" (focus) | PASS |
| r2 | menu | 06:10:24.017 "Row 3, column 3 to row 1, column 3. Removed row 2, column 3. 1 peg left." (done) → 30.879 "Peg Solitaire" → 32.089 done line (done) → 34.573 "Got it \| Button" (focus) | PASS |
| r3 | double-tap | 06:18:22.004 re-read (done) → 22.884 done line (done) → 27.860 "Got it \| Button" (focus) | PASS |

**3 of 3.** Double-tapping "Got it" returned to Settings, with focus on "Navigate back", each time.

### Smoke inside the runs (ruling §5 item 4)
- Rejected tap on T2 (r1): double-tap on the empty (1,1) gave "No peg here." (05:55:59.460, done).
- Coach hint on T2: "Hint: row 1, column 2, jump down to row 3, column 2." is spoken on every step-2 board (r1 05:56:03.475, done).
- "wrong", "invalid" or "error": 0 of 159 utterances.
- 3 utterances are CUT in the whole session. All three are TalkBack's own re-read of the destination after a double-tap, and they are interrupted by the board stop's focus. That is TalkBack's speech, which the pass line does not judge.

## Not re-run (ruling §5 item 5)
- B-8 / D-TBd: judged on session 3's evidence ("Board restarted. 4 pegs left." then "Navigate back", 3 of 3). No new read.
- Gameplay, and every PASS row of `qa-engineer-device2.md`. The change set, `6a98510..fc9b6c3`, touches only `ui/onboarding/TutorialAnim.kt` and `ui/onboarding/TutorialAnnouncements.kt`, plus tests and docs.

## Session changes on ergoz_studio and their restoration
| Change | Restored |
|---|---|
| TalkBack on (Settings UI, "Allow") | Off through the Settings UI (Use TalkBack → "Turn off"). `enabled_accessibility_services` empty, `touch_exploration_enabled` 0, `Bound services:{}` |
| App installed, UMP declined, tutorial played 3 times | Uninstalled. `pm list packages` has no peg package |
| Nothing else | No theme, rotation, size or speech-rate change. TalkBack's notification permission prompt did not appear: it is still denied (USER_FIXED) from session 3 |

`initial-state.txt` and `final-state.txt` are identical (`diff` empty). Shutdown: `adb -s emulator-5580 shell sync`, then `adb -s emulator-5580 emu kill`. `adb devices` was then empty and no `ergoz_studio` qemu process remained. The quick-boot snapshot was neither loaded nor saved.

## Checks
- `sysctl -n vm.loadavg` — 3.23 at the build start (< 12); per-jump values in the table above.
- `git worktree add --detach <scratchpad>/qa-tb3-build fc9b6c3` + `cp local.properties` — pass. Removed with `git worktree remove --force` and `git worktree prune`; `git worktree list` no longer shows it.
- `./gradlew :app:assembleDebug --max-workers=4 --console=plain -q` (in the scratch worktree) — pass (exit 0, no output; APK sha256 `f7515278…19dae65`).
- `./gradlew check` — **not re-run by QA.** This is a narrow device re-check. The developer's loop-5 Check and the code-auditor's loop-5 review are on record (`android-game-dev-device2-fixes.md`, `code-auditor-loop5.md`).
- `git diff --name-only fc9b6c3..HEAD` — only `.planning/reports/talkback-board/android-game-dev-device2-fixes.md` and `code-auditor-loop5.md`.
- `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass (`sys.boot_completed` 1).
- `adb -s emulator-5580 install -r app-debug.apk` — pass. `adb -s emulator-5580 shell sha256sum <base.apk>` — `f75152781267dcccc393a09301a218fa620e6921dda91b4f299fe425519dae65`, matches the build.
- `adb -s emulator-5580 shell dumpsys accessibility` — start: TalkBack bound, `Crashed services:{}`. End, after turning it off: `Bound services:{}`, `Crashed services:{}`.
- `adb -s emulator-5580 logcat -d --pid=3770 | tail -n 500` — 500 lines, 0 FATAL/AndroidRuntime. `logcat -d -b crash` — empty.
- `adb -s emulator-5580 uninstall com.erman.pegsolitarie` — Success.
- `diff initial-state.txt final-state.txt` — identical.
- `adb -s emulator-5580 shell sync; adb -s emulator-5580 emu kill` — pass ("OK: killing emulator, bye bye"; `adb devices` empty).

Log excerpt (`logcat.txt`, app pid 3770; the only E lines, none from app code):
```
11-01 05:48:42.723  3770  3849 E an.pegsolitarie: Failed to query component interface for required system resources: 6
11-01 05:48:43.797  3770  3909 E MESA    : Failed to open rendernode: No such file or directory
11-01 05:50:20.535  3770  3770 E an.pegsolitarie: hiddenapi: Accessing hidden method Landroid/view/accessibility/AccessibilityNodeInfo;->getSelection()…
11-01 05:50:20.537  3770  3770 E an.pegsolitarie: hiddenapi: Accessing hidden method Landroid/view/accessibility/AccessibilityNodeInfo;->setSelection(…
```
The `hiddenapi` lines are logged when TalkBack first binds. They come from the accessibility framework's node handling, and nothing fails.

## Artifacts (`/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/qa/talkback-board/session4/`)
| Path | Size | What it shows |
|---|---|---|
| `speech-raw.log` | 10.7 MB | the full `logcat -v time` stream for the session: TalkBack's "Speaking fragment" and "utterance completed" lines, focus and event times |
| `speech-log.txt` | 17.8 KB | 159 utterances, one per line: time, id, done/CUT, text, event. The source of every table above |
| `logcat.txt` | 104 KB | app-pid logcat, last 500 lines; no crash |
| `initial-state.txt`, `final-state.txt` | 620 B each | accessibility settings, rotation, size, night mode, packages, TTS/TalkBack versions; identical |
| `tb-dumpsys-start.txt`, `tb-dumpsys-end.txt` | 5.9 KB, 3.0 KB | TalkBack bound with no crashed service; unbound at the end |
| `emulator-boot.log`, `build-debug.txt`, `load-build-start.txt` | 8.6 KB, 0 B, 19 B | boot output; the quiet build (no output, exit 0); the load at the build start |
| `setup-01..09-*.png`, `tts-01-talkback-on.png` | 10 files | the UMP form, Skip, Play Games "Not now", the app's Settings, TalkBack's switch and Allow, the TTS check, How to play focused |
| `op2..3-howto.png`, `op1..3-open-12s.png` | 5 files | How to play focused (opening 1 used `setup-09-howto.png`); each opening 12 s after the double-tap |
| `r1-*.png`, `r2-*.png`, `r3-*.png` | 34 files (12, 12, 10) | per run: selection, target, each step change (`*-T1-step2`, `*-T2-step3`), menu captures (`*-menu`), T3 jumps, completion (`*-T3-complete`), after Got it |
| `off-01..05*.png`, `off-03-dialog.png` | 6 files | TalkBack turned off through Settings ("Turn off TalkBack?" → "Turn off") |
| `tools/` | 19 helper scripts | session 3's helpers, copied unchanged (`tb.py`, `t`, `cap.sh`, `quiet.sh`, `speech.py`, `tc.sh`, `menu.sh`, `state.sh`, `shot.sh`, …). Session helpers, never committed |

## Open issues / risks
1. **One emulator image, one sitting.** The result is timing-dependent: F-B1 waits 500 ms after the focus move. It held 6 of 6 here, once under a host load of 21.87. The phone sitting before release should still repeat the 6 step lines (D-TBe, "Recorded, not hidden"; ux-designer open issue). Not tested at non-default speech rates (tech-lead open issue 3).
2. **The menu route now carries no confirmation of the jump itself**, by design (D-TBe item 1, tech-lead open issue 4). What is heard is the window, "Board, N pegs left", then the step line. This is a spec decision, not a finding.
3. L1-2 (hardware keyboard with TalkBack on) stays BLOCKED as recorded in `qa-engineer-device2.md`. It was not in this scope.
4. The `.planning/qa/talkback-board/` directory now holds session3 (62 MB) and session4 (18 MB). The producer deletes it after the Done line.

## Handoff
Next: tech-lead (through the producer), for sign-off. The tech-lead ruling's §6 fallback is **not** triggered: 6 of 6 step lines, 3 of 3 openings, 3 of 3 completions. B-1's residual and A2.4 are closed on the emulator under D-TBe. B-8 stands as D-TBd records it. Before release, the release's phone sitting repeats the 6 step lines.

VERDICT: PASS
