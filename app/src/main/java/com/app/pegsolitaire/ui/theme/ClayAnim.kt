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
 * entrances, celebration pops and the navigation fade.
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

    /**
     * The navigation cross-fade: the `NavHost`'s own default of navigation-compose 2.9.8 made
     * explicit on its four transitions, and the backdrop's sky cross-fade (backdrop spec B-13a,
     * the tech-lead's D2). One constant, so the two cannot drift apart.
     */
    const val NAV_TRANSITION_MS = 700

    /** Per-item stagger for catalog cards + home tiles. */
    const val ENTRANCE_STAGGER_MS = 40

    /**
     * How long one Results star takes to resolve: the duration `EarnedStar` hands to
     * `rememberPopIn`, overriding the shared [POP_IN_MS] default.
     *
     * Raised 260 -> 600 after a device check on the 1.65 s star-pop cue reported that the
     * animation speed no longer matched the sound. It did not: the stars had all settled
     * within about 1.5 s of entering Results while the last cue rang on to roughly 2.9 s, so
     * the celebration looked finished while it was still plainly sounding.
     *
     * **600 ms does not close that gap, and is not trying to.** Star 3 starts at
     * 2 x [STAR_STAGGER_MS] and its cue rings for 1.65 s beyond that, so ending together
     * would need a pop of roughly 1.9 s — which reads as sluggish rather than stately. The
     * aim is only to remove the *impression* that the picture has finished while the sound
     * continues, by keeping each star visibly resolving well into its own tail. Picture and
     * sound still do not end on the same frame, and no value here should be read as claiming
     * they do.
     *
     * Until this change the constant was declared but connected to nothing — every pop,
     * stars included, ran on [POP_IN_MS], so the stars' real duration was 220 ms and raising
     * this number alone would have changed nothing on screen. The value and its use site were
     * therefore fixed together.
     */
    const val STAR_POP_MS = 600

    /**
     * Stagger between the Results stars popping in — and, read through
     * [com.app.pegsolitaire.ui.results.StarPopSounds], the spacing of the star pops that
     * land with them. One token drives both on purpose: the sound is not "near" its star,
     * it is *on* it, and forking the two is how they drift apart.
     *
     * Raised 120 -> 200 -> 260 across the phase 7 audit ("let the stars be slower") and the
     * follow-up pass after it, then 260 -> 500 when the star-pop sample was replaced by the
     * 1.65 s freesound blip and the user picked that spacing by ear over the tighter 260 ms.
     * 500 ms is settled and has not moved since; the retune that followed was to
     * [STAR_POP_MS], not to this token.
     *
     * **Consecutive stars now overlap in motion, deliberately.** [STAR_POP_MS] is 600, so a
     * star is still resolving for its last 100 ms after the next one has begun. A device
     * check found the celebration looked finished while its 1.65 s cue was still ringing, and
     * lengthening each star past this stagger is how that was addressed.
     *
     * An earlier revision of this KDoc asserted the reverse — a deliberate 240 ms of
     * stillness between one star finishing and the next starting. That is void twice over:
     * [STAR_POP_MS] has since been raised past this token, and when the claim was written the
     * stars were not running on [STAR_POP_MS] at all but on the shared [POP_IN_MS], so its
     * arithmetic described no code that existed. Do not infer a gap from this token alone —
     * it is only half of the relationship, and [STAR_POP_MS] is the other half.
     */
    const val STAR_STAGGER_MS = 500

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
