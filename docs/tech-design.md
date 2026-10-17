# Technical design — Peg Solitaire

**Owner: tech-lead.** This is the product's canonical technical design (rules/stacks.md; release
blocker R-6). It was seeded as a transcription by the release-manager on 2026-09-21
(`.planning/reports/phase-14/release-manager-release-docs.md`, O-5) and taken over, re-framed and
completed by the tech-lead on 2026-10-17 (`.planning/reports/phase-14/tech-lead-tech-design.md`).
Every decision below is a tech-lead decision or a transcription of a recorded ruling; each names where
it was decided. Entries are never deleted: a superseded entry gets a "superseded by" line and the
supersession is listed in §17.

**How to read it.** A sign-off, a plan or a review checks code against *this file*, not against the
report that first recorded a decision. When this file and a report disagree, this file wins and the
disagreement is a defect in this file to be fixed by the tech-lead. When a question is not answered
here, the tech-lead decides, records the decision here and says it was a tech-lead decision.

**Product facts the design rests on** (`CLAUDE.md`, `docs/concept-brief.md`): a native Android peg
solitaire game with a Classic mode of five hand-defined boards and a Challenge mode of sequential,
procedurally generated, always-solvable levels; orthogonal jumps only; stars per level; fully
playable signed-out and offline. The Core Value is "infinite, never-repeating, always-solvable
Challenge levels". The app is already published (versionName 2.0, versionCode 2); the release this
design serves is **3.0.0** (`VERSION`), versionCode 30000 (`rules/play-store.md`).

**What this file is not.** Not the GDD (`docs/gdd.md` does not exist; game-designer, §16), not the UX
spec (`docs/ux-spec.md`, ux-designer), not the QA plan, not the release checklist
(`docs/release-checklist.md`, release-manager). It records *technical* decisions only.

## 1. Stack — choice, pins, trade-offs

### 1.1 The stack: `android-compose-game`

The product type is `game`; rules/stacks.md's default for a game is `android-compose-game`, and that
is the stack (`CLAUDE.md`: `Stack: android-compose-game`). The choice predates the studio (phases 1–12
were built under gsd alone) and is confirmed here rather than made here.

| Considered | Verdict | Why |
|---|---|---|
| **android-compose-game** (Kotlin, Jetpack Compose, Material 3, Hilt, Room, pure-JVM `:domain`) | **Chosen** | Fastest path to Play with first-class tooling; the game core is a pure Kotlin module testable in seconds; the whole product is Android-only by scope (no iOS, no web in the concept brief); tight Play Games / AdMob integration is a requirement, not an option. |
| kmp-app | Rejected | No second platform is in scope, Xcode does not exist on the studio machine (rules/stacks.md), and Compose Multiplatform's Skia canvas is a cost with no buyer here. |
| godot / defold / libgdx | Rejected | A static grid puzzle needs no physics, particles or sprite pipeline; an engine would break the native-Compose decision and bring its own toolchain (gdtoolkit, export templates) for nothing the product shows. |
| web-twa | Rejected | A TWA over a PWA cannot carry Play Games v2 sign-in, Saved Games or the GMA Next-Gen SDK natively; the product's Slice-2 features are exactly what a TWA cannot do well. |

Trade-off accepted with the stack: Android only, and the toolchain's compileSdk wall (1.2 below)
binds a handful of AndroidX versions.

### 1.2 Pins

Authority for every pin is `gradle/libs.versions.toml` (each pin carries its rationale comment),
`build-logic/src/main/kotlin/android-app.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`.
This product predates `docs/toolchain.md`; the catalog's comments are its toolchain record.

| Pin | Value | Why (one line) | Decided in |
|---|---|---|---|
| Kotlin | 2.3.20 | Latest 2.3.x with an exactly matching KSP build; no KSP exists for 2.4.x and the project is KSP-only, never KAPT | catalog `[versions]`, 2026-06-28 |
| KSP | 2.3.9 | Built against Kotlin 2.3.20; the stdlib dependency must equal the Kotlin pin | same |
| AGP | 9.1.0 | The tooling's max-supported AGP; built-in Kotlin, so `org.jetbrains.kotlin.android` is never applied | same |
| Gradle wrapper | 9.5.1 | AGP 9.x requires ≥ 9.1; verified green | `gradle-wrapper.properties` |
| JDK / toolchain | 17 | AGP 9.x minimum; `jvmToolchain(17)` in both convention plugins | `android-app.gradle.kts`, `kotlin-jvm.gradle.kts` |
| Compose BOM | 2026.06.00 | One mutually compatible Compose set; no member is hand-pinned | catalog |
| Hilt | 2.60 | 2.57.1's Gradle plugin needs the legacy `BaseExtension` AGP 9.1.0 removed — verified failing 2026-06-28; compiler under KSP | catalog |
| Room | 2.8.4 | Stable line, not the KMP-focused 3.0 alpha; compiler under KSP; schema exported to `app/schemas/` (v1, v2) | catalog |
| Navigation Compose | 2.9.8 | Latest stable on the compileSdk-36 line; 2.10.x needs compileSdk 37 | catalog |
| `androidx.core` | 1.18.0, **forced** | 1.19.0 requires compileSdk 37, not installable in this environment | `app/build.gradle.kts` resolution strategy |
| `androidx.lifecycle` | 2.9.4, **forced as a group** | Atomic group; 2.11.x `-compose` variants require compileSdk 37 | same |
| kotlinx-coroutines / serialization | 1.11.0 / 1.11.0 | Standard substrate; `kotlinx-coroutines-play-services` shares the coroutines pin deliberately | catalog |
| DataStore Preferences | 1.2.1 | Scalars and flags; Flow-based | catalog |
| detekt / ktlint | `dev.detekt` 2.0.0-alpha.5 / 14.2.0 | detekt 2 has no stable release yet (rules/kotlin-android.md names this pre-release) | catalog |
| ArchUnit | 1.3.0 | Bytecode-level boundary tests (§2.4) | catalog |
| Play Games v2 / GMA Next-Gen / UMP | 22.0.0 / 1.4.0 / 4.0.0 | v1 APIs are being removed; the legacy `play-services-ads` line is deprecated and sunsets 2027 — never reintroduce it | catalog, 2026-08-27 |
| compileSdk / targetSdk / **minSdk** | 36 / 36 / **24** | Centralised in the convention plugin; minSdk ruled in 1.3 | `android-app.gradle.kts` |

**Known divergence from `rules/kotlin-android.md`, recorded rather than hidden.** The rule names
newer toolchain versions and minSdk 26 for products *scaffolded today*. This repo predates the rule,
and every version divergence has a recorded environment reason above (the compileSdk-37 wall, the
KSP/Kotlin pairing). Pins move only by the ladder rule in rules/stacks.md (each rung the newest
stable compatible with the rung above) and only outside a release week.

**D-18 — release builds are not offline-safe on a cold cache.** AGP's `produceReleaseComposeMapping`
implicitly resolves `org.jetbrains.kotlin:compose-group-mapping` at a coordinate tracking the Kotlin
pin, and nothing in this repo can pin it because nothing in this repo asks for it. One warm online
run must populate the Gradle cache before a release build. `./gradlew check --offline` is unaffected:
`check` never builds the release variant. *Decided in:* catalog `[versions]` header.

### 1.3 Ruling — minSdk stays 24 for 3.0.0

**Tech-lead ruling (2026-10-17).** `minSdk = 24` stands for the 3.0.0 release. The rule's 26 is a
scaffold-day default for a new product; this is a published app whose installed base includes API
24–25 devices, and raising the floor in a release drops those installs from updates. The technical
side has no forcing function in either direction:

- Nothing in the codebase *needs* 26. The two places the floor shows are handled: fonts ship as
  static instances because variation settings on resource fonts need API 26 (§11); the audio
  silent-mode gate carries an API 24–32 route-inference path that 26 would need anyway (§12).
  `ContextCompat.registerReceiver` and `TypefaceCompat`'s API 24–25 cache copy are the only other
  costs, both small and already paid.
- Raising to 26 would buy ≈ 50 KB of APK (variable fonts) and nothing else measurable.

The decision that *is* open is product-side: which API 24–25 installed share justifies dropping
them. That is the founder's (taste and money, rules/studio.md Question routing). **Routed as a
founder question through the producer, non-blocking, recommendation: raise to 26 in the first minor
release after 3.0.0 whose Play Console device statistics show API 24–25 under 1 % of active installs;
until then 24 stands and this entry is the recorded reason for the divergence.** The tech-lead owns
the raise when it comes: one line in `android-app.gradle.kts`, the font ruling re-opened for variable
instances, nothing else. *(The routed recommendation is superseded 2026-10-17 by the founder's
answer below; §17.)*

**Founder answer — Q-018, option 3 (2026-10-17): `minSdk` stays 24 indefinitely, unless a technical
need appears.** Recorded in `.planning/questions/FOUNDER.md` Q-018 (committed on
`feature/phase-15-ux-refresh`, 98913c8). The Play-statistics trigger above is withdrawn: no release
raises the floor for installed-base share, rule alignment or size. What follows is the tech-lead's
reading of "a technical need", so that the re-opening is a fact and not a judgement call:

- **Re-opens it:** a dependency the app must take — a security fix, a Play-policy-driven SDK
  (Play Games v2, GMA Next-Gen, UMP) or an AndroidX/Compose line the stack has to follow — whose
  supported minimum rises above 24 with no maintained version left at 24; a platform API that a
  scoped feature needs and that has no compat path on API 24–25; a Play Console or Play policy
  requirement. Each is evidenced by a citation (release notes, the artifact's manifest
  `minSdkVersion`, the policy page) and written here before any build changes.
- **Does not re-open it:** the ≈ 50 KB a variable font would save (§11); simpler code paths
  (`ContextCompat.registerReceiver`, the API 24–32 audio route inference, §12); rules/kotlin-android.md's
  26, which is a scaffold-day default for new products (the divergence is recorded in §1.2).
- **When one appears:** the tech-lead records the need and its evidence in this section, the
  producer tells the founder in the next titled update before the release that carries it (the
  answer already authorises a raise for a technical need, so this is information, not a new
  question), and the raise is the change described above — one line in `android-app.gradle.kts`, the
  font ruling (§11) re-opened for variable instances. Until then nothing about the floor is queued,
  scheduled or revisited.

