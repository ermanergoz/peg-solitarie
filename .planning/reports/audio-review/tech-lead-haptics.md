# tech-lead report — audio-review (the tutorial haptics ruling, tech design §16 #17)
Date: 2026-10-18
Branch: feature/audio-review (report only)
Skills loaded: ergoz-soft:standards (role: tech-lead; game; android-compose-game), android-architecture, android-gradle-logic. Rule files read in full from the plugin (1.0.0, because the product snapshot is 0.6.2): studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, stacks.md, testing.md, audio.md and dispatch.md; also the snapshot's lessons.md.
Ran in parallel with: phase-15 H-1 code audit, candy-backdrop spec rewrite, store-listing drafts (release-manager)
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort; the harness names none). This is a staffing deviation from the pinned fable; see below.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` exists neither on this branch nor on phase 15)

## Staffing deviation
`agents/tech-lead.md` pins `model: fable` at `effort: xhigh`. This ruling ran on Opus under the
founder's **Q-019** answer (`/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/questions/FOUNDER.md`,
answered 2026-10-17, option 3): Opus for every Fable-pinned role until 3.0.0 is published, with the
deviation recorded in every report. The same answer settles the silent-mode design's O5: the build's
reviewers run on Opus, as fresh agents that run the Check and their own mutation probes.

## Summary
- **How the tutorial reads Haptics:** through a new read-only seam, `HapticsSetting`, a single `Flow<Boolean>` that `SettingsRepository` now extends. It becomes `TutorialViewModel`'s third collaborator. The pin test names the three types, and a second test pins the seam to that one member. The value rides on the existing `board.hapticsOn` and is carried through the tutorial's only step-install site.
- **The jump haptic** is gameplay's `JumpHaptic`, lifted to `ui/game/JumpHapticEffect.kt` and called from both screens. It therefore honours Haptics exactly as in gameplay. **The wrong-tap reject buzz never fires in the tutorial** (D-01j(2)), whatever Haptics says.
- **Verified in the cached library sources:** below API 30, androidx.core turns `Reject` into `LONG_PRESS`. The amendment's own `SDK_INT >= 30` check is therefore load-bearing. It is tested as a pure predicate, because Robolectric here runs SDK 34 only.
- **For the producer:** the whole android-game-dev scope for one audio brief, 17 items with sources, is listed below.

## The question, and what was read
Tech design §16 #17 (`feature/phase-14-release-docs`, 866dffb) leaves one item open and owed by the
tech-lead: "the tutorial reading `hapticsOn` without breaking `TutorialViewModel`'s two-collaborator
pin". It gates only the wrong-tap half of this branch (§14.1, audio row; candy R6 condition 5;
`15-founder-feedback/tech-lead-signoff.md` Decision 4 row 5). The question comes from the
ux-designer: `docs/ux/invalid-tap-feedback.md` §4 ("Open for the tech-lead") and
`.planning/reports/audio-review/ux-designer.md` open issue 3 and handoff.

Read for it:
- the tutorial on `feature/phase-15-ux-refresh` at ee16a2a: `ui/onboarding/TutorialViewModel.kt`, `TutorialUiState.kt`, `TutorialScreen.kt`, `TutorialRoute.kt` and the four tutorial test files;
- the gameplay haptic path: `GameScreen.kt:164` and `:286-314`, `GameViewModel.kt:534-540`, `GameUiState.hapticsOn`, `GameFlowTest.kt:204-231` and `:382`;
- `audio/JumpSoundEffect.kt`, `data/SettingsRepository.kt` and `di/DataModule.kt:52-54`;
- the phase-15 spec's D-01c, D-01g, D-01i, §5 and §5.1;
- this branch's FOUNDER.md (Q-008 to Q-012), the amendment, `docs/tech/audio-silent-mode.md`, `docs/audio-spec.md` and the five audio-review reports.

Paths below are under `app/src/main/java/com/app/pegsolitaire/` (code) or
`app/src/test/java/com/app/pegsolitaire/` (tests). Line numbers are phase 15 at ee16a2a; where they
move, the names govern.

## Ruling 1 — how the tutorial reads Haptics (tech-lead decision)

**Decision.**
1. A new interface `data/HapticsSetting.kt` declares exactly one member, `val hapticsOn: Flow<Boolean>`.
2. `SettingsRepository` extends it. Its own `hapticsOn` declaration becomes `override`, with its KDoc kept. No member is added or removed, so `SettingsRepositoryImpl` and every existing test fake compile unchanged.
3. Hilt binds the seam to the same repository, `@Binds abstract fun bindHapticsSetting(settings: SettingsRepository): HapticsSetting`, beside `bindSettingsRepository` (mirror its `@Singleton`). Binding from the interface, not from the impl, means anything that replaces `SettingsRepository` replaces this too: one source of truth for the gate.
4. `TutorialViewModel`'s constructor becomes `(onboardingRepository: OnboardingRepository, hapticsSetting: HapticsSetting, @ApplicationScope applicationScope: CoroutineScope)`. In `init`, on `viewModelScope`, it collects `hapticsOn` into `board.hapticsOn`, exactly as `GameViewModel` does (`:534-540`). Like gameplay, it adds no `catch` of its own; the impl's `catch` is the fail-open (D-17).
5. The value lives on **`board.hapticsOn`**, the field `GameUiState` already has, and not on a new `TutorialUiState` field. The screen then calls the shared effect with the same arguments gameplay passes, and `TutorialUiState` (whose fields a reflection test scans for forbidden words) gains nothing.
6. **The trap this closes.** `advanceAfterABeat` installs each step with `TutorialUiState.forStep(nextIndex)` (`TutorialViewModel.kt:250`), which rebuilds the board from `GameUiState.initial()` and so resets `hapticsOn` to `true`. The DataStore flow does not re-emit an unchanged value, so a player with Haptics off would feel T2's and T3's jumps for the rest of the visit. Therefore:
   - `forStep(index: Int, hapticsOn: Boolean)` takes the value as a **required** parameter, with no default, so every install site has to decide;
   - `initial()` passes a named fail-open constant (`true`, D-17, the value `GameUiState.initial()` starts with);
   - the advance reads the value **inside** the update: `_uiState.update { TutorialUiState.forStep(nextIndex, hapticsOn = it.board.hapticsOn) }`. Never pass a value captured outside the lambda, so a collector emission between the two cannot be lost.
7. **The pin is updated, not removed.** `theTutorialViewModelsOnlyCollaboratorsAre…` asserts exactly `OnboardingRepository`, `HapticsSetting` and `CoroutineScope`. A new test pins `HapticsSetting` to its single getter and no supertype. Together they keep the pin's purpose: any progress dependency, or any growth of the seam, turns a test red.

**Where the shared effect lives: `ui/game/`, not `audio/`.** The amendment says "beside
`JumpSoundEffect`", meaning the same lift. The package is a separate choice. `audio/` is the
playback owner that rules/audio.md names, and a haptic is not audio. The tutorial already imports
`ui.game.MoveTransition` and `ui.game.GameUiState`, and §5.1 forbids `GameScreen` and
`GameViewModel`, not the package.

**Alternatives considered and rejected.**

| Option | Verdict | Why |
|---|---|---|
| Inject the whole `SettingsRepository` and widen the pin to it | Rejected | It hands the tutorial `hintsEnabled`, which D-01b overrides; `soundOn`, which only the player may read (`JumpSoundEffect.kt:38-45`: a second reader is "two disagreeing sources of truth"); and three setters. The pin would then permit a type that can do more than the tutorial may, and only a grep would guard the rest |
| Read it at the route or screen: a root `CompositionLocal`, or `AppNavHost` collecting settings | Rejected | It would be a second path for the same gate, different from gameplay's, which carries it in UI state. Composables would read a repository-backed value outside a ViewModel (rules/compose.md; rules/kotlin-android.md: one `StateFlow<UiState>` per ViewModel) |
| A haptics "player" that gates itself, like `SoundPoolPlayer` | Rejected | It rewrites gameplay's working, tested path (`GameFlowTest`). The audio gate sits in the player only because in-flight streams must be stopped; a haptic is blocked before it fires (`JumpSoundEffect.kt:38-41`) |
| A second ViewModel for the tutorial route | Rejected | Two state holders for one screen |
| Drop the pin | Rejected | It is §5.1 prohibitions 2 and 3 as a test (`TutorialViewModelTest.kt:416-425`) |

**§5.1 still holds, all four prohibitions.** `HapticsSetting` can read one Boolean and nothing else.
It cannot write a `LevelResult`, clear a snapshot, advance the ladder or navigate. The tutorial
still hosts `BoardCanvas` (1), runs `TutorialViewModel` (2), writes nothing but the seen flag (3)
and leaves only by `popBackStack()` (4).

## Ruling 2 — the jump haptic honours Haptics in the tutorial exactly as in gameplay

| Property | Gameplay today | Tutorial after this ruling |
|---|---|---|
| Effect | `JumpHaptic`, private (`GameScreen.kt:303-314`) | the same body, lifted to `internal fun JumpHapticEffect(animatingMoveId: Int?, hapticsOn: Boolean)` and called from both screens. Re-implementing it is a defect (D-01j) |
| Key | `state.animating?.id` | `state.board.animating?.id`. `moveOrdinal` is never reset between steps, so ids do not recycle |
| Type | `HapticFeedbackType.ContextClick` (`CONTEXT_CLICK`, API 23, no fallback at any level) | the same |
| Gate | persisted `hapticsOn`, fail-open `true` until the first emission | the same flow, through the seam |
| System touch-feedback setting | honoured (no ignore flag; `ViewCompat.performHapticFeedback(view, constant)`) | the same |
| Sound switch, ringer, speaker gate, background gate | none apply (silent-mode design §1.7: "On Vibrate with the speaker, the jump still buzzes") | none apply |
| OS animations off | the snap path sets and clears `animating` quickly; the keyed effect still sees the id, as the jump cue does | the same race. It gets a JVM test on both call sites, and WT-15 keeps its device pass |

## Ruling 3 — the wrong-tap buzz never fires in the tutorial
D-01j(2) gives tutorial wrong taps the redirect and nothing else: "no shake, no mark, no haptic, no
sound, no announcement". This is the ux-designer's ruling, not mine, and I find nothing technical
against it.
- **Q-012 does not reach the tutorial.** Its context describes the gameplay board: the shake, the soft tok, and a buzz "the Haptics switch still turns off". The tutorial has none of the first two.
- **The code enforces it by where the effect lives.** The reject effect is private to `GameScreen.kt`, beside `InvalidTapSound`, so the tutorial cannot call it. The Haptics value the tutorial collects gates the jump only.
- **If the producer reads Q-012 otherwise,** it is a question for the ux-designer. This shape makes either answer a one-call change.

## Ruling 4 — the gameplay reject haptic: amendment §3.4 confirmed, with one finding
- The pinned Compose BOM 2026.06.00 resolves `androidx.compose.ui:ui-android` **1.11.3**. That version's `PlatformHapticFeedback.android.kt` maps `HapticFeedbackType.Reject` to `HapticFeedbackConstantsCompat.REJECT` and performs it with `ViewCompat.performHapticFeedback(view, constant)`, with no flags.
- androidx.core **1.18.0**, the forced pin, applies `getFeedbackConstantOrFallback`. Below API 30 it rewrites `REJECT` to **`LONG_PRESS`** (`HapticFeedbackConstantsCompat.java:343-347`).
- So the amendment's own `Build.VERSION.SDK_INT >= 30` check is **load-bearing**, not belt and braces. Without it, every API 24–29 phone gets the heavy `LongPress` that §3.4 forbids.
- **How it is tested.** Robolectric here runs SDK 34 only (the silent-mode design §1.10's offline rule), so the below-30 branch cannot be a Robolectric test. The gate becomes a pure function in `ui/game/`, `internal fun wrongTapHapticFires(hapticsOn: Boolean, sdkInt: Int): Boolean`, comparing against `Build.VERSION_CODES.R`. The private effect calls it with `Build.VERSION.SDK_INT`. Do **not** add `@Config(sdk = [29])`.
- **Gates.** Both haptics read the same `hapticsOn` and nothing else. On Vibrate with the speaker, the wrong tap still buzzes and still shakes; only its sound is muted (silent-mode §1.7).

## File-level shape (this ruling's part of the wrong-tap half)

| File | Change |
|---|---|
| `data/HapticsSetting.kt` | **new.** `interface HapticsSetting { val hapticsOn: Flow<Boolean> }`, with KDoc saying why it exists: a read-only seam for a screen that may read this one setting and nothing else, the tutorial pin |
| `data/SettingsRepository.kt` | `interface SettingsRepository : HapticsSetting`; `override val hapticsOn`, KDoc kept. Nothing else |
| `di/DataModule.kt` | one `@Binds` directly under `bindSettingsRepository` (Ruling 1, step 3) |
| `ui/game/JumpHapticEffect.kt` | **new.** `GameScreen.kt:286-314`'s body and KDoc, moved verbatim, plus one paragraph "shared by the gameplay and tutorial screens (D-01j)", modelled on `JumpSoundEffect`'s |
| `ui/game/GameScreen.kt` | the private `JumpHaptic` is deleted; `:164` calls `JumpHapticEffect(animatingMoveId = state.animating?.id, hapticsOn = state.hapticsOn)`. The reject effect (amendment §3.4) is added here as a private effect calling `wrongTapHapticFires` |
| `ui/game/` (the developer picks the file) | `wrongTapHapticFires(hapticsOn, sdkInt)`, pure (Ruling 4) |
| `audio/JumpSoundEffect.kt` | KDoc only: the two `JumpHaptic` mentions (`:14`, `:38`) name `JumpHapticEffect` |
| `ui/onboarding/TutorialViewModel.kt` | the constructor, the collector and the in-lambda carry (Ruling 1, steps 4 and 6). The KDoc's "exactly **two** collaborators" (`:38-40`) becomes three, with the reason the third is allowed: read-only, one setting, not a progress dependency |
| `ui/onboarding/TutorialUiState.kt` | `forStep(index, hapticsOn)` with a required parameter; `initial()` passes the named fail-open constant. **No new field** |
| `ui/onboarding/TutorialScreen.kt` | one line beside `JumpSoundEffect` (`:112`): `JumpHapticEffect(animatingMoveId = state.board.animating?.id, hapticsOn = state.board.hapticsOn)`. The KDoc's "Audio (D-01i)" paragraph gains one sentence on the jump haptic |
| `ui/onboarding/TutorialRoute.kt` | unchanged |

Size: about 60 production lines in nine files, and no new dependency, permission or manifest entry.
The Hilt graph is checked at compile time, so the developer's first green Check proves the binding;
the project has no Hilt test harness.

**Overlap with `feature/candy-backdrop`.** Candy R6 counts four shared files; this ruling adds a
fifth.
- `TutorialScreen.kt` gains one line at `:112`, outside the root `Surface`'s two colour arguments that candy edits (`:115-116`).
- `GameScreen.kt`'s change is outside candy's `Scaffold` colour arguments.
- **`di/DataModule.kt` is the fifth shared file:** candy binds its licence source there (candy report line 287). Each branch adds its own `@Binds` block, so a conflict is a mechanical "keep both", under R6 condition 2's rule.

## Tests the developer writes first (failing, then green)
All are deterministic: fakes, no wall clock, no Robolectric SDK other than 34.

`ui/onboarding/TutorialViewModelTest.kt` (JVM, `runTest`):
1. `theTutorialViewModelsOnlyCollaboratorsAreTheOnboardingRepositoryTheHapticsSettingAndTheApplicationScope`, renamed from today's pin: `containsExactly(OnboardingRepository, HapticsSetting, CoroutineScope)`.
2. `theHapticsSettingSeamExposesOnlyTheHapticsFlow`: `HapticsSetting::class.java.declaredMethods` names exactly `getHapticsOn`, and `.interfaces` is empty.
3. `theTutorialBoardCarriesThePersistedHapticsSetting`: a fake emitting `false` gives `board.hapticsOn == false`; a fake emitting `true` gives `true`.
4. `hapticsAreOnBeforeTheSettingIsRead`: with a never-emitting fake, `board.hapticsOn` is `true` (D-17, as gameplay).
5. `aStepAdvanceKeepsThePersistedHapticsSetting`: with the fake at `false`, play T1, settle the beat, and assert step 2 has `board.hapticsOn == false`; repeat into T3. **This is the test a naive `forStep` fails.**
6. `showHintsIsForcedOnRegardlessOfThePersistedSetting`: kept. Its comment (`:205-206`, "There is no settings collaborator") now says the only settings collaborator cannot read hints.

New `ui/onboarding/TutorialHapticsTest.kt` (Robolectric SDK 34; the real `TutorialScreen` over the
real `TutorialViewModel`; a recording `HapticFeedback` through `LocalHapticFeedback`):

7. `aCompletedTutorialJumpFiresExactlyOneContextClickWhileHapticsAreOn`: a selection fires nothing, and the jump fires exactly `[ContextClick]` (WT-15, unit half).
8. `noHapticFiresOnATutorialJumpWhileHapticsAreOff`.
9. `aCompletedTutorialJumpStillFiresTheHapticWhenTheOsAnimationScaleIsZero`: the 7c race on the JVM, set up as `TutorialSoundTest.disableOsAnimations()` does.
10. `noWrongTapInTheTutorialFiresAnyHapticWhileHapticsAreOn`: T-W1, T-W2 and T-W3 record no event at all (WT-16's haptic half, as a positive assertion).

Gameplay (the lift's regression guard and the shared call site's other half):

11. `GameFlowTest.aJumpFiresExactlyOneLightHapticWhileHapticsAreOn` and `noHapticFiresOnAJumpWhileHapticsAreOff` pass **unchanged** after the lift.
12. New `GameFlowTest.aJumpStillFiresTheHapticWhenTheOsAnimationScaleIsZero`. The jump-sound tests already hold that a guard on one call site of a shared effect says nothing about the other.
13. New `WrongTapHapticPolicyTest` (pure JVM, table-driven): (on, 30) fires; (on, 29), (off, 30) and (off, 29) do not; the boundary is `Build.VERSION_CODES.R`.
14. The amendment's WT-12 UI test at SDK 34: exactly one `Reject` per wrong tap, none on a select or a put-down, none with Haptics off, and never `ContextClick` on a wrong tap.

**Shared doubles, not new copies.**
- `RecordingHapticFeedback` is private to `GameFlowTest` (`:382`). Move it to one `internal` test file that `GameFlowTest`, `TutorialHapticsTest` and the WT-12 test share, rather than adding a third copy (R-8's direction).
- Add `FixedHapticsSetting(on: Boolean)` and a never-emitting `HapticsSetting` to `TutorialTestHarness.kt`.
- Update the four existing `TutorialViewModel(...)` constructions (`TutorialViewModelTest.kt:134`, `:450`; `TutorialSoundTest.kt:170`; `TutorialScreenTest.kt:227`).

## What the audio integration brief must add
1. **Order inside the wrong-tap half:** this ruling's seam and lift first (tests 1–12), because WT-15 and the tutorial redirect build on them. Then the wrong-tap signal, the canvas visuals, the reject haptic, the announcement and the tutorial redirect.
2. **One install site, two carries.** `TutorialUiState.forStep` is the tutorial's only board install. It carries `hapticsOn` (this ruling) and also the wrong-tap ordinal, under the house tick rule the amendment applies to the game's four install sites (§3.7). Each carry gets its own test. The tutorial's wrong-tap signal rides on `board`, never on a new `TutorialUiState` field: that class's reflection test rejects names containing `invalid`, `error`, `retry`, `score` or `fail`.
3. **`SDK_INT >= 30` is load-bearing** (Ruling 4), and the pure-predicate test stands in for any Robolectric SDK below 34.
4. **Review greps over `ui/onboarding/`**, each expected to find 0 matches:
   - `SettingsRepository`, `hintsEnabled`, `soundOn`: the tutorial reads only the seam;
   - `performHapticFeedback`, `LocalHapticFeedback`, `HapticFeedbackType`: the tutorial references only `JumpHapticEffect`;
   - `wrong_tap_`: the amendment's §9 grep.

   The §5.1 greps (`GameScreen`, `GameViewModel`, `ProgressRepository`, `recordResult`, `clearSnapshot`, `advancePast`) are unchanged.
5. **The shared doubles** (tests section above).
6. **Spec text the developer follows where the amendment and this report differ:** the effect's home is `ui/game/JumpHapticEffect.kt`, not beside `JumpSoundEffect` in `audio/`. The ux-designer folds that in.

## The android-game-dev's full audio scope, for one brief after phase 15 merges

**Preconditions.**
1. Phase 15 merges to `develop` (after H-1 to H-4), then `develop` is merged into `feature/audio-review`. The FOUNDER.md conflict is the producer's to resolve (silent-mode design §3, point 4).
2. If check-gates C7 (Kotlin warnings as errors in `:app`'s main compilations) merges first, a new main-code warning on this branch is fixed, never suppressed (tech design §9.8, rule 3). The `InlinedApi` lint warnings (O8) stay warnings; lint is the held half.
3. No Gradle build runs during candy-backdrop's 15e sitting (candy R6, condition 4). The D-checks need a physical phone, not the bench AVD.
4. Reviews run on Opus under Q-019, as fresh reviewers (see Staffing deviation).

**Scope.** Section numbers in the "Source" column refer to the file named in the half's heading.

*Plan A — the silent-mode half* (`/Users/ermanergoz/Desktop/PegSolitarie-audio/docs/tech/audio-silent-mode.md`):

| # | Item | Source | Notes |
|---|---|---|---|
| 1 | **P5:** `AudioCreditsTest` rewritten to the CREDITS.md contract (the `## Files` table read by header; sourced vs in-house rows; exact matches; orphan recipes; sha256), plus the new `AudioAssetFormatTest` | §2.3 | first: the branch is red until it lands (`AudioCreditsTest.kt:35,44`) |
| 2 | **KDoc sweep:** `.ogg` → `.wav` at `SoundCue.kt:44` and `SoundParams.kt:137,155`; `VOLUME_DESELECT`'s RMS figure taken from CREDITS.md (0.73 dB, not 1.92) | §2.3, last paragraph; `sound-designer-assets.md`, Open issues | afterwards `grep -rn '\.ogg' app/src` finds nothing; no re-measuring |
| 3 | **P4a:** `VOLUME_JUMP` 0.9 → 1.0; KDoc for `VOLUME_JUMP`, `VOLUME_INVALID` and `VOLUME_WIN`; `theWinStingerIsTheLoudestCue` renamed to `noCueIsMixedAboveTheWinStinger` | §2.2 | lands before the branch merges, never apart from d1c6e9b's asset |
| 4 | **`MAX_STREAMS` KDoc nit** | §2.4 | comment only; the replacement sentence is given there |
| 5 | **Silent/Vibrate speaker gate (P3, Q-010 option 2):** `SystemAudioState`, `AudioRouteClassifier`, `GameAudioAttributes`, `AndroidSystemAudioState`, the three gates and collectors in `SoundPoolPlayer`, the `AudioModule` bindings, the `standards.gradle.kts:258` failure text and the KDoc ("one mute gate" becomes three) | §1.1–§1.10; §5 "Must not" | tests in §1.10; device checks D1–D6, D8, D9 |
| 6 | **P8, background stop:** `AppVisibility` / `ProcessAppVisibility`, its collector, three player tests and `ProcessAppVisibilityTest` | §2.1 | reads `ProcessLifecycleOwner`, **not** `AppForegroundTracker` (this supersedes the last sentence of `docs/audio-spec.md` P8); device D7 |
| 7 | **V1, volume keys:** `volumeControlStream = STREAM_MUSIC` in `MainActivity.onCreate`, with one KDoc line | §2.5 | device D10 only; no unit test |

