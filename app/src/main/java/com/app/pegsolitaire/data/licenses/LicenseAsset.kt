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
