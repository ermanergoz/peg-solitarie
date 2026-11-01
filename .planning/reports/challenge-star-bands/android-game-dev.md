# android-game-dev report — challenge-star-bands
Date: 2026-11-01
Branch: feature/challenge-star-bands (17c7c28..a26b9e8)
Skills loaded: ergoz-soft:standards (role: android-game-dev; this product's rules snapshot is 0.6.2 and the plugin 1.0.0, so the plugin's rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; the product's `.claude/rules/lessons.md`, 0 active), superpowers:test-driven-development (with its writing-good-tests.md), gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: the ux-designer's and the game-designer's amendments on this branch (b8e4c8c, b5667a9 and the Q-040 re-scope 86dc35f, committed by the producer while I built; I staged none of their files); other worktrees' builds, depth sweeps and emulators on the same host, which set the load figures below
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
The Challenge star bands for 3.0.0, built to the founder's Q-040 re-scope (docs/gdd.md 86dc35f). One clear rule: 1, 2 or 3 pegs earn 3, 2 or 1 stars and advance the ladder; 4 or more is stuck. Also: the mode-aware finish with old-stamp restart, Results' CLEARED tier and next-star line, the Challenge banner copy with the dismissable proven-dead banner, and the curve shifted 30 levels (contract 2, fixtures regenerated).
Four parts I built before Q-040 are now 3.0.1: G6 (fail-proof), G7 (freeform mix), the pool copies and C5's DAO predicate. They come out in one clean revert commit (6b335d4), along with C6's mapper floor (one test kept).
`./scripts/check.sh --offline` is green: BUILD SUCCESSFUL in 11m 9s at a26b9e8. M1 is +2 % (9.04 s against 8.83 s). M2 and M3 have nothing to measure in 3.0.0.

## Outputs
Work commits, in order (`git log 80b8e5a..a26b9e8` also holds the three doc commits named above):
- `17c7c28` C1 `feat(domain): Challenge clear rule as one scoring function` — `domain/.../scoring/ChallengeClearRule.kt` (new: `ChallengeFinish`, `CLEAR_MAX_PEGS = 3`, `PERFECT_STARS`, `finish`, `isCleared`, `status`); `ScoreCalculator.evaluate(stars = …)`; the `GameStatus` KDoc. Stands.
- `629abac` C2 `feat(domain): generator contract v2: ladder cut, fail-proof opening, mix` — built to the pre-Q-040 GDD: offset 50, G6, G7, the pool copies, `GENERATOR_VERSION = 2`, the fuzz `minimumPegs` line. 6b335d4 and a26b9e8 narrow it to Q-040's C2. Its body's M2/M3 figures belong to the reverted contract.
- `911838c` C3 `feat(app): mode-aware finish; clears advance; old stamps regenerate` — `GameViewModel` (`statusFor` by mode, `scoreFor` with the rule's stars, `boardGeneratorVersion`, `restartOnTheCurrentContract`, the load reads the active level); the `GameUiState.stuck` and `GameFinish.minimumPegs` KDocs. Stands.
- `62bc812` C4 `feat(app): Results reads one solved test; CLEARED headline tier` — `ResultsUiState.isSolved` by mode, `ResultsViewModel` reads it from the state, `tierFor` + `CLEARED`, `results_headline_cleared`. Stands.
- `92b7f7d` C5 `feat(data): skill achievements need a three-star play` — reverted whole by 6b335d4 (3.0.1, GD-SB-05).
- `39b9e6a` C6 `fix(data): a Challenge blob without a stored target resolves to one peg` — reverted by 6b335d4 except `aSnapshotStampedWithAnotherGeneratorVersionPassesTheStampThroughUntouched`, which GD-SB-13 keeps (3.0.1, GD-SB-15).
- `98bf1f3` C7 `feat(ui): Challenge banner copy, Cleared headline and next-star line` — `ChallengeClearRule.nextBandUp` + `ChallengeBand`; `ResultsUiState.nextStarBand`; the next-star line (`RESULTS_NEXT_STAR_HINT_TEST_TAG`, `rememberFadeIn`); the three plurals; `RescueBanner` and `NoWinBanner(onDismiss)`; `GameScreen(onDismissProvenDead)`; `GameViewModel.onDismissProvenDeadBanner`; `GameRoute` wiring; previews (Challenge proven-dead banner; new `ResultsScreenPreviews.kt`, one-star clear; light and dark). Stands.
- `6b335d4` `revert: move fail-proof, freeform mix, pool, DAO and mapper to 3.0.1` — the clean revert of everything gdd §14 now marks [3.0.1]:
  - All of C5.
  - C6's `SnapshotMapper` floor by mode and its two Diamond-41 tests.
  - From C2: `DifficultyTier.failProof`, `FAIL_PROOF_LEVELS`, `isFailProof` (G6); `FREEFORM_INTRO_LEVEL = 10`, `FREEFORM_SHARE_DENOMINATOR`, `FREEFORM_DRAW` (G7); the one-peg pool copies (GD-SB-15); and their tests (list below).
  - These files are back to 80b8e5a byte for byte: `LevelGenerator.kt`, `DifficultyTier.kt`, `PlaySessionDao.kt`, `ProgressRepository.kt`, `ProgressAggregate.kt`, `SnapshotMapper.kt`, `ProgressAggregateTest.kt`, `FreeformValidityTest.kt` and `BoardSpecTest.kt`. `PlaySessionDaoTest.kt`, which C5 created, is deleted.
  - The commit also makes the `ChallengeClearRule` and golden-test KDoc true of 3.0.0, and regenerates the fixtures for its intermediate state (offset still 50) so the commit is green.
- `a26b9e8` `feat(domain): shift the ladder thirty levels along the curve (Q-040)` — `Curve.LEVEL_OFFSET = 30`, KDoc as a shift; the curve tests re-based per gdd §14; fixtures 1, 2 and 10 regenerated (100 and 1000 byte-identical: at those levels the two offsets differ only in the score multiplier).
- `/Users/ermanergoz/Desktop/PegSolitarie-stars/.planning/reports/challenge-star-bands/android-game-dev.md` — this report, in its own commit.

Net change against 80b8e5a, for 3.0.0:
- `:domain`: the clear rule; `DifficultyTiers` (offset 30 and the `internal forLevel(levelNumber, levelOffset)` oracle); `GENERATOR_VERSION = 2`; the five fixtures; the fuzz `minimumPegs` line.
- `:app`: `GameViewModel`, `GameRoute`, `GameScreen`, `GameScreenPreviews`, `NoWinBanner` (+ `RescueBanner`), `GameUiState` and `GameFinish` KDoc; `ResultsUiState`, `ResultsViewModel`, `ResultsScreen`, `ResultsCelebration` (`rememberFadeIn`), `ResultsScreenPreviews`; `strings.xml` (one string, three plurals).
- No dependency, no `THIRD_PARTY.md` row, no Room schema change.

## Checks
- `./scripts/check.sh --offline` at a26b9e8 (CLAUDE.md's Check command: `./gradlew check --offline` under the worktree lock) — pass: exit 0, `BUILD SUCCESSFUL in 11m 9s`, 669 s wall, 107 actionable tasks (17 executed, 2 from cache, 88 up-to-date), 1-minute load 3.63 at the start and 3.36 at the end, clean tree. Executed: `:domain:depthSweep`, `:app:lintDebug` and `:domain:koverVerify` (its `verify.err` is empty). `:domain:test` came from the build cache, and `:app:testDebugUnitTest`, `:domain:fuzz`, ktlint and detekt were up-to-date: their identical inputs last ran green in the a26b9e8-tree run on the next line.
- `./gradlew --offline :domain:test :domain:fuzz :domain:ktlintCheck :domain:detekt :domain:koverVerify :app:testDebugUnitTest :app:ktlintCheck :app:detekt` — pass on the 6b335d4 tree (BUILD SUCCESSFUL in 2m 22s) and on the a26b9e8 tree (1m 54s). Both: `:domain` 311 tests in 34 classes; fuzz 10,000 levels, determinism pass, 0 failures; `:app` 1,683 tests in 197 classes; 0 skipped, 0 failures.
- `./gradlew --offline :domain:test --tests '*FreeformValidityTest*'` before the revert — fail, as intended. The shipped `footprintsAreCanonicalBelowTheIntroLevelAndFreeformAtIt` is red on the mix; after the revert it is green.
- `./gradlew --offline :domain:test --tests '*DifficultyTierTest*' --tests '*QualityGateTest*'` at offset 50 — fail, as intended: the four re-based tests (19 run, 4 failed). At offset 30 those four are green and only the golden test is red. Then `UPDATE_GOLDEN=true ./gradlew --offline :domain:test --tests '*ChallengeGeneratorGoldenTest*' --rerun` — regenerated.
- `./gradlew --offline :domain:koverXmlReport` after the Check — pass (LINE 934/940 = 99.36 %, BRANCH 520/559 = 93.02 %; M5).
- `FUZZ_SEEDS=10000 ./gradlew --offline :domain:fuzz --rerun-tasks` (M1, four runs over two trees) — pass each time (10,000 levels, determinism pass, 0 failures).
- `./gradlew --offline :app:testDebugUnitTest :app:ktlintCheck :app:detekt :app:lintDebug` at 98bf1f3 (C7) — pass (1,690 tests in 198 classes, before the revert).
- `./scripts/check.sh --offline` at 98bf1f3, the pre-Q-040 scope — pass (BUILD SUCCESSFUL in 11m 7s). Superseded by the run above.

## Measurements (tech-lead brief §2, narrowed by Q-040)
| # | What | Command | Result | Pass line |
|---|---|---|---|---|
| M1 | Fuzz time at 10,000 levels | `FUZZ_SEEDS=10000 ./gradlew --offline :domain:fuzz --rerun-tasks`, in a `git archive 17c7c28` copy ("before", contract 1) and in this worktree at a26b9e8 ("after"), alternating, each run started at a 1-minute load ≤ 5.0 (driver in the scratchpad, not committed) | before 8.673 s and 8.978 s; after 8.870 s and 9.212 s (the test method's own `time=`); means 8.83 s → 9.04 s, +2 % | ≤ 60 s; above 12 s would need attempt stats |
| M2 | Levels 1–4 accepting attempts under G6 | — | nothing to measure in 3.0.0: G6 is 3.0.1 | — |
| M3 | Accepted freeform share, footprint first appearances | — | nothing to measure in 3.0.0: G7 is 3.0.1 | — |
| M4 | Level-load times on a device | QA's | QA's (SB-Q1, and SB-Q9's Continue) | brief §2 |
| M5 | `:domain` line coverage | kover, from the Check | 934 of 940 lines, 99.36 % (`./gradlew --offline :domain:koverXmlReport` over the final tree's test data, which the Check's `koverVerify` passed). The one uncovered line is in `LevelGenerator`, a file this change leaves as it was at 80b8e5a | ≥ 90 % (koverVerify) |

For the record: on 98bf1f3 (the pre-Q-040 contract), in a quieter window, the same pairs read 6.203 s and 6.162 s before, 5.281 s and 5.403 s after. That tree no longer exists in 3.0.0.

## Tests added, changed and retired (net, at a26b9e8)
Domain (`:domain:test` 294 → 311 tests, 33 → 34 classes; the fuzz harness is its own task):
- `ChallengeClearRuleTest` (new, 12):
  - `onePegLeftIsClearedWithThreeStars`, `twoPegsLeftAreClearedWithTwoStars`, `threePegsLeftAreClearedWithOneStar`
  - `fourOrMorePegsLeftAreNotCleared` (4..64)
  - `aBoardWithALegalMoveIsStillPlayingWhateverItsPegCount`
  - `theClearRuleIgnoresTheFootprintsMinimumPegs`
  - *(brief)* `aPegCountBelowOneIsNotCleared`, `clearMaxPegsEqualsThePerfectStarCountSoTheBandIsOneStarPerPeg`
  - mine: `aTerminalBoardIsWonUpToThreePegsAndStuckFromFour`, and from C7 `theNextBandUpFromAOneStarClearIsTwoPegsForTwoStars`, `theNextBandUpFromATwoStarClearIsOnePegForThreeStars`, `aPerfectClearAndAFinishThatDidNotClearHaveNoNextBandUp`
- `ScoreCalculatorTest` + *(brief)* `evaluateWithExplicitStarsKeepsScoreAndCarriesTheStars`. `theDefaultTargetReproducesTheGlobalBandsAcrossTheWholePegDomain` is green unedited.
- `DifficultyTierTest` + `levelOneDealsTheTierTheOldCurveDealtAtLevelThirtyOne` (the GDD §7.1 row by hand, then equality with the old level 31), `everyLevelDealsTheTierTheOldCurveDealtThirtyLevelsLater` (1..2000, every field, against `forLevel(n + 30, levelOffset = 0)`) and `theSolutionLengthCapIsReachedAtLevelSixHundredAndFortySix`. Its shipped tests are unedited.
- `QualityGateTest` + `levelOneDealsFivePegsAndAFourJumpLine` (SB-D6).
- `FuzzHarnessTest`: names any level whose spec is not the one-peg target (I5/G8, every level).
- `ChallengeGeneratorGoldenTest`: KDoc only. All five fixtures carry `generatorVersion=2`:
  - levels 1, 2 and 10: English-33, 5 pegs, 4 moves;
  - level 100: freeform, 17 cells, 9 pegs, 8 moves;
  - level 1000: freeform, 39 cells, 31 pegs, 30 moves.
- `FreeformValidityTest` and `BoardSpecTest`: as at 80b8e5a.

App (`:app:testDebugUnitTest` 1,641 → 1,683 tests, 194 → 197 classes):
- `GameViewModelTest` + `aChallengeBoardEndingWithTwoPegsEmitsAFinishWithTwoStarsAndAdvancesTheLadder`, `aChallengeBoardEndingWithThreePegsEmitsAFinishWithOneStarAndAdvancesTheLadder`, `aChallengeBoardEndingWithFourPegsIsStuckEmitsNoFinishAndDoesNotAdvance` (also: clock frozen, snapshot kept), `undoFromAStuckChallengeBoardReturnsItToPlayingAndClearsTheBanner`, `aClassicBoardEndingAboveItsFloorIsStillStuckUnderTheChallengeRule` and `dismissingTheProvenDeadBannerClearsOnlyTheProofFlagAndChargesNothing` (SB-V9). `aDiamondFinishAtFivePegsIsOnlyOneStarAndNeverReachesResults` and `stuckThenUndoThenReSolveToWonEmitsExactlyOneFinishedEvent` are green unedited.
- `GameViewModelResumeTest` + `aSnapshotStampedWithGeneratorVersionOneRestoresOnItsStoredBoard` (SB-V7), *(brief)* `restartOfAResumedOldStampLevelServesTheLevelsCurrentBoardAndItsRecordedLine`, *(brief)* `restartOfAResumedCurrentStampLevelKeepsItsStoredBoardWithoutRegenerating` (SB-V8); mine: `leavingAResumedOldStampGameKeepsItsStampSoALaterRestartStillRegenerates`.
- `GameViewModelChallengeAdvanceTest.aStuckChallengeBoardNeverAdvancesTheChallengeLevel` — **fixture changed, sentence unchanged**. It played a two-peg dead end, which R5 makes a two-star clear; it now plays a four-peg one.
- `ResultsViewModelTest`:
  - **deleted** `aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded`: its sentence is false under R5, and gdd §14's GD-SB-14 row retires it.
  - **replaced by** `aOneStarChallengeClearIsRecordedSubmittedAndOffersNextLevel` (SB-V4) and `aChallengeRouteWithFourPegsRecordsNothingAndShowsRetry` (SB-V5).
  - added: `aOneStarChallengeClearIsInterstitialEligible`; mine: `challengeHeadlinesArePerfectSolvedAndClearedByStarsAndStuckPastTheBand`, `aChallengeTwoPegClearIsSolvedWhateverFloorTheRouteCarries`.
  - Six cases that used three pegs as a stuck Challenge route now use four (C4's body names them), and so does `NavFlowTest`'s `STUCK_PEGS_REMAINING`.
- `ProgressRepositoryTest` + `aLaterThreeStarClearUpgradesAOneStarBest`, `aLaterOneStarClearLeavesAThreeStarBestUntouched` (§6.4, CSB-04).
- `SnapshotMapperTest` + `aSnapshotStampedWithAnotherGeneratorVersionPassesTheStampThroughUntouched` (GD-SB-13).
- `ResultsScreenTest` + `aOneStarChallengeResultShowsTheClearedHeadlineAndTheNextStarLine`, `aTwoStarChallengeResultShowsTheNextStarLine`, `aPerfectChallengeResultShowsNoNextStarLine` (SB-U1, 30a) and `classicResultsAndTheStuckChallengeBranchShowNoNextStarLine` (30b). `solvedChallengeShowsExactlyOneNextLevelAndNoReplayOrRetry` is **extended** to one star (30e).
- `ResultsScreenTalkBackTest` + `aOneStarClearIsSummarisedAsClearedAndItsNextStarLineIsTheStopAfterTheStars` (30d).
- `ResultsUiStateTest` (new, 5): the next band comes from the rule; there is none at three stars, on the STUCK branch or in Classic, and the line never names one star.
- `ChallengeStarBandsCopyTest` (new, 4): the four strings at the rule's numbers, and their forbidden words (30c, 30h, 30i).
- `GameScreenRescueBannerTest` (new, 6; there is no `GameScreenTest`, gdd §14):
  - `aStuckChallengeBoardShowsTheChallengeStuckMessageWithItsPegCount` (SB-U2) and `aProvenDeadChallengeBoardSaysOnePegIsOutOfReachNotThatTheLevelIsLost` (SB-U3), plus their Classic halves;
  - `aTapOnTheChallengeProvenDeadBannerPutsItAwayAndTheBoardBeneathThenPlays` (SB-U5, 30j);
  - `aTapOnAStuckChallengeBannersMessageLeavesItUp`.
- `NoWinBannerTest` + `onlyTheChallengeProvenDeadBannerCarriesTheDismissAction` (GD-SB-19), `aDismissableBannerGoesOnATapOutsideItsButtonsOrOnTalkBacksDismissAndNeverOnAButton` and `aBannerWithoutADismissalCallbackHasNoDismissActionAndATapOnItsMessageChangesNothing`.
- `GameScreenHintNoticeTest.theUndecidedNoticeShowsItsOwnTextAndNeverTheBanner` (23a): its exclusion list also names the two Challenge messages.
- `ResultsStarPaletteTest` + the one-earned-star row, in both themes and both palettes (30n's JVM half).

Built, then retired by Q-040 in 6b335d4 (gdd §14 marks each [3.0.1]):
- `:domain`: `onlyTheFirstFourLevelsAreFailProof`, `everyPoolEntryCarriesTheOnePegTargetWhateverItsClassicFloor`, `levelsOneToFourCannotEndWithMoreThanThreePegs`, `allFiveCanonicalFootprintsAppearByLevelThreeHundred`, `footprintsAreCanonicalBelowLevelTenAndMixedFromIt` and `theFreeformShareOverLevelsTenToOneThousandLiesBetweenThirtyFiveAndSixtyFivePercent`; also the every-level walk in `BoardSpecTest`, and footprint comparisons in place of identity ones.
- `:app`: `PlaySessionDaoTest` (5 tests); the two Diamond-41 `SnapshotMapperTest` cases.

Renamed by a26b9e8: the three curve tests (`…FiftyOne`, `…FiftyLevelsLater`, `…SixHundredAndTwentySix`) and `levelOneDealsSixPegsAndAFiveJumpLine`.

TDD evidence:
- Every production change was preceded by a failing run: a compile failure for a missing API or, wherever the API could be stubbed first, a behavioural failure. C1: 6 of 9; C3: 5; C4: 4; C7's Results line: 3; C7's banner tests: at compile.
- The revert: the shipped freeform test red, then green.
- The shift: four tests red at 50, then green at 30.
- Characterization tests passed on their first run, pinning behaviour the code already had: `aSnapshotStampedWithGeneratorVersionOneRestoresOnItsStoredBoard`, the current-stamp restart, the four-peg Challenge route, the two `ProgressRepositoryTest` merges, the stamp pass-through and the palette test's one-star cases.

## Deviations and decisions (for the code-auditor and the tech-lead)
1. **Subjects shortened to 72 characters** (rules/gitflow.md). The brief's C2 subject is 73 characters (" — " became ": "), C3's 90 and C7's 83. Each body names its contract item.
2. **C3: the stamp travels with the board.** `onLeaveScreen` writes the board's own stamp (`boardGeneratorVersion`), not the build's.
   - Without it, a resumed version-1 board re-saved after one trip to Home would be labelled version 2, and every later restart would rebuild the old board.
   - SB-Q9's "Restart serves a different board" would then fail if QA backgrounds the app between Continue and Restart.
   - Pinned by `leavingAResumedOldStampGameKeepsItsStampSoALaterRestartStillRegenerates`. Tech-lead to confirm.
3. **C3: the level load reads the active level, not the route's.** A regenerating restart must load the stamped level, and WR-03's cold start routes with a stale level 1. On a fresh entry and on the resume fallback the two are equal.
4. **C3: one accessibility cost on the regenerating path.** The banner's TalkBack focus move after Restart is withdrawn there: the loading state takes the banner down before the restart ordinal moves. The spoken "Board restarted. N pegs left." still counts the level's pegs. Only the first restart of a resumed old-stamp level takes this path.
5. **C3: `scoreFor`'s exhaustive `when`.** A `NotCleared` Challenge finish shares the Classic arm only for exhaustiveness: only a Won board reaches `scoreFor`, and a Won Challenge board is cleared by construction. No `!!` was needed.
6. **C7: `RescueBanner` lives in `NoWinBanner.kt`**, because `GameScreen.kt` is at detekt's per-file function limit. `NoWinBanner`'s message and actions became private leaves to stay inside `LongMethod`. No suppression; behaviour unchanged.
7. **C4: `results_headline_cleared` landed in C4, not C7**, because `CLEARED` cannot compile without its headline. The copy is verbatim from ux-spec §8.
8. **C4: the solved test exists once.** `ResultsViewModel` builds the state, then ranks it from that state's `isSolved`.
   - `grep -rn -e 'ChallengeClearRule' -e '<= minimumPegs' app/src/main` (comments aside) finds:
     - `ResultsUiState.kt:113-115` (`isSolved`) and `:135` (`nextStarBand`);
     - `GameViewModel.kt:1044` (`scoreFor`'s stars) and `:1811` (`statusFor`);
     - `NoWinBanner.kt:196` (`CLEAR_MAX_PEGS` as a format argument).
   - Since the C5 revert, `ProgressRepository` no longer reads the rule.
9. **C7: a `:domain` addition the brief did not list**: `ChallengeClearRule.nextBandUp(pegs) -> ChallengeBand`. The ux-spec requires both of the next-star line's numbers from "the one `:domain` clear function" (GD-SB-14, GD-SB-20). It reaches the screen as `ResultsUiState.nextStarBand`, derived rather than a constructor parameter, so no route or UI test changed shape. Tech-lead to confirm the name.
10. **C7: Challenge banner precedence.** A Hint asked on an already-stuck Challenge board raises both flags. The stuck message wins, because "you may still clear the level" is false on a dead board, and that banner is never dismissable. Classic keeps its shipped precedence. ux-designer to confirm.
11. **C7: not built — 30g's stored Roborazzi goldens.** The project has no Roborazzi, and tech-design §8.5 keeps stored goldens out of this codebase. The fit at 360×640dp at 1.0 and 2.0 is QA's 200 % device pass, 30g's own fallback. 30n's greyscale read is QA's too.
12. **Commit trailers.** I followed the dispatch and the harness, which ask for `Co-Authored-By: Claude Opus 5.5` and `Claude-Session:` on every commit, as this branch's history has. The plugin 1.0.0 rules/gitflow.md (newer than this product's 0.6.2 snapshot) prescribes exactly one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, and no `Claude-Session:` line. This repo has no prepare-commit-msg hook to normalise them. The producer should rule.
13. **The re-scope's shape: one clean revert commit, then the shift, each green.**
    - 6b335d4 reverts only what gdd §14 marks [3.0.1]. It keeps the offset at 50, so it regenerates the fixtures for that intermediate contract (levels 1, 10, 100, 1000), which never ships.
    - a26b9e8 sets 30, test-first, and regenerates again.
    - The type is `revert:` with a `Refs:` footer, Conventional Commits' recommendation for a revert; it is not among rules/gitflow.md's examples.
    - Nothing was rewritten: 629abac, 92b7f7d and 39b9e6a stay in the history.
14. **`ChallengeClearRule.PERFECT_STARS` (C1, brief-specified) has no production reader in 3.0.0** now that C5 is reverted. It is kept: `clearMaxPegsEqualsThePerfectStarCountSoTheBandIsOneStarPerPeg` pins it, and it is the binding the 3.0.1 DAO predicate uses.
    - 6b335d4 dropped its KDoc's claims about the skill achievements and about a generator reader.
    - It also fixed C1's "a thirty-one-peg level 300", which was wrong at either offset (level 300 deals 17 pegs; 31 is the cap), to "level 1,000".

## Findings for the producer
- **The `game_over_*` strings (ux-spec §8, GDD §16).** Nothing in `app/src` or `domain/src` (main and test, code and XML) references these six outside `values/strings.xml` (`strings.xml:17-22`): `game_over_perfect_headline`, `game_over_perfect_body`, `game_over_near_headline`, `game_over_stuck_headline`, `play_again`, `view_board`. They are verified orphaned and **not deleted**, as asked. The Turkish branch's G-6 criterion should be read against this.
- **30m's grep.**
  - `results_headline_cleared` and `results_next_star_hint` are referenced only from `ui/results/ResultsScreen.kt`.
  - `stuck_message_challenge` and `nowin_message_challenge` are referenced only from `ui/game/NoWinBanner.kt` (`RescueBanner`, which `GameScreen` composes). The criterion's "`GameScreen.kt`" therefore reads `NoWinBanner.kt` (deviation 6).
  - No tutorial string changed.
- **SB-Q6's sequence for QA (gdd §15: "the developer supplies a sequence"), on the 3.0.0 level 1.**
  - Board, (row, col) 0-based from the top-left of the 7×7 grid: on a fresh install, level 1 deals five pegs on the English cross at (4,0), (4,1), (4,2), (5,3) and (5,4).
  - **One jump: (4,1) over (4,2) into (4,3).** Four pegs remain, the board is still playing, and the peg at (4,0) can no longer be reached, so a Hint answers Proven dead and raises the Challenge proven-dead banner.
  - Tap its message: the banner leaves. Hint again: it returns.
  - Then finish. Every finish from there leaves 3 pegs, so this one sequence also covers SB-Q1 ("Cleared!", one star, "Leave 2 pegs for 2 stars.", Next level).
  - How it was found: a breadth-first search over the committed fixture, in a scratch model whose replay of the fixture's recorded line ends at one peg. QA confirms the Proven-dead answer on the device; the model is not the app's solver.
- **SB-Q5 and the 3.0.0 onboarding deviation (GD-SB-21, gdd §7.4), measured for the game-designer.** I enumerated every legal line on levels 1–20 of a26b9e8.
  - Five can be finished uncleared, each only by a one-jump dead end (four pegs, no move):
    - level 3: 2 of its 3 openings, (4,4) over (5,4) into (6,4), and (5,4) over (5,3) into (5,2);
    - level 5: (4,2) over (3,2) into (2,2);
    - level 6: 2 of 4;
    - level 10: 1 of 3;
    - level 13: 2 of 5.
  - The other fifteen, levels 1, 2 and 4 among them, cannot be finished uncleared at all.
  - So level 1 has no one-jump dead end for SB-Q5; level 3 is the first level where one exists.
  - The facts differ from §7.4's "the exposure is the same on each". The text is the game-designer's to amend; I make no judgement.
  - How: the boards came from a temporary golden-sample run, deleted afterwards (the tree was verified clean), with the same scratch model.
- **The tech-lead's brief predates Q-040**, as the game-designer's Amendment 2 notes. I built to gdd §14/§17 as amended; re-briefing is the tech-lead's, routed by the producer.

## Open issues / risks
1. **The depth sweep and the solver-deadline tests are timing measurements on a shared host** (tech-design §8.6). If a reviewer's re-run reds on `:domain:depthSweep` or a deadline test, read the 1-minute load first. This change touched neither the solver nor a budget constant.
2. **M1's quiet window was quiet only by the 1-minute load**: 4.1–4.6 at each start, with 5-minute loads of 6.0–6.5 and 15-minute loads of 7.3–7.5. The trees alternated in one window, so the comparison is fair; the absolute figures are not R-2's.
3. **The old-stamp restart path** (deviations 2–4) is exercised only by pre-release testers' version-1 snapshots, once each. SB-Q9 is its device check.
4. **The 30g/30l fit at 200 %** rests on QA's device pass (deviation 11).
5. **3.0.0 ships the recorded deviation**: levels 3, 5, 6, 10 and 13 can be finished uncleared by one wrong jump (findings). The founder approved it in Q-040, and G6 closes it in 3.0.1.
6. **Pre-existing, not from this change:** `generate(n)` still replays the chain from level 1 (tech-design §15, §16 #41). The `@Suppress` annotations already on `GameViewModel`, `SnapshotMapper` and `GameScreen` are untouched. No new suppression, baseline, `@Ignore` or production `!!` was added.

## Handoff
Next:
- **code-auditor** — the diff `80b8e5a..a26b9e8`: nine work commits plus the three doc commits.
  - Review it against `docs/gdd.md` as amended by 86dc35f (§14–§15 and the [3.0.1] marks, §17 step 2) and the brief as narrowed by Q-040.
  - Confirm that `git diff 80b8e5a a26b9e8 --` is empty for each file listed under 6b335d4 above.
  - Read the bodies of 6b335d4 (what was reverted) and C4 (the deleted test and its replacements).
  - Check the two-site rule with deviation 8's grep, and rule on deviations 2, 3, 5, 6, 8, 9, 10, 12, 13 and 14.
- **qa-engineer** — SB-Q1…Q11 on `ergoz_studio` (a phone if attached), per gdd §15 as amended:
  - SB-Q6 with the sequence above, which also gives SB-Q1's one-star clear;
  - SB-Q5 (informational) on level 1, where none exists, and on level 3's two dead ends if a dead end should be seen;
  - SB-Q8 records that a one-star hint-free play unlocks the award in 3.0.0;
  - SB-Q9 needs the pre-change build (develop `37d3052`) installed first, then this branch's;
  - M4's two load times;
  - the device halves of 30d, 30g, 30k, 30l and 30n;
  - the I5 line in `docs/qa-plan.md`'s fuzz invariants (the harness asserts it on every level).
- **tech-lead** (sign-off, a fresh agent) — M1 and M5 for tech-design §7.3 and §16 #40; deviations 2, 3, 9, 11 and 13; the re-brief for Q-040.
- **producer** — the trailer ruling (deviation 12); the exposure facts to the game-designer (findings); the `game_over_*` result for the Turkish branch.
