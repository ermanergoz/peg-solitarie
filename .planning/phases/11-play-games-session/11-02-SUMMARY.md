---
phase: 11-play-games-session
plan: 02
subsystem: platform
tags: [play-games, hilt, coroutines, sdk-integration, opt-in-gate, kotlin]

# Dependency graph
requires:
  - phase: 11-play-games-session
    plan: 01
    provides: "GamesService.bootstrapIfOptedIn() / signIn() contracts, GamesSignInState.SignedIn.displayName, PlayGamesOptInRepository"
  - phase: 06-platform-foundation-build-hardening
    provides: "ForegroundActivityProvider Activity bridge, play-services-games-v2 22.0.0 on the graph, game_services_project_id resValue, PlayGamesInitProvider removal, PlatformBoundaryRules"
  - phase: 09-consent-rewarded-ads
    provides: "UmpConsentGateway — the @Singleton-on-class / bounded-Activity-wait / never-log shape PlayGamesService copies; MainActivity's bootstrap call site"
provides:
  - "PlayGamesService — the real, SDK-backed GamesService (PGS-01/02/03)"
  - "GamesConstants.ACTIVITY_WAIT_MS — the games-scoped foreground-Activity bound"
  - "The live GamesService binding in PlatformModule (NoOpGamesService is now unbound)"
  - "MainActivity.gamesService.bootstrapIfOptedIn() — the launch-time, opt-in-gated entry point"
  - "kotlinx-coroutines-play-services on the graph — the Task<T> -> suspend bridge"
  - "com.google.android.gms.games.APP_ID manifest meta-data, wiring the Phase-6 game_services_project_id resource"
affects: [11-03-home-opt-in-prompt, 11-04-settings-play-games-section, 12-leaderboards-achievements]

# Tech tracking
tech-stack:
  added:
    - "org.jetbrains.kotlinx:kotlinx-coroutines-play-services (version-ref'd to the existing coroutines 1.11.0 pin)"
  patterns:
    - "Opt-in flag read as the outermost gate, before any SDK touch — proven negatively by asserting the Activity is never even awaited"
    - "Every branch of an async state resolver assigns the state flow explicitly; a bare early return is treated as the bug it is"
    - "Identity fetched separately from display name so a name-lookup failure can degrade the name without losing the player"
    - "Play SDK statics mocked with mockkStatic so call ORDER relative to a gate is observable in a plain JVM test"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt

key-decisions:
  - "Player identity comes from PlayersClient.getCurrentPlayerId(), separately from the name via getCurrentPlayer() — the plan's resolveSignedInState(activity, playerId) signature is only satisfiable this way, and it is what makes D-14's degraded name expressible at all"
  - "A failed player-ID lookup degrades to SignedOut (nothing to build SignedIn from), while a failed display-name lookup degrades to SignedIn(playerId, null) — the two failures are deliberately NOT the same outcome"
  - "signIn() returns whether the session actually resolved to SignedIn, not merely whether the SDK reported isAuthenticated — so it can never return true against a SignedOut session"
  - "NoOpGamesService kept in the tree and its import kept in PlatformModule: it is still the seam's executable reference behaviour (the NoOpAdGateway precedent), not dead code"
  - "The real binding is unconditional for every build type — no play/nosdk flavor split, matching AdMobAdGateway (RESEARCH A3)"

patterns-established:
  - "Negative proof of a gate: assert the guarded collaborator was never called, not just that the happy path works — the violating implementation differs by one `if`'s position and is otherwise identical"
  - "Resolve-don't-return: in an async state machine feeding a UI that hides itself on Unknown, every exit publishes a resolved state"
  - "Split identity from decoration: fetch the minimum that makes a state constructible first, then best-effort the rest"

requirements-completed: []

# Metrics
duration: 28min
completed: 2026-09-04
---

# Phase 11 Plan 02: PlayGamesService — the Real Games Seam Implementation Summary

