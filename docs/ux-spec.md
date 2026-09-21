---
role: ux-designer
phase: 15
slug: ux-refresh
status: draft
created: 2026-09-21
revised: 2026-09-21
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
---

# Peg Solitaire — UX Specification

> The studio's UX contract for Peg Solitaire (rules/studio.md: the ux-designer owns screen
> inventory, layout, interaction model, feedback, theme and accessibility). It is written for the
> **phase-15 five-part UX refresh** the founder asked for; it specifies the
> surfaces that refresh touches completely and references — never re-specifies — everything
> Phases 1–14 already locked.
>
> **The design system of record is `docs/ui/DESIGN-SYSTEM.md` ("Candy Clay").** Nothing here
> introduces a second visual language. Every colour is an M3 role or a `LocalClayColors`
> gradient; every fixed dp lands in `res/values/dimens.xml` as its own named token; every
> duration/ratio lands in a scoped constants object (`ClayAnim`, `GameAnim`, `BoardMetrics`, or a
> new `TutorialAnim`); every string lands in `res/values/strings.xml`. The `dpSpLiteralCheck`
> gate fails the build on a raw `.dp`/`.sp` literal in a composable
> (`app/src/main/res/values/dimens.xml:2-9`).
>
> **Format note.** The house convention for a *phase* contract is
> `.planning/phases/<NN>-<slug>/<NN>-UI-SPEC.md` (the `09-UI-SPEC.md` template). This document
> lives at `docs/ux-spec.md` because rules/reporting.md and the ux-designer role definition name
> that path as the role's durable output, and because this refresh spans five screens rather
> than one phase's deltas. It follows the `09-UI-SPEC.md` section order so a reader moving
> between the two is never surprised. If the producer wants a phase-scoped copy, this file is
> the source and `15-UI-SPEC.md` is a pointer to it — not a fork.
>
> **Amendments — revised 2026-09-21.** This edition folds in two rulings the first edition had
> deliberately routed out of its own authority:
>
> - `.planning/reports/phase-15/game-designer-tutorial-boards.md` — the tutorial's board content
>   (§7 R-4), now closed: three boards proven dead-end-free to one peg by exhaustive enumeration.
> - `.planning/reports/phase-15/tech-lead-architecture.md` — four architecture rulings: how the
>   tutorial is hosted (D-01f/R-5), how the Home progress values are read (§7 R-1/R-2), how the
>   cold-start gate is measured (§6.5, criterion 15e), and the screenshot-suite question (§13).
>
> Each ruling is authoritative in its own domain and is **integrated, not re-argued**; where one
> forced a change to a screen, the screen's answer is this role's and is stated here. Per the
> house style in §3, no earlier decision is rewritten in place: three of the rulings are filed as
> new decisions — **D-01g** (hosting), **D-01h** (the tutorial's audio) and **D-03h** (the Home
> progress read) — and the fourth, the cold-start **measurement contract**, is a named subsection
> of §6.5 rather than a decision, because it is a gate procedure and not a design choice. Every
> decision they amend carries a one-line pointer (D-01a, D-01f, D-03d, D-03g).
>
> Consequences that follow from the rulings but were not themselves proposed as a diff — the
> tutorial's negative contract in §5.1, the tutorial's audio decision, and the §14 criteria the
> new boards and the new gate make wrong or unverifiable — are applied here too.
>
> **Second revision — 2026-09-21 (audio).** `.planning/reports/phase-15/qa-engineer-tutorial-audio.md`
> — the qa-engineer, standing in as the owner of tests and acceptance because no sound-designer is
> staffed for phase 15 (the recusal is recorded in that report) — reviewed **D-01h** and returned
> **FAIL**. The ruling is folded in here as **D-01i**: the tutorial is *not* silent end to end. A
> completed tutorial jump plays the game's own jump pair, because
> `.planning/REQUIREMENTS.md:38` (SOUND-01, marked Complete) says the player hears a jump sound on
> **every** peg jump and D-01h narrowed that requirement without amending it or recording the
> narrowing anywhere. Silence is upheld for everything else on the board and for the completion
> star, and the gap D-01h left — the completion CTA's and `Skip`'s press cues — is now ruled
> explicitly. The edits reach D-01h (pointer), **D-01i** (new), §4.1, §4.4, §5, §5.1 (clarified,
> not relaxed), §9, §13, §14 (7c replaced, 7d/7e new) and §16, which gains its first SOUND-nn
> rows so this class of contradiction is visible next time.
>
> Out of scope here and **not** absorbed into this document: rules/audio.md makes
> `docs/audio-spec.md` the cue list of record and that file has never existed on any branch. The
> gap predates phase 15, belongs to the sound-designer, and the producer is tracking it
> separately. This spec names cues per screen; it does not stand in for that file.

---

## 1. The founder's five asks, and what each became

The founder's words, verbatim:

> "make the tutorial more interactive. remove the info button that starts the tutorial. Homepage
> could be fancier it looks empty with just 2 buttons. Also during the game, redesign the icons
> on the top. remove toolbar from the game"

| Ask | Decision | Verdict |
|-----|----------|---------|
| Make the tutorial more interactive | **D-01** | The tutorial becomes three micro-puzzles the player actually plays on a live board. No step is advanced by a Next button. |
| Remove the info button that starts the tutorial | **D-02** | `HomeScreen.kt:348-354` is deleted. The replay path moves to a Settings › General row; first-run auto-start is unchanged. |
| Homepage looks empty with just 2 buttons | **D-03** | Home gains a hero block, a three-pill progress ribbon and board art on its tiles. The two mode tiles stay primary and stay in the thumb zone. **Bounded by two hard constraints: three existing readouts are device-test oracles that must stay visible on first paint (D-03f), and Home is on the measured ≤ 950 ms cold-start gate (D-03g).** |
| Redesign the in-game icons on the top | **D-04** | The five controls are re-glyphed, re-grouped and re-homed. Two real accessibility defects are fixed in the process. |
| Remove the toolbar from the game | **D-05** | The `TopAppBar` at `GameScreen.kt:502` is deleted. It is the **same surface** as the "icons on the top", so D-04 and D-05 are one change — see D-05 in §3. |

---

## 2. Screen inventory

| # | Screen | Status this refresh | Files |
|---|--------|---------------------|-------|
| 1 | **Tutorial** | **Rewritten.** Was a static illustrated sheet; becomes an interactive coached micro-board flow. Promoted from "a scrim+sheet over Home" to a screen in its own right — and, since D-01g, to a nav destination of its own. | `ui/onboarding/OnboardingOverlay.kt` (replaced), new `ui/onboarding/Tutorial*.kt` incl. `TutorialViewModel`, a `Tutorial` entry in `Routes.kt` + `AppNavHost.kt`, and `object TutorialBoards` in `:domain` |
| 2 | **Home** | **Recomposed.** Top bar deleted; hero, progress ribbon and tile art added; mode tiles unchanged in behaviour. | `ui/home/HomeScreen.kt`, `ui/home/ModeGlyphs.kt` |
| 3 | **Gameplay** | **Chrome replaced.** `TopAppBar` deleted; a status strip and an action dock take its place. Board, rules, animation, dialogs, banner, snackbar all unchanged. | `ui/game/GameScreen.kt`, `ui/game/{Hint,HintToggle,Undo,Restart}Control.kt` |
| 4 | **Settings** | **One row added** (How to play) + one stale supporting line fixed. | `ui/settings/SettingsScreen.kt` |
| 5 | Classic catalog | **Unchanged.** | — |
| 6 | Results | **Unchanged.** | — |

**Deliberately unchanged:** every rule, every **shipped** board footprint, the scoring/star model,
the jump/removal animation, the rescue banner, the ad-offer dialog, the restart confirm dialog,
the Play Games opt-in prompt, and the profile-swap dialog. This refresh is a presentation change
everywhere except one place: the tutorial gains three **new** teaching miniatures
(`object TutorialBoards`, §4.1), which live outside `Boards.CLASSIC_SPECS` and change no existing
board, no catalogue entry and no number the player has already earned.

---

## 3. Decisions

House style per `.planning/phases/*/[0-9]*-CONTEXT.md`: `- **D-nn:** <ruling> — <rationale>.`
Decisions are never rewritten in place; a later ruling is filed as `D-nna` with its date, and the
decision it amends gains a one-line pointer to it.

**One warning about ids.** `D-01`…`D-05` and their letters belong to *this* document. Where a
section cites a decision id that is **not** in the list below — the audio `D-09`, the win
celebration's `D-14`, the info button's `D-15`, and the ad-allowance `D-05` the code records at
`GameUiState.kt:131-135` — it is naming an **earlier phase's** ruling, not one of these. The
ad-allowance id is the one genuine collision, and §4.4 qualifies it where it appears.

### Tutorial (ONB-01, proposed UX-01)

- **D-01:** The tutorial teaches by **making the player perform the jump**, not by describing it.
  Three micro-puzzles run in sequence on a live, tappable board; a step advances **only when the
  player completes it**, never on a Next button. — rules/puzzle-design.md requires the first
  puzzles to "teach without text, the way a level layout teaches", and the current sheet is the
  exact anti-pattern it names: three static `BoardPreview` illustrations plus three sentences of
  prose behind one "Got it" button (`OnboardingOverlay.kt:96-131`, `:179-184`). A player can
  dismiss it today without ever having jumped a peg.

- **D-01a:** The tutorial board is the **real `BoardCanvas` in `interactive = true` mode**, driven
  by a real `GameUiState` and the real rules engine — not a bespoke illustration. — `BoardCanvas`
  already takes a whole `GameUiState` and emits bounds-checked taps
  (`BoardCanvas.kt:87-93`, `:205-212`), so the tutorial inherits selection rings, the breathing
  pulse, legal-destination dots, the jump slide and the removal fade for free, and what the player
  learns is literally the mechanic they will use. The current sheet cannot do this: it routes
  through `BoardPreview`, which hard-codes `interactive = false` (`BoardPreview.kt:86`) and
  overrides its caller's size with `catalog_preview_size` 96dp (`BoardPreview.kt:79`) — which is
  why the `onboarding_illustration_size` 160dp token the overlay passes at
  `OnboardingOverlay.kt:144` has never had any effect on screen.
  *(Clarified 2026-09-21 — see D-01g and §4.1 "The board the tutorial drives": the `GameUiState`
  this ruling names is a **UI model assembled by `TutorialViewModel`**. It is not licence to host
  `GameScreen` or to reach for `GameViewModel`, both of which §5.1 forbids.)*

- **D-01b:** Steps are **T1 one jump → T2 the other axis → T3 clear it**, in that order, each with
  exactly one lesson. `showHints` is forced **on** inside the tutorial regardless of the persisted
  setting, so legal destinations are always visible. — One lesson per step is what makes a step
  completable without reading; forcing the dots on removes the only way a step could become a
  guessing game, which rules/puzzle-design.md forbids ("all needed information visible").

- **D-01c:** **A wrong tap is never a failure.** There is no error state, no red, no modal, no
  retry count, no score. The three wrong-tap classes are answered by escalating *assistance*, not
  by correction — see §4.4. — rules/puzzle-design.md: "first failure impossible before puzzle 5".
  The tutorial is puzzle 0; it must not be losable.

- **D-01d:** **Skip is available on every step** as a labelled text action, and exits immediately.
  System back does exactly what Skip does. Both persist `markSeen()`. — A tutorial the player
  cannot leave is worse than no tutorial. Routing back to the same answer as Skip follows the
  precedent already set for `ProfileSwapDialog`, whose back press is documented as one of three
  spellings of a single answer (`HomeScreen.kt:108-111`). The alternative — back exits without
  marking seen — re-forces the tutorial on the next launch for a player who deliberately left,
  which is the worse of the two failures.

- **D-01e:** There is **no cross-process resume.** `markSeen()` is written on completion or on an
  explicit Skip/back only; a process death mid-tutorial leaves the flag false, so the tutorial
  offers itself again from T1. Within a session, rotation keeps the current step. — Three micro-
  steps are under a minute of play; a persisted step cursor would cost a DataStore key, a
  migration and a test matrix to save a player fifteen seconds.

