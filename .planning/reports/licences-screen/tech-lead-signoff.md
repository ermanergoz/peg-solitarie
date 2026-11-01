# tech-lead report — licences-screen (final technical sign-off)
Date: 2026-11-01
Branch: feature/licences-screen (report only) — signs off `0bf578d..9f9ed01` (74 commits, code head `65a9d4e`; the four commits after it change `.planning/` and `docs/ux/licences-screen.md` only). `develop` is at `305f741`, which is this branch's merge-base, so no further merge-in is owed.
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, stacks, testing, puzzle-design, play-store, kotlin-android, compose and dispatch were read in full; `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-architecture, android-gradle-logic
Ran in parallel with: talkback B-1 ruling, colour-vision QA
Ran on: claude-opus-5-5 at effort high (the founder's 2026-10-31 directive (1): one step below the pin; the harness names no effort). **Deviation:** my agent file pins fable at xhigh. Q-019 puts Fable-pinned roles on Opus until 3.0.0 is published, as in `tech-lead-f4-ruling.md`. The code was written on Opus, so every condition below is checked against the repo, not taken from a report.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **R5's four merge conditions are met.** The ux-designer ruled LS-15 (option F, `ux-designer-ls15.md`) and recorded R4's two observations as the platform's (`ux-designer-r4-observations.md`). The code-auditor passed R2 (`code-auditor-r3.md`) and the merge-in plus the test commit (`code-auditor-merge-home.md`). QA passed pass line 2 (38 of 38 stops) and pass line 4 as ruled (`qa-engineer-r4.md`), after F-2 and F-3 passed (`qa-engineer-recheck.md`).
- **The 15e reading: §10's first-frame test, not the file list. No sitting is owed.** The directive narrows sittings and cannot add one that §10 does not owe. The only code on Home's first frame that changed is four destination registrations at graph build, far below what the instrument can read.
- **The auditor's Info notes I-1 (test d's name) and I-2 (duplicated label helpers) are fix-later.** Neither blocks the merge.
- The Check is green on the head: exit 0, and 1,401 `:app` tests with 0 failed (see Checks). **VERDICT: PASS.**

## Merge conditions, each against its evidence
| # | Condition (source) | Evidence | Met |
|---|---|---|---|
| R5.1 | The ux-designer's LS-15 ruling (`tech-lead-f4-ruling.md` R5) | `ux-designer-ls15.md`: option F, the grading table for R4. `ux-designer-r4-observations.md`: O-R4-1 and O-R4-2 recorded, not findings; "R5's merge condition is unchanged". Both committed (`e406169`, `cb9ea7e`) | yes |
| R5.2 | The code-auditor's PASS | `code-auditor-r3.md` (R2 at `595e2a7`): PASS, its forced Check 1,364 tests green, mutations M1, M2 and probes P1, P2 run by the auditor. `code-auditor-merge-home.md` (merge `398be26`, tests `65a9d4e`): PASS, its forced Check 1,401 tests green, M2, R and M3 run by the auditor | yes |
| R5.3 | QA's PASS on pass line 2 and pass line 4 as ruled | `qa-engineer-r4.md` on `65a9d4e` (APK sha256 `4e404920…dd453b`, installed hash equal): run 1 and run 3b-down PASS, run 5 PASS, pass line 2/3 PASS on 38 of 38 stops. Runs 2, 3a, 3b-up and 4 recorded, as the table says | yes |
| R5.4 | My sign-off | this report | — |
| Rerun ruling | Auditor and QA PASS on F-2, F-3, F-4; re-read the merge-in hunks and the departures T4 and the F-2 wrapper (`tech-lead-rerun-ruling.md`, "Then the tech-lead") | `qa-engineer-recheck.md`: F-2 PASS, F-3 PASS (F-4 then went to `tech-lead-f4-ruling.md`, met above). Every merge-in on the branch (`4069b53`, `c32172f`, `a71f21d`, `6e3d5e0`, `398be26`) has an empty `--cc` and a tree equal to `git merge-tree --write-tree` of its parents: no hand-resolved hunk anywhere. T4 (the non-lazy `Column`) stands, with its limit now stated in the KDoc and the tests (R2 point 5, Minor 2 closed). The F-2 wrapper is the only file under `ui/licenses/` naming `textAlign` or `textDirection` | yes |
| Brief §5.2 | Frozen surfaces | `git diff develop...HEAD` touches none of `THIRD_PARTY.md`, the tech design, `docs/ux-spec.md`, the release checklist, `dimens.xml`, `gradle.properties`, `.github/`, `libs.versions.toml` or `build-logic/build.gradle.kts` (0 lines). `app/build.gradle.kts` gains only `id("google-notices")` | yes |
| Boundaries | Layering and the asset path (brief T1, §3.1) | `data/licenses/` imports nothing from `ui/` (0 matches). `assets.open` has exactly two call sites, `AssetLicenseTextSource` and `AssetGoogleNoticeSource`. No `Intent`, `Uri`, `http`, `URL` or `Socket` in `ui/licenses/` or `data/licenses/`. No `@Suppress`, `@Ignore`, `!!`, `runBlocking`, `GlobalScope`, baseline or lint disable added over the branch | yes |

## The 15e reading
**The question** (`code-auditor-merge-home.md`, start-path section). The branch touches `navigation/AppNavHost.kt`, `navigation/Routes.kt`, `navigation/LicensesDestinations.kt` (new), `di/DataModule.kt` and `di/DispatchersModule.kt` (new). All are on §10's start-path list. The founder's directive (2) of 2026-10-31 reads "15e sittings only where start-path code changed".

**The reading I apply: §10's first-frame test.** A sitting is owed where code that runs before or on Home's first frame changed. Touching a file on the start-path list is where to look, not the test itself.
- **Why not the file list.** §10 has two rules. A branch sits when it changes Home's first frame. A `develop` merge-in voids an existing sitting when it touches a start-path file. The file list belongs to the second rule. The directive is in a block that reduces QA work ("Reduced QA … only where …"). Read as the file list, it would add a sitting that §10 does not owe today. A reduction cannot do that.
- **What on the first frame changed, read in the diff.**
  - `NavHost`'s builder now calls `licensesGraph`, which registers four `composable<>` destinations. Each resolves its route's serializer and builds a route pattern once, at graph build. That is four small classes and their serializers loaded, on a graph that already had six such destinations.
  - `AppNavHost` gains four defaulted content lambdas and a third Settings callback. None of them runs until its destination is opened.
  - `di/`: two unscoped `@Binds` and one `@Provides` on an `object`. Hilt calls none of them until a licences ViewModel asks. The two sources have plain `@Inject` constructors with no `init`, `lazy` or `@Singleton`. The only `init` under `data/licenses/` is `GoogleNoticeText`'s hex check, run when a notice is opened.
  - Nothing is read, decoded or loaded. No collector starts. No SDK is called.
- **Why that cannot be read by a sitting.** The gate fails at `max(2 × floor, 100 ms)`, and phase 15's sitting measured a floor of 3.0 ms. Four destination registrations cost well under a millisecond. A sitting would hold the bench, which colour-vision and candy are queued for, and return a number inside the noise.
- **Brief §5.3's void conditions are unmet.** `App.kt`, `MainActivity.kt`, `ui/home/`, `ui/theme/`, `res/font/`, every `themes.xml` and the manifest are untouched by the branch's own diff. Nothing is injected into `App`. Nothing is read from the root. The merge-in brought develop's `ui/home/` and v35 theme files byte-identical to `305f741`, the tree that passed home-chip-arrival's sitting (`876258e`).

**Ruling: no 15e sitting is owed for this branch.** `Pixel_9a_bench` stays queued for colour-vision, then candy.
- **This branch must merge before candy's sitting** (A11, merge order 6a). Merged after it, it would void that sitting, by the merge-in rule.
- **It does not interleave with colour-vision's sitting either.** Colour-vision's arm A is its merge-base with `develop` at its sitting. If this branch lands first, colour-vision takes `develop` in before it sits, and its A..B stays its own diff. If colour-vision has already sat when this lands, its next merge-in brings this branch's `navigation/` and `di/` files. By §10's merge-in rule, that voids colour-vision's sitting. **So merge this branch before colour-vision sits, or not until colour-vision has merged.**

## The auditors' Info notes
- **I-1, test d's name** (`LicencesAccessibilityExposureTest.aCardHalfAboveTheViewportIsExposedWithItsTitleFirst`). **Fix later, not on this branch.** The oracle is stricter than the name, and the KDoc states the full claim: whole, once, title first. Four reports cite the test by this name, so renaming now would break their trail just before the merge. On the next change to that file, rename it to `aCardHalfAboveTheViewportIsExposedAsItsWholeLabelOnce`, and name the old name in the commit body.
- **I-2, the duplicated `labelOf` and `textOf`** (`LicencesAccessibilityExposureTest.kt:115-125` and `LicencesTalkBackContractTest.kt:309-319`). **Fix later.** `LicenseLine` is sealed, so a new variant breaks the compile in both copies, and neither can drift silently. The follow-up is a test-only refactor: one `internal` helper file in the test source set's `ui/licenses` package, used by both classes. No oracle changes. It rides the I-1 rename, or the next licences change.
- **R3 I-2** (the per-composition `texts` list) stays as the auditor ruled: no action.
- **R3 I-3 and merge-home I-3** (commit trailers) are the producer's call. They are not technical.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/tech-lead-signoff.md` — this report, committed alone. No code, spec or tech-design text was written.