## 2. Modules and boundaries

### 2.1 Modules

Two Gradle modules plus an included build (`settings.gradle.kts`: `include(":app", ":domain")`,
`includeBuild("build-logic")`). This is a deliberate departure from rules/kotlin-android.md's four-module
scaffold (`:game-core`, `:game-data`, `:game-ui`, `:app`): the repo predates the scaffold, and the
boundary the four modules exist to enforce — a pure game core and a platform seam — is enforced here
by one pure module plus bytecode tests (2.4). Splitting `:app` into `:game-data`/`:game-ui` buys
build parallelism this ~100-file app does not need and is not scheduled.

| Module | Plugins | Role | Depends on |
|---|---|---|---|
| `:domain` | `kotlin-jvm` (convention), `standards` | The game core: `model/` (`Board` as a `Long` bitmask, `BoardSpec`, `Move`, `GameStatus`), `rules/RulesEngine`, `generator/` (`LevelGenerator`, `DifficultyTiers`, `GeneratorConstants`, `ChallengeLevelBounds`, freeform footprints), `solver/`, `assist/` (opening book, `GetHintUseCase`), `scoring/`, `progress/` (the cloud merge law), `achievement/`, `ads/` (the cadence policy as pure logic), `telemetry/`, `usecase/`. Committed opening-book resources under `src/main/resources/golden/book/` | **Nothing but the Kotlin stdlib.** Test-only: JUnit, Truth |
| `:app` | `android-app` (convention), `standards`, KSP, Hilt, Room, serialization | `ui/` (home, classic, game, results, settings, onboarding, components, theme), `navigation/`, `data/` (repositories, `db/`, `resume/`, `cloud/`, DataStore stores), `platform/` (ads, games, lifecycle, activity), `audio/`, `di/` (six Hilt modules), `App`, `MainActivity` | `:domain`, AndroidX, Compose BOM, Hilt, Room, DataStore, Play Games v2, GMA, UMP |
| `build-logic` | `kotlin-dsl` | Convention plugins `kotlin-jvm`, `android-app`, `standards`; Gradle-free objects `AdIdResolution`, `GamesIdResolution`, `SigningResolution`, `SigningKeystoreProbe`, `ReleaseInvocation`, `SourceSetPolicy`, each with JUnit tests | catalog plugins |

### 2.2 `:domain` is pure, structurally

`:domain` applies only the Kotlin/JVM plugin, so an `android.*` or `androidx.*` import does not
resolve on its compile classpath, and `DomainPurityTest` (ArchUnit) is the second lock. It declares
**no non-test dependency at all** (`domain/build.gradle.kts`). Immutable data classes, pure
reducers, seeded randomness only (`kotlin.random.Random(Long)` from `stableSeed`), no coroutines in
the generator loop. The generator's output contract is **frozen** (`GeneratorConstants`, Pitfall 2):
a generation-algorithm change ships as a parallel generator version, never as an in-place mutation,
because every Challenge level a player has ever seen is a pure function of the level number.

### 2.3 Layering inside `:app`

Clean Architecture per the Android guide: `ui/` (Compose screens + ViewModels) → `data/`
(repositories behind interfaces, Room DAOs, DataStore stores) and `:domain`; `platform/` is the only
home of SDK types (2.4); `di/` wires it with Hilt constructor injection only. The UI never sees a
Room type: every read model crosses the repository boundary as a plain data class (`LevelResult`,
`HomeProgress`). Dispatchers are injected; an `@ApplicationScope CoroutineScope` (`di/DataModule`)
is the one home for work that must outlive a screen (the profile-swap wipe, the tutorial's seen-flag
write, §3.3).

### 2.4 The `platform/` seam — the rule and its test

**Every platform SDK type (`com.google.android.gms.*`, `com.google.android.ump.*`,
`com.google.android.libraries.ads.*`) lives under `app/.../platform/` and nowhere else.** Enforced by
`PlatformBoundaryTest` / `PlatformBoundaryGateTest` (ArchUnit, bytecode level) with a deliberate
negative fixture (`archfixture/GmsBoundaryViolationFixture.kt`) that proves the rule can fail. A
boundary asserted by review decays; one asserted by a test does not. *Decided in:* PLAT-04,
`.planning/phases/06-platform-foundation-build-hardening/`; re-verified at every phase-13/14 sign-off.

Behind the seam sit three interfaces with no-op implementations that degrade silently —
`AdGateway`, `GamesService`, `CloudSaveStore` — so the game is fully playable signed-out and offline.
The stub-first ordering is deliberate: an auto-granting stub is also what hides missing UI, so the
real implementation is wired last and the UI is built against the stub. *Decided in:* ROADMAP phases
6, 9, 11, 13.

**What is *not* platform.** `audio/` holds framework types (`AudioManager`, `AudioDeviceInfo`,
`BroadcastReceiver`) and is correct where it is: `platform/` is the Play/AdMob/UMP and
foreground-Activity seam the three ArchUnit rules define; a framework API with no SDK and no
Activity does not belong there, and rules/audio.md gives playback to one UI-layer owner outside the
game core. *Decided in:* `docs/tech/audio-silent-mode.md` §1.5 (tech-lead).

### 2.5 `build-logic` and the standards convention plugin

`standards.gradle.kts` is applied by **both** `:app` and `:domain`; anything that must hold for every
`Test` task or every module goes there once (the detekt widening, the literal gates, the result-store
self-heal in §9.6), never as a per-module copy. `build-logic`'s own Kotlin is **outside ktlint and
detekt** (F-5 / auditor F-4): pre-existing, true of every resolver object, mitigated by their
discriminating unit tests (54 at R-1), and recorded so no reader mistakes it for coverage. Applying
ktlint to `build-logic` as its own task is **post-v1.1** (§14), not release-week work.

## 3. State model and navigation

### 3.1 Unidirectional data flow

Every screen has one `@HiltViewModel` exposing one `StateFlow<UiState>`; one-off effects go through a
`SharedFlow`/one-shot signal; the UI sends intents through callbacks hoisted to the screen level.
Composables are stateless below the screen level and take state plus callbacks (rules/compose.md,
`compose-skill`). UI models are immutable data classes. No `runBlocking`, no `first()` on a start
path, no screen-wide loading state on Home (3.4).

### 3.2 Routes

`navigation/Routes.kt` is the single source of truth for destinations — type-safe `@Serializable`
routes (NAV-02) that the `NavHost` (`AppNavHost`), the catalog, Results and `GameViewModel` all build
against. Destinations: Home, Classic catalog, Gameplay (mode + level), Results, Settings, and (phase
15) Tutorial. A sixth screen that is a screen everywhere except in the nav graph is a special case
with no compensating benefit (3.3).

### 3.3 Ruling — the tutorial is a nav destination

**Tech-lead ruling, 2026-09-21** (`.planning/reports/phase-15/tech-lead-architecture.md` §1).
The onboarding tutorial is a `@Serializable data object Tutorial` destination registered as
`composable<Tutorial>` in `AppNavHost`, not a composable above the `NavHost`. Binding parts:

