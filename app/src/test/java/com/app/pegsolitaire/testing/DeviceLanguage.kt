package com.app.pegsolitaire.testing

import android.content.res.Configuration
import androidx.annotation.StringRes
import org.robolectric.RuntimeEnvironment
import java.util.Locale

/**
 * The Robolectric qualifier of a Turkish phone, region and all, as a device in Türkçe (Türkiye) runs:
 * `@Config(qualifiers = TURKISH_DEVICE)` runs a screen test in Turkish (docs/ux/localisation-tr.md §7.6).
 */
internal const val TURKISH_DEVICE = "tr-rTR"

/**
 * String [id] as an English (en-US) device reads it, whatever language the test itself runs in.
 *
 * A screen test in Turkish finds its nodes by the strings its own configuration resolves, never by
 * typed copy; this is the other half, which shows those strings really are Turkish: a value equal
 * to its English one would mean the screen ran in English and the test proved nothing about Turkish.
 */
internal fun englishString(
    @StringRes id: Int,
    vararg args: Any,
): String {
    val app = RuntimeEnvironment.getApplication()
    val configuration = Configuration(app.resources.configuration).apply { setLocale(Locale.US) }
    return app.createConfigurationContext(configuration).getString(id, *args)
}
