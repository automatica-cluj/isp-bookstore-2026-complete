# Contributing

This repository uses an automated release pipeline: every time something lands on `main` or `develop`, GitHub Actions decides whether to cut a new version, builds the Docker image, publishes it to GHCR, and updates the `CHANGELOG`. The only thing **you** have to do is write your commit/PR title in [Conventional Commits](https://www.conventionalcommits.org/) format.

---

## Branch strategy

| Branch           | Purpose                     | Release channel               |
| ---------------- | --------------------------- | ----------------------------- |
| `main`           | Stable code                 | Stable — `v1.2.3`             |
| `develop`        | Integration for next stable | Prerelease — `v1.3.0-beta.1`  |
| `feat/...` etc.  | Topic branches              | No release                    |

Topic branches are cut from `develop`, merged back into `develop` via PR, and periodically `develop` is merged into `main` to cut a stable release.

---

## Conventional Commits

Commit messages (and **PR titles**) must follow the pattern:

```
<type>[optional scope][!]: <lowercase subject>
```

| What you did                   | Example                               | Release        |
| ------------------------------ | ------------------------------------- | -------------- |
| New feature                    | `feat: add order history endpoint`    | minor bump     |
| New feature (scoped)           | `feat(cart): persist cart to DB`      | minor bump     |
| Bug fix                        | `fix: prevent duplicate carts`        | patch bump     |
| Performance improvement        | `perf: cache book list`               | patch bump     |
| Internal refactor              | `refactor: extract JWT filter`        | patch bump     |
| Breaking change                | `feat!: replace JWT with OAuth2`      | **major bump** |
| Breaking change (body variant) | Body contains `BREAKING CHANGE: ...`  | **major bump** |
| Documentation                  | `docs: clarify HTTPS setup`           | no release     |
| Tests only                     | `test: add cart controller tests`     | no release     |
| CI / build                     | `ci: bump setup-java to v4`           | no release     |
| Dependencies (Dependabot)      | `chore(deps): bump postgres to 16.4`  | no release     |
| Chore / maintenance            | `chore: reorganise .env files`        | no release     |

A scope of `no-release` suppresses a release even for `feat:` / `fix:` — useful for work-in-progress that you don't want published yet:

```
feat(no-release): draft new author-search API
```

---

## Workflow

1. **Branch from `develop`:** `git checkout -b feat/cart-persistence develop`
2. **Commit** using Conventional Commits. Individual commit messages inside a PR don't have to be perfect — the PR title is what counts (see below).
3. **Open a PR** targeting `develop`. The PR title **must** be a valid conventional-commit header; a GitHub Action (`PR Title`) blocks merges otherwise.
4. **Wait for CI** (tests + package). Fix anything red.
5. **Squash-and-merge.** The PR title becomes the single commit on `develop`, so that's the line `semantic-release` will read.
6. **Promote to stable:** when `develop` is ready, open a `chore: release X.Y.Z` PR from `develop` → `main` and merge it. The pipeline tags `vX.Y.Z`, updates `CHANGELOG.md`, and publishes the image.

### What triggers a release?

- Push to `main`, commits since the last tag include `feat:`, `fix:`, `perf:`, `refactor:`, or a breaking change → a new stable release (`vX.Y.Z`).
- Push to `develop`, same commit types → a new beta prerelease (`vX.Y.Z-beta.N`).
- Only `docs:` / `chore:` / `test:` / `ci:` / `style:` / `build:` → **no release**, but the image is still rebuilt and pushed with `sha-<short>` and branch tags.

---

## First-time repo setup (maintainer checklist)

Do these once, in the GitHub UI, after the first push:

1. **Settings → General → Pull Requests**
   - Allow squash merging: **on**
   - Allow merge commits / rebase merging: **off**
   - Default commit message for squash merges: **Pull request title**
2. **Settings → Branches → Branch protection rules** — add a rule for `main`:
   - Require a pull request before merging
   - Require status checks to pass: `Build and Test`, `Validate conventional-commit title`
   - Do the same for `develop`
3. **Settings → Actions → General → Workflow permissions**
   - **Read and write permissions** (so `@semantic-release/git` can push the release commit and tag)
   - Allow GitHub Actions to create and approve pull requests: **on**
4. **Settings → Packages** (after the first image is published)
   - Make the image public (course artifact)
   - Link the package to this repository
