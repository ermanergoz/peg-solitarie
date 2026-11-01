# tech-lead report — release-3.0.0: device-pass triage (D-13, D-14, D-15, the Opus run)
Date: 2026-11-01
Branch: release/3.0.0 (report only; HEAD `33eefb3`, whose product code equals `a7c52ab`, the build QA ran: `git diff --stat a7c52ab 33eefb3` outside `.planning`/`docs` touches only `THIRD_PARTY.md`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The snapshot `.claude/rules/VERSION` is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; `.claude/rules/lessons.md`. Skills: android-architecture, android-gradle-logic.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh; Opus under FOUNDER.md Q-019 answer 3 and its 2026-10-24 addendum.
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot; `.planning/recall/RECALL.md` does not exist)

## Summary
- **D-15 is not a product result.** The slowdown hit framework phases that run before any app code, and system_server logged "Slow operation" in the same seconds. The guest was slow, not the animations-off path, which does strictly less work. It does not block as a defect. A defined bench probe (§1.4, under 1 h) runs before the merge to main.
- **D-13 is a product defect, Minor (accessibility).** `HintControl` swaps between two `IconButton` call sites when Hint enters and leaves Working. The focused semantics node is destroyed, and TalkBack falls back to "Navigate back". It does not block 3.0.0. It becomes 3.0.1 item `bugfix/hint-focus` with a Q-PUBLISH line, and is folded into 3.0.0 only if a code loop opens on the release branch anyway.
- **D-14 was a wording error.** The banner has no Dismiss, in code or in spec. The corrected wording is in §3. On it, D-14 is **PASS** on QA's existing 23k evidence, with no re-run.
- **The qa-engineer's Opus run stands** under Q-019 answer 3 and the 2026-10-24 addendum.
- **3.0.0 can go on to Gate 2** once the D-15 probe passes. D-13 is a disclosed 3.0.1 line. D-14 is a doc correction.

## Outputs
- `.planning/reports/release-3.0.0/tech-lead-device-pass-triage.md` — this report: the rulings, the probe and the corrected wording. It is the only file committed.

## 1. D-15 — the animations-off cold start
### 1.1 What the evidence shows
I read QA's `speech-raw.log` (`.planning/qa/release-3.0.0/`) for all six cold starts of the session. For each, I timed the framework milestones that come before `Application.onCreate`, which no app code can influence:

| Start proc | Scales | Collector → zygote-cached loader | Loader → `Configuring clns-9 … base.apk` | `Displayed` |
|---|---|---|---|---|
| 12:48:46 (pid 4987) | 1.0 | 51 ms | 341 ms | +997 ms |
| 12:53:39 (pid 7161) | 1.0 | 29 ms | 263 ms | +876 ms |
| **12:57:51 (pid 8404)** | **0.0** | **293 ms** | **1,133 ms** | **+4,607 ms** |
| **12:58:23 (pid 8725)** | **0.0** | **196 ms** | **1,148 ms** | **+4,468 ms** |
| 12:59:31 (pid 9120) | 1.0 | 23 ms | 258 ms | +837 ms |

- **Framework-only steps ran 4–10× slower in both slow starts.** They are the class-loader set-up inside `bindApplication`. Every later phase was slower by the same 4–5×:
  - the first frame's measure and layout: PerformTraversalsStart → DrawStart was 1,538 ms and 1,582 ms in the decomposed Davey lines;
  - its RenderThread issue took about 300 ms.
  
  No app code path can slow the platform's own class-loader set-up.
- **system_server was slow too.** It logged 13 "Slow operation" lines in minute 12:57, including `startProcess: returned from zygote` at 69 ms, before the app process existed. Five came in 12:47, at the first install. There were none around the fast starts at 12:53 and 12:59.
- **The emulators have a recorded slow regime after a Remove-animations toggle.**
  - `candy-backdrop/qa-engineer-15e.md` jank block: blocks 1–2 ran at about 2 s TotalTime, and "it began after the Remove-animations switch went off just before block 1".
  - `candy-backdrop/qa-engineer-j5.md` attempt 1 was VOID on the same slow regime (N1 median 1,022 ms against 753 ms).
  
  QA switched animations off at 12:56:59. The two slow starts came 52 s and 84 s later.
- **Neither prior launches nor screenrecord explains it.**
  - The slow starts were not the first launches after install. Three cold starts at about 1 s came before them (12:47:39, 12:48:46, 12:53:39).
  - screenrecord ran during the fast 12:48:46 start too.
