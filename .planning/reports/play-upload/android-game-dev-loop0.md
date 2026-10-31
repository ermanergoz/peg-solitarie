# android-game-dev report — play-upload
Date: 2026-10-24
Branch: feature/play-upload (9005fe3..9005fe3: one work commit, off develop f7dd1fe)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing and stacks, read in full, and `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow (its commit-style rules overridden by rules/gitflow.md)
Ran in parallel with: home-chip-arrival code-auditor loop 2, colour-vision QA, licences-screen QA re-run, talkback-board QA
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- `scripts/play-upload.sh` (`9005fe3`, mode 100755) runs `fastlane supply` with `--json_key "$PLAY_SERVICE_ACCOUNT_JSON"`, uploading `app/build/outputs/bundle/release/app-release.aab`, which `:app:bundleRelease` writes, to the `internal` track of `com.erman.pegsolitarie`. It checks no approval, builds nothing and uploads no listing text, image, screenshot or release note.
- It stops before anything is sent, with exit 78, on an argument, an unset, relative or unreadable key path, a missing or empty bundle, or a missing fastlane. It never prints the key's path or reads the key file, and it unsets every `SUPPLY_*` variable so that only its own options reach fastlane.
- `shellcheck scripts/play-upload.sh` (0.11.0, installed today with Homebrew) prints nothing and exits 0. `bash -n` (3.2.57) is clean. The Check (`./scripts/check.sh --max-workers=4`) is green: BUILD SUCCESSFUL in 11 m 18 s, 1,576 tests with 0 failed or skipped, and the fuzz at 10,000 levels with 0 failures.
- **The failure paths were not run.** The publish gate blocks all seven ways I would have run the script here, the two failure-path forms among them (probe below). Any other route is one the hook cannot see, and row PU says nothing routes around it. So, as the dispatch says for doubt, the script was tested with `bash -n` and shellcheck only, and each path is given below from the source.

## Outputs
- `scripts/play-upload.sh` — the upload script, 93 lines, sha256 `35323283ffe33d7afeee5071d93c8d0c94b97a061200c177b55b377dcddf9abd` (commit `9005fe3`).
- `.planning/reports/play-upload/android-game-dev.md` — this report.

### The script, in full, as committed in `9005fe3`

```bash
#!/usr/bin/env bash
# Uploads the signed release bundle in this checkout to the Google Play internal track with
# `fastlane supply` (rules/play-store.md; docs/tech-design.md §14.1 PU).
#
# It checks no approval itself. The gate is the ergoz-soft plugin hook hooks/publish-gate.sh: it
# blocks any command that runs this script, or `fastlane supply`, unless
# .planning/approvals/publish-<VERSION>.approved exists in the repository that holds the script
# (plugin rules/stacks.md, scaffold contract 9). Run it after Gate 2 only, from the checkout the
# release procedure names (feature/publish-<X.Y.Z>-done), and never by a route the hook cannot see.
#
# It builds nothing. It uploads the bundle that `./gradlew :app:bundleRelease` built from the
# release (docs/release-checklist.md §4); that build refuses to package a release without the four
# PLAY_UPLOAD_* signing variables (docs/tech-design.md §9.2), so its bundle carries the upload key.
# It uploads nothing else: the listing text, images and screenshots are what production players
# see, and the founder enters them and the release notes in the Play Console (docs/store/).
#
# Usage: ./scripts/play-upload.sh   (no arguments)
# Environment:
#   PLAY_SERVICE_ACCOUNT_JSON  the absolute path of the Play Console service-account JSON key, kept
#                              outside the repository. The script hands the path to fastlane; it
#                              never prints the path and never reads the file. fastlane's own
#                              summary table does print the path, not the key's content.
#   Every SUPPLY_* variable is unset before fastlane runs, so no supply option comes from the
#   caller's shell.
# Exit status: fastlane's; 78 (EX_CONFIG) when a check below fails, before anything is sent.
#
# Bash 3.2 compatible: macOS ships 3.2.
set -euo pipefail

# The publish gate judges a script named by its path by the repository that holds it, so this
# script uploads that repository's bundle wherever the caller stands.
cd "$(dirname "$0")/.."

