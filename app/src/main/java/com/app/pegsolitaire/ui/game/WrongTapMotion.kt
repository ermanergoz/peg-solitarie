package com.app.pegsolitaire.ui.game

/**
 * The wrong tap's two motions as pure functions of the milliseconds since the tap
 * (`docs/ux/invalid-tap-feedback.md` §3.1): the shake of the tapped cell and the nudge of the
 * held peg's ring. Both run only with animations on; the reduced-motion mark is not a motion,
 * and the board holds it with a plain delay (see `rememberWrongTapFeedback`).
 *
 * Pure so that WT-7's keyframes are asserted exactly on the JVM, and so the board's draw phase
 * reads one number per frame from an elapsed-time clock instead of running two animations.
 * Nothing here allocates per call.
 */
internal object WrongTapMotion {
    /** The shake's keyframes in order: (fraction of the shake, multiple of the amplitude). */
    private val SHAKE_KEYFRAMES: List<Pair<Float, Float>> =
        listOf(
            SHAKE_START to SHAKE_REST,
            GameAnim.WRONG_TAP_SHAKE_KEY_1_AT to GameAnim.WRONG_TAP_SHAKE_KEY_1,
            GameAnim.WRONG_TAP_SHAKE_KEY_2_AT to GameAnim.WRONG_TAP_SHAKE_KEY_2,
            GameAnim.WRONG_TAP_SHAKE_KEY_3_AT to GameAnim.WRONG_TAP_SHAKE_KEY_3,
            GameAnim.WRONG_TAP_SHAKE_KEY_4_AT to GameAnim.WRONG_TAP_SHAKE_KEY_4,
            GameAnim.WRONG_TAP_SHAKE_KEY_5_AT to GameAnim.WRONG_TAP_SHAKE_KEY_5,
        )

    /**
     * How far right (positive) or left the tapped cell is drawn [elapsedMs] after the tap: through
     * the keyframes of [GameAnim.WRONG_TAP_SHAKE_MS], each segment on [GameAnim.WRONG_TAP_EASING],
     * and at rest before the tap and after the shake. Horizontal only, the same in LTR and RTL.
     *
     * @param amplitude the shake's amplitude A, in the unit the caller wants back (px on the board).
     */
    fun shakeOffset(
        elapsedMs: Float,
        amplitude: Float,
    ): Float {
        if (elapsedMs <= 0f || elapsedMs >= GameAnim.WRONG_TAP_SHAKE_MS) return 0f
        val fraction = elapsedMs / GameAnim.WRONG_TAP_SHAKE_MS
        val end = SHAKE_KEYFRAMES.indexOfFirst { it.first >= fraction }
        val (fromAt, from) = SHAKE_KEYFRAMES[end - 1]
        val (toAt, to) = SHAKE_KEYFRAMES[end]
        val eased = GameAnim.WRONG_TAP_EASING.transform((fraction - fromAt) / (toAt - fromAt))
        return (from + (to - from) * eased) * amplitude
    }

    /**
     * The held peg's ring radius, as a multiple of its resting radius, [elapsedMs] after the tap:
     * out to [BoardMetrics.WRONG_TAP_RING_NUDGE_SCALE] over [GameAnim.WRONG_TAP_RING_NUDGE_OUT_MS],
     * back to 1 over [GameAnim.WRONG_TAP_RING_NUDGE_BACK_MS]. It multiplies the breathing pulse.
     */
    fun ringNudgeScale(elapsedMs: Float): Float {
        val out = GameAnim.WRONG_TAP_RING_NUDGE_OUT_MS.toFloat()
        val back = GameAnim.WRONG_TAP_RING_NUDGE_BACK_MS.toFloat()
        val peak = BoardMetrics.WRONG_TAP_RING_NUDGE_SCALE
        return when {
            elapsedMs <= 0f || elapsedMs >= out + back -> RESTING_SCALE
            elapsedMs < out ->
                RESTING_SCALE + (peak - RESTING_SCALE) * GameAnim.WRONG_TAP_EASING.transform(elapsedMs / out)
            else ->
                peak + (RESTING_SCALE - peak) * GameAnim.WRONG_TAP_EASING.transform((elapsedMs - out) / back)
        }
    }

    /** The longer of the two motions, so the board knows when an animated answer is over. */
    val ANIMATED_MS: Int =
        maxOf(GameAnim.WRONG_TAP_SHAKE_MS, GameAnim.WRONG_TAP_RING_NUDGE_OUT_MS + GameAnim.WRONG_TAP_RING_NUDGE_BACK_MS)
}

/** The shake's first keyframe: the tap's own moment, the cell at rest. */
private const val SHAKE_START = 0f
private const val SHAKE_REST = 0f

/** A ring at its resting radius. */
private const val RESTING_SCALE = 1f
