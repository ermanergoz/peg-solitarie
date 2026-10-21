# qa-engineer report — phase-15 tutorial audio ruling (D-01h review)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (fd60d22..this commit — one `docs(15):` commit, this report;
`fd60d22`, the founder's Q-901/Q-902 answers, landed concurrently from another agent while this
review ran and is untouched by it)
Skills loaded: ergoz-soft:standards (role: qa-engineer)
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md, audio.md,
ip-policy.md, lessons.md
Ran in parallel with: phase-14 (in flight in another worktree — not touched)
Loops: 0
Lessons applied: none

## Recusal note (read this first)

rules/audio.md makes the **sound-designer** the owner of cues, and this product's staff has no
sound-designer dispatched for phase 15. The ux-designer ruled D-01h and then flagged, correctly,
that the ruling sits in another role's domain
(`.planning/reports/phase-15/ux-designer-amendments.md:161-166`). I am standing in **as the role
that owns tests and acceptance**, not as the cue author. Concretely:

- I rule on whether D-01h is **checkable and consistent with the requirements of record**, and on
  what acceptance criteria it owes. That is my authority.
- I do **not** author a cue, pick an asset, set a level, or write a line into `SoundCue`. My
  recommendation below reuses two existing cues at their existing levels and introduces nothing.
- I did not edit `docs/ux-spec.md`. D-01h is the ux-designer's to amend; §14 is theirs to extend.
  Everything below is a proposal routed through the producer.
- I did not read `.planning/log/`.

---

## Summary
**VERDICT: FAIL** — not because silence is indefensible, but because D-01h is **broader than its
own reasoning supports and narrows a Complete requirement without saying so**. Two of its three
stated reasons do not argue for silence (one is a permission, one is neutral); the third is
correct but applies only to `InvalidTap`. Meanwhile `SOUND-01` reads "a jump sound on **every**
peg jump" and the tutorial's jumps are real jumps on the real rules engine. I rule that the
**jump pair fires in the tutorial** and everything else on that screen stays silent. Three
further gaps block a PASS: `docs/audio-spec.md` does not exist at all; the completion CTA's
press cue is unruled and collides with SOUND-04/D-11; and criterion 7c has no unit-level test in
the §13 bill despite the project's own precedent that named silence must be asserted.

---

## 1. Is "the tutorial is silent" the right call?

**No — not end to end. The jump cue should fire. Everything else D-01h silences should stay
silent.**

### 1a. What the code mechanically implies (established by reading, not assumed)

The tech-lead's F-17 is correct as far as it goes, and I extend it. Every gameplay cue is played
by a **private `@Composable` effect declared inside `GameScreen.kt`**, and all eight are invoked
from one block at the top of `GameScreen` itself (`GameScreen.kt:157-164`):

| Cue | Effect | Declared at | Keyed on |
|---|---|---|---|
| `playJump(runIndex)` **+ layered `PegRemove`** | `JumpSound` | `GameScreen.kt:321-330` (`:325`, `:327`) | `state.animating` (`MoveTransition.id`) |
| `Select` | `SelectionSound` | `GameScreen.kt:346-351` (`:349`) | `state.selectionTick` |
| `Deselect` | `DeselectionSound` | `GameScreen.kt:362-367` (`:365`) | `state.deselectionTick` |
| `Undo` | `UndoSound` | `GameScreen.kt:378-383` (`:381`) | `state.undoTick` |
| `InvalidTap` | `InvalidTapSound` | `GameScreen.kt:393-398` (`:396`) | `state.rejectedTapTick` |
| `Hint` | `HintSound` | `GameScreen.kt:408-413` (`:411`) | `state.hintSource` |
| `Win` | `WinSound` | `GameScreen.kt:425-430` (`:428`) | `state.status` |

