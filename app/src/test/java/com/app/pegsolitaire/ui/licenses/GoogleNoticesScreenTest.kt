package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Google notices list (spec §4A, LS-12; LS-AC10, LS-AC13; brief A1.2, A1.5): every index line
 * is one tappable row in index order with its "Found in" line, a repeated name carries its
 * ordinal, a tap opens that row's key, and the list publishes its position semantics with the rows
 * counted alone. A malformed index is the error state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [NOTICES_ROBOLECTRIC_SDK], qualifiers = NOTICES_TALL_WINDOW)
class GoogleNoticesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()
    private val opened = mutableListOf<String>()

    @Test
    fun aFixtureIndexListsItsRowsAndOpensOne() {
        render(GoogleNoticesUiState.Loaded(ROWS))

        ROWS.groupBy { it.name }.forEach { (name, sameName) ->
            composeRule.onAllNodesWithText(name).assertCountEquals(sameName.size)
        }
        composeRule.onNodeWithText(OKHTTP.name).performClick()

        assertThat(opened).containsExactly(OKHTTP.key)
    }

    @Test
    fun showsEveryIndexLineInOrderWithItsFoundInLine() {
        render(GoogleNoticesUiState.Loaded(ROWS))

        val nodes = ROWS.map { row -> composeRule.onAllNodesWithText(row.name).fetchSemanticsNodes() }
        val rowNodes = nodes.flatten().distinctBy { it.id }.sortedBy { it.boundsInRoot.top }
        assertThat(rowNodes).hasSize(ROWS.size)
        rowNodes.zip(ROWS).forEach { (node, row) ->
            val texts = node.config[SemanticsProperties.Text].map { it.text }
            assertThat(texts.first()).isEqualTo(row.name)
            assertThat(texts).contains(app.getString(R.string.google_notices_found_in, row.foundIn))
        }
        val list = composeRule.onNodeWithTag(GOOGLE_NOTICES_LIST_TAG).fetchSemanticsNode()
        assertThat(list.config[SemanticsProperties.CollectionInfo].rowCount).isEqualTo(ROWS.size)
        assertThat(rowNodes.map { it.config[SemanticsProperties.CollectionItemInfo].rowIndex })
            .containsExactlyElementsIn(ROWS.indices.toList())
            .inOrder()
        val intro = composeRule.onNodeWithText(app.getString(R.string.google_notices_intro)).fetchSemanticsNode()
        assertThat(intro.config.getOrNull(SemanticsProperties.CollectionItemInfo)).isNull()
    }

    /** The hard case of LS-AC13: same name, same sources — only the ordinal tells them apart. */
    @Test
    fun sameNameRowsWithIdenticalSourcesCarryOrdinalsAndOpenDifferentKeys() {
        render(GoogleNoticesUiState.Loaded(ROWS))

        val first = app.getString(R.string.google_notices_ordinal, 1, 2)
        val second = app.getString(R.string.google_notices_ordinal, 2, 2)
        composeRule.onNodeWithText(first).performClick()
        composeRule.onNodeWithText(second).performClick()

        assertThat(opened).containsExactly(KOTLIN_ONE.key, KOTLIN_TWO.key).inOrder()
        val okhttp = composeRule.onNodeWithText(OKHTTP.name).fetchSemanticsNode()
        assertThat(okhttp.config[SemanticsProperties.Text]).hasSize(2)
    }

    @Test
    fun rowsAreButtonsWithTheReadLabel() {
        render(GoogleNoticesUiState.Loaded(ROWS))

        val config = composeRule.onNodeWithText(OKHTTP.name).fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Button)
        assertThat(config.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo(app.getString(R.string.google_notices_read_action))
    }

    @Test
    fun aMalformedIndexShowsTheErrorState() {
        render(GoogleNoticesUiState.Failed)

        composeRule.onNodeWithText(app.getString(R.string.google_notices_intro)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.google_notices_error)).assertIsDisplayed()
    }

    private fun render(state: GoogleNoticesUiState) {
        composeRule.setContent {
            AppTheme { GoogleNoticesScreen(state = state, onOpenNotice = { opened += it }, onBack = {}) }
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val NOTICES_ROBOLECTRIC_SDK = 34

/** Tall enough that the lazy list composes every fixture row at once. */
private const val NOTICES_TALL_WINDOW = "w411dp-h4000dp"

private val KOTLIN_ONE = GoogleNoticeRow("000000000000000a", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(1, 2))
private val KOTLIN_TWO = GoogleNoticeRow("000000000000000b", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(2, 2))
private val OKHTTP = GoogleNoticeRow("000000000000000c", "OkHttp", "ads-mobile-sdk", ordinal = null)
private val PROTOBUF = GoogleNoticeRow("000000000000000d", "Protobuf", "play-services-cronet", ordinal = null)
private val ROWS = listOf(KOTLIN_ONE, KOTLIN_TWO, OKHTTP, PROTOBUF)
