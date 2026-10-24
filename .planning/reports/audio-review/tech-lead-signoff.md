# tech-lead report — audio-review (sign-off: the audio integration, 17 items, 62c774d..8663710)
Date: 2026-10-24
Branch: feature/audio-review (report only; the range under review is 62c774d..8663710, 19 commits; the worktree head ae58e5e adds only the five reports, the sound-designer's two doc syncs and Q-024)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game — the plugin 1.0.0 rule files read in full because the product snapshot is 0.5.1: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; the snapshot's lessons.md), android-architecture, android-gradle-logic
Ran in parallel with: none named by the dispatch (the gates, hint, ux, candy, cv, lic and tb worktrees exist beside this one; no Gradle ran here)
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none
Loops: 0
Read in full: `.planning/reports/audio-review/android-game-dev-integration.md`, `code-auditor-integration.md`, `sound-designer-integration.md`, `qa-engineer-integration.md`, `tech-lead-haptics.md`; `docs/tech/audio-silent-mode.md`; `docs/ux/invalid-tap-feedback.md`; FOUNDER.md Q-024; the production diff of the range (`app/src/main`, `build-logic`, 2,218 diff lines); `docs/tech-design.md` on `develop` §3.5, §8.5, §9.8, §12, §14.1; `.planning/reports/hint-undecided/tech-lead-brief.md` §10 (on `bugfix/hint-undecided`); `docs/ux-spec.md` on `feature/ux-spec-folds`, the eleventh-revision notes and D-TBa
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **PASS** on `62c774d..8663710`: all 17 items are built as the two designs and the haptics ruling specify, the boundaries and the dependency rules hold, no dependency was added, and the main code is ready for C7. The code-auditor (fable, PASS, 22 mutants killed) and the qa-engineer (fable, PASS WITH BLOCKED ITEMS, emulator and the founder's GM6) are cited by path below; the sound-designer's review (PASS) closes rules/audio.md.
- **Merge:** third of the code branches, after `bugfix/hint-undecided` and `feature/phase-14-check-gates`; it takes `develop` in and re-runs the Check. In-memory dry runs show exactly two conflict files, `GameAnim.kt` and `TutorialViewModel.kt`; both resolutions are ruled here so the merge-in is mechanical. **No 15e sitting** for this branch (ruling, §2.4).
- **QA's FAIL** is a pre-existing frozen gate (`rememberAnimationsEnabled()`), not this branch's code: fixed on `bugfix/reduced-motion-live-gate` after the merge, **cut-blocking, not merge-blocking**; the mechanism is ruled (the gate reads the scale Compose's own clock reads), it touches no start-path cost, so no sitting.
- **BLOCKED device items:** none blocks the merge or the cut. The speaker-route and wired halves close in the release QA session if the founder sets the hardware (Q-025 drafted, two minutes, no testing by the founder); the 60 fps phone recording and the P4c listen go to the Q-PUBLISH-3.0.0 summary.
- **Minors:** one bugfix branch carries the live gate, the cue-replay fix (sound-designer F-1), the auditor's finding 1 and the F-2 KDoc; the ux-designer's fold-in list is in §5; WT-19 goldens stay the post-3.0.0 suite; the tech-design sync after the merge is mine and listed in §6.

## 1. What was reviewed, and what holds

**Evidence, by path.**
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/code-auditor-integration.md` — PASS (fable, xhigh): the Check green on the range (1,185 + 274 + 54 tests, 0 failed or skipped; lint 63 warnings, 0 errors); 22 mutation probes all caught; no suppression, baseline, `@Ignore` or `!!`; nine findings, none above Minor; the eight deviations judged and accepted.
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/qa-engineer-integration.md` — PASS WITH BLOCKED ITEMS (fable, high): every emulator-reachable check passed (WT-10, WT-12, WT-13, WT-14, WT-15, WT-18 visual halves, D2, D3, D6, D7, D8, D10, every cue on its event, P9 on develop); the GM6 session (Android 8.1, API 27, Bluetooth route) passed D5's first half, D7, D8, D9's headphone branch, D10's real-OEM case, WT-12 below API 30, WT-14 under TalkBack 16.2 and the mark at 5.31:1; one FAIL, medium, on a pre-existing path (§3).
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/sound-designer-integration.md` — PASS (Opus under Q-019, recorded there): R1–R13 of rules/audio.md and the spec, each with its line; the spec and CREDITS.md prose synced to 8663710 (committed at c169fd2); F-1, F-2, F-3.
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/android-game-dev-integration.md` — the 17-item table with commits and proving tests; 13 deviations, all judged by the auditor.
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/tech-lead-haptics.md` — my brief (Rulings 1–4, the 17-item scope). No code of mine is in the range; nothing here reviews its own work.

**My own read of the diff, against `docs/tech/audio-silent-mode.md`, `docs/ux/invalid-tap-feedback.md`, the haptics ruling and `docs/tech-design.md` §2, §12.** Paths under `app/src/main/java/com/app/pegsolitaire/`.

| Check | Holds | Where |
|---|---|---|
| `audio/` owns playback; the two readers and the pure pieces live there, not in `platform/`, not in `:domain` (design §1.5) | yes | `audio/SystemAudioState.kt` (no `android.*` import), `audio/AudioRouteClassifier.kt` (int constants only; the allowlist and exclusions are §1.3's exactly), `audio/AndroidSystemAudioState.kt`, `audio/AppVisibility.kt`; `domain/` untouched |
| A3/A5/A6/A7: event-driven, three volatile flags, one collector per gate, `catch { emit(false) }` on the system collector only, `silenceNow()` on every closing, the D-12 pop bypasses `soundOn` only | yes | `audio/SoundPoolPlayer.kt:118-131, 152-178, 194-203, 211-220` |
| Register-then-read; `RECEIVER_NOT_EXPORTED`; main-looper `Handler` for the device callback; `getAudioDevicesForAttributes` behind `SDK_INT >= TIRAMISU` with the pool's own attributes; types only; `BECOMING_NOISY` forces `BuiltIn`; lazy `AudioManager`, so no constructor IPC | yes | `AndroidSystemAudioState.kt:98-168` |
| P8 reads `ProcessLifecycleOwner`, never `AppForegroundTracker`; `STARTED` is the line; de-duplicated | yes | `AppVisibility.kt:215-225` |
| V1: `volumeControlStream = STREAM_MUSIC` after `super.onCreate`, with its reason | yes | `MainActivity.kt:140` |
| Ruling 1: `HapticsSetting` with one member; `SettingsRepository : HapticsSetting`; bound from the interface; `TutorialViewModel`'s third collaborator collected on `viewModelScope`; `forStep(index, hapticsOn, rejectedTapTick)` with required parameters, read **inside** the update lambda; no new `TutorialUiState` field | yes | `data/HapticsSetting.kt`, `data/SettingsRepository.kt:25,33`, `di/DataModule.kt:54-62`, `ui/onboarding/TutorialViewModel.kt:80-126, 265-281`, `TutorialUiState.kt:77-114` |
| Ruling 2: the jump haptic lifted verbatim, called from both screens | yes | `ui/game/JumpHapticEffect.kt`; `GameScreen.kt:166`; `TutorialScreen.kt:116` |
| Rulings 3–4: the reject haptic private to `GameScreen`, gated by `hapticsOn && SDK_INT >= R` in our own code, `HapticFeedbackType.Reject`, through `NewOrdinalEffect`; the tutorial passes no `wrongTapCorrection`, attaches no live region | yes | `GameScreen.kt:360-385`, `ui/game/WrongTap.kt:92-95`, `TutorialScreen.kt:258-266` |
| §3.8's negative contract: `BoardCanvas` draws the shake, the mark and the redirect and nothing else; the cue, the haptic and the announcement are screen-level effects | yes | `ui/components/BoardCanvas.kt`, `ui/components/WrongTapFeedback.kt`, `ui/game/WrongTapAnnouncement.kt` |
| Draw phase: per-frame values read through the clock in draw; `Stroke` built in composition; `WrongTapMotion` allocates nothing per call; the shake moves only the tapped cell's own drawing; the nudge multiplies the pulse | yes | `WrongTapFeedback.kt:146-180`, `BoardCanvas.kt:112-122, 479-498, 560-585`, `ui/game/WrongTapMotion.kt` |
| UDF: the ViewModels write the signal (`rejectedTapTick` + `rejectedTapCell`), the canvas only reads it; ending an answer writes no UI state; no mute flag in any `UiState` | yes | `GameViewModel.kt:823-827`, `TutorialViewModel.kt:302-307`, `GameUiState.kt:84-90, 138, 182, 217` |
| Rotation: the board's answer, the haptic and the announcement never replay a carried ordinal (`firstOrdinal`, `OrdinalWatch`) | yes | `WrongTapFeedback.kt:216-226`, `ui/game/NewOrdinalEffect.kt` |
| Dependencies and licences: none added; `gradle/`, `THIRD_PARTY.md`, the manifest untouched (the auditor's `--stat` over those paths is empty) | yes | — |
| Kotlin warnings (C7 readiness): `@param:ApplicationContext` and `@param:ApplicationScope` on every new or touched constructor; the developer's and the auditor's `compileDebugKotlin --rerun` / `compileReleaseKotlin --rerun` each show 9 `w:` lines, all in the nine files check-gates fixes, none in this range | yes | `AndroidSystemAudioState.kt:96`, `SoundPoolPlayer.kt:74`, `TutorialViewModel.kt:86` |
| Suppressions: none added; `@OptIn(ExperimentalCoroutinesApi::class)` in one test with its reason (an opt-in, not a suppression); the pre-existing `LongMethod` annotations unchanged | yes | auditor, Suppression audit |
| Tests deterministic: fakes, paused clocks, `MotionDurationScale` for reduced motion, Robolectric SDK 34 only (seven `@Config` lines, all 34) | yes | auditor, Checks |

Nothing in the range contradicts the tech design or the two designs. The eight developer deviations the auditor accepted are right on the merits; two of them I confirm as tech-lead decisions because they touch the contract: `NewOrdinalEffect` instead of the `> 0` guard (stronger than the house idiom, and the idiom's rotation replay is now sound-designer F-1, §5), and the `delay` hold for the mark (the only hold that survives an animator scale of 0 — mutant 15 proves it is load-bearing).

## 2. The merge into `develop` — PASS, and the merge-in

### 2.1 Order
`docs/tech-design.md` §14.1 on `develop` (c5f2c54), merge order steps 3a, 4, 5: `bugfix/hint-undecided` (R1) first of the code branches, then `feature/phase-14-check-gates`, then this branch. This branch is the second to merge against each of them, so it **merges `develop` in** (`git merge develop`, never a rebase), resolves the conflicts, re-runs its own Check and the other two branches' named tests, and has the merge commit's conflict hunks re-read by the code-auditor and by a tech-lead (§14.1's last paragraph; hint brief §10.3). This report pre-rules every resolution, so those re-reads are a check, not a decision.

### 2.2 What will conflict — measured, not guessed
`git merge-tree --write-tree` (an in-memory three-way merge; nothing was written to any worktree or branch):
- **against `bugfix/hint-undecided`:** one conflict, `app/src/main/java/com/app/pegsolitaire/ui/game/GameAnim.kt` (both branches append constants after `DOT_POP_END`). `GameScreen.kt`, `GameUiState.kt`, `GameViewModel.kt`, `dimens.xml`, `strings.xml` and `GameViewModelTest.kt` auto-merge.
- **against `feature/phase-14-check-gates`:** one conflict, `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModel.kt` (both edit the constructor line 76: check-gates adds `@param:` to `applicationScope`; this branch rewrites the constructor and already carries `@param:ApplicationScope`). `app/build.gradle.kts` and `build-logic/src/main/kotlin/standards.gradle.kts` auto-merge (the §14.1 "disjoint regions" claim holds: check-gates ends at `:304-312`, this branch's text edit is `:306-310` of the old file, 28 lines apart from check-gates' nearest hunk).
The two branches share no file with each other in these regions, so the merge-in against `develop` once both are there sees the union: **two files**.

### 2.3 Resolutions (ruled)
1. **`GameAnim.kt`:** keep both blocks, in `develop`'s order — `HINT_NOTICE_DISPLAY_MS` and `HINT_NOTICE_EXIT_MS` first, then this branch's "The wrong tap" block from `WRONG_TAP_SHAKE_MS` to `WRONG_TAP_MARK_HOLD_MS`. No renaming, no re-ordering inside either block.
2. **`TutorialViewModel.kt`:** take this branch's constructor whole (three parameters, the `@param:` comment). Check-gates' one-line change is contained in it, so C7's site is fixed either way.
3. The auto-merged files are still read once by the developer for sense, per hint brief §10.3: `GameUiState` has both `hintUndecided` and `rejectedTapCell` in the constructor and in both `initial()`s with KDoc; `GameScreen` keeps both branches' effects and the notice, and the `LongMethod` comment names every surface from both; `strings.xml` and `dimens.xml` keep both comment-headed blocks.
4. Then, in this order: the Check command of the merged `CLAUDE.md` (check-gates' C2 makes it `./scripts/check.sh`); the hint branch's named tests (`./gradlew :domain:test` and the seven `:app` classes listed in hint brief §10.3), and this branch's own `GameFlowTest`, `GameScreenWrongTapTest`, `BoardCanvasWrongTapTest`, `BoardCanvasWrongTapReducedMotionTest`, `TutorialWrongTapTest`, `TutorialHapticsTest`, `SoundPoolPlayerTest`; the code-auditor reads the merge commit's hunks in the two files (a read, not a loop); a fresh tech-lead re-reads the same hunks against this section. A clean Check with those resolutions needs no further review (§14.1 step 2's rule).

### 2.4 C7, §9.8 and the sitting
- **C7 will apply to this branch's main code after the merge-in**, and the evidence says it passes: the nine `w:` lines on this tree are the six `hiltViewModel` imports and the three `@param:` sites that check-gates' C7 fixes; this branch's own files compile clean in both variants, and its `TutorialViewModel.kt` already carries the fix. §9.8 rule 3 governs anything new after the merge: fixed in code, never a suppression or a switch; the import and the `@param:` patterns are mechanical, any other kind stops the developer and comes to the tech-lead.
- **§9.8 rule 2, recorded deviation (tech-lead decision):** the eight new Compose test classes use the house `createComposeRule` (v1), which is deprecated, so they add `w:` lines to a unit-test compilation in files this diff touches — strictly a review finding under rule 2. Accepted, for the reason the deviation is held at all: the v2 rule changes how a suite synchronises, and a module running 31 suites on v1 and 8 on v2 would be the worse state. New suites follow the house rule until the §14.2 migration moves all of them at once. The next tech-design sync adds that sentence to §9.8 (§6 below).
- **No 15e sitting for this branch (ruling).** It edits `MainActivity.kt` and `di/`, which are on §10's start-path list, but that list says when a merge-in invalidates a *standing* sitting; a sitting is owed by a branch that "changes Home's first frame" (§10, §14.1 F-1 and LN rows). This branch does not: one field write in `onCreate`; three `@Binds`; two singletons whose constructors do no IPC (`AudioManager` is lazy, `ProcessLifecycleOwner.get()` is a static getter); the two new collectors run on the application scope's `Dispatchers.Default`, where each first reading costs about a millisecond of binder time off the main thread (design §1.8, §4: "cold start is unchanged"). Candy-backdrop's sitting, last in the order, measures its own delta with this branch in both arms, and PLAT-08's absolute on a phone includes it. Recorded in §14.1's audio row at the sync.

### 2.5 What would turn this PASS into a HOLD
Only a merge-in outcome outside this section: a conflict in a file other than the two named, a Check red for anything other than §9.8's two mechanical warning kinds, or a resolution that is not the one ruled. Any of those goes back to the android-game-dev and to a fresh tech-lead with the merge commit, not to a new review loop of the range.

## 3. QA's FAIL — the frozen reduced-motion gate (ruling)

**The defect, verified in the code.** `rememberAnimationsEnabled()` (`ui/theme/Clay.kt:209-218`) is `remember(context)` over one `Settings.Global.getFloat(ANIMATOR_DURATION_SCALE)` read, so its answer is fixed for the life of the composition. Compose's own animation clock is not: in the pinned `ui-android` 1.11.3, `MotionDurationScaleImpl` (`androidMain/androidx/compose/ui/platform/WindowRecomposer.android.kt:423` onward) backs `scaleFactor` with `mutableFloatStateOf` and collects `getAnimationScaleFlowFor(applicationContext)` (`:101-124`), a `ContentObserver` on the same `Settings.Global` key, one per window, started on the first read of the scale. So after a live toggle to "Remove animations" the clock runs at scale 0 while our gate still says "on": `correctionFor` picks the shake (`WrongTapFeedback.kt:316-324`), its 270 ms tween finishes in its first frame, and the mark branch never runs — exactly what `mark-dark.mp4` shows. The same split affects all 13 call sites in 8 files (`HomeBlobs.kt:33`, `DockOnMark.kt:55`, `GameActionDock.kt:147,174`, `ResultsCelebration.kt:35,57,104`, `HintToggleControl.kt:81`, `TutorialScreen.kt:240,348`, `BoardCanvas.kt:136`, `Clay.kt:152,183`): every phase-15 reduced-motion behaviour is right at the first composition and wrong after a live toggle. The helper predates this range; this branch only made the consequence visible, because the mark is the one transient visual that has no persistent partner.

**Where the fix goes: not this branch.** The helper is phase 15's, it has 13 call sites outside the 17-item scope, and fixing it here would reopen three passed reviews on a range they did not review. The range is correct against its spec and pins the mark whenever the gate reads off (`BoardCanvasWrongTapReducedMotionTest`, mutant 15). It goes on **`bugfix/reduced-motion-live-gate`**, cut from `develop` after this branch has merged (its test needs the wrong-tap harness), android-game-dev → code-auditor + qa-engineer → tech-lead.

**The mechanism (tech-lead decision).** `rememberAnimationsEnabled()` keeps its name and signature and reads the scale the composition's own animations read: the `MotionDurationScale` element of the composition's coroutine context (`rememberCoroutineScope().coroutineContext[MotionDurationScale]`), enabled when `scaleFactor != 0f`; absent (a preview, a test that provides none), the default is enabled, as today's `DEFAULT_ANIMATOR_SCALE`. Reading `scaleFactor` in composition subscribes the composition to Compose's snapshot state, so a live toggle recomposes every reader and the next wrong tap takes the mark path, on every API level. The gate and the clock become one reader of one value and cannot disagree again by construction. Our own `Settings.Global` read goes away; the observer is Compose's, already started by the first animation of the window (Home's entrance). Rejected: a `ContentObserver` of our own (a second observer of the same key, and a registration on Home's first frame, which is a new SDK call on the start path and would force a sitting); a re-read on `ON_RESUME` (misses the multi-resume window and keeps two readers of one setting). The developer verifies the two source facts above in the cached sources jar before writing (the haptics ruling did the same for `Reject`).

**Tests, red first.** (a) A Compose test over the gameplay board whose effect context carries a *mutable* `MotionDurationScale` (the `WrongTapBoards.kt:494-499` harness already provides the element; make its factor mutable): compose at 1, flip to 0, make a wrong tap → the mark on the tapped cell and no displacement; flip back to 1, wrong tap → the shake. (b) The same flip over the tutorial (no mark, no shake, the redirect static) and over Results (the stars snap). (c) The six test files that set `Settings.Global.ANIMATOR_DURATION_SCALE` to drive the gate today (`GameScreenSoundTest`, `ResultsCelebrationSoundTest`, `GameFlowTest`, `BoardCanvasWrongTapReducedMotionTest`, `TutorialHapticsTest`, `TutorialSoundTest`) stay green as they are, because Compose's flow reads that same key (`WindowRecomposer.android.kt:97`) and a provided element wins over it (`:334-337`); the developer runs them unedited and reports the count. No assertion is loosened.

**Device check (qa-engineer).** QA's own repro on the emulator: English-33 open, Home, Remove animations on, back, wrong tap → the mark within 450 ms; the reverse toggle → the shake; then WT-15 and P9 once more with animations off; a screen recording of the first and the restart case of `mark-dark-restart.mp4` as the control.

**Blocking.** Not merge-blocking: the range is right, and the fix is elsewhere. **Blocks the branch cut** — deadline: merged before `release/3.0.0` is cut. The product baseline makes the stack's accessibility rules a condition of shipping, rules/compose.md requires animation to respect reduced motion, D-06a says "never for nothing", and the defect spans every phase-15 reduced-motion behaviour, not only the mark; the fix is one function and one harness change, one review loop. The producer records it in STATUS.md and §14.1 (§6).

**15e.** No sitting, provided the mechanism is the one ruled: `ui/theme/Clay.kt` is on the start-path list, but Home's first frame loses one `Settings` read and gains no call. A developer who deviates to anything that registers or reads on the first frame makes the branch carry a sitting on `Pixel_9a_bench`, and comes to the tech-lead first.

## 4. The BLOCKED device items — ruled one by one

Standing rule: the founder never tests by hand. Setting hardware (headphones off, a headset plugged in) is not testing; listening and judging is.

| Item | What is proven today | Ruling | Closes how |
|---|---|---|---|
| The speaker-route halves of D2, D3, D6, D7, and D5's "Bluetooth off mid-game" half (with D9's D2 half on the API-27 phone) | The emulator (API 36, the platform-route path) ran the whole speaker branch: 0 `started` events for every cue on Vibrate and on Silent, the jump and reject haptics still firing, D6's tails stopped within 0.13 s, D7's within 0.89 s with pops 2–3 never started (`qa-engineer-integration.md`, D2/D3/D6/D7 rows). The GM6 (API 27, the inference path) ran the headphone branch: every cue through `bt_a2dp` on Silent. The one combination no device has shown is the inference path's *speaker* branch (API 24–32, no external output); `AudioRouteClassifierTest` covers it on the JVM (`{speaker, earpiece} → BuiltIn`, the `else` branch), and `AndroidSystemAudioStateTest.anAddedWiredHeadsetMovesTheRouteOffThePhone` shows the inference flipping | **Neither merge- nor cut-blocking.** The "hearing" halves are a listen, which is testing; the play path is the evidence of record | QA's release session on the phone (the R-7 / PLAT-08 sitting), **if** the founder powers off or unpairs the Bluetooth headphones first (Q-025). Not done by the cut → one line in the Q-PUBLISH-3.0.0 summary: "the speaker branch of the Silent/Vibrate gate on an API 24–32 phone: JVM and emulator evidence only" |
| D4, wired headphones | No wired route on the phone during the session. The wired path is the same allowlist (`TYPE_WIRED_HEADSET`, `_HEADPHONES`, `TYPE_USB_HEADSET`) as the Bluetooth path that passed, and the unplug path is `BECOMING_NOISY → BuiltIn`, Robolectric-tested (`becomingNoisyReportsThePhoneAtOnce`) | **Neither merge- nor cut-blocking** | The same release session, if a USB-C or 3.5 mm headset is plugged in (Q-025); else the Q-PUBLISH summary line |
| WT-18 / WT-10 at 60 fps on a phone | The emulator recordings measured the shake frame by frame (W1 +0.9 A / −0.78 A / +0.41 A, W2 and W3 within ±1 px of spec, the ring at 1.18× at +100 ms, the mark gone at 450 ms + one frame, light and dark, volume 0 and Sound off); the GM6's image has no `screenrecord` and its `screencap` lands after the 270 ms shake, while the mark itself was caught four times at 5.31:1 | **Neither merge- nor cut-blocking**: the measurement half is done; the phone half is a second instrument, not a missing fact | Any phone with `screenrecord` (API 28+ images all have it) in a later session; else the Q-PUBLISH summary: "the shake's phone recording: emulator only" |
| P4c, the Deselect listen (−41.5 LUFS) | The play path on the phone is proven (the 3,322-frame track plays to its last frame at the expected gain); audibility on a speaker in a room is a judgement by ear, which no instrument in the studio makes and the founder does not make for us | **Neither merge- nor cut-blocking.** The level is the sound-designer's measured mix; the conditional is already on file (`DESELECT_ATTENUATION` 10 dB → 6 dB, "only if the listen finds the put-down lost") | The Q-PUBLISH-3.0.0 summary states the mix and the open judgement. If the founder, playing, finds the put-down lost, the conditional is a one-constant 3.0.1 change with one test. Optional and QA's own: a relative level of Deselect against Select captured through the Mac's microphone with the phone on the desk is admissible evidence if QA chooses to take it in the release session; it is not required |

**The founder's action, drafted for the producer to queue (non-blocking).** Push line: `Q-025: before the next phone QA on the GM6, switch its Bluetooth headphones off and plug in a wired headset if you have one`.

    ## Q-025 — Headphones off, and a wired headset, for the audio release QA   (open)
    Date: 2026-10-24    Asked by: tech-lead (via the producer)
    Context: QA's GM6 session proved the headphone branch of the Silent/Vibrate gate (Q-010). The "phone itself
    mutes" branch (D2, D3, D6, D7 on the speaker; D5's second half) and the wired branch (D4) were out of reach
    because the Bluetooth headphones stayed connected and no wired headset was attached; QA may not toggle a radio.
    Options: 1) Before QA's next phone session, power off or unpair the Bluetooth headphones and leave a USB-C or
    3.5 mm headset plugged in if you have one; QA runs D2–D4 itself 2) Leave the hardware as it is; those halves are
    recorded as JVM + emulator evidence in the Q-PUBLISH-3.0.0 summary
    Recommendation: tech-lead picks 1 — two minutes of hardware setup and no testing by you closes the one branch of
    Q-010 that is your own requirement, muting on the phone speaker.
    Blocks: nothing
    Answer:

Two more QA observations, routed: open issue 8 (WT-14 by a real finger) belongs to `feature/talkback-board`'s QA, since D-TBa redefines the board's announcement and its stops; open issue 4 (a Classic board never resumes from the catalogue) is a product question for the game-designer and the ux-designer through the producer, not a finding on this range.

## 5. Routing of the minors

| Item | Decision | Owner, where |
|---|---|---|
| **sound-designer F-1** — `InvalidTapSound` (and `Select`, `Deselect`, `Undo`) replay once after a rotation through the `> 0` idiom; the wrong tap's partners do not, so the soft tok sounds alone | Tech-lead decision: the four tick-keyed cue effects in `GameScreen.kt` (`SelectionSound`, `DeselectionSound`, `UndoSound`, `InvalidTapSound`) go through `NewOrdinalEffect`, `InvalidTap` first; `HintSound` (keyed on `hintSource`) and `WinSound` (keyed on `status`) are state-keyed and stay; one rotation test per cue modelled on `GameScreenWrongTapTest.aScreenComposedWithACarriedWrongTapNeitherFeelsNorAnnouncesIt`; `OrdinalWatch` unchanged. The ad snackbar's replay is a UI behaviour for the ux-designer to look at, no change ruled | android-game-dev, the second commit of `bugfix/reduced-motion-live-gate` (same developer, same file, one review loop). A rider, non-gating: if it costs a loop, it drops to 3.0.1 |
| **sound-designer F-2** — the Haptics switch's supporting line ("Vibrate lightly on each jump") and the Settings KDoc ("there is no audio engine yet") | The copy is the ux-designer's, ruled at the fold-in (SET-02 now covers two haptics); the developer applies the ruled string in the bugfix branch if it is ruled by then, else in `feature/talkback-board`, which rewrites `strings.xml` anyway (producer's call). The KDoc line is fixed in the bugfix branch | ux-designer (copy), android-game-dev (KDoc) |
| **code-auditor finding 1** — two identical `tasks.withType<Test>().configureEach { inputs.files(…) }` blocks in `app/build.gradle.kts:468,480` | One block registering both collections | android-game-dev, the bugfix branch |
| **code-auditor finding 2** — the parity literal `% 2 == 0` in `WrongTapAnnouncement.kt:45` | `feature/talkback-board` replaces that file with `BoardAnnouncement.kt` and moves its constants (finding 3). The name goes in there; nothing in the bugfix branch | android-game-dev on talkback-board |
| **code-auditor findings 3–5** (overlap with `feature/talkback-board`) | Finding 4 is already decided by D-TBa item 3 on `feature/ux-spec-folds`: the tutorial speaks D-TB's lines, so `TutorialWrongTapTest.theTutorialBoardIsNoLiveRegionAndNoWrongTapIsAnnounced` is amended deliberately under that ruling, and `TutorialSourceGuardTest` keeps every token with its KDoc corrected (`docs/ux-spec.md` on ux-spec-folds, D-TBa's "What the built code must change: nothing on that branch", and F-44). Findings 3 and 5 are the moves that brief already plans | producer → the talkback-board developer, with D-TBa |
| **Tech design §16 #17 sync** | Already transcribed on `develop` (R-6, synced 2026-10-21: §12's Haptics paragraph, §14.1's audio row, §16 #17 "Decided 2026-10-18"). What remains is the post-merge close, §6 below | tech-lead, after the merge |
| **WT-19 golden images** | **Later.** The contract tests satisfy WT-19 for 3.0.0 (12 dashes, `onSurfaceVariant`, 3 ± 0.4 px at 40 ± 0.4 px on a hole and on a peg; the ring `primary` at 1.18 ± 0.02; light and dark). The golden suite stays `docs/tech-design.md` §8.5's post-3.0.0 item, after R-2: no Roborazzi in the offline cache (a new dependency means a THIRD_PARTY row, the ladder and the CI host-parity decision §8.5 names), candy-backdrop is about to change every pixel behind the board, and a golden recorded from the diff under review is an image of the implementation (standing rule 1). When the suite lands, its first goldens are the amendment's four. The ux-designer amends WT-19's wording at the fold-in | qa-engineer with tech-lead sign-off, post-3.0.0; ux-designer for the wording |
| **The ux-designer's fold-in** of `docs/ux/invalid-tap-feedback.md` into `docs/ux-spec.md` | **After `feature/ux-spec-folds` and `bugfix/hint-undecided` have merged** (both carry `docs/ux-spec.md`), on a docs branch off `develop`. The amendment's §11 map, plus: **renumbering** — D-06 is taken (the restart confirm dialog, ux-spec `:1132`), D-04f/D-04g are the hint notice, D-TB/D-TBa the TalkBack board; the ux-designer picks the free id and records a one-line alias ("D-06 in `docs/ux/invalid-tap-feedback.md` and in the code's comments is D-NN here"), because the code cites D-06, D-06a–d and D-01j(2) in about fifteen KDocs and a comment-only sweep for a number is churn, not a fix; **per D-TBa** — §3.6: the three sentences are superseded by D-TB's lines (item 2), the tutorial speaks them (item 3), the one narrowing (F-43: a line can be replaced by a newer board event before it is spoken); §3.8: the placement rule amended to "either placement, the canvas chooses no wording" (F-44); WT-14: D-TB's lines, the narrowing clause, and the pointer on the traversal line D-TBa names for the audio branch; WT-16: the tutorial announces through the shared announcer, the grep keeps its tokens; **and** the effect's home (`ui/game/JumpHapticEffect.kt`), O4 (the Settings line while the ringer mutes the speaker: decide or close), WT-10's wording at 60 Hz ("gone by 450 ms plus one frame", the developer's deviation 6), the auditor's finding 7 (the board reachable during the one-second hold — note D-TBa makes the board a stop anyway), F-2's copy, WT-19's wording, Q-012's answer in §15. The ux-designer's **review of the built feedback**, which my haptics handoff asked for and which did not run, happens here: from QA's measurements (W1 shake +12.3 / −10.2 / +5.3 px ≈ +0.95 / −0.78 / +0.41 A against the spec's +1.0 / −0.75 / +0.45; the ring 1.18×; the mark 5.31:1 light, 5.6:1 dark) and the emulator. The producer may hold the deletion of `.planning/qa/audio-integration/` until that review has read the recordings; the report quotes every number, so the record stands either way | ux-designer; producer for the timing |

## 6. Owed to `docs/tech-design.md` after the merge (tech-lead, a docs branch off `develop`, with the sync R1 owes)
- §14.1 audio row → **Done**: the merge sha; "no 15e sitting (tech-lead-signoff.md §2.4)"; the second-to-merge duties discharged.
- §16 #17 → closed: "Built on `feature/audio-review` (8f18049), merged <sha>".
- §12 → current state, not branch-conditional: the WAV set, `GameAudioAttributes`, the three gates, `HapticsSetting`; the "On `develop` today" paragraph becomes history.
- §9.8 rule 2 → "new Compose suites follow the house `createComposeRule` until the §14.2 migration moves all suites at once".
- §15 → a row for the frozen reduced-motion gate (likelihood certain on a live toggle / impact medium, accessibility), its branch and its deadline; §16 → the same as an open item with the mechanism of §3 above; §14.1 → a row for `bugfix/reduced-motion-live-gate`, **Blocks the branch cut**.
- §17 → nothing superseded by this report.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/tech-lead-signoff.md` — this report: the verdict, the merge-in resolutions, the reduced-motion ruling, the BLOCKED-item rulings with Q-025, the routing and the tech-design text owed. The only file written.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-audio` unless named; nothing committed, nothing merged, no Gradle run (as dispatched; a sign-off re-runs no gate).
- `cat .claude/rules/VERSION` vs `ergoz-soft/.claude-plugin/plugin.json` — 0.5.1 vs 1.0.0, so the plugin rule files were read in full.
- `git log --oneline 62c774d^..ae58e5e` — 26 commits: the 19 under review, then c63057a, f6d6d6a, c169fd2, e30bfac, d7c5f6d, 00eb8ba, ae58e5e (reports, the doc syncs, Q-024); `git diff --stat 8663710..ae58e5e` — 7 files, all `.planning/` and `docs/`; `git status --short` — clean.
- `git diff --stat 62c774d..8663710` — 69 files, +5,537 / −210; `git diff 62c774d..8663710 -- app/src/main build-logic` (2,218 lines) read in full.
- `git log --oneline -1 develop` — c5f2c54 (release-docs merged); `git show develop:docs/tech-design.md` — 1,485 lines; §3.5, §8.5, §9.8, §12, §14.1 read.
- `git merge-base --is-ancestor develop <branch>` — true for `bugfix/hint-undecided`, `feature/phase-14-check-gates` and `feature/audio-review`.
- `comm -12` over `git diff --name-only` — files shared with hint-undecided: `GameAnim.kt`, `GameScreen.kt`, `GameUiState.kt`, `GameViewModel.kt`, `dimens.xml`, `strings.xml`, `GameViewModelTest.kt`; with check-gates: `app/build.gradle.kts`, `TutorialViewModel.kt`, `standards.gradle.kts`.
- `git merge-tree --write-tree --name-only --messages feature/audio-review bugfix/hint-undecided` — one CONFLICT, `GameAnim.kt`; the other six auto-merge. The same against `feature/phase-14-check-gates` — one CONFLICT, `TutorialViewModel.kt`; `app/build.gradle.kts` and `standards.gradle.kts` auto-merge. In-memory only; no worktree, index or branch changed (`git status` clean afterwards).
- `git diff develop...feature/phase-14-check-gates -- build-logic/src/main/kotlin/android-app.gradle.kts kotlin-jvm.gradle.kts standards.gradle.kts app/build.gradle.kts ui/onboarding/TutorialViewModel.kt` — C7's `allWarningsAsErrors` (unit-test compilations exempt by name, with the coroutines opt-in), the F1 self-heal block and the comment edits, the `hilt-lifecycle-viewmodel-compose` line, the `@param:` fix at `:76`.
- `grep -rn 'rememberAnimationsEnabled' app/src/main/java` — the definition at `Clay.kt:209` and 13 call sites in 8 files (listed in §3); `grep -rln 'rememberAnimationsEnabled\|ANIMATOR_DURATION_SCALE' app/src/test/java` — 6 files (listed in §3).
- `unzip -l`/`unzip -p` of `~/.gradle/caches/modules-2/files-2.1/androidx.compose.ui/ui-android/1.11.3/…/ui-android-1.11.3-sources.jar` — `MotionDurationScale` is a `@Stable` public `CoroutineContext.Element` (`commonMain/androidx/compose/ui/MotionDurationScale.kt`); `WindowRecomposer.android.kt:333-341` puts it in the recomposer's context, a provided element winning; `:423` onward backs `scaleFactor` with `mutableFloatStateOf` and starts `getAnimationScaleFlowFor` on the first read; `:97-124` is that flow: `Settings.Global.getFloat(ANIMATOR_DURATION_SCALE)` plus a `ContentObserver` on its URI.
- `awk '/^## Q-024/,/^## Q-023/' .planning/questions/FOUNDER.md` — Q-024 answered 2026-10-23, option 1 (the founder connects a phone; QA runs the checks).
- On `/Users/ermanergoz/Desktop/PegSolitarie-ux` (`feature/ux-spec-folds`): grep counts in `docs/ux-spec.md` — D-04f 1, D-04g 13, D-TB 80, D-TBa 49, D-06 24, WT-14 2, WT-16 3, D-01j 6; the eleventh-revision notes (`:363-404`), D-TBa items 1–4 (`:1290-1344`) and the fold notes (`:1418-1469`) read.
- On `/Users/ermanergoz/Desktop/PegSolitarie-hint` (`bugfix/hint-undecided`): `tech-lead-brief.md` §10.1–10.3 read (the overlap table, the merge order, the second-to-merge duties and the audio resolutions).
- `./gradlew check` — **not run**: the developer's record at 8663710 (11 m 19 s; 1,185 + 274 + 54 tests, 0 failed or skipped; lint 0 errors) and the code-auditor's own run (14 m 46 s, the same counts, JUnit XML read per suite) are the Check of record for the range; the merge-in re-runs it (§2.3).

## Open issues / risks
1. **The merge-in is the remaining step before `develop`:** two conflict files, resolutions ruled in §2.3; a Check green after it, the other branches' named tests, the code-auditor's read of the two hunks and a fresh tech-lead's re-read of them. §2.5 names what would make it a HOLD.
2. **`bugfix/reduced-motion-live-gate` blocks the branch cut** (§3); it is cut after this merge and before `release/3.0.0`. Its riders (F-1 cues, finding 1, F-2 KDoc) are non-gating.
3. **The ux-designer's review of the built UI did not run** before this sign-off; it is not a gitflow gate, and it is scheduled into the fold-in (§5) from QA's measurements and the emulator.
4. **D-TBa supersedes three strings and one tutorial rule this range builds** (the sentences, "in the game only"). The range is right for the spec it builds; `feature/talkback-board` carries the change at its first `develop` merge-in, and the two tests it amends are named (§5, findings 3–5).
5. **The Q-PUBLISH-3.0.0 summary** will carry whichever of §4's halves are still open at Gate 2: the speaker branch on an API 24–32 phone, D4 wired, the shake's phone recording, the P4c judgement. None is a defect; each is an instrument or a judgement the studio does not have.
6. **R8-minified behaviour of the new audio classes is unverified on a device** (QA open issue 6: the Build command needs the four signing secrets). The release QA on the signed build (R-7's session) covers it; the classes use no reflection and are Hilt-bound, so the risk is low.
7. **Cold start:** the sound-designer's F-3 asked QA to confirm the `SoundPool` construction costs nothing visible on a cold-start trace; it rides PLAT-08's phone measurement, not a sitting (§2.4).
8. The fold-in's renumbering touches no code (the alias rule, §5); a later branch that rewrites a cited KDoc anyway uses the new id.

## Handoff
Next: producer — merge order steps 3a and 4 first; then dispatch the android-game-dev once on `feature/audio-review` to merge `develop` in with §2.3's resolutions, run the merged Check and the named tests; then the code-auditor's read of the two conflict hunks and a fresh tech-lead's re-read against §2.3; then `git merge --no-ff` into `develop`. Queue Q-025 (§4). Record in STATUS.md: this PASS; `bugfix/reduced-motion-live-gate` as a cut blocker with §3 as its brief (plus the riders of §5); the fold-in and its timing; the Q-PUBLISH summary lines; the talkback-board routing of findings 3–5 with D-TBa.
Next: android-game-dev — the merge-in per §2.3 (nothing else on this branch); afterwards the bugfix branch per §3 and §5, red first, verifying the two Compose source facts before writing.
Next: code-auditor — the merge commit's hunks in `GameAnim.kt` and `TutorialViewModel.kt` against §2.3; later the bugfix branch.
Next: qa-engineer — nothing more on this range; the bugfix branch's device check (§3); the release session on the GM6 with §4's halves if Q-025 is answered 1.
Next: ux-designer — the fold-in per §5, after ux-spec-folds and hint-undecided merge; F-2's copy; the WT-10 and WT-19 wordings; finding 7.
Next: tech-lead — the §6 tech-design sync after the merge, and the re-read of the merge commit's two hunks.

VERDICT: PASS

## Merge-in re-read — 2026-10-24 (a fresh tech-lead; `13c04c6` and `9f0e67a`)
Date: 2026-10-24
Branch: feature/audio-review (report only: read `13c04c6`, parents `b39e28e` (audio) and `dbd0a6c` (develop), and `9f0e67a`; the head `76ca1ae` adds only the merge report; this section is uncommitted, as dispatched, for the producer to commit)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game — the plugin 1.0.0 rule files read in full, because the worktree's snapshot is 0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; the snapshot's lessons.md), android-architecture, android-gradle-logic
Ran in parallel with: the code-auditor's read of the same merge commit's code hunks (its own report)
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable. The dispatch reports the Fable limit reached, so this re-read runs on Opus under Q-019's answer 3 ("Opus for every Fable-pinned role until 3.0.0 is published"). Its 2026-10-23 addendum restored Fable while Fable is available. The merge commit was written on Opus too, so here the reviewer shares the author's model. The Q-019 mitigation applied is a fresh agent that trusts no recorded result: every resolution below was checked by git against both parents and the merge base, not read from the merge report.
Fan-out: none
Loops: 0
Read in full: this file's §1–§6; `.planning/reports/audio-review/android-game-dev-merge.md`; `git show --cc 13c04c6`; `git diff 13c04c6^2 13c04c6 -- …/ui/onboarding/TutorialViewModel.kt`; `git show 9f0e67a`; `.planning/questions/FOUNDER.md:1-60` (Q-025 to Q-021) and Q-019; `docs/tech-design.md` §12 and §17. Read in part: `docs/tech-design.md` header, §2.4, §9.8, §13, §14.1, §16; `GameScreen.kt:92-330`; `.planning/reports/hint-undecided/tech-lead-signoff.md` finding 2; `.planning/reports/talkback-board/tech-lead-brief.md` (on `PegSolitarie-tb`, `3e5b893`) at its `GameScreen.kt` lines.
Lessons applied: none (the snapshot has 0 active lessons; `.planning/recall/RECALL.md` does not exist)

### Summary
- **The PASS stands.** The rules allow exactly one resolution for each of the three conflicts, and the merge commit applies each one exactly. `GameAnim.kt` and `TutorialViewModel.kt` follow §2.3 items 1 and 2 to the line. `FOUNDER.md` is decided by its own format (rules/reporting.md), not by §17. Outside those three files the merge commit is the mechanical merge, byte for byte.
- **Open issue 1:** §2.5's trigger fired, and its remedy is this re-read. It is not a hold, because the FOUNDER.md hunk involved no choice of text. §17 does not decide it, because §17's rule covers `docs/tech-design.md` only. A rule for later merge-ins is in R.2.
- **Open issue 2:** the wrong-tap answer stays on the board panel's line, because it is not a surface. `NoWinBanner` is a surface and gets its own line. That line goes on `feature/talkback-board`, in the same hunk that rewrites the board panel's line, not on this branch.
- **The tech-design sync** for audio rides `feature/tech-design-r1-close`, cut from `develop` right after audio's `--no-ff` merge, carrying both R1's close and audio's (R.4).

### R.1 What was verified, mechanically
- **The merge-in is current:** `develop` is still `dbd0a6c`, the merge's second parent.
- **Files changed on both sides** since the merge base `0582e3f`: exactly §2.2's ten code files plus `.planning/questions/FOUNDER.md`, and nothing else (`comm -12` over both `git diff --name-only` lists). An in-memory `git merge-tree` replay gives the same three conflicts, and the other eight files auto-merge.
- **No hand edit outside the conflicts:** `git diff --name-only 61aaaee 13c04c6`, where `61aaaee` is the replayed tree of `git merge-tree --write-tree b39e28e dbd0a6c`, lists only the three conflicted files. No conflict marker remains anywhere in the tree.
- **`GameAnim.kt`:** audio→merge adds exactly `develop`'s ten lines (`HINT_NOTICE_DISPLAY_MS`, `HINT_NOTICE_EXIT_MS`, a blank) before the wrong-tap block. develop→merge adds exactly the wrong-tap block after them. Neither side loses a line. This is §2.3 item 1.
- **`TutorialViewModel.kt`:** the merged file equals `b39e28e` (`git diff --quiet`). `develop`'s only change since the base is `:76`'s `@param:ApplicationScope`, which audio's constructor already carries at `:86`. This is §2.3 item 2, and C7's site is fixed.
- **`FOUNDER.md`:** audio→merge adds exactly `develop`'s two hunks: Q-023 at the top, and the Q-019 addendum line. develop→merge adds exactly audio's two hunks: Q-025 and Q-024 at the top, and Q-012 to Q-008 below Q-013 (on audio since 2026-09-29, new to `develop`). Neither side loses a line, and the ids are unique (`uniq -d` prints nothing). The out-of-order run Q-005, Q-004, Q-003, Q-902, Q-901, Q-007, Q-006 already exists at the merge base; this merge did not cause it.
- **`9f0e67a`:** it changes only the KDoc and the annotation's trailing comment. There is still exactly one `@Suppress("LongMethod")`. The longest line in the list is 98 of 120 characters. The developer's Check ran on this tree: the test results are stamped 09:22:39 and kover 09:28:38, after the commit at 09:12:59. The edited suppression is the code-auditor's to approve (in parallel); this re-read rules only on what the list says. The hint sign-off wanted the move made "in the same commit that adds its wrong-tap effect". That commit is history and rebase is prohibited, so the first commit after the merge is the right substitute.
- **The merged Check of record is the developer's:** 1,565 tests, 0 failed or skipped; fuzz 10,000 / 0; kover 99.02 %; 0 `w:` from `app/src/main` and `domain/src`, debug and release. It was not re-run here, as dispatched. §2.5's second trigger, a red Check, did not fire.

### R.2 Open issue 1 — `FOUNDER.md` against §2.5 (ruling)
- **§2.5's wording is met literally:** FOUNDER.md is a third conflict file. §2.2 missed it because it measured audio against only the two code branches it named. Since `c5f2c54`, `develop` took five merges: ux-spec-folds (`4ff4ec6`), tech-design-sync (`f478ae9`), the status branch (`2d045d1`), R1 (`b1a6cf9`) and check-gates (`dbd0a6c`). Q-023 (`df37c3e`) came in through ux-spec-folds. The scope of §2.2's measurement was short, but its prediction held: against `develop`'s tip the overlap is the ten predicted code files plus this one.
- **§17 does not decide it.** §17's rule says it is "the merge-in rule for this file", meaning `docs/tech-design.md`. FOUNDER.md is the producer's record (rules/studio.md, Hierarchy), and rules/reporting.md defines its format. The merge commit's message cites "tech-design §17 merge-in rule, hunks 2 and 3": that is an analogy that happens to reach the right result, but the authority is the format.
- **What decides it** is the format: newest first, one entry per id. Both sides added whole entries at the same insertion point. Neither side edited an entry the other touched. The ids were already distinct and already continued `develop`'s numbering: `develop`'s highest is Q-023, audio's are Q-024 and Q-025. The format allows exactly one result, the union in the order Q-025, Q-024, Q-023, and R.1 shows that the merged file is that union. Nobody had to choose any text, and that is what separates mechanics from a decision.
- **So it is not a hold, and the PASS stands.** For a conflict in a third file, §2.5 sends the merge commit "to the android-game-dev and to a fresh tech-lead". This re-read is that step, and it finds nothing to undo. Nothing goes back to the developer.
- **For later merge-ins (tech-lead ruling; the sync in R.4 writes it into §14.1's second-to-merge paragraph):**
  - (a) A `.planning/` record kept as entries (FOUNDER.md, STATUS.md, a report both sides appended to) may conflict because each side added whole, distinct entries. Such a conflict is resolved by the record's own format: keep both, in the format's order, and edit no entry.
  - (b) A conflict inside one entry, or two sides claiming the same id, is not mechanics. It stops before the merge-in is committed and goes to the producer, who owns those records.
  - (c) A sign-off that pre-rules a merge-in measures it with `git merge-tree` against `develop`'s tip as it will be at the merge-in, docs branches included. It names every branch still due to merge ahead of it.

### R.3 Open issue 2 — the moved surface list (ruling)
**Definition (tech-lead decision).** For this list, a surface is a composable `GameScreen` places in its layout: a node the player can see or reach. Effects (the cue, haptic and snackbar effects built on `LaunchedEffect` or `NewOrdinalEffect`) and modifiers on an existing node are not surfaces. The list names them on the line of the surface they serve, or not at all. That is why the list has never named the ten effects at `GameScreen.kt:182-195`, from `JumpHapticEffect` to `AdMessageSnackbarEffect`, and that is right.

1. **The wrong-tap answer keeps the board panel's line.** It adds no node:
   - `BoardCanvas` draws the shake, the mark and the redirect (`wrongTapCorrection = true`).
   - The announcement is the board's own node made a polite live region (`:252-253`: "D-06c: the board itself is the polite live region; no new node").
   - The cue and the reject haptic are two effects (`:187-188`).

   A separate line would name a surface a reader would look for and not find. The hint sign-off's finding 2 assumed audio and talkback-board "must each add a surface". The operative part of that ruling — the list in the KDoc, the short pointer, one suppression — is met by `9f0e67a`. Its premise is corrected here: under this definition neither branch adds a surface, and both amend the board panel's line.
2. **`NoWinBanner` is a surface, and it is missing.** It is a composable placed in the board's `Box` (`:267`), its own node with Undo, Restart and Home. It has been hosted since `6035840` (2026-07-13), and no version of the list has named it: not phase 15's `542292f` (2026-09-28), and not the merge base (`0582e3f:142`). The KDoc makes the list the condition the suppression rests on ("that list is the point of it"), so the line is owed. It blocks neither the merge nor the cut: it is a comment on a suppression that is already approved, and the gap predates both branches.
3. **Which branch: `feature/talkback-board`,** in the commit that rewires `GameScreen.kt` (its brief's B4), in the same hunk as its rewrite of the board panel's line. The reasons:
   - That branch must rewrite that line anyway. `.wrongTapAnnouncement(…)` becomes `.boardAnnouncement(…)` (brief `:354`), and the region then carries D-TB's lines, not only the wrong tap's.
   - It edits the `Box` that hosts the banner (`isTraversalGroup`, with the banner read after the last cell, brief `:56-58`).
   - Its brief already commits to "surface lists amended, never a second one" (`:322`).
   - It is the next code branch to merge after audio (step 5a), and its B0 has not started, so the instruction costs nothing.

   Not this branch: the gap predates it, and a commit now would put an unruled edit on the head under re-read (the rule of §3: a pre-existing defect is fixed off the range). Not `bugfix/reduced-motion-live-gate` either: if one branch inserts a line next to the board panel's line while talkback-board rewrites that line, whichever merges second hits an adjacent-line conflict.

   The text for the talkback-board developer is mechanical. The list keeps the body's order, banner before notice, so the line goes between the board panel's line and the Undecided notice's line:

        * - the rescue banner, [NoWinBanner], sharing its slot with the Undecided notice (D-08, D-04f);

   Under the definition above, the board panel's line is that branch's to reword for D-TB. Its cell overlay and announcer live inside `BoardCanvas`, and its traversal properties are modifiers, so neither adds a line. Its code-auditor approves the edited suppression (rules/kotlin-android.md). If talkback-board does not ship in 3.0.0, the line rides the next branch that edits the list.

### R.4 Owed to `docs/tech-design.md` when audio merges
**Carrier: `feature/tech-design-r1-close`.** R1's sign-off §4 set it up: docs only; the tech-lead edits, the code-auditor reads it as text, a fresh tech-lead signs off; no QA; merged before checklist 1.1 reads §14.1 at the cut. It is not cut yet.
- **Cut it from `develop` right after audio's `--no-ff` merge.** It carries R1's owed edits (§7.2, §10, §15, §16 #21, the book-count note) and audio's, listed below.
- **One branch, because** both closes edit §14.1, §15 and §16 of one file. Two docs branches cut from one `develop` would only meet under §17's rule.
- **The name stays** as R1's sign-off and the check-gates re-read cite it. Its report names both closes.
- **If audio's merge is held after all,** r1-close is cut without the audio items, and audio's close goes to a docs-only `feature/tech-design-audio-close` after audio merges. That branch is then the second of the two under §17's rule.

Line numbers below are on `13c04c6`, which is `develop`'s file (audio does not touch `docs/tech-design.md`):
1. **Header `:14-15`:** the haptics ruling is "on `feature/audio-review`"; it becomes "on `develop` since <merge sha>". The sync also adds its own dated sentence (§17 hunk 1).
2. **§2.4 `:214-215`:** drop "the file is on `feature/audio-review` until that branch merges — N-5".
3. **§9.8 rule 2:** add the sentence from the sign-off's §2.4: "new Compose suites follow the house `createComposeRule` until the §14.2 migration moves all suites at once". The suite count in its table row ("29") takes the merged tree's own figure, measured by the author who writes it. For reference: 35 test files name `createComposeRule` on `13c04c6` and 31 on `develop`, and the merge-in Check log shows 33 such `w:` lines.
4. **§9.8 rule 3 `:1078-1082`:** "If check-gates merges after `feature/audio-review` …" becomes history. Check-gates merged first (`dbd0a6c`), audio took it in at `13c04c6`, and audio's main code compiles with 0 `w:` in debug and release.
5. **§12, rewritten as the current state rather than branch-conditional:**
   - `:1181-1183`: `GameAudioAttributes` is the one definition the pool and the route query share; drop "inline … on `develop`".
   - `:1187-1194`: the WAV set is current: twelve mono 16-bit 44.1 kHz WAVs, of which 7 are Kenney and 3 freesound-derived (CC0 1.0), and `sfx_invalid` and `sfx_hint` are rendered from `audio/recipes/`. The `.ogg` bullet becomes a dated "until <sha>" note, and the "if that branch merges before the cut" clause is now fulfilled.
   - `:1198-1200`: the design file is on `develop`, and the C1 precondition is done.
   - The A1–B5 table stays, because the decisions are as built (sign-off §1). Add where each now lives: `audio/SystemAudioState.kt`, `AudioRouteClassifier.kt`, `AndroidSystemAudioState.kt`, `AppVisibility.kt`, `GameAudioAttributes.kt`, the three gates in `SoundPoolPlayer.kt`, and `MainActivity.kt` for V1.
   - `:1223-1225`, "Device checks D1–D10 need a physical phone …": replace with what is proven, on the GM6 (API 27) and the emulator (API 36), per sign-off §4. Name the two open halves (the speaker branch on an API 24–32 phone; D4, wired) and Q-025.
   - Haptics, `:1220-1226`: drop "on `feature/audio-review`" and state the haptics as built:
     - `data/HapticsSetting.kt` has one member, is implemented by `SettingsRepository` and is bound in `di/DataModule.kt`.
     - `ui/game/JumpHapticEffect.kt` is called from both screens.
     - `WrongTapHaptic` is private to `GameScreen`, runs through `NewOrdinalEffect`, and fires `HapticFeedbackType.Reject` only when `hapticsOn && SDK_INT >= R`.
     - The tutorial passes no `wrongTapCorrection` and hosts no reject haptic, cue or announcement.
6. **§13, Sounds row `:1246`:** "For 3.0.0 … once `feature/audio-review` merges" becomes current, and the `.ogg` text becomes history.
7. **§14.1, audio row `:1446`:** marked **Done**, with:
   - the merge sha, and "no 15e sitting (`audio-review/tech-lead-signoff.md` §2.4)";
   - the second-to-merge duties discharged: merge-in `13c04c6` with three conflicts (two per §2.3, FOUNDER.md by its format, this re-read), the merged Check (1,565 / 0; fuzz 10,000 / 0; kover 99.02 %), the code-auditor's hunk read and this re-read;
   - the N-5 parenthetical dropped.
8. **§14.1 merge order:** step 5 (`:1496`) is done, with the sha. Step 5a (`:1498-1501`) has its precondition met (hint-undecided, audio-review and ux-spec-folds all on `develop`), and its "should audio-review slip" clause is moot.
9. **§14.1's second-to-merge paragraph:** add R.2's three-part rule for record files and its measurement against `develop`'s tip.
10. **§14.1, a new row for `bugfix/reduced-motion-live-gate`,** marked **Blocks the branch cut**:
    - its brief is the sign-off's §3, with the riders of §5 (F-1's four cue effects, auditor finding 1, the F-2 KDoc);
    - its chain is android-game-dev → code-auditor + qa-engineer → tech-lead, and it is cut after audio merges;
    - its slot against 5a is set in that row. Both branches edit `GameScreen.kt`'s effect block: F-1 moves `SelectionSound`, `DeselectionSound`, `UndoSound` and `InvalidTapSound` onto `NewOrdinalEffect`, and talkback-board adds its announcement sources beside them (brief `:207`). So whichever merges second carries the merge-in and its re-read.
11. **§15:** a row for the frozen reduced-motion gate: likelihood certain on a live toggle, impact medium (accessibility), with its branch and its deadline.
12. **§16:**
    - #17 closes: "Built on `feature/audio-review` (`8f18049`, 2026-10-21), merged <sha>".
    - A new item for the gate, with the mechanism of §3, numbered after `develop`'s highest when r1-close is cut (#28 today; if another branch numbers first, §17's hunk 3 applies).
13. **§17:** one new row. It supersedes the branch-conditional audio text of the header, §2.4, §9.8 rule 3, §12, §13 and §14.1 ("on `feature/audio-review`", "once … merges", "On `develop` today: twelve `.ogg`") with the as-built state at <sha>. The N-5 row's "moot at the merge" gets its date. This corrects the sign-off's §6 line "§17 → nothing superseded".

### R.5 Checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-audio`, read-only. Nothing was committed or merged, and no Gradle ran, as dispatched (a sign-off re-runs no gate).
- `cat .claude/rules/VERSION` vs the plugin's `plugin.json`: 0.6.2 vs 1.0.0, so the plugin rule files were read in full — pass.
- `git rev-parse develop`: `dbd0a6c`, the merge's second parent — pass.
- `git show --cc 13c04c6`: two combined hunks, `FOUNDER.md` and `GameAnim.kt`; `TutorialViewModel.kt` is absent because it resolved to one parent — read.
- `git diff --quiet b39e28e 13c04c6 -- …/TutorialViewModel.kt` prints identical; `git diff 0582e3f dbd0a6c -- …/TutorialViewModel.kt` shows one line, the `@param:` at `:76` — pass.
- `git diff -U1 b39e28e 13c04c6 -- …/GameAnim.kt` adds only develop's block; `git diff dbd0a6c 13c04c6 -- …/GameAnim.kt` adds only audio's block, with no `-` lines — pass.
- `git diff -U0 {b39e28e,dbd0a6c} 13c04c6 -- .planning/questions/FOUNDER.md`: each side gains exactly the other side's hunks, with no `-` lines; `grep -o '^## Q-…' | sort | uniq -d` prints nothing — pass.
- `comm -12` over `git diff --name-only 0582e3f {b39e28e,dbd0a6c}` (112 and 130 files): 11 shared files, §2.2's ten plus FOUNDER.md — pass.
- `git merge-tree --write-tree --name-only --messages b39e28e dbd0a6c`: three CONFLICT lines, the same three files — pass.
- `git diff --name-only 61aaaee 13c04c6` (the replayed tree against the merge commit): the three conflicted files only — pass.
- `git grep -nE '^(<<<<<<<|>>>>>>>|=======$)' 13c04c6`: no markers — pass.
- `git log -S'NoWinBanner(' -- …/GameScreen.kt`: `6035840`, 2026-07-13. `git log -G'Suppress\("LongMethod"\)'` gives five commits, and none of the five lists names the banner (`git show 0582e3f:…/GameScreen.kt`, `:142`) — confirmed.
- `git log -S'## Q-023' 0582e3f..dbd0a6c`: `df37c3e`, which entered `develop` at `4ff4ec6` (ux-spec-folds) — confirmed.
- `awk` line lengths over `9f0e67a:GameScreen.kt:140-160`: at most 98, against `.editorconfig`'s `max_line_length = 120` — pass. `git log --format='%ci'` and `stat` of `app/build/test-results/testDebugUnitTest` and `domain/build/reports/kover` show the developer's Check finished after `9f0e67a` — pass.
- `grep -n 'feature/audio-review' docs/tech-design.md`: the 13 sites R.4 lists — read.

### R.6 Open issues / risks
1. **The code-auditor's parallel read** of `GameAnim.kt:45-90` and `TutorialViewModel.kt:83-85`, and its approval of the edited suppression in `9f0e67a`, is the merge's other condition. This section does not stand in for it. The evidence of record for the range stays `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/code-auditor-integration.md` and `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/qa-engineer-integration.md` (§1).
2. **`NoWinBanner`'s line** is owed on `feature/talkback-board` (R.3). It is a comment and gates nothing.
3. **`feature/tech-design-r1-close`** now waits on audio's merge (R.4). Its deadline is unchanged: before checklist 1.1 reads §14.1 at the cut, which now also has to see the reduced-motion row marked cut-blocking.
4. **Retro candidate** (for the producer to note; this is not a lesson proposal): a pre-ruled merge-in measured against the named code branches, not `develop`'s tip, mispredicted its conflict set (R.2 (c)).
5. **Staffing:** under Q-019 the reviewer and the author share Opus. The git checks above are the mitigation, and this section claims no result it did not reproduce.

### R.7 Handoff
Next: producer —
- after the code-auditor's PASS on its read, `git merge --no-ff feature/audio-review` into `develop`;
- commit this section;
- record in STATUS.md: the PASS stands, open issues 1 and 2 are ruled (R.2, R.3), and r1-close is now cut after audio;
- pass R.3's line to the talkback-board developer's dispatch (its B4);
- dispatch the tech-lead for `feature/tech-design-r1-close` with R.4 once audio has merged.

Next: android-game-dev (talkback-board) — R.3 item 3's line, in the hunk that rewrites the board panel's line.
Next: tech-lead (a fresh one for r1-close) — R.4, together with R1's owed edits.
Next: code-auditor — nothing new from this section.

VERDICT: PASS
