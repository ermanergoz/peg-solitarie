package com.app.pegsolitaire.ui.theme

import android.content.res.Resources
import com.app.pegsolitaire.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The window theme asks for status-bar icons that stand out against the app's background, in
 * both modes (tech-lead phase-15 sign-off, Decision 3; qa-engineer loop 3, F-2), and below API 35
 * leaves the navigation bar to the platform (ux-spec §6.2, D-05f; tech-lead defect ruling §1, B-1b).
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
 * ## The navigation bar (D-05f, B-1, B-1b)
 *
 * In dark mode the three-button bar drew a light grey scrim with dark buttons (15e O-1; B-1 on the
 * API 36 device). The app targets 36, so from API 35 the window is edge to edge whatever the theme
 * says, and the platform honours `android:windowLightNavigationBar` only while the theme names a
 * navigation-bar colour. So the item lives in `values-v35/` and `values-night-v35/`, beside a
 * transparent bar colour, and `WindowThemeApi35FilesTest` pins those two files: resolving them here
 * would take an android-all jar for API 35, which a test may not download, so the bar is the
 * device's to read (criterion 29 (2)).
 *
 * Below API 35 the window is not edge to edge, and the platform draws its bar opaque: black, on
 * Material.Light. A theme that asked for dark buttons there would put them on black in light mode
 * (B-1b), so below 35 the window theme sets no navigation attribute at all, and the bar is what it
 * was before D-05f, in both modes. The two navigation cases read that at this test's SDK.
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
    fun belowApi35TheLightWindowThemeLeavesTheNavigationButtonsToThePlatform() {
        assertWithMessage("whether the light window theme sets android:windowLightNavigationBar below API 35")
            .that(windowSets(android.R.attr.windowLightNavigationBar))
            .isFalse()
    }

    @Test
    @Config(qualifiers = DARK_MODE_QUALIFIER)
    fun belowApi35TheDarkWindowThemeLeavesTheNavigationButtonsToThePlatform() {
        assertWithMessage("whether the dark window theme sets android:windowLightNavigationBar below API 35")
            .that(windowSets(android.R.attr.windowLightNavigationBar))
            .isFalse()
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
        val resolved = windowTheme().obtainStyledAttributes(intArrayOf(attribute))
        return try {
            resolved.getBoolean(ONLY_ATTRIBUTE, whenUnset)
        } finally {
            resolved.recycle()
        }
    }

    /**
     * Whether the window theme, resolved as in [windowFlag], sets [attribute] anywhere in its
     * chain, the platform parent included.
     */
    private fun windowSets(attribute: Int): Boolean {
        val resolved = windowTheme().obtainStyledAttributes(intArrayOf(attribute))
        return try {
            resolved.hasValue(ONLY_ATTRIBUTE)
        } finally {
            resolved.recycle()
        }
    }

    /** [R.style.Theme_PegSolitaire] applied to a fresh theme over the app's resources. */
    private fun windowTheme(): Resources.Theme =
        RuntimeEnvironment.getApplication().resources.newTheme().apply {
            applyStyle(R.style.Theme_PegSolitaire, true)
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
