package com.app.pegsolitaire.platform.ads.consent

import android.content.Context
import com.app.pegsolitaire.BuildConfig
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.AdConstants
import com.app.pegsolitaire.platform.ads.AdIds
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The User Messaging Platform implementation of [ConsentGateway] — the whole of
 * CONS-01, CONS-02, CONS-03 and CONS-04 in one place (D-16).
 *
 * **The ordering below is load-bearing; do not normalise it.** The consent framework's
 * `requestConsentInfoUpdate()` must run, this session, before anything reads the
 * "may we request ads?" answer: that answer is hard-`false` until the refresh has been
 * asked for at least once per session (Pitfall 4). A readiness check hoisted to the top
 * of [bootstrap] therefore reads `false` on *every* device, everywhere, forever — and the
 * natural-looking repair (delete the check) is a GDPR breach rather than a fix. The
 * refresh is unconditional on every launch for the same reason D-16 gives: consent can be
 * revoked or expire, so "first install only" is wrong twice over.
 *
 * **`@Singleton` is on the class, not only on the `@Binds`.** `@Binds @Singleton` scopes
 * the binding of the *interface*, so anything injecting the concrete type would get a
 * second, unscoped instance carrying its own [_readiness], its own
 * [_privacyOptionsRequired] and its own [initialized] guard — a readiness flow nothing
 * ever updates, permanently `Unknown`. The precedent and the full reasoning are in
 * [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider], annotated
 * this way for exactly the same reason: it holds state.
 *
 * **Nothing here logs, and the absence is enforced by a grep gate rather than by review**
 * — which is also why this paragraph carefully avoids spelling any logging API out.
 * ASVS V7: consent status, the TCF string and ad response info are personal-data-adjacent,
 * so no logging call of any kind may name them. Every failure mode — a form error, a
 * refresh failure, a backgrounded app, a failed SDK initialization — degrades to
 * [AdsReadiness.Unavailable], which D-12 turns into a silent free assist. A consent
 * problem is never a crash, an error wall or a log line.
 */
