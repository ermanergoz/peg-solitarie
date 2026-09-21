# ux-designer report — phase-15 D-01h audio amendment

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (e5ec0bd..this commit — one `docs(15):` commit: the spec
amendment and this report)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, lessons.md
Ran in parallel with: phase-14 (in flight in other worktrees — not touched)
Loops: 0
Lessons applied: none

## Summary

My decision **D-01h** ("the tutorial is silent, end to end") was reviewed and FAILED, and the
review is right. The spec now carries **D-01i**: a completed tutorial jump plays the game's own
jump pair, silence is upheld for select/deselect/invalid-tap/undo/hint/star-pop/win, and the
completion CTA and `Skip` play the app's standard press cues — the gap D-01h left. The deciding
fact is not taste: SOUND-01 is a Complete requirement that says **every** peg jump sounds, and
D-01h narrowed it invisibly because §16 carried no SOUND-nn row at all. §16 now does.

## Outputs

- `docs/ux-spec.md` — amended. Header Amendments note (second revision, naming the review it came
  from); D-01h now opens with an unmissable pointer and is kept verbatim as the record, not the
  contract; **D-01i** filed as the amending decision (house style in §3: no decision is rewritten
  in place); §4.1 gains "The jump the player hears" and its Completion/Exit paragraphs name the
  two press cues; §4.4's two wrong-tap rows keep their silence and say which decision upholds it,
  with a closing note on the deliberate asymmetry; §5's negative contract is now an enumerated
  two-sided list (what plays / what never plays / nothing added to `BoardCanvas`); §5.1 gains a
  "what these four do not forbid" note; §9's audio rows are split by what the tutorial uses;
  §10's reduced-motion list says sound is not gated on motion; §13 gains four audio tests; §14
  replaces 7c and adds 7d/7e; §16 gains SOUND-01, SOUND-02/03 and SOUND-04 rows.
- `.planning/reports/phase-15/ux-designer-audio-amendment.md` — this report. The two earlier
  ux-designer reports in this directory were not touched.

## Checks

Every item from the task, applied or escalated, with what it was verified against.

1. **D-01h amended — the jump cue fires. APPLIED.** The load-bearing fact is a requirements
   fact, not a preference: `.planning/REQUIREMENTS.md:38` (SOUND-01) reads "a jump sound on
   **every** peg jump" and is marked Complete at `:165`. The tutorial's jumps are real jumps on
   the real engine (D-01a), so a blanket-silent tutorial narrowed a shipped requirement without
   amending or scoping it. I took the review's other two reasons — fidelity, and the jump being
   the tutorial's only positive consequence once D-01c removed the rest — as agreeing design
   reasons rather than as the argument. Recorded in D-01i with the requirement cited inline.
   Silence is **upheld** for `Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint`, `StarPop` and
   `Win`, each with its own reason; the `InvalidTap` reason was the one part of D-01h that
   carried its own weight and it is upheld without reservation.

2. **§16 traceability. APPLIED.** Three rows added (SOUND-01, SOUND-02/03, SOUND-04) where there
   were none. The row text names why it is there, so the next narrowing of a Complete requirement
   is visible in the table rather than only in a decision's prose.

3. **The completion CTA and `Skip` ruled. APPLIED**, as the review recommends. The collision was
   real and internal to my own document: D-01h's "no cue at all plays anywhere on the tutorial
   screen" against §4.1's full-width `CtaGradient` button, against SOUND-04
   (`.planning/REQUIREMENTS.md:41`, also Complete) and against D-11's forward/dismiss split
   (`SoundCue.kt:70-74`) — while §5's enumerated list named only board and completion cues, so
   neither a grep nor a test could have caught either failure mode. The house idiom is
   `withClickSound(SoundCue.ButtonForward, …)` (`HomeScreen.kt:447`, `:581`). Ruled: CTA →
   `ButtonForward`, `Skip` → `ButtonDismiss`, system back silent (no press to sound). §5's list
   now names both cues explicitly, and §4.1's Completion and Exit paragraphs carry them.

4. **§14 criteria 7c (replaced), 7d, 7e (new). APPLIED**, in the review's wording. 7c additionally
   instructs QA to exercise it **once with OS animations off** and says why that is a device
   check and not a reading: `runJumpAnimation` takes the snap path at `BoardCanvas.kt:298-304`,
   calling `onAnimationFinished()` immediately, so `animating` is set and cleared in a very short
   window; `JumpSound` is a `LaunchedEffect` keyed on `transition?.id` (`GameScreen.kt:320-330`)
   and whether it wins that race is not provable by reading. The precedent that cues survive
   reduced motion is real and I re-read it: `StarPopSounds` consults `rememberAnimationsEnabled()`
   only to pick the stagger and still plays every cue (`ResultsCelebration.kt:103-113`).

