package com.app.pegsolitaire.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.scoring.ChallengeBand
import com.app.pegsolitaire.ui.backdrop.backdropInputObserver
import com.app.pegsolitaire.ui.components.AnnouncementRegion
import com.app.pegsolitaire.ui.components.BoardAnnouncer
import com.app.pegsolitaire.ui.components.StarGlyph
import com.app.pegsolitaire.ui.components.rememberBoardAnnouncer
import com.app.pegsolitaire.ui.components.rememberBoardSpeechFormat
import com.app.pegsolitaire.ui.game.GameTimerFormat
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.withTabularFigures
import com.app.pegsolitaire.ui.window.stableSystem

/** Test tag of the star row; carries the [EarnedStarCountKey] semantics for the screen test. */
const val RESULTS_STAR_ROW_TEST_TAG = "results_star_row"

/** Test tag of the "New best!" cue, present only when this play improved a stored best (D-09). */
const val RESULTS_NEW_BEST_TEST_TAG = "results_new_best"

/** Test tag of the next-star line, present only on a Challenge clear below three stars (D-SB-2). */
const val RESULTS_NEXT_STAR_HINT_TEST_TAG = "results_next_star_hint"

/** Test tag of the INT-04 quiescence dim, present only while an interstitial is coming. */
const val RESULTS_AD_SCRIM_TEST_TAG = "results_ad_scrim"

/**
 * Semantics: how many of the three stars are filled (earned). The individual star
 * icons are decorative (`contentDescription = null`) — the headline + metrics convey
 * the outcome to screen readers — so this row-level count is what the test asserts.
 */
val EarnedStarCountKey = SemanticsPropertyKey<Int>("EarnedStarCount")

private var SemanticsPropertyReceiver.earnedStarCountValue by EarnedStarCountKey

