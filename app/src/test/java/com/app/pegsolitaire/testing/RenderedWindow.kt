package com.app.pegsolitaire.testing

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * The Compose host as pixels, for tests that must read what a screen **draws** rather than what
 * its semantics tree reports: a fill, a ring, a shadow, an edge. Both need
 * `@GraphicsMode(GraphicsMode.Mode.NATIVE)`: Robolectric's default LEGACY mode stubs every
 * `Canvas` call, so a LEGACY frame is blank and an assertion against it can only lie.
 *
 * There are two ways to take the frame, and they differ in exactly one thing a test may need.
 *
 * - [renderedWindow] draws the host view into a bitmap `Canvas`. That canvas is not hardware
 *   accelerated, so Compose takes its software path and draws every layer **without its
 *   elevation shadow**: fills are exact and edges are crisp, which is what an edge or a colour
 *   read wants.
 * - [renderedWindowWithShadows] asks Robolectric's `PixelCopy` for the window. Robolectric renders
 *   that copy through a real `HardwareRenderer`, lit by the theme's light source, so **elevation
 *   shadows are drawn** — the only way a JVM test can tell a raised clay disc from a flat one.
 *
 * Neither goes through Compose's `captureToImage()`, which first forces a redraw and waits for it
 * — a redraw Robolectric's paused looper never delivers. `PixelCopy` itself renders synchronously
 * under Robolectric and only posts its result.
 *
 * Both return the host's own pixels, so a node's `boundsInRoot` indexes them directly.
 */
fun ComposeContentTestRule.renderedWindow(): Bitmap {
    waitForIdle()
    val host = composeHost()
    return Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888).also { host.draw(Canvas(it)) }
}

/** The host as the window's hardware renderer composites it, elevation shadows included (see [renderedWindow]). */
fun ComposeContentTestRule.renderedWindowWithShadows(): Bitmap {
    waitForIdle()
    val host = composeHost()
    val window = checkNotNull(host.hostActivity()) { "the Compose host is not in an Activity window" }.window
    val decor = window.decorView
    val whole = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
    var result = PIXEL_COPY_PENDING
    PixelCopy.request(window, whole, { result = it }, Handler(Looper.getMainLooper()))
    // The copy is already in `whole`; idling delivers the posted result, so a failed copy fails
    // here instead of passing for a blank frame.
    waitForIdle()
    check(result == PixelCopy.SUCCESS) { "PixelCopy of the window failed with result $result" }
    val origin = IntArray(2).also(host::getLocationInWindow)
    return Bitmap.createBitmap(whole, origin[0], origin[1], host.width, host.height)
}

private fun View.hostActivity(): Activity? =
    generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()

/** No `PixelCopy` result has arrived yet; every real result code is zero or positive. */
private const val PIXEL_COPY_PENDING = -1
