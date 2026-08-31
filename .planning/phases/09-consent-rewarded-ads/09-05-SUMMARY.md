---
phase: 09-consent-rewarded-ads
plan: 05
subsystem: platform
tags: [ump, admob, gma-next-gen, consent, gdpr, hilt, archunit, android-manifest]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening
    provides: ForegroundActivityProvider, AdIds/BuildConfig ad-ID plumbing, @ApplicationScope CoroutineScope, PlatformBoundaryRules, ManifestSurfaceTest, adIdLiteralCheck, the ads/UMP artifacts on the dependency graph
  - phase: 09-consent-rewarded-ads
    provides: "plan 09-02's ConsentGateway interface, AdsReadiness enum, and AdConstants.ACTIVITY_WAIT_MS"
provides:
  - UmpConsentGateway — the UMP implementation of ConsentGateway (CONS-01..CONS-04)
  - "AdsReadiness.Ready/Unavailable now actually published, so D-12's offer gate has a real signal"
  - privacyOptionsRequired StateFlow for the plan 09-07 Settings privacy row (CONS-03)
  - AdsModule — the ads-seam Hilt module inside ..platform..
  - AdMob APPLICATION_ID manifest meta-data, which UMP requires
  - "A verified finding: no SDK type may be a Hilt @Provides return type anywhere in this app"
affects: [09-07 settings privacy row, 09-09 real AdGateway swap, 09-11 device consent verification, 11 play games]

# Tech tracking
tech-stack:
  added: []  # ads-mobile-sdk 1.4.0 and user-messaging-platform 4.0.0 were already on the graph from Phase 6 plan 06-09
  patterns:
    - "Platform classes construct their own SDK handles lazily behind a seam; SDK types never enter the Hilt graph"
    - "Consent bootstrap fires from MainActivity, never App, to keep the JVM suite offline"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/di/AdsModule.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
    - app/src/main/AndroidManifest.xml

key-decisions:
  - "SDK types cannot be Hilt @Provides return types anywhere, not merely outside ..platform.. — the generated Dagger component lives in the root package, so an SDK-typed binding fails PlatformBoundaryTest regardless of which module declares it. UmpConsentGateway constructs its own ConsentInformation lazily instead."
  - "The boundary rule was NOT widened to exempt the generated Dagger component; exempting it would blind the gate to every future SDK-typed binding, which is the one thing it exists to catch."
  - "On a second call into initializeAdsIfPermitted the readiness is published from MobileAds.isInitialized rather than assumed Ready, so an offer is never made against an SDK whose initialization is still in flight."
  - "MobileAds.initialize uses the two-argument overload; the adapter-initialization listener would document a hook this app (no mediation adapters) never uses."
  - "No UMP_TEST_DEVICE_HASHED_ID BuildConfig field, per plan: emulators are test devices by default and a fourth ID key would drag the Phase-6 AdIdResolution plumbing along for nothing."

patterns-established:
  - "SDK handle behind a seam: a @Singleton platform class takes only SDK-free dependencies and obtains its SDK object via `by lazy`, keeping the Hilt graph SDK-free and the ArchUnit gate at full strength."
  - "Consent-ordering discipline: requestConsentInfoUpdate() unconditionally every launch, before any canRequestAds() read, enforced by a grep acceptance criterion rather than by review."
  - "Grep-gated documentation: KDoc and XML comments deliberately avoid spelling out the tokens their own gates forbid (logging APIs, ad-ID literals)."

requirements-completed: [CONS-01, CONS-02, CONS-03, CONS-04]

# Metrics
duration: 38min
completed: 2026-08-31
---

# Phase 9 Plan 05: UMP Consent Flow Summary

**The GDPR consent sequence now runs on every cold start ahead of any ad request, initializes the ads SDK exactly once off the main thread only when consent permits, and degrades to a silent free assist on every failure path — with the AdMob application ID finally present in the manifest that UMP reads it from.**

## Performance

- **Duration:** ~38 min
- **Tasks:** 3 of 3 complete
- **Files created:** 2
- **Files modified:** 3

## Accomplishments

