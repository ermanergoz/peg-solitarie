# android-game-dev report — phase-15 implementation plan

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (fd60d22..this commit — one `docs(15):` commit. Two audio
documents landed on the branch *while* this plan was being written and are folded in, not
re-argued: the qa-engineer's FAIL on D-01h, and the ux-designer's amendment ruling it into
`docs/ux-spec.md` as **D-01i**. The plan was re-read against the amended spec and aligned to the
D-01i id before this commit — see Checks, "The audio ruling landed twice".)
Skills loaded: ergoz-soft:standards (role: android-game-dev — rules/studio.md, reporting.md,
gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md,
kotlin-android.md, compose.md, testing.md, audio.md, ip-policy.md, lessons.md), compose-skill
Ran in parallel with: phase-14 (five `feature/phase-14-*` branches in other worktrees — not touched)
Loops: 0
Lessons applied: none

## Summary
Eight ordered slices, each ending at a green `check` and one commit, with the two things that can
actually go wrong built second and fourth rather than last: the Home progress aggregate (S2) and
the tutorial's persist-before-pop exit (S4). Every behaviour has a named failing test in front of
it, and each of the ten prohibitions has a grep or a named test so a reviewer verifies instead of
trusting. The overturned audio ruling added a slice: lifting `JumpSound` out of `GameScreen` is now
S3, its own commit, because a separate refactor is the only thing that structurally prevents the
duplicated pitch ladder the qa-engineer warned about. Three counts in the inputs were wrong and are
corrected with evidence: 11 test doubles not 16, 13 new strings not 14, and six callers of
`top_bar_touch_target` not one.

## Outputs
- `.planning/phases/15-ux-refresh/15-PLAN.md` — the phase plan: §0 the check precondition and the
  measured baseline, §1 the slice map with inter-slice dependencies and why the order is what it
  is, S1–S8 with test-first sequences, §10 the ten prohibitions as commands, §11 the runnable
  cold-start procedure, §12 the file-by-file inventory, §13 four items routed to the producer,
  plus a threat register and verification block in the house shape.
- `.planning/reports/phase-15/android-game-dev-plan.md` — this report.
- **No file under `app/`, `domain/`, `docs/`, `.planning/{ROADMAP,questions,reports/STATUS}` or
  `VERSION` was changed.** Planning only, as briefed.

## The slice order, in one table

| # | Slice | Depends on | Why here |
|---|-------|-----------|----------|
| S1 | `:domain` `TutorialBoards` + dead-end-freeness proof | — | S4's dependency root; `:domain:test` loop is seconds |
| S2 | Home progress aggregate (DAO → repo → 11 doubles → VM) | — | **Risk 1.** The only new read on the start destination |
| S3 | Lift `JumpSound` into a shared effect | — | Stops the pitch ladder being copied into S4 |
| S4 | Tutorial destination, ViewModel, exit ordering, screen, audio | S1, S3 | **Risk 2.** The navigation loop a dropped write causes |
| S5 | Info button out, Settings row in, `tutorialPending`, two retargets | S4 | Where the two by-design breakages land |
| S6 | Home recomposition (five bands) | S2, S5 | Needs the data and the deleted bar |
| S7 | Gameplay status strip + action dock | S3 (merge order) | Largest diff, fewest unknowns |
| S8 | Token retirement, C1 measurement, evidence | S6, S7 | Both inputs only exist then |

## Checks

Every fact in the plan was established by one of the commands below or by reading a cited
`file:line`. All commands run in `/Users/ermanergoz/Desktop/PegSolitarie-p15` on
`feature/phase-15-ux-refresh`, working tree clean before this commit.

**The check command, both ways**

