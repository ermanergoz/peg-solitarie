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
