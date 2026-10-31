package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment

/**
 * Where a licences screen's text hangs (spec LS-16): its lines, through [ProvideLicencesText], and a
 * text narrower than its column, through [horizontalAlignment] on that column.
 */
internal enum class LicencesTextEdge(
    /** Where a text narrower than its column sits: on the side its lines hang from. */
    val horizontalAlignment: Alignment.Horizontal,
) {
    /** From the layout's start edge: a list and the top bar, which mirror. */
    LayoutStart(Alignment.Start),

    /**
     * From the text's own start edge: a text screen's body, an English document, whose lines hang
     * from the left in every locale. A short text's box sits at the column's left edge too, since
     * the layout's start would put it on the right in an RTL layout.
     */
    TextStart(AbsoluteAlignment.Left),
}
