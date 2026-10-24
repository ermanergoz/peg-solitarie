# qa-engineer report — reduced-motion-live-gate (device session)
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (report only; build from `c6f52dc`, code `45f84ec`; uncommitted, as dispatched)
Skills loaded: ergoz-soft:standards (role qa-engineer; game; android-compose-game). The snapshot (0.5.1) is older than plugin 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing. Also android-emulator-skill
Ran in parallel with: another QA session (talkback-board) on the same host
Ran on: claude-opus-5-5 at effort high (policy). **Staffing deviation, recorded:** the agent file pins fable at high; the dispatch runs this session on Opus under Q-019 answer 3. The code under test was also written on Opus.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot has 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **The live gate works on the device.** With English-33 open, turning "Remove animations" on gives the dashed mark; turning it off gives the shake back. Turning it off brings back the Results star pops and the tutorial guide's pulse; turning it on snaps them. P9 and WT-15 pass with animations off. §8 passes: 6 process starts, 2 rotations and 2 process-death restores, with 0 `MotionDurationScale` lines and 0 crashes.
- **One new finding, low:** the switch reaches the app about 10 s after the toggle, not right away. Until then the gate and the clock both keep the old value, and they agree (no "nothing visible" frame). Measured 4 times, each switch between 9.8 s and 11.7 s after the toggle. This fits Android's delay for content-observer calls to an app that is in the background when the setting changes. It is not this branch's code.
- **BLOCKED:** §6 (the TalkBack count on the Undecided notice) and the completion star caught mid-pop. Reasons below. Plus one incident: another QA session replaced my build on my emulator at 15:57:54.
- **Not restored:** TalkBack is on and "Time to take action" is set to 2 minutes on `Pixel_9b_clean` (emulator-5556). A permission dialog that will not respond blocks the UI way back, and the only other way is a `settings put secure` command, which QA may not use. This is for the producer.

## Device and build
- **Device:** `Pixel_9b_clean`, emulator-5556, API 36, 1080×2424, booted by this session.
  - The dispatch named `Pixel_9`. I booted it, but its `/data` was 97 % full (218 MB free, then falling to 143 MB), and `adb install -r` failed twice: "Requested internal only, but not enough space". `pm trim-caches 4G` freed nothing.
  - I shut `Pixel_9` down (this session booted it) and used `Pixel_9b_clean` instead. It was not running and is not reserved. I never touched `Pixel_9a_bench` (5554) or `Pixel_9_clean` (5570).
  - A real phone (GM6) was attached. The dispatch assigned an AVD, and the session needs changes to shared settings, so I did not use the phone.
- **Build:** `./gradlew :app:assembleDebug --max-workers=4` passed. `app-debug.apk` md5 is `a962385078a3a886457fd93d98170121`. It was installed with `-r` over an older debug build of the same package, so the app's data was kept.
- **How the setting was switched:** through the Settings UI (Accessibility › Color and motion › Remove animations), never with `settings put global`.
  - The dispatch asked for `settings put`, but my agent file forbids `settings put` on global or secure settings on a shared device (F-51, F-28).
  - The UI switch writes all three scales: 0.0/0.0/0.0 when on, 1.0/1.0/1.0 when off. `settings get` confirmed this after every switch.
  - It is also the user's own path, the one the audio sign-off §3 names.
  - The app stayed resident throughout (same pid 3788 or 11177 across trips). I came back with Back, because a launcher intent from the shell opens Home on top (observation 3).
