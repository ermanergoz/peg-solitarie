package com.app.pegsolitaire.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropPreviewHost
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.FinishedContainer
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.finishGlow
import com.app.pegsolitaire.ui.theme.glossFinish
import com.app.pegsolitaire.ui.theme.titleGradientBrush

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
 * stop looking like two different apps. The logo is withdrawn outright rather than redrawn, and so,
 * since D-03p, is the stage that took its slack: the slack is headroom above this row, with nothing in
 * it but the backdrop's sky, so the row heads one contiguous block of bands.
 *
 * **This is not a top bar.** No surface, no elevation, no window insets of its own; it scrolls
 * with the content, and D-03a's deletion of Home's bar and §5's "no M3 `TopAppBar`" are untouched —
 * what the row shares with the deleted bar is one row's geometry, which is not a component. It is
 * the first thing in the column: nothing is drawn above the title but the column's top padding, the
 * headroom (D-03p) and the backdrop behind everything. The headroom never takes the row's bottom edge
 * past the title bound, the line the backdrop keeps clear of its waves (D-03q).
 *
 * The title takes `titleGradientBrush()` at `displaySmall`, takes the width the gear
 * leaves, and has **no `maxLines`**: it is never truncated and never scaled down, so at a large
 * font scale it grows, the row grows with it and the column scrolls (criteria 14, 15i, 15o).
 *
 * **The row has no minimum height of its own (D-03m).** It is exactly as tall as its tallest
 * child: the 48dp gear chip at the default font scale, the title once it is taller. A floor above
 * that would be an invisible band inside the row, and criterion 15p reads Home's gaps between
 * what is drawn: the retired 72dp floor put 12dp of nothing under the gear, so on the 6.7-inch
 * phone the gap a player saw beneath the row was 28dp, not the 16dp band gap.
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
                .testTag(HOME_HERO_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeroTitle(modifier = Modifier.weight(CONTENT_WEIGHT))
        SettingsGear(onOpenSettings = onOpenSettings)
    }
}

/**
 * The gradient wordmark, at `displaySmall` and announced as the screen's heading, in its own
 * `TitleGradient` (colour-vision.md TG-01), which reads ≥ 4.5:1 on the background in both themes.
 */
@Composable
private fun HeroTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall.merge(TextStyle(brush = titleGradientBrush())),
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
    val chip = MaterialTheme.colorScheme.primaryContainer
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = contentColorFor(chip),
        modifier =
            Modifier
                .finishGlow(dimensionResource(R.dimen.elevation_pressed), CircleShape)
                .glossFinish(FinishedContainer.GearChip, CircleShape, fill = chip),
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

/**
 * The hero row at the top of the narrowest supported window, over Home's backdrop as the host
 * draws it (BD-25): the title's gradient on the calm band's sky, and the gear chip's finish, in
 * both themes.
 */
@Preview(name = "Home hero · light", widthDp = PREVIEW_HERO_WIDTH_DP, heightDp = PREVIEW_HERO_HEIGHT_DP)
@Preview(
    name = "Home hero · dark",
    widthDp = PREVIEW_HERO_WIDTH_DP,
    heightDp = PREVIEW_HERO_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeHeroPreview() {
    AppTheme {
        BackdropPreviewHost(BackdropDensity.Landscape) {
            HomeHero(onOpenSettings = {})
        }
    }
}

/** The narrowest supported window, where the title has the least room beside the gear. */
private const val PREVIEW_HERO_WIDTH_DP = 360
private const val PREVIEW_HERO_HEIGHT_DP = 640
