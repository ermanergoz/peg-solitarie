# Technical design — Peg Solitaire

The canonical home for decisions that outlive the report that made them. Six sign-offs have now
been audited against a *report* instead of a design; this file exists so the seventh is not.

**Status and ownership.** `docs/tech-design.md` is the **tech-lead's** required output (release
blocker R-6, `.planning/reports/phase-14/tech-lead-signoff-r3-signing.md` §G). It is *seeded* here
by the release-manager, on the release-documentation branch, because it was blocking the release
paperwork and a docs commit may land on a release branch. Everything below is a **transcription**
of a decision already recorded in a committed report or phase document, with a pointer to the
place that decided it. **No decision was invented here.** Anything not yet decided is in §9 and
says so. The tech-lead owns every future edit, and owns re-writing any framing below that it
judges inaccurate.

Format: one row or block per decision — what was decided, one line of why, and where it was
decided. Entries are not deleted; a superseded entry gets a "superseded by" line.

---

## 1. Stack pins

Authority: `gradle/libs.versions.toml` (each pin carries its own rationale comment in that file),
`build-logic/src/main/kotlin/android-app.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`.

| Pin | Value | Why (one line) | Decided in |
|---|---|---|---|
| Kotlin | 2.3.20 | The latest 2.3.x with an exactly matching KSP build; no KSP exists for 2.4.x and this project is KSP-only, never KAPT | `libs.versions.toml` [versions] comment, 2026-06-28 |
| KSP | 2.3.9 | Built against Kotlin 2.3.20; the stdlib dependency must equal the Kotlin pin | same |
| AGP | 9.1.0 | The tooling's max-supported AGP; resolves on Google Maven | same |
| Gradle wrapper | 9.5.1 | AGP 9.x requires ≥ 9.1; verified green here | `gradle-wrapper.properties` |
| JDK / toolchain | 17 | AGP 9.x minimum, set via `jvmToolchain(17)` in both convention plugins | `android-app.gradle.kts:52`, `kotlin-jvm.gradle.kts:16` |
| Compose BOM | 2026.06.00 | One mutually-compatible Compose set; no member is ever hand-pinned | `libs.versions.toml` |
| Hilt | 2.60 | 2.57.1's Gradle plugin needs the legacy Android `BaseExtension`, which AGP 9.1.0 removed — verified failing 2026-06-28 | same |
| Room | 2.8.4 | Stable line, not the KMP-focused 3.0 alpha; compiler under KSP | same |
| Navigation Compose | 2.9.8 | Latest stable on the compileSdk-36-safe line; 2.10.x needs compileSdk 37 | same |
| `androidx.core` | 1.18.0, **forced** | 1.19.0 requires compileSdk 37, not installable in this environment | `app/build.gradle.kts:447-454` |
| `androidx.lifecycle` | 2.9.4, **forced as a group** | Atomic group; 2.11.x `-compose` variants require compileSdk 37 | `app/build.gradle.kts:455-461` |
| compileSdk / targetSdk / minSdk | 36 / 36 / **24** | Centralised in the convention plugin | `android-app.gradle.kts:26-30` |

**Known divergence from `rules/kotlin-android.md`, recorded rather than hidden.** The studio's
stack rule names Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.20, Compose BOM 2026.09.00, Hilt 2.60.1, KSP
2.3.12 and **minSdk 26**. This repo predates that rule and every version divergence has a recorded
environment reason above (the compileSdk-37 wall, the KSP/Kotlin pairing). **`minSdk 24` has no
such recorded reason** — it is simply older than the rule. Raising it is a player-visible decision
(it drops devices) and is **open**: see §9, routed to the tech-lead with a product-side question
for the founder.

**D-18 — release builds are not offline-safe on a cold cache.** AGP's
`produceReleaseComposeMapping` implicitly resolves `org.jetbrains.kotlin:compose-group-mapping` at
a coordinate that tracks the Kotlin pin, and nothing in this repo can pin it because nothing in
this repo asks for it. One warm online run must populate the Gradle cache before a release build.
`./gradlew check --offline` is unaffected — `check` never builds the release variant.
*Decided in:* `gradle/libs.versions.toml` [versions] header block.

