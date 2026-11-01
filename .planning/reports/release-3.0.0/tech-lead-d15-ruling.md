# tech-lead report — release-3.0.0: D-15 ruling after the trace diagnosis
Date: 2026-11-01
Branch: release/3.0.0 (report only; HEAD `89470c1`. `git diff --stat fb66351 89470c1` outside `.planning` and `docs` is empty, so HEAD's code is the probe's and the trace's build.)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The snapshot `.claude/rules/VERSION` is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch, plus `.claude/rules/lessons.md`. Skills: android-architecture, android-gradle-logic.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh. I run on Opus under FOUNDER.md Q-019 answer 3 and its 2026-10-24 addendum, as the triage did. The harness names the model but not the effort.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)
Inputs read in full: `android-game-dev-d15-trace.md`, `qa-engineer-d15-probe.md`, `tech-lead-device-pass-triage.md` (my own §1.3/§1.4); `Q-PUBLISH-3.0.0-draft.md` lines 36–48 and the READINESS rows D-10 and D-15.

## Summary
- **(a) The probe's FAIL is VOID, an instrument fault.** It is neither a product FAIL nor Major. The probe ran on a bench that the host had throttled: macOS App Nap halved the emulator's speed in both conditions. My pre-registered guest-speed control could not see this, because its baseline was the ON launches, and they were napped too. The no-shopping rule binds a valid measurement; it does not bind a measurement taken on a broken instrument. No code loop opens, and D-13 stays a 3.0.1 item.
- **A re-measure is still required before the merge to main.** The trace's healthy numbers do not replace it, for three reasons. They are diagnostic data from the code's author, not QA's independent run. No un-napped OFF block followed a UI switch. Un-napped ON is a single block of 5 launches. The re-measure costs about 75 minutes and fits this week.
- **(b) The re-measure is pre-registered in §2.** The qa-engineer runs it once, with three changes from §1.4: a nap guard, an absolute guest-speed gate, and a cold reboot after every switch. The pass line is unchanged. My expectation from the trace is a PASS; the line decides.
- **(c) Two Q-PUBLISH lines are drafted in §3:** the D-15 result, and the by-design cost of frame 1 with animations off. Both stay emulator-only.
- **(d) The host fix is a plugin item, not a product item (§4).** All future emulator timing gets the guards. App Nap is disabled for the emulator only with the founder's consent, and `caffeinate` is not expected to help.

## 1. Ruling on (a)

### 1.1 The probe's FAIL is VOID
The trace shows the following directly, not by inference:
- **The decisive pair.** Q0 → Q1 ran in one boot at scale 0.0 throughout, with no setting changed. Only a Settings visit came between them. OFF went from a 497 ms median with no Davey to 1,374 ms with a Davey in 5 of 5 launches.
- **The speed drop was guest-wide.** A guest-only sha256 of 64 MiB, which does not involve the app, went from 34–45 ms to 63–73 ms.
- **Every emulator thread dropped to priority 4** within 3 s of the guest's touch sound, while the Mac's screen was locked.

Other points of the diagnosis rest on inference, as its author says: that App Nap is the mechanism, and that priority 4 means the efficiency cores. My ruling does not depend on them. It rests on the observed host throttle and the observed halving of guest speed. Either one alone makes the instrument unfit for a timing comparison.

The probe fits the same pattern:
- Block 1 came before any switch. It ran fast: intervals 57–83 ms, TotalTime 446–502 ms, no Davey.
- Blocks 2–6 all followed a switch, and every one ran at about 1 s in both conditions. ON had Daveys too (1,150, 1,380 and 749 ms).

§1.4 voided individual launches on a slow guest. It failed to fire here only because its baseline was relative to ON. That is a flaw in my probe design, not evidence about the app. The precedent is J-5 attempt 1, which was VOID on the same slow regime. The whole probe is therefore **VOID**, and §1.3's FAIL route (Major, code loop) does not trigger.

### 1.2 Why frame 1 is longer with animations off: by design, not a defect
- On a healthy bench, OFF frame 1 takes 280–312 ms and ON frame 1 takes 229–255 ms.
- The difference is about 50 ms of RenderThread work. OFF compiles 23 Skia programs on frame 1, against ON's 4. That is because OFF draws the settled Home on frame 1, which the spec requires ("content appears settled").
- ON pays more shader compilation in total (157–163 ms against 99–121 ms), but after first display.
- Main-thread work in frame 1 is the same in both conditions. The reduced-motion gate costs 0.5–1.2 % of main-thread samples.
- Moving the work out of frame 1 would mean showing an unsettled first frame, against the spec. **No fix is wanted, and none is ruled.**

### 1.3 Why the trace does not close §1.4 by itself
- **Independence.** §1.4 is QA's acceptance measurement of a release build. The trace was run by the role that wrote the code (standing rule 1). Its numbers are good diagnosis, and I accept them as diagnosis. Accepting them as the pass evidence would let the author supply its own acceptance.
- **Coverage.**
  - The un-napped OFF launches (P0a, P0b, Q0; 15 kept) all came before any UI switch.
  - The only un-napped ON block is R0, with 5 launches, taken after a switch and a reboot.
  - The question D-15 raised was the behaviour after a switch, and no un-napped OFF block after a switch exists.
  - §1.4 asks for 12 kept launches per condition.
- **Cost against risk.**
  - The re-measure is about 75 minutes of bench time.
  - A FAIL after `v3.0.0` is tagged would force a 3.0.1 before any upload. That is §1.3's original reason, and it still holds.
- **What already stands:** the diagnosis (§1.1, §1.2), and D-15's other clauses, which QA judged PASS: backdrop placement, the fade after the entrance, and the static scene with animations off.

### 1.4 D-15's status now
**Not a defect, and not Major.** The probe is VOID (host App Nap). The re-measure in §2 is pending, and the merge to main waits only on that sequencing step. D-13 is unchanged: it stays a 3.0.1 item, `bugfix/hint-focus`, because no code loop opens on `release/3.0.0`.

## 2. The re-measure (qa-engineer; pre-registered here; run once)
It is §1.4 with the guards below. Anything not restated here is as §1.4 says.

**Who and inputs:**
- A fresh qa-engineer dispatch (Opus under Q-019 answer 3, recorded in its header).
- Inputs: this section, plus the method sections of `qa-engineer-d15-probe.md`.
- It may reuse the trace's helpers `primon.sh` and `calib.sh` from `.planning/qa/release-3.0.0/d15-trace/` after reading them. They are tools, not evidence.
- It does not need the trace report's reasoning.

**Instrument and host:**
- `Pixel_9a_bench`, the only emulator running. Boot it cold every time with `emulator -avd Pixel_9a_bench -port 5584 -no-snapshot-load -no-snapshot-save -no-boot-anim`. No other flags: no `-no-window` and no `-no-audio`, which would change the instrument.
- No Gradle, Kotlin daemon or worker runs during timed launches.
- `uptime` load ≤ 5.0 at the start and at the end.
- No screenrecord, uiautomator or TalkBack between a boot and the end of its block.
- Record whether the Mac screen is locked at the start and at the end. This is informational only: the guards decide.

**Build:**
- `:app:assembleDebug` of `release/3.0.0` HEAD in a detached worktree, built before the first boot. That worktree's own `--stop` ends its daemon.
- Record the APK sha256. It is expected to equal `3e974016…c4ed96b`. If it differs, record `git diff --stat fb66351 HEAD` outside `.planning` and `docs`. If that diff touches code, the run goes back to me before any launch.

**Install:**
- On boot 1 only: `install -r -d --no-incremental`, then `cmd package compile -m speed -f com.erman.pegsolitarie`.
- Read the ART status after boot 1 and again at the end.
- The saved state is the bench's resting data. Hash it before and after.

**Guard 1, the nap guard (per launch):**
- Run `primon.sh`, logging the qemu process's `ps -M` priority histogram and the host load, every second for the whole session.
- A launch is **VOID (napped)** if any qemu thread reads priority 4 in any sample from 2 s before its `am start` to its log dump.

**Guard 2, the absolute speed gate (per block):**
- Before and after each block, time `head -c 67108864 /dev/zero | sha256sum` in the guest, three runs each.
- All six runs must be ≤ 50 ms. Otherwise every launch of the block is **VOID (slow guest)**.

**Guard 3, a cold reboot after every switch:**
- **Block 1 (OFF):** cold boot (the bench rests at 0.0), install and compile, then wait 180 s idle, with no taps.
- **Each later block:**
  1. In the boot that just finished its block, after the post-block gate, switch Remove animations through Settings › Accessibility › Color & motion, never `settings put`.
  2. Take a screenshot and read the three scales (read only).
  3. `adb shell sync`, then `adb emu kill`.
  4. Cold boot with the same flags, wait for `boot_completed`, then 180 s idle with no taps.
  5. Read the three scales again (read only). Run the pre-block gate, then the launches.

**Order and launches:**
- Six blocks: OFF, ON, OFF, ON, OFF, ON. Five cold launches each. Discard the first launch of each block, leaving 12 kept per condition.
- Per launch, as §1.4: `am force-stop`, 2 s, `logcat -c`, `am start -W …MainActivity`, and the log dump 3 s after `-W` returns.
- Record:
  - TotalTime and LaunchState;
  - `Displayed`;
  - the app pid's Davey lines ≥ 700 ms;
  - the framework interval (`Start proc` → `Configuring clns-9 …base.apk`), recorded but not voiding;
  - the qemu priority read just before `am start` and just after the dump.

**Validity:**
- If more than 4 of either condition's 12 kept launches are VOID, the attempt is VOID. Then make **one** re-attempt of the whole run on fresh cold boots, within its own time box.
- A second VOID attempt comes back to me. The fallback I will rule then is the same protocol with the Mac screen unlocked and the emulator window visible. That needs the founder at the Mac, or the founder's consent to §4's host setting, so the producer queues it as a non-blocking Q.

**Pass line, unchanged from §1.4, over the non-VOID kept launches:**
1. median OFF ≤ median ON + 100 ms;
2. every OFF TotalTime < 2,000 ms;
3. no OFF Davey ≥ 700 ms.

Any miss on a valid attempt is **FAIL**. That FAIL is real: D-15 becomes Major and blocks the merge, and §1.3's route applies (I rule the fix; D-13 folds in). The line binds. No re-run shops for a pass, and the probe's numbers are never re-judged under these guards.

**Time box:** 75 minutes of device time per attempt, from the first boot. Running out means inconclusive, which comes back to me.

**Restore:**
- Switch back to OFF through the Settings UI.
- Install the resting APK `b2ee555f…` with `-m speed -f`.
- Leave the scales at 0.0/0.0/0.0, with the app data byte-identical.
- `sync`, then `emu kill`. Remove the worktree.

**Report:** `.planning/reports/release-3.0.0/qa-engineer-d15-remeasure.md`, in the probe's field shape, plus the guard columns: priority before and after each launch, and the gate times per block.

**Outcomes:**
- **PASS:** D-15 PASS. The release-manager fills in §3's line, and the merge to main proceeds.
- **FAIL:** as above.
- **VOID twice:** back to me, as above.

## 3. Q-PUBLISH-3.0.0 lines (release-manager)
Replace the D-15 bullet of `Q-PUBLISH-3.0.0-draft.md` (line 48) with:

> - **Cold start with Remove animations on (D-15), emulator only:** on `Pixel_9a_bench` (debug build), the median cold start was `<OFF median>` ms with animations off against `<ON median>` ms with them on, the slowest `<OFF max>` ms, and no frame ≥ 700 ms. A first probe was voided: the studio Mac's App Nap halved the emulator's speed behind a locked screen, in both conditions. No phone was measured (D-10).

Add, under the same release device pass block:

> - **First frame with Remove animations on (by design):** Home appears settled on its first frame, so that frame draws all of Home at once. On the emulator it was about 50 ms longer than with animations on (about 300 against 250 ms, debug build, empty shader cache). The whole start is not longer: with animations on, the same drawing work happens just after Home appears. A phone's shader cache and GPU driver should make it smaller. This was not measured on a phone.

The D-10 bullet ("cold start ≤ 2 s on a mid-range phone … emulator figures only") stays as written. The two lines above add to it; they do not replace it.

READINESS row D-15 (`.planning/release/3.0.0/READINESS.md:117`): the release-manager replaces the status with this:

> **probe VOID (host App Nap; `tech-lead-d15-ruling.md`); guarded re-measure pending.**

The re-measure's verdict then replaces it.

## 4. Host item (plugin; recommendation only, no host change made)
It goes to the producer, for the 3.0.0 retro or a plugin branch. It is not a product 3.0.1 item.

1. **The guards become standard.** Every emulator timing session in the plugin's QA bench procedure gets Guard 1 (the nap guard) and Guard 2 (the absolute speed gate). A guest-speed control relative to the other condition must never be the only guard again: that is the gap that let this probe through. The same applies to `tech-lead`'s probe pre-registrations.
2. **Prevent App Nap on the studio Mac, with the founder's consent.** This is a change to the founder's machine:
   - First read the emulator's app identity with `lsappinfo info -only bundleid <qemu pid>`.
   - Then `defaults write <that domain> NSAppSleepDisabled -bool YES`.
   - Verify it with Guard 1: priority 31 throughout, behind a locked screen, across a Settings visit with touch sounds.
   - `defaults write NSGlobalDomain NSAppSleepDisabled -bool YES` also works, but disables App Nap for every app. It is not recommended.
   - The alternative is a bench rule: bench sessions only with the screen unlocked and the emulator window visible.
3. **`caffeinate` is not expected to help**, and this is untested. It holds sleep assertions for its own process; it does not stop App Nap of the emulator's process. The trace's host already had display sleep off (`pmset` 0) and was napped anyway. Use it only if Guard 1 shows it working.
4. **Retro finding candidate.** The earlier "post-toggle slow regime" records fit the same pattern: `candy-backdrop/qa-engineer-15e.md`'s jank block, `qa-engineer-j5.md` attempt 1 (VOID), and the original D-15 on `ergoz_studio`. The research-analyst can check each record's host lock state, if it was recorded.

## Outputs
- `.planning/reports/release-3.0.0/tech-lead-d15-ruling.md`: this report, holding the D-15 ruling, the pre-registered re-measure, the Q-PUBLISH lines and the host item. It is the only file committed.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-release rev-parse --abbrev-ref HEAD` and `rev-parse --short HEAD` — pass: `release/3.0.0`, `89470c1`; tree clean.
- `git -C … diff --stat fb66351 HEAD -- . ':(exclude).planning' ':(exclude)docs'` — pass: empty, so HEAD's code is the probe's and the trace's build.
- Read in full: the three input reports. I also read `Q-PUBLISH-3.0.0-draft.md` lines 36–48 and grepped READINESS for D-10 and D-15 — pass: the current D-15 lines are the ones §3 replaces.
- Cross-checks of the trace's numbers against the probe — pass:
  - The probe's block 1 (intervals 57–83 ms, no Davey) matches the trace's un-napped regime.
  - The probe's post-switch intervals (89–227 ms) match the napped regime (120–146 ms medians).
  - The trace's FrameInfo decomposition of the probe's 8 OFF Daveys matches the napped rows.
- Not run: the Check, a build, the emulator. No code changed; this is a read-only ruling. `.planning/log/` was not read.

## Open issues / risks
1. **The merge to main and Gate 2 wait on §2's re-measure.** Release-manager Gate 2 preparation that does not depend on it continues meanwhile.
2. **Risk of a second VOID.** The reboot-after-switch procedure is validated by one boot only (R0). If the bench naps anyway, §2's fallback needs the founder at the Mac, or the founder's consent to the host setting. Either way the producer queues a non-blocking Q.
3. **Inferences carried, not relied on:** App Nap as the named mechanism, and priority 4 as the efficiency cores (§1.1).
4. **Phone evidence stays absent** (D-10). Both §3 lines say so.
5. **Commit trailer.** The commit follows this branch's recent trailer convention, as the session's harness gives it. The plugin 1.0.0 gitflow rule names a single Fable trailer. This is the open question already on file (triage, Open issue 5).

## Handoff
Next: producer —
- **qa-engineer:** run §2 once on `Pixel_9a_bench`, report to `qa-engineer-d15-remeasure.md`.
- **release-manager:** set READINESS D-15 to §3's interim status now, and place §3's frame-1 line in the Q-PUBLISH draft now. Fill in the D-15 line from the re-measure's numbers on a PASS.
- **producer:** take §4 to the 3.0.0 retro or a plugin branch, and §4.2 to the founder as a non-blocking Q (`Blocks: nothing`, unless §2 is VOID twice).
- **android-game-dev, code-auditor:** nothing (no code loop).

**D-15 status:** not a defect and not Major. The probe is VOID (host App Nap). The §2 guarded re-measure is pending; its PASS closes D-15, and its FAIL on a valid attempt makes D-15 Major.
**Merge to main:** not yet unblocked. It waits only on §2's PASS, a sequencing step, not a defect.
**Gate 2:** 3.0.0 can go to Gate 2 after §2 passes, the merge to main and the tag, with §3's two D-15 lines and the existing D-13 3.0.1 line disclosed.

VERDICT: PASS WITH CONDITIONS — the probe's FAIL is voided (instrument fault), D-15 is not a defect, and the merge to main proceeds on a PASS of the §2 guarded re-measure.