**Play Games Services v2 sign-in is now live behind the seam: `PlayGamesService` is the bound `GamesService`, it reads the opt-in flag before it touches anything Google-owned, and every branch it can take resolves `session` rather than leaving it stuck at `Unknown`.**

## Performance

- **Duration:** ~28 min (00:05 → 00:33 UTC+3; excludes the 11m 12s full `check --offline` run)
- **Started:** 2026-09-03T21:05:00Z
- **Completed:** 2026-09-03T21:33:25Z
- **Tasks:** 3 of 3
- **Files modified:** 8 (3 created, 5 modified)

## Accomplishments

- **The D-01 gate is proven, not asserted.** `bootstrapIfOptedIn()` reads `PlayGamesOptInRepository.optedIn` before it awaits an Activity, before `PlayGamesSdk.initialize`, and before any `PlayGames.get*Client` call. The test pins this **negatively** — for a non-opted-in player it verifies `awaitActivity()` was never awaited, `initialize` was never called and no client getter ran. That matters because the violating version of this class is byte-for-byte plausible: it differs only in where one `if` sits, which is exactly the mistake Google's own "initialize in `Application.onCreate`" sample invites.
- **`session` can no longer get stuck at `Unknown`.** Six distinct branches — declined opt-in, no foreground Activity within the bound, a thrown `initialize`/`isAuthenticated`, an unauthenticated player, a failed player-ID lookup, and the happy path — each publish a resolved state. The tempting shape for the declined branch was a bare `return@launch`, which would have hidden the D-11 Settings section forever for the *majority* of players; the explicit `SignedOut` assignment is the fix and the test for it is the first one in the file.
- **D-14's degraded display name is real, not decorative.** `AuthenticationResult` carries no player ID, so resolving identity and name through the single `getCurrentPlayer()` call would mean a name failure also lost the ID — leaving nothing to report `SignedIn(playerId, null)` with. Identity therefore comes from the lighter `getCurrentPlayerId()` first; only then is the name attempted, best-effort.
- **`signIn()`'s decline-versus-error split is implemented and honestly labelled.** A successful Task with `isAuthenticated == false` is a silent `false`; a failed Task throws unless it is identifiably an `ApiException` in the `CommonStatusCodes.CANCELED` family. That classification carries the `[UNVERIFIED against a live Play Console project — D-04]` marker inline in its KDoc rather than being presented as an SDK contract.
- **The binding is live and the boundary held.** `PlatformModule` binds `PlayGamesService` unconditionally, `MainActivity` fires the bootstrap one line after `consentGateway.bootstrap()`, and `./gradlew check --offline` is green — including `PlatformBoundaryTest`, whose SDK-containment rule now has a real `com.google.android.gms.games.*` consumer to police rather than passing vacuously.

## Task Commits

1. **Task 1: Dependency and manifest wiring** — `eeb86d6`
2. **Task 2: PlayGamesService (TDD)** — `56837f1` (test, RED) → `ae52acb` (feat, GREEN)
3. **Task 3: Wire the real implementation into the app** — `678a59e`

