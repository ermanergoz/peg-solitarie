# Phase 8: Assist & Generator Quality Debt - Pattern Map

**Mapped:** 2026-08-29
**Files analyzed:** 32 (11 new · 21 modified)
**Analogs found:** 29 / 32

> **Reading note for the planner.** This phase is overwhelmingly *modification of existing,
> heavily-KDoc'd files*. For those, the analog is the file itself — the pattern to copy is its
> own established shape (scoped constants + decision-ID KDoc + pure-domain signature). The
> genuinely NEW files are where analog selection does real work; those are called out with
> full excerpts and with the three places where **no analog exists** (§No Analog Found).

---

## File Classification

### `:domain` main (production Kotlin)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `domain/src/main/kotlin/.../model/BoardSpec.kt` (M) | model | transform | itself (lines 15–33) | self |
| `domain/src/main/kotlin/.../model/Boards.kt` (M) | model/config | transform | itself (lines 121–136, 254–265) | self |
| `domain/src/main/kotlin/.../solver/Solver.kt` (M, rewrite) | service | batch/transform | itself + `golden/solutions/AuthoringSolver.kt` | self + exact |
| `domain/src/main/kotlin/.../solver/SolverTables.kt` (NEW) | utility (precomputed tables) | transform | `generator/BoardCanonical.kt` | **partial** |
| `domain/src/main/kotlin/.../solver/SolverBudget.kt` (M) | config | — | itself + `scoring/ScoreParams.kt` | self |
| `domain/src/main/kotlin/.../solver/Pagoda.kt` (M) | utility (pure predicate) | transform | itself (lines 28–67) | self |
| `domain/src/main/kotlin/.../scoring/ScoreCalculator.kt` (M) | service (stateless calc) | transform | itself (lines 22–27) | self |
| `domain/src/main/kotlin/.../scoring/ScoreParams.kt` (M) | config constants | — | itself + `generator/GeneratorConstants.kt` | exact |
| `domain/src/main/kotlin/.../assist/OpeningBook.kt` (NEW) | data source (decode + lookup) | file-I/O (classpath) | `golden/solutions/RecordedSolutions.kt` (test) | **role-match, I/O differs** |
| `domain/src/main/kotlin/.../assist/RecordedLines.kt` (NEW) | data source (catalog + decode) | file-I/O (classpath) | `RecordedSolutions.kt` + `SolutionFixtures` | role-match |
| `domain/src/main/kotlin/.../usecase/GetHintUseCase.kt` (M) | use case | request-response | itself (lines 46–58) | self |
| `domain/src/main/resources/golden/**/*.txt` (NEW) | fixture data | file-I/O | `domain/src/test/resources/golden/solutions/*.txt` | exact |

### `:domain` test (authoring + verification)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.../golden/solutions/BeamSearchAuthoring.kt` (NEW) | authoring tool | batch | `AuthoringSolver.kt` (lines 80–118) | exact |
| `.../golden/solutions/OpeningBookAuthoring.kt` (NEW) | authoring tool / emitter | batch + file-I/O | `AuthoringSolver.writeFixtures` (213–227) + `SolutionFixtures` (244–266) | exact |
| `.../golden/solutions/AuthoringSolver.kt` (M) | authoring tool | batch | itself | self |
| `.../golden/solutions/SolutionTest.kt` (M) | test (golden replay) | file-I/O | itself (lines 74–97) | self |
| `.../assist/OpeningBookTest.kt` (NEW) | test (golden integrity) | file-I/O | `SolutionTest.kt` + `ChallengeSolvabilityTest.kt` | exact |
| `.../solver/DepthSweepTest.kt` (NEW) | test (exhaustive sweep) | batch | `SolverBudgetTest.kt` (lines 37–88) | exact |
| `.../solver/CanonicalTest.kt` (NEW) | test (property) | transform | `PagodaTest.kt` (lines 24–36) | exact |
| `.../solver/PagodaTest.kt` (M) | test (property) | transform | itself | self |
| `.../solver/SolverTest.kt` (M) | test (behaviour) | transform | itself | self |
| `.../solver/SolverBudgetTest.kt` (M) | test (budget contract) | batch | itself | self |
| `.../scoring/ScoreCalculatorTest.kt` (M) | test (unit) | transform | itself | self |
| `.../usecase/GetHintUseCaseTest.kt` (M) | test (unit) | request-response | itself | self |
| `domain/build.gradle.kts` (M) | build config | — | itself (lines 12–22) | self |

### `:app` (resume-path + scoring call sites)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `app/.../data/resume/ResumeSnapshotDto.kt` (M) | DTO + format constants | serialization | itself (lines 43–55, 65–101, 127–134) | self |
| `app/.../data/resume/SnapshotMapper.kt` (M) | mapper | transform | itself (lines 141–161) | self |
| `app/src/test/.../resume/SnapshotMapperTest.kt` (M) | test (unit) | transform | itself | self |
| `app/.../ui/game/GameViewModel.kt` (M, call site only) | viewmodel | event-driven | itself (lines 661–672, 832–869) | self |

### Planning docs (D-06 + DEBT-03)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.planning/milestones/v1.0-phases/03-.../03-VALIDATION.md` (M) | doc contract | — | `05-persistence-resume-settings/05-VALIDATION.md` | exact |
| `.planning/milestones/v1.0-phases/04-.../04-VALIDATION.md` (M) | doc contract | — | same | exact |
| `.planning/milestones/v1.0-MILESTONE-AUDIT.md` (M) | doc frontmatter | — | itself (lines 1–31) | self |
| `.planning/ROADMAP.md`, `STATE.md`, `PROJECT.md` (M) | doc | — | itself | self |

---

## Pattern Assignments

### `domain/src/main/kotlin/.../model/BoardSpec.kt` (model, transform) — MODIFY

**Analog:** itself + `generator/GeneratorConstants.kt` (for where the default constant lives).

**Current full class** (`BoardSpec.kt` lines 15–33) — note it is a plain `class`, **not** a data
class, and takes only two ctor params. A third param with a default is source-compatible with
every existing call site (`Boards.kt` ×5, `SnapshotMapper.kt:157`, `FreeformFootprints.kt:50`,
`GameUiState.kt:176`, `OnboardingOverlay.kt:157`, ~8 test sites):

```kotlin
class BoardSpec(
    val inPlay: List<Position>,
    val target: Position?,
) {
    private val indexOf: Map<Position, Int> =
        inPlay
            .sortedWith(compareBy({ it.row }, { it.col }))
            .withIndex()
            .associate { (index, position) -> position to index }

    /** Number of in-play positions (== number of occupied bit indices). */
    val size: Int get() = indexOf.size

    /** The stable bit index for [position], or `null` if it is outside the footprint. */
    fun bit(position: Position): Int? = indexOf[position]

    /** Whether [position] is part of this board's in-play footprint. */
    operator fun contains(position: Position): Boolean = position in indexOf
}
```

**Constant-placement pattern to copy** — CLAUDE.md forbids a bare `1` and the repo's convention
is a *scoped object*, not a top-level const. Copy `GeneratorConstants.kt` lines 15–29:

