package com.app.pegsolitaire.di

import com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdMobAdGateway
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.NoOpGamesService
import com.app.pegsolitaire.platform.games.PlayGamesService
import com.app.pegsolitaire.platform.games.save.CloudSaveStore
import com.app.pegsolitaire.platform.games.save.NoOpCloudSaveStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the platform seam (spec §8.4/§8.5). Mirrors `DataModule`'s
 * `@InstallIn(SingletonComponent::class)` + abstract interface-to-impl binding shape.
 *
 * Four bindings, of three different kinds:
 *
 * - [AdGateway] and [GamesService] are the **Slice-2 swap points**: replacing a no-op stub
 *   with a real implementation introduces rewarded ads or leaderboards with no gameplay or
 *   UI change (D-11). **Both halves are now delivered.** [AdGateway] binds
 *   [AdMobAdGateway], the Google Mobile Ads Next-Gen rewarded implementation; [GamesService]
 *   binds [PlayGamesService], the Play Games Services v2 sign-in implementation (Phase 11).
 *   Each swap was one line here, because everything above each seam had already been built
 *   and tested against the stub — which is the entire return on having written the stubs
 *   first.
 * - [ForegroundActivityProvider] → [DefaultForegroundActivityProvider] is **not** a swap
 *   point. It is permanent: the Slice-2 implementations are precisely the callers that
 *   need a foreground Activity, so this binding is what makes the swap possible rather
 *   than something the swap replaces.
 * - [CloudSaveStore] → [NoOpCloudSaveStore] is the third kind: a **temporary binding, live only
 *   within Phase 13**. Plan 13-06 declared the seam and bound the stub so that wave 3's cloud
 *   push/pull could be written and tested green before any `SnapshotsClient` plumbing existed;
 *   plan 13-09 replaces this one line with `PlayGamesCloudSaveStore`. Cloud save is not shipping
 *   disabled — a reader of an intermediate commit is looking at scaffolding, not a decision.
 *
 * **[NoOpAdGateway] stays in the source tree on purpose, unbound.** It is the seam's
 * reference behaviour — the definition of "the player is never worse off than v1.0" — and
 * `NoOpAdGatewayTest` still pins its contract, which is what keeps the fail-open promise
 * that [AdMobAdGateway] must honour written down somewhere executable. Deleting it because
 * "nothing uses it" would delete the specification along with the code.
 *
 * **[NoOpGamesService] stays in the source tree, unbound, for the same reason
 * [NoOpAdGateway] does.** It declines sign-in (`false`) so nothing renders as though Play
 * Games were connected — the opposite direction from the ad stub, which granted, and for
 * the opposite reason: a missing ad costs the player an assist, a missing leaderboard costs
 * them nothing. `NoOpGamesServiceTest` still pins that contract, which is what keeps the
 * seam's reference behaviour written down somewhere executable.
 *
 * **Do not add an SDK-typed `@Provides` to this file — or to any other.** The original
 * form of this warning said such a provider merely belongs in a module inside
 * `..platform..`, and Phase 9 proved that insufficient. Hilt's generated
 * `DaggerApp_HiltComponents_SingletonC` lives in this *root* package and carries a
 * `Provider<T>` field for every binding it resolves, so an SDK-typed binding fails
 * `PlatformBoundaryTest`'s SDK-containment rule **regardless of which module declares it**
 * — with a confusing violation naming the generated class rather than the file that caused
 * it. Verified empirically in plan 09-05: the provider was moved inside `..platform..`
 * first and the gate stayed red.
 *
 * The working pattern is therefore to keep SDK types out of the graph entirely: a platform
 * class constructs its own SDK handle behind its own seam, and only the SDK-free seam
 * interface is bound. [com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway] does
 * this with `ConsentInformation`; [AdMobAdGateway] needed no SDK-typed binding at all,
 * because its `RewardedAd` never leaves
 * [com.app.pegsolitaire.platform.ads.RewardedAdSlot]. Accordingly
 * [com.app.pegsolitaire.platform.ads.di.AdsModule] holds `@Binds` of interfaces and
 * nothing else. The fix is never to exempt the generated component from the rule; that
 * would blind the gate to every future SDK-typed binding, which is the one thing it exists
 * to catch.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    /**
     * The ad seam, bound to the real rewarded implementation (ADS-01/03/05, CONS-02).
     *
     * This is the whole of the Slice-2 ad swap: one parameter type. Everything the player
     * experiences — the opt-in dialog, the free allowance in front of it, the fail-open
     * grant behind it — was built and proven against [NoOpAdGateway] first, so changing
     * this line changes which ad plays and nothing else.
     *
     * Legal under the SDK-containment rule because both types named here are interfaces or
     * plain platform classes: [AdMobAdGateway] holds Google Mobile Ads types internally but
     * exposes none, so the generated Hilt component sees no SDK type. See the class KDoc
     * above before adding anything to this file.
     */
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: AdMobAdGateway): AdGateway

    /**
     * The Play Games seam, bound to the real sign-in implementation (PGS-01/02/03).
     *
     * **Unconditional, for every build type — no flavor split.** The milestone
     * `ARCHITECTURE.md` sketch speculated about a `play`/`nosdk` flavor dimension to keep
     * the real SDK off the test graph; [AdMobAdGateway] then proved that unnecessary.
     * `PLAT-07` holds because **no JVM test constructs the real Hilt component at all** —
     * `PlatformBoundaryRules` names `DaggerApp_HiltComponents_SingletonC` for a bytecode
     * rule, never to instantiate it — so which implementation is bound cannot affect the
     * offline suite. A flavor dimension would be real build-graph complexity buying a
     * property the codebase already has for free.
     *
     * D-04 — no Play Console project exists yet — constrains this to "not verified on a
     * real device", **not** "not bindable". Binding the stub in debug would mean the code
     * path that actually ships is the one no developer ever runs.
     *
     * Legal under the SDK-containment rule for the same reason [bindAdGateway] is: both
     * types named here are SDK-free. [PlayGamesService] constructs its `GamesSignInClient`
     * and `PlayersClient` per call from an Activity and stores neither, so the generated
     * Hilt component sees no `com.google.android.gms` type. See the class KDoc above before
     * adding anything to this file.
     */
    @Binds
    @Singleton
    abstract fun bindGamesService(impl: PlayGamesService): GamesService

    /**
     * The cloud-save seam (CLOUD-01/05), bound to the silently-degrading stub.
     *
     * **Temporary within Phase 13, and deliberately so.** [NoOpCloudSaveStore] reports "no payload"
     * and "nothing was written" so that the wave-3 push/pull pass compiles and runs green against a
     * real graph before the Saved Games plumbing exists; plan 13-09 changes this parameter type to
     * `PlayGamesCloudSaveStore` and nothing else. Read this binding as scaffolding mid-phase, never
     * as "cloud save shipped disabled".
     *
     * **The bound type must always be the seam — never a gms type, and never a type that exposes
     * one.** [CloudSaveStore] names only Kotlin/JVM types precisely so that the generated
     * `DaggerApp_HiltComponents_SingletonC`, which carries a `Provider<T>` field per binding and
     * which `onlyPlatformDependsOnPlaySdks` inspects at the bytecode level, sees nothing from
     * `com.google.android.gms..`. Plan 13-09's real implementation will hold its `SnapshotsClient`
     * internally and expose none, the way [PlayGamesService] and [AdMobAdGateway] already do. See
     * the class KDoc above before adding anything to this file.
     */
    @Binds
    @Singleton
    abstract fun bindCloudSaveStore(impl: NoOpCloudSaveStore): CloudSaveStore

    @Binds
    @Singleton
    abstract fun bindForegroundActivityProvider(impl: DefaultForegroundActivityProvider): ForegroundActivityProvider
}
