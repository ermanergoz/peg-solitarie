package com.app.pegsolitaire.ui.window

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * The one inset every screen pads off (docs/ux-spec.md D-05g part 3, §6.2 "Immersive play";
 * docs/tech-design.md §3.8 (d)): the system bars' **stable** insets — the room each bar takes when it
 * is shown, read whether or not it is shown now — unioned with the display cutout, on every side.
 *
 * Gameplay and Results hide both bars, and a bar's **visible** inset (`systemBars`, `safeDrawing`,
 * `statusBars`, `navigationBars`, and Material 3's `Scaffold` and `TopAppBar` defaults, which read
 * them) is zero while it is hidden and animates while it slides. A screen laid out against it would
 * jump by the bar's height as the bars come and go; laid out against this, every screen is the same
 * picture with the bars shown, hidden or sliding, and the bars come and go as pixels only. Each
 * `Scaffold` passes it as its `contentWindowInsets`, each `TopAppBar` its top and horizontal sides as
 * its `windowInsets`, and the tutorial's column pads off it; `StableInsetsSourceTest` holds that, and
 * criteria 30d–30f the behaviour.
 *
 * Below API 30 the bars' stable insets are the platform's stable insets, which the edge-to-edge
 * window `MainActivity` asks for defines (§3.8 (e)). No screen has a text field, so no IME is unioned.
 *
 * `systemBarsIgnoringVisibility` is still `@ExperimentalLayoutApi` in the pinned foundation-layout; this
 * getter is the app's one opt-in to it.
 */
val WindowInsets.Companion.stableSystem: WindowInsets
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    get() = systemBarsIgnoringVisibility.union(displayCutout)