- `./gradlew check --offline` — **fail** in 633ms: *"Could not determine the dependencies of task
  ':app:lintReportDebug'. > SDK location not found…"*. This worktree has no `local.properties`
  (`ls -la local.properties` → No such file or directory) and `$ANDROID_HOME` is unset in the
  session. Configuration is fine — `./gradlew help --offline` → **pass**, BUILD SUCCESSFUL in 623ms
  — so it is a missing SDK path, not a build defect.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **pass**, **BUILD SUCCESSFUL
  in 11m 9s**, 98 actionable tasks (10 executed, 1 from cache, 87 up-to-date); `:app` + `:domain`
  ktlint, detekt, lint, the four literal gates and all unit tests green. Independently 11m10s by
  the tech-lead on the same unmodified tree (`tech-lead-architecture.md:480`) — the two agree to
  one second, so it is a stable pre-refresh baseline. `ls -d $HOME/Library/Android/sdk` → exists.
- `sed -n '265,274p' build-logic/src/main/kotlin/standards.gradle.kts` — read: `check` dependsOn
  `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck`,
  `audioFocusLiteralCheck`.

**The test-double count — the tech-lead's 16 is 11**

- `grep -rln 'ProgressRepository' app/src/test | wc -l` — **16** (files that *mention* the type).
- `grep -rn 'ProgressRepository' app/src/test | grep -E 'class |object '` plus
  `grep -n ': ProgressRepository' <each remaining file>` — **11** files declare an implementation
  the compiler will force to override. Listed with file:line in 15-PLAN §12.5:
  `HomeViewModelTest.kt:594`, `HomeViewModelCloudTest.kt:340`, `GameViewModelResumeTest.kt:586`,
  `GameViewModelSoundTest.kt:597`, `GameViewModelTelemetryTest.kt:751`,
  `GameViewModelAllowanceTest.kt:811`, `ClassicCatalogViewModelTest.kt:235`,
  `ResultsViewModelTest.kt:1698`, `ChallengeNextLevelTest.kt:354`, `ProgressSubmitterTest.kt:603`,
  `ProgressSubmitterCloudTest.kt:1192`.
- The five that need **nothing** construct the real Room-backed repository:
  `ProgressRepositoryTest.kt:35`, `ProgressRepositoryFailureTest.kt:44`,
  `ProgressAggregateTest.kt:46`/`:370`, `ProgressImportTest.kt:53`, `ResumeRepositoryTest.kt:37`.
  Two `mockk(relaxed = true)` defaults also need nothing: `GameViewModelAllowanceTest.kt:633`,
  `GameViewModelTelemetryTest.kt:581`. **Confirmed: 11, not 16** — a developer expecting 16 compiler
  errors would hunt five that never arrive.
- `grep -rn ': ProgressRepository' app/src/testDebug app/src/debug` — no such source sets
  (`ls app/src` → `debug main release test testDebug`; `debug`/`testDebug` hold no doubles).

**The two retired tokens — six callers, not one**

- `grep -rn 'top_bar_touch_target\|onboarding_illustration_size' app/src` — **8 hits**: the two
  declarations (`dimens.xml:51`, `:110`) plus `UndoControl.kt:38`, `HintControl.kt:56`,
  `HintControl.kt:66`, `HintToggleControl.kt:39`, `RestartControl.kt:35`, `GameScreen.kt:509` for
  `top_bar_touch_target` and `OnboardingOverlay.kt:144` for `onboarding_illustration_size`. The spec
  says both "lose their last caller"; `top_bar_touch_target` has **six**, so the retirement cannot
  happen before S7 retargets all of them. That is why it is S8.

**The two tests that break by design**

- `sed -n '105,123p' app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — read:
  `theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound`, `clickIcon(R.string.how_to_play)` at `:119`.
- `sed -n '148,159p'` and `sed -n '366,367p' app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt`
  — read: `theProfileSwapDialogIsNeverStackedOnTheOnboardingOverlay` uses
  `ONBOARDING_DISMISS_LABEL`, declared at `:367` as the literal `"Got it"`.
- `sed -n '36,52p' app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — the file's own
  rule that both halves of a selectivity claim must be asserted; the retargets follow it.

**The `GameStatus` trap in the tutorial**

- `cat domain/.../model/GameStatus.kt` — `enum class GameStatus { Playing, Won, Stuck }`.
- `grep -n '^data class GameUiState' -A 20 app/src/main/.../ui/game/GameUiState.kt` — `status` is a
  **required** constructor field, so `TutorialViewModel` cannot assemble a `GameUiState` without
  supplying one, even though §4.1 forbids using `GameStatus` as the completion predicate.
