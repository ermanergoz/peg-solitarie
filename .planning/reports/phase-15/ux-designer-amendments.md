# ux-designer report — phase-15 ux-spec amendments (game-designer + tech-lead rulings)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (966b404..this commit — two `docs(15):` commits: c802908 the
amendment, then the internal-consistency pass it exposed)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, lessons.md
Ran in parallel with: phase-14 (in flight in other worktrees — not touched)
Loops: 0
Lessons applied: none

## Summary
Applied both rulings to `docs/ux-spec.md`, the document this role owns: all of the
game-designer's board ruling and all six of the tech-lead's proposed diffs, plus the
consequences that follow from them. The tutorial's separation from gameplay is now a stated
prohibition (new §5.1), not an inference. Three new decisions carry the rulings in house style
(D-01g hosting, D-01h tutorial audio, D-03h the Home progress read); no earlier decision was
rewritten in place. §14 gained six criteria and rewrote four, including 15e, which is now the
two-arm delta gate rather than a single absolute number.
Nothing was escalated back; one ruling consequence I had to decide myself (tutorial audio) is
recorded as a UX decision, and one conflict for the producer is in Open issues.

## Outputs
- `docs/ux-spec.md` — amended in place: header Amendments note; §2 tutorial row; new D-01g,
  D-01h, D-03h and amendment pointers on D-01a, D-01f, D-03d and D-03g; §4.1 step table, block-quote, Advance,
  Completion and Exit; §4.2 ribbon source + placeholder rule; §4.4 wrong-tap table; **new §5.1
  "The tutorial's hard prohibitions"**; §6.1 token note; §6.5 risk paragraph + measurement
  contract; §7 opening, R-1/R-2/R-4/R-5, new R-6, invariant resolution; §8 string table (one
  string retired); §9 audio row; §11 sound paragraph; §13 test bill + screenshot question
  closed; §14 criteria; §16 traceability. Second commit: the fourteen consistency fixes listed
  below, including the tutorial's scroll behaviour (§4.1/§11/§12) and the corrected T3 peg count.
- `.planning/reports/phase-15/ux-designer-amendments.md` — this report. The first edition's
  report `ux-designer.md` is untouched.
- No file under `app/`, `domain/`, `docs/ui/` or `.planning/{ROADMAP,questions,reports/STATUS}`
  was changed, and neither ruling report was edited.

## Checks

No build in this task — it edits one Markdown document and no source. Per the brief, the
per-diff disposition is the check, plus `file:line` for everything I verified against code
rather than taking on trust.

### The seven proposed diffs

