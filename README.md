# ISP 2026 Bookstore

[![CI](https://github.com/automatica-cluj/isp-bookstore-2026-complete/actions/workflows/ci.yml/badge.svg)](https://github.com/automatica-cluj/isp-bookstore-2026-complete/actions/workflows/ci.yml)
[![Build and Push](https://github.com/automatica-cluj/isp-bookstore-2026-complete/actions/workflows/build-push.yml/badge.svg)](https://github.com/automatica-cluj/isp-bookstore-2026-complete/actions/workflows/build-push.yml)
[![Release](https://img.shields.io/github/v/release/automatica-cluj/isp-bookstore-2026-complete?include_prereleases&sort=semver)](https://github.com/automatica-cluj/isp-bookstore-2026-complete/releases)
[![Image](https://img.shields.io/badge/ghcr.io-isp--bookstore--2026--complete-blue?logo=docker)](https://github.com/automatica-cluj/isp-bookstore-2026-complete/pkgs/container/isp-bookstore-2026-complete)

Spring Boot bookstore API used as the reference application for the **Software Engineering (ISP) 2026** course at UTCN. A **Caddy** reverse proxy terminates TLS in front of the app, with automatic **Let's Encrypt** certificates when deployed behind a DuckDNS subdomain.

> This repo is the standalone home of the example originally published in the course repo at `examples/07-bookstore-caddy-https`. Releases, Docker images, and the CHANGELOG live here.

## Architecture

```
Browser ──HTTPS:443──▶ Caddy ──HTTP:8090──▶ Spring Boot ──▶ PostgreSQL
                      (TLS termination)     (internal)      (internal)
```

## Run locally (builds from source)

```bash
docker compose -f compose.dev.yml up -d --build
```

Open <https://localhost> (browser will warn about the self-signed cert — that's expected).

For HTTPS with a real cert, point a DuckDNS subdomain at your host and run:

```bash
SITE_ADDRESS=my-bookstore.duckdns.org docker compose -f compose.dev.yml up -d --build
```

See [docs/HTTPS_Setup.md](docs/HTTPS_Setup.md) for the full DuckDNS walkthrough.

### Without Docker (H2 in-memory)

```bash
mvn spring-boot:run
```

Then open <http://localhost:8090>.

## Run with a published image (production-like)

Images are published to GHCR on every push to `main` / `develop`:

- `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:latest` — most recent `main`
- `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:v1.2.3` — stable tag
- `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:v1.3.0-beta.1` — prerelease from `develop`
- `ghcr.io/automatica-cluj/isp-bookstore-2026-complete:sha-abc1234` — any commit

```bash
# one-time: copy and adjust env
cp .env.example .env
$EDITOR .env   # set SITE_ADDRESS and APP_IMAGE

# pull + run the published image (no local build)
docker compose -f compose.prod.yml --env-file .env up -d
```

To pin a specific version:

```bash
APP_IMAGE=ghcr.io/automatica-cluj/isp-bookstore-2026-complete:v1.2.3 \
  docker compose -f compose.prod.yml --env-file .env up -d
```

Browse all tags: <https://github.com/automatica-cluj/isp-bookstore-2026-complete/pkgs/container/isp-bookstore-2026-complete>.

## Key files

| File                 | Purpose                                                                 |
| -------------------- | ----------------------------------------------------------------------- |
| `compose.dev.yml`    | Local build — uses the in-tree Dockerfile, good for development         |
| `compose.prod.yml`   | Pulls the published image from GHCR — use on servers                    |
| `.env.example`       | Environment template for `compose.prod.yml`                             |
| `Caddyfile`          | Caddy reverse-proxy + TLS configuration                                 |
| `Dockerfile`         | Multi-stage build (Temurin JDK → JRE) with OCI labels                   |
| `release.config.js`  | semantic-release config (drives versions, CHANGELOG, GitHub releases)   |

## Build & run commands

| Command                                                       | Description                                        |
| ------------------------------------------------------------- | -------------------------------------------------- |
| `docker compose -f compose.dev.yml up -d --build`             | Local build with Caddy + Postgres + app            |
| `docker compose -f compose.prod.yml --env-file .env up -d`    | Pull published image from GHCR and run             |
| `docker compose -f compose.dev.yml down -v`                   | Stop and remove containers + volumes               |
| `mvn spring-boot:run`                                         | Run locally with H2 (no Docker, no Caddy)          |
| `mvn test`                                                    | Run the test suite                                 |

## Release pipeline

On every push to `main` or `develop`:

1. **CI** (`ci.yml`) — compiles, runs tests, packages the JAR.
2. **Build and Push** (`build-push.yml`):
   - Dry-runs `semantic-release` to compute the next version from the commit history.
   - Builds the Docker image with `VERSION` / `VCS_REF` / `BUILD_DATE` build args.
   - Pushes to GHCR tagged with `vX.Y.Z`, `latest` (on `main`), `sha-<short>`, and the branch name.
   - If a new version is due, runs `semantic-release` for real — updates `CHANGELOG.md`, bumps `pom.xml`, tags the commit, and publishes a GitHub Release.

PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/) — enforced by `.github/workflows/pr-title.yml`. See **[CONTRIBUTING.md](CONTRIBUTING.md)** for the cheat sheet and repo-settings checklist.

## Further documentation

- **[CONTRIBUTING.md](CONTRIBUTING.md)** — branch strategy, commit conventions, first-time repo setup
- **[docs/HTTPS_Setup.md](docs/HTTPS_Setup.md)** — HTTPS setup with Caddy and DuckDNS
- **[docs/CORS.md](docs/CORS.md)** — How CORS works and how it's configured here
- **[docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md)** — Architecture, API reference, and glossary
- **[docs/COLLECTIONS-GUIDE.md](docs/COLLECTIONS-GUIDE.md)** — Java Collections patterns walkthrough
- **[docs/AzureVM_Setup.md](docs/AzureVM_Setup.md)** — Azure Student VM deployment guide
