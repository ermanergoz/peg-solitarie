package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.withTabularFigures

/** Semantics tag the tests use to locate the status strip — never its copy (ux-spec §13). */
const val GAME_STATUS_STRIP_TEST_TAG = "game_status_strip"

/** Semantics tag of the timer's clay pill, whose height criteria 18a and 18e measure. */
const val GAME_TIMER_PILL_TEST_TAG = "game_timer_pill"

/**
 * The gameplay status strip (CLAS-02, SCOR-02, ux-spec §4.3, §4.3.1, D-04/D-05a/D-05b/D-05d) —
 * what replaces the deleted `GameTopBar`.
 *
 * A plain transparent [Row]: back chip, context title, timer pill, and **no actions at all**.
 * The strip is status only; the four things the player *does* moved down into [GameActionDock],
 * in the thumb zone where D-04 wants them.
 *
 * ## Big enough to read at a glance (D-05d)
 *
 * The title is `titleLarge` and the timer `headlineSmall`, in a clay pill that grows with the font
 * scale. `titleMedium` had made the strip quiet, and quiet turned out to mean unreadable: the two
 * things a player looks up for mid-game were the smallest text on the screen. The timer takes the
 * larger role because it is the one that changes — the number a player checks.
 *
 * ## The status bar is not applied here (D-05d)
 *
 * The gameplay `Scaffold` already hands its content a padding that holds the status bar and the
 * navigation bar, and `GameScreen` applies it once to the column this strip heads. The strip used
 * to apply `statusBarsPadding()` on top — an empty band as tall as the status bar above it, which
 * with the dock's matching second navigation inset took 72dp from the board on a 360x640dp phone.
 *
 * ## Not a Material bar, under any name (D-05c)
 *
 * It is not a `TopAppBar` and not a `CenterAlignedTopAppBar`: there is no bar background, no bar
 * elevation and no bar title row, which is criterion 16. Making it one again would also bring
 * back the insets and scroll behaviour that the founder's "remove the toolbar" was about.
 *
 * ## Two preserved content descriptions, and the timer's that says its time
 *
 * `navigate_back` is carried across **unchanged** (D-05b): `ButtonSoundTest` finds the back control
 * by it and must keep passing byte-unchanged; if it had to move, the drift would be in the
 * production code and not in the test. The timer's description still **starts** with the unchanged
 * `elapsed_time` label, and since D-05e (§4.3.2) it goes on to the time itself, so the tests that
 * found the clock by the label alone find it by the pill and by the label as the start.
 *
 * The back chip is **silent** (D-09): a back-navigation affordance acts and says nothing, so it
 * carries no `withClickSound` wrap — and the cue must never be folded into the shared
 * [claySquish] below it, which is also on the dock's controls.
 *
 * @param elapsedSeconds the live elapsed time (D-04), rendered `mm:ss` in fixed-width digit cells
 *   so ticking digits never shift the pill's width.
 * @param challengeLevel the Challenge level when this is a Challenge game, else `null` for the
 *   app-name title. The one mode-aware element on the whole screen.
 * @param onNavigateBack leaves the gameplay screen.
 * @param modifier layout modifier for the strip.
 */
@Composable
internal fun GameStatusStrip(
    elapsedSeconds: Int,
    challengeLevel: Int?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.game_status_strip_height))
                .padding(horizontal = dimensionResource(R.dimen.spacing_md))
                .testTag(GAME_STATUS_STRIP_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackChip(onNavigateBack = onNavigateBack)
        GameContextTitle(
            challengeLevel = challengeLevel,
            modifier = Modifier.weight(TITLE_WEIGHT),
        )
        TimerPill(elapsedSeconds = elapsedSeconds)
    }
}

/**
 * The leading back control: a clay circular chip at `game_status_back_size`.
 *
 * 48dp rather than the dock's 56dp, deliberately: it is the one control on this screen the
 * player is not meant to reach for during play, and the accessibility floor is the right size
 * for it (§11 "Touch targets").
 */
@Composable
private fun BackChip(onNavigateBack: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clayShadow(dimensionResource(R.dimen.elevation_pressed), CircleShape),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier =
                Modifier
                    .claySquish()
                    .size(dimensionResource(R.dimen.game_status_back_size)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
                modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
            )
        }
    }
}

/**
 * The context-aware title, carried over from the deleted bar's `GameTopBarTitle` **verbatim in
 * logic** (S2 / D-03): in Challenge mode the neutral "Level N" indicator with its
 * `challenge_level_indicator_desc` announcement, otherwise the static app name.
 *
 * `titleLarge` (D-05d), start-aligned, with `maxLines = 1` and a weight, so a long title — or any
 * title at 200% — truncates **before** it can push the timer pill off screen. The timer must never
 * be clipped: it is the only place elapsed time appears (§11 "Text scaling"). The ellipsis is drawn
 * and never spoken: TalkBack reads the full text.
 */
