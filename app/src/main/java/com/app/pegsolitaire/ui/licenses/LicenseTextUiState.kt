package com.app.pegsolitaire.ui.licenses

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.licenses.GoogleNotice

/** What a text screen names above its body: the catalogue entry, or the Google notice, it opened. */
sealed interface LicenseTextHeader {
    /** A Licenses entry's bundled text or texts (spec §4). */
    @Immutable
    data class Bundled(
        val entry: LicenseEntry,
    ) : LicenseTextHeader

    /**
     * One Google notice (spec §4B), with its "Notice n of m" when its name occurs more than once in
     * the index (spec LS-12 as amended, F-08).
     */
    @Immutable
    data class Notice(
        val notice: GoogleNotice,
        val ordinal: NoticeOrdinal?,
    ) : LicenseTextHeader
}

/**
 * One re-flowed text of a text screen, in its order.
 *
 * @property heading shown over [paragraphs] when the entry opens more than one text (spec LS-10);
 *   `null` for a single text, whose licence name is already the top bar's title.
 * @property paragraphs the text re-flowed by `reflowLicenseText`, one item per paragraph.
 */
@Immutable
data class LoadedTextPart(
    @StringRes val heading: Int?,
    val paragraphs: List<String>,
)

/**
 * The state of a licence text screen (spec §4, §4B; tech design §3.6, §13.1 LN-4).
 *
 * [header] is `null` exactly when the route named nothing the screen could resolve — an unknown id —
 * so the screen shows its error state under a generic title and never a guess.
 */
sealed interface LicenseTextUiState {
    val header: LicenseTextHeader?

    /** Reading; the spinner appears only if this lasts past `LicensesUi.SPINNER_DELAY_MS`. */
    data class Loading(
        override val header: LicenseTextHeader?,
    ) : LicenseTextUiState

    /** Every part read and re-flowed. */
    @Immutable
    data class Loaded(
        override val header: LicenseTextHeader,
        val parts: List<LoadedTextPart>,
    ) : LicenseTextUiState

    /** Nothing to show: an unknown id (no header), or a text that could not be read. */
    data class Failed(
        override val header: LicenseTextHeader?,
    ) : LicenseTextUiState
}
