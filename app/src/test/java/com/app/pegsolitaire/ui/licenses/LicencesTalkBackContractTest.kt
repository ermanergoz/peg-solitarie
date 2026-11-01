package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
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

    /**
     * The contract's order, each card read as its whole label (the tech-lead's F-4 ruling, R2), and
     * each card's stop opening with its title (LS-06: read title first).
     */
    @Test
    fun theLicensesScreenReadsInTheContractsOrder() {
        setLicenses()

        val expected =
            listOf(R.string.navigate_back, R.string.licenses_title, R.string.licenses_intro).map { app.getString(it) } +
                LicenseCatalog.sections.flatMap { section ->
                    listOf(app.getString(section.title)) + section.entries.map { labelOf(it) }
                }

        assertThat(readingOrder()).containsExactlyElementsIn(expected).inOrder()
        allEntries().forEach { entry ->
            assertWithMessage("${entry.id}: what its stop says")
                .that(said(entryNode(entry)))
                .startsWith(app.getString(entry.title))
        }
    }

    /**
     * The tech-lead's F-4 ruling, R2 (device re-check, O-1): a card's speech never depends on which
     * of its texts is on screen. Each card carries its title and its lines, in order, as its own
     * description, which Compose exposes whenever any part of the card is.
     */
    @Test
    fun everyLicensesCardSpeaksItsWholeLabelAsItsOwnDescription() {
        setLicenses()

        assertThat(allEntries()).hasSize(LICENSES_ENTRY_COUNT)
        allEntries().forEach { entry ->
            assertWithMessage("${entry.id}: its description")
                .that(entryNode(entry).config.getOrNull(SemanticsProperties.ContentDescription).orEmpty())
                .containsExactly(labelOf(entry))
        }
    }

    /**
     * R2's other half: every text inside a card is hidden from accessibility services, so TalkBack
     * reads the card's label once and never its texts again. The card itself is not hidden.
     */
    @Test
    fun everyTextInsideALicensesCardIsHiddenFromAccessibility() {
        setLicenses()

        allEntries().forEach { entry ->
            val card = cardOf(app.getString(entry.title))
            assertWithMessage("${entry.id}: whether the card itself is hidden")
                .that(card.config.contains(SemanticsProperties.HideFromAccessibility))
                .isFalse()
            val texts = textNodesUnder(card)
            assertWithMessage("${entry.id}: its texts").that(texts).hasSize(entry.lines.size + 1)
            texts.forEach { text ->
                assertWithMessage("${entry.id}: whether \"${text.config[SemanticsProperties.Text]}\" is hidden")
                    .that(text.config.contains(SemanticsProperties.HideFromAccessibility))
                    .isTrue()
            }
        }
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

    /**
     * Device QA loop 2, F-1: on TalkBack 16 an address read as its own stop after its card, though
     * the merged tree still showed one node. TalkBack stops on any focusable node, so between each
     * address text and its card — in the unmerged tree — no node may be focusable or a merge
     * boundary of its own, and the address's first merging ancestor must be its entry's card.
     */
    @Test
    fun anAddressIsPartOfItsCardsStopAndNeverAStopOfItsOwn() {
        setLicenses()

        val withAddress =
            LicenseCatalog.sections
                .flatMap {
                    it.entries
                }.filter { e -> e.lines.any { it is LicenseLine.Address } }
        assertThat(withAddress).hasSize(ADDRESS_ENTRY_COUNT)
        withAddress.forEach { entry ->
            val address = entry.lines.filterIsInstance<LicenseLine.Address>().single()
            val text =
                composeRule
                    .onNodeWithText(
                        app.getString(address.text),
                        useUnmergedTree = true,
                    ).fetchSemanticsNode()
            val chain = generateSequence(text) { it.parent }.toList()
            val card = chain.first { it.config.isMergingSemanticsOfDescendants }
            assertWithMessage(entry.id).that(card.id).isEqualTo(entryNode(entry).id)
            chain.takeWhile { it.id != card.id }.forEach { node ->
                val focusable =
                    node.config.contains(SemanticsProperties.Focused) ||
                        node.config.contains(SemanticsActions.RequestFocus)
                assertWithMessage("${entry.id}: ${node.config}").that(focusable).isFalse()
            }
        }
    }

    @Test
    fun theLicensesHeadingsAreTheTitleAndTheFourSectionHeaders() {
        setLicenses()

        val expected = listOf(R.string.licenses_title) + LicenseCatalog.sections.map { it.title }

        assertThat(headings()).containsExactlyElementsIn(expected.map { app.getString(it) }).inOrder()
    }

    /**
     * Device re-run F-4 and spec LS-15 (LS-AC06's F-4 half; the tech-lead's re-run ruling, A1.3):
     * on a phone's window, with the list at its top, the title and each section header is in the
     * semantics tree as a heading. These tests guard against lazy composition, which leaves an
     * off-screen header out of that tree. Being in it is necessary for TalkBack's heading
     * navigation, not sufficient: Compose hands accessibility services only the nodes on screen, so
     * this tree cannot show whether TalkBack reaches an off-screen header (the tech-lead's F-4 ruling).
     */
    @Test
    @Config(qualifiers = TALKBACK_PHONE_WINDOW)
    fun eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListAtItsTop() {
        setLicenses()

        assertEachLicensesHeadingIsInTheTree("the list at its top")
    }

    /** As above, with the list scrolled to its middle: to the Protocol Buffers entry. */
    @Test
    @Config(qualifiers = TALKBACK_PHONE_WINDOW)
    fun eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListScrolledToItsMiddle() {
        setLicenses()
        scrollTheListTo(LicenseEntryIds.PROTOBUF)

        assertEachLicensesHeadingIsInTheTree("the list scrolled to its middle")
    }

    /** As above, with the list scrolled to its end: to the notices entry. */
    @Test
    @Config(qualifiers = TALKBACK_PHONE_WINDOW)
    fun eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListScrolledToItsEnd() {
        setLicenses()
        scrollTheListTo(LicenseEntryIds.GOOGLE_NOTICES)

        assertEachLicensesHeadingIsInTheTree("the list scrolled to its end")
    }

    /** O-1's precondition: every entry is in the tree, by its title, before any scroll. */
    @Test
    @Config(qualifiers = TALKBACK_PHONE_WINDOW)
    fun everyLicensesEntryIsInTheTreeWithItsTitleOnAPhoneWindow() {
        setLicenses()

        val entries = LicenseCatalog.sections.flatMap { it.entries }
        assertThat(entries).hasSize(LICENSES_ENTRY_COUNT)
        entries.forEach { entry ->
            composeRule
                .onNode(hasText(app.getString(entry.title)), useUnmergedTree = true)
                .assertExists("${entry.id}: its title in the tree, unscrolled, on a phone's window")
        }
    }

    /**
     * The chevron is decorative: its own node has no description, and the only descriptions on the
     * screen are the back button's and the cards' labels (the tech-lead's F-4 ruling, R2).
     */
    @Test
    fun theChevronCarriesNoDescription() {
        setLicenses()

        val chevrons =
            composeRule.onAllNodesWithTag(LICENCE_CARD_CHEVRON_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertThat(chevrons).hasSize(tappableEntries().size)
        chevrons.forEach { chevron ->
            assertThat(chevron.config.contains(SemanticsProperties.ContentDescription)).isFalse()
        }
        val described =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                .fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.ContentDescription] }

        assertThat(described)
            .containsExactlyElementsIn(listOf(app.getString(R.string.navigate_back)) + allEntries().map { labelOf(it) })
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

    private fun scrollTheListTo(entryId: String) {
        val entry = requireNotNull(LicenseCatalog.entry(entryId))
        composeRule.onNodeWithTag(LICENSES_LIST_TAG).performScrollToNode(hasText(app.getString(entry.title)))
        composeRule.waitForIdle()
    }

    /** Header by header, never as a count (F-19): the title and each section header, as a heading. */
    private fun assertEachLicensesHeadingIsInTheTree(where: String) {
        (listOf(R.string.licenses_title) + LicenseCatalog.sections.map { it.title }).forEach { id ->
            val heading = app.getString(id)
            composeRule
                .onNode(hasText(heading) and isHeading(), useUnmergedTree = true)
                .assertExists("\"$heading\" in the tree as a heading, with $where")
        }
    }

    private fun allEntries(): List<LicenseEntry> = LicenseCatalog.sections.flatMap { it.entries }

    private fun tappableEntries(): List<LicenseEntry> = allEntries().filter { it.opens != null }

    /** What [entry]'s card should say, resolved here from the resources: title, then each line. */
    private fun labelOf(entry: LicenseEntry): String =
        (listOf(app.getString(entry.title)) + entry.lines.map { textOf(it) })
            .joinToString(LicensesUi.CARD_LABEL_SEPARATOR)

    private fun textOf(line: LicenseLine): String =
        when (line) {
            is LicenseLine.Plain -> app.getString(line.text)
            is LicenseLine.SoundCount ->
                app.resources.getQuantityString(R.plurals.licenses_sound_effects, line.count, line.count)
            is LicenseLine.Address -> app.getString(line.text)
        }

    /** The card whose title reads [title], in the unmerged tree: its title's first merging ancestor. */
    private fun cardOf(title: String): SemanticsNode =
        generateSequence(composeRule.onNode(hasText(title), useUnmergedTree = true).fetchSemanticsNode()) { it.parent }
            .first { it.config.isMergingSemanticsOfDescendants }

    /** Every node below [node] in the unmerged tree that carries a text, at any depth. */
    private fun textNodesUnder(node: SemanticsNode): List<SemanticsNode> =
        node.children.flatMap { child ->
            listOfNotNull(child.takeIf { SemanticsProperties.Text in it.config }) + textNodesUnder(child)
        }

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

/** A phone's window, the one the device has (the tech-lead's re-run ruling, A1.3 and probe 8). */
private const val TALKBACK_PHONE_WINDOW = "w411dp-h891dp"

/** Two fonts, three sound packs, eleven open-source entries, two Google entries (spec §3.2). */
private const val LICENSES_ENTRY_COUNT = 18

/** The three sound packs and the Google SDK entry (spec §5.1). */
private const val INERT_ENTRY_COUNT = 4

/** The three sound packs, the Public Suffix List and the Google SDK entry (spec §3.2). */
private const val ADDRESS_ENTRY_COUNT = 5