@Composable
private fun GameContextTitle(
    challengeLevel: Int?,
    modifier: Modifier = Modifier,
) {
    if (challengeLevel != null) {
        val description = stringResource(R.string.challenge_level_indicator_desc, challengeLevel)
        Text(
            text = stringResource(R.string.challenge_level_indicator, challengeLevel),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier =
                modifier.semantics {
                    contentDescription = description
                    heading()
                },
        )
    } else {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.semantics { heading() },
        )
    }
}

/**
 * The trailing timer pill: the `mm:ss` clock in `headlineSmall` on a small clay surface — the back
 * chip's own shadow, so the strip reads as two clay chips framing a title (D-05d).
 *
 * `game_timer_pill_height` is a **minimum**: at a large font scale the pill grows to hold the
 * digits rather than clipping them, which a fixed height would do at 200% (criterion 18e).
 *
 * The zero-padded time is a computed string ([GameTimerFormat]), not a string resource.
 * [TimerDigits] sets every digit in a cell as wide as the face's widest digit, so the pill keeps one
 * width as the clock ticks, even in the shipped face, which has no tabular figures for
 * `withTabularFigures()` to switch on (criterion 18d).
 *
 * ## What TalkBack says (D-05e, §4.3.2)
 *
 * The clock's one node is described as `elapsed_time_spoken` — the `elapsed_time` label, a comma
 * and the time as a duration, "Elapsed time, 5 minutes, 12 seconds" — computed from the same
 * [elapsedSeconds] as the clock, in the same composition, so what TalkBack says and what the pill
 * shows cannot disagree. TalkBack speaks a node's description in place of its text, so with the
 * label alone it said "Elapsed time" and never the time. The node is still the only stop: the cells
 * stay hidden (N-2), and there is no live region, no role and no heading, so the time is read when a
 * player moves to it and at no other time.
 */
@Composable
private fun TimerPill(elapsedSeconds: Int) {
    val description =
        stringResource(
            R.string.elapsed_time_spoken,
            stringResource(R.string.elapsed_time),
            spokenDuration(elapsedSeconds),
        )
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier
                .heightIn(min = dimensionResource(R.dimen.game_timer_pill_height))
                .clayShadow(dimensionResource(R.dimen.elevation_pressed), MaterialTheme.shapes.small)
                .testTag(GAME_TIMER_PILL_TEST_TAG),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.game_timer_pill_padding)),
            contentAlignment = Alignment.Center,
        ) {
            TimerDigits(
                time = GameTimerFormat.format(elapsedSeconds),
                style = MaterialTheme.typography.headlineSmall.withTabularFigures(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/**
 * The elapsed time as TalkBack says it — "5 minutes, 12 seconds" — from the parts
 * [ElapsedTimeSpeech] says, in the copy's plurals and its order of parts (§4.3.2, §8).
 */
@Composable
private fun spokenDuration(totalSeconds: Int): String =
    when (val said = ElapsedTimeSpeech.parts(totalSeconds)) {
        is SpokenDuration.Minutes -> minutesSaid(said.minutes)
        is SpokenDuration.Seconds -> secondsSaid(said.seconds)
        is SpokenDuration.MinutesAndSeconds ->
            stringResource(R.string.duration_minutes_seconds, minutesSaid(said.minutes), secondsSaid(said.seconds))
    }

@Composable
private fun minutesSaid(minutes: Int): String = pluralStringResource(R.plurals.duration_minutes, minutes, minutes)

@Composable
private fun secondsSaid(seconds: Int): String = pluralStringResource(R.plurals.duration_seconds, seconds, seconds)

/**
 * The strip in both of its titles — Classic's app name, then a four-digit Challenge level — on the
 * app background, in both themes.
 */
@Preview(name = "Status strip · light", widthDp = PREVIEW_STRIP_WIDTH_DP)
@Preview(
    name = "Status strip · dark",
    widthDp = PREVIEW_STRIP_WIDTH_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GameStatusStripPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                GameStatusStrip(elapsedSeconds = PREVIEW_ELAPSED_SECONDS, challengeLevel = null, onNavigateBack = {})
                GameStatusStrip(
                    elapsedSeconds = PREVIEW_ELAPSED_SECONDS,
                    challengeLevel = PREVIEW_CHALLENGE_LEVEL,
                    onNavigateBack = {},
                )
            }
        }
    }
}

/** The title takes the width the back chip and the timer pill leave, and truncates inside it. */
private const val TITLE_WEIGHT = 1f

/** One line, always: the timer may never be pushed off screen by a long title. */
private const val TITLE_MAX_LINES = 1

/** The design baseline's width (§4.3.1's width budget), and a representative clock and level. */
private const val PREVIEW_STRIP_WIDTH_DP = 360
private const val PREVIEW_ELAPSED_SECONDS = 488
private const val PREVIEW_CHALLENGE_LEVEL = 1234
