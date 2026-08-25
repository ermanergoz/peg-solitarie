package com.app.pegsolitaire.ui.classic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the Classic board catalog (CLAS-01) plus each board's earned stars (D-10) as
 * an immutable [ClassicCatalogUiState] over a read-only `StateFlow`.
 *
 * The board list itself is a pure pass-through of [LevelRepository.classicCatalog],
 * which is the single source of truth and already enforces the D-18 ascending
 * hole-count order (02-04): no reordering, no filtering. On top of it the ViewModel
 * collects [ProgressRepository.observeResults] for the Classic mode and joins each
 * stored best onto its card by `levelId`, so the screen renders pips from state alone
 * and never touches a repository.
 *
 * A board with no stored result simply carries 0 stars, which is also what an empty or
 * unavailable results stream produces — a missing row can cost pips but can never break
 * the catalog (T-05-10).
 *
 * @param levelRepository the catalog source; its `classicCatalog()` already returns
 *   the 5 descriptors in D-18 order.
 * @param progressRepository the single source of truth for stored per-level bests.
 */
@HiltViewModel
class ClassicCatalogViewModel
    @Inject
    constructor(
        levelRepository: LevelRepository,
        progressRepository: ProgressRepository,
    ) : ViewModel() {
        private val catalog = levelRepository.classicCatalog()

        /**
         * Read-only state stream for the screen; the upstream results Flow never leaks.
         *
         * Shared [SharingStarted.Eagerly] so the pips are already resolved by the time
         * the screen subscribes, with the un-starred catalog as the initial value — the
         * cards therefore appear immediately (as they did before results existed) and
         * simply gain their pips.
         */
        val uiState: StateFlow<ClassicCatalogUiState> =
            progressRepository
                .observeResults(ProgressPersistenceKeys.MODE_CLASSIC)
                .map { results -> catalogWith(results.associate { it.levelId to it.stars }) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = catalogWith(emptyMap()),
                )

        /** Joins [starsByLevelId] onto the D-18-ordered catalog, defaulting to no stars. */
        private fun catalogWith(starsByLevelId: Map<String, Int>): ClassicCatalogUiState =
            ClassicCatalogUiState(
                boards =
                    catalog.map { descriptor ->
                        ClassicBoardUiState(
                            descriptor = descriptor,
                            earnedStars = starsByLevelId[descriptor.id] ?: NO_STARS_EARNED,
                        )
                    },
            )

        private companion object {
            /** A board with no stored result has earned nothing yet. */
            const val NO_STARS_EARNED = 0
        }
    }