/**
 * The stateless end-of-game Results screen (SCOR-03, D-05), in the Candy-Clay
 * celebration skin (DESIGN-SYSTEM.md Results).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (a back nav, the "Results" title) over a centered
 * content column capped at `content_max_width`, stacking: the star-tiered headline
 * (D-08, scaling in), the 3-star row (the first [ResultsUiState.stars] filled with the
 * GoldGradient + a soft glow, popping in sequentially with a 120ms stagger; the rest
 * hollow, in `onSurfaceVariant`: colour-vision §3.4, CV-13a), on a Challenge clear below three
 * stars the next-star line naming the next band up (D-SB-2), the "New best!" pill
 * bouncing in when this play improved a stored best ([ResultsUiState.newBests],
 * D-09), the SCORE / TIME / PEGS LEFT metric
 * block (tabular figures, the score counting up over ~600ms), and the actions — a
 * single CtaGradient primary action (wrapping `replayId`) plus quiet clay chips.
 * Classic has no "next board", so that Challenge-mode action is intentionally absent.
 * All celebration motion is skipped when OS animations are off (values land final
 * immediately).
 *
 * Every value is read straight from [state]; mm:ss is formatted from
 * [ResultsUiState.elapsedSeconds] via [GameTimerFormat] — no scoring is recomputed here.
 *
 * Two decorative layers sit behind all of it (ux-spec D-RC, §4.7), siblings composed before the
 * [Scaffold] so hit testing reaches the column first: the gold rays from the star row's centre on
 * every result ([ResultsRaysLayer], on the screen's own clock, which the root observes input for),
 * and at three stars, once per result, the seeded confetti fall ([ResultsConfettiLayer]).
 *
 * One overlay sits above all of it: while [ResultsUiState.adScrimVisible] is set, an
 * [AdQuiescenceScrim] dims the whole screen and swallows every pointer and back event, so
 * the instant between an ad-eligible continue tap and the interstitial rendering cannot
 * produce an accidental click on the ad (INT-04).
 *
 * @param state the immutable Results UI state to render.
 * @param onReplay re-plays the same level (carries `replayId`). Classic only.
 * @param onCatalog pops back to the Classic catalog. Classic only.
 * @param onHome pops back to Home.
 * @param onNextLevel reports the solved Challenge continue tap (D-01). It carries no level:
 *   the next one is `max(ladder head, N + 1)` and is resolved by `ResultsViewModel` (D-04a).
 * @param onRetry reloads the same deterministic Challenge board when stuck (carries N, D-10).
 * @param onNavigateBack leaves the Results screen (top-bar back nav).
 * @param modifier layout modifier for the screen.
 * @param showRays draws the rays layer; on in the product. A test seam only: a pixel test compares a
 *   frame against the same frame with the layer absent (ux-spec §13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: () -> Unit,
    onRetry: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    showRays: Boolean = true,
) {
    // D-TB (TB-8): the summary is spoken once, on the showing the D-16 latch marks (D-TBa item 7).
    val announcer = rememberBoardAnnouncer()
    ResultsSummaryAnnouncement(state = state, announcer = announcer)
    val raysClock = rememberResultsRaysClock()
    val raysAnchor = remember { RaysAnchor() }
    Box(modifier = modifier.fillMaxSize().backdropInputObserver(raysClock)) {
        if (showRays) {
            ResultsRaysLayer(clock = raysClock, anchor = raysAnchor)
        }
        ResultsConfettiLayer(state = state)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { ResultsTopBar(onNavigateBack = onNavigateBack) },
            contentWindowInsets = WindowInsets.stableSystem,
        ) { contentPadding ->
            ResultsColumn(
                state = state,
                contentPadding = contentPadding,
                onStarRowCentre = { raysAnchor.rowCentreInRoot = it },
                onReplay = onReplay,
                onCatalog = onCatalog,
                onHome = onHome,
                onNextLevel = onNextLevel,
                onRetry = onRetry,
            )
        }

        // The scrim is a SIBLING of the Scaffold, not a child of its content lambda. Inside
        // the content it would be laid out within the content padding, leaving the TopAppBar's
        // back arrow live above it — a working exit from a screen INT-04 requires to be inert.
        if (state.adScrimVisible) {
            AdQuiescenceScrim()
        }
        // D-TB: the screen's spoken channel, drawn last, above everything (defect ruling §4).
        AnnouncementRegion(announcer)
    }
}

/**
 * The scrolling content column, centred and capped at `content_max_width`: the headline, the star
 * row (which reports its centre to [onStarRowCentre] for the rays), the next-star line, "New best!",
 * the metrics and the actions. Stateless.
 */
@Composable
private fun ResultsColumn(
    state: ResultsUiState,
    contentPadding: PaddingValues,
    onStarRowCentre: (Offset) -> Unit,
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: () -> Unit,
    onRetry: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_md)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_lg)),
        ) {
            Headline(tier = state.headlineTier)
            StarRow(earned = state.stars, playCelebration = state.playCelebration, onCentre = onStarRowCentre)
            state.nextStarBand?.let { band -> NextStarLine(band = band) }
            if (state.newBests.any) {
                NewBestBadge()
            }
            MetricBlock(state = state)
            ResultsActions(
                state = state,
                onReplay = onReplay,
                onCatalog = onCatalog,
                onHome = onHome,
                onNextLevel = onNextLevel,
                onRetry = onRetry,
            )
        }
    }
}

/** The tiered headline (D-08), scaling in with an overshoot pop. Text is unchanged. */
@Composable
private fun Headline(tier: ResultsHeadlineTier) {
    val scale = rememberPopIn(delayMillis = NO_DELAY)
    Text(
        text = stringResource(headlineResFor(tier)),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            },
    )
}