No REFACTOR commit was needed. The GREEN implementation needed one formatting correction before commit (ktlint's `chain-method-continuation` forces a four-operator chain multiline); it was folded into the GREEN commit rather than committed as a separate refactor, since no committed state was ever unformatted.

## Files Created/Modified

- `platform/games/PlayGamesService.kt` (new, 254 lines) — `@Singleton` on the class, `AtomicBoolean bootstrapped` guard, `_session` `MutableStateFlow` exposed via `asStateFlow()`, one bounded `awaitForegroundActivity()` helper, and the `signedInSessionFor` / `resolveSignedInState` pair. The five Phase-12 methods stay no-ops.
- `platform/games/GamesConstants.kt` (new) — `ACTIVITY_WAIT_MS = 10_000L`, scoped to the games package rather than reusing `AdConstants` across a package boundary, with the KDoc stating why the duplication is deliberate.
- `platform/games/PlayGamesServiceTest.kt` (new) — 13 tests: 8 on `bootstrapIfOptedIn`, 5 on `signIn`.
- `gradle/libs.versions.toml` — `kotlinx-coroutines-play-services` under `[libraries]`, `version.ref = "coroutines"`.
- `app/build.gradle.kts` — `implementation(libs.kotlinx.coroutines.play.services)`; the stale "may be dropped from release until then" note on the `resValue` strings replaced with the fact that both are now manifest-referenced.
- `AndroidManifest.xml` — `com.google.android.gms.games.APP_ID` meta-data pointing at `@string/game_services_project_id`, immediately above the `PlayGamesInitProvider` removal.
- `di/PlatformModule.kt` — `bindGamesService(impl: PlayGamesService)`; class KDoc updated from "the [GamesService] half is still pending" to both halves delivered; a new method KDoc records the no-flavor-split decision and why `NoOpGamesService` stays unbound in the tree.
- `MainActivity.kt` — `@Inject lateinit var gamesService: GamesService` plus `gamesService.bootstrapIfOptedIn()` on the line immediately after `consentGateway.bootstrap()`, with the "why not `App`" reasoning carried over.

## Decisions Made

- **Two calls to establish a signed-in player, not one.** The plan specified `resolveSignedInState(activity, playerId)` — a signature that presumes a `playerId` already in hand — but did not say where that ID comes from, and `AuthenticationResult` exposes only `isAuthenticated`. `getCurrentPlayerId()` is the ID source; `getCurrentPlayer()` then supplies the name. The alternative (one `getCurrentPlayer()` call for both) is one round trip cheaper on the happy path and makes D-14 **unimplementable**, because the failure that D-14 is about would also destroy the ID the degraded state requires. Recorded as a deviation below.
- **A failed ID lookup is not a failed name lookup.** D-14 protects the *name*. With no ID there is no `SignedIn` to construct, so that path takes D-12's single not-signed-in state. Both are pinned by separate tests so the distinction cannot silently collapse.
- **`signIn()` returns "did the session resolve to `SignedIn`", not "did the SDK say authenticated".** These differ only in the degenerate authenticated-but-unidentifiable case, and letting them differ would let `signIn()` return `true` while `session` reads `SignedOut` — a contradiction every caller in 11-03/11-04 would then have to defend against.
- **Statics mocked rather than wrapped in a client seam.** A hand-written `GamesClients` indirection would have been the more testable-looking design and would have proven less: the failure this plan most needs to exclude is an SDK call happening *before* the opt-in read, and only mocking the real statics can observe that ordering. `mockkStatic` keeps the test plain-JVM (no Robolectric) and the SDK types stay inside `platform..`, where `PlatformBoundaryRules` allows them.
- **`Tasks.forResult` / `forException` over mocked `Task`s.** A completed `Task` is taken by `await()`'s fast path with no listener, executor or `Looper`, so the SDK's own factory works in a plain JVM test and encodes fewer assumptions about `await()`'s internals than stubbing four `Task` members would.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Worktree was missing the gitignored `local.properties`**
- **Found during:** Task 1 (before the first Gradle invocation)
- **Issue:** `local.properties` is gitignored and therefore absent from a fresh worktree, and `ANDROID_HOME` is unset here — every verification command in the plan would have failed with "SDK location not found". Same environmental gap plan 11-01 hit.
- **Fix:** Copied the main checkout's `local.properties` into the worktree root. It stays untracked (`.gitignore` line 7) and was never staged.
- **Files modified:** none tracked.
- **Verification:** `./gradlew :app:assembleDebug` reached the packaging tasks and succeeded.
- **Committed in:** n/a — no tracked file changed.

**2. [Rule 2 - Missing critical functionality] Player-ID source specified**
- **Found during:** Task 2
- **Issue:** The plan's `<action>` gives `resolveSignedInState(activity: Activity, playerId: String)` a `playerId` parameter and describes it as the fallback when `getCurrentPlayer()` fails, but names no source for it. Verified against the 22.0.0 AAR: `AuthenticationResult` exposes only `isAuthenticated()`, so neither the bootstrap's `isAuthenticated()` nor `signIn()` yields an ID. Without a source, the parameter is unfillable and D-14's `SignedIn(playerId, displayName = null)` state is unreachable.
- **Fix:** Added `signedInSessionFor(activity)`, which resolves identity via `PlayersClient.getCurrentPlayerId()` before delegating to `resolveSignedInState(activity, playerId)` for the best-effort name. `resolveSignedInState` keeps the exact signature and fallback semantics the plan specified, including preferring the fetched player's own `playerId` when both are available.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`
- **Verification:** `bootstrapResolvesSignedOutWhenThePlayerIdLookupFails` and `aFailedDisplayNameLookupDegradesTheNameAndNeverTheSignIn` both green — the two failures now provably produce different states.
- **Committed in:** `ae52acb` (part of the GREEN commit)

**3. [Rule 1 - Bug] Test helper typed against a platform type**
- **Found during:** Task 2 RED
- **Issue:** The first draft of the `Player` test double took a `String?` name so a null-named player could be faked. `Player.getDisplayName()` is a Java platform type (`String!`) which Kotlin resolved as non-null, so `every { displayName } returns name` failed to compile — an error unrelated to the intended RED signal.
- **Fix:** Dropped the parameter. D-14's degraded name is produced by the *lookup failing*, not by the SDK returning a nameless player, so a null-name double would have tested a shape the SDK never emits. The helper's KDoc now says so.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`
- **Verification:** RED reduced to exactly one root error, `Unresolved reference 'PlayGamesService'`.
- **Committed in:** `56837f1` (the RED commit)

**4. [Rule 3 - Blocking] ktlint `chain-method-continuation` on the SDK call chains**
- **Found during:** Task 2 GREEN (mandatory CLAUDE.md gate)
- **Issue:** `PlayGames.getGamesSignInClient(activity).isAuthenticated().await().isAuthenticated` is four chain operators, at or above ktlint's force-multiline threshold, so `ktlintMainSourceSetCheck` failed on two lines despite both being well under the 120-column limit.
- **Fix:** Hoisted `val signInClient = PlayGames.getGamesSignInClient(activity)` in both places, leaving three-operator chains. Behaviour unchanged; the client is still constructed per call and never stored as a field.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`
- **Verification:** `./gradlew :app:detekt :app:ktlintCheck --offline --rerun-tasks` green; the 13 tests re-run green after the change.
- **Committed in:** `ae52acb` (part of the GREEN commit)

### Documentation deviations

**5. `NoOpGamesService`'s import kept in `PlatformModule`**
- The plan said to "update the import from `NoOpGamesService` to `PlayGamesService`". The import was **added**, not swapped: the class KDoc still references `[NoOpGamesService]` to explain why the stub stays in the tree unbound (the `NoOpAdGateway` precedent), and ktlint's `no-unused-imports` counts KDoc links as usage. Removing it would have broken the doc link that justifies keeping the file.

---

**Total deviations:** 4 auto-fixed (2× Rule 3 blocking, 1× Rule 2 missing critical functionality, 1× Rule 1 bug) + 1 documentation deviation
**Impact on plan:** None on scope. Deviation 1 was environmental and left no tracked change. Deviation 2 fills a genuine gap in the plan's own instructions and is required for D-14 to be implementable at all. Deviations 3–5 are mechanical.

## Issues Encountered

- **The plan's Pattern-1 snippet for `bootstrapIfOptedIn` shows `resolveSignedInState(activity, playerId)` with `playerId` undefined**, and its prose separately mandates the explicit `SignedOut` assignment that the same snippet writes as a bare `return@launch`. The prose is authoritative on both counts and was followed; the snippet is a sketch inherited from `11-PATTERNS.md`.
- **The `check --offline` suite takes 11m 12s here**, unchanged from wave 1. It was run once, at the end of Task 3, per the plan's verification block; the per-task loop used the scoped `--tests "*.PlayGamesServiceTest"` command instead.
- Pre-existing `ExperimentalCoroutinesApi` opt-in warnings in `SettingsViewModelTest` / `SettingsViewModelSoundTest` were observed again. Out of scope for this plan; left untouched.
- `./gradlew :app:assembleDebug` was run **online** for Task 1, deliberately: the new catalog entry cannot resolve from cache on its first build.

## Known Stubs

`submitScore`, `unlockAchievement`, `incrementAchievement`, `showLeaderboard` and `showAchievements` are `Unit` no-ops in `PlayGamesService`. This is intentional and scoped: `11-CONTEXT.md` leaves it to Claude's discretion whether the Phase-12 methods get real bodies now, and nothing in `ui/` calls any of them until Phase 12. Writing bodies now would ship submission code no surface exercises and no test covers. **Phase 12 resolves these** — they are named in this plan's `provides`/`affects` chain for that reason. No stub blocks this plan's goal: PGS-01/02/03 are about the sign-in session only.

## Threat Flags

None. The plan's threat register covers everything this plan touched, and each `mitigate` disposition landed:

- **T-11-04 (information disclosure)** — mitigated. No logging call exists in `PlayGamesService.kt` or `GamesConstants.kt`; verified by grep for `Log.`/`println`/`Timber`, and no caught exception's message, player ID or display name is passed anywhere but into the state flow.
- **T-11-06 (denial of service)** — mitigated. `bootstrapIfOptedIn()` is a non-suspend `fun` launching on `@ApplicationScope`, never awaited by `onCreate`; both Activity waits go through the single `awaitForegroundActivity()` helper bounded by `GamesConstants.ACTIVITY_WAIT_MS`, and the timeout path is pinned by two tests.
- **T-11-07 (elevation of privilege)** — mitigated. No SDK-typed `@Provides`/`@Binds` was added; `GamesSignInClient`/`PlayersClient` are per-call locals, never fields. `PlatformBoundaryTest` passed inside the green `check --offline`, and its SDK-containment rule is no longer vacuous for `com.google.android.gms.games.*`.
- **T-11-05 (spoofing, accepted)** — unchanged. `playerId` reaches nothing but the state flow; no backend exists in this milestone.
- **T-11-SC (supply chain)** — as registered: `kotlinx-coroutines-play-services` is version-ref'd to the already-audited `coroutines` key, no new version literal. No blocking human checkpoint was required, and none was auto-approved — the artifact resolved on the first online build with no substitution.

No new network endpoint, auth path, file-access pattern or schema change at a trust boundary was introduced beyond what the register already names.

## Verification

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (Task 1; run online for the new dependency). Merged manifest confirmed to carry `com.google.android.gms.games.APP_ID` → `@string/game_services_project_id`.
- `./gradlew :app:compileDebugUnitTestKotlin --offline` — RED confirmed with exactly one root error, `Unresolved reference 'PlayGamesService'`.
- `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesServiceTest" --offline` — **13 tests, 0 failures, 0 skipped** (plan required ≥ 8).
- `./gradlew :app:detekt :app:ktlintCheck --offline --rerun-tasks` — green (mandatory CLAUDE.md gate).
- `./gradlew check --offline` — **BUILD SUCCESSFUL in 11m 12s** (full suite: `PlatformBoundaryTest`, `ManifestSurfaceTest`, `DomainPurityTest`, Roborazzi screenshots, all ViewModel/Compose tests).
- Acceptance greps: `class PlayGamesService` + `@Singleton` + `AtomicBoolean` present; the non-opted-in branch assigns `_session.value = GamesSignInState.SignedOut` explicitly; no logging call in either new main-source file; `abstract fun bindGamesService(impl: PlayGamesService): GamesService`; `gamesService.bootstrapIfOptedIn()` on the line immediately after `consentGateway.bootstrap()`.

## TDD Gate Compliance

Task 2 was `tdd="true"` and shows the required gate sequence in git log: `56837f1` `test(...)` whose RED state was confirmed by a real compiler failure naming the missing class, followed by `ae52acb` `feat(...)` that turned all 13 tests green. No test passed unexpectedly during RED — the suite could not compile, which is the strongest possible RED signal. No REFACTOR gate was needed.

## User Setup Required

**None to run this code; one item still owed before release, unchanged and explicitly not dropped (D-04).**

`PlayGamesService` is bound and will run, but no Play Games project exists in Play Console, so `isAuthenticated()` will answer `false` for every account on every device today — which the app correctly renders as `SignedOut` (D-12). The operator checklist, already recorded in `11-VALIDATION.md`'s **Manual-Only Verifications** table and in ROADMAP External Prerequisites, stands:

1. Create the PGS project on the existing store listing (`com.erman.pegsolitarie`).
2. Register OAuth credentials for **both** the debug-keystore SHA-1 and the original release-keystore SHA-1 (Phase 6 D-21 — no Play App Signing key exists yet).
3. Add a tester account.
4. Then run the three manual verifications `11-VALIDATION.md` lists: real silent sign-in plus the stock "Welcome back" popup on relaunch; the `signIn()` cancel-versus-error distinction (dismiss the chooser → silence; unregistered account → snackbar); and confirmation that the chooser/popup do not trip the Phase-10 interstitial foreground cadence.

Item 4's second bullet is what promotes `signIn()`'s `[UNVERIFIED — D-04]` classification to a confirmed contract, or corrects it. Until then the marker stays inline in both `GamesService.kt` (from plan 11-01) and `PlayGamesService.kt` (this plan) — visible to anyone reading the code, not only to anyone reading planning docs.

One further pre-release item inherited from Phase 6 assumption A1: `play-services-games-v2` ships **no** `proguard.txt` of its own, and its reflection is covered only transitively by `play-services-basement`'s consumer rules. That coverage is now finally *exercised* by real PGS calls, so a minified release build should be smoke-tested against live sign-in before shipping.

## Next Phase Readiness

Wave 2's platform half is done and nothing in it blocks the remaining plans:

- **11-03** (Home opt-in prompt, running in parallel) is unaffected by this plan — it builds against `FakeGamesService` and the seam contracts from 11-01, both untouched here. Its accept path (`setOptedIn()` then `signIn()`) now reaches a real implementation at runtime, and the D-18 error snackbar it renders is fed by this plan's throw branch.
- **11-04** (Settings section) gets exactly what D-06/D-14 need: `SignedIn.displayName` populated on the happy path and `null` on a degraded one, from a session flow that is guaranteed to leave `Unknown` — which is what makes D-11's "hide the section while `Unknown`" safe rather than a permanent hide.
- **Phase 12** inherits five no-op methods with a live, initialized SDK underneath them and an `Activity`-per-call pattern already established.

No blockers. Two things for the phase verifier: the `signIn()` cancel classification is `[UNVERIFIED]` by design (D-04, not an oversight), and `NoOpGamesService` is now unbound but deliberately retained with its test — the same arrangement `NoOpAdGateway` has had since Phase 9.

**Not modified, by instruction:** `STATE.md`, `ROADMAP.md` and `REQUIREMENTS.md`. This plan runs in a worktree alongside 11-03, and PGS-01 is shared between them; the orchestrator owns those writes after the wave merges. `requirements-completed` is deliberately empty above for the same reason — PGS-01/02/03 are not closed until 11-04 lands.

## Self-Check: PASSED

All 3 created files and all 5 modified files verified present on disk. All 4 commits (`eeb86d6`, `56837f1`, `ae52acb`, `678a59e`) verified present in `git log`. No missing items.

---
*Phase: 11-play-games-session*
*Completed: 2026-09-04*
