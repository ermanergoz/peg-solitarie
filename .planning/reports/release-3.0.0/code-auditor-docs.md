# code-auditor report — release-3.0.0: text review of three docs-only branches before the 3.0.0 cut
Date: 2026-11-01
Branch: feature/release-3.0.0-prep (report only). Reviewed, each off `develop` `d251c3b`: `feature/ux-3.0.0-folds` `b4fe627`; `feature/tech-design-3.0.0-sync` `958c44c` + `5ae8421`; `feature/release-3.0.0-prep` `7d43d7f` + `d0e2860`
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The snapshot here is 0.5.1/0.6.2 and the plugin is 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose and testing were read in full; puzzle-design and play-store are byte-identical to the snapshot, read in full there; `.claude/rules/lessons.md`. Also code-reviewer and security-reviewer. compose-skill and compose-performance-audit not loaded: no Compose code in any diff
Ran in parallel with: none known
Ran on: claude-opus-5-5 at effort xhigh. **Deviation:** `agents/code-auditor.md` pins fable; founder answer Q-019 puts every Fable-pinned role on Opus until 3.0.0 is published. The three authors also ran on Opus, so this review is not on a model above theirs (rules/studio.md, Model and effort); every fact below was re-read in the tree or the cited report instead of trusted
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- The three branches touch disjoint files, and every pair merges cleanly (`git merge-tree`). Most claims check out against the merges, the reports and the code at `d251c3b`. `scripts/play-upload.sh` was read fresh, and the as-built contract in the checklist's §4 note and tech-design row PU is accurate.
- `./gradlew :app:bundleRelease` is the right Build command: AGP's lifecycle task, already hooked in `app/build.gradle.kts:262`, writing the AAB the script uploads. No hook reads the line. Three documents still say the change happens on the release branch, and `AGENTS.md:7` still says `assembleRelease`.
- **release-prep FAIL.** T-11's invocation rule is looser than the tech-lead's ruling: a `/usr/bin/env …` form, which the ruling proved passes the publish gate, reads as allowed. Both release-manager reports lack the mandatory `Ran on:` line.
- **ux-folds FAIL**, on the report alone: its spec text is accurate and insertion-only, but the report has no `Ran on:` line.
- **tech-design sync PASS**, with two minor findings. It is insertion-only, confirmed by a word-level diff, and its numbers re-derive.

## Findings

### `feature/release-3.0.0-prep` — must fix
- **docs/release-checklist.md:38 and :115-117** — **T-11 is weaker than the ruling it transcribes.**
  - The tech-lead's ruling reads: "The only invocation is `./scripts/play-upload.sh`, with optional `VAR=value` or **bare** `env …` prefixes" (`.planning/reports/play-upload/tech-lead-signoff.md:106`).
  - The same sign-off's probe table shows the publish gate passing `/usr/bin/env ./scripts/play-upload.sh`, `/usr/bin/env bash …`, `/usr/bin/nohup …`, `/usr/bin/time …` and `exec /bin/bash …` (its §5, rows 4–8).
  - The checklist drops "bare". It forbids only "`/bin/bash …`, `/bin/sh …` or any interpreter [shell] named by its path". So `/usr/bin/env ./scripts/play-upload.sh` reads as an allowed "`env …` prefix", and `/usr/bin/nohup` and `/usr/bin/time` are not excluded.
  - This is the E-1 stopgap, and it is the only thing between a path-named wrapper and an upload without the approval file until the plugin fix is installed. Fix: restore "bare `env`", and forbid any wrapper or interpreter named by its path, with those examples. Tech-design §15's E-1 row and §16 #36 (tds branch) already name `/usr/bin/env`.
- **.planning/reports/release-3.0.0/release-manager-prep.md:1-8 and release-manager-docs-pass.md:1-8** — **neither report has a `Ran on:` line.**
  - rules/reporting.md says: "A report with no Checks section, no `Ran on:` line, or no `Lessons applied:` line is invalid". This holds in plugin 1.0.0 and in this worktree's 0.6.2 snapshot (`.claude/rules/reporting.md:30`).
  - Smaller format slips: `release-manager-prep.md:3`'s Branch line has no range, and the prep report shares its work commit `7d43d7f` (plugin reporting.md: "in its own commit").

### `feature/release-3.0.0-prep` — minor (fix in the same pass)
- **CLAUDE.md:7 against .planning/phases/14-release-readiness/14-RELEASE-PLAN.md:311-312, docs/release-checklist.md:60 and docs/tech-design.md:1037 (§9.5)** — The branch moves the Build command onto `develop` before the cut. All three texts still say the line changes on the release branch. The branch edits the release plan and the checklist but leaves these sentences, and the reason is only in `release-manager-docs-pass.md`, "Outputs".
  - Fix: a dated note in both of those files.
  - Tech-design §9.5 needs one sentence from the tech-lead.
  - **AGENTS.md:7** still reads `Build command: ./gradlew :app:assembleRelease`, against CLAUDE.md's new line.
