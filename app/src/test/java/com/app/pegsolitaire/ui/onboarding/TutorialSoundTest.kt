package com.app.pegsolitaire.ui.onboarding

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.SoundParams
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the tutorial actually sounds like (D-01i, criteria 7c/7d/7e).
 *
 * `GameScreenSoundTest` is the template, and the reason this file exists is the project's own
 * rule that **selectivity is only a real property if both halves are asserted**: "the tutorial
 * is silent apart from the jump" is an assertion in a document until a test proves both the
 * cue that fires and the cues that do not. Without these, the first refactor that folds a cue
 * into a shared modifier breaks the contract silently.
 *
 * Driven through the real [TutorialScreen] over a real [TutorialViewModel], with a recording
 * [FakeSoundPlayer] swapped in through `CompositionLocalProvider(LocalSoundPlayer provides …)`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_SOUND_ROBOLECTRIC_SDK])
class TutorialSoundTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sounds = FakeSoundPlayer()

    @Test
    fun aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt() {
        val viewModel = startTutorial()

        // Select first, then forget it: this test is about the jump gesture's own pair.
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
        tapCell(move.from)
        sounds.clear()

        tapCell(move.to)
        settleRemovalLayer()

        assertThat(sounds.cues).containsExactly(SoundCue.Jump, SoundCue.PegRemove).inOrder()
        // The game's own pitch, from the game's own effect: the first hop of a run is rung 0.
        val jump = sounds.events.first { it.cue == SoundCue.Jump }
        assertThat(jump.rate).isEqualTo(SoundParams.jumpRate(FIRST_RUNG))
    }

    /**
     * Criterion 7c's own named failure mode: the same pair **with OS animations off**.
     *
     * The device reading was one audio event per jump instead of two, on five separate
     * tutorial jumps and on a real Challenge board (`qa-engineer.md` F-2) — the shared
     * effect's coroutine cancelled inside the 60 ms removal delay by the snap path clearing
     * `animating`. The gameplay half of this guard is
     * `GameScreenSoundTest.aJumpStillLayersTheRemovalCueWhenTheOsAnimationScaleIsZero`; both
     * exist because the effect is shared and a guard on one screen says nothing about the
     * other's call site.
     *
     * Criterion 25 forecloses the "expected consequence of reduced motion" reading in so many
     * words: sound is unaffected by that switch.
     */
    @Test
    fun aCompletedJumpStillLayersTheRemovalCueWhenTheOsAnimationScaleIsZero() {
        disableOsAnimations()
        val viewModel = startTutorial()

        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
        tapCell(move.from)
        sounds.clear()

        tapCell(move.to)
        settleRemovalLayer()

        assertThat(sounds.cues).containsExactly(SoundCue.Jump, SoundCue.PegRemove).inOrder()
        val jump = sounds.events.first { it.cue == SoundCue.Jump }
        assertThat(jump.rate).isEqualTo(SoundParams.jumpRate(FIRST_RUNG))
    }

    @Test
    fun theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep() {
        val viewModel = startTutorial()

        // A fresh composition is silent, which is half of the phantom-cue guard.
        assertThat(sounds.cues).isEmpty()

        // Every wrong-tap class the spec enumerates, plus selection and deselection.
        //
        // The recorder is NOT cleared between them, and that is the point: it used to be
        // cleared after the fourth tap, so four of the five gestures this test is named for
        // were never actually asserted and a Select cue on selection would have passed
        // (code-auditor A-1). Nothing here fires a cue, so the whole sequence is assertable
        // as one — which is what the test claims to check.
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
        tapCell(move.from) // select
        tapCell(move.from) // deselect (a re-tap)
        tapCell(move.from) // select again
        tapCell(Position(row = 0, col = 1)) // a different peg while one is held: a no-op
        tapCell(Position(row = 0, col = 0)) // the held peg again — the only deselect

        assertThat(sounds.cues)
            .containsNoneOf(
                SoundCue.Select,
                SoundCue.Deselect,
                SoundCue.InvalidTap,
                SoundCue.Undo,
                SoundCue.Hint,
            )
    }

    @Test
    fun theCompletionStarPopsWithoutTheWinOrStarPopCue() {
        val viewModel = startTutorial()

        playToCompletion(viewModel)
        composeRule.waitForIdle()

        // A tutorial is not a victory: `ClayAnim.STAR_POP_MS` is borrowed as a duration, not
        // as a cue, and D-14's two-beat celebration belongs to a real solve.
        //
        // Not cleared before the assertion, for A-1's reason again: the clear used to sit
        // after the star had already composed, so the one frame that could have fired a star
        // cue was the one frame outside the assertion's reach. The playthrough's own Jump and
        // PegRemove cues are in the list now and are not what `containsNoneOf` looks for.
        assertThat(sounds.cues).containsNoneOf(SoundCue.Win, SoundCue.StarPop)
    }

    @Test
    fun theCompletionButtonPlaysTheForwardPressCueAndSkipPlaysTheDismissCue() {
        val viewModel = startTutorial()

        // Skip, on the very first step (D-01d): it is a dismissal.
        composeRule.onNodeWithText(label(R.string.onboarding_skip)).performClick()
        composeRule.waitForIdle()
        assertThat(sounds.cues).containsExactly(SoundCue.ButtonDismiss)

        // The completion CTA: the same forward press cue every other primary CTA plays
        // (SOUND-04, D-11) — the gap D-01h left unruled and D-01i closed.
        playToCompletion(viewModel)
        sounds.clear()
        composeRule
            .onNodeWithText(label(R.string.onboarding_got_it))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
    }

    /** Composes the real screen over the real ViewModel and returns the holder. */
    private fun startTutorial(): TutorialViewModel {
        val viewModel = TutorialViewModel(SilentOnboardingRepository(), TestApplicationScope)
        composeRule.setContent {
            CompositionLocalProvider(LocalSoundPlayer provides sounds) {
                AppTheme {
                    TutorialRouteUnderTest(viewModel)
                }
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    /** Plays every step's winning line, leaving the tutorial on its completion state. */
    private fun playToCompletion(viewModel: TutorialViewModel) {
        repeat(TutorialUiState.STEP_COUNT) {
            while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                val next = RulesEngine.legalMoves(viewModel.uiState.value.board.board).first()
                tapCell(next.from)
                tapCell(next.to)
            }
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }
    }

    private fun tapCell(position: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (position.col + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                    y = (position.row + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    /**
     * Turns the OS animator scale off, the way a player with reduced motion has it — the same
     * `Settings.Global` value `rememberAnimationsEnabled()` reads, so the board takes its real
     * snap path rather than a mocked approximation. Set **before** the composition, because
     * that read is `remember`ed per context.
     */
    private fun disableOsAnimations() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            ANIMATIONS_OFF,
        )
    }

    /** Lets the removal cue's deliberate lag past the jump cue elapse before an assertion reads. */
    private fun settleRemovalLayer() {
        composeRule.mainClock.advanceTimeBy(REMOVAL_SETTLE_MS)
        composeRule.waitForIdle()
    }

    private fun label(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    private companion object {
        /** Every tutorial footprint sits in a 3-cell square grid, so one divisor serves all three. */
        const val TUTORIAL_GRID = 3

        /** The first rung of the same-peg pitch ladder. */
        const val FIRST_RUNG = 0

        /** Comfortably past `SoundParams.REMOVAL_LAYER_DELAY_MS`. */
        const val REMOVAL_SETTLE_MS = 500L

        /** The OS animator duration scale a player with reduced motion has set (criterion 25). */
        const val ANIMATIONS_OFF = 0f
    }
}

private const val TUTORIAL_SOUND_ROBOLECTRIC_SDK = 34
