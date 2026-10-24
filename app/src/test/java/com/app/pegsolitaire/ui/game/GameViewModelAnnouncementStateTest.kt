package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.TestTimeSource

/**
 * The two fields the board's announcements need that the state did not carry (the talkback-board
 * brief, §5): [GameUiState.restartTick], the restart's ordinal, and [GameUiState.undoneMove], the
 * text of the undo [GameUiState.undoTick] already marks.
 *
 * Every board-install site runs before a restart is possible — a restart does nothing until a board
 * is installed — so the ordinal's carry at those sites cannot be seen through the ViewModel; what can
 * be seen is that it starts at 0 after each kind of install and rises from there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ANNOUNCEMENT_STATE_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent: virtual-time control of the loads
class GameViewModelAnnouncementStateTest {
    private val testDispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private var viewModelKey = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    // ---- restartTick -------------------------------------------------------------------------

    @Test
    fun anImmediateRestartAdvancesTheRestartOrdinal() =
        runGame {
            val viewModel = classic()

            viewModel.onRestart()

            assertThat(viewModel.uiState.value.restartTick).isEqualTo(1)
        }

    @Test
    fun aConfirmedRestartAdvancesItOnceAndADeclinedOneNotAtAll() =
        runGame {
            val viewModel = classic()
            jumpDownIntoTheCentre(viewModel)

            viewModel.onRestart()
            viewModel.onDismissRestart()
            assertThat(viewModel.uiState.value.restartTick).isEqualTo(0)

            viewModel.onRestart()
            viewModel.onConfirmRestart()
            assertThat(viewModel.uiState.value.restartTick).isEqualTo(1)
        }

    @Test
    fun theRestartOrdinalRisesAcrossRestartsAndSurvivesTheMovesBetweenThem() =
        runGame {
            val viewModel = classic()

            viewModel.onRestart()
            jumpDownIntoTheCentre(viewModel)
            viewModel.onUndo()
            runCurrent()
            viewModel.onRestart()

            assertThat(viewModel.uiState.value.restartTick).isEqualTo(2)
        }

    @Test
    fun aChallengeBoardLoadsWithTheOrdinalAtZeroAndARestartBeforeTheLoadIsSilent() =
        runGame {
            val viewModel = challenge()
            viewModel.onRestart()
            runCurrent()
            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.restartTick).isEqualTo(0)

            viewModel.onRestart()

            assertThat(viewModel.uiState.value.restartTick).isEqualTo(1)
        }

    // ---- undoneMove ----------------------------------------------------------------------------

    @Test
    fun anUndoNamesTheMoveItTookBack() =
        runGame {
            val viewModel = classic()
            jumpDownIntoTheCentre(viewModel)

            viewModel.onUndo()
            runCurrent()

            assertThat(viewModel.uiState.value.undoneMove).isEqualTo(DOWN_INTO_THE_CENTRE)
            assertThat(viewModel.uiState.value.undoTick).isEqualTo(1)
        }

    @Test
    fun eachUndoNamesTheMoveItTookBackLatestFirst() =
        runGame {
            val viewModel = classic()
            jumpDownIntoTheCentre(viewModel)
            val second = RulesEngine.legalMoves(viewModel.uiState.value.board).first()
            viewModel.onCellTap(second.from)
            viewModel.onCellTap(second.to)

            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.undoneMove).isEqualTo(second)
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.undoneMove).isEqualTo(DOWN_INTO_THE_CENTRE)
        }

    @Test
    fun aRestartInstallsABoardWithNoUndoneMove() =
        runGame {
            val viewModel = classic()
            jumpDownIntoTheCentre(viewModel)
            viewModel.onUndo()
            runCurrent()
            assertThat(viewModel.uiState.value.undoneMove).isEqualTo(DOWN_INTO_THE_CENTRE)

            viewModel.onRestart()

            assertThat(viewModel.uiState.value.undoneMove).isNull()
        }

    @Test
    fun aResumedGameStartsWithNoUndoneMoveAndTheOrdinalAtZeroAndUndoesItsLastMove() =
        runGame {
            val saved = slot<ResumeSnapshotDto>()
            val progress = mockk<ProgressRepository>(relaxed = true)
            coEvery { progress.saveSnapshot(capture(saved), any()) } just runs
            coEvery { progress.loadSnapshot(any()) } answers { saved.captured }
            val played = classic(progress)
            jumpDownIntoTheCentre(played)
            played.onLeaveScreen()
            runCurrent()

            val resumed = classic(progress, resume = true)
            runCurrent()
            assertThat(resumed.uiState.value.canUndo).isTrue()
            assertThat(resumed.uiState.value.undoneMove).isNull()
            assertThat(resumed.uiState.value.restartTick).isEqualTo(0)

            resumed.onUndo()
            runCurrent()
            assertThat(resumed.uiState.value.undoneMove).isEqualTo(DOWN_INTO_THE_CENTRE)
        }

    // ---- Helpers -------------------------------------------------------------------------------

    /**
     * [runTest] on the shared dispatcher, cancelling every ViewModel before it returns — pass or fail.
     * A move starts the per-second ticker, and `runTest` would otherwise drain it forever.
     */
    private fun runGame(body: suspend TestScope.() -> Unit) =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                viewModelStore.clear()
            }
        }

    private fun jumpDownIntoTheCentre(viewModel: GameViewModel) {
        viewModel.onCellTap(DOWN_INTO_THE_CENTRE.from)
        viewModel.onCellTap(DOWN_INTO_THE_CENTRE.to)
    }

    private fun classic(
        progress: ProgressRepository = mockk(relaxed = true),
        resume: Boolean = false,
    ): GameViewModel = viewModelFor(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33, RESUME_ARG to resume), progress)

    private fun challenge(): GameViewModel =
        viewModelFor(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33, CHALLENGE_LEVEL_ARG to 1), mockk(relaxed = true))

    private fun viewModelFor(
        args: Map<String, Any?>,
        progress: ProgressRepository,
    ): GameViewModel {
        // The typed initializer needs no unchecked cast, so the test adds no suppression.
        val factory =
            viewModelFactory {
                initializer {
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = AnnouncementStateLevels,
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = AnnouncementStateSettings,
                        challengeProgress = mockk(relaxed = true),
                        progressRepository = progress,
                        timeSource = TestTimeSource(),
                        defaultDispatcher = testDispatcher,
                        adGateway = NoOpAdGateway(),
                        getHintUseCase = GetHintUseCase(),
                        savedStateHandle = SavedStateHandle(args),
                    )
                }
            }
        return ViewModelProvider(viewModelStore, factory)["vm${viewModelKey++}", GameViewModel::class.java]
    }

    private companion object {
        /** English-33's opening jump into the centre. */
        val DOWN_INTO_THE_CENTRE = Move(from = Position(1, 3), over = Position(2, 3), to = Position(3, 3))

        const val LEVEL_ID_ARG = "levelId"
        const val CHALLENGE_LEVEL_ARG = "challengeLevel"
        const val RESUME_ARG = "resume"
    }
}

private const val ANNOUNCEMENT_STATE_SDK = 34

/** English-33 for Classic; the real generator for Challenge, as production serves it. */
private object AnnouncementStateLevels : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level = levelOf(levelId, Boards.english33Initial)

    override fun loadChallenge(levelNumber: Int): Level {
        val generated = LevelGenerator.generate(levelNumber)
        val board = generated.startingBoard
        return Level(
            id = "challenge-$levelNumber",
            displayName = "challenge-$levelNumber",
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
            solution = generated.solution,
            generatorVersion = generated.generatorVersion,
        )
    }

    private fun levelOf(
        id: String,
        board: Board,
    ): Level =
        Level(id = id, displayName = id, spec = board.spec, initialBoard = board, holeCount = board.spec.inPlay.size)
}

/** Show-moves, Sound and Haptics on. */
private object AnnouncementStateSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)
    override val soundOn: Flow<Boolean> = flowOf(true)
    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}
