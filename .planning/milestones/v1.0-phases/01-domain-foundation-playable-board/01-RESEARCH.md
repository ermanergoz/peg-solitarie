# Phase 1: Domain Foundation & Playable Board - Research

**Researched:** 2026-06-28
**Domain:** Greenfield native Android (Kotlin 2.4.0 / Compose) — pure-Kotlin game domain core + first playable Compose Canvas board + CI standards gates
**Confidence:** HIGH (stack/architecture inherited from verified project research; the Phase-1 deltas are tooling-version compatibility, which was verified live against official sources this session)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**First Playable Board**
- **D-01:** The single hardcoded Phase-1 board is **English-33** — iconic cross, 33 holes, single center-empty start. Also one of the four Phase-2 Classic boards (layout/fixture work carries forward).

**Move Interaction Feel**
- **D-02:** **Tap-only input** — tap a peg to select (legal destinations highlight), tap a highlighted hole to jump. **No drag** in Phase 1 (drag is a Slice-2 polish item and must not require domain changes).
- **D-03:** **Always two-step.** Even with exactly one legal destination, the player must still tap the destination — no auto-jump. Behavior identical in every case.
- **D-04:** **Deselect by re-tapping the selected peg.** Tapping the selected peg again clears selection. Tapping a different player peg re-selects it. Tapping an empty/non-legal cell is a no-op (does NOT deselect).

**End-State Signal & Restart**
- **D-05:** On game-over (no legal moves), show a **simple modal dialog/overlay** displaying pegs remaining + a win/perfect message, with a Restart action. Minimal "signal the end state" for Phase 1 — the natural seam the Phase-2 Results screen later replaces.
- **D-06:** **Persistent Restart control** (top-bar/icon) available at any time. If a game is in progress (moves made, not ended), Restart shows a quick confirmation. At end-state, Restart in the dialog needs no confirmation.

**Jump / Removal Animation (baseline feel — full polish deferred to Slice 2)**
- **D-07:** Jumping peg moves with a **straight-line slide** from start cell to destination hole (single `Animatable` `Offset`). Arc/hop is a Slice-2 candidate.
- **D-08:** Jumped-over peg removed via **fade + slight shrink** (opacity → 0 while scaling down), timed to finish as the jumper lands.

### Claude's Discretion (resolved in this research — see Architecture Patterns)
- Gradle module topology / domain-purity enforcement (separate `:domain` JVM module vs single app module + Konsist/ArchUnit boundary test). *Note: success criterion #5 names "Konsist/ArchUnit boundary test."*
- Internal board data representation and the `Position`/coordinate model.
- Exact scope of the golden-master determinism scaffold for Phase 1.
- Visual styling of "legal destination" highlighting.

### Deferred Ideas (OUT OF SCOPE — do not plan)
- **Drag-to-jump input** — Slice-2 polish (must not require domain changes).
- **Arc/hop jump motion and pop removal** — Slice-2 visual-polish pass (POLI-01).
- **Auto-jump on single legal move** — considered and explicitly rejected (see D-03).
- Everything in `## Out of scope (later phases)`: Results/scoring/stars, navigation + other 3 classic boards, LevelRepository seam (Phase 2), Challenge generation/Solver (Phase 3), hint/undo (Phase 4), persistence/resume/settings (Phase 5).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| GAME-01 | Player can jump a peg orthogonally over an adjacent peg into an empty hole, removing the jumped peg | `RulesEngine.legalMoves/applyMove` (pure Kotlin); board representation §Board Model; tap-to-jump §Canvas Pattern |
| GAME-02 | Game detects and signals the end state when no legal moves remain | `RulesEngine.isGameOver` (= `legalMoves().isEmpty()`); D-05 modal driven from `GameUiState.status` (state, not one-shot effect) |
| GAME-03 | Board renders on a Compose Canvas with animated jump and peg-removal feedback | §Canvas rendering + hit-test + `Animatable` (D-07 slide / D-08 fade+shrink) |
| GAME-04 | Player can select a peg and see its legal destinations highlighted | `GameUiState.selected` + `legalTargets`; VM selection logic (D-02/03/04) |
| GAME-05 | Player can restart the current level at any time | Restart resets to the hardcoded English-33 initial `Board` (D-06); confirmation when mid-game |
| QUAL-01 | Domain layer is pure Kotlin, enforced by a CI boundary test | §Q1 — `:domain` pure-JVM module (structural) + named ArchUnit/Konsist boundary test |
| QUAL-02 | detekt and ktlint pass as a build gate | §Q4 — `dev.detekt` 2.0.0-alpha.5 + ktlint-gradle 14.2.0 wired into `check`, `ignoreFailures=false` |
| QUAL-03 | No magic numbers/strings; UI dp/sp in `dimens.xml` | detekt `MagicNumber` rule + scoped constants objects; custom Konsist/grep gate for `.dp`/`.sp` literals |
| QUAL-04 | Domain game logic covered by JVM unit tests, including golden-master determinism scaffold | §Q3 — JUnit4 + Truth harness + `BoardCodec` + `GoldenMasterAssert` (reused verbatim by Phase 3) |
</phase_requirements>

## Summary

Phase 1 is a **walking skeleton**: stand up the whole Gradle/Compose scaffold, the pure-Kotlin domain core (`Board`/`Cell`/`Position`/`Move`/`GameState` + `RulesEngine`), and one fully playable hardcoded English-33 board — all guarded by CI standards gates that are green *from the first commit*. The project-level research (`.planning/research/STACK.md`, `ARCHITECTURE.md`, `PITFALLS.md`, `SUMMARY.md`) already fixes the stack, the Clean-Architecture/MVVM/UDF shape, the immutable-model discipline, and the gameplay data-flow. This document does **not** re-derive those; it resolves the four "Claude's Discretion" items and surfaces the Phase-1-specific **tooling-version deltas** that are genuinely new information.

The single most important finding: **the standards toolchain must be bleeding-edge to match the pinned Kotlin 2.4.0.** Stable detekt (1.23.8) is built against Kotlin 2.0.21 and *cannot read Kotlin 2.3+ metadata* — it will break the build. The only detekt that supports Kotlin 2.4.0 is **`dev.detekt` 2.0.0-alpha.5** (plugin id changed from `io.gitlab.arturbosch.detekt`). ktlint via **ktlint-gradle 14.2.0** is Kotlin-2.4-ready and AGP-9-new-DSL aware. Konsist 0.17.3 parses source with an embedded *older* Kotlin compiler, which is a parse-risk on a Kotlin-2.4 codebase — so for the domain-purity gate, prefer a **`:domain` pure-JVM module (structural guarantee) + an ArchUnit bytecode-based boundary test** (Kotlin-version-agnostic), which is also exactly the "strongest guard" the pitfalls research recommends.

