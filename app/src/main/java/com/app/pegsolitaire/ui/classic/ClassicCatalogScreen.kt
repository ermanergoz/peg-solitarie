package com.app.pegsolitaire.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.tintedCardColor
import com.app.pegsolitaire.ui.theme.tintedCardShadow
import com.app.pegsolitaire.ui.theme.tintedCardSupportingColor
import com.app.pegsolitaire.ui.window.stableSystem

/**
 * The stateless Classic catalog screen (CLAS-01, D-11/D-18).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (title "Classic", a back nav) over a
 * [LazyVerticalGrid] of board cards — one column on phones, two-plus on wider
 * screens via `GridCells.Adaptive(catalog_grid_min_cell_width)`. The cards appear in
 * the repository's D-18 ascending hole-count order. Each card shows a read-only
 * [BoardPreview] of the footprint plus the board name and a plural-aware hole count;
 * tapping a whole card emits its `levelId` via [onSelect] for `Gameplay(levelId)`.
 *
 * @param state the immutable catalog UI state (the D-18-ordered descriptors).
 * @param onSelect forwards a tapped board's `levelId` up to the NavHost.
 * @param onNavigateBack leaves the catalog (top-bar back nav → Home).
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicCatalogScreen(
    state: ClassicCatalogUiState,
    onSelect: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { CatalogTopBar(onNavigateBack = onNavigateBack) },
        contentWindowInsets = WindowInsets.stableSystem,
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(dimensionResource(R.dimen.catalog_grid_min_cell_width)),
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width)),
                contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_md)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                itemsIndexed(items = state.boards, key = { _, board -> board.descriptor.id }) { index, board ->
                    BoardCard(board = board, entranceOrder = index, onSelect = onSelect)
                }
            }
        }
    }
}

/** The catalog top bar: a back nav (→ Home) and the "Classic" title. Stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogTopBar(onNavigateBack: () -> Unit) {
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
                text = stringResource(R.string.catalog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

/**
 * One catalog clay board card (D-11): a soft-shadowed, squishy `surfaceContainerHigh`
 * `Card` whose whole surface is the tap target → [onSelect] with the descriptor's
 * `levelId`. It stacks the read-only [BoardPreview] with its earned-star pips (D-10),
 * the board name (`titleMedium`, `onSurface`) and a plural-aware "{n} holes" subtitle
 * (`bodyMedium`, `onSurfaceVariant`). Cards enter with the shared 40ms clay stagger
 * ([entranceOrder]).
 *
 * Every card is given the same fixed `catalog_card_height` so cards in the same grid
 * row line up even when a name wraps to two lines; the name allows up to 2 lines
 * (ellipsized past that) so a long name like "Asymmetric 3-3-2-2" wraps rather than
 * being truncated to one line.
 */
@Composable
private fun BoardCard(
    board: ClassicBoardUiState,
    entranceOrder: Int,
    onSelect: (String) -> Unit,
) {
    val descriptor: ClassicLevelDescriptor = board.descriptor
    // SOUND-04 / D-09 / D-11: picking a board is a navigational CTA, so the card clicks — while
    // CatalogTopBar's back icon above stays silent. The cue is attached per callsite and NEVER to
    // Modifier.claySquish(), which both controls share (exclusion list in the withClickSound KDoc).
    Card(
        onClick = withClickSound(SoundCue.ButtonForward) { onSelect(descriptor.id) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .claySquish()
                .tintedCardShadow(MaterialTheme.shapes.large)
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.catalog_card_height)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        ) {
            BoardPreview(board = descriptor.previewBoard, earnedStars = board.earnedStars)
            Text(
                text = descriptor.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = MAX_NAME_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.holes_count,
                        descriptor.holeCount,
                        descriptor.holeCount,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = tintedCardSupportingColor(),
            )
        }
    }
}

/** Board names may wrap to at most two lines (a long name like "Asymmetric 3-3-2-2" wraps, never truncates to one). */
private const val MAX_NAME_LINES = 2
