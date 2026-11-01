# code-auditor report — phase 14 signing configuration
Date: 2026-09-21
Branch: feature/phase-14-signing-config (fb7889b..5aadd07)
Skills loaded: ergoz-soft:standards (role code-auditor) — rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md (plugin copies; repo snapshot `.claude/rules/VERSION` 0.5.1 < plugin 0.5.2); code-reviewer; security-reviewer. compose-skill / compose-performance-audit not applicable (no Compose or app source in the diff).
Ran in parallel with: phase-14 release-plan track, and a third worktree not identified to me
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` carries 0 active lessons)


Verdict: PASS — no credential can leak by any path found or constructed; no blocking finding.

> Saved by the producer from the code-auditor's final message.

## Summary
The diff adds the project's first release signing configuration and it is sound. Credentials come from the environment only, never reach a file, a log, a cache, a task input or the artifact; the three gates (configuration-time resolve, `verifyReleaseSigning` hung off the *package* tasks, signing config created only when all four resolved) hold on every path I could construct, including `bundleRelease` invoked directly. I re-derived the APK proof with my own throwaway key rather than accepting the developer's. Five findings, none blocking.

## Findings

**F-1 — Medium — `app/build.gradle.kts:179` (via the `isReleaseInvocation` heuristic at `app/build.gradle.kts:88-92`).**
`contains("elease")` is a false positive for release-named tasks that package nothing. Verified: `./gradlew -m :app:lintRelease` with no variables exported fails with `Release build requires the Play upload signing credentials.` `standards.gradle.kts:54-55` already documents that `detektRelease` trips the same gate. Before this diff that cost an operator two non-secret ad IDs; now it costs two cryptographic passwords, so the pressure is to `export` them in an everyday shell (rc file, history, an env dump in a bug report) rather than only in the build shell — a dilution of the very property the design is built on. Impact is security-usability, not correctness; the gate fails closed, and `./gradlew check` is unaffected (verified BUILD SUCCESSFUL with nothing exported). Remediation (one line, non-blocking): for signing only, drop the configuration-time throw and rely on `verifyReleaseSigning`, which is already wired to `packageRelease`/`packageReleaseBundle` and is strictly tighter — or narrow the throw to names that actually package. Does not block merge.

**F-2 — Low — `build-logic/src/main/kotlin/SigningKeystoreProbe.kt:38-43`.**
The "every message names a variable, never a value" property holds for every message the diff adds, but the operator's most common mistake — a mistyped path — is caught first by AGP's own task, not by the probe. Observed on a full `:app:assembleRelease` with `PLAY_UPLOAD_KEYSTORE` pointing at a non-existent file: `> Task :app:validateSigningRelease FAILED` … `Keystore file '/…/absent-upload.jks' not found for signing config 'release'.` — the absolute path, printed by AGP. A filesystem path is not a cryptographic secret, no artifact is produced, and the build still fails before packaging, so this is informational; but the probe's own missing-file branch is effectively unreachable on `assembleRelease` and the report's blanket claim should be read as scoped to the code it added.

**F-3 — Low — `.planning/reports/phase-14/android-game-dev-signing-config.md:187-191`.**
The developer's proof APK and AAB carry the founder's real ad and Play Games IDs with a throwaway signature, and the report states they were left under `app/build/`. That is precisely the artifact that gets uploaded by accident. As of this audit both are gone: I deleted `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/bundle/release/app-release.aab` and `app/build/intermediates/intermediary_bundle/release/packageReleaseBundle/intermediary-bundle.aab` (mine and any predecessor); `find` over the worktree now returns only `app/build/outputs/apk/debug/app-debug.apk`. No `*.jks`, `*.keystore` or `keystore.properties` exists anywhere in the worktree, and none was found under `/private/tmp/claude-501`, `/tmp` or `~/Desktop` (mtime ≤ 3 days). My own throwaway key and its scratchpad directory are deleted. Remediation: deleting throwaway-signed artifacts belongs in the task's definition of done, not in a risk note.

**F-4 — Info — `build-logic/src/main/kotlin/SigningKeystoreProbe.kt:36`.**
Nothing pins the certificate. Four variables naming a *valid but wrong* keystore produce a successful, signed, non-uploadable artifact; the mistake surfaces at Play, not at build. This matches the specification — `14-OPERATOR-CHECKLIST.md` Step 7 item 4 makes the SHA-1 comparison a manual operator step — so it is not a defect in the diff. If the studio later wants it closed in-build, an optional `PLAY_UPLOAD_CERT_SHA256` compared inside `verifyReleaseSigning` is the natural place, and the probe already holds the certificate.

**F-5 — Info — `build-logic/build.gradle.kts` (no `standards` plugin).**
The `standards` convention plugin is applied to `:app` and `:domain` only, so the two new production files are not seen by ktlint or detekt. Pre-existing (it is equally true of `AdIdResolution.kt`), style is consistent with the siblings, and the files carry 15 discriminating tests. Noted so it is not mistaken for coverage.

**Approved suppressions: none requested, none found.** `git diff develop..HEAD | grep -E '^\+.*(@Suppress|@Ignore|baseline|eslint-disable)'` returns only two lines of the developer's own report prose. No test disabled, no baseline entry, no `!!`, no new dependency (`gradle/libs.versions.toml` untouched), therefore no `THIRD_PARTY.md` row due — that file does not exist and its creation is owned by the release plan §5.

## Judgements requested

**Consistency vs. drift (item 3).** `SigningResolution` is a genuine third sibling, not a parallel mechanism: same directory, same `Map`-in/result-out Gradle-free shape, same sealed-result vocabulary, same test location and the same reason for it (no `testReleaseUnitTest` under AGP 9.1.0), same fail-fast register. Its four divergences — environment-only source, no trimming, redacted `toString`, absolute-path requirement — are each documented on the type and each pinned by a named test, so they cannot drift silently. The one asymmetry worth acting on runs the other way: `verifyReleaseAdIds` / `verifyReleaseGamesIds` still hang off the lifecycle task only, and my dry run of `:app:bundleRelease` lists them *after* `packageReleaseBundle` while `verifyReleaseSigning` lists immediately before it. Recommend a follow-up task moving the two ID hooks onto the package tasks; the developer correctly did not do it here.

**Can a debug build be made to require these variables?** No. Verified `-m :app:assembleDebug`, `-m check` and `-m :app:lintDebug` all configure with nothing exported. `releaseSigning == null` yields no `signingConfigs` entry and `signingConfig = signingConfigs.findByName("release")` yields null, reproducing the pre-phase-14 unsigned behaviour exactly. There is no debug-keystore fallback anywhere in the diff.

**Stale configuration reuse.** Cannot happen: configuration cache is off (`gradle.properties`), and I proved it empirically — configuring `:app:assembleRelease` successfully with the four variables and then re-running in the same daemon with them unset fails at configuration rather than reusing the previous `signingConfigs` entry. When configuration cache is eventually enabled, note that the captured `signingEnv` map would enter the cache entry; Gradle encrypts the configuration cache at rest by default since 8.1, so residual risk is low, but this should be re-checked at that switch-on.

## Checks
- `./gradlew --max-workers=2 -m :app:assembleDebug` (no `PLAY_UPLOAD_*`) — pass (BUILD SUCCESSFUL, 9 tasks)
- `./gradlew --max-workers=2 -m check` (no `PLAY_UPLOAD_*`) — pass (BUILD SUCCESSFUL; the Check command is unaffected by this change)
- `./gradlew --max-workers=2 -m :app:assembleRelease` (no `PLAY_UPLOAD_*`) — fails as designed at `app/build.gradle.kts line: 179`, naming all four variables, before any task ran
- `./gradlew --max-workers=2 -m :app:lintRelease` (no `PLAY_UPLOAD_*`) — **fails** with the signing message (F-1)
- `./gradlew --max-workers=2 -m :app:{installRelease,assembleRelease,bundle,assemble}` — pass; `:app:verifyReleaseSigning` precedes `packageRelease` / `packageReleaseBundle` in all four graphs
- `PLAY_UPLOAD_*=<my own throwaway JKS> ./gradlew --max-workers=2 :app:assembleRelease` — pass (BUILD SUCCESSFUL; `app-release.apk`, not `-unsigned`)
- `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk` — pass, independently re-derived: `Verifies`; v1 false, **v2 true**, v3 false, v3.1 false, v4 false; `Number of signers: 1`; `Signer #1 certificate DN: CN=Audit Throwaway, OU=audit, O=audit, L=x, ST=x, C=TR`; SHA-256 `5f99f7b09b303e60c9cc123f5d6e85fd459786b929551f19794b71c86baf864c`; RSA 2048
- `./gradlew --max-workers=2 :app:bundleRelease` then `jarsigner -verify -certs …/app-release.aab` — pass (`jar verified.`)
- `grep -rl '<throwaway store pw>|<throwaway key pw>|<throwaway alias>'` over the entire worktree incl. `app/build/`, plus `~/.gradle/daemon`, `~/.gradle/caches/build-cache-1`, `~/.gradle/workers`, `.gradle/`, `.kotlin/` — **0 hits**
- `./gradlew --max-workers=2 --debug :app:packageRelease` — 73,543 log lines; **0** occurrences of either password, the alias, or the keystore path
- `:app:verifyReleaseSigning` with each credential wrong in turn — pass (four distinct messages: `names a file that does not exist`, `The keystore could not be opened (IOException)` → `PLAY_UPLOAD_STORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS is not an alias in this keystore`, `UnrecoverableKeyException` → `PLAY_UPLOAD_KEY_PASSWORD`; plus the configuration-time relative-path message). Each ends `No unsigned or partially signed artifact was produced.` No value in any of them.
- `--stacktrace :app:assembleRelease` with a wrong store password — fails at `:app:verifyReleaseSigning`; `Caused by: org.gradle.api.GradleException` carries the same redacted text with no underlying cause chain; `ls app/build/outputs/apk/release/` afterwards shows **no APK**
- `:app:assembleRelease` with a non-existent keystore path — fails at AGP's `validateSigningRelease`, printing the path (F-2); no artifact
- `./gradlew --max-workers=2 -p build-logic test` — pass (`SigningResolutionTest` tests=9 failures=0, `SigningKeystoreProbeTest` tests=6 failures=0)
- Mutation testing, 3 mutants, all killed, working tree byte-identical afterwards: trim in `present()` → `passwordWhitespaceIsPreservedVerbatim` FAILED; remove the `toString` override → `theResolvedCredentialsRedactThemselvesWhenPrinted` FAILED; disable the `containsAlias` guard → `saysTheAliasIsAbsentWhenTheKeystoreHasNoSuchAlias` FAILED
- `./gradlew --max-workers=2 :app:ktlintCheck :app:detekt` — pass; `:app:ktlintKotlinScriptCheck` confirmed present, so `app/build.gradle.kts` is linted
- `.gitignore:24-27` — `*.jks`, `*.keystore`, `keystore.properties` present and effective; `**/build/`, `*.apk`, `*.aab` also ignored
- `git status --short` — clean (before, during and after the audit); nothing committed, no source edited
- Full `./gradlew check` and the fuzz run — **not run here** (the producer owns it, per dispatch)

## Open issues / risks
1. F-1 and the ad/PGS hook asymmetry are both one-line follow-ups; neither belongs to this task.
2. `CLAUDE.md`'s Build command still names `:app:assembleRelease`; the release plan §4a puts the change to `:app:bundleRelease` on the release branch. Correctly out of scope here.
3. `app/build.gradle.kts:273` sets `versionCode = 3` while the release plan §6 records an open founder option for `versionCode 30000` under the studio formula. Pre-existing, release-manager's call, not this diff.

## Handoff
Next: tech-lead — PASS, mergeable as-is. F-1 (narrow the signing gate off `contains("elease")`) and moving `verifyReleaseAdIds` / `verifyReleaseGamesIds` onto the package tasks are recommended as one follow-up task, not a re-dispatch of this one. The `enableV3Signing` question is ruled and closed: leave the default, reasoning recorded above.
