package com.app.pegsolitaire.ui.licenses

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.app.pegsolitaire.testing.composeHost
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tech-lead's F-4 ruling, R2, read from the tree TalkBack reads: the accessibility nodes the
 * Compose view's node provider hands a service, not Compose's semantics tree.
 *
 * Compose exposes a merging card's children by their clipped bounds, so the title of a card that
 * straddles the top of the list's viewport is a child not visible to the user, and TalkBack, which
 * builds a merging card's speech from its visible children, read such a card from its second line
 * (device re-check, O-1). A card that carries its label as its own description gets that label as
 * its first child, with the card's own bounds, so it is spoken title first wherever it is clipped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [EXPOSURE_ROBOLECTRIC_SDK], qualifiers = EXPOSURE_PHONE_WINDOW)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LicencesAccessibilityExposureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    /** An accessibility service is on, with touch exploration, as when TalkBack runs. */
    @Before
    fun turnOnAnAccessibilityService() {
        shadowOf(app.getSystemService(AccessibilityManager::class.java)).apply {
            setEnabled(true)
            setTouchExplorationEnabled(true)
            setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        }
    }

    @Test
    fun aCardHalfAboveTheViewportIsExposedWithItsTitleFirst() {
        setLicenses()
        val title = app.getString(requireNotNull(LicenseCatalog.entry(EXPOSED_ENTRY_ID)).title)
        val viewport = composeRule.onNodeWithTag(LICENSES_LIST_TAG).fetchSemanticsNode().boundsInRoot
        val before = textsOfCard(title)

        // Moves the viewport's top edge to halfway between the title's bottom and the last line's top.
        val offset = (bottomOf(before.first()) + topOf(before.last())) / 2 - viewport.top
        composeRule.onNodeWithTag(LICENSES_LIST_TAG).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
            scrollBy(0f, offset)
        }
        composeRule.waitForIdle()

        val after = textsOfCard(title)
        assertWithMessage("the title's bottom, against the viewport's top")
            .that(bottomOf(after.first()))
            .isAtMost(viewport.top)
        assertWithMessage("the last line's top, against the viewport's top")
            .that(topOf(after.last()))
            .isAtLeast(viewport.top)
        assertWithMessage("the last line's bottom, against the viewport's bottom")
            .that(bottomOf(after.last()))
            .isAtMost(viewport.bottom)

        val host = composeRule.composeHost()
        val provider = checkNotNull(host.accessibilityNodeProvider) { "no node provider" }
        val card = checkNotNull(provider.createAccessibilityNodeInfo(cardOf(title).id)) { "the card is not exposed" }
        card.setQueryFromAppProcessEnabled(host, true)
        val children = (0 until card.childCount).map { index -> checkNotNull(card.getChild(index)) { "child $index" } }
        val spoken = children.filter { it.isVisibleToUser }.mapNotNull { spokenBy(it) }

        assertWithMessage("what the card's visible children say, in order")
            .that(spoken.joinToString(LicensesUi.CARD_LABEL_SEPARATOR))
            .startsWith(title)
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

    /** The card whose title reads [title], in the unmerged tree: its title's first merging ancestor. */
    private fun cardOf(title: String): SemanticsNode =
        generateSequence(composeRule.onNode(hasText(title), useUnmergedTree = true).fetchSemanticsNode()) { it.parent }
            .first { it.config.isMergingSemanticsOfDescendants }

    /** The card's texts in the unmerged tree, top to bottom: its title first, its last line last. */
    private fun textsOfCard(title: String): List<SemanticsNode> = textNodesUnder(cardOf(title)).sortedBy { topOf(it) }

    private fun textNodesUnder(node: SemanticsNode): List<SemanticsNode> =
        node.children.flatMap { child ->
            listOfNotNull(child.takeIf { SemanticsProperties.Text in it.config }) + textNodesUnder(child)
        }

    /** Unclipped, so a text above the viewport keeps its place. */
    private fun topOf(node: SemanticsNode): Float = node.positionInRoot.y

    private fun bottomOf(node: SemanticsNode): Float = node.positionInRoot.y + node.size.height

    private fun spokenBy(node: AccessibilityNodeInfo): String? = (node.contentDescription ?: node.text)?.toString()
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val EXPOSURE_ROBOLECTRIC_SDK = 34

/** A phone's window, the one the device has (the tech-lead's re-run ruling, A1.3 and probe 8). */
private const val EXPOSURE_PHONE_WINDOW = "w411dp-h891dp"

/** The card QA heard read without its title, mid-list (device re-check, `tb-f4-down-from-title.txt`). */
private const val EXPOSED_ENTRY_ID = LicenseEntryIds.GSON_TINK_GUAVA
