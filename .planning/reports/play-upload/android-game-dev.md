# android-game-dev report — play-upload (fix of the code-auditor's loop-0 FAIL)
Date: 2026-10-24
Branch: feature/play-upload (6a0b032..6a0b032: one fix commit on 8240177; the round's first pass is 9005fe3, reported in `android-game-dev-loop0.md`)
Skills loaded: ergoz-soft:standards (role android-game-dev; plugin 1.0.0 copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing and stacks, read in full in this agent's first pass, and `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow (its commit-style rules overridden by rules/gitflow.md)
Ran in parallel with: none named in the fix dispatch
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- `6a0b032 fix(release): close the play-upload audit findings` closes F-1 to F-4 of `code-auditor-loop0.md` in `scripts/play-upload.sh` (131 lines, mode 100755), and rules R-1 and R-2 **in**.
- F-1: `unset CDPATH`, then `cd -- "$(dirname -- "$0")/.."`. F-2: exit 78 while `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` is at the root; `--ack_bundle_installation_warning false`; the comments corrected. F-3: exit 78 unless `BASH_VERSION` is set, and the loop now fails closed. F-4: `FASTLANE_OPT_OUT_USAGE=1 FASTLANE_SKIP_UPDATE_CHECK=1` on the fastlane line, said in the header.
- `shellcheck scripts/play-upload.sh` prints nothing and exits 0; `/bin/bash -n < scripts/play-upload.sh` and `zsh -n < scripts/play-upload.sh` exit 0. The Check (`./scripts/check.sh --max-workers=4`) is green: BUILD SUCCESSFUL in 11 m 7 s, 1,576 tests with 0 failed or skipped, and the fuzz at 10,000 levels with 0 failures.
- As before, neither the script nor fastlane was run, by any route. Each fix's behaviour was shown on throwaway files of my own that copy only the construct (Checks).

## Outputs
- `scripts/play-upload.sh` — the fixed script: 131 lines, 6,758 bytes, sha256 `31873d87faf0fd7be213141884ae6239e06a3ada7001431434763bdb2cc20747` (`6a0b032`).
- `.planning/reports/play-upload/android-game-dev.md` — this report.

### Each finding's fix line

| Finding | Fix (line in `6a0b032`) |
|---|---|
| F-1 MAJOR, `cd` followed an exported `CDPATH` | `:60` `unset CDPATH`; `:63` `cd -- "$(dirname -- "$0")/.."`. The `--` also stops a `$0` that starts with `-` from reading as an option. The comment at `:57-59` names the threat. |
| F-2 (a) the `SUPPLY_ROLLOUT` example | `:109-111`: the example is now `SUPPLY_ROOT_URL` only. `rollout` and `version_codes_to_retain` have no `env_name` in `supply/lib/supply/options.rb` (2.233.0), so neither is an example. |
| F-2 (b) `ACK_BUNDLE_INSTALLATION_WARNING` | `:126` `--ack_bundle_installation_warning false`, pinned on the command line, which outranks the variable and any config file. The OBB pair (`"SUPPLY_OBB_MAIN_FILE SIZE"`, `"SUPPLY_OBB_PATCH_FILE SIZE"`, `options.rb:324`, `:334`) is named in the header at `:29-31` as impossible to unset by name and inert for a bundle upload. |
| F-2 (c) config files at the root | `:98-103`: `for config in fastlane .fastlane Supplyfile Appfile` refuses, with exit 78, while any of the four exists, or is a symlink. A fastlane folder is also what makes fastlane load `.env` files: `load_dot_env` returns unless `find_dotenv_directory` finds a folder (`fastlane/lib/fastlane/helper/dotenv_helper.rb:5-8`), and that searches only `FastlaneFolder.path` and its parent (`:14-22`). The path is nil without `./fastlane/` or `./.fastlane/`, since the root's name is not `fastlane` (`fastlane_core/lib/fastlane_core/fastlane_folder.rb:8-15`). |
| F-2 (d) the header | `:25-31`: "Only the options on the fastlane line below decide the upload", then what makes that true: the `SUPPLY_*` unset, the pinned `ack_` option, the four refused names (with the `.env` note), and the OBB pair. |
| F-3 the loop under zsh | `:50-54`: `if [ -z "${BASH_VERSION:-}" ]; then fail "run it with bash, as ./scripts/play-upload.sh"; fi`. It comes before `set -euo pipefail` and every bash-only line. The constants and `fail` above it are plain POSIX, and `zsh -n` parses the whole file. Belt: `:112` reads the names first, `exported="$(compgen -e)" \|\| fail …`, so a missing `compgen` stops the script with 78 instead of skipping the loop (the auditor's "set -e does not fire on a failed substitution in a for word list"). |
| F-4 analytics and the update check | `:120` `FASTLANE_OPT_OUT_USAGE=1 FASTLANE_SKIP_UPDATE_CHECK=1 fastlane supply`, scoped to the fastlane process. Header `:32-33` says so. `Env.truthy?` treats `1` as true (`fastlane_core/lib/fastlane_core/env.rb:3-7`), and the opt-out is read at `analytics/analytics_session.rb:56`. |

### R-1 and R-2, ruled in

- **R-1 in** (`:107`, `:119`): `digest="$(shasum -a 256 "$AAB")" || fail …`, then the upload line prints `(sha256 <hash>)`. The script uploads whatever bundle sits in the checkout, and QA's T-3 makes the hash comparison with checklist 4.6 the only provenance check the procedure has. Printing the hash puts the uploaded bytes' identity into the upload report at no cost. The script only prints the hash; the comparison stays the procedure's (T-3), because the script has no reference value and takes no arguments. A missing `shasum` stops it with 78, before anything is sent.
- **R-2 in** (`:84-93`): a key whose folder resolves inside `$(pwd -P)` is refused with "PLAY_SERVICE_ACCOUNT_JSON names a file inside this checkout. Keep the key outside the repository." The path is not printed. Both sides are physical paths: `cd -- "$(dirname -- "$key")" && pwd -P` in a subshell. So a symlink into the checkout is refused, and a sibling folder sharing the name prefix (`PegSolitarie-pu-keys`) is not. It enforces what the header already told the operator. It does not cover the main checkout or the other worktrees: refusing any git working tree would refuse every path in a home directory kept under git, so the rule stays "this checkout".

### The script, in full, as committed in `6a0b032`

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
# It prints the bundle's sha256 first, for the upload report to compare with checklist item 4.6.
# It uploads nothing else: the listing text, images and screenshots are what production players
# see, and the founder enters them and the release notes in the Play Console (docs/store/).
#
# Usage: ./scripts/play-upload.sh   (no arguments; bash only)
# Environment:
#   PLAY_SERVICE_ACCOUNT_JSON  the absolute path of the Play Console service-account JSON key,
#                              outside this checkout (a key inside it is refused). The script hands
#                              the path to fastlane; it never prints the path and never reads the
#                              file. fastlane's own summary table does print the path, not the
#                              key's content.
# Only the options on the fastlane line below decide the upload. fastlane takes any option it is
# not given from an environment variable or from a config file, so the script unsets every
# SUPPLY_* variable, pins ack_bundle_installation_warning (its variable has no SUPPLY_ prefix) on
# the command line, and refuses to run while a fastlane or .fastlane folder (which also loads .env
# files), a Supplyfile or an Appfile sits at the root. The two OBB size variables
# ("SUPPLY_OBB_MAIN_FILE SIZE", "SUPPLY_OBB_PATCH_FILE SIZE") hold a space, so no shell can unset
# them by name; supply reads them only on the APK path, which --skip_upload_apk true skips.
# fastlane runs with FASTLANE_OPT_OUT_USAGE=1 and FASTLANE_SKIP_UPDATE_CHECK=1, so its only
# network calls are Google's token and Play API calls: no analytics event, no update check.
# Exit status: fastlane's; 78 (EX_CONFIG) when a check below fails, before anything is sent.
#
# Bash 3.2 compatible: macOS ships 3.2.

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

# Bash only, checked before the first bash-only line: under zsh (the shell of Claude Code's Bash
# tool) or dash, `compgen` does not exist and the SUPPLY_* loop below would clear nothing.
if [ -z "${BASH_VERSION:-}" ]; then
    fail "run it with bash, as ./scripts/play-upload.sh"
fi
set -euo pipefail

# An exported CDPATH would send the relative cd below (`scripts/play-upload.sh` from the root) to
# another tree's scripts/ folder, and the script would upload that tree's bundle under this
# repository's approval.
unset CDPATH
# The publish gate judges a script named by its path by the repository that holds it, so this
# script uploads that repository's bundle wherever the caller stands.
cd -- "$(dirname -- "$0")/.."

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
# A key inside the checkout is one `git add` away from a commit. Both sides are physical paths.
key_dir="$(cd -- "$(dirname -- "$key")" && pwd -P)" ||
    fail "PLAY_SERVICE_ACCOUNT_JSON names a file whose folder cannot be resolved."
root="$(pwd -P)"
case "$key_dir/" in
    "$root"/*)
        fail "PLAY_SERVICE_ACCOUNT_JSON names a file inside this checkout." \
            "Keep the key outside the repository."
        ;;
esac
if [ ! -f "$AAB" ] || [ ! -s "$AAB" ]; then
    fail "no bundle at $AAB. This script builds nothing: put the release's signed bundle there," \
        "built by ./gradlew :app:bundleRelease from the release (docs/release-checklist.md §4)."
fi
for config in fastlane .fastlane Supplyfile Appfile; do
    if [ -e "$config" ] || [ -L "$config" ]; then
        fail "$config exists at the repository root, and fastlane would read options from it." \
            "Move it away: only this script's options may decide the upload."
    fi
done
if ! command -v fastlane >/dev/null 2>&1; then
    fail "fastlane is not on PATH. Install it (brew install fastlane) and run this again."
fi
digest="$(shasum -a 256 "$AAB")" || fail "cannot hash the bundle: shasum failed."

# supply reads each option it is not given from its SUPPLY_* variable: a SUPPLY_ROOT_URL left in
# the shell would send the authorised upload, token included, to another host. The names are read
# first, so a failure to list them stops the script instead of skipping the loop.
exported="$(compgen -e)" || fail "cannot list the environment (compgen); run it with bash."
for name in $exported; do
    case "$name" in
        SUPPLY_*) unset -v "$name" ;;
    esac
done

echo "play-upload.sh: uploading $AAB (sha256 ${digest%% *}) to the $TRACK track of $PACKAGE_NAME"
FASTLANE_OPT_OUT_USAGE=1 FASTLANE_SKIP_UPDATE_CHECK=1 fastlane supply \
    --package_name "$PACKAGE_NAME" \
    --track "$TRACK" \
    --release_status "$RELEASE_STATUS" \
    --aab "$AAB" \
    --json_key "$key" \
    --ack_bundle_installation_warning false \
    --skip_upload_apk true \
    --skip_upload_metadata true \
    --skip_upload_changelogs true \
    --skip_upload_images true \
    --skip_upload_screenshots true
```

### Failure paths, from the source (none executed)
All exit 78 with `play-upload.sh: ` before the message, on stderr, before anything is sent. They run in this order:
1. Not bash (`:52-54`): "run it with bash, as ./scripts/play-upload.sh". New (F-3).
2. Any argument (`:65-67`): unchanged.
3. `PLAY_SERVICE_ACCOUNT_JSON` unset or empty (`:69-73`), not absolute (`:74-80`), not a readable regular file (`:81-83`): unchanged.
4. Its folder cannot be resolved (`:85-86`), or it sits inside this checkout (`:88-93`): "PLAY_SERVICE_ACCOUNT_JSON names a file inside this checkout. Keep the key outside the repository." New (R-2).
5. The bundle missing, empty or not a regular file (`:94-97`): unchanged.
6. `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` at the root (`:98-103`): "<name> exists at the repository root, and fastlane would read options from it. Move it away: only this script's options may decide the upload." New (F-2).
7. No fastlane on `PATH` (`:104-106`): unchanged.
8. `shasum` fails (`:107`) or `compgen` fails (`:112`): new (R-1, F-3).

A failed `cd` at `:63` still exits with `cd`'s own status under `set -e`. No message interpolates `$key`. Messages 5 and 6 interpolate only the constant `$AAB` and the loop's own constant names.

## Checks
- `shellcheck scripts/play-upload.sh` (0.11.0) — pass: no output, exit 0.
- `shellcheck --shell=bash --severity=style --format=gcc scripts/play-upload.sh` — pass: no output, exit 0.
- `shellcheck -o all -f gcc scripts/play-upload.sh` — information only: opt-in style notes SC2250 ×26, SC2292 ×9 and SC2249 ×2, the same kinds as at loop 0 and as `scripts/check.sh`. Not adopted, for the same reason.
- `/bin/bash -n < scripts/play-upload.sh` (GNU bash 3.2.57) — pass: exit 0.
- `zsh -n < scripts/play-upload.sh` (zsh 5.9) — pass: exit 0, so under zsh the script reaches the F-3 guard.
- `awk 'length > 100 {…}' < scripts/play-upload.sh` — no line over 100 columns.
- `git ls-files -s scripts/play-upload.sh` — `100755`. `git show HEAD:scripts/play-upload.sh | shasum -a 256` — `31873d87faf0fd7be213141884ae6239e06a3ada7001431434763bdb2cc20747`, the same as the working copy. `git diff --stat 8240177 HEAD` — 1 file, 56 insertions, 18 deletions.
- The fix commit's message: `grep -ci co-authored-by` → 1, `grep -ci claude-session` → 0, and the last line is `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
- Throwaway probes in the session scratchpad. These are my own files, each holding only the construct under test, never this script or any part of it run as one:
  - F-1: `repoA/scripts/probe-old.sh` holds the loop-0 `cd`, and `probe-fixed.sh` holds `unset CDPATH` plus `cd -- "$(dirname -- "$0")/.."`. Both ran from `repoA` with `CDPATH=<scratch>/other` exported and `other/scripts` present. Old: `scripts/…` → `other`, `/bin/bash scripts/…` → `other`, `./scripts/…` → `repoA`. Fixed: all three → `repoA`.
  - F-3: `zsh -c` on a `readonly` constant, a `fail()` function and the `BASH_VERSION` test printed `msg not bash` and exited 78.
  - R-2: `/bin/bash -c` with the script's three lines (`key_dir=…`, `root=…`, the `case`) and four keys. `repoA/keys/k.json` → refused, `repoA/k.json` → refused, `repoA-keys/k.json` → allowed, `link-to-repoA/keys/k.json` → refused.
  - `dirname -- /a/b/c` → `/a/b` on macOS.
- fastlane 2.233.0 sources, read only: the `env_name` of every supply option (`options.rb`); `dotenv_helper.rb:5-8`, `:14-22`; `fastlane_folder.rb:8-15`; `env.rb:3-7`; `analytics/analytics_session.rb:56`; `cli_tools_distributor.rb:78`.
- `./scripts/check.sh --max-workers=4` on `6a0b032` — pass: `BUILD SUCCESSFUL in 11m 7s`, `104 actionable tasks: 11 executed, 93 up-to-date`.
  - Conditions: a `local.properties` holding only `sdk.dir`, removed by the run. Started 23:09:59 at a 1-minute load of 3.08 and ended 23:21:06 at 3.83, on 10 logical cores.
  - `:domain:depthSweep` and `:domain:koverVerify` executed.
  - `:domain:test`, `:domain:fuzz` and `:app:testDebugUnitTest` were UP-TO-DATE: no Gradle input changed since this worktree's loop-0 Check, because no task reads `scripts/`. So their JUnit XML is that run's, on identical inputs: `:domain:test` 294, depthSweep 1, fuzz 1 (`fuzz: examined 10000 levels (requested 10000), determinism pass, 0 failures`), `:app:testDebugUnitTest` 1,225, build-logic 55. That is 1,576 tests, 0 failed, errored or skipped.
  - 0 `w:` lines, and `domain/build/fuzz-failures.txt` absent.
- Not run, as dispatched: the script, and fastlane, by any route.

## Open issues / risks
1. **The cd is no longer the first command.** The constants, `fail`, the bash test, `set` and `unset CDPATH` now come before it. None of them reads or changes the working directory, so the script still moves to its own repository before anything that depends on the working directory. The gate's description ("start with `cd "$(dirname "$0")/.."`", `hooks/lib-cmdpos.sh:118`) holds in that sense, and the plugin text may want the word "first" softened. That is for the producer to route, not a product change.
2. **`scripts/check.sh:38`** has the same `cd` idiom (auditor open issue 3). It fails closed under `CDPATH`, and it is outside this diff. The tech-lead decides.
3. **Carried from loop 0, unchanged:** the lint gate (tech-lead), the release-manager's texts and procedure (QA's T-1 to T-10), the key path in fastlane's summary table, the R8 mapping, and the host changes recorded in `android-game-dev-loop0.md`. This fix changed nothing on the host but scratchpad files and the `sdk.dir`-only `local.properties` the Check uses, which the run removes.

## Handoff
Next:
- **code-auditor:** re-read the fix diff `8240177..6a0b032` against F-1 to F-4, and rule on R-1 and R-2 as ruled in here.
- **tech-lead:** the sign-off after a PASS.
- **qa-engineer:** QA's PASS read `9005fe3`. Whether a re-read of this diff is owed is the producer's call: it adds three refusals and one printed line, and changes no behaviour QA tested.