- `grep -n 'state\.' app/src/main/.../ui/components/BoardCanvas.kt` — reads `board`, `animating`,
  `selected`, `showHints`, `legalTargets`, `hintSource`, `hintTarget` and **never `status`**.
  Resolution in the plan (S4 test 5): pass `GameStatus.Playing` as a constant, evaluate
  `board.pegCount == 1` separately. Without this note the developer either violates the spec or
  gets stuck; it is a reading, not a guess.

**The audio ruling landed twice — plan aligned to the second**

The brief directed me to plan to the outcome rather than to whichever spec revision I happened to
read. That was the right instruction: the spec changed under me mid-task, and the plan is aligned to
the landed version.

- `ls .planning/reports/phase-15/` — `ux-designer-audio-amendment.md` appeared after I had read the
  spec in full; `git status --short` showed `.planning/reports/STATUS.md` modified by another agent
  (two new Done rows, one new blocker row). **I did not stage it** — that file is the producer's.
- `grep -n 'D-01i' docs/ux-spec.md` — **16 hits**. The decision id is **D-01i**, filed as an
  amendment to D-01h in house style (no decision rewritten in place), not "D-01h as amended".
  `sed -n '233,262p'` — read the full D-01i table: jump pair **fires**; `Select`, `Deselect`,
  `InvalidTap`, `Undo`, `Hint`, `StarPop`, `Win` **silent** (D-01h upheld); CTA `ButtonForward` and
  `Skip` `ButtonDismiss` **fire**; system back silent. This matches the outcome the brief gave, so
  no slice changed — only the citations did.
- `sed -n '826,838p' docs/ux-spec.md` — §5 now carries the **enumerated both-halves contract**,
  which is what makes P6 checkable rather than rhetorical.
- `sed -n '864,877p' docs/ux-spec.md` — §5.1 gained "What these four do not forbid — clarified, not
  relaxed", explicitly legalising S3's lift and explicitly stating that lifting one effect is not
  licence to host the file. S3's rationale now cites the spec rather than only the QA report.
- `grep -n 'TutorialSoundTest' -A 3 docs/ux-spec.md` — §13's owed list (`:1328-1333`) names **three**
  `TutorialSoundTest` methods plus the grep guard, and its allow-list is the four cues the screen
  *plays* (`Jump`, `PegRemove`, `ButtonForward`, `ButtonDismiss`). Two findings from that:
  (a) the mechanical grep over `ui/onboarding/` sees only **two** names, because the jump pair is
  named inside `JumpSoundEffect` and the call site names no cue — 15-PLAN §10 P6 now reconciles the
  two lists explicitly, and keeps the stricter two-name form because a `Jump` appearing under
  `ui/onboarding/` is precisely the copy-instead-of-call drift S3 exists to prevent;
  (b) **§13's list has no test for criterion 7e** (`:1398`, the CTA and `Skip` press cues), so the
  plan adds a fourth (S4 test 15). 15-PLAN §13 item 5 records that the plan is a superset of §13's
  bill, never a subset.
- `awk '/^### New strings/,/^The three new tutorial captions/' docs/ux-spec.md | grep -c '^| \`'` —
  **13**. D-01i added no copy. So the 13-vs-14 discrepancy is **not** a spec defect; the spec is
  internally consistent and only downstream counts are stale. 15-PLAN §13 item 1 was downgraded
  from "route to the ux-designer" to "build 13, no action".
- `grep -n 'SOUND-01\|SOUND-04' docs/ux-spec.md` — §16 now carries SOUND-01 (`:1529`), SOUND-02/03
  (`:1530`) and SOUND-04 (`:1531`). The traceability gap that let D-01h narrow a Complete
  requirement invisibly is closed, so nothing on that thread is owed by this plan.

**The audio ruling's mechanics**

- `sed -n '157,164p' app/src/main/.../ui/game/GameScreen.kt` — the one block invoking all eight
  effects; `JumpSound(transition = state.animating)` at `:158`.
