---
phase: 01-domain-foundation-playable-board
plan: 01
subsystem: infra
tags: [gradle, kotlin, agp, compose, detekt, ktlint, archunit, version-catalog, convention-plugins, ci]

# Dependency graph
requires: []
provides:
  - Two-module Gradle project — pure-JVM :domain (no Android plugin) + Android :app
  - gradle/libs.versions.toml as the single source of truth for all pinned versions
  - build-logic/ convention plugins (kotlin-jvm, android-app, standards)
  - Standards gate wired into check: detekt (MagicNumber) + ktlint + custom dp/sp gate + ArchUnit domain-purity test
  - Gradle 9.5.1 wrapper bootstrapped from scratch (no system gradle)
  - Gitflow-gated GitHub Actions CI (.github/workflows/ci.yml)
affects: [01-02 domain core, 01-03 viewmodel-hilt, 01-04 board-canvas, 01-05 game-flow, all later phases]

# Tech tracking
tech-stack:
  added:
    - Kotlin 2.4.0, AGP 9.2.1 (built-in Kotlin), Gradle 9.5.1 wrapper, JDK 17 toolchain
    - Compose BOM 2026.06.00 + Material 3, activity-compose 1.13.0
    - dev.detekt 2.0.0-alpha.5, ktlint-gradle 14.2.0, archunit-junit4 1.3.0
    - JUnit4 4.13.2 + Truth 1.4.4 (catalog also declares Turbine/MockK/coroutines-test/Hilt for later)
  patterns:
    - Pure-JVM :domain module = structural android-free guarantee (no Android plugin on classpath)
    - build-logic convention plugins; every plugin version flows from the shared catalog
    - Standards-as-build-gate (ignoreFailures=false) green from commit 0
    - Custom Gradle verification task for the no-raw-dp/sp rule (no built-in detekt/ktlint rule exists)

key-files:
  created:
    - settings.gradle.kts
    - gradle/libs.versions.toml
    - gradle/wrapper/gradle-wrapper.properties
    - build-logic/src/main/kotlin/kotlin-jvm.gradle.kts
    - build-logic/src/main/kotlin/android-app.gradle.kts
    - build-logic/src/main/kotlin/standards.gradle.kts
    - config/detekt/detekt.yml
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/Placeholder.kt
    - app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt
    - .github/workflows/ci.yml
  modified: []

key-decisions:
  - "Pinned Gradle wrapper to 9.5.1 (AGP 9.2.1 requires >=9.4.1; matches detekt-alpha's tested pairing)"
  - "Used AGP 9.2.1 patch + its built-in Kotlin (org.jetbrains.kotlin.android removed — AGP 9 rejects it)"
  - "Deferred Hilt/KSP: no KSP build exists for Kotlin 2.4.0; empty Plan-01 app needs no DI"
  - "Pinned androidx.core to 1.18.0 (1.19.0 requires compileSdk 37, not installable here)"

patterns-established:
  - "Structural domain purity: :domain is a plain kotlin-jvm module + ArchUnit DomainPurityTest gate"
  - "Convention-plugin layer (build-logic) so :domain and :app share gates without copy-paste"
  - "Negative-check discipline: planted violations prove each gate fails before relying on it"

requirements-completed: [QUAL-01, QUAL-02, QUAL-03]

# Metrics
duration: 35min
completed: 2026-06-28
---

# Phase 1 Plan 01: Build Scaffold & Standards Gates Summary

**Two-module Gradle project (pure-JVM `:domain` + Compose `:app`) with detekt + ktlint + ArchUnit domain-purity + a custom no-dp/sp gate wired into `check`, plus gitflow-gated CI — all green on placeholder code from commit 0.**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-06-28T02:40Z (approx)
- **Completed:** 2026-06-28T03:10Z
- **Tasks:** 3
- **Files modified:** 28 (across 3 task commits)

## Accomplishments
- Bootstrapped the entire Gradle 9.5.1 wrapper from scratch (no system Gradle) and a two-module split where `:domain` is a pure Kotlin/JVM library with **no Android plugin on its classpath** (compiler-enforced purity).
- `gradle/libs.versions.toml` is the single source of truth; even `build-logic`'s plugin classpath pulls versions from it. No module hand-pins a version literal.
- Full standards gate runs under `check` and **fails the build on any violation**: detekt `MagicNumber`, ktlint, a named ArchUnit `DomainPurityTest` (bytecode-based, immune to Kotlin-2.4 source-parse risk), and a custom `dpSpLiteralCheck` Gradle task for QUAL-03.
- Proved each gate actually fails: planted `12.dp` → `dpSpLiteralCheck` fails; planted bare `42` → detekt `MagicNumber` fails (both reverted).
- Gitflow-gated CI (`feature/*→develop`, `release/*`/`hotfix/*→main`) running the full gate + `:domain:test` + `assembleDebug` on JDK 17.
- `./gradlew ktlintCheck detekt :domain:test test assembleDebug` is green; debug APK builds (~11 MB).

## Task Commits

