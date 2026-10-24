package com.app.pegsolitaire.ui.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
import com.app.pegsolitaire.ui.theme.AppTheme

/** The notice body's test tag: the lazy list that carries the scroll-to-node action (brief A1.6). */
internal const val GOOGLE_NOTICE_TEXT_LIST_TAG = "google_notice_text_list"

/**
 * One Google notice, full text (spec §4B, LS-08's exception; tech design §13.1 LN-4).
 *
 * The top bar's title is the notice's name as Google supplied it ("Notice" while, or because, the
 * key resolves to nothing). The body is a **`LazyColumn`, one re-flowed paragraph per item, each in
 * its own `SelectionContainer`**: Cronet's notice is 2,497 paragraphs, and composing them at once is
 * not acceptable on the 2 s-class phone, while a notice needs no whole-text selection. First the
 * plain "Found in" line, then the ordinal of a repeated name, then the paragraphs, "End of text"
 * last. Items are keyed by paragraph index and the list state is saveable.
 */
@Composable
fun GoogleNoticeTextScreen(
    state: LicenseTextUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val header = state.header as? Notice
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LicensesTopBar(
                title = header?.notice?.name ?: stringResource(R.string.google_notice_text_unknown_title),
                onBack = onBack,
            )
        },
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
                        .fillMaxWidth()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .testTag(GOOGLE_NOTICE_TEXT_LIST_TAG),
                contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
            ) {
                header?.let { noticeHeader(it) }
                when (state) {
                    is LicenseTextUiState.Loading ->
                        item(key = LOADING_KEY) { LoadingAfterDelay(stringResource(R.string.licenses_loading)) }
                    is LicenseTextUiState.Failed ->
                        item(key = ERROR_KEY) { LicensesErrorText(stringResource(R.string.google_notice_text_error)) }
                    is LicenseTextUiState.Loaded -> paragraphs(state.parts.flatMap { it.paragraphs })
                }
            }
        }
    }
}

/** The sources line, plain text (loop 1, F-13), and the ordinal of a repeated name under it. */
private fun LazyListScope.noticeHeader(header: Notice) {
    item(key = FOUND_IN_KEY) {
        HeaderLine(
            stringResource(R.string.google_notices_found_in, header.foundIn),
        )
    }
    header.ordinal?.let { ordinal ->
        item(
            key = ORDINAL_KEY,
        ) { HeaderLine(stringResource(R.string.google_notices_ordinal, ordinal.index, ordinal.count)) }
    }
}

/** One item per paragraph, each in its own `SelectionContainer`, then "End of text". */
private fun LazyListScope.paragraphs(paragraphs: List<String>) {
    items(count = paragraphs.size, key = { it }, contentType = { PARAGRAPH_CONTENT_TYPE }) { index ->
        SelectionContainer {
            Text(
                text = paragraphs[index],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    item(key = END_KEY) {
        Text(
            text = stringResource(R.string.license_text_end),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeaderLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val FOUND_IN_KEY = "found-in"
private const val ORDINAL_KEY = "ordinal"
private const val LOADING_KEY = "loading"
private const val ERROR_KEY = "error"
private const val END_KEY = "end"
private const val PARAGRAPH_CONTENT_TYPE = "paragraph"

@Preview(name = "Google notice text · light", showBackground = true)
@Preview(name = "Google notice text · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoogleNoticeTextScreenPreview() {
    val notice = GoogleNotice("0", "Kotlin", listOf("ads-mobile-sdk", "hsdp"), "0")
    AppTheme {
        GoogleNoticeTextScreen(
            state =
                LicenseTextUiState.Loaded(
                    Notice(notice, foundIn = "ads-mobile-sdk, hsdp", ordinal = NoticeOrdinal(index = 2, count = 4)),
                    listOf(LoadedTextPart(heading = null, paragraphs = listOf(PREVIEW_PARAGRAPH, PREVIEW_PARAGRAPH))),
                ),
            onBack = {},
        )
    }
}

@Preview(name = "Google notice text error · light", showBackground = true)
@Preview(name = "Google notice text error · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoogleNoticeTextScreenFailedPreview() {
    AppTheme { GoogleNoticeTextScreen(state = LicenseTextUiState.Failed(header = null), onBack = {}) }
}

private const val PREVIEW_PARAGRAPH = "Licensed under the Apache License, Version 2.0, as supplied by the publisher."
