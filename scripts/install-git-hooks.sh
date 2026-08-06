#!/bin/bash
# Installs repo-tracked git hooks (scripts/git-hooks/*) into .git/hooks/.
# Run once after cloning: ./scripts/install-git-hooks.sh
#
# Enforces .cursor/rules/git-commit-identity.mdc (no Cursor co-author trailers).

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
SRC_DIR="$REPO_ROOT/scripts/git-hooks"
DEST_DIR="$REPO_ROOT/.git/hooks"

if [ ! -d "$SRC_DIR" ]; then
    echo "ERROR: $SRC_DIR not found. Run this from within the repo." >&2
    exit 1
fi

mkdir -p "$DEST_DIR"

for hook in "$SRC_DIR"/*; do
    name="$(basename "$hook")"
    cp "$hook" "$DEST_DIR/$name"
    chmod +x "$DEST_DIR/$name"
    echo "Installed hook: $name"
done

echo ""
echo "Done. Git hooks installed to $DEST_DIR"