- **D-01f:** The tutorial is a **screen, not a dialog over another screen**. It owns the full
  viewport, its own state holder and its own back handling. — Screen inventory is this role's
  call. It now has multi-step state, a live board and two entry points; hosting that inside
  `HomeScreen`'s composition (as `OnboardingOverlay.kt` is hosted at `HomeScreen.kt:174-176`)
  would put a playable board inside the screen whose own job is mode selection. **The hosting
  mechanism — a `@Serializable` nav destination versus a full-screen composable switched above
  the `NavHost` — is the tech-lead's call, not mine**; §4.1 specifies what the player sees either
  way, and §7 lists the one sequencing invariant the mechanism must preserve.
  *(Answered 2026-09-21 — see D-01g. The player-facing contract in §4.1 is unchanged by the
  answer; D-01a's board contract is clarified there.)*

- **D-01g** *(2026-09-21 — tech-lead ruling, `tech-lead-architecture.md` §1/§1a, folded in)*: the
  tutorial is a **`@Serializable` nav destination** (`composable<Tutorial>` in `AppNavHost`) with
  a `@HiltViewModel` **`TutorialViewModel`** scoped to that destination; step state lives in a
  plain `MutableStateFlow` and **not** in `SavedStateHandle`. Three player-visible consequences
  are now contract rather than mechanism, and §4.1 and §14 state them:
  1. **A replay from Settings always starts at T1.** A destination-scoped ViewModel is cleared on
     pop, so a player who skipped at T2 and later taps Settings › How to play gets a fresh T1 —
     the bug an Activity-scoped holder would have shipped.
  2. **The exit persists before it leaves.** `markSeen()` is awaited — on the application scope,
     so a cancelled screen cannot drop it — and only then does the screen pop. Skip, system back
     and the completion CTA are one function with three callers, latched so a double exit is a
     no-op. Without this, Home re-pushes the tutorial the moment it recomposes, and the player is
     in a loop; that loop is a UX failure, which is why it is restated here and not only in §7.
  3. **The tutorial never runs the game's own screen or state holder.** It hosts `BoardCanvas`
     and never `GameScreen`; its state holder is `TutorialViewModel` and never `GameViewModel`.
     This is a **hard contract**, stated as a prohibition in §5.1 — not a preference — because
     `GameViewModel`'s `Won` path clears the mode's resume snapshot and advances the Challenge
     ladder (`GameViewModel.kt:956-975`: `clearSnapshot()` at `:963`,
     `challengeProgress.advancePast(...)` at `:973`). Every tutorial step ends on one peg, so a
     tutorial routed through `GameViewModel` would fire that path **three times per run** and
     destroy two of the three cloud-save device oracles §4.2.1 exists to protect.
  — Hosting is the tech-lead's authority and this spec routed it there; what comes back into this
  document is only what the player can see or lose.

- **D-01h** *(2026-09-21 — UX decision)* — ⚠️ **AMENDED THE SAME DAY: read D-01i first.** Its
  opening clause is **no longer true**: the jump cue *does* fire in the tutorial. Everything else
  this decision silenced is **upheld**, and the completion CTA it left unruled is ruled in D-01i.
  The text below is kept verbatim as the record of what was decided and why it was overturned; it
  is not the contract. — **the tutorial is silent, end to end.** No cue at all
  plays anywhere on the tutorial screen — not `Select`, `Deselect`, `Jump`, `PegRemove`,
  `InvalidTap` or `Undo` on the board, and not `StarPop` or `Win` on the completion star — and no
  cue is added to `BoardCanvas`. The only sound in the whole tutorial flow is the
  `SoundCue.ButtonForward` that the **Settings row** plays on the way in (§4.5), which belongs to
  Settings and not to this screen. — Every sound cue in the game is played by a private effect in `GameScreen`
  (`GameScreen.kt:327-428`); `BoardCanvas` contains no `SoundCue` reference at all, so a tutorial
  hosting the canvas is silent unless a second screen re-implements those effects. Three reasons
  to take the silence deliberately rather than inherit it: rules/audio.md says nothing plays that
  the spec does not name, so silence is compliant; every tutorial event already carries visual
  feedback (the selection ring, the legal-destination dots, the jump slide, the removal fade), so
  the "sound is never the only feedback" rule is satisfied with room to spare; and an `InvalidTap`
  cue would contradict D-01c, which says a wrong tap is never a failure — a rejection sound is the
  one channel that would make it feel like one. The first sound the player hears is therefore the
  first jump of their first **real** board, which is the right moment for it. §4.4's tutorial
  wrong-tap rows and §9's audio row are amended to match. Revisiting this is the sound-designer's
  to propose and mine to rule on; it is not a founder question.

- **D-01i** *(2026-09-21 — UX ruling, amending D-01h; from the qa-engineer's D-01h review,
  standing in for the unstaffed sound-designer — `.planning/reports/phase-15/qa-engineer-tutorial-audio.md`)*:
  **the tutorial is not silent end to end. A completed jump sounds; nothing else on the board
  does; and the two ways out speak the app's own press vocabulary.**

  | Event on the tutorial screen | Cue | Ruling |
  |---|---|---|
  | A peg **completes** a jump | `SoundCue.Jump`, pitched by `playJump(runIndex)`, with `SoundCue.PegRemove` layered `SoundParams.REMOVAL_LAYER_DELAY_MS` beneath it — the same pair, the same levels, the same pitch ladder as the real game | **Fires** |
  | A peg is selected / put back down | `Select`, `Deselect` | **Silent** — D-01h upheld |
  | A peg with no legal jump is tapped; an illegal destination is tapped | `InvalidTap` | **Silent** — D-01h upheld |
  | Undo | `Undo` | **Silent** — there is no Undo in the tutorial at all (§5) |
  | A hint is revealed | `Hint` | **Silent** — `showHints` is forced on (D-01b), so nothing is ever *revealed* and `hintSource` never transitions |
  | The completion star, end of T3 | `StarPop`, `Win` | **Silent** — D-01h upheld; a tutorial is not a victory and D-14's two-beat celebration belongs to a real solve |
  | The completion CTA ("Got it") | `SoundCue.ButtonForward` | **Fires** — the gap D-01h left, now ruled |
  | `Skip`, on any step | `SoundCue.ButtonDismiss` | **Fires** — same ruling |
  | System back | — | **Silent**: there is no button press to sound. It is the same exit path (D-01d), not the same event. |

  **Why the jump cue fires.** The decisive reason is not taste: `.planning/REQUIREMENTS.md:38`
  (SOUND-01) reads "Player hears a jump sound on **every** peg jump, pitch-varied so repeated
  jumps do not fatigue", and it is marked **Complete** (`:165`). The tutorial's jumps are real
  jumps on the real rules engine (D-01a). D-01h therefore made the tutorial the single place in
  the shipped product where a peg jump produces no jump sound — without amending SOUND-01,
  without scoping it, and without registering the narrowing anywhere, since §16 carried **no
  SOUND-nn row at all**. A UX decision may not narrow a Complete requirement invisibly. §16 now
  carries SOUND-01 and SOUND-04.

  Two design reasons agree with it. **Fidelity is the tutorial's whole premise** — D-01/D-01a
  stake the design on the real canvas and the real engine, "what the player learns is literally
  the mechanic they will use", and the tutorial already reproduces the selection ring, the
  destination dots, the jump slide and the removal fade; audio would have been the sole
  deliberate divergence, teaching for thirty seconds that a jump is a silent event and then
  contradicting that at the first real board. And **D-01c has stripped every other consequence
  from the screen** — no score, no failure, no retry count, no red, no modal — which is exactly
  why the completed jump is the tutorial's only moment of positive consequence; removing its one
  audible confirmation leaves the payoff thinner than the teaching needs.

  **What D-01h got right, and keeps.** Its `InvalidTap` reasoning is the load-bearing one and is
  upheld without reservation: a rejection sound is the one channel that would make a wrong tap
  feel like a failure, which D-01c forbids. Its other two reasons — that rules/audio.md permits
  an unnamed cue to be absent, and that every tutorial event already carries visual feedback —
  establish that silence is *permitted* and that either choice is legal. Neither is an argument
  that silence is *right*, and the decision rested on them.

  **Cost, and the shape the developer must use.** One lifted composable and one call site.
  `JumpSound` is `private fun JumpSound(transition: MoveTransition?)` at `GameScreen.kt:320-330`;
  it takes a `MoveTransition?` and reads `LocalSoundPlayer`, and touches no `GameViewModel`, no
  repository and no navigation. Lifting it to a shared `internal` composable that both screens
  call keeps §5.1's four prohibitions intact (see the note under §5.1) and keeps §5's "no
  `SoundCue` reference added to `BoardCanvas`" intact, because the effect lives *beside* the
  screen and never inside the canvas. The state it needs is already there: `BoardCanvas` draws
  the jump slide from `state.animating` (`BoardCanvas.kt:115`), so a tutorial that shows the
  slide D-01a promises has already populated the very field the cue is keyed on.
  **Re-implementing the effect inside the tutorial instead of lifting it is a defect** — the
  `REMOVAL_LAYER_DELAY_MS` layering and the `runIndex` pitch ladder would be duplicated and would
  drift. The reviewer insists on the lift, not the copy.

  **The completion CTA and `Skip`.** D-01h's blanket "no cue at all plays anywhere on the
  tutorial screen" collided with three things in this same document and repo: §4.1's full-width
  `CtaGradient` completion button, SOUND-04 ("Player hears button-press feedback on primary UI
  actions", `.planning/REQUIREMENTS.md:41`, also Complete) and D-11's forward/dismiss split
  (`SoundCue.kt:70-74`). §5's enumerated prohibition named only board and completion cues, so a
  developer building the CTA with the house idiom — `withClickSound(SoundCue.ButtonForward, …)`,
  as at `HomeScreen.kt:447` and `:581` — would have violated a spec sentence that no grep and no
  test could catch, while a developer omitting it would have violated SOUND-04. Ruled: **both
  press cues fire, with the app's standard cues.** They are *exit* actions and belong to the
  navigation vocabulary the rest of the app already speaks; neither can be mistaken for a verdict
  on the player's tapping, which is the only thing D-01c actually protects. §5's list now names
  `ButtonForward` and `ButtonDismiss` explicitly, so the ruling is checkable either way.

  **No new asset and no new licence row.** `Jump` → `R.raw.sfx_jump` (`SoundCue.kt:30`),
  `PegRemove` → `R.raw.sfx_peg_remove` (`SoundCue.kt:33`), `ButtonForward`/`ButtonDismiss`
  (`SoundCue.kt:70-74`) all ship today and are credited CC0 1.0 in `docs/audio/CREDITS.md`. §5's
  "No new audio asset" stands unamended, and §9's row is updated only to say which cues the
  tutorial now uses.

  **One thing this ruling cannot prove by reading, so QA checks it.** `JumpSound` consults no
  animation flag of any kind (`GameScreen.kt:320-330`), and the project's precedent is that cues
  survive reduced motion (`StarPopSounds` reads `rememberAnimationsEnabled()` only to change the
  stagger, `ResultsCelebration.kt:104-106`). But with OS animations off `runJumpAnimation` takes
  the snap path and calls `onAnimationFinished()` immediately (`BoardCanvas.kt:298-304`), so
  `animating` is set and cleared inside a very short window; whether the keyed effect always wins
  that race is a **device check, not a reading**. Criterion 7c therefore requires one pass with
  OS animations off.

  **Not in scope here.** rules/audio.md names `docs/audio-spec.md` as the cue list of record and
  that file does not exist on any branch. Writing it is the sound-designer's work and the
  producer is tracking it; this spec names cues per screen and does not substitute for it.

### The info button (NAV-01, D-15 superseded)

- **D-02:** **Delete the Home top bar's How-to-play `IconButton`** — `HomeScreen.kt:348-354`, the
  `Icons.Rounded.Info` action whose `contentDescription` is `R.string.how_to_play`
  (`strings.xml:62`). Its `onShowHowToPlay` parameter (`HomeScreen.kt:129`, passed to `HomeTopBar`
  at `:151`, wired at `HomeRoute.kt:48`) leaves `HomeScreen` entirely. — The founder asked for it,
  and it is also the weaker of the two affordances it competes with: it is icon-only, it sits in
  the hardest-to-reach corner of the screen, and it reuses `Icons.Rounded.Info` — the *same glyph*
  the in-game show-moves toggle uses for a completely unrelated meaning
  (`HintToggleControl.kt:5`, `:42`). This deletion also removes half of that collision.

- **D-02a:** The replay path is a **Settings › General row, "How to play"**, and that is the only
  permanent entry. First-run auto-start is **unchanged**. — Both routes are required and they
  answer different needs. First-run auto-start (today: `onboardingSeen == false` →
  `onboardingVisible = true`, `HomeViewModel.kt:99-103`) is how a new player meets the game and
  costs a returning player nothing, because `OnboardingRepository.onboardingSeen`
  (`OnboardingRepository.kt:48-51`) is already persisted and fails open to `false` — so a
  returning player is never re-taught and a player whose store read fails is re-offered the
  tutorial rather than silently denied it. The Settings row is where a casual-game player looks
  for "how does this work again", it is a labelled row rather than a bare icon, and
  `SettingsLinkRow` (`SettingsScreen.kt:599`) already supports exactly this shape. Home keeps
  zero tutorial chrome, which is what the founder asked for.

- **D-02b:** `HomeViewModel.onShowHowToPlay()` (`HomeViewModel.kt:227-229`) is **retired** as a
  Home concern. `onDismissOnboarding()`/`markSeen()` (`:232-235`) survive as the tutorial's own
  completion path. — Once Home has no affordance, a Home handler that forces `onboardingVisible`
  true has no caller; leaving it would leave a second writer of the flag that
  `HomeScreen.kt:192-198`'s guard exists to defend against, with nothing writing it.

### Home (NAV-01, PERS-03, CHAL-05, proposed UX-02)

- **D-03:** Home is composed of **five stacked bands** — hero, progress ribbon, Continue chips,
  mode tiles, footer — inside one scrolling column capped at `content_max_width`. The two mode
  tiles remain the primary actions and are **bottom-weighted into the thumb zone**. — The founder
  is right that two tiles on an empty field reads as unfinished, and the fix is not more buttons:
  it is giving the player something about *their own* game to look at. Everything added is either
  the game's own board art or a number the player earned.

- **D-03a:** The Home `TopAppBar` (`HomeScreen.kt:340-363`) is **deleted**; its gradient title
  moves into the hero band and the Settings gear becomes a clay circular chip at the hero's
  top-end. — With the info button gone (D-02) the bar holds one icon and a title, which is chrome
  around a single control. The gradient Fredoka title is the brand moment and deserves display
  scale, not `titleLarge` in a 56dp bar.

- **D-03b:** The hero carries **decorative board art**, not a logo or an illustration. — The
  studio ships no logo, and rules/ip-policy.md means any sourced art needs a licence row. The
  game's own candy pegs and dimples are already an `ImageVector`-free, hex-free, licence-free
  visual language (`BoardCanvas.kt:480-522`); using them costs nothing and cannot be an IP
  problem. Art direction between two equally valid treatments is queued as **Q-901** (§15) with a
  default chosen so nothing blocks.

- **D-03c:** The progress ribbon shows **three pills: Stars · Boards · Level**, and each pill
  renders a neutral em-dash placeholder when its value is not yet known. — Three is the most the
  ribbon can hold at 200% font scale without wrapping into a second row, and the placeholder rule
  is what makes the band shippable before its data lands: the developer can build the ribbon in
  one plan and fill the two derived numbers in another without a half-drawn screen in between.

- **D-03d:** **No number on Home requires a new table, column or DataStore key.** Stars and Boards
  are folded client-side from `ProgressRepository.observeResults(mode)`
  (`ProgressRepository.kt:83`), which already streams `stars`, `bestTimeMs` and `bestScore` per
  level (`ProgressRepository.kt:284-291`); `HomeViewModel` already injects `ProgressRepository`
  (`HomeViewModel.kt:86`) and simply never reads it for stars. Level is
  `HomeUiState.challengeCurrentLevel`, which exists today (`HomeUiState.kt:72`). — This was
  checked against the data layer before the band was designed, per the brief. The
  `suspend fun aggregate()` path (`ProgressRepository.kt:115`) is deliberately **not** used: it is
  a one-shot, not a flow, so a Home band built on it would go stale the moment the player
  finished a level, and its `totalStars` is a deliberate both-modes sum with no `WHERE`
  (`LevelResultDao.kt:66`) that cannot answer "Classic boards cleared". §7 lists the exact
  requirement.
  *(Amended 2026-09-21 — see D-03h. Every conclusion above stands, including the refusal of
  `aggregate()`; only the read **mechanism** — a client-side fold — is superseded.)*

- **D-03e:** Home **does not** show Play Games sign-in state. — The flow exists
  (`GamesService.session`, `GamesService.kt:69`; `GamesSignInState.SignedIn.displayName`,
  `GamesSignInState.kt:63-66`) but only Settings collects it
  (`SettingsViewModel.kt:110-111`), and Settings already renders it well
  (`SettingsScreen.kt:289-330`). Putting an account chip on Home would duplicate a surface, add a
  third `Unknown`-state flicker to the start destination, and spend the player's first screen on
  something that is not playing the game. It would also put a Play Games SDK-backed flow on the
  cold-start critical path (D-03g), which is the second and harder reason.

- **D-03f:** **Three existing readouts are device-test oracles and are load-bearing, not
  decoration.** The Challenge ladder position, the Classic per-board stars and the Continue/resume
  affordance must remain visible, un-collapsed and un-deferred. Nothing in this refresh may hide
  any of them behind a tap, a expander, a scroll, or a later screen. — This is a release-manager
  ruling, not a preference: the cloud-save device checklist (`11-HUMAN-UAT.md`, tests 10/11/13)
  reads its pass/fail off exactly these three, so hiding one does not merely degrade the screen —
  it makes cloud save **unverifiable on a device**. §4.2.1 states where each one lives and what
  "survives" means for it, because two of the three are easy to break by accident and one of them
  does not live on Home at all.

- **D-03g:** **Home's first frame must add no blocking read, no image decode and no asset load.**
  Every visual this refresh adds to Home is vector or `Canvas` geometry drawn from state the app
  already holds; the one genuinely new data read (the progress ribbon, §7 R-1/R-2) paints
  placeholders immediately and fills on its first emission, so it can never hold up first paint. —
  PLAT-08 criterion C1 is a measured pass/fail at **≤ 950 ms cold start**, and a fuller Home is
  first-frame work by definition: every band added here is on that critical path. A "fancier"
  Home that misses C1 has failed, so the budget is designed in rather than discovered at the gate.
  §6.5 states the budget, the risk and the ordered degradation path if measurement shows a
  regression.
  *(Amended 2026-09-21 — the ruling stands, but **how** C1 is checked for this phase changed: it
  is a two-arm delta against `develop`, not a single absolute number against the pre-SDK 633 ms
  reference. The procedure is §6.5 "How C1 is measured for this phase" and the gate is criterion
  15e.)*

- **D-03h** *(2026-09-21 — tech-lead ruling, `tech-lead-architecture.md` §2/§3, folded in)*: the
  two derived ribbon values come from **one `Flow`-returning SQL aggregate**
  (`ProgressRepository.observeHomeProgress()` over `LevelResultDao.observeHomeProgress`), not from
  a client-side fold over `observeResults`. D-03d's conclusions are unchanged — no schema change,
  no migration, no new column, no new DataStore key, no `aggregate()`, one collection for both
  pills, placeholder-first — and so is every pixel of §4.2. Two things it changes are mine, which
  is why it is recorded on this side of the wall and not only in the report:
  1. **The placeholder rule gets stronger, not weaker.** A failed read must render `—`, never
     `0`: the new repository member carries the same fail-degraded `.catch { … }` contract
     `observeResults` has, so "database unavailable" and "no stars yet" stay distinguishable on
     screen. A `0` shown to a player who has earned forty stars is a lie about their own progress
     on the first screen they see. §14 gains a criterion for it.
  2. **The cold-start risk §6.5 named is the one that retires.** The fold's cost grew with the
     player's Challenge history — invisible on a fresh install, worst on the most engaged player.
     The aggregate is one row regardless of the ladder's length. §6.5's ordered degradation path
     is otherwise unchanged and still ends at "the pills stay and show —".
  — Data-layer mechanism is the tech-lead's authority; what this spec owns is what the player sees
  while the read is slow, empty or broken, and that is stated above and in §4.2.

### In-game controls (ASST-01, ASST-02, CLAS-02, SCOR-02, proposed UX-03)

- **D-04:** The five controls are kept, **re-glyphed, re-grouped and re-homed**: read-only status
  (back, title, timer) stays at the top in a chrome-free strip; the four *actions* (Undo, Hint,
  Show-moves, Restart) move into a clay dock at the bottom, in the thumb zone. — Five 48dp actions
  plus a timer label plus a title currently share one `TopAppBar` actions row
  (`GameScreen.kt:519-525`). Four targets at `top_bar_touch_target` 48dp (`dimens.xml:110`) is
  192dp before the timer, on a 360dp-wide phone — the title is already squeezed at default font
  scale and collides at large scale. Splitting by *kind* fixes the crowding and the reach problem
  at once: rules/compose.md requires primary actions in the lower thumb zone of a 6.7-inch phone,
  and Undo — the most-used control in peg solitaire, since backtracking *is* the game — is
  currently in the single worst-reachable corner. The strip that remains carries exactly one icon,
  redrawn in Candy Clay.

- **D-04a:** **Hint stops using `Icons.Rounded.Search`.** A new in-house `HintIcon` (a lightbulb)
  replaces it. — A magnifying glass reads as "search" or "zoom", not "show me a winning move"
  (`HintControl.kt:5`, `:69`). The core icon set has no lightbulb and the extended set is banned,
  so the glyph is authored locally exactly as `UndoIcon.kt` and `ModeGlyphs.kt` already are.

- **D-04b:** **Show-moves stops using `Icons.Rounded.Info` and stops conveying its state by tint
  alone.** A new in-house `ShowMovesIcon` (a peg with two trailing destination dots — literally
  what the feature draws) replaces the glyph, and the ON state adds a filled `primaryContainer`
  pill behind it. — Two defects in one control. The glyph collides with the Home info button's
  meaning (`HintToggleControl.kt:5` vs `HomeScreen.kt:23`), and the on/off state is carried by
  `primary` vs `onSurfaceVariant` tint and nothing else (`HintToggleControl.kt:45-51`), which
  breaks rules/compose.md's "never colour alone (shape, pattern, label)". The content description
  already switches correctly (`show_moves_on`/`show_moves_off`, `strings.xml:90-91`) — the defect
  is purely visual, and a filled-vs-unfilled pill fixes it without new copy.

- **D-04c:** Undo and Hint are **visually louder than** Show-moves and Restart, by fill and not by
  size. All four targets are the same `game_action_button_size`. — Hierarchy has to be legible,
  but four different button sizes in one row reads as a mistake, and unequal targets invite the
  smaller ones to drop under 48dp at some font scale. Fill is the safer channel.

- **D-04d:** The Hint "thinking" state keeps its current behaviour exactly — spinner in place of
  the glyph, `enabled = false`, `hint_thinking` description (`HintControl.kt:47-62`). Undo keeps
  `enabled = canUndo`. — These are correct today, including the TalkBack "disabled" reporting that
  `enabled = false` gives for free; the redesign is a reskin of them, not a rewrite.

### The toolbar (D-05 — and its reconciliation with D-04)

- **D-05:** **The "toolbar" and the "icons on the top" are one surface, not two.** The code is
  unambiguous: `GameScreen.kt:171-187` gives its `Scaffold` a single `topBar`, which is
  `GameTopBar` (`:490-527`), which is a single M3 `TopAppBar` (`:502`) whose three slots are the
  back arrow (`:503-517`), the title (`:518`) and the five actions (`:519-525`). There is no
  second control row anywhere on the screen. So "remove toolbar" and "redesign the icons on the
  top" cannot be satisfied independently, and this spec treats them as **one change with one
  replacement**.

- **D-05a:** **What goes:** the `TopAppBar` composable itself, the `Scaffold`'s `topBar` slot, the
  bar's surface/elevation/inset chrome, and `GameTopBar` as a function. **What stays:** all five
  controls, the context-aware title logic (`GameTopBarTitle`, `:537-552`), the timer label
  (`ElapsedTimeLabel`, `:563-574`), and every handler signature `GameScreen` already takes. —
  Nothing the player can *use* is being removed. What is being removed is the Material chrome
  wrapped around it, which is the only reading of "remove toolbar" that does not also delete
  functionality the game needs.

- **D-05b:** **Back-navigation survives as a visible, labelled control** — a 48dp clay circular
  back chip at the status strip's leading edge, keeping
  `Icons.AutoMirrored.Rounded.ArrowBack` and the `R.string.navigate_back` content description
  unchanged. — This is non-negotiable and needs stating plainly, because the top bar is currently
  the *only* visible way out of a game: `onNavigateBack` is a bare `navController.popBackStack()`
  (`AppNavHost.kt:131`) and there is no `BackHandler` on the gameplay destination — the only one
  in the codebase is `AdQuiescenceScrim.kt:61`. System back therefore still works after the bar
  is deleted, but system back alone is not an acceptable exit: it is invisible, and on
  gesture-navigation devices it is a discoverability failure for exactly the casual player this
  game is for. Keeping the same glyph and the same content description also keeps
  `ButtonSoundTest.kt:226-244` — which finds this control by `R.string.navigate_back` — passing
  unchanged.

- **D-05c:** The status strip is **not** an M3 `TopAppBar`, `CenterAlignedTopAppBar`, or any
  `*TopAppBar`, and the action dock is **not** an M3 `BottomAppBar` or `NavigationBar`. Both are
  plain `Row`s inside the screen's own layout. — Re-adding a Material bar under a different name
  would re-introduce the chrome the founder asked to remove, and `NavigationBar` in particular
  carries destination semantics that would announce four in-game actions to TalkBack as
  navigation targets, which they are not.

---

## 4. Interaction contract — new surfaces (Phase 15)

### 4.1 Tutorial — `TutorialScreen.kt` (ONB-01, D-01/D-01a–h, D-02a)

**Purpose.** Teach the orthogonal jump and the goal by having the player do both, once, in under
a minute, without reading.

**Layout**, top → bottom, full viewport, `background` with the Home blobs reused:

| Band | Content | Notes |
|------|---------|-------|
| Header | Step pips (leading) · `Skip` text button (trailing) | Pips are the only progress signal; no title bar |
| Board | The live `BoardCanvas`, `interactive = true`, on a `surfaceContainerHigh` clay slab (`shapes.extraLarge`, `clayShadow(elevation_card)`), square, `tutorial_board_size` | The one interactive element |
| Caption | One line, `bodyMedium`, `onSurfaceVariant`, centred, reserved at `tutorial_caption_min_height` | Reserved height so a 1↔2-line swap never reflows the board |
| Footer | Empty until T3 completes, then the completion CTA | Keeps the board optically centred during play |

**The four bands sit in a `verticalScroll` column**, exactly as Home does (§4.2) and as §12
already assumes. Scrolling is for the overflow case only: at default font scale on a 360×640dp
phone all four bands fit with no scrolling, and the board keeps `tutorial_board_size` and its
square aspect at every scale rather than shrinking — a 3×3 board there is ≈93dp per cell, and
compressing the one interactive element to win vertical space is the wrong trade. What yields at
200% font scale is the caption's growth pushing the footer below the fold, which is harmless
because the footer is empty until T3 completes.

**Step pips.** Three pips, `tutorial_step_pip_size`. Done = solid `GoldGradient` disc; current =
outlined disc with a `primary` ring; upcoming = outlined disc, `outlineVariant`. Filled-vs-outlined
carries the state, so colour is not the only channel. The row is one semantics node via
`clearAndSetSemantics` announcing `tutorial_step_progress` ("Step 2 of 3") — the same idiom
`EarnedStarPips` already uses (`BoardPreview.kt:100-104`).

**The three steps.**

| Step | Teaches | Footprint | Completes when |
|------|---------|-----------|----------------|
| **T1** | Tap a peg, tap a hole two cells away; the middle peg goes | 1×3 strip: in-play (0,0),(0,1),(0,2); pegs at (0,0),(0,1); hole at (0,2) — exactly one legal move, horizontal | one peg remains (1 move) |
| **T2** | The same jump works on the other axis, and the destination is two cells away — never the hole next door, never a diagonal | full 3×3: all nine cells in play; pegs at (0,1),(1,1); the other seven are holes — exactly one legal move, vertical | one peg remains (1 move) |
| **T3** | The goal is to leave one peg | full 3×3: pegs at (0,0),(0,1),(1,0),(1,1),(1,2); holes at (0,2),(2,0),(2,1),(2,2) — three legal openings, every line wins | one peg remains (4 moves) |

> **Board content is the game-designer's ruling** (R-4, closed 2026-09-21 —
> `.planning/reports/phase-15/game-designer-tutorial-boards.md`). All three footprints are proven
> dead-end-free to one peg by exhaustive enumeration of every reachable position, so D-01c holds
> **structurally**: no sequence of taps can strand the player above the target, and the tutorial
> cannot be lost by anyone. T1 is unchanged from `OnboardingOverlay.kt:155-171`. The
> step-completion predicate is therefore **the same for all three steps — "the board holds one
> peg"** — so no step needs a bespoke completion rule.
>
> Two consequences for the screen, both this role's to state. **First, the footprints fit:** the
> largest is 3×3, which inside `tutorial_board_size` 280dp gives ≈93dp cells — comfortably above
> §11's 48dp floor, so no token changes (§6.1 stands). **Second, T1 and T2 each have exactly one
> legal move.** That is deliberate — a step teaching one mechanic pays nothing for ambiguity — but
> it means the coach must never read as a puzzle refusing the player: with `showHints` forced on
> (D-01b), selecting the only movable peg draws exactly one destination dot, and §4.4's escalation
> is assistance toward that dot, never correction.
>
> **The boards live in `object TutorialBoards` in `:domain` and are never added to
> `Boards.CLASSIC_SPECS`** (§5, §7 R-4). That list is the Classic catalogue and the denominator of
> the Home "Boards" pill (§7 R-2); adding a teaching board to it would change a number the player
> reads on Home and put three miniatures on the catalogue screen.

**Advance.** On completion the caption swaps, the pip advances, and the next footprint installs
after one beat (`TutorialAnim.STEP_ADVANCE_DELAY_MS`) so the player sees their own jump land
before the board changes. No button, no tap-to-continue. A step is complete when its board holds
exactly one peg — the same test for all three steps, evaluated as `board.pegCount == 1` on the
tutorial's **own** state. Not `GameStatus`, not a `minimumPegs` comparison: reaching for the
game's status type is the first step down the path that ends at `GameViewModel`, which §5.1
forbids outright (D-01g).

**The board the tutorial drives.** `BoardCanvas` takes a whole `GameUiState`
(`BoardCanvas.kt:87-93`), and that stays true — but the value is assembled by `TutorialViewModel`
from the current `TutorialBoards` footprint. `GameUiState` here is a **UI model**, not evidence
that the game's own state holder is involved; D-01a's "a real `GameUiState` and the real rules
engine" means exactly that and nothing more (D-01g, §5.1).

**The jump the player hears.** A **completed** jump plays the game's own jump pair — the pitched
`SoundCue.Jump` with `SoundCue.PegRemove` layered beneath it — on every step, including each of
T3's four jumps, and with the same pitch ladder when the same peg jumps twice in a row (D-01i,
SOUND-01). It is the same effect the gameplay screen uses, lifted to a shared composable rather
than re-implemented (`GameScreen.kt:320-330`), and it is keyed on the same `state.animating` the
canvas already draws the slide from (`BoardCanvas.kt:115`). **That is the only sound the board
makes:** selection, deselection, a tap with no legal jump, a tap on an illegal destination and
the completion star are all silent (D-01i, §4.4, §5). The contrast is the point — the one thing
the tutorial rewards audibly is the thing it is teaching.

