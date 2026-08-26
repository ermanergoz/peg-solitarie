# Pitfalls Research

**Domain:** Adding Play Games Services v2, AdMob + UMP, and SFX audio to an **already-shipped** Kotlin/Compose Android game
**Researched:** 2026-08-27
**Confidence:** HIGH for SDK mechanics, dependency-graph facts and Play/AdMob policy (verified against official docs plus direct inspection of the published AARs on Google Maven); MEDIUM where a mitigation is a judgement call about this codebase's structure (variant isolation, Robolectric provider behaviour).

> **Supersedes** the v1.0 pitfalls file's Slice-2 sections (old Pitfalls 13 & 14). Slice-1 pitfalls (generator determinism, solver blowup, Compose recomposition, Room migrations) remain valid and are preserved in git history at the v1.0 milestone commit — this document does **not** repeat them.

This catalogue is scoped to *this* system: `:app` (Compose, Hilt, Room + DataStore, 43 test files / 293 JVM tests, ~10 Robolectric-backed Compose tests, **no `androidTest` source set**), a pure-JVM `:domain` guarded by `DomainPurityTest`, `compileSdk 36` / `minSdk 24` / `targetSdk 36` with 37 uninstallable, detekt+ktlint+dimens gates in `check`, and two no-op Hilt seams (`AdGateway`, `GamesService`) in `PlatformModule`.

---

## Verified Baseline (read this before the pitfalls)

Facts established by fetching and unpacking the actual artifacts from `dl.google.com/dl/android/maven2` on 2026-08-27 — **HIGH confidence**:

