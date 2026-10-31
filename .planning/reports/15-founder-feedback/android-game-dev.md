# android-game-dev report — 15-founder-feedback (slice S11: D-03l, D-04e, D-05d)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (0a644d8..7493b10, plus this report's commit; worktree `/Users/ermanergoz/Desktop/PegSolitarie-p15`)
Skills loaded: ergoz-soft:standards (role: android-game-dev; rule files read from the plugin, 0.6.2, because the snapshot is 0.5.1: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing), superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran on: opus at effort max (agent file `model: opus`, `effort: max`)
Fan-out: none
Ran in parallel with: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
All three founder-feedback decisions are built against `docs/ux-spec.md` §4.2.4, §4.3.1 and §4.4.1, test-first:
- **D-03l.** Home's hero is one row (start-aligned title and gear) on every window, and the peg-cross logo is withdrawn. The upper slack is now a static clay board stage of a real mid-game English-33 position, sized by the slack and not drawn below 48dp. The ribbon sits on the Continue chips.
- **D-05d.** The strip uses `titleLarge`/`headlineSmall` in a 40dp-minimum clay pill, and each system bar is applied once.
- **D-04e.** The dock has five looks from one recipe. Show-moves is a `Role.Switch` named "Show possible moves", `show_moves_off` is deleted, and a spent allowance changes nothing.