**Completion.** After T3: the caption becomes `tutorial_done_caption`, a single gold star pops in
reusing the Results star vocabulary (`ClayAnim.STAR_POP_MS`, `goldGradientFill()`), and the footer
raises one full-width `CtaGradient` button labelled with the **existing** `onboarding_got_it`
("Got it"). Tapping it plays `SoundCue.ButtonForward` — the same press cue every other primary
CTA in the app plays, via the house `withClickSound` idiom (D-01i, D-11, SOUND-04) — then
persists `markSeen()` and leaves. **The star itself pops silently:** neither `SoundCue.Win` nor
`SoundCue.StarPop` plays (D-01h, upheld by D-01i) — a tutorial is not a victory, and D-14's
two-beat celebration belongs to a real solve. `ClayAnim.STAR_POP_MS` is borrowed as a *duration*,
not as a cue. Nor is a star banked, a resume snapshot cleared or the Challenge ladder advanced:
the gold star on this screen is an animation and nothing behind it moves (§5.1, D-01g).

**Exit.** `Skip` (every step), system back and the completion CTA are **one exit path with three
callers** (D-01d, D-01g). The two that are button presses sound like button presses: `Skip` plays
`SoundCue.ButtonDismiss` and the CTA plays `SoundCue.ButtonForward` (D-01i, D-11). System back is
silent — there is no press to sound, and it is the same exit path, not the same event. The path
persists `markSeen()` first and pops only then, so Home can never
re-open the tutorial on the frame after the player left it; a second exit is a latched no-op.
Popping returns the player exactly where they came from — Home on first run, Settings on replay —
and a replay always restarts at T1, because the step state is scoped to the destination and dies
with it.

