package com.app.pegsolitaire.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundParams
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

// The Results celebration helpers (DESIGN-SYSTEM.md Results). The two motion helpers
// return their animated value as a lambda so only the layer/text reading it re-runs, and
// both respect the OS animator-duration-scale gate: with decorative animation off they
// are settled on their final value from the first frame. The audio helper below is the
// deliberate exception to that gate (D-13) — see its KDoc for why the symmetry breaks.

/**
 * A one-shot overshoot pop-in scale, delayed by [delayMillis] for sequencing and running for
 * [durationMillis]. Returns the value as a lambda so only the layer that reads it re-runs.
 * When OS animations are off the scale is settled from the first frame.
 *
 * [durationMillis] defaults to the shared [ClayAnim.POP_IN_MS] entrance pop, which is what
 * the headline and the "New best!" pill use. `EarnedStar` overrides it with the much longer
 * [ClayAnim.STAR_POP_MS] so a star goes on resolving while its own 1.65 s cue rings — before
 * that override existed the stars ran on the default and [ClayAnim.STAR_POP_MS] drove nothing.
 */
@Composable
internal fun rememberPopIn(
    delayMillis: Int,
    durationMillis: Int = ClayAnim.POP_IN_MS,
): () -> Float {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) POP_START_SCALE else SETTLED_SCALE) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = SETTLED_SCALE,
            animationSpec =
                tween(
                    durationMillis = durationMillis,
                    delayMillis = delayMillis,
                    easing = ClayAnim.OVERSHOOT_EASING,
                ),
        )
    }
    return { scale.value }
}

/**
 * Counts a value up from zero to [target] over [ClayAnim.SCORE_COUNT_UP_MS], or lands on
 * [target] immediately when OS animations are off.
 */
@Composable
internal fun rememberCountUp(target: Int): () -> Int {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = remember { Animatable(if (animationsEnabled) COUNT_UP_START else target.toFloat()) }
    LaunchedEffect(target) {
        progress.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(ClayAnim.SCORE_COUNT_UP_MS, easing = ClayAnim.ENTRANCE_EASING),
        )
    }
    return { progress.value.toInt() }
}

/**
 * The celebration's audio beat: one star pop per earned star, climbing the star-pop ladder
 * as the stars land (SOUND-03, D-14's second beat). Emits no UI.
 *
 * The sequence waits [SoundParams.CELEBRATION_LEAD_IN_MS] first, so the ceremony reads as a
 * separate beat from the win stinger that already fired on the board as the winning move
 * landed (D-14), then fires [earned] pops spaced to land with the visual stars.
 *
 * **It plays only when [play] is true (D-16).** That latch is hoisted from
 * `ResultsUiState`, is claimed once per `SavedStateHandle`, and is the only thing standing
 * between a win and a re-entry: coming back to the same Results after a back navigation or
 * a process-death restore shows the stars silently.
 *
 * **This helper deliberately breaks the file's animator-duration-scale symmetry (D-13).**
 * The two motion helpers above settle instantly when OS animations are off. Audio cannot
 * follow them: collapsing the stagger to zero would fire every pop simultaneously — one
 * muddy chord instead of a celebration — so with the gate off the sequence falls back to
 * its own fixed [SoundParams.STAR_POP_STAGGER_MS] and stays a sequence. With animations on
 * the spacing is [ClayAnim.STAR_STAGGER_MS], read from the same token `EarnedStar` uses so
 * each pop lands with its star rather than merely near it; that value is never duplicated
 * here.
 *
 * **The pitch ladder is a prefix (D-15).** `playStarPop(step)` always starts at step 0 and
 * climbs, so a 2-star result plays notes 1-2 and the top resolving note is audibly earned
 * only by a 3-star finish. Centring or rescaling the ladder onto [earned] would hand every
 * result the same final note and throw that away — it is the design, not an off-by-one.
 *
 * @param earned how many stars this result earned — one pop each, in ascending pitch.
 * @param play the D-16 one-shot latch: whether THIS entry into Results should celebrate.
 */
@Composable
internal fun StarPopSounds(
    earned: Int,
    play: Boolean,
) {
    val sound = LocalSoundPlayer.current
    val animationsEnabled = rememberAnimationsEnabled()
    val staggerMillis =
        if (animationsEnabled) ClayAnim.STAR_STAGGER_MS else SoundParams.STAR_POP_STAGGER_MS
    LaunchedEffect(play, earned) {
        if (!play) return@LaunchedEffect
        delay(SoundParams.CELEBRATION_LEAD_IN_MS.toLong())
        repeat(earned) { step ->
            sound.playStarPop(step)
            delay(staggerMillis.toLong())
        }
    }
}

/** Pop-in animation endpoints. */
private const val POP_START_SCALE = 0f
private const val SETTLED_SCALE = 1f

/** Score count-up starting value. */
private const val COUNT_UP_START = 0f
