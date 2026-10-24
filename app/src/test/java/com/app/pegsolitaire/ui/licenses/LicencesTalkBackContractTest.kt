package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The TalkBack contract of spec §5.1 (LS-13, LS-AC06), asserted on the merged semantics tree — the
 * tree TalkBack reads: what is a stop, what each stop says and does, which nodes are headings.
 * QA's seven-point pass line on the device is the other half.
 *
 * A tall window composes every lazy item, so the reading order is read whole.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TALKBACK_ROBOLECTRIC_SDK], qualifiers = TALKBACK_TALL_WINDOW)
class LicencesTalkBackContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun theLicensesScreenReadsInTheContractsOrder() {
        setLicenses()

        val expected =
            listOf(R.string.navigate_back, R.string.licenses_title, R.string.licenses_intro) +
                LicenseCatalog.sections.flatMap { section -> listOf(section.title) + section.entries.map { it.title } }

        assertThat(readingOrder()).containsExactlyElementsIn(expected.map { app.getString(it) }).inOrder()
    }

    @Test
    fun tappableEntriesAreButtonsWithTheReadLabel() {
        setLicenses()

        tappableEntries().filter { it.opens is LicenseEntryTarget.Texts }.forEach { entry ->
            val config = entryNode(entry).config
            assertWithMessage(entry.id).that(config.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Button)
            assertWithMessage(entry.id)
                .that(config.getOrNull(SemanticsActions.OnClick)?.label)
                .isEqualTo(app.getString(R.string.licenses_read_action))
        }
    }

    @Test
    fun theNoticesEntryCarriesTheOpenLabel() {
        setLicenses()

        val config = entryNode(requireNotNull(LicenseCatalog.entry("google-notices"))).config

        assertThat(config.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Button)
        assertThat(config.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo(app.getString(R.string.licenses_google_notices_action))
    }

    /**
     * Spec LS-AC06 as amended (loop 1, F-02): each of the four inert entries is exactly one node
     * in the merged tree, carrying its title and lines, with no click action and no role — read as
     * text, never announced as a control.
     */
    @Test
    fun inertEntriesAreOneMergedNodeWithNoClickActionAndNoRole() {
        setLicenses()

        val inert = LicenseCatalog.sections.flatMap { it.entries }.filter { it.opens == null }
        assertThat(inert).hasSize(INERT_ENTRY_COUNT)
        inert.forEach { entry ->
            val nodes = composeRule.onAllNodesWithText(app.getString(entry.title)).fetchSemanticsNodes()
            assertWithMessage(entry.id).that(nodes).hasSize(1)
            val config = nodes.single().config
            assertWithMessage(entry.id).that(config.contains(SemanticsActions.OnClick)).isFalse()
            assertWithMessage(entry.id).that(config.contains(SemanticsProperties.Role)).isFalse()
            assertWithMessage(entry.id).that(config[SemanticsProperties.Text]).hasSize(entry.lines.size + 1)
        }
    }

    @Test
    fun theLicensesHeadingsAreTheTitleAndTheFourSectionHeaders() {
        setLicenses()

        val expected = listOf(R.string.licenses_title) + LicenseCatalog.sections.map { it.title }

        assertThat(headings()).containsExactlyElementsIn(expected.map { app.getString(it) }).inOrder()
    }

    @Test
    fun theChevronCarriesNoDescription() {
        setLicenses()

        val described =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                .fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.ContentDescription] }

        assertThat(described).containsExactly(app.getString(R.string.navigate_back))
    }

    private fun setLicenses() {
        composeRule.setContent {
            AppTheme {
                LicensesScreen(
                    sections = LicenseCatalog.sections,
                    onOpenEntry = {},
                    onOpenGoogleNotices = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun tappableEntries(): List<LicenseEntry> =
        LicenseCatalog.sections.flatMap { it.entries }.filter { it.opens != null }

    private fun entryNode(entry: LicenseEntry): SemanticsNode =
        composeRule.onNodeWithText(app.getString(entry.title)).fetchSemanticsNode()

    /** The texts of every heading node, in reading order (top to bottom, then start to end). */
    private fun headings(): List<String> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNodes()
            .sortedWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    /**
     * The merged tree's stops in reading order: each node that says something — its first text, or
     * its content description — and nothing below a node that merges its descendants, since those
     * are read as part of it; ordered as TalkBack orders a screen with no traversal indices, top to
     * bottom and then start to end (the Scaffold puts its top bar after the body in the tree).
     */
    private fun readingOrder(): List<String> {
        val stops = mutableListOf<SemanticsNode>()

        fun visit(node: SemanticsNode) {
            if (said(node) != null) stops += node
            if (!node.config.isMergingSemanticsOfDescendants) node.children.forEach(::visit)
        }
        visit(composeRule.onRoot().fetchSemanticsNode())
        return stops
            .sortedWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
            .mapNotNull { said(it) }
    }

    private fun said(node: SemanticsNode): String? =
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            ?: node.config
                .getOrNull(SemanticsProperties.Text)
                ?.firstOrNull()
                ?.text
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TALKBACK_ROBOLECTRIC_SDK = 34

/** Tall enough that the lazy lists compose every one of their items at once. */
private const val TALKBACK_TALL_WINDOW = "w411dp-h4000dp"

/** The three sound packs and the Google SDK entry (spec §5.1). */
private const val INERT_ENTRY_COUNT = 4