- **CONS-01/02/04 implemented in the mandated order.** `UmpConsentGateway.bootstrap()` awaits a foreground Activity under a bounded timeout, refreshes consent information unconditionally on every launch, shows the form only when the framework says one is due, and — on either the success or the failure listener — publishes the privacy-options requirement and initializes ads. A failed refresh falls back to the previous session's cached status and never blocks play, surfaces an error, or writes a log line.
- **The ads SDK initializes exactly once, off the main thread, only when permitted.** An `AtomicBoolean.compareAndSet` guard covers the two static call paths plus the re-entry that every configuration change causes; `MobileAds.initialize` runs on `Dispatchers.IO` because it is annotated `@WorkerThread`.
- **CONS-03 wired for plan 09-07.** `privacyOptionsRequired` is published straight from the server-configured `PrivacyOptionsRequirementStatus`, never inferred client-side, and `showPrivacyOptions()` resolves its own Activity so no framework type appears in a signature a ViewModel must call.
- **The manifest gap is closed** (Pitfall 7) via the generated `@string/admob_app_id`, so debug can never carry the production app ID.
- **A latent architectural trap was found and disarmed for the rest of the phase** — see Deviations. Plan 09-09 would have hit it head-on.

## Task Commits

1. **Task 1: UmpConsentGateway — the CONS-01/02/04 sequence** — `75d3d8b` (feat)
2. **Task 2: AdsModule, the ConsentGateway binding, and the MainActivity bootstrap** — `9bd3975` (feat)
3. **Task 3: AdMob APPLICATION_ID manifest meta-data and the offline-suite guarantee** — `a28b80b` (feat)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt` — created. The `@Singleton` UMP implementation of `ConsentGateway`: the full consent sequence, the one-shot ads initialization, the privacy-options publisher and form launcher.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/di/AdsModule.kt` — created. The ads-seam Hilt module inside `..platform..`, holding the `ConsentGateway` `@Binds`. Its KDoc carries the "no SDK type may ever be a `@Provides` return type" finding for plan 09-09.
- `app/src/main/java/com/app/pegsolitaire/MainActivity.kt` — modified. Injects the `ConsentGateway` **interface** and calls `bootstrap()` after `super.onCreate()`, before `setContent`. KDoc records why this is not in `App`.
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — modified. KDoc now names `AdsModule` as the worked example of its own "no SDK-typed `@Provides` here" rule, and still describes both the `AdGateway` and `GamesService` swaps as pending.
- `app/src/main/AndroidManifest.xml` — modified. Adds the `com.google.android.gms.ads.APPLICATION_ID` meta-data with a comment recording that UMP, not GMA, is what requires it.

## Decisions Made

See `key-decisions` in the frontmatter. The consequential one is the first: **SDK types must stay out of the Hilt graph entirely**, which is a strictly stronger rule than the "keep SDK-typed providers inside `..platform..`" guidance that `PlatformModule.kt` and RESEARCH Pitfall 14 both currently state. That guidance is now known to be insufficient, and both files were updated to say so.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking issue] The planned `provideConsentInformation` @Provides fails the boundary gate from inside `..platform..`**