`BoardCanvas.kt` contains **no** `SoundCue`, `SoundPlayer` or `sound` reference — zero hits
(command C3 below). `StarPop` is not on the game screen at all: it is fired by `StarPopSounds`
on Results (`ResultsCelebration.kt:99-112`, `:111`; called at `ResultsScreen.kt:225`).

**Therefore, by construction:** a tutorial that hosts `BoardCanvas` and not `GameScreen` (§5.1
prohibition 1) is silent on the board **for free**, and a tutorial with no route to Results
(§5.1 prohibition 4) never reaches `StarPop`. D-01h's silence is the default, not a cost.

**Two mechanical facts D-01h and the architecture report do not state, and they change the
argument:**

1. **Firing the jump cue in the tutorial does not touch any of §5.1's four prohibitions.** Those
   prohibitions are about `GameScreen` *the composable* and `GameViewModel` *the state holder* —
   not about the seven effects that happen to be declared in that file today. Lifting
   `JumpSound` from `private` in `GameScreen.kt` into a shared `internal` composable that both
   screens call keeps all four intact: the tutorial still hosts `BoardCanvas`, still runs
   `TutorialViewModel`, still writes no `LevelResult`, still has only `popBackStack()`. It also
   keeps §5's "no `SoundCue` reference added to `BoardCanvas`" intact, because the effect lives
   beside the screen, never inside the canvas. The grep gate the tech-lead proposes
   (`tech-lead-architecture.md:173`) already anticipates this: it says the onboarding package
   must be empty "**or contain only cues the spec names**".
