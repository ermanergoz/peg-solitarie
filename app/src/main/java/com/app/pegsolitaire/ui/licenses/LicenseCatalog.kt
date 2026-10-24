package com.app.pegsolitaire.ui.licenses

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.BundledLicenseText

/**
 * One bundled text an entry opens, with the heading shown over it when the entry opens more than
 * one (spec LS-10, §4).
 */
@Immutable
data class BundledTextRef(
    val text: BundledLicenseText,
    @param:StringRes val heading: Int,
)

/** What tapping an entry opens; an inert entry has none (spec LS-06). */
sealed interface LicenseEntryTarget {
    /** One or more bundled texts, shown in this order; one part shows no part heading. */
    @Immutable
    data class Texts(
        val parts: List<BundledTextRef>,
    ) : LicenseEntryTarget

    /** The one entry that opens the generated Google notices list (spec §4A). */
    data object GoogleNotices : LicenseEntryTarget
}

/** One line under an entry's title, in display order: credit, licence, then parts or address. */
sealed interface LicenseLine {
    /** A plain line of copy. */
    data class Plain(
        @param:StringRes val text: Int,
    ) : LicenseLine

    /** "N sound effects", through the `licenses_sound_effects` plural. */
    data class SoundCount(
        val count: Int,
    ) : LicenseLine

    /** A source address: selectable so it can be copied, never a link (spec LS-05). */
    data class Address(
        @param:StringRes val text: Int,
    ) : LicenseLine
}

/**
 * One entry of the Licenses list (spec §3.2).
 *
 * @property id the route's handle on this entry; stable, never shown, never a path.
 * @property title the entry's name.
 * @property licenseName the License text screen's top-bar title; `null` only for the notices entry.
 * @property lines the lines under the title, in display order.
 * @property opens what a tap opens; `null` makes the entry inert.
 */
@Immutable
data class LicenseEntry(
    val id: String,
    @param:StringRes val title: Int,
    @param:StringRes val licenseName: Int?,
    val lines: List<LicenseLine>,
    val opens: LicenseEntryTarget?,
)

/** One headed section of the Licenses list. */
@Immutable
data class LicenseSection(
    @param:StringRes val title: Int,
    val entries: List<LicenseEntry>,
)

/** The stable ids routes carry for the Licenses entries (tech design §3.6: an id, never a path). */
internal object LicenseEntryIds {
    const val FREDOKA = "fredoka"
    const val NUNITO = "nunito"
    const val KENNEY = "kenney-interface-sounds"
    const val LILMATI = "lilmati-pop-01"
    const val COGFIRE = "cogfirestudios-positive-blip"
    const val ANDROIDX = "androidx"
    const val KOTLIN = "kotlin"
    const val DAGGER_HILT = "dagger-hilt"
    const val OKHTTP_OKIO = "okhttp-okio"
    const val GSON_TINK_GUAVA = "gson-tink-guava"
    const val JAKARTA_INJECT = "jakarta-inject"
    const val ANNOTATION_APIS = "annotation-apis"
    const val PROTOBUF = "protobuf"
    const val CHROMIUM_CRONET = "chromium-cronet"
    const val PUBLIC_SUFFIX_LIST = "public-suffix-list"
    const val CHECKER_QUAL = "checker-qual"
    const val GOOGLE_SDKS = "google-sdks"
    const val GOOGLE_NOTICES = "google-notices"
}

/**
 * Everything the Licenses screen lists, in the spec's order (spec §3.2; tech design §3.6): a
 * hand-curated, static catalogue that mirrors THIRD_PARTY.md's shipped rows (LS-03), so it is code
 * rather than a generated list or a JSON asset. `LicenseCatalogTest` pins it to the spec's §3.2.1
 * mapping, which the release-manager diffs against THIRD_PARTY.md at the cut.
 *
 * The Google notices are not entries here: one entry opens the list the build generates from the
 * Google AARs (§4A), so this catalogue does not change as Google's libraries do.
 */
internal object LicenseCatalog {
    val sections: List<LicenseSection> =
        listOf(
            LicenseSection(R.string.licenses_section_fonts, listOf(FREDOKA, NUNITO)),
            LicenseSection(R.string.licenses_section_sounds, listOf(KENNEY, LILMATI, COGFIRE)),
            LicenseSection(
                R.string.licenses_section_software,
                listOf(
                    ANDROIDX,
                    KOTLIN,
                    DAGGER_HILT,
                    OKHTTP_OKIO,
                    GSON_TINK_GUAVA,
                    JAKARTA_INJECT,
                    ANNOTATION_APIS,
                    PROTOBUF,
                    CHROMIUM_CRONET,
                    PUBLIC_SUFFIX_LIST,
                    CHECKER_QUAL,
                ),
            ),
            LicenseSection(R.string.licenses_section_google, listOf(GOOGLE_SDKS, GOOGLE_NOTICES)),
        )

    private val entriesById: Map<String, LicenseEntry> =
        sections.flatMap { it.entries }.associateBy { it.id }

