package com.app.pegsolitaire.ui.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
import com.app.pegsolitaire.ui.theme.AppTheme

/** The body's edge: an English document, hung from its left edge in every locale (spec LS-16). */
private val BODY_EDGE = LicencesTextEdge.TextStart

/** The bundled text body's test tag: the one `SelectionContainer` every paragraph sits in. */
internal const val LICENSE_TEXT_BODY_TAG = "license_text_body"

/** The capped content column's test tag, in every body state (the tech-lead's re-run ruling, F-3). */
internal const val LICENSE_TEXT_COLUMN_TAG = "license_text_column"

/**
 * The License text screen (spec §4, LS-08, LS-10; tech design §3.6): one entry's bundled text or
 * texts, re-flowed.
 *
 * The top bar's title is the entry's licence name ("Licenses" when the route named nothing the
 * catalogue knows, brief F1). The body is **one `Column` in `verticalScroll`, the whole of it in
 * one `SelectionContainer`** — a selection can then span paragraphs, which it could not over a lazy
 * list that composes only what is on screen — with one `Text` per paragraph for TalkBack: the
 * entry's title and credit as a heading, each part under its own heading when there are two, and
 * "End of text" last. Loading shows nothing for 300 ms, then the spinner; the error state sits
 * under the entry's name.
 *
 * Every text reads in its own direction. The body, an English document, hangs from the column's left
 * edge in every locale, under a top bar that mirrors (spec LS-16).
 */
@Composable
fun LicenseTextScreen(
    state: LicenseTextUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = (state.header as? Bundled)?.entry
    val title = stringResource(entry?.licenseName ?: R.string.licenses_title)
    ProvideLicencesText(BODY_EDGE) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { LicensesTopBar(title = title, onBack = onBack) },
        ) { contentPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (state) {
                    is LicenseTextUiState.Loading ->
                        LoadingAfterDelay(description = stringResource(R.string.licenses_loading))
                    is LicenseTextUiState.Failed -> TextColumn { FailedBody(entry) }
                    is LicenseTextUiState.Loaded ->
                        SelectionContainer(modifier = Modifier.testTag(LICENSE_TEXT_BODY_TAG)) {
                            TextColumn { LoadedBody(entry, state.parts) }
                        }
                }
            }
        }
    }
}

/** The capped, centred, scrolling column every body state uses. */
@Composable
private fun TextColumn(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_md))
                .testTag(LICENSE_TEXT_COLUMN_TAG),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        horizontalAlignment = BODY_EDGE.horizontalAlignment,
    ) {
        content()
    }
}

@Composable
private fun LoadedBody(
    entry: LicenseEntry?,
    parts: List<LoadedTextPart>,
) {
    if (entry != null) EntryHeading(entry)
    parts.forEach { part ->
        part.heading?.let { PartHeading(stringResource(it)) }
        part.paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Text(
        text = stringResource(R.string.license_text_end),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FailedBody(entry: LicenseEntry?) {
    if (entry != null) EntryHeading(entry)
    LicensesErrorText(text = stringResource(R.string.licenses_text_error))
}

/** The entry's title and credit, one heading node (spec §4, §5.1). */
@Composable
private fun EntryHeading(entry: LicenseEntry) {
    val credit = (entry.lines.firstOrNull() as? LicenseLine.Plain)?.text
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
        horizontalAlignment = BODY_EDGE.horizontalAlignment,
    ) {
        Text(
            text = stringResource(entry.title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        credit?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Notice" or a licence name over its text, on a two-text entry (spec LS-10). */
@Composable
internal fun PartHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview(name = "License text · light", showBackground = true)
@Preview(name = "License text · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LicenseTextScreenLoadedPreview() {
    val jakarta = requireNotNull(LicenseCatalog.entry(LicenseEntryIds.JAKARTA_INJECT))
    AppTheme {
        LicenseTextScreen(
            state =
                LicenseTextUiState.Loaded(
                    Bundled(jakarta),
                    listOf(
                        LoadedTextPart(R.string.license_text_part_notice, listOf(PREVIEW_PARAGRAPH)),
                        LoadedTextPart(R.string.licenses_apache_name, listOf(PREVIEW_PARAGRAPH, PREVIEW_PARAGRAPH)),
                    ),
                ),
            onBack = {},
        )
    }
}

@Preview(name = "License text error · light", showBackground = true)
@Preview(name = "License text error · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LicenseTextScreenFailedPreview() {
    AppTheme {
        LicenseTextScreen(
            state = LicenseTextUiState.Failed(Bundled(requireNotNull(LicenseCatalog.entry(LicenseEntryIds.FREDOKA)))),
            onBack = {},
        )
    }
}

private const val PREVIEW_PARAGRAPH =
    "Licensed under the Apache License, Version 2.0; you may not use this file except in compliance with the License."
