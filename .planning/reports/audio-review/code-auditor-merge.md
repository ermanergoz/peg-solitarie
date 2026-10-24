# code-auditor report — audio-review (develop merge-in, hunk read)
Date: 2026-10-24
Branch: feature/audio-review (report only; read: merge `13c04c6` (parents `b39e28e` audio, `dbd0a6c` develop) and `9f0e67a`)
Skills loaded: ergoz-soft:standards (plugin 1.0.0 rule files read in full, since the product snapshot is 0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing, audio; the product's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (deviation: the agent file pins fable; Opus under Q-019 option 3, because the Fable limit is reached)
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
- I read both conflict resolutions in `13c04c6`. Each matches the ruling in sign-off §2.3 exactly, and nothing was lost from either side. The auto-merged `GameUiState.kt`, `GameScreen.kt` and `GameViewModel.kt` carry both sides' changes intact and make sense together.
- The merge has no hand edits beyond the three conflicted files (checked with `--remerge-diff`).
- **APPROVED SUPPRESSION** for the edited, pre-existing `@Suppress("LongMethod")` at `GameScreen.kt:159`. `9f0e67a` follows the finding-2 ruling to the letter.
- One MINOR finding: the moved list leaves out `NoWinBanner`. This predates both branches, affects only documentation and does not block the merge. There are also two INFO notes. **VERDICT: PASS.**

## Staffing deviation (Q-019)
The agent file pins fable at xhigh. This read ran on Opus because the Fable limit is reached, under Q-019's answer 3 ("Opus for every Fable-pinned role until 3.0.0 is published … Recorded as a deviation"). The developer also ran on Opus (`claude-opus-5-5`, per its report), so rule 1's "reviewed on a model superior to its author's" **does not hold** for this read: reviewer and author share a model.

Q-019's mitigations were met as follows:
- **Fresh reviewer:** met. I saw only the files named in the dispatch, not the author's chat or the log.
- **Reviewer-run Check:** not run, following the dispatch's "re-run only if a hunk looks wrong". No hunk did. Instead, I read the Check's own outputs on disk and confirmed they were written after the last source edit (Checks).
- **Mutation probes:** none apply. The merge's own content is two blocks placed next to each other (`GameAnim.kt`), one side taken byte-for-byte (`TutorialViewModel.kt`) and one comment-only commit (`9f0e67a`), so no logic of its own exists to mutate.

## Hunks read
1. **`app/src/main/java/com/app/pegsolitaire/ui/game/GameAnim.kt:45-90` — as ruled (§2.3 item 1).**
   - `:45-53` is develop's block (`HINT_NOTICE_DISPLAY_MS = 5_000`, `HINT_NOTICE_EXIT_MS = 160`, with their KDoc), then the blank line at `:54`.
   - `:55-90` is audio's "The wrong tap" block, from `WRONG_TAP_SHAKE_MS` to `WRONG_TAP_MARK_HOLD_MS`.
   - Measured, nothing lost: audio→merged adds exactly develop's 10 lines and nothing else. Develop→merged adds 37 lines and removes 0. Audio's text after `DOT_POP_END` is byte-equal to the merged text after `HINT_NOTICE_EXIT_MS`.
   - Nothing was renamed or reordered, and no conflict markers remain.
2. **`app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModel.kt:83-85` — as ruled (§2.3 item 2).**
   - The merged file is byte-identical to `b39e28e` (`git diff --stat b39e28e 13c04c6` is empty).
   - Develop's only change to this file since the merge base `0582e3f` is `@ApplicationScope` → `@param:ApplicationScope` at old `:76`. That change is contained in audio's constructor at `:86`, so C7's site is fixed.
   - `:83` is `hapticsSetting: HapticsSetting`, and `:84-85` is the `@param:` comment (KT-73255, accurate). There are three parameters, matching the class KDoc's "exactly **three** collaborators" and the reflection pin in `TutorialViewModelTest`.
3. **Auto-merged files, read for sense (§2.3 item 3).** For each file, the numstat of base→develop equals audio→merged, and base→audio equals develop→merged, so both sides arrived whole.
   - **`GameUiState.kt`:** `hintUndecided` (`:142`) and `rejectedTapCell` (`:148`) are both in the constructor, both `initial()`/`loading()` builders and the KDoc. They are independent: neither branch reads the other's field.
   - **`GameScreen.kt`:**
     - `WrongTapHaptic` (`:188`) sits among develop's unchanged effect list (`:182-195`).
     - The board keeps audio's live region and `wrongTapCorrection = true` (`:249-255`).
     - Develop's banner-or-notice `if/else` (`:266-285`) is unchanged around audio's board hunk.
     - The two features share the board `Box` with no shared state.
   - **`GameViewModel.kt`:** audio's two hunks are the wrong-tap `copy` (`:823-828`) and the board-install comment (`:1654-1656`). Develop's hint hunks are at `:304-1416`. No hunk overlaps another, and no wrong-tap path touches `hintUndecided`, which matches both KDocs: a wrong tap clears neither the notice nor the ordinal's partner, and every board install rebuilds through `initial()`.
4. **`9f0e67a`, `GameScreen.kt:143-159` — as ruled (hint sign-off §2, finding 2).**
   - The list is now the KDoc's last paragraph, "Surfaces hosted, one per line:" (`:149-157`).
   - The annotation reads exactly `@Suppress("LongMethod") // one screen hosts every surface its KDoc lists` (`:159`).
   - The "amend the list" sentence is repointed at the paragraph (`:143-147`).
   - It is still the file's only `@Suppress`, and the repo has no detekt baseline.
   - Every new line is ≤ 100 characters. The list names both branches' additions: the Undecided notice (`:154`) and the wrong-tap answer on the board panel's line (`:153`). That meets §2.3 item 3's "names every surface from both".

## Suppressions
**APPROVED SUPPRESSION: `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:159` — `@Suppress("LongMethod")`, pre-existing since `fd08809`, edited only in its comment.**
- The reason holds. `GameScreen` is the gameplay screen's single stateless renderer. It hosts the Scaffold, status strip, snackbar host, board slab, banner/notice slot, action dock and two dialogs, plus the effect calls; its body (`:162-318`) is well past detekt's default `LongMethod` threshold (`config/detekt/detekt.yml` does not override it).
- Splitting the function is a refactor outside a merge-in.
- The edit makes the suppression more honest, not less: one line per surface in the KDoc replaces a 120-character inline list that had no room left.
- The only change is where the list lives. There is still one suppression, no baseline entry, and no `@Ignore`, `ignoreFailures` or `!!` anywhere in the audio-relative diff.

## Findings
1. **MINOR: `NoWinBanner` is a hosted surface the list does not name.** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:149-157` (the list) against `:266-279` (the banner it hosts).
   - The KDoc says the list holds "every surface this composable hosts" (`:143`).
   - The rescue banner is an interactive surface with Undo, Restart and Home. It was added in `6035840` (2026-07-13), five days after the suppression's first version (`fd08809`), and no version of the line since has named it.
   - The old inline "board panel" item could be read as covering the overlay in the board `Box`. The new line ties "board panel" to `[BoardCanvas]`, and the hint branch listed the banner's slot-mate, the Undecided notice, as its own item. Under the list's own rule, the banner is now the one hosted surface left unnamed. `LoadingIndicator` (`:230`) is the board slot's loading state and reads fairly as part of the board panel line.
   - This predates both branches (the developer's open issue 2 names it) and affects documentation only, so it is not a merge blocker.
   - Fix: one line, `- the rescue banner, [NoWinBanner];`, between the board-panel and Undecided-notice lines.
   - When it lands is the tech-lead's call: a one-line docs commit on this branch before the merge, or together with `feature/talkback-board`'s planned append, which already touches the banner's `onUndo`/`onRestart` lambdas.
2. **INFO: the list renders as part of `@param challengeLevel`.**
   - `:143-157` follow the last block tag (`@param challengeLevel`, `:139-141`). Under KDoc's block-tag rule, a tag's description runs until the next tag, so Quick Documentation and Dokka show both paragraphs under that parameter.
   - This was already true of the suppression paragraph at `0582e3f:137-140`, and the ruling asked for "the last paragraph".
   - It does not matter to the source reader the list exists for. If the tech-lead wants the list rendered on its own, both paragraphs can move above `@param state` (`:121`) at the next amendment. This is not a change request.
3. **INFO: the move is in its own commit, not "in the same commit that adds its wrong-tap effect".** Correct as done. The text being moved ("Undecided notice") exists only after the merge-in. The wrong-tap commit is already history, and this branch merges develop in rather than rebasing (sign-off §2.1). Accepted.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/code-auditor-merge.md`: this report, uncommitted as dispatched. I changed no source and ran no Gradle.

## Checks
- `git show --remerge-diff --format= --name-only 13c04c6`: pass. Only `.planning/questions/FOUNDER.md`, `GameAnim.kt` and `TutorialViewModel.kt` differ from git's own auto-merge, so every other file is git's merge, untouched.
- `git show --remerge-diff --format= 13c04c6 -- app/`: pass. In `GameAnim.kt` both sides are kept, develop's first, with no markers left. In `TutorialViewModel.kt` the audio side is kept, and develop's side of the conflict was empty.
- `git diff b39e28e 13c04c6 -- …/GameAnim.kt`: pass (+10/-0, develop's block at `:45-54`). `git diff dbd0a6c 13c04c6 -- …/GameAnim.kt`: pass (+37/-0). `diff` of audio's tail against the merged tail: "audio tail == merged tail".
- `git diff --stat b39e28e 13c04c6 -- …/TutorialViewModel.kt`: empty, byte-identical. `git diff 0582e3f dbd0a6c -- …/TutorialViewModel.kt`: one line, `@param:ApplicationScope`.
- `git diff --numstat` per side, for `GameUiState.kt` / `GameScreen.kt` / `GameViewModel.kt`: pass. Base→develop equals audio→merged (20/8, 15/1, 41/19), and base→audio equals develop→merged (10/0, 36/32, 9/3).
- `git show --format= 9f0e67a`: pass (+16/-5, one file, KDoc and the annotation's comment only).
- `grep -n '@Suppress\|@file:Suppress' …/GameScreen.kt`: one hit, `:159`.
- `git ls-files | grep -iE 'baseline|detekt'`: only `config/detekt/detekt.yml` and a phase-13 QA report name, so no baseline file. `grep -n LongMethod config/detekt/detekt.yml`: no hit, so detekt's default threshold applies.
- `git diff b39e28e HEAD -- app domain build-logic | grep -nE '^\+.*(@Suppress|@Ignore|ignoreFailures|baseline|!!)'`: 6 hits. One is the edited annotation. The other five are the word "baseline" in KDoc: one in `GameScreen.kt:147`'s "detekt baseline entry", four in develop's test KDoc meaning the 360x640dp design-baseline window.
- `git log -G'Suppress\("LongMethod"\)' -- …/GameScreen.kt` and `git log --reverse -S'NoWinBanner(' -- …/GameScreen.kt`: the line has five versions (`fd08809`, `6b07152`, `542292f`, `45f2b96`, `9f0e67a`). The banner arrived in `6035840`, and no version names it (finding 1).
- `awk` over `GameScreen.kt:140-160`: no line over 100 characters.
- On-disk outputs of the developer's Check, read by me, not re-run (dispatch):
  - `stat` puts `GameScreen.kt`'s mtime at 09:12:39.
  - `app/build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.txt` was written at 09:15:18 and is 0 bytes.
  - `app/build/reports/detekt/detekt.xml` was written at 09:15:01 and is an empty `<checkstyle>`.
  - The JUnit XML tallies are `app testDebugUnitTest 1214 failures 0 errors 0 skipped 0` and `domain test 294 failures 0 errors 0 skipped 0`, written at 09:22:39. All of these postdate the last source edit, and they agree with the recorded total of 1,565 (1,214 + 294 + depthSweep 1 + fuzz 1 + build-logic 55).
  - The developer's recorded Check: `./scripts/check.sh --max-workers=4`, pass, 12m 41s, 1,565 tests, 0 failures, C7 clean in debug and release.
- `git log -3 --format=…`: pass. The subjects are 72 and 60 characters, conventional, and each commit has exactly one `Co-Authored-By: Claude Fable 5.1` trailer.
- `uptime`: load averages 23.81 15.12 17.61, which is one more reason not to start a 12-minute Check that no hunk calls for.

## Open issues / risks
- **Q-019 mitigation not fully met.** The reviewer-run Check was waived by the dispatch, and no mutation probe applies. If the producer reads Q-019 as requiring a reviewer-run Check on every review, one run of `./scripts/check.sh` on this tree is owed, and it is the only thing that could change this verdict.
- **Finding 1** stays open until the tech-lead places the one line (this branch or `feature/talkback-board`).
- `FOUNDER.md:3-40` (the third conflict) was not read by me. It is documentation under tech-design §17, and its hunk re-read and the developer's open issue 1 (whether a third conflicted file is a §2.5 hold) belong to the tech-lead.

## Handoff
Next: **tech-lead** (fresh): re-read `git show --cc 13c04c6` and `git diff 13c04c6^2 13c04c6 -- …/TutorialViewModel.kt` against §2.3, rule on finding 1 and on the developer's open issues 1–2, and read `FOUNDER.md:3-40` under §17. **producer:** `git merge --no-ff feature/audio-review` into develop once that re-read passes, and route finding 1 as the tech-lead rules. Nothing goes back to the android-game-dev from this read.

VERDICT: PASS
