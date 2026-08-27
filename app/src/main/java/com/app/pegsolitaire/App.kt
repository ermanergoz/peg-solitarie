package com.app.pegsolitaire

import android.app.Application
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point and Hilt's `SingletonComponent` root.
 *
 * `@HiltAndroidApp` triggers Hilt's code generation: the processor (running under
 * KSP — no KAPT) emits the component graph so `@AndroidEntryPoint` activities and,
 * later, `@HiltViewModel` view models can be injected with zero further build
 * setup. The Kotlin pin (2.3.20) has a matching KSP build (2.3.9), unblocking DI.
 *
 * It is also the registrar for [ForegroundActivityProvider] (PLAT-01). The ordering in
 * [onCreate] is **not optional**: Hilt performs field injection into an `Application`
 * as part of `super.onCreate()`, so [activityProvider] is only non-null once that call
 * has returned — registering the lifecycle callbacks before it would throw
 * `UninitializedPropertyAccessException`.
 *
 * The declared type is the **interface**, never the implementation. `@Binds @Singleton`
 * scopes the binding of the interface, not of the concrete class, so naming the concrete
 * type here would hand this class a second, unscoped instance that never receives a
 * lifecycle event — and `awaitActivity()` would then hang forever with no exception.
 *
 * `App` is consequently the one class outside `com.app.pegsolitaire.platform..` allowed
 * to depend on [ForegroundActivityProvider]: [PlatformBoundaryTest] exempts it by name
 * with a `haveNameNotMatching(".*\\.App")` clause. That exemption is deliberately
 * documented at both ends — if this registration ever moves, remove the exemption too.
 */
@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var activityProvider: ForegroundActivityProvider

    override fun onCreate() {
        super.onCreate()
        activityProvider.attachTo(this)
    }
}
