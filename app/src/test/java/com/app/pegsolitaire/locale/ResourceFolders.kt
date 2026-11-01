package com.app.pegsolitaire.locale

import java.io.File

/*
 * The languages the app ships, read from its resource folders the way Android reads their
 * qualifiers. Shared by `AppLanguagesTest`, which holds the per-app language list to them, and
 * `BundledFontGlyphCoverageTest`, which holds every font to their characters, so the two can never
 * disagree about which languages ship.
 */

/** The language of the unqualified `values` folder: the English the app was written in. */
internal const val BASE_LANGUAGE = "en"

/**
 * Each shipped language and its `values` folder: [BASE_LANGUAGE] and the unqualified folder first,
 * then every `values-<language>` folder that holds a `strings.xml`, by language.
 */
internal fun shippedLanguageFolders(): Map<String, File> {
    val qualified =
        mainResDir
            .listFiles { dir -> dir.isDirectory && File(dir, STRINGS_FILE).isFile }
            .orEmpty()
            .mapNotNull { dir -> languageOfValuesFolder(dir.name)?.let { it to dir } }
            .sortedBy { (language, _) -> language }
    return linkedMapOf(BASE_LANGUAGE to File(mainResDir, VALUES_FOLDER)) + qualified
}

/**
 * The language a `values` folder's qualifiers name, written as `locales_config.xml` writes it: `tr`
 * for `values-tr`, `tr-TR` for `values-tr-rTR`, `sr-Latn` for `values-b+sr+Latn`. Null for a folder
 * whose qualifiers name no language, such as `values-night` or `values-v35`, and for one that is not
 * a `values` folder. The mobile-network qualifiers, the only ones Android orders before the
 * language, are skipped; `car` is the car UI-mode qualifier, not a language.
 */
internal fun languageOfValuesFolder(folderName: String): String? {
    val qualifiers = folderName.split(QUALIFIER_SEPARATOR)
    val fromLanguage = qualifiers.drop(1).dropWhile { NETWORK_QUALIFIER.matches(it) }
    val first = fromLanguage.firstOrNull()?.takeIf { qualifiers.first() == VALUES_FOLDER }
    val region = fromLanguage.getOrNull(1)?.takeIf { REGION_QUALIFIER.matches(it) }?.drop(1)
    return when {
        first == null -> null
        BCP47_QUALIFIER.matches(first) -> first.removePrefix(BCP47_PREFIX).replace(BCP47_JOIN, LANGUAGE_TAG_JOIN)
        LANGUAGE_QUALIFIER.matches(first) && first != CAR_UI_MODE ->
            listOfNotNull(first, region).joinToString(LANGUAGE_TAG_JOIN)
        else -> null
    }
}

private const val STRINGS_FILE = "strings.xml"
private const val VALUES_FOLDER = "values"
private const val QUALIFIER_SEPARATOR = "-"
private const val BCP47_PREFIX = "b+"
private const val BCP47_JOIN = "+"
private const val LANGUAGE_TAG_JOIN = "-"
private const val CAR_UI_MODE = "car"
private val NETWORK_QUALIFIER = Regex("""(?:mcc|mnc)\d+""")
private val LANGUAGE_QUALIFIER = Regex("""[a-z]{2,3}""")
private val REGION_QUALIFIER = Regex("""r[A-Z]{2}""")
private val BCP47_QUALIFIER = Regex("""b\+[a-z]{2,3}(?:\+[A-Za-z0-9]+)*""")
