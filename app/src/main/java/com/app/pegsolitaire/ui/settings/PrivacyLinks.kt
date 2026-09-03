package com.app.pegsolitaire.ui.settings

/**
 * The privacy-related web links Settings can open (REL-05, D-13/D-15/D-19).
 *
 * Scoped, not global: this object owns privacy links and nothing else, exactly as
 * [com.app.pegsolitaire.audio.SoundParams] owns audio tuning and
 * [com.app.pegsolitaire.ui.theme.ClayAnim] owns motion timings. A URL is a string, not a
 * dp/sp measurement, so `dimens.xml` is the wrong home for it; CLAUDE.md's "no magic
 * strings in logic/UI, split by scope" rule is what puts it here rather than inline at the
 * `ACTION_VIEW` callsite.
 *
 * ## Why a plain `const val` and not a `buildConfigField`
 *
 * The URL is public — it ships in the APK, is printed on the Play listing, and is meant to
 * be read by anyone — so there is nothing to keep out of source control, and it does not
 * vary per build type (D-19 confirms RESEARCH Assumption A6: one static URL for debug and
 * release alike). `AdIds`' build-config plumbing exists because test and production ad-unit
 * IDs genuinely differ and the real ones must stay untracked; neither reason applies here,
 * and routing an invariant public string through `BuildConfig` would only add indirection.
 *
 * ## Why the value must stay a compile-time constant (security)
 *
 * [POLICY_URL] is handed to an implicit `Intent(ACTION_VIEW, …)`, which hands control to
 * whatever app claims the scheme. **The URI is the entire attack surface of that hand-off**
 * (threat T-09-27, ASVS V12): a value read from DataStore, a navigation argument, a deep
 * link, a server response or a remote-config flag could be replaced by an attacker-chosen
 * destination, turning the Settings row into an intent-redirection / phishing vector aimed
 * at a screen the player has every reason to trust. Baking it in at compile time makes that
 * class of attack unreachable rather than merely unlikely.
 *
 * So: this value is a `const val` and must remain one. Do not make it a `var`, do not add a
 * setter, do not add an overload that takes a caller-supplied URL, and do not source any
 * future link here from storage or the network.
 */
object PrivacyLinks {
    /**
     * The live privacy policy (REL-05, D-19), served directly over HTTPS with no redirect
     * and no login.
     *
     * The Play-registered `ermanergoz.github.io` address 301-redirects here; this canonical
     * `ermanergoz.com` form is the one compiled in, so a tap does not spend a redirect hop.
     * The policy text backing this URL is what Phase 14's Data-safety declaration
     * (REL-01/REL-04) must be written from — if the app's data practices change, the policy
     * changes with them, not this constant.
     */
    const val POLICY_URL = "https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html"
}