- **The code agrees.** With animations off, every gated path does less work, and nothing pre-first-frame reads the scale:
  - `rememberAnimationsEnabled()` (`ui/theme/Clay.kt:242`) is a read of the composition's `MotionDurationScale`.
  - `clayEntrance` starts settled.
  - `claySquish` is a no-op.
  - `SceneBackdrop` skips the fade.
  - `BackdropClockDriver` returns before `run()` when `!runs` (`ui/backdrop/BackdropClock.kt:119-129`).
  - The scene itself cannot be on frame 1 (`BackdropSceneReady.kt`).
- **The instrument of record contradicts the FAIL.** `Pixel_9a_bench` at scales 0.0 measured candy's arm B at a 455 ms median TotalTime over 20 kept cold starts (15e).

### 1.2 Ruling
- **Likely cause:** a guest-wide slow regime on `ergoz_studio` after the Remove-animations toggle. That is emulator CPU starvation, not app work.
- **Severity as observed:** none. The FAIL is not a product result.
- **Severity if the probe below reproduces it:** **Major.** It would breach the studio's launch baseline (< 2 s) for every reduced-motion player, and it would block 3.0.0.
- **Likelihood that it is real:** low, on the evidence above.
- **D-15's other clauses stand PASS as QA judged them:** backdrop placement, the fade after the entrance, and the static scene with no fade.

### 1.3 Does it block the merge to main?
- **Not as a defect.** It is a sequencing step: the probe runs before `release/3.0.0` merges to main.
- **Why first:** the probe costs under an hour of device time. A real FAIL found after `v3.0.0` is tagged would force a 3.0.1 before any upload.
- **PASS:** the merge proceeds, and the Q-PUBLISH summary needs no D-15 line beyond its existing emulator-only scope.
- **FAIL:** the merge waits. Route:
  1. The android-game-dev takes one Perfetto trace of a scales-0 cold start on the bench, after the toggle wait.
  2. I rule the fix.
  3. The fix gets the normal review on `release/3.0.0`, and D-13's fix folds into the same loop (§2.4).

### 1.4 The probe (qa-engineer; pre-registered here)
- **Instrument:**
  - `Pixel_9a_bench`, the 15e/J-5 instrument, on a cold boot with `-no-snapshot-load -no-snapshot-save`.
  - It is the only emulator running. No Gradle, Kotlin daemon or worker runs during the timed launches.
  - `uptime` load ≤ 5.0 at the start and the end.
  - `ergoz_studio` is not used.
  - No screenrecord, uiautomator dump or TalkBack during timed launches.
- **Build:**
  - `./gradlew :app:assembleDebug` of `release/3.0.0` HEAD in a detached worktree. HEAD's code equals `a7c52ab`, the build QA ran.
  - Build before the boot, then stop that worktree's daemon. That `--stop` is the QA's own, as in 15e; nobody else runs `--stop`.
  - A release-like (R8) build is not needed for the decision. It could only be faster.
