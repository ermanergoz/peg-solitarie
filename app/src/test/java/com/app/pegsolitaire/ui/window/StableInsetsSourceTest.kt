package com.app.pegsolitaire.ui.window

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The single stable-inset source, held in the source tree (docs/tech-design.md §3.8 (d);
 * docs/ux-spec.md D-05g part 3). Every screen pads off one value, the bars' stable insets unioned
 * with the display cutout (`WindowInsets.stableSystem`, in `ui/window/StableInsets.kt`), because a
 * visible inset is zero while a bar is hidden and animates while it slides. Criteria 30d–30f prove
 * the behaviour on the screens that exist; this keeps a screen added or edited later from reading a
 * visible inset again, which those tests would only catch on the screens they compose.
 *
 * Read from `app/src/main`'s Kotlin with comment lines dropped (`WindowThemeApi35FilesTest`'s shape):
 * - no file but the source reads a bar's inset through Compose — `safeDrawing`, `safeContent`,
 *   `safeGestures`, `statusBars`, `navigationBars` or a bare `systemBars`, or the padding modifier of
 *   any of them. The window controller's type masks (`WindowInsetsCompat.Type.systemBars()`, which
 *   the bar seam hides and shows) are not insets and are not matched;
 * - no file names Material 3's visible-inset defaults, `ScaffoldDefaults.contentWindowInsets` and
 *   `TopAppBarDefaults.windowInsets`;
 * - only the source opts in to `ExperimentalLayoutApi`, whose `systemBarsIgnoringVisibility` it is;
 * - in every file, every `Scaffold(` passes `contentWindowInsets = WindowInsets.stableSystem` and every
 *   `TopAppBar(` passes its top and horizontal sides,
 *   `windowInsets = WindowInsets.stableSystem.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)`.
 */
class StableInsetsSourceTest {
    /**
     * The repository root: the nearest ancestor of the working directory holding
     * `settings.gradle.kts`, so the read behaves the same from the IDE and from Gradle.
     */
    private val repositoryRoot: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, ROOT_MARKER).isFile }
            ?: error("No ancestor of ${File(".").absolutePath} contains $ROOT_MARKER")
    }

    /** Every Kotlin file under the main source root, by repository-relative path, comments dropped. */
    private val sources: Map<String, String> by lazy {
        val root = File(repositoryRoot, MAIN_SOURCES)
        check(root.isDirectory) { "$MAIN_SOURCES is not a directory under $repositoryRoot" }
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == KOTLIN }
            .associate { file -> file.relativeTo(repositoryRoot).invariantSeparatorsPath to codeOf(file) }
    }

    @Test
    fun noFileButTheStableSourceReadsABarsInsetThroughCompose() {
        val offenders =
            sources
                .filterKeys { it != STABLE_SOURCE }
                .flatMap { (path, code) -> VISIBLE_INSET_READ.findAll(code).map { "$path: ${it.value}" } }

        assertWithMessage("Compose bar-inset reads outside $STABLE_SOURCE").that(offenders).isEmpty()
    }

    @Test
    fun noFileNamesMaterialsVisibleInsetDefaults() {
        val offenders =
            sources.flatMap { (path, code) -> M3_VISIBLE_DEFAULTS.findAll(code).map { "$path: ${it.value}" } }

        assertWithMessage("Material 3 visible-inset defaults named in app/src/main").that(offenders).isEmpty()
    }

    @Test
    fun onlyTheStableSourceOptsInToTheExperimentalLayoutApi() {
        val optedIn = sources.filterValues { EXPERIMENTAL_LAYOUT_API in it }.keys

        assertWithMessage("the files that name $EXPERIMENTAL_LAYOUT_API")
            .that(optedIn)
            .containsExactly(STABLE_SOURCE)
    }

    @Test
    fun everyScaffoldPadsItsContentOffTheStableSource() {
        assertEveryCallPassesTheStableSource(
            call = SCAFFOLD_CALL,
            argument = SCAFFOLD_STABLE_ARGUMENT,
            what = "Scaffold",
        )
    }

    @Test
    fun everyTopAppBarPadsOffTheStableSource() {
        assertEveryCallPassesTheStableSource(
            call = TOP_APP_BAR_CALL,
            argument = TOP_APP_BAR_STABLE_ARGUMENT,
            what = "TopAppBar",
        )
    }

    /**
     * In each file, as many [call]s as [argument]s; and at least one [call] in the tree, so a wrong
     * path cannot pass for a clean one.
     */
    private fun assertEveryCallPassesTheStableSource(
        call: Regex,
        argument: Regex,
        what: String,
    ) {
        val calls = sources.mapValues { (_, code) -> call.findAll(code).count() }
        assertWithMessage("$what calls found under $MAIN_SOURCES").that(calls.values.sum()).isGreaterThan(0)
        sources.forEach { (path, code) ->
            assertWithMessage("$what calls in $path that pass the stable source, against all its $what calls")
                .that(argument.findAll(code).count())
                .isEqualTo(calls.getValue(path))
        }
    }

    /**
     * The file's code: every line that is a comment — `//`, a block or KDoc line — dropped, and a
     * trailing `//` comment cut from a code line.
     */
    private fun codeOf(file: File): String =
        file
            .readLines()
            .filterNot { line -> COMMENT_PREFIXES.any { line.trimStart().startsWith(it) } }
            .joinToString(separator = "\n") { line -> TRAILING_COMMENT.replace(line, "") }
}

private const val ROOT_MARKER = "settings.gradle.kts"
private const val MAIN_SOURCES = "app/src/main/java"
private const val KOTLIN = "kt"

/** The one file allowed to read the bars' insets: `WindowInsets.stableSystem`. */
private const val STABLE_SOURCE = "app/src/main/java/com/app/pegsolitaire/ui/window/StableInsets.kt"

private const val EXPERIMENTAL_LAYOUT_API = "ExperimentalLayoutApi"

private val COMMENT_PREFIXES = listOf("//", "/*", "*")

/** A `//` comment after code, preceded by whitespace so a URL's `://` is left alone. */
private val TRAILING_COMMENT = Regex("""\s+//.*$""")

/**
 * A Compose read of a bar's or the safe area's inset: the `WindowInsets` members and their padding
 * modifiers, and the stable insets of one bar. Not preceded by `Type.`, which is the window
 * controller's type mask, not an inset.
 */
private val VISIBLE_INSET_READ =
    Regex(
        """(?<!Type\.)\b(?:(?:safeDrawing|safeContent|safeGestures|systemBars)(?:Padding)?""" +
            """|(?:statusBars|navigationBars)(?:Padding|IgnoringVisibility)?)\b""",
    )

private val M3_VISIBLE_DEFAULTS =
    Regex("""\bScaffoldDefaults\.contentWindowInsets\b|\bTopAppBarDefaults\.windowInsets\b""")

private val SCAFFOLD_CALL = Regex("""\bScaffold\(""")
private val SCAFFOLD_STABLE_ARGUMENT = Regex("""\bcontentWindowInsets\s*=\s*WindowInsets\.stableSystem\b""")
private val TOP_APP_BAR_CALL = Regex("""\bTopAppBar\(""")
private val TOP_APP_BAR_STABLE_ARGUMENT =
    Regex(
        """\bwindowInsets\s*=\s*WindowInsets\.stableSystem\s*\.only\(\s*""" +
            """WindowInsetsSides\.Horizontal\s*\+\s*WindowInsetsSides\.Top\s*\)""",
    )
