package com.app.pegsolitaire.ui.backdrop

import com.app.pegsolitaire.ui.theme.ClayAnim

/**
 * The backdrop's own durations (backdrop spec §4.4), scoped beside [BackdropMetrics] as the
 * project splits constants by owner. The app-wide navigation fade stays in `ClayAnim`.
 *
 * Every loop is at least [MIN_LOOP_MS] (B-05a rule 1), and the game screen's waves are 1.5 times
 * slower than Home's (rule 5).
 */
object BackdropMotion {
    /** Home's back wave: one half-cycle of its bob. */
    const val LANDSCAPE_WAVE_BACK_HALF_MS = 12_000L

    /** Home's front wave: one half-cycle of its bob. */
    const val LANDSCAPE_WAVE_FRONT_HALF_MS = 9_000L

    /** The game screen's back wave: one half-cycle of its bob. */
    const val QUIET_WAVE_BACK_HALF_MS = 18_000L

    /** The game screen's front wave: one half-cycle of its bob. */
    const val QUIET_WAVE_FRONT_HALF_MS = 14_000L

    /** H1 to H9's rise-and-fade periods, in [BackdropMetrics.LANDSCAPE_LIGHTS]' order. */
    val LANDSCAPE_LIGHT_PERIODS_MS: List<Long> =
        listOf(30_000L, 24_000L, 36_000L, 26_000L, 34_000L, 40_000L, 28_000L, 32_000L, 22_000L)

    /** G1 to G5's rise-and-fade periods, in [BackdropMetrics.QUIET_LIGHTS]' order. */
    val QUIET_LIGHT_PERIODS_MS: List<Long> = listOf(36_000L, 42_000L, 30_000L, 44_000L, 38_000L)

    /** One gel sheen loop: a glint in its last quarter. */
    const val GEL_SHEEN_PERIOD_MS = 10_000L

    /** Classic's sheen leads: it passes with no delay into the loop (backdrop spec §3.5.2). */
    const val GEL_SHEEN_CLASSIC_DELAY_MS = 0L

    /** How far the Challenge tile's sheen follows Classic's. */
    const val GEL_SHEEN_CHALLENGE_DELAY_MS = 600L

    /** Home without input this long, and the clock settles (B-22, the tech-lead's A5). */
    const val BACKDROP_HOME_IDLE_MS = 60_000L

    /** The game screen without input this long, and the clock settles. */
    const val BACKDROP_GAME_IDLE_MS = 20_000L

    /** The ease of the clock's rate to 0 on settling, and back to 1 on waking. */
    const val BACKDROP_SETTLE_MS = 2_000L

    /** The floor every loop and half-cycle above keeps (B-05a rule 1). */
    const val MIN_LOOP_MS = 9_000L

    /**
     * Home's last `clayEntrance` order (backdrop spec §4.4): hero 0, ribbon 1, the Continue chips
     * 2–3, the mode tiles 4–5. Named here so `ui/backdrop/` imports nothing from `ui/home/` (the
     * tech-lead's J-4b A2); `BackdropSceneOpenTest` fails if Home lengthens its cascade (BD-23 (e)).
     */
    const val HOME_LAST_ENTRANCE_ORDER = 5

    /**
     * When the scene opens, counted from the gate's first awaited frame (B-11b): the end of Home's
     * longest entrance cascade, its last band's stagger plus one entrance. It elapses in
     * animator-scaled time, as the cascade does, so it is twice as long at scale 2 and ends on its
     * first frame at scale 0. It is not a loop, so [MIN_LOOP_MS] does not bind it.
     */
    const val BACKDROP_SCENE_OPEN_MS = ClayAnim.ENTRANCE_MS + HOME_LAST_ENTRANCE_ORDER * ClayAnim.ENTRANCE_STAGGER_MS

    /**
     * The scene's one fade in, at the open, linear (B-11b): the sky cross-fade's own length, defined
     * as that token so the two cannot drift apart (B-13a, the tech-lead's J-4b A2).
     */
    const val BACKDROP_SCENE_FADE_MS = ClayAnim.NAV_TRANSITION_MS
}
