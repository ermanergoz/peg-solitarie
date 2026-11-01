package com.app.pegsolitaire.ui.window

import android.os.Build
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
 * From API 30 the value is the host's own, `systemBarsIgnoringVisibility` unioned with `displayCutout`.
 * Below API 30 it is read from the window's root insets instead ([RootStableSystemInsets]), because
 * androidx's emulated bar animation corrupts the host's own stable values there; it is the same value,
 * the platform's stable insets, which the edge-to-edge window `MainActivity` asks for defines
 * (§3.8 (e)). No screen has a text field, so no IME is unioned.
 *
 * `systemBarsIgnoringVisibility` is still `@ExperimentalLayoutApi` in the pinned foundation-layout; the
 * opt-in on [stableSystemFromTheHost] is the app's one opt-in to it.
 */
val WindowInsets.Companion.stableSystem: WindowInsets
    @Composable
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stableSystemFromTheHost()
        } else {
            rememberStableSystemFromTheRoot()
        }

/** API 30 and up: the host's own stable bar insets and cutout, which the platform's bar animation keeps exact. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun stableSystemFromTheHost(): WindowInsets =
    WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout)

/**
 * Below API 30: the window's root insets, read again at every inset dispatch to the host (see
 * [RootStableSystemInsets] for why the host's own stable values cannot be used there).
 */
@Composable
private fun rememberStableSystemFromTheRoot(): WindowInsets {
    val view = LocalView.current
    val dispatched = WindowInsets.systemBars
    return remember(view, dispatched) {
        RootStableSystemInsets(rootInsets = { ViewCompat.getRootWindowInsets(view) }, dispatched = dispatched)
    }
}

/**
 * The bars' stable insets and the cutout as the window's **root** insets report them, for API 24–29.
 *
 * Below API 30 androidx emulates the bars' slide for the Compose host, and the emulation hands the
 * host's "ignoring visibility" values the slide's interpolated insets, which carry no stable value: on
 * an API 27 device they fell from 63/126 px to 0/0 as the bars hid, and the bottom stayed 0 after they
 * came back, while the root insets held 63/126 px throughout (the developer's report for D-05g). The
 * root insets are never part of that emulation, and with the edge-to-edge window's
 * `SYSTEM_UI_FLAG_LAYOUT_STABLE` the platform keeps reporting each bar's room there while it is hidden.
 *
 * Each read reads [dispatched] first, the host's visible bar insets, only so that the layout doing the
 * read is invalidated by the host's next dispatch — every hide, show and slide; its value is not used.
 * A read allocates the root's compat insets, a few times per layout pass and only below API 30.
 *
 * @param rootInsets the window's root insets, or `null` before the host is attached.
 * @param dispatched the host's visible bar insets, observed and never used as a value.
 */
internal class RootStableSystemInsets(
    private val rootInsets: () -> WindowInsetsCompat?,
    private val dispatched: WindowInsets,
) : WindowInsets {
    private fun read(density: Density): Insets {
        dispatched.getTop(density)
        val root = rootInsets() ?: return Insets.NONE
        return Insets.max(
            root.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()),
            root.getInsets(WindowInsetsCompat.Type.displayCutout()),
        )
    }

    override fun getLeft(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int = read(density).left

    override fun getTop(density: Density): Int = read(density).top

    override fun getRight(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int = read(density).right

    override fun getBottom(density: Density): Int = read(density).bottom
}
