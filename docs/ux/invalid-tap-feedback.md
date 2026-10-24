---
role: ux-designer
kind: amendment to docs/ux-spec.md (phase 15)
status: draft — fold into docs/ux-spec.md after feature/phase-15-ux-refresh merges
created: 2026-09-29
base-spec: feature/phase-15-ux-refresh @ d017711, docs/ux-spec.md (3,730 lines; § numbers below are that file's)
code-read-at: feature/phase-15-ux-refresh @ d017711; feature/audio-review @ c5b02ef (develop 3f2af90 + audio docs)
trigger: .planning/reports/audio-review/sound-designer.md, finding 1 / P1
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md ("Candy Clay")
---

# Wrong-tap feedback — amendment to the UX spec

> **Why this file exists.** On the gameplay board, a tap that changes nothing plays
> `SoundCue.InvalidTap` and does nothing else: no visual, no haptic (`GameScreen.kt:169`,
> `:377-382` on phase 15; the board state does not change, `GameViewModel.kt:819-823`). That fails
> rules/audio.md ("sound is never the only feedback"). The old cue was also inaudible on a phone
> speaker (−59.4 LUFS through the sound-designer's stand-in). The founder picked a new "soft tok"
> for the sound (Q-008). **This amendment adds the visual, the haptic and the screen-reader
> announcement**, and it rules on the tutorial's jump haptic.
>
> The spec of record, `docs/ux-spec.md`, exists only on the unmerged `feature/phase-15-ux-refresh`.
> So this is a standalone file, keyed to that file's section numbers. §11 below lists every edit
> to fold in after the merge. Nothing here reopens a phase-15 decision. Where a phase-15 sentence
> becomes untrue, it gets a dated pointer, the house style of §3.
>
> **Two corrections of fact to the phase-15 spec, found while tracing:**
> 1. §11 "Sound is never the only feedback" says "the invalid tap has the dots staying put". That
>    is not feedback: nothing on the board changes. It is the gap this file closes.
> 2. §4.4 class 2 says a non-progressing tutorial tap makes "the legal dots re-pop". **The build
>    does not do this.** `TutorialViewModel.registerNonProgressingTap` (`:267-274`) only counts the
>    tap and re-arms the nudge. It changes no state the canvas draws, and the dot pop-in is keyed on
>    `state.selected` alone (`BoardCanvas.kt:159-169`). §4 below builds it with the same mechanism
>    the game uses.

---

## 1. Which taps are wrong — traced in the code

### 1.1 The gameplay board

A tap reaches the ViewModel only when `BoardGeometry.positionAt` (`BoardGeometry.kt:77-85`) maps it
to an **in-play** cell inside that cell's inscribed hit circle. `GameViewModel.onCellTap`
(`GameViewModel.kt:797-825`, identical on develop and phase 15) then takes the first branch that
matches:

| # | The player taps | Code | Wrong? | What they get today |
|---|---|---|---|---|
| N1 | the held peg again | `:807-814`, deselect | **No.** It is the put-down (D-05a) | ring and dots vanish, `Deselect` cue |
| N2 | a legal destination while a peg is held | `:815-816`, `performJump` | **No** | slide, removal fade, jump haptic, jump pair |
| N3 | any peg while nothing is held, **including a peg with no legal jump** | `:817-818`, `select` | **No.** It is a selection with zero dots, the same as tutorial class 1 | ring, breathing pulse, dots (or none), `Select` cue |
| N4 | off the footprint, outside the grid, or a cell's dead corner | never emitted; `positionAt` returns `null` | **No event at all** | nothing (unchanged, see D-06d) |
| **W1** | an **empty hole** while **nothing** is held | `:823`, `else` | **Yes** | `InvalidTap` cue only |
| **W2** | a **different peg** while one is held | `:823`, `else` | **Yes** (the locked tap rule, §4.1 "Tap semantics") | `InvalidTap` cue only |
| **W3** | an **empty hole that is not a legal destination** while a peg is held | `:823`, `else` | **Yes** | `InvalidTap` cue only |

Notes:
- The reducer's comment and `docs/audio-spec.md`'s cue row also name "an out-of-play cell" as a
  rejected tap. The reducer would count one, but **the UI can never deliver one**: `positionAt`
  filters it out first (`it in spec`, `BoardGeometry.kt:83`). W1–W3 are the complete set of wrong
  taps a player can make.
- The rescue banner overlays the board, but taps outside it still reach the board (D-08). W1–W3
  apply there as everywhere else. The two dialogs are modal, so the board takes no taps behind
  them.
- A tap during a jump's 200 ms slide is judged against the already-committed board, and nothing is
  held by then. So a tap on the jumped-over cell (still drawn fading, already empty in state) or on
  the cell the jumper just left is a W1. A tap on the landing cell selects the peg there (N3).

### 1.2 The tutorial board

`TutorialViewModel.onCellTap` (`TutorialViewModel.kt:135-149`) uses the same branch order, by
ruling (§4.1 "Tap semantics"). Its `else` is `registerNonProgressingTap` (`:147`, `:267-274`):

| # | The player taps | §4.4 class | What the build does today |
|---|---|---|---|
| T-N3 | a peg with no legal jump, while nothing is held | class 1 | selects, zero dots, silent |
| T-W1 | an empty hole while nothing is held | class 2 (§4.1 table, "anything else") | **nothing visible**; count + 1, nudge re-armed |
| T-W2 | a different peg while one is held | class 2 | **nothing visible**; count + 1 |
| T-W3 | a hole that is not a destination while a peg is held | class 2 | **nothing visible**; count + 1 (§4.4 promised a dot re-pop) |
| — | the 3rd non-progressing tap, or 4 s idle | class 3 | coach: hint ring + dot, one slab pulse if animations are on (`TutorialScreen.kt:236-243`), release clause |

The tutorial plays no sound for any of these (D-01h as amended by D-01i). So rules/audio.md is
not engaged there: no cue is left without a partner, because no cue plays.

---

## 2. Decisions (fold into §3)

A new group, **"Gameplay board (SOUND-02, SET-02)"**, goes after "The toolbar". It uses `D-06`
because D-01 to D-05 are taken. One tutorial decision, `D-01j`, goes after D-01i. The ids are
proposals. If phase 15 has taken them by the time this is folded in, renumber them and keep the
text.

- **D-06:** **A wrong tap on the gameplay board is answered on four channels: sound, sight, touch
  and speech.** The **sound** is the existing `InvalidTap` cue, now the Q-008 "soft tok", fired
  exactly as today. The **sight** is a short horizontal **head-shake of the tapped cell**. While
  a peg is held, one **redirect** plays with it, pointing where the next tap should go (§3.1). The
  **touch** is a light *reject* haptic, which is not the jump's tick (D-06b). The **speech** is a
  polite screen-reader announcement (D-06c).
  — rules/audio.md requires a non-audio partner. A shake is the one motion that means "no"
  without words, colour or a failure state. It is also the Candy Clay "squishy spring" language
  applied to the thing that was tapped, and it keeps SOUND-02's "quiet, non-punishing": no red,
  no error role, no text on the board, nothing blocked.

- **D-06a:** **Reduced motion swaps the shake for a still mark, never for nothing.** When
  `rememberAnimationsEnabled()` is `false` (`Clay.kt:209`), the tapped cell gets a **dashed ring**
  in `onSurfaceVariant`. It appears in the same frame as the tap, holds for
  `GameAnim.WRONG_TAP_MARK_HOLD_MS` (450 ms) and disappears with no fade. There is no redirect,
  because the held ring and its dots are already on screen and still. — rules/compose.md
  ("animation respects reduced motion") and "never colour alone": the mark is a shape and a
  pattern. A static mark is used because, with the animator duration scale at 0, a fade or a
  shake finishes before its first frame, so an animated fallback would show nothing.

- **D-06b:** **The wrong tap buzzes with the platform's *reject* haptic, which is never the jump's
  tick.** It fires once per wrong tap, only while Settings › Haptics is on (the `hapticsOn` gate
  the jump already reads), and only on API 30 or later. Below API 30 there is no haptic, and no
  fallback. — The jump fires `ContextClick` (`GameScreen.kt:304-314`). If a wrong tap fired the
  same type, success and failure would feel identical, which is worse than no haptic. Below
  API 30 the platform has no reject constant. The constants that do exist there are either a
  single tick like the jump's (`CONTEXT_CLICK`, `KEYBOARD_TAP`, `VIRTUAL_KEY`) or `LONG_PRESS`,
  which is far heavier than a jump and would feel punishing. *Taste check queued as Q-012 (§10),
  non-blocking; the buzz is the built default.*

- **D-06c:** **A wrong tap is announced to a screen reader, politely, in the game only.** There
  is one short sentence per wrong-tap kind (§6). It adds no focus stop, it is spoken for every
  wrong tap including an identical repeat, and it is never re-read on later focus. — A player who
  cannot see the shake gets the one fact that matters: the tap did not take, and why. The tutorial
  announces nothing (D-01c: "nothing is announced as wrong").

- **D-06d:** **Only W1, W2 and W3 are wrong taps.** A put-down, a jump and a selection (even of a
  peg that cannot move) are successful actions, and none of the four channels fires for them. A
  tap that lands on no in-play cell (N4) is **not an event**: the canvas never emits it, and no
  feedback of any kind is added for it. — There is no cell to shake. The player was aiming at
  something on the board, not asking to move, and feedback in empty space would be noise. The
  tap rule itself (§4.1 "Tap semantics", locked by quick task `260628-n4f`) is **not reopened**:
  W2 stays a no-op, and the feedback only makes that no-op visible.

- **D-01j** *(tutorial)*: **(1) The tutorial's jump buzzes like the game's jump.** A completed
  tutorial jump fires the same `ContextClick` haptic at the start of the slide, under the same
  Haptics setting. The asymmetry the sound-designer found is **not intended**. It is a leftover of
  `JumpHaptic` staying `private` to `GameScreen` when `JumpSound` was lifted for D-01i. Phase 15
  never ruled on haptics: §16's SET-02 row reads "Untouched". D-01i's fidelity argument ("what the
  player learns is literally the mechanic they will use") applies unchanged: a tutorial that
  teaches for thirty seconds that a jump is felt by nothing, then buzzes on the first real board,
  is the divergence D-01i refused for sound. The effect is **lifted, not re-implemented**, exactly
  as `JumpSoundEffect` was. **(2) The tutorial's wrong taps get the redirect and nothing else:** no
  shake, no mark, no haptic, no sound, no announcement. That builds §4.4 class 2's promised
  re-pop, refined in §4, and keeps D-01c whole: the redirect is assistance ("look here") and
  never correction ("no").

---

## 3. The gameplay wrong tap, specified (fold in as new §4.4.2)

### 3.1 With animations on

**The shake — every wrong tap (W1, W2, W3).**

| Property | Value |
|---|---|
| What moves | the tapped cell's own drawing only: the **peg sphere and its specular dot** (W2) or the **hole dimple, both tones** (W1, W3). The jumper overlay, the removal overlay, hint rings and destination dots never shake |
| Axis | horizontal only, the same direction in LTR and RTL. It never scales: the scale channel already means selection (breathing) and landing (squash) |
| Amplitude A | `BoardMetrics.WRONG_TAP_SHAKE_RATIO` = **0.10 × cellSize**: ≈ 4.2 dp on a 7×7 board on a 360 dp phone, ≈ 3.3 dp at 9×9, ≈ 6.4 dp on the 480 dp tablet cap |
| Duration | `GameAnim.WRONG_TAP_SHAKE_MS` = **270 ms**, inside the design system's 150–300 ms micro-interaction band |
| Keyframes (time → offset) | 0 ms → 0 · 46 ms → +1.0 A · 105 ms → −0.75 A · 165 ms → +0.45 A · 219 ms → −0.2 A · 270 ms → 0. A decaying wobble of about 2½ swings |
| Easing | `FastOutSlowInEasing` (= `GameAnim.JUMP_SLIDE_EASING`) on every segment; no new curve |
| Start | the first frame after the state change that records the wrong tap; tap to first displaced frame ≤ 100 ms |

**The redirect — only while a peg is held (W2, W3).** Exactly one of these plays, starting in the
same frame as the shake:

| Case | Redirect | Why |
|---|---|---|
| **W2** — a different peg | **ring nudge** on the held peg | The player wanted the other peg. The answer is "you are still holding this one; tap it to put it down" (§4.1 reason 3) |
| **W3**, Show-moves **on**, held peg has ≥ 1 destination | **dots re-pop**: the held peg's destination dots replay their pop-in from `GameAnim.DOT_POP_START` with `ClayAnim.overshootSpring()`, the same animation as on selection (`BoardCanvas.kt:159-169`) | The answer is "these are the places it can go" |
| **W3**, Show-moves **off**, or zero destinations | **ring nudge** | Re-popping dots the player has hidden would turn a wrong tap into a free hint. With zero destinations there is nothing to pop |

**Ring nudge:** the held peg's selection ring (`primary`, `peg_selected_ring_width`, radius
`SELECTED_RING_RATIO`) grows to `BoardMetrics.WRONG_TAP_RING_NUDGE_SCALE` = **1.18×** over
`GameAnim.WRONG_TAP_RING_NUDGE_OUT_MS` = **110 ms**, then returns to 1.0× over
`GameAnim.WRONG_TAP_RING_NUDGE_BACK_MS` = **150 ms**, both `FastOutSlowInEasing`. It multiplies
the breathing pulse and does not replace it. The peg itself does not change size.

W1 has no redirect: nothing is held, and pointing at a movable peg would be a hint.

### 3.2 With animations off (D-06a)

| Channel | Behaviour |
|---|---|
| Shake | **replaced** by the **wrong-tap mark**: a dashed ring around the tapped cell, `BoardMetrics.WRONG_TAP_MARK_RING_RATIO` = 0.40 × cellSize (outside the peg's 0.34 and the hole's 0.16, so it sits on the slab), `BoardMetrics.WRONG_TAP_MARK_DASHES` = 12 equal dashes at 50 % duty, stroke `wrong_tap_mark_width` (3 dp), colour **`onSurfaceVariant`**. It appears whole in the tap's frame, holds **450 ms**, and disappears whole. It is drawn above the cell and below any hint ring |
| Redirect | **none.** The held ring and the dots are already drawn, and neither moves. A ring that blinks thicker for 450 ms would be a flash, which is what a reduced-motion player has asked not to see |
| Haptic, sound, announcement | unchanged. None of them is motion |

The mark's colour pairs are **5.3:1** against the light slab (`#635F69` on `surfaceContainerHigh`
`#F1EAFB`) and **5.6:1** in dark (`#A79FB5` on `#2E2740`), both above the 3:1 graphics floor. The
dashes separate it from the solid `primary` selection ring and the solid `tertiary` hint rings,
so colour is never the only difference.

### 3.3 Timing and interruption

1. **Never blocks input.** The board takes taps throughout. A legal tap 50 ms after a wrong one
   acts normally.
2. **A second wrong tap restarts the feedback on the new cell**, at t = 0. The previous cell is
   drawn at rest in the same frame. A second wrong tap on the same cell restarts it the same way.
3. **A successful action cancels it at once.** On a selection, a put-down, a jump, an undo that
   rewinds, a hint reveal, a restart, or any board install, any running shake, nudge or mark ends
   in that frame and the cell is drawn at its true centre. So a jump's slide never starts from a
   displaced peg.
4. **Draw phase only.** The shake is a draw-time offset. No layout bounds, board slab size or
   neighbouring cell changes.
5. **A read-only canvas never draws any of it** (`interactive = false`: the Home stage and the
   catalogue previews).

### 3.4 Haptic (D-06b)

| Property | Value |
|---|---|
| Type | the platform's reject haptic: `HapticFeedbackConstants.REJECT` (API 30), reached via Compose's `HapticFeedbackType.Reject`. The developer confirms that the pinned BOM maps it to that constant |
| When | once per wrong tap, at the tap, from a **screen-level** effect keyed on the wrong-tap ordinal (with the `> 0` guard, like the cue effects) |
| Gates | `hapticsOn` (SET-02) **and** `Build.VERSION.SDK_INT >= 30`, checked in our own code, so a library fallback below 30 can never run |
| Below API 30 | nothing. Never `ContextClick`, `LongPress`, `KeyboardTap` or `VirtualKey` |
| System setting | honoured the way the jump haptic honours it: no ignore-global-setting flag |
| Relation to the jump | a different type, the same gate, and never on the same tap (a tap is either a jump or a wrong tap). Both stay light: the jump is one short tick and the reject is two short pulses. Nothing in the game uses a `LongPress`-weight haptic |

### 3.5 Sound

Unchanged: `InvalidTapSound` on `rejectedTapTick` (`GameScreen.kt:377-382`). The asset is the
sound-designer's (Q-008 "soft tok", candidate a). **The visual and the haptic never depend on the
sound setting**: they fire with Sound off and at zero media volume, which is the case they exist
for.

### 3.6 Screen reader (D-06c)

| Wrong tap | Announcement (new strings, §6) |
|---|---|
| W1 | `wrong_tap_empty_hole` — "Empty hole. Pick up a peg first." |
| W2 | `wrong_tap_other_peg` — "Already holding a peg. Tap it again to put it down." |
| W3 | `wrong_tap_not_a_destination` — "Can't jump there." |

- **Polite**: it waits for speech in progress and never interrupts it.
- **Every wrong tap is spoken once**, including the second of two identical taps in a row. A live
  region announces only when its content *changes*, so an identical repeat is silently dropped
  unless the developer handles it.
- **No new focus stop.** Gameplay traversal stays back → title → timer → board → dock → banner
  (§11). A later swipe onto the board does not re-read the message.
- Spoken only while a screen reader runs. Nothing changes for anyone else.
- `View.announceForAccessibility` is deprecated from API 36. The phase-15 grep found no live
  region anywhere in the app, so this is the first. The mechanism is the developer's; the
  properties above are the contract.
- **A known limit, recorded and not fixed here:** the board is one `Canvas` with no per-cell
  semantics (`BoardCanvas.kt:196-212`). A TalkBack user therefore cannot aim a tap at a cell in the
  first place. The announcement is correct, and it is cheap to ship now. It becomes fully useful
  once the board gains per-cell semantics, which is a separate accessibility task handed to the
  producer (report, Open issues).

### 3.7 What the UI state must carry (fold into §7)

`rejectedTapTick` records *that* a wrong tap happened, not *where* (`GameUiState.kt:79-83`). The
shake needs the **tapped cell**. So the board's UI model must carry, for each wrong tap, an
**ordinal and the tapped `Position`**.
- The kind (W1/W2/W3) can be derived from the board and `selected` at that moment, because a
  wrong tap changes neither.
- The ordinal follows the house tick rules: it is carried forward, never reset, at all four
  board-install sites, with a `> 0` guard at every consumer.
- The simplest shape pairs the existing `rejectedTapTick` with the cell. That choice, and the
  field names, are the developer's.

### 3.8 Where each channel lives (negative contract, fold into §5)

- **`BoardCanvas` draws the shake, the mark and the redirect, and nothing else.** It gains no
  `SoundCue`, no haptic call and no announcement. Those are screen-level effects beside
  `GameScreen`, like the cue effects. The canvas is shared with the tutorial, which must not
  inherit them.
- The canvas needs to be told whether to draw the **correction** (the shake or the mark). It is on
  for `GameScreen` and off for `TutorialScreen` and for every read-only use. The **redirect** is
  drawn whenever a wrong tap is recorded while a peg is held. The mechanism, for example a
  parameter beside `interactive`, is the developer's.

---

## 4. The tutorial (fold into §4.1 and §4.4, D-01j)

**Wrong taps — §4.4 class 2 row gains this dated note:** *(Built and refined 2026-09-29, D-01j.)*
The re-pop is driven by the same wrong-tap signal as the game (§3.7), with the correction off.

| Tap | Answer (animations on) | Animations off |
|---|---|---|
| T-W3, a hole that is not a destination, peg held | the held peg's **dots re-pop** (`showHints` is forced on, D-01b, so they are always drawn). With **zero** dots (an immovable peg held, class 1 then a stray tap), the **ring nudges** | nothing new; ring and dots are already static |
| T-W2, a different peg, peg held | the **ring nudges**. This refines §4.4's "the legal dots re-pop" for this one kind: pointing at the held peg teaches the put-down, which §4.1 reason 3 says the extra tap should teach. Criterion 3a stays true: the selection and the dots do not change | nothing new |
| T-W1, an empty hole, nothing held | nothing new. The class-3 escalation (3 taps or 4 s) is the designed answer, unchanged | unchanged |

In every row: **no shake, no mark, no haptic, no sound, no announcement** (D-01c, D-01i). The
count, the nudge timer, the escalation and the release clause are all unchanged.

**The jump the player feels — new paragraph in §4.1, after "The jump the player hears":** a
completed tutorial jump fires `ContextClick` at the start of the slide, keyed on
`state.board.animating?.id`, only while Settings › Haptics is on. This is the game's `JumpHaptic`
**lifted to a shared `internal` composable**, beside `JumpSoundEffect` and called once from
`TutorialScreen`. **Re-implementing it is a defect**, on the same reasoning D-01i gives for the
sound. §5.1's four prohibitions are not engaged. The effect reads a move id and a Boolean and
touches no `GameViewModel`, repository or navigation.
- **Open for the tech-lead:** how the tutorial reads the persisted `hapticsOn`.
  `TutorialViewModel` is pinned to exactly two collaborators by a reflection test. A settings
  read is not a progress dependency, so §5.1 does not forbid it, but the pin or the route must
  change. With OS animations off, the same snap-path race criterion 7c names for the sound
  applies to the haptic, so it is checked on a device (WT-15).

---

## 5. Tokens (fold into §6.1, §6.2, §6.4 and §10)

**No new colour, no gradient, no hex, no asset.**

| Token | Home | Value | Note |
|---|---|---|---|
| `wrong_tap_mark_width` | `dimens.xml`, stroke group | 3dp | the same value as `peg_selected_ring_width`, but its own token (one token per owner, the file's own rule), and the same documented sub-4dp stroke exception |
| `WRONG_TAP_SHAKE_RATIO` | `BoardMetrics` | 0.10f | × cellSize |
| `WRONG_TAP_RING_NUDGE_SCALE` | `BoardMetrics` | 1.18f | × the ring radius |
| `WRONG_TAP_MARK_RING_RATIO` | `BoardMetrics` | 0.40f | × cellSize; its own constant, although it equals `SELECTED_RING_RATIO` today |
| `WRONG_TAP_MARK_DASHES` | `BoardMetrics` | 12 | equal dashes, 50 % duty |
| `WRONG_TAP_SHAKE_MS` | `GameAnim` | 270 | keyframe fractions 0.17 / 0.39 / 0.61 / 0.81 / 1.0 at amplitudes +1.0 / −0.75 / +0.45 / −0.2 / 0, kept as named constants beside it |
| `WRONG_TAP_RING_NUDGE_OUT_MS` / `_BACK_MS` | `GameAnim` | 110 / 150 | |
| `WRONG_TAP_MARK_HOLD_MS` | `GameAnim` | 450 | **functional, not decorative**: it applies precisely when animations are off |

**§6.2 colour rows to add:** wrong-tap mark → `onSurfaceVariant`; ring nudge → `primary` (the
existing selection-ring role); dot re-pop → `primary` at `DESTINATION_DOT_ALPHA` (existing).

**§10 rows to add:** "Wrong tap, gameplay" (shake plus redirect, §3.1); "Wrong tap, tutorial"
(redirect only, §4). Add a reduced-motion bullet: the shake, the nudge and the re-pop do not run;
the gameplay mark (§3.2) holds 450 ms; the tutorial shows nothing new.

---

## 6. Copy (fold into §8 "New strings")

| Key | Text | Where |
|---|---|---|
| `wrong_tap_empty_hole` | Empty hole. Pick up a peg first. | announced on W1, gameplay only |
| `wrong_tap_other_peg` | Already holding a peg. Tap it again to put it down. | announced on W2, gameplay only |
| `wrong_tap_not_a_destination` | Can't jump there. | announced on W3, gameplay only |

The tone is neutral and actionable. There is no "wrong", "error" or "invalid", and none of the
three names a failure. "Pick up" and "put down" are the vocabulary the code and D-05a already use
for selection.

---

## 7. Accessibility (fold into §11)

- **"Sound is never the only feedback"**: replace "the invalid tap has the dots staying put" with
  "the invalid tap has the shake of the tapped cell (a dashed mark with animations off), a
  redirect while a peg is held, the reject haptic, and a screen-reader announcement (D-06)".
- **"State, not just colour"** table, new row: *Wrong tap* → **motion** (shake) or **shape and
  pattern** (the dashed mark) + **position** (on the tapped cell) + haptic + announcement. There
  is no colour-only channel.
- **Contrast**: the mark's pairs are 5.3:1 (light) and 5.6:1 (dark), ≥ 3:1. Add them to the
  criterion-27 device check.
- **Touch targets**: unchanged (the board's hit-testing is untouched).
- **Screen-reader traversal**: unchanged. D-06c adds no focus stop.
- **Known gap** (not introduced here): the board has no per-cell semantics (§3.6).

---

## 8. Acceptance criteria (fold into §14 as 29 onward, under a new "Wrong tap (D-06, D-01j)" group)

Each criterion names how it is checked: **unit** = a JVM test of a ViewModel, a reducer or a pure
curve; **UI** = a Compose/Roborazzi test on a controlled clock; **device** = the qa-engineer on a
phone.

- **WT-1** *(unit)* — **Given** nothing is held, **when** the player taps an empty in-play hole,
  **then** the wrong-tap ordinal advances by exactly 1 and carries that cell, and `rejectedTapTick`
  advances by exactly 1.
- **WT-2** *(unit)* — **Given** peg A is held, **when** the player taps peg B, **then** the signal
  carries B, `selected` is still A and `legalTargets` is unchanged.
- **WT-3** *(unit)* — **Given** peg A is held, **when** the player taps an empty hole not in
  `legalTargets`, **then** the signal carries that hole and the board is unchanged.
- **WT-4** *(unit, negative)* — A put-down, a legal jump, and a selection of a peg with zero legal
  jumps each leave both the signal and `rejectedTapTick` unchanged.
- **WT-5** *(unit)* — `BoardGeometry.positionAt` returns `null` for a tap on a blocked
  coordinate, outside the grid, and in a cell's dead corner. So no feedback of any kind follows
  (D-06d; pins the existing behaviour).
- **WT-6** *(unit)* — The signal's ordinal is carried across all four board-install sites, and a
  state built fresh at 0 fires no shake, haptic or announcement on first composition.
- **WT-7** *(unit on the offset curve, or UI on a paused clock)* — With animations on, a wrong tap
  displaces only the tapped cell's drawing, horizontally, by +1.0 A at 46 ms, −0.75 A at 105 ms,
  +0.45 A at 165 ms, −0.2 A at 219 ms and 0 at 270 ms (± 1 px, ± 1 frame), where A = 0.10 ×
  cellSize. No other cell moves, and the canvas's bounds do not change.
- **WT-8** *(UI)* — W2: the tapped peg shakes, the held ring reaches 1.18× within 110 ms and 1.0×
  by 260 ms, and no dot re-pops.
- **WT-9** *(UI)* — W3 with Show-moves on and ≥ 1 destination: the hole shakes and the held peg's
  dots replay from `DOT_POP_START`. With Show-moves **off**: no destination dot is drawn in any
  frame, and the ring nudges. With zero destinations: the ring nudges.
- **WT-10** *(UI)* — With `rememberAnimationsEnabled()` false: no frame shows a displacement, a
  ring-radius change or a dot re-pop. The dashed mark (12 dashes, `onSurfaceVariant`,
  `wrong_tap_mark_width`, 0.40 × cellSize) is drawn in the tap's frame, is still drawn at 440 ms,
  and is gone at 460 ms with no intermediate alpha.
- **WT-11** *(UI)* — A second wrong tap mid-shake restarts the feedback on the new cell. A
  selection, put-down, jump, undo, hint reveal, restart or board install cancels it in the same
  frame, and a jump's slide starts from the cell's true centre. A legal tap 50 ms after a wrong
  tap acts normally.
- **WT-12** *(UI, fake `HapticFeedback`)* — With Haptics on at API ≥ 30: exactly one `Reject` per
  wrong tap, and no haptic on select or put-down. With Haptics off: none. A wrong tap never
  produces `ContextClick`. *(device)* On an API < 30 phone, or the Robolectric equivalent: no
  haptic on a wrong tap.
- **WT-13** *(UI)* — With Sound off (`soundOn = false`), W1, W2 and W3 still shake and still
  buzz. `InvalidTap` fires exactly as before this change when Sound is on.
- **WT-14** *(device, TalkBack on)* — W1, W2 and W3 speak their §6 sentence once each, politely.
  The second of two identical wrong taps is spoken too. Swiping through the gameplay screen
  afterwards finds no new focus stop and does not re-read the sentence. With TalkBack off,
  nothing is spoken.
- **WT-15** *(UI + device)* — In the tutorial, with Haptics on, each completed jump fires exactly
  one `ContextClick` as its slide starts; with Haptics off, none. On a device, check once more
  with OS animations off (the 7c race).
- **WT-16** *(UI)* — In the tutorial: T-W3 re-pops the dots (the ring nudges instead when zero
  dots), T-W2 nudges the ring, and T-W1 shows nothing new. On every step there is no shake, no
  mark, no haptic, no sound and no announcement. The class-3 escalation still fires on the 3rd
  tap and after 4 s. `ui/onboarding/` references no `HapticFeedbackType.Reject` and no
  `wrong_tap_*` string (a grep, §13).
- **WT-17** *(UI)* — A `BoardCanvas` with `interactive = false` never draws any wrong-tap visual.
- **WT-18** *(device — the sound-designer's P1 test)* — With media volume 0, then with Sound off,
  on a phone in light and in dark: W1, W2 and W3 are each identifiable from a 60 fps screen
  recording. With animations off, the mark is visible in a screenshot taken within 450 ms of the
  tap. Record the mark's measured contrast against the slab (≥ 3:1).
- **WT-19** *(Roborazzi)* — Golden images of the reduced-motion mark on a hole and on a peg, and
  of the ring at the peak of its nudge, each in light and dark.

---

## 9. Test impact (fold into §13) and traceability (fold into §16)

- **New:** the WT-1…WT-6 unit tests; WT-7 as a test of the shake offset written as a pure
  function of elapsed time, sampled at the five keyframes (preferred over a clock-driven UI test
  because it is exact and fast; the function's name and home are the developer's); UI tests
  WT-8…WT-13, WT-16 and WT-17; Roborazzi WT-19.
- **Extended:** `theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep` stays as it is. The
  §13 grep over `ui/onboarding/` also forbids `HapticFeedbackType.Reject` and `wrong_tap_*`.
- **Kept:** criterion 3a is unchanged (the selection and the dots do not move on T-W2).
- **§16:** **SOUND-02** gains "the invalid tap's non-audio partners, D-06, WT-7…WT-14, WT-18".
  **SET-02** changes from "Untouched" to "the Haptics gate now also covers the wrong tap (D-06b)
  and the tutorial jump (D-01j); WT-12, WT-15".

---

## 10. Founder question — drafted for the producer to queue (non-blocking)

The id is provisional: Q-011 is the newest on `feature/audio-review`, and Q-003 to Q-005 are on
phase 15. **Nothing waits on it.** The buzz is built as the default, and answer 2 costs one gate.

    ## Q-012 — Should a wrong tap also buzz?   (open)
    Date: 2026-09-29    Asked by: ux-designer
    Context: A wrong tap on the board now shakes the tapped hole or peg (a still dashed ring when
    animations are off) and plays the new "soft tok". Android 11+ phones can also give a short
    "reject" buzz that feels different from a jump's tick; the Haptics switch still turns all off.
    Options: 1) buzz on a wrong tap (built by default) 2) no buzz — shake and sound only
    Recommendation: ux-designer — 1: it tells a player who is looking elsewhere, or cannot hear,
    that the tap did not take, and it cannot be mistaken for a jump.
    Blocks: nothing
    Answer:

---

## 11. Fold-in map (after `feature/phase-15-ux-refresh` merges)

| Phase-15 section | Edit |
|---|---|
| Front matter / revision log | an "Eighth revision — 2026-09-29 (the wrong tap)" note, pointing here |
| §3 Decisions | new group "Gameplay board" with D-06 and D-06a–d; D-01j after D-01i; one-line pointers on D-01c (tutorial wrong taps gain a redirect, never a correction) and D-01i (the jump is also felt) |
| §4.1 | "The jump the player feels" paragraph (§4); a pointer under "Tap semantics" that W2 now shows a ring nudge in the game (D-06) |
| §4.4 | class 2 row: the dated note and table of §4; the sentence "During normal gameplay … wrong-tap handling is **unchanged**" gains "*(Amended 2026-09-29 — D-06: it gains a visual, a haptic and an announcement; §4.4.2.)*" |
| new §4.4.2 | §3 of this file |
| §5 | §3.8's two bullets; "No shake, mark, haptic or announcement in the tutorial" |
| §6.1 / §6.2 / §6.4 / §10 | §5 of this file |
| §7 | §3.7 |
| §8 | §6 (three strings) |
| §9 | "No new asset" stands. The haptic is a platform constant, and the sound is the sound-designer's |
| §11 | §7 |
| §13 / §14 | §9 and §8 (WT-1…WT-19 → criteria 29 onward) |
| §15 | Q-012, once the producer has queued it |
| §16 | the SOUND-02 and SET-02 rows (§9) |

---

## 12. Considered and rejected

- **A red or `error` tint pulse on the cell.** It is colour alone, and it is the one role
  SOUND-02's "non-punishing" and D-01c's "no red" rule out.
- **A scale squash or a vertical bounce of the cell.** The scale channel already means selection
  and landing, and a bounce reads as "accepted".
- **A toast or snackbar ("Invalid move").** It is text over a board in play, slow, and it
  competes with the ad snackbar's host.
- **An animated ring flash.** It collides with the selection and hint rings, and with animations
  off a fade never shows.
- **Re-popping the dots on W2.** The player wanted the other peg, and the held peg is the answer.
- **Re-popping hidden dots when Show-moves is off.** That would be a free hint on every wrong tap.
- **`ContextClick` or `LongPress` for the haptic.** The first feels the same as a jump; the second
  is heavy and punishing.
- **A shake, a haptic or an announcement in the tutorial.** Each says "no", which D-01c forbids.
- **Feedback for taps off the footprint.** There is no cell to anchor it to, and the canvas never
  reports them (D-06d).
- **Making W2 re-select.** That is the locked tap rule (§4.1 "Tap semantics") and it is not
  reopened here.
