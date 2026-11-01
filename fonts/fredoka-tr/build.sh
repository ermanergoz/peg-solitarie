#!/usr/bin/env bash
# Rebuilds app/src/main/res/font/fredoka_medium.ttf and fredoka_semibold.ttf from recipe.json, then
# verifies them against their upstream inputs (docs/tech-design.md §11.1, Q-039; README.md).
#
#   fonts/fredoka-tr/build.sh           rebuild in place; verify.py holds the outputs' sha256 to the
#                                       recipe, so a run on an unchanged tree changes no byte
#   fonts/fredoka-tr/build.sh --record  rebuild and write the outputs' sha256 into recipe.json, after
#                                       a reviewed change to the recipe, a script or a tool version
#
# Needs python3 >= 3.10, and a network once to create the venv. Not part of ./gradlew check, which
# holds the shipped bytes to the recipe without Python (BundledFontRecipeTest).
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
venv="$here/.venv"

if [[ ! -x "$venv/bin/python" ]]; then
  python3 -m venv "$venv"
fi
"$venv/bin/python" -m pip install --quiet --disable-pip-version-check -r "$here/requirements.txt"

"$venv/bin/python" -I "$here/build.py" "$@"
"$venv/bin/python" -I "$here/verify.py"
