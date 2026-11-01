package com.app.pegsolitaire.ui.settings

import com.app.pegsolitaire.platform.ads.consent.AdsReadiness
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A settable in-memory [ConsentGateway] for the Settings tests (CONS-03).
 *
 * Shared across the three `SettingsViewModel*` test classes rather than duplicated
 * per-file: [SettingsViewModelTest] and [SettingsViewModelSoundTest] only need *some*
 * gateway to satisfy the constructor, while `SettingsPrivacyTest` drives
 * [privacyOptionsRequired] as the actual subject under test. One fake keeps those three
 * from drifting apart. (The `SettingsRepository` fakes stay file-private because each test
 * class needs a different observation surface from them; this one is genuinely identical
 * for all three.)
 *
 * Defaults to `false` — the same fail-closed default the real [SettingsUiState] carries —
 * so a test that never touches consent sees the row hidden, which is the production
 * behaviour for the majority of players.
 *
 * @param privacyOptionsRequired the initial CONS-03 requirement answer.
 * @param readiness the initial ads-readiness answer; irrelevant to Settings, which observes
 *   only [privacyOptionsRequired], but part of the interface contract.
 */
class FakeConsentGateway(
    privacyOptionsRequired: Boolean = false,
    readiness: AdsReadiness = AdsReadiness.Unknown,
) : ConsentGateway {
    private val privacyOptionsState = MutableStateFlow(privacyOptionsRequired)
    private val readinessState = MutableStateFlow(readiness)

    override val readiness: StateFlow<AdsReadiness> = readinessState.asStateFlow()

    override val privacyOptionsRequired: StateFlow<Boolean> = privacyOptionsState.asStateFlow()

    /** How many times [bootstrap] was called — the consent flow must be idempotent per Activity. */
    var bootstrapCount = 0
        private set

    /** How many times [showPrivacyOptions] was called (the CONS-03 row's whole job). */
    var showPrivacyOptionsCount = 0
        private set

    override fun bootstrap() {
        bootstrapCount++
    }

    override suspend fun showPrivacyOptions() {
        showPrivacyOptionsCount++
    }

    /** Simulates the consent framework changing its requirement answer after launch. */
    fun emitPrivacyOptionsRequired(required: Boolean) {
        privacyOptionsState.value = required
    }
}
