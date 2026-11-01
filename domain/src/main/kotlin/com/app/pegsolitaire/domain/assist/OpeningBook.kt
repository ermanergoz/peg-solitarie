package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import java.io.IOException

/**
 * What the committed opening book has to say about one position (DEBT-01).
 *
 * There are **three** outcomes, not two, and the third one is the absence of a
 * [BookAnswer] altogether — see [OpeningBook.lookup].
 */
sealed interface BookAnswer {
    /**
     * A first move PROVEN at authoring time to begin a winning line from this position.
     *
     * The proof is a full replay of the complete line through the unchanged
     * `RulesEngine`, performed offline with an unbounded budget (plan 08-08), so a hit is
     * correct by construction and the player-facing deadline is irrelevant to it.
     */
    data class Hit(
        val move: Move,
    ) : BookAnswer

    /**
     * AUTHORITATIVE: this position was **proved** to have no winning line.
     *
     * Earned only by an exhaustive search that enumerated the entire subtree without ever
     * hitting its cap. A search that merely gave up never produces this — it produces an
     * absent entry, which is a miss.
     */
    data object NoWinningMove : BookAnswer
}

/**
 * The production reader for the committed depth-K opening book (DEBT-01).
 *
 * ## The three-outcome contract — the whole point of this class
 *
 * [lookup] returns:
 *  - [BookAnswer.Hit] — a proven first winning move. Trustworthy without any further search.
 *  - [BookAnswer.NoWinningMove] — an AUTHORITATIVE proof that no winning line exists.
 *  - `null` — a **MISS**. The book does not know, and the caller **MUST** fall through to
 *    the solver.
 *
 * A miss is normal, not exceptional: Diamond-41 covers 49 of 107 canonical depth-≤4
 * positions and Wiegleb-45 15 of 16 at depth ≤ 3, each under a gap declared in the file's
 * own header, and every position past the covered depth is a miss by design. Collapsing
 * `null` into [BookAnswer.NoWinningMove] would turn "I do not know" into "there is no
 * way" — precisely the untruthful banner DEBT-01 exists to remove. The two are separated
 * here at the type level so the mistake cannot be made silently.
 *
 * ## How the book is read
 *
 * From the **classpath**, not from a file path. Every other fixture read in this repo
 * opens `src/test/resources/golden/...` as an ordinary file — a relative path that
 * resolves only because Gradle sets the test working directory to the module root, and
 * that mechanism cannot work in a shipped APK. This class instead does, in effect,
 * `OpeningBook::class.java.getResourceAsStream("/golden/book/english33-k4.txt")` — an
 * ABSOLUTE (leading-slash) classpath path assembled from [RESOURCE_DIRECTORY],
 * [FILE_INFIX] and [FILE_SUFFIX] rather than written inline, because no literal path
 * belongs in logic.
 *
 * ## Why the catalog is keyed on the footprint
 *
 * [CATALOG] is keyed on [com.app.pegsolitaire.domain.model.BoardSpec.footprint] — the
 * board's STRUCTURAL identity — never on the spec instance. A resumed game rebuilds its
 * spec from the positions stored in the snapshot blob, so it is a different instance
 * carrying the same footprint. A referentially-keyed catalog would look correct in every
 * normal test and switch the book off silently after process death, which is the hardest
 * class of bug to find later.
 *
 * ## Fail closed, and load at most once
 *
 * A resource that is absent, unreadable, or holds even ONE line the shared
 * [OpeningBookFormat] decoder rejects makes that board's book **unavailable**: every
 * subsequent lookup for it is a miss and falls through to the solver. A partial book is
 * worse than no book, because a dropped entry becomes an invisible miss. That failure is
 * memoised alongside the successes, so a missing resource costs one read rather than one
 * read per hint. This behaviour is trustworthy only because it is directly tested —
 * `OpeningBookTest` drives [readBook] and [parseBook] with malformed, truncated and
 * missing payloads.
 *
 * ## What is deliberately NOT done here
 *
 * The recorded move is **not** re-validated against `RulesEngine` before it is returned.
 * That guard belongs at the hint boundary (plan 08-10, threat T-04-CT), where it also
 * covers the recorded-line path; duplicating it would put the same check in two places
 * with two chances to drift. This is a decision, not an omission. Entry-level legality of
 * the committed data is separately proven in CI by `OpeningBookTest`.
 *
 * ## Assumption A5 (RESEARCH)
 *
 * `:domain` is a plain JVM module, so `getResourceAsStream` works. Phase 6 introduced R8
 * and resource shrinking for the first time, which is the real risk to these resources;
 * the path used here is a constant with no reflection-derived component, and plan 08-11
 * proves the resources survive into the release APK. If they ever do not, the documented
 * escape hatch is to move the book into Kotlin string constants.
 */
object OpeningBook {
    /** The absolute classpath directory holding every committed book resource. */
    private const val RESOURCE_DIRECTORY = "/golden/book/"

    /** Separates a book's board slug from the depth it covers in the resource name. */
    private const val FILE_INFIX = "-k"

    /** Extension of every committed book resource. */
    private const val FILE_SUFFIX = ".txt"

    /**
     * Lines beginning with this are metadata, not entries — every book carries a header
     * line, and a gap board declares its own incompleteness in two more. Filtering them
     * is the caller's job by [OpeningBookFormat]'s design, so the decoder has exactly one
     * notion of a valid line and no second, quieter one.
     */
    private const val COMMENT_PREFIX = "#"

