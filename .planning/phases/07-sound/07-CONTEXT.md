# Phase 7: Sound - Context

**Gathered:** 2026-08-28
**Status:** Ready for planning

<domain>
## Phase Boundary

The game gets its voice: the SOUND-01..06 SFX set wired to the already-persisted `soundOn` flag. Every meaningful action makes a sound — pitch-varied jumps, distinct removal/selection/undo/invalid-tap feedback, primary-button presses, and a win + rising star-pop celebration synced to the Results animation. Sound plays at system media volume and never ducks or interrupts the player's own audio (SOUND-06); turning `soundOn` off silences everything immediately, including queued one-shots (SOUND-05); the first tap of a session is never silent (preload requirement in SOUND-01/02/04 success criteria).

This phase is deliberately **SDK-free and Activity-free** — it must not touch the ads/PGS seams or the Phase 6 platform plumbing, and it can run in parallel with other v1.1 work.

</domain>

<decisions>
## Implementation Decisions

### Sound aesthetic & asset sourcing
- **D-01:** Assets come from **curated CC0 packs** (e.g. Kenney audio sets, freesound CC0). The executor downloads, trims, and commits them during the phase. **License provenance must be recorded in-repo** (a CREDITS/LICENSES note listing each file's source and license).
- **D-02:** Aesthetic is **soft candy-clay pops** — squishy, rounded, playful, matching the Candy Clay visual redesign (merged to `develop` 2026-08-27, see `docs/ui/DESIGN-SYSTEM.md`). Not wooden, not sterile UI blips.
- **D-03:** Curation favors **one cohesive pack family**, borrowing individual sounds elsewhere only where the anchor pack has no fit. Consistency beats per-event optimality.
- **D-04:** Approval is **audit at verify/UAT** — the executor curates and wires the full set; the user judges by playing the build during phase verification and swaps any duds then. No mid-phase audition checkpoint.

### Jump pitch variation (SOUND-01)
- **D-05:** Jump pitch **rises across a run**, resetting to base when the run ends. Random-only variation was rejected — the ladder should feel like a combo reward.
- **D-06:** A "run" is **same-peg only**: pitch climbs only while the same peg keeps jumping (the classic multi-capture run). Jumping with a different peg resets the ladder to base. This is the purist reading — chains are rarer and therefore mean more.
- **D-07:** The ladder has **no pitch cap** — every jump in a run steps up, without ceiling. Epic runs are allowed to get audibly silly; that is the point.
- **D-08:** **Undo steps the ladder back down one** rather than resetting it. The pitch ladder mirrors board state exactly: undoing a run jump then redoing it replays the same pitch it had before.
- **D-07a (resolved at plan-phase, 2026-08-28):** D-07's "no cap" meets SoundPool's hard 2.0× rate ceiling (one octave). User confirmed: **cap at 2.0× via `coerceIn`**, ceiling documented in KDoc. Ladder rises a semitone per jump and plateaus at the octave. Longest recorded same-peg run in the golden solutions is 3, so canonical play never approaches the cap. No AudioTrack/Oboe migration.
- **D-05a (resolved at plan-phase, 2026-08-28):** Re-tapping the selected peg (deselect) **reuses the selection sound** — it is a successful action and must never fire the invalid-tap cue. Symmetric feedback, no extra asset.

### Button-press coverage (SOUND-04)
- **D-09:** The button sound covers **navigational CTAs**: Home mode buttons, board selection, Results actions (Continue/Retry/etc.), and dialog confirm buttons. In-game assist controls (Undo, Hint) keep their own distinct event sounds from SOUND-02; toggles and back navigation stay silent.
- **D-10:** Our button sound **replaces the Android system tick** on controls where it plays (suppress `soundEffectsEnabled` click on those controls) so users with OS touch-sounds on never hear a double click. Controls without our sound keep default OS behavior.
- **D-11:** Buttons use a **forward + dismiss pair**: a brighter click for forward/commit actions (Start, Continue) and a softer one for cancel/dismiss. This deliberately extends the asset set to ~12 sounds — still SOUND-04 scope, not a new capability.
- **D-12:** **Exception to toggle silence:** flipping `soundOn` **ON** in Settings plays one soft confirmation pop — instant proof the audio works. Turning it OFF stays silent (silence is the confirmation). This blip must not fire on the initial Settings-screen state load, only on user-initiated enable.

### Win & star-pop sequencing (SOUND-03)
- **D-13:** With OS animations off (the celebration's `rememberAnimationsEnabled()` gate — stars land instantly), star pops still play on their **own short fixed stagger** (~150 ms apart, rising pitch). The audio celebration survives reduced-motion settings.
- **D-14:** The **win sound fires at the moment the winning move lands on the board** (final jump), then the star pops play with the Results celebration as stars land. Two-beat structure: instant payoff on the board, ceremony on Results.
- **D-15:** Star-pop pitches are a **ladder prefix**: pops always start at the same base note and climb, so a 2-star result plays notes 1–2 and the top/resolving note is audibly reserved for a 3-star finish.
- **D-16:** Celebration audio (win + pops on Results) plays **once per win** — mirroring the existing `recordSolvedPlayOnce()` play-once idiom. Re-entering the same Results (process-death restore, back navigation) shows stars silently.

### Claude's Discretion
- Audio engine choice (SoundPool is the obvious fit for low-latency one-shots with per-stream `setRate` pitch control — confirm at research), file format (OGG vs WAV), preload/warm-up strategy for "first tap never silent", and stream-count limits.
- Exact pitch step size, base pitch, and the invalid-tap character ("quiet, non-punishing" per SOUND-02) — tune by ear within the candy aesthetic.
- Relative volume balancing across the set (invalid tap noticeably quieter than jump; win louder but not startling).
- Architecture placement: whether sound triggering follows the `JumpHaptic` UI-effect pattern (composable keyed on state transitions) or a dedicated player owned at a different layer — subject to the SOUND-05 constraint that queued one-shots must be stoppable immediately when `soundOn` flips off, and SOUND-06's requirement to use game/media audio attributes without requesting audio focus.
- How the same-peg run counter and its undo mirroring integrate with existing move/undo state (likely derivable from `MoveTransition`/move history rather than new domain state — planner decides; keep `:domain` purity).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & scope
- `.planning/REQUIREMENTS.md` — SOUND-01..SOUND-06 (lines under "Sound") are this phase's entire requirement set.
- `.planning/ROADMAP.md` — Phase 7 goal + the four success criteria (pitch-varied jumps & first-tap-never-silent; win/star sync; immediate `soundOn` silence; media-volume no-duck).
- `.planning/PROJECT.md` — project constraints and Key Decisions table.

### Design language
- `docs/ui/DESIGN-SYSTEM.md` — the Candy Clay design system the sound aesthetic must match; its Results section defines the celebration motion the star pops sync to.

### Existing code this phase touches
- `app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt` — `soundOn: Flow<Boolean>` already persisted (SET-01, "no audio engine yet" — this phase delivers that engine).
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — `JumpHaptic` (fires off `state.animating` transitions, gated by a UiState flag): the established per-event side-effect pattern and the model/anchor for jump/removal/selection sound triggers.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` + `GameUiState.kt` — move/undo/hint/invalid-tap handling; `hapticsOn` shows how a settings flag is folded into UiState.
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsCelebration.kt` + `ResultsScreen.kt` — star stagger (`ClayAnim.STAR_STAGGER_MS`), the `rememberAnimationsEnabled()` reduced-motion gate (D-13), and the play-once idiom (D-16).
- `app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt` — animation timing constants the audio stagger should reference, not duplicate (no-magic-numbers rule).

### Project standards
- `CLAUDE.md` — no magic numbers (pitch steps/volumes/stagger become named constants), dimens rule, detekt/ktlint gates, Gitflow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`soundOn` flow** already exists in `SettingsRepository` with the same shape as `hapticsOn` — wiring it into `GameUiState` can copy the `hapticsOn` collection verbatim.
- **`JumpHaptic` pattern** — a small composable keyed on `state.animating?.id` firing a one-shot side effect, gated by a UiState flag. Direct template for per-event sounds on the game screen.
- **`ClayAnim` constants object** — the star stagger and entrance easings live here; star-pop timing should read these, not re-declare them.
- **Seam pattern** (`interface` + impl + `@Binds` Hilt module in `di/PlatformModule.kt`) — if a `SoundPlayer` abstraction is introduced, it should follow this established shape so tests can bind a no-op.

### Established Patterns
- **State hoisting / stateless composables** (CLAUDE.md + compose-skill): sound triggers driven from state transitions, not buried in composable internals.
- **Domain purity is CI-enforced** (`DomainPurityTest`): nothing audio-related may enter `:domain`/domain packages; the run-counter logic, if it needs domain data, derives from existing move history.
- **All gates run in `./gradlew check`** (detekt, ktlint, no-dp/sp gate, purity, ~299 JVM tests) and must stay green and offline.
- **Reduced-motion respect**: the celebration already gates on OS animator-duration-scale — the audio design (D-13) deliberately decouples pops from that gate.

### Integration Points
- `GameScreen`/`GameUiState` — jump, removal, selection, undo, invalid-tap sounds hang off existing state transitions (`animating`, `selected`, `canUndo` deltas, rejected-tap handling in `onCellTap`).
- `ResultsScreen` — star pops sync to the existing staggered star entrance; win-at-final-jump (D-14) hooks the win-detection moment in `GameViewModel`/`GameScreen` before navigation.
- `SettingsScreen`/`SettingsViewModel` — the enable-blip exception (D-12).
- `App`/DI — wherever the player is initialized/preloaded so the first tap of a session is never silent.

</code_context>

<specifics>
## Specific Ideas

- The pitch ladder is the personality of the phase: same-peg runs climb without a cap (D-06/D-07) and undo walks the ladder back down (D-08) — the sound literally mirrors the board.
- The top star-pop note is "earned": only a 3-star finish ever plays it (D-15).
- Review phrase for SOUND-06: *start a podcast, play a full level — the podcast never dips.*

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. (Background music, lose/stuck stingers, and per-board soundscapes were never raised and remain out of scope; the requirement set is SFX-only.)

</deferred>

---

*Phase: 7-Sound*
*Context gathered: 2026-08-28*
