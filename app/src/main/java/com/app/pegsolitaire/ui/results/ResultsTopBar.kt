package com.app.pegsolitaire.ui.results

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
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
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.window.stableSystem

/** The Results top bar: a back nav and the "Results" title. Stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        windowInsets = WindowInsets.stableSystem.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        navigationIcon = {
            IconButton(onClick = onNavigateBack, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}