- `sed -n '321,330p' …/GameScreen.kt` — `private fun JumpSound`: `playJump(runIndex)`,
  `delay(SoundParams.REMOVAL_LAYER_DELAY_MS)`, `play(SoundCue.PegRemove)`. This is the body S3 lifts.
- `grep -n 'SoundCue\|SoundPlayer\|sound' app/src/main/.../ui/components/BoardCanvas.kt` — **0 hits**,
  confirming the ruling's premise and P7.
- `grep -rn 'fun withClickSound' app/src/main` — `audio/LocalSoundPlayer.kt:36`; its KDoc
  (`:25-34`) explains why the cue is per-call-site and not folded into `claySquish()`, which is why
  S3 puts the lifted jump effect in the same package rather than inventing a new one.
- `find app/src/test -name FakeSoundPlayer.kt` — `app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt`
  exists, so the four `TutorialSoundTest` tests need no new infrastructure.

**Cold-start seeding facts (so §11 is runnable and not aspirational)**

- `grep -n 'applicationId' app/build.gradle.kts` — `com.erman.pegsolitarie` at `:279`. **Note: the
  tech-lead's procedure and `docs/ux-spec.md` §6.5 both use `com.app.pegsolitaire`**, which is the
  *package* name, not the `applicationId`. `adb` commands must use `com.erman.pegsolitarie` or they
  silently target nothing. The plan's §11 uses the correct id.
- `grep -n 'const val' app/src/main/.../data/ProgressPersistenceKeys.kt` — `DB_NAME = "peg_solitaire.db"` (`:20`),
  `TABLE_LEVEL_RESULT = "level_result"` (`:23`), `MODE_CHALLENGE = "challenge"` (`:67`).
- `grep -n 'val ' app/src/main/.../data/db/LevelResultEntity.kt` — `mode`, `levelId`, `stars`,
  `bestTimeMs`, `bestScore`, `completedAt` — the six columns the seed SQL writes.
- `sed -n '35,55p' app/src/main/.../di/DatabaseModule.kt` — the DB is built from
  `ProgressPersistenceKeys.DB_NAME`, no destructive fallback, so a corrupted push shows as a crash
  and not as a bad number. The plan says to verify the row count before measuring.
- **Not run:** any `adb`, emulator or device command. Running a device is not this role's authority
  and this is a planning task; §11 is written so the executing developer runs it, with an explicit
  `adb shell which sqlite3` precondition and a host-side fallback because whether the API-36 image
  ships `sqlite3` was not verified from here and must not be assumed.

**The `ANY_STARS` predicate in the new query**

- `grep -rn 'ANY_STARS' app/src/main` — `ProgressRepository.kt:575` (the cleared predicate) and
  `:716` (`const val ANY_STARS = 0`). `sed -n '705,716p'` — its KDoc: *"a stored row means the board
  was cleared regardless of how well."* So `stars >= 0` in the new aggregate is deliberate parity
  with the existing predicate, not a bug. S2's test is required to say so, or a later reader
  "fixes" it to `>= 1` and changes a shipped number.
- `sed -n '55,95p' app/src/main/.../data/db/LevelResultDao.kt` — the `FROM`-less
  correlated-subquery shape of `totals()`, and its both-modes `totalStars` with no `WHERE`
  (`:66`), which the new query copies deliberately.

**House plan structure**

- Read in full: `.planning/phases/13-cloud-save/13-03-PLAN.md` and
  `.planning/phases/08-assist-generator-quality-debt/08-02-PLAN.md`. The house shape is YAML
  frontmatter (`phase`, `plan`, `type`, `wave`, `depends_on`, `files_modified`, `autonomous`,
  `requirements`, `must_haves.truths` / `.artifacts` / `.key_links`) then `<objective>`,
  `<execution_context>`, `<context>`, optional `<interfaces>`, `<tasks>` with
  `<task type="auto" tdd="true">` carrying `<name>/<files>/<read_first>/<behavior>/<action>/<verify>/<acceptance_criteria>/<done>`,
  then `<threat_model>` (Trust Boundaries + STRIDE register with `T-NN-MM` ids),
  `<verification>`, `<success_criteria>`, `<output>`. `ls .planning/phases/13-cloud-save/` — the
  convention is **one file per plan** (`13-01`…`13-12`), so 15-PLAN.md states that each `## S<n>`
  section is the body of `15-0<n>-PLAN.md` if the producer wants per-slice files.