2. **The data the jump cue needs is already in the state the tutorial builds.** §4.1 ("The board
   the tutorial drives") has `TutorialViewModel` assembling a real `GameUiState`; `JumpSound` is
   keyed on `GameUiState.animating`, a `MoveTransition` carrying `id` and `runIndex`
   (`MoveTransition.kt:29-34`). The tutorial must already populate `animating` or `BoardCanvas`
   cannot draw the jump slide and the removal fade that D-01a promises. So the cue costs one
   call site, not a state-model change.

### 1b. Why D-01h's reasoning does not carry its own conclusion

D-01h (`ux-spec.md:193-209`) gives three reasons. Taken one at a time:

- *"rules/audio.md says nothing plays that the spec does not name, so silence is compliant."*
  True, and irrelevant to the choice. That clause establishes that silence is **permitted**; it
  cannot establish that silence is **right**. Every unnamed cue in the game is compliant by the
  same sentence.
- *"every tutorial event already carries visual feedback … so the 'sound is never the only
  feedback' rule is satisfied with room to spare."* Also true, and **neutral**. Note the
  direction of that rule, as the brief flags: it constrains sound-**only** feedback. It is
  satisfied identically whether the jump cue fires or not, because the jump already has the
  slide and the removal fade. It is not an argument for silence; it is an argument that either
  choice is legal.
- *"an `InvalidTap` cue would contradict D-01c."* **Correct, and the only load-bearing reason in
  the decision.** A rejection sound is exactly the channel that would make a wrong tap feel like
  a failure, and D-01c forbids that. I uphold this without reservation — but it argues for
  silencing **one** cue, not seven.

What is left is the editorial claim in the last sentence: *"the first sound the player hears is
therefore the first jump of their first real board, which is the right moment for it."* That is
the whole decision, and it is the part I am overturning.

### 1c. Why the jump cue should fire

**It inverts where the reward belongs.** The tutorial is the player's first thirty seconds and
the one place in the product where a jump is a *discovery*. The first jump of the first real
board is not a milestone — it is the second time the player has done the thing. D-01h trades a
reward at the moment of learning for a reward at the moment of routine.

**It is the one channel where the tutorial breaks its own premise.** D-01/D-01a stake the whole
design on fidelity: the real `BoardCanvas`, the real rules engine, "what the player learns is
literally the mechanic they will use" (`ux-spec.md:119-124`). The tutorial reproduces the
selection ring, the legal-destination dots, the jump slide and the removal fade. Audio is the
sole deliberate divergence. For thirty seconds the player learns that a jump is a silent event,
and the game contradicts that lesson at the first board. A screen whose job is fidelity should
not be the only screen where the core verb behaves differently.

**D-01c has already stripped every other consequence from the screen.** No score, no failure
state, no retry count, no red, no modal (`ux-spec.md:139-142`). That is right, and it is also
why the completed jump is the tutorial's *only* moment of positive consequence. Removing its one
audible confirmation as well leaves the payoff thinner than the teaching needs, at exactly the
point where rules/puzzle-design.md asks the layout to teach without text.

**And a requirements point, which is where my authority actually sits.** `.planning/REQUIREMENTS.md:38`
reads:

> - [x] **SOUND-01**: Player hears a jump sound on every peg jump, pitch-varied so repeated jumps do not fatigue

That requirement is marked **Complete** (`.planning/REQUIREMENTS.md:165`). D-01h makes the
tutorial the single place in the shipped product where a peg jump produces no jump sound, and it
does so without amending SOUND-01, without scoping it, and without registering the narrowing
anywhere: §16 Traceability (`ux-spec.md:1311-1332`) carries **no SOUND-nn row at all**. A UX
decision that silently narrows a Complete requirement is a FAIL item regardless of which way the
taste question goes. Either the cue fires, or SOUND-01 gains an explicit scope clause naming the
tutorial as excluded and §16 records it. The spec currently does neither.

### 1d. What stays silent, and why (I uphold most of D-01h)

| Cue | Ruling | Reason |
|---|---|---|
| `Jump` (via `playJump(runIndex)`) **with `PegRemove` layered under it** | **Fires** | SOUND-01; the tutorial's one moment of reward; fidelity to the mechanic being taught |
| `InvalidTap` | **Silent** — uphold D-01h | D-01c: a rejection sound is the one channel that makes a wrong tap feel like a failure |
| `Select`, `Deselect` | **Silent** — uphold D-01h | Selection is a sub-step, not the lesson. T1 and T2 each offer exactly one legal move (`ux-spec.md:448-449`), so a learner taps several wrong pegs first; a pop on each would be the dominant sound of the tutorial and would bury the jump, inverting the emphasis. SOUND-02 does not say "every", unlike SOUND-01 |
| `Undo` | **Silent** — uphold D-01h | §5 removes Undo from the tutorial entirely (`ux-spec.md:699-702`); there is nothing to sound |
| `Hint` | **Silent** — uphold D-01h | `showHints` is forced on (D-01b); no hint is ever *revealed*, so `hintSource` never transitions |
| `Win`, `StarPop` | **Silent** — uphold D-01h | A tutorial is not a victory; D-14's two-beat celebration belongs to a real solve. §4.1 is right that `ClayAnim.STAR_POP_MS` is borrowed as a duration, not a cue |

Net effect: the tutorial plays **one** cue, on **one** event, and the change to D-01h is one row,
not a rewrite. Everything D-01h protects — no rejection sound, no false victory, no `SoundCue` in
`BoardCanvas` — survives.

**One interaction with §14 criterion 25 (animations off), stated precisely.** `JumpSound` reads
no animation flag of any kind (`GameScreen.kt:321-330`), and the project's audio precedent is
that cues survive reduced motion: `StarPopSounds` consults `rememberAnimationsEnabled()` only to
change the *stagger* and still plays every cue (`ResultsCelebration.kt:104-106`). So nothing
*gates* the jump cue on animation state. I am **not** asserting more than that: with animations
off, `runJumpAnimation` takes the snap path and calls `onAnimationFinished()` immediately
(`BoardCanvas.kt:298-304`), so `animating` is set and cleared within a very short window, and
whether `JumpSound`'s `LaunchedEffect` always wins that race is a **device check, not a reading**.
Criterion 7c must therefore be exercised once with OS animations off — that is the one place this
ruling could fail in practice, and it is cheap to check.

---

## 2. Is the cue already specified, and does it need a licence row?

### 2a. No new asset, and no new licence row. Stated explicitly.

**My ruling introduces no audio asset and needs no THIRD_PARTY / CREDITS row.** Both files
already ship, both are already credited, and both are CC0 1.0:

- `SoundCue.Jump` → `R.raw.sfx_jump` (`SoundCue.kt:30`), pitched by
  `SoundPlayer.playJump(runIndex)` (`SoundPlayer.kt:48`, implemented `SoundPoolPlayer.kt:145`).
  Credited: `docs/audio/CREDITS.md:38` — Kenney *Interface Sounds* `Audio/drop_003.ogg`, CC0 1.0.
- `SoundCue.PegRemove` → `R.raw.sfx_peg_remove` (`SoundCue.kt:33`). Credited:
  `docs/audio/CREDITS.md:39` — Kenney `Audio/drop_002.ogg`, CC0 1.0.

`AudioCreditsTest` already fails the build if any file in `app/src/main/res/raw/` lacks a row
(`docs/audio/CREDITS.md:330-331`), so completeness is enforced and unchanged. The existing
twelve-cue set from Phase 7 covers this, exactly as the brief expected. §5's "**No new audio
asset**" (`ux-spec.md:703`) stands unamended under my ruling.

### 2b. The spec row is needed — and the file it belongs in does not exist

This is a separate FAIL item and it is larger than the question I was asked.

rules/audio.md opens: *"every sound is listed in `docs/audio-spec.md` with the event that fires
it."* **`docs/audio-spec.md` has never existed in this repository** — not on this branch, not on
any branch (commands C4/C5 below: `test -f` → ABSENT; `git log --all --diff-filter=A` → 0
commits). The ux-designer's own handoff assumes it does
(`ux-designer-amendments.md:164`: "`docs/audio-spec.md` may need a line saying the tutorial names
no cue"), which is how the gap stayed invisible.

What stands in for it today is three partial sources, none of which is the artefact the rule
names: `SoundCue.kt` (one KDoc line per cue naming its event — the closest thing to a cue list),
`docs/audio/CREDITS.md` (provenance and licences, not events), and `.planning/REQUIREMENTS.md:38-43`
(SOUND-01..06, requirements not cues). No single document maps **cue → event → screen**, which is
precisely why D-01h could narrow SOUND-01 without anyone noticing.

Creating `docs/audio-spec.md` is the **sound-designer's** work, not mine, and I am not writing
it. What I owe is the exact wording of the row my ruling needs, so it can be dropped in:

> | Cue | Event that fires it | Screens |
> |---|---|---|
> | `SoundCue.Jump`, pitched by `playJump(runIndex)`, with `SoundCue.PegRemove` layered `SoundParams.REMOVAL_LAYER_DELAY_MS` beneath it | **A peg completes a jump and the jumped peg is removed.** | Gameplay (`GameScreen`) **and Tutorial (`TutorialScreen`)** |

and the named-silence rows that make the tutorial's selectivity a stated property rather than an
absence — the project's own precedent is that both halves must be written down
(`ButtonSoundTest.kt:36-52`: *"Selectivity is only a real property if both halves are asserted"*):

> | `SoundCue.Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint` | as today, gameplay events | Gameplay only — **never on the Tutorial screen** (D-01c for `InvalidTap`; D-01h as amended for the rest) |
> | `SoundCue.Win` | the winning move lands | Gameplay only — **never on the Tutorial screen** (a tutorial is not a victory) |
> | `SoundCue.StarPop` | one star landing in the Results celebration | Results only — **never on the Tutorial screen** |

### 2c. A third gap: the completion CTA's press cue is unruled

D-01h says *"No cue at all plays anywhere on the tutorial screen"* (`ux-spec.md:193-194`) and
*"the only sound in the whole tutorial flow is the `SoundCue.ButtonForward` that the Settings row
plays on the way in"* (`:196-197`). But the enumerated prohibition in §5
(`ux-spec.md:695-698`) names only board cues and completion cues — `ButtonForward` and
`ButtonDismiss` appear nowhere in it.

Meanwhile §4.1 gives the tutorial a **full-width `CtaGradient` primary button**
(`ux-spec.md:487-490`), and the house idiom for exactly that control is
`withClickSound(SoundCue.ButtonForward, …)` — used at `HomeScreen.kt:447`, `:581`,
`ClassicCatalogScreen.kt:143`, `ResultsActions.kt:96`, `:123`, `:152`, `GameScreen.kt:631`.
`SOUND-04` (`.planning/REQUIREMENTS.md:41`) reads: *"Player hears button-press feedback on primary
UI actions"*, and `SoundCue.kt:70-74` records D-11 as the forward/dismiss split.

So a developer who builds the completion CTA with the same idiom as every other CTA in the app
will violate D-01h's blanket sentence **without any grep or test catching it**, and a developer
who omits it will violate SOUND-04 and D-11. That is an unresolved contradiction between two
sentences of the same spec, and it is a FAIL item.

**My recommendation, for the ux-designer to rule:** the completion CTA plays `ButtonForward` and
`Skip` plays `ButtonDismiss`. Both are *exit* actions, not teaching events — they belong to the
navigation vocabulary the rest of the app already speaks, and neither can be mistaken for a
verdict on the player's tapping. Whichever way it is ruled, §5's enumerated list must name
`ButtonForward`/`ButtonDismiss` explicitly so the silence (or the sound) is checkable.

---

## 3. Acceptance criteria, in §14 phrasing

For the ux-designer to place in §14 "Candidate acceptance criteria". **7c is replaced**, 7d and
7e are new.

> 7c. Each completed jump in the tutorial plays the **same jump cue as the real game** — the
>     pitched jump with the peg-removal layer under it — on every step, including each of T3's
>     four jumps, and with the game's own pitch ladder when the same peg jumps twice in a row.
>     (SOUND-01, D-01h as amended)
>
> 7d. **Nothing else on the tutorial screen makes a sound.** Tapping a peg, re-tapping to put it
>     back, tapping a peg with no legal jump, tapping an illegal destination, and the completion
>     star are all **silent** — no select, deselect, invalid-tap, undo, hint, star-pop or win cue
>     at any point, on any step, on either entry path. (D-01c, D-01h as amended, rules/audio.md)
>
> 7e. The tutorial's completion button plays the same press cue as every other primary button in
>     the app, and `Skip` plays the app's dismiss press cue; neither adds any other sound, and
>     both behave the same on the first-run path and on the Settings › How to play replay.
>     (SOUND-04, D-11)

Criterion 25 is unaffected and should be read as covering this too: with OS animations off, 7c
still holds — the jump cue is not gated on animation state.

**If the producer routes the question back and the ux-designer upholds D-01h unchanged**, then
7c stands as written today (`ux-spec.md:1196-1197`) but is **not sufficient on its own**: it must
be paired with an amendment to SOUND-01 scoping the tutorial out, a §16 traceability row
recording that narrowing, and 7e above, which is required either way.

### The test that makes any of this more than a device check

§13's test bill (`ux-spec.md:1131-1149`) lists eight owed tests and **not one of them is an audio
test**. Criterion 7c today has a device check and nothing else. The project's own precedent
forbids that: `ButtonSoundTest.kt:36-52` states that selectivity is only a real property if both
halves are asserted, and D-09's negative cases are first-class tests for exactly that reason.

The infrastructure is already in place and the test is cheap: `FakeSoundPlayer`
(`FakeSoundPlayer.kt:25-45`) records every cue **in order and at pitch**, installed through
`CompositionLocalProvider(LocalSoundPlayer provides …)`, and `GameScreenSoundTest` is the
template — `aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` (`:76`) and
`aFreshlyComposedScreenMakesNoSoundAtAll` (`:187`) are the two shapes needed.

**§13 must gain, whichever way D-01h is ruled:**

- `TutorialSoundTest.aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt`
  (drop if silence is upheld)
- `TutorialSoundTest.theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep`
- `TutorialSoundTest.theCompletionStarPopsWithoutTheWinOrStarPopCue`
- a grep-shaped guard that `app/src/main/java/com/app/pegsolitaire/ui/onboarding/` references no
  `SoundCue` the spec does not name — the tech-lead already proposes this shape
  (`tech-lead-architecture.md:173`)

Without these, "the tutorial is silent" is an assertion in a document, not a property of the
build, and the first refactor that folds a cue into a shared modifier will break it silently.

---

## Outputs
- `.planning/reports/phase-15/qa-engineer-tutorial-audio.md` — this report: the ruling on D-01h,
  the proposed spec row, and the three acceptance criteria.
- No source, spec or test file was modified. `docs/ux-spec.md`, `.planning/reports/STATUS.md`,
  `.planning/ROADMAP.md`, `.planning/questions/FOUNDER.md` and `VERSION` were not touched.

## Checks

Commands run (all in `/Users/ermanergoz/Desktop/PegSolitarie-p15`, branch
`feature/phase-15-ux-refresh` at `e0f429b`, working tree clean before this report):

- C1 `git -C /Users/ermanergoz/Desktop/PegSolitarie-p15 status --short` — pass (clean; no other
  worktree touched)
- C2 `grep -rn "SoundCue\." app/src/main | grep -v "audio/SoundCue.kt" | cut -d: -f1 | sort | uniq -c | sort -rn`
  — pass (10 hits in `GameScreen.kt`, 4 `ProfileSwapDialog.kt`, 4 `SoundPoolPlayer.kt`,
  3 `ResultsActions.kt`, 3 `SoundPlayer.kt`, 2 `PlayGamesPromptDialog.kt`, 2 `HomeScreen.kt`,
  2 `AdOfferDialog.kt`, 1 `ClassicCatalogScreen.kt`)
- C3 `grep -n "SoundCue\|SoundPlayer\|sound" app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt`
  — pass (**0 hits**; confirms F-17 and §5's `BoardCanvas` clause)
- C4 `test -f docs/audio-spec.md` — **fail** (ABSENT — the artefact rules/audio.md names as the
  cue list of record does not exist)
- C5 `git log --oneline --all --diff-filter=A -- docs/audio-spec.md | wc -l` — **fail** (`0` — it
  has never existed on any branch)
- C6 `grep -n "SOUND-0" .planning/REQUIREMENTS.md` — pass (SOUND-01..06 at `:38-43`, all marked
  Complete at `:165-170`)
- C7 `grep -rn "SoundCue.Jump\|StarPop" app/src/main` — pass (`Jump` reached only via
  `SoundPoolPlayer.kt:145`; `StarPop` only via `ResultsCelebration.kt:111` / `ResultsScreen.kt:225`
  — neither is on `BoardCanvas`)
- C8 `ls app/src/main/java/com/app/pegsolitaire/ui/onboarding/` — pass (`OnboardingOverlay.kt`
  only; `TutorialScreen.kt` is not built yet, so this ruling lands before the code, which is the
  cheap moment for it)
- C9 `grep -n "fun [a-z]" app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt`
  — pass (10 tests; `:76` and `:187` are the templates a `TutorialSoundTest` would follow)

**`./gradlew check` was deliberately not run.** This task changed no source, no test and no
build file — the only file added is this report. The branch's green baseline is the tech-lead's
recorded run on the same unmodified tree: `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline`
— **pass** (BUILD SUCCESSFUL in 11m10s, 98 actionable tasks;
`tech-lead-architecture.md:480`). Re-running it would re-measure an unchanged tree.

**No hands-on device session was run, and none is owed by this task.** rules/testing.md and my
own quality gate require a hands-on session with screenshots for any task that **changed UI**;
this task changed no UI and produced no artifact to install. The device checks this ruling
creates are criteria 7c/7d/7e, which become owed at the phase-15 build review, on the build that
first contains `TutorialScreen.kt`. Nothing under `.planning/qa/` was produced, so there is no
artifact list.

## Open issues / risks

FAIL items, each with what would close it:

1. **D-01h silences the jump cue, narrowing SOUND-01 without amending it.** Reproduction: read
   `.planning/REQUIREMENTS.md:38` ("a jump sound on **every** peg jump"), then
   `docs/ux-spec.md:193-209` and `:695-698`; then `docs/ux-spec.md:1311-1332` — §16 carries no
   SOUND-nn row, so the narrowing is recorded nowhere. Expected: a peg jump plays the jump cue,
   or the requirement says where it does not. Actual: neither. **Closes when** either the cue
   fires in the tutorial (§1d table) or SOUND-01 gains an explicit tutorial-exclusion clause and
   §16 records it.
2. **`docs/audio-spec.md` does not exist** (C4, C5), though rules/audio.md makes it the cue list
   of record and `ux-designer-amendments.md:164` assumes it does. **Closes when** the
   sound-designer writes it, cue → event → screen, including the rows proposed in §2b. This is
   larger than phase 15 and predates it; it should not block the phase, but it should be
   scheduled, because it is the reason item 1 was invisible.
3. **The tutorial's completion CTA and `Skip` have no ruling on their press cue**, and D-01h's
   blanket sentence contradicts SOUND-04 and D-11 as soon as the CTA is built with the app's own
   `withClickSound` idiom (§2c). Reproduction: compare `docs/ux-spec.md:193-197` with `:487-490`
   and `:695-698`, then `HomeScreen.kt:447`. **Closes when** §5's enumerated list names
   `ButtonForward`/`ButtonDismiss` either way and criterion 7e is added.
4. **Criterion 7c has no unit-level test in §13's bill** (`ux-spec.md:1131-1149` — eight owed
   tests, none about audio), against the project's own rule that named silence must be asserted
   (`ButtonSoundTest.kt:36-52`). **Closes when** §13 gains the `TutorialSoundTest` names listed
   at the end of §3.

Risks:

- **Scope.** My ruling costs one lifted composable and one call site. If the developer instead
  re-implements `JumpSound` inside `TutorialScreen.kt`, the `REMOVAL_LAYER_DELAY_MS` layering and
  the `runIndex` pitch ladder become duplicated and will drift. The reviewer should insist on the
  lift, not the copy.
- **Boundary.** Lifting a private composable out of `GameScreen.kt` must not become an excuse to
  host `GameScreen`. §5.1's four prohibitions are unchanged by this ruling and the grep gates
  that enforce them (`tech-lead-architecture.md:173-174`) still apply verbatim.
- **Recusal.** The sound-designer has never seen D-01h. If one is dispatched before the phase-15
  build, the taste half of §1 is theirs to reopen; the requirements half (items 1–4) is not —
  those are acceptance defects regardless of which cue set is chosen.

## Handoff
Next: **producer** — route this back to the **ux-designer** as the owner of `docs/ux-spec.md`
(D-01h, §5, §13, §14), with items 1, 3 and 4 as the changes to make and §3's criteria as the
wording to paste. Item 2 (`docs/audio-spec.md` missing) is **not** the ux-designer's: queue it
for a **sound-designer** dispatch, or, if none is staffed for v1.1, for the tech-lead as a
follow-up, and note that it predates phase 15 and need not block it. No founder question: this is
a spec contradiction resolved by staff, per rules/studio.md. Once §14 carries 7c/7d/7e, the
phase-15 build review inherits them as device checks and I will run them hands-on against the
first build containing `TutorialScreen.kt`.

VERDICT: FAIL
