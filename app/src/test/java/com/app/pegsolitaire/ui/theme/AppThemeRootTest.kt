package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * How the palette reaches the screens (docs/ux/colour-vision.md §3.3; the tech-lead's brief §1):
 * the root collects the holder's `StateFlow` once and hands the value to `AppTheme`, which
 * provides it through [LocalBoardMarks]. The reader here is a probe outside any board, so what it
 * records is the local itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [APP_THEME_ROOT_ROBOLECTRIC_SDK])
class AppThemeRootTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Property 2: a recreated Activity's first composition already reads the persisted palette —
     * `collectAsStateWithLifecycle` starts from the `StateFlow`'s value, not from a default — so a
     * configuration change never flashes the standard palette first.
     */
    @Test
    fun theFirstCompositionReadsTheHeldPaletteWithNoStandardFrameBeforeIt() {
        val seen = mutableListOf<BoardMarkPalette>()
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            AppThemeRoot(palette = MutableStateFlow(BoardMarkPalette.Colorblind)) { PaletteProbe(seen) }
        }

        assertThat(seen).isNotEmpty()
        assertThat(seen).containsNoneIn(listOf(BoardMarkPalette.Standard))
    }

    /** Property 3: a change in Settings reaches every reader in place — the content is not recreated. */
    @Test
    fun aChangeOfTheSettingReachesTheReaderWithoutRecreatingIt() {
        val seen = mutableListOf<BoardMarkPalette>()
        val instances = mutableSetOf<Any>()
        val palette = MutableStateFlow(BoardMarkPalette.Standard)
        composeRule.setContent {
            AppThemeRoot(palette = palette) { PaletteProbe(seen, instances) }
        }
        composeRule.waitForIdle()

        palette.value = BoardMarkPalette.Colorblind
        composeRule.waitForIdle()
        palette.value = BoardMarkPalette.Standard
        composeRule.waitForIdle()

        val runs =
            seen.fold(emptyList<BoardMarkPalette>()) { acc, read ->
                if (acc.lastOrNull() ==
                    read
                ) {
                    acc
                } else {
                    acc + read
                }
            }
        assertThat(runs)
            .containsExactly(BoardMarkPalette.Standard, BoardMarkPalette.Colorblind, BoardMarkPalette.Standard)
            .inOrder()
        assertThat(instances).hasSize(1)
    }

    /** `AppTheme` provides the marks of the palette it is given, in its own theme. */
    @Test
    fun theThemeProvidesTheMarksOfItsPaletteInItsTheme() {
        var dark: BoardMarks? = null
        var standardLight: BoardMarks? = null
        composeRule.setContent {
            AppTheme(darkTheme = true, boardMarkPalette = BoardMarkPalette.Colorblind) {
                val marks = LocalBoardMarks.current
                SideEffect { dark = marks }
            }
            AppTheme(darkTheme = false) {
                val marks = LocalBoardMarks.current
                SideEffect { standardLight = marks }
            }
        }
        composeRule.waitForIdle()

        assertThat(dark).isEqualTo(boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = true))
        assertThat(standardLight).isEqualTo(boardMarksFor(BoardMarkPalette.Standard, darkTheme = false))
    }

    /** Records the palette every composition reads, and the identity of the content it lives in. */
    @Composable
    private fun PaletteProbe(
        seen: MutableList<BoardMarkPalette>,
        instances: MutableSet<Any> = mutableSetOf(),
    ) {
        val palette = LocalBoardMarks.current.palette
        val instance = remember { Any() }
        SideEffect {
            seen += palette
            instances += instance
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val APP_THEME_ROOT_ROBOLECTRIC_SDK = 34