```kotlin
object GeneratorConstants {
    /**
     * The generator-contract version stamped onto every `GeneratedLevel` (Pitfall
     * 2). Bumped only when the generation algorithm changes; a bump ships as a
     * parallel generator, never an in-place mutation of v1.
     */
    const val GENERATOR_VERSION = 1
    ...
}
```

Note the *counter-example* already in `Solver.kt` line 10–11 — a file-private top-level const:

```kotlin
/** Lone peg count that marks a fully-solved board (D-02: any final peg counts). */
private const val SOLVED_PEG_COUNT = 1
```

This same literal is duplicated a second time in `AuthoringSolver.kt:41` and a third in
`SolutionTest.kt:107`. RESEARCH's §Project Constraints instruction is to **replace** these with
the spec field, not add a fourth copy.

---

### `domain/src/main/kotlin/.../model/Boards.kt` (model/config, transform) — MODIFY

**Analog:** itself.

**Layout-object + derived-footprint pattern** (lines 137–161) — the Diamond's KDoc is one of the
three surfaces Pitfall 7 says must be corrected (the "~5 pegs" claim → F = 2):

```kotlin
/**
 * ...
 * The diamond is the canonical center-empty board (40 pegs). Unlike the cross
 * boards it is NOT reducible to one peg: the GF(4) "colour" invariant
 * (sum of omega^(r+c) and omega^(r-c) over occupied cells, both conserved by every
 * jump) admits no single-peg target from the centre start, and search confirms the
 * floor is several pegs. So the diamond is scored on fewest pegs remaining ...
 */
private object Diamond41Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 9
    /** The column index of the diamond's vertical axis of symmetry. */
    private const val CENTER_INDEX = 4
    /** Per-row half-width: row R spans `(center-hw[R])..(center+hw[R])`. */
    private val halfWidth = intArrayOf(0, 1, 2, 3, 4, 3, 2, 1, 0)
    ...
}
```

**Spec construction site to extend** (lines 254–265) — the one place `minimumPegs = 2` is passed:

```kotlin
    /** The Diamond-41 board; the center start hole is the (D-02-ignored) target. */
    val Diamond41: BoardSpec =
        BoardSpec(
            inPlay = Diamond41Layout.inPlayPositions,
            target = Diamond41Layout.startVacancy,
        )

    /** The Diamond-41 start: every hole filled except the center (4,4) vacancy → 40 pegs. */
    val diamond41Initial: Board =
        Diamond41.inPlay
            .fold(Board(spec = Diamond41, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(Diamond41Layout.startVacancy)
```

Also correct the `Boards` object KDoc (lines 198–213), which repeats the same stale claim.

---

### `domain/src/main/kotlin/.../solver/Solver.kt` (service, batch) — MODIFY (bitboard rewrite)

**Analog:** itself for the public contract; `AuthoringSolver.kt` for the search skeleton.

**Public entry point to preserve verbatim in shape** (`Solver.kt` lines 64–74) — the goal test on
line 69 is where `board.spec.minimumPegs` replaces `SOLVED_PEG_COUNT`:

```kotlin
    fun firstWinningMove(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean = { false },
    ): Move? {
        if (board.pegCount == SOLVED_PEG_COUNT || !Pagoda.mayBeSolvable(board)) {
            lastVisited = 0
            return null
        }
        return searchWithRestarts(board, budget, shouldCancel)
    }
```

> Note for the planner: `==` becomes `<=` under a per-board target (RESEARCH Pattern 1 sketch uses
> `board.pegCount <= target`), which is also the ASVS V5 concern — a forged `minimumPegs <= 0`
> from the resume blob makes this test trivially true and the hint permanently `null`.

**Restart loop containing the defect to fix** (lines 81–98). Every attempt is handed the whole
`budget` (40M nodes), so the 1.5s `shouldCancel` always fires inside attempt 0:

```kotlin
    private fun searchWithRestarts(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean,
    ): Move? {
        var result: Move? = null
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            val search = WinningLineSearch(board.spec, budget, shouldCancel, ATTEMPT_SEED_BASE + attempt)
            val line = search.solveFrom(board)
            lastVisited = search.visited
            result = line?.firstOrNull()
            val provenUnsolvable = line == null && !search.cancelled && !search.hitBudget
            if (result != null || search.cancelled || provenUnsolvable) break
            attempt++
        }
        return result
    }
```

**Allocation-heavy hot loop to replace** (lines 138–167) — the `sortedByDescending` +
`random.nextDouble()` + `RulesEngine.applyMove` copy per node is the ~55× constant factor:

```kotlin
    private fun search(
        current: Board,
        path: ArrayDeque<Move>,
    ): List<Move>? {
        if (current.pegCount == SOLVED_PEG_COUNT) return path.toList()
        var solution: List<Move>? = null
        val withinBudget = seen.size < budget.maxNodes
        if (!withinBudget) hitBudget = true
        val explorable = !cancelled && withinBudget && Pagoda.mayBeSolvable(current)
        if (explorable && seen.add(canonical(current.pegs))) {
            val ordered =
                RulesEngine
                    .legalMoves(current)
                    .sortedByDescending { inwardness(it) + random.nextDouble() * INWARD_JITTER }
            for (move in ordered) {
                if ((nodeCounter++ and SolverBudget.CANCEL_MASK) == 0L && shouldCancel()) {
                    cancelled = true
                }
                if (!cancelled) {
                    path.addLast(move)
                    solution = search(RulesEngine.applyMove(current, move), path)
                    path.removeLast()
                }
                if (cancelled || solution != null) break
            }
        }
        return solution
    }
```

**Cooperative-cancel idiom to preserve** — `(nodeCounter++ and SolverBudget.CANCEL_MASK) == 0L`
(line 155). Keep the mask constant in `SolverBudget`; do not inline 4095.

**Memo to keep as-is** — `VisitedSet` (lines 265–322). RESEARCH §Don't Hand-Roll says it is
correct at ~16 B/entry. **It is duplicated verbatim in `AuthoringSolver.kt` lines 278–335** —
consolidation candidate alongside `symmetryPermutations`.

**Detekt warning:** the rewrite will produce a long, high-complexity method. CLAUDE.md/RESEARCH
say extract table construction out of the hot loop rather than reaching for `@Suppress`.

---

### `domain/src/main/kotlin/.../solver/SolverTables.kt` (utility, transform) — NEW

**Analog:** `generator/BoardCanonical.kt` — **partial match only** (same "derive `IntArray`s from a
`BoardSpec` once, reuse for many positions" shape; nothing in the repo precomputes bitmask move
tables or chunked byte LUTs).

**Spec-derived-permutation pattern to copy** (`BoardCanonical.kt` lines 30–59):

