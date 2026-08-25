package com.app.pegsolitaire.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app's Material 3 theme.
 *
 * Wraps [MaterialTheme] with the baseline light/dark [LightColors]/[DarkColors]
 * schemes and the default [AppTypography] scale (M3 shapes are the stock
 * defaults). Composables resolve every color through `MaterialTheme.colorScheme.*`
 * roles and every dimension through `dimensionResource(...)`, so no hex or dp/sp
 * literal is ever inlined (QUAL-03). Real theming/polish is Slice 2 (POLI-01).
 *
 * @param darkTheme whether to use the dark scheme; defaults to the system setting.
 * @param content the themed content.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
