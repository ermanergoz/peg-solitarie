package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** RED stub — see plan 12-07 Task 1. Replaced by the real implementation in the GREEN commit. */
@Singleton
open class SubmissionSyncStore
    @Inject
    internal constructor(
        @Suppress("unused") private val dataStore: DataStore<Preferences>,
    ) {
        open val pending: Flow<Boolean> = flowOf(false)

        open suspend fun setPending(pending: Boolean) {
            // RED stub: persists nothing.
        }
    }
