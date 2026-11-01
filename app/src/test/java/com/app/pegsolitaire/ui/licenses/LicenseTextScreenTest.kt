package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Failed
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loaded
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The License text screen (spec §4, LS-08; LS-AC05, LS-AC10, LS-AC14): one `Column` body in one
 * `SelectionContainer`, one node per paragraph, a part heading over each text of a two-text entry,
 * "End of text" last; the error state under the entry's name, or under "Licenses" for an id the
 * catalogue does not know.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEXT_SCREEN_ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher for the ViewModel's re-flow
class LicenseTextScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()
    private val fredoka get() = requireNotNull(LicenseCatalog.entry("fredoka"))
    private val jakarta get() = requireNotNull(LicenseCatalog.entry("jakarta-inject"))

    /**
     * One node per paragraph, all inside the one tagged `SelectionContainer` — the structural half
     * of "a selection can span two paragraphs" (brief §4.1); the gesture itself is QA's §8.2 item 8.
     */
    @Test
    fun theBodyIsOneColumnWithOneNodePerParagraphInOneSelectionContainer() {
        render(Loaded(Bundled(fredoka), listOf(LoadedTextPart(null, PARAGRAPHS))))

        PARAGRAPHS.forEach { paragraph ->
            val node = composeRule.onNodeWithText(paragraph, useUnmergedTree = true).fetchSemanticsNode()
            assertWithMessage(paragraph).that(hasBodyAncestor(node)).isTrue()
        }
    }

    @Test
    fun aTwoTextEntryShowsNoticeThenItsLicenceHeadingInThatOrder() {
        val notice = LoadedTextPart(R.string.license_text_part_notice, listOf(NOTICE_PARAGRAPH))
        val licence = LoadedTextPart(R.string.licenses_apache_name, listOf(LICENCE_PARAGRAPH))
        render(Loaded(Bundled(jakarta), listOf(notice, licence)))

        val headings = headingTexts()
        assertThat(headings.first()).isEqualTo(app.getString(R.string.licenses_apache_name))
        assertThat(headings.drop(2))
            .containsExactly(
                app.getString(R.string.license_text_part_notice),
                app.getString(R.string.licenses_apache_name),
            ).inOrder()
        assertThat(top(NOTICE_PARAGRAPH)).isLessThan(top(LICENCE_PARAGRAPH))
    }

    @Test
    fun endOfTextIsTheLastNode() {
        render(Loaded(Bundled(fredoka), listOf(LoadedTextPart(null, PARAGRAPHS))))

        val end = top(app.getString(R.string.license_text_end))
        PARAGRAPHS.forEach { assertThat(top(it)).isLessThan(end) }
    }

    @Test
    fun aLoadedTextScrollsToEndOfText() {
        val long = (1..LONG_TEXT_PARAGRAPHS).map { "Paragraph $it of a long licence text." }
        render(Loaded(Bundled(fredoka), listOf(LoadedTextPart(null, long))))

        composeRule.onNodeWithText(app.getString(R.string.license_text_end)).performScrollTo().assertIsDisplayed()
    }

    /** LS-AC10's error state, through the real ViewModel over a source whose read fails. */
    @Test
    fun aFailedReadShowsTheErrorUnderTheEntryTitle() {
        val viewModel =
            LicenseTextViewModel(
                savedStateHandle = SavedStateHandle(mapOf("entryId" to "fredoka")),
                source = FakeLicenseTextSource(),
                defaultDispatcher = UnconfinedTestDispatcher(),
            )
        composeRule.setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsState()
                LicenseTextScreen(state = state, onBack = {})
            }
        }
        composeRule.waitForIdle()

        val error = app.getString(R.string.licenses_text_error)
        composeRule.onNodeWithText(error).assertIsDisplayed()
        assertThat(top(app.getString(R.string.licenses_fredoka_title))).isLessThan(top(error))
        composeRule.onNodeWithText(app.getString(R.string.licenses_ofl_name)).assertIsDisplayed()
    }

    @Test
    fun anUnknownIdShowsTheErrorUnderTheLicensesTitle() {
        render(Failed(header = null))

        composeRule.onNodeWithText(app.getString(R.string.licenses_title)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.licenses_text_error)).assertIsDisplayed()
    }

    private fun render(state: LicenseTextUiState) {
        composeRule.setContent { AppTheme { LicenseTextScreen(state = state, onBack = {}) } }
        composeRule.waitForIdle()
    }

    private fun top(text: String): Float =
        composeRule
            .onNodeWithText(text, substring = false, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun headingTexts(): List<String> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.top }
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    private fun hasBodyAncestor(node: SemanticsNode): Boolean =
        generateSequence(node.parent) { it.parent }
            .any { it.config.getOrNull(SemanticsProperties.TestTag) == LICENSE_TEXT_BODY_TAG }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TEXT_SCREEN_ROBOLECTRIC_SDK = 34

private const val NOTICE_PARAGRAPH = "Notices for Eclipse Jakarta Dependency Injection."
private const val LICENCE_PARAGRAPH = "Apache License, Version 2.0, January 2004."
private const val LONG_TEXT_PARAGRAPHS = 60
private val PARAGRAPHS = listOf("First paragraph.", "Second paragraph.", "Third paragraph.")
