package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.withTabularFigures
import kotlin.math.max

/**
 * A clock string, the timer's `mm:ss`, with every digit set in a cell as wide as the widest digit
 * of the face [style] resolves to. The clock then keeps one width as it ticks (ux-spec §4.3.1,
 * "Tabular figures must actually be tabular"; criterion 18d).
 *
 * ## Why cells, when the style already asks for tabular figures
 *
 * `withTabularFigures()` only *requests* the `tnum` feature, and the shipped face has none.
 * Fredoka, the downloadable face of the title and headline roles, sets its digits proportionally
 * ("1" is 388 units wide, "2" is 587). A plain `Text` of the time therefore narrowed by up to 18dp
 * whenever a "1" ticked in, and the pill around it narrowed with it. The requirement is the
 * property, not the flag, so the layout keeps it: each digit is centred in its cell, and the colon
 * keeps its own width. In a face whose digits already are tabular, such as the platform fallback,
 * every digit fills its cell and nothing moves.
 *
 * ## Measured while measuring
 *
 * The widest digit is read in the layout pass, not in composition: every pass measures the ten
 * digits in the face the style resolves to at that moment. The downloadable face arrives after
 * the first frame. When it does, the cells' own text is laid out again in it, which lays this
 * clock out again too, so the cells take the real face's digits. A width remembered in
 * composition would keep the fallback's width forever.
 *
 * ## Left to right in every layout direction
 *
 * A clock reads left to right, and so did the single string: digits with a colon between them
 * resolve to one left-to-right run even in a right-to-left paragraph. The cells are therefore
 * placed absolutely and never mirrored.
 *
 * ## One node speaks for the clock
 *
 * The clock's own node carries the time as its text, and [modifier] adds whatever description
 * the caller gives it. The one-character cells are hidden from accessibility services, so TalkBack
 * meets exactly the node the single `Text` was, while a test can still read each cell's text
 * layout.
 *
 * @param time the clock to set, e.g. `05:12`.
 * @param style the text style of every cell.
 * @param color the colour of every cell.
 * @param modifier layout and semantics modifier for the clock's node.
 */
@Composable
internal fun TimerDigits(
    time: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = DECIMAL_DIGITS.length)
    Layout(
        content = {
            time.forEach { character ->
                Text(
                    text = character.toString(),
                    style = style,
                    color = color,
                    modifier = Modifier.semantics { hideFromAccessibility() },
                )
            }
        },
        modifier = modifier.semantics { text = AnnotatedString(time) },
    ) { measurables, constraints ->
        val cell = DECIMAL_DIGITS.maxOf { digit -> measurer.measure(digit.toString(), style).size.width }
        // A cell may be as tall as the clock may be, as the single string was, so a clock clipped
        // vertically still reports it in each cell's own text layout.
        val cellConstraints = Constraints(maxHeight = constraints.maxHeight)
        val characters = measurables.map { it.measure(cellConstraints) }
        val slots =
            characters.mapIndexed { index, character ->
                if (time[index].isDigit()) max(cell, character.width) else character.width
            }
        val height = characters.maxOfOrNull { it.height } ?: 0
        layout(constraints.constrainWidth(slots.sum()), constraints.constrainHeight(height)) {
            var x = 0
            characters.forEachIndexed { index, character ->
                character.place(x + (slots[index] - character.width) / 2, 0)
                x += slots[index]
            }
        }
    }
}

/**
 * The narrowest clock in Fredoka, "11:11", over its widest, "22:22", on the pill's surface in both
 * themes. In the shipped face the two are one width.
 */
@Preview(name = "Timer digits · light")
@Preview(name = "Timer digits · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TimerDigitsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                listOf(PREVIEW_ONES_SECONDS, PREVIEW_TWOS_SECONDS).forEach { seconds ->
                    TimerDigits(
                        time = GameTimerFormat.format(seconds),
                        style = MaterialTheme.typography.headlineSmall.withTabularFigures(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** The ten digits; the widest of them sets the cell. */
private const val DECIMAL_DIGITS = "0123456789"

/** 11:11 and 22:22 in seconds: Fredoka's narrowest digit, "1", and its widest, "2", throughout. */
private const val PREVIEW_ONES_SECONDS = 671
private const val PREVIEW_TWOS_SECONDS = 1342
