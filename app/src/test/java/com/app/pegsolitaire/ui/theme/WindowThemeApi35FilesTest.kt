package com.app.pegsolitaire.ui.theme

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The window theme's API 35 pair, read from its source files (ux-spec §6.2, D-05f; tech-lead defect
 * ruling §1, B-1).
 *
 * The app targets API 36, so from API 35 the window is edge to edge whatever the theme says, and the
 * platform honours `android:windowLightNavigationBar` only while the theme names a navigation-bar
 * colour. A theme that names none leaves the bar to the window background's luminance, and the
 * background Material.Light gives the window is near-white in both modes, so dark mode got a forced
 * light bar (B-1). Each v35 file therefore restates the whole style with three items: the status-bar
 * icons, the navigation buttons, and the bar's transparent colour, which turns the forcing off.
 *
 * This pins the mechanism, not the behaviour. Reading the bar as the window resolves it on API 35
 * would take an android-all jar for 35, which this machine does not hold and a test may not
 * download, so the bar itself is the device's to read (criterion 29 (2)). Below API 35,
 * `WindowThemeTest` resolves the window theme.
 */
class WindowThemeApi35FilesTest {
    /**
     * The repository root: the nearest ancestor of the working directory holding
     * `settings.gradle.kts`, so the read behaves the same from the IDE and from Gradle.
     */
    private val repositoryRoot: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, ROOT_MARKER).isFile }
            ?: error("No ancestor of ${File(".").absolutePath} contains $ROOT_MARKER")
    }

    @Test
    fun theLightApi35WindowThemeNamesATransparentBarUnderDarkButtons() {
        assertTheWholeStyle(
            path = LIGHT_API_35_THEMES,
            items =
                listOf(
                    WINDOW_LIGHT_STATUS_BAR to "true",
                    WINDOW_LIGHT_NAVIGATION_BAR to "true",
                    NAVIGATION_BAR_COLOR to TRANSPARENT,
                ),
        )
    }

    @Test
    fun theDarkApi35WindowThemeNamesATransparentBarUnderLightButtons() {
        assertTheWholeStyle(
            path = DARK_API_35_THEMES,
            items =
                listOf(
                    WINDOW_LIGHT_STATUS_BAR to "false",
                    WINDOW_LIGHT_NAVIGATION_BAR to "false",
                    NAVIGATION_BAR_COLOR to TRANSPARENT,
                ),
        )
    }

    /**
     * The file at the repository-relative [path] holds one style, `Theme.PegSolitaire` on the base
     * files' parent, with exactly [items] as its items, each name once.
     */
    private fun assertTheWholeStyle(
        path: String,
        items: List<Pair<String, String>>,
    ) {
        val file = File(repositoryRoot, path)
        assertWithMessage("whether $path exists").that(file.isFile).isTrue()
        val styles =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName(STYLE)
        assertWithMessage("the styles in $path").that(styles.length).isEqualTo(1)
        val style = styles.item(0) as Element
        assertWithMessage("the style's name in $path").that(style.getAttribute(NAME)).isEqualTo(THEME_NAME)
        assertWithMessage("the style's parent in $path").that(style.getAttribute(PARENT)).isEqualTo(THEME_PARENT)
        val itemElements = style.getElementsByTagName(ITEM)
        val read =
            (0 until itemElements.length)
                .map { index -> itemElements.item(index) as Element }
                .map { item -> item.getAttribute(NAME) to item.textContent.trim() }
        assertWithMessage("the items of $path").that(read).containsExactlyElementsIn(items)
    }
}

private const val ROOT_MARKER = "settings.gradle.kts"

/** The two files, light and dark, that hold the window theme from API 35 on. */
private const val LIGHT_API_35_THEMES = "app/src/main/res/values-v35/themes.xml"
private const val DARK_API_35_THEMES = "app/src/main/res/values-night-v35/themes.xml"

/** The style the manifest gives the window, and the parent the base files give it. */
private const val THEME_NAME = "Theme.PegSolitaire"
private const val THEME_PARENT = "android:Theme.Material.Light.NoActionBar"

/** The three items each file must carry, and the platform's own transparent colour. */
private const val WINDOW_LIGHT_STATUS_BAR = "android:windowLightStatusBar"
private const val WINDOW_LIGHT_NAVIGATION_BAR = "android:windowLightNavigationBar"
private const val NAVIGATION_BAR_COLOR = "android:navigationBarColor"
private const val TRANSPARENT = "@android:color/transparent"

/** The resource file's element and attribute names. */
private const val STYLE = "style"
private const val ITEM = "item"
private const val NAME = "name"
private const val PARENT = "parent"
