package com.app.pegsolitaire.locale

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The Turkish file against the English one (docs/ux/localisation-tr.md §7.6): every key the base
 * file lets a translator reach has a Turkish value, the Turkish file has no key of its own, every
 * value keeps its English format arguments, every plural declares the two quantities Turkish uses,
 * and nothing English is left behind untranslated.
 *
 * Lint's MissingTranslation and ExtraTranslation see the first half on every Check; nothing in lint
 * sees a Turkish value that still reads in English, or a `%2$d` that became `%2$s`. Those fail here,
 * in the build, rather than on a Turkish device.
 */
class TurkishStringsParityTest {
    private val english by lazy { StringResourceFile.read(File(mainResDir, BASE_STRINGS)) }
    private val turkish by lazy { StringResourceFile.read(File(mainResDir, TURKISH_STRINGS)) }

    @Test
    fun everyTranslatableStringHasATurkishValueAndTheTurkishFileHasNoOther() {
        assertThat(turkish.strings.keys).containsExactlyElementsIn(english.translatableStrings.keys)
    }

    @Test
    fun everyTranslatablePluralHasATurkishValueAndTheTurkishFileHasNoOther() {
        assertThat(turkish.plurals.keys).containsExactlyElementsIn(english.translatablePlurals.keys)
    }

    @Test
    fun everyTurkishStringKeepsTheFormatArgumentsOfItsEnglishValue() {
        turkish.strings.forEach { (name, value) ->
            val englishValue = english.strings[name] ?: return@forEach
            assertWithMessage(name).that(formatArguments(value)).isEqualTo(formatArguments(englishValue))
        }
    }

    @Test
    fun everyTurkishPluralItemKeepsTheFormatArgumentsOfItsEnglishItem() {
        turkish.plurals.forEach { (name, items) ->
            val englishItems = english.plurals[name] ?: return@forEach
            items.forEach { (quantity, value) ->
                val englishValue = englishItems[quantity] ?: englishItems.getValue(OTHER)
                assertWithMessage("$name/$quantity")
                    .that(formatArguments(value))
                    .isEqualTo(formatArguments(englishValue))
            }
        }
    }

    @Test
    fun everyTurkishPluralDeclaresExactlyTheOneAndOtherQuantities() {
        // CLDR gives Turkish `one` and `other`: lint's MissingQuantity wants both, and any third is dead.
        turkish.plurals.forEach { (name, items) ->
            assertWithMessage(name).that(items.keys).containsExactly(ONE, OTHER)
        }
    }

    @Test
    fun noTurkishValueIsAnEnglishLeftoverButTheFiveFormatJoins() {
        val stringsLeftEnglish = turkish.strings.filter { (name, value) -> english.strings[name] == value }.keys
        val pluralItemsLeftEnglish =
            turkish.plurals.flatMap { (name, items) ->
                items
                    .filter { (quantity, value) -> english.plurals[name]?.get(quantity) == value }
                    .map { (quantity, _) -> "$name/$quantity" }
            }

        assertThat(FORMAT_JOINS).containsAtLeastElementsIn(stringsLeftEnglish)
        assertThat(pluralItemsLeftEnglish).isEmpty()
    }

    @Test
    fun theTurkishFileCarriesNoTranslatableAttribute() {
        // `translatable` is the base file's decision; in a translation it means nothing (spec §7.4).
        assertThat(turkish.translatableAttributeCount).isEqualTo(0)
    }

    /** The reader the tests above trust: indices, conversions and repeats count; `%%` does not. */
    @Test
    fun theFormatArgumentReaderCountsIndexConversionAndRepeats() {
        assertThat(formatArguments("%2\$d yıldızın %1\$d tanesi")).containsExactly("2\$d", 1, "1\$d", 1)
        assertThat(formatArguments("%1\$d hak, %1\$d daha")).containsExactly("1\$d", 2)
        assertThat(formatArguments("%d piyon, %02d:%02d, %1\$s")).containsExactly("d", 3, "1\$s", 1)
        assertThat(formatArguments("100%% hazır")).isEmpty()
    }
}

private const val BASE_STRINGS = "values/strings.xml"
private const val TURKISH_STRINGS = "values-tr/strings.xml"
private const val ONE = "one"
private const val OTHER = "other"

/**
 * The five keys the spec copies into Turkish as they are (§5.6): format joins and a separator, whose
 * whole value is punctuation around arguments. Any other Turkish value equal to its English one is
 * an untranslated leftover.
 */
private val FORMAT_JOINS =
    setOf(
        "elapsed_time_spoken",
        "board_a11y_separator",
        "board_a11y_announce_step",
        "results_a11y_summary",
        "home_stat_pill_desc",
    )
