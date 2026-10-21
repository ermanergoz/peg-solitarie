package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.TestTimeSource

/**
 * Criterion 21c — **a spent free allowance changes nothing a player can see or hear** (ux-spec
 * §4.4.1, D-04e; the ruling `GameUiState` records beside its assist-funnel fields; ADS-01).
 *
 * "Out of hints" and "out of undos" are not states of the dock. The control looks and announces
 * exactly as it did while the allowance lasted, and the tap raises the opt-in dialog — the only
 * signal the allowance system exists. Drawing Unavailable there would be worse than a rule
 * breach: it would tell the player the assist is gone when one tap would offer it.
 *
 * This is asserted against the **real** `GameViewModel` behind the real `GameScreen`, because
 * the allowance lives nowhere else: `GameUiState` deliberately carries no counter, so no
 * hand-built state can be "spent". The fixture is `GameViewModelAllowanceTest`'s 1x5 strip — one
 * opening jump, and a genuine one-peg line so the real Solver answers every hint — driven through
 * the ViewModel's own intents, exactly as the dock would drive it.
 *
 * What is compared is the whole dock as TalkBack meets it: every node under the dock's tag in
 * the merged tree, with its name, role, enabled state, toggle state and look. Equal lists mean
 * the same nodes, saying the same things, drawn the same way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SPENT_ROBOLECTRIC_SDK])
class DockSpentAllowanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Criterion 21c, Undo: every free undo spent, one jump on the board, and nothing to tell. */
    @Test
    fun aSpentUndoAllowanceLooksAndSpeaksAsBeforeAndItsTapRaisesTheOffer() {
        val game = startGame()
        playOpeningJump(game)
        val whileFree = dockAsTalkBackMeetsIt()

        repeat(AssistAllowanceLimits.FREE_UNDOS) {
            onUi { game.onUndo() }
            playOpeningJump(game)
        }

        assertThat(dockAsTalkBackMeetsIt()).isEqualTo(whileFree)
        composeRule.onNode(dockControl(R.string.undo_action)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.ad_offer_undo_title)).assertIsDisplayed()
    }

    /**
     * Criterion 21c, Hint: every free hint spent — each one leaving its glow on the board, which
     * is itself not an "on" — and the control is still the Available action it was.
     */
    @Test
    fun aSpentHintAllowanceLooksAndSpeaksAsBeforeAndItsTapRaisesTheOffer() {
        val game = startGame()
        val whileFree = dockAsTalkBackMeetsIt()

        repeat(AssistAllowanceLimits.FREE_HINTS) { onUi { game.onHintRequested() } }

        assertThat(dockAsTalkBackMeetsIt()).isEqualTo(whileFree)
        composeRule.onNode(dockControl(R.string.hint_action)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.ad_offer_hint_title)).assertIsDisplayed()
    }

    // ---- Harness ---------------------------------------------------------------

    /** A real `GameViewModel` on the strip, bound to the real screen as `GameRoute` binds it. */
    private fun startGame(): GameViewModel {
        val viewModel =
            GameViewModel(
                rulesEngine = RulesEngine,
                levelRepository = StripLevelRepository(winnableStrip()),
                scoreCalculator = ScoreCalculator,
                settingsRepository = SpentAllowanceSettings,
                // This spec is the dock, not the ladder or the resume snapshot.
                challengeProgress = mockk(relaxed = true),
                progressRepository = mockk(relaxed = true),
                timeSource = TestTimeSource(),
                // The real Solver, inline: every hint resolves before the next intent.
                defaultDispatcher = Dispatchers.Unconfined,
                // Offerable, and the production stub: exhaustion raises the real opt-in dialog.
                adGateway = NoOpAdGateway(),
                getHintUseCase = GetHintUseCase(),
                savedStateHandle = SavedStateHandle(mapOf(SPENT_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)),
            )
        composeRule.setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                GameScreen(
                    state = state,
                    onCellTap = viewModel::onCellTap,
                    onRestart = viewModel::onRestart,
                    onConfirmRestart = viewModel::onConfirmRestart,
                    onDismissRestart = viewModel::onDismissRestart,
                    onAnimationFinished = viewModel::onAnimationFinished,
                    onToggleHints = viewModel::onToggleHints,
                    onUndo = viewModel::onUndo,
                    onHint = viewModel::onHintRequested,
                    onAcceptAdOffer = viewModel::onAcceptAdOffer,
                    onDismissAdOffer = viewModel::onDismissAdOffer,
                    onHome = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    /** The strip's one opening jump: col 0 over col 1 into the hole at col 2. */
    private fun playOpeningJump(game: GameViewModel) {
        onUi { game.onCellTap(at(LEFT_PEG_COL)) }
        onUi { game.onCellTap(at(JUMP_TARGET_COL)) }
    }

    private fun onUi(intent: () -> Unit) {
        composeRule.runOnUiThread(intent)
        composeRule.waitForIdle()
    }

    /**
     * Every node under the dock, in the merged tree TalkBack consumes, reduced to what it tells
     * the player: name, role, enabled, toggle state and look.
     */
    private fun dockAsTalkBackMeetsIt(): List<SeenNode> =
        composeRule
            .onNodeWithTag(GAME_ACTION_DOCK_TEST_TAG)
            .fetchSemanticsNode()
            .descendants()
            .map { node ->
                SeenNode(
                    names = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty(),
                    role = node.config.getOrNull(SemanticsProperties.Role),
                    enabled = node.config.getOrNull(SemanticsProperties.Disabled) == null,
                    toggle = node.config.getOrNull(SemanticsProperties.ToggleableState),
                    look = node.config.getOrNull(DockLookKey),
                )
            }

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    private fun dockControl(
        @StringRes name: Int,
    ): SemanticsMatcher =
        hasContentDescription(string(name)) and
            SemanticsMatcher.keyIsDefined(DockLookKey) and
            hasAnyAncestor(hasTestTag(GAME_ACTION_DOCK_TEST_TAG))

    private fun string(
        @StringRes id: Int,
    ): String = RuntimeEnvironment.getApplication().getString(id)

    /** A 1x5 strip with pegs at cols 0, 1 and 3: one opening jump, and a real one-peg line. */
    private fun winnableStrip(): Board {
        val spec = BoardSpec(inPlay = (0 until STRIP_LENGTH).map { at(it) }, target = null)
        return Board(spec = spec, pegs = 0L)
            .withPeg(at(LEFT_PEG_COL))
            .withPeg(at(ADJACENT_PEG_COL))
            .withPeg(at(THIRD_PEG_COL))
    }

    private fun at(col: Int): Position = Position(row = STRIP_ROW, col = col)
}

/** One dock node as TalkBack meets it. */
private data class SeenNode(
    val names: List<String>,
    val role: Role?,
    val enabled: Boolean,
    val toggle: ToggleableState?,
    val look: DockLook?,
)

/** A [LevelRepository] serving the one strip through Classic; Challenge is unreachable. */
private class StripLevelRepository(
    private val board: Board,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level =
        Level(
            id = levelId,
            displayName = levelId,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )

    override fun loadChallenge(levelNumber: Int): Level = error("This spec is Classic-only")
}

/** Dots, sound and haptics on: the fail-open defaults, and the state the dock starts in. */
private object SpentAllowanceSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** The `Gameplay` route's level-id argument key (the route's property name). */
private const val SPENT_LEVEL_ID_ARG = "levelId"

/** The strip fixture: one row, five holes, pegs at cols 0, 1 and 3. */
private const val STRIP_ROW = 0
private const val STRIP_LENGTH = 5
private const val LEFT_PEG_COL = 0
private const val ADJACENT_PEG_COL = 1
private const val JUMP_TARGET_COL = 2
private const val THIRD_PEG_COL = 3

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SPENT_ROBOLECTRIC_SDK = 34