```kotlin
object BoardCanonical {
    fun symmetryPermutations(spec: BoardSpec): List<IntArray> {
        val dimension = spec.inPlay.maxOf { maxOf(it.row, it.col) } + 1
        val last = dimension - 1
        val transforms: List<(Position) -> Position> =
            listOf(
                { p -> p },
                { p -> Position(row = p.col, col = last - p.row) },
                { p -> Position(row = last - p.row, col = last - p.col) },
                { p -> Position(row = last - p.col, col = p.row) },
                { p -> Position(row = p.row, col = last - p.col) },
                { p -> Position(row = last - p.row, col = p.col) },
                { p -> Position(row = p.col, col = p.row) },
                { p -> Position(row = last - p.col, col = last - p.row) },
            )
        return transforms.mapNotNull { transform ->
            val perm = IntArray(spec.size)
            var preservesFootprint = true
            for (position in spec.inPlay) {
                val image = transform(position)
                val sourceBit = spec.bit(position) ?: error("in-play position must have a bit")
                val imageBit = spec.bit(image)
                if (imageBit == null) {
                    preservesFootprint = false
                    break
                }
                perm[sourceBit] = imageBit
            }
            if (preservesFootprint) perm else null
        }
    }

    /** The lexicographically smallest of [pegs] under all [symmetries] — a canonical key. */
    fun canonical(pegs: Long, symmetries: List<IntArray>): Long { ... }
}
```

**Duplication to consolidate (Open Question 5):** this exact logic exists **three times**:
- `generator/BoardCanonical.kt` lines 30–78 (the `main`-source copy — make this the single home)
- `solver/Solver.kt` lines 191–243 (private inside `WinningLineSearch`)
- `golden/solutions/AuthoringSolver.kt` lines 157–205 (private in the test tool)

The `BoardCanonical` KDoc itself already documents the lift: *"Lifted **verbatim** from the
test-source `AuthoringSolver.symmetryPermutations`/`canonical`."* Chunk count must be derived
(`(spec.size + 7) / 8`), never hardcoded (Pitfall 4).

**`spec.bit(position)` is the bit-index authority** — `BoardSpec.kt:29`. All tables key off it.

---

### `domain/src/main/kotlin/.../solver/SolverBudget.kt` (config) — MODIFY

**Analog:** itself. Copy the property-KDoc + companion-const shape exactly (lines 20–55):

```kotlin
data class SolverBudget(
    val maxNodes: Long,
    val deadline: Duration,
) {
    companion object {
        /**
         * Default node cap — a memory/time SAFETY CEILING, not the operative production
         * limit. ... Gated by [SolverBudgetTest]: it must stay >= 4x the
         * largest search observed over the TRACTABLE corpus ... RAISE this value if the
         * measurement ever exceeds the 4x headroom — never lower the assertion.
         */
        const val DEFAULT_MAX_NODES = 40_000_000L

        /** Default wall-clock ceiling. ... */
        val DEFAULT_DEADLINE = 1_500.milliseconds

        /**
         * Poll mask for the cooperative cancel check: the search tests `shouldCancel()`
         * once every `CANCEL_MASK + 1` (= 4096) node expansions ...
         */
        const val CANCEL_MASK = 4_095L

        /** The shipped budget: the measured [DEFAULT_MAX_NODES] cap + [DEFAULT_DEADLINE]. */
        val DEFAULT = SolverBudget(maxNodes = DEFAULT_MAX_NODES, deadline = DEFAULT_DEADLINE)
    }
}
```

`DEFAULT_DEADLINE` is **frozen** (D-07). New field `perAttemptMaxNodes` (~200_000L) follows the
same KDoc-with-measurement style. Pitfall 2 applies to the `DEFAULT_MAX_NODES` KDoc: re-document
whether the 4× headroom now measures the per-attempt cap or the total.

---

### `domain/src/main/kotlin/.../solver/Pagoda.kt` (utility, transform) — MODIFY

**Analog:** itself (whole file is 68 lines).

**Necessary-condition contract + parity core** (lines 28–67):

```kotlin
object Pagoda {
    /** Number of colour classes in each colouring (the invariant is taken mod 3). */
    private const val COLORING_MODULUS = 3

    /** A reachable single-peg terminal has exactly one odd colour class per colouring. */
    private const val SINGLE_PEG_ODD_CLASSES = 1

    fun mayBeSolvable(board: Board): Boolean {
        val pegCount = board.pegCount
        return terminalParityIsSinglePeg(board, pegCount) { row, col -> row + col } &&
            terminalParityIsSinglePeg(board, pegCount) { row, col -> row - col }
    }

    private inline fun terminalParityIsSinglePeg(
        board: Board,
        pegCount: Int,
        coordinate: (Int, Int) -> Int,
    ): Boolean {
        val classParity = BooleanArray(COLORING_MODULUS)
        for (position in board.spec.inPlay) {
            if (board.cellAt(position) != Cell.Peg) continue
            val residue = Math.floorMod(coordinate(position.row, position.col), COLORING_MODULUS)
            classParity[residue] = !classParity[residue]
        }
        // Each move flips all three colour parities; reaching one peg takes pegCount-1 moves.
        val movesFlipParity = ((pegCount - 1) and 1) == 1
        var oddClasses = 0
        for (parity in classParity) {
            if (parity xor movesFlipParity) oddClasses++
        }
        return oddClasses == SINGLE_PEG_ODD_CLASSES
    }
}
```

**Pitfall 1 lives here.** Generalising `SINGLE_PEG_ODD_CLASSES == 1` to "weight ≤ T and weight ≡ T
(mod 2)" makes the prune near-vacuous for T = 2 — and, second-order, un-mutes the Diamond solver
(today this returns `false` for *every* Diamond position in O(cells), so `Solver` never searches
it). The per-cell `Math.floorMod` loop is also the O(cells) cost RESEARCH replaces with 6
popcounts over precomputed class masks.

**Invariants to preserve:** one-directional (`false` ⟹ provably unsolvable, never a false
reject) and board-agnostic (freeform Challenge footprints run through it too).

---

### `domain/src/main/kotlin/.../scoring/ScoreCalculator.kt` + `ScoreParams.kt` — MODIFY

**Analog:** themselves; `GeneratorConstants.kt` for the "second scoped object" precedent.

**Function to make board-aware** (`ScoreCalculator.kt` lines 22–27) — note zero literals in the
body; every band value comes from `ScoreParams`:

```kotlin
    fun stars(pegsRemaining: Int): Int =
        when {
            pegsRemaining <= ScoreParams.THREE_STAR_MAX_PEGS -> ScoreParams.THREE_STARS
            pegsRemaining <= ScoreParams.TWO_STAR_MAX_PEGS -> ScoreParams.TWO_STARS
            else -> ScoreParams.MIN_STARS
        }
```

**Downstream caller inside the same object** (lines 57–74) — `evaluate` calls `stars(pegsRemaining)`
at line 64, so a new parameter must be threaded through `evaluate` too:

```kotlin
    fun evaluate(
        pegsRemaining: Int,
        pegsCleared: Int,
        elapsedSeconds: Int,
        levelMultiplier: Int = ScoreParams.DEFAULT_LEVEL_MULTIPLIER,
    ): ScoreResult =
        ScoreResult(
            stars = stars(pegsRemaining),
            score = score(...),
            pegsRemaining = pegsRemaining,
            ...
        )
```

**Constants pattern for `DiamondStarParams`** (`ScoreParams.kt` lines 12–36) — copy the
decision-ID-in-KDoc style:

