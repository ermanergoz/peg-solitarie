# android-game-dev report — home-continue-live-boards
Date: 2026-11-01
Branch: feature/home-continue-live-boards (292e719..4de613d, then this report's commit)
Skills loaded: ergoz-soft:standards (role android-game-dev: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md; `.claude/rules/lessons.md`, 0 lessons), superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran on: claude-opus-5-5 at effort max
Ran in parallel with: game-immersive, challenge-star-bands (their own worktrees). On this branch, in this worktree, the ux-designer and the tech-lead committed docs while I built (8077419, be58224, e7d3d33, c9bc27e, 7f4f5a2, 79a43b6, 9dafa11, 3578f6f). None of my commits stages `docs/` or another role's file.
Loops: 0
Lessons applied: none

## Summary
- **The stage is gone:** its slack is headroom above the hero row, held by D-03q's bound, H₀ = min(⅘ S₀, C) on every window, in the pure `homeBandPlacement`.
- **The Classic Continue chip shows the saved position** (the stored moves replayed through the codec, off the first frame) on a 56dp slab and speaks "N pegs left"; a history that does not replay is withheld and purged (ruling 1).
- **The chip room's effect** is keyed on its target (C2b).
- **Built and tested:** 15x, 15aa (1–4, 6), 15ab (1–4); 15y, 15z and 15ac (1–2) against the spec that passed its third check (`79a43b6`, corrected `3578f6f`), expected values from the formula.
- **Checks:** `./gradlew check` is green at `2b898a8` and at `4de613d`, the head (11m 11s).

## Outputs
Commits, in order. Each was committed with its own suites, ktlint and detekt green; the full Check ran at `292e719`, `2b898a8` and `4de613d`. Each carries the dispatch's two trailers.
- `292e719` — C0: `develop` (`37d3052`) merged in; parents `72697d7` and `37d3052`.
- `9fc6350` — C1, rulings 1 and 2.
  - `HomeViewModel` takes `RulesEngine` and the unqualified default `CoroutineDispatcher`.
  - It rebuilds the Classic snapshot with `SnapshotMapper.fromDto` under `withContext(defaultDispatcher)`, after the catalogue check.
  - It withholds and purges a history that does not replay.
  - The id and the board are carried both or neither (`ClassicResumable`).
- `ecca6f1` — C3, with C4's BD-17 and first-frame retargets, which need the thumbnail.
  - `ResumeThumbnail`: a 56dp `surfaceContainerHigh` slab in `shapes.extraSmall`, no finish, tagged `home_resume_thumb`.
  - The row is inset `spacing_md` (ends) and `spacing_sm` (top and bottom); the text column carries `spacing_sm` more.
  - The Classic chip's `stateDescription` is `board_a11y_pegs_left`. The Challenge chip is unchanged.
- `43a5838` — C2, with C4's `FinishRecipeTest`.
  - Deleted: `HomeStage.kt`, `FinishedContainer.StageArt` and both `home_stage_art_*` dimens.
  - `HomeBands` takes the `Scaffold`'s `contentPadding` and computes `titleBoundPx` once, in the viewport's frame (A1.1).
  - `HomeBandColumn` places the column from `homeBandPlacement`: S₀ over four gaps, C, H₀, headroom max(0, H₀ − R), footer min(S₀ − H₀, max(0, S₀ − R)).
  - `ChipRoom` is reduced to `fraction` and `isOpen`.
- `2601863` — C2b, §14.3 item 1: `rememberChipRoom` runs `LaunchedEffect(target)` only, its tween guarded by `openness.value != target`. While animations are off, the fraction reads the target.
- `299a144` — the footer-third read runs on every window, no longer only on the gesture-bar windows (the producer's direction on check-2 F-1).
- `e237878` — the P3 KDocs are worded by what the animations do. Comment-only.
- `2b898a8` — C5: the busiest and compact previews, light and dark, draw `PreviewSave.board`: European-37 twelve first-legal jumps in, 24 pegs.
- `cebddbd` — 15y, 15z and 15ac read against the eight-window table, the 2.0 footer and 15g's fourth clause (the hold released).
- `4de613d` — the thumbnail's slab is pinned to 15aa's 56dp, not to its token.

Files changed in `app/` since `292e719`:
- **Production:**
  - `ui/home/`: `HomeViewModel.kt`, `HomeUiState.kt` (KDoc), `HomeBoardArt.kt`, `HomeScreen.kt`, `HomeBandColumn.kt`, `HomeHero.kt` (KDoc). `HomeStage.kt` is deleted.
  - `ui/theme/FinishRecipe.kt`.
  - `ui/backdrop/BackdropMetrics.kt`: one KDoc word (Deviation 5).
  - `res/values/dimens.xml`: `home_resume_thumb_size` 40 → 56dp; `home_stage_art_min_size` and `_max_size` deleted.
- **Tests, new:** `HomeBandPlacementTest` (11), `HomeContinueThumbnailTest` (9), `HomeHeadroomTest` (19; a `git mv` of `HomeStageTest`, rewritten).
- **Tests, edited:** `HomeViewModelTest` (+5), `HomeViewModelCloudTest` (the factory), `HomeChipArrivalTest` (retargeted, +11), `HomeLayoutTest`, `HomeHeroRowTest` (Deviation 1), `fixture/HomeTestFixture.kt`, `FinishRecipeTest`, `BackdropPixelTests` (BD-17), `BoardCanvasFirstFrameTest`.
- **Tests, deleted:** `HomeStagePositionTest` (ruled; see the next section).

## Checks
**The merge-in (C0).**
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-continue merge develop` — pass (`292e719`).
- `./gradlew check` on the merged tree — pass (BUILD SUCCESSFUL in 11m 11s, 107 tasks, exit 0).

**C1, red first.**
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeViewModelTest' --tests 'com.app.pegsolitaire.ui.home.HomeViewModelCloudTest'` on the old ViewModel — fail as expected (45 tests, 4 failed: the 15aa parts 1 and 2 cases). Green after C1.
- The invariant test is green on the old code, which always carried the id and the board together. Probed: it turns red under the rejected arrow-fallback shape.
- Probed: the hollow fake DTO turns `aClassicSnapshotOnAKnownBoardIsNeverPurged` red under ruling 1. Its assertion is unchanged.

**C3, red first.**
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeContinueThumbnailTest' --tests 'com.app.pegsolitaire.ui.components.BoardCanvasFirstFrameTest' --tests 'com.app.pegsolitaire.ui.backdrop.BoardInteriorIdentityTest'` — fail as expected (15 tests, 10 failed). Green after C3.
- Mutants, each restored:
  - a gloss on the slab turns BD-17's opaque-fill sample red;
  - the row's vertical inset back to `spacing_md` turns two height reads red;
  - a 40dp token stayed green while the test read the token. Since `4de613d` it turns 4 red.

**C2, red first.**
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeBandPlacementTest'` against D-03p-only arithmetic — fail as expected (10 tests, 8 failed: every case the bound decides). Green once C is in.
- The composed suites on the stage layout (`HomeHeadroomTest`, `HomeChipArrivalTest`, `HomeLayoutTest`, `HomeHeroRowTest`, `FinishRecipeTest`, `HomeBandPlacementTest`) — fail as expected (73 tests, 34 failed). Green after C2, once the 15k overflow window had moved (Deviation 2).

**C2b.**
- P3's two parts and step 1b are green on the gate-keyed effect before the change, as loop 3 found.
- Under the auditor's strand mutant M1 (`snapTo(target)` → `stop()`), both parts are red.
- On the new shape, a guard that skips the tween while animations are off turns part 2 red.
- A probe that switches animations back on two frames after switching them off reads every frame settled: the tween ends within a frame once the scale is 0.
- None of these probes is committed.

**The hold released (`cebddbd`, `4de613d`).** Mutants on the new reads, each restored:
- With the bound removed, 20 tests turn red. Among them are the eight-window table test, 15u's own walk and the four new walks on windows where the bound bites. The three new walks on windows where it does not bite stay green.
- A footer that forgets S₀ − H₀ turns all eight 2.0 cases red. Before, they read only the headroom and the bound.
- The eight-window expected figures were recomputed outside the code, in float32 with `Math.round`'s half-up rounding (a scratch Python script, not committed). They match the test row for row and the table as corrected in `3578f6f`: H₀ 192 / 242.4 / 280 / 229.5 / 285.5 / 326.1 / 326.1 / 299.8; tile tops 328 / 378.4 / 416 / 434.9 / 490.9 / 531.5 / 531.5 / 535.2.
- At 2.0 the bound bites on none of the eight windows. The footer is at most 0.202 S₀ everywhere (a fifth, give or take px rounding), and min(S₀ − H₀, max(0, S₀ − R)) in every state (NATIVE probe).
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeBandPlacementTest' --tests 'com.app.pegsolitaire.ui.home.HomeChipArrivalTest' --tests 'com.app.pegsolitaire.ui.home.HomeHeadroomTest' --tests 'com.app.pegsolitaire.ui.home.HomeContinueThumbnailTest'` — pass (11 + 23 + 19 + 9 tests, 0 failed).

**Wider suites, lint and greps.**
- After C2: `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.*' --tests 'com.app.pegsolitaire.ui.backdrop.*' --tests 'com.app.pegsolitaire.ui.components.*' --tests 'com.app.pegsolitaire.ui.theme.*' --tests 'com.app.pegsolitaire.ui.ButtonSoundTest'` — pass (411 tests, 0 failed).
- `./gradlew :app:ktlintCheck :app:detekt` after each commit — pass.
- No `@Suppress`, `@Ignore` or disabled test added: `git diff 292e719 HEAD -- app/ | grep -E '^\+.*(@Suppress|@Ignore|@Disabled)'` prints nothing (exit 1).
- No baseline added: `git diff --name-only 292e719 HEAD | grep -i -E 'baseline|detekt|lint\.xml'` prints nothing (exit 1).
- 15x: `grep -rn 'HomeStage\|HOME_STAGE\|home_stage_art' app/src/main` — pass (no output, exit 1).

**Full Checks.**
- `./gradlew check` at `2b898a8` — pass (BUILD SUCCESSFUL in 11m 49s, 107 tasks, exit 0).
- `./gradlew check` at `4de613d` — pass (BUILD SUCCESSFUL in 11m 11s, 107 tasks, exit 0)

## Tests deleted, retargeted and added, with their authority
**Deleted with their subject.** §13's D-03p row "`HomeChipArrivalTest` (15s–15u) … the `StageArtFade` cases are deleted with their subject":
- `HomeChipArrivalTest.theStageArtFadesAtItsFloorRatherThanVanishingOrPoppingWhenBothChipsCome`.

**`HomeStagePositionTest` removed, its three tests.** Authority: §13's row "`HomeStagePositionTest` (15q) … Retarget to 15aa part 1 … a `HomeViewModel` test over a fake repository", and the brief's C2, "retired, its subject is C1 (a)".
- `aRecordedLineOfLegalJumpsFromTheEnglishStartArrivesExactlyAtTheStage` → `HomeViewModelTest.aRecordedLineOfLegalJumpsIsShownAsExactlyThePositionItLeaves`, beside `aClassicContinueCarriesThePositionTheStoredMovesLeaveAndNotTheStartBoard` and `aLaterSaveOfTheSameBoardIsShownAtItsNewPosition`.
- `theStageDrawsABoardWithNothingSelectedTargetedHintedOrMoving` and `theStagePositionStillHasAJumpToPlay` asserted on `HomeStageBoardState`, the stage's constant, deleted with `HomeStage.kt`. 15q is superseded (§14's header). The thumbnail draws through `StaticBoardArt` (`GameUiState.initial(board)`, `interactive = false`).

**`HomeStageTest` → `HomeHeadroomTest`: retargeted, never deleted.** Authority: §13's row "`HomeStageTest` — 15p parts 1 and 2 …".
- The six `noBlankOver…` cases → the six `on…TheSlackIsHeadroomAboveTheHeroRow…` (15y); the gap read is 15y part 3.
- `onTheTallPhoneTheStageHoldsABoardOfAtLeast120dpAndNoGapExceeds24dp`: its gap read is 15y part 3; the ≥ 120dp art went with its subject.
- `theStageAndItsArtCarryNothingButTestTags` → `nothingIsComposedInTheHeadroomAndTheOnlyBoardOnHomeIsTheClassicThumbnail` (15x).
- The traversal case is kept (15ab part 3).
- `theStageRendersIdenticallyBeforeAndAfterTheFirstProgressEmission` → `theHeroRowAndTheRibbonHoldStillAcrossTheFirstProgressEmission`.

**Retargeted in the other suites.**
- `HomeChipArrivalTest` (15z):
  - `nothingAPlayerCanTapMoves…` ×6 → `theRoomComesFromTheHeadroomAndTheTilesHoldStill…`;
  - `whereTheStageCannotGiveTheRoom…` ×2 → `whereTheHeadroomCannotGiveTheRoom…`;
  - the frame tests now read the hero row and the ribbon together.
- `HomeLayoutTest`:
  - `aWidePhoneKeepsTheFullBandSetWithTheStageArtPresent` → `…ByItsBandSpacing` (15l's D-03p pointer);
  - 15k part 4's overflow read now reads the hero row at the top padding;
  - its two 15g cases read only the midpoint, never ⅘ S₀, S₀ ⁄ 5 or a gap, so §13's D-03q row and the F-2 ruling leave nothing in them to retarget. 15g's fourth clause, under F-2's reading, is read in `HomeHeadroomTest`.
- BD-17 (ruling 4): `homesStageArtGelTilesAndContinueChipsAreIdenticalWithAndWithoutTheBackdrop` → `homesGelTilesContinueChipsAndClassicThumbnailAreIdenticalWithAndWithoutTheBackdrop`.
- `BoardCanvasFirstFrameTest` (15aa part 6): `theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab` → `theFirstFrameOfTheClassicThumbnailDrawsItsPegsOnTheSlab`.
- `FinishRecipeTest` (ruling 4 (b)): the `StageArt` row is gone, and the test is renamed `theGlossLiesOnTheChipsAndPillsAndNeverOnTheSlabOrTheDock`.

**Rows with nothing to retarget.**
- `HomeViewModelTest` / `HomeViewModelCloudTest` (§13's `previewBoard` row): no existing case asserted the descriptor's `previewBoard`. Only the fake changed: it writes codec-valid DTOs through `SnapshotMapper.toDto`, and the cloud factory passes `RulesEngine` and the test dispatcher.
- `HomeScreenTest` and the fixture's `Boards.*Initial` states: unedited, as the row says. The fixture adds the mid-game English board the row allows for 15aa part 3.

**BD-22 (`BackdropMirrorTest`): nothing to retarget.** It renders the host alone and reads no Home node, so BD-22's "Home read" is BD-17's Home case (ruling 4 (c)). It is unedited.

**Added.**
- `HomeViewModelTest`: five tests (15aa parts 1 and 2, and the invariant).
- `HomeContinueThumbnailTest`: nine (15aa parts 3 and 4, 15ab parts 1 and 2).
- `HomeBandPlacementTest`: eleven — A1.2's table, with the eight-window table derived from the formula. It agrees with the table as corrected in `3578f6f`.
- `HomeHeadroomTest`:
  - 15ac part 1 at 1.0 and 2.0 on the eight windows, including the 2.0 footer reads;
  - the footer at most ⅓ S₀ on every window;
  - 15g's fourth clause on the 6.7-inch phone.
- `HomeChipArrivalTest`:
  - 15ac part 2 on the two gesture-bar windows;
  - the frame walk on all eight windows;
  - P3's two parts with step 1b (C2b).

**Ruling 1's spec lines the build relies on**, so the ux-designer's amendment can be checked against the code:
- 15aa part 2 as revised:
  - withheld, with `classicResumeAvailable` false and `classicResumeBoard` null;
  - `clearSnapshot(MODE_CLASSIC)` exactly once;
  - `observeResumeModes` as today;
  - the Challenge chip unaffected;
  - the both-or-neither invariant over five shapes: absent, unreadable, unknown id, forged history, valid.
- 15ab part 2 as revised: a Classic chip in production always has a board and carries the count; the Challenge chip carries no state.
- §4.2.7's struck play-arrow fallback.
- In the code:
  - when `loadSnapshot` returns null, the ViewModel returns null without a clear, because the repository has already purged the row;
  - an unknown id, or a `fromDto` null, clears the row and returns null;
  - `ResumeTile` keeps the arrow path for the Challenge chip only.

## Deviations
1. **`HomeHeroRowTest` edited, though §13's row says "pass unedited".**
   - `assertNothingElseTakesRoomBesideOrAboveTheTitle` read the row's top as exactly one column inset below the viewport. D-03p moves the row down by the headroom, so that read could not pass.
   - It now expects the inset plus the headroom, max(0, H₀ − R). The headroom is computed from the window and the bands (`homeSlack`), never read off the row.
   - The node half, "no node above the title", is unedited and passes.
2. **15k's overflow window moved from `w360dp-h560dp` to `w360dp-h552dp`** (`HomeLayoutTest`).
   - D-03p took the stage's band gap out of the column, 8dp less column there. That left an overflow of exactly the 16dp trailing space.
   - The test's own precondition then failed as written: "the window must overflow past its trailing space".
   - So the window moved 8dp shorter, as the precondition asks. Its three cases are unchanged.
3. **15aa part 3 at font scale 2.0 on `w412dp-h915dp`: the chip is 154dp with the thumbnail against 116dp with the arrow** (NATIVE metrics).
   - The slab is 28dp wider than the arrow, so the text column is 276dp, not 304dp, and the supporting line wraps.
   - The height equality is asserted on the other three readings; "the text sets the height" is asserted on all four.
   - No layout within §4.2.7's 56dp slab, `spacing_md` gap and end inset gives the text the arrow's width. This is a question for the ux-designer.
4. **15aa part 3's "top and bottom edges `spacing_sm` inside the chip" is read as "at least `spacing_sm`, centred".**
   - The text sets the chip at 80dp at default scale, so the 56dp slab sits 12dp inside each edge.
   - Exactly 8dp would need a 72dp chip, which the criterion's height-equality clause forbids.
5. **`ui/backdrop/BackdropMetrics.kt`, on the frozen list, changed by one KDoc word.**
   - The word was `HomeStageMetrics`, which 15x's grep must not find. Comment-only; no constant or behaviour moved.
   - `BackdropPixelTests` was edited because the brief's C4 names it. I read the frozen list's "no test of the backdrop moves" with C4 as its exception.
6. **Commit order C1 → C3 → C2 → C2b → C5, not the brief's C1 → C2 → C3.**
   - BD-17 and the first-frame retarget need the thumbnail (C3).
   - `FinishRecipeTest`'s `StageArt` row must leave with the `StageArt` deletion (C2).
   - Ordered this way, each commit compiles, and its own suites pass on it.
7. **`ChipRoom` lost `present`, `moving`, `progress` and `target`.** Only the stage read them, and the class is internal to `ui/home/`.
8. **Trailers.** The `gitflow` skill says no co-authored lines and no type prefixes. rules/gitflow.md (conventional commits) and the dispatch's two trailers override it, and every commit follows them.

## Open issues / risks
1. **Readings taken where the amended text leaves room.**
   - (a) 15ac part 1's footer clause, "S₀ − H₀ plus one band gap and the bottom padding", is read as the formula of 15y part 2 and 15z part 3: min(S₀ − H₀, max(0, S₀ − R)).
     - At 1.0, R ≤ H₀ in every chip state on all eight windows, so the two agree.
     - At 2.0 the room outgrows the headroom in some states (headroom 0, footer max(0, S₀ − R)), so the unscoped wording would be false there.
     - If the ux-designer means it unscoped, the tests and the spec disagree.
   - (b) 15ac part 2's "no frame of the room's motion" is read as 15z part 5's whole frame walk on all eight windows.
   - (c) 15g's fourth clause is read on the 6.7-inch phone, 15g's own window. 15y part 3's band-gap read, on every window, is stricter.
2. **The second `develop` merge-in after `feature/game-immersive` lands is owed** (A1.3); the producer says when.
   - `HomeScreen.kt`'s content lambda is kept apart from immersive's `contentWindowInsets` line.
   - Untouched: the two Home dialogs, `ui/window/`, `navigation/`, `MainActivity.kt` and `testing/SystemBarInsets.kt`.
3. **Measured D-03q figures at 1.0** (NATIVE, insets supplied; dp):

   | Window (bars) | S₀ | ⅘ S₀ | C | H₀ |
   |---|---|---|---|---|
   | 360×640 (24 + 48) | 240 | 192 | 204.8 | 192 |
   | 360×720 (24 + 48) | 320 | 256 | 242.4 | 242.4 |
   | 360×800 (24 + 48) | 400 | 320 | 280 | 280 |
   | 412×780 (24 + 48) | 336 | 268.8 | 270.6 | 268.8 |
   | 412×850 (24 + 48) | 358 | 286.4 | 295.5 | 286.4 |
   | 412×915 (24 + 48) | 423 | 338.4 | 326.05 | 326.05 |
   | 412×915 (24 + 24) | 447 | 357.6 | 326.05 | 326.05 |
   | Pixel_9 411×923 (54 + 24) | 425 | 340 | 299.81 | 299.81 |

   - The measurements agree with §4.2.7's eight-window table where it computes the same window.
   - `w412dp-h780dp` takes the compact set (708dp usable, under the 720dp threshold). Its S₀ is 336 against the table's full-set 286.9; H₀ is ⅘ S₀ either way.
   - At 2.0 C exceeds ⅘ S₀ on every window: the bound does not bite at 2.0. The hero row measures 53dp there, its title on one line on all eight windows, so C is 321.05dp on the 412×915 windows and 294.81dp on the Pixel_9. Check-3's F-3 estimate (a wrapped title, C ≈ 208dp) is lower, but its conclusion stands: at 2.0 only the bound is read.
4. **QA's 360×640dp window (52 + 24) with both chips: H₀ − R₂ is 0.8dp** (check-2 F-6). It is 15t's case under 15ac part 2, and no composed test reads that window.

## Handoff
Next: code-auditor
- Review `292e719..4de613d` against:
  - the ux-designer's Handoff: §4.2.7, D-03p and D-03q, 15x–15ac, and §13's D-03p and D-03q rows;
  - the tech-lead's brief with Amendment 1.
- The ruled deletions and retargets are listed above with their §13 rows.
- Deviations 1, 2 and 5 are edits outside the letter of §13 or the frozen list, each with its reason.
- C2b's P3 is committed.

Next: qa-engineer — on the device:
- 15aa part 5 — the thumbnail's legibility:
  - one 7-wide and one 9-wide board;
  - early, half-cleared and near-win positions;
  - on the Pixel_9 and at 360×640dp;
  - in light and dark.
- 15ab part 5: TalkBack's one stop with "N pegs left", and a double-tap resuming at that position.
- 15ac part 3:
  - the dark and light no-chip, one-save and both-saves reads;
  - the bound, and glyph contrast at rest and in motion;
  - the cold-start recording;
  - 15y part 3 on the device.
- 15v's re-read as 15z.
- C2b's P3 on a device: a live switch of Remove animations mid-opening.
- The branch's own 15e sitting on `Pixel_9a_bench`. Arm A is the merge-base after the last `develop` merge-in (A1.3): `37d3052` today, until the owed merge-in moves it.

Next: producer
- Open issue 1 (a) and Deviations 3 and 4, for the ux-designer.
- The owed `develop` merge-in (Open issue 2).
