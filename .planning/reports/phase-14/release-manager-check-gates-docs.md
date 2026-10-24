# release-manager report — check-gates-docs (THIRD_PARTY kover row; release-checklist 1.2, 3.1–3.3)
Date: 2026-10-24
Branch: feature/phase-14-check-gates (committed by the producer; working-tree edits on head e6acbca)
Skills loaded: ergoz-soft:standards (role release-manager; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; plus `.claude/rules/lessons.md`; no `.planning/recall/RECALL.md` exists)
Ran in parallel with: none known to this role
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons in the snapshot)

## Summary
- `THIRD_PARTY.md` §0 now has a kover row: Gradle plugin `org.jetbrains.kotlinx.kover` 0.9.11, JetBrains, build-only. It gives the source URL, the licence (Apache-2.0, verified from the POMs and the repository's own LICENSE.TXT at tag v0.9.11) and how it is credited. The "pending feature/phase-14-check-gates" note is gone, and §8 names kover among the build-only plugins.
- `docs/release-checklist.md`: 1.2 and 3.1 name `./scripts/check.sh` as the Check command (CLAUDE.md line 6). 3.1 explains that 100,000 clamps to the whole 10,000-level space and quotes design §7.3 item 6 and the R-2 brief. 3.2 names `domain/build/fuzz-failures.txt` and gives the command that proves it is absent. 3.3 records `:domain` line coverage of 99.0 % (896/905) at bound 90 under `koverVerify`.
- No box was ticked. These are definition edits, not a release run. No Gradle was run and nothing was committed.

## Outputs
- `THIRD_PARTY.md`: §0 line 45 (kover in the plugin list, the pending note removed); new line 46 (kover row: coordinates, URL, licence evidence, credit); §8 line 396 (kover named as build-only).
- `docs/release-checklist.md`: items 1.2 (line 33), 3.1 (line 69), 3.2 (line 70) and 3.3 (line 71).
- `.planning/reports/phase-14/release-manager-check-gates-docs.md`: this report.

## Checks
- `grep -A6 '<licenses>'` on the four cached 0.9.11 POMs under `~/.gradle/caches/modules-2/files-2.1` (`kover-gradle-plugin`, `kover-features-jvm`, `kover-jvm-agent`, and the marker `org.jetbrains.kotlinx.kover.gradle.plugin`): pass. All four say "The Apache Software License, Version 2.0" and give the URL `https://github.com/Kotlin/kotlinx-kover`.
- `curl -sS https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kover-gradle-plugin/0.9.11/kover-gradle-plugin-0.9.11.pom | grep -A3 '<license>'`: pass (Apache Software License, Version 2.0).
- `curl -sS https://raw.githubusercontent.com/Kotlin/kotlinx-kover/v0.9.11/LICENSE.TXT | sed -n 1,4p`: pass ("Apache License / Version 2.0, January 2004"). `api.github.com/repos/Kotlin/kotlinx-kover/license` gives `spdx_id: Apache-2.0`.
- The Plugin Portal marker POM (`plugins.gradle.org/m2/.../org.jetbrains.kotlinx.kover.gradle.plugin-0.9.11.pom`) has no `<licenses>` block, only a dependency on `kover-gradle-plugin`. This is noted, not a failure. The licence is read from the artifact that block points to.
- `grep -n kover domain/build.gradle.kts gradle/libs.versions.toml app/build.gradle.kts`: pass. kover is applied only in `:domain` (line 7) and declared at catalog lines 116 and 237. It is absent from `app/build.gradle.kts`, which agrees with "build-only, not shipped". The developer's C7 check also found `releaseRuntimeClasspath` the same before and after (191 = 191).
- `grep -n '^Check command' CLAUDE.md`: pass (`6:Check command: ./scripts/check.sh`).
- `grep -c 'gradlew check' docs/release-checklist.md`: pass (0).
- `grep -n -o '…(`./scripts/check.sh` is the Check command, `CLAUDE.md` line 6' docs/release-checklist.md`: pass (lines 33 and 69).
- `grep -n -o '100,000 is exhaustive, not a sample'` and `'MAX_CHALLENGE_LEVEL = 10_000'`: pass (line 69). Checked against `GeneratorConstants.kt:42` (`const val MAX_CHALLENGE_LEVEL = 10_000`), `docs/tech-design.md` §7.3 item 6 (lines 676–679) and `tech-lead-r2-gate-brief.md` lines 45–47. The console strings quoted in 3.1 are copied from `FuzzHarnessTest.kt` lines 44–45 and 64.
- `grep -n -o 'test ! -e domain/build/fuzz-failures.txt'`: pass (line 70). The path matches `FuzzHarnessTest.kt:162` (`File("build", "fuzz-failures.txt")` in `:domain`'s test working directory) and design §7.3 item 5.
- `grep -n -o ':domain\` line coverage 99.0 % (896 of 905 lines) at bound 90'`: pass (line 71). The figure comes from the developer's C6 check (root LINE counter, missed 9, covered 896, 99.0055 %).
- `grep -n -o 'kover: Gradle plugin \`org.jetbrains.kotlinx.kover\` 0.9.11'` and `'Licence: Apache-2.0'` in THIRD_PARTY.md: pass (line 46). `'kover 0.9.11, Apache-2.0, included'`: pass (line 396). `grep -c 'pending feature/phase-14-check-gates'`: pass (0).
- Table integrity, counting cells with `awk -F'|'`: pass. THIRD_PARTY §0 rows 45 and 46 have 3 cells each, the same as the table. Checklist rows 3.1, 3.2 and 3.3 have 4 cells each.
- `git diff --stat`: 2 files, +7/−6. `git status --short` shows only those two files and this report.
- Gradle: not run, as the dispatch required.
- The `rules/ip-policy.md` matrix check passes: Apache-2.0 is "yes", and the plugin is not distributed.

## Open issues / risks
- 3.3 now records 99.0 % as the figure measured when the gate was introduced. The cut must still record its own figure, and the text says a figure below 99.0 % is reported, not only passed. That is a reporting rule, not a second gate: the gate stays `minBound(90)`.
- The developer's open issue about kover cutting the margin of the wall-clock deadline tests affects item 3.1 at the cut. A run on a loaded host can go red for reasons unrelated to the release. That is the tech-lead's ruling. Until then, 3.1 should run on a quiet host, and the producer schedules it.
- Checklist line 42 ("A Check command that does not yet implement the studio's own gates (coverage, fuzz) …") is now generic, not a live gap. It is left as written because it is still a correct rule.

## Handoff
Next: producer — commit `THIRD_PARTY.md`, `docs/release-checklist.md` and this report on `feature/phase-14-check-gates` with the branch's other review outputs. The code-auditor reviews this diff with the rest of `develop...HEAD`.

## Pass 2 — code-auditor findings 1 and 2 (`.planning/reports/phase-14/code-auditor-check-gates.md`)
Date: 2026-10-24. Branch: feature/phase-14-check-gates (head 9b04cf4; working-tree edits, committed by the producer). Loops: 1. Only `THIRD_PARTY.md` and `docs/release-checklist.md` were touched. `docs/tech-design.md` belongs to the tech-lead and was not opened for writing.

### Summary
- **Finding 1:** `THIRD_PARTY.md` §4 gains the row `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.3.0, Apache-2.0. It cites the Google Maven POM URL and the LICENSE.txt bundled in the AAR. The §5b `androidx.*` family row now says that direct `androidx.*` coordinates have their own §4 rows, this one included, and adds "Hilt Navigation" to its pulled-by column. The auditor also flagged a stale §0 citation, now `app/build.gradle.kts:478-551` (`feature/phase-14-check-gates` 9b04cf4); it was `478-547` (`develop` 0582e3f).
- **Finding 2:** I chose `--rerun-tasks`, not the JUnit XML. Items 1.2 and 3.1 now run `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks`. A `FROM-CACHE` or `UP-TO-DATE` `:domain:fuzz` leaves 3.1 unticked. Item 3.4 now reads its task list from that run.
- **Why `--rerun-tasks`:**
  - The cache hit affects more than the fuzz lines. Item 3.4 asks to "confirm the task list actually invoked" the literal gates, and 3.3 asks for the measured coverage figure. On a cache hit, all of those tasks come back `FROM-CACHE` too. The XML route would mend 3.1 alone and leave 3.3 and 3.4 reading restored outputs.
  - With the flag, every gate the release relies on runs on the release branch's own tree in that run.
  - It costs almost nothing. `check` is about 11 minutes either way, most of it `depthSweep`, and the green `--rerun-tasks` Checks on this branch took 11m 32s and 11m 48s.
  - `check.sh` passes every argument to `./gradlew check` verbatim (`scripts/check.sh` lines 20 and 110). The developer and the auditor both ran `./scripts/check.sh --offline --rerun-tasks`.

### Checks
- `curl -sS https://dl.google.com/android/maven2/androidx/hilt/hilt-lifecycle-viewmodel-compose/1.3.0/hilt-lifecycle-viewmodel-compose-1.3.0.pom | grep -A4 '<license>'`: pass ("The Apache Software License, Version 2.0").
- `unzip -l` on the cached `hilt-lifecycle-viewmodel-compose-1.3.0.aar`, filtered for `license|notice`: pass. Its only hit is `META-INF/androidx/hilt/hilt-lifecycle-viewmodel-compose/LICENSE.txt` (10,175 B), which opens "Apache License / Version 2.0, January 2004". There is no NOTICE file, so item 3.5a has nothing to add for it. No POM is in the local cache, which agrees with the auditor.
- `grep -n 'hilt-lifecycle-viewmodel-compose\|^hiltNavigationCompose' gradle/libs.versions.toml`: pass. Line 191 uses `version.ref = "hiltNavigationCompose"`, and line 36 sets it to `"1.3.0"`. `sed -n 511p app/build.gradle.kts` → `implementation(libs.hilt.lifecycle.viewmodel.compose)`.
- `grep -n 'implementation(' app/build.gradle.kts | sed -n '1p;$p'`: lines 478 and 551, matching the new §0 citation.
- `grep -n -o '\`androidx.hilt:hilt-lifecycle-viewmodel-compose\` | 1.3.0 | Apache-2.0' THIRD_PARTY.md`: pass (line 197). `'app/build.gradle.kts:478-551'`: pass (line 39). `'Hilt Navigation |'`: pass (line 227).
- `grep -n 'org.gradle.caching' gradle.properties`: `3:org.gradle.caching=true`, the premise of finding 2.
- `grep -n -o 'FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks' docs/release-checklist.md`: pass (lines 33 and 69). `'\`--rerun-tasks\` is required'`: pass (lines 33 and 69). `"3.1's \`--rerun-tasks\` console"`: pass (line 72). The old bare command `` `FUZZ_SEEDS=100000 ./scripts/check.sh` `` occurs 0 times.
- Table integrity, counting cells with `awk -F'|'`: pass. §4 row 197 and §5b row 227 have 4 cells each. Checklist rows 1.2, 3.1 and 3.4 have 4 cells each.
- `git diff --stat`: 2 files, +6/−5. `git status --short` shows those two files and this report.
- Gradle: not run. I did not re-run the auditor's cache-hit reproduction; it is cited from their report (Checks, the `1000`/`1000`/`2000`/`100000` line).

### Open issues / risks
- `--rerun-tasks` also re-runs `:domain:test`'s wall-clock deadline tests under kover. The load-sensitivity open issue from pass 1, a tech-lead ruling, applies unchanged. A run on a quiet host is still the producer's scheduling call.

### Handoff
Next: producer. Commit `THIRD_PARTY.md`, `docs/release-checklist.md` and this report. The code-auditor can confirm that findings 1 and 2 are closed from this diff.

## Pass 3 — qa-engineer open issues 1, 2 and 5 (`.planning/reports/phase-14/qa-engineer-check-gates.md`)
Date: 2026-10-24. Branch: feature/phase-14-check-gates (head b41fcb3; working-tree edit, committed by the producer). Loops: 2. Only `docs/release-checklist.md` was edited, in rows 3.1 to 3.4 alone (`git diff -U0` shows one hunk, `@@ -69,4 +69,4 @@`), so it does not collide with `feature/tech-design-sync`.

### Summary
- **3.1:** the command is now `mkdir -p build; FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks --console=plain > build/release-check.log 2>&1; echo "exit $?"` → `exit 0`.
  - The plain console in a file keeps one `> Task …` line per executed task (QA open issue 5).
  - `build/` is git-ignored by `**/build/`, and the `mkdir` covers a fresh clone.
  - The FROM-CACHE refusal and the fuzz evidence now read from that log.
- **3.2:** now states that the check is sound only because 3.1 forces `--rerun-tasks`.
  - A `FROM-CACHE` green `:domain:fuzz` leaves an earlier red run's `fuzz-failures.txt` in place (QA open issue 2).
  - 3.2 is read only after a 3.1 log showing `> Task :domain:fuzz` executed, and is not ticked if 3.1 ran without the flag.
- **3.3:** the gate is still `:domain:koverVerify` inside `check`, shown as executed in the 3.1 log. The figure now comes from `./gradlew :domain:koverXmlReport --offline` run after 3.1.
  - It is read from the root LINE counter of `domain/build/reports/kover/report.xml`, and that file's mtime must be later than the 3.1 run.
  - The row states why: `koverVerify` writes only `verify.err` (QA open issue 1).
  - The 99.0 % (896/905) baseline at bound 90 is unchanged.
- **3.4:** the evidence is now a grep of the 3.1 log for `> Task :(app|domain):(adIdLiteralCheck|gamesIdLiteralCheck|audioFocusLiteralCheck|dpSpLiteralCheck)`. It must print all eight lines, none of them `FROM-CACHE` or `UP-TO-DATE`, and the count is recorded.

### Checks
- `grep -rhoE '"(adId|gamesId|audioFocus|dpSp)LiteralCheck"' build-logic app domain | sort -u`: pass. The four task names in 3.4 exist.
- `git check-ignore -v build/release-check.log`: pass (`.gitignore:3:**/build/`).
- Proofs, each `grep -n -o` on `docs/release-checklist.md`:
  - `--rerun-tasks --console=plain > build/release-check.log 2>&1` and "`--console=plain` and the file are required": line 69.
  - "This check is sound only because 3.1 forces `--rerun-tasks`": line 70.
  - `./gradlew :domain:koverXmlReport --offline` and "writes only `domain/build/reports/kover/verify.err`": line 71.
  - "must print all eight lines": line 72.
- Table integrity: pass. Rows 3.1 to 3.4 have 4 cells each, counted with an awk split that skips `\|`. The pipes inside 3.4's grep are escaped as `\|`, which is the file's own convention (see 3.6). Copied from the raw markdown, they must be unescaped.
- `git diff --stat`: 1 file, +4/−4. `git status --short`: that file and this report.
- Gradle: not run. The `verify.err`/`report.xml` behaviour and the stale-file behaviour are cited from the QA's evidence (Checks: the `koverXmlReport` mtime line; `mutant-reverted-fuzz.log`), not reproduced here.

### Open issues / risks
- Item 1.2 (on `develop`) keeps `--rerun-tasks` without `--console=plain` or a log file. Its evidence, the wall-clock time and the seed count, prints either way, and the dispatch limited this pass to 3.1–3.4. If the producer wants 1.2 to match, it is a one-line follow-up after `feature/tech-design-sync` lands.
- QA's clean fix for the stale failures file belongs to the android-game-dev's backlog (declare the file as a task output, or delete it in `doFirst`). Until then, 3.2 stays coupled to 3.1's flag, as written.

### Handoff
Next: producer. Commit `docs/release-checklist.md` and this report. The qa-engineer can confirm that open issues 1, 2 (the checklist side) and 5 are closed from this diff.
