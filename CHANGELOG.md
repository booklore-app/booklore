# Changelog

Curated, human-readable record of notable changes in this fork, distinct from
the git commit history (which is the authoritative record of *exactly* what
changed, line by line — see `git log`). This file tracks the *why* and the
bigger picture, updated as we go.

## 2026-08-01 / 2026-08-02

### Added
- **PostgreSQL support** — the project's database layer, schema migrations,
  and every Docker/Podman/Helm example were ported from MariaDB to
  PostgreSQL, which is now the only supported database.
- **Native (non-Docker) install & deploy tooling** — `install.sh` sets up a
  full native install (Java, Node, PostgreSQL, systemd services, reverse
  proxy + TLS via Caddy or nginx/certbot). Supports both a **dev mode**
  (hot reload via `gradlew bootRun` + `ng serve`) and a **production mode**
  (Angular build embedded into a compiled backend jar, single `java -jar`
  process — mirrors what the official Dockerfile does for the container
  image). `deploy.sh` pulls and applies code changes to an existing install,
  rebuilding for production mode automatically when needed.
- **Local documentation archive** — the original docs site
  (`booklore.org/docs`) went offline when the upstream project was
  abandoned; a full local copy now lives at `/docs` in the app itself
  (`booklore-ui/public/docs/`), archived from the last available Wayback
  Machine snapshot, with all in-app doc links repointed to it.
- **Bookshelf metadata provider** — added `bookshelf.nz` (a companion
  catalogue site) as a new metadata source, following the existing
  `BookParser` plugin pattern: ISBN/ASIN direct lookup with search
  fallback, `X-Catalog-Api-Key` auth header, settings UI to
  enable/configure it.
- **Bookdrop reprocess & bulk auto-apply** — Bookdrop's review screen
  previously fetched metadata exactly once automatically with no way to
  retry or pick a different source, and required opening every file
  individually even when a match was obviously correct. Added:
  - a per-file "Refetch metadata" control with a provider dropdown
    (reprocess with current Quick Book Match settings, or pull from one
    specific provider)
  - a match-confidence score (title/author similarity between the file's
    own metadata and the fetched result) shown as a badge per file
  - a bulk "Auto-Apply Matches" action with a configurable confidence
    threshold that applies fetched metadata for every file at or above it
    in one click, leaving low-confidence files for manual review

### Fixed
- Bookdrop metadata (`original_metadata`/`fetched_metadata` and similar
  nullable JSON columns) failing to persist on Postgres — Hibernate needed
  explicit `@JdbcTypeCode(SqlTypes.JSON)` on these fields to bind `NULL`
  correctly against native Postgres `json` columns.
- `install.sh` permission-denied writing `ALLOWED_ORIGINS` during
  reverse-proxy setup (`sed -i` needs write access to the containing
  directory, not just the file, to create its temp file).
- `deploy.sh` failing to find Java for the production build step when run
  in an interactive shell that never sourced SDKMAN.
- Bookshelf provider missing from the Quick Book Match provider dropdown
  (a separate hardcoded list from the ones touched when the provider was
  first added).
- **OPDS/Komga "403 Forbidden" despite correct credentials** — root cause
  was the `opds_server_enabled` app setting being `false` in the database
  on both the dev and production instances (on production, the admin's own
  attempt to toggle it on via Settings had itself silently failed with
  403 — worth revisiting: check whether the production admin account
  actually has the `MANAGE_GLOBAL_PREFERENCES`/admin permission needed to
  save settings). Also found and fixed along the way: `AppSettings` is
  cached in-memory per JVM instance and only invalidated through the
  app's own settings-update path — a direct SQL `UPDATE` to fix the value
  on production didn't take effect until `booklore-api` was restarted.

### Changed
- Rebranded the in-app "Support BookLore" links (GitHub star → this fork,
  Ko-fi instead of the original's donation links) and added a fork notice
  to the README, since the upstream project appears to have been
  abandoned by its maintainer.
- OPDS/Komga Basic Auth security chains now wire an explicit
  `DaoAuthenticationProvider` per chain instead of relying on a single
  shared `AuthenticationManager` bean — more robust with multiple
  `SecurityFilterChain` beans in play, even though it turned out not to
  be the cause of the 403 above.

### Planned
- **iOS companion app** — see [`docs/ios-app-plan.md`](docs/ios-app-plan.md).
  Scoped out 2026-08-02, on hold until the user is back from a trip.
