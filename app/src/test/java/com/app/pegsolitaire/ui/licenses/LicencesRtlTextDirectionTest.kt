package com.app.pegsolitaire.ui.licenses

import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.BundledLicenseText
import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Spec LS-16 and LS-AC19 (device re-run F-2; the tech-lead's re-run ruling and its Amendment 1):
 * under a right-to-left layout every text on the four licences screens reads in its own direction,
 * so its characters stay in written order, and its lines hang from the edge its surface names: the
 * mirrored start (the right) on the two lists and in the top bar, the left on the two text screens.
 * The chrome mirrors throughout.
 *
 * Native graphics, so the real `StaticLayout` resolves each paragraph's direction and lays out its
 * lines. A phone's window, the one the device has (the ruling's probe 8); a lazy list is scrolled
 * through, so that every text is composed, and checked, at some position. Geometry is read
 * unclipped (`positionInRoot` and `size`), because a node scrolled out of view has empty clipped
 * bounds.
 *
 * The direction oracle has two halves. The structural half (the ruling's fallback, taken) holds on
 * every text: its style names [LicensesUi.TEXT_DIRECTION]. The behavioural half reads the paragraph
 * direction, and the edge oracle each line's ends, from the text as it is drawn ([drawnLayoutOf]).
 * A plain-string `Text` answers `GetTextLayoutResult` with a re-layout from its style as given, never
 * resolved against the layout direction and at the incoming width rather than its own (foundation
 * 1.11.3, `ParagraphLayoutCache.slowCreateTextLayoutResultOrNull`), so that result alone shows a
 * left-to-right, start-aligned paragraph wherever the style names no direction, whatever is drawn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RTL_ROBOLECTRIC_SDK], qualifiers = RTL_PHONE_WINDOW)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LicencesRtlTextDirectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    /** The screen on show; setting it composes the next screen in the same host. */
    private var shown by mutableStateOf<Shown?>(null)

    private val fredoka get() = requireNotNull(LicenseCatalog.entry(LicenseEntryIds.FREDOKA))
    private val jakarta get() = requireNotNull(LicenseCatalog.entry(LicenseEntryIds.JAKARTA_INJECT))
    private val fredokaParagraphs by lazy { paragraphsOf(BundledLicenseText.FredokaOfl) }
    private val jakartaNoticeParagraphs by lazy { paragraphsOf(BundledLicenseText.JakartaInjectNotice) }
    private val apacheParagraphs by lazy { paragraphsOf(BundledLicenseText.Apache2) }
    private val noticeParagraphs by lazy { paragraphsOf(BundledLicenseText.ChromiumBsd3) }

    // The bodies of a curated-text and the notice-text fixture: the text's paragraphs, then one with
    // no strong character, so the direction and edge oracles run on the fallback to left to right.
    private val fredokaBody by lazy { fredokaParagraphs + DASH_RULE }
    private val noticeBody by lazy { noticeParagraphs + DASH_RULE }

    // ---- LS-AC19: every text resolves to a left-to-right paragraph --------------------------------

    @Test
    fun everyTextOnTheLicensesScreenIsLaidOutLeftToRightInARightToLeftLayout() {
        start(licenses(), LayoutDirection.Rtl)
        val checked = mutableSetOf<String>()

        atEveryPositionOf(LICENSES_LIST_TAG, licensesScrollTargets()) { checkEveryTextIsLeftToRight(checked) }

        assertKindsChecked(
            checked,
            "the intro" to text(R.string.licenses_intro),
            "a section header" to text(R.string.licenses_section_fonts),
            "a card's title" to text(fredoka.title),
            "a card's other line" to lineText(fredoka.lines.first()),
        )
        assertThat(checked).containsAtLeastElementsIn(licensesTexts().toSet())
    }

    @Test
    fun everyTextOnALicenseTextIsLaidOutLeftToRightInARightToLeftLayout() {
        start(fredokaText(), LayoutDirection.Rtl)
        val checked = mutableSetOf<String>()

        checkEveryTextIsLeftToRight(checked)
        show(jakartaText())
        checkEveryTextIsLeftToRight(checked)

        assertKindsChecked(
            checked,
            "a paragraph of a curated text" to fredokaParagraphs.first(),
            "a paragraph with no strong character" to DASH_RULE,
            "End of text" to text(R.string.license_text_end),
        )
        val shown =
            fredokaBody + jakartaNoticeParagraphs + apacheParagraphs +
                listOf(
                    text(fredoka.title),
                    lineText(fredoka.lines.first()),
                    text(jakarta.title),
                    lineText(jakarta.lines.first()),
                    text(R.string.license_text_part_notice),
                    text(R.string.licenses_apache_name),
                    text(R.string.licenses_ofl_name),
                )
        assertThat(checked).containsAtLeastElementsIn(shown.toSet())
    }

    @Test
    fun everyTextOnTheGoogleNoticesListIsLaidOutLeftToRightInARightToLeftLayout() {
        start(notices(), LayoutDirection.Rtl)
        val checked = mutableSetOf<String>()

        atEveryPositionOf(GOOGLE_NOTICES_LIST_TAG, noticeRowTargets()) { checkEveryTextIsLeftToRight(checked) }

        assertKindsChecked(
            checked,
            "the intro" to text(R.string.google_notices_intro),
            "a card's title" to KOTLIN,
            "a \"Found in\" line" to foundIn(LONG_SOURCES),
            "an ordinal line" to ordinal(1, 2),
        )
        assertThat(checked).containsAtLeastElementsIn(
            NOTICE_ROWS
                .flatMap { row -> listOfNotNull(row.name, foundIn(row.foundIn), row.ordinal?.let(::ordinal)) }
                .toSet(),
        )
    }

    @Test
    fun everyTextOnAGoogleNoticeTextIsLaidOutLeftToRightInARightToLeftLayout() {
        start(noticeText(), LayoutDirection.Rtl)
        val checked = mutableSetOf<String>()

        atEveryPositionOf(GOOGLE_NOTICE_TEXT_LIST_TAG, noticeTextTargets()) { checkEveryTextIsLeftToRight(checked) }

        assertKindsChecked(
            checked,
            "a \"Found in\" line" to foundIn(LONG_SOURCES),
            "an ordinal line" to ordinal(NOTICE_ORDINAL.index, NOTICE_ORDINAL.count),
            "a paragraph of a notice" to noticeParagraphs.first(),
            "a paragraph with no strong character" to DASH_RULE,
            "End of text" to text(R.string.license_text_end),
        )
        assertThat(checked).containsAtLeastElementsIn((noticeBody + NOTICE.name).toSet())
    }

    @Test
    fun theErrorTextsAreLaidOutLeftToRightInARightToLeftLayout() {
        val failed =
            listOf(
                Shown("License text, failed with its entry") {
                    LicenseTextScreen(LicenseTextUiState.Failed(Bundled(fredoka)), onBack = {})
                },
                Shown("License text, failed with no entry") {
                    LicenseTextScreen(LicenseTextUiState.Failed(null), onBack = {})
                },
                Shown("Google notices, failed") {
                    GoogleNoticesScreen(GoogleNoticesUiState.Failed, onOpenNotice = {}, onBack = {})
                },
                Shown("Google notice text, failed with its notice") {
                    GoogleNoticeTextScreen(LicenseTextUiState.Failed(NOTICE_HEADER), onBack = {})
                },
                Shown("Google notice text, failed with no notice") {
                    GoogleNoticeTextScreen(LicenseTextUiState.Failed(null), onBack = {})
                },
            )
        start(failed.first(), LayoutDirection.Rtl)
        val checked = mutableSetOf<String>()

        failed.forEach { screen ->
            show(screen)
            checkEveryTextIsLeftToRight(checked)
        }

        assertThat(checked).containsAtLeast(
            text(R.string.licenses_text_error),
            text(R.string.google_notices_error),
            text(R.string.google_notice_text_error),
        )
    }

    // ---- The chrome mirrors, and the top bar's title hangs from the mirrored start -----------------

    /**
     * Green before the fix and after it: the guard against the wrong fix, a left-to-right layout
     * forced onto a screen or its body, which would set the texts right and un-mirror the chrome.
     */
    @Test
    fun theChromeStillMirrorsInARightToLeftLayout() {
        val screens = listOf(licenses(), fredokaText(), notices(), noticeText())
        start(screens.first(), LayoutDirection.Rtl)
        val wrapped = mutableSetOf<String>()

        screens.forEach { screen ->
            show(screen)
            val back = composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).fetchSemanticsNode()
            val title = topBarTitle()
            assertWithMessage("${screen.name}: the back button's left edge, right of the title's right edge")
                .that(back.edge(Side.Left))
                .isAtLeast(title.edge(Side.Right) - TOLERANCE_PX)
            assertLinesHangFrom(title, title.edge(Side.Right), Side.Right, wrapped)
        }
        // The title's edge check bites only on a title that wraps: the notice text's does.
        assertWithMessage("the top bar titles that wrap").that(wrapped).contains(WRAPPING_TITLE)
        listOf(licenses(), notices()).forEach { screen ->
            show(screen)
            val chevrons =
                composeRule.onAllNodesWithTag(LICENCE_CARD_CHEVRON_TAG, useUnmergedTree = true).fetchSemanticsNodes()
            assertWithMessage("${screen.name}: chevrons composed").that(chevrons).isNotEmpty()
            chevrons.forEach { chevron ->
                val card = generateSequence(chevron) { it.parent }.first { it.config.isMergingSemanticsOfDescendants }
                val cardTitle = card.descendants().first(::hasTextLayout)
                assertWithMessage("${screen.name}: the chevron's right edge, left of \"${textOf(cardTitle)}\"")
                    .that(chevron.edge(Side.Right))
                    .isAtMost(cardTitle.edge(Side.Left) + TOLERANCE_PX)
            }
        }
    }

    // ---- LS-16's two edges ----------------------------------------------------------------------

    @Test
    fun everyLineOnTheLicensesListHangsFromTheMirroredStartEdgeInARightToLeftLayout() {
        start(licenses(), LayoutDirection.Rtl)
        val wrapped = mutableSetOf<String>()

        atEveryPositionOf(LICENSES_LIST_TAG, licensesScrollTargets()) {
            assertListLinesHangFromTheStartEdge(LICENSES_LIST_TAG, Side.Right, wrapped)
        }

        // The check bites only on lines that wrap: these do at a phone's width.
        assertThat(wrapped).containsAtLeast(
            text(R.string.licenses_intro),
            text(R.string.licenses_androidx_parts),
            text(R.string.licenses_psl_address),
        )
    }

    @Test
    fun everyLineOnTheGoogleNoticesListHangsFromTheMirroredStartEdgeInARightToLeftLayout() {
        start(notices(), LayoutDirection.Rtl)
        val wrapped = mutableSetOf<String>()

        atEveryPositionOf(GOOGLE_NOTICES_LIST_TAG, noticeRowTargets()) {
            assertListLinesHangFromTheStartEdge(GOOGLE_NOTICES_LIST_TAG, Side.Right, wrapped)
        }

        assertThat(wrapped).containsAtLeast(text(R.string.google_notices_intro), foundIn(LONG_SOURCES), LONG_NAME)
    }

    @Test
    fun everyLineOfALicenseTextHangsFromTheLeftEdgeInARightToLeftLayout() {
        start(fredokaText(), LayoutDirection.Rtl)
        val wrapped = mutableSetOf<String>()

        assertBodyLinesHangFromTheLeftEdge(LICENSE_TEXT_BODY_TAG, wrapped)
        show(jakartaText())
        assertBodyLinesHangFromTheLeftEdge(LICENSE_TEXT_BODY_TAG, wrapped)

        assertThat(wrapped).containsAtLeast(
            fredokaParagraphs.maxBy { it.length },
            jakartaNoticeParagraphs.maxBy { it.length },
            apacheParagraphs.maxBy { it.length },
        )
    }

    @Test
    fun everyLineOfAGoogleNoticeTextHangsFromTheLeftEdgeInARightToLeftLayout() {
        start(noticeText(), LayoutDirection.Rtl)
        val wrapped = mutableSetOf<String>()

        atEveryPositionOf(GOOGLE_NOTICE_TEXT_LIST_TAG, noticeTextTargets()) {
            assertBodyLinesHangFromTheLeftEdge(GOOGLE_NOTICE_TEXT_LIST_TAG, wrapped)
        }

        assertThat(wrapped).containsAtLeast(foundIn(LONG_SOURCES), noticeParagraphs.maxBy { it.length })
    }

    /** The left-to-right regression guard: in an LTR layout every line of all four screens hangs left. */
    @Test
    fun everyLineHangsFromTheLeftEdgeInALeftToRightLayout() {
        start(licenses(), LayoutDirection.Ltr)
        val wrapped = mutableSetOf<String>()

        assertTopBarTitleHangsFrom(Side.Left, wrapped)
        atEveryPositionOf(LICENSES_LIST_TAG, licensesScrollTargets()) {
            assertListLinesHangFromTheStartEdge(LICENSES_LIST_TAG, Side.Left, wrapped)
        }
        show(notices())
        assertTopBarTitleHangsFrom(Side.Left, wrapped)
        atEveryPositionOf(GOOGLE_NOTICES_LIST_TAG, noticeRowTargets()) {
            assertListLinesHangFromTheStartEdge(GOOGLE_NOTICES_LIST_TAG, Side.Left, wrapped)
        }
        listOf(fredokaText(), jakartaText()).forEach { screen ->
            show(screen)
            assertTopBarTitleHangsFrom(Side.Left, wrapped)
            assertBodyLinesHangFromTheLeftEdge(LICENSE_TEXT_BODY_TAG, wrapped)
        }
        show(noticeText())
        assertTopBarTitleHangsFrom(Side.Left, wrapped)
        atEveryPositionOf(GOOGLE_NOTICE_TEXT_LIST_TAG, noticeTextTargets()) {
            assertBodyLinesHangFromTheLeftEdge(GOOGLE_NOTICE_TEXT_LIST_TAG, wrapped)
        }

        assertThat(wrapped).containsAtLeast(
            text(R.string.licenses_androidx_parts),
            foundIn(LONG_SOURCES),
            fredokaParagraphs.maxBy { it.length },
            noticeParagraphs.maxBy { it.length },
            WRAPPING_TITLE,
        )
    }

    // ---- Hosting the screens --------------------------------------------------------------------

    /** One screen to show, under a [name] that keys its composition and names it in a failure. */
    private class Shown(
        val name: String,
        val content: @Composable () -> Unit,
    )

    private fun start(
        first: Shown,
        direction: LayoutDirection,
    ) {
        shown = first
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    shown?.let { screen -> key(screen.name) { screen.content() } }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun show(next: Shown) {
        shown = next
        composeRule.waitForIdle()
    }

    private fun licenses() =
        Shown("Licenses") {
            LicensesScreen(LicenseCatalog.sections, onOpenEntry = {}, onOpenGoogleNotices = {}, onBack = {})
        }

    private fun fredokaText() =
        Shown("License text, Fredoka") {
            LicenseTextScreen(
                LicenseTextUiState.Loaded(Bundled(fredoka), listOf(LoadedTextPart(null, fredokaBody))),
                onBack = {},
            )
        }

    private fun jakartaText() =
        Shown("License text, Jakarta") {
            LicenseTextScreen(
                LicenseTextUiState.Loaded(
                    Bundled(jakarta),
                    listOf(
                        LoadedTextPart(R.string.license_text_part_notice, jakartaNoticeParagraphs),
                        LoadedTextPart(R.string.licenses_apache_name, apacheParagraphs),
                    ),
                ),
                onBack = {},
            )
        }

    private fun notices() =
        Shown("Google notices") {
            GoogleNoticesScreen(GoogleNoticesUiState.Loaded(NOTICE_ROWS), onOpenNotice = {}, onBack = {})
        }

    private fun noticeText() =
        Shown("Google notice text") {
            GoogleNoticeTextScreen(
                LicenseTextUiState.Loaded(NOTICE_HEADER, listOf(LoadedTextPart(null, noticeBody))),
                onBack = {},
            )
        }

    /** Runs [check] with the list at its top, and again after scrolling each of [targets] into view. */
    private fun atEveryPositionOf(
        listTag: String,
        targets: List<SemanticsMatcher>,
        check: () -> Unit,
    ) {
        check()
        targets.forEach { target ->
            composeRule.onNodeWithTag(listTag).performScrollToNode(target)
            composeRule.waitForIdle()
            check()
        }
    }

    private fun licensesScrollTargets(): List<SemanticsMatcher> =
        LicenseCatalog.sections.flatMap { it.entries }.map { entry -> hasText(text(entry.title)) }

    /** A row by its ordinal when its name repeats, else by its name. */
    private fun noticeRowTargets(): List<SemanticsMatcher> =
        NOTICE_ROWS.map { row -> row.ordinal?.let { hasText(ordinal(it)) } ?: hasText(row.name) }

    private fun noticeTextTargets(): List<SemanticsMatcher> =
        noticeBody.map { hasText(it) } + hasText(text(R.string.license_text_end))

    // ---- The direction oracle -------------------------------------------------------------------

    /** Asserts every text composed now is a left-to-right paragraph by content, naming the text that is not. */
    private fun checkEveryTextIsLeftToRight(checked: MutableSet<String>) {
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .forEach { node ->
                val input = layoutOf(node).layoutInput
                val text = input.text.text
                assertWithMessage("\"$text\": the direction its style names, and its paragraph's as drawn")
                    .that(input.style.textDirection to drawnLayoutOf(node).getParagraphDirection(0))
                    .isEqualTo(LicensesUi.TEXT_DIRECTION to ResolvedTextDirection.Ltr)
                checked += text
            }
    }

    /** LS-AC19's kinds, each found by its text among the nodes checked, so no fixture passes vacuously. */
    private fun assertKindsChecked(
        checked: Set<String>,
        vararg kinds: Pair<String, String>,
    ) {
        kinds.forEach { (kind, text) ->
            assertWithMessage("LS-AC19's \"$kind\" (\"$text\") among the texts checked").that(checked).contains(text)
        }
    }

    // ---- The edge oracle ------------------------------------------------------------------------

    /**
     * On a list, every line hangs from the start edge on [side]: a card's lines from the card's
     * inner start edge, its padding in from the card's own; the intro's from the column's content
     * edge, which every card fills to; a section header's from that edge less the header's own
     * inset. Texts with more than one line are added to [wrapped].
     */
    private fun assertListLinesHangFromTheStartEdge(
        listTag: String,
        side: Side,
        wrapped: MutableSet<String>,
    ) {
        val list = composeRule.onNodeWithTag(listTag, useUnmergedTree = true).fetchSemanticsNode()
        val cards = list.descendants().filter { it.config.isMergingSemanticsOfDescendants }
        assertWithMessage("cards composed on $listTag").that(cards).isNotEmpty()
        val contentEdge = cards.first().edge(side)
        cards.forEach { card ->
            assertWithMessage("every card's ${side.name.lowercase()} edge on $listTag")
                .that(abs(card.edge(side) - contentEdge))
                .isAtMost(TOLERANCE_PX)
        }
        val inCards =
            cards.flatMap { card ->
                val cardEdge = card.edge(side).inward(side, px(R.dimen.spacing_md))
                card.descendants().filter(::hasTextLayout).map { it to cardEdge }
            }
        val inCardIds = inCards.map { it.first.id }.toSet()
        val outside =
            list.descendants().filter { hasTextLayout(it) && it.id !in inCardIds }.map { node ->
                val isHeader = node.config.contains(SemanticsProperties.Heading)
                node to if (isHeader) contentEdge.inward(side, px(R.dimen.spacing_sm)) else contentEdge
            }
        (inCards + outside).forEach { (node, edge) -> assertLinesHangFrom(node, edge, side, wrapped) }
    }

    /** On a text screen, every line of every body text starts at the content column's left edge. */
    private fun assertBodyLinesHangFromTheLeftEdge(
        containerTag: String,
        wrapped: MutableSet<String>,
    ) {
        val container = composeRule.onNodeWithTag(containerTag, useUnmergedTree = true).fetchSemanticsNode()
        val contentLeft = container.edge(Side.Left).inward(Side.Left, px(R.dimen.spacing_md))
        val texts = container.descendants().filter(::hasTextLayout)
        assertWithMessage("body texts composed in $containerTag").that(texts).isNotEmpty()
        texts.forEach { assertLinesHangFrom(it, contentLeft, Side.Left, wrapped) }
    }

    private fun assertTopBarTitleHangsFrom(
        side: Side,
        wrapped: MutableSet<String>,
    ) {
        val title = topBarTitle()
        assertLinesHangFrom(title, title.edge(side), side, wrapped)
    }

    private fun assertLinesHangFrom(
        node: SemanticsNode,
        edge: Float,
        side: Side,
        wrapped: MutableSet<String>,
    ) {
        val layout = drawnLayoutOf(node)
        val text = layout.layoutInput.text.text
        (0 until layout.lineCount).forEach { line ->
            val lineEdge =
                node.positionInRoot.x + if (side == Side.Left) layout.getLineLeft(line) else layout.getLineRight(line)
            assertWithMessage("line ${line + 1} of \"$text\": its ${side.name.lowercase()} edge, against $edge px")
                .that(abs(lineEdge - edge))
                .isAtMost(TOLERANCE_PX)
        }
        if (layout.lineCount > 1) wrapped += text
    }

    /** The top bar's title: the heading text nearest the top of the screen. */
    private fun topBarTitle(): SemanticsNode =
        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            .minBy { it.positionInRoot.y }

    // ---- Small readers --------------------------------------------------------------------------

    private enum class Side { Left, Right }

    private fun SemanticsNode.edge(side: Side): Float =
        if (side == Side.Left) positionInRoot.x else positionInRoot.x + size.width

    private fun Float.inward(
        side: Side,
        inset: Float,
    ): Float = if (side == Side.Left) this + inset else this - inset

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    private fun hasTextLayout(node: SemanticsNode): Boolean = node.config.contains(SemanticsActions.GetTextLayoutResult)

    private fun layoutOf(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    /**
     * The node's text laid out as it is drawn: measured again from its own layout input with a
     * [TextMeasurer], which resolves the style against the layout direction and sizes the paragraph
     * to the text, as the drawn layout does. The drawn size is the check that it reproduced it.
     */
    private fun drawnLayoutOf(node: SemanticsNode): TextLayoutResult {
        val input = layoutOf(node).layoutInput
        val drawn =
            TextMeasurer(input.fontFamilyResolver, input.density, input.layoutDirection).measure(
                text = input.text,
                style = input.style,
                overflow = input.overflow,
                softWrap = input.softWrap,
                maxLines = input.maxLines,
                placeholders = input.placeholders,
                constraints = input.constraints,
                layoutDirection = input.layoutDirection,
                density = input.density,
                fontFamilyResolver = input.fontFamilyResolver,
                skipCache = true,
            )
        assertWithMessage("\"${input.text}\" measured again: its width against the drawn width")
            .that(drawn.size.width)
            .isEqualTo(node.size.width)
        return drawn
    }

    private fun textOf(node: SemanticsNode): String = layoutOf(node).layoutInput.text.text

    private fun px(
        @DimenRes id: Int,
    ): Float = app.resources.getDimension(id)

    private fun text(
        @StringRes id: Int,
    ): String = app.getString(id)

    private fun lineText(line: LicenseLine): String =
        when (line) {
            is LicenseLine.Plain -> text(line.text)
            is LicenseLine.SoundCount ->
                app.resources.getQuantityString(R.plurals.licenses_sound_effects, line.count, line.count)
            is LicenseLine.Address -> text(line.text)
        }

    /** Every text the Licenses screen shows: the title, the intro, each header, each card's lines. */
    private fun licensesTexts(): List<String> =
        listOf(text(R.string.licenses_title), text(R.string.licenses_intro)) +
            LicenseCatalog.sections.flatMap { section ->
                listOf(text(section.title)) +
                    section.entries.flatMap { entry -> listOf(text(entry.title)) + entry.lines.map(::lineText) }
            }

    private fun foundIn(sources: String): String = app.getString(R.string.google_notices_found_in, sources)

    private fun ordinal(ordinal: NoticeOrdinal): String = ordinal(ordinal.index, ordinal.count)

    private fun ordinal(
        index: Int,
        count: Int,
    ): String = app.getString(R.string.google_notices_ordinal, index, count)

    private fun paragraphsOf(text: BundledLicenseText): List<String> =
        reflowLicenseText(
            app.assets
                .open(text.assetPath)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() },
        )
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RTL_ROBOLECTRIC_SDK = 34

/** A phone's window, the one the device has (the tech-lead's re-run ruling, probe 8). */
private const val RTL_PHONE_WINDOW = "w411dp-h891dp"

/** How far a line may sit from its edge: one pixel of rounding. */
private const val TOLERANCE_PX = 1f

private const val KOTLIN = "Kotlin"

/** Sources long enough that a "Found in" line wraps at a phone's width. */
private const val LONG_SOURCES =
    "ads-mobile-sdk, hsdp, play-services-ads-identifier, play-services-appset, play-services-base, " +
        "play-services-basement"

/**
 * A real notice name from the shipped index (`licenses/google/index.tsv`, its longest), long enough
 * that the top bar's title wraps at a phone's width, so the title's edge check bites.
 */
private const val WRAPPING_TITLE = "AndroidX lifecycle viewmodel savedstate library"

/**
 * A rule of dashes, a paragraph of the shipped notices (`c15ad8d2096909c1` and others) with no
 * strong character: under [LicensesUi.TEXT_DIRECTION] it falls back to left to right, never to the
 * layout's direction.
 */
private const val DASH_RULE = "------------------"

/** A notice name long enough that a row's title wraps at a phone's width. */
private const val LONG_NAME =
    "Protocol Buffers Java Lite runtime, as carried inside Google Play services and the Mobile Ads SDK"

private val NOTICE_ROWS =
    listOf(
        GoogleNoticeRow("000000000000000a", KOTLIN, LONG_SOURCES, NoticeOrdinal(1, 2)),
        GoogleNoticeRow("000000000000000b", KOTLIN, LONG_SOURCES, NoticeOrdinal(2, 2)),
        GoogleNoticeRow("000000000000000c", LONG_NAME, "play-services-cronet", ordinal = null),
        GoogleNoticeRow("000000000000000d", "OkHttp", "ads-mobile-sdk", ordinal = null),
    )

private val NOTICE =
    GoogleNotice("000000000000000e", WRAPPING_TITLE, LONG_SOURCES.split(", "), "e000000000000000")

private val NOTICE_ORDINAL = NoticeOrdinal(2, 4)

private val NOTICE_HEADER = Notice(NOTICE, LONG_SOURCES, NOTICE_ORDINAL)
