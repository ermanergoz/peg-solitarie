package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
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

/** Semantics tag of the hero row — a tag the layout tests measure, never announced (§13). */
const val HOME_HERO_TEST_TAG = "home_hero"

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
 *
 * [compact] is the §4.2.2 band set, decided by `HomeBands` from the height available to it and
 * never by this composable: below the threshold the band drops its art, becomes one row, and
 * takes `home_hero_compact_min_height`. The hero is the band that yields because §6.5 already
 * ranks this art as the cheapest thing on Home to lose (step 3 of 4 in its degradation path);
 * what it must not do is lose the **gear**, which is the only route to Settings and, since
 * D-02a, the only permanent route to How to play.
 */
@Composable
internal fun HomeHero(
    compact: Boolean,
    onOpenSettings: () -> Unit,
) {
    if (compact) {
        CompactHero(onOpenSettings = onOpenSettings)
    } else {
        FullHero(onOpenSettings = onOpenSettings)
    }
}

/** The hero with room for its art: the cluster over the title, gear at the top-end corner. */
@Composable
private fun FullHero(onOpenSettings: () -> Unit) {
    Box(
        modifier =
            Modifier
                .clayEntrance(HERO_ENTRANCE_ORDER)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.home_hero_min_height))
                .testTag(HOME_HERO_TEST_TAG),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        ) {
            HomeHeroArt(modifier = Modifier.size(dimensionResource(R.dimen.home_hero_art_size)))
            HeroTitle()
        }
        SettingsGear(
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

/**
 * The hero on a short screen: one row, the title leading and the gear trailing, art not
 * composed at all (§4.2.2).
 *
 * **This is not a top bar.** No surface, no elevation, no window insets of its own; it scrolls
 * with the content and the title keeps display scale. D-03a deleted Home's bar and §5's "no M3
 * `TopAppBar` anywhere" is untouched — what this shares with the deleted bar is one row's
 * geometry, which is not a component.
 *
 * The title is never truncated and never scaled down: there is no `maxLines` here, so a large
 * font scale wraps it, the band grows past its minimum and the column scrolls (criterion 14).
 */
@Composable
private fun CompactHero(onOpenSettings: () -> Unit) {
    Row(
        modifier =
            Modifier
                .clayEntrance(HERO_ENTRANCE_ORDER)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.home_hero_compact_min_height))
                .testTag(HOME_HERO_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeroTitle(modifier = Modifier.weight(CONTENT_WEIGHT))
        SettingsGear(onOpenSettings = onOpenSettings)
    }
}

/** The gradient wordmark, at `displaySmall` and announced as the screen's heading. */
@Composable
private fun HeroTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall.merge(TextStyle(brush = ctaGradientBrush())),
        modifier = modifier.semantics { heading() },
    )
}

/** The clay gear chip — identical in both band sets, because it is the one thing that cannot yield. */
@Composable
private fun SettingsGear(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.clayShadow(dimensionResource(R.dimen.elevation_pressed), CircleShape),
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