readonly PACKAGE_NAME="com.erman.pegsolitarie" # applicationId in app/build.gradle.kts
readonly TRACK="internal"
readonly RELEASE_STATUS="completed" # the internal testers can install it at once
readonly AAB="app/build/outputs/bundle/release/app-release.aab" # what :app:bundleRelease writes
readonly EXIT_PRECONDITION=78 # EX_CONFIG (sysexits.h): nothing was sent

# Prints its arguments as one message, then stops before anything is sent.
fail() {
    echo "play-upload.sh: $*" >&2
    exit "$EXIT_PRECONDITION"
}

if [ "$#" -ne 0 ]; then
    fail "takes no arguments. Usage: ./scripts/play-upload.sh"
fi

key="${PLAY_SERVICE_ACCOUNT_JSON:-}"
if [ -z "$key" ]; then
    fail "PLAY_SERVICE_ACCOUNT_JSON is not set." \
        "Export it as the absolute path of the Play service-account JSON key."
fi
case "$key" in
    /*) ;;
    *)
        fail "PLAY_SERVICE_ACCOUNT_JSON is not an absolute path." \
            "This script runs from the repository root, so give the key's absolute path."
        ;;
esac
if [ ! -f "$key" ] || [ ! -r "$key" ]; then
    fail "PLAY_SERVICE_ACCOUNT_JSON does not name a readable file."
fi
if [ ! -f "$AAB" ] || [ ! -s "$AAB" ]; then
    fail "no bundle at $AAB. This script builds nothing: put the release's signed bundle there," \
        "built by ./gradlew :app:bundleRelease from the release (docs/release-checklist.md §4)."
fi
if ! command -v fastlane >/dev/null 2>&1; then
    fail "fastlane is not on PATH. Install it (brew install fastlane) and run this again."
fi

# fastlane reads every supply option it is not given from a SUPPLY_* variable: a SUPPLY_ROOT_URL
# left in the shell would send the authorised upload to another host, and a SUPPLY_ROLLOUT would
# stage it. Only the options below decide what is sent.
for name in $(compgen -e); do
    case "$name" in
        SUPPLY_*) unset -v "$name" ;;
    esac
done

echo "play-upload.sh: uploading $AAB to the $TRACK track of $PACKAGE_NAME"
fastlane supply \
    --package_name "$PACKAGE_NAME" \
    --track "$TRACK" \
    --release_status "$RELEASE_STATUS" \
    --aab "$AAB" \
    --json_key "$key" \
    --skip_upload_apk true \
    --skip_upload_metadata true \
    --skip_upload_changelogs true \
    --skip_upload_images true \
    --skip_upload_screenshots true
```

### Row PU, requirement by requirement

| Requirement (tech design §14.1 PU, dispatch) | Where |
|---|---|
| Runs `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON` | `:83-88`. The variable is read at `:50`. It is a key-file path, as the studio's earlier Fastfile had it (`json_key_file(ENV["PLAY_SERVICE_ACCOUNT_JSON"])`). |
| Uploads the signed AAB of `:app:bundleRelease` | `:37`, `:87`. AGP's default name: the app sets no `archivesName`, and the release plan 4b and checklist 4.7 use the same path. Signed, because `verifyReleaseSigning` is a `dependsOn` of `packageReleaseBundle` (`app/build.gradle.kts:255-257`; §9.2.5). |
| To the `internal` track | `:35`, `:85`. This flag is load-bearing: supply's default track is production (`Supply::Tracks::DEFAULT = PRODUCTION`, `supply.rb:37` in fastlane 2.233.0). |
| Checks no approval itself | It reads neither `VERSION` nor `.planning/approvals/`. The header says the hook is the gate. `:32` is the `cd "$(dirname "$0")/.."` that `hooks/lib-cmdpos.sh:118` relies on to judge the script by the repository that holds it. |
| A clear message when the variable or the AAB is missing | `:51-53`, `:65-68`, plus the relative-path, unreadable-file, argument and fastlane checks. All go to stderr with the `play-upload.sh:` prefix and exit 78 (table below). |
| Echoes no secret | No message names the key's value, and the script never opens the key file. It has no `set -x`, and its one stdout line (`:82`) names the bundle, the track and the package. fastlane's summary table prints the `json_key` path, because it masks only `json_key_data` (`supply/uploader.rb:9`). The path is not the key (open issue 5). |
| `shellcheck scripts/play-upload.sh` clean | Checks. |

Choices the row leaves open, made here:
- **`--release_status completed`**: supply's default, pinned so that the internal testers can install the build at once (release plan §5 verifies from that download).
- **The `--skip_upload_*` flags**:
  - `apk`: supply's `apk` default is `Dir["*.apk"].last`, so a stray APK in the repository root would join the upload or fail it (`verify_config!`).
  - `metadata`, `changelogs`, `images` and `screenshots`: the metadata default is `./fastlane/metadata/android` or `./metadata` if either exists, and an internal upload must never edit the listing production players see.
- **`SUPPLY_*` unset**: supply reads each option it is not given from its `SUPPLY_*` variable. `SUPPLY_ROOT_URL` "will be used for API calls in place of https://www.googleapis.com/" (`options.rb:261-266`), which would send the authorised upload elsewhere.
- **An absolute key path is required**, because the script `cd`s to the repository root first, and a relative path would then resolve against the root, not against the caller.

### Failure paths, from the source (none executed)

| # | Trigger | stderr, exactly | Exit | Lines |
|---|---|---|---|---|
| 1 | any argument | `play-upload.sh: takes no arguments. Usage: ./scripts/play-upload.sh` | 78 | `:46-48` |
| 2 | `PLAY_SERVICE_ACCOUNT_JSON` unset or empty | `play-upload.sh: PLAY_SERVICE_ACCOUNT_JSON is not set. Export it as the absolute path of the Play service-account JSON key.` | 78 | `:50-54` |
| 3 | the value does not start with `/` | `play-upload.sh: PLAY_SERVICE_ACCOUNT_JSON is not an absolute path. This script runs from the repository root, so give the key's absolute path.` | 78 | `:55-61` |
| 4 | the value names a missing, unreadable or non-regular file | `play-upload.sh: PLAY_SERVICE_ACCOUNT_JSON does not name a readable file.` | 78 | `:62-64` |
| 5 | `app/build/outputs/bundle/release/app-release.aab` missing, empty or not a regular file | `play-upload.sh: no bundle at app/build/outputs/bundle/release/app-release.aab. This script builds nothing: put the release's signed bundle there, built by ./gradlew :app:bundleRelease from the release (docs/release-checklist.md §4).` | 78 | `:65-68` |
| 6 | no `fastlane` on `PATH` | `play-upload.sh: fastlane is not on PATH. Install it (brew install fastlane) and run this again.` | 78 | `:69-71` |

How each path behaves:
- The checks run in this order, all before the `SUPPLY_*` loop and the `fastlane` call, so each of the six exits sends nothing.
- `fail` joins its arguments with one space (`$*`), so each split message prints as the one line above.
- No message interpolates `$key`. Path 5 interpolates only the constant `$AAB`.
- `set -u` cannot fire on an unset variable at `:50`, which reads it through `${…:-}`.
- A failed `cd` at `:32` would exit with `cd`'s own status (1), under `set -e`. Exit 78 covers only the six checks.

### Why they were not run: how the gate judges the script here

I probed `hooks/publish-gate.sh` with Claude Code's PreToolUse payload shape. Nothing ran. Each line of `payloads.jsonl` went to the hook on stdin with the logger off: `ERGOZ_LOG=0 bash ~/.claude/plugins/cache/ergoz-soft/ergoz-soft/1.0.0/hooks/publish-gate.sh`.
- The cache copy of `publish-gate.sh` is byte-identical to `/Users/ermanergoz/Desktop/ergoz-soft/hooks/publish-gate.sh`, the copy that fired live in this session (sha256 `895a0ef7…77ef5c7`; the full value is in Checks).
- `lib-cmdpos.sh` is identical in both copies too (sha256 `8a0a8f37…bc02821c`).
- The payload `cwd` was this worktree, except row 6.

| # | Command | Gate | Message |
|---|---|---|---|
| 1 | `./scripts/play-upload.sh` | exit 2, blocked | `ergoz-soft publish gate: publishing 3.0.0 requires founder approval. Missing: /Users/ermanergoz/Desktop/PegSolitarie-pu/.planning/approvals/publish-3.0.0.approved. …` |
| 2 | `scripts/play-upload.sh` | exit 2 | the same |
| 3 | `bash scripts/play-upload.sh` | exit 2 | the same |
| 4 | `env -u PLAY_SERVICE_ACCOUNT_JSON ./scripts/play-upload.sh` (failure path 2) | exit 2 | the same |
| 5 | `PLAY_SERVICE_ACCOUNT_JSON=/nonexistent/play-key.json ./scripts/play-upload.sh` (failure path 4) | exit 2 | the same |
| 6 | the script's absolute path, run from `/Users/ermanergoz/Desktop/PegSolitarie` | exit 2 | the same, naming this worktree's approval: judged by the repository that holds the script |
| 7 | `bash -n scripts/play-upload.sh` | exit 2 | the same. It is only a syntax check, but it reads as a run (`bash` is a wrapper in `ERGOZ_CMD_POS`), so the syntax check below reads the file on stdin |
| 8 | `shellcheck scripts/play-upload.sh` | exit 0, allowed | — |
| 9 | `/bin/bash -n < scripts/play-upload.sh` | exit 0, allowed | — |

`.planning/approvals/` here holds `concept.approved` only. So a run the gate can see is blocked until Gate 2, and any route it cannot see would be routing around it. I took neither, and I created no approval file, here or in any scratch repository.

## Checks
- `shellcheck --version` — 0.11.0 (Homebrew bottle, installed for this task).
- `shellcheck scripts/play-upload.sh` — pass: no output, exit 0 (also after the last edit, on the committed blob).
- `shellcheck --shell=bash --severity=style --format=gcc scripts/play-upload.sh` — pass: no output, exit 0.
- `shellcheck -o all -f gcc scripts/play-upload.sh` — information only, not the acceptance: exit 1 with 25 opt-in notes: SC2250 ×18 (braces around every variable), SC2292 ×6 (`[[ ]]` over `[ ]`) and SC2249 ×1 (a default branch in the `SUPPLY_*` `case`). Under the same flag `scripts/check.sh` gives SC2250 ×24, SC2292 ×6 and SC2310 ×4. The house style follows none of these opt-in checks, so I did not adopt them.
- `shellcheck scripts/check.sh` — pass: exit 0. Data for open issue 1.
- `/bin/bash -n < scripts/play-upload.sh` (GNU bash 3.2.57) — pass: exit 0.
- `git ls-files -s scripts/play-upload.sh` — `100755`.
- `git show HEAD:scripts/play-upload.sh | shasum -a 256` — `35323283ffe33d7afeee5071d93c8d0c94b97a061200c177b55b377dcddf9abd`, the same as the working copy. `git diff --stat HEAD` is empty.
- `git log -1 --format=%B | grep -ci co-authored-by` → 1; `… | grep -ci claude-session` → 0. The one trailer is the last line. This repository has no `prepare-commit-msg` hook installed, so I wrote the trailer by hand.
- Gate probe — `ERGOZ_LOG=0 bash <plugin 1.0.0>/hooks/publish-gate.sh` per payload line — pass (7 run forms blocked, 2 read forms allowed; table above). `shasum -a 256` of `hooks/publish-gate.sh`: `895a0ef7c76335b1e2902e18e1bfe737323e344e41e6d6601eaf3fc1677ef5c7` in the 1.0.0 cache and in the plugin repository; `hooks/lib-cmdpos.sh`: `8a0a8f3702013966bdcdc46a796da56883737362f256247c51e5a776bc02821c` in both.
- `./scripts/check.sh --max-workers=4` — pass: `BUILD SUCCESSFUL in 11m 18s`, `104 actionable tasks: 43 executed, 61 from cache`.
  - Conditions: a `local.properties` holding only `sdk.dir`, as CI writes it. Started 22:36:34 at a 1-minute load of 2.04 and ended 22:47:53 at 5.75, on 10 logical cores, with two QA emulators running. `--max-workers=4` was there to spare the parallel sessions.
  - Tests, from the JUnit XML, 1,576 in all, 0 failed, errored or skipped:

    | Task | Tests | Suites | How it ran |
    |---|---|---|---|
    | `:domain:test` | 294 | 33 | FROM-CACHE |
    | `:domain:depthSweep` | 1 | 1 | executed; its output: Diamond-41 `undecided=440`, Wiegleb-45 `undecided=4`, as §7.2 states |
    | `:domain:fuzz` | 1 | 1 | FROM-CACHE; its restored output: `fuzz: examined 10000 levels (requested 10000), determinism pass, 0 failures` |
    | `:app:testDebugUnitTest` | 1,225 | 123 | executed |
    | build-logic | 55 | 6 | — |

  - 0 `w:` lines, `domain/build/fuzz-failures.txt` absent, and `:domain:koverVerify` ran.
  - No Gradle input changed: `git grep -n -i 'scripts' -- '*.gradle.kts' 'build-logic/src/main' 'gradle/*.toml' 'gradle.properties'` finds only a comment in `gradle/libs.versions.toml:114` about the plugin's `scripts/latest.sh`, so no task reads `scripts/`.
- `python3 -I <scratchpad>/verify_embed.py` — pass: the fenced block above is byte-identical to the committed script (4,306 bytes, the same sha256).
- No unit test was written. The deliverable is a shell script and changes no `:domain` or `:app` logic. The repository has no shell-test harness, and the gate forbids executing the script before Gate 2. So TDD's red step had no runnable target, and row PU's static acceptance stands in for it: shellcheck clean, and the code-auditor's read.
- Not run, as dispatched: the script, and any `fastlane supply` command. I read fastlane 2.233.0's supply sources (`options.rb`, `uploader.rb`, `client.rb`) instead of running `fastlane --help`, because every fastlane run starts an update check over the network (`fastlane/lib/fastlane/cli_tools_distributor.rb:78`, unless `FASTLANE_SKIP_UPDATE_CHECK` is set).

## Open issues / risks
1. **Should the Check or CI lint the scripts? The tech-lead's call, not wired.** Facts:
   - Both `scripts/*.sh` are shellcheck-clean today.
   - shellcheck was not on the studio Mac until today, so a gate makes it a required host tool.
   - §9.1 registers a new gate in `check` "and nowhere else", because CI runs `check` verbatim (§9.7).
   - I have not checked here whether the ubuntu-latest image ships shellcheck.

   Options:
   - (a) A root-project task in `check` (an `Exec` on `scripts/*.sh`, cacheable on those inputs). It keeps §9.1's one registration, but every local Check then needs shellcheck, and CI may need an install step.
   - (b) A CI-only step before the Gradle steps. It costs seconds, but it is a second registration, which is the drift §9.7 was written against.
   - (c) No gate. Review and checklist 1.7's one `shellcheck scripts/play-upload.sh` carry it, as today.
2. **The failure paths are unexecuted** (above). After Gate 2, a first call with `PLAY_SERVICE_ACCOUNT_JSON` unset passes the gate and exits 78, sending nothing. That would observe path 2 and the gate together. It is the release-manager's procedure to adopt or not.
3. **Where the bundle comes from at upload time.**
   - The script uploads the bundle in the checkout it runs from, which is `feature/publish-<X.Y.Z>-done`, cut from `develop`.
   - A `bundleRelease` there builds `develop`'s tree, not the release. So the bundle must be the one §4 built, carried over: a branch switch in the same worktree keeps the ignored `app/build/`, or it is copied and its sha256 compared with checklist 4.6's record.
   - Failure path 5's message says "built … from the release" for this reason.
   - The release procedure should name the route. The release-manager owns it.
4. **Texts to align. None of them is mine to edit.**
   - Once this branch merges, three texts go stale:
     - `docs/release-checklist.md` 1.7 ("Today it does not exist").
     - The note under its §4 ("The gated upload script does not exist yet").
     - `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` item 2 ("Until it merges").
   - `docs/store/README.md:5-8` says the script "is planned to upload" the listing. Per row PU it uploads only the bundle, and the founder pastes the listing and the release notes.
   - These four are the release-manager's.
   - `.planning/phases/15-ux-refresh/15-PLAN.md:24` and `:137` say `scripts/play-upload.sh` "resolve[s] the approval file from [VERSION] at run time". That is false for this script, which reads no `VERSION`. It is a phase-15 planning record that row PU supersedes; for the tech-lead.
5. **fastlane prints the key's path.** Its summary table masks only `json_key_data`, so the upload's console output names the absolute key path, though never the key. The path carries the user name and, under Google's default file name, the key id. The upload report is committed (`.planning/reports/publish-<X.Y.Z>/release-manager-upload.md`), so it should quote that table without the `json_key` row. That is the release-manager's call. Passing the key's content as `json_key_data` instead would put the secret in the script's memory and in fastlane's environment, which is worse.
6. **No `--mapping` upload.** The row asks for the bundle only. AGP is documented to carry R8's mapping inside the bundle, but I have not verified it here (there is no release bundle on this host). Checking that Play Console shows the deobfuscation file for 30000 after the upload is the release-manager's job. If it is missing, `--mapping app/build/outputs/mapping/release/mapping.txt` is one line.
7. **Host changes on the shared Mac** (for the producer's record):
   - I ran `brew install shellcheck` with `HOMEBREW_NO_AUTO_UPDATE`, `HOMEBREW_NO_INSTALL_UPGRADE` and `HOMEBREW_NO_INSTALLED_DEPENDENTS_CHECK` set. It installed shellcheck 0.11.0 (GPL-3.0, a host tool, not linked into or shipped with the product); its one dependency, gmp 6.3.0, was already installed.
   - I did not set `HOMEBREW_NO_INSTALL_CLEANUP`, so Homebrew's periodic `brew cleanup` ran. The 15 output lines I kept show 9 entries removed from `~/Library/Caches/Homebrew` (an ntfs-3g-mac bottle and its manifest, a portable Ruby, a macFUSE `.dmg`, four API files and a bootsnap cache) and 5 log directories from `~/Library/Logs/Homebrew`, and "Pruned 0 symbolic links and 2 directories from /opt/homebrew". Earlier lines, not kept, may list more.
   - The cleanup also changed 20 Cellar directories at 22:25. Each now holds one version, which reads as older, unlinked kegs removed: dav1d, fontconfig, giflib, graphite2, icu4c@78, jpeg-turbo, lame, libass, libbluray, libtiff, libunibreak, libvmaf, libvpx, little-cms2, mpg123, opus, readline, svt-av1, x265 and xz. I printed only the last 15 lines of brew's output, so the removed versions are not recorded.
   - No linked or current version changed.
   - The Check ran with a `local.properties` holding only `sdk.dir` in this worktree. The file is git-ignored, and I removed it after the run. I changed nothing else on the host.
8. **Two `gate.block` entries in today's studio log, at about 22:32, are mine, from read-only commands.** One was a `grep` whose pattern held the words `fastlane supply`. The other was a `grep` and an `awk` whose quoted parentheses and `{…}` block came before the script's path, which the gate reads as a subshell or a group running it (re-probed offline: `ERGOZ_LOG=0`, payloads in `blocked-mine.jsonl`). Nothing ran. Both are the gate's documented over-block of quoted text. I note them so that the retro does not read them as upload attempts.
9. **`THIRD_PARTY.md` gets no row** from this branch. fastlane (MIT, Homebrew 2.233.0 here) and shellcheck are host tools: nothing ships, and nothing enters the Gradle build. A tooling row would be the release-manager's.

## Handoff
Next:
- **code-auditor:** read `scripts/play-upload.sh` (`9005fe3`, 93 lines) against tech design §14.1 PU and this report's two tables.
  - Re-run `shellcheck scripts/play-upload.sh` and `/bin/bash -n < scripts/play-upload.sh`. The form `bash -n scripts/…` is gate-blocked.
  - Rule on the choices above the failure table, which the row leaves open: the pinned `completed`, the five skip flags, the `SUPPLY_*` unset, the absolute-path rule and exit 78.
- **qa-engineer:** gitflow asks for a QA PASS before the merge, and row PU's chain names none. Nothing here is runnable before Gate 2, so this is a review-mode pass at most. Whether it is owed is the producer's call, as it was for check-gates C7.
- **tech-lead:** the sign-off, and the lint ruling (open issue 1).
- **producer:** route the release-manager's items (open issues 2, 3, 4, 5 and 6) and record the host changes (7).
