package com.app.pegsolitaire.ui.theme

import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The palette setting, collected once for the whole process (docs/ux/colour-vision.md §3.3).
 *
 * The `ChallengeProgress` idiom — `stateIn(@ApplicationScope, Eagerly, default)` over a
 * DataStore flow — which gives the three properties the spec binds:
 * 1. before the store's first read lands, [palette] is [BoardMarkPalette.Standard];
 * 2. a configuration change never flashes the other palette: this is a `@Singleton`, so a
 *    recreated Activity's root starts from the value already held here, not from a default;
 * 3. a change in Settings reaches every consumer without recreating anything: the write lands in
 *    the store, the repository flow emits, and the root's collector re-provides the theme local.
 *
 * `Eagerly` rather than `WhileSubscribed`, for property 2: the value must be warm before a
 * recreated Activity's first frame. Its only injector is `MainActivity`, so the read starts in
 * `onCreate`, off the main thread (the application scope, and DataStore's own dispatcher), as a
 * second subscriber to the shared `app_prefs` store the start path already reads. A failed read
 * reaches here as `false` from the repository, which maps to [BoardMarkPalette.Standard] (CV-04).
 * No interface and no module binding: a holder with an `@Inject` constructor, like
 * `ProgressSubmitter`.
 */
@Singleton
class BoardMarkPaletteState
    @Inject
    constructor(
        settings: SettingsRepository,
        @ApplicationScope scope: CoroutineScope,
    ) {
        /** The palette in use; [BoardMarkPalette.Standard] until the store's first emission. */
        val palette: StateFlow<BoardMarkPalette> =
            settings.colorblindPaletteOn
                .map { on -> if (on) BoardMarkPalette.Colorblind else BoardMarkPalette.Standard }
                .stateIn(scope, SharingStarted.Eagerly, BoardMarkPalette.Standard)
    }
