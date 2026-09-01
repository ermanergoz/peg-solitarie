package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * The advance-on-solve half of CHAL-05 / PERS-02: solving a **Challenge** level is the
 * only thing that moves the persisted [ChallengeProgress] level forward (Phase-3 D-02 —
 * no skipping, no advance for merely reaching a board).
 *
 * The seam under test is [ChallengeProgress.advancePast]: a relaxed MockK stand-in
 * records whether the ViewModel actually calls it — and with which level — so a persisted
 * level that never advances (the "invisible feature" trap) fails here rather than on a
 * device. The cases pin the full truth table:
 *  - a Challenge board played to Won advances exactly once, keyed to the solved level;
 *  - the same board played as a **Classic** level never advances;
 *  - a Challenge board driven into Stuck never advances;
 *  - a back→undo→re-solve cycle (CR-02) only ever advances past the SAME solved level,
 *    so the idempotent store write cannot skip a level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ADVANCE_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class GameViewModelChallengeAdvanceTest {
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

    @Test
    fun solvingAChallengeLevelAdvancesThePersistedLevelExactlyOnce() =
        runTest(testDispatcher) {
            val progress = mockk<ChallengeProgress>(relaxed = true)
            val viewModel =
                viewModelFor(
                    board = wonInOneJumpBoard(),
                    challengeLevel = PLAYED_CHALLENGE_LEVEL,
                    challengeProgress = progress,
                )
            runCurrent() // the async Challenge load

            jumpToWin(viewModel)

            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)
            verify(exactly = 1) { progress.advancePast(PLAYED_CHALLENGE_LEVEL) }
        }

    @Test
    fun reSolvingTheSameBoardAfterAnUndoOnlyEverAdvancesPastTheSolvedLevel() =
        runTest(testDispatcher) {
            // CR-02: navigating to Results does not pop Gameplay, so back → undo (which
            // resets the finished latch) → re-jump re-fires the advance. Every advance
            // must be keyed to the SAME solved level — the store's max(current, N+1) then
            // makes the second call a no-op instead of a level skip.
            val progress = mockk<ChallengeProgress>(relaxed = true)
            val viewModel =
                viewModelFor(
                    board = wonInOneJumpBoard(),
                    challengeLevel = PLAYED_CHALLENGE_LEVEL,
                    challengeProgress = progress,
                )
            runCurrent()

            jumpToWin(viewModel)
            runCurrent()
            viewModel.onUndo()
            runCurrent()
            jumpToWin(viewModel)
            runCurrent()

            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)
            val advancedPast = mutableListOf<Int>()
            verify(atLeast = 1) { progress.advancePast(capture(advancedPast)) }
            // Both fires carry the solved level 7 — never 8 (the blind-increment bug).
            assertThat(advancedPast.toSet()).containsExactly(PLAYED_CHALLENGE_LEVEL)
        }

    @Test
    fun solvingAClassicLevelNeverAdvancesTheChallengeLevel() =
        runTest(testDispatcher) {
            val progress = mockk<ChallengeProgress>(relaxed = true)
            val viewModel =
                viewModelFor(
                    board = wonInOneJumpBoard(),
                    challengeLevel = null,
                    challengeProgress = progress,
                )
            runCurrent()

            jumpToWin(viewModel)

            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Won)
            verify(exactly = 0) { progress.advancePast(any()) }
        }

    @Test
    fun aStuckChallengeBoardNeverAdvancesTheChallengeLevel() =
        runTest(testDispatcher) {
            val progress = mockk<ChallengeProgress>(relaxed = true)
            val viewModel =
                viewModelFor(
                    board = twoMoveStuckLine(),
                    challengeLevel = PLAYED_CHALLENGE_LEVEL,
                    challengeProgress = progress,
                )
            runCurrent()

            // The only available line strands two non-adjacent pegs → Stuck, never Won.
            jump(viewModel, from = Position(row = 0, col = 2), to = Position(row = 0, col = 4))
            jump(viewModel, from = Position(row = 0, col = 0), to = Position(row = 0, col = 2))
            runCurrent()

            assertThat(viewModel.uiState.value.status).isEqualTo(GameStatus.Stuck)
            verify(exactly = 0) { progress.advancePast(any()) }
        }

    // ---- Helpers -------------------------------------------------------------

    /** Plays the single jump that clears [wonInOneJumpBoard] to one peg (Won). */
    private fun jumpToWin(viewModel: GameViewModel) {
        jump(viewModel, from = Position(row = 0, col = 0), to = Position(row = 0, col = 2))
    }

    private fun jump(
        viewModel: GameViewModel,
        from: Position,
        to: Position,
    ) {
        viewModel.onCellTap(from)
        viewModel.onCellTap(to)
    }

    /**
     * Builds a [GameViewModel] over [board], served for BOTH the Classic and the Challenge
     * load path, so the two modes differ only by the `challengeLevel` route arg — the exact
     * variable under test.
     */
    private fun viewModelFor(
        board: Board,
        challengeLevel: Int?,
        challengeProgress: ChallengeProgress,
    ): GameViewModel {
        val args =
            buildMap<String, Any?> {
                put(ADVANCE_LEVEL_ID_ARG, ClassicLevelIds.ENGLISH33)
                if (challengeLevel != null) put(ADVANCE_CHALLENGE_LEVEL_ARG, challengeLevel)
            }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        levelRepository = FixedBoardLevelRepository(board),
                        scoreCalculator = ScoreCalculator,
                        settingsRepository = AlwaysOnSettingsRepository,
                        challengeProgress = challengeProgress,
                        // The resume snapshot has its own spec (GameViewModelResumeTest);
                        // a relaxed stand-in absorbs the clear-on-solve call.
                        progressRepository = mockk(relaxed = true),
                        timeSource = TestTimeSource(),
                        defaultDispatcher = testDispatcher,
                        adGateway = GrantingAdGateway,
                        getHintUseCase = GetHintUseCase(),
                        savedStateHandle = SavedStateHandle(args),
                    ) as T
            }
        return ViewModelProvider(viewModelStore, factory)["vm${viewModelKey++}", GameViewModel::class.java]
    }

    /** A 1x3 strip (pegs at cols 0,1; hole at col 2): one jump leaves a lone peg → Won. */
    private fun wonInOneJumpBoard(): Board =
        Board(spec = horizontalLine(length = WIN_STRIP_LENGTH), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))

    /** A 1x5 strip filled cols 0..3: two jumps strand two non-adjacent pegs → Stuck. */
    private fun twoMoveStuckLine(): Board =
        Board(spec = horizontalLine(length = STUCK_STRIP_LENGTH), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))
            .withPeg(Position(row = 0, col = 2))
            .withPeg(Position(row = 0, col = 3))

    private fun horizontalLine(length: Int): BoardSpec =
        BoardSpec(inPlay = (0 until length).map { Position(row = 0, col = it) }, target = null)
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ADVANCE_ROBOLECTRIC_SDK = 34

