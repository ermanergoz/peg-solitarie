# tech-lead report — phase-14 R-1 gate scope: sign-off before the merge into develop
Date: 2026-10-18
Branch: feature/phase-14-gate-scope (committed by the producer). Reviewed: `develop...c944496` (merge-base `8dc9f09`; code in `f2b5de9`, `86fc4a4`, `fb838f8`). `eb37300` adds the QA report and is a record only.
Skills loaded: ergoz-soft:standards (role tech-lead). This worktree's snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. Also the product's `.claude/rules/lessons.md` (here and on `develop`, v0.6.2). Then android-architecture, android-gradle-logic.
Ran in parallel with: phase-15 H-2 developer fix (`PegSolitarie-p15`), candy-backdrop design directions
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort pin). **Staffing deviation:** the tech-lead is pinned to fable. This sign-off ran on Opus under the founder's Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), `PegSolitarie-p15/.planning/questions/FOUNDER.md`.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/phase-14/code-auditor-gate-scope.md`, `qa-engineer-gate-scope.md`, `android-game-dev-gate-scope.md`; `develop:.planning/reports/phase-14/tech-lead-prerelease-signoff.md`, `develop:.planning/reports/phase-14/tech-lead-signoff-r3-signing.md`; `PegSolitarie-r5/.planning/reports/phase-14/tech-lead-r2-gate-brief.md`. Read in part: `PegSolitarie-r5/docs/tech-design.md` §9, §14 to §17; `PegSolitarie-p15/.planning/reports/15-founder-feedback/tech-lead-signoff.md` (header, Summary, Decision 4); Q-019 in `PegSolitarie-p15/.planning/questions/FOUNDER.md`.
Lessons applied: none (both snapshots hold 0 active lessons; `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
- This sign-off ran on Opus, not the pinned Fable, under Q-019. R-1′ does what rulings 1–4 and the F-8 ruling require, and nothing on the branch has to change. The code-auditor passed it with negative controls and mutations. QA passed it after a green Check (11 min 19 s, all `PLAY_UPLOAD_*` unset) plus its own probes.
- A trial merge into current `develop` (`3f2af90`) is clean. `develop` gained two commits after the branch point: a versionCode hunk in a different part of `app/build.gradle.kts`, and the rules snapshot. The tripwire fires in no in-flight worktree, so merging is safe.
- QA question 1 (CI on `release/**`): the plugin's own rule requires it, and design §9.7 is behind that rule. **It does not block this merge.** It goes into check-gates as commit C3b (addendum A1).
- QA question 2 (manifests and assets): yes, and wider. The literal gates will scan every file in a shipped source set. Today `src/main/AndroidManifest.xml` carries a comment saying it is scanned, and that is false. **It does not block this merge.** It goes into check-gates as commit C3a (addendum A2).
- I also found a defect in my own brief: C3's test count was wrong (addendum A0).

## Sign-off — PASS

**Against the rulings.**
- *Rulings 1 and 2, which must never be split:* both are in `f2b5de9`. It contains the predicate (`ReleaseInvocation.kt`, 10 tests), the package-task hook for all three verify tasks, the lifecycle hook kept as the backstop, and the `validateSigningRelease` ordering. It also corrects the comment that ruling 1 made false (`standards.gradle.kts`, the "fourth reason … now FALSE" note). Moving the predicate into `build-logic` is a faithful rendering of the ruling, and better than the inline snippet. It follows the `AdIdResolution` and `SigningResolution` precedent: `:app` has no `testReleaseUnitTest` under AGP 9.1.0, so a predicate there could never be tested.
- *F-8 and the tripwire:* `86fc4a4`. The roots are main, release and debug, crossed with java, kotlin and res. Test source sets are excluded, and the failure messages say so with a sentence derived from the list. The tripwire throws at configuration time on both modules. Design §9.3 matches.
- *R-1′(c), CI runs `check`:* `fb838f8`. It runs `-p build-logic test` first, then `check assembleDebug`, verbatim. My ruling wanted this in the same commit as (b), and it arrived as a separate commit on the same branch. I accept that. The ruling's purpose was that the widening never lands without CI invoking it. `develop`'s first-parent history never sees the split, and the repository has no remote, so no CI run could have seen it either.
- *TDD deviation on `ReleaseInvocation`:* the code-auditor accepted it as proven-equivalent, using its own three mutations, and I concur. It was one-off and declared, and it sets no precedent for `:domain`.

**Boundaries and standards.**
- No dependency coordinate changed, so no licence row is owed.
- The diff adds no `@Suppress`, `@Ignore`, baseline, disabled test or `!!` (auditor and developer greps; the diff I read in full agrees).
- `:domain`, the app sources and the resources are untouched.
- The two new objects are Gradle-free, and both are pinned by tests (54 build-logic tests, 0 skipped, run by the auditor and by QA).

**What I accept without a repetition, and why.**
- Ruling 2's positive control was never run. That is a correct configuration producing a signed artifact that `apksigner` verifies. The failure it would catch is fail-closed: a broken positive path cannot ship a wrong artifact, it just fails to produce one. The risk it covers is already discharged in three ways:
  - The dry runs build an acyclic graph with the verify tasks first.
  - QA's `:app:aR` run executed `verifyReleaseAdIds` and `verifyReleaseGamesIds` green on real IDs.
  - The signing code is untouched. It was proven with a throwaway key on the signing-config branch.
- The release-manager's signed build on `release/3.0.0` (`14-RELEASE-PLAN.md` §4b) is the positive control of record. If it fails there, that comes to me.

**Merge preconditions (the producer's, not a tech-lead hold):**
1. The Check must be green on the merged tree. The trial merge is clean. Run the Check on `develop` right after the `--no-ff` merge, before check-gates is cut from it. If phase 15 merges first, take `develop` in and re-run the Check before this merge (Decision 4, row 2).
2. This report is committed with the merge.

## Ruling 1 — CI on `release/**` pull requests

**Decision.** `ci.yml` triggers on pushes and pull requests to `develop`, `release/**` and `main`.

**Why.**
- Plugin 1.0.0's `rules/stacks.md` scaffold contract item 8 requires "a CI workflow running the check command on every PR to develop, release/* and main". Design §9.7's "develop and main" predates that line (`develop`'s 0.6.2 snapshot does not carry it), so the design is wrong, not the brief.
- I add push as well as pull request because the studio merges locally with `git merge --no-ff`. Once a remote exists, a pushed branch is the only event GitHub will ever see.
- After C5, a CI run on a release branch is also the release fuzz tier. CI pins `FUZZ_SEEDS=10000`, and 10,000 is the whole level space (§7.3).

**Does not block this merge. It goes into check-gates.**
- The triggers are pre-existing: `fb838f8` changed the steps, not the triggers.
- The repository has no git remote (`git remote` is empty in this worktree and in the main checkout), so the workflow has never run anywhere.
- No `release/*` branch exists. Check-gates already edits `ci.yml` (C5's env), and it merges before `release/3.0.0` is cut (Decision 4). So no pull request into a release branch can predate the fix.

## Ruling 2 — manifests and assets in the literal gates

**Decision.** Yes, and the shape is wider than QA asked. The three ID gates scan **every file under** `src/main`, `src/release` and `src/debug`. That replaces the per-directory list (java, kotlin, res).

**Why.**
- *The subdirectory list has a hole at the most likely paste site.* `src/main/AndroidManifest.xml` is where AdMob's setup steps put the app ID as a literal `android:value`. Its own comment at the APPLICATION_ID tag (`AndroidManifest.xml:50-54`) says an inline ID "would also fail the adIdLiteralCheck gate, which scans all of src/main". That claim is false today: the roots stop at java, kotlin and res, and QA's probe shows a manifest literal passing. This is F-1's class of defect, a gate whose description overstates its scope.
- *More shipped content is unscanned.* Phase 15 adds `app/src/main/assets/licenses/`. `:domain` already ships `src/main/resources/golden/book/*.txt` as Java resources. Neither is scanned.
- *Scanning the whole source set needs no second list.* AGP has a dozen content types per source set. Scanning the whole set makes the source-set list the only hand-maintained list, and the tripwire already guards it.
- *It is cheap and green today.* The gate bodies already walk each root and read every file, binaries included: `res/raw` is scanned now. So the change is one function and its description. I grepped all four markers across every file under the shipped source sets of this branch, phase 15, audio and candy, binaries included: 0 files in each. The widened gates are green on every tree that will merge.

**Does not block this merge. It goes into check-gates.**
- R-1 does exactly the ruled scope and only widens it. The manifest was unscanned on `develop` too.
- Today both manifest tags read `@string/…`.
- Check-gates merges before the release cut, so the hole closes before anything ships.
- Holding a branch that has had three reviews, for a change in the file family C3 already edits, buys nothing.
- With A2 the manifest comment becomes true, so `AndroidManifest.xml` (a phase-15 start-path file) is not edited.

F-9's durable shape is unchanged: deriving the roots from `android.sourceSets` still waits for a flavour or a third build type.

## Addendum to `tech-lead-r2-gate-brief.md` for the android-game-dev (check-gates)

Pass this section to the developer with the brief. Commit order becomes C1, C2, C3, **C3a**, **C3b**, C4, C5, C6. No new file joins §9's list.

**A0 — correction to §4 (C3), the brief's own defect.**
- `theAgpCreatedTestSourceSetsAreKnownSoTheyDoNotFalselyTripTheWire` pins the very pre-authorisation C3 removes, so C3's fix turns it red. Delete it in C3.
- Its successor is `aTestDimensionNobodyHasClassifiedTripsTheWire`. It may list all six removed names: `androidTest`, `androidTestDebug`, `androidTestRelease`, `screenshotTest`, `screenshotTestDebug`, `testRelease`.
- C3's acceptance count is therefore **54, not 55**. A2 brings it to 55, so §9's final "55" stands.

**A2 — commit C3a, `fix(14): the literal gates scan every file in a shipped source set`.**
- *Files:* `SourceSetPolicy.kt`, `SourceSetPolicyTest.kt`, and the comments in `standards.gradle.kts`.
- *Change:*
  - `shippedSourceRoots()` returns the source-set directories themselves: `src/main`, `src/release`, `src/debug`.
  - Delete `SHIPPED_LANGUAGE_DIRS`.
  - `SHIPPED_SOURCE_ROOTS_DESCRIPTION` becomes "every file under src/main, src/release, src/debug".
  - The three gate bodies stay as they are; they already walk each existing root and read every file.
  - Rewrite the `SourceSetPolicy` KDoc and the three gate comments that say "(java, kotlin, res)". The reason they record: the manifest is where AdMob's setup puts the app ID, `assets/` and Java `resources/` ship too, and the source-set list is now the only list.
  - Do **not** edit `AndroidManifest.xml`. Its comment becomes true with this change.
- *Test first:*
  - Add `everyFileOfAShippedSourceSetIsUnderAScannedRoot`. It asserts that each of these starts with some `shippedSourceRoots()` entry plus `/`: `src/main/AndroidManifest.xml`, `src/main/assets/licenses/x.txt`, `src/main/resources/golden/book/x.txt`, `src/debug/AndroidManifest.xml`. Watch it fail on C3's tree.
  - Replace `everyShippedSourceSetIsScannedInAllThreeLanguageDirectories` with the three-directory list.
  - Keep `noTestSourceSetIsScannedByTheLiteralGates`.
  - Update `theGateFailureMessagesCanNameTheRootsTheyActuallyScanned`.
- *Acceptance (R-1's bar, negative control included):*
  1. Plant a non-comment line carrying a fake `ca-app-pub-…`, a fake `CgkI…` and `requestAudioFocus` in three places:
     - a new `app/src/debug/AndroidManifest.xml`, as a `<meta-data android:value=…>`;
     - `app/src/main/assets/probe.txt`;
     - `domain/src/main/resources/probe.txt`.
  2. Each of the three gates fails on `:app` or `:domain`, naming the file.
  3. Negative control: the same line in `app/src/test/resources/probe.txt` and in `domain/src/test/resources/probe.txt` passes.
  4. Delete every probe. `git status --porcelain` must be empty.
  5. The gates are green on the real tree.
  6. `./gradlew -p build-logic test --rerun-tasks` gives 55 tests and 0 failures.

**A1 — commit C3b, `ci(14): run CI on pushes and PRs to release branches`.**
- *File:* `.github/workflows/ci.yml` only.
- *Change:*
  - Set both `on.push.branches` and `on.pull_request.branches` to `[develop, 'release/**', main]`. Quote the entry; `**` so that `release/3.0.0` matches.
  - Rewrite the header comment's coverage sentence to name `bugfix/* → release/*` pull requests and pushes to a release branch.
  - Change nothing else: no job split, no `scripts/check.sh` in CI, no flag changes. C5's `FUZZ_SEEDS` env then covers release-branch runs too.
- *Acceptance:*
  - `python3 -c 'import yaml; d=yaml.safe_load(open(".github/workflows/ci.yml")); print(d[True]["push"]["branches"], d[True]["pull_request"]["branches"])'` prints both lists with `release/**`. (PyYAML reads the `on` key as `True`.)
  - Run `actionlint` if it is installed; otherwise say it is not.
  - State in your report that the repository has no git remote, so no workflow run can be shown as evidence. Do not claim one.

## Text for `docs/tech-design.md` (mine; it lands on `feature/phase-14-release-docs`)

- **§9.3, first sentence becomes:** "The three ID gates read every file under `src/main`, `src/release` and `src/debug` (`SourceSetPolicy.shippedSourceRoots()`; the manifest, `assets/`, Java `resources/` and any later content type included), filtered by `exists()`." Then add: "*Amended 2026-10-18 (`tech-lead-signoff-gate-scope.md`, QA open issue 2):* the per-directory {java, kotlin, res} list had a hole at the manifest, where AdMob's setup puts the app ID and whose comment claimed the gate covered it. The source-set list is now the only list, and the tripwire guards it. Implemented in check-gates C3a."
- **§9.7, "on every push and PR to `develop` and `main`" becomes:** "on every push and PR to `develop`, `release/**` and `main` (rules/stacks.md scaffold contract 8; check-gates C3b). The repository has no git remote today, so the workflow has never run. The local Check is the only gate that has executed, and the 25-minute split trigger waits for the first remote run; a local `check` time is not that figure."
- **§17, two rows:**
  - "`shippedSourceRoots` = (main, release, debug) × {java, kotlin, res}" → "every file under the three shipped source sets" (§9.3).
  - "CI on push and PR to `develop` and `main`" → "`develop`, `release/**`, `main`" (§9.7).
- **§16, item 3:** "Signed off PASS 2026-10-18 (`tech-lead-signoff-gate-scope.md`)". **§14.1, R-1 row:** done once merged. Brief §4's C3 count is corrected by addendum A0.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r1/.planning/reports/phase-14/tech-lead-signoff-gate-scope.md` — this sign-off, with addendum A0–A2 for the check-gates developer and the design text. Uncommitted; the producer commits it.
- No source, test, Gradle, workflow, design or planning file was created or edited by me. No branch switch, no commit.

## Checks
- `git -C PegSolitarie-r1 diff --stat develop...feature/phase-14-gate-scope` — pass (10 files, +866/−61; code in `f2b5de9`, `86fc4a4`, `fb838f8`, the rest are reports)
- `git show --stat f2b5de9` — pass (the predicate, its test and both hooks are in one commit; rulings 1 and 2 are not split)
- The diff read in full: `ci.yml`, `app/build.gradle.kts`, `ReleaseInvocation.kt`, `SourceSetPolicy.kt`, `standards.gradle.kts`, both test classes; the gate bodies at the branch head — pass (matches §9.2.5, §9.2.6, §9.3 and §9.7 as written)
- `git merge-tree --write-tree --name-only develop feature/phase-14-gate-scope` — pass (exit 0, tree `4c2a322`, no conflict). `git diff 8dc9f09 develop -- app/build.gradle.kts`: only `versionCode = 30_000` at :267, disjoint from R-1's hunks at :82 and :240
- `git show feature/phase-14-gate-scope:.github/workflows/ci.yml` — pass (push and pull_request both `[develop, main]`; steps `-p build-logic test`, then `check assembleDebug`). Confirms QA's reading
- `git remote -v`, here and in the main checkout — **0 remotes**: `ci.yml` has never run (basis of ruling 1's non-blocking call and the §9.7 text)
- `grep CI` in plugin `rules/stacks.md` vs `git show develop:.claude/rules/stacks.md` — the plugin's contract item 8 names "develop, release/* and main"; the 0.6.2 snapshot has no such line (§9.7 trails the plugin rule)
- `ls app/src domain/src` in `PegSolitarie-r1`, `-p15`, `-audio`, `-candy`, `-r5`, `-p15-qa` and the main checkout — pass (only `main`, `test`, `debug`, `release`, `testDebug`; the tripwire fires in no in-flight worktree once R-1 merges)
- `grep -rl` for `ca-app-pub-`, `CgkI`, `requestAudioFocus`, `abandonAudioFocusRequest` over `app/src/{main,debug,release}` and `domain/src/main`, in r1, p15, audio and candy (binaries included) — **0 files** in each (A2's widened gates are green on every tree that will merge)
- `sed -n 50,90p app/src/main/AndroidManifest.xml` — both meta-data tags read `@string/…`; the comment at :50-54 claims the gate "scans all of src/main", which is **false** at `c944496` (basis of ruling 2)
- `ls -A app/src/main` in p15 and `find domain/src/main/resources` — `assets/licenses/{Fredoka,Nunito}-OFL.txt` arrive with phase 15; five opening-book files ship today; neither is scanned under R-1's roots
- `git show --format=%b` on the three code commits, and the trailers of the last 40 `develop` commits — the trailers are `Co-Authored-By: Claude Opus 5 (1M context)` + `Claude-Session:`, the same as 38 of the last 40 commits on `develop`, and they predate plugin 1.0.0's trailer rule. Recorded only; I would not rewrite reviewed SHAs to change trailers
- **Not run:** no Gradle task at all. The dispatch forbids it while an android-game-dev builds in `PegSolitarie-p15`, and studio rule 4 says the tech-lead re-runs no gate. Every fact I needed came from the three reports plus the reads and greps above

## Open issues / risks
- **The positive release control is owed to the release branch.** It is the signed artifact produced from a correct configuration and verified by `apksigner`. The release-manager's §4b signed build is the control of record. It fails closed, and a failure there comes to me.
- **CI has never run, because there is no remote.** The +12–20 min estimate and §9.7's 25-minute trigger stay unmeasured until a remote exists. QA's 11 min 19 s is a local figure with 88 tasks up-to-date, not that figure.
- **The code-auditor's report has no `Ran on:` line.** It was written on 2026-09-21, under plugin 0.5.2, before that line became mandatory and before Q-019. Under the policy then in force it ran on its fable pin. QA and this sign-off ran on Opus under Q-019. QA met Q-019's mitigation: a fresh reviewer that ran the Check and its own probes.
- **Auditor findings, already routed:**
  - F-1 (the overclaiming KDoc) and F-2 (pre-authorised source sets) go to check-gates C3.
  - F-3, hoisting the package-task names, is post-v1.1 (§14.2).
  - F-4, ktlint and detekt over `build-logic`, is post-v1.1 (§2.5, §14.2).
  - F-5, the developer report's misattribution: the auditor's report is the correction of record.
  - F-6, the camelCase abbreviations, is covered at execution time.
  - The `depthSweep` CI split follows §9.7's threshold.
  - None needs a further ruling from me.

## Handoff
Next: **producer**
1. Merge `feature/phase-14-gate-scope` with `git merge --no-ff` and commit this report with it. Then run the Check once on `develop`. If phase 15 merges first, take `develop` in and re-run the Check before this merge. No further tech-lead round is needed for R-1.
2. After R-1 and phase 15 have both merged (Decision 4, row 4), cut `feature/phase-14-check-gates`. Dispatch the android-game-dev with `PegSolitarie-r5/.planning/reports/phase-14/tech-lead-r2-gate-brief.md` **and this report's addendum, A0 to A2**. The code-auditor reviews it, and I sign off against the brief plus the addendum.
3. Dispatch me to apply "Text for `docs/tech-design.md`" on `feature/phase-14-release-docs` before that branch merges. It is a docs edit and never holds an artifact.
