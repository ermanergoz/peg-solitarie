package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * The production loader's contract: the committed book is readable the way a shipped
 * APK will read it, and its three lookup outcomes stay distinct.
 *
 * **The rule this file exists to protect.** [OpeningBook.lookup] answers with a
 * [BookAnswer.Hit], with [BookAnswer.NoWinningMove], or with `null`. The last of those
 * is a MISS — the book does not know — and the caller must fall through to the solver.
 * [BookAnswer.NoWinningMove] is an authoritative proof that the position has no winning
 * line. Collapsing the two would reintroduce exactly the untruthful "no winning move"
 * banner DEBT-01 exists to remove, so every test here that asserts `null` is asserting
 * *not* the authoritative answer, not merely "no move".
 */
class OpeningBookTest {
    @Test
    fun theEnglish33StartHasAProvenAndLegalFirstMove() {
        val answer = OpeningBook.lookup(Boards.english33Initial)

        assertThat(answer).isInstanceOf(BookAnswer.Hit::class.java)
        val move = (answer as BookAnswer.Hit).move
        assertWithMessage("the book's English-33 opening move %s must be legal on the start", move)
            .that(RulesEngine.legalMoves(Boards.english33Initial))
            .contains(move)
    }

    @Test
    fun everyShippedClassicStartIsABookHit() {
        SolutionFixtures.ALL.forEach { fixture ->
            val start = startBoardOf(fixture)
            val answer = OpeningBook.lookup(start)

            assertWithMessage("'%s' start must be a book hit, was %s", fixture.name, answer)
                .that(answer)
                .isInstanceOf(BookAnswer.Hit::class.java)
            assertWithMessage("'%s' start move must be legal", fixture.name)
                .that(RulesEngine.legalMoves(start))
                .contains((answer as BookAnswer.Hit).move)
        }
    }

    /**
     * Past the depth the book covers, a lookup is a MISS. The position is chosen out of
     * the depth-(K+1) layer with every shallower mask and every committed-line prefix
     * removed, so it is provably outside the book's remit rather than merely one the
     * loader happened not to find.
     */
    @Test
    fun aPositionDeeperThanTheBookIsAMissRatherThanAnAuthoritativeAnswer() {
        val fixture = fixtureNamed(ENGLISH_33)
        val start = startBoardOf(fixture)
        val layers = layersOf(fixture.spec, start.pegs, BEYOND_BOOK_PLIES)
        val insideTheBook = layers.dropLast(1).flatten().toSet() + prefixMasksOf(fixture)
        val deeper = layers.last().first { mask -> mask !in insideTheBook }

        assertWithMessage("depth-%s mask %s must be a miss", BEYOND_BOOK_PLIES, deeper.toHex())
            .that(OpeningBook.lookup(Board(spec = fixture.spec, pegs = deeper)))
            .isNull()
    }

    /**
     * A generated Challenge board carries a footprint no committed book covers, so the
     * whole board is outside the catalog. That must read as "the book does not know",
     * never as [BookAnswer.NoWinningMove] — a Challenge level is solvable by
     * construction, so an authoritative "no winning move" there would be a flat lie.
     */
    @Test
    fun aGeneratedChallengeBoardIsAMissNeverAnAuthoritativeAnswer() {
        val level = LevelGenerator.generate(CHALLENGE_LEVEL)

        assertWithMessage("a generated Challenge footprint is not in the book catalog")
            .that(OpeningBook.lookup(level.startingBoard))
            .isNull()
    }

    /**
     * **Pitfall 3, restated at the book boundary.** A resumed game rebuilds its
     * [BoardSpec] from the positions stored in the snapshot blob, in whatever order they
     * were serialised — a different instance holding the same footprint. If the loader's
     * catalog were keyed on the spec INSTANCE, this lookup would silently miss and the
     * book would quietly switch itself off after every process death. That is the hardest
     * class of bug to find later: it passes every other test in this file, because every
     * other test uses the shipped spec.
     */
    @Test
    fun aSpecRebuiltFromShuffledPositionsFindsTheSameBookAsTheShippedSpec() {
        val shipped = Boards.Diamond41
        val rebuilt =
            BoardSpec(
                inPlay = shipped.footprint.shuffled(Random(SHUFFLE_SEED)),
                target = shipped.target,
                minimumPegs = shipped.minimumPegs,
            )
        val onRebuilt = OpeningBook.lookup(Board(spec = rebuilt, pegs = Boards.diamond41Initial.pegs))

        assertWithMessage("a structurally-identical spec must find the same entry")
            .that(onRebuilt)
            .isEqualTo(OpeningBook.lookup(Boards.diamond41Initial))
    }

