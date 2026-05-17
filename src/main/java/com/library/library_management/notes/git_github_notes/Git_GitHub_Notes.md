# Git & GitHub — The Complete Guide

A deep-dive reference covering theory, internals, diagrams, real commands, and practical examples. Written with a Java/Spring Boot project (this `library-management` repo) as the running example.

---

## Table of Contents

1. [Theory — Why Version Control Exists](#1-theory--why-version-control-exists)
2. [How Git Works Internally (Mental Model)](#2-how-git-works-internally-mental-model)
3. [The Three Trees & File Lifecycle](#3-the-three-trees--file-lifecycle)
4. [Basic Commands — init, clone, add, commit, push, pull](#4-basic-commands)
5. [Inspecting Changes — status, log, diff](#5-inspecting-changes)
6. [Branching — branch, checkout, switch](#6-branching)
7. [Merging — git merge](#7-merging)
8. [Rebasing — git rebase](#8-rebasing)
9. [Cherry-pick](#9-cherry-pick)
10. [Stash](#10-stash)
11. [Reset vs Revert vs Restore](#11-reset-vs-revert-vs-restore)
12. [.gitignore](#12-gitignore)
13. [Pull Requests & Code Reviews](#13-pull-requests--code-reviews)
14. [GitHub Flow](#14-github-flow)
15. [Conventional Commits](#15-conventional-commits)
16. [Real-World Scenarios](#16-real-world-scenarios)
17. [Cheat Sheet](#17-cheat-sheet)
18. [Glossary](#18-glossary)

---

## 1. Theory — Why Version Control Exists

### The problem before VCS
Imagine 5 developers working on `LibraryService.java`:

```
LibraryService.java
LibraryService_final.java
LibraryService_final_v2.java
LibraryService_final_FINAL.java
LibraryService_john_edit.java
LibraryService_DO_NOT_USE.java
```

Questions you can't answer:
- Who changed this line and why?
- What did the file look like 3 weeks ago?
- How do I merge John's changes with Sarah's without losing work?
- The bug appeared yesterday — which commit broke it?

A **Version Control System (VCS)** solves all of these.

### Centralized VCS (CVCS) vs Distributed VCS (DVCS)

```
CENTRALIZED (SVN, CVS)                DISTRIBUTED (Git, Mercurial)

      ┌──────────┐                          ┌──────────┐
      │  SERVER  │  ← single source         │  SERVER  │  ← just one of many copies
      └─────┬────┘                          └─────┬────┘
       ┌────┼────┐                            ┌───┼───┐
       ▼    ▼    ▼                            ▼   ▼   ▼
    [Dev1][Dev2][Dev3]                  [Dev1][Dev2][Dev3]
    thin clients                        each has FULL history
    (need network to commit)            (works offline)
```

In Git, **every developer has the entire repository** (history, branches, everything) on their machine. The "remote" on GitHub is just a convention — technically all clones are equal.

### Git vs GitHub — clear separation

| | Git | GitHub |
|---|---|---|
| **Type** | Software (CLI tool) | Web service |
| **Installed where** | Your computer | github.com servers |
| **Created by** | Linus Torvalds (2005) | Chris Wanstrath et al. (2008), now Microsoft |
| **Purpose** | Track history, branch, merge | Host repos, PRs, issues, CI/CD, collaboration |
| **Open source?** | Yes | No (the platform) |
| **Alternatives** | Mercurial, SVN, Perforce | GitLab, Bitbucket, Gitea, Azure DevOps |

> **Analogy:** Git is the **engine**. GitHub is the **car dealership + service center** built around it.

---

## 2. How Git Works Internally (Mental Model)

Understanding the data model makes every command obvious. Git is **NOT** a "diff-based" system. It's a **snapshot-based** system backed by a content-addressable key-value store.

### The four object types in `.git/objects/`

```
┌────────────────────────────────────────────────────────────────┐
│                      GIT OBJECT MODEL                          │
│                                                                │
│  COMMIT ──► TREE ──► BLOB (file content)                       │
│    │         │   ──► TREE ──► BLOB                             │
│    │         │           ──► BLOB                              │
│    │                                                           │
│    └──► parent: another COMMIT (forms history chain)           │
│                                                                │
│  TAG    ──► COMMIT (named pointer to a specific commit)        │
└────────────────────────────────────────────────────────────────┘
```

| Object | What it stores | Analogy |
|---|---|---|
| **blob** | File contents (raw bytes) | A file with no name |
| **tree** | Directory listing — names + blob/tree hashes | A folder |
| **commit** | A tree hash + parent hash + author + message | A snapshot in time |
| **tag** | A friendly name for a commit | A bookmark |

Every object is identified by its **SHA-1 hash** (40 hex chars, e.g. `a1b2c3d...`). Same content → same hash → automatic deduplication.

### A commit visualized

```
commit a1b2c3d
├── parent:  9f8e7d6
├── author:  John <john@email.com>
├── date:    2026-05-17
├── message: "feat(book): add ISBN search"
└── tree:    f4e5d6c
              ├── pom.xml ───────── blob 1a2b3c
              └── src/
                  └── main/
                      └── java/
                          └── Book.java ─── blob 4d5e6f
```

### Branches are just pointers

A branch is **not** a copy. It's a 40-byte text file pointing to a commit SHA.

```
.git/refs/heads/main           contains:  a1b2c3d4e5f6...
.git/refs/heads/feature/login  contains:  9f8e7d6c5b4a...
HEAD                            contains:  ref: refs/heads/main
```

This is why branching in Git is **instant** — it just creates a pointer file. Compare to SVN where branching copies the entire codebase.

### The full picture

```
                    refs/heads/main ──► C5
                                          \
                                           C4
                                           /
                            HEAD ──► C3 (feature/login ──► C3)
                                     /
                                    C2
                                    /
                                   C1
                                   /
                                  C0 (initial commit)
```

---

## 3. The Three Trees & File Lifecycle

Every file in Git lives in one of these **three "trees"**:

```
┌──────────────────┐    git add    ┌──────────────────┐    git commit   ┌──────────────────┐
│  WORKING DIR     │  ───────────► │  STAGING (INDEX) │  ─────────────► │   REPOSITORY     │
│  (your files)    │               │  (next snapshot) │                 │  (.git history)  │
└──────────────────┘   git restore └──────────────────┘   git reset     └──────────────────┘
                       ◄─────────                       ◄─────────────
```

| Tree | Where | What |
|---|---|---|
| **Working Directory** | Visible files on disk | What you edit in your IDE |
| **Staging Area / Index** | `.git/index` | What goes in the **next** commit |
| **Repository (HEAD)** | `.git/objects/` | All committed history |

### File states

```
                ┌──────────────────────────────────────────┐
                │              UNTRACKED                   │  (new file, Git doesn't know it)
                └────────────────┬─────────────────────────┘
                                 │ git add
                                 ▼
                ┌──────────────────────────────────────────┐
                │               STAGED                     │  (ready for next commit)
                └────────────────┬─────────────────────────┘
                                 │ git commit
                                 ▼
                ┌──────────────────────────────────────────┐
                │             UNMODIFIED                   │  (clean — matches HEAD)
                └────────────────┬─────────────────────────┘
                                 │ edit file
                                 ▼
                ┌──────────────────────────────────────────┐
                │              MODIFIED                    │  (changed but not staged)
                └──────────────────────────────────────────┘
                                 │ git add → STAGED → git commit → UNMODIFIED
                                 │ git restore → UNMODIFIED (discards changes!)
```

### Example sequence

```bash
$ echo "Hello" > greeting.txt          # UNTRACKED
$ git status
  Untracked files: greeting.txt

$ git add greeting.txt                  # STAGED
$ git status
  Changes to be committed: new file: greeting.txt

$ git commit -m "feat: add greeting"    # COMMITTED (UNMODIFIED)
$ git status
  nothing to commit, working tree clean

$ echo "World" >> greeting.txt          # MODIFIED
$ git status
  Changes not staged for commit: modified: greeting.txt
```

---

## 4. Basic Commands

### `git init` — start a new repo

```bash
cd library-management
git init
```

What it creates:
```
.git/
├── HEAD              # points to current branch
├── config            # local config
├── description
├── hooks/            # scripts that run on events
├── info/
├── objects/          # all blobs, trees, commits
└── refs/             # branch & tag pointers
    ├── heads/
    └── tags/
```

### `git clone` — copy an existing repo

```bash
git clone https://github.com/user/library-management.git
git clone git@github.com:user/repo.git         # via SSH (no password needed)
git clone https://github.com/user/repo.git lib # custom folder name
git clone --depth 1 https://...                # shallow clone (only latest commit, faster)
git clone --branch feature/login https://...   # clone a specific branch
```

### `git add` — stage changes

```bash
git add Book.java                  # one file
git add src/main/java/             # whole folder
git add .                          # everything in current dir (recursive)
git add -A                         # everything including deletions
git add -p                         # interactively pick "hunks" of a file
git add *.java                     # glob
```

**Power move — `git add -p`:** Splits your changes into chunks and asks for each: stage it? skip it? split it further? Lets you make **focused commits** even if you changed multiple things in one file.

### `git commit` — save a snapshot

```bash
git commit -m "feat: add ISBN search endpoint"
git commit -m "fix: null check" -m "Detailed body goes here..."   # title + body
git commit -am "..."             # auto-stage tracked files (skips git add)
git commit --amend               # edit the LAST commit (message or files)
git commit --amend --no-edit     # add staged changes to last commit silently
git commit --allow-empty -m "trigger CI"  # commit with no changes
```

> `--amend` rewrites the last commit. Safe if it hasn't been pushed yet. If pushed, you'd need a force-push (dangerous on shared branches).

### `git push` — upload to remote

```bash
git push                                # push current branch (if upstream set)
git push origin main                    # explicit
git push -u origin feature/login        # set upstream on first push
git push --force                        # DANGEROUS — overwrites remote
git push --force-with-lease             # safer force — fails if remote moved
git push origin --delete feature/login  # delete a remote branch
git push --tags                         # push tags too
```

### `git pull` — fetch + merge

```bash
git pull                       # = git fetch + git merge
git pull --rebase              # = git fetch + git rebase (cleaner)
git pull origin main           # explicit
```

`git fetch` alone just downloads remote changes without touching your branch — useful to inspect before integrating.

```bash
git fetch origin
git log HEAD..origin/main      # see what's new on remote
git merge origin/main          # then integrate
```

### First-time setup

```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
git config --global pull.rebase false        # 'merge' default for pulls
git config --global core.editor "code --wait"    # VS Code as commit editor
git config --global core.autocrlf true       # Windows line endings
```

Check config:
```bash
git config --list
git config --global --edit
```

---

## 5. Inspecting Changes

### `git status` — your most-used command

```bash
git status
git status -s        # short format:  M = modified, A = added, ?? = untracked
git status -b        # show branch info
```

### `git log` — history

```bash
git log                              # full
git log --oneline                    # compact (one line per commit)
git log --oneline --graph --all      # visual tree across all branches
git log --oneline --graph --all --decorate    # add branch/tag labels
git log -p                           # show diff per commit
git log -p Book.java                 # history of one file (with diffs)
git log --stat                       # file change stats per commit
git log --author="John"
git log --grep="ISBN"                # search commit messages
git log --since="2 weeks ago" --until="yesterday"
git log a1b2c3d..HEAD                # commits between two refs
git log --follow Book.java           # follow file across renames
```

Example output of `git log --oneline --graph --all`:

```
* a1b2c3d (HEAD -> main, origin/main) feat(book): add ISBN search
* 9f8e7d6 chore(deps): replace h2 with postgresql
| * 5c4b3a2 (feature/login) feat(auth): hash passwords
| * 7e6d5c4 feat(auth): add login endpoint
|/
* 3d2c1b0 Initial commit
```

### `git diff` — see actual changes

```bash
git diff                          # unstaged changes
git diff --staged                 # staged changes (what `git commit` will save)
git diff HEAD                     # all uncommitted changes
git diff main feature/login       # compare two branches
git diff main..feature/login      # same
git diff main...feature/login     # changes ON feature since branching from main
git diff HEAD~3 HEAD              # last 3 commits combined
git diff Book.java                # single file
git diff --stat                   # summary only
git diff --word-diff              # word-level (good for prose)
```

### `git show` — inspect any commit/object

```bash
git show                  # last commit
git show a1b2c3d          # specific commit (message + diff)
git show a1b2c3d:Book.java   # file content at that commit
git show HEAD~2           # 2 commits ago
```

### `git blame` — who wrote this line?

```bash
git blame Book.java
git blame -L 10,20 Book.java     # only lines 10-20
```

### `git bisect` — binary-search for the commit that broke things

```bash
git bisect start
git bisect bad                 # current is broken
git bisect good a1b2c3d        # this old commit worked
# Git checks out a midpoint commit. You test:
git bisect good                # or bad
# ... repeat until Git finds the culprit
git bisect reset
```

---

## 6. Branching

A **branch** is a movable pointer to a commit. Default name: `main`.

### Visualize before branching

```
   main
    │
    ▼
C1──C2──C3
```

### After `git switch -c feature/login`

```
                  feature/login
                       │
                       ▼
C1──C2──C3
        ▲
        │
       main
```

### Commits on the new branch

```
                       feature/login
                              │
                              ▼
C1──C2──C3──────────────F1──F2
        ▲
        │
       main
```

### Commands

```bash
# List
git branch                     # local
git branch -r                  # remote
git branch -a                  # all
git branch -v                  # with last commit
git branch --merged            # branches merged into current
git branch --no-merged         # NOT yet merged (useful before deleting)

# Create
git branch feature/login       # create (don't switch)
git switch -c feature/login    # create + switch (modern, recommended)
git checkout -b feature/login  # create + switch (older syntax)
git branch feature/login a1b2c3d   # branch from a specific commit

# Switch
git switch main
git switch -                   # toggle to previous branch (like 'cd -')
git checkout main              # older syntax

# Rename
git branch -m old-name new-name        # rename
git branch -m new-name                  # rename current branch

# Delete
git branch -d feature/login    # safe (refuses if unmerged)
git branch -D feature/login    # force
git push origin --delete feature/login   # delete remote branch
```

### `git switch` vs `git checkout`

| Operation | Modern (preferred) | Legacy |
|---|---|---|
| Switch branch | `git switch <branch>` | `git checkout <branch>` |
| Create + switch | `git switch -c <branch>` | `git checkout -b <branch>` |
| Discard file change | `git restore <file>` | `git checkout <file>` |
| Unstage | `git restore --staged <file>` | `git reset HEAD <file>` |

Reason: `checkout` does too many things, leading to confusion. `switch` and `restore` (added in Git 2.23, 2019) split it into focused commands.

---

## 7. Merging

Combines two branches. Two main strategies:

### A. Fast-forward merge

Happens when `main` hasn't diverged since you branched.

**Before:**
```
                  feature/login
                       │
                       ▼
C1──C2──C3──F1──F2
            ▲
            │
           main
```

**After `git switch main && git merge feature/login`:**
```
                       feature/login
                              │
                              ▼
C1──C2──C3──F1──F2
                  ▲
                  │
                 main
```
No new commit — `main` just slides forward.

### B. Three-way merge (merge commit)

Happens when both branches have new commits.

**Before:**
```
              feature/login
                   │
                   ▼
        ┌─────F1──F2
       /
C1──C2──C3──M1──M2
                 ▲
                 │
                main
```

**After `git switch main && git merge feature/login`:**
```
        ┌─────F1──F2────────┐
       /                     \
C1──C2──C3──M1──M2────────────MC ◄── main
                                  │
                              merge commit
                            (has 2 parents)
```
A new "merge commit" `MC` is created with **two parents**.

### Commands

```bash
git switch main
git pull                            # get latest
git merge feature/login             # do the merge
git merge --no-ff feature/login     # FORCE a merge commit even if FF possible
git merge --ff-only feature/login   # REFUSE if not FF (linear history)
git merge --squash feature/login    # combine all feature commits into ONE on main
git merge --abort                   # cancel in-progress merge (conflicts)
```

### Merge conflicts

When both branches modified the **same lines**, Git can't decide. It marks the file:

```java
public class Book {
    private String title;
<<<<<<< HEAD
    private String author;
    private int year;
=======
    private String authorName;
    private String publisher;
>>>>>>> feature/login
}
```

**Resolution steps:**
1. Open the file, decide what to keep, remove `<<<`, `===`, `>>>` markers.
2. `git add <file>` once each conflicted file is fixed.
3. `git commit` (or `git merge --continue`) to finalize.
4. Or `git merge --abort` to bail out entirely.

Use a 3-way merge tool for big conflicts:
```bash
git config --global merge.tool vscode
git mergetool
```

---

## 8. Rebasing

`git rebase` **rewrites** your branch's commits as if they started from a different point. Creates a **linear history**.

### Merge vs Rebase — visual

```
INITIAL STATE:
        F1──F2  ← feature
       /
C1──C2──M1──M2  ← main


AFTER git merge feature (on main):
        F1──F2────┐
       /          \
C1──C2──M1──M2────MC  ← main (has merge commit MC)


AFTER git rebase main (on feature):
C1──C2──M1──M2──F1'──F2'  ← feature
                  ▲
                  └── new commits! F1' has same changes as F1 but different SHA
```

After rebase, `feature` can then be **fast-forward merged** into `main` for a totally linear history.

### Commands

```bash
git switch feature/login
git fetch origin
git rebase origin/main          # replay my commits on top of latest main

# If conflicts:
# fix files, then:
git add <file>
git rebase --continue
git rebase --skip               # skip current commit
git rebase --abort              # cancel everything
```

### Interactive rebase — rewrite history

```bash
git rebase -i HEAD~5    # edit last 5 commits
```

Opens an editor:
```
pick a1b2c3d feat: add login endpoint
pick b2c3d4e fix typo
pick c3d4e5f fix another typo
pick d4e5f6g add validation
pick e5f6g7h address review comments
```

Change `pick` to:
| Command | Effect |
|---|---|
| `pick` (or `p`) | Keep as-is |
| `reword` (or `r`) | Keep commit, edit message |
| `edit` (or `e`) | Pause to amend commit |
| `squash` (or `s`) | Merge into previous commit (combines messages) |
| `fixup` (or `f`) | Like squash but discards this commit's message |
| `drop` (or `d`) | Delete commit |
| `exec` (or `x`) | Run a shell command |

Reorder lines to reorder commits.

**Common cleanup before opening PR:**
```
pick   a1b2c3d feat: add login endpoint
fixup  b2c3d4e fix typo
fixup  c3d4e5f fix another typo
pick   d4e5f6g add validation
fixup  e5f6g7h address review comments
```
Result: 2 clean commits instead of 5.

### ⚠️ The Golden Rule of Rebasing

> **Never rebase commits that have been pushed and shared with others.**

Why? Rebasing **creates new commit SHAs**. If someone else pulled the old commits, their history diverges from yours. Chaos.

Safe scenarios:
- Rebasing **your own local feature branch** onto latest `main`
- Cleaning up local commits before pushing
- Squashing on a feature branch nobody else uses

### When to merge vs rebase

| Situation | Choose |
|---|---|
| Integrating finished feature into `main` | `merge` (preserves "this work was a unit" context) |
| Keeping your in-progress feature up to date with `main` | `rebase` (clean, no merge bubbles) |
| Cleaning up messy WIP commits before PR | `rebase -i` |
| Shared branch with multiple contributors | `merge` (rebasing rewrites their history) |
| Personal branch, want linear log | `rebase` |

---

## 9. Cherry-pick

Copy a single commit (or range) onto your current branch.

### Visual

```
Before:
        A──B──C   ← main
         \
          D──E──F   ← develop


After git switch main && git cherry-pick E:
        A──B──C──E'   ← main  (E' is a new commit with same changes as E)
         \
          D──E──F   ← develop  (E still here)
```

### Commands

```bash
git cherry-pick a1b2c3d                  # one commit
git cherry-pick a1b2c3d b2c3d4e          # multiple
git cherry-pick a1b2c3d..b2c3d4e         # range (exclusive of first)
git cherry-pick a1b2c3d^..b2c3d4e        # range (inclusive)
git cherry-pick -n a1b2c3d               # apply changes but DON'T commit
git cherry-pick -x a1b2c3d               # add "(cherry picked from ...)" footer
git cherry-pick --continue / --abort     # after conflicts
```

### Real example

Bug fix `a1b2c3d` was pushed to `develop`. Production runs `release/v1.0`. You need the fix in both:

```bash
git switch release/v1.0
git cherry-pick a1b2c3d
git push origin release/v1.0
```

---

## 10. Stash

Temporarily shelve uncommitted changes.

### Visual

```
WORKING DIR has changes  ──► git stash ──►  WORKING DIR is clean
                                            (changes saved to "stash stack")

Later: git stash pop                     ──►  WORKING DIR has changes back
```

### Commands

```bash
git stash                       # shelve tracked changes
git stash -u                    # include untracked files
git stash -a                    # include EVERYTHING (even ignored)
git stash push -m "WIP login fix"      # save with message
git stash list                  # all stashes
# stash@{0}: WIP on main: ...
# stash@{1}: On feature/login: WIP fix

git stash show                  # summary of latest stash
git stash show -p stash@{1}     # full diff of specific stash

git stash apply                 # apply latest (KEEPS stash)
git stash apply stash@{1}       # apply specific
git stash pop                   # apply + DELETE from stack

git stash drop stash@{0}        # delete one
git stash clear                 # delete all (no undo!)

git stash branch fix-it stash@{0}   # create a new branch from a stash
```

### Classic scenario

You're mid-feature when an urgent prod bug appears:

```bash
# Working on feature/login, files modified
git stash push -m "halfway through login form"

git switch main
git pull
git switch -c hotfix/payment-crash
# ... fix bug, commit, PR, merge ...

git switch feature/login
git stash pop                    # resume exactly where you left off
```

---

## 11. Reset vs Revert vs Restore

These three commands all "undo" things — but they're profoundly different.

### Quick comparison

| Command | What it does | History rewritten? | Safe to use on shared branches? |
|---|---|---|---|
| `git restore` | Discards file changes (working dir or staging) | No | Yes |
| `git revert` | Creates a NEW commit that undoes a previous commit | No | **Yes** ✅ |
| `git reset` | Moves branch pointer; can discard commits/changes | **Yes** | **No** ❌ |

### `git restore` — undo file changes (Git ≥ 2.23)

```bash
git restore Book.java                    # discard unstaged changes to file
git restore .                            # discard ALL unstaged changes (DANGER)
git restore --staged Book.java           # unstage (file goes back to modified)
git restore --source=HEAD~2 Book.java    # get file content from 2 commits ago
```

### `git revert` — safe undo via new commit

```bash
git revert a1b2c3d              # creates new commit "Revert <msg>"
git revert HEAD                 # revert most recent commit
git revert HEAD~3..HEAD         # revert last 3 commits (creates 3 new revert commits)
git revert -n a1b2c3d           # revert but DON'T commit yet (lets you combine)
git revert a1b2c3d -m 1         # revert a MERGE commit (mainline = parent 1)
```

**Visual:**
```
Before:  C1──C2──C3──C4   ← main
                    ▲
                    bug is in C3

After git revert C3:
         C1──C2──C3──C4──C3'   ← main
                          ▲
                          new commit that undoes C3's changes
```

### `git reset` — rewrite history (powerful, dangerous)

Three modes, controlling what happens to working dir & staging:

```
                  ┌──────────────────┬─────────────────┬─────────────────┐
                  │   Branch ptr     │   Staging area  │  Working dir    │
                  ├──────────────────┼─────────────────┼─────────────────┤
  reset --soft    │   Moved          │   Untouched     │   Untouched     │
  reset --mixed   │   Moved          │   Reset to HEAD │   Untouched     │  ← DEFAULT
  reset --hard    │   Moved          │   Reset to HEAD │   RESET (LOST!) │
                  └──────────────────┴─────────────────┴─────────────────┘
```

```bash
git reset --soft  HEAD~1     # undo last commit; changes stay STAGED
git reset --mixed HEAD~1     # undo last commit; changes stay UNSTAGED (default)
git reset --hard  HEAD~1     # undo last commit; CHANGES DELETED
git reset --hard origin/main # nuke local commits, match remote exactly

git reset HEAD Book.java     # unstage Book.java (equivalent to restore --staged)
```

**Visual of `--soft`:**
```
Before:  C1──C2──C3   ← main, HEAD
                  ▲
                  staged: nothing
                  working: clean

After git reset --soft HEAD~1:
         C1──C2   ← main, HEAD
              ▲
              staged: all of C3's changes
              working: clean
              (C3 is unreachable but not deleted — recoverable with reflog)
```

### Recovery via reflog

Git keeps a local log of HEAD movements for ~30 days, even for "deleted" commits.

```bash
git reflog
# a1b2c3d HEAD@{0}: reset: moving to HEAD~1
# 9f8e7d6 HEAD@{1}: commit: feat(book): add ISBN search   ← the "lost" commit
# ...

git reset --hard 9f8e7d6     # bring it back!
```

This makes `--hard` recoverable in most cases — but **only if you remember within 30 days and the data wasn't pruned**.

### Rule of thumb

| Situation | Use |
|---|---|
| "I made a typo in the file I haven't committed" | `git restore <file>` |
| "I staged the wrong file" | `git restore --staged <file>` |
| "I want to undo a pushed commit safely" | `git revert <sha>` |
| "I want to drop my last 2 local commits, keep changes" | `git reset --soft HEAD~2` |
| "I want to start over from scratch" | `git reset --hard origin/main` |
| "I rebased and lost commits" | `git reflog` → `git reset --hard <sha>` |

---

## 12. `.gitignore`

Tells Git which files to **never track**. Lives at repo root (can also exist in subfolders).

### Example for this Java/Spring Boot project

```gitignore
###################
# Build artifacts #
###################
target/
build/
out/
*.class
*.jar
*.war
*.ear

#######
# IDE #
#######
# IntelliJ
.idea/
*.iml
*.ipr
*.iws

# Eclipse
.settings/
.project
.classpath
.factorypath

# VS Code
.vscode/
*.code-workspace

# NetBeans
/nbproject/private/
/nbbuild/
/dist/

##################
# Maven / Gradle #
##################
.mvn/wrapper/maven-wrapper.jar
!.mvn/wrapper/maven-wrapper.properties
.gradle/

##############
# OS clutter #
##############
.DS_Store
Thumbs.db
desktop.ini

########
# Logs #
########
*.log
logs/

#####################
# Local environment #
#####################
.env
.env.local
application-local.properties
application-secret.properties

###################
# Test coverage  #
###################
coverage/
*.exec
jacoco.xml
```

### Pattern syntax cheat sheet

| Pattern | Matches | Example |
|---|---|---|
| `file.txt` | A specific file anywhere | `notes.txt` in any folder |
| `/file.txt` | Only at repo root | Just `/notes.txt`, not `docs/notes.txt` |
| `folder/` | Any folder with that name | `target/` anywhere |
| `*.log` | Any file ending in `.log` | `app.log`, `error.log` |
| `**/temp` | `temp` in any subdir | `a/temp`, `a/b/c/temp` |
| `docs/**` | Everything inside `docs/` | `docs/a.md`, `docs/x/y.md` |
| `!important.log` | Exception — DO track | overrides `*.log` |
| `# comment` | Ignored line | — |

### Critical gotcha

> `.gitignore` **only affects untracked files**.

If you already committed `secrets.properties`, adding it to `.gitignore` does nothing — Git still tracks it. To stop:

```bash
git rm --cached secrets.properties
echo "secrets.properties" >> .gitignore
git commit -m "chore: stop tracking secrets.properties"
git push
```

### Globally ignore files (per-user, not per-repo)

```bash
git config --global core.excludesFile ~/.gitignore_global
```
Put OS/IDE files there (e.g. `.DS_Store`, `.idea/`). Keeps team `.gitignore` clean.

### Useful tools
- https://www.toptal.com/developers/gitignore — generates a `.gitignore` for any stack
- `git check-ignore -v <file>` — explain why a file is ignored

---

## 13. Pull Requests & Code Reviews

A **Pull Request (PR)** is a GitHub feature (called "Merge Request" on GitLab) — a formal proposal to merge one branch into another, with discussion, review, and CI built in.

### Anatomy of a PR

```
┌────────────────────────────────────────────────────────────────┐
│ Pull Request #42                                               │
├────────────────────────────────────────────────────────────────┤
│ Title:  feat(book): add ISBN search endpoint                   │
│ Branch: feature/isbn-search ──► main                           │
│                                                                │
│ Description:                                                   │
│   • Adds GET /api/books/search?isbn=...                        │
│   • Uses derived query method                                  │
│   • Closes #38                                                 │
│                                                                │
│ ┌──── Conversation ────┬──── Commits (3) ────┬──── Files ────┐ │
│ │                      │                     │                │ │
│ │  @reviewer commented:│  a1b2c3d feat: ...  │  +35  -2       │ │
│ │   "Add a test?"      │  b2c3d4e test: ...  │  Book.java     │ │
│ │                      │  c3d4e5f docs: ...  │  BookController│ │
│ │  @author replied:    │                     │  BookTest.java │ │
│ │   "Added in c3d4..."│                     │                │ │
│ │                      │                     │                │ │
│ └──────────────────────┴─────────────────────┴────────────────┘ │
│                                                                │
│  ✅ All checks passed                                          │
│  ✅ Approved by @senior-dev                                    │
│                                                                │
│       [ Squash and merge ]  [ Merge ]  [ Rebase ]              │
└────────────────────────────────────────────────────────────────┘
```

### Lifecycle

```
   1. Branch off main      2. Push + Open PR     3. Review + CI
   ───────────────────────► ────────────────────► ────────────────►
                                                           │
                                                           ▼
   6. Delete branch    5. Merge to main      4. Address feedback
   ◄─────────────────  ◄──────────────────── ◄──────────────────
```

### Why PRs?

| Benefit | What it means |
|---|---|
| **Code review** | Catch bugs before they ship, share knowledge |
| **CI/CD gates** | Tests/lint must pass before merge |
| **Audit trail** | Every change documented with discussion |
| **Knowledge sharing** | Reviewers learn the codebase |
| **Quality** | Forces a second pair of eyes |
| **Security** | Sensitive changes get extra scrutiny |

### Three merge strategies on GitHub

```
═══════════════════════════════════════════════════════════════════
 1. MERGE COMMIT (preserves all history + merge commit)
═══════════════════════════════════════════════════════════════════
        F1──F2──F3──┐
       /             \
   C1──C2──C3─────────MC ◄── main
       └ merge commit shows "PR #42 merged"
       └ all 3 feature commits visible
       └ true history preserved


═══════════════════════════════════════════════════════════════════
 2. SQUASH AND MERGE (collapses to ONE commit) ⭐ most popular
═══════════════════════════════════════════════════════════════════
   C1──C2──C3──[F1+F2+F3 squashed] ◄── main
                      ▲
                      Single commit on main with PR title
                      Individual feature commits LOST
                      Cleanest main history


═══════════════════════════════════════════════════════════════════
 3. REBASE AND MERGE (linear, no merge commit)
═══════════════════════════════════════════════════════════════════
   C1──C2──C3──F1'──F2'──F3' ◄── main
                  ▲
                  Each feature commit replayed individually
                  No merge commit
                  Linear timeline
```

| Strategy | Best for | Drawback |
|---|---|---|
| Merge commit | Preserving feature context | Cluttered log |
| **Squash** | Clean `main` history, small PRs | Loses commit granularity |
| Rebase | Linear log, keep commit detail | Rewrites SHAs |

### Code review best practices

**As an author:**
- Self-review your own PR diff first
- Keep PRs small (< 400 lines ideal, < 1000 max)
- Write a clear description: what, why, screenshots, ticket link
- Respond to every comment (resolve or discuss)
- Push fixes as new commits; squash later

**As a reviewer:**
- Review **the code, not the person**
- Ask questions instead of demanding ("What do you think about…?")
- Distinguish blocking issues from nits ("nit:" prefix)
- Look for: correctness, tests, security, performance, readability
- Approve when you'd be happy maintaining this code

### PR template (`.github/pull_request_template.md`)

```markdown
## Summary
Brief description of what this PR does.

## Why
What problem does this solve? Link to ticket.

## Changes
- Bullet list of key changes

## Testing
- [ ] Unit tests added/updated
- [ ] Manually tested locally
- [ ] CI passes

## Screenshots (if UI change)

## Checklist
- [ ] Follows conventional commits
- [ ] No breaking changes (or documented)
- [ ] Updated docs
```

---

## 14. GitHub Flow

The simplest, most popular branching strategy. Used by GitHub itself.

### The five rules

```
1.  main is always deployable   ──── never push broken code to main
2.  Branch for every change     ──── feature/, fix/, chore/
3.  Push and open PR early      ──── visibility, early feedback
4.  Review + CI must pass       ──── automated gate before merge
5.  Merge → deploy → delete     ──── close the loop
```

### Diagram

```
                                          ┌────► [Deploy to prod]
                                          │
   main ────────────────────────●─────────●─────────●────────────►
                                ▲          ▲         ▲
                                │ merge    │ merge   │ merge
                                │          │         │
                feature/login ──┘          │         │
                                fix/typo───┘         │
                                          chore/deps─┘
```

### Branch naming convention

| Prefix | Purpose | Example |
|---|---|---|
| `feature/` | New user-facing feature | `feature/isbn-search` |
| `fix/` | Bug fix | `fix/null-on-empty-author` |
| `hotfix/` | Urgent production fix | `hotfix/payment-crash` |
| `chore/` | Maintenance, deps, refactor | `chore/upgrade-postgres-driver` |
| `docs/` | Documentation only | `docs/api-endpoints` |
| `test/` | Test additions only | `test/borrow-service-coverage` |
| `refactor/` | Code restructure, no behavior change | `refactor/extract-validator` |

### GitHub Flow vs Git Flow

```
GITHUB FLOW (simple, modern)
═══════════════════════════════════════
       main ──────●─────●─────●────►
                  ▲     ▲     ▲
              feature  fix   chore


GIT FLOW (heavier, older — Vincent Driessen 2010)
═══════════════════════════════════════
       main ───────────●─────●─────────────●──────►
                       ▲     ▲             ▲
                     release  hotfix      release
                       ▲                    ▲
     develop ─────●────●─────●──────●───────●──────►
                  ▲    ▲     ▲      ▲
                feat feat  feat   feat
```

| | GitHub Flow | Git Flow |
|---|---|---|
| **Branches** | `main` + feature | `main`, `develop`, `release/*`, `hotfix/*`, feature |
| **Deploys** | Continuous (merge → deploy) | Releases (cut from `develop`) |
| **Fits** | Web apps, SaaS, CI/CD | Versioned releases, native apps |
| **Complexity** | Low | High |

> **Use GitHub Flow** unless you have a strong reason not to.

### Protected branches

On GitHub: Settings → Branches → Add rule for `main`:
- ✅ Require PR before merging
- ✅ Require approvals (1-2)
- ✅ Require status checks (CI must pass)
- ✅ Require up-to-date with base branch
- ✅ Restrict force-push
- ✅ Restrict deletion

---

## 15. Conventional Commits

A formal spec for commit messages, parseable by humans **and** tools.

### Format

```
<type>(<scope>): <short summary>     ← header (≤ 72 chars)
<blank line>
<body>                                ← optional, detailed explanation
<blank line>
<footer>                              ← optional, references or breaking changes
```

### Common types

| Type | When to use | Version bump |
|---|---|---|
| `feat` | New feature for users | **minor** |
| `fix` | Bug fix for users | **patch** |
| `docs` | Docs only | none |
| `style` | Whitespace, formatting, semicolons | none |
| `refactor` | Code change, no behavior change | none |
| `perf` | Performance improvement | patch |
| `test` | Adding/fixing tests | none |
| `build` | Build system / dependencies | none |
| `ci` | CI config (GitHub Actions, etc.) | none |
| `chore` | Maintenance, no production code | none |
| `revert` | Reverts a previous commit | varies |
| `BREAKING CHANGE` (in footer or `!`) | Backwards-incompatible | **major** |

### Examples — good ✅

```
feat(book): add ISBN search endpoint

Adds GET /api/books/search?isbn=... using a Spring Data JPA derived query.
Returns 404 if no book matches.

Closes #42
```

```
fix(borrow): prevent borrowing when availableCopies is 0

Previously, BorrowService allowed creating a BorrowRecord even when
the book had no available copies. Now throws IllegalOperationException.

Fixes #58
```

```
chore(deps): replace h2 driver with postgresql
```

```
refactor(service): extract email validation into helper

No behavior change.
```

```
feat(api)!: rename /members endpoint to /users

BREAKING CHANGE: clients calling GET/POST/PUT /members must now
use /users. Update your API integrations.
```

### Examples — bad ❌

```
update
fixed it
asdf
stuff
WIP
final
.
final v2
```

### Why bother?

1. **Auto-generated changelogs** — tools like `standard-version`, `semantic-release`, `release-please` parse your commits into a `CHANGELOG.md`.
2. **Automatic versioning** — `feat` → minor bump, `fix` → patch, `BREAKING CHANGE` → major. No human deciding.
3. **Filterable history** — `git log --grep="^feat"` shows only features.
4. **Onboarding** — new devs can read a year of history and understand the project's evolution.
5. **Discipline** — forces thoughtful commits ("what TYPE of change is this?").

### Tools

- **commitlint** + **husky** — block bad commits at commit time
- **commitizen** — interactive CLI to format messages (`git cz` instead of `git commit`)
- **semantic-release** — full automation: changelog + version + tag + GitHub release

---

## 16. Real-World Scenarios

Concrete recipes for common situations.

### Scenario 1: "I committed to the wrong branch"

```bash
# You committed to main but meant feature/login
git switch feature/login
git cherry-pick main          # copy the commit here
git switch main
git reset --hard HEAD~1       # remove it from main
```

### Scenario 2: "I want to discard ALL my local changes"

```bash
git restore .                 # unstaged
git restore --staged .        # unstage everything
# or nuke from orbit:
git reset --hard HEAD
git clean -fd                 # also delete untracked files/folders
```

### Scenario 3: "I accidentally committed secrets"

```bash
# Before pushing:
git reset --soft HEAD~1
# Edit file to remove secret
git add .
git commit -m "feat: ..."

# After pushing (BAD — secret is now in history):
# Option A: rewrite history with BFG Repo Cleaner or git-filter-repo
# Option B: rotate the secret IMMEDIATELY (assume compromised)
```

### Scenario 4: "Update my feature branch with latest main"

**Option A — rebase (clean):**
```bash
git switch feature/login
git fetch origin
git rebase origin/main
# resolve conflicts if any
git push --force-with-lease    # rebase rewrote history
```

**Option B — merge (safe):**
```bash
git switch feature/login
git fetch origin
git merge origin/main
git push
```

### Scenario 5: "I want to undo a commit that's already pushed"

```bash
git revert a1b2c3d
git push
```
(Don't use `reset --hard` + force-push on a shared branch unless you've coordinated with the team.)

### Scenario 6: "Squash my 8 messy commits before opening PR"

```bash
git rebase -i origin/main
# Mark first as 'pick', rest as 'squash' or 'fixup'
# Save & edit final commit message
git push --force-with-lease
```

### Scenario 7: "Find which commit broke the build"

```bash
git bisect start
git bisect bad                            # current is broken
git bisect good v1.0.0                    # this tag worked
# Git checks out a midpoint — run your tests
git bisect bad   # or good
# Repeat. Git narrows down via binary search.
git bisect reset
```

### Scenario 8: "I deleted a branch I needed"

```bash
git reflog
# Find the SHA of the last commit on that branch
git switch -c restored-branch a1b2c3d
```

### Scenario 9: "Make a release"

```bash
git switch main
git pull
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# On GitHub: Releases → Draft → pick tag → paste changelog
```

### Scenario 10: "Working with forks"

```bash
git clone https://github.com/MY-USERNAME/library-management.git
cd library-management
git remote add upstream https://github.com/ORIGINAL-OWNER/library-management.git

# Stay in sync with upstream
git fetch upstream
git switch main
git merge upstream/main
git push origin main
```

---

## 17. Cheat Sheet

```bash
# ════════ SETUP ════════
git config --global user.name "Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
git config --global pull.rebase false

# ════════ START ════════
git init
git clone <url>
git clone --depth 1 <url>        # shallow

# ════════ EVERYDAY ════════
git status
git add .
git add -p                         # interactive
git commit -m "feat: ..."
git commit --amend
git push
git pull
git pull --rebase

# ════════ BRANCHES ════════
git branch                         # list
git switch <branch>                # change
git switch -c <branch>             # create + switch
git branch -d <branch>             # delete
git push origin --delete <branch>  # delete remote

# ════════ SYNC WITH MAIN ════════
git fetch origin
git rebase origin/main             # OR git merge origin/main

# ════════ INSPECT ════════
git log --oneline --graph --all --decorate
git diff
git diff --staged
git diff main..feature
git show <sha>
git blame <file>
git reflog

# ════════ UNDO ════════
git restore <file>                 # discard unstaged
git restore --staged <file>        # unstage
git commit --amend                 # edit last
git revert <sha>                   # safe undo
git reset --soft  HEAD~1           # undo commit, keep staged
git reset --mixed HEAD~1           # undo commit, keep unstaged
git reset --hard  HEAD~1           # NUKE last commit
git reset --hard origin/main       # match remote exactly
git clean -fd                      # delete untracked files

# ════════ STASH ════════
git stash push -m "WIP"
git stash list
git stash pop
git stash apply stash@{1}
git stash drop stash@{0}

# ════════ MERGE / REBASE / CHERRY-PICK ════════
git merge <branch>
git merge --no-ff <branch>
git merge --squash <branch>
git rebase <branch>
git rebase -i HEAD~5
git rebase --continue / --abort
git cherry-pick <sha>

# ════════ REMOTES ════════
git remote -v
git remote add origin <url>
git remote set-url origin <url>
git push -u origin <branch>
git fetch --all --prune

# ════════ TAGS ════════
git tag -a v1.0.0 -m "Release"
git tag                            # list
git push origin v1.0.0
git push --tags

# ════════ ADVANCED ════════
git bisect start / good / bad / reset
git worktree add ../hotfix main
git submodule add <url>
```

---

## 18. Glossary

| Term | Definition |
|---|---|
| **Repository (repo)** | A project tracked by Git (the `.git/` folder + files) |
| **Working directory** | The files you see and edit |
| **Staging area / Index** | A holding zone for changes that will go in the next commit |
| **Commit** | A snapshot of staged changes, with author/message/parent |
| **Branch** | A movable pointer to a commit |
| **HEAD** | Pointer to the commit you're currently on |
| **Remote** | A version of the repo hosted elsewhere (e.g. GitHub) |
| **Origin** | Default name for the remote you cloned from |
| **Upstream** | The branch your local branch tracks |
| **Fast-forward** | A merge with no divergence — just moves the pointer |
| **Merge commit** | A commit with two (or more) parents, from merging branches |
| **Conflict** | When Git can't auto-merge changes to the same lines |
| **Detached HEAD** | HEAD points to a commit, not a branch (no anchor) |
| **Reflog** | Local log of HEAD movements (recovery tool) |
| **Tag** | A named pointer to a specific commit (usually for releases) |
| **Fork** | A personal copy of someone else's repo on GitHub |
| **Pull Request (PR)** | A request to merge a branch, with review/CI |
| **Squash** | Combine multiple commits into one |
| **Cherry-pick** | Copy a specific commit onto another branch |
| **Stash** | Shelve uncommitted changes temporarily |
| **Rebase** | Replay commits on a new base, rewriting history |
| **Revert** | Create a new commit that undoes a previous one |
| **Reset** | Move branch pointer; can discard changes |

---

## Further Reading

- 📘 [Pro Git Book](https://git-scm.com/book/en/v2) — free, official, comprehensive
- 🎮 [Learn Git Branching](https://learngitbranching.js.org) — interactive visualizer
- 📜 [Conventional Commits Spec](https://www.conventionalcommits.org)
- 🌊 [GitHub Flow Guide](https://docs.github.com/en/get-started/quickstart/github-flow)
- 🛠️ [Oh Shit, Git!?!](https://ohshitgit.com) — recovery recipes for "what did I just do"
- 🎓 [Atlassian Git Tutorials](https://www.atlassian.com/git/tutorials)
- 🔍 [git-scm Reference](https://git-scm.com/docs) — official man pages

---

> *"Git gets easier once you get the basic idea that branches are homeomorphic endofunctors mapping submanifolds of a Hilbert space."* — joke from Stack Overflow.
>
> Translation: **branches are just pointers to commits.** That's it. Everything else follows.
