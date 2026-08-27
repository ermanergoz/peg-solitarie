package com.app.pegsolitaire.data

import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.generator.LevelGenerator
import javax.inject.Inject

/**
 * Source of procedurally-generated Challenge levels. Implemented by the
 * reverse-generation [LevelGenerator] behind this repository seam, so the
 * `LevelRepository` (and its Hilt binding) route Challenge loads without the UI ever
 * branching on Classic-vs-Challenge.
 */
interface ChallengeLevelSource {
    /** Loads (generates) the level for [levelNumber]. */
    fun load(levelNumber: Int): Level
}

/**
 * The real Challenge source: wraps the pure-domain [LevelGenerator] and maps its
 * [com.app.pegsolitaire.domain.generator.GeneratedLevel] onto the data-layer [Level],
 * carrying the recorded `solution` and `generatorVersion` through (RESEARCH Open Q1).
 *
 * The untrusted `levelNumber` is clamped into
 * `[ChallengeLevelBounds.MIN, ChallengeLevelBounds.MAX]` — the single `:domain`
 * definition of the range (D-15) — at this data
 * boundary (T-03-07 / T-03-09 / V5) before it reaches the generator: a `< 1` value
 * clamps up to level 1 and a forged huge value clamps down to the frozen ceiling, so
 * the id and the generated board stay consistent and the O(level) replay can never
 * hang (CR-01). The [Level.displayName] is a stable, non-UI label (the id) — the
 * visible "Level N" copy is a UI-layer string resource added in 03-04, so no Android
 * resource lookup lives in the data layer here.
 */
class GeneratedChallengeLevelSource
    @Inject
    constructor(
        private val generator: LevelGenerator,
    ) : ChallengeLevelSource {
        override fun load(levelNumber: Int): Level {
            val clamped = ChallengeLevelBounds.clamp(levelNumber)
            val generated = generator.generate(clamped)
            val spec = generated.startingBoard.spec
            val id = "$CHALLENGE_ID_PREFIX$clamped"
            return Level(
                id = id,
                displayName = id,
                spec = spec,
                initialBoard = generated.startingBoard,
                holeCount = spec.size,
                solution = generated.solution,
                generatorVersion = generated.generatorVersion,
            )
        }
    }

/** Stable, non-UI id prefix for a generated Challenge [Level] (e.g. `challenge-7`). */
private const val CHALLENGE_ID_PREFIX = "challenge-"