**Primary recommendation:** Scaffold gates *before* feature code (so the first feature commit is already green); split a pure-JVM `:domain` module for a structural android-free guarantee plus a named boundary test; represent the board as a `Long` peg-bitmask encapsulated behind `Board` accessors (clean equals/hashCode, deterministic serialization, forward-compatible with the Phase-3 Solver/generator); and build a reusable golden-master harness (`BoardCodec` + `GoldenMasterAssert`) over `RulesEngine` now so Phase 3 only feeds it generator output.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Legal-move enumeration / apply-move / game-over | Domain (pure Kotlin) | — | `RulesEngine` is stateless pure logic; the project's highest-value JVM test target and dependency root |
| Board state (occupancy, immutability) | Domain | — | `Board`/`GameState` immutable `data class`; SSOT that `StateFlow` emits |
| Tap → Position hit-testing | UI (Compose) | Domain (validates) | Pixel→grid math is a UI concern; legality is re-decided in the VM/domain, never trusted from the tap |
| Selection / legal-target highlighting state | UI (ViewModel `UiState`) | Domain (`legalMoves`) | State derived from domain; rendered by stateless Canvas |
| Jump / removal animation | UI (Compose `Animatable`) | — | Pure render of a declarative `MoveTransition`; **no game logic in Canvas** |
| Game-over signal (D-05 dialog) | UI (state-driven) | Domain (`isGameOver`) | `status` field in `UiState`; dialog is replayable state, not a one-shot effect |
| Restart (D-06) | UI (ViewModel) | Domain (initial board) | VM resets `GameState` to the hardcoded English-33 initial board |
| DI wiring (interface→impl) | DI (Hilt) | — | `GameViewModel` via `hiltViewModel()`; no platform SDKs in Slice 1 |
| Standards enforcement (purity, lint, format, magic numbers) | Build/CI (Gradle + convention plugin) | — | detekt/ktlint/ArchUnit run in `check`; module boundary is structural |

## Standard Stack

> The full app stack is **already fixed and verified** in `CLAUDE.md` + `.planning/research/STACK.md` (Kotlin 2.4.0, AGP 9.2.0, Gradle 9.1+, JDK 17, Compose BOM 2026.06.00, Hilt 2.57.1, KSP2, Material 3, JUnit4/Truth/Turbine/MockK/Roborazzi). **Do not re-pin those.** The table below is the **Phase-1 delta**: the standards-gate tooling not yet pinned in CLAUDE.md, with versions verified live this session.

### Core (Phase-1 standards-gate tooling — newly verified)

| Library / Plugin | Version | Plugin / Coordinate | Purpose | Why Standard |
|------------------|---------|---------------------|---------|--------------|
| detekt | **2.0.0-alpha.5** | `dev.detekt` (Gradle plugin) | Static analysis incl. `MagicNumber` (QUAL-02, QUAL-03) | **Only** detekt supporting Kotlin 2.4.0; stable 1.23.8 can't read Kotlin 2.3+ metadata `[CITED: detekt.dev/docs/introduction/compatibility]` |
| ktlint-gradle | **14.2.0** | `org.jlleitschuh.gradle.ktlint` | Format/lint gate (QUAL-02) | Kotlin 2.4.0 compatible, Isolated-Projects + AGP-9 new-DSL aware `[CITED: plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint]` |
| ArchUnit (JUnit4) | **1.3.x** (verify at setup) | `com.tngtech.archunit:archunit-junit4` | Named domain-purity boundary test on bytecode (QUAL-01) | Bytecode-based → **immune to Kotlin 2.4 source-parse risk**; satisfies "ArchUnit boundary test" in success criterion #5 `[ASSUMED]` |
| Konsist | **0.17.3** | `com.lemonappdev:konsist` | *Alternative* purity/convention test (Kotlin-native DSL) | Latest release; embeds Kotlin 2.0.20 compiler — parses source, minor Kotlin-2.4 parse risk `[CITED: github.com/LemonAppDev/konsist/releases]` |

### Supporting (inherited — confirm present in catalog for Phase 1)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| kotlinx.collections.immutable | 0.4.0 (verify) | `PersistentList`/`PersistentMap` + `@Immutable`-friendly collections | Only if board uses Map representation; **not needed** if using the recommended `Long` bitmask |
| Truth | 1.4.x | Golden-master + RulesEngine assertions | All domain JVM tests (QUAL-04) |
| Turbine | 1.2.1 | `GameUiState` emission assertions | `GameViewModel` tests |
| Roborazzi | 1.x | Optional Canvas snapshot smoke | Lock the board visual once it renders (optional in Phase 1) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `dev.detekt` 2.0.0-alpha.5 | detekt 1.23.8 stable | **Will not work** — can't read Kotlin 2.4 metadata; build breaks. Alpha is the only viable path given the pinned toolchain |
| ArchUnit boundary test | Konsist boundary test | Konsist is Kotlin-native and reads nicer, but parses source with an embedded 2.0.20 compiler (parse risk on Kotlin 2.4). ArchUnit reads bytecode → version-agnostic. Use Konsist if 2-module split makes the test purely belt-and-suspenders |
| `:domain` separate JVM module | Single app module + boundary test only | Single-module is the ARCHITECTURE.md default ("modularize only if build times demand it"), but the module split is the *strongest* purity guard (Pitfall 9) and domain purity is a hard gate (QUAL-01) + the project's core-risk root |
| `Long` bitmask board | `PersistentMap<Position,Cell>` | Map is more readable for newcomers; bitmask gives free perfect equals/hashCode, trivial deterministic serialization, and is the representation the Phase-3 Solver wants. Encapsulate behind `Board` accessors so the choice is swappable |

