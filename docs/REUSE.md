# Reuse this repo for your own project

This repository ships a complete, working CI/CD setup: tests on every push, automatic
semantic versioning from your commit/PR titles, a `CHANGELOG`, GitHub Releases, and a
multi-arch Docker image published to GHCR. You can lift the whole thing into your own
repository and have it working in a few minutes.

The GitHub Actions workflows are **portable as-is** — `build-push.yml` derives the image
name from `${{ github.repository }}` and authenticates with the auto-provided
`GITHUB_TOKEN`, so they adapt to whatever repo they run in with no edits.

---

## 1. Get the code into your repository

**Recommended — use as a template** (cleanest history):

1. On the GitHub page, click **Use this template → Create a new repository**.
2. A template gives you a fresh repo with **no commit history, tags, or releases**, so
   `semantic-release` starts from scratch (your first release will be `v1.0.0`).

> ⚠️ A template copies *files*, so `CHANGELOG.md` still contains this project's history and
> `pom.xml` still says `1.6.x`. Because there are no git tags, the first release is `v1.0.0`
> regardless — but to avoid a confusing "1.0.0 stacked above 1.6.2" changelog, reset those
> two files first (see the helper snippet in §5).

**Alternatives:**

- **Fork** — carries the existing tags/history, so versions *continue* from the current
  number instead of restarting. Note: forks have Actions **disabled by default** (enable
  them under the fork's **Actions** tab).
- **Clone + push to a new empty repo:**
  ```bash
  git clone https://github.com/automatica-cluj/isp-bookstore-2026-complete.git my-app
  cd my-app
  rm -rf .git && git init
  git add . && git commit -m "chore: initial import"
  git remote add origin git@github.com:<you>/<your-repo>.git
  git push -u origin main
  ```

---

## 2. Configure the repository (one-time)

These are the same steps as the "First-time repo setup" checklist in
[`CONTRIBUTING.md`](../CONTRIBUTING.md).

1. **Settings → Actions → General → Workflow permissions**
   - Select **Read and write permissions**.
   - Tick **Allow GitHub Actions to create and approve pull requests**.

   > **This is the critical step.** Without write permission the release job can't push the
   > `chore(release): X.Y.Z` commit and tag, and it fails with an HTTP 403.

2. **Settings → General → Pull Requests**
   - Allow **squash merging**; turn off merge commits and rebase merging.
   - Set **Default commit message for squash merges → Pull request title**.

3. **Settings → Branches → Branch protection rule** for `main`
   - Require a pull request before merging.
   - Require status checks: **`Build and Test`** and **`Validate conventional-commit title`**.

---

## 3. Secrets — none required

`GITHUB_TOKEN` is injected automatically by GitHub Actions. The workflow's
`packages: write` permission lets it publish to `ghcr.io/<your-owner>/<your-repo>`. You do
**not** need a personal access token or any Docker Hub credentials.

---

## 4. Cut your first release

1. Branch from `main`, make a change, open a PR with a Conventional-Commit title
   (e.g. `feat: add hello endpoint`).
2. Squash-merge it. Watch the **Actions** tab:
   - `semantic-release` computes the version, updates `CHANGELOG.md`, bumps `pom.xml`,
     tags it, and creates a GitHub Release.
   - The Docker image is built and pushed to GHCR.
3. The GHCR package starts **private**. To allow anonymous `docker pull`, go to the package
   page → **Package settings → Change visibility → Public**, and link it to the repository.

See [`RELEASE-WALKTHROUGH.md`](RELEASE-WALKTHROUGH.md) for a click-by-click version of this.

---

## 5. Replace the hardcoded owner/repo (cosmetic)

The workflows and `release.config.js` need **no changes**. Only docs, the compose default
image, and the Docker image-source labels mention the original owner. Run this from the repo
root to swap them in one go (works on macOS and Linux):

```bash
OLD="automatica-cluj/isp-bookstore-2026-complete"
NEW="<your-owner>/<your-repo>"            # e.g. octocat/my-bookstore

grep -rl "$OLD" README.md docs/ compose.prod.yml Dockerfile Dockerfile.runtime \
  | xargs perl -pi -e "s{\Q$OLD\E}{$NEW}g"
```

If you used a template and want a clean version history, also reset the release-managed
files **before** your first release:

```bash
# Blank the changelog down to its header
cat > CHANGELOG.md <<'EOF'
# Changelog

All notable changes to this project will be documented in this file.
EOF

# Reset the project version (semantic-release will set the real one on first release)
mvn versions:set -DnewVersion=0.0.0 -DgenerateBackupPoms=false
```

Commit these with a no-release title, e.g. `chore: reset project metadata for reuse`.

---

## What you get out of the box

| Capability | Where |
| ---------- | ----- |
| Tests + JAR build on every push/PR | `.github/workflows/ci.yml` |
| Conventional-commit PR-title enforcement | `.github/workflows/pr-title.yml` |
| Auto versioning + CHANGELOG + GitHub Release | `.github/workflows/build-push.yml` + `release.config.js` |
| Multi-arch image (`amd64` + `arm64`) on GHCR | `.github/workflows/build-push.yml` + `Dockerfile.runtime` |
| Weekly dependency updates (majors ignored) | `.github/dependabot.yml` |
| Local dev stack (Caddy + Postgres + app) | `compose.dev.yml`, `Dockerfile` |

The release model is **trunk-based**: everything ships from `main`, and your PR title decides
the version bump. Full rules in [`CONTRIBUTING.md`](../CONTRIBUTING.md).
