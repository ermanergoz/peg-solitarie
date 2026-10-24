# android-game-dev report — audio-integration
Date: 2026-10-23
Branch: feature/audio-review (44b14bb..8663710, 19 commits on 62c774d); this report: committed by the producer
Skills loaded: ergoz-soft:standards (role android-game-dev; studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing), superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: check-gates (PegSolitarie-gates) and hint-undecided (PegSolitarie-hint), as the producer noted; no other build ran in this worktree
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: the agent returned this report as text because the harness refused a report file from a
> subagent; the producer wrote it here verbatim (HTML entities unescaped) and committed it.

## Summary
- All 17 items of the tech-lead's audio scope are built test-first, in the brief's order: Plan A (items 1–7), then Plan B (items 8–17).
- The Check passed after Plan A (at `df903b3`) and after Plan B (at `8663710`).
- No `@Suppress`, baseline, `@Ignore` or `!!` was added, and main code has no new warning.
- The main deviations are WT-19 tested as its contract instead of golden images, and the wrong-tap haptic and announcement keyed so a rotation does not replay them.
- QA's device checks remain.

## Outputs
| # | Item | Done | Commit(s) | Proving tests |
|---|---|---|---|---|
| 1 | P5: credits test rewritten, asset format test | yes | 44b14bb, 6b4eb08 | `AudioCreditsTest` (9), `AudioAssetFormatTest` (6), `CreditsTableTest` (4) |
| 2 | `.ogg` → `.wav` KDoc; `VOLUME_DESELECT` RMS from CREDITS.md | yes | 1572197 | grep `\.ogg` over `app/src` → 0 |
| 3 | P4a: `VOLUME_JUMP` 1.0, test rename | yes | 2e82c3e | `SoundParamsTest.theJumpIsMixedAsLoudAsThePeakRuleAllows`, `.noCueIsMixedAboveTheWinStinger` |
| 4 | `MAX_STREAMS` KDoc | yes | 13b0a83 | comment only |
| 5 | Silent/Vibrate speaker gate (P3) | yes | c2a16dc, 8b62414, 930d816 | `SystemAudioSnapshotTest` (7), `AudioRouteClassifierTest` (3), `AndroidSystemAudioStateTest` (7), 12 gate tests in `SoundPoolPlayerTest` |
| 6 | P8: stop sound in the background | yes | 116df6e | `ProcessAppVisibilityTest`, 3 background tests in `SoundPoolPlayerTest` |
| 7 | V1: volume keys drive media volume | yes | df903b3 | none by design; device check D10 |
| 8 | Tutorial Haptics seam, shared jump haptic | yes | 8f18049 | tests 1–12 (`TutorialViewModelTest`, `TutorialHapticsTest`, `GameFlowTest`) |
| 9 | Wrong-tap signal (ordinal + cell) | yes | 09ce3eb, 137884a, 8663710 | `GameViewModelSoundTest` (WT-1…WT-4, WT-6 at all four install sites), two `TutorialViewModelTest` tests, `BoardGeometryTest` (WT-5) |
| 10 | The shake | yes | 8f4bf24 | `WrongTapMotionTest` (4), board keyframe test |
| 11 | The redirect (ring nudge, dot re-pop) | yes | 8f4bf24 | `WrongTapTest` (5), board tests for WT-8 and WT-9 |
| 12 | Reduced-motion dashed mark | yes | 8f4bf24 | `BoardCanvasWrongTapReducedMotionTest` (6) |
| 13 | Interruption; read-only board; `wrongTapCorrection` flag | yes | 8f4bf24 | `BoardCanvasWrongTapTest` (11), `WrongTapFeedbackTest` (4) |
| 14 | Reject haptic | yes | fe32924 | `WrongTapHapticPolicyTest` (test 13), `GameScreenWrongTapTest` (WT-12, and WT-6's haptic half) |
| 15 | Polite announcement | yes | 5e0af0d, cc65702 | `GameScreenWrongTapTest` (4 announcement tests + carried ordinal) |
| 16 | Sound unchanged; visuals and haptic don't depend on Sound | yes | fe32924 (tests only) | WT-13 tests; `InvalidTapSound` is byte-identical to develop |
| 17 | Tutorial: redirect only | yes | 9334984 | `TutorialWrongTapTest` (3), `TutorialSourceGuardTest`, test 10, `TutorialSoundTest`, tutorial-board pixel tests |

Main sources:
- `audio/`: new `SystemAudioState.kt`, `AudioRouteClassifier.kt`, `GameAudioAttributes.kt`, `AndroidSystemAudioState.kt`, `AppVisibility.kt`
- `data/HapticsSetting.kt` (new)
- `ui/game/`: new `JumpHapticEffect.kt`, `WrongTap.kt`, `WrongTapMotion.kt`, `NewOrdinalEffect.kt`, `WrongTapAnnouncement.kt`
- `ui/components/WrongTapFeedback.kt` (new)
- Changes in `SoundPoolPlayer.kt`, `BoardCanvas.kt`, `GameScreen.kt`, `GameViewModel.kt`, the tutorial classes, `strings.xml`, `dimens.xml` and `app/build.gradle.kts`.

Test helper: `ui/components/WrongTapBoards.kt` draws the board under test beside a control board. The control receives every action except the wrong tap, so "nothing else moves" becomes "pixels equal the control's".

## Checks
- `./gradlew check` at df903b3 (after Plan A) — pass. 11m 33s; `:app` 1,118 tests, `:domain` 275, `build-logic` 54, none failed or skipped; lint 0 errors, 63 warnings, of which 5 new (`InlinedApi`, the expected O8).
- `./gradlew --max-workers=4 check` at 8663710 (after Plan B) — pass. 11m 19s.
  - `:app` 1,185 tests, `:domain` 274 + depth sweep 1, `build-logic` 54; 0 skipped, failed or errored.
  - App tests and lint ran in this run. The domain and build-logic tests, ktlint and detekt were up to date from earlier runs, with unchanged inputs.
  - Lint: 0 errors, 63 warnings, the same total as after Plan A. The flagged lines in `strings.xml` and `dimens.xml` are outside my additions.
- `./gradlew --offline --max-workers=4 :app:compileDebugKotlin --rerun`, then the same for `compileReleaseKotlin` — pass. 9 warnings in each variant, all in files this branch never touches: 6 `hiltViewModel` deprecations and 3 annotation-target warnings (KT-73255). None comes from this branch.
- Mutation runs, every mutant caught:
  - Board answer (7 mutants), haptic `> 0` guard, announcement (5 mutants).
  - Tutorial wiring: correction flag on, live region added, and a comment naming `wrong_tap_` (both a line-adding and a same-line edit).
  - WT-5 dead corner (only the new test catches it).
  - Clock guard (only `WrongTapFeedbackTest` catches it).
- Review greps over `ui/onboarding/`: `SettingsRepository`, `hintsEnabled`, `soundOn`, `performHapticFeedback`, `LocalHapticFeedback`, `HapticFeedbackType` and `wrong_tap_` all return 0. The §5.1 greps match develop (`GameViewModel` 3 = 3; the rest 0).
- Suppression audit of all lines added since `62c774d`: no `@Suppress`, `@Ignore`, `!!` or baseline. One `@OptIn(ExperimentalCoroutinesApi::class)` in `AndroidSystemAudioStateTest`, with its reason, as in 16 other test files.
- Invisible-character scan of every changed file and every commit message: clean.
- `git status --porcelain --untracked-files=all` at 8663710: empty.

## Deviations
1. **Commit trailer.** All commits carry `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, per the studio's gitflow rule, not the harness's attribution line.
2. **WT-19 is tested as its contract, not as Roborazzi golden images.**
   - Why: the repo has no screenshot setup, the tech-lead ruled on 2026-09-21 that there is "no golden-image suite in Phase 15" (`docs/ux-spec.md` §13), and Roborazzi isn't in the offline Gradle cache.
   - The mark is asserted as 12 dashes in `onSurfaceVariant`, 3 ± 0.4 px wide at a 40 ± 0.4 px radius, on a hole and on a peg.
   - The ring at the peak of its nudge is asserted as `primary` at 1.18 ± 0.02 of its radius.
   - Each in light and dark.
3. **The reject haptic and the announcement fire through `NewOrdinalEffect`, not §3.4's "`> 0` guard".** With the house guard, a rotation would buzz and speak again for a tap made before it. Screen tests catch the `> 0` version.
4. **The board's answer is decided in the tap's own frame, during composition.** Effects only end it (time out, or any successful action, which ends it for good). A guard keeps a new answer at t = 0 until its own effect restarts the clock. On a phone, without the guard, one frame would show the new cell at the phase an interrupted shake left behind. The test clock can't show that frame, so `WrongTapFeedbackTest` pins it directly.
5. **The mark's 450 ms hold uses a `delay`, not an animation clock.** At an animator scale of 0 an animated hold ends in its first frame, on exactly the phones the mark is for. The reduced-motion test class runs with that scale at 0.
6. **WT-10's "drawn at 440 ms, gone at 460 ms" is checked on the frame grid.** The tests check that the mark is drawn whole from the tap's frame through at least 440 ms and is gone by 450 ms plus one frame. At 60 Hz it disappears at about 467 ms, because ±10 ms is narrower than one 60 Hz frame. This needs a ux-designer decision; I propose no code change.
7. **How the announcement works (§3.6 left this to the developer).**
   - The board node itself is the polite live region; no new node is added.
   - Each wrong tap puts its sentence there for 1 s.
   - Every other tap appends an unspoken zero-width space (U+200B, written as an escape), so an identical repeat still changes the content and is spoken again.
   - During that one second the board can be reached by a swipe; after it, it can't (tested).
8. **Item 16's tests went into item 14's commit.** Item 16 changes no production code.
9. **Three existing `GameViewModelTest` specs were updated in 137884a.** Item 9's commit `09ce3eb` left them red, and I found that on a full run before the Check. They now expect the exact tapped cell as well as the ordinal, so they are stricter, not looser.
10. **Tests beyond the brief:**
    - `WrongTapFeedbackTest` (the clock guard).
    - `TutorialSourceGuardTest`, which turns the onboarding review greps into a test. `app/build.gradle.kts` declares the onboarding sources as a test input, so a comment-only edit reruns it.
    - The WT-5 dead-corner test. That case had no test before, and a version that skips the hit radius passed everything.
11. **`@param:ApplicationScope`** on `TutorialViewModel`'s constructor (item 8), so the new collaborator adds no compiler warning (KT-73255).
12. **Test harness details:**
    - Each test frame first lets the main thread finish pending work, so a state change made since the last frame shows up in the next one.
    - The control board sits 320 px below, a multiple of 16, so gradient dithering lines up.
    - The same drawing at those two positions differs by at most 20 (summed colour steps), measured over 160 frames, so pixels count as different only above 30.
13. **The new test classes use the house `createComposeRule`**, which is deprecated in favour of v2, as 31 other test files do. These warnings are in test code only.

## Open issues / risks
1. Existing behaviour, unchanged: the cue effects and the ad snackbar use `> 0` guards, so a rotation replays the last cue once.
2. The prose in `docs/audio/CREDITS.md` still describes the old credits test. That file belongs to the sound-designer.
3. Any audio output not on the allowlist counts as the phone speaker (for example `TYPE_HDMI_EARC` below API 33). By design this errs toward muting.
4. Robolectric tests that build the real `App` also start the real `SoundPoolPlayer` and its listeners. Harmless.
5. TalkBack behaviour is only verifiable on a device (WT-14). If TalkBack drops identical repeats anyway, the fallback is to clear the region for more than 100 ms before setting it again.
6. The WT-10 timing at 60 Hz (deviation 6).
7. The board still has no per-cell semantics; `feature/talkback-board` addresses this.
8. Precondition 2 (check-gates C7) has not merged; main code has no warning from this branch.
9. `docs/tech-design.md` still marks §16 #17 as "Open" until the tech-lead syncs it.

## Handoff
**Next: code-auditor.** Review `62c774d..8663710`. Points worth a close look:
- `ui/components/WrongTapFeedback.kt`
- `NewOrdinalEffect.kt`
- `WrongTapAnnouncement.kt`
- The two input declarations in `app/build.gradle.kts`
- The three updated `GameViewModelTest` specs (137884a)

`GameScreen`'s existing `LongMethod` suppression is unchanged. Its body only gains an effect, a state read and a modifier, no new surface.

**Next: qa-engineer.** On a physical phone, with wired and Bluetooth headphones:
- D1–D10 from `docs/tech/audio-silent-mode.md` §5.
- WT-12's below-API-30 half, if such a phone exists. Otherwise `WrongTapHapticPolicyTest` is the evidence and the producer decides.
- WT-14 with TalkBack:
  - Every wrong tap is spoken once, politely, including the second of two identical taps.
  - After about 1 s, swiping finds no new stop and doesn't re-read the sentence (within that second the board is reachable by design).
  - Nothing is spoken with TalkBack off.
- WT-15 with OS animations off.
- WT-18:
  - W1, W2 and W3 are identifiable in a 60 fps recording, at volume 0 and with Sound off, light and dark.
  - With animations off, the mark shows in a screenshot taken within 450 ms of the tap.
  - The mark's contrast is at least 3:1.
- WT-10 timing on a 60 Hz phone.
- The P4c listen, and the P9 recheck on develop.

**For the producer to route:**
- **feature/talkback-board (Q-023):** item 15's board-level live region is `Modifier.wrongTapAnnouncement` in `app/src/main/java/com/app/pegsolitaire/ui/game/WrongTapAnnouncement.kt`.
  - The modifier starts at line 57. It sets `liveRegion = LiveRegionMode.Polite` at line 59 and, while a sentence is held, the node's `contentDescription` at line 60.
  - It is fed by `rememberWrongTapAnnouncement` at line 38.
  - `GameScreen.kt` computes it at line 164 and applies it to the gameplay `BoardCanvas` modifier at line 237. So it sits on the same node as `BOARD_CANVAS_TEST_TAG` (`ui/components/BoardCanvas.kt`, line 225).
  - Integrate rather than duplicate: keep one polite live region. If the board gets a label of its own, move the sentence to a dedicated child node. Otherwise the sentence overwrites the label for a second, and the label breaks the repeat trick.
  - Keep `GameScreenWrongTapTest`'s announcement tests and `TutorialWrongTapTest.theTutorialBoardIsNoLiveRegionAndNoWrongTapIsAnnounced` green.
- **sound-designer:** the stale rows in `docs/audio-spec.md` that the brief lists, `CREDITS.md`'s prose, and the integration review against `rules/audio.md`.
- **ux-designer:** fold the amendment into the UX spec and renumber D-06; decide on WT-10 at 60 Hz; review the first-second reachability of the announcement.
- **tech-lead:** sign-off, the tech-design sync for §16 #17, and whether to switch WT-19 to golden images now.