```kotlin
object ScoreParams {
    /**
     * Max pegs remaining for a 3-star result (D-01). At or below this count the
     * board scores 3 stars; the boundary is 1 peg (a textbook perfect solve),
     * so `stars(0)` and `stars(1)` both yield 3.
     */
    const val THREE_STAR_MAX_PEGS = 1

    /**
     * Max pegs remaining for a 2-star result (D-01). Pegs remaining in the 2-3
     * band score 2 stars.
     */
    const val TWO_STAR_MAX_PEGS = 3

    /**
     * The star floor (D-01). More than [TWO_STAR_MAX_PEGS] pegs remaining still
     * earns this many stars — never 0 ...
     */
    const val MIN_STARS = 1

    /** Stars awarded in the top band (`<= `[THREE_STAR_MAX_PEGS] pegs, D-01). */
    const val THREE_STARS = 3

    /** Stars awarded in the middle band (2-3 pegs, D-01). */
    const val TWO_STARS = 2
    ...
}
```

**Anti-pattern already documented in-file** (`ScoreCalculator.kt` lines 37–38): *"this function
stays free of any level state (no `ScoreCalculator` ↔ level coupling)"*. Per-board bands must
arrive as `minimumPegs: Int` / the `BoardSpec`, **never** a Classic level id.

---

### `domain/src/main/kotlin/.../usecase/GetHintUseCase.kt` (use case, request-response) — MODIFY

**Analog:** itself.

**The whole decision body to extend with the book/recorded-line paths** (lines 46–58):

```kotlin
    operator fun invoke(
        board: Board,
        solution: List<Move>?,
        movesPlayed: Int,
        budget: SolverBudget = SolverBudget.DEFAULT,
        shouldCancel: () -> Boolean = { false },
    ): Move? {
        val recorded = solution?.getOrNull(movesPlayed)
        if (recorded != null && recorded in RulesEngine.legalMoves(board)) {
            return recorded
        }
        return Solver.firstWinningMove(board, budget, shouldCancel)
    }
```

**The legality-guard pattern to replicate for every book hit** (threat T-04-CT, class KDoc lines
25–30): *"Blind `solution[moveCount]` indexing is FORBIDDEN. The recorded move is returned only
when it is both in range AND currently a member of `RulesEngine.legalMoves`."* RESEARCH's Security
table repeats this for the book: **never return a book move unchecked**.

**Book-miss vs. book-`NoWinningMove` (Pattern 2 correctness rule):** a *miss* must fall through to
the Solver; a *hit* recording "no winning move" is authoritative. Do not collapse them to `null`
at the same point in the chain.

**Purity constraint restated in the class KDoc** (lines 31–33): *"no Android, no coroutines.
Cancellation crosses the boundary as a plain [shouldCancel] lambda."*

---

### `domain/src/main/kotlin/.../assist/OpeningBook.kt` + `RecordedLines.kt` (data source, file-I/O) — NEW

**Analog:** `domain/src/test/.../golden/solutions/RecordedSolutions.kt` — **role-match; the I/O
mechanism must change** (see §No Analog Found #1).

**Decoder pattern to copy** (`RecordedSolutions.kt` lines 16–63) — note the named parse constants
(no literals), the `require` with a regeneration hint, and the exact-inverse-of-encoder contract:

```kotlin
object RecordedSolutions {
    private const val MOVE_ARROW = "->"
    private const val OPEN_PAREN = "("
    private const val CLOSE_PAREN = ")"
    private const val FIELD_SEPARATOR = ","
    private const val FROM_OVER_TO = 3
    private const val ROW_INDEX = 0
    private const val COL_INDEX = 1

    /** Loads and decodes the winning line committed for [fixtureName] (e.g. `"european37"`). */
    fun load(fixtureName: String): List<Move> {
        val file = File(SolutionFixtures.DIRECTORY, "$fixtureName.txt")
        require(file.exists()) {
            "Missing recorded-solution fixture '$fixtureName.txt' at ${file.path}. " +
                "Re-run with UPDATE_GOLDEN=true to (re)generate fixtures."
        }
        return file
            .readText()
            .trim()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map(::decodeMove)
            .toList()
    }

    private fun decodeMove(line: String): Move {
        val parts = line.split(MOVE_ARROW)
        require(parts.size == FROM_OVER_TO) { "Malformed move '$line': expected from->over->to." }
        return Move(
            from = decodePosition(parts[0]),
            over = decodePosition(parts[1]),
            to = decodePosition(parts[2]),
        )
    }

    private fun decodePosition(token: String): Position {
        val fields = token.trim().removePrefix(OPEN_PAREN).removeSuffix(CLOSE_PAREN).split(FIELD_SEPARATOR)
        require(fields.size == 2) { "Malformed position '$token': expected (row,col)." }
        return Position(row = fields[ROW_INDEX].trim().toInt(), col = fields[COL_INDEX].trim().toInt())
    }
}
```

**Encoder side (the format contract this must invert)** — `golden/BoardCodec.kt` lines 43–51:

```kotlin
    /** Encodes [moves] one-per-line as `(r,c)->(r,c)->(r,c)`, stable across runs. */
    fun encodeMoves(moves: List<Move>): String {
        if (moves.isEmpty()) return EMPTY_MOVES
        return moves.joinToString(LINE_SEPARATOR) { move ->
            encodePosition(move.from) + MOVE_ARROW + encodePosition(move.over) + MOVE_ARROW + encodePosition(move.to)
        }
    }

    private fun encodePosition(position: Position): String = "(${position.row},${position.col})"
```

`BoardCodec` currently lives in the **test** source set (`domain/src/test/.../golden/BoardCodec.kt`).
If `OpeningBook`/`RecordedLines` decode `main`-source resources, either the move encoding moves to
`main` or the book gets its own `main`-side codec. Planner decision — flag it.

**Deviation required — fail-closed parse.** `RecordedSolutions` throws (`require`) on a malformed
fixture, which is correct for a test but wrong for production. RESEARCH §Security: *"any malformed
line makes the whole book unavailable (fall through to the Solver) rather than silently serving a
partial book."* Copy the fail-closed idiom from `SnapshotMapper.decode` (`SnapshotMapper.kt`
lines 118–123) instead:

```kotlin
    fun decode(blob: String): ResumeSnapshotDto? =
        try {
            json.decodeFromString<ResumeSnapshotDto>(blob)
        } catch (ignored: IllegalArgumentException) {
            null
        }
```

**Fixture-catalog pattern for `RecordedLines`** (`AuthoringSolver.kt` lines 244–266) — a scoped
`object` holding a `Fixture` data class + `ALL` list keyed to real `Boards.*` specs:

