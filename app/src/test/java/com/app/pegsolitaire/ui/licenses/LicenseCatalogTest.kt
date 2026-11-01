package com.app.pegsolitaire.ui.licenses

import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.BundledLicenseText
import com.app.pegsolitaire.data.licenses.BundledLicenseText.Apache2
import com.app.pegsolitaire.data.licenses.BundledLicenseText.CheckerQualMit
import com.app.pegsolitaire.data.licenses.BundledLicenseText.ChromiumBsd3
import com.app.pegsolitaire.data.licenses.BundledLicenseText.FredokaOfl
import com.app.pegsolitaire.data.licenses.BundledLicenseText.JakartaInjectNotice
import com.app.pegsolitaire.data.licenses.BundledLicenseText.Mpl2
import com.app.pegsolitaire.data.licenses.BundledLicenseText.NunitoOfl
import com.app.pegsolitaire.data.licenses.BundledLicenseText.ProtobufBsd3
import com.app.pegsolitaire.data.licenses.BundledLicenseText.PublicSuffixListNotice
import com.app.pegsolitaire.ui.licenses.LicenseEntryTarget.GoogleNotices
import com.app.pegsolitaire.ui.licenses.LicenseEntryTarget.Texts
import com.app.pegsolitaire.ui.licenses.LicenseLine.Address
import com.app.pegsolitaire.ui.licenses.LicenseLine.Plain
import com.app.pegsolitaire.ui.licenses.LicenseLine.SoundCount
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * LS-AC03 (spec §3.2, §3.2.1; brief §0.2): the Licenses catalogue is exactly THIRD_PARTY.md's
 * shipped, credit- or licence-bearing rows, in the spec's order, and nothing it must never list;
 * and the curated licence files and the catalogue name each other completely.
 *
 * [EXPECTED] is the spec's §3.2 table transcribed by hand — the release-manager diffs §3.2.1
 * against THIRD_PARTY.md at checklist items 1.4 and 6, and this pins the code to that table.
 * String values are read from `strings.xml` on disk, so the test needs no Android runtime.
 */
class LicenseCatalogTest {
    private val repoRoot = findRepositoryRoot()
    private val strings: Map<String, String> by lazy { readStringResources() }

    @Test
    fun theCatalogueEqualsTheThirdPartyMappingInOrder() {
        val actual = LicenseCatalog.sections

        assertThat(actual.map { it.title }).containsExactlyElementsIn(EXPECTED.map { it.title }).inOrder()
        EXPECTED.zip(actual).forEach { (expected, section) ->
            assertThat(section.entries.map { it.id })
                .containsExactlyElementsIn(expected.entries.map { it.id })
                .inOrder()
            expected.entries.zip(section.entries).forEach { (expectedEntry, entry) ->
                assertWithMessage(expectedEntry.id).that(entry).isEqualTo(expectedEntry)
            }
        }
    }

    @Test
    fun entryFindsEveryCatalogueIdAndNothingElse() {
        LicenseCatalog.sections.flatMap { it.entries }.forEach { entry ->
            assertThat(LicenseCatalog.entry(entry.id)).isEqualTo(entry)
        }
        assertThat(LicenseCatalog.entry("missing")).isNull()
        assertThat(LicenseCatalog.entry("")).isNull()
        assertThat(LicenseCatalog.entry("licenses/Apache-2.0.txt")).isNull()
    }

    @Test
    fun noNeverListedItemIsPresent() {
        val shown =
            LicenseCatalog.sections
                .flatMap { section -> section.entries }
                .flatMap { entry -> listOf(entry.title) + entry.lines.mapNotNull { (it as? Plain)?.text } }

        NEVER_LISTED.forEach { name ->
            val word = Regex("\\b${Regex.escape(name)}\\b", RegexOption.IGNORE_CASE)
            val scanned = shown.filterNot { id -> APP_NAMED_AS_MODIFIER == (id to name) }.map { id -> stringValue(id) }
            assertWithMessage("never listed: $name").that(scanned.filter { word.containsMatchIn(it) }).isEmpty()
        }
    }