    /** The entry a route's [id] names, or `null` for an id the catalogue does not know. */
    fun entry(id: String): LicenseEntry? = entriesById[id]
}

/*
 * The entries, one per THIRD_PARTY.md row or family (spec §3.2.1). The sound counts are
 * THIRD_PARTY.md §2's: a re-cut of the sound set is a change to these three numbers.
 */

private const val KENNEY_SOUND_COUNT = 7
private const val LILMATI_SOUND_COUNT = 2
private const val COGFIRE_SOUND_COUNT = 1

private val OFL_TEXT_LINE = LicenseLine.Plain(R.string.licenses_ofl_name)
private val CC0_LINE = LicenseLine.Plain(R.string.licenses_cc0_name)
private val APACHE_LINE = LicenseLine.Plain(R.string.licenses_apache_name)
private val BSD3_LINE = LicenseLine.Plain(R.string.licenses_bsd3_name)

private val APACHE_TEXT =
    LicenseEntryTarget.Texts(listOf(BundledTextRef(BundledLicenseText.Apache2, R.string.licenses_apache_name)))

private val FREDOKA =
    LicenseEntry(
        id = LicenseEntryIds.FREDOKA,
        title = R.string.licenses_fredoka_title,
        licenseName = R.string.licenses_ofl_name,
        lines = listOf(LicenseLine.Plain(R.string.licenses_fredoka_credit), OFL_TEXT_LINE),
        opens = singleText(BundledLicenseText.FredokaOfl, R.string.licenses_ofl_name),
    )

private val NUNITO =
    LicenseEntry(
        id = LicenseEntryIds.NUNITO,
        title = R.string.licenses_nunito_title,
        licenseName = R.string.licenses_ofl_name,
        lines = listOf(LicenseLine.Plain(R.string.licenses_nunito_credit), OFL_TEXT_LINE),
        opens = singleText(BundledLicenseText.NunitoOfl, R.string.licenses_ofl_name),
    )

private val KENNEY =
    soundEntry(
        LicenseEntryIds.KENNEY,
        R.string.licenses_kenney_title,
        KENNEY_SOUND_COUNT,
        R.string.licenses_kenney_address,
    )

private val LILMATI =
    soundEntry(
        LicenseEntryIds.LILMATI,
        R.string.licenses_lilmati_title,
        LILMATI_SOUND_COUNT,
        R.string.licenses_lilmati_address,
    )

private val COGFIRE =
    soundEntry(
        LicenseEntryIds.COGFIRE,
        R.string.licenses_cogfire_title,
        COGFIRE_SOUND_COUNT,
        R.string.licenses_cogfire_address,
    )

private val ANDROIDX =
    apacheEntry(
        LicenseEntryIds.ANDROIDX,
        R.string.licenses_androidx_title,
        R.string.licenses_androidx_credit,
        R.string.licenses_androidx_parts,
    )

private val KOTLIN =
    apacheEntry(
        LicenseEntryIds.KOTLIN,
        R.string.licenses_kotlin_title,
        R.string.licenses_kotlin_credit,
        R.string.licenses_kotlin_parts,
    )

private val DAGGER_HILT =
    apacheEntry(
        LicenseEntryIds.DAGGER_HILT,
        R.string.licenses_dagger_title,
        R.string.licenses_dagger_credit,
        parts = null,
    )

private val OKHTTP_OKIO =
    apacheEntry(
        LicenseEntryIds.OKHTTP_OKIO,
        R.string.licenses_okhttp_title,
        R.string.licenses_okhttp_credit,
        R.string.licenses_okhttp_parts,
    )

private val GSON_TINK_GUAVA =
    apacheEntry(
        LicenseEntryIds.GSON_TINK_GUAVA,
        R.string.licenses_gson_title,
        R.string.licenses_gson_credit,
        R.string.licenses_gson_parts,
    )

/** Its NOTICE, then the Apache text (spec LS-10): Apache-2.0 §4(d) asks the NOTICE to travel along. */
private val JAKARTA_INJECT =
    LicenseEntry(
        id = LicenseEntryIds.JAKARTA_INJECT,
        title = R.string.licenses_jakarta_title,
        licenseName = R.string.licenses_apache_name,
        lines = listOf(LicenseLine.Plain(R.string.licenses_jakarta_credit), APACHE_LINE),
        opens =
            LicenseEntryTarget.Texts(
                listOf(
                    BundledTextRef(BundledLicenseText.JakartaInjectNotice, R.string.license_text_part_notice),
                    BundledTextRef(BundledLicenseText.Apache2, R.string.licenses_apache_name),
                ),
            ),
    )

private val ANNOTATION_APIS =
    apacheEntry(
        LicenseEntryIds.ANNOTATION_APIS,
        R.string.licenses_annotations_title,
        R.string.licenses_annotations_credit,
        R.string.licenses_annotations_parts,
    )

