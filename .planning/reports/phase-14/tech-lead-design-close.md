# tech-lead report — phase-14 release-docs: the tech-design sync owed by the phase-15 close and the haptics ruling, and this branch's merge readiness
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer). Base head `f548e32`; this dispatch's two files are uncommitted in `/Users/ermanergoz/Desktop/PegSolitarie-r5`.
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The product's snapshot is 0.5.1 (`.claude/rules/VERSION`) and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose, stacks, testing and dispatch. puzzle-design and play-store are byte-identical in both (`diff -q`), so I read them in full from the snapshot. Also the product's `.claude/rules/lessons.md`. Then android-architecture and android-gradle-logic, **after** the edits rather than before (my error; neither changes any edit below).
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort; the harness names none). **Staffing deviation:** the tech-lead is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17, `.planning/questions/FOUNDER.md`: "Opus for every Fable-pinned role until 3.0.0 is published").
Fan-out: none
Loops: 0
Read in full: `.planning/reports/15-founder-feedback/tech-lead-signoff-close.md`, `tech-lead-signoff.md` and `qa-engineer-15e.md` (on `develop`); `.planning/reports/audio-review/tech-lead-haptics.md` (on `feature/audio-review`); `.planning/reports/phase-14/research-analyst-font-licences.md` (this branch)
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

VERDICT: NOT READY — one precondition of Decision 4 row 3 is open, a two-row `THIRD_PARTY.md` deletion owned by the release-manager (below). The verdict becomes READY without a new tech-lead dispatch once that check reads 0.

## Summary
- **Every owed edit is applied** to `docs/tech-design.md`: the phase-15 close's §10, §14.1, §16 and §17 text, and the haptics ruling's four edits. `866dffb` does carry the earlier sign-off's text; 13 greps find each item once.
- **Seven consequential edits** in the same file, outside both owed lists, are listed separately (part D) for the reviewer.
- **Merge mechanics are clean.** The branch merges into `develop` without a conflict, no Check input reads any file it changes, the OFL-1.1 verdict line is recorded, and the §2, §7 and §8 font text matches `tech-lead-fonts.md`.
- **Not met:** `THIRD_PARTY.md` §3 still has the `font_certs.xml` row and §4 the `ui-text-google-fonts` row. `tech-lead-fonts.md` deletes both, and each row's own "keep while `develop` still carries it" condition lapsed when phase 15 merged at `c9228a9`.

## A. Edits owed by the phase-15 close (`tech-lead-signoff-close.md`, "Docs edits still owed")
| Where | Edit |
|---|---|
| §10, 15e row, Status | After "Runs once on phase 15 before its merge (H-3)": "Sat 2026-10-18 on `Pixel_9a_bench`: **PASS**, delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B `ee16a2a` (`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`)". Each figure was checked against the evidence file's "sitting of record" (verdict line, Builds table) and against `qa-engineer-15e.md` |
| §10, 15e row, Value | The instrument note and the ART note, with the close's wording |
| §10, 15e row, Decided in | `15-founder-feedback/tech-lead-signoff-close.md` D-1 added |
| §14.1, Phase 15 | "Depends on" now opens with "**Done.** Signed off PASS 2026-10-21 (…): H-1 `code-auditor-loop4.md` + `code-auditor-loop5.md`; H-3 `qa-engineer-15e.md`; H-4 recorded in STATUS 2026-10-18". It then records the merge: `c9228a9`, `--no-ff` (parents `c3d3437` and `ea1332b`), with no merge-in needed. The HOLD wording stays as the record, in the past tense. H-4's date was checked: STATUS recorded "H-4 is done" in `2e7094e` on 2026-10-18 |
| §14.1, F-1 | Scope widened with the close's INFO rows 1, 3, 4 and 5: the two test-only hardenings (M3d, M5c), the stale KDoc lines, and `HomeStageTest`'s class KDoc. Row 4's comment edits are in `ui/home/`, a start-path directory. The close leaves the sitting question to that branch's QA, and the row says so |
| §16 #18 | 15e O-1, the dark three-button scrim (the close's INFO row 9): not a defect; the ux-designer rules; the mechanism if it is wanted (`windowLightNavigationBar` in `values-v27/` and `values-night-v27/`, a `WindowThemeTest` case); the sitting condition; the deadline |
| §16 #19 | The Fable re-audit question (the close's INFO row 10): not a merge condition; producer → the 3.0.0 retro |
| §17 | The new row: P-1.1's `avdmanager` route superseded by "`avdmanager` or a copy, closed by the P-1.2 diff" |