### 4.2 Home — `HomeScreen.kt` (NAV-01, PERS-03, CHAL-05, D-03/D-03a–h)

Single `verticalScroll` column, `widthIn(max = content_max_width)`, five bands:

**1 — Hero.** `home_hero_min_height`. The app title in `displaySmall` (Fredoka) filled with
`ctaGradientBrush()`, and decorative board art at `home_hero_art_size`. The Settings gear is a
48dp clay circular chip at the band's top-end, keeping `Icons.Rounded.Settings` and the
`settings_action` content description. The art is `contentDescription = null` and is fully static
when animations are off. **No info button (D-02).**

**2 — Progress ribbon.** One `Row`, three equal-weight clay pills, `home_stat_pill_min_height`,
`spacing_sm` between them, each a `surfaceContainerHigh` `shapes.medium` surface with
`clayShadow(elevation_card)`:

| Pill | Glyph | Value | Source |
|------|-------|-------|--------|
| Stars | `Icons.Filled.Star` + `goldGradientFill()` | total stars earned, both modes | `observeHomeProgress()` — one row, one collector (§7 R-1, D-03h) |
| Boards | existing `rememberClassicGlyph()` | Classic boards cleared `/ 5` | the same row, same collector (§7 R-2, D-03h) |
| Level | existing `rememberChallengeGlyph()` | the Challenge ladder position | `challengeCurrentLevel` — **exists today** |

Each pill is one merged semantics node reading label-then-value ("Stars, 7"), never a bare number.
A pill with no value yet renders `stat_value_unknown` ("—") and announces
`stat_value_unknown_desc` ("not yet available"). **"Not yet" and "the read failed" render
identically, and neither ever renders as `0`** (D-03h): a pill shows a number only once a real
value has arrived for it. The pills are **not** tappable — they are status,
and making them navigate would put three more competing targets on the screen the founder already
finds busy.

> The Level pill shows `challengeCurrentLevel`, which is the **next** level to play and is
> off-by-one from the highest level banked, by design (`ProgressRepository.kt:107-113`,
> `ChallengeProgress.kt:64-74`). The copy is therefore "Level N" — the level the Challenge tile
> will enter — and never "highest" or "best". Do not "reconcile" the two numbers.

**3 — Continue chips.** Behaviour, ordering and data unchanged (`HomeScreen.kt:293-318`). One
visual addition: the Classic chip's trailing accent becomes a `home_resume_thumb_size` board
thumbnail of the saved board instead of a generic play arrow, since
`classicResumeLevelId` already identifies the board and the catalogue is static — no new data.
The Challenge chip keeps the play arrow (a procedural level has no memorable footprint).

**4 — Mode tiles.** `ClassicTile` and `ChallengeTile` keep their containers, glyph badges, titles,
supporting slots, entrance stagger, squish, `ButtonForward` cue and every callback
(`HomeScreen.kt:498-560`). Each gains trailing board art at `home_tile_art_size` — a real footprint
for Classic, the bolt for Challenge. Classic's supporting line stays `classic_tile_supporting`
("5 boards").

**5 — Footer.** Empty. Reserved, specified as empty so a later phase does not read the gap as an
invitation.

**Thumb zone.** A weighted spacer sits between bands 2 and 3, so on a tall phone the slack goes
*above* the Continue chips and the mode tiles stay in the lower half. When content exceeds the
viewport the column scrolls instead of compressing. Home does not scroll today
(`HomeScreen.kt:285-292` is a plain `Column`) and at 200% font scale with two Continue chips
present it already overflows — adding `verticalScroll` is part of this change, not optional
polish.

**First-paint rule (D-03f).** Scrolling exists for the overflow case, not as a place to put
things. At **default font scale on a 360×640dp phone with both Continue chips present**, bands 3
and 4 — the Continue chips and both mode tiles — must be **fully within the first viewport with no
scrolling**. The hero is the band that yields: `home_hero_min_height` is a *minimum*, and the hero
must be allowed to compress toward it (art first, then title scale) before anything below it
leaves the fold. A Continue chip that is only reachable by scrolling is a broken oracle, not a
layout compromise.

#### 4.2.1 Load-bearing readouts — do not hide these (D-03f)

Three readouts are the pass/fail oracles of the cloud-save device checklist
(`11-HUMAN-UAT.md`, tests 10/11/13). They are listed here so the developer and QA both treat them
as contract rather than decoration. **None may be moved behind a tap, an expander, a scroll
position, an animation, or a later screen** — and, since 2026-09-21, none may be *written* by the
tutorial either: oracles 1 and 3 are exactly what `GameViewModel`'s `Won` path rewrites, which is
why §5.1 forbids the tutorial from going near it.

| # | Oracle | Where it lives after this refresh | What "survives" means |
|---|--------|-----------------------------------|------------------------|
| 1 | **Challenge ladder position** | The Challenge tile's level chip (`challenge_tile_continue`, "Continue — Level N", `HomeScreen.kt:544-558`) — unchanged — **and** additionally the ribbon's Level pill | The tile chip is the oracle and stays exactly as it is. The Level pill is **additive**; if the two ever disagree the tile is right. Both read the same `challengeCurrentLevel`, so they cannot diverge by construction. |
| 2 | **Classic per-board stars** | The **Classic catalog**, as gold pips under each board preview (`BoardPreview.kt:100-123`, `ClassicBoardUiState.earnedStars`) — this refresh does not touch that screen | Per-board stars have never been on Home and are not being moved there. What must survive is the **path**: the Classic tile remains a single tap from Home to the catalog, and the catalog's pips are untouched. The ribbon's **Stars pill is an aggregate total and is explicitly NOT a replacement for the per-board pips** — do not "simplify" the catalog by pointing at it. |
| 3 | **Continue / resume affordance** | Home band 3, up to two gold chips — behaviour, data and ordering unchanged (`HomeScreen.kt:293-318`) | Visible on first paint without scrolling (first-paint rule above). The added board thumbnail is cosmetic; the chip's presence, its title, its supporting line and its resume target are the oracle. |

**Considered and rejected**, so the trade-off is visible rather than lost:

- *Collapsing the Continue chips into a single "Continue" button that asks which mode.* Rejected:
  it costs a tap, it hides which modes actually have a saved game, and it destroys oracle 3 — the
  device test needs to see *that* a Classic resume and a Challenge resume exist independently,
  which a merged button cannot show.
- *Moving the Challenge level from the tile chip into the ribbon pill only.* Rejected under D-03f:
  the tile chip is the oracle, so the pill is additive and the chip stays.
- *Putting per-board star pips on the Home Classic tile.* Rejected: it needs the same per-board
  fold as the catalog on the cold-start path (D-03g) for information the catalog already shows
  one tap away, and it would create a second place for per-board stars to be wrong.
- *A "recent activity" or "last played" band.* Rejected: it is the Continue chip with worse copy,
  and it needs `completedAt` ordering — a new read on the cold-start path for no new information.

### 4.3 Gameplay status strip — `GameStatusStrip.kt` (CLAS-02, SCOR-02, D-04/D-05a/D-05b)

Replaces `GameTopBar` (`GameScreen.kt:490-527`). A plain `Row`, `game_status_strip_height`,
transparent, `statusBarsPadding()`, `spacing_md` horizontal padding, `CenterVertically`:

| Slot | Content |
|------|---------|
| Leading | Back: `game_status_back_size` clay circular chip, `surfaceContainerHigh`, `clayShadow(elevation_pressed)`, `claySquish()`, `Icons.AutoMirrored.Rounded.ArrowBack` at `top_bar_icon_size`, `contentDescription = navigate_back`, **silent** (D-09) |
| Centre | The context title — `GameTopBarTitle` logic verbatim: `challenge_level_indicator` + its `_desc` for Challenge, `app_name` for Classic (`GameScreen.kt:537-552`). `titleMedium`, `onSurface`, `maxLines = 1`, `weight(1f)`, `semantics { heading() }` |
| Trailing | Timer pill: `game_timer_pill_height`, `surfaceContainerHigh`, `shapes.small`, `GameTimerFormat.format()` in `titleMedium.withTabularFigures()`, `contentDescription = elapsed_time` |

> `elapsed_time` must stay the timer's content description: `GameFlowTest.kt:129` locates it by
> that string.

No actions live here. The strip is status only, which is what lets it be this quiet.

### 4.4 Gameplay action dock — `GameActionDock.kt` (ASST-01, ASST-02, D-04/D-04a–d), and the tutorial's wrong-tap contract (D-01c, D-01h)

> Two unrelated surfaces share this section because the second is a single table: the dock below,
> then the tutorial-only wrong-tap answers at the end. The tutorial has **no dock** (§4.1).

A single clay slab below the board, in the thumb zone: `surfaceContainerHigh`, `shapes.large`,
`clayShadow(elevation_card)`, `game_action_dock_height`, `navigationBarsPadding()`, holding four
targets in a `SpaceEvenly` `Row`, each `game_action_button_size` (56dp) with a minimum
`spacing_sm` gap:

| Order | Control | Glyph | Resting | State channel | Cue |
|-------|---------|-------|---------|---------------|-----|
| 1 | **Undo** | local `UndoIcon` (unchanged) | `primaryContainer` fill, `onPrimaryContainer` glyph | `enabled = canUndo`; disabled → flat `surfaceVariant` fill + `onSurfaceVariant` glyph | `SoundCue.Undo` (existing, ASST-02) |
| 2 | **Hint** | new local `HintIcon` (lightbulb) | `primaryContainer` fill, `onPrimaryContainer` glyph | `solverThinking` → spinner in place of glyph + `enabled = false` + `hint_thinking` | `SoundCue.Hint` (existing) |
| 3 | **Show moves** | new local `ShowMovesIcon` (peg + two dots) | transparent, `outline` border, `onSurfaceVariant` glyph | **ON → filled `primaryContainer` pill + `primary` glyph; OFF → outlined + `onSurfaceVariant`.** Fill, not tint alone (D-04b) | **silent** (D-09: toggles stay quiet) |
| 4 | **Restart** | `Icons.Rounded.Refresh` (unchanged) | transparent, `outline` border, `onSurfaceVariant` glyph | — | **silent** (D-09) |

Every glyph renders at `game_action_glyph_size`. Every content description is the existing string —
`undo_action`, `hint_action`/`hint_thinking`, `show_moves_on`/`show_moves_off`, `restart_board`
(`strings.xml:84-91`, `:14`). **No new copy for the dock.**

> `show_moves_on` must stay the toggle's ON content description: `ButtonSoundTest.kt:226-244`
> locates it by that string.

