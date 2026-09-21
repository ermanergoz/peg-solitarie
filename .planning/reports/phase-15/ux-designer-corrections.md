# ux-designer report — phase-15 ux-spec corrections (measurement identity, orphaned strings)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (c6bddd4..this commit — one `docs(15):` commit)
Skills loaded: ergoz-soft:standards (role: ux-designer)
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, lessons.md
Ran in parallel with: the phase-15 build (same worktree, developer working under `app/src` and
`domain/src` — neither touched here)
Loops: 0
Lessons applied: none

## Summary
Two corrections to `docs/ux-spec.md` from the developer's planning pass, plus the header
amendment line. The orphaned-strings item is now a ruling in §8 (delete all three), verified
against the code first. The package-id item did **not** hold for this document — the spec never
printed a package id anywhere — so the fix is different from the one requested: §6.5 now *states*
the launch identity, and the wrong id stays where it actually is, in another role's report.
One thing the requested fix would not have caught on its own: correcting only the prefix still
leaves a component that does not resolve, because the class package and the `applicationId` are
different strings.

## Outputs
- `docs/ux-spec.md` — three edits, nothing else:
  - header Amendments note — one new paragraph for this correction pass;
  - **§6.5**, "How C1 is measured for this phase" — one new bullet naming the launch identity, the
    fully-qualified component, and a `resolve-activity` form that cannot rot;
  - **§8 › Deleted** — `onboarding_step_1/2/3` ruled deleted, with the verification recorded, plus
    an explicit "not deleted with them" line for `onboarding_got_it` / `onboarding_skip`.
- `.planning/reports/phase-15/ux-designer-corrections.md` — this report. The earlier
  `ux-designer.md` and `ux-designer-amendments.md` are untouched.
- Not touched: anything under `app/`, `domain/`, `docs/ui/`, `15-PLAN.md`, `STATUS.md`,
  `ROADMAP.md`, `FOUNDER.md`, `VERSION`, and every report authored by another role — including
  `tech-lead-architecture.md`, which carries the wrong id and is not this role's file to edit.
  D-01h/D-01i, §5.1 and every acceptance criterion are unchanged; §14 needed no edit (see Checks).

## Checks

No build: this task edits one Markdown document and no source, so the per-item evidence below is
the check. Every claim carries `file:line`. All greps were run read-only in the worktree
`/Users/ermanergoz/Desktop/PegSolitarie-p15`.

### 1. The `applicationId`, verified before editing

- `grep -n "applicationId" app/build.gradle.kts` → one hit:
  `app/build.gradle.kts:279: applicationId = "com.erman.pegsolitarie"`. Confirmed by reading
  `:265-282`, whose own comment records why ("-arie", the published listing, versionCode ≥ 3).
