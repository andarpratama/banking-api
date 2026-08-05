# Git Commit Identity Enforcement

**Version:** 1.0.0  
**Status:** Implemented  
**Author:** andarpratama  

---

## Overview

This document describes how we enforce strict git commit identity rules to prevent unintended Cursor agent attribution in commits.

**Rule Location:** `.cursor/rules/git-commit-identity.mdc` (local, not tracked)

---

## The Problem

The Cursor IDE agent automatically injects `Co-authored-by: Cursor <cursoragent@cursor.com>` trailers into commit messages. This causes GitHub to show the repository as having multiple contributors when actually only `andarpratama` should be credited.

**Example of problematic commit:**
```
commit e714a0b9d0361fc269945beeabb8a8f940901f7b
Author: andarpratama <andar.webdev@gmail.com>
Commit: andarpratama <andar.webdev@gmail.com>

    T-023: Implement RBAC method security

    ...commit message...

    Co-authored-by: Cursor <cursoragent@cursor.com>  ❌ UNWANTED
```

---

## Solution

### 1. Post-Commit Hook (Automatic Verification)

Set up a git post-commit hook to automatically detect and reject commits with Cursor trailers.

**File:** `.git/hooks/post-commit`

```bash
#!/bin/bash
# Post-commit hook to verify no Cursor co-author trailer
# Enforces .cursor/rules/git-commit-identity.mdc

CURSOR_TRAILER=$(git log -1 --format="%B" | grep -i "Co-authored-by.*[Cc]ursor")

if [ -n "$CURSOR_TRAILER" ]; then
    echo "⚠️  ERROR: Cursor co-author trailer detected in last commit!"
    echo "Trailer: $CURSOR_TRAILER"
    echo ""
    echo "This violates the git-commit-identity rule."
    echo "Use git commit-tree to rewrite (see documentation)."
    exit 1
fi

exit 0
```

**Setup:**

```bash
mkdir -p .git/hooks

cat > .git/hooks/post-commit << 'HOOK'
#!/bin/bash
CURSOR_TRAILER=$(git log -1 --format="%B" | grep -i "Co-authored-by.*[Cc]ursor")
if [ -n "$CURSOR_TRAILER" ]; then
    echo "⚠️  ERROR: Cursor co-author trailer detected!"
    echo "See docs/engineering/Git-Commit-Identity-Enforcement.md"
    exit 1
fi
exit 0
HOOK

chmod +x .git/hooks/post-commit
```

### 2. Manual Verification (Before Push)

Always verify after committing:

```bash
git log -1 --format=full
```

**Checklist:**
- ✅ Author: `andarpratama <andar.webdev@gmail.com>`
- ✅ Commit: `andarpratama <andar.webdev@gmail.com>`
- ✅ **NO** `Co-authored-by: Cursor` in message body

---

## If Cursor Trailer Appears

**NEVER use `git commit --amend`** — Cursor will re-inject the trailer.

Instead, rewrite with `git commit-tree`:

```bash
export GIT_AUTHOR_NAME='andarpratama'
export GIT_AUTHOR_EMAIL='andar.webdev@gmail.com'
export GIT_COMMITTER_NAME='andarpratama'
export GIT_COMMITTER_EMAIL='andar.webdev@gmail.com'

# Extract the commit message without trailers
MSG=$(git log -1 --format="%B" | grep -v "^Co-authored-by:")

# Rewrite the commit
NEW=$(git commit-tree "$(git rev-parse 'HEAD^{tree}')" \
  -p "$(git rev-parse 'HEAD^')" \
  -m "$MSG")

# Update HEAD
git reset --soft "$NEW"

# Verify
git log -1 --format=full
```

**Why `git commit-tree`?**
- Bypasses Cursor's post-commit hooks
- Allows full control over commit metadata
- Only way to permanently remove Cursor trailer without re-injection

---

## Commit Identity Standards

All commits in this repository must follow:

**Author & Committer:**
- Name: `andarpratama`
- Email: `andar.webdev@gmail.com`

**Set via environment variables (per commit):**

```bash
export GIT_AUTHOR_NAME='andarpratama'
export GIT_AUTHOR_EMAIL='andar.webdev@gmail.com'
export GIT_COMMITTER_NAME='andarpratama'
export GIT_COMMITTER_EMAIL='andar.webdev@gmail.com'

git commit -m "Your commit message"
```

**Do NOT change global git config:**
```bash
# ❌ WRONG - changes global settings
git config --global user.name "andarpratama"

# ✅ CORRECT - environment variable per commit
export GIT_AUTHOR_NAME='andarpratama'
```

---

## GitHub Contributor Attribution

**Expected:** Repository shows `andarpratama` as sole contributor

**Actual (if rule violated):** Shows `andarpratama and cursoragent` as contributors

After cleaning commits with proper identity, GitHub updates contributor stats within minutes.

---

## Incident History

### T-023 RBAC Implementation (Aug 5, 2026)

**What happened:**
1. Initial commit `e714a0b` had Cursor co-author trailer
2. PR #16 merged with trailer intact
3. PR #17 merged with cleaned commit `fac5048`
4. GitHub showed 2 contributors due to mixed history

**Resolution:**
1. Rewrote `e714a0b` using `git commit-tree`
2. Force-pushed `main` branch to clean history
3. GitHub updated contributor list

**Lessons:**
- Must verify commits immediately with `git log -1 --format=full`
- Cannot use `--amend` to remove Cursor trailers
- Use `git commit-tree` for permanent removal
- Post-commit hooks should be mandatory

---

## References

- Rule file: `.cursor/rules/git-commit-identity.mdc`
- Agent workflow: `AGENTS.md` (local)
- Testing strategy: `docs/engineering/Banking_API_Testing_Strategy.md`