| Fact | Value | Consequence |
|---|---|---|
| Latest `play-services-ads` | **25.4.0** (2026-06-17) | The `CLAUDE.md` pin of **23.6.0 is ~2 major versions stale** |
| Latest `play-services-games-v2` | **22.0.0** (2026-07-29) | The pin of "~21.x" is one major stale |
| Latest `user-messaging-platform` | **4.0.0** | GMA 25.0.0 bumped its UMP dependency to 4.0.0 |
| GMA legacy SDK status | **"in maintenance mode"** — Google directs new work to the **GMA Next-Gen SDK** | Decide explicitly; do not drift into it by accident |
| `minCompileSdk` across the whole Slice-2 graph | max = **34** (`androidx.browser:1.8.0`, `androidx.webkit:1.12.1`); `privacysandbox.ads` = 33; all `com.google.android.gms` AARs = 1 | **compileSdk 36 is SAFE.** No Slice-2 SDK forces 37. |
| `play-services-games-v2:22.0.0` `minSdkVersion` | **24** | Exactly the project's `minSdk 24`. Zero headroom — a future bump to 25 would be a hard block. |
| `play-services-ads:25.4.0` `minSdkVersion` | 23 | Fine |
| New transitives entering the graph | `androidx.browser 1.8.0`, `androidx.webkit 1.12.1`, `androidx.privacysandbox.ads 1.0.0-beta05`, `androidx.datastore 1.0.0`, `androidx.core 1.10.1`, `kotlin-stdlib 2.1.0`, `kotlinx-coroutines 1.8.0`, `play-services-basement 18.9.0` | A **beta** artifact lands in the release graph; `datastore` and `core` requests are *below* the project's pins |
| Manifest-merged ContentProviders | `com.google.android.gms.ads.MobileAdsInitProvider`, `com.google.android.gms.games.provider.PlayGamesInitProvider` | Both auto-run at **cold start**, before `App.onCreate` finishes |
| Manifest-merged permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_{AD_ID,ATTRIBUTION,TOPICS}` | App currently declares **zero** permissions |
| `PlayGames.get*Client(...)` signatures (javap on the 22.0.0 AAR) | **every** getter takes `android.app.Activity` — `getGamesSignInClient`, `getLeaderboardsClient`, `getAchievementsClient`, `getSnapshotsClient`, `getPlayersClient`, `getRecallClient` | There is **no** `Context` overload |
| `PlayGamesSdk.initialize(...)` | takes `android.content.Context` | Only the SDK init is Context-scoped |
| `RewardedAd.show(...)` | takes `(Activity, OnUserEarnedRewardListener)` | Same Activity requirement |
| `kotlinx-coroutines-play-services` | **1.11.0** published, matches the project's coroutines pin exactly | Use it — don't hand-roll `Task` → `suspend` bridges |

---

## Critical Pitfalls

### Pitfall 1: The existing seam signatures physically cannot host the real SDKs — no `Activity`

**What goes wrong:**
`AdGateway.requestRewarded(reason): Boolean` and `GamesService.signIn(): Boolean` / `submitScore(id, score)` take **no `Activity`**, and both are `@Singleton`-bound in `PlatformModule`. Every real call site needs one:

- `RewardedAd.show(activity, listener)` — Activity required
- `PlayGames.getGamesSignInClient(activity)` / `getLeaderboardsClient(activity)` / `getSnapshotsClient(activity)` — **verified by `javap`: no `Context` overload exists**
- `UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, callback)` — Activity required

The "swap one Hilt binding, nothing else changes" promise written into `PlatformModule`'s KDoc is **false as written**. The discovery usually happens mid-implementation, and the panic fix is one of: (a) a `@Singleton` holding a static `Activity` reference (guaranteed leak, LeakCanary-visible, and it survives config change into a destroyed Activity), (b) `applicationContext` cast to Activity (`ClassCastException`), or (c) an `AppCompatActivity.currentActivity` global.

**Why it happens:**
The Slice-1 seams were designed from the *gameplay* side ("what does the game need?") without ever compiling against the SDK. Every fullscreen Play/AdMob surface is Activity-scoped because it launches an Activity, and no amount of interface design removes that.

**How to avoid:**
- Introduce a **`CurrentActivityProvider`** — an `Application.ActivityLifecycleCallbacks` singleton holding a `WeakReference<Activity>`, updated on `onActivityResumed` and cleared on `onActivityPaused`. Register it in `App.onCreate`. This is the standard, leak-safe pattern; the project's single-Activity Compose architecture makes it trivially correct.
- **Or** change the seam signatures to accept an `Activity` and hoist activity access into the Compose layer via `LocalActivity` / `LocalContext.findActivity()`, passing it down as an event parameter. This keeps ViewModels Activity-free but pushes plumbing into the UI.
- Decide this **before** writing any SDK code. It determines the shape of every subsequent phase.
- Whichever you pick, add a detekt/ArchUnit rule forbidding `android.app.Activity` fields outside `platform/`.

**Warning signs:**
A `lateinit var activity: Activity` anywhere; `context as Activity`; `AdGateway` implementations that compile but `show()` is unreachable; LeakCanary reporting a retained `MainActivity` after rotation.

**Phase to address:** **Phase A — SDK seam & build baseline**, before any SDK is added. This is the single highest-leverage decision in the milestone.

---

### Pitfall 2: The 293-test suite dies at cold start because of auto-init ContentProviders

**What goes wrong:**
Both `play-services-ads-api` and `play-services-games-v2` declare ContentProviders in their manifests (`MobileAdsInitProvider`, `PlayGamesInitProvider` — **verified in the AAR manifests**). Manifest merge puts them in `:app`'s merged manifest. The ~10 Robolectric-backed Compose tests (`GameFlowTest`, `HomeScreenTest`, `ResultsScreenTest`, …) run against that merged manifest with `isIncludeAndroidResources = true`. When Robolectric brings up the application, the providers can be instantiated — inside a JVM with no Play services, no network, and no AdMob app ID. Result: slow, flaky, or hard-failing tests, plus network attempts from CI. The 293-green invariant is the project's main safety net, and it breaks in a way that looks unrelated to the change that caused it.

Separately, `PlayGamesInitProvider` means **automatic sign-in fires on every cold start** without you calling anything, and `MobileAdsInitProvider` runs ad-SDK bootstrap on the startup path *before* `App.onCreate` completes — measurable cold-start regression on low-end devices.

**Why it happens:**
ContentProvider auto-init is invisible: it isn't in your source, your Hilt graph, or your `Application` class. Developers assume "I didn't call `MobileAds.initialize()`, so nothing runs." Robolectric's provider handling has historically varied across versions (MEDIUM confidence on exact 4.16 behaviour — **verify empirically in the first spike**, don't assume either way).

**How to avoid:**
- **Structural (recommended):** add a `flavorDimension` — e.g. `play` vs `nosdk` — and declare the SDKs as `playImplementation(...)`. Bind the real `AdGateway`/`GamesService` in a `src/play/` Hilt module and the existing no-ops in `src/nosdk/`. Run `check`/CI against the `nosdk` variant. The SDKs are then not on the unit-test classpath *or* in the test manifest at all, and CI stays genuinely offline. This is the only mitigation that also keeps CI free of the Google Maven artifacts.
- **Tactical minimum:** in `src/debug/AndroidManifest.xml` (the source set Robolectric actually sees — the project already learned this for the Room schema assets), remove the providers:
  ```xml
  <provider android:name="com.google.android.gms.ads.MobileAdsInitProvider"
            tools:node="remove" />
  <provider android:name="com.google.android.gms.games.provider.PlayGamesInitProvider"
            tools:node="remove" />
  ```
  Then initialise both SDKs **explicitly** from `App.onCreate` on `Dispatchers.IO`, so production behaviour is deliberate and test behaviour is inert.
- Also remove `PlayGamesInitProvider` in **release** if you want sign-in to happen at a moment you choose rather than at process start.
- Guard the invariant: add a test that asserts `AdGateway`/`GamesService` resolve to the fake in tests, and a CI job that runs `check` with **no network** (`--offline` after a warm cache) so an accidental SDK call fails loudly.

**Warning signs:**
Test wall-clock jumps after adding the dependency; `IllegalStateException: Missing application ID` from a *test*; Robolectric logs mentioning `gms`; a cold-start regression in the Play Console vitals after release; a sign-in toast on app launch you never coded.

**Phase to address:** **Phase A — SDK seam & build baseline.** Prove `./gradlew check` stays green **in the same commit** that adds the dependencies.

---

### Pitfall 3: Shipping the wrong ad unit IDs in the wrong build → non-appealable account suspension

**What goes wrong:**
Two symmetric failures. (a) **Real** ad unit IDs in `debug`/CI/emulator builds: you, your testers, and your automated Compose tests generate impressions and clicks on live inventory. Google classifies this as invalid traffic. Per AdMob's own help centre, **account suspensions for invalid traffic are non-appealable**, and additional issues found during suspension can permanently disable the account. (b) **Test** ad unit IDs left in the release build: zero revenue, discovered weeks later.

For this project the risk is unusually high because rewarded ads gate **hint, undo and retry** — the three most-tapped controls in the game — and because the Robolectric Compose tests already drive those controls (`HintControlTest`, `UndoControlTest`). Wire real IDs and your CI becomes a click farm.

**Why it happens:**
One hardcoded constant, "I'll swap it before release." The project's own no-magic-strings rule pushes IDs into a constants object, which then looks safely centralised and is *never made build-type aware*.

**How to avoid:**
- IDs come from **`buildConfigField`**, never a shared constants object: real IDs on `release`, Google's test IDs (`ca-app-pub-3940256099942544/...`) on `debug`. Same for the manifest `com.google.android.gms.ads.APPLICATION_ID` — use a `manifestPlaceholders` per build type.
- Add `RequestConfiguration.Builder().setTestDeviceIds(...)` with your physical device's hashed ID, taken from logcat on first run, for any device you'll ever hold.
- Add a **release-blocking test** that asserts `BuildConfig.REWARDED_AD_UNIT_ID` does *not* start with the Google test publisher prefix in the release variant, and *does* in debug.
- Never tap your own live ads. Never ask friends/testers to "check the ads work" on a live build.
- If you adopt the flavor isolation from Pitfall 2, the `nosdk` variant additionally can't serve ads at all.

**Warning signs:**
An ad unit ID string literal in any `Constants.kt`; the AdMob dashboard showing impressions from your own device model; `manifestPlaceholders` absent from `app/build.gradle.kts`; "Ads restricted" or "invalid traffic" mail from AdMob.

**Recovery:** none for suspension. Prevention is the whole strategy.

**Phase to address:** **Phase C — UMP + AdMob**, but the `buildConfigField` plumbing lands in **Phase A** so it physically cannot be forgotten.

---

### Pitfall 4: The no-op `AdGateway` auto-grants, so no opt-in UI exists — the swap silently makes the app policy-violating

**What goes wrong:**
`NoOpAdGateway.requestRewarded()` returns `true`. That means today's hint/undo/retry UI almost certainly **calls it and proceeds immediately** — there is no "Watch an ad for a free hint? [Yes] [No]" dialog anywhere, because none was needed. Swapping in a real AdMob implementation therefore produces an app where tapping *Hint* launches a fullscreen video with **no affirmative opt-in**. AdMob's rewarded inventory policy is explicit: rewarded ads "must only be served after a user affirmatively and unambiguously opts in (such as by tapping a button that represents 'yes' or 'accept')", must not oblige the user to interact, must be skippable/dismissible, and declining "must not impede or interfere with the normal usage" of the app. That's an immediate *Rewards implementation — User choice* / *Full disclosure* violation, the two most commonly reported AdMob restrictions.

**Why it happens:**
The seam was designed to make ads invisible to gameplay. It succeeded — including making the *consent moment* invisible. The stub's `true` return is a false green: every existing test passes, and the missing dialog is only a policy problem once a real SDK is behind it.

**How to avoid:**
- Build the **opt-in dialog first, in a phase before the SDK arrives**, while the stub is still in place. It is pure Compose + ViewModel work, it is testable with the existing Robolectric harness, and it must state both the action and the reward ("Watch a short ad to get a hint?").
- Decline path must be first-class: the player who taps *No* must land back exactly where they were with the game fully usable. Since Slice 1 shipped hints as free, consider keeping **N free hints per level** so the ad is genuinely optional — this also satisfies "must not impede normal usage" by construction.
- Grant the reward **only** in `OnUserEarnedRewardListener`, never in `onAdDismissedFullScreenContent` and never optimistically before `show()`.
- Because `requestRewarded` returns `Boolean`, make **`false` mean "not granted"** and audit every call site for correct handling — the stub never returned `false`, so those branches have literally never executed. Add tests that force `false`.

**Warning signs:**
No dialog composable in `ui/game` for ads; call sites that ignore the return value of `requestRewarded`; `AdReason` used only for logging; zero tests exercising a `false` grant.

**Phase to address:** **Phase B/C boundary** — ship the opt-in UI + `false`-path handling against the *stub*, then swap the SDK in behind it.

---

### Pitfall 5: UMP sequencing errors — requesting ads before consent, or shipping without a privacy entry point

**What goes wrong:**
Four distinct sequencing errors, all common:
1. Calling `MobileAds.initialize()` and immediately `RewardedAd.load()` without ever checking `canRequestAds()`. In EEA/UK this serves personalised ads without consent — a GDPR and Google EU User Consent Policy breach.
2. Checking `canRequestAds()` **before** `requestConsentInfoUpdate()`. It **always returns `false` until `requestConsentInfoUpdate()` has been called** — so the check silently disables ads everywhere, and the "fix" is usually to delete the check.
3. Initialising the Mobile Ads SDK twice — once eagerly and once in the consent callback — producing duplicate ad requests. Google's own sample uses an `AtomicBoolean` guard for exactly this.
4. Not implementing `getPrivacyOptionsRequirementStatus()` → no "Privacy options" entry point. Google requires a persistent, user-reachable way to change consent when required. This project *has* a Settings screen (`ui/settings/SettingsScreen.kt`), so the omission is pure oversight, and it is a common cause of app rejection/ad restriction.
5. Inferring consent by reading TCF strings out of SharedPreferences/DataStore instead of calling `requestConsentInfoUpdate()` on every launch — Google's docs warn this "can lead to a TCF 3.3 error if consent is expired".

**Why it happens:**
`MobileAds.initialize()` is policy-safe to call before the form (it processes no personal data), which developers over-generalise into "ads are safe before consent". They aren't — the boundary is at the **ad request**, not initialisation.

**How to avoid:**
Fixed order, every launch, no exceptions:
```
requestConsentInfoUpdate()          // ALWAYS, every app launch
  └─> loadAndShowConsentFormIfRequired(activity) { formError ->
        if (canRequestAds()) initializeMobileAdsOnce()   // AtomicBoolean guard
      }