```kotlin
internal object SolutionFixtures {
    /** Subdirectory (under `src/test/resources/golden`) holding the committed fixtures. */
    const val DIRECTORY = "src/test/resources/golden/solutions"
    private const val FIXTURE_SUFFIX = ".txt"

    /** One board's fixture recipe: solve [spec] from the single-vacancy [startVacancy]. */
    internal data class Fixture(
        val name: String,
        val spec: BoardSpec,
        val startVacancy: Position,
    ) {
        val fileName: String get() = name + FIXTURE_SUFFIX
    }

    val ALL: List<Fixture> =
        listOf(
            Fixture(name = "english33", spec = Boards.English33, startVacancy = Position(row = 3, col = 3)),
            Fixture(name = "european37", spec = Boards.European37, startVacancy = Position(row = 3, col = 2)),
            Fixture(name = "asymmetric39", spec = Boards.Asymmetric39, startVacancy = Position(row = 4, col = 3)),
            Fixture(name = "wiegleb45", spec = Boards.Wiegleb45, startVacancy = Position(row = 4, col = 4)),
        )
}
```

Diamond-41 gets a fifth entry here (target = `minimumPegs`, not 1), and the KDoc at lines 235–242
("there is no one-peg solution to author", "the floor is several pegs") is Pitfall-7 surface #2.

---

### `domain/src/test/.../golden/solutions/BeamSearchAuthoring.kt` + `OpeningBookAuthoring.kt` — NEW

**Analog:** `AuthoringSolver.kt` (exact — same source set, same purpose, same `UPDATE_GOLDEN`
discipline).

**Authoring-tool KDoc + constants header to copy** (lines 13–65):

```kotlin
/**
 * One-off AUTHORING-TIME solver that GENERATES the committed recorded-solution
 * fixtures (D-19). Confined to the `:domain` **test** source set and never on any
 * shipped/runtime classpath, so the "no Solver in `:domain` main" scope (and
 * `DomainPurityTest`) holds.
 * ...
 * Fixtures are emitted in our exact `(row,col)`/[Move] encoding by
 * construction (Pitfall 7); a fixed seed sequence keeps regenerated fixtures byte-stable.
 */
object AuthoringSolver {
    /** Fixed base seed so the randomized tie-breaking is fully reproducible (byte-stable fixtures). */
    private const val BASE_SEED = 0x5150_2602L

    /** Independent randomized attempts (fresh seed + memo each) before giving up. */
    private const val MAX_RESTARTS = 30

    /**
     * Per-attempt ceiling on distinct (symmetry-canonical) positions. ...
     */
    private const val MAX_DISTINCT_PER_ATTEMPT = 25_000_000L
    ...
}
```

**Emitter pattern to copy** (`AuthoringSolver.kt` lines 213–227) — builds the start board from the
real `Boards.*` spec (never a transcribed mask), fails loudly with the visited count:

```kotlin
    fun writeFixtures(solutionsDir: File) {
        solutionsDir.mkdirs()
        SolutionFixtures.ALL.forEach { fixture ->
            val start =
                fixture.spec.inPlay
                    .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
                    .withoutPeg(fixture.startVacancy)
            val line =
                requireNotNull(forwardSolve(start)) {
                    "Board '${fixture.name}' did not solve to one peg from ${fixture.startVacancy} " +
                        "within the search budget (visited=$lastVisited)."
                }
            File(solutionsDir, fixture.fileName).writeText(BoardCodec.encodeMoves(line))
        }
    }
```

**Search skeleton for the beam frontier** (lines 80–118) — reuse `RulesEngine.legalMoves` /
`applyMove` as the move authority and `VisitedSet` for the per-ply dedup:

```kotlin
    fun forwardSolve(start: Board): List<Move>? {
        val symmetries = symmetryPermutations(start.spec)
        val center = centroidOf(start.spec)
        for (attempt in 0 until MAX_RESTARTS) {
            val random = Random(BASE_SEED + attempt)
            val seen = VisitedSet()
            fun search(board: Board, path: ArrayDeque<Move>): List<Move>? {
                // Goal check FIRST (before the memo) ...
                if (board.pegCount == SOLVED_PEG_COUNT) return path.toList()
                ...
            }
            val result = search(start, ArrayDeque())
            if (result != null) { lastVisited = seen.size.toLong(); return result }
        }
        return null
    }
```

**Pitfall 6 (byte-stability):** the existing tool promises byte-stable regeneration via
`BASE_SEED + attempt`. A beam with a `HashSet` frontier breaks that — use an ordered frontier +
seeded RNG, or explicitly downgrade the contract in the KDoc.

**Heap hook to extend** (`domain/build.gradle.kts` lines 12–22):

```kotlin
// The shipped recorded-solution tests only REPLAY committed fixtures and need no
// extra memory. ONLY when regenerating those fixtures (`UPDATE_GOLDEN=true`) does
// the one-off authoring search run; give it a larger heap then so normal CI runs
// stay at the default (D-19 — the search never ships).
tasks.withType<Test>().configureEach {
    val regeneratingFixtures =
        System.getenv("UPDATE_GOLDEN")?.equals("true", ignoreCase = true) == true
    if (regeneratingFixtures) {
        maxHeapSize = "4g"
    }
}
```

(→ `"8g"` per RESEARCH §Environment Availability.)

---

### `domain/src/test/.../golden/solutions/SolutionTest.kt` (test, golden replay) — MODIFY

**Analog:** itself.

**Replay assertion to extend for the Diamond floor** (lines 80–97) — re-validates every step
against `RulesEngine.legalMoves` before applying, then asserts the terminal peg count:

```kotlin
    private fun assertReplayReducesToOnePeg(
        fixtureName: String,
        initial: Board,
    ) {
        val moves = RecordedSolutions.load(fixtureName)
        val finalBoard =
            moves.fold(initial) { board, move ->
                assertWithMessage(
                    "Recorded move %s for '%s' is not legal on the replayed board.",
                    move,
                    fixtureName,
                ).that(move in RulesEngine.legalMoves(board)).isTrue()
                RulesEngine.applyMove(board, move)
            }
        assertWithMessage("'%s' recorded solution must reduce to one peg.", fixtureName)
            .that(finalBoard.pegCount)
            .isEqualTo(SOLVED_PEG_COUNT)
    }
```

**Authoring gate + flag idiom** (lines 74–103) — the only regeneration entry point; a normal CI run
performs zero search:

```kotlin
    /**
     * Authoring-only regeneration of the committed fixtures (D-19). A no-op unless
     * `UPDATE_GOLDEN=true` is set, so a normal CI run performs zero search — the
     * search code only ever executes at authoring time.
     */
    @Test
    fun regenerateSolutionFixturesWhenUpdateFlagSet() {
        if (!isUpdateEnabled()) return
        AuthoringSolver.writeFixtures(File(SolutionFixtures.DIRECTORY))
    }

    private fun isUpdateEnabled(): Boolean =
        isFlagTrue(System.getenv(UPDATE_GOLDEN_FLAG)) ||
            isFlagTrue(System.getProperty(UPDATE_GOLDEN_FLAG))

    private fun isFlagTrue(value: String?): Boolean = value?.equals("true", ignoreCase = true) == true
```