*Plan B — the wrong-tap half* (`/Users/ermanergoz/Desktop/PegSolitarie-audio/docs/ux/invalid-tap-feedback.md`, with this report):

| # | Item | Source | Notes |
|---|---|---|---|
| 8 | **The tutorial's Haptics read and the shared jump haptic** | this report, Rulings 1–2 and the file table; amendment §4, D-01j(1) | first in this half; tests 1–12; WT-15 |
| 9 | **The wrong-tap signal** (ordinal + `Position`) in the UI state, carried at the game's four install sites and at the tutorial's `forStep` | §3.7; this report's brief item 2 | WT-1 to WT-6 |
| 10 | **The shake:** keyframes, amplitude, tokens | §3.1, §5 | WT-7, as a pure offset function |
| 11 | **The redirect:** ring nudge and dot re-pop rules (W2, W3 × Show-moves × destinations) | §3.1 | WT-8, WT-9 |
| 12 | **The reduced-motion dashed mark** | §3.2 (D-06a) | WT-10; WT-19 (Roborazzi goldens) |
| 13 | **Interruption rules; the read-only canvas draws none; the canvas negative contract** (a correction flag beside `interactive`) | §3.3, §3.8 | WT-11, WT-17 |
| 14 | **The reject haptic:** `hapticsOn` and `SDK_INT >= 30` in our own code; never `ContextClick` or `LongPress` | §3.4 (D-06b; Q-012 "yes, buzz"); this report, Ruling 4 | WT-12 and test 13; it still buzzes on Vibrate |
| 15 | **The screen-reader announcement:** polite, every repeat spoken, no focus stop; three strings | §3.6, §6 (D-06c) | WT-14 (device, TalkBack) |
| 16 | **Sound unchanged;** the visual and the haptic do not depend on Sound | §3.5 | WT-13 |
| 17 | **Tutorial wrong taps:** the redirect only, no correction channel | §4 (D-01j(2)); this report, Ruling 3 | WT-16; test 10 |