## 2. Architecture boundaries

| Decision | Why | Decided in |
|---|---|---|
| `:domain` is pure Kotlin/JVM with **no** Android dependency and **no** non-test dependency at all | Core logic is testable on the JVM in seconds; the purity is structural, not a convention | `domain/build.gradle.kts`; `rules/kotlin-android.md` |
| Every platform SDK type (`com.google.android.gms.*`, `com.google.android.ump.*`) lives under `app/.../platform/` and nowhere else, enforced by a bytecode (ArchUnit) test with a deliberate negative fixture | An SDK type leaking into `data/` or `ui/` is how a seam quietly stops being a seam; a boundary asserted by review decays, one asserted by a test does not | PLAT-04, `.planning/phases/06-platform-foundation-build-hardening/` |
| Ads, Play Games and cloud save sit behind the `AdGateway`, `GamesService` and `CloudSaveStore` seams, with no-op implementations that degrade silently | The game must be fully playable signed-out and offline; a stub that auto-grants is also what hides missing UI, so the stub-first ordering is deliberate and the real implementation is wired last | ROADMAP phases 6, 9, 11, 13 notes |
| Cloud save is a **second data source behind `ProgressRepository`**, never a method the UI sees | Repositories stay the single source of truth; no screen knows whether a value came from Room or from the cloud | `.planning/phases/13-cloud-save/13-CONTEXT.md` (Phase Boundary) |
| The `AdReason.RETRY` path is never ad-gated | Retry is the player's recovery action; charging for it is the one ad placement that would be resented | ROADMAP phase 10 notes |

## 3. Cloud save

| # | Decision | Why | Decided in |
|---|---|---|---|
| 3.1 | **The merge is a monotonic union** — `max` stars, `min` time, `max` score per `(mode, levelId)`, `max` ladder, `max` played time — extracted into `:domain` as a pure function and proved **commutative, associative and idempotent** by JVM tests *before* any SDK code was written | A join-semilattice cannot lose progress whichever side runs first or twice; the proof is what makes "no prompt and no loss" a property rather than a hope | `13-CONTEXT.md` Phase Boundary + Discretion; ROADMAP phase 13 note; plan `13-01-PLAN.md` |
| 3.2 | **`RESOLUTION_POLICY_MANUAL`**, never the default most-recently-modified policy; conflicts are resolved by merging both sides and committing the merge | Last-writer-wins silently deletes the loser's progress — the exact outcome 3.1 exists to prevent | ROADMAP phase 13 note; `13-CONTEXT.md`; plan `13-09-PLAN.md` |
| 3.3 | **D-03 — restore is fully silent.** No toast, no dialog, no Settings status row; catalog star pips and the Challenge tile update through the existing Room/DataStore flows. A cloud import must **never** fire the "New best!" flag or the Results celebration | Those are *per-play* signals; firing them *per-merge* would congratulate a player for something they did on another device. The stock "Welcome back" popup stays the only launch-time signal | `13-CONTEXT.md` D-03 |
| 3.4 | **D-14 — the replace path wipes everything**: `level_result`, `play_session`, the Challenge ladder (back to level 1) and **both** resume snapshots, then imports the cloud save. Dialog copy must state exactly what is replaced; no cloud save for the new account means a clean slate | The device becomes that account's device outright, and the previous account's data already lives in its own cloud save. A half-wipe would leave one account's rows attributed to another | `13-CONTEXT.md` D-14 |
| 3.5 | **D-13 — every dismissal means keep-local.** Back, scrim tap, recents: all route to "keep this device's progress" | The destructive choice must be the one that is explicitly taken, never the one that happens by walking away | `13-CONTEXT.md` D-13; the single call chain and the not-`SignedIn` early return were traced and PASSed in `.planning/reports/phase-13/code-auditor-phase-sweep.md` ("Destructive paths") |
| 3.6 | **D-04 — a stale in-progress game is kept and the ladder jumps underneath it.** Nothing on the resume path is touched by a cloud event | The player's half-finished board is theirs; the ladder is an aggregate and can move without disturbing it | `13-CONTEXT.md` D-04 |
| 3.7 | **The in-progress board snapshot never leaves the device** | CLOUD-04; the payload stays a pure semilattice of bests, which is what makes 3.1's laws hold | `13-CONTEXT.md` D-06; ROADMAP CLOUD-04 |

