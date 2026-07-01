package com.app.pegsolitaire.data

import javax.inject.Inject

/**
 * Source of procedurally-generated Challenge levels. Implemented in Phase 3 by the
 * reverse-generation generator; until then it is a deliberate throwing stub so the
 * repository seam (and its Hilt binding) is stable now and the generator drops in
 * behind the same interface later.
 */
interface ChallengeLevelSource {
    /** Loads (generates) the level for [levelNumber] — STUB until Phase 3. */
    fun load(levelNumber: Int): Level
}

/**
 * Phase-2 placeholder. There is no Challenge route this phase (D-09), so this is
 * never invoked; it throws to make an accidental call fail loudly.
 */
class StubChallengeLevelSource
    @Inject
    constructor() : ChallengeLevelSource {
        override fun load(levelNumber: Int): Level = error("Challenge generation arrives in Phase 3")
    }