1. **Task 1: Root scaffold, version catalog, two modules, convention plugins** — `b7dc7ec` (feat)
2. **Task 2: Standards gates (detekt + ktlint + ArchUnit purity + dp/sp) wired into check** — `58b70af` (feat)
3. **Task 3: Gitflow-gated CI workflow** — `0ce515f` (feat)

**Plan metadata:** (final docs commit — this SUMMARY + STATE/ROADMAP/REQUIREMENTS)

## Files Created/Modified
- `settings.gradle.kts` / `build.gradle.kts` — root build, includes `:app`, `:domain`, `build-logic`
- `gradle/libs.versions.toml` — all pinned versions + plugin-classpath libs + bundles
- `gradle/wrapper/*`, `gradlew`, `gradlew.bat` — Gradle 9.5.1 wrapper
- `build-logic/{settings,build}.gradle.kts` + 3 convention plugins (kotlin-jvm, android-app, standards)
- `config/detekt/detekt.yml` — MagicNumber + scoped-constants exceptions; FunctionNaming exempts @Composable
- `domain/build.gradle.kts` + `Placeholder.kt` + `PlaceholderTest.kt` — pure-JVM module, non-empty test
- `app/build.gradle.kts`, `AndroidManifest.xml`, `App.kt`, `MainActivity.kt`, `ui/theme/Theme.kt`, `res/values/{strings,themes}.xml`
- `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` — ArchUnit boundary gate
- `.editorconfig`, `.gitignore`, `gradle.properties`, `.github/workflows/ci.yml`

## Decisions Made
- **AGP 9.2.1 + built-in Kotlin:** AGP 9 ships Kotlin support; the `org.jetbrains.kotlin.android` plugin is rejected, so it is not applied (compose compiler plugin still applied explicitly).
- **Gradle 9.5.1 wrapper:** AGP 9.2.1 requires Gradle ≥9.4.1; 9.5.1 also matches detekt-alpha's tested pairing (CLAUDE.md's "9.1+" / RESEARCH's "latest 9.x").
- **detekt config:** `MagicNumber` active with scoped-constants exceptions (`ignorePropertyDeclaration`, `ignoreCompanionObjectPropertyDeclaration`, `ignoreConstantDeclaration`, `ignoreNamedArgument`); `FunctionNaming` exempts `@Composable`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Hilt/KSP deferred — no KSP build for Kotlin 2.4.0**
- **Found during:** Task 1 (convention plugins / app scaffold)
- **Issue:** The plan's `android-app` convention + app surface call for Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`) via KSP. KSP has **no release for Kotlin 2.4.0** (latest is 2.3.9, Kotlin-2.3 line; all `2.4.0-*` coordinates 404 on Maven Central). Hilt's annotation processor runs on KSP, so Hilt cannot be wired on the pinned Kotlin 2.4.0.
- **Fix:** Kept Hilt + KSP declared in the catalog but did **not** apply them. `App` is a plain `Application`, `MainActivity` a plain `ComponentActivity`. Plan-01's must-haves and acceptance criteria do not functionally require DI (empty themed app), so all gates + `assembleDebug` stay green.
- **Files modified:** build-logic/src/main/kotlin/android-app.gradle.kts, app/src/main/java/.../{App,MainActivity}.kt
- **Verification:** `assembleDebug` builds the APK; no Hilt graph needed yet.
- **Committed in:** `b7dc7ec` (Task 1). **See Blocker below — must be resolved before Plan 03 (which needs `@HiltViewModel`).**

**2. [Rule 3 - Blocking] AGP 9 built-in Kotlin — removed `org.jetbrains.kotlin.android`**
- **Found during:** Task 1 (first `assembleDebug`)
- **Issue:** AGP 9.2.1 enables built-in Kotlin and hard-rejects the standalone `org.jetbrains.kotlin.android` plugin.
- **Fix:** Removed that plugin from the `android-app` convention; kept `org.jetbrains.kotlin.plugin.compose`; configured the Kotlin toolchain via the AGP-registered `kotlin` extension.
- **Files modified:** build-logic/src/main/kotlin/android-app.gradle.kts
- **Verification:** `assembleDebug` green.
- **Committed in:** `b7dc7ec`

**3. [Rule 3 - Blocking] Pinned androidx.core to 1.18.0 (1.19.0 needs uninstallable compileSdk 37)**
- **Found during:** Task 1 (`checkDebugAarMetadata`)
- **Issue:** The transitively-resolved `androidx.core:core:1.19.0` requires `compileSdk 37`, which is not installed and not installable here (no `cmdline-tools`/`sdkmanager`; `platform-37` download 404). compileSdk is pinned at 36.
- **Fix:** Forced `androidx.core:core`/`core-ktx` to 1.18.0 (highest line with `minCompileSdk=36`) via a catalog-driven `resolutionStrategy.eachDependency`. The empty app uses no core-1.19 APIs.
- **Files modified:** gradle/libs.versions.toml, app/build.gradle.kts
- **Verification:** `assembleDebug` green.
- **Committed in:** `b7dc7ec`

