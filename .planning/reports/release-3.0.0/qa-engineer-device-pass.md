# qa-engineer report — release-3.0.0: device pass, the items runnable now
Date: 2026-11-01
Branch: release/3.0.0 (report only; built at `a7c52ab`, worktree `/Users/ermanergoz/Desktop/PegSolitarie-release`)
Skills loaded: ergoz-soft:standards (role qa-engineer; game; android-compose-game). Rules read in full, from the plugin copy because the snapshot `.claude/rules/VERSION` is 0.6.2 and the plugin is 1.0.0: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, testing. logging, puzzle-design and play-store are identical in both copies, and the snapshot copies were loaded through CLAUDE.md. Lessons from `.claude/rules/lessons.md`: none active. Skills: android-emulator-skill, android-testing.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort high (policy). This dispatch ran on the harness's model. The agent file pins fable at high (see Open issues 6).
Loops: 0
Lessons applied: none

## Summary
- Ran on emulator `ergoz_studio` (API 36, TalkBack 16) with the **debug** build of `a7c52ab`. The release AAB is founder-blocked (READINESS 4.3), so D-1 was not run and bundletool was not needed.
- **PASS:** D-4 (form-cue basis), D-5, D-6, D-8, plus one smoke.
- **D-16:** a draft set was captured.
- **FAIL: 2 items.**
  - **D-13:** after a double-tap on Hint, TalkBack focus left Hint for "Navigate back" in 3 of 4 raises. The notice's speech, its order and its Dismiss all passed.
  - **D-15:** with Remove animations on, a cold start took 4.47 s and 4.61 s, with two Davey frames of about 2 s. With animations on it took 0.84 to 1.08 s, the saved state the same.
- **Spec contradiction, D-14:** the banner has no Dismiss action, in code or in spec, so it cannot have an actions-menu Dismiss.
- **D-7 is BLOCKED:** no published 2.0 build exists on this host. Only the 3.0.0 half was captured.
- **VERDICT: FAIL.**

## Outputs
- `.planning/reports/release-3.0.0/qa-engineer-device-pass.md` — this report, the only committed file.
- `.planning/qa/release-3.0.0/` — the session artifacts, git-ignored (Artifacts section below). It also holds the release-manager's `deps.txt`, `coords.txt`, `check.log` and `old.txt`, which I did not touch.

## Build and deviation
- READINESS §3 names the release AAB only for D-1, through bundletool. D-1 waits for 4.3 and 4.6, which are founder-blocked: no `PLAY_UPLOAD_*` variable is set. The release-manager's handoff runs D-4..D-8 and D-13..D-16 on a debug build. I used `./gradlew :app:assembleDebug`.
- **What this build is:**
  - `versionName` 3.0.0, `versionCode` 30000, `minSdk` 24, `targetSdk` 36.
  - It carries Google's test ad IDs: the UMP form read "Welcome to Publisher Test Ads".
  - It is not minified, so it is not the store copy. No ad was shown or tapped.
- bundletool was not installed. No `brew install` was run, because no runnable item needs it.

