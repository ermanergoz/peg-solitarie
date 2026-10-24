package com.app.pegsolitaire.data.licenses

/**
 * An asset the licences screens may open, and the only kind of path `LicenseTextSource` reads
 * (tech design §3.6, §13.1 LN-4).
 *
 * Sealed so that its two implementations are the whole list: [BundledLicenseText], the curated
 * texts whose paths are written once in that enum, and `GoogleNoticeText`, a generated notice whose
 * path comes from the parsed index. Neither can be built from a route argument, which is the point:
 * a route is restorable, forgeable state, so nothing it carries ever reaches `AssetManager.open`.
 */
sealed interface LicenseAsset {
    /** The path under the APK's `assets/` directory. */
    val assetPath: String
}

/**
 * One generated Google notice text, `licenses/google/<stem>.txt` (tech design §13.1 LN-3, LN-4).
 *
 * Built only from a stem the parsed index gives — `parseGoogleNoticeIndex` validated it, and
 * `GoogleNoticeTextViewModel` builds this from the notice its route's key resolved to — never from
 * a route. The constructor is `internal` and accepts exactly sixteen lowercase hex digits, so not
 * even a malformed stem can turn this into a path outside `licenses/google/`.
 *
 * @property stem the first 16 hex digits of the text's SHA-256, as the index gives it.
 */
@JvmInline
value class GoogleNoticeText internal constructor(
    val stem: String,
) : LicenseAsset {
    init {
        require(GoogleNoticeAssets.isHexId(stem)) { "A notice stem is sixteen lowercase hex digits." }
    }

    override val assetPath: String get() = GoogleNoticeAssets.textPath(stem)
}
