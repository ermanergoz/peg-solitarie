package com.app.pegsolitaire.data

import android.content.Context
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
 */
class DefaultClassicLevelSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ClassicLevelSource {
        override fun descriptors(): List<ClassicLevelDescriptor> = TODO("GREEN: map the catalog to descriptors")

        override fun load(levelId: String): Level = TODO("GREEN: look up by id, fail fast on unknown")
    }
