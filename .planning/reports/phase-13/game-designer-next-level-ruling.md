# game-designer report — phase-13 "Next level" after a merged resume (ruling)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (93874bd..93874bd — nothing committed by me; this report is left uncommitted for the producer)
Skills loaded: ergoz-soft:standards (role game-designer) — plugin copies of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`, `ip-policy.md`, plus the repo snapshot `.claude/rules/lessons.md`; `the-fool` (pre-mortem mode, run against my own proposed ruling — mode selection was fixed by the brief rather than asked, because a subagent must not put an `AskUserQuestion` in front of the founder)
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Serving level 9 (in QA's final run, level 12) is a defect** — and not merely against loose UAT wording: phase-13 decision **D-04 states the opposite in its own text** ("the ladder stays at 50 and 'Next level' serves level 50"). The shipped behaviour contradicts a locked decision of the phase it shipped in.
- **Ruling: "Next level" serves `max(ladder head, N+1)`** — the Challenge tile's number. In every normal play that is N+1 and nothing changes; only behind-the-head finishes differ.
- **Nothing is told to the player. D-03 stands unamended** — and the button carries no number (`action_next_level` = "Next level", no format arg), so no promise of "N+1" is ever made on screen to be broken.
- **Does not block phase 13.** No CLOUD-0x criterion fails; no data loss; the ladder is intact; the head is reachable from Home. Fix as a small follow-up **before the v1.1 release branch is cut**.
- The fix belongs in Challenge navigation (Results), **not** in cloud-save code — the rule must hold on a device that has never signed in.

---

## A. Defect or acceptable — defect, low severity

Both readings in the brief were weighed. Reading (ii) ("Next level" means "the one after the level you just played") loses, for four reasons:

1. **D-04 already decided it, in the opposite direction.** `13-CONTEXT.md` D-04 is not ambiguous: *"Solving it calls `advancePast(10) = max(50, 11) = 50`, so the ladder stays at 50 and 'Next level' serves level 50."* The UAT sentence QA marked FAIL is a faithful restatement of that, not a loose paraphrase. The wording is not the thing that is wrong.
2. **The cost is unbounded, and QA saw its mildest form.** The damage is `gap − 1` replayed levels, where `gap = head − N`. QA's run had `gap = 2` → one replay, which reads as a curiosity. D-04's own worked example is `gap = 40` → **39 consecutive already-solved levels** before the player sees new content again, each one ending on a Results screen that can award no new star and move no ladder, each one handing them "Next level" into the next replay. That is a phone-plus-tablet player's ordinary situation, not a corner case, and it is the shape of a quit.
3. **It contradicts the mode's only promise.** CHAL-01/CHAL-05 and the Core Value line are "sequential, never-repeating, always forward". Challenge has no level picker and no replay affordance anywhere in the app precisely because the ladder *is* the navigation. A forward action that silently points backwards is the one thing the mode is not allowed to do.
4. **A replay of a completed level is a null play.** Stars merge by `max`, score by `max`, time by `min` — a level already at 3/3 cannot be improved on the dimension the Results screen celebrates. The player spends a whole session for a Results screen that structurally cannot say anything new.

Against that, reading (ii)'s real argument — "9 → 14 is disorienting" — is worth something but costs less: it is one moment of *positive* surprise (I am further along than I thought), on a screen where the number was never promised, and the player has been staring at `Continue — Level 13` on Home since the merge. Disorientation for disorientation, the backward serve costs the same confusion **plus** a wasted play session, and at larger gaps costs dozens.

**Severity: low** (QA's classification stands). No data loss, no crash, ladder monotonic throughout, head always reachable from Home.

## B. What "Next level" should serve

**Rule (D-04a):** the Results "Next level" action serves **`max(challenge_current_level, N + 1)`**, where `N` is the level just solved and `challenge_current_level` is read *after* `advancePast(N)` has been applied.

- **In one sentence for a developer:** "Next level" is a pointer to the ladder head, never an arithmetic successor of the level just played — with `N + 1` kept inside a `max` so a stale ladder read can never re-serve `N`.
- **Why `max(...)` and not plain "the head":** the pre-mortem's worst failure mode is a read-before-write race serving the player the level they just finished, i.e. an unbreakable loop on one board. `max` makes that state unreachable by construction, even if the ladder write lands late. This is load-bearing, not belt-and-braces: the repo already carries a documented ladder/load-window race note (WR-01).
- **Why not "the lowest uncompleted level":** with a monotonic ladder and in-order solving, the lowest uncompleted level **is** the head — always, with no holes possible (the merge takes `max` of two heads, and neither device can hold a best above its own head). They are the same number; the head wins because it is a single stored field that three surfaces already agree on, needs no query, and is exactly what the Challenge tile displays.
- **What the player should see and feel:** they tap an unnumbered "Next level" and land on a board whose header names the level they already knew they were on from Home. The feeling to aim for is *"right, I'm back where I was"* — continuity with the other device. The feeling to avoid is *"why am I doing this one again?"*, which is what the current behaviour produces.
- **Consequence that is accepted, not a bug:** D-04 deliberately keeps the stale board, so the player may finish one already-completed level (the resumed one). That single play is the price of not destroying an unfinished board and is the right trade. It is also exactly why the *next* one must not be a second one — one is an artefact of a preserved board; two is a pattern the player will read as the game being broken.

## C. Does the player need to be told anything — no, and D-03 stands

**Nothing is shown. No toast, no dialog, no banner, no "(already solved)" annotation, no Settings row.** D-03 is not amended and its reasons hold in full:

- **The merge is still not an event.** D-03's premise is that a restore is invisible because nothing the player did caused it. This ruling does not create a player-visible sync event; it makes an existing forward action agree with a number the player is already looking at.
- **No promise is broken on screen.** Confirmed: the label is `action_next_level` = **"Next level"**, unnumbered, with no format argument — the app never tells the player the next one will be `N + 1`. There is therefore no expectation to manage.
- **The number is already visible in two places the player has seen** — the Home Challenge tile (`Continue — Level 13`, shown before and after the merge) and the gameplay screen's own level header. That is the explanation, and it is ambient rather than announced, which is the D-03 style.
- **Any copy we could write would be the rejected banner in disguise.** "Your progress from another device moved you ahead" is a progress-restored notification with different wording, and it would also train players to tap through sync prompts — the exact failure `.planning/REQUIREMENTS.md` bans under "Conflict-resolution dialog on every sync".

Residual risk accepted: a small number of players may read the skip as "the game skipped a level". It is strictly smaller than the confusion the current behaviour causes, and it is self-correcting the moment they look at Home. If support evidence ever contradicts that, the right response is the **existing** Challenge tile subtitle, not a new notification — and it would be a D-03 revision argued on its own evidence, not a side effect of this ruling.

## D. Severity and timing — ship phase 13; fix before the v1.1 release

- **Does not block the phase 13 merge.** Checked against the phase's four success criteria and CLOUD-01..05: the union is lossless and prompt-free (CLOUD-02), the in-progress board is preserved and never synced (CLOUD-04), failure still degrades silently (CLOUD-05). None of them is the behaviour in question. Re-opening a green, four-times-verified cloud phase to change *Challenge navigation* is the larger risk; the change does not touch a line of cloud-save code.
- **Fix before the release branch is cut for v1.1** — a small, self-contained follow-up (`bugfix/*` or a quick task), sized at one call site plus tests. Rationale for not deferring further: the phase would otherwise ship against its own recorded decision D-04, and the defect's cost scales with the merge gap, which grows the longer a two-device player uses the app.
- **The UAT text stays as written.** `11-HUMAN-UAT.md` test 11's third clause is correct and should remain the manual check; test 11's result line should stay `PASS, D-04 sub-check PARTIAL` until the follow-up lands, then be re-run at a gap of ≥ 2.
- **Invariant and named tests** (JVM, no device, all in Challenge navigation — every invariant maps to a test name):
  | Invariant | Test name |
  |---|---|
  | Behind the head, "Next level" serves the head | `ChallengeNextLevelTest.nextLevelIsLadderHeadAfterSolvingBehindTheHead` |
  | At the head, behaviour is unchanged (N+1) | `ChallengeNextLevelTest.nextLevelIsSuccessorWhenSolvedLevelIsTheHead` |
  | Never re-serves the level just played, even on a stale ladder read | `ChallengeNextLevelTest.nextLevelNeverServesTheLevelJustPlayed` |
  | Never serves a level recorded as completed while the head is ahead | `ChallengeNextLevelTest.nextLevelNeverServesACompletedLevelWhenTheHeadIsAhead` |
  | Manual, two devices, gap ≥ 2 | `11-HUMAN-UAT.md` test 11, D-04 sub-check clause 3 |
- **Not my call, stated for the tech-lead:** where the ladder is read on the Results path and how the ordering against `advancePast` is guaranteed is architecture, not gameplay. The gameplay contract is only the `max(head, N+1)` rule and the four invariants above.

## E. Related cases this ruling must cover

Confirmed by inspection of every Challenge entry point (four call sites, all funnelling through one navigation helper; the only catalog UI in the app is Classic-only):

1. **There is no Challenge level picker, level list or replay-an-old-level UI.** So the *only* ways to be below the head today are (a) D-04's kept in-progress game after a merge, and (b) Results "Retry" re-entering that same level. **Both are covered by the same rule** — after a retry of a behind-head level, solving it also serves the head.
2. **First play after a restore on a fresh device (UAT test 10).** No in-progress game exists, so `N` equals the head and `max(head, N+1) = N+1`. Behaviour is identical to today. The rule degenerates correctly and needs no special case.
3. **Offline divergence / reconvergence (UAT test 13).** The device solving offline is at its own head throughout, so again `N+1`. Covered, no change.
4. **Classic mode is untouched.** Classic has no ladder and no "Next level" action; this ruling is Challenge-only and Classic's Results must **not** gain one. Replaying a Classic board stays first-class — that is a deliberate difference between the modes, not an inconsistency.
5. **Abandoning the resumed level** (exit without solving) produces no Results and no "Next level" — nothing to rule on; the Home tile continues to point at the head.
6. **Forward-looking, so the question is not re-opened later:** if a Challenge level browser or a daily-challenge replay ever ships (v1.2), the same rule pre-decides it — a replayed completed level either shows **no** "Next level" affordance at all, or one that still points at the head. It must never point at `played + 1`, and a replay must never move the ladder or fire "New best!" unless it genuinely beats the stored best.

## Outputs
- `.planning/reports/phase-13/game-designer-next-level-ruling.md` — this ruling (the decision record; **D-04a**). No app source, test, plan or UAT file was edited.

## Checks
- Read `.planning/reports/phase-13/qa-engineer-uat-final.md` (test 11, D-04 sub-check clause 3, and the "Open issues" bug entry) — pass (observation taken as given, not re-derived: resumed level 11, merged head 13, "Next level" served 12).
- Read `.planning/phases/13-cloud-save/13-CONTEXT.md` D-01..D-16 — pass, and **material**: D-04's own text specifies the merged head, which turns "is the UAT wording loose?" into "the build contradicts the decision".
- Read `.planning/REQUIREMENTS.md` (CLOUD-01..05, Milestone Decisions: cloud-save scope, conflict resolution; Out of Scope: conflict dialogs on every sync) — pass, no requirement is violated by the current behaviour, which is why D is "do not block".
- Read `.planning/PROJECT.md` (Core Value, Challenge description, Key Decisions) and `.planning/milestones/v1.0-REQUIREMENTS.md` (CHAL-01..05, PERS-02) — pass; CHAL-05 "continue from their highest reached Challenge level" is the v1.0 precedent this ruling restores.
- Read `.planning/ROADMAP.md` phase 13 goal and its four success criteria — pass (none of the four is the behaviour under ruling).
- Read `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` test 11 — pass (expected text is a faithful restatement of D-04; it stays as written).
- `docs/gdd.md` — **absent**; no GDD exists for this product, so this ruling could not be filed in GDD §13 as the role procedure directs. Recorded as a risk below and filed here instead.
- `.planning/answers/` — **absent** in this repo (it follows gsd-core layout); the ruling is delivered at the report path the brief named rather than inventing a parallel tree.
- `.planning/recall/RECALL.md` — absent; `.claude/rules/lessons.md` read, 0 active lessons.
- Two UI facts confirmed by read-only inspection (delegated, no files changed): the Results action string is `action_next_level` = "Next level" with **no level number**; the served level is computed from the Results route argument (`level just played + 1`) and `challenge_current_level` is never read on that path; and there is **no Challenge level picker anywhere** — the only entry points are the Home Challenge tile (head), the Home Continue tile (in-progress), Results "Next level", and Results "Retry" (same level).
- `the-fool` pre-mortem run against my own proposed ruling — pass, and it changed the ruling: the "serve the head" formulation was replaced by **`max(head, N+1)`** after the "stale ladder read re-serves level N, player loops on one board forever" narrative, and the "fix it in the cloud path" option was eliminated by the inversion check ("what would guarantee failure": coupling Challenge navigation to a cloud event, when the rule must hold on a never-signed-in device).
- No Gradle, emulator, git or app-source action was taken — pass (`git status` untouched by me; the role is prohibited from production code).

## Open issues / risks
- **`docs/gdd.md` does not exist.** Three phase-13 reports now note `docs/tech-design.md` is missing; the gameplay side has the same hole. Gameplay decisions of this kind (D-04a) have nowhere canonical to live and are accumulating in phase context files, which future phases do not read. Recommend a `docs/gdd.md` be scheduled — not inside phase 13, and not authored under a bug-fix deadline.
- **D-04's text in `13-CONTEXT.md` is now historically inaccurate about shipped behaviour** until the follow-up lands. Whoever files the follow-up should cite D-04 *and* this ruling as D-04a, so the next reader does not conclude the decision was silently reversed.
- **Gap-of-1 blindness is a UAT hazard beyond this bug.** The superseded run passed clause 3 only because `head = N + 1`. Any future two-device UAT that exercises the ladder should stage a gap of **≥ 2** by construction. Worth adding to `docs/qa-plan.md` alongside QA's offline/online conflict recipe.
- **Founder taste, non-blocking, no question queued:** whether a two-device player would *prefer* an explicit "you're further ahead on another device" acknowledgement is ultimately taste. I am not queueing a `Q-<NNN>` because D-03 already decided the class of question and nothing in this evidence weakens it; if the producer wants it asked anyway, the recommendation to carry is "stay silent".
- I did not run the repo Check command or any test: this task produced no code and the role is prohibited from it. The invariant table above is a specification for whoever writes the follow-up, not a claim that tests exist.

## Handoff
Next: **producer** — route the follow-up and record the decision.
1. Merge phase 13 as planned; this is not a blocker.
2. File the follow-up as `bugfix/challenge-next-level-head` (or a quick task) **before the v1.1 release branch**: `max(challenge_current_level, N + 1)` at the Results "Next level" action, plus the four named JVM tests. **tech-lead** owns where the ladder is read and how ordering against `advancePast` is guaranteed; **ux-designer** need not be involved — no copy, no layout and no new surface changes.
3. Leave `11-HUMAN-UAT.md` test 11 at `PASS, D-04 sub-check PARTIAL` and re-run the sub-check at a gap of ≥ 2 after the fix; the expected text needs no edit.
4. Record this ruling as **D-04a** wherever phase decisions are kept, and consider scheduling `docs/gdd.md`.
