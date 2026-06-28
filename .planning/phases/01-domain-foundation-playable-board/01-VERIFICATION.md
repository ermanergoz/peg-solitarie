---
phase: 01-domain-foundation-playable-board
verified: 2026-06-28T00:00:00Z
status: verified
score: 5/5 must-haves verified
overrides_applied: 0
re_verification: false
human_verification:
  - test: "Install the debug APK on a Pixel AVD and play the English-33 board end-to-end. Confirm: (1) tapping a peg highlights legal destinations with a primary-color dot; (2) re-tapping the peg deselects; (3) tapping a highlighted hole causes the peg to visibly SLIDE in a straight line (~200 ms) into the hole while the jumped peg FADES + SHRINKS and is gone as the jumper lands; (4) playing down to no remaining moves surfaces the correct game-over tier dialog (Perfect!/So close!/No moves left) with a 'Play Again' button; (5) mid-game top-bar Restart shows the 'Restart this board?' confirm and 'Restart Board' resets to the 32-peg center-empty board."
    expected: "All five behaviors work; the jump/removal feel is smooth and satisfying with no jank, incorrect timing, or missing visuals."
    why_human: "Animation quality — slide smoothness (~200 ms tween), fade+shrink removal timing (~180 ms), highlight clarity, and overall 'satisfying feel' — is perceptual and cannot be asserted by the Robolectric GameFlowTest, which only checks state changes and dialog text."
---

# Phase 1: Domain Foundation & Playable Board — Verification Report

