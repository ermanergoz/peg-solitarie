package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Move

/**
 * The immutable output of [LevelGenerator.generate] for a single Challenge level.
 *
 * Pure-domain value type (zero Android dependencies, QUAL-01): every field is a
 * plain `val` over a domain type. Solvable **by construction** — [solution] is the
 * recorded winning line the reverse-walk produced, and replaying it forward through
 * `RulesEngine.applyMove` folds [startingBoard] down to one peg.
 *
 * Two fields are carried now even though nothing consumes them downstream this
 * phase (RESEARCH Open Q1): [solution] reaches Phase 4 (free hints) and
 * [generatorVersion] reaches Phase 5 (resume) / Slice 2 (leaderboards) so a
 * generator change can be detected. Stamping [generatorVersion] from day one is the
 * frozen-contract guarantee (Pitfall 2).
 *
 * @property levelNumber The 1-based Challenge level this board was generated for.
 * @property startingBoard The board the player is dealt (solvable by construction).
 * @property solution The recorded winning line: forward [Move]s that replay through
 *   `RulesEngine.applyMove` to reduce [startingBoard] to a single peg.
 * @property generatorVersion The frozen generator-contract version that produced
 *   this level (see `GeneratorConstants.GENERATOR_VERSION`).
 */
data class GeneratedLevel(
    val levelNumber: Int,
    val startingBoard: Board,
    val solution: List<Move>,
    val generatorVersion: Int,
)
