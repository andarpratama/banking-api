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

### 1. `prepare-commit-msg` Hook (Primary Defense — Automatic Fix)

A `post-commit`-only hook (v1 of this doc) only **detects** the trailer after the
commit object already exists — it doesn't stop it from being created or pushed if
the warning is missed (see Incident History below: this happened twice, T-023 and
T-030). The fix is a `prepare-commit-msg` hook, which runs **before** the commit is
created and edits the message file directly, stripping the trailer so it never
makes it into the commit object at all.

Hooks are tracked in the repo (git does not version `.git/hooks/` itself) under
`scripts/git-hooks/` and installed per clone with:

```bash
./scripts/install-git-hooks.sh
```

Source: `scripts/git-hooks/prepare-commit-msg` (strips `Co-authored-by: ... [Cc]ursor`
and `cursoragent@cursor.com` lines from the commit message file before commit).

In a Cursor sandbox, installing may require the `all` permission if `.git/hooks`
is mounted read-only by default (`chmod`/write access denied otherwise).

### 2. `post-commit` Hook (Secondary Safety Net)

Kept as a fallback in case a commit bypasses hooks entirely (e.g. `--no-verify`).
It only warns — it does not modify the commit — so the `git commit-tree` fix below
is still needed if it fires.

Source: `scripts/git-hooks/post-commit`.

### 3. Manual Verification (Before Push)

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

### T-030 Customer Feature (Aug 6, 2026)

**What happened:**
1. `post-commit` hook existed but was **not executable** (`-rw-r--r--`); a prior
   sandboxed `chmod +x` attempt had silently failed ("Read-only file system"),
   so the hook never ran and never warned.
2. Commit `4fed446` was pushed with the Cursor trailer intact and merged via PR #18.
3. Caught manually from the GitHub UI (showed `andarpratama and cursoragent`).
4. Rewrote the commit with `git commit-tree` → `a16a86f`, opened PR #19, merged.
5. Reset `main` to before both duplicate merges and cherry-picked only the clean
   commit, then force-pushed `main` to eliminate `4fed446` from history entirely.

**Resolution:**
1. Confirmed `chmod +x` on `.git/hooks/*` works when the `all` sandbox permission
   is granted (it was blocked under default sandbox restrictions).
2. Replaced the detect-only `post-commit` approach with a `prepare-commit-msg`
   hook that strips the trailer **before** the commit is created — self-healing
   instead of relying on someone reading a warning.
3. Moved hook sources into `scripts/git-hooks/` (tracked in git) with an
   `scripts/install-git-hooks.sh` installer, so the fix survives across clones
   and fresh sandbox sessions instead of living only in the local `.git/hooks/`.

**Lessons:**
- A hook that only detects after the fact is not enough if nobody reads the
  output before pushing — prefer hooks that actively fix the message.
- `.git/hooks/` is not tracked by git; anything installed there is lost on a
  fresh clone/session unless there's a tracked install script.
- Verify hook executability (`ls -la .git/hooks/`) as part of hook setup, not
  just that the file exists.

---

## References

- Rule file: `.cursor/rules/git-commit-identity.mdc`
- Hook setup guide: `.ai/git-hooks-setup.md`
- Hook sources: `scripts/git-hooks/prepare-commit-msg`, `scripts/git-hooks/post-commit`
- Install script: `scripts/install-git-hooks.sh`
- Agent workflow: `AGENTS.md` (local)
- Testing strategy: `docs/engineering/Banking_API_Testing_Strategy.md`