- Other structural facts read: `Routes.kt` in full (the five existing `@Serializable` routes and the
  KDoc voice `Tutorial` must match); `AppNavHost.kt:95-145` (the content-slot idiom);
  `HomeRoute.kt` in full (`onShowHowToPlay` wired at `:48`); `HomeUiState.kt:69-85`
  (`onboardingVisible` at `:70`, no star/score fields); `GameScreen.kt:165-215` and `:489-536`
  (the `Scaffold` `topBar` slot and `GameTopBar`); `SettingsScreen.kt:194-218` and `:664-673`
  (`GeneralSection` and the ten positional `*_ORDER` constants that S5 must renumber to eleven);
  `ProgressProjections.kt:41`/`:63`; `BoardCanvas.kt:80-110` (the `GameUiState` parameter);
  `grep -rn 'TEST_TAG' app/src/main` (the existing seven tag constants the four new ones imitate).

**Contract documents read in full or in the cited sections**

`docs/ux-spec.md` (all 1345 lines); `.planning/reports/phase-15/tech-lead-architecture.md` (all);
`.planning/reports/phase-15/game-designer-tutorial-boards.md` (all);
`.planning/reports/phase-15/qa-engineer-tutorial-audio.md` (all);
`.planning/reports/phase-15/ux-designer-amendments.md` (Summary, Outputs, Open issues, Handoff);
`.planning/reports/phase-15/release-manager-sequencing.md` (§0, §The ruling, §VERSION);
`.planning/ROADMAP.md` Phase 15 (`:399-419`); `.claude/rules/*` via ergoz-soft:standards.

**Not run**

- `./gradlew check` was run **twice** (once failing without the SDK to document the precondition,
  once green to establish the baseline); no source, test or build file was modified, so there is
  nothing else to verify. No emulator, no device, no `adb`.
- `.planning/log/` was not opened.

## Open issues / risks

Four items neither the tech-lead nor the ux-designer resolved. Per the brief I did not pick
silently; all four are recorded in 15-PLAN §13 with a decided default so nothing waits.

1. **Criterion 7e has no test in §13's owed list.** `ux-spec.md:1328-1333` names three
   `TutorialSoundTest` methods and the grep guard; criterion 7e (`:1398`) requires the completion
   CTA's `ButtonForward` and `Skip`'s `ButtonDismiss` and no listed test asserts them. **Default:
   S4 carries a fourth test (test 15).** The plan is a superset of §13's bill; no ruling needed.
   *(The new-string count is 13 and the spec is internally consistent at 13 — verified by counting
   the live table. Any "14" downstream is a pre-amendment figure, so there is nothing to route.)*
2. **`onboarding_step_1/2/3` (`strings.xml:161-163`) are orphaned and the spec does not say what
   becomes of them.** §8's Deleted list names only `how_to_play`; the New-strings note calls the
   three tutorial captions *rewrites* of these three, which implies replacement without stating it.
   `OnboardingOverlay.kt` is their only caller and S5 deletes it. **Default: delete all three in
   S5**, on the precedent §8 sets by retiring a string with no referent. Three lines either way.
   Producer → ux-designer.
3. **`docs/ux-spec.md` §6.5 and the tech-lead's §3 both use `com.app.pegsolitaire` in the `adb`
   commands; the `applicationId` is `com.erman.pegsolitarie`** (`app/build.gradle.kts:279`). An
   `am start -W` against the wrong id returns an error, not a number, so this would have cost the
   executing developer a confused half-hour. 15-PLAN §11 uses the correct id. Producer → ux-designer
   for the spec line; no decision needed, it is a typo of record.
