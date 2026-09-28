package com.app.pegsolitaire.ui.home

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush

/** Semantics tag of the hero row — a tag the layout tests measure, never announced (§13). */
const val HOME_HERO_TEST_TAG = "home_hero"

/**
 * Band 1 — the hero **row** (ux-spec §4.2.4, D-03l): the gradient Fredoka title start-aligned, the
 * Settings gear as a clay circular chip at the row's end, on every window and in every band set.
 *
 * ## Why one row, and no logo
 *
 * The founder named the peg-cross logo and "how the title sits" in the same breath, and they had
 * one cause. Centring was the logo's composition — a centred emblem wants a centred word under it
 * — and a title centred under art and above a ribbon belongs to neither. Start-aligned in the
 * gear's row it reads as the screen's heading, grounded by the only other element that shares its
 * job; and it is the geometry the 360dp-wide class has shown since D-03i, so the two width classes
 * stop looking like two different apps. The logo is withdrawn outright rather than redrawn: the
 * slack it stood in is now the [HomeStage], which shows the game itself.
 *
 * **This is not a top bar.** No surface, no elevation, no window insets of its own; it scrolls
 * with the content, and D-03a's deletion of Home's bar and §5's "no M3 `TopAppBar`" are untouched —
 * what the row shares with the deleted bar is one row's geometry, which is not a component. It is
 * the first thing in the column: nothing is drawn above the title but the column's top padding and
 * the ambient blobs behind everything.
 *
 * The title keeps the shared `ctaGradientBrush()` at `displaySmall`, takes the width the gear
 * leaves, and has **no `maxLines`**: it is never truncated and never scaled down, so at a large
 * font scale it wraps, the row grows past `home_hero_row_min_height` and the column scrolls
 * (criteria 14, 15i, 15o).
 *
 * The gear keeps `Icons.Rounded.Settings` and — deliberately — the **unchanged** `settings_action`
 * content description (D-05b's rule applied to Home): `ButtonSoundTest` locates it by that string,
 * and it stays silent. It is the only route to Settings and, since D-02a, the only permanent route
 * to How to play, which is why it never yields on any window.
 */
@Composable
internal fun HomeHero(onOpenSettings: () -> Unit) {
    Row(
        modifier =
            Modifier
                .clayEntrance(HERO_ENTRANCE_ORDER)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.home_hero_row_min_height))
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

/**
 * The clay gear chip. Its 48dp target is `IconButton`'s own minimum rather than a new dimen: the
 * framework guarantees it, and a token would be a second, driftable statement of the same number.
 * The clay chip is the `Surface` around it.
 */
@Composable
private fun SettingsGear(onOpenSettings: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clayShadow(dimensionResource(R.dimen.elevation_pressed), CircleShape),
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

/** The hero row on the design baseline's width, on the app background, in both themes. */
@Preview(name = "Home hero · light", widthDp = PREVIEW_HERO_WIDTH_DP)
@Preview(name = "Home hero · dark", widthDp = PREVIEW_HERO_WIDTH_DP, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeHeroPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeHero(onOpenSettings = {})
        }
    }
}

/** The narrowest supported window, where the title has the least room beside the gear. */
private const val PREVIEW_HERO_WIDTH_DP = 360
