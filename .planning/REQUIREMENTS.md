# Requirements: Peg Solitaire (Android)

**Defined:** 2026-06-28
**Core Value:** Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying.

## v1 Requirements

Slice 1 — Playable Core. Fully playable and testable; ads and Play Games exist only as no-op stubs.

### Gameplay

- [x] **GAME-01**: Player can jump a peg orthogonally over an adjacent peg into an empty hole, removing the jumped peg
- [x] **GAME-02**: Game detects and signals the end state when no legal moves remain
- [x] **GAME-03**: Board renders on a Compose Canvas with animated jump and peg-removal feedback
- [x] **GAME-04**: Player can select a peg and see its legal destinations highlighted
- [x] **GAME-05**: Player can restart the current level at any time

### Classic Mode

- [ ] **CLAS-01**: Player can browse and select from a catalog of classic boards (English-33, European-37, Diamond-41, Wiegleb-45, Asymmetric 3-3-2-2 — Triangle-15 removed; all boards are orthogonal square-grid, see Phase 2 CONTEXT board-catalog resolution)
- [ ] **CLAS-02**: Player can play any classic board to completion with correct rules

### Challenge Mode

- [ ] **CHAL-01**: Player can play sequential Challenge levels numbered from 1
- [ ] **CHAL-02**: Each Challenge level is procedurally generated and guaranteed solvable
- [ ] **CHAL-03**: Challenge level N is deterministic — the same board every time (carries a generatorVersion)
- [ ] **CHAL-04**: Challenge difficulty scales with level number via board size/shape, peg density, and starting pattern
- [ ] **CHAL-05**: Player can continue from their highest reached Challenge level

### Scoring

- [ ] **SCOR-01**: Player earns 1–3 stars at level end based on pegs remaining
- [ ] **SCOR-02**: Elapsed time contributes to a separate numeric score
- [ ] **SCOR-03**: Results screen shows stars, pegs remaining, time, and score

### Assist

- [ ] **ASST-01**: Player can use a hint that reveals the next winning move (free in Slice 1)
- [ ] **ASST-02**: Player can undo the last move
- [ ] **ASST-03**: Hint handles deviation — falls back to the solver, and shows a clear "no winning move" state when the position is unsolvable

### Persistence

- [ ] **PERS-01**: Player's per-level results (stars, best time, best score) are saved locally
- [ ] **PERS-02**: Player's highest Challenge level reached is saved
- [ ] **PERS-03**: Player can resume an in-progress game after leaving (board + move history + elapsed time restored)

### Navigation & Screens

- [ ] **NAV-01**: App presents a Home screen with entry to Classic and Challenge modes
- [ ] **NAV-02**: Player can navigate between Home, mode selection, gameplay, and results screens

### Settings

- [ ] **SET-01**: Player can toggle sound on/off
- [ ] **SET-02**: Player can toggle haptics on/off

### Onboarding

- [ ] **ONB-01**: First-time player sees a brief how-to-play explanation of the jump rule

### Quality & Standards

- [x] **QUAL-01**: Domain layer is pure Kotlin with no Android dependencies, enforced by a CI boundary test
- [x] **QUAL-02**: detekt and ktlint are configured and pass as a build gate
- [x] **QUAL-03**: No magic numbers/strings — values live in scoped constants files; all UI dp/sp in `res/values/dimens.xml`
- [x] **QUAL-04**: Domain game logic (rules, generator, solver, scoring) covered by JVM unit tests, including a golden-master determinism test for generated levels

## v2 Requirements

Slice 2 — Online & Monetization. Deferred; implemented by swapping Hilt bindings behind existing interfaces.

### Play Games

- **PGS-01**: Player can sign in with Google Play Games Services v2
- **PGS-02**: Player's results submit to leaderboards (highest level, best scores)
- **PGS-03**: Player can unlock achievements
- **PGS-04**: Player progress syncs via cloud save with explicit conflict-merge (max level + best-per-level)

### Monetization

- **ADS-01**: Player can watch a rewarded ad to gain a hint/undo/retry
- **ADS-02**: Interstitial ads show between levels with frequency capping and UMP consent

### Polish

- **POLI-01**: Visual theming/polish pass beyond the single clean v1 theme

## Out of Scope

| Feature | Reason |
|---------|--------|
| HTML5 / Google H5 Games Ads | Incompatible with native Kotlin build; AdMob is the native equivalent |
| Online / real-time multiplayer | Not core to a single-player puzzle game |
| Daily challenges | Deferred; not needed to validate core value |
| Custom board editor | Deferred; not core |
| Level sharing | Deferred; depends on social/online infra |
| Diagonal jumps | Challenge keeps classic orthogonal-only rules |
| Compose Multiplatform / iOS | Android-only for now |
| Lives / energy / forced sign-in | Known casual-puzzle churn drivers; deliberately excluded |

## Traceability

Each v1 requirement maps to exactly one phase. See `.planning/ROADMAP.md` for phase detail.

| Requirement | Phase | Status |
|-------------|-------|--------|
| GAME-01 | Phase 1 | Complete |
| GAME-02 | Phase 1 | Complete |
| GAME-03 | Phase 1 | Complete |
| GAME-04 | Phase 1 | Complete |
| GAME-05 | Phase 1 | Complete |
| QUAL-01 | Phase 1 | Complete |
| QUAL-02 | Phase 1 | Complete |
| QUAL-03 | Phase 1 | Complete |
| QUAL-04 | Phase 1 | Complete |
| CLAS-01 | Phase 2 | Pending |
| CLAS-02 | Phase 2 | Pending |
| SCOR-01 | Phase 2 | Pending |
| SCOR-02 | Phase 2 | Pending |
| SCOR-03 | Phase 2 | Pending |
| NAV-01 | Phase 2 | Pending |
| NAV-02 | Phase 2 | Pending |
| ONB-01 | Phase 2 | Pending |
| CHAL-01 | Phase 3 | Pending |
| CHAL-02 | Phase 3 | Pending |
| CHAL-03 | Phase 3 | Pending |
| CHAL-04 | Phase 3 | Pending |
| ASST-01 | Phase 4 | Pending |
| ASST-02 | Phase 4 | Pending |
| ASST-03 | Phase 4 | Pending |
| PERS-01 | Phase 5 | Pending |
| PERS-02 | Phase 5 | Pending |
| PERS-03 | Phase 5 | Pending |
| SET-01 | Phase 5 | Pending |
| SET-02 | Phase 5 | Pending |
| CHAL-05 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 30 total
- Mapped to phases: 30 ✓
- Unmapped: 0

---
*Requirements defined: 2026-06-28*
*Last updated: 2026-06-28 after roadmap creation (traceability mapped)*
