package com.app.pegsolitaire.ui.onboarding

/**
 * Scoped motion + pacing constants for the tutorial (ux-spec §6.4, §10).
 *
 * Unitless milliseconds and counts, so they belong here and **not** in `dimens.xml` — that
 * file owns dp/sp dimensions only. Everything visual the tutorial reuses (`claySquish`,
 * `clayEntrance`, the selection pulse, the star pop) comes from `ClayAnim` and `GameAnim`
 * unchanged; nothing here invents a curve.
 */
object TutorialAnim {
    /**
     * The beat between a step's last jump landing and the next footprint installing, so the
     * player sees their own jump resolve before the board changes under them (§4.1).
     *
     * Comfortably past `GameAnim.JUMP_SLIDE_MS` (200) plus `GameAnim.REMOVAL_MS` (180), so
     * the board is visibly settled — not mid-animation — when it is replaced.
     *
     * It is **functional, not decorative**: unlike every other value here it still applies
     * when OS animations are off, because collapsing it would swap boards under the player's
     * finger (§10). `TutorialViewModel` therefore does not gate it on `rememberAnimationsEnabled`.
     */
    const val STEP_ADVANCE_DELAY_MS = 650

    /**
     * How long the tutorial waits, with no progress made, before the coach offers help
     * unprompted (§4.4 class 1). Long enough that a player who is reading the caption is not
     * interrupted, short enough that a stuck-feeling player is not left alone.
     */
    const val NUDGE_DELAY_MS = 4_000

    /**
     * One coach pulse. Matches `GameAnim.PULSE_MS` — the board's own selected-peg breathing
     * half-cycle — so the escalation reads as the board's existing language rather than a
     * second highlight vocabulary (§10).
     */
    const val COACH_PULSE_MS = 900

    /**
     * How many consecutive taps that make no progress trigger the single escalation (§4.4
     * class 3). It escalates **once** and never past this: more help, never less, and never a
     * block.
     */
    const val NON_PROGRESSING_TAPS_BEFORE_COACH = 3
}
