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