### 4. D-07a — constraints on any future `SCHEMA_VERSION` bump

**A hard gate.** `SCHEMA_VERSION` is 1 and no payload in the world declares 2, so none of this is
reachable in the shipped build — **and today both the right and the wrong behaviour pass**, which
is exactly why it is written here and not left in a review report. Any reader bumping
`SCHEMA_VERSION`, widening the payload or adding a writer must satisfy all three, each with a test
that fails first. *Decided in:* `.planning/phases/13-cloud-save/13-CONTEXT.md` §D-07a, from the
phase-13 code-auditor sweep F-1/F-2/F-3; re-verified against the code in
`.planning/reports/phase-14/tech-lead-prerelease-signoff.md`.

- **C-1 — the read path and the write path must refuse the same payload.** `decode` refuses a
  payload whose `schemaVersion` exceeds `SCHEMA_VERSION`; `mergeBytes` reaches the same bytes via
  `decodeOrEmpty`, which maps *both* "corrupt" and "decodable but newer" to the empty identity, and
  `commitOrDiscard` then writes a full overwrite. "Refuse to read it, then delete it" is therefore
  reachable. **Before any bump: `mergeBytes` must distinguish the two null causes and, for
  "decodable but newer", return the remote bytes verbatim or abort the push — never the identity.**
  This goes live during any staged rollout, which is the normal state of two devices on different
  app versions, and it is silent data loss on the device that is behind.
- **C-2 — cap the row count at decode; do *not* validate Classic ids against the shipped set.**
  Values are validated and the Challenge `levelId` is bounded, but cardinality is not, and the
  codec's own threat model treats the payload as attacker-influenceable. Uncapped rows persist,
  inflate the leaderboard and achievement aggregates, and can wedge every later push at
  `OVER_CAP`. Rejecting an *unknown id*, however, would drop a future sixth board's rows from a
  newer writer — the forward-compatibility break D-07 forbids. Dropping unknown-id rows during the
  **trim** is admissible.
- **C-3 — widen `dropCount` to `Long`** in the same change that lifts either of the above. The
  `Int` product wraps with a full ladder and a large overshoot; the loop still terminates because
  of its `maxOf(1, …)` floor, but degrades to one row per pass.

## 5. Build, signing and gates

