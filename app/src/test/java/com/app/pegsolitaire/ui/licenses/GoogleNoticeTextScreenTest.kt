package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Failed
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loaded
import com.app.pegsolitaire.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Google notice text screen (spec §4B, LS-08's exception; LS-AC10, LS-AC14, LS-AC15; brief
 * A1.6): the notice's name as the title, its sources and ordinal, then a lazy body one paragraph per
 * item — so Cronet's 2,497 paragraphs are never composed at once — scrolled to "End of text" through
 * the tagged list, which is the node that carries the scroll action.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [NOTICE_TEXT_ROBOLECTRIC_SDK])
class GoogleNoticeTextScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()
    private val endOfText get() = app.getString(R.string.license_text_end)

    @Test
    fun aFixtureTextLoadsAndScrollsToTheEnd() {
        render(loaded(paragraphs = 40))

        composeRule.onNodeWithText(NOTICE.name).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.google_notices_found_in, FOUND_IN)).assertIsDisplayed()
        composeRule.onNodeWithTag(GOOGLE_NOTICE_TEXT_LIST_TAG).performScrollToNode(hasText(endOfText))
        composeRule.onNodeWithText(endOfText).assertIsDisplayed()
    }

    @Test
    fun aTwoThousandFiveHundredParagraphNoticeOpensAtTheFirstAndScrollsToTheEnd() {
        render(loaded(paragraphs = CRONET_SHAPED_PARAGRAPHS))

        composeRule.onNodeWithText(paragraph(1)).assertIsDisplayed()
        composeRule.onAllNodesWithText(endOfText).assertCountEquals(0)
        composeRule.onNodeWithTag(GOOGLE_NOTICE_TEXT_LIST_TAG).performScrollToNode(hasText(endOfText))
        composeRule.onNodeWithText(endOfText).assertIsDisplayed()
    }

    @Test
    fun theOrdinalOfARepeatedNameFollowsTheSourcesLine() {
        render(Loaded(Notice(NOTICE, NoticeOrdinal(2, 4)), listOf(LoadedTextPart(null, listOf(paragraph(1))))))

        composeRule.onNodeWithText(app.getString(R.string.google_notices_ordinal, 2, 4)).assertIsDisplayed()
    }

    @Test
    fun anUnknownKeyShowsTheErrorUnderTheNoticeTitle() {
        render(Failed(header = null))

        composeRule.onNodeWithText(app.getString(R.string.google_notice_text_unknown_title)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.google_notice_text_error)).assertIsDisplayed()
    }

    private fun loaded(paragraphs: Int): LicenseTextUiState =
        Loaded(Notice(NOTICE, ordinal = null), listOf(LoadedTextPart(null, (1..paragraphs).map(::paragraph))))

    private fun paragraph(index: Int): String = "Paragraph $index of the notice."

    private fun render(state: LicenseTextUiState) {
        composeRule.setContent { AppTheme { GoogleNoticeTextScreen(state = state, onBack = {}) } }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val NOTICE_TEXT_ROBOLECTRIC_SDK = 34

/** The shape of Cronet's notice, the largest the build generates (LS-AC15). */
private const val CRONET_SHAPED_PARAGRAPHS = 2_500

private const val FOUND_IN = "play-services-cronet, ads-mobile-sdk"
private val NOTICE =
    GoogleNotice("000000000000000a", "Cronet", listOf("play-services-cronet", "ads-mobile-sdk"), "a000000000000000")
