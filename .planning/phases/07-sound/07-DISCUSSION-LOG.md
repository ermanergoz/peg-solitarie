# Phase 7: Sound - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-28
**Phase:** 7-Sound
**Areas discussed:** Sound aesthetic & asset sourcing, Jump pitch-variation behavior, Button-press coverage, Win/star sequencing edge cases

---

## Sound aesthetic & asset sourcing

| Option | Description | Selected |
|--------|-------------|----------|
| Curated CC0 packs | Free CC0 libraries (Kenney, freesound); executor downloads/trims in-phase; license provenance recorded in-repo | ✓ |
| Synthesized in-pipeline | Procedurally generated WAV/OGG committed to repo; full pitch control, more synthetic character | |
| I'll supply the assets | User commissions/hand-picks; blocks execution until assets land | |

**User's choice:** Curated CC0 packs (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Soft candy-clay pops | Squishy, rounded, playful — matches the Candy Clay redesign | ✓ |
| Classic wooden board | Wood taps, marble clicks — at odds with candy visuals | |
| Neutral UI blips | Clean minimal tones, less character | |

**User's choice:** Soft candy-clay pops (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Audit at verify/UAT | Executor curates full set; user judges by playing the build at verification; duds swapped then | ✓ |
| Shortlist before wiring | 2–3 candidates per event auditioned mid-phase before integration | |
| Full discretion | No dedicated listening review | |

**User's choice:** Audit at verify/UAT (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| One family, patch gaps | Anchor on one cohesive pack; borrow elsewhere only where no fit | ✓ |
| Best sound per event | Cherry-pick per event across sources, accepting tonal variety | |
| You decide | Leave to curator's ear | |

**User's choice:** One family, patch gaps (recommended option)

---

## Jump pitch-variation behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Rising on chains | Pitch steps up across a run, resets after; combo-reward feel | ✓ |
| Random within range | Random pitch in a comfortable band; simplest anti-fatigue | |
| Tied to progress | Pitch rises as pegs deplete across the whole level | |

**User's choice:** Rising on chains (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Quick succession | Any jumps within a short time window climb; pause resets | |
| Same peg only | Pitch rises only while the same peg keeps jumping; different peg resets | ✓ |
| You decide | Implementation judgment | |

**User's choice:** Same peg only — the purist multi-capture-run reading (declined the recommended time-window option)

| Option | Description | Selected |
|--------|-------------|----------|
| Cap after ~4 steps | Ladder holds at a cap so long runs never get squeaky | |
| Keep rising | No ceiling; every run jump steps up | ✓ |
| You decide | Implementation tuning | |

**User's choice:** Keep rising (declined the recommended cap)

| Option | Description | Selected |
|--------|-------------|----------|
| Step back down | Undo steps the ladder back one; ladder mirrors board state | ✓ |
| Full reset | Any undo resets to base pitch | |
| You decide | Implementation judgment | |

**User's choice:** Step back down (recommended option)

---

## Button-press coverage

| Option | Description | Selected |
|--------|-------------|----------|
| Navigational CTAs | Mode buttons, board selection, Results actions, dialog confirms; Undo/Hint keep distinct sounds; toggles/back silent | ✓ |
| Every tappable control | All buttons/toggles/rows click; risks noise fatigue | |
| Minimal — commits only | Only irreversible commits sound | |

**User's choice:** Navigational CTAs (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Replace it | Suppress the OS touch-sound tick on controls that play our sound | ✓ |
| Layer on top | Leave OS behavior; two overlapping clicks possible | |
| You decide | Implementation judgment | |

**User's choice:** Replace it (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Single sound | One candy click for every covered CTA; stays at 11 assets | |
| Forward + dismiss pair | Brighter forward click, softer cancel/dismiss; ~12th asset | ✓ |
| You decide | Use a natural pair if the pack has one | |

**User's choice:** Forward + dismiss pair (declined the recommended single sound)

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, blip on enable | Turning soundOn ON plays one soft pop; OFF stays silent | ✓ |
| No exception | Toggles silent everywhere | |

**User's choice:** Yes, blip on enable (recommended option)

---

## Win/star sequencing edge cases

| Option | Description | Selected |
|--------|-------------|----------|
| Own short stagger | With OS animations off, pops play on a fixed ~150ms rising rhythm anyway | ✓ |
| Single chord | Collapse pops into one combined hit when animations are off | |
| Skip pops, win only | No animation → no sequence | |

**User's choice:** Own short stagger (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Win at final jump | Win sound at the winning move on the board; pops at Results | ✓ |
| All at Results | Fanfare + pops concentrated at Results entry | |
| You decide | Based on transition feel | |

**User's choice:** Win at final jump (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Ladder prefix | Pops start at base and climb; top note reserved for 3-star | ✓ |
| Always resolve | Last star always lands the resolving note regardless of count | |
| You decide | Implementation tuning | |

**User's choice:** Ladder prefix (recommended option)

| Option | Description | Selected |
|--------|-------------|----------|
| Once per win | Celebration audio only the first time that win's Results appears | ✓ |
| Follow the animation | Replay whenever the celebration animation replays | |
| You decide | Match existing animation re-entry behavior | |

**User's choice:** Once per win (recommended option)

---

## Claude's Discretion

- Audio engine choice, file format, preload/warm-up strategy, stream limits
- Pitch step size, base pitch, invalid-tap character, volume balancing
- Architecture placement of the sound player (UI-effect pattern vs dedicated seam), within SOUND-05/06 constraints
- How the same-peg run counter and undo mirroring integrate with existing move state

## Deferred Ideas

None — discussion stayed within phase scope.
