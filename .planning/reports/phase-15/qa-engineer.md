# qa-engineer report — phase-15 UX refresh

**STAFFING DEVIATION — READ FIRST.** This review was run on **Opus**, the developer's own
model. `agents/qa-engineer.md` sets `model: fable` precisely so that review runs on a different
model from the author (`android-game-dev` is `model: opus`; rules/studio.md standing rule 1).
Fable's allowance is exhausted (HTTP 429) and the founder decided — against the producer's
stated recommendation, and reaffirmed — that review proceeds on Opus
(`.planning/questions/FOUNDER.md` Q-003). **Model independence is therefore absent from this
review.** What was preserved: I received the requirements (`docs/ux-spec.md` §14/§11,
`.planning/ROADMAP.md` Phase 15, `15-PLAN.md` §11) and the diff, and did **not** read
`.planning/reports/phase-15/android-game-dev-*.md` or `.planning/log/`. Every verdict below
that says "device" was taken from the device; every verdict that says "reading" says so.

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (2397732..b49a340; build under test = tree at `9650f3d`,
the four later commits are `docs(15)` only — `git diff --name-only 9650f3d..b49a340 -- app/src domain/src` = 0 files)
Skills loaded: ergoz-soft:standards (rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md — plugin copies read, snapshot is v0.5.1 vs plugin v0.6.2)
Ran in parallel with: phase-14 (other worktrees, untouched)
Ran on: Opus 5 (1M context) at effort xhigh — **not** the `model: fable` its agent file specifies (see deviation above)
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
Hands-on session on a booted `Pixel_9a` (API 36) emulator, 55 screenshots, an audio-event
oracle and a cold-start null experiment. The tutorial's teaching contract is sound and its two
near-identical tap paths behave exactly as ruled. Two device-only failures that no unit test
could have caught: **the tutorial header draws under the system status bar, so a tap on the
visible "Skip tutorial" is a no-op**, and **the jump cue loses its peg-removal layer whenever
OS animations are off** — criterion 7c's own named failure mode, confirmed twice on the
tutorial and once on the real game. Criterion 15 fails on a fresh profile (the founder's "looks
empty" has moved to the bottom third of the screen, not gone). 15a fails as the ux-designer
already ruled. 15e's delta is **UNMEASURABLE** on this host: a null experiment against the same
build reports 59 ms where the truth is 0.

**VERDICT: FAIL**

## Ran on
| | |
|---|---|
| Device | AVD `Pixel_9a`, `emulator-5566`, booted headless by this session and shut down after it |
| API level | 36 (`ro.build.version.sdk`) |
| Screen | 1080×2424 @ 420 dpi = **411×923 dp** (a 6.7-inch-class phone), plus an overridden 720×1280 @ 320 dpi = **360×640 dp** pass |
| Variant | `:app:installDebug` (`app-debug.apk`) |
| Launch component | `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`, resolved from the device (`cmd package resolve-activity --brief`) |
| Other device | `emulator-5560` (`Pixel_9`) was already running and belongs to another session; left untouched |

## Checks

### The Check command
No `.planning/reports/phase-15/gsd-gate.md` exists — this phase was not executed through gsd
waves — so per my procedure I ran the Check myself and record the summary.

- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **pass**
  (BUILD SUCCESSFUL in 11m08s, 98 actionable tasks: 10 executed, 4 from cache, 84 up-to-date —
  matching the plan's 11m09s/11m10s baseline). Log: `check-offline-run2.log`.

**It failed twice first, and the cause was local build state, not the diff.** Recorded because
the next person will hit it:

- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **fail**
  (`Execution failed for task ':domain:depthSweep' > java.io.EOFException`, BUILD FAILED in 11s).
- `… ./gradlew :domain:depthSweep --offline --stacktrace` — **fail**, root cause:
  ```
  Caused by: java.io.UncheckedIOException: java.io.EOFException
      at …SerializableTestResultStore.hasResults(SerializableTestResultStore.java:310)
      at org.gradle.api.tasks.testing.Test.getPreviousFailedTestClasses(Test.java:721)
  Caused by: com.esotericsoftware.kryo.KryoException: Buffer underflow.
  ```
  `domain/build/test-results/depthSweep/binary/{results-generic.bin,output-events.bin}` were
  both **0 bytes** — a truncated store from an interrupted earlier run. Gradle 9.5.1 reads the
  previous store before executing and cannot survive an empty one.
- `rm -rf domain/build/test-results/depthSweep && … :domain:depthSweep --offline` — **fail**
  once more on the first rebuild (`NoSuchFileException: …/in-progress-results-generic.bin`),
  then **green on every run since**, including the full `check` above.
- Attribution: phase 15 touches **no** build file and no `:domain` test but the tutorial board
  (`git diff --name-only develop...HEAD -- '*build.gradle.kts' 'build-logic/' 'gradle/'` is
  empty). This is environment state, not a code defect, and is not a FAIL item — but a CI runner
  that ever interrupts `:domain:depthSweep` will wedge the same way until the directory is
  deleted. Routed to the tech-lead below.

### Tutorial — the full pass (criteria 1, 2, 3, 3a, 4, 5, 6, 7, 7a, 7b)

| Criterion | Result | Evidence |
|---|---|---|
| 1 — advances only on a real jump, no Next/Continue button | **PASS (device)** | T1 advanced only after tapping (0,0) then (0,2); no button exists on any step — the only controls in the tree are `Step N of 3` and `Skip tutorial` (`02`, `05`, `12`, `13`) |
| 2 — both jump axes on a live board | **PASS (device)** | T1's only legal move is horizontal (`05`), T2's only legal move is vertical (`11`, `12`); both were played |
| 3 — immovable peg: selects, no dots, no error/red/dialog, coach escalates and releases | **PASS (device)** | `06` shows T2's middle peg selected with **zero** destination dots and no error surface; `07`, six seconds later, shows the selection ring gone and the coach ring raised on (0,1)→(2,1) — the release clause firing on the idle trigger |
| 3 release clause — **not** released when the held peg **is** the coached source | **PASS (device)** | `08`→`10`: peg (0,1) held while the coach points at (0,1); after the escalation the legal-target dot at (2,1) is still drawn. Proved behaviourally: tapping (2,1) immediately executed the jump (`11`), which is only possible if the selection survived |
| 3a — a different peg while one is held is a no-op | **PASS (device), on both screens** | Tutorial: `03`→`04` (T1) and `08`→`09` (T2) — first peg still ringed, its dot unmoved, no red, no sound (0 audio events, below). Real game: `GameViewModel.onCellTap` selects only while nothing is held (`GameViewModel.kt:817`), and the tutorial's branch order is now identical (`TutorialViewModel.kt:141-148`) |
| 4 — three non-progressing taps increase help, never mention Undo, never block | **PASS (device)** | `13`: three rapid taps on an empty cell raised the coach (source ring + destination ring + connector). The only text on screen stayed the step's own caption, `"Keep jumping. Clear the board down to one peg."` — no Undo, no "stuck", no block |
| 5 — Skip reachable on **every** step; system back does the same; lands where you came from; no re-open | **FAIL (device)** — see **F-1** | System back: **works** (`16` — exited mid-T3 to Home, Home did not re-push the tutorial, and the next cold launch went straight to Home, `seq-08`). Skip: **the visible control does not respond to a tap on itself** |
| 6 — rotation keeps step and board | **PASS (device)** | `14` (landscape) holds T3, the same five pegs, the same coach state, the board unshrunk; `15` restores portrait identically |
| 7 — completion shows one primary button that returns the player where they started; no re-appearance | **PASS (device)** | `39`: one peg left, `"That's the whole game. Go clear a board."`, one gold star, one "Got it". Tapping it landed back on **Settings** (`40`), the screen the replay was launched from |
| 7a — nothing behind the tutorial moves | **PASS (device)** | Home dumped before (`31`) and after a full run **and** a skip (`42`): `Stars, 3` / `Boards, 0/5` / `Level, 2`, `Continue Classic`, `Continue Challenge — Pick up Level 2 where you left off`, tile `Continue — Level 3`… identical strings **and identical bounds**, to the pixel. No star banked, no snapshot cleared, no ladder advance, Results never reached |
| 7b — Settings › How to play always restarts at step 1 | **PASS (device)** | Skipped mid-T3, then Settings › How to play → `Step 1 of 3` on the first board (`33`); repeated later with the same result (`41` precursor) |

### Tutorial audio (criteria 7c, 7d, 7e) — measured, not guessed

No per-cue logcat exists, so I used `adb shell dumpsys audio`'s player-event log as the oracle:
the app's SoundPool appears as one `piid`, and every cue logs `player piid:135 event:started`
with a millisecond timestamp. Calibration on the tutorial with **animations on**:

```
09-28 17:11:46:607 player piid:135 event:started     <- SoundCue.Jump
09-28 17:11:46:667 player piid:135 event:started     <- SoundCue.PegRemove, +60 ms
```
60 ms is exactly `SoundParams.REMOVAL_LAYER_DELAY_MS = 60`. One jump = two events; silence = zero.

| Criterion | Result | Measurement |
|---|---|---|
| 7c — the same pair as the real game, on every step, **with animations off** | **FAIL** — see **F-2** | Animations **on**, one T1 jump: **delta 2**, 60 ms apart. Animations **off** (`window/transition/animator_duration_scale = 0`), five separate completed jumps (T2 once, T3 ×4): **delta 1 every time**. The pitched Jump still fires; the peg-removal layer never does |
| 7d — nothing else on the tutorial makes a sound | **PASS (device)** | Six taps in one probe — select, re-tap to deselect, an immovable peg, an illegal hole, a different peg while one was held, an out-of-play cell — **delta 0**. The completion star likewise added nothing: the final jump + star produced exactly 1 event |
| 7e — CTA and Skip play the house press cues, nothing else | **PASS (device)** | "Got it" → **delta 1**; Skip (tapped at y=155, below the occluded band) → **delta 1**. Both on the Settings replay path |

### Gameplay chrome (criteria 16–24)

| Criterion | Result | Evidence |
|---|---|---|
| 16 — no Material top app bar | **PASS (device)** | `18`: no bar background, no elevation, no title row — a transparent strip holding a circular back chip, the title and a timer pill |
| 17 — visible labelled back control; system back does the same | **PASS (device)** | `Navigate back`, clickable node `[42,295][168,421]` = 126 px = **48 dp**; tapping it returned Home (`27`); system back did the same (`31`) |
| 18 — timer visible and announced | **PASS (device)** | `00:00` with `content-desc="Elapsed time"`, and ticking (`00:02`, `00:09` later) |
| 19 — all four actions reachable, working, lower half | **PASS (device)** | `Undo`/`Hint`/`Show possible moves`/`Restart Board` at y 2088–2235 on a 2424 px screen = **86–92 %** of screen height. Undo exercised (`21`→`22`), Hint exercised on three boards, Show-moves toggled twice, all four present on every gameplay screen visited |
| 20 — Hint is not a magnifier; Show-moves shares no glyph | **PASS (device)** | `crop-dock-compare.png`: Undo = curved arrow, Hint = **lightbulb**, Show-moves = peg + two trailing dots, Restart = refresh. No magnifier anywhere |
| 21 — Show-moves state by **fill**, and announced | **PASS (device)** | `crop-dock-compare.png` rows 1–2: ON = filled `primaryContainer` disc + bright glyph; OFF = transparent disc + outline ring + muted glyph. Description flips `"Show possible moves"` ↔ `"Hide possible moves"` |
| 22 — Undo disabled and announced before any move | **PASS (device)** | Fresh board: Undo's clickable node reports `enabled=false` (TalkBack says "disabled") and renders flat, beside two filled controls. After one jump: `enabled=true` (`21`) |
| 23 — hint spinner + disabled + no second search | **reading only, NOT settled on device** | The thinking window is shorter than my sampling: `:domain:depthSweep` records `english33 … maxPerCall=44ms`, and every hint I took returned before a `uiautomator` dump could run, so `hint_thinking` ("Finding a move") was never observable. Asserted by `HintControlTest`; **reported as untested on device** |
| 24 — no allowance count, badge or ad glyph | **PASS (device)** | `18`, `43`, `44`, `45` and `crop-dock-compare.png`: four bare controls, no badge, no "×N", no ad glyph |

### Home (criteria 8, 11, 12, 13b, 14, 15, 15a–15d, 15f)

| Criterion | Result | Evidence |
|---|---|---|
| 8 — no info/help icon anywhere on Home | **PASS (device)** | `17`, `31`, `53`: the only chrome control is the Settings gear |
| 11 — hero band, three pills, tile art | **PASS with one shortfall** | `17`: titled hero with a decorative peg cluster, `Stars · Boards · Level`, and a 33-hole board miniature on the **Classic** tile. The **Challenge tile carries a lightning glyph, not board art** — §4.2 specifies two `BoardCanvas` draws. Cosmetic, recorded as **O-1**, not a FAIL |
| 12 — Level pill and Challenge tile agree; the tile enters that level | **PASS (device)** | `Level, 1`/"Continue — Level 1" → `Level, 2`/"Continue — Level 2" → `Level, 3`/"Continue — Level 3" across three Home visits; tapping the tile entered exactly that level each time |
| 13b — Boards denominator 5; no tutorial board in the catalogue | **PASS (device)** | `28`: exactly five cards — English-33, European-37, Asymmetric 3-3-2-2, Diamond-41, Wiegleb-45 |
| 14 — max system font size, nothing clipped, whole screen reachable by scrolling | **PASS (device)** | `font_scale 2.0`: `48`/`49` — pills wrap label-above-value without truncating, chips wrap to two lines, the Challenge tile's level chip wraps to two lines, everything reachable by scrolling. Gameplay strip at 200 % (`50`): title `[189,302][786,413]`, timer `[828,316][1017,400]` — **the timer is not clipped** |
| 15 — both mode tiles in the lower half of a 6.7-inch phone | **FAIL on a fresh profile** — see **F-3** | With both Continue chips: tiles at `[1553..1805]` and `[1847..2099]` on a 2424 px screen — both fully in the lower half, **passes** (`31`). With **no** resume saved: Classic tile at `[1047..1299]`, centre **1173 vs a 1212 px midline** — its centre is in the *upper* half, and the bottom **34 %** of the screen (y 1593→2424) is empty background (`17`) |
| 15a — 360×640 dp, default font, both resumes: chips **and** both tiles visible without scrolling | **FAIL (device), as already ruled** | `46` at `wm size 720x1280` / `wm density 320`: hero, title, three pills and both chips are visible; the Classic tile is cut off at the fold (its label lands at y≈1216 px = 608 dp) and the **Challenge tile is not on screen at all**. Measured shortfall ≈ **180 dp** (the column needs ≈ 800 dp against ≈ 620 dp of usable height). Everything is reachable by scrolling (`47`) — but 15a asks for *without* scrolling. The ux-designer has ruled this pre-existing and owned by the follow-up slice; recorded with numbers, **not** counted against this build |
| 15b — the Challenge tile keeps the ladder position on its own face, agreeing with the pill | **PASS (device)** | Same evidence as 12 |
| 15c — Classic catalogue in one tap, per-board star pips intact | **PASS (device)** | `28`: one tap from Home; each of the five cards carries `content-desc="0 of 3 stars"`. The Home Stars pill has not replaced them |
| 15d — no readout behind a tap, expander or scroll | **PASS on 411×923 dp; fails as a consequence of 15a on 360×640 dp** | `31`: every readout visible at once on the reference device |
| 15f — placeholder-first, never a spinner or a blank band, no layout shift | **PASS by reading; NOT observable on device** | `HomeViewModel.kt:191-201` fills the ribbon from a **sixth independent collector**, not a `combine`, so the state emits before the query returns; `HomeUiState` starts both values `null`; `HomeProgressRibbon.StatPill` paints `stat_value_unknown` ("—") for `null` and takes its width from the caller's equal `weight`, so a value arriving later cannot reflow the row; a **failed** read is mapped back to `null`, so a player with stars never reads zero (criterion 13a). On device: **no spinner and no blank band were seen in any frame** of a recorded launch (`launch.mp4`, `contact-tail.png`) or a 12-sample timed screenshot ladder (`seq-01`, `seq-07`, `seq-08`). The placeholder window itself is shorter than my best sampling interval (≈ 250 ms), so the "—" frame was never captured — **reported as verified by reading, not by the device** |

### Cold start (criterion 15e) — **UNMEASURABLE at this sitting; delta deferred**

Per the coordinator's mid-task correction I ran only the **null experiment** (the same build
measured twice, true delta = 0 by construction) and did not build `develop`.

Seeding first, because the plan's on-device `sqlite3` path does not exist on this image and its
host-side fallback is incomplete on a WAL-journalled Room database. The WAL-aware form works and
I **verified it on screen** rather than trusting the file: after pushing back, Home read
`Stars, 401 · Boards, 0/5 · Level, 3` (`55-home-seeded-200.png`), matching `SELECT SUM(stars)` =
401 over 200 seeded rows.

`./coldstart.sh` — `adb shell am start -W -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`,
10 samples, first discarded, `am force-stop` + 2 s between:

| Probe | Raw samples (ms), warm-up first | min | median | max | spread | host 1-min load |
|---|---|---|---|---|---|---|
| 1 | *(1924)* 1835 1909 1832 1723 1891 1712 1926 1673 1812 | 1673 | **1832** | 1926 | **253** | 6.43 |
| 2 | *(1926)* 1939 1823 1797 1714 1773 1918 1760 1745 1752 | 1714 | **1773** | 1939 | **225** | 8.49 |

- **Null delta = \|1832 − 1773\| = 59 ms**, where the true delta is zero.
- Within-probe spread 253 ms and 225 ms — **4.9× and 4.4×** the 46 ms range the 50 ms threshold
  was calibrated from (Phase 6, 603–649 ms around 633 ms).
- **The instrument cannot resolve 50 ms on this host.** It reports 59 ms of difference between a
  build and itself against a gate that asks whether a difference is ≤ 50 ms.
- The absolute medians (~1.8 s) are **not** evidence about the app: a `depthSweep` JVM belonging
  to another session held ~100 % of a core throughout (`-DRUN_SWEEP=true`, worker tmpdir under
  this session's scratchpad), plus the second, pre-existing `Pixel_9` emulator at ~44 %.
- **Reusable calibration, and the point of the exercise:** this is the **second independent null
  experiment** on this AVD class. `15-BUILD-EVIDENCE.md` records 957 vs 1014 = **57 ms** at an
  earlier sitting; mine is **59 ms** at a different one, under a different load. Two independent
  nulls agreeing at 57–59 ms say the noise floor of `am start -W` on `Pixel_9a` on this host is
  **≈ 60 ms, not 50 ms** — so the 50 ms threshold is below the instrument's resolution and the
  gate as written cannot be decided by it on a host like this one. The re-take needs either a
  genuinely quiet host or a `:macrobenchmark` module.
- **Criterion 15e: delta DEFERRED to the follow-up branch** (Home's layout mechanism is
  changing, so a number taken against this layout would not be evidence). Not pass, not fail, not
  a number.

### Cross-cutting (criteria 25, 26, 27, 28)

| Criterion | Result | Evidence |
|---|---|---|
| 25 — usable with OS animations off, nothing stuck, the tutorial still pauses after a jump | **PASS for motion, FAIL for its sound clause** | With all three animation scales at 0 I completed T2, all four T3 jumps, the completion CTA, Settings, Home and a resumed Challenge game; nothing was stuck mid-animation and the step-advance beat still let each jump resolve before the board changed (`36`, `37`, `38`, `39`). But 25's own sentence — *"Sound is unaffected by this switch"* — is false here: see **F-2** |
| 26 — ≥ 48 dp, ≥ 8 dp apart, TalkBack-labelled, both themes | **PARTIAL — the TalkBack half is NOT settled** | Geometry and labels from the device: dock targets 147 px = **56 dp** with 73 px = 28 dp gaps; back chip 126 px = **48 dp**; every new interactive element carries a `content-desc` (`Navigate back`, `Elapsed time`, `Undo`, `Hint`, `Show possible moves`/`Hide possible moves`, `Restart Board`, `Step 1 of 3`, `Settings`, `Stars, 3`…). Both themes captured (`52`, `53` light; the rest dark). **TalkBack itself could not be exercised:** `com.google.android.marvin.talkback` installed and enabled fine, but its TTS engine crashed on this image — `dumpsys accessibility` showed the foreground window as *"Speech Recognition and Synthesis from Google keeps stopping"* — and `uiautomator` exposes input focus, not accessibility focus, so no traversal order or spoken string could be read. **The announced-order half of criterion 26 is untested** |
| 27 — 4.5:1 / 3:1 contrast in both themes | **NOT settled** | Both themes captured, and nothing read as obviously low-contrast in the new surfaces, but I made no measurement and will not report a pass without one. One real light-theme problem found by eye is **O-2** below |
| 28 — no sound is the only feedback | **PASS (device)** | Every cue measured had a visual partner: the jump has the slide and the removal fade, the two press cues have their own buttons; the tutorial's silent events (select, deselect, wrong tap, star) all carry visual feedback |

### Reading review of the new tests against §14

Strong and well-aimed. `TutorialViewModelTest` pins both halves of the release clause
(`theCoachEscalationPutsDownAHeldPegItIsNotPointingAt`,
`theCoachEscalationKeepsAHeldPegThatIsItsOwnCoachedSource`), criterion 3a
(`aTapOnADifferentPegWhileOneIsHeldChangesNothingExactlyAsTheGameplayBoardDoes`), the exit
ordering, and the collaborator set by reflection. `TutorialBoardsTest` proves both axes
structurally and that no tutorial board reaches the catalogue. `GameScreenChromeTest` owns
criteria 16–22 and 24. `TutorialSoundTest` asserts both halves of the selectivity. Two gaps:

- **G-1 — criterion 4's threshold is unowned.** `NON_PROGRESSING_TAPS_BEFORE_COACH` is
  referenced by **no** test (`grep -rn NON_PROGRESSING_TAPS_BEFORE_COACH app/src/test/` is
  empty). The escalation is tested, but the count is not: it could be changed from 3 to 1 or to
  10 and the whole suite would stay green. I verified the behaviour on the device; the suite
  should still own it.
- **G-2 — nothing exercises the animations-off path.** `rememberAnimationsEnabled`, `animator
  duration` and any equivalent appear nowhere under `app/src/test/`. `TutorialSoundTest`'s
  `aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` asserts
  `containsExactly(Jump, PegRemove)` with animations enabled only — which is exactly why F-2
  survived to the device. This is the criterion the spec itself flagged as device-only, and it is
  the one that failed.

Note in fairness: `skipIsReachableOnEveryStep` passes and is a correct test. A Compose test
composes without window insets, so no unit test could have caught F-1.

## FAIL items

### F-1 — HIGH — The tutorial draws under the system status bar; a tap on the visible "Skip tutorial" does nothing
**Criterion 5** ("Skip is reachable on every step"), §11 touch targets.
**Steps:** clear app data or open Settings › How to play; on any tutorial step, tap the centre of
the "Skip tutorial" label.
**Expected:** the tutorial exits.
**Actual:** nothing happens — the tutorial stays on the same step, and **0** audio events are
logged (the dismiss cue never fires).
**Why:** `TutorialScreen`'s root is a bare `Surface` + `Column` with `.padding(spacing_md)` and
**no** inset modifier. Across the whole UI package only `GameStatusStrip.kt:81`
(`statusBarsPadding`) and `GameActionDock.kt:95` (`navigationBarsPadding`) apply insets:
```
$ grep -rn "statusBarsPadding\|systemBarsPadding\|safeDrawingPadding\|navigationBarsPadding" app/src/main/java/com/app/pegsolitaire/
app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt:8, :95
app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt:11, :81
```
**Measurements.** System bar inset, from `dumpsys window`:
`InsetsSource id=27 type=displayCutout frame=[0,0][1080,142] visible=true sideHint=TOP`.
Skip's clickable node is `[774,43][1038,169]` — **126 px (48 dp) of target, of which 99 px is
inside the occluded band**. Only `y = 143…169` (26 px ≈ **10 dp**) is tappable. The label glyphs
themselves sit at `[806,79][1006,132]`, entirely inside it. The step pips `[42,89][180,121]` are
drawn over the system clock.
**Proof, both directions:** tap (906,106), the label's visual centre → no-op, 0 audio events,
step unchanged (`41-after-skip-back-to-settings.png` still shows step 1). Tap (906,155), the
26 px sliver → Skip fires, **1** audio event, player lands on Settings.
**Worse at scale:** at `font_scale 2.0` the label grows to `[629,63][1006,161]` and is rendered
straight over the signal and wifi icons — illegible as well as untappable (`51`). Present in both
themes (`52` light).
**Mitigating:** system back still exits, so the player is not trapped — which is the only reason
this is HIGH and not critical. D-01d's own words are "a tutorial the player cannot leave is worse
than no tutorial"; the control that promise rests on does not respond to being pressed.
**Screenshots:** `02-tutorial-t1.png`, `41-after-skip-back-to-settings.png`,
`51-tutorial-fontscale-200.png`, `52-tutorial-light.png`.

### F-2 — HIGH — With OS animations off, the jump cue loses its peg-removal layer (tutorial *and* the shipped game)
**Criteria 7c and 25.** This is the failure mode criterion 7c was written to hunt, and it is real.
**Steps:** `adb shell settings put global animator_duration_scale 0` (and the window/transition
scales); complete any jump in the tutorial, or any jump in a real game.
**Expected:** the pitched `SoundCue.Jump` with `SoundCue.PegRemove` layered 60 ms beneath it.
**Actual:** only the Jump. Measured with `dumpsys audio`'s player-event log:

| Surface | Animations | Completed jumps probed | Audio events per jump |
|---|---|---|---|
| Tutorial | **on** | 1 (T1) | **2**, 60 ms apart |
| Tutorial | **off** | 5 (T2 ×1, T3 ×4) | **1** every time |
| Real game (Challenge L2) | **off** | 1 | Select + Jump only; **no** +60 ms event |
| Real game (Challenge L2) | **on** | 1 | Select, Jump, **PegRemove at +61 ms**, then Win at +262 ms and two star pops |

The control pair, animations on, real game:
```
09-28 17:17:22:784 player piid:135 event:started     <- Select
09-28 17:17:23:821 player piid:135 event:started     <- Jump
09-28 17:17:23:882 player piid:135 event:started     <- PegRemove, +61 ms
09-28 17:17:24:144 player piid:135 event:started     <- Win, +262 ms
```
and the same jump with animations off:
```
09-28 17:16:41:219 player piid:135 event:started     <- Select
09-28 17:16:42:254 player piid:135 event:started     <- Jump   (nothing at +60 ms)
```
**Why (reading, offered to the developer, not a fix):** `JumpSoundEffect` is
`LaunchedEffect(transition?.id) { playJump(); delay(REMOVAL_LAYER_DELAY_MS = 60); play(PegRemove) }`.
With animations disabled `runJumpAnimation` takes the snap path
(`BoardCanvas.kt:298-304`), `onAnimationFinished()` clears `animating` almost immediately, the
key becomes `null` and the coroutine is cancelled **inside** its 60 ms delay.
**Attribution, stated plainly:** the effect is shared, and S3 lifted it out of `GameScreen.kt`
unchanged — so this is a **pre-existing defect of the shipped game that phase 15 inherited and
propagated onto a second screen**, not one this phase authored. It is still a criterion-7c
failure on this build, and criterion 25 forecloses the "expected consequence of reduced motion"
reading in so many words. The scope matters for routing: fixing it fixes the shipped game too.
**Test gap:** G-2 — no test exercises the animations-off path, so nothing would have caught it.

### F-3 — MEDIUM — Criterion 15: on a fresh profile the Classic tile is not in the lower half, and the bottom third of Home is empty
**Criterion 15.** With **no** resume saved (a brand-new player — the exact player the refresh is
for), on the 411×923 dp reference device: Classic tile `[42,1047][1038,1299]`, centre **1173**
against a screen midline of **1212** — the tile's centre is in the upper half. Below the Challenge
tile, y 1593→2424 is **831 px = 34 % of the screen** of empty background.
With both Continue chips present the same screen passes (`31`).
This is the fixed-spacer consequence: `Modifier.weight` does not work inside a `verticalScroll`
column, so all slack falls to the bottom instead of above the chips. The ux-designer has already
ruled a `BoxWithConstraints` + `heightIn(min)` + weighted-spacer replacement for a follow-up
slice; recorded here because the device says criterion 15 does not hold today.
**Screenshot:** `17-home-fresh-no-info-button.png`.

### G-1 — LOW (test gap) — criterion 4's three-tap threshold has no test
`NON_PROGRESSING_TAPS_BEFORE_COACH` is referenced by no test. Behaviour verified on device
(`13`); the suite does not own the constant.

### G-2 — MEDIUM (test gap) — nothing exercises the animations-off path
See F-2. `TutorialSoundTest` asserts the jump pair with animations enabled only.

## What the new Home actually looks like — the founder's question

Plainly: **it is much fuller than it was, and it is still noticeably empty at the bottom.**

The top two thirds now earn their space. A decorative five-peg cluster sits above a large
gradient "Peg Solitaire" wordmark; under it a three-pill ribbon reads Stars · Boards · Level at a
glance; under that the two mode tiles are wide, coloured cards with real content — the Classic
tile carries a 33-hole board miniature, the Challenge tile its level chip. On a player with
saved games the two Continue chips slot in above the tiles and the column fills the screen
edge to edge with nothing dead in it (`31-home-both-continue-chips.png`). That version of Home
looks finished, and it is the version most returning players will see.

The complaint is not fully answered for a **new** player. With no saved game there are no chips,
and because the slack all falls to the bottom of the scroll column, the last **34 %** of the
screen — the whole thumb zone, the part of a 6.7-inch phone a player actually holds — is bare
background with a decorative blob drifting through it
(`17-home-fresh-no-info-button.png`). The emptiness the founder named has not been removed so
much as **relocated from the middle of the screen to the bottom of it**, and it lands in the
worst place for a phone. The two mode tiles, which the refresh deliberately wanted in the thumb
zone, float just above the midline instead.

Light theme reads well (`53-home-light.png`) apart from O-2.

## Observations (not FAIL items)

- **O-1 — the Challenge tile has no board art.** §4.2 specifies two `BoardCanvas` draws for the
  mode tiles; the Classic tile has one, the Challenge tile shows a large lightning glyph. Criterion
  11's "board art on the mode tiles" is half met. Cosmetic.
- **O-2 — light theme: the system status-bar icons are white on a light background.** On Home and
  the tutorial in light theme, the clock and the signal/wifi/battery icons are nearly invisible
  (`53-home-light.png`, `52-tutorial-light.png`). The window is not setting
  `isAppearanceLightStatusBars` for the light theme. App-wide (a `MainActivity`/theme concern),
  but newly conspicuous because Home no longer paints a `TopAppBar` behind the status bar.
- **O-3 — a Classic board's status strip is titled "Peg Solitaire", not the board's name.**
  `30-hint-spinner.png` on English-33. Challenge correctly shows "Level N" with
  `content-desc="Challenge level N"`. Possibly unchanged behaviour; flagged for the ux-designer.
- **O-4 — first run stacks three surfaces.** A brand-new player sees the UMP consent form painted
  **over** the tutorial's first board (`01-firstrun-tutorial-t1.png`), then the Play Games sign-in
  dialog on the way back to Home. Phase 9/11 behaviour, not this phase's contract, but it is the
  first thing a new player meets and the tutorial is not the first thing they see.
- **O-5 — landscape.** `AndroidManifest.xml` declares no `screenOrientation`, so the tutorial does
  rotate. It survives rotation correctly (criterion 6), but in landscape the caption falls below
  the fold (`14`). §12 says landscape is unsupported in this refresh and asks that an unlocked
  manifest be flagged to the tech-lead — flagged.
- **O-6 — `:domain:depthSweep` wedges on an interrupted run.** See the Check section. A CI runner
  that ever kills the sweep mid-write will fail every later build with
  `java.io.EOFException` until `domain/build/test-results/depthSweep` is deleted. Worth a
  `doFirst { delete(...) }` or equivalent; tech-lead's call.

## Artifacts

All under `.planning/qa/phase-15/` (git-ignored via `.gitignore:47`; nothing committed).
Total **9.8 MB**, 73 files. The load-bearing ones:

| Path | Size | What it shows |
|---|---|---|
| `01-firstrun-tutorial-t1.png` | 264 KB | First launch: UMP consent form painted over the tutorial board (O-4) |
| `02-tutorial-t1.png` | 88 KB | T1, and the pips + "Skip tutorial" drawn over the system clock and wifi icons (F-1) |
| `03-t1-peg00-selected.png` / `04-…-3a.png` | 140/96 KB | Criterion 3a on T1 — selection and dot unchanged after tapping a different peg |
| `06-t2-immovable-peg-selected.png` | 172 KB | Criterion 3 — immovable peg selects with zero dots, no error, no red |
| `07-t2-coach-escalated-release.png` | 104 KB | The release clause: the held peg is put back down, the coach ring raised |
| `08`/`09`/`10`/`11` | 112/116/112/120 KB | The opposite path — the coached peg is **not** released, proved by the jump executing |
| `13-t3-three-nonprogressing-taps-escalation.png` | 136 KB | Criterion 4 — three taps raise help; caption unchanged, no Undo mention |
| `14-t3-landscape-rotation.png` | 128 KB | Criterion 6 — same step, same board after rotation |
| `17-home-fresh-no-info-button.png` | 140 KB | **F-3** — fresh-profile Home, Classic tile above the midline, bottom 34 % empty; and no info icon (criterion 8) |
| `18-gameplay-chrome.png` | 116 KB | Criteria 16–22 — no top app bar, back chip, timer pill, four-action dock in the thumb zone |
| `crop-dock-compare.png` | 68 KB | Criterion 21 — Show-moves ON filled vs OFF outlined, stacked with the hint state |
| `28-classic-catalogue-star-pips.png` | 184 KB | Criteria 15c/13b — five boards, per-board star pips, one tap from Home |
| `31-home-both-continue-chips.png` | 192 KB | Criteria 15/15d — the full Home with both chips; the version that passes |
| `39-tutorial-completed-cta.png` | 84 KB | Criterion 7 — one peg, done caption, gold star, single "Got it" |
| `42-home-after-tutorial-7a.png` | 184 KB | Criterion 7a — Home identical after a full run and a skip |
| `41-after-skip-back-to-settings.png` | 88 KB | **F-1** — the tutorial still on step 1 after tapping Skip's visual centre |
| `46-home-360x640dp.png` / `47-…-scrolled.png` | 132/96 KB | **15a** — Challenge tile off-screen at 360×640 dp; reachable only by scrolling |
| `48`/`49-home-fontscale-200*.png` | 204/208 KB | Criterion 14 — 200 % font, nothing clipped |
| `50-gameplay-fontscale-200.png` | 124 KB | §11 — the timer is not clipped at 200 % |
| `51-tutorial-fontscale-200.png` | 140 KB | **F-1** at 200 % — Skip rendered over the wifi/signal icons |
| `52-tutorial-light.png` / `53-home-light.png` | 84/200 KB | Light theme, and **O-2** (white system icons on a light background) |
| `55-home-seeded-200.png` | 204 KB | Seeding verified on screen: `Stars, 401 · Boards, 0/5 · Level, 3` over 200 rows |
| `check-offline-run2.log` | 45 KB | The green Check run |
| `depthsweep-retry1.log` / `depthsweep-retry2.log` | — | The two Check failures and the Kryo root cause (O-6) |
| `launch.mp4`, `contact-firstpaint.png`, `contact-tail.png`, `seq-01/07/08.png` | 545 KB + | The 15f launch capture and the timed screenshot ladder |
| `qa.sh`, `audioprobe.sh`, `coldstart.sh`, `tbwalk.sh` | < 4 KB | Session helper scripts — never committed (rules/testing.md) |

## Open issues / risks
- **Review ran without model independence** (Q-003). This report is only as good as one Opus
  agent reading another Opus agent's output; the two device failures were found by the device,
  not by reading, which is some protection — but a Fable re-review of the diff is still owed
  when the allowance returns.
- **Criterion 15e cannot be settled by `am start -W` on a host like this one.** Two independent
  nulls at 57 ms and 59 ms against a 50 ms gate. The re-take needs a quiet host or a
  `:macrobenchmark` module; the phase should not close claiming a delta either way.
- **Criteria 23, 26 (TalkBack half) and 27 are untested**, each for a stated reason. None is
  reported as a pass.
- F-2's scope is the shipped game, not only the tutorial — whoever fixes it should measure the
  gameplay board too.

## Handoff
Next: **tech-lead** — three things to route. (1) **F-1 and F-2 back to the developer**: F-1 is a
missing inset on `TutorialScreen` (one modifier, but it is product code and mine to report, not
to write); F-2 needs the removal layer to survive the snap path, and G-2 needs a test that runs
with animations disabled so it cannot regress. (2) **F-3 and 15a** fold into the Home layout
slice the ux-designer has already specified; my numbers (34 % dead footer at 411×923 dp,
≈ 180 dp short at 360×640 dp) are the acceptance targets. (3) **O-6**, the `:domain:depthSweep`
result-store wedge, is a build-reliability call. Criterion 15e's delta is deferred to the branch
that lands the layout fix, with the ≈ 60 ms noise-floor finding attached so the re-take does not
repeat the same instrument. Producer: criteria 23, 26 and 27 remain owed and need a device or a
tool that can settle them.
