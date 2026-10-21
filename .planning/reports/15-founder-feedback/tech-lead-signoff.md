# tech-lead report — phase-15 ux-refresh: sign-off before the merge into develop
Date: 2026-10-18
Branch: feature/phase-15-ux-refresh (committed by the producer). Reviewed: `develop...e1875cb` (merge-base `2397732`). The later commits, `c50962d..c33564d`, are records only.
Skills loaded: ergoz-soft:standards (role tech-lead). The product's snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. Also the product's `.claude/rules/lessons.md`. Then android-architecture, android-gradle-logic.
Ran in parallel with: phase-14 R-1 qa-engineer review (`PegSolitarie-r1`), candy-backdrop design directions (ux-designer)
Ran on: claude-opus-5-5 at effort xhigh (the agent file's pin). **Staffing deviation:** the tech-lead is pinned to fable. This ran on Opus under the founder's Q-014 (phase-15 rulings, 2026-09-29) and Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), both in `.planning/questions/FOUNDER.md`.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/15-founder-feedback/code-auditor-loop3.md`, `qa-engineer-loop3.md`, `code-auditor.md`, `code-auditor-loop2.md`, `tech-lead-fonts.md`; `.planning/reports/phase-15/qa-engineer.md`, `code-auditor.md`, `code-auditor-s9.md`; `PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-architecture.md`; `PegSolitarie-r5/.planning/reports/phase-14/tech-lead-r2-gate-brief.md`; `.planning/questions/FOUNDER.md`; `.planning/reports/STATUS.md`
Lessons applied: none (the snapshot holds 0 active lessons, the plugin's `memory/LESSONS.md` holds none, and `.planning/recall/RECALL.md` does not exist)

VERDICT: HOLD

## Summary
- The code is sound. Both loop-3 reviews pass: the Check is green with all 98 tasks executed, in two independent runs. The verdict is still HOLD, because four slice-closing items stand. This sign-off ran on Opus, not the pinned Fable, under Q-014 and Q-019.
- No code-auditor has audited S10 (`a4d00ab..49db2ff`), the author's fix for the S9 merge blocker MB-1 and for A-10, so the code-auditor audits it (H-1). F-2 is fixed on this branch (H-2). 15e is sat before this merge on `Pixel_9a_bench`, a new AVD cloned from the Pixel_9a with room to install (H-3). The producer shows the founder the screenshots (H-4).
- E-2 is accepted. F-1 is routed: a ux-designer ruling, then android-game-dev, merged into develop before `release/3.0.0` is cut.
- Merge order: release-plan any time. Then phase 15 and R-1, in either order. Then release-docs, check-gates, audio and the F-1 fix, with candy-backdrop last. This reverses my brief's "check-gates before phase 15".
- When H-1 to H-4 close as written below, the HOLD becomes PASS without a new tech-lead dispatch. Any other outcome comes back to me.

## Inputs read in part
I read these in part, and where they bear on a decision I cite the passage:
- `.planning/reports/15-founder-feedback/qa-engineer.md` and `qa-engineer-loop2.md` (their FAIL, E- and O- items and the TalkBack rows).
- The developer reports' Open issues and Handoff sections: `15-founder-feedback/android-game-dev.md`, `-fix1.md` and `-fix2.md`; `phase-15/android-game-dev-s9.md` and `-s5-s8.md`.
- `phase-15/tech-lead-depthsweep.md`, the ruling section.
- `PegSolitarie-r5/docs/tech-design.md` §3 and §10–§16.
- `docs/ux-spec.md` §6.5 (the "Which builds" table and preconditions 1–8) and criterion 15e.
- The S10 commit bodies.
- `HomeViewModel.kt:200-242`, `HomeLayoutTest.kt`, `themes.xml`, `Theme.kt`, `AndroidManifest.xml`, and the AVD `config.ini` files.

I did not read `.planning/log/`.

## Decision 1 — E-1: criterion 15e is not deferred to candy-backdrop; it is sat before this merge, on a new bench AVD

**Ruling.** Phase 15 does not merge with 15e left to the candy-backdrop sitting. One sitting on this branch's own arms comes first, on the instrument below.

**Why the candy sitting cannot stand in for this one:**
1. **It measures something else.** Its arm A is its merge-base with `develop`, and that base must contain phase 15 (candy R3). So that sitting measures the backdrop alone. Phase 15's delta would never be measured, the fonts' blocking load included. That load is the one exception to §6.5's "no asset load" rule on Home's first frame. I allowed it on one condition: that 15e measures it (`tech-lead-fonts.md` Ruling 1, the cold-start row; tech design §11).
2. **The spec makes this slice's sitting a closing condition.** Criterion 15e says "that slice does not close without them". §6.5's "Which builds" table and its 2026-09-29 pointer say the same.
3. **"Decided there" has no date.** Candy waits on the founder's design approval: Q-020 reopened the direction and Q-021 is queued.
4. **Candy needs a working instrument too.** Fixing the instrument now costs nothing extra.

**The instrument: `Pixel_9a_bench`, replacing the Pixel_9a.** This amends Ruling 2's precondition 1. It is a new AVD with the Pixel_9a's hardware profile and system image, read from `~/.android/avd/Pixel_9a.avd/config.ini`:
- `image.sysdir.1 = system-images/android-36.1/google_apis_playstore_ps16k/arm64-v8a/` (16 KB pages);
- `hw.device.name = pixel_9a`, `hw.cpu.ncore = 4`, `hw.ramSize = 2048`, `vm.heapSize = 228`;
- `hw.lcd.width/height/density = 1080/2424/420`, `hw.gpu.mode = auto`.

The one intended difference is `disk.dataPartition.size = 16G` (the 9a has 6G). It is used for no other testing. None of the five other AVDs carries this image, and the host has 52 GiB free.

**Why not free space on the Pixel_9a:**
- **It touches state this product does not own.** The only levers are uninstalling two other products' test installs, or trimming GMS's and other packages' caches on a shared device. GMS also refills its caches in the background, which adds noise mid-sitting.
- **It would still sit on a nearly full partition.** At 90–95% full, Android's storage monitor and f2fs garbage collection can fire between samples, which perturbs the measurement in its own right.
- **The wall comes back.** Other products keep filling that AVD, so candy's sitting and any release sitting would hit it again.
- **Growing its data partition needs a wipe.** The dispatch forbids that, and a wipe would destroy the 9a's Play Games and consent state.

**What the bench keeps from Ruling 2's reasons:**
- the profile and the image, so the same 16 KB pages, cores and RAM;
- it is not the founder's Pixel_9;
- one emulator only.

**What it loses:** history only. The two earlier null floors (57 and 59 ms) were measured on the 9a, but the verdict never used them. Each sitting measures its own floor, and the VOID line (floor > 150 ms) protects the decision.
- **No Google account on the bench.** Both arms then run the same signed-out start path, so the relative delta is valid. An absolute `TotalTime` is not evidence in any case (D-03j).
- **Fallback:** if the bench cannot be created or booted, stop and report to the tech-lead. A sitting on the Pixel_9 or the Pixel_9a is VOID.

**The procedure** is Ruling 2's "The procedure the qa-engineer runs" (`tech-lead-fonts.md`), with these changes and nothing else.

- **P-1 Instrument, once, untimed.**
  1. `avdmanager create avd -n Pixel_9a_bench -k "system-images;android-36.1;google_apis_playstore_ps16k;arm64-v8a" -d pixel_9a`.
  2. Set its `config.ini` to the values above. Record its diff against `Pixel_9a.avd/config.ini`: only the name, the paths and `disk.dataPartition.size` may differ.
  3. Cold-boot it once and complete setup without a Google account.
  4. Run `adb shell cmd package bg-dexopt-job` to completion, so the platform's own compile job cannot run during the sitting.
  5. Install arm B. Pass the app's first-run surfaces once, as a player would: the consent form, the tutorial, and the app's Play Games prompt, dismissed. After this, no first-run surface may appear in a sample.
  6. Seed the 200-level ladder and confirm Home reads "Stars, 401".
  7. Pass AC-F7's discriminator on this install: the "Level 111" ink width is 82 ± 2 dp.
  8. Shut the emulator down. Cold-boot it again for the sitting with `-no-snapshot-save`, then wait as Ruling 2 says.
- **P-2 Arms.**
  - Arm A = `git merge-base develop feature/phase-15-ux-refresh` at the sitting. That is `2397732` today. `develop`'s `3f2af90` adds only `.claude/`, `CLAUDE.md` and `.planning/` files, none of which is in the APK.
  - Arm B = the branch's code head after H-2.
  - A..B is then exactly phase 15's diff, whatever `develop` gains meanwhile, as in candy R3.
- **P-3 Storage, a new precondition 9.** `dumpsys diskstats` must show `Data-Free` ≥ 4 GB at the start and at the end. Record both.
- **P-4 A quiet host.** The producer declares it before the sitting:
  - exactly one emulator: the Pixel_9a, left running at `emulator-5554`, and the Pixel_9 are both shut down;
  - no Gradle build in any worktree: R-1's QA has finished, the audio branch is idle, and no auditor is running;
  - no other agent session is building or measuring.
- **What stays valid after the sitting:**
  - A later `develop` merge-in leaves the sitting standing unless it changes a start-path file: `App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, `res/values/themes.xml` or `AndroidManifest.xml`. R-1 and the docs branches change none of them.
  - A test-only commit does not change the APK, so it leaves the sitting standing too.
