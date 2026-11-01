# qa-engineer report — home-chip-arrival (device checks after the 15e sitting)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (report only; nothing committed — the producer commits). Worktree `/Users/ermanergoz/Desktop/PegSolitarie-home`, head `b0ec4f2`; arm B `7448713`.
Skills loaded: ergoz-soft:standards (role qa-engineer; the product snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; `.claude/rules/lessons.md`), android-emulator-skill
Ran in parallel with: not tracked by name. During the session Gradle ran in `PegSolitarie-cv`, `PegSolitarie-lic` and other worktrees (until ≈16:39), and another session's `Pixel_9b_clean` (`emulator-5556`) stayed up throughout. `Pixel_9_clean` was not used.
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** the agent file pins fable. The dispatch staffed Opus under **Q-019**, the same model as the developer. Mitigation: every verdict below rests on a device artifact listed here, which any later reader can re-check.
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot; no `.planning/recall/RECALL.md`)
Authority: `tech-lead-device-ruling.md` §1 (Ruling 1, C-1 to C-10). It allows a fresh QA session to run the device checks outside the sitting's agent session.

## Summary
- **Three production defects were found on arm B's APK.** Under C-9 each goes to the developer, then the code-auditor, then a new 15e sitting.
  - **29 (2) FAIL.** In dark mode the three-button bar is still light: scrim relative luminance 0.607, against a limit of 0.18. Arm A looks identical, so D-05f's `values-night-v27` change has no effect on an API 36 device.
  - **18g FAIL.** TalkBack speaks the focused running timer again every 2.5–3 s. This triggers the pre-ruled whole-minute fallback (brief §1.6), which is not built yet.
  - **26b FAIL on gameplay.** TalkBack visits the timer *first*, before "Navigate back", and not after the title. This happens on Classic and Challenge, in light and in dark. Arm A behaves the same, so the defect predates this branch.
- **Passed:** 15w's device half; 26b on Home and on the tutorial; 29 (2) in light mode, for the gesture handle and for the status-bar icons.
- **15v (all eight recordings): BLOCKED.** From 16:20 to 19:20 the host never had exactly one emulator, because another session's `Pixel_9b_clean` stayed up. Gradle was quiet from ≈16:39. I made no recordings.
- C-1, C-2 and C-3 held. The bench was cold-booted, restored to its resting state and shut down (`emu kill`, 19:21).