private val PROTOBUF =
    LicenseEntry(
        id = LicenseEntryIds.PROTOBUF,
        title = R.string.licenses_protobuf_title,
        licenseName = R.string.licenses_bsd3_name,
        lines =
            listOf(
                LicenseLine.Plain(R.string.licenses_protobuf_credit),
                BSD3_LINE,
                LicenseLine.Plain(R.string.licenses_protobuf_parts),
            ),
        opens = singleText(BundledLicenseText.ProtobufBsd3, R.string.licenses_bsd3_name),
    )

private val CHROMIUM_CRONET =
    LicenseEntry(
        id = LicenseEntryIds.CHROMIUM_CRONET,
        title = R.string.licenses_chromium_title,
        licenseName = R.string.licenses_bsd3_name,
        lines =
            listOf(
                LicenseLine.Plain(R.string.licenses_chromium_credit),
                BSD3_LINE,
                LicenseLine.Plain(R.string.licenses_chromium_parts),
            ),
        opens = singleText(BundledLicenseText.ChromiumBsd3, R.string.licenses_bsd3_name),
    )

/**
 * OkHttp's NOTICE, then the MPL-2.0 text (spec LS-10, LS-11); the address names the exact revision
 * of the list okhttp 4.11.0 compiled in, which MPL-2.0 §3.2(a) asks the app to tell recipients.
 */
private val PUBLIC_SUFFIX_LIST =
    LicenseEntry(
        id = LicenseEntryIds.PUBLIC_SUFFIX_LIST,
        title = R.string.licenses_psl_title,
        licenseName = R.string.licenses_mpl_name,
        lines =
            listOf(
                LicenseLine.Plain(R.string.licenses_psl_credit),
                LicenseLine.Plain(R.string.licenses_mpl_name),
                LicenseLine.Plain(R.string.licenses_psl_parts),
                LicenseLine.Address(R.string.licenses_psl_address),
            ),
        opens =
            LicenseEntryTarget.Texts(
                listOf(
                    BundledTextRef(BundledLicenseText.PublicSuffixListNotice, R.string.license_text_part_notice),
                    BundledTextRef(BundledLicenseText.Mpl2, R.string.licenses_mpl_name),
                ),
            ),
    )

/** Credited although R8 keeps none of it: a 1 KB credit replaces a per-release mapping check (LN-5). */
private val CHECKER_QUAL =
    LicenseEntry(
        id = LicenseEntryIds.CHECKER_QUAL,
        title = R.string.licenses_checker_title,
        licenseName = R.string.licenses_mit_name,
        lines =
            listOf(
                LicenseLine.Plain(R.string.licenses_checker_credit),
                LicenseLine.Plain(R.string.licenses_mit_name),
            ),
        opens = singleText(BundledLicenseText.CheckerQualMit, R.string.licenses_mit_name),
    )

/** Inert: the SDK licence is not bundled, so its address is shown as selectable text (LS-05). */
private val GOOGLE_SDKS =
    LicenseEntry(
        id = LicenseEntryIds.GOOGLE_SDKS,
        title = R.string.licenses_google_sdk_title,
        licenseName = R.string.licenses_android_sdk_name,
        lines =
            listOf(
                LicenseLine.Plain(R.string.licenses_google_sdk_parts),
                LicenseLine.Plain(R.string.licenses_android_sdk_name),
                LicenseLine.Address(R.string.licenses_android_sdk_address),
            ),
        opens = null,
    )

private val GOOGLE_NOTICES =
    LicenseEntry(
        id = LicenseEntryIds.GOOGLE_NOTICES,
        title = R.string.licenses_google_notices_title,
        licenseName = null,
        lines = listOf(LicenseLine.Plain(R.string.licenses_google_notices_supporting)),
        opens = LicenseEntryTarget.GoogleNotices,
    )

private fun singleText(
    text: BundledLicenseText,
    @StringRes heading: Int,
): LicenseEntryTarget = LicenseEntryTarget.Texts(listOf(BundledTextRef(text, heading)))

/** A sound pack: CC0, nothing bundled to open, so inert, with its source address (LS-05). */
private fun soundEntry(
    id: String,
    @StringRes title: Int,
    count: Int,
    @StringRes address: Int,
): LicenseEntry =
    LicenseEntry(
        id = id,
        title = title,
        licenseName = R.string.licenses_cc0_name,
        lines = listOf(LicenseLine.SoundCount(count), CC0_LINE, LicenseLine.Address(address)),
        opens = null,
    )

/** An Apache-2.0 family: credit, licence, and its parts line when it has one; opens the Apache text. */
private fun apacheEntry(
    id: String,
    @StringRes title: Int,
    @StringRes credit: Int,
    @StringRes parts: Int?,
): LicenseEntry =
    LicenseEntry(
        id = id,
        title = title,
        licenseName = R.string.licenses_apache_name,
        lines = listOfNotNull(LicenseLine.Plain(credit), APACHE_LINE, parts?.let { LicenseLine.Plain(it) }),
        opens = APACHE_TEXT,
    )