    @Test
    fun everyBundledTextNamesAnExistingNonEmptyAssetFile() {
        BundledLicenseText.entries.forEach { text ->
            val file = File(repoRoot, "$ASSETS_DIR/${text.assetPath}")
            assertWithMessage(text.assetPath).that(file.isFile).isTrue()
            assertWithMessage(text.assetPath).that(file.length()).isGreaterThan(0L)
        }
    }

    /**
     * Every committed file in `assets/licenses/` is a [BundledLicenseText] (seven entries share
     * `Apache-2.0.txt` through one constant, spec LS-AC03 as amended), so no licence file ships
     * that no entry can open.
     */
    @Test
    fun everyCommittedLicenceFileIsOpenedByAtLeastOneEntry() {
        val committed = licencesDirectory().listFiles { file -> file.isFile }.orEmpty().map { it.name }
        val named = BundledLicenseText.entries.map { it.assetPath.substringAfterLast('/') }

        assertThat(committed).containsExactlyElementsIn(named)
    }

    @Test
    fun everyBundledTextIsReachableFromTheCatalogue() {
        val opened =
            LicenseCatalog.sections
                .flatMap { it.entries }
                .mapNotNull { it.opens as? Texts }
                .flatMap { target -> target.parts.map { it.text } }
                .toSet()

        assertThat(opened).containsExactlyElementsIn(BundledLicenseText.entries)
    }

    /**
     * Brief §0.2: no exclusion by name. The generated `google/` tree lives under `build/` and is
     * never committed (tech design §13.1 LN-3), so a committed subdirectory here — whatever its
     * name — is that rule broken.
     */
    @Test
    fun theLicencesAssetDirectoryHoldsFilesOnly() {
        assertThat(licencesDirectory().listFiles { file -> file.isDirectory }.orEmpty()).isEmpty()
    }

    /**
     * MPL-2.0 §3.2(a) asks the app to say where the version it ships can be had
     * (`research-analyst-mpl.md` verdict 2): okhttp 4.11.0 compiled the list at this exact commit.
     * The constant changes, with the string, only on the research-analyst's re-derivation (spec §9
     * item 13).
     */
    @Test
    fun thePublicSuffixListAddressNamesTheShippedRevision() {
        val entry = requireNotNull(LicenseCatalog.entry("public-suffix-list"))

        assertThat(entry.lines).contains(Address(R.string.licenses_psl_address))
        assertThat(strings.getValue("licenses_psl_address")).contains(SHIPPED_PSL_REVISION)
    }

    private fun licencesDirectory(): File = File(repoRoot, "$ASSETS_DIR/licenses")

    /** The value `strings.xml` gives the string resource [id] (its name found through `R.string`). */
    private fun stringValue(id: Int): String {
        val field =
            R.string::class.java.fields
                .first { it.getInt(null) == id }
        return strings.getValue(field.name)
    }

