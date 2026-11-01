package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Tells [clock] about every pointer-down and key-down in the host (B-22 rule 3, the tech-lead's
 * A5.3), so idle motion settles and a touch wakes it. It sits on the host `Box` that parents the
 * `NavHost` and the backdrop, never on the backdrop itself (BD-15).
 *
 * It only observes: pointer events are read at the `Initial` pass and never consumed, and the key
 * handler always returns false, so no tap, drag, long-press or key on the board, a tile or a
 * control is delayed or changed.
 */
fun Modifier.backdropInputObserver(clock: BackdropClock): Modifier =
    pointerInput(clock) {
        awaitPointerEventScope {
            while (true) {
                if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) clock.onInput()
            }
        }
    }.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) clock.onInput()
        false
    }
