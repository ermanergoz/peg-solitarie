# ux-designer report — hint-undecided (check of D-04f, §4.4.2, criteria 23a–23k)
Date: 2026-10-21
Branch: bugfix/hint-undecided (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch, plus agents/ux-designer.md and the product's `.claude/rules/lessons.md`. Further skills: ui-ux-pro-max, android-accessibility, compose-skill (and its references/accessibility.md).
Ran in parallel with: the tech-lead's amendment of `tech-lead-brief.md` in this worktree (per the dispatch). I treated its §6 and §8 as possibly changing.
Ran on: claude-opus-5-5 at effort xhigh. The effort is the agent file's pin, because the harness names no effort. **Staffing deviation:** the ux-designer is pinned to fable at xhigh (`agents/ux-designer.md`). This check ran on Opus under the founder's Q-019 (answered 2026-10-17): Opus for every Fable-pinned role until 3.0.0 is published. One safeguard in rules/studio.md is therefore suspended for this check: a fable-written spec is meant to be checked by a fresh fable agent. I did not read the author's report (`.planning/reports/hint-undecided/ux-designer.md`), as the dispatch instructed.
Fan-out: none. Two haiku Explore helpers ran `diff`, `grep` and `find` and returned raw output, because this role has no Bash, Grep or Glob. Neither ran git. Neither wrote or committed anything.
Loops: 0
Lessons applied: none. The snapshot holds 0 active lessons, and the product has no `.planning/recall/RECALL.md`.

## Summary
- **The amendment is faithful to the ruling.** It meets all six points of GD-HT §2 and GD-HT-3. It changes no existing line: every hunk of `61c4bb4..f221ae4` is an addition apart from the front-matter date. Its ids are unique. Every criterion can be tested, either as a JVM test or as a named device procedure. On this branch's code it is feasible to build.
- **No BLOCKER or MAJOR finding. Five MINOR findings and seven INFO notes.**
- **F-1:** the banner's live region needs a ruling before H4. `NoWinBanner.kt` is frozen in the brief's §6, and "23k is owed either way" does not say which branch 23k applies to.
- **F-2 to F-5** are spec edits for the author: the Cancelled row, a pointer at criterion 21a, the TalkBack position, and what the notice covers at 200% font scale.
- **The tr-TR "copy of record only" ruling is sound.** No `values-tr*` or `values-b+tr*` directory exists in any of the 8 worktrees. The two branches I could not see without git get a one-line check in the Handoff.

## Findings

Severity scale: BLOCKER (the amendment cannot ship), MAJOR (it must change before the build depends on it), MINOR (fix in a follow-up spec commit, preferably before H4), INFO (no change required).

**F-1 — MINOR — The banner's announcement collides with the brief's frozen file, and the spec does not say where 23k applies.**
- Lines: `docs/ux-spec.md:2793-2803`, `:381`, `:4443-4447`.
- **The collision.**
  - The spec makes the banner's message a polite live region (`:2796-2798`). The region has to sit on the message `Text` (`NoWinBanner.kt:68-72`) and leave out the three buttons, so the edit must go inside `NoWinBanner.kt`.
  - The brief freezes that file. Its §6 requires `git diff develop...HEAD -- …/NoWinBanner.kt` to print nothing (`tech-lead-brief.md:222`).
  - The spec says so itself and routes the choice to the tech-lead and the producer (`:2799-2803`). That handling is correct, and it is not silent.
- **What is left open.** `:2803` says "Criterion 23k is owed either way". Suppose the line moves to its own branch. Then 23k cannot pass on `bugfix/hint-undecided`, and the auditor and QA have no rule for whether to fail this branch or carry 23k forward.
- **Asked of the spec:** one sentence saying that 23k applies to whichever branch carries the semantics line, and is not judged on a branch that does not carry it.
- **My recommendation on the choice itself** (the tech-lead's and the producer's to make): the line rides this branch, as the author recommends. The brief's §6 should allow exactly that one semantics hunk.
- **Why:**
  - If the line is split off, 3.0.0 can ship with the notice spoken and the proof silent: TalkBack would say "it may still be winnable", and say nothing on a position proven lost (`:2793-2796`).
  - The change does not touch the banner's look, copy, actions or slot.
  - It also gives the stuck banner its first announcement. Today that banner is silent (no `liveRegion` anywhere in `ui/game/`, helper grep).
- This must be settled before H4 starts.

**F-2 — MINOR — The "Cancelled" row names a cause the UI cannot produce, and as a test row it cannot be told apart from idle.**
- Lines: `docs/ux-spec.md:2488`, `:2608`, `:3897`.
- **The cause.** The row lists "a new Hint tap" as one way a search gets superseded.
  - In the UI that cannot happen: Hint is disabled while it is Working (D-04d, `:984-987`), and passed criterion 23 says "a second tap cannot start a second search" (`:4363-4364`).
  - Where it does happen (a repeat request to the ViewModel), the control is Working, not "Available — identical".
- **The test row.** §13 asks 21a's table-driven test to add the row, expecting Available (`:3897`). But a cancelled search leaves the same state as idle: `hintUndecided`, `noWinningMove` and `solverThinking` are all false. The row therefore re-runs the idle row and proves nothing new.
- **Asked of the spec:**
  - In the UI-facing tables, keep the ruling's domain definition but name only jump, undo, restart and leaving as the causes the player can produce.
  - Either state the row's state vector explicitly (all three flags false after each of those events), or turn the row into a note under the table.

**F-3 — MINOR — Criterion 21a, a passed phase-15 criterion, widens by reference without a pointer at the criterion.**
- Lines: `docs/ux-spec.md:2495-2498`, `:3897` against `:4326-4331` and `:4366-4367`.
- §4.4.1 says "The two new rows join criterion 21a's table" (`:2497-2498`), and §13 repeats it. So the change is not silent in the document.
- It is silent at the criterion itself:
  - 21a's text in §14 (`:4326-4331`) has no dated pointer.
  - The new group's header says "criterion 23 and the rest of this section stand unchanged" (`:4366-4367`), but 21a's coverage did change.
- The house style requires a one-line pointer on any amended item (`:389-391`).
- **Asked of the spec:** a dated pointer under 21a naming the two added rows (D-04f). This matters most for a QA engineer who reads §14 alone.

**F-4 — MINOR — The notice's TalkBack position depends on a traversal line the specified layout probably does not produce, and no criterion checks it.**
- Lines: `docs/ux-spec.md:2768-2769` and `:3734-3735`, which rely on `:3691-3692`; host at `:2664`; device check at `:4430-4434`.
- **What the spec says.** §11's gameplay order ends with "dock (Undo → Hint → Show-moves → Restart) → any raised banner". The notice "takes the banner's place" in that order.
- **What the layout gives.**
  - The host the spec names is the board region's `Box`. The `Column` lays that `Box` out above the dock (`GameScreen.kt:193-262`).
  - The app sets no `traversalIndex` or `isTraversalGroup` (helper grep, 0 hits).
  - So Compose's default geometric traversal is expected to read board → notice (or banner) → dock: the reverse of the line.
  - The line predates this amendment (it is phase-15 text, unchanged in the diff). The notice now inherits it.
- 23j.5 checks only that the notice is one focus stop, not where that stop sits.
- **Asked of the spec:**
  - State the order the host actually produces, or name the traversal modifier that would produce the stated order.
  - Add the position to 23j.5.
  - Correct §11's line with a dated note rather than in place.

**F-5 — MINOR — At large font scale the notice hides much of the board for as long as the system timeout, and the player cannot dismiss it.**
- Lines: `docs/ux-spec.md:2673`, `:2675-2677`, `:2690-2695`, `:2706-2708`, `:3790`.
- **What the notice covers.**
  - It is opaque (`surfaceContainerHighest`) and lets every touch through.
  - At 200% font scale it is ≈170–220dp of a 368dp board region, up to about 60% of it.
  - It stays for 5 s, or for the system's "Time to take action" value. That setting is honoured on purpose, and Android offers up to 2 minutes.
- **What the player can do.**
  - A peg under the notice can still be picked up but cannot be seen. Picking up a peg deliberately does not end the notice (`:2706-2708`).
  - It ends only on a jump, an undo, a restart or a new Hint request. Undo may spend an allowance, which leads to the ad offer.
- The spec checked that the notice fits in the board region (`:2677`, `:3733`). It did not check what the notice covers.
- **Asked of the spec:** an explicit ruling. One option: once the base 5 s has passed, the first touch on the board ends the notice. Another: give it a TalkBack dismiss action, which is the M3 snackbar's own idiom. If the spec decides to keep today's behaviour, say so and give the reason.

**I-1 — INFO — Ids are unique. One in-flight collision exists, and the spec handles it.**
- Lines: `docs/ux-spec.md:291-315`; definitions at `:1016` (D-04f), `:2591` (§4.4.2) and `:4371`–`:4443` (23a–23k, one definition each).
- None of these ids appears in develop's copy of the spec (`PegSolitarie-dev`). None appears in the `-ux`, `-r5`, `-audio` or `-gates` copies.
- The audio amendment proposes "new §4.4.2" (`PegSolitarie-audio/docs/ux/invalid-tap-feedback.md:154`, `:471`). It also proposes a D-06 family, including **D-06c, a polite screen-reader announcement for a wrong tap** (`:125`).
- The header already tells that fold-in to take neither id (`:313-315`).
- **Added here:** when audio folds in, D-06c must also be reconciled with the new §11 "Announcements" line (`:3719-3723`).

**I-2 — INFO — The tr-TR "copy of record only" ruling is sound.**
- Lines: `docs/ux-spec.md:2616`, `:2641-2649`, `:2858-2859`, `:3422`.
- **The evidence:**
  - There is no `values-tr*` or `values-b+*` resource directory in any of the 8 worktrees (helper `find`). Those worktrees cover develop, this branch, release-docs, release-plan, ux-spec-folds, audio-review, check-gates and candy-backdrop.
  - The build declares no `localeFilters` or `localeConfig`.
  - The tr-TR listing says "Oyunun arayüzü İngilizcedir." (`PegSolitarie-r5/docs/store/tr-TR/full_description.txt:37`). It uses "İpucu (Hint)" (`:15`) and the "siz" register.
- **The Turkish sentence** is grammatical. Its impersonal passive avoids choosing a register.
- **Two small points:**
  - "konum" does not appear in the listing. It is acceptable as written.
  - The wording "`res/values` and `res/values-night` only" (`:2645`) is slightly off: the `-candy` and release-plan worktrees hold `values` only.
- **The claim "on every branch" is checkable without git everywhere except two branches:**
  - `main`: its reflog's last update is a merge of develop.
  - `feature/phase-14-kickoff`: one docs commit.
  - The producer can close both with the git line in the Handoff.

**I-3 — INFO — The "never twice" rule does not cover a recreation while the notice is still up.**
- Lines: `docs/ux-spec.md:2710-2718`, `:4411-4413`.
- The rule covers a notice that has already timed out. It does not cover a configuration change mid-display, such as a dark-mode switch, a font-scale change or a window resize.
- In that case a fresh timer and a newly composed live-region node would give a second full 5 s, and the notice might be spoken again.
- Consider saving the raise's elapsed time and extending 23g to a recreation mid-display.

**I-4 — INFO — The timeout call is written as shorthand.**
- Lines: `docs/ux-spec.md:2692-2694`.
- `LocalAccessibilityManager.current` is nullable, and the spec's call omits that. Fall back to `HINT_NOTICE_DISPLAY_MS` when it is null, which is what M3's `SnackbarHost` does.

**I-5 — INFO — Two citation and wording nits.**
- `:2495-2496` attributes "four ways" to GD-HT-1, which names three outcomes. The fourth, Cancelled, comes from GD-HT-3.
- `:2626-2628` calls `ad_unavailable_*`'s dash an instruction. It is a resolution: "hints refilled for free", and `strings.xml:270-272` calls the shape "problem — resolution". The argument still holds, because the notice gives neither.

**I-6 — INFO — "Not counted as a hint used" is not restated.**
- Line: `docs/ux-spec.md:2611`.
- The spec says an Undecided hint is free (GD-HT-5). It does not repeat D-09's "counted only when revealed".
- That rule belongs to the ViewModel (the ruling's AC-14), and criterion 24 already forbids any count in the UI, so no screen is affected.
- Optionally add one clause to the outcome table.

**I-7 — INFO — Reduced motion has only a device check.**
- Lines: `docs/ux-spec.md:4395-4399`, `:4429`.
- Only 23j.4 (a device check) covers it. A JVM variant of 23e is cheap: set `ANIMATOR_DURATION_SCALE` to 0 in Robolectric and assert that the display time is unchanged.

## Checks against the dispatch

**Faithful to the ruling** (GD-HT §2, GD-HT-1/2/3/5): **pass.**

| Ruling point | Where the spec meets it |
|---|---|
| Never the rescue banner | `:1019-1020`, `:2607`, `:2684-2686`; R-7 keeps the flag out of `showRescueBanner` (`:3381`); tested by 23a and 23h |
| Never "no winning move" | `:2615`, `:2632`, `:2639`, `:3440-3441`; tested by 23b |
| Never undo or restart | `:2632`, `:2850-2853`; tested by 23b |
| Never "try again" | `:2633-2637`. This is stricter than the ruling, which allowed an invitation after the next move. |
| Free, not counted | Free at `:2611` and `:4619-4620`; counting is I-6 |
| Neutral and short-lived | Neutral container `:2667`; 5 s plus the clearing events `:2688-2708`; never resumed `:2710-2713` |

Two related points also hold. A cancelled search shows nothing (`:2608`, `:2682`, `:2854-2855`). A deadline on an unchanged board is never silence (`:2679-2682`, the ruling's AC-11).

**Consistent with the rest of the spec:** pass, except F-2, F-3 and F-4.
- Every diff hunk is an addition (`7c7` is the front-matter date). No passed criterion's text was edited.
- The banner's look, copy, actions and slot are unchanged. Its one change is disclosed (F-1).
- The dock reads `solverThinking` only (`GameActionDock.kt:86-123`), so the "Available — identical" rows are true.

**Every criterion testable:** pass.
- On the JVM: 23a–23i and 23k's first half. These are Compose UI tests, ViewModel tests and a unit test, all with named matchers, tags and clocks.
- On a device: 23j and 23k's second half, as named TalkBack procedures.
- F-4 asks for one more device clause.

**Accessibility:** pass, except F-4 and F-5.
- **Polite live region:** `:2757-2771`, tested by 23d.
- **Not colour alone:** four channels that are not hue (`:2774-2778`, `:3726-3728`), tested by 23j.6.
- **System timeout honoured:** `:2690-2695`, tested by 23e and 23j.5.
- **Reduced motion:** `:2745-2747`, `:3563-3565`, tested by 23j.4.
- **Contrast:** I did not recompute it from `Color.kt`. Criterion 27 on a device governs.

**Feasible on this branch:** pass, except F-1.
- **Host.** `GameScreen.kt:193-252` already holds the banner in a `Box` at `BottomCenter`.
- **Touches pass through.** `clayShadow` is `Modifier.shadow` and `clayEntrance` is a `graphicsLayer` (`Clay.kt:132-138`, `:181-201`). Neither consumes a pointer event, so a plain container passes taps through.
- **The snackbar rejection is accurate.**
  - The host is an M3 `Snackbar`, a `Surface`, using `surfaceContainerHighest` and `shapes.medium` (`ClaySnackbarHost.kt:54-68`).
  - The `Scaffold` places it at the screen's foot (`GameScreen.kt:178-181`).
  - The message is shown through a queued `showSnackbar` (`:447-462`).
- **Tokens and constants.**
  - `board_max_size` is 480dp (`dimens.xml:20`), `shape_corner_extra_large` is 32dp (`:205`), and `ClayAnim.ENTRANCE_MS` is 240 (`ClayAnim.kt:29`). So the 160 ms exit is two thirds of the entrance, as stated.
  - `GameAnim` has no `HINT_NOTICE_*` names (`GameAnim.kt:17-44`), and `HintIcon` exists (`HintIcon.kt:29`).
- **ViewModel seam.**
  - The clearing sites match R-7 and the brief: `GameViewModel.kt:931`, `:1280` and `:1347`.
  - The jump clears the flag in the same update that sets `animating` (`:917-937`), so the notice leaves "as the slide starts".
- **Timing.** In production an Undecided comes from the 1.5 s deadline. The attempt and node caps outlast it (`SolverBudget.kt:53,82,95`; `Solver.kt:71,87,106`), so the spinner frame is always composed between a Hint tap and its answer. That means the screen-side raise count (`:2762-2766`) always sees the flag go false and then true.

**en-US wording and house tone:** pass.
- Like `challenge_load_error` ("Couldn't load this level. Tap Retry."), it uses two plain sentences and a "Couldn't" subject. It names the control as `hint_action` and the listing do.
- "position" is new in the app's strings. It matches the listing line the ruling suggests for after R1.
- The apostrophe escape follows `strings.xml:138`.

**tr-TR ruling:** sound (I-2).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-hint/.planning/reports/hint-undecided/ux-designer-check.md`: this report. I edited no other file and committed nothing.

## Checks
This role has no Bash. Every check is a full or ranged Read, or a helper's `diff`, `grep` or `find`. Nothing ran git.
- `Read` `.git/logs/refs/heads/bugfix/hint-undecided`: pass. The branch is `0582e3f` → `61c4bb4` (brief) → `f221ae4` (spec) → `ad7b83d` (author's report). The worktree HEAD is this branch, and the dev worktree is on `develop` at `0582e3f`. So `diff PegSolitarie-dev/docs/ux-spec.md PegSolitarie-hint/docs/ux-spec.md` equals the amendment, given that `61c4bb4` added only the brief.
- `diff -u <dev>/docs/ux-spec.md <hint>/docs/ux-spec.md | grep '^@@'` (helper): 25 hunks. Every change command is `a` (an addition) except `7c7`.
- `Read docs/ux-spec.md`: `:1-449`, `:560-699`, `:880-1039`, `:2380-3224`, `:3380-4637`. These cover every changed region and §11, §13 and §14 in full.
- `grep -n` for `D-04[a-z]`, `23[a-k]`, `undecided`, `tr-TR`, `nowin_message` over both copies (helper): pass. Each id is defined once. Develop's copy has none of them.
- `grep` of the same ids over the `-ux`, `-r5`, `-audio` (spec and `invalid-tap-feedback.md`) and `-gates` copies (helper): only the audio §4.4.2 and D-06 proposals, recorded as I-1.
- `find PegSolitarie* -type d -path '*/src/*/res/values*'`, `-name 'values-tr*'`, `-name 'values-b+*'` (helper): pass. There is no Turkish resource in any worktree.
- `grep localeFilters|resourceConfigurations|localeConfig` over `app/build.gradle.kts` and the manifest (helper): 0 hits.
- `grep traversalIndex|isTraversalGroup` over `app/src/main/java` (helper): 0 hits (F-4).
- `Read` the ruling (all 333 lines), the brief (all 396 lines), `NoWinBanner.kt`, `GameUiState.kt`, `GameScreen.kt`, `GameActionDock.kt`, `strings.xml`, `dimens.xml`, `GameAnim.kt`, `ClayAnim.kt`, `ClaySnackbarHost.kt`, `SolverBudget.kt` (all), `Clay.kt:1-80,120-229`, `GameViewModel.kt:900-949,1296-1404`, `Solver.kt:56-165`, `HintIcon.kt:1-40`, and the r5 `tr-TR/full_description.txt`: pass, as cited in the findings.
- **Not run:** `./gradlew check`. I have no Bash, and this is a review of a docs-only diff that changes no Check input.

## Open issues / risks
- **F-1 waits on the tech-lead and the producer.** H4 cannot satisfy both the spec and the brief's §6 until one of them changes.
- **Possible staleness.** The brief was being amended while I read it. If its §8 seam changes (for example, a tick appears), R-7 (`:3381`) and the "no tick" rule (`:2717-2718`) must be re-read against it.
- **Q-019 deviation.** This check and, per the brief, the brief itself ran on Opus. The fable cross-check of a fable-pinned spec is suspended until 3.0.0 is published.
- **Contrast** (`:2780-2788`) was not independently recomputed. Criterion 27 on a device governs.

## Handoff
Next: producer.
- **tech-lead, before H4 (F-1):** decide whether the banner's one semantics line rides `bugfix/hint-undecided`. I recommend it does, with §6 of the brief allowing exactly that hunk in `NoWinBanner.kt`.
- **ux-designer (the spec's author), in a follow-up spec commit before H4:**
  - F-1's sentence on where 23k applies;
  - F-2's Cancelled row;
  - F-3's pointer at 21a;
  - F-4's traversal order and its clause in 23j.5;
  - F-5's ruling on what the notice covers.
  - I-1 to I-7 are optional.
- **producer, the branch check I could not run:** `git log --all --oneline --name-only -- 'app/src/*/res/values-tr*' 'app/src/*/res/values-b+tr*'` must print nothing. That closes I-2 for `main` and `feature/phase-14-kickoff`.
- **At the audio fold-in (I-1):** renumber its "new §4.4.2", and reconcile D-06c's polite announcement with §11's new "Announcements" line.
- **Commit:** this report, after reading it.

VERDICT: PASS
