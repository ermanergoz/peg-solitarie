package com.app.pegsolitaire.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.app.pegsolitaire.ui.game.MoveTransition
import kotlinx.coroutines.delay

/**
 * Plays the pitched jump cue as each jump lands, then layers the peg-removal cue under it
 * (SOUND-01/SOUND-02).
 *
 * Keyed on [MoveTransition.id] — the same move ordinal `JumpHaptic` uses — so it fires
 * exactly once per jump rather than once per recomposition. The pitch comes from
 * [MoveTransition.runIndex], the same-peg ladder step (D-05/D-06); because that index is
 * derived from the move history rather than stored, undoing and replaying a jump replays
 * its original pitch (D-08) with nothing to keep in sync here.
 *
 * Every jump removes exactly one peg, so the two cues always co-occur; the removal trails
 * by [SoundParams.REMOVAL_LAYER_DELAY_MS] so it reads as a texture layer beneath the pitched
 * pop instead of a second competing hit.
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
 * **The cue takes no enable/disable gate parameter, and that asymmetry with `JumpHaptic` is
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
    LaunchedEffect(transition?.id) {
        if (transition != null) {
            sound.playJump(runIndex = transition.runIndex)
            delay(SoundParams.REMOVAL_LAYER_DELAY_MS.toLong())
            sound.play(SoundCue.PegRemove)
        }
    }
}
