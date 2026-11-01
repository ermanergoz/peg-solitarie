package com.app.pegsolitaire.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.app.pegsolitaire.ui.game.MoveTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays the pitched jump cue as each jump lands, then layers the peg-removal cue under it
 * (SOUND-01/SOUND-02).
 *
 * Keyed on [MoveTransition.id] — the same move ordinal `JumpHapticEffect` uses — so it fires
 * exactly once per jump rather than once per recomposition. The pitch comes from
 * [MoveTransition.runIndex], the same-peg ladder step (D-05/D-06); because that index is
 * derived from the move history rather than stored, undoing and replaying a jump replays
 * its original pitch (D-08) with nothing to keep in sync here.
 *
 * Every jump removes exactly one peg, so the two cues always co-occur; the removal trails
 * by [SoundParams.REMOVAL_LAYER_DELAY_MS] so it reads as a texture layer beneath the pitched
 * pop instead of a second competing hit. That trailing cue is launched on the **composition**
 * scope rather than inside the keyed effect, so it survives `animating` being cleared while it
 * waits — see the comment at the call.
 *
 * **Shared by the gameplay screen and the tutorial screen** (D-01i), because SOUND-01 says
 * *every* peg jump sounds and both screens jump real pegs on the real rules engine. It was
 * lifted out of `GameScreen.kt` rather than re-implemented for the tutorial precisely so the
 * [SoundParams.REMOVAL_LAYER_DELAY_MS] layering and the `runIndex` pitch ladder exist exactly
 * once and cannot drift apart. It lives *beside* the screens and never inside `BoardCanvas`:
 * putting a cue on the shared board leaf would give audio to every screen that draws a board
 * at once, which §5's negative contract forbids.
 *
 * Lifting it changes none of §5.1's four prohibitions (`ux-spec.md:864-877`): it takes a
 * [MoveTransition] and reads [LocalSoundPlayer], and touches no `GameViewModel`, no
 * repository, no navigation and no persistence.
 *
 * **The cue takes no enable/disable gate parameter, and that asymmetry with `JumpHapticEffect` is
 * deliberate.** `hapticsOn` is carried in `GameUiState` because a haptic can only ever be
 * blocked *before* it fires. Audio cannot: SOUND-05 requires silencing one-shots that are
 * already in flight, which only the object holding the live stream ids can do. The audio gate
 * therefore lives inside the player implementation, which subscribes to the persisted setting
 * itself. Do not "fix" this by copying the `hapticsOn` plumbing — a UI-state flag can block
 * future calls and nothing else, and adding one would give the mute two disagreeing sources
 * of truth.
 */
@Composable
internal fun JumpSoundEffect(transition: MoveTransition?) {
    val sound = LocalSoundPlayer.current

    // The removal layer runs on the composition scope, NOT inside the keyed effect below —
    // the same reason, and the same shape, as `BoardCanvas`' landing squash
    // (`BoardCanvas.kt:129-134`).
    //
    // `onAnimationFinished()` clears `animating`, which changes this effect's key to null and
    // cancels its coroutine. With OS animations ON that happens after the ~200ms slide, long
    // past the 60ms this cue waits, so the pair sounded. With animations OFF `runJumpAnimation`
    // takes the snap path and clears `animating` in the same frame, cancelling the effect
    // INSIDE its delay: the pitched pop played and the removal layer never did. Measured on
    // device as one audio event per jump instead of two, on the tutorial and on a real board
    // (qa-engineer F-2, criteria 7c/25 — "sound is unaffected by this switch").
    //
    // Only the DELAYED cue moves. The key stays `transition?.id` and the jump still plays
    // inside the keyed effect, because the null gap between two jumps is load-bearing: the id
    // is the move ordinal and RECYCLES after an undo, so a replayed jump carries an id this
    // effect has already seen and is re-fired only by the key having passed through null in
    // between. Latching the last non-null transition would survive the snap path and silence
    // the replay instead — which is why
    // `GameScreenSoundTest.aJumpReplayedAfterAnUndoSoundsAgainEvenThoughItsMoveIdIsReused`
    // exists.
    //
    // The scope dies with the composition, so leaving the screen inside those 60ms still
    // drops the layer — the one case where dropping it is right.
    val layerScope = rememberCoroutineScope()
    LaunchedEffect(transition?.id) {
        if (transition != null) {
            sound.playJump(runIndex = transition.runIndex)
            layerScope.launch {
                delay(SoundParams.REMOVAL_LAYER_DELAY_MS.toLong())
                sound.play(SoundCue.PegRemove)
            }
        }
    }
}
