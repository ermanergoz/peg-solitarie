package com.app.pegsolitaire.ui.game

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.testing.RecordingHapticFeedback
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.components.BoardGeometry
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.time.TestTimeSource

/**
 * The gameplay screen's answer to a wrong tap beyond the board's drawing (D-06b, §3.4–§3.5 of
 * `docs/ux/invalid-tap-feedback.md`; WT-12 and WT-13), driven by real taps through the real
 * [GameScreen] over a real [GameViewModel], with recording haptic and sound engines swapped in.
 *
 * On the English-33 start the only hole is the centre, so W1 is a tap there with nothing held, W2
 * is a tap on another peg while one is held, and W3 — which needs a second hole — comes after one
 * jump, holding a peg whose only destination is not the hole tapped.
 *
 * The shake is read off the window's pixels on a paused clock: the tapped cell's
 * intensity-weighted centroid against the same cell before the tap. That makes these the tests of
 * `GameScreen`'s own wiring — that its board corrects a wrong tap at all — where
 * `BoardCanvasWrongTapTest` holds the curves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WRONG_TAP_SCREEN_SDK], qualifiers = WRONG_TAP_SCREEN_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenWrongTapTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val haptics = RecordingHapticFeedback()
    private val sounds = FakeSoundPlayer()

    /** The surface the board is drawn on, as composed. */
    private var slab = 0

    // ---- The reject haptic (D-06b; WT-12) -----------------------------------------------

    @Test
    fun eachWrongTapIsFeltOnceAndASelectionOrAPutDownIsNot() {
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))

        tapCell(CENTRE) // W1
        tapCell(CENTRE) // W1 again: every repeat is felt
        tapCell(LEFT_PEG) // a selection
        tapCell(TOP_PEG) // W2, holding LEFT_PEG
        tapCell(LEFT_PEG) // the put-down

        assertThat(haptics.events).containsExactly(REJECT, REJECT, REJECT)
    }

    @Test
    fun aWrongTapOnAHoleThatIsNotADestinationIsFeltAsARejectNeverAsAContextClick() {
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))
        jumpLeftPegIntoTheCentre()
        haptics.events.clear()

        tapCell(RIGHT_PEG) // its only jump lands on the hole the jumped peg left
        tapCell(LEFT_PEG) // W3: the other new hole

        assertThat(haptics.events).containsExactly(REJECT)
    }

    @Test
    fun noWrongTapIsFeltWhileHapticsAreOff() {
        val viewModel = startGame(WrongTapSettings(soundOn = true, hapticsOn = false))

        tapCell(CENTRE) // W1
        tapCell(LEFT_PEG)
        tapCell(TOP_PEG) // W2
        tapCell(LEFT_PEG)
        jumpLeftPegIntoTheCentre()
        tapCell(RIGHT_PEG)
        tapCell(LEFT_PEG) // W3

        assertThat(viewModel.uiState.value.rejectedTapTick).isEqualTo(THREE_WRONG_TAPS)
        assertThat(haptics.events).isEmpty()
    }

    @Test
    fun aScreenComposedWithACarriedWrongTapNeitherFeelsNorAnnouncesIt() {
        // WT-6: a rotation recreates the screen while the ViewModel keeps its state, ordinal
        // included. The house `> 0` guard would buzz and speak again for a tap made before it.
        val carried = GameUiState.initial(Boards.english33Initial)
        showState(carried.copy(rejectedTapTick = CARRIED_ORDINAL, rejectedTapCell = CENTRE))

        assertThat(haptics.events).isEmpty()
        assertThat(boardAnnouncement()).isNull()
    }

    // ---- The announcement (D-06c as superseded by D-TBa items 2 and 3; WT-14's JVM half) ----

    @Test
    fun eachWrongTapPutsItsOwnSentenceOnTheBoardsPoliteLiveRegion() {
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))

        tapCell(CENTRE) // W1
        assertAnnounced(R.string.board_a11y_rejected_no_peg)
        tapCell(LEFT_PEG)
        tapCell(TOP_PEG) // W2
        assertAnnounced(R.string.board_a11y_rejected_put_down_first)
        tapCell(LEFT_PEG)
        jumpLeftPegIntoTheCentre()
        tapCell(RIGHT_PEG)
        tapCell(LEFT_PEG) // W3
        assertAnnounced(R.string.board_a11y_rejected_not_target)
    }

    @Test
    fun aSecondIdenticalWrongTapChangesTheLiveRegionSoItIsSpokenAgain() {
        // A live region speaks only when its content changes, so an identical repeat must change it.
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))

        tapCell(CENTRE)
        val first = boardAnnouncement()
        tapCell(CENTRE)
        val second = boardAnnouncement()

        assertThat(second).isNotEqualTo(first)
        assertAnnounced(R.string.board_a11y_rejected_no_peg)
    }

    @Test
    fun theSentenceIsWithdrawnAfterItsHoldSoTheBoardIsNoFocusStopAfterwards() {
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))
        tapCell(CENTRE)
        assertAnnounced(R.string.board_a11y_rejected_no_peg)

        composeRule.mainClock.advanceTimeBy(PAST_THE_HOLD_MS)
        composeRule.waitForIdle()

        // Nothing left to read on a later swipe: no description, no text, no action.
        val board = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().config
        assertThat(board.getOrNull(SemanticsProperties.ContentDescription)).isNull()
        assertThat(board.getOrNull(SemanticsProperties.Text)).isNull()
        assertThat(board.getOrNull(SemanticsProperties.LiveRegion)).isEqualTo(LiveRegionMode.Polite)
    }

    @Test
    fun aSelectionAndAPutDownEachAnnounceTheirOwnLine() {
        // D-TB: a selection and a put-down are board events, spoken on the same channel.
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))

        tapCell(LEFT_PEG)
        assertThat(boardAnnouncement()?.let(::spoken)).isEqualTo("Selected row 4, column 2, 1 jump.")
        tapCell(LEFT_PEG)
        assertThat(boardAnnouncement()?.let(::spoken)).isEqualTo("Peg put down, row 4, column 2.")
    }

    // ---- Sound is not a gate (§3.5; WT-13) ------------------------------------------------

    @Test
    fun withSoundOffEachWrongTapStillShakesItsCellAndIsStillFelt() {
        startGame(WrongTapSettings(soundOn = false, hapticsOn = true))

        assertWrongTapShakes(CENTRE) // W1
        tapCell(LEFT_PEG)
        assertWrongTapShakes(TOP_PEG) // W2
        tapCell(LEFT_PEG)
        jumpLeftPegIntoTheCentre()
        tapCell(RIGHT_PEG)
        assertWrongTapShakes(LEFT_PEG) // W3

        assertThat(haptics.events.filter { it == REJECT }).hasSize(THREE_WRONG_TAPS)
    }

    @Test
    fun withSoundOnEachWrongTapStillPlaysExactlyOneInvalidTapCue() {
        startGame(WrongTapSettings(soundOn = true, hapticsOn = true))

        tapCell(CENTRE) // W1
        tapCell(LEFT_PEG)
        tapCell(TOP_PEG) // W2
        tapCell(LEFT_PEG)
        jumpLeftPegIntoTheCentre()
        tapCell(RIGHT_PEG)
        tapCell(LEFT_PEG) // W3

        assertThat(sounds.cues.filter { it == SoundCue.InvalidTap }).hasSize(THREE_WRONG_TAPS)
    }

    // ---- Helpers ---------------------------------------------------------------------------

    private fun startGame(settings: SettingsRepository): GameViewModel {
        val viewModel =
            GameViewModel(
                rulesEngine = RulesEngine,
                levelRepository = OneBoardLevels(Boards.english33Initial),
                scoreCalculator = ScoreCalculator,
                settingsRepository = settings,
                challengeProgress = mockk(relaxed = true),
                progressRepository = mockk(relaxed = true),
                timeSource = TestTimeSource(),
                defaultDispatcher = Dispatchers.Unconfined,
                adGateway = NoOpAdGateway(),
                getHintUseCase = GetHintUseCase(),
                savedStateHandle = SavedStateHandle(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)),
            )
        composeRule.setContent {
            AppTheme {
                slab = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
                CompositionLocalProvider(LocalHapticFeedback provides haptics, LocalSoundPlayer provides sounds) {
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
        }
        composeRule.waitForIdle()
        return viewModel
    }

    /** Composes the screen over a fixed [state], every callback a no-op. */
    private fun showState(state: GameUiState) {
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalHapticFeedback provides haptics, LocalSoundPlayer provides sounds) {
                    GameScreen(
                        state = state,
                        onCellTap = {},
                        onRestart = {},
                        onConfirmRestart = {},
                        onDismissRestart = {},
                        onAnimationFinished = {},
                        onToggleHints = {},
                        onUndo = {},
                        onHint = {},
                        onAcceptAdOffer = {},
                        onDismissAdOffer = {},
                        onHome = {},
                        onNavigateBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The board's live-region text right now, or `null` when it carries none. */
    private fun boardAnnouncement(): String? =
        composeRule
            .onNodeWithTag(BOARD_CANVAS_TEST_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.single()

    /** The board is a polite live region whose text is [sentence], as a screen reader reads it. */
    private fun assertAnnounced(sentence: Int) {
        val board = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().config
        val expected = RuntimeEnvironment.getApplication().getString(sentence)
        assertThat(board.getOrNull(SemanticsProperties.LiveRegion)).isEqualTo(LiveRegionMode.Polite)
        assertThat(boardAnnouncement()?.let(::spoken)).isEqualTo(expected)
    }

    /** What a speech engine says for [text]: the repeat marker is a zero-width space, unspoken. */
    private fun spoken(text: String): String = text.replace(ZERO_WIDTH_SPACE, "").trim()

    private fun jumpLeftPegIntoTheCentre() {
        tapCell(LEFT_PEG)
        tapCell(CENTRE)
    }

    private fun tapCell(cell: Position) {
        clickCell(cell)
        composeRule.waitForIdle()
    }

    private fun clickCell(cell: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (cell.col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID,
                    y = (cell.row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID,
                ),
            )
        }
    }

    /**
     * Taps [cell] on a paused clock and watches its drawing: it must swing sideways by most of the
     * amplitude (0.10 × cellSize) and come back to rest, with the clock released again afterwards.
     */
    private fun assertWrongTapShakes(cell: Position) {
        composeRule.mainClock.autoAdvance = false
        val before = composeRule.renderedWindow()
        val box = cellBox(cell)
        clickCell(cell)
        val offsets =
            (1..WATCH_FRAMES).map {
                composeRule.waitForIdle()
                composeRule.mainClock.advanceTimeByFrame()
                centroidX(composeRule.renderedWindow(), box) - centroidX(before, box)
            }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        val amplitude = box.width * BoardMetrics.WRONG_TAP_SHAKE_RATIO
        assertWithMessage("$cell's swing, px").that(offsets.maxOf { abs(it) }).isAtLeast(PEAK_SAMPLE_FLOOR * amplitude)
        assertWithMessage("$cell at the end, px").that(abs(offsets.last())).isLessThan(SUBPIXEL)
    }

    private fun cellBox(cell: Position): Rect {
        val canvas = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val geometry = BoardGeometry.forCanvas(Boards.English33, Size(canvas.width, canvas.height))
        val center = canvas.topLeft + geometry.center(cell)
        val half = geometry.cellSize / 2
        return Rect(center.x - half, center.y - half, center.x + half, center.y + half)
    }

    /** The x of [box]'s centroid, each pixel weighted by its distance from the slab colour. */
    private fun centroidX(
        bitmap: Bitmap,
        box: Rect,
    ): Float {
        var weighted = 0.0
        var total = 0.0
        for (y in box.top.toInt() until box.bottom.toInt()) {
            for (x in box.left.toInt() until box.right.toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val weight = CHANNEL_SHIFTS.sumOf { abs((pixel shr it and CHANNEL) - (slab shr it and CHANNEL)) }
                weighted += weight.toDouble() * x
                total += weight
            }
        }
        return (weighted / total).toFloat()
    }

    private companion object {
        val REJECT = HapticFeedbackType.Reject

        /** The English-33 start's only hole, and three of its pegs. */
        val CENTRE = Position(row = 3, col = 3)
        val LEFT_PEG = Position(row = 3, col = 1)
        val RIGHT_PEG = Position(row = 3, col = 4)
        val TOP_PEG = Position(row = 0, col = 2)

        const val THREE_WRONG_TAPS = 3
        const val CARRIED_ORDINAL = 3

        /** Comfortably past the announcement's hold. */
        const val PAST_THE_HOLD_MS = 2_000L
        const val ZERO_WIDTH_SPACE = "\u200B"
        const val ENGLISH33_GRID = 7
        const val LEVEL_ID_ARG = "levelId"

        /** Past the 270 ms shake and the frames it takes to start. */
        const val WATCH_FRAMES = 24

        /** On 16 ms frames the sampled peak lands within a few percent of the amplitude. */
        const val PEAK_SAMPLE_FLOOR = 0.85f
        const val SUBPIXEL = 0.5f

        val CHANNEL_SHIFTS = listOf(16, 8, 0)
        const val CHANNEL = 0xFF
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val WRONG_TAP_SCREEN_SDK = 34

/** A tall phone at 1.5× density, so the board's cells are big enough to read a shake from. */
private const val WRONG_TAP_SCREEN_QUALIFIERS = "w400dp-h800dp-hdpi"

/** Serves one fixture board through the Classic path. */
private class OneBoardLevels(
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

    override fun loadChallenge(levelNumber: Int): Level = error("Classic-only wrong-tap spec")
}

/** Fixed Sound and Haptics switches; Show-moves stays on. */
private class WrongTapSettings(
    soundOn: Boolean,
    hapticsOn: Boolean,
) : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(soundOn)

    override val hapticsOn: Flow<Boolean> = flowOf(hapticsOn)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}
