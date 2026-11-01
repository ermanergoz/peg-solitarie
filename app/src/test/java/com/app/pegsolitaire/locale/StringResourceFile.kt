package com.app.pegsolitaire.locale

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * One `strings.xml` as the locale tests read it: its `<string>` and `<plurals>` entries by name, each
 * value as the raw text the resource carries (Android's `\'` escapes kept, XML character references
 * resolved), and the names the file marks `translatable="false"`.
 *
 * A plain XML read of the working tree, runner-free like `UserFacingTextHasNoProtectedNamesTest`:
 * these tests are about the two resource files as written, not about what a device resolves.
 */
internal class StringResourceFile private constructor(
    val strings: Map<String, String>,
    val plurals: Map<String, Map<String, String>>,
    val untranslatable: Set<String>,
    val translatableAttributeCount: Int,
) {
    /** The `<string>` entries a translation must carry: every one not marked `translatable="false"`. */
    val translatableStrings: Map<String, String> get() = strings.filterKeys { it !in untranslatable }

    /** The `<plurals>` entries a translation must carry. */
    val translatablePlurals: Map<String, Map<String, String>> get() = plurals.filterKeys { it !in untranslatable }

    /** Every value in the file, plural items included. */
    val allValues: List<String> get() = strings.values + plurals.values.flatMap { it.values }

    companion object {
        fun read(file: File): StringResourceFile {
            val root =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(file)
                    .documentElement
            val strings = linkedMapOf<String, String>()
            val plurals = linkedMapOf<String, Map<String, String>>()
            val untranslatable = mutableSetOf<String>()
            var translatableAttributes = 0
            root.childElements().forEach { element ->
                val name = element.getAttribute(NAME_ATTRIBUTE)
                if (element.hasAttribute(TRANSLATABLE_ATTRIBUTE)) {
                    translatableAttributes++
                    if (element.getAttribute(TRANSLATABLE_ATTRIBUTE) == FALSE) untranslatable += name
                }
                when (element.tagName) {
                    STRING_TAG -> strings[name] = element.textContent
                    PLURALS_TAG ->
                        plurals[name] =
                            element
                                .childElements()
                                .filter { it.tagName == ITEM_TAG }
                                .associate { it.getAttribute(QUANTITY_ATTRIBUTE) to it.textContent }
                }
            }
            return StringResourceFile(strings, plurals, untranslatable, translatableAttributes)
        }

        private fun Element.childElements(): List<Element> =
            (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

        private const val STRING_TAG = "string"
        private const val PLURALS_TAG = "plurals"
        private const val ITEM_TAG = "item"
        private const val NAME_ATTRIBUTE = "name"
        private const val QUANTITY_ATTRIBUTE = "quantity"
        private const val TRANSLATABLE_ATTRIBUTE = "translatable"
        private const val FALSE = "false"
    }
}

/**
 * The format specifiers of [value] as a multiset: each specifier once per occurrence, keyed by its
 * argument index (`1$` for `%1$d`, empty for a bare `%d`) and its conversion letter. A literal `%%`
 * is not a specifier.
 */
internal fun formatArguments(value: String): Map<String, Int> =
    FORMAT_SPECIFIER
        .findAll(value)
        .filter { it.groupValues[CONVERSION_GROUP] != LITERAL_PERCENT }
        .map { it.groupValues[INDEX_GROUP] + it.groupValues[CONVERSION_GROUP] }
        .groupingBy { it }
        .eachCount()

/** The repository root: the nearest ancestor of the working directory holding `settings.gradle.kts`. */
internal val repositoryRoot: File by lazy {
    generateSequence(File(".").absoluteFile) { it.parentFile }.firstOrNull { File(it, ROOT_MARKER).isFile }
        ?: error("No ancestor of ${File(".").absolutePath} contains $ROOT_MARKER.")
}

/** `app/src/main/res`, where the resource folders the locale tests read live. */
internal val mainResDir: File get() = File(repositoryRoot, MAIN_RES_DIR)

/** Java's `Formatter` syntax: `%`, an optional `n$` index, flags, width, precision, the conversion. */
private val FORMAT_SPECIFIER = Regex("""%(\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?([a-zA-Z%])""")
private const val INDEX_GROUP = 1
private const val CONVERSION_GROUP = 2
private const val LITERAL_PERCENT = "%"
private const val ROOT_MARKER = "settings.gradle.kts"
private const val MAIN_RES_DIR = "app/src/main/res"
