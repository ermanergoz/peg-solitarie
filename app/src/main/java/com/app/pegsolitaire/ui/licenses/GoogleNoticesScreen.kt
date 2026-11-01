package com.app.pegsolitaire.ui.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.ui.backdrop.BACKDROP_PREVIEW_HEIGHT_DP
import com.app.pegsolitaire.ui.backdrop.BACKDROP_PREVIEW_WIDTH_DP
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropPreviewHost
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.window.stableSystem

/** The Google notices list's test tag: the node that carries the scroll actions (brief A1.6). */
internal const val GOOGLE_NOTICES_LIST_TAG = "google_notices_list"

/**
 * The Google notices list (spec §4A, LS-12; tech design §13.1 LN-4): the intro, then one row per
 * line of the generated index, in index order — the name as Google supplied it, "Found in" its
 * sources, and "Notice n of m" on a repeated name. Every row is tappable (forward cue, "Read the
 * notice") and opens its key; the route carries the key alone.
 *
 * Position by ear (brief A1.2): the list publishes `CollectionInfo` with the rows counted alone and
 * each row its `CollectionItemInfo`, so TalkBack says "N of M" and the intro is not row 0. The list
 * is keyed by index key with saveable state, so its place survives the round trip to a text. The
 * entrance stagger covers the first [LicensesUi.STAGGERED_ROWS] items, the intro counted (spec
 * F-23); later rows enter plainly. Loading shows the delayed spinner under the intro; Failed, the error, with no retry.
 *
 * Every text reads in its own direction and hangs from the layout's start edge, the right in an RTL
 * layout, so a row keeps its mirrored shape (spec LS-16).
 */
@Composable
fun GoogleNoticesScreen(
    state: GoogleNoticesUiState,
    onOpenNotice: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = (state as? GoogleNoticesUiState.Loaded)?.rows.orEmpty()
    ProvideLicencesText(LicencesTextEdge.LayoutStart) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { LicensesTopBar(title = stringResource(R.string.google_notices_title), onBack = onBack) },
            contentWindowInsets = WindowInsets.stableSystem,
        ) { contentPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier =
                        Modifier
                            .widthIn(max = dimensionResource(R.dimen.content_max_width))
                            .fillMaxWidth()
                            .testTag(GOOGLE_NOTICES_LIST_TAG)
                            .semantics { collectionInfo = CollectionInfo(rowCount = rows.size, columnCount = 1) },
                    contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_md)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
                ) {
                    item(key = INTRO_KEY) {
                        Text(
                            text = stringResource(R.string.google_notices_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.licencesEntrance(0),
                        )
                    }
                    when (state) {
                        GoogleNoticesUiState.Loading ->
                            item(key = LOADING_KEY) {
                                LoadingAfterDelay(stringResource(R.string.google_notices_loading))
                            }
                        GoogleNoticesUiState.Failed ->
                            item(key = ERROR_KEY) { LicensesErrorText(stringResource(R.string.google_notices_error)) }
                        is GoogleNoticesUiState.Loaded ->
                            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                                GoogleNoticeRowCard(
                                    row = row,
                                    index = index,
                                    onOpenNotice = onOpenNotice,
                                    // The intro is item 0, so row `index` is item `index + 1` (F-23).
                                    modifier = Modifier.licencesEntrance(index + 1),
                                )
                            }
                    }
                }
            }
        }
    }
}

/**
 * One notice row: the shared clay card, merged into one stop that knows its place in the list. Its
 * name, "Found in" line and any ordinal are resolved once, and that one list is both what the row
 * shows and the label it speaks, name first (the tech-lead's F-4 ruling, R2).
 */
@Composable
private fun GoogleNoticeRowCard(
    row: GoogleNoticeRow,
    index: Int,
    onOpenNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = remember(row.key, onOpenNotice) { { onOpenNotice(row.key) } }
    val texts =
        listOfNotNull(
            row.name,
            stringResource(R.string.google_notices_found_in, row.foundIn),
            row.ordinal?.let { stringResource(R.string.google_notices_ordinal, it.index, it.count) },
        )
    LicenceCard(
        onClick = withClickSound(SoundCue.ButtonForward, open),
        clickLabel = stringResource(R.string.google_notices_read_action),
        label = texts.joinToString(LicensesUi.CARD_LABEL_SEPARATOR),
        modifier = modifier,
        semantics = {
            collectionItemInfo =
                CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
        },
    ) {
        LicenceCardTexts(texts)
    }
}

private const val INTRO_KEY = "intro"
private const val LOADING_KEY = "loading"
private const val ERROR_KEY = "error"

@Preview(name = "Google notices · light", widthDp = BACKDROP_PREVIEW_WIDTH_DP, heightDp = BACKDROP_PREVIEW_HEIGHT_DP)
@Preview(
    name = "Google notices · dark",
    widthDp = BACKDROP_PREVIEW_WIDTH_DP,
    heightDp = BACKDROP_PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GoogleNoticesScreenPreview() {
    AppTheme {
        BackdropPreviewHost(BackdropDensity.Sparse) {
            GoogleNoticesScreen(
                state =
                    GoogleNoticesUiState.Loaded(
                        listOf(
                            GoogleNoticeRow("0", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(1, 2)),
                            GoogleNoticeRow("1", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(2, 2)),
                            GoogleNoticeRow("2", "OkHttp", "ads-mobile-sdk", ordinal = null),
                        ),
                    ),
                onOpenNotice = {},
                onBack = {},
            )
        }
    }
}

@Preview(
    name = "Google notices error · light",
    widthDp = BACKDROP_PREVIEW_WIDTH_DP,
    heightDp = BACKDROP_PREVIEW_HEIGHT_DP,
)
@Preview(
    name = "Google notices error · dark",
    widthDp = BACKDROP_PREVIEW_WIDTH_DP,
    heightDp = BACKDROP_PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GoogleNoticesScreenFailedPreview() {
    AppTheme {
        BackdropPreviewHost(BackdropDensity.Sparse) {
            GoogleNoticesScreen(state = GoogleNoticesUiState.Failed, onOpenNotice = {}, onBack = {})
        }
    }
}
