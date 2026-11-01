package com.app.pegsolitaire.di

import com.app.pegsolitaire.platform.games.save.DebugSnapshotPickerEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The **release** variant's binding for [DebugSnapshotPickerEntry]: a gate with nothing behind it.
 *
 * The QA-only snapshot picker lives in the `debug` source set and is not compiled into release
 * (tech-lead L-3), so this variant has no `show` to bind. It provides an inert one — and `false` for
 * the build gate, which already made [DebugSnapshotPickerEntry.onIntent] return on its first line.
 * A release build is therefore inert twice over: it ignores the extra, and there is nothing it could
 * have opened.
 *
 * **This module is not dead weight, it is the other half of the split.** Hilt resolves one binding
 * per type, and the debug module names a class that does not exist here, so exactly one of the two
 * must be compiled per build type. Deleting this file breaks the release build rather than shrinking
 * it.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReleaseSnapshotPickerModule {
    @Provides
    @Singleton
    fun provideDebugSnapshotPickerEntry(): DebugSnapshotPickerEntry =
        DebugSnapshotPickerEntry(debugBuild = false, show = {})
}
