package com.app.pegsolitaire.data.licenses

/**
 * The nine curated licence texts the app bundles under `assets/licenses/` (spec §7; tech design
 * §3.6, §13.1 LN-5) — and the **only** place a curated asset path is written.
 *
 * Each file is committed byte-identical to its source and pinned by its full SHA-256 in
 * `BundledLicenseTextHashTest`; `LicenseCatalogTest` proves every constant names an existing file,
 * every committed file is named by a constant, and every constant is opened by an entry. The file
 * names follow the spec's `<Name>-<Licence>.txt` pattern, except Jakarta's NOTICE, which keeps the
 * `.md` of the jar it comes from.
 *
 * In `data/` rather than beside the catalogue in `ui/` (brief §0.4, T1): the data layer reads
 * these paths, and `data` importing a `ui` type would invert the layering (tech design §2.3).
 */
enum class BundledLicenseText(
    override val assetPath: String,
) : LicenseAsset {
    /** Fredoka's SIL Open Font License 1.1 notice, as shipped with the font. */
    FredokaOfl("licenses/Fredoka-OFL.txt"),

    /** Nunito's SIL Open Font License 1.1 notice, as shipped with the font. */
    NunitoOfl("licenses/Nunito-OFL.txt"),

    /** The Apache License 2.0, from apache.org; opened by every Apache-2.0 entry. */
    Apache2("licenses/Apache-2.0.txt"),

    /** Jakarta Dependency Injection's `NOTICE.md`, from `jakarta.inject-api` 2.0.1's jar. */
    JakartaInjectNotice("licenses/Jakarta-Inject-NOTICE.md"),

    /** Protocol Buffers' BSD 3-Clause `LICENSE` at v22.3. */
    ProtobufBsd3("licenses/Protobuf-BSD-3-Clause.txt"),

    /** Chromium's BSD 3-Clause `LICENSE` at 143.0.7445.0 (the Cronet classes). */
    ChromiumBsd3("licenses/Chromium-BSD-3-Clause.txt"),

    /** Checker Framework qualifiers' MIT `LICENSE.txt` at 3.33.0. */
    CheckerQualMit("licenses/CheckerQual-MIT.txt"),

    /** OkHttp 4.11.0's NOTICE for the Public Suffix List data it compiles in. */
    PublicSuffixListNotice("licenses/PublicSuffixList-NOTICE.txt"),

    /** The Mozilla Public License 2.0, from mozilla.org (spec LS-11). */
    Mpl2("licenses/MPL-2.0.txt"),
}