| # | Proposed diff | Disposition | Why |
|---|---------------|-------------|-----|
| GD-1 | §4.1 step table — replace all three rows with the proven T1/T2/T3 footprints | **Applied verbatim** | Board content is the game-designer's authority and the proof is exhaustive enumeration over the product's own `RulesEngine`, not argument. The old T2/T3 were my defaults and were explicitly labelled as such. |
| GD-2 | §4.1 block-quote — replace "board content is the game-designer's call" with the closed ruling | **Applied, extended** | Took the proposed text, then added the three screen-side consequences that are mine and were not in the diff: the 3×3 fits `tutorial_board_size` at ≈93dp cells (so §6.1 stands, §11's floor holds), T1/T2 being single-move changes how the coach must read, and the `TutorialBoards`-not-`CLASSIC_SPECS` rule needed to be visible where the screen is specified, not only in §7. |
| GD-3 | §4.1 "Advance" — add the uniform one-peg completion predicate | **Applied, then reconciled with TL-6** | The sentence is the game-designer's; the tech-lead's later ruling forbids expressing it via `GameStatus` or `minimumPegs`. Merged: "the board holds one peg", evaluated as `board.pegCount == 1` on the tutorial's own state, with the `GameStatus` route named as the first step toward `GameViewModel`. See "Collisions" below. |
| GD-4 | §4.4 class-3 row + retire `tutorial_undo_nudge` from §8 | **Applied, adapted** | Row applied as written. The string is deleted from the table and recorded under a new "Retired before it shipped" note rather than silently vanishing, so a reader of the copy contract can see why the count dropped. The game-designer left per-step escalation copy to me: I kept "re-show the step's own caption" — no new string, and it cannot claim the player is stuck. |
| TL-1 | §7 R-1/R-2 — replace the client-side fold with one `Flow` SQL aggregate | **Applied, and its UX consequence made explicit** | Data-layer mechanism is the tech-lead's authority. Two things it changes on my side are now in the spec: a **failed** read must render "—" and never `0` (D-03h #1, criterion 13a), and the unbounded-history cold-start risk retires (§6.5, D-03h #2). §4.2's ribbon Source column was updated to match — it still said "folded from `observeResults`". |
| TL-2 | §7 R-5 — mark resolved (Hilt `TutorialViewModel`, no `SavedStateHandle`) | **Applied** | Also added the matching prohibition to §5 ("no `SavedStateHandle` step cursor"), because the reason it is forbidden is a UX decision (D-01e) that a developer could otherwise "fix" in good faith. |
| TL-3 | §7 — add the hosting resolution after the invariant paragraph | **Applied, extended** | Added the resolution as proposed, then stated the player-visible failure in D-01g: a dropped `markSeen()` leaves Home re-pushing the tutorial, which is a trap the player cannot escape — that belongs in a decision, not only in a data note. |
| TL-4 | §6.5 Handoff — the two-arm measurement contract | **Applied in full** | Instrument, seeded ladder, both pass conditions, the failure routing, and where the tables go. Criterion 15e rewritten to match (see below). |
| TL-5 | §13 "Risk worth naming" — close the screenshot question | **Applied, extended** | Closed as ruled and not reopened. Added the binding list Phase 15 owes instead (testTags, retargets not deletions, one UI test per changed screen, the `:domain` board test, the exit-ordering tests, the denominator guard, the placeholder tests) — §13's own two named gaps needed an answer, and the answer is that list. |
| TL-6 | §5 / §4.1 — the tutorial's negative contract | **Applied as a new §5.1, strengthened** | The brief asked for this to read as a requirement. §5's bullet list is a list of absences; a four-row table with the damage spelled out per row is harder to skim past. Row 3 names the real cost — a first-run tutorial deleting a returning player's Continue chip and advancing their ladder three times, making `11-HUMAN-UAT.md` 10/11/13 unverifiable. |

Count: seven proposals (one game-designer ruling, six tech-lead diffs), enumerated above as ten
edit sites because GD-1..GD-4 are four separate edits inside the one ruling. All applied; none
rejected; none escalated.

### Consequences applied that were not themselves a proposed diff

- **§5.1** — the hard prohibitions (the brief's item 2), stated as requirements.
- **D-01h, the tutorial's audio** — routed to me by the tech-lead (its Open issue 3) as a
  decision nobody had consciously taken. Ruled: **silent**. §4.4's two wrong-tap rows claimed
  the `Select` and `InvalidTap` cues and were factually wrong once the tutorial hosts
  `BoardCanvas`; §9's audio row and §11's sound paragraph now match, and criterion 7c tells QA
  not to file the silence as a defect.
- **§4.1 "The board the tutorial drives"** — D-01a says the board is driven by "a real
  `GameUiState` and the real rules engine", which reads as licence to reach for `GameViewModel`.
  Clarified that `GameUiState` there is a UI model assembled by `TutorialViewModel`.
- **§4.1 Exit** — one exit path, three callers, persist-before-pop, replay restarts at T1.
- **§4.4** — the note that wrong-tap class 1 fires on T2/T3 by design (the game-designer's
  risk 4), so QA expects it.
- **§7 opening** — kept the "no schema change" sentence (it is still literally true) and added
  the honest cost D-03h does carry: one query, one projection, one repository member, and a
  one-line override in each of the 16 test doubles.
- **§16 traceability** — PERS-03 and CHAL-05 now point at §5.1, because those two requirements
  are exactly what the prohibition protects; CLAS-01 points at the `CLASSIC_SPECS` rule; PLAT-08
  names the delta gate.

### §14 acceptance criteria — what changed and why

| Criterion | Change | Reason |
|-----------|--------|--------|
| 2 | Strengthened to name the structural guarantee (T1 horizontal-only, T2 vertical-only) | The old wording was a hope; the new boards make it unbreakable, and the criterion should say so. |
| 3 | Now names the tap: T2's middle peg | It was unverifiable as written ("a peg that cannot move" — which one?). The new boards make the answer concrete. |
| 4 | Adds "never mentions Undo or tells the player they are stuck" | Follows the retirement of `tutorial_undo_nudge`. |
| 5 | Adds "Home does not immediately re-open the tutorial" | The persist-before-pop failure is player-visible and was checked nowhere. |
| **7a (new)** | Nothing behind the tutorial moves: stars, both Continue chips and the Challenge level unchanged after a full run, on a player who has a saved Challenge game | This is §5.1 made checkable, and it is the criterion that protects the two cloud-save oracles. |
| **7b (new)** | A Settings replay always restarts at T1 | The bug an Activity-scoped state holder would ship; now load-bearing. |
| **7c (new)** | The tutorial plays no sound | D-01h, and it stops silence being filed as a defect. |
| **13a (new)** | A failed read shows "—", never `0` | D-03h; a `0` on a player with forty stars is a lie on the first screen. |
| **13b (new)** | Boards-pill denominator is 5 and no tutorial board appears in the catalogue | The game-designer's "single easiest mistake to make with this ruling", made device-checkable. |
| **15e** | Rewritten as the two-arm delta gate: both arms, same AVD/host/sitting, seeded ~200-level ladder, after-median ≤ 950 ms **and** delta ≤ 50 ms, with the failure routing | As written it no longer matched the gate it was checking: a single absolute number against a pre-SDK reference would charge five phases of SDK cost to this refresh. |

Criteria 1, 6, 7, 8–15d, 15f and 16–28 are unchanged and still hold.

### Verified against code rather than taken on trust

- `GameViewModel.kt:956-975` — the `Won` path: `clearSnapshot()` at `:963`,
  `challengeProgress.advancePast(solvedChallengeLevel)` at `:973`, both inside the
  `finishedEmitted` latch. This is the fact §5.1 rows 2 and 3 rest on, and it is as reported.
- `HomeScreen.kt:198` — `if (state.profileSwapVisible && !state.onboardingVisible && …)`, with
  the comment at `:188-197` naming the second writer of `onboardingVisible`. Confirms the guard
  survives in shape as `!state.tutorialPending` (§7).
- `OnboardingOverlay.kt:155-171` — T1's footprint (`JUMP_SPEC` + two `withPeg` calls) is exactly
  the kept board. The first edition cited `:156-165`, which covers the spec but not the pegs;
  §4.1 now cites `:155-171`.
- `OnboardingOverlay.kt:144` — the overlay passes `onboarding_illustration_size` to
  `BoardPreview`, which is the dead token §6.1 retires. Unchanged by this amendment.

### Internal-consistency pass over the amended document

A full read-through of the amended spec was run against nine consistency questions (stale
"fold"/"default"/"undecided" claims, cue claims contradicting D-01h, malformed tables, duplicate
decision or requirement ids, §14 numbering, dangling cross-references). It reported **clean** on
the amendment's own targets — no surviving `tutorial_undo_nudge`, no surviving client-side-fold
claim, no surviving "T2/T3 footprints are defaults", no undecided-hosting claim without a
pointer, every table well-formed, no duplicate ids, §14 numbering sound and every criterion
cross-reference resolving. It found fourteen other defects, all fixed in the second commit:

- **One factual error, mine, and it is the one worth naming.** §4.4 and criterion 3 said "two of
  T3's five pegs" have no legal jump at the start. I re-derived it from the ruling's footprint
  (pegs (0,0),(0,1),(1,0),(1,1),(1,2); holes (0,2),(2,0),(2,1),(2,2)): all three legal openings
  start at (0,0) or (0,1), so the pegs with no move are (1,0),(1,1),(1,2) — **three**, the whole
  middle row. Corrected in both places, with the reason stated so QA can check it.
- **One real contradiction between two sections, mine to resolve.** §12 said the tutorial column
  scrolls at 200% font; §4.1 specified a fixed four-band full-viewport layout with no scroll
  container. Resolved in §4.1's favour of scrolling, with the board explicitly **not** shrinking
  (compressing the one interactive element to win vertical space is the wrong trade at 200%);
  §12's row and §11's text-scaling list now match.
- **One gap in D-01h that criterion 7c had already assumed.** The decision enumerated board cues
  only; criterion 7c required "no completion cue". §4.1's completion borrows
  `ClayAnim.STAR_POP_MS`, which is a duration and not a cue, but nothing said `SoundCue.StarPop`
  was excluded. D-01h now names the completion cues too, and §4.1 says the star pops silently.
- **A decision-id collision worth a standing warning.** This document's `D-05` is the toolbar
  deletion; §4.4 also cited an earlier phase's `D-05` (the ad allowance at
  `GameUiState.kt:131-135`), and `D-09`/`D-14`/`D-15` are likewise earlier phases'. §3's preamble
  now warns about it once and §4.4 qualifies the one genuine collision.
- **Housekeeping:** D-03g gained the amendment pointer the house style requires (its gate is now
  the two-arm delta); the preamble's ruling count was short by one and now says why the
  measurement contract is a §6.5 subsection rather than a decision; §4.1/§4.2/§4.4's heading
  scopes were stale (`D-01a–f`, `D-03a–e`); §5's `SavedStateHandle` bullet cited D-01g where the
  decision it protects is D-01e; §1 pointed at §5 for an argument that lives in §3's D-05; §2 and
  §5 claimed "no change to any board footprint" while the refresh adds three new tutorial
  miniatures; and D-01f's "§8 lists the invariant" was a pre-existing typo for §7.

### Not run
- `./gradlew check` — **not run.** This task edits two Markdown files and no source, so the build
  tree is byte-identical to the branch point apart from `docs/ux-spec.md` and this report. The
  tech-lead's report records `ANDROID_HOME=… ./gradlew check --offline` green at 11m10s on this
  worktree as the pre-refresh baseline, taken at the same commit.
- No device, emulator or measurement — neither is this role's authority, and the C1 procedure is
  specified for the developer and the gate for the qa-engineer.

## Open issues / risks

1. **One conflict for the producer to route back — the tutorial's audio is my ruling on a
   question the tech-lead raised, and the sound-designer has not seen it.** D-01h (silence) is
   within this role's authority over feedback and it is what the tech-lead recommended, but
   rules/audio.md makes the sound-designer the owner of cues, and `docs/audio-spec.md` may need
   a line saying the tutorial names no cue. **Not blocking**; routing it to the sound-designer
   for confirmation is cheap and should happen before the phase's audio review.
2. **No ruling was reverted and no ruling conflicts with a UX requirement**, so nothing goes
   back to the game-designer or tech-lead for that reason. The one place two proposals collided
   — GD-3's completion predicate versus TL-6's "no `GameStatus` in `:app`" — was reconciled in
   the tech-lead's favour on the mechanism and the game-designer's on the wording, because the
   tech-lead's ruling postdates it and is about a call site, not about gameplay.
3. **`docs/ux-spec.md` is now ~1300 lines.** It is a five-screen contract carrying three
   rulings, and it is still one document per the role definition; if the producer wants the
   phase-scoped `15-UI-SPEC.md`, the header already says this file is the source and that file
   is a pointer.
4. **§17 checker sign-off is still `pending` and `status: draft`.** This role does not sign its
   own document (rules/studio.md standing rule 1); the checker dimensions are for a reviewer.
5. **Q-901 and Q-902 are unchanged and still open, still non-blocking, still defaulted.** The
   tech-lead's note that Q-901's option 2 puts an animation on the cold-start path is already
   the reason §15 defaults to option 1; nothing needed to change.

## Handoff

Next: **producer** — (a) confirm D-01h with the sound-designer (Open issue 1), non-blocking;
(b) the spec is ready to plan against: §4.1/§5.1 and §7 name the files, the prohibitions and the
tests for the tutorial, §6.5 names the measurement contract, and §14 is the criteria list.
Then: **tech-lead / planner** — §5.1's four prohibitions and §13's test list are the two places
this document now binds the plan; both were written to be checkable by grep or by a named test.
Then: **android-game-dev** — read §5.1 before writing the tutorial: hosting `GameScreen` or
`GameViewModel` there is the one mistake in this phase that looks correct on screen and is wrong
in the database.
Then: **qa-engineer** — criteria 3, 4, 7a, 7b, 7c, 13a, 13b and 15e are the new or rewritten
device checks; 7a needs a player with a saved Challenge game and existing stars, checked before
and after a full tutorial run.
