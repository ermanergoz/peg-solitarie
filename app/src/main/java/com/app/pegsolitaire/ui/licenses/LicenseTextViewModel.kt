package com.app.pegsolitaire.ui.licenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.licenses.LicenseTextSource
import com.app.pegsolitaire.navigation.LicenseText
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Failed
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loaded
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The License text screen's state holder (spec §4, LS-09; tech design §3.6; brief §3.4).
 *
 * It resolves the [LicenseText] route's entry id against [LicenseCatalog]. An id the catalogue does
 * not know, or an entry that opens no bundled text, is [Failed] with no header **and no read**: a
 * route is forgeable state, so it may name an entry but never reach an asset. Otherwise it reads
 * each of the entry's texts in catalogue order through [source] (off the main thread), re-flows
 * them on [defaultDispatcher], and shows [Loaded] — or [Failed] under the entry's name when a text
 * cannot be read. A single text carries no part heading, because its licence name is already the
 * top bar's title; a notice-then-licence entry carries each part's heading (spec LS-10).
 *
 * @param defaultDispatcher the CPU dispatcher for the re-flow (`DomainModule`'s Default).
 */
@HiltViewModel
class LicenseTextViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val source: LicenseTextSource,
        private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val entry: LicenseEntry? = LicenseCatalog.entry(savedStateHandle.toRoute<LicenseText>().entryId)
        private val texts: LicenseEntryTarget.Texts? = entry?.opens as? LicenseEntryTarget.Texts

        private val state =
            MutableStateFlow(if (entry != null && texts != null) Loading(Bundled(entry)) else Failed(header = null))

        /** The screen's one state; starts at [Loading] or, for an unresolvable id, [Failed]. */
        val uiState: StateFlow<LicenseTextUiState> = state.asStateFlow()

        init {
            if (entry != null && texts != null) {
                viewModelScope.launch { state.value = load(Bundled(entry), texts) }
            }
        }

        private suspend fun load(
            header: Bundled,
            texts: LicenseEntryTarget.Texts,
        ): LicenseTextUiState {
            val raws = texts.parts.map { part -> source.read(part.text) }
            val read = raws.filterNotNull()
            if (read.size != raws.size) return Failed(header)
            val parts =
                withContext(defaultDispatcher) {
                    texts.parts.zip(read) { part, raw ->
                        LoadedTextPart(
                            heading = part.heading.takeIf { texts.parts.size > 1 },
                            paragraphs = reflowLicenseText(raw),
                        )
                    }
                }
            return Loaded(header, parts)
        }
    }
