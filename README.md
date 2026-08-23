<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/logo-with-text-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="assets/logo-with-text-light.svg">
    <img src="assets/logo-with-text-light.svg" alt="BookLore" height="80" />
  </picture>
</p>

<p align="center"><strong>Your books deserve a home. This is it.</strong></p>

<p align="center">
BookLore is a self-hosted app that brings your entire book collection under one roof.<br/>
Organize, read, annotate, sync across devices, and share, all without relying on third-party services.
</p>

---

> [!NOTE]
> **This is a fork.** The original [booklore-app/booklore](https://github.com/booklore-app/booklore) project appears to have been
> abandoned by its maintainer. This fork keeps it running and moving forward — see [What's Changed](#whats-changed-in-this-fork) below.

---

## 🔀 What's Changed in This Fork

- **PostgreSQL instead of MariaDB.** The database layer, all schema migrations, and every Docker/Podman/Helm example have been
  ported from MariaDB to PostgreSQL.
- **Native (non-Docker) install & deploy tooling.** `install.sh` sets up a full native install (Java, Node, PostgreSQL,
  systemd services, reverse proxy + TLS via Caddy or nginx/certbot) without requiring Docker. `deploy.sh` pulls and applies
  code changes to an existing install.
- **Local documentation archive.** The original docs site (`booklore.org/docs`) went offline along with the abandoned project;
  a full local copy now lives at `/docs` in the app itself so in-app help links keep working.

---

## ✨ Features

| | Feature | Description |
|:---:|:---|:---|
| 📚 | **Smart Shelves** | Custom and dynamic shelves that organize themselves with rule-based Magic Shelves, filters, and full-text search |
| 🔍 | **Automatic Metadata** | Covers, descriptions, reviews, and ratings pulled from Google Books, Open Library, and Amazon, all editable |
| 📖 | **Built-in Reader** | Open PDFs, EPUBs, and comics right in the browser with annotations, highlights, and reading progress |
| 🔄 | **Device Sync** | Connect your Kobo, use any OPDS-compatible app, or sync progress with KOReader. Your library follows you everywhere |
| 👥 | **Multi-User Ready** | Individual shelves, progress, and preferences per user with local or OIDC authentication |
| 📥 | **BookDrop** | Drop files into a watched folder and BookLore detects, enriches, and queues them for import automatically |
| 📧 | **One-Click Sharing** | Send any book to a Kindle, an email address, or a friend instantly |

---

## 🚀 Quick Start

All you need is [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/).

<details>
<summary><strong>📦 Image Repositories</strong></summary>

| Registry | Image                              |
|----------|------------------------------------|
| GitHub Container Registry | `ghcr.io/booklore-app/booklore` |

</details>

### Step 1: Environment Configuration

Create a `.env` file:

```ini
# Application
APP_USER_ID=1000
APP_GROUP_ID=1000
TZ=Etc/UTC

# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/booklore
DB_USER=booklore
DB_PASSWORD=ChangeMe_BookLoreApp_2025!

# Storage: LOCAL (default) or NETWORK (disables file operations, see Network Storage section below)
DISK_TYPE=LOCAL

# PostgreSQL
POSTGRES_DB=booklore
```

### Step 2: Docker Compose

Create a `docker-compose.yml`:

```yaml
services:
  booklore:
    image: ghcr.io/booklore-app/booklore:latest
    container_name: booklore
    environment:
      - USER_ID=${APP_USER_ID}
      - GROUP_ID=${APP_GROUP_ID}
      - TZ=${TZ}
      - DATABASE_URL=${DATABASE_URL}
      - DATABASE_USERNAME=${DB_USER}
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - DISK_TYPE=${DISK_TYPE}
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "6060:6060"
    volumes:
      - ./data:/app/data
      - ./books:/books
      - ./bookdrop:/bookdrop
    healthcheck:
      test: wget -q -O - http://localhost:6060/api/v1/healthcheck
      interval: 60s
      retries: 5
      start_period: 60s
      timeout: 10s
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: postgres
    environment:
      - TZ=${TZ}
      - POSTGRES_DB=${POSTGRES_DB}
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./postgres/data:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: [ "CMD", "pg_isready", "-U", "${DB_USER}", "-d", "${POSTGRES_DB}" ]
      interval: 5s
      timeout: 5s
      retries: 10
```

### Step 3: Launch

```bash
docker compose up -d
```

Open **http://localhost:6060**, create your admin account, and start building your library.

---

## ⚠️ Network Storage (NAS / NFS / SMB / CIFS)

> [!CAUTION]
> BookLore's file operations (metadata writing, file renaming, file organization) are built for **local file systems only**. Network-attached storage (NAS, NFS, SMB/CIFS mounts, cloud-backed FUSE, etc.) is **unsupported and untested**. Mount options, network latency, caching, and filesystem semantics are all outside BookLore's control and can cause silent file corruption, incomplete writes, missing files, and other unpredictable behavior. **Issues related to network storage will be closed without investigation.**

If your book files live on network storage, set `DISK_TYPE=NETWORK` in your `.env` file. This puts BookLore into **network storage mode**, which disables all file write and reorganization features. Metadata is stored in the database only and your files are never modified. This is the only supported configuration for network storage.

---

## 📥 BookDrop: Zero-Effort Import

Drop book files into a folder. BookLore picks them up, pulls metadata, and queues everything for your review.

```mermaid
graph LR
    A[📁 Drop Files] --> B[🔍 Auto-Detect]
    B --> C[📊 Extract Metadata]
    C --> D[✅ Review & Import]
```

| Step | What Happens |
|:---|:---|
| 1. **Watch** | BookLore monitors the BookDrop folder around the clock |
| 2. **Detect** | New files are picked up and parsed automatically |
| 3. **Enrich** | Metadata is fetched from Google Books and Open Library |
| 4. **Import** | You review, tweak if needed, and add to your library |

Mount the volume in `docker-compose.yml`:

```yaml
volumes:
  - ./bookdrop:/bookdrop
```

---

## 💜 Support This Fork

BookLore is free, open source, and built with care. Here's how you can give back:

| Action | How |
|:---|:---|
| ⭐ **Star this repo** | It's the simplest way to help others find this fork |
| ☕ **Buy me a coffee** | [Ko-fi](https://ko-fi.com/xspader) — a one-time tip to fuel continued development |
| 📢 **Tell someone** | Share BookLore with a friend, a subreddit, or your local book club |

---

## 🌍 Translations

BookLore is used by readers around the world. Help make it accessible in your language on [Weblate](https://hosted.weblate.org/engage/booklore/).

<a href="https://hosted.weblate.org/engage/booklore/">
  <img src="https://hosted.weblate.org/widget/booklore/multi-auto.svg?v=1" alt="Translation status" />
</a>

---

## 🌟 Sponsors & Partners

<table>
<tr>

<a href="https://jb.gg/OpenSource">
  <img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg" alt="JetBrains" height="40" />
</a>


</td>
</tr>
</table>
</div>

---

<div align="center">

## ⚖️ License

**GNU Affero General Public License v3.0**

Copyright 2024–2026 BookLore

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/agpl-3.0.html)

</div>
