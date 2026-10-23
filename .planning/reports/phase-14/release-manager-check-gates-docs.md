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
