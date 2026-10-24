# Technical design — Peg Solitaire

**Owner: tech-lead.** This is the product's canonical technical design (rules/stacks.md; release
blocker R-6). It was seeded as a transcription by the release-manager on 2026-09-21
(`.planning/reports/phase-14/release-manager-release-docs.md`, O-5) and taken over, re-framed and
completed by the tech-lead on 2026-10-17 (`.planning/reports/phase-14/tech-lead-tech-design.md`).
Brought up to date on 2026-10-18 (`.planning/reports/phase-14/tech-lead-design-sync.md`) with three
reports: the phase-15 sign-off (`.planning/reports/15-founder-feedback/tech-lead-signoff.md`, on
`feature/phase-15-ux-refresh`), the R-1 gate-scope sign-off
(`.planning/reports/phase-14/tech-lead-signoff-gate-scope.md`, on `develop`) and the candy-backdrop
architecture rulings (`.planning/reports/candy-backdrop/tech-lead-architecture.md`, on
`feature/candy-backdrop`). Synced again on 2026-10-21 (`.planning/reports/phase-14/tech-lead-design-close.md`)
with two more: the phase-15 close (`.planning/reports/15-founder-feedback/tech-lead-signoff-close.md`, on
`develop`) and the tutorial haptics ruling (`.planning/reports/audio-review/tech-lead-haptics.md`, on
`feature/audio-review` — *on `develop` since `6aa7624`, 2026-10-24*). Corrected on 2026-10-21 against `develop` at `0582e3f`
(`.planning/reports/phase-14/tech-lead-release-docs-f8.md`): the code-auditor's F-8 and the tech-design
side of its F-1 (`.planning/reports/phase-14/code-auditor-release-docs.md`), and the AC-F4 pattern the
release-manager found (`.planning/reports/phase-14/release-manager-font-rows.md`). Updated on 2026-10-21 for the
release blockers (`.planning/reports/phase-14/tech-lead-release-docs-blockers.md`): R1 from the game-designer's
hint ruling (`.planning/reports/phase-14/game-designer-hint-timeout.md`) and its merge order from the tech-lead's
brief on `bugfix/hint-undecided`; the upload script; the notices ruling (§13.1); the book counts reconciled (§7.2);
and the licences screens' own branch (`.planning/reports/candy-backdrop/tech-lead-direction-b.md` A9, on
`feature/candy-backdrop`). Synced on 2026-10-24 on the docs-only `feature/tech-design-sync`
(`.planning/reports/phase-14/tech-lead-tech-design-sync.md`) with the edits two sign-offs left to this file: the
release-docs sign-off's N-1a, N-2b, N-4, N-5 and N-8 (`.planning/reports/phase-14/tech-lead-signoff-release-docs.md`)
and the "§14.1" section of the ux-spec-folds sign-off (`.planning/reports/ux-spec-folds/tech-lead-signoff.md`): the
TalkBack board's §14.1 row and merge step 5a, the F-1 row widened with D-03n, D-03o, D-05e and D-05f, §16 #15 and #18
closed by D-05e and D-05f, §16 #22 closed by the game-designer's Amendment 1, two new §16 rows (the gameplay cell-size
deviation; the TalkBack board), §10's start-path list made unconditional, and the lanes cut on 2026-10-24 recorded.
Synced on 2026-10-24 at the R-2 sign-off
(`.planning/reports/phase-14/tech-lead-signoff-check-gates.md`, on `feature/phase-14-check-gates`): the measured
fuzz and coverage figures (§7.3, §8.4), the Check command and its lock as built (§9.1, §9.6), CI's release
triggers (§9.7), the warnings gate as built (§9.8), the solver deadline tests' placement (§8.6) and the
code-auditor's open issues ruled (§14.2, §16 #25–#27). Synced on 2026-10-24 on the docs-only
`feature/tech-design-r1-close` (`.planning/reports/phase-14/tech-lead-tech-design-r1-close.md`) with the closes of
two merges: R1's (`.planning/reports/hint-undecided/tech-lead-signoff.md` §4; merged as `b1a6cf9`) — §7.2's three
outcomes and the book counts confirmed, §8.1's Compose-rule convention, §10's Hint-latency row, §14.1 R1 done,
§15's residual Undecided rate, §16 #21 closed and #29–#31 added — and audio's
(`.planning/reports/audio-review/tech-lead-signoff.md`, "Merge-in re-read" R.4; merged as `6aa7624`) — §2.4, §9.8
rules 2 and 3, §12 and §13 as built, §14.1's audio row done and its merge order, the record-file merge rule,
`bugfix/reduced-motion-live-gate` (§14.1 RM, §15, §16 #28), §16 #17 closed, and §17. The same sync records four
facts already true on `develop`: R-2 done (`dbd0a6c`), steps 3a and 4 done, and row TB's and §16 #24's
preconditions met. Its review loop 1 (`.planning/reports/phase-14/code-auditor-tech-design-r1-close.md`) corrected
row RM's, §15's and #28's counts to `6aa7624`'s (F-1), six precision points (F-2 to F-7; F-6 and F-7 in
`d656263`'s text, §17 and §8.1), and reconciled §16 #30's fallback with "the founder never tests by hand" (I-3). Every decision below is a tech-lead decision or a transcription of a recorded ruling; each names where
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
stable compatible with the rung above) and only outside a release week. The rule's "Android Lint and
Kotlin warnings as errors" (plugin 1.0.0) is ruled in §9.8: errors in `:app`'s main compilations and
in every `:domain` compilation from check-gates C7, and a held deviation for `:app`'s unit-test
compilations and for lint until the first release branch after 3.0.0.

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
| `:domain` | `kotlin-jvm` (convention), `standards` | The game core: `model/` (`Board` as a `Long` bitmask, `BoardSpec`, `Move`, `GameStatus`), `rules/RulesEngine`, `generator/` (`LevelGenerator`, `DifficultyTiers`, `GeneratorConstants`, `ChallengeLevelBounds`, freeform footprints), `solver/`, `assist/` (`OpeningBook`, `OpeningBookFormat`, `AssistAllowance`), `scoring/`, `progress/` (the cloud merge law), `achievement/`, `ads/` (the cadence policy as pure logic), `telemetry/`, `tutorial/` (`TutorialBoards`, the tutorial's own footprints, §3.3), `usecase/` (`GetHintUseCase`, §7.2). Committed opening-book resources under `src/main/resources/golden/book/`. *(Corrected 2026-10-21, code-auditor F-8: this cell omitted `tutorial/` and put `GetHintUseCase` in `assist/`.)* | **Nothing but the Kotlin stdlib.** Test-only: JUnit, Truth |
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
game core. *Decided in:* `docs/tech/audio-silent-mode.md` §1.5 (tech-lead; on `develop` since `6aa7624`,
2026-10-24 — *until then this read "the file is on `feature/audio-review` until that branch merges — N-5,
2026-10-24"*).

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

*One exception, tech-lead, 2026-10-17 (candy-backdrop R4):* the Licenses **list** screen has no
ViewModel. It has no data source, no async work and no event beyond navigation, so a ViewModel would
hold nothing. Its text screen has one (§3.6).

### 3.2 Routes

`navigation/Routes.kt` is the single source of truth for destinations — type-safe `@Serializable`
routes (NAV-02) that the `NavHost` (`AppNavHost`), the catalog, Results and `GameViewModel` all build
against. Destinations: Home, Classic catalog, Gameplay (mode + level), Results, Settings, (phase 15)
Tutorial, and (`feature/candy-backdrop`) `Licenses` and `LicenseText(entryId)` (§3.6). A sixth screen that is a screen everywhere except in the nav graph is a special case
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
  `:domain`'s own `tutorial/` package (`domain/tutorial/TutorialBoards.kt`), not in `model/` with
  `Boards.kt`, and is absent from `Boards.CLASSIC_SPECS`; its completion predicate is
  `board.pegCount == 1` on its own state with no `GameStatus` and no `minimumPegs`. Each prohibition
  is one `grep` over `ui/onboarding/`. The tutorial board is silent by default (every sound cue is
  played by a private effect in `GameScreen`), which rules/audio.md makes compliant; the ux-designer
  and sound-designer own any cue it gains.
  *(Corrected 2026-10-21, code-auditor F-8: this bullet read "lives in `:domain` beside `Boards.kt`",
  the 2026-09-21 ruling's own words. Phase 15 built it in `tutorial/` and was signed off so. What the
  ruling binds is that a tutorial board is never a Classic board, and that holds: `CLASSIC_SPECS` is
  the five Classic boards (`Boards.kt:323-324`). The package is accepted as built, a tech-lead
  decision.)*

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

### 3.5 Ruling — one backdrop behind the `NavHost`, and its heavy layer off Home's first frame

**Tech-lead rulings, 2026-10-17** (`.planning/reports/candy-backdrop/tech-lead-architecture.md` R1 and
R3, on `feature/candy-backdrop`). **Status on 2026-10-18:** those rulings were written against the
sweets landscape, which the founder reopened (Q-020). The founder has since chosen direction **B,
"Deep gloss"** (Q-021, 2026-10-18): a deep violet world with glossy waves, rising soft lights and gel
mode cards. The ux-designer is rewriting `docs/ux/candy-backdrop.md` for B. This section keeps the
parts of R1 and R3 that do not depend on the look, and they bind the B build. The B spec sets the
per-destination variants' names and layer contents; R1's Landscape, Quiet and Sparse are the sweets
spec's. **R2, the stage wings (B-12), is void**: it ruled on the sweets landscape's wings, which
direction B replaces. Anything B puts on Home's
stage or tiles, the gel mode cards included, gets its own fit ruling against D-03l clause 4 and the
phase-15 suites when the B spec lands (§16 #16). Until then nothing on the stage or the tiles is ruled.

- **One instance, in `AppNavHost`, behind the `NavHost`.** Screens draw no backdrop of their own. They
  only stop painting over it: transparent `Scaffold` and `TopAppBar` containers, and a transparent
  root `Surface` on the tutorial. Why not one backdrop per screen: the `NavHost`'s default transition
  cross-fades both screens (`fadeIn`/`fadeOut(tween(700))` in navigation-compose 2.9.8), so two
  translucent per-screen backdrops would dip toward `background` mid-transition. One host also builds
  its geometry once per window size, theme and variant, not on every entry into composition.
- **Where the state lives.** The variant is a pure function of the **committed** back-stack
  destination, a mapping in `navigation/`. Its `else` branch is the calmest variant, so a destination
  added later can never put text on a saturated colour. The backdrop composable (`ui/backdrop/`) is
  stateless with respect to app state: it knows no route and reads no `UiState`. A thin adapter reads
  the back stack, so a destination change recomposes only the adapter and the backdrop. Under
  predictive back the backdrop follows the committed destination, not the gesture, so it never dips.
- **The composition order is load-bearing.** The adapter is composed **after** the `NavHost` in the
  same `Box` and drawn **behind** it with `Modifier.zIndex(−1f)`. It starts from the synchronous
  `navController.currentBackStackEntry`. The `NavHost` restores a saved back stack synchronously while
  it composes, so after a process-death restore frame 1 shows the right variant. An adapter composed
  first would read `null` on frame 1. A KDoc on the `Box` says why, and a `StateRestorationTester`
  restore test pins it.
- **The cross-fade.** One constant, `ClayAnim.NAV_TRANSITION_MS = 700` (today's default made
  explicit), drives the `NavHost`'s four transitions and the backdrop's fade, so the two cannot drift.
  There are two slots. The current variant is opaque below. The incoming one is above, its alpha read
  inside `graphicsLayer { }`. A change mid-fade first promotes the in-flight slot to full alpha. The
  outgoing backdrop is never translucent over `background`. With reduced motion the swap is instant.
- **Per frame.** Every animated quantity is read in the draw phase (`graphicsLayer` or a draw lambda),
  never in composition, so an animated drift is a layer-property update and not a recomposition plus
  a re-layout. Today's `HomeBlobs` reads its drift in composition and feeds `Modifier.offset(Dp)`,
  which lint flags as `UseOfNonLambdaOffsetOverload` (§9.8). Static layers are built in
  `drawWithCache`'s cache block, keyed on size, variant, palette, layout direction and the status-bar
  inset. Draw lambdas allocate nothing: no `Path`, `Stroke`, `Brush`, list or `PathEffect` built per
  frame, indexed loops rather than `forEach`, no per-shape `clipPath`. Whether a layer becomes an
  offscreen texture is decided by measurement at the `compose-performance-audit`, not up front.
- **Geometry is a pure model** (`ui/backdrop/BackdropGeometry.kt`). It holds immutable primitives
  built from px inputs (an `@Immutable` dimens holder resolved once from `dimens.xml`) and named
  ratios. It contains no Compose `Path` and no `android.*` import, so it is tested without composing.
  **The palette:** the hexes live only in `ui/theme/Color.kt`, behind an `@Immutable` holder and a
  `staticCompositionLocalOf` that `AppTheme` provides beside `LocalClayColors`.
- **The F-32 theme `Surface`, which the app lacks today.** `AppTheme` wraps `MaterialTheme` only.
  `AppNavHost`'s root becomes `Surface(color = background, contentColor = onBackground,
  Modifier.fillMaxSize())`, rules/compose.md's F-32 surface. Without it a transparent `Scaffold`'s
  content colour falls back to the default black, and dark-mode text that names no colour turns black.
  Every transparent container also passes `contentColor = onBackground` itself, so a screen is right
  in a preview or a test without the host. Frame 1 gains one solid fill (window background, host
  `Surface`, backdrop base), inside what 15e measures. Accepted.
- **Insets.** The backdrop fills the window under both bars and any cutout and is never padded. Each
  screen keeps the one inset application it has today: the `Scaffold` content padding, and the
  tutorial column's `safeDrawingPadding()`. No scroll container draws under a transparent top bar.
  `MainActivity` is untouched: no `enableEdgeToEdge()`, no `isNavigationBarContrastEnforced`.
  **Known divergence from rules/compose.md F-33**, which has the navigation host pad off the system
  bars once with `safeDrawingPadding()`. Its intent, each bar applied exactly once, is met and tested
  by 18b/18c. Moving the insets up into the host would re-lay out every screen and re-open phase 15's
  15k/18b/18c geometry, so it is not in 3.0.0 (§14.2).
- **The first-frame gate (R3).** One flag, set one frame late, keeps the costly decorative layer off
  Home's first frame. `rememberBackdropSceneReady()` is called once in the host:
  `remember { mutableStateOf(false) }` and `LaunchedEffect(Unit) { withFrameNanos { }; ready.value = true }`.
  The `State<Boolean>` itself, not its value, is provided through a `staticCompositionLocalOf` whose
  default is always true, so previews and component tests show everything. Only the readers that call
  `.value` recompose on the flip. It runs on the Compose frame clock, so the test clock steps it
  exactly. Rejected: a state write from a draw lambda, `delay(16)`, `View.post` and `Choreographer`.
  Frame 1 is the window background, the host `Surface`, the cheap layers and Home's content. The gated
  layer is recorded on frame 2, or frame 3 at the latest. **What the gate covers in B:** every layer
  whose geometry is built from a model, and every full-window fill beyond the first gradient, unless
  the tech-lead rules otherwise when the B spec lands. The spec's degradation steps become one-line
  changes on this flag and are built only if a sitting fails.
- **15e on that branch** (§10). Arm A is `git merge-base develop feature/candy-backdrop` at the
  sitting, and it must contain phase 15. Arm B is the branch head. The instrument is
  `Pixel_9a_bench`. The sitting comes last, before the merge: after the code-auditor's PASS on the
  final diff and after `develop` has been merged in. A jank check runs as a separate block after the
  sitting, never inside it: animations on, `dumpsys gfxinfo` "Janky frames", PASS when
  `median_B ≤ median_A + 1`.

### 3.6 Ruling — the licences screens

**Tech-lead ruling, 2026-10-17** (`candy-backdrop/tech-lead-architecture.md` R4). It does not depend
on the backdrop's look, so Q-021 leaves it standing. It is built on `feature/candy-backdrop` against
`docs/ux/licences-screen.md`.

*Corrected 2026-10-21:* since 2026-10-18 it is built on its own branch, **`feature/licences-screen`**, off
`develop`, not on `feature/candy-backdrop` (`candy-backdrop/tech-lead-direction-b.md` A9: the screen is a
release need whether or not the backdrop reaches 3.0.0). That branch brings `docs/ux/licences-screen.md`,
`tech-lead-architecture.md` and the A9 addendum over in its first commit, and it carries BD-04's Apache-2.0
carve-out. LS-AC08's backdrop rows belong to whichever of the two branches lands second (A9, E7). R4 is
otherwise unchanged. What the screens must list, the Google SDK notices included, is §13.1.

- **Routes** (`navigation/Routes.kt`): `@Serializable data object Licenses` and
  `@Serializable data class LicenseText(val entryId: String)`. Settings opens Licenses, Licenses opens
  a text, and `onBack` pops. **A route carries the entry's stable id, never an asset path.** An id the
  catalogue does not know shows the text screen's error state; it is not a crash and not a file
  lookup. Route arguments are restorable, forgeable state (the `Gameplay(levelId)` and T-05-08
  reasoning), so nothing from a route reaches `AssetManager.open`.
- **The catalogue is code.** `internal object LicenseCatalog` in `ui/licenses/` holds `@Immutable` data
  classes with read-only `List` fields, the codebase's idiom; `kotlinx.collections.immutable` would be
  a new dependency. `enum class BundledLicenseText` is the **only** place an asset path is written:
  `licenses/Fredoka-OFL.txt`, `licenses/Nunito-OFL.txt`, `licenses/Apache-2.0.txt`. Rejected: a
  generated list, a JSON asset (a read and a parser for nine static entries), `oss-licenses` and
  AboutLibraries (new dependencies).
- **The list has no ViewModel** (§3.1's exception). A stateless `LicensesScreen` is fed the
  catalogue. Its `LazyColumn` is keyed by entry id with saveable state, so the position survives the
  round trip to a text.
- **The text screen.** `@HiltViewModel LicenseTextViewModel(savedStateHandle, source,
  defaultDispatcher)` exposes a sealed `LicenseTextUiState`: `Loading`, `Loaded(paragraphs)` and
  `Failed`, which also covers an unknown id and a failed read. The 300 ms spinner threshold is UI
  timing on the composition clock, so the Compose test clock controls it. The body is a `Column` in
  `verticalScroll`, not a `LazyColumn`, because a `SelectionContainer` around a lazy list selects only
  what is composed; the longest text is 11,358 B. It keeps one `Text` per paragraph for TalkBack.
  *Exception, 2026-10-21 (§13.1, LN-4):* a Google SDK notice renders one paragraph per `LazyColumn` item,
  because the largest is 785,183 B in 2,497 paragraphs.
- **Reading off the main thread.** `data/LicenseTextSource` (`suspend fun read(text): String?`) is
  implemented by `AssetLicenseTextSource` on an `@IoDispatcher`, a **new qualifier** in
  `di/DispatchersModule.kt`. It must be qualified because the graph already binds an unqualified
  `CoroutineDispatcher` (Default, `DomainModule`), and a second unqualified binding is a Hilt
  duplicate-binding error. The source catches `IOException` only and returns `null`. The re-flow is a
  pure function, `reflowLicenseText(raw)`: normalise line endings, split on blank lines, join a
  paragraph's lines with single spaces, drop empty paragraphs. It changes whitespace only, and the
  asset stays untouched.
- **`app/src/main/assets/licenses/Apache-2.0.txt`** is byte-identical to
  `https://www.apache.org/licenses/LICENSE-2.0.txt` (11,358 B, sha256
  `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`) and committed unmodified. It adds
  no dependency (§13).

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
| Classic hints answer from a **committed opening book** first (`src/main/resources/golden/book/`), one per board at that board's own depth (`OpeningBook.CATALOG`, `OpeningBook.kt:121-159`): **depth 4** for English-33, European-37, Asymmetric-39 and Diamond-41, **depth 3 for Wiegleb-45** (`wiegleb45-k3.txt`). A depth-K book holds the positions up to K moves from the start (the frontier `DepthSweepTest` re-derives by BFS), so along a line that avoids its gap it answers the first K + 1 hints: five on the depth-4 boards, four on Wiegleb-45. Two books have a documented gap, positions inside their depth that are absent: 440 on Diamond-41 (`diamond41-k4.txt:2`) and 4 on Wiegleb-45 (`wiegleb45-k3.txt:2`). A Challenge level still on its recorded line answers from that line. Everything else falls through to a bitboard solver with a restart policy under a 1.5 s deadline (`SolverBudget.DEFAULT_DEADLINE`). A book "no winning move" is a **proof**, and only English-33's book has any: 4 positions. A book miss is a fall-through. What the player is told when the solver finds nothing is set out below the table | A strengthened solver alone cannot meet the deadline for Wiegleb-45: 0 of 76 depth-≤4 positions resolved even at ~15M nodes/s | ROADMAP phase 8, escalation resolved 2026-08-29; `08-BOOK-EVIDENCE.md`; Wiegleb-45's depth 3 is plan 08-08's checkpoint rescope (`OpeningBook.kt:124-129`). *(Corrected 2026-10-21, code-auditor F-8 and F-1: this row read "committed depth-4 opening book" for all five boards.)* *(Corrected again 2026-10-21, "The book counts, reconciled" below: a book also holds every state of its board's committed golden line past depth K, so along that line every hint is a book answer; and the gap headers' "of 140" and "of 818" are not depth-≤K counts.)* |
| **Diamond-41's fewest-pegs floor is 2, not 1**, proved two ways: a committed 38-move line replayed through `RulesEngine`, and the GF(4) colour invariant checked over all 41 single-peg targets | Star bands anchored on an unreachable one-peg target made the top rating impossible | ROADMAP phase 8 (D-06); plan `08-06` |
| Star bands are derived **per board** from that board's own `minimumPegs` | One shared assumption cannot be right for five geometries | plan `08-07` |
| `DepthSweepTest` runs in its own `:domain:depthSweep` task wired into `check`, double-gated by `TestFlags.RUN_SWEEP` (task property **and** body check), `outputs.upToDateWhen { false }` because it asserts per-call latency against the deadline on this host | The sweep costs ~11 minutes (Diamond-41's 440 off-book positions); the per-commit `:domain:test` gate is held to seconds; neither half silently disables the other | `domain/build.gradle.kts`; `DepthSweepTest` KDoc |
| The sweep stays in `check` at full cost; the honest lever on the 11 minutes is closing Diamond-41's opening-book gap, **not** dropping `upToDateWhen { false }` (a timing gate inheriting a green measured on a differently loaded host is meaningless) | — | `tech-lead-depthsweep.md`, Open issues |

**What "No winning move" means now** (2026-10-24; R1 as built on `bugfix/hint-undecided` and merged to
`develop` as `b1a6cf9`). The game-designer's ruling is `.planning/reports/phase-14/game-designer-hint-timeout.md`
(GD-HT-1 to GD-HT-6, with Amendment 1); the mechanism is the tech-lead's brief
`.planning/reports/hint-undecided/tech-lead-brief.md` §1–§3, with Amendments 1 and 2; the sign-off is
`.planning/reports/hint-undecided/tech-lead-signoff.md`.
- **A hint has three outcomes, and only a proof raises the banner.**
  - `Solver.decide(board, budget, shouldCancel)` returns a `SolverOutcome` (`domain/.../solver/SolverOutcome.kt`).
    `Found(move)` is the first move of a winning line the search completed. `ProvenDead` is earned only three
    ways: the board is already at its target, the Pagoda condition rejects it, or an attempt exhausted its whole
    forward subtree without hitting its node cap and without being cancelled. `Undecided` is every other stop:
    the cancel poll fired, the attempt limit was spent, or the cumulative node ceiling was reached. The solver
    reports what it proved, never why it stopped; `:domain` has no `Cancelled` value and reads no clock (brief §2).
  - `GetHintUseCase` returns a `HintOutcome` (`domain/.../usecase/HintOutcome.kt`), the only outcome type `:app`
    imports. A stored move that is legal on this board (the recorded line, the book) is `Found`. The book's
    `NoWinningMove` is `ProvenDead` without a search, a proof made at authoring time. Everything else goes to
    `Solver.decide`, mapped one to one by `toHintOutcome` (`GetHintUseCase.kt`). That mapping and the ViewModel's
    `when (outcome)` over `HintOutcome` are exhaustive `when`s with no `else`, so a fourth outcome fails the build;
    the book dispatch `when (fromBook)` keeps its `else`, which is the fall-through to the solver. The dependency
    runs use case → solver, never back (§2.2). *(Until loop 1 this read "Both mappings are exhaustive `when`s with
    no `else`" without naming them; code-auditor F-3.)*
  - `GameViewModel.onHintRequested` maps `ProvenDead` to `GameUiState.noWinningMove`, the rescue banner "No
    winning move — undo or restart", and `Undecided` to `GameUiState.hintUndecided`. Both return before
    `pendingHint`, the funnel, the allowance and the gateway, so neither is ever charged (GD-HT-5, ADS-06).
    `showRescueBanner` stays `stuck || noWinningMove`; `hintUndecided` is never part of it.
- **The Undecided notice** (`HintUndecidedNotice`; `docs/ux-spec.md` D-04f and D-04g, criteria 23a–23k) reads
  "Hint couldn't work this position out in time. It may still be winnable." It is free, transient and never
  persisted. It takes the banner's slot on the board, one occupant at a time: the banner when
  `showRescueBanner`, otherwise the notice (`GameScreen.kt`, `UndecidedNoticeSlot`). Its display time
  (`GameAnim.HINT_NOTICE_DISPLAY_MS`, 5 s), its put-away and its "never twice" memory are the screen's own: the
  replayable truth, `hintUndecided`, stays in the ViewModel, and the view's timing stays in the view (§3.1). It is
  one merged semantics node, a polite live region with a `dismiss` action.
- **Deadline versus supersession** (brief §2). One `shouldCancel` lambda, `{ !isActive ||
  deadline.hasPassedNow() }`, unchanged under D-07. A deadline on an unchanged board leaves the job active: the
  outcome, Undecided, is applied and the notice shows (AC-11; GD-HT-3, a latched stop never becomes silence). A
  superseded search, because the board changed or the screen was left, is never observed: `cancelHintSearch()`
  cancels the job, `withContext`'s prompt cancellation discards the result, and the board-identity guard ahead of
  the `when` is the second lock.
- **Where Undecided can appear:** wherever the solver answers and runs out — past each book's depth off the
  board's committed golden line, in Diamond-41's 440 and Wiegleb-45's 4 gap positions, and on a Challenge level
  once the player has left its recorded line (the table above and "The book counts, reconciled" below). The
  only ProvenDead answers are English-33's 4 book proofs, a Pagoda reject, a board at its target and an exhausted
  search. D-07's deadline and every budget constant are frozen (GD-HT-4), so the Undecided rate is a property of
  the device: R1's QA saw 6 of 6 off-book hints Undecided on the founder's GM6 (API 27) and 17 of 17 on the
  emulator (API 36), with Found and ProvenDead unaffected and "No winning move" on no winnable position
  (`.planning/reports/hint-undecided/qa-engineer.md`). As a rate it is unmeasured until R5's census (3.1,
  GD-HT-6; §15).
- **What the tests prove.** Each branch of the change is a named test, written red first
  (`.planning/reports/hint-undecided/android-game-dev.md`). `DepthSweepTest` (AC-8) sorts every solver fallback
  over the covered frontiers into exactly one of the three outcomes: a Found move is legal, ProvenDead comes only
  from an exhausted search, and an Undecided never comes from anywhere but a search. A book proof is served with
  no solver poll.

*Superseded 2026-10-24 (§17): the box and the transcription below describe `develop` before R1 merged. They stay
as the record.*

**What "No winning move" meant until R1 merged** *(until 2026-10-24 this heading read "What 'No winning move'
means today")*. This is a transcription of `develop` at `0582e3f`, not a
ruling; added 2026-10-21 for code-auditor F-1.

> **Ruled 2026-10-21, and replaced when R1 merges.** The game-designer ruled the behaviour below a
> defect, severity MAJOR (`.planning/reports/phase-14/game-designer-hint-timeout.md`, GD-HT-1 to
> GD-HT-6): a hint has three outcomes the player sees, Found, ProvenDead and Undecided, and only
> ProvenDead may raise "No winning move". The fix is R1 on `bugfix/hint-undecided` (§14.1), and it must
> merge before `release/3.0.0` is cut. Its mechanism is the tech-lead's brief on that branch
> (`.planning/reports/hint-undecided/tech-lead-brief.md`): `Solver.decide` returns `SolverOutcome`,
> `GetHintUseCase` returns `HintOutcome`, the ViewModel maps Undecided to `GameUiState.hintUndecided`, and
> the D-07 deadline and every budget constant stay frozen. **Until R1 merges, the text below remains the
> description of `develop`.** When it merges, the tech-lead rewrites this subsection to the three
> outcomes, with §10's Hint-latency row, §15's row and §16 #21 (brief, Handoff).
- `GameViewModel.onHintRequested` raises the rescue banner "No winning move — undo or restart"
  (`nowin_message`, `strings.xml:108`, chosen at `GameScreen.kt:241`) on **any** `null` from
  `GetHintUseCase` (`GameViewModel.kt:1384-1385`). That `null` has two sources, and they differ in
  strength:
  - **The book's `NoWinningMove` is a proof.** It comes from a search that completed at authoring
    time, and it short-circuits without a solver call.
  - **The solver's `null` is not a proof.** It folds an unsolvable position, a budget miss and a
    cancellation into one `null` (D-04, `GetHintUseCase.kt:30-34`). So it means "no winning move found
    within 1.5 s". The ViewModel's own comment says a budget miss reaches the banner
    (`GameViewModel.kt:1381-1383`). Such a hint is never charged (ADS-06).
- **So the banner is a proof only when the book gave the answer.** Wherever the solver answers, it
  can appear on a winnable position:
  - past each book's depth, on every Classic board, off the board's committed golden line (the line's
    own states are book entries at every depth; see the reconciliation below);
  - in Diamond-41's and Wiegleb-45's gaps;
  - on a Challenge level once the player has left the recorded line.
- **The measured worst case is Wiegleb-45.** Before the book, none of its 76 depth-≤4 positions
  resolved inside the deadline on the measured host (`GetHintUseCase.kt:54-58`; `DepthSweepTest`
  KDoc). Its book stops at depth 3, so every position four moves from the start is a book miss and
  goes to a search that found no answer in time on any of them. *(Corrected 2026-10-21: every such
  position but one. The committed golden line's state at depth 4 is a book entry, and it is exactly the
  position a player reaches by following Hint from the start; see the reconciliation below.)*
- **What the tests prove.** Over every covered position, `DepthSweepTest` asserts three things: a
  book hit is legal on its board, a book proof is served without a search, and a solver answer is
  either `null` or a legal move. It does not, and cannot, make a solver `null` a proof. Nothing else
  in the code does either, so no text should describe one as a proof.
- **The product question stays open (§16 #21).** Whether the banner's wording or the behaviour on a
  budget miss should change belongs to the game-designer and the ux-designer
  (`code-auditor-release-docs.md`, Open issues). Any code change that ruling needs comes back here.
  *Superseded 2026-10-21:* ruled; see the box at the head of this subsection and §16 #21.

**The book counts, reconciled** (2026-10-21; the count mismatch the game-designer's ruling raised in
its Open issues, and the hint brief in its own). Each book header's `entries=` and each gap header's
"N of M" disagree with the frontier `DepthSweepTest` examines. All of the difference is book entries
the sweep never reaches, and they are the states of the board's committed golden line past depth K.

| Board | K | Golden-line moves | Header `entries=` | Depth-≤K frontier (sweep `examined`) | Covered (served + proven dead) | Absent (gap) | Line states past K (moves − K − 1) |
|---|---|---|---|---|---|---|---|
| English-33 | 4 | 31 | 399 | 373 | 373 (369 + 4) | 0 | 26 |
| European-37 | 4 | 35 | 639 | 609 | 609 | 0 | 30 |
| Asymmetric-39 | 4 | 37 | 493 | 461 | 461 | 0 | 32 |
| Diamond-41 | 4 | 38 | 378 | 785 | 345 | 440 | 33 |
| Wiegleb-45 | 3 | 43 | 136 | 101 | 97 | 4 | 39 |

- **Two identities hold on every board:** `entries` = covered + line states past K (399 = 373 + 26 …
  136 = 97 + 39), and frontier = covered + absent (785 = 345 + 440; 101 = 97 + 4). The gaps the brief
  and the ruling compare agree: 4 and 440.
- **Why the books hold more.** The authoring tool seeds every state of the committed golden line
  (`golden/solutions/<board>.txt`) with that line's own move before it decides the frontier, and it
  writes `frontier + prefixes` (`OpeningBookAuthoring.kt`, `seedFromCommittedLine` and
  `collectEntries`). Read from the five book files: past depth K there is exactly one entry per depth,
  and its move is the golden line's move at that depth.
- **Why the headers say 140 and 818.** The gap note's "of M" is `undecided + entries`, so it counts the
  line states too. **Quote the frontier instead:** Wiegleb-45 lacks 4 of its 101 depth-≤3 positions, and
  Diamond-41 lacks 440 of its 785 depth-≤4 positions. "440 of 818" and "4 of 140" are wrong as position
  counts, in the ruling's reach table and in `08-VERIFICATION.md`'s override 1.
- **Why the sweep never reaches them.** It walks the depth-≤K frontier only. Play does reach them:
  `OpeningBook.lookup` has no depth check, so a line state past K is a book hit, instant, with no solver
  poll.
- **What that means for a player.** At every golden-line state the book's answer is the line's own
  move. A player who follows Hint from the untouched start therefore stays on the golden line, and
  every hint along it is a book answer, on all five boards. The solver is reached only off that line.
  The ruling's "Any position four jumps into Wiegleb-45 qualifies, whether or not the player followed
  Hint to get there" holds for every position four jumps in **except** the golden line's own. **AC-18's
  first scenario cannot pass as written:** after four followed hints, the fifth is an instant Found from
  the book, not Undecided. AC-2 is safe, because its precondition asserts the book miss. Routed in §16 #22.
- **The books stay as they are for 3.0.0.** They are frozen (GD-HT-6; the brief's §6). The gap note's
  denominator is fixed in `OpeningBookAuthoring.kt` when R2 re-authors the books (3.1).
- **Confirmed at R1's close** (2026-10-24, `hint-undecided/tech-lead-signoff.md` §4 item 5). The hint brief's
  open bullet compared two boards, the gapped ones: 136 = 97 + 39 (Wiegleb-45) and 378 = 345 + 33 (Diamond-41),
  against the sweep's frontiers of 101 and 785 positions at depth ≤ K. The third figure the sign-off names is
  this table's own, not the brief's: 399 = 373 + 26 (English-33, whose 373 covered include its 4 proven-dead
  entries). All three read the same in the book headers (`entries=` 136, 378, 399). This subsection closes that
  bullet ("The count mismatch the ruling raised is not reconciled here", brief, Open issues). *(Until loop 1 this
  read "The three header figures the hint brief's open bullet compared"; code-auditor F-2.)* R1 changed no book (brief §6). One stale
  KDoc remains: `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt:250` still
  names `Solver.firstWinningMove`, which R1 deleted in favour of `Solver.decide`. It sits in the frozen golden
  package (brief A2.3), so it is fixed by the 3.1 R2/R2b item that lifts that freeze.

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

**Measured figures** (R-2 as built: C4 `2520fa7`, C5 `e5f4e9b`; transcribed at sign-off 2026-10-24 from
`android-game-dev-check-gates.md` §7 and `code-auditor-check-gates.md` item 3). The harness method's own
`time=` from the JUnit XML, `--rerun-tasks`, on a quiet host (1-minute load 2.7–3.4, no other build):
1,000 levels in 0.495 s and 0.540 s (≈ 1,850–2,020 levels/s over both passes); 10,000 levels in 5.989 s and
**5.955 s** (≈ 1,679 levels/s). The code-auditor re-timed 10,000 at 5.982 s (load 2.9–3.5). Rule 1 of item 7
holds with a ten-fold margin, so **`fuzzSeedsDefault = "10000"`**: a local `check`, CI and the release all
walk the whole level space (`examined 10000`, determinism pass, 0 failures, in every green run). The `check`
wall clock, both with `--rerun-tasks`: 13 m 26 s before the branch (807 s, 98 tasks, a loaded host, load
42 → 22) against 11 m 48 s after (710 s, 104 tasks, load 15 → 3); the code-auditor's run 11 m 32 s (694 s).
The delta is host noise; the branch's real cost is the fuzz (≈ 5–6 s inside the run) plus kover's
instrumentation, report and verify, a few seconds, so the ≈ 11–12 minute figure stands and is still the
sweep's. As built, `generate(n)` is `generateLadder(n).last()` with the clamp inside the sequence (item 2's
`clamp(n)` is idempotent); the golden fixtures are unchanged and green, and the auditor's `level % 97`
certificate mutation was caught by the harness alone (`:domain:test` stayed green), which is the harness's
value. **A cache hit is a pass, not a gap** (auditor finding 2): the task keeps Gradle's up-to-date and
build-cache semantics by item 8, so an unchanged tree at the same count comes back `FROM-CACHE` and prints
no `fuzz:` line; the lines are in the restored JUnit XML's `system-out`. The release run therefore passes
`--rerun-tasks` (checklist 1.2 and 3.1, `77dcf88`), so R-7's evidence is the release's own execution.

## 8. Testing

### 8.1 Where tests live and what runs where

| Suite | Runner | Gate | Cost |
|---|---|---|---|
| `:domain:test` — 294 tests in 33 classes, from 41 `.kt` files, 33 of them `*Test.kt` (measured on the merged tree `ffa49aa`, check-gates' `develop` merge-in, 2026-10-24: the JUnit XML of that merge's Check, 33 suites, 0 failed or skipped; *until that merge-in's re-read this cell read "280 tests in 40 files at check-gates" — 280 tests there, but git counts 41 `.kt` files at `c632b45` too; before the merge-in, "37 `.kt` files, 30 of them `*Test.kt` (counted on `develop` at `4ff4ec6`, 2026-10-24, N-8)", and before that "36 test files"*): rules, generator properties, golden fixtures, solver, book, scoring, merge law, the ladder and `FUZZ_SEEDS` parse tests | JVM, JUnit4 + Truth, under kover's agent (§8.4) | per commit, in `check` | ~30 s |
| `:domain:depthSweep` | JVM, own task | `check` | ~11 min (§7.2) |
| `:domain:fuzz` — `FuzzHarnessTest` | JVM, own task, outside kover | `check` at 10,000 = CI = release (exhaustive, §7.3) | ≈ 6 s (5.955 s at 10,000) |
| `:domain:koverVerify` | kover 0.9.11 over `:domain:test` only | `check` (§8.4) | seconds |
| `:app:testDebugUnitTest` — ViewModels (Turbine), repositories against the real in-memory Room DB, Robolectric Compose UI tests (one per screen's main flow), ArchUnit boundary tests, codec and merge tests | JVM, Robolectric 4.16 SDK 34 (the only `android-all` image cached offline — **do not add `@Config(sdk = ...)` for another level**: it needs a network fetch and breaks `check --offline`) | `check` | minutes |
| `build-logic` tests (54 at R-1; 54 after check-gates C3, whose brief count of 55 was wrong, addendum A0; 55 after C3a) — the release-gate resolvers | JVM | `check`: the root `build.gradle.kts:18-20` makes it depend on the included build's `:check` (PLAT-06, `7b28007`, 2026-08-27), and the 55 were part of the merge-in's Check on `ffa49aa`, served from the build cache ("build-logic 55 (cache)", `phase-14/android-game-dev-check-gates-merge.md:34`; *until r1-close loop 1, code-auditor F-7: "the 55 ran in the merge-in's Check"*); CI also runs them first, alone, to fail fast (`./gradlew -p build-logic test`, §9.7) *(until check-gates' merge-in re-read, 2026-10-24: "CI's first step (`./gradlew -p build-logic test`); not reachable from the root `check`", stale since `7b28007`)* | seconds |
| Device sessions | qa-engineer, real device else emulator | every executed plan, every phase, every release | — |

Deterministic tests only: no wall clock, no network, no unseeded random, no sleeps (the four solver
deadline tests are the recorded exception, §8.6). No `@Ignore`,
disabled test or suppression without the code-auditor's approval line. Test names read as sentences.
TDD for core logic.

**The Compose test rule (tech-lead decision, 2026-10-24; `hint-undecided/tech-lead-signoff.md` §2, finding 4).**
The repo's default stays `androidx.compose.ui.test.junit4.createComposeRule` (v1). The `junit4.v2` rule is allowed
in a test class only where a test needs a behaviour the default rule does not give, and that class's KDoc says
which one. One class uses it today, `GameScreenHintNoticeTest`, for the paused-clock recreation and the tap-write
delivery its developer documents. Its KDoc sentence is added by the first branch that next touches that file. No
other class migrates (33 suites import the v1 rule on `develop` at `6aa7624`) until one deliberate decision: the
migration of every suite at once that §9.8 rule 2 holds and §14.2 schedules.

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
scope and exclusions here. **Measured floor and bound (C6 `42d4812`, transcribed 2026-10-24):** kover
**0.9.11** (`org.jetbrains.kotlinx.kover`, JetBrains, Apache-2.0, build-only; the newest stable on Maven
Central and the Plugin Portal on 2026-10-23 by `scripts/latest.sh`), applied beside AGP 9.1.0 and Kotlin
2.3.20 with no JaCoCo fallback. Root LINE counter of `domain/build/reports/kover/report.xml`: 896 of 905
lines, **99.0 %** (INSTRUCTION 97.8 %, BRANCH 92.3 %), re-measured by the code-auditor at the same figure;
so the bound is the rule's **`minBound(90)`** and no raise is scheduled. `koverVerify` attaches itself to
`check`; its graph holds `:domain:test` and neither `depthSweep` nor `fuzz` (`disabledForTestTasks`),
proven by `-m`; a bound of 100 fails naming the rule. One online warm run was needed for the plugin's
artifacts (PLAT-07/D-18); every run since is `--offline`. Known cost: kover's agent slows `:domain:test`'s
four wall-clock solver deadline tests about 2.5× — ruled in §8.6.

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

*Pointer, 2026-10-17 (`candy-backdrop/tech-lead-architecture.md` R5):* stored goldens stay out, but
**pixel comparisons inside one test run** are allowed. Two renders in the same JVM, on the same host
and in the same test, are compared in memory: with and without the backdrop, font scale 1.0 against
2.0, LTR against mirrored RTL. Nothing is stored and there is no baseline to drift, so neither
objection above applies. An exact-equality pixel test that meets shader dithering states a
±1-per-channel tolerance as a named constant and keeps the assertion.

### 8.6 The solver deadline tests — placement ruling (2026-10-24)

Four tests in `:domain:test` assert the product's 1.5 s hint budget (§10) on the wall clock:
`SolverTest.resolvesTheEnglish33FullStartInsideTheProductionDeadline`, `…European37…`, `…Asymmetric39…`
(a `System.nanoTime()` `shouldCancel` over `SolverBudget.DEFAULT.deadline`, the envelope `GameViewModel`
builds) and `SolverBudgetTest.everyTractableSolvablePositionReturnsAMoveWithinBudget` (a per-call cap).
They predate R-2 and they are the recorded exception to §8.1's "no wall clock": the budget is a product
fact, and a test that measures it measures the host too. Evidence (`android-game-dev-check-gates.md`,
`code-auditor-check-gates.md` finding 4): on a quiet host they take 0.17–0.40 s under kover's agent (a 4–9×
margin); at 1-minute load 20–25 the agent makes them 2–3× slower (European-37: 2.04 / 1.95 / 1.49 s against
0.72 / 0.84 / 0.74 s without it); at load 20 → 69 three went red in the developer's first final Check,
and one pair went red with the agent **off**. So host load alone breaks them; kover raises the frequency,
not the kind.

**Ruling.**
1. *Placement, not deletion, and not a wider margin.* A deadline test loosened to survive load measures
   nothing; one deleted drops the DEBT-01 regression guard. The four move to their own class,
   `SolverDeadlineTest`, run by their own `Test` task `:domain:solverDeadline` in the sweep's shape: wired
   into `check`, `outputs.upToDateWhen { false }` (a timing assertion inherits no green from a differently
   loaded host, §7.2), and **outside kover's instrumentation** (`disabledForTestTasks`), so the agent's 2.5×
   is gone and `:domain:test` stays the per-commit gate. The solver's lines stay covered by `SolverTest`'s
   and `SolverBudgetTest`'s remaining cases, `GetHintUseCaseTest` and R1's `Solver.decide` tests; the
   developer measures `koverVerify` after the move and reports the figure (≥ 90 stands; 99.0 % leaves room).
2. *Not on check-gates.* `bugfix/hint-undecided` rewrites both files to `Solver.decide` and merges first
   (§14.1 step 3a); a move here would collide with a reviewed branch. It is its own small branch,
   `bugfix/solver-deadline-task`, cut from `develop` after R1 and check-gates have merged: android-game-dev
   → code-auditor → tech-lead; no device session (test placement only, no product code).
3. *Not a branch-cut blocker.* The release Check (checklist 3.1) runs on a quiet host by the
   release-manager's own rule, under §17's host-quiet threshold (1-minute load ≤ half the logical cores,
   5.0 here), where the margins hold; a red there is a re-run, fail-closed, never a wrong artifact. It lands
   before the cut if it is green in one review loop (R-8's pattern) — the producer's schedule — else in the
   first post-3.0.0 batch (§14.2), and the quiet-host rule governs every Check until then.
4. *Operator rule until it lands:* read the 1-minute load before a Check; above the threshold, wait or
   expect this red and re-run; never widen the deadline.

## 9. Build, signing and gates

### 9.1 The Check command

`CLAUDE.md` and `AGENTS.md`: `Check command: ./scripts/check.sh` (since check-gates C2, `139b418`: the
script runs `./gradlew check "$@"` under the per-worktree lock, 9.6). `check` runs, for
both modules: detekt (one widened task, 9.4), ktlintCheck, `dpSpLiteralCheck`, `adIdLiteralCheck`,
`gamesIdLiteralCheck`, `audioFocusLiteralCheck`; Android lint; `:domain:test`, `:domain:depthSweep`,
`:app:testDebugUnitTest`; and since R-2 (C5, C6), `:domain:fuzz` and `:domain:koverVerify`. Since check-gates
C7, a Kotlin warning in `:app`'s main compilations or in any `:domain` compilation fails the
compilation, and so the Check (§9.8). **A new gate is
registered in `standards.gradle.kts`'s `check` block (module-wide gates) or the owning module's
`check` dependency (module-specific tasks), and nowhere else** — CI invokes `check` verbatim (9.7), so
that is the one registration CI cannot forget. Wall clock ≈ 11.5–12 min with `--rerun-tasks` on a quiet host
(11 m 32 s and 11 m 48 s at the R-2 sign-off, §7.3), dominated by the sweep; the fuzz adds ≈ 6 s and kover a
few seconds.

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

The three ID gates read every file under `src/main`, `src/release` and `src/debug`
(`SourceSetPolicy.shippedSourceRoots()`; the manifest, `assets/`, Java `resources/` and any later
content type included), filtered by `exists()`. **Test source sets are excluded on purpose
and the failure messages say so**, derived from the list (`SHIPPED_SOURCE_ROOTS_DESCRIPTION`), because
a gate whose failure text misdescribes its own scope is how F-1's false claim happened. `src/debug` is
not cosmetic: a real ad unit id there is AdMob invalid traffic against the account, and a focus request
there falsifies every QA audio observation. `src/test*` legitimately names the test-publisher prefix,
the key names and the focus API. *Decided in:* `tech-lead-prerelease-signoff.md` §B; implemented R-1,
PASS `code-auditor-gate-scope.md` (planted probes in all five source sets, negative control included);
signed off PASS in `tech-lead-signoff-gate-scope.md` and merged to `develop` as `c3d3437`.

*Amended 2026-10-18 (`tech-lead-signoff-gate-scope.md` ruling 2, on QA's open issue 2):* the
per-directory {java, kotlin, res} list had a hole at the manifest. That is where AdMob's setup puts the
app ID, and the manifest's own comment (`AndroidManifest.xml:50-54`) claimed the gate covered it.
`assets/` (phase 15's licence texts) and `:domain`'s Java `resources/` (the opening book) ship too and
were unscanned. The source-set list is now the only list, and the tripwire guards it. Implemented in
check-gates C3a; with it the manifest comment becomes true, so `AndroidManifest.xml` is not edited.
On every tree due to merge (R-1, phase 15, audio, candy) the four markers occur in 0 files under the
shipped source sets, binaries included, so the widened gates start green.

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

**As built and signed off (check-gates C1 `348279d`, C2 `139b418`; `tech-lead-signoff-check-gates.md`,
2026-10-24).** F1 is the ruled block, verbatim, in `standards.gradle.kts`; its red-first evidence is the
planted 0-byte store (`:domain:test`, `:app:testDebugUnitTest`, and the sweep's store planted immediately
before the final Check, which healed inside it, twice for the developer and once for the auditor). F2 is
`scripts/check.sh` with four accepted departures from the brief's letter: the signal traps *exit*
(129/130/143) and the EXIT trap releases, all set only after the `mkdir` succeeds, so a signal landing before
Gradle starts cannot leave Gradle running unlocked and a queued run cannot delete the holder's lock; the
script `cd`s to the worktree root first (from a subdirectory, `gradlew check` would run that subproject
only); an empty PID file gets a one-second grace; a `--wait` run re-checks staleness on every poll. Gradle is
a plain child, never `exec`. `AGENTS.md` is a byte-identical copy of `CLAUDE.md` (§16 #8). Behaviour under
signals, measured: a TERM to the script alone is acted on when Gradle returns (bash defers a trapped signal
while a foreground child runs), so the lock is held for the whole build and released after it — the wanted
behaviour. **Known limit, recorded (auditor finding 3; the brief's own stale-reclaim rule):** after
`kill -9` of the script (a harness timeout is the realistic sender) the lock holds only a dead PID and the
next run reclaims it at once, while the orphaned Gradle client finishes its build — two builds in one
worktree for the orphan's remaining minutes, M1's shape. F1 bounds the damage to a false red and a retry,
never a wedge, and no artifact can be wrong, so 3.0.0 ships with it. *Fix shape, ruled:* record the Gradle
child's PID in the lock as well (`"$root/gradlew" … &`, then `wait`, the TERM trap forwarding to the child
before it exits) and reclaim only when both PIDs are dead; it lands with the next edit of `scripts/check.sh`,
post-3.0.0 (§14.2), together with the INT comment the auditor found overstated (finding 6: under bash 3.2
a SIGINT delivered to the script alone while Gradle runs is discarded when Gradle exits normally, and the
script exits with Gradle's status; the lock is still released). *Operator rule meanwhile:* a Check is run
in the background or in a driver script with a timeout above its wall clock (the harness's foreground cap
is 10 minutes, the Check ≈ 12), and after any Check killed with SIGKILL, `pgrep -fl GradleWrapperMain` and
wait for the orphan before the next Check in that worktree.

### 9.7 CI runs `check`, verbatim

`.github/workflows/ci.yml` runs `./gradlew -p build-logic test` (first, fail-fast) then
`./gradlew check assembleDebug` on every push and PR to `develop` and `main`: those are the only
triggers on `develop` today (`ci.yml:7-11`). **`release/**` is ruled and built:** check-gates C3b
(`8687fca`) sets push and pull_request to `[develop, 'release/**', main]` (rules/stacks.md scaffold
contract 8; `tech-lead-signoff-gate-scope.md` ruling 1; §14.1), signed off 2026-10-24 and live on
`develop` once that branch merges. *(Written 2026-10-18: until that branch merges, a push to a `release/*`
branch would trigger no CI run.)*
*(Corrected 2026-10-21, code-auditor F-8: this paragraph stated `release/**` as a current trigger.)*
Push as well as pull request, because the studio merges locally with `git merge --no-ff`: once a
remote exists, a pushed branch is the only event GitHub sees. The repository has no git remote today,
so the workflow has never run. The local Check is the only gate that has executed, and the 25-minute
split trigger below waits for the first remote run; a local `check` time is not that figure. It used
to name tasks and so never ran the four literal gates. **Ruling on the sweep's CI cost:** stays in the same job
until the measured wall clock of the `verify` job exceeds **25 minutes**; then `:domain:depthSweep`
moves to its own parallel job in the same workflow whose union with the first job is still `check`
(never back to a list of task names). The producer records the first measured figure in STATUS.md.
Since R-2 (C5) the step carries `FUZZ_SEEDS: "10000"` explicitly whatever the local default (§7.3). The
step's own comment at `ci.yml:58` still calls `check` "the Check command in CLAUDE.md" (auditor finding 7;
the header at `:3-8` is right): fixed with the next edit of the file (§14.2). The build-logic step's comment at
`:48-49` says those tests are "not part of the root `check`"; the root `check` has reached them since `7b28007`
(§8.1), so the step is a fail-fast first run, not their only one, and the comment is corrected in the same edit
(check-gates' merge-in re-read, 2026-10-24).

### 9.8 Kotlin and lint warnings — ruling (2026-10-18)

The plugin's rules/kotlin-android.md (1.0.0) has the Check run "with Android Lint and Kotlin warnings
as errors". Neither is configured: there is no `allWarningsAsErrors` and no `lint { }` block in
`build-logic/`, `app/build.gradle.kts` or `domain/build.gradle.kts`. **Tech-lead ruling
(`.planning/reports/phase-14/tech-lead-design-sync.md`): the main code becomes warning-free and gated
now, as a small developer task. The unit-test compilations and lint are a held deviation for 3.0.0.**

**The count.** The newest Check log on disk is `./gradlew --no-daemon --no-build-cache --rerun-tasks
check` on `feature/phase-15-ux-refresh` at `e1875cb` (2026-10-17): green, 98 of 98 tasks executed,
**239 `w:` lines**. *(2026-10-24, r1-close loop 1, code-auditor F-4: one row, the `createComposeRule` row, has
since been re-measured on the merged tree `6aa7624` (33, was 29), so the Lines column now sums to 243 and the
table mixes two trees. The 239 is the `e1875cb` recount; every other row is that tree's.)*

| Where | Lines | Kind | Fix |
|---|---|---|---|
| `:app` main | 6 | `androidx.hilt.navigation.compose.hiltViewModel` is deprecated, "moved to package `androidx.hilt.lifecycle.viewmodel.compose`", in `ClassicCatalogRoute.kt:29`, `GameRoute.kt:49`, `HomeRoute.kt:59`, `TutorialRoute.kt:33`, `ResultsRoute.kt:63` and `SettingsRoute.kt:28` | The import. In `hilt-navigation-compose` 1.3.0's sources the deprecated function is an inline forwarder to the new one, so behaviour does not change |
| `:app` main | 4 | Kotlin's annotation default-target change: a qualifier on a constructor `private val` "is currently applied to the value parameter only", in `ChallengeProgress.kt:54`, `ClassicLevelSource.kt:40`, `InterstitialCadenceStore.kt:98` and `TutorialViewModel.kt:76` | An explicit `@param:` use-site target. That is today's behaviour, written down |
| `:domain`, main and test; `build-logic` | 0 | — | — |
| `:app` unit tests (`src/test`, `src/testDebug`) | 198 | Opt-in required for `kotlinx.coroutines.ExperimentalCoroutinesApi`, the coroutines test API | A compiler opt-in on `:app`'s unit-test compilations only |
| same | 33 on the merged tree `6aa7624` (33 suites import the v1 rule; audio's merge-in Check printed 33 such lines, run on `9f0e67a`, the merge-in `13c04c6` plus a KDoc-only commit, `audio-review/android-game-dev-merge.md:3, 30`); *29 in this count at `e1875cb`, until 2026-10-24* | `createComposeRule` is deprecated for `junit4.v2.createComposeRule`. The v2 rule's `StandardTestDispatcher` queues work that the v1 rule runs at once | A migration that changes how 33 Compose suites synchronise (*29 until 2026-10-24*), the phase-15 geometry suites among them |
| same | 1 | `GameUiState::class.members` (`GameScreenSoundTest.kt:300`) needs `kotlin-reflect`, which reaches the test runtime only transitively | Java reflection, or a declared test dependency |
| build environment | 1 | "Detected multiple Kotlin daemon sessions" | Not a compiler diagnostic about the code |

**Lint** in the same run (`lint-results-debug.txt`): **0 errors, 57 warnings.**
- 35 are "a newer version exists" checks: `GradleDependency` 18, `NewerVersionAvailable` 14,
  `AndroidGradlePluginVersion` 3. The ladder rule and the compileSdk-37 wall (§1.2) forbid acting on
  them in a release week, and some cannot be acted on at compileSdk 36 at all.
- `UnusedResources` 13 (dimens and strings).
- `PluralsCandidate` 5: user-facing strings, the ux-designer's copy.
- `UseOfNonLambdaOffsetOverload` 2: `HomeBlobs`, which the backdrop branch replaces (§3.5).
- `TypographyDashes` 1, and `FieldSiteTargetOnQualifierAnnotation` 1 (`App.kt:90`).

**Ruling.**
1. **Developer task: check-gates commit C7**, `feat(14): fail the build on Kotlin warnings in shipped
   code`, on `feature/phase-14-check-gates` after C6. That branch is cut from `develop` only after
   phase 15 has merged (§14.1), and two of the ten sites are phase-15 files. Scope:
   - `allWarningsAsErrors` on every main compilation of `:app` (debug and release) and on every
     compilation of `:domain` (main and test), set once in the convention plugins
     (`android-app.gradle.kts`, `kotlin-jvm.gradle.kts`) with a comment pointing here. `:app`'s
     unit-test compilations are exempt by name, in the same place.
   - The opt-in of `kotlinx.coroutines.ExperimentalCoroutinesApi` on `:app`'s unit-test compilations
     only, never on a main compilation.
   - The ten main-source fixes in the table, and nothing else in `ui/` or `data/`.
   - `androidx.hilt:hilt-lifecycle-viewmodel-compose` declared directly, because the code now imports
     it. It takes the `hiltNavigationCompose` version ref (1.3.0), so the two cannot drift; 1.4.x needs
     compileSdk 37. It is already on the release runtime classpath through `hilt-navigation-compose`
     1.3.0, so nothing shipped changes.
   - Not in C7: no test migration, no lint setting, no `@Suppress`, no `-Xwarning-level`, `-nowarn`
     or `suppressWarnings`, no resource or string edit.
   - The acceptance is addendum A3 of the design-sync report.
2. **Held deviation for 3.0.0: `:app`'s unit-test compilations and Android Lint keep warnings as
   warnings.**
   - *Why, tests:* after the opt-in, what remains is the `createComposeRule` v2 migration. It changes
     how 29 reviewed Compose suites synchronise (33 on `6aa7624`, 2026-10-24), in a release week, after 101 reviewed phase-15
     commits, and the backdrop branch must run the phase-15 suites unchanged. It is a change to test
     behaviour, not a cleanup.
   - *Why, lint:* about two thirds of its warnings are version checks the pins are held against for
     recorded reasons. The rest touch user-facing strings, and resources the backdrop branch is
     rewriting. Making them errors now means disabling checks or adding a baseline, and both hide
     warnings rather than fix them.
   - *While it holds, no growth:* a diff that adds a unit-test compilation warning, or a lint warning,
     in a file it touches is a review finding, fixed in that diff. The code-auditor reads the Check
     log's `w:` lines and `app/build/reports/lint-results-debug.txt` for the diff's files.
   - *One recorded exception (tech-lead decision, 2026-10-24, `audio-review/tech-lead-signoff.md` §2.4):* new
     Compose suites follow the house `createComposeRule` until the §14.2 migration moves all suites at once. The
     v1 rule's deprecation line that such a suite adds is not a review finding under the clause above, because a
     module with some suites on v1 and some on v2 would be the worse state. The v2 rule is used only as §8.1
     allows.
   - **End:** before the first `release/*` branch after `release/3.0.0` is cut; a `hotfix/*` is
     exempt. Closing it means the v2 migration and the reflection call, `allWarningsAsErrors` on the
     unit-test compilations, and `warningsAsErrors` in lint. The version checks there are either acted
     on by the ladder or disabled by a line the code-auditor approves, with its reason. Owner:
     android-game-dev via the producer, then the code-auditor and the tech-lead (§14.2).
3. **Every branch merged after C7 compiles its main code warning-free.** A new main warning fails that
   branch's Check once it has taken `develop` in. It is fixed on that branch, in the code, never by a
   suppression or a compiler switch. If check-gates merges after `feature/audio-review`, C7's
   acceptance runs on the merged tree. The same two patterns, the import and the `@param:` target,
   then apply to audio's code too; any other kind of warning stops the developer and comes to the
   tech-lead. *(2026-10-24: the "if" is history. Check-gates merged first, as `dbd0a6c`; audio took it in
   at `13c04c6`, and audio's main code compiles with 0 `w:` in debug and release, with no warning-fix
   commit (`audio-review/android-game-dev-merge.md`). The rule itself stands for every later branch.)*

**As built and signed off (C7 `f15e58a`; 2026-10-24).** The recount on the cut (`0582e3f`, 2026-10-21,
`--rerun-tasks --no-build-cache`): 253 `w:` lines — 10 in `compileDebugKotlin` and the same 10 in
`compileReleaseKotlin` (the table's ten sites exactly), 0 in `:domain` and `build-logic`, 232 in
`compileDebugUnitTestKotlin` (202 `ExperimentalCoroutinesApi` opt-ins, four more than the table from
phase-15 tests; 29 `createComposeRule`; 1 kotlin-reflect), 1 environmental. After C7: 0 from any
`/src/main/`, `/src/debug/`, `/src/release/` or `domain/` path, `:app:compileReleaseKotlin` clean with no
`PLAY_UPLOAD_*` set, and a residue of 30 in the unit tests (29 + 1; the opt-in lines are gone). The flag is
`allWarningsAsErrors` on every `KotlinCompile` of `:app` except the tasks named `…UnitTestKotlin`, and on
every compilation of `:domain` (`kotlin-jvm.gradle.kts`); the opt-in rides the same exemption. The
code-auditor's probes failed each gated compilation (main, debug, release, `:domain` main and test) and
warned in each exempt one, and a `flatMapLatest` without `@OptIn` in `app/src/main` failed. Two facts for
the record: Kotlin 2.3.20's K2 reports no unused local, so the warning probe is a call to a `@Deprecated`
function; and both `hiltViewModel`s are `inline reified` (the two 1.3.0 sources jars), the deprecated
one's whole body a call to the new one, so the six call sites compile to the same instructions under either
import — C7 changes what compiles, not what is compiled, and the release runtime classpath is the identical
set (361 coordinates). **Scope:** §9.8 reaches module compilations only; a Gradle *build-script*
diagnostic is the script compiler's and is outside it (auditor finding 5: `app/build.gradle.kts:446`'s
deprecated `assets.srcDir(…)`, on `develop` since `f391ac6`; §14.2). Rule 3 applies to R1's code on the
merged tree (§14.1 step 4).

## 10. Performance budgets

| Budget | Value | Status | Decided in |
|---|---|---|---|
| **Cold start, the gate** | **< 2 s on a named mid-range physical phone** (rules/studio.md product baseline) | **Never measured on a physical device** — every number in the repo is from an emulator. Owner: PLAT-08 / phase 14; the reference device is named in the release evidence. *Decided default if no physical device is available:* the release proceeds on the interleaved emulator gate below plus the Macrobenchmark follow-up, with the absolute bar recorded as **unverified**, never silently passed. Buying a test phone is a founder question the producer folds into the release track | `tech-lead-coldstart-gate.md` §4, Open issues |
| Cold start, tracked target | ≤ 950 ms `am start -W` TotalTime on AVD `Pixel_9a` API 36, debug | **Demoted from gate to tracked target.** It derives from a pre-SDK 633 ms baseline and a different `applicationId`; the same AVD read 957 ms and 1832 ms with nothing changing but host load (1.83×), so an absolute emulator reading is a property of the host, not the app | same §2, §4 |
| Cold-start regression (criterion 15e) | Interleaved A/B gate: `median_B − median_A ≤ max(2 × floor, 100 ms)` with `floor = \|median(N1) − median(N2)\|` from a **same-sitting null control** of one build in both positions; **VOID** (not pass, not fail) when `floor > 150 ms`; ≥ 20 kept samples per arm in blocks of 5, first of each block discarded, `am force-stop` + 2 s between samples; ART normalised with `cmd package compile -m speed -f`; animations off; exactly one emulator; no Gradle daemon anywhere; 1-minute load ≤ half the host's logical cores (5.0 on the studio's 10-core M4) read before and after with the launcher idle; arms share one `applicationId`, so each arm is swapped in **only at a block boundary** and the null control swaps the same way; valid only while both arms share the Room schema and DataStore keys; **`Pixel_9a_bench`** is the instrument of record: the `Pixel_9a`'s profile and image (`android-36.1` `google_apis_playstore_ps16k`, arm64, 16 KB pages, 4 cores, 2 GB RAM, 1080×2424 at 420 dpi) with a 16 GB data partition, no Google account, and used for no other testing; `/data` free ≥ 4 GB (`dumpsys diskstats`, `Data-Free`) at the start and the end of the sitting, both recorded; a sitting on any other AVD is VOID, the `Pixel_9` and the `Pixel_9a` included; arm A = the branch's merge-base with `develop` at the sitting, so A..B is exactly the branch's diff; a later `develop` merge-in leaves a sitting standing unless it changes a start-path file (`App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, `res/values/themes.xml`, `AndroidManifest.xml`, and since H-2 `res/values-night/themes.xml`; `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml` too — *until 2026-10-24 this read "if §16 #18's change lands"; D-05f ruled the change wanted (`docs/ux-spec.md` §6.2, criterion 29) and both files land on `bugfix/home-chip-arrival` (§14.1 F-1, §16 #18), so they are start-path files without condition*); a test-only commit leaves it standing. *Instrument note (2026-10-21, `tech-lead-signoff-close.md` D-1):* the bench was written from `Pixel_9a.avd`'s own `config.ini` and `.ini`, since `avdmanager` is not installed. `avdmanager` and the copy are both valid. Either way the sitting records the diff against `Pixel_9a.avd`, and only `AvdId`, `avd.ini.displayname`, `disk.dataPartition.size` and the `.ini`'s `path`/`path.rel` may differ. An extra key makes the sitting VOID. *ART note (same):* a debuggable APK stays at `[status=verify]` after `compile -m speed -f`. The ART precondition holds when both arms read the same status | Replaces the ill-posed ≤ 50 ms delta (a within-arm range promoted to a between-arm threshold — the tech-lead's own specification error). Runs once on phase 15 before its merge (H-3). Sat 2026-10-18 on `Pixel_9a_bench`: **PASS**, delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B `ee16a2a` (`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`). Each later branch that changes Home's first frame carries its own sitting on the bench: candy-backdrop (§3.5), and F-1 — *conditional until 2026-10-24 ("if its fix changes the first frame"); owed since: D-03n changes the no-chip split and `ui/home/`'s measuring, and D-05f edits two start-path files (§14.1 F-1)*. A VOID is re-run, never reported as a pass or a fail | `tech-lead-coldstart-gate.md` §3; `15-founder-feedback/tech-lead-fonts.md` Ruling 2; `15-founder-feedback/tech-lead-signoff.md` Decision 1; `15-founder-feedback/tech-lead-signoff-close.md` D-1 |
| Instrument succession | `:macrobenchmark` module (`androidx.benchmark`, Apache-2.0) with `StartupTimingMetric`, `StartupMode.COLD`, `CompilationMode.Full()`, ≥ 10 iterations, physical device; plus `reportFullyDrawn()` in `MainActivity` so time-to-full-display becomes measurable (criterion 15f's real subject) | **Scheduled, not phase 15's**; producer schedules, android-game-dev implements, qa-engineer runs. `reportFullyDrawn()` is its own small plan | `tech-lead-coldstart-gate.md` §6 |
| Home's first frame | No blocking read, no image decode, no asset load, no bitmap, no new SDK call — **one named exception: the five bundled type faces** (§11), which load blocking on first use so the first frame is final; the progress ribbon fills asynchronously into a placeholder; the backdrop's gated layer is off frame 1 (§3.5); the Continue chips arrive after first paint by design, because their collector decodes both resume snapshots before it emits (the WR-02/CR-01 phantom-chip guard), and what moves when they arrive is F-1 (§16 #13) | Held structurally by the independent-collector shape (§3.4) | `docs/ux-spec.md` §6.5 as amended; `15-founder-feedback/tech-lead-signoff.md` Decision 3 |
| Hint latency | 1.5 s deadline per `GetHintUseCase` call; book hits in single-digit ms; `depthSweep` measures it exhaustively over every covered frontier | Diamond-41's 440 off-book positions wait out the deadline — the honest cost of the book's documented gap. Past each book's depth (4, or 3 on Wiegleb-45), every Classic hint off the board's committed golden line is a solver call of up to the full deadline; along that line every state is a book entry, so every hint there is instant (§7.2). A miss reads Undecided, never "No winning move": only a proof raises the banner (§7.2, R1 merged as `b1a6cf9`). *(Until 2026-10-24 this cell read: "…every Classic hint is a solver call of up to the full deadline, and a miss reads "No winning move" (§7.2). (2026-10-21: except along each board's committed golden line, whose every state is a book entry, §7.2. Once R1 merges, a miss reads Undecided, not "No winning move"; this row is aligned then, §14.1 R1.)")* | §7.2 |
| `check` wall clock | ≈ 11.5–12 min with `--rerun-tasks` on a quiet host (11 m 32 s and 11 m 48 s at the R-2 sign-off); the fuzz at 10,000 adds ≈ 6 s, kover a few seconds; no second multi-minute gate | Measured 2026-10-23/24 (§7.3) | §7.3, §9.1, §9.7 |
| APK size | Baselined, not gated (D-12): last recorded release APK 2,038,611 B at phase 7, before the ads and PGS SDKs; the fonts add ≈ 229 KiB compressed | — | `06-CONTEXT.md:42`; §11 |
| Cue path | `play()` is one volatile read, zero IPC, zero allocation; no audio work on the main thread beyond the play call | — | §12 |
| Idle Home | `HomeBlobs` draws at ≈ 61 fps idle (O-B) — a pre-release `compose-performance-audit` item (rules/compose.md), not ruled here. The backdrop branch moves every animated drift into the draw phase (§3.5), which lowers each frame's cost, not the frame count, so the audit runs after that branch | open, owner android-game-dev at the audit | `tech-lead-fonts.md`, Open issues; `15-founder-feedback/tech-lead-signoff.md` (O-B) |

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
`THIRD_PARTY.md` carries one row per family (§13).

*Amended 2026-10-18 (`15-founder-feedback/tech-lead-signoff.md` Decision 2):* **AC-F7** now reads
"Both faces render on a cold start (the on-screen discriminators), and the APK contains no
downloadable-font path (a dex and `resources.arsc` scan with a positive control)". Its airplane-mode
cold start was a proxy for one property, that no face depends on the network. The scan tests that
property directly and for every environment: arm B's 30 dex files and its `resources.arsc` hold 0
`com.google.android.gms.fonts` authority strings, 0 `googlefonts` classes and 0 certificate resources,
against 5, 47 and 3 on `develop`, the positive control. **No literal airplane run is owed, now or at
release.** A face discriminator uses a tracking-free role, such as titleLarge's "Level N": the Nunito
band once assumed bodyMedium's 0.25 sp tracking where Material3 1.4.0 sets 0.2 sp (loop-3 O-1). AC-F5's
release half (the AAB keeps the five fonts, `bundletool dump resources`) is the release-manager's, on
`release/3.0.0`. If 15e fails on the build that first bundles the
faces, the tech-lead rules on their share **before** any art is removed: removing art cannot pay for
type.

*Amended 2026-10-21 (`.planning/reports/phase-14/release-manager-font-rows.md`, Open issues; tech-lead
correction):* **AC-F4** as written in `tech-lead-fonts.md` can never return 0 hits on a correct tree.
Its `googlefonts` token matches line 1 of `assets/licenses/Nunito-OFL.txt`, which reads "Copyright
2014 The Nunito Project Authors (https://github.com/googlefonts/nunito)". OFL-1.1 requires that text
to ship unmodified. AC-F4 now reads: this command returns 0 hits.

    git grep -n -E "ui\.text\.googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle

- The narrowed token is the Compose package `androidx.compose.ui.text.googlefonts`, which is what the
  token was there to catch. It also catches the package's `Font` import, which `GoogleFont` alone
  would miss.
- On `develop` (`0582e3f`) the corrected pattern returns no hits.
- Positive control: on `c9228a9^1`, the tree before phase 15, it still hits `Type.kt` (5 lines),
  `font_certs.xml`, `app/build.gradle.kts` and `gradle/libs.versions.toml`.
- AC-F7's scan is unaffected, because the licence text is an asset and sits in neither the dex nor
  `resources.arsc`.

## 12. Audio

Playback is owned by one UI-layer class, `audio/SoundPoolPlayer` (`SoundPlayer` interface; cues in
`SoundCue`, mix constants in `SoundParams`), preloaded through `SoundPool` with `USAGE_GAME` +
`CONTENT_TYPE_SONIFICATION` attributes from `audio/GameAudioAttributes`, the one definition the pool and the
route query share *(until 2026-10-24, when audio merged as `6aa7624`, this read "inline in `SoundPoolPlayer` on
`develop`; on `feature/audio-review`, `GameAudioAttributes` …")*. **No audio focus is ever requested** (SOUND-06: an app that holds no focus cannot
duck the player's music), proved by `audioFocusLiteralCheck`. Assets: twelve cue files under
`res/raw`, each with a `docs/audio/CREDITS.md` row. As built, on `develop` since `6aa7624`, they are twelve mono
16-bit 44.1 kHz WAVs:
- Seven are Kenney *Interface Sounds* and three are freesound-derived, from two freesound sounds
  (`sfx_deselect` is derived from `sfx_select`'s source), all CC0 1.0.
- Two are rendered in-house (`sfx_invalid`, `sfx_hint`), each from a committed `audio/recipes/<cue>.json`.
- This is the set 3.0.0 ships: audio-review merged before `release/3.0.0` is cut (§14.1, merge order step 5), so
  the condition this list carried until 2026-10-24 ("if that branch merges before `release/3.0.0` is cut") is met.
- *Until 2026-10-24 (`6aa7624`) this list opened with:* "**On `develop` today:** twelve `.ogg` files, all CC0
  1.0. Nine come from Kenney *Interface Sounds*. Three come from two freesound sounds, since `sfx_deselect` is
  derived from `sfx_select`'s source." *The branch had replaced them on 2026-09-29 (`af2b554`, `d1c6e9b`).*

`:domain` plays nothing. *(Corrected 2026-10-21, code-auditor F-8: this paragraph described the
audio-review branch's WAV set, and its `GameAudioAttributes`, as current. Current since 2026-10-24.)*

**The silent-mode design** (`docs/tech/audio-silent-mode.md`, on `develop` since `6aa7624` — tech-lead, 2026-09-29,
Q-010 option 2; built on `feature/audio-review` after `develop` contained phase 15, so C1 is done — *until
2026-10-24 this read "on `feature/audio-review` until that branch merges, N-5, 2026-10-24 … builds on
`feature/audio-review` only after `develop` contains phase 15 — C1"*). The decisions below are as built
(`audio-review/tech-lead-signoff.md` §1). Where each now lives, under `app/.../`: `audio/SystemAudioState.kt` (the
seam, `SystemAudioSnapshot`), `audio/AudioRouteClassifier.kt` (A4's allowlist), `audio/AndroidSystemAudioState.kt`
(A3's three triggers, A4's two paths), `audio/AppVisibility.kt` (B1), `audio/GameAudioAttributes.kt`, the three
gates — the in-app `soundOn`, the speaker gate and the background gate — in `audio/SoundPoolPlayer.kt` (A2, A5–A7),
and `MainActivity.kt` for V1 (B5):

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
External. *Device checks, as proven (2026-10-24; `audio-review/tech-lead-signoff.md` §4,
`audio-review/qa-engineer-integration.md`):* the emulator (API 36, the platform-route path) passed D2, D3, D6, D7, D8
and D10 on the speaker branch — no cue started on Vibrate or on Silent, the jump and reject haptics still fired, the
tails stopped. The founder's GM6 (API 27, the inference path, a Bluetooth route) passed D5's first half, D7, D8,
D9's headphone branch and D10's real-OEM case. Two halves stay open, neither merge- nor cut-blocking: the speaker
branch on an API 24–32 phone (JVM-tested in `AudioRouteClassifierTest`), and D4, wired headphones (the same
allowlist as Bluetooth; `BECOMING_NOISY` Robolectric-tested). Both close in the release QA session on the phone if
the founder answers Q-025 with option 1 (Bluetooth headphones off, a wired headset in); otherwise each is one line in
the Q-PUBLISH-3.0.0 summary. *Until 2026-10-24 this read: "Device checks D1–D10 need a physical phone with wired
and Bluetooth headphones."*

**Haptics** (tech-lead decision 2026-10-18, `.planning/reports/audio-review/tech-lead-haptics.md`; built on
`feature/audio-review`, the tutorial seam at `8f18049`, and on `develop` since `6aa7624`; §16 #17, closed). Two
haptics, both gated by the persisted `hapticsOn` and by nothing else: not Sound, not the ringer, not the
background gate. As built:
- `data/HapticsSetting.kt` has one member. `SettingsRepository` implements it, `di/DataModule.kt` binds it from that
  interface, and `TutorialViewModel` reads it as its third collaborator.
- The jump's `ContextClick` fires through one shared effect, `ui/game/JumpHapticEffect.kt`, called from both
  `GameScreen` and `TutorialScreen`.
- The wrong tap's `Reject` is `WrongTapHaptic`, private to `GameScreen`. It runs through `NewOrdinalEffect` and
  fires `HapticFeedbackType.Reject` only when `hapticsOn && SDK_INT >= R`, by our own check
  (`wrongTapHapticFires`), because androidx.core 1.18.0 falls back to `LONG_PRESS` below 30.
- The tutorial passes no `wrongTapCorrection` and hosts no reject haptic, cue or announcement (D-01j), as of
  `6aa7624`; `feature/talkback-board` changes the tutorial's announcement under D-TBa (§14.1 TB).

*Until 2026-10-24 this paragraph cited the ruling "on `feature/audio-review`" and described it as a ruling: "The
jump's `ContextClick` fires on gameplay and in the tutorial, through one shared effect. The wrong tap's `Reject`
fires on gameplay only, and only on API 30+ by our own check … The tutorial reads the gate through
`HapticsSetting`."*

## 13. Dependencies and licences

`THIRD_PARTY.md` (release-manager) is the licence record of what ships, derived from Gradle
configuration, one row per idea, asset, snippet and dependency; the release gate blocks on any row
without a licence. This section is the tech-lead's dependency *policy* and the licence verdict per
family; it does not duplicate the rows.

| Family | Coordinates (direct) | Licence | Copyleft? | Note |
|---|---|---|---|---|
| Kotlin stdlib, kotlinx-coroutines, kotlinx-serialization | `org.jetbrains.kotlin*`, `org.jetbrains.kotlinx:*` | Apache-2.0 | no | `kotlinx-coroutines-play-services` shares the coroutines pin |
| AndroidX | core, activity-compose, lifecycle (atomic group), navigation-compose, datastore-preferences, room (runtime, ktx, compiler), hilt-navigation-compose, hilt-lifecycle-viewmodel-compose (direct since C7, §9.8), Compose BOM members | Apache-2.0 | no | the compileSdk-36 forced versions (§1.2) |
| Dagger / Hilt | `com.google.dagger:hilt-android`, `hilt-compiler` | Apache-2.0 | no | KSP, never KAPT |
| Error Prone annotations | `com.google.errorprone:error_prone_annotations` | Apache-2.0 | no | `compileOnly`; Dagger 2.60 no longer exports it |
| Play Games Services v2 | `com.google.android.gms:play-services-games-v2` 22.0.0 | Android SDK Licence (proprietary, permitted) | no | behind `GamesService`/`CloudSaveStore`; ships no `proguard.txt` of its own |
| Google Mobile Ads Next-Gen | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` 1.4.0 | Android SDK Licence (proprietary, permitted) | no | behind `AdGateway`; read the coordinate character for character — the legacy `play-services-ads` is a different artifact and is never reintroduced |
| User Messaging Platform | `com.google.android.ump:user-messaging-platform` 4.0.0 | **unverified (U-1)** — the AAR carries its dependencies' licences and no licence of its own; one online dependency listing settles it. *Settled 2026-10-21:* the POM declares the Android Software Development Kit License, so U-1 closes (`research-analyst-release-docs-licences.md` #2) | no | release-checklist 3.5; its bundled notices (59 components) are §13.1's |
| Play inline-install library (transitive) | `com.google.android.play:hsdp` 2.0.1, through `ads-mobile-sdk` 1.4.0; no app code calls it | Android Software Development Kit License (POM), and the Play Core SDK terms grant redistribution expressly (`research-analyst-release-docs-licences.md` #1) | no | added 2026-10-21; its bundled notices (70 components) are §13.1's |
| Notice-bearing transitive families | Protocol Buffers (`protobuf-javalite` 3.22.3 and DataStore's repackaged copy), Chromium Cronet (Java only, 143.7445.0), the Public Suffix List data inside OkHttp 4.11.0, checker-qual 3.33.0 | BSD-3-Clause; BSD-3-Clause; MPL-2.0 for that data file only; MIT | MPL-2.0 file-level only, the file unmodified; **not** an escalation | added 2026-10-21 (rows F–J of `research-analyst-release-docs-licences.md`); the notice each needs is §13.1's |
| Fonts | Fredoka v17 (Medium, SemiBold), Nunito v32 (Regular, SemiBold, Bold), static, unmodified | OFL-1.1, no Reserved Font Name | file-level only; **not** an escalation (§11) | notice shipped in `assets/licenses/` |
| Sounds | **For 3.0.0, on `develop` since `6aa7624`** (§12): twelve WAVs, 7 Kenney *Interface Sounds* + 3 freesound-derived + 2 rendered in-house from `audio/recipes/`, per `docs/audio/CREDITS.md`. *Until 2026-10-24 this cell read "(twelve WAVs, once `feature/audio-review` merges, §12) … **On `develop` today:** twelve `.ogg`, 9 Kenney + 3 freesound, per `docs/audio/CREDITS.md`"* | CC0 1.0 for the sourced files; own work for the two in-house renders | no | attribution a courtesy; `THIRD_PARTY.md` §2 carries the 3.0.0 rows. *(Corrected 2026-10-21, code-auditor F-8: this row gave only the `develop` count, as if it were the 3.0.0 set.)* |
| Licence text (candy-backdrop, §3.6) | `assets/licenses/Apache-2.0.txt`, byte-identical to apache.org's (sha256 `cfc7749b…3d30`) | the text's own redistribution terms (the research-analyst confirms them in one line) | no | an asset, not a dependency; its `THIRD_PARTY.md` row is the release-manager's |
| Test-only | JUnit 4 (EPL-1.0), Truth, Turbine, MockK, kotlinx-coroutines-test, Robolectric, ArchUnit, Room testing, Compose ui-test | various permissive | no | never on a variant's runtime classpath |
| Build-only | AGP, Kotlin, KSP, Hilt, Room, detekt, ktlint Gradle plugins; **kover 0.9.11 (R-2, Apache-2.0, JetBrains)** | permissive | no | run on the build machine; kover's `THIRD_PARTY.md` §0 row landed with check-gates (`ec49df3`), its licence read from the four 0.9.11 POMs and the repository's `LICENSE.TXT` |

**Policy.** Non-copyleft additions are autonomous (rules/studio.md) and still get a catalog comment
with the reason and a `THIRD_PARTY.md` row; any GPL/LGPL/AGPL/CC-BY-SA candidate stops and escalates
to the founder through the producer before it is added; "no licence" or "unknown" is never added.
Nothing shipped carries an **unmet** attribution obligation; the studio-rule gap of an in-app licences
surface is scheduled with `feature/candy-backdrop` (§3.6, §16 #4). *(2026-10-21: that sentence is under
review, §16 #20. The research-analyst has since found notice duties that neither `THIRD_PARTY.md` nor the
Licenses spec covers yet: BSD-3, MIT and MPL-2.0 families on the release graph, and the Google SDKs' own
notice files.)* *(Also 2026-10-21: the licences surface is built on `feature/licences-screen`, not on
`feature/candy-backdrop` (§3.6). §13.1 rules how it meets those duties; the sentence holds again once that
branch merges with §13.1's acceptance green.)* No dependency was added by phases
13–14; R-2 adds kover (build-only), phase 15 removes `ui-text-google-fonts`, the Macrobenchmark
follow-up will add `androidx.benchmark` (Apache-2.0). The candy-backdrop branch adds none (the
Apache-2.0 text is an asset; `kotlinx.collections.immutable`, `oss-licenses` and AboutLibraries were
rejected, §3.6). Check-gates C7 declares `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.3.0
directly, since the code then imports it (§9.8). It is already on the release runtime classpath through
`hilt-navigation-compose` 1.3.0, under the AndroidX row above, so nothing shipped changes. Its own §4 row
(Apache-2.0, the POM and the AAR's bundled `LICENSE.txt`) landed 2026-10-24 (`77dcf88`, auditor finding 1).

### 13.1 Ruling — the notices 3.0.0 must carry, and how (§16 #20)

**Tech-lead ruling, 2026-10-21** (`.planning/reports/phase-14/tech-lead-release-docs-blockers.md`). Inputs:
`research-analyst-release-docs-licences.md` (rows A–J and its Apache table) and `research-analyst-font-licences.md`
(open issues 1–3).

**The facts.** Every Google AAR on the release graph carries `third_party_licenses.json` and `.txt` at its
root, and AGP packages neither. Measured read-only on 2026-10-21 in the Gradle cache, at the versions the
research report resolved:

| AAR | Components | Notice file |
|---|---|---|
| `ads-mobile-sdk` 1.4.0 | 98 | 1,451,545 B |
| `hsdp` 2.0.1 | 70 | 814,987 B |
| `user-messaging-platform` 4.0.0 | 59 | 788,804 B |
| `play-services-games-v2` 22.0.0 | 24 | 316,585 B |
| `play-services-appset` 16.0.1 | 19 | 274,609 B |
| `play-services-basement` 18.9.0 | 17 | 331,345 B |
| `play-services-base` 18.5.0 and `play-services-tasks` 18.2.0 | 16 each | 313,001 B each |
| `play-services-ads-identifier` 18.0.0 | 15 | 128,302 B |
| `play-services-cronet` 18.0.1 | 15 | 912,339 B |

Merged, that is 100 component names, 108 distinct (name, text) pairs and 42 distinct texts: 2,053,748 B,
≈ 273 KB deflated. One name can carry several texts (Kotlin 4, J2ObjC 5, Cronet 2). The largest text is
Cronet's in `play-services-cronet`, 785,183 B in 2,497 paragraphs. Google's guide puts displaying these
notices on the app's developer. The release listing (checklist 3.5) stays the authority on which AARs ship.

**LN-1 — The route.** Generate the notices at build time from each variant's own runtime classpath, bundle
them as assets, and show them in-app. Rejected:
- **Google's `oss-licenses` plugin and its activity.** The activity is a separate screen in its own look,
  as LS-03 and §3.6 found. The plugin alone would be a new build dependency on a release-week path, and
  its fit with AGP 9.1's new DSL, which has no legacy variant API (the reason Hilt is 2.60, §1.2), is
  unverified. It also lists every other dependency's POM-declared licence by URL: about 190 entries that
  duplicate LS-03's curated list and still carry none of rows F–J's notice texts.
- **A committed copy.** It puts 2 MB of text in the repository, and it goes stale silently on the next SDK
  bump unless a second gate re-derives it from the classpath anyway.
- **A link to Google's page.** LS-05 keeps both screens inside the app, and the duty is the developer's.

**LN-2 — The build.** A new precompiled convention plugin, `build-logic/src/main/kotlin/google-notices.gradle.kts`,
is applied by `:app` only, as one line in its `plugins {}`. It leaves `standards.gradle.kts` and
`android-app.gradle.kts` alone, so it shares no file with check-gates or the audio branch.
- **Registration.** Through `androidComponents.onVariants`, it registers a cacheable task,
  `generate<Variant>GoogleNotices`, for **every** variant, so `:app`'s unit tests, which run on debug only,
  read what the release ships.
- **Input and output.** The task's input is the variant's runtime-classpath AAR files:
  `variant.runtimeConfiguration` through an artifact view on Gradle's public `artifactType` attribute, value
  `aar`. Its output directory goes in through `variant.sources.assets?.addGeneratedSourceDirectory(...)`. An
  AGP-internal class on this path stops the developer, who reports to the tech-lead.
- **The merge is a Gradle-free pure object**, `GoogleNoticeMerge`. It is unit-tested in `build-logic/src/test`,
  as `AdIdResolution` is (§2.5).
- **No new dependency.** The JSON is read with what build-logic's classpath already has (Gradle's bundled
  `groovy.json.JsonSlurper`), or with a small tested parser for its flat shape,
  `{"<name>": {"start": n, "length": n}}`. Anything new stops the developer and goes to the research-analyst
  for a verdict first.
- **Failures.** An AAR without a root `third_party_licenses.json` contributes nothing. The build fails, naming
  the AAR, when a slice runs past the end of its `.txt`, the JSON does not parse, or a name contains a tab or
  a line break.

**LN-3 — The output.** Generated per variant and never committed:
- `assets/licenses/google/<stem>.txt`: one file per distinct text, byte-identical to its slice. `<stem>` is the
  first 16 hex digits of the text's SHA-256.
- `assets/licenses/google/index.tsv`: UTF-8, one line per distinct (name, text) pair, in the form
  `<key>\t<name>\t<stem>\t<sources>`.
  - `<sources>` lists the `artifactId`s of the AARs that carry the pair, comma-separated and sorted.
  - `<key>` is the first 16 hex digits of the SHA-256 of `<name>`, a U+0000 and `<stem>`.
  - Lines sort by name, case-insensitively, then by stem.
- Every distinct text is kept, so a name with several texts gets one line for each, told apart by `<sources>`.
  The same classpath yields byte-identical output.

**LN-4 — The app.**
- **The index.** `data/GoogleNoticeSource` (`suspend fun index(): List<GoogleNotice>?`) is implemented by
  `AssetGoogleNoticeSource` on §3.6's `@IoDispatcher`. It parses with a pure `parseGoogleNoticeIndex(raw)`. A
  malformed line makes the whole index `null`, which is the error state. `GoogleNotice` is an `@Immutable` data
  class of key, name, sources and stem.
- **The texts** are read through §3.6's `LicenseTextSource`, at the stem the index gives.
- **Routes.** `@Serializable data object GoogleNotices` and `@Serializable data class GoogleNoticeText(val key: String)`.
  A route carries the key, never a stem or a path. The key is resolved through the index read from the APK, and an
  unknown key is the error state. So nothing from a route reaches `AssetManager.open`, as §3.6 requires.
- **The list** loads, so it has a ViewModel, `GoogleNoticesViewModel`. It exposes `Loading`, `Loaded` and `Failed`,
  under §3.6's 300 ms spinner rule, and it shows a `LazyColumn` keyed by `key`.
- **A text** has its own `GoogleNoticeTextViewModel`, exposing §3.6's `LicenseTextUiState`. It renders **one paragraph
  per `LazyColumn` item**, each in its own `SelectionContainer`, re-flowed by §3.6's `reflowLicenseText`. §3.6's
  whole-text `Column` stays for the bundled licence texts. Composing 2,497 paragraphs at once is not acceptable on
  the 2 s-class phone (§10), and a notice needs no whole-text selection.

**LN-5 — What the Licenses screen must show.** This is the technical floor. The words, the grouping and the order are
the ux-designer's, in `licences-screen.md`.
1. **Every shipped `THIRD_PARTY.md` row that carries a licence or a credit** (LS-03, LS-04). Each text below ships
   verbatim at the research-analyst's hash, and its path is written once, in `BundledLicenseText` (§3.6).
   - *Protocol Buffers*, one entry naming both copies (rows F and G): protobuf's v22.3 `LICENSE`, 1,732 B,
     sha256 `6e5e1173…`.
   - *Chromium (Cronet)* (row H): Chromium's `LICENSE` at 143.0.7445.0, 1,536 B, `368cca11…`.
   - *Public Suffix List* (row J): OkHttp's `NOTICE`, 218 B, `8a9c58fb…`. It names the data's source and MPL-2.0,
     which meets MPL §3.2(a). The full MPL-2.0 text is optional, the ux-designer's call.
   - *Checker Framework qualifiers* (row I): its MIT text, 1,126 B, `e90eaa08…`. **Credited, a tech-lead decision:**
     a 1 KB credit replaces the per-release R8 mapping check that checklist 3.5a would otherwise need (research
     report, open issue 2).
   - *Jakarta Dependency Injection*: `Jakarta-Inject-NOTICE.md` verbatim (1,074 B, `992ed62d…a512214df540`), with the
     Apache-2.0 text (font report, open issue 3).
   - The Apache-2.0 libraries in the research report's table (OkHttp, Okio, Gson, Tink, Guava, jsr305, the annotation
     libraries and javax.inject) open the Apache-2.0 text already bundled. One entry or several is the ux-designer's
     call.
2. **Google libraries.**
   - The inert SDK-licence entry stays, with the terms' address (LS-05). It now also names the User Messaging Platform
     (U-1 is closed), the Play inline-install library (`hsdp`) and the Play services libraries.
   - A new tappable entry opens LN-4's notices list, and each line there opens its text. Every pair in the index is
     two taps from the Licenses screen.
3. **LS-01 to LS-07 otherwise stand.** The index is data inside one curated entry, not a dependency dump.

**LN-6 — Who builds it.**
- **ux-designer:** amends `licences-screen.md` for LN-5 (the entries, the notices list and its text screen, strings,
  accessibility, LS-AC rows), before the developer dispatch.
- **android-game-dev:** builds LN-2 to LN-5, test-first, on `feature/licences-screen`.
- **research-analyst:** a verdict on any dependency the build would add (none is expected), and the per-family
  verdicts against the release listing (U-6, checklist 3.5).
- **release-manager:** the `THIRD_PARTY.md` rows (F–J, U-1, `hsdp`, the §4 wording) and the AAB listing.
- **code-auditor and qa-engineer** review, and the **tech-lead** signs off.

**LN-7 — Acceptance.**
1. **`GoogleNoticeMergeTest`** (build-logic), watched red first:
   - two AARs that share a pair give one line with both sources;
   - one name with two texts gives two lines;
   - permuted inputs give byte-identical output;
   - each `<stem>.txt` equals its source slice byte for byte;
   - each failure in LN-2 fails the build, naming the AAR;
   - an AAR without the JSON contributes nothing.
2. **`:app` JVM tests** on the debug variant:
   - `index.tsv` parses, holds at least one line, and has unique keys;
   - every stem it names exists, and the file's SHA-256 starts with that stem;
   - its sources include `ads-mobile-sdk`, `user-messaging-platform`, `hsdp` and `play-services-games-v2`;
   - `parseGoogleNoticeIndex` is tested on a well-formed, an empty and a malformed index;
   - each LN-5 text exists at its hash.
3. **Compose UI tests:**
   - Licenses → the Google notices entry → one line → its text;
   - an unknown key shows the error state;
   - the largest notice opens at its first paragraph and scrolls to its last.
4. **Release** (release-manager, checklist 3.5 and 4.6a — *2026-10-24, N-2b: until then this line read "1.4 and 4.6a"; 1.4 is a pre-cut item and the AAB exists only after the cut, so the listing below belongs to 3.5 and 4.6a, which run on `release/3.0.0`*):
   - `unzip -l <aab> | grep 'base/assets/licenses/'` lists `google/index.tsv`, every stem file it names, and LN-5's
     texts;
   - the index's sources equal the set of AARs on `releaseRuntimeClasspath` that carry `third_party_licenses.json`,
     from checklist 3.5's listing.
5. **Device** (qa-engineer):
   - both lists and three texts, Cronet's included, in light and dark, at 200 % font scale, and with TalkBack;
   - the Cronet notice opens without an ANR and scrolls end to end;
   - both screens in airplane mode (LS-AC07).
6. **The branch:**
   - the Check is green;
   - `git diff develop...HEAD -- gradle/libs.versions.toml build-logic/build.gradle.kts` adds no coordinate;
   - the code is warning-free under C7 once check-gates is on `develop` (§9.8);
   - the release APK's size delta is recorded. It is baselined, not gated (D-12, §10); the estimate is ≈ 0.3 MB
     compressed.

**LN-8 — When.** With the Licenses screen, before `release/3.0.0` is cut (§16 #20, unchanged).
`feature/licences-screen` lands before candy-backdrop's 15e sitting (A11; §14.1, merge order step 6a).

## 14. Technical order

The order gsd plans within and the producer schedules. Per module the order is core → data → UI →
polish; across the remaining v1.1 work it is the dependency order below.

### 14.1 Before the release branch is cut (3.0.0)

**How release-checklist item 1.1 reads this table** (added 2026-10-21). A row whose last cell says **Blocks the
branch cut** is merged, or deferred with a named owner, before `release/3.0.0` is cut. A row marked **Done** or
non-gating blocks nothing. R-5, R-7 and PLAT-08 are gated by their own checklist items (1.4; 1.2 and 3.1; §10's
recorded default), and the upload script by item 1.7 at Gate 2. A row with no such marker has no branch-cut
deadline in this file: whether the cut waits for it is the producer's schedule, recorded in STATUS.md.

| # | Item | Owner | Depends on |
|---|---|---|---|
| R-1 | Gate task (rulings 9.2.5, 9.2.6, 9.3, 9.7) — `feature/phase-14-gate-scope` | android-game-dev → code-auditor PASS → qa-engineer → tech-lead sign-off → merge | **Done**: signed off PASS 2026-10-18 (`tech-lead-signoff-gate-scope.md`) and merged to `develop` as `c3d3437`. Its two non-blocking rulings ride check-gates as C3a (§9.3) and C3b (§9.7) |
| **R-2 + F1/F2 + F-2** | **One branch, `feature/phase-14-check-gates`, serial commits**: C1 F1 self-heal → C2 F2 lock script + Check command → C3 F-2 trimmed known-set + F-1 KDoc (acceptance count **54**, not the brief's 55: addendum A0) → C3a the literal gates over every file of a shipped source set (§9.3; count 55) → C3b CI on `release/**` (§9.7) → C4 `generateLadder` → C5 fuzz harness + tiers + CI env → C6 kover gate → C7 Kotlin warnings as errors in `:app`'s main compilations and all of `:domain` (§9.8). Brief: `.planning/reports/phase-14/tech-lead-r2-gate-brief.md`, with addendum A0–A2 of `tech-lead-signoff-gate-scope.md` and addendum A3 of `tech-lead-design-sync.md` | android-game-dev → code-auditor → tech-lead | **Done.** Merged to `develop` 2026-10-24 as `dbd0a6c`, after 3a, with `develop` taken in at `ffa49aa` and that merge commit re-read (`phase-14/tech-lead-merge-read-check-gates.md`) *(added at r1-close, 2026-10-24)*. **Signed off PASS 2026-10-24** (`tech-lead-signoff-check-gates.md`; auditor PASS `code-auditor-check-gates.md`, on fable): nine code commits `348279d..f15e58a` plus the release-manager's `ec49df3` and `77dcf88`. It was cut from `develop` after R-1 and phase 15 (`c5f2c54`), as ruled (`15-founder-feedback/tech-lead-signoff.md` Decision 4: the kover bound is measured on the `:domain` that ships, with phase 15's `TutorialBoards`, and two of C7's sites are phase-15 files). **Merges after 3a** (`bugfix/hint-undecided`): the two share no file and the three-way trial merge is clean; it takes `develop` in and re-runs the Check on the merged tree before the `--no-ff` merge (C7 rule 3 on R1's code: the import and `@param:` patterns are fixed on this branch by the android-game-dev with the code-auditor reading the hunk; any other kind stops and comes to the tech-lead). The qa-engineer PASS rules/gitflow.md asks for is a review-mode session; C7 needs no device session (the sign-off's ruling 1). Follow-ups it leaves: §8.6 (row DT), §9.6's known limit, §14.2. *Superseded:* "merges before phase 15, which then merges `develop` in" (§17). **Blocks the branch cut** (§15; checklist 1.2 runs its `./scripts/check.sh --rerun-tasks`) |
| R-8 | Test hygiene: fold the two duplicate doubles; bound the two `AppForegroundTrackerTest` awaits | android-game-dev → code-auditor | parallel, non-gating; slips to v1.1.1 if not green in one loop |
| Phase 15 | UX refresh with the font bundling and the follow-up slice; 15e sat on `Pixel_9a_bench` before the merge | android-game-dev → code-auditor + qa-engineer → tech-lead | **Done.** Signed off PASS 2026-10-21 (`15-founder-feedback/tech-lead-signoff-close.md`): H-1 `code-auditor-loop4.md` + `code-auditor-loop5.md`; H-3 `qa-engineer-15e.md`; H-4 recorded in STATUS 2026-10-18. Merged to `develop` 2026-10-21 as `c9228a9` (`git merge --no-ff`; `develop` was still at the merge-base `c3d3437`, so no merge-in was needed). The record: it depended on H-1 to H-4 of `15-founder-feedback/tech-lead-signoff.md` (verdict HOLD, which became PASS without a new tech-lead dispatch because they closed as written); it merged before R-2, after R-1, and had taken `develop` in (`ee16a2a`). *Superseded:* "R-2 branch merged first" (§17) |
| F-1 | Home re-lays out ≈ 0.6 s after first paint when the Continue chips arrive — `bugfix/home-chip-arrival`, off `develop` after phase 15, test first (a Compose test emits no-chip, one-chip and two-chip `HomeUiState`s and asserts the bounds the design ruling pins). **Scope widened 2026-10-21** with rows 1, 3, 4 and 5 of `15-founder-feedback/tech-lead-signoff-close.md`'s INFO table, because this branch reshapes Home and edits `HomeLayoutTest` anyway: (1) test-only: the compact pill's `textsWithin()` must be exactly `[label, value]`, watched red against M3d, since the pixel read cannot see a glyph drawn as text (`code-auditor-loop5.md` INFO 1); (3) test-only: assert the full pill's glyph ink at `w412dp-h915dp`, the inverse of (1)'s read, watched red against M5c, which at ≥ 400 dp only `HomeStageTest` catches, by 1 dp (loop 5 INFO 3); (4) the stale KDoc fixed to the measured truth, 0 dp over at 360×640 on NATIVE and on the device: `HomeScreen.kt:272-277` and `:350-358` (the retired 15h claim, "~72dp over"), the compact preview's KDoc, and `HomeBandSet.kt:45` ("under a full hero") (loop 4 INFO). These lines are in `ui/home/`, a start-path directory, and whether a comment-only edit there leaves a sitting standing is not ruled: this branch's QA decides whether it needs a sitting; (5) `HomeStageTest`'s class KDoc corrected: LEGACY over-wraps, it does not under-wrap (loop 4 INFO). **Scope widened again 2026-10-24** (`.planning/reports/ux-spec-folds/tech-lead-signoff.md`, "§14.1") with the ux-designer's four amendments, checked PASS (`.planning/reports/ux-spec-folds/ux-designer-check-loop5.md`) and on `develop` with `feature/ux-spec-folds` (`4ff4ec6`): **D-03n** (`docs/ux-spec.md` §4.2.5; criteria 15s–15v — the footer takes ⅕ of the no-chip slack, the stage gives the chips their room, hero row and tiles pinned; 15s–15u are JVM bounds tests, no stored goldens, §8.5; part 4, "the first frame is whole", leaves its cause undiagnosed, and §10's first-frame rule — no blocking read, decode, asset load or bitmap — binds whatever the developer finds, a non-drawing fix coming to the tech-lead on this branch); **D-03o** (§4.2.6; 15w — the progress pill never breaks a word, one ribbon shape decided by the labels; 15w's font-scale reads are in-run pixel comparisons, §8.5); **D-05e** (§4.3.2; 18f, 18g, 26b — the timer is one stop reading "Elapsed time, 5 minutes, 12 seconds", never a live region; the four tests §13 names are retargeted, never deleted: `GameFlowTest.kt:129`, `GameScreenChromeTest.kt:62` and `:75`, the `GameStatusStripTest.kt:312-314` helper; closes §16 #15); **D-05f** (§6.2; 29 — the dark three-button navigation scrim in dark mode, by §16 #18's mechanism: `android:windowLightNavigationBar` in `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml`, each restating the whole style, a `WindowThemeTest` case, three-button and gesture in both themes; closes §16 #18's question). Its **15e sitting on `Pixel_9a_bench` is owed, no longer conditional**: D-03n changes the no-chip split and `ui/home/`'s measuring, and D-05f edits two start-path files (§10). Cut from `develop` at `4ff4ec6` on 2026-10-24 (`PegSolitarie-home`), after the spec merged, as the ux-spec-folds sign-off's Handoff 4 asked | ux-designer (§4.2.4/§4.2.2 amendment) → tech-lead (a mechanism ruling only if the design needs more than layout) → android-game-dev → code-auditor + qa-engineer (a screen-recorded cold start with one and two chips, read frame by frame; its own 15e sitting — *"if the fix changes Home's first frame" until 2026-10-24; owed since, see the item cell* — then 15v, 15w, 18g, 26b and 29's device halves in the same session) → tech-lead | phase 15 merged. **Deadline: merged before `release/3.0.0` is cut**, preferably before the candy-backdrop developer dispatch (both reshape Home); if candy goes first, the second to merge takes `develop` in and re-runs candy's three-chip-state checks. A chip shown from the mode list before the decode would bring the phantom chip back, and is rejected (sign-off Decision 3; §3.4's independent-collector shape is kept, and D-03n is the answer to §10's "what moves when they arrive"). **Blocks the branch cut** |
| R1 | **The Undecided hint outcome**, `bugfix/hint-undecided` (added 2026-10-21). The game-designer ruled that "No winning move" on an unproven search is a defect, severity MAJOR, and that R1 must merge before `release/3.0.0` is cut (`.planning/reports/phase-14/game-designer-hint-timeout.md`, GD-HT-1 to GD-HT-6, AC-1 to AC-21). A hint has three outcomes: Found, ProvenDead, which alone raises the banner, and Undecided, a neutral notice that is free, transient and never persisted. The mechanism is the tech-lead's brief on that branch (`.planning/reports/hint-undecided/tech-lead-brief.md`): `SolverOutcome` and `HintOutcome`, `Solver.decide`, `GameUiState.hintUndecided`, commits H1 to H5 red-first, the frozen surfaces of its §6 (the generator, the books, D-07 and every budget constant, the ad paths, the banner and its words) and the acceptance list of its §7 | ux-designer (the notice's text, form, announcement and ux-spec §4.4 row, committed on that branch before H4) → android-game-dev → code-auditor → qa-engineer (AC-18 to AC-21 on a device; AC-18's first scenario is corrected first, §16 #22) → tech-lead sign-off | **Done.** Signed off PASS 2026-10-24 (`hint-undecided/tech-lead-signoff.md`, extended to the merge-in `df3dc2a` and its one-line fix `67c285b`) on the code-auditor's PASS (`hint-undecided/code-auditor.md`) and QA's PASS WITH BLOCKED ITEMS (`hint-undecided/qa-engineer.md`), and merged to `develop` 2026-10-24 as `b1a6cf9`, first of the code branches (step 3a done). The second-to-merge duties of brief §10.3 were discharged by check-gates (merge-in `ffa49aa`, merged `dbd0a6c`) and audio-review (merge-in `13c04c6` with the `GameAnim.kt` keep-both; the `LongMethod` surface list moved into `GameScreen`'s KDoc at `9f0e67a`; merged `6aa7624`). The owed edits of this file landed on `feature/tech-design-r1-close`: §7.2, §8.1, §10, §15 and §16 #21. QA's two blocked items are §16 #29 and #30, and the developer's test fragility is #31; none blocked the merge. *The record:* cut from `develop` at `0582e3f`. Its reviewers need the ruling and §7.2 on the tree, so it merges `develop` in once this branch (release-docs) has merged (brief §0). No 15e sitting unless the brief's §9 tripwire prints. It merges first of the code branches (merge order step 3a). When it merges, this file owes: §7.2's "What 'No winning move' means today" rewritten to the three outcomes, §10's Hint-latency row, §15's row reworded as the residual Undecided rate (R5, 3.1), and §16 #21 closed (brief, Handoff). **Blocks the branch cut** (GD-HT-1, MAJOR) |
| **TB** | **The TalkBack-playable board**, `feature/talkback-board` (added 2026-10-24, from `.planning/reports/ux-spec-folds/tech-lead-signoff.md` "§14.1", which closes the release-docs sign-off's N-1a). The founder answered **Q-023 with option 1** (2026-10-21, `.planning/questions/FOUNDER.md`, on `develop` since `4ff4ec6`): the board becomes playable with TalkBack in 3.0.0. Scope: the game-designer's ruling (`.planning/reports/ux-spec-folds/game-designer-talkback-board.md`, GD-TB-1 to GD-TB-5, M1–M8, TB-1 to TB-12). Contract: `docs/ux-spec.md` D-TB and D-TBa (§3), §11.1, §8's D-TB table (29 strings, two plurals), §13's D-TB rows and criteria TB-1 to TB-12, as checked (`.planning/reports/ux-spec-folds/ux-designer-check-loop5.md`, PASS) and merged with `feature/ux-spec-folds`. Mechanism: the tech-lead's brief `.planning/reports/talkback-board/tech-lead-brief.md` with Amendments 1 and 2 (on `feature/talkback-board` until it merges: `26910de`, `23ca39a`, `3e5b893`) — a semantics layer over the board's existing geometry, a JVM speech model with no Android and no solver (TB-12, ArchUnit-pinned), input focus gated on touch exploration, the slab-before-banner traversal index, commits B0–B7. Not in scope: W1–W7 | android-game-dev → code-auditor → qa-engineer (TalkBack device pass with the speech log: TB-7, TB-9, TB-10, 26b; a working TTS engine confirmed first, brief §10) → tech-lead sign-off (re-reading B0's conflict hunks) | Cut from `develop` at `0582e3f`; B0 merges `develop` in only once `bugfix/hint-undecided`, `feature/audio-review` and `feature/ux-spec-folds` are on `develop` (F-TB-4: the tutorial's rejected-tap signal and the banner's 23k region are theirs; the spec merged 2026-10-24), and this file (row TDS) is there too. *(2026-10-24: all four are — `4ff4ec6`, row TDS `f478ae9`, R1 `b1a6cf9`, audio `6aa7624`; added at r1-close with step 5a's note.)* No 15e sitting unless the brief's §12 tripwire prints. Merge order step 5a. It edits `BoardCanvas.kt`, which `feature/colour-vision` also edits, so it merges before 6b. **Blocks the branch cut, with the founder's Q-023 clause** (checklist 1.1a reads this row): the cut waits for it only if it is the last item standing — and in exactly that case the producer queues one pre-cut `Q-<NNN>`, "wait, or ship option 2", the moment every other **Blocks the branch cut** row is merged or deferred and 1.1a is the only unticked pre-cut item (`Blocks: nothing` — the cut already waits by Q-023's standing answer, the branch keeps building, and a "ship" answer lifts the wait: 1.1a is then deferred with a named owner under 1.1's rule and its no-claim clause binds); it is the one new founder question this row allows, since `Q-PUBLISH-3.0.0` is written after the bundle (checklist 4.8) and cannot carry a pre-cut choice. *Tech-lead sign-off 2026-10-24 (`phase-14/tech-lead-signoff-tech-design-sync.md`, on the code-auditor's open issue); until then this clause read "in which case the producer puts 'wait, or ship option 2' into the Q-PUBLISH-3.0.0 summary rather than raising a new question".* Its merge adds the four D-TBa loop-5 pointers on the hint and audio passages (B7) |
| Audio follow-up | `docs/tech/audio-silent-mode.md` §5 build order (P5 → P4a → gate → P8 → V1; the file is on `develop` since `6aa7624` — *until 2026-10-24 "the file is on `feature/audio-review` until it merges — N-5, 2026-10-24"*) | android-game-dev → code-auditor → qa-engineer (physical phone) → sound-designer review | **Done.** Signed off PASS 2026-10-24 (`audio-review/tech-lead-signoff.md`) on the code-auditor's PASS (`audio-review/code-auditor-integration.md`), QA's PASS WITH BLOCKED ITEMS (`audio-review/qa-engineer-integration.md`) and the sound-designer's PASS (`audio-review/sound-designer-integration.md`), and merged to `develop` 2026-10-24 as `6aa7624`, third of the code branches (step 5 done). **No 15e sitting** (`audio-review/tech-lead-signoff.md` §2.4). The second-to-merge duties discharged: the merge-in `13c04c6`, with three conflicts — `GameAnim.kt` and `TutorialViewModel.kt` resolved by that sign-off's §2.3, and `.planning/questions/FOUNDER.md` by its own format (its "Merge-in re-read" R.2) — the merged Check (1,565 tests, 0 failed or skipped; fuzz 10,000 / 0; kover 99.02 %; 0 `w:` from main code, debug and release; `audio-review/android-game-dev-merge.md`), the code-auditor's hunk read (`audio-review/code-auditor-merge.md`, PASS) and a fresh tech-lead's re-read (PASS). QA's FAIL was a pre-existing defect, now row RM. *The record:* `develop` contains phase 15 (C1), merged in first. Runs in parallel with check-gates and F-1, each in its own worktree; audio and check-gates edit `standards.gradle.kts` in disjoint regions, and the second to finish merges `develop` in and re-runs the Check (C7's rule 3 applies, §9.8). *(Until 2026-10-18 this row also read "The wrong-tap half also waits on the tech-lead's haptics ruling (§16 #17)". That ruling is made, §12 and §16 #17, so the wrong-tap half waits on nothing more.)* |
| **RM** | **The live reduced-motion gate**, `bugfix/reduced-motion-live-gate` (added 2026-10-24; its brief is `.planning/reports/audio-review/tech-lead-signoff.md` §3, with the riders of its §5). QA's FAIL on audio — after a live toggle to "Remove animations", a wrong tap shook instead of marking — is a pre-existing defect: `rememberAnimationsEnabled()` (`ui/theme/Clay.kt`) remembers one `Settings.Global` read for the life of the composition, while Compose's own animation clock follows the setting live, so all 14 call sites in 9 files on `6aa7624` are right at the first composition and wrong after a toggle (§16 #28). *The count (loop 1, code-auditor F-1):* the fourteenth site is R1's `ui/game/HintUndecidedNotice.kt:170`. The brief's §3 counts "13 call sites in 8 files" and "the six" tests because it measured audio's head `b39e28e` before R1 merged, and it still carries those stale counts; this row wins (the file's "How to read it"), and the producer has told the RM developer. **Mechanism (tech-lead decision):** the function keeps its name and signature and reads the composition's own `MotionDurationScale` (`rememberCoroutineScope().coroutineContext[MotionDurationScale]`), enabled when `scaleFactor != 0f` and enabled when the element is absent; our own `Settings.Global` read goes away. Rejected: a `ContentObserver` of our own, and a re-read on `ON_RESUME`. **Tests, red first:** a mutable `MotionDurationScale` flipped 1 → 0 → 1 over the gameplay board (the mark, then the shake), the tutorial and Results. The seven test files that drive the gate through `Settings.Global` on `6aa7624` stay green unedited, and the developer reports the count: the brief's six (`GameScreenSoundTest`, `ResultsCelebrationSoundTest`, `GameFlowTest`, `BoardCanvasWrongTapReducedMotionTest`, `TutorialHapticsTest`, `TutorialSoundTest`) plus R1's `GameScreenHintNoticeTest` (`:354`). **R1's notice (tech-lead decision, loop 1):** it is in scope by construction, since it reads the same one function; no extra red-first test is required, because 23e's test pins its behaviour at each gate value and the (a)/(b) flips prove that the function reads live. QA's repro adds one step: after a live toggle to "Remove animations", an off-book Hint's notice keeps its whole time and leaves in one frame (23e). **Riders, non-gating** (the brief's open issue 2): sound-designer F-1 (`SelectionSound`, `DeselectionSound`, `UndoSound` and `InvalidTapSound` through `NewOrdinalEffect`, one rotation test each), which alone drops to 3.0.1 if it costs a loop (brief §5); code-auditor finding 1 (one `inputs.files` block in `app/build.gradle.kts`); and the F-2 Settings KDoc. *(Until loop 1 this read "(a rider that costs a loop drops to 3.0.1)" for all three, wider than §5; code-auditor F-5 (b).)* | android-game-dev → code-auditor + qa-engineer (the emulator repro: the mark within 450 ms after a live toggle, the reverse toggle, then WT-15 and P9 with animations off, and the notice step above) → tech-lead sign-off | Audio merged (its tests need the wrong-tap harness). Cut from `develop` at `6aa7624` on 2026-10-24 (`PegSolitarie-rm`). **No 15e sitting** with the ruled mechanism: `ui/theme/` is a start-path directory, but Home's first frame loses one `Settings` read and gains no call; a deviation that registers or reads anything on the first frame carries a sitting on `Pixel_9a_bench` and comes to the tech-lead first. **Its slot against 5a:** both branches edit `GameScreen.kt`'s effect block (F-1 moves the four cue effects onto `NewOrdinalEffect`; talkback-board adds its announcement sources beside them), so whichever merges second takes `develop` in, keeps both, re-runs the other's named tests and has the merge commit's hunks re-read (the second-to-merge paragraph below the merge order). **Blocks the branch cut** (rules/compose.md: animation respects reduced motion; an accessibility rule of the product baseline) |
| Candy-backdrop | The backdrop, direction B "Deep gloss" (Q-021), and the licences screens — `feature/candy-backdrop` (§3.5, §3.6) | ux-designer (the B spec rewrite, in progress) → tech-lead (the B fit ruling, §16 #16) → android-game-dev → code-auditor + qa-engineer → tech-lead | phase 15 merged, and `develop` merged into the branch before its first code commit (candy R6). Merges **last**. Its own 15e sitting on `Pixel_9a_bench` is its final step (arm A = its merge-base, §3.5). In parallel with audio only as two developer dispatches in two worktrees, not two gsd phases; they share five files (`GameScreen.kt`, `TutorialScreen.kt`, `dimens.xml`, `strings.xml`, where this branch adds comment-headed blocks only; and `di/DataModule.kt`, where each branch adds its own `@Binds` block, so a conflict there is a mechanical keep-both). The fifth came with the haptics ruling (2026-10-18, §16 #17); the count was four before it. It edits and reads no `THIRD_PARTY.md`. *Corrected 2026-10-21:* the licences screens left this branch on 2026-10-18 for `feature/licences-screen` (`candy-backdrop/tech-lead-direction-b.md` A9; row LN below). This branch carries the backdrop and the Home title fix, and the colour-blind palette is on `feature/colour-vision` (A9). Its sequencing is A11's (merge order steps 6a to 7) |
| LN | **The Licenses screens with every notice 3.0.0 owes**, `feature/licences-screen` (added 2026-10-21; §3.6, §13.1). The screens of R4, plus rows F–J and the Jakarta NOTICE bundled verbatim, and the Google SDK notices generated at build time and shown behind the Google entry (LN-1 to LN-8) | ux-designer (`licences-screen.md` amended for LN-5 before the dispatch) → android-game-dev → code-auditor + qa-engineer → tech-lead; release-manager for the `THIRD_PARTY.md` rows and the AAB listing; research-analyst for any new dependency (none expected) and U-6 | Phase 15 merged (done). The branch is cut from `develop` and brings its spec over (A9). It lands before candy-backdrop's 15e sitting, because it edits `navigation/`, a start-path directory (A11; merge order step 6a). *2026-10-24:* cut from `develop` at `c5f2c54` (`PegSolitarie-lic`; `0bf578d` carries the spec and rulings over from candy, A9); the LN-5 spec amendment is in progress. **Blocks the branch cut** (§16 #20's deadline: BSD-3 and MIT make the notice a condition of redistribution) |
| PU | **`scripts/play-upload.sh`**, on `feature/play-upload` off `develop` (added 2026-10-21; release-manager F-6, `.planning/reports/phase-14/release-manager-release-docs-fixes.md`). It runs `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON`, uploading the signed AAB of `:app:bundleRelease` to the `internal` track (`14-RELEASE-PLAN.md` item 2). **It checks no approval itself:** the gate is `hooks/publish-gate.sh`, which blocks any command that runs `play-upload.sh` or `fastlane supply` without `.planning/approvals/publish-<VERSION>.approved` (plugin rules/stacks.md 1.0.0, scaffold contract 9). This supersedes the release plan's "hard refusal … belt as well as the plugin hook's braces" and checklist 1.7's "refuses to run without" (§17). It fails with a message when the variable or the AAB is missing, and it echoes no secret. Acceptance: `shellcheck scripts/play-upload.sh` clean, and the code-auditor's read against this row. No session executes it before Gate 2, and nothing routes around the hook | android-game-dev → code-auditor → tech-lead sign-off | Nothing; scripts only. It is code, so it never goes on the release branch. **Blocks Gate 2, not the branch cut**: item 1.7 is also met by the producer recording the founder's Console upload (operator checklist Step 8 item 1) |
| TDS | **This file's sync**, `feature/tech-design-sync` (added 2026-10-24; docs only — `docs/tech-design.md`, and in the same worktree the release-manager's `docs/release-checklist.md` and `THIRD_PARTY.md` edits, with the three reports; *"`docs/tech-design.md` and its report" until the 2026-10-24 sign-off, auditor F-4*). It carries every tech-design edit the two sign-offs left to the tech-lead (the header's 2026-10-24 sentence), in one commit, distinct sections: N-1a (row TB, step 5a), N-2b (LN-7 item 4), N-4 (§16 #22), N-5 (the `audio-silent-mode.md` citations), N-8 (§8.1's count), the F-1 row, §16 #15 and #18, §16 #23 and #24, §10, §17. Ruled by `.planning/reports/ux-spec-folds/tech-lead-signoff.md` ("Which branch it rides"): not `feature/talkback-board`, whose B0 waits on hint and audio, so a row that blocks the cut would otherwise sit only on the branch it blocks on; not `feature/phase-14-release-docs`, which had merged (`c5f2c54`). Review footing as for release-docs: the code-auditor reads it as text, a fresh tech-lead signs off, QA not applicable | tech-lead (edits) → code-auditor (text) → tech-lead sign-off (fresh) | Cut from `develop` at `4ff4ec6` on 2026-10-24 (`PegSolitarie-tds`), after `feature/ux-spec-folds` merged. Merges any time once its reports exist (merge order step 3b), and **must be on `develop` before the `feature/talkback-board` developer dispatch and before checklist 1.1 runs** (1.1 reads this table at the cut). The release-manager's edits ride the same worktree in parallel, disjoint files: `docs/release-checklist.md` 1.1a, 1.4, 3.5 and 4.6a, and `THIRD_PARTY.md` `:93`, `:381` and row J — N-1b, N-2a, N-3, N-6 and N-7 (`1cf8f32`), then the code-auditor's F-1, F-2, F-3, F-5, F-6 and F-7 and row J's exact PSL version (`fd2c220`). *Until the 2026-10-24 sign-off (auditor F-4): "A `docs/release-checklist.md` edit by the release-manager (N-1b, N-2a, N-6, N-7)".* |
| R-4 | D-14 account-swap UAT with a second Play Games account | qa-engineer | a second account (else a founder question) |
| R-5 | `THIRD_PARTY.md` — U-1 and §5b closed by the release-branch dependency listing; the founder's store-artwork rows; before release-docs merges, the font rows carry `tech-lead-fonts.md`'s text and the research-analyst's OFL-1.1 verdict line; the Apache-2.0 text's row with the candy build | release-manager; research-analyst; founder (Q-F3) | — |
| R-6 | This file | tech-lead | done 2026-10-17; synced 2026-10-18 (`tech-lead-design-sync.md`) and 2026-10-21 (the phase-15 close and the haptics ruling, `tech-lead-design-close.md`); the R-2 figures transcribed 2026-10-24 (`tech-lead-signoff-check-gates.md`) |
| R-7 | `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` on the release branch (exhaustive, §7.3; `--rerun-tasks` so the fuzz lines are the release's own execution, auditor finding 2), `domain/build/fuzz-failures.txt` absent, coverage figure recorded (99.0 % at the gate's introduction) | release-manager | R-2 (signed off) |
| DT | **The solver deadline tests' own task**, `bugfix/solver-deadline-task` (§8.6, added 2026-10-24): `SolverDeadlineTest` run by `:domain:solverDeadline` outside kover, always-run, in `check`; `koverVerify` re-measured after the move | android-game-dev → code-auditor → tech-lead | R1 and R-2 merged (it edits the files R1 rewrites). No branch-cut marker: before the cut if green in one loop, else §14.2; the quiet-host rule governs the release Check meanwhile |
| PLAT-08 | Cold start on a named physical phone, or the unverified record (§10) | qa-engineer; producer for the device question | — |

**Merge order into `develop`** (`15-founder-feedback/tech-lead-signoff.md` Decision 4, 2026-10-18).
It supersedes the R-2 brief's §0 and this table's earlier "R-2 before phase 15" (§17). *Amended 2026-10-21*
with three sources: R1 (step 3a, from `.planning/reports/hint-undecided/tech-lead-brief.md` §10 on
`bugfix/hint-undecided`); the licences and colour-vision branches (steps 6a and 6b, from
`candy-backdrop/tech-lead-direction-b.md` A9 and A11 on `feature/candy-backdrop`); and the upload script
(step 6c). The existing steps keep their numbers, because §12 cites step 5. The inserted steps are lettered.
*Amended 2026-10-24* with two more: the TalkBack board (step 5a, slot from `.planning/reports/talkback-board/tech-lead-brief.md`
§13.2 on `feature/talkback-board`, written into this file by `.planning/reports/ux-spec-folds/tech-lead-signoff.md`
"§14.1", which also ruled the carrier), and this file's own docs-only carrier (step 3b, row TDS).
1. `feature/phase-14-release-plan`: any time, once its reports exist. It carries `.planning/` and
   `docs/` only, depends on nothing, and nothing depends on it. Merged 2026-10-21 (`0582e3f`).
2. `feature/phase-15-ux-refresh` (after H-1 to H-4) and `feature/phase-14-gate-scope` (R-1), in either
   order: first signed off, first merged. R-1 went first (`c3d3437`); phase 15 followed on 2026-10-21
   (`c9228a9`). The two share only
   `app/build.gradle.kts`, in disjoint hunks. The branch that merges second takes `develop` in and
   re-runs the Check. A clean merge with a green Check needs no new review; a conflict in a code file
   goes to the code-auditor.
3. `feature/phase-14-release-docs`, after phase 15, with the R-5 font rows and this file. Its order
   relative to R-1 is free. *(2026-10-21:)* it carries the hint ruling and §7.2, which `bugfix/hint-undecided`'s
   reviewers need on that branch's tree (brief §0), so it merges ahead of 3a wherever the reviews allow.
3a. **`bugfix/hint-undecided` (R1), the first of the code branches** (brief §10.2). It blocks the cut
   (GD-HT-1, MAJOR), it is the smallest branch, and it is the furthest from the build files. Steps 4 and 5
   then absorb it through the `develop` merge-in each already owes, so C7's warning gate and C6's coverage
   bound are measured on a tree that contains it. *(2026-10-24:)* `feature/ux-spec-folds` (docs only; D-TB,
   D-TBa, D-03n, D-03o, D-05e, D-05f) merged first of this pair, as `4ff4ec6`, with no merge-in; 3a takes
   `develop` in before its qa-engineer dispatch and resolves the eleven `docs/ux-spec.md` hunks by the
   ux-spec-folds sign-off's rule (the hint side first, this branch's side after it, each a contiguous block;
   the `revised:` date takes 2026-10-24), the ux-designer reads the resolved hunks once, and 3a's tech-lead
   sign-off re-reads them. *(Done 2026-10-24: merge-in `df3dc2a` with its one-line fix `67c285b`, merged as
   `b1a6cf9`; row R1.)*
3b. **`feature/tech-design-sync`** (row TDS), docs only: any time, once its reports exist, like step 1 — and
   before the `feature/talkback-board` developer dispatch and before checklist 1.1 runs. It carries this file
   only, so it conflicts with no code branch; a later branch that edits this file (R1's §7.2 rewrite, LN,
   5a's close of §16 #24) takes `develop` in and keeps both, distinct sections. *(2026-10-24, check-gates'
   merge-in re-read: "keeps both" holds for distinct sections only; where both sides number new rows in one
   list or write one figure measured at two trees, the merge-in rule in §17 governs.)*
4. `feature/phase-14-check-gates`, cut from `develop` only after R-1 and phase 15 have merged. *(2026-10-24:)*
   signed off PASS; it merges after 3a, with `develop` merged in and the Check green on the merged tree
   (R-2 row). *(Done 2026-10-24: merge-in `ffa49aa`, merged as `dbd0a6c`.)*
5. `feature/audio-review`, after phase 15, merging `develop` in first. *(Done 2026-10-24: merge-in `13c04c6`,
   merged as `6aa7624`; row "Audio follow-up". Row RM, `bugfix/reduced-motion-live-gate`, was cut from it the
   same day; its slot against 5a is set in its row.)*
5a. **`feature/talkback-board`** (row TB), after 3a and 5: its B0 happens only once `bugfix/hint-undecided`,
   `feature/audio-review` and `feature/ux-spec-folds` are on `develop` (the last since `4ff4ec6`), and it
   merges before 6b and 7 (colour-vision edits the same file, `BoardCanvas.kt`; the game-designer's "merged
   before candy-backdrop"). Its place against 4, 6, 6a and 6c is free. The second of any pair does the brief's
   §13.3. Should audio-review slip behind the point where this branch would be the last item standing, the
   producer applies Q-023's clause as row TB's pre-cut `Q-<NNN>` *(2026-10-24, the sign-off: "at Gate 2" until
   then — Gate 2 comes after the bundle and cannot carry a pre-cut choice)* rather than having this branch copy
   audio's fields (the two cannot both own `rejectedTapCell`). *(2026-10-24: the precondition is met —
   `bugfix/hint-undecided` (`b1a6cf9`), `feature/audio-review` (`6aa7624`) and `feature/ux-spec-folds` (`4ff4ec6`)
   are on `develop` — so the "should audio-review slip" clause is moot. Against row RM, see that row.)*
6. `bugfix/home-chip-arrival` (F-1), after phase 15 and before `release/3.0.0`. *(2026-10-21:)* it shares
   no code with R1, so its place relative to 3a is free (brief §10.2). *(2026-10-24:)* cut from `develop` at
   `4ff4ec6` (`PegSolitarie-home`), after the spec merged; its scope is row F-1's as widened that day, and
   its 15e sitting is owed.
6a. **`feature/licences-screen`** (row LN), after phase 15 and before candy-backdrop's 15e sitting: it edits
   `navigation/`, a start-path directory, and merging it after the sitting forces a re-sitting (A11). Its
   place relative to 3a–6 is free. It merges `develop` in first, so if check-gates is already there, its
   build-logic and `:app` code meet C7. *(2026-10-24:)* cut from `develop` at `c5f2c54` (`PegSolitarie-lic`).
6b. **`feature/colour-vision`**, either before candy-backdrop's 15e sitting or after candy-backdrop merges,
   never between the two. It needs its own sitting, and the tech-lead's brief for it is still owed (A9,
   A11). It has no branch-cut marker in this file (see the note above the table). *(2026-10-24:)* cut from
   `develop` at `c5f2c54` (`PegSolitarie-cv`; `8f67d74` carries its spec and rulings over from candy); the
   tech-lead's brief is in progress. It edits `BoardCanvas.kt`, which 5a also edits, so it merges after 5a and
   does the TalkBack brief's §13.3 for that pair (keep `BoardCanvas`'s `if (!interactive)` branch first and the
   Box second; its palette hunks slot below; `BoardSemanticsTest` and `BoardCanvasTest` green).
6c. **`feature/play-upload`** (row PU), scripts only, so its place is free. It is due before Gate 2, not
   before the cut.
7. `feature/candy-backdrop`, last. Since A9 it carries the backdrop and the Home title fix. It is built
   after steps 4 to 6 are on `develop` (A11).

Steps 3a to 6c run side by side in their own worktrees, and merges stay serial. If a later branch is ready
first, it merges first (brief §10.2). For every pair, the branch that merges second merges `develop` in
(never a rebase), resolves the conflicts, re-runs its own Check and the other branch's named tests, and has
its tech-lead sign-off re-read the merge commit's conflict hunks. The brief's §10.3 gives the resolutions
for R1 against check-gates, the audio branch and the ux-spec folds. *(2026-10-24:)* the TalkBack brief's §13.3
gives 5a's — for its B0 after 3a and 5, and for 4, 6a, 6b and 7 against it — and the ux-spec-folds sign-off's
eleven-hunk table gives 3a's against the merged spec. Then the release-manager cuts
`release/3.0.0`. *(Until 2026-10-21 this paragraph read: "Steps 4 to 6 run side by side in their own
worktrees. Then the release-manager cuts `release/3.0.0`.")*

**Record files in a merge-in** (tech-lead ruling, 2026-10-24: `audio-review/tech-lead-signoff.md`, "Merge-in
re-read" R.2, after audio's merge-in met a third conflict, `.planning/questions/FOUNDER.md`, that its §2.2 had not
predicted). §17's merge-in rule governs this file only; this rule governs the `.planning/` records.
- (a) A `.planning/` record kept as entries (FOUNDER.md, STATUS.md, a report both sides appended to) may conflict
  because each side added whole, distinct entries. Such a conflict is resolved by the record's own format: keep
  both, in the format's order, and edit no entry.
- (b) A conflict inside one entry, or two sides claiming the same id, is not mechanics. It stops before the
  merge-in is committed and goes to the producer, who owns those records.
- (c) A sign-off that pre-rules a merge-in measures it with `git merge-tree` against `develop`'s tip as it will be
  at the merge-in, docs branches included, and names every branch still due to merge ahead of it.

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
surface (§16) — *scheduled 2026-10-17 with `feature/candy-backdrop` (§3.6), so no longer backlog*;
**the held half of Kotlin and lint warnings as errors (§9.8)**: the `createComposeRule` v2 migration,
the `kotlin-reflect` call in `GameScreenSoundTest`, `allWarningsAsErrors` on `:app`'s unit-test
compilations and `warningsAsErrors` in lint — **due before the first `release/*` branch after
`release/3.0.0` is cut**; rules/compose.md F-33's host-level `safeDrawingPadding()` (§3.5), only with a
layout reason, since moving the insets re-opens every screen's geometry; `docs/gdd.md` (game-designer); a `compose-performance-audit` before release
(rules/compose.md; O-B idle Home); the upstream Gradle issue for `hasResults()` validating existence
but not validity (low priority); the `Pixel_9a` emulator fault preserved as a control (an environment
fault — emulator DNS — not an app change). **Added 2026-10-24 (the R-2 sign-off,
`tech-lead-signoff-check-gates.md`):** `scripts/check.sh` records the Gradle child's PID and reclaims only
when both PIDs are dead, closing the `kill -9` window (§9.6, auditor finding 3), with its INT comment
corrected in the same edit (finding 6); `ci.yml:58`'s step comment names `./scripts/check.sh` as the Check
command with the file's next edit (finding 7), and in the same edit `ci.yml:48-49`'s build-logic comment stops
saying those tests are outside the root `check` (§9.7; added at check-gates' merge-in re-read, 2026-10-24); `app/build.gradle.kts:446`'s deprecated
`assets.srcDir(roomSchemaDir)` is replaced by the form the AGP ladder step's release notes name, with that
step (finding 5: a build-script diagnostic, outside §9.8, a break only on the AGP that removes the method);
and the solver deadline tests' own task (§8.6, row DT) if it slips past the cut.

## 15. Risks

| Risk | Likelihood / impact | Mitigation | Owner |
|---|---|---|---|
| **The Core Value is asserted on six sampled levels** until R-2 lands; a generator knob tune could ship an unsolvable level | low / severe. *Closed 2026-10-24* | R-2's exhaustive ladder walk (§7.3) is the gate: green on all 10,000 levels with the determinism pass, in `check`, CI and the release alike; it caught a planted certificate defect that `:domain:test` missed (the auditor's `level % 97` mutation) | android-game-dev, tech-lead |
| The fuzz harness at 10,000 does not fit in 60 s and the default drops to 1,000 | medium / low. *Closed 2026-10-24* | The decision rule in §7.3 is pre-written; CI and release stay exhaustive either way. Measured: 5.955 s at 10,000, so the default is 10,000 | developer measures, reports |
| kover cannot apply beside AGP 9.1.0 / Kotlin 2.3.20, or instruments the sweep | low / medium. *Closed 2026-10-24* | kover is applied to `:domain` only (pure JVM); exclusion of `depthSweep`/`fuzz` is an acceptance check; fallback pre-ruled in the brief: Gradle's built-in JaCoCo on `:domain` at the same bound, recorded here as a deviation. kover 0.9.11 applied; `koverVerify`'s graph holds `:domain:test` only | developer stops and reports if neither applies |
| `check`'s 11 minutes make the concurrent-build race near-certain on a shared host | high / medium | F1 (self-heal) + F2 (lock), both on check-gates since 2026-10-23; residual: the `kill -9` reclaim window (§9.6, a known limit, post-3.0.0) and every hand-run `./gradlew <task>` outside the lock; the Diamond-41 book closes the cost later | android-game-dev; producer's dispatch discipline |
| `:domain:test`'s four wall-clock solver deadline tests go red on a loaded host, 2.5× sooner under kover's agent, so a Check reads red for a reason unrelated to the diff | medium on a shared host, low on a quiet one / low (a re-run; fail-closed) | §8.6: their own task outside kover (row DT, §14.1); the quiet-host rule for every Check until then, and for the release Check regardless | android-game-dev; producer's scheduling; release-manager for 3.1 |
| A signed AAB with a placeholder id on disk after a failed build | closed by 9.2.5 | package-task `dependsOn`, proven by execution | — |
| The 2 s cold-start bar has never been measured on a physical device | certain / medium | Recorded as unverified rather than passed; Macrobenchmark follow-up; founder question on a test phone | producer, qa-engineer |
| A future `SCHEMA_VERSION` bump silently deletes the behind device's progress (C-1) | n/a today / severe | §4.4 is a hard gate; both behaviours pass today, which is why it is written down | whoever bumps it |
| `InMemoryPreferencesDataStore` edited without its contract test | low / high | F-11 rule (§8.2) | code-auditor on any diff touching it |
| The configuration cache is switched on without re-auditing the signing capture | low / medium | §9.2.7 names the fix shape | tech-lead on any `gradle.properties` change |
| `ResultsViewModel` at detekt's function ceiling grows a `@Suppress` in a hurry | medium / low | F-4 extraction scheduled (§14.2) | android-game-dev |
| Phase 15 and the R-2 branch both edit `gradle/libs.versions.toml` | medium / low | Disjoint regions; phase 15 merges first; R-2 is cut after it (§14.1). *Superseded mitigation:* "the R-2 branch merges first; phase 15 merges `develop` in" | producer |
| Fable is exhausted and a Fable-pinned review cannot run | observed / process | rules/studio.md: the step is not done; the founder's standing answers (Q-003, Q-011, Q-014) are scoped, not general. **Q-019 (2026-10-17): Opus for every Fable-pinned role until 3.0.0 is published**, the deviation recorded in every report. Reviewers then share the developer's model, so the mitigation is fresh reviewers that run the Check and their own mutation probes rather than trusting recorded results | producer |
| 15e on the new bench VOIDs (unknown floor) or FAILs on the fonts' blocking load (estimate 10–40 ms against a threshold of at least 100 ms) | medium / low | A VOID is a re-run, never a verdict; a FAIL brings the fonts' share to the tech-lead before any art is removed (§11). *2026-10-21:* phase 15's sitting passed (delta 33.0 ms, floor 3.0 ms, §10), so the fonts' share was never reopened. The row stands for the bench's next sittings: candy-backdrop's, and F-1's — *"if it carries one" until 2026-10-24; owed since (§14.1 F-1)* | qa-engineer, tech-lead |
| **The residual Undecided rate off the book** (since R1 merged as `b1a6cf9`, 2026-10-24): past each Classic book's depth off the board's committed golden line, in Diamond-41's and Wiegleb-45's gaps and off a Challenge level's recorded line, a hint search that runs out reads Undecided (§7.2). The answer is truthful and free, but it gives the player no move. *Until 2026-10-24 this cell read: "Hint shows "No winning move" on a solver budget miss, on a position that may be winnable: past each Classic book's depth, in Diamond-41's and Wiegleb-45's gaps, and off a Challenge level's recorded line (§7.2). (2026-10-21: past each book's depth only off the board's committed golden line, whose states are all book entries, §7.2.)"* | high off the book and device-dependent: R1's QA saw 6 of 6 off-book hints Undecided on the GM6 (API 27) and 17 of 17 on the emulator (API 36), with Found and ProvenDead unaffected / low (a hint that helps less; no false claim). *Until 2026-10-24: "high (measured on Wiegleb-45: none of its depth-≤4 positions resolved inside the deadline before the book) / medium (player trust; a misleading-claims exposure where a text calls it a proof). Until R1 merges"* | R1 is built and merged: only a proof raises "No winning move" (GD-HT-1), the notice says the position may still be winnable, and an Undecided hint is never charged (GD-HT-5). The rate is unmeasured until R5's census (3.1, GD-HT-6), which, with deeper books and line continuity (R2, R2b), is the lever; D-07 and every budget constant stay frozen for 3.0.0 (GD-HT-4). The texts follow the ruling's §4 "After R1" column from this merge on. *The record, until 2026-10-24:* "Texts describe only what the code proves (code-auditor F-1, the release-manager's store and changelog fixes); the banner and behaviour question is open (§16 #21). **Ruled 2026-10-21:** a defect, MAJOR (GD-HT-1). R1 on `bugfix/hint-undecided` (§14.1) makes a budget miss read Undecided, never "No winning move", and it blocks the branch cut. Until R1 merges, every text follows the "Today" column of the ruling's §4. Once it merges, the residual is the Undecided rate off the book, which is unmeasured until R5 (3.1, GD-HT-6), and this row is reworded to that" | game-designer for R5 and the 3.1 item; release-manager for the texts ("After R1" column). *Until 2026-10-24 also: android-game-dev for R1, through code-auditor and qa-engineer to the tech-lead's sign-off; ux-designer for the notice* |
| **The reduced-motion gate is frozen at the first composition** (QA's FAIL on audio, pre-existing; §16 #28): after a live toggle to "Remove animations", every phase-15 reduced-motion behaviour, R1's Undecided notice and the wrong tap's mark (14 call sites in 9 files on `6aa7624`) follow the old setting while Compose's animation clock follows the new one | certain on a live toggle / medium (accessibility: rules/compose.md and the product baseline) | `bugfix/reduced-motion-live-gate` (§14.1 RM): the gate reads the composition's own `MotionDurationScale`, so the gate and the clock are one reader of one value; red-first tests flip a mutable scale live; QA repeats its emulator repro. **Deadline: merged before `release/3.0.0` is cut** | android-game-dev → code-auditor + qa-engineer → tech-lead |
| The Licenses screens, a branch-cut blocker (§16 #20), slip behind the backdrop | medium / high (no cut without them) | They have their own branch since A9, so neither candy-backdrop's 15e sitting nor its capture review holds them (§14.1 LN). The generated notices need no new dependency (§13.1 LN-2) | producer schedules; android-game-dev builds |
| The notices task cannot reach the runtime AARs through AGP 9.1's public variant API | low / medium | LN-2 names the public route (`variant.runtimeConfiguration` and the `artifactType` attribute). An AGP-internal import stops the developer, who reports; the tech-lead rules the fallback, and no oss-licenses plugin is added in a hurry | android-game-dev, tech-lead |
| F-1, the Home layout jump, sits on `develop` until its fix lands | certain / low | `develop` ships nothing; the deadline holds it out of 3.0.0 (§14.1) | producer |
| A branch merged after check-gates C7 carries a new main-compilation warning | medium / low | Its Check fails once it has taken `develop` in; it is fixed on that branch, never by a suppression or a compiler switch (§9.8 rule 3) | that branch's developer |
| The B direction's gel mode cards reshape Home's tiles against phase 15's geometry (15g, 15k, 15p, 15r) | medium / medium | B's own surfaces get a fit ruling against D-03l clause 4 and the phase-15 suites before the build, and the phase-15 suites run unchanged (§3.5, §16 #16) | tech-lead, ux-designer |

## 16. Open items — each with an owner

Every item the seeded file listed as "decided by nobody" is now either decided or has a named owner.

| # | Item | Status | Owner |
|---|---|---|---|
| 1 | `minSdk 24` vs the rule's 26 | **Decided** (§1.3): 24 for 3.0.0, with the reason recorded; the raise is a founder question (installed-base share) the producer queues, non-blocking, with the tech-lead's recommendation. **Answered 2026-10-17 (Q-018, option 3):** 24 indefinitely, unless a technical need appears; nothing is queued, and §1.3 defines what re-opens it | tech-lead, only on a recorded technical need (§1.3) |
| 2 | R-2 / R-7 — kover and fuzz | **Designed** (§7.3, §8.4) and briefed; implementation is the next gate branch. **Implemented on `feature/phase-14-check-gates` and signed off PASS 2026-10-24** (`tech-lead-signoff-check-gates.md`): 10,000 levels in 5.955 s, tier 10,000; kover 0.9.11, 99.0 % at bound 90. R-7 is the release-manager's run with `--rerun-tasks` | R-2 closed; R-7 release-manager |
| 3 | R-1′ — the gate task | **Implemented** on `feature/phase-14-gate-scope`, auditor PASS; qa-engineer and tech-lead sign-off next; auditor F-2 ruled into the R-2 branch (§9.3). **Signed off PASS 2026-10-18** (`tech-lead-signoff-gate-scope.md`) and **merged** to `develop` (`c3d3437`). Its two non-blocking rulings ride check-gates as C3a and C3b; brief §4's C3 count is corrected by addendum A0 | closed |
| 4 | In-app licences surface | Studio-rule gap, not a legal one: the OFL notice condition is met by the licence files in the APK, everything else is CC0, Apache-2.0 or a permitted SDK licence. **Backlog v1.1.1**, needs a ux-designer (a Settings row) and a developer. **Scheduled 2026-10-17** with `feature/candy-backdrop` (§3.6), no longer v1.1.1 backlog: `docs/ux/licences-screen.md` is specified. *Corrected 2026-10-21:* on its own branch, `feature/licences-screen`, since 2026-10-18 (A9; §3.6, §14.1 LN). It is now also where the notices of §16 #20 ship (§13.1) | ux-designer, android-game-dev via producer; the Apache-2.0 text's `THIRD_PARTY.md` row is the release-manager's, its redistribution-terms line the research-analyst's |
| 5 | `docs/gdd.md` | Not a technical item | game-designer via producer |
| 6 | `THIRD_PARTY.md` §5b per-coordinate enumeration, U-1 | Closed on the release branch by `:app:dependencies --configuration releaseRuntimeClasspath` (checklist 3.5) | release-manager |
| 7 | Post-v1.1 backlog | **Recorded** in §14.2 | producer carries it |
| 8 | `AGENTS.md` | rules/stacks.md's scaffold contract wants it identical to `CLAUDE.md`; this product predates the contract and has none. Decision: add it as a copy when the Check command line changes (F2), so both are written once. **Done** in check-gates C2 (`139b418`): a byte-identical copy, `cmp` clean | closed |
| 9 | Release-checklist alignment with R-2: 3.1's command becomes `FUZZ_SEEDS=100000 ./scripts/check.sh`; 3.2's path is `domain/build/fuzz-failures.txt`; 3.3 records the kover figure | Text edits on the release-manager's file. **Done** on check-gates (`ec49df3`, `77dcf88`): 1.2 and 3.1 with `--rerun-tasks`, 3.2's path, 3.3's 99.0 % figure | closed |
| 10 | The named mid-range physical phone for the 2 s bar | Founder question (money), folded into the release track; decided default recorded in §10 | producer |
| 11 | The phase-15 spec and design-system text the font and cold-start rulings require (`docs/ux-spec.md` §6.5, §6.3, §9, criteria 15e/18d; `docs/ui/DESIGN-SYSTEM.md` Typography) | Routed; until applied the rulings are the governing text. Added 2026-10-18: the fold of `15-founder-feedback/tech-lead-signoff.md` Decision 1 (§6.5 precondition 1 and a new precondition 9, the "Which builds" pointer, criterion 15e's AVD and arm A), and candy R1–R6's departures D1–D11 into the two candy specs, as far as the B rewrite keeps them. Added 2026-10-21: folds b–e of `15-founder-feedback/tech-lead-signoff-close.md` (§6.5 precondition 1: the bench may be made with `avdmanager` or by copying the 9a's files, held by the recorded diff; criterion 15k part 2's 0 dp reading on real metrics and what "overflows" means; the 15e O-1 ruling, §16 #18; and the items still owed from the sign-off). That report found the 2026-10-18 fold still unapplied (its fold a). Both folds land before the next 15e sitting, whose qa-engineer reads §6.5, and before `release/3.0.0` is cut | ux-designer via producer |
| 12 | The plugin side of F2 (the stack's Check command carrying the lock) and OFL-1.1 in rules/ip-policy.md's matrix | Plugin changes, not product ones | producer → retro |
| 13 | F-1: Home re-lays out ≈ 0.6 s after first paint when the Continue chips arrive | **Routed, not blocking** (`15-founder-feedback/tech-lead-signoff.md` Decision 3): it fails no written criterion, and its fix is a design decision first, namely what may move when a chip arrives. §6.5 forbids a blocking read before Home's first frame, so the chips always arrive late (§10). **Deadline: merged before `release/3.0.0` is cut** (§14.1) | ux-designer → android-game-dev → code-auditor + qa-engineer → tech-lead |
| 14 | Kotlin and lint warnings as errors (plugin rules/kotlin-android.md 1.0.0) | **Decided 2026-10-18** (§9.8): check-gates C7 makes `:app`'s main compilations and all of `:domain` warning-free and gated; `:app`'s unit-test compilations and lint are a held deviation that ends before the first release branch after 3.0.0 | android-game-dev via producer (C7, then §14.2) |
| 15 | How the timer is announced: TalkBack likely speaks "Elapsed time" without the value (O-A, A-8, I-2), plus criterion 26's spoken-order half | Routed (`15-founder-feedback/tech-lead-signoff.md`, routed after the merge). **Before the `release/3.0.0` QA closes.** **Closed 2026-10-24 by D-05e** (`docs/ux-spec.md` §4.3.2; criteria 18f, 18g, 26b; checked PASS in `.planning/reports/ux-spec-folds/ux-designer-check-loop5.md`; on `develop` since `4ff4ec6`): the timer is one stop reading "Elapsed time, 5 minutes, 12 seconds", never a live region, and criterion 26's spoken-order half is 26b. The ruling changes code, so it is built on `bugfix/home-chip-arrival` (§14.1 F-1, the four test retargets named there) and TalkBack's speech output is captured by 18g's device half in that branch's QA session. Nothing is owed by this item any more; what remains is F-1's chain | ux-designer rules the spoken form (done, D-05e); qa-engineer captures TalkBack's speech output (18g, on `bugfix/home-chip-arrival`); android-game-dev, since the ruling changes code (the same branch) |
| 16 | The fit of direction B's own surfaces (the gel mode cards, anything on Home's stage) with D-03l clause 4 and the phase-15 suites | Open: the B spec rewrite is in progress; candy R2's wing rulings are void (§3.5) | tech-lead, when the B spec lands; before the candy-backdrop developer dispatch |
| 17 | The tutorial reading `hapticsOn` without breaking `TutorialViewModel`'s two-collaborator pin (the wrong-tap feedback's open question) | Open, owed by the tech-lead (candy R6 condition 5; `15-founder-feedback/tech-lead-signoff.md` Decision 4 row 5). It gates only the wrong-tap half of the audio branch. **Decided 2026-10-18** (`.planning/reports/audio-review/tech-lead-haptics.md`, `feature/audio-review`). A read-only `HapticsSetting` seam (`data/`, extended by `SettingsRepository`, bound to it) is `TutorialViewModel`'s third collaborator. The pin names the three types, and a second test pins the seam to its one member. The value rides on `board.hapticsOn` and is carried through `forStep`. The jump haptic is gameplay's, lifted to `ui/game/JumpHapticEffect.kt`; the tutorial never fires the reject haptic (D-01j). Owner: the android-game-dev, in the audio brief (§12, §14.1). **Closed 2026-10-24:** built on `feature/audio-review` (`8f18049`, 2026-10-21), merged as `6aa7624`; signed off in `audio-review/tech-lead-signoff.md` §1 (Ruling 1 to Ruling 4 hold); §12's Haptics paragraph states it as built | closed *(until 2026-10-24: "android-game-dev via producer, in the audio brief (the ruling was the tech-lead's, made)")* |
| 18 | **15e O-1:** in dark mode the three-button navigation bar shows the platform's light grey scrim (`#cdccd0`, buttons `#666666` at 3.59:1). Pre-existing: arm A is pixel-identical (`15-founder-feedback/qa-engineer-15e.md`, Findings) | **Not a defect** (`15-founder-feedback/tech-lead-signoff-close.md`, INFO row 9): it fails no criterion, and 3.59:1 clears the 3:1 bar for UI components. Whether a dark three-button bar is wanted is the ux-designer's decision (§16 #11). **If it is wanted, the mechanism is the window theme, as for the F-2 status-bar fix** (`windowLightStatusBar` in `res/values/` and `res/values-night/themes.xml`, pinned by `WindowThemeTest`): `android:windowLightNavigationBar` true in light and false in night. That attribute is API 27 and minSdk is 24 (§1.3), so it goes in `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml`, each restating the whole style, since styles do not merge across qualifiers. A `WindowThemeTest` case pins it. Both files are start-path files (§10), so the change rides a branch that runs its own 15e sitting on the bench: `bugfix/home-chip-arrival` if that branch runs one, else `feature/candy-backdrop`. Its device check covers three-button and gesture modes in both themes. **Deadline:** the ruling before `release/3.0.0`; the change only if it is ruled wanted, otherwise post-3.0.0. **Ruled wanted 2026-10-24 — D-05f** (`docs/ux-spec.md` §6.2; criterion 29; checked PASS in `ux-designer-check-loop5.md`; on `develop` since `4ff4ec6`): exactly the mechanism above — `android:windowLightNavigationBar` in `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml`, each restating the whole style, a `WindowThemeTest` case, three-button and gesture in both themes — on `bugfix/home-chip-arrival`, whose 15e sitting is owed (§14.1 F-1), so the "if that branch runs one, else `feature/candy-backdrop`" fork is settled and the two v27 files are start-path files without condition (§10). Closes when that branch merges | ux-designer rules (done, D-05f); then android-game-dev → code-auditor + qa-engineer → tech-lead, on `bugfix/home-chip-arrival` |
| 19 | A Fable re-audit of phase 15's diff: both phase-15 audits say one "is owed when the policy allows" (`15-founder-feedback/code-auditor-loop4.md` and `code-auditor-loop5.md`, Open issues) | **Not a merge condition** (`15-founder-feedback/tech-lead-signoff-close.md`, INFO row 10). Q-019 is the founder's answer: Opus for the review chain until 3.0.0 is published, with fresh reviewers, the Check and mutation probes run by the reviewer (§15, the Fable row). Both audits did exactly that. Whether phase 15's diff gets a Fable re-audit after 3.0.0 is a policy and money question. **When:** after 3.0.0 is published | producer → the 3.0.0 retro; a non-blocking founder question if it costs money |
| 20 | The notice duties beyond the fonts: BSD-3 (`protobuf-javalite`, Cronet), MIT (`checker-qual`) and MPL-2.0 data (OkHttp's public-suffix list) on the release graph, the Jakarta Inject `NOTICE.md`, and the `third_party_licenses.txt` that every Google SDK AAR carries and AGP never packages (`.planning/reports/phase-14/research-analyst-font-licences.md`, open issues 1–3) | **Open, recorded 2026-10-21.** It puts §13's "nothing shipped carries an unmet attribution obligation" under review. The research-analyst expects no escalation under the matrix. Owed: a tech-lead ruling on the Google SDK notices (bundle the texts behind the Google entry, or revisit LS-03 and adopt the oss-licenses data), a research-analyst verdict per family against the release-branch listing, and the ux-designer's `licences-screen.md` amendments. **Deadline (tech-lead decision, 2026-10-21):** with the Licenses screen, before `release/3.0.0` is cut, because BSD-3 and MIT make the notice a condition of redistribution, not a courtesy. **Ruled 2026-10-21 (§13.1, LN-1 to LN-8).** The research-analyst's verdicts are in (`research-analyst-release-docs-licences.md`: rows A–J, 192 of 192 POMs, nothing copyleft or unknown, U-1 closed). The Google SDK notices are generated at build time from each variant's runtime classpath, with no new dependency, and shown behind the Google entry. Rows F–J and the Jakarta NOTICE ship verbatim, and checker-qual is credited. **Still open until** `feature/licences-screen` merges with LN-7 green, and the release-manager's rows and AAB listing (LN-7 item 4) close it at the cut. The deadline is unchanged | ux-designer (`licences-screen.md` for LN-5) → android-game-dev (LN-2 to LN-5) → code-auditor + qa-engineer → tech-lead; research-analyst for any new dependency and U-6; release-manager for `THIRD_PARTY.md` (F–J, U-1, `hsdp`, §4, §5b, §7) and the AAB listing |
| 21 | Hint's "No winning move" banner on a solver budget miss (`code-auditor-release-docs.md` F-1, Open issues): the banner is raised on any `null` from `GetHintUseCase`, and the solver's `null` folds a budget miss and a cancellation in with a true dead end (§7.2) | **Open, recorded 2026-10-21. Not ruled here.** Whether the wording or the behaviour should change is a product question. A game-designer is ruling it, with the ux-designer for the banner. If the ruling needs code, the tech-lead then rules the mechanism and records it in §7.2. Until then, §7.2's transcription is what the code does. **Ruled 2026-10-21** (`.planning/reports/phase-14/game-designer-hint-timeout.md`): **a defect, severity MAJOR.** The game-designer picked R1: three outcomes, and only a proven dead end raises "No winning move". An Undecided search gets its own neutral, free, transient notice, and a cancelled search is silent (GD-HT-1 to GD-HT-5). R1 must merge before `release/3.0.0` is cut. Deeper books, line continuity and the census are one 3.1 item (GD-HT-6). The tech-lead ruled the mechanism in `.planning/reports/hint-undecided/tech-lead-brief.md` on `bugfix/hint-undecided` (§14.1 R1). **Closes when R1 merges** and §7.2, §10 and §15 are brought to it. **Closed 2026-10-24** on R1's merge (`b1a6cf9`): built per GD-HT-1 to GD-HT-6 and the brief with its Amendments 1 and 2; reported in `hint-undecided/android-game-dev.md`, reviewed in `hint-undecided/code-auditor.md` (PASS) and `hint-undecided/qa-engineer.md` (PASS WITH BLOCKED ITEMS), and signed off in `hint-undecided/tech-lead-signoff.md`. §7.2, §10 and §15 are brought to it on `feature/tech-design-r1-close`. #22 was closed already. QA's two blocked items and the developer's test fragility are #29–#31 | closed; release-manager for the copy, the ruling's §4 "After R1" column from this merge on *(until 2026-10-24: "android-game-dev (R1) → code-auditor + qa-engineer → tech-lead (sign-off, then the §7.2 rewrite); ux-designer for the notice; release-manager for the copy ("Today" column until R1 merges)")* |
| 22 | **AC-18's first scenario contradicts the book** (found 2026-10-21, §7.2 "The book counts, reconciled"). AC-18 says that after a player follows Hint for four moves on Wiegleb-45, the fifth hint spins for about 1.5 s and shows Undecided. But every state of the committed golden line is a book entry, and following Hint from the start keeps the player on that line, so the fifth hint is an instant Found. The brief's §7 Device list ("including the two reached without following Hint") inherits the same premise. AC-2 is unaffected, because it asserts the book miss | **Closed 2026-10-24** (N-4): the game-designer's **Amendment 1** to the hint ruling (`.planning/reports/phase-14/game-designer-hint-timeout.md`, the amendment headed 2026-10-21, committed as `1afae9f` on 2026-10-23, on `develop` — *"commit `1afae9f`, 2026-10-21" until the 2026-10-24 sign-off, auditor F-8*) is the correction this row asked for — AC-18 rewritten as walks A to C, three Wiegleb-45 positions off the committed golden line, each reached without following Hint, the book miss asserted as a precondition (AC-18a–d), plus AC-2c; the reach table and F-1's "follow the hints" example are struck through there with the same reasoning. The status read Open only because this file's last edit (`aaaba67`) preceded the amendment. R1's qa-engineer runs the amended AC-18. *Until 2026-10-24 this cell read:* **Open.** AC-18 is the game-designer's. Suggested correction, which the game-designer may word otherwise: three Wiegleb-45 positions four jumps in, each off the golden line, each reached without following Hint, with the book miss asserted in the domain test that supplies it, plus a control in which the hint-follower's fifth hint is a book Found. The same reasoning bounds the ruling's reach table and F-1's "follow the hints" example: on every Classic board, a hint-follower from the untouched start never reaches the solver. R2b's 3.1 target still holds for a player who leaves the line. **Deadline:** before the qa-engineer runs AC-18 on `bugfix/hint-undecided` | game-designer via producer; qa-engineer; android-game-dev for the sequences (brief §7, Device) |
| 23 | **The gameplay cells are below the studio's 48dp touch-target rule** (recorded 2026-10-24; `docs/ux-spec.md` §11.1 "Cell size", D-TBa item 10, on `develop` since `4ff4ec6`). rules/compose.md asks ≥ 48dp targets with ≥ 8dp spacing. On gameplay a cell's finger target and its TalkBack focus rectangle are both the board's geometry (`HIT_RADIUS_RATIO` 0.5 × the cell, `BoardMetrics.kt`, `BoardGeometry.kt`): on a 360dp-wide phone the canvas is 296dp, so a cell is **≈ 42.3dp** on a 7-wide grid (English-33, European-37; 48dp from a 400dp window), **37.0dp** on 8-wide (Asymmetric 3-3-2-2; from 448dp) and **≈ 32.9dp** on 9-wide (Diamond-41, Wiegleb-45; from 496dp), neighbouring targets touching with no spacing; at the 480dp slab cap they are 64, 56 and ≈ 49.8dp. **WCAG 2.2 SC 2.5.8 (AA, 24 × 24 CSS px) is met** by every cell, with no exception claimed. The tutorial meets the studio rule (≈ 83dp cells). It is the board's geometry as shipped since phase 4, which D-TB does not change; the spec records it and 26a reads a cell as one stop with its description and its action label. No code answer exists short of W1's row navigation, a 3.1 item | **A recorded studio-rule deviation, not a defect of any branch.** It goes to the founder in the **Q-PUBLISH-3.0.0** summary with §11.1's table (the ux-spec-folds sign-off, Handoff 5; the game-designer's and ux-designer's rulings) — a taste call, never the studio's. Nothing in 3.0.0 waits on it | producer (carries it, with the sizes above, into Q-PUBLISH-3.0.0); ux-designer and game-designer own any later geometry change (W1, 3.1) |
| 24 | **The TalkBack-playable board** (added 2026-10-24; §14.1 row TB, merge order step 5a). Q-023 option 1 (2026-10-21): playable with TalkBack in 3.0.0, the release waiting for it only if it is the last item left. Contract D-TB and D-TBa (`docs/ux-spec.md` §3, §11.1, §8, §13, TB-1 to TB-12), checked PASS and on `develop`; mechanism the tech-lead's brief with Amendments 1 and 2 (on `feature/talkback-board`) | **Open, owned.** The developer dispatch waits for `bugfix/hint-undecided`, `feature/audio-review`, `feature/ux-spec-folds` (done, `4ff4ec6`) and this file (row TDS) to be on `develop`. *(2026-10-24: all four are — R1 `b1a6cf9`, audio `6aa7624`, row TDS `f478ae9` — so the dispatch waits on nothing in this file.)* Two things the brief pre-rules inside the review loop, never as a new branch: F-TB-1's fallback if the device shows TalkBack not following input focus, and A2.4's caption-only first step if "Step 1 of 3" is heard twice. Its merge adds the four D-TBa loop-5 pointers (B7) and brings the audio fold-in's §3.8 placement rule (brief A2.3) to the tech-lead at that fold-in. **Closes when `feature/talkback-board` merges** with its code-auditor PASS, the qa-engineer's TalkBack device pass (a working TTS engine first; a crashed TTS is VOID, an undrivable double-tap BLOCKED) and the tech-lead's sign-off re-reading B0's hunks | android-game-dev → code-auditor → qa-engineer → tech-lead sign-off (as row TB); producer for the Q-023 clause if it is the last item standing — row TB's pre-cut `Q-<NNN>`, *not Gate 2 (2026-10-24, the sign-off)* |
| 25 | The four solver deadline tests under kover's agent on a loaded host (`code-auditor-check-gates.md` finding 4) | **Ruled 2026-10-24** (§8.6): their own `Test` task outside kover, on `bugfix/solver-deadline-task` after R1 and R-2 merge; not a branch-cut blocker; the quiet-host rule meanwhile | android-game-dev → code-auditor → tech-lead; the producer's schedule |
| 26 | `scripts/check.sh` after `kill -9`: the lock is reclaimable while the orphaned Gradle client still builds (finding 3) | **Known limit for 3.0.0, fix shape ruled** (§9.6): record the child's PID too and reclaim only when both are dead; post-3.0.0, with the INT comment (finding 6) | android-game-dev via producer, post-3.0.0 |
| 27 | `app/build.gradle.kts:446`'s deprecated `assets.srcDir(…)` (finding 5) | Outside §9.8 (a build-script diagnostic); a break only on the AGP that removes it; fixed with the AGP ladder step (§14.2) | tech-lead at the ladder step; android-game-dev |
| 28 | **The reduced-motion gate is read once per composition** (added 2026-10-24; QA's FAIL on `feature/audio-review`, `audio-review/qa-engineer-integration.md`, ruled in `audio-review/tech-lead-signoff.md` §3). `rememberAnimationsEnabled()` (`ui/theme/Clay.kt:209-218`) is `remember(context)` over one `Settings.Global.getFloat(ANIMATOR_DURATION_SCALE)` read, so its answer is fixed for the life of the composition. Compose's own clock is not: `MotionDurationScaleImpl` (pinned `ui-android` 1.11.3) observes the same key live. After a live toggle the clock runs at scale 0 while the gate still says "on": the wrong tap takes the shake, its tween ends in its first frame, and the mark never draws. The same split affects all 14 call sites in 9 files on `6aa7624` (`HomeBlobs.kt`, `DockOnMark.kt`, `GameActionDock.kt`, `ResultsCelebration.kt`, `HintToggleControl.kt`, `TutorialScreen.kt`, `BoardCanvas.kt`, `Clay.kt`, and R1's `HintUndecidedNotice.kt:170`), and seven test files drive the gate through `Settings.Global` (R1's `GameScreenHintNoticeTest.kt:354` the seventh). The ruling's §3 counts 13 in 8 and six tests, measured on audio's head `b39e28e` before R1 merged; it carries those stale counts, and this row wins (loop 1, code-auditor F-1). The helper is phase 15's; audio only made the defect visible | **Open, owned. Blocks the branch cut** (§14.1 RM). Mechanism ruled (tech-lead decision): the gate reads the composition's own `MotionDurationScale` element (`rememberCoroutineScope().coroutineContext[MotionDurationScale]`), enabled when `scaleFactor != 0f`, and enabled when the element is absent (a preview, a test that provides none). Reading `scaleFactor` in composition subscribes the composition to snapshot state, so a live toggle recomposes every reader on every API level, and the gate and the clock cannot disagree again. The developer first verifies the two Compose source facts in the cached sources jar. No 15e sitting with this mechanism. **Closes when** `bugfix/reduced-motion-live-gate` merges with its code-auditor PASS, QA's emulator repro and the tech-lead's sign-off | android-game-dev → code-auditor + qa-engineer → tech-lead |
| 29 | **A Diamond-41 jump did not commit on the founder's GM6** (added 2026-10-24; R1's QA, `hint-undecided/qa-engineer.md`, a blocked item; ruled in `hint-undecided/tech-lead-signoff.md` §1.1). Classic › Diamond-41, tap (2,4) then (4,4): in three attempts on the GM6 (Android 8.1, API 27) the jump did not commit. On the same phone the same two-tap jumps committed on Wiegleb-45 and English-33, and on the emulator Diamond-41 played through to AC-20. The phone fell asleep under adb input while taps still reached the app, so the failure may be an artefact of injected input. Not R1's: its diff leaves board input untouched | **Open, owned. Severity MINOR, provisional** (tooling suspected). It becomes **MAJOR** — a Classic board unplayable by touch on an API 27 device, with minSdk 24 (§1.3) — if it reproduces on an Android 8.1 (API 27) emulator image with the screen kept on, or by finger on the GM6 if the founder lends it; it then blocks `release/3.0.0` through the release checklist. It never blocked R1's merge | android-game-dev via the producer, as its own task: it reproduces QA's exact sequence on the API 27 emulator image, and by finger on the GM6 if the founder lends it (the ruling's §1.1 and Handoff 5); tech-lead for the final severity. *(Until loop 1 this cell also read "qa-engineer to reproduce", which the ruling does not say; code-auditor F-5 (a).)* |
| 30 | **TalkBack by hand for the Undecided notice and the banner** (added 2026-10-24; R1's QA, a blocked item; ruled in `hint-undecided/tech-lead-signoff.md` §1.1). Criterion 23j item 5's spoken-order half and 23k's spoken traversal were accepted for R1's merge on tree-order evidence: the accessibility tree's order in every device dump, and the spoken text captured through TalkBack's "Display speech output". TalkBack's gesture-driven traversal and its actions menu (Dismiss) were never observed, because injected input does not drive TalkBack on the API 36 image | **Open, owned. Owed before `release/3.0.0`; if no hardware set-up makes TalkBack drivable by the cut, disclosed instead as one line in the Q-PUBLISH-3.0.0 summary** (tech-lead decision; the second ending as ruled below). Carried to row TB's TalkBack device pass on `feature/talkback-board`, which changes the traversal (the slab's index before the banner) and is the only place the notice's final spoken order exists. That pass drives real TalkBack: a scrcpy or hardware-keyboard session, or an image TalkBack accepts input from. **If adb still cannot (tech-lead decision, r1-close loop 1, code-auditor I-3):** the founder never tests by hand (`audio-review/tech-lead-signoff.md` §4), so the producer's non-blocking founder question asks only for hardware set-up that lets QA drive TalkBack itself — the GM6 or another phone over USB with a hardware keyboard, the way Q-024 and Q-025 ask for hardware — never for a pass the founder runs. If no set-up makes TalkBack drivable by the cut, the two halves become one line in the Q-PUBLISH-3.0.0 summary ("the notice's and the banner's TalkBack traversal: tree-order and speech-output evidence only"), as audio's open halves do. Never back to R1. *(Until loop 1 this read "QA's fallback, a founder-run TalkBack pass before release, goes to the producer as a non-blocking founder question", as `hint-undecided/tech-lead-signoff.md` §1.1 ruled; the signing tech-lead confirms or rules back this reconciliation of the two rulings.)* *(Confirmed 2026-10-24 at the r1-close sign-off, `.planning/reports/phase-14/tech-lead-signoff-tech-design-r1-close.md`, which also put the second ending into this cell's opening words; until then they read "**Open, owned. Owed before `release/3.0.0`**" alone, code-auditor L1-1. The disclosure is the last resort, never a waiver: QA's TalkBack pass on row TB stays the item's close.)* | qa-engineer of row TB; producer for the hardware question if it is needed, and for the Q-PUBLISH line |
| 31 | **`GameViewModelResumeTest` can hang instead of failing** (added 2026-10-24; found by R1's developer, `hint-undecided/android-game-dev.md`, and ruled in `hint-undecided/tech-lead-signoff.md` §2 (b)). In that class's pattern, a failed precondition under a live ticker hangs `runTest`'s drain instead of failing the test, so a regression there reads as a stuck Check, not a red one. Only R1's new test is wrapped in try/finally; the class's other tests share the pattern, which predates R1 | **Open, owned. Test-only, no branch-cut marker.** Scheduled after the 3.0.0 cut's code branches | android-game-dev via the producer → code-auditor → tech-lead |

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
| "`Pixel_9a` is the instrument of record and a `Pixel_9` sitting is VOID" (`tech-lead-fonts.md` Ruling 2) | `Pixel_9a_bench`: the same profile and image with a 16 GB data partition, `/data` free ≥ 4 GB; the 9a's partition was 95 % full and refused both arms | §10; `15-founder-feedback/tech-lead-signoff.md` Decision 1 |
| 15e's arm A = "`develop`", and "runs once, on the phase-15 follow-up slice" | Arm A = the branch's merge-base with `develop` at the sitting; runs on phase 15 before its merge, and each later branch that changes Home's first frame carries its own sitting | §10, §3.5; same |
| AC-F7's airplane-mode cold start | The dex and `resources.arsc` scan with a positive control; no literal airplane run, now or at release | §11; `tech-lead-signoff.md` Decision 2 |
| "R-2 merges before phase 15, which then merges `develop` in" (`tech-lead-depthsweep.md` ruling 2; `tech-lead-r2-gate-brief.md` §0); phase 15 depending on "R-2 branch merged first" | Phase 15 merges first; R-2 is cut from `develop` after R-1 and phase 15 | §14.1, §15; `tech-lead-signoff.md` Decision 4 |
| `shippedSourceRoots` = (main, release, debug) × {java, kotlin, res} | Every file under the three shipped source sets | §9.3; `tech-lead-signoff-gate-scope.md` ruling 2 (check-gates C3a) |
| CI on push and PR to `develop` and `main` | `develop`, `release/**`, `main` (ruled; not built until check-gates C3b merges, so `develop`'s `ci.yml` still triggers on `develop` and `main` only) | §9.7; `tech-lead-signoff-gate-scope.md` ruling 1 (check-gates C3b) |
| The R-2 brief §4's C3 acceptance count, 55 | 54 at C3, since C3 deletes the test that pinned the pre-authorised source sets; 55 after C3a | §8.1, §14.1; `tech-lead-signoff-gate-scope.md` addendum A0 |
| The R-2 brief §10's "any UI, ViewModel or `:app` production code" and "any phase-15 file" out of scope | Lifted for C7's ten main-source sites only | §9.8, §14.1; `tech-lead-design-sync.md` addendum A3 |
| §3.1's "every screen has one `@HiltViewModel`" without exception | Except the Licenses list, which holds nothing | §3.1, §3.6; candy R4 |
| §16 #4's in-app licences surface as "Backlog v1.1.1" | Scheduled with `feature/candy-backdrop` | §3.6, §13, §14.2, §16 #4 |
| Candy R2, the stage wings (B-12), and R1's sweets-specific variants (Landscape, Quiet, Sparse and their layers) | Void for the wings, by the founder's Q-021 choice of direction B "Deep gloss"; the variants come from the B spec rewrite; R1's host, state, cross-fade, F-32 surface and insets, and R3's gate, stand | §3.5 |
| P-1.1 "`avdmanager create avd -n Pixel_9a_bench …`" as the only route to the bench (`tech-lead-signoff.md` Decision 1) | `avdmanager`, or a copy of `Pixel_9a.avd`'s `config.ini` and `.ini`; either is closed by the P-1.2 diff, which every sitting records | §10; `tech-lead-signoff-close.md` D-1 |
| AC-F4's `googlefonts` token (`15-founder-feedback/tech-lead-fonts.md` AC-F4), which matches the OFL text's upstream URL | `ui\.text\.googlefonts`; the rest of the pattern is unchanged | §11; `release-manager-font-rows.md`, Open issues |
| §3.3's "`TutorialBoards` lives in `:domain` beside `Boards.kt`" (`phase-15/tech-lead-architecture.md` §1) | Its own `tutorial/` package, as built and signed off; the binding half, absent from `CLASSIC_SPECS`, stands | §2.1, §3.3; code-auditor F-8 |
| Phase 4 D-04 (a solver budget miss folds into "no winning move") and `08-VERIFICATION.md` override 1's "a documented completeness gap, not an untruthfulness defect" | GD-HT-1: three outcomes, and only ProvenDead raises the banner. D-04's own precondition is falsified, and the override's reasoning is overturned (a dated correction note is in `08-VERIFICATION.md`) | §7.2, §14.1 R1, §16 #21; `game-designer-hint-timeout.md`. *Note confirmed 2026-10-24 at R1's close: `.planning/phases/08-assist-generator-quality-debt/08-VERIFICATION.md:43`, "Correction — 2026-10-21", committed in `aaaba67`* |
| The licences screens built on `feature/candy-backdrop` (§3.6, §13, §14.1's candy row, §16 #4, as of 2026-10-17) | Their own branch, `feature/licences-screen`, from 2026-10-18; corrected here on 2026-10-21 | §3.6, §13, §14.1 LN, §16 #4; `candy-backdrop/tech-lead-direction-b.md` A9 |
| §14.1's seven-step merge order (2026-10-18) | The same steps plus 3a (R1, first among the code branches), 6a (licences), 6b (colour-vision) and 6c (upload script), and the second-to-merge duties of the hint brief's §10.3 | §14.1; `hint-undecided/tech-lead-brief.md` §10; A9, A11 |
| §7.2's "a depth-K book holds the positions up to K moves from the start", and the gap headers read as depth-≤K counts ("4 of 140", "440 of 818") | A book also holds every state of its committed golden line past K; the depth-≤K frontiers are 101 (Wiegleb-45) and 785 (Diamond-41) | §7.2, "The book counts, reconciled" |
| §3.6's whole-text `Column` for every licence text | Kept for the bundled licence texts; a Google SDK notice renders one paragraph per `LazyColumn` item | §3.6, §13.1 LN-4 |
| §3.6's inert "Google libraries" entry as the whole Google credit (LS-03, LS-05) | The inert SDK-licence entry plus a tappable entry that opens the generated notices list | §13.1 LN-5 |
| The upload script's own approval check: the release plan's "hard refusal … belt as well as the plugin hook's braces" (`14-RELEASE-PLAN.md` item 2) and checklist 1.7's "refuses to run without" the approval file | No check of its own; `hooks/publish-gate.sh` is the gate (plugin rules/stacks.md 1.0.0, scaffold contract 9). Both texts are the release-manager's to align | §14.1 PU |
| N-1a's vehicle, "the talkback brief's commit on `feature/talkback-board`, after that branch takes `develop` in" (`tech-lead-signoff-release-docs.md`, 2026-10-23), and the TalkBack brief's Handoff, "record this branch as step 5a … on `feature/phase-14-release-docs`" | The docs-only `feature/tech-design-sync` (row TDS, step 3b), cut from `develop` after `feature/ux-spec-folds` merged: release-docs had merged (`c5f2c54`), and a cut-blocking row must not sit only on the branch it blocks on. The deadline — before the TalkBack developer dispatch and before checklist 1.1 — holds and is met earlier | §14.1 TB, TDS; `ux-spec-folds/tech-lead-signoff.md` "Which branch it rides" |
| §14.1's merge order with 3a, 6a–6c (2026-10-21) | The same steps plus 3b (this file's carrier, docs only) and 5a (the TalkBack board: after 3a and 5, before 6b and 7, free against 4, 6, 6a, 6c), with the TalkBack brief's §13.3 second-to-merge duties and the ux-spec-folds sign-off's eleven-hunk rule for 3a | §14.1; `talkback-board/tech-lead-brief.md` §13.2; `ux-spec-folds/tech-lead-signoff.md` |
| §16 #22 "**Open.** AC-18 is the game-designer's. Suggested correction …" (2026-10-21, as of `aaaba67`) | Closed: the game-designer's Amendment 1 (`1afae9f`, AC-18a–d and AC-2c) is that correction, headed the same day (its commit `1afae9f` is dated 2026-10-23) | §16 #22; `game-designer-hint-timeout.md` Amendment 1 (N-4) |
| §16 #15 "Routed … ux-designer rules the spoken form" | Closed by D-05e: one stop, "Elapsed time, 5 minutes, 12 seconds", never a live region; built on `bugfix/home-chip-arrival`, captured by 18g | §16 #15, §14.1 F-1; `docs/ux-spec.md` §4.3.2 |
| §16 #18 "**If it is wanted**, the mechanism is the window theme … rides `bugfix/home-chip-arrival` if that branch runs one, else `feature/candy-backdrop`" | Wanted, by D-05f; the mechanism unchanged; on `bugfix/home-chip-arrival`, whose 15e sitting is owed | §16 #18, §14.1 F-1, §10; `docs/ux-spec.md` §6.2, criterion 29 |
| §10's "`res/values-v27/themes.xml` and `res/values-night-v27/themes.xml` too **if §16 #18's change lands**", and "F-1 **if** its fix changes the first frame"; §14.1 F-1's "its own 15e sitting **if** the fix changes Home's first frame"; §15's "F-1's **if it carries one**" | Both unconditional: the two v27 files are start-path files, and F-1's sitting on `Pixel_9a_bench` is owed (D-03n changes the no-chip split and `ui/home/`'s measuring; D-05f edits two start-path files) | §10, §14.1 F-1, §15 |
| §8.1's "`:domain:test` — 36 test files" | 37 `.kt` files, 30 of them `*Test.kt`, on `develop` at `4ff4ec6` | §8.1 (N-8) |
| §13.1 LN-7 item 4, "release-manager, checklist 1.4 and 4.6a" | "3.5 and 4.6a": 1.4 is pre-cut and cannot list an AAB; the listing is 3.5's and 4.6a's on `release/3.0.0` | §13.1 LN-7 (N-2b) |
| The uncited `docs/tech/audio-silent-mode.md` at §2.4, §12 and §14.1's audio row, read as if on `develop` | Each citation says the file is on `feature/audio-review` until that branch merges; moot at the merge — *moot since 2026-10-24: audio merged as `6aa7624`, and each citation now says "on `develop` since `6aa7624`", the N-5 wording kept beside it as the record* | §2.4, §12, §14.1 (N-5) |
| Row TB's, step 5a's and §16 #24's vehicle for Q-023's "wait, or ship option 2": the `Q-PUBLISH-3.0.0` summary, "rather than raising a new question" (2026-10-24, as transcribed from `ux-spec-folds/tech-lead-signoff.md`) | One pre-cut `Q-<NNN>`, queued by the producer only when `feature/talkback-board` is the last item standing (`Blocks: nothing`): `Q-PUBLISH-3.0.0` is written after the bundle (checklist 4.8) and cannot carry a pre-cut choice; checklist 1.1a's "put to the founder through `Q-PUBLISH-3.0.0`" is the release-manager's to align | §14.1 TB, step 5a, §16 #24; `phase-14/tech-lead-signoff-tech-design-sync.md` |
| `CLAUDE.md`'s `Check command: ./gradlew check` (§9.1) | `./scripts/check.sh`, the lock around `./gradlew check` (check-gates C2; `AGENTS.md` identical) | §9.1, §9.6 |
| §9.7's "`release/**` is ruled but not built" (2026-10-21) and this table's "not built until check-gates C3b merges" | Built (C3b `8687fca`), signed off 2026-10-24; live on `develop` at the merge | §9.7 |
| The R-2 brief §3's `trap 'rm -rf "$lock"' EXIT INT TERM HUP` | Traps that exit (129/130/143) beside an EXIT trap that releases, set only once the `mkdir` succeeds | §9.6 |
| §7.3 item 2's `generate(n) = generateLadder(clamp(n)).last()` | `generateLadder(n).last()`, the clamp inside the sequence | §7.3 |
| §8.1's "no wall clock" with no exception, and §8.4's unqualified instrumentation of `:domain:test` | The four solver deadline tests are the recorded exception, placed in their own task outside kover (row DT) | §8.6; `code-auditor-check-gates.md` finding 4 |
| "`check` wall clock ≈ 11 min" (§9.1, §10) | ≈ 11.5–12 min with `--rerun-tasks` on a quiet host; the branch's additions are seconds | §7.3, §9.1, §10 |
| R-7's and checklist 1.2/3.1's `FUZZ_SEEDS=100000 ./scripts/check.sh` | The same with `--rerun-tasks`, so a cached fuzz prints its evidence | §7.3, §14.1 R-7; `77dcf88` |
| §16 #23, #24 and #25 as numbered on `feature/phase-14-check-gates` (`74eec52`, the R-2 sign-off's transcription: the solver deadline tests under kover's agent, `scripts/check.sh` after `kill -9`, and `app/build.gradle.kts`'s deprecated `assets.srcDir(…)`), and the header's "§16 #23–#25" | §16 #25, #26 and #27, the same three rows, renumbered at check-gates' `develop` merge-in (`ffa49aa`, 2026-10-24): `feature/tech-design-sync` reached `develop` first with its own #23 (the gameplay cell size) and #24 (the TalkBack board), and the branch that merges second continues after `develop`'s highest number and updates every cross-reference in this file (here only the header's; at the merge-in's re-read no other tracked file cited the three numbers). Reports that cite the old numbers are records and stay as written | §16 #25–#27, the header; `phase-14/tech-lead-signoff-tech-design-sync.md` item 4 |
| §14.1 step 3b's "takes `develop` in and keeps both, distinct sections", read as the whole rule for a merge-in that conflicts in this file (2026-10-24, row TDS) | **The merge-in rule for this file** (tech-lead ruling, `phase-14/tech-lead-signoff-tech-design-sync.md` item 4, written here at check-gates' merge-in re-read; the figure clause and the last sentence are that re-read's). The branch that merges second takes `develop` in on its own branch, never a rebase. Its developer makes the merge-in by the rules below, or the producer for a docs-only branch with no developer. The Check re-runs on the merged tree, and a fresh tech-lead re-reads the merge commit's conflict hunks (`git show --cc`) before the `--no-ff` merge; docs hunks need no code-auditor. Hunks *(numbered here (1)–(5); the ruling's item 4 numbers its hunks 1–7, so this row's (1) is item 4's 1, (2) is its 3 and 6–7, (3) is its 4, (4) is its 2, and (5) is its 5 — a citation by number says which numbering it uses; mapping added at r1-close loop 1, code-auditor F-6)*: (1) the two branches' dated sync sentences in the header: both, `develop`'s first; (2) amendments to different rows, steps or sections, and rows adjacent only: both, in the document's order; (3) both sides number new rows in one list (§16): the second to merge renumbers its own rows to continue after `develop`'s highest number, updates every cross-reference to them in this file, and adds a §17 row naming the old numbers; reports that cite the old numbers are records and stay as written; (4) one cell holding a figure measured at two trees: not both. The cell takes the merged tree's own figure when the merge-in's Check measures it, else the later tree's, and the replaced figures stay as a dated "until then" note; (5) §17 rows: both, `develop`'s first. A hunk that none of these decides is a choice of text, not merge mechanics: it stops and comes to the tech-lead before the merge-in is committed | §14.1 step 3b, §16, §8.1, the header; `phase-14/tech-lead-signoff-tech-design-sync.md` item 4; `phase-14/tech-lead-merge-read-check-gates.md` |
| §8.1's `:domain:test` cell "280 tests in 40 files at check-gates", as the merge-in resolved it by item 4's hunk 2, and its build-logic row's "not reachable from the root `check`" | 294 tests in 33 classes from 41 `.kt` files, measured on the merged tree `ffa49aa`. Build-logic's 55 tests are reached by the root `check` (`build.gradle.kts:18-20`, `7b28007`, 2026-08-27); CI's `-p build-logic test` step is a fail-fast first run, and `ci.yml:48-49`'s comment is corrected with finding 7's edit | §8.1, §9.7, §14.2; the merge report's open issues 2 and 3 (`phase-14/android-game-dev-check-gates-merge.md`) |
| §7.2's "What 'No winning move' means today" (a transcription of `develop` at `0582e3f`, 2026-10-21) and the 2026-10-21 box at its head ("replaced when R1 merges") | "What 'No winning move' means now": three outcomes, only ProvenDead raises the banner, and Undecided gets its own notice in the banner's slot, as built (R1, merged as `b1a6cf9`). Both older texts stay below it, retitled "meant until R1 merged", as the record | §7.2; `hint-undecided/tech-lead-signoff.md` §4 items 1 and 5 |
| §10's Hint-latency row ("a miss reads "No winning move"", and its "once R1 merges" parenthetical), §15's row "Hint shows "No winning move" on a solver budget miss", §14.1 R1 and §16 #21 as pending | A miss reads Undecided, never "No winning move"; §15's row is the residual Undecided rate off the book, unmeasured until R5 (3.1); R1 done and #21 closed on the merge (`b1a6cf9`) | §10, §14.1 R1, §15, §16 #21; same §4 items 2–4 and 6 |
| The branch-conditional audio text: the header's and §12's "on `feature/audio-review`" for the haptics ruling; §2.4's, §12's and §14.1's "the file is on `feature/audio-review` until that branch merges"; §9.8 rule 3's "If check-gates merges after `feature/audio-review`"; §12's "inline in `SoundPoolPlayer` on `develop`", "On `develop` today: twelve `.ogg`" and "Device checks D1–D10 need a physical phone"; §13's Sounds row "once `feature/audio-review` merges"; §14.1's audio row and step 5 as pending; §16 #17 as open | The as-built state at `6aa7624`: `GameAudioAttributes`, the twelve WAVs, the three gates and where each lives, `HapticsSetting`, the device checks proven and the two halves still open (Q-025); check-gates merged first (`dbd0a6c`), and audio's main code compiles with 0 `w:`; the audio row done with its merge-in record; #17 closed. Each older sentence stays in place, dated. This also corrects the audio sign-off's §6 line "§17 → nothing superseded" | header, §2.4, §9.8, §12, §13, §14.1, §16 #17; `audio-review/tech-lead-signoff.md` "Merge-in re-read" R.4 |
| §9.8's table row and rule 2 counting 29 `createComposeRule` suites (counted at `e1875cb`, 2026-10-17) as the current figure | 33 on the merged tree `6aa7624` (33 suites import the v1 rule; audio's merge-in Check printed 33 such `w:` lines), one class on the v2 rule under §8.1's convention; new suites stay on v1 until the §14.2 migration (§9.8 rule 2's recorded exception) | §8.1, §9.8; `audio-review/tech-lead-signoff.md` §2.4 and R.4 item 3; `hint-undecided/tech-lead-signoff.md` §2 finding 4 |
| §14.1's second-to-merge paragraph, "resolves the conflicts", with no rule for a `.planning/` record; a pre-ruled merge-in measured against the named code branches only (`audio-review/tech-lead-signoff.md` §2.2); and audio's merge commit citing this table's merge-in rule for `.planning/questions/FOUNDER.md` | **Record files in a merge-in** (§14.1, below the second-to-merge paragraph): entries kept by the record's own format, in its order, no entry edited; a conflict inside an entry, or a shared id, stops for the producer; a pre-ruling measures against `develop`'s tip as it will be at the merge-in, docs branches included. This table's merge-in rule (its row on §14.1 step 3b) governs `docs/tech-design.md` only | §14.1; `audio-review/tech-lead-signoff.md` "Merge-in re-read" R.2 |
| §16 #30's fallback as `hint-undecided/tech-lead-signoff.md` §1.1 ruled it: "a founder-run TalkBack pass before release", as a non-blocking founder question | The founder never tests by hand (`audio-review/tech-lead-signoff.md` §4): the founder question asks only for hardware set-up that lets QA drive TalkBack; if none works by the cut, the two halves are one disclosed line in the Q-PUBLISH-3.0.0 summary (tech-lead decision, r1-close loop 1, code-auditor I-3; confirmed with L1-1's wording at the r1-close sign-off) | §16 #30; `phase-14/tech-lead-signoff-tech-design-r1-close.md` |
