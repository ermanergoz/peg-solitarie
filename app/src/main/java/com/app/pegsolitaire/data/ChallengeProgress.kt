package com.app.pegsolitaire.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped constant for the Challenge starting level (QUAL-03 — no inline literal). */
private const val FIRST_CHALLENGE_LEVEL = 1

/**
 * The in-memory holder for the player's current Challenge level (D-04).
 *
 * This phase tracks the current level in memory only — Challenge restarts at level 1
 * each session. It is deliberately shaped as the persistence seam Phase 5 (CHAL-05 /
 * PERS-02) will back with DataStore so "continue from highest level reached" drops in
 * behind the same interface without touching the gameplay/results wiring.
 *
 * Exposes the current level as a read-only [StateFlow] (the UI observes it; only this
 * holder mutates it) and advances on solve (D-02 — only a 1-peg solve unlocks N+1).
 * A constructor-injectable `@Singleton`, mirroring the repository-seam shape — no Hilt
 * `@Binds` entry needed. Zero Android dependencies, so it is JVM-unit-testable.
 */
@Singleton
class ChallengeProgress
    @Inject
    constructor() {
        private val _currentLevel = MutableStateFlow(FIRST_CHALLENGE_LEVEL)

        /** The current Challenge level (1-based), starting at [FIRST_CHALLENGE_LEVEL]. */
        val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

        /** Advances to the next level (N → N+1) — called when the player solves level N. */
        fun advanceToNext() {
            _currentLevel.value += 1
        }

        /** Resets progress back to the first level (a fresh Challenge run). */
        fun reset() {
            _currentLevel.value = FIRST_CHALLENGE_LEVEL
        }
    }