- **Verdict:** as §6.5 rules. A FAIL brings the fonts' share back to me before any degradation step (Ruling 2). A VOID is re-run on the bench, never reported as a pass or a fail.
- **Evidence:**
  - `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` gets the tables in §6.5's field shape, plus the `config.ini` diff and both `Data-Free` readings.
  - The qa-engineer's report goes to `.planning/reports/15-founder-feedback/qa-engineer-15e.md`.

## Decision 2 — E-2: the dex/resources scan replaces the airplane-mode run

**Accepted. No literal airplane run is owed, now or at release.**

AC-F7's airplane half was a proxy for one property: no face depends on the network. QA's substitute (`qa-engineer-loop3.md`, AC-F7) tests that property directly, for every environment rather than one:
- Arm B's 30 dex files and its `resources.arsc` contain **0** `com.google.android.gms.fonts` authority strings, **0** `googlefonts` classes and **0** certificate resources.
- Arm A, `develop`, is the positive control: it shows **5, 47 and 3**. The scan finds the downloadable path wherever it exists.
- T1 pins every font as a blocking `ResourceFont` carrying D3's resource ids. The code-auditor's M-d mutation turned it red (`code-auditor-loop3.md`).
- The platform resolves `res/font` from the APK and fetches nothing.

A literal airplane run would add no information. It also needs a radio toggle, which QA may not make on a shared device.

**AC-F7 now reads:** "Both faces render on a cold start (the on-screen discriminators), and the APK contains no downloadable-font path (a dex and `resources.arsc` scan with a positive control)."

**Loop-3 O-1, my error.** My Nunito band, 142.6 ± 2.5 dp, assumed bodyMedium's 0.25 sp tracking. Material3 1.4.0 sets 0.2 sp, and the device's effective tracking varies by string. The band was mis-derived; the face was right. I accept QA's glyph-shape comparison, together with the reading 5 dp beyond Roboto's widest, as AC-F7's Nunito half. Future discriminators use a tracking-free role, such as titleLarge's "Level N".