## Checks
- `Read` in full: `tech-lead-brief.md` (two pages, §5.3 included), `tech-lead-f4-ruling.md`, `ux-designer-ls15.md`, `ux-designer-r4-observations.md`, `code-auditor-r3.md`, `code-auditor-merge-home.md`, `qa-engineer-recheck.md`, `qa-engineer-r4.md`, `android-game-dev-f4-r2.md` and `android-game-dev-merge-home.md` — pass. `tech-lead-rerun-ruling.md`: lines 1–30 and 128–467 (F-4, the owed work, the sign-off condition and Amendment 1), read from its `grep -n` outline; F-2's and F-3's mechanics (31–127) were reviewed in earlier rounds — pass.
- `sed -n 1230p docs/tech-design.md` (§10, criterion 15e) and `grep -n` of the founder directive in `.planning/reports/STATUS.md:8` — pass. These are the two rules and the directive the 15e reading rests on.
- `git rev-parse develop` and `git merge-base HEAD develop` — pass: both `305f741`.
- For each merge `4069b53`, `c32172f`, `a71f21d`, `6e3d5e0` and `398be26`: `git show --cc --format= <m> | wc -l` and `git merge-tree --write-tree <m>^1 <m>^2` against `<m>^{tree}` — pass: 0 lines each, and every tree equal.
- `git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/{di,navigation}`, read in full — pass. It shows the first-frame changes listed above, and nothing else.
- `git diff --name-only develop...HEAD -- app/src/main | grep -E 'App\.kt|MainActivity|/di/|/navigation/|ui/home/|ui/theme/|res/font/|themes\.xml|AndroidManifest'` — pass: only the five `di/` and `navigation/` files.
- Boundary greps over the branch diff and the head (the Boundaries row above): data → ui imports 0; coordinate lines 0; forbidden markers 0; `assets.open` two sites; network names 0; frozen files 0; `textAlign` and `textDirection` only in `LicencesTextDirection.kt` — pass.
- `uptime` before the Check: 1-minute load 1.98 (below 12).
- `./scripts/check.sh --wait --max-workers=4 --console=plain` on `9f9ed01`, 05:11:14 to 05:22:22 — **pass**.
  - Exit 0: "BUILD SUCCESSFUL in 11m 8s", 107 actionable tasks, 11 executed and 96 up to date. The log has 0 `w:` and 0 `e:` lines.
  - Executed fresh: `:app:lintDebug`, `:app:lint` and `:domain:depthSweep`, among others.
  - Up to date, so their inputs are unchanged since the auditor's forced run on the same code: `:app:testDebugUnitTest`, `:app:detekt`, the ktlint runs and `:app:verifyGoogleNoticesParity`. The `:app` results on disk are that run's (04:54): 152 files, 1,401 tests, 0 failures, 0 errors, 0 skipped.
  - The 1-minute load was 1.98 at the start and 3.43 at the end. No emulator was touched.

## Open issues / risks
1. **Merge order around the bench** (the 15e section above). Merge this branch before colour-vision sits, or not until colour-vision has merged. Merge it before candy sits in either case. The producer schedules this.
2. **The tech design still lacks T4, the F-2 wrapper and R2** (§3.6). Add the 15e reading above to §10 in the same docs sync, so the next branch does not ask again. This is the producer's to schedule (also R1's open issue 5).
3. **TalkBack's behaviour going up and backward is the platform's, by the ux-designer's ruling.** If players report it, option E (a pinned section index) is the recorded remedy, and option B needs a device spike first.
4. **I-1 and I-2** are fix-later, as ruled above.
5. **Staffing (Q-019):** Opus signed off Opus work. The mitigation is that every condition was checked against the repo, and the Check was run here.

## Handoff
Next: producer —
1. **Merge `feature/licences-screen` into `develop`** with `--no-ff`, per the merge order in open issue 1. No 15e sitting is owed first.
2. **Schedule the tech-design docs sync:** T4, the F-2 wrapper, R2, and the 15e reading.
3. **The I-1 rename and the I-2 helper:** fold them into the next licences change, as a test-only commit.

VERDICT: PASS
