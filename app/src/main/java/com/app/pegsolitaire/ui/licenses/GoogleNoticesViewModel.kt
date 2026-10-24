package com.app.pegsolitaire.ui.licenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.data.licenses.GoogleNoticeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Google notices list's state holder (spec §4A, LS-09, LS-12; tech design §13.1 LN-4).
 *
 * It reads the generated index once (off the main thread, in [source]) and builds the rows once:
 * the sources joined for the "Found in" line and an ordinal on every repeated name, so composition
 * only formats them (spec §6). A missing, malformed or **empty** index is [GoogleNoticesUiState.Failed]:
 * a build that produced no notices is a defect (LN-7 item 2), and an empty list would hide it.
 */
@HiltViewModel
class GoogleNoticesViewModel
    @Inject
    constructor(
        private val source: GoogleNoticeSource,
    ) : ViewModel() {
        private val state = MutableStateFlow<GoogleNoticesUiState>(GoogleNoticesUiState.Loading)

        /** The list's one state; starts at [GoogleNoticesUiState.Loading]. */
        val uiState: StateFlow<GoogleNoticesUiState> = state.asStateFlow()

        init {
            viewModelScope.launch { state.value = rowsOf(source.index()) }
        }

        private fun rowsOf(index: List<GoogleNotice>?): GoogleNoticesUiState {
            if (index.isNullOrEmpty()) return GoogleNoticesUiState.Failed
            val ordinals = index.noticeOrdinals()
            return GoogleNoticesUiState.Loaded(
                index.map { notice ->
                    GoogleNoticeRow(
                        key = notice.key,
                        name = notice.name,
                        foundIn = notice.foundIn(),
                        ordinal = ordinals[notice.key],
                    )
                },
            )
        }
    }