4. **The C1 seeding step has no procedure of record and mine has an unverified precondition.**
   Neither the spec nor the tech-lead says *how* to seed ~200 banked Challenge levels. §11 gives a
   `run-as sqlite3` recipe with a host-side fallback, gated on `adb shell which sqlite3` — I could
   not verify from a planning task whether the API-36 emulator image ships `sqlite3`, and running a
   device is not this role's authority. Flagged so the developer checks rather than discovers.

Carried forward from the inputs, not mine to close: **`docs/audio-spec.md` does not exist** though
rules/audio.md makes it the cue list of record, and this phase now places a cue on a new screen
(qa-engineer FAIL item 2 — a sound-designer dispatch or a tech-lead follow-up; predates and should
not block Phase 15); and **`docs/tech-design.md` does not exist**, so four binding architecture
rulings live only in a report (tech-lead Open issue 1).

**No escalation is owed.** Q-901 and Q-902 are answered and built to. No acceptance criterion
conflicts with the tech design or the GDD. No copyleft dependency — no dependency at all, which is
what keeps `check --offline` green without an online warm. No slice needs a suppression beyond the
`@Suppress("MagicNumber")` path-data precedent `UndoIcon.kt` and `ModeGlyphs.kt` already carry, and
the plan states that anything beyond it is a code-auditor question rather than a developer decision.

**Risks in the plan itself**

- **S6 and S7 are the two largest visual diffs and neither is protected by golden images** — the
  tech-lead ruled no screenshot suite in Phase 15 and I do not reopen it. The compensation is the
  four `testTag` constants and the semantic assertions in S6/S7, plus criteria 15a/15f/26/27 as
  device checks. A reviewer should expect to read those diffs rather than diff pixels.
- **Three slices touch `GameScreen.kt`** (S3 the effects block, S7 the `Scaffold` and `GameTopBar`,
  S4 not at all once S3 has landed). Disjoint regions, but if the producer parallelises into
  worktrees the merge order is S3 → S7.
- **The phase's own `check` bill is about 90 minutes of wall clock** (eight slices × 11m10s). The
  plan tells the developer to iterate on narrow `--tests` filters and spend the full `check` once
  per slice; a developer who runs `check` after every edit will spend a day on builds.

## Handoff

Next: **producer** — route to the **ux-designer**: the orphaned `onboarding_step_1/2/3`
(15-PLAN §13 item 2) and the `com.app.pegsolitaire` → `com.erman.pegsolitarie` `applicationId` typo
in the §6.5 `adb` commands (item 3). Item 5 (no test for criterion 7e) is already answered by the
plan and needs confirmation only; item 4's `sqlite3` precondition is information for the developer,
not a decision. Carry the two pre-existing gaps (`docs/audio-spec.md`, `docs/tech-design.md`) on
their existing tracks. Then dispatch the build:
the eight slices are independently dispatchable in the order given, and S1/S2/S3 have no
dependencies on each other if the producer wants three worktrees in wave 1.

Then: **android-game-dev (execution)** — export `ANDROID_HOME` before anything else; build S1→S8 in
order; deliberately break the two proof tests once each (S1 acceptance criterion 5, S4 acceptance
criterion "break test 1") because a proof that cannot fail is not a proof.

Then: **code-auditor** — 15-PLAN §10 is the review contract: ten prohibitions, each one command or
one named test, none of which needs the author's reasoning. Add S2's rule ("no Kotlin fold over
`observeResults(MODE_CHALLENGE)` anywhere") and S3's ("exactly one `playJump` and one
`REMOVAL_LAYER_DELAY_MS` call site repo-wide"). I am the wrong role to review the plan I wrote or
the diff that implements it.

Then: **qa-engineer** — the device bill is criteria 3, 4, 7a, 7b, 7c, 7d, 7e, 13a, 13b, 15a, 15e,
15f and 25, and 15-PLAN §11.3 lists the four readings that cannot be claimed from a unit test: the
two cold-start tables, the 15f first-paint check, the 15a first-viewport check, and criterion 7c
**with OS animations off**, where `runJumpAnimation`'s snap path (`BoardCanvas.kt:298-304`) makes
the jump cue's `LaunchedEffect` a race that only a device can settle.
