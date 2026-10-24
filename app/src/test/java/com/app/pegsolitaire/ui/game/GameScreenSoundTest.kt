package com.app.pegsolitaire.ui.game

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.SoundParams
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
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
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
 * What the board actually sounds like, asserted JVM-side via Robolectric.
 *
 * The companion to `GameViewModelSoundTest`: that spec proves the *state* a cue is fired
 * from, this one proves the cue itself — which sample, in what order, at what pitch. It
 * drives the real [GameScreen] over a real [GameViewModel] (exactly as `GameFlowTest` does)
 * and swaps a recording `FakeSoundPlayer` in through
 * `CompositionLocalProvider(LocalSoundPlayer provides …)`, the same trick `GameFlowTest`
 * already uses for `LocalHapticFeedback`.
 *
 * Two things it deliberately pins beyond "the right cue fired":
 *  - **A fresh state makes no sound at all** — the `> 0` ordinal guards hold on first
 *    composition, which is half of the phantom-cue fix (RESEARCH Pitfall 2).
 *  - **The screen has no audio gate.** Unlike `hapticsOn`, silencing is the player's job
 *    (SOUND-05 needs to stop one-shots already in flight), so the screen calls the player
 *    unconditionally and the persisted setting never reaches the UI state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREEN_SOUND_ROBOLECTRIC_SDK])
class GameScreenSoundTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    /** Records every cue the screen fires instead of playing it. */
    private val sounds = FakeSoundPlayer()

    // ---- Jump + removal (SOUND-01/SOUND-02, D-05/D-06) ----------------------

    @Test
    fun aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt() {
        startGame(chainBoard())

        // Select first, then forget it: this test is about the jump gesture's own pair.
        tapCell(row = CHAIN_ROW, col = 0)
        sounds.clear()

        tapCell(row = CHAIN_ROW, col = 2)
        settleRemovalLayer()

        assertThat(sounds.cues).containsExactly(SoundCue.Jump, SoundCue.PegRemove).inOrder()
    }

    /**
     * The same pair **with the OS animation scale at zero** (criteria 7c and 25).
     *
     * Criterion 25 says in so many words that "sound is unaffected by this switch", and it
     * was: `dumpsys audio` recorded **one** player event per jump with animations off against
     * two 60 ms apart with them on — on the tutorial *and* on a real Challenge board, so this
     * is the shipped game's own defect and not the tutorial's. The mechanism is a cancellation:
     * with animations disabled `runJumpAnimation` takes the snap path, `onAnimationFinished()`
     * clears `animating` in the same frame, and the keyed effect's coroutine dies inside the
     * 60 ms the removal layer waits.
     *
     * No test had ever run this path, which is exactly why it survived to a device session
     * (`qa-engineer.md` G-2). This is that test, and it belongs here as much as on the
     * tutorial: the cue is shared, so the guard has to sit on both screens or the next lift
     * will find only one of them covered.
     */
    @Test
    fun aJumpStillLayersTheRemovalCueWhenTheOsAnimationScaleIsZero() {
        disableOsAnimations()
        startGame(chainBoard())

        tapCell(row = CHAIN_ROW, col = 0)
        sounds.clear()

        tapCell(row = CHAIN_ROW, col = 2)
        settleRemovalLayer()

        assertThat(sounds.cues).containsExactly(SoundCue.Jump, SoundCue.PegRemove).inOrder()
        // The layering is a layering, not two cues in a heap: the pitched pop first, the
        // texture under it. Reduced motion may not reorder the pair.
        assertThat(sounds.events.first().cue).isEqualTo(SoundCue.Jump)
    }

    /** And the ladder is the ladder with animations off: the fix may not cost the pitch. */
    @Test
    fun theJumpPitchStillClimbsWhenTheOsAnimationScaleIsZero() {
        disableOsAnimations()
        startGame(chainBoard())

        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()
        val firstJump = sounds.events.first { it.cue == SoundCue.Jump }
        sounds.clear()
        jump(fromCol = 2, toCol = 4)
        settleRemovalLayer()
        val secondJump = sounds.events.first { it.cue == SoundCue.Jump }

        assertThat(firstJump.rate).isEqualTo(SoundParams.jumpRate(0))
        assertThat(secondJump.rate).isEqualTo(SoundParams.jumpRate(1))
        assertThat(sounds.cues).containsExactly(SoundCue.Select, SoundCue.Jump, SoundCue.PegRemove).inOrder()
    }

    /**
     * The move id is the move ordinal, so it **recycles after an undo** — and the cue has to
     * fire again for the replayed jump.
     *
     * This is the property that decides how the cancellation above may be fixed. Latching the
     * last non-null transition and keying on its id would survive the snap path and quietly
     * silence this case, because the replayed jump carries the id the latch is already holding.
     * The null gap between two jumps is load-bearing; only the delayed second cue may outlive
     * the effect.
     */
    @Test
    fun aJumpReplayedAfterAnUndoSoundsAgainEvenThoughItsMoveIdIsReused() {
        disableOsAnimations()
        startGame(chainBoard())

        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.undo_action))
            .performClick()
        composeRule.waitForIdle()
        sounds.clear()

        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()

        assertThat(sounds.cues).containsExactly(SoundCue.Select, SoundCue.Jump, SoundCue.PegRemove).inOrder()
    }

    @Test
    fun aSamePegChainClimbsTheJumpPitchOneRungPerHop() {
        startGame(chainBoard())

        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()
        val firstJump = sounds.events.first { it.cue == SoundCue.Jump }
        assertThat(firstJump.rate).isEqualTo(SoundParams.jumpRate(0))

        sounds.clear()
        jump(fromCol = 2, toCol = 4)
        settleRemovalLayer()
        val secondJump = sounds.events.first { it.cue == SoundCue.Jump }

        // D-05/D-06: the same peg jumping again is the next rung, not a fresh run.
        assertThat(secondJump.rate).isEqualTo(SoundParams.jumpRate(1))
        assertThat(secondJump.rate).isGreaterThan(firstJump.rate)
    }

    // ---- Selection vs. rejection (SOUND-02, D-05a) --------------------------

    @Test
    fun selectingAPegThenReTappingItPlaysThePickUpThenThePutDownAndNeverTheInvalidOne() {
        startGame(chainBoard())

        tapCell(row = CHAIN_ROW, col = 0)
        assertThat(sounds.cues).containsExactly(SoundCue.Select)

        // D-05a as amended (2026-08-28): the deselect is still a successful action, but it
        // now has its own cue rather than replaying the pick-up's.
        tapCell(row = CHAIN_ROW, col = 0)

        assertThat(sounds.cues).containsExactly(SoundCue.Select, SoundCue.Deselect).inOrder()
        // D-05a's surviving half, and the reason this assertion outlives the amendment.
        assertThat(sounds.cues).doesNotContain(SoundCue.InvalidTap)
    }

    @Test
    fun tappingAnEmptyCellWithNothingSelectedPlaysOnlyTheInvalidTapCue() {
        startGame(chainBoard())

        tapCell(row = CHAIN_ROW, col = 2) // an empty hole, nothing selected

        assertThat(sounds.cues).containsExactly(SoundCue.InvalidTap)
        assertThat(sounds.cues).doesNotContain(SoundCue.Select)
        assertThat(sounds.cues).doesNotContain(SoundCue.Deselect)
    }

    // ---- Assist controls keep their own cues (SOUND-02, D-09) ---------------

    @Test
    fun undoPlaysExactlyOneUndoCue() {
        startGame(chainBoard())
        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()
        sounds.clear()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.undo_action))
            .performClick()
        composeRule.waitForIdle()

        assertThat(sounds.cues).containsExactly(SoundCue.Undo)
    }

    @Test
    fun aHintThatRevealsAMovePlaysExactlyOneHintCue() {
        startGame(chainBoard())

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.hint_action))
            .performClick()
        composeRule.waitForIdle()

        assertThat(sounds.cues).containsExactly(SoundCue.Hint)
    }

    // ---- The win lands on the board, not on Results (SOUND-03, D-14) --------

    @Test
    fun theWinningMovePlaysTheWinCueWhileStillOnTheGameScreen() {
        val viewModel = startGame(chainBoard())

        jump(fromCol = 0, toCol = 2)
        jump(fromCol = 2, toCol = 4)
        sounds.clear()
        jump(fromCol = 4, toCol = 6) // the final jump leaves a single peg

        // D-14, first beat: the stinger is already recorded at the moment the move lands,
        // with the game screen still composed and no navigation yet performed.
        assertThat(sounds.cues).contains(SoundCue.Win)
        assertThat(viewModel.uiState.value.board.pegCount).isEqualTo(1)
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).assertExists()
    }

    // ---- No phantom cue on first composition (RESEARCH Pitfall 2) -----------

    @Test
    fun aFreshlyComposedScreenMakesNoSoundAtAll() {
        startGame(chainBoard())

        // Every ordinal starts at 0 and `animating`/`hintSource` start null, so each
        // one-shot effect runs once on first composition and fires nothing.
        assertThat(sounds.events).isEmpty()
        assertThat(sounds.preloadCalls).isEqualTo(0)
    }

    // ---- The screen has no audio gate (deliberate JumpHapticEffect asymmetry) ---

    @Test
    fun theScreenStillCallsThePlayerWhenTheSoundSettingIsOff() {
        // Silencing is the player's job, because SOUND-05 needs to stop one-shots that are
        // already in flight — something no UI flag can do. So the screen is unconditional:
        // with the persisted audio setting off, the cue still reaches the player, and the
        // player is what stays quiet.
        startGame(chainBoard(), settings = FixedGameSettings(soundEnabled = false))

        jump(fromCol = 0, toCol = 2)
        settleRemovalLayer()

        assertThat(sounds.cues).contains(SoundCue.Jump)
    }

    @Test
    fun theGameUiStateCarriesNoAudioGateField() {
        // The structural half of the same argument: if an audio flag ever appears on the
        // state, the mute has two disagreeing sources of truth.
        val audioGates =
            GameUiState::class
                .members
                .map { it.name.lowercase() }
                .filter { it.contains("sound") || it.contains("audio") || it.contains("mute") }

        assertThat(audioGates).isEmpty()
    }

    // ---- Helpers -------------------------------------------------------------

    private fun startGame(
        initialBoard: Board,
        settings: SettingsRepository = FixedGameSettings(),
    ): GameViewModel {
        val viewModel =
            GameViewModel(
                rulesEngine = RulesEngine,
                levelRepository = SoundScreenLevelRepository(initialBoard),
                scoreCalculator = ScoreCalculator,
                settingsRepository = settings,
                challengeProgress = mockk(relaxed = true),
                progressRepository = mockk(relaxed = true),
                timeSource = TestTimeSource(),
                defaultDispatcher = Dispatchers.Unconfined,
                adGateway = NoOpAdGateway(),
                getHintUseCase = GetHintUseCase(),
                savedStateHandle =
                    SavedStateHandle(
                        mapOf(SCREEN_SOUND_LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33),
                    ),
            )
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalSoundPlayer provides sounds) {
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

    /**
     * Turns the OS animator scale off for this test, the way a player with reduced motion has
     * it — the same `Settings.Global` value `rememberAnimationsEnabled()` reads, so the whole
     * screen takes the disabled path rather than a mocked approximation of it. It must be set
     * **before** the composition, because that read is `remember`ed per context.
     */
    private fun disableOsAnimations() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, ANIMATIONS_OFF)
    }

    /** Selects the peg at [fromCol] then taps [toCol] — one jump along the chain row. */
    private fun jump(
        fromCol: Int,
        toCol: Int,
    ) {
        tapCell(row = CHAIN_ROW, col = fromCol)
        tapCell(row = CHAIN_ROW, col = toCol)
    }

    private fun tapCell(
        row: Int,
        col: Int,
    ) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(cellCenter(row = row, col = col))
        }
        composeRule.waitForIdle()
    }

    /**
     * Lets the removal cue's deliberate lag past the jump cue elapse, so the layered pair
     * is fully recorded before an assertion reads the list.
     */
    private fun settleRemovalLayer() {
        composeRule.mainClock.advanceTimeBy(REMOVAL_SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    /**
     * Row 3 of the English-33 footprint carries four pegs at cols 0, 1, 3 and 5. One peg
     * walks the whole row — 0→2, 2→4, 4→6 — a three-hop same-peg run (D-06) whose last
     * jump leaves a single peg, so the same fixture drives the ladder AND the win.
     */
    private fun chainBoard(): Board =
        Board(spec = Boards.English33, pegs = 0L)
            .withPeg(Position(row = CHAIN_ROW, col = 0))
            .withPeg(Position(row = CHAIN_ROW, col = 1))
            .withPeg(Position(row = CHAIN_ROW, col = 3))
            .withPeg(Position(row = CHAIN_ROW, col = 5))
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SCREEN_SOUND_ROBOLECTRIC_SDK = 34

/** The `Gameplay(levelId)` route arg key (the route's property name). */
private const val SCREEN_SOUND_LEVEL_ID_ARG = "levelId"

/** The English-33 footprint occupies a 7×7 lattice. */
private const val ENGLISH33_GRID = 7

/** The full-width row the chain fixture is played along. */
private const val CHAIN_ROW = 3

/** Comfortably past `SoundParams.REMOVAL_LAYER_DELAY_MS`, so the layered cue has landed. */
private const val REMOVAL_SETTLE_MILLIS = 500L

/** The OS animator duration scale a player with reduced motion has set (criterion 25). */
private const val ANIMATIONS_OFF = 0f

/** A cell center as a fraction of the square board canvas (grid centered, origin 0). */
private fun androidx.compose.ui.test.TouchInjectionScope.cellCenter(
    row: Int,
    col: Int,
) = percentOffset(
    x = (col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID,
    y = (row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH33_GRID,
)

/** Serves the single fixture board through the Classic path; Challenge is never used here. */
private class SoundScreenLevelRepository(
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

    override fun loadChallenge(levelNumber: Int): Level = error("Classic-only sound spec")
}

/**
 * Fixed settings for the screen under test. [soundEnabled] exists to prove a negative: the
 * screen never reads it, so flipping it changes nothing about which cues are fired.
 */
private class FixedGameSettings(
    private val soundEnabled: Boolean = true,
) : SettingsRepository {
    override val colorblindPaletteOn: Flow<Boolean> = flowOf(false)

    override suspend fun setColorblindPaletteOn(on: Boolean) = Unit

    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(soundEnabled)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}
