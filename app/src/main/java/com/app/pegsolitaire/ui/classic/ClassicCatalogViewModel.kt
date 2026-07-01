package com.app.pegsolitaire.ui.classic

import androidx.lifecycle.ViewModel
import com.app.pegsolitaire.data.LevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Exposes the Classic board catalog (CLAS-01) as an immutable
 * [ClassicCatalogUiState] over a read-only `StateFlow`.
 *
 * This ViewModel is a pure pass-through: it seeds its state once from
 * [LevelRepository.classicCatalog], which is the single source of truth and already
 * enforces the D-18 ascending hole-count order (02-04). It performs **no** reordering
 * or filtering, and never leaks the mutable flow (the same UDF discipline as
 * `GameViewModel`).
 *
 * @param levelRepository the catalog source; its `classicCatalog()` already returns
 *   the 5 descriptors in D-18 order.
 */
@HiltViewModel
class ClassicCatalogViewModel
    @Inject
    constructor(
        levelRepository: LevelRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ClassicCatalogUiState(levelRepository.classicCatalog()))

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<ClassicCatalogUiState> = _uiState.asStateFlow()
    }
