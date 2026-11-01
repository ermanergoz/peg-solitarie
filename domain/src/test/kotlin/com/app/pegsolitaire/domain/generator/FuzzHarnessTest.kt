package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.TestFlags
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * rules/puzzle-design.md invariant 8, the fuzz harness: generate → validate → solve → replay →
 * verify win, over the first `FUZZ_SEEDS` Challenge levels (docs/tech-design.md §7.3).
 *
 * - **The seed is the level number.** `FUZZ_SEEDS = N` walks levels `1..min(N, MAX)`: the chain is
 *   sequential, so a prefix is the only sample that costs O(N), through one
 *   [LevelGenerator.generateLadder] pass. A value above [ChallengeLevelBounds.MAX] runs the whole
 *   finite level space and says so — the release tier, `FUZZ_SEEDS=100000`, is exhaustive.
 * - **"Solve" is certificate verification.** Each level's recorded [GeneratedLevel.solution] is the
 *   generator's proof; replaying it is the check. Running the solver per level would re-create the
 *   depth sweep's cost ten-thousand-fold and prove nothing the certificate does not.
 * - **Per level:** its number equals its position in the chain; every move stays inside the
 *   footprint and is legal on the board it is applied to; the replay ends at exactly one peg; the
 *   level carries [GeneratorConstants.GENERATOR_VERSION]; its spec carries the one-peg target
 *   [BoardConstants.MINIMUM_PEGS_DEFAULT] (docs/gdd.md I5/G8), so a Classic floor leaking into
 *   Challenge with a footprint — Diamond-41's two — is a named failing level, not a sampled miss.
 * - **Over the run:** a second, independently built ladder is element-wise identical (invariant 3,
 *   determinism), and the examined count equals `min(N, MAX)` and is above zero — anti-vacuity,
 *   the depth sweep's T-08-39 standard: "0 levels examined, 0 failures" cannot read as a pass.
 * - **Failing levels go to `build/fuzz-failures.txt`** (the module's, so `domain/build/…`), one
 *   line each, before the test fails. The file is deleted when a run starts, so a green run leaves
 *   none; the release checklist reads that path.
 *
 * Gated like `DepthSweepTest`: the body runs only with [TestFlags.RUN_FUZZ], which the dedicated
 * `:domain:fuzz` task sets and `check` depends on. Under a plain `:domain:test` it returns at once.
 */
class FuzzHarnessTest {
    @Test
    fun everyLevelOfTheRequestedPrefixReplaysItsCertificateToOnePegAndRegeneratesIdentically() {
        if (!TestFlags.isEnabled(TestFlags.RUN_FUZZ)) return

        val requested = FuzzSeeds.parse(TestFlags.value(TestFlags.FUZZ_SEEDS))
        val upTo = minOf(requested, ChallengeLevelBounds.MAX)
        if (requested > ChallengeLevelBounds.MAX) {
            println(
                "fuzz: requested $requested exceeds the level space ${ChallengeLevelBounds.MAX}; " +
                    "running the whole space (exhaustive)",
            )
        }
        FAILURES_FILE.delete()

        val failures = mutableListOf<String>()
        val firstPass = mutableListOf<LevelRecord>()
        var examined = 0
        LevelGenerator.generateLadder(upTo).forEachIndexed { index, level ->
            examined++
            certificateFailure(expectedLevel = index + 1, level = level)?.let(failures::add)
            firstPass += LevelRecord.of(level)
        }
        val divergences = determinismFailures(firstPass, LevelGenerator.generateLadder(upTo))
        failures += divergences

        if (failures.isNotEmpty()) writeFailures(failures)
        val determinism = if (divergences.isEmpty()) "pass" else "fail"
        println(
            "fuzz: examined $examined levels (requested $requested), " +
                "determinism $determinism, ${failures.size} failures",
        )

        assertWithMessage("levels examined (anti-vacuity, T-08-39)").that(examined).isEqualTo(upTo)
        assertWithMessage("levels examined (anti-vacuity, T-08-39)").that(examined).isGreaterThan(0)
        assertWithMessage("failing levels, also written to %s", FAILURES_FILE.absolutePath)
            .that(failures)
            .isEmpty()
    }

    /** Everything wrong with one level's certificate, as one `level <n>: …` line, or null. */
    private fun certificateFailure(
        expectedLevel: Int,
        level: GeneratedLevel,
    ): String? {
        val reasons =
            listOfNotNull(
                "levelNumber ${level.levelNumber}".takeIf { level.levelNumber != expectedLevel },
                "generatorVersion ${level.generatorVersion}, expected ${GeneratorConstants.GENERATOR_VERSION}"
                    .takeIf { level.generatorVersion != GeneratorConstants.GENERATOR_VERSION },
                "minimumPegs ${level.startingBoard.spec.minimumPegs}, expected ${BoardConstants.MINIMUM_PEGS_DEFAULT}"
                    .takeIf { level.startingBoard.spec.minimumPegs != BoardConstants.MINIMUM_PEGS_DEFAULT },
                replayFailure(level),
            )
        return if (reasons.isEmpty()) null else "level $expectedLevel: ${reasons.joinToString("; ")}"
    }

    /**
     * Replays the recorded solution through the unchanged [RulesEngine] and names the first thing
     * wrong with it, or returns null when it folds the starting board to exactly one peg. The replay
     * stops at the first bad move: applying a move that leaves the footprint would throw.
     */
    private fun replayFailure(level: GeneratedLevel): String? {
        val spec = level.startingBoard.spec
        var board = level.startingBoard
        for ((step, move) in level.solution.withIndex()) {
            val problem =
                when {
                    move.positions().any { it !in spec } -> "leaves the footprint"
                    move !in RulesEngine.legalMoves(board) -> "is not legal on the replayed board"
                    else -> null
                }
            if (problem != null) return "move ${step + 1} $move $problem"
            board = RulesEngine.applyMove(board, move)
        }
        return "final pegCount ${board.pegCount}, expected 1".takeIf { board.pegCount != 1 }
    }

    /** Invariant 3: the second ladder must match the first element by element, and in length. */
    private fun determinismFailures(
        firstPass: List<LevelRecord>,
        secondLadder: Sequence<GeneratedLevel>,
    ): List<String> {
        val divergences = mutableListOf<String>()
        var secondCount = 0
        secondLadder.forEachIndexed { index, level ->
            secondCount++
            if (firstPass.getOrNull(index) != LevelRecord.of(level)) {
                divergences += "level ${index + 1}: regenerating the ladder gave a different level (determinism)"
            }
        }
        if (secondCount != firstPass.size) {
            divergences += "ladder: the second pass yielded $secondCount levels, the first ${firstPass.size}"
        }
        return divergences
    }

    private fun writeFailures(failures: List<String>) {
        FAILURES_FILE.parentFile?.mkdirs()
        FAILURES_FILE.writeText(failures.joinToString(separator = "\n", postfix = "\n"))
    }

    private fun Move.positions(): List<Position> = listOf(from, over, to)

    /**
     * What determinism compares: the board's content, not its spec's identity — a freeform
     * footprint is a fresh `BoardSpec` instance per generation, so `inPlay` is compared instead.
     */
    private data class LevelRecord(
        val levelNumber: Int,
        val pegs: Long,
        val inPlay: List<Position>,
        val solution: List<Move>,
        val generatorVersion: Int,
    ) {
        companion object {
            fun of(level: GeneratedLevel): LevelRecord =
                LevelRecord(
                    levelNumber = level.levelNumber,
                    pegs = level.startingBoard.pegs,
                    inPlay = level.startingBoard.spec.inPlay,
                    solution = level.solution,
                    generatorVersion = level.generatorVersion,
                )
        }
    }

    private companion object {
        /** Relative to the test's working directory, the module: `domain/build/fuzz-failures.txt`. */
        val FAILURES_FILE = File("build", "fuzz-failures.txt")
    }
}
