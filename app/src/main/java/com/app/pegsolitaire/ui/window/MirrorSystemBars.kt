package com.app.pegsolitaire.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Gives the dialog it is composed in the bar state of the screen the dialog opens over
 * (docs/ux-spec.md D-05g, §6.2 "Dialogs mirror it"; docs/tech-design.md §3.8 (c)).
 *
 * A Compose dialog is a window of its own, and a window that fits the system bars shows them for as
 * long as it is open: over the board, a dialog that asked for nothing would bring both bars back.
 * So every dialog places this first in its content. It reads its own window through the dialog's
 * root view, which provides it ([DialogWindowProvider]), reads the host's state
 * ([LocalSystemBarsState]), and asks that window for the same state through [LocalSystemBars], in
 * the composition that opens the dialog and again whenever the state changes. It gives nothing back
 * on closing: the host window keeps its own request. It draws nothing and lays out nothing.
 *
 * Outside a dialog window it asks nothing. A dialog that opens only on Home today mirrors anyway,
 * for the rule's uniformity, so a dialog later hosted over the board is immersive by construction.
 */
@Composable
internal fun MirrorSystemBars() {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    val state = LocalSystemBarsState.current.value
    val systemBars = LocalSystemBars.current
    SideEffect {
        if (dialogWindow != null) systemBars.apply(dialogWindow, state)
    }
}
