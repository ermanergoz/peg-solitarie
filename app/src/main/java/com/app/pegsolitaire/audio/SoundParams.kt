package com.app.pegsolitaire.audio

import kotlin.math.pow

/**
 * Audio tuning tokens: playback rates, per-cue volumes, celebration timings and the
 * `SoundPool` voice budget.
 *
 * Every value here is unitless — a `Float` rate multiplier, a `Float` linear volume in
 * `0f..1f`, an `Int` millisecond delay or an `Int` voice count. None of them is a dp/sp
 * measurement, so `dimens.xml` (which QUAL-03 reserves for UI dimensions, and which
 * `dpSpLiteralCheck` enforces) is the wrong home. They live in this scoped constants
 * object instead, exactly as [com.app.pegsolitaire.ui.game.GameAnim] and
 * [com.app.pegsolitaire.ui.theme.ClayAnim] do for motion timings. Scoped, not global:
 * this object owns audio and only audio.
 *
 * [jumpRate] and [starPopRate] are pure functions of an `Int` and are **total** for every
 * `Int`, negatives included — see their KDoc.
 */
object SoundParams {
    /** The sample's own recorded pitch: `SoundPool` rate 1.0 is the original frequency. */
    const val JUMP_BASE_RATE = 1.0f

    /** One equal-tempered semitone (2^(1/12)) — one rung per same-peg jump (D-05/D-06). */
    const val SEMITONE_STEP = 1.05946f

    /** `SoundPool`'s hard lower playback-rate bound (half speed, one octave down). */
    const val MIN_RATE = 0.5f

    /**
     * `SoundPool`'s hard upper playback-rate bound — one octave up (D-07a).
     *
     * D-07 asked for a ladder with no cap, but the engine's ceiling is 2.0x whatever the
     * caller passes, so "no cap" is not available. Clamping explicitly makes the ceiling a
     * documented decision rather than a silent engine surprise, and gives the ladder a
     * tested contract at the top end. The longest same-peg run in the repo's committed
     * golden solutions is 3, so canonical play never comes near it.
     */
    const val MAX_RATE = 2.0f

    /** A major third per star, so three stars outline a rising triad (D-15). */
    const val STAR_POP_STEP_SEMITONES = 4

    /**
     * Where the star ladder starts: two full rungs **below** the sample's own recorded pitch.
     *
     * Derived from [STAR_POP_STEP_SEMITONES] rather than written as a bare float, so the
     * two cannot drift. It hangs the triad *under* the sample rather than centring it on
     * it: a three-star finish plays a major sixth below the recorded pitch, a major third
     * below it, then the recorded pitch itself. Two stars stop a major third below, one
     * star two rungs below.
     *
     * Lowered twice, both times after the same complaint about pitch height. The phase 7
     * audit ("the star sound is too high pitched") moved the base off 1.0 to one rung down,
     * which centred the triad and left the third star a major third *above* the already
     * bright `glass_005` sample; the follow-up ("last beep on results screen is too high
     * pitched") moved it one rung further, so the top of the ladder is now the sample's own
     * unpitched sound.
     *
     * That ceiling is the point of the `2 *`: [starPopRate] can no longer return a rate
     * above 1.0 for any reachable star count, so the loudest, latest, most exposed cue in
     * the game can never play back faster than it was recorded and can never re-acquire the
     * sped-up edge both complaints were about. Raising the base again reintroduces it.
     *
     * Both moves transposed the whole triad and left its shape — a rising major third per
     * star — untouched, so 3 stars still resolve strictly higher than 2 (D-15). Narrowing
     * [STAR_POP_STEP_SEMITONES] would have flattened that climb instead, which is the
     * property D-15 is actually about, so the height was fixed and the shape left alone.
     */
    val STAR_POP_BASE_RATE: Float = SEMITONE_STEP.pow(-2 * STAR_POP_STEP_SEMITONES)

