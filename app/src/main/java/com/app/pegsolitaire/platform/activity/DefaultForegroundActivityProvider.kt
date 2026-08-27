package com.app.pegsolitaire.platform.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The process-lifetime [ForegroundActivityProvider], fed by
 * `Application.registerActivityLifecycleCallbacks` and holding the resumed Activity
 * behind a [WeakReference] so a singleton can never outlive a view hierarchy
 * (T-06-14). Both clears are **identity-checked** (reference equality, never `equals`):
 * on an A→B transition B resumes before A pauses, so an unchecked clear would evict the
 * live Activity and leave the platform layer blind.
 *
 * **This class carries `@Singleton`, and that is not a stylistic slip.**
 * `@Binds @Singleton fun bind(impl: X): Y` scopes the binding of *`Y`*, not of `X` — so
 * if anything injected the concrete type it would get a **second, unscoped instance**
 * that is never fed a lifecycle event, and `awaitActivity()` would hang forever with no
 * exception (T-06-15). [com.app.pegsolitaire.App] therefore injects the *interface* and
 * never names this class. Its precedent is
 * [com.app.pegsolitaire.data.ChallengeProgress], the repo's other `@Singleton`-on-the-class,
 * annotated that way for the same reason: it holds state. `NoOpAdGateway` and
 * `NoOpGamesService` deliberately do **not** carry `@Singleton` on the class, which is
 * correct for them because they are stateless and nothing injects their concrete type.
 * Do not "normalise" this one back to match them.
 *
 * Unlike [com.app.pegsolitaire.data.ChallengeProgress], this class injects no app-lifetime
 * coroutine scope and does no eager sharing: `mapNotNull { … }.first()` needs neither, so
 * the seam has no app-lifetime dependency at all. Only [ChallengeProgress]'s `@Singleton`
 * *shell* was copied here, not its body.
 */
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() : ForegroundActivityProvider, Application.ActivityLifecycleCallbacks {
        private val resumed = MutableStateFlow<WeakReference<Activity>?>(null)

        override val current: Activity? get() = resumed.value?.get()

        override suspend fun awaitActivity(): Activity = resumed.mapNotNull { it?.get() }.first()

        override fun attachTo(application: Application) = application.registerActivityLifecycleCallbacks(this)

        override fun onActivityResumed(activity: Activity) {
            resumed.value = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            if (current === activity) resumed.value = null
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (current === activity) resumed.value = null
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit
    }
