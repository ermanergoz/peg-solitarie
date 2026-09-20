package com.app.pegsolitaire.platform.games.save

import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.games.GamesConstants
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.google.android.gms.games.PlayGames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's own "select snapshot" screen, opened read-only — the only way left to **see** what a
 * commit's listing metadata looks like (CLOUD-03), now that the Play Games app has no saves screen
 * and `am start -a com.google.android.gms.games.SHOW_SELECT_SNAPSHOT` does not resolve from a shell.
 *
 * **Nothing player-facing reaches this class.** Its one caller is [DebugSnapshotPickerEntry], which
 * is inert unless the build is debuggable, so D-03's silent design and the "exactly one cloud-save
 * dialog" reservation on [CloudSaveConstants.SNAPSHOT_NAME] are both untouched in what ships.
 *
 * It is a **viewer**: `allowAddButton` and `allowDelete` are both `false`, so a QA session can
 * neither create a second snapshot nor delete the player's only one, and whatever the tester taps
 * comes back as an activity result that nothing consumes.
 *
 * The launch follows [com.app.pegsolitaire.platform.games.PlayGamesService]'s stock-UI shape, for
 * its reasons: the SDK javadoc requires `startActivityForResult` so the calling package can be
 * identified, the client is built per call and held in no field, every failure is swallowed, and
 * nothing is logged.
 */
@Singleton
class PlayGamesSnapshotPicker
    @Inject
    constructor(
        private val activityProvider: ForegroundActivityProvider,
        private val gamesService: GamesService,
    ) {
        /**
         * Shows the picker once the session is signed in and an Activity is in the foreground, or
         * does nothing at all if either fails to arrive within its bound.
         *
         * **Why it waits for the session.** The debug extra is read in `onCreate`, on the same
         * frame the sign-in bootstrap is started, so an immediate call would reach the SDK before
         * it knows who the player is and be refused. Both waits are bounded: an unbounded one
         * inside a process-lifetime singleton parks a coroutine for the life of the process
         * (T-11-06).
         */
        suspend fun show() {
            withTimeoutOrNull(CloudSaveConstants.DEBUG_PICKER_SIGN_IN_WAIT_MS) {
                gamesService.session.first { it is GamesSignInState.SignedIn }
            } ?: return
            val activity =
                withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() } ?: return

            runCatching {
                val intent =
                    PlayGames
                        .getSnapshotsClient(activity)
                        .getSelectSnapshotIntent(
                            CloudSaveConstants.DEBUG_PICKER_TITLE,
                            // allowAddButton, then allowDelete: a viewer offers neither.
                            false,
                            false,
                            CloudSaveConstants.DEBUG_PICKER_MAX_SNAPSHOTS,
                        ).await()
                activity.startActivityForResult(intent, CloudSaveConstants.DEBUG_PICKER_REQUEST_CODE)
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
