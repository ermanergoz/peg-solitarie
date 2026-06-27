# Feature Research

**Domain:** Single-player board puzzle game (peg solitaire) — native Android, casual/brain-training segment
**Researched:** 2026-06-28
**Confidence:** HIGH (spec-driven, corroborated by Google Play competitor surveys and casual-puzzle UX literature)

## Context

This research maps the feature landscape for peg solitaire and comparable casual puzzle games, then categorizes each feature as **table stakes**, **differentiator**, or **anti-feature** for *this* product. The project's stated Core Value is **infinite, unique, guaranteed-solvable procedurally-generated Challenge levels** plus a Classic mode — so categorization is anchored to that, not to a generic puzzle app.

The product ships in two slices (per spec §11):
- **Slice 1 — Playable Core:** rules, both modes, generator, hint/undo, stars/score, local persistence + resume, the 5 screens, ad/games SDKs stubbed behind interfaces. Fully playable and testable, no SDKs.
- **Slice 2 — Online & Monetization:** Play Games (sign-in, leaderboards, achievements, cloud save), AdMob (rewarded + interstitial), theming/polish.

Every feature below is tagged with the slice it belongs to so the roadmap respects that boundary.

## Competitor Baseline (what shipped peg-solitaire apps actually have)

Surveying the leading Google Play peg-solitaire titles (Pinkpointer's *Peg Solitaire*, *Peg Genius*, rc4812 *Logic Puzzle*, *Solo Noble* variants) the recurring feature set is:

- A **catalog of named classic boards** (English, European/Continental, Triangle, Diamond, plus many exotic shapes — Wiegleb, Hexagon, Star, etc.). Board *variety* is the main axis competitors compete on.
- **Undo / redo** (usually unlimited), **board reset/restart**, **hint / movable-position highlighting**.
- **Move counter + timer / best-score stats** per board.
- **Google Play achievements** (common); leaderboards (less common).
- Ad-supported free tier, occasional paid "remove ads."

Crucially, **none differentiate on an infinite procedurally-generated, guaranteed-solvable progression** — they ship finite hand-authored catalogs. That gap is exactly this product's differentiator.

## Feature Landscape

### Table Stakes (Users Expect These)

Missing any of these makes a peg-solitaire app feel broken or unfinished. Users give no credit for having them but churn without them.

| Feature | Why Expected | Complexity | Slice | Notes |
|---------|--------------|------------|-------|-------|
| Correct orthogonal jump mechanic (tap-to-select peg, tap empty target; jumped peg removed) | This *is* the game | MEDIUM | 1 | `RulesEngine.legalMoves/applyMove`. Pure Kotlin, heavily unit-tested. Tap-tap selection is the casual-friendly default; drag is a polish option. |
| Clear win/lose detection (game ends when no legal move remains; 1 peg = ideal) | Players must know when they're done and how they did | LOW | 1 | Game-over = `legalMoves().isEmpty()`. Surfaced on Results screen. |
| Undo last move | Single most-requested puzzle affordance; competitors offer unlimited undo | LOW–MEDIUM | 1 | Move-history stack. Spec makes it free in Slice 1, ad-gated in Slice 2. Multi-step undo expected; redo optional. |
| Restart / reset current board | Recover from a dead-end without leaving the level | LOW | 1 | Re-load level from seed/catalog; deterministic seeding makes this trivial. |
| Hint (show a valid next move) | Brain-training audience expects a nudge; reduces rage-quit churn | MEDIUM | 1 | Reverse-generation *records the winning line*, so a free hint is "replay next recorded move from current state," with `Solver` fallback when the player deviated. Far cheaper than a generic solver-on-demand. |
| Classic boards catalog (English-33, European-37, Triangle-15, Diamond-41) | The canonical shapes; their absence reads as "incomplete peg solitaire" | LOW–MEDIUM | 1 | Static data + `Solver` test-validates solvability. Only 4 at launch — acceptable because Challenge mode supplies effectively infinite content. |
| Level / board select screen | Players choose what to play; resume the journey | MEDIUM | 1 | Classic picker (grid of boards) + Challenge continue/level-select. |
| Star rating per level (1–3) | Universal casual-puzzle completion signal; drives replay | LOW | 1 | Driven by pegs remaining (1→★★★, 2→★★, 3→★, 4+→none). Decoupled from time so slow-but-perfect still earns 3 stars. |
| Progress persistence (highest level, per-level best stars/score) | Losing progress on app close is unacceptable | MEDIUM | 1 | `ProgressRepository` + Room/DataStore. |
| Resume in-progress game | Players expect to return to a half-solved board after backgrounding | MEDIUM | 1 | `InProgressGame` snapshot (serialized board, move history, elapsed). Deterministic seeds keep snapshots small. |
| Sound + haptics toggles in Settings | Standard mobile courtesy; off-by-context (commute) usage | LOW | 1 | `SettingsRepository` via DataStore. Jump/removal SFX + a satisfying haptic tick is core to "satisfying jump feel" goal. |
| Visible move/peg counter and timer during play | Players self-measure; feeds score | LOW | 1 | Pegs-remaining count + elapsed timer in the gameplay HUD. |
| Satisfying jump/removal animation | "Polished" is a stated goal; juice is a retention driver in casual puzzle | MEDIUM | 1 | Compose `Canvas` animation of peg travel + removed-peg fade/pop. |
| Lightweight tutorial / first-level onboarding | Not everyone knows peg-solitaire rules; FTUE drives D1 retention | LOW–MEDIUM | 1 | Interactive overlay on first play (show a legal jump), skippable. Casual-puzzle best practice: teach via doing, not text walls. |

### Differentiators (Competitive Advantage)

These are where the product wins. They map directly to Core Value and to gaps in the competitor set.

| Feature | Value Proposition | Complexity | Slice | Notes |
|---------|-------------------|------------|-------|-------|
| **Infinite, sequential Challenge levels** | Never run out of content; "one more level" loop. No competitor offers endless peg solitaire | HIGH | 1 | `LevelGenerator`. The headline feature. |
| **Guaranteed-solvable generation (reverse-generation)** | Every level is fair — never an impossible board (the #1 trust-killer for procedural puzzles) | HIGH | 1 | Generate backwards from the 1-peg solved state via reverse jumps. Solvability is *structural*, not searched-for. |
| **Recorded winning line → free, reliable hints** | Hints are correct by construction and cheap to compute; underpins the whole hint UX | MEDIUM | 1 | Falls out of reverse-generation for free; `Solver` only needed as fallback after deviation. |
| **Deterministic level seeding (level N is always identical)** | Enables retry, resume, and fair cross-device/cross-player comparison; foundation for leaderboards | MEDIUM | 1 | `seed = hash(N)`. Reproducibility validated in tests. |
| **Difficulty curve via board size/shape, peg density, starting pattern** | Sense of progression and mastery; keeps the infinite stream from feeling samey | HIGH | 1 | Data-driven knobs so the curve is tunable without code changes (spec flags this as the key tuning risk). |
| **Separate ranking score (stars from pegs, time feeds score)** | Rewards speed for competitive players *without* punishing methodical players with withheld stars | LOW–MEDIUM | 1 | `score = base(pegsRemaining)*1000 − timePenalty`, floored. Score reserved for Slice 2 leaderboards. |
| **Leaderboards on highest level + best scores** | Turns the deterministic infinite ladder into a competitive race; strong retention/virality hook | MEDIUM | 2 | Google Play Games v2. Only meaningful *because* levels are deterministic. |
| **Achievements** | Cheap, well-understood progression rewards (e.g. "reach level 50", "perfect 10 in a row") | LOW–MEDIUM | 2 | Play Games. |
| **Cloud save (cross-device progress)** | Protects the long Challenge climb across devices/reinstalls | MEDIUM | 2 | Play Games Saved Games, behind existing repositories. |

### Anti-Features (Commonly Requested, Often Problematic — Do NOT Build)

These are explicitly out of scope per PROJECT.md/spec, plus genre traps. Documented to prevent scope creep.

| Feature | Why Requested / Surface Appeal | Why Problematic | Alternative |
|---------|-------------------------------|-----------------|-------------|
| Diagonal jumps | "More moves = more variety" | Breaks the classic rule set both modes commit to; doubles generator/solver complexity and balancing | Keep orthogonal-only across both modes (spec rule). Variety comes from board shape, not jump direction. |
| Online / real-time multiplayer | Social engagement, virality | Peg solitaire is inherently solo; massive infra + matchmaking cost for a turnless puzzle | Asynchronous competition via leaderboards (Slice 2). |
| Daily challenges | Habit/retention loop | Extra content pipeline + notification infra; not needed to validate core value | Deferred. The infinite deterministic ladder already provides an endless "next" loop. |
| Custom board editor / level sharing | Power-user creativity, UGC | Solvability of user boards isn't guaranteed → undermines the "always solvable" trust promise; moderation/sharing infra | Deferred. Generator already produces infinite unique boards. |
| Multiple visual themes / skins | Personalization | Multiplies asset + QA surface before product-market fit | Ship one clean Material 3 theme; theming is Slice 2 polish at most. |
| Compose Multiplatform / iOS | Wider reach | Doubles platform surface pre-validation; conflicts with tight Play Games/AdMob native integration | Android-only now. |
| HTML5 / H5 Games Ads | "Web reach + easy ads" | Incompatible with chosen native Kotlin build | AdMob is the native equivalent (rewarded + interstitial). |
| Par-time gating of the 3rd star | "Rewards skill" | Withholds stars from slow-but-perfect solves → frustration; spec deliberately rejected this | Stars from pegs remaining; time lives in the *ranking score* instead. (Flagged open decision — default is the score model.) |
| Unlimited free hints / unlimited free undo (Slice 2) | "Player-friendly" | Removes the rewarded-ad monetization lever and trivializes Challenge difficulty | Free in Slice 1 to validate fun; gate behind rewarded ads in Slice 2 (`AdGateway` decides; gameplay just requests "grant a hint"). |
| Aggressive / frequent interstitials | Revenue | Mid-puzzle or every-level interstitials are the top churn driver in casual puzzle | Interstitials only between levels with a frequency cap; lean on rewarded (opt-in) ads. Slice 2. |
| Forced account/sign-in at launch | "Capture the user" | Sign-in friction tanks D1 retention; the core game needs no account | Play Games sign-in optional and deferred to Slice 2; full game playable signed-out. |
| Lives / energy / timers-to-play | Monetization pressure | Antithetical to a relaxed brain-training solitaire; punishes the target audience | None — keep play unlimited. Monetize via opt-in rewarded ads + light interstitials. |

## Feature Dependencies

```
RulesEngine (legal moves, apply, game-over)
    └──required by──> Gameplay loop (tap-to-jump, win/lose)
                          ├──required by──> Undo (move-history stack)
                          ├──required by──> Restart (reload from seed/catalog)
                          └──required by──> Star/Score calculation

LevelGenerator (reverse-generation, seeded)
    ├──produces──> Guaranteed-solvable board  ──required by──> Challenge mode
    ├──produces──> Recorded winning line  ──required by──> Hint (with Solver fallback)
    └──depends on──> Deterministic seeding (hash(N))
                          └──required by──> Resume, Retry, Leaderboards (Slice 2)

Difficulty scaling ──parameterizes──> LevelGenerator

ProgressRepository (highest level, per-level results)
    ├──required by──> Level select / Challenge continue
    └──required by──> Cloud save (Slice 2, same repository, cloud source)

InProgressGame snapshot ──required by──> Resume

ScoreCalculator (stars from pegs; numeric score from time)
    └──required by──> Results screen ──feeds──> Leaderboards (Slice 2)

AdGateway / GamesService interfaces (Slice 1 stubs)
    └──required by──> AdMob + Play Games implementations (Slice 2)
```

### Dependency Notes

- **Hint requires LevelGenerator's recorded line:** because solvability is structural, the canonical hint is "next recorded move." The `Solver` is only the fallback path once a player deviates — so Hint can ship in Slice 1 without an expensive general solver on the hot path.
- **Resume / Retry / Leaderboards all require deterministic seeding:** if level N weren't reproducible, snapshots and cross-player comparison would be impossible. Seeding is a foundational early dependency, not a feature to bolt on later.
- **Cloud save reuses ProgressRepository:** because persistence sits behind the repository (single source of truth), adding the Play Games Saved Games source in Slice 2 requires no changes to game/UI code.
- **All monetization/social features depend on the Slice 1 interface stubs (`AdGateway`, `GamesService`):** defining these no-op interfaces in Slice 1 is what keeps the core SDK-free and lets Slice 2 drop in implementations without touching domain logic.
- **Difficulty scaling conflicts with nothing but must be data-driven:** spec flags curve-tuning as the top risk; keep knobs in data so tuning never requires regenerating the seed→board contract.

## MVP Definition

### Launch With (Slice 1 — Playable Core)

The minimum to validate the Core Value ("are infinite, fair, generated levels actually fun?").

- [ ] Orthogonal jump mechanic + win/lose detection — the game itself
- [ ] Classic mode with 4 boards (English-33, European-37, Triangle-15, Diamond-41) — genre credibility
- [ ] Challenge mode: infinite, sequential, deterministic, guaranteed-solvable levels — the differentiator under test
- [ ] Difficulty scaling (board size/shape, density, starting pattern), data-driven — without it the infinite stream feels flat
- [ ] Free hint (recorded line + Solver fallback) and undo — table-stakes affordances
- [ ] Restart, pegs/timer HUD — table stakes
- [ ] Stars (from pegs) + numeric ranking score (time) on Results screen
- [ ] Local persistence: highest level, per-level best stars/score, in-progress resume
- [ ] Level select (Classic picker + Challenge continue/select)
- [ ] Settings: sound + haptics toggles
- [ ] Lightweight first-level onboarding (skippable)
- [ ] Five screens: Home, Classic picker, Challenge, Gameplay, Results
- [ ] `AdGateway` + `GamesService` as no-op stubs (so the core stays SDK-free)

### Add After Validation (Slice 2 — Online & Monetization)

Trigger: Slice 1 proves the generated levels are fun and the difficulty curve is acceptable.

- [ ] Google Play Games sign-in (optional, non-blocking)
- [ ] Leaderboards (highest level reached, best scores) — turns the deterministic ladder competitive
- [ ] Achievements
- [ ] Cloud save (Play Games Saved Games via existing repositories)
- [ ] AdMob rewarded ads gating hint/undo/retry (`AdGateway` implementation)
- [ ] AdMob interstitials between levels (frequency-capped)
- [ ] Visual theming / polish pass

### Future Consideration (v2+ — deferred, not anti-features)

Defer until product-market fit; revisit only if data demands.

- [ ] Additional / exotic classic boards (Hexagon, Star, Wiegleb, etc.) — competitors' main axis; cheap to add as static data once core is proven
- [ ] Daily challenge built on the existing deterministic seed system — low marginal cost if retention needs a habit hook
- [ ] Redo (complement to undo) — minor convenience
- [ ] Color-blind / accessibility palette and larger-target options — broaden audience

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Orthogonal jump mechanic + win/lose | HIGH | MEDIUM | P1 |
| Challenge generator (reverse-gen, seeded, solvable) | HIGH | HIGH | P1 |
| Difficulty scaling (data-driven) | HIGH | HIGH | P1 |
| Classic 4-board catalog | HIGH | LOW | P1 |
| Hint (recorded line + Solver fallback) | HIGH | MEDIUM | P1 |
| Undo + Restart | HIGH | LOW | P1 |
| Stars + ranking score | HIGH | LOW | P1 |
| Local persistence + resume | HIGH | MEDIUM | P1 |
| Level select screens | MEDIUM | MEDIUM | P1 |
| Settings (sound/haptics) | MEDIUM | LOW | P1 |
| Onboarding / first-level tutorial | HIGH | LOW | P1 |
| Jump/removal animation + juice | MEDIUM | MEDIUM | P1 |
| AdGateway / GamesService stub interfaces | MEDIUM | LOW | P1 |
| Leaderboards | HIGH | MEDIUM | P2 |
| Achievements | MEDIUM | LOW | P2 |
| Cloud save | MEDIUM | MEDIUM | P2 |
| Rewarded ads (hint/undo/retry) | HIGH (revenue) | MEDIUM | P2 |
| Interstitials between levels | MEDIUM (revenue) | LOW | P2 |
| Theming / polish | MEDIUM | MEDIUM | P2 |
| Additional exotic boards | MEDIUM | LOW | P3 |
| Daily challenge | MEDIUM | MEDIUM | P3 |
| Redo / accessibility extras | LOW | LOW | P3 |

**Priority key:** P1 = Slice 1 launch · P2 = Slice 2 · P3 = future consideration

## Competitor Feature Analysis

| Feature | Pinkpointer / Peg Genius / typical Play apps | Online peg solvers (pegsolitaire.fun) | Our Approach |
|---------|----------------------------------------------|---------------------------------------|--------------|
| Content model | Finite catalog of hand-authored boards (often many exotic shapes) | Fixed set + auto-solver | **Infinite procedurally-generated deterministic ladder** + 4 classics |
| Solvability guarantee | Known per static board | N/A | **Structural guarantee via reverse-generation** |
| Hints | On-demand solver or movable-position highlight | Full auto-solver | **Recorded winning line** (free, exact) + Solver fallback |
| Undo/Restart | Usually unlimited undo + reset | Undo | Undo (free S1 / ad-gated S2) + Restart |
| Scoring | Move counter / timer / best stats | Move count | **Stars from pegs (fair) + separate time-based ranking score** |
| Social | Play achievements common; leaderboards rare | None | Achievements + **leaderboards on the deterministic ladder** (S2) |
| Monetization | Banner/interstitial ads, paid remove-ads | Free web | **Opt-in rewarded** (hint/undo/retry) + capped interstitials (S2) |
| Themes | Some offer multiple skins | Minimal | One clean Material 3 theme (theming deferred) |

## Sources

- Google Play competitor survey — *Peg Solitaire* (Pinkpointer), *Peg Genius*, *Peg Solitaire: Logic Puzzle* (rc4812), *Solo Noble* variants: https://play.google.com/store/apps/details?id=com.pinkpointer.pegsolitaire , https://play.google.com/store/apps/details?id=com.peg.solitaire , https://play.google.com/store/apps/details?id=rc4812.android.senku (board variety, undo/redo, hints, timers, Play achievements) — MEDIUM confidence (store listings)
- Online reference for board taxonomy and auto-solver conventions — https://pegsolitaire.fun/ — MEDIUM
- Casual-puzzle onboarding & retention UX — Udonis FTUE guide (https://www.blog.udonis.co/mobile-marketing/mobile-games/first-time-user-experience), Sunday.gg level-design for ad-monetized casual games (https://sunday.gg/mobile-game-level-design-key-considerations-for-ad-monetized-casual-mobile-games/), Apple "Onboarding for Games" (https://developer.apple.com/app-store/onboarding-for-games/) — MEDIUM (informs table-stakes UX and ad-frequency anti-features)
- Project spec `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` and `.planning/PROJECT.md` — HIGH (authoritative for scope, slices, mechanics)

---
*Feature research for: single-player peg-solitaire puzzle game (native Android)*
*Researched: 2026-06-28*