- **Install:**
  - `adb install -r -d --no-incremental`, then `cmd package compile -m speed -f com.erman.pegsolitarie` (15e's precondition 5).
  - Record the ART status once.
  - The bench's resting data, with its one Classic save, is the saved state. Record its hash before and after, as 15e did.
- **Conditions:**
  - OFF = scales 0.0; ON = 1.0.
  - Switch only through Accessibility › Remove animations in the Settings UI, never `settings put`.
  - **After every switch, wait 180 s** with the launcher idle, then read `settings get global animator_duration_scale` (read only).
- **Order:**
  - Six blocks, OFF, ON, OFF, ON, OFF, ON, of five cold launches each. Discard the first launch of each block, which leaves 12 kept per condition.
  - Per launch: `am force-stop com.erman.pegsolitarie`, 2 s, then `am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
  - Record TotalTime, the `Displayed` line, Davey lines ≥ 700 ms for the app pid, and LaunchState (COLD).
- **Guest-speed control:**
  - For every launch, record the framework-only interval: `Start proc …com.erman.pegsolitarie` → `Configuring clns-9 for other apk …com.erman.pegsolitarie…/base.apk`.
  - A launch whose interval exceeds 2 × the median interval of the ON launches is **VOID** (slow regime). It is listed and excluded.
  - If more than 4 of either condition's 12 kept launches are VOID, the probe is VOID. Make one re-attempt on a fresh cold boot (J-5's rule). A second VOID → the probe goes back to me.
- **Pass line:**
  - over the non-VOID kept launches, **median OFF ≤ median ON + 100 ms**;
  - **every** non-VOID OFF launch has TotalTime < 2,000 ms;
  - no non-VOID OFF launch has a Davey ≥ 700 ms.
  
  Any miss is **FAIL**. The line binds, and no re-run shops for a pass.
- **Time box:** 45 minutes of device time after boot. Running out means inconclusive, which goes back to me; it is not a reason to continue.
- **Restore:** leave the bench at scales 0.0, as found, then `sync`, then `emu kill`. Report in 15e's field shape.
- **Report path:** `.planning/reports/release-3.0.0/qa-engineer-d15-probe.md`.

## 2. D-13 — focus leaves Hint after a double-tap
### 2.1 Ruling: a product defect, not platform behaviour
- **D-TBd does not cover it.** D-TBd records where TalkBack puts focus when a *dialog window closes*. Here no window opens or closes. Focus moved 1.1 s after the double-tap, with nothing between (QA's D-13 row).
- **23j item 5 is an accepted criterion,** and its bullet 1 reads: "Double-tapping Hint leaves focus on Hint". The build fails it.

### 2.2 Cause, from the code
In `app/src/main/java/com/app/pegsolitaire/ui/game/HintControl.kt:56-90`, the two looks are two different `IconButton` call sites in an `if`/`else`:

```kotlin
if (look.recipe.glyph == DockGlyph.Spinner) {
    IconButton(onClick = onHint, enabled = false, modifier = … .semantics { contentDescription = thinkingDescription } …)
} else {
    IconButton(onClick = onHint, modifier = … ) { Icon(contentDescription = stringResource(R.string.hint_action), …) }
}
```

- **What happens:** Compose gives each branch its own group. Entering Working disposes the focused button's `LayoutNode` and creates a new one with a new semantics id. Leaving Working does the same again.
- **What TalkBack does:** it loses the accessibility-focused virtual node and lands on the screen's first stop, "Navigate back".
- **The fourth raise kept focus.** That is consistent with TalkBack's own focus-restore heuristics, and does not count against the cause.
- **Scope beyond the notice:** every Solver-backed hint takes this path, so an off-book *Found* hint loses focus too.
- **Confirmation:** it is the developer's to prove, with a red test first (§2.4).

### 2.3 Severity and the release
- **Severity: Minor (accessibility).**
  - The notice is still spoken once, its order and its Dismiss pass, and no state is lost.
  - Focus moves to a spoken stop at the top of the screen.
  - The worst case is a second double-tap on "Navigate back", which leaves the game. The in-progress game is persisted (resume), so the player can continue.
- **It does not block 3.0.0.** It is a disclosed **3.0.1** item.
- **The ux-designer's severity read** (QA's handoff) is welcome but not awaited. If the ux-designer rules the fix must land in 3.0.0, the producer takes the fold-in route below; nothing else changes.
- **Q-PUBLISH-3.0.0 line (D-13).** It replaces the conditional §16 #30 line, which no longer applies because the notice was raised:
  > TalkBack, hint focus (3.0.1): after a double-tap on Hint, when the hint needs a search, TalkBack focus can move to the screen's first stop, "Navigate back", instead of staying on Hint (3 of 4 on the emulator). The "couldn't work it out" notice is still spoken once; its order and its Dismiss pass.

### 2.4 Fix route
- **The branch:**
  - In 3.0.1, `bugfix/hint-focus` off `develop` after 3.0.0 has merged back.
  - If the D-15 probe FAILs and a code loop opens on `release/3.0.0` anyway, cut it off `release/3.0.0` and fold it into that loop.
- **The fix:** android-game-dev, red first. One `IconButton` call site with `enabled = !solverThinking`:
  - The button's own `semantics` carries the content description in both states: `hint_action` at rest, `hint_thinking` while Working.
  - The lightbulb `Icon` and the spinner become decorative children (`contentDescription = null`), swapped inside the button's content.
  - `dockLook(look)` and `DockActionChip` are unchanged.
- **Tests:**
  - A Compose UI test fetches the Hint node, toggles `solverThinking` false → true → false, and asserts the same `SemanticsNode.id` throughout. It must be red on the current code.
  - The same test checks the node reads `hint_thinking` and is disabled while Working, and reads `hint_action` and is enabled at rest.
- **Review:** code-auditor; then the qa-engineer re-runs 23j item 5, bullet 1, on the emulator with TalkBack. The pass is 4 of 4 raises keeping focus on the Hint stop, plus one off-book Found hint keeping focus. Then my sign-off.
- **tech-design:** a new §16 row for it, added in the next tech-design sync.

## 3. D-14 — the corrected wording
**Fact:**
- `ui/game/NoWinBanner.kt` gives the banner's message `liveRegion = Polite` and no `dismiss` action.
- 23k says the banner's "look, copy, actions and behaviour [are] otherwise unchanged". Its actions are Undo, Restart and Home.
- The actions-menu Dismiss belongs to the Undecided notice (`HintUndecidedNotice.kt:97-99`, 23j item 5), and it passed under D-13.

**READINESS (`.planning/release/3.0.0/READINESS.md`, row D-14). For the release-manager, replace the row's first two cells with:**
> | **D-14** | **§16 #30, the banner's TalkBack half:** at AC-19's English-33 dead position, Hint raises the "no winning move" banner and TalkBack speaks "No winning move — undo or restart" once (23k). The banner has no Dismiss, in spec or in code (23k: actions Undo, Restart, Home, unchanged); its traversal and its Undo and Restart were closed on row TB. The actions-menu Dismiss is the Undecided notice's and is checked under D-13 (23j item 5). *(Corrected 2026-11-01, tech-lead device-pass triage: until then this row asked for a banner actions-menu Dismiss, a wording error.)* |

**tech-design §16 #30 (`docs/tech-design.md`). For the tech-lead's next tech-design sync on `release/3.0.0`, replace the sentence that begins "**Still open: the notice's half** (23j item 5's spoken order, 23d spoken) and the banner's actions-menu Dismiss:" with:**
> **Still open: the notice's half** (23j item 5: its spoken order, its actions-menu Dismiss, and focus staying on Hint; 23d spoken). The banner has no Dismiss (23k: its actions are Undo, Restart and Home, unchanged), so it owes no actions-menu check. *(Corrected 2026-11-01, `release-3.0.0/tech-lead-device-pass-triage.md`: until then this sentence gave the notice's Dismiss to the banner, a wording error.)*

Then append the result:
> **2026-11-01, release device pass (`release-3.0.0/qa-engineer-device-pass.md`, D-13/D-14):** the notice was raised on the emulator; spoken once, its order and its Dismiss PASS; focus staying on Hint FAIL (3 of 4), ruled a Minor defect and moved to 3.0.1 (`bugfix/hint-focus`, a new §16 row) with a Q-PUBLISH-3.0.0 line. The banner's 23k speech PASS. **Closed for 3.0.0** apart from that 3.0.1 item.

The conditional Q-PUBLISH line "the notice's TalkBack traversal: tree-order and speech-output evidence only" in `Q-PUBLISH-3.0.0-draft.md:43` is withdrawn, because the notice was raised. The D-13 line in §2.3 takes its place.

**D-14's result on the corrected wording: PASS, with no re-run.**
- Hint spoke "No winning move — undo or restart" at 13:18:56.7 (QA's D-14 section).
- The banner's traversal, Undo and Restart are row TB's (`talkback-board/qa-engineer.md` 26b; `qa-engineer-device2.md` A2.6; TB-7, B-8, B-9).

## 4. Does the qa-engineer's Opus run stand?
**Yes.**
- rules/studio.md (Model and effort) offers the founder two choices when fable is unavailable: wait, or change the policy.
- The founder chose to change it. Q-019 answer 3 reads "Opus for every Fable-pinned role until 3.0.0 is published". The addendum of 2026-10-24 says "option 3 stands again". 3.0.0 is not published.
- The run records the deviation in its header, as the answer requires.
- It reviews no code, and its verdicts rest on artifacts I could re-read; I did, for D-15.
- This report runs on the same footing.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-release log --oneline -5` — pass: HEAD `33eefb3`.
- `git -C … diff --stat a7c52ab 33eefb3 -- . ':(exclude).planning' ':(exclude)docs'` — pass: only `THIRD_PARTY.md`, so HEAD's code equals QA's build.
- `grep -nE "Displayed com.erman|Davey|Skipped …|Start proc …" speech-raw.log` — pass: six cold starts, two at +4.6 s and +4.5 s, the rest 0.84–1.08 s.
- A per-pid milestone grep for pids 4987, 7161, 8404, 8725 and 9120 — pass: the table in §1.1. The framework-only phases are 4–10× slower in the two slow starts.
- A Python decomposition of the five Davey lines — pass: frame 1 spends about 1.54 s in measure and layout and about 0.3 s on RenderThread, consistent with a uniform slowdown.
- `grep "Slow operation" | uniq -c` by minute — pass: 13 at 12:57 and none around 12:53 or 12:59.
- `grep "E/screenrecord"` and `ffprobe` of the three D-15 videos — pass: screenrecord also ran in the fast 12:48 start.
- Code reads:
  - `HintControl.kt`, whole file — the cause in §2.2;
  - `Clay.kt:165-250`, `BackdropClock.kt:80-270`, `BackdropSceneReady.kt`, `SceneBackdrop.kt:90-130`, `HomeScreen.kt:400-425` — the animations-off branches do less work;
  - `HintUndecidedNotice.kt` semantics, `GameScreen.kt` focus requesters.
- Spec reads:
  - `docs/ux-spec.md` 23j and 23k (6898-6960), and the D-TBd lines;
  - `docs/tech-design.md` §16 #30;
  - READINESS D-13 to D-15;
  - `Q-PUBLISH-3.0.0-draft.md`;
  - FOUNDER.md Q-019;
  - `candy-backdrop/qa-engineer-15e.md` in full;
  - `candy-backdrop/qa-engineer-j5.md` summary;
  - `candy-backdrop/tech-lead-signoff-final.md` (J-5 and §4 lines).
- Not run, and not needed for these rulings: the Check, a build and JVM tests. No code changed. `.planning/log/` was not read.

## Open issues / risks
1. **The D-15 probe is owed before the merge to main** (§1.4). Its FAIL path makes D-15 Major and blocking, and opens a code loop on `release/3.0.0`.
2. **D-13's cause is from code reading.** The developer's red test in 3.0.1 confirms it. If that test is green on today's code, the cause is elsewhere, and the item comes back to me with the test.
3. **The doc edits in §3 are not applied by this report.** The release-manager owns the READINESS and Q-PUBLISH draft edits, and the tech-lead's next tech-design sync owns §16 #30 and the new 3.0.1 row.
4. **D-13 also touches Found hints** (§2.2). QA's 3.0.1 re-run covers one.
5. **Commit trailer.** This report's commit follows the trailer convention of this branch's recent commits, as the session's harness gives it. The plugin 1.0.0 gitflow rule names a single Fable trailer. That is the same open question that 15e's report (Open issue 5) records.

## Handoff
Next: producer —
- **qa-engineer:** run the D-15 probe of §1.4 on `Pixel_9a_bench` before the merge to main.
- **release-manager:**
  - apply the READINESS D-14 wording (§3);
  - add the D-13 Q-PUBLISH line (§2.3) to `Q-PUBLISH-3.0.0-draft.md`;
  - withdraw its conditional §16 #30 line.
- **tech-lead (tech-design sync):** §16 #30's correction and result (§3), and a new §16 row for `bugfix/hint-focus` (3.0.1).
- **ux-designer:** optional severity read on D-13, not awaited (§2.3).
- **android-game-dev (3.0.1, or folded in on a D-15 FAIL):** `bugfix/hint-focus` per §2.4.

**Rulings, one line each:**
- **D-15:** not a product result: a guest slow regime on `ergoz_studio` after the toggle. It is not a defect and blocks only as a sequencing step: the §1.4 bench probe runs before the merge to main. A probe FAIL makes it Major and blocking.
- **D-13:** a product defect, Minor accessibility, caused by `HintControl`'s two `IconButton` call sites. It does not block 3.0.0. It becomes 3.0.1 `bugfix/hint-focus` with a Q-PUBLISH-3.0.0 line, folded into 3.0.0 only if a release-branch code loop opens anyway.
- **D-14:** a wording error. The banner has no Dismiss. The corrected READINESS and §16 #30 text is in §3, and D-14 is PASS on QA's 23k evidence.
- **The Opus QA run:** stands, under Q-019 answer 3 and the 2026-10-24 addendum.
- **Gate 2:** 3.0.0 may proceed to Gate 2 once the D-15 probe passes, with D-13 disclosed as a 3.0.1 line.

VERDICT: PASS WITH CONDITIONS (the D-15 probe before the merge to main; the D-13 Q-PUBLISH line; the §3 doc corrections)
