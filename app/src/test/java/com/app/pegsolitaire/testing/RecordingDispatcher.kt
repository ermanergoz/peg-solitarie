package com.app.pegsolitaire.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that forwards to [delegate] and counts every dispatch it is asked to make.
 *
 * Proves a class runs its work on the dispatcher it was **injected** with: a class that ignored the
 * injection (a hard-coded `Dispatchers.IO`, or no switch at all) leaves [dispatches] at zero, which a
 * paused test dispatcher alone cannot show deterministically. Test-source-set only.
 */
internal class RecordingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    /** How many times work was dispatched through this dispatcher. */
    var dispatches = 0
        private set

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatches++
        delegate.dispatch(context, block)
    }
}
