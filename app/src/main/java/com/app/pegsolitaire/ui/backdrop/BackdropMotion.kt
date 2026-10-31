package com.app.pegsolitaire.ui.backdrop

/**
 * The backdrop's own durations (candy-backdrop.md §4.4), scoped beside [BackdropMetrics] as the
 * project splits constants by owner. The app-wide navigation fade stays in `ClayAnim`.
 *
 * Every loop is at least [MIN_LOOP_MS] (B-05a rule 1), and the game screen's waves are 1.5 times
 * slower than Home's (rule 5).
 */
object BackdropMotion {
    /** Home's back wave: one half-cycle of its bob. */
    const val LANDSCAPE_WAVE_BACK_HALF_MS = 1L

    /** Home's front wave: one half-cycle of its bob. */
    const val LANDSCAPE_WAVE_FRONT_HALF_MS = 1L

    /** The game screen's back wave: one half-cycle of its bob. */
    const val QUIET_WAVE_BACK_HALF_MS = 1L

    /** The game screen's front wave: one half-cycle of its bob. */
    const val QUIET_WAVE_FRONT_HALF_MS = 1L

    /** H1 to H9's rise-and-fade periods, in [BackdropMetrics.LANDSCAPE_LIGHTS]' order. */
    val LANDSCAPE_LIGHT_PERIODS_MS: List<Long> =
        listOf(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L)

    /** G1 to G5's rise-and-fade periods, in [BackdropMetrics.QUIET_LIGHTS]' order. */
    val QUIET_LIGHT_PERIODS_MS: List<Long> = listOf(1L, 1L, 1L, 1L, 1L)

    /** One gel sheen loop: a glint in its last quarter. */
    const val GEL_SHEEN_PERIOD_MS = 1L

    /** How far the Challenge tile's sheen follows Classic's. */
    const val GEL_SHEEN_CHALLENGE_DELAY_MS = 1L

    /** Home without input this long, and the clock settles (B-22, the tech-lead's A5). */
    const val BACKDROP_HOME_IDLE_MS = 1L

    /** The game screen without input this long, and the clock settles. */
    const val BACKDROP_GAME_IDLE_MS = 1L

    /** The ease of the clock's rate to 0 on settling, and back to 1 on waking. */
    const val BACKDROP_SETTLE_MS = 1L

    /** The floor every loop and half-cycle above keeps (B-05a rule 1). */
    const val MIN_LOOP_MS = 1L
}
