package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rotation recreates the Activity while the ViewModel keeps its state, every ordinal included
 * (sound-designer F-1, tech-lead-signoff.md §5). The gameplay screen composed from scratch with a
 * carried ordinal plays none of the four tick-keyed cues for an event made before it, and plays
 * each once for the next event, as the wrong tap's haptic and announcement already do
 * (`GameScreenWrongTapTest.aScreenComposedWithACarriedWrongTapNeitherFeelsNorAnnouncesIt`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CUE_ROTATION_SDK])
class GameScreenCueRotationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sounds = FakeSoundPlayer()
    private val screen = mutableStateOf(GameUiState.initial(Boards.english33Initial))

    @Test
    fun aScreenComposedWithACarriedWrongTapPlaysNoInvalidTapCueUntilTheNextWrongTap() {
        showCarried { it.copy(rejectedTapTick = CARRIED_ORDINAL, rejectedTapCell = CENTRE) }
        assertThat(sounds.cues).isEmpty()

        next { it.copy(rejectedTapTick = it.rejectedTapTick + 1) }
        assertThat(sounds.cues).containsExactly(SoundCue.InvalidTap)
    }

    @Test
    fun aScreenComposedWithACarriedSelectionPlaysNoPickUpCueUntilTheNextSelection() {
        showCarried { it.copy(selectionTick = CARRIED_ORDINAL) }
        assertThat(sounds.cues).isEmpty()

        next { it.copy(selectionTick = it.selectionTick + 1) }
        assertThat(sounds.cues).containsExactly(SoundCue.Select)
    }

    @Test
    fun aScreenComposedWithACarriedPutDownPlaysNoPutDownCueUntilTheNextPutDown() {
        showCarried { it.copy(deselectionTick = CARRIED_ORDINAL) }
        assertThat(sounds.cues).isEmpty()

        next { it.copy(deselectionTick = it.deselectionTick + 1) }
        assertThat(sounds.cues).containsExactly(SoundCue.Deselect)
    }

    @Test
    fun aScreenComposedWithACarriedUndoPlaysNoUndoCueUntilTheNextUndo() {
        showCarried { it.copy(undoTick = CARRIED_ORDINAL) }
        assertThat(sounds.cues).isEmpty()

        next { it.copy(undoTick = it.undoTick + 1) }
        assertThat(sounds.cues).containsExactly(SoundCue.Undo)
    }

    /** Composes the screen from scratch over a state that carries an ordinal, as after a rotation. */
    private fun showCarried(carry: (GameUiState) -> GameUiState) {
        screen.value = carry(screen.value)
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalSoundPlayer provides sounds) {
                    GameScreen(
                        state = screen.value,
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

    /** The next event of the same kind, as `GameViewModel` writes it: the ordinal advances by one. */
    private fun next(event: (GameUiState) -> GameUiState) {
        screen.value = event(screen.value)
        composeRule.waitForIdle()
    }

    private companion object {
        /** The English-33 start's only hole, where a tap with nothing held is a wrong tap. */
        val CENTRE = Position(row = 3, col = 3)
        const val CARRIED_ORDINAL = 3
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val CUE_ROTATION_SDK = 34
