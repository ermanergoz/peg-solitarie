package com.app.pegsolitaire.data

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import javax.inject.Inject

/**
 * A fully-resolved Classic level ready to play: its [BoardSpec] footprint, the
 * [initialBoard] start state, a human-readable [displayName] (resolved from a
 * string resource in the data layer, never a literal — QUAL-03), and the
 * [holeCount] of its footprint.
 *
 * Immutable `data class` of plain values — the same minimal-model convention as
 * the domain's `Move`/`Position`.
 */
data class Level(
    val id: String,
    val displayName: String,
    val spec: BoardSpec,
    val initialBoard: Board,
    val holeCount: Int,
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
 * from a fixed catalog; Challenge levels are procedurally generated in Phase 3 (the
 * source is a throwing stub until then — the interface is shaped now so the
 * generator drops in behind the same seam).
 */
interface LevelRepository {
    /** The Classic catalog as descriptors in ascending hole-count order (D-18). */
    fun classicCatalog(): List<ClassicLevelDescriptor>

    /** Loads a Classic level by id; an unknown id fails fast (Pitfall 5). */
    fun loadClassic(levelId: String): Level

    /** Loads a Challenge level by number — STUB until Phase 3. */
    fun loadChallenge(levelNumber: Int): Level
}

/**
 * Default [LevelRepository] that delegates to a [ClassicLevelSource] (the real
 * catalog) and a [ChallengeLevelSource] (a Phase-3 stub). Keeping the two sources
 * separate lets Phase 3 swap the Challenge generator in without touching the
 * Classic path.
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