**Installation (version catalog additions — `gradle/libs.versions.toml`):**
```toml
[versions]
detekt   = "2.0.0-alpha.5"   # dev.detekt — ONLY line supporting Kotlin 2.4; verify newest alpha at setup
ktlint   = "14.2.0"          # org.jlleitschuh.gradle.ktlint
archunit = "1.3.0"           # verify latest at setup
konsist  = "0.17.3"          # only if choosing Konsist over ArchUnit
immutableCollections = "0.4.0" # only if Map-based board

[libraries]
archunit-junit4 = { group = "com.tngtech.archunit", name = "archunit-junit4", version.ref = "archunit" }
konsist         = { group = "com.lemonappdev",      name = "konsist",         version.ref = "konsist" }

[plugins]
detekt = { id = "dev.detekt", version.ref = "detekt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

**Version verification at setup (run before locking the catalog):**
```bash
# detekt — confirm newest 2.0.0-alpha.N and that it lists Kotlin 2.4.0
#   https://detekt.dev/docs/introduction/compatibility/
# ktlint-gradle — confirm 14.2.0+ default ktlint engine version
#   ./gradlew dependencies | grep ktlint   (after applying the plugin)
# ArchUnit — mvnrepository.com/artifact/com.tngtech.archunit/archunit-junit4
# Konsist — github.com/LemonAppDev/konsist/releases  (check compatibility page for Kotlin 2.4 note)
```

## Package Legitimacy Audit

> slopcheck was **not run** this session (tool not installed in environment). Per the legitimacy protocol's graceful-degradation rule, packages are tagged by their verification source. All four were confirmed against **authoritative** sources (official Gradle Plugin Portal pages / official GitHub release pages) this session, not merely registry existence. The planner should still gate the **alpha** detekt behind awareness (it is pre-release software, intentionally chosen).

| Package | Registry | Age | Source Repo | Verified via | Disposition |
|---------|----------|-----|-------------|--------------|-------------|
| `dev.detekt` 2.0.0-alpha.5 | Gradle Plugin Portal | alpha, 2026-06-17 | github.com/detekt/detekt | plugins.gradle.org/plugin/dev.detekt (official) | **Approved — flag as alpha** |
| `org.jlleitschuh.gradle.ktlint` 14.2.0 | Gradle Plugin Portal | 2026-03-12 | github.com/JLLeitschuh/ktlint-gradle | plugins.gradle.org (official) | Approved |
| `com.tngtech.archunit:archunit-junit4` 1.3.x | Maven Central | mature (8+ yrs) | github.com/TNG/ArchUnit | well-established OSS | Approved (verify exact version) |
| `com.lemonappdev:konsist` 0.17.3 | Maven Central | active | github.com/LemonAppDev/konsist | github releases (official) | Approved (only if chosen over ArchUnit) |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none — but `dev.detekt` 2.0.0-alpha.5 is **pre-release by necessity** (only Kotlin-2.4-compatible line). Pin the exact alpha, re-verify a newer alpha/RC/stable at planning time, and treat a detekt upgrade as a deliberate event.

## Architecture Patterns

> The macro-architecture (UI→Domain→Data, immutable `UiState` over `StateFlow`, `Channel<Effect>`, Canvas-as-pure-render, no logic in composables) is fully specified in `.planning/research/ARCHITECTURE.md` Patterns 1 & 5 and Anti-Patterns 1/5/6. Below: the four discretion items resolved into concrete, implementation-ready recommendations, plus the walking-skeleton sequence.

### System Architecture Diagram (Phase 1 slice)

```
                 ┌──────────────────────────── UI LAYER (Compose, MVVM) ────────────────────────────┐
  tap (Offset)   │  GameRoute ──collectAsStateWithLifecycle()──► GameUiState                         │
  ───────────────┼─►  GameScreen (stateless)                                                          │
                 │       ├─ BoardCanvas(state, onCellTap)  ── DrawScope: holes/pegs/highlights        │
                 │       │     pointerInput: Offset → Position (grid math)                             │
                 │       │     LaunchedEffect(transition.id): Animatable<Offset> slide (D-07)          │
                 │       │                                   Animatable<Float> alpha+scale (D-08)      │
                 │       ├─ RestartControl (D-06, confirm if mid-game)                                 │
                 │       └─ GameOverDialog (D-05) shown when status ∈ {Won, Stuck}                     │
                 │            ▲ state down                          │ onCellTap / onRestart up         │
                 │  ┌─────────┴──────────────── GameViewModel ──────▼──────────────────────────────┐  │
                 │  │ MutableStateFlow<GameUiState> (private) → val uiState (read-only)             │  │
                 │  │ onCellTap(pos): select / deselect (D-04) / two-step jump (D-03)               │  │
                 │  │ onRestart(): reset to initial board                                           │  │
                 │  └──────────────────────────────┬───────────────────────────────────────────────┘  │
                 └─────────────────────────────────┼──────────────────────────────────────────────────┘
                                                   │ pure calls (no Android types cross here)
                 ┌─────────────────────────────────▼──────── DOMAIN (:domain pure-JVM module) ─────────┐
                 │  RulesEngine.legalMoves(board) / applyMove(board, move) / isGameOver / pegCount      │
                 │  Board (Long peg-bitmask) · BoardSpec (English-33, Position↔bitIndex) · Move · Cell  │
                 │  GameState(board, moveHistory, status)  — all immutable data class                   │
                 │  ── zero android.* on the classpath (structural) ──                                  │
                 └─────────────────────────────────────────────────────────────────────────────────────┘
