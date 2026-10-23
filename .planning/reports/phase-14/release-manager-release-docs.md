# release-manager report — phase 14 release documents (R-5 + three missing files)

Date: 2026-09-21
Branch: feature/phase-14-release-docs (off develop `8dc9f09`; **uncommitted — see Open issues O-1**)
Skills loaded: `ergoz-soft:standards` (role: release-manager) → plugin copies (v0.5.2, newer than this worktree's `.claude/rules/VERSION` 0.5.1) of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`, `kotlin-android.md`, `compose.md`, `testing.md`, `ip-policy.md`; repo `.claude/rules/lessons.md`. **Not loaded, declared:** `gitflow` and `gsd-ship` skills — no branch was cut, merged, tagged or pushed in this task and no artifact was built; the gitflow *rules* were read and are what `docs/release-checklist.md` §§2/6/7 transcribe. `compose-skill` not loaded: no Compose file is touched.
Ran in parallel with: the R-1′ gate task track (other worktrees, untouched)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- Four documents written, all documentation: `THIRD_PARTY.md`, `CHANGELOG.md`, `docs/release-checklist.md`, `docs/tech-design.md`. Zero app source, test or Gradle file touched; no Gradle task run; no emulator or adb touched; no upload, no deploy, no approval file.
- `THIRD_PARTY.md` derives the shipped set from **Gradle configuration** rather than from the 40-entry version catalogue, and licences are read from cached POMs and from the artifacts themselves where they carry one — six rows are honestly **unverified**, each naming the one thing that would settle it.
- `docs/tech-design.md` is R-6 and belongs to the tech-lead. It is seeded here as **transcription only**, every entry pointing at the report or phase document that decided it, with nine items marked open. I invented no decision.
- One thing I refused: I did not work around the studio's branch-guard hook, which misreads this worktree's branch. The work is written and staged and is **not committed** (O-1).

## Outputs
- `THIRD_PARTY.md` — every shipped idea, asset, snippet and dependency with its licence; §0 states how the shipped line was drawn; §6 is the unverified register; §7 the founder placeholders.
- `CHANGELOG.md` — Keep a Changelog 1.1.0, starting at 3.0.0, player layer + separate "Under the hood" layer.
- `docs/release-checklist.md` — the studio side of cutting `release/<X.Y.Z>`: pre-cut gates, the cut, the gates on the branch, the artifact, real-track-download verification, merge/tag/back-merge, and §7 rollback.
- `docs/tech-design.md` — seeded canonical decision record, nine sections plus an open list.
- `.planning/reports/phase-14/release-manager-release-docs.md` — this report.

## Checks

Documentation checks (what each claim rests on):

- `git rev-parse --abbrev-ref HEAD` in the worktree — **pass** (`feature/phase-14-release-docs`; no branch switch, no merge, no tag, no push at any point).
- `git status --porcelain` before starting — **pass** (clean; the four files plus this report are the only additions).
- `grep -n 'implementation\|ksp(\|compileOnly\|testImplementation\|debugImplementation' app/build.gradle.kts` + `cat domain/build.gradle.kts` — **pass**: 21 `implementation` coordinates + `project(":domain")`; `:domain` declares **only** `testImplementation(junit)` and `testImplementation(truth)`, i.e. zero shipped dependency of its own. This is the factual basis of `THIRD_PARTY.md` §0.
- POM `<licenses>` read from `~/.gradle/caches/modules-2/files-2.1` for 11 coordinates — **pass**: Apache-2.0 declared by `androidx.compose.ui:ui`, `androidx.compose.material3:material3`, `androidx.compose.ui:ui-text-google-fonts`, `androidx.core:core-ktx`, `androidx.navigation:navigation-compose`, `androidx.lifecycle:lifecycle-process`, `androidx.activity:activity-compose`, `androidx.hilt:hilt-navigation-compose`, `com.google.dagger:hilt-android`, `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`, `org.jetbrains.kotlin:kotlin-stdlib`.
- Same cache, Google SDKs — **pass**: `com.google.android.gms:play-services-games-v2` and `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` both declare **"Android Software Development Kit License"**, https://developer.android.com/studio/terms.html — **not** Apache-2.0. Recorded as proprietary-but-permitted rather than swept into a blanket Apache row.
- `unzip -p <room-runtime.aar> META-INF/androidx/room/room-runtime/LICENSE.txt` — **pass** (full Apache 2.0 text inside the artifact itself).
- `unzip -l <user-messaging-platform-4.0.0.aar>` — **finding**: carries `third_party_licenses.json` / `.txt` for its *dependencies* and **no licence of its own**, and no POM is in the local cache. Recorded as **unverified (U-1)**, not inferred from its siblings.
- `find app/src/main/res -type f ! -name '*.xml'` — **pass**: 12 files, all `res/raw/*.ogg`. **No raster image anywhere in the app's resources**, which is what makes the store artwork genuinely unrecorded rather than merely undocumented.
- `docs/audio/CREDITS.md` read — **pass**: twelve-row per-file provenance table, 9 Kenney + 3 freesound borrows, all CC0 1.0, with the reasoning for attribution being a courtesy. `THIRD_PARTY.md` references it rather than duplicating it.
- `sed -n '1,25p' app/src/main/res/values/font_certs.xml` — **pass**: the file's own header records the certs as "the standard GMS signing certs published in the Android downloadable-fonts documentation" — the basis of the §3 snippet row.
- `.planning/ROADMAP.md` read in full — **pass**: phases 6–14 success criteria are the source for every `CHANGELOG.md` bullet; the criteria are already written in outcome language.
- `git log v1.0..HEAD --no-merges --oneline --grep='^fix' -E | wc -l` — **37**. Each was read; the ones describing behaviour a player could observe became the Fixed section, the intermediate-build-state ones did not.
- `git rev-list v1.0..develop --count` — **647** (quoted in `CHANGELOG.md`'s convention note as the reason a commit dump is not a changelog).
- `cat VERSION` / `grep versionCode app/build.gradle.kts` — **3.0.0** / **`versionCode = 3`**. The mismatch against `rules/play-store.md`'s formula (30000) is a **release-branch edit and is not made here** — this branch is off `develop` and a version bump on `develop` would be wrong.
- `grep 'Build command' CLAUDE.md` — `./gradlew :app:assembleRelease` (an APK). Recorded in `docs/release-checklist.md` 2.6 as a release-branch edit, not changed here.
- Reports read in full for the tech-design transcription: `tech-lead-prerelease-signoff.md`, `tech-lead-signoff-r3-signing.md`, `android-game-dev-signing-config.md`, plus `.planning/phases/13-cloud-save/13-CONTEXT.md` (D-03, D-04, D-06, D-07a C-1/C-2/C-3, D-13, D-14) — **pass**: every `docs/tech-design.md` row cites one of them.
- Secret sweep of everything I wrote — `grep -nE 'ermanergoz@|ca-app-pub-|CgkI|\.jks|/Users/|pub-[0-9]|com\.erman\.' THIRD_PARTY.md CHANGELOG.md docs/release-checklist.md docs/tech-design.md` — **pass, 0 hits**. No email, no account id, no keystore path, no password, no Play Console identifier, and deliberately not the `applicationId` either: `docs/release-checklist.md` 5.1 says "the installer is the Play Store package" and points at `app/build.gradle.kts` instead of repeating the package name.
- `ls .planning/approvals/` — **`concept.approved` only**. No `publish-3.0.0.approved` exists, **none was created**, and none should be.
- **Deliberately not run:** any Gradle task (including `:app:dependencies`, which is what would close `THIRD_PARTY.md` §5b and is on the release-branch checklist as 3.5), `./gradlew check`, any build, adb, any emulator, any network fetch, `git merge`, `git tag`, `git push`, and any upload or deploy script.

## Open issues / risks

- **O-1 — I could not commit, and I would not work around the hook.** The studio's `branch-guard.sh` PreToolUse hook resolves the branch with `git -C "$cwd" rev-parse --abbrev-ref HEAD`, where `$cwd` is the **session's primary working directory**. This agent's primary directory is `/Users/ermanergoz/Desktop/PegSolitarie`, which is on `develop`, while the work is in the isolated worktree `/Users/ermanergoz/Desktop/PegSolitarie-r5` on `feature/phase-14-release-docs`. The guard therefore reads `develop` and blocks the commit, correctly by its own logic and incorrectly as to fact. Shell `cd` does not help: an agent's cwd resets to the primary directory between Bash calls (verified — `cd` to the worktree, then `pwd` in the next call, prints the primary path). The hook's own "Known limits" note describes textual ways around it; **I did not use any of them**, because the repo has **no `pre-commit` hook installed** in the shared `.git/hooks` (checked), so the PreToolUse hook is the *only* gitflow enforcement in force and evading it would leave nothing. The files are written and staged. **Producer: either commit them from a session whose primary directory is the worktree, or dispatch worktree tasks with the worktree as the primary directory in the first place.** The exact commit message is in the handoff.
- **O-2 — four `THIRD_PARTY.md` rows block the release and only the founder can fill them** (U-2..U-5): the 20 achievement icons, the listing 512×512 icon, the 1024×500 feature graphic, and any store screenshot containing non-app artwork. `rules/ip-policy.md` blocks a release when a row lacks a licence, so this is a literal blocker. §7 lists the exact five questions. This is operator checklist Step 9 / Q-F3.
- **O-3 — `user-messaging-platform:4.0.0` is unverified** (U-1) and is a shipped dependency. One online command settles it. It would be dishonest to write "Apache-2.0" or "Android SDK Licence" on the strength of its siblings, so it is written as unverified and the gate can see it.
- **O-4 — `THIRD_PARTY.md` §5b is licence-complete by publisher but unenumerated by coordinate.** No `releaseRuntimeClasspath` listing was taken (Gradle deliberately not run). No *licence* risk hides in that gap — every family is permissive or permitted — but a coordinate from an unlisted family could. Checklist item 3.5 closes it on the release branch.
- **O-5 — `docs/tech-design.md` is the tech-lead's file (R-6) and I seeded it.** Every entry is a transcription with a pointer and nothing is invented, but the tech-lead has not reviewed the *framing*, the omissions, or the §9 open list, and should — particularly §1's `minSdk 24` divergence from `rules/kotlin-android.md`, which I recorded as open because no report decides it. If the tech-lead would rather start from a blank file, this one is cheap to discard.
- **O-6 — found while writing, routed rather than fixed:** (a) **no in-app licences surface** exists (`rules/ip-policy.md` asks for one beside `THIRD_PARTY.md`); nothing shipped carries an attribution *obligation* — everything is CC0, OFL, Apache-2.0 or a permitted Google SDK — so it is a studio-rule gap, not a legal blocker, and it needs a ux-designer plus a developer. (b) **`SKILLS.md` does not exist**; no external skill has been installed, so there is nothing to record, noted so the absence is a fact. (c) `CLAUDE.md`'s Build command names an APK and `versionCode` is 3 — both are release-branch edits already on the checklist, and neither is made here.
- **O-7 — the checklist and the changelog both assume gates that do not exist yet.** `docs/release-checklist.md` 3.1–3.3 require a release-settings `check`, an absent `build/fuzz-failures.txt` and a green coverage gate; R-2 has not landed. The checklist is written against the studio's rules rather than against today's build on purpose, so the gap is visible as an unticked box rather than absent from the list.

## Handoff

Next: **producer**.

1. **Commit the work** (O-1), from a checkout whose branch the guard can see:
   ```
   git -C /Users/ermanergoz/Desktop/PegSolitarie-r5 commit -F <message below>
   ```
   Subject: `docs(14): add THIRD_PARTY, CHANGELOG, release checklist and tech design`, body describing the four files, message ending with the two attribution lines this task was given. Everything is already staged.
2. **Queue Q-F3 to the founder** (non-blocking for the studio, blocking for the release): the five store-artwork questions in `THIRD_PARTY.md` §7. Nothing else in that file is waiting on anyone.
3. **Route O-3** to whoever next has the machine online — one dependency listing settles U-1 and O-4 together, and it is checklist item 3.5 on the release branch anyway.
4. **Send `docs/tech-design.md` to the tech-lead** as R-6 input, flagging §1's `minSdk 24` divergence and the §9 open list (O-5).
5. **Backlog O-6(a)** — the in-app licences surface — for v1.1.1, with a ux-designer.
6. R-5 is **complete except for the founder rows**; R-1′, R-2, R-4 and R-7 are unchanged and are not mine.
