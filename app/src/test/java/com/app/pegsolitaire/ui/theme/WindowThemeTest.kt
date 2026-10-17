package com.app.pegsolitaire.ui.theme

import com.app.pegsolitaire.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The window theme asks for status-bar icons that stand out against the app's background, in
 * both modes (tech-lead phase-15 sign-off, Decision 3; qa-engineer loop 3, F-2).
 *
 * ## What this guards
 *
 * Edge to edge, the status bar has no colour of its own. The app's background shows through
 * it, and the window decides only whether the clock and icons draw dark or light. The parent,
 * `android:Theme.Material.Light.NoActionBar`, does not ask for dark icons. So in the light theme
 * they drew white on the near-white background, about 1.1:1, on every screen (F-2).
 *
 * The light test fails when `res/values/themes.xml` does not ask for dark icons. The dark test
 * fails when `res/values-night/themes.xml` is missing or loses its item: the night window would
 * then inherit the light theme's dark icons and draw them on the dark background.
 *
 * Each qualifier is the exact one the window gets. Dark mode follows the system, because the
 * app calls `AppTheme(darkTheme = isSystemInDarkTheme())` and only previews pass the parameter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WINDOW_THEME_ROBOLECTRIC_SDK])
class WindowThemeTest {
    @Test
    @Config(qualifiers = LIGHT_MODE_QUALIFIER)
    fun theLightWindowThemeAsksForDarkStatusBarIcons() {
        assertThat(windowLightStatusBar(whenUnset = false)).isTrue()
    }

    @Test
    @Config(qualifiers = DARK_MODE_QUALIFIER)
    fun theDarkWindowThemeAsksForLightStatusBarIcons() {
        assertThat(windowLightStatusBar(whenUnset = true)).isFalse()
    }

    /**
     * Resolves [R.style.Theme_PegSolitaire] as the window does, on a fresh theme over the app's
     * resources under the test's qualifier, and reads `android:windowLightStatusBar`.
     *
     * [whenUnset] is always the wrong answer for the caller, so a theme chain that sets the
     * attribute nowhere cannot pass.
     */
    private fun windowLightStatusBar(whenUnset: Boolean): Boolean {
        val theme = RuntimeEnvironment.getApplication().resources.newTheme()
        theme.applyStyle(R.style.Theme_PegSolitaire, true)
        val resolved = theme.obtainStyledAttributes(STATUS_BAR_ATTRIBUTES)
        return try {
            resolved.getBoolean(LIGHT_STATUS_BAR, whenUnset)
        } finally {
            resolved.recycle()
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val WINDOW_THEME_ROBOLECTRIC_SDK = 34

/** The system's light mode. `res/values/` answers it. */
private const val LIGHT_MODE_QUALIFIER = "notnight"

/** The system's dark mode. `res/values-night/` answers it. */
private const val DARK_MODE_QUALIFIER = "night"

/** The one attribute this test reads from the resolved theme. */
private val STATUS_BAR_ATTRIBUTES = intArrayOf(android.R.attr.windowLightStatusBar)

/** The position of `android:windowLightStatusBar` in [STATUS_BAR_ATTRIBUTES]. */
private const val LIGHT_STATUS_BAR = 0
