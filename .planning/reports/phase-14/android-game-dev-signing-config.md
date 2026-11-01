# android-game-dev report — phase-14 release signing configuration
Date: 2026-09-21
Branch: feature/phase-14-signing-config (fb7889b..bb91b6a)
Skills loaded: ergoz-soft:standards (role android-game-dev) — rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, kotlin-android.md, play-store.md, testing.md (plugin copies; the repo snapshot `.claude/rules/VERSION` is 0.5.1 against plugin 0.5.2, so the plugin copies were read per the loader's staleness rule)
Ran in parallel with: phase-14 release-plan track (`feature/phase-14-release-plan`), and a third worktree the producer named but did not identify to me
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` carries 0 active lessons)

## Summary
The build now signs release packages with the Play upload key, read from the four
`PLAY_UPLOAD_*` environment variables and from nowhere else. Three gates stand between a
release invocation and an artifact: a configuration-time resolver that names the missing
variable, a `verifyReleaseSigning` task that opens the keystore before the package task runs,
and a signing config that exists only when all four resolved — so the release variant is either
signed with the upload key or unsigned as before, with no third state. Proven end to end with a
throwaway keystore generated outside the repo and deleted afterwards; the founder's real
keystore and passwords were never involved.

## Outputs
- `build-logic/src/main/kotlin/SigningResolution.kt` — `SigningKeys` (the four variable names),
  `SigningResolutionResult`, and the Gradle-free resolver. Environment-only by design: no
  `local.properties` source, no placeholder prefix, and nothing is trimmed (whitespace inside a
  password is a character of the password). `Resolved.toString()` is overridden to redact.
- `build-logic/src/main/kotlin/SigningKeystoreProbe.kt` — opens the keystore the way the signer
  will and distinguishes missing file / unopenable store / absent alias / unrecoverable key /
  certificate-only entry. Format-agnostic (`KeyStore.getInstance(File, char[])` probes).
- `build-logic/src/test/kotlin/SigningResolutionTest.kt` — 9 tests.
- `build-logic/src/test/kotlin/SigningKeystoreProbeTest.kt` — 6 tests, fixture keystore built
  in-process from JDK APIs in the JUnit temporary folder (no `keytool` subprocess, no committed
  binary).
- `app/build.gradle.kts` — `signingEnv` / `releaseSigning` resolution beside the existing ad and
  Play Games blocks; `verifyReleaseSigning`; `signingConfigs { create("release") }` created only
  when all four resolved; `release { signingConfig = signingConfigs.findByName("release") }`;
  `import java.io.File`.
- `local.properties.template` — a closing block naming the four variables with **nothing to
  copy** (no `=` on any line), stating that these are the one set of values that must never be
  in a file, plus a pointer to it from the header.

No new dependency, so no `THIRD_PARTY.md` row is due — and that file does not exist in this
worktree (the release plan §5 already owns creating it).

## How it is wired

Configuration time, reusing the file's existing `isReleaseInvocation` heuristic verbatim:

    val releaseSigning: SigningResolutionResult.Resolved? =
        when (val resolved = SigningResolution.resolve(signingEnv)) {
            is SigningResolutionResult.Resolved -> resolved
            is SigningResolutionResult.Failure -> {
                if (isReleaseInvocation) throw GradleException(resolved.message)
                null
            }
        }

`null` reproduces the pre-phase-14 state exactly (no signing config, unsigned APK), so a debug
build, `check`, `test`, IDE sync and a fresh clone are unaffected. There is deliberately no
fallback to the debug keystore — the one substitution D-16 exists to forbid.

Execution time, `verifyReleaseSigning` is a dependency of **both** `assembleRelease`/
`bundleRelease` (guarantees it runs) and `packageRelease`/`packageReleaseBundle` (guarantees it
runs *first*: a dependency of the lifecycle task is only a sibling of the package task and
Gradle may order the two either way — the same latent gap the existing `verifyReleaseAdIds`
hook has, which I did not change).

Secrets: every failure message names a **variable**, never a value, including the keystore path.
A Gradle log gets pasted into issues and reports; two of the 15 tests assert directly that no
message contains a credential value, and one asserts `Resolved.toString()` redacts.

## Checks
- `./gradlew --max-workers=2 -p build-logic test` — **pass** (15 new tests, 0 failures:
  `TEST-SigningResolutionTest.xml` tests="9" failures="0", `TEST-SigningKeystoreProbeTest.xml`
  tests="6" failures="0"). Written first and confirmed red — `Unresolved reference 'SigningKeys'`,
  `:compileTestKotlin` FAILED — before the implementation existed.
- `keytool -genkeypair -v -keystore <scratchpad>/throwaway-upload.jks -storetype JKS -alias
  throwaway-upload -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Throwaway Signing Proof, …"`
  — **pass**. Generated in the session scratchpad, never in the repo or the worktree; deleted
  afterwards (see "Cleanup").
- `env PLAY_UPLOAD_KEYSTORE=… PLAY_UPLOAD_KEY_ALIAS=… PLAY_UPLOAD_KEY_PASSWORD=…
  PLAY_UPLOAD_STORE_PASSWORD=… ./gradlew --max-workers=2 :app:assembleRelease` — **pass**
  (BUILD SUCCESSFUL in 3m 2s; ad + Play Games IDs came from the worktree's existing untracked
  `local.properties`). Output is `app/build/outputs/apk/release/**app-release.apk**` — note the
  name: no longer `app-release-unsigned.apk`.
- `apksigner verify --print-certs --verbose app/build/outputs/apk/release/app-release.apk` —
  **pass**:

      Verifies
      Verified using v1 scheme (JAR signing): false
      Verified using v2 scheme (APK Signature Scheme v2): true
      Verified using v3 scheme (APK Signature Scheme v3): false
      Verified using v3.1 scheme (APK Signature Scheme v3.1): false
      Verified using v4 scheme (APK Signature Scheme v4): false
      Verified for SourceStamp: false
      Number of signers: 1
      Signer #1 certificate DN: CN=Throwaway Signing Proof, OU=android-game-dev, O=ergoz-soft-scratch, C=TR
      Signer #1 certificate SHA-256 digest: 44df12c6206a2e884722e52ab00563d77cd68a8ee053284900c9b560b77e27a2
      Signer #1 certificate SHA-1 digest: 2269d5d8a927c6374c1a33e4549d6eeffd1b444d
      Signer #1 key algorithm: RSA
      Signer #1 key size (bits): 2048

- `./gradlew --max-workers=2 :app:bundleRelease` with the same four variables, then
  `jarsigner -verify -verbose:summary -certs app/build/outputs/bundle/release/app-release.aab`
  — **pass** (the AAB is the artifact §4b of the release plan actually uploads):

      X.509, CN=Throwaway Signing Proof, OU=android-game-dev, O=ergoz-soft-scratch, C=TR
      - Signed by "CN=Throwaway Signing Proof, OU=android-game-dev, O=ergoz-soft-scratch, C=TR"
      jar verified.

- Negative case 1 — `env -u PLAY_UPLOAD_KEYSTORE -u PLAY_UPLOAD_KEY_ALIAS
  -u PLAY_UPLOAD_KEY_PASSWORD -u PLAY_UPLOAD_STORE_PASSWORD ./gradlew --max-workers=2
  :app:assembleRelease` — **fails as designed**, exit 1, at configuration
  (`app/build.gradle.kts line: 179`), before any task ran:

      Release build requires the Play upload signing credentials.
      Missing: PLAY_UPLOAD_KEYSTORE
        export PLAY_UPLOAD_KEYSTORE=… in the shell that runs the build.
      Missing: PLAY_UPLOAD_KEY_ALIAS
      …
      These four are cryptographic secrets: environment variables only — never local.properties,
      never a committed file, never a Gradle property.
      Debug builds are signed with the local debug key and need none of this.

- Negative case 2 — `env -u PLAY_UPLOAD_KEYSTORE -u PLAY_UPLOAD_KEY_ALIAS
  -u PLAY_UPLOAD_KEY_PASSWORD -u PLAY_UPLOAD_STORE_PASSWORD ./gradlew --max-workers=2
  :app:assembleDebug` — **pass**, exit 0 (BUILD SUCCESSFUL in 1m 19s). Debug needs none of the
  four. `:app:tasks --group=verification` also configures cleanly with all four unset.
- Bad-credential cases, each `./gradlew --max-workers=2 :app:verifyReleaseSigning` with one
  variable deliberately wrong — all four **fail with their own message**, and none prints a
  value:
  - relative path → `PLAY_UPLOAD_KEYSTORE must be an absolute path.` (configuration time)
  - path to a non-existent file → `PLAY_UPLOAD_KEYSTORE names a file that does not exist (or is
    not a file).`
  - wrong alias → `PLAY_UPLOAD_KEY_ALIAS is not an alias in this keystore.` + `List the aliases
    with: keytool -list -keystore "$PLAY_UPLOAD_KEYSTORE"`
  - wrong key password → `The key under PLAY_UPLOAD_KEY_ALIAS could not be opened
    (UnrecoverableKeyException).` + `PLAY_UPLOAD_KEY_PASSWORD is the usual cause.`
  - wrong store password → `The keystore could not be opened (IOException).` +
    `PLAY_UPLOAD_STORE_PASSWORD is the usual cause; a file that is not a keystore is the other.`
  Each ends `No unsigned or partially signed artifact was produced.` With all four correct the
  task succeeds in 644 ms.
- `./gradlew --max-workers=2 detekt ktlintCheck` (signing variables unset) — **pass**
  (BUILD SUCCESSFUL in 33s). `./gradlew --max-workers=2 :app:ktlintKotlinScriptCheck
  --rerun-tasks` — **pass**, confirming `app/build.gradle.kts` itself is linted rather than
  silently skipped. No `@Suppress`, no baseline, no `@Ignore` added anywhere.
- `git status --porcelain` — **clean**, no output, nothing untracked left behind.
- `.gitignore` confirmed to already block the keystore, as Step 7 item 2 claims — lines 24-27
  under `# Signing — never commit release keystores or signing secrets`: `*.jks`, `*.keystore`,
  `keystore.properties`. `find … -name '*.jks' -o -name '*.keystore' -o -name
  'keystore.properties'` over the whole worktree returns nothing.
- Secret-shape grep over the diff — `git diff develop | grep -cE
  'throwaway-upload|throwaway-store-pw|throwaway-key-pw|/Users/|\.jks$|BEGIN [A-Z ]*PRIVATE KEY'`
  → **1**, and that one line is
  `+#   export PLAY_UPLOAD_KEYSTORE=/absolute/path/outside/this/repo/upload.jks` in the template
  — a documentation placeholder, no real path. No throwaway credential appears anywhere in the
  diff. `git grep` over the tracked tree finds the two password variable names only in the
  template's documentation table.
- Secret-shape grep over the **build output** — `grep -rl 'throwaway' app/build/` returns
  nothing, and the only signing artefact AGP 9.1.0 writes there is
  `intermediates/signing_config_versions/release/.../signing-config-versions.json`, whose entire
  content is `{"enableV1Signing":false,"enableV2Signing":true,"enableV3Signing":false,
  "enableV4Signing":false}` — scheme flags, no credentials. Worth recording: older AGP lines did
  write signing config *data* into intermediates, and this one does not.

## Cleanup
The throwaway keystore was generated only in the session scratchpad
(`…/scratchpad/throwaway-upload.jks`), never in the repository or the worktree, and was deleted
after the proof: `rm -fv <scratchpad>/throwaway-upload.jks` → removed, and a follow-up `test -e`
prints `deleted`. Its passwords were invented for this session, are recorded nowhere, and are
useless — the key was issued with `-validity 1`.

## Open issues / risks
1. **`enableV3Signing` is `false`.** AGP's default here signs the APK with v2 only. It does not
   affect the release path (Play App Signing re-signs the APK it generates, and the uploaded AAB
   is JAR-signed, which `jarsigner` verified above), so I changed nothing. Flagging it because it
   is the kind of default a reader would expect to be `true`; if the tech-lead wants key-rotation
   support later it is a one-line addition to the signing config.
2. **The `isReleaseInvocation` task-name heuristic is inherited, not improved.** I deliberately
   reused it rather than inventing a second gate. Its one gap — an invocation form the name test
   misses — is covered for signing by `verifyReleaseSigning` hanging off the package tasks, which
   is strictly tighter than the existing ad-ID hook. If the auditor wants the ad/Play-Games hooks
   moved to the package tasks too, that is a separate one-line change I did not make because it
   is not this task.
3. **`CLAUDE.md`'s Build command still says `:app:assembleRelease`.** The release plan §4a puts
   the change to `:app:bundleRelease` on the release branch, so I left it alone. Both are now
   proven signed, so either works.
4. **Fuzz / full `check` not run** — the producer runs it, per the dispatch. `detekt`,
   `ktlintCheck`, `:app:ktlintKotlinScriptCheck` and `build-logic:test` were run here.
5. **Nothing was done with the founder's real keystore or AdMob credentials.** The release APK
   and AAB built above carry the real ad and Play Games IDs from the untracked
   `local.properties` but a throwaway signature — per `14-RELEASE-PLAN.md` §4c they are *not*
   release candidates and must not be uploaded. Both live only under the gitignored
   `app/build/`.

## Handoff
Next: code-auditor — the diff is two commits on `feature/phase-14-signing-config`
(`fb7889b`, `bb91b6a`) touching `build-logic/src/{main,test}/kotlin/Signing*.kt`,
`app/build.gradle.kts` and `local.properties.template`. Points worth their attention: the
deliberate divergences from `AdIdResolution` (environment-only source, no trimming, redacted
`toString`), the package-task hook, and whether risk 1 (`enableV3Signing`) should be closed here
or deferred.
Then: qa-engineer — the failure-message register is the user-facing surface of this change; the
five bad-credential messages above are reproducible with any throwaway keystore and need no real
credential.
Then: producer — runs the real signed build with the founder's keystore per
`14-OPERATOR-CHECKLIST.md` Step 7; the four variables are the only interface, and the build will
say precisely which one is wrong if any is.