**Phase Goal:** A single hardcoded peg solitaire board is fully playable end-to-end — tap a peg, tap a legal hole, the peg jumps with animation, the jumped peg is removed, and game-over is detected — built on a pure-Kotlin domain core (immutable model + RulesEngine) guarded by CI standards gates from the very first commit.
**Verified:** 2026-06-28
**Status:** VERIFIED — manual emulator playtest approved by user 2026-06-28 ("it runs fine, no issues"). All 9 requirements covered; standards gates real and passing; build runs on AGP 9.1.0 (tooling-supported).
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (from ROADMAP.md Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player can tap a peg and see its legal orthogonal destinations highlighted on the board | VERIFIED | `GameViewModel.select()` builds `legalTargets` from `RulesEngine.legalMoves`; `BoardCanvas` renders `DESTINATION_DOT_ALPHA` dots on `legalTargets`; `GameViewModelTest.selectingPegHighlightsItsLegalDestinations` + `GameFlowTest.tappingPegThenHolePerformsTheJumpAndRemovesAPeg` assert `LegalTargetCount = 1` after tap |
| 2 | Player can complete a legal jump — the peg slides into the empty hole with animation and the jumped peg is removed | VERIFIED (automated); PENDING (animation feel — human) | `GameViewModel.performJump` applies `RulesEngine.applyMove` then sets `MoveTransition`; `BoardCanvas` drives `Animatable<Offset>` (jump slide, 200 ms) + `Animatable<Float>` (fade+shrink, 180 ms) on `LaunchedEffect(transition?.id)`; `GameFlowTest` confirms pegCount drops 32→31 and jumper is removed; animation FEEL is human-only |
| 3 | The game detects and signals the end state (game-over) when no legal moves remain | VERIFIED | `RulesEngine.isGameOver = legalMoves().isEmpty()`; `GameViewModel.statusFor` returns `Won`/`Stuck`; `GameScreen.GameOverOverlay` shows `GameOverDialog` on terminal `status`; `GameFlowTest.reachingATerminalPositionShowsTheGameOverDialog` asserts `GameStatus.Won` + "Perfect!" text displayed |
| 4 | Player can restart the current board to its initial state at any time | VERIFIED | `GameViewModel.onRestart` raises `confirmRestart` mid-game; `onConfirmRestart` calls `resetToInitial`; `RestartControl` + `RestartConfirmDialog` in `GameScreen`; `GameViewModelTest.midGameRestartAsksForConfirmationThenResets` + `GameFlowTest.midGameRestartConfirmsThenResetsToTheInitialBoard` assert full flow |
| 5 | CI gates are green: domain has zero `android.*` imports (ArchUnit), `detekt` + `ktlint` pass as build gate, no magic numbers/hardcoded dp/sp, and a JVM unit-test harness (incl. golden-master) covers RulesEngine | VERIFIED | `DomainPurityTest` (ArchUnit bytecode gate, `@RunWith(ArchUnitRunner)`, wired via `check`); `standards.gradle.kts` wires `detekt` (ignoreFailures=false) + `ktlintCheck` + `dpSpLiteralCheck` into `check`; `dimens.xml` holds all fixed tokens; `BoardMetrics`/`GameAnim` are scoped constants; `GoldenMasterTest` (5 fixtures + harness); CI workflow runs full gate on `feature/*→develop` |

**Score:** 5/5 truths verified (one sub-criterion of Truth 2 — animation feel — requires human)

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/src/main/kotlin/.../model/Board.kt` | Immutable Long-bitmask board | VERIFIED | 51 lines; `data class` with `cellAt/withPeg/withoutPeg/pegCount` — no mutation |
| `domain/src/main/kotlin/.../model/Boards.kt` | English-33 spec + initial board | VERIFIED | English33Layout scoped constants; `Boards.english33Initial` folds pegs then clears center |
| `domain/src/main/kotlin/.../rules/RulesEngine.kt` | Stateless rules authority | VERIFIED | 89 lines; `legalMoves` deterministically sorted, `applyMove` returns new `Board`, `isGameOver` |
| `domain/src/test/.../rules/RulesEngineTest.kt` | Unit tests for RulesEngine | VERIFIED | 7 tests: initial 4 moves, sorted order, applyMove contract, no-diagonal, isGameOver |
| `domain/src/test/.../golden/GoldenMasterTest.kt` | Golden-master determinism harness | VERIFIED | 5 tests; 4 committed fixtures; `BoardCodec` + `GoldenMasterAssert` reusable for Phase 3 |
| `app/src/main/java/.../game/GameUiState.kt` | @Immutable UiState | VERIFIED | `@Immutable data class`, all `val`, `GameUiState.initial(board)` factory |
| `app/src/main/java/.../game/GameViewModel.kt` | Hilt ViewModel with move logic | VERIFIED | 167 lines; `@HiltViewModel`; re-derives `Move` from engine before applying; `StatusFlow`; no legality outside VM |
| `app/src/main/java/.../components/BoardCanvas.kt` | Stateless Compose Canvas leaf | VERIFIED | 232 lines; no `RulesEngine`/`legalMoves` imports; two `Animatable` instances (offset + float); `onCellTap` emitted only |
| `app/src/main/java/.../components/BoardGeometry.kt` | Hoisted pixel math + hit-test | VERIFIED | `positionAt` bounds-checks footprint + hit radius; allocated once via `forCanvas`, `remember`-ed |
| `app/src/main/java/.../game/GameScreen.kt` | Stateless Screen composable | VERIFIED | M3 `Scaffold` + `TopAppBar` + board panel + `GameOverDialog` + `RestartConfirmDialog`; semantics keys attached |
| `app/src/main/java/.../game/GameRoute.kt` | Stateful route seam | VERIFIED | `hiltViewModel()` + `collectAsStateWithLifecycle`; zero logic; delegates to `GameScreen` |
| `app/src/main/java/com/app/pegsolitaire/MainActivity.kt` | Host activity | VERIFIED | `@AndroidEntryPoint`; `setContent { AppTheme { GameRoute() } }` |
| `app/src/main/res/values/dimens.xml` | All fixed dp tokens | VERIFIED | Spacing scale, layout tokens, intentional sub-4dp stroke widths; no hardcoded dp elsewhere |
| `app/src/main/java/.../game/BoardMetrics.kt` | Unitless board geometry ratios | VERIFIED | All ratios named; no raw dp/sp; `GRID_DIMENSION`, `PEG_RADIUS_RATIO`, etc. |
| `app/src/main/java/.../game/GameAnim.kt` | Jump/removal durations | VERIFIED | `JUMP_SLIDE_MS = 200`, `REMOVAL_MS = 180`, `SELECT_FADE_MS = 120` + easings; named, not inline |
| `app/src/test/.../DomainPurityTest.kt` | ArchUnit boundary gate | VERIFIED | `@RunWith(ArchUnitRunner::class)`; `noClasses().in("..domain..").dependOnClassesThat().resideInAnyPackage("android..", "androidx..")` |
| `build-logic/src/main/kotlin/standards.gradle.kts` | detekt + ktlint + dp/sp gate | VERIFIED | All three wired via `tasks.named("check").configure { dependsOn("detekt", "ktlintCheck", dpSpLiteralCheck) }`; `ignoreFailures=false` |
| `config/detekt/detekt.yml` | detekt rules config | VERIFIED | `MagicNumber` active; scoped-constants exceptions; `@Composable` PascalCase exemption |
| `.github/workflows/ci.yml` | Gitflow-gated CI | VERIFIED | Triggers on `push`/`PR` targeting `develop`/`main`; runs `ktlintCheck detekt :domain:test test assembleDebug --no-daemon` on JDK 17 |
| `app/src/test/.../GameViewModelTest.kt` | Turbine ViewModel tests | VERIFIED | 12 tests covering selection, re-tap, reselect, jump, animationFinished, blocked no-op, forged-move no-op, Won, Stuck, confirm-restart, dismiss-restart, end-state restart |
| `app/src/test/.../GameFlowTest.kt` | Robolectric end-to-end Compose test | VERIFIED | 3 tests: tap→jump→removal (GAME-01), terminal→dialog (GAME-02), mid-game restart flow (GAME-05); drives real `GameScreen` |
| `app/src/test/.../BoardCanvasTest.kt` | JVM geometry / hit-test tests | VERIFIED | 6 tests: zero-size not-ready, cellSize, center→cell mapping, every in-play cell, blocked corner, outside-board dropped |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MainActivity` | `GameRoute` | `setContent { AppTheme { GameRoute() } }` | WIRED | Confirmed in `MainActivity.kt` line 21 |
| `GameRoute` | `GameViewModel` | `hiltViewModel()` | WIRED | `GameRoute.kt` line 22 |
| `GameRoute` | `GameScreen` | state + callback refs passed directly | WIRED | `GameRoute.kt` lines 24-32 |
| `GameScreen` | `BoardCanvas` | `BoardCanvas(state, onCellTap, onAnimationFinished, ...)` inside `Surface` | WIRED | `GameScreen.kt` line 120 |
| `BoardCanvas` tap | `GameViewModel.onCellTap` | `geometry.positionAt(offset)?.let(onCellTap)` → Route wires `onCellTap = viewModel::onCellTap` | WIRED | `BoardCanvas.kt` line 114; `GameRoute.kt` line 26 |
| `GameViewModel` | `RulesEngine` | constructor injection; `rulesEngine.legalMoves(board)` + `rulesEngine.applyMove` | WIRED | `GameViewModel.kt` lines 68, 96 |
| `DomainModule` | `GameViewModel` | `@HiltViewModel` + `@Inject constructor(rulesEngine: RulesEngine, initialBoard: Board)` | WIRED | `DomainModule.kt` provides both; `GameViewModel.kt` consumes both |
| `standards.gradle.kts` | `check` task | `tasks.named("check").configure { dependsOn(...) }` | WIRED | Confirmed in `standards.gradle.kts` line 75 |
| `DomainPurityTest` | `:app:testDebugUnitTest` → `check` | ArchUnit test discovered by JUnit runner under `check` | WIRED | `@RunWith(ArchUnitRunner::class)` present |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `GameScreen` / `BoardCanvas` | `state: GameUiState` | `GameViewModel._uiState: MutableStateFlow` | Yes — updated on every `onCellTap` / `applyMove` call from `RulesEngine` | FLOWING |
| `GameUiState.board` | board state | `RulesEngine.applyMove(board, move)` returning a new immutable `Board` | Yes | FLOWING |
| `GameUiState.legalTargets` | highlighted positions | `RulesEngine.legalMoves(board).filter { it.from == position }.map { it.to }` | Yes | FLOWING |
| `GameUiState.status` | Won/Stuck/Playing | `statusFor(nextBoard)` calling `RulesEngine.isGameOver` + `board.pegCount` | Yes | FLOWING |
| `BoardCanvas` animation | `jumpOffset` / `removalProgress` | `Animatable` driven by `LaunchedEffect(transition?.id)` on post-commit `MoveTransition` | Yes — values flow from real domain moves | FLOWING |

---

## Behavioral Spot-Checks

These are JVM-level checks already run by the executor (confirmed by SUMMARY verification evidence). The build gate and automated test suite serve as spot-checks.

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full gate: detekt + ktlint + domain tests + ArchUnit purity + dp/sp gate + debug build | `./gradlew ktlintCheck detekt :domain:test test assembleDebug` | exit 0 (reported in all 5 SUMMARY.md verification sections) | PASS |
| GameFlowTest (3 Robolectric Compose tests): tap→jump, terminal→dialog, restart flow | `./gradlew :app:testDebugUnitTest --tests "*GameFlowTest"` | 3/3 green, `failures="0"` (reported in 01-05-SUMMARY.md) | PASS |
| GameViewModelTest (12 Turbine tests) | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelTest"` | 12 tests green (01-03-SUMMARY.md) | PASS |
| GoldenMasterTest (5 snapshot tests) | `./gradlew :domain:test --tests "*GoldenMasterTest"` | 5/5 green (01-02-SUMMARY.md) | PASS |
| BoardCanvasTest (6 geometry / hit-test tests) | `./gradlew :app:testDebugUnitTest --tests "*BoardCanvasTest"` | 6/6 green (01-04-SUMMARY.md) | PASS |
| No raw dp/sp literals in ui/ | dpSpLiteralCheck (custom Gradle task) | exit 0 confirmed in 01-04-SUMMARY.md and 01-05-SUMMARY.md | PASS |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| GAME-01 | 01-02, 01-05 | Player can jump a peg orthogonally over an adjacent peg into an empty hole, removing the jumped peg | SATISFIED | `RulesEngine.applyMove` removes `from`+`over`, sets `to`; `GameFlowTest` asserts pegCount 32→31 |
| GAME-02 | 01-02, 01-03 | Game detects and signals the end state when no legal moves remain | SATISFIED | `RulesEngine.isGameOver()`; `GameViewModel.statusFor()`; `GameViewModelTest` covers Won + Stuck; `GameFlowTest` asserts dialog appears |
| GAME-03 | 01-04, 01-05 | Board renders on Compose Canvas with animated jump and peg-removal feedback | SATISFIED (automated) + PENDING (feel) | `BoardCanvas` with two `Animatable` instances; `GameFlowTest` asserts state; animation FEEL needs human |
| GAME-04 | 01-03, 01-04 | Player can select a peg and see its legal destinations highlighted | SATISFIED | `GameViewModel.select()` builds `legalTargets`; `BoardCanvas` draws `DESTINATION_DOT`; `GameViewModelTest` asserts highlights |
| GAME-05 | 01-03, 01-05 | Player can restart the current level at any time | SATISFIED | `GameViewModel.onRestart/onConfirmRestart/onDismissRestart`; `RestartControl` + `RestartConfirmDialog`; `GameFlowTest` covers full restart flow |
| QUAL-01 | 01-01 | Domain layer is pure Kotlin with no Android dependencies, enforced by a CI boundary test | SATISFIED | `:domain` has no Android plugin; `DomainPurityTest` (ArchUnit, bytecode) wired under `check`; zero `android.*`/`androidx.*` imports in domain source confirmed by grep |
| QUAL-02 | 01-01 | detekt and ktlint configured and pass as a build gate | SATISFIED | `standards.gradle.kts`: `ignoreFailures=false`, both wired into `check`; CI runs them on every push/PR |
| QUAL-03 | 01-01, 01-04 | No magic numbers/strings in logic/UI; all dp/sp in dimens.xml | SATISFIED | `dpSpLiteralCheck` passes (zero hits in ui/); `dimens.xml` has all fixed tokens; `BoardMetrics`/`GameAnim` are scoped constants; `MagicNumber` detekt rule active |
| QUAL-04 | 01-02 | Domain game logic covered by JVM unit tests including golden-master determinism test | SATISFIED | `RulesEngineTest` (7), `BoardTest`, `GoldenMasterTest` (5 + 4 committed fixtures); `BoardCodec`+`GoldenMasterAssert` harness reusable for Phase 3 |

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | — | — | None found |

**Debt marker scan:** Zero `TBD`, `FIXME`, or `XXX` markers in any source file under `domain/src` or `app/src`. The `return null` at `BoardGeometry.kt:62` is a legitimate guard clause (not a stub): it returns null when the canvas has not been measured to a usable size, which is the correct typed behavior for `positionAt(offset): Position?`.

**Stub scan:** No placeholder returns (`return null`, `return []`, `return {}`), no console-log-only handlers, no `TODO()` throws in production source. The `known stubs` noted in 01-01-SUMMARY.md (`Placeholder.kt`) were removed in Plan 02 as confirmed by the SUMMARY and by the source tree (no `Placeholder.kt` exists).

**QUAL-03 gate verification:** grep for `\d+\.dp` and `\d+\.sp` in `app/src/main/java` returns zero matches. Every dp value flows through `dimensionResource(R.dimen.*)` in composables or through `BoardMetrics`/`GameAnim` unitless ratios for proportional geometry.

**Domain purity verification:** grep for `android.` / `androidx.` import lines in `domain/src/main/kotlin` returns zero matches.

**Legality separation verification:** grep for `RulesEngine|legalMoves|applyMove|isGameOver` in `BoardCanvas.kt` and `BoardGeometry.kt` returns zero matches — no legality logic in the Canvas layer.

---

## Architectural Notes

**Kotlin version deviation (pre-approved):** CLAUDE.md specifies Kotlin 2.4.0 but the build uses 2.3.20. This is a documented, user-approved deviation from 01-01-SUMMARY.md (RESOLVED section): KSP has no published build for Kotlin 2.4.x; 2.3.20 + KSP 2.3.9 is the only pairing that satisfies the no-KAPT constraint. The deviation is recorded in `gradle/libs.versions.toml` with a comment. Hilt 2.60 (vs. planned 2.57.1) was similarly bumped for AGP 9.2.1 compatibility. Neither change weakens any functional or standards requirement.

**Robolectric SDK pin:** `@Config(sdk = 34)` is used in `GameFlowTest` because no Robolectric android-all jar exists for Android 16 (SDK 36) yet. The app's `compileSdk = 36` is unaffected; this is a test-runner constraint, not a functional limitation.

**`hiltViewModel()` deprecation:** The `hiltViewModel()` import from `androidx.hilt.navigation.compose` is flagged as deprecated (moved to `androidx.hilt.lifecycle.viewmodel.compose`), but that package is in the compileSdk-37 artifact, which cannot be installed in this environment. The deprecated overload is the compileSdk-36-safe path and is functionally identical. Not a blocker; Phase 2 can upgrade when compileSdk 37 becomes available.

---

## Human Verification Required

### 1. Emulator Playtest — Jump/Removal Animation Feel (GAME-03)

**Test:** With a Pixel AVD running, execute `./gradlew :app:installDebug` then launch "Peg Solitaire" (or `adb shell am start -n com.app.pegsolitaire/.MainActivity`). Play the following sequence:
1. Tap a peg near the center (e.g., the peg above the center hole at row 1, col 3). Confirm its legal destination(s) show as a highlighted dot (primary color, ~38% alpha).
2. Re-tap the selected peg — confirm it deselects. Tap a different peg — confirm reselection.
3. Tap a highlighted destination. Confirm the peg **slides straight** into the hole in approximately 200 ms (FastOutSlowIn) while the jumped peg **fades and shrinks** simultaneously and is gone as the jumper lands.
4. Play until no moves remain. Confirm the correct game-over tier appears: "Perfect!" (1 peg left), "So close!" (2–3 pegs), or "No moves left" (4+ pegs). Confirm "Play Again" button is present.
5. Mid-game, tap the Restart (refresh) icon in the top bar. Confirm the "Restart this board?" dialog appears. Confirm "Keep Playing" cancels and "Restart Board" resets to 32 pegs.

**Expected:** All five behaviors work as described. The jump slide feels smooth (no jank or skip). The removal fade+shrink completes as the jumper lands. Highlights are clearly visible. Dialog copy matches the spec.

**Why human:** Animation quality — slide smoothness, fade+shrink timing, highlight clarity, and overall perceived "feel" — is perceptual. The automated `GameFlowTest` asserts state changes and text presence via Robolectric but cannot judge motion quality, timing feel, or visual clarity. This is VALIDATION task 01-05-03 (`checkpoint:human-verify`, gate="blocking"), explicitly deferred from automation.

**Resume signal:** Reply "approved" if the board plays correctly and the jump/removal feel is satisfying, or describe what feels off (timing, visual, copy mismatch).

---

## Gaps Summary

No gaps identified. All 5 ROADMAP success criteria have direct code evidence. All 9 assigned requirements (GAME-01 through GAME-05, QUAL-01 through QUAL-04) are satisfied by substantive, wired, data-flowing artifacts and confirmed by automated tests. No debt markers, no stubs, no orphaned artifacts.

The single outstanding item — emulator playtest for animation feel — is an inherently human-only check (perceptual judgment of motion quality), not a code gap. It was planned from the start as the only manual verification step for this phase.

---

_Verified: 2026-06-28_
_Verifier: Claude (gsd-verifier)_