**4. [Rule 3 - Blocking] Fixed ktlint-gradle catalog coordinate**
- **Found during:** Task 2 (build-logic classpath resolution)
- **Issue:** `org.jlleitschuh.gradle.ktlint:ktlint-gradle` (plugin-id-as-group) does not exist; the artifact group is `org.jlleitschuh.gradle`.
- **Fix:** Corrected the catalog library to `org.jlleitschuh.gradle:ktlint-gradle:14.2.0` (Gradle Plugin Portal).
- **Files modified:** gradle/libs.versions.toml
- **Committed in:** `58b70af`

**5. [Rule 2 - Missing Critical] Added strings.xml + themes.xml for a buildable host**
- **Found during:** Task 1 (manifest/resource processing)
- **Issue:** The manifest needs an app label and a window theme; without `@string/app_name` and a host `@style` the resource merge/build is incomplete.
- **Fix:** Added `res/values/strings.xml` (`app_name`) and `res/values/themes.xml` (`Theme.PegSolitaire`, framework parent — no legacy Material-Components dependency).
- **Files modified:** app/src/main/res/values/strings.xml, app/src/main/res/values/themes.xml
- **Committed in:** `b7dc7ec`

**6. [Rule 1 - Bug] @Composable exempted from function-naming (detekt + ktlint)**
- **Found during:** Task 2 (first gate run)
- **Issue:** Both detekt `FunctionNaming` and ktlint `function-naming` flag the PascalCase `@Composable AppTheme`.
- **Fix:** `ignoreAnnotated: ['Composable']` in detekt.yml; `ktlint_function_naming_ignore_when_annotated_with = Composable` in `.editorconfig` — the canonical Compose configuration.
- **Files modified:** config/detekt/detekt.yml, .editorconfig
- **Committed in:** `58b70af`

**7. [Rule 1 - Bug] ArchUnit test needed `@RunWith(ArchUnitRunner)` to be discovered**
- **Found during:** Task 2 (`:app:testDebugUnitTest` "no tests discovered")
- **Issue:** archunit-junit4 (JUnit4) requires the `ArchUnitRunner` to discover `@ArchTest` fields; without it the task fails on zero discovered tests.
- **Fix:** Added `@RunWith(ArchUnitRunner::class)` to `DomainPurityTest`.
- **Files modified:** app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt
- **Committed in:** `58b70af`

---

**Total deviations:** 7 auto-fixed (5 blocking, 1 missing-critical, 2 bug — items 6/7 both bugs).
**Impact on plan:** All necessary to achieve the plan's prime directive (gates green from commit 0). One carries forward as a blocker (Hilt/KSP, item 1). No scope creep; no must-have weakened — domain purity, all three QUAL gates, and the debug build are intact.

## Issues Encountered
- **No system Gradle.** Bootstrapped the wrapper by running a cached Gradle 9.3.1 in a temp dir (with a stub settings file) targeting 9.5.1, then copied the wrapper files in. Resolved.
- **Bleeding-edge version verification.** Live-verified every pinned plugin/dependency against the official Gradle Plugin Portal / Maven Central / Google Maven before locking the catalog (supply-chain mitigation T-01-SC). All resolved except the Hilt/KSP gap and the core/SDK gap handled above.

## Known Stubs
- `domain/.../Placeholder.kt` (+ `PlaceholderTest`) — intentional non-empty placeholder so `:domain:test` is meaningful from commit 0. Replaced by the real `Board`/`BoardSpec`/`Move`/`GameState`/`RulesEngine` in **Plan 02** (documented in the file).
- `app/.../App.kt`, `MainActivity.kt`, `ui/theme/Theme.kt` — minimal empty themed surface (no game UI). Real game route lands in Plans 04–05.

## Blockers (carried to STATE.md)
- **Hilt/KSP vs Kotlin 2.4.0 (resolve before Plan 03).** KSP has no Kotlin-2.4.0 build (latest 2.3.9). Plan 03 introduces `@HiltViewModel`, which needs the Hilt annotation processor on KSP. Before Plan 03 the team must either (a) wait for / pin a KSP 2.4.x once published, or (b) make a stack decision (e.g. align Kotlin to the 2.3.x line that KSP supports) — an architectural call that owns the CLAUDE.md Kotlin pin. Hilt + KSP coordinates are already declared in the catalog, ready to apply.

## User Setup Required
- `local.properties` (gitignored) is written with `sdk.dir=/Users/ermanergoz/Library/Android/sdk`. CI writes it from the runner SDK root. No secrets.
- **Branch protection (manual, GitHub):** mark the `CI / verify` job a required status check on `develop` (and `main`) so the gitflow gate is enforced on `feature/*→develop` PRs.

## Next Phase Readiness
- Scaffold + gates are green and unbypassable — Plan 02 (pure-Kotlin domain core + golden-master harness) can build directly on `:domain` with JUnit4 + Truth already wired.
- **Before Plan 03:** resolve the Hilt/KSP blocker above.

## Self-Check: PASSED

- All 10 key created files exist on disk (verified).
- All 3 task commits present in git history (`b7dc7ec`, `58b70af`, `0ce515f`).
- `./gradlew ktlintCheck detekt :domain:test test assembleDebug` exits 0; debug APK built.

---
*Phase: 01-domain-foundation-playable-board*
*Completed: 2026-06-28*