**The dock never shows an allowance count.** No badge, no "×3", no ad glyph, no allowance-driven
disable. That negative contract is inherited verbatim from the **ad-allowance ruling the code
records at `GameUiState.kt:131-135`** — an earlier phase's `D-05`, not this document's toolbar
D-05 (§3) — and this refresh does not relax it.

**Wrong-tap handling on the board — tutorial only (D-01c).** Three classes, escalating assistance,
never correction:

| Class | Answer |
|-------|--------|
| Tapped a peg with no legal jump | The peg selects and draws zero destination dots. After `TutorialAnim.NUDGE_DELAY_MS` with no progress, the coachable peg's ring re-pulses. **Silent** (D-01h, upheld by D-01i) — the ring and the absent dots are the whole answer, and no `Select` cue fires either. |
| Tapped an illegal destination while a peg is selected | The legal dots re-pop and the selection is kept. Nothing is lost, nothing is announced as wrong. **Silent** (D-01h, upheld by D-01i): the in-game `InvalidTap` cue is deliberately *not* carried into the tutorial, because a rejection sound is the one channel that would make a wrong tap feel like a failure — which D-01c forbids. This is the one reason in D-01h that carried its own weight, and D-01i upholds it without reservation. |
| Three consecutive non-progressing taps in a step | The coach escalates once: the source ring pulses **and** the destination dot brightens **and** the caption re-shows the step's own caption. It never escalates past this, it never blocks, and **it never mentions Undo** — no tutorial board can box the player in, so there is nothing to be rescued from, and promising a rescue from an impossible failure teaches the player to expect failure. |

Expect class 1 to fire on T2 and T3 in normal use: T2's middle peg, and **three of T3's five
pegs** — (1,0), (1,1) and (1,2), since all three of T3's legal openings start at (0,0) or (0,1) —
have no legal jump at the start. That is unavoidable in any peg-solitaire opening (a peg with no
hole two cells beyond a neighbour cannot move), so it is a case QA should see rather than a
defect; §14 criterion 3 names the tap to check.

**Every wrong tap in the table is silent; a *completed* jump is not** (D-01i, §4.1). That
asymmetry is deliberate and is the tutorial's whole audio design: the one sound on the screen
marks the one thing the player got right. It is also why the silences above are checkable
criteria (7d) and owed tests (§13) rather than an absence nobody asserts.

During normal gameplay (outside the tutorial) wrong-tap handling is **unchanged** — the existing
`rejectedTapTick` → `InvalidTap` path (`GameScreen.kt:393-398`), cue included.

### 4.5 Settings — one row added (D-02a)

A `SettingsLinkRow` in `GeneralSection` (`SettingsScreen.kt:193-218`), **above** the Sound toggle:
label `settings_how_to_play_label` ("How to play"), supporting
`settings_how_to_play_supporting` ("Replay the tutorial"), `onClick` → the tutorial,
`SoundCue.ButtonForward` (D-09: a navigational CTA). Full-row target, ≥ `settings_row_min_height`
72dp.

Two mechanical notes for the developer: the section's entrance-order constants are positional, so
inserting a row means renumbering the cascade; and `settings_sound_supporting` is stale copy
("Saved for the upcoming sound effects", `strings.xml:143`) — sound shipped in Phase 7, and this
refresh should fix it to describe what the toggle now does.

---

## 5. Deliberate non-UI (negative contract)

Things a reader might expect from this spec and will not find, each on purpose:

- **No second control surface on the gameplay screen.** One status strip, one action dock, nothing
  else. The rescue banner, the ad-offer dialog, the restart confirm and the snackbar are the
  screen's only other surfaces and all four are unchanged.
- **No M3 `TopAppBar`, `BottomAppBar` or `NavigationBar` anywhere in this refresh** (D-05c).
- **No confirm-quit sheet on gameplay back.** Progress is already snapshotted for resume
  (PERS-03), so leaving loses nothing, and there is no `BackHandler` on gameplay to hang one from.
- **No allowance counters, badges or ad affordances** on the dock (§4.4).
- **No Play Games chip on Home** (D-03e).
- **No tappable progress pills** (§4.2).
- **No loading state, skeleton or spinner on Home.** The ribbon paints placeholders and fills;
  first paint never waits (D-03g, §6.5).
- **No per-board star pips on Home.** They stay on the Classic catalog, untouched (§4.2.1).
- **Nothing on Home collapsed behind a tap or an expander.** All three device-test oracles are
  visible on first paint (D-03f).
- **No tutorial step persisted across process death** (D-01e) — and specifically **no
  `SavedStateHandle` step cursor**, which would survive process death and resume at T2, silently
  contradicting D-01e. Forbidding the cursor is D-01g's clause; the decision it protects is
  D-01e's.
- **No sound in the tutorial beyond the jump pair and the two exit presses** (D-01h as amended by
  D-01i). The enumerated contract, both halves, because only an enumerated one is checkable:
  - **Plays:** `SoundCue.Jump` (pitched by `playJump(runIndex)`) with `SoundCue.PegRemove`
    layered under it, on a **completed** jump; `SoundCue.ButtonForward` on the completion CTA;
    `SoundCue.ButtonDismiss` on `Skip`.
  - **Never plays:** `Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint` on the board, and
    `StarPop` and `Win` on the completion star — at any point, on any step, on either entry path.
    System back is silent too.
  - **No `SoundCue` reference is added to `BoardCanvas`** — that file has none today and gaining
    one would put audio on the shared board component for every screen at once. The jump effect
    is a composable declared *beside* the screen and lifted from `GameScreen.kt:320-330`, never
    moved into the canvas.
- **No Undo control, and no copy naming Undo, anywhere in the tutorial** (§4.1, §4.4). The
  tutorial's bands are Header / Board / Caption / Footer; there is no dock to hold one, the
  boards make being stuck impossible, and Undo is met in the real game where it is genuinely
  useful.
- **No new audio asset.** Every cue this refresh needs already exists in `SoundCue` (§9).
- **No third-party art, font or icon asset** (§9).
- **No change to any rule, footprint, star band, score formula or generator.** Board *content* for
  all three tutorial steps is the game-designer's ruling (§4.1, §7 R-4), not a UX decision, and
  **`TutorialBoards` is never added to `Boards.CLASSIC_SPECS`** — that list is the Classic
  catalogue and the denominator of the Home Boards pill.

### 5.1 The tutorial's hard prohibitions (D-01g) — requirements, not inferences

These four are stated as a contract because the failure they prevent is silent, expensive and
**not** recoverable by the player. They bind the developer and the reviewer equally; each is
checkable by one grep over `app/src/main/.../ui/onboarding/`.

| # | Prohibition | Why it is a contract and not a preference |
|---|-------------|-------------------------------------------|
| 1 | The tutorial hosts **`BoardCanvas`** and **never `GameScreen`** | `GameScreen` owns every sound cue (`GameScreen.kt:327-428`) and the whole gameplay chrome; hosting it would drag the dock, the banner, the dialogs and `SoundCue.Win` into a teaching screen. |
| 2 | The tutorial's state holder is **`TutorialViewModel`** and **never `GameViewModel`** | `GameViewModel`'s `Won` path does three irreversible things: it emits `finished` → Results, it **clears the mode's resume snapshot** (`GameViewModel.kt:963`) and it **advances the Challenge ladder** (`:973`). Every tutorial step ends on exactly one peg, so the path would fire **three times per run**. |
| 3 | The tutorial writes **no `LevelResult`**, clears **no resume snapshot** and calls **no ladder advance** | This is prohibition 2's consequence, stated separately because it is the damage: a first-run tutorial on a player who already has a saved Challenge game would delete their Continue chip and push their level counter forward three times. Two of the three §4.2.1 device oracles are written on that path, so the loss is not only the player's — it makes `11-HUMAN-UAT.md` tests 10/11/13 **unverifiable**, and §4.2.1 exists precisely to keep them verifiable. |
| 4 | The tutorial has **no route to Results** | Its only outbound edge is `popBackStack()` (D-01g). A tutorial that can reach the Results screen can bank a star, and a banked tutorial star corrupts the Home Stars pill (§4.2) on the player's very first launch. |

A tutorial that trips any of these looks correct on screen and is wrong in the database. That is
the failure class this section exists to name.

**What these four do not forbid — clarified 2026-09-21, not relaxed (D-01i).** They bind **where
the tutorial's screen and its state holder come from**, not **where a `@Composable` effect is
declared**. `JumpSound` (`GameScreen.kt:320-330`) takes a `MoveTransition?` and reads
`LocalSoundPlayer`; it touches no `GameViewModel`, no repository, no navigation and no
persistence. Calling a shared `internal` copy of it from the tutorial leaves all four intact: the
tutorial still hosts `BoardCanvas` and never `GameScreen` (1), still runs `TutorialViewModel` and
never `GameViewModel` (2), still writes no `LevelResult`, clears no snapshot and advances no
ladder (3), and still has `popBackStack()` as its only outbound edge (4). Prohibition 1's stated
reason — that hosting `GameScreen` would drag the dock, the banner, the dialogs and `SoundCue.Win`
into a teaching screen — is unchanged and unweakened by this; **lifting one effect out of that
file is not licence to host the file**, and the grep gate over
`app/src/main/.../ui/onboarding/` still applies verbatim, now reading "references no `SoundCue`
the spec does not name" (§13) rather than "references no `SoundCue`".

---

## 6. Design tokens

### 6.1 New `dimens.xml` tokens

Following the file's own one-token-per-owner convention (`dimens.xml:113-132`), each new token is
declared even where an existing token shares its value. All multiples of 4.

| Token | Value | Owner |
|-------|-------|-------|
| `tutorial_board_size` | 280dp | The tutorial's interactive board slab — the largest footprint is 3×3, so ≈93dp cells, well above §11's 48dp floor |
| `tutorial_caption_min_height` | 48dp | Reserved caption height (1↔2 line swap must not reflow the board) |
| `tutorial_step_pip_size` | 12dp | One progress pip |
| `game_status_strip_height` | 56dp | The gameplay status strip |
| `game_status_back_size` | 48dp | The status strip's back chip |
| `game_timer_pill_height` | 32dp | The status strip's timer pill |
| `game_action_dock_height` | 72dp | The gameplay action dock slab |
| `game_action_button_size` | 56dp | One dock action target |
| `game_action_glyph_size` | 28dp | One dock action glyph |
| `home_hero_min_height` | 160dp | The Home hero band |
| `home_hero_art_size` | 120dp | The Home hero's decorative board art |
| `home_stat_pill_min_height` | 64dp | One progress-ribbon pill |
| `home_stat_pill_glyph_size` | 20dp | One progress-ribbon pill glyph |
| `home_resume_thumb_size` | 40dp | The Classic Continue chip's board thumbnail |
| `home_tile_art_size` | 56dp | A mode tile's trailing board art |

**Reused unchanged:** `spacing_xs/sm/md/lg/xl`, `content_max_width`, `mode_tile_min_height`,
`resume_tile_min_height`, `settings_row_min_height`, `settings_row_icon_size`, `top_bar_icon_size`,
`elevation_card`, `elevation_pressed`, `elevation_dialog`, `entrance_slide_distance`,
`mode_glyph_size`, `blob_size`, `blob_drift_range`, `peg_selected_ring_width`, all five
`shape_corner_*`.

**Retired:** `top_bar_touch_target` (48dp) and `onboarding_illustration_size` (160dp) lose their
last callers. `onboarding_illustration_size` never took effect in the first place
(`BoardPreview.kt:79` overrides it) — deleting it removes a token that has been lying about the
layout since Phase 2.

### 6.2 Colour

**No new colour, no new gradient, no hex.** Every surface here maps to a role already in
`docs/ui/DESIGN-SYSTEM.md`:

| Surface | Light / Dark role |
|---------|-------------------|
| Status strip, action dock, stat pill, tutorial slab | `surfaceContainerHigh` on `background` |
| Dock loud actions (Undo, Hint) | `primaryContainer` / `onPrimaryContainer` |
| Dock quiet actions (Show-moves OFF, Restart) | transparent + `outline` border, `onSurfaceVariant` glyph |
| Show-moves ON | `primaryContainer` fill + `primary` glyph |
| Disabled Undo | `surfaceVariant` fill + `onSurfaceVariant` glyph |
| Hero title, tutorial CTA | `ctaGradientBrush()` with `onPrimary` label |
| Stars pill glyph, done pips, completion star | `goldGradientFill()` — `tertiary` stays reserved for hint/reward semantics |
| Tutorial coach ring | `primary` (the existing selection-ring role) |
| Captions, supporting lines | `onSurfaceVariant` |

### 6.3 Typography

M3 roles only, per the Fredoka/Nunito mapping in the design system. Home title `displaySmall`;
gameplay title and timer `titleMedium`; stat-pill values `titleMedium`; stat-pill labels
`labelMedium`; tutorial caption `bodyMedium`; all button labels `labelLarge`. Timer and every
numeric stat value use `withTabularFigures()` so digits never shift width.

### 6.4 Motion

New constants go in a scoped `TutorialAnim` object (unitless ms/floats, so not `dimens.xml`):
`STEP_ADVANCE_DELAY_MS`, `NUDGE_DELAY_MS`, `COACH_PULSE_MS`. Everything else reuses `ClayAnim` and
`GameAnim` unchanged. See §10.

### 6.5 Cold-start budget for Home (D-03g)

**PLAT-08 C1 is a measured pass/fail at ≤ 950 ms cold start, and Home is the start destination**
— so everything added to Home in §4.2 is on the critical path. The budget below is part of the
design, not a review note.

**Hard rules for Home's first frame.** No blocking read. No image decode. No asset load. No
bitmap. No new SDK call. Every visual this refresh adds is one of:

| Added element | First-frame cost | Verdict |
|---------------|------------------|---------|
| Hero title (gradient text) | Brush + existing `displaySmall` role | Negligible — replaces a `TopAppBar` title that is itself being deleted |
| Hero board art | One `BoardCanvas` draw of a static footprint — vector geometry, no decode | Low |
| Ribbon pills (3) | Three `Surface`s + existing `ImageVector` glyphs | Negligible |
| Ribbon **values** | **One single-row SQL aggregate behind a `Flow`** (§7 R-1/R-2, D-03h) | **The one remaining risk — see below** |
| Continue chip thumbnail | One `BoardCanvas` draw, only when a Classic resume exists | Low, and conditional |
| Mode tile art (2) | Two `BoardCanvas` draws | Low |
| `verticalScroll` on the column | Scroll state allocation | Negligible |

