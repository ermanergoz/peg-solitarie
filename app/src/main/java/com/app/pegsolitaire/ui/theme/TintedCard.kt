package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R

/*
 * The clay cards of the Sparse screens (Settings rows, Licences entries, the Classic catalogue): in
 * light theme a translucent violet over the sky, so a card follows the sky's gradient and always
 * reads a shade darker than what is around it. Dark theme keeps the opaque `surfaceContainerHigh`
 * and its clay shadow; its violet is light and would brighten the cards.
 */

/** The opacity of the theme's violet a card lays over the sky in light theme. */
private const val CARD_TINT_ALPHA = 0.10f

/**
 * How far a card's supporting text moves from `onSurfaceVariant` toward `onSurface` in light theme.
 * Where the card is darkest, over the sky's bottom, `onSurfaceVariant` falls below 4.5:1; halfway
 * keeps it at about 6:1.
 */
private const val CARD_SUPPORTING_DARKEN = 0.5f

private val isLightScheme: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

/** A Sparse-screen card's container colour. */
@Composable
internal fun tintedCardColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (isLightScheme) scheme.primary.copy(alpha = CARD_TINT_ALPHA) else scheme.surfaceContainerHigh
}

/** A Sparse-screen card's supporting text and secondary icons: darkened in light theme to stay legible. */
@Composable
internal fun tintedCardSupportingColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (isLightScheme) {
        lerp(scheme.onSurfaceVariant, scheme.onSurface, CARD_SUPPORTING_DARKEN)
    } else {
        scheme.onSurfaceVariant
    }
}

/**
 * A Sparse-screen card's clay shadow, dark theme only. A light-theme card is translucent, and a
 * platform shadow under a translucent shape shows through it; the tint alone separates the card.
 */
@Composable
internal fun Modifier.tintedCardShadow(shape: Shape): Modifier =
    if (isLightScheme) this else clayShadow(dimensionResource(R.dimen.elevation_card), shape)
