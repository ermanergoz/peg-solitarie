package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.model.Move

/**
 * What a hint request comes back with (GD-HT-1): the [GetHintUseCase] contract, and the
 * only outcome type the UI layer imports.
 *
 * It merges three sources — the recorded line, the opening book and the solver — so its
 * [ProvenDead] has more provenances than the solver's own, and the solver's type stays
 * free to change without touching the UI.
 */
sealed interface HintOutcome {
    /** A move that begins a winning line from the board the hint was asked for. */
    data class Found(
        val move: Move,
    ) : HintOutcome

    /**
     * PROVED: no winning line exists from this position — the book's authoritative entry,
     * an exhausted search, or a Pagoda reject. The only outcome that may raise "no winning
     * move".
     */
    data object ProvenDead : HintOutcome

    /**
     * The search stopped with no move and no proof: its deadline, its node ceiling or its
     * attempt limit. It says nothing about the position, which may still be winnable.
     */
    data object Undecided : HintOutcome
}
