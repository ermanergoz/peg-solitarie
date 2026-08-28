package com.app.pegsolitaire.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [SoundPlayer] every composable reaches for. `MainActivity` provides the real,
 * Hilt-built one; everything below it just reads `LocalSoundPlayer.current`.
 *
 * `static` because the value never changes after that single provide — the cheaper
 * CompositionLocal form applies, and nothing re-composes when it is read.
 *
 * The default is [NoOpSoundPlayer], deliberately **not** a throwing "no player provided"
 * default, which is the other common shape for a CompositionLocal with no sane value. A
 * `@Preview`, a screenshot test or any composable rendered outside `MainActivity` must
 * render silently rather than crash; that is the same fail-open posture
 * [com.app.pegsolitaire.data.SettingsRepository] takes on a corrupt read (D-17). Silence
 * is a correct rendering of a game with no audio engine attached; a crash is not.
 */
val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> { NoOpSoundPlayer() }

/**
 * Wraps [onClick] so [cue] plays first, for the tappable surfaces that should make a
 * sound (D-11).
 *
 * A **per-callsite wrapper, not a `Modifier`.** The obvious-looking simplification is to
 * fold the cue into `Modifier.claySquish()`, since every tappable clay surface already has
 * it — and that is exactly why it must not go there. `claySquish()` is also on the top-bar
 * back buttons and the Settings switches, which D-09 requires to stay silent, so attaching
 * the cue to the modifier would blanket-apply it to precisely the controls D-09 excludes.
 * Opting *out* per callsite is the wrong default: a new silent control would start making
 * noise by omission. Keep the sound opt-in, and keep this out of `Clay.kt`.
 */
@Composable
fun withClickSound(
    cue: SoundCue,
    onClick: () -> Unit,
): () -> Unit {
    val sound = LocalSoundPlayer.current
    return remember(cue, onClick, sound) {
        {
            sound.play(cue)
            onClick()
        }
    }
}