Criteria 15n–15r, 18a–18c and 18e, and 21a–21e have tests; every new test was watched fail on the missing behaviour or on a mutation. **No `@Suppress` was added.**

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/game/DockLook.kt` — the five looks, `DockLookRecipe` (fill, ring, shadow, glyph shape, on mark, plus glyph strength and press response), `DockLookKey` (replaces `DockActionFilledKey`), `Modifier.dockLook`.
- `.../ui/game/DockMetrics.kt` — `UNAVAILABLE_GLYPH_ALPHA` (0.38) and the on mark's 45° rim angle.
- `.../ui/game/DockOnMark.kt` — the on mark. It is drawn in the draw phase outside the disc's clip and is never a node. It scales in with `overshootSpring`, out with a tween, and not at all with animations off.
- `.../ui/game/GameActionDock.kt` — `DockActionChip(look)` draws fill, ring, shadow, mark and squish from the recipe, and cross-fades over `PRESS_IN_MS`, instantly into Working. The dock no longer applies `navigationBarsPadding()`. It also hosts the `DockPreviewSlab` preview helper.
- `.../ui/game/{UndoControl,HintControl,RestartControl}.kt` — the actions in their looks. `enabled`, descriptions and the thinking branch are unchanged. Each has light and dark previews.
- `.../ui/game/HintToggleControl.kt` — Show-moves as a `toggleable(role = Role.Switch)` with a constant name on its own node. The glyph cross-fades between `ShowMovesIcon` and `ShowMovesOffIcon`. Light and dark previews.
- `.../ui/game/ShowMovesIcon.kt` — adds `ShowMovesOffIcon`: path strings, sharing the circles' path, with the glyph knocked out one stroke-width either side of the slash through a clip group. No suppression.
- `.../ui/game/GameStatusStrip.kt` — `titleLarge` title, `headlineSmall` tabular timer in a `heightIn(min = 40dp)` pill with 12dp sides and the back chip's shadow, `statusBarsPadding()` removed, `GAME_TIMER_PILL_TEST_TAG`, light and dark previews.
- `.../ui/game/GameScreen.kt` — `GAME_BOARD_SLAB_TEST_TAG` on the board slab, and a KDoc note that the `Scaffold` applies each bar once.
- `.../ui/game/GameScreenPreviews.kt` — light and dark previews of `GameScreen`. They live in their own file because `GameScreen.kt` is at detekt's 11-function limit.
- `.../ui/home/HomeHero.kt` — the one-row hero (`HOME_HERO_TEST_TAG`) replaces `FullHero`/`CompactHero`. Light and dark previews.
- `.../ui/home/HomeStage.kt` — `HomeStage` (weighted `BoxWithConstraints`, art edge `min(height, width, 320dp)`, composed only at ≥ 48dp), `HomeStageArt` (surfaceContainerHigh, extraLarge, card shadow, read-only `BoardCanvas`, 0.06 inset), `HomeStageMetrics`, the constant `HomeStageBoardState` (14 pegs), and light and dark previews.
- `.../ui/home/HomeBoardArt.kt` — `HomeHeroArt`, `HOME_HERO_ART_TEST_TAG` and the peg cluster are deleted.
- `.../ui/home/HomeScreen.kt` — the stage takes the upper weight beneath the hero (`STAGE_WEIGHT`, renamed from `SLACK_ABOVE_CHIPS_WEIGHT`, value unchanged). The spacer between the ribbon and the chips is removed. KDoc updated.
- `.../ui/home/HomeBandSet.kt` — KDoc: the compact set now switches only spacing and the pill.
- `app/src/main/res/values/dimens.xml`:
  - Added: `home_hero_row_min_height` 72, `home_stage_art_min_size` 48, `home_stage_art_max_size` 320, `game_timer_pill_padding` 12, `game_action_mark_size` 24, `game_action_mark_glyph_size` 16, `game_action_ring_width` 2.
  - Changed: `game_timer_pill_height` 32 → 40, now a minimum.
  - Retired: `home_hero_min_height`, `home_hero_art_size`, `home_hero_compact_min_height`.
- `app/src/main/res/values/strings.xml` — `show_moves_off` deleted; `show_moves_on` documented as the constant name.
- Tests, new:
  - `ui/game/DockLookRecipeTest.kt` (21b)
  - `ui/game/DockStateLanguageTest.kt` (21a)
  - `ui/game/DockOnMarkAndSlashTest.kt` (21e, NATIVE graphics, pixel reads)
  - `ui/game/DockSpentAllowanceTest.kt` (21c, real `GameViewModel`)
  - `ui/game/GameStatusStripTest.kt` (18a, 18b, 18c, 18e, NATIVE)
  - `ui/home/HomeStagePositionTest.kt` (15q)
  - `ui/home/HomeStageTest.kt` (15p, 15r, NATIVE)
  - `ui/home/HomeHeroRowTest.kt` (15n, 15o, NATIVE)
- Tests, retargeted (§13), none deleted or `@Ignore`d:
  - `GameScreenChromeTest.showMovesCarriesItsStateByFillAndNotByTintAlone` is now `showMovesCarriesItsStateByFillRingMarkAndSwitchStateNeverByTint`. It was the only reader of `DockActionFilledKey`.
  - `HomeLayoutTest.theHeroKeepsItsArtOnAPhoneAboveTheCompactThreshold` is now `theStageArtIsPresentOnAPhoneAboveTheCompactThreshold`.
  - The 15k narrow-baseline test's "hero art absent" is now "stage art absent on the overflowing column".
  - `aNarrowTallPhoneIsCompactEvenThoughTheWideThresholdWouldKeepItFull` now reads the compact pill shape.
- Test harness: `testing/SystemBarInsets.kt` now dispatches the status bar as `statusBars()` and the navigation bar as `navigationBars()` (see Open issues 3). `ui/home/fixture/HomeTestFixture.kt` gains `chipsState`, `EVERY_CHIP_STATE` and `columnBoundsOf`.
- `.planning/reports/15-founder-feedback/android-game-dev.md` — this report.

## Criteria → tests
| Criterion | Test(s) | Watched fail on |
|---|---|---|
| 15n | `HomeHeroRowTest.nothingButTheGearIsComposedBesideOrAboveTheTitleOnATallPhone`, `…OnTheNarrowBaseline`; `grep -rn 'HomeHeroArt' app/src/main` = 0 | old hero: "unexpected: home_hero_art" |
| 15o | `HomeHeroRowTest.theTitleSitsInOneRowWithTheGearOnATallPhone`, `…OnTheNarrowBaseline`, `atDoubleFontScaleTheTitleWrapsRatherThanShrinksAndTheGearStaysInTheRow` | old hero: title at 105dp, not 16; a mutation centring the title text |
| 15p (1, 2) | `HomeStageTest.onTheTallPhoneTheStageHoldsABoardOfAtLeast120dpAndNoGapExceeds24dp`, plus `noBlankOver{64,80}dpOnA{360x640,360x720,360x800,412x780,412x850,412x915}dpPhoneInAnyChipState` (4 chip states each) | the old empty spacer: 172–292dp blanks |
| 15p (3) | device — qa-engineer | — |
| 15q | `HomeStagePositionTest.aRecordedLineOfLegalJumpsFromTheEnglishStartArrivesExactlyAtTheStage`, `theStageDrawsABoardWithNothingSelectedTargetedHintedOrMoving`, `theStagePositionStillHasAJumpToPlay` | missing state; one peg moved |
| 15r | `HomeStageTest.theStageAndItsArtCarryNothingButTestTags`, `homesTalkBackTraversalRunsTitleGearPillsChipsTilesAndNothingElse`, `theStageRendersIdenticallyBeforeAndAfterTheFirstProgressEmission` | no stage; a mutation giving the art a description |
| 18a | `GameStatusStripTest.theTitleIsTwentyTwoSpAndTheTimerTwentyFourSpWithTabularFiguresInAFortyDpPill`, `bothTitlesRenderWholeOnA360dpStripAtDefaultScale` | 16sp; a mutation to a `displaySmall` title (ellipsised) |
| 18b | `GameStatusStripTest.eachSystemBarIsAppliedToTheGameplayScreenOnce` (device half: QA) | strip top 48dp, not 24 |
| 18c | `GameStatusStripTest.theBoardSlabKeepsItsFullWidthAtDefaultFontScale`, `…AtDoubleFontScale` | slab 304dp, not 328 |
| 18d | device — qa-engineer | — |
| 18e | `GameStatusStripTest.atDoubleFontScaleTheTimerIsWholeInAGrownPillAndTheTitleStillSpeaksInFull` | timer cut short at 200%; a mutation back to a fixed pill |
| 21a | `DockStateLanguageTest.everyControlIsInTheLookItsStateRequires` (12 rows) | no look key; a mutation holding Undo Available |
| 21b | `DockLookRecipeTest.everyPairOfLooksDiffersInAtLeastTwoChannelsThatAreNotHue`, `onAndOffDifferInEveryChannelTheDockHas` | missing type; a mutation giving Working a shadow |
| 21c | `DockSpentAllowanceTest.aSpentUndoAllowanceLooksAndSpeaksAsBeforeAndItsTapRaisesTheOffer`, `aSpentHintAllowanceLooksAndSpeaksAsBeforeAndItsTapRaisesTheOffer` | passes by construction; failed under an allowance-driven Undo disable and a Hint drawn Working while a hint shows |
| 21d | `GameScreenChromeTest.showMovesCarriesItsStateByFillRingMarkAndSwitchStateNeverByTint`; `grep -rn 'show_moves_off' app/src` = 0; `ButtonSoundTest` diff = 0 lines | no look key or switch |
| 21e | `DockOnMarkAndSlashTest.noActionEverReachesOnOrOffAndTheToggleReachesNothingElse`, `onlyShowMovesOnDrawsTheOnMark`, `noActionInAnyLookDrawsTheOnMark`, `onlyShowMovesOffDrawsTheSlashedGlyph` | no mark and no slash drawn; a mutation turning Restart Off |
| 21f, 21g | device — qa-engineer | — |

## Checks
- `./gradlew check` (baseline, at `bffe558` before any change) — pass (BUILD SUCCESSFUL in 11m 7s)
- `./gradlew check` (final, at `7493b10`) — pass (BUILD SUCCESSFUL in 11m 7s, exit 0; `:app:testDebugUnitTest` 1044 tests in 95 classes, 0 failures, 0 skipped; `:domain:test` 274 tests, 0 failures; detekt, ktlint, lint, dpSpLiteralCheck and the ID/audio gates green)
- `ANDROID_SERIAL=emulator-5560 ./gradlew :app:installDebug` — pass ("Installed on 1 device", Pixel_9 AVD, API 36; the emulator was not restarted and the app was not launched)
- `./gradlew :app:testDebugUnitTest --tests '<class>'` — red, then green, for each class above. The failure messages are quoted in the table and in each commit message.
- `grep -rn 'show_moves_off' app/src` — pass (0 hits)
- `grep -rn 'HomeHeroArt' app/src/main` — pass (0 hits)
- `git diff bffe558..HEAD | grep -E '^\+.*@(file:)?Suppress'` — pass (0 lines: no suppression added)
- `git diff bffe558..HEAD -- app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt` — pass (0 lines: both pass unedited)
- P10 `grep -rniE 'allowance|badge|×[0-9]|adReason' app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt` — raw 4 hits, all the pre-existing KDoc that states the negative contract; comment-filtered 0. The new element is the "on mark" everywhere, never "badge".
- P8 `grep -rnE 'TopAppBar|BottomAppBar|NavigationBar' app/src/main/java/com/app/pegsolitaire/ui/{home,game,onboarding} | grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` — pass (0 hits)

## Spec ambiguities resolved
1. **The suppression.** §9 and the dispatch expected one `@Suppress("MagicNumber")` for `ShowMovesOffIcon` under `ModeGlyphs.kt`'s builder precedent. The precedent in `ui/game` (`UndoIcon`, `HintIcon`, `ShowMovesIcon`) is path-string data that needs none, so the slashed glyph is written that way. **Zero suppressions were added; there is nothing for the code-auditor to approve.**
2. **Which layer applies the insets** (§4.3.1 leaves it to the developer). The `Scaffold`'s content padding does. The strip's `statusBarsPadding()` and the dock's `navigationBarsPadding()` are removed.
3. **"The on mark is drawn, not a node."** It is a draw-phase modifier on the disc's `Surface`, outside its clip. Because no semantics test can see it, 21e reads it off real pixels (NATIVE graphics).
4. **The stage art and semantics.** The stage's only semantics is its tag, as specified. The art carries only a test tag, plus the `Shape` hint that `Modifier.background(color, shape)` records, which is never announced and never focusable. It has no role, label, action or state; 15r asserts exactly that.
5. **The stage position** ("the developer picks"). I chose English-33 after the first 18 jumps of a winning line: 14 pegs over all four arms, the centre empty, with a jump still to play. The 18-jump line was found with a throwaway solver and is recorded in `HomeStagePositionTest`.
6. **The slash geometry.** A 1.5-unit strip from (3,3) to (21,21), with the glyph knocked out 1.5 units (one stroke-width) either side. It reads as "crossed out" in both themes. The peg is cut deeply, which I judged acceptable. The ux-designer may want to see it.
7. **Working.** The spinner is `primary`, and every channel changes instantly on entering Working.

## Open issues / risks
1. **Robolectric's default LEGACY graphics fakes text metrics.**
   - Measured: "Peg Solitaire" at 22sp comes out 13dp × 36dp under LEGACY and 126dp × 28dp under NATIVE.
   - My new geometry and text tests run NATIVE. `HomeLayoutTest` (15g–15m) still runs LEGACY.
   - Under NATIVE, 360×640dp with the 24/48dp insets and both chips has **0dp** of scroll overflow: the Challenge tile ends exactly at 568dp. Under LEGACY it overflows by 68dp.
   - So §4.2.3's ≈640–700dp arithmetic, and 15k's "the fold cuts through the second tile", hold only under LEGACY metrics. 15k part 2 is stated conditionally, so a fitting column still passes it.
   - Device numbers govern. This is for the qa-engineer and ux-designer; I did not touch 15k.
2. **`GetTextLayoutResult` on a wrap-content `Text`** re-lays the text out at its constraints' maximum width, so `hasVisualOverflow` always reads true there. `GameStatusStripTest` checks the timer's height, line count and line width instead, and documents why.
3. **The inset harness reported both bars on every per-type inset.** `setInsets(systemBars(), …)` stores the value on each type in the mask, so `statusBarsPadding()` also received the 48dp navigation bar. It now dispatches `statusBars()` and `navigationBars()` separately. The union a `Scaffold` or `safeDrawingPadding()` reads is unchanged, and `TutorialScreenTest` and `HomeLayoutTest` stay green.
4. **Criterion 15e was not run by me.** This slice changes Home's first frame, so §6.5's "any later branch that changes Home's first frame: the same protocol" applies. It is already assigned to the qa-engineer on this branch (code-auditor-s9 B2).
   - The design-side rules hold: no read, no decode, no asset and no SDK call are added. The stage is one `BoardCanvas` draw of a constant (replacing the cluster's draw) plus one `BoxWithConstraints` subcomposition, and 15r shows it is independent of `HomeUiState`.
   - A sitting would reinstall the develop build and force-stop the app dozens of times on the emulator the founder is watching, so I did not run it.
5. **Device checks owed:** 15p part 3; 18b's device half; 18d (whether Fredoka honours `tnum`); 21f and 21g (contrast and greyscale read); 26/26a; 27.
6. **Shadows are invisible under Robolectric NATIVE.** The Available look's "raised" channel, and the pale disc at ≈1.04:1 against the slab by design, can only be judged on a device (21g).
7. **Q-003 staffing deviation.** The code-auditor and qa-engineer run on the author's model (Opus). The founder's recorded answer stands; noted here so the reviewers carry it.

## Handoff
Next:
- **code-auditor** — review `bffe558..HEAD` against §4.2.4, §4.3.1, §4.4.1 and §13's retarget table.
  - No suppression, baseline or `@Ignore` was added. `DockActionFilledKey`'s only reader was retargeted, not deleted.
  - The P10 filtered grep is 0, and the mark is never called "badge".
  - Hunt for assertions that cannot fail. Each test above was watched fail; the mutations are listed in the commit messages.
- **qa-engineer** — on a device:
  - 15e (§6.5 second edition), 15p part 3, 18b device half, 18d, 21f, 21g, 26/26a, 27;
  - glass numbers for Open issue 1 (does 360×640dp with both chips scroll at all?).
  - `emulator-5560` (Pixel_9, API 36) has the branch build installed.
- **ux-designer** (through the producer) — Open issue 1's measurement, and a look at the slash's weight (Spec ambiguity 6).
