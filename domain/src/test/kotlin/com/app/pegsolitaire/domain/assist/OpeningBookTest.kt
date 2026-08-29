package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
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
 *
 * ## What a green run of this file proves
 *
 * That every position a player can reach within the depth each shipped Classic book
 * advertises has a committed answer that is **legal on the exact position it is filed
 * under** and **chain-closes** into the same book — minus, on Diamond-41 and Wiegleb-45,
 * the gap each of those files declares in its own header. That the loader's slugs and the
 * authoring tool's fixture names cannot drift apart. And that a corrupt, truncated or
 * missing resource degrades to no book for that board rather than to a partial one.
 *
 * ## What it deliberately does NOT prove
 *
 * That a recorded move BEGINS a winning line. That was established at authoring time by
 * replaying each complete line through the unchanged [RulesEngine] (plan 08-08) and is not
 * re-derivable in a fast CI test — Wiegleb-45's authoring run alone took 24 minutes. The
 * end-to-end truthfulness claim is asserted by plan 08-10's depth sweep.
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

    /**
     * The coverage claim, asserted in BOTH directions, with the symmetric difference in
     * the failure message so a gap is diagnosable.
     *
     * `extra` must be empty on every board — a book may not hold a mask the frontier and
     * the committed line do not produce. `missing` must equal the gap the file DECLARES in
     * its own header: 0 for the three cross boards, 440 for Diamond-41 and 4 for
     * Wiegleb-45 (plan 08-08's developer-approved documented gaps). Asserting against the
     * header rather than against a number written here is what stops an artifact quietly
     * growing its own gap — widening it without editing the header fails, and editing the
     * header is a reviewable diff.
     */
    @Test
    fun eachBooksCoverageIsItsFrontierAndCommittedLineMinusItsDeclaredGap() {
        SolutionFixtures.ALL.forEach { fixture ->
            val depth = committedOf(fixture).depth
            val book = bookOf(fixture)
            val expected = frontierOf(fixture, depth) + prefixMasksOf(fixture)
            val extra = book.keys - expected
            val missing = expected - book.keys
            val declared = declaredOf(fixture)

            assertWithMessage("'%s' holds masks outside its depth-%s remit: %s", fixture.name, depth, hex(extra))
                .that(extra)
                .isEmpty()
            assertWithMessage(
                "'%s' is missing %s depth-<=%s masks but declares a gap of %s: %s",
                fixture.name,
                missing.size,
                depth,
                declared.absent,
                hex(missing),
            ).that(missing.size)
                .isEqualTo(declared.absent)
            assertWithMessage("'%s' header must account for every position in its remit", fixture.name)
                .that(book.size + declared.absent)
                .isEqualTo(declared.total)
        }
    }

    @Test
    fun everyBookEntrysMoveIsLegalOnTheExactPositionItIsFiledUnder() {
        SolutionFixtures.ALL.forEach { fixture ->
            bookOf(fixture).forEach { (mask, move) ->
                if (move != null) {
                    val board = Board(spec = fixture.spec, pegs = mask)
                    assertWithMessage("'%s' entry %s names a move illegal there", fixture.name, hexOf(mask))
                        .that(RulesEngine.legalMoves(board))
                        .contains(move)
                }
            }
        }
    }

    /**
     * What makes the book a usable guarantee rather than a pile of independent claims. If
     * a book move led OUT of the book before the covered depth, a player following hints
     * would fall off the guarantee earlier than the depth advertises.
     */
    @Test
    fun everyEntryBelowTheCoveredDepthChainClosesIntoTheSameBook() {
        SolutionFixtures.ALL.forEach { fixture ->
            val depth = committedOf(fixture).depth
            val book = bookOf(fixture)
            val plyOf = plyIndexOf(fixture, depth)
            book.forEach { (mask, move) ->
                val ply = plyOf[mask]
                if (move != null && ply != null && ply < depth) {
                    val next = RulesEngine.applyMove(Board(spec = fixture.spec, pegs = mask), move).pegs
                    assertWithMessage(
                        "'%s' entry %s at ply %s jumps out of the book to %s",
                        fixture.name,
                        hexOf(mask),
                        ply,
                        hexOf(next),
                    ).that(book.keys)
                        .contains(next)
                }
            }
        }
    }

    @Test
    fun noBookEntryHoldsAPegOutsideItsBoardsFootprint() {
        SolutionFixtures.ALL.forEach { fixture ->
            bookOf(fixture).keys.forEach { mask ->
                assertWithMessage("'%s' entry %s sets a bit outside its footprint", fixture.name, hexOf(mask))
                    .that(mask ushr fixture.spec.size)
                    .isEqualTo(0L)
            }
        }
    }

    /**
     * The plan expected zero authoritative markers anywhere. English-33 in fact ships
     * FOUR, each backed by an exhaustive DFS that COMPLETED — a proof, not a beam giving up
     * (plan 08-08, developer decision 1). Asserting "none" would therefore assert something
     * false about the shipped artifacts, so this asserts the stronger honest property: each
     * book carries exactly as many markers as its own header declares, and English-33 is
     * the only board that declares any.
     */
    @Test
    fun everyBookCarriesExactlyTheProvenDeadMarkersItsHeaderDeclares() {
        val boardsWithMarkers = ArrayList<String>()
        SolutionFixtures.ALL.forEach { fixture ->
            val markers = bookOf(fixture).count { (_, move) -> move == null }
            if (markers > 0) boardsWithMarkers.add(fixture.name)
            assertWithMessage("'%s' marker count must match its own header", fixture.name)
                .that(markers)
                .isEqualTo(declaredOf(fixture).provenDead)
        }
        assertWithMessage("only English-33's authoring run could exhaust a subtree")
            .that(boardsWithMarkers)
            .containsExactly(ENGLISH_33)
    }

    /**
     * The drift this prevents: the authoring tool writes each file by its
     * [SolutionFixtures] name and the production loader reads it by its own catalog slug.
     * Nothing else connects the two spellings, so a rename on either side alone would ship
     * a book nobody reads — and every lookup would silently become a miss, which is the
     * one failure mode that produces no error at all.
     */
    @Test
    fun theAuthoringFixtureNamesAndTheLoadersSlugsCannotDrift() {
        assertThat(OpeningBook.CATALOG.values.mapTo(HashSet()) { it.slug })
            .isEqualTo(SolutionFixtures.ALL.mapTo(HashSet()) { it.name })
    }

    @Test
    fun everyCatalogedResourceResolvesAndTheyFitTheSizeBound() {
        val bytes = SolutionFixtures.ALL.sumOf { resourceTextOf(it).toByteArray().size }

        assertWithMessage("total committed book bytes").that(bytes).isLessThan(MAX_BOOK_BYTES)
    }

    /**
     * An EXTERNAL check on this file's own BFS. RESEARCH measured the canonical depth table
     * independently, and its English-33 row reproduces the published depth sequence for the
     * English central game — so deriving these numbers from the enumeration they verify
     * would make the check vacuous. They are written out on purpose.
     *
     * Wiegleb-45 appears twice deliberately: 76 is its depth-4 row, which checks the BFS,
     * and 16 is its depth-3 row, which is the depth its committed book actually covers.
     */
    @Test
    fun theDerivedFrontierMatchesResearchsCanonicalDepthTable() {
        RESEARCH_CANONICAL_AT_DEPTH_4.forEach { (name, expected) ->
            assertWithMessage("'%s' canonical depth-%s frontier", name, BOOK_PLIES)
                .that(canonicalCountOf(fixtureNamed(name), BOOK_PLIES))
                .isEqualTo(expected)
        }
        assertWithMessage("Wiegleb-45's canonical depth-%s frontier — the depth it ships at", WIEGLEB_PLIES)
            .that(canonicalCountOf(fixtureNamed(WIEGLEB_45), WIEGLEB_PLIES))
            .isEqualTo(WIEGLEB_CANONICAL_AT_DEPTH_3)
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

    /** The raw masks reachable within [plies] moves of the board's committed start. */
    private fun frontierOf(
        fixture: SolutionFixtures.Fixture,
        plies: Int,
    ): Set<Long> = layersOf(fixture.spec, startBoardOf(fixture).pegs, plies).flatten().toSet()

    /** Each frontier mask's shallowest ply, so chain closure can be scoped to `ply < depth`. */
    private fun plyIndexOf(
        fixture: SolutionFixtures.Fixture,
        plies: Int,
    ): Map<Long, Int> {
        val index = HashMap<Long, Int>()
        layersOf(fixture.spec, startBoardOf(fixture).pegs, plies)
            .forEachIndexed { ply, layer -> layer.forEach { mask -> index.putIfAbsent(mask, ply) } }
        return index
    }

    private fun canonicalCountOf(
        fixture: SolutionFixtures.Fixture,
        plies: Int,
    ): Int {
        val tables = BoardCanonical.chunkTables(fixture.spec)
        return frontierOf(fixture, plies).mapTo(HashSet()) { BoardCanonical.canonicalFast(it, tables) }.size
    }

    private fun committedOf(fixture: SolutionFixtures.Fixture): OpeningBook.CommittedBook =
        OpeningBook.CATALOG.getValue(fixture.spec.footprint)

    /** The committed resource's own bytes, read off the same classpath the loader uses. */
    private fun resourceTextOf(fixture: SolutionFixtures.Fixture): String =
        requireNotNull(OpeningBook::class.java.getResourceAsStream(committedOf(fixture).resourcePath)) {
            "Missing committed book resource '${committedOf(fixture).resourcePath}' on the test classpath."
        }.use { stream -> stream.bufferedReader(Charsets.UTF_8).readText() }

    private fun bookOf(fixture: SolutionFixtures.Fixture): Map<Long, Move?> =
        requireNotNull(OpeningBook.parseBook(resourceTextOf(fixture).lineSequence())) {
            "Committed book '${fixture.name}' failed to parse — the shipped artifact is malformed."
        }

    /**
     * What the file says about ITSELF: its proven-dead count, and the documented gap it
     * declares (absent positions, and the total its remit holds). A book with no gap line
     * declares an absent count of zero and a total equal to its entry count.
     */
    private fun declaredOf(fixture: SolutionFixtures.Fixture): Declared {
        val text = resourceTextOf(fixture)
        val header = text.lineSequence().first()
        val entries = keyedValue(header, ENTRIES_KEY)
        val gap = text.lineSequence().firstOrNull { GAP_PATTERN.containsMatchIn(it) }
        val declaredGap = gap?.let { GAP_PATTERN.find(it) }?.destructured
        return Declared(
            provenDead = keyedValue(header, PROVEN_DEAD_KEY),
            absent = declaredGap?.component1()?.toInt() ?: 0,
            total = declaredGap?.component2()?.toInt() ?: entries,
        )
    }

    private fun keyedValue(
        line: String,
        key: String,
    ): Int = requireNotNull(Regex(Regex.escape(key) + KEYED_VALUE_TAIL).find(line)).groupValues[1].toInt()

    private fun hexOf(mask: Long): String = mask.toHex()

    /** Masks in hex for a failure message, truncated so a 440-mask gap stays readable. */
    private fun hex(masks: Collection<Long>): String =
        masks.take(MASKS_SHOWN).joinToString(prefix = "[", postfix = if (masks.size > MASKS_SHOWN) ", …]" else "]") {
            it.toHex()
        }

    private fun Long.toHex(): String = java.lang.Long.toHexString(this)

    /** A book file's claims about its own contents, parsed from its comment header. */
    private data class Declared(
        val provenDead: Int,
        val absent: Int,
        val total: Int,
    )

    private companion object {
        const val ENGLISH_33 = "english33"
        const val WIEGLEB_45 = "wiegleb45"

        /** The depth four of the five books cover. */
        const val BOOK_PLIES = 4

        /** Wiegleb-45's reduced, honestly-named depth (plan 08-08 rescope). */
        const val WIEGLEB_PLIES = 3

        /**
         * RESEARCH's independently-measured canonical depth-≤4 frontier sizes. Hard-coded
         * because they are an EXTERNAL check on [layersOf].
         */
        val RESEARCH_CANONICAL_AT_DEPTH_4 =
            mapOf(
                ENGLISH_33 to 51,
                "european37" to 312,
                "asymmetric39" to 233,
                "diamond41" to 107,
                WIEGLEB_45 to 76,
            )

        /** RESEARCH's Wiegleb-45 rows summed to depth 3: 1 + 1 + 3 + 11. */
        const val WIEGLEB_CANONICAL_AT_DEPTH_3 = 16

        /** Plan 08-08's committed-size bound. The five books measure 47 619 bytes. */
        const val MAX_BOOK_BYTES = 250_000

        const val ENTRIES_KEY = "entries"
        const val PROVEN_DEAD_KEY = "proven-dead"
        const val KEYED_VALUE_TAIL = "=(\\d+)"
        const val MASKS_SHOWN = 12

        /** Matches the `DOCUMENTED GAP: <absent> of <total>` line a gap board's header carries. */
        val GAP_PATTERN = Regex("""DOCUMENTED GAP: (\d+) of (\d+)""")

        /** One ply past the deepest layer any committed book covers. */
        const val BEYOND_BOOK_PLIES = 5

        /** An arbitrary Challenge level; every generated footprint is outside the catalog. */
        const val CHALLENGE_LEVEL = 1

        /** Fixed so the shuffled-footprint rebuild is identical on every run and in CI. */
        const val SHUFFLE_SEED = 0x0B00_1209L

        const val REPEATED_LOOKUPS = 64
    }
}
