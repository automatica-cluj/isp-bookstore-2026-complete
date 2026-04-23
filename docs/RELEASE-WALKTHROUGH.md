# Release walkthrough — from GitHub UI

End-to-end example: add a small feature entirely through the GitHub web UI (no local clone) and watch the automated release pipeline publish a **beta prerelease** and then a **stable release**.

> **Prerequisites:** you already have Write access to the repository and the first-time maintainer checklist in `CONTRIBUTING.md` has been done.

---

## How the pipeline decides what to release

The release is driven entirely by the **PR title** (because PRs are squash-merged, the PR title becomes the only commit message on `develop` / `main`). `semantic-release` reads that title:

| PR title prefix                        | Effect on version           |
| -------------------------------------- | --------------------------- |
| `feat: ...`                            | minor bump (1.2.0 → 1.3.0)  |
| `fix:` / `perf:` / `refactor:`         | patch bump (1.2.0 → 1.2.1)  |
| `feat!: ...` or `BREAKING CHANGE:` body | major bump (1.2.0 → 2.0.0)  |
| `docs:` / `chore:` / `test:` / `ci:`   | **no release**              |

Branch channels:

- `develop` → **prerelease** `vX.Y.Z-beta.N` (channel `beta`)
- `main` → **stable** `vX.Y.Z`

---

## The demo feature

Add a public endpoint `GET /api/books/count` that returns the total number of books.

Two files change:

1. `src/main/java/com/bookstore/service/BookService.java` — new `countAll()` method
2. `src/main/java/com/bookstore/controller/BookController.java` — new `@GetMapping("/count")`

The endpoint is public by default (the existing `SecurityConfig` already permits `GET /api/books/**`).

---

## Step 1 — Create a topic branch from `develop`

1. On the repo page, click the **branch dropdown** (top-left of the file list).
2. Type `feat/books-count` in the search box.
3. Above the dropdown, switch **Source** to `develop`.
4. Click **Create branch: feat/books-count from 'develop'**.

You are now viewing the repo on the new branch.

---

## Step 2 — Edit `BookService.java`

1. Open `src/main/java/com/bookstore/service/BookService.java`.
2. Click the **pencil** (✏️) icon → **Edit this file**.
3. Inside the class, just below `findAll()`, add:

   ```java
       public long countAll() {
           return bookRepository.count();
       }
   ```

4. At the bottom, under **Commit changes**:
   - Commit message: `add BookService.countAll`
   - Leave the **extended description** empty.
   - Keep **Commit directly to the `feat/books-count` branch** selected.
   - Click **Commit changes**.

---

## Step 3 — Edit `BookController.java`

1. Open `src/main/java/com/bookstore/controller/BookController.java` (make sure the branch selector still says `feat/books-count`).
2. Click **Edit this file**.
3. Just below the existing `getAllBooks()` method, add:

   ```java
       @GetMapping("/count")
       public ResponseEntity<Long> countBooks() {
           return ResponseEntity.ok(bookService.countAll());
       }
   ```

4. Commit message: `add GET /api/books/count endpoint` → **Commit changes**.

---

## Step 4 — Open the Pull Request

1. A yellow banner appears: **Compare & pull request**. Click it.
   (Alternatively: **Pull requests** tab → **New pull request** → base: `develop`, compare: `feat/books-count`.)
2. Fill in:
   - **base:** `develop`, **compare:** `feat/books-count` (verify both!)
   - **Title** — this matters, it drives the release:
     ```
     feat: add GET /api/books/count endpoint
     ```
   - **Description:** optional, short summary.
3. Click **Create pull request**.

### What to watch for

Two required checks run:

- `Build and Test` — `mvn test` (from `.github/workflows/ci.yml`)
- `Validate conventional-commit title` — fails if the PR title doesn't match the Conventional Commits grammar (from `.github/workflows/pr-title.yml`)

Fix the title in-place if the second check fails — it re-runs automatically.

---

## Step 5 — Squash-and-merge into `develop`

1. When both checks are green, click **Squash and merge**.
2. **Verify the squash commit message matches the PR title** — `feat: add GET /api/books/count endpoint`. (Repo settings default this correctly.)
3. Click **Confirm squash and merge**.
4. Delete the branch when prompted.

### What happens automatically

Open the **Actions** tab. The `Build and Push` workflow runs on `develop`:

1. Dry-runs `semantic-release` to compute the next version.
2. Builds the Docker image with that version.
3. Pushes to GHCR:
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:v1.3.0-beta.1` (example)
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:develop`
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:sha-abc1234`
4. Runs `semantic-release` for real:
   - Updates `CHANGELOG.md`
   - Bumps `<version>` in `pom.xml`
   - Tags the commit `v1.3.0-beta.1`
   - Publishes a **GitHub Release** marked as **Pre-release**
   - Pushes the `chore(release): 1.3.0-beta.1 [skip ci]` commit back to `develop`

Confirm on the **Releases** page: the new entry is there, flagged `Pre-release`.

---

## Step 6 — Promote `develop` to `main` (cut the stable release)

1. Go to **Pull requests** → **New pull request**.
2. **base:** `main`, **compare:** `develop`.
3. Title:
   ```
   chore(release): promote 1.3.0 to stable
   ```
   (Or repeat the feat headline — either works. `chore:` does not itself trigger a release, but the underlying `feat:` commits reaching `main` do.)
4. **Create pull request** → wait for checks → **Merge pull request** (or **Create a merge commit** — **not** squash here, so individual feat/fix commits carry through to `main`).

> Tip: if squash is the only option your repo allows, use a `feat:` title that summarizes the release. `semantic-release` will still compute the right bump.

### What happens automatically on `main`

The same `Build and Push` workflow runs and this time produces a **stable release**:

- Git tag `v1.3.0`
- GHCR tags: `v1.3.0`, `latest`, `sha-<short>`, `main`
- **GitHub Release** (not prerelease)
- `CHANGELOG.md` updated with the stable section
- `pom.xml` bumped to `1.3.0`
- Release commit: `chore(release): 1.3.0 [skip ci]`

---

## Step 7 — Verify

| Check | Where |
| ----- | ----- |
| Stable release exists | **Releases** page — latest entry `v1.3.0`, no "Pre-release" label |
| Docker image published | <https://github.com/automatica-cluj/isp-bookstore-2026-complete/pkgs/container/isp-bookstore-2026-complete> — `v1.3.0` and `latest` tags present |
| `CHANGELOG.md` updated | Repo root on `main` |
| `pom.xml` version bumped | `<version>1.3.0</version>` on `main` |
| README badges green | **CI**, **Build and Push**, and the **Release** badge shows `v1.3.0` |

---

## Common gotchas

- **PR title fails validation** — `feat add count` is wrong (missing colon). Use `feat: add count`.
- **No release appeared** — your title was `docs:`, `chore:`, `test:`, `ci:`, `build:`, or `style:`. These are intentionally no-release. The image was still built and pushed with a `sha-<short>` tag.
- **Suppressing a release deliberately** — add the `no-release` scope: `feat(no-release): draft new endpoint`.
- **Don't hand-edit `CHANGELOG.md` or `pom.xml` `<version>`** — `semantic-release` owns both; your edits will be overwritten on the next release commit.
- **`[skip ci]` on the release commit is normal** — it prevents the release workflow from looping on its own commit. Don't add `[skip ci]` to your feature commits.
- **Forgot to branch from `develop`?** — topic branches should come from `develop`. If you branched from `main`, rebase onto `develop` or recreate the branch; merging `main`-based work into `develop` can pull unreleased fixes into your PR diff.