5. **§13's test bill gains four audio tests. APPLIED**, named as the review names them. The bill
   listed eight owed tests and no audio test, against this repo's own precedent:
   `ButtonSoundTest.kt:41-46` ("Selectivity is only a real property if both halves are asserted
   … without them a future 'simplification' that folds the cue into the shared
   `Modifier.claySquish()` would pass every positive test while making exactly the excluded
   controls noisy"). The infrastructure exists — `FakeSoundPlayer` records cue, order and pitch
   (`FakeSoundPlayer.kt:25-48`), and `GameScreenSoundTest` supplies both shapes
   (`:76-87` positive, `:186-194` "makes no sound at all"). The fourth item is the grep guard over
   `ui/onboarding/`, now with a non-empty allow-list.

6. **§5.1 verified NOT breached, and NOT weakened. APPLIED as a clarification.** I checked the
   claim rather than accepting it. `JumpSound` is `private fun JumpSound(transition:
   MoveTransition?)` at `GameScreen.kt:320-330`: its whole input is a `MoveTransition?` plus
   `LocalSoundPlayer`, and it reaches no `GameViewModel`, no repository, no navigation and no
   persistence. Lifting it to a shared `internal` composable leaves all four prohibitions
   literally true — the tutorial still hosts `BoardCanvas` and not `GameScreen`, still runs
   `TutorialViewModel`, still writes nothing, still has `popBackStack()` as its only outbound
   edge. The state it needs is already required: `BoardCanvas` reads `val transition =
   state.animating` (`BoardCanvas.kt:115`) to draw the jump slide D-01a promises, so
   `TutorialViewModel` must populate `animating` regardless of the cue. **No prohibition text was
   changed.** What I added is a note saying what the four bind (screen and state holder) versus
   what they do not (where a `@Composable` effect is declared), plus the explicit warning that
   lifting an effect out of `GameScreen.kt` is not licence to host it. There is no conflict
   between §5.1 and the audio, so nothing needed to be escalated on that ground.

7. **The developer must lift the effect, not copy it. APPLIED** as a spec sentence rather than
   left as a review risk: re-implementing `JumpSound` inside the tutorial duplicates the
   `REMOVAL_LAYER_DELAY_MS` layering and the `runIndex` pitch ladder, and they will drift. D-01i
   calls the copy a defect and §5 keeps the `BoardCanvas` clause that stops the other wrong turn.

8. **No new asset, no new licence row. VERIFIED.** `Jump` → `R.raw.sfx_jump` (`SoundCue.kt:30`),
   `PegRemove` → `R.raw.sfx_peg_remove` (`:33`), `ButtonForward`/`ButtonDismiss` (`:70-74`) all
   ship and are credited CC0 1.0. §5's "No new audio asset" stands unamended; §9's rows now state
   which cues the tutorial uses and which it never plays.

9. **`docs/audio-spec.md`. NOT ABSORBED — escalated to the producer, as instructed.** The file
   does not exist on this branch (confirmed) and rules/audio.md makes it the cue list of record.
   It predates phase 15, it is the sound-designer's artefact, and the producer is tracking it.
   The header note says so explicitly so a reader does not mistake §9 for that file.

10. **Scope. HONOURED.** No production code was written and nothing under `app/src/` was touched;
    `.planning/reports/STATUS.md`, `.planning/ROADMAP.md`, `.planning/questions/FOUNDER.md`, the
    root `VERSION` and both existing ux-designer reports were not touched. Work is confined to the
    `/Users/ermanergoz/Desktop/PegSolitarie-p15` worktree.

- `./gradlew check` — **not run, and not owed by this task.** This change touches one document and
  adds one report; no source, test or build file changed, so the check command would re-measure an
  unchanged tree. The branch's green baseline is the tech-lead's recorded run on this same tree
  (`ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — BUILD SUCCESSFUL,
  `tech-lead-architecture.md:480`).
- Spec self-consistency after the edits — pass. §4.1 (screen contract), §4.4 (wrong-tap rows),
  §5 (negative contract), §5.1, §9, §13, §14 and §16 all state the same cue set: jump pair +
  two press cues audible, everything else silent.

## Open issues / risks

- **The device check in 7c is the one place this ruling can fail in practice.** If the cue is
  dropped with animations off, the fix is the developer's (the effect must observe the transition
  before it clears), not a re-ruling — the requirement does not change. QA should report it as a
  7c failure, not as a reason to restore silence.
- **`docs/audio-spec.md` is still missing**, so cue → event → screen lives in three partial places
  (`SoundCue.kt` KDoc, `docs/audio/CREDITS.md`, and now this spec). That is the reason a Complete
  requirement could be narrowed unnoticed; until the file exists, §16's new SOUND rows are the
  only cross-check.
- **Recusal stands.** No sound-designer has seen either D-01h or D-01i. The taste half is theirs
  to reopen if one is staffed before the build; the requirements half (SOUND-01, SOUND-04) is not
  a taste question and does not reopen with it.

## Handoff

Next: **producer** — D-01h's FAIL is closed on the spec side; items 1, 3 and 4 of the review are
applied and item 2 (`docs/audio-spec.md`) is left with you as a sound-designer or tech-lead
follow-up, non-blocking. The phase-15 build task inherits three new device criteria (7c with an
animations-off pass, 7d, 7e) and four owed audio tests; the developer must **lift** `JumpSound`
from `GameScreen.kt:320-330` into a shared `internal` composable rather than copy it, and must not
add any `SoundCue` reference to `BoardCanvas`.