/**
 * The three-star row (D-01): the first [earned] stars are filled [StarGlyph]s over a soft gold
 * glow, popping in sequentially (spring overshoot, [ClayAnim.STAR_STAGGER_MS] apart); the
 * remaining stars are unearned [StarGlyph]s. Both draw in the active palette
 * (docs/ux/colour-vision.md §3.4). Each icon is decorative; the row
 * exposes [EarnedStarCountKey] so the test can assert the filled count without
 * per-icon semantics.
 *
 * The row is also where the celebration gets its voice: [StarPopSounds] fires one rising
 * pop per earned star on the same stagger (SOUND-03), gated by [playCelebration] so it
 * sounds once per win (D-16). That latch is hoisted from [ResultsUiState] and never
 * remembered here — a `remember` survives neither the configuration change nor the
 * process-death restore the latch exists to cover, so this row stays stateless.
 *
 * @param earned how many of the three stars this result filled.
 * @param playCelebration whether THIS entry into Results should sound the pops (D-16).
 * @param onCentre receives the row's centre in root coordinates on every layout, for the rays (D-RC-1).
 */
@Composable
private fun StarRow(
    earned: Int,
    playCelebration: Boolean,
    onCentre: (Offset) -> Unit,
) {
    StarPopSounds(earned = earned, play = playCelebration)
    // D-TB (TB-8): one stop, "2 of 3 stars"; the stars inside stay decorative.
    val starsSpoken = stringResource(R.string.catalog_stars_earned, earned, CelebrationFx.TOTAL_STARS)
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .testTag(RESULTS_STAR_ROW_TEST_TAG)
                .reportCentreInRoot(onCentre)
                .semantics(mergeDescendants = true) {
                    contentDescription = starsSpoken
                    earnedStarCountValue = earned
                },
    ) {
        repeat(CelebrationFx.TOTAL_STARS) { index ->
            val isEarned = index < earned
            if (isEarned) {
                EarnedStar(order = index)
            } else {
                StarGlyph(earned = false, size = dimensionResource(R.dimen.results_star_size))
            }
        }
    }
}

/**
 * One earned star: a soft gold glow behind a GoldGradient star, popping in at [order].
 *
 * The pop runs for [ClayAnim.STAR_POP_MS] rather than the shared default, so the star is
 * still resolving well into the 1.65 s cue that fires with it; see that token's KDoc for why
 * it deliberately exceeds [ClayAnim.STAR_STAGGER_MS] and lets consecutive stars overlap.
 */
@Composable
private fun EarnedStar(order: Int) {
    val scale =
        rememberPopIn(
            delayMillis = order * ClayAnim.STAR_STAGGER_MS,
            durationMillis = ClayAnim.STAR_POP_MS,
        )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            },
    ) {
        Box(
            modifier =
                Modifier
                    .size(dimensionResource(R.dimen.results_star_glow_size))
                    .alpha(STAR_GLOW_ALPHA)
                    .background(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape),
        )
        StarGlyph(earned = true, size = dimensionResource(R.dimen.results_star_size))
    }
}

/**
 * The next-star line (ux-spec D-SB-2, §4.6; docs/gdd.md GD-SB-08/20): under the star row of a
 * Challenge clear below three stars, the peg count the next star needs — "Leave 2 pegs for 2
 * stars." at one star, "Leave 1 peg for 3 stars." at two. Both numbers are [band]'s, which
 * [ResultsUiState.nextStarBand] takes from the clear rule; this leaf only words them.
 *
 * `bodyLarge` in `onSurface` (D-RC-2: every uncontained text on Results reads ≥ 4.5:1 under the
 * rays), centred and wrapping with no line cap. It fades in on the
 * "New best!" pill's timing, after the stars have landed (prose fades, it does not pop), and is
 * present from the first frame with animations off. Its own `Text` node — no live region, no role,
 * not merged into the star row — so TalkBack reaches it as the stop after the stars.
 */
