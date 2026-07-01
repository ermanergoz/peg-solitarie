package com.app.pegsolitaire.data

import android.content.Context
import androidx.annotation.StringRes
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Source of the static Classic catalog: maps the pure-domain `Boards.*` specs to
 * playable [Level]s / [ClassicLevelDescriptor]s in D-18 (ascending hole-count)
 * order, and validates a requested id against the known catalog (fail-fast).
 */
interface ClassicLevelSource {
    /** The five descriptors in ascending hole-count order (D-18). */
    fun descriptors(): List<ClassicLevelDescriptor>

    /** Loads a level by id; an unknown/forged id throws (Pitfall 5 — T-02-04-ID). */
    fun load(levelId: String): Level
}

/**
 * Real [ClassicLevelSource] over the five domain board specs.
 *
 * The application [Context] is injected so board display names resolve from string
 * resources (`R.string.board_*`) in the data layer rather than as magic-string
 * literals (QUAL-03) — keeping domain-pure board data in `:domain` and the
 * Android-resource lookup in `:app`.
 *
 * The catalog is built once into an insertion-ordered map keyed by level id, so
 * [descriptors] preserves D-18 ascending hole-count order and [load] is an O(1)
 * lookup with a fail-fast check on an unknown id.
 */
class DefaultClassicLevelSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ClassicLevelSource {
        /** id -> Level, in D-18 ascending hole-count order (LinkedHashMap preserves it). */
        private val levelsById: Map<String, Level> =
            listOf(
                level(
                    ClassicLevelIds.ENGLISH33,
                    R.string.board_english33,
                    Boards.English33,
                    Boards.english33Initial,
                ),
                level(
                    ClassicLevelIds.EUROPEAN37,
                    R.string.board_european37,
                    Boards.European37,
                    Boards.european37Initial,
                ),
                level(
                    ClassicLevelIds.ASYMMETRIC39,
                    R.string.board_asymmetric39,
                    Boards.Asymmetric39,
                    Boards.asymmetric39Initial,
                ),
                level(
                    ClassicLevelIds.DIAMOND41,
                    R.string.board_diamond41,
                    Boards.Diamond41,
                    Boards.diamond41Initial,
                ),
                level(
                    ClassicLevelIds.WIEGLEB45,
                    R.string.board_wiegleb45,
                    Boards.Wiegleb45,
                    Boards.wiegleb45Initial,
                ),
            ).associateBy { it.id }

        override fun descriptors(): List<ClassicLevelDescriptor> =
            levelsById.values.map { level ->
                ClassicLevelDescriptor(
                    id = level.id,
                    displayName = level.displayName,
                    holeCount = level.holeCount,
                    // Preview shows the start state, so the start-empty hole is visible (D-11).
                    previewBoard = level.initialBoard,
                )
            }

        override fun load(levelId: String): Level =
            requireNotNull(levelsById[levelId]) { "Unknown Classic level id: $levelId" }

        /** Resolves [nameRes] to a display name and derives `holeCount` from the spec. */
        private fun level(
            id: String,
            @StringRes nameRes: Int,
            spec: BoardSpec,
            initialBoard: Board,
        ): Level =
            Level(
                id = id,
                displayName = context.getString(nameRes),
                spec = spec,
                initialBoard = initialBoard,
                holeCount = spec.size,
            )
    }
