package com.app.pegsolitaire

import android.app.Application

/**
 * Application entry point.
 *
 * Intentionally a plain [Application] in Plan 01: Hilt's `@HiltAndroidApp` is
 * deferred because KSP (which runs Hilt's annotation processor) has no Kotlin
 * 2.4.0 build available yet. DI is wired in the plan that first needs a real
 * object graph (`@HiltViewModel`). See 01-01-SUMMARY.md "Deferred: Hilt/KSP".
 */
class App : Application()
