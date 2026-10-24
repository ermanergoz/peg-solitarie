package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Fires one light haptic tick as each jump animation begins (SET-02 / D-15).
 *
 * Haptic emission is a Compose UI concern, so it lives here rather than in the ViewModel:
 * the screen reads the already-collected `hapticsOn` gate (it never touches
 * `SettingsRepository`) and the effect is keyed on the [MoveTransition.id] move ordinal —
 * so it fires exactly once per jump, not once per recomposition. When the setting is off
 * nothing fires and gameplay is otherwise identical.
 *
 * [HapticFeedbackType.ContextClick] is the lightest type in the pinned Compose BOM that is
 * available across the whole supported range (`CONTEXT_CLICK`, API 23 ≤ minSdk 24) — the
 * newer `SegmentTick`/`GestureEnd` constants map to API-34-only platform constants and
 * would be silent on most devices, and `LongPress` is far too heavy for a peg hop.
 *
 * **Shared by the gameplay screen and the tutorial screen** (D-01j), the way `JumpSoundEffect`
 * is (D-01i): both screens jump real pegs on the real rules engine, and a tutorial that taught
 * for thirty seconds that a jump is felt by nothing, then buzzed on the first real board, would be
 * the divergence D-01i refused for sound. It was lifted out of `GameScreen.kt` rather than
 * re-implemented, so the type, the key and the gate exist exactly once. It lives beside the
 * screens and never inside `BoardCanvas`, which every board on every screen draws with. Both
 * screens carry the gate the same way, on their board state: the tutorial reads the persisted
 * switch through the read-only `HapticsSetting` seam (tech-lead ruling 2026-10-18). Lifting it
 * engages none of §5.1's four prohibitions: it reads a move id and a Boolean, and touches no
 * `GameViewModel`, repository or navigation.
 *
 * @param animatingMoveId the in-flight jump's move ordinal, or `null` when settled.
 * @param hapticsOn the persisted haptics gate carried by the UI state.
 */
@Composable
internal fun JumpHapticEffect(
    animatingMoveId: Int?,
    hapticsOn: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(animatingMoveId) {
        if (animatingMoveId != null && hapticsOn) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}
