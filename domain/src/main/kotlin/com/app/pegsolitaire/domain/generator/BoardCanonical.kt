package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position

/**
 * Symmetry canonicalization for a board's `pegs` bitmask — the dedup gate's key.
 *
 * Lifted **verbatim** from the test-source `AuthoringSolver.symmetryPermutations`
 * /`canonical` (RESEARCH §Quality Gates — "lift that canonicalization rather than
 * re-deriving it"; §Don't Hand-Roll). The generator's symmetry-normalized dedup
 * gate (Pitfall 3) folds symmetric starting boards onto one key so a board and its
 * dihedral images count as the same puzzle "in the player's sequence".
 *
 * Pure Kotlin, zero Android deps (QUAL-01): operates only on [BoardSpec]/[Position]
 * and a `Long` mask.
 */
object BoardCanonical {
    /**
     * The bit permutations of [spec]'s symmetry group (the dihedral transforms of
     * its square bounding box that map the footprint onto itself). Applying any of
     * these to a `pegs` mask yields a symmetric position; folding a mask to the
     * lexicographically smallest image under them ([canonical]) gives a
     * symmetry-invariant key.
     *
     * A transform is included only if it maps every in-play cell onto another
     * in-play cell (preserves the footprint); asymmetric footprints keep just the
     * identity.
     */
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
    fun canonical(
        pegs: Long,
        symmetries: List<IntArray>,
    ): Long {
        var minimum = pegs
        for (perm in symmetries) {
            var transformed = 0L
            var remaining = pegs
            while (remaining != 0L) {
                val bit = java.lang.Long.numberOfTrailingZeros(remaining)
                transformed = transformed or (1L shl perm[bit])
                remaining = remaining and (remaining - 1)
            }
            if (transformed < minimum) minimum = transformed
        }
        return minimum
    }

    /**
     * The precomputed byte-chunk lookup tables of one footprint's symmetry group —
     * build once with [chunkTables], then permute any number of masks with
     * [canonicalFast].
     *
     * [perSymmetry] is indexed `[symmetryIndex][chunkIndex][byteValue]` and yields the
     * already-permuted `Long` contribution of that byte of the source mask, so a whole
     * 64-bit mask is transformed with `chunkCount` array lookups and ORs instead of one
     * iteration per set bit.
     */
    class ChunkTables internal constructor(
        internal val perSymmetry: Array<Array<LongArray>>,
    )

    /**
     * The [ChunkTables] for [spec]'s symmetry group, memoised on the footprint's
     * structural key so repeated searches over the same board pay the build cost once.
     *
     * **Pitfall 4 (a real, observed corruption — do not "simplify" this).** The chunk
     * count MUST be derived from `spec.size`. Hard-coding 5 chunks covers bits 0-39
     * only and silently DROPS bit 40 on the 41-hole Diamond and bits 40-44 on
     * Wiegleb-45. The canonical mask then holds fewer pegs than the position it came
     * from, the search memo conflates distinct positions, and the search "reaches"
     * states that do not exist — during research a Diamond-41 search reported reaching
     * one peg, which the GF(4) colour invariant proves impossible. `CanonicalTest`
     * pins the high-bit cases explicitly.
     *
     * **Size.** `symmetryCount * chunkCount * 256` longs — at most
     * `8 * 6 * 256 = 12288` longs (96 KB) for the largest shipped footprint, built once
     * per footprint and cached.
     */
    @Synchronized
    fun chunkTables(spec: BoardSpec): ChunkTables = cache.getOrPut(footprintKey(spec)) { buildChunkTables(spec) }

