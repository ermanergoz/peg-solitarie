package com.app.pegsolitaire.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.FakePlayGamesOptInRepository
import com.app.pegsolitaire.data.FakeProgressSubmitter
import com.app.pegsolitaire.data.HomeProgress
import com.app.pegsolitaire.data.ImportOutcome
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesService
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Coverage of [HomeViewModel]'s onboarding gate (ONB-01, D-13/D-14/D-15) and the
 * persisted Challenge level it surfaces to the Challenge tile (CHAL-05 / PERS-02).
 *
 * A fake [OnboardingRepository] backed by a settable `MutableStateFlow<Boolean>`
 * stands in for the DataStore so the gate is driven deterministically:
 *  - on first launch (`onboardingSeen == false`) the overlay is visible (D-14);
 *  - once seen it is hidden;
 *  - the help affordance forces it visible again independent of the flag (D-15);
 *  - dismissing persists `markSeen()` and hides it (D-13).
 *
 * A [ChallengeProgress] stand-in drives `currentLevel` the same way, proving the tile's
 * entry level comes from persistence (never a hardcoded 1) and re-emits on an advance.
 *
 * A [FakeResumeProgressRepository] drives the per-mode resume snapshots (PERS-03), proving
 * Home offers a Continue affordance exactly when one exists — for either mode, for both at
 * once (D-03), dropped again when a solve clears it (T-05-12), and silently absent when the
 * stored blob is unreadable (T-05-03). Its Classic snapshots are written by the real codec
 * ([SnapshotMapper.toDto]) and read back by the real one, so the Classic chip's board is the
 * position the stored moves leave (D-03p, criterion 15aa), and a history that does not replay
 * is withheld and purged (the tech-lead's ruling 1).
 *
 * `Dispatchers.setMain(StandardTestDispatcher())` makes the `viewModelScope`
 * collector deterministic under `runTest` virtual time.
 */
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theTutorialIsPendingOnFirstLaunchWhenItHasNotBeenSeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isTrue()
        }

    @Test
    fun theTutorialIsNotPendingOnceItHasBeenSeen() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = viewModelFor(repository)
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isFalse()
        }

    /**
     * The flag stops standing the moment the **tutorial's own** write lands (D-01g/D-02b).
     *
     * This is the retarget of `dismissOnboardingPersistsSeenAndHidesTheOverlay`: the write is
     * `TutorialViewModel`'s now, not Home's, so the test drives the repository the way that
     * ViewModel does and asserts Home follows. It matters because `tutorialPending` is a pure
     * mirror of `!seen` — Home has no writer of its own left to set it false locally, and if
     * the mirror did not follow the store, Home would re-push the tutorial forever.
     */
    @Test
    fun theTutorialStopsBeingPendingWhenTheSeenFlagIsPersistedByTheTutorial() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(repository)
            runCurrent()
            assertThat(viewModel.uiState.value.tutorialPending).isTrue()

            repository.markSeen()
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isFalse()
        }

    /**
     * Offered once, and never again from Home (D-02/D-02a/D-02b).
     *
     * The retarget of `showHowToPlayForcesTheOverlayVisibleEvenAfterItHasBeenSeen`, whose
     * subject — the permanent Home affordance that forced the overlay open at any time — is
     * deleted by D-02. What replaces it is the property that deletion buys: the persisted flag
     * is the **only** writer, so no re-emission and no Home interaction can re-raise the
     * tutorial once it is seen. Settings › How to play is the one later way in, and it
     * navigates rather than flipping this flag.
     */
    @Test
    fun theTutorialIsOfferedOnceAndNeverAgainOnceTheFlagIsPersisted() =
        runTest(testDispatcher) {
            val repository = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(repository)
            runCurrent()
            assertThat(viewModel.uiState.value.tutorialPending).isTrue()

            repository.markSeen()
            runCurrent()
            assertThat(viewModel.uiState.value.tutorialPending).isFalse()

            // An unrelated store write re-emits the unchanged flag: still not pending.
            repository.seen.value = true
            runCurrent()
            assertThat(viewModel.uiState.value.tutorialPending).isFalse()
            assertThat(repository.markSeenCount).isEqualTo(1)
        }

    // ---- The Home progress ribbon (§7 R-1/R-2, D-03c) ----------------------

    @Test
    fun theRibbonValuesAreNullUntilTheFirstProgressEmission() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)

            viewModel.uiState.test {
                // D-03c as an assertion rather than a paragraph: the pills paint "—"
                // before the database has answered, and Home never waits on the read.
                val initial = awaitItem()
                assertThat(initial.totalStars).isNull()
                assertThat(initial.classicBoardsCleared).isNull()

                runCurrent()
                progress.emitHomeProgress(HomeProgress(totalStars = RIBBON_STARS, classicBoardsCleared = RIBBON_BOARDS))
                runCurrent()

                val filled = expectMostRecentItem()
                assertThat(filled.totalStars).isEqualTo(RIBBON_STARS)
                assertThat(filled.classicBoardsCleared).isEqualTo(RIBBON_BOARDS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Persisted Challenge level (CHAL-05 / PERS-02) ----------------------

    @Test
    fun challengeCurrentLevelMirrorsThePersistedLevel() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = PERSISTED_LEVEL,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(PERSISTED_LEVEL)
        }

    @Test
    fun challengeCurrentLevelDefaultsToLevelOneBeforeTheStoreEmits() =
        runTest(testDispatcher) {
            // Nothing collected yet (no runCurrent): the tile must already paint a usable
            // level rather than 0/blank — the fail-open default (T-05-02).
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = PERSISTED_LEVEL,
                )

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(FIRST_LEVEL)
        }

    @Test
    fun advancingTheStoredLevelWhileOnHomeReEmitsTheNewLevel() =
        runTest(testDispatcher) {
            val levels = MutableStateFlow(FIRST_LEVEL)
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), levels = levels)

            viewModel.uiState.test {
                assertThat(awaitItem().challengeCurrentLevel).isEqualTo(FIRST_LEVEL)

                // A solve advanced the persisted level while Home was in the back stack.
                levels.value = PERSISTED_LEVEL
                runCurrent()

                assertThat(awaitItem().challengeCurrentLevel).isEqualTo(PERSISTED_LEVEL)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Helpers -------------------------------------------------------------

    // ---- Resume affordances (PERS-03, D-01/D-03) -----------------------------

    @Test
    fun noContinueAffordanceIsOfferedWhenNothingIsSaved() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true))
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isNull()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun aClassicSnapshotOffersAContinueCarryingItsBoardId() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isTrue()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isEqualTo(RESUMABLE_LEVEL_ID)
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun bothModesCanOfferAContinueAtTheSameTime() =
        runTest(testDispatcher) {
            // D-03: one snapshot PER MODE — leaving a Classic game to play Challenge keeps
            // both, so Home shows two independent Continue affordances.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isTrue()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isTrue()
        }

    @Test
    fun clearingASnapshotWhileOnHomeDropsItsContinueAffordance() =
        runTest(testDispatcher) {
            // A solve clears the mode's snapshot (D-05); Home must stop offering it rather
            // than leaving a stale Continue pointing at a finished board (T-05-12).
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()
            assertThat(viewModel.uiState.value.challengeResumeAvailable).isTrue()

            progress.clear(ProgressPersistenceKeys.MODE_CHALLENGE)
            runCurrent()

            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun anUnreadableClassicSnapshotSimplyOffersNoContinue() =
        runTest(testDispatcher) {
            // T-05-03: the mode is listed but its blob does not decode — the affordance
            // disappears; nothing throws and no error surfaces to the player.
            val progress = FakeResumeProgressRepository()
            progress.putUnreadable(ProgressPersistenceKeys.MODE_CLASSIC)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
        }

    @Test
    fun aChallengeSnapshotOffersAContinueAtItsStampedLevelNotTheColdStartLadder() =
        runTest(testDispatcher) {
            // WR-03: at a cold start the ladder flow still sits at its fail-open initial
            // value while the snapshot's stamp names the real level — the tile must carry
            // the stamp, or the restored level-N board gets played AS level 1.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            val viewModel =
                viewModelFor(
                    FakeOnboardingRepository(initiallySeen = true),
                    persistedLevel = FIRST_LEVEL,
                    progress = progress,
                )
            runCurrent()

            assertThat(viewModel.uiState.value.challengeCurrentLevel).isEqualTo(FIRST_LEVEL)
            assertThat(viewModel.uiState.value.challengeResumeLevel).isEqualTo(PERSISTED_LEVEL)
        }

    @Test
    fun anUnreadableChallengeSnapshotOffersNoContinue() =
        runTest(testDispatcher) {
            // WR-02: the Challenge mode lists a row but its blob does not decode — keying
            // the tile on row existence alone would show a phantom "Continue Challenge"
            // that forever starts a fresh board. The tile needs a decodable blob.
            val progress = FakeResumeProgressRepository()
            progress.putUnreadable(ProgressPersistenceKeys.MODE_CHALLENGE)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.challengeResumeAvailable).isFalse()
        }

    @Test
    fun aClassicSnapshotOnAnUnknownBoardOffersNoContinueAndPurgesTheRow() =
        runTest(testDispatcher) {
            // CR-01: the blob decodes but its levelId is not a catalog board (tampering,
            // or an app update removed the board). Offering it would navigate into the
            // Gameplay route's fail-fast catalog lookup — an app crash on Continue — so
            // the affordance is withheld and the stale row purged.
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, UNKNOWN_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isNull()
            assertThat(progress.clears).containsExactly(ProgressPersistenceKeys.MODE_CLASSIC)
        }

    @Test
    fun aClassicSnapshotOnAKnownBoardIsNeverPurged() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeLevelId).isEqualTo(RESUMABLE_LEVEL_ID)
            assertThat(progress.clears).isEmpty()
        }

    // ---- The Classic chip shows the saved position (D-03p, criterion 15aa) ------

    /**
     * Criterion 15aa part 1: the Classic Continue chip's board is the position the player left — the
     * stored initial board with the stored moves replayed — and never the catalogue's start board,
     * which is what the chip drew until D-03p.
     *
     * Each count of moves is a fresh save of the European-37 board through the real codec, with the
     * first legal jump taken each time. The board the state carries is the one the resume path itself
     * rebuilds from that save ([SnapshotMapper.fromDto], the codec `GameViewModel` resumes with), it
     * has one peg fewer per jump, and from the first jump on it is not the start board. With no move
     * played the saved position *is* the start board, which is the one case the old chip drew right.
     */
    @Test
    fun aClassicContinueCarriesThePositionTheStoredMovesLeaveAndNotTheStartBoard() =
        runTest(testDispatcher) {
            PEGS_LEFT_AFTER_MOVES.forEach { (played, pegsLeft) ->
                val progress = FakeResumeProgressRepository()
                progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(played))
                val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
                runCurrent()

                val shown = viewModel.uiState.value.classicResumeBoard
                val resumed =
                    SnapshotMapper.fromDto(progress.stored(ProgressPersistenceKeys.MODE_CLASSIC), RulesEngine)
                assertWithMessage("$played moves played: the position the chip carries, against the resume path's")
                    .that(shown?.position())
                    .isEqualTo(checkNotNull(resumed).currentBoard.position())
                assertWithMessage("$played moves played: pegs left on it").that(shown?.pegCount).isEqualTo(pegsLeft)
                if (played == 0) {
                    assertWithMessage("no move played: the start board")
                        .that(shown?.position())
                        .isEqualTo(Boards.european37Initial.position())
                } else {
                    assertWithMessage("$played moves played: not the start board")
                        .that(shown?.position())
                        .isNotEqualTo(Boards.european37Initial.position())
                }
            }
        }

    /**
     * Criterion 15aa part 1, against a hand-checked position: a recorded line of 18 legal jumps from
     * the English-33 start — the line `HomeStagePositionTest` proved for the withdrawn stage (criterion
     * 15q, whose replay idea 15aa part 1 keeps) — saved and read back, is shown as exactly the 14 pegs
     * it leaves, written out here peg by peg rather than computed.
     */
    @Test
    fun aRecordedLineOfLegalJumpsIsShownAsExactlyThePositionItLeaves() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, ClassicLevelIds.ENGLISH33, recordedEnglishLine())
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.classicResumeLevelId).isEqualTo(ClassicLevelIds.ENGLISH33)
            assertThat(state.classicResumeBoard?.position()).isEqualTo(RECORDED_LINE_POSITION.position())
            assertThat(state.classicResumeBoard?.pegCount).isEqualTo(RECORDED_LINE_PEGS_LEFT)
        }

    /**
     * Criterion 15aa part 1's last clause: the thumbnail follows the save. A later save of the same
     * board — the player went back in, played on and left again — re-emits the resume modes as Room's
     * query does on every write, and the chip's board becomes the new position.
     */
    @Test
    fun aLaterSaveOfTheSameBoardIsShownAtItsNewPosition() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(FIRST_SAVE_MOVES))
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()
            val firstSave = viewModel.uiState.value.classicResumeBoard
            assertThat(firstSave?.pegCount).isEqualTo(PEGS_LEFT_AFTER_FIRST_SAVE)

            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(LATER_SAVE_MOVES))
            runCurrent()

            val state = viewModel.uiState.value
            val resumed = SnapshotMapper.fromDto(progress.stored(ProgressPersistenceKeys.MODE_CLASSIC), RulesEngine)
            assertThat(state.classicResumeLevelId).isEqualTo(RESUMABLE_LEVEL_ID)
            assertThat(state.classicResumeBoard?.pegCount).isEqualTo(PEGS_LEFT_AFTER_LATER_SAVE)
            assertThat(state.classicResumeBoard?.position()).isEqualTo(checkNotNull(resumed).currentBoard.position())
            assertThat(state.classicResumeBoard?.position()).isNotEqualTo(firstSave?.position())
        }

    /**
     * Criterion 15aa part 2, as the tech-lead's ruling 1 amends it: a Classic snapshot that decodes
     * and names a catalogue board but whose moves do not replay — one forged tuple, as
     * `GameViewModelResumeTest` forges it — offers **no** Continue, and its row is purged, exactly once,
     * as CR-01 purges an unknown id and as the resume path itself purges it on the tap (WR-02). A chip
     * for it would promise a saved game and start a fresh board. The Challenge chip beside it is
     * untouched.
     */
    @Test
    fun aClassicSnapshotWhoseMovesDoNotReplayOffersNoContinueAndPurgesTheRow() =
        runTest(testDispatcher) {
            val progress = FakeResumeProgressRepository()
            progress.put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(FIRST_SAVE_MOVES))
            progress.put(ProgressPersistenceKeys.MODE_CHALLENGE, PERSISTED_LEVEL.toString())
            progress.tamperWithMoves(ProgressPersistenceKeys.MODE_CLASSIC)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
            runCurrent()

            assertThat(viewModel.uiState.value.classicResumeAvailable).isFalse()
            assertThat(viewModel.uiState.value.classicResumeLevelId).isNull()
            assertThat(viewModel.uiState.value.classicResumeBoard).isNull()
            assertThat(progress.clears).containsExactly(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(viewModel.uiState.value.challengeResumeLevel).isEqualTo(PERSISTED_LEVEL)
        }

    /**
     * The invariant ruling 1 rests on (the tech-lead's brief, C1 (c)): the state never carries a
     * Classic board id without its board, nor a board without its id — so the Classic chip never shows
     * the play arrow in place of a position it could not rebuild. Read over every shape a stored
     * Classic snapshot can take: absent, unreadable, on an unknown board, with a forged history, and
     * whole.
     */
    @Test
    fun theStateCarriesAClassicBoardIdExactlyWhenItCarriesItsBoard() =
        runTest(testDispatcher) {
            CLASSIC_SNAPSHOT_SHAPES.forEach { (shape, store) ->
                val progress = FakeResumeProgressRepository().apply(store)
                val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), progress = progress)
                runCurrent()

                val state = viewModel.uiState.value
                assertWithMessage("$shape: an id with no board, or a board with no id")
                    .that(state.classicResumeLevelId == null)
                    .isEqualTo(state.classicResumeBoard == null)
            }
        }

    // ---- Play Games opt-in prompt (PGS-01, D-15/D-17/D-18) --------------------

    @Test
    fun theOptInPromptStaysHiddenWhileTheTutorialIsStillPending() =
        runTest(testDispatcher) {
            // D-15: the two first-run surfaces never stack — the tutorial goes first.
            val optIn = FakePlayGamesOptInRepository()
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = false), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isTrue()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    @Test
    fun theOptInPromptShowsOnceOnboardingIsSeenAndTheQuestionIsStillUnanswered() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository()
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()
        }

    @Test
    fun finishingTheTutorialHandsOverToTheOptInPrompt() =
        runTest(testDispatcher) {
            // The real first-run sequence: the tutorial first, then the prompt takes its
            // place. The handover is the PERSISTED flag (D-01f), which is why the tutorial
            // awaits its write before it pops — a dropped write would withhold this prompt
            // for an extra launch.
            val optIn = FakePlayGamesOptInRepository()
            val onboarding = FakeOnboardingRepository(initiallySeen = false)
            val viewModel = viewModelFor(onboarding, optIn = optIn)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()

            onboarding.markSeen()
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isFalse()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()
        }

    @Test
    fun theOptInPromptNeverShowsAgainOnceAnswered() =
        runTest(testDispatcher) {
            // D-17 / T-11-08: "Not now" retires the prompt permanently — no nag on a later
            // launch, which is what keeps this out of dark-pattern territory.
            val optIn = FakePlayGamesOptInRepository(initiallyAnswered = true)
            val viewModel = viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn)
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    /**
     * The retarget of `theOptInPromptStaysRetiredEvenWhenTheHelpAffordanceReopensOnboarding`.
     *
     * That test staged the second writer D-02b deletes — the Home affordance that forced the
     * overlay open — so its premise is gone. What survives, and is the property that actually
     * mattered, is that an unrelated re-emission of the two already-answered flags cannot
     * resurrect the retired prompt (WR-07).
     */
    @Test
    fun theOptInPromptStaysRetiredAcrossFurtherFlagEmissions() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository(initiallyAnswered = true)
            val onboarding = FakeOnboardingRepository(initiallySeen = true)
            val viewModel = viewModelFor(onboarding, optIn = optIn)
            runCurrent()

            // A genuine re-emission in both directions — the fail-open shape a corrupt read
            // then a recovered one produces. Neither swing may resurrect an answered prompt.
            onboarding.seen.value = false
            runCurrent()
            assertThat(viewModel.uiState.value.tutorialPending).isTrue()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()

            onboarding.seen.value = true
            runCurrent()

            assertThat(viewModel.uiState.value.tutorialPending).isFalse()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
        }

    @Test
    fun acceptingHidesThePromptPersistsTheOptInAndSignsIn() =
        runTest(testDispatcher) {
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(optIn.setOptedInCount).isEqualTo(1)
            assertThat(games.signInCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun acceptingHidesThePromptBeforeTheSignInRoundTripEvenBegins() =
        runTest(testDispatcher) {
            // T-11-09: the dialog must never sit on screen waiting for the SDK — no wall,
            // no pause. Asserted with no runCurrent(), so nothing the launch does has run.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(games.signInCallCount).isEqualTo(0)
        }

    @Test
    fun aGenuineSignInErrorRaisesTheErrorTick() =
        runTest(testDispatcher) {
            // D-18: only a throw — an SDK-reported failure — surfaces the Home snackbar.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService().apply { signInBehavior = FakeGamesService.SignInBehavior.Error() }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(1)
            // The opt-in still stands, so the next launch retries silently (D-18).
            assertThat(optIn.optedInState.value).isTrue()
        }

    @Test
    fun theScreenConsumesAShownSignInErrorSoItCannotReplay() =
        runTest(testDispatcher) {
            // CR-01: this ViewModel outlives every visit to Home, so a shown error must be
            // handed back and reset — otherwise every later return to Home replays it.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService().apply { signInBehavior = FakeGamesService.SignInBehavior.Error() }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()
            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(1)

            viewModel.onPlayGamesSignInErrorShown()

            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun aCancelledSignInRoundTripNeverRaisesTheErrorTick() =
        runTest(testDispatcher) {
            // WR-04: cancellation is this scope being cleared, not an SDK error — the
            // runCatching around signIn() must not convert it into a snackbar.
            val optIn = FakePlayGamesOptInRepository()
            val games =
                FakeGamesService().apply {
                    signInBehavior = FakeGamesService.SignInBehavior.Error(CancellationException("cleared"))
                }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(games.signInCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun aPlainChooserDismissalStaysSilent() =
        runTest(testDispatcher) {
            // D-18 / D-10 parity: signIn() returning false is a player-initiated cancel,
            // not an error — no snackbar, nothing to apologise for.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService().apply { signInBehavior = FakeGamesService.SignInBehavior.Decline }
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()

            viewModel.onAcceptPlayGamesPrompt()
            runCurrent()

            assertThat(games.signInCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    @Test
    fun decliningHidesThePromptPersistsNotNowAndNeverTouchesTheSdk() =
        runTest(testDispatcher) {
            // D-16/D-17: declining leaves the game exactly as it was — the only change in
            // the world is the pair of persisted flags.
            val optIn = FakePlayGamesOptInRepository()
            val games = FakeGamesService()
            val viewModel =
                viewModelFor(FakeOnboardingRepository(initiallySeen = true), optIn = optIn, games = games)
            runCurrent()
            assertThat(viewModel.uiState.value.playGamesPromptVisible).isTrue()

            viewModel.onDeclinePlayGamesPrompt()
            runCurrent()

            assertThat(viewModel.uiState.value.playGamesPromptVisible).isFalse()
            assertThat(optIn.setNotNowCount).isEqualTo(1)
            assertThat(optIn.setOptedInCount).isEqualTo(0)
            assertThat(games.signInCallCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.playGamesSignInErrorTick).isEqualTo(0)
        }

    // ---- Helpers -------------------------------------------------------------

    /**
     * Builds the ViewModel over the given onboarding fake, a [ChallengeProgress]
     * stand-in whose `currentLevel` is driven by [levels] (defaulting to a fixed
     * [persistedLevel]), a resume store [progress] (empty unless a test fills it), and a
     * catalog that knows exactly the real Classic ids — neither real store is touched.
     *
     * [optIn] defaults to an ALREADY-ANSWERED prompt so the pre-Play-Games tests above
     * (onboarding, ladder level, resume affordances) keep asserting the surface they were
     * written for — an unanswered default would silently raise the opt-in dialog under
     * every one of them. The submitter defaults to one with **no** pending account-switch
     * question, for the same reason: Phase 13's dialog must not appear under assertions
     * about anything else. Its own coverage is `HomeViewModelCloudTest`.
     *
     * The rules engine is the shipped one, and the replay's dispatcher is this class's test
     * dispatcher, so [runCurrent] drives the rebuild's hop exactly as it drives the collectors.
     */
    private fun viewModelFor(
        repository: OnboardingRepository,
        persistedLevel: Int = FIRST_LEVEL,
        levels: MutableStateFlow<Int> = MutableStateFlow(persistedLevel),
        progress: ProgressRepository = FakeResumeProgressRepository(),
        optIn: PlayGamesOptInRepository = FakePlayGamesOptInRepository(initiallyAnswered = true),
        games: GamesService = FakeGamesService(),
        submitter: ProgressSubmitter = FakeProgressSubmitter(),
    ): HomeViewModel {
        val challengeProgress = mockk<ChallengeProgress>(relaxed = true)
        every { challengeProgress.currentLevel } returns levels
        return HomeViewModel(
            repository,
            challengeProgress,
            progress,
            CatalogOnlyLevelRepository,
            RulesEngine,
            testDispatcher,
            optIn,
            games,
            submitter,
        )
    }
}

/** The Classic board a saved game sits on — a real catalog id, carried into the resume. */
private val RESUMABLE_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** A levelId no catalog board has ever had — the CR-01 stale/tampered snapshot. */
private const val UNKNOWN_LEVEL_ID = "ghost-board"

/**
 * Every Classic id over its own start board, as the shipped catalogue serves them
 * (`DefaultClassicLevelSource`): the board a save of that id starts from.
 */
private val CATALOGUE_BOARDS: Map<String, Board> =
    mapOf(
        ClassicLevelIds.ENGLISH33 to Boards.english33Initial,
        ClassicLevelIds.EUROPEAN37 to Boards.european37Initial,
        ClassicLevelIds.ASYMMETRIC39 to Boards.asymmetric39Initial,
        ClassicLevelIds.DIAMOND41 to Boards.diamond41Initial,
        ClassicLevelIds.WIEGLEB45 to Boards.wiegleb45Initial,
    )

/**
 * A [LevelRepository] that answers only the catalog question Home asks (CR-01): the real
 * Classic ids, each over its own start board. The load paths are never taken from Home.
 */
private object CatalogOnlyLevelRepository : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> =
        CATALOGUE_BOARDS.map { (id, board) ->
            ClassicLevelDescriptor(
                id = id,
                displayName = id,
                holeCount = board.spec.inPlay.size,
                previewBoard = board,
            )
        }

    override fun loadClassic(levelId: String): Level = error("Home never loads a level")

    override fun loadChallenge(levelNumber: Int): Level = error("Home never loads a level")
}

/**
 * A board as the position it shows: its footprint and its pegs. `BoardSpec` declares no `equals`
 * and the codec builds a fresh spec for every board it rebuilds, so two boards are the same
 * position when these are equal — "always compare footprints" (`BoardSpec.footprint`).
 */
private fun Board.position(): Pair<List<Position>, Long> = spec.footprint to pegs

/**
 * The first legal jump, [count] times over, from the European-37 start: a line any player could
 * have played, written by nobody's hand.
 */
private fun firstLegalMoves(count: Int): List<Move> {
    var board = Boards.european37Initial
    return List(count) {
        RulesEngine.legalMoves(board).first().also { move -> board = RulesEngine.applyMove(board, move) }
    }
}

/**
 * The recorded English-33 line, each jump found among the legal moves where it is played, so the
 * line is proved legal as it is built (the way `HomeStagePositionTest` proved it).
 */
private fun recordedEnglishLine(): List<Move> {
    var board = Boards.english33Initial
    return RECORDED_ENGLISH_LINE.map { (from, to) ->
        checkNotNull(RulesEngine.legalMoves(board).firstOrNull { it.from == from && it.to == to }) {
            "the recorded jump $from to $to is not legal where it is played"
        }.also { move -> board = RulesEngine.applyMove(board, move) }
    }
}

/**
 * The first 18 jumps of a winning English-33 line from the standard start (centre empty), as
 * `from to` pairs; the jumped peg is implied by the rules.
 */
private val RECORDED_ENGLISH_LINE: List<Pair<Position, Position>> =
    listOf(
        Position(5, 3) to Position(3, 3),
        Position(2, 3) to Position(4, 3),
        Position(2, 1) to Position(2, 3),
        Position(3, 5) to Position(3, 3),
        Position(4, 2) to Position(2, 2),
        Position(2, 3) to Position(2, 1),
        Position(1, 4) to Position(3, 4),
        Position(4, 4) to Position(2, 4),
        Position(0, 3) to Position(2, 3),
        Position(3, 0) to Position(3, 2),
        Position(3, 3) to Position(1, 3),
        Position(4, 0) to Position(4, 2),
        Position(0, 2) to Position(2, 2),
        Position(4, 3) to Position(4, 1),
        Position(6, 2) to Position(4, 2),
        Position(6, 4) to Position(6, 2),
        Position(4, 6) to Position(4, 4),
        Position(3, 2) to Position(1, 2),
    )

/** Where [RECORDED_ENGLISH_LINE] leaves the English board: its 14 pegs, row by row, by hand. */
private val RECORDED_LINE_POSITION: Board =
    listOf(
        Position(0, 4),
        Position(1, 2),
        Position(1, 3),
        Position(2, 0),
        Position(2, 1),
        Position(2, 4),
        Position(2, 5),
        Position(2, 6),
        Position(3, 6),
        Position(4, 1),
        Position(4, 2),
        Position(4, 4),
        Position(5, 4),
        Position(6, 2),
    ).fold(Board(spec = Boards.English33, pegs = 0L)) { board, peg -> board.withPeg(peg) }

/** The recorded line's position counted: 32 pegs at the start, one fewer per jump, 18 jumps. */
private const val RECORDED_LINE_PEGS_LEFT = 14

/**
 * Moves played on a European-37 save against the pegs left: 36 at the start (37 holes, the
 * centre empty), one fewer per jump.
 */
private val PEGS_LEFT_AFTER_MOVES: List<Pair<Int, Int>> =
    listOf(
        0 to 36,
        1 to 35,
        4 to 32,
        9 to 27,
    )

/** The two saves of one European-37 game, and the pegs each leaves. */
private const val FIRST_SAVE_MOVES = 2
private const val PEGS_LEFT_AFTER_FIRST_SAVE = 34
private const val LATER_SAVE_MOVES = 7
private const val PEGS_LEFT_AFTER_LATER_SAVE = 29

/**
 * A move tuple no board can play, nine cells off the edge: the forged history
 * `GameViewModelResumeTest` writes (T-05-04), seen from Home.
 */
private val FORGED_MOVE_TUPLE = intArrayOf(9, 9, 9, 9, 9, 9)

/** Every shape a stored Classic snapshot can take, each staged on a fresh store. */
private val CLASSIC_SNAPSHOT_SHAPES: List<Pair<String, FakeResumeProgressRepository.() -> Unit>> =
    listOf(
        "absent" to {},
        "unreadable" to { putUnreadable(ProgressPersistenceKeys.MODE_CLASSIC) },
        "on an unknown board" to { put(ProgressPersistenceKeys.MODE_CLASSIC, UNKNOWN_LEVEL_ID) },
        "with a forged history" to {
            put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(FIRST_SAVE_MOVES))
            tamperWithMoves(ProgressPersistenceKeys.MODE_CLASSIC)
        },
        "whole" to { put(ProgressPersistenceKeys.MODE_CLASSIC, RESUMABLE_LEVEL_ID, firstLegalMoves(FIRST_SAVE_MOVES)) },
    )

/**
 * A [ProgressRepository] holding just enough of the resume surface for Home: which modes
 * have a snapshot, and what each holds. A Classic snapshot is written by the real codec from its
 * board's start and the moves played since, exactly as `GameViewModel` writes one, so Home reads
 * back a blob the codec accepts. [putUnreadable] models a stored blob that fails to decode — the
 * repository's own fail-safe, seen from up here as a mode that lists but cannot be loaded — and
 * [tamperWithMoves] one that decodes but whose history does not replay.
 *
 * The mode list re-emits on every write, as Room's observing query does on every write to the
 * table, even when the list itself is unchanged: that is how a later save of the same board reaches
 * Home (criterion 15aa part 1).
 */
private class FakeResumeProgressRepository : ProgressRepository {
    private val snapshots = mutableMapOf<String, ResumeSnapshotDto?>()
    private val modes =
        MutableSharedFlow<List<String>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply {
            tryEmit(emptyList())
        }

    /** The modes Home purged as unusable (CR-01) — observable by the tests. */
    val clears = mutableListOf<String>()

    fun put(
        mode: String,
        levelId: String,
        moves: List<Move> = emptyList(),
    ) {
        snapshots[mode] = snapshotOf(mode = mode, levelId = levelId, moves = moves)
        publishModes()
    }

    /** A mode whose row exists but whose payload is unusable (`loadSnapshot` → null). */
    fun putUnreadable(mode: String) {
        snapshots[mode] = null
        publishModes()
    }

    /** Rewrites [mode]'s stored history to one move no board can play: it decodes, and does not replay. */
    fun tamperWithMoves(mode: String) {
        val stored = stored(mode)
        snapshots[mode] = stored.copy(moves = listOf(FORGED_MOVE_TUPLE.copyOf()))
        publishModes()
    }

    /** The snapshot stored for [mode], as Home loads it. */
    fun stored(mode: String): ResumeSnapshotDto =
        requireNotNull(snapshots[mode]) { "nothing readable stored for $mode" }

    fun clear(mode: String) {
        snapshots -= mode
        publishModes()
    }

    private fun publishModes() {
        modes.tryEmit(snapshots.keys.toList())
    }

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("Home never records a result")

    /** Unused by this screen — Home never reads the achievement aggregate. */
    override suspend fun aggregate(): ProgressAggregate = ProgressAggregate()

    /** Unused by this screen — no per-board leaderboard read happens here. */
    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    /**
     * The Home ribbon aggregate, settable so a test can drive the placeholder → value
     * transition. Starts at [HomeProgress.UNKNOWN]'s absence: nothing emitted yet.
     */
    private val homeProgress = MutableSharedFlow<HomeProgress>(replay = 1)

    fun emitHomeProgress(progress: HomeProgress) {
        homeProgress.tryEmit(progress)
    }

    override fun observeHomeProgress(): Flow<HomeProgress> = homeProgress

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("Home never saves a snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = snapshots[mode]

    override suspend fun clearSnapshot(mode: String) {
        clears += mode
        snapshots -= mode
        publishModes()
    }

    override fun observeResumeModes(): Flow<List<String>> = modes

    /** Unreachable from here — Home never calls cloud save (CLOUD-01/CLOUD-04). */
    override suspend fun exportProgress(ladder: Int): CloudProgress = error("Home never calls cloud save")

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("Home never calls cloud save")

    override suspend fun resetAllProgress() = error("Home never calls cloud save")

    /**
     * A Classic snapshot is the codec's own, from the board's start and [moves]; a board id the
     * catalogue does not know (CR-01) still stores a whole English board, so only its id is wrong.
     * Home reads nothing of a Challenge snapshot but its stamp, so that one stays a bare stamp.
     */
    private fun snapshotOf(
        mode: String,
        levelId: String,
        moves: List<Move>,
    ): ResumeSnapshotDto =
        if (mode == ProgressPersistenceKeys.MODE_CLASSIC) {
            SnapshotMapper.toDto(
                mode = mode,
                levelId = levelId,
                initialBoard = CATALOGUE_BOARDS[levelId] ?: Boards.english33Initial,
                moveHistory = moves,
                elapsedMs = 0L,
            )
        } else {
            ResumeSnapshotDto(
                mode = mode,
                levelId = levelId,
                // A Challenge snapshot always carries its provenance stamp (WR-03) — the
                // level the tile must label and resume into.
                challengeLevel = levelId.toIntOrNull(),
                inPlay = emptyList(),
                initialPegs = 0L,
                moves = emptyList(),
                elapsedMs = 0L,
            )
        }
}

/** The ribbon's star total once the aggregate answers — distinct from every other fixture number. */
private const val RIBBON_STARS = 41

/** The ribbon's cleared-board count once the aggregate answers. */
private const val RIBBON_BOARDS = 3

/** A persisted level well past the on-ramp, so "continues at the stored level" is unambiguous. */
private const val PERSISTED_LEVEL = 7

/** The fail-open Challenge entry level (a fresh install, or a corrupt read). */
private const val FIRST_LEVEL = 1

/**
 * A settable in-memory [OnboardingRepository]: overrides the DataStore-backed members
 * so [seen] drives the flag and [markSeen] is observable, with the base store never
 * touched (its constructor takes a no-op, never-emitting [DataStore]).
 */
private class FakeOnboardingRepository(
    initiallySeen: Boolean,
) : OnboardingRepository(NoopPreferencesDataStore) {
    val seen = MutableStateFlow(initiallySeen)
    var markSeenCount = 0
        private set

    override val onboardingSeen: Flow<Boolean> get() = seen

    override suspend fun markSeen() {
        markSeenCount++
        seen.value = true
    }
}

/** A never-emitting [DataStore] so the base [OnboardingRepository] constructor is safe in the fake. */
private object NoopPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        error("not used by the fake")
}
