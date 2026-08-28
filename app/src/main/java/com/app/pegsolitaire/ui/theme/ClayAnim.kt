package com.app.pegsolitaire.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Shared Candy-Clay motion tokens (DESIGN-SYSTEM.md §Motion).
 *
 * Durations are unitless `Int` ms and scales/alphas unitless `Float`s (not
 * dp/sp), so they live in this scoped object rather than `dimens.xml`
 * (QUAL-03). Board-specific ratios/durations stay in `BoardMetrics`/`GameAnim`;
 * this object owns the cross-screen clay juice: squish presses, staggered
 * entrances, celebration pops and ambient blob drift.
 */
object ClayAnim {
    /** Every tappable clay surface scales to this while pressed. */
    const val PRESSED_SCALE = 0.94f

    /** The resting (released) squish scale. */
    const val IDLE_SCALE = 1f

    /** Quick press-in so the squish reads as immediate contact. */
    const val PRESS_IN_MS = 90

    /** Fade+slide-up entrance duration for cards/tiles (micro-range 150–300ms). */
    const val ENTRANCE_MS = 240

    /** Per-item stagger for catalog cards + home tiles. */
    const val ENTRANCE_STAGGER_MS = 40

    /** One full ambient background-blob drift cycle (~8s, DESIGN-SYSTEM.md Home). */
    const val BLOB_DRIFT_MS = 8000

    /** Low alpha of the two ambient home background blobs. */
    const val BLOB_ALPHA = 0.10f

    /** Sequential star pop duration on Results. */
    const val STAR_POP_MS = 260

    /**
     * Stagger between the Results stars popping in — and, read through
     * [com.app.pegsolitaire.ui.results.StarPopSounds], the spacing of the star pops that
     * land with them. One token drives both on purpose: the sound is not "near" its star,
     * it is *on* it, and forking the two is how they drift apart.
     *
     * Raised from 120 ms after the phase 7 audit ("let the stars be slower"), then to 260 ms
     * on a follow-up pass. At 120 ms each star arrived barely halfway through the previous
     * pop's [STAR_POP_MS] and the three read as one burst; 200 ms separated them but still
     * overlapped. Matching [STAR_POP_MS] exactly is the point the overlap reaches zero — each
     * star now begins as the previous one finishes, so the three are strictly sequential.
     * Keep the two equal if either is retuned; a stagger below [STAR_POP_MS] reintroduces
     * overlap, and one above it opens a silent gap mid-celebration.
     */
    const val STAR_STAGGER_MS = 260

    /** Results score count-up duration. */
    const val SCORE_COUNT_UP_MS = 600

    /** Entrance pop for the Results headline / "New best!" pill. */
    const val POP_IN_MS = 220

    /** Spring release of the squish: MediumBouncy damping at Medium stiffness. */
    fun releaseSpring(): SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** A bouncy spring for pop-in overshoot effects (legal dots, landing squash). */
    fun overshootSpring(): SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    /**
     * Overshoot easing for delayed pops (tween-driven so `delayMillis` staggering
     * stays on the frame clock — deterministic under the Compose test clock).
     */
    val OVERSHOOT_EASING: Easing =
        CubicBezierEasing(OVERSHOOT_X1, OVERSHOOT_Y1, OVERSHOOT_X2, OVERSHOOT_Y2)

    /** Standard deceleration for entrances (fade + slide-up). */
    val ENTRANCE_EASING: Easing =
        CubicBezierEasing(ENTRANCE_X1, ENTRANCE_Y1, ENTRANCE_X2, ENTRANCE_Y2)

    // Control points: EaseOutBack-style overshoot curve.
    private const val OVERSHOOT_X1 = 0.34f
    private const val OVERSHOOT_Y1 = 1.56f
    private const val OVERSHOOT_X2 = 0.64f
    private const val OVERSHOOT_Y2 = 1f

    // Control points: decelerate (ease-out) curve.
    private const val ENTRANCE_X1 = 0f
    private const val ENTRANCE_Y1 = 0f
    private const val ENTRANCE_X2 = 0.2f
    private const val ENTRANCE_Y2 = 1f
}