    private fun readStringResources(): Map<String, String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File(repoRoot, STRINGS_PATH))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun findRepositoryRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, ROOT_MARKER).isFile) return candidate
            candidate = candidate.parentFile
        }
        error("Could not locate the repository root from ${File(".").absolutePath}.")
    }

    private companion object {
        const val ROOT_MARKER = "settings.gradle.kts"
        const val ASSETS_DIR = "app/src/main/assets"
        const val STRINGS_PATH = "app/src/main/res/values/strings.xml"
        const val SHIPPED_PSL_REVISION = "a312abe0e11421aff8fe909a5e02f36b7c5d28de"

        /**
         * Spec §3.2 "Never listed": THIRD_PARTY.md §1's idea rows, the studio's own work, what does
         * not ship in 3.0.0, and §8's test-, debug-, compile- and build-only items — each matched
         * as a whole word, so "Networking" does not count as naming a king.
         */
        val NEVER_LISTED =
            listOf(
                "Peg Solitaire",
                "English-33",
                "European-37",
                "Asymmetric-39",
                "Diamond-41",
                "Wiegleb-45",
                "peg-solitarie",
                "candy",
                "King",
                "sfx_invalid",
                "sfx_hint",
                "opening book",
                "launcher",
                "font_certs",
                "ui-text-google-fonts",
                "JUnit",
                "Truth",
                "Turbine",
                "MockK",
                "ArchUnit",
                "Robolectric",
                "coroutines-test",
                "room-testing",
                "ui-test",
                "ui-tooling",
                "detekt",
                "ktlint",
                "kover",
            )

        /**
         * The one line, and the one name, the never-listed scan does not hold: D-17's statement that
         * the bundled Fredoka was modified for the app (spec §3.2's Fredoka row; docs/ux/localisation-tr.md
         * §5.4). "Peg Solitaire" there names the app as the modifier, which OFL §1 asks to be stated;
         * the never-listed "Peg Solitaire" is the traditional game of THIRD_PARTY.md §1, which no entry
         * may credit, and every other line is still scanned for it.
         */
        val APP_NAMED_AS_MODIFIER = R.string.licenses_fredoka_modified to "Peg Solitaire"

        /** Spec §3.2's table, row for row, in its order. */
        val EXPECTED =
            listOf(
                LicenseSection(
                    R.string.licenses_section_fonts,
                    listOf(
                        entry(
                            "fredoka",
                            R.string.licenses_fredoka_title,
                            R.string.licenses_ofl_name,
                            listOf(
                                plain(R.string.licenses_fredoka_credit),
                                plain(R.string.licenses_ofl_name),
                                plain(R.string.licenses_fredoka_modified),
                            ),
                            texts(FredokaOfl to R.string.licenses_ofl_name),
                        ),
                        entry(
                            "nunito",
                            R.string.licenses_nunito_title,
                            R.string.licenses_ofl_name,
                            listOf(plain(R.string.licenses_nunito_credit), plain(R.string.licenses_ofl_name)),
                            texts(NunitoOfl to R.string.licenses_ofl_name),
                        ),
                    ),
                ),
                LicenseSection(
                    R.string.licenses_section_sounds,
                    listOf(
                        sound(
                            "kenney-interface-sounds",
                            R.string.licenses_kenney_title,
                            7,
                            R.string.licenses_kenney_address,
                        ),
                        sound(
                            "lilmati-pop-01",
                            R.string.licenses_lilmati_title,
                            2,
                            R.string.licenses_lilmati_address,
                        ),
                        sound(
                            "cogfirestudios-positive-blip",
                            R.string.licenses_cogfire_title,
                            1,
                            R.string.licenses_cogfire_address,
                        ),
                    ),
                ),
                LicenseSection(
                    R.string.licenses_section_software,
                    listOf(
                        apache(
                            "androidx",
                            R.string.licenses_androidx_title,
                            R.string.licenses_androidx_credit,
                            R.string.licenses_androidx_parts,
                        ),
                        apache(
                            "kotlin",
                            R.string.licenses_kotlin_title,
                            R.string.licenses_kotlin_credit,
                            R.string.licenses_kotlin_parts,
                        ),
                        apache(
                            "dagger-hilt",
                            R.string.licenses_dagger_title,
                            R.string.licenses_dagger_credit,
                            parts = null,
                        ),
                        apache(
                            "okhttp-okio",
                            R.string.licenses_okhttp_title,
                            R.string.licenses_okhttp_credit,
                            R.string.licenses_okhttp_parts,
                        ),
                        apache(
                            "gson-tink-guava",
                            R.string.licenses_gson_title,
                            R.string.licenses_gson_credit,
                            R.string.licenses_gson_parts,
                        ),
                        entry(
                            "jakarta-inject",
                            R.string.licenses_jakarta_title,
                            R.string.licenses_apache_name,
                            listOf(plain(R.string.licenses_jakarta_credit), plain(R.string.licenses_apache_name)),
                            texts(
                                JakartaInjectNotice to R.string.license_text_part_notice,
                                Apache2 to R.string.licenses_apache_name,
                            ),
                        ),
                        apache(
                            "annotation-apis",
                            R.string.licenses_annotations_title,
                            R.string.licenses_annotations_credit,
                            R.string.licenses_annotations_parts,
                        ),
                        entry(
                            "protobuf",
                            R.string.licenses_protobuf_title,
                            R.string.licenses_bsd3_name,
                            listOf(
                                plain(R.string.licenses_protobuf_credit),
                                plain(R.string.licenses_bsd3_name),
                                plain(R.string.licenses_protobuf_parts),
                            ),
                            texts(ProtobufBsd3 to R.string.licenses_bsd3_name),
                        ),
                        entry(
                            "chromium-cronet",
                            R.string.licenses_chromium_title,
                            R.string.licenses_bsd3_name,
                            listOf(
                                plain(R.string.licenses_chromium_credit),
                                plain(R.string.licenses_bsd3_name),
                                plain(R.string.licenses_chromium_parts),
                            ),
                            texts(ChromiumBsd3 to R.string.licenses_bsd3_name),
                        ),
                        entry(
                            "public-suffix-list",
                            R.string.licenses_psl_title,
                            R.string.licenses_mpl_name,
                            listOf(
                                plain(R.string.licenses_psl_credit),
                                plain(R.string.licenses_mpl_name),
                                plain(R.string.licenses_psl_parts),
                                Address(R.string.licenses_psl_address),
                            ),
                            texts(
                                PublicSuffixListNotice to R.string.license_text_part_notice,
                                Mpl2 to R.string.licenses_mpl_name,
                            ),
                        ),
                        entry(
                            "checker-qual",
                            R.string.licenses_checker_title,
                            R.string.licenses_mit_name,
                            listOf(plain(R.string.licenses_checker_credit), plain(R.string.licenses_mit_name)),
                            texts(CheckerQualMit to R.string.licenses_mit_name),
                        ),
                    ),
                ),
                LicenseSection(
                    R.string.licenses_section_google,
                    listOf(
                        entry(
                            "google-sdks",
                            R.string.licenses_google_sdk_title,
                            R.string.licenses_android_sdk_name,
                            listOf(
                                plain(R.string.licenses_google_sdk_parts),
                                plain(R.string.licenses_android_sdk_name),
                                Address(R.string.licenses_android_sdk_address),
                            ),
                            opens = null,
                        ),
                        entry(
                            "google-notices",
                            R.string.licenses_google_notices_title,
                            licenseName = null,
                            listOf(plain(R.string.licenses_google_notices_supporting)),
                            GoogleNotices,
                        ),
                    ),
                ),
            )

        fun entry(
            id: String,
            title: Int,
            licenseName: Int?,
            lines: List<LicenseLine>,
            opens: LicenseEntryTarget?,
        ): LicenseEntry = LicenseEntry(id = id, title = title, licenseName = licenseName, lines = lines, opens = opens)

        fun plain(text: Int): LicenseLine = Plain(text)

        fun texts(vararg parts: Pair<BundledLicenseText, Int>): Texts =
            Texts(parts.map { (text, heading) -> BundledTextRef(text, heading) })

        fun sound(
            id: String,
            title: Int,
            count: Int,
            address: Int,
        ): LicenseEntry =
            entry(
                id,
                title,
                R.string.licenses_cc0_name,
                listOf(SoundCount(count), plain(R.string.licenses_cc0_name), Address(address)),
                opens = null,
            )

        fun apache(
            id: String,
            title: Int,
            credit: Int,
            parts: Int?,
        ): LicenseEntry =
            entry(
                id,
                title,
                R.string.licenses_apache_name,
                listOfNotNull(plain(credit), plain(R.string.licenses_apache_name), parts?.let { plain(it) }),
                texts(Apache2 to R.string.licenses_apache_name),
            )
    }
}
