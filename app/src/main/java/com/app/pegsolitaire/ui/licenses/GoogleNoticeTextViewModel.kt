package com.app.pegsolitaire.ui.licenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.licenses.GoogleNoticeSource
import com.app.pegsolitaire.data.licenses.GoogleNoticeText
import com.app.pegsolitaire.data.licenses.LicenseTextSource
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
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
import com.app.pegsolitaire.navigation.GoogleNoticeText as GoogleNoticeTextRoute

/**
 * The Google notice text screen's state holder (spec §4B, LS-09; tech design §13.1 LN-4; brief
 * §3.4) — the lazy reader for Cronet's 785 KB notice.
 *
 * The route carries a key and nothing else. The key is resolved against the index the APK carries
 * ([noticeSource], read and parsed off the main thread); an unknown key, or an index that cannot be
 * read, is [Failed] with no header and **no text read**. Only then is the notice's own stem read
 * through [textSource] and re-flowed on [defaultDispatcher]: nothing is parsed, split or re-flowed
 * on the main thread, which does only the state emissions. The raw text is a local, released once
 * re-flowed; a notice is one part, with no part heading.
 *
 * @param defaultDispatcher the CPU dispatcher for the re-flow (`DomainModule`'s Default).
 */
@HiltViewModel
class GoogleNoticeTextViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val noticeSource: GoogleNoticeSource,
        private val textSource: LicenseTextSource,
        private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val key: String = savedStateHandle.toRoute<GoogleNoticeTextRoute>().key

        private val state = MutableStateFlow<LicenseTextUiState>(Loading(header = null))

        /** The screen's one state; starts at [Loading] with no header until the key is resolved. */
        val uiState: StateFlow<LicenseTextUiState> = state.asStateFlow()

        init {
            viewModelScope.launch { load() }
        }

        private suspend fun load() {
            val index = noticeSource.index().orEmpty()
            val notice = index.firstOrNull { it.key == key }
            if (notice == null) {
                state.value = Failed(header = null)
                return
            }
            val header = Notice(notice, notice.foundIn(), index.noticeOrdinals()[notice.key])
            state.value = Loading(header)
            val raw = textSource.read(GoogleNoticeText(notice.stem))
            state.value =
                if (raw == null) {
                    Failed(header)
                } else {
                    val paragraphs = withContext(defaultDispatcher) { reflowLicenseText(raw) }
                    Loaded(header, listOf(LoadedTextPart(heading = null, paragraphs = paragraphs)))
                }
        }
    }
