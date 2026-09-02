package com.app.pegsolitaire.platform.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.app.pegsolitaire.data.InterstitialCadenceStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates real whole-app foreground/background into the interstitial session boundary
 * (D-04, D-16) — see the implementation commit for the full contract.
 */
@Singleton
class AppForegroundTracker
    @Inject
    constructor(
        private val store: InterstitialCadenceStore,
    ) : DefaultLifecycleObserver {
        fun attach(): Unit = TODO("10-02 task 2: observe ProcessLifecycleOwner")

        override fun onStart(owner: LifecycleOwner): Unit = TODO("10-02 task 2: $store")

        override fun onStop(owner: LifecycleOwner): Unit = TODO("10-02 task 2: $store")
    }