    /**
     * The lexicographically smallest image of [pegs] under [tables]' symmetry group —
     * an exact, faster equivalent of [canonical] for any mask whose set bits lie inside
     * the footprint the tables were built for. `CanonicalTest` gates that equivalence
     * over a seeded corpus on every shipped footprint, which is what makes swapping a
     * solver's inner loop onto this path safe.
     */
    fun canonicalFast(
        pegs: Long,
        tables: ChunkTables,
    ): Long {
        var minimum = pegs
        for (chunks in tables.perSymmetry) {
            var transformed = 0L
            var remaining = pegs
            var chunk = 0
            while (chunk < chunks.size) {
                val byteValue = (remaining and CHUNK_VALUE_MASK).toInt()
                if (byteValue != 0) transformed = transformed or chunks[chunk][byteValue]
                remaining = remaining ushr BITS_PER_CHUNK
                chunk++
            }
            if (transformed < minimum) minimum = transformed
        }
        return minimum
    }

    /**
     * The footprint's STRUCTURAL cache key: its in-play positions in the same stable
     * `(row, col)` order [BoardSpec] assigns bit indices in. Keying on this rather than
     * on referential identity means a spec rebuilt from a resume blob hits the same
     * cache entry as the shipped catalog spec it was rebuilt from.
     */
    private fun footprintKey(spec: BoardSpec): List<Position> =
        spec.inPlay.sortedWith(compareBy({ it.row }, { it.col }))

    private fun buildChunkTables(spec: BoardSpec): ChunkTables {
        val symmetries = symmetryPermutations(spec)
        val chunkCount = (spec.size + BITS_PER_CHUNK - 1) / BITS_PER_CHUNK
        return ChunkTables(Array(symmetries.size) { symmetry -> buildSymmetryTable(symmetries[symmetry], chunkCount) })
    }

    private fun buildSymmetryTable(
        permutation: IntArray,
        chunkCount: Int,
    ): Array<LongArray> = Array(chunkCount) { chunk -> buildChunkTable(permutation, chunk) }

    /**
     * The permuted image of every possible value of one byte-chunk of a mask, built
     * incrementally: the image of `value` is the image of its lowest set bit OR'd with
     * the image of the rest. Source bits past the end of [permutation] are outside the
     * footprint and can never be set in a real position mask, so they contribute
     * nothing.
     */
    private fun buildChunkTable(
        permutation: IntArray,
        chunk: Int,
    ): LongArray {
        val firstBit = chunk * BITS_PER_CHUNK
        val table = LongArray(CHUNK_VALUE_COUNT)
        for (value in 1 until CHUNK_VALUE_COUNT) {
            val sourceBit = firstBit + Integer.numberOfTrailingZeros(value)
            val lowestBitImage = if (sourceBit < permutation.size) 1L shl permutation[sourceBit] else 0L
            table[value] = lowestBitImage or table[value and (value - 1)]
        }
        return table
    }

    /** Bits of a `pegs` mask covered by one lookup table — one byte, so 256 entries per chunk. */
    private const val BITS_PER_CHUNK = 8

    /** Distinct values one chunk can take (`2^BITS_PER_CHUNK`) — the per-chunk table length. */
    private const val CHUNK_VALUE_COUNT = 1 shl BITS_PER_CHUNK

    /** Low-bit mask that extracts one chunk from a `pegs` mask. */
    private const val CHUNK_VALUE_MASK = (CHUNK_VALUE_COUNT - 1).toLong()

    /**
     * Footprints whose tables stay resident. Challenge generates a FRESH footprint per
     * level, so an unbounded cache would grow without limit at up to 96 KB per entry.
     * Six covers the five Classic footprints plus the one Challenge footprint in play,
     * which is the whole working set of any real session.
     */
    private const val MAX_CACHED_FOOTPRINTS = 6

    /** `LinkedHashMap`'s default load factor — carried explicitly by the access-order constructor. */
    private const val CACHE_LOAD_FACTOR = 0.75f

    /**
     * Access-ordered, capacity-bounded table cache. Guarded by `@Synchronized` on
     * [chunkTables] rather than a coroutine primitive: `:domain` is pure Kotlin/JVM
     * with no coroutine dependency (QUAL-01).
     */
    private val cache =
        object : LinkedHashMap<List<Position>, ChunkTables>(MAX_CACHED_FOOTPRINTS, CACHE_LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<List<Position>, ChunkTables>): Boolean =
                size > MAX_CACHED_FOOTPRINTS
        }
}