/** The `Gameplay(levelId)` route arg key (the route's property name). */
private const val ADVANCE_LEVEL_ID_ARG = "levelId"

/** The `Gameplay(challengeLevel)` route arg key (the route's property name). */
private const val ADVANCE_CHALLENGE_LEVEL_ARG = "challengeLevel"

/** The Challenge level the test plays — deliberately not 1, so an advance is unambiguous. */
private const val PLAYED_CHALLENGE_LEVEL = 7

/** A 1x3 strip is the smallest board with exactly one winning jump. */
private const val WIN_STRIP_LENGTH = 3

/** A 1x5 strip is the smallest board whose only line dead-ends with two pegs left. */
private const val STUCK_STRIP_LENGTH = 5

/**
 * A [LevelRepository] that serves the SAME board through both load paths, so a test can
 * play one board as a Classic level or as a Challenge level and change nothing else.
 */
private class FixedBoardLevelRepository(
    private val board: Board,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level = levelOf(levelId)

    override fun loadChallenge(levelNumber: Int): Level = levelOf("challenge-$levelNumber")

    private fun levelOf(id: String): Level =
        Level(
            id = id,
            displayName = id,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )
}

/** Helpers-on settings; this spec is about the advance, not the hint/haptic gates. */
private object AlwaysOnSettingsRepository : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** The auto-grant stub's behavior: every rewarded request answers [RewardOutcome.Granted]. */
private object GrantingAdGateway : AdGateway {
    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

    override fun showInterstitial() = Unit
}