## Decision 3 — F-2 blocks and is fixed on this branch; F-1 is routed with a deadline

### F-2 (white status-bar icons in the light theme): blocks; fixed on this branch before the sitting (H-2)
**Why here, and why now:**
- **Phase 15 made it conspicuous.** D-03/D-05 took the top bars off Home and gameplay, the two screens this phase exists to improve (`phase-15/qa-engineer.md` O-2; `phase-15/android-game-dev-s9.md`, open issues).
- **The founder will see it.** It shows in every light shot of the Q-013 set, which the founder is to see before the merge.
- **The fix is cheap here.** The HOLD already sends this branch through one more audit and one more device session. Folding the fix in costs one small developer dispatch. A separate bugfix branch would need its own audit, QA run and sign-off.
- **It must be in arm B.** `res/values/themes.xml` is the launch window's theme, so the fix lands before the sitting, not after it.

**The mechanism (android-game-dev, test first):**
- `res/values/themes.xml`: `<item name="android:windowLightStatusBar">true</item>` on `Theme.PegSolitaire`. The attribute is API 23+, and minSdk is 24.
- A new `res/values-night/themes.xml` with the same style name and parent, setting the attribute to `false` and nothing else.
- The window theme is the right place, not a Compose `SideEffect` with `WindowInsetsControllerCompat`:
  - it applies from the window's first frame, before any composition;
  - it adds no code and no SDK call to Home's first composition (§6.5's hard rules);
  - it leaves `MainActivity.kt` (the audio branch's V1 line) and `Theme.kt` (candy's `LocalBackdropColors`) untouched, so neither waiting branch collides with it.
- Dark mode follows the system: `AppTheme(darkTheme = isSystemInDarkTheme())`, and only previews pass the parameter. So a `-night` resource is exact.
- The navigation bar is left alone, with no `windowLightNavigationBar`. The gesture handle adapts on its own, and the three-button bar keeps the platform scrim under targetSdk 36 (candy R1). QA confirms both on the device.
- No other change: not the window background, not the parent theme, not `MainActivity`, not any composable.
- **Test:** a Robolectric test resolves `R.style.Theme_PegSolitaire` under the `notnight` and `night` qualifiers and reads `android.R.attr.windowLightStatusBar` as `true` and `false`. It is shown red before the XML change.

**G-1 rides in the same dispatch, test-only.** It is S1–S8 QA's low test gap: `grep` finds **0** references to `NON_PROGRESSING_TAPS_BEFORE_COACH` under `app/src/test`. The new test:
- asserts that the third consecutive non-progressing tap raises the coach and the second does not;
- references the constant;
- is watched fail with the constant changed.

### F-1 (Home re-lays out ≈ 0.6 s after first paint when the Continue chips arrive): routed, not blocking
**Why not block:**
- QA counted it against no written criterion: 15f and 15r hold on their letter.
- Its fix is a design decision first: what may move when a chip arrives.
- The fix re-opens Home's geometry (15g, 15k, 15p) and possibly its first frame. That would restart the 15e sitting and add one review loop or more to the branch two others wait on.
- `develop` is the integration branch, not a release. The deadline below keeps F-1 out of every shipped build.

**Technical cause, for the ruling:**
- `HomeViewModel.kt:209-238`: the chip collector opens and decodes both resume snapshots before it emits.
- That decode is the WR-02/CR-01 guard against a phantom chip. So the chip state arrives after first paint by design: ≈ 0.6 s on this AVD, later on a low-end phone.
- §6.5 forbids a blocking read before Home's first frame, so no mechanism can bring the chips into frame 1.
- The ruling must therefore assume they arrive late and say what moves when they do. Options include pinning the tiles while the stage absorbs the change, animating the change, or reserving the space per device; that choice is the ux-designer's.
- Showing a chip from the mode list before the decode would bring the phantom chip back. I reject it technically.

**Route:**
1. ux-designer: the ruling, as a §4.2.4/§4.2.2 amendment.
2. tech-lead: a mechanism ruling, only if the design needs more than layout.
3. android-game-dev: `bugfix/home-chip-arrival`, off `develop` after phase 15 merges, test first. A Compose test emits a no-chip `HomeUiState`, then a one-chip one and a two-chip one, and asserts the bounds the ruling pins.
4. code-auditor.
5. qa-engineer: a screen-recorded cold start with one chip and with two, read frame by frame. If the fix changes Home's first frame, it carries its own 15e sitting on the bench (§6.5's "Any later branch" row).
6. tech-lead sign-off.

**Deadline:** merged into `develop` before `release/3.0.0` is cut. Preferably it also lands before candy's developer dispatch, because both branches reshape the same stage. If candy goes first, whichever branch merges second takes `develop` in and re-runs candy's BD-07 three-chip-state checks.

## Decision 4 — the order of the merges into develop

Footprints come from `git diff --stat develop...<branch>`, today:

| # | Branch | When | Why |
|---|---|---|---|
| 1 | `feature/phase-14-release-plan` | Any time, now included, once its reports exist | `.planning/` and `docs/` only; no code. Nothing depends on it, and it depends on nothing |
| 2 | `feature/phase-15-ux-refresh` (after H-1 to H-4) and `feature/phase-14-gate-scope` (R-1, after its QA and sign-off) | **Either order**: first signed off, first merged | See the notes on rows 2 and 4 below |
| 3 | `feature/phase-14-release-docs` | After phase 15 | Before it merges, its `THIRD_PARTY.md` §2/§3/§4/§7 font rows carry the text in `tech-lead-fonts.md` (code-auditor loop-3 MINOR), and the research-analyst's OFL-1.1 verdict line if not yet recorded. Rows for assets `develop` does not hold yet (audio, the Apache-2.0 text) are the release-manager's to place; the release gate reads the file on `release/3.0.0`. It also carries `docs/tech-design.md`, with the supersessions under "Text for other roles' files". Its order relative to R-1 is free |
| 4 | `feature/phase-14-check-gates` | **Cut from `develop` only after both R-1 and phase 15 have merged.** This supersedes brief §0 and tech design §14.1 | See the note on row 4 below |
| 5 | `feature/audio-review` | After phase 15, merging `develop` in first (STATUS; tech design §12, C1) | It runs in parallel with check-gates and the F-1 fix, each in its own worktree. Audio and check-gates both edit `standards.gradle.kts`, in disjoint regions: the second to finish merges `develop` in and re-runs the Check. The wrong-tap half still waits on the haptics ruling I owe |
| 6 | `bugfix/home-chip-arrival` (F-1) | After phase 15, before `release/3.0.0` | Decision 3 |
| 7 | `feature/candy-backdrop` | Last | After the founder's design approval, after merging `develop` in (candy R6), and with its own 15e sitting on `Pixel_9a_bench` as its final step (candy R3, arm A = its merge-base) |

Then the release-manager cuts `release/3.0.0`.

**Row 2, phase 15 and R-1:**
- The two branches share only `app/build.gradle.kts`, in disjoint hunks: R-1 at lines 82 and 242, phase 15 at 379 and 478.
- The branch that merges second takes `develop` in and re-runs the Check before its own merge. A clean merge with a green Check needs no new review; a conflict in a code file goes to the code-auditor.
- If R-1 lands first, phase 15's sitting stands across the merge-in: R-1 touches no start-path file.
- The merge-in will also conflict, mechanically, on `.claude/rules/` (0.5.1 here, 0.6.2 on develop) and on `FOUNDER.md`.

**Row 4, why check-gates now waits for phase 15:**
- **The premise of the old order has lapsed.** `tech-lead-depthsweep.md` ruling 2 put the gate fix first because the merge was blocked anyway: the fix "costs **nothing on the critical path**". The branch was never started. Waiting for it now would hold phase 15, and through phase 15 both audio and candy.
- **The same ruling already allows it.** Its ruling 1 says B1 does not block the phase-15 merge, and every Check since has been green with one build per worktree.
- **The kover bound must be measured on the shipping module.** C6 measures it on `:domain`, and that module ships with phase 15's `TutorialBoards`. Measured before phase 15, the bound would be set on a module the next merge changes.
- The two branches' catalog edits are disjoint.
- Its §7 fuzz timings need no other Gradle build on the host, just like the 15e sitting.

## Conditions before the merge (H-1 to H-4)