## Per-criterion results
| Criterion | Result | Evidence (under `.planning/qa/home-chip-arrival/device/`) |
|---|---|---|
| C-1 code identity | **held** | Both `git diff --stat` commands printed nothing (Checks). |
| C-2 APK identity | **held** | The installed `base.apk` was `9d507a88…2decaa` at the start. It was the same after the arm-A comparison and the reinstall of `armB-7448713.apk`, whose file hash is also `9d507a88…`. Nothing was rebuilt. |
| C-3 bench state | **held** | `Pixel_9a_bench`, `Accounts: 0`; 200 challenge rows, `SUM(stars)` 401, `challenge_current_level` 111 (pref bytes `…\x18o`), one `classic` resume row. The only difference from `bench-state-after.txt` is `Data-Free` (13 526 924K against 13 533 388K). No re-seed was needed. |
| C-4 boot | **done** | Cold boot at 15:37:38 with `-no-snapshot-load -no-snapshot-save -no-boot-anim` on port 5584; `sys.boot_completed=1` by 15:42:50. |
| **15v** parts 1–4 (8 recordings, closing-chip shadow included) | **BLOCKED** | Under C-5, P-4 was not met. `quiet-15v.log` has 352 polls from 16:20:25 to 19:20:29, and 0 of them show one emulator. Gradle was quiet from 16:21 (`quiet=yes` on most later lines), but `Pixel_9b_clean` was up the whole time. The old `15v/nat-1save-off/` was renamed `old-15v-nat-1save-off-unread-not-record/` and not read (C-7). The drivers are ready: `rec1.sh`, `an15v.py`. |
| **15w** device half | **PASS** | `15w-360-fs2-night-no.png` and `-yes.png` at 1080×1920 / 480 dpi (360×640 dp), `font_scale` 2.0. "Stars", "Boards" and "Level" each read whole on one line. The pills are stacked (label over value, no glyph), at one height. |
| **29 (2)** dark, three-button | **FAIL** | Home, gameplay and the tutorial all show scrim `(205,204,208)`, luminance **0.607** (limit 0.18), with buttons `(102,102,102)` at 3.59:1. `dumpsys window`: `mLastAppearance=LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS` in night mode (`29/recheck-threebutton-dark-home.png`). Arm A gives the same pixels and the same flags (`29/armA-threebutton-dark-home.png`). |
| 29 (2) light, three-button | PASS | Scrim `(249,248,250)` L 0.942; buttons at 5.42:1. |
| 29 (2) gesture handle, both themes | PASS | Light: handle `(98,96,100)` on `(244,241,250)`, 5.57:1. Dark: `(237,237,238)` on `(27,22,38)`, 15.09:1. |
| 29 (2) status-bar icons | PASS (as H-2 left them) | Black icons on light (18.8:1), white on dark (17.7:1), in both navigation modes. |
| **18g** first half: focus speaks the value | PASS | `18g-dark/sheet.png`: focused at 00:22–00:23, TalkBack says "Elapsed time, 22 seconds". |
| **18g** second half: 10 s of rest, no more speech | **FAIL** | Same run: the focus stays on the timer and TalkBack speaks again at 25, 28, 30 and 33 s (rows at +3.1, +5.8, +8.1, +10.9 s). By 18g's own text, the whole-minute fallback must now be built, and 18g re-read against it. |
| **26b** Home, light and dark | PASS | `26b/home-dark2/`, `26b/home-light/`: "Peg Solitaire" + "Heading" → "Settings" + "Button" → "Stars, 401" → "Boards, 0/5" → "Level, 111" → "Continue Classic" → "Continue Challenge" → "Classic, 5 boards" → "Challenge, Continue — Level 111", then the end of the list. The stage, its art and the footer are never a stop. |
| **26b** gameplay, Classic and Challenge, light and dark | **FAIL** | `26b/game-classic-{dark,light}/`, `26b/game-challenge-{dark,light}/`: the order is "Navigate back" → title ("Peg Solitaire" or "Challenge level 111", Heading) → Undo → Hint → "Show possible moves" (On, Switch) → "Restart Board" → end of list. The swipe after the end wraps to **"Elapsed time, N seconds" and then "Navigate back"**. In `26b/game-order-check/`, a swipe left from "Navigate back" lands on the timer, and the next swipe left stays there because it is the first stop. The accessibility tree itself is in spec order (`26b/game-tree.xml`: back, title, timer, Undo…). Arm A is the same (`26b/armA-order/sheet.png`). The board stop is absent, which is not a failure (26b's 2026-10-23 note). |
| **26b** tutorial, light and dark | PASS | `26b/tutorial-{dark,light}/`: "Step 1 of 3" → "Skip tutorial" → caption. On completion (`26b/tutorial-done-light/`, light only): pip row → Skip → "That's the whole game. Go clear a board." → "Got it". |
| C-8 restore and shut down | **done** | See Restore record below. |

## Bugs filed
**B-1 (29 (2), high for the criterion; cosmetic) — In dark mode the three-button bar stays light.**
- Steps: bench, API 36, three-button navigation (`cmd overlay enable-exclusive --category …navbar.threebutton`), `cmd uimode night yes`, then a cold launch of Home (gameplay and the tutorial show the same).
- Expected: a dark scrim with L ≤ 0.18, and light buttons at ≥ 3:1.
- Actual: a light-grey scrim `(205,204,208)`, L 0.607, with dark buttons `(102,102,102)`.
- Evidence: `29/threebutton-night_yes-{home,game,tutorial}.png`, `29/recheck-threebutton-dark-home.png`, `29/bottoms.png`.
- Log: `mLastAppearance=LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS`.
- The APK does carry the fix: `aapt2` shows `(night-v27)` with `0x0101056c=false` (`windowLightNavigationBar`).
- Arm A is pixel-identical, so the theme item does not reach the window on API 36.
- **Hypothesis only, for the developer:** `FORCE_LIGHT_NAVIGATION_BARS` is set by the system, perhaps from the `android:Theme.Material.Light.NoActionBar` parent (a light theme in both modes) under API 35+ edge-to-edge. The JVM test at `sdk = 34` cannot see this.

**B-2 (18g) — The focused running timer is spoken again every 2.5–3 s.**
- Steps: TalkBack on with "Display speech output"; Classic game running; touch-explore to the back control, then to the timer; leave focus there for 13 s.
- Expected: one utterance.
- Actual: "Elapsed time, 22 seconds", then "…25 seconds", "…28 seconds", "…30 seconds", "…33 seconds".
- Evidence: `18g-dark/sheet.png` (18 changed frames with timestamps) and `18g-dark/*.png`.
- Route: the pre-ruled whole-minute fallback (brief §1.6: a minutes-only description plus `duration_under_a_minute`). Then 18g is read again: at most once a minute.

**B-3 (26b, gameplay) — The timer is the first TalkBack stop, not the third.**
- Steps: TalkBack on; open Classic or Challenge; swipe right from "Navigate back" through to the end, then once more. Or swipe left once from "Navigate back".
- Expected: back → title → timer → Undo.
- Actual: timer → back → title → Undo… The timer is skipped between the title and Undo.
- Evidence: `26b/game-*/sheet-*.png`, `26b/game-order-check/sheet.png`, `26b/game-tree.xml`, `26b/armA-order/sheet.png`.
- It predates this branch (arm A `6aa7624` is the same). It still fails 26b, which applies to this branch. The tree order is correct, so this is TalkBack's traversal (Compose traversal hints) and not the node order.

## Outputs
- This report. Session artifacts are git-ignored, under `.planning/qa/home-chip-arrival/device/` (123 MB):
  - `bench-state-start.txt`, `bench-state-end.txt` (955 B each): the bench's settings, the installed sha256 and `Data-Free` at both ends.
  - `boot.txt` (71 B), `emulator.log` (11 KB): the cold boot.
  - `db-start/` (the resting data, pulled at the start and restored at the end), `db-end/` (pulled after the restore), `two-saves/` (the 1-save state plus a Challenge save made by one jump on level 111, kept for 15v).
  - `home-start.png`, `home-two-saves.png`, `challenge-111*.png` (≈100–200 KB): Home with one save and with two. The tiles did not move between the two (15s, by eye).
  - `15w-360-fs2-night-{no,yes}.png` (≈180 KB) and `.xml` (12 KB): 15w.
  - `29/` (2.5 MB): 12 screens (`{threebutton,gestural}-night_{no,yes}-{home,game,tutorial}.png`), `bottoms.png` (the bars stacked), `recheck-threebutton-dark-home.png`, `armA-threebutton-dark-home.png`.
  - `18g-dark/` (9.1 MB): 18g's frames, `times.txt` and `sheet.png`.
  - `26b/` (104 MB): one folder per walk, with burst frames per swipe and `sheet-*.png` (focus frame plus speech overlay); `game-order-check/`, `game-tree.xml`, `armA-order/`.
  - `tb-tts-health.txt` (3 KB), `tb-on.png`, `tb-devsettings.png`, `tb-devsettings-restored.png`, `tb-after-dontallow.png`, `tb-nav-test*.png`: TTS health and TalkBack setup.
  - `quiet-15v.log` (34 KB): every 15v host poll.
  - Helpers: `bars.sh`, `walk.sh`, `walk2.sh`, `tbswipe.sh`, `g18.sh`, `hostq.sh`, `waitq.sh`, `rec1.sh`, `an15v.py`, and `kev.py` (unused: SELinux denies writes to `/dev/input`).
- Renamed, not read: `.planning/qa/home-chip-arrival/old-15v-nat-1save-off-unread-not-record/` (5.3 MB, the previous session's recording; C-7).
- **Log excerpts:**
  ```
  10-24 15:50:23.388 I TextToSpeech: Sucessfully bound to com.google.android.tts
  10-24 15:50:23.470 I GoogleTtsServicePeer: Creating Google TTS service, version googletts.google-speech-apk_20250804.02_p3.800153222
  Bound services:{Service[label=TalkBack, feedbackType[FEEDBACK_SPOKEN, FEEDBACK_HAPTIC, FEEDBACK_AUDIBLE] …]}
  (night, three-button, app focused)  mLastAppearance=LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS
  ```

## Checks
- `git diff --stat 7448713 HEAD -- . ':!.planning'` — pass: empty (head `b0ec4f2`).
- `git diff --stat 6aa7624 develop -- . ':!.planning' ':!docs' ':!CLAUDE.md' ':!AGENTS.md'` — pass: empty (`develop` `d21ad07`).
- `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim` — pass: booted 15:42:50; `emu avd name` = `Pixel_9a_bench`.
- `state.sh` (`pm path`, `sha256sum`, settings, `dumpsys account`, `diskstats`) — pass: `9d507a88c23cfce5b144619f4bdbf95b93eaf45b7eab728dbede1b3afd2decaa`; `Accounts: 0`; `Data-Free` 13 526 924K at the start and 13 575 508K at the end.
- `shasum -a 256 armB-7448713.apk` — `9d507a88…2decaa`. `shasum -a 256 armA-6aa7624.apk` — `35effc51…db64a3d`.
- `sqlite3` on the pulled DB: 200 rows, sum 401, `resume_snapshot` = `classic`. After the restore, the Classic `jsonBlob` hash is the same as at the start (`76b0d896…`), and the prefs file is byte-identical.
- `aapt2 dump resources armB-7448713.apk` — `(night-v27)`: `0x010104e0=false`, `0x0101056c=false`.
- `dumpsys window` (night, three-button) — `LIGHT_NAVIGATION_BARS FORCE_LIGHT_NAVIGATION_BARS` on both arms.
- TalkBack 16.0.0.777931756 with Google TTS `20250804.02_p3`: TTS health passed (bound, connected, overlay text shown), so the `Pixel_9_clean` fallback was not needed.
- **TalkBack was driven with the emulator console's touchscreen events** (`adb emu event mouse x y 0 1|0`, in `tbswipe.sh`). Swipes, touch exploration and double-taps work on the API 36 image, where `adb shell input` and keyboard events do not. The utterances are read from TalkBack's "Display speech output" overlay, captured in bursts after each gesture.
- `hostq.sh` (C-5) — 352 polls from 16:20:25 to 19:20:29; never one emulator. 15v BLOCKED.
- Not run: the Check (`./gradlew check`), any Gradle build, any rebuild. These are device checks, and C-2 forbids a rebuild.

## Restore record (C-8)
Final state, from `bench-state-end.txt` at 19:21:04:
- arm B installed, `9d507a88…2decaa`, versionName 3.0.0;
- animations 0/0/0; font scale 1.0; light mode; gesture navigation;
- `wm size` and `wm density` at the physical values (1080×2424, 420);
- `enabled_accessibility_services` unset (`null`), `accessibility_enabled` 0;
- `Accounts: 0`; `Data-Free` 13 575 508K;
- the seeded ladder and the one Classic save restored from `db-start/`.

Every system change in this session, and what happened to it:
- `wm size`/`wm density` 360×640 dp and `font_scale` 2.0 (15w) — reset.
- `cmd uimode night` yes/no (15w, 29, 26b) — back to `no`.
- Navigation overlay switched to three-button and back (29) — `gestural` again.
- TalkBack enabled through secure settings, and disabled again.
- TalkBack's developer options "Display speech output" (on → off) and "Log output level" (ERROR → VERBOSE → ERROR) — both restored.
- `setprop log.tag.talkback VERBOSE` — not persistent, and cleared by the shutdown.
- TalkBack's `POST_NOTIFICATIONS`: its prompt was answered "Don't allow" twice, then its flags were cleared with `pm clear-permission-flags … user-set user-fixed`. It is `granted=false` with no user flags, as found.
- Arm A installed for comparison (`install -r -d --no-incremental`, data kept), then arm B reinstalled. The hash was verified.
- The Classic and Challenge saves were changed during play — restored to `db-start/`.

The bench was shut down with `adb -s emulator-5584 emu kill` at 19:21:27; no `Pixel_9a_bench` process remained. I touched no other emulator and not the GM6.

## Open issues / risks
1. **Three production defects mean another loop.**
   - B-1 needs a mechanism that works on API 35+.
   - B-2 is the pre-ruled fallback.
   - B-3 is a pre-existing traversal defect. The tech-lead may rule whether it is fixed on this branch or waived to a follow-up, since arm A has it too.

   Any production fix re-owes the 15e sitting (C-9), and 15v then follows that new sitting.
2. **15v is BLOCKED by the host, not by the product.** P-4's "exactly one emulator" was never met in three hours. Another session's `Pixel_9b_clean` was up the whole time, showing a permissions dialog. Before the next 15v block, the producer must actually clear the host: emulators as well as Gradle.
3. **One TalkBack walk was disturbed while the host was loaded.** A first "tutorial-light" attempt landed on Home, and its swipes skipped stops: Stars → Continue Challenge. I counted it as gesture noise from the console-touch driver under load. Its frames were overwritten by the clean tutorial walk. Every verdict above comes from clean walks, and each gameplay finding reproduced four or more times.
4. Tutorial completion ("Got it") was walked in light only. Dark covered the pip row, Skip and the caption.
5. Retro note (R-3d's process lesson, and tooling): `adb emu event mouse` makes TalkBack testable on the studio's API 36 emulator images. Earlier sessions recorded this as impossible (`hint-undecided/qa-engineer.md`, Open issue 3).

## Handoff
Next: producer —
- Route B-1, B-2 and B-3 to the tech-lead and developer (C-9). B-2's fix is already ruled (brief §1.6). B-1 and B-3 need a mechanism ruling.
- After the fix: the code-auditor, then a new 15e sitting, then the device checks again in full, including all eight 15v recordings on a host the producer has cleared of every other emulator.
- The tech-lead's sign-off cannot proceed on this record.

VERDICT: FAIL
