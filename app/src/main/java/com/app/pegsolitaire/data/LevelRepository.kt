package com.app.pegsolitaire.data

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import javax.inject.Inject

/**
 * A fully-resolved level ready to play: its [BoardSpec] footprint, the
 * [initialBoard] start state, a human-readable [displayName] (resolved from a
 * string resource in the data layer, never a literal — QUAL-03), and the
 * [holeCount] of its footprint.
 *
 * Immutable `data class` of plain values — the same minimal-model convention as
 * the domain's `Move`/`Position`.
 *
 * The last two fields are Challenge-only seams carried through from the generator
 * (RESEARCH Open Q1): Classic levels leave both `null` (they have no recorded line
 * and no generator stamp) and are unaffected. [solution] is the generator's recorded
 * winning line (Phase 4 hints); [generatorVersion] is the frozen-contract stamp
 * (Phase 5 resume) — carried now so no downstream schema change is needed later.
 *
 * @property solution The recorded forward winning line for a generated Challenge
 *   level, or `null` for a Classic level.
 * @property generatorVersion The generator-contract version that produced a Challenge
 *   level, or `null` for a Classic level.
 */
data class Level(
    val id: String,
    val displayName: String,
    val spec: BoardSpec,
    val initialBoard: Board,
    val holeCount: Int,
    val solution: List<Move>? = null,
    val generatorVersion: Int? = null,
)

/**
 * A lightweight catalog entry for the Classic browse screen (CLAS-01): just enough
 * to render a card — the stable [id], the resolved [displayName], the [holeCount]
 * subtitle, and a [previewBoard] (the start state, so the start-empty hole shows in
 * the mini preview). The full [Level] is loaded on selection via
 * [LevelRepository.loadClassic].
 */
data class ClassicLevelDescriptor(
    val id: String,
    val displayName: String,
    val holeCount: Int,
    val previewBoard: Board,
)

/**
 * The stable, named ids of the five Classic boards (QUAL-03 — no string literals at
 * call sites; an unknown/forged id is rejected by [ClassicLevelSource.load]).
 */
object ClassicLevelIds {
    const val ENGLISH33 = "english33"
    const val EUROPEAN37 = "european37"
    const val ASYMMETRIC39 = "asymmetric39"
    const val DIAMOND41 = "diamond41"
    const val WIEGLEB45 = "wiegleb45"
}

/**
 * Single source of truth for level data (Repository pattern). Classic levels come
 * from a fixed catalog; Challenge levels are procedurally generated behind the same
 * seam. Routing between the two lives HERE (in the repository), never in the UI —
 * callers ask for a Classic id or a Challenge number and get a [Level] either way
 * (Anti-Pattern 2: no Classic-vs-Challenge branching leaks upward).
 */
interface LevelRepository {
    /** The Classic catalog as descriptors in ascending hole-count order (D-18). */
    fun classicCatalog(): List<ClassicLevelDescriptor>

    /** Loads a Classic level by id; an unknown id fails fast (Pitfall 5). */
    fun loadClassic(levelId: String): Level

    /**
     * Loads (generates) the Challenge level for [levelNumber]. The returned [Level]
     * carries the generator's recorded [Level.solution] and [Level.generatorVersion].
     * A `levelNumber < 1` is clamped to level 1 at the generator boundary.
     */
    fun loadChallenge(levelNumber: Int): Level
}

/**
 * Default [LevelRepository] that delegates to a [ClassicLevelSource] (the real
 * catalog) and a [ChallengeLevelSource] (the reverse-generation generator). Keeping
 * the two sources separate keeps the Classic path untouched by Challenge generation.
 */
class DefaultLevelRepository
    @Inject
    constructor(
        private val classicSource: ClassicLevelSource,
        private val challengeSource: ChallengeLevelSource,
    ) : LevelRepository {
        override fun classicCatalog(): List<ClassicLevelDescriptor> = classicSource.descriptors()

        override fun loadClassic(levelId: String): Level = classicSource.load(levelId)

        override fun loadChallenge(levelNumber: Int): Level = challengeSource.load(levelNumber)
    }
