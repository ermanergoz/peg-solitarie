package com.app.pegsolitaire.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R

/**
 * The app's "Candy Clay" Material 3 theme (docs/ui/DESIGN-SYSTEM.md).
 *
 * Wraps [MaterialTheme] with the clay light/dark [LightColors]/[DarkColors]
 * schemes, the Fredoka + Nunito [AppTypography] scale, and the chunky rounded
 * [clayShapes] override (12/16/20/28/32dp, all via dimens). Composables resolve
 * every color through `MaterialTheme.colorScheme.*` roles and every dimension
 * through `dimensionResource(...)`, so no hex or dp/sp literal is ever inlined
 * (QUAL-03).
 *
 * @param darkTheme whether to use the dark scheme; defaults to the system setting.
 * @param content the themed content.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClayColors provides if (darkTheme) DarkClayColors else LightClayColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = clayShapes(),
            content = content,
        )
    }
}

/**
 * The clay [Shapes] scale: extraSmall 12 / small 16 / medium 20 / large 28 /
 * extraLarge 32 (dp via dimens — DESIGN-SYSTEM.md §Shape). Tiles/cards use
 * `large`, buttons `medium`, dialogs `extraLarge`.
 */
@Composable
private fun clayShapes(): Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(dimensionResource(R.dimen.shape_corner_extra_small)),
        small = RoundedCornerShape(dimensionResource(R.dimen.shape_corner_small)),
        medium = RoundedCornerShape(dimensionResource(R.dimen.shape_corner_medium)),
        large = RoundedCornerShape(dimensionResource(R.dimen.shape_corner_large)),
        extraLarge = RoundedCornerShape(dimensionResource(R.dimen.shape_corner_extra_large)),
    )
