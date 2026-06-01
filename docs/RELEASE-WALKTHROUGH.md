# Release walkthrough — from GitHub UI

End-to-end example: add a small feature entirely through the GitHub web UI (no local clone) and watch the automated release pipeline cut a new version and publish the image.

> **Prerequisites:** you already have Write access to the repository and the first-time maintainer checklist in `CONTRIBUTING.md` has been done.

---

## How the pipeline decides what to release

This project is **trunk-based**: everything ships from `main`. The release is driven entirely by the **PR title** (because PRs are squash-merged, the PR title becomes the only commit message on `main`). `semantic-release` reads that title:

| PR title prefix                        | Effect on version           |
| -------------------------------------- | --------------------------- |
| `feat: ...`                            | minor bump (1.2.0 → 1.3.0)  |
| `fix:` / `perf:` / `refactor:`         | patch bump (1.2.0 → 1.2.1)  |
| `feat!: ...` or `BREAKING CHANGE:` body | major bump (1.2.0 → 2.0.0)  |
| `docs:` / `chore:` / `test:` / `ci:`   | **no release**              |

There is no `develop` branch and no beta/prerelease channel — a single merge to `main` is the whole release.

---

## The demo feature

Add a public endpoint `GET /api/books/count` that returns the total number of books.

Two files change:

1. `src/main/java/com/bookstore/service/BookService.java` — new `countAll()` method
2. `src/main/java/com/bookstore/controller/BookController.java` — new `@GetMapping("/count")`

The endpoint is public by default (the existing `SecurityConfig` already permits `GET /api/books/**`).

---

## Step 1 — Create a topic branch from `main`

1. On the repo page, click the **branch dropdown** (top-left of the file list).
2. Type `feat/books-count` in the search box.
3. Make sure **Source** is `main`.
4. Click **Create branch: feat/books-count from 'main'**.

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
   (Alternatively: **Pull requests** tab → **New pull request** → base: `main`, compare: `feat/books-count`.)
2. Fill in:
   - **base:** `main`, **compare:** `feat/books-count` (verify both!)
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

## Step 5 — Squash-and-merge into `main`

1. When both checks are green, click **Squash and merge**.
2. **Verify the squash commit message matches the PR title** — `feat: add GET /api/books/count endpoint`. (Repo settings default this correctly.)
3. Click **Confirm squash and merge**.
4. Delete the branch when prompted.

### What happens automatically

Open the **Actions** tab. The `Release and Publish` workflow runs on `main`:

1. Runs `semantic-release` once. It reads the `feat:` commit, computes the next version (e.g. `1.3.0`), then:
   - Updates `CHANGELOG.md`
   - Bumps `<version>` in `pom.xml`
   - Tags the commit `v1.3.0`
   - Publishes a **GitHub Release**
   - Pushes the `chore(release): 1.3.0 [skip ci]` commit back to `main`
2. Builds the Docker image with that version and pushes to GHCR:
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:v1.3.0`
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:latest`
   - `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:sha-abc1234`

Confirm on the **Releases** page: the new `v1.3.0` entry is there.

---

## Step 6 — Verify

| Check | Where |
| ----- | ----- |
| Release exists | **Releases** page — latest entry `v1.3.0` |
| Docker image published | <https://github.com/automatica-cluj/isp-bookstore-2026-complete/pkgs/container/isp-bookstore-2026-complete> — `v1.3.0` and `latest` tags present |
| `CHANGELOG.md` updated | Repo root on `main` |
| `pom.xml` version bumped | `<version>1.3.0</version>` on `main` |
| README badges green | **CI**, **Release and Publish**, and the **Release** badge shows `v1.3.0` |

---

## Common gotchas

- **PR title fails validation** — `feat add count` is wrong (missing colon). Use `feat: add count`.
- **No release appeared** — your title was `docs:`, `chore:`, `test:`, `ci:`, `build:`, or `style:`. These are intentionally no-release. The image was still built and pushed with a `sha-<short>` tag.
- **Suppressing a release deliberately** — add the `no-release` scope: `feat(no-release): draft new endpoint`.
- **Don't hand-edit `CHANGELOG.md` or `pom.xml` `<version>`** — `semantic-release` owns both; your edits will be overwritten on the next release commit.
- **`[skip ci]` on the release commit is normal** — it prevents the release workflow from looping on its own commit. Don't add `[skip ci]` to your feature commits.