This `env-or-system-property` idiom (duplicated in `GoldenMasterAssert.kt` lines 63–67) is the
**only in-repo precedent for gating a slow test** — the `DepthSweepTest` `-PrunSweep=true` gate
has to extend it (see §No Analog Found #2).

Class KDoc line 25 (*"search confirms the floor is several pegs"*) is Pitfall-7 surface #3.

---

### `domain/src/test/.../solver/DepthSweepTest.kt` (test, batch sweep) — NEW

**Analog:** `SolverBudgetTest.kt` (exact — corpus loop, per-call wall-clock guard, headroom
assertion, extensive "why this corpus" KDoc).

**Corpus-loop + wall-clock-guard pattern to copy** (lines 37–88):

```kotlin
    @Test
    fun everyTractableSolvablePositionReturnsAMoveWithinBudget() {
        var maxObservedNodes = 0L
        val perCallCapNanos = PER_CALL_CAP_MS * NANOS_PER_MILLI

        fun decide(board: Board, label: String) {
            val startedAt = System.nanoTime()
            val move =
                Solver.firstWinningMove(board, SolverBudget.DEFAULT) {
                    System.nanoTime() - startedAt > perCallCapNanos
                }
            assertWithMessage("solvable position %s must return a move within budget", label)
                .that(move)
                .isNotNull()
            maxObservedNodes = maxOf(maxObservedNodes, Solver.lastVisited)
        }

        SolutionFixtures.ALL.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            if (fixture.name == REFERENCE_BOARD) decide(start, "${fixture.name}@start")
            for (prefixLength in maxOf(0, line.size - ENDGAME_DEPTH) until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                decide(board, "${fixture.name}@prefix$prefixLength")
            }
        }
        ...
        assertThat(SolverBudget.DEFAULT_MAX_NODES).isAtLeast(HEADROOM_FACTOR * maxObservedNodes)
    }

    private companion object {
        /** Minimum node headroom the budget must keep over the largest observed search. */
        private const val HEADROOM_FACTOR = 4L
        /** The board whose full solution line is tractable end-to-end (measured ~21k nodes). */
        private const val REFERENCE_BOARD = "english33"
        /** Endgame-tail depth measured on the larger boards (cheap, few-peg positions). */
        private const val ENDGAME_DEPTH = 10
        /** Hard per-solve wall-clock guard — tractable positions resolve far inside it. */
        private const val PER_CALL_CAP_MS = 2_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
```

**Shared position builders to reuse** (`SolverTest.kt` lines 61–72) — already `internal` and
already shared by three solver tests:

```kotlin
internal object SolverSamples {
    /** The single-vacancy start board for [fixture] (identical to the shipped `Boards.*Initial`). */
    fun startBoard(fixture: SolutionFixtures.Fixture): Board =
        fixture.spec.inPlay
            .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    /** Folds [moves] forward through [RulesEngine.applyMove] from [start]. */
    fun replay(start: Board, moves: List<Move>): Board =
        moves.fold(start) { board, move -> RulesEngine.applyMove(board, move) }
    ...
}
```

The depth-K frontier enumerator is a BFS over `RulesEngine.legalMoves` + `BoardCanonical.canonical`
dedup — same primitives, new composition.

**Pitfall 5:** measured ~6 min at K=4. Must NOT land in the default `:domain:test` run.

---

### `domain/src/test/.../solver/CanonicalTest.kt` (test, property) — NEW

**Analog:** `PagodaTest.kt` (exact — a small one-directional property spec over the fixture corpus).

**Whole file to model on** (lines 16–37):

```kotlin
class PagodaTest {
    @Test
    fun rejectsAKnownDeadPosition() {
        // Two pegs on the same mod-3 colour class with no peg between them: the
        // colour-parity necessary condition proves this cannot reach a single peg.
        assertThat(Pagoda.mayBeSolvable(SolverSamples.deadBoard)).isFalse()
    }

    @Test
    fun neverRejectsASolvablePosition() {
        SolutionFixtures.ALL.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Every prefix of a real winning line is, by construction, still solvable —
            // a necessary condition must accept all of them.
            for (prefixLength in 0 until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                assertThat(Pagoda.mayBeSolvable(board)).isTrue()
            }
        }
    }
}
```

Properties to assert (Pitfall 4): `canonical(m).countOneBits() == m.countOneBits()` and
`canonical(canonical(m)) == canonical(m)`, over all five footprints — including the bit-40/44 cases
that a hardcoded 5-chunk canonicaliser silently drops.

---

### `domain/src/test/.../assist/OpeningBookTest.kt` (test, golden integrity) — NEW

**Analogs:** `SolutionTest.assertReplayReducesToOnePeg` (excerpt above) for the replay leg;
`ChallengeSolvabilityTest.kt` lines 24–36 for the property-over-samples shape:

```kotlin
    @Test
    fun everyGeneratedSolutionReplaysLegallyAndFoldsToOnePeg() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val finalBoard =
                level.solution.fold(level.startingBoard) { board, move ->
                    // Each recorded move must be a legal forward move on the replayed board.
                    assertThat(move in RulesEngine.legalMoves(board)).isTrue()
                    RulesEngine.applyMove(board, move)
                }
            assertThat(finalBoard.pegCount).isEqualTo(1)
        }
    }
```

Book integrity = the same fold, per entry, terminating at `spec.minimumPegs` instead of `1`.

---

### `domain/src/test/.../scoring/ScoreCalculatorTest.kt` (test, unit) — MODIFY

**Analog:** itself. Band-boundary style to copy for the Diamond bands (lines 14–33):

```kotlin
    @Test
    fun starsAreThreeAtAndBelowTheThreeStarBoundary() {
        // Boundary at THREE_STAR_MAX_PEGS = 1: a perfect (1-peg) solve scores 3,
        // and the theoretical 0-peg case also scores 3 (never undercut the floor).
        assertThat(ScoreCalculator.stars(pegsRemaining = 1)).isEqualTo(3)
        assertThat(ScoreCalculator.stars(pegsRemaining = 0)).isEqualTo(3)
    }

    @Test
    fun starsAreTwoAcrossTheTwoToThreePegBand() {
        assertThat(ScoreCalculator.stars(pegsRemaining = 2)).isEqualTo(2)
        assertThat(ScoreCalculator.stars(pegsRemaining = 3)).isEqualTo(2)
    }

    @Test
    fun starsAreOneAboveTheTwoStarBandAndNeverZero() {
        // D-01 overrides spec §7: more than 3 pegs still earns MIN_STARS = 1, not 0.
        assertThat(ScoreCalculator.stars(pegsRemaining = 4)).isEqualTo(1)
        assertThat(ScoreCalculator.stars(pegsRemaining = 32)).isEqualTo(1)
    }
```

Class KDoc (lines 6–12) states magic numbers are *intentional* in test code — detekt's `MagicNumber`
rule excludes the test source set. Diamond bands: 2→3★, 3–4→2★, ≥5→1★.

---

### `domain/src/test/.../usecase/GetHintUseCaseTest.kt` (test, unit) — MODIFY

**Analog:** itself. The **solver-never-ran spy** is the pattern the Diamond "book-served, no
1.5s search" assertion (Pitfall 1) must reuse (lines 32–54):

```kotlin
    @Test
    fun onRecordedLineReturnsTheRecordedMoveWithoutInvokingTheSolver() {
        val level = LevelGenerator.generate(LEVEL_WITH_RECORDED_LINE)
        var solverPolled = 0

        val move =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = 0,
                // If the Solver ran it would poll this ~every 4096 nodes; a `true`
                // return would also force it to null. A non-null recorded result with
                // zero polls proves the Solver branch was never entered.
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(move).isEqualTo(level.solution[0])
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(move)
    }
```

---

### `app/.../data/resume/ResumeSnapshotDto.kt` + `SnapshotMapper.kt` (Pitfall 3) — MODIFY

**Analog:** themselves.

**DTO shape — every new field must be nullable-with-default for backward compat** (lines 43–55):

```kotlin
@Serializable
data class ResumeSnapshotDto(
    val mode: String,
    val levelId: String,
    val challengeLevel: Int? = null,
    val generatorVersion: Int? = null,
    val inPlay: List<IntArray>,
    val targetRow: Int? = null,
    val targetCol: Int? = null,
    val initialPegs: Long,
    val moves: List<IntArray>,
    val elapsedMs: Long,
)
```

**The exact function that silently defaults `minimumPegs` to 1** (`SnapshotMapper.kt` lines 146–161):

```kotlin
    private fun ResumeSnapshotDto.toBoardSpecOrNull(): BoardSpec? {
        val positions = inPlay.mapNotNull { it.toPositionOrNull() }
        val row = targetRow
        val col = targetCol
        val target = if (row != null && col != null) Position(row = row, col = col) else null
        val footprintIsWellFormed =
            positions.size == inPlay.size &&
                positions.size in 1..MAX_BOARD_POSITIONS &&
                positions.distinct().size == positions.size
        val targetIsWellFormed = (row == null) == (col == null) && (target == null || target in positions)
        return if (footprintIsWellFormed && targetIsWellFormed) {
            BoardSpec(inPlay = positions, target = target)
        } else {
            null
        }
    }
```

**Range-validation pattern to copy for `minimumPegs in 1..spec.size`** (`ResumeSnapshotDto.kt`
lines 127–134) — reject → whole snapshot rejected → "no resume", never a crash:

```kotlin
internal fun IntArray.toPositionOrNull(): Position? =
    with(ResumeSnapshotFormat) {
        if (size != POSITION_TUPLE_SIZE) return null
        val row = this@toPositionOrNull[POSITION_ROW]
        val col = this@toPositionOrNull[POSITION_COL]
        if (row !in 0 until MAX_BOARD_POSITIONS || col !in 0 until MAX_BOARD_POSITIONS) return null
        Position(row = row, col = col)
    }
```

**Format-constants object to extend** (lines 65–101) — `ResumeSnapshotFormat`, with the on-disk
contract warning: *"These indices are an on-disk contract. Changing one changes how every stored
blob is read; a real format change needs a new field rather than a re-shuffle."*

**Forward-compat guarantee already in place** (`SnapshotMapper.kt` line 59):
`private val json = Json { ignoreUnknownKeys = true }` — *"Unknown keys are ignored so a future
field cannot orphan today's saved games."* An old blob simply lacks `minimumPegs`; the planner must
decide (Open Question 3) whether absence means "default 1" or "re-resolve from `ClassicLevelSource`
by level id".

**Test analog:** `SnapshotMapperTest.kt` already has 18 cases including
`aPreviouslyValidEnglish33SnapshotStillDecodesToTheSameBoard` (line 198) — the legacy-blob case to
clone for a Diamond-41 blob written before this phase.

**Verified fact (closes RESEARCH assumption A6):** `LevelResultEntity` (`app/.../data/db/
LevelResultEntity.kt` lines 25–38) stores `mode, levelId, stars, bestTimeMs, bestScore,
completedAt` — **`pegsRemaining` is NOT persisted.** No one-shot Diamond star re-derivation is
possible; the upsert-on-better path is the only migration, exactly as CONTEXT assumed.

---

### `app/.../ui/game/GameViewModel.kt` (viewmodel, event-driven) — MODIFY (call site only)

**Analog:** itself.

**Stars call site that gains the board dimension** (lines 661–672):

```kotlin
                    val pegsRemaining = nextBoard.pegCount
                    val startingPegCount = initialBoard?.pegCount ?: pegsRemaining
                    _finished.trySend(
                        scoreCalculator.evaluate(
                            pegsRemaining = pegsRemaining,
                            pegsCleared = startingPegCount - pegsRemaining,
                            elapsedSeconds = _uiState.value.elapsedSeconds,
                            levelMultiplier = levelMultiplier,
                        ),
                    )
```

`nextBoard.spec.minimumPegs` is available right there — no new ViewModel state.

**Hint orchestration — UNCHANGED by this phase** (lines 832–869). Reproduced so the planner can
confirm the deadline/cancellation contract is untouched (D-07) and that `hintJob?.cancel()` already
enforces single-in-flight (Pitfall 1 mitigation):

```kotlin
        fun onHintRequested() {
            hintJob?.cancel()
            _uiState.update {
                it.copy(solverThinking = true, noWinningMove = false, hintSource = null, hintTarget = null)
            }
            hintJob =
                viewModelScope.launch {
                    when (adGateway.requestRewarded(AdReason.HINT)) { ... }
                    val deadline = timeSource.markNow() + SolverBudget.DEFAULT.deadline
                    val move =
                        withContext(defaultDispatcher) {
                            getHintUseCase(
                                board = _uiState.value.board,
                                solution = solution.takeIf { onRecordedLine },
                                movesPlayed = moveHistory.size,
                                shouldCancel = { !isActive || deadline.hasPassedNow() },
                            )
                        }
                    _uiState.update {
                        if (move != null) {
                            it.copy(hintSource = move.from, hintTarget = move.to, solverThinking = false)
                        } else {
                            it.copy(noWinningMove = true, solverThinking = false)
                        }
                    }
                }
        }
```

---

### `03-VALIDATION.md` / `04-VALIDATION.md` (doc contract) — MODIFY (DEBT-03)

**Analog:** `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-VALIDATION.md`
(exact — the only `status: approved` + `nyquist_compliant: true` contract with a fully populated
verification map).

**Frontmatter target state:**

```yaml
---
phase: 05
slug: persistence-resume-settings
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-13
---
```

**Per-Task Verification Map row shape to copy** (vs. the literal `4-XX-XX` / `TBD` placeholders
currently in 03/04):

```markdown
| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-03 | 03 | 2 | PERS-03 | T-05-03 | Resume snapshot round-trips board+full moveHistory+elapsed, NO regeneration; corrupt blob → null (fail-safe) | unit | `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapperTest" --tests "*DomainPurityTest"` | ❌ W0 | ⬜ pending |
```

**Sign-off block to copy** (note `[x]` on the nyquist line + a dated Approval string):

```markdown
## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-07-13 (plan-check passed; every executable task carries a scoped `<automated>` verify)
```

**Audit frontmatter to update** (`v1.0-MILESTONE-AUDIT.md` lines 27–31):

```yaml
nyquist:
  compliant_phases: [01, 02, 05]
  partial_phases: [03, 04]
  missing_phases: []
  overall: partial
```

Its `tech_debt` block (lines 14–26) also carries the two stale claims D-06 targets — the
"(4,2) start" line and the Wiegleb/solver envelope line.

---

## Shared Patterns

### 1. Domain purity — no Android, no coroutines
**Source:** `domain/build.gradle.kts` lines 1–5; `GetHintUseCase.kt` lines 31–33; `Solver.kt` line 27
**Apply to:** every `:domain` file in this phase
```kotlin
plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
    id("standards")
}
```
Cancellation crosses the boundary only as `shouldCancel: () -> Boolean = { false }`. Never
introduce `suspend`. `DomainPurityTest`/`PlatformBoundaryTest` are the gates. `OpeningBook` must use
`Class.getResourceAsStream`, never `android.content.res`.

### 2. Scoped constants objects, decision-ID KDoc
**Source:** `scoring/ScoreParams.kt` lines 3–10; `generator/GeneratorConstants.kt` lines 3–14
**Apply to:** `DiamondStarParams`, `MINIMUM_PEGS_DEFAULT`, `DEFAULT_PER_ATTEMPT_MAX_NODES`, book `K`
```kotlin
/**
 * Scoped tunable constants for the peg-solitaire scoring model.
 *
 * Split by scope (QUAL-03's no-magic-number rule): every value is a KDoc'd
 * `const val` tied to the design decision that fixes it, rather than a literal
 * inlined in [ScoreCalculator]. ...
 */