```

### Q1 — Gradle module topology + domain-purity enforcement  *(RECOMMENDATION)*

**Recommendation: two-module split — `:domain` (pure-JVM) + `:app` (Android) — with a named ArchUnit boundary test as belt-and-suspenders.**

- `:domain` applies **only** `org.jetbrains.kotlin.jvm` (a plain Kotlin/JVM library, no Android plugin). `android.*` is **not on its compile classpath**, so an Android import *cannot compile*. This is a structural guarantee, strictly stronger than any test (a test can be skipped/disabled; a missing classpath cannot be bypassed). Pitfall 9 explicitly calls the module boundary "the strongest guard."
- `:app` applies `com.android.application` + Compose/Hilt and `implementation(project(":domain"))`. Dependency direction is enforced by Gradle.
- **Named boundary test** (satisfies the literal wording of success criterion #5): add an **ArchUnit** test (in `:domain/test` or `:app`) asserting `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAPackage("android..")`. Prefer **ArchUnit over Konsist** here because ArchUnit reads **bytecode** and is therefore immune to the Kotlin-2.4 source-parse risk that affects Konsist's embedded 2.0.20 compiler.
- Shared build config (jvmToolchain(17), detekt, ktlint, ArchUnit deps) goes in a **`build-logic/` convention plugin** applied by both modules, so the gate is uniform and there's no copy-paste.

**Why this over single-module (the ARCHITECTURE.md default):** ARCHITECTURE Anti-Pattern 6 warns against *elaborate multi-module graphs* before the game is playable — that warning is about over-modularization, not the single highest-value split. Domain purity is a **hard success criterion (QUAL-01)** over the **project's #1-risk core**, and the spec/CLAUDE.md repeatedly frame the pure-Kotlin domain as the foundational requirement. Exactly one split (`:domain` + `:app`) is the minimal structure that makes the gate unbypassable. **Honest caveat:** single-module + ArchUnit/Konsist test is fully acceptable per the criterion wording and is the lower-ceremony MVP path; if the planner prefers minimal scaffold, that is defensible — but then the boundary test is the *only* guard, so it must run in CI from commit 0 and never be `@Disabled`.

### Q2 — Board data representation + coordinate model  *(RECOMMENDATION)*

**Recommendation: encapsulate occupancy behind `Board` accessors; back it with a `Long` peg-bitmask over a static `BoardSpec`.**

- `Position(val row: Int, val col: Int)` — immutable `value class`-friendly `data class`; the public coordinate type the UI and tests speak.
- `BoardSpec` — immutable static footprint: the set of in-play `Position`s (English-33 cross) + optional target hole, assigning each in-play position a **stable bit index 0..N-1** (sorted by `(row, col)` for determinism). English-33 = 33 holes → fits one `Long` (also covers European-37, Diamond-41, Triangle-15 → all ≤ 41 ≤ 64 bits, so the same scheme carries to Phase 2). The spec is created once and shared.
- `Board(val spec: BoardSpec, val pegs: Long)` — `pegs` bit set ⇒ peg present; bit clear & in-spec ⇒ empty; not in spec ⇒ `Blocked`. `Cell` is **derived** via `board.cellAt(pos)`, never stored redundantly.
- `Move(val from: Position, val over: Position, val to: Position)` — colinear, spacing 2.
- `GameState(val board: Board, val moveHistory: List<Move>, val status: GameStatus)` — immutable; every move produces a new instance via `copy()` (Pitfall 6).

**Why bitmask:** satisfies all four stated optimization goals at once — (1) *immutability*: a `Long` copy is free; (2) *fast legal-move enumeration*: bit ops; (3) *clean equals/hashCode for snapshotting*: a `data class` over a `Long` has perfect, collision-free equality for golden-master diffing; (4) *forward-compat*: the bitboard is exactly what the Phase-3 `Solver` transposition table + pagoda pruning and the generator's reverse-move enumeration want — building it now avoids a Phase-3 representation rewrite. **Encapsulation insurance:** `RulesEngine` and the UI only ever touch `cellAt`/`withPeg`/`withoutPeg`/`legalMoves` — so if the team prefers a `PersistentMap<Position,Cell>` for Phase-1 readability, it can be swapped without touching `RulesEngine`. Either way: **enumerate candidate moves into a deterministically sorted `List` (by `(row, col, direction)`) before returning them** — Phase 1 has no PRNG, but establishing the sorted-enumeration discipline now is the cheapest possible de-risking of the Phase-3 determinism contract (Pitfall 1).

### Q3 — Golden-master / determinism scaffold scope for Phase 1  *(RECOMMENDATION)*

**Recommendation: build the reusable harness now, exercised on `RulesEngine` over English-33; Phase 3 feeds it generator output unchanged.**

- **`BoardCodec`** — a deterministic, stable, human-diffable encoding of `Board`/`Move`. Recommend a **row-major ASCII grid** (`O`=peg, `.`=empty, ` `=blocked) for readable CI diffs **plus** the `pegs` `Long` as hex for exactness. This codec is the load-bearing artifact reused verbatim in Phase 3 (and powers the Phase-5 resume serialization conceptually).
- **`GoldenMasterAssert`** — reads a committed fixture resource, compares the encoded actual against it, and on mismatch writes a `<name>.actual` file + fails with a readable diff. An opt-in env flag (`UPDATE_GOLDEN=true`) regenerates fixtures deliberately (never auto-overwrite).
- **Phase-1 snapshot targets** (committed fixtures under `:domain/src/test/resources/golden/`):
  1. The canonical English-33 initial board (center empty).
  2. The full **sorted** legal-move list from the initial position (4 moves by symmetry) — proves enumeration order is stable.
  3. A fixed scripted move sequence (a known English-33 solution prefix) and the board after each step — proves `applyMove` determinism.
  4. The terminal board of a played-out line + `isGameOver == true` + `pegCount` — proves game-over detection.
- **Harness:** JUnit4 + Truth, in `:domain` (pure JVM, no Robolectric). This is the "golden-master determinism scaffold" named in success criterion #5 and QUAL-04 — in Phase 1 it has no generator to snapshot, so it locks the *encoding + assertion contract* the Phase-3 generator (`levels 1, 2, 10, 100, 1000`) will plug into.

### Q4 — detekt + ktlint as a build gate  *(RECOMMENDATION)*

**Recommendation:** wire both into the `check` lifecycle via the convention plugin so the build **fails** on violation, from commit 0.

- **detekt:** apply `dev.detekt` **2.0.0-alpha.5** (id changed from `io.gitlab.arturbosch.detekt`). The `detekt` task already fails on findings (`ignoreFailures = false` is the default — keep it). Provide `config/detekt/detekt.yml`; **enable `MagicNumber`** (style ruleset) — it operates on PSI literals and **does not require type resolution**, so it runs without a compiled classpath and is unaffected by metadata concerns. Configure `MagicNumber` strictly to honor CLAUDE.md ("no magic numbers in logic/UI"): keep `ignoreNumbers` minimal, set `ignorePropertyDeclaration`/`ignoreCompanionObjectPropertyDeclaration`/`ignoreNamedArgument` to permit the *scoped constants objects* the project mandates, and exclude `**/test/**` so fixtures/tests aren't penalized.
- **ktlint:** apply `org.jlleitschuh.gradle.ktlint` **14.2.0** (bundles a ktlint 1.x engine; override `ktlint { version.set(...) }` only if needed). `ktlintCheck` fails on violations by default. Add both `detekt` and `ktlintCheck` as dependencies of `check` (or run them explicitly in CI).
- **"No hardcoded dp/sp" (QUAL-03) is NOT built into detekt or ktlint.** Recommend a **custom gate**: simplest robust option is a small **Konsist or ArchUnit-style test / Gradle verification task** that scans `ui/` Kotlin sources for numeric `.dp`/`.sp` literals and fails unless they reference `dimensionResource(...)` / a `Dimens` accessor. (A custom detekt `Rule` via `RuleSetProvider` is the heavier alternative.) Flag this explicitly so the planner schedules it — it is custom work, not a config toggle.
- **CI from first commit:** a GitHub Actions workflow (`.github/workflows/ci.yml` — the repo already has `.github/`) running `./gradlew ktlintCheck detekt :domain:test test assembleDebug` on push + PR. Gitflow: require the check on `feature/* → develop` PRs. JDK 17 via `actions/setup-java` (Temurin 17). Export `ANDROID_HOME` / provide `local.properties` for the Android build step.

### Q5 — Compose Canvas board: render + tap hit-test + jump/removal animation  *(RECOMMENDATION)*

> Compose *how* is governed by `~/.claude/skills/compose-skill` (state hoisting, Route/Screen/Leaf boundary, `collectAsStateWithLifecycle`, effects-via-Channel). The skill's `animations-advanced.md` (Canvas + `Animatable`) and `compose-essentials.md` (three phases / draw-phase reads) are the relevant references.

- **Geometry (hoisted once):** `remember(boardSpec, canvasSize)` computes `cellSize = min(width,height) / gridDimension`, per-position center `Offset`s, peg radius, and reusable `Path`/`Brush` objects. **Never allocate inside `DrawScope`** (Pitfall 8).
- **Hit-test (tap → Position):** `Modifier.pointerInput(boardSpec) { detectTapGestures { offset -> val col = (offset.x / cellSize).toInt(); val row = (offset.y / cellSize).toInt(); val pos = Position(row, col); if (pos in boardSpec && hitWithinPegRadius(offset, pos)) onCellTap(pos) } }`. The Canvas emits `onCellTap(Position)` **up**; it computes no legality.
- **Selection logic (D-02/03/04, in the ViewModel):** `onCellTap(pos)`:
  - `pos` holds the player's peg → if `pos == selected` clear selection (D-04 deselect); else select `pos` and set `legalTargets = RulesEngine.legalMoves(board).filter { it.from == pos }.map { it.to }`.
  - `pos ∈ legalTargets` → perform the jump (**two-step always**, D-03): build the `Move`, `applyMove`, push to history, set `animating = MoveTransition(...)`, clear selection; then check `isGameOver`.
  - empty / non-legal cell with a selection active → **no-op** (D-04: does NOT deselect).
- **Animation (D-07 slide / D-08 fade+shrink):** the VM commits the move to domain state *immediately* and sets `animating = MoveTransition(from, to, removed = over, id = moveHistory.size)` (animate-from-already-committed-state, per ARCHITECTURE Pattern 5 — avoids snapshot drift). In the composable:
  - `LaunchedEffect(transition.id)` launches an `Animatable(fromCenter)` animating to `toCenter` (straight line, `tween`) for the jumper, and an `Animatable(1f)` driving the jumped peg's `alpha`→0 + `scale`→0, **timed to finish as the jumper lands** (same/closely-coupled duration). Read the animated values inside `Canvas` via lambda so only the **draw phase** re-runs (Pitfall 7/8).
  - On completion call `onAnimationFinished()` → VM clears `animating`. Using `moveHistory.size` as the `Animatable` key makes a new move restart the animation cleanly.
- **End-state (D-05) and restart (D-06):** model game-over as `GameUiState.status ∈ {Won, Stuck}` and render the dialog from that — it is **replayable state** (survives rotation), not a one-shot `Channel` effect (effects are for navigation/snackbar). Restart control resets `GameState` to the English-33 initial board; if `moveHistory.isNotEmpty()` and not at end-state, show a confirm dialog first (a small transient state flag).
- **State hoisting:** `GameUiState` is immutable and `@Immutable`-annotated; `GameViewModel` owns the private `MutableStateFlow`; `BoardCanvas` is a stateless leaf receiving `state` + callbacks (Anti-Pattern 1/10).

### Q6 — Walking-skeleton sequencing  *(RECOMMENDATION)*

The thinnest end-to-end vertical slice, ordered so **CI gates are green from the first commit**:

1. **Commit 0 — Scaffold + gates first (green on near-empty code).** Gradle wrapper (latest 9.x), `settings.gradle.kts` with `:app` + `:domain`, `gradle/libs.versions.toml`, `build-logic/` convention plugins (jvmToolchain 17, detekt, ktlint, boundary-test deps), `config/detekt/detekt.yml`, `.editorconfig`, `.github/workflows/ci.yml`, and one trivial `:domain` type + its test so `:domain:test` is non-empty. **Gate: `./gradlew ktlintCheck detekt check` green before any feature code.** This is what makes "gates green from the very first commit" literally true (QUAL-01..04 established up front, not retrofitted).
2. **Domain core (pure JVM).** `Position`, `Cell`, `Move`, `BoardSpec` (English-33), `Board` (bitmask + accessors), `GameState`, `RulesEngine` (`legalMoves`/`applyMove`/`isGameOver`/`pegCount`). JVM unit tests + the golden-master harness (`BoardCodec`, `GoldenMasterAssert`, fixtures). *(GAME-01 logic, GAME-02 detection, QUAL-04.)*
3. **ViewModel + UiState.** `GameUiState`, `GameViewModel` (select/deselect/two-step jump/restart/game-over), Hilt module + `@HiltViewModel`. Turbine tests over emissions. *(GAME-04 selection, GAME-05 restart, GAME-02 signal.)*
4. **Compose render + one real tap-to-jump.** `BoardCanvas` (hoisted geometry, draw, hit-test), `GameScreen`/`GameRoute`, `dimens.xml`, animation (D-07/D-08), `GameOverDialog` (D-05), `RestartControl` (D-06). `MainActivity` + `@HiltAndroidApp`. *(GAME-03, GAME-01 end-to-end.)*
5. **Dev build green.** `./gradlew assembleDebug check` green; optional Roborazzi board snapshot to lock the visual.

**Order rationale:** gates *before* feature code (so the first feature commit is already green and can never regress the criterion); pure domain *before* UI (testable dependency root, removes foundational risk on the JVM with no emulator); ViewModel *before* Canvas (Canvas is a pure render of `UiState`); animation *last* (polish atop an already-correct logical jump).

### Recommended Project Structure (Phase 1 surface)

```
peg-solitaire/
├─ settings.gradle.kts            # includes :app, :domain, build-logic
├─ gradle/libs.versions.toml      # single source of truth for versions
├─ build-logic/                   # convention plugins: detekt, ktlint, kotlin-jvm, android-app
├─ config/detekt/detekt.yml       # MagicNumber + ruleset config
├─ .github/workflows/ci.yml       # ktlintCheck + detekt + tests + assembleDebug, gitflow-gated
├─ domain/                        # PURE KOTLIN/JVM module — no Android plugin/classpath
│  ├─ src/main/kotlin/com/app/pegsolitaire/domain/
│  │  ├─ model/                   # Position, Cell, Move, BoardSpec, Board, GameState, GameStatus
│  │  └─ rules/                   # RulesEngine
│  └─ src/test/kotlin/...         # RulesEngine tests, golden-master (BoardCodec, GoldenMasterAssert)
│     └─ resources/golden/        # committed fixtures
└─ app/                           # Android module
   └─ src/main/java/com/app/pegsolitaire/
      ├─ ui/game/                 # GameRoute, GameScreen, GameViewModel, GameUiState, GameOverDialog
      ├─ ui/components/           # BoardCanvas, peg/board drawing
      ├─ ui/theme/                # Material 3 theme  +  res/values/dimens.xml
      ├─ di/                      # Hilt modules
      └─ MainActivity / App
   └─ src/test/java/...           # ArchUnit/Konsist boundary test (named gate), VM tests, dp/sp gate
```

### Anti-Patterns to Avoid (Phase-1 relevant; full list in ARCHITECTURE.md)
- **Game logic in `BoardCanvas`/`onClick`** — compute legality/apply/game-over in VM/domain only (Anti-Pattern 1).
- **Mutable `Board` re-emitted** — breaks `StateFlow` recomposition; use immutable `copy()` (Pitfall 6).
- **Game-over modeled as a one-shot effect** — it's replayable state; model as `status` (effects are for nav/snackbar).
- **Premature many-module graph** — exactly one split (`:domain`+`:app`); no further modularization in Phase 1.
- **Using stable detekt 1.x** — incompatible with Kotlin 2.4; only `dev.detekt` 2.0-alpha works.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Magic-number / style detection | Custom regex scanners | detekt `MagicNumber` + rulesets | Battle-tested; PSI-accurate; no type resolution needed |
| Format/lint | Hand-written style checks | ktlint-gradle | Canonical Kotlin formatter; auto-fix; CI-gating built in |
| Domain-purity enforcement | A grep for `android.` | `:domain` JVM module (structural) + ArchUnit | Compiler/bytecode guarantee beats string matching; Pitfall 9 calls it the strongest guard |
| Frame-timed animation | Manual `withFrameNanos` loop / progress in `UiState` | Compose `Animatable` / `animate*AsState` | Coroutine-driven, cancellable, frame-synced, draw-phase reads (Pitfalls 7/8) |
| DI wiring | Manual factories / service locator | Hilt (`@HiltViewModel`, `hiltViewModel()`) | Project mandate; compile-time graph; swaps Slice-2 bindings without touching gameplay |
| Version consistency | Hand-pinned per-artifact versions | Version catalog + Compose BOM | Prevents Compose/androidx drift (CLAUDE.md mandate) |
| Board persistence/equality | Custom hashing | `data class` over a `Long` bitmask | Free correct equals/hashCode; deterministic serialization for golden-master |
| 2D game rendering | A game engine (LibGDX/Godot) | Compose `Canvas` + `DrawScope` | A static grid puzzle; engine breaks the native-Compose decision (STACK.md) |

**Key insight:** every Phase-1 "build vs. use" decision has a mature, project-sanctioned tool. The *only* legitimately custom code is the game domain itself (`RulesEngine` + model), the golden-master codec, and the "no dp/sp literal" gate — everything else is wiring.

## Common Pitfalls

> Full catalogue in `.planning/research/PITFALLS.md`. Phase-1-applicable subset + new tooling pitfalls:

### Pitfall T1 (NEW): Standards toolchain doesn't support Kotlin 2.4 → build breaks on commit 0
**What goes wrong:** Applying stable detekt 1.23.8 against the pinned Kotlin 2.4.0; detekt cannot read Kotlin 2.3+ metadata and the `check` task fails (or behaves unpredictably with a `kotlin-compiler-embeddable` classpath-conflict warning).
**Why it happens:** Training data and most tutorials reference detekt 1.x stable; the Kotlin-2.4-compatible detekt is a 2.0 **alpha** with a **renamed plugin id** (`dev.detekt`).
**How to avoid:** Pin `dev.detekt` 2.0.0-alpha.5 (verify newest alpha/RC at setup); pin ktlint-gradle 14.2.0; for the purity test prefer bytecode-based ArchUnit over source-parsing Konsist on a Kotlin-2.4 codebase.
**Warning signs:** `Could not read Kotlin metadata`, `kotlin-compiler-embeddable is present in the build classpath along Kotlin Gradle plugin`, detekt failing only on newer-syntax files.

### Pitfall 6: `StateFlow` doesn't recompose because `Board` is mutated in place
**Avoid:** all domain models immutable `data class` with `val`; moves produce a new `GameState` via `copy()`; never mutate a backing array and re-emit. Add a test asserting a mutated board never equals the original.

### Pitfall 9: Android dependencies leak into the "pure Kotlin" domain
**Avoid:** the `:domain` JVM module makes `android.*` un-importable (structural). The named ArchUnit/Konsist boundary test is the CI-visible gate (QUAL-01). Keep all `@Parcelize`/serialization/`Log`/`Context` out of `:domain`.

### Pitfall 10: Logic creeps into composables / `MutableStateFlow` leaks out of the ViewModel
**Avoid:** composables render state + emit events only; VM exposes read-only `val uiState: StateFlow`; collect with `collectAsStateWithLifecycle()`.

### Pitfall 8: Canvas allocates / over-draws every frame
**Avoid:** hoist grid geometry / `Path` / `Brush` via `remember`; read animating values via lambda so only the draw phase re-runs; no object creation inside `DrawScope`.

### Pitfall 7 (forward-looking): Timer recomposes the whole board
**Note:** Phase 1 has **no elapsed-time clock** (scoring/time is Phase 2). Establish the state-shape discipline now: when the timer arrives, it must live *outside* the board's state holder (separate `StateFlow` / lambda read), never bundled into the `GameUiState` the Canvas reads.

### Pitfall 1 (forward-looking): non-deterministic enumeration
**Note:** Phase 1 has no PRNG, but `RulesEngine.legalMoves` should return a **deterministically sorted `List`** now — the cheapest possible de-risking of the Phase-3 generator determinism contract, and it makes the golden-master move-list fixture stable.

## Code Examples

> Illustrative shapes (verify exact APIs against Context7 / Compose BOM 2026.06.00 + the compose-skill at implementation time). Source: ARCHITECTURE.md Patterns 1/5, compose-skill `animations-advanced.md`.

### Immutable board over a Long bitmask (domain, pure Kotlin)
```kotlin
data class Position(val row: Int, val col: Int)

class BoardSpec(val inPlay: List<Position>, val target: Position?) {
    private val indexOf = inPlay.sortedWith(compareBy({ it.row }, { it.col }))
        .withIndex().associate { (i, p) -> p to i }
    val size get() = indexOf.size
    fun bit(p: Position): Int? = indexOf[p]
    operator fun contains(p: Position) = p in indexOf
}

data class Board(val spec: BoardSpec, val pegs: Long) {       // free equals/hashCode
    fun cellAt(p: Position): Cell = when {
        p !in spec -> Cell.Blocked
        (pegs shr spec.bit(p)!!) and 1L == 1L -> Cell.Peg
        else -> Cell.Empty
    }
    fun withPeg(p: Position) = copy(pegs = pegs or (1L shl spec.bit(p)!!))
    fun withoutPeg(p: Position) = copy(pegs = pegs and (1L shl spec.bit(p)!!).inv())
    val pegCount: Int get() = java.lang.Long.bitCount(pegs)   // pure-JVM, allowed in :domain
}
```

### RulesEngine — deterministic sorted enumeration (domain)
```kotlin
object RulesEngine {
    private val dirs = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0) // stable order
    fun legalMoves(b: Board): List<Move> = buildList {
        for (from in b.spec.inPlay) if (b.cellAt(from) == Cell.Peg)
            for ((dr, dc) in dirs) {
                val over = Position(from.row + dr, from.col + dc)
                val to = Position(from.row + 2*dr, from.col + 2*dc)
                if (to in b.spec && b.cellAt(over) == Cell.Peg && b.cellAt(to) == Cell.Empty)
                    add(Move(from, over, to))
            }
    }.sortedWith(compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }))
    fun applyMove(b: Board, m: Move) = b.withoutPeg(m.from).withoutPeg(m.over).withPeg(m.to)
    fun isGameOver(b: Board) = legalMoves(b).isEmpty()
}
```

### ArchUnit domain-purity boundary test (named gate for criterion #5)
```kotlin
@AnalyzeClasses(packages = ["com.app.pegsolitaire.domain"])
class DomainPurityTest {
    @ArchTest val noAndroid: ArchRule =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("android..", "androidx..")
}
```

### Animate-from-committed-state (UI, Compose)
```kotlin
// state.animating committed by the VM AFTER applyMove; Canvas is a pure render.
val jump = remember { Animatable(transition.fromCenter, Offset.VectorConverter) }
val fade = remember { Animatable(1f) }
LaunchedEffect(transition.id) {
    launch { jump.animateTo(transition.toCenter, tween(JUMP_MS)) }   // D-07 slide
    fade.animateTo(0f, tween(JUMP_MS))                              // D-08 fade+shrink
    onAnimationFinished()                                          // VM clears `animating`
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| detekt id `io.gitlab.arturbosch.detekt` | **`dev.detekt`** | detekt 2.0 | Plugin id rename; old id won't resolve 2.x; alpha is the only Kotlin-2.4 line |
| detekt 1.x stable | detekt 2.0.0-alpha for Kotlin 2.4 | 2026 | Stable can't read Kotlin 2.3+ metadata — must use alpha on this toolchain |
| KAPT for Hilt/Room | KSP2 (default in Kotlin 2.x) | Kotlin 2.x | No KAPT anywhere (CLAUDE.md) |
| Standalone Compose compiler artifact | `org.jetbrains.kotlin.plugin.compose` @ Kotlin version | Kotlin 2.0 | Compose compiler ships with Kotlin |
| `collectAsState()` | `collectAsStateWithLifecycle()` | lifecycle-runtime-compose | Avoids background collection / stale resume |

**Deprecated/outdated for this phase:**
- detekt 1.x, plugin id `io.gitlab.arturbosch.detekt` — replaced by `dev.detekt` 2.x.
- Material 2, `LiveData`/`RxJava`, hand-pinned Compose versions, `SharedPreferences` — all per CLAUDE.md "What NOT to Use" (none belong in Phase 1).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | ArchUnit `archunit-junit4` 1.3.x is current and works under JUnit4 + Kotlin 2.4 bytecode | Standard Stack / Q1 | Low — ArchUnit is bytecode-based and mature; verify exact version + that it sees Kotlin classes |
| A2 | detekt `MagicNumber` runs without type resolution on Kotlin 2.4 source | Q4 | Low-Med — if alpha changed rule internals, may need `detekt.yml` tweak; MagicNumber is historically a non-type-resolution style rule |
| A3 | Konsist 0.17.3 (embedded Kotlin 2.0.20 compiler) can parse Kotlin 2.4 domain source | Standard Stack | Med — mitigated by recommending ArchUnit instead; only matters if planner picks Konsist |
| A4 | detekt 2.0.0-alpha.5 is stable enough to gate CI for this project | Package Audit | Med — it's pre-release; pin exactly, re-verify newer alpha/RC/stable at planning time |
| A5 | English-33 (and the other 3 Phase-2 footprints) each fit in a single 64-bit `Long` | Q2 | Low — 33/37/41/15 holes all ≤ 64; verified by counting |
| A6 | No elapsed-time clock in Phase 1 (scoring/time is Phase 2) | Pitfall 7 | Low — confirmed by CONTEXT scope ("scoring … Phase 2") |
| A7 | "No dp/sp literal" needs a custom gate (no built-in detekt/ktlint rule) | Q4 | Low — accurate as of current detekt/ktlint; plan custom Konsist/grep task |

**These `[ASSUMED]`/version items should be confirmed at the start of planning** by re-running the version-verification commands in the Installation section — the toolchain is intentionally bleeding-edge and moves between research and execution.

## Open Questions

1. **Exact newest detekt 2.0 alpha/RC at planning time.**
   - What we know: 2.0.0-alpha.5 (2026-06-17) supports Kotlin 2.4.0, paired with Gradle 9.5.1 / AGP 9.2.1.
   - What's unclear: whether a newer alpha/RC/stable exists at execution.
   - Recommendation: re-check the detekt compatibility table; pin the newest line that lists Kotlin 2.4.0; bump AGP to 9.2.1 + use the latest Gradle 9.x wrapper to match detekt's tested pairing.

2. **AGP 9.2.0 vs 9.2.1 / Gradle wrapper version.**
   - What we know: CLAUDE.md pins AGP 9.2.0 / Gradle 9.1+; detekt alpha was tested against AGP 9.2.1 / Gradle 9.5.1.
   - Recommendation: AGP 9.2.1 is a patch and Gradle 9.5.1 > 9.1 — both compatible with CLAUDE.md's constraints; prefer them for the cleanest detekt pairing.

3. **`MainActivity`/`@HiltAndroidApp` minimum vs. theme polish.**
   - What we know: Phase 1 needs a single screen.
   - Recommendation: minimal Material 3 theme + one Activity hosting the game route; defer real theming to Slice 2 (POLI-01).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | AGP 9.x toolchain | ✓ | 17.0.15 (Homebrew) | — (set via `jvmToolchain(17)`) |
| Android SDK platforms | `compileSdk` | ✓ | android-30…36.1 (so `compileSdk 36` available) | — |
| Android build-tools | app build | ✓ | up to 36.0.0 | — |
| adb | install/run on device/emulator | ✓ | on PATH (`/opt/homebrew/bin/adb`) | — |
| Emulator + AVDs | instrumented/UI/Roborazzi-on-device | ✓ | Pixel, Pixel_9, Pixel_9a AVDs present | Roborazzi runs JVM-side (no device needed) |
| Gradle | build engine | ✗ (no system gradle) | — | **Gradle wrapper** (generate 9.x) — standard, not a blocker |
| cmdline-tools / sdkmanager | install new SDK packages | ✗ | — | Needed platforms already installed; install cmdline-tools only if a missing API level is required |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | Android build to find SDK | ✗ (unset) | SDK present at `~/Library/Android/sdk` | Provide `local.properties` (`sdk.dir=…`) or export `ANDROID_HOME` in CI |
| Kotlin compiler | compile | ✓ (via Gradle) | 2.4.0 (pulled by Gradle) | — (no system `kotlinc` needed) |

**Missing dependencies with no fallback:** none — the toolchain is fully present.
**Missing dependencies with fallback:**
- No system Gradle → generate the **Gradle wrapper** (pin latest 9.x; required by AGP 9 ≥ 9.1).
- `ANDROID_HOME` unset → add `local.properties` with `sdk.dir=/Users/ermanergoz/Library/Android/sdk` (gitignored) and export `ANDROID_HOME` in the CI workflow.
- No `cmdline-tools` → only needed if a not-yet-installed API level is targeted; current platforms (incl. 36) cover any reasonable `compileSdk`.

## Validation Architecture

> nyquist_validation is enabled (config.json `workflow.nyquist_validation: true`).

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Google Truth 1.4.x (domain); Turbine 1.2.1 (VM); Compose UI Test + Robolectric/Roborazzi (UI, optional Phase 1) |
| Config file | none yet — created in Wave 0 (convention plugin + `:domain` test source set) |
| Quick run command | `./gradlew :domain:test` (pure JVM, no emulator — fast) |
| Full suite command | `./gradlew ktlintCheck detekt check` (lint + format + all unit tests + boundary test) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GAME-01 | Legal orthogonal jump removes jumped peg | unit | `./gradlew :domain:test --tests *RulesEngineTest` | ❌ Wave 0 |
| GAME-01 | Golden-master: scripted line → expected boards | unit (golden) | `./gradlew :domain:test --tests *GoldenMasterTest` | ❌ Wave 0 |
| GAME-02 | `isGameOver` true when no legal moves | unit | `./gradlew :domain:test --tests *RulesEngineTest` | ❌ Wave 0 |
| GAME-04 | Select peg → correct sorted legal targets | unit + VM | `./gradlew :domain:test`; `./gradlew :app:testDebugUnitTest --tests *GameViewModelTest` | ❌ Wave 0 |
| GAME-05 | Restart resets to initial board | VM | `./gradlew :app:testDebugUnitTest --tests *GameViewModelTest` | ❌ Wave 0 |
| GAME-03 | Board renders + animates (smoke) | UI (Robolectric/Roborazzi) | `./gradlew :app:testDebugUnitTest --tests *BoardCanvas*` | ❌ Wave 0 (optional) |
| QUAL-01 | Domain has zero `android.*`/`androidx.*` | arch | `./gradlew :app:testDebugUnitTest --tests *DomainPurityTest` (+ structural module) | ❌ Wave 0 |
| QUAL-02 | detekt + ktlint pass | gate | `./gradlew detekt ktlintCheck` | ❌ Wave 0 |
| QUAL-03 | No magic numbers; no dp/sp literals | gate | `./gradlew detekt` + custom dp/sp test/task | ❌ Wave 0 |
| QUAL-04 | RulesEngine + golden-master covered | unit | `./gradlew :domain:test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (and `:app:testDebugUnitTest` once VM exists) — seconds, JVM-only.
- **Per wave merge:** `./gradlew ktlintCheck detekt check`.
- **Phase gate:** `./gradlew ktlintCheck detekt check assembleDebug` green before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] `build-logic/` convention plugins (kotlin-jvm, android-app, detekt, ktlint) — establishes the gate
- [ ] `gradle/libs.versions.toml` with the Phase-1 tooling additions
- [ ] `config/detekt/detekt.yml` (MagicNumber + ruleset config)
- [ ] `.github/workflows/ci.yml` (ktlintCheck + detekt + tests + assembleDebug, gitflow-gated)
- [ ] `:domain` test source set + `RulesEngineTest`, `BoardCodec`, `GoldenMasterAssert`, fixtures (covers GAME-01/02, QUAL-04)
- [ ] `DomainPurityTest` (ArchUnit) + structural `:domain` module (QUAL-01)
- [ ] `GameViewModelTest` (Turbine) — selection/jump/restart/game-over (GAME-04/05/02)
- [ ] Custom "no dp/sp literal" gate (QUAL-03) — Konsist test or Gradle verification task
- [ ] Framework install: add JUnit4/Truth/Turbine/ArchUnit to catalog + module deps

## Security Domain

> `security_enforcement` is absent in config → treated as enabled. Phase 1 is an **offline, single-player, local-only** game with **no network, no auth, no persistence, no crypto, no PII** (persistence is Phase 5; ads/Play Games are Slice 2). Most ASVS categories are therefore N/A for this phase.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth in Slice 1 (Play Games sign-in is Slice 2, optional) |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No multi-user / no protected resources |
| V5 Input Validation | yes | The only untrusted input is **taps**. The VM/`RulesEngine` re-validate every move against `legalMoves` — the UI is never trusted to submit a legal `Move`. Tap→Position bounds-checked against `BoardSpec`. |
| V6 Cryptography | no | No secrets/crypto in Phase 1 |
| V7 Error Handling/Logging | minor | No `android.util.Log` or sensitive data in `:domain` (also a purity rule); fail closed on illegal moves (no-op, never crash) |

### Known Threat Patterns for this phase
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| UI submits an illegal/forged move | Tampering | RulesEngine is the single authority; VM rebuilds `legalTargets` from domain and only applies moves in that set |
| Tap maps to out-of-board Position | Tampering/DoS | Bounds-check against `BoardSpec`; non-in-play taps are no-ops (D-04) |
| Supply-chain (alpha/new plugins) | Tampering | Pin exact versions in the catalog; verify against official Gradle Plugin Portal/GitHub; treat detekt-alpha bumps as deliberate (Package Audit) |

## Sources

### Primary (HIGH confidence)
- `.planning/research/STACK.md`, `ARCHITECTURE.md`, `PITFALLS.md`, `SUMMARY.md` — verified project-level stack/architecture/pitfalls (2026-06-28); inherited wholesale, not re-derived
- `CLAUDE.md` — mandatory tech-stack pins + code standards
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §3, §8, §8.2, §8.5 — game rules, architecture, domain layer, package structure
- `~/.claude/skills/compose-skill/SKILL.md` — UDF/state-hoisting/Route-Screen-Leaf boundary, effects-via-Channel, `collectAsStateWithLifecycle`
- https://detekt.dev/docs/introduction/compatibility/ — **only detekt 2.0.0-alpha.5 supports Kotlin 2.4.0**; stable 1.x can't read Kotlin 2.3+ metadata (verified this session)
- https://plugins.gradle.org/plugin/dev.detekt — detekt 2.x plugin id + version (verified this session)
- https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint — ktlint-gradle 14.2.0, Kotlin 2.4 + new-DSL support (verified this session)

### Secondary (MEDIUM confidence)
- https://github.com/LemonAppDev/konsist + docs.konsist.lemonappdev.com/help/compatibility — Konsist 0.17.3, embedded Kotlin 2.0.20 compiler (parse-risk caveat)
- https://github.com/detekt/detekt/issues/8865, /7883, /7384 — detekt vs newer-Kotlin metadata/classpath conflicts (corroborates T1)
- ArchUnit (`com.tngtech.archunit:archunit-junit4`) — bytecode-based layer/import rules (version to verify at setup)

### Tertiary (LOW confidence — verify at setup)
- Exact ArchUnit/Konsist latest versions and ktlint-gradle's bundled ktlint engine version — confirm via `./gradlew dependencies` / Maven Central at implementation time

## Metadata

**Confidence breakdown:**
- Standard stack (app): HIGH — inherited from verified project research + CLAUDE.md
- Standard stack (Phase-1 tooling deltas): HIGH for detekt/ktlint (verified live against official Gradle Plugin Portal + detekt compatibility table); MEDIUM for exact ArchUnit/Konsist versions (verify at setup)
- Architecture / discretion resolutions (Q1–Q6): HIGH — derived from verified ARCHITECTURE.md/PITFALLS.md + spec + compose-skill; recommendations are prescriptive with honest alternatives
- Pitfalls: HIGH — project pitfalls catalogue + the new toolchain-compatibility pitfall verified this session

**Research date:** 2026-06-28
**Valid until:** ~2026-07-12 (7–14 days — the standards toolchain is intentionally bleeding-edge: detekt is an alpha and ktlint-gradle/Konsist/ArchUnit move; re-verify versions at planning start)
