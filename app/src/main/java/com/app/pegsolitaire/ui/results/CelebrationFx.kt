package com.app.pegsolitaire.ui.results

import com.app.pegsolitaire.ui.theme.ClayAnim

/**
 * The Results celebration layers' own numbers (ux-spec D-RC, §4.7, §6.4): unitless ms, fractions,
 * degrees and counts, scoped to the screen that owns the layers, so not `dimens.xml` and nothing
 * joins `ClayAnim`. The pieces' and the sway's dp sizes are `dimens.xml` tokens (§6.1); the rays'
 * peak alphas live with `ClayColors.celebrationRay` in `Clay.kt`, the one place its colour is decided
 * (§6.2); the idle and settle times are `BackdropMotion`'s, read from there and never copied.
 */
internal object CelebrationFx {
    /**
     * The stars on the row, earned and unearned: the one count the Results screen draws, speaks and
     * sequences by; the confetti waits for the last one to pop.
     */
    const val TOTAL_STARS = 3

    // ---- The rays (D-RC-1 to D-RC-3) ----

    /** Twelve wedges round the star row's centre. */
    const val RAY_COUNT = 12

    /** One wedge's width; wedge and gap are equal on the 30° pitch. */
    const val RAY_WEDGE_DEGREES = 15f

    /** A full turn, the pitch's numerator and the rotation's span. */
    const val FULL_TURN_DEGREES = 360f

    /** Full ray strength from the centre out to this fraction of the window's height. */
    const val RAY_CORE_RADIUS_FRACTION = 0.10f

    /** The ray has faded to nothing at this fraction of the window's height. */
    const val RAY_FADE_RADIUS_FRACTION = 0.60f

    /** Where the centre sits, as a fraction of the height, until the star row first reports. */
    const val RAY_FALLBACK_CENTRE_Y = 0.42f

    /** The centre's horizontal fallback: the middle of the window. */
    const val RAY_FALLBACK_CENTRE_X = 0.5f

    /** One clockwise revolution, linear: 7.5° a second, one pitch every 4 s. */
    const val RAY_ROTATION_PERIOD_MS = 48_000L

    /** One breath of a wedge's alpha. */
    const val RAY_SHIMMER_PERIOD_MS = 2_800L

    /** How far the breath dips: the multiplier runs 1 − depth … 1 (0.60 … 1.00). */
    const val RAY_SHIMMER_DEPTH = 0.40f

    /** The rest pose's angle: wedge 0's bisector straight up. */
    const val RAY_REST_ANGLE_DEGREES = 0f

    // ---- The confetti (D-RC-4, D-RC-5) ----

    /** Pieces in one burst, on every window size. */
    const val CONFETTI_PIECE_COUNT = 90

    /** The fall starts as the third star's pop begins, in animator-scaled time. */
    const val CONFETTI_START_MS = (TOTAL_STARS - 1) * ClayAnim.STAR_STAGGER_MS

    /** Pieces spawn over this span after the start. */
    const val CONFETTI_SPAWN_MS = 600

    /** Pieces spawn up to this fraction of the height above the top edge. */
    const val CONFETTI_SPAWN_BAND_FRACTION = 0.08f

    /** The quickest fall from spawn to below the bottom edge. */
    const val CONFETTI_FALL_MIN_MS = 1_800

    /** The slowest fall. */
    const val CONFETTI_FALL_MAX_MS = 2_600

    /** The layer leaves the composition this long after the start: the last spawn's slowest fall. */
    const val CONFETTI_TOTAL_MS = CONFETTI_SPAWN_MS + CONFETTI_FALL_MAX_MS

    /** The shortest sway period. */
    const val CONFETTI_SWAY_PERIOD_MIN_MS = 900

    /** The longest sway period. */
    const val CONFETTI_SWAY_PERIOD_MAX_MS = 1_500

    /** The fastest tumble, either way round, in revolutions a second. */
    const val CONFETTI_TUMBLE_MAX_RPS = 1.5f

    /** The quickest flip (the `cos` of the piece's x scale). */
    const val CONFETTI_FLIP_MIN_MS = 500

    /** The slowest flip. */
    const val CONFETTI_FLIP_MAX_MS = 900

    /** A piece's alpha until its fade. */
    const val CONFETTI_ALPHA = 0.92f

    /** The last fraction of a piece's fall over which it fades to nothing. */
    const val CONFETTI_FADE_FRACTION = 0.20f

    /** A strip's short side as a fraction of its long side `s`. */
    const val CONFETTI_STRIP_ASPECT = 0.45f

    /** A square's side as a fraction of `s`. */
    const val CONFETTI_SQUARE_SCALE = 0.70f

    /** A disc's diameter as a fraction of `s`. */
    const val CONFETTI_DISC_SCALE = 0.60f

    /** Colours in `ClayColors.confettiPalette`, drawn with equal weights. */
    const val CONFETTI_COLOUR_COUNT = 6

    /** The multiplier that folds the replay id's hash and the Challenge level into one seed. */
    const val CONFETTI_SEED_MULTIPLIER = 31
}
