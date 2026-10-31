# tech-lead report — release-docs-f8
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer). Worktree `PegSolitarie-r5`, edited on top of `4d44106`. I committed nothing. Every fact was checked against `develop` at `0582e3f`, read-only in `PegSolitarie-dev`, and the audio facts against `feature/audio-review` at `8b62414` in `PegSolitarie-audio`.
Skills loaded: ergoz-soft:standards (role tech-lead). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read these plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. I also read the product's `.claude/rules/lessons.md`. `.planning/recall/RECALL.md` does not exist. I also invoked android-architecture and android-gradle-logic, but only after the edits. Neither bears on a factual text correction.
Ran in parallel with: the release-manager's release-docs fixes in this worktree (`release-manager-release-docs-fixes.md`, committed as `f589f36` and `4d44106` during this task; it touched none of my lines), and a game-designer ruling the hint-banner question (per the dispatch)
Ran on: claude-opus-5-5 at effort xhigh (policy: the agent file's pin; the harness names the model only). **Staffing deviation:** the tech-lead is pinned to fable. This task ran on Opus under the founder's Q-019 (answered 2026-10-17; recorded in `PegSolitarie-dev/.planning/questions/FOUNDER.md`): Opus for every Fable-pinned role until 3.0.0 is published, with each report recording the deviation.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **F-8:** all four errors in `docs/tech-design.md` are fixed against `develop`:
  - the per-board book depth;
  - the sound-file counts, now split into `develop` and the 3.0.0 set;
  - CI on `release/**`, now recorded as ruled but not built;
  - where `TutorialBoards` lives, plus the §2.1 package list.
- **F-1 (tech-design side):** §7.2 now records what the code does today. "No winning move" is a proof only when the book answers. A solver `null` folds a budget miss and a cancellation in with a true dead end. Wiegleb-45's book is depth 3. I did not rule the product question; it is recorded as open (§16 #21, §15 risk).
- **AC-F4:** the release-manager's finding is recorded. The corrected pattern is in §11 and §17, with a positive control.
- Each correction keeps the old text in a dated note, following the file's "entries are never deleted" rule.

## Outputs
- `docs/tech-design.md`, 87 lines added and 15 removed (my uncommitted edit). Section by section:
  - **Header:** a line recording this correction pass.
  - **§2.1 `:domain` row (F-8d):**
    - adds the `tutorial/` package (`TutorialBoards`);
    - moves `GetHintUseCase` to `usecase/`, where it is on `develop`, instead of `assist/`;
    - names the real `assist/` contents: `OpeningBook`, `OpeningBookFormat`, `AssistAllowance`.
  - **§3.3 (F-8d):**
    - `TutorialBoards` is in `domain/tutorial/`, not beside `Boards.kt`, which is in `model/`.
    - A dated note records the 2026-09-21 ruling's wording. It also records my decision that the package is accepted as built: the binding half of the ruling, "absent from `CLASSIC_SPECS`", holds (`Boards.kt:323-324`).
  - **§7.2 row 1 (F-8a, F-1):**
    - book depth per board: 4 on English-33, European-37, Asymmetric-39 and Diamond-41, and **3 on Wiegleb-45** (`OpeningBook.kt:121,129`; `wiegleb45-k3.txt`);
    - what depth K covers: the positions up to K moves in, which is K + 1 hints along a line that avoids the gap;
    - the two gaps: Diamond-41 440, Wiegleb-45 4;
    - the Challenge recorded-line path;
    - book proofs exist only on English-33 (4 positions).
  - **§7.2, new "What 'No winning move' means today" (F-1):** a transcription, not a ruling. It covers:
    - the banner is raised on any `null` (`GameViewModel.kt:1384-1385`, `strings.xml:108`, `GameScreen.kt:241`);
    - the book `NoWinningMove` is a proof, and the solver `null` is not (`GetHintUseCase.kt:30-34`, D-04; `GameViewModel.kt:1381-1383`);
    - where the banner can appear on a winnable position;
    - the Wiegleb-45 worst case;
    - what `DepthSweepTest` does and does not assert;
    - that the product question is open.
  - **§9.7 and the §17 CI row (F-8c):** `develop`'s `ci.yml:7-11` triggers on `develop` and `main` only. `release/**` is ruled and lands with check-gates C3b.
  - **§10 "Hint latency":** past each book's depth, a Classic hint is a solver call, and a miss reads "No winning move".
  - **§11 (AC-F4):** amended to the corrected pattern, with the reason, the result on `develop` and a positive control. AC-F7 is unaffected.
  - **§12 (F-8b):**
    - on `develop`: twelve `.ogg` files (9 Kenney, 3 freesound, CC0);
    - on `feature/audio-review`: twelve WAVs (7 Kenney, 3 freesound-derived, 2 in-house from `audio/recipes/`), which is the 3.0.0 set if that branch merges before the cut;
    - `GameAudioAttributes` exists only on that branch.
  - **§13 Sounds row (F-8b):** the 3.0.0 set (7 + 3 + 2) and the `develop` set (9 + 3) are now separate. The licence cell reads CC0 for the sourced files and own work for the in-house ones.
  - **§15:** a new risk row for the hint banner on a budget miss.
  - **§16 #21:** the banner question is open, owned by the game-designer and the ux-designer. Not ruled here.
  - **§17:** two rows: the AC-F4 token, and the §3.3 "beside `Boards.kt`" wording.
  - **Housekeeping, outside the findings:** §10's 15e row escaped the absolute-value bars in `floor = |…|`. They split that table row into 7 cells against a 5-cell header. The defect predates this edit.
- `.planning/reports/phase-14/tech-lead-release-docs-f8.md` — this report.

### AC-F4 — the corrected pattern (finding from `release-manager-font-rows.md`)
`tech-lead-fonts.md` AC-F4 says `git grep -n -E "googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle` returns 0 hits. On `develop` it hits `app/src/main/assets/licenses/Nunito-OFL.txt:1`: "Copyright 2014 The Nunito Project Authors (https://github.com/googlefonts/nunito)". OFL-1.1 requires that text to ship unmodified, so the AC as written can never pass on a correct tree. The corrected AC-F4 is that this command returns 0 hits:

    git grep -n -E "ui\.text\.googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle

- `ui\.text\.googlefonts` is the Compose package the token existed to catch, `androidx.compose.ui.text.googlefonts`.
- It still catches the package's `Font` import, which `GoogleFont` alone would miss.
- I chose to narrow the token rather than exclude `assets/licenses/` by pathspec. That keeps the whole `app` tree in scope.
- The tech design cited AC-F4 only inside the range "AC-F1–F10" (§11). AC-F4 is now amended there, as AC-F7 was, and a §17 row records it.

## Checks
All are read-only. Paths: `D=/Users/ermanergoz/Desktop/PegSolitarie-dev` and `A=/Users/ermanergoz/Desktop/PegSolitarie-audio`. Commands without `-C` ran in `PegSolitarie-r5`.

**F-8a, book depth**
- `grep -c 'committed \*\*depth-4 opening book\*\*' docs/tech-design.md` — pass (0; the wrong claim is gone, and only the dated note quotes it).
- `grep -c 'depth 3 for Wiegleb-45' docs/tech-design.md` — pass (1).
- `git -C $D grep -n -E 'BOOK_DEPTH = [0-9]' develop -- domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/OpeningBook.kt` — pass (`:121 BOOK_DEPTH = 4`, `:129 WIEGLEB_BOOK_DEPTH = 3`).
- `git -C $D ls-tree --name-only develop domain/src/main/resources/golden/book/` — pass (four `-k4.txt` books and `wiegleb45-k3.txt`).
- `head -1 $D/domain/src/main/resources/golden/book/*.txt` — pass (`proven-dead=4` on english33 only; every other book has 0).
- `for f in diamond41-k4 wiegleb45-k3; do sed -n 2p $D/domain/src/main/resources/golden/book/$f.txt; done` — pass (gaps of 440 of 818 and 4 of 140).

**F-8b, sound files**
- `grep -c '9 Kenney + 3 freesound borrows, per' docs/tech-design.md` — pass (0).
- `git -C $D ls-tree --name-only develop app/src/main/res/raw/ | grep -c '\.ogg$'` — pass (12). `grep -c` of the kenney.nl and freesound.org rows in `$D/docs/audio/CREDITS.md` — pass (9 and 3).
- `git -C $A ls-tree --name-only feature/audio-review app/src/main/res/raw/ | grep -c '\.wav$'` — pass (12).
- `git -C $A show feature/audio-review:docs/audio/CREDITS.md | grep -c` over the kenney.nl, freesound.org and own-work `.wav` rows — pass (7, 3 and 2).
- `git -C $A ls-tree --name-only feature/audio-review audio/recipes/` — pass (`hint.json`, `invalid.json`).
- `git -C $D grep -l GameAudioAttributes develop` — pass (no hits on `develop`; it exists only on the audio branch).

**F-8c, CI triggers**
- `grep -c 'on every push and PR to \`develop\`, \`release/\*\*\` and \`main\`' docs/tech-design.md` — pass (0).
- `grep -c 'is ruled but not built' docs/tech-design.md` — pass (1).
- `git -C $D show develop:.github/workflows/ci.yml | sed -n '7,11p'` — pass (`branches: [develop, main]` for both push and pull_request).

**F-8d, `TutorialBoards`**
- `grep -c 'beside \`Boards.kt\` and is absent' docs/tech-design.md` — pass (0).
- `grep -c '\`tutorial/\` (\`TutorialBoards\`' docs/tech-design.md` — pass (1).
- `git -C $D ls-tree -r --name-only develop | grep -E 'domain/(tutorial/TutorialBoards|model/Boards|usecase/GetHintUseCase)\.kt'` — pass (all three paths as the design now states).
- `sed -n 323,324p` of `Boards.kt` — pass (`CLASSIC_SPECS` is the five Classic boards).

**F-1, the hint banner**
- `grep -n -E 'GameViewModel.kt:1384-1385|GetHintUseCase.kt:30-34|^\| 21 \| Hint' docs/tech-design.md` — pass (§7.2 at :554 and :559, §16 #21 at :1178).
- `git -C $D show develop:…/GameViewModel.kt | sed -n '1384,1385p'` — pass (`if (move == null)` sets `noWinningMove = true`). `sed -n 1381,1383p` of the same file — pass (the budget-miss comment).
- `git -C $D show develop:…/GetHintUseCase.kt | sed -n '32,34p'` — pass ("unsolvable, budget-missed and cancelled all fold to the same `null` … not a proof").
- `sed -n 108p strings.xml` and `sed -n 241p GameScreen.kt` on `develop` — pass ("No winning move — undo or restart"; `noWinningMove` selects `nowin_message`).
- `grep -n 'DEFAULT_DEADLINE = ' SolverBudget.kt` on `develop` — pass (`1_500.milliseconds`).

**AC-F4**
- The AC as written, `git -C $D grep -n -E "googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" develop -- app gradle` — fail as expected (exit 0, the one hit `Nunito-OFL.txt:1`). This is the finding reproduced.
- The corrected pattern, `git -C $D grep -n -E "ui\.text\.googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" develop -- app gradle` — pass (exit 1, no hits). The same command over the `PegSolitarie-dev` working tree — pass (exit 1).
- Positive control, `git -C $D grep -c -E "<corrected pattern>" 'c9228a9^1' -- app gradle` — pass:
  - hits in `app/build.gradle.kts` (1), `Type.kt` (5), `font_certs.xml` (1) and `gradle/libs.versions.toml` (1);
  - the narrowed token alone hits `Type.kt:7-8`, the `googlefonts.Font` and `googlefonts.GoogleFont` imports.
- `git show HEAD:docs/tech-design.md | grep -c 'AC-F4'` — 0. Before this edit the design cited AC-F4 only inside the range "AC-F1–F10".

**Integrity**
- A `python3 -I` check of every Markdown table in `docs/tech-design.md` (each row's pipe count against its header's, escaped `\|` excluded) — pass (0 mismatched rows). It found 1 before the §10 escape fix, and that row predates this edit.
- `git status --short` — my only change is `docs/tech-design.md` plus this report. The five other files changed in this worktree are the release-manager's, committed in `f589f36`.

**Not run, as dispatched:** the Check command (no Gradle). The edit touches no Check input.

## Open issues / risks
- **The auditor's F-1 example is off by one, and the CHANGELOG wording undercounts by the same one.**
  - The book frontier is every position up to K plies from the start (`DepthSweepTest.frontierOf`; for the depth-4 books the sweep pins 0 fall-throughs on English-33, European-37 and Asymmetric-39).
  - So on Wiegleb-45 the fourth hint, asked after three moves, is still a book answer. The **fifth** hint, after four moves, is the first one off the book. The auditor wrote "a fourth Hint is off the book".
  - The conclusion stands: that fifth hint goes to a solver that measured no answer in time on any depth-≤4 Wiegleb position.
  - `CHANGELOG.md:140` now reads "the first four moves (three on Wiegleb-45)". If that means hints, the book answers five (four on Wiegleb-45). If it means the positions up to four moves in (three on Wiegleb-45), it is exact.
  - This is the release-manager's file. It is non-blocking, because it understates coverage and claims nothing false.
- **F-1 itself stays open.** The listing, the changelog's "now means *proven*" (`CHANGELOG.md:141`) and the banner question wait for the game-designer's ruling. If the ruling changes code, the mechanism comes to the tech-lead, and §7.2 and §16 #21 are updated then.
- **§16 #21 adds a risk row and an open item but rules nothing.** Both only record ownership.
- **Staffing deviation:** this ran on Opus, not Fable (Q-019). The review of this diff therefore runs on the same model as its author. The mitigation Q-019 names is a fresh reviewer who re-runs the commands above.
- **Not in this dispatch:** `release-manager-release-docs-fixes.md` (Open issues) routes `scripts/play-upload.sh` (F-6) to the tech-lead, for assignment to the android-game-dev. I did not act on it here. The producer should dispatch it as its own task.

## Handoff
Next: producer —
- commit `docs/tech-design.md` and this report on `feature/phase-14-release-docs`;
- return the branch to the same fresh code-auditor for F-8 and the tech-design side of F-1. The commands under Checks reproduce each fix;
- give the game-designer §7.2's transcription ("What 'No winning move' means today") and the off-by-one note above as inputs to the banner ruling;
- tell the release-manager that the `CHANGELOG.md:140` count is ambiguous (Open issues).
