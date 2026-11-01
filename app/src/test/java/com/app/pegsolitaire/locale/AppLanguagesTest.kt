package com.app.pegsolitaire.locale

import android.content.res.Configuration
import android.content.res.XmlResourceParser
import com.app.pegsolitaire.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import androidx.compose.ui.R as ComposeUiR

/**
 * The languages Android 13+ offers for Peg Solitaire under Settings › System › Languages › App
 * languages (docs/ux/localisation-tr.md D-9, §7.4, C-5): the merged manifest's
 * `android:localeConfig`, and the locale list it names as the packaged resource parses.
 *
 * Robolectric's package parser leaves `ApplicationInfo`'s locale-config resource unset (the field is
 * API 33's, and `android.app.LocaleConfig` reports "not specified" here however the manifest reads),
 * so the attribute is read from the merged manifest Robolectric itself runs, the file AGP's
 * `test_config.properties` names: what the manifest merger handed the packager, not the source file.
 *
 * The expected list is derived from the resource folders, never typed: the base language plus every
 * `values-<language>` folder that holds a `strings.xml`. A language added without a picker entry, or
 * a picker entry with no strings behind it, fails here; so does a language the picker offers but the
 * build strips (`localeFilters`), because a player who picks it would read English; and so does a
 * library translation the build keeps for a language the app does not speak (spec C-6).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [APP_LANGUAGES_SDK])
class AppLanguagesTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun theMergedManifestPointsThePerAppLanguageSettingAtTheLocaleList() {
        val application = mergedManifest().getElementsByTagName(APPLICATION_TAG).item(0) as Element

        assertThat(application.getAttributeNS(ANDROID_NAMESPACE, LOCALE_CONFIG_ATTRIBUTE))
            .isEqualTo(XML_REFERENCE_PREFIX + app.resources.getResourceEntryName(R.xml.locales_config))
    }

    @Test
    fun theLocaleListNamesTheBaseLanguageAndEveryLanguageWithStrings() {
        assertThat(offeredLanguages()).containsExactlyElementsIn(languagesWithStrings())
    }

    @Test
    fun everyOfferedLanguageButTheBaseResolvesItsOwnAppAndLibraryStrings() {
        offeredLanguages().filter { it != BASE_LANGUAGE }.forEach { language ->
            val locale = Locale.forLanguageTag(language)
            assertWithMessage("$language, the app's strings")
                .that(stringIn(locale, APP_PROBE))
                .isNotEqualTo(stringIn(BASE_DEVICE_LOCALE, APP_PROBE))
            assertWithMessage("$language, Compose's own strings, which TalkBack speaks")
                .that(stringIn(locale, LIBRARY_PROBE))
                .isNotEqualTo(stringIn(BASE_DEVICE_LOCALE, LIBRARY_PROBE))
        }
    }

    @Test
    fun aLanguageTheAppDoesNotOfferShowsTheLibrariesInEnglishToo() {
        // Spec §7.4 and C-6: a German device reads English throughout, never English screens around
        // a library's German, so the build keeps no library translation the app itself lacks.
        val unoffered = Locale.GERMANY
        assertWithMessage("pick a language the app does not offer")
            .that(offeredLanguages())
            .doesNotContain(unoffered.language)

        assertThat(stringIn(unoffered, LIBRARY_PROBE)).isEqualTo(stringIn(BASE_DEVICE_LOCALE, LIBRARY_PROBE))
    }

    /** The folder reader the expected list rests on: a language comes only from a language qualifier. */
    @Test
    fun theFolderReaderTakesALanguageOnlyFromALanguageQualifier() {
        assertThat(languageOfValuesFolder("values-tr")).isEqualTo("tr")
        assertThat(languageOfValuesFolder("values-tr-rTR")).isEqualTo("tr-TR")
        assertThat(languageOfValuesFolder("values-b+sr+Latn")).isEqualTo("sr-Latn")
        assertThat(languageOfValuesFolder("values-mcc286-tr")).isEqualTo("tr")
        val noLanguage =
            listOf(
                "values",
                "values-night",
                "values-v35",
                "values-night-v35",
                "values-car",
                "values-sw600dp",
                "drawable-tr",
            )
        noLanguage.forEach { folder -> assertWithMessage(folder).that(languageOfValuesFolder(folder)).isNull() }
    }

    /** The `android:name` of every `<locale>` in the packaged `res/xml/locales_config.xml`. */
    private fun offeredLanguages(): List<String> {
        val parser = app.resources.getXml(R.xml.locales_config)
        val languages = mutableListOf<String>()
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            if (parser.eventType == XmlResourceParser.START_TAG && parser.name == LOCALE_TAG) {
                languages += checkNotNull(parser.getAttributeValue(ANDROID_NAMESPACE, NAME_ATTRIBUTE))
            }
        }
        return languages
    }

    /** String [id] as a [locale] device resolves it from the packaged resources. */
    private fun stringIn(
        locale: Locale,
        id: Int,
    ): String {
        val configuration = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(configuration).getString(id)
    }

    private fun languagesWithStrings(): List<String> =
        listOf(BASE_LANGUAGE) +
            mainResDir
                .listFiles { dir -> dir.isDirectory && File(dir, STRINGS_FILE).isFile }
                .orEmpty()
                .mapNotNull { languageOfValuesFolder(it.name) }

    private fun mergedManifest(): Element {
        val testConfig =
            Properties().apply {
                // The system loader: Robolectric's sandbox loader does not serve AGP's config file.
                val stream = checkNotNull(ClassLoader.getSystemResourceAsStream(TEST_CONFIG)) { TEST_CONFIG }
                stream.use { load(it) }
            }
        val manifest = File(checkNotNull(testConfig.getProperty(MERGED_MANIFEST_KEY)) { MERGED_MANIFEST_KEY })
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(manifest).documentElement
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val APP_LANGUAGES_SDK = 34

/** The language of the unqualified `values` folder: the English the app was written in. */
private const val BASE_LANGUAGE = "en"

/**
 * An English device as it really is, region and all. A bare `en` would resolve the libraries'
 * `en-XC` pseudo-locale (text wrapped in bidi marks), which no device ever runs.
 */
private val BASE_DEVICE_LOCALE: Locale = Locale.US

/** Any key the parity test holds translated shows whether a language's app strings shipped. */
private val APP_PROBE = R.string.settings_title

/**
 * A string the Compose UI library translates into German and Turkish alike ("Navigationsmenü",
 * "Gezinme menüsü"), so it shows which of a library's languages the build kept.
 */
private val LIBRARY_PROBE = ComposeUiR.string.navigation_menu

private const val STRINGS_FILE = "strings.xml"

/**
 * The language a `values` folder's qualifiers name, written as `locales_config.xml` writes it: `tr`
 * for `values-tr`, `tr-TR` for `values-tr-rTR`, `sr-Latn` for `values-b+sr+Latn`. Null for a folder
 * whose qualifiers name no language, such as `values-night` or `values-v35`, and for one that is not
 * a `values` folder. The mobile-network qualifiers, the only ones Android orders before the
 * language, are skipped; `car` is the car UI-mode qualifier, not a language.
 */
private fun languageOfValuesFolder(folderName: String): String? {
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

/** AGP's unit-test config on the test classpath, and its key for the merged manifest Robolectric runs. */
private const val TEST_CONFIG = "com/android/tools/test_config.properties"
private const val MERGED_MANIFEST_KEY = "android_merged_manifest"

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val APPLICATION_TAG = "application"
private const val LOCALE_CONFIG_ATTRIBUTE = "localeConfig"
private const val LOCALE_TAG = "locale"
private const val NAME_ATTRIBUTE = "name"
private const val XML_REFERENCE_PREFIX = "@xml/"
