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
 * both modes (tech-lead phase-15 sign-off, Decision 3; qa-engineer loop 3, F-2), and for
 * navigation buttons that take the theme too (ux-spec §6.2, D-05f; criterion 29 part 1).
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
 *
 * ## The navigation bar (D-05f)
 *
 * In dark mode the three-button bar drew the platform's light grey scrim with dark buttons (15e
 * O-1). `android:windowLightNavigationBar` asks for dark buttons, and with them the light scrim, in
 * light mode, and light ones over the dark scrim in night. It is an API 27 attribute and minSdk is
 * 24, so it lives in `values-v27/` and `values-night-v27/`, each restating the whole style. At this
 * test's SDK both placements resolve alike, so where it lives is read in the diff and by lint's
 * `NewApi` check, not here; an API 26 case would need an android-all jar from the network.
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

    @Test
    @Config(qualifiers = LIGHT_MODE_QUALIFIER)
    fun theLightWindowThemeAsksForDarkNavigationButtons() {
        assertThat(windowFlag(android.R.attr.windowLightNavigationBar, whenUnset = false)).isTrue()
    }

    @Test
    @Config(qualifiers = DARK_MODE_QUALIFIER)
    fun theDarkWindowThemeAsksForLightNavigationButtons() {
        assertThat(windowFlag(android.R.attr.windowLightNavigationBar, whenUnset = true)).isFalse()
    }

    /** `android:windowLightStatusBar` as the window resolves it: see [windowFlag]. */
    private fun windowLightStatusBar(whenUnset: Boolean): Boolean =
        windowFlag(android.R.attr.windowLightStatusBar, whenUnset)

    /**
     * Resolves [R.style.Theme_PegSolitaire] as the window does, on a fresh theme over the app's
     * resources under the test's qualifier, and reads the boolean [attribute].
     *
     * [whenUnset] is always the wrong answer for the caller, so a theme chain that sets the
     * attribute nowhere cannot pass.
     */
    private fun windowFlag(
        attribute: Int,
        whenUnset: Boolean,
    ): Boolean {
        val theme = RuntimeEnvironment.getApplication().resources.newTheme()
        theme.applyStyle(R.style.Theme_PegSolitaire, true)
        val resolved = theme.obtainStyledAttributes(intArrayOf(attribute))
        return try {
            resolved.getBoolean(ONLY_ATTRIBUTE, whenUnset)
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

/** The position of the one attribute each read asks the resolved theme for. */
private const val ONLY_ATTRIBUTE = 0
