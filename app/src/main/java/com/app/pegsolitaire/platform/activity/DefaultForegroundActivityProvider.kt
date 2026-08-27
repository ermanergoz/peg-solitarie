package com.app.pegsolitaire.platform.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Skeleton [ForegroundActivityProvider] — behavior lands in the GREEN step.
 */
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() : ForegroundActivityProvider, Application.ActivityLifecycleCallbacks {
        override val current: Activity? get() = null

        override suspend fun awaitActivity(): Activity = error("not implemented")

        override fun attachTo(application: Application) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit

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