- **.planning/release/3.0.0/READINESS.md** — the file contradicts itself after the docs pass:
  - :25 says 1.7's text "is false — T-2". T-2 is done.
  - :37 and :47 still list 2.6/T-4 as owed, with "Build command is `./gradlew :app:assembleRelease`". :48-49 still say 4.7 "needs T-5's wording" and 4.8's command "needs T-6/T-11".
  - :83 says "None is done yet … 1.7 still says the script does not exist", above a table that marks T-2 to T-11 done.
  - :92 lands T-7 in an "upload report template" that does not exist. T-7 is in the checklist's §4 note (`docs/release-checklist.md:130-132`).
- **READINESS.md:18 and release-manager-docs-pass.md "Summary"** — "row DT recorded by the producer as DEFERRED to 3.0.1, owner tech-lead". No committed file records that decision: `.planning/reports/STATUS.md:62` still reads "row DT, optional pre-cut". Tech-design row DT (`docs/tech-design.md:1742` on the tds branch) says "else §14.2", not 3.0.1, and the tds branch's §14.3 "one list" of 3.0.1 items has no DT. The producer's STATUS line, or the tech-lead's DT cell, should carry the deferral that READINESS cites.
- **.planning/release/3.0.0/CHANGELOG-draft.md:30-31** — "The game timer is read once, in whole minutes, by TalkBack, instead of being re-spoken every few seconds".
  - The re-speaking (B-2) was found on `bugfix/home-chip-arrival`'s own build and never shipped. Before that branch, the timer's description was "Elapsed time" alone, with no value (`docs/ux-spec.md` 18f: "Today's code fails it: the description is 'Elapsed time' alone"; tech-design §16 #15).
  - The player-facing change is that TalkBack now says the elapsed time, in whole minutes. Checklist 2.5 maps the release notes to this bullet.
- **READINESS.md:69 (D-1)** — "on the release APK". The Build command now makes an AAB, and `agents/qa-engineer.md` step 7 installs `<apk>` from the Build command's output. The release device pass needs a stated install route from the AAB (`bundletool build-apks` / `install-apks`) or a separate APK build. Name it before the pass.
- **Commits `7d43d7f` and `d0e2860`** carry `Co-Authored-By: Claude Opus 5.5` and a `Claude-Session:` line. rules/gitflow.md asks for exactly one trailer, the studio's, and no `Claude-Session:` line. Non-blocking; the producer decides whether to reword before the merge.

### `feature/release-3.0.0-prep` — information only
- `docs/release-checklist.md` §4 note, "it uploads … `app-release.aab` of the checkout it runs from": the script `cd`s to the repository that holds it (`scripts/play-upload.sh:63`), whatever the caller's directory. Under T-11's `./scripts/play-upload.sh` from the root, the two are the same.
- "Every refusal exits 78 before anything is sent" is true of every check, all through `fail` (`:45-48`). A failed `cd` at `:63` exits 1 under `set -e`, but it is not a refusal and sends nothing.
- `14-RELEASE-PLAN.md:278`: item 2's earlier dated correction ("*Corrected 2026-10-21 …*") is replaced by a one-line "earlier said" pointer, not kept. The release plan has no append-only rule, so this is not a finding.
- The checklist's §4 note and 1.7 replace the old "does not exist" texts outright. The tech-lead's T-2 asked for that rewrite, and the checklist is a living list with no append-only rule.

### `feature/ux-3.0.0-folds` — must fix
- **.planning/reports/release-3.0.0/ux-designer-folds.md:1-8** — no `Ran on:` line, so the report is invalid (rules/reporting.md, as above). Its Open issue 5 says "written on Opus (Q-019)", so the line can be written from that.

### `feature/ux-3.0.0-folds` — minor
- **Commit `b4fe627`**: the same trailer deviation as the release-manager's (`Co-Authored-By: Claude Opus 5.5` and `Claude-Session:`).

The spec content itself is accurate and insertion-only:
- the frontmatter `revised:` lines are appended;
- the `WindowThemeTest` row and CV-AC11 gain dated notes;
- no earlier sentence is removed (`git diff develop...b4fe627`: 4 changed lines, each the old text plus an appended note).

Verified in the code at `d251c3b`:
- the strings are `elapsed_time_spoken`, `duration_minutes` (plurals) and `duration_under_a_minute`, and `duration_seconds` and `duration_minutes_seconds` are absent;
- the `values-v35/` and `values-night-v35/` pair each restate `Theme.PegSolitaire` on `android:Theme.Material.Light.NoActionBar` with exactly three items, transparent `navigationBarColor` included, and no v27 directory remains;
- `WindowThemeTest.kt:64` and `:72` are the two below-35 cases, and `WindowThemeApi35FilesTest.kt` exists;
- `clearAndSetSemantics` is on the clock in `TimerPill` (`GameStatusStrip.kt:227`, `:251`);
- 18g's (a)–(c) match `home-chip-arrival/qa-engineer-device2.md:15`;
- SN-1 matches `colour-vision/qa-engineer.md:62-70` and the code: `GameViewModel.kt:967` emits the finish only on `Won`, `:1701` sets Won at `pegCount <= minimumPegs`, and `ScoreCalculator.stars` gives 3 at `<= minimumPegs`.

### `feature/tech-design-3.0.0-sync` — minor (non-blocking)
- **docs/tech-design.md:1909 (§14.3 item 3) and :1986 (§16 #34)**: "`BoardCanvas.kt:583`" is the line on `1bc442f` and `bc95b2a`. On `develop` `d251c3b`, the tree the header says the sync is against, `drawSelectedPeg` is at `:592` and its per-frame `Stroke` at `:617`. Correct it at the next sync, or name the tree.
- **docs/tech-design.md:1123 (§9.5)** still reads "The Build command's move … is a release-branch edit". This conflicts with release-prep's CLAUDE.md change. Not this branch's error, but the tech-lead owns the sentence: a dated note when release-prep merges.
- **.planning/reports/release-3.0.0/tech-lead-tech-design-sync.md:5**: "Ran on: claude-opus-5-5 (the harness names no effort)" is not the `<model> at effort <effort>` form. The line exists, so the report is valid.

Verified in the code and reports:
- **Insertion-only.** A word-level `difflib` pass over all 24 rewritten lines finds no deleted words except two in-line replacements, both kept in dated notes: the v27 → v35 start-path pair in §10, and the owner cells "closed *(until 2026-11-01: …)*".
- **Merges.**
  - The merge commits and their order are as stated: `305f741`, `1bc442f`, `bc95b2a` and `d251c3b`.
  - `1dd1080` is dated 2026-10-31, and its one merge-in `7c2a885` brought `STATUS.md` only.
  - The talkback merge-ins `b84853e`, `a980033` and `2fe6568`, the licences commits `7f03051` and `65a9d4e`, and home's `6cbf333` and `5708d95` are all on `develop`.
  - `d251c3b`'s code equals `2fe6568`'s outside `.planning/` and `docs/`.
- **Counts on `d251c3b`.**
  - The v1/v2 rule importers are 54 and 11: nine in `ui/licenses/`, plus `LicensesNavFlowTest` and `GameScreenHintNoticeTest`.
  - `ANIMATOR_DURATION_SCALE` in `app/src/test` hits only `SystemMotionScale.kt` and `AnimationsEnabledGateWithoutScaleTest.kt`.
  - `rememberAnimationsEnabled()` has 15 calls in 10 files.
  - `HomeBandColumn.kt:273` is `LaunchedEffect(target, animationsEnabled)`, and `HomeScreen.kt:399` is the new gate read.
- **15e sittings.**
  - F-1: 25.5 ms, floor 18.5 ms, arms `f7dd1fe`/`5708d95` (`qa-engineer-sitting2.md:14`).
  - The earlier F-1 sitting: 16.0/6.0 ms, `6aa7624`/`7448713`.
  - colour-vision: −4.0 ms, floor 4.0 ms, `305f741`/`155045e`, attempt 2 on the 05:06 boot (`colour-vision/qa-engineer-15e.md:14-15`).
- **Row PU.** Row PU's as-built list matches `scripts/play-upload.sh` at `d251c3b`.

## Cross-branch consistency
- **Build command:** release-prep moves it pre-cut. The tds branch's §9.5 and release-prep's own release-plan item 5 and checklist §2 say it moves on the release branch (findings above).
- **DT:** READINESS says it is deferred to 3.0.1. Tech-design row DT says "else §14.2", and §14.3 omits it (finding above).
- **E-1:** the tds branch's §15 and §16 #36 name `/usr/bin/env` forms as passing the gate. Release-prep's T-11 leaves them allowed (must-fix above).
- **Release device pass:** the tds branch moves §16 #30's notice half, and the banner's actions-menu Dismiss, to the release device pass (tech-lead report, Open issue 5). READINESS §3's D-list (:65-80) does not have it yet. The producer adds it (the tech-lead asked); no branch is wrong.
- **D-05f, D-05e and SN-1:** the ux folds and the tds branch's §16 #18, §10, §17 and row F-1 agree on the v35 pair, the deleted v27 pair, "below API 35" and the floored whole minutes.

## The Build command change, answered
- **Does it exist?**
  - `:app:bundleRelease` is AGP's lifecycle task for the release variant.
  - `app/build.gradle.kts:262` already hooks it beside `assembleRelease`, so the signing and ID gates run on it.
  - `build-logic/.../ReleaseInvocation.kt:47` lists it as a release invocation.
  - It writes `app/build/outputs/bundle/release/app-release.aab`, AGP's default path, which is the script's `AAB` (`scripts/play-upload.sh:41`) and checklist 4.3's path.
  - Not executed: the dispatch forbids Gradle.
- **Who reads the line?**
  - No plugin hook reads it (`grep -rn 'Build command' hooks/`: no hit).
  - `scripts/gsd-config.sh:34` and `:137` copy it into gsd's `workflow.build_command`, but only when that script is re-run. This product's `.planning/config.json` holds no `build_command`, so nothing stale.
  - `scripts/check-product.sh:31` checks only that the line exists.
  - Agents read it: `agents/release-manager.md` step 7 expects a signed AAB, which now matches. `agents/qa-engineer.md` step 7 installs `<apk>`, which no longer matches (minor finding above).
  - The rules/stacks.md contract ("an installable or deployable artifact") is met by an AAB.

## Outputs
- `.planning/reports/release-3.0.0/code-auditor-docs.md` — this report, committed alone on `feature/release-3.0.0-prep`.

## Checks
- Check command (`./scripts/check.sh`): **not run.** The dispatch forbids Gradle while a performance measurement needs a quiet host. The diffs are docs-only (`git diff --stat develop...<branch>`: no file under `app/`, `domain/`, `build-logic/`, `scripts/` or `gradle/`), so no code result depends on it.
- `git merge-tree --write-tree --name-only` on each pair — pass, all three exit 0: ux×tds `4329c84`, ux×rel `c83cc38`, tds×rel `5625c3c`. The changed-file sets are disjoint.
- `git merge-base develop <branch>` — `d251c3b` for all three.
- `git log -1 --format='%h %s | parents %p'` and `git merge-base --is-ancestor` for every cited commit — pass (listed in the findings).
- `cat -n scripts/play-upload.sh` on `develop` (131 lines) — read in full against the §4 note, row PU, the T-items and the store README.
- `sed -n 25,60p` and `grep -n build_cmd` on `scripts/gsd-config.sh`, plus `grep -rn 'Build command'` over the plugin's `hooks scripts skills agents rules` — as stated above.
- `git grep` on `d251c3b` for:
  - the v1/v2 test-rule importers;
  - `ANIMATOR_DURATION_SCALE`;
  - `rememberAnimationsEnabled()`;
  - `drawSelectedPeg`/`Stroke(` on four trees;
  - the strings and themes.
  The results are as quoted.
- `python3 -I deleted_words.py tds.diff` (scratchpad): a word-level pairing of every removed line with its replacement. Results: tds, 2 in-line replacements, both preserved as dated text; rel, the deletions listed above; ux, appends only.
- `git log -1 --format=%B` on the five branch commits — trailers as reported.
- `.planning/log/` was not read.

## Open issues / risks
1. E-1 is still unfixed in plugin 1.0.0: `hooks/lib-cmdpos.sh:62` lists the wrapper words without a path prefix, and the plugin has no `feature/gate-path-wrappers` branch. Until the fix is installed, T-11's wording is the gate's only cover against path-named wrappers, which is why it is a must-fix here.
2. Rule 1 is not met by this review: author and reviewer both ran on Opus under Q-019 (tech-design §15, §16 #38).
3. Nothing here was executed: no Gradle, no script, no fastlane.

## Handoff
Next: producer — route the must-fix items to their authors, then re-dispatch this review on the fixed heads:
- **release-manager** (`feature/release-3.0.0-prep`): T-11 at checklist 1.7 and the §4 note; `Ran on:` in both reports. The minor items in the same pass: the release plan :311-312 and checklist :60 dated notes, AGENTS.md:7 (or route that file to whoever owns it), READINESS's stale lines and T-7's landing, the DT source, CHANGELOG-draft :30-31, and the D-1 install route.
- **ux-designer** (`feature/ux-3.0.0-folds`): add `Ran on:` to its report.
- **tech-design sync:** PASS. It goes to the fresh tech-lead's sign-off. Fold the `BoardCanvas.kt:583` line and a §9.5 dated note into the tech-lead's next sync, or into this branch before the sign-off if the producer prefers.
- **producer**: record the DT deferral in STATUS.md, if that is the decision READINESS cites. Add §16 #30's notice half and the banner Dismiss to the release device pass.

VERDICT (feature/ux-3.0.0-folds): FAIL — 1 must-fix (report validity); spec content PASS
VERDICT (feature/tech-design-3.0.0-sync): PASS — 3 minor, non-blocking
VERDICT (feature/release-3.0.0-prep): FAIL — 2 must-fix (T-11 weaker than the ruling; reports invalid), 7 minor

VERDICT: FAIL