- **Incident (coordinator's warning, verified).** At 15:57:53–54 another QA session uninstalled my build on emulator-5556 and installed the talkback-board build, which wiped the app's data.
  - On-device md5 was `2a2ff3f5…`, with lastUpdateTime 15:57:54.
  - I reinstalled at about 16:00 and the md5 matched `a962…` again.
  - Every result in the table below was recorded before 15:57:54, on my build, so nothing needed re-running. The only work after that time was the TalkBack setup for §6, which never reached the app.

## Results
| Check | Verdict | Evidence |
|---|---|---|
| §3 Remove animations **on** with English-33 resident → the mark within 450 ms | **PASS** (after the ~10 s delivery, finding F1) | `d1-on-latency.mp4`. Toggled at 15:41:04.303, back in the app at 06.957. Taps 1–4 (+4.3 to +9.8 s) still shook: about 233 ms of motion, max 532 px changed, a full shake, so the clock was still at 1 as well. From tap 5 (+11.65 s) every tap drew the mark, 1,444 px changed in the tap's first video frame, held 464–468 ms (`d1-on-latency-diff.txt`). `d1-shake-vs-mark.png` shows a shake frame next to the 12-dash ring |
| §3 Remove animations **off** → the shake returns | **PASS** (same delay) | `d2-off-latency.mp4`. Toggled at 15:41:58.628. Taps 1–4 (to +9.8 s) drew the mark (465 ms). From tap 5 (+11.7 s) every tap shook (233 ms). Earlier runs `b1`–`b3`, `c1` agree (taps 4–8 s after a toggle get the old path, and taps 12 s or more after get the new one) |
| WT-15 (tutorial jump haptic, animations off) | **PASS** | `wt15-vibrator.txt`: 6 jumps, 6 `performHapticFeedback(constant=6)` TICKs at 15:48:38.031, 49.606, 15:49:06.305, 08.539, 10.772, 13.005, each within about 20 ms of the jump's tap. None for a pick-up |
| P9 (removal layer, animations off) | **PASS** (play path) | `p9-audio-*.txt`, English-33, scales 0.0. Select 15:45:48.917, jump 49.773, removal **49.841 (+68 ms)**, haptic 49.772. In the tutorial with animations off, all 6 jumps play jump + removal 62–63 ms apart (`wt15-audio.txt`) |
| Results stars after a live toggle | **PASS** | **On** (toggled 15:51:31, finished 12 s later): the Results screen ("Perfect!", 3 stars) comes in one frame at 1.916 s, with no further frame in the recording; the stars snap (`r2-*`). The pop sounds still stagger: 51.606, 52.234, 52.862. **Off** (15:52:52, finished 12 s later): 28 changing frames over 2.7 s, with the stars popping in turn and the third overshooting (`r3-pop-frames.png`) |
| Tutorial redirect after a live toggle | **PASS** | Composed at 1: the selected peg's orange guide pulses all the time, with about 4,000 px changing on every frame (`t1-*`). After live **on** plus 12 s: 3 frames in 9 s. Each T-W2 tap is one static frame change, with no shake, no mark and no pulse (`t2-frames.png`) |
| Tutorial completion star (composed with animations off) | **PASS** | `t5-complete-small.png`: the star is drawn settled at full size |
| Completion star caught mid-pop by a live toggle | **BLOCKED** | The toggle can only be made from Settings, so the app is in the background, and the change arrives about 10 s later (F1). That is far longer than the star's 0.7 s spring, so it cannot be caught on the device by this route. The ruling (F1.3) makes `TutorialCompletionStarLiveMotionTest` the evidence of record |
| §6 count of the Undecided notice's TalkBack announcements across a live toggle | **BLOCKED** | The notice was raised on my build (Diamond-41 off book, 15:56, `n2-notice.png`). The 2-minute timeout was set and TalkBack turned on (16:00:51). The app was then relaunched, and TalkBack's first-run "Allow Android Accessibility Suite to send you notifications?" dialog took the screen. Its buttons ignore injected single taps, double-taps (80 and 150 ms) and Tab, DPAD and Enter, and Back. The permission controller ANR'd once ("isn't responding", Wait chosen). The app was never reached under TalkBack |
| §8 cold start, rotation, process-death restore, animations off and on | **PASS** | Per state: a cold start, English-33, rotate to landscape and back (`adb emu rotate`), Home, `am kill`, then a relaunch. Restored from Recents into the game (`s8-*-5-restored-recents.png`, Elapsed and Undo present). `grep -c MotionDurationScale` = 0 and `grep -c -E 'FATAL EXCEPTION\|E AndroidRuntime'` = 0 in `s8-off-logcat-all.txt` (4,887 lines), `s8-on-logcat-all.txt` (4,611), `s8-off-restore-logcat.txt` and `s8-on-restore-logcat.txt`. 6 `Start proc` lines in all. The restored game composed with animations off draws the mark (`e1-*`) |

Logcat excerpt (`s8-on-logcat-all.txt`, the animations-off start path):

    10-24 15:44:39.553   669   703 I ActivityManager: Start proc 10632:com.erman.pegsolitarie/u0a212 for next-top-activity {…MainActivity}
    10-24 15:44:42.005   669   693 I ActivityTaskManager: Displayed com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity for user 0: +2s477ms
    10-24 15:44:59.663   669  1730 I ActivityManager: Killing 10632:com.erman.pegsolitarie/u0a212 (adj 700): kill background
    10-24 15:45:00.793   669   703 I ActivityManager: Start proc 10900:com.erman.pegsolitarie/u0a212 for next-top-activity {…MainActivity}
    $ grep -c 'MotionDurationScale' s8-on-logcat-all.txt   →   0

## Findings
1. **F1 — low, routed to the tech-lead and ux-designer (not this branch's code): reduced motion arrives about 10 s after the toggle.**
   - **Steps.** Open English-33, go to Settings › Accessibility › Color and motion, turn Remove animations on, press Back, and make a wrong tap every 1.8 s.
   - **Expected:** the mark from the first tap back.
   - **Actual:** a full-motion shake until about 10 s after the toggle, then the mark.
   - **Evidence:** `d1-on-latency-diff.txt` and `d2-off-latency-diff.txt`. The same holds in both directions, and for the stars and the tutorial pulse.
   - **The gate and Compose's clock stay in step the whole time.** The shake plays its full 233 ms, so this is not the old "nothing visible" defect.
   - **Likely cause:** the platform delays a content-observer call to an app that is in the background when the setting changes. The ruled design leaves this to Compose's observer and rejected a re-read on resume (audio sign-off §3).
   - **For a decision, not a fix here:** the ux-designer and tech-lead decide whether about 10 s of full motion after the user turns Remove animations on is acceptable.
2. **Observation — landscape clips the board.** `s8-off-3-rotated.png` shows English-33's top and bottom rows cut off in landscape. It is outside this branch and is probably older than it. For the producer.
3. **Observation — a launcher intent from the shell returns to Home.** `am start -a MAIN -c LAUNCHER` to the resident task lands on Home ("Continue Classic"), not the game (`a1-03-pair.png`). Recents and Back keep the game. This is probably an artifact of the shell intent's flags, recorded only so it is not mistaken for this branch.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rm/.planning/reports/reduced-motion-live-gate/qa-engineer.md`: this report.
- Artifacts are in `/Users/ermanergoz/Desktop/PegSolitarie-rm/.planning/qa/reduced-motion-live-gate/` (git-ignored, about 15 MB):
  - **Recordings** (mp4, 12 Mbit/s), each with `-diff.txt` (per-frame changed px in the cell crop), `-taps.txt` and `-ffmpeg.txt`:
    - `d1-on-latency.mp4` (570,424 B) and `d2-off-latency.mp4` (694,086 B): the latency series;
    - `a1`/`a2`, `b1`–`b3` and `c1`/`c2-*.mp4` (152–473 KB): live-toggle series. `a2` is void: the shell intent had sent the app to Home;
    - `e1-restored-on-w1.mp4` (152,174 B): the mark after a restore;
    - `r2-live-on-results.mp4` (147,820 B) and `r3-live-off-results.mp4` (432,221 B): the Results entrance;
    - `t1-anim-on-redirect.mp4` (971,357 B) and `t2-live-on-redirect.mp4`: the tutorial.
  - **Images:**
    - `d1-shake-vs-mark.png` (67,545 B): a shake frame next to the dashed mark;
    - `r3-pop-frames.png` (39,376 B), `r2-results-small.png` (35,392 B), `t2-frames.png` (28,548 B), `t5-complete-small.png`;
    - `s8-{off,on}-{1-cold,2-game,3-rotated,4-restored,5-restored-recents}.png` (108–300 KB);
    - `n2-notice.png` (217,702 B): the Undecided notice, my build, 15:56.
  - **Logs:** `s8-off-logcat-all.txt` (660,890 B), `s8-on-logcat-all.txt` (619,690 B), `s8-*-restore-logcat.txt` (264–303 KB), `p9-audio-before.txt`/`p9-audio-after.txt`/`p9-vibrator.txt`, `wt15-vibrator.txt`/`wt15-audio.txt`, `r2-audio.txt`, `build-debug.txt` (4,030 B).
  - **Helper scripts** (never committed): `ui.py`, `motion.sh`, `motion_kb.sh`, `wrongtap.sh`, `analyse.sh`, `rec.sh`, `startpath.sh`, `solve33.py` with `english-solution.txt` (31 moves), and `diamond.py`.

## Checks
- `./gradlew :app:assembleDebug --max-workers=4`: pass (BUILD SUCCESSFUL).
- `adb -s emulator-5556 install -r …/app-debug.apk`: pass. It was reinstalled after the incident, and `md5sum` on the device gave `a962385078a3a886457fd93d98170121`.
- `adb -s emulator-5556 install -r` on `Pixel_9`: fail ×2 (not enough space). Mitigated by switching AVD.
- `screenrecord` + `ffmpeg … crop,showinfo` + `wrongtap.sh`/`rec.sh`: mark and shake timings as tabulated above.
- `grep -c 'MotionDurationScale'` over four logcat dumps: 0, 0, 0, 0 (pass).
- `dumpsys audio` and `dumpsys vibrator_manager`: P9 and WT-15 as tabulated above.
- The Check was not run. This is a review step with a recorded green Check (`android-game-dev-finding1.md`).

## Open issues / risks
1. **System changes NOT restored on `Pixel_9b_clean` (emulator-5556, left running):**
   - **TalkBack is on.** A notification-permission dialog is up and will not respond to injected input.
   - **"Time to take action" is set to 2 minutes** (`accessibility_interactive_ui_timeout_ms` and `accessibility_non_interactive_ui_timeout_ms` read 120000; they were `null` before).
   - Either a person at the emulator, or a role allowed `settings put secure enabled_accessibility_services ''` and Timing controls › Default, must restore them. **Producer, please route this.**
2. **Restored and recorded:**
   - Remove animations is off, and the scales read 1.0/1.0/1.0. Before the session, `animator_duration_scale` read `null`, the same default, which is now written explicitly.
   - The emulator's rotation is back to portrait.
   - `com.erman.pegsolitarie` is this branch's build. Its data was wiped by the other session's reinstall, not by me.
   - `Pixel_9` was shut down by me (I booted it). It is unchanged apart from a boot.
   - `pm trim-caches 4G` ran on `Pixel_9`. It deletes only app caches, and it freed nothing.
3. **§6 and the mid-pop star are BLOCKED.** If §6 must be shown on a device, rerun it on an AVD whose TalkBack has been set up once by hand. Split-screen with Settings would keep the app in the foreground, which avoids the ~10 s delay of F1.
4. **The ux-designer still owes its §6 judgement**, which the tech-lead routed. There is no device count to give it yet.

## Handoff
Next: tech-lead — the sign-off. Every cut-blocking device check passed: the mark on, the shake off, WT-15, P9, the Results stars, the tutorial redirect and §8. Weigh F1, the ~10 s delivery, and the two BLOCKED items.
Next: producer:
- restore TalkBack and the accessibility timeout on `Pixel_9b_clean` (open issue 1);
- record the cross-session reinstall incident;
- route F1 to the tech-lead and ux-designer, and observations 2–3 to the backlog;
- delete this artifact directory after the Done line.

VERDICT: PASS WITH BLOCKED ITEMS