**Order:**
1. H-2.
2. H-1 (at least H-2's production delta).
3. H-3.
4. H-4.

H-1 and H-3 never run at the same time, because the sitting needs a quiet host. A test-only follow-up from H-1 may run before or after the sitting, but never during it.

**H-1 — code-auditor. Audit S10 together with H-2's delta.**
- **What S10 is.** Four commits: `a4d00ab`, `90633e8`, `098b403` and `49db2ff`, audited as they stand at HEAD. They are the author's fix for the S9 merge blocker MB-1 and for A-10, plus D-03k parts 1–2. They touch `HomeBandSet.kt`, `HomeScreen.kt`, `HomeProgressRibbon.kt`, `dimens.xml`, `HomeLayoutTest`, `TutorialScreenTest` and `SystemBarInsets.kt`.
- **Why it is owed.** S10 falls outside every audited range:
  - S9: `f9ad048..30a2cad`;
  - S11: `bffe558..d017711`, `cd25522..7cc46bf` and `2cfad41..e1875cb`.
  - It has no executor report: `grep` over `.planning/` for its SHAs or "15-s10" finds nothing.
  - So MB-1 has been closed by its author only, and rules/gitflow.md wants a code-auditor PASS on code that merges.
- **Mutations to show.** Each must turn the named test red:
  - the 15k tests, on the S9 shape;
  - 15l, with the compact threshold read on height only;
  - 15m, with the stacked, glyph-bearing pill on the narrow class;
  - the CTA clearance guard, without `safeDrawingPadding()`.
- **The LEGACY metrics question.** Rule on the S11 developer's own disclosure (`15-founder-feedback/android-game-dev.md`, open issue 1):
  - `HomeLayoutTest` carries no `@GraphicsMode`, and the repo has no `robolectric.properties`. So the suite runs on LEGACY's fake text metrics, while the eight other geometry suites opt into NATIVE.
  - Under NATIVE, and on the device (`qa-engineer-loop3.md`, 15k: "Overflow 0dp"), the 360×640 both-chips column does not overflow. So `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap`, and part 3's scroll test, pass only on fake metrics.
  - Unless the auditor shows otherwise, the developer moves the suite to NATIVE, in the real faces. Parts 2–3 are then asserted conditionally, as the spec states them: if the column overflows, the fold cuts through a band and one short scroll completes the screen; if it does not, the overflow is 0 and every band is whole.
  - This is test-only. No QA re-walk is owed, and the sitting stands.
- **H-2's delta:** the XML diff, the attribute test watched red, and G-1's test watched red.
- **Closes on:** a PASS with the Check green and every task executed.

**H-2 — android-game-dev. F-2 and G-1, as Decision 3 specifies, test first.** On this branch, before the sitting.

**H-3 — qa-engineer, on `Pixel_9a_bench`.**
- First, the 15e sitting by Ruling 2 as amended in Decision 1.
- Then, in the same session, after the sitting:
  - **F-2 on the device.** In light and dark, on Home, gameplay, the catalogue, Results, Settings and the tutorial: the status-bar icons are dark in light mode and light in dark mode, with a contrast reading. The navigation bar is unchanged, in both gesture and three-button modes.
  - **S1–S8's F-2 re-checked on a device.** No device has yet confirmed the S9 fix. With animations off, one tutorial jump and one gameplay jump each log two `dumpsys audio` events, 60 ms apart: Jump, then PegRemove.
  - **The light founder screenshots, recaptured.** The eight light shots (`founder/01, 03, …, 15`). The bench is not signed in to Play Games, so the Settings shot shows that row signed out.
- **Closes on:** 15e PASS, every device check PASS, and the shots recaptured. A 15e FAIL comes to the tech-lead, fonts first. A VOID is re-run.

**H-4 — producer. Show the founder the screenshots before the merge** (Q-013's own answer; AC-F10).
- The 16 shots in the real faces, with the light ones taken from H-3.
- Say what stills cannot show: F-1 is routed, with its deadline.
- This is not an approval gate. Q-013 asks that the founder sees the shots; an objection becomes a new question.

**The HOLD becomes PASS without a new tech-lead dispatch** once:
- H-1 is a PASS;
- H-3 records 15e PASS and its device checks pass;
- H-4 is done.

The producer then records it in STATUS, citing `code-auditor-loop4.md` (or whatever path the producer gives the H-1 audit), `qa-engineer-15e.md` and this report. Any other outcome returns to me.

## Every FAIL raised in an earlier round: closed or routed

| Round (report) | Item | On this branch | Owner if open |
|---|---|---|---|
| Design, tutorial audio (`phase-15/qa-engineer-tutorial-audio.md`) | FAIL: a blanket-silent tutorial contradicts SOUND-01 | **Closed.** Fixed by the D-01i amendment (`phase-15/ux-designer-audio-amendment.md`); 7d and 7e PASS on the device (`phase-15/qa-engineer.md`) | — |
| S1–S8 QA, F-1, HIGH (`phase-15/qa-engineer.md`) | Skip sits under the status bar, so tapping it does nothing | **Closed.** `f9ad048` adds `safeDrawingPadding()` on the column. Removing it turns the guard red (`code-auditor-s9.md`, probe 2). "Skip works" on the device (`15-founder-feedback/qa-engineer.md:61`) | — |
| S1–S8 QA, F-2, HIGH | The peg-removal cue is lost when OS animations are off | **Closed in code.** `3400828`; reverting it turns four animations-off tests red (`code-auditor-s9.md`, probe 1). **The device re-check is owed: H-3** | qa-engineer |
| S1–S8 QA, F-3, MEDIUM | Criterion 15 fails on a fresh profile | **Closed.** By S9's slack, then S11's stage. Loop 3, 15g PASS: tile 1's top at 557–605 dp, below the 476.8 dp midpoint, in all four chip states | — |
| S1–S8 QA, G-1, LOW | Criterion 4's threshold has no test | **Open**: 0 test references → **H-2** | android-game-dev |
| S1–S8 QA, G-2, MEDIUM | Nothing tests the animations-off path | **Closed.** S9's four tests, probe 1 | — |
| S1–S8 QA, 15a | 360×640 with both chips must not need a scroll | **Superseded.** 15h, then 15k (`phase-15/ux-designer-compact-home.md`). 15k device PASS in S11 loop 1 and loop 3: overflow 0 dp | — |
| S1–S8 code-auditor, B1 (`phase-15/code-auditor.md`) | The Check is red: the `depthSweep` result-store wedge | **Closed for the definition of done.** Every Check since S9 is green; loop 3 executed all 98 tasks, twice. **The mechanism (M1/M2) is routed** to check-gates C1/C2, after R-1 and phase 15 merge | android-game-dev, via the producer |
| S1–S8 code-auditor, B2 / QA 15e | Cold start not run | **Open → H-3** (Decision 1) | qa-engineer |
| S1–S8 code-auditor, B3 | 15f unsettled | **Closed.** Loop 3 15f PASS on its letter: a screen-recorded cold start with no spinner and no face swap. Its layout jump is F-1, routed (Decision 3) | — |
| S1–S8 code-auditor, criterion 15 | The fixed spacer | **Closed** with F-3 | — |
| S9 code-auditor, MB-1, blocker (`code-auditor-s9.md`) | 15h was certified by a tautology | **Fixed by its author** in `098b403`; 15k PASS on the device. **Never audited → H-1** | code-auditor |
| S9 code-auditor, A-10 | The CTA guard is not a guard | **Fixed by its author** in `49db2ff`. **Never audited → H-1** | code-auditor |
| S11 r1 code-auditor, F-1, MAJOR (`15-founder-feedback/code-auditor.md`) | The dock's drawn fill, ring and shadow are never observed | **Closed** in loop 2: mutations F, F2 and F3 turn red | — |
| S11 r1 code-auditor, F-2 and F-3, MINOR | 18b measures a wrapper; 15n/15o read semantics only | **Closed** in loop 2: B, B-strip, B-dock, H, H2 and H3 turn red | — |
| S11 r1 QA, F-1 (`15-founder-feedback/qa-engineer.md`) | 18d: the timer pill jitters | **Closed.** `TimerDigits`. Loop 3 on the device: the pill keeps one width at five readings, at two scales, on two windows, and across a live scale change | — |
| S11 r1 QA, E-1 | 15p's hero reading | **Closed.** D-03m. Loop 3: FLOOR72 turns red, FLOOR48 stays green; 16 dp on the device | — |
| S11 r2 code-auditor, N-1, MAJOR (`code-auditor-loop2.md`) | 18e cannot see a sideways clip | **Closed** in loop 3: T8 turns red | — |
| S11 r2 code-auditor, N-2 | One TalkBack node and RTL order are untested | **Closed** in loop 3: T3 and T5 turn red | — |
| S11 r2 QA, F-A, F-B, F-C (`qa-engineer-loop2.md`) | Fonts never load; a corrupt certificate; 18d tests blind to the face | **Closed** in loop 3. M-a to M-d and M2 turn red; the AC-F4 grep finds 0 code hits; the discriminators pass on the device | — |
| S11 r2 QA, E-1 | 15e precondition 7 | **Ruled** (Ruling 2), then blocked again by storage → Decision 1 | — |
| Loop 3 QA, E-1, E-2, F-1, F-2 (`qa-engineer-loop3.md`) | — | Decisions 1, 2 and 3 | as given there |

## Items deferred to this sign-off: rulings

These were left for this sign-off by STATUS item 8 and by the reviewers' handoffs.

- **A-5 (a `Board` parameter is unstable at the UI boundary): no change.**
  - Kotlin 2.3.20's Compose compiler has strong skipping on by default (it has been since 2.0.20), and this build turns it off nowhere: `featureFlags` and stability configuration appear in neither `build-logic/` nor `app/build.gradle.kts`.
  - An unstable parameter is then compared by instance. `HomeUiState.copy` carries the same `Board` instance across unrelated emissions, so `ResumeTile` and `HomeBoardArt` skip.
  - A stability configuration file is the move only if a profile shows them recomposing.
- **A-7, I-4 and my AC-F6 (kover): not part of this merge.**
  - No kover task exists in this build. AC-F6 named one, and that was my error; AC-F6 is met by the Check as configured.
  - Kover arrives with check-gates C6, which is now ordered after phase 15 (Decision 4).
- **Kotlin warnings as errors** (the 1.0.0 kotlin-android rule; e.g. the deprecated `hiltViewModel` at `HomeRoute.kt:59`): pre-existing and outside this diff. The tech-lead rules on it in the tech design before `release/3.0.0`: either a held deviation for 3.0.0, or a small developer task.
- **A-14 (one `BoxWithConstraints` subcomposition on the start destination):** its cost is inside what 15e measures. No further action.
- **O-5 (no orientation lock): none is added.**
  - Android 16 ignores orientation restrictions on displays ≥ 600 dp wide for apps that target API 36. A lock would therefore give phones one behaviour and tablets another, and the platform is withdrawing the opt-out.
  - The app rotates, as it always has, and criterion 6 holds.
  - Landscape layout quality is a post-3.0.0 item for the ux-designer.
- **O-B (idle Home draws at ≈ 61 fps):** unchanged. It stays a pre-release `compose-performance-audit` item (tech design §10), run after candy, because candy moves the orbs' drift into `graphicsLayer`.
- **Criterion 23 (the hint spinner on a device): settled by `HintControlTest`.**
  - The control is stateless, and the spinner is a pure function of `UiState`.
  - On the device the spinner shows for ≤ 44 ms, shorter than any frame-sampling tool QA has.
  - No device reading is owed.
- **Commit trailers (code-auditor loop 3, INFO): no history rewrite.**
  - A rewrite would change every SHA that every review of this branch cites.
  - The commits predate the trailer rule in this product's 0.5.1 snapshot.
  - The merge commit follows the current rule. That is the producer's.
- **The missing S10 executor report:** not a merge condition. The commit bodies carry the red-first evidence (`a4d00ab`, `90633e8`, `098b403`, `49db2ff`). It is a process finding for the retro (producer).

## Routed after the merge: owner and deadline
| Item | Owner | Deadline |
|---|---|---|
| F-1, the Home layout jump when chips arrive (Decision 3) | ux-designer → android-game-dev → code-auditor + qa-engineer → tech-lead | merged before `release/3.0.0` is cut |
| How the timer is announced (O-A, A-8, I-2: TalkBack likely speaks "Elapsed time" without the value), plus criterion 26's spoken-order half | ux-designer rules the spoken form; qa-engineer captures TalkBack's speech output (the "Display speech output" path S11 loop 1 used); android-game-dev, if the ruling changes code | before the `release/3.0.0` QA closes |
| O-2 / R-3: at 200 % the compact pill labels break mid-word, worse in Nunito | ux-designer | before `release/3.0.0` |
| Criterion 27's remainder: contrast measured app-wide, both themes | qa-engineer | `release/3.0.0` QA |
| `THIRD_PARTY.md` font rows and the OFL-1.1 verdict line | release-manager / research-analyst, on `feature/phase-14-release-docs` | before that branch merges (Decision 4, row 3) |
| AC-F5's release half: the AAB keeps the five fonts (`bundletool dump resources --bundle <aab> \| grep 'font/'`) | release-manager | on `release/3.0.0` |
| `docs/ui/DESIGN-SYSTEM.md` §Typography text (`tech-lead-fonts.md`) | ux-designer | non-blocking, before release |
| Fold Decision 1 into `docs/ux-spec.md` §6.5 and criterion 15e (text below) | ux-designer | non-blocking: this report governs until it is applied (the tech design §16 item 11 precedent). Not committed while H-1 is reading the diff |
| `docs/tech-design.md` supersessions (text below) | tech-lead, on `feature/phase-14-release-docs` | before that branch merges |
| The B1 mechanism: result-store self-heal and lock | android-game-dev, check-gates C1/C2 | before `release/3.0.0` |
| Kotlin warnings as errors (above) | tech-lead | before `release/3.0.0` |
| O-1 (the Challenge tile shows a glyph, not board art), O-4 (a first run stacks three surfaces) | ux-designer | non-blocking; post-3.0.0 unless the producer pulls them in |
| `.planning/qa/` is over 200 MB (loop 3, O-3) | producer | after this task's Done line (rules/testing.md); the bench session's artifacts follow the same rule |
| Retro items: an unaudited slice reached a merge decision (S10); a shared AVD was allowed to fill up; my discriminator band ignored the role's tracking; my AC-F6 named a task the build lacks; the producer-scheduled gate fix that was never started | producer → retro | the 3.0.0 retro |

## Text for other roles' files

### `docs/ux-spec.md` (ux-designer, house style: superseded wording kept as the record)
**§6.5, precondition 1.** After the 2026-09-29 sentence, add:
> *(Amended 2026-10-18 — tech-lead phase-15 sign-off, Decision 1, on the qa-engineer's loop-3 E-1: the `Pixel_9a`'s `/data` was 95 % full and refused both arms.)* The AVD is **`Pixel_9a_bench`**: the `Pixel_9a`'s hardware profile and system image (`android-36.1` `google_apis_playstore_ps16k`, arm64, 16 KB pages, 4 cores, 2 GB RAM, 1080×2424 at 420 dpi) with a 16 GB data partition, used for no other testing. A sitting on any other AVD is VOID; neither the `Pixel_9` nor the `Pixel_9a` is a substitute.

Keep the 2026-09-29 sentence ("A sitting on any other AVD is VOID; the `Pixel_9` is not a substitute") as the superseded record.

**§6.5, a new precondition 9:**
> 9. `/data` free ≥ 4 GB (`dumpsys diskstats`, `Data-Free`) at the start and the end of the sitting, both recorded. *(Added 2026-10-18.)*

**§6.5, the 2026-09-29 pointer under "Which builds".** Append:
> *(2026-10-18.)* Arm A is the merge-base of `develop` and the branch at the sitting, so A..B is exactly the branch's diff; arm B is the head that also carries the light-status-bar fix (sign-off H-2). A later `develop` merge-in leaves the sitting standing unless it changes a start-path file.

**Criterion 15e, its first paragraph.** Make two replacements, and keep the old words in the amendment note:
- "one AVD (`Pixel_9a`, API 36)" → "one AVD (`Pixel_9a_bench`, §6.5 precondition 1)";
- "Arm A = `develop`" → "Arm A = the merge-base of `develop` and the branch".

**AC-F7** (in `tech-lead-fonts.md`, not the spec): it now reads as Decision 2 above. Nothing to edit in the spec.

### `docs/tech-design.md` on `feature/phase-14-release-docs` (tech-lead)
- **§10, row "Cold-start regression (criterion 15e)":**
  - Replace "`Pixel_9a` is the instrument of record and a `Pixel_9` sitting is VOID" with "`Pixel_9a_bench` (the `Pixel_9a` profile and image with a 16 GB data partition, `/data` free ≥ 4 GB) is the instrument of record; a sitting on any other AVD is VOID; arm A = the branch's merge-base with `develop`".
  - Replace "Runs once, on the phase-15 follow-up slice, with `develop` as arm A" with "Runs once on phase 15 before its merge (`15-founder-feedback/tech-lead-signoff.md` Decision 1)".
- **§14.1, row "R-2 + F1/F2 + F-2":** its "Depends on" becomes "R-1 **and phase 15** merged; cut from `develop` after both (sign-off Decision 4: the kover bound is measured on the `:domain` that ships)".
- **§14.1, row "Phase 15":** its "Depends on" becomes "H-1 to H-4 of `15-founder-feedback/tech-lead-signoff.md`; merges before R-2, in either order with R-1".
- **§14.1, new row "F-1":** Home chip-arrival layout jump. Owner: ux-designer → android-game-dev → code-auditor + qa-engineer → tech-lead. Depends on: phase 15 merged. Deadline: before the release branch is cut.
- **§15, row "Phase 15 and the R-2 branch both edit `gradle/libs.versions.toml`":** its mitigation becomes "Disjoint regions; phase 15 merges first; R-2 is cut after it".
- **§16, new rows:**
  - F-1 (owner as above);
  - Kotlin warnings as errors, the 1.0.0 rule (tech-lead, before the release branch);
  - the timer's TalkBack form (ux-designer).
- **§17, new rows:**

| Superseded | By | Where |
|---|---|---|
| "`Pixel_9a` is the instrument of record" (`tech-lead-fonts.md` Ruling 2) | `Pixel_9a_bench`, same profile and image, 16 GB data partition, `/data` free ≥ 4 GB; the 9a's partition was 95 % full and refused both arms | §10; `15-founder-feedback/tech-lead-signoff.md` Decision 1 |
| 15e's arm A = "`develop`" | Arm A = the branch's merge-base with `develop` | same |
| AC-F7's airplane-mode cold start | The dex and `resources.arsc` scan with a positive control | `tech-lead-signoff.md` Decision 2 |
| "R-2 merges before phase 15, which then merges `develop` in" (`tech-lead-depthsweep.md` ruling 2; `tech-lead-r2-gate-brief.md` §0) | Phase 15 merges first; R-2 is cut from `develop` after R-1 and phase 15 | §14.1; `tech-lead-signoff.md` Decision 4 |

## Outputs
- `.planning/reports/15-founder-feedback/tech-lead-signoff.md` — this sign-off: VERDICT HOLD, the four decisions, the conditions H-1 to H-4 with their owners, the closure table, the routed items and the text for other roles' files. It is not committed; the producer commits it. I edited no other file.

## Checks
- `git -C PegSolitarie-p15 log --oneline --reverse develop..feature/phase-15-ux-refresh` — pass. 101 commits. S10's four (`a4d00ab`, `90633e8`, `098b403`, `49db2ff`) fall outside every audited range.
- `grep -rln '15-s10\|a4d00ab\|90633e8\|098b403\|49db2ff' .planning` — 0 files: S10 has no executor report and no audit.
- `git show --format=%B` on the four S10 commits — red-first evidence recorded in each body.
- `git diff --stat e1875cb feature/phase-15-ux-refresh -- . ':!.planning'` — empty: everything after `e1875cb` is records. So both loop-3 Check runs (`--rerun-tasks`, 98/98 executed; app 1,061, domain 274 plus the depth sweep) cover the head's code.
- `git diff --stat develop...feature/phase-15-ux-refresh -- app/schemas` — empty. `grep -c '^+.*PreferencesKey'` over the `app/src/main` diff — 0. So precondition 7's validity holds.
- `git merge-base develop feature/phase-15-ux-refresh` gives `2397732`. `git log 2397732..develop` shows `e354048` and `3f2af90`: the rules snapshot, `CLAUDE.md` and `FOUNDER.md` only.
- `git diff --stat develop...<branch>` for gate-scope, release-docs, release-plan, audio-review and candy-backdrop — the footprints in Decision 4. R-1 and phase 15 overlap only in `app/build.gradle.kts`, in disjoint hunks (`@@ -82`, `@@ -242` vs `@@ -379`, `@@ -478`).
- `grep -n 'GraphicsMode\|@Config' HomeLayoutTest.kt` — no `@GraphicsMode`. `find app/src/test -name robolectric.properties` finds nothing. 8 test files opt into NATIVE. So `HomeLayoutTest` runs LEGACY (H-1).
- `grep -rn 'windowLight\|statusBarColor\|WindowCompat\|enableEdgeToEdge\|isAppearanceLight' app/src/main` — 0. There is no `res/values-night/`, and `themes.xml` parents `android:Theme.Material.Light.NoActionBar`. `AppTheme` defaults to `isSystemInDarkTheme()`; only previews pass `darkTheme`.
- `grep -rn -i 'strongSkipping\|featureFlags\|stabilityConfiguration' build-logic app/build.gradle.kts` — only Gradle-generated accessors; Kotlin is `2.3.20` (`libs.versions.toml:12`). So strong skipping is the compiler default (A-5).
- `grep -n 'screenOrientation' AndroidManifest.xml` — none (O-5).
- `grep -rln NON_PROGRESSING_TAPS_BEFORE_COACH app/src/test` — 0 (G-1 open).
- `sed -n 200,242p HomeViewModel.kt` — the chip collector decodes both snapshots before it emits (F-1's cause).
- `grep config.ini` on all six AVDs:
  - `Pixel_9a`: `android-36.1` `google_apis_playstore_ps16k` arm64, 4 cores, 2048 MB, `disk.dataPartition.size=6G`, 1080×2424 at 420.
  - No other AVD carries that image: `Pixel_9`, `Pixel_9_clean`, `Pixel_9b_clean` and `ergoz_studio` are `android-36` `google_apis_playstore`; `Pixel` is `android-27`.
  - `df -h ~` — 52 GiB free.
- The Check command was **not run** by me. Both loop-3 reviewers ran `./gradlew --no-daemon --no-build-cache --rerun-tasks check` on `e1875cb`, green with 98/98 executed, and nothing after it is code. A qa-engineer was building in `PegSolitarie-r1`, and the host takes one Gradle build at a time.

## Open issues / risks
- **Staffing deviation.** This sign-off ran on Opus, the developer's model (Q-014, Q-019). It rests on the reviewers' own runs and mutations, not on agreement with the author. The loop-3 code-auditor ran on Fable.
- **The bench's first sitting may VOID.** It is a new instrument with an unknown floor. A VOID costs a re-run, not a decision.
- **15e may FAIL on the fonts' blocking load.** My estimate is 10–40 ms, against a threshold of at least 100 ms. If it fails, the fonts' share comes back to me before any art is removed.
- **H-1 may need more than a test change.** Ordering H-1 before H-3 keeps the measurement on the final APK.
- **F-1 stays on `develop` until its fix lands.** `develop` ships nothing; the deadline holds it out of 3.0.0.
- **Expected mechanical conflicts** when phase 15 merges `develop` in: `.claude/rules/` (0.5.1 vs 0.6.2) and `.planning/questions/FOUNDER.md`. The producer resolves them; no code file is involved.
- **I amended three of my own earlier rulings here:**
  - Ruling 2's instrument and its arm A;
  - AC-F7;
  - the merge order in `tech-lead-depthsweep.md` and `tech-lead-r2-gate-brief.md`.
  Each amendment is recorded in the §17 rows above. Until the tech-lead applies those edits, this report is the governing text.

## Handoff
Next: producer —
1. Dispatch android-game-dev for **H-2**: F-2 and G-1, per Decision 3, on `feature/phase-15-ux-refresh`, test first. In parallel, if wanted, dispatch the ux-designer for the spec fold (docs only, non-blocking).
2. Then the code-auditor for **H-1**: S10 plus H-2's delta, the four mutations and the LEGACY ruling. Give it a new report path, e.g. `code-auditor-loop4.md`.
3. Then declare a quiet host (P-4) and dispatch the qa-engineer for **H-3** on `Pixel_9a_bench` → `qa-engineer-15e.md`.
4. Then **H-4**.
5. Then merge in Decision 4's order, recording the PASS in STATUS as described under "Conditions before the merge".
6. Route the table under "Routed after the merge".
7. Dispatch the tech-lead later for the `docs/tech-design.md` edits on `feature/phase-14-release-docs`.

VERDICT: HOLD
