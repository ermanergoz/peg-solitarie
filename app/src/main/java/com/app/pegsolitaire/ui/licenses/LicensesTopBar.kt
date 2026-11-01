package com.app.pegsolitaire.ui.licenses

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * The top bar all four licences screens share (spec §3.1, §4, §4A, §4B): a silent 48dp back with
 * the shared squish, and the [title] as the screen's heading, allowed to wrap (no `maxLines`).
 * Back makes no sound (D-09): it is back-navigation, not a forward CTA. The title reads in its own
 * direction and hangs from the layout's start, beside the back arrow (spec LS-16). It paints no
 * colour, so the backdrop's wash shows through it, as every top bar does (candy-backdrop BD-14, E7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
        title = {
            ProvideLicencesText(LicencesTextEdge.LayoutStart) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
    )
}