// plus, in parallel, immediately after requestConsentInfoUpdate:
if (canRequestAds()) initializeMobileAdsOnce()           // same guard
```
- Model this as a `ConsentGateway` with a `StateFlow<ConsentState>` that `AdGateway` **must** observe; `AdGateway.requestRewarded` returns `false` unless consent state permits. That makes "no ad request before consent" a type-level invariant rather than a code-review promise.
- Add a `PrivacyOptions` row to `SettingsScreen`, visibility driven by `getPrivacyOptionsRequirementStatus() == REQUIRED`, calling `showPrivacyOptionsForm(activity)`.
- Test with `ConsentDebugSettings` + `DEBUG_GEOGRAPHY_EEA` and a hashed test device ID; use `consentInformation.reset()` between runs to replay first-install. Emulators are test devices by default (UMP ≥ 2.2.0).
- Pin **UMP 4.0.0** to match GMA 25.x's declared dependency.

**Warning signs:**
`canRequestAds()` called at the top of `onCreate`; no `AtomicBoolean` around init; no privacy row in Settings; consent state read from DataStore; consent tested only outside the EEA.

**Phase to address:** **Phase C — UMP + AdMob.** UMP is implemented **first**, ads second.

---

### Pitfall 6: Interstitial cadence — this game's retry loop is a policy landmine

**What goes wrong:**
"Interstitials between levels" sounds compliant. In practice, peg solitaire play is **retry-dense**: a player who fails a Challenge level taps *Restart* five times in two minutes. If the interstitial hook fires on level end (including failed levels) or on restart, you produce back-to-back fullscreen ads during the most frustrating part of the game. AdMob's disallowed-implementations policy specifically prohibits: interstitials on app load and app exit; more than one interstitial per **two** user actions (back-button presses count); and an interstitial "immediately after another interstitial ad was shown to and closed by the user". It also prohibits ads that "appear suddenly while users are engaged in tasks like gaming".

Add accidental clicks: the Results screen's celebratory CTA (the Candy-Clay "Next Level" action) is a big, primed tap target. If an interstitial lands there under latency, the player's queued tap hits the ad. Accidental clicks are explicitly counted as invalid traffic.

**Why it happens:**
The `AdGateway.showInterstitial()` seam has **no cadence parameter and no state** — it is a fire-and-forget method, so the natural implementation is "call it at level end". All frequency logic has to be invented at swap time.

**How to avoid:**
- Put cadence in the **repository layer with persisted state**, not in the SDK wrapper: minimum N completed levels **and** minimum M minutes since the last interstitial, reset across process death via DataStore (the project already has DataStore for scalars).
- **Never** show an interstitial after a *failed* level or a restart — only after a *won* level, and not on every win.
- **Never** on app open (use App Open ads if you want that surface, which is a separate format with its own rules) and never on exit/back.
- Enforce a **latency guard**: pre-load the interstitial, and if it isn't already loaded at the decision point, skip it entirely rather than showing it late over the next screen.
- Add a **tap-quiescence delay** before any fullscreen ad on the Results screen (~500ms after the last user tap) so queued taps can't land on the ad.
- Unit-test the cadence policy as pure logic — it's a perfect fit for the existing JVM test style, and it means the rule is verified without the SDK.

**Warning signs:**
`showInterstitial()` called in a `LaunchedEffect` on the Results route; no persisted "last interstitial at" timestamp; ad shown on restart; AdMob "ad serving limited" notice; a spike in CTR that looks too good.

**Phase to address:** **Phase C — UMP + AdMob.** Design the cadence policy as domain-ish pure logic during **Phase A** so it can be tested before the SDK exists.

---

### Pitfall 7: Play Console setup ordering — publish the app before PGS and every non-tester breaks

**What goes wrong:**
Play Games Services has **four independent publish/approval switches**, and getting the order wrong produces sign-in failures that look like code bugs:

1. **OAuth consent screen (Google Cloud) not published** → non-tester accounts get 404/OAuth errors. Google's doc: *"Before publishing the app in the Play Console, publish the consent screen in Google Cloud. Without this step, the public audience won't be able to use any of the Play Games Services features."*
2. **PGS settings not published before the app** → Google's doc: *"At release, publish the Play Games Services settings first before you publish your game… This might cause players who are authenticating in with non-tester accounts to encounter errors."*
3. **You are not a tester.** *"The account that created the game in the Play Console does not automatically become a tester."* You will debug a working integration for a day.
4. **OAuth client created directly in Google Cloud instead of through Play Console** → no association with your achievements/leaderboards.

**Why it happens:**
The Play Console UI does not enforce the ordering, and the failure mode (`ERROR_NOT_AUTHORIZED`, silent `isAuthenticated() == false`) is identical to a code bug. Under Gitflow, PGS validation is also easy to defer to `release/*`, which is exactly when the ordering mistake becomes expensive.

**How to avoid:**
- Write the ordering into the phase plan as an explicit, ticked checklist: Cloud project + Games API → PGS project in Play Console → configure **and publish** the OAuth consent screen (scopes `games`, `games_lite`, `drive.appdata`) → create the Android credential **through Play Console** → add game properties (display name minimum) → **add yourself and testers** (allow a couple of hours to propagate) → **Publish Game (PGS)** → publish app.
- Verify `@string/game_services_project_id` is the **numeric project ID only**. Using the full `…apps.googleusercontent.com` client ID is a documented error.
- Do the whole console setup in **Phase A/D kickoff**, not at release. Sign-in cannot be validated locally without it.

**Warning signs:**
`isAuthenticated()` false on every account; sign-in works for you but not a second Gmail; PGS status "Ready to publish"; app ID string containing dots.

**Phase to address:** **Phase D — Play Games v2**, front-loaded as a prerequisite task.

---

### Pitfall 8: SHA-1 registered is the **upload** key, not the Play **app signing** key

**What goes wrong:**
The classic PGS failure: everything works on your machine, then the internal-testing build downloaded from Play **cannot sign in** (`ERROR_NOT_AUTHORIZED`). With Play App Signing enabled, Google re-signs your AAB with the *app signing key*, which is a different certificate from your *upload key*. The OAuth client must carry the **app signing** SHA-1 (Play Console → App integrity), not the upload certificate.

The mirror-image failure: you register only the **release** SHA-1, so local `debug` builds can never authenticate — and since this project has **no `androidTest` source set**, your only manual verification path is a debug build on a device.

**Why it happens:**
Play Console shows both fingerprints on nearby pages, the labels are similar, and the error surfaces only after a Play-distributed install.

**How to avoid:**
- Register **two Android credentials, same package name**: one with the Play **app signing** SHA-1 (release) and one with `~/.android/debug.keystore`'s SHA-1 (`keytool -list -keystore ~/.android/debug.keystore -v`, alias `androiddebugkey`). This is the documented best practice.
- If CI or a second developer machine builds debug APKs, their debug keystores have *different* SHA-1s — either share one debug keystore in the repo (it's not a secret) or register each.
- Verify the shipped artifact: `unzip app.apk && keytool -printcert -file META-INF/CERT.RSA` and compare.
- Test sign-in from an **internal testing track download**, not just a local install, before merging `release/*` to `main`.

**Warning signs:**
Sign-in OK on `adb install`, fails from Play; only one credential listed in PGS; the SHA-1 in PGS matches "Upload certificate" on the App integrity page.

**Phase to address:** **Phase D — Play Games v2**, and again as a release-gate item in the final phase.

---

### Pitfall 9: Writing PGS v2 as if it were v1 (sign-in buttons, sign-out, `GoogleSignIn`)

**What goes wrong:**
Most tutorials, Stack Overflow answers, and model training data predate v2. Code written that way won't even compile against `play-services-games-v2` (`GoogleSignInClient`, `Games.getXxxClient()`, `GamesClient`, `GamesMetadataClient`, `GoogleApiClient` are all removed), and the *design* is also wrong: **v2 authenticates automatically at launch, and there is no sign-out.** Games that ship a "Sign out" button in Settings simply cannot implement it. Games that ship an always-visible "Sign in" button show it to already-signed-in players.

Two more v1-era assumptions that bite:
- The v1 SDK relied on Google Sign-In, which was removed from `play-services-auth`. Anything still on v1 has an unresolvable dependency; `com.google.android.gms:play-services-games:25.0.0` removed the v1 API entirely.
- **Login quota**: automatic sign-in at every launch multiplies your login requests versus v1's opt-in flow. Google's migration doc explicitly says to review quota.

The current `GamesService.signIn(): Boolean` seam models an *interactive, user-triggered* sign-in — a v1 shape. In v2 the right model is closer to `observeAuthState(): StateFlow<Boolean>` plus an optional `promptSignIn()` for the manual retry case.

**How to avoid:**
- Flow: `PlayGamesSdk.initialize(context)` (or let `PlayGamesInitProvider` do it) → `PlayGames.getGamesSignInClient(activity).isAuthenticated()` → if `false`, **then** show an in-game "Sign in to Play Games" affordance calling `signIn()`.
- Remove any sign-out concept from the Settings design before it's built.
- Reshape the seam now: `signIn()` stays for the manual retry, but add an authentication-state flow so the UI doesn't have to call a suspend function to render a badge.
- Use `kotlinx-coroutines-play-services:1.11.0` `.await()` on the returned `Task<T>` instead of hand-rolling `suspendCancellableCoroutine` (double-resume and leaked continuations are the standard bugs there).
- Beware the docs' **anonymous-listener warning**: the Play Games SDK holds some listeners as **weak references**, so anonymous lambdas can be GC'd before invocation. Hold strong references in your `GamesService` implementation.

**Warning signs:**
`import com.google.android.gms.auth.api.signin.*`; a "Sign out" row in Settings; `signIn()` called unconditionally at startup; `Task` bridged with a hand-written `suspendCoroutine`.

**Phase to address:** **Phase D — Play Games v2.**

---

### Pitfall 10: Saved Games conflict handling that quietly deletes a player's stars

**What goes wrong:**
`SnapshotsClient.open()` returns `DataOrConflict<Snapshot>`. The path of least resistance is `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED`, which — per Google's own wording — "blindly chooses the most recent data, [so] it is possible that a player's changes may get lost." For this game the cloud blob is *the entire progress model*: per-level stars, best times, best scores, and `highestLevelReached`. A player who plays offline on a tablet and then opens the phone can lose weeks of progress in one sync. Worse: the first sign-in on a device where the player already has local progress can be treated as "cloud wins", wiping a fully-populated local Room DB.

Two more specific traps:
- `RESOLUTION_POLICY_HIGHEST_PROGRESS` / `LONGEST_PLAYTIME` are *not* semantically correct here — "longest playtime" is not "most stars".
- Snapshots have a **max data size** (`getSnapshotsClient(activity).getMaxDataSize()`). A naive dump of the whole Room DB including in-progress board snapshots and move histories can approach it as the Challenge ladder grows.

**Why it happens:**
Conflicts essentially never fire during single-device development, so the branch is written blind and never executed until real users have two devices.

**How to avoid:**
- Use **`RESOLUTION_POLICY_MANUAL`** and implement a **field-wise merge**, because this domain has a natural join: `highestLevelReached = max(local, remote)`; per `(mode, levelId)` take `max(stars)`, `min(bestTimeMs)`, `max(bestScore)`. Union of level keys. This is a **pure function over two progress snapshots** — put it in `:domain` and unit-test it exhaustively with the existing JVM harness. That is the single most valuable test in this phase.
- **Never** sync the `InProgressGame` resume snapshot to the cloud. It's device-local, it's the largest blob, and merging it is meaningless. Sync only results + ladder position.
- Local Room stays the **single source of truth**; cloud is a second data source behind `ProgressRepository`, exactly as the v1.0 architecture already anticipated. A signed-out or offline player is never blocked.
- Sync at coarse checkpoints (level complete, app foreground/background), never per move.
- Version the cloud blob with a **format version byte** and `generatorVersion`, mirroring the local persistence discipline.
- Test the conflict path deliberately: two emulators, same account, both offline, both play, both come online.

**Warning signs:**
`open(name, true)` with a resolution policy argument and no `isConflict()` branch; merge logic living in `:app`; the resume snapshot in the cloud payload; no size check against `getMaxDataSize()`; no two-device test in the plan.

**Phase to address:** **Phase E — Cloud save.** Merge function lands in `:domain` *first*, tested, before any Snapshots code is written.

---

### Pitfall 11: SoundPool plays silence — the first tap of every session

**What goes wrong:**
`SoundPool.load()` is **asynchronous**. Call `play()` before decoding finishes and it returns `0` and produces no sound. The player's very first peg tap after cold start is silent; every subsequent one works. It reads as "the sound is broken sometimes" and is nearly impossible to reproduce on a warm device.

Related failures specific to this game:
- **Loading on the main thread.** Decoding several short WAV/OGG files during `App.onCreate` or the first composition adds jank exactly where the Candy-Clay entry animation runs.
- **`maxStreams` exhaustion.** Peg solitaire is a tap-spam game. With a low `maxStreams`, SoundPool "will automatically stop a previously playing stream based first on priority and then by age" — so rapid successive jumps clip each other. With too high a value you waste memory.
- **Leaking across Activity recreation.** A `SoundPool` created in a composable, `remember`ed without disposal, or owned by the Activity, leaks native audio resources on every rotation/theme change. `release()` makes the pool permanently unusable — *"The SoundPool can no longer be used and the reference should be set to null"* — so a shared instance released by one screen breaks every other screen.
- **No audio focus / no lifecycle pause.** SFX keep firing over an incoming call or the user's music, or play from a backgrounded app. `autoPause()`/`autoResume()` exist precisely for this and are usually never called.
- **Ignoring the persisted `soundOn` flag at the wrong layer** — checking it in the composable instead of the player, so it's easy to add a new call site that bypasses the setting.
- **Ad audio collision.** A rewarded video's audio and your SFX overlapping is jarring and reads as a bug.

**How to avoid:**
- One `@Singleton` `SoundPlayer` in the Hilt graph, constructed with `@ApplicationContext`, holding one `SoundPool` built with `AudioAttributes` `USAGE_GAME` / `CONTENT_TYPE_SONIFICATION`. Never in a composable, never Activity-scoped.
- Load all clips **once, on `Dispatchers.IO`**, into a `Map<SoundId, Int>`; maintain a `Set<Int>` of ready sample IDs populated from `setOnLoadCompleteListener`. `play()` becomes a no-op (not a crash, not a queue) for any sample not yet ready. Pre-warm during the Home screen so the first gameplay tap is always ready.
- Gate on the persisted `soundOn` flag **inside** `SoundPlayer` (collecting `SettingsRepository`'s Flow), so no call site can bypass it.
- `maxStreams` sized to the realistic concurrency of the board (a jump + a peg-pop + a star-pop ≈ 4–6); set sensible per-sound `priority`.
- Request transient audio focus (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) or, at minimum, call `autoPause()` on `ON_STOP` and `autoResume()` on `ON_START` via a `DefaultLifecycleObserver` on `ProcessLifecycleOwner`.
- Mute SFX for the duration of a fullscreen ad (`onAdShowedFullScreenContent` → `autoPause()`, `onAdDismissedFullScreenContent` → `autoResume()`).
- Test it: `SoundPlayer` is Android-dependent, so assert it via Robolectric (`ShadowSoundPool`) or, better, keep the *decision* logic (which sound for which event, mute state) in a pure class that JVM-tests, and keep the `SoundPool` wrapper trivially thin.

**Warning signs:**
`SoundPool(...)` inside a `@Composable` or `Activity`; `play()` immediately after `load()`; no `OnLoadCompleteListener`; `release()` called from `onDestroy` of a rotating Activity; SFX audible while a rewarded ad plays; sound continues after backgrounding.

**Phase to address:** **Phase B — Sound**, first SDK-free phase of the milestone (lowest risk, highest player-visible payoff, and it exercises the new "platform singleton" pattern before the SDKs arrive).

---

### Pitfall 12: The dependency graph silently regresses the compileSdk-36 pinning strategy

**What goes wrong:**
The project survives on `compileSdk 36` only because of two blunt `resolutionStrategy.eachDependency` `useVersion` forces (`androidx.core` → 1.18.0, all of `androidx.lifecycle` → 2.9.4). The Slice-2 SDKs drag in **new, unforced** androidx groups (verified from the POMs): `androidx.browser 1.8.0`, `androidx.webkit 1.12.1`, `androidx.privacysandbox.ads 1.0.0-beta05`, `androidx.datastore 1.0.0`, plus requests for `androidx.core 1.10.1`, `kotlin-stdlib 2.1.0` and `kotlinx-coroutines 1.8.0`.

**Good news, verified:** none of these currently requires more than `compileSdk 34`, so **compileSdk 36 holds today**. The pitfall is that (a) nothing *guards* that, and (b) a routine version bump of ads or games can introduce a `browser`/`webkit` line whose `minCompileSdk` is 37, and the failure surfaces as an opaque AAR-metadata error deep in a variant task.

Secondary hazards, all real:
- `androidx.privacysandbox.ads:1.0.0-beta05` — a **beta** artifact in your release graph, unpinned and invisible in `libs.versions.toml`. That violates the catalog's stated "single source of truth for every pinned version" rule.
- `androidx.datastore` is requested at **1.0.0** while the project uses `datastore-preferences 1.2.1`. Conflict resolution upgrades, but if the group is treated atomically a mixed graph is possible — worth an explicit force.
- `play-services-basement 18.9.0` is a shared floor across ads, games-v2 **and** UMP; bumping one artifact without the others can create a basement version skew, the classic gms `NoSuchMethodError` at runtime that never shows at compile time.
- The `eachDependency` blunt-force approach *downgrades* as well as upgrades. Forcing `androidx.core` to 1.18.0 when a new SDK requires 1.19.0+ would produce a runtime `NoSuchMethodError` with no build warning.

**How to avoid:**
- Add every new transitive group to `libs.versions.toml` and add explicit `resolutionStrategy` entries (or, better, `constraints { }` which upgrade-only and won't silently downgrade).
- Add a **build-gate task** that runs `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` and fails if any resolved artifact's `minCompileSdk` exceeds 36 — or, cheaply, snapshot the resolved dependency list into a committed fixture and diff it in CI. That turns "a bump broke compileSdk" from a surprise into a red build with a readable diff.
- Enable **Gradle dependency verification / dependency locking** for the release classpath so a beta artifact can't change under you.
- Bump `play-services-ads`, `play-services-games-v2` and `user-messaging-platform` **together**, and pin `play-services-basement` explicitly.
- **Resolve the stale pins:** `CLAUDE.md` says ads 23.6.0 / games ~21.x. Current are 25.4.0 and 22.0.0. Note that `play-services-games-v2:22.0.0` has `minSdkVersion 24` — exactly this project's floor, zero headroom.
- **Decide on GMA Next-Gen explicitly.** The classic `play-services-ads` line is documented as *"in maintenance mode"*. Staying on it for v1.1 is defensible (stable, well-documented, 25.4.0 is 2 months old); drifting onto it without a decision record is not.

**Warning signs:**
`Dependency 'x' requires libraries and applications that depend on it to compile against version 37 or later`; a `-beta`/`-alpha` artifact in the release dependency report; `NoSuchMethodError` in a gms class at runtime; the catalog no longer describing the full graph.

**Phase to address:** **Phase A — SDK seam & build baseline.** Add the dependency-graph gate in the same commit as the first SDK.

---

### Pitfall 13: New permissions and data collection break the Play Console declarations (and possibly Families policy)

**What goes wrong:**
The app currently declares **zero permissions** and collects nothing. Adding the ad SDK merges in `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `com.google.android.gms.permission.AD_ID` and three `ACCESS_ADSERVICES_*` permissions (verified in the `play-services-ads-api` manifest). Consequences that are easy to miss:

- The **Data safety** form must now declare collection of the **Advertising ID** and device/app identifiers. An inaccurate Data safety declaration is a Play policy violation and a common cause of release rejection.
- The **"Contains ads"** declaration in Play Console becomes mandatory. Missing it is a policy violation.
- If the app's target audience is set to include children, **Families Policy** applies: `AD_ID` cannot be used, ad content must be age-appropriate, and neutral age screening may be required. A casual peg-solitaire game is very likely to be classified as appealing to children — decide the target-audience answer deliberately.
- `RECEIVE_BOOT_COMPLETED` appearing in the store's permission list on an offline puzzle game generates suspicious-looking reviews. It can be removed via `tools:node="remove"` if you accept the (minor) functionality loss — evaluate rather than accept silently.
- **`android:allowBackup="true"`** is currently set. With cloud save arriving, auto-backup restoring a stale Room DB onto a device that also syncs Snapshots is a third conflicting source of truth. Decide the interaction explicitly (usually: exclude the Room DB from auto-backup once Snapshots is authoritative, or keep backup and make the merge idempotent).

**How to avoid:**
- Treat the Play Console declarations as a **checklist item in the release phase**, with the merged manifest (`app/build/outputs/logs/manifest-merger-*-report.txt`) as evidence.
- Audit the merged manifest **in the same PR** that adds each SDK; diff it against the previous release.
- Answer the target-audience/Families questions **before** picking ad formats, since it constrains them.
- Add a `backup_rules.xml` / `data_extraction_rules.xml` decision alongside the cloud-save phase.

**Warning signs:**
No manifest-merger diff in the SDK PR; Data safety form untouched since v1.0; "Contains ads" unchecked; target audience never explicitly answered.

**Phase to address:** **Phase F — Release readiness**, with the manifest audit happening in Phases C/D/E as each SDK lands.

---

### Pitfall 14: SDK integration erodes the quality gates the project depends on

**What goes wrong:**
Four gates that currently pass will start fighting the SDK code, and the fastest fix for each is a suppression:

1. **`DomainPurityTest` has a blind spot.** It forbids `android..` and `androidx..` in `com.app.pegsolitaire.domain` — but **not `com.google.android.gms..` or `com.google.android.ump..`**. Nothing stops a `LeaderboardId` constant carrying a gms import, or an `AdReason`→SDK mapping landing in `:domain`. The gate would stay green while purity is lost. (`:domain` is a separate pure-JVM module so it wouldn't *compile* — but any decision to move the merge/cadence logic into `:app` would silently pass.)
2. **detekt complexity.** AdMob and PGS are callback-oriented (`RewardedAdLoadCallback`, `FullScreenContentCallback`, `OnCompleteListener`, `DataOrConflict` branching). Naive translation produces deeply-nested object expressions that trip `LongMethod`, `TooManyFunctions`, `NestedBlockDepth` and `ComplexCondition`. The reflex is `@Suppress`.
3. **No-magic-strings.** Leaderboard IDs, achievement IDs, snapshot names, consent form keys and ad unit IDs are all strings. They must go into scoped constants objects (per `CLAUDE.md`) — **except** ad unit IDs and the AdMob app ID, which must be `buildConfigField`/`manifestPlaceholders` (Pitfall 3). Getting this backwards produces either a lint failure or a shipped test ID.
4. **No hardcoded dp/sp.** New UI — the rewarded opt-in dialog, the consent-required banner, the privacy-options settings row, the Play Games badge/sign-in affordance — must use the dimens resource file. Easy to forget on "small" additions.

**How to avoid:**
- **Extend `DomainPurityTest` in Phase A**, before any SDK arrives: add `"com.google.android.gms..", "com.google.android.ump.."` to the forbidden packages, and add a **second rule** asserting nothing outside `com.app.pegsolitaire.platform..` depends on those packages. That makes the seam architecturally enforced rather than conventional — and it is the cheapest possible guard.
- Wrap every SDK callback at the boundary with `kotlinx-coroutines-play-services` `.await()` / `suspendCancellableCoroutine`, so the rest of the codebase sees flat suspend functions and detekt sees short methods.
- Add `LeaderboardIds` / `AchievementIds` / `SnapshotNames` / `AdCadence` constants objects under `platform/`, scoped per feature as the standards require.
- Run `./gradlew check` on **every** SDK commit, not at phase end. A gate that goes red mid-phase is cheap; a gate suppressed to unblock a merge is permanent debt.

**Warning signs:**
Any new `@Suppress` in `platform/`; a detekt baseline file appearing; string literals for IDs; `.dp` literals in new dialogs; `DomainPurityTest` unchanged at the end of the milestone.

**Phase to address:** **Phase A — SDK seam & build baseline** (extend the gates first), enforced continuously thereafter.

---

### Pitfall 15: Ad SDK on the cold-start path — ANRs and a vitals regression on a previously-instant game

**What goes wrong:**
v1.0 is a fully offline game with no SDKs; cold start is essentially Compose + Room. Adding the ad SDK on the startup path is the single most common cause of ANR regressions in casual games. `MobileAdsInitProvider` runs before `App.onCreate` completes; `MobileAds.initialize()` on the main thread does disk and network work; and the first `RewardedAd.load()` can block if issued during startup. Play Console's Android vitals ANR threshold is a **distribution-affecting** metric, so this is not merely cosmetic.

There is a genuine, easily-missed **tension in Google's own guidance**: `MobileAds.initialize()` should be called on a **background thread**, but the rewarded/interstitial docs say *"Make all calls to the Google Mobile Ads SDK on the main thread."* Both are true — init is the exception; load/show are not.

**How to avoid:**
- `MobileAds.initialize()` from `App.onCreate` inside `CoroutineScope(Dispatchers.IO).launch { }` — **not** on the main thread, and **not** blocking `onCreate`.
- Keep `OPTIMIZE_INITIALIZATION` and `OPTIMIZE_AD_LOADING` enabled (default `true` since GMA 24.0.0; verify they weren't disabled by a merged manifest).
- Do **all** `load()`/`show()`/callback work on the **main** thread.
- Do not pre-load any ad during startup. Pre-load the rewarded ad when the player enters gameplay; pre-load the interstitial when a level is nearly complete.
- Remember rewarded ads **expire ~1 hour** after load and can be shown **once** — a long peg-solitaire session will hold a stale ad. Track load time, reload on expiry, and null the reference in `onAdDismissedFullScreenContent`.
- Consider `play-services-ads-lite` if you don't need the full artifact (smaller, fewer merged components).
- Measure: take a **macrobenchmark cold-start baseline before** the SDK lands, and compare after. Without the baseline you can't tell a regression from noise.
- Ship LeakCanary in debug (already recommended in the stack doc) to catch the Activity retention that ad/PGS callbacks cause.

**Warning signs:**
`MobileAds.initialize(this)` directly in `onCreate`; `runBlocking` anywhere near ad code; ANR cluster in `MobileAdsInitProvider`/`ApplicationInfo`; the same `RewardedAd` object shown twice; "ad failed to show" with a full-hour-old load.

**Phase to address:** **Phase C — UMP + AdMob**; cold-start baseline captured in **Phase A**.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Static/`@Singleton`-held `Activity` for SDK calls | Preserves the existing seam signatures unchanged | Guaranteed Activity leak; destroyed-Activity crashes on rotation (Pitfall 1) | **Never.** Use a `WeakReference` lifecycle provider |
| SDKs added to `:app` with no variant isolation | One-line Gradle change | SDKs on the unit-test classpath forever; CI needs network; Robolectric fragility (Pitfall 2) | Acceptable **only** with `tools:node="remove"` on both providers in `debug` **and** `@TestInstallIn` fakes for every test |
| Ad unit IDs in a shared constants object | Satisfies no-magic-strings; feels centralised | Real IDs in debug builds → invalid traffic → non-appealable ban (Pitfall 3) | **Never.** `buildConfigField` per build type |
| Real ads wired before the opt-in dialog exists | Faster to a demo | Ships a policy-violating rewarded implementation (Pitfall 4) | **Never.** Dialog first, against the stub |
| `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED` for Snapshots | No conflict code to write | Silent, unrecoverable player progress loss (Pitfall 10) | **Never** for progress data |
| `SoundPool` owned by a composable/Activity | Simple, local | Native leak per config change; `release()` breaks other screens (Pitfall 11) | Never — use an app-scoped singleton |
| `@Suppress` on detekt findings in `platform/` | Unblocks the merge | The gate stops meaning anything; suppressions never get removed (Pitfall 14) | Only with an inline TODO + a tracked follow-up in the same milestone |
| Deferring Play Console/PGS setup to `release/*` | Keeps feature branches moving | Sign-in cannot be validated at all until release; ordering mistakes surface at the worst time (Pitfalls 7, 8) | Never — console setup is a Phase-D **prerequisite** |
| Keeping `CLAUDE.md`'s ads 23.6.0 / games 21.x pins | No re-verification work | Two majors behind; misses the UMP 4.0.0 pairing and the Next-Gen decision (Pitfall 12) | Only with a written decision record explaining why |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| PGS v2 client access | Passing `applicationContext` / a `Context` | **Every** `PlayGames.get*Client()` takes `android.app.Activity` (verified by `javap`); only `PlayGamesSdk.initialize()` takes `Context` |
| PGS v2 sign-in | Building a sign-in/sign-out button pair like v1 | Auto sign-in at launch; query `isAuthenticated()`; offer manual `signIn()` only when false; **no sign-out exists** |
| PGS listeners | Anonymous lambdas for `ImageManager`/callbacks | SDK holds some listeners as **weak references** — hold a strong ref on a persistent object |
| PGS app ID | Putting the full `…apps.googleusercontent.com` client ID in `@string/game_services_project_id` | **Numeric project ID only** — the full client ID is a documented cause of errors |
| PGS credentials | One credential with the release SHA-1 | **Two** credentials, same package name: Play **app signing** SHA-1 + `debug.keystore` SHA-1 |
| PGS publish order | Publish the app, then PGS | Publish OAuth consent screen → publish PGS settings → publish app. Add yourself as a tester. |
| Snapshots | `MOST_RECENTLY_MODIFIED`, or ignoring `isConflict()` | `RESOLUTION_POLICY_MANUAL` + a pure field-wise merge in `:domain`; check `getMaxDataSize()` |
| UMP | `canRequestAds()` checked before `requestConsentInfoUpdate()` | It returns `false` until `requestConsentInfoUpdate()` runs — call it **every** launch, first |
| UMP | No privacy-options entry point | `getPrivacyOptionsRequirementStatus() == REQUIRED` → visible row in the existing `SettingsScreen` → `showPrivacyOptionsForm(activity)` |
| UMP | Reading TCF strings from prefs to infer consent | Google warns this causes TCF 3.3 errors on expiry — always use the SDK |
| GMA init | `MobileAds.initialize()` on the main thread | Background thread; keep `OPTIMIZE_*` flags on; **all other** GMA calls on the main thread |
| GMA manifest | Omitting `com.google.android.gms.ads.APPLICATION_ID` meta-data | Hard crash: *"Missing application ID"*. Use `manifestPlaceholders` per build type |
| GMA rewarded | Reusing a `RewardedAd` object; showing a >1h-old ad | One show per object; null it in `onAdDismissedFullScreenContent`; reload on expiry |
| GMA interstitial | Showing on app open, on exit, on restart, or back-to-back | Between *won* levels only; ≤1 per two user actions; frequency-capped in persisted state |
| `Task<T>` → coroutines | Hand-rolled `suspendCoroutine` wrappers | `kotlinx-coroutines-play-services:1.11.0` `.await()` — version matches the project pin exactly |
| SoundPool | `play()` right after `load()` | Track ready IDs via `setOnLoadCompleteListener`; `play()` is a no-op until ready |
| Manifest merge | Not reading the merger report | Diff `manifest-merger-*-report.txt` in every SDK PR — permissions and providers arrive invisibly |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| `MobileAdsInitProvider` / `PlayGamesInitProvider` on the startup path | Cold-start p50/p90 regression; ANR cluster before `App.onCreate` | Remove providers via `tools:node="remove"`; init explicitly on `Dispatchers.IO` | Immediately on low-end/older devices; visible in Android vitals within days of rollout |
| `MobileAds.initialize()` on the main thread | ANRs at launch | Background-thread init; keep `OPTIMIZE_INITIALIZATION` on | Under cold start on a slow network |
| Pre-loading an ad during startup | Startup jank + wasted request | Pre-load on gameplay entry / near level end | First launch on a cold cache |
| Stale rewarded ad (>1h) | "Ad not ready" on a long session; wasted fill | Track load timestamp, reload on expiry, null after show | Long single-session play — common in puzzle games |
| SoundPool `maxStreams` too low | Rapid taps clip earlier SFX | Size to real concurrency (~4–6); set per-sound priority | Fast tap sequences / chained jumps |
| SoundPool loading on main thread | Jank on first gameplay entry | Load on `Dispatchers.IO`, pre-warm on Home | Every cold start |
| Cloud sync per move | Battery, quota, latency | Sync at level complete + foreground/background only | As soon as a player is signed in |
| PGS login quota from automatic sign-in | Quota errors at scale | Review PGS quota in Cloud Console; don't force extra interactive sign-ins | Only at meaningful install volume — but the doc explicitly warns about it |

## Security / Policy Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Real ad unit IDs in debug/CI builds | **Non-appealable** AdMob account suspension for invalid traffic | `buildConfigField` per build type + a test asserting the debug ID is a Google test ID |
| Tapping your own live ads to "check it works" | Same as above | Test IDs everywhere; `setTestDeviceIds` for physical devices |
| Serving ads before `canRequestAds()` | GDPR / EU User Consent Policy breach; ad restriction | `ConsentGateway` state gates `AdGateway`; test with `DEBUG_GEOGRAPHY_EEA` |
| Data safety form not updated for `AD_ID` | Play policy violation; release rejection | Update the form in the release phase using the manifest-merger report as evidence |
| "Contains ads" not declared | Play policy violation | Declare in Play Console before the release rollout |
| Target-audience question answered casually | Families Policy applies retroactively; `AD_ID` becomes prohibited | Decide target audience **before** choosing ad formats |
| Trusting the client's score for leaderboards | Spoofable rankings | PGS leaderboards are server-side; keep the score formula deterministic and recomputable; accept that a local-first single-player game has limited anti-cheat |
| Cloud save as the authoritative source | One bad merge wipes progress irrecoverably | Local Room stays SSOT; cloud is a synced second source; merge is `max`-wise and idempotent |
| Auto-backup + cloud save both restoring | Three-way conflicting state | Decide `allowBackup` / `data_extraction_rules.xml` explicitly alongside Snapshots |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Rewarded ad launches with no opt-in prompt | Ambush; 1-star reviews; **AdMob policy violation** | Explicit yes/no dialog stating action + reward; grant only on the reward callback |
| Declining an ad blocks the hint entirely | "Pay-to-play" feel; violates "must not impede normal usage" | Keep N free hints per level; ads buy *extra*, not baseline |
| Interstitial after a failed level or restart | Punishes failure; drives churn | Only after a **won** level, frequency-capped |
| Interstitial lands on the Results CTA under latency | Accidental clicks → invalid traffic **and** user anger | Pre-load only; skip if not ready; add a tap-quiescence delay |
| Silent first sound effect of every session | "The sound is broken" | Pre-warm SoundPool on Home; no-op until loaded |
| SFX playing over a rewarded video | Reads as a bug | `autoPause()` on `onAdShowedFullScreenContent` |
| A "Sign out of Play Games" row in Settings | Doesn't exist in v2; dead or crashing control | Show sign-in state only; offer manual sign-in when unauthenticated |
| Gating gameplay behind Play Games sign-in | Excludes offline and privacy-declining players | Fully playable signed-out; PGS is purely additive |
| Progress lost after installing on a second device | Rage-uninstall, 1-star review | Manual conflict resolution with a `max`-wise merge |
| No privacy-options entry point in EEA | Users cannot revoke consent; policy exposure | Conditional row in the existing Settings screen |

## "Looks Done But Isn't" Checklist

- [ ] **Rewarded ads:** Ad plays and reward grants — verify there is an **opt-in dialog with a working decline**, the decline path is tested, and the reward fires only from `OnUserEarnedRewardListener` (Pitfall 4).
- [ ] **Ad unit IDs:** Ads render — verify the **debug** variant uses Google test IDs and the **release** variant does not, enforced by a test (Pitfall 3).
- [ ] **UMP consent:** Form appears — verify `requestConsentInfoUpdate()` runs on **every** launch, `canRequestAds()` gates every ad request, init is guarded by an `AtomicBoolean`, and the **privacy-options row** exists in Settings (Pitfall 5).
- [ ] **Interstitials:** They show between levels — verify the frequency cap survives **process death** (persisted), that failed levels/restarts never trigger one, and that an unloaded ad is skipped rather than shown late (Pitfall 6).
- [ ] **PGS sign-in:** Works on your device — verify it works on a **second Google account** and from an **internal-testing track download** (Play app signing SHA-1), not just `adb install` (Pitfalls 7, 8).
- [ ] **PGS console:** Configured — verify the OAuth consent screen is **published**, PGS settings are **published before** the app, and you are on the tester list (Pitfall 7).
- [ ] **Cloud save:** Syncs — verify the **two-device conflict** path with both devices offline-then-online, and that the merge takes max stars / min time / max level rather than one side (Pitfall 10).
- [ ] **Cloud save:** Payload — verify the in-progress resume snapshot is **excluded** and the blob is under `getMaxDataSize()` (Pitfall 10).
- [ ] **Sound:** Plays — verify the **first** SFX after cold start, that `soundOn = false` silences **every** call site, and that SFX pause on backgrounding and during ads (Pitfall 11).
- [ ] **Tests:** 293 green — verify they are green **with the SDKs on the graph**, with **no network** available, and that neither init ContentProvider runs in Robolectric (Pitfall 2).
- [ ] **Gates:** `check` passes — verify `DomainPurityTest` was **extended** to forbid `com.google.android.gms..`/`com.google.android.ump..` outside `platform/`, and that no new `@Suppress` or detekt baseline was added (Pitfall 14).
- [ ] **Build:** Compiles — verify `compileSdk` is still **36**, no `-alpha`/`-beta` artifact is in the release runtime classpath unpinned, and the resolved-dependency fixture was reviewed (Pitfall 12).
- [ ] **Manifest:** Merges — verify the **manifest-merger report** was diffed and every new permission is accounted for in the Play Console **Data safety** form and the **Contains ads** declaration (Pitfall 13).
- [ ] **Startup:** Feels fine — verify against a **macrobenchmark cold-start baseline captured before the SDKs landed**, not by feel (Pitfall 15).

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| AdMob invalid-traffic suspension (P3) | **CATASTROPHIC** | Suspensions are non-appealable; earnings are refunded to advertisers; further issues can permanently disable the account. There is no recovery — only prevention. |
| Rewarded policy violation flagged (P4) | HIGH | Add the opt-in dialog + decline path, request review, expect ad-serving limits meanwhile |
| Cloud-save data loss in the wild (P10) | HIGH | Only recoverable if a local copy survives on one device; ship the merge fix, then a one-time "restore from this device" prompt |
| Seam redesign forced mid-phase (P1) | MEDIUM | Introduce `CurrentActivityProvider`, update `PlatformModule` and call sites; contained if caught in Phase A, expensive once three phases depend on the old shape |
| Robolectric suite broken by SDKs (P2) | LOW–MEDIUM | `tools:node="remove"` on both providers in `src/debug`; add `@TestInstallIn` fakes; escalate to flavor isolation if flakiness persists |
| compileSdk 37 forced by a transitive (P12) | MEDIUM | Pin the offending artifact down via `constraints`; if impossible, pin the SDK to its last 36-compatible version and record the decision |
| Cold-start/ANR regression shipped (P15) | MEDIUM | Move init off the main thread, remove init providers, halt rollout and ship a hotfix off `main` per Gitflow |
| PGS sign-in broken only in production (P8) | LOW | Add the app-signing SHA-1 credential in Play Console; propagates in hours; no app update required |
| Silent-first-sound (P11) | LOW | Add `OnLoadCompleteListener` gating + Home-screen pre-warm |

## Pitfall-to-Phase Mapping

Suggested phase structure for v1.1, ordered so each phase's prerequisites are prevented by the one before it.

| # | Pitfall | Prevention Phase | Verification |
|---|---------|------------------|--------------|
| 1 | Seams have no `Activity` | **A — SDK seam & build baseline** | A `CurrentActivityProvider` (or hoisted-Activity seam) exists and is unit/Robolectric-tested; LeakCanary clean across rotation |
| 2 | Auto-init providers break tests & startup | **A** | `./gradlew check` green **offline** in the same commit that adds the SDK deps; no gms in Robolectric logs |
| 12 | Dependency graph / compileSdk 36 ceiling | **A** | Resolved-dependency fixture committed and diffed in CI; `compileSdk` still 36; version-catalog covers every new group |
| 14 | Quality gates erode | **A** | `DomainPurityTest` extended to `com.google.android.gms..`/`com.google.android.ump..` + a "gms only in `platform/`" rule; zero new suppressions at milestone end |
| 3 | Wrong ad unit IDs per build type | **A** (plumbing) / **C** (values) | Release-blocking test asserting debug uses Google test IDs |
| 11 | SoundPool failure modes | **B — Sound** | First-SFX-after-cold-start manual check; `soundOn=false` silences all paths; pause during ads and backgrounding |
| 4 | No opt-in UI for rewarded | **B/C boundary** (build against the stub) | Opt-in dialog + `false`-grant path tested with the existing Robolectric harness, **before** AdMob lands |
| 5 | UMP sequencing | **C — UMP + AdMob** (UMP first) | `DEBUG_GEOGRAPHY_EEA` run with `reset()`; privacy row visible; ad request provably blocked pre-consent |
| 6 | Interstitial cadence / accidental clicks | **C** | Cadence policy unit-tested as pure logic; persisted across process death; never on fail/restart |
| 15 | Cold start / ANR | **C** (baseline captured in **A**) | Macrobenchmark before/after; no ANR cluster in vitals after staged rollout |
| 7 | Play Console PGS ordering | **D — Play Games v2** (prerequisite task) | Second Google account signs in; PGS status "Published" before app publish |
| 8 | SHA-1 upload vs app signing key | **D** + **F** | Sign-in verified from an internal-testing **track download** |
| 9 | v1-shaped sign-in code | **D** | No `com.google.android.gms.auth.api.signin` imports; no sign-out control; `isAuthenticated()`-driven UI |
| 10 | Saved Games conflict merge | **E — Cloud save** | Pure merge function in `:domain` with exhaustive JVM tests; two-device offline→online conflict exercised |
| 13 | Permissions / Data safety / Families | **F — Release readiness** (audited per SDK in C/D/E) | Manifest-merger diff reviewed per PR; Data safety + Contains-ads declarations updated before rollout |

**Ordering rationale:** Phase A is a pure-infrastructure phase that adds **no** user-visible feature — resist the temptation to skip it. Six of the fifteen pitfalls are prevented there, and three of them (seam shape, provider isolation, gate extension) become progressively more expensive with every subsequent phase built on the wrong foundation. Phase B (sound) is deliberately second: it is SDK-free, player-visible, and exercises the new app-scoped-platform-singleton pattern on low stakes before AdMob depends on it.

**Research flags:** Phase C (UMP + AdMob) and Phase E (cloud save) both warrant phase-specific research at execution time — C because ad policy interpretation is nuanced and the Next-Gen SDK decision may have moved, E because the merge semantics need to be specified against the actual Room schema.

## Sources

**Verified by direct artifact inspection (HIGH)** — POMs, AAR manifests and `javap` on `dl.google.com/dl/android/maven2`, 2026-08-27:
- `play-services-ads` maven-metadata (latest 25.4.0), `play-services-ads/25.4.0/*.pom`
- `play-services-ads-api/25.4.0` `AndroidManifest.xml` — `MobileAdsInitProvider`, merged permissions, `OPTIMIZE_*` flags
- `play-services-games-v2` maven-metadata (latest 22.0.0), POM, `AndroidManifest.xml` (`PlayGamesInitProvider`, `minSdkVersion 24`), and `javap` on `PlayGames` / `PlayGamesSdk` / `SnapshotsClient`
- `user-messaging-platform/4.0.0` POM + manifest
- `aar-metadata.properties` `minCompileSdk` for `androidx.browser:1.8.0` (34), `androidx.webkit:1.12.1` (34), `androidx.privacysandbox.ads:1.0.0-beta05` (33), all gms AARs (1)
- `kotlinx-coroutines-play-services` maven-metadata (1.11.0 available)

**Play Games Services (HIGH):**
- Migrate to Play Games Services v2 — https://developer.android.com/games/pgs/android/migrate-to-v2
- Migration overview (v1 / Google Sign-In removal) — https://developer.android.com/games/pgs/migration_overview
- Set up Play Games Services in Play Console (publish ordering, testers, credentials) — https://developer.android.com/games/pgs/console/setup
- Troubleshooting PGS (SHA-1, app ID, testers, weak-reference listeners) — https://developer.android.com/games/pgs/android/troubleshooting
- Saved games / conflict resolution — https://developer.android.com/games/pgs/android/saved-games and https://developers.google.com/android/games_v1/reference/com/google/android/gms/games/snapshot/Snapshots
- Use Play App Signing / client authentication — https://support.google.com/googleplay/android-developer/answer/9842756 and https://developers.google.com/android/guides/client-auth

**AdMob / UMP (HIGH):**
- GMA Android release notes (25.4.0; maintenance mode; Next-Gen) — https://developers.google.com/admob/android/rel-notes
- GMA quick start (compileSdk ≥35, minSdk ≥23, APPLICATION_ID crash) — https://developers.google.com/admob/android/quick-start
- Rewarded ads implementation (`show(Activity, …)`, one-show, ~1h expiry, main-thread rule) — https://developers.google.com/admob/android/rewarded
- Optimize initialization and ad loading (background init, `OPTIMIZE_*`) — https://developers.google.com/ad-manager/mobile-ads-sdk/android/optimize-initialization
- Set up UMP SDK (sequence, `canRequestAds()`, privacy options, debug geography) — https://developers.google.com/admob/android/privacy
- Disallowed interstitial implementations — https://support.google.com/admob/answer/6201362
- Policies for ad units that offer rewards — https://support.google.com/admob/answer/7313578
- Invalid traffic / suspension (non-appealable) — https://support.google.com/admob/answer/6213019 and https://support.google.com/admob/answer/3342099

**Audio (MEDIUM–HIGH):**
- `SoundPool` reference semantics (`play()` returns 0 on failure; `release()` finality; stream stealing by priority then age; `autoPause`/`autoResume`) — https://developer.android.com/reference/android/media/SoundPool

**Testing (MEDIUM):**
- Hilt testing guide / `@TestInstallIn` — https://developer.android.com/training/dependency-injection/hilt-testing and https://dagger.dev/hilt/testing.html
- Robolectric ContentProvider handling — https://github.com/robolectric/robolectric/issues/8461 (behaviour in 4.16 must be verified empirically, not assumed)

**Project context:**
- `.planning/PROJECT.md`, `CLAUDE.md`
- Inspected in-repo: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/src/main/kotlin/android-app.gradle.kts`, `app/src/main/AndroidManifest.xml`, `platform/ads/AdGateway.kt`, `platform/games/GamesService.kt`, `di/PlatformModule.kt`, `DomainPurityTest.kt`

---
*Pitfalls research for: adding Play Games v2, AdMob/UMP and SFX to a shipped Compose Android game*
*Researched: 2026-08-27*
