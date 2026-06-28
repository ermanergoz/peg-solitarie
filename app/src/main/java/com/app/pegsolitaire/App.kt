package com.app.pegsolitaire

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point and Hilt's `SingletonComponent` root.
 *
 * `@HiltAndroidApp` triggers Hilt's code generation: the processor (running under
 * KSP — no KAPT) emits the component graph so `@AndroidEntryPoint` activities and,
 * later, `@HiltViewModel` view models can be injected with zero further build
 * setup. The Kotlin pin (2.3.20) has a matching KSP build (2.3.9), unblocking DI.
 */
@HiltAndroidApp
class App : Application()
