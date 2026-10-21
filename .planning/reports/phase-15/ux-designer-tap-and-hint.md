# ux-designer report — phase-15 tutorial tap semantics and the D-01i hint reason

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (1537d5e..HEAD — one `docs(15)` commit; the three modified
files under `app/src/test/` in the worktree are the concurrent developer's and were **not** staged)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, lessons.md
Ran in parallel with: phase-15 S5–S8 build (same branch, `app/src/` + `domain/src/` — untouched here)
Loops: 0
Lessons applied: none

## Summary
Two items from `.planning/reports/phase-15/android-game-dev-s1-s4.md`, both closed in
`docs/ux-spec.md`. **(1)** The second-peg tap is ruled to **match the gameplay board** — the held
peg stays held — which **requires a change to what S4 built** (it re-selects); the developer's real
defect is answered instead by a new release clause on §4.4's escalation. **(2)** D-01i's reason for
the silent `Hint` cue was factually wrong and is replaced; the audio ruling itself is untouched and
still tested. No other decision reopened; §5.1, §4.2, §6.5 and the audio ruling's substance not
touched.

## Outputs
- `docs/ux-spec.md` — header Amendments note: one new paragraph ("Third revision — 2026-09-21").
- `docs/ux-spec.md` §4.1 — new **"Tap semantics — identical to the real game"** block: a five-row
  tap table, the ruling on the fourth row with three reasons, and the consequence for the build.
- `docs/ux-spec.md` §4.4 — class 1 and class 2 widened (another peg is class 2, not a
  re-selection); class 3's trigger now names both triggers; new **"The release clause"** paragraph.
- `docs/ux-spec.md` D-01i — the `Hint` table row's reason replaced (verdict **Silent**, unchanged);
  new **"Why the `Hint` row's reason changed"** block above "Why the jump cue fires".
- `docs/ux-spec.md` §14 — criterion **3** gains the release clause; new criterion **3a**.
- `.planning/reports/phase-15/ux-designer-tap-and-hint.md` — this report.

## The two rulings

**1. A tap on a second peg while one is held: the tutorial matches the main game.**
The held peg stays held; the tap is §4.4 class 2 (non-progressing, silent, feeds the escalation).
Reasons, in the spec at §4.1: a tap rule is the mechanic the tutorial exists to teach, not chrome,
so re-selection would be a gesture the tutorial teaches and the game refuses; the refusal would
land within a graduate's first taps of a real board, because "this peg cannot move" is the ordinary
case on a full board, not a corner case; and matching costs one tap of recovery, which is itself the
deselect gesture worth teaching.

The developer's premise is right and the remedy is wrong. A player who taps an immovable peg must
not be left holding it — but the fix belongs in *assistance*, which is tutorial-only and diverges
from nothing, not in the tap table. Hence the new **release clause**: when the escalation fires
while a peg is held that is not the peg the coach is about to point at, the escalation puts that peg
back down, silently, in the same state change that raises the ring — on both triggers (three
non-progressing taps and the idle nudge). Without it, matching the game would have made the help
*unreachable*: while a peg is held, a tap on the pulsing peg is class 2 and changes nothing, so the
coach would point at a peg the player cannot pick up. It deliberately does **not** release when the
held peg *is* the coached source — that would undo a correct selection.

**2. D-01i's `Hint` reason.** The verdict (**Silent**) is unchanged and is not mine to reopen; only
the reason is. The silence rests on where the cue *effects* live, not on what the state does.

## Checks

This pass changed one document and no production or test code, so `./gradlew check` is not owed by
it and was not run — this role has no shell and the S1–S4 runs in the developer's report are the
record for the code. What follows is verification by reading, each with its `file:line`.

- **The hint transition is real — D-01i's stated premise is false.** `escalateCoach()` at
  `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModel.kt:270`, and the state
  update at **`TutorialViewModel.kt:276`**:
  `.withBoard(it.board.copy(hintSource = hint.from, hintTarget = hint.to))` — so `hintSource`
  transitions `null → Position` on every coach escalation, and back to `null` on the next jump
  (`TutorialViewModel.kt:200`). — **fail for the old reason, pass for the ruling.** Required by my
  own §4.4 class 3 ("the source ring pulses **and** the destination dot brightens") and §10 ("the
  tutorial reuses the board's own idiom rather than adding a highlight").
- **The true reason for the silence.** `private fun HintSound(hintSource: Position?)` at
  **`app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:372-377`**, keyed
  `LaunchedEffect(hintSource)` at `:374` with a non-null guard at `:375`, called only at
  `GameScreen.kt:162`; it was **not** lifted. Five of the six board cue effects remain private to
  that file (`GameScreen.kt:310-394`). The tutorial's only cue call site is
  `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt:105`
  (`JumpSoundEffect(transition = state.board.animating)`, the lifted effect at
  `app/src/main/java/com/app/pegsolitaire/audio/JumpSoundEffect.kt:44`). A screen that hosts no
  `Hint` effect cannot play `Hint` however its state moves. — **pass.**
- **The false premise is not duplicated.** `hintSource` and the phrase "never transitions" each
  occur exactly once in `docs/ux-spec.md`, both in the D-01i row now rewritten. `showHints` occurs
  three times; the other two (D-01b in §3, and the game-designer block quote in §4.1) state only
  that the dots are forced on, which is true (`TutorialUiState.kt:88`) and claims nothing about
  `hintSource`. — **pass**, one sentence to fix, and it is fixed.
- **The main game's existing tap behaviour, established from the code.**
  `GameViewModel.onCellTap` at
  **`app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:797-825`**: selection fires
  only when nothing is selected — **`:817`**,
  `state.selected == null && state.board.cellAt(position) == Cell.Peg -> select(position)` — and
  every other tap falls to the no-op that records the quiet invalid-tap ordinal, **`:823`**. Its
  KDoc states the rule outright at **`:788-796`**: *"re-tapping the selected peg is the ONLY way to
  deselect, and tapping a DIFFERENT peg while one is selected is a no-op (selection unchanged)"* —
  the rule quick task `260628-n4f` locked. — **pass:** in the main game a tap on a different peg
  changes nothing.
- **Does the ruling match what the developer built? NO — it requires a change.**
  `TutorialViewModel.onCellTap` at `TutorialViewModel.kt:119-136` has a fourth branch at
  **`:133`** — `board.cellAt(position) == Cell.Peg -> select(position)`, commented "Any other peg
  SELECTS, including while another peg is already held" (`:129-132`). Under this ruling that branch
  comes out and the case falls through to `registerNonProgressingTap()`, and `escalateCoach()`
  (`:270-279`) gains the release. The developer's Deviation-3 test that assumes re-selection changes
  with it. — **fail against the spec as now written; change owed.**
- **Scope kept.** The only files this pass wrote are `docs/ux-spec.md` and this report. The three
  `app/src/test/` files modified in the worktree are the concurrent developer's and were not staged
  or read for this ruling; `.planning/phases/15-ux-refresh/15-PLAN.md`, `STATUS.md`, `ROADMAP.md`,
  `FOUNDER.md`, `VERSION` and every other role's report are untouched. — **pass.**

## Open issues / risks
1. **The tap ruling is a change request, not a note.** S4 ships re-selection today; the spec now
   says otherwise, and the release clause is new work in `escalateCoach()`. Both are small and both
   are in `TutorialViewModel` only — no screen, no canvas, no cue. Producer to route.
2. **Criterion 3a is a device check with a two-screen shape:** it asks QA to perform the same tap on
   the tutorial *and* on a real game and see the same answer. That is deliberate — a divergence in
   tap semantics is only visible by comparison — but it means 3a cannot be discharged by a unit test
   alone. A Compose test that the selection is unchanged after a second-peg tap is worth having as
   the cheap half.
3. **Four stale `GameScreen.kt` line citations elsewhere in this document**, all created by S3's
   lift and none touched by this pass (out of its scope): §2 and D-01i's "Cost" paragraph and §10
   cite `JumpSound` at `GameScreen.kt:320-330` (now `JumpSoundEffect.kt:44`); D-01h cites the
   private cue effects as `:327-428` (now `:310-394`); §4.4's last line cites the `InvalidTap` path
   as `:393-398` (now `InvalidTapSound` at `:357-361`). They are citation drift, not wrong rulings.
   I did not fix them because this pass was scoped to two items and three of the four sit inside the
   audio ruling I was told not to touch. Worth one follow-up docs pass once S5–S8 land, since the
   same lift will move more lines.
4. **§4.1's heading still reads `D-01/D-01a–h`** while D-01i exists and is cited throughout the
   section. Same class of drift as item 3; left alone for the same reason.

## Handoff
Next: **producer** — one change request to route to the android-game-dev building S5–S8: remove the
"any other peg selects" branch in `TutorialViewModel.onCellTap` (`:133`) and add §4.4's release
clause to `escalateCoach()` (`:270-279`), with the Deviation-3 test rewritten to assert the
selection is *unchanged*. Nothing else in the spec changed for the developer.

Then: **qa-engineer** — criterion **3a** is new and criterion **3** gained a clause (the held peg is
put down when the coach escalates); both are device checks on the tutorial, and 3a is checked on the
gameplay board as well so the two screens are compared rather than trusted.

Then: **code-auditor** — the D-01i reason replacement is a documentation correction with the code
citations in Checks above; the verdict it documents is unchanged, so no audio behaviour is in
question.
