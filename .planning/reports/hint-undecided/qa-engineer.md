# qa-engineer report — hint-undecided (R1: Undecided is not "no winning move")
Date: 2026-10-24
Branch: bugfix/hint-undecided (report only; worktree head `53bd85f`, code head `7201742` with develop merged in)
Skills loaded: ergoz-soft:standards (role qa-engineer; the product snapshot is 0.5.1 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md` and the plugin's `memory/LESSONS.md`, both with 0 lessons), android-emulator-skill, android-testing
Ran in parallel with: none
Ran on: claude-fable-5-1 at effort high (the role's pin)
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons; no `.planning/recall/RECALL.md`)

## Summary
- The fix holds on both devices: every Hint asked off the book on Wiegleb-45 (walks A, B, C) and in the Diamond-41 gap ended **Undecided** with the D-04g notice, and "No winning move" never appeared on a winnable position. On the GM6 (Android 8.1) the Undecided rate on walks A–C was **6 of 6** Hint requests (two per walk); on the emulator it was 17 of 17 across walks A–C, the Diamond gap, AC-21, item 7 and item 5.
- AC-18a (control), AC-19, AC-20, AC-21 and 23j items 1–7 pass on the emulator; AC-19's banner and the stuck banner are both spoken by TalkBack (23k's device half).
- BLOCKED, not failed: AC-19 and AC-20 on the GM6 (its Diamond-41 board did not commit a two-tap jump in three attempts, and the phone drops to sleep under adb input — see Open issues), and the *spoken* halves of 23j item 5's traversal/Dismiss bullet and 23k's traversal: TalkBack's gestures, keyboard navigation and actions menu cannot be driven over adb on this image (as the phase-15 sessions also found). The tree order the dumps show is the spec's order in every case.
- No high-severity bug. **VERDICT: PASS WITH BLOCKED ITEMS.**

## Build and devices
- Build: `./gradlew --max-workers=4 :app:assembleDebug` in `/Users/ermanergoz/Desktop/PegSolitarie-hint` at `53bd85f` — BUILD SUCCESSFUL in 14 s; `app/build/outputs/apk/debug/app-debug.apk` sha256 `db15a4a0…0391ef` (20,994,365 bytes), `com.erman.pegsolitarie` 3.0.0, versionCode 30000, debug-signed (cert SHA-256 `8a4e64a3…1a4a6`).
- (1) Emulator **Pixel_9_clean** = `emulator-5556`, API 36, 1080×2424 @420 (411×923 dp), booted by this session and shut down at its end. `emulator-5554` (`Pixel_9a_bench`) was never touched.
- (2) Founder's phone **GM6** `PJBQV4USFAHEKBRS`, Android 8.1 / API 27, 720×1280 @320 (360×640 dp), TalkBack 16.2 and Google TTS installed. Its installed `com.erman.pegsolitarie` (3.0.0, versionCode 3, test-only debug sideload, same debug cert) was pulled first (`gm6-original-base.apk`, sha256 `dbe100e2…655589`), the build installed with `install -r` (data kept), and the original put back at the end with `install -r -d -t` — the reinstalled `base.apk` pulled back hashes `dbe100e2…` again, versionCode 3, `firstInstallTime` unchanged (2026-03-16).
- TTS engine confirmed before the TalkBack checks: Settings › Text-to-speech output shows preferred engine "Speech Recognition and Synthesis from Google", and logcat at TalkBack start: `TextToSpeech: Sucessfully bound to com.google.android.tts` / `Connected to TTS engine`.

## Method
Board cells were tapped by computed centres from the board canvas bounds (`uiautomator`: emulator `[84,733][996,1645]`, GM6 `[64,272][656,864]`); every walk's resulting position was verified by pixel at the expected empty holes before Hint was tapped. Hint timing comes from timestamped screencap frames after the tap (`hint.py`), read by a pixel probe (`probe.py`): the Hint bulb's centre is dark when the bulb is drawn and light while the spinner replaces it; the notice is detected by its dark glyph/text in its band. Text and tree order come from `uiautomator` dumps (`ui.py`). TalkBack speech was captured with its "Display speech output" overlay (screenshots), accessibility focus with the green focus frame (`focusbox.py`).

## Per-criterion results
| Criterion | Device | Result | Evidence |
|---|---|---|---|
| AC-18a control: Hint ×3 from the Wiegleb-45 start, each at once, no notice/banner | emulator | **PASS** | Hints 1–3: gold glow already in the first frame after the tap (t+20 ms), bulb never became a spinner, no notice/banner node; the glowing moves were the golden line's (2,4)→(4,4), (0,4)→(2,4), (3,6)→(3,4). A 4th Hint raised "Out of free hints … Watch a short ad for 3 more?" (allowance spent, used by AC-21). `emu-ac18a/` |
| AC-18b walk A → Hint → Undecided notice, never "No winning move" | emulator; GM6 | **PASS**; **PASS** | Emulator: spinner t+0.02–1.44 s, notice fading in at 1.58 s, text exact; three Hints on the position, all Undecided. GM6: spinner still at t+1.66 s, notice present by the next capture (4.5 s; the phone's screencap cadence is ~0.8 s), gone by 7.4 s; two Hints, both Undecided. `emu-walkA/`, `gm6-walkA/` |
| AC-18c walk B | emulator; GM6 | **PASS**; **PASS** | Emulator: spinner to 1.46 s, notice at 1.6 s, up to 6.48 s, gone 6.75 s. GM6: notice by 2.4 s, gone by 7.5 s; 2/2 Undecided. `emu-walkB/`, `gm6-walkB/` |
| AC-18d walk C | emulator; GM6 | **PASS**; **PASS** | Emulator: notice by 1.7 s, gone by 7.2 s. GM6: notice by 2.4 s, gone by 7.5 s; 2/2 Undecided. `emu-walkC/`, `gm6-walkC/` |
| AC-18 pass line: no walk showed a move, none at once | both | **PASS** | No replacement walk was needed; orientation matches the coordinates (the book answered AC-18a's golden moves at the expected cells). |
| AC-19 English-33 proven dead end → banner at once | emulator; GM6 | **PASS**; **BLOCKED** | Emulator: "No winning move — undo or restart" with Undo/Restart/Home in the tree and in the first frame (t+20 ms), bulb never a spinner. GM6: navigation to English-33 failed under adb (see Open issues 1–2); not run. `emu-ac19/` |
| AC-20 Diamond-41 gap → notice, never "No winning move" | emulator; GM6 | **PASS**; **BLOCKED** | Emulator: spinner ≤1.3 s, notice by 1.8 s, gone by 6.7 s; Hint again → fresh notice; 2/2 Undecided. GM6: the gap sequence's jumps never committed (Open issue 1), so the position could not be reached; Undecided rate n/a. `emu-ac20/`, `gm6-ac20/` |
| AC-21 hints spent → Undecided shows the notice, no ad offer | emulator | **PASS** | After AC-18a (3 free hints used, 4th raised the offer, declined), Restart, walk A, Hint: spinner to 1.48 s, notice at 1.64 s; tree holds the notice and no dialog/offer. `emu-ac21/` |
| 23j.1 notice above the dock in the frame the spinner clears; exact 23b text; covers no dock control; tap puts it away and changes nothing | emulator (A, B, C, AC-20); GM6 (A, B, C) | **PASS** | Notice bounds emulator `[158,1919][996,2025]` vs dock `[42,2130]…`; GM6 `[120,848][656,928]` vs dock `[32,1008]…`. Text equals `hint_undecided_message` in every dump. Spinner frame → notice frame with no blank beat (bulb back and notice fading in in the same frame). A tap on the notice removed it within one frame (≤140 ms) on both devices; the board was unchanged (at 200% the canvas crop before/after is byte-identical, item 7). A cue cannot be heard over adb; 23i (JVM) covers silence. |
| 23j.2 fades after ≈5 s, or at once on the next jump | both | **PASS** (5-s half) | Display ≈5.0–5.1 s on every raise (emulator 1.58→6.56 s present, 6.70 s gone; animations-off run 1.48→6.49 s, gone 6.76 s; GM6 ≤2.4→6.8 s, gone 7.4 s). The "at once on the next jump" half was not exercised on a device; 23f (ViewModel) covers it. |
| 23j.3 Hint again on the unchanged board: notice goes at the tap, spinner ≈1.5 s, fresh notice | both | **PASS** | Emulator (A, B, C, AC-20): notice gone in the first frame after the tap (t+21–70 ms), spinner to ≈1.45 s, fresh notice at ≈1.6 s. GM6 (A, B, C): gone by the first capture (0.8 s), fresh notice by 2.4 s. |
| 23j.4 OS animations off: snaps in and out, still ≈5 s | emulator | **PASS** | "Remove animations" switched on through Settings (scales read 0.0): notice present fully at t+1.48 s with no intermediate fade value, gone with none at 6.76 s; ≈5.1 s. Switched back (scales 1.0). `emu-item4/` |
| 23j.5a TalkBack: double-tapping Hint speaks the notice once, never "No winning move"; focus stays on Hint | emulator | **PASS (speech) / BLOCKED (focus)** | Overlay after activation: "Hint couldn't work this position out in time. It may still be winnable."; never "No winning move" in any overlay or dump. Accessibility focus (green frame) stayed where it was (the timer) and did not move to the notice; it could not be placed on Hint first because injected taps pass through TalkBack as clicks on this image and do not move focus. `emu-talkback/02-…`, `03-…` |
| 23j.5b traversal timer → notice → Undo → Hint → Show-moves → Restart; notice one stop, no button; Dismiss in the actions menu | emulator | **PASS (tree order) / BLOCKED (spoken order, Dismiss)** | Every dump with the notice up lists: `Elapsed time` → notice text → `Undo` → `Hint` → `Show possible moves` → `Restart Board`; the notice node has no click action in the dumps. Alt+Right (`input keycombination`) and injected swipes never moved focus (frame bbox `[846,174][1005,255]` in all 7 frames), so the spoken order and the actions menu were not observed. 23c (JVM) proves the `Dismiss` action. |
| 23j.5c Time to take action 1 minute → notice stays ≈1 minute | emulator | **PASS** | Set through Settings (secure value 60000): notice present at t+58 s, gone at t+64 s. |
| 23j.6 greyscale, light and dark: notice vs banner read as different things | emulator | **PASS** | Light and dark (via `cmd uimode night yes`, restored) captures converted to greyscale: the notice is a pill with the lightbulb glyph and no buttons; the banner a medium-corner slab with Undo · Restart · Home. `emu-item6/*-grey.png` |
| 23j.7 200% font, 360 dp, Time to take action 2 minutes | emulator (`wm density 480`) | **PASS** | Notice `[180,1520][984,1968]` (≈187 dp tall) covers the board's lower rows and ends above the dock (`02-notice-200.png`). One tap at cell (8,4) under it removed it at once (0 notice nodes) and the canvas crop before/after is byte-identical (sha256 `89ddd00e…`). Left alone after a fresh Hint: present at t+120 s, gone at t+128 s. Density and font restored. `emu-item7/` |
| 23k device half: AC-19's Hint speaks "No winning move — undo or restart"; the stuck sequence's 6th jump speaks "No moves left — undo or restart"; traversal message → Undo → Restart → Home → dock | emulator | **PASS (speech, tree order) / BLOCKED (spoken traversal)** | Overlays: "No winning move — undo or restart" (1.2 s and 2.7 s after the Hint tap); "No moves left — undo or restart" (1.0 s and 2.5 s after the 6th jump). Tree order in both: message → `Undo` → `Restart` → `Home` → dock `Undo`/`Hint`/`Show possible moves`/`Restart Board`. Spoken traversal not drivable (as above). `emu-23k/` |

GM6 Undecided rate: walks A–C **6/6** (every Hint request, including the Hint-again on each walk); AC-20 not reached. Emulator: 17/17 off-book requests Undecided; the only non-Undecided answers were the book's (AC-18a) and the proven dead end (AC-19), as designed.

## Outputs
- `.planning/reports/hint-undecided/qa-engineer.md` — this report.
- Artifacts under `.planning/qa/hint-undecided/` (113 MB, git-ignored; never committed):
  - `build-assembleDebug.txt` (4.3 KB) build log; `emulator-boot.txt` (303 B).
  - `gm6-original-base.apk` (20.5 MB) the founder's APK as pulled, sha256 `dbe100e2…`.
  - `emu-00-home.png`, `emu-01-classic.png`, `emu-02-wiegleb-start.png`, `emu-03-restart-dialog.png`, `emu-04-diamond-start.png` (170–230 KB each) navigation.
  - `emu-walkA/` (94 files, 20 MB), `emu-walkB/` (68, 15 MB), `emu-walkC/` (24, 5.2 MB): `00/01-*.png` board after the walk, `hint1-t<ms>.png` frames after the first Hint, `hint2-…`/`hint3-…` Hint-again frames, `tapaway-…` frames after a tap on the notice.
  - `emu-ac18a/` (49, 9.5 MB) hintA–D frames and `0N-after-hintN.png`; `emu-ac19/` (17, 4.6 MB) incl. `02-banner-light.png`; `emu-ac20/` (27, 5.6 MB); `emu-ac21/` (23, 5.0 MB).
  - `emu-item4/` (31, 6.5 MB) animations-off frames and the Settings switch captures; `emu-item6/` (8, 1.7 MB) `notice-light/dark(-grey).png`, `banner-light/dark(-grey).png`; `emu-item7/` (7, 1.1 MB) `02-notice-200.png` coverage, `03-after-tap-on-notice.png`, `04-at-120s.png`.
  - `emu-talkback/` (12, 2.5 MB) `00-talkback-on.png`, `02-hint-focused.png`, `03-notice-spoken.png` (overlay with the notice text), `trav-1..6.png`, `swipe-1.png`; `emu-23k/` (6, 1.9 MB) `02/03-banner-*.png`, `04/05-stuck-*.png` (overlays with both banner utterances).
  - `gm6/` (9, 1.1 MB) navigation and wake captures; `gm6-walkA/` (13, 2.3 MB), `gm6-walkB/` (17, 3.0 MB), `gm6-walkC/` (17, 3.0 MB) as the emulator folders; `gm6-ac20/` (19, 3.3 MB) and `gm6-ac19/` (5, 852 KB) the blocked attempts (`02-select-diag.png`, `03-after-second-tap.png`, `04-later.png` show the uncommitted jump).
  - `emu-logcat.txt` (156 lines, 0 exception/fatal/ANR hits) and `gm6-logcat.txt` (63 lines, 0 hits) — the app's own log, dumped while it ran.
  - Session helpers: `ui.py`, `walk.py`, `hint.py`, `probe.py`, `focusbox.py`.
- Log excerpt (emulator, TalkBack start, the TTS confirmation):
  ```
  10-24 05:38:31.718 21142 21142 I TextToSpeech: Sucessfully bound to com.google.android.tts
  10-24 05:38:31.718   682   682 D TextToSpeechManagerPerUserService: Connected successfully to TTS engine: com.google.android.tts
  10-24 05:38:31.718 21142 21568 I TextToSpeech: Connected to TTS engine
  ```
  GM6 app log, typical of the whole dump (nothing from the hint path, no errors):
  ```
  10-24 05:28:51.790  6260  6260 W AudioTrack: AUDIO_OUTPUT_FLAG_FAST denied, rates do not match 44100 Hz, require 48000 Hz
  10-24 05:29:00.456  6260  6272 I zygote64: Background concurrent copying GC freed 261958(6MB) AllocSpace objects, 0(0B) LOS objects, 49% free, 6MB/12MB
  ```

## Checks
- `./gradlew --max-workers=4 :app:assembleDebug` (worktree, `53bd85f`) — pass (BUILD SUCCESSFUL in 14s; 55 actionable tasks).
- `./gradlew check` — not re-run: this is a review of a bugfix branch whose Check the developer ran green at `7201742` (its report) and the code-auditor's review is PASS; QA's work here is the device session.
- `python3 ~/.claude/skills/android-emulator-skill/scripts/emulator_manage.py --boot Pixel_9_clean` — pass (`emulator-5556`, `sys.boot_completed=1` within 5 s of registering).
- `adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` — pass (Success; data kept).
- `adb -s PJBQV4USFAHEKBRS pull …/base.apk gm6-original-base.apk` — pass; `adb -s PJBQV4USFAHEKBRS install -r app-debug.apk` — pass (Success); `adb -s PJBQV4USFAHEKBRS install -r -d -t gm6-original-base.apk` — pass (Success; `-r` alone failed with VERSION_DOWNGRADE, `-r -d` with TEST_ONLY); pulled back and hashed — pass (`dbe100e2…` = original).
- `apksigner verify --print-certs` on both APKs — pass (same debug cert `8a4e64a3…`).
- `adb shell uiautomator dump` + text checks per criterion — pass as tabled; `No winning move` never present on a walk or gap position (grep of every dump).
- `python3 ~/.claude/skills/android-emulator-skill/scripts/emulator_manage.py --shutdown emulator-5556` — run at the end of the session (the emulator this session booted).

## System changes (every one restored unless marked residual)
- Emulator `Pixel_9_clean`: app-debug installed over the existing install (data kept; the app was already present on this AVD before the session and is left installed, as earlier sessions left it); Accessibility › Remove animations on → off (global scales 0.0 → 1.0); `settings put system font_scale 2.0` → 1.0; `wm density 480` → `wm density reset` (420); `cmd uimode night yes` → `no`; Time to take action 2 minutes / 1 minute → Default (secure `accessibility_non_interactive_ui_timeout_ms` now `0`, was `null`: both mean Default — residual as a value); TalkBack on → off (`enabled_accessibility_services` empty, `touch_exploration_enabled` 0); TalkBack › Display speech output on → off; **residual:** TalkBack's own notification-permission prompt could not be dismissed with Back under TalkBack and was answered "Don't allow", so `POST_NOTIFICATIONS` for `com.google.android.marvin.talkback` stays `granted=false` but its flags gained `USER_FIXED`. Emulator shut down.
- GM6: app swapped and restored as above (data kept throughout); `settings put system screen_off_timeout` 60000 → 1800000 → 60000; `svc power stayon usb` (global `stay_on_while_plugged_in` 0 → 2) → `svc power stayon false` (0). No `settings put` on global or secure, no radio or Bluetooth change, no wipe. Font scale 1.0, animation scales 1.0, no accessibility service, STREAM_MUSIC unmuted at index 5 — all as found. The app is left on a Classic board screen rather than Home, and its "Continue Classic" slot now points at the board last opened in this session. Battery rose from 2 % to 26 % on USB during the session.

## Open issues / risks
1. **GM6, Diamond-41: a two-tap jump did not commit** (3 attempts, `gm6-ac20/02-select-diag.png` → `04-later.png`): the first tap highlighted the peg and the centre target, the second tap left both pegs in place with the highlights still drawn, while the same two-tap jumps worked on Wiegleb-45 and English-33 on the same phone and all boards on the emulator. Reproduction: GM6, Classic › Diamond-41, tap (2,4) then (4,4). Not in this branch's diff (board input is untouched) and possibly an interaction with the phone's sleep/injected-input behaviour (issue 2); it blocks AC-19/AC-20 on the GM6 here and is **routed to the producer for the developer to look at on that phone**, severity unknown.
2. **GM6 sleeps under adb input**: `mUserActivitySummary=0x0` while injected taps arrive, so the screen went off after its 60 s timeout even mid-sequence; worse, taps still reach the app while asleep (a blind Hint produced a notice in a dump taken with `mWakefulness=Asleep`). Every GM6 result above is from captures verified non-black; captures taken asleep were discarded. `svc power stayon usb` was the workaround. A lesson for the retro.
3. **TalkBack cannot be driven over adb on the API 36 image**: injected taps pass through as clicks without moving accessibility focus; `input keycombination ALT_LEFT DPAD_RIGHT` and injected swipes do not navigate; the actions menu cannot be opened. This blocks the spoken-order and Dismiss halves of 23j.5 and 23k (third session in a row, see phase-15). Spoken content was captured via "Display speech output". Suggested for the retro: a founder-run TalkBack pass before release, or a scrcpy/physical-keyboard session.
4. 23j.2's "at once on the next jump" half was not exercised on a device (23f covers it on the JVM); 23j.1's "no cue plays" rests on 23i (JVM), since adb cannot hear the device.
5. The emulator's notification-permission flag change on TalkBack (residual, above).

## Handoff
Next: **tech-lead** — sign-off on `bugfix/hint-undecided`: QA PASS WITH BLOCKED ITEMS; the blocked items are tooling limits (TalkBack over adb) and GM6 navigation, not product failures, and every truth criterion (AC-18b–d, AC-20, AC-21, 23j.1–4, 23j.6–7, 23k's speech) passed on the emulator, with AC-18b–d also on the GM6.
Next: **producer** — route Open issue 1 (GM6 Diamond-41 jump) to the android-game-dev as a separate observation, and issues 2–3 to the retro; delete `.planning/qa/hint-undecided/` (113 MB) after the STATUS.md Done line.

VERDICT: PASS WITH BLOCKED ITEMS