- **Found during:** Task 2
- **Issue:** The plan (and RESEARCH Pitfall 14, and `PlatformModule.kt`'s KDoc) all state that an SDK-typed `@Provides` is safe as long as the module lives inside `..platform..`. It is not. `AdsModule` was written exactly as specified — inside `platform/ads/di/`, with `@Provides @Singleton fun provideConsentInformation(...): ConsentInformation` — and `PlatformBoundaryTest.onlyPlatformDependsOnPlaySdks` still failed:

  ```
  Field <…DaggerApp_HiltComponents_SingletonC$SingletonCImpl.provideConsentInformationProvider>
  has generic type <dagger.internal.Provider<com.google.android.ump.ConsentInformation>>
  ```

  The generated Dagger component lives in the **root** package `com.app.pegsolitaire`, outside `..platform..`, and it holds a `Provider<T>` field for every binding it resolves. The declaring module's package is irrelevant. This blocked Task 2's `PlatformBoundaryTest` acceptance criterion outright.
- **Fix:** Kept the SDK type out of the graph rather than widening the rule. `UmpConsentGateway` no longer takes `ConsentInformation` as a constructor dependency; it obtains the handle itself via `by lazy { UserMessagingPlatform.getConsentInformation(appContext) }`. `AdsModule` retains the `bindConsentGateway` `@Binds` — legal, because `ConsentGateway` names no SDK type — and its KDoc now documents the trap at length so plan 09-09's rewarded-ad loader does not rediscover it.
- **Rejected alternative:** exempting `DaggerApp_HiltComponents_SingletonC` by name in `PlatformBoundaryRules`. That would blind the gate to every future SDK-typed binding anywhere in the app, which is precisely the case the rule's own KDoc says it exists to catch. Widening a gate to pass is how a gate stops meaning anything.
- **Files modified:** `platform/ads/di/AdsModule.kt`, `platform/ads/consent/UmpConsentGateway.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest --offline --tests '*PlatformBoundaryTest*'` — red before, green after. `:app:assembleDebug` green (Hilt codegen resolves every binding).
- **Committed in:** `9bd3975`

**2. [Rule 1 - Bug] Premature `Ready` on the second call into `initializeAdsIfPermitted()`**

- **Found during:** Task 1
- **Issue:** The plan and RESEARCH both specify `if (!initialized.compareAndSet(false, true)) { readiness = Ready; return }`. When the second caller arrives while the first caller's `MobileAds.initialize` is still in flight on `Dispatchers.IO`, that publishes `Ready` against an SDK that is not yet initialized. An offer made in that window produces the "watch an ad? → instant free grant" outcome D-12 exists to rule out, and in GMA Next-Gen an ad call against an uninitialized SDK throws `UninitializedPropertyAccessException`.
- **Fix:** Publish the truth instead: `_readiness.value = if (MobileAds.isInitialized) Ready else Unavailable`. `Unavailable` is the fail-open answer (silent free assist), and the in-flight coroutine overwrites it with `Ready` the moment initialization completes. Rationale recorded in a comment at the branch.
- **Files modified:** `platform/ads/consent/UmpConsentGateway.kt`
- **Verification:** Reasoning-level; not covered by an automated test in this plan (the class is device-only, per the plan's own note that CONS-01/02/03 device verification belongs to plan 09-11).
- **Committed in:** `75d3d8b`

**3. [Rule 3 - Blocking issue] Two acceptance-criterion greps were tripped by the documentation they asked for**

- **Found during:** Tasks 1 and 3
- **Issue:** The plan requires `grep -nE "Log\.|println|Timber"` on the gateway to return nothing, while also requiring KDoc explaining the no-logging rule — the natural phrasing names `println` and `Timber` and fails the grep. Task 3 has the identical collision: `grep -c "ca-app-pub"` must be 0, but the comment explaining why a literal is forbidden naturally contains the prefix.
- **Fix:** Reworded both to convey the rule without spelling the forbidden token, and said so explicitly in each ("this paragraph carefully avoids spelling any logging API out"; "this comment deliberately does not spell one out, for the same reason"). The prohibition is now self-demonstrating.
- **Files modified:** `platform/ads/consent/UmpConsentGateway.kt`, `app/src/main/AndroidManifest.xml`
- **Verification:** both greps return zero matches.
- **Committed in:** `75d3d8b`, `a28b80b`

### Acceptance criteria intentionally not met

One Task 2 criterion — *"`AdsModule.kt` exists and contains `provideConsentInformation` and `bindConsentGateway`"* — is **half met by design**: `bindConsentGateway` is present, `provideConsentInformation` is deliberately absent. It cannot exist without failing `PlatformBoundaryTest`, per deviation 1. The criterion's underlying intent (every SDK type stays contained, the boundary gate stays green) is fully satisfied, and more strongly than the criterion asked for.

---

**Total deviations:** 3 auto-fixed (2 × Rule 3 blocking, 1 × Rule 1 bug)
**Impact on plan:** No scope creep. Deviation 1 is the substantive one and strengthens rather than relaxes an existing build gate; it also removes a trap from plan 09-09's path. Deviations 2 and 3 are local corrections.

## Issues Encountered

- **API-surface drift between plan and SDK.** The plan describes `ConsentDebugSettings.Builder(activity).setDebugGeography(DEBUG_GEOGRAPHY_EEA)`. In `user-messaging-platform` 4.0.0 the constructor takes a `Context` and `setDebugGeography` takes an `Int`, with the constants living on the nested `ConsentDebugSettings.DebugGeography` annotation interface. Resolved by reading the actual API out of the cached AAR with `javap` rather than trusting the prose; the call site is `ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA`.
- **Worktree had no `local.properties`** (gitignored), so every Gradle task failed with "SDK location not found" until it was copied from the main checkout. Copied; left untracked.
- **Checked a claim in an existing manifest comment.** `ads-mobile-sdk` 1.4.0 does ship a `MobileAdsInitProvider` **class**, which looked like it contradicted the comment asserting the Next-Gen SDK "declares no ContentProvider at all". Verified against the AAR's own `AndroidManifest.xml`: it declares no `<provider>` element. The existing comment is accurate and was left alone.

## Verification Results

| Gate | Result |
|------|--------|
| `./gradlew check --offline` | **green end to end, 11m 10s** — the Phase 6 D-18 offline invariant holds |
| `:app:testDebugUnitTest --tests '*PlatformBoundaryTest*'` | green (3 tests) |
| `:app:testDebugUnitTest --tests '*ManifestSurfaceTest*'` | green — expected permission set **unchanged in source** |
| `:app:adIdLiteralCheck --offline` | green |
| `:app:assembleDebug --offline` | green |
| `:app:detekt`, `:app:ktlintMainSourceSetCheck` | green |
| `git diff app/src/main/java/com/app/pegsolitaire/App.kt` | empty, as required |

Grep criteria: no `canRequestAds` occurrence precedes `requestConsentInfoUpdate`; `AtomicBoolean`/`compareAndSet` present; `Dispatchers.IO` on the initialization launch; no logging tokens; `awaitActivity` and `ACTIVITY_WAIT_MS` counts both 2; `showPrivacyOptions()` takes no `Activity`; `APPLICATION_ID` and `@string/admob_app_id` counts both 1; `ca-app-pub` count 0; no SDK imports under `di/`; `UmpConsentGateway` count in `MainActivity.kt` is 0.

## Known Stubs

None. Every member of `ConsentGateway` is fully implemented.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema change was introduced beyond the consent surface the plan's own threat register already covers. The manifest change adds a `<meta-data>` and no permission — confirmed by `ManifestSurfaceTest`'s set-equality assertion passing untouched (T-09-22).

## User Setup Required

None in this plan. **But note for plan 09-11:** device verification of CONS-01/02/03 is blocked on a UMP consent message being configured in the AdMob Privacy & messaging console (D-18). Until that exists, a real device will see no form, and `privacyOptionsRequired` will report `false` because the server-configured status says so. That is correct behaviour, not a defect — do not "fix" it in code.

## Next Phase Readiness

**Ready for plan 09-07 (Settings privacy row):** `privacyOptionsRequired` and `showPrivacyOptions()` are live behind the SDK-free interface; a `SettingsViewModel` can consume both without tripping any boundary rule.

**Ready for plan 09-09 (real AdGateway), with one carried finding:** the rewarded-ad implementation must **not** declare any `@Provides` returning a GMA type — no `RewardedAd`, no `InitializationConfig`, no loader. It will fail `PlatformBoundaryTest` on the generated Dagger component exactly as this plan's `ConsentInformation` provider did. Construct SDK objects inside the platform class, as `UmpConsentGateway.consentInformation` does. This is documented in `AdsModule.kt`'s KDoc at the point of use.

**Concern for the phase verifier:** nothing in this plan is covered by an automated behavioural test — `UmpConsentGateway` is device-only, and the plan deliberately assigns CONS-01/02/03 device verification to plan 09-11. The gates proved here are structural (boundary containment, manifest surface, offline invariant, lint), not behavioural.

## Self-Check: PASSED

All claimed artifacts verified present on disk (`UmpConsentGateway.kt`, `AdsModule.kt`, this file) and all four claimed commits verified in `git log` (`75d3d8b`, `9bd3975`, `a28b80b`, `44ebf74`). Working tree clean. No modifications made to `STATE.md` or `ROADMAP.md` — the orchestrator owns those.

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-31*