## Per-item results
| # | Item | Result | Evidence (paths under `.planning/qa/release-3.0.0/`) |
|---|---|---|---|
| D-1 | Palette + TalkBack 2-minute check on the release AAB | **BLOCKED (founder)** | It needs the AAB (4.3: F-7/F-8) and its sha256 (4.6). Not run |
| D-2 | Tutorial step lines, both routes | **UNVERIFIED (no phone)** | No phone attached. READINESS: "else device3 stands" (`talkback-board/qa-engineer-device3.md`, emulator) |
| D-3 | L1-2 hardware keyboard with TalkBack | **UNVERIFIED (no phone)** | Needs a phone and a keyboard; the Q-PUBLISH untested line applies |
| D-4 | CV-AC14, system colour correction | **PASS, on form cues; not judged by eye** | See D-4 below |
| D-5 | 23e device half, the off-book notice after a live Remove-animations switch | **PASS** | See D-5 below |
| D-6 | One tap on the real launcher icon with a game resident | **PASS** | See D-6 below |
| D-7 | English-33 landscape, published 2.0 against 3.0.0 | **BLOCKED (no 2.0 build)** | 3.0.0 half only: `d7-e33-landscape-3.0.0.png` shows the board clipped top and bottom (rows 1–2 and 6–7 cut, the dock overlapping), as observation 2 said. There is no 2.0 APK on this host, and the 2.0 source is the old repository, not this codebase. The comparison needs the Play Store 2.0 on a device (the founder's phone, or the producer provides the build) |
| D-8 | Settings rows 0–13 present, ordered, TalkBack-reachable; Licenses opens offline over the backdrop | **PASS** | See D-8 below |
| D-9 | Audio Q-010 branches D2–D4 | **UNVERIFIED (no phone)** | Needs a phone and a wired headset (Q-025) |
| D-10 | Cold start ≤ 2 s on a mid-range phone | **UNVERIFIED (no phone)** | Emulator figures only. See D-15 for the animations-off start |
| D-11 | compose-performance-audit, backdrop | closed earlier (J-4a/J-4b) | Not owed, as READINESS says |
| D-12 | §5 store-copy items 5.0–5.10 | **BLOCKED (founder; after upload)** | Not run |
| D-13 | §16 #30, the notice half with TalkBack | **FAIL (one bullet of 23j item 5)** | See D-13 below |
| D-14 | §16 #30, the banner's actions-menu Dismiss | **SPEC CONTRADICTION (no product defect)** | See D-14 below |
| D-15 | Candy on this build | **FAIL (the stall clause, animations off)**; the other clauses PASS | See D-15 below |
| D-16 | Store screenshots recaptured after candy | **PASS as a draft set** | 9 captures in `store/`. See D-16 below |
| Smoke | Install, UMP, tutorial skip, Play Games "Not now", Home, a Classic win, Results, catalogue, Settings, Licenses, Challenge, dark theme | **PASS** | `grep -cE "FATAL EXCEPTION|ANR in com.erman" speech-raw.log` → 0; `logcat.txt` (the app pid, last 500 lines) has no E or F line |

### D-4 — CV-AC14
- **Settings used:**
  - The palette was switched on in the app (Settings switch `checked="true"`).
  - The scenario was English-33 with Show-moves on: a revealed book hint (`d4-base-hint.png`), then a selected peg with its legal-target disc (`d4-base-select.png`).
  - System Accessibility › Color correction was switched on through the Settings UI, in each mode in turn, with the game captured each time:
    - Deuteranomaly, the default, written as 12 at the end (`d4-cc-deuteranomaly.png`);
    - Protanomaly, `daltonizer=11` (`d4-cc-protanomaly.png`);
    - Tritanomaly, 13 (`d4-cc-tritanomaly.png`);
    - Grayscale, 0 (`d4-cc-grayscale.png`).
  - The game ran in every mode, with its selection kept and no crash.
- **The captures cannot show the transform.** The mean RGB of a peg, the target disc and the dock is identical with correction on and off: peg 242/119/181, target 124/58/237, dock 153/101/241. This is the same capture limit CV-AC11 met (`colour-vision/qa-engineer.md`).
- **The verdict rests on form.** Every distinction §3.4 makes is a form cue: the solid ring with its halo against the dashed hint rings, the opaque disc against the ring. Colour correction is a whole-screen per-pixel colour matrix and moves no shape. The luminance-only case is CV-AC07's JVM row.
- **Not judged by eye on the panel.** I cannot see the emulator window, so this item stands on form cues, not on an eye judgement.

### D-5 — 23e device half
- **Set-up:**
  - Wiegleb-45, walk A by hand (`d5-01-walkA-done.png`: holes (0,4), (1,4), (3,0), (3,1), (3,3), 40 pegs).
  - The app stayed resident while Accessibility › Remove animations was switched on in the Settings UI at 12:56:59. All three global scales read 0.0.
  - I returned through the launcher icon and tapped Hint 20 s after the switch, at 12:57:17.6, recording the screen (`d5-hint-anim-off.mp4`, 311 KB).
- **Frame times** (from `ffprobe`, `frames-d5/sheet.png`):
  - The tap lands at 0.61 s and the spinner runs from 0.61 to 2.13 s (1.52 s).
  - The notice is complete in its first frame, at 2.127 s. It still shows at 6.276 s.
  - It is gone in the next frame, 7.156 s, with no partial frame between.
- **Result:** it stays **5.03 s** and leaves in one frame. Text: "Hint couldn't work this position out in time. It may still be winnable." It was Undecided, never "No winning move".

### D-6 — the real launcher icon
- **A first attempt was invalid, and is recorded:**
  - The task's root had been started by my `am start -n` without the LAUNCHER category, for the D-15 timing. That is the confound the RM sign-off itself describes.
  - The app-drawer tap stacked a second `MainActivity` (task `sz=2`, `d6-04-after-icon-tap.png`).
  - It is not a product result, because its precondition did not hold.
- **The valid run:**
  - I force-stopped the app (I installed it), launched it from the app-drawer icon, and continued Classic English-33 to 29 pegs (`d6-05-game-resident.png`).
  - Then Home, the app drawer, and one tap on the Peg Solitaire icon. The game came back at 29 pegs, task `#82 sz=1` (`d6-06-after-real-icon-tap.png`).
- **No start-path defect.**

### D-8 — Settings rows, TalkBack, Licenses
- **The rendered order** (`d8-01-settings-top.png`, `d8-02-settings-bottom.png`): General › How to play, Sound, Haptics, Colorblind palette | Play Games › Sign in | About › Licenses | Privacy › Privacy policy, Manage privacy choices. That is the 0–13 renumbering of `docs/ux/colour-vision.md` §3.1 / LS §2, with Privacy last.
- **TalkBack:** a forward swipe from "Navigate back" reaches every header and row in that order. Each heading is spoken as "Heading". Sound, Haptics and Colorblind palette are spoken as "On|Off, …, Switch". It continues with "Licenses | Fonts, sounds and open-source software", and ends "Manage privacy choices | Change your ad consent choices" (speech log, `d8-12-settings-tb-end.png`).
- **Licenses:**
  - It opens over the sparse static wash that `docs/ux/candy-backdrop.md` §1 gives Licences (`d15-licenses.png`). The Fredoka text opens (`d15-licence-text-fredoka.png`).
  - **"Offline" was not tested with the radio off:** radio toggles are PROHIBITED on the shared device.
  - The code reads only bundled assets (`data/licenses/GoogleNoticeAssets.kt`: `licenses/google/…`; `Routes.kt:172,186`: only `AssetManager.open`).

### D-13 — the notice with TalkBack (23j item 5, 23d)
Wiegleb-45 walk A, TalkBack on, the Hint stop double-tapped. The speech times below are from TalkBack's own log.

| 23j item 5 / 23d | Result |
|---|---|
| The notice spoken once, never "No winning move" (23d) | **PASS.** 13:07:56.1 "Hint couldn't work this position out in time. It may still be winnable." was spoken once (done), 3.0 s after the double-tap. In 4 raises "No winning move" was never spoken |
| "Double-tapping Hint leaves focus on Hint" | **FAIL, 3 of 4 raises.** 1.1 s after the double-tap, TalkBack focus moved to "Navigate back \| Button" (13:07:54.2, 13:09:33.4 and 13:12:52.5; the fourth raise, 13:14:34, kept it). On screen: `d13-03-notice.png`, with the green focus box on the back arrow while the notice shows. No uiautomator call ran between the double-tap and the jump in those three raises. (A dump does reset TalkBack's focus, as it did at 13:17:36 and 13:19:06, which are not counted.) Likely cause, for the developer to confirm: the Hint node changes or is disabled while Working, and TalkBack drops focus to the first stop |
| Forward order: notice, Undo, Hint, Show-moves, Restart | **PASS.** 13:09:34.6 the notice → 35.4 "Undo" → 36.3 "Hint \| Button" → 37.2 "On \| Show possible moves" → 38.1 "Restart Board". The notice is one stop, with no "Button". Going backward after the notice had timed out, Undo goes to "Row 9, column 6", the last cell |
| Its actions menu offers Dismiss, which puts it away | **PASS.** With Time to take action set to 1 minute, so the notice could not time out: the notice was raised at 13:14:37.4, then TalkBack menu → "Actions" → "Dismiss". Dismiss was activated at 13:14:50.0, at 12.6 s of the 60 s. The tree then held 0 notice nodes (`d13-22-after-dismiss.png`). Focus then rests on "Navigate back" as the menu window closes (the D-TBd pattern) |

The item fails on one bullet. Severity is for the ux-designer and tech-lead. I would rate it Minor to Major for a TalkBack player: after asking for a hint, they lose their place. Not re-run.

### D-14 — the banner's actions-menu Dismiss
- **Set-up:** AC-19's English-33 dead position, played with TalkBack jump actions: (1,3)→(3,3), (4,3)→(2,3), (3,1)→(3,3), (2,3)→(4,3), 28 pegs (`d14-02-ac19-position.png`).
- **The banner:** Hint spoke "No winning move — undo or restart" at 13:18:56.7 (23k).
- **The menu on the banner:** with focus on its message, the TalkBack menu opened at "Describe image". It has no "Actions" entry (`d14-04-banner-menu.png`). The notice's menu, by contrast, opens at "Actions".
- **Why there is no defect:**
  - `ui/game/NoWinBanner.kt` gives the message only `liveRegion = Polite`, and no dismiss or custom action.
  - `docs/ux-spec.md` gives the banner no dismiss: 23k says "look, copy, actions and behaviour otherwise unchanged", and its actions are Undo, Restart and Home.
- **The cause is the wording.** The "actions-menu (Dismiss)" in §16 #30 and in READINESS D-14 matches 23j item 5's notice Dismiss, which passed under D-13. For the tech-lead: correct the D-14 / §16 #30 wording. If a banner Dismiss is actually wanted, that is new design for the ux-designer.

### D-15 — candy on this build
| Clause | Result |
|---|---|
| Backdrop behind Home, gameplay and Licenses | **PASS.** Waves and lights on Home (`04-home.png`) and gameplay (`store/02…`, `store/07…`); the sparse wash on Licenses (`d15-licenses.png`), as the spec says |
| Fades in after Home's entrance | **PASS.** Mean RGB of the band y 2150–2300 in `d15-cold-anim-on.mp4`: first paint ramps 1.81→1.94 s, holds until 2.08 s, then ramps 2.08→2.77 s (227/197/239 → 208/142/231) and stays. The second ramp is the 700 ms fade (measured 0.69 s), about 0.27 s after Home's first paint |
| Static with animations off, no fade | **PASS for the fade.** In `d15-cold-anim-off-2.mp4` the scene arrives as one step, 228/197/240 → 208/140/231 between 5.374 and 5.558 s, 0.57 s after first paint. No ramp. I cannot name it "frame 3": screenrecord writes only changed frames |
| No visible stall beyond the recorded one | **FAIL with animations off.** Details below |

**The animations-off cold start:**
- Two cold starts with Remove animations on: `ActivityTaskManager: Displayed … +4s607ms` (12:57:55.954) and `+4s468ms` (12:58:27.716, `am start -W` TotalTime 4468). Both have two Davey frames of about 2 s each (2048/1952 ms; 2066/1979 ms) and "Skipped 111/113 frames".
- **The control:** the same saved state and the same launcher intent with animations on gave TotalTime **837 ms** at 12:59:32. The earlier on-runs were +1080, +997 and +876 ms, with no Davey.
- **Host:** quiet, load 2.77 at 12:59, and only this emulator running.
- **Tension with the record:** candy's 15e sitting on `Pixel_9a_bench` ran with the scales at 0.0 (`candy-backdrop/qa-engineer-15e.md:193`) and recorded about 455 ms TTID. The tech-lead should weigh that against this result.
- **Scope:** one emulator and a debug APK. Not re-run.
- **Logcat excerpt** (`speech-raw.log`, ≤ 20 lines):
```
11-01 12:57:55.954 I/ActivityTaskManager(  669): Displayed com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity for user 0: +4s607ms
11-01 12:57:55.992 I/HWUI    ( 8404): Davey! duration=2048ms; Flags=1, ...
11-01 12:57:55.998 I/Choreographer( 8404): Skipped 111 frames!  The application may be doing too much work on its main thread.
11-01 12:57:56.120 I/HWUI    ( 8404): Davey! duration=1952ms; Flags=0, ...
11-01 12:58:27.716 I/ActivityTaskManager(  669): Displayed com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity for user 0: +4s468ms
11-01 12:58:27.730 I/HWUI    ( 8725): Davey! duration=2066ms; Flags=1, ...
11-01 12:58:27.748 I/Choreographer( 8725): Skipped 113 frames!  The application may be doing too much work on its main thread.
11-01 12:58:27.865 I/HWUI    ( 8725): Davey! duration=1979ms; Flags=0, ...
11-01 12:59:32.185 I/ActivityTaskManager(  669): Displayed com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity for user 0: +837ms
```

### D-16 — store screenshots
- **What was captured:** plain captures from the emulator, 1080×2424, en-US, no overlay. Kept under `.planning/qa/release-3.0.0/store/`:
  1. `01-home.png`, light, with the backdrop;
  2. `02-classic-e33-midgame.png`, a selected peg with its target and the dock;
  3. `03-challenge-hint.png`, Level 1 with a hint;
  4. `04-results.png`, three stars, 7200, 01:30;
  5. `05-catalogue.png`, the pips;
  6. `06-tutorial.png`, step 1;
  7. `07-gameplay-dark.png`;
  8. `08-palette-on-selection.png` and `08b-palette-on-hint.png`.
- **Why they are not committed:** `docs/store/README.md` asks for captures "of the 3.0.0 release candidate on a phone". `scripts/play-upload.sh` uploads no images, because the founder enters them in the Console. There is no repository path for them. These come from a debug build on an emulator, with the emulator's status bar.
- **They are a draft.** The final set comes from the RC install, the store copy of §5.
- **No tr-TR set.** Changing the device locale was not in this session's scope.

## Device and system changes (all restored or noted)
| Change | Restored |
|---|---|
| App installed (it was absent at the start) | Uninstalled (`adb uninstall` → Success) |
| Remove animations on (Settings UI) | Off through the UI. All three scales are 1.0 again |
| Colour correction on, four modes (Settings UI) | Off through the UI. It reads `enabled=0`, `daltonizer=12` where it was `null`/`null`: the platform's own off state and default mode, the same effect. Not set back to null, because `settings delete/put secure` is PROHIBITED |
| Dark theme on (Settings UI) | Off. `Night mode: no` |
| Time to take action 1 minute (Settings UI) | "Default". It reads `0/0` where it was `null/null`: the same effect |
| TalkBack on (Settings UI, Allow) | "Turn off" through the UI. `enabled_accessibility_services` empty, `accessibility_enabled` 0, `touch_exploration_enabled` 0, `Bound services:{}`, `Crashed services:{}` |
| Rotation (`adb emu rotate`, the console's sensor) | Rotated back to ROTATION_0. `user_rotation` 0 and `accelerometer_rotation` 1 are unchanged |
| **Incident:** an extra double-tap in the TalkBack menu activated "Describe image", and the next one opened a link in its "Get detailed image descriptions?" dialog. That started Chrome's FirstRunActivity | Backed out with Back, with nothing accepted in TalkBack or Chrome. The emulator was booted `-no-snapshot-save`, so the Chrome task did not survive the shutdown. Chrome's first-run state may have files of its own on `/data`: not checked, and not cleared, because clearing a package I did not install is PROHIBITED |
| `/sdcard/ui.xml` (my uiautomator dumps) | Deleted |
| Emulator: booted by this session (`-port 5580 -no-snapshot-load -no-snapshot-save`) | `adb shell sync`, then `adb emu kill`. `adb devices` is empty; no qemu for `ergoz_studio` remains |
| `Pixel_9a`, `Pixel_9`, `Pixel_9a_bench` | Not touched |

`initial-state.txt` and `final-state.txt` (taken before the uninstall) differ only in the colour-correction and timeout rows above.

## Checks
- `./gradlew :app:assembleDebug --console=plain` — pass (BUILD SUCCESSFUL in 4s; `app-debug.apk` 22,240,692 B).
- `emulator -avd ergoz_studio -port 5580 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass (`sys.boot_completed=1`; fingerprint `google/sdk_gphone64_arm64/emu64a:16/BP22.250325.006/13344233`).
- `adb -s emulator-5580 install app/build/outputs/apk/debug/app-debug.apk` — pass (versionCode 30000, versionName 3.0.0).
- `adb … shell am start -W …` — Anim on: COLD, TotalTime 997 / 837 ms. Anim off: TotalTime 4468 ms (D-15 FAIL).
- `ffprobe` / `tools/series.py` on `d5-hint-anim-off.mp4` — pass (notice 2.127 → gone 7.156 s, no partial frame).
- `grep -cE "FATAL EXCEPTION|ANR in com.erman" speech-raw.log` — pass (0).
- `adb … logcat -d --pid=$(pidof com.erman.pegsolitarie) | tail -n 500 > logcat.txt` — pass (63 lines, no E/F line).
- `diff initial-state.txt final-state.txt` — the two equivalent-default rows listed above.
- Not run: bundletool (D-1 blocked), the Check command (the release Check was the release-manager's, `release-manager-cut.md`; this pass changes no code). `.planning/log/` was not read.

## Artifacts (`.planning/qa/release-3.0.0/`, git-ignored, 67 MB with the release-manager's files)
| Path | Size | What it shows |
|---|---|---|
| `speech-raw.log` | 32.0 MB | the full `logcat -v time` for the session: TalkBack's "Speaking fragment" lines, Displayed/Davey lines |
| `logcat.txt` | 9.1 KB | the app pid's last 500 lines at the end of the session |
| `initial-state.txt`, `final-state.txt` | 602 B, 699 B | device settings before and after |
| `build-debug.log`, `emulator.log` | 3.8 KB, 9.5 KB | the build and the emulator boot |
| `d15-cold-anim-on.mp4`, `frames-on/` | 7.0 MB | anim-on cold start, the 0.69 s scene fade |
| `d15-cold-anim-off.mp4`, `d15-cold-anim-off-2.mp4`, `frames-off/` | 318 KB, 759 KB | anim-off cold starts: about 4.3 s of splash, then the scene as a step |
| `d5-hint-anim-off.mp4`, `frames-d5/` | 311 KB | D-5: the spinner for 1.52 s, the notice for 5.03 s, a one-frame exit |
| `d4-*.png` | 98–393 KB each | D-4 scenario and the four correction modes |
| `d6-01…06-*.png` | 346 KB–1.4 MB | D-6, both attempts |
| `d7-e33-landscape-3.0.0.png` | 173 KB | D-7, the 3.0.0 half: board clipped in landscape |
| `d8-01/02/11/12-*.png` | 203–222 KB | Settings order, light, TalkBack focus |
| `d13-*.png` | 151–347 KB | D-13: the focus box on "Navigate back" (`d13-03`), the menu "Actions" → "Dismiss" (`d13-21`), after the Dismiss (`d13-22`) |
| `d14-*.png` | 250–396 KB | D-14: the AC-19 position, the banner, its TalkBack menu with no Actions entry |
| `d15-licenses.png`, `d15-licence-text-fredoka.png` | 265 KB, 475 KB | Licenses over its wash, and a licence text |
| `store/*.png` | 112–392 KB | D-16 draft set (9 files) |
| `tools/` | — | session helpers, never committed: talkback-board session 4's TalkBack driver (`tb.py`, `t`, `g.sh`, `play.sh`, `cap.sh`, `speech.py`, `menu.sh`, `plan33.py`, `solve33.py`), and new ones: `ui.py` (uiautomator tap and list), `mv.sh` (plain-tap jumps), `series.py` (per-frame region RGB), `px.sh`, and `quiet.sh` rewritten to count speech lines only |
| `e33-solution.txt`, `ac19*.txt`, `d5-t0.txt` | < 1 KB | move lists, the switch time |

## Open issues / risks
1. **D-13 FAIL (23j item 5, bullet 1).**
   - After a double-tap on Hint, TalkBack focus left Hint for "Navigate back" in 3 of 4 raises.
   - It is new: none of the talkback-board or hint-undecided reports records it.
   - For the tech-lead and the ux-designer: decide severity and whether it blocks the 3.0.0 merge to main or is a disclosed 3.0.1 item.
2. **D-15 FAIL (the animations-off cold start).**
   - The measurements: 4.47 and 4.61 s, with two Davey frames of about 2 s, against 0.84–1.08 s with animations on, on the same emulator and the same saved state.
   - It conflicts with candy's bench at scale 0 (about 455 ms).
   - For the tech-lead: whether it reproduces on the bench or a phone, and whether it bears on PLAT-08 / D-03j for reduced-motion players.
   - Emulator and debug APK only.
3. **D-14 is a spec contradiction.** No banner Dismiss exists or is specified. The wording of READINESS D-14 and of tech-design §16 #30 ("the banner's actions-menu Dismiss") is the tech-lead's to correct. The notice's Dismiss passed under D-13.
4. **Still owed:**
   - D-1 (the AAB and its hash; founder 4.1–4.3, 4.6);
   - D-7 (a published 2.0 build: the founder's phone, or the producer sources the APK);
   - D-12 (after upload);
   - D-2, D-3, D-9, D-10 (no phone; the Q-PUBLISH unverified lines);
   - D-16's final set from the RC.
5. **Not judged by eye:** D-4 rests on form cues, because screencap cannot show the colour correction. A phone in the room would close it by eye.
6. **Model.** The agent file pins fable at high. This dispatch ran on claude-opus-5-5. rules/studio.md (Model and effort) says a fable step does not move to another model. This pass reviews no code, and its device evidence stands on the artifacts. Whether it counts is the producer's to rule.
7. **Device leftovers.**
   - Colour correction reads 0/12 and the timeout 0/0 where both were null: the same effect, the platform's own values.
   - Chrome's FirstRunActivity was opened, by accident, and not accepted.

## Handoff
Next: producer —
- **tech-lead:** D-13 (focus leaves Hint), D-15 (the animations-off start), and the wording of D-14 / §16 #30. Severity and a merge ruling for each.
- **ux-designer:** D-13's severity for a TalkBack player, through the producer.
- **founder (Q-PUBLISH / F-items):** D-1, D-7's 2.0 comparison, D-12, and the phone lines D-2, D-3, D-9, D-10.
- **qa-engineer, later:** D-1 on the AAB once 4.3 and 4.6 exist; D-16's final captures from the RC.

VERDICT: FAIL