object ScoreParams { ... }
```
One object per scope. Every const's KDoc names the decision ID (D-xx / DEBT-xx) that fixes it and
says whether it is frozen or tunable.

### 3. Golden-master generate/replay split
**Source:** `SolutionTest.kt` lines 69–103; `AuthoringSolver.kt` lines 13–38, 213–227;
`GoldenMasterAssert.kt` lines 17–67
**Apply to:** the Diamond floor line, the opening book, any new fixture
- Generation lives in the **test** source set, runs only under `UPDATE_GOLDEN=true`
- Fixtures are generated against real `Boards.*` values — never hand-transcribed (Pitfall 7)
- Normal CI runs perform **zero search**; they only replay/look up
- Flag read is `System.getenv(FLAG) || System.getProperty(FLAG)`, case-insensitive `"true"`

### 4. `RulesEngine` is the single move authority — re-validate everything
**Source:** `GetHintUseCase.kt` lines 53–56; `SolutionTest.kt` lines 86–93; `SnapshotMapper.kt`
lines 130–139; `ChallengeSolvabilityTest.kt` lines 29–34
**Apply to:** book hits, recorded-line hits, fixture replay, resume replay
```kotlin
        val recorded = solution?.getOrNull(movesPlayed)
        if (recorded != null && recorded in RulesEngine.legalMoves(board)) {
            return recorded
        }
