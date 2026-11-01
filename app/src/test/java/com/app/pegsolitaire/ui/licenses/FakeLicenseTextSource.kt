package com.app.pegsolitaire.ui.licenses

import com.app.pegsolitaire.data.licenses.LicenseAsset
import com.app.pegsolitaire.data.licenses.LicenseTextSource

/**
 * A [LicenseTextSource] that serves [texts] and records every asset it was asked for, in order — so
 * a test can assert both what a screen showed and that an unknown id never reached a read at all
 * (spec LS-AC14). An asset absent from [texts] reads as `null`, the real source's failure.
 */
internal class FakeLicenseTextSource(
    private val texts: Map<LicenseAsset, String> = emptyMap(),
) : LicenseTextSource {
    private val recorded = mutableListOf<LicenseAsset>()

    /** Every asset read, in call order. */
    val reads: List<LicenseAsset> get() = recorded.toList()

    override suspend fun read(asset: LicenseAsset): String? {
        recorded += asset
        return texts[asset]
    }
}
