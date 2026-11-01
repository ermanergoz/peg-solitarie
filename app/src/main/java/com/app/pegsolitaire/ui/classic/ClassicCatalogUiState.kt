package com.app.pegsolitaire.ui.classic

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.ClassicLevelDescriptor

/**
 * The single immutable UI state for the Classic catalog screen (CLAS-01).
 *
 * Owned by [ClassicCatalogViewModel] and exposed over a read-only `StateFlow`; the
 * screen is a stateless render of this object (state hoisting — the same UDF
 * convention as `GameUiState`). [boards] is a read-only `List` carried in the
 * `LevelRepository.classicCatalog()` D-18 ascending hole-count order — the catalog
 * never reorders or filters.
 *
 * @property boards the 5 Classic board cards to render, in D-18 order.
 */
@Immutable
data class ClassicCatalogUiState(
    val boards: List<ClassicBoardUiState>,
) {
    companion object {
        /** The pre-data state: no boards yet (the VM seeds the real list synchronously). */
        val Empty = ClassicCatalogUiState(boards = emptyList())
    }
}

/**
 * One catalog card: the board's static [descriptor] plus how far the player has got on
 * it (D-10).
 *
 * [earnedStars] is resolved in the ViewModel from the stored `LevelResult` for this
 * board id — the composable never reads a repository — and is `0` for a board that has
 * never been solved, which is also the fail-safe value when results are unavailable
 * (T-05-10: a missing result costs pips, never the catalog).
 *
 * @property descriptor the board's name, hole count and preview footprint.
 * @property earnedStars the best star count stored for this board, 0-3.
 */
@Immutable
data class ClassicBoardUiState(
    val descriptor: ClassicLevelDescriptor,
    val earnedStars: Int = NO_STARS_EARNED,
)

/** A board with no stored result has earned nothing yet. */
private const val NO_STARS_EARNED = 0
