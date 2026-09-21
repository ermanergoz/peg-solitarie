package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush

/**
 * Band 1 — the hero: the gradient Fredoka title, the static peg-cluster art, and the Settings
 * gear as a clay circular chip at the band's top-end.
 *
 * The title moves here from the deleted `TopAppBar` and grows to `displaySmall`; it keeps the
 * shared `ctaGradientBrush()`, so it is the same object the player sees on every CTA.
 *
 * The gear keeps `Icons.Rounded.Settings` and — deliberately — the **unchanged**
 * `settings_action` content description (D-05b's rule applied to Home): `ButtonSoundTest`
 * locates it by that string, and it stays silent, exactly as it was in the bar.
 *
 * Its 48dp target is `IconButton`'s own minimum rather than a new dimen: the framework
 * guarantees it, the gear has always relied on it here, and a token would be a second,
 * driftable statement of the same number. The clay chip is the `Surface` around it.
 *
 * `home_hero_min_height` is a **minimum**: the band must be allowed to compress toward it —
 * art first, then title scale — before anything below it leaves the fold (D-03f).
 */
@Composable
internal fun HomeHero(onOpenSettings: () -> Unit) {
    Box(
        modifier =
            Modifier
                .clayEntrance(HERO_ENTRANCE_ORDER)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.home_hero_min_height)),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        ) {
            HomeHeroArt(modifier = Modifier.size(dimensionResource(R.dimen.home_hero_art_size)))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.merge(TextStyle(brush = ctaGradientBrush())),
                modifier = Modifier.semantics { heading() },
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .clayShadow(dimensionResource(R.dimen.elevation_pressed), CircleShape),
        ) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_action),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        }
    }
}
