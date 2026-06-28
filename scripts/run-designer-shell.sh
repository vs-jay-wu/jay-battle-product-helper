#!/usr/bin/env bash
#
# Launch the standalone Designer Shell — the universal control panel + project
# picker (core module `designer-shell/`, NOT examples/ragdoll-cat/designer-shell,
# which is a Compose target host). On launch it scans examples/ for adopter apps
# that declare a .designer-shell.json and lists them in the RepoPicker.
#
# Usage:  scripts/run-designer-shell.sh        (run from anywhere)
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT/designer-shell"

echo "[run] launching Designer Shell from $REPO_ROOT/designer-shell ..."
exec ./gradlew run --console=plain
