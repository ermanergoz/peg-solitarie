package com.app.pegsolitaire.ui.classic

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.ClassicLevelDescriptor

/**
 * The single immutable UI state for the Classic catalog screen (CLAS-01).
 *
 * Owned by [ClassicCatalogViewModel] and exposed over a read-only `StateFlow`; the
 * screen is a stateless render of this object (state hoisting — the same UDF
 * convention as `GameUiState`). [boards] is a read-only `List` carried verbatim from
 * `LevelRepository.classicCatalog()` in its D-18 ascending hole-count order — the
 * catalog never reorders or filters.
 *
 * @property boards the 5 Classic board descriptors to render as cards, in D-18 order.
 */
@Immutable
data class ClassicCatalogUiState(
    val boards: List<ClassicLevelDescriptor>,
) {
    companion object {
        /** The pre-data state: no boards yet (the VM seeds the real list synchronously). */
        val Empty = ClassicCatalogUiState(boards = emptyList())
    }
}
