package com.app.pegsolitaire.data

import kotlinx.coroutines.flow.Flow

/**
 * The one setting a screen may read when it may read no other: whether haptic feedback is on
 * (SET-02).
 *
 * It exists for the tutorial. `TutorialViewModel` is pinned to its collaborators by a reflection
 * test (ux-spec §5.1, prohibitions 2 and 3), and handing it the whole [SettingsRepository] would
 * widen that pin to a type that can read the hint switch the tutorial overrides (D-01b), read the
 * sound switch only the player may read, and write all three. This seam reads one Boolean and
 * writes nothing, so the pin can name it and still mean "no progress dependency"; a second test
 * keeps it to this single member (tech-lead ruling 2026-10-18).
 *
 * [SettingsRepository] extends it, and Hilt binds it to that same repository, so there is one
 * source of truth for the switch.
 */
interface HapticsSetting {
    /** Whether haptic feedback is enabled; fail-open default `true` (SET-02). */
    val hapticsOn: Flow<Boolean>
}
