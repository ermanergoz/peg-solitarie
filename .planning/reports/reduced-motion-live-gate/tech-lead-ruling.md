# tech-lead report — reduced-motion-live-gate (ruling on open issue 1; routing of open issues 3, 4, 5)
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (report only; read at `90c76cb`: the range `6aa7624..47e7477` and the developer's report. This file is uncommitted, as dispatched, for the producer to commit)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2, so the plugin 1.0.0 rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. Also the snapshot's lessons.md, android-architecture and android-gradle-logic
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The dispatch reports that the Fable limit is reached, so this ruling runs on Opus under Q-019's answer 3 (2026-10-17). The 2026-10-23 addendum restored Fable only while Fable is available. The code under discussion was also written on Opus, so the ruling role and the author share a model. Mitigation: I re-read every source fact this ruling rests on myself, in the cached 1.11.3 sources jars and in the tree. None is taken from the developer's report (Checks).
Fan-out: none
Loops: 0
Read in full: `.planning/reports/reduced-motion-live-gate/android-game-dev.md`; `.planning/reports/audio-review/tech-lead-signoff.md` (all 319 lines; §3 and §5 are this branch's brief); `git diff 6aa7624..47e7477` for `ui/theme/Clay.kt`, `testing/SystemMotionScale.kt`, `GameScreenHintNoticeTest.kt` and the two gate tests; `.planning/questions/FOUNDER.md` Q-019. Read in part: `docs/tech-design.md` on `develop` (`d21ad07`): §8.1's Compose-test-rule paragraph, §9.8 rule 2, §14.1 row RM, §15's gate row, §16 #28; `docs/ux-spec.md` on `develop`: criteria 23d–23g and D-TBa's "each is spoken once"; the seven test files at the lines cited; `ui/game/HintUndecidedNotice.kt:130-190`; the 14 call sites of the gate
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Ruling: A.** A test drives reduced motion through the composition's `MotionDurationScale` (`SystemMotionScale` in the rule's `effectContext`), never through `Settings.Global`. Assertions stay unchanged. B is rejected. Section 3 lists exactly which files change and how.
- **The edits ride this branch** before the code-auditor's review: commit 1 is test-only, and commit 2 is KDoc-only (open issue 4). The auditor reviews `6aa7624..<new head>` in one pass. GameScreenHintNoticeTest's setup change (a) is ratified.
- **Open issue 3** is not on this branch and does not block the cut. QA observes it on the device, the ux-designer judges it, and any fix goes to 3.0.1 with its own brief.
- **Open issue 4:** the settings and test KDocs are fixed on this branch; ux-spec 23e goes to the ux-designer; the tech-design text is owed at the RM close. **No 15e sitting: confirmed** from Compose's source.

## 1. The conflict, verified
- **The audio sign-off §3(c) rested on one premise:** "Compose's flow reads that same key (`WindowRecomposer.android.kt:97`) and a provided element wins over it". That premise holds on a device, but not under Compose's test rule.
  - The v1 rule replaces the window-recomposer factory: `WindowRecomposerPolicy.withFactory({ recomposer })`, `ComposeUiTest.android.kt:764`.
  - It builds its own `Recomposer` from `effectContext` plus its interceptor, frame clock, infinite-animation policy, exception handler and a `Job` (`:591-601`).
  - So `MotionDurationScaleImpl` is never installed. It is the only code that turns `Settings.Global.ANIMATOR_DURATION_SCALE` into a scale (`WindowRecomposer.android.kt:94-130, 335-337, 423-449`). Under the rule, a `Settings.Global` write therefore reaches nothing in Compose.
- **With no element in the context, the clock runs at 1** (`SuspendAnimation.kt:309-313`, `?: 1f`), and the ruled gate is on. The gate and the clock agree, as ruled.
- **Before this branch, the seven tests ran a state no phone has.** Our gate was off, because it read Robolectric's `Settings.Global`. Compose's clock ran at 1.
- **The 43/43 green count is real, but 7 tests in 5 files are now vacuous.** They take the animated path and pass for the same reason the first test of each pair passes. The 7c-race guards are the case that matters. By their own KDocs the race lives on the snap path, and the gate is what selects that path:
  - `GameScreenSoundTest.kt:91-99`: "with animations disabled `runJumpAnimation` takes the snap path";
  - `GameFlowTest.kt:236-241`;
  - `TutorialHapticsTest.kt:72-74`.

  A regression of that race would now pass the Check.
- **The developer's probe count matches the tree.** The six files hold 13 + 8 + 7 + 4 + 5 + 6 = 43 tests. The 6 failures are exactly `BoardCanvasWrongTapReducedMotionTest`'s six `@Test`s. That file is the only one whose rule carries the element: `WrongTapBoards.kt:495-496, 501-502`, the v1 rule with `effectContext`.
- **(a) `GameScreenHintNoticeTest` is consistent with this.** Unedited, it ran 19/20: its reduced-motion test went red, because the 160 ms fade ran under the new gate. That test was not vacuous; it caught the change. Its setup change in `7382351` already has the A shape.
- **§3(c) was the tech-lead role's error.** The premise was checked against the production code path, not the path the tests run on. I own it here; section 9 records the supersession.

## 2. Ruling: A (tech-lead decision)
In a Compose test, reduced motion is the composition's `MotionDurationScale`, provided through the rule's `effectContext` (`testing/SystemMotionScale.kt`). It is never a `Settings.Global` write.

1. **A models the phone.** On a device, the gate and the clock both read the window's scale. With `SystemMotionScale` at 0, both read 0. The old hybrid (gate off, clock at 1) never occurs on a device, so A makes the seven tests more faithful than they were.
2. **A keeps the ruled mechanism whole.** There is one reader of one value, "absent → enabled" holds, and the app makes no `Settings` read of its own. The new live tests and `BoardCanvasWrongTapReducedMotionTest` already drive reduced motion this way; A makes it the only way.
3. **"Stay green unedited" was a means, not a requirement.** It was evidence that the new gate reads what the old one read. Its premise failed, so the requirement falls with it. What survives is §3's last sentence: "No assertion is loosened."

**B is rejected.**
- In production the element is always present. The window recomposer installs it (`WindowRecomposer.android.kt:335-337`), and dialogs and popups compose under that recomposer. B's fallback would therefore run only in previews and tests: production code that exists only for the test suite.
- In those tests, B would rebuild the hybrid above, so the seven tests would go on testing a state no phone has.
- B re-adds the `Settings.Global` read that §3 removed, and splits the gate from the clock again wherever the element is absent.
- It contradicts §3 and §16 #28 ("enabled when the element is absent"), and `AnimationsEnabledGateWithoutScaleTest` pins the opposite.

**Also rejected:**
- **(C) A test-side bridge** that maps Robolectric's `Settings.Global` into a scale element, so the old writes keep working. It re-implements Compose's observer in test code to keep a story that is no longer true.
- **(D) Dropping the seven tests** as covered by the new live tests. The live tests cover the board, tutorial and Results visuals under reduced motion. They do not cover the cue layering, the haptics or the Results sound stagger.

## 3. The test files that change, and how (commit 1)
Commit 1 is one new commit, test-only: `test(motion): drive reduced motion through the composition's scale`. The table below is the whole list of files it may touch. Paths are under `app/src/test/java/com/app/pegsolitaire/`.

| File | What changes | What must not change |
|---|---|---|
| `ui/game/GameScreenSoundTest.kt` | Add `private val motion = SystemMotionScale()`. The rule becomes `createComposeRule(effectContext = motion)`, still v1. `disableOsAnimations()`'s body becomes `motion.turnAnimationsOff()`, and its KDoc (`:356-361`) is rewritten for the new mechanism. In the class KDoc, "the `> 0` ordinal guards" (`:57`) is reworded to name the guards as built after `335bc3b`: `NewOrdinalEffect` for Select, Deselect, Undo and InvalidTap | The three tests' names and bodies (they call the helper, so no line in them changes); every assertion; the other ten tests |
| `ui/game/GameFlowTest.kt` | The same `motion` and rule. The `Settings.Global.putFloat` line at `:245` becomes `motion.turnAnimationsOff()` | The test's name and its other lines; every assertion; the other seven tests |
| `ui/results/ResultsCelebrationSoundTest.kt` | The same `motion` and rule. `disableOsAnimations()`'s body becomes `motion.turnAnimationsOff()`. The class KDoc's D-13 bullet says "the animation scale at 0" wherever it names the OS setting as the mechanism | Test bodies; assertions; the other six tests |
| `ui/onboarding/TutorialHapticsTest.kt` | The same `motion` and rule. The write at `:78-82` becomes `motion.turnAnimationsOff()` | As above; the other three tests |
| `ui/onboarding/TutorialSoundTest.kt` | The same `motion` and rule. The helper's body becomes `motion.turnAnimationsOff()`, and its KDoc (`:208-213`) is rewritten | As above; the other four tests |
| `ui/components/BoardCanvasWrongTapReducedMotionTest.kt` | Delete the dead `@Before turnTheSystemAnimationsOff()` (`:51-58`) and its constant (`:181`). Correct the class KDoc at `:26-27`: the scale is 0 in the composition's `MotionDurationScale`, which the board's gate and every `Animatable` read | Its rule (`createWrongTapComposeRule(animationsOn = false)`); every test |
| `ui/game/GameScreenHintNoticeTest.kt` | (a) is ratified as done in `7382351`. Add the class-KDoc sentence that §8.1 owes from "the first branch that next touches that file": which behaviour the v2 rule gives this class. Its developer documents it at `:521` and `:618`: the paused-clock recreation and the tap-write delivery | Everything else |
| The four new classes on the v2 rule: `ui/theme/AnimationsEnabledGateTest.kt`, `ui/theme/AnimationsEnabledGateWithoutScaleTest.kt`, `ui/game/GameScreenCueRotationTest.kt`, `ui/results/ResultsStarsLiveMotionTest.kt` | §8.1: either each class KDoc names the behaviour the v1 rule does not give it, shown by the test going red on v1, or the class moves to v1. Providing the scale element is no reason by itself: v1 takes `effectContext` (`AndroidComposeTestRule.android.kt:66`), and `WrongTapBoards.kt:501-502` already uses it | The tests and their assertions |

Rules for every file:
- Remove anything that becomes unused: `android.provider.Settings`, `RuntimeEnvironment` where nothing else uses it, and the `ANIMATIONS_OFF` / `ANIMATIONS_DISABLED_SCALE` constants.
- Test names stay as they are. "When the OS animation scale is zero" still names the condition.
- Rule versions stay as they are, so the commit adds no `w:` line (§9.8 rule 2). The v1 overload that takes `effectContext` carries the same single deprecation warning as the no-argument call it replaces, whose own overload is `HIDDEN`.
- No file outside the table changes. No test is added, and none is removed.

**If one of the seven goes red under A,** the developer does not touch its assertion. It stops and reports to the tech-lead through the producer. Under A the gate and the clock are both at 0, which is the phone's state; the hybrid ran the clock at 1. A red there would be a defect on the device path that the hybrid hid.

## 4. What the developer reports for commit 1
- **The commit's footprint.** `git show --stat` lists only files from the table in section 3. `git show -U0 <sha> | grep -E '^[-+].*(assert|containsExactly|isEqualTo|hasSize|isEmpty)'` prints nothing.
- **The probe, re-run.** The developer's temporary `check(gate on)` in `rememberAnimationsEnabled()` is never committed. Across the seven files' 63 tests it must fail in **exactly 14**:
  - the seven named in open issue 1;
  - `BoardCanvasWrongTapReducedMotionTest`'s six;
  - `GameScreenHintNoticeTest.withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame`.

  It must fail in none of the other 49. On `90c76cb` the same probe fails in 7, so the difference shows the seven guards reach the path again.
- **The green runs.** With the probe removed, the seven files pass 63/63. `./scripts/check.sh` is green, with the test total unchanged at 1,575 and 0 new `w:` lines.
- **The four v2 classes.** For each: the KDoc sentence and the v1 red behind it, or the move to v1 and its green.

## 5. It rides this branch, before the code-auditor's review
**Yes.** Commits 1 and 2 go on `bugfix/reduced-motion-live-gate` after `90c76cb`, and the code-auditor reviews `6aa7624..<new head>` in one pass.
- **The coverage hole is this branch's own consequence.** Its mechanism is what made the seven guards vacuous. Merging a cut-blocking accessibility fix whose reduced-motion guards test nothing would trade a visible defect for an invisible one.
- **Now it costs no loop; later it would cost one.** The review has not started, so the branch is still at Loops: 0.
- **It decides nothing new.** Section 3 fixes every line, so the auditor checks the diff against this ruling, not against its own design judgement.
- **No other branch is a better home.** talkback-board edits `GameScreen.kt`, not these test files, and `develop` would carry the vacuous guards between the two merges.

The developer's re-dispatch writes `.planning/reports/reduced-motion-live-gate/android-game-dev-ruling.md`, so the first report stays as its record.

## 6. Open issue 3: the notice re-spoken on a live toggle (routed)
**The fact.**
- `UndecidedNoticeSlot` (`ui/game/HintUndecidedNotice.kt:170-189`) uses the gate to pick between two structures: an `AnimatedVisibility` around the notice, or the plain notice.
- With the gate live, a toggle while the notice is up disposes one structure and composes the other. The notice's node is new, and a new polite live region speaks. Criterion 23d relies on exactly that mechanism for each raise.
- A toggle from off to on also replays `clayEntrance(0)`.
- Of the 14 call sites, this is the only one where the gate picks a structure that hosts a live region. The others pick an animation spec, snap, or skip a decorative effect.

**Ruling: not this branch, and not cut-blocking.**
- **Reach.** It takes a live change of the system animator scale while the notice is showing. On a phone that means leaving for Settings (Accessibility, "Remove animations"; there is no stock quick toggle) and coming back within the notice's display time, or an `adb shell settings put`. On the way back, TalkBack speaks the window anyway.
- **Harm.** The text equals the visible string; it is still true and still polite. The cost is a repeat, not a loss or an error, so the stack's accessibility rules the product baseline names still hold.
- **Cost of a fix.** It is not a one-liner. Any single-structure shape has to keep two criteria: 23e's "gone one frame after t₀ + 5 000 ms, no exit frames", and 23d's new node per raise. An always-present `AnimatedVisibility` will probably cost the paused-clock harness one frame, the delivery lag that `BoardCanvasWrongTapReducedMotionTest.kt:33-35` documents. On a cut-blocking branch, that is a loop risk for a rare duplicate.

**Route (producer):**
- **qa-engineer, in this branch's device session:**
  - with TalkBack on, raise the notice;
  - run `adb shell settings put global animator_duration_scale 0` with the app resident, then the same command with `1`;
  - count the announcements of the notice text in each direction, and note whether the notice visibly re-enters;
  - then leave for Settings and come back within the display time, with a long "Time to take action" so that the notice outlives the trip;
  - quote the TalkBack or logcat lines.
- **ux-designer:** judge it against 23d ("Spoken politely, every time", per raise) and D-TBa's "each is spoken once". Decide whether a toggle counts as a new occasion, and write the decision into the ux-spec.
- **If the ux-designer calls it a defect:** it becomes a 3.0.1 item, with a tech-lead brief on the fix's shape: node identity kept across a gate flip, 23e's one-frame exit kept, and red first on a semantics-id test. If `feature/talkback-board` touches the slot before then, the fix rides that branch. **If not a defect:** it is closed, with a line under 23d.

## 7. Open issue 4: stale text (routed)
| Text | Ruling | Where |
|---|---|---|
| **The settings KDocs.** `ui/settings/SettingsUiState.kt:28-32`: `soundOn` says "Persisted only this phase — there is no audio engine yet (D-16)…", and `hapticsOn` says GameScreen "fires a light tick on each jump only". `data/SettingsRepository.kt:17-19`: "persisted flag only for now; no audio engine yet, D-16", and `hapticsOn` "gates the light haptic fired on a jump" | It is the falsehood that F-2's KDoc half was ruled to fix (audio sign-off §5), in two more files. Use `47e7477`'s SettingsScreen wording: Sound's gate is the sound player's own (SOUND-05); Haptics gates the jump's light tick (D-15) and the wrong tap's reject buzz (D-06b) | This branch, before the review: commit 2, KDoc-only, `docs(settings): drop the stale audio-engine lines from two KDocs`. Non-gating: if it costs a loop, it drops to 3.0.1 |
| **The test KDocs.** `GameScreenSoundTest`'s "`> 0` guards"; the helper KDocs that say the gate reads `Settings.Global` "`remember`ed per context" (`GameScreenSoundTest.kt:356-361`, `TutorialSoundTest.kt:208-213`); `BoardCanvasWrongTapReducedMotionTest.kt:26-27` | Corrected in commit 1 (section 3), since commit 1 edits those files anyway | This branch |
| **`docs/ux-spec.md` 23e:** "On Robolectric with `Settings.Global.ANIMATOR_DURATION_SCALE` set to 0, …" | The behaviour stands, but the test setup it names no longer works. Reword it as "with the composition's animation scale at 0, as on a phone with Remove animations on" | ux-designer, at the next fold-in |
| **`docs/tech-design.md` row RM and §16 #28:** "The seven test files … stay green unedited" | Superseded by section 2 | Tech-lead, at the RM close (section 9) |

## 8. Open issue 5: no 15e sitting (confirmed)
§3's 15e condition was "Home's first frame loses one `Settings` read and gains no call". Compose's own source shows that it holds as built.

**Before this branch:**
- Home's first composition made our own read, `remember(context) { Settings.Global.getFloat(…) }`.
- Home's entrance animations then started in that same first frame and read `coroutineContext.durationScale`. These are the `clayEntrance` calls (`HomeHero.kt:75`, `HomeStage.kt:98`, `HomeScreen.kt:487, 659`, `HomeProgressRibbon.kt:84`) and `HomeBlobs`' infinite drift (`HomeScreen.kt:156`). That read started Compose's observation.

**After this branch:**
- The first `rememberAnimationsEnabled()` in Home's composition (`HomeBlobs.kt:33`) makes Compose's first read of `MotionDurationScaleImpl.scaleFactor`.
- That getter starts the same observation, once per window (`WindowRecomposer.android.kt:430-449`). The observation's one `Settings.Global.getFloat` is its `stateIn` initial value (`:96-97, 127-130`), cached per application context (`:94`).
- The `ContentObserver` registration (`:117`) runs inside the collector that the getter `launch`es, so it is dispatched rather than run in composition, as before.

**Net:** the observer start moves from Home's first effects to Home's first composition, inside the same frame. Home's first frame does one `Settings` read fewer, and gains no SDK call or registration of ours. No sitting is owed. PLAT-08's phone measurement at release covers the absolute cost.

**One device-only check for QA's session.** The first production read of the scale now happens in composition.
- *Why.* Compose throws `MotionDurationScale scale factor requested before recomposer loop start` (`:444`) when a composition reads the scale before its window recomposer's loop has set the scope (`:375-376`).
- *Why it should not happen.* In an Activity the lifecycle is at least CREATED when the view attaches, so ON_CREATE is delivered synchronously, before the first composition.
- *Why it still needs a device.* No JVM test runs the window recomposer, so the Check cannot see this path.
- *QA:* make a cold start with "Remove animations" off and with it on, rotate, and restore after process death (`adb shell am kill` after backgrounding). Logcat must be free of that exception; quote the filter line.

## 9. Owed to `docs/tech-design.md` (not edited here, as dispatched)
**Carrier:** the RM close sync, a docs-only branch off `develop` after RM merges. It runs like r1-close: the tech-lead edits, the code-auditor reads it as text, and a fresh tech-lead signs it off.
1. **§14.1 row RM and §16 #28.** Replace "The seven test files that drive the gate through `Settings.Global` on `6aa7624` stay green unedited, and the developer reports the count …" with:
   - Under Compose's test rule, no `MotionDurationScale` exists unless the test provides one (`ComposeUiTest.android.kt:591-601, 764`), so a `Settings.Global` write reaches neither the gate nor the clock.
   - The seven files drive reduced motion through `SystemMotionScale` in the rule's `effectContext`, with assertions unchanged (`reduced-motion-live-gate/tech-lead-ruling.md` §2–§3).
   - `GameScreenHintNoticeTest`'s setup change has that shape.
2. **§8.1:** one sentence after the Compose-test-rule paragraph: "A test that needs reduced motion provides it as the composition's `MotionDurationScale` (`testing/SystemMotionScale.kt`) in the rule's `effectContext`. A `Settings.Global` write reaches nothing under Compose's test rule."
3. **§17:** one row. It supersedes the audio sign-off §3(c)'s premise ("Compose's flow reads that same key … stay green as they are") and RM/#28's "stay green unedited", in favour of this ruling.
4. **§16:** an item for open issue 3 if the ux-designer opens it (section 6); otherwise nothing.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rm/.planning/reports/reduced-motion-live-gate/tech-lead-ruling.md`: this ruling. It is the only file written.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-rm` unless named. No Gradle was run and nothing was committed: a ruling re-runs no gate.
- `cat .claude/rules/VERSION` (0.6.2) against the plugin's `plugin.json` (1.0.0): older, so the plugin rule files were read in full — pass.
- `git status --short`: clean. `git rev-parse --short HEAD`: `90c76cb`. `git merge-base --is-ancestor 6aa7624 HEAD`: true — pass.
- `git diff --stat 6aa7624..HEAD`: 13 files, +615 / −66; the gate, helper, notice-test and gate-test hunks were read — read.
- `unzip` of `ui-test-android-1.11.3-sources.jar`: at `ComposeUiTest.android.kt:591-601` the recomposer context is `effectContext` plus the rule's own elements, and at `:764` the rule calls `WindowRecomposerPolicy.withFactory({ recomposer })`. No scale element exists unless `effectContext` carries one — confirmed.
- `ui-android-1.11.3-sources.jar`, `WindowRecomposer.android.kt`: the flow and its observer (`:94-130`), the element chosen (`:335-337`), the scope set on ON_CREATE (`:375-376`), and the impl with its lazy start and its `error(…)` (`:423-449`, `:444`) — confirmed.
- `animation-core-android-1.11.3-sources.jar`, `SuspendAnimation.kt:309-313`: `this[MotionDurationScale]?.scaleFactor ?: 1f` — confirmed.
- `ui-test-junit4-android-1.11.3-sources.jar`: the v1 `createComposeRule(effectContext: CoroutineContext)` is at `AndroidComposeTestRule.android.kt:66` with `DeprecationLevel.WARNING`, and the no-argument `expect` is `HIDDEN` (`ComposeTestRule.jvmAndAndroid.kt:272-275`) — confirmed.
- `grep -c '@Test'` over the seven files: 13, 8, 7, 4, 5, 6 and 20, which is 43 for the six files and 63 with the notice test. This matches the developer's 43 — pass.
- `git grep -n 'ANIMATOR_DURATION_SCALE…' -- app/src`: main code has no read of the key (only the KDoc at `Clay.kt:212`). Tests write it in the five files, in `BoardCanvasWrongTapReducedMotionTest` (dead), and in `AnimationsEnabledGateWithoutScaleTest` (on purpose) — read.
- `git grep -n 'import androidx.compose.ui.test.junit4'` over the branch's changed tests: the four new classes and `GameScreenHintNoticeTest` use v2; `WrongTapBoards.kt` uses v1 with `effectContext` — read.
- `git grep -n -A6 'rememberAnimationsEnabled()' -- app/src/main`: 14 call sites. Only `HintUndecidedNotice.kt:170` picks a structure that hosts a live region — read.
- `git show develop:docs/tech-design.md`: row RM (`:1571`), §15 (`:1726`), #28 (`:1766`), §8.1's v2 paragraph and §9.8 rule 2's exception — read.
- `git show develop:docs/ux-spec.md`: `:6250-6283` (23d–23g) and `:6535-6540` — read.
- `awk '/^## Q-019/,/^## Q-018/' .planning/questions/FOUNDER.md`: answer 3 and the 2026-10-23 addendum — read.

## Open issues / risks
1. **A red under A among the seven** would be a device-path defect (section 3). It is not expected, and it is not ruled in advance: it comes back to the tech-lead.
2. **Open issue 3's re-speak ships in 3.0.0** unless the ux-designer rules otherwise and a fix makes the cut (section 6).
3. **The four v2 classes** are this branch's to settle under §8.1 (section 3). I make no finding on them here; the code-auditor reviews the result.
4. **Q-019:** this ruling, the code and, on Opus, the review share a model. The auditor's own mutation probe on one 7c-race guard is the strongest single check that commit 1 restored the guards. Under Q-019 the auditor runs it, not trusting section 4's counts.
5. **The developer's open issue 2** (no git hooks in this worktree; trailers reworded by hand) is studio tooling. It is the producer's, and it is not ruled here.
6. **Retro candidate** (for the producer to note; not a lesson proposal): a "stays green unedited" premise was checked on the production code path (the window recomposer), not on the path the tests run (the test rule's own recomposer). The result, green but vacuous guards, is invisible to a green Check.

## Handoff
Next: producer —
- commit this file;
- dispatch a fresh android-game-dev for commits 1 and 2 (sections 3, 4 and 7), writing `android-game-dev-ruling.md`;
- then the code-auditor on `6aa7624..<new head>`, with this ruling among its requirements;
- the qa-engineer's device session as the developer's handoff names it, plus section 6's TalkBack toggle and section 8's start-path logcat check;
- route section 6's judgement and section 7's 23e line to the ux-designer;
- record in STATUS.md: ruling A, and that RM still blocks the cut.

Next: android-game-dev — commit 1 exactly as section 3 lists it, then commit 2 (section 7), with the evidence of section 4. Stop and report on any red among the seven.
Next: code-auditor — the whole range in one pass; commit 1 against section 3's table, file by file; its own mutation probe on one 7c-race guard (Q-019 mitigation); the four v2 classes against §8.1.
Next: qa-engineer — the branch's device check, plus sections 6 and 8.
Next: ux-designer — section 6's judgement; the wording of 23e.
Next: tech-lead (a fresh one) — the sign-off, which checks commit 1 against this ruling; later, the section 9 sync at the RM close.

RULING: A

## Finding 1 routing — 2026-10-24
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (report only; read at `9c56347`. The ruling above is committed unchanged at `d1a54fa`. This section is uncommitted, as dispatched, for the producer to commit)
Ran on: claude-opus-5-5 at effort xhigh (policy). The same staffing deviation as above: Q-019 answer 3, the Fable limit reached, the ruling role sharing the author's model.
Read in full: `.planning/reports/reduced-motion-live-gate/code-auditor.md`. Read in part: `ui/onboarding/TutorialScreen.kt:345-375`; `ui/results/ResultsCelebration.kt:30-48` (`rememberPopIn`); `SuspendAnimation.kt:222-338` (animation-core 1.11.3 sources)
Lessons applied: none

### F1.1 What the finding is, verified
- **The code.** `CompletionStar` (`TutorialScreen.kt:347-361`) is the only main-code effect keyed on the gate: `LaunchedEffect(animationsEnabled) { if (animationsEnabled) scale.animateTo(SETTLED_SCALE, overshootSpring()) }`. `git grep 'LaunchedEffect(animationsEnabled'` finds this one hit, which matches the auditor's census of the 14 call sites.
- **What a live flip does.** Turning animations off mid-pop changes the key, so Compose cancels the running spring. The new effect does nothing, and the star keeps whatever scale it had reached. The auditor's probe measured about 0.53× until the screen leaves.
- **Why it is this branch's regression.** A running animation re-reads the scale on every frame (`SuspendAnimation.kt:278-282`), and at a scale of 0 it jumps to its end (`:334-335`).
  - On `6aa7624` the gate was frozen, so the effect's key never changed. On a live toggle, Compose's clock would have finished the spring on its next frame, at 1×.
  - The stuck star exists only because this branch made the gate live: the key flips and cancels the spring before that next frame comes.

### F1.2 Ruling: a rider on this branch now, and it gates the branch
**A rider, now** (tech-lead decision):
- It is a reduced-motion defect that this branch's own fix created. A cut-blocking accessibility fix should not ship one, and §3's own deadline (rules/compose.md: "animation respects reduced motion") covers it.
- The cost is one composable and one test on the `SystemMotionScale` harness that is already there. QA has not started, so the auditor's short read runs before QA and no device session is spent twice.
- 3.0.1 would ship a known regression of 3.0.0's own fix, with the fix already in hand. Low likelihood does not outweigh a near-zero cost.

**It gates the branch.** Unlike the §5 riders of the audio sign-off, which fixed defects older than the branch, this one cannot drop to 3.0.1: dropping it would ship the regression. If the short read does not pass it, the producer brings it back to the tech-lead. It is never dropped silently.

**The shape (tech-lead decision): the house idiom, not the auditor's `else` branch.**
- *The house idiom.* Key the effect on `Unit` and always `animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())`. That is how `rememberPopIn`, the Results star this composable claims to share a vocabulary with, already does it (`ResultsCelebration.kt:37`). The initial value still comes from the gate (`remember { Animatable(if (animationsEnabled) STAR_POP_START else SETTLED_SCALE) }`, unchanged).
- *What it then does in each case:*
  - composed with animations off: the star starts at 1×, and the `animateTo` is a no-op;
  - composed with animations on: the star springs;
  - turned off mid-pop: the composition's clock ends the spring on its next frame at 1×, because the gate and the clock are one value (§2).
- *Why not `else scale.snapTo(SETTLED_SCALE)`.* It would fix the symptom but keep the only effect that restarts on a gate flip, the very construct that caused the defect. The idiom removes that construct, so the 14 call sites have no gate-keyed effect left.

### F1.3 What changes, and the evidence
**Commit 3.** Production plus test: `fix(motion): let the clock settle the tutorial's completion star`, after `9c56347`.
- **Main code:** only `ui/onboarding/TutorialScreen.kt`, inside `CompletionStar`. The effect's key becomes `Unit` and the `if (animationsEnabled)` guard goes. The KDoc gains one line saying that reduced motion is the clock's, as in `rememberPopIn`. Nothing else in the file changes.
- **Test, red first:** one new test on the live-motion harness (`SystemMotionScale` in `effectContext`, v1 rule per §8.1). Its name reads as a sentence, for example `aStarCaughtMidPopByAnimationsTurningOffSettlesWholeOnTheNextFrame`.
  - Complete the tutorial with animations on, and advance into the spring.
  - Call `motion.turnAnimationsOff()`.
  - Assert the star reads as settled by the next frame, and still settled after 90 frames. Read it the way the auditor's probe did: its pixel reading was 656 settled, 76 mid-pop, and 184 when stuck (about 0.53×).
  - On `9c56347` the test must go red at the stuck reading; after the change it goes green.
- **Placement and count:** the test lives in an existing tutorial test class that already composes the completion card, or a new `TutorialCompletionStarLiveMotionTest`; the developer chooses and says which. No other test is edited. The test total becomes 1,576.
- **The developer reports** in `android-game-dev-finding1.md`:
  - the red and the green, with the measured sizes;
  - `./scripts/check.sh` green, with no new main `w:` line;
  - `git show --stat` showing the two files only.
- **The code-auditor's short read** goes in `code-auditor-finding1.md`: commit 3 only, against F1.2 and F1.3, plus its own mutant (the `LaunchedEffect(animationsEnabled)` key restored makes the new test red). It is a read, not a loop of the range. Then QA's device session runs on that head. QA gets no extra step: a 0.7 s spring is not a fair thing to toggle by hand, and the JVM test is the evidence of record.

### F1.4 Owed to `docs/tech-design.md` at the RM close (adds to §9 above)
5. **§16 #28's mechanism paragraph**, one sentence: "No effect is keyed on the gate. An effect that runs a decorative animation starts it unconditionally, or reads the gate once at its start, and lets the composition's clock settle it; keying an effect on `rememberAnimationsEnabled()` cancels a running animation on a live flip and strands it (`reduced-motion-live-gate` finding 1)."

### F1.5 Checks
- `git log --oneline -8`: the head is `9c56347`; the ruling was committed at `d1a54fa`; `git diff --stat d1a54fa HEAD -- …/tech-lead-ruling.md` prints nothing — read.
- `sed -n 325,375p ui/onboarding/TutorialScreen.kt`: `CompletionStar` at `:347-361`; `LaunchedEffect(animationsEnabled)` at `:350`; `SETTLED_SCALE` at `:368` and `STAR_POP_START` at `:374` — confirmed.
- `git grep -n 'LaunchedEffect(animationsEnabled\|LaunchedEffect(rememberAnimationsEnabled' -- app/src/main`: one hit — confirmed.
- `ResultsCelebration.kt:30-48`: `rememberPopIn` uses `LaunchedEffect(Unit)` with an unconditional `animateTo` and the initial value from the gate — confirmed.
- `animation-core-android-1.11.3-sources.jar`, `SuspendAnimation.kt:278-282` (scale read each frame inside the run loop) and `:334-335` (scale 0 means play time equals duration) — confirmed.
- No Gradle was run and nothing was committed.

### F1.6 Handoff
Next: producer —
- commit this section;
- dispatch a fresh android-game-dev for commit 3 (F1.3), writing `android-game-dev-finding1.md`;
- then the code-auditor's short read, writing `code-auditor-finding1.md`;
- then QA's device session on that head, as already routed (§6, §8);
- then a fresh tech-lead for the sign-off;
- in STATUS.md: Finding 1 is a gating rider; RM still blocks the cut.

Next: android-game-dev — F1.3, red first. Stop and report if the idiom shape fails the test.
Next: code-auditor — F1.3's short read and mutant.
Next: tech-lead (a fresh one) — the sign-off covers commit 3 against F1.2.

FINDING 1: RIDER (GATING), HOUSE-IDIOM SHAPE

## F1 amendment — 2026-10-24
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (report only; read at `e44e716`, where the Finding 1 section is committed. The developer's uncommitted `TutorialScreen.kt` and `TutorialCompletionStarLiveMotionTest.kt` were left untouched. This section is uncommitted, as dispatched)
Ran on: claude-opus-5-5 at effort xhigh (policy). The same staffing deviation as above (Q-019 answer 3).
Lessons applied: none

### FA.1 The developer's stop is right; the error is mine
In animation-core 1.11.3 the run loop reads the scale before it waits for the frame. `SuspendAnimation.kt:278-282`:
- `val durationScale = coroutineContext.durationScale` at `:279`;
- then `animation.callWithFrameNanos { … doAnimationFrameWithScale(it, durationScale, …) }` at `:280-281`.

So the spring is already waiting for frame N, holding the scale it read before the switch, when animations are turned off. Frame N plays at the old scale, and the new scale takes effect from frame N+1. That is the developer's reading, 165 then 639 px. F1.1 cited those same lines but missed the order, so "by the next frame" in F1.1, F1.2 and F1.3 was my error. This amendment supersedes it in four places:
- F1.1's "on its next frame" means "within two frames".
- F1.1's "before that next frame comes" means "before the clock's new scale reaches it".
- F1.2's "on its next frame" means "within two frames".
- F1.3's assertion is replaced by FA.2 below.

The old head's behaviour was "within two frames" too, so the regression argument for the rider stands unchanged.

### FA.2 Ruling: option 1 (tech-lead decision)
**Keep the house-idiom shape, and amend the assertion.**
- **The assertion becomes:** "The star reads settled no later than the second frame after the switch, and still settled 90 frames later."
- **The test does not assert the first frame's value.** It advances two frames and asserts settled, then advances 90 frames and asserts settled again. Pinning the one-frame lag would tie the test to an internal detail of Compose's loop that a Compose update may remove. "No later than" stays true either way.
- **Red on `e44e716` still holds:** the stuck star reads 165 at both points.

**Why not option 2:**
1. **The lag is Compose's own and applies to every animation.** Every running `Animatable` in the app takes the new scale one frame late on a live toggle. Under the idiom, the star now behaves exactly like all of them, which is the point of making the gate and the clock one value (§2).
2. **One frame of the star's own spring is about 16 ms,** on a path the auditor rates as low likelihood. That is no reason to bring back a gate-keyed effect, the construct that caused Finding 1 and that F1.4 is about to forbid in the tech design.
3. **Option 2 would only move the lag.** The gate itself flips in the recomposition of frame N, and a restarted effect's `snapTo` lands after that recomposition is applied. Option 2 would buy the same frame at the cost of the rule.

**The test name** that F1.3 suggested ends "…OnTheNextFrame". Rename it to a sentence that matches FA.2, for example `aStarCaughtMidPopByAnimationsTurningOffSettlesWholeWithinTwoFrames`.

### FA.3 The KDoc's final wording (`CompletionStar`, `TutorialScreen.kt`)
The developer's uncommitted line says "…and at scale 0 ends on its next frame". That carries my error. The KDoc reads, in full:

    /**
     * One gold star, popped in with the Results star vocabulary. Decorative: it announces nothing.
     *
     * Reduced motion is the clock's, as in `rememberPopIn`: the spring always runs, and its effect is never
     * keyed on the gate. Composed with animations off, the star starts settled; turned off mid-pop, the spring
     * takes the new scale from the composition's clock and ends at full size within two frames.
     */

`rememberPopIn` stays in backticks rather than a `[link]`, because it is `internal` in another package. Every line is at most 120 characters.

### FA.4 Owed text (amends F1.4)
§16 #28's sentence reads as F1.4 has it, with one clause added after "lets the composition's clock settle it": "(a running animation takes a new scale from its next frame, so within two frames of a switch)".

### FA.5 Checks
- `git log --oneline -4`: the head is `e44e716`. `git status --short`: the developer's `M …/TutorialScreen.kt` and `?? …/TutorialCompletionStarLiveMotionTest.kt`, both untouched — read.
- `animation-core-android-1.11.3-sources.jar`, `SuspendAnimation.kt:276-282`: the scale is read at `:279` before the frame wait at `:280`, and used in the callback at `:281` — confirmed.
- `sed -n 344,348p …/TutorialScreen.kt`: the developer's current KDoc line, "at scale 0 ends on its next frame" — read. The text is replaced by FA.3; the edit is the developer's.
- No Gradle was run and nothing was committed.

### FA.6 Handoff
Next: android-game-dev — finish commit 3 with FA.2's assertion, FA.2's test name and FA.3's KDoc. The red is on `e44e716` and the green after the change. Report both readings at the first and second frames after the switch, for the record only; the test asserts the second.
Next: code-auditor — the short read of F1.3, with FA.2 and FA.3 in place of F1.3's "next frame"; the same mutant.
Next: producer — commit this section with the developer's commit 3 report; nothing else changes in F1.6's order.

F1 AMENDMENT: OPTION 1