| # | Decision | Why | Decided in |
|---|---|---|---|
| 5.1 | **Signing credentials come only from environment variables** — `PLAY_UPLOAD_KEYSTORE`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`, `PLAY_UPLOAD_STORE_PASSWORD`. No `local.properties` source, no placeholder-prefix convention, no committed keystore, no debug-keystore fallback | D-16: a committed debug-keystore shortcut is exactly the kind of thing that survives to a store upload. `local.properties.template` names the four variables with **nothing to copy** (no `=` on any line) | `.planning/reports/phase-14/android-game-dev-signing-config.md`; PASS in `tech-lead-signoff-r3-signing.md` §B |
| 5.2 | **No third state.** `releaseSigning` is `null` or all four credentials; `signingConfigs { }` is created only in the second case and the release variant reads `findByName("release")`, so the artifact is either signed with the upload key or unsigned exactly as before | The forbidden substitution is not merely not taken — there is no code path that could take it | same |
| 5.3 | **Nothing interpolates a credential into a message**, and `SigningResolutionResult.Resolved.toString()` is overridden to redact | A Gradle stack trace is the thing that gets pasted into an issue | same |
| 5.4 | **Passwords are not trimmed.** Whitespace inside a password is a character of the password | Trimming would surface much later as an unrecoverable-key error with no explanation | same |
| 5.5 | **`enableV3Signing` stays at the AGP default. Do not revisit before v1.1** | The upload artifact is an AAB that Play App Signing re-signs before distribution, so the scheme set on the *upload* APK is invisible to every installed device, and key rotation is a Console operation on the app signing key | `tech-lead-signoff-r3-signing.md` §B |
| 5.6 | **The debug snapshot picker lives in a `src/debug` source set**, with one Hilt binding per build type and a release binding that is inert twice over (`debugBuild = false` and an empty `show`) | L-3: a debug-only QA surface must be *structurally* out of the release artifact, not merely disabled. Absence was proved at class-output level, in the R8-minified `classes.dex`, and by the QA-only title string | `.planning/reports/phase-14/code-auditor-prerelease-fixes.md`; `tech-lead-prerelease-signoff.md` §A |
| 5.7 | **Kover and the fuzz harness are decided, not waived — and bounded.** kover: measure `:domain` first; at ≥ 90 % line coverage wire `koverVerify` at 90 % into `check` and stop; below it, set the threshold at the measured floor rounded **down** to a whole percent and record the shortfall and the target here. fuzz: one test class replaying `LevelGenerator.generate(n).solution` through `RulesEngine`, asserting a single peg plus regeneration determinism, over a seed count read from `FUZZ_SEEDS` through the existing `TestFlags` pattern, one Gradle task shaped like `:domain:depthSweep`; `check` at 1,000, CI at 10,000, release at 100,000. Measure generator throughput before choosing the `check` tier — `depthSweep` already costs ~11 minutes and a second multi-minute gate is not acceptable in every developer's `check` | The product's Core Value is "always-solvable procedurally-generated levels", and solvability is currently asserted on six sampled levels against `rules/testing.md`'s ≥ 1,000 / ≥ 10,000 / ≥ 100,000 tiers. Nobody writes tests to chase a coverage number in a release week | `tech-lead-prerelease-signoff.md` §D and its bounding paragraph. **Status: open (R-2 / R-7).** |
| 5.8 | **The three literal gates read `src/main` + `src/release` + `src/debug`; test source sets stay excluded and must say so**, and every `GradleException` must name the roots it actually scanned. Plus a configuration-time tripwire that fails loudly on an unknown `src/*` directory | A real ad-unit id in `src/debug` is AdMob invalid traffic and risks the account, and a focus request under `src/debug` would silently falsify every QA audio observation. `src/test*` is excluded deliberately because those sources legitimately name the test-publisher prefix and the key names. A gate whose failure text misdescribes its own scope is how the previous false claim happened | `tech-lead-prerelease-signoff.md` §B. **Status: ruled, not yet implemented (R-1′).** |
| 5.9 | **`isReleaseInvocation` becomes a packaging-verb test**, not a `contains("elease")` substring test — and the two ID checks move onto `packageRelease`/`packageReleaseBundle` **in the same change, never split** | Narrowing the configuration gate alone would stop the ad-ID and Play-Games-ID gates firing on analysis tasks while the execution-time backstops do not yet cover the packaging path; a lifecycle-task dependency is a *sibling* of the package task and Gradle may order it after, leaving a **signed** AAB carrying an unresolved ad id on disk after a failed build | `tech-lead-signoff-r3-signing.md` §C and §D. **Status: ruled, not yet implemented (R-1′).** |
| 5.10 | **CI must run `check`.** `.github/workflows/ci.yml` runs `ktlintCheck detekt :domain:test test assembleDebug`, and all four literal gates are wired into `check` and only into `check` — so the integration gate on every `feature/* → develop` PR never executes them | Widening a gate that CI does not invoke is half a fix | `tech-lead-signoff-r3-signing.md` §G. **Status: open (R-1′c).** |
| 5.11 | **`InMemoryPreferencesDataStore` carries its own contract test, and that test is the *only* guard on it.** Any future edit to the double that is not accompanied by a test failing without it removes the mitigation **silently** — the ten migrated classes are green against both known mutations | This is a rule on the file, not a risk in a report nobody re-reads (F-11) | `tech-lead-signoff-r3-signing.md` §A, Open issues |
| 5.12 | **`build-logic/` is outside ktlint and detekt.** The `standards` convention plugin is applied to `:app` and `:domain` only, so the build-logic Kotlin files are linted by neither | Pre-existing and true of the ad-ID resolver too; the files carry discriminating unit tests. Recorded so the next reader does not mistake it for coverage (F-5) | `tech-lead-signoff-r3-signing.md` §B |
| 5.13 | **Do not enable Gradle's configuration cache without re-auditing the signing capture.** `signingEnv` resolves the environment eagerly into a `Map<String, String>` at configuration time and `verifyReleaseSigning`'s `doLast` closes over it, so two passwords would be serialised into the cache entry (encrypted at rest since Gradle 8.1 — low residual risk, wrong shape). The fix for that day is to carry the `Provider` into the task and resolve it there | Recorded so the shape of the fix is not rediscovered under time pressure | `tech-lead-signoff-r3-signing.md`, Open issues |
| 5.14 | **detekt reads the build-type source sets** via one widened task rather than `detektDebug`/`detektRelease`, because the convention plugin is applied to `:domain` where the variant tasks do not exist and the plugin deliberately carries no AGP types | One task, one config, one report, one name in `check`, at 2 s instead of 44 s (F-1) | `tech-lead-prerelease-signoff.md` §A |

## 6. Assist and generator

| Decision | Why | Decided in |
|---|---|---|
| Hints answer from a **committed depth-4 opening book** first and fall through to a bitboard solver with a restart policy; the book's "no winning move" is a **proof**, a book miss is a fall-through | Research measured that a strengthened solver alone cannot meet the 1.5 s deadline for Wiegleb-45 — 0 of 76 depth-≤4 positions resolved even at ~15M nodes/s, a ~4-order-of-magnitude gap | ROADMAP phase 8, escalation resolved 2026-08-29; `08-BOOK-EVIDENCE.md` |
| **Diamond-41's fewest-pegs floor is 2, not 1**, proved two ways: a committed 38-move line replayed through the unchanged `RulesEngine`, and the GF(4) colour invariant checked exhaustively over all 41 single-peg positions | Star bands anchored on an unreachable one-peg target made the board's top rating impossible to earn | ROADMAP phase 8 criterion 2 (D-06); plan `08-06-PLAN.md` as a hard gate |
| Star bands are derived **per board** from that board's own `minimumPegs` | One shared assumption cannot be right for five different geometries | ROADMAP phase 8; plan `08-07-PLAN.md` |
| `DepthSweepTest` runs in its own `:domain:depthSweep` task wired into `check`, not in the default `test` run, and is double-gated by a task property *and* a body check | The sweep costs minutes; the per-commit `:domain:test` gate is held to seconds. Neither half silently disables the other | `domain/build.gradle.kts` |

## 7. Session, ads and Play Games

| Decision | Why | Decided in |
|---|---|---|
| Play Games is **explicit opt-in**: the SDK is never initialised until the player accepts the one-time prompt or taps the Settings row; sign-in is once per process | PGS v2 has no sign-out and no sign-in wall; v1 patterns must not be ported | ROADMAP phase 11 notes, planning decision 2026-09-03 |
| Leaderboard/achievement submission hooks into `ResultsViewModel.recordSolvedPlayOnce()`, **not** `GameViewModel` | `finishedEmitted` is reset by `onUndo()` and would double-submit; the Results hook already has a process-death-safe exactly-once guard | ROADMAP phase 12 notes |
| Aggregates are **SQL-backed**, never an in-memory fold | Challenge reaches thousands of rows | ROADMAP phase 12 notes |
| The interstitial session boundary is measured from `ProcessLifecycleOwner` foreground/background transitions, with the cap persisted so it survives process death | A per-session cap that resets on process death is not a cap | ROADMAP phase 10; plan `10-02-PLAN.md` |
| **D-04a — "Next level" serves `clamp(max(settledLevel(), solvedLevel + 1))`**, read through the *awaitable* ladder value rather than a cached `stateIn` value, with the arithmetic outside the stateless leaf composable | Reading the cached value can serve a level the player has already completed; the leaf reports a tap and owns no navigation arithmetic | `.planning/reports/phase-13/game-designer-next-level-ruling.md` §B; implementation PASSed in `tech-lead-prerelease-signoff.md` §A, pinned by seven named invariant tests |
| `ChallengeNextLevelTest`'s seeded, never-advanced `ladderScope` fixture is **left alone** and not folded into the shared DataStore double | That fixture is what makes the settled-versus-cached distinction observable at all, and it is what pins D-04a's seven invariants | `tech-lead-signoff-r3-signing.md` §F |

## 8. Testing

| Decision | Why | Decided in |
|---|---|---|
| Unit tests use a **shared in-memory `DataStore<Preferences>` double** across all ten affected classes; no test constructs `PreferenceDataStoreFactory.create` without an explicit scope | Real file I/O on DataStore's own `Dispatchers.IO` scope, outside `runTest`'s virtual clock, produced a 60-second hang reproduced 5 times in 23 runs and absent in 52 runs after the migration (Fisher p = 0.020) | `tech-lead-prerelease-signoff.md` §C; `tech-lead-signoff-r3-signing.md` §A; `.planning/reports/phase-14/qa-engineer-datastore-acceptance.md` |
| The double must preserve `updateData` read-modify-write atomicity and `data`'s current-then-subsequent emission, and carry its own test pinning both | An unverified fake under ten classes and ~200 tests is a worse risk than the flake it replaces | `tech-lead-prerelease-signoff.md` §C, condition 1 |
| **If a 60-second `UncompletedCoroutinesError` is ever seen in this suite again, `AppForegroundTrackerTest` is the first place to look**, and the injected-clock fix is promoted immediately | Two `first { }` awaits there have open predicates against an independently sampled wall clock; a backwards clock step makes them unsatisfiable, and the symptom is indistinguishable from the fixed one | `tech-lead-signoff-r3-signing.md` §E |
| The injected clock in `AppForegroundTracker` is **deferred to after v1.1** | It re-cuts the interstitial session boundary — the mechanism that limits how many ads a long sitting can produce — days before a first release, and the device confirmation of its A1 assumption is still open | same |

## 9. Open — decided by nobody yet

Listed so they are not mistaken for settled. Each needs an owner's ruling, not a transcription.

1. **`minSdk 24` versus `rules/kotlin-android.md`'s 26** (§1). Player-visible; needs a tech-lead
   ruling and probably a founder question about the device floor.
2. **R-2 / R-7 — the kover and fuzz gates** (§5.7). Bounded and decided in shape; **unimplemented**,
   and R-7's 100,000-seed release run cannot happen until R-2 lands.
3. **R-1′ — the gate task** (§5.8, §5.9, §5.10). Ruled in full; unimplemented. Rulings 5.8 and 5.9
   must not be split.
4. **An in-app licences surface.** `rules/ip-policy.md` asks for attribution in a licences screen
   as well as in `THIRD_PARTY.md`; Settings has no such row. Nothing shipped carries an attribution
   *obligation* (everything is CC0, OFL, Apache-2.0 or a permitted Google SDK), so this is a
   studio-rule gap rather than a legal one. Needs a ux-designer and a developer.
5. **`docs/gdd.md`** — flagged by the game-designer, not scheduled.
6. **`THIRD_PARTY.md` §5b's per-coordinate enumeration** — closed by one dependency listing on the
   release branch; see `docs/release-checklist.md` §3.5.
7. **Post-v1.1 backlog, recorded so none of it is rediscovered:** F-4 (`ResultsViewModel` sits
   exactly at detekt's 11-function ceiling, so the next change to that screen forces an extraction
   or a `@Suppress`), FU-1 (snapshot handle lifetime and pull diagnosability), F-9 (derive the gate
   source roots from `android.sourceSets` when a flavour or a third build type appears),
   `MagicNumber`'s `**/testDebug/**` alignment, `isReleaseInvocation`'s substring guard for
   hand-run release analysis tasks, and the `Pixel_9a` environment fault preserved as a control.