Net: Home's first frame gains up to **four `BoardCanvas` draws** (hero, thumbnail, two tiles) of
static geometry, and **one new single-row Room query** behind a Flow.

**The one remaining risk, and its mitigation.** R-1/R-2 add a `level_result` read to app start.
**D-03c's placeholder rule is the mitigation, and it is mandatory, not cosmetic:** the ribbon
paints `stat_value_unknown` ("—") on the first frame and fills on the collector's first emission.
The developer must **not** gate first paint on the query, must not `runBlocking` it, must not
`first()` it on the start path, and must not introduce a Home-wide loading state to accommodate it
— a spinner on the start destination would trade a measured gate for a worse one. The pills are
sized from their labels, not their values, so the fill causes no reflow.

The *unbounded* version of this risk is retired by D-03h: the read is one aggregated row whatever
the player's Challenge history, so it can no longer be cheap in the lab and expensive on the
players who have played most. What remains is an ordinary asynchronous query, and the placeholder
rule is what keeps it off the first frame.

**Ordered degradation path if measurement shows a C1 regression.** Drop in this order, stopping as
soon as C1 passes; each step is a visual loss only and costs no oracle (§4.2.1) and no function:

1. **Mode tile art** (2 draws) — the tiles already have glyph badges and read fine without it.
2. **Continue chip thumbnail** (1 conditional draw) — reverts to the existing play-arrow accent.
3. **Hero board art** (1 draw) — reverts to Q-901 option 3, the gradient title alone.
4. **Ribbon values** — the pills stay and show "—" permanently until a later phase moves the read
   off the start path. Do **not** delete the ribbon; the Level pill costs nothing (`HomeUiState`
   already carries it) and the band is what answers the founder's "looks empty".

**Nothing in the degradation path touches the Challenge tile chip, the Continue chips themselves,
or the Classic catalog** — those are D-03f oracles and are not available to trade.

**Handoff:** this is a design budget, not a measurement. The developer measures C1 with the fuller
Home before the phase closes; the qa-engineer checks the numbers at the gate.

**How C1 is measured for this phase** *(2026-09-21 — tech-lead ruling,
`tech-lead-architecture.md` §3, folded in)*. The Phase-6 reference of 633 ms is **pre-SDK**: UMP,
AdMob, Play Games, leaderboards and cloud save all landed after it, so a Phase-15 developer who
compared against it would be charging five phases of SDK cost to a UX refresh. Phase 15's own gate
is therefore a **two-arm delta**, and the absolute ≤ 950 ms stays PLAT-08's:

- **Two arms, same AVD, same host, same sitting, back to back:** arm A = `develop` (control),
  arm B = `feature/phase-15-ux-refresh`. Phase 6's instrument exactly — AVD `Pixel_9a` API 36,
  **debug** variant, `am start -W` TotalTime, 10 samples, first discarded as warm-up, re-install
  between arms so neither benefits from the other's warm ART state.
- **The player state is identical across arms and deliberately non-trivial:** seed a Challenge
  ladder of at least ~200 banked levels, or the one read this phase adds is measured on an empty
  table and proves nothing about a real player.
- **Pass condition, both parts:** (1) after-median ≤ **950 ms**, and (2)
  (after-median − before-median) ≤ **50 ms**. 50 ms is this instrument's noise floor on this AVD,
  not a guess — Phase 6's nine samples spanned 603–649 ms around a 633 ms median.
- **If (2) fails**, walk the ordered degradation path above and re-measure after each step,
  stopping at the first pass. **If (1) fails while (2) passes**, the regression is not this
  refresh's: it escalates to the producer as PLAT-08/Phase-14 work rather than being paid for out
  of this phase's visual budget.
- **Where the numbers go:** `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, section
  `## Cold-start measurement`, one table per arm in the identical field shape as
  `06-BUILD-EVIDENCE.md:315-323`, plus the seeded-ladder row and the same precision caveat. The
  phase does not close without both tables. Criterion 15f (placeholders, no spinner, no reflow) is
  a separate visual check at the same sitting — the timing number does not cover it.

---

## 7. Data requirements (for the tech-lead and developer)

Nothing here needs a schema change, a migration, a new DataStore key or a DI change. That sentence
survives D-03h unchanged — a new `SELECT` over existing columns is not a schema change, and the
exported Room schema and its version are untouched. What D-03h does cost, stated so the planner is
not surprised by it: one new `@Query`, one two-field projection, one new `ProgressRepository`
member — and therefore a one-line override in each of the repository's test doubles, of which the
tech-lead counted 16. That cost is mechanical and the compiler names every site.

| # | Requirement | Why it is cheap |
|---|-------------|-----------------|
| **R-1** ⚠️ | `HomeUiState` gains `totalStars: Int?`. `HomeViewModel` reads `ProgressRepository.observeHomeProgress()` — a `Flow` over **one** SQL aggregate (`LevelResultDao.observeHomeProgress`) that computes both ribbon values in a single row. | `ProgressRepository` is already injected (`HomeViewModel.kt:86`); the aggregate is the `Flow` sibling of the existing `totals()` shape. `null` until the first emission → the pill shows "—" (D-03c), and a **failed** read also emits unknown, so the pill never shows `0` for a broken read (D-03h). **⚠️ COLD-START (D-03g):** this is the only new read on the start destination. Nullable-and-placeholder is mandatory — no `runBlocking`, no `first()` on the start path, no Home-wide loading state. Measure C1 per §6.5. |
| **R-2** ⚠️ | `HomeUiState` gains `classicBoardsCleared: Int?`, from the **same single query**, whose `classicBoardsCleared` field is mode-filtered and counts rows with `stars >= ProgressConstants.ANY_STARS`. Denominator is the 5 `ClassicBoard` enum values / `Boards.CLASSIC_SPECS.size` — never a stored count. | One query, two fields, one collector: do not add a second collection for the second pill. **Must not** be a Kotlin fold over `observeResults` — over `MODE_CHALLENGE` that is the whole unbounded ladder in memory on the start destination, which `LevelResultDao.kt:41-47` already rules out for a *background* submitter (D-03h). **Must not** use `aggregate().totalStars` — deliberately both-modes with no `WHERE` (`LevelResultDao.kt:66`) and a `suspend` one-shot that goes stale after every solve. **⚠️ Same cold-start note as R-1.** |
| **R-3** | `HomeScreen` loses the `onShowHowToPlay` parameter; `SettingsScreen` gains an `onShowHowToPlay` parameter; `AppNavHost` routes the tutorial from Settings and from first run. | Mechanical. `HomeRoute.kt:48` moves to the Settings route. |
| **R-4** | **Closed 2026-09-21** (game-designer ruling). All three footprints are fixed in §4.1 and proven dead-end-free to one peg. They live in a new `object TutorialBoards` in `:domain` beside `Boards.kt` and **must not** join `Boards.CLASSIC_SPECS`. | The proof is exhaustive enumeration over the product's own `RulesEngine`; the invariant is guarded by a permanent `:domain` test, not by a report (§13). |
| **R-5** | **Resolved 2026-09-21** (tech-lead ruling): a Hilt `TutorialViewModel` scoped to the `Tutorial` nav destination; step state in a plain `MutableStateFlow`, **not** `SavedStateHandle`. | A saved cursor would survive process death and resume at T2, contradicting D-01e. Destination scoping is also what makes a Settings replay restart at T1 (§4.1, D-01g). |
| **R-6** | The two derived pill values reach the UI as a UI-layer read model, the way `LevelResult` does today — no Room type reaches a ViewModel. | Stated so the ribbon's contract survives a refactor of the query behind it. |

**Invariant the hosting mechanism must preserve (D-01f).** `HomeViewModel` sequences three
first-run surfaces — onboarding, then the Play Games opt-in prompt, then the profile-swap dialog —
and the latter two are gated on the **persisted** `onboardingSeen` flag, not on overlay visibility
(`HomeViewModel.kt:119-128`, `:145-160`). Moving the tutorial off Home therefore preserves the
sequencing by construction, **provided `markSeen()` is still written before the player returns to
Home**. `HomeScreen.kt:198`'s `!state.onboardingVisible` guard loses its meaning once Home hosts no
overlay; whatever replaces it must keep the same one-way safety — the wipe question may be
withheld, never raised early.

**Resolved** *(2026-09-21 — tech-lead ruling)*: the tutorial is a `@Serializable` nav destination;
`markSeen()` is **awaited before the pop**, issued on the application scope so a cancelled screen
cannot drop it; and the guard survives in shape as a standing boolean on `HomeUiState`
(`!state.tutorialPending`, the mirror of `!seen`). The player-visible reason this ordering is
binding is in D-01g: a fire-and-forget write that loses its race leaves `tutorialPending` true, so
Home re-pushes the tutorial on the next composition and the player cannot get out — and on the
launch after a dropped write, both the Play Games opt-in and the profile-swap question are
withheld for an extra launch.

---

## 8. Copywriting contract

### New strings

| Name | Value | Where |
|------|-------|-------|
| `tutorial_step_progress` | `Step %1$d of %2$d` | Pip row content description |
| `tutorial_step_1_caption` | `Tap a peg, then tap the empty hole two cells away.` | T1 |
| `tutorial_step_2_caption` | `Jumps work up, down, left and right — never diagonally.` | T2 |
| `tutorial_step_3_caption` | `Keep jumping. Clear the board down to one peg.` | T3 |
| `tutorial_done_caption` | `That's the whole game. Go clear a board.` | Completion |
| `settings_how_to_play_label` | `How to play` | Settings row |
| `settings_how_to_play_supporting` | `Replay the tutorial` | Settings row |
| `stat_value_unknown` | `—` | A ribbon pill with no value yet — or whose read failed (D-03h); the two render identically and neither ever renders `0` |
| `stat_value_unknown_desc` | `not yet available` | Its announcement |
| `home_stat_stars_label` | `Stars` | Ribbon pill label |
| `home_stat_boards_label` | `Boards` | Ribbon pill label |
| `home_stat_level_label` | `Level` | Ribbon pill label |
| `home_stat_boards_value` | `%1$d/%2$d` | Boards pill value |

The three new tutorial captions are **rewrites** of `onboarding_step_1/2/3`
(`strings.xml:161-163`) shortened to one line each, because the player is now doing the thing the
sentence used to describe. `tutorial_step_2_caption` ("never diagonally") is **earned** by T2's
footprint: the full 3×3 puts four diagonal holes and two adjacent holes visibly in play and gives
none of them a destination dot, so the sentence describes something the player can see withheld.

**Retired before it shipped.** `tutorial_undo_nudge` ("Boxed in? Undo takes your last jump back.")
was in the first edition's table and is **gone** — the tutorial has no Undo control to point at,
and the game-designer's boards make being boxed in impossible, so the line named a control the
player cannot see and a situation that cannot occur. The escalation re-shows the step's own
caption instead (§4.4), so the copy contract carries one string fewer and needs no per-step
escalation line.

### Reused unchanged

`onboarding_got_it` (the completion CTA), `onboarding_skip` (the Skip action), `app_name`,
`navigate_back`, `elapsed_time`, `challenge_level_indicator`, `challenge_level_indicator_desc`,
`loading_level`, `undo_action`, `hint_action`, `hint_thinking`, `show_moves_on`, `show_moves_off`,
`restart_board`, `classic_tile_title`, `classic_tile_supporting`, `challenge_tile_title`,
`challenge_tile_continue`, `resume_classic_title`, `resume_classic_supporting`,
`resume_challenge_title`, `resume_challenge_supporting`, `settings_action`, `catalog_stars_earned`.

### Deleted

`how_to_play` (`strings.xml:62`) — its only caller is the deleted info button. Removing it is what
makes D-02 complete rather than cosmetic.

### Fixed

`settings_sound_supporting` — "Saved for the upcoming sound effects" is stale since Phase 7.

---

## 9. Assets and licences

Every asset this refresh needs, with its licence. **No third-party asset is introduced, so no new
`THIRD_PARTY.md` or `docs/audio/CREDITS.md` row is required.**

| Asset | Kind | Source | Licence |
|-------|------|--------|---------|
| `Icons.AutoMirrored.Rounded.ArrowBack` | vector | `material-icons-core`, already a dependency | Apache-2.0 (notice already shipped) |
| `Icons.Rounded.Refresh` | vector | `material-icons-core` | Apache-2.0 |
| `Icons.Rounded.Settings` | vector | `material-icons-core` | Apache-2.0 |
| `Icons.Filled.Star` | vector | `material-icons-core` | Apache-2.0 |
| `UndoIcon` | vector | in-house, `ui/game/UndoIcon.kt` (existing) | project-owned |
| `rememberClassicGlyph`, `rememberChallengeGlyph` | vector | in-house, `ui/home/ModeGlyphs.kt` (existing) | project-owned |
| **`HintIcon`** (lightbulb) | vector | **new, in-house** — core set has no lightbulb and the extended set is banned | project-owned |
| **`ShowMovesIcon`** (peg + two dots) | vector | **new, in-house** — core set has no visibility glyph | project-owned |
| Hero / tile / thumbnail board art | drawn | `BoardCanvas` primitives, no asset file | project-owned |
| `SoundCue.Jump` (`R.raw.sfx_jump`, `SoundCue.kt:30`) + `SoundCue.PegRemove` (`R.raw.sfx_peg_remove`, `:33`) | audio | already in `res/raw`, credited in `docs/audio/CREDITS.md` — **newly placed on the tutorial screen** by D-01i, as the existing pair at existing levels; no asset, no volume and no pitch value changes | CC0 1.0 (as recorded) |
| `SoundCue.ButtonForward`, `SoundCue.ButtonDismiss` (`SoundCue.kt:70-74`) | audio | already in `res/raw`, credited — newly placed on the Settings "How to play" row (§4.5, `ButtonForward`), the tutorial's completion CTA (`ButtonForward`) and `Skip` (`ButtonDismiss`), all by the existing `withClickSound` idiom (D-01i, D-11) | CC0 1.0 (as recorded) |
| `SoundCue.Select`, `Deselect`, `Undo`, `InvalidTap`, `Hint`, `Win`, `StarPop` | audio | already in `res/raw`, credited — **unchanged, and explicitly never played on the tutorial screen** (D-01h as amended by D-01i, §5) | CC0 1.0 (as recorded) |
| Fredoka, Nunito | font | downloadable Google Fonts, already wired | OFL (already shipped) |

