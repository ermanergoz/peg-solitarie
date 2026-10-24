package com.app.pegsolitaire.data.licenses

/**
 * Reads one licence text the app bundles or generates (tech design §3.6, §13.1 LN-4).
 *
 * Main-safe: an implementation moves the read off the caller's thread. It takes a [LicenseAsset]
 * and nothing else, so only the curated enum's paths and the parsed index's stems can ever be
 * opened — never a string from a route.
 */
interface LicenseTextSource {
    /** The whole text decoded as UTF-8, or `null` when the asset cannot be read. */
    suspend fun read(asset: LicenseAsset): String?
}
