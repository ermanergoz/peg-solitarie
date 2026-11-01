package com.app.pegsolitaire.ui.window

/**
 * What the window asks of the status and navigation bars (docs/ux-spec.md D-05g; docs/tech-design.md
 * §3.8 (a)). One state covers both bars: D-05g hides both or neither.
 */
enum class SystemBarsState {
    /** Both bars shown, with the platform's default behaviour: every screen but the two below. */
    Shown,

    /**
     * Both bars hidden; a swipe from the top or the bottom edge shows them for a moment as the
     * platform's translucent overlay, which goes away by itself. Gameplay and Results.
     */
    Hidden,
}