Both new vectors follow the `UndoIcon.kt` / `ModeGlyphs.kt` precedent: a 24-unit viewport, filled
opaque black, recoloured by the host `Icon`'s tint, `@Suppress("MagicNumber")` on the path data
with the same justification comment the existing two carry.

---

## 10. Animation and motion

All new motion reuses the Candy Clay vocabulary; nothing invents a curve.

| Moment | Motion |
|--------|--------|
| Any tappable clay surface pressed | `Modifier.claySquish()` — unchanged |
| Home bands entering | `Modifier.clayEntrance(order)`, 40ms stagger: hero 0, ribbon 1, Continue chips 2..3, tiles next |
| Dock entering with the board | `clayEntrance` after the board slab |
| Show-moves toggling ON/OFF | pill fill cross-fades over `ClayAnim.PRESS_IN_MS` |
| Tutorial coach ring | the **existing** selection pulse (`BoardMetrics.PULSE_MAX_SCALE`, `GameAnim.PULSE_MS`, reverse-repeat) — the tutorial reuses the board's own idiom rather than adding a highlight |
| Tutorial step advance | caption cross-fade + pip fill, then the next footprint after `TutorialAnim.STEP_ADVANCE_DELAY_MS` |
| Tutorial nudge escalation | one `TutorialAnim.COACH_PULSE_MS` pulse, `TutorialAnim.NUDGE_DELAY_MS` after the last non-progressing tap |
| Tutorial completion star | `ClayAnim.STAR_POP_MS` + `overshootSpring()` + `goldGradientFill()` |
| Hero board art | idle — no ambient motion of its own; the two existing `HomeBlobs` remain the only drifting elements |

**Reduced motion is a hard requirement.** Every decorative animation above is gated on
`rememberAnimationsEnabled()` (`Clay.kt:209-218`), which reads
`Settings.Global.ANIMATOR_DURATION_SCALE`. When it returns `false`:

- `claySquish` is already a no-op (`Clay.kt:152-153`) and `clayEntrance` already settles instantly
  (`Clay.kt:185`) — inherited, nothing to add.
- The tutorial coach pulse, the star pop, the caption cross-fade and the pill cross-fade **do not
  run**; the ring, the star, the caption and the fill appear settled.
- `TutorialAnim.STEP_ADVANCE_DELAY_MS` still applies. It is a **functional** beat — the player must
  see their jump resolve before the board is replaced — not decoration, and collapsing it would
  make the tutorial swap boards under the player's finger.
- The board's own jump/removal already resolve instantly in this mode
  (`BoardCanvas.kt:298-304`), so the tutorial inherits correct behaviour without a second gate.
- **Sound is not motion and is not gated on it.** The tutorial's jump pair (D-01i) still plays
  with animations off: `JumpSound` reads no animation flag (`GameScreen.kt:320-330`), and the
  project's precedent is that cues survive reduced motion (`ResultsCelebration.kt:104-106`
  consults `rememberAnimationsEnabled()` only to change the star stagger and still plays every
  cue). The one thing reading cannot prove is whether the keyed effect wins the race against the
  snap path at `BoardCanvas.kt:298-304`, which sets and clears `animating` in a very short
  window — so criterion 7c is exercised once with OS animations off, on a device.

---

## 11. Accessibility

Checklist for `android-compose-game` (rules/compose.md + the android-accessibility audit), stated
so QA can check each line on a device.

**Touch targets.** Every interactive element ≥ 48dp with ≥ 8dp spacing. Dock actions are 56dp
(`game_action_button_size`) with ≥ `spacing_sm` gaps — the largest targets in the app, deliberately,
because they sit under the thumb during play. The back chip is 48dp
(`game_status_back_size`). `Skip` is a `TextButton`, which applies
`minimumInteractiveComponentSize()`. Settings rows are full-width at ≥ 72dp. Mode tiles are ≥ 96dp
and tappable across their whole surface. The board's own hit-testing is unchanged
(`board_min_touch_target`).

**Labels.** Every interactive element carries a `contentDescription` or a text label; every
decorative element carries `contentDescription = null`. Decorative: the hero board art, the tile
art, the Continue thumbnail, the ribbon pill glyphs (the pill speaks as one node), the individual
step pips (the row speaks as one node), the completion star. Labelled: back, Settings gear, all
four dock actions, Skip, the completion CTA, the Settings row, both mode tiles, both Continue
chips, the timer.

**State, not just colour.** No control conveys state by hue alone.

| Control | Channels |
|---------|----------|
| Show-moves ON/OFF | **fill** (filled pill vs outlined) + tint + `show_moves_on`/`show_moves_off` |
| Undo enabled/disabled | `enabled` (TalkBack announces "disabled") + fill + tint |
| Hint idle/thinking | glyph→spinner swap + `enabled` + `hint_action`/`hint_thinking` |
| Step pips | **filled vs outlined** + `tutorial_step_progress` |
| Ribbon pill known/unknown | value text ("7" vs "—") + `stat_value_unknown_desc` |

This is a fix, not a restatement: `HintToggleControl.kt:45-51` carries its state in tint alone
today.

**Contrast.** ≥ 4.5:1 for all body and label text and ≥ 3:1 for glyphs and boundaries, in **both**
light and dark. The pairs in use are all from the design system's verified set. Two to check
explicitly on device because they are new pairings: `onPrimaryContainer` glyph on
`primaryContainer` fill (dock loud actions), and `onSurfaceVariant` glyph on `surfaceVariant` fill
(disabled Undo — perceivable even though WCAG exempts disabled controls). The gradient CTA label
sits on the gradient's dark half by construction (`Clay.kt:91-103`).

**Text scaling.** Every size is an M3 type role, so all text scales with the system font size.
Four layouts must be verified at 200%: the Home column (scrolls — §4.2), the tutorial column
(also scrolls, board unshrunk — §4.1), the three-pill ribbon
(each pill wraps its label under its value rather than truncating), and the gameplay status strip
(the title is `maxLines = 1` with `weight(1f)` and truncates before it can push the timer pill off
screen — the timer must never be clipped, since it is the only place elapsed time appears).

**Screen-reader traversal.** Tutorial order: pips → Skip → board → caption → CTA. Gameplay order:
back → title → timer → board → dock (Undo → Hint → Show-moves → Restart) → any raised banner.
Home order: title → gear → ribbon pills → Continue chips → mode tiles. Titles carry
`semantics { heading() }` so a screen-reader user can jump bands.

**Sound is never the only feedback** (rules/audio.md). Every cue in this refresh is paired: the
jump has the slide and the removal; selection has the ring and the pulse; the invalid tap has the
dots staying put; Undo has the board stepping back; the completion cue has the star and the
caption. The dock's two silent controls (Show-moves, Restart) are silent by contract (D-09) and
both give immediate visual feedback. **The tutorial passes this rule twice over** (D-01h as
amended by D-01i): its one board cue — the jump pair on a completed jump — is paired with the
slide and the removal fade exactly as in the real game, and every event it deliberately leaves
silent (select, deselect, invalid tap, the completion star) carries visual feedback on its own,
so no information there is audio-only. Note the direction of the rule: it forbids sound as the
*sole* channel; it never requires silence. QA should read the absent select, deselect and
invalid-tap cues as the design and the present jump cue as required by SOUND-01 (§14, criteria
7c/7d/7e).

**One-handed use.** The four in-game actions and both Home mode tiles sit in the lower half of a
6.7-inch phone. This is the reach problem D-04 exists to fix.

---

## 12. Responsiveness