@Singleton
class UmpConsentGateway
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
        private val activityProvider: ForegroundActivityProvider,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : ConsentGateway {
        /**
         * The process-wide consent-information handle, obtained here rather than injected.
         *
         * **This is deliberately not a Hilt binding, and the reason is a build gate.**
         * `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` reads bytecode, and Hilt's
         * generated `DaggerApp_HiltComponents_SingletonC` lives in the *root* package —
         * outside `..platform..`. A `@Provides` returning this type therefore plants a
         * `Provider<ConsentInformation>` field in that generated class and fails the rule,
         * no matter which module declares the provider. Verified empirically: the module
         * was written inside `..platform..` first and the gate still went red. Moving the
         * module is not the fix; keeping the type out of the graph entirely is.
         *
         * Any future ads-SDK type — a rewarded-ad loader, an initialization config — hits
         * exactly the same wall. Construct it behind a platform-owned seam like this one,
         * never `@Provides` it.
         *
         * `by lazy` rather than an initializer so the consent SDK is not touched merely by
         * Hilt constructing this singleton; nothing loads until [bootstrap] runs.
         */
        private val consentInformation: ConsentInformation by lazy {
            UserMessagingPlatform.getConsentInformation(appContext)
        }

        private val _readiness = MutableStateFlow(AdsReadiness.Unknown)
        private val _privacyOptionsRequired = MutableStateFlow(false)

        override val readiness: StateFlow<AdsReadiness> = _readiness.asStateFlow()

        override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

        /**
         * Exactly one ads-SDK initialization per process (Pitfall 5).
         *
         * Two static call paths reach [initializeAdsIfPermitted] — the consent refresh's
         * success listener and its failure listener — and [bootstrap] itself runs again on
         * every Activity creation, so a configuration change alone is enough to reach it a
         * second time. Without the guard the SDK initializes twice and issues duplicate
         * requests.
         */
        private val initialized = AtomicBoolean(false)

        override fun bootstrap() {
            // Dispatchers.Main because the consent framework's entry points take an
            // Activity and may put a form on screen; the injected application scope runs on
            // Dispatchers.Default.
            scope.launch(Dispatchers.Main) {
                // The provider's own KDoc makes the timeout the caller's responsibility: a
                // backgrounded app may never resume, and an unbounded await would hang this
                // gateway for the life of the process (T-09-20). Giving up leaves readiness
                // Unknown, which every offer site already treats as "not offerable".
                val activity =
                    withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
                        activityProvider.awaitActivity()
                    } ?: return@launch

                val parameters =
                    ConsentRequestParameters
                        .Builder()
                        .apply {
                            // Debug builds only, so an EEA form can be exercised from a
                            // non-EEA test location. Deliberately no test-device hashed ID:
                            // emulators are test devices by default, and adding a fourth ID
                            // key would drag the Phase-6 AdIdResolution plumbing along for a
                            // capability this phase never needs.
                            if (BuildConfig.DEBUG) {
                                setConsentDebugSettings(
                                    ConsentDebugSettings
                                        .Builder(activity)
                                        .setDebugGeography(
                                            ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
                                        ).build(),
                                )
                            }
                        }.build()

                // CONS-01 — unconditional, every launch, and before any readiness check.
                consentInformation.requestConsentInfoUpdate(
                    activity,
                    parameters,
                    {
                        // CONS-02 — show the form only when the framework says one is due.
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            if (formError != null) _readiness.value = AdsReadiness.Unavailable
                            publishPrivacyOptionsRequirement()
                            initializeAdsIfPermitted()
                        }
                    },
                    {
                        // CONS-04 — a failed refresh never blocks play and never surfaces an
                        // error. Carrying on uses the previous session's cached status, which
                        // is precisely the required fallback.
                        publishPrivacyOptionsRequirement()
                        initializeAdsIfPermitted()
                    },
                )
            }
        }

        override suspend fun showPrivacyOptions() {
            val activity =
                withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
                    activityProvider.awaitActivity()
                } ?: return

            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
                        // The form error is deliberately discarded rather than logged: a form
                        // that failed to open leaves the player's stored choices untouched,
                        // and the only honest reaction is to re-read the requirement below.
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }

            publishPrivacyOptionsRequirement()
        }

        /**
         * Initializes the ads SDK, once, only when consent permits it (CONS-02, T-09-17).
         *
         * Never call this before the consent refresh has been requested this session — see
         * the class KDoc.
         */
        private fun initializeAdsIfPermitted() {
            if (!consentInformation.canRequestAds()) {
                // Consent refused, or the refresh failed with no usable cached status.
                if (_readiness.value == AdsReadiness.Unknown) {
                    _readiness.value = AdsReadiness.Unavailable
                }
                return
            }
            if (!initialized.compareAndSet(false, true)) {
                // A second caller: report what is actually true rather than assuming Ready.
                // The first caller's initialization may still be in flight, and publishing
                // Ready early would offer an ad against an uninitialized SDK — the exact
                // "watch an ad? → instant free grant" absurdity D-12 exists to rule out
                // (Pitfall 16). Unavailable is the fail-open answer, and the in-flight
                // launch below overwrites it with the truth the moment it finishes.
                _readiness.value =
                    if (MobileAds.isInitialized) AdsReadiness.Ready else AdsReadiness.Unavailable
                return
            }
            // MobileAds.initialize is annotated @WorkerThread — "this must be called on a
            // background thread, failure to do so may cause an ANR". Initialization is also
            // mandatory in GMA Next-Gen, unlike the legacy SDK: skipping it throws
            // UninitializedPropertyAccessException on the first ad call.
            scope.launch(Dispatchers.IO) {
                // The two-argument overload, deliberately: the third parameter is an
                // adapter-initialization listener, and this app runs no mediation adapters,
                // so an empty callback would only document a hook nothing uses.
                MobileAds.initialize(
                    appContext,
                    InitializationConfig.Builder(AdIds.admobAppId).build(),
                )
                _readiness.value =
                    if (MobileAds.isInitialized) AdsReadiness.Ready else AdsReadiness.Unavailable
            }
        }

        /**
         * Publishes CONS-03's privacy-options requirement straight from the consent
         * framework.
         *
         * The status is configured server-side in the AdMob Privacy & messaging UI and is
         * the only source of truth. Do **not** infer it from a client-side region check: the
         * app would be guessing at a value it is being told (T-09-18).
         */
        private fun publishPrivacyOptionsRequirement() {
            _privacyOptionsRequired.value =
                consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }
    }