    /** The depth four of the five committed books cover. */
    private const val BOOK_DEPTH = 4

    /**
     * Wiegleb-45's reduced depth. Its committed resource is named `wiegleb45-k3.txt`, not
     * `-k4`, so the artifact cannot misrepresent its own coverage (plan 08-08 checkpoint
     * decision) — which is exactly why the depth is part of the catalog rather than one
     * shared suffix.
     */
    private const val WIEGLEB_BOOK_DEPTH = 3

    /**
     * One committed book: the board slug the authoring tool writes under, and the depth
     * that book actually covers.
     *
     * The depth is carried per board rather than assumed, because the five resources do
     * NOT share a suffix.
     */
    internal data class CommittedBook(
        val slug: String,
        val depth: Int,
    ) {
        /** The absolute classpath path this book is read from. */
        val resourcePath: String get() = RESOURCE_DIRECTORY + slug + FILE_INFIX + depth + FILE_SUFFIX
    }

    /**
     * Board footprint -> the book committed for it.
     *
     * The slugs are the ones the committed files carry, which are also the
     * `SolutionFixtures` names the authoring tool writes by. Nothing but a test connects
     * those two spellings, so `OpeningBookTest` asserts they agree.
     */
    internal val CATALOG: Map<List<Position>, CommittedBook> =
        mapOf(
            Boards.English33.footprint to CommittedBook(slug = "english33", depth = BOOK_DEPTH),
            Boards.European37.footprint to CommittedBook(slug = "european37", depth = BOOK_DEPTH),
            Boards.Asymmetric39.footprint to CommittedBook(slug = "asymmetric39", depth = BOOK_DEPTH),
            Boards.Diamond41.footprint to CommittedBook(slug = "diamond41", depth = BOOK_DEPTH),
            Boards.Wiegleb45.footprint to CommittedBook(slug = "wiegleb45", depth = WIEGLEB_BOOK_DEPTH),
        )

    /**
     * How many times a book resource has actually been opened.
     *
     * Observability for threat T-08-35 (re-reading the book on every hint). A hint is
     * requested many times per game, so "loads at most once per board" has to be an
     * asserted fact rather than a claim about the code's shape.
     */
    internal var resourceReads: Int = 0
        private set

    /** Loaded books by slug. A `null` VALUE is a memoised unavailable book, not an absent load. */
    private val loaded = HashMap<String, Map<Long, Move?>?>()

    /**
     * The book's answer for [board], or `null` when the book does not know.
     *
     * `null` is a MISS and the caller must fall through to the solver;
     * [BookAnswer.NoWinningMove] is authoritative. See the class KDoc — the distinction is
     * the requirement, not an implementation detail.
     *
     * Never throws, for any board.
     */
    fun lookup(board: Board): BookAnswer? {
        val book = bookFor(board.spec.footprint) ?: return null
        val move = book[board.pegs]
        return when {
            move != null -> BookAnswer.Hit(move)
            board.pegs in book -> BookAnswer.NoWinningMove
            else -> null
        }
    }

    /**
     * [footprint]'s entry map, `null` if the board has no book or its book is unavailable.
     *
     * Synchronised rather than coroutine-guarded: `:domain` is pure Kotlin with no
     * coroutine dependency, and this is a one-shot load behind a plain map.
     */
    @Synchronized
    private fun bookFor(footprint: List<Position>): Map<Long, Move?>? {
        val committed = CATALOG[footprint] ?: return null
        // Membership, not `?:` — a memoised FAILURE is stored as a null VALUE, and
        // re-reading a missing resource on every hint is the denial of service T-08-35.
        if (committed.slug !in loaded) {
            loaded[committed.slug] = readBook(committed.resourcePath)
        }
        return loaded[committed.slug]
    }

    /**
     * Reads and decodes the book at the absolute classpath [resourcePath], or `null` when
     * the resource is absent, unreadable, or malformed anywhere.
     *
     * `internal` so the missing-resource path is driven directly by a test against a path
     * that does not exist — the production loader then exercises the very code the test
     * proved fails closed. `IOException` is caught by name rather than by a blanket
     * `catch`, so a genuine programming error elsewhere still surfaces.
     */
    internal fun readBook(resourcePath: String): Map<Long, Move?>? =
        try {
            resourceReads++
            OpeningBook::class.java.getResourceAsStream(resourcePath)?.use { stream ->
                parseBook(stream.bufferedReader(Charsets.UTF_8).lineSequence())
            }
        } catch (ignored: IOException) {
            null
        }

    /**
     * Decodes a whole book, or `null` if ANY entry line is malformed — never a partial map.
     *
     * `internal` so malformed and truncated payloads can be driven straight through this
     * step without touching the committed resources. Blank lines and `#` comments are
     * skipped here, which is the filtering [OpeningBookFormat] deliberately delegates to
     * its caller.
     */
    internal fun parseBook(lines: Sequence<String>): Map<Long, Move?>? {
        val entries = HashMap<Long, Move?>()
        for (line in lines) {
            if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) continue
            val (pegs, move) = OpeningBookFormat.decodeEntry(line) ?: return null
            entries[pegs] = move
        }
        return entries
    }
}
