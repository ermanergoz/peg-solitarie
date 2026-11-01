package com.app.pegsolitaire.testing

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * What a screen reader is handed, rather than the semantics tree a Compose test reads (the
 * talkback-board defect ruling, §1–§4). The tree is never pruned; the view's accessibility node
 * provider is, and it hands a service only the nodes no sibling drawn above covers. Compose also
 * sends a service its events only while one is running, so a test that reads either turns one on
 * first, as TalkBack is: enabled, one service bound, touch exploration on.
 */
internal fun turnOnATouchExplorationService() {
    val manager = shadowOf(accessibilityManager())
    manager.setEnabled(true)
    manager.setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
    manager.setTouchExplorationEnabled(true)
}

/** What the node provider hands a service for [node], or `null` for a node it prunes. */
internal fun ComposeContentTestRule.nodeInfoHandedOut(node: SemanticsNode): AccessibilityNodeInfo? =
    checkNotNull(composeHost().accessibilityNodeProvider) { "no node provider" }.createAccessibilityNodeInfo(node.id)

/** Every polite or assertive live region the node provider hands a service, as it hands them. */
internal fun ComposeContentTestRule.liveRegionsHandedOut(): List<AccessibilityNodeInfo> =
    onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filterNot { it.isRoot }
        .mapNotNull { nodeInfoHandedOut(it) }
        .filter { it.liveRegion != View.ACCESSIBILITY_LIVE_REGION_NONE }

/**
 * The text of every content-description change Compose has sent the running service so far, in
 * order: what a live region's change reaches TalkBack as. A zero-width repeat mark is unspoken, so
 * it is dropped here as a speech engine drops it.
 */
internal fun descriptionChangesSent(): List<String> =
    shadowOf(accessibilityManager())
        .sentAccessibilityEvents
        .filter { it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED }
        .filter { it.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION != 0 }
        .mapNotNull { it.contentDescription?.toString()?.replace(ZERO_WIDTH_SPACE, "") }

/** Every event Compose has sent the running service so far, in the order it sent them. */
internal fun eventsSent(): List<AccessibilityEvent> = shadowOf(accessibilityManager()).sentAccessibilityEvents

/**
 * Whether this event tells the service that [node] took input focus: the event TalkBack follows with
 * its own focus. Matched on the virtual node the event names, so the host view's own focus event is
 * never mistaken for a stop's.
 */
internal fun AccessibilityEvent.isFocusOf(node: SemanticsNode): Boolean =
    eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED && shadowOf(this).virtualDescendantId == node.id

/** Whether this event is a content-description change to [line], a zero-width repeat mark dropped. */
internal fun AccessibilityEvent.isDescriptionChangeTo(line: String): Boolean =
    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
        contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION != 0 &&
        contentDescription?.toString()?.replace(ZERO_WIDTH_SPACE, "") == line

/**
 * The virtual id of the node a service reads next after [node], from the traversal hint Compose
 * writes while a service runs, or `null` when [node] has none: a node outside Compose's order.
 */
internal fun ComposeContentTestRule.traversalBeforeOf(node: SemanticsNode): Int? {
    val provider = checkNotNull(composeHost().accessibilityNodeProvider) { "no node provider" }
    val info = provider.createAccessibilityNodeInfo(node.id) ?: return null
    provider.addExtraDataToAccessibilityNodeInfo(node.id, info, TRAVERSAL_BEFORE_KEY, null)
    return if (info.extras.containsKey(TRAVERSAL_BEFORE_KEY)) info.extras.getInt(TRAVERSAL_BEFORE_KEY) else null
}

private fun accessibilityManager(): AccessibilityManager =
    RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)

private const val ZERO_WIDTH_SPACE = "​"

/** Compose\'s test-only extra for the traversal hint (ui 1.11.3, `ExtraDataTestTraversalBeforeVal`). */
private const val TRAVERSAL_BEFORE_KEY = "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
