package com.app.pegsolitaire.ui.licenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection

/**
 * Lays out every text in [content] in its own direction, [LicensesUi.TEXT_DIRECTION], with its lines
 * hung from [edge] (spec LS-16; the tech-lead's re-run ruling, F-2, and its Amendment 1).
 *
 * It re-provides the enclosing theme with each style of its typography merged with that direction
 * and the edge's alignment; the colours, shapes and motion are the enclosing theme's. A wrapper, not
 * a style at each call site, because every text on these screens names its typography role, so a
 * `LocalTextStyle` would reach none of them and a per-call-site style would miss the next text
 * written. No call site sets `textAlign` or `textDirection`, which would override it.
 */
@Composable
internal fun ProvideLicencesText(
    edge: LicencesTextEdge,
    content: @Composable () -> Unit,
) {
    val typography = MaterialTheme.typography
    val layoutDirection = LocalLayoutDirection.current
    val derived =
        remember(typography, edge, layoutDirection) {
            typography.merged(
                TextStyle(textDirection = LicensesUi.TEXT_DIRECTION, textAlign = edge.textAlign(layoutDirection)),
            )
        }
    MaterialTheme(typography = derived, content = content)
}

/**
 * The alignment that hangs a line from this edge. `TextAlign.Start` and `End` follow the paragraph's
 * own direction (ui-text 1.11.3, `AndroidParagraph`), and an English paragraph is left to right in
 * every layout, so [LicencesTextEdge.LayoutStart] is resolved here from the layout direction to an
 * absolute side: `Right` in an RTL layout, `Left` otherwise. [LicencesTextEdge.TextStart] is the
 * paragraph's own start, the left for English in every locale.
 */
private fun LicencesTextEdge.textAlign(layoutDirection: LayoutDirection): TextAlign =
    when (this) {
        LicencesTextEdge.LayoutStart -> if (layoutDirection == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
        LicencesTextEdge.TextStart -> TextAlign.Start
    }

/** This typography with [style] merged into each of its styles. */
private fun Typography.merged(style: TextStyle): Typography =
    copy(
        displayLarge = displayLarge.merge(style),
        displayMedium = displayMedium.merge(style),
        displaySmall = displaySmall.merge(style),
        headlineLarge = headlineLarge.merge(style),
        headlineMedium = headlineMedium.merge(style),
        headlineSmall = headlineSmall.merge(style),
        titleLarge = titleLarge.merge(style),
        titleMedium = titleMedium.merge(style),
        titleSmall = titleSmall.merge(style),
        bodyLarge = bodyLarge.merge(style),
        bodyMedium = bodyMedium.merge(style),
        bodySmall = bodySmall.merge(style),
        labelLarge = labelLarge.merge(style),
        labelMedium = labelMedium.merge(style),
        labelSmall = labelSmall.merge(style),
    )