| Case | Behaviour |
|------|-----------|
| Narrow phone (≤ 360dp) | Home column scrolls; ribbon pills shrink to equal weights, never wrap to two rows; gameplay title truncates before the timer clips |
| Tall phone (6.7") | Home's weighted spacer pushes slack above the Continue chips so mode tiles stay low; the gameplay board centres between strip and dock |
| Short phone / 200% font | Home scrolls; the tutorial column scrolls with the board keeping `tutorial_board_size` and its aspect ratio (§4.1) — the caption grows and the empty footer leaves the fold, the board does not shrink; the dock stays pinned below the board |
| Tablet / large | Content capped at `content_max_width` 480dp and centred — unchanged; board capped at `board_max_size` 480dp — unchanged |
| Landscape | Not a supported orientation change in this refresh; existing behaviour preserved. Flagged to the tech-lead if the manifest does not lock orientation. |
| RTL | The back arrow and `UndoIcon` are already auto-mirrored (`UndoIcon.kt:29`); the status strip and dock are `Row`s and mirror by default; nothing uses absolute left/right padding |
| Dark theme | Every role above has a dark variant in the design system; both themes are in scope for every new composable's `@Preview` pair |

---

## 13. Test impact (read this before writing code)

Existing tests this refresh **will** break, with the fix:

| Test | Why it breaks | Fix |
|------|---------------|-----|
| `ButtonSoundTest.kt:105-123` `theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound` | Clicks `R.string.how_to_play` by content description; that button and that string are deleted (D-02) | Retarget to the Settings row; the gear half of the assertion stays valid |
| `HomeScreenTest.kt:148-159` `theProfileSwapDialogIsNeverStackedOnTheOnboardingOverlay` | Locates the overlay by the hardcoded literal `"Got it"` (`:367`) and the overlay stops being hosted by Home (D-01f/D-01g) | **Retarget, never delete:** `theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending`, finding the dialog by `PROFILE_SWAP_DIALOG_TEST_TAG` as today and the pending state by the state field — never by button copy |
| `ButtonSoundTest.kt:226-244` | Clicks `navigate_back` and `show_moves_on` on the game screen | **Passes unchanged** — both content descriptions are deliberately preserved (D-05b, §4.4) |
| `GameFlowTest.kt:129` | Finds the timer by `elapsed_time` | **Passes unchanged** — deliberately preserved (§4.3) |

**Device checklist that must keep working.** `11-HUMAN-UAT.md` tests 10, 11 and 13 read their
pass/fail off the three Home readouts in §4.2.1. This refresh is designed so those tests run
**unchanged** — the Challenge tile chip, the Continue chips and the Classic catalogue's star pips
are all untouched in behaviour. If a plan proposes changing any of the three, it is changing a
test oracle and the qa-engineer and release-manager need to know before, not after. The other way
those three tests can break is not a layout change at all: a tutorial routed through
`GameViewModel` would rewrite two of them from underneath. §5.1 is why that is a prohibition, and
criterion 7a is the device check for it.

Two gaps QA should close rather than inherit:

- **The tutorial has no test of its own.** There is no `OnboardingOverlayTest.kt`; the only
  assertion that touches it is the incidental one above, and the captions, the skip action and the
  illustrations are asserted nowhere. An interactive tutorial needs a real Compose UI test per
  rules/testing.md ("one UI test per screen covering its main flow").
- **The overlay carries no `testTag`**, unlike `PROFILE_SWAP_DIALOG_TEST_TAG` and
  `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG`. The replacement should carry one so no future test has to
  find a screen by its button copy.

**What this refresh owes in tests, beyond the two retargets** *(2026-09-21 — tech-lead and
game-designer rulings, folded in; the list is theirs, it is repeated here because it is the answer
to the two gaps above)*:

- Every new or replaced surface carries a `testTag` constant — `TUTORIAL_SCREEN_TEST_TAG`,
  `HOME_PROGRESS_RIBBON_TEST_TAG`, `GAME_STATUS_STRIP_TEST_TAG`, `GAME_ACTION_DOCK_TEST_TAG` — in
  the same idiom as the two existing dialog tags. This is what stops the next test finding a
  screen by its copy.
- The two breaking tests are **retargeted, never deleted and never `@Ignore`d**.
- One Compose UI test per changed screen's main flow, the tutorial included.
- A `:domain` test that every legal line of every tutorial board ends on exactly one peg, over
  `TutorialBoards.ALL`, plus the two axis guards (T1 horizontal-only, T2 vertical-only) — these
  are the unit-level guard for §14 criterion 2, which otherwise has only a device check.
- The ordering test for the exit path (the seen flag is persisted *before* the exit signal) and
  the "every exit route persists exactly once" test — criterion 5's real teeth.
- A denominator guard that the Boards pill's denominator is the Classic catalogue size (5), so a
  tutorial board joining `CLASSIC_SPECS` cannot change a shipped number silently.
- A placeholder test that a ribbon pill with no value yet shows "—", and a repository test that a
  failed read degrades to unknown rather than to `0` (D-03h).

**Four audio tests** *(2026-09-21 — added with D-01i; the list above carried none, and criterion
7c had a device check and nothing else)*. The project's own rule is that a named silence is only
a real property when both halves are asserted — `ButtonSoundTest.kt:41-46`: "Selectivity is only
a real property if both halves are asserted, so the negative cases here are first-class tests
rather than afterthoughts … without them a future 'simplification' that folds the cue into the
shared `Modifier.claySquish()` would pass every positive test while making exactly the excluded
controls noisy." The infrastructure exists and the tests are cheap: `FakeSoundPlayer`
(`FakeSoundPlayer.kt:25-48`) records every cue in order and at pitch, installed through
`CompositionLocalProvider(LocalSoundPlayer provides …)`, and `GameScreenSoundTest` is the
template — `aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` (`:76-87`) and
`aFreshlyComposedScreenMakesNoSoundAtAll` (`:186-194`) are the two shapes needed.

- `TutorialSoundTest.aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt`
- `TutorialSoundTest.theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep`
- `TutorialSoundTest.theCompletionStarPopsWithoutTheWinOrStarPopCue`
- a grep-shaped guard that `app/src/main/java/com/app/pegsolitaire/ui/onboarding/` references no
  `SoundCue` the spec does not name — the shape the tech-lead already proposes, now with a
  non-empty allow-list (`Jump`, `PegRemove`, `ButtonForward`, `ButtonDismiss`).

Without these, the tutorial's audio contract is an assertion in a document rather than a property
of the build, and the first refactor that folds a cue into a shared modifier breaks it silently.

**Closed — the screenshot-suite question.** The first edition raised the absence of any
screenshot infrastructure (no Roborazzi, no Paparazzi, no `screenshotTest` source set, no
`androidTest` directory) as a gap against the stack CLAUDE.md names. **Ruled 2026-09-21
(tech-lead): no golden-image suite in Phase 15; filed as a post-v1.1 follow-up.** The reasoning I
accept and will not reopen: every pixel on all five screens is intentionally changing, so goldens
recorded during this phase would be images of the implementation under review rather than of the
contract — and recording them *after* the refresh captures the design the studio intends to keep.
What Phase 15 owes instead is the list above, which is binding rather than advisory.

---

## 14. Candidate acceptance criteria

Observable truths a QA engineer can check on a device, in the roadmap's house phrasing.

**Tutorial (D-01)**

1. On a first launch, the player reaches a tutorial that requires them to **complete a real jump**
   before it advances; no step can be advanced by a Next or Continue button. (ONB-01, D-01)
2. The player performs at least one horizontal jump and at least one jump on the other axis, on a
   live board, before the tutorial ends — **structurally**: T1 offers exactly one legal move and
   it is horizontal, T2 offers exactly one and it is vertical, so no sequence of taps can finish
   the tutorial without both. (ONB-01, D-01b)
3. On **T2**, tapping the middle peg — which has no legal jump — selects it, draws no destination
   dots, and produces no error, no red, no dialog and no lost progress; after a pause the tutorial
   re-pulses a peg that *can* move. (D-01c) *(The same holds for three of T3's five pegs — the
   whole middle row; this is normal peg solitaire, not a defect.)*
4. Three consecutive taps that make no progress cause the tutorial to show more help, not less;
   the player is never blocked from continuing; and the help **never mentions Undo** or tells the
   player they are stuck. (D-01c, §4.4)
5. `Skip` is reachable on every step and exits immediately; pressing system back does the same
   thing; after either, the player lands where they came from, Home does **not** immediately
   re-open the tutorial, and the tutorial does not appear again on the next launch. (D-01d,
   D-01g)
6. Rotating the device mid-tutorial keeps the player on the same step with the same board.
   (D-01e)
7. Completing the final step shows a single primary button that returns the player to where they
   started, and the tutorial does not appear again on the next launch. (D-01)
7a. **Nothing behind the tutorial moves.** On a player who has a saved Challenge game and existing
    stars, running the tutorial end to end — or skipping it — leaves the Home Stars pill, both
    Continue chips and the Challenge tile's level **exactly as they were**: no star banked, no
    resume snapshot cleared, no ladder advance, and the Results screen is never reached.
    (D-01g, §5.1, D-03f)
7b. Opening Settings › How to play after skipping mid-tutorial starts again at **step 1**, on the
    first board, every time. (D-01g)
7c. Each completed jump in the tutorial plays the **same jump cue as the real game** — the pitched
    jump with the peg-removal layer under it — on every step, including each of T3's four jumps,
    and with the game's own pitch ladder when the same peg jumps twice in a row. **Check this
    once more with OS animations off** (Developer options → Animator duration scale: off): with
    animations disabled the jump takes the snap path (`BoardCanvas.kt:298-304`), so `animating`
    is set and cleared inside a very short window, and whether the cue still fires in that window
    can only be settled on a device — it is not provable by reading the code.
    (SOUND-01, D-01h as amended by D-01i)

7d. **Nothing else on the tutorial screen makes a sound.** Tapping a peg, re-tapping to put it
    back, tapping a peg with no legal jump, tapping an illegal destination, and the completion
    star are all **silent** — no select, deselect, invalid-tap, undo, hint, star-pop or win cue
    at any point, on any step, on either entry path. (D-01c, D-01h as amended by D-01i,
    rules/audio.md)

7e. The tutorial's completion button plays the same press cue as every other primary button in
    the app, and `Skip` plays the app's dismiss press cue; neither adds any other sound, and both
    behave the same on the first-run path and on the Settings › How to play replay.
    (SOUND-04, D-11, D-01i)

**Info button (D-02)**

8. There is **no** information or help icon anywhere on the Home screen. (D-02)
9. Settings › General has a "How to play" row that opens the tutorial, and it works for a player
   who has already finished it. (D-02a)
10. A returning player who has seen the tutorial launches straight into Home and is never shown
    it again unless they ask from Settings. (D-02a)

**Home (D-03)**

11. Home shows more than the two mode tiles: a titled hero band, three progress pills
    (Stars · Boards · Level) and board art on the mode tiles. (D-03)
12. The Level pill and the Challenge tile show the **same** level number, and tapping the tile
    enters that level. (CHAL-05, D-03c)
13. A pill whose value is not yet known shows "—" rather than 0 or a blank, and a screen reader
    announces it as not yet available. (D-03c)
13a. A pill whose underlying read **fails** shows "—" as well — never `0`. A player with stars
     already earned never sees their total reported as zero. (D-03h)
13b. The Boards pill's denominator is the Classic catalogue size (5), and the Classic catalogue
     screen still lists exactly those five boards — no tutorial board appears on it. (D-03c, §5)
14. With the system font size at maximum and both Continue chips present, the whole Home screen is
    reachable by scrolling and nothing is clipped. (D-03)
15. On a 6.7-inch phone the two mode tiles sit in the lower half of the screen. (D-03)

**Home load-bearing readouts (D-03f)** — these are the cloud-save device oracles; a failure here
blocks tests 10/11/13 in `11-HUMAN-UAT.md`, not just the look of the screen.

15a. On a 360×640dp phone at default font scale, with **both** a Classic and a Challenge game
     saved, the Continue chips **and** both mode tiles are fully visible on Home without
     scrolling. (D-03f, PERS-03)
15b. The Challenge tile still shows the ladder position on its own face ("Continue — Level N"),
     and it shows the same number as the ribbon's Level pill. (D-03f, CHAL-05)
15c. Tapping the Classic tile reaches the catalogue in one tap, and each board card there still
     shows its earned-star pips. The Home Stars pill is a total and has not replaced them.
     (D-03f, CLAS-01)
15d. No readout on Home is behind a tap, an expander, a "show more", or a scroll position.
     (D-03f)

**Home cold start (D-03g, PLAT-08 C1)**

15e. Cold start is measured as a **two-arm delta**, not as a single absolute number: arm A =
     `develop`, arm B = this branch, same AVD (`Pixel_9a`, API 36), same host, same sitting,
     debug variant, 10 `am start -W` samples per arm with the first discarded, re-installed
     between arms, on a device seeded with ~200 banked Challenge levels. **Both must hold:**
     (1) after-median **≤ 950 ms**, and (2) (after-median − before-median) **≤ 50 ms**. Both
     arms' tables are recorded in `15-BUILD-EVIDENCE.md` and the phase does not close without
     them. If (2) fails, §6.5's degradation path is walked and re-measured after each step; if
     (1) fails while (2) passes, the regression belongs to PLAT-08/Phase 14 and escalates to the
     producer rather than being paid for out of this refresh. (PLAT-08 C1, §6.5)
15f. On first paint, before any progress data has loaded, Home shows the ribbon with "—"
     placeholders — never a spinner, never a blank band, never a blocked screen — and the values
     fill in without the layout shifting. (D-03c, D-03g)

**In-game controls (D-04) and the toolbar (D-05)**

16. The gameplay screen has **no Material top app bar**: no bar background, no bar elevation, no
    bar title row. (D-05)
17. A visible, labelled back control remains on the gameplay screen and returns the player to
    where they came from; system back does the same. (D-05b)
18. The elapsed timer is still visible during play and is still announced as the elapsed time.
    (SCOR-02, D-05a)
19. Undo, Hint, Show-moves and Restart are all still reachable, all still work, and all four sit in
    the lower half of the screen. (ASST-01, ASST-02, D-04)
20. The Hint control does not use a magnifying-glass glyph, and the Show-moves control does not use
    the same glyph as any other control in the app. (D-04a, D-04b)
21. Turning Show-moves on and off changes the control's **fill**, not only its colour, and a
    screen reader announces which state it is in. (D-04b)
22. With no moves made, Undo is visibly and announced-as disabled; after one jump it is enabled.
    (ASST-02, D-04d)
23. Requesting a hint shows a spinner in place of the glyph, the control reports as disabled while
    thinking, and a second tap cannot start a second search. (ASST-01, D-04d)
24. No control anywhere shows a hint or undo allowance count, badge or ad glyph. (§5)

**Cross-cutting**

25. With OS animations turned off, every screen in this refresh is fully usable, nothing is stuck
    mid-animation, and the tutorial still pauses long enough after a jump for the player to see it
    resolve. **Sound is unaffected by this switch** — run criterion 7c's animations-off pass at
    the same sitting; a jump that loses its cue when animations are off is a 7c failure, not an
    expected consequence of reduced motion. (rules/compose.md, §10, §14 7c)
26. Every new interactive element is at least 48dp, at least 8dp from its neighbours, and reachable
    by TalkBack with a meaningful label, in both light and dark. (rules/compose.md, §11)
27. Every text and glyph pair introduced by this refresh meets 4.5:1 / 3:1 contrast in both light
    and dark. (§11)
28. No screen in this refresh plays a sound as its only feedback for an event. (rules/audio.md)

---

## 15. Open questions for the founder (non-blocking)

Both are art direction between equals, which rules/studio.md routes to the founder. Both have a
default decided so no work waits.

**Q-901 — Home hero treatment.** Options: (1) a **static** decorative peg cluster in Candy Clay;
(2) a **slowly drifting** mini board that idles behind the title; (3) no art — a larger gradient
title alone. **Recommendation and default: (1).** It fills the emptiness the founder named, costs
no asset licence, cannot distract from the two tiles, and adds no battery cost on the app's first
screen. Option 2 competes with the two `HomeBlobs` that already drift there, and it would put a
running animation on the cold-start critical path (D-03g). Option 3 is also step 3 of the §6.5
degradation path, so if C1 measurement forces it, the founder's answer is already implemented.
**Blocks: nothing.**

**Q-902 — Do the in-game actions move to the bottom?** The founder said "redesign the icons on the
top" and "remove toolbar from the game" in the same breath. This spec reads that as one surface
(D-05) and splits it: read-only status stays top, the four actions move to a thumb-zone dock.
The alternative reading is to keep all four icons at the top, redrawn as a clay cluster.
**Recommendation and default: the split**, because rules/compose.md requires primary actions in
the lower thumb zone of a 6.7-inch phone and Undo — the control a peg-solitaire player uses most —
is currently in the worst-reachable corner of the screen. If the founder wants the icons visually
"on the top", §4.3 and §4.4 swap places and nothing else in this spec changes.
**Blocks: nothing.**

---

## 16. Traceability

| Requirement | Surface in this contract |
|-------------|--------------------------|
| ONB-01 | §4.1 Tutorial (rewritten, boards fixed by the R-4 ruling); §4.5 Settings replay row; §5.1 the tutorial's hard prohibitions |
| NAV-01 | §4.2 Home recomposition; §4.2 top bar deleted; D-01g the Tutorial nav destination |
| PERS-03 | §4.2 Continue chips — behaviour unchanged, thumbnail added; §5.1 #2/#3 — the tutorial may not clear a resume snapshot |
| CHAL-05 | §4.2 Level pill + Challenge tile, same number, same entry level; §5.1 #2/#3 — the tutorial may not advance the ladder |
| CLAS-02 | §4.3 status strip — title and timer preserved |
| SCOR-02 | §4.3 timer pill, `elapsed_time` preserved |
| ASST-01 | §4.4 Hint action, new glyph, thinking state unchanged |
| ASST-02 | §4.4 Undo action, new position, `canUndo` unchanged |
| SOUND-01 | §4.1 "The jump the player hears" + D-01i — a completed tutorial jump plays the game's own pitched jump with the removal layer under it, so the requirement's "**every** peg jump" holds on the tutorial's real jumps too; criterion 7c (including one pass with OS animations off); §13 `TutorialSoundTest`. **Added 2026-09-21:** §16 carried no SOUND-nn row, which is how D-01h narrowed a Complete requirement invisibly. |
| SOUND-02/03 | §4.1, §5, criterion 7d — the tutorial deliberately plays **none** of `Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint`, `Win`, `StarPop`; gameplay and Results are untouched, so the requirements are unnarrowed. The exclusion is a stated, tested property (§13), not an absence. |
| SOUND-04 | §4.1 Completion/Exit and §4.5 — the tutorial's completion CTA plays `ButtonForward` and `Skip` plays `ButtonDismiss` via the house `withClickSound` idiom (D-01i, D-11), and the Settings "How to play" row plays `ButtonForward`; criterion 7e |
| SET-02 | Untouched — haptics gate is unchanged |
| ADS-01/02 | §5 — no allowance surface added; dialog unchanged |
| CLAS-01 | §4.2.1 — Classic catalogue and its per-board star pips untouched; one tap from Home; §5 / criterion 13b — `TutorialBoards` never joins `Boards.CLASSIC_SPECS`, so the catalogue and the Boards-pill denominator are unchanged |
| PLAT-08 (C1) | §6.5 cold-start budget **and** the two-arm measurement contract; §7 R-1/R-2 (one aggregate, D-03h); criteria 15e (delta gate) / 15f |
| QUAL-03 | §6.1 every new dp is a named token; §6.2 no hex; §6.4 durations in scoped objects |
| Proposed UX-01 | §4.1 — interactive tutorial (registry entry is the producer's to confirm) |
| Proposed UX-02 | §4.2 — Home composition |
| Proposed UX-03 | §4.3/§4.4 — gameplay chrome replacement |

---

## 17. Checker sign-off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Accessibility: PASS
- [ ] Dimension 7 Registry Safety: not applicable (native Android; no component registry)

**Approval:** pending