    /**
     * Gap between consecutive star pops **when animations are disabled** (D-13).
     *
     * This is the fallback stagger only. When animations are enabled the celebration
     * sequence takes its spacing from the Results star-stagger token in
     * [com.app.pegsolitaire.ui.theme.ClayAnim], so the audio lands with the visual pop;
     * that value is deliberately read from there and never duplicated here.
     *
     * Raised from 150 ms alongside that token's 120 -> 200, then to 325 ms alongside its
     * 200 -> 260 (the phase 7 audit's "let the stars be slower", and the follow-up pass
     * after it). Each step held this fallback at 1.25x the visual token, so it keeps the
     * slightly wider gap it always had — with no star landing to pace them, the pops need
     * a touch more air to read as three events — and the two paths still feel alike.
     */
    const val STAR_POP_STAGGER_MS = 325

    /** Gap between the win stinger and the first star pop, so D-14's two beats read as two. */
    const val CELEBRATION_LEAD_IN_MS = 250

    /** The removal cue trails the jump cue by this much, layering under it instead of doubling it. */
    const val REMOVAL_LAYER_DELAY_MS = 60

    /**
     * The `SoundPool` voice budget. Worst realistic concurrency is the win stinger plus
     * three star pops; above this `SoundPool` voice-steals by priority, then by age.
     */
    const val MAX_STREAMS = 8

    /** Priority of an ordinary gameplay/UI cue — the first thing voice stealing sacrifices. */
    const val PRIORITY_DEFAULT = 0

    /** Priority of the win stinger and star pops — never stolen for a jump. */
    const val PRIORITY_CELEBRATION = 1

    /** The signature jump pop, mixed loudest of the gameplay cues. */
    const val VOLUME_JUMP = 0.9f

    /** The jumped-peg removal: a texture layer under [VOLUME_JUMP], not a competing hit. */
    const val VOLUME_REMOVAL = 0.45f

    /** Peg selection: a light tick that must not compete with the jump. */
    const val VOLUME_SELECT = 0.6f

    /** Undo: audible as a deliberate action, below the jump. */
    const val VOLUME_UNDO = 0.7f

    /** Rejected tap: the quietest cue in the set — SOUND-02's "quiet, non-punishing". */
    const val VOLUME_INVALID = 0.3f

    /** Hint reveal: a chime at conversational level. */
    const val VOLUME_HINT = 0.6f

    /** The win stinger: the loudest moment in the game, and the reference all others sit under. */
    const val VOLUME_WIN = 1.0f

    /** Star pops: just under the stinger so the triad reads as its tail. */
    const val VOLUME_STAR_POP = 0.85f

    /** Forward/dismiss button clicks (D-11) — present but never the loudest thing on screen. */
    const val VOLUME_BUTTON = 0.7f

    /** The one-shot confirmation pop played when sound is switched on (D-12). */
    const val VOLUME_SOUND_ENABLED = 0.6f

    /**
     * The playback rate for the [runIndex]-th consecutive jump by the same peg: one
     * semitone per rung (D-05/D-06), ceilinged at [MAX_RATE] (D-07a).
     *
     * `runIndex` is derived from gameplay state and is not range-checked by its type, so
     * the [coerceIn] is a correctness requirement, not defensive noise (T-07-02 / ASVS
     * V5): it makes the function total for every `Int`, including a negative index, which
     * would otherwise map to an arbitrarily small rate.
     */
    fun jumpRate(runIndex: Int): Float = (JUMP_BASE_RATE * SEMITONE_STEP.pow(runIndex)).coerceIn(MIN_RATE, MAX_RATE)

    /**
     * The playback rate for the [step]-th star pop: a major third per star (D-15), on the
     * same clamped range as [jumpRate] and total for every `Int` for the same reason.
     */
    fun starPopRate(step: Int): Float =
        (STAR_POP_BASE_RATE * SEMITONE_STEP.pow(step * STAR_POP_STEP_SEMITONES))
            .coerceIn(MIN_RATE, MAX_RATE)
}