    /**
     * Threat T-08-35: a hint is requested repeatedly during a game, so re-reading and
     * re-parsing a board's resource per lookup would put file I/O on the hint path. The
     * delta is asserted rather than an absolute count, because another test in this class
     * may already have loaded English-33 — "at most one more read" holds either way.
     */
    @Test
    fun repeatedLookupsReadABoardsResourceAtMostOnce() {
        val before = OpeningBook.resourceReads

        repeat(REPEATED_LOOKUPS) { OpeningBook.lookup(Boards.english33Initial) }

        assertWithMessage("%s lookups must cost at most one resource read", REPEATED_LOOKUPS)
            .that(OpeningBook.resourceReads - before)
            .isAtMost(1)
    }

    /**
     * The loader sits on the hint path inside a shipped APK, so no board — including
     * masks that set bits outside the footprint, which no legal game can produce —
     * may make it throw.
     */
    @Test
    fun lookupNeverThrowsForAnyBoard() {
        val degenerate = listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)
        Boards.CLASSIC_SPECS.forEach { spec ->
            degenerate.forEach { pegs ->
                assertWithMessage("lookup on '%s' with mask %s must return", spec.size, pegs.toHex())
                    .that(runCatching { OpeningBook.lookup(Board(spec = spec, pegs = pegs)) }.isSuccess)
                    .isTrue()
            }
        }
    }

    private fun fixtureNamed(name: String): SolutionFixtures.Fixture =
        SolutionFixtures.ALL.first { fixture -> fixture.name == name }

    private fun startBoardOf(fixture: SolutionFixtures.Fixture): Board =
        fixture.spec.inPlay
            .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    /**
     * The raw masks reachable in exactly 0, 1, ... [plies] moves, expanded through the
     * unchanged [RulesEngine] rather than a private re-derivation of the jump rule.
     *
     * Deliberately NOT the authoring tool's enumeration: the point of the coverage tests
     * is to catch an authoring bug, and a test that reuses the buggy enumeration would
     * agree with it.
     */
    private fun layersOf(
        spec: BoardSpec,
        startPegs: Long,
        plies: Int,
    ): List<Set<Long>> {
        val layers = ArrayList<Set<Long>>()
        val seen = hashSetOf(startPegs)
        var layer: Set<Long> = setOf(startPegs)
        layers.add(layer)
        repeat(plies) {
            val next = LinkedHashSet<Long>()
            layer.forEach { pegs ->
                val board = Board(spec = spec, pegs = pegs)
                RulesEngine.legalMoves(board).forEach { move ->
                    val child = RulesEngine.applyMove(board, move).pegs
                    if (seen.add(child)) next.add(child)
                }
            }
            layer = next
            layers.add(layer)
        }
        return layers
    }

    /** Every board state along the board's committed golden line, before each move. */
    private fun prefixMasksOf(fixture: SolutionFixtures.Fixture): Set<Long> {
        val masks = LinkedHashSet<Long>()
        var board = startBoardOf(fixture)
        RecordedSolutions.load(fixture.name).forEach { move ->
            masks.add(board.pegs)
            board = RulesEngine.applyMove(board, move)
        }
        return masks
    }

    private fun Long.toHex(): String = java.lang.Long.toHexString(this)

    private companion object {
        const val ENGLISH_33 = "english33"

        /** One ply past the deepest layer any committed book covers. */
        const val BEYOND_BOOK_PLIES = 5

        /** An arbitrary Challenge level; every generated footprint is outside the catalog. */
        const val CHALLENGE_LEVEL = 1

        /** Fixed so the shuffled-footprint rebuild is identical on every run and in CI. */
        const val SHUFFLE_SEED = 0x0B00_1209L

        const val REPEATED_LOOKUPS = 64
    }
}
