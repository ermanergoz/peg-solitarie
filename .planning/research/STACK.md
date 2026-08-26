# Stack Research

**Domain:** Android game monetization + platform services (Play Games Services v2, AdMob, UMP consent) and short-SFX audio, added to a shipped Kotlin/Compose game
**Researched:** 2026-08-27
**Confidence:** HIGH

> Scope note: this document covers **only the additions** needed for v1.1 "Slice 2". The v1.0 toolchain
> (Kotlin 2.3.20, AGP 9.1.0, Gradle 9.5.1, JDK 17, Compose BOM 2026.06.00, Hilt 2.60/KSP 2.3.9, Room 2.8.4,
> DataStore 1.2.1, Navigation 2.9.8, coroutines/serialization 1.11.0, compileSdk 36 / minSdk 24 / targetSdk 36)
> is already validated and is **not** re-researched here.

---

## Headline Findings

1. **The CLAUDE.md ads pin is unsafe.** CLAUDE.md pins `play-services-ads 23.6.0`. Per Google's official
   deprecation schedule, the **v23.x line was deprecated on 2026-02-17 and sunsets 2027-06-30** ("ads at risk
   of not serving"). Shipping 23.6.0 today buys ~10 months of life. Do not use it.
2. **Next-Gen is no longer "evaluate it" — it is Google's designated preferred SDK.** On **2026-07-06** Google
   designated the **GMA Next-Gen SDK** the preferred Android ads SDK and reclassified `play-services-ads` as
   *legacy, in maintenance mode*. Next-Gen went GA at 1.0.0 (April 2026); current is **1.4.0 (2026-08-20)**.
   Official guidance: **new integrations should use Next-Gen.** This overturns the CLAUDE.md assumption that
   "the classic line remains the safe default."
3. **compileSdk 36 is safe for every SDK in this milestone — verified empirically, not from prose.** Every
   candidate AAR and every new transitive dependency was downloaded from Google Maven and its
   `aar-metadata.properties` inspected. **Nothing forces compileSdk 37.** Highest floor found is
   `minCompileSdk=34`. No `resolutionStrategy` pin is needed for any of these. (Full table below.)
4. **`play-services-games-v2` latest is 22.0.0, not ~21.x** as CLAUDE.md guessed. 22.0.0 (2026-07-29) is purely
   additive (`GameStatsClient`, `PlayerGameEvent`) — no breaking changes, no SDK-level bumps.
5. **Sound needs zero new dependencies.** `android.media.SoundPool` is a platform API and is the correct tool
   for short game SFX. Do not add Media3/ExoPlayer.
6. **Two blockers live outside the code**: this app has **no `INTERNET` permission** and **no R8/minification
   configured at all** (no `buildTypes`, no proguard files). Both must be addressed in this milestone.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **GMA Next-Gen SDK** (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`) | **1.4.0** | Rewarded ads (hint/undo/retry) + interstitials (between levels) behind `AdGateway` | Google's **designated preferred Android ads SDK since 2026-07-06**; the legacy line is explicitly in maintenance mode and receives no new features. GA since 1.0.0 (Apr 2026), 5 patch releases in. `minSdk 24` matches this project **exactly**; `compileSdk 35+` satisfied by 36. Its built-in `RewardedAdPreloader`/`InterstitialAdPreloader` (auto-preload + managed retry) is a direct fit for "hint must be ready the instant the player taps it" — with the legacy SDK you hand-roll that cache. Google cites 27% latency reduction and 17% footprint cut vs legacy. |
| **User Messaging Platform (UMP)** (`com.google.android.ump:user-messaging-platform`) | **4.0.0** | GDPR/consent gathering before any ad request | Mandatory before serving ads in consent regions. Released 2025-10-31 — ~10 months stable. Next-Gen pulls it transitively, but **declare it explicitly** so the version is catalog-governed rather than inherited. Tiny (0.39 MB, only depends on `play-services-basement`). |
| **Play Games Services v2** (`com.google.android.gms:play-services-games-v2`) | **22.0.0** | Sign-in, leaderboards, achievements, Saved Games (cloud save) behind `GamesService` | The only supported PGS line — v1 APIs are being removed. 22.0.0 (2026-07-29) is additive over 21.0.0. Remarkably light: **0.41 MB**, depends only on `play-services-base`/`-basement`/`-tasks` — no androidx pulled in at all, so zero risk to the project's carefully-pinned androidx graph. Since 21.0.0 the deprecated **Google Sign-In and Google Drive dependencies were removed**, so Saved Games needs no Drive scope. |
| **kotlinx-coroutines-play-services** (`org.jetbrains.kotlinx`) | **1.11.0** | Bridges GMS `Task<T>` → `suspend` via `.await()` | PGS is entirely `Task`-based (`isAuthenticated()`, `submitScore()`, `SnapshotsClient.open()`), but the existing seam is `suspend fun signIn(): Boolean`. This is the official adapter and its version **matches the project's coroutines pin (1.11.0) exactly** — no version skew. Without it you hand-write `suspendCancellableCoroutine` wrappers for every call. |
| **`android.media.SoundPool`** | Platform API (min API 21 builder; project minSdk 24) | Short SFX: peg select, jump, peg pop, win sting, star pop, invalid-move buzz | Purpose-built for exactly this: pre-decoded PCM samples held in memory, low latency, no per-play decode cost, built-in concurrent-stream capping. **Adds zero dependencies and zero APK weight beyond the audio assets.** The alternative (Media3) is for streams/playlists/background playback — none of which this game has. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `com.google.android.gms:play-services-ads` | **25.4.0** | Legacy Mobile Ads SDK | **Fallback only.** Use if Next-Gen proves unstable in the rewarded flow or if third-party mediation is ever required (Next-Gen supports AdMob mediation *only*). Supported until 2027-06-30, sunsets 2028-06-30. The `AdGateway` seam makes this a one-class + one-`@Binds` swap. |
| `androidx.compose.ui:ui` → `LocalHapticFeedback` | via Compose BOM (already present) | Consume the already-persisted `hapticsOn` flag | Zero new dependency. `HapticFeedbackType.LongPress`/`TextHandleMove` for peg-jump and win feedback. Pairs naturally with the sound work. |

**Explicitly NOT added:** no Media3/ExoPlayer, no Oboe/NDK audio, no Firebase, no third-party mediation adapters, no `play-services-auth` (PGS v2 removed the Google Sign-In dependency).

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| **R8 / `minifyEnabled`** | Shrink + obfuscate release builds | **Currently not configured at all** — the project has no `buildTypes` block and no proguard files. Adding a ~5.5 MB ads SDK plus a large transitive graph to an unminified release build is the single biggest APK-size risk in this milestone. Both SDKs ship consumer ProGuard rules, so enabling R8 needs no hand-written keep rules for them. Add `buildTypes { release { isMinifyEnabled = true; isShrinkResources = true } }`. |
| **AdMob test ad unit IDs** | Develop without policy risk | Never request production ad units from a dev build — it is a policy violation and can suspend the account. Interstitial `ca-app-pub-3940256099942544/1033173712`; rewarded `ca-app-pub-3940256099942544/5224354917`; test app ID `ca-app-pub-3940256099942544~3347511713`. Wire these via a build-type-scoped constant (fits the project's "no magic strings, per-scope constants objects" rule). |
| **Play Console → PGS testers list** | Test sign-in before publishing | PGS auth **fails for any account not on the testers list** until the PGS config is published. This is the #1 cause of "sign-in silently fails" during development. |
| **APK Analyzer / `bundletool`** | Verify the size delta | Take a baseline size measurement *before* adding the SDKs so the delta is attributable. |

---

## Installation

### `gradle/libs.versions.toml` additions

```toml
[versions]
# --- Slice 2: ads, consent, Play Games, audio (v1.1) ---
# GMA Next-Gen — Google's DESIGNATED PREFERRED Android ads SDK since 2026-07-06.
# The legacy `play-services-ads` line is in maintenance mode (deprecated 2027-06-30,
# sunset 2028-06-30). NOT the CLAUDE.md-pinned 23.6.0: the v23.x line was already
# deprecated 2026-02-17 and sunsets 2027-06-30. Verified: AAR declares minSdk 24
# (== project minSdk) and minCompileSdk=1, so compileSdk 36 is unaffected.
adsMobileSdk = "1.4.0"
# UMP consent. Pulled transitively by ads-mobile-sdk 1.4.0, but declared explicitly
# so the version is catalog-governed. AAR declares minSdk 23, no compileSdk floor.
ump = "4.0.0"
# Play Games Services v2 — latest (22.0.0, 2026-07-29; additive over 21.0.0:
# GameStatsClient + PlayerGameEvent). CLAUDE.md guessed ~21.x. Depends only on
# play-services-base/-basement/-tasks: pulls ZERO androidx, so it cannot disturb
# the compileSdk-36 androidx pins. minCompileSdk=1 verified.
playServicesGames = "22.0.0"

[libraries]
# --- Ads + consent (behind AdGateway) ---
ads-mobile-sdk = { group = "com.google.android.libraries.ads.mobile.sdk", name = "ads-mobile-sdk", version.ref = "adsMobileSdk" }
user-messaging-platform = { group = "com.google.android.ump", name = "user-messaging-platform", version.ref = "ump" }

# --- Play Games Services v2 (behind GamesService) ---
play-services-games-v2 = { group = "com.google.android.gms", name = "play-services-games-v2", version.ref = "playServicesGames" }

# --- GMS Task<T> -> suspend bridge. Version ref reuses the existing `coroutines`
#     pin (1.11.0) so the adapter can never skew from the coroutines runtime.
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
```

No new `[plugins]` entries. No new repositories — all four artifacts resolve from `google()`, which
`settings.gradle.kts` already declares.

### `app/build.gradle.kts` additions

```kotlin
dependencies {
    // Ads: GMA Next-Gen behind AdGateway. UMP declared explicitly (also transitive).
    implementation(libs.ads.mobile.sdk)
    implementation(libs.user.messaging.platform)

    // Play Games Services v2 behind GamesService, plus the Task -> suspend bridge.
    implementation(libs.play.services.games.v2)
    implementation(libs.kotlinx.coroutines.play.services)
}
```

Note: **audio requires no dependency line at all** — `SoundPool` is in the platform. Only the assets
(`res/raw/*.ogg`) are added.

### `AndroidManifest.xml` additions

```xml
<!-- Required: the app is currently OFFLINE and declares no permissions whatsoever.
     ads-mobile-sdk merges INTERNET + ACCESS_NETWORK_STATE + AD_ID + READ_BASIC_PHONE_STATE
     via manifest merger, but declare INTERNET explicitly for clarity. PGS needs it too. -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application ...>
    <!-- PGS v2: value is the Games services PROJECT ID from Play Console,
         NOT the AdMob app ID. Different number, common mix-up. -->
    <meta-data
        android:name="com.google.android.gms.games.APP_ID"
        android:value="@string/game_services_project_id" />

    <!-- UMP requires the AdMob app ID in the manifest even on Next-Gen
         (Next-Gen itself takes the app ID programmatically in MobileAds.initialize). -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="@string/admob_app_id" />
</application>
```

---

## compileSdk 36 Safety Verification (the hard gate)

Every candidate AAR was downloaded from `dl.google.com/dl/android/maven2` and
`META-INF/com/android/build/gradle/aar-metadata.properties` was read directly. `minCompileSdk` is the
*only* thing that can force a compileSdk bump, and AGP reads it from exactly this file.

| Artifact | Version | `minCompileSdk` | AAR minSdk | Verdict |
|----------|---------|-----------------|------------|---------|
| `ads-mobile-sdk` (Next-Gen) | 1.4.0 | **1** | 24 | Safe |
| `play-services-ads` (legacy) | 25.4.0 | *(no aar-metadata → no floor)* | — | Safe |
| `user-messaging-platform` | 4.0.0 | *(no aar-metadata → no floor)* | 23 | Safe |
| `play-services-games-v2` | 22.0.0 | **1** | 24 | Safe |
| `play-services-games-v2` | 21.0.0 | **1** | 24 | Safe |

New transitive androidx artifacts these SDKs introduce (the place a compileSdk-37 trap would actually hide):

| Transitive artifact | Resolved version | `minCompileSdk` | Introduced by |
|---------------------|------------------|-----------------|---------------|
| `androidx.browser:browser` | 1.8.0 | 34 | both ads SDKs |
| `androidx.webkit:webkit` | 1.15.0 / 1.12.1 | 33 / 34 | Next-Gen / legacy |
| `androidx.work:work-runtime` | 2.7.0 | 31 | Next-Gen |
| `androidx.privacysandbox.ads:ads-adservices` | 1.0.0-beta05 | 33 | legacy |
| `androidx.datastore:datastore` | 1.0.0 | 30 | both ads SDKs |

**Conclusion: the maximum floor across the entire new graph is `minCompileSdk=34`.** compileSdk 36 clears it
with room to spare. **No lib line in this milestone forces compileSdk 37**, and **no new
`resolutionStrategy.eachDependency` pin is required.**

Why the existing pins are also undisturbed: every androidx version requested by these SDKs
(`core` 1.10.1/1.12.0, `datastore` 1.0.0, `activity` 1.2.3, `annotation` 1.6.0) is **lower** than what the
project already resolves (`core` 1.18.0, `datastore-preferences` 1.2.1, `activity-compose` 1.13.0). Gradle
picks the highest, so the existing forced pins continue to win and nothing is dragged upward.
`androidx.lifecycle` is not requested by any of these SDKs at all — the atomic-group hazard that bit
`hilt-navigation-compose` does not recur here.

Kotlin/coroutines skew is also benign: `play-services-ads` requests `kotlin-stdlib 2.1.0` +
`kotlinx-coroutines 1.8.0`; Next-Gen requests `kotlin-stdlib 1.9.20` + `kotlinx-coroutines 1.9.0`. The project's
2.3.20 / 1.11.0 are higher and win.

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| **GMA Next-Gen `ads-mobile-sdk` 1.4.0** | `play-services-ads` **25.4.0** (legacy) | Use if (a) Next-Gen shows instability in the rewarded flow under real fill, (b) third-party mediation is ever needed — **Next-Gen supports AdMob mediation only, all other mediation platforms are incompatible**, or (c) Android TV support is needed (Next-Gen does not support Android TV). 25.4.0 is supported until 2027-06-30. Because `AdGateway` is a two-method seam, this fallback is a single class + a single `@Binds` change — budget ~half a day, not a rewrite. |
| **GMA Next-Gen 1.4.0** | `play-services-ads` **23.6.0** (the CLAUDE.md pin) | **Never.** The v23.x line is already deprecated (2026-02-17) and sunsets 2027-06-30. This pin is stale and should be corrected in CLAUDE.md. |
| **`play-services-games-v2` 22.0.0** | 21.0.0 | Only if 22.0.0's `GameStatsClient`/`PlayerGameEvent` additions somehow conflict. No known reason — 22.0.0 is additive and its dependency set is *identical* to 21.0.0 apart from a `play-services-basement` bump (18.4.0 → 18.9.0), which the ads SDK requires at 18.9.0 anyway. Using 22.0.0 keeps `basement` on one version across both SDKs. |
| **`SoundPool`** | `androidx.media3:media3-exoplayer` | Only if background music, streaming, playlists, or seek/pause is added. For fire-and-forget clips under ~2 s, Media3 is a large dependency and a heavier per-sound object graph for strictly worse latency. Not warranted by any v1.1 requirement. |
| **`SoundPool`** | `MediaPlayer` | Only for a single long looping track (e.g. background music). `MediaPlayer` decodes on play, cannot overlap instances cheaply, and has a fragile state machine — wrong for rapid overlapping peg-pop SFX. |
| **`SoundPool`** | Oboe / AAudio (NDK) | Only for rhythm games or synthesis needing sub-10 ms deterministic latency. Requires the NDK and C++, which would breach the pure-Kotlin/Compose constraint for zero benefit on UI SFX. |
| **`kotlinx-coroutines-play-services`** | Hand-rolled `suspendCancellableCoroutine` wrappers | If you object to a dependency for one function. Not recommended: you would re-implement cancellation and exception mapping for every `Task` call site, and the official adapter is version-locked to the coroutines pin you already have. |
| **Room for cloud-save payload** | PGS `SnapshotsClient` byte[] | Not either/or — Room stays the local source of truth; Saved Games is a **second data source behind the existing `ProgressRepository`**, exactly as the v1.0 architecture anticipated. Serialize the payload with the kotlinx-serialization already in the build. Snapshot cap is **3 MB**, far above this game's needs. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **`play-services-ads:23.6.0`** (CLAUDE.md's pin) | Deprecated 2026-02-17; **sunsets 2027-06-30** — "ads at risk of not serving," and the outdated-SDK designation blocks new store releases. Shipping it starts the clock at ~10 months. | `ads-mobile-sdk 1.4.0` (or `play-services-ads 25.4.0` as fallback) |
| **Any `play-services-ads` v22.x or earlier** | Already sunset. | `ads-mobile-sdk 1.4.0` |
| **Third-party mediation adapters (AppLovin, ironSource, Unity Ads…) with Next-Gen** | Officially incompatible — "other mediation platforms are not compatible with GMA Next-Gen SDK." Only AdMob mediation or no mediation works. | No mediation (fine for v1.1), or drop to legacy 25.4.0 if mediation becomes a requirement |
| **`play-services-games` (v1)** | v1 APIs are being removed from the SDK. | `play-services-games-v2:22.0.0` |
| **`play-services-auth` / Google Sign-In for PGS** | PGS v2 21.0.0 **removed** the deprecated Google Sign-In dependency; v2 authenticates automatically at launch. Adding it re-introduces a dead auth path. | `PlayGamesSdk.initialize()` + `GamesSignInClient.isAuthenticated()` |
| **Google Drive dependency for Saved Games** | Removed in PGS v2 21.0.0. No Drive scope is required in v2. | `PlayGames.getSnapshotsClient(activity)` from `play-services-games-v2` |
| **`androidx.media3` / ExoPlayer for SFX** | Streaming/playlist machinery for clips that are already fully in memory; meaningful APK cost for worse latency. | `android.media.SoundPool` |
| **Requesting ads before UMP resolves** | Next-Gen **preloads ads on `initialize()`**, so initializing before consent can fire requests before consent exists — a compliance violation in consent regions. | Gather consent → check `canRequestAds()` → *then* `MobileAds.initialize()` |
| **Calling any `MobileAds` method before `initialize()` (Next-Gen)** | Unlike legacy, initialization is **mandatory**; otherwise `UninitializedPropertyAccessException` is thrown. | Initialize on a background thread at app start (post-consent); `initialize()` on the main thread risks ANR |
| **Production ad unit IDs in debug builds** | AdMob policy violation; risks account suspension. | Google test ad unit IDs, selected per build type |
| **An unminified release build** | No `buildTypes`/R8 exists today; ~5.5 MB of ads SDK plus guava/tink/okhttp/cronet/work-runtime would ship whole. | Enable `isMinifyEnabled` + `isShrinkResources` in this milestone |
| **`SharedPreferences` for a consent/ads cache** | Already ruled out project-wide; UMP persists its own state anyway. | Existing DataStore Preferences |

---

## Integration Notes (seam-level)

These are the concrete friction points between the new SDKs and the existing v1.0 architecture.

**1. Both SDKs need an `Activity`; the seams are injected into ViewModels.** This is the main architectural
work of the milestone, not a detail.
- `AdGateway.showInterstitial()` and Next-Gen's `ad.show(activity)` need an Activity.
- `PlayGames.getGamesSignInClient(activity)` / `getSnapshotsClient(activity)` need an Activity.
- Neither existing seam signature carries one, and ViewModels must not hold Activity references (leak).
- Standard resolution: a `@Singleton ActivityProvider` holding a `WeakReference<Activity>` updated from
  `Application.ActivityLifecycleCallbacks` (or a `LocalActivity`-driven holder set from the Compose layer),
  injected into the *implementations only*. The `AdGateway`/`GamesService` interfaces stay Activity-free, so
  `:domain` purity and the ArchUnit gate are unaffected.

**2. The existing seam signatures survive mostly intact.**
- `suspend fun requestRewarded(reason: AdReason): Boolean` — maps cleanly onto
  `RewardedAdPreloader.pollAd()` + `show()` with a `suspendCancellableCoroutine` around
  `RewardedAdEventCallback` (Next-Gen's ads API is **callback-based, not suspend-based**). Return `false` on
  no-fill so the existing "not granted → skip assisted action" contract already handles the degenerate case.
- `suspend fun signIn(): Boolean` — maps onto `GamesSignInClient.isAuthenticated().await()` via
  `kotlinx-coroutines-play-services`. The v1.0 doc comment already says callers treat `false` as "Play Games
  unavailable and hide the affordance," which is *exactly* the behaviour Google's guidance demands (do not
  force manual sign-in; PGS retries automatically).
- `fun submitScore(leaderboardId: String, score: Long)` — fire-and-forget already matches `Task` semantics.
- `GamesService` will need **new methods for cloud save**; the v1.0 comment explicitly scoped Saved Games out
  of this interface and routed it behind `ProgressRepository` instead. Honour that: add a separate
  `CloudSaveDataSource`, do not bloat `GamesService`.

**3. PGS v2 sign-in is automatic.** `PlayGamesSdk.initialize(this)` goes in the existing `App.onCreate()`.
Do not build a mandatory sign-in gate; show an optional sign-in button only when `isAuthenticated()` is false.

**4. Sound implementation shape.** A `@Singleton class SoundPlayer @Inject constructor(@ApplicationContext ctx)`
built with `SoundPool.Builder()`, `AudioAttributes` `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION`, and
`maxStreams` ~4–6 (peg pops can overlap). Load all `.ogg` clips from `res/raw` once at startup and cache the
returned sound IDs — `SoundPool.load()` is async, so **playing before `OnLoadCompleteListener` fires silently
does nothing**, a classic first-SFX-is-missing bug. Gate every `play()` on the already-persisted `soundOn`
flag read from DataStore. Per project standards, sample rate/`maxStreams`/volume/priority constants belong in
a scoped constants object (e.g. `SoundConstants`), not inline literals.

**5. New permissions have Play Console consequences.** `ads-mobile-sdk` merges in `AD_ID` and
`READ_BASIC_PHONE_STATE`. The **Data safety form must be updated** to disclose advertising ID usage, and if the
app is ever targeted at children/families, `AD_ID` must be explicitly removed with a `tools:node="remove"`
override. This is a store-rejection risk, not a build risk.

---

## Google Play Console / AdMob Prerequisites (non-code, do these first)

These gate testing and cannot be done from the repo. Several have multi-hour to multi-day latency.

**Play Games Services**
1. Create the PGS project in Play Console (Grow → Play Games Services → Setup and management → Configuration).
2. Configure the **OAuth consent screen** in the linked Google Cloud project.
3. Create **OAuth credentials** with the app's **SHA-1** fingerprint — one per signing key (debug **and** upload/Play App Signing). Missing the debug SHA-1 is why local sign-in fails.
4. Create leaderboards + achievements; record their generated IDs into a scoped constants object (they are opaque strings, e.g. `CgkI…`).
5. **Enable Saved Games** in the PGS features section — off by default; cloud save silently fails without it.
6. Add **tester accounts**. PGS auth fails for non-testers until the configuration is published.
7. Publish the PGS configuration.
8. Copy the **Games services project ID** into `@string/game_services_project_id` (distinct from the AdMob app ID).

**AdMob**
1. Create/verify the AdMob account and link it to the Play Console app.
2. Register the app → obtain the **AdMob App ID** (`ca-app-pub-…~…`).
3. Create **two ad units**: one rewarded, one interstitial → obtain unit IDs (`ca-app-pub-…/…`).
4. **`app-ads.txt` — required.** Since January 2025 new AdMob apps must be verified with an `app-ads.txt` file. This needs a **developer website**, its URL entered in the Play Console store listing contact details, and the file hosted at `https://<domain>/app-ads.txt`. Then request verification in AdMob → App settings → Verify app. Crawl/verification takes **up to 24 hours**. **If no developer website exists yet, this is the long-lead item of the whole milestone — start it first.**
5. Configure a **UMP consent message / privacy message** in the AdMob UI (Privacy & messaging → GDPR + IAB TCF as applicable). The UMP SDK renders nothing if no message is configured.
6. Complete the Play Console **Data safety** declaration for advertising ID and any collected identifiers.

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| `ads-mobile-sdk 1.4.0` | compileSdk 36 ✔ (needs ≥35), minSdk 24 ✔ (needs 24 — exact match), Kotlin 2.3.20 ✔ (needs ≥1.9), AGP 9.1.0 ✔ (needs ≥7.3.1) | Bundles `user-messaging-platform 4.0.0` transitively. Requests `kotlin-stdlib 1.9.20`, `coroutines 1.9.0` — both lower than the project pins, so the project's win. Does **not** support Android TV (irrelevant here). |
| `user-messaging-platform 4.0.0` | minSdk 23 ✔, no compileSdk floor | Released 2025-10-31. Depends only on `play-services-basement 18.9.0` — same version the ads SDK wants, so no conflict. |
| `play-services-games-v2 22.0.0` | compileSdk 36 ✔, minSdk 24 ✔ (exact match) | Pulls **no androidx at all** → cannot perturb the compileSdk-36 androidx pins. Requests `play-services-basement 18.9.0`, aligning with the ads SDK (21.0.0 would request 18.4.0 and be upgraded anyway). |
| `kotlinx-coroutines-play-services 1.11.0` | coroutines 1.11.0 ✔ (project pin), Kotlin 2.3.20 ✔ | Version-ref it to the existing `coroutines` entry so the two can never drift. |
| `SoundPool` | minSdk 24 ✔ | `SoundPool.Builder` requires API 21; project floor is 24. No compatibility shim needed. |
| `play-services-ads 25.4.0` (fallback) | compileSdk 36 ✔ (needs ≥35), minSdk 24 ✔ (needs ≥23) | Deprecates 2027-06-30, sunsets 2028-06-30. Does **not** bundle UMP — must be declared explicitly. |

**Legacy ads SDK lifecycle (official):**

| Line | Status | Deprecation | Sunset |
|------|--------|-------------|--------|
| v25.x | Supported | 2027-06-30 | 2028-06-30 |
| v24.x | Supported | 2027-06-30 | 2028-06-30 |
| **v23.x (CLAUDE.md's 23.6.0)** | **Deprecated** | **2026-02-17** | **2027-06-30** |
| v22.x and earlier | Sunset | — | — |

---

## Risks & Open Questions

| Risk | Severity | Assessment / Mitigation |
|------|----------|-------------------------|
| **APK size growth** | Medium | The two ads AARs are near-identical in isolation (Next-Gen 5.54 MB vs legacy 5.56 MB), but Next-Gen's *transitive* graph is heavier: guava 32.0.0-android, tink-android, okhttp, gson, `work-runtime` (1.46 MB), cronet-api + cronet-fallback. Legacy's extra graph is only small androidx artifacts (~0.5 MB). Google claims a net **17% footprint cut**, which is plausible post-R8 but **unverified for this app**. Mitigation: enable R8 (currently absent), take a baseline size reading before integration, and measure the delta. If the shrunk delta is unacceptable, the `AdGateway` seam makes falling back to 25.4.0 cheap. |
| **Next-Gen maturity** | Medium | GA ~4 months (1.0.0 Apr 2026 → 1.4.0 Aug 2026); smaller community troubleshooting corpus than legacy's decade of Stack Overflow. Mitigation: Google maintains an official samples repo (`googleads/gma-next-gen-sdk-android-examples`); the seam bounds the blast radius; legacy 25.4.0 is a supported escape hatch until 2027-06-30. |
| **`app-ads.txt` needs a developer website** | Medium | Hard external prerequisite with up-to-24 h verification latency, and it may require registering a domain. Sequence it **first** in the roadmap so it is not discovered on release day. |
| **PGS sign-in fails in dev** | Low | Almost always missing debug SHA-1 credentials, an unpublished PGS config, or a non-tester account. All three are Play Console fixes, not code fixes — but they can burn a day if not anticipated. |
| **UMP 4.0.0 changelog not located** | Low | Google's UMP release-notes URLs 404'd during research; version and date (4.0.0, 2025-10-31) confirmed via Google Maven metadata and the official setup page's current dependency snippet, which cites 4.0.0. The four-call API surface (`getConsentInformation` → `requestConsentInfoUpdate` → `loadAndShowConsentFormIfRequired` → `canRequestAds`) is confirmed from the official setup page. **Breaking changes 3.x → 4.0.0 are unconfirmed** — but irrelevant here, since this is a greenfield integration with no 3.x code to migrate. |
| **No `INTERNET` permission today** | Low | The app is fully offline. Manifest merger from `ads-mobile-sdk` would add it implicitly, but declaring it explicitly avoids a confusing implicit-permission diff at review time. |

---

## Sources

- https://developers.google.com/admob/android/deprecation — **official** legacy SDK deprecation/sunset table (v23.x deprecated 2026-02-17 / sunset 2027-06-30; v25.x supported → 2027-06-30 / 2028-06-30); Next-Gen named the designated successor (HIGH)
- https://developers.google.com/admob/android/next-gen/quick-start — Next-Gen artifact + 1.4.0, minSdk 24, compileSdk ≥35, Kotlin ≥1.9, background-thread `MobileAds.initialize()` (HIGH)
- https://developers.google.com/admob/android/next-gen/rel-notes — Next-Gen version history; 1.0.0 GA Apr 2026, 1.4.0 released 2026-08-20; supported ad formats (HIGH)
- https://developers.google.com/admob/android/next-gen/migration — mandatory initialization, package moves, **third-party mediation incompatibility**, consent-before-initialize warning (HIGH)
- https://developers.google.com/admob/android/next-gen/sdk — Next-Gen does not support Android TV (HIGH)
- https://developers.google.com/admob/android/next-gen/interstitial + /rewarded — callback-based API, `InterstitialAdPreloader`/`RewardedAdPreloader`, test ad unit IDs (HIGH)
- https://developers.google.com/admob/android/rel-notes — legacy `play-services-ads` 25.4.0 (2026-06-17) latest; "maintenance mode" banner (HIGH)
- https://developers.google.com/admob/android/quick-start — legacy dependency, compileSdk ≥35, minSdk 23, `APPLICATION_ID` meta-data crash warning (HIGH)
- https://developers.google.com/admob/android/privacy — UMP 4.0.0 dependency and the four-call consent flow (HIGH)
- https://support.google.com/admob/answer/9363762 + https://developers.google.com/admob/android/app-ads — `app-ads.txt` requirement since Jan 2025, developer-website + Play Console store-listing steps, ≤24 h verification (HIGH)
- https://developer.android.com/games/pgs/android/android-start — PGS v2 dependency and the Play Console prerequisite list (PGS project, OAuth consent, credentials/SHA-1, testers, publish) (HIGH)
- https://developer.android.com/games/pgs/android/android-signin — automatic v2 sign-in, `PlayGamesSdk.initialize()`, `GamesSignInClient.isAuthenticated()`, no-forced-sign-in guidance (HIGH)
- https://developer.android.com/games/pgs/android/saved-games — `PlayGames.getSnapshotsClient()`, no Drive scope in v2, 3 MB snapshot cap, conflict-resolution policies, Play Console enable step (HIGH)
- https://developers.google.com/android/guides/releases — `play-services-games-v2` 21.0.0 (Drive + Google Sign-In removed) and 22.0.0 (`GameStatsClient`, `PlayerGameEvent`, additive) (HIGH)
- **Direct Google Maven artifact inspection** (`dl.google.com/dl/android/maven2`, 2026-08-27) — `maven-metadata.xml` for all five artifacts; `aar-metadata.properties` `minCompileSdk` for every AAR and new transitive; POM dependency graphs; AAR manifest permissions; AAR file sizes (**HIGH — primary-source empirical verification, the basis of the compileSdk-36 safety claim**)
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-play-services/ — 1.11.0 latest, matches the project's coroutines pin (HIGH)
- https://ppc.land/google-forces-android-developers-off-legacy-mobile-ads-sdk-by-2028/ — 2026-07-06 "preferred SDK" designation, three-stage timeline, 27% latency / 17% footprint claims (MEDIUM — trade press; the dates are independently corroborated by the official deprecation page above, the performance percentages are not)
- SoundPool guidance — Android platform reference + practitioner sources: pre-decoded low-latency samples, `USAGE_GAME` AudioAttributes, `maxStreams`, async `load()`, `release()` lifecycle (MEDIUM — well-established platform API, corroborated across multiple sources; no single authoritative 2026-dated page)

---
*Stack research for: Android game platform services (PGS v2), monetization (AdMob + UMP), and short-SFX audio*
*Researched: 2026-08-27*
