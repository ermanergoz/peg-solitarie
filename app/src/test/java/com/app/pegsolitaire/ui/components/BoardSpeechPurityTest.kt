package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.DoNotIncludeAndroidTestOutput
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.runner.RunWith

/**
 * TB-12, pinned: nothing spoken about the board can leak a winning move, because nothing that speaks
 * can reach the code that finds one. Every cell text, action label and announcement is built from the
 * board, the selection, `legalTargets`, the move just made or undone and the revealed hint — so the
 * classes that build them (the model, its format, the cell overlay and the board's announcer) may not
 * depend on the solver, the hint use case, the assist search or the level generator. The revealed hint
 * reaches them only as two positions in the state, after the player asked for it.
 */
@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(
    packages = ["com.app.pegsolitaire.ui.components"],
    // Shipped classes only: the tests that drive the model may reach the generator for fixtures.
    importOptions = [DoNotIncludeAndroidTestOutput::class],
)
class BoardSpeechPurityTest {
    @ArchTest
    val nothingThatSpeaksTheBoardCanReachTheSolverOrTheGenerator: ArchRule =
        noClasses()
            .that(speakTheBoard)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..domain.solver..",
                "..domain.usecase..",
                "..domain.assist..",
                "..domain.generator..",
            )

    private companion object {
        /** The classes (and their nested and synthetic classes) that turn the board into words. */
        val SPEAKING_PREFIXES = listOf("BoardSpeech", "BoardEvent", "BoardCellOverlay", "BoardAnnounc")

        val speakTheBoard: DescribedPredicate<JavaClass> =
            DescribedPredicate.describe("speak the board") { javaClass ->
                val outermost = javaClass.name.substringAfterLast('.').substringBefore('$')
                SPEAKING_PREFIXES.any { outermost.startsWith(it) }
            }
    }
}
