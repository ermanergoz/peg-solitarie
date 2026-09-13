package com.app.pegsolitaire.data

import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.games.GamesService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** RED stub — see plan 12-07 Task 2. Replaced by the real pass in the GREEN commit. */
@Singleton
open class ProgressSubmitter
    @Inject
    internal constructor(
        @Suppress("unused") private val progressRepository: ProgressRepository,
        @Suppress("unused") private val gamesService: GamesService,
        @Suppress("unused") private val syncStore: SubmissionSyncStore,
        @param:ApplicationScope @Suppress("unused") private val scope: CoroutineScope,
    ) {
        private val passMutex = Mutex()

        open fun start() {
            // RED stub: no collector.
        }

        open fun onResultRecorded() {
            // RED stub: submits nothing.
        }

        private suspend fun submitAll() = passMutex.withLock { }
    }