**Numbering.** The dispatch's "row 9" and "row 10" are rows of the close's INFO table. §16 already ran to #17, so the new rows are §16 **#18** and **#19**.

## B. Edits owed by the haptics ruling (`feature/audio-review`, `.planning/reports/audio-review/tech-lead-haptics.md`, committed `c77e4b6`, "Text for `docs/tech-design.md`", lines 231–238)
| Where | Edit |
|---|---|
| §16 #17 | Status: "**Decided 2026-10-18** (…)", verbatim, appended after the original "Open, owed by the tech-lead …", which is kept as the record. Owner: android-game-dev via the producer, in the audio brief |
| §12 | The new "Haptics" paragraph, verbatim, headed with its source and §16 #17 |
| §14.1, audio row | "The wrong-tap half also waits on the tech-lead's haptics ruling (§16 #17)" is no longer live text. **In house style it is kept as an italic record** ("Until 2026-10-18 this row also read …; that ruling is made"), not deleted as the ruling's text says |
| §14.1, candy row | "four files" becomes "five files", adding `di/DataModule.kt`, where each branch adds its own `@Binds` block (a mechanical keep-both). It also notes that the count was four before the ruling |

## C. The earlier sign-off's tech-design text (`tech-lead-signoff.md`, "Text for other roles' files"): present since `866dffb`, nothing missing
- §10: `Pixel_9a_bench` is the instrument of record; arm A is the merge-base; the row says "Runs once on phase 15 before its merge (H-3)".
- §14.1: R-2's "R-1 **and phase 15** merged; cut from `develop` after both"; the Phase 15 row's H-1 to H-4; the F-1 row.
- §15: "Disjoint regions; phase 15 merges first; R-2 is cut after it".
- §16: #13 F-1, #14 warnings as errors, #15 the timer's TalkBack form.
- §17: the four rows (the instrument, arm A, AC-F7's airplane run, the merge order).

## D. Consequential edits: mine, in neither owed list (flagged for whoever reads this diff)
1. **Header:** one provenance sentence for this sync.
2. **§10 start-path list:** `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml` are added, "if §16 #18's change lands". This transcribes the close's row 9 ("Both files are start-path files"), so a later sitting does not miss them.
3. **§14.1 R-6:** "synced … 2026-10-21 (`tech-lead-design-close.md`)".
4. **§14.1 merge order:** release-plan merged 2026-10-21 (`0582e3f`), and phase 15 followed on 2026-10-21 (`c9228a9`).
5. **§15, the 15e risk row:** the outcome is noted (33.0 ms, the fonts' share never reopened). The row stays for candy's sitting, and F-1's if it carries one.
6. **§16 #11:** a pointer to the close's ux-spec folds b–e, and the finding that fold a (2026-10-18) is still unapplied.
7. **§13 and a new §16 #20:** `research-analyst-font-licences.md` (on this branch, open issues 1–3) found notice duties that neither `THIRD_PARTY.md` nor the Licenses spec covers: BSD-3, MIT and MPL-2.0 families, the Jakarta NOTICE, and the Google SDKs' own notice files. That contradicts §13's "nothing shipped carries an unmet attribution obligation", and this file wins over reports, so I marked §13's sentence "under review, §16 #20". #20 records the owners. **Its deadline is a tech-lead decision made here:** with the Licenses screen, before `release/3.0.0` is cut, because BSD-3 and MIT make the notice a condition of redistribution. The Google-notice ruling itself is **not** made here. It is owed by me on dispatch.

## Merge readiness: Decision 4 row 3 of `tech-lead-signoff.md`
| Precondition (row 3) | State | Evidence |
|---|---|---|
| Merges after phase 15 | **met** | `c9228a9`, a `--no-ff` merge (parents `c3d3437`, `ea1332b`), 2026-10-21. `develop` is now `0582e3f` (release-plan) |
| `THIRD_PARTY.md` §2: the opening sentence and the two font rows carry `tech-lead-fonts.md`'s text | **met** | The opening sentence names "five font files" in `app/src/main/res/font/` (`:84-87`). The rows at `:140-141` match the ruling's text: same items, sources, sha256 prefixes, upstreams, copyright lines and licence, and How verified and Credit carry more evidence. The release-manager recorded that re-check at `:93-96` |
| §3: "delete the `font_certs.xml` row" | **NOT met** | The row stands at `:167`, kept "only while `develop` still carries the file". `develop` no longer carries it: `git ls-tree -r develop -- app/src/main/res/values/` finds no font file |
| §4: "delete the `androidx.compose.ui:ui-text-google-fonts` row" | **NOT met** | The row stands at `:180`, "listed while `develop` still declares it". `develop` declares it nowhere: `git grep` over `gradle/libs.versions.toml` and `app/build.gradle.kts` finds nothing |
| §7: the "unmet attribution obligation" wording | **met** | `:326-328` |
| §8: nothing to add unless T5's fixture survived | **met** | `develop` has no `app/src/test/resources/**/font*` and no `ShippedFace` |
| The research-analyst's OFL-1.1 verdict line | **met** | `THIRD_PARTY.md:97`, "OFL-1.1 verdict (research-analyst, 2026-10-18): CLEAN …", with the report `research-analyst-font-licences.md` (`eadf23c`, `0afd68f`). That report is valid: it has a Checks section and the `Ran on:` and `Lessons applied:` lines |
| Audio and Apache-2.0 rows (the release-manager's to place; not a precondition) | placed | the §2 audio rows (`:128-139`) and the Apache-2.0.txt row (`:142`) |
| `docs/tech-design.md` with the sign-off's supersessions | **met** | `866dffb` (part C), plus this dispatch's edits once the producer commits them |

**The same owner, in the same edit (accuracy, not a separate blocker):** `THIRD_PARTY.md:12-18`'s table of changes "still on unmerged branches" still lists the font change, with "What `develop` (3f2af90) still holds: the downloadable-font path, nothing bundled". `:93` ("Font rows: on `feature/phase-15-ux-refresh` today") and `:105` ("files on `feature/phase-15-ux-refresh`") are stale in the same way since `c9228a9`.

**Mechanics:**
- `git merge-tree --write-tree develop HEAD`: clean (exit 0).
- No file has changed on both sides since the merge-base `8dc9f09`.
- No Check input reads a file this branch changes. The branch changes no code, so `develop`'s Check result is the merged tree's. The Check was not run: the dispatch forbids Gradle.

**Review:** Decision 4 row 3 sets this branch's merge preconditions and names no code-auditor or QA step. The release-plan branch, docs only, merged on the same footing (`0582e3f`). rules/gitflow.md (1.0.0) does not name `feature/phase-14-release-docs` among the pipeline branches, so how the branch merges is the producer's call, as it was for release-plan. **My own tech-design edits are reviewed by no other role in this step, and this report does not approve them (standing rule 1).** If the producer wants an independent read before the merge, the route is a code-auditor reading the diff as text.

## Outputs
- `docs/tech-design.md`: parts A, B and D above (`git diff --stat`: 1 file, 31 insertions, 13 deletions). Uncommitted.
- `.planning/reports/phase-14/tech-lead-design-close.md`: this report. Uncommitted.

## Checks
- `git -C PegSolitarie-r5 status` at the start: on `feature/phase-14-release-docs`, clean, head `f548e32`. `git diff --name-only` at the end: `docs/tech-design.md` only, plus this new report.
- `git diff --check -- docs/tech-design.md`: pass (exit 0).
- Table structure: `awk` pipe counts on every touched row. §14.1, §15 and §16 rows have 5 pipes (4 columns); the §17 row has 4 (3 columns). The 15e row has 7 both before (`git show HEAD:…`) and after; the 2 extra pipes are the pre-existing `|median(N1) − median(N2)|`. The §16 numbering reads 1 to 20 with no gap.
- Each edit landed: `grep -c -F -- '<text>' docs/tech-design.md` = **1** for each of the following.
  - `Synced again on 2026-10-21`
  - ``Sat 2026-10-18 on `Pixel_9a_bench`: **PASS**, delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B `ee16a2a` ``
  - `*Instrument note (2026-10-21, …D-1):*`
  - `The ART precondition holds when both arms read the same status`
  - ``…tech-lead-signoff-close.md` D-1 |`` (Decided in)
  - ``…values-night-v27/themes.xml` too if §16 #18``
  - `**Haptics** (tech-lead decision 2026-10-18`
  - `that sentence is under` (§13)
  - `Signed off PASS 2026-10-21 (…): H-1 … H-3 … H-4 recorded in STATUS 2026-10-18`
  - ``Merged to `develop` 2026-10-21 as `c9228a9` (`git merge --no-ff` ``
  - `**Scope widened 2026-10-21** with rows 1, 3, 4 and 5`
  - `*(Until 2026-10-18 this row also read "The wrong-tap half also waits`
  - `they share five files`
  - R-6 `2026-10-21 (the phase-15 close and the haptics ruling`
  - ``Merged 2026-10-21 (`0582e3f`)``
  - `phase 15 followed on 2026-10-21`
  - `phase 15's sitting passed (delta 33.0 ms`
  - `Added 2026-10-21: folds b–e`
  - ``**Decided 2026-10-18** (`.planning/reports/audio-review/tech-lead-haptics.md`, `feature/audio-review`)``
  - `| 18 | **15e O-1:**`
  - `| 19 | A Fable re-audit`
  - `| 20 | The notice duties`
  - ``| P-1.1 "`avdmanager create avd -n Pixel_9a_bench …`" as the only route``
- The old wording is gone as live text, kept only as the record: `§9.8). The wrong-tap half also waits` gives **0**; `they share four files` gives **0**.
- The earlier sign-off's text (part C): the same grep gives **1** for each of the 13 items, among them ``**`Pixel_9a_bench`** is the instrument of record``, `R-1 **and phase 15** merged; cut from…`, `Disjoint regions; phase 15 merges first; R-2 is cut after it`, `| 13 | F-1`, `| 14 | Kotlin and lint warnings`, `| 15 | How the timer is announced`, and the four §17 rows. `git log -1 866dffb -- docs/tech-design.md`: "sync the tech design with the phase-15 and R-1 sign-offs".
- The cited figures: `sed -n 10,127p .planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` on `develop`. Verdict line "424.0 − 391.0 = **33.0 ms** … `floor` = |438.5 − 435.5| = **3.0 ms**"; Builds: arm A `c3d3437`, arm B `ee16a2a`; dated 2026-10-18. These agree with `qa-engineer-15e.md`.
- `git show -s --format='%h %ad parents=%p %s' c9228a9`: "2026-10-21 18:15:14 … parents=c3d3437 ea1332b Merge branch 'feature/phase-15-ux-refresh' into develop".
- `git log develop -S 'H-4 is done' -- .planning/reports/STATUS.md`: `2e7094e` (2026-10-18) is the first commit to record it.
- `git -C PegSolitarie-audio log --oneline -- .planning/reports/audio-review/tech-lead-haptics.md`: `c77e4b6`, committed on `feature/audio-review`.
- `git grep -l windowLightStatusBar develop -- app/src/test`: `ui/theme/WindowThemeTest.kt`. `git ls-tree` shows `res/values/` and `res/values-night/themes.xml` only. `minSdk = 24` (`build-logic/…/android-app.gradle.kts:29`). Together these support #18's mechanism text.
- `git merge-tree --write-tree --name-only develop HEAD`: pass (exit 0, tree `4a73299`, no conflicted paths).
- `comm -12` of `git diff --name-only develop...HEAD` against `git diff --name-only 8dc9f09 develop`: empty.
- `git grep -c -E 'THIRD_PARTY|CHANGELOG|tech-design|release-checklist|ad-testing-safety|docs/store' develop -- build-logic app/build.gradle.kts domain/build.gradle.kts build.gradle.kts settings.gradle.kts app/src/test domain/src/test scripts .github`: 0.
- `git ls-tree -r --name-only develop -- app/src/main/res/values/ | grep -i font`: none. `git grep 'google-fonts\|googlefonts\|ui.text.google' develop -- gradle/libs.versions.toml app/build.gradle.kts`: none. `git ls-tree … app/src/test/resources/ | grep -i font`: none, and `ShippedFace` is absent. Listing `res/font` and `assets/licenses` on `develop` shows the five fonts and both OFL texts.
- `awk '/^## 3\./,/^## 5\./' THIRD_PARTY.md | grep -c -E 'font_certs|ui-text-google-fonts'`: **2**, the open precondition. It must read **0**.
- `./gradlew check`: **not run**. The dispatch forbids Gradle, and this branch changes no code.
- I did not read `.planning/log/`.

## Open issues / risks
1. **Staffing deviation (Q-019).** Opus, not Fable. This dispatch transcribed recorded rulings and made one small deadline decision (#20). No code was judged.
2. **The required skills were loaded late** (after the edits). Neither changes anything here; the slip is recorded.
3. **Still owed to this file, not in this dispatch:** the candy direction-B addendum, `.planning/reports/candy-backdrop/tech-lead-direction-b.md` on `feature/candy-backdrop` (its open issue 6 and handoff 7). It closes §16 #16 and carries the split into `feature/licences-screen` and `feature/colour-vision` that STATUS's In flight item 5 already schedules. §14.1's candy row and merge order do not show that split yet. Schedule it before the first of those branches is dispatched.
4. **Owed rulings by the tech-lead:** the Google SDK notices (§16 #20; the research-analyst's open issue 2). The close's row 6 (244 `w:` lines) needs no new ruling: §9.8 ruling 1 already puts the `ExperimentalCoroutinesApi` opt-in on `:app`'s unit-test compilations (C7). §9.8's 239 is the count at `e1875cb`, which loop 4 re-measured as 244 after H-2 and the R-1 merge-in.
5. **The haptics ruling touches a start-path directory.** Its `@Binds` goes in `di/DataModule.kt`, and `di/` is on §10's list. No recorded sitting is affected: phase 15 has merged, and candy sits last on a tree that already holds audio. **But if F-1 carries a 15e sitting and then takes `develop` in after audio has merged, that merge-in changes `di/`, and per §10 the sitting comes back to the tech-lead.** Tell F-1's QA and the producer.
6. The research-analyst's open issue 1 also makes `THIRD_PARTY.md` §5b's "no licence risk hides in the gap" and §7's "every dependency is Apache-2.0 or a permitted Google SDK" incomplete about notice duties. That is the release-manager's wording, recorded in §16 #20, and not a row-3 precondition.
7. `PegSolitarie-dev` first showed `develop` at `7989df7`, and it now reads `0582e3f`. Both are release-plan merges with the same parents and the same tree (`2d0d9af…`), and `7989df7` is on no branch. Nothing to do.

## Handoff
Next: release-manager (via the producer) — on `feature/phase-14-release-docs`:
1. Delete the §3 `font_certs.xml` row and the §4 `ui-text-google-fonts` row, per `tech-lead-fonts.md` "THIRD_PARTY.md — the text".
2. In the same commit, refresh the stale "unmerged branch" pointers for the fonts (`:12-18`, `:93`, `:105`) now that `develop` holds phase 15.
3. Report with `awk '/^## 3\./,/^## 5\./' THIRD_PARTY.md | grep -c -E 'font_certs|ui-text-google-fonts'` reading 0.

Next: producer —
1. Commit `docs/tech-design.md` and this report on the branch, as `docs(14): …`.
2. When the release-manager's check reads 0, the branch is **READY** without a new tech-lead dispatch. Merge it with `git merge --no-ff`; `merge-tree` is clean today.
3. Pass §14.1's widened F-1 scope to the `bugfix/home-chip-arrival` dispatch, and open issue 5 to F-1's QA.
4. Schedule the direction-B tech-design sync (open issue 3) and the Google-notice ruling (§16 #20) for the tech-lead.

VERDICT: NOT READY — `THIRD_PARTY.md` §3 `font_certs.xml` row and §4 `ui-text-google-fonts` row still stand; `tech-lead-fonts.md` deletes both, and each row's keep-while-develop-holds-it condition lapsed at `c9228a9` (owner: release-manager)
