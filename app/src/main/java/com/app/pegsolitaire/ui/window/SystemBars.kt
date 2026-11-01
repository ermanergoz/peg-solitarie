package com.app.pegsolitaire.ui.window

import android.view.Window
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The window's system-bar seam (docs/tech-design.md §3.8 (a); docs/ux-spec.md D-05g): applies a
 * [SystemBarsState] to a window. Idempotent: asking again for the state a window already has
 * changes nothing.
 *
 * **It takes a [Window], never an `Activity`.** `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity`
 * forbids `android.app.Activity` anywhere under `ui`, and a window is all the bars need: the host's
 * own, handed down by `MainActivity`, or a dialog's, read from the dialog's root view
 * ([MirrorSystemBars]).
 *
 * **It is not `platform/`.** That package holds the seams to the Play SDKs and to the foreground
 * Activity (§2.4). This is a framework type with no SDK and no Activity behind it, the same reason
 * the audio player lives beside the UI and not there.
 */
interface SystemBars {
    /** Asks [window] for [state]: both bars hidden with transient reveal, or both shown. */
    fun apply(
        window: Window,
        state: SystemBarsState,
    )
}

/** The seam that applies nothing: [LocalSystemBars]' default, so a preview or a test touches no window. */
object NoOpSystemBars : SystemBars {
    override fun apply(
        window: Window,
        state: SystemBarsState,
    ) = Unit
}

/**
 * The production seam, through `WindowInsetsControllerCompat`, which carries the request to every
 * API level the app supports (the legacy `SYSTEM_UI_FLAG`s and immersive-sticky below API 30).
 * Hidden sets the behaviour first, so the bars never hide with the default one; Shown brings them
 * back first and then restores the default behaviour. The controller is built from the window and
 * its decor view, never through the deprecated `ViewCompat` lookup.
 */
object WindowInsetsSystemBars : SystemBars {
    override fun apply(
        window: Window,
        state: SystemBarsState,
    ) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        when (state) {
            SystemBarsState.Hidden -> {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            SystemBarsState.Shown -> {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
    }
}

/**
 * The [SystemBars] every bar request goes through. `MainActivity` provides [WindowInsetsSystemBars];
 * a test provides a recording fake. The default is [NoOpSystemBars] and never a throwing "nothing
 * provided", for `LocalSoundPlayer`'s reason: a preview, or a composable rendered outside the
 * activity, must render and touch nothing rather than crash.
 */
val LocalSystemBars = staticCompositionLocalOf<SystemBars> { NoOpSystemBars }

/**
 * The bar state of the destination the host shows, for the windows that must mirror it: the
 * dialogs ([MirrorSystemBars]). The host provides the `State` it owns, never its value, so a
 * navigation recomposes only the readers that call `.value` (the backdrop's `LocalBackdropSceneReady`
 * idiom, docs/tech-design.md §3.5 R3). Outside the host it reads [SystemBarsState.Shown].
 */
val LocalSystemBarsState = staticCompositionLocalOf<State<SystemBarsState>> { ShownOutsideTheHost }

/** [LocalSystemBarsState]'s default: always [SystemBarsState.Shown]. */
private object ShownOutsideTheHost : State<SystemBarsState> {
    override val value: SystemBarsState get() = SystemBarsState.Shown
}