**Order.** One brief, two plans, serial in the one worktree.
- Plan A first, in the design's §5 order: 1–2, 3–4, 5, 6, 7. Item 1 turns the branch green.
- Then Plan B: 8, 9, 10–13, 14, 15, 16–17.
- The two plans touch disjoint files. Plan B's only edit under `audio/` is `JumpSoundEffect.kt`'s KDoc, which Plan A does not touch. The Check runs after each plan.

**Conditional, not in the first brief.** P4c: change `DESELECT_ATTENUATION` from 10 dB to 6 dB
**only if** the qa-engineer's phone listen finds the put-down lost (`docs/audio-spec.md`, P4(c)).

**Not the developer's work** (for the producer's other dispatches):
- **ux-designer:** fold the amendment into `docs/ux-spec.md` (amendment §11). Renumber D-06, because the phase-15 spec already cites a "D-06" for the restart confirm dialog (`docs/ux-spec.md:945` on phase 15). Record the effect's home (`ui/game/`). Silent-mode O4 is still open (the Settings line while the ringer mutes the speaker). Review the UI diff and QA's captures.
- **sound-designer:** after the build, update the `docs/audio-spec.md` rows that go stale: `:33` (InvalidTap's Haptic and Visual columns), `:57` (tutorial jump, "no haptic here"), `:163-165` (silent mode), `:166-167` (background), `:168-169` ("the only haptic in the game is on the jump") and `:355-356` (P8 via `AppForegroundTracker`). Then review the integration against rules/audio.md.
- **qa-engineer:**
  - D1–D10 on a physical phone with wired and Bluetooth headphones (O3);
  - WT-12's API < 30 half, if such a device exists, otherwise test 13 is the evidence and the producer decides, as for O3;
  - WT-14 with TalkBack; WT-15 with OS animations off; WT-18;
  - the P4c listen, and P9's removal-layer recheck on `develop`.
- **research-analyst and release-manager:** provenance (P6), and the release shrink re-run on the WAV set (`sound-designer-assets.md`).
- **tech-lead:** the tech-design sync below.

## Text for `docs/tech-design.md` (for the next tech-design sync)
That file lives only on `feature/phase-14-release-docs`, outside this dispatch, so this step does not
edit it. §16 #17 hands the item to the tech-lead, and this report is the ruling of record until the
file is synced. The text to transcribe:
- **§16 #17, Status:** "**Decided 2026-10-18** (`.planning/reports/audio-review/tech-lead-haptics.md`, `feature/audio-review`). A read-only `HapticsSetting` seam (`data/`, extended by `SettingsRepository`, bound to it) is `TutorialViewModel`'s third collaborator. The pin names the three types, and a second test pins the seam to its one member. The value rides on `board.hapticsOn` and is carried through `forStep`. The jump haptic is gameplay's, lifted to `ui/game/JumpHapticEffect.kt`; the tutorial never fires the reject haptic (D-01j). Owner: the android-game-dev, in the audio brief."
- **§12, a new "Haptics" paragraph:** "Two haptics, both gated by the persisted `hapticsOn` and by nothing else: not Sound, not the ringer, not the background gate. The jump's `ContextClick` fires on gameplay and in the tutorial, through one shared effect. The wrong tap's `Reject` fires on gameplay only, and only on API 30+ by our own check, because androidx.core 1.18.0 falls back to `LONG_PRESS` below 30. The tutorial reads the gate through `HapticsSetting`."
- **§14.1, audio row:** delete "The wrong-tap half also waits on the tech-lead's haptics ruling (§16 #17)".
- **§14.1, candy row:** "they share four files" becomes five, adding `di/DataModule.kt`.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/tech-lead-haptics.md` — this report: Rulings 1–4, the file-level shape, the tests to write first, the brief additions, the producer's 17-item scope with sources, and the tech-design sync text.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-p15 log --oneline -1` — ee16a2a, the phase-15 tree every file reference above was read at.
- `grep -rni 'haptic' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` on phase 15 — 0 hits: the tutorial has no haptic today.
- `grep -rn 'TutorialViewModel(' app/src/test` on phase 15 — 4 constructions (`TutorialViewModelTest.kt:134`, `:450`; `TutorialSoundTest.kt:170`; `TutorialScreenTest.kt:227`).
- `grep -rn 'JumpHaptic' app/src docs` on phase 15 — code references at `GameScreen.kt:164`, `:304` and `JumpSoundEffect.kt:14`, `:38`, plus one test comment (`GameScreenSoundTest.kt:278`).
- `grep -rln 'ArchRule\|archunit' app/src/test` and a check of their package rules — none constrains `data/` or `ui/onboarding/`, so the new seam trips no boundary test.
- `strings ~/.gradle/caches/modules-2/metadata-2.107/descriptors/androidx.compose/compose-bom/2026.06.00/*/descriptor.bin | grep -A2 ui-android` — the BOM pins `ui-android` 1.11.3.
- `unzip` of `ui-android-1.11.3-sources.jar` (`PlatformHapticFeedback.android.kt`) and `core-1.18.0-sources.jar` (`HapticFeedbackConstantsCompat.java`, `ViewCompat.java`) into the session scratchpad, then `grep` — `Reject` maps to `REJECT`; below API 30 it falls back to `LONG_PRESS` (`:343-347`); `ContextClick` has no fallback; `performHapticFeedback(view, constant)` passes no flags.
- `grep -n 'DataModule' <candy>/.planning/reports/candy-backdrop/tech-lead-architecture.md` — candy binds in `di/DataModule.kt` (line 287), the fifth shared file.
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-audio diff --cached --check -- .planning/reports/audio-review/tech-lead-haptics.md` — pass (exit 0, no whitespace errors).
- `./gradlew check` — **not run.** The dispatch forbids a Gradle build while a code-auditor holds the host. This change is documentation only. The branch's last recorded Check is red for the known `AudioCreditsTest` reason (scope item 1).

## Open issues / risks
1. **Not build-verified.** The interface-to-interface `@Binds`, the `override` on `SettingsRepository` and the constructor change follow Dagger's and Kotlin's documented rules, but no compile ran here. The developer's first Check proves them. If Hilt rejects the alias, bind from `SettingsRepositoryImpl` instead; it is the same singleton, and that change needs no new ruling.
2. **Line numbers are phase 15 at ee16a2a.** H-1 follow-ups or the merge may move them; the names govern.
3. **Q-012's reach.** I read it as the gameplay board (Ruling 3). If the producer reads it otherwise, it is a question for the ux-designer, not the founder, and the code change is one call.
4. **D-06 id collision** at the amendment's fold-in (phase-15 `docs/ux-spec.md:945`). It is the ux-designer's to renumber.
5. **WT-12's below-30 device half** may have no device. Test 13 is the JVM evidence, and the producer decides, as for O3.
6. **The tech design still reads "Open" for §16 #17** until it is synced (text above). Until then, this report governs that item.
7. **The candy overlap grows to five files** with `di/DataModule.kt`. A conflict is mechanical (keep both `@Binds` blocks); tell both developers.

## Handoff
Next: producer — once phase 15 has merged and `develop` is merged into `feature/audio-review`, dispatch the android-game-dev **once**, with the scope tables above (Plan A, then Plan B) and three files: `docs/tech/audio-silent-mode.md`, `docs/ux/invalid-tap-feedback.md` and this report. Then dispatch the code-auditor, the qa-engineer (physical phone), the sound-designer's review and the ux-designer's review, then the tech-lead's sign-off. Schedule the tech-design sync (text above) when the release-docs worktree is free. Tell the candy-backdrop developer about the fifth shared file.
Next: android-game-dev — build Plan A in the design's §5 order, then Plan B starting with this report's seam and lift. Write tests 1–14 first. Follow the file table and the brief additions.
Next: ux-designer — at the fold-in: renumber D-06, record `ui/game/JumpHapticEffect.kt` as the shared effect's home, and close or keep O4.
Next: sound-designer — after the build, update the stale `docs/audio-spec.md` rows listed above.
