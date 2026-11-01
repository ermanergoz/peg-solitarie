package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.licenses.GoogleNotice

/** "Notice [index] of [count]", both 1-based (spec LS-12 as amended in loop 1, F-08). */
@Immutable
data class NoticeOrdinal(
    val index: Int,
    val count: Int,
)

/**
 * One row of the Google notices list (spec §4A; brief §0.3, A1.5), built once per index load in
 * `GoogleNoticesViewModel` so that composition only formats it.
 *
 * @property key the row's index key: the list's item key and the only thing the route carries.
 * @property name the component name exactly as Google supplied it.
 * @property foundIn the artifactIds that carry the pair, joined with ", " for the "Found in" line.
 * @property ordinal "Notice n of m" when the name occurs more than once in the index; else `null`.
 */
@Immutable
data class GoogleNoticeRow(
    val key: String,
    val name: String,
    val foundIn: String,
    val ordinal: NoticeOrdinal?,
)

/**
 * The notice's sources as its "Found in" line shows them (spec LS-12, §6): the artifactIds joined
 * with a comma and a space. The one place the separator is written, so a notice reads the same on
 * the list and on its text screen; both ViewModels build it once per load, never composition.
 */
internal fun GoogleNotice.foundIn(): String = sources.joinToString(SOURCES_SEPARATOR)

private const val SOURCES_SEPARATOR = ", "

/** The Google notices list's state (spec §4A, LS-09). */
sealed interface GoogleNoticesUiState {
    /** The index is being read; the spinner appears only after `LicensesUi.SPINNER_DELAY_MS`. */
    data object Loading : GoogleNoticesUiState

    /** The index, one row per line, in index order. */
    @Immutable
    data class Loaded(
        val rows: List<GoogleNoticeRow>,
    ) : GoogleNoticesUiState

    /** The index is missing, unreadable, malformed or empty; there is no retry. */
    data object Failed : GoogleNoticesUiState
}
