/**
 * Which `src/<name>` source sets the standards gates read, and what happens when one appears that
 * nobody has classified (F-8 / F-9).
 *
 * Two lists, deliberately different, and the difference is the whole point:
 *  - [shippedSourceRoots] feeds `adIdLiteralCheck` (PLAT-06 / D-05), `gamesIdLiteralCheck`
 *    (PLAT-06 / D-17) and `audioFocusLiteralCheck` (SOUND-06). It covers every source set whose
 *    content SHIPS in an artifact — `src/main`, `src/release`, `src/debug` — WHOLE: the gates read
 *    every file under each directory, whatever its content type, and deliberately exclude every
 *    test source set. Whole, because a per-directory list had a hole at the most likely paste site
 *    (docs/tech-design.md §9.3, amended 2026-10-18): the manifest is where AdMob's setup puts the
 *    app ID, and `assets/` (the licence texts) and `:domain`'s Java `resources/` (the opening book)
 *    ship too, yet none of them was under `java`, `kotlin` or `res`. Scanning the whole source set
 *    makes the source-set list the only list, and the tripwire below guards it. Each of the three
 *    gates has the same recorded reason for skipping `src/test`: it legitimately asserts on
 *    Google's test-publisher prefix, it spells Play Games key names (`BuildConfigIdsTest`), and it
 *    may name the audio-focus API in an assertion. `src/testDebug` is a second spelling of the
 *    same exemption, so it is excluded too. This is the opposite of the detekt widening below, and
 *    on purpose: style rules apply to test code, "no production credential in source" does not.
 *  - [variantSourceRoots] feeds the plain `detekt` task, whose own defaults see only
 *    `src/main` + `src/test`. It covers the build-type source sets detekt would otherwise treat as
 *    an undeclared blanket suppression, test source sets INCLUDED.
 *
 * `src/release` is the substantive half of the first list and `src/debug` is not cosmetic: D-05's
 * rule is "Google's test units in debug, real untracked ones in release", so a real ad unit id in
 * `src/debug` is the breach the rule exists to prevent — it serves real ads to a developer's own
 * build, which is AdMob invalid traffic and risks the account rather than the binary. SOUND-06's
 * evidence is gathered on debug builds for the same reason.
 *
 * **The tripwire.** Both lists are hand-maintained, and "the KDoc tells the next reader to extend
 * the list" is exactly the protection that failed in F-1. [unknownSourceSetNames] is checked at
 * configuration time against the directories that actually exist, so the day someone adds a
 * `staging` build type or a product flavour the build FAILS with a message naming both lists,
 * instead of three gates quietly scanning a directory that no longer holds all the source.
 */
object SourceSetPolicy {
    /** The directory every source set lives under, relative to the module. */
    const val SOURCE_DIR = "src"

    /** Source sets whose content ships inside an artifact. */
    val SHIPPED_SOURCE_SET_NAMES = listOf("main", "release", "debug")

    /** Source sets detekt's own defaults miss. `main` and `test` are already covered by them. */
    val VARIANT_SOURCE_SET_NAMES = listOf("debug", "release", "testDebug")

    /** detekt parses code only, so there is no `res` here. */
    val VARIANT_LANGUAGE_DIRS = listOf("java", "kotlin")

    /**
     * The source sets that exist in this repository, each decided for BOTH lists above, and no
     * other name. The tripwire's question is "has anyone classified this directory?", not "is it
     * scanned?", so a name is known only once it exists on disk AND both [shippedSourceRoots] and
     * [variantSourceRoots] have been decided for it, test source sets included. A name outside this
     * set means a new build type, a new product flavour or a new test dimension, and in every one
     * of those cases both lists need a decision from a human.
     *
     * This set once also pre-authorised the source sets AGP could create but this repository does
     * not have: `androidTest`, `androidTestDebug`, `androidTestRelease`, `screenshotTest`,
     * `screenshotTestDebug` and `testRelease`. That was a hole (auditor F-2, docs/tech-design.md
     * §9.3): only [VARIANT_SOURCE_SET_NAMES] widens the detekt task, so the Kotlin in a
     * pre-authorised `src/androidTest` would have been read by no detekt task while the tripwire
     * stayed silent. Now the day `src/androidTest` appears (the golden-image suite, design §8.5),
     * the wire trips and forces both decisions.
     */
    val KNOWN_SOURCE_SET_NAMES = setOf("main", "test", "debug", "release", "testDebug")

    /**
     * Module-relative roots the three literal gates scan, unfiltered — the caller drops absentees.
     * Each root is a whole source-set directory: the manifest, `assets/`, Java `resources/` and any
     * content type a later AGP adds are read with the code, so no second list can fall behind.
     */
    fun shippedSourceRoots(): List<String> = SHIPPED_SOURCE_SET_NAMES.map { "$SOURCE_DIR/$it" }

    /** Module-relative roots added to the plain `detekt` task, unfiltered. */
    fun variantSourceRoots(): List<String> = roots(VARIANT_SOURCE_SET_NAMES, VARIANT_LANGUAGE_DIRS)

    /**
     * What a gate's failure message says it scanned. A gate whose failure text misdescribes its own
     * scope is how F-1's false claim happened, so this is derived from the list, never retyped.
     */
    val SHIPPED_SOURCE_ROOTS_DESCRIPTION: String =
        "every file under " + shippedSourceRoots().joinToString(", ")

    /** The existing `src/<name>` directory names nobody has classified, in the order given. */
    fun unknownSourceSetNames(existingSourceSetNames: Iterable<String>): List<String> =
        existingSourceSetNames.filterNot { it in KNOWN_SOURCE_SET_NAMES }

    /**
     * The configuration-time failure. It names the module, the directory, both lists and the two
     * decisions their owner has to make, because the person who reads this is the person who just
     * added a build type and has no reason to know these gates exist.
     */
    fun unknownSourceSetMessage(
        modulePath: String,
        unknownSourceSetNames: List<String>,
    ): String =
        buildString {
            appendLine(
                "Unclassified source set(s) in $modulePath: " +
                    unknownSourceSetNames.joinToString(", ") { "$SOURCE_DIR/$it" },
            )
            appendLine(
                "  The standards gates read a hand-maintained list of source roots, so a new " +
                    "build type or product flavour would be scanned by NOTHING and the gates " +
                    "would still report success. That silence is what this failure replaces.",
            )
            appendLine(
                "  Decide both, in build-logic/src/main/kotlin/SourceSetPolicy.kt:",
            )
            appendLine(
                "    1. shippedSourceRoots — does this source set SHIP? Then add it to " +
                    "SHIPPED_SOURCE_SET_NAMES so the ad-ID, Play-Games-ID and audio-focus gates " +
                    "read it. Test source sets are excluded on purpose; see the KDoc.",
            )
            appendLine(
                "    2. variantSourceRoots — does it hold Kotlin detekt would otherwise not see? " +
                    "Then add it to VARIANT_SOURCE_SET_NAMES.",
            )
            append(
                "  Then add the name to KNOWN_SOURCE_SET_NAMES to clear this failure.",
            )
        }

    private fun roots(
        sourceSetNames: List<String>,
        languageDirs: List<String>,
    ): List<String> =
        sourceSetNames.flatMap { sourceSet ->
            languageDirs.map { language -> "$SOURCE_DIR/$sourceSet/$language" }
        }
}