```
Threat T-04-CT: a hinted move must be a legal move of the *current* board. Never return a stored
move unchecked.

### 5. Fail-closed decoding of untrusted / on-disk input
**Source:** `SnapshotMapper.kt` lines 38–55 (class KDoc), 118–123 (`decode`), 141–161
**Apply to:** `OpeningBook` parse, `RecordedLines` parse, `minimumPegs` decode
> *"Nothing read back from disk is trusted (Security §V5, T-05-03/T-05-04) ... [decode] never
> throws ([Json] failures degrade to `null`) and [fromDto] validates before it rebuilds ... Any
> failure yields `null`, which callers surface as 'no resume' ... rather than as a crash."*

A malformed book line makes the **whole book** unavailable (fall through to Solver), not a partial
book. `minimumPegs` must be validated `in 1..spec.size` on decode.

### 6. KDoc that explains *why*, with measurements and threat IDs
**Source:** `SolverBudgetTest.kt` lines 13–35; `SolverBudget.kt` lines 25–36; `AuthoringSolver.kt`
lines 49–57
Every non-obvious constant carries its measurement and its standing instruction, e.g.
*"RAISE this value if the measurement ever exceeds the 4x headroom — never lower the assertion."*
This phase changes several of those measurements (Pitfall 2) — re-document, do not silently edit.

### 7. Gitflow + gates
**Source:** `CLAUDE.md`; RESEARCH §Project Constraints
Branch `gsd/phase-8-assist-generator-quality-debt` off `develop`. Gate:
`./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug`.

---

## No Analog Found

| File / Mechanism | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `domain/src/main/.../assist/OpeningBook.kt` — **classpath resource loading** | data source | file-I/O | **Zero occurrences of `getResourceAsStream` / `getResource` / `javaClass.classLoader` anywhere in `app/src` or `domain/src`** (verified by grep). Every existing fixture read is `java.io.File("src/test/resources/golden/...")` — a *relative path that only resolves because Gradle sets the test working dir to the module root*. That mechanism cannot work in a shipped APK. `domain/src/main/resources/` does not exist yet either. This is a genuinely new mechanism: use RESEARCH §Runtime State Inventory + assumption A5 (and the Phase-6 R8 resource-shrinking coordination note), and treat the Kotlin-string-constant fallback as the escape hatch. |
| `DepthSweepTest.kt` — **Gradle-property gating (`-PrunSweep=true`)** | test infra | — | No test in the repo is gated by a Gradle property, and `domain/build.gradle.kts` has no `systemProperty`/`Test` filtering wiring. The nearest precedent is the `UPDATE_GOLDEN` env-or-system-property read *inside the test body* (`SolutionTest.kt:99–103`, `GoldenMasterAssert.kt:63–67`) plus the `tasks.withType<Test>` heap hook (`domain/build.gradle.kts:16–22`). Planner must choose: extend the in-body flag idiom (cheapest, matches precedent) or add a real gated Gradle task wired into `check` but not `:domain:test` (Pitfall 5). |
| `SolverTables.kt` — **bitboard move/parity/symmetry-chunk tables** | utility | transform | Partial analog only. `BoardCanonical` supplies the "derive `IntArray`s from a `BoardSpec`" shape and `Boards.kt`'s layout objects supply the "precomputed array from layout rules" idiom, but nothing in the repo builds `needMask`/`toMask`/`xorMask` `LongArray`s, colour-class masks, or 8×N×256 chunk LUTs — nor memoises tables per `BoardSpec`. Use RESEARCH §Pattern 3 as the source, and add the Pitfall-4 round-trip property test since there is no existing correctness net for this code shape. |

---

## Metadata

**Analog search scope:**
`domain/src/main/kotlin/com/app/pegsolitaire/domain/{model,solver,scoring,usecase,rules,generator}/`,
`domain/src/test/kotlin/com/app/pegsolitaire/domain/{golden,golden/solutions,solver,scoring,usecase,generator}/`,
`domain/src/test/resources/golden/`, `domain/build.gradle.kts`,
`app/src/main/java/com/app/pegsolitaire/{data/resume,data/db,ui/game}/`,
`app/src/test/java/com/app/pegsolitaire/data/resume/`,
`.planning/milestones/v1.0-phases/*/`, `.planning/milestones/v1.0-MILESTONE-AUDIT.md`

**Files scanned:** 44 (24 read in full, 20 via targeted grep/head)
**Pattern extraction date:** 2026-08-29