- Its state holder is a `@HiltViewModel TutorialViewModel` obtained with `hiltViewModel()` **inside**
  the destination, so it binds to the `NavBackStackEntry` and every visit starts at step T1. Step
  state lives in a plain `MutableStateFlow`, **never** in `SavedStateHandle`: a saved cursor would
  survive process death and contradict D-01e ("a process death mid-tutorial re-offers the tutorial
  from T1").
- **Persist before pop.** The ViewModel exposes exactly one exit path; it awaits `markSeen()` and only
  then emits a one-shot exit signal the screen navigates on. Skip, system Back (a `BackHandler` on the
  destination, the codebase's second after `AdQuiescenceScrim`) and the completion CTA all route
  through it. The write runs on the `@ApplicationScope` scope so a cancelled `viewModelScope` cannot
  drop it. A double exit is a latched no-op. Without this the fire-and-forget `markSeen()` plus an
  immediate pop produces a navigation loop (Home re-pushes the tutorial) and withholds the PGS prompt
  and the profile-swap dialog for an extra launch.
- Home auto-starts it from a `LaunchedEffect` on the persisted `tutorialPending` flag with
  `navigate(Tutorial) { launchSingleTop = true }`; the profile-swap dialog keeps its
  `!state.tutorialPending` guard.
- **A tutorial win is not a level win, structurally.** The tutorial hosts `BoardCanvas`, never
  `GameScreen`; its state holder is `TutorialViewModel`, never `GameViewModel` (whose `Won` path
  clears the mode's resume snapshot and advances the Challenge ladder); its only outbound edge is
  `popBackStack()`, so it can never reach Results or `recordResult`; `TutorialBoards` lives in
  `:domain` beside `Boards.kt` and is absent from `Boards.CLASSIC_SPECS`; its completion predicate is
  `board.pegCount == 1` on its own state with no `GameStatus` and no `minimumPegs`. Each prohibition
  is one `grep` over `ui/onboarding/`. The tutorial board is silent by default (every sound cue is
  played by a private effect in `GameScreen`), which rules/audio.md makes compliant; the ux-designer
  and sound-designer own any cue it gains.

### 3.4 Ruling — Home reads aggregates, never folds

**Tech-lead ruling, 2026-09-21** (same report, §2). The two Home ribbon values (total stars, Classic
boards cleared) come from **one `Flow`-returning SQL aggregate**, `LevelResultDao.observeHomeProgress`
→ `ProgressRepository.observeHomeProgress(): Flow<HomeProgress>`, with the same `.catch { emit(UNKNOWN) }`
fail-degraded contract as `observeResults` (a failed read renders "—", never `0`). A client-side fold
over `observeResults(MODE_CHALLENGE)` is **rejected**: the Challenge ladder is unbounded, Room
re-materialises the whole `Flow` on every write, and the start destination would load the player's
entire history on every visit — the exact fold `LevelResultDao.totals`' KDoc already forbids for a
background submitter. Standing rule, checkable in one grep: **no Kotlin fold over
`observeResults(MODE_CHALLENGE)` anywhere.** `HomeViewModel.init` keeps its independent-collector
shape over one `MutableStateFlow(HomeUiState())`; it is never restructured into a single `combine`,
which would make the whole Home state wait on a database round trip. `aggregate()` (suspend,
one-shot, goes stale) is not used for a live value.

## 4. Persistence

### 4.1 Room versus DataStore — the split

| Data | Store | Why |
|---|---|---|
| `level_result` — per `(mode, levelId)`: stars, best time, best score, completed-at | **Room** | Many rows, keyed, queried ("best for level N", totals, standings); upsert-on-better is Room's job |
| `play_session` and the resume snapshots (board + move history + elapsed, one per mode) | **Room** | One transactional store for everything a merge or a wipe must touch together (§5) |
| Challenge ladder (`challenge_current_level`), onboarding seen, settings (sound, haptics), Play Games opt-in, cloud profile, interstitial cadence, submission sync markers | **DataStore (Preferences)** | Scalars read on start or on an event; no queries |

Schema is exported to `app/schemas/` (versions 1 and 2) through the Room Gradle plugin's
`schemaDirectory`; a migration test runs under Robolectric. **A new `SELECT` over existing columns is
not a schema change** and needs no migration or version bump (3.4 added one without touching
`roomSchemaVersion`).

### 4.2 Aggregates are SQL

Every multi-row number the app shows or submits (totals, standings, Home progress) is a `@Query`
projection — `FROM`-less correlated subqueries that always return exactly one row, `COALESCE` on the
only nullable aggregate, column names bound to `ProgressPersistenceKeys.COLUMN_*` constants, read
models in `data/db/ProgressProjections.kt`. Never an in-memory fold: the Challenge ladder reaches
thousands of rows. *Decided in:* ROADMAP phase 12; restated as a rule in 3.4.

### 4.3 Repositories are the single source of truth

`ProgressRepository` owns `level_result`, the resume snapshots and (as a second data source, §5) the
cloud save; no screen knows whether a value came from Room or from the cloud. Its only writer of
`level_result` is `recordResult`, whose only `main` caller is `ResultsViewModel` — a fact the tutorial
ruling (3.3) depends on. Repositories are main-safe `suspend`/`Flow` APIs with injected dispatchers;
storage failures degrade through `StorageFailSafe` rather than crash.

### 4.4 D-07a — constraints on any future `SCHEMA_VERSION` bump of the cloud payload

**A hard gate.** `SCHEMA_VERSION` is 1 and no payload in the world declares 2, so none of this is
reachable in the shipped build — **and today both the right and the wrong behaviour pass**, which is
why it is written here. Any reader bumping `SCHEMA_VERSION`, widening the payload or adding a writer
must satisfy all three, each with a test that fails first. *Decided in:*
`.planning/phases/13-cloud-save/13-CONTEXT.md` §D-07a, from the phase-13 auditor sweep F-1/F-2/F-3;
re-verified in `tech-lead-prerelease-signoff.md`.

- **C-1 — the read path and the write path must refuse the same payload.** `decode` refuses a payload
  whose `schemaVersion` exceeds `SCHEMA_VERSION`; `mergeBytes` reaches the same bytes via
  `decodeOrEmpty`, which maps *both* "corrupt" and "decodable but newer" to the empty identity, and
  `commitOrDiscard` then writes a full overwrite — "refuse to read it, then delete it" is reachable.
  Before any bump, `mergeBytes` must distinguish the two null causes and, for "decodable but newer",
  return the remote bytes verbatim or abort the push, never the identity. A staged rollout is the
  normal state of two devices on different versions, and this is silent data loss on the one behind.
- **C-2 — cap the row count at decode; do *not* validate Classic ids against the shipped set.**
  Values are validated and the Challenge `levelId` is bounded, but cardinality is not, and the codec's
  threat model treats the payload as attacker-influenceable. Rejecting an unknown id would drop a
  future sixth board's rows — the forward-compatibility break D-07 forbids; dropping unknown-id rows
  during the trim is admissible.
- **C-3 — widen `dropCount` to `Long`** in the same change that lifts either of the above.

## 5. Cloud save

Play Games Saved Games, one snapshot, behind `CloudSaveStore` in `platform/games/save/`, consumed by
`ProgressRepository` as a second data source (§4.3). *Decided in:* `13-CONTEXT.md`; ROADMAP phase 13;
`.planning/reports/phase-13/tech-lead-signoff.md`.

| # | Decision | Why |
|---|---|---|
| 5.1 | **The merge is a monotonic union** — `max` stars, `min` time, `max` score per `(mode, levelId)`, `max` ladder, `max` played time — a pure function in `:domain/progress` proved **commutative, associative and idempotent** by JVM tests before any SDK code was written | A join-semilattice cannot lose progress whichever side runs first or twice; the proof is what makes "no prompt and no loss" a property |
| 5.2 | **`RESOLUTION_POLICY_MANUAL`**, never most-recently-modified; a conflict is resolved by merging both sides and committing the merge, bounded by `MAX_CONFLICT_RETRIES` | Last-writer-wins silently deletes the loser's progress — the outcome 5.1 exists to prevent |
| 5.3 | **D-03 — restore is fully silent.** No toast, dialog or Settings row; a cloud import never fires "New best!" or the Results celebration | Those are per-play signals; firing them per-merge congratulates a player for another device's play |
| 5.4 | **D-14 — the replace path wipes everything** (`level_result`, `play_session`, the ladder back to 1, both resume snapshots) in transactions, then imports; dialog copy states exactly what is replaced | A half-wipe leaves one account's rows attributed to another |
| 5.5 | **D-13 — every dismissal means keep-local** (Back, scrim, recents) | The destructive choice is the one explicitly taken, never the one that happens by walking away |
| 5.6 | **D-04 — a stale in-progress game is kept and the ladder jumps underneath it**; nothing on the resume path is touched by a cloud event | The half-finished board is the player's; the ladder is an aggregate |
| 5.7 | **The in-progress board snapshot never leaves the device** (CLOUD-04) | Keeps the payload a pure semilattice of bests, which is what makes 5.1's laws hold |
| 5.8 | `COVER_PX = 384` (384 × 384 × 4 = 589,824 B against the measured 819,200 B raw-ARGB cap) | A1's exact-integer measurement; the 512 cover produced "cover image is too large" on every commit |
| 5.9 | Outcome diagnostics (`CloudSaveStage` + status code) are written to DataStore and read only through `run-as` on debuggable builds; `change_pending = true` means "this device's latest local commit has not flushed", **not** "this device cannot upload" — the upload signal is `false` **together with** `lastModifiedAt` equal to this device's own last push | Three reports misread the flag as an upload oracle |

### 5.10 Handle ownership — the rule (FU-1)

**Tech-lead rule.** Every `Snapshot` handle the app opens has exactly one owner, and that owner ends it
with exactly one of `commitAndClose` / `discardAndClose`, inside `try`/`finally`, with the close's
failure observable in the outcome diagnostics (5.9) — never swallowed into a `closed` latch. Two
recorded exceptions, both deliberate and both **pinned by tests that fail if either close is added or
removed**:

1. **The give-up branch leaves the conflicting snapshot unclosed — correct, accepted permanently.**
   `discardAndClose` discards all changes to the file; on the give-up branch `resolveOnce` never ran,
   so the conflicting side's bytes — exactly where an un-uploaded offline commit lives — were never
   read into anything. Closing would be silent data loss traded for a visible, recoverable leaked
   handle. The dangling conflict record is the SDK's own designed state (`open` is documented to
   return a conflict "which will need to be resolved"; the next pass re-opens it), and the branch
   needs `MAX_CONFLICT_RETRIES` consecutive conflicts, against a maximum of two ever observed.
2. **The superseded and the final resolved snapshot pairs of the retry loop are abandoned today, and
   either closing both or recording the SDK's ownership passes.** FU-1 must choose one and pin it.
   *Tech-lead default for FU-1:* close what the app opened and did not hand on — the loop is the
   owner of every result `resolveConflict` hands back (the code's own comment says so) — and record
   any handle left to the SDK as an explicit ownership claim with a citation, not folklore. The 22.0.0
   javadoc does **not** say an unclosed handle fails the next open for the process; the `try`/`finally`
   practice is kept as reasoning, not as SDK contract.

FU-1 (one file, one test class, no player-visible change) also carries: a failed `discardAndClose`
reported as a distinct stage; `ensureActive()` after the `try` in `load`/`save`; `>=` on the retry
bound; `conflictsResolved` carried on the exhaustion path. **Deferred to after v1.1 on the strength of
field evidence**: two genuine conflicts ran on healthy devices, one with the full wedge signature, and
neither device wedged. If a device is ever again observed unable to upload, FU-1 is the first place to
look.

## 6. Session, ads and Play Games

| Decision | Why | Decided in |
|---|---|---|
| Play Games is **explicit opt-in**: the SDK is never initialised until the player accepts the one-time prompt or taps the Settings row; sign-in is once per process; `PlayGamesInitProvider` is removed from the manifest | PGS v2 has no sign-out and no sign-in wall; v1 patterns are not ported | ROADMAP phase 11, 2026-09-03 |
| GMA Next-Gen declares no auto-init provider and requires an explicit background-thread `MobileAds.initialize`; UMP consent is gathered before any ad request | SDK contract (D-17); CONS-01 | catalog `[versions]`; phase 9 |
| `AdReason.RETRY` is **never ad-gated** | Retry is the player's recovery action | ROADMAP phase 10 |
| The interstitial session boundary is measured from `ProcessLifecycleOwner` foreground/background transitions (`AppForegroundTracker`), with the cap persisted (`InterstitialCadenceStore`) so it survives process death | A per-session cap that resets on process death is not a cap | plan `10-02` |
| Leaderboard/achievement submission hooks into `ResultsViewModel.recordSolvedPlayOnce()`, **not** `GameViewModel` | `finishedEmitted` is reset by `onUndo()` and would double-submit; the Results hook has a process-death-safe exactly-once guard | ROADMAP phase 12 |
| **D-04a — "Next level" serves `ChallengeLevelBounds.clamp(maxOf(challengeProgress.settledLevel(), solvedLevel + 1))`**, read in `ResultsViewModel.onNextLevelTapped` through the *awaitable* `settledLevel()` (a suspend read straight from DataStore), never the cached `stateIn` value, with the arithmetic outside the stateless leaf (`ResultsActions` passes no number) | The cached value can serve a level the player has already completed after a cloud merge moved the ladder; the awaitable read is ordered after `GameViewModel`'s `advancePast(N)`; the `max` makes the re-serve loop unreachable even if the ladder write lands late (WR-01) | `game-designer-next-level-ruling.md` §B; `tech-lead-signoff.md` FU-3; PASSed in `tech-lead-prerelease-signoff.md` §A, pinned by seven named invariant tests in `ChallengeNextLevelTest`; device-verified at merge gap 5 |
| `ChallengeNextLevelTest`'s seeded, never-advanced `ladderScope` fixture is **left alone**, not folded into the shared DataStore double | It is what makes the settled-versus-cached distinction observable, and it pins D-04a's seven invariants | `tech-lead-signoff-r3-signing.md` §F |
| The debug snapshot picker lives in `src/debug` with one Hilt binding per build type; the release binding is inert twice over (`debugBuild = false`, empty `show`) | A debug-only QA surface must be structurally out of the release artifact; absence proved at class output, R8 dex and string level | `tech-lead-signoff.md` §C (L-3); `tech-lead-prerelease-signoff.md` §A |

## 7. Generator, assist and the fuzz harness

### 7.1 The Challenge generator — facts the design rests on

- `LevelGenerator.generate(levelNumber)` is a **pure function of the level number**: reverse-generation
  from a solved one-peg board with a `stableSeed(level, salt)` PRNG, a bounded reject-and-reseed loop
  (`MAX_ATTEMPTS = 1000`) over five quality gates, and an exact rolling dedup window
  (`DEDUP_WINDOW = 8`) over symmetry-canonical keys. The recorded `GeneratedLevel.solution` is the
  solvability certificate: replaying it forward through the unchanged `RulesEngine.applyMove` folds
  the starting board to one peg.
- **`generate(n)` replays the chain from level 1 to `n`** to rebuild the dedup window, so its cost is
  O(n), and `generate(n-1)`'s pass is a prefix of `generate(n)`'s. That is why the level number is
  clamped to **`MAX_CHALLENGE_LEVEL = 10_000`** (`ChallengeLevelBounds`, CR-01, D-15): the level space
  is finite and exhaustively enumerable.
- The output contract is frozen (§2.2). The golden fixtures (`ChallengeGeneratorGoldenTest`,
  `SolutionFixtures`, levels ≤ 1000) pin it byte-for-byte; any refactor of the generator must leave
  them green unchanged.

### 7.2 Assist

| Decision | Why | Decided in |
|---|---|---|
| Hints answer from a **committed depth-4 opening book** first (`src/main/resources/golden/book/`) and fall through to a bitboard solver with a restart policy under a 1.5 s deadline; a book "no winning move" is a **proof**, a book miss is a fall-through | A strengthened solver alone cannot meet the deadline for Wiegleb-45: 0 of 76 depth-≤4 positions resolved even at ~15M nodes/s | ROADMAP phase 8, escalation resolved 2026-08-29; `08-BOOK-EVIDENCE.md` |
| **Diamond-41's fewest-pegs floor is 2, not 1**, proved two ways: a committed 38-move line replayed through `RulesEngine`, and the GF(4) colour invariant checked over all 41 single-peg targets | Star bands anchored on an unreachable one-peg target made the top rating impossible | ROADMAP phase 8 (D-06); plan `08-06` |
| Star bands are derived **per board** from that board's own `minimumPegs` | One shared assumption cannot be right for five geometries | plan `08-07` |
| `DepthSweepTest` runs in its own `:domain:depthSweep` task wired into `check`, double-gated by `TestFlags.RUN_SWEEP` (task property **and** body check), `outputs.upToDateWhen { false }` because it asserts per-call latency against the deadline on this host | The sweep costs ~11 minutes (Diamond-41's 440 off-book positions); the per-commit `:domain:test` gate is held to seconds; neither half silently disables the other | `domain/build.gradle.kts`; `DepthSweepTest` KDoc |
| The sweep stays in `check` at full cost; the honest lever on the 11 minutes is closing Diamond-41's opening-book gap, **not** dropping `upToDateWhen { false }` (a timing gate inheriting a green measured on a differently loaded host is meaningless) | — | `tech-lead-depthsweep.md`, Open issues |

### 7.3 The fuzz harness — design (R-2, tech-lead ruling 2026-10-17)

rules/puzzle-design.md invariant 8 and rules/testing.md require a `FUZZ_SEEDS`-honouring harness
(generate → validate → solve → replay → verify win) at ≥ 1,000 / ≥ 10,000 (CI) / ≥ 100,000 (release)
seeds. Today solvability is asserted on six sampled levels (`ChallengeSolvabilityTest`). The design
below is what `.planning/reports/phase-14/tech-lead-r2-gate-brief.md` instructs the developer to build.

1. **The seed is the level number; the seed space is `1..MAX_CHALLENGE_LEVEL`.** `FUZZ_SEEDS = N`
   means levels `1..min(N, MAX)`, a prefix of the chain — because the chain is sequential and
   `generate(k)` is a prefix of `generate(k+1)`, a prefix is the only sample that costs O(N) rather
   than O(N²). A random sample of levels would cost O(N × MAX) and buy nothing: the space is finite.
2. **`LevelGenerator.generateLadder(upTo: Int): Sequence<GeneratedLevel>`** is added to `:domain`,
   yielding each accepted level of the chain in order, and `generate(n)` becomes
   `generateLadder(clamp(n)).last()`. Behaviour-preserving by construction (the loop body is
   unchanged, only lifted); pinned by a consistency test against `generate` and by the golden
   fixtures staying green. The frozen contract is about *output*; exposing the same chain is not a
   contract change.
3. **"Solve" is certificate verification, not an independent search.** The recorded `solution` is the
   generator's proof; the harness verifies it — every move legal on the replayed board, the final
   board at exactly one peg. Running the solver per level would re-create the depth sweep's cost
   ten-thousand-fold and prove nothing the certificate does not.
4. **Per level, the harness asserts:** `levelNumber` equals its index; every move legal on the board it
   is applied to; final `pegCount == 1`; every position inside the footprint;
   `generatorVersion == GENERATOR_VERSION`. **Over the run:** a second, independently constructed
   ladder is element-wise identical (invariant 3, determinism); the count examined equals
   `min(N, MAX)` and is > 0 (anti-vacuity, the depth sweep's T-08-39 standard).
5. **Failing levels are written to `domain/build/fuzz-failures.txt`** (one line per level with the
   failed assertion) before the test fails; the file is deleted at the start of a run and is absent
   on green. The release checklist's "failing seeds file absent" reads this path.
6. **Tiers.** `FUZZ_SEEDS` is read through `TestFlags` as env var or `-D` property; a non-integer or
   non-positive value **fails** with a message naming the value (rules/stacks.md contract 6); a value
   above `MAX` runs the whole space and prints that it did. For this product the CI tier (10,000) is
   **exhaustive**, and so is the release tier: `FUZZ_SEEDS=100000` is accepted and means "the entire
   finite level space plus the determinism pass", which is strictly stronger than 100,000 samples
   from an infinite one. Recorded here so nobody reads R-7's run as 90,000 seeds short.
7. **The `check` tier is chosen by measurement, not guessed.** The developer measures the harness at
   1,000 and at 10,000 (the test method's `time` in the JUnit XML, warm JIT, the per-worktree lock
   held, host load recorded) and applies this rule: 10,000 in ≤ 60 s wall → the default is 10,000
   (`check` = CI = exhaustive, and rules/stacks.md's "10,000 by default" is met); otherwise 1,000 in
   ≤ 60 s → the default is 1,000 and CI sets `FUZZ_SEEDS=10000` explicitly; otherwise the developer
   stops and reports the numbers. `check` already costs ~11 minutes; it does not get a second
   multi-minute gate.
8. **Shape:** one test class, `FuzzHarnessTest`, gated by a new `TestFlags.RUN_FUZZ` the way the sweep
   is; one `Test` task `:domain:fuzz` in `domain/build.gradle.kts` in `depthSweep`'s exact shape (its
   own filter, `systemProperty` for the flag and the count, `showStandardStreams`), wired into
   `check`; **excluded from kover** (§8.4). Unlike the sweep it is a pure function of code and `N`, so
   it keeps Gradle's normal up-to-date semantics with `FUZZ_SEEDS` declared as a task input.

**Measured figures** (to be transcribed from the R-2 developer report at sign-off): generation rate
(levels/s) at 1,000 and 10,000; the default tier chosen; `check` wall-clock delta. *Pending R-2.*

## 8. Testing

### 8.1 Where tests live and what runs where

| Suite | Runner | Gate | Cost |
|---|---|---|---|
| `:domain:test` — 36 test files: rules, generator properties, golden fixtures, solver, book, scoring, merge law | JVM, JUnit4 + Truth | per commit, in `check` | ~31 s |
| `:domain:depthSweep` | JVM, own task | `check` | ~11 min (§7.2) |
| `:domain:fuzz` | JVM, own task | `check` at the measured tier; CI 10,000; release exhaustive | measured (§7.3) |
| `:app:testDebugUnitTest` — ViewModels (Turbine), repositories against the real in-memory Room DB, Robolectric Compose UI tests (one per screen's main flow), ArchUnit boundary tests, codec and merge tests | JVM, Robolectric 4.16 SDK 34 (the only `android-all` image cached offline — **do not add `@Config(sdk = ...)` for another level**: it needs a network fetch and breaks `check --offline`) | `check` | minutes |
| `build-logic` tests (54 at R-1) — the release-gate resolvers | JVM | CI's first step (`./gradlew -p build-logic test`); not reachable from the root `check` | seconds |
| Device sessions | qa-engineer, real device else emulator | every executed plan, every phase, every release | — |

Deterministic tests only: no wall clock, no network, no unseeded random, no sleeps. No `@Ignore`,
disabled test or suppression without the code-auditor's approval line. Test names read as sentences.
TDD for core logic.

### 8.2 The shared DataStore double — contract (F-10)

Unit tests use one shared in-memory `DataStore<Preferences>` double, `InMemoryPreferencesDataStore`,
across the ten classes that need a store; **no test constructs `PreferenceDataStoreFactory.create`
without an explicit scope** (today: zero call sites, five KDoc mentions). *Decided in:*
`tech-lead-prerelease-signoff.md` §C; evidence `qa-engineer-datastore-acceptance.md` (5 hangs in 23
runs before, 0 in 52 after, Fisher p = 0.020); PASSed in `tech-lead-signoff-r3-signing.md` §A.

- The double preserves `updateData`'s read-modify-write atomicity under concurrent callers (a `Mutex`
  held across the caller's suspending transform) and `data`'s current-then-subsequent emission
  (`replay = 1`), and **carries its own contract test pinning both**.
- **F-11, a rule on the file:** `InMemoryPreferencesDataStoreTest` is the *only* guard on the double —
  the ten migrated classes are green against both known mutations. Any edit to the double that is not
  accompanied by a test failing without it removes the mitigation silently. No edit without that test.
- A class that genuinely asserts file-backed behaviour keeps a real store with an explicit scope tied
  to the test scheduler and says so in its KDoc. None needed it; the case stays open as the exception,
  not the rule.
- `ChallengeNextLevelTest`'s seeded `inMemoryDataStore(level)` variant is **not** folded in (§6). The
  two byte-identical zero-arg private copies (`ProgressSubmitterCloudTest`, `ResultsViewModelTest`)
  are folded into the shared double in R-8 (non-gating).

### 8.3 Wall-clock reads in tests

`AppForegroundTrackerTest` holds two open `first { }` awaits against a clock the production code
samples independently; a backwards clock step makes them unsatisfiable and the symptom is a 60-second
`UncompletedCoroutinesError` indistinguishable from F-10's. **Rule:** if that error is ever seen in
this suite again, `AppForegroundTrackerTest` is the first place to look. R-8 bounds both awaits with
`withTimeout` inside `runTest` (virtual time → an immediate named failure). The honest fix — an
injected clock in `AppForegroundTracker` — is **deferred past v1.1** because it re-cuts the
interstitial session boundary days before a first release, with its device confirmation of
assumption A1 still open. *Decided in:* `tech-lead-signoff-r3-signing.md` §E.

### 8.4 Coverage gate (kover) — ruling

rules/testing.md: core logic ≥ 90 % line coverage, measured with kover. **Scope: `:domain` only.**
`:app`'s coverage is not gated (the rule is about core logic; `:app` is UI, platform seam and
repositories whose tests are counted by name per acceptance criterion). *Procedure, decided by the
tech-lead and bounded:* add the kover plugin to `:domain`, **measure first**; at ≥ 90 % wire
`koverVerify` at 90 into `check` and stop; below it, set the bound at the measured floor rounded
**down** to a whole percent, record the shortfall and the 90 target here, and schedule the raise —
nobody writes tests to chase a number in a release week. kover's instrumentation and report inputs
**exclude `depthSweep` and `fuzz`**: `koverVerify` must depend on `:domain:test` only, or `check`'s
coverage step would re-run the 11-minute sweep. *Decided in:* `tech-lead-prerelease-signoff.md` §D;
scope and exclusions here. **Measured floor and bound:** *pending R-2; transcribed at sign-off.*

### 8.5 Golden-image screenshot suite — not in phase 15

**Tech-lead ruling, 2026-09-21** (`tech-lead-architecture.md` §4): no Roborazzi suite lands in the
UX-refresh phase. A golden recorded from the diff under review is an image of the implementation,
not of the contract (standing rule 1); CI is `ubuntu-latest` and development is macOS/arm64, so the
host-parity decision (CI-only recording, per-platform baselines, or a tolerance) is a plan of its own;
and R-2 wins on the same budget. Phase 15 instead owes a `testTag` constant on every new surface, the
two intentionally broken tests retargeted (never deleted or `@Ignore`d), and one Compose UI test per
changed screen. The suite is a roadmap item — "Golden-image screenshot suite (Roborazzi)", owner
qa-engineer with tech-lead sign-off — **after v1.1 and after R-2**, recorded against the post-refresh
screens.

## 9. Build, signing and gates

### 9.1 The Check command

`CLAUDE.md`: `Check command: ./gradlew check` (becoming `./scripts/check.sh`, 9.6). `check` runs, for
both modules: detekt (one widened task, 9.4), ktlintCheck, `dpSpLiteralCheck`, `adIdLiteralCheck`,
`gamesIdLiteralCheck`, `audioFocusLiteralCheck`; Android lint; `:domain:test`, `:domain:depthSweep`,
`:app:testDebugUnitTest`; and after R-2, `:domain:fuzz` and `:domain:koverVerify`. **A new gate is
registered in `standards.gradle.kts`'s `check` block (module-wide gates) or the owning module's
`check` dependency (module-specific tasks), and nowhere else** — CI invokes `check` verbatim (9.7), so
that is the one registration CI cannot forget. Wall clock today ≈ 11 min, dominated by the sweep.

### 9.2 Signing — environment only, three gates, no third state

| # | Decision | Why | Decided in |
|---|---|---|---|
| 9.2.1 | **Credentials come only from `PLAY_UPLOAD_KEYSTORE`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`, `PLAY_UPLOAD_STORE_PASSWORD`.** No `local.properties` source, no placeholder prefix, no committed keystore, no debug-keystore fallback; `local.properties.template` names the four with nothing to copy | D-16: a debug-keystore shortcut is the thing that survives to a store upload | `android-game-dev-signing-config.md`; PASS `tech-lead-signoff-r3-signing.md` §B |
| 9.2.2 | **No third state.** `releaseSigning` is `null` or all four; `signingConfigs { }` is created only in the second case and the release variant reads `findByName("release")`: the artifact is either signed with the upload key or unsigned exactly as before | The forbidden substitution has no code path | same |
| 9.2.3 | **Nothing interpolates a credential or the keystore path into a message**; `SigningResolutionResult.Resolved.toString()` redacts; passwords are **not trimmed** | A Gradle stack trace gets pasted into an issue; whitespace in a password is a character of it | same |
| 9.2.4 | **`enableV3Signing` stays at the AGP default. Do not revisit before v1.1** | The upload artifact is an AAB Play App Signing re-signs; the upload scheme is invisible to devices | same |
| 9.2.5 | **Three gates on a release invocation:** a configuration-time throw (fast, operator latency), `verifyReleaseSigning` / `verifyReleaseAdIds` / `verifyReleaseGamesIds` as `dependsOn` of **`packageRelease` and `packageReleaseBundle`** (hard happens-before; proven by an executed build that failed with no artifact on disk), and the same three on the `assembleRelease`/`bundleRelease` lifecycle hook as belt-and-braces (silent-on-no-match survives an AGP rename); `validateSigningRelease` ordered after `verifyReleaseSigning` | A dependency of a lifecycle task is a *sibling* of the package task; before this a **signed** AAB carrying an unresolved ad id could sit on disk after a failed build | rulings 1–2, `tech-lead-signoff-r3-signing.md` §C–D; implemented in R-1, PASS `code-auditor-gate-scope.md` |
| 9.2.6 | **`isReleaseInvocation` is a packaging-verb prefix test** (`ReleaseInvocation`, build-logic, 10 tests): `assemble`/`bundle`/`package`/`install`/`publish` + "elease" on the last path segment; analysis tasks (`lintRelease`, `detektRelease`, `compileReleaseKotlin`) no longer demand credentials. Rulings 9.2.5 and 9.2.6 were one change and **must never be split** | Narrowing the configuration gate is safe only because the packaging backstop exists | same |
| 9.2.7 | **Do not enable Gradle's configuration cache without re-auditing the signing capture.** `signingEnv` resolves the environment eagerly into a `Map` at configuration time and `verifyReleaseSigning`'s `doLast` closes over it; with the cache on, two passwords would be serialised into the cache entry. The fix for that day: carry the `Provider` into the task and resolve it there | Recorded so the shape of the fix is not rediscovered under pressure | `tech-lead-signoff-r3-signing.md`, Open issues; `gradle.properties` keeps CC off |

### 9.3 The literal gates and their scope (F-8 / R-1)

The three ID gates read `SourceSetPolicy.shippedSourceRoots()` = `src/main` + `src/release` +
`src/debug` × `{java,kotlin,res}`, filtered by `exists()`. **Test source sets are excluded on purpose
and the failure messages say so**, derived from the list (`SHIPPED_SOURCE_ROOTS_DESCRIPTION`), because
a gate whose failure text misdescribes its own scope is how F-1's false claim happened. `src/debug` is
not cosmetic: a real ad unit id there is AdMob invalid traffic against the account, and a focus request
there falsifies every QA audio observation. `src/test*` legitimately names the test-publisher prefix,
the key names and the focus API. *Decided in:* `tech-lead-prerelease-signoff.md` §B; implemented R-1,
PASS `code-auditor-gate-scope.md` (planted probes in all five source sets, negative control included).

**The tripwire.** At configuration time, any `src/<name>` directory outside `KNOWN_SOURCE_SET_NAMES`
fails the build naming both lists and the two decisions. **Ruling on auditor F-2 (2026-10-17):**
`KNOWN_SOURCE_SET_NAMES` is trimmed to the five source sets that exist (`main`, `test`, `debug`,
`release`, `testDebug`). The pre-authorised `androidTest*`, `screenshotTest*` and `testRelease` are
removed: their Kotlin would be seen by no detekt task (only `VARIANT_SOURCE_SET_NAMES` feeds the
widening), and the tripwire's question is "has anyone classified this?" — the day `src/androidTest`
appears (the golden-image suite, §8.5), the wire must trip and force the detekt decision. Goes into the
next gate branch (the R-2 brief). F-9's durable shape — deriving the roots from `android.sourceSets` —
waits for a flavour or a third build type.

### 9.4 detekt reads the build-type source sets through one widened task

`detekt` (the one task `check` invokes) has `src/debug`, `src/release`, `src/testDebug` added to its
source; not `detektDebug`/`detektRelease`, because this convention plugin is applied to `:domain`
where variant tasks do not exist and it carries no AGP types. One task, one config, one report, 2 s
instead of 44 s. `MagicNumber`'s `**/test/**` exclude does not match `**/testDebug/**`; leave it until
`src/testDebug` grows past a handful of files (the trigger is the first legitimate false positive).
*Decided in:* `tech-lead-prerelease-signoff.md` §A–B.

### 9.5 Versioning and the build command

`VERSION` = `3.0.0`; `versionCode = 30_000` (`X*10000 + Y*100 + Z`, rules/play-store.md), above the
published 2. `namespace` stays `com.app.pegsolitaire` (code packages, `BuildConfig`); `applicationId`
is `com.erman.pegsolitarie` (the published listing). Release is minified and resource-shrunk. The
Build command's move from `:app:assembleRelease` to `:app:bundleRelease` is a release-branch edit the
release-manager owns.

### 9.6 Check-gate concurrency — F1 and F2 (tech-lead ruling 2026-09-28, scheduled 2026-10-17)

`:domain:depthSweep` was left permanently red by two concurrent Gradle builds in one worktree: a
second build's `fs.delete(binaryResultsDirectory)` unlinks the first's in-progress result store (M1),
and a truncated `results-generic.bin` beside an intact `output-events.bin` makes Gradle's own
`SerializableTestResultStore.hasResults()` throw `EOFException` before any test runs, on every later
invocation, until the directory is deleted by hand (M2). Both reproduced deterministically; neither is
memory, a timeout, a killed worker or the diff. Latent since phase 8, filed in phase 12, lost.
*Decided in:* `.planning/reports/phase-15/tech-lead-depthsweep.md`.

- **F1 — self-heal (fixes M2), in `standards.gradle.kts`:** `tasks.withType<Test>().configureEach {
  val binaryResults = binaryResultsDirectory; doFirst { binaryResults.get().asFile.deleteRecursively() } }`.
  Covers `:domain:test`, `:domain:depthSweep`, `:domain:fuzz`, `:app:testDebugUnitTest` in one place.
  Cost: previously-failed-first ordering, immaterial here. Configuration-cache safe (captures only the
  task's own `DirectoryProperty`). Verified (E4) before it was specified.
- **F2 — one build at a time per worktree (fixes M1):** `scripts/check.sh` takes an exclusive
  per-worktree lock (atomic `mkdir` on `.gradle/ergoz-build.lock`, PID inside, stale-PID reclaim,
  `trap` release, `--wait` for a caller that prefers to queue) and runs `./gradlew check "$@"`;
  `CLAUDE.md`'s Check command becomes `./scripts/check.sh`. `flock` does not exist on macOS and
  `lockf`/`shlock` are BSD-only; `mkdir` is atomic on both. The lock is the mechanical backstop for
  rules/gitflow.md's "one worktree per concurrently working agent", which convention failed twice. It
  generalises: two builds in one worktree also race on merged resources, KSP output, lint models and
  the build cache. CI (one build per runner) keeps the direct `./gradlew check`.
- Rejected: a per-invocation unique `binaryResultsDirectory` (non-deterministic `@OutputDirectory`,
  litters `build/`, protects one output of several); `--rerun-tasks` (proven not to help); a cleanup
  step in a wrapper only (leaves every hand-run task exposed); splitting the sweep.
- **The gate is not weakened.** The sweep stays in `check`, always-run.

### 9.7 CI runs `check`, verbatim

`.github/workflows/ci.yml` runs `./gradlew -p build-logic test` (first, fail-fast) then
`./gradlew check assembleDebug` on every push and PR to `develop` and `main`. It used to name tasks
and so never ran the four literal gates. **Ruling on the sweep's CI cost:** stays in the same job
until the measured wall clock of the `verify` job exceeds **25 minutes**; then `:domain:depthSweep`
moves to its own parallel job in the same workflow whose union with the first job is still `check`
(never back to a list of task names). The producer records the first measured figure in STATUS.md.
After R-2 the step carries `FUZZ_SEEDS=10000` explicitly whatever the local default (§7.3).

## 10. Performance budgets

| Budget | Value | Status | Decided in |
|---|---|---|---|
| **Cold start, the gate** | **< 2 s on a named mid-range physical phone** (rules/studio.md product baseline) | **Never measured on a physical device** — every number in the repo is from an emulator. Owner: PLAT-08 / phase 14; the reference device is named in the release evidence. *Decided default if no physical device is available:* the release proceeds on the interleaved emulator gate below plus the Macrobenchmark follow-up, with the absolute bar recorded as **unverified**, never silently passed. Buying a test phone is a founder question the producer folds into the release track | `tech-lead-coldstart-gate.md` §4, Open issues |
| Cold start, tracked target | ≤ 950 ms `am start -W` TotalTime on AVD `Pixel_9a` API 36, debug | **Demoted from gate to tracked target.** It derives from a pre-SDK 633 ms baseline and a different `applicationId`; the same AVD read 957 ms and 1832 ms with nothing changing but host load (1.83×), so an absolute emulator reading is a property of the host, not the app | same §2, §4 |
| Cold-start regression (criterion 15e) | Interleaved A/B gate: `median_B − median_A ≤ max(2 × floor, 100 ms)` with `floor = |median(N1) − median(N2)|` from a **same-sitting null control** of one build in both positions; **VOID** (not pass, not fail) when `floor > 150 ms`; ≥ 20 kept samples per arm in blocks of 5, first of each block discarded, `am force-stop` + 2 s between samples; ART normalised with `cmd package compile -m speed -f`; animations off; exactly one emulator; no Gradle daemon anywhere; 1-minute load ≤ half the host's logical cores (5.0 on the studio's 10-core M4) read before and after with the launcher idle; arms share one `applicationId`, so each arm is swapped in **only at a block boundary** and the null control swaps the same way; valid only while both arms share the Room schema and DataStore keys; `Pixel_9a` is the instrument of record and a `Pixel_9` sitting is VOID | Replaces the ill-posed ≤ 50 ms delta (a within-arm range promoted to a between-arm threshold — the tech-lead's own specification error). Runs once, on the phase-15 follow-up slice, with `develop` as arm A | `tech-lead-coldstart-gate.md` §3; `15-founder-feedback/tech-lead-fonts.md` Ruling 2 |
| Instrument succession | `:macrobenchmark` module (`androidx.benchmark`, Apache-2.0) with `StartupTimingMetric`, `StartupMode.COLD`, `CompilationMode.Full()`, ≥ 10 iterations, physical device; plus `reportFullyDrawn()` in `MainActivity` so time-to-full-display becomes measurable (criterion 15f's real subject) | **Scheduled, not phase 15's**; producer schedules, android-game-dev implements, qa-engineer runs. `reportFullyDrawn()` is its own small plan | `tech-lead-coldstart-gate.md` §6 |
| Home's first frame | No blocking read, no image decode, no asset load, no bitmap, no new SDK call — **one named exception: the five bundled type faces** (§11), which load blocking on first use so the first frame is final; the progress ribbon fills asynchronously into a placeholder | Held structurally by the independent-collector shape (§3.4) | `docs/ux-spec.md` §6.5 as amended |
| Hint latency | 1.5 s deadline per `GetHintUseCase` call; book hits in single-digit ms; `depthSweep` measures it exhaustively over every covered frontier | Diamond-41's 440 off-book positions wait out the deadline — the honest cost of the book's documented gap | §7.2 |
| `check` wall clock | ≈ 11 min today; the fuzz default tier adds ≤ 60 s (§7.3); no second multi-minute gate | — | §7.3, §9.7 |
| APK size | Baselined, not gated (D-12): last recorded release APK 2,038,611 B at phase 7, before the ads and PGS SDKs; the fonts add ≈ 229 KiB compressed | — | `06-CONTEXT.md:42`; §11 |
| Cue path | `play()` is one volatile read, zero IPC, zero allocation; no audio work on the main thread beyond the play call | — | §12 |
| Idle Home | `HomeBlobs` draws at ≈ 61 fps idle (O-B) — a pre-release `compose-performance-audit` item (rules/compose.md), not ruled here | open, owner android-game-dev at the audit | `tech-lead-fonts.md`, Open issues |

## 11. Typography — the fonts are bundled

**Tech-lead ruling, 2026-09-29** (`.planning/reports/15-founder-feedback/tech-lead-fonts.md`, Q-013).
Fredoka Medium and SemiBold and Nunito Regular, SemiBold and Bold ship as **five unmodified static
OFL-1.1 files in `app/src/main/res/font/`**, as a `FontFamily` each in `ui/theme/AppFonts.kt`
(`internal object AppFonts`, its own file so there is no in-file initialisation order to get wrong —
the F-A bug), every `Font(resId, weight, loadingStrategy = FontLoadingStrategy.Blocking)`. The
downloadable Google Fonts path is **deleted**: the `GoogleFont` provider, `font_certs.xml`, the
`ui-text-google-fonts` dependency and the test-only fixture.

| Weighed | Repair the downloadable path | **Bundle (chosen)** |
|---|---|---|
| First launch offline / no Play services | Roboto for that process, silently (Compose keeps a failed resolve as its permanent fallback) | The real faces on every device, online or not |
| Home on every cold start | Faces arrive after the first frame → a visible face swap and a relayout of the start destination | The first frame is final; no swap, no relayout (§6.5, 15f) |
| Fragility | Depends on the GMS signing lineage; one corrupted certificate = silent Roboto | No certificate, no provider, no IPC |
| Cold start | Cost paid after first paint (provider IPC + relayout) | Blocking resource loads during Home's first composition: ≈ 364 KB raw, ≈ 175 KB inflated, cached per process; **estimate** 10–40 ms on the shared-host emulator, inside 15e's measured delta, which is the measurement |
| Static vs variable | — | **Static**: minSdk is 24 (§1.3) and variation settings on resource fonts need API 26; a variable Nunito would render ExtraLight on 24–25 and is read once per weight anyway (saving ≈ 50 KB compressed) |
| Testability | Robolectric has no provider; every face test went through a fixture, which is how F-A hid for a month | Tests render the production `AppTypography` from `R.font` and go red on a fall back to Roboto (T1–T3) |

Consequences recorded: APK +496,332 B raw (≈ 229 KiB compressed); OFL-1.1 §2's notice condition is met
by the stand-alone `assets/licenses/Fredoka-OFL.txt` and `Nunito-OFL.txt` in the APK (no Reserved
Font Name in either licence; files unmodified; OFL-1.1's copyleft binds only the font files, like
MPL-2.0's file-level row — **not a copyleft escalation**, verdict recorded by the research-analyst);
Fredoka has **no `tnum`** (its digits are proportional, "1" = 388, "2" = 587 per 1000 em), which is
why `TimerDigits` sets the timer's digits in fixed cells; Nunito's digits are tabular by design. The
late-face path in `TimerDigits` stays as a guard for a post-layout face or font-scale change.
Acceptance (AC-F1–F10) and the device re-walk with the digit-"1" discriminators are in the ruling;
`THIRD_PARTY.md` carries one row per family (§13). If 15e fails on the build that first bundles the
faces, the tech-lead rules on their share **before** any art is removed: removing art cannot pay for
type.

## 12. Audio

Playback is owned by one UI-layer class, `audio/SoundPoolPlayer` (`SoundPlayer` interface; cues in
`SoundCue`, mix constants in `SoundParams`), preloaded through `SoundPool` with `USAGE_GAME` +
`CONTENT_TYPE_SONIFICATION` attributes (`GameAudioAttributes`, the one definition the pool and the
route query share). **No audio focus is ever requested** (SOUND-06: an app that holds no focus cannot
duck the player's music), proved by `audioFocusLiteralCheck`. Assets: twelve mono 16-bit 44.1 kHz WAVs
under `res/raw`, each with a `docs/audio/CREDITS.md` row and, for in-house sounds, a committed
`audio/recipes/<cue>.json`. `:domain` plays nothing.

**The silent-mode design** (`docs/tech/audio-silent-mode.md`, tech-lead, 2026-09-29, Q-010 option 2;
builds on `feature/audio-review` only after `develop` contains phase 15 — C1):

| # | Decision |
|---|---|
| A1 | Game sound mutes when the ringer is **Silent or Vibrate and** the output is **the phone itself** (`TYPE_BUILTIN_SPEAKER`, `_EARPIECE`, `_SPEAKER_SAFE`). Every external output keeps sound. Do Not Disturb is not read. Nothing is written to any system setting |
| A2 | The gate lives in `SoundPoolPlayer` beside the `soundOn` gate, fed by an injected `SystemAudioState` seam in `audio/` (`RingerMode`, `OutputRoute`, `SystemAudioSnapshot.mutesGameSound`); `SoundPlayer`, every caller and `:domain` are unchanged; no "muted" flag reaches any `UiState` |
| A3 | **Event-driven, never checked at play time**: `RINGER_MODE_CHANGED_ACTION`, `ACTION_AUDIO_BECOMING_NOISY` (forces `BuiltIn` at once) and an `AudioDeviceCallback` trigger a full re-read on the application scope's `Dispatchers.Default`; `play()` reads one volatile |
| A4 | Route: API 33+ asks the platform where the pool's own attributes route (`getAudioDevicesForAttributes`); API 24–32 infers from connected outputs through an **allowlist** of external media types (unknown types count as the phone — the quiet direction). Only `getType()` is read, never address or name, so no Bluetooth permission |
| A5 | Entering the muted state **stops streams in flight** (`silenceNow()`), the SOUND-05 standard |
| A6 | The `SoundEnabled` confirmation pop still bypasses only the in-app mute (D-12) and honours the speaker and background gates |
| A7 | Fail-open, one collector per input, never `combine`; a reader that throws or never emits leaves today's behaviour and can never disable the in-app mute |
| B1 | **P8**: the process `ON_STOP` (`ProcessLifecycleOwner`, via a new `AppVisibility` seam, **not** `AppForegroundTracker`, which is the interstitial boundary in `platform/`) stops streams and blocks new cues until `ON_START` |
| B2 | **P4a**: `VOLUME_JUMP` 0.9 → 1.0 lands together with the re-levelled asset, never apart |
| B3 | **P5**: `AudioCreditsTest` reads the `## Files` table by header, classifies sourced (https + allow-listed licence) vs in-house (`audio/recipes/<stem>.json` exists), covers every file in `res/raw`; a new `AudioAssetFormatTest` parses each WAV (PCM, mono, 44.1 kHz, 16-bit, ≤ 200 KB, peak ≤ −1 dBFS, exactly `SoundCue.entries.size` files) |
| B5 | **V1**: `volumeControlStream = STREAM_MUSIC` in `MainActivity`, so the volume keys can never step the phone into Vibrate from inside the game |

Accepted limitations: a route change with no device change (the output switcher) is unseen until the
next event; on API 24–32 a connected Bluetooth sink with media moved back to the phone reads as
External. Device checks D1–D10 need a physical phone with wired and Bluetooth headphones.

## 13. Dependencies and licences

`THIRD_PARTY.md` (release-manager) is the licence record of what ships, derived from Gradle
configuration, one row per idea, asset, snippet and dependency; the release gate blocks on any row
without a licence. This section is the tech-lead's dependency *policy* and the licence verdict per
family; it does not duplicate the rows.

| Family | Coordinates (direct) | Licence | Copyleft? | Note |
|---|---|---|---|---|
| Kotlin stdlib, kotlinx-coroutines, kotlinx-serialization | `org.jetbrains.kotlin*`, `org.jetbrains.kotlinx:*` | Apache-2.0 | no | `kotlinx-coroutines-play-services` shares the coroutines pin |
| AndroidX | core, activity-compose, lifecycle (atomic group), navigation-compose, datastore-preferences, room (runtime, ktx, compiler), hilt-navigation-compose, Compose BOM members | Apache-2.0 | no | the compileSdk-36 forced versions (§1.2) |
| Dagger / Hilt | `com.google.dagger:hilt-android`, `hilt-compiler` | Apache-2.0 | no | KSP, never KAPT |
| Error Prone annotations | `com.google.errorprone:error_prone_annotations` | Apache-2.0 | no | `compileOnly`; Dagger 2.60 no longer exports it |
| Play Games Services v2 | `com.google.android.gms:play-services-games-v2` 22.0.0 | Android SDK Licence (proprietary, permitted) | no | behind `GamesService`/`CloudSaveStore`; ships no `proguard.txt` of its own |
| Google Mobile Ads Next-Gen | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` 1.4.0 | Android SDK Licence (proprietary, permitted) | no | behind `AdGateway`; read the coordinate character for character — the legacy `play-services-ads` is a different artifact and is never reintroduced |
| User Messaging Platform | `com.google.android.ump:user-messaging-platform` 4.0.0 | **unverified (U-1)** — the AAR carries its dependencies' licences and no licence of its own; one online dependency listing settles it | — | release-checklist 3.5 |
| Fonts | Fredoka v17 (Medium, SemiBold), Nunito v32 (Regular, SemiBold, Bold), static, unmodified | OFL-1.1, no Reserved Font Name | file-level only; **not** an escalation (§11) | notice shipped in `assets/licenses/` |
| Sounds | 9 Kenney + 3 freesound borrows, per `docs/audio/CREDITS.md` | CC0 1.0 | no | attribution a courtesy |
| Test-only | JUnit 4 (EPL-1.0), Truth, Turbine, MockK, kotlinx-coroutines-test, Robolectric, ArchUnit, Room testing, Compose ui-test | various permissive | no | never on a variant's runtime classpath |
| Build-only | AGP, Kotlin, KSP, Hilt, Room, detekt, ktlint Gradle plugins; **kover (R-2, Apache-2.0, JetBrains)** | permissive | no | run on the build machine; kover joins `THIRD_PARTY.md`'s build-tooling row when R-2 lands (release-manager's file) |

**Policy.** Non-copyleft additions are autonomous (rules/studio.md) and still get a catalog comment
with the reason and a `THIRD_PARTY.md` row; any GPL/LGPL/AGPL/CC-BY-SA candidate stops and escalates
to the founder through the producer before it is added; "no licence" or "unknown" is never added.
Nothing shipped carries an **unmet** attribution obligation; the studio-rule gap of an in-app licences
surface is open (§16). No dependency was added by phases 13–14; R-2 adds kover (build-only), phase 15
removes `ui-text-google-fonts`, the Macrobenchmark follow-up will add `androidx.benchmark` (Apache-2.0).

## 14. Technical order

The order gsd plans within and the producer schedules. Per module the order is core → data → UI →
polish; across the remaining v1.1 work it is the dependency order below.

### 14.1 Before the release branch is cut (3.0.0)

| # | Item | Owner | Depends on |
|---|---|---|---|
| R-1 | Gate task (rulings 9.2.5, 9.2.6, 9.3, 9.7) — `feature/phase-14-gate-scope` | android-game-dev → code-auditor PASS → qa-engineer → tech-lead sign-off → merge | — |
| **R-2 + F1/F2 + F-2** | **One branch, serial commits**: F1 self-heal → F2 lock script + Check command → F-2 trimmed known-set + F-1 KDoc → `generateLadder` → fuzz harness + tiers + CI env → kover gate. Brief: `.planning/reports/phase-14/tech-lead-r2-gate-brief.md` | android-game-dev → code-auditor → tech-lead | R-1 merged (same file region in `standards.gradle.kts`, same `check` block); merges **before** phase 15, which then merges `develop` in |
| R-8 | Test hygiene: fold the two duplicate doubles; bound the two `AppForegroundTrackerTest` awaits | android-game-dev → code-auditor | parallel, non-gating; slips to v1.1.1 if not green in one loop |
| Phase 15 | UX refresh with the font bundling, the follow-up slice, and 15e on that slice | android-game-dev → code-auditor + qa-engineer → tech-lead | R-2 branch merged first (so phase 15's closing Check runs on a gate that cannot wedge); its catalog edit (removing `ui-text-google-fonts`) and R-2's (adding kover) are disjoint regions |
| Audio follow-up | `docs/tech/audio-silent-mode.md` §5 build order (P5 → P4a → gate → P8 → V1) | android-game-dev → code-auditor → qa-engineer (physical phone) → sound-designer review | `develop` contains phase 15 (C1) |
| R-4 | D-14 account-swap UAT with a second Play Games account | qa-engineer | a second account (else a founder question) |
| R-5 | `THIRD_PARTY.md` — U-1 and §5b closed by the release-branch dependency listing; the founder's store-artwork rows | release-manager; founder (Q-F3) | — |
| R-6 | This file | tech-lead | done 2026-10-17; the R-2 figures transcribed at sign-off |
| R-7 | `FUZZ_SEEDS=100000 ./scripts/check.sh` on the release branch (exhaustive, §7.3), `domain/build/fuzz-failures.txt` absent, coverage figure recorded | release-manager | R-2 |
| PLAT-08 | Cold start on a named physical phone, or the unverified record (§10) | qa-engineer; producer for the device question | — |

### 14.2 After v1.1 — recorded so none of it is rediscovered

F-4 `ResultsViewModel`'s interstitial-cluster extraction (it sits exactly at detekt's 11-function
ceiling; the next change to that screen forces an extraction or a `@Suppress`, so schedule the
extraction); FU-1 handle lifetime and pull diagnosability (§5.10); D-07a C-1/C-2/C-3 on any payload
schema bump (§4.4); the injected clock in `AppForegroundTracker` (§8.3); F-9's AGP-derived source
roots at the flavour / third-build-type trigger; `MagicNumber`'s `**/testDebug/**` alignment at the
first false positive; ktlint/detekt over `build-logic` (§2.5); hoisting the package-task names onto
`ReleaseInvocation` as pinned constants (auditor F-3); the Macrobenchmark module and
`reportFullyDrawn()` (§10); the golden-image suite after R-2 (§8.5); the `:domain:depthSweep` CI job
split at the 25-minute measured threshold (§9.7); closing Diamond-41's opening-book gap (the honest
lever on `check`'s 11 minutes); minSdk 26 when the Play statistics permit (§1.3) — *superseded
2026-10-17 by Q-018: 24 stands until a technical need appears, so this is no longer backlog*; the in-app licences
surface (§16); `docs/gdd.md` (game-designer); a `compose-performance-audit` before release
(rules/compose.md; O-B idle Home); the upstream Gradle issue for `hasResults()` validating existence
but not validity (low priority); the `Pixel_9a` emulator fault preserved as a control (an environment
fault — emulator DNS — not an app change).

## 15. Risks

| Risk | Likelihood / impact | Mitigation | Owner |
|---|---|---|---|
| **The Core Value is asserted on six sampled levels** until R-2 lands; a generator knob tune could ship an unsolvable level | low / severe | R-2's exhaustive ladder sweep (§7.3) is the gate; it blocks the branch cut | android-game-dev, tech-lead |
| The fuzz harness at 10,000 does not fit in 60 s and the default drops to 1,000 | medium / low | The decision rule in §7.3 is pre-written; CI and release stay exhaustive either way | developer measures, reports |
| kover cannot apply beside AGP 9.1.0 / Kotlin 2.3.20, or instruments the sweep | low / medium | kover is applied to `:domain` only (pure JVM); exclusion of `depthSweep`/`fuzz` is an acceptance check; fallback pre-ruled in the brief: Gradle's built-in JaCoCo on `:domain` at the same bound, recorded here as a deviation | developer stops and reports if neither applies |
| `check`'s 11 minutes make the concurrent-build race near-certain on a shared host | high / medium | F1 (self-heal) + F2 (lock); the Diamond-41 book closes the cost later | android-game-dev; producer's dispatch discipline |
| A signed AAB with a placeholder id on disk after a failed build | closed by 9.2.5 | package-task `dependsOn`, proven by execution | — |
| The 2 s cold-start bar has never been measured on a physical device | certain / medium | Recorded as unverified rather than passed; Macrobenchmark follow-up; founder question on a test phone | producer, qa-engineer |
| A future `SCHEMA_VERSION` bump silently deletes the behind device's progress (C-1) | n/a today / severe | §4.4 is a hard gate; both behaviours pass today, which is why it is written down | whoever bumps it |
| `InMemoryPreferencesDataStore` edited without its contract test | low / high | F-11 rule (§8.2) | code-auditor on any diff touching it |
| The configuration cache is switched on without re-auditing the signing capture | low / medium | §9.2.7 names the fix shape | tech-lead on any `gradle.properties` change |
| `ResultsViewModel` at detekt's function ceiling grows a `@Suppress` in a hurry | medium / low | F-4 extraction scheduled (§14.2) | android-game-dev |
| Phase 15 and the R-2 branch both edit `gradle/libs.versions.toml` | medium / low | Disjoint regions; the R-2 branch merges first; phase 15 merges `develop` in | producer |
| Fable is exhausted and a Fable-pinned review cannot run | observed / process | rules/studio.md: the step is not done; the founder's standing answers (Q-003, Q-011, Q-014) are scoped, not general | producer |

## 16. Open items — each with an owner

Every item the seeded file listed as "decided by nobody" is now either decided or has a named owner.

| # | Item | Status | Owner |
|---|---|---|---|
| 1 | `minSdk 24` vs the rule's 26 | **Decided** (§1.3): 24 for 3.0.0, with the reason recorded; the raise is a founder question (installed-base share) the producer queues, non-blocking, with the tech-lead's recommendation. **Answered 2026-10-17 (Q-018, option 3):** 24 indefinitely, unless a technical need appears; nothing is queued, and §1.3 defines what re-opens it | tech-lead, only on a recorded technical need (§1.3) |
| 2 | R-2 / R-7 — kover and fuzz | **Designed** (§7.3, §8.4) and briefed; implementation is the next gate branch | android-game-dev → code-auditor → tech-lead |
| 3 | R-1′ — the gate task | **Implemented** on `feature/phase-14-gate-scope`, auditor PASS; qa-engineer and tech-lead sign-off next; auditor F-2 ruled into the R-2 branch (§9.3) | producer sequencing |
| 4 | In-app licences surface | Studio-rule gap, not a legal one: the OFL notice condition is met by the licence files in the APK, everything else is CC0, Apache-2.0 or a permitted SDK licence. **Backlog v1.1.1**, needs a ux-designer (a Settings row) and a developer | ux-designer, android-game-dev via producer |
| 5 | `docs/gdd.md` | Not a technical item | game-designer via producer |
| 6 | `THIRD_PARTY.md` §5b per-coordinate enumeration, U-1 | Closed on the release branch by `:app:dependencies --configuration releaseRuntimeClasspath` (checklist 3.5) | release-manager |
| 7 | Post-v1.1 backlog | **Recorded** in §14.2 | producer carries it |
| 8 | `AGENTS.md` | rules/stacks.md's scaffold contract wants it identical to `CLAUDE.md`; this product predates the contract and has none. Decision: add it as a copy when the Check command line changes (F2), so both are written once | producer |
| 9 | Release-checklist alignment with R-2: 3.1's command becomes `FUZZ_SEEDS=100000 ./scripts/check.sh`; 3.2's path is `domain/build/fuzz-failures.txt`; 3.3 records the kover figure | Text edits on the release-manager's file | release-manager |
| 10 | The named mid-range physical phone for the 2 s bar | Founder question (money), folded into the release track; decided default recorded in §10 | producer |
| 11 | The phase-15 spec and design-system text the font and cold-start rulings require (`docs/ux-spec.md` §6.5, §6.3, §9, criteria 15e/18d; `docs/ui/DESIGN-SYSTEM.md` Typography) | Routed; until applied the rulings are the governing text | ux-designer via producer |
| 12 | The plugin side of F2 (the stack's Check command carrying the lock) and OFL-1.1 in rules/ip-policy.md's matrix | Plugin changes, not product ones | producer → retro |

## 17. Supersessions

Nothing is deleted from this file; a superseded decision stays with a pointer to what replaced it.

| Superseded | By | Where |
|---|---|---|
| Criterion 15e's "after-median ≤ 950 ms **and** delta ≤ 50 ms" (`tech-lead-architecture.md` §3, 2026-09-21) | The interleaved A/B gate with a same-sitting null floor; 950 ms demoted to a tracked target; the gate is 2 s on a physical phone | §10; `tech-lead-coldstart-gate.md` |
| "Both arms installed and kept installed" (`tech-lead-coldstart-gate.md` §3) | Each arm swapped in only at a block boundary (the arms share one `applicationId`) | §10; `tech-lead-fonts.md` Ruling 2 |
| Host-quiet threshold "1-minute load < 2.0" | ≤ half the host's logical cores (5.0 here), read before and after with the launcher idle | same |
| The downloadable Google Fonts path (`ui-text-google-fonts`, `font_certs.xml`) | Five bundled static OFL files in `res/font` | §11 |
| Item-C partial-migration fallback for the DataStore double (`tech-lead-prerelease-signoff.md` §C) | Retired: all ten classes migrated and green twelve times; the release-manager has nothing to decide | §8.2; `tech-lead-signoff-r3-signing.md` §A |
| `isReleaseInvocation` as `contains("elease")` | A packaging-verb prefix test | §9.2.6 |
| The ID checks on the lifecycle hook only | Also on `packageRelease` / `packageReleaseBundle` | §9.2.5 |
| `adIdSourceRoots` = `src/main` only | `shippedSourceRoots` = main + release + debug, test sets excluded and said so | §9.3 |
| CI running a list of task names | CI running `check` verbatim | §9.7 |
| `KNOWN_SOURCE_SET_NAMES` pre-authorising `androidTest*`, `screenshotTest*`, `testRelease` (R-1 as merged) | Trimmed to the five source sets that exist | §9.3 |
| `bugfix/check-gate-concurrency` as its own branch (`tech-lead-depthsweep.md` F2) | F1 and F2 ride the R-2 gate branch, first two commits | §14.1 |
| "R-2 edits `standards.gradle.kts`" (the file-collision premise in both phase-14 sign-offs) | The fuzz task and kover wiring live in `domain/build.gradle.kts`; only F1 touches `standards.gradle.kts`. The one-branch ruling stands on the shared `check` registration, the same author and the same auditor, not on a textual collision | §14.1 |
| The seeded §9 "Open — decided by nobody yet" (release-manager, 2026-09-21) | §16, each item decided or owned | §16 |
| `versionCode = 3` | `30_000` (merged to `develop` as `feature/phase-14-version-code`) | §9.5 |
| §1.3's routed recommendation: raise `minSdk` to 26 in the first minor after 3.0.0 whose Play statistics show API 24–25 under 1 % of active installs | The founder's Q-018 answer (option 3, 2026-10-17): 24 indefinitely, unless a technical need appears; §1.3 lists what counts | §1.3, §14.2, §16 #1 |
