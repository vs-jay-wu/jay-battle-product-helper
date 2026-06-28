#!/usr/bin/env bash
#
# Launch the standalone Designer Shell WITH Compose Hot Reload — for developing
# the shell's OWN Compose UI (edits hot-swap into the running window via `--auto`).
# For normal use (launching the picker to control other apps) use the sibling
# script run-designer-shell.sh (`./gradlew run`) instead.
#
# Note: the FIRST run provisions a JetBrains Runtime via the foojay toolchain,
# which may download a few hundred MB.
#
# Usage:  scripts/run-designer-shell-hot.sh        (run from anywhere)
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT/designer-shell"

echo "[run-hot] launching Designer Shell (Compose Hot Reload) from $REPO_ROOT/designer-shell ..."
exec ./gradlew hotRun --auto --console=plain