@Composable
private fun NextStarLine(band: ChallengeBand) {
    val alpha = rememberFadeIn(delayMillis = CelebrationFx.TOTAL_STARS * ClayAnim.STAR_STAGGER_MS)
    Text(
        text =
            pluralStringResource(
                R.plurals.results_next_star_hint,
                band.pegsRemaining,
                band.pegsRemaining,
                band.stars,
            ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(RESULTS_NEXT_STAR_HINT_TEST_TAG)
                .graphicsLayer { this.alpha = alpha() },
    )
}

/**
 * The "New best!" cue (D-09): a gold `tertiaryContainer` clay pill that bounces in,
 * shown only when [ResultsUiState.newBests] reports that this play improved at least
 * one of the level's stored bests. It is deliberately NOT the CtaGradient accent —
 * the single accent on the screen stays the one primary action (UI-SPEC S3) — and it
 * is a pure render of state, so recomposition can never re-fire it.
 */
@Composable
private fun NewBestBadge() {
    val scale = rememberPopIn(delayMillis = CelebrationFx.TOTAL_STARS * ClayAnim.STAR_STAGGER_MS)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier =
            Modifier
                .testTag(RESULTS_NEW_BEST_TEST_TAG)
                .graphicsLayer {
                    scaleX = scale()
                    scaleY = scale()
                },
    ) {
        Text(
            text = stringResource(R.string.results_new_best),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_sm),
                    vertical = dimensionResource(R.dimen.spacing_xs),
                ),
        )
    }
}

/**
 * The SCORE / TIME / PEGS LEFT metric block: three labelled values read straight from
 * [state]. The score counts up over ~600ms (final value immediately when animations are
 * off); the time is the `elapsedSeconds` rendered `mm:ss` via [GameTimerFormat] (no
 * scoring recomputed here). All three use tabular figures so the row never jitters.
 */
@Composable
private fun MetricBlock(state: ResultsUiState) {
    val score = rememberCountUp(target = state.score)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric(value = score().toString(), label = stringResource(R.string.results_label_score))
        Metric(
            value = GameTimerFormat.format(state.elapsedSeconds),
            label = stringResource(R.string.results_label_time),
        )
        Metric(
            value = state.pegsRemaining.toString(),
            label = stringResource(R.string.results_label_pegs_left),
        )
    }
}

/**
 * One labelled metric: the [value] (`titleLarge` + tabular figures, `onSurface`) over
 * its [label] caption (`labelLarge`, `onSurface`: D-RC-2, the size and weight steps carry the hierarchy).
 */
@Composable
private fun Metric(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.withTabularFigures(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Results' summary (D-TB, TB-8): "Solved! — 2 of 3 stars, 2 pegs left.", put on the screen's polite
 * channel once per result (D-TBa item 7). [ResultsUiState.playCelebration] marks the result's first
 * showing, but the ViewModel and its latch survive a rotation and a return onto the same entry, so
 * the screen also keeps its own saved "spoken" flag: a recreated screen, or one restored after process
 * death, speaks nothing (loop 1, F-1).
 */
@Composable
private fun ResultsSummaryAnnouncement(
    state: ResultsUiState,
    announcer: BoardAnnouncer,
) {
    val summary =
        rememberBoardSpeechFormat().resultsSummary(
            headlineRes = headlineResFor(state.headlineTier),
            stars = state.stars,
            totalStars = CelebrationFx.TOTAL_STARS,
            pegsLeft = state.pegsRemaining,
        )
    var spoken by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.playCelebration) {
        if (state.playCelebration && !spoken) {
            spoken = true
            announcer.announce(summary)
        }
    }
}

/** The headline string resource for [tier] (D-08). */
private fun headlineResFor(tier: ResultsHeadlineTier): Int =
    when (tier) {
        ResultsHeadlineTier.PERFECT -> R.string.results_headline_perfect
        ResultsHeadlineTier.SOLVED -> R.string.results_headline_solved
        ResultsHeadlineTier.CLEARED -> R.string.results_headline_cleared
        ResultsHeadlineTier.STUCK -> R.string.results_headline_stuck
    }

/** No sequencing delay (the first element in a celebration sequence). */
private const val NO_DELAY = 0

/** Alpha of the soft gold glow disc behind an earned star. */
private const val STAR_GLOW_ALPHA = 0.35f