- `app/build.gradle.kts:263: namespace = "com.app.pegsolitaire"` and the comment at `:277-278`
  ("The `namespace` (code packages, BuildConfig) deliberately stays com.app.pegsolitaire — only the
  store identity changes"). The two strings differ in both prefix and stem, on purpose.

### 2. Where the wrong id actually is — the requested edit had no target in this file

- `grep -n "pegsolitaire\|pegsolitarie" docs/ux-spec.md` → **one** hit in the whole document:
  `docs/ux-spec.md:1331`, and it is a source *path* in a §13 test guard
  (`app/src/main/java/com/app/pegsolitaire/ui/onboarding/`), which is the namespace and is
  **correct** as written. Left alone.
- `grep -n "adb \|am start\|shell am\|run-as" docs/ux-spec.md` → two hits, `:1005` and `:1445`
  (criterion 15e), both of which say `am start -W` in prose and print **no command line and no
  package id**. So §6.5 never had a wrong id to fix, and §14 echoes no command.
- The wrong id is real, but it lives in another role's report:
  `grep -n "pegsolitaire\|pegsolitarie" .planning/reports/phase-15/tech-lead-architecture.md` →
  `:360 adb shell am force-stop com.app.pegsolitaire` and
  `:362 adb shell am start -W -n com.app.pegsolitaire/.MainActivity | grep TotalTime`.
  **Not edited** (rules/studio.md: no role rewrites another's report). Routed in Open issues.
- `.planning/phases/15-ux-refresh/15-PLAN.md:1054,1086,1088` uses `com.erman.pegsolitarie`, i.e.
  the right package — but `:1088` is `-n com.erman.pegsolitarie/.MainActivity`, which is the
  second half of the same bug (below). Not edited: the plan is not this role's file.

### 3. The component, not just the prefix (the part that would still have failed)

- `grep -rn "class MainActivity" app/src/main --include="*.kt"` →
  `app/src/main/java/com/app/pegsolitaire/MainActivity.kt:47`, whose `package` declaration is
  `com.app.pegsolitaire` (`MainActivity.kt:1`).
- `AndroidManifest.xml:32-39`: exactly one `<activity>`, `android:name=".MainActivity"` with the
  MAIN/LAUNCHER filter; `grep -n "activity-alias" app/src/main/AndroidManifest.xml` → no match;
  `grep -rn "applicationIdSuffix" app/build.gradle.kts` → no match.
- Therefore the launch component is `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
  The `/.MainActivity` shorthand expands against the launch package and names
  `com.erman.pegsolitarie.MainActivity`, which does not exist — the same "error instead of a
  number" failure as the wrong prefix. §6.5 now states both halves and prefers
  `adb shell am start -W -n "$(adb shell cmd package resolve-activity --brief com.erman.pegsolitarie | tail -1)"`,
  which asks the device rather than hard-coding the pair. **This is not verified on a device** —
  it is read off the manifest, the namespace and the `applicationId`; the developer confirms it
  on the first arm-A sample, where a wrong component is visible immediately.

### 4. The three strings have no remaining caller

- `grep -rn "onboarding_step_1\|onboarding_step_2\|onboarding_step_3" . --include="*.kt"
  --include="*.kts" --include="*.xml" --include="*.md"` → in the app, exactly six lines:
  `app/src/main/res/values/strings.xml:161`, `:162`, `:163` (the definitions) and
  `app/src/main/java/com/app/pegsolitaire/ui/onboarding/OnboardingOverlay.kt:181`, `:182`, `:183`
  (the only reads). Every other hit is prose — `docs/ux-spec.md:1083`, four v1.0 milestone
  planning files, `15-PLAN.md:1180,1336`, `android-game-dev-plan.md:252`.
- `grep -rn "onboarding_step" . --include="*.kt" --include="*.xml"` (broader: any suffix, code and
  resources only) → the same six lines, nothing else. No layout, no test, no other locale file.
- `OnboardingOverlay.kt` is deleted by this refresh (§2 screen 1, D-01g), so the three lose their
  only caller. **Recorded as deleted in §8.** The deletion cannot break the build.
- Guard recorded with it: `onboarding_got_it` and `onboarding_skip` share the prefix, are **kept**
  (§8 › Reused unchanged) and must not be swept with the group.

### 5. The string count — nothing to reconcile

- `grep -n "14 new\|14 string\|fourteen\|13 new\|13 string\|thirteen\|new strings" docs/ux-spec.md`
  → **no matches** before this pass. The spec states no count anywhere; it carries only the §8
  table, which I counted line by line at **13** rows (`:1069-1081`), consistent with
  `tutorial_undo_nudge` having been retired in the earlier amendment (§8, "Retired before it
  shipped"). No "14" existed to correct, so no criterion or table was edited. The header's new
  paragraph is the only place a number is now written, and it says thirteen.

## Open issues / risks
- **For the producer, not fixed here:** `tech-lead-architecture.md:360,362` still carries
  `com.app.pegsolitaire` in its `adb` commands. A developer who follows the report rather than the
  plan hits the silent-failure path. It needs either a tech-lead correction or a producer note
  that §6.5 supersedes it; the spec now says so from its side.
- **Second half of the same typo, in a file I may not edit:** `15-PLAN.md:1088` uses
  `-n com.erman.pegsolitarie/.MainActivity`. Right package, wrong class expansion. The developer
  should take the component from §6.5 (or the `resolve-activity` form) when running the arms.
- The `resolve-activity` command and the component are derived from the manifest and the build
  file, not measured on a device. First real check is arm A's first sample.
- Untouched and still open elsewhere: `docs/audio-spec.md` does not exist (sound-designer track),
  and the C1 seeding precondition (`adb shell which sqlite3`, `15-PLAN.md:1058`) is unverified —
  neither is this role's.

## Handoff
Next: **producer** — both returned items are closed from the spec's side; the developer can build
S5 (the three strings go with `OnboardingOverlay.kt`) and run the C1 arms against §6.5's component.
Two follow-ups are yours to route: the wrong id in `tech-lead-architecture.md:360,362` (tech-lead)
and the `/.MainActivity` expansion in `15-PLAN.md:1088` (developer). Neither blocks the build.
