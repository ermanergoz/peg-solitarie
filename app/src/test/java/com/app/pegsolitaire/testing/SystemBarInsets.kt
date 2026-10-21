package com.app.pegsolitaire.testing

import android.view.View
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Hands the composition the system-bar insets a device reports and Robolectric does not.
 *
 * ## Why every short-window layout assertion needs this
 *
 * A Compose test composes with **zero** window insets, so every node lands where the layout puts
 * it and a screen drawing under the status bar looks identical to one that respects it. That blind
 * spot is how a tutorial whose visible "Skip" was a no-op — 99 px of a 126 px target inside a
 * 142 px system-bar inset — reached a device with a green suite (F-1).
 *
 * It is also how a layout property can read "fits" on a window no player holds. `docs/ux-spec.md`
 * §13, "Two test idioms this refresh now forbids", states the second one: the narrow class's design
 * baseline is **568dp** of usable height at 360dp width (640 − 24 status − 48 three-button
 * navigation), not 640dp, and the compact band column lands within a few dp of 640 — so a
 * zero-inset reading says "fits" on exactly the window where the device does not. A reading taken
 * without insets is **neither a pass nor a fail** (criterion 15k).
 *
 * ## Why it dispatches at the Compose view and not the decor view
 *
 * Robolectric never delivers insets of its own, and dispatching them at the decor view does not
 * reach Compose: `WindowInsets.safeDrawing` installs its listener on the `ComposeView`, and the
 * shadowed `ViewRootImpl` above it does not forward. Dispatching straight at that view does,
 * through the same `ViewCompat` listener the Compose inset APIs read — so the values arrive exactly
 * as a real status and navigation bar's would, and an M3 `Scaffold` above the content subtracts
 * them into its `contentPadding` the way it does on glass.
 *
 * ## Each bar reported as its own type
 *
 * The status bar is dispatched as `statusBars()` with a top inset only and the navigation bar as
 * `navigationBars()` with a bottom inset only, exactly as a phone reports them. Their union is the
 * `systemBars()` (and `safeDrawing`) inset a `Scaffold` or `safeDrawingPadding()` reads, so a screen
 * that consumes the union sees what it always saw. What changes is the per-type read: an earlier
 * version set both values on the combined `systemBars()` type, which the platform stores on every
 * type in the mask — so `statusBarsPadding()` also received the navigation bar's bottom inset, and
 * a screen applying a bar twice (ux-spec §4.3.1, criterion 18b) measured a defect no device shows.
 *
 * Written for `TutorialScreenTest` in the phase's F-1 slice and lifted here the moment a second
 * screen needed it, rather than copied — two copies of an inset harness are two places for the
 * dispatched type or the listener target to drift.
 *
 * @param top the status-bar inset to report.
 * @param bottom the navigation-bar inset to report.
 */
fun ComposeContentTestRule.applySystemBarInsets(
    top: Dp,
    bottom: Dp,
) {
    val insets =
        WindowInsetsCompat
            .Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, insetPx(top), 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, insetPx(bottom)))
            .setVisible(WindowInsetsCompat.Type.systemBars(), true)
            .build()
    ViewCompat.dispatchApplyWindowInsets(composeHost(), insets)
    waitForIdle()
}

/**
 * The `AndroidComposeView` behind the semantics tree — the view the Compose inset APIs install
 * their listener on, and the view [renderedWindow] draws — reached through the tree rather than
 * through the Activity so a caller keeps `createComposeRule()`.
 */
internal fun ComposeContentTestRule.composeHost(): View = (onRoot().fetchSemanticsNode().root as ViewRootForTest).view

private fun ComposeContentTestRule.insetPx(dp: Dp): Int = with(density) { dp.roundToPx() }
