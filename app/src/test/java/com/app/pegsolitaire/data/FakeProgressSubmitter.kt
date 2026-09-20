package com.app.pegsolitaire.data

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A settable in-memory [ProgressSubmitter]: drives the D-12 profile-swap question from a
 * test and records how it was answered, with the real pass — and therefore every store and
 * the Play Games seam — never reached.
 *
 * It sits here, beside the class it fakes, rather than beside one consumer, for the same
 * reason [FakePlayGamesOptInRepository] does: two test files need it. `HomeViewModelCloudTest`
 * drives the swap question against it, and `HomeViewModelTest` needs the *quiet* form of it
 * for every pre-Phase-13 assertion — a file-private copy in each would be two doubles free to
 * drift apart on what "no question pending" means.
 *
 * ## Why the collaborators are relaxed mocks
 *
 * The superclass takes a repository, the games seam, four stores and a scope, and this
 * subclass overrides every member that could reach any of them ([start], [onResultRecorded],
 * [profileSwap], [onProfileSwapResolved]) — the base class's own constructor touches none of
 * them, it only builds flags, a mutex and a flow. Threading eight real collaborators through
 * would be noise that says nothing about the behaviour under test. They are still the real
 * types, so this double cannot drift away from the constructor it stands in for.
 *
 * The scope is a plain [SupervisorJob] scope that is never used: nothing here launches.
 *
 * @param initialRequest the question pending at construction — `null` (no question) for
 *   every test that is not about the swap.
 */
class FakeProgressSubmitter(
    initialRequest: ProfileSwapRequest? = null,
) : ProgressSubmitter(
        progressRepository = mockk(relaxed = true),
        gamesService = mockk(relaxed = true),
        syncStore = mockk(relaxed = true),
        cloudSyncStore = mockk(relaxed = true),
        cloudProfileStore = mockk(relaxed = true),
        cloudSaveStore = mockk(relaxed = true),
        challengeProgress = mockk(relaxed = true),
        scope = CoroutineScope(SupervisorJob()),
    ) {
    private val requests = MutableStateFlow(initialRequest)

    override val profileSwap: StateFlow<ProfileSwapRequest?> = requests.asStateFlow()

    /**
     * How each answer arrived, in order — `true` for D-13's merge, `false` for D-14's
     * replace. A list rather than a count so "exactly once, with exactly this answer" is
     * one assertion.
     */
    val resolvedWith = mutableListOf<Boolean>()

    /** Raises (or clears, with `null`) the account-switch question the real pass would raise. */
    fun raise(request: ProfileSwapRequest?) {
        requests.value = request
    }

    /** The real one starts a session collector; a test drives [raise] instead. */
    override fun start() = Unit

    /** The real one launches a whole pass; nothing in Home ever records a result. */
    override fun onResultRecorded() = Unit

    override fun onProfileSwapResolved(keepLocalProgress: Boolean) {
        resolvedWith += keepLocalProgress
    }
}
