package com.app.pegsolitaire.di

import com.app.pegsolitaire.BuildConfig
import com.app.pegsolitaire.platform.games.save.DebugSnapshotPickerEntry
import com.app.pegsolitaire.platform.games.save.PlayGamesSnapshotPicker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The **debug** variant's binding for [DebugSnapshotPickerEntry]: the gate wired to the real
 * [PlayGamesSnapshotPicker] (CLOUD-03 verification).
 *
 * This module and its release sibling (`app/src/release/.../ReleaseSnapshotPickerModule.kt`) are one
 * binding declared twice, once per build type, which is what keeps the picker out of the release
 * build **structurally** rather than behind a runtime boolean (tech-lead L-3). A single module in
 * `src/main` could not do it: the release variant does not compile [PlayGamesSnapshotPicker] at all,
 * so no `src/main` file may name it.
 *
 * The provider takes the picker as a parameter and hands the gate `picker::show`, so the only type
 * the generated component ever sees is the SDK-free picker class — it constructs its snapshot client
 * per call and holds none, the same containment [PlatformModule]'s KDoc spells out at length. Do not
 * add an SDK-typed `@Provides` here either; the rule is about the generated component, not about
 * which module declares the binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugSnapshotPickerModule {
    /**
     * [BuildConfig.DEBUG] is still read rather than passed as `true`: this module is compiled for
     * the debug build type, whose `BuildConfig.DEBUG` *is* `true`, and reading it keeps the value
     * the build's own rather than a second claim about the build that could drift from it.
     */
    @Provides
    @Singleton
    fun provideDebugSnapshotPickerEntry(picker: PlayGamesSnapshotPicker): DebugSnapshotPickerEntry =
        DebugSnapshotPickerEntry(debugBuild = BuildConfig.DEBUG, show = picker::show)
}
